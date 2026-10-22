-- ════════════════════════════════════════════════════════════════════
--  0071 — 크루 러닝: 크루 · 하루 하나의 공용 체력(공용 내구도) · 출발 때 참여 확정 · 실시간 차감 · 2분 인원 확인
--         (2026-10-22 크루 러닝 핸드오프 R01 ~ R13)
--
--  0070(무리별 크루 체력을 사람 수로 나눈 몫)을 대신한다. 0070 표(crew_run_days)는 남겨 두고 쓰지 않는다.
--
--  R01 오늘 크루 체력 = 그 크루 · 그 한국 날짜에 하나. 참가자가 "실제로 출발할 때" 고른 신발의 최대 내구도 × (1 + 레벨 체력 보너스)를
--      더한다(남은 개인 내구도가 아니다). 대기실 입장 · 신발 고르기 · 준비만으로는 더하지 않는다. 방이 달라도 같은 풀이다.
--      0/0 은 아직 기여가 없는 새 하루, 0/816 은 다 쓴 것 — 어느 쪽도 출발을 막지 않는다. 자정(한국)에 새 풀, 지난 풀은 남긴다.
--  R02 달리는 사람마다 1분에 5(12초에 1)를 공용 체력에서 쓴다. 일시정지한 사람은 쓰지 않는다. 개인 신발은 닳지 않는다.
--      앱이 달리는 동안 5초마다 상태를 알리고(crew_run_beat), 서버가 그때마다 풀을 정산한다. 30초 넘게 소식이 없는 사람은
--      쓰지 않는 것으로 본다(끝난 것으로 보지는 않는다 — D09).
--  R03 0 이 된 때부터 적립을 멈춘다(기록은 계속). 풀이 0 이던 구간을 따로 적어 두고(crew_pool_empty), 정산 때 그 구간에
--      달린 경로 거리만큼 적립 걸음을 뺀다(경로가 없으면 시간 비율). 새 참가자의 기여로 다시 생기면 그 뒤로는 다시 적립한다(D06).
--  R04 보상의 기본은 출발 때 고른 신발(스냅샷)의 효율로 기존 공식 그대로. 평균 내거나 나누지 않는다.
--  R05 사용자 · 한국 날짜에 한 번(크루를 바꿔도). 출발이 서버에서 확정될 때(party_start) 참여 기록 · 신발 스냅샷 · 공용 기여를
--      같은 트랜잭션으로 적는다. 끝내면 그날 다시 들어올 수 없다.
--  R06 일시정지 · 다시 달리기 가능. 다른 참가자가 모두 끝내 한 사람만 남으면 그 사람도 서버가 끝낸다(LAST_PARTICIPANT_END).
--  R07 인원 보너스 인원 = 같은 크루 · 출발 무렵(출발 90초 안)과 출발 2분 뒤 모두 서로 500m 안(곁 기록 party_near로 이어진 무리) ·
--      휴대폰 인증(auth.users.phone_confirmed_at)이 확인된 사람. 출발 2분(+15초) 뒤 서버가 한 번 정한다(이후 바꾸지 않음 — D02).
--      미인증자도 참가 · 기여 · 자기 신발 보상은 그대로, 인원에서만 빠진다(economy_settings.crew_bonus_requires_phone).
--  R08 · R10 보상 = B + B × 인원 보너스(구간 하나) + B × 레벨 보너스. 확정 인원 보너스는 러닝 전체 적립에 적용한다(D01).
--  R09 경험치 = 유효 크루 러닝의 사람마다 자기 거리. 레벨은 내려가지 않는다. 레벨 업 기록(crew_level_events)은 레벨마다 한 번.
--  R11 개인 하루 상한 · 에너지 · 신발 내구도와 따로 — 크루 보상 상한은 없다.
--  R13 크루 대기실 출발은 준비한 크루원 2명 이상(혼자 출발 버튼은 앱에서 막는다).
-- ════════════════════════════════════════════════════════════════════

-- ── 설정 · 휴대폰 인증 ────────────────────────────────────────────────

insert into public.economy_settings (key, value) values
  -- 인원 보너스에 휴대폰 인증을 요구하는가(R07 · R12). 끄는 것은 사용자 결정으로만
  ('crew_bonus_requires_phone', 'true'::jsonb)
on conflict (key) do nothing;

-- 서버가 믿을 수 있는 휴대폰 인증 — Supabase 인증(auth.users.phone_confirmed_at). 앱이 쓰거나 보낼 수 없다
create or replace function economy.phone_verified(p_user uuid) returns boolean
language sql stable security definer set search_path = '' as $$
  select coalesce((select u.phone_confirmed_at is not null from auth.users u where u.id = p_user), false)
$$;
revoke all on function economy.phone_verified(uuid) from public;

