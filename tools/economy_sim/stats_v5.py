#!/usr/bin/env python3
"""능력치 대격변(5판) 모형 — 지금 서버(stats_today.py, 서버와 맞대어 본 셈) 위에 바꾼 규칙만 얹는다. 서버를 부르지 않는다.

바뀌는 것(사용자 2026-11-21): 능력치 넷 · 부작용 없음 · 줄어드는 굽이 · 닳은 뒤 13% · 하루 에너지 Lv1 20분 → 만렙 60분 ·
착화감 = 최종 SUP +%(신발 상태가 좋을수록) · 행운(docs/luck-spec-2026-11-21.md). 나머지(걸음당 SUP · 뽑은 효율 · 뽑은 내구도 ·
레벨마다 포인트 · 수리비 식 · 1분마다 5)는 지금 그대로다.

한계(포인트로 오르는 크기)는 등급마다 하나의 수 B 로 정하고, B 는 "만렙 · 40분 러너의 하루"가 지금 서버와 같아지게 맞춘다(--fit).
설계 · 숫자의 뜻: docs/stat-overhaul-2026-11-21.md

    python3 tools/economy_sim/stats_v5.py           # 표(지금 ↔ 5판)
    python3 tools/economy_sim/stats_v5.py --fit     # 등급마다 B 를 다시 맞춘다
    python3 tools/economy_sim/stats_v5.py --check   # 설계 목표를 벗어나면 1
"""
import math, os, sys

sys.path.insert(0, os.path.dirname(__file__))
import stats_today as T  # noqa: E402
import luck_model  # noqa: E402

SPM = 165
WORN_RATE = 0.13
TAU = 0.20                      # 굽이: 1 − e^(−포인트 ÷ (0.2 × 만렙 포인트)) — 넷 모두(0.3 이면 Lv10 신발의 효율 몰빵이 최선의 85 ~ 94%)
ENERGY_MIN = (20.0, 60.0)       # 포인트 없이 달리기 기준 하루 에너지(분) — Lv1 · 만렙
SHAPE = dict(eff=1.0, comfort=1.0, life=1.0, energy=0.5)   # 한계 = 모양 × B(등급)
COND_FULL = 0.5                 # 착화감: 내구도 50% 이상이면 그대로, 그 아래로 0% 까지 줄어든다
COMFORT_ROLL = {g: (0.025 * T.ORD[g], 0.025 * T.ORD[g] + 0.05) for g in T.GRADES}     # 뽑은 착화감(최종 +%) — 일반 0 ~ 5% … 피니시 12.5 ~ 17.5%
LUCK_ROLL = {'COMMON': (0, 8), 'RARE': (4, 14), 'EPIC': (8, 20), 'LEGENDARY': (12, 26), 'REDLINE': (16, 32), 'FINISH': (20, 40)}
# 행운이 포인트로 다가가는 끝 — 등급마다(다른 능력치처럼 높은 등급일수록 크게). 1.0 이면 기획서 3.2 그대로(모든 등급 100)
LUCK_MAX = {g: 0.40 + 0.12 * T.ORD[g] for g in T.GRADES}     # 일반 40 · 레어 52 · 에픽 64 · 레전더리 76 · 레드라인 88 · 피니시 100
# (모든 등급 100 이면 일반 신발은 행운 몰빵이 정답이 된다 — 상자 40 SUP 가 일반 신발 하루의 절반이라)
# 줍는 것(stat-overhaul 9장): 일반 · 레어 = 상자 조각 5개 = 일반 상자(40 SUP) · 대의 일반 상자 — 신발을 모으는 행운.
# 에픽 이상 = 조각 · 상자 대신 그 신발에 바로 드는 강화 경험치 — 소 300 · 중 600(조각 2) · 대 3,100(레어 한 켤레) ·
#   1,000 경험치 = 24 SUP(서버 forge_xp_sup_per_unit). 만렙 신발이면 가방의 경험치 병(다른 신발에 넣는다).
#   상급 상자는 쓰지 않는다(사용자 "나중에 갈수록 상급상자는 쓸모없을텐데") · 수명 회복도 쓰지 않는다(대의 상자를 가득 수리로 바꾸면 피니시 수리비의 83 ~ 105% 를 메워 소각이 무너진다)
XP_LOOT = {'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'}
XP_SMALL, XP_BIG, SUP_PER_XP = 300, 3100, 24 / 1000
LOOT_OF = lambda g: (dict(shard_value=XP_SMALL * SUP_PER_XP, box_value=XP_BIG * SUP_PER_XP) if g in XP_LOOT
                     else dict(box_sup=40.0, shard_per_box=5, box_shards=None))
