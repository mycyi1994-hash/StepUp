package com.stepup.android

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.ExifInterface
import androidx.test.platform.app.InstrumentationRegistry
import com.stepup.android.data.repo.AvatarPhotoStore
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * 프로필 사진 저장(프로필 수정 v1) — 새 사진은 임시 파일에서 확인한 뒤에만 기존 사진과 바꾸고, 못 하면 기존 사진 · 설정이 그대로다.
 * 긴 변 512px · JPEG, 사진의 방향은 바로 세운다.
 */
class AvatarPhotoStoreTest {
    private lateinit var dir: File
    private val name = "avatar_test.jpg"
    private var commits = 0

    @Before fun setUp() {
        dir = File(InstrumentationRegistry.getInstrumentation().targetContext.cacheDir, "avatar-store-test").apply {
            deleteRecursively()
            mkdirs()
        }
        commits = 0
    }

    @After fun tearDown() {
        dir.deleteRecursively()
    }

    private fun store(commit: suspend () -> Unit = { commits++ }) = AvatarPhotoStore(dir, name, commit)

    private fun jpeg(width: Int, height: Int): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }.toByteArray()
    }

    private fun size(file: File): Pair<Int, Int> {
        val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, o)
        return o.outWidth to o.outHeight
    }

    @Test fun validPhotoIsShrunkAndCommittedOnce() = runBlocking {
        val big = jpeg(3000, 2000)
        assertTrue(store().save { ByteArrayInputStream(big) })
        assertEquals(1, commits)
        val (w, h) = size(File(dir, name))
        assertEquals(512, maxOf(w, h))
        assertTrue("aspect kept $w×$h", kotlin.math.abs(w.toDouble() / h - 1.5) < 0.02)
        assertFalse(File(dir, "$name.new").exists())
        assertFalse(File(dir, "$name.old").exists())
    }

    @Test fun unreadableDataKeepsThePreviousPhoto() = runBlocking {
        assertTrue(store().save { ByteArrayInputStream(jpeg(300, 300)) })
        val before = File(dir, name).readBytes()
        assertFalse(store().save { ByteArrayInputStream(byteArrayOf(1, 2, 3, 4, 5)) })
        assertFalse(store().save { null })
        assertArrayEquals(before, File(dir, name).readBytes())
        assertEquals(1, commits)
        assertFalse(File(dir, "$name.new").exists())
    }

    @Test fun failedSettingsWriteRestoresThePreviousPhoto() = runBlocking {
        assertTrue(store().save { ByteArrayInputStream(jpeg(300, 300)) })
        val before = File(dir, name).readBytes()
        val failing = store { throw IOException("prefs") }
        assertFalse(failing.save { ByteArrayInputStream(jpeg(200, 100)) })
        assertArrayEquals(before, File(dir, name).readBytes())
        assertFalse(File(dir, "$name.old").exists())
    }

    @Test fun firstPhotoWithFailedSettingsLeavesNoFile() = runBlocking {
        assertFalse(store { throw IOException("prefs") }.save { ByteArrayInputStream(jpeg(200, 100)) })
        assertFalse(File(dir, name).exists())
    }

    @Test fun rotatedPhotoIsStoredUpright() = runBlocking {
        val source = File(dir, "source.jpg").apply { writeBytes(jpeg(400, 200)) }
        ExifInterface(source.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        assertTrue(store().save { source.inputStream() })
        assertEquals(200 to 400, size(File(dir, name)))
    }
}
