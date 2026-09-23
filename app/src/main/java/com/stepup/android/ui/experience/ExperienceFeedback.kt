package com.stepup.android.ui.experience

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.SystemClock
import android.provider.Settings
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.RawRes
import com.stepup.android.R

enum class FeedbackCue(@RawRes val sound: Int, val gain: Float = .55f) {
    Tap(R.raw.cue_tap, .28f), Select(R.raw.cue_select, .35f),
    Success(R.raw.cue_success), Reward(R.raw.cue_reward, .62f),
    Start(R.raw.cue_start), Pause(R.raw.cue_pause), Lap(R.raw.cue_lap),
    Countdown(R.raw.cue_countdown), Error(R.raw.cue_error, .40f),
}

/** Activity-owned short sounds. Never starts music, changes volume, or requests audio focus. */
class ExperienceFeedback(context: Context, private val view: View) : AutoCloseable {
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
    var hapticsEnabled = false
    var foreground = false
        set(value) {
            field = value
            if (!value) stop()
        }
    private var closed = false
    private var lastCueAt = -1_000L
    private var lastCue: FeedbackCue? = null

    init {
        pool.setOnLoadCompleteListener { _, sample, status ->
            if (status == 0 && !closed) loaded.add(sample)
        }
        samples = FeedbackCue.entries.associateWith { pool.load(app, it.sound, 1) }
    }

    fun play(cue: FeedbackCue) {
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
                FeedbackCue.Tap, FeedbackCue.Select -> HapticFeedbackConstants.CLOCK_TICK
                FeedbackCue.Error -> HapticFeedbackConstants.LONG_PRESS
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
            musicPlaying = audio.isMusicActive,
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
    }

    override fun close() {
        if (closed) return
        closed = true
        stop()
        pool.setOnLoadCompleteListener(null)
        pool.release()
        loaded.clear()
    }
}
