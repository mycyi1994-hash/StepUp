package com.stepup.android.data.remote

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** id_token 로그인(구글 · Apple)의 요청 모양과 일회용 값 — 안드로이드 · iOS 둘 다에서 돈다 */
class AuthIdTokenTest {
    private class Recorder : HttpPoster {
        val bodies = mutableListOf<Pair<String, String>>()
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            bodies += url to body
            return HttpResponse(400, """{"msg":"no"}""")
        }
        override suspend fun get(url: String, headers: Map<String, String>) = HttpResponse(500, "")
    }

    @Test fun appleAndGoogleBodies() = runTest {
        val http = Recorder()
        val auth = SupabaseAuth("https://x.supabase.co", "anon", http)
        auth.signInWithApple("aTok\"en", "raw1")
        auth.signInWithGoogle("gTok", "raw2")
        auth.signInWithGoogle("gTok")
        assertEquals("https://x.supabase.co/auth/v1/token?grant_type=id_token", http.bodies[0].first)
        assertEquals("""{"provider":"apple","id_token":"aTok\"en","nonce":"raw1"}""", http.bodies[0].second)
        assertEquals("""{"provider":"google","id_token":"gTok","nonce":"raw2"}""", http.bodies[1].second)
        assertEquals("""{"provider":"google","id_token":"gTok"}""", http.bodies[2].second)
    }

    @Test fun nonceIsHexAndHashed() {
        val a = AuthNonce.create()
        val b = AuthNonce.create()
        assertEquals(64, a.raw.length)
        assertTrue(a.raw.all { it in '0'..'9' || it in 'a'..'f' })
        assertEquals(sha256(a.raw.encodeToByteArray()).toHex(), a.hashed)
        assertNotEquals(a.raw, b.raw)
        // SHA-256("abc") — 표준 시험 값
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256("abc".encodeToByteArray()).toHex())
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256(ByteArray(0)).toHex())
    }
}
