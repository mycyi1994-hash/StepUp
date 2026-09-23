package com.stepup.android.ui.components

import androidx.annotation.DrawableRes
import com.stepup.android.R
import com.stepup.android.domain.AvatarArt

// tools/gen_avatar_res.py 가 만든 파일 — 손으로 고치지 않는다.

/** 캐릭터 그림의 리소스. 없는 열쇠는 null — 화면은 그 그림을 고르지 않는다. */
@DrawableRes
fun avatarArtResOrNull(key: String): Int? = when (key) {
    "female_idle" -> R.drawable.avatar_female_idle
    "lumi_idle_base_wnd_010" -> R.drawable.avatar_lumi_idle_base_wnd_010
    "lumi_idle_fir_001" -> R.drawable.avatar_lumi_idle_fir_001
    "lumi_idle_fir_002" -> R.drawable.avatar_lumi_idle_fir_002
    "lumi_idle_fir_003" -> R.drawable.avatar_lumi_idle_fir_003
    "lumi_idle_fir_004" -> R.drawable.avatar_lumi_idle_fir_004
    "lumi_idle_fir_005" -> R.drawable.avatar_lumi_idle_fir_005
    "lumi_idle_fir_006" -> R.drawable.avatar_lumi_idle_fir_006
    "lumi_idle_fir_007" -> R.drawable.avatar_lumi_idle_fir_007
    "lumi_idle_fir_008" -> R.drawable.avatar_lumi_idle_fir_008
    "lumi_idle_fir_009" -> R.drawable.avatar_lumi_idle_fir_009
    "lumi_idle_fir_010" -> R.drawable.avatar_lumi_idle_fir_010
    "lumi_idle_fir_011" -> R.drawable.avatar_lumi_idle_fir_011
    "lumi_idle_fir_012" -> R.drawable.avatar_lumi_idle_fir_012
    "lumi_idle_fir_013" -> R.drawable.avatar_lumi_idle_fir_013
    "lumi_idle_lit_001" -> R.drawable.avatar_lumi_idle_lit_001
    "lumi_idle_lit_002" -> R.drawable.avatar_lumi_idle_lit_002
    "lumi_idle_lit_003" -> R.drawable.avatar_lumi_idle_lit_003
    "lumi_idle_lit_004" -> R.drawable.avatar_lumi_idle_lit_004
    "lumi_idle_lit_005" -> R.drawable.avatar_lumi_idle_lit_005
    "lumi_idle_lit_006" -> R.drawable.avatar_lumi_idle_lit_006
    "lumi_idle_lit_007" -> R.drawable.avatar_lumi_idle_lit_007
    "lumi_idle_lit_008" -> R.drawable.avatar_lumi_idle_lit_008
    "lumi_idle_lit_009" -> R.drawable.avatar_lumi_idle_lit_009
    "lumi_idle_lit_010" -> R.drawable.avatar_lumi_idle_lit_010
    "lumi_idle_lit_011" -> R.drawable.avatar_lumi_idle_lit_011
    "lumi_idle_lit_012" -> R.drawable.avatar_lumi_idle_lit_012
    "lumi_idle_lit_013" -> R.drawable.avatar_lumi_idle_lit_013
    "lumi_idle_lum_clo_001" -> R.drawable.avatar_lumi_idle_lum_clo_001
    "lumi_idle_lum_clo_002" -> R.drawable.avatar_lumi_idle_lum_clo_002
    "lumi_idle_lum_clo_003" -> R.drawable.avatar_lumi_idle_lum_clo_003
    "lumi_idle_lum_clo_004" -> R.drawable.avatar_lumi_idle_lum_clo_004
    "lumi_idle_lum_clo_005" -> R.drawable.avatar_lumi_idle_lum_clo_005
    "lumi_idle_wat_001" -> R.drawable.avatar_lumi_idle_wat_001
    "lumi_idle_wat_002" -> R.drawable.avatar_lumi_idle_wat_002
    "lumi_idle_wat_003" -> R.drawable.avatar_lumi_idle_wat_003
    "lumi_idle_wat_004" -> R.drawable.avatar_lumi_idle_wat_004
    "lumi_idle_wat_005" -> R.drawable.avatar_lumi_idle_wat_005
    "lumi_idle_wat_006" -> R.drawable.avatar_lumi_idle_wat_006
    "lumi_idle_wat_007" -> R.drawable.avatar_lumi_idle_wat_007
    "lumi_idle_wat_008" -> R.drawable.avatar_lumi_idle_wat_008
    "lumi_idle_wat_009" -> R.drawable.avatar_lumi_idle_wat_009
    "lumi_idle_wat_010" -> R.drawable.avatar_lumi_idle_wat_010
    "lumi_idle_wat_011" -> R.drawable.avatar_lumi_idle_wat_011
    "lumi_idle_wat_012" -> R.drawable.avatar_lumi_idle_wat_012
    "lumi_idle_wat_013" -> R.drawable.avatar_lumi_idle_wat_013
    "lumi_idle_wnd_001" -> R.drawable.avatar_lumi_idle_wnd_001
    "lumi_idle_wnd_002" -> R.drawable.avatar_lumi_idle_wnd_002
    "lumi_idle_wnd_003" -> R.drawable.avatar_lumi_idle_wnd_003
    "lumi_idle_wnd_004" -> R.drawable.avatar_lumi_idle_wnd_004
    "lumi_idle_wnd_005" -> R.drawable.avatar_lumi_idle_wnd_005
    "lumi_idle_wnd_006" -> R.drawable.avatar_lumi_idle_wnd_006
    "lumi_idle_wnd_007" -> R.drawable.avatar_lumi_idle_wnd_007
    "lumi_idle_wnd_008" -> R.drawable.avatar_lumi_idle_wnd_008
    "lumi_idle_wnd_009" -> R.drawable.avatar_lumi_idle_wnd_009
    "lumi_idle_wnd_010" -> R.drawable.avatar_lumi_idle_wnd_010
    "lumi_idle_wnd_011" -> R.drawable.avatar_lumi_idle_wnd_011
    "lumi_idle_wnd_012" -> R.drawable.avatar_lumi_idle_wnd_012
    "lumi_idle_wnd_013" -> R.drawable.avatar_lumi_idle_wnd_013
    "male_idle" -> R.drawable.avatar_male_idle
    "male_running" -> R.drawable.avatar_male_running
    "runo_idle_clo_001" -> R.drawable.avatar_runo_idle_clo_001
    "runo_idle_clo_002" -> R.drawable.avatar_runo_idle_clo_002
    "runo_idle_clo_003" -> R.drawable.avatar_runo_idle_clo_003
    "runo_idle_clo_004" -> R.drawable.avatar_runo_idle_clo_004
    "runo_idle_clo_005" -> R.drawable.avatar_runo_idle_clo_005
    "runo_idle_fir_001" -> R.drawable.avatar_runo_idle_fir_001
    "runo_idle_fir_002" -> R.drawable.avatar_runo_idle_fir_002
    "runo_idle_fir_003" -> R.drawable.avatar_runo_idle_fir_003
    "runo_idle_fir_004" -> R.drawable.avatar_runo_idle_fir_004
    "runo_idle_fir_005" -> R.drawable.avatar_runo_idle_fir_005
    "runo_idle_fir_006" -> R.drawable.avatar_runo_idle_fir_006
    "runo_idle_fir_007" -> R.drawable.avatar_runo_idle_fir_007
    "runo_idle_fir_008" -> R.drawable.avatar_runo_idle_fir_008
    "runo_idle_fir_009" -> R.drawable.avatar_runo_idle_fir_009
    "runo_idle_fir_010" -> R.drawable.avatar_runo_idle_fir_010
    "runo_idle_fir_011" -> R.drawable.avatar_runo_idle_fir_011
    "runo_idle_fir_012" -> R.drawable.avatar_runo_idle_fir_012
    "runo_idle_fir_013" -> R.drawable.avatar_runo_idle_fir_013
    "runo_idle_lit_001" -> R.drawable.avatar_runo_idle_lit_001
    "runo_idle_lit_002" -> R.drawable.avatar_runo_idle_lit_002
    "runo_idle_lit_003" -> R.drawable.avatar_runo_idle_lit_003
    "runo_idle_lit_004" -> R.drawable.avatar_runo_idle_lit_004
    "runo_idle_lit_005" -> R.drawable.avatar_runo_idle_lit_005
    "runo_idle_lit_006" -> R.drawable.avatar_runo_idle_lit_006
    "runo_idle_lit_007" -> R.drawable.avatar_runo_idle_lit_007
    "runo_idle_lit_008" -> R.drawable.avatar_runo_idle_lit_008
    "runo_idle_lit_009" -> R.drawable.avatar_runo_idle_lit_009
    "runo_idle_lit_010" -> R.drawable.avatar_runo_idle_lit_010
    "runo_idle_lit_011" -> R.drawable.avatar_runo_idle_lit_011
    "runo_idle_lit_012" -> R.drawable.avatar_runo_idle_lit_012
    "runo_idle_lit_013" -> R.drawable.avatar_runo_idle_lit_013
    "runo_idle_wat_001" -> R.drawable.avatar_runo_idle_wat_001
    "runo_idle_wat_002" -> R.drawable.avatar_runo_idle_wat_002
    "runo_idle_wat_003" -> R.drawable.avatar_runo_idle_wat_003
    "runo_idle_wat_004" -> R.drawable.avatar_runo_idle_wat_004
    "runo_idle_wat_005" -> R.drawable.avatar_runo_idle_wat_005
    "runo_idle_wat_006" -> R.drawable.avatar_runo_idle_wat_006
    "runo_idle_wat_007" -> R.drawable.avatar_runo_idle_wat_007
    "runo_idle_wat_008" -> R.drawable.avatar_runo_idle_wat_008
    "runo_idle_wat_009" -> R.drawable.avatar_runo_idle_wat_009
    "runo_idle_wat_010" -> R.drawable.avatar_runo_idle_wat_010
    "runo_idle_wat_011" -> R.drawable.avatar_runo_idle_wat_011
    "runo_idle_wat_012" -> R.drawable.avatar_runo_idle_wat_012
    "runo_idle_wat_013" -> R.drawable.avatar_runo_idle_wat_013
    "runo_idle_wnd_001" -> R.drawable.avatar_runo_idle_wnd_001
    "runo_idle_wnd_002" -> R.drawable.avatar_runo_idle_wnd_002
    "runo_idle_wnd_003" -> R.drawable.avatar_runo_idle_wnd_003
    "runo_idle_wnd_004" -> R.drawable.avatar_runo_idle_wnd_004
    "runo_idle_wnd_005" -> R.drawable.avatar_runo_idle_wnd_005
    "runo_idle_wnd_006" -> R.drawable.avatar_runo_idle_wnd_006
    "runo_idle_wnd_007" -> R.drawable.avatar_runo_idle_wnd_007
    "runo_idle_wnd_008" -> R.drawable.avatar_runo_idle_wnd_008
    "runo_idle_wnd_009" -> R.drawable.avatar_runo_idle_wnd_009
    "runo_idle_wnd_010" -> R.drawable.avatar_runo_idle_wnd_010
    "runo_idle_wnd_010_v2" -> R.drawable.avatar_runo_idle_wnd_010_v2
    "runo_idle_wnd_011" -> R.drawable.avatar_runo_idle_wnd_011
    "runo_idle_wnd_012" -> R.drawable.avatar_runo_idle_wnd_012
    "runo_idle_wnd_013" -> R.drawable.avatar_runo_idle_wnd_013
    else -> null
}

