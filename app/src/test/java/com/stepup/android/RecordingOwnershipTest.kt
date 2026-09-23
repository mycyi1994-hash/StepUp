package com.stepup.android

import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.*
import com.stepup.android.data.repo.ServerSessionRecorder
import com.stepup.android.domain.RecordingOwner
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class RecordingOwnershipTest {
    private fun login(id: String) = AuthSession(
        accessToken = "token-$id", refreshToken = "refresh-$id", expiresIn = 3600,
        expiresAt = 9_999_999_999L, user = AuthUser(id = id, isAnonymous = false),
    )
    private class Store(var value: AuthSession?) : AuthSessionStore {
        override suspend fun load() = value
        override suspend fun save(session: AuthSession) { value = session }
        override suspend fun clear() { value = null }
    }
    private class Http : HttpPoster {
        val requests = mutableListOf<Pair<String, String?>>()
        var onRecord: () -> Unit = {}
        var courseResponse = HttpResponse(200, "[]")
        override suspend fun post(url: String, body: String, headers: Map<String, String>): HttpResponse {
            requests += url.substringAfterLast('/') to headers["Authorization"]
            return when (url.substringAfterLast('/')) {
                "record_session" -> {
                    onRecord()
                    HttpResponse(200, """[{"session_id":7,"verdict":"CLEAN","points_awarded":12.0,"balance":12.0}]""")
                }
                "course_run_submit" -> courseResponse
                else -> HttpResponse(200, "null")
            }
        }
        override suspend fun get(url: String, headers: Map<String, String>) = post(url, "", headers)
    }
    private fun holder(store: Store, http: Http) = SessionHolder(
        SupabaseAuth("https://test.supabase.co", "test", http), store, now = { 1000L },
    )
    private fun server(holder: SessionHolder, http: Http) =
        StepUpServer("https://test.supabase.co", "test", holder, http)
    private fun run(owner: String, crew: String = "") = WalkSessionEntity(
        startedAt = 1_700_000_000_000, endedAt = 1_700_000_600_000,
        steps = 1000, durationSec = 600, distanceMeters = 760.0, calories = 40.0,
        pointsEarned = 12.0, recordingOwner = owner, track = "saved-track", crewId = crew,
    )

    @Test fun startingAccountIsRetainedAfterAnotherAccountSignsIn() = runBlocking {
        val store = Store(login("A")); val http = Http(); val holder = holder(store, http)
        val recorded = run(holder.recordingOwner())
        store.save(login("B"))
        assertEquals("account:A", recorded.recordingOwner)
        assertTrue(ServerSessionRecorder(server(holder, http)).record(recorded) is ServerResult.SignInRequired)
        assertTrue(http.requests.isEmpty())
    }

    @Test fun guestAndUnattributedLegacyNeverUploadWithTheCurrentLogin() = runBlocking {
        val store = Store(null); val http = Http(); val holder = holder(store, http)
        assertEquals(RecordingOwner.GUEST, holder.recordingOwner())
        store.save(login("A"))
        val recorder = ServerSessionRecorder(server(holder, http))
        for (owner in listOf(RecordingOwner.GUEST, RecordingOwner.LEGACY, "account:")) {
            assertTrue(recorder.record(run(owner)) is ServerResult.SignInRequired)
        }
        assertTrue(http.requests.isEmpty())
    }

    @Test fun accountSwitchAfterRecordCannotRetagACrewWithTheReplacementToken() = runBlocking {
        val store = Store(login("A")); val http = Http(); val server = server(holder(store, http), http)
        http.onRecord = { store.value = login("B") }
        val result = ServerSessionRecorder(server).record(run("account:A", "crew-A"))
        assertTrue(result is ServerResult.SignInRequired)
        assertEquals(listOf("record_session" to "Bearer token-A"), http.requests)
    }

    @Test fun accountSwitchBeforeCourseSubmissionKeepsTheCourseForRetry() = runBlocking {
        val store = Store(login("A")); val http = Http(); val server = server(holder(store, http), http)
        var acknowledged = false
        http.onRecord = { store.value = login("B") }
        val recorder = ServerSessionRecorder(server, CourseApi(server), { "course" }, { acknowledged = true })
        assertTrue(recorder.record(run("account:A")) is ServerResult.SignInRequired)
        assertFalse(acknowledged)
        assertEquals(1, http.requests.size)
        store.save(login("A")); http.onRecord = {}
        assertTrue(recorder.record(run("account:A")) is ServerResult.Ok)
        assertTrue(acknowledged)
        assertTrue(http.requests.all { it.second == "Bearer token-A" })
    }

    @Test fun offlineCourseSubmissionIsNotAcknowledgedUntilServerSuccess() = runBlocking {
        val store = Store(login("A")); val http = Http(); val server = server(holder(store, http), http)
        var acknowledgements = 0
        val recorder = ServerSessionRecorder(server, CourseApi(server), { "course" }, { acknowledgements++ })
        http.courseResponse = HttpResponse(0, "offline")
        assertTrue(recorder.record(run("account:A")) is ServerResult.Retry)
        assertEquals(0, acknowledgements)
        http.courseResponse = HttpResponse(200, "[]")
        assertTrue(recorder.record(run("account:A")) is ServerResult.Ok)
        assertEquals(1, acknowledgements)
    }

    @Test fun matchingAccountUsesItsTokenForEveryWrite() = runBlocking {
        val store = Store(login("A")); val http = Http(); val server = server(holder(store, http), http)
        val recorder = ServerSessionRecorder(server, CourseApi(server), { "course" })
        assertTrue(recorder.record(run("account:A", "crew-A")) is ServerResult.Ok)
        assertEquals(listOf("record_session", "session_tag_crew", "course_run_submit"), http.requests.map { it.first })
        assertTrue(http.requests.all { it.second == "Bearer token-A" })
    }
}
