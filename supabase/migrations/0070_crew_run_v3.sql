-- ════════════════════════════════════════════════════════════════════
--  0070 — 크루 러닝 3판: 함께 쓰는 크루 체력 · 하루 한 번 · 500m · 인원 보너스 · 크루 레벨 1 ~ 20
--         (2026-10-21 사용자 결정)
--
--  사용자 결정: "크루 시작할 때 신발의 모든 능력치를 합산하고 n빵" · "크루 자체의 체력도 따로 — 그러니까 하루에 내 자유러닝 +
--  크루러닝이 가능" · "크루 내구도는 00:00(UTC+9) · 수리는 안 해도 됨" · "시작 전에 신발을 골라 그 신발만" ·
--  보상은 "6명 이상 +50% 20명 이상 +150% · 2~5명은 작은 보너스 · 7~19명은 명당 +2%" · "겹치지 않게" · "최대 20명" ·
--  크루 보상 하루 상한은 "일단 무제한" · 인원은 "500m 안에서 크루 들어와서 달리기에서 스타트한 사람 · 크루원 수" ·
--  크루 체력 0 이면 "적립만 멈추고 기록은 계속"(5번 추천) · 크루 레벨은 20까지, 레벨당 체력 +6% · 보상 +1%.
--  0069 의 "개인 에너지를 크루 체력의 내 몫으로" 규칙은 이것이 대신한다(0069 표 · crew_stamina 는 예전 앱을 위해 남긴다).
--
--  1. 크루 러닝 = 크루 방에서 출발해 출발 2분 뒤에도 서로 500m 안(party_near — 크루 방은 500m, 번개는 그대로 200m)으로
--     이어진 무리 중 지금 크루원이 2명 이상(나 포함). 한 사람은 하루(한국 시각, 러닝을 시작한 날)에 한 번만 —
--     그날 두 번째로 올린 크루 러닝은 보통 러닝(개인 에너지 · 개인 신발, 인원 보너스 없음)이다. 일시정지는 괜찮고,
--     끝내면(올리면) 그날 크루 러닝은 끝난다. 무리에서 혼자 남으면(곁 기록이 끊긴 뒤 60초) 그 뒤는 적립하지 않는다.
--  2. 크루 체력(그 무리) = 무리의 크루원이 출발 때 고른 신발의 최대 내구도 합 × (1 + 6% × (크루 레벨 − 1)).
--     한 사람이 달린 1분마다 5 씩 쓴다(신발과 같은 빠르기). 몫은 똑같이 — 한 사람이 쓸 수 있는 시간 =
--     크루 체력 ÷ 인원 × 12초. 다 쓰면 그 뒤 걸음은 적립하지 않는다(기록은 그대로). 수리 없음 — 다음 날 다시.
--     크루 러닝은 개인 에너지 · 개인 신발 내구도를 쓰지 않는다(신발에 거리만 쌓는다).
--  3. 보상 = 적립 걸음 × 걸음 값 × (1 + 고른 신발의 효율 — 평균 내지 않고 내 신발) × (1 + 인원 보너스 + 레벨 보너스) × 부스터.
--     인원 보너스(크루원 수): 2명 5% · 3명 10% · 4명 15% · 5명 20% · 6명 50% · 7 ~ 19명 50% + 6명 넘는 한 명마다 2% · 20명 150%.
--     레벨 보너스: 레벨마다 1%(레벨 20 이면 19%) — 인원 보너스에 더한다(곱하지 않는다).
--     하루 적립 상한 · 경로 없는 러닝 상한 · 올린 날 상한 · 하루 걸음 상한은 크루 러닝에 걸지 않고, 개인 상한의 합에서도 뺀다.
--  4. 크루 레벨 1 ~ 20 = 크루 러닝 누적 거리 — 20 · 50 · 100 · 180 · 300 · 450 · 650 · 900 · 1,200 · 1,600 · 2,100 · 2,700 ·
--     3,400 · 4,200 · 5,100 · 6,100 · 7,200 · 8,500 · 10,000km 에서 레벨 2 ~ 20. 크루 체력 · 레벨 보너스는 출발 때의 레벨로.
--  5. 크루 방은 20명까지 — 꽉 찬 대기실에 오는 사람은 새 방을 연다. 크루원이면 누구나 방을 연다(그대로).
--  6. 신발 고르기: 대기실에서 party_pick_shoe — 고르지 않으면 신은 신발. 출발 때(party_start) 신발과 최대 내구도를 적어 둔다.
--  7. 앱이 읽는 것: crew_run_today(크루 홈 · 대기실) · crew_run_room(대기실 · 달리는 중) · crew_run_result(결과) · crew_level(20단계).
-- ════════════════════════════════════════════════════════════════════

