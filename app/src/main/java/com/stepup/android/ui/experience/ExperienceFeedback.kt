package com.stepup.android.ui.experience

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.media.SoundPool
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RawRes
import com.stepup.android.R

/** 효과음 파일 · 크기 — 공용 이름([FeedbackCue])마다 안드로이드 소리(res/raw) */
private val FeedbackCue.sound: Int @RawRes get() = when (this) {
    FeedbackCue.Tap -> R.raw.ui_tap
    FeedbackCue.Select -> R.raw.ui_select
    FeedbackCue.Back -> R.raw.ui_back
    FeedbackCue.Toggle -> R.raw.ui_toggle
    FeedbackCue.SheetOpen -> R.raw.ui_sheet_open
    FeedbackCue.SheetClose -> R.raw.ui_sheet_close
    FeedbackCue.BackgroundSwitch -> R.raw.ui_background_switch
    FeedbackCue.Equip -> R.raw.item_equip
    FeedbackCue.Success -> R.raw.action_success
    FeedbackCue.Error -> R.raw.action_error
    FeedbackCue.Reward -> R.raw.reward_claim
    FeedbackCue.Countdown -> R.raw.run_countdown
    FeedbackCue.Start -> R.raw.run_start
    FeedbackCue.Resume -> R.raw.run_resume
    FeedbackCue.Pause -> R.raw.run_pause
    FeedbackCue.Lap -> R.raw.run_lap
    FeedbackCue.Finish -> R.raw.run_finish
    FeedbackCue.DrawEnter -> R.raw.draw_enter
    FeedbackCue.DrawCharge -> R.raw.draw_charge
    FeedbackCue.DrawBoxOpen -> R.raw.draw_box_open
    FeedbackCue.DrawCommon -> R.raw.draw_reveal_common
    FeedbackCue.DrawRare -> R.raw.draw_reveal_rare
    FeedbackCue.DrawEpic -> R.raw.draw_reveal_epic
    FeedbackCue.DrawLegendary -> R.raw.draw_reveal_legendary
    FeedbackCue.DrawCancel -> R.raw.draw_cancel
    FeedbackCue.DrawFail -> R.raw.draw_fail
}

private val FeedbackCue.gain: Float get() = when (this) {
    FeedbackCue.Tap -> .28f
    FeedbackCue.Select -> .35f
    FeedbackCue.Back -> .32f
    FeedbackCue.Toggle -> .34f
    FeedbackCue.SheetOpen -> .30f
    FeedbackCue.SheetClose -> .28f
    FeedbackCue.BackgroundSwitch -> .34f
    FeedbackCue.Equip -> .40f
    FeedbackCue.Error -> .40f
    FeedbackCue.Reward -> .62f
    FeedbackCue.DrawEnter -> .36f
    else -> .55f
}

private val AmbientScene.sound: Int @RawRes get() = when (this) {
    AmbientScene.Night -> R.raw.ambience_night_river
    AmbientScene.Dawn -> R.raw.ambience_dawn_river
    AmbientScene.Wardrobe -> R.raw.ambience_wardrobe_terrace
}

