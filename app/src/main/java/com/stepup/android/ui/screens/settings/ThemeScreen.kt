package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.SettingsChoiceRow
import com.stepup.android.ui.components.SettingsNote
import com.stepup.android.ui.components.SettingsNotice
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.theme.StepUpNumbers
import com.stepup.android.ui.theme.ThemeMode
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class ThemeOption(val mode: ThemeMode, val label: Int, val note: Int)

/** 시안 순서 — 기기 설정 따르기 · 어둡게 · 밝게. 저장 값(SYSTEM · DARK · LIGHT)은 그대로다 */
private val OPTIONS = listOf(
    ThemeOption(ThemeMode.SYSTEM, R.string.set_theme_system, R.string.set_theme_system_desc),
    ThemeOption(ThemeMode.DARK, R.string.set_theme_dark, R.string.set_theme_dark_desc),
    ThemeOption(ThemeMode.LIGHT, R.string.set_theme_light, R.string.set_theme_light_desc),
)

/**
 * 화면 테마(설정 v1 16 · 17). 고르는 즉시 바뀐다 — 액티비티를 다시 만들지 않아 보던 자리가 그대로다.
 * 저장하지 못하면 직전 테마로 되돌리고 알린다(적용 중인 값과 저장된 값이 어긋난 채 두지 않는다).
 * 위 미리보기의 거리는 이 계정의 실제 누적 러닝 거리다 — 예시 숫자를 기록처럼 보이지 않는다.
 */
@Composable
fun ThemeScreen(onBack: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val totals by ServiceLocator.stepRepository.observeRunTotals().collectAsStateWithLifecycle(initialValue = null)
    var failed by remember { mutableStateOf(false) }
    ThemeContent(
        selected = AppTheme.mode, distanceMeters = totals?.meters, saveFailed = failed, onBack = onBack,
        onPick = { mode ->
            val previous = AppTheme.mode
            if (mode != previous) {
                AppTheme.change(mode)
                scope.launch {
                    failed = try {
                        ServiceLocator.userPrefs.setThemeMode(mode.name)
                        false
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        AppTheme.change(previous)
                        true
                    }
                }
            }
        },
    )
}

@Composable
fun ThemeContent(
    selected: ThemeMode,
    distanceMeters: Double?,
    saveFailed: Boolean = false,
    onBack: () -> Unit = {},
    onPick: (ThemeMode) -> Unit = {},
) {
    DetailPage(title = stringResource(R.string.set_theme), onBack = onBack) {
        item { ThemePreview(distanceMeters) }
        item {
            Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                OPTIONS.forEach { option ->
                    SettingsChoiceRow(
                        stringResource(option.label), selected = option.mode == selected, onClick = { onPick(option.mode) },
                        description = stringResource(option.note), modifier = Modifier.testTag("theme-${option.mode.name.lowercase()}"),
                    )
                }
            }
        }
        if (saveFailed) {
            item {
                SettingsNotice(stringResource(R.string.set_save_failed), stringResource(R.string.set_save_failed_body),
                    Modifier.padding(top = 18.dp).testTag("theme-save-failed"))
            }
        } else {
            item { SettingsNote(stringResource(R.string.set_theme_note)) }
        }
    }
}

/** 화면 미리보기 — 고른 테마의 면 · 글자 · 강조색. 거리는 실제 누적 러닝 거리 */
@Composable
private fun ThemePreview(distanceMeters: Double?) {
    val p = settingsPalette()
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(p.surface)
            .padding(horizontal = 22.dp, vertical = 20.dp).semantics(mergeDescendants = true) {}
            .testTag("theme-preview"),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.set_theme_preview), color = p.secondary, fontSize = 13.sp)
            Text(stringResource(R.string.set_theme_preview_label), color = p.text, fontSize = 15.sp)
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    distanceMeters?.let { String.format(Locale.ROOT, "%,.1f", it / 1000) } ?: "—",
                    color = p.text, fontSize = 44.sp, fontFamily = StepUpNumbers, fontWeight = FontWeight.SemiBold,
                )
                Text("km", color = p.accent, fontSize = 18.sp, modifier = Modifier.padding(bottom = 8.dp))
            }
        }
        // 달린 길 한 가닥 — 장식
        Canvas(Modifier.size(width = 64.dp, height = 90.dp).align(Alignment.CenterEnd)) {
            val path = Path().apply {
                moveTo(size.width * 0.78f, size.height * 0.06f)
                cubicTo(size.width * 0.2f, size.height * 0.2f, size.width * 0.05f, size.height * 0.4f, size.width * 0.45f, size.height * 0.62f)
                cubicTo(size.width * 0.75f, size.height * 0.78f, size.width * 0.35f, size.height * 0.92f, size.width * 0.3f, size.height * 0.98f)
            }
            drawPath(path, p.accent.copy(alpha = 0.85f), style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(p.accent, radius = 3.5.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.06f))
        }
    }
}
