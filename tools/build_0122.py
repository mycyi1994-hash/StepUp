#!/usr/bin/env python3
"""서버 0122(경제 점검 고침)를 만든다 — 고칠 함수는 마지막 정의를 그대로 복사해 고칠 곳만 바꾼다.

    python3 tools/build_0122.py   # supabase/migrations/0122_economy_audit_fixes.sql 을 다시 쓴다

복사 원본(마지막 정의): record_session · together_settle (0120) · course_run_submit (0024) · economy_bootstrap (0116) ·
ops_setting_rule (0121) · box_status (0118). 바꿀 곳은 한 번씩만 나와야 한다(아니면 멈춘다).
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIG = ROOT / "supabase" / "migrations"
OUT = MIG / "0122_economy_audit_fixes.sql"


def lines(name: str, start: int, end: int) -> str:
    text = (MIG / name).read_text(encoding="utf-8").split("\n")
    return "\n".join(text[start - 1:end]) + "\n"


def patch(src: str, old: str, new: str, count: int = 1) -> str:
    n = src.count(old)
    if n != count:
        raise SystemExit(f"바꿀 곳이 {count}번이 아니라 {n}번: {old[:90]!r}")
    return src.replace(old, new)


# ── 복사 · 고침 ─────────────────────────────────────────────────────────
record_session = lines("0120_together_bonus_fix.sql", 848, 1649)
record_session = patch(
    record_session,
    "where s.user_id = v_user and x.flash_post_id = v_flash and s.party_size > 1 and s.verdict <> 'VOID')) then",
    "where s.user_id = v_user and x.flash_post_id = v_flash\n"
    "              and (s.party_size > 1 or (s.together_base is not null and economy.together_qualifies(s)))\n"
    "              and s.verdict <> 'VOID')) then  -- 0122: 원격 번개는 인원이 나중에 정해져 처음엔 1명으로 적힌다 — 함께 달린 사람으로 올린 러닝이면 받은 것",
)
record_session = patch(
    record_session,
    "        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0), 0));",
    "        -- 0122: 10분 전에 끝낸 · 소식이 끊긴 사람이 맡긴 몫(묶어 둔 체력, 0120 R3)은 쓰지 않는다 — 내 몫은 쓴다\n"
    "        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0)\n"
    "                                         - economy.crew_pool_held_except(v_cr.crew_id, v_cr.day, now(), v_user), 0));",
)
record_session = patch(
    record_session,
    "      v_cmult := least(v_cmult, economy.crew_blend_mult(v_cr.party_id, v_user, v_cmult));",
    "      v_cmult := least(v_cmult, economy.crew_blend_mult(v_cr.party_id, v_user, v_cmult, v_unit));  -- 0122: 평균에 달린 만큼 무게",
)
record_session = patch(
    record_session,
    "    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit\n  )",
    "    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit, together_paid\n  )",
)
record_session = patch(
    record_session,
    "    v_crew, v_pool, v_reward_steps, v_tb, v_day_max, case when v_pool = 'CREW' then round(v_unit, 8) end\n  )",
    "    v_crew, v_pool, v_reward_steps, v_tb, v_day_max, case when v_pool = 'CREW' then round(v_unit, 8) end,\n"
    "    -- 0122: 예전 규칙(ALL)이면 인원 보너스를 적립에 이미 넣었다 — 규칙을 다시 켜도 정산이 한 번 더 주지 않게 준 것으로 적는다\n"
    "    case when v_pool = 'CREW' and not economy.together_on('CREW') then coalesce(v_head_sup, 0) else 0 end\n  )",
)
record_session = patch(
    record_session,
    "    perform economy.together_settle(v_party_id);\n"
    "    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;\n"
    "  end if;\n",
    "    perform economy.together_settle(v_party_id);\n"
    "    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;\n"
    "  end if;\n"
    "  -- 0122: 남이 올릴 때 내가 마침 다른 것을 기록하던 중이라 미뤄 둔 정산 — 내 원장 자물쇠를 잡은 지금 한다\n"
    "  if exists (select 1 from economy.together_pending q where q.user_id = v_user) then\n"
    "    perform economy.together_pending_run(v_user);\n"
    "    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;\n"
    "  end if;\n",
)
record_session = patch(
    record_session,
    "함께 달린 사람만 · 하루 최대 SUP (0120)';",
    "함께 달린 사람만 · 하루 최대 SUP (0120) · 묶인 체력 · 8 : 2 무게 · 번개 한 번 · 미룬 정산 (0122)';",
)

together_settle = lines("0120_together_bonus_fix.sql", 175, 292)
together_settle = patch(
    together_settle,
    "economy.crew_blend_mult(p_party, s.user_id, 1 + coalesce(v_c.shoe_eff_bps, 0) / 10000.0), 4);",
    "economy.crew_blend_mult(p_party, s.user_id, 1 + coalesce(v_c.shoe_eff_bps, 0) / 10000.0,\n"
    "                                                                   coalesce(s.together_unit, 0)), 4);",
)
together_settle = patch(
    together_settle,
    "    v_diff := v_target - s.together_paid;\n    if v_diff >= 0.0001 then\n",
    "    v_diff := v_target - s.together_paid;\n"
    "    -- 0122: 그 사람의 원장 자물쇠를 기다리지 않고 잡아 본다. 그 사람이 지금 다른 것을 기록하는 중이면(하루 최대를 둘이 함께 읽어 넘던 것)\n"
    "    -- 미뤄 두고, 그 사람이 다음에 러닝을 올릴 때 이 방을 다시 정산한다(together_pending_run). 내 자물쇠는 이미 잡고 있어 늘 잡힌다\n"
    "    if v_diff >= 0.0001 and not pg_try_advisory_xact_lock(hashtext('ledger:' || s.user_id::text)) then\n"
    "      insert into economy.together_pending (party_id, user_id) values (p_party, s.user_id) on conflict do nothing;\n"
    "      v_diff := 0;\n"
    "    end if;\n"
    "    if v_diff >= 0.0001 then\n",
)

course_run_submit = lines("0024_session_reward.sql", 635, 715)
course_run_submit = patch(
    course_run_submit,
    "    if v_reward >= 1 and (select c.owner_id from public.courses c where c.id = v_course) <> v_user then\n",
    "    -- 0122: 코스 보상도 하루 최대 SUP 에 든다(0120) — 남은 만큼만, 1 SUP 아래면 주지 않는다\n"
    "    if economy.daily_max_on() then\n"
    "      perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));\n"
    "      v_reward := least(v_reward, floor(greatest(economy.day_room(v_user, economy.game_day(v_session.started_at), null), 0)));\n"
    "    end if;\n"
    "    if v_reward >= 1 and (select c.owner_id from public.courses c where c.id = v_course) <> v_user then\n",
)

bootstrap = lines("0116_economy_v4.sql", 318, 351)
bootstrap = patch(
    bootstrap,
    "    insert into public.draw_grants (user_id, kind, granted)\n"
    "    values (v_user, 'FREE', 10)\n"
    "    on conflict (user_id, kind) do nothing;\n",
    "    -- 0122: 한 번은 상자 원장(SIGNUP)으로 센다 — 예전엔 무료 뽑기 줄이 없을 때만 넣어서, 첫 러닝 상자가 먼저 오면 10개가 사라졌다\n"
    "    insert into economy.box_awards (user_id, source, kind, day) values (v_user, 'SIGNUP', 'FREE', economy.game_day(now()))\n"
    "    on conflict (user_id, source) do nothing;\n"
    "    if found then\n"
    "      insert into public.draw_grants as d (user_id, kind, granted) values (v_user, 'FREE', 10)\n"
    "      on conflict (user_id, kind) do update set granted = d.granted + 10;\n"
    "    end if;\n",
)

ops_rule = lines("0121_runner_level.sql", 218, 267)
ops_rule = patch(
    ops_rule,
    "('runner_level_steps', 'num', 1000, 100000, '러너 레벨: 한 레벨에 필요한 러닝 걸음'),",
    "('runner_level_steps', 'num', 10000, 100000, '러너 레벨: 한 레벨에 필요한 러닝 걸음(낮추면 지난 걸음으로 상자가 나가 되돌릴 수 없어 10,000 아래로는 못 내린다)'),",
)
ops_rule = patch(
    ops_rule,
    "('runner_energy_cap', 'num', 1, 60, '러너 레벨: 하루 에너지 가장 많이(16.5 = 약 60분)')",
    "('runner_energy_cap', 'num', 1, 60, '러너 레벨: 하루 에너지 가장 많이(10 = 약 36분 · 0121 전과 같다 — 대격변과 함께 16.5 = 약 60분)')",
)

box_status = lines("0118_forge_xp_daily.sql", 716, 812)
box_status = patch(
    box_status,
    "a.source like 'RUN%'",
    "(a.source like 'RUN%' and a.source not like 'RUNNER%')",
    count=2,
)

# ── 새로 쓰는 것 ─────────────────────────────────────────────────────────
HEAD = r"""-- 0122 경제 점검 고침 (2026-11-22) — 사용자 "버그는 다 고치고 · a 는 너가 알아서해봐(인플레이션 막게) · b 는 괜찮음 · c 는 한시적"
-- 점검: docs/점검-2026-11-22-경제.md(네 갈래 — 들어오는 SUP · 여러 계정 · 쓰는 곳 · 튼튼함). tools/build_0122.py 가 만든다(고칠 함수는 마지막 정의를 복사).
--   1. 예전 앱 market_import 가 IMPORT 신발의 레벨을 올려 상급 상자 6개 — 다시 올려도 레벨을 바꾸지 않고, 레벨 상자는 IMPORT · MINT 를 뺀다
--   2. 크루 러닝: 소식 없이 달린 시간을 올릴 때 묶어 둔 체력까지 썼다 — 남의 묶인 몫은 빼고 쓴다. 묶는 사람도 넓힌다:
--      지금 박동을 보내며 달리는 중이 아니고 함께 달린 사람으로 올린 러닝도 없는 사람(10분 가짜 박동 뒤 빠진 부계정 · 박동 없는 사람)
--   3. 러너 걸음: 상한에 걸려 FLAGGED 가 된 러닝도 센다 — 무효 · 과속으로 멈춘 러닝만 뺀다
--   4. 하루 최대 SUP 에 닿아도 목표를 채운 날은 남긴다(연속 · 7일 상급 상자)
--   5. 크루 효율 8 : 2 — 평균에 각자 달린 만큼 무게(남은 내 몫까지), 보상 걸음 0(보류 계정)은 함께 달린 사람이 아니다
--   6. 강화 화면의 하루 에너지는 러너 레벨 에너지 — Lv10 이상이면 모든 재료가 '아까운 재료'로 찍혔다
--   7. 함께 달린 보너스 정산은 남의 원장 자물쇠를 기다리지 않고 잡아 본다 — 바쁘면 미뤄 두고 그 사람이 다음에 올릴 때(동시에 올려 하루 최대를 넘던 것)
--   8. 번개 보너스는 글 하나에 한 번 — 원격 번개도
--   9. 크루 보너스 규칙을 ALL 로 되돌렸다 다시 켜도 인원 보너스를 두 번 주지 않는다 · runner_level_steps 는 10,000 아래로 못 낮춘다
--  10. 코스 완주 · 도전 보상도 하루 최대 SUP 에 든다(코스는 남은 만큼만, 도전은 상금 그대로 — 그 뒤 러닝이 덜 받는다)
--  11. 가입 선물 무료 뽑기 10개 — 첫 러닝 상자가 먼저 와도 받는다
--  12. (0121) 러닝 뒤 "새로 받은 보상"에 러너 레벨 상자가 "러닝 상자"로 섞이던 것
--   A. 인플레이션: 러너 레벨의 하루 에너지 상한을 10(약 36분 — 0121 전 모두의 값)으로. 새 계정은 20분부터 그대로, 레벨 상자 · 배지도 그대로.
--      대격변(기본 적립 낮춤)을 켤 때 runner_energy_cap 을 16.5(약 60분)로 함께 올린다
-- 무료 수리(48시간에 한 켤레 가득)는 한시적(사용자) — 끝낼 때 free_repair_hours = 0.

