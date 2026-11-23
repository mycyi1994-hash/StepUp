#!/usr/bin/env python3
"""에픽 이상 신발의 행운 목록 — 터지는 행운 여럿을 한 번에 맞춘다(docs/stat-overhaul-2026-11-21.md 12장). 서버를 부르지 않는다.

사용자 "에픽 이상 신발이 터질 수 있는 행운을 왜 자꾸 하나로만 끝내는 거임?" — 성장(달린 만큼 자란다)은 growth_luck.py(11장) 그대로 쓰고,
행운은 언제 · 무엇이 터지는지 아홉 가지로 나눈다. 크기는 등급마다(에픽 < 레전더리 < 레드라인 < 피니시), 종류는 레벨마다 열린다.

    python3 tools/economy_sim/luck_catalog.py           # 등급마다 행운 목록 · 러닝마다 터지는 비율 · 크기 · 기대값 · 성장 · 크루 · 발행
    python3 tools/economy_sim/luck_catalog.py --check   # 설계 목표 — 밖이면 1
"""
import os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import growth_luck as GL  # noqa: E402

V, TD = GL.V, GL.TD
HIGH, KO, MAXLV, SPM = GL.HIGH, GL.KO, GL.MAXLV, GL.SPM
ORD = {'EPIC': 0, 'LEGENDARY': 1, 'REDLINE': 2, 'FINISH': 3}

# ── 행운 아홉 가지(제안) — 크기는 에픽 · 레전더리 · 레드라인 · 피니시 ───────────────────────────
WIN_M = (2, 3, 4, 5)            # ① 황금 구간 — 다음 L 걸음 × M (SUP 도 경험치도)
START_B = (0.20, 0.30, 0.40, 0.50)   # ④ 행운의 출발 — 이번 러닝 내내 적립 + %
JACK_M = (2, 3, 4, 5)           # ⑧ 결승 대박 — 그 러닝 기본 적립 × M
WIND_MIN = (3, 5, 7, 10)        # ⑤ 세컨드 윈드 — 에너지가 다 떨어진 순간 + 분(하루 한 번)
CRIT_M = (2, 3, 4, 5)           # ③ 강화 대성공 — 그 재료 경험치 × M(확률 10% + 10%Λ — 지금은 × 2)
SPREAD_M = (1.5, 2.0, 2.5, 3.0) # ⑦ 행운 퍼짐 — 함께 달린 크루원 500걸음 × M
CREW_JACK = 2.0                 # ⑨ 크루 대박(피니시만) — 내 결승 대박이 터지면 함께 달린 크루원 그 러닝 × 2
OPEN = dict(window=1, finds=1, crit=1, start=5, wind=10, nowear=15, spread=20, jackpot=25, crewjack=30)

# 행운의 값(만렙 · 행운 끝 · 40분)에서 SUP 로 찍히는 몫 = 11장과 같은 크기(10장 행운 값의 SUP · 배수 · 수리 몫) — 셋이 나눈다
BUDGET = GL.BUDGET
SPLIT = dict(window=0.50, start=0.30, jackpot=0.20)
NOWEAR_END = 0.15     # 행운 끝에서 러닝 15% 가 닳지 않음 — 수리를 메우는 몫 15%(윗선 25%)
WIND_END = 0.30       # 세컨드 윈드 확률 = 30% × Λ(행운 끝 피니시 30%)
FLOW_START_WIN = 1.5  # 흐름: 행운의 출발이 터진 러닝은 황금 구간 확률 × 1.5
FLOW_WIN_JACK = 2.0   # 흐름: 한 러닝에 황금 구간이 둘 이상이면 결승 대박 확률 × 2
CRIT_BASE = (0.10, 0.10)   # 강화 대성공 확률 = 10% + 10% × Λ

K = {}   # 확률 = K[g][종류] × Λ


def lam_end(g):
    return GL.lam_at(g, MAXLV[g], True)


def opened(kind, lv):
    return lv >= OPEN[kind]


