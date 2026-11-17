#!/usr/bin/env python3
"""러닝 음성 코치 — 규칙 엔진 참조 구현과 가상 러닝 검증.

설계 문서: docs/run-voice-coaching-2026-11-17.md

앱에 넣을 규칙(문서 "코치가 하는 말 전부" · "페이스를 판단하는 방법" …)을 그대로 옮긴 순수 계산이다.
가상 러닝(초보 달리기-걷기 · 꾸준한 5km · 후반 처짐 · 목표 페이스 · 신호등 · GPS 끊김 · 걷기 …)에
폰이 실제로 받는 것과 같은 흐린 입력(GPS 오차 · 걸음 뭉쳐 오기 · 거리 계단)을 넣어 코치가 언제 무엇을 말하는지 본다.

    python3 tools/coach_sim.py                  # 장면별 코치 말 전체 + 검증 표
    python3 tools/coach_sim.py --scene target5  # 한 장면만
    python3 tools/coach_sim.py --check          # 검증 기준을 못 넘으면 1 로 끝난다

앱 코드(Kotlin `domain/runcoach`)는 이 파일과 같은 값 · 같은 차례를 따른다 — 값을 바꾸면 둘을 함께.
"""
from __future__ import annotations

import argparse
import math
import random
import statistics
import sys
from collections import deque
from dataclasses import dataclass, field
from typing import Callable, Deque, Dict, List, Optional, Sequence, Tuple

# ────────────────────────────────────────────────────────────────────────────
# 규칙 값 — 문서 "규칙 값" 표와 같다. 규칙에 쓰는 숫자는 모두 여기에 이름을 붙여 둔다.
# ────────────────────────────────────────────────────────────────────────────

STRIDE_M = 0.762            # 앱 RewardEconomy.STRIDE_METERS
GPS_MIN_M = 100.0           # 앱 RunDistance.GPS_MIN_M — 이 뒤로 GPS 거리. 구간 안내도 이 뒤부터

# 달리기 · 걷기 · 서 있음
CAD_WINDOW_S = 30           # 케이던스 창(조언에 쓰는 값)
MODE_WINDOW_S = 15          # 달리기 · 걷기를 가르는 창(빨리 알아채야 한다)
RUN_CAD = 140               # 이 이상이면 달리기
WALK_CAD = 130              # 이 아래면 걷기(사이는 앞 상태 유지)
MODE_CAD_LOW = 120          # 이 위에서는 속도도 본다 — 느린 조깅(분당 130걸음대)을 걷기로 잘못 보지 않게
RUN_SPEED_MPS = 1.9         # 그때 이 속도(8분 46초 페이스) 이상이면 달리기 — 걷기로는 나오지 않는 속도
SPEED_WINDOW_S = 20         # 그 속도를 재는 시간
SPEED_FRESH_S = 12          # 마지막 거리 점이 이보다 오래됐으면 속도를 모르는 것으로
RUN_CAD_MARGIN = 10         # 평소 케이던스가 낮은 러너는 문턱을 (평소 − 10)으로 낮춘다(120 아래로는 아님)
STAND_CAD = 30              # 이 아래면 서 있음
MODE_DWELL_S = 6            # 상태가 바뀌려면 이만큼 이어져야
STEP_STALE_S = 20           # 걸음 소식이 이만큼 없고 GPS 로도 움직이지 않으면 멈춘 것 — 케이던스를 지금 시각까지로 잰다
MOVING_MPS = 1.0            # GPS 로 이 속도 이상이면 움직이는 중
WALKER_AFTER_S = 240        # 이만큼 지난 뒤, 걷는 시간이 달린 시간의 WALKER_RATIO 배를 넘으면 걷는 사람으로 대한다
WALKER_RATIO = 3

# 페이스 — 가상의 러너와 벌어진 거리
GHOST_SLACK = 0.03          # 목표 페이스: 3% 안쪽 차이는 쌓지 않는다
GHOST_SLACK_USUAL = 0.06    # 평소 페이스(출발 가드): 6%
GHOST_BIAS_PER_M = 0.0027   # GPS 가 흐릴수록 거리가 길게 재진다 — 오차 1m 마다 '빠르다' 쪽 여유를 이만큼 넓힌다
GHOST_BIAS_FREE_M = 5.0     # … 오차가 이보다 클 때만
GHOST_H_MIN_M = 12.0        # 이만큼 앞서거나 뒤처지면 알린다(최소)
GHOST_H_ACC = 4.0           # … 또는 GPS 오차의 4배 가운데 큰 쪽
GHOST_H_ACC_START = 3.0     # 출발 6분 동안은 3배(출발 과속은 빨리 잡는 쪽이 이득이 크다)
GHOST_GAP_S = 20            # 거리 점 사이가 이보다 길면 처음부터
PACE_MAX_ACC_M = 12.0       # GPS 오차가 이보다 크면 페이스 조언(출발 · 목표 · 처짐)을 하지 않는다 — 구간 안내만
PACE_SETTLE_S = 60          # 출발 뒤 이만큼은 페이스 판단을 쉰다(속도가 붙고 GPS 가 자리 잡는 시간)
RESUME_SETTLE_S = 30        # 일시정지를 풀거나 걷다 다시 달릴 때
GPS_BACK_SETTLE_S = 45      # GPS 가 끊겼다 돌아왔을 때
GPS_GAP_RESET_S = 15        # 이만큼 끊겼으면 거리 점을 새로 모은다
PACE_CLEAR_S = 60           # (말하기 전) 이만큼 다시 쌓이지 않으면 벗어남이 끝난 것
PACE_BACK_M = 400.0         # 말한 뒤 이만큼(또는 GPS 오차의 60배) 달린 페이스로 "돌아왔다"를 본다
PACE_BACK_ACC = 60.0
PACE_BACK_BAND = 0.035      # 그 페이스가 목표의 ±3.5% 안이고
PACE_BACK_NOW = 2.0         # … 가장 최근 200m 도 그 두 배(±7%) 안이고(빠르다 느려지는 중에 평균만 맞는 것을 거른다)
PACE_BACK_HOLD_S = 20       # 그 상태가 이만큼 이어지면 돌아온 것
PACE_BACK_SAY_S = 40        # "돌아왔어요"는 이 안에 말하지 못하면 접는다
DELTA_MIN_M = 200.0         # "몇 초 빠르다"는 이만큼 달린 값으로만 말한다
PACE_RECUE_S = (120, 240)   # 같은 벗어남에서 다시 말하는 간격(두 번째 · 세 번째)
PACE_CUES_PER_EXCURSION = 3 # 세 번 말해도 그대로면 그 방향(빠름 · 느림)은 이번 러닝에서 더 말하지 않는다
PACE_CUES_PER_RUN = 5       # 한 러닝에서 같은 방향 조언은 많아야 이만큼
HILL_EXTRA = 2.5            # 느려졌는데 발은 그대로면(오르막 · 맞바람) 문턱을 이만큼 올린다
FEET_SLOWED = 0.96          # 발이 느려졌다 = 최근 1분 달리기 케이던스가 이번 러닝 달리기 케이던스의 96% 이하
LATE_SHARE = 0.75           # 목표 거리의 이만큼을 지나면 '편하게'는 재촉하지 않는다

START_GUARD_FROM_S = 60     # 출발 가드가 보는 시간
START_GUARD_TO_S = 360

# 처짐
BLOCK_M = 200.0             # 처짐 판단용 구간
BLOCK_RUN_SHARE = 0.9       # 구간의 이만큼을 달렸어야 달린 구간
FADE_MIN_ACTIVE_S = 12 * 60
FADE_MIN_BLOCKS = 8
FADE_TRIM = 3               # 자리 잡은 페이스 = 처음 셋 · 마지막 셋을 뺀 구간의 중앙값
FADE_PACE = 1.08            # 자리 잡은 페이스보다 8% 느림(최근 세 구간 평균)
FADE_PACE_EACH = 1.075      # … 그리고 최근 두 구간이 저마다 7.5% 이상 느림(한 구간만 갑자기 느린 것은 처짐이 아니다)
FADE_CAD = 0.96             # 그리고 케이던스 4% 떨어짐
FADE_RUN_S = 180            # 걷다 막 달리기 시작한 때는 보지 않는다(이만큼 이어 달린 뒤)
FADE_STEP = 0.15            # 앞 세 구간 평균보다 15%(그 앞 구간은 절반인 7.5%) 넘게 달라지면 일부러 속도를 바꾼 것 — 거기서부터 새로 본다
FADE_MAX = 2
FADE_GAP_S = 8 * 60

# 걷기
WALK_BOUT_S = 20            # 달리다 걷기로 바뀐 뒤 이만큼 걸으면 한마디(러닝마다 한 번)
WALK_BOUT_AFTER_RUN_S = 180
WALK_BOUT_CAD = 80          # 걷는 것으로 치는 가장 낮은 케이던스(멈추는 중 · 다시 출발하는 중은 걷기가 아니다)
BRISK_CAD = 100             # 걷기: 분당 100걸음 = 중강도(CADENCE-Adults)
BRISK_LOW_S = 180
BRISK_MIN_ACTIVE_S = 300
BRISK_OK_S = 600            # 이만큼 100걸음 이상으로 걸으면 "잘 걷고 있어요"(구간 안내에 붙여서만)
BRISK_MAX = 2
BRISK_GAP_S = 10 * 60

# 장거리 · 시스템
LONG_RUN = (1.10, 2.00)     # 최근 30일 가장 긴 러닝의 110% · 두 배
LONG_RUN_MIN_M = 2000.0
LONG_CARD = 1.30            # 러닝 뒤 "내일은 쉬거나 가볍게"는 130% 부터
STILL_S = 30                # 앱 RunStill
GPS_WEAK_S = 60             # 이만큼 끊기면 알린다
GPS_WEAK_MAX = 2            # 러닝에 두 번까지
SHOE_LOW_S = 5 * 60         # 신발 5분 남음
SHOE_NEAR_S = 120           # 2분 아래면 "거의 다 닳았어요"
CREW_LOW = 0.20             # 크루 공용 체력 20%
OVERSPEED_STOP_S = 30       # 앱 RunOverspeed.STOP_MS
GOAL_SPLIT_NEAR_M = 50.0    # 목표 지점과 겹치는 구간 안내는 목표 달성 말이 대신한다
RESTORE_QUIET_S = 60        # 러닝이 되살아난 뒤 조용한 시간

# '자세히'
FORM_FROM_S = 8 * 60        # 자세 한마디(러닝마다 셋까지, 10분 간격, 다른 조언이 5분 없었을 때)
FORM_GAP_S = 10 * 60
FORM_QUIET_S = 5 * 60
HYDRATE_S = (45 * 60, 30 * 60)   # 45분에 한 번, 그 뒤 30분마다
HYDRATE_WINDOW_S = 120

# 구간 안내
LAP_DIRTY_S = 20            # 구간 안에 걷기 · 멈춤 · GPS 끊김이 이만큼 있으면 견주지 않는다
LAP_COMPARE = 0.05          # 앞 구간과 5% 이상 다를 때만 견줘 말한다
USUAL_COMPARE = 0.04        # 첫 구간이 평소와 4% 이상 다르면 "평소보다 …"
STEADY_BAND = 0.025         # 세 구간이 ±2.5% 면 "고르게"
BEST_1K_MARGIN = 0.99       # 가장 빠른 1km 보다 1% 넘게 빨라야 "가장 빠른"
TALK_TEST_S = (240, 480)    # 처음 쓰는 사람에게 "말할 수 있는 속도" 한마디를 하는 때

# 오늘의 한 가지 · 내 기준
FOCUS_START_FAST = 0.04     # 첫 1km 가 나머지 평균보다 4% 이상 빨랐던 러닝이 최근 셋 중 둘
FOCUS_CALM = 0.02           # 첫 1km 가 평소보다 2% 넘게 빠르지 않으면 차분한 출발
FOCUS_UNEVEN = 0.10         # 2km 뒤 구간의 (가장 느림 − 가장 빠름) ÷ 평균이 10% 이상인 러닝이 최근 셋 중 둘
FOCUS_EVEN_BAND = 0.03      # 지금까지 구간의 중앙값과 ±3% 면 같은 페이스
FOCUS_EVEN_DONE = 0.7       # 구간의 70% 가 그랬으면 해낸 것
FOCUS_RETURN_DAYS = 14      # 이만큼 쉬었으면 "오랜만"
FOCUS_RETURN_MAX_DAYS = 180 # 이보다 오래 쉬었으면 처음 하는 사람으로
FOCUS_RETURN_SHARE = 0.7    # 오랜만인 날은 쉬기 전 평소 거리의 70% 에서 한마디, 100% 에서 한 번 더
FOCUS_FARTHER_M = 500.0     # 조금 더: 평소 거리 + 500m(또는 10%), 최근 가장 긴 러닝의 110% 를 넘지 않게
FOCUS_FARTHER_BAND = 0.10
FOCUS_FARTHER_DAYS = 4
EASY_DAY_LOAD = 1.3         # 최근 7일 거리가 그 앞 3주 평균의 1.3배를 넘으면 가볍게
PROFILE_DAYS = 42           # 평소 값은 최근 6주
PROFILE_HALF_LIFE = 14.0    # 최근 러닝일수록 무겁게(2주마다 절반)
PROFILE_MIN_RUNS = 3
RUN_KIND_CAD = 135          # 러닝 평균 케이던스가 이 이상이거나 평균 속도가 RUN_SPEED_MPS 이상이면 달린 러닝
BEST_1K_DAYS = 90
LAP_MIN_S = 150             # 앱 CoachingQuality.MIN_SPLIT_SEC

# 말의 차례
MIN_GAP = {0: 1, 1: 4, 2: 15, 3: 45, 4: 45}   # 앞말이 끝난 뒤 다음 말까지(초) — 우선순위마다
MIN_GAP_SPLIT = 6           # 구간 안내는 때가 중요하다 — 앞말 뒤 6초면 말한다
ADVICE_EVERY = {"BRIEF": None, "STANDARD": 240, "DETAILED": 120}   # 조언 · 격려 예산: "N초에 하나"
MERGE_AHEAD_S = 40          # 구간 안내가 이 안에 오면 조언을 거기 붙인다
START_QUIET_S = 20          # 출발 인사 뒤 조용한 시간
CUE_TTL = {0: 20, 1: 12, 2: 20, 3: 15, 4: 15}  # 한 번뿐인 말(구간 · 일시정지 …)은 차례가 이만큼 안 오면 버린다
HOLD_S = {"HALFWAY": 60, "LAST_KM": 60, "LAST_400": 30, "LONG": 180, "FOCUS": 60, "START_ITEMS": 90}
MAX_UTTER_S = {"BRIEF": 9.0, "STANDARD": 9.0, "DETAILED": 11.0}   # 한 번에 말하는 길이. 넘으면 짧은 꼴로, 그래도 넘으면 꼬리를 뗀다

SYLLABLES_PER_S = 5.2       # 한국어 음성 빠르기(보통)
PAUSE_S = 0.28              # 마침표 · 쉼표


# ────────────────────────────────────────────────────────────────────────────
# 말하기 — 숫자를 글로(음성 엔진이 6:15 · km 를 제멋대로 읽지 않게)
# ────────────────────────────────────────────────────────────────────────────

