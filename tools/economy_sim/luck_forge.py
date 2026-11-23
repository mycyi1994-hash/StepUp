#!/usr/bin/env python3
"""신발 성장(달린 만큼 자란다) + 에픽 이상 행운 = 경험치 · 강화 효과 — docs/stat-overhaul-2026-11-21.md 13장(13.6 점검 뒤). 서버를 부르지 않는다.

사용자 "너무 지저분한디 · 에픽부터는 행운에서 경험치 관련, 강화 관련으로 러닝 외적으로도 · 에픽 · 레전 3개, 레드 4개, 피니시 5개" → "문제 없는지 쭉 살펴봐".
모든 신발의 행운(행운 기획서 — 출발 · 줍기 · 보상, 에픽 이상의 줍기는 경험치 — 9장)은 그대로 두고, 에픽 이상에 효과를 등급만큼 더한다:

  ① 경험치 러닝 — 러닝이 끝날 때      이번 러닝 경험치 × M            (에픽 · 레전더리 · 레드라인 · 피니시)
  ② 강화 대성공 — 재료를 넣을 때      그 재료의 제 값 × M(물려주는 몫은 그대로)
  ③ 보너스 포인트 — 레벨이 오를 때    그 레벨 포인트 + N
  ④ 잠재 각성 — 5레벨마다             가장 낮은 뽑은 능력치(효율 · 내구도 · 착화감) + 등급 범위의 R    (레드라인부터)
  ⑤ 한계 돌파 — 만렙이 된 뒤          달린 경험치가 통 하나만큼 찰 때마다 포인트 + 1, 상한    (피니시만)

성장: 경험치 걸음(에너지 안 · 닳기 전 · 경로로 확인된 걸음 — 서버의 "보상 걸음"과 다르다) × RUN_XP × 3.1^(등급−1) + 재료 + 줍기.
에너지는 지금 서버처럼 가진 신발 가운데 가장 높은 레벨을 따른다(ENERGY_RULE — 'OWNED', 신은 신발이면 'SHOE').
②·③·④ 의 Λ 는 그 레벨에 들어선 때의 값(포인트를 바꿔도 다음 레벨까지 그대로 — 다시 나누기 시점 꼼수 막기, 13.6).

    python3 tools/economy_sim/luck_forge.py           # 효과 · 크기 · 성장 · 만렙 하루 SUP · "행운으로 키우고 만렙에서 바꾸기" 견줌
    python3 tools/economy_sim/luck_forge.py --check   # 설계 목표 — 밖이면 1
"""
import math, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import stats_v5 as V  # noqa: E402
import stats_today as TD  # noqa: E402
import luck_model as LM  # noqa: E402

for _line in open(os.path.join(HERE, 'stats_v5_fit.txt'), encoding='utf-8'):
    _g, _b, _k = _line.split(); V.B[_g], V.K[_g] = float(_b), float(_k)

G = list(TD.GRADES)
HIGH = ('EPIC', 'LEGENDARY', 'REDLINE', 'FINISH')
KO = dict(zip(G, ['일반', '레어', '에픽', '레전더리', '레드라인', '피니시']))
MAXLV, SPM = TD.MAX_LEVEL, V.SPM
F = {g: 3.1 ** max(TD.ORD[g] - 1, 0) for g in G}           # 통의 크기(서버 economy.forge_need 의 3.1^(등급−1))
ORD = {'EPIC': 0, 'LEGENDARY': 1, 'REDLINE': 2, 'FINISH': 3}
COUNT = dict(EPIC=3, LEGENDARY=3, REDLINE=4, FINISH=5)    # 등급마다 효과 수(사용자)


def need(g, lv):
    """0119 서버 그대로 — Lv 의 통 = 2,880 × 3.1^(등급−1) × (1 + 0.3 × (Lv − 1)), 만렙이면 없다"""
    return 2880 * F[g] * (1 + 0.3 * (lv - 1)) if lv < MAXLV[g] else None


TANK = {g: sum(need(g, l) for l in range(1, MAXLV[g])) for g in G}
BOX_DAY = 3 * 1506 + 2 / 7 * 10972     # pace.py 의 수급(보통 상자 3 · 상급 주 2) — 대성공 전
LEVEL_BOX = 10972                      # 5레벨마다 상급 상자 하나 — 대성공 전
SERVER_CRIT = (0.10, 2)                # 지금 0118: 10% 고정 · × 2

