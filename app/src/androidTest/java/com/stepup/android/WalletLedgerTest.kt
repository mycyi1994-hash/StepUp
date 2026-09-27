package com.stepup.android

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.local.AppDatabase
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardType
import com.stepup.android.ui.screens.rewards.LedgerFilter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 지갑 v1 이용 내역 — 거르개는 처음 읽은 한 쪽이 아니라 원장 전체에서, 쪽은 (시각, id) 최근순으로 이어진다(겹침 · 빠짐 없음).
 * 잔액 · 누계는 거르개와 상관없이 원장 전체.
 */
class WalletLedgerTest {
    @Test fun pagesFilterTheWholeLedgerInAStableOrder() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        try {
            val dao = db.rewardDao()
            // 가장 오래된 사용 한 줄 + 같은 시각의 적립 130줄 + 금액 0 인 모르는 종류 한 줄
            dao.insert(RewardEntity(timestamp = 1, type = RewardType.SPEND_UPGRADE, amount = -20.0, description = "old spend"))
            repeat(130) { dao.insert(RewardEntity(timestamp = 1_000, type = RewardType.EARN_WALK, amount = 1.0, description = "walk $it")) }
            dao.insert(RewardEntity(timestamp = 2_000, type = "UNKNOWN_KIND", amount = 0.0, description = "zero"))

            // 사용 — 적립 130줄 뒤의 한 줄도 첫 쪽에서 찾는다
            val spent = dao.observeLedgerPage(LedgerFilter.SPENT.kind, 30).first()
            assertEquals(listOf(-20.0), spent.map { it.amount })
            // 적립 — 양수만(0 은 적립도 사용도 아니다)
            val earned = dao.observeLedgerPage(LedgerFilter.EARNED.kind, 500).first()
            assertEquals(130, earned.size)
            assertTrue(earned.all { it.amount > 0 })
            // 전체 — 최근순, 0 인 줄은 전체에서만
            val all = dao.observeLedgerPage(LedgerFilter.ALL.kind, 500).first()
            assertEquals(132, all.size)
            assertEquals(0.0, all.first().amount, 0.0)
            assertEquals(-20.0, all.last().amount, 0.0)

            // 같은 시각은 id 가 큰 것부터 — 다음 쪽이 앞 쪽을 그대로 잇는다
            val first = dao.observeLedgerPage(LedgerFilter.ALL.kind, 30).first()
            val firstTwo = dao.observeLedgerPage(LedgerFilter.ALL.kind, 60).first()
            assertEquals(first, firstTwo.take(30))
            assertEquals(60, firstTwo.map { it.id }.distinct().size)
            val sameTime = all.filter { it.timestamp == 1_000L }.map { it.id }
            assertEquals(sameTime.sortedDescending(), sameTime)

            // 잔액 · 누계는 원장 전체 그대로
            val totals = dao.observeTotals().first()
            assertEquals(130.0, totals.earned, 1e-9)
            assertEquals(20.0, totals.spent, 1e-9)
            assertEquals(110.0, totals.balance, 1e-9)
        } finally {
            db.close()
        }
    }
}
