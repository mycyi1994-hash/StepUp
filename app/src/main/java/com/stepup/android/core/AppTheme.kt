package com.stepup.android.core

import androidx.compose.runtime.mutableStateOf
import com.stepup.android.ui.theme.ThemeMode

/**
 * 고른 화면 테마를 프로세스에 들고 있는 자리.
 *
 * 앱이 켜질 때 저장소에서 한 번 읽어 [bootstrap] 으로 꽂고, 설정 화면에서
 * 바꾸면 [change] 로 갈아 끼운다. 스냅샷 상태라 바뀌는 즉시 화면이 다시
 * 그려진다 — 액티비티를 재생성하지 않으므로 보던 화면 그대로 색만 바뀐다.
 *
 * 첫 프레임 전에 값을 정해 두는 것이 중요하다. 나중에 정하면 어둡게 쓰는
 * 사람이 앱을 켤 때마다 흰 화면이 한 번 번쩍인다 — [AppLocale] 의 언어를
 * 앱 시작 때 확정하는 것과 같은 이유다.
 */
object AppTheme {

    private val state = mutableStateOf(ThemeMode.SYSTEM)

    /** 지금 고른 테마 */
    val mode: ThemeMode get() = state.value

    fun bootstrap(saved: String) {
        state.value = ThemeMode.of(saved)
    }

    fun change(next: ThemeMode) {
        state.value = next
    }
}
