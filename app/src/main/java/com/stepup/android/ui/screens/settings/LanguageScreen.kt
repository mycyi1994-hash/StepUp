package com.stepup.android.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.core.AppLocale
import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 고를 수 있는 언어 — 이름은 그 언어 자체로 적어 어느 언어에서도 알아볼 수 있게 한다 */
private data class LanguageOption(val tag: String, val nativeName: String, val labelRes: Int)

private val OPTIONS = listOf(
    LanguageOption(AppLocale.SYSTEM, "", R.string.language_system),
    LanguageOption("en", "English", R.string.language_en),
    LanguageOption("ko", "한국어", R.string.language_ko),
    LanguageOption("zh", "中文", R.string.language_zh),
    LanguageOption("ja", "日本語", R.string.language_ja),
)

/**
 * 언어 설정.
 *
 * 고르는 즉시 저장하고 화면을 새 언어로 다시 그린다.
 * Android 13 이상에서는 OS의 앱별 언어 설정에도 그대로 반영된다.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    var selected by remember { mutableStateOf(AppLocale.tag) }

    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_language), onBack = onBack,
    ) {
        item {
            com.stepup.android.ui.components.InformationNote(
                text = stringResource(R.string.language_note), icon = Icons.Filled.Language,
            )
        }

        items(OPTIONS.size) { index ->
            val option = OPTIONS[index]
            LanguageRow(
                option = option,
                checked = option.tag == selected,
                onClick = {
                    if (option.tag != selected) {
                        selected = option.tag
                        // 화면이 곧 재생성되므로 저장은 화면 수명과 무관한 스코프에서 한다
                        CoroutineScope(Dispatchers.IO).launch {
                            ServiceLocator.userPrefs.setLanguage(option.tag)
                        }
                        if (AppLocale.change(context, option.tag)) activity?.recreate()
                    }
                },
            )
        }
    }
}

@Composable
private fun LanguageRow(option: LanguageOption, checked: Boolean, onClick: () -> Unit) {
    val label = stringResource(option.labelRes)
    com.stepup.android.ui.components.PreferenceChoice(
        title = label,
        description = option.nativeName.takeIf { it.isNotEmpty() && it != label },
        selected = checked, onClick = onClick,
    )
}