-- ── 크루 레벨 1 ~ 20 · 보너스 ─────────────────────────────────────────

-- 그 레벨이 되는 크루 러닝 누적 거리(km). 레벨 1 이하 · 20 넘으면 null
create or replace function economy.crew_level_km(p_level int) returns numeric
language sql immutable set search_path = '' as $$
  select case p_level
    when 2 then 20 when 3 then 50 when 4 then 100 when 5 then 180 when 6 then 300
    when 7 then 450 when 8 then 650 when 9 then 900 when 10 then 1200 when 11 then 1600
    when 12 then 2100 when 13 then 2700 when 14 then 3400 when 15 then 4200 when 16 then 5100
    when 17 then 6100 when 18 then 7200 when 19 then 8500 when 20 then 10000
  end::numeric
$$;

create or replace function economy.crew_level_for_km(p_km numeric) returns int
language sql immutable set search_path = '' as $$
  select coalesce(max(l), 1)::int from generate_series(2, 20) l where coalesce(p_km, 0) >= economy.crew_level_km(l)
$$;

-- 크루 체력 보너스(비율) — 레벨마다 +6%, 더하기(레벨 20 이면 +114%)
create or replace function economy.crew_level_stamina(p_level int) returns numeric
language sql immutable set search_path = '' as $$
  select 0.06 * (least(greatest(coalesce(p_level, 1), 1), 20) - 1)
$$;

-- 보상 보너스(비율) — 레벨마다 +1%(레벨 20 이면 +19%). 인원 보너스에 더한다
create or replace function economy.crew_level_reward(p_level int) returns numeric
language sql immutable set search_path = '' as $$
  select 0.01 * (least(greatest(coalesce(p_level, 1), 1), 20) - 1)
$$;

-- 인원 보너스(비율) — 함께 달린 크루원 수
create or replace function economy.crew_headcount_bonus(p_n int) returns numeric
language sql immutable set search_path = '' as $$
  select case
    when coalesce(p_n, 1) >= 20 then 1.50
    when p_n >= 7 then 0.50 + 0.02 * (p_n - 6)
    when p_n = 6 then 0.50
    when p_n >= 2 then 0.05 * (p_n - 1)
    else 0
  end::numeric
$$;

-- 크루 방 인원 · 곁 거리(m) · 혼자 남은 뒤 봐주는 초 · 한 사람이 1분에 쓰는 크루 체력
create or replace function economy.crew_room_max() returns int
language sql immutable set search_path = '' as $$ select 20 $$;
create or replace function economy.crew_near_m() returns int
language sql immutable set search_path = '' as $$ select 500 $$;
create or replace function economy.crew_alone_grace_sec() returns int
language sql immutable set search_path = '' as $$ select 60 $$;

revoke all on function economy.crew_level_stamina(int) from public;
revoke all on function economy.crew_level_reward(int) from public;
revoke all on function economy.crew_headcount_bonus(int) from public;
revoke all on function economy.crew_room_max() from public;
revoke all on function economy.crew_near_m() from public;
revoke all on function economy.crew_alone_grace_sec() from public;

comment on column public.crews.level is
  '크루 레벨 1 ~ 20 — 크루 러닝 누적 거리(run_km)로 record_session 이 올린다. 크루 러닝이 아직 없으면 비어 있다 — "새 크루" (0070)';

-- 새 기준(20km 부터)으로 지금 레벨을 다시 맞춘다 — 내려가지 않는다
update public.crews c set level = greatest(coalesce(c.level, 1), economy.crew_level_for_km(c.run_km))
 where c.run_km > 0 and c.level is distinct from greatest(coalesce(c.level, 1), economy.crew_level_for_km(c.run_km));

-- ── 방 · 출발 때 적어 두는 것 ─────────────────────────────────────────

-- 대기실에서 고른 신발(비어 있으면 신은 신발)
alter table public.party_members add column if not exists sneaker_id bigint references public.market_sneakers on delete set null;
-- 출발 때의 신발과 그 최대 내구도 — 크루 체력은 이것으로 센다
alter table public.party_runs add column if not exists sneaker_id bigint;
alter table public.party_runs add column if not exists shoe_dur_max int;
-- 출발 때의 크루 레벨 — 크루 체력 · 레벨 보너스는 이것으로
alter table public.parties add column if not exists crew_level int;

