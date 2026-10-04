package com.stepup.android.ui.screens.settings

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.stepup.android.R
import com.stepup.android.core.AppLocale
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.BlueChoiceRow
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueNote
import com.stepup.android.ui.components.BluePage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 한 가지 언어 — 저장 값(SYSTEM · en · ko · zh · ja)은 그대로. 영어 · 한국어는 제 이름, 중국어 · 일본어는 풀이와 제 이름 */
private data class LanguageOption(val tag: String, val nativeName: String?, val labelRes: Int?)

private val OPTIONS = listOf(
    LanguageOption(AppLocale.SYSTEM, null, R.string.set_theme_system),
    LanguageOption("en", "English", null),
    LanguageOption("ko", "한국어", null),
    LanguageOption("zh", "中文", R.string.language_zh),
    LanguageOption("ja", "日本語", R.string.language_ja),
)

/**
 * 언어(설정 v1 18 · 19 → 파란 톤 v4 SET18 · 19). 고르는 즉시 저장하고 화면을 새 언어로 다시 그린다 — Android 13 이상은 OS 의 앱별 언어 설정에도
 * 그대로 반영된다. 저장은 화면이 다시 만들어져도 끊기지 않는 곳에서 한다(고른 뒤 이전 언어로 돌아가지 않게).
 * 게시물 · 이름처럼 사람이 쓴 글은 번역하지 않는다.
 */
@Composable
fun LanguageScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    val activity = remember(context) { context as? Activity }
    var selected by remember { mutableStateOf(AppLocale.tag) }
    LanguageContent(selected, onBack) { tag ->
        if (tag != selected) {
            selected = tag
            // 화면이 곧 재생성되므로 저장은 화면 수명과 무관한 스코프에서 한다
            CoroutineScope(Dispatchers.IO).launch { ServiceLocator.userPrefs.setLanguage(tag) }
            if (AppLocale.change(context, tag)) activity?.recreate()
        }
    }
}

@Composable
fun LanguageContent(selected: String, onBack: () -> Unit = {}, onPick: (String) -> Unit = {}) {
    BluePage(title = stringResource(R.string.set_language), onBack = onBack) {
        item { BlueNote(stringResource(R.string.set_language_intro), intro = true) }
        item {
            BlueGroup {
                OPTIONS.forEach { option ->
                    val label = option.labelRes?.let { stringResource(it) } ?: option.nativeName.orEmpty()
                    BlueChoiceRow(
                        label, selected = option.tag == selected, onClick = { onPick(option.tag) },
                        description = option.nativeName?.takeIf { option.labelRes != null && it != label },
                        modifier = Modifier.testTag("language-${option.tag.ifEmpty { "system" }}"),
                    )
                }
            }
        }
        // 사람이 쓴 글 · 이름은 번역하지 않는다
        item { BlueNote(stringResource(R.string.set_language_note)) }
    }
}
