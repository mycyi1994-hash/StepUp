package com.stepup.android

import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.RecordTotals
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RecordPeriod
import com.stepup.android.domain.RunMark
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TrackPoint
import com.stepup.android.domain.bestDay
import com.stepup.android.domain.monthBars
import com.stepup.android.domain.range
import com.stepup.android.domain.weekBars
import com.stepup.android.ui.MainScaffold
import com.stepup.android.ui.Screen
import com.stepup.android.ui.components.CommerceBackdrop
import com.stepup.android.ui.experience.ExperienceProvider
import com.stepup.android.ui.screens.records.DeleteState
import com.stepup.android.ui.screens.records.PeriodSheet
import com.stepup.android.ui.screens.records.RecordLookup
import com.stepup.android.ui.screens.records.RecordStatsContent
import com.stepup.android.ui.screens.records.RecordsContent
import com.stepup.android.ui.screens.records.RecordsLoad
import com.stepup.android.ui.screens.records.RunRecordContent
import com.stepup.android.ui.screens.records.RunRouteMapContent
import com.stepup.android.ui.screens.records.StatWindow
import com.stepup.android.ui.screens.records.StatsUi
import com.stepup.android.ui.theme.StepUpTheme
import com.stepup.android.ui.theme.ThemeMode
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 내 러닝 기록(2026-09-28 전달본, docs/redesign/running-records) — 장면 번호는 시안의 01~16 그대로.
 *
 * [recordsInTheApp] 은 앱 셸 안에서 내 정보 → 내 러닝 기록으로 간다. 이 기기의 러닝 세션에 테스트 기록(이번 달, 경로 있는
 * 셋 + 시간만 있는 하나)을 넣고, 목록 합계가 저장소의 이번 달 전체 합계와 같은지, 기간 시트는 "적용"해야만 바뀌는지,
 * 통계 · 상세 · 경로 확대 · 삭제(실패하면 기록이 남고, 성공하면 목록과 합계에서 빠진다)를 실제로 탄다. 넣은 기록은 업로드되지
 * 않게 REJECTED 로 넣고 끝에 지운다. [recordStates] 는 전달본 sample-data.json(예시 — 2026-09-28 기준 9월 러닝 8개)을
 * 화면에 바로 넣어 시안과 같은 날짜 · 숫자로 찍는다. 경로는 예시 모양이지 실제 위치가 아니다.
 */
