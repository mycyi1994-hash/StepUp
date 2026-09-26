package com.stepup.android

import com.stepup.android.ui.screens.events.ChallengeFocus
import com.stepup.android.ui.screens.events.ChallengeKind
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class ChallengeFocusTest {
    private fun seoul(hour: Int) =
        ZonedDateTime.of(2026, 9, 26, hour, 5, 0, 0, ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()

    @Test fun stepChallengesAddThisRunsSteps() {
        val daily = ChallengeFocus(ChallengeKind.DAILY, base = 5_000.0, target = 8_000.0)
        assertEquals(6_200.0, daily.expected(1_200, 0.9, seoul(9)), 0.0)
        assertEquals(0.775f, daily.fraction(6_200.0), 0.001f)
        val weekly = ChallengeFocus(ChallengeKind.WEEKLY, base = 79_500.0, target = 80_000.0)
        assertEquals(1f, weekly.fraction(weekly.expected(900, 0.7, seoul(9))), 0f)
    }

    @Test fun nightQuestCountsOnlyRunsStartedAfterEightPmSeoul() {
        val night = ChallengeFocus(ChallengeKind.NIGHT, base = 4.0, target = 20.0)
        assertEquals(4.0, night.expected(3_000, 2.5, seoul(19)), 0.0)
        assertEquals(6.5, night.expected(3_000, 2.5, seoul(20)), 0.0)
    }

    @Test fun focusBelongsToTheFirstRunItMeets() {
        val focus = ChallengeFocus(ChallengeKind.DAILY, base = 1_000.0, target = 8_000.0)
        com.stepup.android.ui.screens.events.ChallengeRunFocus.set(focus)
        try {
            val first = seoul(9)
            assertEquals(true, com.stepup.android.ui.screens.events.ChallengeRunFocus.matches(first))
            com.stepup.android.ui.screens.events.ChallengeRunFocus.bind(first)
            com.stepup.android.ui.screens.events.ChallengeRunFocus.bind(first)
            assertEquals(focus, com.stepup.android.ui.screens.events.ChallengeRunFocus.current.value)
            // 다른 러닝(모임 · 알림에서 시작)을 만나면 옛 기준값을 보이지 않는다
            val later = seoul(21)
            assertEquals(false, com.stepup.android.ui.screens.events.ChallengeRunFocus.matches(later))
            com.stepup.android.ui.screens.events.ChallengeRunFocus.bind(later)
            assertEquals(null, com.stepup.android.ui.screens.events.ChallengeRunFocus.current.value)
        } finally {
            com.stepup.android.ui.screens.events.ChallengeRunFocus.clear()
        }
    }
}
