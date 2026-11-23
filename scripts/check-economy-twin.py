#!/usr/bin/env python3
"""경제 4판 쌍둥이 — 앱의 [ShoeRules.FALLBACK](domain/EconomyV4.kt)이 서버 0116 · 0118 · 0119 · 0120 과 같은 값인지 맞대어 본다.

앱은 서버(shoe_rules_v4)를 읽기 전까지 FALLBACK 으로 지수 · 별 · 포인트 미리보기를 센다. 서버만 고치고 앱을 잊으면
처음 몇 초 동안 다른 숫자가 보인다. 서버 값을 바꾸면 둘을 함께 고친다.

0118(강화 경험치 · 오늘의 보상, 2026-11-19): `forge` 묶음(계단 · 필요 · 스펙 · 대성공 · 물려줌 · 수리비 단위 · 최대 켤레 · 규칙 판)과
`goal` 묶음(목표 범위 · 새 계정 · 단위)도 맞대어 본다. 설정 값은 마이그레이션 차례대로 — 처음 넣은 값(on conflict do nothing)에
뒤의 update 가 덮는다(예: 0118 이 forge_material_min_durability 를 50 → 100).

0119(경제의 뿌리, 2026-11-21): `stats` 묶음 — 닳은 뒤 적립 비율 · 효율 포인트가 닳음 / 에너지를 올리는 세기 · 내구도 포인트와 수리비 ·
레벨마다 에너지 · 분당 닳음 · 다시 나누기 간격([StatRule.FALLBACK]).

2026-11-22 경제 점검: 앱 상수 가운데 서버 규칙을 따라 쓴 것 — `RewardEconomy`(걸음당 SUP · 에너지 1 의 걸음 · 착화감 상한으로 정해지는
가장 작은 에너지 비율 · 기본 하루 에너지 · 인원 배수 · 내구도 1 의 초 · 에너지를 분으로 옮기는 분당 걸음)와 러너 레벨(0121 · 0122 —
`RunnerLevel` 의 만렙 · 한 레벨 걸음 · 배지 레벨)도 맞대어 본다. 서버 함수는 마이그레이션 가운데 마지막 정의를, 설정은 0121 부터
차례대로(처음 넣은 값 → 뒤의 update 가 덮는다 — 0122 가 runner_energy_cap 16.5 → 10) 읽는다. 러너 레벨의 하루 에너지(runner_energy_base ·
runner_energy_per_level · runner_energy_cap)는 앱이 숫자로 갖고 있지 않고 서버 값(runner_me · my_economy)을 읽는다 — 여기서는 읽은 값만 보인다.
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

# ── 0120 함께 달린 사람 · 하루 최대 ────────────────────────────────
SQL_0120_PATH = ROOT / 'supabase/migrations/0120_together_bonus_fix.sql'
if not SQL_0120_PATH.exists():
    sys.exit('서버 0120(supabase/migrations/0120_together_bonus_fix.sql)을 찾지 못했다 — 앱 TogetherRule · DailyMaxRule.FALLBACK 은 0120 과 맞대어 본다')
SQL_0120 = re.sub(r'--[^\n]*', '', SQL_0120_PATH.read_text())
s120 = {}
for block in re.findall(r"insert\s+into\s+public\.economy_settings\s*\(\s*key\s*,\s*value\s*\)\s*values(.*?);", SQL_0120, re.I | re.S):
    for k, v in SETTING_ROW.findall(block):
        s120.setdefault(k, v.strip().strip('"'))
together = fallback_block('TogetherRule')
daily = fallback_block('DailyMaxRule')
together_twins = [
    (together, 'rule', 'crew_bonus_rule'),
    (together, 'minMinutes', 'together_min_minutes'),
    (together, 'minSteps', 'together_min_steps'),
    (together, 'goalShare', 'together_goal_share'),
    (together, 'topupHours', 'together_topup_hours'),
    (together, 'blendShare', 'crew_blend_share'),
    (daily, 'rule', 'daily_max_rule'),
    (daily, 'hourSteps', 'daily_max_hour_steps'),
    (daily, 'hourMult', 'daily_max_hour_mult'),
]
for block, name, key in together_twins:
    app_v = kt_value(block, name)
    if key not in s120:
        bad.append(f'설정 {key}: 서버 0120 에 없다')
        continue
    srv_v = s120[key] if isinstance(app_v, str) else float(s120[key])
    if app_v != srv_v:
        bad.append(f'설정 {key}: 앱 {app_v} · 서버 {srv_v}')
m = re.search(r"'goal_included'\s*,\s*(true|false)", SQL_0120)
app_goal = re.search(r'goalIncluded\s*=\s*(true|false)', daily)
if not m or not app_goal:
    bad.append('goal_included: 서버 0120 의 daily_max 묶음이나 앱 DailyMaxRule.FALLBACK 에서 찾지 못했다')
elif m.group(1) != app_goal.group(1):
    bad.append(f'goal_included: 앱 {app_goal.group(1)} · 서버 {m.group(1)}')

# ── 앱 상수(RewardEconomy) · 러너 레벨(0121 · 0122) ─────────────────
MIGRATIONS = sorted((ROOT / 'supabase/migrations').glob('[0-9][0-9][0-9][0-9]_*.sql'))


def last_fn(schema, name):
    """마이그레이션 가운데 마지막 `create or replace function schema.name` 의 본문($$ … $$)"""
    pattern = re.compile(r'create\s+or\s+replace\s+function\s+' + schema + r'\.' + re.escape(name) + r'\s*\(.*?\$\$(.*?)\$\$', re.S | re.I)
    body = None
    for path in MIGRATIONS:
        for m in pattern.finditer(re.sub(r'--[^\n]*', '', path.read_text())):
            body = m.group(1)
    if body is None:
        bad.append(f'서버 함수 {schema}.{name} 를 마이그레이션에서 찾지 못했다')
    return body or ''


def effective_settings(paths):
    """설정의 마지막 값 — 파일 차례 · 글 차례대로 insert(on conflict do nothing 이면 처음 값만) · update(덮는다)"""
    out = {}
    insert = re.compile(r"insert\s+into\s+public\.economy_settings\s*\(\s*key\s*,\s*value\s*\)\s*values(.*?);", re.I | re.S)
    for path in paths:
        sql = re.sub(r'--[^\n]*', '', path.read_text())
        events = [(m.start(), 'insert', m) for m in insert.finditer(sql)] + [(m.start(), 'update', m) for m in SETTING_UPDATE.finditer(sql)]
        for _, kind, m in sorted(events, key=lambda e: e[0]):
            if kind == 'insert':
                overwrite = re.search(r'do\s+update', m.group(1), re.I) is not None
                for k, v in SETTING_ROW.findall(m.group(1)):
                    if overwrite:
                        out[k] = v.strip().strip('"')
                    else:
                        out.setdefault(k, v.strip().strip('"'))
            else:
                out[m.group(2)] = m.group(1).strip().strip('"')
    return out


def kt_const(src, name):
    m = re.search(r'\b(?:const\s+)?val\s+' + name + r'\s*(?::\s*[A-Za-z<>]+)?\s*=\s*([0-9_.]+)L?\b', src)
    if not m:
        bad.append(f'앱 상수 {name} 를 찾지 못했다')
        return None
    return float(m.group(1).replace('_', ''))


def sql_number(body, regex, what):
    m = re.search(regex, body, re.S | re.I)
    if not m:
        bad.append(f'{what}: 서버에서 찾지 못했다')
        return None
    return [float(g) for g in m.groups()]


RE_KT = (ROOT / 'app/src/main/java/com/stepup/android/domain/RewardEconomy.kt').read_text()
RL_KT = (ROOT / 'app/src/main/java/com/stepup/android/domain/RunnerLevel.kt').read_text()
const_twins = []


def twin(label, app_v, srv_v):
    const_twins.append(label)
    if app_v is not None and srv_v is not None and abs(app_v - srv_v) > 1e-9:
        bad.append(f'{label}: 앱 {app_v:g} · 서버 {srv_v:g}')


spe = sql_number(last_fn('economy', 'steps_per_energy'), r'select\s+([0-9.]+)', 'steps_per_energy')
twin('RewardEconomy.STEPS_PER_ENERGY ↔ economy.steps_per_energy()', kt_const(RE_KT, 'STEPS_PER_ENERGY'), spe and spe[0])
base = sql_number(last_fn('economy', 'base_max_energy'), r'select\s+([0-9.]+)', 'base_max_energy')
twin('RewardEconomy.BASE_MAX_ENERGY ↔ economy.base_max_energy()', kt_const(RE_KT, 'BASE_MAX_ENERGY'), base and base[0])
cap_bps = sql_number(last_fn('economy', 'comfort_cap_bps'), r'select\s+([0-9]+)', 'comfort_cap_bps')
twin('RewardEconomy.MIN_ENERGY_FACTOR ↔ 1 − economy.comfort_cap_bps() ÷ 10000', kt_const(RE_KT, 'MIN_ENERGY_FACTOR'),
     cap_bps and round(1 - cap_bps[0] / 10000, 9))
party = sql_number(last_fn('economy', 'party_multiplier'),
                   r'select\s+1\s*\+\s*([0-9.]+)\s*\*\s*least\(.*?,\s*([0-9]+)\s*\)\s*$', 'party_multiplier')
twin('RewardEconomy.PARTY_BONUS_RATE ↔ economy.party_multiplier()', kt_const(RE_KT, 'PARTY_BONUS_RATE'), party and party[0])
twin('RewardEconomy.PARTY_BONUS_CAP ↔ economy.party_multiplier()', kt_const(RE_KT, 'PARTY_BONUS_CAP'), party and party[1])
sec = sql_number(last_fn('economy', 'durability_sec_per_point'), r'select\s+([0-9]+)', 'durability_sec_per_point')
twin('RewardEconomy.DURABILITY_SEC_PER_POINT ↔ economy.durability_sec_per_point()', kt_const(RE_KT, 'DURABILITY_SEC_PER_POINT'), sec and sec[0])
twin('RewardEconomy.POINTS_PER_STEP ↔ points_per_step', kt_const(RE_KT, 'POINTS_PER_STEP'), float(settings['points_per_step']))
# 에너지 → 분: runner_me 의 energy_minutes 들이 모두 같은 분당 걸음(÷ 165)으로 나눈다
runner_me = last_fn('public', 'runner_me')
per_min = {float(x) for x in re.findall(r'v_spe\s*/\s*([0-9.]+)', runner_me)}
if len(per_min) != 1:
    bad.append(f'runner_me 의 분당 걸음: 한 값이 아니다 {sorted(per_min)}')
else:
    twin('RewardEconomy.RUN_STEPS_PER_MINUTE ↔ runner_me() energy_minutes ÷ N', kt_const(RE_KT, 'RUN_STEPS_PER_MINUTE'), per_min.pop())

# 러너 레벨 설정 — 0121 부터 차례대로(0122 가 runner_energy_cap 을 16.5 → 10)
runner = effective_settings([p for p in MIGRATIONS if p.name >= '0121'])
for key in ('runner_level_steps', 'runner_level_max', 'runner_energy_base', 'runner_energy_per_level', 'runner_energy_cap'):
    if key not in runner:
        bad.append(f'설정 {key}: 서버 0121 · 0122 에서 찾지 못했다')
if 'runner_level_steps' in runner:
    twin('RunnerLevel.DEFAULT_LEVEL_STEPS ↔ runner_level_steps', kt_const(RL_KT, 'DEFAULT_LEVEL_STEPS'), float(runner['runner_level_steps']))
if 'runner_level_max' in runner:
    twin('RunnerLevel.DEFAULT_MAX_LEVEL ↔ runner_level_max', kt_const(RL_KT, 'DEFAULT_MAX_LEVEL'), float(runner['runner_level_max']))
srv_badges = re.search(r"'badge_levels'\s*,\s*jsonb_build_array\(([^)]*)\)", runner_me)
app_badges = re.search(r'DEFAULT_BADGE_LEVELS\s*:\s*List<Int>\s*=\s*listOf\(([^)]*)\)', RL_KT)
const_twins.append('RunnerLevel.DEFAULT_BADGE_LEVELS ↔ runner_me() badge_levels')
if not srv_badges or not app_badges:
    bad.append('배지 레벨: runner_me 의 badge_levels 나 앱 RunnerLevel.DEFAULT_BADGE_LEVELS 를 찾지 못했다')
elif [int(x) for x in srv_badges.group(1).split(',')] != [int(x) for x in app_badges.group(1).split(',')]:
    bad.append(f'배지 레벨: 앱 {app_badges.group(1)} · 서버 {srv_badges.group(1)}')

if bad:
    print('앱과 서버(0116 · 0118 · 0119 · 0120 · 0121 · 0122)가 다르다:\n  ' + '\n  '.join(bad))
    sys.exit(1)
print(f'경제 4판 쌍둥이 — 등급 {len(GRADES)} · 설정 {len(kt_values) + 3} · 강화 · 목표 {len(twins) + 2} · 능력치 규칙 {len(stat_twins) + 4} · 함께 달리기 · 하루 최대 {len(together_twins) + 1} · 앱 상수 · 러너 레벨 {len(const_twins)} 모두 같다')
print('  러너 레벨 하루 에너지(서버 값 — 앱은 runner_me · my_economy 를 읽는다): Lv1 ' + runner.get('runner_energy_base', '?')
      + ' · 레벨마다 ' + runner.get('runner_energy_per_level', '?') + ' · 상한 ' + runner.get('runner_energy_cap', '?'))
