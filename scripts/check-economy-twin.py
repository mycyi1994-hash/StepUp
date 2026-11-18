#!/usr/bin/env python3
"""경제 4판 쌍둥이 — 앱의 [ShoeRules.FALLBACK](domain/EconomyV4.kt)이 서버 0116 과 같은 값인지 맞대어 본다.

앱은 서버(shoe_rules_v4)를 읽기 전까지 FALLBACK 으로 지수 · 별 · 포인트 미리보기를 센다. 서버만 고치고 앱을 잊으면
처음 몇 초 동안 다른 숫자가 보인다. 서버 값을 바꾸면 둘을 함께 고친다.
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SQL = (ROOT / 'supabase/migrations/0116_economy_v4.sql').read_text()
MAXL = (ROOT / 'supabase/migrations/0066_shoe_economy_v3.sql').read_text()
KT = (ROOT / 'app/src/main/java/com/stepup/android/domain/EconomyV4.kt').read_text()
GRADES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']


def fn_body(src, name):
    m = re.search(r'create or replace function economy\.' + re.escape(name) + r'\b.*?\$\$(.*?)\$\$', src, re.S)
    if not m:
        sys.exit(f'서버 함수 {name} 를 찾지 못했다')
    return m.group(1)


def cases(body):
    """case p_rarity when 'X' then N … else N end 를 [lo, hi] 두 줄로"""
    out = []
    for block in re.findall(r'case p_rarity(.*?)end', body, re.S):
        d = dict(re.findall(r"when '([A-Z]+)' then (-?\d+)", block))
        e = re.search(r'else (-?\d+)', block)
        out.append([int(d.get(g, e.group(1) if e else 0)) for g in GRADES])
    return out


server = {}
eff = cases(fn_body(SQL, 'efficiency_range'))
dur = cases(fn_body(SQL, 'durability_range'))
ppl = cases(fn_body(SQL, 'points_per_level_of'))[0]
mile = cases(fn_body(SQL, 'points_milestone_of'))[0]
rate = cases(fn_body(SQL, 'point_rate_of'))[0]
maxl = cases(fn_body(MAXL, 'max_level'))[0]
for i, g in enumerate(GRADES):
    lo = (140 + 10 * i) / 1.4
    server[g] = (eff[0][i], eff[1][i], round(10000 * (1 - 100 / lo)), round(10000 * (1 - 100 / (lo * 1.8))),
                 dur[0][i], dur[1][i], ppl[i], mile[i], rate[i], maxl[i])

app = {}
for m in re.finditer(r'GradeRule\("([A-Z]+)",\s*([^)]*)\)', KT):
    nums = [int(x.strip()) for x in m.group(2).split(',')]
    app[m.group(1)] = tuple(nums)

bad = []
for g in GRADES:
    if app.get(g) != server[g]:
        bad.append(f'{g}: 앱 {app.get(g)} · 서버 {server[g]}')

settings = dict(re.findall(r"\('([a-z_]+)', '([^']*)'::jsonb\)", SQL))
kt_values = {
    'points_per_step': re.search(r'pointsPerStep = ([0-9.]+)', KT).group(1),
    'repair_share': re.search(r'repairShare = ([0-9.]+)', KT).group(1),
    'free_repair_hours': re.search(r'freeRepairHours = ([0-9.]+)', KT).group(1),
    'forge_material_min_durability': re.search(r'forgeMaterialMinDurability = ([0-9.]+)', KT).group(1),
}
for k, v in kt_values.items():
    if float(settings[k]) != float(v):
        bad.append(f'설정 {k}: 앱 {v} · 서버 {settings[k]}')
for k, kname in (('reward_luck', 'rewardLuck'), ('wear_luck', 'wearLuck')):
    srv = [float(x) for x in settings[k].strip('[]').split(',')]
    kt = [float(x) for x in re.search(kname + r' = listOf\(([^)]*)\)', KT).group(1).split(',')]
    if srv != kt:
        bad.append(f'설정 {k}: 앱 {kt} · 서버 {srv}')
cap = re.search(r'comfort_cap_bps\(\) returns integer\s*language sql immutable as \$\$ select (\d+)', SQL).group(1)
if int(cap) != int(re.search(r'comfortCapBps = (\d+)', KT).group(1)):
    bad.append('착화감 상한이 다르다')

if bad:
    print('앱 FALLBACK 과 서버 0116 이 다르다:\n  ' + '\n  '.join(bad))
    sys.exit(1)
print(f'경제 4판 쌍둥이 — 등급 {len(GRADES)} · 설정 {len(kt_values) + 3} 모두 같다')
