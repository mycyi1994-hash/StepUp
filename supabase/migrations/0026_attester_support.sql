-- 어테스터 받침 — 체인을 어디까지 읽었는지, 작업을 부른 사람이 주인인지.
--
--   chain_cursors            컨트랙트마다 마지막으로 처리한 블록. 워커는 저장소가 없다.
--   attester_op_payload      사용자가 부른 작업이 그 사람 것인지 서버가 한 번 더 본다.
--                            남의 작업 번호를 넣어 SIGNED 로 바꿔 놓는 장난을 막는다.

create table if not exists public.chain_cursors (
  name text primary key,
  block bigint not null check (block >= 0),
  updated_at timestamptz not null default now()
);

alter table public.chain_cursors enable row level security;
revoke all on public.chain_cursors from anon, authenticated;

create or replace function public.attester_cursor_get(p_name text)
returns bigint
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return (select block from public.chain_cursors where name = p_name);
end $$;

-- 앞으로만 간다. 워커 두 개가 겹쳐 돌아도 커서가 뒤로 가서 같은 구간을 두 번
-- 처리하지 않게 — 같은 이벤트를 두 번 받아도 chain_events 가 막지만, 한 번 더 막는다.
create or replace function public.attester_cursor_set(p_name text, p_block bigint)
returns void
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  insert into public.chain_cursors (name, block) values (p_name, p_block)
  on conflict (name) do update
    set block = greatest(public.chain_cursors.block, excluded.block), updated_at = now();
end $$;

-- 0025 의 attester_op_payload(uuid) 를 사용자 확인이 있는 것으로 바꾼다.
drop function if exists public.attester_op_payload(uuid);

create or replace function public.attester_op_payload(p_op uuid, p_user uuid)
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
  perform economy.attester_guard();
  select * into v from public.chain_ops o where o.id = p_op for update;
  if not found or p_user is null or v.user_id is distinct from p_user then
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
         coalesce(s.km_run < s.lock_km, false)
    from (select 1) one
    left join public.market_sneakers s on s.id = v.sneaker_id;
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
