package com.stepup.android.data.remote

import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSUserDefaults

/**
 * iOS 로그인 창 — Swift(iosApp 의 SignInUi.swift)가 채운다. 창 자체(Apple AuthenticationServices · 구글 GoogleSignIn SDK)만
 * 기기 코드이고, nonce · Supabase 로그인 · 세션 보관 · 토큰 철회는 Kotlin([IosAuth])이 한다.
 */
interface IosSignInUi {
    /**
     * Apple 로그인 창. [hashedNonce] 를 요청에 넣는다. 끝나면 onDone(idToken, authorizationCode, error) —
     * 사람이 닫으면 error = "cancelled".
     */
    fun apple(hashedNonce: String, onDone: (String?, String?, String?) -> Unit)

    /** 구글 로그인 창. 앱에 iOS 클라이언트 ID 가 없으면 error = "not_configured". 끝나면 onDone(idToken, error) */
    fun google(hashedNonce: String, onDone: (String?, String?) -> Unit)
}

/** iOS 로그인의 결말 — 화면이 고를 글자는 안드로이드 로그인과 같다 */
sealed interface IosSignInResult {
    data class Ok(val session: AuthSession) : IosSignInResult
    data object Cancelled : IosSignInResult
    data class NotConfigured(val provider: String) : IosSignInResult
    data class Failed(val reason: String, val retry: Boolean) : IosSignInResult
}

/**
 * iOS 로그인 · 계정 삭제 순서.
 *
 * Apple 로 로그인한 계정을 지울 때는 Apple 토큰을 먼저 철회한다(App Store 심사 5.1.1(v)) — Apple 로그인 창을 한 번 더 열어
 * 새 authorizationCode 를 받아 워커(`/v1/apple/revoke`)에 보낸다. 철회가 안 돼도 계정 삭제는 막지 않는다.
 */
object IosAuth {
    /** iosApp 이 시작할 때 넣는다 */
    var ui: IosSignInUi? = null

    private const val PROVIDER_KEY = "stepup.auth.provider"
    private const val INSTALLED_KEY = "stepup.installed"

    /** 앱이 켜질 때(iosApp) — 새로 깐 첫 실행이면 지난 설치의 키체인 로그인을 지운다(키체인은 앱을 지워도 남는다) */
    fun onLaunch() {
        val defaults = NSUserDefaults.standardUserDefaults
        if (defaults.boolForKey(INSTALLED_KEY)) return
        KeychainSessionStore().wipe()
        defaults.removeObjectForKey(PROVIDER_KEY)
        defaults.setBool(true, INSTALLED_KEY)
    }

    /** 마지막으로 로그인한 수단("apple" · "google") — 계정 삭제 때 Apple 철회가 필요한지 */
    var lastProvider: String?
        get() = NSUserDefaults.standardUserDefaults.stringForKey(PROVIDER_KEY)
        private set(value) {
            if (value == null) NSUserDefaults.standardUserDefaults.removeObjectForKey(PROVIDER_KEY)
            else NSUserDefaults.standardUserDefaults.setObject(value, PROVIDER_KEY)
        }

    suspend fun signInWithApple(auth: SupabaseAuth, store: AuthSessionStore): IosSignInResult {
        val window = ui ?: return IosSignInResult.NotConfigured("apple")
        val nonce = AuthNonce.create()
        val (idToken, _, error) = suspendCancellableCoroutine { c ->
            window.apple(nonce.hashed) { token, code, err -> if (c.isActive) c.resume(Triple(token, code, err)) }
        }
        if (error == "cancelled") return IosSignInResult.Cancelled
        if (idToken.isNullOrBlank()) return IosSignInResult.Failed(error ?: "Apple 로그인 응답이 비었습니다", retry = true)
        return finish(auth.signInWithApple(idToken, nonce.raw), store, "apple")
    }

    suspend fun signInWithGoogle(auth: SupabaseAuth, store: AuthSessionStore): IosSignInResult {
        val window = ui ?: return IosSignInResult.NotConfigured("google")
        val nonce = AuthNonce.create()
        val (idToken, error) = suspendCancellableCoroutine { c ->
            window.google(nonce.hashed) { token, err -> if (c.isActive) c.resume(token to err) }
        }
        return when {
            error == "cancelled" -> IosSignInResult.Cancelled
            error == "not_configured" -> IosSignInResult.NotConfigured("google")
            idToken.isNullOrBlank() -> IosSignInResult.Failed(error ?: "구글 로그인 응답이 비었습니다", retry = true)
            else -> finish(auth.signInWithGoogle(idToken, nonce.raw), store, "google")
        }
    }

    private suspend fun finish(result: AuthResult, store: AuthSessionStore, provider: String): IosSignInResult = when (result) {
        is AuthResult.Ok -> {
            store.save(result.session)
            lastProvider = provider
            IosSignInResult.Ok(result.session)
        }
        is AuthResult.Rejected -> IosSignInResult.Failed(result.reason, retry = false)
        is AuthResult.Retry -> IosSignInResult.Failed(result.reason, retry = true)
    }

    /**
     * 계정 삭제 — Apple 계정이면 철회를 먼저 시도한 뒤 서버에서 지운다. 지워졌으면 이 기기의 세션 · 표시도 지운다.
     * [attesterUrl] 이 비었으면 철회를 건너뛴다.
     */
    suspend fun deleteAccount(server: StepUpServer, sessions: SessionHolder, store: AuthSessionStore, attesterUrl: String): AccountDeletion {
        if (lastProvider == "apple") revokeApple(server, sessions, attesterUrl)
        val deleted = server.deleteAccount()
        if (deleted == AccountDeletion.Deleted) signOut(store)
        return deleted
    }

    suspend fun signOut(store: AuthSessionStore) {
        store.clear()
        lastProvider = null
    }

    /** Apple 토큰 철회 — 창을 다시 열어 새 code 를 받는다(5분 · 한 번). 성공 여부만 돌려준다 */
    private suspend fun revokeApple(server: StepUpServer, sessions: SessionHolder, attesterUrl: String): Boolean {
        val window = ui ?: return false
        if (attesterUrl.isBlank()) return false
        val token = (sessions.accessToken() as? TokenResult.Ok)?.accessToken ?: return false
        val code = suspendCancellableCoroutine<String?> { c ->
            window.apple(AuthNonce.create().hashed) { _, authCode, _ -> if (c.isActive) c.resume(authCode) }
        } ?: return false
        val response = server.http.post(
            "${attesterUrl.trimEnd('/')}/v1/apple/revoke",
            """{"code":${code.asJsonString()}}""",
            mapOf("Content-Type" to "application/json", "Authorization" to "Bearer $token"),
        )
        return response.status in 200..299 && response.body.contains("\"ok\":true")
    }
}
