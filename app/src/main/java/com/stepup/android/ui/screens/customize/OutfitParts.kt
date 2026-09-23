package com.stepup.android.ui.screens.customize

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import com.stepup.android.R
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Outfits
import com.stepup.android.ui.components.RunnerAvatar

@StringRes
fun outfitNameRes(outfit: Outfit): Int = when (outfit.id) {
    Outfits.STARTER_HOODIE.id -> R.string.outfit_starter_hoodie
    Outfits.STARTER_TEE.id -> R.string.outfit_starter_tee
    Outfits.UP_HOODIE.id -> R.string.outfit_up_hoodie
    Outfits.SPORTY_JACKET.id -> R.string.outfit_sporty_jacket
    Outfits.NEON_TRACK.id -> R.string.outfit_neon_track
    Outfits.STORM_SHELL.id -> R.string.outfit_storm_shell
    else -> R.string.outfit_starter_hoodie
}

/**
 * 의상 썸네일 — 그 옷을 입은 캐릭터의 윗몸.
 *
 * 옷만 따로 그리면 입었을 때 어떻게 보일지 짐작해야 한다. 같은 캐릭터가
 * 입은 모습을 잘라 보여 주면 짐작할 것이 없다.
 */
@Composable
fun OutfitThumb(look: AvatarLook, outfit: Outfit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.clipToBounds(), contentAlignment = Alignment.Center) {
        val w = maxWidth
        // 캐릭터를 칸보다 크게 그리고 위로 올려 몸통(모자 아래 ~ 허리)이 가운데 오게 한다
        RunnerAvatar(
            look = look.copy(outfit = outfit, trial = false),
            modifier = Modifier
                .requiredSize(w * 1.35f, w * 1.35f * 1.42f)
                .offset(y = w * 0.12f),
            showGround = false,
            animate = false,
        )
    }
}
