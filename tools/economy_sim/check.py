#!/usr/bin/env python3
"""경제 불변식 검사 — 실제 서버 규칙(setup.sql)으로 하루 플레이를 돌려, 숫자를 바꿔도 다음이 늘 지켜지는지 본다.

  1. 어떤 신발로든 하루 에너지를 다 쓰면 남는다(걷기도) — 운이 가장 나쁜 날(보상 × 0.7 · 닳음 × 1.444)에도.
  2. 강화하면 하루에 남는 돈이 줄지 않는다(하루 상한에 닿은 칸은 뺀다).
  3. 새 사용자는 첫 러닝(걷기 10분) 뒤에 수리할 수 있다.
  4. 신발을 갈아 신는 것만으로 남은 에너지가 사라지지 않는다.

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
        if len(p) >= 12 and p[0] in GRADES:
            rows.append(dict(rarity=p[0], lvl=int(p[1]), mode=p[2], earned=float(p[7]), repair=float(p[8]),
                             net=float(p[9]), reasons=p[12] if len(p) > 12 else ''))
    if len(rows) < 60:
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
                bad.append(f"[1] {label} {r['rarity']} Lv{r['lvl']} {r['mode']}: 남는 돈 {r['net']}")
    by = {}
    for r in mean:
        by.setdefault((r['rarity'], r['mode']), []).append(r)
    for (rarity, mode), rs in by.items():
        rs.sort(key=lambda r: r['lvl'])
        for a, b in zip(rs, rs[1:]):
            if b['net'] < a['net'] and '상한' not in b['reasons']:
                bad.append(f"[2] {rarity} {mode}: Lv{a['lvl']} {a['net']} → Lv{b['lvl']} {b['net']}")

    if not re.search(r'\[수리\] 됨', flows):
        bad.append('[3] 새 사용자가 첫 러닝 뒤에 수리하지 못한다:\n' + '\n'.join(l for l in flows.splitlines() if l.startswith('[')))
    m = re.search(r'\[Lv1 새 신발로 갈아 신음\] 남은 에너지 ([0-9.]+)', flows)
    if not m or float(m.group(1)) <= 0:
        bad.append('[4] 신발을 갈아 신었더니 남은 에너지가 사라졌다')

    for r in sorted(mean, key=lambda r: (GRADES.index(r['rarity']), r['lvl'], r['mode'])):
        print(f"{r['rarity']:<9} Lv{r['lvl']:<2} {r['mode']:<4} 적립 {r['earned']:>8.1f} 수리 {r['repair']:>7.1f} 남음 {r['net']:>8.1f}")
    print('가장 나쁜 운에서 가장 적게 남는 칸:', min(worst, key=lambda r: r['net']))
    if bad:
        print('\n'.join(bad))
        sys.exit(1)
    print('경제 불변식 넷 — 모두 지켜진다')


if __name__ == '__main__':
    main()
