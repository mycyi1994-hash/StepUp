-- ════════════════════════════════════════════════════════════════════
--  0069 — 크루 체력: 크루 러닝은 개인 에너지 대신 크루가 함께 쓰는 체력으로 · 크루 레벨은 크루 러닝 누적 거리로
--         (2026-10-21 사용자 결정)
--
--  사용자 결정: "크루달리기할때 크루 체력 자체를 아예 따로" · "크루원 모든 능력치 합산 + 크루 자체 레벨 이렇게 해서 공유되는
--  크루 체력" · 다 쓰면 "그 뒤는 적립 없음" · 최대치는 "신발 레벨을 따라감" · 크루 러닝은 "함께 달린 2명 이상" ·
--  기여는 "신발 에너지 최대치" · 한 사람 몫은 "크루 체력 ÷ 기여 인원" · 크루 레벨은 "크루 러닝 누적 km" ·
--  기여로 세는 크루원은 "어제까지 가입 + 7일 안 러닝".
--
--  1. 크루 체력(그 크루의 하루, 한국 시각) = 기여 크루원의 신발 에너지 최대치(10 + 2 × (신은 신발 레벨 − 1)) 합
--     + 크루 레벨 보너스(레벨 하나마다 10, 레벨 1 은 0).
--     - 기여 크루원 = 그날이 시작되기 전에 가입했고, 그 전 7일 안에 무효가 아닌 러닝이 있는 크루원.
--     - 그날 처음 크루 체력을 읽거나 쓸 때 한 번 정해 둔다(economy.crew_stamina_days · crew_stamina_members). 그날 안에
--       신발을 바꾸거나 레벨이 올라도 다음 날부터 바뀐다 — 하루 안에서 몫이 흔들리지 않게.
--  2. 한 사람 몫 = 크루 체력 ÷ 기여 크루원 수. 몫끼리 겹치지 않아 먼저 올린 사람이 남의 몫을 쓰지 못한다.
--  3. 크루 러닝 = 그 크루의 함께 달리기 방에서 출발해 출발 2분 뒤에도 200m 안에 2명 이상(나 포함)이 함께였고(보너스 인원과
--     같은 무리 — party_for, 0056) 내가 그 크루원. 크루 러닝을 한 기여 크루원은 개인 에너지 대신 자기 몫의 크루 체력을 쓴다
--     (쓰는 양은 개인 에너지와 같다 — 적립 걸음 600 에 1, 착화감만큼 덜). 몫을 다 쓰면 그 뒤 걸음은 적립하지 않는다 —
--     개인 에너지로 넘어가지 않는다. 기여 크루원이 아닌 사람(갓 가입 · 오래 쉼)의 크루 러닝은 예전처럼 개인 에너지.
--     혼자 뛴 러닝 · 번개 러닝은 그대로 개인 에너지. 인원 보너스(×1.1 ~ ×1.5) · 하루 적립 상한(600 SUP, 개인 · 크루 합쳐) ·
--     상한에 걸리면 받은 몫만큼만 쓰기(0068)는 그대로다.
--  4. 크루 레벨 = 크루 러닝의 인정 거리(함께 달린 사람마다 자기 거리) 누적 — 30 · 100 · 250 · 500 · 900 · 1,500 · 2,300 ·
--     3,300 · 4,500 km 에서 레벨 2 ~ 10. 비어 있던 crews.level(0047 — "새 크루")을 크루 러닝 거리가 생기면 채운다(첫 크루 러닝 뒤
--     레벨 1 부터). 아직 크루 러닝이 없으면 그대로 비워 둔다 — 앱은 "새 크루"로 보인다. 레벨은 내려가지 않는다.
--     지금까지의 크루 러닝(크루에 묶인 2명 이상 러닝)으로 한 번 채운다.
--  5. 러닝 기록에 받쳐 준 체력(energy_pool)과 크루 러닝의 크루(crew_id)를 남긴다 — session_tag_crew(0056)를 기다리지 않는다.
--
--  그대로인 것: 판정 · 적립 금액 · 상한 · 인원 보너스 · 신발 내구도(1분마다 5) · 개인 에너지(혼자 · 번개 러닝) · 부스터.
-- ════════════════════════════════════════════════════════════════════

