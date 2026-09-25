-- 도전 보상 — 서버가 확인하고 서버가 지급한다.
--
-- 도전(주간 걸음·나이트 러너)의 "받기"는 폰이 목표를 채웠다고 판단하면 폰 안
-- 원장에만 SUP 를 적었다. 폰을 고치면 몇 번이든 받을 수 있었고, 서버 원장
-- (sup_ledger)에는 남지 않아 기기를 바꾸면 사라졌다.
--
-- 이제 받기는 event_claim() 하나로만 한다. 서버가 자기 기록으로 목표를 다시
-- 재고, 기간마다 한 번만, 원장에 EARN_EVENT 로 적는다. 앱은 서버가 준 금액을
-- 받은 뒤에야 "받음"으로 바꾼다.

-- ── 일별 걸음 올리기 ───────────────────────────────────────────────
--
-- 주간 걸음 도전은 러닝이 아닌 걸음까지 센다. 그 값은 폰에만 있었다. 표(0002
-- daily_steps)에 직접 쓸 수도 있지만, 여기로만 받으면 두 가지를 지킨다.
--   - 하루 걸음이 줄어들지 않는다(걸음은 하루 안에서 늘기만 한다).
--   - 하루 상한(economy.max_daily_steps)을 넘지 못한다.
create or replace function public.steps_sync(p_days json)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_today bigint := (now() at time zone 'UTC')::date - date '1970-01-01';
  v_day json;
  v_epoch bigint;
  v_steps int;
  v_goal int;
  v_count int := 0;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_days is null or json_typeof(p_days) <> 'array' or json_array_length(p_days) > 31 then
    raise exception '걸음 기록이 올바르지 않습니다' using errcode = '22023';
  end if;

  for v_day in select * from json_array_elements(p_days) loop
    v_epoch := (v_day->>'epoch_day')::bigint;
    v_steps := least(greatest(coalesce((v_day->>'steps')::int, 0), 0), economy.max_daily_steps());
    v_goal := least(greatest(coalesce((v_day->>'goal')::int, 8000), 1), 100000);
    -- 시간대 차이로 하루 앞선 날짜까지만 받는다. 먼 미래·먼 과거는 버린다.
    if v_epoch is null or v_epoch > v_today + 1 or v_epoch < v_today - 30 then
      continue;
    end if;
    insert into public.daily_steps (user_id, epoch_day, steps, goal, updated_at)
    values (v_user, v_epoch, v_steps, v_goal, now())
    on conflict (user_id, epoch_day) do update
      set steps = greatest(public.daily_steps.steps, excluded.steps),
          goal = excluded.goal,
          updated_at = now();
    v_count := v_count + 1;
  end loop;
  return v_count;
end;
$$;

-- ── 도전 ───────────────────────────────────────────────────────────

-- 도전 정의. 금액과 목표는 여기가 정본이다(앱의 Events 와 같은 값).
create or replace function economy.event_reward(p_event text) returns numeric
  language sql immutable as $$
    select case p_event when 'step_surge' then 250 when 'night_quest' then 300 end
  $$;

create or replace function economy.event_target(p_event text) returns double precision
  language sql immutable as $$
    select case p_event when 'step_surge' then 80000 when 'night_quest' then 20 end
  $$;

-- 나이트 러너가 세는 시작 시각(현지 시각)
create or replace function economy.night_from_hour() returns int
  language sql immutable as $$ select 20 $$;

create table if not exists public.event_claims (
  user_id uuid not null references auth.users on delete cascade,
  event_id text not null check (event_id in ('step_surge', 'night_quest')),
  -- 한 번씩 받는 단위. 주간 도전은 ISO 주('2026-W39'), 한정 도전은 'once'.
  period text not null,
  amount numeric(20, 4) not null check (amount > 0),
  progress double precision not null default 0,
  claimed_at timestamptz not null default now(),
  primary key (user_id, event_id, period)
);

comment on table public.event_claims is
  '도전 보상을 받은 기록. 기간마다 한 줄 — 같은 기간에 두 번 받을 수 없다.';

