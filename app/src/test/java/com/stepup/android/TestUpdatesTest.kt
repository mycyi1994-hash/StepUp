package com.stepup.android

import com.stepup.android.core.TestBuild
import com.stepup.android.core.TestUpdates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
}
