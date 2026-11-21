#!/usr/bin/env python3
"""에픽 이상 행운 = 경험치 · 강화 효과(docs/stat-overhaul-2026-11-21.md 13장). 서버를 부르지 않는다.

사용자 "너무 지저분한디 · 에픽부터는 행운에서 경험치 관련, 강화 관련으로 러닝 외적으로도 · 에픽 · 레전 3개, 레드 4개, 피니시 5개".
모든 신발의 행운(행운 기획서 — 출발 · 줍기 · 보상)은 그대로 두고, 에픽 이상에 다섯 순간의 효과를 등급만큼 더한다:

  ① 경험치 러닝 — 러닝이 끝날 때      이번 러닝 경험치 × M          (에픽 · 레전더리 · 레드라인 · 피니시)
  ② 강화 대성공 — 재료를 넣을 때      그 재료 경험치 × M            (지금 0118 의 × 2 를 등급만큼)
  ③ 보너스 포인트 — 레벨이 오를 때    그 레벨 포인트 + N
  ④ 잠재 각성 — 5레벨마다             가장 낮은 뽑은 능력치 + 등급 범위의 R    (레드라인부터)
  ⑤ 한계 돌파 — 만렙이 된 뒤          경험치가 통 하나만큼 찰 때마다 포인트 + 1, 상한    (피니시만)

성장(달린 만큼 자란다 — growth_luck.py 11.2)은 그대로 쓴다.

    python3 tools/economy_sim/luck_forge.py           # 효과마다 확률 · 크기 · 성장 · 만렙 하루 SUP
    python3 tools/economy_sim/luck_forge.py --check   # 설계 목표 — 밖이면 1
"""
import os, sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import growth_luck as GL  # noqa: E402

V, TD = GL.V, GL.TD
HIGH, KO, MAXLV, SPM = GL.HIGH, GL.KO, GL.MAXLV, GL.SPM
ORD = {'EPIC': 0, 'LEGENDARY': 1, 'REDLINE': 2, 'FINISH': 3}
COUNT = dict(EPIC=3, LEGENDARY=3, REDLINE=4, FINISH=5)    # 등급마다 효과 수(사용자)

# ── 손잡이(제안) — 크기는 에픽 · 레전더리 · 레드라인 · 피니시 ─────────────────────────────
RUN_P = (0.20, 0.15, 0.10, 0.08)   # ① 행운 끝에서 러닝마다 터지는 비율(확률 = 이 값 × Λ / Λ끝)
RUN_M = (2, 3, 4, 5)               # ①
CRIT_P = (0.10, 0.10)              # ② 확률 = 10% + 10% × Λ(지금 0118 그대로)
CRIT_M = (3, 4, 5, 6)              # ② 지금(0118)은 모든 등급 × 2 — 에픽부터 × 3
BONUS_P = 0.30                     # ③ 확률 = 30% × Λ
BONUS_N = (1, 2, 3, 4)             # ③ 레벨마다 받는 포인트(4 · 5 · 6 · 8)에 더해지는 몫
AWAKE_P = 0.40                     # ④ 확률 = 40% × Λ(5레벨마다)
AWAKE_R = (0.0, 0.0, 0.10, 0.15)   # ④ 등급 범위의 몫(레드라인 10% · 피니시 15%)
BREAK_P = (0.50, 0.50)             # ⑤ 확률 = 50% + 50% × Λ
BREAK_CAP = 16                     # ⑤ 만렙 뒤 포인트 상한(피니시 레벨 둘 치)
FIND_XP = (300, 600, 3100)         # 행운 기획서 줍기의 경험치(9장 — 소 · 중 · 대)


def lam_end(g):
    return GL.lam_at(g, MAXLV[g], True)


def p_run(g, lam):
    return RUN_P[ORD[g]] * lam / lam_end(g)


def p_crit(lam):
    return min(CRIT_P[0] + CRIT_P[1] * lam, 1.0)


