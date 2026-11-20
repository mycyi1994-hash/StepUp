#!/usr/bin/env python3
"""경제 4판 쌍둥이 — 앱의 [ShoeRules.FALLBACK](domain/EconomyV4.kt)이 서버 0116 · 0118 · 0119 와 같은 값인지 맞대어 본다.

앱은 서버(shoe_rules_v4)를 읽기 전까지 FALLBACK 으로 지수 · 별 · 포인트 미리보기를 센다. 서버만 고치고 앱을 잊으면
처음 몇 초 동안 다른 숫자가 보인다. 서버 값을 바꾸면 둘을 함께 고친다.

0118(강화 경험치 · 오늘의 보상, 2026-11-19): `forge` 묶음(계단 · 필요 · 스펙 · 대성공 · 물려줌 · 수리비 단위 · 최대 켤레 · 규칙 판)과
`goal` 묶음(목표 범위 · 새 계정 · 단위)도 맞대어 본다. 설정 값은 마이그레이션 차례대로 — 처음 넣은 값(on conflict do nothing)에
뒤의 update 가 덮는다(예: 0118 이 forge_material_min_durability 를 50 → 100).

0119(경제의 뿌리, 2026-11-21): `stats` 묶음 — 닳은 뒤 적립 비율 · 효율 포인트가 닳음 / 에너지를 올리는 세기 · 내구도 포인트와 수리비 ·
레벨마다 에너지 · 분당 닳음 · 다시 나누기 간격([StatRule.FALLBACK]).
"""
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SQL = (ROOT / 'supabase/migrations/0116_economy_v4.sql').read_text()
SQL_0118_PATH = ROOT / 'supabase/migrations/0118_forge_xp_daily.sql'
if not SQL_0118_PATH.exists():
    sys.exit('서버 0118(supabase/migrations/0118_forge_xp_daily.sql)을 찾지 못했다 — 앱 FALLBACK 의 강화 · 목표 값은 0118 과 맞대어 본다')
SQL_0118 = re.sub(r'--[^\n]*', '', SQL_0118_PATH.read_text())
SQL_0119_PATH = ROOT / 'supabase/migrations/0119_economy_roots.sql'
if not SQL_0119_PATH.exists():
    sys.exit('서버 0119(supabase/migrations/0119_economy_roots.sql)를 찾지 못했다 — 앱 StatRule.FALLBACK 은 0119 와 맞대어 본다')
SQL_0119 = re.sub(r'--[^\n]*', '', SQL_0119_PATH.read_text())
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
# 0118 — 새 설정(처음 넣은 값이 남는다: on conflict do nothing) · 예전 설정을 바꾸는 update(덮는다)
SETTING_ROW = re.compile(r"\(\s*'([a-z_0-9]+)'\s*,\s*'([^']*)'(?:::jsonb)?\s*\)")
SETTING_UPDATE = re.compile(
    r"update\s+public\.economy_settings\s+set\s+value\s*=\s*'([^']*)'(?:::jsonb)?[^;]*?where\s+key\s*=\s*'([a-z_0-9]+)'", re.I | re.S)
for block in re.findall(r"insert\s+into\s+public\.economy_settings\s*\(\s*key\s*,\s*value\s*\)\s*values(.*?);", SQL_0118, re.I | re.S):
    for k, v in SETTING_ROW.findall(block):
        settings.setdefault(k, v)
for block in re.findall(r"insert\s+into\s+public\.economy_settings\s*\(\s*key\s*,\s*value\s*\)\s*values(.*?);", SQL_0119, re.I | re.S):
    for k, v in SETTING_ROW.findall(block):
        settings.setdefault(k, v)
for v, k in SETTING_UPDATE.findall(SQL + SQL_0118 + SQL_0119):
    settings[k] = v
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

# ── 0118 강화 경험치 · 목표 ─────────────────────────────────────────
def fallback_block(cls):
    m = re.search(r'val FALLBACK = ' + cls + r'\((.*?)\n\s*\)', KT, re.S) or re.search(r'val FALLBACK = ' + cls + r'\(([^)]*)\)', KT)
    if not m:
        sys.exit(f'앱 {cls}.FALLBACK 을 찾지 못했다')
    return m.group(1)


def kt_value(block, name):
    m = re.search(r'\b' + name + r'\s*=\s*("[^"]*"|listOf\([^)]*\)|-?[0-9.]+)', block)
    if not m:
        sys.exit(f'앱 FALLBACK 에서 {name} 을 찾지 못했다')
    v = m.group(1)
    if v.startswith('"'):
        return v.strip('"')
    if v.startswith('listOf'):
        return [float(x) for x in v[len('listOf('):-1].split(',')]
    return float(v)