comment on column public.party_runs.shoe_dur_max is '출발 때 고른 신발의 최대 내구도 — 크루 체력(무리의 합)으로 센다 (0070)';
comment on column public.parties.crew_level is '출발 때의 크루 레벨 — 크루 체력 · 레벨 보너스 (0070)';

-- 대기실에서 크루 러닝에 신을 신발을 고른다 — 내 신발 · 앱에 있는 · 팔지 않는 것. null 이면 신은 신발로
create or replace function public.party_pick_shoe(p_party bigint, p_sneaker bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
begin
  perform public.party_assert_member(p_party);
  if not exists (select 1 from public.parties where id = p_party and status = 'LOBBY') then
    raise exception '이미 출발한 파티입니다' using errcode = '23514';
  end if;
  if p_sneaker is not null then
    perform economy.my_app_sneaker(v_user, p_sneaker);
  end if;
  update public.party_members set sneaker_id = p_sneaker, last_seen = now()
   where party_id = p_party and user_id = v_user;
end;
$$;
revoke all on function public.party_pick_shoe(bigint, bigint) from public, anon;
grant execute on function public.party_pick_shoe(bigint, bigint) to authenticated;

-- 이 사람이 크루 러닝에 신을 신발 — 고른 신발이 아직 내 것(앱 · 팔지 않음)이면 그것, 아니면 신은 신발
create or replace function economy.party_shoe(p_user uuid, p_pick bigint)
returns setof public.market_sneakers
language sql stable security definer set search_path = public as $$
  select x.* from public.market_sneakers x
   where x.owner_id = p_user and x.chain_state = 'APP' and x.status = 'OWNED'
     and (x.id = p_pick or x.equipped)
   order by (x.id is not distinct from p_pick) desc, x.id
   limit 1
$$;
revoke all on function economy.party_shoe(uuid, bigint) from public;

-- ── 하루 한 번 · 크루 러닝 기록 ──────────────────────────────────────

create table if not exists economy.crew_run_days (
  user_id uuid not null references auth.users on delete cascade,
  -- 러닝을 시작한 날(한국 시각)
  day date not null,
  crew_id uuid references public.crews on delete set null,
  party_id bigint,
  session_id bigint,
  -- 함께 달린 크루원 수(나 포함) · 출발 때 크루 레벨
  runners int not null,
  crew_level int not null,
  -- 무리의 크루 체력(고른 신발 최대 내구도 합 × 레벨 보너스) · 한 사람 몫 · 몫으로 달릴 수 있는 초
  pool numeric(12, 2) not null,
  share numeric(12, 2) not null,
  limit_sec int not null,
  -- 내가 쓴 크루 체력(달린 1분마다 5, 몫을 넘지 않는다)
  used numeric(12, 2) not null default 0,
  -- 무리와 함께였던 초(혼자 남은 뒤 60초까지) — 러닝 시간보다 짧으면 "혼자 남아 끝남"
  together_sec int not null,
  headcount_bonus numeric(6, 4) not null,
  level_bonus numeric(6, 4) not null,
  sneaker_id bigint,
  created_at timestamptz not null default now(),
  primary key (user_id, day)
);
create index if not exists crew_run_days_crew on economy.crew_run_days (crew_id, day);
comment on table economy.crew_run_days is '크루 러닝 — 한 사람 하루(한국 시각) 한 번. record_session 이 적는다 (0070)';
alter table economy.crew_run_days enable row level security;
revoke all on economy.crew_run_days from public, anon, authenticated;

-- 이 방에서 출발 2분 뒤에도 곁이었던 사람들로 이어진 내 무리(나 포함) — economy.party_for 와 같은 무리
create or replace function economy.party_group(p_party bigint, p_user uuid, p_starts timestamptz)
returns setof uuid
language sql stable security definer set search_path = public as $$
  with recursive e as (
    select n.user_a, n.user_b from public.party_near n
     where n.party_id = p_party and n.last_at >= p_starts + interval '2 minutes'),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select u from g
$$;
revoke all on function economy.party_group(bigint, uuid, timestamptz) from public;

-- ── 대기실 · 출발 · 위치 보고 ─────────────────────────────────────────

-- 준비 — 오늘 크루 러닝을 이미 한 사람은 크루 방에서 준비할 수 없다(하루 한 번)
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
     and exists (select 1 from economy.crew_run_days d
                  where d.user_id = auth.uid() and d.day = economy.game_day(now())) then
    raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
  end if;
  update public.party_members set ready = p_ready, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 출발 — 0024 그대로 + 사람마다 고른 신발 · 최대 내구도, 크루 방은 크루 레벨을 적어 둔다
create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
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
  update public.parties p
     set status = 'COUNTDOWN', starts_at = v_starts,
         crew_level = case when p.crew_id is not null
                           then (select greatest(coalesce(c.level, 1), 1) from public.crews c where c.id = p.crew_id) end
   where p.id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at, sneaker_id, shoe_dur_max)
  select p_party, m.user_id, v_starts, s.id, s.dur_max
    from public.party_members m
    left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
   where m.party_id = p_party
  on conflict do nothing;
