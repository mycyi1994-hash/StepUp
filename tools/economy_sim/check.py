#!/usr/bin/env python3
"""경제 불변식 검사 — 실제 서버 규칙(setup.sql)으로 하루 플레이를 돌려, 숫자를 바꿔도 다음이 늘 지켜지는지 본다.

  1. 어떤 신발로든 하루 에너지를 다 쓰면 남는다(걷기도) — 운이 가장 나쁜 날(보상 × 0.7 · 닳음 × 1.444)에도.
  2. 강화하면 하루에 남는 돈이 줄지 않는다(하루 상한에 닿은 칸은 뺀다).
  3. 새 사용자는 첫 러닝(걷기 10분) 뒤에 수리할 수 있다.
  4. 신발을 갈아 신는 것만으로 남은 에너지가 사라지지 않는다.
  5. 닳은 재료(강화 경험치, 0118): 수리하고 넣기 = 그냥 넣기 — 수리비만큼 제 값이 오른다(다 닳아 0 이면 수리해도 손해).
  6. 기준 러너(0119): 새 사용자가 첫 신발 그대로 한 번에 걷기 55분 · 달리기 37분이면 그 날의 기본 목표를 채운다.
  7. 수리하는 쪽이 늘 낫다(0119): 수리하며 달려 남는 돈 > 다 닳은 신발로 같은 에너지를 써서 버는 돈(적립의 worn_rate).
  8. 예전 앱의 확률 강화가 새 경험치 강화보다 싸지 않다(0119 — 같은 굽이).
포인트는 네 가지로 나눠 본다(고르게 · 효율만 · 내구도만 · 착화감만) — 1 · 2 · 7 은 넷 모두에서.

무료 수리는 끄고 잰다(가장 나쁜 쪽). 깨지면 1 로 끝난다 — CI(.github/workflows/test-sql.yml)가 돌린다.

    python3 tools/economy_sim/check.py
"""
import os
import re
import subprocess
import sys
import tempfile

HERE = os.path.dirname(os.path.abspath(__file__))
GRADES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']
ALLOCS = ['EVEN', 'EFF', 'DUR', 'COMFORT']
WORN_RATE = 0.30      # 서버 설정 worn_rate 의 기본값 — 바꾸면 여기도

BASE = """
update public.economy_settings set value = '0'::jsonb where key = 'free_repair_hours';
update public.economy_settings set value = '{reward}'::jsonb where key = 'reward_luck';
update public.economy_settings set value = '{wear}'::jsonb where key = 'wear_luck';
"""


def run(label, reward, wear):
    with tempfile.NamedTemporaryFile('w', suffix='.sql', delete=False) as f:
        f.write(BASE.format(reward=reward, wear=wear))
        tweak = f.name
    os.chmod(tweak, 0o644)
    out = subprocess.run(['bash', os.path.join(HERE, 'run.sh'), tweak], capture_output=True, text=True)
    os.unlink(tweak)
    if out.returncode != 0:
        print(out.stdout[-3000:], out.stderr[-3000:])
        sys.exit(f'{label}: 시뮬레이션이 돌지 않았다')
    rows = []
    for line in out.stdout.splitlines():
        p = [x.strip() for x in line.split('|')]
        if len(p) >= 13 and p[0] in GRADES:
            rows.append(dict(rarity=p[0], lvl=int(p[1]), mode=p[2], alloc=p[3], earned=float(p[8]), repair=float(p[9]),
                             net=float(p[10]), reasons=p[13] if len(p) > 13 else ''))
    if len(rows) < 240:
        print(out.stdout[-3000:])
        sys.exit(f'{label}: 표를 읽지 못했다({len(rows)}줄)')
    return out.stdout, rows


