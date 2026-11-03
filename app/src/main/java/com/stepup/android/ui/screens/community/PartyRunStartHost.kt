package com.stepup.android.ui.screens.community

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.PartyPhase
import com.stepup.android.service.StartGroup
import com.stepup.android.service.WalkSessionService
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.screens.walk.CrewLaunch

/**
 * 모임(크루 · 번개) 러닝의 출발 — 대기실 화면이 아니라 앱 화면(MainScaffold)이 늘 맡는다(2026-11-03).
 *
 * 예전에는 대기실 화면이 떠 있을 때만 러닝을 켰다. 준비를 마친 사람이 크루 채팅에 가 있거나 화면이 꺼져 있으면
 * 서버(party_start)는 출발 명단 · 오늘 한 번을 이미 적었는데 이 폰의 러닝은 켜지지 않아 오늘 크루 러닝을 잃었다.
 * 이제 방 상태(CrewRepository.party)를 보고 어느 화면에 있든 출발 연출(CrewLaunch)과 러닝을 켠다.
 * 앱이 뒤에 있는 동안은 보지 않는다(뒤에서는 러닝 서비스를 켤 수 없다) — 돌아왔을 때 출발한 지 2분 안이면 켠다(startIsFresh).
 * 준비를 마치고 출발을 기다리는 동안은 화면이 꺼지지 않게 둔다.
 *
 * @param onLobby 대기실 화면이 맨 위에 있다 — 출발을 놓쳤을 때 새 대기실을 여는 것은 대기실을 보고 있을 때만
 * @param onRunStarted 러닝이 켜졌다(또는 이미 돌고 있다) — 러닝 화면으로
 */
@Composable
fun PartyRunStartHost(onLobby: Boolean, onRunStarted: () -> Unit) {
    val context = LocalContext.current
    val repo = ServiceLocator.crewRepository
    val party by repo.party.collectAsStateWithLifecycle()
    val goRun by rememberUpdatedState(onRunStarted)
    val needPrecise = stringResource(R.string.run_gps_need_precise)

    // 출발한 러닝에 이 방(크루 · 번개)과 이름을 적어 둔다 — 결과 · 공유 · 되살린 러닝이 같은 모임으로 보이게(정산과 무관)
    val startRun: () -> Unit = {
        val room = repo.party.value
        val title = room.crewName.ifBlank {
            room.flashPostId?.let { id -> ServiceLocator.communityRepository.posts.value.firstOrNull { it.id == id }?.title }.orEmpty()
        }
        WalkSessionService.start(context, room.partySize, StartGroup.of(room, title))
        goRun()
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        if (repo.party.value.phase == PartyPhase.RUNNING && StepPermissions.hasActivityRecognition(context)) {
            when {
                WalkSessionService.state.value.isActive -> goRun()
                // 위치 없이는 출발하지 않는다 — 준비한 뒤 권한을 거둔 경우. 왜 출발하지 않는지 알린다
                !StepPermissions.hasPreciseLocation(context) ->
                    android.widget.Toast.makeText(context, needPrecise, android.widget.Toast.LENGTH_LONG).show()
                // 권한 창에 오래 머물렀다 — 출발한 지 오래된 방의 러닝을 늦게 켜지 않는다(아래 출발과 같은 판단)
                !repo.startIsFresh() -> repo.missedStart()
                else -> startRun()
            }
        }
    }

    // 크루 대기실의 출발 연출 — 서버 출발 시각에 맞춘 3 · 2 · 1 · 0 · StepUP! · 펼침(앱 맨 위 겹 CrewLaunchOverlay 가 그린다)
    val crewRoom = party.flashPostId == null && !party.crewId.isNullOrBlank()
    LaunchedEffect(crewRoom, party.phase, party.partyId, party.startsAtLocal) {
        val id = party.partyId
        if (!crewRoom || id == null) return@LaunchedEffect
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
        when {
            // 이 폰의 러닝이 이미 돌고 있다 — 하나 더 켜지 않고 그 러닝으로 간다
            WalkSessionService.state.value.isActive -> {
                PartyRunStart.mark(id)
                goRun()
            }
            // 출발한 지 오래됐다(앱을 떠나 있었다) — 옛 방의 러닝을 늦게 켜지 않는다. 새 대기실은 대기실을 보고 있을 때만 연다
            // (다른 화면에서 열면 보이지 않는 방에 앱이 들어가 있게 된다)
            !repo.startIsFresh() -> if (onLobby) repo.missedStart()
            else -> {
                PartyRunStart.mark(id)
                val missing = partyRunPermissions(context)
                if (missing.isEmpty()) startRun() else permissionLauncher.launch(missing)
            }
        }
    }

    // 준비를 마치고 출발을 기다리는 동안 화면을 켜 둔다 — 화면이 꺼지면 출발 때 이 폰의 러닝을 켤 수 없다
    val waiting = party.partyId != null && party.myReady &&
        (party.phase == PartyPhase.LOBBY || party.phase == PartyPhase.COUNTDOWN)
    val view = LocalView.current
    DisposableEffect(view, waiting) {
        if (waiting) view.keepScreenOn = true
        onDispose { if (waiting) view.keepScreenOn = false }
    }
}

/** 이미 러닝을 켠(러닝 화면으로 옮긴) 방 — 앱이 살아 있는 동안 */
private object PartyRunStart {
    @Volatile private var last: Long? = null
    fun handled(partyId: Long): Boolean = last == partyId
    fun mark(partyId: Long) {
        last = partyId
    }
}

/** 준비 · 출발에 필요한 권한 — 걸음 · 알림 · 위치. 대략적인 위치만 있으면 정확한 위치를 다시 묻는다(Android 12+ 는 바꾸기 창) */
internal fun partyRunPermissions(context: Context): Array<String> {
    val missing = StepPermissions.missing(context)
    if (StepPermissions.hasPreciseLocation(context) || android.Manifest.permission.ACCESS_FINE_LOCATION in missing) return missing
    return missing + arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION)
}
