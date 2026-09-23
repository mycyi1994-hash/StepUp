"""res/drawable-nodpi 의 캐릭터 · 의상 그림으로 AvatarArtRes.kt 를 만든다.

그림 이름 규칙
  avatar_<key>.webp   — AvatarArt.key 가 <key> 인 캐릭터 그림
  outfit_clo_NNN.webp — CLO-NNN 의상 상품 그림(RUNO)
  outfit_lum_clo_NNN.webp — LUM-CLO-NNN 의상 상품 그림(LUMI, 모자 포함)
"""
from pathlib import Path
from PIL import Image
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res/drawable-nodpi'
OUT = ROOT / 'app/src/main/java/com/stepup/android/ui/components/AvatarArtRes.kt'
avatars = sorted(p.stem for p in RES.glob('avatar_*.webp'))
outfits = sorted(p.stem for p in RES.glob('outfit_*clo_*.webp'))
lines = [
    'package com.stepup.android.ui.components',
    '',
    'import androidx.annotation.DrawableRes',
    'import com.stepup.android.R',
    'import com.stepup.android.domain.AvatarArt',
    '',
    '// tools/gen_avatar_res.py 가 만든 파일 — 손으로 고치지 않는다.',
    '',
    '/** 캐릭터 그림의 리소스. 없는 열쇠는 null — 화면은 그 그림을 고르지 않는다. */',
    '@DrawableRes',
    'fun avatarArtResOrNull(key: String): Int? = when (key) {',
]
for a in avatars:
    lines.append(f'    "{a[len("avatar_"):]}" -> R.drawable.{a}')
lines += ['    else -> null', '}', '',
          '@DrawableRes',
          'fun AvatarArt.drawableRes(): Int = avatarArtResOrNull(key) ?: R.drawable.avatar_male_idle',
          '',
          '/** 의상 상품 그림 — 캐릭터별 디자인 번호로 찾는다. 기본 의상은 없다 */',
          '@DrawableRes',
          'fun outfitProductRes(outfitId: String): Int? = when (outfitId) {']
for o in outfits:
    # outfit_clo_001 → "CLO-001", outfit_lum_clo_001 → "LUM-CLO-001"
    lines.append(f'    "{o[len("outfit_"):].upper().replace("_", "-")}" -> R.drawable.{o}')
lines += ['    else -> null', '}', '',
          '/** Source aspect and transparent space below the feet; measured from alpha, never a screen offset. */',
          'data class AvatarArtGeometry(val aspectRatio: Float, val bottomInsetFraction: Float)',
          'fun AvatarArt.geometry(): AvatarArtGeometry = when (key) {']
for a in avatars:
    with Image.open(RES / (a + '.webp')) as im:
        # Ignore faint edge glow when finding the physical silhouette. Read only: art is unchanged.
        bounds = im.getchannel('A').point(lambda alpha: 255 if alpha > 128 else 0).getbbox()
        if bounds is None:
            raise ValueError(f'No visible silhouette: {a}')
        lines.append(f'    "{a[len("avatar_"):]}" -> AvatarArtGeometry({im.width / im.height:.8f}f, {(im.height - bounds[3]) / im.height:.8f}f)')
lines += ['    else -> AvatarArtGeometry(1f, 0f)', '}', '']
OUT.write_text('\n'.join(lines), encoding='utf-8')
print(f'{len(avatars)} avatars, {len(outfits)} outfit products -> {OUT.relative_to(ROOT)}')
