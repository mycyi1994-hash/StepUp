package com.stepup.android.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.window.DialogProperties
import androidx.core.view.WindowCompat
import com.stepup.android.core.AndroidHost
import java.io.File

/** 낱말 단위 줄바꿈(Android 13+ 구 단위, 그 아래는 기본 줄바꿈) — 예전 KoreanLineBreak 그대로 */
actual fun koreanLineBreak(): LineBreak = LineBreak(
    strategy = LineBreak.Strategy.Balanced,
    strictness = LineBreak.Strictness.Strict,
    wordBreak = LineBreak.WordBreak.Phrase,
)

actual fun readCustomAvatar(): ImageBitmap? = runCatching {
    val file = File(AndroidHost.app.filesDir, com.stepup.android.data.prefs.UserPrefs.AVATAR_FILE)
    if (!file.exists()) null else android.graphics.BitmapFactory.decodeFile(file.path)?.asImageBitmap()
}.getOrNull()

@Composable
actual fun SystemBackHandler(enabled: Boolean, onBack: () -> Unit) {
    androidx.activity.compose.BackHandler(enabled = enabled, onBack = onBack)
}

/** 창 그늘 0 · 시스템 창 겹치기 — 화면이 직접 뒷면을 그린다 */
actual fun clearDialogProperties(dismissOnClickOutside: Boolean): DialogProperties =
    DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = dismissOnClickOutside)

/** 화면이 늘 남색이라 상태바 · 내비바 아이콘을 밝게 — 떠나면 앞의 값으로 되돌린다 */
@Composable
actual fun LightSystemBarIcons() {
    val view = LocalView.current
    if (view.isInEditMode) return
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val status = controller?.isAppearanceLightStatusBars
        val navigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            if (controller != null) {
                status?.let { controller.isAppearanceLightStatusBars = it }
                navigation?.let { controller.isAppearanceLightNavigationBars = it }
            }
        }
    }
}

actual fun is24HourClock(): Boolean = android.text.format.DateFormat.is24HourFormat(AndroidHost.app)

/**
 * 안드로이드 — Compose 리소스가 푼 그림을 앱 전체에서 다시 쓴다(리소스 라이브러리의 그림 캐시). 따로 미리 풀 곳이 없다.
 * 예전 RasterCache(R.drawable 를 뒤에서 미리 풀기)의 자리 — 탭 넘김이 끊기면 여기에 미리 풀기를 더한다.
 */
actual object RasterCache {
    actual fun isReady(id: org.jetbrains.compose.resources.DrawableResource): Boolean = true
    actual fun prewarm(vararg ids: org.jetbrains.compose.resources.DrawableResource) = Unit
}