def srv_value(key):
    if key not in settings:
        bad.append(f'설정 {key}: 서버 0118 에 없다')
        return None
    raw = settings[key].strip()
    if raw.startswith('['):
        return [float(x) for x in raw.strip('[]').split(',')]
    return float(raw.strip('"'))


forge = fallback_block('ForgeXpRule')
goal = fallback_block('GoalRule')
twins = [
    (forge, 'step', 'forge_xp_step'),
    (forge, 'need', 'forge_xp_need'),
    (forge, 'spec', 'forge_xp_spec'),
    (forge, 'critChance', 'forge_xp_crit_chance'),
    (forge, 'critMult', 'forge_xp_crit_mult'),
    (forge, 'inherit', 'forge_xp_inherit'),
    (forge, 'supPerUnit', 'forge_xp_sup_per_unit'),
    (forge, 'maxMaterials', 'forge_xp_max_materials'),
    (goal, 'min', 'goal_min'),
    (goal, 'max', 'goal_max'),
    (goal, 'default', 'goal_default'),
]
for block, name, key in twins:
    app_v, srv_v = kt_value(block, name), srv_value(key)
    if srv_v is not None and app_v != srv_v:
        bad.append(f'설정 {key}: 앱 {app_v} · 서버 {srv_v}')

# 목표 단위 — 설정(goal_step)이 있으면 그 값, 없으면 shoe_rules_v4 의 goal 묶음에 적힌 'step', N
goal_step = kt_value(goal, 'step')
if 'goal_step' in settings:
    srv_step = float(settings['goal_step'])
else:
    m = re.search(r"'goal'\s*,\s*jsonb_build_object\((.*?)'step'\s*,\s*(\d+)", SQL_0118, re.S)
    srv_step = float(m.group(2)) if m else None
if srv_step is None:
    bad.append('목표 단위: 서버 0118 의 goal 묶음에서 step 을 찾지 못했다')
elif srv_step != goal_step:
    bad.append(f'목표 단위: 앱 {goal_step} · 서버 {srv_step}')

# 규칙 판 — 서버가 돌려주는 rule_version 글자
rule_version = kt_value(forge, 'ruleVersion')
if f"'{rule_version}'" not in SQL_0118:
    bad.append(f'강화 규칙 판: 앱 {rule_version} 이 서버 0118 에 없다')

# ── 0119 능력치 규칙 ────────────────────────────────────────────────
stat = fallback_block('StatRule')
stat_twins = [('wornRate', 'worn_rate'), ('effWearExp', 'eff_wear_exp'), ('effEnergyExp', 'eff_energy_exp'),
              ('durRepairExp', 'dur_repair_exp'), ('energyPerLevel', 'energy_per_level')]
for name, key in stat_twins:
    app_v, srv_v = kt_value(stat, name), srv_value(key)
    if srv_v is not None and app_v != srv_v:
        bad.append(f'설정 {key}: 앱 {app_v} · 서버 {srv_v}')
# 규칙 묶음에 글자로 적힌 값 — 'wear_per_minute', 5 · 'respec_every', 5
for name, key in (('wearPerMinute', 'wear_per_minute'), ('respecEvery', 'respec_every')):
    m = re.search(r"'" + key + r"'\s*,\s*(\d+)", SQL_0119)
    if not m:
        bad.append(f'{key}: 서버 0119 의 stats 묶음에서 찾지 못했다')
    elif float(m.group(1)) != kt_value(stat, name):
        bad.append(f'{key}: 앱 {kt_value(stat, name)} · 서버 {m.group(1)}')
# 하루 에너지 기본값 · 에너지 1 의 걸음 — 0003 의 함수
CORE = (ROOT / 'supabase/migrations/0003_ledger.sql').read_text()
for name, fn in (('energyBase', 'base_max_energy'), ('stepsPerEnergy', 'steps_per_energy')):
    m = re.search(r'function economy\.' + fn + r'\(\)[^$]*\$\$\s*select\s+([0-9.]+)', CORE, re.S)
    if not m:
        bad.append(f'{fn}: 서버 0003 에서 찾지 못했다')
    elif float(m.group(1)) != kt_value(stat, name):
        bad.append(f'{fn}: 앱 {kt_value(stat, name)} · 서버 {m.group(1)}')

if bad:
    print('앱 FALLBACK 과 서버 0116 · 0118 · 0119 가 다르다:\n  ' + '\n  '.join(bad))
    sys.exit(1)
print(f'경제 4판 쌍둥이 — 등급 {len(GRADES)} · 설정 {len(kt_values) + 3} · 강화 · 목표 {len(twins) + 2} · 능력치 규칙 {len(stat_twins) + 4} 모두 같다')
