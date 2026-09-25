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
    "lumi_idle_studio_lavender" -> R.drawable.avatar_lumi_idle_studio_lavender
    "lumi_idle_studio_olive" -> R.drawable.avatar_lumi_idle_studio_olive
    "lumi_idle_studio_pink" -> R.drawable.avatar_lumi_idle_studio_pink
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
    "lumi_sit_studio_pink_v2" -> R.drawable.avatar_lumi_sit_studio_pink_v2
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
    "runo_idle_studio_lavender" -> R.drawable.avatar_runo_idle_studio_lavender
    "runo_idle_studio_olive" -> R.drawable.avatar_runo_idle_studio_olive
    "runo_idle_studio_pink" -> R.drawable.avatar_runo_idle_studio_pink
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
    "runo_sit_studio_pink_v2" -> R.drawable.avatar_runo_sit_studio_pink_v2
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
    "STUDIO-LAVENDER" -> R.drawable.outfit_studio_lavender
    "STUDIO-OLIVE" -> R.drawable.outfit_studio_olive
    "STUDIO-PINK" -> R.drawable.outfit_studio_pink
    else -> null
}

/** Source aspect and transparent space below the feet; measured from alpha, never a screen offset. */
data class AvatarArtGeometry(val aspectRatio: Float, val bottomInsetFraction: Float)
fun AvatarArt.geometry(): AvatarArtGeometry = when (key) {
    "female_idle" -> AvatarArtGeometry(0.66666667f, 0.01367188f)
    "lumi_idle_base_wnd_010" -> AvatarArtGeometry(0.66666667f, 0.01302083f)
    "lumi_idle_fir_001" -> AvatarArtGeometry(0.64260563f, 0.01144366f)
    "lumi_idle_fir_002" -> AvatarArtGeometry(0.63116197f, 0.01144366f)
    "lumi_idle_fir_003" -> AvatarArtGeometry(0.63380282f, 0.01144366f)
    "lumi_idle_fir_004" -> AvatarArtGeometry(0.64172535f, 0.00792254f)
    "lumi_idle_fir_005" -> AvatarArtGeometry(0.64736842f, 0.01491228f)
    "lumi_idle_fir_006" -> AvatarArtGeometry(0.62807018f, 0.01491228f)
    "lumi_idle_fir_007" -> AvatarArtGeometry(0.63333333f, 0.01491228f)
    "lumi_idle_fir_008" -> AvatarArtGeometry(0.63333333f, 0.00964912f)
    "lumi_idle_fir_009" -> AvatarArtGeometry(0.65302491f, 0.00800712f)
    "lumi_idle_fir_010" -> AvatarArtGeometry(0.63701068f, 0.00800712f)
    "lumi_idle_fir_011" -> AvatarArtGeometry(0.64323843f, 0.00978648f)
    "lumi_idle_fir_012" -> AvatarArtGeometry(0.64501779f, 0.00000000f)
    "lumi_idle_fir_013" -> AvatarArtGeometry(0.69726562f, 0.00000000f)
    "lumi_idle_lit_001" -> AvatarArtGeometry(0.59943182f, 0.01231061f)
    "lumi_idle_lit_002" -> AvatarArtGeometry(0.60416667f, 0.00094697f)
    "lumi_idle_lit_003" -> AvatarArtGeometry(0.60700758f, 0.01325758f)
    "lumi_idle_lit_004" -> AvatarArtGeometry(0.59185606f, 0.01325758f)
    "lumi_idle_lit_005" -> AvatarArtGeometry(0.61886792f, 0.00849057f)
    "lumi_idle_lit_006" -> AvatarArtGeometry(0.61603774f, 0.00849057f)
    "lumi_idle_lit_007" -> AvatarArtGeometry(0.62547170f, 0.00660377f)
    "lumi_idle_lit_008" -> AvatarArtGeometry(0.60849057f, 0.00660377f)
    "lumi_idle_lit_009" -> AvatarArtGeometry(0.62167300f, 0.00855513f)
    "lumi_idle_lit_010" -> AvatarArtGeometry(0.61406844f, 0.00855513f)
    "lumi_idle_lit_011" -> AvatarArtGeometry(0.61692015f, 0.00855513f)
    "lumi_idle_lit_012" -> AvatarArtGeometry(0.60551331f, 0.00665399f)
    "lumi_idle_lit_013" -> AvatarArtGeometry(0.60737327f, 0.02396313f)
    "lumi_idle_lum_clo_001" -> AvatarArtGeometry(0.57756233f, 0.01177285f)
    "lumi_idle_lum_clo_002" -> AvatarArtGeometry(0.58033241f, 0.01108033f)
    "lumi_idle_lum_clo_003" -> AvatarArtGeometry(0.57686981f, 0.01108033f)
    "lumi_idle_lum_clo_004" -> AvatarArtGeometry(0.57617729f, 0.01108033f)
    "lumi_idle_lum_clo_005" -> AvatarArtGeometry(0.57479224f, 0.01177285f)
    "lumi_idle_studio_lavender" -> AvatarArtGeometry(0.66666667f, 0.01171875f)
    "lumi_idle_studio_olive" -> AvatarArtGeometry(0.66666667f, 0.01171875f)
    "lumi_idle_studio_pink" -> AvatarArtGeometry(0.66666667f, 0.01236979f)
    "lumi_idle_wat_001" -> AvatarArtGeometry(0.61481481f, 0.00000000f)
    "lumi_idle_wat_002" -> AvatarArtGeometry(0.61574074f, 0.00000000f)
    "lumi_idle_wat_003" -> AvatarArtGeometry(0.62592593f, 0.00000000f)
    "lumi_idle_wat_004" -> AvatarArtGeometry(0.63796296f, 0.00000000f)
    "lumi_idle_wat_005" -> AvatarArtGeometry(0.63867925f, 0.00000000f)
    "lumi_idle_wat_006" -> AvatarArtGeometry(0.62547170f, 0.00000000f)
    "lumi_idle_wat_007" -> AvatarArtGeometry(0.65471698f, 0.00000000f)
    "lumi_idle_wat_008" -> AvatarArtGeometry(0.65471698f, 0.00000000f)
    "lumi_idle_wat_009" -> AvatarArtGeometry(0.63212928f, 0.00000000f)
    "lumi_idle_wat_010" -> AvatarArtGeometry(0.62452471f, 0.00000000f)
    "lumi_idle_wat_011" -> AvatarArtGeometry(0.63973384f, 0.00000000f)
    "lumi_idle_wat_012" -> AvatarArtGeometry(0.66254753f, 0.00000000f)
    "lumi_idle_wat_013" -> AvatarArtGeometry(0.61808118f, 0.00000000f)
    "lumi_idle_wnd_001" -> AvatarArtGeometry(0.54285714f, 0.02396313f)
    "lumi_idle_wnd_002" -> AvatarArtGeometry(0.54086318f, 0.02387511f)
    "lumi_idle_wnd_003" -> AvatarArtGeometry(0.54086318f, 0.02387511f)
    "lumi_idle_wnd_004" -> AvatarArtGeometry(0.55004591f, 0.02387511f)
    "lumi_idle_wnd_005" -> AvatarArtGeometry(0.54285714f, 0.02396313f)
    "lumi_idle_wnd_006" -> AvatarArtGeometry(0.55023041f, 0.02396313f)
    "lumi_idle_wnd_007" -> AvatarArtGeometry(0.54285714f, 0.02396313f)
    "lumi_idle_wnd_008" -> AvatarArtGeometry(0.55096419f, 0.02387511f)
    "lumi_idle_wnd_009" -> AvatarArtGeometry(0.54654378f, 0.02396313f)
    "lumi_idle_wnd_010" -> AvatarArtGeometry(0.54361800f, 0.02387511f)
    "lumi_idle_wnd_011" -> AvatarArtGeometry(0.53351699f, 0.02387511f)
    "lumi_idle_wnd_012" -> AvatarArtGeometry(0.54820937f, 0.02387511f)
    "lumi_idle_wnd_013" -> AvatarArtGeometry(0.58137255f, 0.00000000f)
    "lumi_sit_studio_pink_v2" -> AvatarArtGeometry(0.66666667f, 0.05013021f)
    "male_idle" -> AvatarArtGeometry(0.53327534f, 0.01130926f)
    "male_running" -> AvatarArtGeometry(0.71428571f, 0.01549865f)
    "runo_idle_clo_001" -> AvatarArtGeometry(0.52080444f, 0.00000000f)
    "runo_idle_clo_002" -> AvatarArtGeometry(0.52493075f, 0.00000000f)
    "runo_idle_clo_003" -> AvatarArtGeometry(0.50969529f, 0.00000000f)
    "runo_idle_clo_004" -> AvatarArtGeometry(0.49791956f, 0.00000000f)
    "runo_idle_clo_005" -> AvatarArtGeometry(0.50799166f, 0.00000000f)
    "runo_idle_fir_001" -> AvatarArtGeometry(0.50866551f, 0.01039861f)
    "runo_idle_fir_002" -> AvatarArtGeometry(0.51255411f, 0.00692641f)
    "runo_idle_fir_003" -> AvatarArtGeometry(0.51126516f, 0.00779896f)
    "runo_idle_fir_004" -> AvatarArtGeometry(0.50000000f, 0.01038062f)
    "runo_idle_fir_005" -> AvatarArtGeometry(0.51989619f, 0.01038062f)
    "runo_idle_fir_006" -> AvatarArtGeometry(0.51386482f, 0.01039861f)
    "runo_idle_fir_007" -> AvatarArtGeometry(0.50346021f, 0.00692042f)
    "runo_idle_fir_008" -> AvatarArtGeometry(0.49568221f, 0.00690846f)
    "runo_idle_fir_009" -> AvatarArtGeometry(0.51384083f, 0.01038062f)
    "runo_idle_fir_010" -> AvatarArtGeometry(0.50993950f, 0.01037165f)
    "runo_idle_fir_011" -> AvatarArtGeometry(0.50823938f, 0.00693842f)
    "runo_idle_fir_012" -> AvatarArtGeometry(0.49956785f, 0.00691443f)
    "runo_idle_fir_013" -> AvatarArtGeometry(0.51643599f, 0.01038062f)
    "runo_idle_lit_001" -> AvatarArtGeometry(0.53539020f, 0.02359347f)
    "runo_idle_lit_002" -> AvatarArtGeometry(0.53442029f, 0.02355072f)
    "runo_idle_lit_003" -> AvatarArtGeometry(0.53272727f, 0.02363636f)
    "runo_idle_lit_004" -> AvatarArtGeometry(0.53545455f, 0.02363636f)
    "runo_idle_lit_005" -> AvatarArtGeometry(0.52920036f, 0.02336029f)
    "runo_idle_lit_006" -> AvatarArtGeometry(0.52607914f, 0.02338129f)
    "runo_idle_lit_007" -> AvatarArtGeometry(0.52415027f, 0.02325581f)
    "runo_idle_lit_008" -> AvatarArtGeometry(0.52560647f, 0.02336029f)
    "runo_idle_lit_009" -> AvatarArtGeometry(0.53369763f, 0.02367942f)
    "runo_idle_lit_010" -> AvatarArtGeometry(0.53467153f, 0.02372263f)
    "runo_idle_lit_011" -> AvatarArtGeometry(0.53467153f, 0.02372263f)
    "runo_idle_lit_012" -> AvatarArtGeometry(0.53839122f, 0.02376600f)
    "runo_idle_lit_013" -> AvatarArtGeometry(0.52840397f, 0.02344454f)
    "runo_idle_studio_lavender" -> AvatarArtGeometry(0.66666667f, 0.03776042f)
    "runo_idle_studio_olive" -> AvatarArtGeometry(0.66666667f, 0.03710938f)
    "runo_idle_studio_pink" -> AvatarArtGeometry(0.66666667f, 0.03841146f)
    "runo_idle_wat_001" -> AvatarArtGeometry(0.53685168f, 0.02365787f)
    "runo_idle_wat_002" -> AvatarArtGeometry(0.52536232f, 0.02355072f)
    "runo_idle_wat_003" -> AvatarArtGeometry(0.52402539f, 0.02357208f)
    "runo_idle_wat_004" -> AvatarArtGeometry(0.51363636f, 0.02363636f)
    "runo_idle_wat_005" -> AvatarArtGeometry(0.52057245f, 0.02325581f)
    "runo_idle_wat_006" -> AvatarArtGeometry(0.51699463f, 0.02325581f)
    "runo_idle_wat_007" -> AvatarArtGeometry(0.51745748f, 0.02327663f)
    "runo_idle_wat_008" -> AvatarArtGeometry(0.51387645f, 0.02327663f)
    "runo_idle_wat_009" -> AvatarArtGeometry(0.52678571f, 0.02321429f)
    "runo_idle_wat_010" -> AvatarArtGeometry(0.52274755f, 0.02319358f)
    "runo_idle_wat_011" -> AvatarArtGeometry(0.51607143f, 0.02321429f)
    "runo_idle_wat_012" -> AvatarArtGeometry(0.51875000f, 0.02321429f)
    "runo_idle_wat_013" -> AvatarArtGeometry(0.53183857f, 0.02331839f)
    "runo_idle_wnd_001" -> AvatarArtGeometry(0.50269300f, 0.02333932f)
    "runo_idle_wnd_002" -> AvatarArtGeometry(0.49327354f, 0.02331839f)
    "runo_idle_wnd_003" -> AvatarArtGeometry(0.49012567f, 0.02333932f)
    "runo_idle_wnd_004" -> AvatarArtGeometry(0.48653501f, 0.02333932f)
    "runo_idle_wnd_005" -> AvatarArtGeometry(0.49119718f, 0.02288732f)
    "runo_idle_wnd_006" -> AvatarArtGeometry(0.48629531f, 0.02298851f)
    "runo_idle_wnd_007" -> AvatarArtGeometry(0.48853616f, 0.02292769f)
    "runo_idle_wnd_008" -> AvatarArtGeometry(0.48190644f, 0.02294793f)
    "runo_idle_wnd_009" -> AvatarArtGeometry(0.49336870f, 0.02298851f)
    "runo_idle_wnd_010" -> AvatarArtGeometry(0.49602824f, 0.02294793f)
    "runo_idle_wnd_010_v2" -> AvatarArtGeometry(0.66666667f, 0.03906250f)
    "runo_idle_wnd_011" -> AvatarArtGeometry(0.48896734f, 0.02294793f)
    "runo_idle_wnd_012" -> AvatarArtGeometry(0.49157054f, 0.02307010f)
    "runo_idle_wnd_013" -> AvatarArtGeometry(0.49692172f, 0.02286719f)
    "runo_sit_studio_pink_v2" -> AvatarArtGeometry(0.66666667f, 0.02994792f)
    else -> AvatarArtGeometry(1f, 0f)
}
