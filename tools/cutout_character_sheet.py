"""장비 시트용 캐릭터 떼어 내기 — 바탕색 모형 방식.

바탕은 부드러운 그라데이션이다. 잘라 낸 칸의 양쪽 가장자리 열에서 줄마다 바탕색을
재고, 그 색과 가까운 픽셀만 가장자리에서부터 채워 지운다. 흰 옷 · 흰 밑창은 바탕보다
확실히 밝고 희어서 남는다.
"""
import sys, numpy as np
from PIL import Image, ImageFilter, ImageDraw
from collections import deque
from scipy import ndimage
from scipy.spatial import ConvexHull
src, dst = sys.argv[1], sys.argv[2]
tol = float(sys.argv[3]) if len(sys.argv) > 3 else 24.0
low_from = float(sys.argv[4]) if len(sys.argv) > 4 else 0.74
# low_from 이 1 이상이면 캐릭터가 아닌 상품 그림(옷만) — 발 · 바닥 처리를 건너뛴다
figure = low_from < 1.0
im = np.array(Image.open(src).convert('RGB')).astype(np.float32)
h, w, _ = im.shape
# 줄마다 왼쪽 · 오른쪽 가장자리의 바탕색. 옆 칸의 상품 신발이 한쪽 가장자리에
# 걸리면 그쪽 값은 바탕이 아니다 — 위 줄에서 이어지는 쪽을 고른다.
left = np.median(im[:, :5], axis=1); right = np.median(im[:, -5:], axis=1)
bg_row = np.zeros_like(left)
prev = np.median(np.concatenate([im[:8, :5].reshape(-1, 3), im[:8, -5:].reshape(-1, 3)]), axis=0)
for y in range(h):
    dl = np.linalg.norm(left[y] - prev); dr_ = np.linalg.norm(right[y] - prev)
    pick = left[y] if dl <= dr_ else right[y]
    if min(dl, dr_) > 30: pick = prev          # 두 쪽 다 튀면 위 줄 값을 잇는다
    bg_row[y] = pick; prev = 0.8 * prev + 0.2 * pick
bg_row = ndimage.median_filter(bg_row, size=(15, 1))
dist = np.sqrt(((im - bg_row[:, None, :]) ** 2).sum(2))
lum = 0.299*im[...,0] + 0.587*im[...,1] + 0.114*im[...,2]
mx = im.max(2); mn = im.min(2); sat = (mx - mn) / np.maximum(mx, 1)
loose = dist < tol
bgish = loose
seen = np.zeros((h, w), bool); q = deque()
for x in range(w):
    for y in (0, h-1):
        if bgish[y, x]: seen[y, x] = True; q.append((y, x))
for y in range(h):
    for x in (0, w-1):
        if bgish[y, x] and not seen[y, x]: seen[y, x] = True; q.append((y, x))
while q:
    y, x = q.popleft()
    for dy, dx in ((1,0),(-1,0),(0,1),(0,-1)):
        ny, nx = y+dy, x+dx
        if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx] and bgish[ny, nx]:
            seen[ny, nx] = True; q.append((ny, nx))
fg = ~seen
# 좁은 기준의 바탕 — 신발 높이에서 바탕과 거의 같은 색(dist < 9)만 바탕으로 본다.
# 흰 어퍼의 푸른 그늘은 바탕과 가까워 넉넉한 기준으로는 바깥 바탕과 함께 지워진다.
# 이 결과는 아래에서 신발 껍질 안쪽에만 쓴다(바닥 그늘까지 되살리지 않도록).
tbg = dist < 9
tseen = np.zeros((h, w), bool); tq = deque()
for x in range(w):
    for y in (0, h-1):
        if tbg[y, x]: tseen[y, x] = True; tq.append((y, x))
for y in range(h):
    for x in (0, w-1):
        if tbg[y, x] and not tseen[y, x]: tseen[y, x] = True; tq.append((y, x))
while tq:
    y, x = tq.popleft()
    for dy, dx in ((1,0),(-1,0),(0,1),(0,-1)):
        ny, nx = y+dy, x+dx
        if 0 <= ny < h and 0 <= nx < w and not tseen[ny, nx] and tbg[ny, nx]:
            tseen[ny, nx] = True; tq.append((ny, nx))
fg_tight = ~tseen
# 칸 격자선처럼 가는 줄은 떼어 낸다
fg = ndimage.binary_opening(fg, structure=np.ones((3, 3)))
lab, n = ndimage.label(fg)
if n > 1:
    sizes = ndimage.sum(fg, lab, range(1, n+1)); fg = lab == (1 + int(np.argmax(sizes)))
fg = ndimage.binary_fill_holes(fg)
# 다리 사이 — 아래쪽의 바탕과 같은 색 덩어리만 지운다
low = np.zeros_like(fg); low[int(h*low_from):] = True
shoe_top = int(h * 0.83)
# 반바지 바로 아래(신발 위)의 바탕색 덩어리는 넉넉한 기준으로 찾는다.
gap = loose & fg & low; gap[shoe_top:] = False
hl, hn = ndimage.label(gap)
for i, sl in enumerate(ndimage.find_objects(hl), start=1):
    if sl is not None and (hl[sl] == i).sum() > 60:
        fg[hl == i] = False
