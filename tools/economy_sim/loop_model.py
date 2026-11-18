#!/usr/bin/env python3
"""설계안(docs/shoe-stats-boxes-2026-11-18.md)의 하루 흐름을 손으로 세는 계산기.

서버 규칙을 그대로 돌리는 것은 run.sh(실제 함수)다. 이 파일은 서버에 아직 없는 것 — 상자 수급처 · 상자 값 ·
"시간이 모자란 사람"(에너지를 다 못 쓰는 사람) — 을 같은 식으로 세어 표로 낸다. 값을 바꾸면 문서의 표도 함께.
"""
PPS = 0.016            # 걸음당 SUP (계획 1단계)
WEAR_PER_MIN = 5
UNIT = {'일반': 0.1625, '레어': 0.14295, '에픽': 0.11318}   # 내구도 1 의 수리값(Lv0 기준, 계획 1단계)
LEVEL_STEP = 0.02
EFF0 = {'일반': 0.10, '레어': 0.18, '에픽': 0.29}           # 뽑는 값의 가운데
CMF0 = {'일반': 0.03, '레어': 0.06, '에픽': 0.09}
DUR0 = {'일반': 150, '레어': 200, '에픽': 250}
PTS = {'일반': 4, '레어': 5, '에픽': 6}                      # 레벨마다 포인트
EFF_PER_PT = 0.005
BOX_PRICE = 40
RUN_BOX_STEPS = (2000, 4000)                                 # 오늘 적립 걸음 — 러닝 상자 둘
FORGE_OK = lambda lvl, bonus=90: min(1.0, (1000 - 30 * lvl + bonus) / 1000)   # 강화 성공 확률(서버 forge-v2)
FORGE_FEE = 10                                               # (선택) 강화 한 번의 수수료 = 이 값 × 대상 레벨

PEOPLE = [('가볍게 — 걷기 20분', 110, 20), ('보통 — 걷기 40분', 110, 40),
          ('러너 — 달리기 30분', 165, 30), ('열심히 — 달리기 60분', 165, 60)]


def day(rarity, level, spm, minutes):
    eff = EFF0[rarity] + EFF_PER_PT * PTS[rarity] * (level - 1)
    energy = 10 + 2 * (level - 1)
    cap = energy * 600 / (1 - CMF0[rarity])
    steps = min(spm * minutes, cap)
    played = steps / spm                                   # 에너지를 다 쓰면 멈춘다고 본다
    earn = steps * PPS * (1 + eff)
    repair = played * WEAR_PER_MIN * UNIT[rarity] * (1 + LEVEL_STEP * level)
    boxes = 1 + sum(steps >= t for t in RUN_BOX_STEPS)
    return earn, repair, boxes, played


def main():
    print('── 사람별 하루(일반 신발 · 포인트는 모두 효율) ──')
    print('%-22s %4s %8s %8s %8s %6s %s' % ('', 'Lv', '적립', '수리', '남는 돈', '상자', '남는 돈으로 사는 상자'))
    for name, spm, minutes in PEOPLE:
        for lvl in (1, 10, 20):
            e, r, b, played = day('일반', lvl, spm, minutes)
            print('%-22s %4d %8.0f %8.0f %8.0f %6d %6.1f' % (name, lvl, e, r, e - r, b, (e - r) / BOX_PRICE))
    print()
    print('── 일반 신발 Lv20 까지(재료 3켤레 · 같은 등급 Lv1 재료) ──')
    att = sum(1 / FORGE_OK(l) for l in range(1, 20))
    fee = sum(FORGE_FEE * l / FORGE_OK(l) for l in range(1, 20))
    shoes = att * 3
    for free, buy in ((1, 0), (2, 0), (3, 0), (3, 1), (3, 3)):
        days = shoes / (free + buy)
        print('하루 무료 %d + 구매 %d → %3.0f일, 상자 값 합 %5.0f SUP' % (free, buy, days, days * buy * BOX_PRICE))
    print('(선택) 강화 수수료 %d × 레벨이면 Lv20 까지 합 약 %.0f SUP — 시도 %.1f번' % (FORGE_FEE, fee, att))
    print()
    print('── 주력 신발을 고쳐 신기 vs 새 신발(상자 %d SUP)을 신어 버리기 — 걷기 30분 ──' % BOX_PRICE)
    for lvl in (1, 10, 20):
        main_e = 3300 * PPS * (1 + EFF0['일반'] + EFF_PER_PT * 4 * (lvl - 1))
        fresh_e = 3300 * PPS * (1 + EFF0['일반'])
        rep = 150 * UNIT['일반'] * (1 + LEVEL_STEP * lvl)
        print('주력 Lv%-2d: 고쳐 신으면 %.0f − 수리 %.0f = %.0f · 새 신발은 %.0f − 상자 값 %d = %.0f'
              % (lvl, main_e, rep, main_e - rep, fresh_e, BOX_PRICE, fresh_e - BOX_PRICE))


if __name__ == '__main__':
    main()
