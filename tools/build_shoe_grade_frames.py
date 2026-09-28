"""신발 등급 프레임 v8 — 시안 레이어를 앱 그림으로 옮긴다(docs/redesign/shoe-grade-frames).

원본은 design/shoe-grade-frames-v8/build.py 의 좌표(440 × 418) 그대로다. Chromium 으로 투명 배경에 그려
app/src/main/res/drawable-nodpi/shoe_grade_*.webp 를 만든다.

  뒤 효과(back)   비교판 카드의 등급색 오라 + back-effect — 2배(880 × 836), 그리고 격자 칸용 1배(back_small)
  앞 효과(front)  front-effect — 2배와 1배(front_small). 일반(01)은 비어 있어 만들지 않는다
  프레임(frame)   frame — 2배, 그리고 목록 칸용 1배(frame_small)

1배 그림은 폭이 좁은 자리(보관함 2열 격자 · 보유 목록 칸)가 쓴다 — 칸마다 2배 그림을 풀면 메모리를 네 배 쓴다.

앱에는 보이는 갈래 여섯(일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시 = 01–06, domain/ShoeTier.kt)을 넣는다.
레드라인 · 피니시는 새 도감 레전더리 시리즈의 그림일 뿐 서버 등급 · 확률은 그대로다(2026-09-28 신발 화면 확정안).
--preview 는 문서용 합성 그림(실제 신발 몇 켤레를 얹은 것 — 앱 캡처가 아니다).

    python3 tools/build_shoe_grade_frames.py            # 앱 그림
    python3 tools/build_shoe_grade_frames.py --preview  # 문서 미리 보기(01–04 · 05–06)

Chromium 은 CHROME 환경 변수, 없으면 Playwright 의 것을 찾는다.
"""
from pathlib import Path
import os
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'design/shoe-grade-frames-v8/build.py'
RES = ROOT / 'app/src/main/res/drawable-nodpi'
DOCS = ROOT / 'docs/redesign/shoe-grade-frames'
APP_TIERS = {0: 'common', 1: 'rare', 2: 'epic', 3: 'legendary', 4: 'redline', 5: 'finish'}
# 앱과 같은 자리 — SneakerImage.kt 의 SneakerGradeStage / SneakerGradeThumb
STAGE_SLOT = (61, 58, 320)
THUMB_SLOT = (69, 69, 296)
OUTER = [(53, 25), (387, 25), (415, 53), (415, 365), (387, 393), (53, 393), (25, 365), (25, 53)]
STAGE_FILL = (8, 19, 32, 255)


def load_geometry():
    """build.py 의 함수만 읽는다 — 파일 끝의 시안 내보내기는 돌리지 않는다."""
    src = SOURCE.read_text(encoding='utf-8')
    ns = {'__file__': str(SOURCE)}
    exec(compile(src[:src.index('all_defs = ')], str(SOURCE), 'exec'), ns)
    return ns


def chrome():
    if os.environ.get('CHROME'):
        return os.environ['CHROME']
    for path in sorted(Path('/opt/pw-browsers').glob('chromium-*/chrome-linux/chrome'), reverse=True):
        return str(path)
    sys.exit('Chromium 을 찾지 못했다 — CHROME 에 경로를 준다')


def layer_svg(g, kind, i):
    if kind == 'back':
        # 비교판 카드가 back-effect 앞에 까는 등급색 오라까지 — 카드 표면의 일부라 뒤 레이어에 함께 굽는다
        body = f'<ellipse cx="220" cy="207" rx="173" ry="159" fill="url(#aura{i})"/>' + g['back_effects'](i)
    elif kind == 'front':
        body = g['front_effects'](i)
    else:
        body = g['frame'](i)
    return g['shell'](440, 418, body, g['defs'](i))


