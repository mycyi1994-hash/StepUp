-- 지갑과 체인 — 연결 · 꺼내기 · 넣기 · 지갑 보너스 · 정지 스위치 · 정산 대조.
--
-- 흐름 (서명과 체인 제출은 어테스터가 한다 — attester/):
--
--   사용자  ──요청──▶  서버: 조건 검사 → 작업(chain_ops) 예약 · 잔고/신발 잠금
--   어테스터 ──서명 전 확인──▶ 서버: 예약된 작업만 서명 재료를 준다 → SIGNED
--   어테스터 ──제출──▶ 체인 → SUBMITTED
--   어테스터 ──확정 블록을 기다린 뒤 이벤트──▶ 서버: CONFIRMED (한 이벤트는 한 번만)
--   만료: 서명 유효 시간 + 안전 마진이 지나고, 체인에서 그 작업 번호가 안 쓰였음을
--         어테스터가 확인했을 때만 되돌린다 → EXPIRED
--
-- 사고를 막는 겹:
--   - 지갑 1개 = 계정 1개, 한 번 쓴 지갑은 다른 계정에 못 붙는다
--   - 지갑을 붙이거나 바꾸면 72시간 꺼내기 금지 (계정 탈취 대비), 알림을 보낸다
--   - 꺼내기는 2단계 인증(aal2) · 가입 7일 · 누적 20km 이후
--   - 사람별 · 전체 하루 상한, 전체 하루 NFT 발행 상한
--   - 정지 스위치(chain_paused) — 어테스터나 관리자가 켠다
--   - 작업 번호마다 체인에서 한 번만 성공한다(컨트랙트가 막는다)

-- ══════════════════════════════════════════════════════════════════
-- 어테스터 역할
-- ══════════════════════════════════════════════════════════════════
--
-- 어테스터는 service_role 을 쓰지 않는다. 이 역할로 서명된 JWT 하나만 들고,
-- 아래 attester_* 함수만 부를 수 있다. 표는 하나도 직접 못 읽는다.
do $$ begin
  create role stepup_attester nologin;
exception when duplicate_object then null; end $$;

do $$ begin
  -- Supabase 의 PostgREST 는 authenticator 로 접속해 JWT 의 role 로 바꾼다.
  if exists (select 1 from pg_roles where rolname = 'authenticator') then
    execute 'grant stepup_attester to authenticator';
  end if;
end $$;

grant usage on schema public to stepup_attester;

-- 2단계 인증을 거친 로그인인가 (Supabase JWT 의 aal)
create or replace function economy.mfa_ok() returns boolean
  language sql stable as $$
  select coalesce(auth.jwt() ->> 'aal', 'aal1') = 'aal2'
$$;

-- 사용자 번호 ↔ 체인의 bytes32. uuid 16바이트를 앞을 0 으로 채워 32바이트로.
create or replace function economy.account_ref(p_user uuid) returns text
  language sql immutable as $$
  select '0x' || lpad(replace(p_user::text, '-', ''), 64, '0')
$$;

create or replace function economy.account_from_ref(p_ref text) returns uuid
  language plpgsql immutable as $$
declare v text := lower(regexp_replace(coalesce(p_ref, ''), '^0x', ''));
begin
  if v !~ '^0{32}[0-9a-f]{32}$' then
    return null;
  end if;
  return (substr(v, 33, 8) || '-' || substr(v, 41, 4) || '-' || substr(v, 45, 4) || '-'
          || substr(v, 49, 4) || '-' || substr(v, 53, 12))::uuid;
end $$;

-- 작업 번호 ↔ bytes32. 컨트랙트가 이 값으로 "한 번만"을 지킨다.
create or replace function economy.op_ref(p_op uuid) returns text
  language sql immutable as $$
  select '0x' || lpad(replace(p_op::text, '-', ''), 64, '0')
$$;

-- ══════════════════════════════════════════════════════════════════
-- 지갑
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.wallet_links (
  user_id uuid primary key references auth.users on delete cascade,
  address text not null unique check (address ~ '^0x[0-9a-f]{40}$'),
  linked_at timestamptz not null default now(),
  -- 붙이거나 바꾼 마지막 시각. 여기서 72시간은 꺼내지 못한다.
  changed_at timestamptz not null default now()
);