alter table public.event_claims enable row level security;
revoke insert, update, delete on public.event_claims from anon, authenticated;
drop policy if exists event_claims_select_own on public.event_claims;
create policy event_claims_select_own on public.event_claims
  for select using ((select auth.uid()) = user_id);
grant select on public.event_claims to authenticated;

-- 도전 기간. 주간 도전은 이번 ISO 주(한국 시각), 나머지는 한 번.
-- 사용자가 보낸 시간대는 쓰지 않는다 — 시간대를 바꿔 가며 같은 주를 두 번 받지 못하게.
create or replace function economy.event_period(p_event text, p_tz text) returns text
  language sql stable as $$
    select case p_event
      when 'step_surge' then to_char(now() at time zone 'Asia/Seoul', 'IYYY-"W"IW')
      else 'once'
    end
  $$;

-- 서버 기록으로 잰 진행값. 주간 걸음: 최근 7일(오늘 포함) 걸음 합.
-- 나이트 러너: 현지 저녁 8시 이후에 시작한 러닝 거리(km) 합 — 판정에서 걸린
-- 세션(FLAGGED · VOID)은 빼고 센다.
create or replace function public.event_progress(p_event text, p_tz text default 'Asia/Seoul')
returns double precision
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_tz text := coalesce(nullif(p_tz, ''), 'Asia/Seoul');
  v_today bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (select 1 from pg_timezone_names where name = v_tz) then
    v_tz := 'Asia/Seoul';
  end if;
  v_today := (now() at time zone v_tz)::date - date '1970-01-01';

  if p_event = 'step_surge' then
    return coalesce((
      select sum(d.steps) from public.daily_steps d
       where d.user_id = v_user and d.epoch_day between v_today - 6 and v_today
    ), 0);
  elsif p_event = 'night_quest' then
    return coalesce((
      select sum(s.distance_meters) / 1000.0 from public.walk_sessions s
       where s.user_id = v_user
         and s.verdict not in ('FLAGGED', 'VOID')
         and extract(hour from s.started_at at time zone v_tz) >= economy.night_from_hour()
    ), 0);
  end if;
  raise exception '없는 도전입니다' using errcode = '22023';
end;
$$;

-- 도전 보상 받기. 목표를 채웠고 이번 기간에 아직 안 받았으면 원장에 적고 금액을
-- 돌려준다. 이미 받았으면 23505, 목표 미달이면 23514 로 거절한다.
create or replace function public.event_claim(p_event text, p_tz text default 'Asia/Seoul')
returns numeric
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_tz text := coalesce(nullif(p_tz, ''), 'Asia/Seoul');
  v_reward numeric := economy.event_reward(p_event);
  v_progress double precision;
  v_period text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_reward is null then
    raise exception '없는 도전입니다' using errcode = '22023';
  end if;
  if not exists (select 1 from pg_timezone_names where name = v_tz) then
    v_tz := 'Asia/Seoul';
  end if;

  -- 같은 사람의 동시 요청이 둘 다 통과하지 않게 줄 세운다
  perform pg_advisory_xact_lock(hashtext('event_claim:' || v_user::text));

  v_period := economy.event_period(p_event, v_tz);
  if exists (select 1 from public.event_claims
              where user_id = v_user and event_id = p_event and period = v_period) then
    raise exception '이미 받은 보상입니다' using errcode = '23505';
  end if;

  v_progress := public.event_progress(p_event, v_tz);
  if v_progress < economy.event_target(p_event) then
    raise exception '아직 목표를 채우지 않았습니다 (%/%)', round(v_progress::numeric, 1),
      economy.event_target(p_event) using errcode = '23514';
  end if;

  insert into public.event_claims (user_id, event_id, period, amount, progress)
  values (v_user, p_event, v_period, v_reward, v_progress);
  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'EARN_EVENT', v_reward, '도전 보상: ' || p_event || ' ' || v_period);
  return v_reward;
end;
$$;

revoke execute on function public.event_progress(text, text) from public, anon;
revoke execute on function public.event_claim(text, text) from public, anon;
revoke execute on function public.steps_sync(json) from public, anon;
grant execute on function public.event_progress(text, text) to authenticated;
grant execute on function public.event_claim(text, text) to authenticated;
grant execute on function public.steps_sync(json) to authenticated;
