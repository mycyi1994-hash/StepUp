-- 서버 GPS 검증 — 경로로 거리를 재고, 걸음과 경로가 서로 맞는지 본다.
--
-- 지금까지 서버는 경로에서 최고 속도와 "말이 안 되는 구간"만 봤다. 거리는
-- 걸음 × 0.762 m 로 셌기 때문에, 폰을 흔들어 걸음만 늘리면 거리와 적립이 같이
-- 늘었다. 이제 서버가 경로로 거리를 직접 재고 다음을 판정한다.
--
--   - 경로 시각이 러닝 시간 밖이면 VOID      — 다른 날 경로를 붙여 올린 것
--   - 이미 올린 경로와 똑같으면 VOID         — 같은 경로를 다시 쓴 것
--   - 1km 넘게 움직였는데 걸음이 거의 없으면 VOID — 자전거·킥보드
--   - 걸음 거리가 GPS 거리의 2배를 넘으면 FLAGGED — 경로가 받쳐 주는 만큼만 적립
--
-- 실내(러닝머신)처럼 경로가 짧은 세션은 경로 검사를 하지 않는다. 그 경우는
-- 케이던스 검사(0003)만 받는다.

-- ── 경로 거리 ──────────────────────────────────────────────────────
drop function if exists economy.track_summary(text);

create or replace function economy.track_summary(p_track text)
returns table (
  gps_m double precision,
  points int,
  first_at bigint,
  last_at bigint
)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  v_plat double precision;
  v_plng double precision;
  v_pat bigint;
  v_have boolean := false;
  v_d double precision;
  v_dt double precision;
  v_total double precision := 0;
  v_n int := 0;
  v_first bigint;
  v_last bigint;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0, null::bigint, null::bigint;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;
    continue when v_lat not between -90 and 90 or v_lng not between -180 and 180;

    v_n := v_n + 1;
    v_first := least(coalesce(v_first, v_at), v_at);
    v_last := greatest(coalesce(v_last, v_at), v_at);

    if v_have then
      v_d := economy.haversine_m(v_plat, v_plng, v_lat, v_lng);
      v_dt := (v_at - v_pat) / 1000.0;
      -- 튄 구간(시속 60km 초과)은 거리에 넣지 않는다. 시각이 같거나 거꾸로 간
      -- 점도 넣지 않는다 — 한 점이 튀었다고 수백 m 가 거저 생기면 안 된다.
      if v_dt > 0 and v_d / v_dt * 3.6 <= economy.speed_glitch_kmh() then
        v_total := v_total + v_d;
      end if;
    end if;
    v_plat := v_lat; v_plng := v_lng; v_pat := v_at; v_have := true;
  end loop;

  return query select v_total, v_n, v_first, v_last;
end;
$$;

comment on function economy.track_summary is
  '경로의 실측 거리(튄 구간 제외)와 점 개수, 첫·마지막 시각(epoch ms).';

-- 경로 검사를 할 만큼 움직였는가. 이보다 짧으면 실내로 본다.
create or replace function economy.gps_check_min_m() returns double precision
  language sql immutable as $$ select 300::double precision $$;

-- 걸음 거리가 GPS 거리의 몇 배까지면 받아 주나. 보폭은 사람마다 다르고
-- GPS 는 모퉁이를 깎아 먹으므로 넉넉하게 2배.
create or replace function economy.step_gps_max_ratio() returns double precision
  language sql immutable as $$ select 2::double precision $$;

-- 경로 시각이 러닝 시간에서 이만큼(ms)까지 벗어나도 봐준다.
create or replace function economy.track_time_slack_ms() returns bigint
  language sql immutable as $$ select 120000::bigint $$;

alter table public.walk_sessions
  add column if not exists gps_distance_m double precision not null default 0;

comment on column public.walk_sessions.gps_distance_m is
  '서버가 경로에서 직접 잰 거리(m). 튄 구간은 빼고 잰다. 경로가 없으면 0.';

create index if not exists walk_sessions_user_track
  on public.walk_sessions (user_id, md5(track)) where track <> '';