-- 한 번 붙은 지갑은 영원히 그 계정 것. 계정을 여러 개 만들어 한 지갑으로 몰아
-- 보너스를 받거나 상한을 나눠 쓰지 못하게 한다.
create table if not exists public.wallet_history (
  address text primary key,
  user_id uuid not null references auth.users on delete cascade,
  first_linked_at timestamptz not null default now()
);

create table if not exists public.wallet_link_nonces (
  user_id uuid primary key references auth.users on delete cascade,
  nonce text not null,
  created_at timestamptz not null default now()
);

alter table public.wallet_links enable row level security;
alter table public.wallet_history enable row level security;
alter table public.wallet_link_nonces enable row level security;
revoke all on public.wallet_links, public.wallet_history, public.wallet_link_nonces from anon, authenticated;
drop policy if exists wallet_links_select_own on public.wallet_links;
create policy wallet_links_select_own on public.wallet_links
  for select using ((select auth.uid()) = user_id);
grant select on public.wallet_links to authenticated;

-- 지갑이 서명할 문장. 어테스터가 이 문장의 서명을 검증한 뒤 attester_wallet_link 를 부른다.
create or replace function public.wallet_link_challenge()
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_nonce text := replace(gen_random_uuid()::text, '-', '');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  insert into public.wallet_link_nonces (user_id, nonce) values (v_user, v_nonce)
  on conflict (user_id) do update set nonce = excluded.nonce, created_at = now();
  return economy.wallet_link_message(v_user, v_nonce);
end $$;

create or replace function economy.wallet_link_message(p_user uuid, p_nonce text) returns text
  language sql immutable as $$
  select format(E'StepUp 지갑 연결\n계정: %s\n확인 번호: %s\n\n이 서명은 거래가 아니며 수수료가 들지 않습니다.', p_user, p_nonce)
$$;

revoke all on function public.wallet_link_challenge() from public, anon;
grant execute on function public.wallet_link_challenge() to authenticated;

-- 보안 알림 — 알림 설정과 상관없이 보낸다. 모르는 사이 지갑이 바뀌면 알아야 한다.
create or replace function economy.security_notice(p_user uuid, p_kind text, p_args jsonb)
returns void language plpgsql security definer set search_path = public as $$
begin
  if exists (select 1 from public.push_tokens t where t.user_id = p_user) then
    insert into public.push_outbox (user_id, kind, args, link)
    values (p_user, p_kind, coalesce(p_args, '{}'::jsonb), 'stepup://wallet');
  end if;
end $$;
revoke all on function economy.security_notice(uuid, text, jsonb) from public;

/*
 * 어테스터 — 지갑 서명을 검증한 뒤 부른다.
 * 처음 붙이는 지갑이면 보너스 뽑기 10회(Genesis 1)를 준다.
 */
create or replace function public.attester_wallet_link(p_user uuid, p_address text, p_nonce text)
returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_addr text := lower(p_address);
  v_first boolean;
