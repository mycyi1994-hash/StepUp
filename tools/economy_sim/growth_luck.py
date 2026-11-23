#!/usr/bin/env python3
"""신발 성장(달린 만큼 자란다) + 에픽 이상 행운(황금 구간) — docs/stat-overhaul-2026-11-21.md 11장. 서버를 부르지 않는다.

사용자 "높은 등급으로 갈수록 행운이 터졌을 때 확실히 좋아야 하고 · 신발의 레벨 성장 로직이 확실히 뭔가 있어야".

  성장: 경험치 통 하나(서버 0119 의 통 그대로)를 셋이 채운다 — ① 이 신발로 달린 보상 걸음(새로) ② 재료 넣기(지금 강화) ③ 행운.
        달리기 경험치는 걸음마다 RUN_XP × 3.1^(등급−1) — 통이 큰 등급일수록 달리기가 크다. 높은 등급은 달려서 큰다.
  행운: 에픽 이상은 황금 구간 — 보상 걸음 1,000 마다 한 번 굴려 터지면 다음 L 걸음이 × M(SUP 도 경험치도).
        M 은 등급마다(에픽 2 · 레전더리 3 · 레드라인 4 · 피니시 5) — 높은 등급일수록 드물고 크다. 5레벨마다 하나씩 열린다(WINDOW_RULES).

    python3 tools/economy_sim/growth_luck.py           # 만렙까지 걸리는 날 · 황금 구간 · 크루 나눔 · 발행
    python3 tools/economy_sim/growth_luck.py --check   # 설계 목표(11장) — 밖이면 1
"""
import math, os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import luck_tiers as LT  # noqa: E402

V, TD = LT.load()
G = list(TD.GRADES)
HIGH = ('EPIC', 'LEGENDARY', 'REDLINE', 'FINISH')
KO = dict(zip(G, ['일반', '레어', '에픽', '레전더리', '레드라인', '피니시']))
F = {g: 3.1 ** max(TD.ORD[g] - 1, 0) for g in G}           # 통의 크기(서버 economy.forge_need 의 3.1^(등급−1))
MAXLV = TD.MAX_LEVEL
SPM = V.SPM


def need(g, lv):
    """0119 서버 그대로 — Lv 의 통 = 2,880 × 3.1^(등급−1) × (1 + 0.3 × (Lv − 1))"""
    return 2880 * F[g] * (1 + 0.3 * (lv - 1)) if lv < MAXLV[g] else None


TANK = {g: sum(need(g, l) for l in range(1, MAXLV[g])) for g in G}
BOX_XP_DAY = 1.1 * (3 * 1506 + 2 / 7 * 10972)   # pace.py 의 수급 — 보통 상자 3 · 상급 주 2 · 대성공 10%(하루 약 8,400)
LEVEL_BOX_XP = 1.1 * 10972                       # 5레벨마다 상급 상자 하나

# ── 손잡이(제안) ─────────────────────────────────────────────────────
RUN_XP = None          # 보상 걸음 하나의 경험치 = RUN_XP × F — fit_run_xp() 가 "피니시 만렙 365일"에 맞춘다
FINISH_DAYS = 365
MINUTES = 40           # 기준 러너 — 날마다 40분
M = dict(EPIC=2, LEGENDARY=3, REDLINE=4, FINISH=5)        # 황금 구간 배수
CHECK = 1000           # 보상 걸음 1,000 마다 한 번 굴린다(구간 안에서는 굴리지 않는다)
BUDGET = dict(EPIC=0.088, LEGENDARY=0.105, REDLINE=0.124, FINISH=0.149)   # 만렙 · 행운 끝 · 40분에 황금 구간이 더하는 SUP ÷ 기본
                                                                             # = 10장 행운 값에서 SUP · 배수 · 수리 몫(경험치 몫 빼고) — 효율과 겨루는 크기 그대로