# 그 아래로 이어진 바탕색도 지운다(발목 사이 · 발 옆 바닥). 신발 안의 흰 그늘까지 함께
# 지워지는 것은 바로 아래에서 되살린다.
el, en = ndimage.label(loose | ~fg)
out_ids = np.unique(el[~fg]); out_ids = out_ids[out_ids > 0]
fg &= ~np.isin(el, out_ids)
# 밑창 — 신발마다 볼록 껍질
y0 = int(h * float(sys.argv[5])) if len(sys.argv) > 5 else int(h * 0.90)
# 바닥 그늘은 바탕보다 어둡다. 흰 밑창은 바탕보다 밝다 — 밝기로 가른다.
bg_lum = (0.299*bg_row[:, 0] + 0.587*bg_row[:, 1] + 0.114*bg_row[:, 2])[:, None]
shadowish = (lum > 110) & (lum < bg_lum - 4) & (sat < 0.40) & (dist < 85)
band = np.zeros_like(fg); band[y0:] = fg[y0:] & ~shadowish[y0:]
cols = band.sum(0); lo, hi = int(w*0.3), int(w*0.7)
cx = lo + int(np.argmin(cols[lo:hi])); band[:, max(cx-1,0):cx+2] = False
band = ndimage.binary_opening(band, iterations=2)
bl, bn = ndimage.label(band)
hull_img = Image.new('L', (w, h), 0); dr = ImageDraw.Draw(hull_img)
for i in range(1, bn+1):
    ys, xs = np.nonzero(bl == i)
    if len(xs) < 120: continue
    pts = np.stack([xs, ys], 1)
    try: hv = ConvexHull(pts).vertices
    except Exception: continue
    dr.polygon([tuple(map(int, pts[j])) for j in hv], fill=255)
hull = np.array(hull_img) > 0
if not figure: hull[:] = False
fg |= hull
# 발 아래 바닥 그늘 — 껍질 밖, 밝고 흐린 것
fr = np.zeros_like(fg); fr[int(h*0.86):] = True
# 바깥 바탕에 닿은 덩어리만 지운다. 신발 안의 흰 그늘은 둘레가 신발이라 남는다.
cand = fr & ~hull & fg & (shadowish | ((dist < tol * 1.3) & (lum < bg_lum + 3)))
touch = ndimage.binary_dilation(~fg, iterations=1)
cl, cn = ndimage.label(cand)
if cn:
    hit = np.unique(cl[touch & cand]); hit = hit[hit > 0]
    fg &= ~np.isin(cl, hit)
# 밑창 높이의 껍질 밖 — 푸르고 흐린 바닥 그늘(바탕보다 어둡거나 비슷한 밝기)은 지운다
hazeish = (sat < 0.42) & (im[..., 2] >= im[..., 0]) & (im[..., 2] >= im[..., 1]) & (lum > 100) & (lum < bg_lum + 3)
floor = np.zeros_like(fg); floor[y0:] = True
if figure:
    fg &= ~(floor & ~hull & hazeish)
fg = ndimage.binary_opening(fg, structure=np.ones((2, 2))) | hull
lab, n = ndimage.label(fg)
if n > 1:
    sizes = ndimage.sum(fg, lab, range(1, n+1)); fg = lab == (1 + int(np.argmax(sizes)))
# 신발 안 되살리기 — 두 발 각각의 볼록 껍질 안에서 좁은 기준으로 전경인 픽셀
st = int(h * 0.84)
sb = np.zeros_like(fg); sb[st:] = fg[st:]
sc = sb.sum(0); slo, shi = int(w * 0.3), int(w * 0.7)
split = slo + int(np.argmin(sc[slo:shi]))
shoe_img = Image.new('L', (w, h), 0); sdr = ImageDraw.Draw(shoe_img)
for a, b in ((0, split), (split + 1, w)):
    ys, xs = np.nonzero(sb[:, a:b])
    if len(xs) < 120: continue
    pts = np.stack([xs + a, ys], 1)
    try: hv = ConvexHull(pts).vertices
    except Exception: continue
    sdr.polygon([tuple(map(int, pts[j])) for j in hv], fill=255)
shoe_hull = np.array(shoe_img) > 0
if figure:  # 상품 그림(옷만)에는 발이 없다
    fg |= shoe_hull & fg_tight
# 가장자리의 밝은 바탕 번짐 — 경계 두 줄을 안쪽 색으로
inner = ndimage.binary_erosion(fg, iterations=2)
arr = np.array(Image.open(src).convert('RGB'))
ring = fg & ~inner
if ring.any():
    idx = ndimage.distance_transform_edt(~inner, return_distances=False, return_indices=True)
    arr[ring] = arr[idx[0][ring], idx[1][ring]]
alpha = Image.fromarray((fg*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.7))
out = Image.fromarray(arr).convert('RGBA'); out.putalpha(alpha)
bbox = out.getchannel('A').point(lambda v: 255 if v > 8 else 0).getbbox()
out = out.crop((max(bbox[0]-6,0), max(bbox[1]-6,0), min(bbox[2]+6,w), min(bbox[3]+6,h)))
out.save(dst); print(dst, out.size)