def run_once(g, lv, lam, n, rng, bound_steps=0):
    """한 러닝 — 걸음 값으로 더해지는 SUP(기본 = n), 경험치 배수, 닳지 않음, 세컨드 윈드로 더 적립한 걸음"""
    o = ORD[g]; k = K[g]
    ev = dict(start=False, windows=0, jackpot=False, nowear=False, wind=0, extra=0.0, xp_extra=0.0)
    if opened('start', lv) and rng.random() < k['start'] * lam:
        ev['start'] = True; ev['extra'] += START_B[o] * n
    q = min(k['window'] * lam * (FLOW_START_WIN if ev['start'] else 1.0), 1.0)
    ws, wextra = GL.run_windows(n, q, GL.window_rule(g, lv), WIN_M[o], rng)
    ev['windows'] = len(ws); ev['extra'] += wextra; ev['xp_extra'] += wextra
    if opened('jackpot', lv):
        pj = k['jackpot'] * lam * (FLOW_WIN_JACK if len(ws) >= 2 else 1.0)
        if rng.random() < pj:
            ev['jackpot'] = True; ev['extra'] += (JACK_M[o] - 1) * n
    if opened('nowear', lv) and rng.random() < k['nowear'] * lam:
        ev['nowear'] = True
    if opened('wind', lv) and bound_steps > 0 and rng.random() < k['wind'] * lam:
        ev['wind'] = min(WIND_MIN[o] * SPM, bound_steps); ev['extra'] += ev['wind']
    return ev


def stats(g, lv, lam, minutes=GL.MINUTES, runs=20000, seed=5):
    rng = random.Random(seed)
    n = GL.reward_steps(g, lv, minutes)
    bound = max(minutes * SPM - n, 0)
    acc = dict(start=0, win=0, jack=0, nowear=0, wind=0, any=0, two=0, extra=0.0, x_start=0.0, x_win=0.0, x_jack=0.0, x_wind=0.0, xp=0.0, best=0.0)
    for _ in range(runs):
        e = run_once(g, lv, lam, n, rng, bound)
        kinds = e['start'] + (e['windows'] > 0) + e['jackpot'] + e['nowear'] + (e['wind'] > 0)
        acc['start'] += e['start']; acc['win'] += e['windows'] > 0; acc['jack'] += e['jackpot']; acc['nowear'] += e['nowear']; acc['wind'] += e['wind'] > 0
        acc['any'] += kinds > 0; acc['two'] += kinds >= 2
        acc['extra'] += e['extra']; acc['xp'] += e['xp_extra']; acc['best'] = max(acc['best'], e['extra'] / n)
        acc['x_start'] += START_B[ORD[g]] * n * e['start']
        acc['x_jack'] += (JACK_M[ORD[g]] - 1) * n * e['jackpot']
        acc['x_wind'] += e['wind']
    acc['x_win'] = acc['xp']
    out = {k: (v / runs if k not in ('best',) else v) for k, v in acc.items()}
    for k in ('extra', 'x_start', 'x_win', 'x_jack', 'x_wind', 'xp'):
        out[k] /= n
    out['n'] = n
    return out


def calibrate():
    for g in HIGH:
        o = ORD[g]; lam = lam_end(g); lv = MAXLV[g]; B = BUDGET[g]
        K[g] = dict(start=SPLIT['start'] * B / (START_B[o] * lam), window=0.0, jackpot=0.0,
                    nowear=NOWEAR_END / lam, wind=WIND_END)
        # 황금 구간(흐름 포함) → 결승 대박(흐름 포함) 차례로 맞춘다
        for kind, key, share in (('window', 'x_win', SPLIT['window']), ('jackpot', 'x_jack', SPLIT['jackpot'])):
            lo, hi = 0.0, 0.5
            for _ in range(20):
                K[g][kind] = (lo + hi) / 2
                if stats(g, lv, lam, runs=4000)[key] < share * B: lo = K[g][kind]
                else: hi = K[g][kind]
            K[g][kind] = (lo + hi) / 2


