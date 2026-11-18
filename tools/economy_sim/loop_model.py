#!/usr/bin/env python3
"""설계안 2판(docs/shoe-stats-boxes-2026-11-18.md)의 하루 흐름을 손으로 세는 계산기.

서버 규칙을 그대로 돌리는 것은 run.sh(실제 함수 — tweak-design-2026-11-18.sql)다. 이 파일은 서버에 아직 없는 것 —
상자 수급처 · 상자 값 · "시간이 모자란 사람"(에너지를 다 못 쓰는 사람) · 뽑기 운 — 을 같은 식으로 세어 표로 낸다.
값을 바꾸면 문서의 표도 함께.
"""
import random

GRADES = ['일반', '레어', '에픽', '레전더리', '레드라인', '피니시']
PPS = 0.0118                 # 효율 100 인 신발의 걸음당 SUP — 가운데 일반(140)이 계획 1단계의 적립과 같다
SPREAD = 1.8                 # 같은 등급 안에서 가장 높은 값 ÷ 가장 낮은 값
EFF_STEP, DUR_STEP = 1.5, 1.35
EFF_LO = [100 * EFF_STEP ** i for i in range(6)]              # 효율 지수의 바닥
CMF_LO = [100 + 10 * i / 1.4 for i in range(6)]               # 착화감 지수의 바닥(가운데가 등급마다 +10)
DUR_LO_MIN = [22 * DUR_STEP ** i for i in range(6)]           # 한 켤레로 달리는 분의 바닥
UNIT = [0.1625 * 1.5 ** i for i in range(6)]                  # 내구도 1 의 수리값(레벨 0)
LEVEL_STEP = 0.02
MAX_LEVEL = [20, 20, 26, 27, 28, 30]
BOX_PRICE, PREMIUM_PRICE = 40, 600
RUN_BOX_STEPS = (2000, 4000)
FORGE_OK = lambda lvl, bonus=90: min(1.0, (1000 - 30 * lvl + bonus) / 1000)   # 강화 성공 확률(서버 forge-v2)


def points(level):
    """레벨마다 2, 5레벨마다 +3"""
    return sum(2 + (3 if l % 5 == 0 else 0) for l in range(2, level + 1))


def roll(lo, pos):
    """pos 0 = 가장 낮게, 0.5 = 가운데, 1 = 가장 높게"""
    return lo * (1 + (SPREAD - 1) * pos)


def run(g, level, steps_wanted, spm, eff_pos=0.5, cmf_pos=0.5, pts_eff=None):
    pts_eff = points(level) if pts_eff is None else pts_eff
    eff = roll(EFF_LO[g], eff_pos) * (1 + pts_eff / 100)
    cmf = roll(CMF_LO[g], cmf_pos)
    energy = 10 + 2 * (level - 1)
    steps = min(steps_wanted, energy * 600 * cmf / 100)
    minutes = steps / spm
    earn = steps * PPS * eff / 100
    repair = minutes * 5 * UNIT[g] * (1 + LEVEL_STEP * level)
    return eff, earn, repair, minutes, steps


def main():
    print('── 같은 걷기 30분(3,300보) · Lv1 — 등급과 뽑기 운 ──')
    print('%-6s %14s %10s %22s %8s %8s' % ('', '효율 지수', '달리는 분', '적립(낮게/가운데/높게)', '수리', '남는 돈'))
    for g, name in enumerate(GRADES):
        e = [run(g, 1, 3300, 110, eff_pos=p)[1] for p in (0, 0.5, 1)]
        rep = run(g, 1, 3300, 110)[2]
        print('%-6s %6.0f ~ %-6.0f %4.0f ~ %-4.0f %7.0f / %4.0f / %-6.0f %8.0f %8.0f'
              % (name, EFF_LO[g], EFF_LO[g] * SPREAD, DUR_LO_MIN[g], DUR_LO_MIN[g] * SPREAD, e[0], e[1], e[2], rep, e[1] - rep))
    print()
    print('── 레벨업 — 가운데 일반 신발(효율 140)을 효율에 몰아 찍으면 ──')
    for lvl in (1, 2, 5, 10, 15, 20):
        eff, earn, rep, _, _ = run(0, lvl, 4400, 110)
        print('Lv%-2d 포인트 %2d  효율 %3.0f  하루 에너지 %2d  걷기 40분: 적립 %3.0f − 수리 %2.0f = %3.0f'
              % (lvl, points(lvl), eff, 10 + 2 * (lvl - 1), earn, rep, earn - rep))
    print('가운데 레어 Lv1 의 효율 %.0f — 일반 Lv20(효율 %.0f)과 같다' % (roll(EFF_LO[1], .5), roll(EFF_LO[0], .5) * (1 + points(20) / 100)))
    print()
    print('── 사람별 하루(가운데 신발 · Lv5 · 포인트는 효율) ──')
    for name, spm, minutes in (('가볍게 — 걷기 20분', 110, 20), ('보통 — 걷기 40분', 110, 40), ('러너 — 달리기 30분', 165, 30), ('열심히 — 달리기 60분', 165, 60)):
        cells = []
        for g in (0, 1, 2):
            _, earn, rep, _, steps = run(g, 5, spm * minutes, spm)
            cells.append('%s %3.0f−%3.0f=%+4.0f' % (GRADES[g], earn, rep, earn - rep))
        boxes = 1 + sum(run(0, 5, spm * minutes, spm)[4] >= t for t in RUN_BOX_STEPS)
        print('%-20s %s  무료 상자 %d' % (name, ' · '.join(cells), boxes))
    print()
    print('── 일반 신발 Lv20 까지(재료 3켤레) ──')
    att = sum(1 / FORGE_OK(l) for l in range(1, 20))
    for free, buy in ((1, 0), (2, 0), (3, 0), (3, 1), (3, 3)):
        days = att * 3 / (free + buy)
        print('하루 무료 %d + 구매 %d → %3.0f일, 상자 값 합 %5.0f SUP' % (free, buy, days, days * buy * BOX_PRICE))
    print()
    print('── 뽑기 운(주사위 두 개 합 — 가운데가 자주, 양 끝은 드물게) ──')
    random.seed(11)
    n = 300000
    tri = lambda: (random.random() + random.random()) / 2
    avg = [(tri() + tri() + tri()) / 3 for _ in range(n)]
    one = [tri() for _ in range(n)]
    print('별: ★ %.0f%% · ★★ %.0f%% · ★★★ %.0f%%' % (100 * sum(a < .5 for a in avg) / n, 100 * sum(.5 <= a < .65 for a in avg) / n, 100 * sum(a >= .65 for a in avg) / n))
    print('한 능력치가 범위의 위 10%% 안에 뽑힐 확률 %.1f%% · 아래 10%% %.1f%%' % (100 * sum(o >= .9 for o in one) / n, 100 * sum(o < .1 for o in one) / n))
    best = sorted(max(tri() for _ in range(30)) for _ in range(20000))[10000]
    print('한 달 동안 뽑은 일반 30켤레 가운데 가장 좋은 효율 — 보통 범위의 %.0f%% 자리(효율 %.0f)' % (100 * best, roll(EFF_LO[0], best)))


if __name__ == '__main__':
    main()
