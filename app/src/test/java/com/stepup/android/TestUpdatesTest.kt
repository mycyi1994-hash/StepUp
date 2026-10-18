package com.stepup.android

import com.stepup.android.core.TestBuild
import com.stepup.android.core.TestUpdates
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

class TestUpdatesTest {
    @Test fun parsesTheReleaseInfoWrittenByCi() {
        val build = TestUpdates.parse("""{"commitTime":1790000000,"commit":"abc1234","version":"1.17.0"}""")
        assertEquals(TestBuild(1_790_000_000, "abc1234", "1.17.0"), build)
    }

    @Test fun rejectsBrokenOrEmptyInfo() {
        assertNull(TestUpdates.parse("Not Found"))
        assertNull(TestUpdates.parse("""{"commit":"abc1234"}"""))
    }

    @Test fun onlyALaterCommitIsAnUpdate() {
        val remote = TestBuild(2_000, "bbbbbbb", "1.17.0")
        assertTrue(TestUpdates.isNewer(remote, localCommitTime = 1_000, localCommit = "aaaaaaa"))
        assertFalse("same build", TestUpdates.isNewer(remote, 2_000, "bbbbbbb"))
        assertFalse("this phone is newer (work-branch build)", TestUpdates.isNewer(remote, 3_000, "ccccccc"))
        assertFalse("same commit even if times disagree", TestUpdates.isNewer(remote, 1_000, "bbbbbbb"))
        assertFalse("unknown local build never nags", TestUpdates.isNewer(remote, 0, ""))
    }

    @Test fun aBuildWithAnOlderDatabaseIsNotOffered() {
        // 데이터베이스 판 18 의 작업 브랜치 빌드(18025) 위에 판 17 의 main 빌드(17025)는 안드로이드가 설치를 막는다
        val older = TestUpdates.parse("""{"commitTime":2000,"commit":"bbbbbbb","version":"1.17.0","versionCode":17025}""")!!
        assertEquals(17025, older.versionCode)
        assertFalse(TestUpdates.isNewer(older, 1_000, "aaaaaaa", localVersionCode = 18025))
        assertTrue("same database", TestUpdates.isNewer(older, 1_000, "aaaaaaa", localVersionCode = 17025))
        assertTrue("newer database", TestUpdates.isNewer(older, 1_000, "aaaaaaa", localVersionCode = 16025))
        assertTrue("old build info without versionCode", TestUpdates.isNewer(TestBuild(2_000, "bbbbbbb", "1.17.0"), 1_000, "aaaaaaa", 18025))
    }

    @Test fun testBuildsCarryTheDatabaseVersionInVersionCode() {
        // app/build.gradle.kts — debug · qa 의 versionCode = 데이터베이스 판 × 1000 + 스토어 versionCode (스토어 빌드는 손으로 올린 값)
        assumeTrue(BuildConfig.BUILD_TYPE != "release")
        val schemas = listOf(File("schemas/com.stepup.android.data.local.AppDatabase"), File("app/schemas/com.stepup.android.data.local.AppDatabase"))
            .first { it.isDirectory }
        val newest = schemas.listFiles().orEmpty().mapNotNull { it.name.removeSuffix(".json").toIntOrNull() }.max()
        assertEquals(newest, BuildConfig.VERSION_CODE / 1000)
        assertTrue(BuildConfig.VERSION_CODE % 1000 in 1..999)
    }

    @Test fun checksAgainWhenTheAppComesBackButNotTooOften() {
        val build = TestBuild(2_000, "bbbbbbb", "1.17.0")
        // 처음 켤 때는 바로
        assertTrue(TestUpdates.shouldCheck(TestUpdates.State.Idle, force = false, sinceLastMs = -1))
        // 돌아올 때는 5분이 지났을 때만 — 최신이었어도, "나중에"를 눌렀어도
        assertFalse(TestUpdates.shouldCheck(TestUpdates.State.UpToDate, false, 60_000))
        assertTrue(TestUpdates.shouldCheck(TestUpdates.State.UpToDate, false, TestUpdates.RECHECK_MS))
        assertTrue(TestUpdates.shouldCheck(TestUpdates.State.Dismissed(build), false, TestUpdates.RECHECK_MS))
        // 받는 중 · 알리는 중에는 묻지 않는다. 설정에서 누르면 바로
        assertFalse(TestUpdates.shouldCheck(TestUpdates.State.Available(build), true, TestUpdates.RECHECK_MS))
        assertFalse(TestUpdates.shouldCheck(TestUpdates.State.Downloading(build, null), true, TestUpdates.RECHECK_MS))
        assertTrue(TestUpdates.shouldCheck(TestUpdates.State.UpToDate, true, 0))
    }

    @Test fun aDismissedBuildStaysQuietUntilANewerOneIsUp() {
        val dismissed = TestUpdates.State.Dismissed(TestBuild(2_000, "bbbbbbb", "1.17.0"))
        val same = TestBuild(2_000, "bbbbbbb", "1.17.0")
        val next = TestBuild(3_000, "ccccccc", "1.17.0")
        assertEquals(dismissed, TestUpdates.afterCheck(dismissed, same, newer = true, force = false))
        assertEquals(TestUpdates.State.Available(next), TestUpdates.afterCheck(dismissed, next, newer = true, force = false))
        assertEquals(TestUpdates.State.Available(same), TestUpdates.afterCheck(dismissed, same, newer = true, force = true))
        assertEquals(TestUpdates.State.UpToDate, TestUpdates.afterCheck(TestUpdates.State.Idle, same, newer = false, force = false))
        assertEquals(dismissed, TestUpdates.afterCheck(dismissed, null, newer = false, force = false))
        assertEquals(TestUpdates.State.CheckFailed, TestUpdates.afterCheck(TestUpdates.State.Idle, null, newer = false, force = true))
    }
}