-- ════════════════════════════════════════════════════════════════════
--  0. 설정 · 칸
-- ════════════════════════════════════════════════════════════════════
alter table economy.goal_days add column if not exists met_at timestamptz;
comment on column economy.goal_days.met_at is '그 날의 목표를 채운 때 — 하루 최대 SUP 에 막혀 목표 보너스를 못 받아도 연속 · 7일 상자는 이것으로 센다 (0122)';

create table if not exists economy.together_pending (
  party_id bigint not null,
  user_id uuid not null references auth.users (id) on delete cascade,
  at timestamptz not null default now(),
  primary key (party_id, user_id)
);
alter table economy.together_pending enable row level security;
comment on table economy.together_pending is '함께 달린 보너스 정산을 미뤄 둔 사람 — 정산할 때 그 사람이 다른 것을 기록하던 중이었다. 그 사람이 다음에 올릴 때 다시 정산한다 (0122)';

-- ════════════════════════════════════════════════════════════════════
--  1. 예전 앱 신발 등록 — 레벨을 따라가지 않는다 · 레벨 상자는 앱 안 신발만
-- ════════════════════════════════════════════════════════════════════
create or replace function public.market_import(
  p_local_id bigint,
  p_faction text,
  p_rarity text,
  p_variant int,
  p_level int,
  p_luck numeric,
  p_comfort numeric,
  p_durability int
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_id bigint;
  v_count int;
begin
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;
  if v_id is not null then
    -- 0122: 다시 올려도 레벨을 따라가지 않는다 — 폰이 적은 레벨로 서버 신발의 레벨이 오르면 레벨 상자(0116)가 나갔다(아무 계정이나 상급 6개)
    return v_id;
  end if;

  select count(*) into v_count from public.market_imports where user_id = v_user;
  if v_count >= economy.market_import_cap() then
    raise exception '올릴 수 있는 수를 넘었습니다 (최대 %)', economy.market_import_cap()
      using errcode = '22023';
  end if;

  insert into public.market_sneakers
    (owner_id, faction, rarity, variant, level, luck, comfort, durability)
  values
    (v_user, p_faction, p_rarity, p_variant, p_level, p_luck, p_comfort, p_durability)
  returning id into v_id;

  insert into public.market_imports (user_id, local_id, sneaker_id)
  values (v_user, p_local_id, v_id);

  return v_id;
end $$;
comment on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) is
  '폰의 신발을 거래소 장부에 올린다. 같은 신발은 한 번만, 계정당 상한이 있다. 다시 올려도 레벨은 바꾸지 않는다 (0122)';

