#!/usr/bin/env python3
"""AI 코칭의 규칙이 앱(Kotlin)과 서버(0059 SQL) 두 곳에 있다 — 둘이 같은지 맞대어 본다.

앱은 오프라인에서도 구간 · 템플릿 · 계획을 그려야 하고, 서버는 앱을 믿지 않으려고 같은 것을 스스로 만든다.
두 곳에 같은 값을 두면 한쪽만 자란다. 그래서 값이 하나라도 다르면 여기서 빨갛게 멈춘다:

  · 품질 규칙 값(구간 최소 · 최대 초, 부분 구간 최소 m, "비슷했어요" 폭)과 걸음 길이
  · 훈련 템플릿(id · 가족 · 단계별 분 · 반복)
  · 다음 주 계획의 요일(주 2 · 3 · 4회)
  · 근거 · 후보 id(서버가 만드는 id 를 앱이 모두 그릴 줄 아는가)
"""
import json
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent
KT = ROOT / "app/src/main/java/com/stepup/android/domain"
SQL = (ROOT / "supabase/migrations/0059_coaching.sql").read_text(encoding="utf-8")
splits = (KT / "coaching/CoachingSplits.kt").read_text(encoding="utf-8")
plans = (KT / "coaching/CoachingPlans.kt").read_text(encoding="utf-8")
ai = (KT / "coaching/CoachingAi.kt").read_text(encoding="utf-8")
economy = (KT / "RewardEconomy.kt").read_text(encoding="utf-8")

errors = []


def kt_const(text, name):
    m = re.search(rf"const val {name}\s*=\s*([0-9_.]+)L?\b", text)
    if not m:
        errors.append(f"Kotlin 상수 {name} 를 찾지 못했다")
        return None
    return float(m.group(1).replace("_", ""))


def sql_fn(name):
    m = re.search(rf"function coaching\.{name}\(\) returns \w+ language sql immutable as \$\$ select ([0-9.]+)", SQL)
    if not m:
        errors.append(f"SQL 함수 coaching.{name}() 를 찾지 못했다")
        return None
    return float(m.group(1))


for kt_name, sql_name in [("MIN_SPLIT_SEC", "min_split_sec"), ("MAX_SPLIT_SEC", "max_split_sec"),
                          ("MIN_PARTIAL_METERS", "min_partial_m"), ("EVEN_SPLIT_SEC", "even_split_sec")]:
    a, b = kt_const(splits, kt_name), sql_fn(sql_name)
    if a is not None and b is not None and a != b:
        errors.append(f"{kt_name}={a} 인데 coaching.{sql_name}()={b}")
stride_kt, stride_sql = kt_const(economy, "STRIDE_METERS"), sql_fn("stride_m")
if stride_kt is not None and stride_sql is not None and stride_kt != stride_sql:
    errors.append(f"STRIDE_METERS={stride_kt} 인데 coaching.stride_m()={stride_sql}")

# 템플릿 — Kotlin: easy(w, r, c) · walkRun(w, run, walk, rounds, c)
kt_templates = {}
block = re.search(r"val all: List<WorkoutTemplate> = listOf\((.*?)\n    \)", plans, re.S)
if not block:
    errors.append("CoachingTemplates.all 을 찾지 못했다")
else:
    for kind, args in re.findall(r"(easy|walkRun)\(([^)]*)\)", block.group(1)):
        n = [int(x) for x in args.split(",")]
        if kind == "easy":
            w, r, c = n
            steps = [{"role": "WARMUP", "minutes": w}, {"role": "EASY_RUN", "minutes": r}, {"role": "COOLDOWN", "minutes": c}]
            kt_templates[f"easy_{w + r + c}"] = ("easy", w + r + c, steps)
        else:
            w, run, walk, rounds, c = n
            mid = (run + walk) * rounds
            steps = [{"role": "WARMUP", "minutes": w},
                     {"role": "RUN_WALK", "minutes": mid, "run": run, "walk": walk, "rounds": rounds},
                     {"role": "COOLDOWN", "minutes": c}]
            kt_templates[f"walk_run_{w + mid + c}"] = ("walk_run", w + mid + c, steps)
sql_templates = {tid: (family, int(minutes), json.loads(steps))
                 for tid, family, minutes, steps in re.findall(r"\('(\w+)', '(\w+)', (\d+), '(\[.*?\])'::jsonb\)", SQL)}
if not kt_templates or kt_templates != sql_templates:
    errors.append(f"템플릿이 다르다\n  앱:   {kt_templates}\n  서버: {sql_templates}")

# 다음 주 계획의 요일
days = {"MONDAY": 0, "TUESDAY": 1, "WEDNESDAY": 2, "THURSDAY": 3, "FRIDAY": 4, "SATURDAY": 5, "SUNDAY": 6}
kt_plan = {}
for key, names in re.findall(r"(\d|else) -> listOf\(([^)]*)\)", plans):
    kt_plan[key] = [days[d.strip().removeprefix("DayOfWeek.")] for d in names.split(",")]
m = re.search(r"case p_runs when 2 then array\[([^\]]*)\] when 3 then array\[([^\]]*)\] else array\[([^\]]*)\] end", SQL)
sql_plan = {} if not m else {"2": [int(x) for x in m.group(1).split(",")], "3": [int(x) for x in m.group(2).split(",")],
                             "else": [int(x) for x in m.group(3).split(",")]}
if not kt_plan or kt_plan != sql_plan:
    errors.append(f"계획 요일이 다르다 — 앱 {kt_plan} · 서버 {sql_plan}")

# 근거 · 후보 id — 서버가 만드는 id 는 모두 앱이 안다
kt_ids = set(re.findall(r'const val (?:FACT|CAND)_\w+ = "([a-z_.]+)"', ai))
sql_fact_ids = set(re.findall(r"'id', '((?:run|week)\.[a-z_]+)'", SQL))
sql_cand_ids = set(re.findall(r"'id', '((?:pace|plan)\.[a-z_]+)'", SQL))
missing = (sql_fact_ids | sql_cand_ids) - kt_ids
if not sql_fact_ids or missing:
    errors.append(f"서버가 만드는 id 를 앱이 모른다: {sorted(missing)}")
if "'template.' || x.id" not in SQL or 'CAND_TEMPLATE_PREFIX = "template."' not in ai:
    errors.append("템플릿 후보 id 의 머리('template.')가 다르다")

if errors:
    print("\n".join(errors), file=sys.stderr)
    sys.exit(1)
print(f"AI 코칭 쌍둥이 — 규칙 값 5 · 템플릿 {len(kt_templates)} · 계획 요일 3 · id {len(sql_fact_ids | sql_cand_ids)}: 앱 = 서버")
