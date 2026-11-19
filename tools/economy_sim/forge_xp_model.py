#!/usr/bin/env python3
"""경험치 강화 설계 모형 — docs/forge-xp-daily-2026-11-19.md 의 숫자를 낸다(서버를 부르지 않는다).

재료 값(일반 1켤레 = 1.00) · 레벨마다 필요한 값 · 상자 수급으로 걸리는 날 · 지금(확률) 방식과의 비용 비교 ·
SUP 로 산 상자의 경험치 값 · 레벨 하나의 값어치(효율 포인트 · 에너지)와 본전 뽑는 날.

    python3 tools/economy_sim/forge_xp_model.py
"""
G = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']
KO = dict(zip(G, ['일반', '레어', '에픽', '레전더리', '레드라인', '피니시']))
MAXLV = dict(zip(G, [20, 20, 26, 27, 28, 30]))
PPL = dict(zip(G, [2, 3, 4, 5, 6, 8]))                 # 레벨마다 포인트 (0116)
RATE = dict(zip(G, [.010, .011, .0125, .014, .016, .018]))  # 포인트 1 = 적립 × (1 + RATE)
EFF_MID = dict(zip(G, [4000, 11000, 21500, 37250, 60875, 96313]))
PPS, REPAIR = 0.0118, 0.45

STEP = 3.1                                             # 윗등급 1켤레 = 아랫등급 3.1켤레 값
V = {g: STEP ** i for i, g in enumerate(G)}            # 재료 값(일반 = 1)
NEED1 = 2.88                                           # Lv1 → 2 에 필요한 "한 등급 아래" 켤레 수(일반은 일반 2.88켤레)
N1 = {g: NEED1 * V[G[max(i - 1, 0)]] for i, g in enumerate(G)}


def need_pct(level):
    """그 레벨에서 다음 레벨까지 — Lv1 을 100 으로. 지금 확률 방식의 평균 비용(3 ÷ 성공 확률)과 같은 굽이"""
    return 100 * 1.03 / (1.06 - 0.03 * level)


def units(grade, frm, to):
    return sum(N1[grade] * need_pct(L) / 100 for L in range(frm, to))


def old_mats(frm, to, bonus=20):
    """지금 방식 — 한 등급 아래 Lv1 재료 3개씩, 평균 재료 수"""
    return sum(3 / min(1.0, (1000 - 30 * L + 3 * bonus) / 1000) for L in range(frm, to))


NORMAL = {'COMMON': .79, 'RARE': .20, 'EPIC': .01}
PREMIUM = {'RARE': .60, 'EPIC': .28, 'LEGENDARY': .09, 'REDLINE': .025, 'FINISH': .005}
CRIT = 1.10                                            # 대성공 10% × 2배 → 평균 +10%
box_units = sum(p * V[g] for g, p in NORMAL.items())
prem_units = sum(p * V[g] for g, p in PREMIUM.items())

print('── 재료 값(일반 1켤레 = 1.00) · Lv1 → 2 에 필요한 값')
for g in G:
    print(f'  {KO[g]:5s} 값 {V[g]:7.2f}   필요 {N1[g]:7.2f}')

print('\n── 재료 1켤레가 채우는 경험치(대상 Lv1) — 등급 차이로')
for d, name in ((1, '한 등급 위'), (0, '같은 등급'), (-1, '한 등급 아래'), (-2, '두 등급 아래'), (-3, '세 등급 아래'), (-4, '네 등급 아래')):
    print(f'  {name:8s} {STEP ** (d + 1) / NEED1 * 100:6.1f}%')
print('  (일반 신발이 대상일 때는 레어 대상과 같다 — 일반 34.7% · 레어 107.6%)')

print('\n── 레벨이 오를수록(한 등급 아래 재료 기준)   새 경험치 | 지금 확률 방식 평균')
for L in (1, 5, 10, 15, 19, 20, 25, 29):
    print(f'  Lv{L:2d} → {L + 1:2d}   1켤레 {34.7 / need_pct(L) * 100:5.1f}%   {NEED1 * need_pct(L) / 100:5.1f}켤레 | {3 / min(1, (1060 - 30 * L) / 1000):5.1f}켤레')

print(f'\n── 상자 하나의 경험치 값 — 보통 상자 {box_units:.2f} · 상급 상자 {prem_units:.2f} (일반 켤레)')
print(f'   SUP 로 사면 — 보통 40 SUP ÷ {box_units * CRIT:.2f} = {40 / (box_units * CRIT):.1f} SUP/켤레 값 · 상급 600 SUP → {600 / (prem_units * CRIT):.1f} SUP/켤레 값')

print('\n── 걸리는 날(보통 상자만 재료로, 대성공 포함) — 가볍게 2상자 / 꾸준히 3상자 / 상자도 사서 6상자')
for g in G:
    row = []
    for to in sorted({5, 10, MAXLV[g]}):
        u = units(g, 1, to)
        row.append(f'Lv{to:2d} {u:7.0f}켤레값 ' + ' / '.join(f'{u / (b * box_units * CRIT):6.0f}일' for b in (2, 3, 6)))
    print(f'  {KO[g]:5s} ' + ' | '.join(row))

print('\n── 지금 방식과 견주기 — 만렙까지 재료 값(일반 켤레)')
for g in G[:3]:
    i = G.index(g)
    old = old_mats(1, MAXLV[g]) * V[G[max(i - 1, 0)]]
    print(f'  {KO[g]:5s} 지금 {old:6.0f} (한 등급 아래만 · 3개씩) → 새 {units(g, 1, MAXLV[g]):6.0f} (÷ 대성공 1.1 = {units(g, 1, MAXLV[g]) / CRIT:6.0f})')

print('\n── 레벨 하나의 값어치 — 하루 8,000보 러너 · 포인트를 모두 효율에 · 수리비 45% 뺀 순수익')
for g in G[:4]:
    for L in (1, 5, 10, 19):
        def gross(level, steps=8000):
            energy = 10 + 2 * (level - 1)
            s = min(steps, energy * 600)
            pts = PPL[g] * (level - 1)
            return s * PPS * (1 + EFF_MID[g] / 10000) * (1 + RATE[g] * pts)
        d = (gross(L + 1) - gross(L)) * (1 - REPAIR)
        cost_units = N1[g] * need_pct(L) / 100
        sup = cost_units * 40 / (box_units * CRIT)
        print(f'  {KO[g]:5s} Lv{L:2d}→{L + 1:2d}  하루 순수익 +{d:5.1f} SUP (지금 {gross(L) * (1 - REPAIR):6.1f})   '
              f'재료 {cost_units:6.1f}켤레값 = 상자로 사면 {sup:6.0f} SUP → 본전 {sup / d if d > 0 else float("inf"):5.0f}일')
