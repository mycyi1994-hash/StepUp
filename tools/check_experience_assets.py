"""Checks the shipped earcons, localization parity, font binaries and attribution."""
from pathlib import Path
import struct
import wave
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'

def webp_has_alpha(head: bytes) -> bool:
    if head[:4] != b'RIFF' or head[8:12] != b'WEBP':
        return False
    if head[12:16] == b'VP8X':
        return bool(head[20] & 0x10)
    if head[12:16] == b'VP8L':
        # Lossless WebP stores the alpha flag in bit 28 of its image header.
        return head[20] == 0x2f and bool(int.from_bytes(head[21:25], 'little') & (1 << 28))
    return False
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
manifest = __import__('json').loads((ROOT / 'design/redesign-2026-09/audio/manifest.json').read_text(encoding='utf-8'))
assert len(manifest) == 29
for entry in manifest:
    source = ROOT / 'design/redesign-2026-09/audio' / entry['file']
    packaged = RES / 'raw' / source.name
    assert source.is_file() and packaged.is_file(), entry['file']
    assert source.read_bytes() == packaged.read_bytes(), f'{source.name}: packaged audio differs'
    with wave.open(str(packaged)) as audio:
        assert (audio.getnchannels(), audio.getsampwidth(), audio.getframerate()) == (1, 2, 44100), source.name
        seconds = audio.getnframes() / audio.getframerate()
        assert (7.9 < seconds < 8.1) if source.parent.name == 'ambience' else (0.09 < seconds < 2.0), source.name
base = {node.attrib['name'] for node in ET.parse(RES / 'values/experience.xml').getroot()}
for locale in ['values-ko', 'values-ja', 'values-zh']:
    assert {node.attrib['name'] for node in ET.parse(RES / locale / 'experience.xml').getroot()} == base
for font in (RES / 'font').glob('*.ttf'):
    assert font.read_bytes()[:4] == b'\x00\x01\x00\x00', font.name
for family in ['Pretendard', 'Barlow']:
    assert 'SIL OPEN FONT LICENSE' in (ROOT / f'app/src/main/assets/licenses/{family}-OFL.txt').read_text()

from PIL import Image
for path in (RES / 'drawable-nodpi').glob('sneaker_*.webp'):
    assert webp_has_alpha(path.read_bytes()[:30]), path.name
assert len(list((RES / 'drawable-nodpi').glob('sneaker_*.webp'))) == 52
for name in ('home_banner_blue_night.png', 'home_banner_dawn.png'):
    with Image.open(RES / 'drawable-nodpi' / name) as im:
        assert im.width >= 1500 and 1.8 <= im.width / im.height <= 2.2, name
assert not list((RES / 'drawable-nodpi').glob('avatar_*'))
assert not list((RES / 'drawable-nodpi').glob('outfit_*'))
assert not list((RES / 'drawable-nodpi').glob('run_frame_*'))
assert not list((RES / 'drawable-nodpi').glob('scene_*'))
print('PASS: existing sound/font/localization checks; 52 alpha shoe assets; 2 independent landscape banners; retired images absent')
