-- ════════════════════════════════════════════════════════════════════
--  0081 — 체인 스냅숏: 6시간마다 사람 · 크루 · 전체 상태를 올린다 (2026-11-02)
--
--  사용자: "새로운 게 있어야만 찍는 거임? 모든 데이터를 스냅샷처럼 주기적으로 계속 업로드" →
--  고른 값: 사람 + 크루 + 전체 · 6시간마다 · 활동 기록(0078)은 그대로 두고 더한다.
--
--  · 워커가 2분마다 attester_snapshot_tick() 을 부른다 — 이번 칸(한국 시간 0 · 6 · 12 · 18시)을
--    아직 줄 세우지 않았으면 한 번만 줄 세운다(economy.snapshot_slots).
--      SNAP_USER  계정마다 하나 — 누적 거리 · 러닝 수 · 잔고 · 신발 수 · 크루 수
--      SNAP_CREW  크루마다 하나 — 인원 · 크루 레벨 · 크루 러닝 누적 거리
--      SNAP_ALL   전체 하나     — 계정 · 크루 · 러닝 · 누적 거리 · 신발 · 잔고 합계
--  · 값은 보낼 때(가져갈 때) 서버에 적힌 것이고, 0078 과 같이 봉인한다(소금 친 dataHash · 대조용 원본).
--    사람은 가명(사람마다 비밀값), 받는 사람은 켠 사람만 지갑. 걸음 수 · 위치는 올리지 않는다.
--  · 보내는 시각 늦추기(10 ~ 60분)도 0078 그대로다 — 칸 시각은 slot 칸에 남는다.
--  · 주인 없는 일은 거둬지므로(attester_jobs_claim) 크루 스냅숏은 크루장, 전체 스냅숏은 어테스터 계정 이름으로
--    줄 세운다. 어테스터 계정이 없으면 전체 스냅숏은 건너뛴다.
--  · 앱의 내 체인 기록(my_chain_activity)에는 스냅숏을 보이지 않는다 — 6시간마다 쌓여 활동 기록을 가린다.
--
--  운영 값(economy_settings):
--    snapshot_enabled  false 면 새 칸을 줄 세우지 않는다(이미 선 것은 나간다)
--    snapshot_hours    칸 길이(시간) — 24 의 약수(1 · 2 · 3 · 4 · 6 · 8 · 12 · 24)
--  막는 것: 전체 하루 상한(chain_jobs_global_daily)은 그대로다 — 계정 × 하루 칸 수가 상한을 넘으면 다음 날로 밀린다.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('snapshot_enabled', 'true'::jsonb),
  ('snapshot_hours', '6'::jsonb)
on conflict (key) do nothing;

-- ══════════════════════════════════════════════════════════════════
-- 종류 — 0078 목록 + 스냅숏 셋
-- ══════════════════════════════════════════════════════════════════
alter table public.chain_jobs drop constraint if exists chain_jobs_kind_check;
alter table public.chain_jobs add constraint chain_jobs_kind_check check (kind in (
  'RUN_PROOF', 'COURSE_RUN', 'BADGE', 'VAULT_MINT', 'STATS_SYNC',
  'SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE', 'DRAW_SEED', 'DRAW_REVEAL',
  'RUN_SETTLE', 'SHOE_WEAR', 'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'PARTY_START', 'DAILY_STEPS', 'CHECK_IN',
  'SNAP_USER', 'SNAP_CREW', 'SNAP_ALL'
)) not valid;

create or replace function economy.snapshot_kinds() returns text[]
  language sql immutable as $$ select array['SNAP_USER', 'SNAP_CREW', 'SNAP_ALL'] $$;

-- 봉인 · 가져가기 · 관리자 통계가 이 목록으로 새 기록을 알아본다(0078)
create or replace function economy.activity_kinds() returns text[]
  language sql immutable as $$
  select array['SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE', 'DRAW_SEED', 'DRAW_REVEAL',
               'RUN_SETTLE', 'SHOE_WEAR', 'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'PARTY_START', 'DAILY_STEPS', 'CHECK_IN',
               'SNAP_USER', 'SNAP_CREW', 'SNAP_ALL']
$$;

