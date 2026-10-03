package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.ExperiencePreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueInlineError
import com.stepup.android.ui.components.BlueLoadFailed
import com.stepup.android.ui.components.BlueNote
import com.stepup.android.ui.components.BluePage
import com.stepup.android.ui.components.BlueSkeleton
import com.stepup.android.ui.components.BlueSwitchRow
import com.stepup.android.ui.components.blueListColors
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 소리 · 진동 · 동작(설정 v1 20 → 파란 톤 v4 SET20 · 39) — 효과음 · 배경 환경음 · 터치 진동 · 동작 줄이기(기존 ExperiencePreferences 그대로),
 * 그리고 기존 날씨 풍경 스위치. 저장된 값을 읽은 뒤에 스위치를 보인다(읽기 전의 기본값을 실제 값처럼 보이지 않는다).
 *
 * "효과 미리 듣기"는 기존 보상 소리(FeedbackCue.Reward)를 한 번 낸다 — 효과음이나 진동이 켜져 있을 때만 누를 수 있고,
 * 보상을 주거나 러닝 · 뽑기를 실행하지 않는다. 무음 · 방해금지 · 휴대폰 설정은 소리 쪽이 그대로 따른다.
 */
@Composable
fun ExperienceSettingsScreen(onBack: () -> Unit = {}) {
    val prefs = ServiceLocator.userPrefs
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedback.current
    var attempt by remember { mutableIntStateOf(0) }
    val load by remember(attempt) {
        prefs.experience.map<ExperiencePreferences, SettingsLoad<ExperiencePreferences>> { SettingsLoad.Ready(it) }
            .catch { emit(SettingsLoad.Failed) }
    }.collectAsState(initial = SettingsLoad.Loading)
    val weatherOn by prefs.weatherBackground.collectAsStateWithLifecycle(initialValue = false)
    var failed by remember { mutableStateOf(false) }
    fun save(block: suspend () -> Unit) {
        scope.launch {
            failed = try {
                block()
                false
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                true
            }
        }
    }
    ExperienceContent(
        load = load, weatherOn = weatherOn, saveFailed = failed, onBack = onBack, onReload = { attempt++ },
        onSounds = { save { prefs.setSounds(it) } },
        onAmbience = { save { prefs.setAmbience(it) } },
        onHaptics = { save { prefs.setHaptics(it) } },
        onReducedMotion = { save { prefs.setReducedMotion(it) } },
        onWeather = { save { prefs.setWeatherBackground(it) } },
        onPreview = { feedback?.play(FeedbackCue.Reward) },
    )
}

@Composable
fun ExperienceContent(
    load: SettingsLoad<ExperiencePreferences>,
    weatherOn: Boolean = false,
    saveFailed: Boolean = false,
    onBack: () -> Unit = {},
    onReload: () -> Unit = {},
    onSounds: (Boolean) -> Unit = {},
    onAmbience: (Boolean) -> Unit = {},
    onHaptics: (Boolean) -> Unit = {},
    onReducedMotion: (Boolean) -> Unit = {},
    onWeather: (Boolean) -> Unit = {},
    onPreview: () -> Unit = {},
) {
    BluePage(title = stringResource(R.string.set_experience), onBack = onBack) {
        when (load) {
            SettingsLoad.Loading -> {
                item { BlueNote(stringResource(R.string.set_loading), intro = true) }
                item { BlueSkeleton(5) }
            }
            SettingsLoad.Failed -> item {
                BlueLoadFailed(stringResource(R.string.set_load_failed), stringResource(R.string.set_load_failed_body),
                    stringResource(R.string.set_reload), onReload)
            }
            is SettingsLoad.Ready -> {
                val settings = load.value
                item { BlueNote(stringResource(R.string.experience_intro), intro = true) }
                item {
                    BlueGroup {
                        BlueSwitchRow(stringResource(R.string.experience_sound), stringResource(R.string.experience_sound_desc),
                            settings.sounds, onSounds, Modifier.testTag("experience-sound"))
                        BlueSwitchRow(stringResource(R.string.experience_ambience), stringResource(R.string.experience_ambience_desc),
                            settings.ambience, onAmbience, Modifier.testTag("experience-ambience"))
                        BlueSwitchRow(stringResource(R.string.experience_haptic), stringResource(R.string.experience_haptic_desc),
                            settings.haptics, onHaptics, Modifier.testTag("experience-haptic"))
                        BlueSwitchRow(stringResource(R.string.experience_motion), stringResource(R.string.experience_motion_desc),
                            settings.reducedMotion, onReducedMotion, Modifier.testTag("experience-motion"))
                        // S2 날씨 풍경 — 대략적인 위치를 날씨 서비스에 보내므로 켤 때만 동작한다(기존 기능 · 기존 설명 그대로)
                        BlueSwitchRow(stringResource(R.string.experience_weather), stringResource(R.string.experience_weather_desc),
                            weatherOn, onWeather, Modifier.testTag("experience-weather"))
                    }
                }
                item {
                    // 기존 피드백 소리 · 진동만 낸다(보상 · 러닝 · 뽑기를 시작하지 않는다)
                    PreviewButton(
                        stringResource(R.string.experience_preview), onPreview,
                        enabled = settings.sounds || settings.haptics,
                        modifier = Modifier.padding(top = 4.dp).testTag("experience-preview"),
                    )
                }
                if (saveFailed) {
                    // SET39 — 이전 설정 그대로
                    item { BlueInlineError(stringResource(R.string.set_save_failed), stringResource(R.string.set_save_failed_body)) }
                }
                item { BlueNote(stringResource(R.string.experience_system_note)) }
            }
        }
    }
}

/** 효과 미리 듣기 — 파란 테두리 칸 · 왼쪽 소리 아이콘(다른 화면으로 가는 화살표가 아니다) */
@Composable
private fun PreviewButton(label: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier = Modifier) {
    val t = runTone()
    val c = blueListColors()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(c.face, shape)
            .border(1.5.dp, if (enabled) c.edge else t.divider, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Outlined.VolumeUp, contentDescription = null, tint = if (enabled) t.label else t.muted,
            modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = runTextStyle(18.sp, if (enabled) t.text else t.muted, FontWeight.Bold))
    }
}
