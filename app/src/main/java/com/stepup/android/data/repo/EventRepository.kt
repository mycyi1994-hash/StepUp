package com.stepup.android.data.repo

import com.stepup.android.data.local.ClaimedEventDao
import com.stepup.android.data.local.ClaimedEventEntity
import com.stepup.android.data.local.NotificationType
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.StepDao
import com.stepup.android.data.remote.DaySteps
import com.stepup.android.data.remote.EventApi
import com.stepup.android.data.remote.ServerResult
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.IsoFields
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class EventKind { CHALLENGE, CAMPAIGN, MISSION }

/** 진행형 이벤트 정의 (보상 금액과 목표는 코드로 고정) */
data class EventDef(
    val id: String,
    val kind: EventKind,
    val reward: Double,
    val target: Double,
    /** 목표 달성 없이도 수령 가능한지 (캠페인 등) */
    val claimableWithoutTarget: Boolean = false,
)

object Events {
    val STEP_SURGE = EventDef("step_surge", EventKind.CHALLENGE, 250.0, 80_000.0)
    val REFER = EventDef("refer", EventKind.MISSION, 500.0, 3.0)
    val NIGHT_QUEST = EventDef("night_quest", EventKind.CHALLENGE, 300.0, 20.0)
    val NEON_HORIZON = EventDef("neon_horizon", EventKind.CAMPAIGN, 15_000.0, 1.0)

    val ALL = listOf(STEP_SURGE, REFER, NIGHT_QUEST, NEON_HORIZON)
}

/** 보상 받기를 눌렀을 때의 결말 */
sealed interface EventClaimResult {
    /** 서버가 원장에 적었다 */
    data class Paid(val amount: Double) : EventClaimResult

    /** 이번 기간에 이미 받았다(다른 기기에서 받았을 수도 있다) */
    data object AlreadyClaimed : EventClaimResult

    /** 서버 기록으로는 아직 목표에 못 미친다 */
    data object NotFinished : EventClaimResult

    data object SignInRequired : EventClaimResult

    /** 서버에 닿지 못했다. 다시 누르면 된다. */
    data object Failed : EventClaimResult
}

/**
 * 도전 보상.
 *
 * 받기는 서버가 한다(`event_claim`). 서버가 자기 기록으로 목표를 다시 재고,
 * 기간마다 한 번만 원장에 적는다. 폰의 원장에는 서버가 준 금액을 **받은 뒤에**
 * 똑같이 적어 화면 잔고를 맞춘다. 폰이 먼저 적으면 거절당한 보상이 잔고에 남는다.
 */
class EventRepository(
    private val dao: ClaimedEventDao,
    private val rewardRepository: RewardRepository,
    private val api: EventApi,
    private val stepDao: StepDao,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {

    /** 이번 기간에 받은 도전의 id(`step_surge` · `night_quest`) */
    val claimedIds: Flow<Set<String>> =
        dao.observeAll().map { list ->
            val keys = list.map { it.eventId }.toSet()
            Events.ALL.filter { periodKey(it) in keys }.map { it.id }.toSet()
        }

    /**
     * 보상 받기.
     *
     * @param progress 폰이 잰 진행도(0..1). 목표 전이면 서버에 묻지도 않는다.
     */
    suspend fun claim(def: EventDef, progress: Float): EventClaimResult {
        val key = periodKey(def)
        if (dao.byId(key) != null) return EventClaimResult.AlreadyClaimed
        if (!def.claimableWithoutTarget && progress < 1f) return EventClaimResult.NotFinished

        // 주간 걸음은 서버가 올려 둔 일별 걸음으로 잰다. 받기 전에 최근 걸음을 올린다.
        if (def == Events.STEP_SURGE) {
            val today = LocalDate.now(zone()).toEpochDay()
            val days = (today - 7..today).mapNotNull { day ->
                stepDao.byDay(day)?.let { DaySteps(it.epochDay, it.steps, it.goal) }
            }
            if (days.isNotEmpty()) {
                val synced = api.syncSteps(days)
                if (synced is ServerResult.SignInRequired) return EventClaimResult.SignInRequired
                if (synced !is ServerResult.Ok) return EventClaimResult.Failed
            }
        }

        return when (val result = api.claim(def.id, zone().id)) {
            is ServerResult.Ok -> {
                dao.insert(ClaimedEventEntity(key, System.currentTimeMillis(), result.value))
                rewardRepository.credit(RewardType.EARN_EVENT, result.value, "이벤트 보상: ${def.id}")
                rewardRepository.notify(NotificationType.EVENT_CLAIMED, def.id, result.value)
                EventClaimResult.Paid(result.value)
            }
            is ServerResult.Rejected ->
                if (result.reason.contains(ALREADY_CLAIMED)) {
                    // 다른 기기에서 받았다. 잔고는 그 기기 쪽에서 이미 올랐으므로 여기선 표시만 맞춘다.
                    dao.insert(ClaimedEventEntity(key, System.currentTimeMillis(), 0.0))
                    EventClaimResult.AlreadyClaimed
                } else {
                    EventClaimResult.NotFinished
                }
            is ServerResult.SignInRequired -> EventClaimResult.SignInRequired
            is ServerResult.Retry -> EventClaimResult.Failed
        }
    }

    /**
     * 한 번씩 받는 단위의 이름. 주간 도전은 ISO 주마다(`step_surge:2026-W39`),
     * 나머지는 한 번(`night_quest`). 서버의 economy.event_period 와 같은 규칙이다.
     */
    fun periodKey(def: EventDef, today: LocalDate = LocalDate.now(zone())): String =
        eventPeriodKey(def, today)

    private companion object {
        /** 서버가 "이미 받음"을 알리는 문구(0014_events.sql) */
        const val ALREADY_CLAIMED = "이미 받은"
    }
}

/** [EventRepository.periodKey] 의 규칙 — 서버의 economy.event_period 와 같다. */
internal fun eventPeriodKey(def: EventDef, today: LocalDate): String =
    if (def == Events.STEP_SURGE) {
        val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
        val year = today.get(IsoFields.WEEK_BASED_YEAR)
        "${def.id}:%d-W%02d".format(Locale.ROOT, year, week)
    } else {
        def.id
    }