/** Activity-owned short sounds. Never starts music, changes volume, or requests audio focus. */
class ExperienceFeedback(context: Context, private val view: View) : AutoCloseable, ExperiencePlayer {
    private val app = context.applicationContext
    private val audio = app.getSystemService(AudioManager::class.java)
    private val notifications = app.getSystemService(NotificationManager::class.java)
    private val loaded = mutableSetOf<Int>()
    private val streams = ArrayDeque<Int>()
    private val pool = SoundPool.Builder().setMaxStreams(2)
        .setAudioAttributes(AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
        .build()
    private val samples: Map<FeedbackCue, Int>
    var soundsEnabled = false
        set(value) {
            field = value
            if (!value) stop() else refreshAmbient()
        }
    var ambientEnabled = false
        set(value) {
            field = value
            refreshAmbient()
        }
    private var ambientScene: AmbientScene? = null
    private var ambientPlaying: AmbientScene? = null
    private var ambientPlayer: MediaPlayer? = null
    var hapticsEnabled = false
    var foreground = false
        set(value) {
            field = value
            if (!value) stop() else refreshAmbient()
        }
    private var closed = false
    private var lastCueAt = -1_000L
    private var lastCue: FeedbackCue? = null
    private val playbackObserver = object : AudioManager.AudioPlaybackCallback() {
        override fun onPlaybackConfigChanged(configs: MutableList<android.media.AudioPlaybackConfiguration>?) {
            refreshAmbient()
        }
    }

    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (status == 0 && !closed) loaded.add(sample)
        }
        samples = FeedbackCue.entries.associateWith { pool.load(app, it.sound, 1) }
        audio.registerAudioPlaybackCallback(playbackObserver, Handler(Looper.getMainLooper()))
    }

    override fun setAmbientScene(scene: AmbientScene?) {
        if (ambientScene == scene) return
        ambientScene = scene
        refreshAmbient()
    }

    fun recheckAmbientPolicy() = refreshAmbient()

    private fun otherMediaActive(): Boolean = runCatching { audio.activePlaybackConfigurations.any { config ->
            config.audioAttributes.usage in listOf(
                AudioAttributes.USAGE_MEDIA, AudioAttributes.USAGE_GAME,
            )
        } }.getOrDefault(audio.isMusicActive)

    /** Scene loops are opt-in, foreground-only and yield to other media. */
    private fun refreshAmbient() {
        if (closed) return
        val scene = ambientScene?.takeIf {
            soundsEnabled && ambientEnabled && foreground && !otherMediaActive() &&
                audio.ringerMode == AudioManager.RINGER_MODE_NORMAL &&
                audio.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0 &&
                notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL
        }
        if (scene == ambientPlaying) return
        stopAmbient()
        if (scene == null) return
        try {
            ambientPlayer = MediaPlayer.create(app, scene.sound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build(), 0)?.apply {
                isLooping = true
                setVolume(.16f, .16f)
                start()
            }
            if (ambientPlayer != null) ambientPlaying = scene
        } catch (_: Exception) {
            stopAmbient()
        }
    }

    private fun stopAmbient() {
        ambientPlaying = null
        ambientPlayer?.release()
        ambientPlayer = null
    }

    override fun play(cue: FeedbackCue) {
        if (closed || !foreground) return
        val now = SystemClock.uptimeMillis()
        val elapsed = now - lastCueAt
        if (cue == lastCue && elapsed < 100) return
        // A navigation click must not mask a result or the beginning of a run.
        if (cue == FeedbackCue.Tap && elapsed < 180) return
        lastCue = cue
        lastCueAt = now
        if (hapticsEnabled) {
            val effect = when (cue) {
                FeedbackCue.Tap, FeedbackCue.Select, FeedbackCue.Back,
                FeedbackCue.Toggle, FeedbackCue.BackgroundSwitch -> HapticFeedbackConstants.CLOCK_TICK
                FeedbackCue.Error, FeedbackCue.DrawFail -> HapticFeedbackConstants.LONG_PRESS
                else -> HapticFeedbackConstants.CONTEXT_CLICK
            }
            view.performHapticFeedback(effect)
        }
        val conditions = SoundConditions(
            enabled = soundsEnabled, foreground = foreground,
            ringerNormal = audio.ringerMode == AudioManager.RINGER_MODE_NORMAL,
            interruptionsAllowed = notifications.currentInterruptionFilter == NotificationManager.INTERRUPTION_FILTER_ALL,
            systemEffectsEnabled = Settings.System.getInt(app.contentResolver, Settings.System.SOUND_EFFECTS_ENABLED, 1) != 0,
            audibleVolume = audio.getStreamVolume(AudioManager.STREAM_SYSTEM) > 0,
            musicPlaying = otherMediaActive(),
        )
        if (!conditions.mayPlay) return
        val sample = samples[cue]?.takeIf { it in loaded } ?: return
        if (streams.size >= 2) pool.stop(streams.removeFirst())
        val stream = pool.play(sample, cue.gain, cue.gain, 1, 0, 1f)
        if (stream != 0) streams.addLast(stream)
    }

    fun stop() {
        streams.forEach(pool::stop)
        streams.clear()
        stopAmbient()
    }

    override fun close() {
        if (closed) return
        closed = true
        stop()
        audio.unregisterAudioPlaybackCallback(playbackObserver)
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}
