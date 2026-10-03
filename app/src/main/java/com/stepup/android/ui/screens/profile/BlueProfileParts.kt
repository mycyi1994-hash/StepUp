package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.ui.components.AvatarEmojis
import com.stepup.android.ui.components.avatarEmoji
import com.stepup.android.ui.components.runTone

/**
 * 파란 톤 프로필 사진 원 — 갤러리 사진 · 기본 이미지(기존 16개 이모지, 번호 그대로) · 사진 파일이 없을 때의 사람 윤곽.
 * 사람 윤곽은 파일이 없을 때만의 대체 모양이다 — 저장된 기본 이미지나 사진을 덮지 않는다.
 */
@Composable
internal fun BlueAvatar(
    avatarId: Int,
    photo: ImageBitmap?,
    size: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    ring: Color? = null,
) {
    val t = runTone()
    val face = if (t.dark) listOf(Color(0xFF1B4278), Color(0xFF0A2246)) else listOf(Color(0xFFE6EFFF), Color(0xFFCADCFB))
    Box(
        modifier.size(size).clip(CircleShape)
            .background(Brush.linearGradient(face), CircleShape)
            .border(2.dp, ring ?: t.cyan.copy(alpha = 0.9f), CircleShape)
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatarId == UserPrefs.AVATAR_CUSTOM && photo != null ->
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(CircleShape))
            avatarId in AvatarEmojis.indices -> Text(avatarEmoji(avatarId), fontSize = (size.value * 0.42f).sp)
            else -> Icon(
                Icons.Outlined.Person, contentDescription = null,
                tint = if (t.dark) Color(0xFFCFE0FF) else t.cobalt, modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}