end;
$$;

-- 달리는 동안의 위치 보고 — 0056 그대로, 곁 거리는 크루 방 500m · 번개 200m
create or replace function public.party_ping(p_party bigint, p_lat double precision, p_lng double precision)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_near int;
begin
  perform public.party_assert_member(p_party);
  if p_lat is not null and (p_lat not between -90 and 90 or p_lng is null or p_lng not between -180 and 180) then
    raise exception '위치가 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 위치 없이 오면(GPS 를 잃었다) 남은 옛 점을 지운다 — 다른 사람에게 지금 위치처럼 보이지 않게
  update public.party_members
     set lat = p_lat, lng = case when p_lat is null then null else p_lng end, last_seen = now(),
         pinged_at = case when p_lat is not null then now() else pinged_at end
   where party_id = p_party and user_id = v_me;
  if p_lat is null then
    return;
  end if;
  -- 위치가 있는 보고만 "뛰는 중"으로 친다(출발한 사람만)
  update public.party_runs
     set last_ping = now()
   where party_id = p_party and user_id = v_me and starts_at <= now();
  if not found then
    return;
  end if;
  select case when p.crew_id is not null then economy.crew_near_m() else 200 end into v_near
    from public.parties p where p.id = p_party;
  -- 30초 안에 위치를 보낸, 같이 출발한 다른 사람이 곁(크루 500m · 번개 200m)에 있으면 적는다
  insert into public.party_near (party_id, user_a, user_b)
  select p_party, least(v_me, o.user_id), greatest(v_me, o.user_id)
    from public.party_members o
    join public.party_runs r
      on r.party_id = o.party_id and r.user_id = o.user_id and r.starts_at <= now()
   where o.party_id = p_party and o.user_id <> v_me
     and o.pinged_at > now() - interval '30 seconds'
     and o.lat is not null and o.lng is not null
     and economy.haversine_m(p_lat, p_lng, o.lat, o.lng) <= v_near
  on conflict (party_id, user_a, user_b) do update set last_at = now();
end;
$$;

-- 로비 열기 — 0056 그대로 + 이미 들어가 있는 방을 먼저 · 크루 대기실은 20명까지(꽉 차면 새 방)
create or replace function public.party_open(p_crew uuid, p_post bigint)
returns bigint
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_party bigint;
  v_status text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if (p_crew is null) = (p_post is null) then
    raise exception '크루 또는 번개 하나를 골라야 합니다' using errcode = '22023';
  end if;
  if not public.party_can_enter(p_crew, p_post) then
    raise exception '크루원이나 번개 참가자만 들어갈 수 있습니다' using errcode = '42501';
  end if;

  -- 아무도 안 보는 방과 오래된 방은 닫는다. 닫는 방에 남은 위치 · 거리는 지운다.
  with closed as (
    update public.parties p set status = 'FINISHED'
     where p.status <> 'FINISHED'
       and (p.created_at < now() - interval '6 hours'
            or not exists (
              select 1 from public.party_members m
               where m.party_id = p.id and m.last_seen > now() - interval '10 minutes'
            ))
    returning p.id)
  update public.party_members m set lat = null, lng = null, km = null
    from closed
   where m.party_id = closed.id;

  -- 이 크루(번개)의 열린 방. 두 사람이 동시에 열어도 방이 둘 생기지 않게 잠근다.
  perform pg_advisory_xact_lock(hashtext(coalesce(p_crew::text, 'post:' || p_post)));

  -- 이미 들어가 있는 방이 먼저 — 꽉 차 새 방이 열린 뒤에도 앞 방 사람은 제 방으로 돌아간다
  select p.id, p.status into v_party, v_status
    from public.parties p
   where p.status <> 'FINISHED'
     and ((p_crew is not null and p.crew_id = p_crew)
          or (p_post is not null and p.flash_post_id = p_post))
   order by exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = v_user) desc,
            p.created_at desc
   limit 1;

  -- 이미 출발한 방에 늦게 온 사람은 끼워 넣지 않는다. 대신 새 방을 연다.
  if v_party is not null and v_status <> 'LOBBY' and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) then
    v_party := null;
  end if;

  -- 크루 대기실은 20명까지 — 꽉 찬 방에 새로 오는 사람은 새 방을 연다
  if v_party is not null and p_crew is not null and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) and (select count(*) from public.party_members m where m.party_id = v_party) >= economy.crew_room_max() then
    v_party := null;
  end if;

  -- 진행자가 내보낸 방에는 다시 들어가지 않는다
  if v_party is not null and exists (
    select 1 from public.party_kicks k where k.party_id = v_party and k.user_id = v_user
  ) then
    raise exception '진행자가 내보낸 방입니다' using errcode = '42501';
  end if;

  if v_party is null then
    insert into public.parties (crew_id, flash_post_id, host_id)
    values (p_crew, p_post, v_user)
    returning id into v_party;
  end if;

  insert into public.party_members (party_id, user_id)
  values (v_party, v_user)
  on conflict (party_id, user_id) do update set last_seen = now();

  return v_party;
