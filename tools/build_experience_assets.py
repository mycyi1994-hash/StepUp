"""Reproducible, original StepUp UI earcons. No runtime synthesis or network audio."""
from pathlib import Path
import math
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
RATE = 44100
# (start, frequency, duration, gain); a shared D-major palette keeps cues related.
CUES = {
    "tap": [(0, 880, .045, .22)],
    "select": [(0, 587.33, .07, .20), (.025, 880, .08, .15)],
    "success": [(0, 587.33, .17, .23), (.08, 739.99, .19, .22), (.17, 880, .26, .19)],
    "reward": [(0, 587.33, .23, .22), (.10, 739.99, .24, .21), (.20, 880, .31, .19), (.30, 1174.66, .38, .16)],
    "start": [(0, 587.33, .12, .23), (.085, 880, .25, .22)],
    "pause": [(0, 659.25, .09, .21), (.055, 440, .12, .17)],
    "lap": [(0, 880, .12, .22), (.08, 1174.66, .20, .17)],
    "countdown": [(0, 587.33, .12, .25)],
    "error": [(0, 293.66, .10, .21), (.095, 277.18, .14, .17)],
}

def render(notes):
    duration = max(start + duration for start, _, duration, _ in notes) + .025
    samples = [0.0] * math.ceil(duration * RATE)
    for start, frequency, duration, gain in notes:
        for n in range(int(duration * RATE)):
            t = n / RATE
            attack = min(1.0, t / .006)
            release = min(1.0, (duration - t) / .030)
            envelope = attack * release * math.exp(-4.5 * t / duration)
            tone = math.sin(math.tau * frequency * t) + .12 * math.sin(math.tau * frequency * 2 * t)
            samples[int(start * RATE) + n] += tone * envelope * gain
    assert max(map(abs, samples)) < .8, "Clipping/headroom check failed"
    return b"".join(struct.pack('<h', round(s * 32767)) for s in samples)

if __name__ == '__main__':
    target = ROOT / 'app/src/main/res/raw'
    target.mkdir(parents=True, exist_ok=True)
    for cue, notes in CUES.items():
        with wave.open(str(target / f'cue_{cue}.wav'), 'wb') as audio:
            audio.setnchannels(1)
            audio.setsampwidth(2)
            audio.setframerate(RATE)
            audio.writeframes(render(notes))
    print(f'Created {len(CUES)} original, mono PCM earcons')