# 하루 남는 SUP 를 맞출 때 쓰는 러너 섞음(분: 몫) — 실제 사용 숫자(tools/economy_sim/usage.sql)가 오면 바꾼다
MIX = {20: 0.25, 40: 0.40, 60: 0.25, 90: 0.10}
# 등급마다 두 수 — --fit 이 맞춘 값. 만렙 · 40분 러너(최선 배분)에서
#   B = 포인트로 오르는 한계: "포인트가 하루를 몇 배로 만드는가"(포인트 0 대비)가 지금 서버와 같게
#   K = 뽑은 효율에 곱하는 수: 하루에 남는 SUP 가 지금 서버와 같게(에너지 · 착화감 · 행운 · 부작용 없음으로 바뀐 몫을 되돌린다)
B = {g: 0.0 for g in T.GRADES}
K = {g: 1.0 for g in T.GRADES}
B_FILE = os.path.join(os.path.dirname(__file__), 'stats_v5_fit.txt')
if os.path.exists(B_FILE):
    for line in open(B_FILE, encoding='utf-8'):
        k, b, kk = line.split()
        B[k], K[k] = float(b), float(kk)


def curve(p, g):
    pmax = T.points_total(g, T.MAX_LEVEL[g])
    return 1 - math.exp(-p / (TAU * pmax)) if p > 0 else 0.0


_luck = {}


# 행운이 주는 상자 · 수리의 값 — True 면 그 신발 등급의 값(상자는 등급 상자 = 하루 적립에 비례, 수리는 그 신발의 가득 수리비),
# False 면 기획서 그대로(레어 가운데 신발 기준 고정값 40 · 51.5 SUP — 높은 등급일수록 행운의 값이 작아진다)


def luck_value(lam, energy, run_sup, sup_per_energy, level, repair_sup=51.5, loot=None):
    loot = loot or LOOT_OF('RARE')
    key = (round(lam * 20) / 20, round(energy * 2) / 2, round(math.log(max(run_sup, 1)) * 20) / 20, round(sup_per_energy, 0), level,
           round(repair_sup), tuple(sorted((k, v) for k, v in loot.items() if v is not None)))
    if key not in _luck:
        rs = math.exp(key[2])
        o = luck_model.simulate(key[0], max(key[1], 0.1), rs, days=1200, level=level)
        _luck[key] = luck_model.value(o, rs, key[1], key[3], repair_sup=repair_sup, **loot)['합'] / rs
    return _luck[key] * run_sup


def shoe(g, q=0.5, k=None):
    lo, hi = COMFORT_ROLL[g]
    llo, lhi = LUCK_ROLL[g]
    return dict(eff=(1 + T.stat_efficiency(g, q) / 10000.0) * (K[g] if k is None else k), dur=T.stat_durability(g, q), comfort=lo + (hi - lo) * q,
                luck=(llo + (lhi - llo) * q) / 100.0)