-- 신발이 5 · 10 · 15 …레벨에 닿으면 상급 상자 — 계정마다 그 레벨 처음 한 번. 0122: 폰에서 올린 신발(IMPORT) · 체인 신발(MINT)은 뺀다
create or replace function economy.box_on_level() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare v_l int;
begin
  if new.owner_id is null or new.origin in ('IMPORT', 'MINT') or new.level / 5 <= old.level / 5 then
    return null;
  end if;
  for v_l in select generate_series((old.level / 5 + 1) * 5, (new.level / 5) * 5, 5) loop
    perform economy.box_grant(new.owner_id, 'LEVEL:' || v_l, 'GIFT', economy.game_day(now()));
  end loop;
  return null;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  2. 크루 러닝 — 묶인 체력(묶는 사람을 넓힌다 · 남의 묶인 몫은 쓰지 않는다)
-- ════════════════════════════════════════════════════════════════════
-- 묶어 둔 몫(R3) — 0122: 맡긴 것 가운데 안 쓴 몫을 묶는 사람을 넓힌다. 지금 박동을 보내며 달리는 중(30초 안)이 아니고,
-- 함께 달린 사람으로 올린 러닝도 없으면 묶는다. 0120 은 "서버가 센 시간 10분 미만"만 묶어, 부계정이 10분 넘게 가짜 박동을 보내고 빠지면
-- 남은 둘이 그 몫까지 썼다(점검 2차 E1 — 1,217 SUP). 함께 달린 사람으로 올리면 풀린다 · 끝까지 올리지 않으면 쓴 만큼만 남는다
--   · 올렸다 → 함께 달린 사람이 아니면 묶는다(0120 ②)
--   · 박동을 보내며 달리는 중(30초 안) → 묶지 않는다
--   · 끝냈다(ENDED)인데 함께 달린 사람으로 올린 러닝이 없다 → 묶는다(10분 가짜 박동 뒤 끝낸 부계정 — 올리면 풀린다)
--   · 5분 넘게 소식이 없다 → 묶는다(박동 없는 부계정 · 출발을 놓친 사람 — 올리면 풀린다, 돌려받기는 0120 그대로 언제든)
--   · 그 밖(일시정지 · 잠깐 끊김) → 0120 ① 그대로(서버가 센 시간 10분 미만만) — 정직한 러너의 짧은 끊김으로 남의 적립이 멈추지 않게
create or replace function economy.crew_pool_held_where(r economy.crew_runs, p_at timestamptz)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select case
    when r.session_id is not null then
      not exists (select 1 from public.walk_sessions x where x.id = r.session_id and economy.together_qualifies(x))
    when r.state = 'RUNNING' and r.last_beat_at >= p_at - interval '30 seconds' then false
    when r.state = 'ENDED' then true
    when r.last_beat_at < p_at - interval '5 minutes' and r.state <> 'PAUSED' then true
    else r.active_sec < economy.together_min_sec()
  end
