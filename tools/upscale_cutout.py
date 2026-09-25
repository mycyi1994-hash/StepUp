"""떼어 낸 캐릭터를 AI 초해상도(Real-ESRGAN)로 4배 키운다.

모양(알파)은 원래 크기에서 떼어 낸 것을 쓰고, 색은 원본 칸을 Real-ESRGAN 으로
키운 것에서 가져온다. 가장자리에 남은 바탕색은 안쪽 색으로 덮는다.

  from upscale_cutout import upscale_cutout
  upscale_cutout(raw_png, alpha_png, dst_webp, cache_dir)

필요한 것: pip install realesrgan-ncnn-py, Vulkan 드라이버(CPU 는 mesa 의 lavapipe —
VK_ICD_FILENAMES=/usr/share/vulkan/icd.d/lvp_icd.json). 한 칸에 CPU 로 30초쯤 걸려서
키운 결과를 cache_dir 에 원본의 해시로 저장해 두고 다시 쓴다.
"""
import hashlib, os
import numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage

MODEL = 3      # realesrgan-x4plus-anime — 3D 장난감 질감에서 가장자리가 가장 깨끗했다
SCALE = 4
_esr = None


def _sr(raw_png, cache_dir):
    data = open(raw_png, 'rb').read()
    key = hashlib.sha1(data + bytes([MODEL])).hexdigest()[:16]
    os.makedirs(cache_dir, exist_ok=True)
    path = os.path.join(cache_dir, key + '.png')
    if not os.path.exists(path):
        global _esr
        if _esr is None:
            os.environ.setdefault('VK_ICD_FILENAMES', '/usr/share/vulkan/icd.d/lvp_icd.json')
            from realesrgan_ncnn_py import Realesrgan
            _esr = Realesrgan(gpuid=0, model=MODEL)
        _esr.process_pil(Image.open(raw_png).convert('RGB')).save(path)
    return Image.open(path).convert('RGB')


def upscale_cutout(raw_png, alpha_png, dst, cache_dir, quality=88):
    """raw_png 와 alpha_png 는 같은 크기(칸 전체)여야 한다."""
    big = _sr(raw_png, cache_dir)
    a = Image.open(alpha_png)
    a = a.getchannel('A') if a.mode in ('RGBA', 'LA') else a.convert('L')
    assert a.size[0] * SCALE == big.width and a.size[1] * SCALE == big.height, (a.size, big.size)
    rgb = np.array(big)
    # 알파는 부드럽게 키운 뒤 다시 조인다 — 계단이 없고 경계가 번지지 않게
    A0 = np.array(a.resize(big.size, Image.BICUBIC).filter(ImageFilter.GaussianBlur(1.2))) > 128
    # 원래 크기의 가장자리 1~2px 에 섞여 있던 바탕은 4배로 키우면 두꺼운 흰 테가 된다.
    # 키운 그림에서 경계 띠 안의 픽셀을 주변 바탕색과 비교해 바탕에 가까운 것을 걷어 낸다.
    outside = ~ndimage.binary_dilation(A0, iterations=6)
    wgt = ndimage.gaussian_filter(outside.astype(np.float32), 18)
    bg = np.stack([ndimage.gaussian_filter(rgb[..., c].astype(np.float32) * outside, 18) for c in range(3)], -1)
    bg /= np.maximum(wgt, 1e-4)[..., None]
    dist = np.sqrt(((rgb.astype(np.float32) - bg) ** 2).sum(2))
    band = ndimage.binary_dilation(A0, iterations=3) & ~ndimage.binary_erosion(A0, iterations=12)
    fg = A0 & ~(band & (dist < 42) & (wgt > 0.02))
    fg = ndimage.binary_opening(fg, iterations=2)
    lab, n = ndimage.label(fg)
    if n > 1:
        sizes = ndimage.sum(fg, lab, range(1, n + 1))
        fg = np.isin(lab, 1 + np.nonzero(sizes >= 0.01 * sizes.max())[0])
    # 경계 띠를 걷어 내며 생긴 바늘구멍만 메운다 — 다리 사이 같은 진짜 틈은 그대로
    holes = ndimage.binary_fill_holes(fg) & ~fg
    hl, hn = ndimage.label(holes)
    if hn:
        hs = ndimage.sum(holes, hl, range(1, hn + 1))
        fg |= np.isin(hl, 1 + np.nonzero(hs < 300)[0])
    A = np.array(Image.fromarray((fg * 255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(1.1))).astype(np.float32)
    # 가장자리 2px 는 아직 바탕색이 비친다 — 안쪽 색으로 덮는다
    inner = ndimage.binary_erosion(fg, iterations=3)
    ring = (A > 0) & ~inner
    if ring.any() and inner.any():
        idx = ndimage.distance_transform_edt(~inner, return_distances=False, return_indices=True)
        rgb[ring] = rgb[idx[0][ring], idx[1][ring]]
    out = Image.fromarray(rgb).convert('RGBA')
    out.putalpha(Image.fromarray(A.astype(np.uint8)))
    bb = out.getchannel('A').point(lambda v: 255 if v > 8 else 0).getbbox()
    m = 6 * SCALE
    out = out.crop((max(bb[0]-m, 0), max(bb[1]-m, 0), min(bb[2]+m, out.width), min(bb[3]+m, out.height)))
    out.save(dst, 'WEBP', quality=quality, method=6, exact=True)
    return out.size
