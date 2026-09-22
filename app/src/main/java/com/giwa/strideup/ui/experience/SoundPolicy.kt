package com.giwa.strideup.ui.experience

/** All gates are required; UI feedback must never override a quieter device setting. */
data class SoundConditions(
    val enabled: Boolean,
    val foreground: Boolean,
    val ringerNormal: Boolean,
    val interruptionsAllowed: Boolean,
    val systemEffectsEnabled: Boolean,
    val audibleVolume: Boolean,
    val musicPlaying: Boolean,
) {
    val mayPlay: Boolean get() = enabled && foreground && ringerNormal && interruptionsAllowed &&
        systemEffectsEnabled && audibleVolume && !musicPlaying
}