@DrawableRes
fun AvatarArt.drawableRes(): Int = avatarArtResOrNull(key) ?: R.drawable.avatar_male_idle

/** 의상 상품 그림 — 캐릭터별 디자인 번호로 찾는다. 기본 의상은 없다 */
@DrawableRes
fun outfitProductRes(outfitId: String): Int? = when (outfitId) {
    "CLO-001" -> R.drawable.outfit_clo_001
    "CLO-002" -> R.drawable.outfit_clo_002
    "CLO-003" -> R.drawable.outfit_clo_003
    "CLO-004" -> R.drawable.outfit_clo_004
    "CLO-005" -> R.drawable.outfit_clo_005
    "LUM-CLO-001" -> R.drawable.outfit_lum_clo_001
    "LUM-CLO-002" -> R.drawable.outfit_lum_clo_002
    "LUM-CLO-003" -> R.drawable.outfit_lum_clo_003
    "LUM-CLO-004" -> R.drawable.outfit_lum_clo_004
    "LUM-CLO-005" -> R.drawable.outfit_lum_clo_005
    else -> null
}

/** Source aspect and transparent space below the feet; measured from alpha, never a screen offset. */
data class AvatarArtGeometry(val aspectRatio: Float, val bottomInsetFraction: Float)
fun AvatarArt.geometry(): AvatarArtGeometry = when (key) {
    "female_idle" -> AvatarArtGeometry(0.66666667f, 0.01367188f)
    "lumi_idle_base_wnd_010" -> AvatarArtGeometry(0.66666667f, 0.01302083f)
    "lumi_idle_fir_001" -> AvatarArtGeometry(0.64084507f, 0.00821596f)
    "lumi_idle_fir_002" -> AvatarArtGeometry(0.63380282f, 0.01056338f)
    "lumi_idle_fir_003" -> AvatarArtGeometry(0.63732394f, 0.01056338f)
    "lumi_idle_fir_004" -> AvatarArtGeometry(0.64436620f, 0.00352113f)
    "lumi_idle_fir_005" -> AvatarArtGeometry(0.64912281f, 0.01403509f)
    "lumi_idle_fir_006" -> AvatarArtGeometry(0.63157895f, 0.01403509f)
    "lumi_idle_fir_007" -> AvatarArtGeometry(0.63157895f, 0.01403509f)
    "lumi_idle_fir_008" -> AvatarArtGeometry(0.63859649f, 0.00467836f)
    "lumi_idle_fir_009" -> AvatarArtGeometry(0.65836299f, 0.00711744f)
    "lumi_idle_fir_010" -> AvatarArtGeometry(0.63701068f, 0.00711744f)
    "lumi_idle_fir_011" -> AvatarArtGeometry(0.64412811f, 0.00711744f)
    "lumi_idle_fir_012" -> AvatarArtGeometry(0.64768683f, 0.00000000f)
    "lumi_idle_fir_013" -> AvatarArtGeometry(0.69921875f, 0.00000000f)
    "lumi_idle_lit_001" -> AvatarArtGeometry(0.59848485f, 0.00757576f)
    "lumi_idle_lit_002" -> AvatarArtGeometry(0.60606061f, 0.00000000f)
    "lumi_idle_lit_003" -> AvatarArtGeometry(0.60606061f, 0.00757576f)
    "lumi_idle_lit_004" -> AvatarArtGeometry(0.59090909f, 0.01136364f)
    "lumi_idle_lit_005" -> AvatarArtGeometry(0.61886792f, 0.00754717f)
    "lumi_idle_lit_006" -> AvatarArtGeometry(0.61509434f, 0.00754717f)
    "lumi_idle_lit_007" -> AvatarArtGeometry(0.62641509f, 0.00377358f)
    "lumi_idle_lit_008" -> AvatarArtGeometry(0.60754717f, 0.00377358f)
    "lumi_idle_lit_009" -> AvatarArtGeometry(0.61977186f, 0.00760456f)
    "lumi_idle_lit_010" -> AvatarArtGeometry(0.61216730f, 0.00380228f)
    "lumi_idle_lit_011" -> AvatarArtGeometry(0.61596958f, 0.00760456f)
    "lumi_idle_lit_012" -> AvatarArtGeometry(0.60836502f, 0.00380228f)
    "lumi_idle_lit_013" -> AvatarArtGeometry(0.60885609f, 0.02214022f)
    "lumi_idle_lum_clo_001" -> AvatarArtGeometry(0.58171745f, 0.01108033f)
    "lumi_idle_lum_clo_002" -> AvatarArtGeometry(0.58448753f, 0.01108033f)
    "lumi_idle_lum_clo_003" -> AvatarArtGeometry(0.58171745f, 0.01108033f)
    "lumi_idle_lum_clo_004" -> AvatarArtGeometry(0.57894737f, 0.01108033f)
    "lumi_idle_lum_clo_005" -> AvatarArtGeometry(0.57894737f, 0.01108033f)
    "lumi_idle_wat_001" -> AvatarArtGeometry(0.62592593f, 0.00000000f)
    "lumi_idle_wat_002" -> AvatarArtGeometry(0.62222222f, 0.00000000f)
    "lumi_idle_wat_003" -> AvatarArtGeometry(0.63333333f, 0.00000000f)
    "lumi_idle_wat_004" -> AvatarArtGeometry(0.64074074f, 0.00000000f)
    "lumi_idle_wat_005" -> AvatarArtGeometry(0.64905660f, 0.00000000f)
    "lumi_idle_wat_006" -> AvatarArtGeometry(0.63773585f, 0.00000000f)
    "lumi_idle_wat_007" -> AvatarArtGeometry(0.65283019f, 0.00000000f)
    "lumi_idle_wat_008" -> AvatarArtGeometry(0.65283019f, 0.00000000f)
    "lumi_idle_wat_009" -> AvatarArtGeometry(0.63878327f, 0.00000000f)
    "lumi_idle_wat_010" -> AvatarArtGeometry(0.62737643f, 0.00000000f)
    "lumi_idle_wat_011" -> AvatarArtGeometry(0.64638783f, 0.00000000f)
    "lumi_idle_wat_012" -> AvatarArtGeometry(0.66920152f, 0.00000000f)
    "lumi_idle_wat_013" -> AvatarArtGeometry(0.61623616f, 0.00000000f)
    "lumi_idle_wnd_001" -> AvatarArtGeometry(0.54243542f, 0.02214022f)
    "lumi_idle_wnd_002" -> AvatarArtGeometry(0.54044118f, 0.02205882f)
    "lumi_idle_wnd_003" -> AvatarArtGeometry(0.54044118f, 0.02205882f)
    "lumi_idle_wnd_004" -> AvatarArtGeometry(0.55147059f, 0.02205882f)
    "lumi_idle_wnd_005" -> AvatarArtGeometry(0.54243542f, 0.02214022f)
    "lumi_idle_wnd_006" -> AvatarArtGeometry(0.54981550f, 0.02214022f)
    "lumi_idle_wnd_007" -> AvatarArtGeometry(0.54243542f, 0.02214022f)
    "lumi_idle_wnd_008" -> AvatarArtGeometry(0.55147059f, 0.02205882f)
    "lumi_idle_wnd_009" -> AvatarArtGeometry(0.54612546f, 0.02214022f)
    "lumi_idle_wnd_010" -> AvatarArtGeometry(0.54411765f, 0.02205882f)
    "lumi_idle_wnd_011" -> AvatarArtGeometry(0.53308824f, 0.02205882f)
    "lumi_idle_wnd_012" -> AvatarArtGeometry(0.54945055f, 0.02319902f)
    "lumi_idle_wnd_013" -> AvatarArtGeometry(0.58039216f, 0.00000000f)
    "male_idle" -> AvatarArtGeometry(0.53926702f, 0.01221640f)
    "male_running" -> AvatarArtGeometry(0.71428571f, 0.01549865f)
    "runo_idle_clo_001" -> AvatarArtGeometry(0.53038674f, 0.00000000f)
    "runo_idle_clo_002" -> AvatarArtGeometry(0.52892562f, 0.00000000f)
    "runo_idle_clo_003" -> AvatarArtGeometry(0.51239669f, 0.00000000f)
    "runo_idle_clo_004" -> AvatarArtGeometry(0.50137741f, 0.00000000f)
    "runo_idle_clo_005" -> AvatarArtGeometry(0.50828729f, 0.00000000f)
    "runo_idle_fir_001" -> AvatarArtGeometry(0.51211073f, 0.01038062f)
    "runo_idle_fir_002" -> AvatarArtGeometry(0.51379310f, 0.00689655f)
    "runo_idle_fir_003" -> AvatarArtGeometry(0.51211073f, 0.00692042f)
    "runo_idle_fir_004" -> AvatarArtGeometry(0.50000000f, 0.01034483f)
    "runo_idle_fir_005" -> AvatarArtGeometry(0.52068966f, 0.01034483f)
    "runo_idle_fir_006" -> AvatarArtGeometry(0.51557093f, 0.01038062f)
    "runo_idle_fir_007" -> AvatarArtGeometry(0.50344828f, 0.00689655f)
    "runo_idle_fir_008" -> AvatarArtGeometry(0.49655172f, 0.00689655f)
    "runo_idle_fir_009" -> AvatarArtGeometry(0.51379310f, 0.01034483f)
    "runo_idle_fir_010" -> AvatarArtGeometry(0.51034483f, 0.01034483f)
    "runo_idle_fir_011" -> AvatarArtGeometry(0.50865052f, 0.00692042f)
    "runo_idle_fir_012" -> AvatarArtGeometry(0.50344828f, 0.00689655f)
    "runo_idle_fir_013" -> AvatarArtGeometry(0.51724138f, 0.01034483f)
    "runo_idle_lit_001" -> AvatarArtGeometry(0.53237410f, 0.02517986f)
    "runo_idle_lit_002" -> AvatarArtGeometry(0.53429603f, 0.02527076f)
    "runo_idle_lit_003" -> AvatarArtGeometry(0.52877698f, 0.02517986f)
    "runo_idle_lit_004" -> AvatarArtGeometry(0.53623188f, 0.02536232f)
    "runo_idle_lit_005" -> AvatarArtGeometry(0.52857143f, 0.02500000f)
    "runo_idle_lit_006" -> AvatarArtGeometry(0.52688172f, 0.02508961f)
    "runo_idle_lit_007" -> AvatarArtGeometry(0.52669039f, 0.02491103f)
    "runo_idle_lit_008" -> AvatarArtGeometry(0.52500000f, 0.02500000f)
    "runo_idle_lit_009" -> AvatarArtGeometry(0.53260870f, 0.02536232f)
    "runo_idle_lit_010" -> AvatarArtGeometry(0.53454545f, 0.02545455f)
    "runo_idle_lit_011" -> AvatarArtGeometry(0.53454545f, 0.02545455f)
    "runo_idle_lit_012" -> AvatarArtGeometry(0.53818182f, 0.02545455f)
    "runo_idle_lit_013" -> AvatarArtGeometry(0.52688172f, 0.02508961f)
    "runo_idle_wat_001" -> AvatarArtGeometry(0.53623188f, 0.02536232f)
    "runo_idle_wat_002" -> AvatarArtGeometry(0.52517986f, 0.02517986f)
    "runo_idle_wat_003" -> AvatarArtGeometry(0.52517986f, 0.02517986f)
    "runo_idle_wat_004" -> AvatarArtGeometry(0.51449275f, 0.02536232f)
    "runo_idle_wat_005" -> AvatarArtGeometry(0.52313167f, 0.02491103f)
    "runo_idle_wat_006" -> AvatarArtGeometry(0.51601423f, 0.02491103f)
    "runo_idle_wat_007" -> AvatarArtGeometry(0.51601423f, 0.02491103f)
    "runo_idle_wat_008" -> AvatarArtGeometry(0.51245552f, 0.02491103f)
    "runo_idle_wat_009" -> AvatarArtGeometry(0.52482270f, 0.02482270f)
    "runo_idle_wat_010" -> AvatarArtGeometry(0.52482270f, 0.02482270f)
    "runo_idle_wat_011" -> AvatarArtGeometry(0.51773050f, 0.02482270f)
    "runo_idle_wat_012" -> AvatarArtGeometry(0.52127660f, 0.02482270f)
    "runo_idle_wat_013" -> AvatarArtGeometry(0.53214286f, 0.02500000f)
    "runo_idle_wnd_001" -> AvatarArtGeometry(0.50357143f, 0.02500000f)
    "runo_idle_wnd_002" -> AvatarArtGeometry(0.49110320f, 0.02491103f)
    "runo_idle_wnd_003" -> AvatarArtGeometry(0.48928571f, 0.02500000f)
    "runo_idle_wnd_004" -> AvatarArtGeometry(0.48571429f, 0.02500000f)
    "runo_idle_wnd_005" -> AvatarArtGeometry(0.49122807f, 0.02456140f)
    "runo_idle_wnd_006" -> AvatarArtGeometry(0.48591549f, 0.02464789f)
    "runo_idle_wnd_007" -> AvatarArtGeometry(0.48771930f, 0.02456140f)
    "runo_idle_wnd_008" -> AvatarArtGeometry(0.48070175f, 0.02456140f)
    "runo_idle_wnd_009" -> AvatarArtGeometry(0.49295775f, 0.02464789f)
    "runo_idle_wnd_010" -> AvatarArtGeometry(0.49473684f, 0.02456140f)
    "runo_idle_wnd_010_v2" -> AvatarArtGeometry(0.66666667f, 0.03906250f)
    "runo_idle_wnd_011" -> AvatarArtGeometry(0.48771930f, 0.02456140f)
    "runo_idle_wnd_012" -> AvatarArtGeometry(0.49116608f, 0.02473498f)
    "runo_idle_wnd_013" -> AvatarArtGeometry(0.49650350f, 0.02447552f)
    else -> AvatarArtGeometry(1f, 0f)
}