-- ══════════════════════════════════════════════════════════════════
-- 칸 — 한국 시간으로 자른 칸의 시작 = YYYYMMDDHH (EAS 에 uint32 로)
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.snapshot_slots (
  slot int primary key,
  users int not null default 0,
  crews int not null default 0,
  overall int not null default 0,
  created_at timestamptz not null default now()
);
revoke all on economy.snapshot_slots from public;
do $$ begin
  execute 'revoke all on economy.snapshot_slots from anon, authenticated';
exception when undefined_object then null; end $$;

create or replace function economy.snapshot_slot(p_at timestamptz) returns int
language sql stable security definer set search_path = public, economy as $$
  with k as (
    select (p_at at time zone 'Asia/Seoul') as t,
           case when coalesce(economy.setting_num('snapshot_hours'), 6)::int in (1, 2, 3, 4, 6, 8, 12, 24)
                then coalesce(economy.setting_num('snapshot_hours'), 6)::int else 6 end as h
  )
  select to_char(k.t, 'YYYYMMDD')::int * 100 + (extract(hour from k.t)::int / k.h) * k.h from k
$$;
revoke all on function economy.snapshot_slot(timestamptz) from public;

-- ══════════════════════════════════════════════════════════════════
-- 줄 세우기 — 워커가 2분마다 부른다. 이번 칸을 처음 볼 때만 줄 세운다
-- ══════════════════════════════════════════════════════════════════
create or replace function public.attester_snapshot_tick()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_slot int;
  v_users int := 0;
  v_crews int := 0;
  v_all int := 0;
  v_owner uuid;