_SINO = ["", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"]
_NATIVE = ["", "한", "두", "세", "네", "다섯", "여섯", "일곱", "여덟", "아홉"]


def sino(n: int) -> str:
    """0 ~ 9999 를 한자어 수로(음절 수를 세는 데 쓴다 — 말할 글은 숫자 그대로 둔다)."""
    if n == 0:
        return "영"
    out = ""
    for unit, name in ((1000, "천"), (100, "백"), (10, "십")):
        d, n = divmod(n, unit)
        if d:
            out += ("" if d == 1 else _SINO[d]) + name
    return out + _SINO[n]


def pace_text(sec_per_km: float) -> str:
    s = int(round(sec_per_km))
    m, r = divmod(s, 60)
    return f"{m}분 {r}초" if r else f"{m}분"


def clock_text(sec: float) -> str:
    """시간. '시간'은 고유어로 읽어야 해서 글자로 넣고("한 시간"), 시간이 있으면 분을 빼지 않는다("한 시간 0분 33초")."""
    s = int(round(sec))
    h, rem = divmod(s, 3600)
    m, r = divmod(rem, 60)
    parts = []
    if h:
        parts.append(f"{_NATIVE[h] if h < 10 else h} 시간" if h < 10 else f"{h}시간")
    if m or (h and r):
        parts.append(f"{m}분")
    if r or not parts:
        parts.append(f"{r}초")
    return " ".join(parts)


def km_text(meters: float) -> str:
    """거리 — 0.1km 아래는 버린다(4.98km 를 5킬로미터라고 하지 않는다)."""
    tenths = int((meters + 0.5) // 100)
    if tenths % 10 == 0:
        return f"{tenths // 10}킬로미터"
    return f"{tenths / 10:.1f}킬로미터"


def delta_text(sec: float) -> str:
    """차이는 5초 단위 — "약 13초"보다 "15초"가 달리며 듣기 쉽다. 60초가 넘으면 분으로."""
    r = max(5, int(5 * round(abs(sec) / 5.0)))
    if r >= 60:
        return f"{r // 60}분 {r % 60}초" if r % 60 else f"{r // 60}분"
    return f"{r}초"


def with_particle(word: str, with_final: str, without: str) -> str:
    """받침에 따라 조사를 붙인다(을/를 · 과/와)."""
    last = word[-1]
    final = ("가" <= last <= "힣") and (ord(last) - 0xAC00) % 28 != 0
    return word + (with_final if final else without)


def speech_seconds(text: str) -> float:
    """말하는 데 걸리는 시간(초) 어림 — 음절 수와 쉼."""
    syll = 0
    num = ""
    pauses = 0

    def flush() -> int:
        nonlocal num
        if not num:
            return 0
        whole, _, frac = num.partition(".")
        n = len(sino(int(whole))) if whole else 0
        if frac:
            n += 1 + len(frac)
        num = ""
        return n

    for ch in text:
        if ch.isdigit() or (ch == "." and num and not num.endswith(".")):
            num += ch
            continue
        if num.endswith("."):
            num = num[:-1]
            syll += flush()
            pauses += 1
        else:
            syll += flush()
        if "가" <= ch <= "힣":
            syll += 1
        elif ch in ".,!?":
            pauses += 1
    syll += flush()
    return syll / SYLLABLES_PER_S + pauses * PAUSE_S


# ────────────────────────────────────────────────────────────────────────────
# 입력 — 앱의 러닝 서비스가 1초마다 주는 값(WalkSessionState 에서 만든다)
# ────────────────────────────────────────────────────────────────────────────

@dataclass
class Tick:
    wall: int                 # 시작부터 흐른 시간(멈춘 시간 포함, 초)
    active: int               # 달린 시간(멈춘 시간 뺌) = elapsedSec
    paused: bool
    steps: int
    steps_at: float           # 그 걸음 수가 세어진 때(달린 시간, 초) — 걸음 센서가 주는 시각
    live_m: float             # RunDistance.liveKm(GPS 100m 부터 GPS, 그 전엔 걸음)
    gps_m: float              # GPS 거리(gpsKm)
    gps_ok: bool              # 최근 10초 안에 믿을 만한 위치가 왔다
    acc_m: float              # 최근 위치 오차(중앙값)
    laps: List[Tuple[int, float, int]]  # (번호, 누적 km, 누적 초) — 앱 RunLap
    still: bool = False       # 앱 RunStill(30초)
    shoe_left_s: Optional[int] = None
    crew_pool_left: Optional[float] = None  # 0 ~ 1
    overspeed_left: Optional[int] = None    # 앱 RunOverspeed 가 세는 중이면 남은 초
    overspeed_stopped: bool = False
    location_on: bool = True


@dataclass
class Profile:
    """내 기록에서 만든 기준 — 문서 "내 기준"."""
    tier: int = 0                         # 0 기록 부족 · 1 평소 값 있음
    kind: str = "RUN"                     # RUN · WALK(평소 걷는 사람)
    usual_pace: Optional[float] = None    # 초/km
    usual_cadence: Optional[float] = None
    longest30_m: Optional[float] = None
    best_1k: Optional[float] = None       # 최근 90일 가장 빠른 1km(초)
    coached_runs: int = 0
    usual_m: Optional[float] = None       # 평소 거리
    focus: Optional[str] = None           # 오늘의 한 가지: RETURN · EASY_DAY · START_EASY · EVEN · FARTHER
    focus_goal_m: Optional[float] = None  # FARTHER · RETURN 의 거리
    focus_why: Optional[str] = None       # EASY_DAY 의 까닭: HARD(힘들었어요 두 번) · LOAD(많이 달림)


@dataclass
class PastRun:
    """폰에 남은 지난 러닝 한 줄(walk_sessions) + 코치 기록."""
    days_ago: float
    meters: float
    seconds: int
    steps: int
    laps: List[int] = field(default_factory=list)   # 1km 구간 기록(초)
    void: bool = False
    rpe: Optional[str] = None                        # EASY · OK · HARD(러닝 뒤 한 번 누르기)
    coached: bool = False
    focus: Optional[str] = None
    focus_done: bool = False


@dataclass
class Plan:
    level: str = "STANDARD"               # BRIEF · STANDARD · DETAILED
    style: str = "EASY"                   # EASY(편하게) · CHALLENGE(기록 도전)
    split: str = "KM"                     # KM · OFF
    goal_m: Optional[float] = None        # 코치 시트에서 고른 목표 거리(앱의 goalKm 기본값 5km 는 목표로 치지 않는다)
    target_pace: Optional[float] = None   # 초/km
    items: Sequence[str] = ()             # 출발 때 쓴 아이템(RUN_BOOSTER · REWARD_DOUBLE · ENERGY_DRINK)
    event: bool = False                   # 보상 이벤트 중


@dataclass
class Cue:
    id: str
    prio: int
    text: str
    tail: Optional[str] = None     # 구간 안내 끝에 붙일 짧은 꼴
    followup: bool = False         # 이미 꺼낸 이야기의 뒷말(조언 예산을 따로 쓰지 않는다)
    tail_only: bool = False        # 구간 안내에 붙을 때만 말한다(따로 말할 만큼 급하지 않다)
    interrupt: bool = False        # 말하던 것을 끊고 들어간다(과속뿐)
    short: Optional[str] = None    # 너무 길 때 쓰는 짧은 꼴(구간 안내)
    tail_key: Optional[str] = None # 제 꼬리를 실제로 말했을 때 "말했다"고 적을 이름
    born: int = 0


@dataclass
class Spoken:
    wall: int
    active: int
    id: str
    text: str
    seconds: float
    live_m: float = 0.0


def _wmedian(pairs: List[Tuple[float, float]]) -> Optional[float]:
    """무게 중앙값 — (값, 무게)."""
    pairs = sorted(p for p in pairs if p[1] > 0)
    total = sum(w for _, w in pairs)
    if total <= 0:
        return None
    acc = 0.0
    for v, w in pairs:
        acc += w
        if acc >= total / 2:
            return v
    return pairs[-1][0]


def run_kind(meters: float, seconds: float, steps: int) -> str:
    """달린 러닝인가 걸은 러닝인가 — 케이던스가 높거나, 걷기로는 나오지 않는 속도면 달린 것."""
    if seconds <= 0:
        return "RUN"
    return "RUN" if steps * 60.0 / seconds >= RUN_KIND_CAD or meters / seconds >= RUN_SPEED_MPS else "WALK"


def build_profile(history: List[PastRun]) -> Profile:
    """지난 러닝으로 내 기준과 오늘의 한 가지를 만든다 — 러닝 시작 전에 한 번."""
    runs = sorted((r for r in history if not r.void and r.meters >= 500 and r.seconds >= 300),
                  key=lambda r: r.days_ago)
    if not runs:
        return Profile()

    def kind(r: PastRun) -> str:
        return run_kind(r.meters, r.seconds, r.steps)

    last5 = runs[:5]
    k = "RUN" if sum(1 for r in last5 if kind(r) == "RUN") * 2 >= len(last5) else "WALK"
    same = [r for r in runs if kind(r) == k]
    since = runs[0].days_ago
    # 오래 쉬었다 돌아온 사람은 쉬기 전 6주로 평소 값을 만든다(쉰 날만큼 기준을 뒤로 민다)
    returning = FOCUS_RETURN_DAYS <= since <= FOCUS_RETURN_MAX_DAYS
    shift = since if returning else 0.0
    if since > FOCUS_RETURN_MAX_DAYS:
        return Profile(kind=k, coached_runs=sum(1 for r in history if r.coached))
    recent = [r for r in same if r.days_ago - shift <= PROFILE_DAYS and (r.meters >= 1000 or r.seconds >= 480)]
    prof = Profile(kind=k, coached_runs=sum(1 for r in history if r.coached))
    in30 = [r.meters for r in same if r.days_ago <= 30]
    prof.longest30_m = max(in30) if in30 else None
    if len(recent) >= PROFILE_MIN_RUNS:
        prof.tier = 1
        w = [(r, 0.5 ** ((r.days_ago - shift) / PROFILE_HALF_LIFE)) for r in recent]
        prof.usual_pace = _wmedian([(r.seconds / (r.meters / 1000.0), x) for r, x in w])
        prof.usual_cadence = _wmedian([(r.steps * 60.0 / r.seconds, x) for r, x in w])
        prof.usual_m = statistics.median(r.meters for r in recent[:5])
    if prof.tier < 1:
        return prof
    if k == "RUN":
        laps = [x for r in same if r.days_ago <= BEST_1K_DAYS for x in r.laps if x >= LAP_MIN_S]
        prof.best_1k = float(min(laps)) if laps else None

    # 오늘의 한 가지 — 위에서부터 처음 맞는 것. 최근 두 번 내리 해낸 것은 건너뛴다
    done = [r.focus for r in runs[:2] if r.coached and r.focus_done]
    mastered = done[0] if len(done) == 2 and done[0] == done[1] else None
    last7 = sum(r.meters for r in same if r.days_ago <= 7)
    before = [r for r in same if 7 < r.days_ago <= 28]
    # 앞 3주 기록이 셋은 있어야 "평소보다 많이"를 말할 수 있다(막 시작한 사람을 과하게 달렸다고 하지 않게)
    prev21 = sum(r.meters for r in before) / 3.0 if len(before) >= 3 else 0.0
    hard2 = len(runs) >= 2 and all(r.rpe == "HARD" for r in runs[:2])
    loaded = prev21 > 0 and last7 > prev21 * EASY_DAY_LOAD
    with_laps = [r for r in same if len(r.laps) >= 3][:3]
    fast_start = sum(1 for r in with_laps
                     if r.laps[0] <= (sum(r.laps[1:]) / len(r.laps[1:])) * (1 - FOCUS_START_FAST))
    long_laps = [r for r in same if len(r.laps) >= 4][:3]

    def spread(r: PastRun) -> float:
        rest = r.laps[1:]
        return (max(rest) - min(rest)) / (sum(rest) / len(rest))

    uneven = sum(1 for r in long_laps if spread(r) >= FOCUS_UNEVEN)
    last3 = same[:3]
    steady_dist = (len(last3) == 3 and prof.usual_m and prof.usual_m >= LONG_RUN_MIN_M
                   and all(abs(r.meters - prof.usual_m) <= prof.usual_m * FOCUS_FARTHER_BAND for r in last3)
                   and all(r.rpe != "HARD" for r in last3) and since <= FOCUS_FARTHER_DAYS)
    for name, ok in (
        ("RETURN", returning),
        ("EASY_DAY", hard2 or loaded),
        ("START_EASY", k == "RUN" and len(with_laps) >= 2 and fast_start >= 2),
        ("EVEN", k == "RUN" and len(long_laps) >= 2 and uneven >= 2),
        ("FARTHER", bool(steady_dist)),
    ):
        if ok and name != mastered:
            prof.focus = name
            break
    if prof.focus == "EASY_DAY":
        prof.focus_why = "HARD" if hard2 else "LOAD"
    if prof.focus == "RETURN" and prof.usual_m:
        prof.focus_goal_m = prof.usual_m * FOCUS_RETURN_SHARE
    if prof.focus == "FARTHER" and prof.usual_m:
        goal = prof.usual_m + max(FOCUS_FARTHER_M, prof.usual_m * FOCUS_FARTHER_BAND)
        if prof.longest30_m:
            goal = min(goal, prof.longest30_m * LONG_RUN[0])
        prof.focus_goal_m = goal if goal >= prof.usual_m + 200 else None
        if prof.focus_goal_m is None:
            prof.focus = None
    return prof


# ────────────────────────────────────────────────────────────────────────────
# 규칙 엔진
#
# 엔진은 "지금 말할 만한 것"을 내놓을 뿐이고, 언제 말할지는 조정자가 정한다.
#   - 한 번뿐인 일(출발 · 구간 · 일시정지 · 목표 달성)은 step() 이 돌려준다 — 차례가 안 오면 버려진다.
#   - 이어지는 상태(빠르다 · 처진다 · 걷는다 …)는 live 에 올려 둔다 — 상태가 풀리면 내려가고,
#     조정자가 실제로 말했을 때만 spoken() 으로 횟수 · 간격을 센다(말하지 못한 조언이 센 것으로 치이지 않게).
# ────────────────────────────────────────────────────────────────────────────

ITEM_SPEECH = {"RUN_BOOSTER": "러닝 부스터", "REWARD_DOUBLE": "보상 두 배권", "ENERGY_DRINK": "에너지 드링크"}
ADVICE_IDS = ("PACE_FAST", "PACE_SLOW", "PACE_BACK", "START_FAST", "FADE", "WALK_OK", "BRISK", "BRISK_OK",
              "TALK_TEST", "FORM", "HYDRATE", "FOCUS_USUAL", "FOCUS_GOAL", "FOCUS_FULL")


class Ghost:
    """목표 속도로 달리는 가상의 러너와 벌어진 거리(CUSUM).

    순간 페이스는 GPS 오차에 흔들리지만, "목표보다 몇 미터 앞섰나"는 양 끝 두 점의 오차만 받는다 —
    오래 벗어날수록 확실해지고, 크게 벗어날수록 빨리 잡힌다. 작은 차이(slack 안쪽)는 쌓이지 않는다.
    """

    def __init__(self, slack: float):
        self.slack = slack
        self.fast = 0.0
        self.slow = 0.0
        self.fast_from: Optional[Tuple[int, float]] = None   # 쌓이기 시작한 점(시각, 거리)
        self.slow_from: Optional[Tuple[int, float]] = None
        self.fast_acc = 0.0     # 쌓이는 동안 가장 흐렸던 GPS 오차 — 문턱은 이것으로 잡는다
        self.slow_acc = 0.0     # (흐린 곳에서 쌓인 거리가, 맑은 곳의 낮은 문턱을 넘어 버리지 않게)

    def reset(self) -> None:
        self.fast = 0.0
        self.slow = 0.0
        self.fast_from = None
        self.slow_from = None
        self.fast_acc = 0.0
        self.slow_acc = 0.0

    def add(self, t0: int, d0: float, t1: int, d1: float, target_pace: float, acc: float,
            extra_fast: float = 0.0) -> None:
        v = 1000.0 / target_pace
        dd, dt = d1 - d0, t1 - t0
        if self.fast <= 0.0:
            self.fast_from = (t0, d0)
            self.fast_acc = 0.0
        if self.slow <= 0.0:
            self.slow_from = (t0, d0)
            self.slow_acc = 0.0
        self.fast_acc = max(self.fast_acc, acc)
        self.slow_acc = max(self.slow_acc, acc)
        self.fast = max(0.0, self.fast + dd - v * (1 + self.slack + extra_fast) * dt)
        self.slow = max(0.0, self.slow + v * (1 - self.slack) * dt - dd)

    def span_pace(self, direction: str, t1: int, d1: float) -> Optional[float]:
        start = self.fast_from if direction == "FAST" else self.slow_from
        if start is None or d1 - start[1] < DELTA_MIN_M or t1 <= start[0]:
            return None
        return (t1 - start[0]) / (d1 - start[1]) * 1000.0


class Engine:
    def __init__(self, profile: Profile, plan: Plan, restored: bool = False):
        self.p = profile
        self.plan = plan
        self.restored = restored
        self.out: List[Cue] = []
        self.live: Dict[str, Cue] = {}
        self.live_soft = False
        self.now: Optional[Tick] = None
        self.quiet_until = -1                   # 이 시각(wall)까지는 급한 말 · 마디만
        # 걸음 · 상태
        self.step_buf: Deque[Tuple[float, int]] = deque()
        self.cad_hist: Deque[float] = deque(maxlen=90)
        self.mode = "RUN" if profile.kind == "RUN" else "WALK"
        usual = profile.usual_cadence if profile.kind == "RUN" else None
        # 평소 케이던스가 낮은 러너(느린 조깅)는 달리기 문턱을 그 사람에게 맞춘다
        self.run_cad = min(RUN_CAD, max(MODE_CAD_LOW, usual - RUN_CAD_MARGIN)) if usual else RUN_CAD
        self.walk_cad = self.run_cad - (RUN_CAD - WALK_CAD)
        self.mode_want = self.mode
        self.mode_since = 0
        self.run_s = 0
        self.walk_s = 0
        self.bout_run_s = 0
        self.bout_walk_s = 0
        self.bout_counted = False
        self.walk_bouts = 0
        self.run_cads: List[float] = []
        # GPS
        self.gps: Deque[Tuple[int, float]] = deque()
        self.last_gps_m = 0.0
        self.gps_bad_s = 0
        self.gps_gap_said = False
        self.gps_weak_n = 0
        self.settle_until = PACE_SETTLE_S
        # 목표 · 평소 페이스
        self.ghost = Ghost(GHOST_SLACK)
        self.guard = Ghost(GHOST_SLACK if profile.focus in ("EASY_DAY", "RETURN") else GHOST_SLACK_USUAL)
        self.exc_dir: Optional[str] = None
        self.exc_cues = 0
        self.exc_last = -10 ** 9
        self.exc_clear_since: Optional[int] = None
        self.muted: Dict[str, bool] = {}        # 그 방향은 이번 러닝에서 더 말하지 않는다
        self.dir_cues: Dict[str, int] = {"FAST": 0, "SLOW": 0}
        self.back_since: Optional[int] = None
        self.back_until = -1
        self.cue_at_m = 0.0                     # "돌아왔다"를 재기 시작하는 거리(마지막 조언 · 마지막으로 달리지 않은 곳)
        # 처짐
        self.blocks: List[Tuple[float, float, bool]] = []   # (페이스, 케이던스, 내내 달렸나)
        self.rblocks: List[Tuple[float, float]] = []        # 달린 구간만
        self.regime_from = 0                    # 일부러 속도를 바꾼 뒤의 첫 구간
        self.block_start: Optional[Tuple[int, float]] = None
        self.block_mode_s = [0, 0]
        self.fade_n = 0
        self.fade_last = -10 ** 9
        self.fade_ref: Optional[float] = None
        self.fade_on = False
        # 안내
        self.laps_said = 0
        self.lap_off_s = 0
        self.lap_dirty_prev = True
        self.lap_clean: List[bool] = []
        self.said: Dict[str, int] = {}
        self.was_paused = False
        self.still_n = 0
        self.still_on = False
        self.over_on = False
        self.loc_off = False
        self.brisk_low_s = 0
        self.brisk_ok_s = 0
        self.brisk_last = -10 ** 9
        self.goal_done = False
        self.started = False
        self.until: Dict[str, int] = {}
        self.last_advice_at = -10 ** 9          # 조언 · 격려를 마지막으로 말한 때(자세 한마디가 본다)
        self.form_n = 0
        self.form_last = -10 ** 9
        self.hydrate_next = HYDRATE_S[0]
        self.facts: Dict[str, object] = {}      # 러닝 뒤 코치 한마디에 쓸 사실

    # ── 작은 도구 ──
    def once(self, key: str) -> bool:
        if key in self.said:
            return False
        self.said[key] = 1
        return True

    def hold(self, cue: Cue) -> None:
        old = self.live.get(cue.id)
        cue.born = old.born if old else self.now.wall
        self.live[cue.id] = cue

    def drop(self, *ids: str) -> None:
        for i in ids:
            self.live.pop(i, None)

    def _timed(self, key: str, seconds: int) -> bool:
        """key 를 처음 볼 때부터 seconds 동안만 참 — 그 안에 말하지 못한 안내는 접는다."""
        t = self.now.wall
        if key not in self.until:
            self.until[key] = t + seconds
        return t <= self.until[key]

    @property
    def walker(self) -> bool:
        """이번 러닝을 걷는 사람으로 대할지 — 평소 걷는 사람이거나, 4분 넘게 거의 걷기만 했다."""
        return self.p.kind == "WALK" or (self.now is not None and self.now.active >= WALKER_AFTER_S
                                         and self.walk_s > self.run_s * WALKER_RATIO)

    def cadence(self, window: int = CAD_WINDOW_S) -> Optional[float]:
        """분당 걸음 — 걸음 센서가 알려 준 시각 사이로 잰다(뭉쳐 와도 흔들리지 않는다)."""
        if self.now is None:
            return None
        now = self.now.active
        if len(self.step_buf) < 2:
            return 0.0 if now >= window else None       # 걸음이 아예 없다 — 서 있다
        t1, s1 = self.step_buf[-1]
        t0, s0 = self.step_buf[0]
        for t, n in self.step_buf:
            if now - t <= window:
                break
            t0, s0 = t, n
        # 걸음 소식이 한동안 없다 — GPS 로는 움직이고 있으면 소식이 늦는 것이니 마지막 값을 그대로 둔다
        stale = now - t1 > STEP_STALE_S and not self._moving()
        end = float(now) if stale else t1
        if end - t0 < window * 0.5:
            return self.cad_hist[-1] if (self.cad_hist and not stale) else None
        return (s1 - s0) * 60.0 / (end - t0)

    def _moving(self) -> bool:
        speed = self.recent_speed()
        return speed is not None and speed >= MOVING_MPS

    def cadence_med(self, seconds: int = 60) -> Optional[float]:
        vals = list(self.cad_hist)[-seconds:]
        return statistics.median(vals) if len(vals) >= 20 else None

    def run_cadence_now(self) -> Optional[float]:
        """달리는 동안만 센 최근 1분 케이던스."""
        vals = self.run_cads[-60:]
        return statistics.median(vals) if len(vals) >= 30 else None

    def run_cadence(self) -> Optional[float]:
        """이번 러닝의 달리기 케이던스(2분 넘게 달린 뒤) — 그 전에는 평소 값."""
        if len(self.run_cads) >= 120:
            return statistics.median(self.run_cads)
        return self.p.usual_cadence

    def recent_pace(self, meters: float, after_m: float = 0.0) -> Optional[float]:
        """최근 [meters] 의 페이스(초/km) — 거리 점의 시각으로 잰다. [after_m] 뒤의 점만 쓴다."""
        if len(self.gps) < 2:
            return None
        t1, d1 = self.gps[-1]
        for t0, d0 in reversed(self.gps):
            if d0 < after_m:
                return None
            if d1 - d0 >= meters:
                return (t1 - t0) / (d1 - d0) * 1000.0 if t1 > t0 else None
        return None

    def recent_speed(self) -> Optional[float]:
        """최근 20초의 속도(m/s) — 달리기 · 걷기를 가를 때 케이던스와 함께 본다."""
        if len(self.gps) < 2 or self.now.active - self.gps[-1][0] > SPEED_FRESH_S or not self.now.gps_ok:
            return None
        t1, d1 = self.gps[-1]
        for t0, d0 in reversed(self.gps):
            if t1 - t0 >= SPEED_WINDOW_S:
                return (d1 - d0) / (t1 - t0)
        return None

    def _lap_pace(self, laps: List[Tuple[int, float, int]], i: int) -> float:
        a = laps[i]
        b = laps[i - 1] if i - 1 >= 0 else (0, 0.0, 0)
        return (a[2] - b[2]) / (a[1] - b[1])

    # ── 1초마다 ──
    def step(self, t: Tick) -> List[Cue]:
        self.out = []
        self.now = t
        if not self.started:
            self.started = True
            if self.restored:
                self._restore(t)
            else:
                self._start(t)
        if self._pause_edges(t):
            self.live.clear()
            return self.out
        self._steps(t)
        self._gps(t)
        self._system(t)
        self._splits(t)
        self._goal(t)
        if self.goal_done:
            self.drop(*ADVICE_IDS)      # 목표를 이룬 뒤에는 구간 · 경고만(마무리 조깅을 "처진다"고 하지 않는다)
        else:
            self._pace(t)
            self._fade(t)
            self._walk(t)
            self._talk_test(t)
            self._focus(t)
            self._detailed(t)
        self._long_run(t)
        return self.out

    def emit(self, cue: Cue) -> None:
        cue.born = self.now.wall
        self.out.append(cue)

    def tail_used(self, key: Optional[str]) -> None:
        """구간 안내가 제 꼬리를 실제로 말했다."""
        if key:
            self.said[key] = 1

    def spoken(self, cue_id: str) -> None:
        """조정자가 실제로 말했다 — 횟수 · 간격은 여기서만 센다."""
        t = self.now
        cue = self.live.pop(cue_id, None)
        if cue is not None and cue.prio >= 3:
            self.last_advice_at = t.active
        self.facts[cue_id] = int(self.facts.get(cue_id, 0)) + 1
        if cue_id == "FORM":
            self.form_n += 1
            self.form_last = t.active
        elif cue_id == "HYDRATE":
            self.hydrate_next = t.active + HYDRATE_S[1]
        elif cue_id in ("PACE_FAST", "PACE_SLOW"):
            self.exc_cues += 1
            self.exc_last = t.active
            self.cue_at_m = t.gps_m
            self.back_since = None
            direction = "FAST" if cue_id == "PACE_FAST" else "SLOW"
            self.dir_cues[direction] += 1
            if self.exc_cues >= PACE_CUES_PER_EXCURSION or self.dir_cues[direction] >= PACE_CUES_PER_RUN:
                self.muted[direction] = True
            if cue_id == "PACE_SLOW" and self.live_soft:
                self.muted["SLOW"] = True
            # 다시 쌓여야 다음 말을 한다
            if cue_id == "PACE_FAST":
                self.ghost.fast = 0.0
            else:
                self.ghost.slow = 0.0
        elif cue_id == "FADE":
            self.fade_n += 1
            self.fade_last = t.active
            last = self.rblocks[-3:]
            self.fade_ref = sum(b[0] for b in last) / max(1, len(last))
            self.fade_on = False
        elif cue_id == "BRISK":
            self.said["BRISK"] = self.said.get("BRISK", 0) + 1
            self.brisk_last = t.active
            self.brisk_low_s = 0
        else:
            self.said[cue_id] = 1

    # ── 시작 · 되살아남 · 멈춤 ──
    def _start(self, t: Tick) -> None:
        plan = self.plan
        text = "걷기를 시작해요." if self.p.kind == "WALK" else "러닝을 시작해요."
        if plan.goal_m and plan.target_pace:
            text += f" 목표 {km_text(plan.goal_m)}, 킬로미터당 {pace_text(plan.target_pace)}."
        elif plan.goal_m:
            text += f" 목표 {km_text(plan.goal_m)}."
        elif plan.target_pace:
            text += f" 목표 페이스 {pace_text(plan.target_pace)}."
        elif plan.level != "BRIEF":
            focus = self.p.focus
            if focus == "RETURN":
                text += " 오랜만이에요. 오늘은 평소보다 짧고 편하게 가요."
            elif focus == "EASY_DAY" and self.p.focus_why == "HARD":
                text += " 지난 러닝이 힘들었죠. 오늘은 평소보다 천천히 가요."
            elif focus == "EASY_DAY":
                text += " 요즘 많이 달렸어요. 오늘은 평소보다 천천히 가요."
            elif focus == "START_EASY":
                text += " 오늘은 첫 1킬로미터를 천천히 가 봐요."
            elif focus == "EVEN":
                text += " 오늘은 처음부터 끝까지 같은 페이스로 가 봐요."
            elif focus == "FARTHER" and self.p.focus_goal_m:
                text += f" 오늘은 조금 더, {km_text(self.p.focus_goal_m)}까지 가 봐요."
        self.emit(Cue("RUN_START", 1, text))

    def _start_items(self) -> None:
        """출발 때 쓴 아이템 · 보상 이벤트 — 사실만, 금액 · 배수는 말하지 않는다."""
        names = [ITEM_SPEECH[i] for i in self.plan.items if i in ITEM_SPEECH]
        parts = []
        if names:
            head = [with_particle(n, "과", "와") for n in names[:-1]]
            parts.append(" ".join(head + [with_particle(names[-1], "을", "를")]) + " 썼어요.")
        if self.plan.event:
            parts.append("지금 보상 이벤트 중이에요.")
        if parts and "START_ITEMS" not in self.said and self._timed("START_ITEMS", HOLD_S["START_ITEMS"]):
            self.hold(Cue("START_ITEMS", 2, " ".join(parts)))
        else:
            self.drop("START_ITEMS")

    def _restore(self, t: Tick) -> None:
        """앱이 죽었다 러닝이 되살아났다 — 지나간 것을 다시 말하지 않고, 한동안 조용히 있다가 이어 간다."""
        self.quiet_until = t.wall + RESTORE_QUIET_S
        self.settle_until = t.active + GPS_BACK_SETTLE_S
        self.laps_said = len(t.laps)
        self.lap_clean = [False] * len(t.laps)      # 앞 구간이 깨끗했는지 모른다 — 견주지 않는다
        self.last_gps_m = t.gps_m
        passed = ["START_ITEMS"]
        g = self.plan.goal_m
        if g:
            self.goal_done = t.live_m >= g
            passed += [k for k, ok in (("HALFWAY", t.live_m >= g / 2), ("LAST_KM", g - t.live_m <= 1000),
                                       ("LAST_400", g - t.live_m <= 400)) if ok]
        longest = self.p.longest30_m
        if longest:
            passed += [k for k, ok in (("LONG_110", t.live_m >= longest * LONG_RUN[0]),
                                       ("LONG_200", t.live_m >= longest * LONG_RUN[1])) if ok]
        if t.shoe_left_s is not None:
            passed += [k for k, ok in (("SHOE_LOW", t.shoe_left_s <= SHOE_LOW_S), ("SHOE_WORN", t.shoe_left_s <= 0)) if ok]
        if t.crew_pool_left is not None:
            passed += [k for k, ok in (("CREW_LOW", t.crew_pool_left <= CREW_LOW), ("CREW_EMPTY", t.crew_pool_left <= 0)) if ok]
        if self.p.usual_m and t.live_m >= self.p.usual_m:
            passed += ["FOCUS_USUAL", "FOCUS_FULL"]
        if self.p.focus_goal_m and t.live_m >= self.p.focus_goal_m:
            passed.append("FOCUS_GOAL")
        if t.active >= TALK_TEST_S[0]:
            passed.append("TALK_TEST")
        if t.active >= START_GUARD_FROM_S:
            passed.append("START_FAST")             # 출발은 이미 지났다
        for k in passed:
            self.said[k] = 1
        while self.hydrate_next <= t.active:
            self.hydrate_next += HYDRATE_S[1]

    def _pause_edges(self, t: Tick) -> bool:
        if t.paused and not self.was_paused:
            self.was_paused = True
            self.emit(Cue("PAUSED", 1, "일시정지."))
        elif not t.paused and self.was_paused:
            self.was_paused = False
            self.settle_until = t.active + RESUME_SETTLE_S
            self.ghost.reset()
            self.guard.reset()
            self.cue_at_m = t.gps_m
            self.step_buf.clear()
            self.cad_hist.clear()
            self.block_start = None
            self.lap_off_s += LAP_DIRTY_S
            self.emit(Cue("RESUMED", 1, "다시 달려요." if not self.walker else "다시 출발해요."))
        return t.paused

    # ── 걸음 · 달리기/걷기 ──
    def _steps(self, t: Tick) -> None:
        if not self.step_buf or t.steps > self.step_buf[-1][1]:
            self.step_buf.append((t.steps_at, t.steps))
        while len(self.step_buf) > 2 and t.active - self.step_buf[1][0] >= CAD_WINDOW_S:
            self.step_buf.popleft()
        cad = self.cadence()
        c5 = self.cadence(MODE_WINDOW_S)
        if cad is None or c5 is None:
            return
        self.cad_hist.append(cad)
        speed = self.recent_speed()
        fast_feet = speed is not None and speed >= RUN_SPEED_MPS     # 걷기로는 나오지 않는 속도
        want = self.mode
        if c5 >= self.run_cad or (c5 >= MODE_CAD_LOW and fast_feet):
            want = "RUN"
        elif c5 < STAND_CAD:
            want = "STAND"
        elif c5 < self.walk_cad and not fast_feet:
            want = "WALK"
        if want != self.mode_want:
            self.mode_want = want
            self.mode_since = t.active
        if want != self.mode and t.active - self.mode_since >= MODE_DWELL_S:
            prev, self.mode = self.mode, want
            if self.mode == "WALK":
                self.bout_walk_s = 0
                self.bout_counted = False
            if self.mode == "RUN":
                self.bout_run_s = 0
                if prev != "RUN":
                    # 걷다 · 섰다 다시 달린다 — 속도가 붙는 동안은 페이스를 따지지 않는다
                    self.settle_until = max(self.settle_until, t.active + RESUME_SETTLE_S)
                    self.ghost.reset()
                    self.guard.reset()
                    self.cue_at_m = t.gps_m
        if self.mode == "RUN":
            self.run_s += 1
            self.bout_run_s += 1
            self.run_cads.append(cad)
        elif self.mode == "WALK":
            self.walk_s += 1
            if WALK_BOUT_CAD <= c5 < self.walk_cad:
                self.bout_walk_s += 1
            elif c5 >= self.run_cad or c5 < WALK_BOUT_CAD:
                self.bout_walk_s = 0
        main = "WALK" if self.walker else "RUN"
        if self.mode != main or not t.gps_ok:
            self.lap_off_s += 1

    # ── GPS 거리 점 ──
    def _gps(self, t: Tick) -> None:
        if not t.gps_ok:
            self.gps_bad_s += 1
            if (self.gps_bad_s >= GPS_WEAK_S and not self.gps_gap_said and self.gps_weak_n < GPS_WEAK_MAX
                    and t.active > 30 and self.mode != "STAND" and t.location_on):
                self.gps_gap_said = True
                self.gps_weak_n += 1
                self.emit(Cue("GPS_WEAK", 1, "GPS 신호가 약해요. 거리와 페이스 안내를 잠시 쉴게요."))
        else:
            if self.gps_bad_s >= GPS_GAP_RESET_S:
                # 끊겼다 돌아왔다 — 페이스 판단은 새로 시작한다
                self.settle_until = max(self.settle_until, t.active + GPS_BACK_SETTLE_S)
                self.ghost.reset()
                self.guard.reset()
                self.gps.clear()
                self.block_start = None
                self.cue_at_m = t.gps_m
            self.gps_bad_s = 0
            self.gps_gap_said = False
        if t.gps_m > self.last_gps_m + 0.01:
            prev = self.gps[-1] if self.gps else None
            self.gps.append((t.active, t.gps_m))
            self.last_gps_m = t.gps_m
            while self.gps and t.gps_m - self.gps[0][1] > 1200.0:
                self.gps.popleft()
            if prev is not None:
                usable = (t.gps_ok and t.acc_m <= PACE_MAX_ACC_M and t.gps_m >= GPS_MIN_M
                          and t.active >= self.settle_until
                          and t.active - prev[0] <= GHOST_GAP_S and self.mode == "RUN")
                if usable:
                    # GPS 가 흐리면 거리가 길게 재져 빨라 보인다 — 그만큼 '빠르다' 쪽 여유를 넓힌다
                    extra = GHOST_BIAS_PER_M * max(0.0, t.acc_m - GHOST_BIAS_FREE_M)
                    if self.plan.target_pace:
                        self.ghost.add(prev[0], prev[1], t.active, t.gps_m, self.plan.target_pace, t.acc_m, extra)
                    if self.p.usual_pace and self.p.kind == "RUN":
                        self.guard.add(prev[0], prev[1], t.active, t.gps_m, self.p.usual_pace, t.acc_m, extra)
                else:
                    self.ghost.reset()
                    self.guard.reset()
                    self.cue_at_m = t.gps_m     # "돌아왔다"는 내내 달린 거리로만 잰다
            self._block(t)

    def _block(self, t: Tick) -> None:
        if self.block_start is None:
            self.block_start = (t.active, t.gps_m)
            self.block_mode_s = [0, 0]
            return
        t0, d0 = self.block_start
        if t.gps_m - d0 < BLOCK_M:
            return
        pace = (t.active - t0) / (t.gps_m - d0) * 1000.0
        run_s, all_s = self.block_mode_s
        cad = self.run_cadence_now() or 0.0
        ran = all_s > 0 and run_s / all_s >= BLOCK_RUN_SHARE
        self.blocks.append((pace, cad, ran))
        self.block_start = (t.active, t.gps_m)
        self.block_mode_s = [0, 0]
        if ran:
            self.rblocks.append((pace, cad))
            rb = self.rblocks
            if len(rb) >= 5:
                # 두 구간이 내리 앞 세 구간 평균과 크게 다르다 — 일부러 속도를 바꿨다(마무리 조깅 · 빠른 구간)
                prior = sum(b[0] for b in rb[-5:-2]) / 3
                a, b = rb[-2][0] / prior - 1.0, rb[-1][0] / prior - 1.0
                if (b >= FADE_STEP and a >= FADE_STEP / 2) or (b <= -FADE_STEP and a <= -FADE_STEP / 2):
                    self.regime_from = len(rb) - 2
                    self.fade_ref = None
        self._fade_check(t)

    # ── 앱이 이미 하던 경고를 말로 ──
    def _system(self, t: Tick) -> None:
        if self.block_start is not None:
            self.block_mode_s[1] += 1
            if self.mode == "RUN":
                self.block_mode_s[0] += 1
        if self.plan.items or self.plan.event:
            self._start_items()
        if t.overspeed_stopped:
            if self.once("OVERSPEED_STOP"):
                self.emit(Cue("OVERSPEED_STOP", 0, "과속으로 러닝이 멈췄어요.", interrupt=True))
        elif t.overspeed_left is not None and not self.over_on:
            self.over_on = True
            self.emit(Cue("OVERSPEED_WARN", 0, f"속도가 너무 빨라요. 이대로면 {OVERSPEED_STOP_S}초 안에 러닝이 멈춰요.",
                          interrupt=True))
        elif t.overspeed_left is None:
            self.over_on = False
        if not t.location_on and not self.loc_off:
            self.loc_off = True
            if self.said.get("LOCATION_OFF", 0) < 2:
                self.said["LOCATION_OFF"] = self.said.get("LOCATION_OFF", 0) + 1
                self.emit(Cue("LOCATION_OFF", 0, "위치가 꺼졌어요. 거리 기록이 멈춰요."))
        elif t.location_on:
            self.loc_off = False
        if t.still and not self.still_on:
            self.still_on = True
            self.still_n += 1
            if self.still_n == 1:   # 두 번째부터는 진동 · 화면 띠만(신호등마다 말하지 않는다)
                self.emit(Cue("STILL", 1, "30초 넘게 멈춰 있어요. 쉬는 거라면 일시정지를 눌러 주세요."))
        elif not t.still:
            self.still_on = False
        if t.shoe_left_s is not None:
            if t.shoe_left_s <= 0:
                self.drop("SHOE_LOW")
                if self.once("SHOE_WORN"):
                    self.emit(Cue("SHOE_WORN", 0, "신발 내구도를 다 썼어요. 지금부터는 적립되지 않고, 기록은 계속돼요."))
            elif t.shoe_left_s <= SHOE_LOW_S and "SHOE_LOW" not in self.said:
                if t.shoe_left_s >= SHOE_NEAR_S:
                    text = f"신발 내구도가 {int(round(t.shoe_left_s / 60.0))}분쯤 남았어요."
                else:
                    text = "신발 내구도가 거의 다 닳았어요."
                self.hold(Cue("SHOE_LOW", 2, text))
        if t.crew_pool_left is not None:
            if t.crew_pool_left <= 0:
                self.drop("CREW_LOW")
                if self.once("CREW_EMPTY"):
                    self.emit(Cue("CREW_EMPTY", 0, "크루 공용 체력을 다 썼어요. 적립은 멈추고 기록은 계속돼요."))
            elif t.crew_pool_left <= CREW_LOW and "CREW_LOW" not in self.said:
                pct = max(5, int(5 * round(t.crew_pool_left * 100 / 5.0)))
                self.hold(Cue("CREW_LOW", 2, f"크루 공용 체력이 {pct}퍼센트 남았어요."))

    # ── 구간 안내 ──
    def _splits(self, t: Tick) -> None:
        if len(t.laps) <= self.laps_said:
            return
        dirty = self.lap_off_s >= LAP_DIRTY_S
        while len(self.lap_clean) < len(t.laps):
            self.lap_clean.append(not dirty)
        dirty_prev = self.lap_dirty_prev
        self.lap_dirty_prev = dirty
        self.lap_off_s = 0
        self.laps_said = len(t.laps)
        # 구간은 GPS 거리로 잴 때만 말한다(GPS 100m 전에는 걸음 어림 — 자유 러닝 화면에는 그 거리가 보이지도 않는다)
        if self.plan.split == "OFF" or t.gps_m < GPS_MIN_M:
            return
        n, cum_km, cum_s = t.laps[-1]
        idx = len(t.laps) - 1
        prev_km = t.laps[-2][1] if idx >= 1 else 0.0
        if cum_km <= prev_km:
            return
        g = self.plan.goal_m
        if g and abs(cum_km * 1000.0 - g) <= GOAL_SPLIT_NEAR_M:
            return                              # 목표 달성 말이 시간까지 말한다
        lap = self._lap_pace(t.laps, idx)
        if self.plan.level == "BRIEF":
            text = f"1킬로미터. {pace_text(lap)}." if n == 1 else f"{n}킬로미터. 이번 구간 {pace_text(lap)}."
            self.emit(Cue("SPLIT", 2, text))
            return
        base = (f"1킬로미터. {pace_text(lap)}." if n == 1
                else f"{n}킬로미터. {clock_text(cum_s)}. 이번 구간 {pace_text(lap)}.")
        full = base
        if self.plan.level == "DETAILED" and n >= 2:
            full += f" 평균 페이스 {pace_text(cum_s / cum_km)}."
        tail, key = (None, None) if self.goal_done else self._split_tail(t, n, lap, cum_km, cum_s, dirty, dirty_prev)
        self.emit(Cue("SPLIT", 2, full, tail=tail, short=base, tail_key=key))

    def _split_tail(self, t: Tick, n: int, lap: float, cum_km: float, cum_s: int,
                    dirty: bool, dirty_prev: bool) -> Tuple[Optional[str], Optional[str]]:
        plan = self.plan
        idx = len(t.laps) - 1
        # 1) 목표가 있으면 목표와의 차이, 절반 뒤에는 예상 도착
        if plan.target_pace:
            if plan.goal_m and plan.goal_m * 0.5 <= cum_km * 1000 < plan.goal_m - 500:
                eta = cum_s + (plan.goal_m / 1000 - cum_km) * (cum_s / cum_km)
                return f"이대로면 {clock_text(eta)}에 도착해요.", None
            if dirty:
                return None, None
            d = lap - plan.target_pace
            if abs(d) <= plan.target_pace * GHOST_SLACK:
                return "목표 페이스예요.", None
            return f"목표보다 {delta_text(d)} {'느려요' if d > 0 else '빨라요'}.", None
        if dirty or (self.walker and self.p.kind != "WALK"):
            return None, None       # 걷기 · 신호 대기 · GPS 끊김이 섞인 구간은 견주지 않는다
        # 2) 최근 가장 빠른 1km(평소 값이 있는 사람만)
        if (self.p.tier >= 1 and self.p.best_1k and lap <= self.p.best_1k * BEST_1K_MARGIN
                and self.mode == "RUN" and "BEST_1K" not in self.said):
            return "최근 석 달 중 가장 빠른 1킬로미터예요.", "BEST_1K"
        # 3) 첫 구간: 평소와 견줌(오늘의 한 가지가 출발이면 해냈는지)
        if n == 1 and self.p.usual_pace:
            d = lap - self.p.usual_pace
            if self.p.focus in ("START_EASY", "EASY_DAY", "RETURN"):
                calm = d >= -self.p.usual_pace * FOCUS_CALM
                self.facts["focus_done"] = calm
                if calm:
                    return "좋아요, 차분하게 출발했어요.", None
                return f"평소보다 {delta_text(d)} 빨랐어요. 여기부터 조금만 늦춰요.", None
            if abs(d) >= self.p.usual_pace * USUAL_COMPARE:
                return f"평소보다 {delta_text(d)} {'느린' if d > 0 else '빠른'} 페이스예요.", None
            return "평소 페이스예요.", None
        # 3-1) 오늘의 한 가지가 고른 페이스면: 지금까지의 깨끗한 구간과 견줌
        if self.p.focus == "EVEN" and idx >= 1:
            before = [self._lap_pace(t.laps, i) for i in range(idx) if self.lap_clean[i]]
            if before:
                ref = statistics.median(before)
                d = lap - ref
                even = abs(d) <= ref * FOCUS_EVEN_BAND
                self.facts["even_ok"] = int(self.facts.get("even_ok", 0)) + (1 if even else 0)
                self.facts["even_n"] = int(self.facts.get("even_n", 0)) + 1
                if even:
                    return "지금까지와 같은 페이스예요. 좋아요.", None
                return f"지금까지보다 {delta_text(d)} {'느려요' if d > 0 else '빨라요'}.", None
        # 4) 고르게 가는 중(러닝마다 한 번)
        if idx >= 2 and "STEADY" not in self.said and not dirty_prev and self.lap_clean[idx - 2]:
            last3 = [self._lap_pace(t.laps, i) for i in (idx - 2, idx - 1, idx)]
            mean = sum(last3) / 3
            if all(abs(x - mean) <= mean * STEADY_BAND for x in last3):
                return "페이스가 고르게 유지되고 있어요.", "STEADY"
        # 5) 앞 구간과 견줌(둘 다 깨끗한 구간일 때만)
        if idx >= 1 and not dirty_prev:
            prev = self._lap_pace(t.laps, idx - 1)
            d = lap - prev
            if abs(d) >= prev * LAP_COMPARE:
                return f"지난 구간보다 {delta_text(d)} {'느려졌어요' if d > 0 else '빨라졌어요'}.", None
        return None, None

    # ── 목표 거리 ──
    def _goal(self, t: Tick) -> None:
        g = self.plan.goal_m
        if not g or self.goal_done:
            return
        left = g - t.live_m
        if left <= 0:
            self.goal_done = True
            self.facts["goal_s"] = t.active
            self.drop("HALFWAY", "LAST_KM", "LAST_400")
            rest = "잘했어요." if self.walker else "잘했어요. 천천히 걸으며 숨을 골라요."
            self.emit(Cue("GOAL_REACHED", 1, f"목표 {km_text(g)} 달성! {clock_text(t.active)}. {rest}"))
            return
        if g >= 2000 and t.live_m >= g / 2 and "HALFWAY" not in self.said and self._timed("HALFWAY", HOLD_S["HALFWAY"]):
            self.hold(Cue("HALFWAY", 2, "절반 왔어요.", tail="절반 왔어요."))
        else:
            self.drop("HALFWAY")
        if g >= 3000 and left <= 1000 and "LAST_KM" not in self.said and self._timed("LAST_KM", HOLD_S["LAST_KM"]):
            self.hold(Cue("LAST_KM", 2, "마지막 1킬로미터예요.", tail="마지막 1킬로미터예요."))
        else:
            self.drop("LAST_KM")
        if (g >= 3000 and left <= 400 and self.plan.style == "CHALLENGE" and "LAST_400" not in self.said
                and self._timed("LAST_400", HOLD_S["LAST_400"])):
            self.hold(Cue("LAST_400", 2, "400미터 남았어요. 남은 힘을 써요."))
        else:
            self.drop("LAST_400")

    # ── 페이스 ──
    def _pace(self, t: Tick) -> None:
        if t.wall > self.back_until:
            self.drop("PACE_BACK")
        if (self.plan.level == "BRIEF" or self.mode != "RUN" or not t.gps_ok or t.acc_m > PACE_MAX_ACC_M):
            self.drop("PACE_FAST", "PACE_SLOW", "START_FAST")
            return
        k = GHOST_H_ACC_START if t.active <= START_GUARD_TO_S else GHOST_H_ACC

        def gate(acc_seen: float) -> float:
            return max(GHOST_H_MIN_M, k * max(t.acc_m, acc_seen))

        # 목표가 없을 때: 출발 가드(평소보다 한참 빠른 출발) — 러닝마다 한 번
        if not self.plan.target_pace:
            if (self.p.usual_pace and self.p.kind == "RUN" and "START_FAST" not in self.said
                    and START_GUARD_FROM_S <= t.active <= START_GUARD_TO_S
                    and self.guard.fast >= gate(self.guard.fast_acc)):
                self.hold(Cue("START_FAST", 3, "출발이 평소보다 빨라요. 숨이 편한 속도로 조금만 늦춰요.",
                              tail="출발이 평소보다 빨랐어요. 조금만 늦춰요."))
            else:
                self.drop("START_FAST")
            return

        target = self.plan.target_pace
        run_cad = self.run_cadence()
        cad_now = self.run_cadence_now()
        feet_slowed = bool(run_cad and cad_now and cad_now <= run_cad * FEET_SLOWED)
        # 느려졌는데 발은 그대로다 — 오르막 · 맞바람일 수 있다. 문턱을 올리고, 말하더라도 재촉하지 않는다
        fast_h = gate(self.ghost.fast_acc)
        slow_h = gate(self.ghost.slow_acc) * (1.0 if feet_slowed else HILL_EXTRA)
        direction = "FAST" if self.ghost.fast >= fast_h else "SLOW" if self.ghost.slow >= slow_h else None
        h = fast_h if (self.exc_dir or direction) == "FAST" else gate(self.ghost.slow_acc)

        # 말한 뒤: 그 뒤로 내내 달린 페이스가 목표의 ±3.5% 이고 다시 벌어지고 있지 않으면 돌아온 것.
        # 반대쪽으로 넘어갔으면(빠르다가 너무 느려짐) 칭찬 없이 끝낸다 — 그쪽 벗어남이 따로 잡는다
        if self.exc_dir and self.exc_cues > 0:
            w = max(PACE_BACK_M, PACE_BACK_ACC * t.acc_m)
            p = self.recent_pace(w, after_m=self.cue_at_m)
            level = self.ghost.fast if self.exc_dir == "FAST" else self.ghost.slow
            state = None
            if p is not None and level <= h * 0.5:
                dev = (p - target) / target
                last = self.recent_pace(DELTA_MIN_M, after_m=self.cue_at_m)
                near = last is not None and abs(last - target) <= target * PACE_BACK_BAND * PACE_BACK_NOW
                if abs(dev) <= PACE_BACK_BAND and near:
                    state = "BACK"
                elif (dev > 0) == (self.exc_dir == "FAST"):
                    state = "OVER"
            if state is None:
                self.back_since = None
            else:
                if self.back_since is None:
                    self.back_since = t.active
                if t.active - self.back_since >= PACE_BACK_HOLD_S:
                    if state == "BACK":
                        self.back_until = t.wall + PACE_BACK_SAY_S
                        self.hold(Cue("PACE_BACK", 4, "좋아요, 목표 페이스예요.", tail="목표 페이스로 돌아왔어요.",
                                      followup=True))
                        self.ghost.reset()
                    self.exc_dir, self.exc_cues, self.exc_clear_since, self.back_since = None, 0, None, None
                    self.drop("PACE_FAST", "PACE_SLOW")
                    return

        if direction is None:
            self.drop("PACE_FAST", "PACE_SLOW")
            if self.exc_dir and self.exc_cues == 0:
                # 말하기 전에 스스로 돌아왔다 — 조용히 끝낸다
                level = self.ghost.fast if self.exc_dir == "FAST" else self.ghost.slow
                if level > h * 0.5:
                    self.exc_clear_since = None
                else:
                    if self.exc_clear_since is None:
                        self.exc_clear_since = t.active
                    if t.active - self.exc_clear_since >= PACE_CLEAR_S:
                        self.exc_dir, self.exc_clear_since = None, None
            return

        if self.exc_dir != direction:
            self.exc_dir, self.exc_cues, self.exc_clear_since = direction, 0, None
            self.exc_last = -10 ** 9
            self.back_since = None
            self.drop("PACE_FAST", "PACE_SLOW")
        self.exc_clear_since = None
        self.drop("PACE_BACK")
        if self.muted.get(direction) or self.exc_cues >= PACE_CUES_PER_EXCURSION:
            self.drop("PACE_FAST", "PACE_SLOW")
            return
        if self.exc_cues > 0 and t.active - self.exc_last < PACE_RECUE_S[min(self.exc_cues, len(PACE_RECUE_S)) - 1]:
            self.drop("PACE_FAST", "PACE_SLOW")
            return
        # 몇 초 차이인지: 쌓이기 시작한 뒤로 200m 넘게 달렸으면 그 구간, 아니면 최근 200m, 그것도 없으면 숫자 없이
        span = self.ghost.span_pace(direction, t.active, t.gps_m)
        if span is None:
            span = self.recent_pace(DELTA_MIN_M)
        d = (span - target) if span is not None else None
        if d is not None and ((direction == "FAST" and d > -5) or (direction == "SLOW" and d < 5)):
            d = None
        follow = self.exc_cues > 0
        if t.active <= START_GUARD_TO_S and not follow:
            d = None                    # 출발 직후의 거리는 믿기 어렵다 — 첫 말은 숫자 없이
        again = "아직 " if follow else ""
        late = bool(self.plan.goal_m and t.live_m >= self.plan.goal_m * LATE_SHARE)
        self.live_soft = False
        if direction == "FAST":
            lead = "출발이 목표보다" if t.active <= START_GUARD_TO_S and not follow else "목표보다"
            how = f"{delta_text(d)} 빨라요" if d is not None else "빨라요"
            self.hold(Cue("PACE_FAST", 3, f"{again}{lead} {how}. 조금만 늦춰요.",
                          tail=f"{again}목표보다 {how}. 조금만 늦춰요.", followup=follow))
        else:
            how = f"{delta_text(d)} 느려요" if d is not None else "느려요"
            if late and self.plan.style == "EASY":
                self.live_soft = True
                text = "목표보다 조금 느려요. 힘들면 지금 페이스를 지켜도 좋아요."
                tail = text
            elif not feet_slowed:
                text = f"{again}목표보다 {how}."      # 오르막일 수 있다 — 재촉하지 않는다
                tail = text
            else:
                text = f"{again}목표보다 {how}. 발을 조금 더 빠르게 굴려 봐요."
                tail = f"{again}목표보다 {how}. 발을 조금 더 빠르게."
            self.hold(Cue("PACE_SLOW", 3, text, tail=tail, followup=follow))

    # ── 후반 처짐(목표 페이스가 없을 때 — 있으면 '느려요'가 같은 일을 한다) ──
    def _fade_check(self, t: Tick) -> None:
        """200m 구간이 닫힐 때마다 — 자리 잡은 페이스보다 느려지고 발도 느려졌는지."""
        self.fade_on = False
        if (self.plan.level == "BRIEF" or self.plan.target_pace or self.fade_n >= FADE_MAX
                or t.active < FADE_MIN_ACTIVE_S or t.acc_m > PACE_MAX_ACC_M
                or t.active - self.fade_last < FADE_GAP_S):
            return
        seg = self.rblocks[self.regime_from:]
        if len(seg) < FADE_MIN_BLOCKS:
            return
        settled = seg[FADE_TRIM:-FADE_TRIM]
        cads = [b[1] for b in settled if b[1] > 0]
        if len(settled) < 2 or not cads:
            return
        base_pace = statistics.median(b[0] for b in settled)
        base_cad = statistics.median(cads)
        now_pace = sum(b[0] for b in seg[-3:]) / 3
        now_cad = self.run_cadence_now()
        ref = self.fade_ref or base_pace
        self.fade_on = bool(now_cad and self.bout_run_s >= FADE_RUN_S and all(b[2] for b in self.blocks[-3:])
                            and now_pace >= ref * FADE_PACE and all(b[0] >= ref * FADE_PACE_EACH for b in seg[-2:])
                            and now_cad <= base_cad * FADE_CAD)

    def _fade(self, t: Tick) -> None:
        if self.fade_on and self.mode == "RUN" and self.fade_n < FADE_MAX:
            if self.fade_n == 0:
                text = "페이스가 조금씩 느려지고 있어요. 어깨 힘을 빼고, 발을 가볍게 굴려요."
                tail = "페이스가 조금씩 느려지고 있어요. 발을 가볍게 굴려요."
            else:
                text = "페이스가 더 떨어졌어요. 잠깐 걸으며 숨을 골라도 좋아요."
                tail = text
            self.hold(Cue("FADE", 3, text, tail=tail))
        else:
            self.drop("FADE")

    # ── 걷기 ──
    def _walk(self, t: Tick) -> None:
        if self.plan.level == "BRIEF":
            return
        if not self.walker:
            walking = self.mode == "WALK" and self.bout_walk_s >= WALK_BOUT_S
            if walking and not self.bout_counted:
                self.bout_counted = True
                self.walk_bouts += 1
            if walking and self.run_s >= WALK_BOUT_AFTER_RUN_S and "WALK_OK" not in self.said:
                self.hold(Cue("WALK_OK", 3, "걸어도 좋아요. 숨이 돌아오면 천천히 다시 달려요."))
            else:
                self.drop("WALK_OK")
            return
        self.drop("WALK_OK")
        cad = self.cadence_med(60)
        if cad is None or self.mode != "WALK":
            self.drop("BRISK")
            return
        if cad < BRISK_CAD:
            self.brisk_low_s += 1
            self.brisk_ok_s = 0
        else:
            self.brisk_ok_s += 1
            self.brisk_low_s = max(0, self.brisk_low_s - 2)
        if (self.brisk_low_s >= BRISK_LOW_S and t.active >= BRISK_MIN_ACTIVE_S
                and self.said.get("BRISK", 0) < BRISK_MAX and t.active - self.brisk_last >= BRISK_GAP_S):
            self.hold(Cue("BRISK", 3, "조금만 빠르게 걸으면 운동 효과가 커져요. 팔을 앞뒤로 크게 흔들어 보세요.",
                          tail="조금만 더 빠르게 걸어 봐요."))
        else:
            self.drop("BRISK")
        if self.brisk_ok_s >= BRISK_OK_S and "BRISK_OK" not in self.said:
            self.hold(Cue("BRISK_OK", 4, "운동 효과가 나는 빠르기로 잘 걷고 있어요.",
                          tail="운동 효과가 나는 빠르기로 잘 걷고 있어요.", tail_only=True))
        else:
            self.drop("BRISK_OK")

    # ── 장거리 가드 ──
    def _long_run(self, t: Tick) -> None:
        longest = self.p.longest30_m
        if not longest or longest < LONG_RUN_MIN_M or self.p.tier < 1 or self.walker:
            return
        if t.live_m >= longest * LONG_RUN[0]:
            self.facts["long_ratio"] = t.live_m / longest
        # 오늘의 한 가지가 거리면 그 말이 110% 안내를 대신한다
        own_goal = self.p.focus in ("FARTHER", "RETURN") and bool(self.p.focus_goal_m)
        pending = bool(self.plan.goal_m) and not self.goal_done      # 사용자가 정한 목표로 가는 중
        if t.live_m >= longest * LONG_RUN[1] and "LONG_200" not in self.said and self._timed("LONG_200", HOLD_S["LONG"]):
            self.drop("LONG_110")
            end = "몸이 무거우면 걷거나 멈춰도 좋아요." if pending else "오늘은 여기서 마무리하는 게 좋아요."
            self.hold(Cue("LONG_200", 2, f"최근 한 달 가장 긴 러닝의 두 배를 달렸어요. {end}"))
        else:
            self.drop("LONG_200")
            if (t.live_m >= longest * LONG_RUN[0] and "LONG_110" not in self.said and not own_goal
                    and self._timed("LONG_110", HOLD_S["LONG"])):
                self.hold(Cue("LONG_110", 2, "최근 한 달 중 가장 긴 러닝이에요. 여기부터는 몸 상태를 살피며 무리하지 말아요.",
                              tail="최근 한 달 중 가장 긴 러닝이에요. 무리하지 말아요."))
            else:
                self.drop("LONG_110")

    # ── 오늘의 한 가지: 거리(사용자가 목표를 직접 정한 러닝에서는 말하지 않는다) ──
    def _focus(self, t: Tick) -> None:
        goal = self.p.focus_goal_m
        if not goal or self.plan.goal_m or self.plan.target_pace or self.plan.level == "BRIEF":
            return
        usual = self.p.usual_m or 0.0
        if self.p.focus == "FARTHER":
            if (t.live_m >= usual and t.live_m < goal - 100 and "FOCUS_USUAL" not in self.said
                    and self._timed("FOCUS_USUAL", HOLD_S["FOCUS"])):
                self.hold(Cue("FOCUS_USUAL", 2, f"평소 달리던 거리예요. {km_text(goal)}까지 조금만 더 가요.",
                              tail=f"평소 거리예요. {km_text(goal)}까지 조금만 더."))
            else:
                self.drop("FOCUS_USUAL")
            if t.live_m >= goal and "FOCUS_GOAL" not in self.said and self._timed("FOCUS_GOAL", HOLD_S["FOCUS"]):
                self.facts["focus_done"] = True
                self.hold(Cue("FOCUS_GOAL", 2, f"오늘 목표 {km_text(goal)}, 해냈어요. 평소보다 더 달렸어요."))
            else:
                self.drop("FOCUS_GOAL")
        elif self.p.focus == "RETURN":
            if (t.live_m >= goal and t.live_m < usual - 100 and "FOCUS_GOAL" not in self.said
                    and self._timed("FOCUS_GOAL", HOLD_S["FOCUS"])):
                self.hold(Cue("FOCUS_GOAL", 2, "오랜만이니 오늘은 여기까지만 해도 충분해요. 더 간다면 천천히."))
            else:
                self.drop("FOCUS_GOAL")
            if usual and t.live_m >= usual and "FOCUS_FULL" not in self.said and self._timed("FOCUS_FULL", HOLD_S["FOCUS"]):
                self.hold(Cue("FOCUS_FULL", 2, "쉬기 전 평소 거리까지 왔어요. 오늘은 여기서 마무리하는 게 좋아요."))
            else:
                self.drop("FOCUS_FULL")

    # ── '자세히': 자세 한마디 · 물 ──
    FORM_TIPS = (
        "어깨 힘을 빼고, 팔은 가볍게 앞뒤로 흔들어요.",
        "시선은 멀리 두고, 허리를 곧게 세워요.",
        "발은 몸 바로 아래에 가볍게 디뎌요.",
    )

    def _detailed(self, t: Tick) -> None:
        if self.plan.level != "DETAILED":
            return
        if (self.mode == "RUN" and self.form_n < len(self.FORM_TIPS) and t.active >= FORM_FROM_S
                and t.active - self.form_last >= FORM_GAP_S and t.active - self.last_advice_at >= FORM_QUIET_S):
            self.hold(Cue("FORM", 4, self.FORM_TIPS[self.form_n]))
        else:
            self.drop("FORM")
        if t.active > self.hydrate_next + HYDRATE_WINDOW_S:
            self.hydrate_next += HYDRATE_S[1]       # 말할 틈이 없었다 — 다음 차례로
        if self.hydrate_next <= t.active <= self.hydrate_next + HYDRATE_WINDOW_S:
            self.hold(Cue("HYDRATE", 4, "물을 챙겼다면 한 모금 마셔요."))
        else:
            self.drop("HYDRATE")

    # ── 처음 쓰는 사람: 말할 수 있는 속도 ──
    def _talk_test(self, t: Tick) -> None:
        if (self.plan.level != "BRIEF" and self.p.tier == 0 and self.p.coached_runs < 3 and self.mode == "RUN"
                and not self.plan.target_pace and TALK_TEST_S[0] <= t.active <= TALK_TEST_S[1]
                and "TALK_TEST" not in self.said and not self.walker):
            self.hold(Cue("TALK_TEST", 3, "옆 사람과 짧게 말할 수 있으면 알맞은 속도예요. 숨이 차면 조금 늦춰요."))
        else:
            self.drop("TALK_TEST")


# ────────────────────────────────────────────────────────────────────────────
# 조정자 — 무엇을 지금 말할지(우선순위 · 간격 · 예산 · 합치기 · 버리기)
# ────────────────────────────────────────────────────────────────────────────

class Arbiter:
    def __init__(self, plan: Plan, engine: Engine):
        self.plan = plan
        self.engine = engine
        self.queue: List[Cue] = []
        self.busy_until = 0.0
        self.last_end = -10 ** 9
        self.last_advice = -10 ** 9
        self.spoken: List[Spoken] = []
        self.dropped: List[Tuple[int, str, str]] = []

    def offer(self, cues: List[Cue]) -> None:
        self.queue.extend(cues)

    def _advice_ok(self, c: Cue, now: int) -> bool:
        every = ADVICE_EVERY[self.plan.level]
        if c.prio < 3:
            return True
        if every is None:
            return False
        return c.followup or now - self.last_advice >= every

    def tick(self, t: Tick, eta_split_s: Optional[float]) -> None:
        now = t.wall
        keep: List[Cue] = []
        for c in self.queue:
            if now - c.born > CUE_TTL[c.prio]:
                self.dropped.append((now, c.id, "늦음"))
            else:
                keep.append(c)
        self.queue = keep
        if now < self.busy_until:
            if not any(c.interrupt for c in self.queue):
                return
            # 과속 경고만 말하던 것을 끊고 들어간다(30초 안에 러닝이 멈춘다)
            if self.spoken:
                self.spoken[-1].seconds = max(0.0, now - self.spoken[-1].wall)
            self.busy_until = now
            self.last_end = now - MIN_GAP[0]
        quiet = now < self.engine.quiet_until
        split_soon = (self.plan.split != "OFF" and eta_split_s is not None and eta_split_s <= MERGE_AHEAD_S
                      and t.gps_m >= GPS_MIN_M)
        cands = sorted(self.queue + list(self.engine.live.values()), key=lambda c: (c.prio, c.born))
        for c in cands:
            if c.tail_only or not self._advice_ok(c, now):
                continue
            fresh = c in self.queue                    # 방금 일어난 한 번뿐인 일
            if c.prio >= 2 and ((quiet and not fresh) or (now < START_QUIET_S and not self.engine.restored)):
                continue
            if split_soon and c.id != "SPLIT" and ((c.prio >= 2 and c.tail) or c.prio >= 3):
                continue                              # 곧 구간 안내 — 거기 붙이거나(꼬리가 있으면) 그 뒤로 미룬다
            if now - self.last_end < (MIN_GAP_SPLIT if c.id == "SPLIT" else MIN_GAP[c.prio]):
                continue
            text = c.text
            advice = c.prio >= 3
            if c.id == "SPLIT":
                self.queue.remove(c)
                text, extra, key = self._compose_split(c, now)
                if extra is not None:
                    advice = extra.prio >= 3 and not extra.followup
                    self.engine.spoken(extra.id)
                else:
                    self.engine.tail_used(key)
            elif c in self.queue:
                self.queue.remove(c)
            else:
                self.engine.spoken(c.id)
                advice = advice and not c.followup
            secs = speech_seconds(text)
            self.spoken.append(Spoken(now, t.active, c.id, text, secs, t.live_m))
            self.busy_until = now + secs
            self.last_end = now + secs
            if advice:
                self.last_advice = now
            return

    def _pick_tail(self, now: int) -> Optional[Cue]:
        best: Optional[Cue] = None
        if now < self.engine.quiet_until:
            return None
        for c in self.engine.live.values():
            if not c.tail or not self._advice_ok(c, now):
                continue
            if best is None or (c.prio, c.born) < (best.prio, best.born):
                best = c
        return best

    def _compose_split(self, c: Cue, now: int) -> Tuple[str, Optional[Cue], Optional[str]]:
        """구간 안내 + 꼬리 하나. 기다리던 말이 있으면 그것이 꼬리가 되고, 없으면 구간 스스로의 꼬리.
        9초를 넘으면 짧은 꼴(평균 페이스를 뺀 것)로, 그래도 넘으면 꼬리를 뗀다 — 떼인 조언은 다음 차례를 기다린다."""
        extra = self._pick_tail(now)
        bodies = [c.text] + ([c.short] if c.short and c.short != c.text else [])
        options: List[Tuple[str, Optional[Cue], Optional[str]]] = []
        if extra is not None:
            options += [(f"{b} {extra.tail}", extra, None) for b in bodies]
        if c.tail:
            options += [(f"{b} {c.tail}", None, c.tail_key) for b in bodies]
        options += [(b, None, None) for b in bodies]
        for text, ex, key in options:
            if speech_seconds(text) <= MAX_UTTER_S[self.plan.level]:
                return text, ex, key
        return options[-1]


# ────────────────────────────────────────────────────────────────────────────
# 가상 러닝 — 참값을 만들고, 폰이 받는 흐린 값으로 바꾼다
# ────────────────────────────────────────────────────────────────────────────

@dataclass
class Seg:
    seconds: int
    pace: Optional[float] = None       # 초/km(앱에 보이는 값), None = 서 있음
    cadence: float = 0.0
    paused: bool = False
    gps: bool = True
    acc: Optional[float] = None        # 이 구간의 GPS 오차(m)
    pace_to: Optional[float] = None    # 구간 끝 페이스(서서히 바뀜)
    cad_to: Optional[float] = None
    overspeed: bool = False            # 앱의 과속 세기가 켜져 있다(30초에서 줄어든다)
    stopped: bool = False              # 과속으로 러닝이 멈췄다
    location_off: bool = False         # 위치 기능을 껐다


def mmss(text: str) -> float:
    m, s = text.split(":")
    return int(m) * 60 + int(s)


class Phone:
    """폰이 받는 값.

    GPS: 2.5초마다, 서로 이어진 오차(20초쯤 이어짐 · 보고된 오차 ÷ 1.5), 오차의 두 배(적어도 8m)를 움직여야 거리로
    (앱 TrackFixRule — 구글 위치 서비스로 받은 위치). 끊겼다 돌아오면 그 사이 거리는 더하지 않는다(앱 resetAnchor).
    걸음: 몇 초씩 뭉쳐서 오고, 센서는 마지막 걸음이 세어진 시각을 함께 준다.
    """

    def __init__(self, rng: random.Random, acc: float, step_time: str = "sensor", step_batch_s: float = 2.2):
        self.rng = rng
        self.acc = acc
        self.step_time = step_time          # sensor = 센서가 준 시각 · arrival = 앱이 받은 시각
        self.step_batch_s = step_batch_s    # 걸음 소식 사이 평균 간격
        self.nx = 0.0
        self.ny = 0.0
        self.anchor: Optional[Tuple[float, float]] = None
        self.gps_m = 0.0
        self.last_fix_wall = -10 ** 9
        self.true_steps = 0.0
        self.seen_steps = 0
        self.seen_at = 0.0
        self.next_step_news = 0.0
        self.step_log: List[Tuple[float, float]] = []   # 초마다의 (참 걸음 수, 케이던스)

    def noise(self, dt: float, acc: float) -> None:
        sigma = acc / 1.5
        rho = math.exp(-dt / 20.0)
        k = sigma * math.sqrt(1 - rho * rho)
        self.nx = self.nx * rho + self.rng.gauss(0, k)
        self.ny = self.ny * rho + self.rng.gauss(0, k)

    def fix(self, wall: int, x_true: float, acc: float) -> None:
        self.noise(2.5, acc)
        px, py = x_true + self.nx, self.ny
        self.last_fix_wall = wall
        if acc > 50:
            return
        if self.anchor is None:
            self.anchor = (px, py)
            return
        d = math.hypot(px - self.anchor[0], py - self.anchor[1])
        if d >= max(8.0, 2.0 * acc):
            self.gps_m += d
            self.anchor = (px, py)

    def lose_gps(self) -> None:
        self.anchor = None

    def steps(self, cadence: float, active: int) -> None:
        """걸음 센서는 걸음 수를 몇 초씩 묵혔다가 한꺼번에 알려 준다 — 알려 준 수는 "센서 시각"까지의 수이고,
        그 시각은 앱이 받은 때보다 앞선다(묵힌 만큼)."""
        self.true_steps += cadence / 60.0
        self.step_log.append((self.true_steps, cadence))
        if active < self.next_step_news:
            return
        self.next_step_news = active + self.step_batch_s * self.rng.uniform(0.5, 1.5)
        lag = int(self.rng.uniform(0.0, self.step_batch_s))            # 묵힌 시간
        at = max(0, len(self.step_log) - 1 - lag)
        count, cad = self.step_log[at]
        if int(count) > self.seen_steps:
            self.seen_steps = int(count)
            stamp = (active - (len(self.step_log) - 1 - at)) - ((count - int(count)) * 60.0 / cad if cad > 0 else 0.0)
            self.seen_at = stamp if self.step_time == "sensor" else float(active)


@dataclass
class Result:
    name: str
    title: str
    spoken: List[Spoken]
    dropped: List[Tuple[int, str, str]]
    wall: int
    active: int
    meters: float
    engine: Engine
    laps: List[Tuple[int, float, int]] = field(default_factory=list)
    steps: int = 0
    points: List[Tuple[int, float]] = field(default_factory=list)   # GPS 거리가 늘어난 때(달린 시간, 거리)
    cads: List[float] = field(default_factory=list)                 # 엔진이 잰 15초 케이던스
    modes: Dict[str, int] = field(default_factory=dict)              # 엔진이 본 상태별 시간(초)


# ────────────────────────────────────────────────────────────────────────────
# 러닝 뒤 코치 한마디 — 잘한 점 하나 · 다음에 해 볼 것 하나(문서 "러닝 뒤 코치 한마디")
# ────────────────────────────────────────────────────────────────────────────

def round5(sec: float) -> float:
    return 5.0 * round(sec / 5.0)


def coach_card(res: Result) -> Tuple[str, Optional[str]]:
    e, plan, prof = res.engine, res.engine.plan, res.engine.p
    facts = e.facts
    laps = res.laps
    if plan.goal_m and e.goal_done:
        laps = [l for l in laps if l[1] * 1000.0 <= plan.goal_m + GOAL_SPLIT_NEAR_M]   # 목표 뒤의 마무리 구간은 보지 않는다
    paces = [e._lap_pace(laps, i) for i in range(len(laps))]
    flags = (e.lap_clean + [False] * len(paces))[:len(paces)]
    clean = [p for p, ok in zip(paces, flags) if ok]
    all_clean = bool(paces) and len(clean) == len(paces)
    avg = res.active / (res.meters / 1000.0) if res.meters >= 100 else None
    if plan.goal_m and e.goal_done and facts.get("goal_s"):
        avg = float(facts["goal_s"]) / (plan.goal_m / 1000.0)
    cad = res.steps * 60.0 / res.active if res.active else 0.0
    walker = e.walker
    half = len(paces) // 2

    # 잘한 점 — 위에서부터 처음 맞는 것
    if prof.focus in ("START_EASY", "EASY_DAY", "RETURN") and facts.get("focus_done") and paces:
        good = f"차분하게 출발했어요 — 첫 1킬로미터 {pace_text(paces[0])}."
    elif (prof.focus == "EVEN" and int(facts.get("even_n", 0)) >= 2
          and int(facts.get("even_ok", 0)) >= FOCUS_EVEN_DONE * int(facts.get("even_n", 0))):
        good = f"구간 {int(facts['even_n'])}개 중 {int(facts['even_ok'])}개를 같은 페이스로 이어 갔어요."
        facts["focus_done"] = True
    elif prof.focus == "FARTHER" and facts.get("focus_done") and prof.usual_m:
        good = f"평소보다 {int((res.meters - prof.usual_m) // 100) * 100}미터 더 달렸어요."
    elif (plan.goal_m and e.goal_done and plan.target_pace and avg
          and abs(avg - plan.target_pace) <= plan.target_pace * GHOST_SLACK):
        good = f"목표 {km_text(plan.goal_m)}를 목표 페이스로 마쳤어요."
    elif "BEST_1K" in e.said and clean:
        good = f"최근 석 달 중 가장 빠른 1킬로미터({pace_text(min(clean))})가 나왔어요."
    elif facts.get("long_ratio"):
        good = "최근 한 달 중 가장 긴 러닝이었어요."
    elif all_clean and len(paces) >= 4 and sum(paces[half:]) / (len(paces) - half) <= sum(paces[:half]) / half * 0.97:
        good = "후반이 전반보다 빨랐어요. 힘을 잘 나눠 썼어요."
    elif all_clean and len(paces) >= 3 and (max(paces) - min(paces)) <= (sum(paces) / len(paces)) * LAP_COMPARE:
        good = "처음부터 끝까지 고른 페이스로 달렸어요." if not walker else "처음부터 끝까지 고른 페이스로 걸었어요."
    elif facts.get("PACE_BACK"):
        good = "페이스가 벗어났을 때 바로 되찾았어요."
    elif walker and cad >= BRISK_CAD:
        good = f"운동 효과가 나는 빠르기(분당 {int(round(cad))}걸음)로 걸었어요."
    elif plan.goal_m and e.goal_done:
        good = f"목표 {km_text(plan.goal_m)}를 해냈어요."
    elif res.meters >= 100:
        good = f"오늘도 해냈어요 — {km_text(res.meters)}."
    else:
        good = "오늘도 나왔어요. 그게 가장 어려운 일이에요."

    # 다음에 해 볼 것 — 위에서부터 처음 맞는 것(없으면 말하지 않는다)
    nxt = None
    rest = [p for p, ok in zip(paces[1:], flags[1:]) if ok]     # 걷기 · 신호 대기가 섞인 구간은 빼고 견준다
    if e.muted.get("FAST") and plan.target_pace and avg and not e.live_soft:
        nxt = f"목표보다 꾸준히 빨랐어요. 다음엔 목표를 {pace_text(round5(avg))}로 당겨 봐요."
    elif ((e.muted.get("SLOW") or facts.get("PACE_SLOW")) and plan.target_pace and avg
          and avg > plan.target_pace * (1 + GHOST_SLACK)):
        nxt = f"오늘 목표는 조금 빨랐어요. 다음엔 {pace_text(round5(avg))}로 잡아 봐요."
    elif (len(rest) >= 2 and flags[0] and not walker
          and paces[0] <= (sum(rest) / len(rest)) * (1 - FOCUS_START_FAST)):
        gap = sum(rest) / len(rest) - paces[0]
        nxt = (f"첫 1킬로미터가 나머지보다 {delta_text(gap)} 빨랐어요. "
               f"다음엔 첫 1킬로미터를 {pace_text(round5(sum(rest) / len(rest)))}로 가 봐요.")
    elif facts.get("FADE"):
        nxt = "후반에 페이스가 처졌어요. 처음 절반을 10초 천천히 가면 끝까지 지킬 수 있어요."
    elif not walker and e.walk_bouts >= 3:
        nxt = f"달리다 {e.walk_bouts}번 걸었어요. 다음엔 처음부터 4분 달리고 1분 걷기로 정해 두고 가 봐요."
    elif float(facts.get("long_ratio", 0.0)) >= LONG_CARD:
        nxt = "평소보다 많이 달렸어요. 내일은 쉬거나 가볍게 걸어요."
    elif walker and cad < BRISK_CAD and res.active >= BRISK_MIN_ACTIVE_S:
        nxt = "다음엔 분당 100걸음을 목표로 조금만 빠르게 걸어 봐요."
    return good, nxt


_BIAS: Dict[float, float] = {}


def gps_bias(acc: float) -> float:
    """GPS 거리는 실제보다 길게 재진다(오차가 길을 지그재그로 만든다 — 이 모형에서 오차 5m 는 약 1%, 10m 는 2.4%).

    러너가 아는 것은 앱에 보이는 페이스이므로, 장면의 페이스는 "앱에 보이는 값"으로 적고
    여기서 구한 비율만큼 실제 속도를 늦춰 넣는다.
    """
    if acc not in _BIAS:
        ratios = []
        for seed in range(12):
            phone = Phone(random.Random(900 + seed), acc)
            x = 0.0
            for wall in range(1, 1501):
                x += 2.8
                if wall % 5 in (0, 2):
                    phone.fix(wall, x, acc)
            ratios.append(phone.gps_m / x)
        _BIAS[acc] = statistics.mean(ratios)
    return _BIAS[acc]


def simulate(name: str, title: str, segs: List[Seg], profile: Profile, plan: Plan, seed: int = 7,
             acc: float = 6.0, bias_acc: Optional[float] = None, shoe_left_s: Optional[int] = None,
             crew_pool_s: Optional[int] = None, crew_start: float = 1.0, restore_at: Optional[int] = None,
             step_time: str = "sensor", step_batch_s: float = 2.2) -> Result:
    """[bias_acc]: 장면의 페이스가 "앱에 보이는 값"이 되는 GPS 오차(기본은 이 러닝의 오차).
    러닝 중에 오차가 달라지는 장면에서는 러너의 실제 속도는 그대로인데 앱에 보이는 페이스만 달라진다."""
    rng = random.Random(seed)
    phone = Phone(rng, acc, step_time, step_batch_s)
    engine = Engine(profile, plan)
    arb = Arbiter(plan, engine)
    bias = gps_bias(bias_acc if bias_acc is not None else acc)
    x = 0.0
    wall = 0
    active = 0
    laps: List[Tuple[int, float, int]] = []
    jitter = 0.0
    cjit = 0.0
    still_since: Optional[int] = None
    last_steps = 0
    last_gps = 0.0
    live = 0.0
    prev_live = (0.0, 0.0)        # 거리가 마지막으로 늘어난 때(달린 시간, 거리) — 구간 시각을 사이에서 잰다
    points: List[Tuple[int, float]] = []
    modes: Dict[str, int] = {}
    cads: List[float] = []
    stopped = False
    for seg in segs:
        for i in range(seg.seconds):
            wall += 1
            if restore_at is not None and wall == restore_at:
                engine = Engine(profile, plan, restored=True)      # 앱이 죽었다 러닝이 되살아났다
                arb.engine = engine
                arb.queue.clear()
            frac = i / max(1, seg.seconds - 1)
            pace = seg.pace
            if pace is not None and seg.pace_to is not None:
                pace = seg.pace + (seg.pace_to - seg.pace) * frac
            cad = seg.cadence + ((seg.cad_to - seg.cadence) * frac if seg.cad_to is not None else 0.0)
            a = seg.acc if seg.acc is not None else acc
            # 사람은 기계처럼 일정하지 않다: 속도 ±2% · 케이던스 ±1.5%(30초쯤 이어지는 흔들림)
            jitter = jitter * math.exp(-1 / 30.0) + rng.gauss(0, 0.02 * math.sqrt(1 - math.exp(-2 / 30.0)))
            cjit = cjit * math.exp(-1 / 30.0) + rng.gauss(0, 0.015 * math.sqrt(1 - math.exp(-2 / 30.0)))
            v = 0.0 if pace is None else (1000.0 / pace) / bias * (1 + jitter)
            c = 0.0 if pace is None else cad * (1 + cjit)
            gps_on = seg.gps and not seg.location_off
            if not seg.paused:
                active += 1
                x += v
                phone.steps(c, active)
            if not gps_on:
                phone.lose_gps()
            elif wall % 5 in (0, 2) and not seg.paused:
                phone.fix(wall, x, a)
            gps_ok = gps_on and (wall - phone.last_fix_wall) <= 10 and a <= 25
            steps = phone.seen_steps
            new_live = phone.gps_m if phone.gps_m >= GPS_MIN_M else steps * STRIDE_M
            if new_live > live:
                while new_live >= (len(laps) + 1) * 1000.0:
                    mark = (len(laps) + 1) * 1000.0
                    t0, d0 = prev_live
                    # 구간이 닫힌 시각은 앞뒤 두 거리 점 사이에서 잰다(거리가 계단으로 늘어도 구간 기록이 밀리지 않게)
                    at = t0 + (active - t0) * (mark - d0) / (new_live - d0) if new_live > d0 else active
                    laps.append((len(laps) + 1, float(len(laps) + 1), int(round(at))))
                live = new_live
                prev_live = (float(active), live)
            if phone.gps_m > last_gps and not seg.paused:
                points.append((active, phone.gps_m))
            moved = steps > last_steps or phone.gps_m > last_gps
            last_steps, last_gps = steps, phone.gps_m
            if moved or seg.paused:
                still_since = None
            elif still_since is None:
                still_since = wall
            still = still_since is not None and wall - still_since >= STILL_S
            stopped = stopped or seg.stopped
            tick = Tick(wall=wall, active=active, paused=seg.paused, steps=steps, steps_at=phone.seen_at, live_m=live,
                        gps_m=phone.gps_m, gps_ok=gps_ok, acc_m=a, laps=list(laps), still=still,
                        shoe_left_s=None if shoe_left_s is None else shoe_left_s - active,
                        crew_pool_left=None if crew_pool_s is None else max(0.0, crew_start - active / crew_pool_s),
                        overspeed_left=max(0, OVERSPEED_STOP_S - i) if seg.overspeed else None,
                        overspeed_stopped=seg.stopped, location_on=not seg.location_off)
            arb.offer(engine.step(tick))
            if not seg.paused:
                modes[engine.mode] = modes.get(engine.mode, 0) + 1
                c15 = engine.cadence(MODE_WINDOW_S)
                if c15 is not None and active > 60:
                    cads.append(c15)
            eta = None
            if v > 0.5:
                eta = ((len(laps) + 1) * 1000.0 - live) / (v * bias)
            arb.tick(tick, eta)
    # 끝 인사(과속으로 멈춘 러닝은 그 말이 끝이다)
    if not stopped:
        avg = active / (live / 1000.0) if live >= 100 else None
        end = "걷기 종료. " if engine.walker else "러닝 종료. "
        end += f"{km_text(live)}, {clock_text(active)}, 평균 페이스 {pace_text(avg)}." if avg else f"{clock_text(active)}."
        arb.spoken.append(Spoken(wall + 1, active, "RUN_END", end, speech_seconds(end), live))
    return Result(name, title, arb.spoken, arb.dropped, wall, active, live, engine, list(laps), phone.seen_steps,
                  points, cads, modes)


# ────────────────────────────────────────────────────────────────────────────
# 장면 — 페이스는 "앱에 보이는 값"
# ────────────────────────────────────────────────────────────────────────────

def scenes() -> Dict[str, Callable[[int], Result]]:
    P, S = mmss, Seg

    def past(days: float, km: float, pace: str, cad: int = 166, laps: Optional[List[str]] = None,
             rpe: Optional[str] = None, **kw) -> PastRun:
        sec = int(km * P(pace))
        return PastRun(days, km * 1000, sec, int(sec * cad / 60), [int(P(x)) for x in (laps or [])], rpe=rpe, **kw)

    def beginner(seed: int) -> Result:
        segs: List[Seg] = []
        for _ in range(5):
            segs += [S(240, P("7:30"), 158), S(60, P("11:00"), 112)]
        return simulate("beginner", "초보 · 4분 달리고 1분 걷기 25분(기록 없음 · 코치 처음)", segs, Profile(tier=0), Plan(), seed)

    def steady5(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 5200, P("5:30"), 6)
        segs = [S(210, P("5:35"), 172), S(60, P("5:35"), 172, pace_to=P("6:18"), cad_to=166),
                S(1260, P("6:18"), 166), S(330, P("6:02"), 169)]
        return simulate("steady5", "꾸준한 러너 5km · 출발만 빠름(평소 6분 20초)", segs, prof, Plan(), seed)

    def fade8(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:00"), 168, 6500, P("5:30"), 12)
        segs = [S(1980, P("6:00"), 168), S(420, P("6:00"), 168, pace_to=P("6:45"), cad_to=157),
                S(800, P("6:45"), 157)]
        return simulate("fade8", "후반 처짐 8km(평소 6분 · 최근 가장 긴 러닝 6.5km)", segs, prof, Plan(), seed)

    def target5(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:05"), 168, 6000, P("5:25"), 9)
        plan = Plan(goal_m=5000, target_pace=P("6:00"))
        segs = [S(240, P("5:30"), 172), S(480, P("5:58"), 168),
                S(150, P("6:40"), 171),            # 오르막: 느려지지만 발은 그대로(조금 잦아진다)
                S(80, P("5:43"), 167),             # 내리막
                S(370, P("6:00"), 168),
                S(200, P("6:00"), 168, pace_to=P("6:35"), cad_to=158),
                S(330, P("6:35"), 158)]
        return simulate("target5", "목표 5km · 킬로미터당 6분(출발 빠름 · 오르막 · 후반 느려짐)", segs, prof, plan, seed)

    def city6(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:10"), 167, 7000, P("5:35"), 15)
        segs = [S(480, P("6:10"), 167), S(45, None), S(300, P("6:08"), 167),
                S(90, P("6:10"), 167, gps=False),  # 지하차도
                S(280, P("6:10"), 167), S(50, None), S(1000, P("6:12"), 166)]
        return simulate("city6", "도심 6km · 신호등 두 번 · GPS 끊김 90초", segs, prof, Plan(), seed)

    def walker(seed: int) -> Result:
        prof = Profile(1, "WALK", P("10:30"), 108, 4500, None, 4)
        segs = [S(900, P("10:20"), 110), S(420, P("12:40"), 93), S(1320, P("10:15"), 111)]
        return simulate("walker", "걷기 4km · 중간에 느려짐(평소 걷는 사람)", segs, prof, Plan(), seed)

    def stubborn(seed: int) -> Result:
        prof = Profile(1, "RUN", P("5:40"), 170, 8000, P("5:05"), 20)
        plan = Plan(goal_m=5000, target_pace=P("6:00"))
        return simulate("stubborn", "말 안 듣는 러너 · 목표 6분인데 끝까지 5분 30초",
                        [S(1680, P("5:30"), 172)], prof, plan, seed)

    def tempo(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 6000, P("5:20"), 10)
        return simulate("tempo", "일부러 빠르게 5km(평소 6분 20초 · 오늘 5분 45초)",
                        [S(1760, P("5:45"), 171)], prof, Plan(), seed)

    def quiet(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 6000, P("5:40"), 10)
        return simulate("quiet", "평소대로 30분 · 빌딩 숲(GPS 오차 15m)",
                        [S(1800, P("6:20"), 166)], prof, Plan(), seed, acc=15.0)

    def long11(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:30"), 165, 5500, P("5:50"), 14)
        return simulate("long11", "장거리 11km(최근 가장 긴 러닝 5.5km)",
                        [S(4400, P("6:30"), 165)], prof, Plan(), seed)

    def shoe(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 6000, P("5:40"), 10)
        return simulate("shoe", "신발이 12분 뒤 다 닳는 5km",
                        [S(1930, P("6:20"), 166)], prof, Plan(), seed, shoe_left_s=720)

    def brief(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 5200, P("5:50"), 6)
        segs = [S(240, P("5:30"), 172), S(1300, P("6:15"), 166)]
        return simulate("brief", "'간단히'로 둔 러너 · 출발이 빨라도 구간만", segs, prof, Plan(level="BRIEF"), seed)

    def detailed(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:05"), 168, 6000, P("5:25"), 9)
        plan = Plan(level="DETAILED", style="CHALLENGE", goal_m=5000, target_pace=P("6:00"))
        segs = [S(240, P("5:30"), 172), S(480, P("5:58"), 168), S(150, P("6:40"), 171), S(80, P("5:43"), 167),
                S(370, P("6:00"), 168), S(200, P("6:00"), 168, pace_to=P("6:35"), cad_to=158),
                S(330, P("6:35"), 158)]
        return simulate("detailed", "같은 목표 5km 를 '자세히 · 기록 도전'으로", segs, prof, plan, seed)

    def focus_start(seed: int) -> Result:
        hist = [past(2, 5.0, "6:20", laps=["5:50", "6:25", "6:30", "6:28", "6:27"]),
                past(4, 4.0, "6:22", laps=["5:55", "6:30", "6:32", "6:31"]),
                past(7, 5.0, "6:18", laps=["6:15", "6:20", "6:18", "6:19", "6:18"]),
                past(10, 5.0, "6:25", laps=["5:58", "6:30", "6:33", "6:32", "6:32"])]
        return simulate("focus_start", "오늘의 한 가지 '천천히 출발' — 지난 러닝 셋 중 둘이 출발 과속",
                        [S(1920, P("6:22"), 166)], build_profile(hist), Plan(), seed)

    def farther(seed: int) -> Result:
        hist = [past(1, 3.0, "7:00", 160, laps=["7:00", "7:02", "6:58"], rpe="OK"),
                past(3, 3.1, "7:05", 160, laps=["7:03", "7:06", "7:05"], rpe="EASY"),
                past(5, 2.9, "7:02", 160, laps=["7:00", "7:04"], rpe="OK"),
                past(8, 3.0, "7:10", 159, laps=["7:08", "7:12", "7:10"])]
        return simulate("farther", "오늘의 한 가지 '조금 더' — 3km 를 세 번 편하게 달린 사람",
                        [S(1560, P("7:02"), 160)], build_profile(hist), Plan(), seed)

    def comeback(seed: int) -> Result:
        hist = [past(50, 5.0, "6:10", 167, laps=["6:08", "6:10", "6:12", "6:10", "6:10"]),
                past(53, 5.2, "6:12", 167), past(56, 4.8, "6:08", 167), past(61, 6.0, "6:15", 166)]
        return simulate("comeback", "오늘의 한 가지 '오랜만' — 50일 만에 달리는 사람(쉬기 전 평소 5km)",
                        [S(2000, P("6:25"), 165)], build_profile(hist), Plan(), seed)

    # ── 검토에서 나온 어려운 경우 ──
    def overspeed(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:10"), 167, 7000, P("5:35"), 15)
        segs = [S(366, P("6:10"), 167), S(8, P("6:10"), 167, overspeed=True),      # 구간 안내 도중에 과속 세기가 켜진다
                S(400, P("6:10"), 167), S(30, P("6:10"), 167, overspeed=True), S(1, None, stopped=True)]
        return simulate("overspeed", "과속 경고 두 번 · 두 번째는 30초를 채워 멈춤", segs, prof, Plan(), seed)

    def restore(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:15"), 166, 7000, P("5:40"), 10)
        return simulate("restore", "목표 6km · 25분(4km 지점)에 앱이 죽었다 되살아남",
                        [S(2300, P("6:15"), 166)], prof, Plan(goal_m=6000), seed, restore_at=1500)

    def slowjog(seed: int) -> Result:
        hist = [past(2, 3.5, "8:30", 133), past(4, 3.4, "8:35", 133), past(7, 3.6, "8:28", 134)]
        return simulate("slowjog", "느린 조깅 30분 · 분당 133걸음 · 8분 30초 페이스",
                        [S(1800, P("8:30"), 133)], build_profile(hist), Plan(), seed)

    def cooldown(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:00"), 168, 7000, P("5:30"), 12)
        segs = [S(1440, P("6:00"), 168), S(420, P("7:40"), 160)]
        return simulate("cooldown", "4km 를 달리고 일부러 느리게 마무리 조깅 7분", segs, prof, Plan(), seed)

    def yoyo(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:00"), 168, 7000, P("5:20"), 12)
        segs = [S(240, P("5:20"), 174), S(600, P("6:50"), 168)]
        return simulate("yoyo", "목표 6분 · 4분은 5분 20초, 그 뒤 6분 50초(반대로 넘어감)", segs, prof,
                        Plan(target_pace=P("6:00")), seed)

    def walkbreak(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:00"), 168, 7000, P("5:10"), 12)
        segs = [S(300, P("5:20"), 174), S(60, P("11:00"), 112), S(420, P("5:20"), 174)]
        return simulate("walkbreak", "목표 6분 · 5분 20초로 달리다 1분 걷고 다시 5분 20초", segs, prof,
                        Plan(target_pace=P("6:00")), seed)

    def aftergoal(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:00"), 168, 7000, P("5:30"), 12)
        segs = [S(1090, P("6:00"), 168), S(120, P("11:00"), 112), S(600, P("7:30"), 158)]
        return simulate("aftergoal", "목표 3km 를 이룬 뒤 2분 걷고 10분 천천히 조깅", segs, prof, Plan(goal_m=3000), seed)

    def itemstart(seed: int) -> Result:
        prof = Profile(1, "RUN", P("6:20"), 166, 6000, P("5:40"), 10)
        plan = Plan(items=("RUN_BOOSTER", "ENERGY_DRINK"), event=True)
        return simulate("itemstart", "아이템 둘을 쓰고 보상 이벤트 중에 출발 · 신발 90초 · 크루 체력 5%",
                        [S(700, P("6:20"), 166)], prof, plan, seed, shoe_left_s=90, crew_pool_s=20000, crew_start=0.05)

    return {f.__name__: f for f in (beginner, steady5, fade8, target5, city6, walker, stubborn, tempo, quiet,
                                    long11, shoe, brief, detailed, focus_start, farther, comeback,
                                    overspeed, restore, slowjog, cooldown, yoyo, walkbreak, aftergoal, itemstart)}


# ────────────────────────────────────────────────────────────────────────────
# 검증 — 틀린 말 · 놓친 말 · 말이 너무 많지 않은지
# ────────────────────────────────────────────────────────────────────────────

PACE_ADVICE = ("PACE_FAST", "PACE_SLOW", "START_FAST", "FADE")
PACE_WORDS = ("늦춰요", "발을 조금 더", "느려지고 있어요", "목표보다 조금 느려요")


def advice_count(res: Result) -> int:
    n = sum(1 for s in res.spoken if s.id in PACE_ADVICE)
    return n + sum(1 for s in res.spoken if s.id == "SPLIT" and any(w in s.text for w in PACE_WORDS))


def density(res: Result) -> Tuple[float, float, int]:
    """(분당 말 수, 말하는 시간 비율 %, 2분 창에서 가장 많이 말한 수) — 끝 인사는 뺀다."""
    said_ = [s for s in res.spoken if s.id != "RUN_END"]
    talk = sum(s.seconds for s in said_)
    worst = 0
    for s in said_:
        worst = max(worst, sum(1 for o in said_ if 0 <= o.wall - s.wall < 120))
    return len(said_) / (res.wall / 60.0), 100.0 * talk / res.wall, worst


def _steady(acc_segs: List[Tuple[int, float]], target: bool, seed: int, bias_acc: Optional[float] = None) -> Result:
    """제 페이스(목표 = 평소 = 6분)를 지키는 러너. [acc_segs] = (초, GPS 오차)들."""
    P = mmss
    prof = Profile(1, "RUN", P("6:00"), 168, 9000, P("5:20"), 10)
    plan = Plan(target_pace=P("6:00") if target else None)
    segs = [Seg(sec, P("6:00"), 168, acc=a) for sec, a in acc_segs]
    return simulate("mc", "", segs, prof, plan, seed=1000 + seed, acc=acc_segs[0][1], bias_acc=bias_acc)


def monte_false(acc_segs: List[Tuple[int, float]], runs: int, target: bool, bias_acc: Optional[float] = None) -> Tuple[float, float]:
    """페이스 조언이 한 번이라도 나온 러닝 비율(%) · 러닝당 평균."""
    bad = 0
    total = 0
    for seed in range(runs):
        n = advice_count(_steady(acc_segs, target, seed, bias_acc))
        bad += 1 if n else 0
        total += n
    return 100.0 * bad / runs, total / runs


def minute_pace(points: List[Tuple[int, float]], t: int, window: int = 60) -> Optional[float]:
    """흔한 방식의 "지금 페이스" — 최근 1분 동안 거리 점 사이로 잰 페이스(초/km)."""
    hi = None
    lo = None
    for at, d in reversed(points):
        if at > t:
            continue
        if hi is None:
            hi = (at, d)
        if at <= t - window:
            lo = (at, d)
            break
    if hi is None or lo is None or hi[0] - lo[0] < window * 0.5 or hi[1] <= lo[1]:
        return None
    return (hi[0] - lo[0]) / (hi[1] - lo[1]) * 1000.0


def monte_naive(acc: float, runs: int) -> Tuple[float, float, float]:
    """견줌 — 흔한 방식: 최근 1분 페이스가 목표에서 5% 넘게 30초 이어지면 말한다(말한 뒤 2분 쉼).
    제 페이스로 30분 달린 러너에게 (조언이 나온 러닝 %, 러닝당 평균, 1분 페이스의 흔들림 %)."""
    target = mmss("6:00")
    bad = 0
    total = 0
    devs: List[float] = []
    for seed in range(runs):
        res = _steady([(1800, acc)], False, seed)
        out_s = 0
        quiet_until = 0
        cues = 0
        idx = 0
        pts = res.points
        for t in range(60 + PACE_SETTLE_S, res.active):
            while idx + 1 < len(pts) and pts[idx + 1][0] <= t:
                idx += 1
            j = idx
            while j > 0 and pts[j][0] > t - 60:
                j -= 1
            if pts[idx][0] - pts[j][0] < 30 or pts[idx][1] <= pts[j][1]:
                out_s = 0
                continue
            pace = (pts[idx][0] - pts[j][0]) / (pts[idx][1] - pts[j][1]) * 1000.0
            if t % 20 == 0:
                devs.append((pace - target) / target)
            out_s = out_s + 1 if abs(pace - target) > target * 0.05 else 0
            if out_s >= 30 and t >= quiet_until:
                cues += 1
                quiet_until = t + 120
                out_s = 0
        bad += 1 if cues else 0
        total += cues
    return 100.0 * bad / runs, total / runs, 100.0 * statistics.pstdev(devs)


def monte_delay(dev: float, acc: float, runs: int) -> Tuple[float, float, float]:
    """10분에 목표보다 dev 만큼 빨라진 러너 — 알아채기까지(중앙값 · 열에 아홉 · 7분 안에 못 잡은 비율 %)."""
    P = mmss
    delays: List[float] = []
    miss = 0
    for seed in range(runs):
        prof = Profile(1, "RUN", P("6:00"), 168, 9000, P("5:20"), 10)
        plan = Plan(target_pace=P("6:00"))
        segs = [Seg(600, P("6:00"), 168), Seg(420, P("6:00") / (1 + dev), 170)]
        res = simulate("mc", "", segs, prof, plan, seed=5000 + seed, acc=acc)
        hit = [s.wall for s in res.spoken
               if s.wall > 600 and (s.id == "PACE_FAST" or (s.id == "SPLIT" and "늦춰요" in s.text))]
        if hit:
            delays.append(hit[0] - 600)
        else:
            miss += 1
    if not delays:
        return float("nan"), float("nan"), 100.0
    delays.sort()
    return statistics.median(delays), delays[max(0, int(len(delays) * 0.9) - 1)], 100.0 * miss / runs


def monte_cadence(step_time: str, batch_s: float, runs: int) -> Tuple[float, float, float]:
    """분당 166걸음으로 꾸준히 달리는 러너 20분 — (달리기가 아니라고 본 시간 %, 30초 넘게 그런 러닝 %, 15초 케이던스의 흔들림 %)."""
    P = mmss
    wrong = 0
    total = 0
    any_wrong = 0
    errs: List[float] = []
    for seed in range(runs):
        prof = Profile(1, "RUN", P("6:00"), 166, 9000, P("5:20"), 10)
        res = simulate("mc", "", [Seg(1200, P("6:00"), 166)], prof, Plan(level="BRIEF"), seed=3000 + seed,
                       step_time=step_time, step_batch_s=batch_s)
        off = sum(v for k, v in res.modes.items() if k != "RUN")
        wrong += off
        total += sum(res.modes.values())
        any_wrong += 1 if off > 30 else 0
        errs += [(c - 166.0) / 166.0 for c in res.cads[::5]]
    return 100.0 * wrong / max(1, total), 100.0 * any_wrong / runs, 100.0 * statistics.pstdev(errs)


def said(res: Result, key: str, lo: int = 0, hi: int = 10 ** 9) -> int:
    """[lo, hi] 초 사이에 key(말의 종류나 글 조각)가 나온 횟수."""
    return sum(1 for s in res.spoken if lo <= s.wall <= hi and (s.id == key or key in s.text))


def _only(r: Result, allowed: Sequence[str]) -> Optional[str]:
    for s in r.spoken:
        if s.id not in allowed:
            return f"나오면 안 되는 말: {s.text}"
    return None


def _beginner(r: Result) -> Optional[str]:
    if said(r, "WALK_OK") != 1 or said(r, "TALK_TEST") != 1:
        return "걷기 한마디 · 말할 수 있는 속도 한마디가 한 번씩 나와야 한다"
    for k in ("FADE", "START_FAST", "BRISK", "지난 구간보다", "가장 빠른"):
        if said(r, k):
            return f"나오면 안 되는 말: {k}"
    return None


def _steady5(r: Result) -> Optional[str]:
    if said(r, "출발이 평소보다", 0, 330) != 1:
        return "출발 가드가 5분 30초 안에 한 번"
    if said(r, "FADE") or said(r, "출발이 평소보다", 331):
        return "그 뒤 조언은 없어야 한다"
    return None


def _fade8(r: Result) -> Optional[str]:
    if said(r, "느려지고 있어요", 2100) < 1:
        return "처짐 조언"
    if said(r, "느려지고 있어요", 0, 2040) or said(r, "출발이"):
        return "처지기 전 조언"
    if said(r, "가장 긴 러닝이에요") != 1:
        return "장거리 안내"
    return None


def _target5(r: Result) -> Optional[str]:
    if said(r, "출발이 목표보다", 0, 330) < 1:
        return "출발 빠름"
    if said(r, "PACE_SLOW", 720, 900) or said(r, "느려요.", 720, 900):
        return "오르막(12:00 ~ 14:30)에서 느리다고 말했다"
    if said(r, "PACE_FAST", 870, 1000) or said(r, "늦춰요", 870, 1000):
        return "내리막에서 빠르다고 말했다"
    if said(r, "느려요", 1400) < 1:
        return "후반 느려짐"
    if said(r, "GOAL_REACHED") != 1 or said(r, "절반 왔어요") != 1 or said(r, "마지막 1킬로미터") != 1:
        return "절반 · 마지막 1km · 달성"
    if said(r, "FADE") or said(r, "5킬로미터. "):
        return "목표 페이스 러닝에서는 처짐 조언 · 목표 지점의 구간 안내를 하지 않는다"
    return None


def _city6(r: Result) -> Optional[str]:
    if said(r, "GPS_WEAK") != 1 or said(r, "STILL") > 1:
        return "GPS 약함 한 번 · 멈춤은 많아야 한 번"
    for k in ("출발이", "FADE", "WALK_OK", "지난 구간보다", "고르게"):
        if said(r, k):
            return f"나오면 안 되는 말: {k}"
    return None


def _walker(r: Result) -> Optional[str]:
    if said(r, "BRISK", 1000, 1500) != 1:
        return "느려진 구간에서 빠르게 걷기 조언 한 번"
    if said(r, "운동 효과가 나는") != 1:
        return "잘 걷는다는 말 한 번"
    for k in ("출발이", "FADE", "WALK_OK", "LONG_", "러닝"):
        if said(r, k):
            return f"나오면 안 되는 말: {k}"
    return None


def _stubborn(r: Result) -> Optional[str]:
    n = said(r, "늦춰요")
    if not 3 <= n <= 4:
        return f"빠르다는 말은 세 번(많아야 네 번)인데 {n}번"
    if said(r, "늦춰요", 1000):
        return "17분 뒤에도 말했다"
    if said(r, "PACE_BACK") or said(r, "돌아왔어요") or said(r, "PACE_SLOW"):
        return "돌아오지 않았는데 돌아왔다고 했다"
    return None


def _tempo(r: Result) -> Optional[str]:
    if said(r, "출발이 평소보다") != 1 or said(r, "FADE"):
        return "출발 가드 한 번만"
    return None


def _quiet(r: Result) -> Optional[str]:
    return _only(r, ("RUN_START", "SPLIT", "RUN_END"))


def _long11(r: Result) -> Optional[str]:
    if said(r, "가장 긴 러닝이에요") != 1 or said(r, "두 배를 달렸어요") != 1 or said(r, "출발이") or said(r, "FADE"):
        return "110% · 두 배 안내만"
    return None


def _shoe(r: Result) -> Optional[str]:
    if said(r, "5분쯤", 400, 460) != 1 or said(r, "SHOE_WORN", 715, 730) != 1:
        return "5분 전 · 다 닳음"
    return None


def _brief(r: Result) -> Optional[str]:
    for s in r.spoken:
        if s.id not in ("RUN_START", "SPLIT", "RUN_END") or "평소" in s.text or "지난" in s.text:
            return f"간단히인데 다른 말: {s.text}"
    return None


def _detailed(r: Result) -> Optional[str]:
    if said(r, "FADE") or said(r, "보폭"):
        return "목표 페이스 러닝에서 서로 어긋나는 보폭 조언"
    if said(r, "평균") < 2 or said(r, "FORM") < 1:
        return "평균 페이스 · 자세 한마디"
    return None


def _focus_start(r: Result) -> Optional[str]:
    if r.engine.p.focus != "START_EASY" or said(r, "첫 1킬로미터를 천천히") != 1:
        return "출발 인사에 오늘의 한 가지"
    if said(r, "차분하게 출발했어요") != 1 or said(r, "출발이"):
        return "첫 구간에서 해냈다고 말해야 한다"
    if "차분하게" not in coach_card(r)[0]:
        return "러닝 뒤 한마디"
    return None


def _farther(r: Result) -> Optional[str]:
    if (r.engine.p.focus != "FARTHER" or said(r, "평소 거리예요") + said(r, "평소 달리던 거리예요") != 1
            or said(r, "해냈어요") != 1 or said(r, "가장 긴 러닝이에요")):
        return "평소 거리에서 한마디 · 목표에서 한마디"
    return None


def _comeback(r: Result) -> Optional[str]:
    if r.engine.p.focus != "RETURN" or said(r, "오랜만이에요") != 1:
        return "50일을 쉬어도 오랜만 인사"
    if said(r, "여기까지만 해도 충분해요") != 1 or said(r, "쉬기 전 평소 거리") != 1:
        return "70% · 쉬기 전 평소 거리에서 한마디씩"
    return None


def _overspeed(r: Result) -> Optional[str]:
    if said(r, "OVERSPEED_WARN") != 2 or said(r, "OVERSPEED_STOP") != 1 or said(r, "RUN_END"):
        return "경고 두 번 · 멈춤 한 번 · 끝 인사 없음"
    first = [s for s in r.spoken if s.id == "OVERSPEED_WARN"][0]
    if first.wall > 369:
        return f"과속 경고가 구간 안내를 끊고 바로 나와야 한다({first.wall}초)"
    return None


def _restore(r: Result) -> Optional[str]:
    if said(r, "RUN_START") != 1 or said(r, "절반 왔어요", 1500):
        return "되살아난 뒤 출발 인사 · 지나간 절반 안내를 다시 하지 않는다"
    if any(1500 <= s.wall < 1500 + RESTORE_QUIET_S and s.id != "SPLIT" for s in r.spoken):
        return "되살아난 뒤 60초는 새로 닫힌 구간 말고는 조용히"
    if said(r, "4킬로미터.", 1560) or said(r, "4킬로미터.") > 1:
        return "4km 구간은 많아야 한 번, 제때에만(죽기 직전에 닫힌 구간은 잃는다)"
    if said(r, "마지막 1킬로미터", 1560) != 1 or said(r, "GOAL_REACHED") != 1:
        return "그 뒤 안내는 이어 간다"
    return None


def _slowjog(r: Result) -> Optional[str]:
    if said(r, "WALK_OK") or said(r, "걷기") or said(r, "BRISK"):
        return "느린 조깅을 걷기로 봤다"
    if r.engine.p.kind != "RUN" or r.modes.get("RUN", 0) < 0.95 * sum(r.modes.values()):
        return "달리는 사람 · 달리는 시간으로 봐야 한다"
    return None


def _cooldown(r: Result) -> Optional[str]:
    if said(r, "FADE") or "처졌어요" in (coach_card(r)[1] or ""):
        return "일부러 한 마무리 조깅을 처짐으로 봤다"
    return None


def _yoyo(r: Result) -> Optional[str]:
    if said(r, "PACE_BACK") or said(r, "돌아왔어요"):
        return "빠르다 느려진 것을 돌아왔다고 했다"
    if said(r, "늦춰요", 0, 260) < 1:
        return "빠를 때 한 번은 말해야 한다"
    return None


def _walkbreak(r: Result) -> Optional[str]:
    if said(r, "PACE_BACK") or said(r, "돌아왔어요"):
        return "걸은 거리 때문에 돌아왔다고 했다"
    return None


def _aftergoal(r: Result) -> Optional[str]:
    goal = [s.wall for s in r.spoken if s.id == "GOAL_REACHED"]
    if len(goal) != 1:
        return "목표 달성 한 번"
    for s in r.spoken:
        if s.wall > goal[0] and s.id not in ("SPLIT", "RUN_END"):
            return f"목표 뒤에 조언: {s.text}"
    if coach_card(r)[1]:
        return f"목표 뒤의 마무리를 돌아보기에 넣었다: {coach_card(r)[1]}"
    return None


def _itemstart(r: Result) -> Optional[str]:
    if said(r, "러닝 부스터와 에너지 드링크를 썼어요. 지금 보상 이벤트 중이에요.") != 1:
        return "아이템 · 이벤트 한 줄"
    if said(r, "거의 다 닳았어요") != 1 or said(r, "5분쯤") or said(r, "SHOE_WORN") != 1:
        return "신발은 남은 만큼 말한다"
    if said(r, "5퍼센트") != 1 or said(r, "20퍼센트"):
        return "크루 체력은 남은 만큼 말한다"
    return None


CHECKS: List[Tuple[str, str, Callable[[Result], Optional[str]]]] = [
    ("beginner", "걷기 한마디 · 말할 수 있는 속도 한마디, 구간 비교 없음", _beginner),
    ("steady5", "출발이 빠르다고 한 번, 그 뒤 조언 없음", _steady5),
    ("fade8", "처진 뒤에만 처짐 조언 · 장거리 안내", _fade8),
    ("target5", "출발 빠름 · 후반 느림, 오르막 · 내리막엔 조용히", _target5),
    ("city6", "GPS 약함 한 번, 페이스 조언 · 구간 비교 없음", _city6),
    ("walker", "느려졌을 때 조언 한 번 · 잘 걷는다는 말 한 번", _walker),
    ("stubborn", "세 번 말하고 조용히, '돌아왔어요' 없음", _stubborn),
    ("tempo", "출발 가드 한 번만", _tempo),
    ("quiet", "구간 안내만", _quiet),
    ("long11", "110% · 두 배 안내만", _long11),
    ("shoe", "5분 전 · 다 닳았을 때", _shoe),
    ("brief", "구간 숫자만", _brief),
    ("detailed", "평균 페이스 · 자세 한마디, 어긋나는 조언 없음", _detailed),
    ("focus_start", "오늘의 한 가지(천천히 출발)를 말하고, 해냈다고 알려 준다", _focus_start),
    ("farther", "평소 거리에서 한 번 · 조금 더 간 곳에서 한 번", _farther),
    ("comeback", "오랜만 인사 · 70% · 쉬기 전 평소 거리", _comeback),
    ("overspeed", "과속 경고가 말하던 것을 끊는다 · 멈추면 그 말이 끝", _overspeed),
    ("restore", "지나간 안내를 다시 하지 않고 60초 뒤 이어 간다", _restore),
    ("slowjog", "걷기로 보지 않는다", _slowjog),
    ("cooldown", "처짐으로 보지 않는다", _cooldown),
    ("yoyo", "'돌아왔어요'라고 하지 않는다", _yoyo),
    ("walkbreak", "걸은 뒤에도 '돌아왔어요'라고 하지 않는다", _walkbreak),
    ("aftergoal", "목표 뒤에는 구간만", _aftergoal),
    ("itemstart", "아이템 · 이벤트 한 줄, 신발 · 크루 체력은 남은 만큼", _itemstart),
]


def monte_scene(fn: Callable[[int], Result], runs: int, check: Callable[[Result], Optional[str]]) -> Tuple[float, List[str], float]:
    """장면을 씨앗(GPS · 걸음 흔들림)을 바꿔 가며 — (기대대로 된 비율 %, 어긋난 까닭, 가장 긴 말 초)."""
    ok = 0
    why: List[str] = []
    longest = 0.0
    for seed in range(runs):
        res = fn(100 + seed)
        longest = max([longest] + [s.seconds for s in res.spoken])
        problem = check(res)
        if problem is None:
            ok += 1
        elif len(why) < 3:
            why.append(f"씨앗 {100 + seed}: {problem}")
    return 100.0 * ok / runs, why, longest


def self_test() -> List[str]:
    """내 기준 만들기(build_profile) · 숫자 읽기 — 작은 예로 확인."""
    P = mmss
    bad: List[str] = []

    def expect(name: str, ok: bool) -> None:
        if not ok:
            bad.append(name)

    def past(days: float, km: float, pace: str, cad: int = 166, laps: Optional[List[str]] = None, **kw) -> PastRun:
        sec = int(km * P(pace))
        return PastRun(days, km * 1000, sec, int(sec * cad / 60), [int(P(x)) for x in (laps or [])], **kw)

    expect("기록 없음 = 0단계", build_profile([]).tier == 0)
    p = build_profile([past(1, 3, "6:00", laps=["5:40", "6:05", "6:10"]), past(3, 3, "6:10")])
    expect("두 번으로는 평소 값 · 가장 빠른 1km 를 만들지 않는다", p.usual_pace is None and p.best_1k is None)
    p = build_profile([past(1, 5, "6:00"), past(3, 5, "6:10"), past(6, 5, "6:20"), past(40, 5, "7:30")])
    expect("평소 페이스는 최근 러닝 쪽 중앙값", p.tier == 1 and abs(p.usual_pace - P("6:10")) <= 1)
    expect("가장 긴 러닝은 30일 안에서", p.longest30_m == 5000)
    p = build_profile([past(1, 3, "10:30", 110), past(2, 3, "10:40", 108), past(4, 3, "10:20", 112)])
    expect("걷는 사람", p.kind == "WALK" and p.best_1k is None and abs(p.usual_cadence - 110) <= 2)
    p = build_profile([past(1, 3, "8:30", 133), past(2, 3, "8:35", 132), past(4, 3, "8:28", 134)])
    expect("분당 133걸음이어도 8분 30초 페이스면 달리는 사람", p.kind == "RUN")
    p = build_profile([past(1, 5, "6:00"), past(2, 5, "6:00"), past(3, 5, "6:00"), past(50, 9, "6:00"),
                       PastRun(2, 12000, 3000, 8000, void=True)])
    expect("무효 러닝 · 30일 넘은 러닝은 가장 긴 러닝에서 뺀다", p.longest30_m == 5000)
    p = build_profile([past(1, 5, "6:00", laps=["5:20", "6:05", "6:10"]), past(2, 5, "6:00", laps=["5:25", "6:05", "6:10"]),
                       past(3, 5, "6:00", laps=["6:00", "6:00", "6:00"])])
    expect("출발 과속 둘 → 천천히 출발", p.focus == "START_EASY" and p.best_1k == P("5:20"))
    p = build_profile([past(1, 5, "6:00", laps=["5:20", "6:05", "6:10"], coached=True, focus="START_EASY", focus_done=True),
                       past(2, 5, "6:00", laps=["5:25", "6:05", "6:10"], coached=True, focus="START_EASY", focus_done=True),
                       past(3, 5, "6:00", laps=["6:00", "6:00", "6:00"])])
    expect("두 번 내리 해냈으면 다음 주제로", p.focus != "START_EASY")
    p = build_profile([past(1, 5, "6:00", rpe="HARD"), past(2, 5, "6:00", rpe="HARD"), past(4, 5, "6:00")])
    expect("힘들었어요 두 번 → 가볍게(까닭: 힘듦)", p.focus == "EASY_DAY" and p.focus_why == "HARD")
    p = build_profile([past(1, 8, "6:00"), past(2, 8, "6:00"), past(4, 8, "6:00"), past(9, 4, "6:00"),
                       past(16, 4, "6:00"), past(23, 4, "6:00")])
    expect("이번 주 거리가 앞 3주 평균의 1.3배 넘음 → 가볍게(까닭: 많이 달림)", p.focus == "EASY_DAY" and p.focus_why == "LOAD")
    p = build_profile([past(50, 5, "6:10"), past(53, 5, "6:12"), past(56, 5, "6:08")])
    expect("50일 쉬어도 오랜만 — 쉬기 전 값으로", p.focus == "RETURN" and p.tier == 1 and abs(p.usual_pace - P("6:10")) <= 2
           and p.focus_goal_m == 3500)
    expect("반년 넘게 쉬었으면 처음 하는 사람으로", build_profile([past(200, 5, "6:10"), past(203, 5, "6:12"), past(206, 5, "6:08")]).tier == 0)
    expect("읽기: 6분 5초", pace_text(365) == "6분 5초" and pace_text(360) == "6분")
    expect("읽기: 한 시간 4분 23초 · 한 시간 0분 33초", clock_text(3863) == "한 시간 4분 23초" and clock_text(3633) == "한 시간 0분 33초")
    expect("읽기: 차이 5초 단위 · 1분 넘으면 분으로", delta_text(-13) == "15초" and delta_text(68) == "1분 10초")
    expect("읽기: 5킬로미터 · 4.9킬로미터(올리지 않는다)", km_text(5010) == "5킬로미터" and km_text(4980) == "4.9킬로미터")
    expect("조사: 부스터를 · 두 배권을 · 드링크와", with_particle("러닝 부스터", "을", "를") == "러닝 부스터를"
           and with_particle("보상 두 배권", "을", "를") == "보상 두 배권을" and with_particle("에너지 드링크", "과", "와") == "에너지 드링크와")
    return bad


def fmt_wall(sec: int) -> str:
    return f"{sec // 60:02d}:{sec % 60:02d}"


def print_scene(res: Result) -> None:
    per_min, talk, worst = density(res)
    print(f"\n### {res.title}")
    print(f"\n{res.meters / 1000:.2f}km · {fmt_wall(res.active)} · 말 {len(res.spoken)}번 "
          f"(분당 {per_min:.2f}번 · 말하는 시간 {talk:.1f}% · 2분 안 최다 {worst}번)\n")
    print("| 시각 | 거리 | 코치가 한 말 |")
    print("|---|---|---|")
    for s in res.spoken:
        print(f"| {fmt_wall(s.wall)} | {s.live_m / 1000:.2f}km | {s.text} |")
    good, nxt = coach_card(res)
    print(f"\n러닝 뒤 코치 한마디 — 잘한 점: {good}" + (f" / 다음엔: {nxt}" if nxt else ""))


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--scene")
    ap.add_argument("--check", action="store_true")
    ap.add_argument("--runs", type=int, default=200)
    ap.add_argument("--seed", type=int, default=7)
    args = ap.parse_args()
    all_scenes = scenes()
    if args.scene:
        print_scene(all_scenes[args.scene](args.seed))
        return 0

    failures: List[str] = [f"자가 시험: {x}" for x in self_test()]
    if not args.check:
        print("## 장면 — 코치가 한 말 전체")
        for fn in all_scenes.values():
            print_scene(fn(args.seed))

    print("\n## 검증")
    print("\n### 말의 양")
    print("\n| 장면 | 분당 말 수 | 말하는 시간 | 2분 안 최다 |")
    print("|---|---|---|---|")
    for fn in all_scenes.values():
        res = fn(args.seed)
        per_min, talk, worst = density(res)
        print(f"| {res.title} | {per_min:.2f} | {talk:.1f}% | {worst} |")
        limit = 0.9 if res.name in ("detailed", "itemstart", "overspeed") else 0.6
        if per_min > limit:
            failures.append(f"{res.name}: 분당 {per_min:.2f}번")
        if worst > 3 and res.name != "itemstart":
            failures.append(f"{res.name}: 2분 안 {worst}번")

    n = args.runs
    print(f"\n### 틀린 조언 — 제 페이스로 30분 달린 러너에게 페이스 조언이 나온 러닝(각 {n}번)")
    print("\n| GPS 오차 | 목표 페이스 모드 | 자유 러닝 | 견줌: 흔한 방식 | 1분 페이스의 흔들림 |")
    print("|---|---|---|---|---|")
    for acc in (3.0, 5.0, 10.0, 15.0):
        a, an = monte_false([(1800, acc)], n, True)
        b, bn = monte_false([(1800, acc)], n, False)
        c, cn, sd = monte_naive(acc, n)
        print(f"| {acc:.0f}m | {a:.1f}% (러닝당 {an:.2f}번) | {b:.1f}% (러닝당 {bn:.2f}번) | {c:.1f}% (러닝당 {cn:.2f}번) | ±{sd:.1f}% |")
        if a > 5.0:
            failures.append(f"틀린 조언(목표, 오차 {acc:.0f}m) {a:.1f}%")
        if b > 2.0:
            failures.append(f"틀린 조언(자유, 오차 {acc:.0f}m) {b:.1f}%")

    print(f"\n### 틀린 조언 — GPS 품질이 달라질 때(러너의 실제 속도는 그대로, 각 {n}번)")
    print("\n| 장면 | 목표 페이스 모드 | 자유 러닝 |")
    print("|---|---|---|")
    mixes = [(f"가운데 10분만 오차 {m:.1f}m(앞뒤는 5m)", [(600, 5.0), (600, m), (600, 5.0)]) for m in (8.0, 10.0, 11.5)]
    mixes += [(f"내내 오차 {m:.0f}m 인 곳에서, 좋은 GPS(5m) 기준 6분으로 달림", [(1800, m)]) for m in (8.0, 10.0)]
    for label, segs in mixes:
        a, an = monte_false(segs, n, True, bias_acc=5.0)
        b, bn = monte_false(segs, n, False, bias_acc=5.0)
        print(f"| {label} | {a:.1f}% (러닝당 {an:.2f}번) | {b:.1f}% (러닝당 {bn:.2f}번) |")
        if a > 6.0 or b > 3.0:
            failures.append(f"틀린 조언({label}) {a:.1f}% · {b:.1f}%")

    print(f"\n### 알아채는 데 걸리는 시간 — 10분에 목표보다 빨라진 러너(각 {n}번)")
    print("\n| 벗어난 정도 | GPS 오차 | 중앙값 | 열에 아홉은 이 안에 | 7분 안에 못 잡음 |")
    print("|---|---|---|---|---|")
    for dev in (0.08, 0.12, 0.20):
        for acc in (3.0, 5.0, 10.0):
            med, p90, miss = monte_delay(dev, acc, n)
            print(f"| {dev * 100:.0f}% 빠름 | {acc:.0f}m | {med:.0f}초 | {p90:.0f}초 | {miss:.1f}% |")
            if dev >= 0.12 and acc <= 5 and (miss > 2.0 or med > 120):
                failures.append(f"늦음({dev * 100:.0f}%, 오차 {acc:.0f}m): 중앙값 {med:.0f}초 · 놓침 {miss:.1f}%")

    m = max(40, n // 2)
    print(f"\n### 걸음 시각 — 분당 166걸음으로 꾸준히 달리는 러너를 걷는 것으로 잘못 본 시간(각 {m}번)")
    print("\n| 걸음 소식 간격 | 센서가 준 시각으로 | 앱이 받은 시각으로 |")
    print("|---|---|---|")
    for batch in (2.2, 5.0, 8.0):
        a, ar, asd = monte_cadence("sensor", batch, m)
        b, br, bsd = monte_cadence("arrival", batch, m)
        print(f"| 평균 {batch:.0f}초 | 케이던스 ±{asd:.1f}% · 잘못 본 시간 {a:.2f}% (러닝의 {ar:.0f}%) "
              f"| 케이던스 ±{bsd:.1f}% · 잘못 본 시간 {b:.2f}% (러닝의 {br:.0f}%) |")
        if batch <= 5.0 and a > 1.0:
            failures.append(f"걸음 시각(센서, {batch:.0f}초): {a:.2f}%")

    print(f"\n### 장면마다 나와야 할 말 · 나오면 안 되는 말(GPS · 걸음 흔들림을 바꿔 각 {m}번)")
    print("\n| 장면 | 기대 | 기대대로 |")
    print("|---|---|---|")
    longest = [0.0, 0.0]
    for name, what, check in CHECKS:
        ok, why, lng = monte_scene(all_scenes[name], m, check)
        which = 1 if name == "detailed" else 0
        longest[which] = max(longest[which], lng)
        print(f"| {all_scenes[name](args.seed).title} | {what} | {ok:.0f}% |")
        if ok < 95:
            failures.append(f"{name}: 기대대로 {ok:.0f}% ({'; '.join(why)})")
    print(f"\n가장 긴 말: 보통 {longest[0]:.1f}초(한도 {MAX_UTTER_S['STANDARD']:.0f}초) · "
          f"자세히 {longest[1]:.1f}초(한도 {MAX_UTTER_S['DETAILED']:.0f}초)")
    if longest[0] > MAX_UTTER_S["STANDARD"] + 0.05 or longest[1] > MAX_UTTER_S["DETAILED"] + 0.05:
        failures.append(f"가장 긴 말 {longest[0]:.1f}초 · {longest[1]:.1f}초")

    if failures:
        print("\n**기준 미달**: " + " / ".join(failures))
        return 1 if args.check else 0
    print("\n**모든 기준 통과**")
    return 0


if __name__ == "__main__":
    sys.exit(main())