# ── 손잡이(제안) — 크기는 에픽 · 레전더리 · 레드라인 · 피니시 ─────────────────────────────
RUN_XP = None          # 경험치 걸음 하나 = RUN_XP × F — fit_run_xp() 가 "지금 서버 + 달리기 경험치로 피니시 만렙 365일"에 맞춘다
FINISH_DAYS = 365
MINUTES = 40
ENERGY_RULE = 'OWNED'  # 'OWNED' = 가진 신발 가운데 가장 높은 레벨(지금 서버, 낮은 등급을 만렙으로 둔 사람은 60분) · 'SHOE' = 키우는 신발의 레벨
RUN_P = (0.20, 0.15, 0.10, 0.08)   # ① 행운 끝에서 러닝마다 터지는 비율(확률 = 이 값 × Λ / Λ끝)
RUN_M = (2, 3, 4, 5)
CRIT_P = (0.10, 0.10)              # ② 확률 = 10% + 10% × Λ(행운 기획서)
CRIT_M = (3, 4, 5, 6)              # ② 제 값에만(지금은 모든 등급 × 2)
BONUS_P = 0.30                     # ③ 확률 = 30% × Λ
BONUS_N = (1, 2, 3, 4)
AWAKE_P = 0.40                     # ④ 확률 = 40% × Λ(5레벨마다)
AWAKE_R = (0.0, 0.0, 0.10, 0.15)
BREAK_P = (0.50, 0.50)             # ⑤ 확률 = 50% + 50% × Λ — 달린 경험치(① 포함)만, 재료 · 줍기는 들지 않는다(만렙 신발은 재료를 받지 않는다 — 0054)
BREAK_CAP = 16
FIND_XP = ((300, 1.0), (600, 0.55), (3100, 0.60))   # 9장 — 소 · 중 · 대 줍기에서 경험치가 나오는 양 · 몫(나머지는 아이템)


def energy_override():
    if ENERGY_RULE == 'OWNED':
        V.ENERGY_MIN = (60.0, 60.0)
    else:
        V.ENERGY_MIN = (20.0, 60.0)


def lam_of(g, lv, mode, alloc=None):
    s = V.shoe(g)
    if mode == 'zero': return s['luck']
    pl = TD.points_total(g, lv) if mode == 'all' else (alloc[3] if alloc else 0)
    return s['luck'] + max(V.LUCK_MAX[g] - s['luck'], 0) * V.curve(pl, g)


def lam_end(g):
    return lam_of(g, MAXLV[g], 'all')


def p_run(g, lam):
    return RUN_P[ORD[g]] * lam / lam_end(g) if g in ORD else 0.0


def xp_steps(g, lv, alloc, minutes=MINUTES):
    """경험치 걸음 — 에너지 안 · 닳기 전(날마다 가득 수리)"""
    r = V.day(g, lv, minutes, alloc, detail=True, with_luck=False)
    return min(r['rewarded_min'], r['life_min']) * SPM


_finds = {}


def finds_xp(g, lam, energy, lv):
    if g not in ORD: return 0.0
    key = (round(lam, 2), round(energy, 1))
    if key not in _finds:
        o = LM.simulate(key[0], max(key[1], 0.1), 100.0, days=1500, level=lv)
        d = o['days']
        _finds[key] = sum(o[t] / d * xp * share for t, (xp, share) in zip(('S', 'M', 'B'), FIND_XP))
    return _finds[key]


def alloc_of(g, lv, mode):
    tot = TD.points_total(g, lv)
    if mode == 'all': return (0, 0, 0, tot)
    if mode == 'eff': return (tot, 0, 0, 0)
    return _best_noluck(g, lv)


_bn = {}


def _best_noluck(g, lv):
    """행운 0 가운데 가장 남는 배분(효율 · 내구도 · 착화감) — 행운 없이 사는 사람"""
    if (g, lv) not in _bn:
        tot = TD.points_total(g, lv)
        cands = [a for a in V.allocations(tot, 6) if a[3] == 0] or [(0, 0, 0, 0)]
        _bn[(g, lv)] = max(cands, key=lambda a: V.day(g, lv, MINUTES, a, with_luck=False))
    return _bn[(g, lv)]