-- 인원 보너스에 셀 수 있는 사람인가(인증 조건만 — 크루 · 거리는 따로 본다)
create or replace function economy.crew_bonus_person(p_user uuid) returns boolean
language sql stable security definer set search_path = public, economy as $$
  select coalesce((economy.setting('crew_bonus_requires_phone') #>> '{}')::boolean, true) = false
      or economy.phone_verified(p_user)
$$;
revoke all on function economy.crew_bonus_person(uuid) from public;

-- 한 사람이 1초에 쓰는 공용 체력(1분에 5)
create or replace function economy.crew_drain_per_sec() returns numeric
language sql immutable set search_path = '' as $$ select 5 / 60.0 $$;
revoke all on function economy.crew_drain_per_sec() from public;

-- ── 표 ───────────────────────────────────────────────────────────────

-- 크루 · 한국 날짜의 공용 체력 — 총량(기여 합, 분모)과 쓴 양. 남은 양 = capacity − consumed
create table if not exists economy.crew_day_pools (
  crew_id uuid not null references public.crews on delete cascade,
  day date not null,
  -- 소수 자릿수를 정하지 않는다 — 5초마다 정산해도 반올림이 쌓이지 않게(화면이 소수 한 자리로 보인다)
  capacity numeric not null default 0,
  consumed numeric not null default 0,
  contributors int not null default 0,
  -- 이 때까지 쓴 양을 셌다
  settled_at timestamptz not null default now(),
  version bigint not null default 0,
  -- 지금 0 이면 0 이 된 때
  exhausted_at timestamptz,
  -- 20% 아래로 처음 내려간 때(같은 소진 구간에서 알림을 되풀이하지 않게)
  low_at timestamptz,
  created_at timestamptz not null default now(),
  primary key (crew_id, day),
  constraint crew_day_pools_sane check (consumed >= 0 and capacity >= 0 and consumed <= capacity + 0.001)
);
comment on table economy.crew_day_pools is '크루 · 한국 날짜의 공용 체력 — 출발 때 기여, 달리는 사람마다 1분에 5 (0071)';

-- 공용 체력이 0 이던 구간 — 이 구간에 달린 걸음은 적립하지 않는다(to_at 이 비었으면 아직 0)
create table if not exists economy.crew_pool_empty (
  crew_id uuid not null,
  day date not null,
  from_at timestamptz not null,
  to_at timestamptz,
  primary key (crew_id, day, from_at),
  foreign key (crew_id, day) references economy.crew_day_pools (crew_id, day) on delete cascade
);

-- 크루 러닝 참여 — 사용자 · 한국 날짜에 하나(crew_id 는 키가 아니다 — 크루를 바꿔도 하루 한 번)
create table if not exists economy.crew_runs (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  crew_id uuid references public.crews on delete set null,
  party_id bigint not null,
  started_at timestamptz not null,
  state text not null default 'RUNNING' check (state in ('RUNNING', 'PAUSED', 'ENDED')),
  last_beat_at timestamptz not null default now(),
  ended_at timestamptz,
  -- USER_END · LAST_PARTICIPANT_END · RECORDED(올린 러닝으로 끝남)
  terminal_reason text,
  -- 출발 때 고른 신발 스냅샷
  sneaker_id bigint,
  shoe_dur_max int not null default 0,
  shoe_eff_bps int not null default 0,
  crew_level int not null default 1,
  contribution numeric(14, 4) not null default 0,
  -- 내가 쓴 공용 체력 · 달린 초(서버가 센 값)
  consumed numeric not null default 0,
  active_sec numeric not null default 0,
  -- 달린 초 중 공용 체력이 0 이던 초 — 경로가 없는 러닝의 적립 몫을 일시정지와 상관없이 센다
  empty_sec numeric not null default 0,
  -- 인원 보너스 — PENDING(2분 전) · CONFIRMED · UNAVAILABLE
  bonus_status text not null default 'PENDING' check (bonus_status in ('PENDING', 'CONFIRMED', 'UNAVAILABLE')),
  bonus_count int not null default 0,
  bonus_pct numeric(6, 4) not null default 0,
  bonus_reason text,
  level_pct numeric(6, 4) not null default 0,
  -- 정산 — 올린 러닝 하나와 짝(같은 러닝을 다시 올려도 바뀌지 않는다)
  session_id bigint,
  base_sup numeric(20, 4),
  headcount_sup numeric(20, 4),
  level_sup numeric(20, 4),
  total_sup numeric(20, 4),
  xp_m numeric(14, 1),
  level_before int,
  level_after int,
  settled_at timestamptz,
  created_at timestamptz not null default now(),
  primary key (user_id, day)
);
create index if not exists crew_runs_crew_day on economy.crew_runs (crew_id, day);
create index if not exists crew_runs_party on economy.crew_runs (party_id);
comment on table economy.crew_runs is '크루 러닝 참여 — 사용자 · 한국 날짜에 한 번. 출발 때 만든다 (0071)';

-- 크루 레벨 업 — 레벨마다 한 번
create table if not exists economy.crew_level_events (
  crew_id uuid not null references public.crews on delete cascade,
  level int not null,
  reached_at timestamptz not null default now(),
  session_id bigint,
  run_km numeric(12, 3),
  primary key (crew_id, level)
);

alter table economy.crew_day_pools enable row level security;
alter table economy.crew_pool_empty enable row level security;
alter table economy.crew_runs enable row level security;
alter table economy.crew_level_events enable row level security;
revoke all on economy.crew_day_pools, economy.crew_pool_empty, economy.crew_runs, economy.crew_level_events
  from public, anon, authenticated;

-- 0070 으로 오늘(과 그 전) 크루 러닝을 올린 사람 — 하루 한 번이 끊기지 않게 참여로 옮겨 둔다(끝난 · 정산된 러닝)
insert into economy.crew_runs (user_id, day, crew_id, party_id, started_at, state, last_beat_at, ended_at, terminal_reason,
                               sneaker_id, crew_level, bonus_status, bonus_count, bonus_pct, level_pct, session_id, settled_at)
select d.user_id, d.day, d.crew_id, coalesce(d.party_id, 0), d.created_at, 'ENDED', d.created_at, d.created_at, 'RECORDED',
       d.sneaker_id, d.crew_level, 'CONFIRMED', d.runners, d.headcount_bonus, d.level_bonus, d.session_id, d.created_at
  from economy.crew_run_days d
on conflict (user_id, day) do nothing;

-- ── 공용 체력 정산 ───────────────────────────────────────────────────

-- 그 크루 · 날짜의 공용 체력을 p_at 까지 센다 — 직전 정산 뒤 달리는 사람(30초 안에 소식) × 1분에 5.
-- 한 번에 30초까지만 센다(소식이 끊긴 사이를 몰아 깎지 않는다). 풀 행을 잠근다 — 모든 크루 러닝 함수가 이 순서(풀 → 사람)로 잠근다.
create or replace function economy.crew_pool_settle(p_crew uuid, p_day date, p_at timestamptz default now())
returns economy.crew_day_pools
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v economy.crew_day_pools;
  v_dt numeric;
  v_n int;
  v_rate numeric;
  v_need numeric;
  v_left numeric;
  v_each numeric;
  v_use numeric;
  v_from timestamptz;
  v_empty_dt numeric;
begin
  insert into economy.crew_day_pools (crew_id, day, settled_at) values (p_crew, p_day, p_at)
  on conflict (crew_id, day) do nothing;
  select * into v from economy.crew_day_pools d where d.crew_id = p_crew and d.day = p_day for update;
  v_dt := extract(epoch from (p_at - v.settled_at));
  if v_dt <= 0 then
    return v;
  end if;
  v_dt := least(v_dt, 30);
  v_from := p_at - make_interval(secs => v_dt);
  select count(*)::int into v_n from economy.crew_runs r
   where r.crew_id = p_crew and r.day = p_day and r.state = 'RUNNING'
     and r.last_beat_at >= p_at - interval '30 seconds';
  if v_n > 0 then
    -- 1분에 5 = 12초에 1 — 나눗셈 한 번으로(5/60 을 먼저 반올림하면 오차가 쌓인다)
    v_rate := v_n / economy.durability_sec_per_point()::numeric;
    v_need := v_n * v_dt / economy.durability_sec_per_point()::numeric;
    v_left := greatest(v.capacity - v.consumed, 0);
    -- 사람마다 똑같이 — 풀에서 쓴 양은 사람마다 쓴 양의 합과 같다(반올림하지 않는다)
    v_each := least(v_need, v_left) / v_n;
    v_use := v_each * v_n;
    -- 이 사이 중 0 이던 초 — 처음부터 0 이면 전부, 이 사이에 0 이 됐으면 그 뒤만
    v_empty_dt := case when v_left <= 0 then v_dt
                       when v_need >= v_left then v_dt - v_left * economy.durability_sec_per_point() / v_n
                       else 0 end;
    update economy.crew_runs r
       set consumed = r.consumed + v_each,
           active_sec = r.active_sec + v_dt,
           empty_sec = r.empty_sec + v_empty_dt
     where r.crew_id = p_crew and r.day = p_day and r.state = 'RUNNING'
       and r.last_beat_at >= p_at - interval '30 seconds';
    v.consumed := v.consumed + v_use;
    -- 이 사이에 0 이 됐다 — 정확한 때부터 적립을 멈춘다
    if v_left > 0 and v_need >= v_left then
      v.consumed := v.capacity;
      v.exhausted_at := v_from + make_interval(secs => v_left * economy.durability_sec_per_point() / v_n);
      insert into economy.crew_pool_empty (crew_id, day, from_at) values (p_crew, p_day, v.exhausted_at)
      on conflict do nothing;
    -- 처음부터 0 인데 달리는 사람이 있다(기여 0 인 출발 · 다 쓴 뒤 열린 구간이 없는 경우) — 0 인 구간을 연다.
    -- 새 하루 0/0 은 화면에서 "새 하루"로 따로 보이지만(총량 0), 적립은 다 쓴 것과 같이 멈춘다
    elsif v_left <= 0 and not exists (select 1 from economy.crew_pool_empty x
                                       where x.crew_id = p_crew and x.day = p_day and x.to_at is null) then
      v.exhausted_at := coalesce(v.exhausted_at, v_from);
      insert into economy.crew_pool_empty (crew_id, day, from_at) values (p_crew, p_day, v_from)
      on conflict do nothing;
    end if;
    if v.low_at is null and v.capacity > 0 and v.capacity - v.consumed <= v.capacity * 0.2 then
      v.low_at := p_at;
    end if;
  end if;
  update economy.crew_day_pools d
     set consumed = v.consumed, settled_at = p_at, version = d.version + 1,
         exhausted_at = v.exhausted_at, low_at = v.low_at
   where d.crew_id = p_crew and d.day = p_day
  returning * into v;
  return v;
end;
$$;
revoke all on function economy.crew_pool_settle(uuid, date, timestamptz) from public;

-- 공용 체력 한 덩이(앱이 읽는 모양) — 남은 양 · 총량 · 쓴 양 · 기여한 사람 · 지금 쓰는 사람(크루 전체) · 0 인가 · 20% 아래인가
create or replace function economy.crew_pool_json(p_crew uuid, p_day date)
returns jsonb
language sql
stable
security definer
set search_path = public, economy
as $$
  select jsonb_build_object(
    'day', p_day,
    'total', coalesce(d.capacity, 0),
    'remaining', greatest(coalesce(d.capacity, 0) - coalesce(d.consumed, 0), 0),
    'consumed', coalesce(d.consumed, 0),
    'contributors', coalesce(d.contributors, 0),
    'active_runners', (select count(*) from economy.crew_runs r
                        where r.crew_id = p_crew and r.day = p_day and r.state = 'RUNNING'
                          and r.last_beat_at >= now() - interval '30 seconds'),
    'exhausted', d.capacity is not null and d.capacity > 0 and d.capacity - d.consumed <= 0,
    'low', d.capacity is not null and d.capacity > 0 and d.capacity - d.consumed <= d.capacity * 0.2,
    'version', coalesce(d.version, 0),
    'as_of', coalesce(d.settled_at, now()))
    from (select 1) one
    left join economy.crew_day_pools d on d.crew_id = p_crew and d.day = p_day
$$;
revoke all on function economy.crew_pool_json(uuid, date) from public;

-- ── 2분 인원 확인 ────────────────────────────────────────────────────

-- 출발 2분(+15초 — 위치 보고를 기다린다) 뒤 한 번 정한다. 끝난 뒤 정산에서는 2분이 지났으면 그때 정한다.
-- 출발 90초 안에 곁이었고 2분 뒤에도 곁이었던 짝으로 이어진 무리 중 지금 그 크루원 · 인증된 사람의 수.
create or replace function economy.crew_run_bonus_eval(p_user uuid, p_day date, p_final boolean default false)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  r economy.crew_runs;
  v_check timestamptz;
  v_n int;
  v_located boolean;
begin
  select * into r from economy.crew_runs c where c.user_id = p_user and c.day = p_day for update;
  if not found or r.bonus_status <> 'PENDING' then
    return;
  end if;
  v_check := r.started_at + interval '2 minutes';
  if now() < v_check + interval '15 seconds' and not p_final then
    return;
  end if;
  -- 2분이 되기 전에 끝났다 — 인원을 확인할 수 없다(인원 보너스 없음, D01)
  if (r.ended_at is not null and r.ended_at < v_check) or now() < v_check then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'ENDED_BEFORE_CHECK', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  v_located := exists (select 1 from public.party_members m
                        where m.party_id = r.party_id and m.user_id = p_user and m.pinged_at is not null)
            or exists (select 1 from public.party_near n
                        where n.party_id = r.party_id and (n.user_a = p_user or n.user_b = p_user));
  if not v_located then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'NO_LOCATION', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  with recursive e as (
    select n.user_a, n.user_b from public.party_near n
     where n.party_id = r.party_id
       and n.first_at <= r.started_at + interval '90 seconds'
       and n.last_at >= v_check),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select count(*)::int into v_n
    from g
    join economy.crew_runs c on c.user_id = g.u and c.party_id = r.party_id
    join public.crew_members m on m.crew_id = r.crew_id and m.user_id = g.u
   where economy.crew_bonus_person(g.u);
  update economy.crew_runs c
     set bonus_status = 'CONFIRMED', bonus_count = v_n, bonus_pct = economy.crew_headcount_bonus(v_n),
         bonus_reason = case when v_n < 2 then 'TOO_FEW' end
   where c.user_id = p_user and c.day = p_day;
end;
$$;
revoke all on function economy.crew_run_bonus_eval(uuid, date, boolean) from public;

-- 한 사람의 러닝이 끝났다 — 같은 방에서 끝나지 않은 사람이 한 명만 남으면 그 사람도 끝낸다(R06, 일시정지도 끝나지 않은 것)
create or replace function economy.crew_run_close(p_user uuid, p_day date, p_reason text)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  r economy.crew_runs;
  v_left int;
begin
  update economy.crew_runs c
     set state = 'ENDED', ended_at = coalesce(c.ended_at, now()), terminal_reason = coalesce(c.terminal_reason, p_reason)
   where c.user_id = p_user and c.day = p_day and c.state <> 'ENDED'
  returning * into r;
  if not found then
    return;
  end if;
  select count(*)::int into v_left from economy.crew_runs c where c.party_id = r.party_id and c.state <> 'ENDED';
  if v_left = 1 then
    update economy.crew_runs c
       set state = 'ENDED', ended_at = now(), terminal_reason = 'LAST_PARTICIPANT_END'
     where c.party_id = r.party_id and c.state <> 'ENDED';
  end if;
end;
$$;
revoke all on function economy.crew_run_close(uuid, date, text) from public;

-- ── 대기실 · 출발 ────────────────────────────────────────────────────

-- 준비 — 오늘 크루 러닝을 이미 한 사람은 크루 방에서 준비할 수 없다(하루 한 번, 크루를 바꿔도)
create or replace function public.party_ready(p_party bigint, p_ready boolean)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
begin
  perform public.party_assert_member(p_party);
  if not exists (select 1 from public.parties where id = p_party and status = 'LOBBY') then
    raise exception '이미 출발한 파티입니다' using errcode = '23514';
  end if;
  if p_ready and exists (select 1 from public.parties where id = p_party and crew_id is not null)
     and exists (select 1 from economy.crew_runs c
                  where c.user_id = auth.uid() and c.day = economy.game_day(now())) then
    raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
  end if;
  update public.party_members set ready = p_ready, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 출발 — 0070 그대로 + 크루 방은 참여 · 신발 스냅샷 · 공용 기여를 같은 트랜잭션으로(R01 · R05).
-- 준비하지 않은 사람 · 오늘 이미 한 사람 · 지금 크루원이 아닌 사람은 출발 명단에서 뺀다. 크루 방은 2명 이상이어야 출발한다
create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_starts timestamptz := now() + interval '4 seconds';
  v_party public.parties;
  v_day date;
  v_level int;
  v_n int;
  v_added numeric;
  v_pool economy.crew_day_pools;
begin
  select * into v_party from public.parties where id = p_party and host_id = auth.uid() and status = 'LOBBY' for update;
  if not found then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;

  if v_party.crew_id is not null then
    v_day := economy.game_day(v_starts);
    v_level := greatest(coalesce((select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
    -- 공용 풀을 먼저 잠근다(모든 크루 러닝 함수가 같은 차례) — 같은 크루의 다른 방이 동시에 출발해도 기여가 섞이지 않게
    perform economy.crew_pool_settle(v_party.crew_id, v_day, now());
    delete from public.party_members m
     where m.party_id = p_party
       and (exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day)
            or not exists (select 1 from public.crew_members cm where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id));
    select count(*)::int into v_n from public.party_members m where m.party_id = p_party;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    if not exists (select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()) then
      raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
    end if;
  end if;

  update public.parties p
     set status = 'COUNTDOWN', starts_at = v_starts,
         crew_level = case when p.crew_id is not null then v_level end
   where p.id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at, sneaker_id, shoe_dur_max)
  select p_party, m.user_id, v_starts, s.id, s.dur_max
    from public.party_members m
    left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
   where m.party_id = p_party
  on conflict do nothing;

  if v_party.crew_id is not null then
    -- 참여 · 신발 스냅샷 · 기여 = 최대 내구도 × (1 + 레벨 체력 보너스). 같은 날 다른 방에서 먼저 출발했으면(동시 출발) 이 방에서 뺀다
    with ins as (
      insert into economy.crew_runs (user_id, day, crew_id, party_id, started_at, state, last_beat_at,
                                     sneaker_id, shoe_dur_max, shoe_eff_bps, crew_level, contribution, level_pct)
      select m.user_id, v_day, v_party.crew_id, p_party, v_starts, 'RUNNING', v_starts,
             s.id, coalesce(s.dur_max, 0), coalesce(e.efficiency_bps, 0), v_level,
             round(coalesce(s.dur_max, 0) * (1 + economy.crew_level_stamina(v_level)), 4), economy.crew_level_reward(v_level)
        from public.party_members m
        left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
        left join lateral economy.shoe_effective(s) e on s.id is not null
       where m.party_id = p_party
      on conflict (user_id, day) do nothing
      returning user_id, contribution)
    select count(*)::int, coalesce(sum(ins.contribution), 0) into v_n, v_added from ins;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    delete from public.party_runs r
     where r.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = r.user_id and c.party_id = p_party);
    delete from public.party_members m
     where m.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.party_id = p_party);
    update economy.crew_day_pools d
       set capacity = d.capacity + v_added, contributors = d.contributors + v_n, version = d.version + 1,
           exhausted_at = case when d.capacity + v_added > d.consumed then null else d.exhausted_at end,
           low_at = case when d.capacity + v_added - d.consumed > (d.capacity + v_added) * 0.2 then null else d.low_at end
     where d.crew_id = v_party.crew_id and d.day = v_day
    returning * into v_pool;
    -- 새 기여로 다시 생겼으면 0 이던 구간을 닫는다(그 뒤로는 다시 적립 — D06)
    if v_pool.capacity > v_pool.consumed then
      update economy.crew_pool_empty x set to_at = now()
       where x.crew_id = v_party.crew_id and x.day = v_day and x.to_at is null;
    end if;
  end if;
end;
$$;

-- ── 달리는 동안 ──────────────────────────────────────────────────────

-- 달리는 동안 5초마다(그리고 일시정지 · 다시 달리기 · 끝내기 때 바로) — 내 상태를 알리고 공용 체력을 정산한다.
-- p_state: RUNNING · PAUSED · ENDED. 끝난 러닝은 다시 살리지 않는다. 크루 러닝이 아니면 {"crew_run": false}
create or replace function public.crew_run_beat(p_party bigint, p_state text)
returns jsonb
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  r economy.crew_runs;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_state not in ('RUNNING', 'PAUSED', 'ENDED') then
    raise exception 'bad_state' using errcode = '22023';
  end if;
  select * into r from economy.crew_runs c where c.user_id = v_user and c.party_id = p_party;
  if not found or r.crew_id is null then
    return jsonb_build_object('crew_run', false, 'server_now', now());
  end if;
  -- 지금까지를 예전 상태로 센 뒤 상태를 바꾼다
  perform economy.crew_pool_settle(r.crew_id, r.day, now());
  if r.state <> 'ENDED' then
    if p_state = 'ENDED' then
      perform economy.crew_run_close(v_user, r.day, 'USER_END');
    else
      update economy.crew_runs c set state = p_state, last_beat_at = now()
       where c.user_id = v_user and c.day = r.day and c.state <> 'ENDED';
    end if;
  end if;
  perform economy.crew_run_bonus_eval(v_user, r.day, false);
  return public.crew_run_live(p_party);
end;
$$;
revoke all on function public.crew_run_beat(bigint, text) from public, anon;
grant execute on function public.crew_run_beat(bigint, text) to authenticated;

-- 달리는 중 화면 한 덩이 — 내 상태 · 끝난 까닭 · 공용 체력 · 같은 방 · 크루 전체 달리는 사람 · 인원 보너스 · 레벨 보너스 · 적립 상태
create or replace function public.crew_run_live(p_party bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  r economy.crew_runs;
  v_pool jsonb;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into r from economy.crew_runs c where c.user_id = v_user and c.party_id = p_party;
  if not found or r.crew_id is null then
    return jsonb_build_object('crew_run', false, 'server_now', now());
  end if;
  v_pool := economy.crew_pool_json(r.crew_id, r.day);
  return jsonb_build_object(
    'crew_run', true,
    'server_now', now(),
    'party_id', p_party,
    'crew_id', r.crew_id,
    'day', r.day,
    'started_at', r.started_at,
    'state', r.state,
    'terminal_reason', r.terminal_reason,
    'contribution', r.contribution,
    'consumed', r.consumed,
    'active_sec', r.active_sec,
    'sneaker_id', r.sneaker_id,
    'pool', v_pool,
    'participants', (select count(*) from economy.crew_runs c where c.party_id = p_party),
    'not_ended', (select count(*) from economy.crew_runs c where c.party_id = p_party and c.state <> 'ENDED'),
    'room_active_runners', (select count(*) from economy.crew_runs c
                             where c.party_id = p_party and c.state = 'RUNNING'
                               and c.last_beat_at >= now() - interval '30 seconds'),
    'bonus', jsonb_build_object(
      'status', r.bonus_status, 'check_at', r.started_at + interval '2 minutes',
      'count', r.bonus_count, 'pct', r.bonus_pct, 'reason', r.bonus_reason,
      'verified', economy.crew_bonus_person(v_user)),
    'level', jsonb_build_object('level', r.crew_level, 'pct', r.level_pct),
    'earning_state', case
      when r.state = 'PAUSED' then 'PAUSED'
      when r.state = 'ENDED' then 'ENDED'
      when (v_pool->>'exhausted')::boolean then 'POOL_EMPTY'
      when r.bonus_status = 'PENDING' then 'PENDING'
      else 'ACCRUING' end);
end;
$$;
revoke all on function public.crew_run_live(bigint) from public, anon;
grant execute on function public.crew_run_live(bigint) to authenticated;

-- 경로에서 공용 체력이 0 이던 구간에 달린 거리(m)와 전체 거리 — 구간의 가운데 시각으로 가른다(튄 구간은 둘 다 빼고).
-- p_cut 이 있으면(서버가 이미 끝낸 참여) 그 뒤 거리를 late_m 으로 따로 세고 적립에서도 뺀다(empty_m 에 더한다)
create or replace function economy.track_meters_empty(p_track text, p_crew uuid, p_day date, p_cut timestamptz,
                                                      out total_m double precision, out empty_m double precision,
                                                      out late_m double precision)
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision; v_lng double precision; v_at bigint;
  v_plat double precision; v_plng double precision; v_pat bigint;
  v_have boolean := false;
  v_d double precision; v_dt double precision;
  v_mid timestamptz;
  v_ranges tstzrange[];
begin
  total_m := 0;
  empty_m := 0;
  late_m := 0;
  if p_track is null or p_track = '' then
    return;
  end if;
  select coalesce(array_agg(tstzrange(x.from_at, coalesce(x.to_at, 'infinity'::timestamptz))), '{}')
    into v_ranges
    from economy.crew_pool_empty x where x.crew_id = p_crew and x.day = p_day;
  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';
    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;
    continue when v_lat not between -90 and 90 or v_lng not between -180 and 180;
    if v_have then
      v_d := economy.haversine_m(v_plat, v_plng, v_lat, v_lng);
      v_dt := (v_at - v_pat) / 1000.0;
      if v_dt > 0 and v_d / v_dt * 3.6 <= economy.speed_glitch_kmh() then
        total_m := total_m + v_d;
        v_mid := to_timestamp((v_pat + v_at) / 2000.0);
        if p_cut is not null and v_mid >= p_cut then
          late_m := late_m + v_d;
          empty_m := empty_m + v_d;
        elsif exists (select 1 from unnest(v_ranges) rg where rg @> v_mid) then
          empty_m := empty_m + v_d;
        end if;
      end if;
    end if;
    v_plat := v_lat; v_plng := v_lng; v_pat := v_at; v_have := true;
  end loop;
end;
$$;
revoke all on function economy.track_meters_empty(text, uuid, date, timestamptz) from public;

-- 시간으로 — 러닝 시간 중 공용 체력이 0 이던 초(경로가 없을 때)
create or replace function economy.crew_empty_sec(p_crew uuid, p_day date, p_from timestamptz, p_to timestamptz)
returns numeric
language sql
stable
security definer
set search_path = public, economy
as $$
  select coalesce(sum(greatest(extract(epoch from (least(coalesce(x.to_at, p_to), p_to) - greatest(x.from_at, p_from))), 0)), 0)::numeric
    from economy.crew_pool_empty x
   where x.crew_id = p_crew and x.day = p_day
     and x.from_at < p_to and coalesce(x.to_at, 'infinity'::timestamptz) > p_from
$$;
revoke all on function economy.crew_empty_sec(uuid, date, timestamptz, timestamptz) from public;

-- ── 러닝 기록 ────────────────────────────────────────────────────────

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
  -- 크루 러닝 3판 (0070)
  v_party_starts timestamptz;
  v_crew_room boolean := false;
  v_crew_level int := 1;
  v_runners int := 0;
  v_pool_pts numeric := 0;
  v_pool_max numeric := 0;
  v_share numeric := 0;
  v_limit_sec int := 0;
  v_wall numeric;
  v_together_sec int := 0;
  v_crew_sec numeric := 0;
  v_crew_used numeric := 0;
  v_headcount numeric := 0;
  v_level_bonus numeric := 0;
  -- 크루 러닝 — 공용 체력 (0071)
  v_cr economy.crew_runs;
  v_is_cr boolean := false;
  v_base numeric := 0;
  v_head_sup numeric := 0;
  v_lvl_sup numeric := 0;
  v_tm double precision;
  v_em double precision;
  v_lm double precision;
  v_frac numeric := 1;
  -- 서버가 이미 끝낸 참여(마지막 한 사람 자동 종료 · 끝냄)면 그때 + 60초(폰 시계 차이) 뒤의 기록은 크루 보상 · 경험치에서 뺀다
  v_cut timestamptz;
  v_xp_frac numeric := 1;
  v_level_now int;
  v_level_was int;
  v_starters int := 1;
  v_xp_m numeric := 0;
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

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
  end if;

  -- 크루 러닝(0071) — 이 러닝의 방에서 서버가 출발을 확정한 참여(economy.crew_runs, party_start)가 있어야 한다.
  -- 앱이 크루 러닝이라고 말해서가 아니다. 참여가 이미 정산됐으면(같은 방 · 같은 날 두 번째 러닝) 보통 러닝이다
  if v_party_id is not null then
    select p.crew_id into v_crew from public.parties p where p.id = v_party_id;
    v_crew_room := v_crew is not null;
    v_crew := null;
    if v_crew_room then
      select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.party_id = v_party_id;
      if found and v_cr.session_id is null and v_cr.crew_id is not null then
        v_is_cr := true;
      elsif found then
        v_party_id := null;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '오늘 크루 러닝을 이미 했습니다');
      end if;
      -- 크루 방의 보너스는 크루 러닝 보너스 하나다 — 크루 러닝이 아니면 예전 함께 달리기 보너스도 없다(겹치지 않게)
      if not v_is_cr then
        v_party := 1;
      end if;
    end if;
  end if;
  if v_is_cr then
    v_pool := 'CREW';
    v_crew := v_cr.crew_id;
    v_crew_level := v_cr.crew_level;
    v_party_starts := v_cr.started_at;
    select count(*)::int into v_starters from economy.crew_runs c where c.party_id = v_cr.party_id;
    v_party := v_starters;
    if v_cr.state = 'ENDED' and v_cr.terminal_reason in ('USER_END', 'LAST_PARTICIPANT_END') and v_cr.ended_at is not null then
      v_cut := v_cr.ended_at + interval '60 seconds';
    end if;
    -- 공용 체력을 지금까지 정산하고(풀 → 사람 차례로 잠근다) 이 러닝을 끝낸다 — 다른 사람이 모두 끝났으면 마지막 사람도
    perform economy.crew_pool_settle(v_cr.crew_id, v_cr.day, now());
    perform economy.crew_run_close(v_user, v_cr.day, 'RECORDED');
    -- 인원 보너스 — 2분이 지났으면 지금 정하고, 아니면 확인할 수 없다(D01)
    perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
    select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
    v_headcount := v_cr.bonus_pct;
    v_level_bonus := v_cr.level_pct;
  end if;

  -- ── 하루 걸음 상한 (한국 하루) — 개인 러닝만, 크루 러닝 걸음은 세지 않는다 (0070) ──
  if v_rewardable > 0 and v_pool = 'PERSONAL' then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and s.energy_pool <> 'CREW'
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

  -- ── 신발 ──
  -- 크루 러닝은 출발 때 고른 신발(아직 내 것이면) — 효율 · 착화감은 그 신발 것 (0070)
  if v_pool = 'CREW' then
    -- 출발 때 고른 신발 — 기록에 적는 번호만. 효율은 출발 때 스냅샷(v_cr.shoe_eff_bps)으로 센다
    select x.* into v_shoe from public.market_sneakers x where x.id = v_cr.sneaker_id and x.owner_id = v_user;
  end if;
  if v_shoe.id is null then
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  if v_shoe.id is null then
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

  if v_pool = 'CREW' then
    -- 크루 러닝은 개인 신발 내구도 · 개인 에너지를 쓰지 않는다(R02 · R11). 효율은 출발 때 고른 신발의 스냅샷(R04).
    -- 공용 체력이 0 이던 구간에 달린 걸음은 적립하지 않는다(R03) — 경로를 시간으로 갈라 그 구간의 거리만큼, 경로가 없으면 시간만큼
    v_dur := 100;
    v_eff := v_cr.shoe_eff_bps;
    v_comfort := 0;
    select t.total_m, t.empty_m, t.late_m into v_tm, v_em, v_lm
      from economy.track_meters_empty(v_track, v_cr.crew_id, v_cr.day, v_cut) t;
    if v_tm > 0 then
      v_frac := greatest(1 - v_em / v_tm, 0)::numeric;
      v_xp_frac := greatest(1 - v_lm / v_tm, 0)::numeric;
    else
      -- 경로가 없으면 시간으로. 서버가 끝낸 뒤의 시간은 빼고, 0 이던 몫은 서버가 센 달린 초 중 0 이던 초로(일시정지와 상관없다)
      v_xp_frac := case when v_cut is null or v_cut >= p_ended_at then 1
                        else least(greatest(extract(epoch from (v_cut - p_started_at)), 0)
                                   / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 1)::numeric end;
      if v_cr.active_sec > 0 then
        v_frac := greatest(1 - v_cr.empty_sec / v_cr.active_sec, 0) * v_xp_frac;
      else
        v_frac := greatest(v_xp_frac - economy.crew_empty_sec(v_cr.crew_id, v_cr.day, p_started_at, least(p_ended_at, coalesce(v_cut, p_ended_at)))
                                       / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 0);
      end if;
    end if;
    if v_rewardable > 0 and v_frac < 1 then
      v_rewardable := floor(v_rewardable * v_frac)::int;
      if v_frac < v_xp_frac then
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 체력을 다 썼습니다');
      end if;
    end if;
    if v_xp_frac < 1 then
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 러닝이 끝난 뒤의 기록은 크루 보상에서 뺐습니다');
    end if;
  else
    -- 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 (0066) — 남은 내구도로 달릴 수 있던 시간(1분마다 5)만큼만.
    -- 적립 배율은 남은 비율이 0 보다 크면 그대로라(economy.durability_factor) 거의 닳은 신발로 오래 달려도 다 받던 것을 막는다
    if v_rewardable > 0 and v_shoe.id is not null
       and v_shoe.dur_now * economy.durability_sec_per_point() < v_elapsed then
      v_rewardable := floor(v_rewardable * v_shoe.dur_now * economy.durability_sec_per_point()
                            / greatest(v_elapsed, 1)::numeric)::int;
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
    end if;

    -- 개인 에너지 — 적립 걸음 600 에 1, 착화감만큼 덜
    if v_rewardable > 0 then
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
      v_energy_left := economy.energy_left(v_user, v_gday);
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
  end if;

  if economy.boost_active(v_user, 'XP_BOOSTER', p_started_at) then
    v_xp := 2;
  end if;

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    v_points := round(
      v_rewardable * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * economy.durability_factor(v_dur)
        * economy.party_multiplier(v_party)
        * v_xp,
      4
    );
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  if v_points > 0 and v_pool = 'PERSONAL' then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW' and economy.game_day(s.started_at) = v_gday;
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
  if v_points > 0 and not v_gps_backed and v_pool = 'PERSONAL' then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and s.energy_pool <> 'CREW'
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and v_pool = 'PERSONAL' then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW'
       and economy.game_day(s.created_at) = economy.game_day(now());
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

  -- 개인 에너지는 개인 러닝만 쓴다 — 크루 러닝은 공용 체력(crew_runs · crew_day_pools)
  if v_energy_used > 0 and v_pool = 'PERSONAL' then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 크루 레벨 — 경험치는 유효 크루 러닝의 사람마다 자기 거리(R09). 보류 · 무효 · 크루를 나간 뒤는 쌓지 않는다. 레벨은 내려가지 않고,
  -- 넘은 레벨마다 레벨 업을 한 번 적는다. 정산은 참여 한 줄에 한 번(같은 러닝을 다시 올리면 위에서 원래 결과를 돌려준다)
  if v_pool = 'CREW' then
    select coalesce(c.level, 1) into v_level_was from public.crews c where c.id = v_crew for update;
    v_level_now := v_level_was;
    -- 서버가 끝낸 뒤의 거리는 빼고(v_xp_frac)
    if v_verdict <> 'VOID' and not v_held and v_distance_m * v_xp_frac > 0
       and exists (select 1 from public.crew_members m where m.crew_id = v_crew and m.user_id = v_user) then
      update public.crews c
         set run_km = c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3),
             level = greatest(coalesce(c.level, 1),
                              economy.crew_level_for_km(c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3)))
       where c.id = v_crew
      returning c.level into v_level_now;
      v_xp_m := round((v_distance_m * v_xp_frac)::numeric, 1);
      insert into economy.crew_level_events (crew_id, level, session_id, run_km)
      select v_crew, l, v_session_id, (select run_km from public.crews where id = v_crew)
        from generate_series(v_level_was + 1, v_level_now) l
      on conflict (crew_id, level) do nothing;
    end if;
    update economy.crew_runs c
       set session_id = v_session_id, base_sup = case when v_points > 0 then v_base else 0 end,
           headcount_sup = case when v_points > 0 then v_head_sup else 0 end,
           level_sup = case when v_points > 0 then v_lvl_sup else 0 end, total_sup = v_points,
           xp_m = v_xp_m,
           level_before = v_level_was, level_after = v_level_now, settled_at = now()
     where c.user_id = v_user and c.day = v_cr.day;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064). 달린 시간은 멈춘 시간을 뺀 값(v_elapsed).
  -- 거리 · 걸음과 상관없다 — 켜 두고 움직이지 않아도 닳는다(0068, 2026-10-19 사용자 결정. 예전엔 거리 0 이면 닳지 않았다).
  -- 무효 러닝은 닳지 않는다. 남은 비율(durability_pts)은 트리거가 다시 센다.
  -- 크루 러닝은 크루 체력을 쓰고 신발은 닳지 않는다 — 거리만 쌓는다 (0070, 사용자 결정 "하루에 내 자유러닝 + 크루러닝")
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0) end,
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
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지·크루 러닝(크루 · 하루 공용 체력 · 출발 때 참여)·파티·부스터·상한 모두 서버 값으로.';