def day(g, level, minutes, alloc, b=None, with_luck=True, spm=SPM, detail=False, k=None):
    """하루 한 번 [minutes] 분 — alloc = (효율, 내구도, 착화감, 행운) 포인트. 날마다 가득 수리(지금 셈과 같은 수리비 식)"""
    b = B[g] if b is None else b
    s = shoe(g, k=k)
    pe, pd, pc, pl = alloc
    eff = s['eff'] * (1 + SHAPE['eff'] * b * curve(pe, g))
    dmax = s['dur'] * (1 + SHAPE['life'] * b * curve(pd, g))
    spe_mult = 1 + SHAPE['energy'] * b * curve(pd, g)
    comfort = s['comfort'] + SHAPE['comfort'] * b * curve(pc, g)
    lam = s['luck'] + max(LUCK_MAX[g] - s['luck'], 0) * curve(pl, g)
    lmax = T.MAX_LEVEL[g]
    e_min = ENERGY_MIN[0] + (ENERGY_MIN[1] - ENERGY_MIN[0]) * (level - 1) / max(lmax - 1, 1)
    energy_steps = e_min * SPM * spe_mult                     # 하루 에너지로 적립하는 걸음(달리기 걸음 기준)
    n = minutes * spm
    can = min(n, energy_steps)
    life_steps = dmax * 12 / 60 * spm
    full = min(can, life_steps)
    worn = can - full
    pps = T.PPS * eff
    base = (full + worn * WORN_RATE) * pps
    # 착화감 × 신발 상태 — 가득에서 시작해 full 동안 고르게 닳는다(상태 1 → 1 − full/life). 닳은 구간은 0
    s0, s1 = 1.0, 1 - full / life_steps if life_steps > 0 else 0.0
    def cond_int(a, c):  # ∫ min(1, x / COND_FULL) dx, x 가 a → c 로 고르게 내려갈 때의 평균 × 걸음
        if a <= c: return 0.0
        hi = max(min(a, 1.0), COND_FULL); lo = max(c, COND_FULL)
        part1 = max(hi - lo, 0) if a > COND_FULL else 0            # 50% 위 — 그대로
        top = min(a, COND_FULL); bot = min(c, COND_FULL)
        part2 = (top ** 2 - bot ** 2) / (2 * COND_FULL) if top > bot else 0   # 50% 아래 — 줄어듦
        return (part1 + part2) / (a - c)
    cond = cond_int(s0, s1) if full > 0 else 0.0
    comfort_gain = full * pps * comfort * cond
    gross = base + comfort_gain
    full_repair = T.REPAIR_SHARE * 110 * T.PPS / 5 * s['eff'] * s['dur']                   # 가득 수리비 — 포인트와 상관없다
    repair = min(dmax, 5 * minutes * can / n) * full_repair / dmax
    luck = luck_value(lam, can / (T.STEPS_PER_ENERGY * spe_mult), gross, T.STEPS_PER_ENERGY * spe_mult * pps, level,
                      repair_sup=full_repair, loot=LOOT_OF(g)) if with_luck else 0.0   # 수리권 = 그 신발 가득 수리(아이템이 하는 일 그대로)
    net = gross + luck - repair
    if detail:
        return dict(net=net, base=base, comfort=comfort_gain, luck=luck, repair=repair, eff=eff, dmax=dmax, comfort_pct=comfort,
                    cond=cond, lam=lam, energy_min=energy_steps / SPM, life_min=dmax / 5, rewarded_min=can / spm)
    return net


