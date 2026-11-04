package com.stepup.android.ui.experience

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
import com.stepup.android.core.ServiceLocator

/* 모션 · 소리 값(MotionPreferences · LocalMotion · LocalFeedback)은 공용(sharedUi ui/experience) — 여기서는 안드로이드 소리 · 진동을 넣는다 */

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
    LaunchedEffect(lifecycle, feedback) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                kotlinx.coroutines.delay(2_000)
                feedback.recheckAmbientPolicy()
            }
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
        feedback.ambientEnabled = preferences?.ambience == true
        feedback.hapticsEnabled = preferences?.haptics == true
        if (!feedback.soundsEnabled) feedback.stop()
    }
    CompositionLocalProvider(
        LocalFeedback provides feedback,
        LocalMotion provides MotionPreferences(reducedBySystem || preferences?.reducedMotion != false, active),
        content = content,
    )
}