$$;
create or replace function economy.crew_pool_held(p_crew uuid, p_day date, p_at timestamptz)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when not economy.together_on('CREW') then 0 else coalesce((
    select sum(greatest(r.contribution - r.consumed, 0)) from economy.crew_runs r
     where r.crew_id = p_crew and r.day = p_day and economy.crew_pool_held_where(r, p_at)), 0) end
$$;
-- 그 가운데 p_user 의 줄은 빼고 — 올리는 사람이 자기 맡긴 몫은 쓴다
create or replace function economy.crew_pool_held_except(p_crew uuid, p_day date, p_at timestamptz, p_user uuid)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when not economy.together_on('CREW') then 0 else coalesce((
    select sum(greatest(r.contribution - r.consumed, 0)) from economy.crew_runs r
     where r.crew_id = p_crew and r.day = p_day and r.user_id <> p_user and economy.crew_pool_held_where(r, p_at)), 0) end
$$;


-- ════════════════════════════════════════════════════════════════════
--  3. 함께 달린 사람 · 크루 효율 8 : 2 의 무게
-- ════════════════════════════════════════════════════════════════════
-- 0120 + 보상 걸음이 있어야(적립이 보류된 계정 · 경로가 받쳐 주지 않은 걸음은 함께 달린 사람이 아니다)
create or replace function economy.together_qualifies(s public.walk_sessions)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select s.verdict <> 'VOID' and s.gps_backed
     and coalesce(s.reward_steps, 1) > 0
     and coalesce(s.verified_steps, 0) >= economy.together_min_steps()
     and coalesce(s.duration_sec, 0) >= economy.together_min_sec()
     and coalesce(s.gps_credit_m, 0) >= coalesce(economy.together_goal_m(s.party_id, s.user_id), 0)
