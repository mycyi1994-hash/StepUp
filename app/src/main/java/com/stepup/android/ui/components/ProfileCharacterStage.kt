package com.stepup.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.stepup.android.R
import com.stepup.android.domain.AvatarArtCatalog
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.AvatarPose

/**
 * Profile scene layers: root-owned scenery → contact shadow → bench → equipped portrait.
 * The seated composition scales as one square, preserving contact points on small screens.
 * Clothing/shoes within each portrait are still a complete image, not separate equipment layers.
 */
@Composable
fun ProfileCharacterStage(
    look: AvatarLook,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    // Legacy signature retained for saved profile compatibility; no mascot art is packaged.
    CharacterStage(look = look, pose = AvatarPose.IDLE, modifier = modifier, contentDescription = contentDescription)
}
