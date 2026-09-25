package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import com.stepup.android.R
import com.stepup.android.domain.AvatarArt

// tools/gen_avatar_res.py 가 만든 파일 — 손으로 고치지 않는다.

/** 캐릭터 그림의 리소스. 없는 열쇠는 null — 화면은 그 그림을 고르지 않는다. */
@DrawableRes
fun avatarArtResOrNull(key: String): Int? = when (key) {
    else -> null
}

@DrawableRes
fun AvatarArt.drawableRes(): Int? = avatarArtResOrNull(key)

/** 의상 상품 그림 — 캐릭터별 디자인 번호로 찾는다. 기본 의상은 없다 */
@DrawableRes
fun outfitProductRes(outfitId: String): Int? = when (outfitId) {
    else -> null
}

/** Source aspect and transparent space below the feet; measured from alpha, never a screen offset. */
data class AvatarArtGeometry(val aspectRatio: Float, val bottomInsetFraction: Float)
fun AvatarArt.geometry(): AvatarArtGeometry = when (key) {
    else -> AvatarArtGeometry(1f, 0f)
}