FIND_XP = dict(S=0.05, M=0.15, B=0.50)   # 에픽 이상 줍기 = 그 신발이 하루에 받는 경험치(달리기 + 상자)의 몫 — 대는 "반나절 치"(높은 등급일수록 저절로 크다)
SHARE_LV = 20          # Lv20 부터 크루 나눔
SHARE_STEPS = 500      # 크루원이 받는 구간 길이
SHARE_MULT = lambda g: 1 + (M[g] - 1) / 2                  # 크루원 배수 = 내 배수의 절반만큼


def window_rule(g, lv):
    """5레벨마다 열리는 것 — (구간 길이, 이어질 확률, 이어지는 상한)"""
    length = 1500 if lv >= 15 else 1250 if lv >= 5 else 1000
    chain = 0.50 if lv >= 25 else 0.25 if lv >= 10 else 0.0
    cap = None if lv >= 30 else 3
    return length, chain, cap


KQ = {}   # 굴림 확률 = KQ × Λ — calibrate() 가 BUDGET 에 맞춘다


def run_windows(n, q, rule, mult, rng):
    """보상 걸음 n 인 러닝 한 번 — 구간들의 (시작, 길이) 와 더해진 걸음 값"""
    length, chain, cap = rule
    pos, out = 0, []
    while pos + CHECK <= n:
        pos += CHECK
        if rng.random() < q:
            L, k = length, 0
            while rng.random() < chain and (cap is None or k < cap):
                L += length; k += 1
            w = min(L, n - pos)
            out.append((pos, w))
            pos += w
    extra = sum(w for _, w in out) * (mult - 1)
    return out, extra


def window_stats(g, lv, lam, minutes=MINUTES, runs=20000, seed=11, n=None):
    rng = random.Random(seed)
    n = n or reward_steps(g, lv, minutes)
    q = min(KQ[g] * lam, 1.0)
    rule = window_rule(g, lv)
    fired = extra_sum = extra_fired = 0.0
    big = 0
    for _ in range(runs):
        ws, extra = run_windows(n, q, rule, M[g], rng)
        extra_sum += extra
        if ws:
            fired += 1; extra_fired += extra
            if extra / n >= 1.0: big += 1
    return dict(q=q, ev=extra_sum / runs / n, p=fired / runs, when=(extra_fired / fired / n) if fired else 0.0, big=big / runs, n=n)


def reward_steps(g, lv, minutes=MINUTES):
    tot = TD.points_total(g, lv)
    return V.day(g, lv, minutes, (0, 0, 0, tot), detail=True, with_luck=False)['rewarded_min'] * SPM


def lam_at(g, lv, all_luck):
    s = V.shoe(g)
    if not all_luck: return s['luck']
    return s['luck'] + max(V.LUCK_MAX[g] - s['luck'], 0) * V.curve(TD.points_total(g, lv), g)


def calibrate():
    for g in HIGH:
        lv = MAXLV[g]; lam = lam_at(g, lv, True)
        lo, hi = 0.0, 0.5
        for _ in range(22):
            KQ[g] = (lo + hi) / 2
            if window_stats(g, lv, lam, runs=4000)['ev'] < BUDGET[g]: lo = KQ[g]
            else: hi = KQ[g]
        KQ[g] = (lo + hi) / 2


_finds = {}


def finds_per_day(g, lv, lam, energy):
    key = (g, lv, round(lam, 3))
    if key not in _finds:
        o = LT.simulate(lam, max(energy, 0.1), 100.0, 10.0, days=1500, level=lv)
        _finds[key] = (o['S'] / o['days'], o['M'] / o['days'], o['B'] / o['days'])
    return _finds[key]


