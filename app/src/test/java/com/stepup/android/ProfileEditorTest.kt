package com.stepup.android

import com.stepup.android.data.repo.NameWork
import com.stepup.android.data.repo.PhotoWork
import com.stepup.android.data.repo.ProfileEditor
import com.stepup.android.data.repo.nicknameChanged
import com.stepup.android.data.repo.normalizedNickname
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 프로필 수정의 저장 — 기존 규칙(닉네임 정규화 · 16자), 겹쳐 저장하지 않기, 실패하면 초안을 남기기 */
class ProfileEditorTest {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private class Gate {
        var next = CompletableDeferred<Unit>()
        val saved = mutableListOf<Any>()
        suspend fun hold(value: Any) {
            next.await()
            saved += value
        }
    }

    @Test
    fun `이름 비교는 저장과 같은 정규화 — 앞뒤 공백 · 16자`() {
        assertEquals("민수", normalizedNickname("  민수 ", 16))
        assertFalse(nicknameChanged("민수", " 민수 ", 16))
        assertTrue(nicknameChanged("민수", "", 16))
        assertFalse(nicknameChanged("", "   ", 16))
        // 17자를 붙여 넣어도 저장되는 것은 앞의 16자 — 같은 이름이면 바뀐 것이 아니다
        assertFalse(nicknameChanged("한강아침러너_STEPUP_01", "한강아침러너_STEPUP_012", 16))
        assertTrue(nicknameChanged("아침러너", "한강아침러너_STEPUP_01", 16))
    }

    @Test
    fun `닉네임 저장 — 저장 중에는 다시 받지 않고, 끝나면 저장됨`() {
        val gate = Gate()
        val editor = ProfileEditor({ gate.hold(it) }, { }, { true }, scope)
        assertTrue(editor.saveName("아침러너"))
        assertEquals(NameWork.Saving("아침러너"), editor.name.value)
        assertFalse(editor.saveName("다른이름"))
        // 사진도 이름을 저장하는 동안은 받지 않는다
        assertFalse(editor.savePhoto { null })
        gate.next.complete(Unit)
        assertEquals(NameWork.Saved, editor.name.value)
        assertEquals(listOf<Any>("아침러너"), gate.saved)
        editor.acknowledgeName()
        assertEquals(NameWork.Idle, editor.name.value)
    }

    @Test
    fun `닉네임 저장 실패 — 누른 때의 초안을 남기고, 다시 들어와도 되살린다`() {
        val editor = ProfileEditor({ throw IOException("disk") }, { }, { true }, scope)
        editor.saveName("아침러너")
        assertEquals(NameWork.Failed("아침러너"), editor.name.value)
        assertEquals("아침러너", editor.startSession())
        editor.discardName()
        assertEquals(NameWork.Idle, editor.name.value)
        assertNull(editor.startSession())
    }

    @Test
    fun `사진 — 저장 중에는 기본 이미지 · 이름 저장을 막고, 실패는 실패로 끝난다`() {
        val gate = CompletableDeferred<Boolean>()
        val saved = mutableListOf<Int>()
        val editor = ProfileEditor({ }, { saved += it }, { gate.await() }, scope)
        assertTrue(editor.savePhoto { null })
        assertEquals(PhotoWork.Saving, editor.photo.value)
        assertFalse(editor.savePhoto { null })
        assertFalse(editor.saveDefaultImage(3, current = 0))
        assertFalse(editor.saveName("아침러너"))
        gate.complete(false)
        assertEquals(PhotoWork.Failed, editor.photo.value)
        // 새 방문에서는 앞선 실패를 보이지 않는다
        editor.startSession()
        assertEquals(PhotoWork.Idle, editor.photo.value)
        assertTrue(saved.isEmpty())
    }

    @Test
    fun `기본 이미지 — 지금 쓰는 것을 다시 누르면 저장하지 않고, 저장 실패는 시트에 남는다`() {
        val saved = mutableListOf<Int>()
        var fail = false
        val editor = ProfileEditor({ }, { if (fail) throw IOException("disk") else saved += it }, { true }, scope)
        assertFalse(editor.saveDefaultImage(2, current = 2))
        assertTrue(saved.isEmpty())
        assertTrue(editor.saveDefaultImage(5, current = 2))
        assertEquals(listOf(5), saved)
        assertEquals(PhotoWork.Saved, editor.defaultImage.value)
        editor.acknowledgeDefaultImage()
        fail = true
        editor.saveDefaultImage(7, current = 5)
        assertEquals(PhotoWork.Failed, editor.defaultImage.value)
        assertEquals(listOf(5), saved)
    }
}
