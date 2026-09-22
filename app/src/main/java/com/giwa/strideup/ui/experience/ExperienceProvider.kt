package com.giwa.strideup.ui.experience

import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.giwa.strideup.core.ServiceLocator

data class MotionPreferences(val reduced: Boolean = false, val active: Boolean = true) {
    val decorative: Boolean get() = !reduced && active
    fun duration(millis: Int): Int = if (reduced) 0 else millis
}

val LocalMotion = staticCompositionLocalOf { MotionPreferences() }
val LocalFeedback = staticCompositionLocalOf<ExperienceFeedback?> { null }

@Composable
fun ExperienceProvider(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val preferences by ServiceLocator.userPrefs.experience.collectAsStateWithLifecycle(initialValue = null)
    val feedback = remember(context, view) { ExperienceFeedback(context, view) }
    LaunchedEffect(lifecycle, feedback) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            ExperienceEvents.cues.collect { feedback.play(it) }
        }
    }
    var active by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) }
    fun systemReduced() = Settings.Global.getFloat(
        context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    var reducedBySystem by remember { mutableStateOf(systemReduced()) }

    DisposableEffect(lifecycle, feedback) {
        val observer = LifecycleEventObserver { _, _ ->
            active = lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
            feedback.foreground = active
            if (active) reducedBySystem = systemReduced()
        }
        val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) { reducedBySystem = systemReduced() }
        }
        context.contentResolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, settingsObserver)
        lifecycle.addObserver(observer)
        feedback.foreground = active
        onDispose {
            lifecycle.removeObserver(observer)
            context.contentResolver.unregisterContentObserver(settingsObserver)
            feedback.close()
        }
    }
    SideEffect {
        feedback.soundsEnabled = preferences?.sounds == true
        feedback.hapticsEnabled = preferences?.haptics == true
        if (!feedback.soundsEnabled) feedback.stop()
    }
    CompositionLocalProvider(
        LocalFeedback provides feedback,
        LocalMotion provides MotionPreferences(reducedBySystem || preferences?.reducedMotion != false, active),
        content = content,
    )
}