def main():
    bad = []
    flows, mean = run('평균 운', '[1, 1, 1]', '[1, 1, 1]')
    _, worst = run('가장 나쁜 운', '[0.7, 0.7, 0.7]', '[1.444, 1.444, 1.444]')

    for label, rows in (('평균 운', mean), ('가장 나쁜 운', worst)):
        for r in rows:
            if r['net'] <= 0:
                bad.append(f"[1] {label} {r['rarity']} Lv{r['lvl']} {r['mode']} {r['alloc']}: 남는 돈 {r['net']}")
    # 2. Lv1 줄(포인트 없음)은 고르게(EVEN)에만 있다 — 다른 배분의 첫 칸으로도 쓴다.
    #    효율만 넣은 신발은 하루에 버는 양이 같다(같은 돈을 더 짧은 시간에) — 분 단위로 끊겨 생기는 차이 2% 는 봐준다
    lv1 = {(r['rarity'], r['mode']): r for r in mean if r['lvl'] == 1}
    by = {}
    for r in mean:
        if r['lvl'] > 1:
            by.setdefault((r['rarity'], r['mode'], r['alloc']), []).append(r)
    for (rarity, mode, alloc), rs in by.items():
        rs = [lv1[(rarity, mode)]] + sorted(rs, key=lambda r: r['lvl'])
        for a, b in zip(rs, rs[1:]):
            slack = 0.98 if alloc == 'EFF' else 1.0
            if b['net'] < a['net'] * slack and '상한' not in b['reasons']:
                bad.append(f"[2] {rarity} {mode} {alloc}: Lv{a['lvl']} {a['net']} → Lv{b['lvl']} {b['net']}")
    # 7. 수리하지 않고 다 닳은 신발로 같은 에너지를 쓰면 적립의 30%(worn_rate)를 수리비 없이 번다 — 수리하는 쪽이 그보다 남아야 한다
    for r in mean:
        if r['net'] <= WORN_RATE * r['earned'] * 1.1:
            bad.append(f"[7] {r['rarity']} Lv{r['lvl']} {r['mode']} {r['alloc']}: 수리하며 {r['net']} ≤ 안 고치고 {WORN_RATE * r['earned']:.1f}")

    if not re.search(r'\[수리\] 됨', flows):
        bad.append('[3] 새 사용자가 첫 러닝 뒤에 수리하지 못한다:\n' + '\n'.join(l for l in flows.splitlines() if l.startswith('[')))
    m0 = re.search(r'\[Lv10 신발로 30분 달림\] 남은 에너지 ([0-9.]+)', flows)
    m = re.search(r'\[Lv1 새 신발로 갈아 신음\] 남은 에너지 ([0-9.]+)', flows)
    if not m or not m0 or float(m.group(1)) <= 0 or float(m.group(1)) < float(m0.group(1)) - 0.001:
        bad.append(f'[4] 신발을 갈아 신었더니 남은 에너지가 사라졌다({m0 and m0.group(1)} → {m and m.group(1)})')

    # 6. 기준 러너 — 새 사용자의 첫날
    first = re.findall(r'\[첫날 (\S+) (\d+)분\] 보상 걸음 (\d+) / 목표 (\d+) · \+([0-9.]+) SUP', flows)
    if len(first) < 2:
        bad.append('[6] 첫날 줄을 읽지 못했다')
    for mode, mins, got, goal, sup in first:
        if int(got) < int(goal) or float(sup) <= 0:
            bad.append(f'[6] 새 사용자 {mode} {mins}분: 보상 걸음 {got} / 목표 {goal} · +{sup} SUP')

    # 8. 예전 앱의 확률 강화(재료 3켤레의 평균 비용) ≥ 통 — 1% 는 반올림
    oldnew = re.findall(r'^OLDNEW\|(\w+)\|(\d+)\|(\w+)\|(\d+)\|(\d+)$', flows, re.M)
    if len(oldnew) < 100:
        bad.append(f'[8] 예전 · 새 강화 표를 읽지 못했다({len(oldnew)}줄)')
    for g, lv, kind, old, new in oldnew:
        if int(old) < int(new) * 0.99:
            bad.append(f'[8] {g} Lv{lv} {kind}: 예전 앱 {old} < 새 강화 {new}')

    # 5. 닳은 재료 — forge.sql 의 REPAIR 줄: 닳은 채 제 값 + 수리비(SUP ÷ 24 × 1,000) = 가득 제 값
    repairs = re.findall(r'^REPAIR\|(\w+)\|(\w+)\|(\d+)\|(\d+)\|([0-9.]+)$', flows, re.M)
    if len({g for g, *_ in repairs}) < 4:
        bad.append(f'[5] 닳은 재료 표를 읽지 못했다({len(repairs)}줄)')
    for g, state, worn, full, cost in repairs:
        worn, full, part = int(worn), int(full), float(cost) / 24 * 1000
        if worn > 0 and abs(worn + part - full) > 2:
            bad.append(f'[5] {g} {state}: 닳은 채 {worn} + 수리 {part:.0f} ≠ 가득 {full}')
        if worn == 0 and full - part > 2:
            bad.append(f'[5] {g} {state}: 다 닳아 0 인데 수리하면 {full - part:.0f} 만큼 이득')

    for r in sorted(mean, key=lambda r: (GRADES.index(r['rarity']), r['lvl'], r['mode'], ALLOCS.index(r['alloc']))):
        print(f"{r['rarity']:<9} Lv{r['lvl']:<2} {r['mode']:<4} {r['alloc']:<7} 적립 {r['earned']:>8.1f} 수리 {r['repair']:>7.1f} 남음 {r['net']:>8.1f}")
    print('가장 나쁜 운에서 가장 적게 남는 칸:', min(worst, key=lambda r: r['net']))
    for mode, mins, got, goal, sup in first:
        print(f'새 사용자 첫날 {mode} {mins}분: 보상 걸음 {got} / 목표 {goal} · +{sup} SUP')
    if bad:
        print('\n'.join(bad))
        sys.exit(1)
    print('경제 불변식 여덟 — 모두 지켜진다')


if __name__ == '__main__':
    main()
