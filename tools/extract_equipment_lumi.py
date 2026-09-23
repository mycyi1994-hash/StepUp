"""루미 장비 시트에서 착용 전신과 의상 상품 그림을 떼어 낸다.
# 사용: python3 tools/extract_equipment_lumi.py design/equipment/lumi <작업 폴더> app/src/main/res/drawable-nodpi

  신발 시트 4장 — 칸마다 추천 의상(불=LUM-CLO-002, 물=003, 번개=004, 바람=005) + 그 신발을
                 신은 루미 (13 × 4 = 52). 칸 위치는 두 눈(청록 캡슐)으로 찾는다.
  의상 시트 1장 — 가운데 줄: 그 의상 + 기본 신발의 루미 (5), 윗줄 왼쪽: 모자 · 상의 · 반바지 앞면 (5)

떼어 내기는 tools/cutout_character_sheet.py(칸 안쪽 바탕을 면으로 추정 — CUTOUT_BG2D).
같은 칸의 상품 신발은 캐릭터 오른손 뒤에 겹쳐 있어 손 · 다리의 짙은 남색 윤곽을 기준으로 자른다.
"""
import os, sys, subprocess, numpy as np
from PIL import Image
from scipy import ndimage
SRC, OUT, RES = sys.argv[1], sys.argv[2], sys.argv[3]
CUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'cutout_character_sheet.py')
os.makedirs(OUT, exist_ok=True)


sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from upscale_cutout import upscale_cutout


def upscale(raw, cut, dst):
    """원래 크기의 알파 + Real-ESRGAN 4배 색 — tools/upscale_cutout.py"""
    return upscale_cutout(raw, cut, dst, os.path.join(OUT, 'esr_cache'))