begin
  if v_addr !~ '^0x[0-9a-f]{40}$' then
    raise exception '지갑 주소가 올바르지 않습니다' using errcode = '22023';
  end if;
  if not exists (select 1 from public.wallet_link_nonces
                  where user_id = p_user and nonce = p_nonce
                    and created_at > now() - interval '10 minutes') then
    raise exception '확인 번호가 맞지 않거나 만료되었습니다' using errcode = '22023';
  end if;
  delete from public.wallet_link_nonces where user_id = p_user;

  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));

  if exists (select 1 from public.wallet_history where address = v_addr and user_id <> p_user) then
    raise exception '다른 계정에 연결된 적 있는 지갑입니다' using errcode = '23505';
  end if;

  v_first := not exists (select 1 from public.wallet_history where user_id = p_user);

  insert into public.wallet_history (address, user_id) values (v_addr, p_user)
  on conflict (address) do nothing;

  insert into public.wallet_links (user_id, address) values (p_user, v_addr)
  on conflict (user_id) do update
    set address = excluded.address,
        changed_at = case when public.wallet_links.address = excluded.address
                          then public.wallet_links.changed_at else now() end;

  if v_first then
    insert into public.draw_grants (user_id, kind, granted, genesis_granted)
    values (p_user, 'BONUS', 10, 1)
    on conflict (user_id, kind) do nothing;
  end if;

  perform economy.security_notice(p_user, 'WALLET_LINKED',
    jsonb_build_object('address', left(v_addr, 6) || '…' || right(v_addr, 4)));
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 체인 작업
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.chain_ops (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('SUP_WITHDRAW', 'SNEAKER_WITHDRAW', 'BONUS_MINT')),
  status text not null default 'RESERVED'
    check (status in ('RESERVED', 'SIGNED', 'SUBMITTED', 'CONFIRMED', 'EXPIRED')),
  wallet text not null,
  amount numeric(20, 4),
  sneaker_id bigint references public.market_sneakers on delete set null,
  deadline timestamptz not null,
  tx_hash text,
  block_number bigint,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  check ((kind = 'SUP_WITHDRAW') = (amount is not null)),
  check ((kind <> 'SUP_WITHDRAW') = (sneaker_id is not null))
);

create index if not exists chain_ops_user_recent on public.chain_ops (user_id, created_at desc);
create index if not exists chain_ops_open on public.chain_ops (status, deadline)
  where status in ('RESERVED', 'SIGNED', 'SUBMITTED');

alter table public.chain_ops enable row level security;
revoke all on public.chain_ops from anon, authenticated;
drop policy if exists chain_ops_select_own on public.chain_ops;
create policy chain_ops_select_own on public.chain_ops
  for select using ((select auth.uid()) = user_id);
grant select on public.chain_ops to authenticated;

-- 체인에서 본 이벤트. (거래, 로그 번호) 하나는 한 번만 처리한다.
create table if not exists public.chain_events (
  tx_hash text not null,
  log_index int not null,
  block_number bigint not null,
  kind text not null,
  data jsonb not null,
  processed_at timestamptz not null default now(),
  primary key (tx_hash, log_index)
);

alter table public.chain_events enable row level security;
revoke all on public.chain_events from anon, authenticated;

-- 꺼내기 공통 조건. 걸리면 이유와 함께 거절한다.
create or replace function economy.withdraw_gate(p_user uuid, p_need_mfa boolean default true)
returns text
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_wallet public.wallet_links;
  v_profile public.profiles;
begin
  if (economy.setting('chain_paused') #>> '{}')::boolean then
    raise exception '지금은 체인 작업을 잠시 멈췄습니다' using errcode = '55000';
  end if;

  select * into v_wallet from public.wallet_links where user_id = p_user;
  if not found then
    raise exception '먼저 지갑을 연결해 주세요' using errcode = '23514';
  end if;

  if p_need_mfa then
    if not economy.mfa_ok() then
      raise exception '2단계 인증이 필요합니다' using errcode = '42501';
    end if;
    if v_wallet.changed_at > now() - make_interval(hours => economy.setting_num('wallet_cooldown_hours')::int) then
      raise exception '지갑을 연결하거나 바꾼 뒤 %시간은 꺼낼 수 없습니다',
        economy.setting_num('wallet_cooldown_hours') using errcode = '23514';
    end if;
    select * into v_profile from public.profiles where id = p_user;
    if v_profile.created_at > now() - make_interval(days => economy.setting_num('withdraw_min_account_days')::int) then
      raise exception '가입 %일이 지나야 꺼낼 수 있습니다',
        economy.setting_num('withdraw_min_account_days') using errcode = '23514';
    end if;
    if v_profile.lifetime_km < economy.setting_num('withdraw_min_km') then
      raise exception '누적 %km 를 달려야 꺼낼 수 있습니다',
        economy.setting_num('withdraw_min_km') using errcode = '23514';
    end if;
  end if;

  return v_wallet.address;
end $$;
revoke all on function economy.withdraw_gate(uuid, boolean) from public;

create or replace function economy.today_start() returns timestamptz
  language sql stable as $$ select economy.game_day_start(economy.game_day(now())) $$;

-- 오늘(한국) 꺼낸 SUP — 만료된 것은 빼고
create or replace function economy.withdrawn_today(p_user uuid default null) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(o.amount), 0) from public.chain_ops o
   where o.kind = 'SUP_WITHDRAW' and o.status <> 'EXPIRED'
     and o.created_at >= economy.today_start()
     and (p_user is null or o.user_id = p_user)
