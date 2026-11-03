package com.stepup.android.data.remote

/**
 * 로그인 일회용 값 한 쌍(구글 · Apple id_token 로그인).
 *
 * 로그인 창(구글 · Apple)에는 **해시**를, 서버(Supabase)에는 **원본**을 준다. 서버가 원본을 해시해 토큰 안의 값과 맞춰 보면
 * 그 토큰이 이 요청에 대한 답인지 확인된다. 안드로이드 GoogleSignIn 의 예전 방식 그대로(32바이트 → 소문자 16진수, SHA-256 16진수).
 */
data class AuthNonce(val raw: String, val hashed: String) {
    companion object {
        fun create(): AuthNonce {
            val raw = secureRandomBytes(32).toHex()
            return AuthNonce(raw, sha256(raw.encodeToByteArray()).toHex())
        }
    }
}

fun ByteArray.toHex(): String = joinToString("") { (it.toInt() and 0xff).toString(16).padStart(2, '0') }

/** 암호학적으로 안전한 난수 — 안드로이드 SecureRandom, iOS SecRandomCopyBytes */
expect fun secureRandomBytes(size: Int): ByteArray

/** SHA-256 — 안드로이드 MessageDigest, iOS CommonCrypto */
expect fun sha256(data: ByteArray): ByteArray