def day_crew(g, level, minutes, alloc, people=4, crew_level=1, b=None, k=None):
    """크루 러닝 한 번 — 모두 같은 신발 · 같은 배분이면 8 : 2 는 내 신발과 같다. 개인 에너지 · 신발 닳음 · 수리 없음,
    공용 체력 = 모두의 최대 내구도 × (1 + 6% × (크루 Lv − 1)), 달리는 사람마다 1분에 5 — 비면 적립 0. 착화감은 신발이 닳지 않으니 그대로.
    보상 = B × (1 + 인원 + 레벨). 크루 행운은 늘 발동이라 능력치 값에 넣지 않는다(기획서 7.5)"""
    b = B[g] if b is None else b
    s = shoe(g, k=k)
    pe, pd, pc, pl = alloc
    eff = s['eff'] * (1 + SHAPE['eff'] * b * curve(pe, g))
    dmax = s['dur'] * (1 + SHAPE['life'] * b * curve(pd, g))
    comfort = s['comfort'] + SHAPE['comfort'] * b * curve(pc, g)
    pool_min = people * dmax * (1 + 0.06 * (crew_level - 1)) / (5 * people)
    earn_min = min(minutes, pool_min)
    head = {1: 0, 2: 0.05, 3: 0.10, 4: 0.15, 5: 0.20}.get(people, 0.5)
    base = earn_min * SPM * T.PPS * eff * (1 + comfort)
    return base * (1 + head + 0.01 * (crew_level - 1))


