package com.stepup.android

import com.stepup.android.domain.averagePace
import com.stepup.android.domain.coaching.CoachingQuality
import com.stepup.android.domain.coaching.KmSplit
import com.stepup.android.domain.coaching.LapCodec
import com.stepup.android.domain.coaching.RunSplits
import com.stepup.android.domain.coaching.SavedLap
import com.stepup.android.domain.coaching.SplitGap
import com.stepup.android.domain.coaching.paceText
import com.stepup.android.domain.coaching.runSplits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AI 코칭의 1km 구간 — 지시서 13절 검수 숫자(3.24km · 24분18초 · 432 · 446 · 472 · 마지막 0.24km 108초)와
 * 구간을 보이지 않는 까닭들. 구간은 랩의 누적 활성 시간 차로만 센다.
 */
class CoachingSplitsTest {
    /** 랩은 누적 초 — 432, 432+446, 432+446+472 */
    private val sampleLaps = LapCodec.encode(listOf(SavedLap(1, 1.0, 432), SavedLap(2, 2.0, 878), SavedLap(3, 3.0, 1350)))

    @Test fun `검수 숫자 — 3_24km 24분18초는 450초 per km 이고 7분30초다`() {
        val pace = averagePace(1458, 3240.0)
        assertEquals(450.0, pace!!, 1e-9)
        assertEquals("7'30\"", paceText(pace))
    }

    @Test fun `검수 숫자 — 전체 1km 구간 432 446 472 와 마지막 0_24km 108초, 합계 1458초`() {
        val splits = runSplits(3240.0, 1458, sampleLaps) as RunSplits.Ready
        assertEquals(listOf(KmSplit(1, 432), KmSplit(2, 446), KmSplit(3, 472)), splits.full)
        val partial = splits.partial!!
        assertEquals(240.0, partial.meters, 1e-6)
        assertEquals(108L, partial.seconds)
        assertEquals(1458L, splits.full.sumOf { it.seconds } + partial.seconds)
        // 마지막 전체 구간과 첫 구간 차이 40초 — 부분 구간(108초)과는 견주지 않는다
        assertEquals(40L, splits.full.last().seconds - splits.full.first().seconds)
        assertEquals(listOf("7'12\"", "7'26\"", "7'52\""), splits.full.map { paceText(it.seconds.toDouble()) })
    }

    @Test fun `평균 페이스는 전체 시간 나누기 전체 거리 — 구간 페이스의 단순 평균이 아니다`() {
        // 검수 샘플은 두 값이 우연히 같다(450) — 갈라지는 기록으로 본다: 2.5km 1150초, 구간 420 · 480, 남은 0.5km 250초
        val laps = LapCodec.encode(listOf(SavedLap(1, 1.0, 420), SavedLap(2, 2.0, 900)))
        val splits = runSplits(2500.0, 1150, laps) as RunSplits.Ready
        assertEquals(listOf(420L, 480L), splits.full.map { it.seconds })
        assertEquals(500.0, splits.partial!!.paceSecPerKm, 1e-9)
        val naive = (splits.full.map { it.seconds.toDouble() } + splits.partial!!.paceSecPerKm).average()
        assertEquals(460.0, averagePace(1150, 2500.0)!!, 1e-9)
        assertTrue(kotlin.math.abs(naive - 460.0) > 5)
    }

    @Test fun `랩을 남기기 전의 기록은 구간이 없다고 한다`() {
        assertEquals(RunSplits.Unavailable(SplitGap.NOT_SAVED), runSplits(3240.0, 1458, ""))
    }

    @Test fun `수동 랩이 섞이면 구간을 보이지 않는다`() {
        val laps = LapCodec.encode(listOf(SavedLap(1, 0.5, 200), SavedLap(2, 2.0, 878), SavedLap(3, 3.0, 1350)))
        assertEquals(RunSplits.Unavailable(SplitGap.MANUAL_LAPS), runSplits(3240.0, 1458, laps))
    }

