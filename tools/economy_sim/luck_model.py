#!/usr/bin/env python3
"""행운 능력치 설계 모형 — docs/luck-spec-2026-11-21.md 의 표를 낸다(서버를 부르지 않는다. 몬테카를로).

행운 수치 λ(0 ~ 100) 하나가 세 순간(출발 · 달리는 중 에너지 5마다 · 보상)과 늘 적용되는 것(손상 · 대성공)의 확률을 정한다.
흐름(순간들이 이어짐) · 등급과 연쇄 · 아깝다 게이지 · 클로버 · 꾸준함까지 넣어 하루 기대값을 잰다.

    python3 tools/economy_sim/luck_model.py            # 표
    python3 tools/economy_sim/luck_model.py --check    # 설계 목표(기획서 9장)를 벗어나면 1
"""
import math, random, sys

# ── 설정값(기획서 12장의 손잡이와 같은 이름) ───────────────────────────────
P = dict(
    luck_start=(0.03, 0.22), luck_start_cap=0.50,          # 출발: 3% + 22% × Λ
    luck_find=(0.06, 0.30), luck_find_cap=0.70,            # 줍기(에너지 5마다): 6% + 30% × Λ
    luck_reward=(0.015, 0.09), luck_reward_cap=0.25,       # 보상(퍼센트 · 고정 따로 한 번씩): 1.5% + 9% × Λ
    luck_reward_bonus=(0.10, 0.30),                        # 퍼센트 행운: +10 ~ 30%(작은 쪽이 자주 — 평균 17%). 30% 는 드물다
    luck_reward_flat=(8.0, 1.2),                           # 고정 행운: 8 + 1.2 × 신발 레벨 SUP(퍼센트 뒤에 그대로 더한다)
    luck_flat_cap=0.10,                                    # 고정 SUP 는 그 러닝이 번 SUP 의 10% 를 넘지 않는다
    luck_energy_step=5,                                    # 에너지를 이만큼 쓸 때마다 한 번
    flow_start_find=1.5, flow_find_reward=1.5, flow_find_need=2, flow_afterglow=2.0,   # 흐름
    streak_step=0.03, streak_max=7,                        # 꾸준함: 연속 하루마다 × (1 + 0.03), 7일까지
    pity_step=0.04, pity_sure=10,                          # 아깝다: 실패마다 +4%p, 10번 실패하면 다음은 확정
    tier_small=(0.70, -0.20), tier_mid=(0.25, 0.12), tier_big=(0.05, 0.08),          # 등급 비율 = a + b × Λ
    find_daily_cap=6, clover_cap=0,                        # 클로버는 아직 넣지 않는다(사용자 2026-11-21) — 0 이면 꺼짐
    damage_cut=0.5, crit_add=0.10,
    clover_bad_run=0.25, clover_reward=0.30,               # 불운 보상 · 보상 행운 때 클로버
    start_energy_cut=0.30, start_wear_cut=0.50,            # 출발 버프
)
# 등급별 줍는 것(비율) — 조각 5개 = 일반 상자 1
LOOT = {
    'S': [('shard', 1, 1.00)],
    'M': [('shard', 2, 0.55), ('drink', 1, 0.28), ('booster', 1, 0.17)],
    'B': [('box', 1, 0.60), ('repair', 1, 0.25), ('double', 1, 0.10), ('refill', 1, 0.05)],
}
REWARD_LUCK = (0.7, 1.0, 1.3)        # 보상 운(모든 러닝 — 지금 서버 값)
BOX_SUP = 40.0                       # 일반 상자 상점 값

def tri(rng, lo, mode, hi): return rng.triangular(lo, hi, mode)
def prob(pair, lam): return pair[0] + pair[1] * lam

