package com.stepup.android

import com.stepup.android.ui.components.LockCover
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * 잠금 화면 덮개(2026-11-22 보안 점검) — 달리는 동안 잠긴 폰에서는 앱 화면 위에 러닝 현황만 보이고, 대화 상자 · 시트 · 팝업 ·
 * 펼침 메뉴(덮개보다 위의 따로 된 창)는 그리지 않는다. 창을 새로 만드는 곳이 이 문을 빠뜨리면 그 창은 잠금 화면 위에서 눌린다.
 */
class LockCoverTest {

    @Test
    fun `달리는 중이고 기기가 잠겨 있을 때만 덮는다`() {
        assertTrue(LockCover.covers(running = true, locked = true))
        assertFalse(LockCover.covers(running = true, locked = false))
        assertFalse(LockCover.covers(running = false, locked = true))
        assertFalse(LockCover.covers(running = false, locked = false))
    }

    @Test
    fun `창을 만드는 곳은 모두 덮개 문을 먼저 지난다`() {
        val root = listOf(File("src/main/java"), File("app/src/main/java")).first { it.isDirectory }
        val missing = mutableListOf<String>()
        var seen = 0
        root.walkTopDown().filter { it.isFile && it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            WINDOW.findAll(text).forEach { match ->
                val at = match.range.first
                val lineStart = text.lastIndexOf('\n', at) + 1
                val before = text.substring(lineStart, at).trim()
                // 주석 · KDoc · import 는 창을 만들지 않는다
                if (before.startsWith("//") || before.startsWith("*") || before.startsWith("import")) return@forEach
                seen++
                // 창을 부르는 컴포저블(가장 가까운 앞의 @Composable fun — 안쪽의 작은 fun dismiss() 는 건너뛴다)의 시작부터 이 줄까지 문이 있어야 한다
                val head = FUN.findAll(text.substring(0, at)).lastOrNull()?.range?.first ?: 0
                if (!text.substring(head, at).contains("LockCover.")) {
                    missing += "${file.name}:${text.substring(0, at).count { it == '\n' } + 1}"
                }
            }
        }
        assertTrue("창을 만드는 곳을 거의 못 찾았다($seen) — 소스를 읽지 못했다", seen >= 15)
        assertEquals("잠금 화면 덮개(LockCover) 없이 창을 만드는 곳", emptyList<String>(), missing)
    }

    private companion object {
        /** 따로 된 창을 만드는 것 — Compose 대화 상자 · 시트 · 팝업 · 펼침 메뉴와 안드로이드 시간 · 날짜 고르기 */
        val WINDOW = Regex(
            """(?<![\w])(?:[a-z]\w*\.)*(Dialog|AlertDialog|BasicAlertDialog|ModalBottomSheet|Popup|DropdownMenu|ExposedDropdownMenu|TimePickerDialog|DatePickerDialog)\s*\(""",
        )

        /** 컴포저블 함수 선언 — 앞에 다른 표시(@OptIn …)나 private · internal 이 있어도 */
        val FUN = Regex("""@Composable\s+(?:@[\w.]+(?:\([^)]*\))?\s+)*(?:(?:private|internal|public|inline|override)\s+)*fun\s""")
    }
}