def allocations(total, parts=12):
    step = max(total // parts, 1)
    n = total // step
    rest = total - n * step
    for a in range(n + 1):
        for b_ in range(n + 1 - a):
            for c in range(n + 1 - a - b_):
                d = n - a - b_ - c
                yield (a * step + rest, b_ * step, c * step, d * step)


def best(g, level, minutes, b=None, parts=12, k=None):
    tot = T.points_total(g, level)
    if tot == 0:
        return day(g, level, minutes, (0, 0, 0, 0), b, k=k), (0, 0, 0, 0)
    return max((day(g, level, minutes, al, b, k=k), al) for al in allocations(tot, parts))


def today_best(g, level, minutes):
    tot = T.points_total(g, level)
    st = max(tot // 24, 1)
    top = None
    for pe in range(0, tot + 1, st):
        for pd in range(0, tot - pe + 1, st):
            v = T.day_net(g, level, minutes, pe, tot - pe - pd, pd)
            if top is None or v > top[0]:
                top = (v, (pe, pd, tot - pe - pd))
    return top


def bisect(fn, lo, hi, target, n=18):
    for _ in range(n):
        mid = (lo + hi) / 2
        if fn(mid) < target: lo = mid
        else: hi = mid
    return (lo + hi) / 2


def fit():
    for g in T.GRADES:
        lv = T.MAX_LEVEL[g]
        mix = lambda fn: sum(w * fn(m) for m, w in MIX.items()) / sum(MIX.values())
        t_best = mix(lambda m: today_best(g, lv, m)[0])
        t_ratio = t_best / mix(lambda m: today_best(g, 1, m)[0])          # 지금: 포인트가 하루를 몇 배로(Lv1 은 포인트 0 · 에너지는 같다)
        b, k = 0.5, 1.0
        for _ in range(3):
            b = bisect(lambda x: mix(lambda m: best(g, lv, m, x, 8, k)[0]) / mix(lambda m: day(g, lv, m, (0, 0, 0, 0), x, k=k)), 0.0, 6.0, t_ratio)
            k = bisect(lambda x: mix(lambda m: best(g, lv, m, b, 8, x)[0]), 0.2, 2.0, t_best)
        B[g], K[g] = round(b, 3), round(k, 3)
        print(f'{g}: B {B[g]} · K {K[g]} (지금 만렙 섞음: 최선 {t_best:.1f} · 포인트 ×{t_ratio:.2f})')
    with open(B_FILE, 'w', encoding='utf-8') as f:
        for g in T.GRADES:
            f.write(f'{g} {B[g]} {K[g]}\n')


RUNNERS = (20, 40, 60, 90)
STATS = ('효율', '내구도', '착화감', '행운')


def table():
    rows = []
    for g in T.GRADES:
        for lv in (1, 10, T.MAX_LEVEL[g]):
            for mi in RUNNERS:
                tot = T.points_total(g, lv)
                tb = today_best(g, lv, mi)
                vb = best(g, lv, mi)
                alls = {s: day(g, lv, mi, tuple(tot if i == k else 0 for i in range(4))) for k, s in enumerate(STATS)}
                even = day(g, lv, mi, (tot - 3 * (tot // 4), tot // 4, tot // 4, tot // 4))
                rows.append(dict(g=g, lv=lv, mi=mi, today=tb[0], v5=vb[0], al=vb[1], alls=alls, even=even, tot=tot))
    return rows


def show(rows):
    print('등급 Lv  분 | 지금 최선 → 5판 최선(효/내/착/행) | 효율만 · 내구도만 · 착화감만 · 행운만 · 고르게 (최선 대비 %)')
    for r in rows:
        a = r['alls']
        pct = lambda x: f'{100 * x / r["v5"]:3.0f}' if r['v5'] > 0 else '  -'
        print(f"{T.ORD[r['g']]}{r['g'][:4]} {r['lv']:2d} {r['mi']:2d} | {r['today']:7.1f} → {r['v5']:7.1f} ({'/'.join(map(str, r['al']))}) {100 * r['v5'] / r['today'] - 100:+4.0f}% | "
              f"{pct(a['효율'])} · {pct(a['내구도'])} · {pct(a['착화감'])} · {pct(a['행운'])} · {pct(r['even'])}")


def check(rows):
    """설계 목표 — docs/stat-overhaul-2026-11-21.md 3장"""
    bad = []
    for r in rows:
        name = f"{r['g']} Lv{r['lv']} {r['mi']}분"
        top_level = r['lv'] == T.MAX_LEVEL[r['g']]
        if r['tot'] < 20:
            continue                                   # 포인트가 적으면(일반 · 레어 Lv1 ~ 9) 굽이가 아직 곧다
        e = r['alls']['효율'] / r['v5']
        if e > (0.85 if top_level else 0.93): bad.append(f'{name}: 효율만 = 최선의 {100 * e:.0f}%')
        for st in ('내구도', '착화감', '행운'):
            if r['alls'][st] / r['v5'] > 0.95: bad.append(f'{name}: {st}만 = 최선의 {100 * r["alls"][st] / r["v5"]:.0f}%(몰빵이 정답)')
        if r['even'] < 0.78 * r['v5']: bad.append(f'{name}: 고르게 = 최선의 {100 * r["even"] / r["v5"]:.0f}%(78% 아래)')
    for g in T.GRADES:
        lv = T.MAX_LEVEL[g]
        mine = [r for r in rows if r['g'] == g and r['lv'] == lv]
        t = sum(MIX.get(r['mi'], 0) * r['today'] for r in mine); v = sum(MIX.get(r['mi'], 0) * r['v5'] for r in mine)
        if abs(v / t - 1) > 0.03: bad.append(f'{g} 만렙: 섞음 하루가 지금과 {100 * (v / t - 1):+.1f}%(맞춘 값에서 벗어남 — --fit)')
        short = next(r for r in mine if r['mi'] == 20); long_ = next(r for r in mine if r['mi'] == 90)
        if long_['al'][1] / long_['tot'] < short['al'][1] / short['tot'] + 0.10:
            bad.append(f'{g} 만렙: 오래 달리는 사람의 최선에 내구도가 더 들지 않는다(20분 {short["al"]} · 90분 {long_["al"]})')
        if any(r['al'][0] == r['tot'] for r in mine): bad.append(f'{g} 만렙: 최선이 효율 몰빵인 러너가 있다')
        if not any(r['al'][3] > 0 for r in mine): bad.append(f'{g} 만렙: 어느 러너도 행운에 포인트를 넣지 않는다')
    if bad:
        print('\n'.join(bad)); sys.exit(1)
    print('5판 설계 목표 — 모두 안쪽')


if __name__ == '__main__':
    if '--fit' in sys.argv:
        fit()
    rows = table()
    show(rows)
    if '--check' in sys.argv:
        check(rows)
