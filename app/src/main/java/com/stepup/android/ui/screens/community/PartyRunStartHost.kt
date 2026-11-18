package com.stepup.android.ui.screens.community

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.data.repo.PartyProblem
import com.stepup.android.service.RunGroup
import com.stepup.android.service.StartGroup
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.screens.walk.CrewLaunch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 모임(크루 · 번개) 러닝의 출발 — 대기실 화면이 아니라 앱 화면(MainScaffold)이 늘 맡는다(2026-11-03).
 *
 * 예전에는 대기실 화면이 떠 있을 때만 러닝을 켰다. 준비를 마친 사람이 크루 채팅에 가 있거나 화면이 꺼져 있으면
 * 서버(party_start)는 출발 명단 · 오늘 한 번을 이미 적었는데 이 폰의 러닝은 켜지지 않아 오늘 크루 러닝을 잃었다.
 * 이제 방 상태(CrewRepository.party)를 보고 어느 화면에 있든 출발 연출(CrewLaunch)과 러닝을 켠다.
 * 앱이 뒤에 있는 동안은 보지 않는다(뒤에서는 러닝 서비스를 켤 수 없다) — 돌아왔을 때 출발한 지 2분 안이면 켠다(startIsFresh).
 * 대기실에서 출발을 기다리는 동안(준비한 사람 · 진행자)은 화면이 꺼지지 않게 둔다.
 *
 * 러닝이 실제로 켜진 것을 본 뒤에만 러닝 화면으로 간다(2026-11-18 점검). 이 폰이 러닝을 켜지 못했으면(과속으로 멈춘 뒤 10분 ·
 * 끝나지 않은 러닝 · 걸음 · 정확한 위치 권한 · 시작 거절 · 따로 달리는 중) 그 방에서 나오고 달리지 않은 오늘 크루 러닝을 돌려받는다
 * ([com.stepup.android.data.repo.CrewRepository.missedStart]) — 예전에는 방을 "처리함"으로 적은 채 러닝 화면으로 가서, 서버에는
 * 오늘 크루 러닝을 한 사람으로 남고 앱은 그 방을 "달리는 중"으로 쥐고 있었다. 2분 넘게 지나 돌아온 출발도 어느 화면에서든 같은 길로 정리한다.
 *
 * @param onLobby 대기실 화면이 맨 위에 있다 — 놓친 출발 · 켜지 못한 출발은 대기실이 화면으로 알린다. 다른 화면이면 한 줄로 알린다
 * @param onRunStarted 러닝이 켜졌다(또는 이 모임의 러닝이 이미 돌고 있다) — 러닝 화면으로
 */
