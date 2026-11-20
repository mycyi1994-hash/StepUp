"""레벨이 오르는 속도 — 날마다 꾸준히 하는 무과금 사용자를 모의로 돌린다(서버를 부르지 않는다, 값은 0118 그대로).

하루 보통 상자 3(출석 1 + 러닝 2) · 주마다 상급 2(주간 걸음 + 7일 연속) · 5레벨마다 상급 1, 받은 신발은 모두 주력 신발에 넣는다.
  1) 예전 확률 강화(3켤레 · 성공률)와 새 경험치 강화를 같은 상자 운으로 견준다
  2) 통이 레벨마다 커지는 굽이 후보("레벨마다 +N%")로 등급별 만렙까지 걸리는 날
2026-11-21 점검(docs/economy-roots-2026-11-21.md 뿌리 2): 레어 만렙 예전 24일 → 0118 9일. "레벨마다 +30%"면 25일 · 에픽 121일.

    python3 tools/economy_sim/pace.py
"""
import random, statistics, sys
G = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']
MAXLV = [20, 20, 26, 27, 28, 30]
NORMAL = [(0, .79), (1, .20), (2, .01)]
PREMIUM = [(1, .60), (2, .28), (3, .09), (4, .025), (5, .005)]
STEP, NEED = 3.1, 2.88

def need(g, lv):
    if lv >= MAXLV[g]: return None
    return round(1000 * NEED * STEP ** max(g - 1, 0) * 1.03 / (1.06 - 0.03 * lv))

def draw(pool, rng):
    x = rng.random(); a = 0
    for g, p in pool:
        a += p
        if x < a: return g
    return pool[-1][0]

def day_boxes(day, extra_normal=0, extra_premium_week=0):
    n = 3 + extra_normal
    p = (1 if day % 7 == 0 else 0) * (2 + extra_premium_week)      # 주간 걸음 + 7일 연속(+ 산 것)
    return n, p

def run_new(rng, days, switch, extra_normal=0, extra_premium_week=0):
    main = None            # [grade, level, xp]
    got_level_box = set(); out = {}; hist = []
    for day in range(1, days + 1):
        n, p = day_boxes(day, extra_normal, extra_premium_week)
        loot = [draw(NORMAL, rng) for _ in range(n)] + [draw(PREMIUM, rng) for _ in range(p)]
        queue = loot[:]
        while queue:
            g = queue.pop()
            if main is None:
                if g >= 1: main = [g, 1, 0]; continue      # 첫 레어부터 키운다(첫 신발은 재료가 못 되니 안 키운다)
                stash = out.setdefault('stash', []); stash.append(g); continue
            if switch and g > main[0]:
                # 더 높은 등급이 나오면 갈아탄다 — 예전 주력은 재료로(들어간 경험치의 50%)
                inv = sum(need(main[0], l) for l in range(1, main[1])) + main[2]
                val = 1000 * STEP ** main[0] + 0.5 * inv
                main = [g, 1, 0]
                gain = val
            else:
                own = 1000 * STEP ** g
                gain = own * (2 if rng.random() < 0.10 else 1)
            for s in out.pop('stash', []):
                gain += 1000 * STEP ** s * (2 if rng.random() < 0.10 else 1)
            main[2] += gain
            while True:
                nd = need(main[0], main[1])
                if nd is None: main[2] = 0; break
                if main[2] < nd: break
                main[2] -= nd; main[1] += 1
                if main[1] % 5 == 0 and main[1] not in got_level_box:
                    got_level_box.add(main[1]); queue.append(draw(PREMIUM, rng))
        top = max(main[1] if main else 1, 1)
        hist.append((day, main[0] if main else 0, top))
    return hist

