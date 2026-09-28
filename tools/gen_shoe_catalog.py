"""design/shoes-2026-09/catalog.json 으로 새 신발 도감(70종)의 앱 · 서버 · 워커 · 웹 파일을 만든다.

  python3 tools/gen_shoe_catalog.py [--images <누끼 딴 webp 폴더>]

catalog.json 이 원본이다. 모델 번호는 서버(sneaker_models) · 체인(v3 모델 번호)에 그대로 올라가므로 한 번 정하면
바꾸지 않는다(등급도). 새 모델은 뒤에 번호를 더한다.

만드는 것
  앱   res/drawable-nodpi/shoe_<번호>.webp          --images 를 주면 거기서(파일 이름 = catalog 의 file) 복사
       res/values{,-ko,-ja,-zh}/shoes.xml            이름 — 기본 · ja · zh 는 영어, ko 는 한국어(기존 도감과 같은 규칙)
       java/.../domain/ShoeCatalog.kt                번호 → 등급 · 시리즈 · 영어 이름
       java/.../ui/components/ShoeCatalogRes.kt      번호 → 그림 · 이름 리소스
  서버 supabase/migrations/0045_shoe_catalog.sql     '-- 도감 시작' ~ '-- 도감 끝' 사이
  워커 attester/src/shoe-catalog.js                  메타데이터 이름 · 그림 · v3 도감 추가
  웹   web/assets/sneakers/shoe_<번호>.webp          NFT 메타데이터 그림(앱 그림과 같은 파일)
그림은 tools/matte_shoes.py 로 원본에서 바탕을 걷어 만든다.
"""
import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / 'design/shoes-2026-09/catalog.json'
RES = ROOT / 'app/src/main/res'
DRAWABLE = RES / 'drawable-nodpi'
DOMAIN = ROOT / 'app/src/main/java/com/stepup/android/domain/ShoeCatalog.kt'
UI_RES = ROOT / 'app/src/main/java/com/stepup/android/ui/components/ShoeCatalogRes.kt'
SQL = ROOT / 'supabase/migrations/0045_shoe_catalog.sql'
WORKER = ROOT / 'attester/src/shoe-catalog.js'
WEB = ROOT / 'web/assets/sneakers'
RARITIES = ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')
GENERATED = 'tools/gen_shoe_catalog.py 가 design/shoes-2026-09/catalog.json 으로 만든 파일 — 손으로 고치지 않는다.'


def load():
    models = json.loads(CATALOG.read_text(encoding='utf-8'))['models']
    ids = [m['id'] for m in models]
    assert len(ids) == len(set(ids)), '같은 번호가 두 번 있다'
    assert ids == sorted(ids), '번호 차례대로 적는다'
    for m in models:
        assert isinstance(m['id'], int) and 1000 <= m['id'] < 2**31, m
        assert m['rarity'] in RARITIES, m
        assert re.fullmatch(r'[A-Za-z0-9-]+', m['file']), m
        for key in ('series', 'en', 'ko'):
            assert m[key].strip() == m[key] and m[key], m
    return models


def xml_text(s):
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    return s.replace("'", "\\'").replace('"', '\\"')


def kt(s):
    return s.replace('\\', '\\\\').replace('"', '\\"').replace('$', '\\$')


def sql(s):
    return "'" + s.replace("'", "''") + "'"


def js(s):
    return "'" + s.replace('\\', '\\\\').replace("'", "\\'") + "'"