def find_heads(path, dbg=False):
    im=np.array(Image.open(path).convert('RGB')).astype(int)
    R,G,B=im[...,0],im[...,1],im[...,2]
    lum=0.299*R+0.587*G+0.114*B
    eye=(lum>150)&(B>=R)&(B>200)
    lab,k=ndimage.label(eye)
    blobs=[]
    for i,sl in enumerate(ndimage.find_objects(lab),1):
        h=sl[0].stop-sl[0].start; w=sl[1].stop-sl[1].start; a=(lab[sl]==i).sum()
        if 30<a<900 and h>=1.3*w and 10<h<50:
            y0,y1,x0,x1=sl[0].start,sl[0].stop,sl[1].start,sl[1].stop
            ring=lum[max(y0-6,0):y1+6, max(x0-8,0):x1+8]
            if np.median(ring)<110:
                blobs.append(((y0+y1)/2,(x0+x1)/2))
    pairs=[];used=set()
    for i,a in enumerate(blobs):
        for j,b in enumerate(blobs):
            if j<=i or i in used or j in used: continue
            if abs(a[0]-b[0])<10 and 15<abs(a[1]-b[1])<60:
                pairs.append((int((a[0]+b[0])/2),int((a[1]+b[1])/2))); used|={i,j}
    pairs.sort(key=lambda p:(p[0]//150,p[1]))
    return pairs

def split_product(png, raw, ey, ex):
    """같은 칸의 상품 신발(오른쪽) · 옆 칸의 상품 신발(왼쪽 끝) 떼기.

    상품 신발은 캐릭터의 오른손 뒤에 겹쳐 있어 색이나 깎기로는 갈리지 않는다.
    캐릭터 쪽의 기준을 찾는다 — 짙은 남색 오른손의 오른쪽 끝(그 높이까지)과,
    손 아래에서는 오른 다리의 오른쪽 끝. 그 밖을 지운다.
    """
    im = Image.open(png).convert('RGBA'); a = np.array(im.getchannel('A')) > 128
    rgb = np.array(Image.open(raw).convert('RGB')).astype(int)
    lum = 0.299*rgb[..., 0] + 0.587*rgb[..., 1] + 0.114*rgb[..., 2]
    h, w = a.shape
    dark = ndimage.binary_opening((lum < 60) & a, iterations=1)
    feet_top = ey + 148

    def hand(x0, x1, pick):
        zone = np.zeros_like(a); zone[ey+60:ey+140, max(x0, 0):x1] = True
        lab, n = ndimage.label(dark & zone)
        if not n:
            return None
        sizes = ndimage.sum(dark & zone, lab, range(1, n+1))
        i = 1 + int(np.argmax(sizes))
        if sizes[i-1] < 60:
            return None
        ys, xs = np.nonzero(lab == i)
        return (xs.max() if pick == 'right' else xs.min()), ys.max(), ys.min()

    def leg_edge(top, x0, x1, core, pick):
        """손 아래의 다리 — 다리 자리(core)에 걸친 짙은 덩어리만"""
        sub = np.zeros_like(a); sub[top:feet_top, max(x0, 0):x1] = True
        lab, n = ndimage.label(dark & sub)
        best = None
        for i in range(1, n+1):
            ys, xs = np.nonzero(lab == i)
            if core[0] <= xs.min() <= core[1] or core[0] <= xs.max() <= core[1] or (xs.min() < core[0] and xs.max() > core[1]):
                e = xs.max() if pick == 'right' else xs.min()
                best = e if best is None else (max(best, e) if pick == 'right' else min(best, e))
        return best

    rm = np.zeros_like(a)
    r = hand(ex+5, ex+50, 'right')
    # 손은 눈 중심에서 오른쪽으로 25px 넘게 떨어져 있다 — 더 가까우면 지퍼 · 반바지를 잘못 잡은 것
    if r and r[0] < ex + 25: r = None
    cutA, hb = (r[0] + 3, r[1] + 2) if r else (ex + 42, ey + 120)
    e = leg_edge(hb, ex-45, cutA, (ex-25, ex+12), 'right')
    cutB = max(e + 4 if e is not None else ex + 12, ex + 12)
    cutB = min(cutB, cutA)
    rm[ey+5:hb, cutA:] = True
    rm[hb:feet_top, cutB:] = True
    rm[ey+80:, :max(ex-98, 0)] = True
    a &= ~rm
    # 발 높이 — 상품 신발 밑창은 발끝보다 위에서 끝난다. 맨 아래 25줄(두 발만 있는 곳)의
    # 오른쪽 끝보다 오른쪽은 지운다.
    rows = np.nonzero(a.any(1))[0]
    if len(rows):
        bot = rows.max()
        foot = a[bot-25:bot+1]
        if foot.any():
            rf = np.nonzero(foot.any(0))[0].max()
            a[feet_top-12:bot-25, rf+3:] = False
    # 오른발 옆 — 상품 신발 밑창의 얇은 가로 띠(세로로 7px 안 되는 것)
    zone = np.zeros_like(a); zone[feet_top-12:feet_top+22, ex+30:] = True
    a = (a & ~zone) | ndimage.binary_opening(a & zone, structure=np.ones((7, 1)))
    mx = rgb.max(2); mn = rgb.min(2); sat = (mx - mn) / np.maximum(mx, 1)
    pale = (lum > 165) & (sat < 0.3)
    # 왼쪽 위 — 포니테일 옆의 칸 테두리 · 밝은 바탕
    tl = np.zeros_like(a); tl[:ey+40, :max(ex-70, 0)] = True
    a &= ~(tl & pale)
    # 손 옆 — 손에만 붙어 있는 조각(상품 신발 뒤꿈치)
    hands = ndimage.binary_dilation(dark & (np.arange(w)[None, :] > ex + 5) & (np.arange(h)[:, None] > ey + 60), iterations=2)
    rest = a & ~hands
    lab3, n3 = ndimage.label(rest)
    if n3 > 1:
        s3 = ndimage.sum(rest, lab3, range(1, n3+1)); keep = 1 + int(np.argmax(s3))
        for i in range(1, n3+1):
            if i != keep and s3[i-1] < 900:
                a &= ~(lab3 == i)
    lab2, n2 = ndimage.label(a)
    if n2 > 1:
        s = ndimage.sum(a, lab2, range(1, n2+1)); a = lab2 == 1 + int(np.argmax(s))
    alpha = np.array(im.getchannel('A')); alpha[~a] = 0
    im.putalpha(Image.fromarray(alpha))
    bb = im.getchannel('A').point(lambda v: 255 if v > 8 else 0).getbbox()
    im.save(png)  # 칸 크기 그대로 — 알파를 upscale_cutout 에 넘긴다
    return (int(cutA - ex), int(hb - ey), int(cutB - ex))
sheets = [('01_LUMI_FIRE_13', 'fir'), ('02_LUMI_WATER_13', 'wat'), ('03_LUMI_LIGHTNING_13', 'lit'), ('04_LUMI_WIND_13', 'wnd')]
# 칸 격자선(열 · 줄) — 줄마다 중앙값 밝기가 푹 꺼지는 자리. 칸 안쪽만 잘라야
# 칸 테두리 · 옆 칸의 상품 신발이 바탕 추정에 끼지 않는다.
GRID = {
    'fir': ([19, 320, 626, 930, 1235], [86, 376, 667, 954, 1216]),
    'wat': ([24, 324, 628, 934, 1230], [97, 373, 644, 913, 1190]),
    'lit': ([23, 325, 626, 929, 1231], [83, 353, 624, 893, 1175]),
    'wnd': ([23, 326, 629, 926, 1229], [83, 369, 655, 941, 1208]),
}
for name, prefix in sheets:
    path = os.path.join(SRC, name + '.webp')
    img = Image.open(path).convert('RGB')
    heads = find_heads(path)[:13]
    assert len(heads) == 13, (name, len(heads))
    for i, (ey, ex) in enumerate(heads, start=1):
        key = f'{prefix}_{i:03d}'
        xs, ys = GRID[prefix]
        cl = max(x for x in xs if x < ex); rt = max(y for y in ys if y < ey); rb = min(y for y in ys if y > ey)
        # 바람 시트는 칸 안에 둥근 판 테두리가 한 겹 더 있다
        inset = 9 if prefix == 'wnd' else 3
        x0, y0 = cl + inset, rt + inset
        crop = img.crop((x0, y0, min(ex+72, img.width), rb - 3))
        raw = os.path.join(OUT, f'{key}_raw.png'); crop.save(raw)
        cut = os.path.join(OUT, f'{key}_cut.png')
        subprocess.run([sys.executable, CUT, raw, cut, '16', '0.74'], check=True, capture_output=True, env={**os.environ, 'CUTOUT_NOCROP': '1', 'CUTOUT_BG2D': '1'})
        k = split_product(cut, raw, ey - y0, ex - x0)
        # cutout crops to bbox — redo without crop: keep the offset by re-running on the same canvas
        print(key, upscale(raw, cut, os.path.join(RES, f'avatar_lumi_idle_{key}.webp')))

# 의상 시트
img = Image.open(os.path.join(SRC, '05_LUMI_WARDROBE_05.webp')).convert('RGB')
cols = [63, 341, 630, 919, 1207, 1510]   # 칸 격자선
for i in range(5):
    l, r = cols[i], cols[i+1]; mid = (l + r) // 2
    raw = os.path.join(OUT, f'clo_{i+1:03d}_raw.png'); img.crop((l+4, 402, r-4, 763)).save(raw)
    cut = os.path.join(OUT, f'clo_{i+1:03d}_cut.png')
    # 흰 양말이 바탕으로 지워져 신발이 몸과 끊겨도 남기도록 CUTOUT_KEEP
    subprocess.run([sys.executable, CUT, raw, cut, '24', '0.78'], check=True, capture_output=True,
                   env={**os.environ, 'CUTOUT_BG2D': '1', 'CUTOUT_KEEP': '0.03', 'CUTOUT_NOCROP': '1'})
    print('LUM-CLO', i+1, upscale(raw, cut, os.path.join(RES, f'avatar_lumi_idle_lum_clo_{i+1:03d}.webp')))
    raw = os.path.join(OUT, f'clo_{i+1:03d}_item_raw.png'); img.crop((l+2, 136, mid+6, 392)).save(raw)
    cut = os.path.join(OUT, f'clo_{i+1:03d}_item_cut.png')
    # 모자 · 상의 · 반바지는 따로 떨어진 세 덩어리다
    subprocess.run([sys.executable, CUT, raw, cut, '24' if i + 1 in (1, 2, 4) else '16', '1.0'],
                   check=True, capture_output=True, env={**os.environ, 'CUTOUT_KEEP': '0.06', 'CUTOUT_NOCROP': '1'})
    print('LUM-CLO item', i+1, upscale(raw, cut, os.path.join(RES, f'outfit_lum_clo_{i+1:03d}.webp')))
