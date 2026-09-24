"""Generate StepUp's original, deterministic mobile SFX pack.

Run from the repository root with: python design/redesign-2026-09/audio/generate_sfx.py
This produces audio assets only; the app's playback mapping is intentionally untouched.
"""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np
import soundfile as sf
from scipy.signal import butter, sosfilt


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "sfx"
SR = 44_100
RNG = np.random.default_rng(20260925)


def canvas(seconds: float) -> np.ndarray:
    return np.zeros(round(seconds * SR), dtype=np.float64)


def add(dest: np.ndarray, sound: np.ndarray, at: float = 0) -> None:
    start = round(at * SR)
    if start >= len(dest):
        return
    end = min(start + len(sound), len(dest))
    dest[start:end] += sound[: end - start]


def shape(n: int, attack: float = .004, release: float = .030) -> np.ndarray:
    t = np.arange(n) / SR
    duration = n / SR
    return np.minimum(1., t / max(attack, 1 / SR)) * np.minimum(
        1., (duration - t) / max(release, 1 / SR)
    )


def ping(freq: float, seconds: float, *, amp: float = 1., decay: float | None = None,
         bright: float = .12, slide: float = 0) -> np.ndarray:
    n = round(seconds * SR)
    t = np.arange(n) / SR
    hz = freq * (1 + slide * np.exp(-t / .045))
    phase = 2 * np.pi * np.cumsum(hz) / SR
    envelope = (1 - np.exp(-t / .003)) * np.exp(-t / (decay or seconds / 3.1)) * shape(n)
    wave = np.sin(phase) + bright * np.sin(2 * phase + .3) + bright * .27 * np.sin(3 * phase)
    return amp * wave * envelope


def noise(seconds: float, *, low: float = 1200, high: float = 9500,
          amp: float = 1., decay: float = .045, attack: float = .001) -> np.ndarray:
    n = round(seconds * SR)
    white = RNG.standard_normal(n)
    if low <= 0:
        sos = butter(3, high, "lowpass", fs=SR, output="sos")
    else:
        sos = butter(3, [low, high], "bandpass", fs=SR, output="sos")
    band = sosfilt(sos, white)
    peak = max(np.max(np.abs(band)), 1e-9)
    t = np.arange(n) / SR
    env = (1 - np.exp(-t / max(attack, 1 / SR))) * np.exp(-t / decay) * shape(n)
    return amp * band / peak * env


def whoosh(seconds: float, *, amp: float = .35, upward: bool = True) -> np.ndarray:
    n = round(seconds * SR)
    white = RNG.standard_normal(n)
    bands = []
    for low, high in [(200, 2200), (1100, 5900), (3500, 12000)]:
        band = sosfilt(butter(2, [low, high], "bandpass", fs=SR, output="sos"), white)
        bands.append(band / max(np.max(np.abs(band)), 1e-9))
    p = np.linspace(0, 1, n)
    progression = p if upward else 1 - p
    mix = ((1 - progression) ** 2 * bands[0] +
           2 * progression * (1 - progression) * bands[1] +
           progression ** 2 * bands[2])
    env = np.sin(np.pi * p) ** 1.8 * shape(n, .008, .035)
    return amp * mix * env


def sweep(start: float, end: float, seconds: float, *, amp: float = .35,
          decay: float | None = None, bright: float = .08) -> np.ndarray:
    n = round(seconds * SR)
    p = np.linspace(0, 1, n)
    hz = start * (end / start) ** p
    phase = 2 * np.pi * np.cumsum(hz) / SR
    env = np.sin(np.pi * p) ** .75 * shape(n, .006, .025)
    if decay is not None:
        env *= np.exp(-np.arange(n) / SR / decay)
    return amp * (np.sin(phase) + bright * np.sin(2 * phase)) * env


def thud(seconds: float = .25, amp: float = .50) -> np.ndarray:
    n = round(seconds * SR)
    t = np.arange(n) / SR
    freq = 54 + 88 * np.exp(-t / .026)
    phase = 2 * np.pi * np.cumsum(freq) / SR
    body = np.sin(phase) * np.exp(-t / .068) * shape(n, .002, .035)
    return amp * body


def tick(*, amp: float = .42, low: float = 1550) -> np.ndarray:
    result = canvas(.074)
    add(result, noise(.058, low=low, amp=amp, decay=.013))
    add(result, ping(790, .065, amp=amp * .28, decay=.016, bright=.30))
    return result