def days_to_max(g, all_luck, run_xp=None, minutes=MINUTES, detail=False):
    run_xp = RUN_XP if run_xp is None else run_xp
    lv, xp, day = 1, 0.0, 0
    src = dict(run=0.0, box=0.0, luck=0.0)
    while lv < MAXLV[g] and day < 20000:
        day += 1
        n = reward_steps(g, lv, minutes)
        run = n * run_xp * F[g]
        luck = 0.0
        if g in HIGH:
            lam = lam_at(g, lv, all_luck)
            ws = window_stats(g, lv, lam, minutes, runs=1500, n=n)
            luck += ws['ev'] * run                                   # 구간 안의 걸음은 경험치도 × M
            S, Mi, B = finds_per_day(g, lv, lam, n / TD.STEPS_PER_ENERGY)
            luck += (S * FIND_XP['S'] + Mi * FIND_XP['M'] + B * FIND_XP['B']) * (run + BOX_XP_DAY)
        box = BOX_XP_DAY
        xp += run + luck + box
        src['run'] += run; src['luck'] += luck; src['box'] += box
        while lv < MAXLV[g] and xp >= need(g, lv):
            xp -= need(g, lv); lv += 1
            if lv % 5 == 0: xp += LEVEL_BOX_XP; src['box'] += LEVEL_BOX_XP
    if detail:
        tot = sum(src.values())
        return day, {k: v / tot for k, v in src.items()}
    return day


def fit_run_xp():
    lo, hi = 0.0, 2.0
    for _ in range(26):
        mid = (lo + hi) / 2
        if days_to_max('FINISH', False, run_xp=mid) > FINISH_DAYS: lo = mid
        else: hi = mid
    return (lo + hi) / 2


def headcount(n):
    return LT.headcount(n)


def crew_share(g, lam, n_mate):
    """Lv20 넘은 에픽 이상 한 사람과 함께 달린 크루원 — 그 러닝에서 그 사람의 첫 구간 때 크루원도 SHARE_STEPS 걸음 × SHARE_MULT(한 러닝에 한 번)"""
    st = window_stats(g, MAXLV[g], lam, runs=6000)
    gain = st['p'] * SHARE_STEPS * (SHARE_MULT(g) - 1) / n_mate
    return st['p'], gain


def issuance(g):
    """만렙 40분 — 포인트를 모두 행운에 넣었을 때 찍히는 SUP(기본 + 황금 구간) ÷ 모두 효율에 넣었을 때"""
    lv = MAXLV[g]; tot = TD.points_total(g, lv)
    rl = V.day(g, lv, MINUTES, (0, 0, 0, tot), detail=True, with_luck=False)
    re = V.day(g, lv, MINUTES, (tot, 0, 0, 0), detail=True, with_luck=False)
    luck_sup = (rl['base'] + rl['comfort']) * (1 + window_stats(g, lv, lam_at(g, lv, True))['ev'])
    return luck_sup / (re['base'] + re['comfort']), rl['base'] + rl['comfort']


