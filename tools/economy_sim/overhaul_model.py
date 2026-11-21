#!/usr/bin/env python3
"""능력치 대격변 설계 모형(초안) — 네 능력치(효율 · 내구도 · 착화감 · 행운)를 한 모형에서 맞춘다. 서버를 부르지 않는다.

정해진 것(2026-11-21 사용자): 능력치에 부작용 없음 · 한 능력치에 많이 넣을수록 포인트 하나의 효과가 준다 · 효율이 늘 정답이면 안 된다 ·
닳은 뒤 적립 11 ~ 15% · 하루 에너지는 레벨에 따라(Lv1 약 20분 → 만렙 약 60분) · 착화감 = 최종 SUP +%(신발 상태가 좋을수록 · 상한) ·
내구도 = 신발 + 에너지가 오래 감 + 수리비 · 행운 = docs/luck-spec-2026-11-21.md.

이 파일의 숫자(G · 착화감 뽑은 값 · 상한 · 상태 계수)는 **초안**이다 — 사용자와 하나씩 정한 뒤 굳힌다(docs/stat-overhaul-2026-11-21.md).

    python3 tools/economy_sim/overhaul_model.py            # 표: 러너 유형 × 등급마다 몰빵 넷 · 고르게 · 최선
    python3 tools/economy_sim/overhaul_model.py --check    # 설계 목표(효율 몰빵은 최선의 90% 아래 · 고르게도 85% 넘게 · 시간에 따라 1등이 바뀜)를 벗어나면 1
"""
import itertools, math, os, sys

sys.path.insert(0, os.path.dirname(__file__))
import luck_model  # noqa: E402

PPS = 0.0118          # 걸음당 SUP(서버 points_per_step)
SPM = 165             # 달리기 분당 걸음
WEAR_PER_MIN = 5      # 1분마다 닳는 내구도
SEC_PER_POINT = 12    # 내구도 1 = 12초
REPAIR_SHARE = 0.45   # 가득 수리비 = 그 내구도로 버는 기본 SUP 의 45%
WORN_RATE = 0.13      # 다 닳은 뒤 적립(11 ~ 15% 의 가운데)
STEPS_PER_ENERGY = 600

# ── 초안 손잡이 ───────────────────────────────────────────────────────────
D = dict(
    tau=0.30,          # 굽이: 효과 = 한계 × (1 − e^(−포인트 ÷ τ)), τ = 그 등급 만렙 포인트의 30%(행운과 같은 굽이)
    G_eff=0.5,         # 효율: 뽑은 효율 지수 × (1 + 0.5 × 굽이) — 다 넣으면 약 +48%(1.0 이면 효율 몰빵이 짧은 러너에게 최선의 87 ~ 98%)
    G_dur=1.0,         # 내구도: 최대 내구도 × (1 + 1.0 × 굽이)
    G_dur_energy=0.5,  # 내구도: 에너지 1 로 걷는 걸음 × (1 + 0.5 × 굽이)
    comfort_cap=0.50,  # 착화감: 최종 SUP +% 의 상한(50% — 30% 면 착화감이 효율의 1/5 값이라 아무도 넣지 않는다)
    cond_floor=0.5,    # 착화감 × (0.5 + 0.5 × 신발 상태) — 다 닳은 신발은 절반만
    energy_lv1=3300,   # 하루 에너지(걸음) Lv1 = 달리기 20분
    energy_max=9900,   # 만렙 = 달리기 60분
)
# 등급: 효율 지수(가운데) · 최대 내구도(가운데) · 착화감 뽑은 값(%, 초안 가운데) · 행운 뽑은 값 · 레벨마다 포인트 · 5레벨 보너스 · 만렙
GRADES = {
    'COMMON':    (140.0, 155.0, 3.0, 0.04, 2, 3, 20),
    'RARE':      (210.0, 207.5, 6.0, 0.09, 3, 5, 20),
    'EPIC':      (315.0, 280.0, 8.5, 0.14, 4, 6, 26),
    'LEGENDARY': (472.5, 377.5, 11.0, 0.19, 5, 8, 27),
    'REDLINE':   (708.8, 512.5, 13.5, 0.24, 6, 9, 28),
    'FINISH':    (1063.1, 692.5, 17.0, 0.30, 8, 12, 30),
}
NAMES = {'COMMON': '일반', 'RARE': '레어', 'EPIC': '에픽', 'LEGENDARY': '레전더리', 'REDLINE': '레드라인', 'FINISH': '피니시'}
STATS = ('효율', '내구도', '착화감', '행운')