def grow(g, mode, effects=True, base_luck=True, run_xp=None, horizon=None, after=None):
    """Lv1 → 만렙(→ ⑤). mode: 'zero' 행운 0 · 'all' 행운 다 · 'none' 행운 0 + 다른 셋 최선.
    effects=False 면 지금 서버의 강화(10% · × 2)와 달리기 경험치만. horizon 이 있으면 그날까지 SUP 합도 센다(after = 만렙 뒤 배분 모드)"""
    run_xp = RUN_XP if run_xp is None else run_xp
    o = ORD.get(g)
    lv, xp, day = 1, 0.0, 0
    bonus = awake = brk = brk_xp = sup = 0.0
    max_day = brk_day = None
    lam_lv = None
    while day < 20000:
        cur_mode = mode if (max_day is None or after is None) else after
        alloc = alloc_of(g, lv, cur_mode)
        if lam_lv is None: lam_lv = lam_of(g, lv, cur_mode, alloc)      # 그 레벨에 들어선 때의 Λ
        lam_now = lam_of(g, lv, cur_mode, alloc)
        steps = xp_steps(g, lv, alloc)
        run = steps * run_xp * F[g]
        hi = effects and g in ORD
        if hi: run *= 1 + p_run(g, lam_now) * (RUN_M[o] - 1)
        cp, cm = ((min(CRIT_P[0] + CRIT_P[1] * lam_lv, 1.0), CRIT_M[o]) if hi else SERVER_CRIT)
        box = BOX_DAY * (1 + cp * (cm - 1))
        finds = finds_xp(g, lam_now, steps / TD.STEPS_PER_ENERGY, lv) if (base_luck and effects) else 0.0
        day += 1
        if horizon:
            tot = TD.points_total(g, lv) + bonus + brk
            sup += V.day(g, lv, MINUTES, alloc) if bonus + brk == 0 else _sup_with_extra(g, lv, alloc, bonus + brk)
        if lv < MAXLV[g]:
            xp += run + box + finds
            while lv < MAXLV[g] and xp >= need(g, lv):
                xp -= need(g, lv); lv += 1
                if hi: bonus += BONUS_P * lam_lv * BONUS_N[o]
                if lv % 5 == 0:
                    xp += LEVEL_BOX * (1 + cp * (cm - 1))
                    if hi and COUNT[g] >= 4: awake += AWAKE_P * lam_lv
                lam_lv = None
            if lv >= MAXLV[g] and max_day is None:
                max_day = day
                if not horizon and not (hi and COUNT[g] >= 5): break
        else:
            if hi and COUNT[g] >= 5 and brk < BREAK_CAP:
                brk_xp += run
                tank = need(g, MAXLV[g] - 1)
                while brk_xp >= tank and brk < BREAK_CAP:
                    brk_xp -= tank; brk += min(BREAK_P[0] + BREAK_P[1] * lam_now, 1.0)
                if brk >= BREAK_CAP - 1e-9 and brk_day is None:
                    brk_day = day - max_day; brk = BREAK_CAP
            if not horizon and (brk_day is not None or not (hi and COUNT[g] >= 5) or day - max_day > 3000): break
        if horizon and day >= horizon: break
    return dict(days=max_day, bonus=bonus, awake=awake, brk=brk, brk_days=brk_day, sup=sup)


def _sup_with_extra(g, lv, alloc, extra):
    a = list(alloc); a[0] += extra       # 더 받은 포인트는 효율에 더한다고 본다(어림)
    return V.day(g, lv, MINUTES, tuple(a))


def fit_run_xp():
    lo, hi = 0.0, 2.0
    for _ in range(24):
        mid = (lo + hi) / 2
        if grow('FINISH', 'none', effects=False, run_xp=mid)['days'] > FINISH_DAYS: lo = mid
        else: hi = mid
    return (lo + hi) / 2


def best_total(g, lv, total, parts=10):
    if total <= 0: return V.day(g, lv, MINUTES, (0, 0, 0, 0))
    return max(V.day(g, lv, MINUTES, al) for al in V.allocations(int(round(total)), parts))


def awake_gain(g, r):
    """④ 한 번 — 효율 · 내구도 · 착화감 가운데 하나의 뽑은 자리를 + r, 셋의 평균 값 비율(만렙 · 40분 · 최선 배분)"""
    lv = MAXLV[g]; tot = TD.points_total(g, lv)
    base = best_total(g, lv, tot)
    orig = V.shoe
    gains = []
    for which in ('eff', 'dur', 'comfort'):
        def patched(gg, q=0.5, k=None, _w=which):
            s = orig(gg, q, k)
            if gg == g: s[_w] = orig(gg, min(q + r, 1.0), k)[_w]
            return s
        V.shoe = patched
        try: gains.append(best_total(g, lv, tot) / base - 1)
        finally: V.shoe = orig
    return sum(gains) / len(gains)


