"""장비 시트에서 루노 착용 전신과 의상 상품 그림을 떼어 낸다.
# 사용: python3 tools/extract_equipment.py design/equipment <작업 폴더> app/src/main/res/drawable-nodpi

  신발 시트 4장 — 셀마다 기본 의상 + 해당 신발을 신은 루노 (13 × 4 = 52)
  의상 시트 1장 — 가운데 줄: 해당 의상 + 기본 신발을 입은 루노 (5), 윗줄 왼쪽: 의상 앞면 상품 (5)
"""
import subprocess, sys, os, numpy as np
from PIL import Image, ImageFilter
from scipy import ndimage
SRC = sys.argv[1]; OUT = sys.argv[2]; RES = sys.argv[3]
CUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'cutout_character_sheet.py')
os.makedirs(OUT, exist_ok=True)

def boxes(path):
    im = np.array(Image.open(path).convert('RGB')).astype(int)
    lum = 0.299*im[...,0] + 0.587*im[...,1] + 0.114*im[...,2]
    dark = ndimage.binary_closing(lum < 75, iterations=2)
    lab, k = ndimage.label(dark)
    res = []
    for i, sl in enumerate(ndimage.find_objects(lab)):
        h = sl[0].stop - sl[0].start
        if h > 150 and (lab[sl] == i+1).sum() > 8000:
            res.append((sl[0].start, sl[1].start, sl[0].stop, sl[1].stop))
    res.sort(key=lambda r: (r[0] // 150, r[1]))
    return res

def finish(raw, dst, scale=3, low=0.74, tol=24):
    tmp = dst + '.cut.png'
    subprocess.run([sys.executable, CUT, raw, tmp, str(tol), str(low)], check=True, capture_output=True)
    im = Image.open(tmp).convert('RGBA')
    big = im.resize((im.width*scale, im.height*scale), Image.LANCZOS)
    rgb = big.convert('RGB').filter(ImageFilter.UnsharpMask(radius=1.4, percent=70, threshold=2))
    out = rgb.convert('RGBA'); out.putalpha(big.getchannel('A'))
    out.save(dst, 'WEBP', quality=90, method=6, exact=True)
    os.remove(tmp)
    return out.size

sheets = [('01_RUNO_FIRE_13', 'fir'), ('02_RUNO_WATER_13', 'wat'), ('03_RUNO_LIGHTNING_13', 'lit'), ('04_RUNO_WIND_13', 'wnd')]
for name, prefix in sheets:
    path = os.path.join(SRC, name + '.webp')
    img = Image.open(path).convert('RGB')
    bs = boxes(path)[:13]
    assert len(bs) == 13, (name, len(bs))
    for i, (y0, x0, y1, x1) in enumerate(bs, start=1):
        # 오른쪽 옆의 상품 신발을 피해 오른쪽 여유는 조금만, 발이 들어오게 아래로 길게
        crop = img.crop((max(x0-14, 0), max(y0-8, 0), x1+2, min(y0+282, img.height)))
        raw = os.path.join(OUT, f'{prefix}_{i:03d}_raw.png'); crop.save(raw)
        size = finish(raw, os.path.join(RES, f'avatar_runo_idle_{prefix}_{i:03d}.webp'))
        print(name, i, size)

# 의상 시트
path = os.path.join(SRC, '05_RUNO_WARDROBE_05.webp')
img = Image.open(path).convert('RGB')
cols = [(82, 378), (380, 686), (688, 992), (994, 1298), (1300, 1604)]
for i, (l, r) in enumerate(cols, start=1):
    crop = img.crop((l+8, 288, r-8, 652))
    raw = os.path.join(OUT, f'clo_{i:03d}_equipped_raw.png'); crop.save(raw)
    print('CLO', i, finish(raw, os.path.join(RES, f'avatar_runo_idle_clo_{i:03d}.webp'), low=0.78))
    mid = (l + r) // 2
    crop = img.crop((l+4, 84, mid+6, 296))
    raw = os.path.join(OUT, f'clo_{i:03d}_item_raw.png'); crop.save(raw)
    print('CLO item', i, finish(raw, os.path.join(RES, f'outfit_clo_{i:03d}.webp'), scale=2, low=1.0,
                                  # 흰 옷(CLO-005)은 바탕과 가까워 기준을 좁힌다
                                  tol=16 if i == 5 else 24))