end;
$$;

-- ── 러닝 기록 ────────────────────────────────────────────────────────

-- 0069 그대로 + 크루 러닝은 함께 쓰는 크루 체력(시간) · 내 신발 · 인원 · 레벨 보너스 · 하루 한 번 · 개인 상한 밖 (0070)
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

  -- 크루 러닝 — 크루 방에서 출발해 출발 2분 뒤에도 곁(500m)으로 이어진 무리에 지금 크루원이 2명 이상(나 포함)이고,
  -- 내가 그 크루원이다. 무효 러닝 · 번개 방 · 크루를 나간 뒤 올린 러닝은 크루 러닝이 아니다
  if v_party_id is not null and v_party >= 2 and v_verdict <> 'VOID' then
    select p.crew_id, p.starts_at, greatest(coalesce(p.crew_level, c.level, 1), 1)
      into v_crew, v_party_starts, v_crew_level
      from public.parties p left join public.crews c on c.id = p.crew_id
     where p.id = v_party_id;
    v_crew_room := v_crew is not null;
    if v_crew is not null and not exists (select 1 from public.crew_members m
                                           where m.crew_id = v_crew and m.user_id = v_user) then
      v_crew := null;
    end if;
    -- 하루 한 번 — 그날(러닝을 시작한 날) 이미 크루 러닝을 올렸으면 보통 러닝이다
    if v_crew is not null and exists (select 1 from economy.crew_run_days d
                                       where d.user_id = v_user and d.day = v_gday) then
      v_crew := null;
      v_party_id := null;
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '오늘 크루 러닝을 이미 했습니다');
    end if;
    if v_crew is not null then
      -- 무리의 크루원 수와 그들이 출발 때 고른 신발의 최대 내구도 합 — 그날 다른 방에서 크루 러닝을 이미 한 사람은 빼고
      select count(*)::int, coalesce(sum(coalesce(r.shoe_dur_max, 0)), 0)
        into v_runners, v_pool_pts
        from economy.party_group(v_party_id, v_user, v_party_starts) g(u)
        join public.crew_members m on m.crew_id = v_crew and m.user_id = g.u
        left join public.party_runs r on r.party_id = v_party_id and r.user_id = g.u
       where not exists (select 1 from economy.crew_run_days d
                          where d.user_id = g.u and d.day = v_gday and d.party_id is distinct from v_party_id);
      if v_runners < 2 then
        v_crew := null;
      end if;
    end if;
    -- 크루 방의 보너스는 크루 러닝 보너스 하나다 — 크루 러닝이 아니면 예전 함께 달리기 보너스도 없다(겹치지 않게)
    if v_crew_room and v_crew is null then
      v_party := 1;
    end if;
  end if;
  if v_crew is not null then
    v_pool := 'CREW';
    v_party := v_runners;
    v_pool_max := round(v_pool_pts * (1 + economy.crew_level_stamina(v_crew_level)), 2);
    v_share := round(v_pool_max / v_runners, 2);
    v_limit_sec := floor(v_share * economy.durability_sec_per_point())::int;
    v_headcount := economy.crew_headcount_bonus(v_runners);
    v_level_bonus := economy.crew_level_reward(v_crew_level);
    -- 무리와 함께였던 때 — 나와 무리의 곁 기록이 마지막으로 적힌 때 + 60초. 그 뒤는 혼자 남은 것이다
    v_wall := greatest(extract(epoch from (p_ended_at - p_started_at)), 1);
    select least(greatest(extract(epoch from (max(n.last_at) - p_started_at)) + economy.crew_alone_grace_sec(), 0), v_wall)::int
      into v_together_sec
      from public.party_near n
     where n.party_id = v_party_id and (n.user_a = v_user or n.user_b = v_user)
       and n.last_at >= v_party_starts + interval '2 minutes';
    v_together_sec := coalesce(v_together_sec, 0);
    -- 크루 체력으로 달린 초 = 함께였던 동안의 러닝 시간(멈춘 시간 뺀 비율대로), 몫을 넘지 않게
    v_crew_sec := least(v_elapsed * v_together_sec / v_wall, v_limit_sec);
    v_crew_used := round(v_crew_sec / economy.durability_sec_per_point(), 2);
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
    select x.* into v_shoe
      from public.party_runs r join public.market_sneakers x on x.id = r.sneaker_id
     where r.party_id = v_party_id and r.user_id = v_user
       and x.owner_id = v_user and x.chain_state = 'APP' and x.status = 'OWNED';
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
    -- 크루 러닝은 개인 신발 내구도 · 개인 에너지를 쓰지 않는다. 크루 체력 몫으로 달린 초 · 무리와 함께였던 동안만 적립한다
    -- (사용자 결정 "크루 체력 0 이면 적립만 멈추고 기록은 계속" · "혼자 남으면 크루 러닝 종료")
    v_dur := 100;
    if v_rewardable > 0 and v_crew_sec < v_elapsed then
      v_rewardable := floor(v_rewardable * v_crew_sec / greatest(v_elapsed, 1)::numeric)::int;
      if v_elapsed * v_together_sec / v_wall > v_limit_sec then
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 체력을 다 썼습니다');
      end if;
      if v_together_sec < v_wall then
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '혼자 남아 크루 러닝이 끝났습니다');
      end if;
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

  v_points := round(
    v_rewardable * economy.points_per_step()
      * (1 + v_eff / 10000.0)
      * economy.durability_factor(v_dur)
      -- 크루 러닝은 인원 보너스 + 레벨 보너스(더하기 — 겹쳐 곱하지 않는다), 나머지는 예전 함께 달리기 보너스
      * case when v_pool = 'CREW' then 1 + v_headcount + v_level_bonus else economy.party_multiplier(v_party) end
      * v_xp,
    4
  );
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

  -- 개인 에너지는 개인 러닝만 쓴다 — 크루 러닝은 크루 체력(아래 crew_run_days)
  if v_energy_used > 0 and v_pool = 'PERSONAL' then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 오늘의 크루 러닝 — 하루 한 번(위에서 이미 봤고 사람마다 잠가 두어 겹치지 않는다) (0070)
  if v_pool = 'CREW' then
    insert into economy.crew_run_days (user_id, day, crew_id, party_id, session_id, runners, crew_level,
                                       pool, share, limit_sec, used, together_sec, headcount_bonus, level_bonus, sneaker_id)
    values (v_user, v_gday, v_crew, v_party_id, v_session_id, v_runners, v_crew_level,
            v_pool_max, v_share, v_limit_sec, v_crew_used, v_together_sec, v_headcount, v_level_bonus, v_shoe.id)
    on conflict (user_id, day) do nothing;
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
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지·크루 러닝(함께 쓰는 크루 체력 · 하루 한 번)·파티·부스터·상한 모두 서버 값으로.';