-- ── 크루 레벨 기준 ────────────────────────────────────────────────────

-- 그 레벨이 되는 크루 러닝 누적 거리(km). 레벨 1 이하 · 10 넘으면 null
create or replace function economy.crew_level_km(p_level int) returns numeric
language sql immutable set search_path = '' as $$
  select case p_level
    when 2 then 30 when 3 then 100 when 4 then 250 when 5 then 500 when 6 then 900
    when 7 then 1500 when 8 then 2300 when 9 then 3300 when 10 then 4500
  end::numeric
$$;

create or replace function economy.crew_level_for_km(p_km numeric) returns int
language sql immutable set search_path = '' as $$
  select coalesce(max(l), 1)::int from generate_series(2, 10) l where coalesce(p_km, 0) >= economy.crew_level_km(l)
$$;

-- 크루 레벨 하나마다 더하는 크루 체력(레벨 1 은 0)
create or replace function economy.crew_level_bonus() returns numeric
language sql immutable set search_path = '' as $$ select 10::numeric $$;

revoke all on function economy.crew_level_km(int) from public;
revoke all on function economy.crew_level_for_km(numeric) from public;
revoke all on function economy.crew_level_bonus() from public;

-- ── 크루 러닝 누적 거리 · 러닝이 쓴 체력 ─────────────────────────────

alter table public.crews add column if not exists run_km numeric(12, 3) not null default 0;
comment on column public.crews.run_km is '크루 러닝(그 크루의 방 · 함께 달린 2명 이상)의 인정 거리 누적(km, 사람마다 자기 거리) — 크루 레벨 기준 (0069)';
comment on column public.crews.level is '크루 레벨 1 ~ 10 — 크루 러닝 누적 거리(run_km)로 record_session 이 올린다. 크루 러닝이 아직 없으면 비어 있다 — "새 크루" (0069)';

alter table public.walk_sessions add column if not exists energy_pool text not null default 'PERSONAL';
do $$ begin
  if not exists (select 1 from pg_constraint where conname = 'walk_sessions_energy_pool_check') then
    alter table public.walk_sessions add constraint walk_sessions_energy_pool_check check (energy_pool in ('PERSONAL', 'CREW'));
  end if;
end $$;
comment on column public.walk_sessions.energy_pool is '이 러닝의 적립을 받쳐 준 체력 — PERSONAL 개인 에너지 · CREW 크루 체력 (0069)';

-- 지금까지의 크루 러닝(크루에 묶인 2명 이상 러닝, 무효 · 보류 제외)으로 한 번 채운다 — 다시 붙여넣어도 이미 센 크루는 건너뛴다
update public.crews c
   set run_km = x.km
  from (select s.crew_id, round((sum(s.distance_meters) / 1000)::numeric, 3) as km
          from public.walk_sessions s
         where s.crew_id is not null and s.party_size >= 2 and s.verdict <> 'VOID'
           and s.verdict_reason not like '%최근 무효 러닝이 많아%'
         group by s.crew_id) x
 where x.crew_id = c.id and c.run_km = 0;
update public.crews c set level = greatest(coalesce(c.level, 1), economy.crew_level_for_km(c.run_km))
 where c.run_km > 0 and c.level is distinct from greatest(coalesce(c.level, 1), economy.crew_level_for_km(c.run_km));

-- ── 그날의 크루 체력 ──────────────────────────────────────────────────

create table if not exists economy.crew_stamina_days (
  crew_id uuid not null references public.crews(id) on delete cascade,
  day date not null,
  -- 그날 크루 체력 = 기여 합 + 레벨 보너스 (기여 크루원이 없으면 0)
  max numeric(10, 4) not null,
  -- 정한 때의 크루 레벨
  level int not null,
  contributors int not null,
  -- 한 사람 몫 = max ÷ contributors (4자리에서 버림)
  share numeric(10, 4) not null,
  created_at timestamptz not null default now(),
  primary key (crew_id, day)
);
comment on table economy.crew_stamina_days is '크루 체력 — 그 크루의 하루(한국 시각)에 처음 읽거나 쓸 때 정해 둔다 (0069)';