def simulate(lam, energy_per_day, run_sup, days=20000, seed=7, streak=7, repair_full=51.5, energy_day_sup=None,
             runs_per_day=1, level=10, p=P):
    """lam = Λ(0 ~ 1) · energy_per_day = 하루에 쓰는 에너지 · run_sup = 하루 러닝의 기본 SUP(운 전)."""
    rng = random.Random(seed)
    k = 1 + p['streak_step'] * min(streak, p['streak_max'])
    carry = 0.0; pity = 0; afterglow = False; clover = 0
    out = dict(start=0, finds=0, S=0, M=0, B=0, reward=0, shard=0, box=0, drink=0, booster=0, repair=0, double=0, refill=0,
               clover_get=0, clover_lost=0, sup_bonus=0.0, sup_flat=0.0, reward_flat=0, wear_saved=0.0, energy_saved=0.0, floor_gain=0.0, days=days)
    for _ in range(days):
        finds_today = 0
        for _r in range(runs_per_day):
            e_run = energy_per_day / runs_per_day
            sup = run_sup / runs_per_day
            # ① 출발
            ps = min(prob(p['luck_start'], lam) * k * (p['flow_afterglow'] if afterglow else 1), p['luck_start_cap'])
            afterglow = False
            started = rng.random() < ps
            buff = rng.choice(('energy', 'wear', 'floor')) if started else None
            if started: out['start'] += 1
            luck_roll = tri(rng, *REWARD_LUCK)
            if buff == 'floor' and luck_roll < 1.0:
                out['floor_gain'] += (1.0 - luck_roll) * sup; luck_roll = 1.0
            if buff == 'wear': out['wear_saved'] += p['start_wear_cut'] * min(e_run / 10.0, 1.0) * repair_full
            if buff == 'energy': out['energy_saved'] += p['start_energy_cut'] * e_run
            # ② 달리는 중 — 에너지 5마다
            carry += e_run; chain = 0; finds_run = 0
            while carry >= p['luck_energy_step'] and finds_today < p['find_daily_cap']:
                carry -= p['luck_energy_step']
                pf = prob(p['luck_find'], lam) * k * (p['flow_start_find'] if started else 1) + p['pity_step'] * pity
                pf = 1.0 if pity >= p['pity_sure'] else min(pf, p['luck_find_cap'])
                if rng.random() < pf:
                    pity = 0; finds_run += 1; finds_today += 1; out['finds'] += 1
                    w = [max(prob(p['tier_small'], lam), 0), prob(p['tier_mid'], lam), prob(p['tier_big'], lam)]
                    t = rng.choices(['S', 'M', 'B'], w)[0]
                    if chain >= 1: t = {'S': 'M', 'M': 'B', 'B': 'B'}[t]          # 연쇄: 연달아 주우면 한 등급 위
                    chain += 1; out[t] += 1
                    name, n, _ = rng.choices(LOOT[t], [x[2] for x in LOOT[t]])[0]
                    if name == 'clover':
                        got = min(n, p['clover_cap'] - clover); clover += got; out['clover_get'] += got; out['clover_lost'] += n - got
                    else: out[name] += n
                else:
                    pity += 1; chain = 0
            # ③ 보상
            pr = min(prob(p['luck_reward'], lam) * k * (p['flow_find_reward'] if finds_run >= p['flow_find_need'] else 1), p['luck_reward_cap'])
            # 퍼센트 행운 · 고정 행운을 따로 한 번씩 굴린다(둘 다 터질 수도 있다). 고정은 퍼센트를 다 계산한 뒤에 그대로 더한다
            if rng.random() < pr:
                size = p['luck_reward_bonus'][0] + (p['luck_reward_bonus'][1] - p['luck_reward_bonus'][0]) * rng.random() ** 2
                out['reward'] += 1; out['sup_bonus'] += size * sup * luck_roll; afterglow = True
            if rng.random() < pr:
                out['reward_flat'] += 1; afterglow = True
                out['sup_flat'] += min(p['luck_reward_flat'][0] + p['luck_reward_flat'][1] * level, p['luck_flat_cap'] * sup * luck_roll)
            # 클로버는 모이면 쓴다고 본다(강화 대성공 확정 3개)
            if clover >= 3: clover -= 3
    return out