    @Test fun `랩 수가 기록 거리의 정수 km 와 다르면 맞지 않는다`() {
        val two = LapCodec.encode(listOf(SavedLap(1, 1.0, 432), SavedLap(2, 2.0, 878)))
        assertEquals(RunSplits.Unavailable(SplitGap.MISMATCH), runSplits(3240.0, 1458, two))
    }

    @Test fun `랩 시간이 거꾸로 가거나 기록 시간을 넘으면 맞지 않는다`() {
        val backwards = LapCodec.encode(listOf(SavedLap(1, 1.0, 432), SavedLap(2, 2.0, 400), SavedLap(3, 3.0, 1350)))
        assertEquals(RunSplits.Unavailable(SplitGap.MISMATCH), runSplits(3240.0, 1458, backwards))
        val beyond = LapCodec.encode(listOf(SavedLap(1, 1.0, 432), SavedLap(2, 2.0, 878), SavedLap(3, 3.0, 1500)))
        assertEquals(RunSplits.Unavailable(SplitGap.MISMATCH), runSplits(3240.0, 1458, beyond))
    }

    @Test fun `깨진 랩 글자는 일부만 읽지 않고 맞지 않는다고 한다`() {
        assertNull(LapCodec.decode("1,1.0,432;2,2.0"))
        assertNull(LapCodec.decode("x,1.0,432"))
        assertNull(LapCodec.decode("1,NaN,432"))
        assertEquals(RunSplits.Unavailable(SplitGap.MISMATCH), runSplits(3240.0, 1458, "1,1.0,432;oops"))
    }

    @Test fun `사람이 낼 수 없는 구간은 보이지 않는다`() {
        val fast = LapCodec.encode(listOf(SavedLap(1, 1.0, CoachingQuality.MIN_SPLIT_SEC - 1)))
        assertEquals(RunSplits.Unavailable(SplitGap.OUT_OF_RANGE), runSplits(1500.0, 900, fast))
        val ok = LapCodec.encode(listOf(SavedLap(1, 1.0, CoachingQuality.MIN_SPLIT_SEC)))
        assertTrue(runSplits(1500.0, 900, ok) is RunSplits.Ready)
    }

    @Test fun `1km 를 못 채운 러닝 · 거리 없는 러닝`() {
        assertEquals(RunSplits.Unavailable(SplitGap.TOO_SHORT), runSplits(999.9, 400, ""))
        assertEquals(RunSplits.Unavailable(SplitGap.NO_DISTANCE), runSplits(0.0, 400, sampleLaps))
        assertEquals(RunSplits.Unavailable(SplitGap.NO_DISTANCE), runSplits(-5.0, 400, sampleLaps))
        assertEquals(RunSplits.Unavailable(SplitGap.NO_DISTANCE), runSplits(Double.NaN, 400, sampleLaps))
        assertEquals(RunSplits.Unavailable(SplitGap.NO_DISTANCE), runSplits(Double.POSITIVE_INFINITY, 400, sampleLaps))
        assertEquals(RunSplits.Unavailable(SplitGap.NO_DISTANCE), runSplits(3240.0, 0, sampleLaps))
    }

    @Test fun `기록 거리는 걸음 거리 — 서비스와 같은 식으로 정수 km 를 센다`() {
        // 4252걸음 × 0.762m = 3240.024m → 3km. 서비스의 자동 랩도 (m / 1000).toInt() 로 센다
        val meters = 4252 * 0.762
        assertTrue(runSplits(meters, 1458, sampleLaps) is RunSplits.Ready)
    }

    @Test fun `아주 짧은 마지막 남은 거리는 부분 구간으로 보이지 않는다`() {
        val splits = runSplits(3005.0, 1360, sampleLaps) as RunSplits.Ready
        assertNull(splits.partial)
        assertEquals(3, splits.full.size)
    }

    @Test fun `랩 글자는 로캘과 상관없이 같은 모양으로 오간다`() {
        val laps = listOf(SavedLap(1, 1.0, 432), SavedLap(2, 2.0, 878))
        val saved = LapCodec.encode(laps)
        assertEquals("1,1.0,432;2,2.0,878", saved)
        assertEquals(laps, LapCodec.decode(saved))
        assertEquals(emptyList<SavedLap>(), LapCodec.decode(""))
    }
}