$$;

-- 크루 평균 효율 배수의 아래 끝 — 0122: 사람마다 무게(효율 뺀 바탕 together_unit = 보상 걸음 × 걸음당 SUP × 배수 × 운)를 두되
-- 남의 무게는 내 무게까지만(least). 10분 달린 센 신발이 60분 달린 약한 신발의 평균을 사람 수만큼 끌어올리던 것을 막는다(+58% → 약 +13%).
-- 아직 올리지 않은 출발한 사람은 무게를 몰라 내 무게로(가장 크게) 넣어 본다 — 그래서 실제 평균은 이 값 아래로 내려가지 않는다(오르기만 한다)
create or replace function economy.crew_blend_avg(p_party bigint, p_user uuid, p_self_m numeric, p_self_w numeric)
returns numeric language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_from timestamptz;
  v_until timestamptz;
  v_w numeric := greatest(coalesce(p_self_w, 0), 0);
  v_sum numeric;
  v_ws numeric;
  m numeric;
begin
  if v_w <= 0 then
    return p_self_m;
  end if;
  v_sum := p_self_m * v_w;
  v_ws := v_w;
  select min(c.started_at) into v_from from economy.crew_runs c where c.party_id = p_party;
  if v_from is null then
    return p_self_m;
  end if;
  v_until := v_from + make_interval(hours => greatest(economy.fx('together_topup_hours', 24), 0)::int);
  select v_sum + coalesce(sum((1 + c.shoe_eff_bps / 10000.0) * least(greatest(coalesce(x.together_unit, 0), 0), v_w)), 0),
         v_ws + coalesce(sum(least(greatest(coalesce(x.together_unit, 0), 0), v_w)), 0)
    into v_sum, v_ws
    from public.walk_sessions x join economy.crew_runs c on c.session_id = x.id and c.user_id = x.user_id
   where x.party_id = p_party and x.user_id <> p_user and x.together_base is not null and x.created_at <= v_until
     and economy.together_qualifies(x);
  if now() <= v_until then
    for m in select 1 + c.shoe_eff_bps / 10000.0 as m from economy.crew_runs c
              where c.party_id = p_party and c.user_id <> p_user and c.session_id is null
              order by 1 loop
      exit when m >= v_sum / v_ws;
      v_sum := v_sum + m * v_w;
      v_ws := v_ws + v_w;
    end loop;
  end if;
  return round(v_sum / v_ws, 6);
