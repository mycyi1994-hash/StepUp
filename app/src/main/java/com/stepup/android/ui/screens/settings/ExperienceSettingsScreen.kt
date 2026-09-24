package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.ExperiencePreferences
import com.stepup.android.ui.components.*
import com.stepup.android.ui.experience.*
import com.stepup.android.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun ExperienceSettingsScreen(onBack: () -> Unit) {
    val prefs = ServiceLocator.userPrefs
    val settings by prefs.experience.collectAsStateWithLifecycle(initialValue = ExperiencePreferences())
    val scope = rememberCoroutineScope()
    val feedback = LocalFeedback.current
    com.stepup.android.ui.components.DetailPage(
        title = stringResource(R.string.settings_experience), onBack = onBack,
    ) {
        item {
            Text(stringResource(R.string.experience_intro), style = MaterialTheme.typography.bodyLarge, color = Silver, modifier = Modifier.padding(vertical = 8.dp))
        }
        item {
            ExperienceToggle(Icons.Filled.VolumeUp, R.string.experience_sound, R.string.experience_sound_desc, settings.sounds) {
                scope.launch { prefs.setSounds(it) }
            }
        }
        item {
            ExperienceToggle(Icons.Filled.Vibration, R.string.experience_haptic, R.string.experience_haptic_desc, settings.haptics) {
                scope.launch { prefs.setHaptics(it) }
            }
        }
        item {
            ExperienceToggle(Icons.Filled.Animation, R.string.experience_motion, R.string.experience_motion_desc, settings.reducedMotion) {
                scope.launch { prefs.setReducedMotion(it) }
            }
        }
        item {
            GhostButton(stringResource(R.string.experience_preview),
                onClick = { feedback?.play(FeedbackCue.Reward) }, enabled = settings.sounds || settings.haptics, modifier = Modifier.fillMaxWidth())
        }
        item {
            InformationNote(stringResource(R.string.experience_system_note), Icons.Filled.Info)
        }
    }
}

@Composable
private fun ExperienceToggle(icon: ImageVector, title: Int, description: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    PreferenceToggle(
        title = stringResource(title), description = stringResource(description), icon = icon,
        checked = checked, onCheckedChange = onChange,
    )
}
