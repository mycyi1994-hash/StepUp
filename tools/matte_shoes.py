"""새 신발 원본(밝은 한 가지 색 바탕 PNG)에서 바탕을 걷어 투명 webp 로 만든다.

  python3 tools/matte_shoes.py <원본 폴더> <출력 폴더>

원본 폴더 아래(하위 폴더 포함)의 *.png 를 모두 읽어 같은 이름의 .webp 로 쓴다 — 2026-09-28 사용자가 준
stepup-grade-70(grade-1 · grade-2 · grade-3 폴더). 결과를 tools/gen_shoe_catalog.py --images 로 넣는다.

  - 가장자리와 이어진 바탕색만 지운다. 신발 안의 흰 밑창은 외곽선이 막아 남는다.
  - 외곽선 안에 갇힌 바탕(끈 사이 · 뒤꿈치 고리 안)은 두꺼운 덩어리만 지운다 — 가는 흰 하이라이트 선은 남긴다.
  - 경계 2px 는 바탕색과의 거리로 반투명, 바탕색 번짐을 걷어 낸다(흰 테두리가 어두운 화면에서 뜨지 않게).
  - 신발 범위로 자르고 6% 여백의 정사각형 640px. 원본이 모두 같은 크기 · 같은 배치라 신발 사이 크기 비교가 유지된다.
"""
import sys
from multiprocessing import Pool
from pathlib import Path

import numpy as np
from PIL import Image
from scipy import ndimage


def matte(path, size=640, margin=0.06, t_bg=18.0, t_edge=90.0):
    rgb = np.asarray(Image.open(path).convert('RGB')).astype(np.float64)
    border = np.concatenate([rgb[:6].reshape(-1, 3), rgb[-6:].reshape(-1, 3), rgb[:, :6].reshape(-1, 3), rgb[:, -6:].reshape(-1, 3)])
    bg = np.median(border, axis=0)
    dist = np.sqrt(((rgb - bg) ** 2).sum(axis=2))
    near = dist < t_bg
    labels, _ = ndimage.label(near)
    edge_labels = np.unique(np.concatenate([labels[0], labels[-1], labels[:, 0], labels[:, -1]]))
    background = np.isin(labels, edge_labels[edge_labels != 0])
    exact = dist < 6
    xl, xn = ndimage.label(exact)
    edt = ndimage.distance_transform_edt(exact)
    for i in range(1, xn + 1):
        comp = xl == i
        if background[comp].any():
            continue
        if comp.sum() >= 500 and edt[comp].max() >= 7:
            background |= ndimage.binary_dilation(comp, iterations=2) & near
    fg = ~background
    ring = fg & ndimage.binary_dilation(background, iterations=2)
    alpha = np.where(background, 0.0, 1.0)
    alpha[ring] = np.clip((dist[ring] - t_bg * 0.5) / (t_edge - t_bg * 0.5), 0.0, 1.0)
    a3 = alpha[..., None]
    color = np.clip(np.where(a3 > 0.02, (rgb - (1 - a3) * bg) / np.maximum(a3, 0.02), 0), 0, 255)
    rgba = np.dstack([color, alpha * 255]).astype(np.uint8)
    ys, xs = np.where(alpha > 0.05)
    y0, y1, x0, x1 = ys.min(), ys.max(), xs.min(), xs.max()
    side = int(max(y1 - y0, x1 - x0) * (1 + 2 * margin))
    canvas = Image.new('RGBA', (side, side), (0, 0, 0, 0))
    crop = Image.fromarray(rgba).crop((x0, y0, x1 + 1, y1 + 1))
    canvas.paste(crop, ((side - crop.width) // 2, (side - crop.height) // 2))
    return canvas.resize((size, size), Image.LANCZOS)


def job(args):
    src, out = args
    matte(src).save(out, 'WEBP', quality=90, method=6)
    return out.name


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit(__doc__)
    src_dir, out_dir = Path(sys.argv[1]), Path(sys.argv[2])
    out_dir.mkdir(parents=True, exist_ok=True)
    items = [(p, out_dir / (p.stem + '.webp')) for p in sorted(src_dir.rglob('*.png'))]
    with Pool() as pool:
        for name in pool.imap(job, items):
            print(name)
    print(f'{len(items)} -> {out_dir}')
