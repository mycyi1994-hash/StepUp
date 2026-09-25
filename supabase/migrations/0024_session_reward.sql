-- 러닝 보상 — 서버가 신발 · 에너지 · 파티 · 부스터까지 모두 계산한다.
--
-- 0018 까지는 폰이 부스트(bps)와 파티 인원을 보냈고 서버는 그 값을 믿었다.
-- 이제 폰이 보내는 값 가운데 믿는 것은 걸음 · 시각 · 경로뿐이고, 그것도 검사한다.
--
--   가짜 위치 앱          폰이 "모의 위치"라고 알리면 보상 0 (VOID)
--   파티 인원             파티 출발 때 서버가 적어 둔 명단으로 센다 (결정표 X2)
--   신발                  서버 신발 표에서 지금 신은 신발의 효율성 · 착화감 · 내구도
--   에너지                서버가 센다. 한국 자정에 다시 찬다
--   하루 상한             걸음 48,000 보 + 금액 상한(신규 계정은 절반)
--   무효가 잦은 사람      최근 7일에 무효 러닝이 5번 이상이면 적립을 보류
--
-- 인자는 옛 앱과 맞춘다. p_boost_bps · p_party_size · p_faction 은 받기만 하고 쓰지 않는다.

-- ══════════════════════════════════════════════════════════════════
-- 세션 표에 칸 더하기
-- ══════════════════════════════════════════════════════════════════
alter table public.walk_sessions
  add column if not exists mock_location boolean not null default false,
  -- 검사를 통과한 걸음(에너지 · 금액 상한을 걸기 전). 목표 달성과 도전에 쓴다.
  add column if not exists verified_steps int,
  add column if not exists energy_used numeric(10, 4) not null default 0,
  add column if not exists sneaker_id bigint,
  -- 경로가 받쳐 준 러닝인가(경로 300m 이상). 걸음만 있는 러닝은 폰이 걸음을 지어낼 수 있다.
  add column if not exists gps_backed boolean not null default false,
  -- 경로가 받쳐 준 걸음. 목표 달성 · 주간 도전은 이것만 센다.
  add column if not exists backed_steps int not null default 0,
  -- 신발 잠금 거리 · 꺼내기 조건에 쳐 주는 거리(m). 경로로 잰 거리만, 하루 상한 안에서.
  add column if not exists gps_credit_m double precision not null default 0,
  -- 파티 보너스를 받은 파티. 한 파티에서 한 러닝만 받는다.
  add column if not exists party_id bigint;

-- 꺼내기 조건(누적 거리)은 경로로 잰 거리만 센다. lifetime_km 는 순위용이라 걸음 거리도 들어간다.
alter table public.profiles add column if not exists gps_km double precision not null default 0;

-- 운영 값 — 걸음만 있는 러닝의 하루 적립 상한, 올린 날 기준 하루 적립 상한, 하루에 쳐 주는 경로 거리
insert into public.economy_settings (key, value) values
  ('no_gps_daily_cap',  '60'::jsonb),
  ('upload_daily_cap',  '1200'::jsonb),
  ('gps_km_daily_cap',  '50'::jsonb)
on conflict (key) do nothing;

-- 효율성은 레전더리 30레벨에서 +27.5% 까지 오른다. 예전 상한(20%)으로는 못 담는다.
alter table public.walk_sessions drop constraint if exists walk_sessions_boost_bps_check;
alter table public.walk_sessions add constraint walk_sessions_boost_bps_check
  check (boost_bps between 0 and 5000);

-- ══════════════════════════════════════════════════════════════════
-- 파티 러닝 명단 — 출발할 때 적어 둔다 (결정표 X2)
-- ══════════════════════════════════════════════════════════════════
--
-- 참가자는 러닝을 마치면 방에서 나가고(party_leave) 방 명단에서 지워진다.
-- 그래서 출발 순간의 명단을 따로 남긴다. 적립 때는 이 명단의 인원만 믿는다.
create table if not exists public.party_runs (
  party_id bigint not null references public.parties on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  starts_at timestamptz not null,
  primary key (party_id, user_id)
);

create index if not exists party_runs_user_time on public.party_runs (user_id, starts_at);

alter table public.party_runs enable row level security;
revoke all on public.party_runs from anon, authenticated;

create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare v_starts timestamptz := now() + interval '4 seconds';
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;
  update public.parties
     set status = 'COUNTDOWN', starts_at = v_starts
   where id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at)
  select p_party, m.user_id, v_starts from public.party_members m where m.party_id = p_party
  on conflict do nothing;
end;
$$;

-- 이 러닝의 파티. 출발 시각이 러닝 시작과 10분 안쪽인 파티 명단에서 찾는다.
-- 인원은 그 명단의 사람 수다.
drop function if exists economy.party_size_for(uuid, timestamptz);
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with p as (
    select r.party_id from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
     order by abs(extract(epoch from (r.starts_at - p_started_at)))
     limit 1)
  select p.party_id, (select count(*)::int from public.party_runs r2 where r2.party_id = p.party_id)
    from p
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;