class RecordsDesignTest {
    @get:Rule(order = 0) val appLanguage = object : org.junit.rules.ExternalResource() {
        private var previous = ""
        override fun before() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            val context = instrumentation.targetContext
            com.stepup.android.core.AppLocale.syncFromSystem(context)
            previous = com.stepup.android.core.AppLocale.tag
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(context, "ko") }
        }
        override fun after() {
            val instrumentation = InstrumentationRegistry.getInstrumentation()
            instrumentation.runOnMainSync { com.stepup.android.core.AppLocale.change(instrumentation.targetContext, previous) }
        }
    }
    @get:Rule(order = 1) val compose = createAndroidComposeRule<ComponentActivity>()

    private val directory get() = File(compose.activity.getExternalFilesDir(null), "running-records").apply { mkdirs() }

    @Test fun recordsInTheApp() {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val month = YearMonth.from(today)
        val repo = ServiceLocator.runRecordsRepository
        val dao = ServiceLocator.database.walkSessionDao()
        val db = ServiceLocator.database.openHelper.writableDatabase
        db.execSQL("DELETE FROM walk_sessions WHERE faction = '$MARK'")
        // 이번 달 안의 날(오늘부터 이틀씩 거슬러, 1일 앞으로는 가지 않는다) — 경로 있는 셋 + 시간만 있는 하나
        val now = System.currentTimeMillis()
        fun at(daysBack: Long, time: LocalTime): Long {
            val day = maxOf(month.atDay(1), today.minusDays(daysBack))
            return minOf(LocalDateTime.of(day, time).atZone(zone).toInstant().toEpochMilli(), now - 10 * 60_000L)
        }
        val fixtures = listOf(
            Fixture(at(0, LocalTime.of(7, 12)), 3200.0, 1344, shape = 0),
            Fixture(at(2, LocalTime.of(19, 3)), 5000.0, 1950, shape = 1),
            Fixture(at(4, LocalTime.of(6, 48)), 4000.0, 1600, shape = 2),
            Fixture(at(6, LocalTime.of(8, 10)), 0.0, 1470, shape = null),
        )
        val inMonth = fixtures.filter { YearMonth.from(java.time.Instant.ofEpochMilli(it.startedAt).atZone(zone)) == month }
        val range = RecordPeriod.Month(month).range(zone)
        val before = runBlocking { repo.totals(range).first() }
        val ids = runBlocking {
            fixtures.forEach { f ->
                dao.insert(WalkSessionEntity(
                    startedAt = f.startedAt, endedAt = f.startedAt + f.seconds * 1000, steps = (f.meters / 0.762).toInt().coerceAtLeast(1),
                    durationSec = f.seconds, distanceMeters = f.meters, calories = 0.0, pointsEarned = 0.0,
                    track = f.shape?.let { RunTrack.encode(route(it).mapIndexed { i, p -> TrackPoint(p.lat, p.lng, f.startedAt + i * 10_000L) }) }.orEmpty(),
                    faction = MARK, uploadState = "REJECTED", verdict = "VOID",
                ))
            }
            ServiceLocator.database.query("SELECT id, startedAt, distanceMeters FROM walk_sessions WHERE faction = '$MARK'", null).use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getLong(0), c.getLong(1), c.getDouble(2))) }
            }
        }
        assertEquals(4, ids.size)
        // 오늘 넣은 경로 있는 3.2km 러닝 · 시간만 있는 러닝(달 초에는 날짜가 겹칠 수 있어 내용으로 고른다)
        val gpsId = ids.first { it.second == fixtures[0].startedAt }.first
        val timeOnlyId = ids.first { it.third == 0.0 }.first
        try {
            // 저장소의 이번 달 합계 = 넣기 전 + 이번 달에 넣은 것(다른 테스트가 남긴 기록이 있어도)
            val after = runBlocking { repo.totals(range).first() }
            assertEquals(before.runs + inMonth.size, after.runs)
            assertEquals(before.meters + inMonth.sumOf { it.meters }, after.meters, 0.01)
            edgeToEdge()
            compose.setContent {
                StepUpTheme(ThemeMode.DARK) { ExperienceProvider { MainScaffold(initialTab = Screen.Profile) } }
            }
            tap("profile-records")
            awaitTag("records-list")
            // 01 — 이번 달. 요약 카드는 저장소의 기간 전체 합계(불러온 한 쪽의 합이 아니다)
            compose.onNodeWithTag("records-period").assertTextContains(monthLabel(month), substring = true)
            compose.onNodeWithTag("records-distance", useUnmergedTree = true).assertTextEquals(km(after.meters))
            // 이 기기에 다른 테스트가 남긴 더 새 러닝이 위에 있을 수 있다 — 아래로 읽어 넣은 줄을 찾고 맨 위로
            scrollToRow("record-row-$gpsId")
            compose.onNodeWithTag("records-list").performScrollToIndex(0)
            shot("01-record-list")

            // 03 — 기간 시트: 고른 것은 "적용"해야 목록에 들어간다. 닫으면 그대로
            tap("records-period")
            awaitTag("period-sheet")
            tap("period-all")
            compose.onNodeWithTag("period-all").assertIsSelected()
            shot("03-period-sheet")
            back()
            awaitGone("period-sheet")
            compose.onNodeWithTag("records-period").assertTextContains(monthLabel(month), substring = true)
            tap("records-period")
            tap("period-all")
            tap("period-apply")
            awaitGone("period-sheet")
            // 02 — 전체 기간
            compose.waitUntil(10_000) {
                runCatching { compose.onNodeWithTag("records-period").assertTextContains(korean(R.string.rec_period_all), substring = true) }.isSuccess
            }
            val all = runBlocking { repo.totals(RecordPeriod.All.range(zone)).first() }
            compose.waitUntil(10_000) {
                runCatching { compose.onNodeWithTag("records-distance", useUnmergedTree = true).assertTextEquals(km(all.meters)) }.isSuccess
            }
            shot("02-all-records")

            // 06 · 04 · 05 — 통계: 전체 기간에서 들어오면 이번 달의 월간, 주간은 이번 주, 날짜 칸을 누르면 그 날의 거리
            tap("records-stats")
            awaitTag("stats-monthly")
            compose.onNodeWithTag("stats-monthly").assertIsSelected()
            compose.onNodeWithTag("stats-next").assertIsNotEnabled()
            awaitTag("stats-chart")
            shot("06-month-statistics")
            tap("stats-weekly")
            compose.onNodeWithTag("stats-weekly").assertIsSelected()
            awaitTag("stats-chart")
            shot("04-week-statistics")
            val todayBar = today.dayOfWeek.value - 1
            tap("stats-bar-$todayBar")
            awaitTag("stats-tip")
            compose.onNodeWithTag("stats-bar-$todayBar").assertIsSelected()
            shot("05-week-selected-day")
            back()
            // 통계에서 돌아오면 목록의 기간 그대로
            awaitTag("records-list")
            compose.onNodeWithTag("records-period").assertTextContains(korean(R.string.rec_period_all), substring = true)

            // 11 · 12 — 상세와 경로 확대: 목록에서 고른 그 러닝
            scrollToRow("record-row-$gpsId")
            tap("record-row-$gpsId")
            awaitTag("run-route")
            compose.onNodeWithTag("run-distance").assertTextContains("3.2", substring = true)
            shot("11-run-detail")
            tap("run-route")
            awaitTag("route-map")
            compose.onNodeWithTag("route-summary").assertTextContains("3.20", substring = true)
            shot("12-route-expanded")
            tap("route-back")
            awaitTag("run-route")
            back()
            awaitTag("records-list")

            // 13 — 거리 · 경로 없이 시간만 남은 러닝
            scrollToRow("record-row-$timeOnlyId")
            tap("record-row-$timeOnlyId")
            awaitTag("run-no-route")
            compose.onNodeWithTag("run-time").assertTextContains("24:30", substring = true)
            compose.onNodeWithTag("run-distance", useUnmergedTree = true).assertTextEquals("—")
            shot("13-no-gps-detail")

            // 14 · 16 — 삭제 확인, 못 지우면 기록을 두고 알린다(저장소 실패를 넣어 실제로 막는다)
            tap("run-delete")
            awaitTag("run-delete-sheet")
            shot("14-delete-confirm")
            tap("run-delete-cancel")
            awaitGone("run-delete-sheet")
            db.execSQL("CREATE TRIGGER fail_record_delete BEFORE DELETE ON walk_sessions WHEN OLD.faction = '$MARK' " +
                "BEGIN SELECT RAISE(ABORT, 'injected delete failure'); END")
            try {
                tap("run-delete")
                tap("run-delete-confirm")
                awaitTag("run-delete-notice")
                compose.waitUntil(10_000) {
                    compose.onAllNodesWithText(korean(R.string.run_rec_delete_failed_title), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty()
                }
                assertTrue(runBlocking { dao.observeRecord("legacy", timeOnlyId).first() } != null)
                shot("16-delete-error")
            } finally {
                db.execSQL("DROP TRIGGER IF EXISTS fail_record_delete")
            }
            // 다시 삭제(H16 의 주 버튼 → H14) → 목록으로, 그 줄과 합계에서 빠진다
            tap("run-delete-retry")
            tap("run-delete-confirm")
            awaitTag("records-list")
            awaitGone("record-row-$timeOnlyId")
            assertEquals(null, runBlocking { dao.observeRecord("legacy", timeOnlyId).first() })
            val afterDelete = runBlocking { repo.totals(RecordPeriod.All.range(zone)).first() }
            assertEquals(all.runs - 1, afterDelete.runs)
            shot("17-after-delete")
        } finally {
            db.execSQL("DROP TRIGGER IF EXISTS fail_record_delete")
            db.execSQL("DELETE FROM walk_sessions WHERE faction = '$MARK'")
        }
    }

    /** 전달본 예시(2026-09-28 기준 9월 러닝 8개)로 시안과 같은 날짜 · 숫자의 장면 — 기기에서 만들 수 없는 상태 포함 */
    @Test fun recordStates() {
        var light by mutableStateOf(false)
        var large by mutableStateOf(false)
        var narrow by mutableStateOf(false)
        edgeToEdge()
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, if (large) 1.3f else 1f)) {
                StepUpTheme(if (light) ThemeMode.LIGHT else ThemeMode.DARK) {
                    ExperienceProvider {
                        Box(Modifier.fillMaxSize()) {
                            CommerceBackdrop(Modifier.fillMaxSize())
                            Box(
                                Modifier.then(if (narrow) Modifier.requiredWidth(320.dp).fillMaxHeight() else Modifier.fillMaxSize())
                                    .align(Alignment.TopCenter).statusBarsPadding(),
                            ) { scene() }
                        }
                    }
                }
            }
        }
        val september = YearMonth.of(2026, 9)
        val rows = SAMPLE.mapIndexed { i, s ->
            RunRecordRow(i + 1L, s.startedAt, s.startedAt + s.seconds * 1000, s.seconds, s.meters, 4000, "SIGNED", hasTrack = true)
        }
        val monthTotals = totals(SAMPLE)
        val list = RecordsLoad.Ready(RecordPeriod.Month(september), monthTotals, rows, anyRecords = true, more = false)
        val routeOf: suspend (Long) -> List<GeoPoint> = { id -> route(SAMPLE[(id - 1).toInt()].shape ?: 0) }
        fun records(load: RecordsLoad, period: RecordPeriod = RecordPeriod.Month(september)): @Composable () -> Unit = {
            RecordsContent(load, period, route = routeOf, zone = SEOUL, today = TODAY)
        }

        show("s01-record-list", "records-list", records(list))
        compose.onNodeWithTag("records-distance", useUnmergedTree = true).assertTextEquals("34.2")
        compose.onNodeWithTag("records-runs", useUnmergedTree = true).assertTextEquals("8")
        show("s02-all-records", "records-list", records(list.copy(period = RecordPeriod.All), RecordPeriod.All))
        show("s03-period-sheet", "period-sheet") {
            RecordsContent(list, RecordPeriod.Month(september), route = routeOf, zone = SEOUL, today = TODAY)
            PeriodSheet(RecordPeriod.Month(september), september, YearMonth.of(2026, 7), onApply = {}, onDismiss = {})
        }
        // 이번 달부터 첫 기록의 달까지 — 아직 오지 않은 달은 목록에 없다
        compose.onNodeWithTag("period-month-2026-09").assertIsSelected()
        compose.onAllNodesWithTag("period-month-2026-10").assertCountEquals(0)
        show("s07-first-empty", "records-empty",
            records(RecordsLoad.Ready(RecordPeriod.Month(september), totals(emptyList()), emptyList(), anyRecords = false, more = false)))
        show("s08-period-empty", "records-period-empty",
            records(RecordsLoad.Ready(RecordPeriod.Month(YearMonth.of(2026, 8)), totals(emptyList()), emptyList(), anyRecords = true, more = false),
                RecordPeriod.Month(YearMonth.of(2026, 8))))
        show("s09-loading", "records-loading", records(RecordsLoad.Loading))
        compose.onAllNodesWithTag("records-distance", useUnmergedTree = true).assertCountEquals(0)
        show("s10-load-error", "records-failed", records(RecordsLoad.Failed))
        show("s10b-stale", "records-stale", records(list.copy(stale = true)))

        // 통계 — 주는 9월 21일 ~ 27일, 달은 9월(오늘 2026-09-28: 29 · 30일은 아직 오지 않아 막대가 없다)
        val marks = SAMPLE.mapIndexed { i, s -> RunMark(s.startedAt, s.meters, i + 1L) }
        val monday = LocalDate.of(2026, 9, 21)
        fun inWeek(at: Long) = java.time.Instant.ofEpochMilli(at).atZone(SEOUL).toLocalDate() in monday..monday.plusDays(6)
        val weekRuns = SAMPLE.filter { inWeek(it.startedAt) }
        val weekMarks = marks.filter { inWeek(it.startedAt) }
        val week = StatsUi(StatWindow.Week(monday), totals(weekRuns), weekBars(monday, marks, TODAY, SEOUL), canGoNext = true, thisMonth = september,
            marks = weekMarks, best = bestDay(weekMarks, SEOUL))
        val monthUi = StatsUi(StatWindow.Month(september), monthTotals, monthBars(september, marks, TODAY, SEOUL), canGoNext = false,
            thisMonth = september, marks = marks, best = bestDay(marks, SEOUL))
        show("s04-week-statistics", "stats-chart") { RecordStatsContent(week, zone = SEOUL) }
        // 가장 많이 달린 날 — 그 주의 9월 25일 5.0km
        compose.onNodeWithTag("stats-best").assertTextContains("9월 25일", substring = true)
        compose.onNodeWithTag("stats-distance").assertTextEquals("15.2")
        compose.onNodeWithTag("stats-time").assertTextContains("1시간 43분", substring = true)
        compose.onNodeWithTag("stats-pace").assertTextContains("6'49\" /km", substring = true)
        show("s05-week-selected-day", "stats-tip") { RecordStatsContent(week, initialSelection = 4, zone = SEOUL) }
        compose.onNodeWithTag("stats-tip", useUnmergedTree = true).assertTextEquals("5.0")
        // 고른 날짜의 카드 — 그 날 러닝 하나면 카드가 그 기록(H11)으로 간다
        compose.onNodeWithTag("stats-day").assertTextContains("9월 25일 금요일", substring = true)
        compose.onNodeWithTag("stats-day-card").assertHasClickAction()
        show("s06-month-statistics", "stats-chart") { RecordStatsContent(monthUi, zone = SEOUL) }
        compose.onNodeWithTag("stats-distance").assertTextEquals("34.2")
        // 29 · 30일 칸은 아직 오지 않은 날 — 누를 수 없다
        compose.onNodeWithTag("stats-bar-4").assertIsNotEnabled()
        show("s15-statistics-empty", "stats-empty") {
            RecordStatsContent(StatsUi(StatWindow.Week(LocalDate.of(2026, 9, 21)), totals(emptyList()),
                weekBars(LocalDate.of(2026, 9, 21), emptyList(), TODAY, SEOUL), canGoNext = true, thisMonth = september, anyRecords = false))
        }
        // 기록이 하나도 없으면 "—" 와 빈 축, 러닝 시작(U01)
        compose.onNodeWithTag("stats-distance").assertTextEquals("—")
        compose.onNodeWithTag("stats-start-run").assertIsDisplayed()
        show("s15b-statistics-empty-period", "stats-empty") {
            RecordStatsContent(StatsUi(StatWindow.Month(YearMonth.of(2026, 8)), totals(emptyList()),
                monthBars(YearMonth.of(2026, 8), emptyList(), TODAY, SEOUL), canGoNext = true, thisMonth = september))
        }

        // 상세 · 경로 · 삭제
        val run = SAMPLE.first()
        val session = WalkSessionEntity(id = 1, startedAt = run.startedAt, endedAt = run.startedAt + run.seconds * 1000, steps = 4200,
            durationSec = run.seconds, distanceMeters = run.meters, calories = 0.0, pointsEarned = 0.0, uploadState = "SIGNED")
        val found = RecordLookup.Found(session)
        val path = route(0)
        fun detail(delete: DeleteState, lookup: RecordLookup = found, points: List<GeoPoint>? = path): @Composable () -> Unit = {
            RunRecordContent(lookup, points, delete, zone = SEOUL)
        }
        show("s11-run-detail", "run-route", detail(DeleteState.Closed))
        compose.onNodeWithTag("run-date").assertTextEquals("2026.09.27 · 오전 7:12")
        compose.onNodeWithTag("run-pace", useUnmergedTree = true).assertTextEquals("7'00\"")
        compose.onNodeWithTag("run-speed", useUnmergedTree = true).assertTextEquals("8.6")
        show("s12-route-expanded", "route-map") { RunRouteMapContent(found, path, zone = SEOUL) }
        val timeOnly = RecordLookup.Found(session.copy(distanceMeters = 0.0))
        show("s13-no-gps-detail", "run-no-route", detail(DeleteState.Closed, timeOnly, emptyList()))
        compose.onNodeWithTag("run-time").assertTextContains("22:24", substring = true)
        show("s14-delete-confirm", "run-delete-sheet", detail(DeleteState.Confirm))
        compose.onNodeWithTag("run-delete-subject", useUnmergedTree = true).assertTextEquals("9월 27일 · 3.20km · 22분 24초")
        show("s14b-deleting", "run-delete-sheet", detail(DeleteState.Deleting))
        show("s16-delete-error", "run-delete-notice", detail(DeleteState.Failed))
        compose.onNodeWithTag("run-back-to-list").assertIsDisplayed()
        compose.onNodeWithTag("run-delete-retry").assertIsDisplayed()
        show("s16b-delete-uploading", "run-delete-notice", detail(DeleteState.Uploading))
        show("s18-record-missing", "run-missing", detail(DeleteState.Closed, RecordLookup.Missing))

        // 밝은 테마 · 큰 글씨 · 320dp
        compose.runOnIdle { light = true }
        show("s30-light-list", "records-list", records(list))
        show("s31-light-detail", "run-route", detail(DeleteState.Closed))
        compose.runOnIdle { light = false; large = true }
        show("s32-large-font-list", "records-list", records(list))
        show("s33-large-font-stats", "stats-chart") { RecordStatsContent(week, initialSelection = 4, zone = SEOUL) }
        show("s34-large-font-detail", "run-route", detail(DeleteState.Closed))
        compose.runOnIdle { large = false; narrow = true }
        show("s35-narrow-list", "records-list", records(list))
        show("s36-narrow-stats", "stats-chart") { RecordStatsContent(week, zone = SEOUL) }
        // 7열 막대가 좁은 폭에서도 칸마다 48dp 높이 이상 누를 곳을 가진다
        compose.onNodeWithTag("stats-bar-0").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected))
    }

    // ── 도우미 ─────────────────────────────────────────────────────

    private data class Fixture(val startedAt: Long, val meters: Double, val seconds: Long, val shape: Int?)

    private fun totals(runs: List<Fixture>) = RecordTotals(
        runs = runs.size,
        meters = runs.sumOf { it.meters },
        seconds = runs.sumOf { it.seconds },
        pacedMeters = runs.filter { it.meters > 0 && it.seconds > 0 }.sumOf { it.meters },
        pacedSeconds = runs.filter { it.meters > 0 && it.seconds > 0 }.sumOf { it.seconds },
        measured = runs.count { it.meters > 0 },
    )

    /** 예시 경로 — 여의도 공원 둘레의 마름모 · 네모 · 세모(실제 러닝 위치가 아니다). 출발점은 아래쪽 변 가운데 */
    private fun route(shape: Int): List<GeoPoint> {
        val center = GeoPoint(37.5262, 126.9236)
        val corners = when (shape) {
            1 -> listOf(-0.8 to -0.6, 0.8 to -0.6, 0.8 to 0.6, -0.8 to 0.6)
            2 -> listOf(0.0 to -0.9, 0.9 to 0.7, -0.9 to 0.7)
            else -> listOf(0.0 to -1.0, 1.0 to 0.0, 0.0 to 1.0, -1.0 to 0.0)
        }
        val steps = 18
        val loop = corners.indices.flatMap { i ->
            val (x0, y0) = corners[i]
            val (x1, y1) = corners[(i + 1) % corners.size]
            (0 until steps).map { k ->
                val t = k / steps.toDouble()
                // 모서리를 조금 둥글게 — 변 가운데를 살짝 밖으로
                val bulge = 1 + 0.05 * sin(PI * t)
                GeoPoint(center.lat - (y0 + (y1 - y0) * t) * bulge * 0.0045, center.lng + (x0 + (x1 - x0) * t) * bulge * 0.0058)
            }
        }
        val start = (corners.size - 2) * steps + steps / 2
        val ordered = loop.drop(start) + loop.take(start)
        return ordered + ordered.first()
    }

    private fun km(meters: Double) = String.format(Locale.ROOT, "%,.1f", meters / 1000.0)

    private fun monthLabel(month: YearMonth): String =
        java.time.format.DateTimeFormatter.ofPattern(korean(R.string.rec_month_pattern), Locale.KOREAN).format(month)

    private var scene by mutableStateOf<@Composable () -> Unit>({})

    private fun show(name: String, readyTag: String, content: @Composable () -> Unit) {
        compose.runOnIdle { scene = content }
        awaitTag(readyTag)
        shot(name)
    }

    private fun tap(tag: String) {
        awaitTag(tag)
        val node = compose.onNodeWithTag(tag)
        runCatching { node.performScrollTo() }
        node.performClick()
        compose.waitForIdle()
    }

    private fun back() {
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(android.view.KeyEvent.KEYCODE_BACK)
        compose.waitForIdle()
    }

    private fun korean(id: Int): String {
        val config = Configuration(compose.activity.resources.configuration).apply { setLocale(Locale.KOREAN) }
        return compose.activity.createConfigurationContext(config).getString(id)
    }

    private fun edgeToEdge() {
        runBlocking {
            ServiceLocator.userPrefs.setReducedMotion(true)
            ServiceLocator.userPrefs.setSounds(false)
            ServiceLocator.userPrefs.setHaptics(false)
            ServiceLocator.userPrefs.setGuideSeen()
        }
        compose.activityRule.scenario.onActivity {
            it.enableEdgeToEdge(
                statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            )
        }
    }

    /** 목록을 아래로 읽어(다음 쪽을 이어 읽으며) 그 줄까지 간다 */
    private fun scrollToRow(tag: String) {
        var found = false
        repeat(20) {
            if (!found) {
                found = runCatching { compose.onNodeWithTag("records-list").performScrollToNode(hasTestTag(tag)) }.isSuccess
                if (!found) {
                    compose.waitForIdle()
                    Thread.sleep(300)
                }
            }
        }
        assertTrue("$tag reachable", found)
    }

    private fun awaitTag(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun awaitGone(tag: String) {
        compose.waitUntil(10_000) { compose.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }
    }

    private fun shot(name: String, settle: Long = 700) {
        compose.waitForIdle()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(settle)
        captureDisplay(File(directory, "$name.png"))
    }

    private companion object {
        /** 이 테스트가 넣은 러닝만 골라 지운다 */
        const val MARK = "records-test"
        val SEOUL: ZoneId = ZoneId.of("Asia/Seoul")
        val TODAY: LocalDate = LocalDate.of(2026, 9, 28)

        private fun kst(month: Int, day: Int, hour: Int, minute: Int): Long =
            LocalDateTime.of(2026, month, day, hour, minute).atZone(SEOUL).toInstant().toEpochMilli()

        /** 전달본 sample-data.json 의 9월 러닝 8개(예시) — 최근 먼저 */
        val SAMPLE = listOf(
            Fixture(kst(9, 27, 7, 12), 3200.0, 1344, 0),
            Fixture(kst(9, 25, 19, 3), 5000.0, 1950, 1),
            Fixture(kst(9, 23, 6, 48), 4000.0, 1600, 2),
            Fixture(kst(9, 21, 19, 20), 3000.0, 1320, 0),
            Fixture(kst(9, 18, 7, 2), 6000.0, 2520, 1),
            Fixture(kst(9, 14, 6, 55), 5000.0, 2050, 2),
            Fixture(kst(9, 10, 18, 45), 4500.0, 1890, 1),
            Fixture(kst(9, 6, 8, 10), 3500.0, 1470, 0),
        )
    }
}