def grow(g, all_luck, effects=True, after_max_days=0):
    """Lv1 → 만렙(그리고 그 뒤) — 날마다 40분 · 상자 재료 모두. 기대값으로 센다(① · ② 는 경험치, ③ · ④ · ⑤ 는 모은 기대값)"""
    o = ORD[g]
    lv, xp, day = 1, 0.0, 0
    base_box = GL.BOX_XP_DAY / 1.1                     # 지금 대성공(10% × 2) 빼고
    bonus = awake = brk = 0.0
    brk_xp = 0.0; brk_day = None
    while day < 20000:
        if lv >= MAXLV[g] and (day >= after_max_days_total(g, after_max_days, day0=day) if False else False): break
        lam = GL.lam_at(g, lv, all_luck)
        n = GL.reward_steps(g, lv)
        run = n * GL.RUN_XP * GL.F[g]
        if effects:
            run *= 1 + p_run(g, lam) * (RUN_M[o] - 1)
            box = base_box * (1 + p_crit(lam) * (CRIT_M[o] - 1))
        else:
            box = base_box * (1 + p_crit(lam) * 1)        # 지금 그대로(× 2)
        S, Mi, B = GL.finds_per_day(g, lv, lam, n / TD.STEPS_PER_ENERGY)
        finds = S * FIND_XP[0] + Mi * 0.55 * FIND_XP[1] + B * 0.60 * FIND_XP[2]
        day += 1
        if lv < MAXLV[g]:
            xp += run + box + finds
            while lv < MAXLV[g] and xp >= GL.need(g, lv):
                xp -= GL.need(g, lv); lv += 1
                lam_up = GL.lam_at(g, lv, all_luck)
                if effects: bonus += BONUS_P * lam_up * BONUS_N[o]
                if lv % 5 == 0:
                    xp += GL.LEVEL_BOX_XP
                    if effects and COUNT[g] >= 4: awake += AWAKE_P * lam_up
            if lv >= MAXLV[g]:
                max_day = day
                if not (effects and COUNT[g] >= 5): break
        else:
            # ⑤ 한계 돌파 — 만렙 뒤에는 Lv(만렙 − 1) 통만큼 찰 때마다 포인트 + 1 (확률)
            brk_xp += run + box + finds
            tank = GL.need(g, MAXLV[g] - 1)
            while brk_xp >= tank and brk < BREAK_CAP:
                brk_xp -= tank; brk += min(BREAK_P[0] + BREAK_P[1] * lam, 1.0)
            if brk >= BREAK_CAP - 1e-9:
                brk_day = day - max_day; break
            if day - max_day > 3000: break
    return dict(days=max_day, bonus=bonus, awake=awake, brk=min(brk, BREAK_CAP), brk_days=brk_day)


def after_max_days_total(*a, **k):
    return 0


def best_total(g, lv, minutes, total, parts=10):
    if total <= 0: return V.day(g, lv, minutes, (0, 0, 0, 0))
    return max(V.day(g, lv, minutes, al) for al in V.allocations(int(round(total)), parts))


def awake_gain(g, r):
    """④ 한 번 — 효율 · 내구도 · 착화감 가운데 하나의 뽑은 자리를 + r(가운데 0.5 → 0.5 + r), 셋의 평균 값 비율(만렙 · 40분 · 최선 배분)"""
    lv = MAXLV[g]; tot = TD.points_total(g, lv)
    base = best_total(g, lv, 40, tot)
    orig = V.shoe
    gains = []
    for which in ('eff', 'dur', 'comfort'):
        def patched(gg, q=0.5, k=None, _w=which):
            s = orig(gg, q, k)
            if gg == g:
                s2 = orig(gg, q + r, k)
                s[_w] = s2[_w]
            return s
        V.shoe = patched
        try:
            gains.append(best_total(g, lv, 40, tot) / base - 1)
        finally:
            V.shoe = orig
    return sum(gains) / len(gains)