def echo(signal: np.ndarray, taps: tuple[tuple[float, float], ...]) -> np.ndarray:
    result = signal.copy()
    for delay, gain in taps:
        offset = round(delay * SR)
        result[offset:] += gain * signal[:-offset]
    return result


def finish(signal: np.ndarray, *, peak: float = .29) -> np.ndarray:
    # Keep the same quiet relative level as the app's existing SoundPool cues.
    filtered = sosfilt(butter(2, 32, "highpass", fs=SR, output="sos"), signal)
    filtered -= np.mean(filtered)
    filtered *= shape(len(filtered), .003, .025)
    level = np.max(np.abs(filtered))
    if level < 1e-8:
        raise ValueError("empty sound")
    return np.asarray(filtered * (peak / level), dtype=np.float32)


def create() -> dict[str, np.ndarray]:
    sounds: dict[str, np.ndarray] = {}

    x = canvas(.105)
    add(x, tick(amp=.53, low=1700))
    add(x, ping(690, .082, amp=.31, decay=.021, bright=.06))
    sounds["ui_tap"] = finish(x, peak=.22)

    x = canvas(.19)
    add(x, tick(amp=.28), 0)
    add(x, ping(620, .11, amp=.32, decay=.039), .006)
    add(x, ping(890, .13, amp=.43, decay=.061), .041)
    sounds["ui_select"] = finish(x, peak=.25)

    x = canvas(.18)
    add(x, tick(amp=.34, low=1000))
    add(x, sweep(730, 440, .15, amp=.43, decay=.09), .011)
    sounds["ui_back"] = finish(x, peak=.24)

    x = canvas(.18)
    add(x, tick(amp=.45, low=1700))
    add(x, ping(880, .11, amp=.30, decay=.038), .011)
    add(x, ping(1175, .10, amp=.38, decay=.042), .051)
    sounds["ui_toggle"] = finish(x, peak=.25)

    x = canvas(.36)
    add(x, whoosh(.27, amp=.42), .008)
    add(x, ping(523.25, .19, amp=.30, decay=.075), .094)
    add(x, ping(783.99, .17, amp=.30, decay=.078), .151)
    sounds["ui_sheet_open"] = finish(x)

    x = canvas(.27)
    add(x, whoosh(.18, amp=.30, upward=False))
    add(x, ping(587.33, .17, amp=.34, decay=.049), .012)
    add(x, tick(amp=.23, low=950), .136)
    sounds["ui_sheet_close"] = finish(x, peak=.25)

    x = canvas(.43)
    add(x, whoosh(.34, amp=.48), .02)
    add(x, sweep(380, 790, .24, amp=.29), .034)
    add(x, ping(880, .19, amp=.27, decay=.080), .174)
    sounds["ui_background_switch"] = finish(x, peak=.27)

    x = canvas(.49)
    add(x, thud(.17, .23))
    add(x, tick(amp=.58, low=2000), .018)
    add(x, ping(587.33, .24, amp=.38, decay=.085), .063)
    add(x, ping(880, .29, amp=.42, decay=.11), .117)
    sounds["item_equip"] = finish(x, peak=.31)

    x = canvas(.49)
    add(x, ping(587.33, .29, amp=.40, decay=.09), .005)
    add(x, ping(880, .29, amp=.45, decay=.11), .115)
    add(x, noise(.18, low=3100, amp=.12, decay=.065), .122)
    sounds["action_success"] = finish(x, peak=.28)

    x = canvas(.34)
    add(x, tick(amp=.25, low=700))
    add(x, sweep(350, 240, .24, amp=.46, decay=.14), .022)
    add(x, ping(185, .24, amp=.21, decay=.08, bright=.24), .065)
    sounds["action_error"] = finish(x, peak=.25)

    x = canvas(.85)
    add(x, thud(.20, .18))
    for at, freq, amp in [(0.00, 587.33, .34), (.13, 783.99, .40), (.26, 1174.66, .45)]:
        add(x, ping(freq, .42, amp=amp, decay=.16, bright=.17), at)
    add(x, noise(.38, low=4300, amp=.15, decay=.15), .30)
    sounds["reward_claim"] = finish(echo(x, ((.071, .11), (.139, .065))), peak=.31)

    x = canvas(.19)
    add(x, ping(659.25, .16, amp=.54, decay=.055, bright=.055, slide=.045))
    add(x, tick(amp=.16, low=2200))
    sounds["run_countdown"] = finish(x, peak=.26)

    x = canvas(.57)
    add(x, thud(.24, .48))
    add(x, whoosh(.29, amp=.34), .025)
    add(x, ping(587.33, .27, amp=.42, decay=.092), .097)
    add(x, ping(880, .28, amp=.45, decay=.105), .205)
    sounds["run_start"] = finish(x, peak=.30)

    x = canvas(.28)
    add(x, ping(523.25, .15, amp=.45, decay=.052), .007)
    add(x, ping(392, .15, amp=.38, decay=.050), .092)
    add(x, tick(amp=.24, low=750), .104)
    sounds["run_pause"] = finish(x, peak=.27)

    x = canvas(.39)
    add(x, tick(amp=.30, low=1200))
    add(x, sweep(440, 880, .23, amp=.34), .009)
    add(x, ping(880, .20, amp=.37, decay=.073), .142)
    sounds["run_resume"] = finish(x, peak=.28)

    x = canvas(.46)
    add(x, tick(amp=.20), .006)
    add(x, ping(783.99, .26, amp=.41, decay=.095), .019)
    add(x, ping(1046.50, .29, amp=.44, decay=.105), .127)
    sounds["run_lap"] = finish(x, peak=.29)

    x = canvas(.98)
    add(x, thud(.25, .26))
    for at, freq in [(0, 440), (.12, 587.33), (.24, 783.99), (.40, 1174.66)]:
        add(x, ping(freq, .46, amp=.40, decay=.18, bright=.16), at)
    add(x, noise(.42, low=4200, amp=.14, decay=.17), .42)
    sounds["run_finish"] = finish(echo(x, ((.082, .10), (.16, .06))), peak=.32)

    x = canvas(.59)
    add(x, tick(amp=.35, low=1500))
    add(x, whoosh(.39, amp=.32), .05)
    add(x, ping(440, .28, amp=.25, decay=.10), .11)
    add(x, ping(880, .27, amp=.29, decay=.12), .226)
    sounds["draw_enter"] = finish(x, peak=.28)

    x = canvas(1.30)
    for at, freq in [(.09, 220), (.36, 293.66), (.63, 392), (.90, 523.25)]:
        add(x, ping(freq, .40, amp=.27, decay=.16, bright=.13, slide=-.10), at)
    add(x, sweep(155, 820, .96, amp=.27), .06)
    add(x, whoosh(.89, amp=.36), .19)
    add(x, noise(.32, low=3600, amp=.17, decay=.12), .94)
    sounds["draw_charge"] = finish(x, peak=.31)

    x = canvas(.96)
    add(x, thud(.31, .64))
    add(x, tick(amp=.76, low=1150), .022)
    add(x, whoosh(.41, amp=.48), .095)
    for at, freq in [(.18, 734), (.28, 1320), (.38, 1990)]:
        add(x, ping(freq, .43, amp=.30, decay=.13, bright=.25), at)
    add(x, noise(.43, low=4600, amp=.19, decay=.13), .37)
    sounds["draw_box_open"] = finish(echo(x, ((.063, .075),)), peak=.33)

    x = canvas(.67)
    add(x, tick(amp=.19), .012)
    add(x, ping(587.33, .36, amp=.39, decay=.12), .052)
    add(x, ping(783.99, .35, amp=.41, decay=.14), .182)
    sounds["draw_reveal_common"] = finish(x, peak=.28)

    x = canvas(.94)
    add(x, thud(.21, .14))
    for at, freq in [(.06, 587.33), (.19, 880), (.34, 1174.66)]:
        add(x, ping(freq, .48, amp=.41, decay=.19, bright=.18), at)
    add(x, noise(.44, low=4400, amp=.12, decay=.16), .37)
    sounds["draw_reveal_rare"] = finish(echo(x, ((.083, .09),)), peak=.31)

    x = canvas(1.28)
    add(x, thud(.28, .24))
    for at, freq in [(.04, 440), (.18, 587.33), (.32, 880), (.50, 1318.51)]:
        add(x, ping(freq, .63, amp=.41, decay=.25, bright=.20), at)
    add(x, whoosh(.62, amp=.22), .23)
    add(x, noise(.53, low=5000, amp=.16, decay=.24), .51)
    sounds["draw_reveal_epic"] = finish(echo(x, ((.09, .12), (.18, .065))), peak=.33)

    x = canvas(1.82)
    add(x, thud(.31, .39))
    add(x, whoosh(.68, amp=.29), .09)
    for at, freq in [(.04, 293.66), (.19, 440), (.35, 587.33), (.53, 880), (.75, 1174.66)]:
        add(x, ping(freq, .82, amp=.43, decay=.34, bright=.20), at)
    add(x, ping(1760, .75, amp=.23, decay=.30, bright=.29), .81)
    add(x, noise(.64, low=4900, amp=.18, decay=.24), .88)
    sounds["draw_reveal_legendary"] = finish(echo(x, ((.087, .13), (.174, .08), (.26, .035))), peak=.34)

    x = canvas(.32)
    add(x, tick(amp=.28, low=800))
    add(x, sweep(590, 390, .22, amp=.32, decay=.11), .023)
    sounds["draw_cancel"] = finish(x, peak=.24)

    x = canvas(.39)
    add(x, tick(amp=.35, low=700))
    add(x, sweep(420, 210, .30, amp=.43, decay=.16), .024)
    add(x, noise(.16, low=280, high=1600, amp=.19, decay=.05), .063)
    sounds["draw_fail"] = finish(x, peak=.26)

    return sounds