@Composable
fun PartyRunStartHost(onLobby: Boolean, onRunStarted: () -> Unit) {
    val context = LocalContext.current
    val repo = ServiceLocator.crewRepository
    val party by repo.party.collectAsStateWithLifecycle()
    val goRun by rememberUpdatedState(onRunStarted)
    val lobbyOnTop by rememberUpdatedState(onLobby)
    val scope = rememberCoroutineScope()
    val startFailed = stringResource(R.string.run_start_failed)
    val missedToast = stringResource(R.string.tlf_missed_toast)
    val blockedToast = stringResource(R.string.tlf_start_blocked_toast)
    val leftBehindToast = stringResource(R.string.tlf_left_behind_toast)

    // 함께 출발하지 못했다 — 그 방에서 나오고(크루 방이면 달리지 않은 오늘 크루 러닝을 돌려받는다) 알린다.
    // 대기실을 보고 있으면 그 화면이 까닭 · 돌려받았는지를 보인다. 다른 화면이면 한 줄로 알린다
    val giveUp: (Boolean) -> Unit = { blocked ->
        repo.missedStart(blocked = blocked)
        if (!lobbyOnTop) Toast.makeText(context, if (blocked) blockedToast else missedToast, Toast.LENGTH_LONG).show()
    }

    // 출발한 러닝에 이 방(크루 · 번개)과 이름을 적어 둔다 — 결과 · 공유 · 되살린 러닝이 같은 모임으로 보이게(정산과 무관).
    // 러닝이 켜진 것을 본 뒤에 러닝 화면으로 간다. 서비스가 거절했으면(시작 거절 횟수가 늘었다) 가지 않고 방에서 나온다
    val startRun: (Long) -> Unit = { id ->
        val room = repo.party.value
        val title = room.crewName.ifBlank {
            room.flashPostId?.let { post -> ServiceLocator.communityRepository.posts.value.firstOrNull { it.id == post }?.title }.orEmpty()
        }
        val refusals = WalkSessionService.startRefusals.value
        WalkSessionService.start(context, room.partySize, StartGroup.of(room, title))
        scope.launch {
            val started = withTimeoutOrNull(START_WAIT_MS) {
                combine(WalkSessionService.state, WalkSessionService.startRefusals) { run, refused ->
                    when {
                        run.isActive -> true
                        refused != refusals -> false
                        else -> null
                    }
                }.filterNotNull().first()
            } ?: WalkSessionService.state.value.isActive
            when {
                started -> goRun()
                // 그사이 방을 떠났으면(다른 방 · 나가기) 그 방의 일이 아니다
                repo.party.value.partyId == id -> giveUp(true)
                else -> Unit
            }
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        val room = repo.party.value
        val id = room.partyId
        if (room.phase == PartyPhase.RUNNING && id != null) {
            val run = WalkSessionService.state.value
            when {
                run.isActive && run.group != RunGroup.NONE -> goRun()
                // 걸음 · 정확한 위치 없이는 출발하지 않는다(준비한 뒤 권한을 거둔 경우) — 왜 출발하지 않는지 알리고 방에서 나온다
                !StepPermissions.hasActivityRecognition(context) || !StepPermissions.hasPreciseLocation(context) -> {
                    Toast.makeText(context, startFailed, Toast.LENGTH_LONG).show()
                    giveUp(true)
                }
                // 권한 창에 오래 머물렀다 — 출발한 지 오래된 방의 러닝을 늦게 켜지 않는다(아래 출발과 같은 판단)
                !repo.startIsFresh() -> giveUp(false)
                else -> startRun(id)
            }
        }
    }

    // 크루 대기실의 출발 연출 — 서버 출발 시각에 맞춘 3 · 2 · 1 · 0 · StepUP! · 펼침(앱 맨 위 겹 CrewLaunchOverlay 가 그린다)
    val crewRoom = party.isCrewRoom
    LaunchedEffect(crewRoom, party.phase, party.partyId, party.startsAtLocal) {
        val id = party.partyId
        if (id == null) {
            // 방에서 나왔다(나가기 · 놓친 출발 · 켜지 못한 출발) — 돌던 출발 연출을 거둔다(나온 사람에게 3 · 2 · 1 이 이어지지 않게)
            CrewLaunch.end()
            return@LaunchedEffect
        }
        if (!crewRoom) return@LaunchedEffect
        when (party.phase) {
            PartyPhase.COUNTDOWN -> if (party.startsAtLocal > 0) CrewLaunch.begin(id, party.startsAtLocal)
            PartyPhase.LOBBY, PartyPhase.IDLE -> CrewLaunch.end(id)
            else -> Unit
        }
    }

    // 서버가 정한 출발 시각이 되면 실제 세션을 시작한다 — 한 방에 한 번(화면을 다시 그려도 다시 켜거나 다시 옮기지 않게)
    LaunchedEffect(party.phase, party.partyId, onLobby) {
        val id = party.partyId ?: return@LaunchedEffect
        if (party.phase != PartyPhase.RUNNING || PartyRunStart.handled(id)) return@LaunchedEffect
        val run = WalkSessionService.state.value
        when {
            // 이 모임의 러닝이 이미 돌고 있다(되살린 러닝 등) — 하나 더 켜지 않고 그 러닝으로 간다
            run.isActive && run.group != RunGroup.NONE -> {
                PartyRunStart.mark(id)
                goRun()
            }
            // 따로 켠 혼자 러닝이 돌고 있다 — 이 출발은 함께할 수 없다. 방에서 나오고 오늘 크루 러닝을 돌려받는다(혼자 러닝은 그대로)
            run.isActive -> {
                PartyRunStart.mark(id)
                giveUp(true)
            }
            // 출발한 지 오래됐다(앱을 떠나 있었다) — 옛 방의 러닝을 늦게 켜지 않는다. 어느 화면에 있든 그 방에서 나오고 돌려받는다
            // (예전에는 대기실을 보고 있을 때만 정리해, 다른 화면에 있으면 방을 "달리는 중"으로 쥔 채 남았다)
            !repo.startIsFresh() -> giveUp(false)
            else -> {
                PartyRunStart.mark(id)
                val missing = partyStartPermissions(context)
                if (missing.isEmpty()) startRun(id) else permissionLauncher.launch(missing)
            }
        }
    }

    // 나를 빼고 출발했다(준비 전이었거나 대기실을 보고 있지 않았다) — 대기실 밖(채팅 등)에 있으면 한 줄로 알린다. 대기실이면 그 화면이 알린다
    LaunchedEffect(party.problem, party.crewId, party.flashPostId) {
        if (party.problem == PartyProblem.LEFT_BEHIND && !lobbyOnTop) {
            Toast.makeText(context, leftBehindToast, Toast.LENGTH_LONG).show()
        }
    }

    // 대기실에서 출발을 기다리는 동안 화면을 켜 둔다 — 화면이 꺼지면 출발 때 이 폰의 러닝을 켤 수 없고, 15초 넘게 대기실을 묻지 않은
    // 준비 인원은 출발에서 빠진다. 진행자는 준비 전에도 켜 둔다(화면이 꺼지고 30초 뒤 진행자가 말없이 다른 사람에게 넘어갔다 — 2026-11-18).
    // 방을 더 묻지 않게 된 뒤(내보냄 · 닫힘 등)의 굳은 "준비 완료"로는 켜 두지 않는다
    val waiting = party.inRoom && (party.myReady || party.isHost) &&
        (party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.COUNTDOWN)
    val view = LocalView.current
    DisposableEffect(view, waiting) {
        if (waiting) view.keepScreenOn = true
        onDispose { if (waiting) view.keepScreenOn = false }
    }
}

/** 러닝 서비스가 켜졌는지(거절했는지) 기다리는 시간 — 저장본 · 설정을 읽고 켜는 데 보통 1초 안. 넘으면 지금 상태로 정한다 */
private const val START_WAIT_MS = 10_000L

/** 이미 러닝을 켠(켜려 한) 방 — 앱이 살아 있는 동안 */
private object PartyRunStart {
    @Volatile private var last: Long? = null
    fun handled(partyId: Long): Boolean = last == partyId
    fun mark(partyId: Long) {
        last = partyId
    }
}

/**
 * 준비할 때 묻는 권한 — 걸음 · 알림 · 위치. 대략적인 위치만 있으면 정확한 위치를 다시 묻는다(Android 12+ 는 바꾸기 창).
 * 알림은 준비에 꼭 필요하지 않다 — 거절해도 준비한다(대기실 화면이 정한다).
 */
internal fun partyRunPermissions(context: Context): Array<String> = withPrecise(context, StepPermissions.missing(context))

/**
 * 출발 순간에 꼭 있어야 하는 권한 — 걸음 · 정확한 위치. 알림 권한은 여기서 묻지 않는다: 출발 순간에 권한 창이 뜨면 답할 때까지
 * 이 폰의 러닝이 늦게 켜졌다(2026-11-18 점검 — 알림은 준비할 때 묻는다).
 */
internal fun partyStartPermissions(context: Context): Array<String> = withPrecise(context, StepPermissions.missingForStart(context))

private fun withPrecise(context: Context, missing: Array<String>): Array<String> {
    if (StepPermissions.hasPreciseLocation(context) || android.Manifest.permission.ACCESS_FINE_LOCATION in missing) return missing
    return missing + arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
}