def value(o, run_sup, e_day, sup_per_energy, box_sup=None, repair_sup=51.5, shard_per_box=5, box_shards=None):
    """하루 기대값(SUP 로 친 값) — 아이템은 그 사람이 한 번 달려 버는 SUP 로 값을 매긴다.
    box_sup · repair_sup — 상자 · 수리 한 번의 값(기본 = 레어 가운데 신발. 대격변 모형은 그 신발 등급의 값을 넘긴다)
    shard_per_box — 조각 몇 개가 상자 하나인가 · box_shards — "대"의 상자 대신 주는 조각 수(None 이면 상자 그대로) — 에픽 이상의 상급 조각(stat-overhaul 7장)"""
    BOX_SUP_ = BOX_SUP if box_sup is None else box_sup
    d = o['days']
    boxes = (o['shard'] + (o['box'] * box_shards if box_shards else 0)) / shard_per_box + (0 if box_shards else o['box'])
    v = dict(
        보상행운=(o['sup_bonus'] + o['sup_flat']) / d,
        출발버프=(o['floor_gain'] + o['wear_saved'] + o['energy_saved'] * sup_per_energy * 0.5) / d,
        상자=boxes * BOX_SUP_ / d,
        아이템=(o['drink'] * 0.25 * run_sup + o['booster'] * 0.5 * run_sup + o['double'] * 1.0 * run_sup
               + o['repair'] * repair_sup + o['refill'] * min(run_sup, 10 * sup_per_energy) * 0.5) / d,
        클로버=o['clover_get'] * 10.0 / d,
    )
    v['합'] = sum(v.values())
    return v

PERSONAS = [  # 이름 · 하루에 쓰는 에너지 · (레어 Lv10 · 포인트 없음 기준) 에너지 1의 SUP
    ('가볍게(15분 · 에너지 2.8)', 2.8), ('보통(30분 · 에너지 5.5)', 5.5), ('오래(60분 · 에너지 11)', 11.0)]
SUP_PER_ENERGY = 895 * 0.0118 * 2.1          # 레어 가운데 신발: 에너지 1 = 895보 = 22.2 SUP
LAMS = [(0.09, '포인트 0(뽑은 값만)'), (0.41, '10포인트'), (0.62, '20포인트'), (0.84, '40포인트'), (0.97, '77포인트(전부)')]

def lam_of(base, points, pmax, tau_frac=0.30):
    """행운 수치 Λ — 뽑은 값에서 시작해 포인트로 100 에 다가간다(많이 넣을수록 덜 오른다)"""
    return base + (1 - base) * (1 - math.exp(-points / (tau_frac * pmax)))

def table():
    rows = []
    for name, e in PERSONAS:
        run_sup = e * SUP_PER_ENERGY
        for lam, label in LAMS:
            o = simulate(lam, e, run_sup)
            v = value(o, run_sup, e, SUP_PER_ENERGY); d = o['days']
            rows.append((name, label, lam, run_sup, o, v))
            print(f"{name:22s} {label:18s} Λ{lam:.2f} | 출발 {100*o['start']/d:4.1f}% · 줍기 {o['finds']/d:.2f}/일(소{o['S']/d:.2f} 중{o['M']/d:.2f} 대{o['B']/d:.2f}) · 보상 %{100*o['reward']/d:4.1f} 고정{100*o['reward_flat']/d:4.1f}% "
                  f"| 상자 {(o['shard']/5+o['box'])/d:.3f}/일 · 클로버 {o['clover_get']/d:.3f}/일 | 값 {v['합']:5.1f} SUP/일 = 기본 적립의 {100*v['합']/run_sup:4.1f}% "
                  f"(보상 % {o['sup_bonus']/d:.1f} + 고정 {o['sup_flat']/d:.1f} · 출발 {v['출발버프']:.1f} · 상자 {v['상자']:.1f} · 아이템 {v['아이템']:.1f})")
        print()
    return rows

