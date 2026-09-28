-- ════════════════════════════════════════════════════════════════════
--  0044 — 온체인 활동 1 · 2단계 (2026-09-28, 가스비는 모두 우리가 낸다 — 지금은 테스트넷)
--
--  사용자가 실제로 한 일 하나 = 체인 기록 최대 하나. 서버가 확인한 것만 올린다.
--
--    RUN_PROOF   서버가 경로로 확인한 러닝 1회(1km 이상, 무효 · 적립 보류 아님)
--                → EAS 증명(거리 · 시간 · 날짜 · 가명). 위치는 올리지 않는다.
--    COURSE_RUN  서버가 코스를 따라 달렸다고 확인한 완주 1회 → EAS 증명
--    BADGE       첫 러닝 · 누적 거리 · 연속 러닝 날 달성(한 사람에 한 번씩) → EAS 증명
--    VAULT_MINT  뽑은 신발(무료 · 상급) → v3 신발 컨트랙트의 금고로 발행. 앱에서 그대로 신는다
--    STATS_SYNC  금고에 있는 v3 신발의 강화 · 수리 → 체인 스탯 갱신(레벨은 오르기만)
--
--  흐름: 서버가 일을 줄 세운다(chain_jobs) → 어테스터 워커가 1분마다 몇 개씩 가져가
--  서명 · 가스비 대납으로 보낸다 → 영수증(과 v3 이벤트)으로 확정한다.
--
--  한 번만 — (종류, ref) 가 겹치면 줄을 세우지 않는다. 워커는 서명한 거래(raw)를 서버에 먼저
--  적고 나서 보낸다. 끊겨도 같은 거래를 다시 보낼 뿐 새 거래를 만들지 않는다 — 그 번호(nonce)가
--  다른 거래로 쓰여 이 거래가 영영 못 들어간다는 것이 확인된 뒤에만 다시 서명한다.
--
--  사람을 가리는 값 — 계정 번호 대신 가명(서버만 아는 salt 로 만든 해시)을 올린다. 지갑을
--  연결한 사람은 증명의 받는 사람(recipient)이 그 지갑이다. 연결 전이면 받는 사람 없음(0).
--
--  상급 뽑기는 더 이상 지갑 발행 예약(BONUS_MINT)을 만들지 않는다 — 예약만 하고 보내지 않아
--  10분 뒤 버려지던 빈틈을 금고 발행으로 바꾼다(신발은 앱에 남아 바로 신는다).
--  웹 지갑 페이지의 보너스 뽑기(bonus_draw_request)는 그대로 지갑으로 발행한다.
--
--  운영 값(economy_settings, admin_economy_set 로 바꾼다):
--    chain_jobs_enabled       false 면 새로 줄 세우지 않고 워커도 가져가지 않는다
--    chain_jobs_global_daily  하루(한국)에 보내는 체인 기록 전체 상한
--    run_proof_min_m          증명하는 러닝의 최소 경로 거리
--    run_proof_user_daily     한 사람의 하루 러닝 증명 상한(코스 완주 증명도 따로 이만큼)
--    badge_distance_km        누적 거리 배지 단계
--    badge_streak_days        연속 러닝 날 배지 단계
--    chain_job_deadline_sec   v3 서명의 유효 시간
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('chain_jobs_enabled',      'true'::jsonb),
  ('chain_jobs_global_daily', '20000'::jsonb),
  ('run_proof_min_m',         '1000'::jsonb),
  ('run_proof_user_daily',    '10'::jsonb),
  ('badge_distance_km',       '[10, 50, 100, 300, 500, 1000]'::jsonb),
  ('badge_streak_days',       '[3, 7, 14, 30, 100]'::jsonb),
  ('chain_job_deadline_sec',  '3600'::jsonb)
on conflict (key) do nothing;

-- ══════════════════════════════════════════════════════════════════
-- 가명 — 서버만 아는 salt. 앱 · 워커 · 관리자 화면 어디에도 나가지 않는다.
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.chain_secrets (
  k text primary key,
  v bytea not null
);
revoke all on economy.chain_secrets from public;
do $$ begin
  execute 'revoke all on economy.chain_secrets from anon, authenticated';
exception when undefined_object then null; end $$;
insert into economy.chain_secrets (k, v) values ('pseudonym_salt', economy.random_bytes32())
on conflict (k) do nothing;

-- 가명 bytes32 — 같은 사람(또는 러닝 · 코스)은 늘 같은 값, 거꾸로는 풀 수 없다
create or replace function economy.pseudonym(p_kind text, p_id text) returns text
language sql stable security definer set search_path = public, economy as $$
  select '0x' || encode(sha256(s.v || convert_to(p_kind || ':' || p_id, 'UTF8')), 'hex')
    from economy.chain_secrets s where s.k = 'pseudonym_salt'
$$;
revoke all on function economy.pseudonym(text, text) from public;

-- 한국 날짜 → 20260928 (EAS 에 uint32 로)
create or replace function economy.day_number(p_day date) returns int
  language sql immutable as $$ select to_char(p_day, 'YYYYMMDD')::int $$;

-- v3 토큰 번호는 여기서 시작한다(v2 번호와 겹치지 않는다 — contracts/StepUpSneakersV3.sol)
create or replace function economy.v3_first_token() returns numeric
  language sql immutable as $$ select 1000001::numeric $$;

create index if not exists market_sneakers_token on public.market_sneakers (token_id) where token_id is not null;

-- ══════════════════════════════════════════════════════════════════
-- 줄
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.chain_jobs (
  id bigint generated always as identity primary key,
  -- 체인에 올라가는 작업 번호(bytes32). chain_ops 의 번호와 같은 꼴이라 한 컨트랙트에서 겹치지 않는다
  op_id uuid not null default gen_random_uuid() unique,
  kind text not null check (kind in ('RUN_PROOF', 'COURSE_RUN', 'BADGE', 'VAULT_MINT', 'STATS_SYNC')),
  -- 무엇에 대한 기록인가 — (kind, ref) 하나에 한 번
  ref text not null,
  user_id uuid references auth.users on delete set null,
  sneaker_id bigint references public.market_sneakers on delete set null,
  session_id bigint references public.walk_sessions on delete set null,
  -- 줄 세울 때 정한 값(날짜 · 거리 · 배지 …). 체인에 보낼 값은 가져갈 때 다시 만든다
  args jsonb not null default '{}'::jsonb,
  status text not null default 'QUEUED'
    check (status in ('QUEUED', 'CLAIMED', 'SENT', 'CONFIRMED', 'FAILED', 'CANCELLED')),
  attempts int not null default 0,
  next_at timestamptz not null default now(),
  lease_until timestamptz,
  -- 워커에 준 서명 재료(감사용)
  payload jsonb,
  nonce bigint,
  tx_hash text,
  raw_tx text,
  sent_at timestamptz,
  -- 보낸 거래의 번호(nonce)가 다른 거래로 쓰인 것을 본 횟수 — 세 번 보면 이 거래는 죽었다
  dead_checks int not null default 0,
  -- 확정 결과 — EAS 증명 번호(uid) 또는 v3 토큰 번호
  result text,
  block_number bigint,
  error text,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique (kind, ref)
);