-- ── 앱이 읽는 것 ──────────────────────────────────────────────────────

-- 크루 러닝 규칙 표 — 인원 보너스(1 ~ 20명) · 레벨(1 ~ 20). 앱은 이 값을 그대로 보인다
create or replace function economy.crew_run_rules()
returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object(
    'room_max', economy.crew_room_max(),
    'near_m', economy.crew_near_m(),
    'confirm_sec', 120,
    'alone_grace_sec', economy.crew_alone_grace_sec(),
    -- 한 사람이 달린 1분마다 쓰는 크루 체력
    'drain_per_min', 60 / economy.durability_sec_per_point(),
    'headcount', (select jsonb_agg(jsonb_build_object('n', n, 'bonus', economy.crew_headcount_bonus(n)) order by n)
                    from generate_series(1, 20) n),
    'levels', (select jsonb_agg(jsonb_build_object('level', l, 'km', coalesce(economy.crew_level_km(l), 0),
                                                   'stamina', economy.crew_level_stamina(l),
                                                   'reward', economy.crew_level_reward(l)) order by l)
                 from generate_series(1, 20) l))
$$;
revoke all on function economy.crew_run_rules() from public;

-- 내 오늘 크루 러닝 한 줄(없으면 null)
create or replace function economy.crew_run_row_json(d economy.crew_run_days)
returns jsonb
language sql stable set search_path = '' as $$
  select case when d.user_id is null then null else jsonb_build_object(
    'day', d.day, 'crew_id', d.crew_id, 'session_id', d.session_id, 'runners', d.runners, 'crew_level', d.crew_level,
    'pool', d.pool, 'share', d.share, 'limit_sec', d.limit_sec, 'used', d.used, 'together_sec', d.together_sec,
    'headcount_bonus', d.headcount_bonus, 'level_bonus', d.level_bonus, 'sneaker_id', d.sneaker_id) end