-- ══════════════════════════════════════════════════════════════════
-- 적립 (0018 을 대신한다)
-- ══════════════════════════════════════════════════════════════════

-- 인자가 하나 늘어서(p_mock_location) 옛 8개짜리를 먼저 치운다. 둘 다 있으면
-- 옛 앱의 호출이 어느 쪽인지 모호해져 실패한다.
drop function if exists public.record_session(timestamptz, timestamptz, int, int, text, int, int, text);

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
    select e.efficiency_bps, e.comfort_bps, e.durability into v_eff, v_comfort, v_dur
      from economy.sneaker_effective(v_shoe.origin, v_shoe.rarity, v_shoe.level,
             v_shoe.efficiency_bps, v_shoe.comfort_bps, v_shoe.durability_pts) e;
  end if;

  if v_rewardable > 0 then
    v_energy_left := economy.energy_left(v_user, v_gday);
    v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
    if v_rewardable * v_energy_per_step > v_energy_left then
      v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
    end if;
    v_energy_used := round(v_rewardable * v_energy_per_step, 4);
  end if;

  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
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
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 낮아 적립이 줄었습니다');
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
    gps_backed, backed_steps, gps_credit_m, party_id
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  if v_energy_used > 0 then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_distance_m > 0 then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           durability_pts = greatest(s.durability_pts
             - round((v_distance_m / 1000)::numeric * economy.durability_loss_per_km(s.rarity), 2), 0),
           updated_at = now()
     where s.id = v_shoe.id;
    update public.market_sneakers s set durability = floor(s.durability_pts)::int
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
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지·파티·부스터·상한 모두 서버 값으로.';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 목표 달성 보너스 — 서버가 확인한 걸음으로
-- ══════════════════════════════════════════════════════════════════
--
-- 앱의 RewardEconomy.goalBonus 와 같은 식: 목표 1,000보당 2.5 SUP, 연속 달성
-- 하루당 +10% (7일까지). 걸음은 폰의 걸음 센서가 아니라 서버가 검사한 러닝의
-- 걸음(verified_steps)만 센다 — 폰이 올린 하루 걸음(daily_steps)은 누구나 적어 넣을 수 있다.
create or replace function economy.verified_steps_on(p_user uuid, p_day date) returns int
language sql stable security definer set search_path = public, economy as $$
  select least(coalesce(sum(s.backed_steps), 0), economy.max_daily_steps())::int
    from public.walk_sessions s
   where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED')
     and economy.game_day(s.started_at) = p_day
$$;
revoke all on function economy.verified_steps_on(uuid, date) from public;

create or replace function public.goal_claim()
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
  v_goal int;
  v_streak int := 1;
  v_day date;
  v_bonus numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  select least(greatest(coalesce(p.daily_goal, 8000), 1000), 30000) into v_goal
    from public.profiles p where p.id = v_user;

  if economy.verified_steps_on(v_user, v_today) < v_goal then
    raise exception '아직 목표를 채우지 않았습니다' using errcode = '23514';
  end if;

  -- 어제부터 거꾸로 — 보너스를 받았거나 스트릭 보호가 덮은 날이 이어진 만큼
  v_day := v_today - 1;
  while v_streak < 8 loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day)
      or exists (select 1 from public.boosts b
                  where b.user_id = v_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(v_day + 1)
                    and b.ends_at > economy.game_day_start(v_day)));
    v_streak := v_streak + 1;
    v_day := v_day - 1;
  end loop;

  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  -- 같은 날 두 번 받으면 ref 가 겹쳐 23505 로 거절된다.
  perform economy.ledger_apply(v_user, 'BONUS_GOAL', v_bonus,
                               format('목표 달성 보너스 (%s일 연속)', v_streak), 'goal:' || v_today);
  return v_bonus;
end $$;

revoke all on function public.goal_claim() from public, anon;
grant execute on function public.goal_claim() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 도전 진행 — 주간 걸음도 서버가 확인한 걸음으로
-- ══════════════════════════════════════════════════════════════════
create or replace function public.event_progress(p_event text, p_tz text default 'Asia/Seoul')
returns double precision
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  -- 게임의 하루 · 주는 한국 시각이다. 폰이 보낸 시간대는 쓰지 않는다(바꿔 가며 두 번 받지 못하게).
  v_tz text := 'Asia/Seoul';
  v_today date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  if p_event = 'daily_goal' then
    -- 오늘의 도전(하루 목표) — 목표 보너스(goal_claim)가 세는 것과 같은 걸음(서버가 확인한 러닝 걸음)
    return economy.verified_steps_on(v_user, v_today);
  elsif p_event = 'step_surge' then
    -- 예전에는 폰이 올린 하루 걸음(daily_steps)을 더했다. 그 값은 폰이 마음대로
    -- 적을 수 있어 250 SUP 가 거저 나갔다. 경로가 받쳐 준 러닝 걸음만, 하루 상한까지 센다.
    -- 받는 단위(이번 ISO 주)와 같은 기간만 센다 — 최근 7일로 세면 지난주 걸음으로 이번 주를 또 받는다.
    return coalesce((
      select sum(economy.verified_steps_on(v_user, d::date))
        from generate_series(v_today - (extract(isodow from v_today)::int - 1), v_today, interval '1 day') d
    ), 0);
  elsif p_event = 'night_quest' then
    return coalesce((
      -- 경로가 받쳐 준 러닝만 — 걸음만 있는 러닝의 거리(걸음 × 0.762)는 폰이 지어낼 수 있다
      select sum(s.distance_meters) / 1000.0 from public.walk_sessions s
       where s.user_id = v_user
         and s.verdict not in ('FLAGGED', 'VOID')
         -- 0024 전 기록은 gps_backed 가 없다 — 서버가 경로로 잰 거리(0018)로 본다
         and (s.gps_backed or s.gps_distance_m >= economy.gps_check_min_m())
         and extract(hour from s.started_at at time zone v_tz) >= economy.night_from_hour()
    ), 0);
  end if;
  raise exception '없는 도전입니다' using errcode = '22023';
