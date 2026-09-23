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
for name in ['avatar_male_running', 'avatar_female_idle']:
    head = (RES / 'drawable-nodpi' / f'{name}.webp').read_bytes()[:30]
    assert head[:4] == b'RIFF' and head[8:12] == b'WEBP' and head[12:16] == b'VP8X', name
    assert head[20] & 0x10, f'{name}: no alpha channel'
    width = int.from_bytes(head[24:27], 'little') + 1
    height = int.from_bytes(head[27:30], 'little') + 1
    assert width >= 900 and height >= 1300, f'{name}: {width}x{height}'
print('PASS: 9 bounded, click-free PCM cues; 4-locale setting parity; font binaries and licenses; 2 alpha avatar images')
