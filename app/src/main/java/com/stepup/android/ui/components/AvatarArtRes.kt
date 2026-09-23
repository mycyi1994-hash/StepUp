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