def points_at(g, level):
    _, _, _, _, ppl, ms, _ = GRADES[g]
    return (level - 1) * ppl + (level // 5) * ms


def curve(p, pmax, d=D):
    return 1 - math.exp(-p / (d['tau'] * pmax)) if p > 0 else 0.0


_luck_cache = {}


def luck_value(lam, energy, run_sup, sup_per_energy, level):
    key = (round(lam, 2), round(energy, 1), round(run_sup, 0), round(sup_per_energy, 1), level)
    if key not in _luck_cache:
        o = luck_model.simulate(key[0], max(key[1], 0.1), max(key[2], 1.0), days=2500, level=level)
        _luck_cache[key] = luck_model.value(o, key[2], key[1], key[3])['합']
    return _luck_cache[key]


def day(g, level, minutes, alloc, d=D, with_luck=True):
    """하루 한 번 [minutes] 분 달리는 사람 — alloc = 효율 · 내구도 · 착화감 · 행운 포인트. 돌려주는 값: 하루에 남는 SUP 와 내역"""
    e0, d0, c0, l0, _, _, lmax = GRADES[g]
    pmax = points_at(g, lmax)
    pe, pd, pc, pl = alloc
    eff = e0 * (1 + d['G_eff'] * curve(pe, pmax, d))
    dur = d0 * (1 + d['G_dur'] * curve(pd, pmax, d))
    steps_per_energy = STEPS_PER_ENERGY * (1 + d['G_dur_energy'] * curve(pd, pmax, d))
    comfort = c0 / 100 + (d['comfort_cap'] - c0 / 100) * curve(pc, pmax, d)
    lam = l0 + (1 - l0) * curve(pl, pmax, d)
    energy_steps = (d['energy_lv1'] + (d['energy_max'] - d['energy_lv1']) * (level - 1) / max(lmax - 1, 1)) * steps_per_energy / STEPS_PER_ENERGY
    rew_min = min(minutes, energy_steps / SPM)                 # 에너지 안에서 적립하는 분
    life_min = dur * SEC_PER_POINT / 60                         # 가득에서 달리는 분
    full_min = min(rew_min, life_min)
    worn_min = rew_min - full_min
    per_step = PPS * eff / 100
    # 신발 상태: 가득에서 시작해 full_min 동안 고르게 닳는다(날마다 가득 수리) — 닳은 구간은 0
    avg_state = ((1 - full_min / life_min / 2) * full_min) / rew_min if rew_min > 0 else 1
    cond = d['cond_floor'] + (1 - d['cond_floor']) * avg_state
    base = (full_min + worn_min * WORN_RATE) * SPM * per_step
    gross = base * (1 + comfort * cond)
    full_repair = REPAIR_SHARE * d0 * SEC_PER_POINT / 60 * SPM * PPS * e0 / 100   # 가득 수리비 — 포인트와 상관없다
    repair = full_repair * (full_min * WEAR_PER_MIN) / dur
    luck = 0.0
    if with_luck:
        energy_used = rew_min * SPM / steps_per_energy
        luck = luck_value(lam, energy_used, gross, steps_per_energy * per_step, level)
    return dict(net=gross + luck - repair, gross=gross, luck=luck, repair=repair, rew_min=rew_min, life_min=life_min,
                eff=eff, dur=dur, comfort=comfort, lam=lam, cond=cond)


def allocations(total, step):
    n = total // step
    for a in range(n + 1):
        for b in range(n + 1 - a):
            for c in range(n + 1 - a - b):
                e = n - a - b - c
                yield (e * step, a * step, b * step, c * step)
    # 나머지 포인트는 효율에(격자 밖) — 비교는 같은 합으로


def best(g, level, minutes, d=D):
    total = points_at(g, level)
    step = max(total // 10, 1)
    rows = []
    for al in allocations(total - total % step, step):
        al = (al[0] + total % step,) + al[1:]
        rows.append((day(g, level, minutes, al, d)['net'], al))
    rows.sort(reverse=True)
    return rows[0]


RUNNERS = [(20, '가볍게 20분'), (40, '보통 40분'), (60, '1시간'), (90, '오래 90분')]
SHOES = [('RARE', 20), ('EPIC', 26), ('LEGENDARY', 27), ('FINISH', 30), ('RARE', 10)]


def table(d=D):
    out = []
    for g, lv in SHOES:
        total = points_at(g, lv)
        print(f'── {NAMES[g]} Lv{lv} · 포인트 {total} ──')
        for minutes, name in RUNNERS:
            alls = {s: day(g, lv, minutes, tuple(total if i == k else 0 for i in range(4)), d)['net'] for k, s in enumerate(STATS)}
            even = day(g, lv, minutes, tuple(total // 4 + (total % 4 if i == 0 else 0) for i in range(4)), d)['net']
            b_net, b_al = best(g, lv, minutes, d)
            top = max(alls, key=alls.get)
            line = ' · '.join(f'{s}만 {alls[s]:6.1f}' for s in STATS)
            print(f'  {name:9s} | {line} · 고르게 {even:6.1f} | 최선 {b_net:6.1f} ({"/".join(str(x) for x in b_al)}) '
                  f'| 효율만 = 최선의 {100 * alls["효율"] / b_net:4.0f}% · 몰빵 중 1등 {top}')
            out.append((g, lv, minutes, alls, even, b_net, b_al))
        print()
    return out


def check(rows):
    bad = []
    tops = set()
    for g, lv, minutes, alls, even, b_net, b_al in rows:
        r = alls['효율'] / b_net
        if r > 0.90:
            bad.append(f'{NAMES[g]} Lv{lv} {minutes}분: 효율만 = 최선의 {100 * r:.0f}%(90% 넘음 — 효율 몰빵이 거의 정답이 된다)')
        if b_al[0] == sum(b_al):
            bad.append(f'{NAMES[g]} Lv{lv} {minutes}분: 최선이 효율 몰빵')
        tops.add(max(alls, key=alls.get))
        if even < 0.85 * b_net:
            bad.append(f'{NAMES[g]} Lv{lv} {minutes}분: 고르게가 최선의 {100 * even / b_net:.0f}%(85% 아래 — 모르고 나눠도 크게 손해 보지 않게)')
    if len(tops) < 2:
        bad.append(f'몰빵 가운데 1등이 늘 {tops} — 달리는 시간에 따라 답이 달라야 한다')
    if bad:
        print('\n'.join(bad)); sys.exit(1)
    print('대격변 설계 목표(초안) — 모두 안쪽')


if __name__ == '__main__':
    rows = table()
    if '--check' in sys.argv:
        check(rows)
