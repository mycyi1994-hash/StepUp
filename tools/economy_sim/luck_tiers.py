#!/usr/bin/env python3
"""에픽 이상 행운 — "주사위를 다루는 행운" 모형(docs/stat-overhaul-2026-11-21.md 10장). 서버를 부르지 않는다.

행운에는 두 갈래가 있다.
  ① 뜻밖의 것 — 행운이 새로 굴리는 주사위(출발 버프 · 줍기 · 보상 행운). 일반 · 레어는 이것뿐이다(luck-spec 4장 그대로).
  ② 있는 주사위 다루기 — 러닝마다 이미 굴리는 주사위(보상 운 ×0.7 ~ 1.3 · 닳음 운 ×0.667 ~ 1.444)를 두 번 굴려 좋은 쪽.
에픽 이상은 ② 가 붙고, 고정 SUP(작게 버는 러닝용)를 ② 가 대신하며, 출발 버프는 고른다.

    python3 tools/economy_sim/luck_tiers.py           # 등급 · 러너마다 행운의 값 · 갈래 · 발행 · 수리 몫 · 크루 나눔
    python3 tools/economy_sim/luck_tiers.py --check   # 설계 목표(10장) — 밖이면 1
"""
import math, os, random, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import luck_model as LM  # noqa: E402

REWARD_LUCK = (0.7, 1.0, 1.3)
WEAR_LUCK = (0.667, 0.889, 1.444)

# ── 에픽 이상 손잡이(제안) ─────────────────────────────────────────────
T = dict(
    dice=0.40,          # 보상 운 두 번 굴림 확률 = 40% × Λ(끝은 등급마다 64 · 76 · 88 · 100 → 26 · 30 · 35 · 40%) — 늘(고르지 않는다)
    flat=False,         # 고정 SUP 행운은 없다(그 자리를 두 번 굴림이 맡는다 — 고정 SUP 는 적게 버는 러닝을 위한 것)
    pick=True,          # 출발 행운이 깃들면 둘 가운데 고른다(에너지 −30% · 닳음 −50%) — 오늘 얼마나 달릴지는 러너만 안다.
                        # 셋째였던 "이번 러닝 두 번 굴림 확정"은 모형에서 한 번도 뽑히지 않아(15 ~ 45 SUP < 닳음 47 ~ 140) 뺐다
    reward=(0.015, 0.06),   # 퍼센트 행운 확률 = 1.5% + 6% × Λ(일반 · 레어 9%) — 보상을 지키는 일은 두 번 굴림이 나눠 맡는다
    mid=[('shard', 2, 0.62), ('drink', 1, 0.28), ('booster', 1, 0.10)],                     # 중: shard = 경험치 300 × 2 · 부스터 17 → 10%
    big=[('box', 1, 0.80), ('repair', 1, 0.08), ('double', 1, 0.07), ('refill', 1, 0.05)],   # 대: box = 경험치 3,100 · 내구도권 25 → 8% · 2배권 10 → 7%(소각 · 발행을 지킨다)
    share=dict(REDLINE=0.5, FINISH=0.5),   # 크루 러닝: 함께 달린 크루원에게 내 두 번 굴림 확률의 절반(여럿이면 가장 큰 하나).
                                           # 행운 합 S 에 무게를 더 주는 안(×1.5 · ×2)은 2명 크루에서 크루원 +4% — 한 사람 더 온 값을 넘어 뺐다
)


def tri(rng, lo, mode, hi):
    return rng.triangular(lo, hi, mode)