-- ── 앱이 읽는 것 ──────────────────────────────────────────────────────

-- 규칙 표 — 0070 그대로 + 인원 보너스에 휴대폰 인증이 필요한가
create or replace function economy.crew_run_rules()
returns jsonb
language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object(
    'room_max', economy.crew_room_max(),
    'near_m', economy.crew_near_m(),
    'confirm_sec', 120,
    'drain_per_min', 60 / economy.durability_sec_per_point(),
    'requires_phone', coalesce((economy.setting('crew_bonus_requires_phone') #>> '{}')::boolean, true),
    'headcount', (select jsonb_agg(jsonb_build_object('n', n, 'bonus', economy.crew_headcount_bonus(n)) order by n)
                    from generate_series(1, 20) n),
    'levels', (select jsonb_agg(jsonb_build_object('level', l, 'km', coalesce(economy.crew_level_km(l), 0),
                                                   'stamina', economy.crew_level_stamina(l),
                                                   'reward', economy.crew_level_reward(l)) order by l)
                 from generate_series(1, 20) l))
$$;
revoke all on function economy.crew_run_rules() from public;

-- 내 참여 한 줄(오늘) — 했는지 · 상태 · 어느 크루 · 끝난 까닭 · 다음 참여 가능 시각
create or replace function economy.crew_run_mine_json(p_user uuid, p_day date)
returns jsonb
language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object(
    'used_today', c.user_id is not null,
    'state', c.state,
    'crew_id', c.crew_id,
    'crew_name', (select cr.name from public.crews cr where cr.id = c.crew_id),
    'party_id', c.party_id,
    'session_id', c.session_id,
    'started_at', c.started_at,
    'ended_at', c.ended_at,
    'terminal_reason', c.terminal_reason,
    'contribution', c.contribution,
    'consumed', c.consumed,
    'distance_m', (select s.distance_meters from public.walk_sessions s where s.id = c.session_id),
    'total_sup', c.total_sup,
    'next_eligible_at', economy.game_day_start(p_day + 1))
    from (select 1) one
    left join economy.crew_runs c on c.user_id = p_user and c.day = p_day
$$;
revoke all on function economy.crew_run_mine_json(uuid, date) from public;

-- 크루 홈 · 입구 — 오늘 공용 체력(정산한 값) · 내 오늘 참여 · 크루 레벨 · 규칙. 크루원만
create or replace function public.crew_run_today(p_crew uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_level int;
  v_km numeric;
begin
  perform public.crew_home_member(p_crew);
  -- 오늘 풀이 있으면 지금까지 정산한다(없으면 0/0 — 새 하루, 행을 만들지 않는다)
  if exists (select 1 from economy.crew_day_pools d where d.crew_id = p_crew and d.day = v_day) then
    perform economy.crew_pool_settle(p_crew, v_day, now());
  end if;
  select c.level, c.run_km into v_level, v_km from public.crews c where c.id = p_crew;
  return jsonb_build_object(
    'server_now', now(),
    'day', v_day,
    'resets_at', economy.game_day_start(v_day + 1),
    'pool', economy.crew_pool_json(p_crew, v_day),
    'mine', economy.crew_run_mine_json(v_user, v_day),
    'verified', economy.phone_verified(v_user),
    'crew_level', v_level,
    'run_km', v_km,
    'next_level_km', economy.crew_level_km(coalesce(v_level, 1) + 1),
    'stamina_bonus', economy.crew_level_stamina(v_level),
    'reward_bonus', economy.crew_level_reward(v_level),
    'level_event', (select jsonb_build_object('level', e.level, 'reached_at', e.reached_at, 'run_km', e.run_km)
                      from economy.crew_level_events e where e.crew_id = p_crew order by e.level desc limit 1),
    'rules', economy.crew_run_rules());
end;
$$;
revoke all on function public.crew_run_today(uuid) from public, anon;
grant execute on function public.crew_run_today(uuid) to authenticated;

-- 대기실 — 사람마다 준비 · 크루원 · 인증(참 · 거짓만, 번호는 없다) · 오늘 했는지 · 고른 신발(지금 · 최대 내구도),
-- 출발 전 미리보기(준비한 크루원 중 인증된 사람으로 센 인원 보너스 · 레벨 보너스 · 내 기여), 오늘 공용 체력. 방 사람만
create or replace function public.crew_run_room(p_party bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_party public.parties;
  v_level int;
  v_day date := economy.game_day(now());
  v_members jsonb;
  v_ready int;
  v_eligible int;
  v_mine numeric;
  v_all int;
begin
  perform public.party_assert_member(p_party);
  select * into v_party from public.parties where id = p_party;
  v_level := greatest(coalesce(v_party.crew_level, (select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
  with x as (
    select m.user_id, m.joined_at, m.ready, pr.display_name,
           coalesce(r.sneaker_id, s.id) as sneaker_id,
           case when r.user_id is not null then r.shoe_dur_max else s.dur_max end as dur_max,
           sh.dur_now, sh.rarity, sh.level as shoe_level, sh.variant, sh.model_id,
           v_party.crew_id is not null and exists (select 1 from public.crew_members cm
                                                    where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id) as crew_member,
           exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day and c.party_id <> p_party) as ran_today,
           economy.crew_bonus_person(m.user_id) as verified
      from public.party_members m
      left join public.profiles pr on pr.id = m.user_id
      left join public.party_runs r on r.party_id = m.party_id and r.user_id = m.user_id
      left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on r.user_id is null
      left join public.market_sneakers sh on sh.id = coalesce(r.sneaker_id, s.id)
     where m.party_id = p_party)
  select coalesce(jsonb_agg(jsonb_build_object(
           'user_id', x.user_id, 'name', coalesce(x.display_name, '러너'), 'is_me', x.user_id = v_user,
           'is_host', x.user_id = v_party.host_id, 'ready', x.ready, 'crew_member', x.crew_member,
           'ran_today', x.ran_today, 'verified', x.verified,
           'sneaker_id', x.sneaker_id, 'rarity', x.rarity, 'shoe_level', x.shoe_level, 'variant', x.variant,
           'model_id', x.model_id, 'dur_max', coalesce(x.dur_max, 0), 'dur_now', x.dur_now)
           order by (x.user_id = v_party.host_id) desc, x.joined_at), '[]'::jsonb),
         count(*) filter (where x.ready and x.crew_member and not x.ran_today),
         count(*) filter (where x.ready and x.crew_member and not x.ran_today and x.verified),
         count(*),
         max(case when x.user_id = v_user then coalesce(x.dur_max, 0) end)
    into v_members, v_ready, v_eligible, v_all, v_mine
    from x;
  return jsonb_build_object(
    'server_now', now(),
    'party_id', p_party,
    'crew_id', v_party.crew_id,
    'status', v_party.status,
    'starts_at', v_party.starts_at,
    'capacity', economy.crew_room_max(),
    'crew_level', v_level,
    'stamina_bonus', economy.crew_level_stamina(v_level),
    'reward_bonus', economy.crew_level_reward(v_level),
    'members_count', v_all,
    'ready_count', v_ready,
    'bonus_preview', jsonb_build_object('count', v_eligible, 'pct', economy.crew_headcount_bonus(v_eligible),
                                        'next_pct', economy.crew_headcount_bonus(v_eligible + 1)),
    'my_contribution', round(coalesce(v_mine, 0) * (1 + economy.crew_level_stamina(v_level)), 4),
    'my_verified', economy.crew_bonus_person(v_user),
    'pool', case when v_party.crew_id is not null then economy.crew_pool_json(v_party.crew_id, v_day) end,
    'members', v_members,
    'rules', economy.crew_run_rules());
end;
$$;
revoke all on function public.crew_run_room(bigint) from public, anon;
grant execute on function public.crew_run_room(bigint) to authenticated;

-- 결과 — 내 러닝(시작 시각)이 크루 러닝이었으면 정산 한 줄(기본 · 인원 · 레벨 · 합), 인원 보너스 판정, 공용 체력 사용,
-- 크루 경험치 · 레벨 전후, 끝난 까닭. 크루 러닝이 아니었으면 null. 아직 올리지 않았으면 settled=false
create or replace function public.crew_run_result(p_started_at timestamptz)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_s public.walk_sessions;
  c economy.crew_runs;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into v_s from public.walk_sessions s where s.user_id = v_user and s.started_at = p_started_at;
  if not found or v_s.energy_pool <> 'CREW' then
    return null;
  end if;
  select * into c from economy.crew_runs r where r.user_id = v_user and r.session_id = v_s.id;
  if not found then
    return null;
  end if;
  return jsonb_build_object(
    'settled', c.settled_at is not null,
    'day', c.day,
    'crew_id', c.crew_id,
    'crew_name', (select cr.name from public.crews cr where cr.id = c.crew_id),
    'session_id', c.session_id,
    'verdict', v_s.verdict,
    'reason', v_s.verdict_reason,
    'terminal_reason', c.terminal_reason,
    'participants', (select count(*) from economy.crew_runs r where r.party_id = c.party_id),
    'reward', jsonb_build_object('base', c.base_sup, 'headcount_pct', c.bonus_pct, 'headcount', c.headcount_sup,
                                 'level_pct', c.level_pct, 'level', c.level_sup, 'total', c.total_sup),
    'bonus', jsonb_build_object('status', c.bonus_status, 'count', c.bonus_count, 'pct', c.bonus_pct, 'reason', c.bonus_reason),
    'pool_usage', jsonb_build_object('contribution', c.contribution, 'consumed', c.consumed, 'active_sec', c.active_sec,
                                     'pool', economy.crew_pool_json(c.crew_id, c.day), 'personal_durability_delta', 0),
    'xp', jsonb_build_object('distance_m', c.xp_m, 'applied', coalesce(c.xp_m, 0) > 0,
                             'level_before', c.level_before, 'level_after', c.level_after,
                             'run_km', (select cr.run_km from public.crews cr where cr.id = c.crew_id),
                             'next_level_km', (select economy.crew_level_km(coalesce(cr.level, 1) + 1)
                                                 from public.crews cr where cr.id = c.crew_id)),
    'next_eligible_at', economy.game_day_start(c.day + 1));
end;
$$;
revoke all on function public.crew_run_result(timestamptz) from public, anon;
grant execute on function public.crew_run_result(timestamptz) to authenticated;
