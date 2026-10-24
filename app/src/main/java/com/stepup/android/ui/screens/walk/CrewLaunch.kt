package com.stepup.android.ui.screens.walk

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.LocalMotion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first

/*
 * 크루 출발 연출(2026-10-24 핸드오프 장면 35 · 36, 04_DESIGN_BACKGROUND 7절) — 자유 러닝의 승인된 3 · 2 · 1 · 0 → StepUP! → 가운데 펼침
 * (FreeRunStartStage)을 서버가 정한 출발 시각에 맞춰 그대로 쓴다(COUNTDOWN_EXISTING — 터널을 다시 깔지 않는다).
 *
 * - 서버 출발 시각(startsAtLocal)이 펼침 시작(REVEAL_AT)이 되게 시계를 맞춘다. 출발까지 3.76초보다 많이 남았으면 "출발 신호를 맞추는 중"(35).
 * - 앱 맨 위 겹(StepUpRoot)이 그린다 — 대기실에서 러닝 화면으로 길을 옮겨도 숫자 · 로고가 끊기지 않고, 러닝이 실제로 켜진 뒤 그 화면을
 *   가운데에서 펼쳐 보인다(빈 프레임 · 다시 그리기 없음). 연출은 출발을 다시 요청하지 않는다 — 출발은 서버가 이미 정했다.
 * - 출발 시각이 지나 5초 안에 러닝이 켜지지 않으면(권한 · 거절) 거둔다. 출발이 정해진 뒤에는 뒤로 가기로 끊지 않는다.
 */
object CrewLaunch {
    /** 한 번의 출발 — 방 번호 · 이 폰 시계로 고친 출발 시각 */
    data class Launch(val partyId: Long, val startsAtLocal: Long)

    private val _state = MutableStateFlow<Launch?>(null)
    val state: StateFlow<Launch?> = _state

    /** 대기실이 카운트다운을 받았다 — 같은 출발이면 그대로(다시 시작하지 않는다) */
    fun begin(partyId: Long, startsAtLocal: Long) {
        val now = _state.value
        if (now?.partyId == partyId && now.startsAtLocal == startsAtLocal) return
        _state.value = Launch(partyId, startsAtLocal)
    }

    /** 연출을 거둔다 — [partyId] 를 주면 그 출발일 때만 */
    fun end(partyId: Long? = null) {
        if (partyId == null || _state.value?.partyId == partyId) _state.value = null
    }
}

/** 연출 시계(ms, 숫자 3 이 보인 때부터) — 서버 출발 시각이 펼침 시작(REVEAL_AT)이 되게. 음수면 아직 3 전 */
internal fun crewLaunchClock(startsAtLocal: Long, nowMs: Long): Long = FreeRunStartMotion.REVEAL_AT - (startsAtLocal - nowMs)

/** 앱 맨 위 겹 — 크루 출발이 정해진 동안만 */
@Composable
fun CrewLaunchOverlay() {
    val launch by CrewLaunch.state.collectAsStateWithLifecycle()
    val current = launch ?: return
    key(current) { CrewLaunchSequence(current) }
}

@Composable
private fun CrewLaunchSequence(launch: CrewLaunch.Launch) {
    val motion = LocalMotion.current
    // 시작할 때의 모션 설정으로 끝까지 — 도중에 바뀌어도 한 흐름이다
    val reduced = remember { motion.reduced }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var revealAt by remember { mutableLongStateOf(-1L) }
    LaunchedEffect(launch) {
        while (true) {
            withFrameNanos { }
            now = System.currentTimeMillis()
            val clock = crewLaunchClock(launch.startsAtLocal, now)
            when {
                // 출발 시각 — 러닝이 실제로 켜진 것을 본 뒤에 펼친다
                revealAt < 0 -> when {
                    clock >= FreeRunStartMotion.REVEAL_AT && WalkSessionService.state.value.isActive -> revealAt = clock
                    clock >= FreeRunStartMotion.REVEAL_AT + FreeRunStartMotion.START_TIMEOUT_MS -> {
                        CrewLaunch.end(launch.partyId)
                        return@LaunchedEffect
                    }
                }
                clock - revealAt >= FreeRunStartMotion.revealDuration(reduced) -> {
                    CrewLaunch.end(launch.partyId)
                    return@LaunchedEffect
                }
            }
        }
    }
    // 출발이 정해진 뒤 — 뒤로 가기로 끊지 않는다(서버는 이미 출발 명단 · 기여를 적었다)
    BackHandler { }
    // 펼칠 러닝 화면의 지도 타일을 미리 받는다(지금 자리 둘레) — 펼친 지도가 바로 그려지게
    val here = rememberCurrentLocation(enabled = revealAt < 0)
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val window = LocalWindowInfo.current.containerSize
    val latestHere by rememberUpdatedState(here)
    LaunchedEffect(launch) {
        val center = snapshotFlow { latestHere }.filterNotNull().first()
        runCatching { prefetchRunMapTiles(context, center, window.width, (window.height * 0.7f).toInt(), density) }
    }
    val clock = crewLaunchClock(launch.startsAtLocal, now)
    if (clock < 0) {
        CrewStartingStage()
        return
    }
    Box(Modifier.fillMaxSize().testTag("party-countdown")) {
        FreeRunStartStage(
            clock = { crewLaunchClock(launch.startsAtLocal, now) },
            revealSince = { if (revealAt >= 0) crewLaunchClock(launch.startsAtLocal, now) - revealAt else null },
            reduced = reduced,
        )
    }
}

/** 출발 신호를 맞추는 중(35) — 숫자 3 전. 카운트다운과 같은 바탕(남색 · 가운데 빛)에 짧은 글 · 도는 표시 */
@Composable
internal fun CrewStartingStage(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize()
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .testTag("party-starting-stage"),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(StartNavy)
            val c = Offset(size.width / 2f, size.height * 0.48f)
            val r = size.height * 0.46f
            drawCircle(Brush.radialGradient(0f to StartGlow, 0.8f to StartGlow.copy(alpha = 0f), center = c, radius = r), radius = r, center = c)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
            RunSpinner(Modifier.size(36.dp))
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.crew5_starting), style = navyText(18.sp, LobbyInk.Text, FontWeight.Bold),
                textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
