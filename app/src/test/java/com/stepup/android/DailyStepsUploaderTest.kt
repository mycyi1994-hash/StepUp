package com.stepup.android

import com.stepup.android.data.remote.DaySteps
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.repo.DailyStepsUploader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 하루 걸음 올리기 — 체인 하루 걸음 기록(DAILY_STEPS)이 0 건이던 것(앱이 steps_sync 를 부르지 않았다, 2026-11-18) */
class DailyStepsUploaderTest {

    private class Fake {
        val days = mutableMapOf<Long, Pair<Int, Int>>()
        val sends = mutableListOf<List<DaySteps>>()
        var answer: ServerResult<Unit> = ServerResult.Ok(Unit)
        var today = 100L
        var who: String? = "account:a"
        fun uploader() = DailyStepsUploader(
            readDay = { days[it] },
            send = { sends += it; answer },
            today = { today },
            account = { who },
        )
    }

    @Test fun `어제 · 오늘만, 바뀐 날만 보낸다`() = runBlocking {
        val fake = Fake().apply { days[98] = 9000 to 8000; days[99] = 7000 to 8000; days[100] = 1200 to 8000 }
        val up = fake.uploader()
        assertTrue(up.upload())
        assertEquals(listOf(DaySteps(99, 7000, 8000), DaySteps(100, 1200, 8000)), fake.sends.single())
        // 바뀐 것이 없으면 보내지 않는다
        assertTrue(up.upload())
        assertEquals(1, fake.sends.size)
        // 오늘 목표를 채웠다 — 오늘만 다시
        fake.days[100] = 8100 to 8000
        up.upload()
        assertEquals(listOf(DaySteps(100, 8100, 8000)), fake.sends.last())
    }

    @Test fun `걸음 0 인 날은 보내지 않는다 · 못 보냈으면 다음에 다시`() = runBlocking {
        val fake = Fake().apply { days[99] = 0 to 8000; days[100] = 500 to 8000; answer = ServerResult.Retry("offline") }
        val up = fake.uploader()
        assertFalse(up.upload())
        assertEquals(listOf(DaySteps(100, 500, 8000)), fake.sends.single())
        fake.answer = ServerResult.Ok(Unit)
        assertTrue(up.upload())
        assertEquals(2, fake.sends.size)
    }

    @Test fun `계정이 바뀌면 같은 값도 다시 보낸다`() = runBlocking {
        val fake = Fake().apply { days[100] = 3000 to 8000 }
        val up = fake.uploader()
        up.upload()
        fake.who = "account:b"
        up.upload()
        assertEquals(2, fake.sends.size)
        // 로그아웃 정리도 같다
        up.forget()
        up.upload()
        assertEquals(3, fake.sends.size)
    }
}