create table if not exists economy.crew_stamina_members (
  crew_id uuid not null,
  day date not null,
  user_id uuid not null references auth.users(id) on delete cascade,
  -- 이 사람이 보탠 체력 = 정한 때 신은 신발의 에너지 최대치
  contribution numeric(10, 4) not null,
  -- 크루 러닝에 쓴 크루 체력 (몫을 넘지 않는다)
  used numeric(10, 4) not null default 0,
  primary key (crew_id, day, user_id),
  foreign key (crew_id, day) references economy.crew_stamina_days (crew_id, day) on delete cascade
);
comment on table economy.crew_stamina_members is '그날 크루 체력을 나눠 받는 크루원 — 보탠 양과 쓴 양 (0069)';

alter table economy.crew_stamina_days enable row level security;
alter table economy.crew_stamina_members enable row level security;
revoke all on economy.crew_stamina_days, economy.crew_stamina_members from public, anon, authenticated;

-- 그날의 크루 체력 — 없으면 지금 정해 둔다. 같은 크루 · 날을 여럿이 동시에 처음 열어도 한 번만 만든다
create or replace function economy.crew_stamina_day(p_crew uuid, p_day date)
returns economy.crew_stamina_days
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v economy.crew_stamina_days;
  v_level int;
  v_sum numeric;
  v_n int;
  v_max numeric;
begin
  select * into v from economy.crew_stamina_days d where d.crew_id = p_crew and d.day = p_day;
  if found then
    return v;
  end if;
  perform pg_advisory_xact_lock(hashtext('crew_stamina:' || p_crew::text || ':' || p_day::text));
  select * into v from economy.crew_stamina_days d where d.crew_id = p_crew and d.day = p_day;
  if found then
    return v;
  end if;

  select greatest(coalesce(c.level, 1), 1) into v_level from public.crews c where c.id = p_crew;
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  insert into economy.crew_stamina_days (crew_id, day, max, level, contributors, share)
  values (p_crew, p_day, 0, v_level, 0, 0);
  -- 기여 크루원 — 그날이 시작되기 전에 가입했고 그 전 7일 안에 무효가 아닌 러닝이 있다(안 뛰는 회원 · 부계정으로 불리지 않게).
  -- 보탠 양은 지금 신은 신발의 에너지 최대치(10 + 2 × (레벨 − 1), 신발 없으면 10)
  insert into economy.crew_stamina_members (crew_id, day, user_id, contribution)
  select p_crew, p_day, m.user_id, economy.energy_max(m.user_id)
    from public.crew_members m
   where m.crew_id = p_crew
     and m.joined_at < economy.game_day_start(p_day)
     and exists (select 1 from public.walk_sessions s
                  where s.user_id = m.user_id and s.verdict <> 'VOID'
                    and s.started_at >= economy.game_day_start(p_day - 7)
                    and s.started_at < economy.game_day_start(p_day));
  select coalesce(sum(x.contribution), 0), count(*)::int into v_sum, v_n
    from economy.crew_stamina_members x where x.crew_id = p_crew and x.day = p_day;
  v_max := case when v_n > 0 then v_sum + economy.crew_level_bonus() * (v_level - 1) else 0 end;
  update economy.crew_stamina_days d
     set max = v_max,
         contributors = v_n,
         share = case when v_n > 0 then trunc(v_max / v_n, 4) else 0 end
   where d.crew_id = p_crew and d.day = p_day
  returning * into v;
  return v;
end;
$$;
revoke all on function economy.crew_stamina_day(uuid, date) from public;

-- ── 러닝 기록 ────────────────────────────────────────────────────────