def main(check=False):
    global RUN_XP
    energy_override()
    RUN_XP = fit_run_xp()
    bad = []
    print(f'에너지: {ENERGY_RULE} · 경험치 걸음 하나 = {RUN_XP:.4f} × 3.1^(등급−1) — ' + ' · '.join(f'{KO[g]} {RUN_XP * F[g]:.2f}' for g in G)
          + f' (지금 서버 + 달리기 경험치로 피니시 만렙 {FINISH_DAYS}일에 맞춤)')
    print('\n만렙까지(날마다 40분 · 상자 재료 모두) — 지금 서버(상자만) · 달리기 경험치 더함 · 행운 0포인트 · 행운 다 넣음')
    for g in G:
        now = grow(g, 'none', effects=False, run_xp=0.0)['days']
        r_run = grow(g, 'none', effects=False)
        line = f'  {KO[g]:5s} Lv{MAXLV[g]} · 통 {TANK[g] / 1e6:5.2f}M | {now:5d}일 → {r_run["days"]:4d}일'
        if g in ORD:
            r0, r1 = grow(g, 'zero'), grow(g, 'all')
            line += f' → 0포인트 {r0["days"]}일 · 다 넣음 {r1["days"]}일({100 * (1 - r1["days"] / r_run["days"]):.0f}% 빠름)'
            if check and not (0.05 <= 1 - r1['days'] / r_run['days'] <= 0.30):
                bad.append(f'{g}: 행운 다 넣으면 만렙 {100 * (1 - r1["days"] / r_run["days"]):.0f}% 빨라짐(5 ~ 30% 밖)')
        print(line)
    print('\n에픽 이상 — 다 넣었을 때 남는 것 · 만렙 하루 SUP(40분 · 최선 배분)')
    for g in HIGH:
        o = ORD[g]; lv = MAXLV[g]; tot = TD.points_total(g, lv)
        base = best_total(g, lv, tot)
        for name, mode in (('0포인트', 'zero'), ('다 넣음', 'all')):
            r = grow(g, mode)
            aw = r['awake'] * awake_gain(g, AWAKE_R[o]) if COUNT[g] >= 4 else 0.0
            gain = best_total(g, lv, tot + r['bonus'] + r['brk']) / base * (1 + aw) - 1
            extra = (f' · ④ {r["awake"]:.1f}번' if COUNT[g] >= 4 else '') + (f' · ⑤ +{BREAK_CAP} 까지 만렙 뒤 {r["brk_days"]}일' if COUNT[g] >= 5 else '')
            print(f'  {KO[g]:5s} {name} | ③ +{r["bonus"]:.1f}포인트({100 * r["bonus"] / tot:.0f}%){extra} → 만렙 하루 SUP +{100 * gain:.1f}%')
            if check and mode == 'all' and gain > 0.15: bad.append(f'{g}: 만렙 하루 SUP +{100 * gain:.1f}%(15% 넘음)')
    print('\n"행운으로 키우고 만렙에서 다시 나누기"(13.6) — 첫 만렙의 두 배 날까지 하루 SUP 합(행운 0 · 최선) 대비')
    for g in HIGH:
        H = 2 * grow(g, 'none')['days']
        a = grow(g, 'none', horizon=H)
        b = grow(g, 'all', horizon=H, after='none')
        c = grow(g, 'all', horizon=H)
        print(f'  {KO[g]:5s} {H}일 | 행운 0 끝까지 {a["sup"]:9.0f} · 행운 → 만렙에서 바꿈 {b["sup"]:9.0f}({100 * (b["sup"] / a["sup"] - 1):+.1f}%) · 행운 끝까지 {c["sup"]:9.0f}({100 * (c["sup"] / a["sup"] - 1):+.1f}%)')
        if check and b['sup'] > a['sup'] * 1.05: bad.append(f'{g}: 행운으로 키우고 바꾸는 쪽이 5% 넘게 남음 — 행운이 늘 답')
    if check:
        if bad: print('\n'.join(bad)); sys.exit(1)
        print('\n경험치 · 강화 행운 목표 — 모두 안쪽')


if __name__ == '__main__':
    if '--shoe-energy' in sys.argv: ENERGY_RULE = 'SHOE'
    main('--check' in sys.argv)
