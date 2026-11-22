package com.stepup.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import java.util.Collections
import java.util.WeakHashMap

/**
 * 잠금 화면 덮개(`LockRunHost`) 아래의 창들(2026-11-22 보안 점검).
 *
 * 달리는 동안 앱은 잠금 화면 위에 뜰 수 있고, 기기가 잠겨 있으면 덮개가 앱 화면을 덮어 러닝 현황만 보인다.
 * 그런데 대화 상자 · 시트 · 팝업 · 펼침 메뉴는 같은 액티비티의 **따로 된 창**이라 덮개보다 위에 뜨고 잠금 화면 위에서도 눌렸다
 * (잠긴 폰을 주운 사람이 계정 삭제 시트를 누를 수 있었다). 그래서 덮여 있는 동안에는 창을 만드는 곳이 모두 그리지 않는다 —
 * 창을 만드는 컴포저블은 맨 앞에서 `if (LockCover.hidesWindows()) return`, 안드로이드 대화 상자(시간 고르기)는 [show] 로 띄운다.
 * 창을 열어 둔 상태(보일지 · 고른 값)는 부른 쪽에 그대로 남아 잠금을 풀면 시트가 다시 뜬다. 앱 화면 · 길은 그대로 그려 둔다.
 * `LockCoverTest` 가 창을 만드는 곳마다 이 문을 거치는지 소스에서 본다.
 */
object LockCover {

    /** 덮개가 앱 화면을 덮고 있다 — `LockRunHost` 가 준다(그 밖에서는 늘 false) */
    val LocalCovered = compositionLocalOf { false }

    /** 지금 창(대화 상자 · 시트 · 팝업 · 펼침 메뉴)을 그리지 말아야 하는가 */
    @Composable
    @ReadOnlyComposable
    fun hidesWindows(): Boolean = LocalCovered.current

    /** 덮을까 — 달리는 중이고 기기가 잠겨 있다 */
    fun covers(running: Boolean, locked: Boolean): Boolean = running && locked

    private val platformDialogs: MutableSet<android.app.Dialog> = Collections.newSetFromMap(WeakHashMap())

    /** 안드로이드 대화 상자를 띄운다 — 덮개가 덮이면 [dismissAll] 이 닫는다(앱 화면을 누를 수 없으니 덮인 동안 새로 열리지 않는다) */
    fun show(dialog: android.app.Dialog) {
        platformDialogs += dialog
        dialog.show()
    }

    /** 띄워 둔 안드로이드 대화 상자를 모두 닫는다 — 덮개가 덮일 때 */
    fun dismissAll() {
        val open = platformDialogs.toList()
        platformDialogs.clear()
        open.forEach { runCatching { it.dismiss() } }
    }
}