-- ── 적립 (0003 을 대신한다) ────────────────────────────────────────
--
-- 인자는 그대로다. 앱은 바뀌지 않아도 된다.
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  p_faction text default ''
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day bigint;
  v_already_today int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_faction text;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
  v_gps_m double precision := 0;
  v_points_n int := 0;
  v_first_at bigint;
  v_last_at bigint;
  v_step_m double precision;
  v_distance_m double precision;
  v_track text := coalesce(p_track, '');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- ── 형식 검사 ──
  if p_steps is null or p_steps < 0 then
    raise exception '걸음 수가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_ended_at < p_started_at then
    raise exception '종료 시각이 시작보다 빠릅니다' using errcode = '22023';
  end if;
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;

  v_elapsed := greatest(coalesce(p_duration_sec, 0), 0);
  v_day := floor(extract(epoch from p_started_at) / 86400)::bigint;
  v_faction := case when coalesce(p_faction, '') in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')
                    then p_faction else '' end;
  v_step_m := p_steps * 0.762;

  select t.top_speed_kmh, t.glitch_ratio
    into v_top_speed, v_glitch
    from economy.track_speed_stats(v_track) t;

  select t.gps_m, t.points, t.first_at, t.last_at
    into v_gps_m, v_points_n, v_first_at, v_last_at
    from economy.track_summary(v_track) t;

  -- ── 판정 ──
  if v_elapsed >= 60 and p_steps::numeric * 60 / v_elapsed > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';

  -- 경로는 이 러닝 동안 찍힌 것이어야 한다. 어제 뛴 경로를 오늘 세션에 붙이면
  -- 한 번 뛰고 두 번 받는다.
  elsif v_points_n >= 2 and (
        v_first_at < (extract(epoch from p_started_at) * 1000)::bigint - economy.track_time_slack_ms()
     or v_last_at > (extract(epoch from p_ended_at) * 1000)::bigint + economy.track_time_slack_ms()) then
    v_verdict := 'VOID';
    v_reason := '경로 시각이 러닝 시간과 맞지 않습니다';

  -- 똑같은 경로를 다른 세션으로 다시 올렸다. 사람이 두 번 뛰어서 좌표와 시각이
  -- 한 글자도 안 틀리게 같을 수는 없다. (같은 세션의 재시도는 시작 시각이 같아
  -- 여기 걸리지 않고, 아래 on conflict 가 원래 결과를 돌려준다.)
  elsif v_track <> '' and exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user
           and s.track <> ''
           and md5(s.track) = md5(v_track)
           and s.started_at <> p_started_at) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로입니다';

  -- 1km 넘게 움직였는데 걸음이 그 20% 도 안 된다 — 발이 아니라 바퀴다.
  elsif v_gps_m >= 1000 and v_step_m < v_gps_m * 0.2 then
    v_verdict := 'VOID';
    v_reason := '걸음 없이 이동한 거리입니다';
  end if;

  -- ── 적립 대상 걸음 ──
  if v_verdict = 'VOID' then
    v_rewardable := 0;
  else
    v_rewardable := p_steps;

    -- 걸음이 경로보다 훨씬 많다 — 폰을 흔든 몫은 경로가 받쳐 주는 만큼만.
    if v_gps_m >= economy.gps_check_min_m()
       and v_step_m > v_gps_m * economy.step_gps_max_ratio() then
      v_rewardable := floor(v_gps_m * economy.step_gps_max_ratio() / 0.762)::int;
      v_verdict := 'FLAGGED';
      v_reason := '걸음 수가 GPS 거리보다 많습니다';
    end if;

    select coalesce(sum(s.rewarded_steps), 0) into v_already_today
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and floor(extract(epoch from s.started_at) / 86400)::bigint = v_day;

    if v_rewardable > economy.max_daily_steps() - v_already_today then
      v_rewardable := greatest(economy.max_daily_steps() - v_already_today, 0);
      v_verdict := 'FLAGGED';
      v_reason := case when v_reason = '' then '하루 적립 상한에 걸렸습니다'
                       else v_reason || ' · 하루 적립 상한에 걸렸습니다' end;
    end if;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * economy.party_multiplier(p_party_size)
      * economy.boost_multiplier(p_boost_bps),
    4
  );

  -- 거리 — 경로가 충분하면 경로로, 아니면(실내·GPS 없음) 걸음으로.
  -- 경로 검사에 걸려 걸음이 깎였으면 거리도 인정된 걸음만큼이다.
  v_distance_m := case
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_rewardable, 0) * 0.762 * 3 + 100)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, p_boost_bps, p_party_size,
    v_faction, case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable
  )
  on conflict (user_id, started_at) do nothing
  returning id into v_session_id;

  if v_session_id is null then
    select s.id, s.verdict, s.points_awarded
      into v_session_id, v_verdict, v_points
      from public.walk_sessions s
     where s.user_id = v_user and s.started_at = p_started_at;

    return query
      select v_session_id, v_verdict, v_points,
             coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
    return;
  end if;

  if v_points > 0 then
    v_kind := case when coalesce(p_party_size, 1) > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query
    select v_session_id, v_verdict, v_points,
           coalesce((select b.balance from public.sup_balances b where b.user_id = v_user), 0);
end;
$$;

comment on function public.record_session is
  '러닝 세션을 기록하고 적립액을 서버가 직접 계산한다. 경로로 거리를 재고 걸음과 맞는지 본다.';

grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text)
  to authenticated;