def growth_days(g, all_luck):
    """11장 성장 위에 — 황금 구간 경험치 · 줍기 · 강화 대성공(등급 배수)"""
    lv, xp, day = 1, 0.0, 0
    base_box = GL.BOX_XP_DAY / 1.1          # 대성공 빼고
    while lv < MAXLV[g] and day < 20000:
        day += 1
        lam = GL.lam_at(g, lv, all_luck)
        n = GL.reward_steps(g, lv)
        run = n * GL.RUN_XP * GL.F[g]
        st = _st(g, lv, lam)
        luck = st['xp'] * run
        S, Mi, B = GL.finds_per_day(g, lv, lam, n / TD.STEPS_PER_ENERGY)
        crit = min(CRIT_BASE[0] + CRIT_BASE[1] * lam, 1.0)
        box = base_box * (1 + crit * (CRIT_M[ORD[g]] - 1))
        luck += (S * GL.FIND_XP['S'] + Mi * GL.FIND_XP['M'] + B * GL.FIND_XP['B']) * (run + box)
        xp += run + luck + box
        while lv < MAXLV[g] and xp >= GL.need(g, lv):
            xp -= GL.need(g, lv); lv += 1
            if lv % 5 == 0: xp += GL.LEVEL_BOX_XP
    return day


_cache = {}


def _st(g, lv, lam):
    key = (g, lv, round(lam, 3))
    if key not in _cache: _cache[key] = stats(g, lv, lam, runs=1500)
    return _cache[key]


def base_sup(g, alloc_luck=True, minutes=GL.MINUTES):
    lv = MAXLV[g]; tot = TD.points_total(g, lv)
    r = V.day(g, lv, minutes, (0, 0, 0, tot) if alloc_luck else (tot, 0, 0, 0), detail=True, with_luck=False)
    return r['base'] + r['comfort'], r['repair']


