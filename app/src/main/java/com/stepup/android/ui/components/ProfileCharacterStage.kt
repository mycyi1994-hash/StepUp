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
    val render = AvatarArtCatalog.resolve(look, AvatarPose.SIT)
    if (!render.exactPose || !render.lookShown) {
        CharacterStage(
            look = look, pose = AvatarPose.IDLE, skyline = false,
            animate = false, characterFraction = 0.95f,
            contentDescription = contentDescription,
            modifier = modifier.testTag("profile-character-standing"),
        )
        return
    }

    BoxWithConstraints(
        modifier.testTag("profile-character-seated").then(
            if (contentDescription != null) Modifier.semantics {
                this.contentDescription = contentDescription
            } else Modifier,
        ),
    ) {
        val sceneSize = minOf(maxWidth, maxHeight)
        // Shared 1536 × 1536 composition frame; bench source is 1536 × 1024,
        // portrait source is 1024 × 1536. All offsets refer to these source frames.
        val benchTop = if (look.gender == AvatarGender.FEMALE) 440f else 480f
        Box(Modifier.align(Alignment.BottomCenter).size(sceneSize)) {
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.63f, size.height * 0.958f)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.Black.copy(alpha = 0.35f), Color.Transparent),
                        center = center,
                        radius = size.width * 0.27f,
                    ),
                    topLeft = Offset(size.width * 0.38f, size.height * 0.932f),
                    size = Size(size.width * 0.5f, size.height * 0.05f),
                )
            }
            Image(
                painter = painterResource(R.drawable.prop_profile_bench_v2),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .offset(y = sceneSize * (benchTop / 1536f))
                    .size(width = sceneSize, height = sceneSize * (1024f / 1536f)),
            )
            AvatarImage(
                art = render.art,
                modifier = Modifier
                    .offset(x = sceneSize * (256f / 1536f))
                    .size(width = sceneSize * (1024f / 1536f), height = sceneSize),
            )
        }
    }
}