def main(check=False):
    GL.calibrate(); GL.RUN_XP = GL.fit_run_xp()
    bad = []
    print('에픽 이상 행운 효과(행운 끝 기준 — 0포인트는 그 신발 뽑은 행운)')
    for g in HIGH:
        o = ORD[g]; le = lam_end(g); l0 = GL.lam_at(g, MAXLV[g], False)
        cells = [f'① 러닝 끝 {100 * p_run(g, l0):.1f} → {100 * p_run(g, le):.0f}% · 경험치 × {RUN_M[o]}',
                 f'② 강화 {100 * p_crit(l0):.0f} → {100 * p_crit(le):.0f}% · 재료 × {CRIT_M[o]}',
                 f'③ 레벨업 {100 * BONUS_P * l0:.0f} → {100 * BONUS_P * le:.0f}% · 포인트 + {BONUS_N[o]}']
        if COUNT[g] >= 4: cells.append(f'④ 5레벨마다 {100 * AWAKE_P * l0:.0f} → {100 * AWAKE_P * le:.0f}% · 능력치 + 범위 {100 * AWAKE_R[o]:.0f}%')
        if COUNT[g] >= 5: cells.append(f'⑤ 만렙 뒤 통마다 {100 * (BREAK_P[0] + BREAK_P[1] * l0):.0f} → {100 * min(BREAK_P[0] + BREAK_P[1] * le, 1):.0f}% · 포인트 + 1(상한 {BREAK_CAP})')
        print(f'  {KO[g]:5s} {COUNT[g]}개 | ' + ' · '.join(cells))
    print('\n성장 — 만렙까지(날마다 40분 · 상자 재료 모두) · 그동안 ③ 보너스 포인트 · ④ 각성 · 만렙 하루 SUP(40분 · 최선 배분)')
    for g in HIGH:
        o = ORD[g]; lv = MAXLV[g]; tot = TD.points_total(g, lv)
        now0 = grow(g, False, effects=False)['days']
        r0, r1 = grow(g, False), grow(g, True)
        base_sup = best_total(g, lv, 40, tot)
        rows = []
        for name, r in (('0포인트', r0), ('다 넣음', r1)):
            sup = best_total(g, lv, 40, tot + r['bonus'] + r['brk'])
            aw = r['awake'] * awake_gain(g, AWAKE_R[o]) if COUNT[g] >= 4 else 0.0
            gain = (sup / base_sup) * (1 + aw) - 1
            rows.append((name, r, gain, aw))
            if check and name == '다 넣음':
                speed = 1 - r['days'] / now0
                if not (0.05 <= speed <= 0.30): bad.append(f'{g}: 행운 다 넣으면 만렙 {100 * speed:.0f}% 빨라짐(5 ~ 30% 밖)')
                if gain > 0.15: bad.append(f'{g}: 만렙 하루 SUP +{100 * gain:.1f}%(15% 넘음)')
                if r['bonus'] / tot > 0.15: bad.append(f'{g}: 보너스 포인트 {r["bonus"]:.0f}(만렙 포인트의 15% 넘음)')
        print(f'  {KO[g]:5s} 만렙 Lv{lv} · 포인트 {tot} | 효과 없이 {now0}일')
        for name, r, gain, aw in rows:
            extra = f' · ④ 각성 {r["awake"]:.1f}번(한 번에 하루 SUP +{100 * awake_gain(g, AWAKE_R[o]):.1f}%)' if COUNT[g] >= 4 else ''
            brk = f' · ⑤ 만렙 뒤 +{BREAK_CAP} 까지 {r["brk_days"]}일' if COUNT[g] >= 5 and r['brk_days'] else ''
            print(f'      {name}: {r["days"]}일({100 * (1 - r["days"] / now0):.0f}% 빨라짐) · ③ +{r["bonus"]:.1f}포인트({100 * r["bonus"] / tot:.0f}%){extra}{brk} → 만렙 하루 SUP +{100 * gain:.1f}%')
    print('\n터졌을 때(행운 끝 · 만렙 근처) — 한 번의 크기')
    for g in HIGH:
        o = ORD[g]; lv = MAXLV[g] - 1
        n = GL.reward_steps(g, lv); run = n * GL.RUN_XP * GL.F[g]; tank = GL.need(g, lv)
        mat = 1000 * 3.1 ** 1 * 1.0                                         # 레어 한 켤레(가운데 뽑은 값)
        print(f'  {KO[g]:5s} | ① 40분 러닝 경험치 {run / 1000:.0f}k → +{run * (RUN_M[o] - 1) / 1000:.0f}k(그 레벨 통의 +{100 * run * (RUN_M[o] - 1) / tank:.0f}%) · '
              f'② 레어 재료 {mat / 1000:.1f}k → +{mat * (CRIT_M[o] - 1) / 1000:.1f}k(+{100 * mat * (CRIT_M[o] - 1) / tank:.1f}%) · '
              f'③ 포인트 {TD.PPL[g]} → {TD.PPL[g] + BONUS_N[o]}' + (f' · ④ 하루 SUP +{100 * awake_gain(g, AWAKE_R[o]):.1f}%(평생)' if COUNT[g] >= 4 else ''))
    if check:
        if bad: print('\n'.join(bad)); sys.exit(1)
        print('\n경험치 · 강화 행운 목표 — 모두 안쪽')


if __name__ == '__main__':
    main('--check' in sys.argv)