def main(check=False):
    global RUN_XP
    calibrate()
    RUN_XP = fit_run_xp()
    bad = []
    print(f'달리기 경험치: 보상 걸음 하나 = {RUN_XP:.4f} × 3.1^(등급−1) — '
          + ' · '.join(f'{KO[g]} {RUN_XP * F[g]:.2f}' for g in G) + f'(피니시 만렙 {FINISH_DAYS}일에 맞춤)')
    print('\n만렙까지(날마다 40분 · 상자 재료 모두 넣음) — 지금(0119, 상자만) → 새 규칙(행운 0포인트 · 행운 다 넣음) · 경험치 출처(달리기 · 상자 · 행운)')
    import pace
    now = {}
    for i, g in enumerate(G):
        lv, xp, d = 1, 0.0, 0
        while lv < MAXLV[g]:
            d += 1; xp += BOX_XP_DAY
            while lv < MAXLV[g] and xp >= need(g, lv):
                xp -= need(g, lv); lv += 1
                if lv % 5 == 0: xp += LEVEL_BOX_XP
        now[g] = d
        d0, src0 = days_to_max(g, False, detail=True)
        d1, src1 = days_to_max(g, True, detail=True)
        km = d0 * MINUTES * SPM * 0.8 / 1000
        print(f'  {KO[g]:5s} Lv{MAXLV[g]} · 통 {TANK[g] / 1e6:6.2f}M | {d:5d}일 → {d0:4d}일(약 {km:5.0f}km) · {d1:4d}일 | '
              f'{100 * src0["run"]:3.0f} · {100 * src0["box"]:3.0f} · {100 * src0["luck"]:2.0f}% → 행운 다 넣으면 {100 * src1["run"]:3.0f} · {100 * src1["box"]:3.0f} · {100 * src1["luck"]:2.0f}%')
        if check and g in HIGH and not (0.70 <= d1 / d0 <= 0.95):
            bad.append(f'{g}: 행운 다 넣으면 만렙이 {100 * (1 - d1 / d0):.0f}% 빨라짐(5 ~ 30% 밖)')
    print('\n황금 구간(만렙 · 40분) — 굴림 확률(1,000걸음마다) · 러닝마다 터지는 비율 · 터지면 그 러닝 + % · 그 SUP · 기대값 · 그 러닝 두 배 넘는 판')
    for g in HIGH:
        lv = MAXLV[g]
        for name, al in (('0포인트', False), ('다 넣음', True)):
            lam = lam_at(g, lv, al); st = window_stats(g, lv, lam)
            _, rs = issuance(g)
            print(f'  {KO[g]:5s} ×{M[g]} {name} λ{100 * lam:3.0f} | {100 * st["q"]:4.2f}% · {100 * st["p"]:4.1f}% · +{100 * st["when"]:4.0f}% · +{rs * st["when"]:6.0f} SUP · '
                  f'기대 {100 * st["ev"]:4.1f}% · {100 * st["big"]:4.2f}%')
    print('\n레벨마다 황금 구간(행운 다 넣음 · 40분 · 그 레벨의 에너지) — 러닝마다 터지는 비율 / 터지면 + %')
    for g in HIGH:
        cells = []
        for lv in (1, 5, 10, 15, 20, 25, 30):
            if lv > MAXLV[g]: continue
            st = window_stats(g, lv, lam_at(g, lv, True), runs=6000)
            cells.append(f'Lv{lv} {100 * st["p"]:3.0f}% / +{100 * st["when"]:3.0f}%')
        print(f'  {KO[g]:5s} | ' + ' · '.join(cells))
    print('\n크루 나눔(Lv20 넘은 신발 · 행운 다 넣음 · 크루원도 40분) — 크루원 한 사람이 더 받는 몫 vs 윗선(한 사람 더 온 인원 보너스)')
    n_mate = MINUTES * SPM
    for g in HIGH:
        p, gain = crew_share(g, lam_at(g, MAXLV[g], True), n_mate)
        print(f'  {KO[g]:5s} | 그 러닝에 나눔 {100 * p:4.1f}% · 받으면 {SHARE_STEPS}걸음 × {SHARE_MULT(g):.1f} · 크루원 한 사람 +{100 * gain:3.1f}% | 윗선 2 ~ 4명 5% · 5명 30% · 그 밖 2%')
        if check and gain > 0.02: bad.append(f'{g}: 크루 나눔 +{100 * gain:.1f}%(2% 넘음)')
    print('\n발행 — 만렙 40분, 포인트를 모두 행운에 넣은 날 찍히는 SUP ÷ 모두 효율에 넣은 날(1 아래면 행운이 효율보다 덜 찍는다)')
    for g in HIGH:
        r, _ = issuance(g)
        print(f'  {KO[g]:5s} | {r:.2f}')
        if check and r > 1.0: bad.append(f'{g}: 행운 몰빵이 효율 몰빵보다 SUP 를 더 찍음({r:.2f})')
    if check:
        if bad:
            print('\n'.join(bad)); sys.exit(1)
        print('\n성장 · 황금 구간 목표 — 모두 안쪽')


if __name__ == '__main__':
    main('--check' in sys.argv)
