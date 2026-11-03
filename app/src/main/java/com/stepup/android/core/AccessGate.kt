package com.stepup.android.core

import android.content.Context
import com.stepup.android.data.remote.AccessStatusRow
import com.stepup.android.data.remote.ServerResult
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 접속 코드 문(서버 0079) — 앱이 지금 막혀 있는가를 한곳에서 안다.
 *
 * 서버가 정본이다. 앱은 막힌 사람에게 코드 입력 화면을 보일 뿐이고, 권한 없는 요청은 서버가 403 으로 막는다.
 * 마지막으로 받은 상태를 폰에 적어 둔다 — 연결 없이 켜도 이미 들어온 사람을 다시 막지 않게.
 * 한 번도 받은 적이 없고 연결도 안 되면 막지 않는다(열린 문에서 연결이 끊긴 사람을 막지 않게 — 막혀 있어도 서버가 막는다).
 */
object AccessGate {
    private const val FILE = "stepup_access"

    private val _state = MutableStateFlow<AccessStatusRow?>(null)

    /** 마지막으로 안 상태 — null 이면 아직 모른다 */
    val state: StateFlow<AccessStatusRow?> = _state.asStateFlow()

    private val _checked = MutableStateFlow(false)

    /** 지금 계정의 상태를 서버에서 받았는가 — 로그인 직후 첫 답을 기다리는 동안은 false(코드 창 대신 불러오는 중을 보인다) */
    val checked: StateFlow<Boolean> = _checked.asStateFlow()

    private val _failed = MutableStateFlow(false)

    /** 로그인한 채로 마지막에 물었을 때 답을 못 받았는가 — 코드 창의 "다시 확인" */
    val failed: StateFlow<Boolean> = _failed.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile
    private var lastServerAsk = 0L

    fun load(context: Context) {
        val p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val mode = p.getString("mode", null) ?: return
        _state.value = AccessStatusRow(mode = mode, granted = p.getBoolean("granted", true))
        // 예전 앱은 "checked" 를 적지 않았다 — 권한 값이 남아 있으면 받은 적이 있는 것으로 본다
        _checked.value = p.getBoolean("checked", p.contains("granted"))
    }

    /** 다시 묻는다 — 로그인했으면 내 상태, 아니면 문만. 못 받으면 아는 값을 그대로 둔다 */
    suspend fun refresh(context: Context): AccessStatusRow? {
        val api = ServiceLocator.accessApi
        if (!api.isConfigured) {
            // 서버 없는 빌드 — 기다릴 답이 없다
            _checked.value = true
            return _state.value
        }
        var signedIn = false
        val fresh = try {
            signedIn = ServiceLocator.sessionHolder.isSignedIn()
            if (signedIn) {
                (api.status() as? ServerResult.Ok)?.value
            } else {
                (api.mode() as? ServerResult.Ok)?.value?.let { AccessStatusRow(mode = it, granted = it == "OPEN") }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        }
        // 묻는 사이에 로그인했으면 로그인 전 답(문만)은 버린다 — 로그인 뒤 답을 기다리게 둔다
        if (!signedIn && runCatching { ServiceLocator.sessionHolder.isSignedIn() }.getOrDefault(false)) return _state.value
        if (signedIn) _failed.value = fresh == null
        if (fresh != null) {
            val before = _state.value
            _state.value = fresh
            // 로그인 안 한 답(문만)은 계정의 답이 아니다 — 로그인하면 다시 기다린다
            _checked.value = signedIn
            context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("mode", fresh.mode).putBoolean("granted", fresh.granted).putBoolean("checked", signedIn).apply()
            // 막혀 있다가 열렸다(코드를 받음 · 문이 열림) — 막혀 있는 동안 403 으로 못 한 로그인 뒤 일을 다시 한다
            if (signedIn && before?.blocked == true && !fresh.blocked) afterGranted(context.applicationContext)
        }
        return _state.value
    }

    /** 로그인 화면이 로그인 직후 하는 일과 같다 — 푸시 주소 · 잔고 · 신발 · 남은 러닝 올리기 */
    private fun afterGranted(context: Context) {
        runCatching { ServiceLocator.pushRegistrar.syncInBackground() }
        runCatching { ServiceLocator.refreshEconomyInBackground() }
        runCatching { com.stepup.android.sync.SessionUploadWorker.schedule(context) }
    }

    /**
     * 서버가 요청을 403 access_required 로 막았다 — 권한을 다시 묻는다(코드를 넣은 뒤 사본이 늦었을 수 있다).
     * 30초에 한 번만, 기다리지 않고.
     */
    fun onServerBlocked(context: Context) {
        val now = System.currentTimeMillis()
        if (now - lastServerAsk < SERVER_ASK_GAP_MS) return
        lastServerAsk = now
        val app = context.applicationContext
        scope.launch { runCatching { refresh(app) } }
    }

    private const val SERVER_ASK_GAP_MS = 30_000L

    /** 로그아웃 · 다른 계정 — 앞 계정의 권한을 들고 있지 않게 */
    fun forget(context: Context) {
        _state.value = _state.value?.let { AccessStatusRow(mode = it.mode, granted = it.mode == "OPEN") }
        _checked.value = false
        _failed.value = false
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().remove("granted").remove("checked").apply()
    }
}

/** 막혀 있는가 — 문이 닫혔고 권한이 없다 */
val AccessStatusRow.blocked: Boolean get() = mode != "OPEN" && !granted
