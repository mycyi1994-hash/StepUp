#!/usr/bin/env python3
"""지금 서버(0119)의 하루 셈을 그대로 옮긴 것 — `economy.points_day_net`(supabase/migrations/0119_economy_roots.sql)과 같은 식.

대격변 모형(stats_v5.py)의 기준선이다. 서버와 맞는지는 `tools/together_sim/17-stats-baseline.sql` 이 낸 값과 맞대어 본다:

    bash tools/together_sim/run.sh tools/together_sim/17-stats-baseline.sql | python3 tools/economy_sim/stats_today.py --verify
"""
import math, sys

PPS = 0.0118
STEPS_PER_ENERGY = 600
ENERGY = 10.0            # 하루 에너지(energy_per_level 0)
WORN_RATE = 0.30
REPAIR_SHARE = 0.45
COMFORT_CAP = 7000
ORD = {'COMMON': 0, 'RARE': 1, 'EPIC': 2, 'LEGENDARY': 3, 'REDLINE': 4, 'FINISH': 5}
EFF_RANGE = {'COMMON': (0, 8000), 'RARE': (5000, 17000), 'EPIC': (12500, 30500), 'LEGENDARY': (23750, 50750),
             'REDLINE': (40625, 81125), 'FINISH': (65938, 126688)}
DUR_RANGE = {'COMMON': (110, 200), 'RARE': (150, 265), 'EPIC': (200, 360), 'LEGENDARY': (270, 485),
             'REDLINE': (365, 660), 'FINISH': (495, 890)}
PPL = {'COMMON': 2, 'RARE': 3, 'EPIC': 4, 'LEGENDARY': 5, 'REDLINE': 6, 'FINISH': 8}
MILESTONE = {'COMMON': 3, 'RARE': 5, 'EPIC': 6, 'LEGENDARY': 8, 'REDLINE': 9, 'FINISH': 12}
RATE = {'COMMON': 100, 'RARE': 110, 'EPIC': 125, 'LEGENDARY': 140, 'REDLINE': 160, 'FINISH': 180}
MAX_LEVEL = {'COMMON': 20, 'RARE': 20, 'EPIC': 26, 'LEGENDARY': 27, 'REDLINE': 28, 'FINISH': 30}
GRADES = list(ORD)


def pg_round(x):
    """Postgres round(numeric) — 0.5 는 0 에서 먼 쪽으로"""
    return int(math.floor(abs(x) + 0.5)) * (1 if x >= 0 else -1)


def stat_efficiency(g, q):
    lo, hi = EFF_RANGE[g]
    return lo + pg_round((hi - lo) * min(max(q, 0), 1))


def comfort_index_lo(g):
    return (140 + 10 * ORD[g]) / 1.4


def stat_comfort(g, q):
    return pg_round(10000 * (1 - 100 / (comfort_index_lo(g) * (1 + 0.8 * min(max(q, 0), 1)))))


def stat_durability(g, q):
    lo, hi = DUR_RANGE[g]
    return 5 * pg_round((lo + (hi - lo) * min(max(q, 0), 1)) / 5.0)


def points_total(g, level):
    return PPL[g] * (max(level, 1) - 1) + MILESTONE[g] * (max(level, 1) // 5)


def comfort_with_points(c, g, pts):
    return 10000 - pg_round((10000 - c) / (1 + RATE[g] * pts / 10000.0))


def dur_max_of(base, g, pts):
    return base + pg_round(base * RATE[g] * pts / 10000.0)


def day_net(g, level, minutes, pe, pc, pd, q=0.5, spm=165, detail=False):
    """하루 한 번 [minutes] 분 · 분당 [spm] 걸음, 날마다 가득 수리 — points_day_net 과 같은 셈(운 1 · 보너스 없음)"""
    eff_bps, comfort_bps, dur_base = stat_efficiency(g, q), stat_comfort(g, q), stat_durability(g, q)
    rate = RATE[g] / 10000.0
    u = 1 + rate * pe
    v = 1 + rate * pd
    wm = em = u                                   # eff_wear_exp · eff_energy_exp = 1
    comfort = min(comfort_with_points(comfort_bps, g, pc), COMFORT_CAP) / 10000.0
    dmax = dur_max_of(dur_base, g, pd)
    pps = PPS * (10000 + eff_bps) / 10000.0 * u
    unit = REPAIR_SHARE * 110 * PPS / 5 * (10000 + eff_bps) / 10000.0 * u / wm / v
    e = (1 - comfort) / STEPS_PER_ENERGY * em
    n, t = int(minutes * spm), int(minutes * 60)
    life = math.floor(n * dmax * 12 / wm / t) if dmax * 12 / wm < t else n
    want = n                                      # worn_rate > 0 — 닳은 뒤 걸음도 센다
    can = min(want, max(math.floor(ENERGY / e), 0))
    full = min(can, life)
    tail = can - full
    earned = (full + tail * WORN_RATE) * pps
    repair = min(dmax, 5 * math.floor(t / 60.0) * wm * can / max(n, 1)) * unit
    net = round(earned - repair, 2)
    if detail:
        return dict(net=net, earned=earned, repair=repair, steps=can, full=full, worn=tail, life_min=dmax / 5 / wm,
                    energy_min=ENERGY / e / spm)
    return net


def verify(lines):
    bad = n = 0
    worst = 0.0
    for line in lines:
        if not line.startswith('BASE|'):
            continue
        _, g, lv, mi, pe, pc, pd, net = line.strip().split('|')
        mine = day_net(g, int(lv), int(mi), int(pe), int(pc), int(pd))
        n += 1
        d = abs(mine - float(net))
        worst = max(worst, d)
        if d > 0.1:            # 숫자 자리 차이(numeric · float) — 744줄 가운데 6줄이 0.06 SUP 안쪽
            bad += 1
            if bad <= 10:
                print(f'다름: {g} Lv{lv} {mi}분 {pe}/{pc}/{pd} — 서버 {net} · 모형 {mine}')
    print(f'지금 서버 셈 맞대기 — {n}줄 · 다른 줄 {bad} · 가장 큰 차이 {worst:.3f} SUP')
    return bad == 0 and n > 0


if __name__ == '__main__':
    if '--verify' in sys.argv:
        sys.exit(0 if verify(sys.stdin) else 1)