def seamless_air(low: float, high: float, seconds: float = 8.) -> np.ndarray:
    n = round(seconds * SR)
    overlap = round(.7 * SR)
    source = RNG.standard_normal(n + overlap)
    filtered = sosfilt(butter(2, [low, high], "bandpass", fs=SR, output="sos"), source)
    filtered /= max(np.max(np.abs(filtered)), 1e-9)
    loop = filtered[:n].copy()
    fade = np.linspace(0, 1, overlap, endpoint=False)
    loop[:overlap] = filtered[n:] * (1 - fade) + filtered[:overlap] * fade
    return loop


def create_ambience() -> dict[str, np.ndarray]:
    """Optional, quiet eight-second loops shared by the seven visual scenes."""
    n = 8 * SR
    t = np.arange(n) / SR
    cycle = 2 * np.pi * t / 8

    night = (.33 + .13 * np.sin(cycle - .4)) * seamless_air(70, 900)
    night += (.13 + .07 * np.sin(2 * cycle + .7)) * seamless_air(500, 3800)
    night += .052 * np.sin(2 * np.pi * 110 * t) + .022 * np.sin(2 * np.pi * 220 * t)
    add(night, ping(880, .38, amp=.085, decay=.14, bright=.04), 2.34)
    add(night, ping(1174.66, .30, amp=.065, decay=.11, bright=.04), 5.91)

    dawn = (.32 + .13 * np.sin(cycle + .5)) * seamless_air(140, 1800)
    dawn += (.17 + .055 * np.sin(3 * cycle)) * seamless_air(1500, 7000)
    dawn += .028 * np.sin(2 * np.pi * 146.875 * t)
    add(dawn, sweep(1200, 1650, .20, amp=.08), 2.23)
    add(dawn, sweep(1350, 1920, .16, amp=.06), 5.54)

    terrace = (.29 + .075 * np.sin(cycle - .8)) * seamless_air(160, 1400)
    terrace += .11 * seamless_air(1800, 6000)
    terrace += .055 * np.sin(2 * np.pi * 165 * t)
    terrace += .017 * np.sin(2 * np.pi * 330 * t)
    add(terrace, ping(660, .41, amp=.06, decay=.13, bright=.08), 3.16)

    result = {}
    for name, sound in [
        ("ambience_night_river", night),
        ("ambience_dawn_river", dawn),
        ("ambience_wardrobe_terrace", terrace),
    ]:
        sound -= np.mean(sound)
        sound *= .095 / max(np.max(np.abs(sound)), 1e-9)
        seam = sound[0] - sound[-1]
        sound[-round(.02 * SR):] += seam * np.linspace(0, 1, round(.02 * SR))
        result[name] = np.asarray(sound, dtype=np.float32)
    return result