end;
$$;

-- 코스 경로의 실제 길이(m). 경로는 "위도,경도[,…];…" — 시각이 없어도 된다. 점이 5개보다 적으면 0.
create or replace function economy.course_length_m(p_track text) returns double precision
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_plat double precision;
  v_plng double precision;
  v_total double precision := 0;
  v_n int := 0;
begin
  foreach v_chunk in array string_to_array(coalesce(p_track, ''), ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    continue when v_lat not between -90 and 90 or v_lng not between -180 and 180;
    if v_n > 0 then
      v_total := v_total + economy.haversine_m(v_plat, v_plng, v_lat, v_lng);
    end if;
    v_plat := v_lat; v_plng := v_lng; v_n := v_n + 1;
  end loop;
  return case when v_n < 5 then 0 else v_total end;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 코스 완주 보상 — 서버가 확인한 완주에만
-- ══════════════════════════════════════════════════════════════════
--
-- 코스 거리는 만든 사람이 적은 값이라 믿지 않는다. 실제로 달린 거리와 코스
-- 거리 중 짧은 쪽으로 1km 당 1 SUP, 최대 42 SUP. 같은 코스는 하루에 한 번만.
create or replace function public.course_run_submit(p_course_track text, p_started_at timestamptz)
returns table (
  course_id bigint,
  duration_sec int,
  rank int,
  runners int
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_course bigint;
  v_course_track text;
  v_course_km double precision;
  v_session public.walk_sessions%rowtype;
  v_inserted bigint;
  v_reward numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  v_course := public.course_by_track(p_course_track);
  if v_course is null then
    return;
  end if;
  select c.track, c.distance_km into v_course_track, v_course_km from public.courses c where c.id = v_course;

  select * into v_session
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if not found then
    raise exception '러닝 기록이 서버에 없습니다' using errcode = '22023';
  end if;
  if v_session.verdict = 'VOID' or v_session.duration_sec <= 0 then
    raise exception '인정되지 않은 러닝입니다' using errcode = '23514';
  end if;
  if economy.course_coverage(v_course_track, v_session.track) < 0.8 then
    raise exception '코스를 따라 달리지 않았습니다' using errcode = '23514';
  end if;

  insert into public.course_runs (course_id, user_id, session_id, duration_sec)
  values (v_course, v_user, v_session.id, v_session.duration_sec)
  on conflict do nothing
  returning id into v_inserted;

  if v_inserted is not null then
    update public.courses set run_count = run_count + 1 where id = v_course;

    -- 남이 만든 코스만, 경로로 잰 거리로, 하루 한 번. 자기 코스를 여러 개 만들어
    -- 러닝 하나로 보상을 여러 번 받지 못하게 한다.
    -- 코스 거리도 만든 사람이 적은 값 대신 경로로 잰 길이까지만 — 점 하나짜리 코스에 42km 를
    -- 적어 두고 아무 러닝에나 보상을 받지 못하게.
    v_reward := least(floor(least(coalesce(v_course_km, 0), economy.course_length_m(v_course_track) / 1000.0,
                                  v_session.gps_credit_m / 1000.0)), 42);
    if v_reward >= 1 and (select c.owner_id from public.courses c where c.id = v_course) <> v_user then
      begin
        perform economy.ledger_apply(v_user, 'EARN_COURSE', v_reward, '코스 완주 보상',
          'course:' || economy.game_day(v_session.started_at));
      exception when unique_violation then
        null;  -- 이 날의 코스 보상은 이미 받았다. 기록은 남기고 보상만 건너뛴다.
      end;
    end if;
  end if;

  return query
    with best as (
      select r.user_id, min(r.duration_sec) as t
        from public.course_runs r
       where r.course_id = v_course
       group by r.user_id
    )
    select v_course,
           (select b.t from best b where b.user_id = v_user),
           (select count(*)::int + 1 from best b
             where b.t < (select b2.t from best b2 where b2.user_id = v_user)),
           (select count(*)::int from best);
end;
$$;