create index if not exists chain_jobs_ready on public.chain_jobs (next_at, id) where status in ('QUEUED', 'CLAIMED');
create index if not exists chain_jobs_sent on public.chain_jobs (nonce) where status = 'SENT';
create index if not exists chain_jobs_user on public.chain_jobs (user_id, created_at desc);
create index if not exists chain_jobs_sneaker on public.chain_jobs (sneaker_id) where sneaker_id is not null;
create index if not exists chain_jobs_sent_day on public.chain_jobs (sent_at) where sent_at is not null;
-- 같은 신발의 스탯 갱신은 기다리는 것 하나로 모은다 — 가져갈 때 그때의 스탯을 보낸다
create unique index if not exists chain_jobs_sync_waiting on public.chain_jobs (sneaker_id)
  where kind = 'STATS_SYNC' and status = 'QUEUED';

alter table public.chain_jobs enable row level security;
revoke all on public.chain_jobs from anon, authenticated;

-- 누적 거리 · 연속 러닝 날 — 배지를 세는 곳. 앱은 읽지도 쓰지도 못한다
create table if not exists public.chain_badge_progress (
  user_id uuid primary key references auth.users on delete cascade,
  total_m double precision not null default 0,
  runs int not null default 0,
  streak int not null default 0,
  last_day date,
  updated_at timestamptz not null default now()
);
alter table public.chain_badge_progress enable row level security;
revoke all on public.chain_badge_progress from anon, authenticated;