META = [
    ("ui_tap", "공통", "가벼운 버튼 터치"),
    ("ui_select", "공통", "하단 탭·선택 확정"),
    ("ui_back", "공통", "뒤로 가기"),
    ("ui_toggle", "공통", "설정 토글 전환"),
    ("ui_sheet_open", "공통", "시트·상세 패널 열기"),
    ("ui_sheet_close", "공통", "시트·상세 패널 닫기"),
    ("ui_background_switch", "공통", "홈 배경 좌우 전환"),
    ("item_equip", "꾸미기", "의상·신발 착용 확정"),
    ("action_success", "결과", "저장·게시·참여 성공"),
    ("action_error", "결과", "일반 오류·거절"),
    ("reward_claim", "결과", "보상 지급이 확정된 순간"),
    ("run_countdown", "러닝", "출발 카운트다운 한 박자"),
    ("run_start", "러닝", "실제 러닝 시작"),
    ("run_pause", "러닝", "일시정지"),
    ("run_resume", "러닝", "일시정지 후 재개"),
    ("run_lap", "러닝", "랩·구간 완료"),
    ("run_finish", "러닝", "러닝 결과 확정"),
    ("draw_enter", "뽑기", "미스터리 박스 화면 진입"),
    ("draw_charge", "뽑기", "거래 확정 후 상자 충전"),
    ("draw_box_open", "뽑기", "상자 열림 연출"),
    ("draw_reveal_common", "뽑기", "확정된 일반 결과 공개"),
    ("draw_reveal_rare", "뽑기", "확정된 희귀 결과 공개"),
    ("draw_reveal_epic", "뽑기", "확정된 에픽 결과 공개"),
    ("draw_reveal_legendary", "뽑기", "확정된 전설 결과 공개"),
    ("draw_cancel", "뽑기", "지갑 서명·거래 취소"),
    ("draw_fail", "뽑기", "뽑기 거래 실패"),
]

