package com.stepup.android

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.prefs.UserPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.job
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.File
import java.util.UUID

class CourseQueuePersistenceTest {
    @Test fun moreThanTwentyUnacknowledgedCoursesSurviveReopeningAndReads() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "course-queue-${UUID.randomUUID()}.preferences_pb")
        var scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        fun open() = UserPrefs(context, PreferenceDataStoreFactory.create(scope = scope, produceFile = { file }))
        var prefs = open()
        val expected = (1L..30L).associateWith { "37.5,127.0;37.51,127.${it}" }
        try {
            expected.forEach { (startedAt, track) -> prefs.addPendingCourseRun(startedAt, track) }
            scope.coroutineContext.job.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            prefs = open()
            repeat(2) {
                expected.forEach { (startedAt, track) -> assertEquals(track, prefs.pendingCourseRun(startedAt)) }
            }
            // Only the acknowledged item is consumed; a repeated acknowledgement is harmless.
            assertEquals(expected[1L], prefs.takePendingCourseRun(1L))
            assertNull(prefs.takePendingCourseRun(1L))
            scope.coroutineContext.job.cancelAndJoin()
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            prefs = open()
            assertNull(prefs.pendingCourseRun(1L))
            expected.filterKeys { it != 1L }.forEach { (startedAt, track) ->
                assertEquals(track, prefs.pendingCourseRun(startedAt))
            }
        } finally {
            scope.coroutineContext.job.cancelAndJoin()
            file.delete()
        }
    }
}