def simulate(lam, energy_per_day, run_sup, repair_day, die='REWARD', days=6000, seed=7, streak=7, level=10, p=LM.P, t=T,
             energy_bound=0.5):
    """에픽 이상 한 사람 — die = 'REWARD' 또는 'WEAR'(두 번 굴림이 지키는 쪽, 러닝 전에 고른다).
    repair_day = 이 러닝이 닳게 하는 내구도의 수리비(그 신발 값). energy_bound = 아낀 에너지가 실제 적립이 되는 몫"""
    rng = random.Random(seed)
    k = 1 + p['streak_step'] * min(streak, p['streak_max'])
    carry = 0.0; pity = 0; afterglow = False
    o = dict(start=0, finds=0, S=0, M=0, B=0, shard=0, box=0, drink=0, booster=0, repair=0, double=0, refill=0,
             sup_bonus=0.0, dice=0, dice_sup=0.0, dice_wear=0.0, buff_energy=0.0, buff_wear=0.0, days=days)
    sup_per_energy = run_sup / max(energy_per_day, 0.1)
    a = min(t['dice'] * lam, 1.0)
    for _ in range(days):
        finds_today = 0
        e_run, sup = energy_per_day, run_sup
        ps = min(LM.prob(p['luck_start'], lam) * k * (p['flow_afterglow'] if afterglow else 1), p['luck_start_cap'])
        afterglow = False
        started = rng.random() < ps
        sure_dice = False
        if started:
            o['start'] += 1
            # 둘 가운데 그날 더 값진 것을 고른다고 본다(일반 · 레어는 고르지 않고 셋 가운데 하나가 정해진다 — luck-spec 4.1)
            v_energy = p['start_energy_cut'] * e_run * sup_per_energy * energy_bound
            v_wear = p['start_wear_cut'] * repair_day
            best = max((v_energy, 'energy'), (v_wear, 'wear'))[1] if t['pick'] else rng.choice(('energy', 'wear', 'floor'))
            if best == 'energy': o['buff_energy'] += v_energy
            elif best == 'wear': o['buff_wear'] += v_wear
            elif best == 'dice': sure_dice = True
        # 있는 주사위 — 보상 운 · 닳음 운(두 번 굴림은 고른 쪽 하나만)
        r1 = tri(rng, *REWARD_LUCK); r2 = tri(rng, *REWARD_LUCK)
        w1 = tri(rng, *WEAR_LUCK); w2 = tri(rng, *WEAR_LUCK)
        roll = sure_dice or rng.random() < a
        luck_roll = r1
        if roll:
            o['dice'] += 1
            if die == 'REWARD':
                luck_roll = max(r1, r2); o['dice_sup'] += (luck_roll - r1) * sup
            else:
                o['dice_wear'] += (w1 - min(w1, w2)) / (WEAR_LUCK[0] + WEAR_LUCK[1] + WEAR_LUCK[2]) * 3 * repair_day
        # 달리는 중 — 줍기(luck-spec 4.2 그대로)
        carry += e_run; chain = 0; finds_run = 0
        while carry >= p['luck_energy_step'] and finds_today < p['find_daily_cap']:
            carry -= p['luck_energy_step']
            pf = LM.prob(p['luck_find'], lam) * k * (p['flow_start_find'] if started else 1) + p['pity_step'] * pity
            pf = 1.0 if pity >= p['pity_sure'] else min(pf, p['luck_find_cap'])
            if rng.random() < pf:
                pity = 0; finds_run += 1; finds_today += 1; o['finds'] += 1
                w = [max(LM.prob(p['tier_small'], lam), 0), LM.prob(p['tier_mid'], lam), LM.prob(p['tier_big'], lam)]
                tier = rng.choices(['S', 'M', 'B'], w)[0]
                if chain >= 1: tier = {'S': 'M', 'M': 'B', 'B': 'B'}[tier]
                chain += 1; o[tier] += 1
                loot = t['big'] if tier == 'B' else t['mid'] if tier == 'M' else LM.LOOT[tier]
                name, n, _ = rng.choices(loot, [x[2] for x in loot])[0]
                if name in o: o[name] += n
            else:
                pity += 1; chain = 0
        # 보상 — 퍼센트 행운(고정 SUP 는 없다)
        pr = min(LM.prob(t['reward'], lam) * k * (p['flow_find_reward'] if finds_run >= p['flow_find_need'] else 1), p['luck_reward_cap'])
        if rng.random() < pr:
            size = p['luck_reward_bonus'][0] + (p['luck_reward_bonus'][1] - p['luck_reward_bonus'][0]) * rng.random() ** 2
            o['sup_bonus'] += size * sup * luck_roll; afterglow = True
    return o


def value(o, run_sup, full_repair, energy_bound, xp_small=300, xp_big=3100, sup_per_xp=0.024):
    """하루 기대값(SUP 로 친 값). 줍는 것은 강화 경험치(1,000 = 24 SUP).
    SUP = 지금 찍히는 SUP(퍼센트 행운 + 두 번 굴림) · 배수 = 부스터 · 2배권이 나중 러닝에서 찍는 SUP ·
    에너지 = 에너지 버프 · 드링크 · 할당량권(에너지가 모자라는 러닝에서만 값) · 수리 = 수리비가 덜 드는 몫"""
    d = o['days']
    v = dict(
        SUP=(o['sup_bonus'] + o['dice_sup']) / d,
        배수=(o['booster'] * 0.5 + o['double'] * 1.0) * run_sup / d,
        에너지=(o['buff_energy'] + (o['drink'] * 0.25 + o['refill'] * 0.5) * run_sup * energy_bound) / d,
        수리=(o['dice_wear'] + o['buff_wear'] + o['repair'] * full_repair) / d,
        경험치=(o['shard'] * xp_small + o['box'] * xp_big) * sup_per_xp / d,
    )
    v['합'] = sum(v.values())
    return v