end $$;
create or replace function economy.crew_blend_mult(p_party bigint, p_user uuid, p_self_m numeric, p_self_w numeric)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select case when economy.crew_blend_share() <= 0 then p_self_m
              else round((1 - economy.crew_blend_share()) * p_self_m
                         + economy.crew_blend_share() * economy.crew_blend_avg(p_party, p_user, p_self_m, p_self_w), 6) end
$$;
-- 사람 수로 세던 예전 것(0120)은 지운다 — 0120 을 다시 올려도 이 줄이 다시 지운다
drop function if exists economy.crew_blend_mult(bigint, uuid, numeric);
drop function if exists economy.crew_blend_avg(bigint, uuid, numeric);

-- 그날(한국 날짜) 받은 SUP — 0120 + 코스 완주 · 도전 보상(0122, 하루 최대 SUP 밖이던 것)
create or replace function economy.day_earned(p_user uuid, p_day date)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select coalesce((select sum(s.points_awarded) from public.walk_sessions s
                    where s.user_id = p_user and economy.game_day(s.started_at) = p_day), 0)
       + coalesce((select sum(l.amount) from public.sup_ledger l
                    where l.user_id = p_user and l.ref in ('goal:' || p_day, 'course:' || p_day)), 0)
       + coalesce((select sum(l.amount) from public.sup_ledger l
                    where l.user_id = p_user and l.kind = 'EARN_EVENT' and l.amount > 0
                      and l.occurred_at >= economy.game_day_start(p_day) and l.occurred_at < economy.game_day_start(p_day + 1)), 0)
$$;

"""

SETTLE_TAIL = r"""
-- 미뤄 둔 정산을 한다 — record_session 이 끝에서(그 사람의 원장 자물쇠를 잡은 채) 부른다
create or replace function economy.together_pending_run(p_user uuid)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_party bigint; v_n int := 0;
begin
  for v_party in select q.party_id from economy.together_pending q where q.user_id = p_user order by q.party_id loop
    -- 방 자물쇠는 잡아 보기만 — 바쁘면 다음에(두 사람이 서로 다른 차례로 두 방을 기다려 멈추지 않게)
    continue when not pg_try_advisory_xact_lock(hashtext('together:' || v_party));
    delete from economy.together_pending q where q.user_id = p_user and q.party_id = v_party;
    v_n := v_n + coalesce(economy.together_settle(v_party), 0);
  end loop;
  return v_n;
end $$;

"""

GOAL = r"""
-- ════════════════════════════════════════════════════════════════════
--  4. 목표 — 하루 최대에 막혀도 채운 날은 남긴다
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.goal_streak_before(p_user uuid, p_day date, p_max int default 60)
returns int language plpgsql stable security definer set search_path = public, economy as $$
declare v int := 0; d date := p_day - 1;
begin
  while v < p_max loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || d)
      or exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = d and g.met_at is not null)
      or exists (select 1 from public.boosts b
                  where b.user_id = p_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(d + 1) and b.ends_at > economy.game_day_start(d)));
    v := v + 1; d := d - 1;
  end loop;
  return v;
end $$;

-- 0120 본문 + 채운 날을 먼저 남긴다(goal_days.met_at). 하루 최대 SUP 에 막혀 보너스가 0 이어도 연속 · 7일 상급 상자는 그대로 —
-- 예전엔 0 이면 아무것도 남기지 않아 다음 날 연속이 1 로 돌아갔고, 크루에서는 올린 차례로 갈렸다
create or replace function economy.goal_settle(p_user uuid, p_day date)
returns numeric language plpgsql security definer set search_path = public, economy as $$
declare
  v_goal int;
  v_streak int;
  v_bonus numeric;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
  v_room numeric;