def check(rows):
    bad = []
    for name, label, lam, run_sup, o, v in rows:
        pct = 100 * v['합'] / run_sup
        if lam < 0.15 and not (2 <= pct <= 7): bad.append(f'{name} {label}: 행운을 안 올린 신발의 값 {pct:.1f}% (목표 2 ~ 7%)')
        if lam > 0.95 and not (12 <= pct <= 30): bad.append(f'{name} {label}: 행운을 다 올린 신발의 값 {pct:.1f}% (목표 12 ~ 30%)')
        if (o['shard'] / 5 + o['box']) / o['days'] > 0.6: bad.append(f'{name} {label}: 상자가 하루 {(o["shard"]/5+o["box"])/o["days"]:.2f}개(0.6 넘음 — 레벨 속도)')
        if 100 * v['보상행운'] / run_sup > 5: bad.append(f'{name} {label}: 보상 행운의 SUP 가 {100*v["보상행운"]/run_sup:.1f}%(5% 넘음 — 발행량. 고정 SUP 는 짜게)')
    if bad:
        print('\n'.join(bad)); sys.exit(1)
    print('행운 설계 목표 — 모두 안쪽')

# ── 크루 러닝의 행운(사용자 2026-11-21) — 확률이 아니라 늘 발동: 출발 전 버프(효율 · 착화감 +N%) + 끝난 뒤 고정 SUP ──
CREW = dict(buff=(0.02, 0.12), flat=(1.0, 12.0), s0=3.0, level_buff=0.02, level_flat=0.03, buff_cap=0.15,
            flat_share=(0.10, 0.25, 20))      # 고정 SUP 는 기본 보상의 10% 까지, 인원이 늘수록 올라 20명 이상이면 25%

def crew_luck(lams, crew_level=1, base_reward=123.0, c=CREW):
    """lams = 함께 출발한 사람들이 고른 신발의 Λ. 합이 클수록(사람이 많을수록 · 행운이 높을수록) 커지되 점점 덜 커진다"""
    S = sum(lams); g = 1 - math.exp(-S / c['s0']); n_people = len(lams)
    n = min((c['buff'][0] + (c['buff'][1] - c['buff'][0]) * g) * (1 + c['level_buff'] * (crew_level - 1)), c['buff_cap'])
    lo, hi, full = c['flat_share']
    share = lo + (hi - lo) * min(max((n_people - 2) / (full - 2), 0), 1)
    f = min((c['flat'][0] + (c['flat'][1] - c['flat'][0]) * g) * (1 + c['level_flat'] * (crew_level - 1)), share * base_reward)
    return S, n, f

def crew_table():
    print('\n크루 러닝의 행운 — 인원 · 평균 행운 · 크루 레벨 → 출발 버프(효율 · 착화감) · 끝난 뒤 고정 SUP(한 사람) · 기본 보상 123 SUP 대비')
    for n, avg, lv in ((2, 0.09, 1), (2, 0.35, 1), (5, 0.09, 1), (5, 0.40, 1), (5, 0.40, 5), (10, 0.50, 1), (10, 0.50, 5), (20, 0.60, 1), (20, 0.60, 10)):
        S, buff, flat = crew_luck([avg] * n, lv)
        print(f'  {n:2d}명 · 평균 행운 {round(avg*100):3d} · 크루 Lv{lv:2d} | 행운 합 {S:5.2f} | 버프 +{100*buff:4.1f}% · 고정 +{flat:4.1f} SUP | 값 {123*buff + flat:5.1f} SUP = 기본 보상의 {100*(123*buff + flat)/123:4.1f}%')

if __name__ == '__main__':
    r = table()
    crew_table()
    print('행운 수치(레어 · 뽑은 값 9 · 만렙 77포인트):', ' · '.join(f'{p}포인트 → {100*lam_of(0.09, p, 77):.0f}' for p in (0, 5, 10, 20, 40, 77)))
    if '--check' in sys.argv[1:]: check(r)