$$;
revoke all on function economy.crew_run_row_json(economy.crew_run_days) from public;

-- 오늘(한국 시각) 이 크루의 크루 러닝 — 크루원만. 크루 홈 · 대기실 · 레벨 화면이 읽는다
create or replace function public.crew_run_today(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_me economy.crew_run_days;
  v_level int;
  v_km numeric;
begin
  -- 크루 홈과 같은 문 — 지금 이 크루의 멤버가 아니면 crew_not_member(42501, 0049)
  perform public.crew_home_member(p_crew);
  select * into v_me from economy.crew_run_days d where d.user_id = v_user and d.day = v_day;
  select c.level, c.run_km into v_level, v_km from public.crews c where c.id = p_crew;
  return jsonb_build_object(
    'day', v_day,
    -- 다음 날(크루 체력이 새로 차는 때)
    'resets_at', economy.game_day_start(v_day + 1),
    -- 오늘 내 크루 러닝(어느 크루에서든 하루 한 번) — 했으면 그 한 줄
    'ran_today', v_me.user_id is not null,
    'mine', economy.crew_run_row_json(v_me),
    -- 지금 크루 레벨(크루 러닝이 아직 없으면 null — "새 크루") · 누적 거리 · 다음 레벨이 되는 거리(20 이면 null) · 보너스
    'crew_level', v_level,
    'run_km', v_km,
    'next_level_km', economy.crew_level_km(coalesce(v_level, 1) + 1),
    'stamina_bonus', economy.crew_level_stamina(v_level),
    'reward_bonus', economy.crew_level_reward(v_level),
    -- 오늘 이 크루에서 크루 러닝을 올린 사람 · 그들의 크루 체력 합 · 쓴 양
    'today', (select jsonb_build_object('runners', count(*), 'pool', coalesce(sum(d.share), 0),
                                        'used', coalesce(sum(d.used), 0))
                from economy.crew_run_days d where d.crew_id = p_crew and d.day = v_day),
    'rules', economy.crew_run_rules());
end;
$$;
revoke all on function public.crew_run_today(uuid) from public, anon;
grant execute on function public.crew_run_today(uuid) to authenticated;

-- 대기실 · 달리는 중 — 이 방의 사람마다 고른 신발과 크루 체력 미리보기. 방 사람만.
-- 출발 전에는 고른 신발(없으면 신은 신발)로, 출발한 뒤에는 출발 때 적어 둔 신발로 센다.
-- 미리보기는 방의 크루원 모두가 함께 달린다고 친 값이다 — 실제는 출발 2분 뒤 곁(500m)이었던 무리로 서버가 센다
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
  v_members jsonb;
  v_n int;
  v_pts numeric;
  v_pool numeric;
begin
  perform public.party_assert_member(p_party);
  select * into v_party from public.parties where id = p_party;
  v_level := greatest(coalesce(v_party.crew_level, (select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
  with x as (
    select m.user_id, m.joined_at, pr.display_name,
           coalesce(r.sneaker_id, s.id) as sneaker_id,
           case when r.user_id is not null then r.shoe_dur_max else s.dur_max end as dur_max,
           sh.rarity, sh.level as shoe_level, sh.variant,
           v_party.crew_id is not null and exists (select 1 from public.crew_members cm
                                                    where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id) as crew_member,
           exists (select 1 from economy.crew_run_days d
                    where d.user_id = m.user_id and d.day = economy.game_day(now())) as ran_today
      from public.party_members m
      left join public.profiles pr on pr.id = m.user_id
      left join public.party_runs r on r.party_id = m.party_id and r.user_id = m.user_id
      left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on r.user_id is null
      left join public.market_sneakers sh on sh.id = coalesce(r.sneaker_id, s.id)
     where m.party_id = p_party)
  select coalesce(jsonb_agg(jsonb_build_object(
           'user_id', x.user_id, 'name', coalesce(x.display_name, '러너'), 'is_me', x.user_id = v_user,
           'is_host', x.user_id = v_party.host_id, 'crew_member', x.crew_member, 'ran_today', x.ran_today,
           'sneaker_id', x.sneaker_id, 'rarity', x.rarity, 'shoe_level', x.shoe_level, 'variant', x.variant,
           'dur_max', coalesce(x.dur_max, 0))
           order by (x.user_id = v_party.host_id) desc, x.joined_at), '[]'::jsonb),
         count(*) filter (where x.crew_member),
         coalesce(sum(coalesce(x.dur_max, 0)) filter (where x.crew_member), 0)
    into v_members, v_n, v_pts
    from x;
  v_pool := round(v_pts * (1 + economy.crew_level_stamina(v_level)), 2);
  return jsonb_build_object(
    'party_id', p_party,
    'crew_id', v_party.crew_id,
    'status', v_party.status,
    'starts_at', v_party.starts_at,
    'crew_level', v_level,
    'stamina_bonus', economy.crew_level_stamina(v_level),
    'reward_bonus', economy.crew_level_reward(v_level),
    'runners', v_n,
    'pool', v_pool,
    'share', case when v_n > 0 then round(v_pool / v_n, 2) else 0 end,
    'share_sec', case when v_n > 0 then floor(v_pool / v_n * economy.durability_sec_per_point())::int else 0 end,
    'headcount_bonus', economy.crew_headcount_bonus(v_n),
    'members', v_members,
    'rules', economy.crew_run_rules());
end;
$$;
revoke all on function public.crew_run_room(bigint) from public, anon;
grant execute on function public.crew_run_room(bigint) to authenticated;

-- 결과 — 내 러닝(시작 시각)이 크루 러닝이었으면 그 한 줄, 아니면 null
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
  v_d economy.crew_run_days;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into v_s from public.walk_sessions s where s.user_id = v_user and s.started_at = p_started_at;
  if not found or v_s.energy_pool <> 'CREW' then
    return null;
  end if;
  select * into v_d from economy.crew_run_days d where d.user_id = v_user and d.session_id = v_s.id;
  if not found then
    return null;
  end if;
  return economy.crew_run_row_json(v_d) || jsonb_build_object(
    'points', v_s.points_awarded,
    'verdict', v_s.verdict,
    'reason', v_s.verdict_reason,
    'distance_m', v_s.distance_meters,
    'crew_km', round((v_s.distance_meters / 1000)::numeric, 3),
    'crew_name', (select c.name from public.crews c where c.id = v_d.crew_id),
    'crew_level_now', (select c.level from public.crews c where c.id = v_d.crew_id),
    'crew_run_km', (select c.run_km from public.crews c where c.id = v_d.crew_id),
    'next_level_km', (select economy.crew_level_km(coalesce(c.level, 1) + 1) from public.crews c where c.id = v_d.crew_id));
end;
$$;
revoke all on function public.crew_run_result(timestamptz) from public, anon;
grant execute on function public.crew_run_result(timestamptz) to authenticated;

-- 레벨 안내 — 0069 그대로 + 레벨 표 20단계(크루 체력 · 보상 보너스). 'bonus' 는 예전 앱을 위해 0069 값 그대로 둔다
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
    'run_km', v_run_km,
    'next_level_km', economy.crew_level_km(coalesce(v_level, 1) + 1),
    'level_bonus', economy.crew_level_bonus() * (coalesce(v_level, 1) - 1),
    'level_max', 20,
    'stamina_bonus', economy.crew_level_stamina(v_level),
    'reward_bonus', economy.crew_level_reward(v_level),
    'levels', (select jsonb_agg(jsonb_build_object('level', l, 'km', coalesce(economy.crew_level_km(l), 0),
                                                   'bonus', economy.crew_level_bonus() * (l - 1),
                                                   'stamina', economy.crew_level_stamina(l),
                                                   'reward', economy.crew_level_reward(l)) order by l)
                 from generate_series(1, 20) l));
end;
$$;
revoke execute on function public.crew_level(uuid) from public, anon;
grant execute on function public.crew_level(uuid) to authenticated;