begin
  if p_user is null or p_day is null or p_day > economy.game_day(now()) or p_day < economy.game_day(now()) - 7 then
    return null;
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));
  -- 받은 날이면 끝. 채운 날로만 남아 있으면(하루 최대에 막혔다) 남은 몫이 생겼을 때 다시 준다(같은 날 이벤트로 하루 최대가 커질 때)
  if exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || p_day) then
    return null;
  end if;
  v_goal := economy.goal_of(p_user, p_day);
  if economy.verified_steps_on(p_user, p_day) < v_goal then
    return null;
  end if;
  v_streak := 1 + economy.goal_streak_before(p_user, p_day, greatest(8, v_need));
  if not exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = p_day and g.met_at is not null) then
    insert into economy.goal_days (user_id, day, goal, met_at) values (p_user, p_day, v_goal, now())
    on conflict (user_id, day) do update set met_at = coalesce(economy.goal_days.met_at, now());
  end if;
  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  -- 하루 최대 SUP 에 목표 보너스도 든다(0120) — 남은 만큼만
  if economy.daily_max_on() then
    v_room := economy.day_room(p_user, p_day, null);
    v_bonus := round(least(v_bonus, greatest(v_room, 0)), 2);
  end if;
  if v_bonus > 0 then
    perform economy.ledger_apply(p_user, 'BONUS_GOAL', v_bonus,
                                 format('목표 달성 보너스 (%s일 연속)', least(v_streak, 8)), 'goal:' || p_day);
    if p_day < economy.game_day(now()) then
      update public.sup_ledger l set occurred_at = economy.game_day_start(p_day + 1) - interval '1 second'
       where l.user_id = p_user and l.ref = 'goal:' || p_day;
    end if;
  end if;
  if v_need > 0 and v_streak >= v_need and not exists (
       select 1 from economy.box_awards a
        where a.user_id = p_user and a.source like 'STREAK:%' and a.day > p_day - v_need and a.day <= p_day) then
    perform economy.box_grant(p_user, 'STREAK:' || p_day, 'GIFT', p_day);
  end if;
  return greatest(v_bonus, 0);
end $$;

-- ════════════════════════════════════════════════════════════════════
--  5. 러너 걸음 — 상한에 걸린 러닝도 센다
-- ════════════════════════════════════════════════════════════════════
-- 0121 은 CLEAN 만 셌다. FLAGGED 는 과속 말고도 하루 걸음 상한 · 하루 최대 SUP · 걸음이 GPS 보다 많아 자름에도 붙어
-- 그 러닝의 걸음이 통째로 빠졌다(오늘의 보상은 같은 러닝을 다 센다). 보상 걸음은 무효 · 보류 · 경로 없음이면 이미 0 이고,
-- 과속이 이어져 멈춘 러닝(run_penalties)만 따로 뺀다
create or replace function economy.runner_steps_of(p_user uuid)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(d.s), 0)::bigint from (
    select least(sum(greatest(coalesce(s.reward_steps, s.backed_steps, 0), 0)), economy.max_daily_steps()) as s
      from public.walk_sessions s
     where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED')
       and not exists (select 1 from public.run_penalties x where x.user_id = s.user_id and x.run_started_at = s.started_at)
     group by economy.game_day(s.started_at)) d
$$;

-- ════════════════════════════════════════════════════════════════════
--  6. 강화 화면의 하루 에너지 — 러너 레벨이 켜져 있으면 신발 레벨과 상관없다
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.forge_account_after(p_user uuid, p_target bigint, p_level int, p_burn bigint[],
    out energy numeric, out cap numeric)
