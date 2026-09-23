package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
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
            Text(stringResource(R.string.experience_intro), style = MaterialTheme.typography.bodyMedium, color = Silver)
        }
        item {
            ExperienceToggle(R.string.experience_sound, R.string.experience_sound_desc, settings.sounds) {
                scope.launch { prefs.setSounds(it) }
            }
        }
        item {
            ExperienceToggle(R.string.experience_haptic, R.string.experience_haptic_desc, settings.haptics) {
                scope.launch { prefs.setHaptics(it) }
            }
        }
        item {
            ExperienceToggle(R.string.experience_motion, R.string.experience_motion_desc, settings.reducedMotion) {
                scope.launch { prefs.setReducedMotion(it) }
            }
        }
        item {
            GhostButton(stringResource(R.string.experience_preview),
                onClick = { feedback?.play(FeedbackCue.Reward) }, enabled = settings.sounds || settings.haptics)
        }
        item {
            Text(stringResource(R.string.experience_system_note), style = MaterialTheme.typography.bodySmall, color = Silver)
        }
    }
}

@Composable
private fun ExperienceToggle(title: Int, description: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    GlowCard(Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(stringResource(description), style = MaterialTheme.typography.bodySmall, color = Silver)
            }
            Switch(checked, onCheckedChange = null)
        }
    }
}