-- 0068 그대로 + 크루 러닝은 크루 체력(나눠 받는 크루원) · 크루 레벨 거리 · 크루 러닝의 크루와 받쳐 준 체력을 남긴다
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  p_faction text default '',
  -- 안드로이드가 "모의 위치"로 표시한 좌표가 하나라도 있었나
  p_mock_location boolean default false
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
  v_gday date;
  v_already_steps int;
  v_already_points numeric;
  v_verified int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_cap numeric;
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
  v_gps_m double precision := 0;
  v_points_n int := 0;
  v_first_at bigint;
  v_last_at bigint;
  v_step_m double precision;
  v_distance_m double precision;
  v_track text := coalesce(p_track, '');
  v_party int := 1;
  v_party_id bigint;
  v_gps_backed boolean;
  v_credit_m double precision := 0;
  v_cap_left numeric;
  v_shoe public.market_sneakers;
  v_eff int := 0;
  v_comfort int := 0;
  v_dur numeric := 100;
  v_energy_left numeric;
  v_energy_per_step numeric;
  v_energy_used numeric := 0;
  v_xp numeric := 1;
  v_recent_void int;
  -- 무효가 잦아 보류 중 — 목표 보너스 · 주간 도전 · 코스 보상에도 이 러닝을 세지 않는다
  v_held boolean := false;
  v_new_account boolean;
  -- 상한(하루 금액 · 경로 없는 러닝 · 올린 날)을 걸기 전 금액 — 상한에 걸려 덜 받은 만큼 에너지도 덜 쓴다 (0068)
  v_points_full numeric := 0;
  -- 크루 러닝(그 크루의 방 · 함께 달린 2명 이상 · 크루원)이면 그 크루 — 크루 체력 · 크루 레벨 거리 (0069)
  v_crew uuid;
  -- 이 러닝의 적립을 받쳐 준 체력 — PERSONAL 개인 에너지 · CREW 크루 체력(그날 나눠 받는 크루원만) (0069)
  v_pool text := 'PERSONAL';
  v_stamina economy.crew_stamina_days;
  v_my_used numeric := 0;
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
  if p_started_at < now() - interval '7 days' and not exists (
       select 1 from public.walk_sessions s
        where s.user_id = v_user and s.started_at = p_started_at) then
    raise exception '너무 오래된 러닝입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  -- 이미 올린 러닝의 재시도면 원래 결과를 그대로 돌려준다. 아래 계산을 다시 하면
  -- 에너지 · 내구도를 두 번 깎는다.
  select s.id, s.verdict, s.points_awarded into v_session_id, v_verdict, v_points
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
    return;
  end if;
  v_verdict := 'CLEAN';

  v_elapsed := least(
    greatest(coalesce(p_duration_sec, 0), 0),
    floor(extract(epoch from (p_ended_at - p_started_at)))::int
  );
  v_gday := economy.game_day(p_started_at);
  v_step_m := p_steps * 0.762;

  select t.top_speed_kmh, t.glitch_ratio into v_top_speed, v_glitch
    from economy.track_speed_stats(v_track) t;
  select t.gps_m, t.points, t.first_at, t.last_at
    into v_gps_m, v_points_n, v_first_at, v_last_at
    from economy.track_summary(v_track) t;
  -- 경로 거리는 러닝 시간 안에 달릴 수 있는 만큼만(시속 25km). 경로 시각은 앞뒤로 여유를
  -- 두므로, 아주 짧은 러닝 여러 개에 같은 몇 분짜리 경로를 붙여 거리를 여러 번 받지 못하게.
  v_gps_m := least(coalesce(v_gps_m, 0), greatest(v_elapsed, 1) * 7.0);

  -- ── 판정 ──
  if coalesce(p_mock_location, false) then
    v_verdict := 'VOID';
    v_reason := '가짜 위치가 감지되었습니다';

  -- 과속으로 멈춘 뒤 10분(러닝 금지 시간) 안에 시작한 러닝은 적립하지 않는다 (0065)
  elsif exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user
           and p_started_at >= x.stopped_at and p_started_at < x.banned_until) then
    v_verdict := 'VOID';
    v_reason := '과속으로 멈춘 뒤 러닝 금지 시간에 시작한 러닝입니다';

  -- 짧은 러닝도 실제 시간으로 잰다(예전엔 60초로 쳐서 1ms 러닝에 240걸음을 줬다)
  elsif p_steps::numeric * 60 / greatest(v_elapsed, 1) > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';

  elsif v_points_n >= 2 and (
        v_first_at < (extract(epoch from p_started_at) * 1000)::bigint - economy.track_time_slack_ms()
     or v_last_at > (extract(epoch from p_ended_at) * 1000)::bigint + economy.track_time_slack_ms()) then
    v_verdict := 'VOID';
    v_reason := '경로 시각이 러닝 시간과 맞지 않습니다';

  elsif v_track <> '' and exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.track <> ''
           and md5(s.track) = md5(v_track) and s.started_at <> p_started_at) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로입니다';

  -- 같은 사람의 러닝은 시간이 겹칠 수 없다. 겹치게 여러 개를 올리면 같은 시간을
  -- 여러 번 받는다.
  elsif exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.verdict <> 'VOID'
           and s.started_at < p_ended_at and s.ended_at > p_started_at) then
    v_verdict := 'VOID';
    v_reason := '다른 러닝과 시간이 겹칩니다';

  elsif v_gps_m >= 1000 and v_step_m < v_gps_m * 0.2 then
    v_verdict := 'VOID';
    v_reason := '걸음 없이 이동한 거리입니다';
  end if;

  -- ── 검사를 통과한 걸음 ──
  if v_verdict = 'VOID' then
    v_verified := 0;
  else
    v_verified := p_steps;
    if v_gps_m >= economy.gps_check_min_m()
       and v_step_m > v_gps_m * economy.step_gps_max_ratio() then
      v_verified := floor(v_gps_m * economy.step_gps_max_ratio() / 0.762)::int;
      v_verdict := 'FLAGGED';
      v_reason := '걸음 수가 GPS 거리보다 많습니다';
    end if;
  end if;
  v_rewardable := v_verified;

  -- 과속이 30초 넘게 이어져 앱이 멈춘 러닝 — 적립 규칙은 그대로, 판정에 까닭을 남긴다 (0065)
  if v_verdict <> 'VOID' and exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user and x.run_started_at = p_started_at) then
    v_verdict := 'FLAGGED';
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '과속이 이어져 러닝이 멈췄습니다');
  end if;

  -- ── 하루 걸음 상한 (한국 하루) ──
  if v_rewardable > 0 then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and economy.game_day(s.started_at) = v_gday;
    if v_rewardable > economy.max_daily_steps() - v_already_steps then
      v_rewardable := greatest(economy.max_daily_steps() - v_already_steps, 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 무효가 잦으면 보류 ──
  -- 적립할 걸음이 없는 러닝도 본다 — 걸음 0 인 경로 러닝이 보류 중에도 잠금 거리 · 꺼내기 조건 거리를 받았다
  if v_verdict <> 'VOID' then
    select count(*) into v_recent_void from public.walk_sessions s
     where s.user_id = v_user and s.verdict = 'VOID'
       and s.started_at > now() - interval '7 days';
    if v_recent_void >= 5 then
      v_rewardable := 0;
      v_held := true;
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '최근 무효 러닝이 많아 적립을 보류합니다');
    end if;
  end if;

  -- ── 신발 · 에너지 ──
  select * into v_shoe from economy.equipped_sneaker(v_user);
  if not found then
    -- 신은 신발이 없으면(꺼내기로 벗었다) 첫 신발을 신긴다. 신발 없이 달리면 내구도 100 · 닳지 않음으로
    -- 쳐서, 닳은 신발을 신고 수리비를 내는 것보다 나았다. 첫 신발을 아직 안 받은 계정(앱은 로그인하자마자
    -- 받는다)은 예전처럼 신발 없이 계산한다 — 앱이 받기 전에 러닝이 먼저 올라가도 적립을 잃지 않게.
    update public.market_sneakers set equipped = true, updated_at = now()
     where id = (select m.id from public.market_sneakers m
                  where m.owner_id = v_user and m.origin = 'STARTER'
                    and m.chain_state = 'APP' and m.status = 'OWNED'
                  limit 1)
       and not exists (select 1 from public.market_sneakers e where e.owner_id = v_user and e.equipped);
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  if v_shoe.id is not null then
    -- 효율 · 착화감은 레벨 1 값 + 나눈 포인트, 내구도는 남은 비율(%) — 이 러닝 전의 값으로 배율을 정한다 (0059)
    select e.efficiency_bps, e.comfort_bps, e.durability into v_eff, v_comfort, v_dur
      from economy.shoe_effective(v_shoe) e;
  end if;

  -- 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 (0066) — 남은 내구도로 달릴 수 있던 시간(1분마다 5)만큼만.
  -- 적립 배율은 남은 비율이 0 보다 크면 그대로라(economy.durability_factor) 거의 닳은 신발로 오래 달려도 다 받던 것을 막는다
  if v_rewardable > 0 and v_shoe.id is not null
     and v_shoe.dur_now * economy.durability_sec_per_point() < v_elapsed then
    v_rewardable := floor(v_rewardable * v_shoe.dur_now * economy.durability_sec_per_point()
                          / greatest(v_elapsed, 1)::numeric)::int;
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
  end if;

  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
  end if;

  -- 크루 러닝 — 그 크루의 함께 달리기 방에서 출발해, 출발 2분 뒤에도 200m 안에 2명 이상(나 포함)이 함께였고(보너스 인원과
  -- 같은 무리 — party_for), 내가 그 크루원이다. 크루 방에서 혼자 · 번개 방 · 크루를 나간 뒤 올린 러닝은 크루 러닝이 아니다 (0069)
  if v_party_id is not null and v_party >= 2 and v_verdict <> 'VOID' then
    select p.crew_id into v_crew from public.parties p where p.id = v_party_id;
    if v_crew is not null and not exists (select 1 from public.crew_members m
                                           where m.crew_id = v_crew and m.user_id = v_user) then
      v_crew := null;
    end if;
  end if;
  -- 크루 러닝이고, 러닝을 시작한 날(한국 시각) 크루 체력을 나눠 받는 크루원(그날 전에 가입 · 그 전 7일 안에 달림)이면
  -- 개인 에너지 대신 크루 체력을 쓴다. 나눠 받지 않는 크루원(갓 가입 · 오래 쉼)은 크루 러닝도 개인 에너지로 (0069)
  if v_crew is not null then
    v_stamina := economy.crew_stamina_day(v_crew, v_gday);
    select m.used into v_my_used from economy.crew_stamina_members m
     where m.crew_id = v_crew and m.day = v_gday and m.user_id = v_user;
    if found then
      v_pool := 'CREW';
    end if;
  end if;

  if v_rewardable > 0 then
    -- 내 몫 = 그날 크루 체력 ÷ 나눠 받는 크루원 수(2026-10-21 사용자 결정 "크루 체력 ÷ 기여 인원"). 크루 체력을 다 쓰면 그 뒤
    -- 걸음은 적립하지 않는다 — 개인 에너지로 넘어가지 않는다(사용자 결정 "그 뒤는 적립 없음"). 쓰는 양은 개인 에너지와 같다
    -- (적립 걸음 600 에 1, 착화감만큼 덜)
    v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
    if v_pool = 'CREW' then
      v_energy_left := greatest(v_stamina.share - v_my_used, 0);
    else
      v_energy_left := economy.energy_left(v_user, v_gday);
    end if;
    if v_rewardable * v_energy_per_step > v_energy_left then
      v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
      v_reason := concat_ws(' · ', nullif(v_reason, ''),
                            case when v_pool = 'CREW' then '크루 체력을 다 썼습니다' else '에너지를 다 썼습니다' end);
    end if;
    v_energy_used := round(v_rewardable * v_energy_per_step, 4);
  end if;

  if economy.boost_active(v_user, 'XP_BOOSTER', p_started_at) then
    v_xp := 2;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * (1 + v_eff / 10000.0)
      * economy.durability_factor(v_dur)
      * economy.party_multiplier(v_party)
      * v_xp,
    4
  );
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  if v_points > 0 then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap - v_already_points then
      v_points := greatest(round(v_cap - v_already_points, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 금액 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 걸음만 있는 러닝 · 한꺼번에 올린 러닝 ──
  --
  -- 경로가 없으면 걸음은 폰이 지어낼 수 있다. 그런 러닝은 하루 적은 몫만 준다.
  -- 7일치를 모아 한 번에 올리면 날마다 상한이 새로 열리므로, 올린 날 기준 상한도 둔다.
  v_gps_backed := v_verdict <> 'VOID' and v_gps_m >= economy.gps_check_min_m();
  if v_points > 0 and not v_gps_backed then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- 상한에 걸려 덜 받았으면 받은 몫만큼만 에너지를 쓴다 — 쓴 에너지 × 받은 SUP ÷ 상한 전 SUP (0068).
  -- 예전엔 하루 금액 상한에 걸려 0 SUP 를 받은 러닝도 에너지를 다 썼다. 적립 걸음(rewarded_steps)은 그대로 둔다
  if v_energy_used > 0 and v_points < v_points_full then
    v_energy_used := round(v_energy_used * v_points / v_points_full, 4);
  end if;

  v_distance_m := case
    -- 걸음이 있어야 100m 여유를 준다 — 걸음 0 인 경로만으로 거리를 받지 않게
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_verified, 0) * 0.762 * 3
                                          + case when v_verified > 0 then 100 else 0 end)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- 잠금 거리 · 꺼내기 조건에 쳐 주는 거리 — 경로로 잰 것만, 하루 상한 안에서
  if v_gps_backed and not v_held then
    select greatest(economy.setting_num('gps_km_daily_cap') * 1000 - coalesce(sum(s.gps_credit_m), 0), 0)
      into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    v_credit_m := least(v_distance_m, v_cap_left);
  end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  if v_energy_used > 0 then
    if v_pool = 'CREW' then
      update economy.crew_stamina_members m set used = m.used + v_energy_used
       where m.crew_id = v_crew and m.day = v_gday and m.user_id = v_user;
    else
      insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
      on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
    end if;
  end if;

  -- 크루 레벨 — 크루 러닝의 인정 거리를 크루에 쌓는다(함께 달린 사람마다 자기 거리). 보류 중인 러닝은 쌓지 않는다 (0069)
  if v_crew is not null and v_verdict <> 'VOID' and not v_held and v_distance_m > 0 then
    update public.crews c
       set run_km = c.run_km + round((v_distance_m / 1000)::numeric, 3),
           level = greatest(coalesce(c.level, 1), economy.crew_level_for_km(c.run_km + round((v_distance_m / 1000)::numeric, 3)))
     where c.id = v_crew;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064). 달린 시간은 멈춘 시간을 뺀 값(v_elapsed).
  -- 거리 · 걸음과 상관없다 — 켜 두고 움직이지 않아도 닳는다(0068, 2026-10-19 사용자 결정. 예전엔 거리 0 이면 닳지 않았다).
  -- 무효 러닝은 닳지 않는다. 남은 비율(durability_pts)은 트리거가 다시 센다
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0),
           updated_at = now()
     where s.id = v_shoe.id;
  end if;

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           gps_km = gps_km + (v_credit_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
end;
$$;


comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지(크루 러닝은 크루 체력)·파티·부스터·상한 모두 서버 값으로.';

-- ── 앱이 읽는 크루 체력 ──────────────────────────────────────────────

-- 오늘(한국 시각)의 크루 체력 — 크루원만. 크루 홈 · 레벨 화면 · 대기실 · 러닝 중 예상 적립이 읽는다
create or replace function public.crew_stamina(p_crew uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v economy.crew_stamina_days;
  v_used numeric;
  v_me economy.crew_stamina_members;
  v_km numeric;
  v_level int;
begin
  -- 크루 홈과 같은 문 — 지금 이 크루의 멤버가 아니면 crew_not_member(42501, 0049)
  perform public.crew_home_member(p_crew);
  v := economy.crew_stamina_day(p_crew, v_day);
  select coalesce(sum(m.used), 0) into v_used
    from economy.crew_stamina_members m where m.crew_id = p_crew and m.day = v_day;
  select * into v_me from economy.crew_stamina_members m
   where m.crew_id = p_crew and m.day = v_day and m.user_id = v_user;
  select c.run_km, c.level into v_km, v_level from public.crews c where c.id = p_crew;
  return jsonb_build_object(
    'day', v_day,
    'max', v.max,
    'used', v_used,
    'left', greatest(v.max - v_used, 0),
    'contributors', v.contributors,
    'share', v.share,
    -- 오늘 체력을 정한 때의 레벨과 그 보너스 — 레벨이 오늘 올랐으면 보너스는 내일부터
    'level', v.level,
    'level_bonus', economy.crew_level_bonus() * (v.level - 1),
    'me', jsonb_build_object(
      'user_id', v_user,
      'contributor', v_me.user_id is not null,
      'contribution', coalesce(v_me.contribution, 0),
      'used', coalesce(v_me.used, 0),
      'left', case when v_me.user_id is null then 0 else greatest(v.share - v_me.used, 0) end),
    'members', coalesce((
      -- 나눠 받는 크루원과 보탠 양만 — 사람마다 쓴 양은 크루원에게 보이지 않는다(합계 used 만)
      select jsonb_agg(jsonb_build_object(
               'user_id', m.user_id, 'name', coalesce(pr.display_name, '러너'), 'contribution', m.contribution)
             order by m.contribution desc, pr.display_name)
        from economy.crew_stamina_members m left join public.profiles pr on pr.id = m.user_id
       where m.crew_id = p_crew and m.day = v_day), '[]'::jsonb),
    -- 지금 크루 레벨(크루 러닝이 아직 없으면 null — "새 크루") · 누적 거리 · 다음 레벨이 되는 거리(10 이면 null)
    'crew_level', v_level,
    'run_km', v_km,
    'next_level_km', economy.crew_level_km(coalesce(v_level, 1) + 1));
end;
$$;
revoke all on function public.crew_stamina(uuid) from public, anon;
grant execute on function public.crew_stamina(uuid) to authenticated;

-- 레벨 안내 — 0047 그대로(레벨이 비어 있으면 null — "새 크루") + 레벨 기준(크루 러닝 누적 거리) · 다음 레벨까지 · 레벨 보너스 (0069)
create or replace function public.crew_level(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_level int;
  v_run_km numeric;
  v_km double precision;
  v_runners int;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select c.level, c.run_km into v_level, v_run_km
    from public.crews c where c.id = p_crew and not public.is_hidden('CREW', c.id::text);
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  select w.km, w.runners into v_km, v_runners from public.crew_week_stats(p_crew) w;
  return jsonb_build_object(
    'level', v_level,
    'week_km', coalesce(v_km, 0),
    'week_runners', coalesce(v_runners, 0),
    'member_count', (select count(*) from public.crew_members m where m.crew_id = p_crew),
    -- 크루 러닝 누적 거리 · 다음 레벨이 되는 거리(10 이면 null) · 지금 레벨이 더하는 크루 체력 · 레벨 표 (0069)
    'run_km', v_run_km,
    'next_level_km', economy.crew_level_km(coalesce(v_level, 1) + 1),
    'level_bonus', economy.crew_level_bonus() * (coalesce(v_level, 1) - 1),
    'levels', (select jsonb_agg(jsonb_build_object('level', l, 'km', coalesce(economy.crew_level_km(l), 0),
                                                   'bonus', economy.crew_level_bonus() * (l - 1)) order by l)
                 from generate_series(1, 10) l));
end;
$$;
revoke execute on function public.crew_level(uuid) from public, anon;
grant execute on function public.crew_level(uuid) to authenticated;