def render(g, kind, i, scale, work: Path) -> Image.Image:
    w, h = 440 * scale, 418 * scale
    svg = layer_svg(g, kind, i).replace('width="440" height="418"', f'width="{w}" height="{h}"', 1)
    html = work / f'{kind}-{i}-{scale}.html'
    html.write_text('<!doctype html><html><head><style>html,body{margin:0;padding:0;background:transparent;'
                    f'overflow:hidden}}svg{{display:block}}</style></head><body>{svg}</body></html>', encoding='utf-8')
    png = work / f'{kind}-{i}-{scale}.png'
    # 창 크기에 브라우저 틀이 섞여 아래가 잘리지 않게 넉넉히 열고 잘라 낸다
    subprocess.run([chrome(), '--headless=new', '--no-sandbox', '--disable-gpu', '--hide-scrollbars',
                    '--default-background-color=00000000', f'--window-size={w + 40},{h + 400}',
                    '--force-device-scale-factor=1', f'--screenshot={png}', html.as_uri()],
                   check=True, capture_output=True)
    return Image.open(png).convert('RGBA').crop((0, 0, w, h))


def layers(g, work):
    out = {}
    for i in range(6):
        for kind in ('back', 'front', 'frame'):
            out[kind, i, 2] = render(g, kind, i, 2, work)
            out[kind, i, 1] = render(g, kind, i, 1, work)
    return out


def write_app(art):
    for old in RES.glob('shoe_grade_*.webp'):
        old.unlink()
    for i, name in APP_TIERS.items():
        files = {f'shoe_grade_{name}_back': art['back', i, 2], f'shoe_grade_{name}_back_small': art['back', i, 1],
                 f'shoe_grade_{name}_frame': art['frame', i, 2], f'shoe_grade_{name}_frame_small': art['frame', i, 1]}
        if art['front', i, 2].getbbox():
            files[f'shoe_grade_{name}_front'] = art['front', i, 2]
            files[f'shoe_grade_{name}_front_small'] = art['front', i, 1]
        for stem, im in files.items():
            im.save(RES / f'{stem}.webp', 'WEBP', lossless=True, quality=100, method=6)
            print(stem, (RES / f'{stem}.webp').stat().st_size)


def compose(art, i, shoe, thumb=False, scale=2):
    w, h = 440 * scale, 418 * scale
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(im).polygon([(x * scale, y * scale) for x, y in OUTER],
                               fill=(16, 30, 50, 255) if thumb else STAGE_FILL)
    if not thumb:
        im.alpha_composite(art['back', i, 2])
    x, y, s = THUMB_SLOT if thumb else STAGE_SLOT
    shoe_im = Image.open(RES / f'{shoe}.webp').convert('RGBA').resize((s * scale, s * scale), Image.LANCZOS)
    im.alpha_composite(shoe_im, (x * scale, y * scale))
    if not thumb:
        im.alpha_composite(art['front', i, 2])
    im.alpha_composite(art['frame', i, 2])
    return im


def sheet(cells, columns, cell_width=520, background=(5, 11, 22, 255)):
    pad = 20
    cell_height = round(cell_width * 418 / 440)
    rows = (len(cells) + columns - 1) // columns
    out = Image.new('RGBA', (columns * (cell_width + pad) + pad, rows * (cell_height + pad) + pad), background)
    for k, cell in enumerate(cells):
        out.alpha_composite(cell.resize((cell_width, cell_height), Image.LANCZOS),
                            (pad + (k % columns) * (cell_width + pad), pad + (k // columns) * (cell_height + pad)))
    return out.convert('RGB')


def write_preview(art):
    shoes = ['sneaker_wind_01', 'sneaker_fire_01', 'sneaker_water_07', 'sneaker_lightning_10']
    # 01–04: 앱에 들어간 등급 — 실제 신발 네 켤레를 같은 자리에(앱 캡처가 아니라 그림 확인용)
    sheet([compose(art, i, shoe) for shoe in shoes[:2] for i in range(4)], 4).save(
        DOCS / 'preview-01-04-real-shoes.webp', 'WEBP', quality=88, method=6)
    # 05–06: 레드라인 · 피니시 — 새 도감 레전더리 시리즈의 그림(앱에 들어간다). 서버 등급 · NFT · 드롭률 · 보상은 그대로
    sheet([compose(art, i, shoe) for shoe in shoes[:2] for i in (4, 5)], 2).save(
        DOCS / 'preview-05-06-concept.webp', 'WEBP', quality=88, method=6)
    print('previews →', DOCS)


if __name__ == '__main__':
    geometry = load_geometry()
    with tempfile.TemporaryDirectory() as tmp:
        rendered = layers(geometry, Path(tmp))
        if '--preview' in sys.argv:
            write_preview(rendered)
        else:
            write_app(rendered)