def main(check=False):
    GL.calibrate(); GL.RUN_XP = GL.fit_run_xp()
    calibrate()
    bad = []
    print('확률(× Λ — 행운을 다 넣은 만렙의 Λ 를 곱한 값이 괄호):')
    for g in HIGH:
        lam = lam_end(g); k = K[g]
        print(f'  {KO[g]:5s} λ끝 {100 * lam:3.0f} | 황금 구간(1,000걸음마다) {100 * k["window"]:.2f}%({100 * k["window"] * lam:.2f}) · 출발 {100 * k["start"]:.1f}%({100 * k["start"] * lam:.1f}) · '
              f'결승 대박 {100 * k["jackpot"]:.2f}%({100 * k["jackpot"] * lam:.2f}) · 무손상 {100 * k["nowear"]:.1f}%({100 * k["nowear"] * lam:.1f}) · 세컨드 윈드 {100 * k["wind"]:.0f}%({100 * k["wind"] * lam:.0f})')
    for minutes in (40, 90):
        print(f'\n만렙 · {minutes}분 러닝 — 러닝마다 터지는 비율 / 터지면 그 러닝 + %(SUP) · 기대값')
        for g in HIGH:
            o = ORD[g]; rs, repair = base_sup(g, True, minutes)
            for name, al in (('0포인트', False), ('다 넣음', True)):
                lam = GL.lam_at(g, MAXLV[g], al); st = stats(g, MAXLV[g], lam, minutes)
                when_win = st['x_win'] / st['win'] if st['win'] else 0
                print(f'  {KO[g]:5s} {name} λ{100 * lam:3.0f} | 황금 구간 {100 * st["win"]:4.1f}% / +{100 * when_win:3.0f}%({rs * when_win:5.0f}) · '
                      f'출발 {100 * st["start"]:4.1f}% / +{100 * START_B[o]:2.0f}%({rs * START_B[o]:4.0f}) · '
                      f'결승 대박 {100 * st["jack"]:4.2f}% / +{100 * (JACK_M[o] - 1):3.0f}%({rs * (JACK_M[o] - 1):5.0f}) · '
                      f'무손상 {100 * st["nowear"]:4.1f}%(수리 {repair:3.0f}) · 세컨드 윈드 {100 * st["wind"]:4.1f}% · '
                      f'하나라도 {100 * st["any"]:4.1f}% · 둘 이상 {100 * st["two"]:4.1f}% · SUP 기대 {100 * st["extra"]:4.1f}% · 가장 큰 판 +{100 * st["best"]:4.0f}%')
                if check and al and minutes == 40 and abs(st['extra'] - BUDGET[g]) > 0.012:
                    bad.append(f'{g}: SUP 기대값 {100 * st["extra"]:.1f}%(목표 {100 * BUDGET[g]:.1f}%)')
                if check and al and st['nowear'] > 0.25:
                    bad.append(f'{g}: 무손상 {100 * st["nowear"]:.0f}%(수리 메움 25% 넘음)')
    print('\n레벨마다(행운 다 넣음 · 40분 · 그 레벨의 에너지) — 열린 종류 · 러닝마다 하나라도 터지는 비율 · SUP 기대값')
    for g in HIGH:
        cells = []
        for lv in (1, 5, 10, 15, 20, 25, 30):
            if lv > MAXLV[g]: continue
            st = stats(g, lv, GL.lam_at(g, lv, True), runs=6000)
            n_open = sum(1 for kk, v in OPEN.items() if lv >= v and (kk != 'crewjack' or g == 'FINISH') and kk not in ('spread',) ) + (1 if lv >= 20 else 0)
            cells.append(f'Lv{lv} {n_open}종 · {100 * st["any"]:3.0f}% · {100 * st["extra"]:4.1f}%')
            if check and st['extra'] > BUDGET[g] + 0.02:
                bad.append(f'{g} Lv{lv}: SUP 기대값 {100 * st["extra"]:.1f}% — 만렙({100 * BUDGET[g]:.1f}%)보다 2%p 넘게 큼(에너지가 모자란 레벨의 세컨드 윈드)')
        print(f'  {KO[g]:5s} | ' + ' · '.join(cells))
    print('\n만렙까지(날마다 40분) — 행운 0포인트 → 다 넣음(황금 구간 경험치 · 줍기 · 강화 대성공 × 등급)')
    for g in HIGH:
        d0, d1 = growth_days(g, False), growth_days(g, True)
        print(f'  {KO[g]:5s} | {d0}일 → {d1}일({100 * (1 - d1 / d0):.0f}% 빨라짐) · 강화 대성공 × {CRIT_M[ORD[g]]}')
        if check and not (0.70 <= d1 / d0 <= 0.95): bad.append(f'{g}: 만렙 {100 * (1 - d1 / d0):.0f}% 빨라짐(5 ~ 30% 밖)')
    print('\n크루(만렙 · 행운 다 넣음 · 크루원도 40분) — 크루원 한 사람이 더 받는 몫(윗선 2%)')
    n_mate = GL.MINUTES * SPM
    for g in HIGH:
        st = stats(g, MAXLV[g], lam_end(g))
        spread = st['win'] * GL.SHARE_STEPS * (SPREAD_M[ORD[g]] - 1) / n_mate
        cj = st['jack'] * (CREW_JACK - 1) if g == 'FINISH' else 0.0
        print(f'  {KO[g]:5s} | 행운 퍼짐 +{100 * spread:.2f}%' + (f' · 크루 대박 {100 * st["jack"]:.2f}% 러닝 × 2 → +{100 * cj:.2f}%' if cj else '') + f' · 합 +{100 * (spread + cj):.2f}%')
        if check and spread + cj > 0.02: bad.append(f'{g}: 크루원 +{100 * (spread + cj):.1f}%(2% 넘음)')
    print('\n발행 — 만렙 40분, 모두 행운에 넣은 날(기본 + 행운) ÷ 모두 효율에 넣은 날')
    for g in HIGH:
        rl, _ = base_sup(g, True); re, _ = base_sup(g, False)
        r = rl * (1 + stats(g, MAXLV[g], lam_end(g))['extra']) / re
        print(f'  {KO[g]:5s} | {r:.2f}')
        if check and r > 1.0: bad.append(f'{g}: 행운 몰빵이 효율 몰빵보다 더 찍음({r:.2f})')
    if check:
        if bad: print('\n'.join(bad)); sys.exit(1)
        print('\n행운 목록 목표 — 모두 안쪽')


if __name__ == '__main__':
    main('--check' in sys.argv)
