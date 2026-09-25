"""밝은 회청색 바탕의 캐릭터 가이드에서 캐릭터를 떼어 낸다.
바탕(저채도 · 중간~밝은 밝기, 흰색은 제외)을 가장자리에서부터 채워 지운다.

avatar_male_idle.webp: design/characters/runo-guide.webp 의 3/4 전신 (420, 80, 780, 670) 을
잘라 이 도구로 떼어 내고 tools/upscale_cutout.py 로 4배 키운다."""
import os, sys, numpy as np
from PIL import Image, ImageFilter
from collections import deque
src, dst = sys.argv[1], sys.argv[2]
im = np.array(Image.open(src).convert('RGB')).astype(np.int32)
h, w, _ = im.shape
r, g, b = im[...,0], im[...,1], im[...,2]
mx = im.max(2); mn = im.min(2)
sat = (mx - mn) / np.maximum(mx, 1)
lum = 0.299*r + 0.587*g + 0.114*b
# 바탕 후보: 회청색(파랑이 조금 더 큼), 저채도, 밝기 105~236
bgish = (sat < 0.30) & (lum > 105) & (lum < 236) & (b >= r)
seen = np.zeros((h, w), bool)
q = deque()
for x in range(w):
    for y in (0, h-1):
        if bgish[y, x]: q.append((y, x)); seen[y, x] = True
for y in range(h):
    for x in (0, w-1):
        if bgish[y, x] and not seen[y, x]: q.append((y, x)); seen[y, x] = True
while q:
    y, x = q.popleft()
    for dy, dx in ((1,0),(-1,0),(0,1),(0,-1)):
        ny, nx = y+dy, x+dx
        if 0 <= ny < h and 0 <= nx < w and not seen[ny, nx] and bgish[ny, nx]:
            seen[ny, nx] = True; q.append((ny, nx))
fg = ~seen
# 작은 섬(격자 점 등) 제거: 가장 큰 연결 성분만 남긴다
from scipy import ndimage
lab, n = ndimage.label(fg)
if n > 1:
    sizes = ndimage.sum(fg, lab, range(1, n+1))
    keep = 1 + int(np.argmax(sizes))
    fg = lab == keep
# 다리 사이처럼 캐릭터에 둘러싸인 바탕도 지운다 — 넓은 저채도 바탕 덩어리만
holes = ndimage.binary_fill_holes(fg) & ~fg
fg = ndimage.binary_fill_holes(fg)
bluish = bgish & ((b - r) > 28) & (lum > 150)
hl, hn = ndimage.label(bluish & fg)
if hn:
    hs = ndimage.sum(bluish & fg, hl, range(1, hn+1))
    for i, sz in enumerate(hs, start=1):
        if sz > 120:
            fg[hl == i] = False
# 신발 — 안쪽 밑창의 흰 그늘이 바닥색과 같아 파먹힌다. 발 높이 띠에서 신발 덩어리마다
# 볼록 껍질을 채워 원래 픽셀로 되돌린다(신발 외곽은 볼록하다).
from scipy.spatial import ConvexHull
from PIL import ImageDraw
y0 = int(h * 0.905)
band = np.zeros_like(fg); band[y0:] = fg[y0:]
# 두 발이 바닥 그늘로 이어져 한 덩어리가 되지 않게, 가운데에서 가장 빈 세로줄로 가른다
cols = band.sum(0)
lo, hi = int(w*0.3), int(w*0.7)
cut_x = lo + int(np.argmin(cols[lo:hi]))
band[:, max(cut_x-1,0):cut_x+2] = False
# 밑창 아래로 번진 얇은 바닥 그늘은 떼어 낸다
band = ndimage.binary_opening(band, iterations=2)
bl, bn = ndimage.label(band)
hull_img = Image.new('L', (w, h), 0); dr = ImageDraw.Draw(hull_img)
for i in range(1, bn + 1):
    ys, xs = np.nonzero(bl == i)
    if len(xs) < 300: continue
    pts = np.stack([xs, ys], 1)
    hv = ConvexHull(pts).vertices
    dr.polygon([tuple(map(int, pts[j])) for j in hv], fill=255)
hull = np.array(hull_img) > 0
fg = fg | hull
# 신발 둘레 밖에 남은 밝은 바닥 그늘 — 발 높이 띠에서만 지운다
fringe = np.zeros_like(fg); fringe[int(h*0.86):] = True
fg &= ~(fringe & ~hull & (lum > 120) & (sat < 0.35))
alpha = Image.fromarray((fg*255).astype(np.uint8)).filter(ImageFilter.GaussianBlur(0.8))
# 가장자리 반투명 픽셀의 밝은 바탕색 번짐을 줄이려고 경계 색을 안쪽 색으로 당긴다
rgb = Image.open(src).convert('RGB')
inner = ndimage.binary_erosion(fg, iterations=2)
arr = np.array(rgb)
edge = fg & ~inner
if edge.any():
    idx = ndimage.distance_transform_edt(~inner, return_distances=False, return_indices=True)
    arr[edge] = arr[idx[0][edge], idx[1][edge]]
out = Image.fromarray(arr).convert('RGBA'); out.putalpha(alpha)
bbox = out.getchannel('A').point(lambda v: 255 if v > 8 else 0).getbbox()
# CUTOUT_NOCROP=1 이면 칸 크기 그대로(뒤에서 tools/upscale_cutout.py 로 키울 때)
if not os.environ.get('CUTOUT_NOCROP'):
    out = out.crop((max(bbox[0]-6,0), max(bbox[1]-6,0), min(bbox[2]+6,w), min(bbox[3]+6,h)))
out.save(dst)
print(dst, out.size)
