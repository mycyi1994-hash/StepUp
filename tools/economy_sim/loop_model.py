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
REPAIR_SHARE = 0.45          # 수리비 = 그 신발로 걸을 때(분당 110걸음) 버는 돈의 이 몫 — 효율에 비례한다
PTS_LEVEL = [2, 3, 4, 5, 6, 8]            # 레벨마다 포인트(등급마다)
PTS_MILE = [3, 5, 6, 8, 9, 12]            # 5레벨마다 보너스
RATE = [1.0, 1.1, 1.25, 1.4, 1.6, 1.8]    # 포인트 1 이 올리는 폭(뽑은 값의 %)
MAX_LEVEL = [20, 20, 26, 27, 28, 30]
BOX_PRICE, PREMIUM_PRICE = 40, 600
RUN_BOX_STEPS = (2000, 4000)
FORGE_OK = lambda lvl, bonus=90: min(1.0, (1000 - 30 * lvl + bonus) / 1000)   # 강화 성공 확률(서버 forge-v2)


def points(g, level):
    """레벨마다 PTS_LEVEL, 5레벨마다 PTS_MILE"""
    return PTS_LEVEL[g] * (level - 1) + PTS_MILE[g] * (level // 5)


def roll(lo, pos):
    """pos 0 = 가장 낮게, 0.5 = 가운데, 1 = 가장 높게"""
    return lo * (1 + (SPREAD - 1) * pos)


def run(g, level, steps_wanted, spm, eff_pos=0.5, cmf_pos=0.5, pts_eff=None):
    pts_eff = points(g, level) if pts_eff is None else pts_eff
    eff = roll(EFF_LO[g], eff_pos) * (1 + RATE[g] * pts_eff / 100)
    cmf = roll(CMF_LO[g], cmf_pos)
    energy = 10 + 2 * (level - 1)
    steps = min(steps_wanted, energy * 600 * cmf / 100)
    minutes = steps / spm
    earn = steps * PPS * eff / 100
    repair = minutes * REPAIR_SHARE * 110 * PPS * eff / 100
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
    print('── 레벨업 — 등급마다(가운데 신발 · 효율에 몰아 찍으면) ──')
    print('%-6s %6s %8s %10s %8s %8s %10s %10s' % ('', '레벨당', '포인트1', '한 레벨', 'Lv1', 'Lv10', 'Lv20', '최대 레벨'))
    for g, name in enumerate(GRADES):
        e = lambda lvl: run(g, lvl, 3300, 110)[0]
        m = MAX_LEVEL[g]
        print('%-6s %5d개 %7.2f%% %9.1f%% %8.0f %8.0f %10.0f %6.0f(Lv%d, ×%.2f)'
              % (name, PTS_LEVEL[g], RATE[g], PTS_LEVEL[g] * RATE[g], e(1), e(10), e(20), e(m), m, e(m) / e(1)))
    print('가장 센 신발(가운데 피니시 Lv30) ÷ 가운데 일반 새 신발 = %.0f배' % (run(5, 30, 1, 110)[0] / run(0, 1, 1, 110)[0]))
    print()
    print('── 같은 걷기 30분 — 적립 / 남는 돈(가운데 신발 · 효율에 몰면) ──')
    for g, name in enumerate(GRADES):
        cells = []
        for lvl in (1, 5, 10, 20):
            _, earn, rep, _, _ = run(g, lvl, 3300, 110)
            cells.append('Lv%-2d %5.0f / %-5.0f' % (lvl, earn, earn - rep))
        print('%-6s %s' % (name, '   '.join(cells)))
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