def run_rows(V, TD):
    rows = []
    for g in ('EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'):
        lv = TD.MAX_LEVEL[g]; tot = TD.points_total(g, lv); s = V.shoe(g)
        full = 0.45 * 110 * TD.PPS / 5 * s['eff'] * s['dur']
        for name, lam in (('0포인트', s['luck']), ('전부', s['luck'] + (V.LUCK_MAX[g] - s['luck']) * V.curve(tot, g))):
            for mi in (20, 40, 60, 90):
                r = V.day(g, lv, mi, (0, 0, 0, tot), detail=True, with_luck=False)
                rs = r['base'] + r['comfort']; energy = r['rewarded_min'] * V.SPM / TD.STEPS_PER_ENERGY
                bound = 1.0 if mi > r['energy_min'] else 0.0
                o = simulate(lam, max(energy, 0.1), rs, r['repair'], level=lv, energy_bound=bound)
                rows.append(dict(g=g, mi=mi, name=name, lam=lam, rs=rs, repair=r['repair'], o=o, v=value(o, rs, full, bound)))
    return rows


def load():
    import stats_v5 as V, stats_today as TD
    fit = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'stats_v5_fit.txt')
    for line in open(fit, encoding='utf-8'):
        g, b, kk = line.split(); V.B[g], V.K[g] = float(b), float(kk)
    return V, TD


def table(rows):
    print('등급 분 | 행운 · 두 번 굴림 | 값%(지금 SUP · 배수 SUP · 에너지 · 수리 · 경험치) | 수리비 가운데 행운이 메우는 몫')
    for r in rows:
        v, rs = r['v'], r['rs']; a = min(T['dice'] * r['lam'], 1)
        print(f"{r['g'][:4]} {r['mi']:2d} | {r['name']} λ{100 * r['lam']:3.0f} · {100 * a:2.0f}% | {100 * v['합'] / rs:4.1f}% "
              f"({100 * v['SUP'] / rs:.1f} · {100 * v['배수'] / rs:.1f} · {100 * v['에너지'] / rs:.1f} · {100 * v['수리'] / rs:.1f} · {100 * v['경험치'] / rs:.1f}) | "
              f"{100 * v['수리'] / max(r['repair'], 1):3.0f}%")


def check(rows):
    bad = []
    for r in rows:
        v, rs = r['v'], r['rs']; tag = f"{r['g']} {r['mi']}분 {r['name']}"
        if 100 * v['SUP'] / rs > 5: bad.append(f'{tag}: 지금 찍히는 SUP {100 * v["SUP"] / rs:.1f}%(5% 넘음)')
        if 100 * (v['SUP'] + v['배수']) / rs > 12: bad.append(f'{tag}: 배수까지 찍히는 SUP {100 * (v["SUP"] + v["배수"]) / rs:.1f}%(12% 넘음)')
        if v['수리'] > 0.25 * r['repair']: bad.append(f'{tag}: 수리비의 {100 * v["수리"] / r["repair"]:.0f}% 를 행운이 메움(25% 넘음 — 소각)')
        if r['name'] == '전부' and r['mi'] == 40 and not (15 <= 100 * v['합'] / rs <= 30): bad.append(f'{tag}: 값 {100 * v["합"] / rs:.1f}%(15 ~ 30% 밖)')
    for g, x in issuance(rows).items():
        if 100 * x > 12: bad.append(f'{g}: 행운이 새로 찍게 하는 SUP(달리는 시간 분포 평균) {100 * x:.1f}%(12% 넘음)')
    for r in crew_rows():
        if r['mate_gain'] / 400 > r['bound']: bad.append(f"크루 {r['n']}명 {r['g']} λ{r['lam']}: 크루원 한 사람 +{100 * r['mate_gain'] / 400:.1f}%(한 사람 더 온 값 {100 * r['bound']:.0f}% 넘음)")
    if bad:
        print('\n'.join(bad)); sys.exit(1)
    print('에픽 이상 행운 목표 — 모두 안쪽')


MIX = {20: 0.25, 40: 0.40, 60: 0.25, 90: 0.10}   # stats_v5.MIX 와 같다 — 하루 달리는 시간의 분포