create or replace function economy.chain_jobs_on() returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select coalesce((economy.setting('chain_jobs_enabled') #>> '{}')::boolean, false)
$$;
revoke all on function economy.chain_jobs_on() from public;

-- 증명할 만한 러닝인가 — 무효가 아니고, 경로가 받쳐 주고, 적립 보류(무효가 잦은 사람)가 아니고,
-- 가짜 위치가 아니고, 경로로 잰 거리가 기준 이상. 하루 상한에 걸린(FLAGGED) 러닝도 거리는 진짜다.
create or replace function economy.run_provable(s public.walk_sessions) returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select s.verdict <> 'VOID' and s.gps_backed and s.backed_steps > 0 and not s.mock_location
         and s.gps_distance_m >= coalesce(economy.setting_num('run_proof_min_m'), 1000)
$$;
revoke all on function economy.run_provable(public.walk_sessions) from public;

-- ══════════════════════════════════════════════════════════════════
-- 배지
-- ══════════════════════════════════════════════════════════════════
-- 처음 세는 사람은 지난 러닝으로 채운다 — 이미 달린 거리 · 연속 날도 그 사람이 한 일이다
create or replace function economy.badge_progress_init(p_user uuid) returns public.chain_badge_progress
language plpgsql security definer set search_path = public, economy as $$
declare
  v public.chain_badge_progress;
  v_day date;
  v_prev date;
  v_streak int := 0;
begin
  select * into v from public.chain_badge_progress where user_id = p_user for update;
  if found then
    return v;
  end if;
  -- 가장 최근 날부터 거꾸로 이어진 날만 센다
  for v_day in
    select distinct economy.game_day(s.started_at) as d
      from public.walk_sessions s
     where s.user_id = p_user and economy.run_provable(s)
     order by d desc
     limit 400
  loop
    exit when v_prev is not null and v_day <> v_prev - 1;
    v_streak := v_streak + 1;
    v_prev := v_day;
  end loop;
  insert into public.chain_badge_progress (user_id, total_m, runs, streak, last_day)
  select p_user, coalesce(sum(s.gps_credit_m), 0), count(*), v_streak,
         max(economy.game_day(s.started_at))
    from public.walk_sessions s
   where s.user_id = p_user and economy.run_provable(s)
  on conflict (user_id) do nothing;
  select * into v from public.chain_badge_progress where user_id = p_user for update;
  return v;
end $$;
revoke all on function economy.badge_progress_init(uuid) from public;

create or replace function economy.badge_enqueue(p_user uuid, p_badge text, p_value int, p_day date)
returns void language plpgsql security definer set search_path = public, economy as $$
begin
  insert into public.chain_jobs (kind, ref, user_id, args)
  values ('BADGE', 'badge:' || p_user || ':' || p_badge || ':' || p_value, p_user,
          jsonb_build_object('badge', p_badge, 'value', p_value, 'day', economy.day_number(p_day)))
  on conflict (kind, ref) do nothing;
end $$;
revoke all on function economy.badge_enqueue(uuid, text, int, date) from public;

-- ══════════════════════════════════════════════════════════════════
-- 러닝이 기록될 때 — 러닝 증명 · 배지 (record_session 이 walk_sessions 에 넣은 뒤)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_jobs_on_run() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_day date := economy.game_day(new.started_at);
  v_today int;
  v public.chain_badge_progress;
  v_fresh boolean;
  v_km jsonb;
  v_n int;
begin
  -- 체인 기록이 실패해도 러닝 기록은 그대로 된다 — 경고만 남긴다
  begin
    if not economy.chain_jobs_on() or not economy.run_provable(new) then
      return null;
    end if;

    -- 러닝 증명 — 한 사람의 하루 상한 안에서
    select count(*) into v_today from public.chain_jobs j
     where j.user_id = new.user_id and j.kind = 'RUN_PROOF' and (j.args ->> 'day')::int = economy.day_number(v_day);
    if v_today < coalesce(economy.setting_num('run_proof_user_daily'), 0) then
      insert into public.chain_jobs (kind, ref, user_id, session_id, args)
      values ('RUN_PROOF', 'run:' || new.id, new.user_id, new.id,
              jsonb_build_object('day', economy.day_number(v_day),
                                 'distance_m', round(new.gps_distance_m)::int,
                                 'duration_sec', new.duration_sec))
      on conflict (kind, ref) do nothing;
    end if;

    -- 배지 — 처음이면 지난 러닝(이 러닝 포함)으로 채우고, 아니면 이 러닝을 더한다
    v_fresh := not exists (select 1 from public.chain_badge_progress where user_id = new.user_id);
    v := economy.badge_progress_init(new.user_id);
    if not v_fresh then
      v.total_m := v.total_m + new.gps_credit_m;
      v.runs := v.runs + 1;
      if v.last_day is null or v_day > v.last_day + 1 then
        v.streak := 1;
      elsif v_day = v.last_day + 1 then
        v.streak := v.streak + 1;
      end if;
      -- 예전 날짜의 러닝을 늦게 올린 것은 연속 날을 바꾸지 않는다
      v.last_day := greatest(coalesce(v.last_day, v_day), v_day);
      update public.chain_badge_progress
         set total_m = v.total_m, runs = v.runs, streak = v.streak, last_day = v.last_day, updated_at = now()
       where user_id = new.user_id;
    end if;

    if v.runs >= 1 then
      perform economy.badge_enqueue(new.user_id, 'FIRST_RUN', 1, v_day);
    end if;
    for v_km in select * from jsonb_array_elements(coalesce(economy.setting('badge_distance_km'), '[]'::jsonb)) loop
      v_n := (v_km #>> '{}')::int;
      if v.total_m >= v_n * 1000 then
        perform economy.badge_enqueue(new.user_id, 'DISTANCE_KM', v_n, v_day);
      end if;
    end loop;
    for v_km in select * from jsonb_array_elements(coalesce(economy.setting('badge_streak_days'), '[]'::jsonb)) loop
      v_n := (v_km #>> '{}')::int;
      if v.streak >= v_n then
        perform economy.badge_enqueue(new.user_id, 'STREAK_DAYS', v_n, v_day);
      end if;
    end loop;
    return null;
  exception when others then
    raise warning 'chain_jobs_on_run: %', sqlerrm;
  end;
  return null;
end $$;
revoke all on function economy.chain_jobs_on_run() from public;

drop trigger if exists walk_sessions_chain_jobs on public.walk_sessions;
create trigger walk_sessions_chain_jobs
  after insert on public.walk_sessions
  for each row execute function economy.chain_jobs_on_run();

-- ══════════════════════════════════════════════════════════════════
-- 코스 완주가 기록될 때 (course_run_submit 이 확인한 것만 들어온다)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_jobs_on_course_run() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  s public.walk_sessions;
  v_km double precision;
begin
  -- 체인 기록이 실패해도 코스 완주 기록은 그대로 된다 — 경고만 남긴다
  begin
    if not economy.chain_jobs_on() then
      return null;
    end if;
    select * into s from public.walk_sessions where id = new.session_id;
    if not found or s.verdict = 'VOID' or s.mock_location then
      return null;
    end if;
    -- 러닝 증명과 같은 기준 — 짧은 코스를 되풀이해 기록을 불리지 못하게 거리 · 하루 상한
    select c.distance_km into v_km from public.courses c where c.id = new.course_id;
    if coalesce(v_km, 0) * 1000 < coalesce(economy.setting_num('run_proof_min_m'), 1000)
       or (select count(*) from public.chain_jobs j
            where j.user_id = new.user_id and j.kind = 'COURSE_RUN'
              and (j.args ->> 'day')::int = economy.day_number(economy.game_day(s.started_at)))
          >= coalesce(economy.setting_num('run_proof_user_daily'), 0) then
      return null;
    end if;
    insert into public.chain_jobs (kind, ref, user_id, session_id, args)
    values ('COURSE_RUN', 'course_run:' || new.id, new.user_id, new.session_id,
            jsonb_build_object('course_id', new.course_id,
                               'day', economy.day_number(economy.game_day(s.started_at)),
                               'distance_m', round(coalesce(v_km, 0) * 1000)::int,
                               'duration_sec', new.duration_sec))
    on conflict (kind, ref) do nothing;
    return null;
  exception when others then
    raise warning 'chain_jobs_on_course_run: %', sqlerrm;
  end;
  return null;
end $$;
revoke all on function economy.chain_jobs_on_course_run() from public;

drop trigger if exists course_runs_chain_jobs on public.course_runs;
create trigger course_runs_chain_jobs
  after insert on public.course_runs
  for each row execute function economy.chain_jobs_on_course_run();

-- ══════════════════════════════════════════════════════════════════
-- 신발 — 뽑으면 금고 발행, 강화 · 수리하면 스탯 갱신
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.vault_mint_enqueue(p_sneaker bigint) returns void
language plpgsql security definer set search_path = public, economy as $$
begin
  -- 체인 기록이 실패해도 뽑기는 그대로 된다 — 경고만 남긴다
  begin
    if not economy.chain_jobs_on() then
      return;
    end if;
    insert into public.chain_jobs (kind, ref, user_id, sneaker_id)
    select 'VAULT_MINT', 'mint:' || s.id, s.owner_id, s.id
      from public.market_sneakers s
     where s.id = p_sneaker and s.token_id is null
    on conflict (kind, ref) do nothing;
  exception when others then
    raise warning 'vault_mint_enqueue: %', sqlerrm;
  end;
  return;
end $$;
revoke all on function economy.vault_mint_enqueue(bigint) from public;

-- 레벨이 오르거나(강화) 내구도가 오르면(수리) — 금고에 있는 v3 신발만. 달려서 닳는 것은 올리지 않는다.
create or replace function economy.chain_jobs_on_sneaker() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  -- 체인 기록이 실패해도 강화 · 수리는 그대로 된다 — 경고만 남긴다
  begin
    if economy.chain_jobs_on()
       and new.token_id >= economy.v3_first_token() and new.chain_state = 'APP'
       and (new.level > old.level or new.durability_pts > old.durability_pts) then
      insert into public.chain_jobs (kind, ref, user_id, sneaker_id)
      values ('STATS_SYNC', 'sync:' || new.id || ':' || gen_random_uuid(), new.owner_id, new.id)
      on conflict (sneaker_id) where kind = 'STATS_SYNC' and status = 'QUEUED' do nothing;
    end if;
    return null;
  exception when others then
    raise warning 'chain_jobs_on_sneaker: %', sqlerrm;
  end;
  return null;
end $$;
revoke all on function economy.chain_jobs_on_sneaker() from public;

drop trigger if exists market_sneakers_chain_jobs on public.market_sneakers;
create trigger market_sneakers_chain_jobs
  after update of level, durability_pts on public.market_sneakers
  for each row execute function economy.chain_jobs_on_sneaker();

-- 무료 뽑기 — 0042 그대로 + 금고 발행
create or replace function public.draw_free()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_daily int := greatest(coalesce(economy.setting_num('daily_free_draws'), 0)::int, 0);
  v_day date := economy.game_day(now());
  v_used int;
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select coalesce((select d.used from public.draw_daily d where d.user_id = v_user and d.day = v_day), 0)
    into v_used;
  if v_used < v_daily then
    insert into public.draw_daily (user_id, day, used) values (v_user, v_day, 1)
    on conflict (user_id, day) do update set used = public.draw_daily.used + 1;
  else
    update public.draw_grants set used = used + 1
     where user_id = v_user and kind = 'FREE' and used < granted;
    if not found then
      raise exception '무료 뽑기가 남아 있지 않습니다' using errcode = '23514';
    end if;
  end if;

  v_id := economy.draw_create(v_user, 'FREE_DRAW');
  perform economy.vault_mint_enqueue(v_id);
  return v_id;
end $$;

-- 상급 뽑기 — 0042 에서 지갑 발행 예약(BONUS_MINT)만 금고 발행으로 바꿨다. 신발은 앱에 남는다.
create or replace function public.premium_draw()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_genesis boolean := false;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 지갑을 연결한 사람만(지갑 혜택), 체인이 멈춰 있으면 기다린다 — 예전과 같은 조건
  perform economy.withdraw_gate(v_user, false);

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g
   where g.user_id = v_user and g.kind = 'BONUS' and g.used < g.granted;
  if found then
    update public.draw_grants
       set used = used + 1,
           genesis_used = genesis_used + case when v_genesis then 1 else 0 end
     where user_id = v_user and kind = 'BONUS';
  else
    v_genesis := false;
    update public.draw_grants set used = used + 1
     where user_id = v_user and kind = 'RUN' and used < granted;
    if not found then
      raise exception '상급 뽑기가 남아 있지 않습니다' using errcode = '23514';
    end if;
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  perform economy.vault_mint_enqueue(v_id);
  return v_id;
end $$;

revoke all on function public.draw_free() from public, anon;
revoke all on function public.premium_draw() from public, anon;
grant execute on function public.draw_free() to authenticated;
grant execute on function public.premium_draw() to authenticated;

-- 이미 뽑은 신발도 체인에 — 한 번. 아직 토큰이 없는 뽑기 신발(첫 신발 · 예전 폰 신발 제외).
-- 상급 뽑기로 예약만 되어 있던(곧 만료될) 신발은 만료 뒤 앱으로 돌아오면 가져갈 때 발행된다.
-- 1분에 한 켤레씩 차례를 준다 — 한꺼번에 줄 서면 다 빠질 때까지 새 러닝 증명 · 새 뽑기가 뒤로 밀린다.
insert into public.chain_jobs (kind, ref, user_id, sneaker_id, next_at)
select 'VAULT_MINT', 'mint:' || s.id, s.owner_id, s.id,
       now() + make_interval(mins => (row_number() over (order by s.id))::int)
  from public.market_sneakers s
 where s.origin in ('FREE_DRAW', 'PAID_DRAW', 'BONUS_DRAW')
   and s.token_id is null and s.owner_id is not null
   and economy.chain_jobs_on()
on conflict (kind, ref) do nothing;

-- ══════════════════════════════════════════════════════════════════
-- 꺼내기 — 체인에 기록하는 중인 신발은 기다린다
-- ══════════════════════════════════════════════════════════════════
-- 금고 발행 · 스탯 갱신을 보내는 중에 꺼내면 같은 신발이 두 컨트랙트에 생기거나 갱신이 헛돈다.
-- 아직 가져가지 않은 일은 그대로 둔다 — 가져갈 때 꺼내는 중(작업이 열려 있음)이면 미루고,
-- 꺼내기가 끝나 토큰이 생겼으면 금고 발행을 거둔다.
create or replace function public.sneaker_withdraw_request(p_sneaker_id bigint)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v public.market_sneakers;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  v := economy.my_app_sneaker(v_user, p_sneaker_id);
  if not v.withdrawable or v.origin in ('IMPORT', 'MINT', 'STARTER') then
    raise exception '꺼낼 수 없는 신발입니다' using errcode = '22023';
  end if;
  if v.km_run < v.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 꺼낼 수 있습니다', round(v.lock_km - v.km_run, 1)
      using errcode = '23514';
  end if;
  if exists (select 1 from public.chain_jobs j
              where j.sneaker_id = p_sneaker_id and j.kind in ('VAULT_MINT', 'STATS_SYNC')
                and j.status in ('CLAIMED', 'SENT')) then
    raise exception '이 신발을 체인에 기록하는 중입니다. 잠시 뒤에 다시 해 주세요' using errcode = '55000';
  end if;
  if v.token_id is null and economy.mints_today('SNEAKER_WITHDRAW') >= economy.setting_num('mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  update public.market_sneakers
     set chain_state = 'WITHDRAWING', equipped = false, updated_at = now()
   where id = p_sneaker_id;
  -- 신고 있던 신발을 꺼내면 첫 신발로 갈아 신긴다(신발 없이 달리는 틈이 없게)
  update public.market_sneakers set equipped = true, updated_at = now()
   where id = (select m.id from public.market_sneakers m
                where m.owner_id = v_user and m.origin = 'STARTER'
                  and m.chain_state = 'APP' and m.status = 'OWNED' limit 1)
     and not exists (select 1 from public.market_sneakers e where e.owner_id = v_user and e.equipped);

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'SNEAKER_WITHDRAW', v_wallet, p_sneaker_id, economy.op_deadline());
  return v_op;
end $$;
revoke all on function public.sneaker_withdraw_request(bigint) from public, anon;
grant execute on function public.sneaker_withdraw_request(bigint) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 어테스터 — 가져가기 · 보냄 · 결과
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.job_requeue(p_id bigint, p_error text, p_retry_sec int default null)
returns void language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_jobs;
begin
  select * into v from public.chain_jobs where id = p_id for update;
  if not found or v.status in ('CONFIRMED', 'CANCELLED', 'FAILED') then
    return;
  end if;
  if v.attempts + 1 >= 6 then
    update public.chain_jobs
       set status = 'FAILED', attempts = attempts + 1, error = left(p_error, 500), lease_until = null, updated_at = now()
     where id = p_id;
    return;
  end if;
  -- 같은 신발의 스탯 갱신이 이미 기다리고 있으면 이것은 거둔다(기다리는 것이 지금 스탯을 보낸다)
  if v.kind = 'STATS_SYNC' and exists (select 1 from public.chain_jobs j
                                        where j.sneaker_id = v.sneaker_id and j.kind = 'STATS_SYNC'
                                          and j.status = 'QUEUED' and j.id <> v.id) then
    update public.chain_jobs set status = 'CANCELLED', error = 'superseded', updated_at = now() where id = p_id;
    return;
  end if;
  update public.chain_jobs
     set status = 'QUEUED', attempts = attempts + 1, error = left(p_error, 500),
         -- 1분 · 2분 · 4분 · 8분 … (최대 1시간)
         next_at = now() + make_interval(secs => coalesce(p_retry_sec, least(3600, 60 * power(2, v.attempts)::int))),
         lease_until = null, nonce = null, tx_hash = null, raw_tx = null, sent_at = null, dead_checks = 0,
         updated_at = now()
   where id = p_id;
end $$;
revoke all on function economy.job_requeue(bigint, text, int) from public;

create or replace function economy.job_cancel(p_id bigint, p_reason text)
returns void language plpgsql security definer set search_path = public, economy as $$
begin
  update public.chain_jobs
     set status = 'CANCELLED', error = p_reason, lease_until = null, updated_at = now()
   where id = p_id and status in ('QUEUED', 'CLAIMED');
end $$;
revoke all on function economy.job_cancel(bigint, text) from public;

/*
 * 보낼 일을 가져간다. 워커가 다룰 수 있는 종류만(p_kinds), 몇 개만(p_limit).
 * 가져간 일은 2분 동안 이 워커 것(CLAIMED) — 그 안에 서명한 거래를 적지 않으면 다음 워커가 다시 가져간다
 * (서명한 거래를 적기 전에는 보내지 않으므로 두 번 나가지 않는다).
 *
 * 돌려주는 payload (종류마다):
 *   RUN_PROOF   recipient · runner · run · day · distance_m · duration_sec
 *   COURSE_RUN  recipient · runner · course · run · day · distance_m · duration_sec
 *   BADGE       recipient · runner · badge · value · day
 *   VAULT_MINT  account · faction · rarity · variant · level · efficiency_bps · comfort_bps · durability · genesis_no · deadline_unix
 *   STATS_SYNC  token_id · level · durability · deadline_unix
 */
create or replace function public.attester_jobs_claim(p_kinds text[], p_limit int)
returns table (job_id bigint, op_ref text, kind text, payload jsonb)
language plpgsql security definer set search_path = public, economy as $$
declare
  j public.chain_jobs;
  v_left int;
  v_taken int := 0;
  v_want int := greatest(0, least(coalesce(p_limit, 0), 50));
  v_payload jsonb;
  v_wallet text;
  v_owner uuid;
  s public.market_sneakers;
  w public.walk_sessions;
  v_deadline bigint := extract(epoch from now())::bigint
                       + coalesce(economy.setting_num('chain_job_deadline_sec'), 3600)::bigint;
begin
  perform economy.attester_guard();
  if coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false) or not economy.chain_jobs_on() then
    return;
  end if;
  -- 오늘 보낸 수 + 지금 보내는 중인 수가 하루 상한 안에서만
  v_left := coalesce(economy.setting_num('chain_jobs_global_daily'), 0)::int
            - (select count(*) from public.chain_jobs c
                where (c.sent_at >= economy.today_start()) or c.status = 'CLAIMED')::int;
  v_want := least(v_want, greatest(v_left, 0));

  for j in
    select * from public.chain_jobs c
     where c.kind = any (p_kinds)
       and (c.status = 'QUEUED' or (c.status = 'CLAIMED' and c.lease_until < now()))
       and c.next_at <= now()
     order by c.next_at, c.id
     limit 200
     for update skip locked
  loop
    exit when v_taken >= v_want;
    v_payload := null;

    if j.user_id is null then
      perform economy.job_cancel(j.id, 'account deleted');
      continue;
    end if;

    if j.kind in ('RUN_PROOF', 'COURSE_RUN', 'BADGE') then
      if j.kind <> 'BADGE' then
        select * into w from public.walk_sessions where id = j.session_id;
        if not found or w.verdict = 'VOID' then
          perform economy.job_cancel(j.id, 'run gone');
          continue;
        end if;
      end if;
      select address into v_wallet from public.wallet_links where user_id = j.user_id;
      v_payload := jsonb_build_object(
        'recipient', coalesce(v_wallet, '0x0000000000000000000000000000000000000000'),
        'runner', economy.pseudonym('runner', j.user_id::text),
        'day', (j.args ->> 'day')::int);
      if j.kind = 'RUN_PROOF' then
        v_payload := v_payload || jsonb_build_object(
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      elsif j.kind = 'COURSE_RUN' then
        v_payload := v_payload || jsonb_build_object(
          'course', economy.pseudonym('course', j.args ->> 'course_id'),
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      else
        v_payload := v_payload || jsonb_build_object(
          'badge', j.args ->> 'badge',
          'value', (j.args ->> 'value')::int);
      end if;

    else
      -- 신발 — 지금 주인의 꺼내기와 엇갈리지 않게 같은 잠금을 잡는다(앱에서 팔렸으면 새 주인).
      -- 못 잡으면(꺼내는 중) 다음에 — 기다리지 않아 서로 잠금을 기다리며 멈추는 일이 없다
      select owner_id into v_owner from public.market_sneakers where id = j.sneaker_id;
      if v_owner is null then
        perform economy.job_cancel(j.id, 'sneaker gone');
        continue;
      end if;
      if not pg_try_advisory_xact_lock(hashtext('ledger:' || v_owner::text)) then
        continue;
      end if;
      select * into s from public.market_sneakers where id = j.sneaker_id;
      if not found or s.owner_id is distinct from v_owner then
        continue;
      end if;
      if exists (select 1 from public.chain_ops o
                  where o.sneaker_id = s.id and o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')) then
        -- 꺼내는 중 — 끝나거나 만료된 뒤에 다시 본다
        update public.chain_jobs set next_at = now() + interval '10 minutes', updated_at = now() where id = j.id;
        continue;
      end if;
      if j.kind = 'VAULT_MINT' then
        if s.token_id is not null then
          perform economy.job_cancel(j.id, 'already on chain');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'account', economy.pseudonym('runner', s.owner_id::text),
          'faction', s.faction, 'rarity', s.rarity, 'variant', s.variant, 'level', s.level,
          'efficiency_bps', s.efficiency_bps, 'comfort_bps', s.comfort_bps,
          'durability', s.durability_pts, 'genesis_no', s.genesis_no,
          'deadline_unix', v_deadline);
      else
        if s.token_id is null or s.token_id < economy.v3_first_token() or s.chain_state <> 'APP' then
          perform economy.job_cancel(j.id, 'not in the v3 vault');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'token_id', s.token_id::text, 'level', s.level, 'durability', s.durability_pts,
          'deadline_unix', v_deadline);
      end if;
    end if;

    update public.chain_jobs c
       set status = 'CLAIMED', lease_until = now() + interval '2 minutes', payload = v_payload, updated_at = now()
     where c.id = j.id;
    v_taken := v_taken + 1;
    job_id := j.id;
    op_ref := economy.op_ref(j.op_id);
    kind := j.kind;
    payload := v_payload;
    return next;
  end loop;
end $$;

/*
 * 서명한 거래를 보내기 전에 적는다. [{ "id": 1, "nonce": 12, "tx": "0x…", "raw": "0x…" }]
 * 가져간 일(CLAIMED)만 받는다 — 적힌 일만 보낸다.
 */
create or replace function public.attester_jobs_signed(p_items jsonb)
returns setof bigint
language plpgsql security definer set search_path = public, economy as $$
declare it jsonb;
begin
  perform economy.attester_guard();
  for it in select * from jsonb_array_elements(coalesce(p_items, '[]'::jsonb)) loop
    if (it ->> 'tx') !~ '^0x[0-9a-fA-F]{64}$' or (it ->> 'raw') !~ '^0x[0-9a-fA-F]+$' then
      raise exception '거래가 올바르지 않습니다' using errcode = '22023';
    end if;
    return query
    update public.chain_jobs
       set status = 'SENT', nonce = (it ->> 'nonce')::bigint, tx_hash = lower(it ->> 'tx'),
           raw_tx = it ->> 'raw', sent_at = now(), dead_checks = 0, lease_until = null, updated_at = now()
     where id = (it ->> 'id')::bigint and status = 'CLAIMED'
    returning id;
  end loop;
end $$;

/*
 * 가져갔지만 보내지 못한 일. [{ "id": 1, "error": "…", "cancel": true|false, "retry_sec": 60 }]
 * cancel — 체인이 받지 않을 일(바뀐 것 없는 갱신 · 금고에 없는 신발 · 취소된 작업 번호).
 */
create or replace function public.attester_jobs_release(p_items jsonb)
returns void
language plpgsql security definer set search_path = public, economy as $$
declare it jsonb;
begin
  perform economy.attester_guard();
  for it in select * from jsonb_array_elements(coalesce(p_items, '[]'::jsonb)) loop
    if exists (select 1 from public.chain_jobs where id = (it ->> 'id')::bigint and status = 'CLAIMED') then
      if coalesce((it ->> 'cancel')::boolean, false) then
        perform economy.job_cancel((it ->> 'id')::bigint, left(coalesce(it ->> 'error', 'cancelled'), 500));
      else
        perform economy.job_requeue((it ->> 'id')::bigint, coalesce(it ->> 'error', 'released'),
                                    nullif(it ->> 'retry_sec', '')::int);
      end if;
    end if;
  end loop;
end $$;

-- 보낸 뒤 확정을 기다리는 일 — 번호(nonce) 순서로
create or replace function public.attester_jobs_open(p_limit int default 20)
returns table (job_id bigint, kind text, nonce bigint, tx_hash text, raw_tx text, sent_at timestamptz, dead_checks int)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select c.id, c.kind, c.nonce, c.tx_hash, c.raw_tx, c.sent_at, c.dead_checks
    from public.chain_jobs c
   where c.status = 'SENT'
   order by c.nonce nulls last, c.id
   limit greatest(1, least(coalesce(p_limit, 20), 100));
end $$;

-- 확정 — 이미 확정이면 아무것도 안 한다. 금고 발행이면 그 신발에 토큰 번호를 적는다.
create or replace function economy.job_confirm(p_id bigint, p_tx text, p_block bigint, p_result text)
returns text language plpgsql security definer set search_path = public, economy as $$
declare
  v public.chain_jobs;
  v_token numeric;
begin
  select * into v from public.chain_jobs where id = p_id for update;
  if not found then
    return 'UNKNOWN';
  end if;
  if v.status = 'CONFIRMED' then
    if v.kind = 'VAULT_MINT' and p_result is not null and v.result is distinct from p_result then
      return 'MISMATCH';
    end if;
    return 'DUPLICATE';
  end if;
  if v.kind = 'VAULT_MINT' then
    v_token := nullif(p_result, '')::numeric;
    if v_token is null or v_token < economy.v3_first_token()
       or exists (select 1 from public.market_sneakers where token_id = v_token and id is distinct from v.sneaker_id)
       or exists (select 1 from public.market_sneakers where id = v.sneaker_id and token_id is not null
                                                          and token_id <> v_token) then
      return 'MISMATCH';
    end if;
    update public.market_sneakers set token_id = v_token, updated_at = now()
     where id = v.sneaker_id and token_id is null;
  end if;
  update public.chain_jobs
     set status = 'CONFIRMED', tx_hash = coalesce(lower(p_tx), tx_hash), block_number = p_block,
         result = coalesce(p_result, result), raw_tx = null, lease_until = null, error = null, updated_at = now()
   where id = p_id;
  return 'CONFIRMED';
end $$;
revoke all on function economy.job_confirm(bigint, text, bigint, text) from public;

/*
 * 보낸 일의 결과. [{ "id": 1, "status": "CONFIRMED"|"RETRY"|"DEAD", "block": 123, "result": "0x…|1000001", "error": "…" }]
 *   CONFIRMED  영수증이 성공 — result 는 EAS 증명 번호(uid) 또는 v3 토큰 번호
 *   RETRY      영수증이 실패(되돌림) — 다시 줄에(새로 서명)
 *   DEAD       이 거래의 번호가 다른 거래로 쓰였고 이 거래는 없다 — 세 번 보면 다시 줄에
 * 돌려주는 값: 받아들이지 못한 것(MISMATCH) — 워커가 멈춤 신호로 쓴다.
 */
create or replace function public.attester_jobs_result(p_items jsonb)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  it jsonb;
  v public.chain_jobs;
  v_out text := 'OK';
  v_res text;
begin
  perform economy.attester_guard();
  for it in select * from jsonb_array_elements(coalesce(p_items, '[]'::jsonb)) loop
    select * into v from public.chain_jobs where id = (it ->> 'id')::bigint for update;
    continue when not found or v.status <> 'SENT';
    if it ->> 'status' = 'CONFIRMED' then
      v_res := economy.job_confirm(v.id, v.tx_hash, nullif(it ->> 'block', '')::bigint, it ->> 'result');
      if v_res = 'MISMATCH' then
        perform economy.chain_alarm('chain_job_mismatch', v.tx_hash, it);
        v_out := 'MISMATCH';
      end if;
    elsif it ->> 'status' = 'RETRY' then
      perform economy.job_requeue(v.id, coalesce(it ->> 'error', 'reverted'));
    elsif it ->> 'status' = 'DEAD' then
      if v.dead_checks + 1 >= 3 then
        perform economy.job_requeue(v.id, 'nonce taken by another transaction', 0);
      else
        update public.chain_jobs set dead_checks = dead_checks + 1, updated_at = now() where id = v.id;
      end if;
    end if;
  end loop;
  return v_out;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 체인 이벤트 — v3 의 금고 발행 · 스탯 갱신 · 작업 취소를 더한다 (0029 에 이어)
-- ══════════════════════════════════════════════════════════════════
/*
 *   SUP_CLAIMED        {"op": opRef, "runner": "0x…", "amount": "12.5"}
 *   SNEAKER_RELEASED   {"op": opRef, "tokenId": "123", "to": "0x…"}          (v2 · v3)
 *   OP_CANCELLED       {"op": opRef}                                           (v2 · v3)
 *   SUP_DEPOSITED      {"account": accountRef, "amount": "12.5"}
 *   SNEAKER_DEPOSITED  {"account": accountRef, "tokenId": "123", "from": "0x…"} (v2 · v3)
 *   VAULT_MINTED       {"op": opRef, "tokenId": "1000001", "account": "0x…"}   (v3)
 *   STATS_SYNCED       {"op": opRef, "tokenId": "1000001", "level": 3, "durability": 9500} (v3)
 *
 * 돌려주는 값: CONFIRMED · CREDITED · CANCELLED · ORPHAN · DUPLICATE · IGNORED,
 * 그리고 멈춰야 하는 UNKNOWN_OP · MISMATCH (어테스터가 컨트랙트도 멈춘다).
 */
create or replace function public.attester_chain_event(
  p_tx text, p_log int, p_block bigint, p_kind text, p_data jsonb
) returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_tx text := lower(p_tx);
  v_op uuid;
  v_user uuid;
  v_token numeric;
  v_amount numeric;
  o public.chain_ops;
  j public.chain_jobs;
  v_shoe_token numeric;
  v_res text;
begin
  perform economy.attester_guard();
  insert into public.chain_events (tx_hash, log_index, block_number, kind, data)
  values (v_tx, p_log, p_block, p_kind, p_data)
  on conflict do nothing;
  if not found then
    return 'DUPLICATE';
  end if;

  if p_kind in ('VAULT_MINTED', 'STATS_SYNCED') then
    v_op := economy.account_from_ref(p_data ->> 'op');
    select * into j from public.chain_jobs c
     where c.op_id = v_op and c.kind = case p_kind when 'VAULT_MINTED' then 'VAULT_MINT' else 'STATS_SYNC' end
       for update;
    if v_op is null or not found then
      -- 서버가 줄 세우지 않은 발행 · 갱신이 체인에서 일어났다 → 서명 키가 샜다
      perform economy.chain_alarm('chain_unknown_op', v_tx, p_data);
      return 'UNKNOWN_OP';
    end if;
    v_token := nullif(p_data ->> 'tokenId', '')::numeric;
    if p_kind = 'STATS_SYNCED' then
      if v_token is distinct from nullif(j.payload ->> 'token_id', '')::numeric then
        perform economy.chain_alarm('chain_op_mismatch', v_tx, p_data || jsonb_build_object('job', j.id));
        return 'MISMATCH';
      end if;
      if j.status = 'CONFIRMED' then
        return 'CONFIRMED';
      end if;
      update public.chain_jobs
         set status = 'CONFIRMED', tx_hash = v_tx, block_number = p_block, result = v_token::text,
             raw_tx = null, lease_until = null, error = null, updated_at = now()
       where id = j.id;
      return 'CONFIRMED';
    end if;
    -- 영수증으로 먼저 확정했을 수도 있다(같은 토큰이면 DUPLICATE, 다르면 MISMATCH)
    v_res := economy.job_confirm(j.id, v_tx, p_block, v_token::text);
    if v_res = 'MISMATCH' then
      perform economy.chain_alarm('chain_op_mismatch', v_tx, p_data || jsonb_build_object('job', j.id));
      return 'MISMATCH';
    end if;
    return 'CONFIRMED';
  end if;

  if p_kind in ('SUP_CLAIMED', 'SNEAKER_RELEASED') then
    v_op := economy.account_from_ref(p_data ->> 'op');
    v_token := nullif(p_data ->> 'tokenId', '')::numeric;
    select * into o from public.chain_ops c
     where c.id = v_op
       and c.kind = any (case p_kind when 'SUP_CLAIMED' then array['SUP_WITHDRAW']
                                     else array['SNEAKER_WITHDRAW', 'BONUS_MINT'] end);
    if v_op is null or not found then
      -- 서버가 서명하지 않은 작업으로 체인에서 나갔다 → 서명 키가 샜다
      perform economy.chain_alarm('chain_unknown_op', v_tx, p_data);
      return 'UNKNOWN_OP';
    end if;

    -- 작업 번호는 맞는데 받는 쪽 · 금액 · 신발이 서버 기록과 다르다 → 역시 멈춘다
    if p_kind = 'SUP_CLAIMED' then
      if (p_data ? 'runner' and lower(p_data ->> 'runner') is distinct from lower(o.wallet))
         or (p_data ? 'amount' and (p_data ->> 'amount')::numeric is distinct from o.amount) then
        perform economy.chain_alarm('chain_op_mismatch', v_tx,
          p_data || jsonb_build_object('wallet', o.wallet, 'expected', o.amount));
        return 'MISMATCH';
      end if;
    else
      select token_id into v_shoe_token from public.market_sneakers where id = o.sneaker_id;
      if (p_data ? 'to' and lower(p_data ->> 'to') is distinct from lower(o.wallet))
         or (v_shoe_token is not null and v_token is distinct from v_shoe_token)
         or (v_token is not null and exists (select 1 from public.market_sneakers
                                              where token_id = v_token and id is distinct from o.sneaker_id)) then
        perform economy.chain_alarm('chain_op_mismatch', v_tx,
          p_data || jsonb_build_object('wallet', o.wallet, 'sneaker', o.sneaker_id, 'token', v_shoe_token));
        return 'MISMATCH';
      end if;
    end if;

    perform economy.op_confirm(v_op, v_tx, p_block, v_token);
    return 'CONFIRMED';

  elsif p_kind = 'OP_CANCELLED' then
    v_op := economy.account_from_ref(p_data ->> 'op');
    -- v3 의 금고 발행 · 스탯 갱신 번호 — 그 번호로는 이제 체인이 받지 않는다. 그 일은 거둔다
    select * into j from public.chain_jobs c where c.op_id = v_op for update;
    if v_op is not null and found then
      if j.status not in ('CONFIRMED', 'CANCELLED', 'FAILED') then
        update public.chain_jobs
           set status = 'CANCELLED', error = 'cancelled on chain', raw_tx = null, lease_until = null, updated_at = now()
         where id = j.id;
      end if;
      return 'CANCELLED';
    end if;
    -- 관리자(또는 지킴이)가 체인에서 작업 번호를 막았다. 그 번호로는 이제 풀 수 없으니
    -- 신발을 앱으로 돌려놓는다. 만료를 기다리지 않는다.
    select * into o from public.chain_ops c where c.id = v_op for update;
    if v_op is null or not found or o.kind = 'SUP_WITHDRAW' then
      perform public.admin_log('chain_cancel_unknown', v_tx, p_data);
      return 'IGNORED';
    end if;
    if o.status in ('CONFIRMED', 'EXPIRED') then
      return 'IGNORED';
    end if;
    update public.chain_ops set status = 'EXPIRED', updated_at = now() where id = o.id;
    update public.market_sneakers
       set chain_state = 'APP', updated_at = now()
     where id = o.sneaker_id and chain_state = 'WITHDRAWING';
    return 'CANCELLED';

  elsif p_kind = 'SUP_DEPOSITED' then
    v_user := economy.account_from_ref(p_data ->> 'account');
    -- 원장은 4자리까지다. 남는 자리는 버린다 — 넣은 것보다 더 주지 않는다.
    v_amount := trunc((p_data ->> 'amount')::numeric, 4);
    if v_user is null or not exists (select 1 from auth.users where id = v_user) or v_amount <= 0 then
      -- 받을 계정이 없는 입금. 토큰은 금고에 있으므로 사람이 보고 돌려준다.
      perform public.admin_log('chain_orphan_deposit', v_tx, p_data);
      return 'ORPHAN';
    end if;
    perform economy.ledger_apply(v_user, 'CHAIN_DEPOSIT', v_amount, '체인에서 넣기',
                                 'dep:' || v_tx || ':' || p_log);
    return 'CREDITED';

  elsif p_kind = 'SNEAKER_DEPOSITED' then
    v_user := economy.account_from_ref(p_data ->> 'account');
    v_token := (p_data ->> 'tokenId')::numeric;
    if v_user is null or not exists (select 1 from auth.users where id = v_user)
       or not exists (select 1 from public.market_sneakers where token_id = v_token and chain_state = 'ON_CHAIN') then
      perform public.admin_log('chain_orphan_deposit', v_tx, p_data);
      return 'ORPHAN';
    end if;
    -- 잠금 거리를 못 채운(무료) 신발은 넣은 지갑이 그 계정의 지갑(지금 또는 예전)일 때만
    -- 받는다. 무료 신발을 다른 계정으로 옮겨 몰아주지 못하게.
    if exists (select 1 from public.market_sneakers where token_id = v_token and km_run < lock_km)
       and not exists (select 1 from public.wallet_links w
                        where w.user_id = v_user and w.address = lower(p_data ->> 'from'))
       and not exists (select 1 from public.wallet_history h
                        where h.user_id = v_user and h.address = lower(p_data ->> 'from')) then
      perform public.admin_log('chain_locked_deposit_mismatch', v_tx, p_data);
      return 'ORPHAN';
    end if;
    -- 넣은 사람이 새 주인이다(체인에서 샀을 수 있다). 스탯은 꺼낼 때 서버가 적은 값 그대로다.
    -- 주인 바꾸기 트리거는 이 표시가 있을 때만 체인 → 앱 이전을 허락한다.
    perform set_config('stepup.chain_deposit', 'on', true);
    update public.market_sneakers
       set owner_id = v_user, chain_state = 'APP', status = 'OWNED', equipped = false, updated_at = now()
     where token_id = v_token and chain_state = 'ON_CHAIN';
    perform set_config('stepup.chain_deposit', 'off', true);
    return 'CREDITED';
  end if;

  perform public.admin_log('chain_unknown_event', v_tx, jsonb_build_object('kind', p_kind, 'data', p_data));
  return 'IGNORED';
end $$;

-- 만료 차례의 작업 — 신발 토큰 번호를 더했다(v2 · v3 중 어느 컨트랙트를 볼지)
drop function if exists public.attester_due_ops();
create function public.attester_due_ops()
returns table (op_id uuid, op_ref text, status text, kind text, deadline timestamptz, tx_hash text, early boolean,
               token_id numeric)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select o.id, economy.op_ref(o.id), o.status, o.kind, o.deadline, o.tx_hash,
         o.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now(),
         s.token_id
    from public.chain_ops o
    left join public.market_sneakers s on s.id = o.sneaker_id
   where o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')
     and o.deadline < now()
   order by o.deadline
   limit 200;
end $$;

-- 어테스터 함수 권한 — 0025 와 같은 규칙을 새 함수에도 (첫 줄의 attester_guard 가 막는다)
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig from pg_proc p
     where p.pronamespace = 'public'::regnamespace and p.proname like 'attester\_%'
  loop
    execute format('revoke all on function %s from public, anon, authenticated', r.sig);
    execute format('grant execute on function %s to stepup_attester, authenticated', r.sig);
  end loop;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 내 체인 기록 — 앱 · 웹이 읽는다(서명한 거래 · 가명은 내보내지 않는다)
-- ══════════════════════════════════════════════════════════════════
create or replace function public.my_chain_activity(p_limit int default 50)
returns table (
  id bigint,
  kind text,
  status text,
  day int,
  distance_m int,
  duration_sec int,
  badge text,
  badge_value int,
  sneaker_id bigint,
  tx_hash text,
  result text,
  created_at timestamptz,
  confirmed_at timestamptz
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select c.id, c.kind,
         -- 확정 전(보내는 중)은 모두 대기로 — 서버가 확인하기 전에는 "기록 완료"가 아니다
         case when c.status = 'CONFIRMED' then 'CONFIRMED'
              when c.status in ('CANCELLED', 'FAILED') then c.status
              else 'PENDING' end,
         (c.args ->> 'day')::int, (c.args ->> 'distance_m')::int, (c.args ->> 'duration_sec')::int,
         c.args ->> 'badge', (c.args ->> 'value')::int, c.sneaker_id,
         case when c.status = 'CONFIRMED' then c.tx_hash end,
         case when c.status = 'CONFIRMED' then c.result end,
         c.created_at,
         case when c.status = 'CONFIRMED' then c.updated_at end
    from public.chain_jobs c
   where c.user_id = v_user
   order by c.id desc
   limit greatest(1, least(coalesce(p_limit, 50), 200));
end $$;
revoke all on function public.my_chain_activity(int) from public, anon;
grant execute on function public.my_chain_activity(int) to authenticated;

-- 관리자 — 종류마다 몇 건이 기다리고 · 나가고 · 확정됐는지(오늘 · 전체)
create or replace function public.admin_chain_jobs_stats()
returns table (kind text, queued bigint, in_flight bigint, confirmed_today bigint, confirmed_total bigint,
               failed bigint, cancelled bigint)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform public.admin_guard();
  return query
  select k.kind,
         count(*) filter (where c.status = 'QUEUED'),
         count(*) filter (where c.status in ('CLAIMED', 'SENT')),
         count(*) filter (where c.status = 'CONFIRMED' and c.updated_at >= economy.today_start()),
         count(*) filter (where c.status = 'CONFIRMED'),
         count(*) filter (where c.status = 'FAILED'),
         count(*) filter (where c.status = 'CANCELLED')
    from (values ('RUN_PROOF'), ('COURSE_RUN'), ('BADGE'), ('VAULT_MINT'), ('STATS_SYNC')) k(kind)
    left join public.chain_jobs c on c.kind = k.kind
   group by k.kind
   order by k.kind;
end $$;
revoke all on function public.admin_chain_jobs_stats() from public, anon;
grant execute on function public.admin_chain_jobs_stats() to authenticated;
