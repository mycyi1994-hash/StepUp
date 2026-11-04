package com.stepup.android

import com.stepup.android.data.remote.SessionRecorded
import com.stepup.android.data.remote.serverJson
import com.stepup.android.domain.RunVerdictReason
import com.stepup.android.domain.RunVerdictReason.Kind
import kotlinx.serialization.decodeFromString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RunVerdictReasonTest {

    @Test fun 적립_0_이면_서버_까닭을_앞에서부터_둘까지() {
        val note = RunVerdictReason.note(
            "SIGNED", 0.0,
            "걸음 수가 GPS 거리보다 많습니다 · 하루 적립 금액 상한에 걸렸습니다 · 에너지를 다 썼습니다", "",
        )!!
        assertTrue(note.zero)
        assertEquals(listOf(Kind.STEPS_OVER_GPS, Kind.DAILY_SUP), note.kinds)
    }

    @Test fun 모르는_까닭은_짓지_않는다() {
        val note = RunVerdictReason.note("SIGNED", 0.0, "새로 생긴 까닭", "")!!
        assertTrue(note.zero)
        assertTrue(note.kinds.isEmpty())
        // 적립된 러닝에 모르는 까닭뿐이면 줄을 두지 않는다
        assertNull(RunVerdictReason.note("SIGNED", 12.0, "새로 생긴 까닭", ""))
        assertNull(RunVerdictReason.note("SIGNED", 12.0, "", ""))
    }

    @Test fun 줄어든_적립은_까닭만() {
        val note = RunVerdictReason.note("SIGNED", 3.5, "내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다", "")!!
        assertEquals(false, note.zero)
        assertEquals(listOf(Kind.WORN_AFTER), note.kinds)
    }

    @Test fun 서버가_답하지_않은_러닝은_보이지_않는다() {
        assertNull(RunVerdictReason.note("PENDING", 0.0, "", ""))
        assertNull(RunVerdictReason.note("FAILED", 0.0, "", "연결이 끊겼어요"))
    }

    @Test fun 거절된_러닝은_거절_까닭() {
        val note = RunVerdictReason.note("REJECTED", 0.0, "", "너무 오래된 러닝입니다")!!
        assertTrue(note.zero)
        assertEquals(listOf(Kind.TOO_OLD), note.kinds)
    }

    @Test fun 예전_서버_응답도_읽는다() {
        val old = serverJson.decodeFromString<List<SessionRecorded>>(
            """[{"session_id":1,"verdict":"CLEAN","points_awarded":2.5,"balance":10}]""",
        ).single()
        assertNull(old.verdictReason)
        val now = serverJson.decodeFromString<List<SessionRecorded>>(
            """[{"session_id":1,"verdict":"VOID","points_awarded":0,"balance":10,"verdict_reason":"가짜 위치가 감지되었습니다"}]""",
        ).single()
        assertEquals("가짜 위치가 감지되었습니다", now.verdictReason)
    }
}