def issuance(rows):
    """등급마다(행운 전부) 행운이 새로 찍게 하는 SUP — 지금 SUP + 배수 + 에너지(에너지가 모자라는 러너만) ÷ 기본, 달리는 시간 분포로 평균"""
    out = {}
    for r in rows:
        if r['name'] != '전부': continue
        v = r['v']; out.setdefault(r['g'], 0.0)
        out[r['g']] += MIX[r['mi']] * (v['SUP'] + v['배수'] + v['에너지']) / r['rs']
    return out


def headcount(n):
    """economy.crew_headcount_bonus(0070) 그대로 — 함께 달린 크루원 수 → 인원 보너스 비율"""
    if n >= 20: return 1.50
    if n >= 7: return 0.50 + 0.02 * (n - 6)
    if n == 6: return 0.50
    if n >= 2: return 0.05 * (n - 1)
    return 0.0


def share_bound(n):
    """나눔의 윗선 = 그 크루에 한 사람이 더 왔을 때 오르는 인원 보너스(20명부터는 2%) — 나눔이 사람 수를 이기지 않게"""
    return max(headcount(n + 1) - headcount(n), 0.02)


def crew_share(n, holder_g, holder_lam, own_a=0.0, base=400.0, t=T):
    """크루 러닝 n 명 가운데 레드라인 · 피니시 한 사람 — 함께 달린 크루원은 그 사람 두 번 굴림 확률의 [share] 만큼으로 보상 운을 두 번 굴린다.
    크루원이 이미 제 신발로 두 번 굴림이 있으면(에픽 이상) 둘 가운데 큰 하나 — 겹치지 않는다. 행운 합 S(luck-spec 7.5)는 그대로"""
    a_holder = min(t['dice'] * holder_lam, 1.0)
    a_mate = max(own_a, t['share'].get(holder_g, 0.0) * a_holder)
    mate_gain = 0.070 * (a_mate - own_a) * base         # E[max(r1, r2) − r1] = 0.070(보상 운 삼각 0.7 · 1.0 · 1.3)
    return dict(a_mate=a_mate, mate_gain=mate_gain, crew_gain=mate_gain * (n - 1), bad=bad_days(a_mate)[0], bound=share_bound(n))


def crew_rows():
    rows = []
    for n in (2, 5, 10, 20):
        for g, lam in (('REDLINE', 0.24), ('REDLINE', 0.88), ('FINISH', 0.30), ('FINISH', 1.00)):
            rows.append(dict(n=n, g=g, lam=lam, **crew_share(n, g, lam)))
    return rows


def crew_table(rows):
    print('\n크루 나눔(기본 보상 400 SUP · 크루원은 일반 · 레어): 크루원 두 번 굴림 · 크루원 한 사람 +SUP · 크루 전체 · 윗선(한 사람 더 온 인원 보너스) · 나쁜 날')
    for r in rows:
        print(f"  {r['n']:2d}명 · {r['g'][:4]} λ{100 * r['lam']:3.0f} | 두 번 굴림 {100 * r['a_mate']:4.1f}% | 한 사람 +{r['mate_gain']:4.1f} SUP({100 * r['mate_gain'] / 400:.1f}%) · "
              f"크루 전체 +{r['crew_gain']:5.1f} | 윗선 {100 * r['bound']:.0f}% | 보상 운 0.85 아래 12.4 → {100 * r['bad']:4.1f}%")


def bad_days(a):
    """두 번 굴림 확률 a 일 때 나쁜 날 비율(보상 운 0.85 아래 · 닳음 운 1.2 넘음)"""
    rng = random.Random(3); n = 200000; rb = wb = 0
    for _ in range(n):
        r = tri(rng, *REWARD_LUCK); w = tri(rng, *WEAR_LUCK)
        if rng.random() < a:
            r = max(r, tri(rng, *REWARD_LUCK)); w = min(w, tri(rng, *WEAR_LUCK))
        rb += r < 0.85; wb += w > 1.2
    return rb / n, wb / n


if __name__ == '__main__':
    V, TD = load()
    rows = run_rows(V, TD)
    table(rows)
    for a in (0.0, 0.26, 0.30, 0.35, 0.40, 1.0):
        rb, wb = bad_days(a)
        print(f'두 번 굴림 {100 * a:.0f}%: 보상 운 0.85 아래 {100 * rb:.1f}% · 닳음 운 1.2 넘음 {100 * wb:.1f}%')
    print('행운 전부 · 달리는 시간 분포 평균으로 새로 찍히는 SUP:', ' · '.join(f'{g} {100 * x:.1f}%' for g, x in issuance(rows).items()))
    crew_table(crew_rows())
    if '--check' in sys.argv:
        check(rows)
