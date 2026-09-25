package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.theme.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class ThemeOption(
    val mode: ThemeMode,
    val icon: ImageVector,
    val labelRes: Int,
    val noteRes: Int,
)

private val OPTIONS = listOf(
    ThemeOption(ThemeMode.SYSTEM, Icons.Filled.PhoneAndroid, R.string.theme_system, R.string.theme_system_note),
    ThemeOption(ThemeMode.LIGHT, Icons.Filled.LightMode, R.string.theme_light, R.string.theme_light_note),
    ThemeOption(ThemeMode.DARK, Icons.Filled.DarkMode, R.string.theme_dark, R.string.theme_dark_note),
)

/**
 * 화면 테마 설정.
 *
 * 고르는 즉시 바뀐다. 액티비티를 다시 만들지 않으므로 보던 자리가 그대로
 * 있고 색만 갈린다 — 언어와 달리 리소스를 다시 읽을 일이 없기 때문이다.
 */
@Composable
fun ThemeScreen(onBack: () -> Unit = {}) {
    val selected = AppTheme.mode

    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_theme), onBack = onBack,
    ) {
        item {
            com.stepup.android.ui.components.InformationNote(
                text = stringResource(R.string.theme_note), icon = Icons.Filled.DarkMode,
            )
        }

        items(OPTIONS.size) { index ->
            val option = OPTIONS[index]
            ThemeRow(
                option = option,
                checked = option.mode == selected,
                onClick = {
                    if (option.mode != selected) {
                        AppTheme.change(option.mode)
                        // 저장은 화면 수명과 무관한 스코프에서 — 고르자마자
                        // 뒤로 나가도 선택이 남아야 한다
                        CoroutineScope(Dispatchers.IO).launch {
                            ServiceLocator.userPrefs.setThemeMode(option.mode.name)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ThemeRow(option: ThemeOption, checked: Boolean, onClick: () -> Unit) {
    com.stepup.android.ui.components.PreferenceChoice(
        title = stringResource(option.labelRes),
        description = stringResource(option.noteRes),
        icon = option.icon, selected = checked, onClick = onClick,
    )
}