$$;
revoke all on function economy.withdrawn_today(uuid) from public;

create or replace function economy.mints_today() returns int
language sql stable security definer set search_path = public, economy as $$
  select count(*)::int from public.chain_ops o
   where o.kind in ('SNEAKER_WITHDRAW', 'BONUS_MINT') and o.status <> 'EXPIRED'
     and o.created_at >= economy.today_start()
$$;
revoke all on function economy.mints_today() from public;

create or replace function economy.op_deadline() returns timestamptz
  language sql stable as $$
  select now() + make_interval(secs => economy.setting_num('op_deadline_sec')::int)
$$;

-- ── SUP 꺼내기 ──
create or replace function public.sup_withdraw_request(p_amount numeric)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_amount numeric := round(p_amount, 4);
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_amount is null or v_amount < 1 then
    raise exception '1 SUP 이상부터 꺼낼 수 있습니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 전체 상한은 모두가 함께 쓰므로 전체 잠금 아래에서 센다
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  if v_amount > economy.setting_num('withdraw_user_daily_sup') - economy.withdrawn_today(v_user) then
    raise exception '오늘 꺼낼 수 있는 양을 넘었습니다' using errcode = '23514';
  end if;
  if v_amount > economy.setting_num('withdraw_global_daily_sup') - economy.withdrawn_today(null) then
    raise exception '오늘 전체 꺼내기 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  -- 예약하는 순간 잔고에서 뺀다. 서명을 기다리는 동안 같은 돈을 앱에서 쓰지 못하게.
  perform economy.ledger_apply(v_user, 'CHAIN_WITHDRAW', -v_amount, '체인으로 꺼내기', 'op:' || v_op);

  insert into public.chain_ops (id, user_id, kind, wallet, amount, deadline)
  values (v_op, v_user, 'SUP_WITHDRAW', v_wallet, v_amount, economy.op_deadline());
  return v_op;
end $$;

-- ── 신발 꺼내기 ──
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
  if v.token_id is null and economy.mints_today() >= economy.setting_num('mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  update public.market_sneakers
     set chain_state = 'WITHDRAWING', equipped = false, updated_at = now()
   where id = p_sneaker_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'SNEAKER_WITHDRAW', v_wallet, p_sneaker_id, economy.op_deadline());
  return v_op;
end $$;

-- ── 지갑 보너스 뽑기 (체인에서 발행) ──
-- 첫 번째가 Genesis(희귀 이상 확정)다. 꺼내기가 아니라 받는 것이라 2단계 인증과
-- 72시간 대기는 묻지 않는다 — 받은 NFT 는 50km 를 달리기 전에는 옮길 수 없다.
create or replace function public.bonus_draw_request()
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_genesis boolean;
  v_id bigint;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, false);

  if economy.mints_today() >= economy.setting_num('mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g where g.user_id = v_user and g.kind = 'BONUS';

  update public.draw_grants
     set used = used + 1,
         genesis_used = genesis_used + case when v_genesis then 1 else 0 end
   where user_id = v_user and kind = 'BONUS' and used < granted;
  if not found then
    raise exception '보너스 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' end, coalesce(v_genesis, false));
  update public.market_sneakers set chain_state = 'WITHDRAWING' where id = v_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'BONUS_MINT', v_wallet, v_id, economy.op_deadline());
  return v_op;
end $$;

revoke all on function public.sup_withdraw_request(numeric) from public, anon;
revoke all on function public.sneaker_withdraw_request(bigint) from public, anon;
revoke all on function public.bonus_draw_request() from public, anon;
grant execute on function public.sup_withdraw_request(numeric) to authenticated;
grant execute on function public.sneaker_withdraw_request(bigint) to authenticated;
grant execute on function public.bonus_draw_request() to authenticated;