begin
  perform economy.attester_guard();
  if coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false) or not economy.chain_jobs_on()
     or not coalesce((economy.setting('snapshot_enabled') #>> '{}')::boolean, false) then
    return jsonb_build_object('skipped', 'off');
  end if;
  v_slot := economy.snapshot_slot(now());
  insert into economy.snapshot_slots (slot) values (v_slot) on conflict (slot) do nothing;
  if not found then
    return jsonb_build_object('slot', v_slot, 'already', true);
  end if;

  insert into public.chain_jobs (kind, ref, user_id, args)
  select 'SNAP_USER', 'snap:user:' || p.id || ':' || v_slot, p.id, jsonb_build_object('slot', v_slot)
    from public.profiles p
   where exists (select 1 from auth.users u where u.id = p.id)
  on conflict (kind, ref) do nothing;
  get diagnostics v_users = row_count;

  insert into public.chain_jobs (kind, ref, user_id, args)
  select 'SNAP_CREW', 'snap:crew:' || c.id || ':' || v_slot, c.owner_id,
         jsonb_build_object('slot', v_slot, 'crew_id', c.id)
    from public.crews c
  on conflict (kind, ref) do nothing;
  get diagnostics v_crews = row_count;

  -- 전체는 사람에 묶이지 않는다 — 어테스터 계정 이름으로(없으면 건너뛴다)
  select u.id into v_owner from auth.users u
   where u.id::text = (economy.setting('attester_user_id') #>> '{}');
  if v_owner is not null then
    insert into public.chain_jobs (kind, ref, user_id, args)
    values ('SNAP_ALL', 'snap:all:' || v_slot, v_owner, jsonb_build_object('slot', v_slot))
    on conflict (kind, ref) do nothing;
    get diagnostics v_all = row_count;
  end if;

  update economy.snapshot_slots set users = v_users, crews = v_crews, overall = v_all where slot = v_slot;
  return jsonb_build_object('slot', v_slot, 'users', v_users, 'crews', v_crews, 'all', v_all);
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 보낼 재료 — 키는 워커 스키마의 칸 이름 그대로(attester/src/eas.js ACTIVITY_SCHEMAS).
-- '_audit' 는 체인에 올리지 않고 대조용 원본에만. null 이면 거둔다
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.snapshot_payload(j public.chain_jobs) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_slot int := (j.args ->> 'slot')::int;
  v_zero text := '0x0000000000000000000000000000000000000000';
  p public.profiles;
  c public.crews;
  v_balance numeric;
  r record;
begin
  if v_slot is null then
    return null;
  end if;
  if j.kind = 'SNAP_USER' then
    select * into p from public.profiles x where x.id = j.user_id;
    if not found then
      return null;
    end if;
    v_balance := greatest(economy.balance_of(j.user_id), 0);
    return jsonb_build_object(
      'recipient', economy.chain_recipient(j.user_id),
      'runner', economy.person_pseudonym(j.user_id),
      'slot', v_slot,
      'totalM', round(greatest(p.lifetime_km, 0) * 1000)::bigint::text,
      'runs', (select count(*) from public.walk_sessions w where w.user_id = j.user_id and w.verdict <> 'VOID')::int,
      'pointsMilli', round(v_balance * 1000)::bigint::text,
      'shoes', (select count(*) from public.market_sneakers s where s.owner_id = j.user_id)::int,
      'crews', (select count(*) from public.crew_members m where m.user_id = j.user_id)::int,
      '_audit', jsonb_build_object('lifetime_km', p.lifetime_km, 'balance', v_balance));
  elsif j.kind = 'SNAP_CREW' then
    select * into c from public.crews x where x.id = (j.args ->> 'crew_id')::uuid;
    if not found then
      return null;
    end if;
    return jsonb_build_object(
      'recipient', v_zero,
      'crew', economy.pseudonym('crew', c.id::text),
      'slot', v_slot,
      'members', (select count(*) from public.crew_members m where m.crew_id = c.id)::int,
      'level', coalesce(c.level, 0),
      'runM', round(greatest(coalesce(c.run_km, 0), 0) * 1000)::bigint::text,
      '_audit', jsonb_build_object('run_km', c.run_km));
  elsif j.kind = 'SNAP_ALL' then
    select (select count(*) from public.profiles)::int as accounts,
           (select count(*) from public.crews)::int as crews,
           (select count(*) from public.walk_sessions w where w.verdict <> 'VOID') as runs,
           (select coalesce(sum(greatest(x.lifetime_km, 0)), 0) from public.profiles x) as km,
           (select count(*) from public.market_sneakers) as shoes,
           (select coalesce(sum(l.amount), 0) from public.sup_ledger l) as points
      into r;
    return jsonb_build_object(
      'recipient', v_zero,
      'slot', v_slot,
      'accounts', r.accounts,
      'crews', r.crews,
      'runs', r.runs::bigint::text,
      'totalM', round(r.km * 1000)::bigint::text,
      'shoes', r.shoes::bigint::text,
      'pointsMilli', round(greatest(r.points, 0) * 1000)::bigint::text,
      '_audit', jsonb_build_object('km', r.km, 'points', r.points));
  end if;
  return null;
end $$;
revoke all on function economy.snapshot_payload(public.chain_jobs) from public;

-- 재료 봉인 — 0078 그대로, 스냅숏은 snapshot_payload 로 만든다
create or replace function economy.activity_seal(j public.chain_jobs) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  au economy.chain_audit;
  p jsonb;
  v_salt bytea;
  v_snap text;
  v_hash text;
begin
  select * into au from economy.chain_audit x where x.job_id = j.id;
  if found then
    return au.payload;
  end if;
  if j.kind = any (economy.snapshot_kinds()) then
    p := economy.snapshot_payload(j);
  else
    p := economy.activity_payload(j);
  end if;
  if p is null then
    return null;
  end if;
  v_salt := economy.random_bytes32();
  v_snap := 'stepup-audit-v1|' || j.kind || '|' || j.ref || '|' || (p - 'recipient')::text;
  v_hash := '0x' || encode(sha256(v_salt || convert_to(v_snap, 'UTF8')), 'hex');
  p := (p - '_audit') || jsonb_build_object('dataHash', v_hash);
  insert into economy.chain_audit (job_id, user_id, recipe, salt, snapshot, data_hash, payload)
  values (j.id, j.user_id, 'stepup-audit-v1', v_salt, v_snap, v_hash, p);
  return p;
end $$;
revoke all on function economy.activity_seal(public.chain_jobs) from public;

-- ══════════════════════════════════════════════════════════════════
-- 내 체인 기록 — 0044 그대로, 스냅숏은 빼고(6시간마다 쌓여 활동 기록을 가린다)
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
     and c.kind <> all (economy.snapshot_kinds())
   order by c.id desc
   limit greatest(1, least(coalesce(p_limit, 50), 200));
end $$;
revoke all on function public.my_chain_activity(int) from public, anon;
grant execute on function public.my_chain_activity(int) to authenticated;

-- 어테스터 함수 권한 — 0044 와 같은 규칙(첫 줄의 attester_guard 가 막는다)
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
