"""Checks the shipped earcons, localization parity, font binaries and attribution."""
from pathlib import Path
import struct
import wave
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
expected = {'tap', 'select', 'success', 'reward', 'start', 'pause', 'lap', 'countdown', 'error'}
files = {p.stem.removeprefix('cue_'): p for p in (RES / 'raw').glob('cue_*.wav')}
assert files.keys() == expected
for name, path in files.items():
    with wave.open(str(path)) as audio:
        assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 44100)
        samples = struct.unpack('<' + 'h' * audio.getnframes(), audio.readframes(audio.getnframes()))
        assert 0.04 < len(samples) / 44100 < 1.0, name
        assert 0.01 < max(map(abs, samples)) / 32768 < .8, name
        assert abs(samples[0]) < 10 and abs(samples[-1]) < 10, name
        assert max(abs(a - b) for a, b in zip(samples, samples[1:])) < 4000, name
base = {node.attrib['name'] for node in ET.parse(RES / 'values/experience.xml').getroot()}
for locale in ['values-ko', 'values-ja', 'values-zh']:
    assert {node.attrib['name'] for node in ET.parse(RES / locale / 'experience.xml').getroot()} == base
for font in (RES / 'font').glob('*.ttf'):
    assert font.read_bytes()[:4] == b'\x00\x01\x00\x00', font.name
for family in ['Pretendard', 'Barlow']:
    assert 'SIL OPEN FONT LICENSE' in (ROOT / f'app/src/main/assets/licenses/{family}-OFL.txt').read_text()
# 러너 캐릭터 그림 — 투명 배경(알파)이 있는 WebP 이고, 화면에서 크게 쓰므로
# 작은 썸네일로 바뀌지 않았는지 캔버스 크기도 본다.
for name in ['avatar_male_running', 'avatar_female_idle', 'avatar_runo_idle_wnd_010_v2', 'avatar_lumi_idle_base_wnd_010']:
    head = (RES / 'drawable-nodpi' / f'{name}.webp').read_bytes()[:30]
    assert head[:4] == b'RIFF' and head[8:12] == b'WEBP' and head[12:16] == b'VP8X', name
    assert head[20] & 0x10, f'{name}: no alpha channel'
    width = int.from_bytes(head[24:27], 'little') + 1
    height = int.from_bytes(head[27:30], 'little') + 1
    assert width >= 900 and height >= 1300, f'{name}: {width}x{height}'
# RUNO 서 있기 — 캐릭터 가이드에서 떼어 낸 그림(원본 309×573 을 2배). 알파만 본다.
for name in ['outfit_runo_base', 'outfit_lumi_base']:
    head = (RES / 'drawable-nodpi' / f'{name}.webp').read_bytes()[:30]
    assert head[12:16] == b'VP8X' and head[20] & 0x10, f'{name}: no alpha'
    assert int.from_bytes(head[24:27], 'little') + 1 >= 1000, name
    assert int.from_bytes(head[27:30], 'little') + 1 >= 1000, name
head = (RES / 'drawable-nodpi' / 'avatar_male_idle.webp').read_bytes()[:30]
assert head[12:16] == b'VP8X' and head[20] & 0x10, 'avatar_male_idle: no alpha'
# RUNO 장비 그림 — 신발 52 · 의상 5 착용 전신과 의상 상품 5 (design/equipment 시트에서 떼어 냄)
import json
catalog = json.loads((ROOT / 'design/equipment/equipment-catalog.json').read_text(encoding='utf-8'))
shoe_ids = [x['id'] for x in catalog['shoes']]
outfit_ids = [x['id'] for x in catalog['outfits']]
assert len(shoe_ids) == 52 and len(set(shoe_ids)) == 52, len(shoe_ids)
for p in ['FIR', 'WAT', 'LIT', 'WND']:
    assert sum(1 for i in shoe_ids if i.startswith(p)) == 13, p
assert outfit_ids == ['CLO-001', 'CLO-002', 'CLO-003', 'CLO-004', 'CLO-005'], outfit_ids
wanted = [f"avatar_runo_idle_{i.lower().replace('-', '_')}" for i in shoe_ids + outfit_ids]
wanted += [f"outfit_{i.lower().replace('-', '_')}" for i in outfit_ids]
for name in wanted:
    head = (RES / 'drawable-nodpi' / f'{name}.webp').read_bytes()[:30]
    assert head[12:16] == b'VP8X' and head[20] & 0x10, f'{name}: no alpha'
# LUMI 장비 그림 — 신발 52(속성별 추천 의상) · 의상 5 착용 전신과 모자 포함 의상 상품 5
lumi = json.loads((ROOT / 'design/equipment/lumi/equipment-catalog.json').read_text(encoding='utf-8'))
assert sorted(x['id'] for x in lumi['shoes']) == sorted(shoe_ids), 'LUMI shoe ids differ from RUNO'
lumi_outfits = [x['id'] for x in lumi['outfits']]
assert lumi_outfits == ['LUM-' + i for i in outfit_ids], lumi_outfits
wanted = [f"avatar_lumi_idle_{i.lower().replace('-', '_')}" for i in shoe_ids + lumi_outfits]
wanted += [f"outfit_{i.lower().replace('-', '_')}" for i in lumi_outfits]
for name in wanted:
    head = (RES / 'drawable-nodpi' / f'{name}.webp').read_bytes()[:30]
    assert head[12:16] == b'VP8X' and head[20] & 0x10, f'{name}: no alpha'
print('PASS: 9 bounded, click-free PCM cues; 4-locale setting parity; font binaries and licenses; 5 base/starter alpha avatar images; 52 shoe + 5 outfit figures and 5 outfit products for RUNO and for LUMI')