-- 내 지갑 · 꺼내기 상태
create or replace function public.my_wallet()
returns table (
  address text,
  linked_at timestamptz,
  withdraw_open_at timestamptz,
  mfa boolean,
  withdrawn_today numeric,
  withdraw_daily_limit numeric,
  bonus_left int,
  genesis_left int,
  paused boolean
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select w.address, w.linked_at,
         w.changed_at + make_interval(hours => economy.setting_num('wallet_cooldown_hours')::int),
         economy.mfa_ok(),
         economy.withdrawn_today(v_user),
         economy.setting_num('withdraw_user_daily_sup'),
         coalesce((select g.granted - g.used from public.draw_grants g
                    where g.user_id = v_user and g.kind = 'BONUS'), 0),
         coalesce((select g.genesis_granted - g.genesis_used from public.draw_grants g
                    where g.user_id = v_user and g.kind = 'BONUS'), 0),
         (economy.setting('chain_paused') #>> '{}')::boolean
    from (select 1) one
    left join public.wallet_links w on w.user_id = v_user;
end $$;

revoke all on function public.my_wallet() from public, anon;
grant execute on function public.my_wallet() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 어테스터 함수
-- ══════════════════════════════════════════════════════════════════

/*
 * 서명 재료. RESERVED(또는 이미 SIGNED)이고 유효 시간이 남은 작업만 준다.
 * 같은 작업을 다시 물어도 같은 재료가 나온다 — 체인은 작업 번호로 한 번만 받는다.
 */
create or replace function public.attester_op_payload(p_op uuid)
returns table (
  op_id uuid,
  op_ref text,
  kind text,
  wallet text,
  account_ref text,
  amount numeric,
  deadline_unix bigint,
  run_day date,
  sneaker_id bigint,
  token_id numeric,
  faction text,
  rarity text,
  variant int,
  level int,
  efficiency_bps int,
  comfort_bps int,
  durability numeric,
  genesis_no int,
  transfer_locked boolean
)
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops o where o.id = p_op for update;
  if not found then
    raise exception '없는 작업입니다' using errcode = '22023';
  end if;
  if v.status not in ('RESERVED', 'SIGNED') or v.deadline <= now() then
    raise exception '서명할 수 없는 작업입니다 (%)', v.status using errcode = '22023';
  end if;
  if (economy.setting('chain_paused') #>> '{}')::boolean then
    raise exception '지금은 체인 작업을 잠시 멈췄습니다' using errcode = '55000';
  end if;

  update public.chain_ops o set status = 'SIGNED', updated_at = now() where o.id = p_op;

  return query
  select v.id, economy.op_ref(v.id), v.kind, v.wallet, economy.account_ref(v.user_id),
         v.amount, extract(epoch from v.deadline)::bigint, economy.game_day(v.created_at),
         s.id, s.token_id, s.faction, s.rarity, s.variant, s.level,
         s.efficiency_bps, s.comfort_bps, s.durability_pts, s.genesis_no,
         -- 무료로 받은 신발은 잠금 거리를 채우기 전에는 체인에서도 못 옮긴다
         coalesce(s.km_run < s.lock_km, false)
    from (select 1) one
    left join public.market_sneakers s on s.id = v.sneaker_id;
end $$;

create or replace function public.attester_op_submitted(p_op uuid, p_tx text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  if p_tx !~ '^0x[0-9a-fA-F]{64}$' then
    raise exception '거래 해시가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.chain_ops set status = 'SUBMITTED', tx_hash = lower(p_tx), updated_at = now()
   where id = p_op and status in ('SIGNED', 'SUBMITTED');
end $$;

-- 작업을 확정한다. 이미 확정이면 아무것도 안 한다.
create or replace function economy.op_confirm(p_op uuid, p_tx text, p_block bigint, p_token numeric)
returns void language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops where id = p_op for update;
  if not found or v.status = 'CONFIRMED' then
    return;
  end if;
  if v.status = 'EXPIRED' then
    -- 되돌린 뒤에 체인에서 성공했다 — 일어나면 안 되는 일(만료 마진 · 체인 확인이
    -- 막는다). 두 번 받는 것을 막기 위해 되돌린 것을 다시 거두고 경보를 남긴다.
    if v.kind = 'SUP_WITHDRAW' then
      begin
        perform economy.ledger_apply(v.user_id, 'CHAIN_WITHDRAW', -v.amount,
          '만료 뒤 체인 확정 — 되돌린 금액 회수', 'reclaim:' || v.id);
      exception when others then
        null;  -- 잔고가 모자라면 여기서는 못 거둔다. 아래 경보로 사람이 본다.
      end;
    end if;
    perform public.admin_log('chain_late_confirm', v.id::text, jsonb_build_object('tx', p_tx));
  end if;

  update public.chain_ops
     set status = 'CONFIRMED', tx_hash = coalesce(lower(p_tx), tx_hash), block_number = p_block, updated_at = now()
   where id = p_op;

  -- 꺼내는 중인 신발만 체인으로 옮긴다. 이미 다시 넣어 앱에 돌아온 신발을 늦게 온
  -- 이벤트가 체인으로 되돌려 놓지 않게. (어테스터는 이벤트를 블록·로그 순서대로 넘긴다.)
  if v.kind in ('SNEAKER_WITHDRAW', 'BONUS_MINT') then
    update public.market_sneakers
       set chain_state = 'ON_CHAIN', equipped = false,
           token_id = coalesce(p_token, token_id), updated_at = now()
     where id = v.sneaker_id and chain_state in ('WITHDRAWING', 'APP');
  end if;
end $$;
revoke all on function economy.op_confirm(uuid, text, bigint, numeric) from public;

/*
 * 확정된 체인 이벤트 하나. 어테스터가 확정 블록까지 기다린 뒤, 블록 번호 · 로그 번호
 * 순서대로 부른다(같은 신발의 꺼내기 이벤트가 넣기 이벤트보다 먼저 와야 한다).
 *
 *   SUP_CLAIMED        {"op": opRef}
 *   SNEAKER_RELEASED   {"op": opRef, "tokenId": "123"}
 *   SUP_DEPOSITED      {"account": accountRef, "amount": "12.5"}
 *   SNEAKER_DEPOSITED  {"account": accountRef, "tokenId": "123"}
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
begin
  insert into public.chain_events (tx_hash, log_index, block_number, kind, data)
  values (v_tx, p_log, p_block, p_kind, p_data)
  on conflict do nothing;
  if not found then
    return 'DUPLICATE';
  end if;

  if p_kind in ('SUP_CLAIMED', 'SNEAKER_RELEASED') then
    v_op := economy.account_from_ref(p_data ->> 'op');
    v_token := nullif(p_data ->> 'tokenId', '')::numeric;
    if v_op is null or not exists (select 1 from public.chain_ops where id = v_op) then
      perform public.admin_log('chain_unknown_op', v_tx, p_data);
      return 'UNKNOWN_OP';
    end if;
    perform economy.op_confirm(v_op, v_tx, p_block, v_token);
    return 'CONFIRMED';

  elsif p_kind = 'SUP_DEPOSITED' then
    v_user := economy.account_from_ref(p_data ->> 'account');
    v_amount := round((p_data ->> 'amount')::numeric, 4);
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
       or not exists (select 1 from public.market_sneakers where token_id = v_token) then
      perform public.admin_log('chain_orphan_deposit', v_tx, p_data);
      return 'ORPHAN';
    end if;
    -- 넣은 사람이 새 주인이다(체인에서 샀을 수 있다). 스탯은 꺼낼 때 서버가 적은 값 그대로다.
    update public.market_sneakers
       set owner_id = v_user, chain_state = 'APP', status = 'OWNED', equipped = false, updated_at = now()
     where token_id = v_token;
    return 'CREDITED';
  end if;

  perform public.admin_log('chain_unknown_event', v_tx, jsonb_build_object('kind', p_kind, 'data', p_data));
  return 'IGNORED';
end $$;

-- 되돌릴 차례가 된 작업 — 서명 유효 시간 + 안전 마진이 지난 것
create or replace function public.attester_due_ops()
returns table (op_id uuid, op_ref text, status text, kind text, deadline timestamptz, tx_hash text)
language sql stable security definer set search_path = public, economy as $$
  select o.id, economy.op_ref(o.id), o.status, o.kind, o.deadline, o.tx_hash
    from public.chain_ops o
   where o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')
     and o.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) < now()
   order by o.deadline
   limit 200
$$;

/*
 * 만료. 어테스터가 체인에서 이 작업 번호가 쓰였는지 확인하고 부른다.
 *   p_used_on_chain = true   → 이벤트를 놓친 것. 확정으로 처리한다.
 *   p_used_on_chain = false  → 되돌린다(잔고 환불 · 신발 앱으로).
 * 서명한 적 없는 작업(RESERVED)은 체인에 있을 수 없으므로 마진 없이도 되돌린다.
 */
create or replace function public.attester_op_expire(p_op uuid, p_used_on_chain boolean, p_token numeric default null)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  select * into v from public.chain_ops where id = p_op for update;
  if not found then
    raise exception '없는 작업입니다' using errcode = '22023';
  end if;
  if v.status in ('CONFIRMED', 'EXPIRED') then
    return v.status;
  end if;
  if v.status <> 'RESERVED'
     and v.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now() then
    raise exception '아직 만료 마진이 지나지 않았습니다' using errcode = '55000';
  end if;
  if v.status = 'RESERVED' and v.deadline >= now() then
    raise exception '아직 유효한 작업입니다' using errcode = '55000';
  end if;

  if p_used_on_chain then
    perform economy.op_confirm(p_op, v.tx_hash, v.block_number, p_token);
    return 'CONFIRMED';
  end if;

  update public.chain_ops set status = 'EXPIRED', updated_at = now() where id = p_op;

  if v.kind = 'SUP_WITHDRAW' then
    perform economy.ledger_apply(v.user_id, 'CHAIN_REFUND', v.amount, '꺼내기 만료 — 되돌림', 'refund:' || v.id);
  else
    -- 신발은 앱으로 돌아온다. 보너스 뽑기로 만든 신발도 사라지지 않고 앱에 남는다.
    update public.market_sneakers
       set chain_state = 'APP', updated_at = now()
     where id = v.sneaker_id and chain_state = 'WITHDRAWING';
  end if;
  return 'EXPIRED';
end $$;

-- 이상 징후를 본 어테스터가 스스로 멈춘다. 다시 켜는 것은 관리자만(admin_economy_set).
create or replace function public.attester_pause(p_reason text)
returns void
language plpgsql security definer set search_path = public as $$
begin
  update public.economy_settings set value = 'true'::jsonb, updated_at = now() where key = 'chain_paused';
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'chain_pause', 'attester', jsonb_build_object('reason', p_reason));
end $$;

-- 정산 대조 — 서버 장부의 합. 어테스터가 체인의 발행·입금 합과 비교한다.
create or replace function public.attester_ledger_totals()
returns table (
  sup_withdrawn_confirmed numeric,
  sup_withdraw_pending numeric,
  sup_deposited numeric,
  sneakers_on_chain bigint,
  sneakers_pending bigint
)
language sql stable security definer set search_path = public as $$
  select
    coalesce((select sum(amount) from public.chain_ops where kind = 'SUP_WITHDRAW' and status = 'CONFIRMED'), 0),
    coalesce((select sum(amount) from public.chain_ops
               where kind = 'SUP_WITHDRAW' and status in ('RESERVED', 'SIGNED', 'SUBMITTED')), 0),
    coalesce((select sum(amount) from public.sup_ledger where kind = 'CHAIN_DEPOSIT'), 0),
    (select count(*) from public.market_sneakers where chain_state = 'ON_CHAIN'),
    (select count(*) from public.market_sneakers where chain_state in ('WITHDRAWING', 'DEPOSITING'))
$$;

-- 어테스터 함수는 어테스터 역할만. 앱 권한(anon·authenticated)으로는 부를 수 없다.
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig from pg_proc p
     where p.pronamespace = 'public'::regnamespace and p.proname like 'attester\_%'
  loop
    execute format('revoke all on function %s from public, anon, authenticated', r.sig);
    execute format('grant execute on function %s to stepup_attester', r.sig);
  end loop;
end $$;
