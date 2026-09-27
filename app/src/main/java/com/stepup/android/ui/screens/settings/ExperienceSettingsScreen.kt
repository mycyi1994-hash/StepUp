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
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.SettingsLoadFailed
import com.stepup.android.ui.components.SettingsNote
import com.stepup.android.ui.components.SettingsNotice
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSkeleton
import com.stepup.android.ui.components.SettingsSwitchRow
import com.stepup.android.ui.experience.FeedbackCue
import com.stepup.android.ui.experience.LocalFeedback
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * 소리 · 진동 · 동작(설정 v1 20) — 효과음 · 배경 환경음 · 터치 진동 · 동작 줄이기(기존 ExperiencePreferences 그대로),
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
    DetailPage(title = stringResource(R.string.set_experience), onBack = onBack) {
        when (load) {
            SettingsLoad.Loading -> {
                item { SettingsNote(stringResource(R.string.set_loading), top = true) }
                item { SettingsSkeleton(4, Modifier.padding(top = 18.dp)) }
            }
            SettingsLoad.Failed -> item {
                SettingsLoadFailed(stringResource(R.string.set_load_failed), stringResource(R.string.set_load_failed_body),
                    stringResource(R.string.set_reload), onReload)
            }
            is SettingsLoad.Ready -> {
                val settings = load.value
                item { SettingsNote(stringResource(R.string.experience_intro), top = true) }
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        SettingsSwitchRow(stringResource(R.string.experience_sound), stringResource(R.string.experience_sound_desc),
                            settings.sounds, onSounds, Modifier.testTag("experience-sound"))
                        SettingsSwitchRow(stringResource(R.string.experience_ambience), stringResource(R.string.experience_ambience_desc),
                            settings.ambience, onAmbience, Modifier.testTag("experience-ambience"))
                        SettingsSwitchRow(stringResource(R.string.experience_haptic), stringResource(R.string.experience_haptic_desc),
                            settings.haptics, onHaptics, Modifier.testTag("experience-haptic"))
                        SettingsSwitchRow(stringResource(R.string.experience_motion), stringResource(R.string.experience_motion_desc),
                            settings.reducedMotion, onReducedMotion, Modifier.testTag("experience-motion"))
                        // S2 날씨 풍경 — 대략적인 위치를 날씨 서비스에 보내므로 켤 때만 동작한다(기존 기능)
                        SettingsSwitchRow(stringResource(R.string.experience_weather), stringResource(R.string.experience_weather_desc),
                            weatherOn, onWeather, Modifier.testTag("experience-weather"))
                    }
                }
                item {
                    SettingsSecondaryButton(
                        stringResource(R.string.experience_preview), onPreview,
                        Modifier.fillMaxWidth().padding(top = 18.dp).testTag("experience-preview"),
                        enabled = settings.sounds || settings.haptics,
                    )
                }
                if (saveFailed) {
                    item {
                        SettingsNotice(stringResource(R.string.set_save_failed), stringResource(R.string.set_save_failed_body),
                            Modifier.padding(top = 12.dp))
                    }
                }
                item { SettingsNote(stringResource(R.string.experience_system_note)) }
            }
        }
    }
}