AMBIENCE_META = [
    ("ambience_night_river", "밤 강변 · 홈/러닝/프로필"),
    ("ambience_dawn_river", "새벽·노을 강변 · 홈/러닝/꾸미기"),
    ("ambience_wardrobe_terrace", "옷장 테라스 · 꾸미기"),
]


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    ambience_out = ROOT / "ambience"
    ambience_out.mkdir(parents=True, exist_ok=True)
    sounds = create()
    ambiences = create_ambience()
    assert set(sounds) == {name for name, _, _ in META}
    assert set(ambiences) == {name for name, _ in AMBIENCE_META}
    rows = []
    for name, group, use in META:
        signal = sounds[name]
        sf.write(OUT / f"{name}.wav", signal, SR, subtype="PCM_16")
        rows.append({
            "file": f"sfx/{name}.wav", "group": group, "use": use,
            "duration_ms": round(len(signal) / SR * 1000),
            "peak_dbfs": round(float(20 * np.log10(np.max(np.abs(signal)))), 1),
        })
    for name, use in AMBIENCE_META:
        signal = ambiences[name]
        sf.write(ambience_out / f"{name}.wav", signal, SR, subtype="PCM_16")
        rows.append({
            "file": f"ambience/{name}.wav", "group": "배경(선택)", "use": use,
            "duration_ms": round(len(signal) / SR * 1000),
            "peak_dbfs": round(float(20 * np.log10(np.max(np.abs(signal)))), 1),
        })
    (ROOT / "manifest.json").write_text(json.dumps(rows, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    # A listening reel is an asset preview, never an app event sound.
    for reel_name, selected in [
        ("preview_all", [name for name, _, _ in META]),
        ("preview_highlights", [
            "ui_select", "ui_background_switch", "item_equip", "run_start",
            "run_finish", "draw_charge", "draw_box_open",
            "draw_reveal_common", "draw_reveal_rare", "draw_reveal_epic", "draw_reveal_legendary",
        ]),
    ]:
        gap = np.zeros(round(.35 * SR), dtype=np.float32)
        reel = np.concatenate([part for name in selected for part in (sounds[name], gap)])
        sf.write(ROOT / f"{reel_name}.wav", reel, SR, subtype="PCM_16")
    ambience_reel = np.concatenate([
        part for name, _ in AMBIENCE_META
        for part in (ambiences[name][: 4 * SR], np.zeros(round(.5 * SR), dtype=np.float32))
    ])
    sf.write(ROOT / "preview_ambience.wav", ambience_reel, SR, subtype="PCM_16")


if __name__ == "__main__":
    main()