def write(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    old = path.read_text(encoding='utf-8') if path.exists() else None
    if old != text:
        path.write_text(text, encoding='utf-8')
        print('wrote', path.relative_to(ROOT))


def images(models, src):
    DRAWABLE.mkdir(parents=True, exist_ok=True)
    WEB.mkdir(parents=True, exist_ok=True)
    for m in models:
        app = DRAWABLE / f"shoe_{m['id']}.webp"
        if src is not None:
            shutil.copyfile(src / f"{m['file']}.webp", app)
        if not app.exists():
            raise SystemExit(f'그림이 없다: {app.relative_to(ROOT)} — --images 로 넣는다')
        web = WEB / app.name
        if not web.exists() or web.read_bytes() != app.read_bytes():
            shutil.copyfile(app, web)


def strings(models):
    for folder, key in (('values', 'en'), ('values-ko', 'ko'), ('values-ja', 'en'), ('values-zh', 'en')):
        lines = ['<?xml version="1.0" encoding="utf-8"?>', f'<!-- {GENERATED} -->', '<resources>']
        lines += [f'    <string name="shoe_{m["id"]}">{xml_text(m[key])}</string>' for m in models]
        lines += ['</resources>', '']
        write(RES / folder / 'shoes.xml', '\n'.join(lines))


def kotlin(models):
    rows = [f'        Model({m["id"]}, Rarity.{m["rarity"]}, "{kt(m["series"])}", "{kt(m["en"])}"),' for m in models]
    write(DOMAIN, '\n'.join([
        'package com.stepup.android.domain',
        '',
        f'// {GENERATED}',
        '',
        '/**',
        ' * 새 신발 도감(2026-09-28) — 뽑기에서 나오는 신발. 번호는 서버(sneaker_models) · 체인(v3 모델 번호)과 같다.',
        ' * 등급은 그림 폴더 그대로다: grade-1 레어, grade-2 에픽, grade-3 레전더리(레전더리 · 레드라인 · 피니시 시리즈).',
        ' * 예전 52종(속성 × 변형)은 [SneakerDesigns] 에 그대로 있다 — 이미 가진 신발과 첫 신발이 쓴다.',
        ' */',
        'object ShoeCatalog {',
        '    data class Model(val id: Int, val rarity: Rarity, val series: String, val englishName: String)',
        '',
        '    val models: List<Model> = listOf(',
        *rows,
        '    )',
        '',
        '    private val byId: Map<Int, Model> = models.associateBy { it.id }',
        '',
        '    fun of(id: Int?): Model? = id?.let(byId::get)',
        '}',
        '',
    ]))
    image = [f'    {m["id"]} -> R.drawable.shoe_{m["id"]}' for m in models]
    name = [f'    {m["id"]} -> R.string.shoe_{m["id"]}' for m in models]
    write(UI_RES, '\n'.join([
        'package com.stepup.android.ui.components',
        '',
        'import androidx.annotation.DrawableRes',
        'import androidx.annotation.StringRes',
        'import com.stepup.android.R',
        '',
        f'// {GENERATED}',
        '',
        '/** 새 도감 신발 그림 — 없는 번호는 null(예전 속성 그림을 쓴다) */',
        '@DrawableRes',
        'fun shoeModelImageRes(id: Int): Int? = when (id) {',
        *image,
        '    else -> null',
        '}',
        '',
        '/** 새 도감 신발 이름 — 없는 번호는 null */',
        '@StringRes',
        'fun shoeModelNameRes(id: Int): Int? = when (id) {',
        *name,
        '    else -> null',
        '}',
        '',
    ]))


def server(models):
    text = SQL.read_text(encoding='utf-8')
    start, end = '-- 도감 시작', '-- 도감 끝'
    if start not in text or end not in text:
        raise SystemExit(f'{SQL.relative_to(ROOT)} 에 "{start}" · "{end}" 가 없다')
    rows = ',\n'.join(
        f"  ({m['id']}, {sql(m['rarity'])}, {sql(m['series'])}, {sql(m['file'])}, {sql(m['en'])}, {sql(m['ko'])})"
        for m in models)
    pairs = ', '.join(f"({m['id']}, {sql(m['rarity'])})" for m in models)
    block = '\n'.join([
        f'{start} ({GENERATED})',
        '-- 체인에 올라간 번호의 등급은 바꿀 수 없다 — 이미 있는 번호의 등급이 다르면 올리지 않는다(이름만 고칠 수 있다)',
        'do $$',
        'begin',
        '  if exists (select 1 from public.sneaker_models m',
        f'              join (values {pairs}) e(id, rarity) on e.id = m.id',
        '             where e.rarity <> m.rarity) then',
        "    raise exception '도감 번호의 등급이 바뀌었습니다 — 새 번호를 주세요';",
        '  end if;',
        'end $$;',
        'insert into public.sneaker_models (id, rarity, series, slug, name_en, name_ko) values',
        rows,
        'on conflict (id) do update',
        '  set series = excluded.series, slug = excluded.slug, name_en = excluded.name_en, name_ko = excluded.name_ko;',
        end,
    ])
    head, rest = text.split(start, 1)
    _, tail = rest.split(end, 1)
    write(SQL, head + block + tail)


def worker(models):
    rows = [f"  {m['id']}: {{ rarity: {js(m['rarity'])}, series: {js(m['series'])}, en: {js(m['en'])}, ko: {js(m['ko'])}, file: 'shoe_{m['id']}.webp' }},"
            for m in models]
    write(WORKER, '\n'.join([
        f'// {GENERATED}',
        '',
        '/**',
        ' * 새 신발 도감(2026-09-28). 번호 → 등급 · 시리즈 · 이름 · 그림 파일(web/assets/sneakers).',
        ' * 서버 sneaker_models · 앱 ShoeCatalog 와 같은 표다. v3 도감에는 워커가 한 번 더한다(src/catalog.js).',
        ' */',
        'export const SHOE_MODELS = Object.freeze({',
        *rows,
        '})',
        '',
    ]))


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--images', type=Path, help='누끼 딴 webp 폴더(tools/matte_shoes.py 결과)')
    args = ap.parse_args()
    models = load()
    images(models, args.images)
    strings(models)
    kotlin(models)
    server(models)
    worker(models)
    print(f'{len(models)} models')


if __name__ == '__main__':
    main()