language sql stable security definer set search_path = public, economy as $$
  select case when economy.runner_on() then economy.energy_max(p_user)
              else economy.energy_max_for_level(coalesce(max(case when s.id = p_target then p_level
                                                                  when s.origin in ('IMPORT', 'MINT') then 1 else s.level end), 1)) end,
         case when not coalesce((economy.setting('earn_cap_by_shoe') #>> '{}')::boolean, true) then 1
              else greatest(1, coalesce(max(10000 + e.efficiency_bps) / 14000.0, 1)) end
    from public.market_sneakers s
    left join lateral economy.shoe_effective(s) e on true
   where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'
     and not (s.id = any(coalesce(p_burn, '{}'::bigint[])))
$$;

"""

TAIL = r"""
-- ════════════════════════════════════════════════════════════════════
--  12. 권한
-- ════════════════════════════════════════════════════════════════════
revoke all on function economy.crew_pool_held_except(uuid, date, timestamptz, uuid) from public, anon, authenticated;
revoke all on function economy.crew_pool_held(uuid, date, timestamptz) from public, anon, authenticated;
revoke all on function economy.crew_pool_held_where(economy.crew_runs, timestamptz) from public, anon, authenticated;
revoke all on function economy.crew_blend_avg(bigint, uuid, numeric, numeric) from public, anon, authenticated;
revoke all on function economy.crew_blend_mult(bigint, uuid, numeric, numeric) from public, anon, authenticated;
revoke all on function economy.together_pending_run(uuid) from public, anon, authenticated;
revoke all on function economy.together_qualifies(public.walk_sessions) from public, anon, authenticated;
revoke all on function economy.day_earned(uuid, date) from public, anon, authenticated;
revoke all on function economy.together_settle(bigint) from public, anon, authenticated;
revoke all on function economy.goal_settle(uuid, date) from public, anon, authenticated;
revoke all on function economy.goal_streak_before(uuid, date, int) from public, anon, authenticated;
revoke all on function economy.runner_steps_of(uuid) from public, anon, authenticated;
revoke all on function economy.forge_account_after(uuid, bigint, int, bigint[]) from public, anon, authenticated;
revoke all on function economy.box_on_level() from public;
revoke all on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) from public, anon;
grant execute on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  13. 한 번만: 에너지 상한 · 가입 선물 표시 · 러너 걸음 다시 세기 · 하루 최대에 막혔던 목표 날
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'audit_fix_20261122') then
    -- A. 인플레이션 — 0121 이 넣은 기본값(16.5)일 때만 10 으로. 운영자가 바꿔 둔 값은 건드리지 않는다
    update public.economy_settings set value = '10'::jsonb, updated_at = now()
     where key = 'runner_energy_cap' and value = '16.5'::jsonb;
    -- 11. 이미 무료 뽑기 줄이 있는 계정은 가입 선물을 받은 것으로 센다(새 길이 한 번 더 주지 않게)
    insert into economy.box_awards (user_id, source, kind, day)
    select d.user_id, 'SIGNUP', 'FREE', economy.game_day(d.created_at)
      from public.draw_grants d where d.kind = 'FREE'
    on conflict (user_id, source) do nothing;
    -- 4. 지난 7일 가운데 목표를 채웠는데 하루 최대 SUP 에 막혀 아무것도 남지 않은 날
    insert into economy.goal_days (user_id, day, goal, met_at)
    select u.id, d.day::date, economy.goal_peek(u.id, d.day::date), now()
      from auth.users u
     cross join generate_series(economy.game_day(now()) - 7, economy.game_day(now()) - 1, interval '1 day') d(day)
     where economy.daily_max_on()
       and not exists (select 1 from public.sup_ledger l where l.user_id = u.id and l.ref = 'goal:' || d.day::date)
       and exists (select 1 from public.walk_sessions s where s.user_id = u.id and economy.game_day(s.started_at) = d.day::date)
       and economy.verified_steps_on(u.id, d.day::date) >= economy.goal_peek(u.id, d.day::date)
       and economy.day_room(u.id, d.day::date, null) <= 0
    on conflict (user_id, day) do update set met_at = coalesce(economy.goal_days.met_at, excluded.met_at);
    -- 3. 상한에 걸린 러닝까지 센 걸음으로 레벨을 다시 — 새로 닿은 레벨의 상자를 준다(러너 규칙이 켜져 있을 때만)
    perform economy.runner_refresh(u.id) from auth.users u;
    insert into public.economy_settings (key, value) values ('audit_fix_20261122', to_jsonb(now()));
  end if;
end $$;
"""


def main() -> None:
    parts = [
        HEAD,
        "-- 함께 달린 보너스 정산 — 0120 본문 + 남의 원장 자물쇠는 잡아 보기만(바쁘면 미룸) · 8 : 2 무게(7)\n",
        together_settle,
        SETTLE_TAIL,
        GOAL,
        "-- ════════════════════════════════════════════════════════════════════\n"
        "--  7. 코스 완주 보상 — 0024 본문 + 하루 최대 SUP 안에서(10)\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        course_run_submit,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  8. 가입 선물 — 0116 본문 + 상자 원장으로 한 번(11)\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        bootstrap,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  9. 운영자 손잡이 — 0121 목록 + runner_level_steps 는 10,000 부터(9) · 에너지 상한 글\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        ops_rule,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  10. 상자 현황 — 0118 본문 + 러너 레벨 상자는 '러닝 상자'가 아니다(12)\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        box_status,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  11. record_session — 0120 본문 + 묶인 체력(2) · 8 : 2 무게(5) · 번개 한 번(8) · ALL 의 인원 보너스(9) · 미룬 정산(7)\n"
        "--      다시 고칠 때는 이것(0122)을 복사한다\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        record_session,
        TAIL,
    ]
    OUT.write_text("".join(parts), encoding="utf-8")
    print(f"썼다: {OUT.relative_to(ROOT)} ({sum(p.count(chr(10)) for p in parts)}줄)")


if __name__ == "__main__":
    main()