def run_old(rng, days):
    """예전: 대상 레어 고정, 재료 3켤레(한 등급 아래 이상 = 아무 신발), 성공률 = 1000 − 30L + 보너스"""
    main = None; inv = []; got = set(); hist = []
    for day in range(1, days + 1):
        n, p = day_boxes(day)
        queue = [draw(NORMAL, rng) for _ in range(n)] + [draw(PREMIUM, rng) for _ in range(p)]
        while queue:
            g = queue.pop()
            if main is None and g >= 1: main = [g, 1]; continue
            inv.append(g)
            while main and main[1] < MAXLV[main[0]]:
                ok = sorted(x for x in inv if x >= main[0] - 1)
                if len(ok) < 3: break
                use = ok[:3]
                for x in use: inv.remove(x)
                bonus = sum(20 * {-1: 2, 0: 3, 1: 4, 2: 5}.get(x - main[0], 6) // 2 for x in use)
                if rng.random() * 1000 < min(1000, 1000 - 30 * main[1] + bonus):
                    main[1] += 1
                    if main[1] % 5 == 0 and main[1] not in got: got.add(main[1]); queue.append(draw(PREMIUM, rng))
        hist.append((day, main[0] if main else 0, main[1] if main else 1))
    return hist

def summarize(label, runs, marks=(7, 14, 30, 60, 90)):
    line = []
    for d in marks:
        lv = sorted(h[d - 1][2] for h in runs); gr = sorted(h[d - 1][1] for h in runs)
        line.append(f'{d}일: Lv{lv[len(lv)//2]}({["일반","레어","에픽","레전","레드","피니"][gr[len(gr)//2]]}) 에너지 {10 + 2 * (lv[len(lv)//2] - 1)}')
    print(f'{label:34s} ' + ' | '.join(line))

N = 1500
rng = random.Random(118)
summarize('예전 확률 강화 · 레어 주력', [run_old(rng, 90) for _ in range(N)])
summarize('새 경험치 · 레어 주력 고정', [run_new(rng, 90, False) for _ in range(N)])
summarize('새 경험치 · 높은 등급으로 갈아탐', [run_new(rng, 90, True) for _ in range(N)])
summarize('새 경험치 · 갈아탐 + 상자 다 삼', [run_new(rng, 90, True, 3, 2) for _ in range(N)])
def days_to(runs, lv):
    ds = sorted(next((d for d, g, l in h if l >= lv), 999) for h in runs)
    return ds[len(ds)//2]
old = [run_old(rng, 200) for _ in range(N)]; new = [run_new(rng, 200, False) for _ in range(N)]
print('레어 주력이 Lv10 / Lv20(만렙)에 닿는 날(가운데 값) — 예전:', days_to(old, 10), '/', days_to(old, 20), '· 새:', days_to(new, 10), '/', days_to(new, 20))


# ── 2) 통이 레벨마다 커지는 굽이 — 주력 등급을 고정했을 때 만렙까지(가운데 값) ──
def run_fixed(rng, days, grade):
    main = [grade, 1, 0]; got = set(); hist = []
    for day in range(1, days + 1):
        n, p = day_boxes(day)
        q = [draw(NORMAL, rng) for _ in range(n)] + [draw(PREMIUM, rng) for _ in range(p)]
        while q:
            g = q.pop()
            main[2] += 1000 * STEP ** g * (2 if rng.random() < 0.10 else 1)
            while True:
                nd = need(main[0], main[1])
                if nd is None: main[2] = 0; break
                if main[2] < nd: break
                main[2] -= nd; main[1] += 1
                if main[1] % 5 == 0 and main[1] not in got: got.add(main[1]); q.append(draw(PREMIUM, rng))
        hist.append(main[1])
        if main[1] >= MAXLV[grade]: break
    return hist

def linear(k):
    return lambda g, lv: None if lv >= MAXLV[g] else round(1000 * NEED * STEP ** max(g - 1, 0) * (1 + k * (lv - 1)))

_now = need
print()
for label, fn in [('지금(0118)', _now)] + [(f'레벨마다 +{int(k * 100)}%', linear(k)) for k in (0.25, 0.30, 0.35)]:
    need = fn
    rng = random.Random(21); out = []
    for g, nm, horizon, n in ((1, '레어', 400, 400), (2, '에픽', 1500, 200), (3, '레전더리', 6000, 80)):
        runs = [run_fixed(rng, horizon, g) for _ in range(n)]
        ds = sorted(len(h) for h in runs); d7 = sorted(h[min(6, len(h) - 1)] for h in runs)
        out.append(f'{nm} 만렙 {ds[len(ds) // 2]}일(7일째 Lv{d7[len(d7) // 2]})')
    print(f'{label:12s} 레어 통(켤레값) Lv1 · 10 · 19 = {need(1, 1) / 1000:.1f} · {need(1, 10) / 1000:.1f} · {need(1, 19) / 1000:.1f} | ' + ' | '.join(out))
need = _now
