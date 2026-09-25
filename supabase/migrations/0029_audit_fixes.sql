-- ════════════════════════════════════════════════════════════════════
--  0029 — 전체 점검 반영 (2026-09-25)
--
--  1. 만료: 계정을 지운 사람의 SUP 꺼내기가 만료되면 되돌려 줄 계정이 없다. 예전에는
--     여기서 오류가 나 어테스터의 만료 · 대조가 매분 같은 자리에서 멈췄다. 이제 만료만
--     하고 기록을 남긴다.
--  2. 입금: 4자리보다 잘게 넣은 SUP 는 버림으로 받는다(반올림하면 넣은 것보다 더 준다).
--     대조용 합도 버림으로 돌려준다 — 체인 금고보다 커 보여 거짓으로 멈추지 않게.
--  3. 모르는 작업 번호로 SUP 가 나가거나 신발이 풀렸다 → 서명 키가 샌 것. 체인 작업을
--     멈춘다(어테스터가 컨트랙트도 멈춘다).
--  4. 아는 작업 번호라도 받는 지갑 · 금액 · 신발 번호가 서버 기록과 다르면 멈춘다.
--  5. 관리자가 체인에서 작업을 취소하면(OpCancelled) 그 신발을 앱으로 돌려놓는다.
--  6. 잠긴 신발을 넣을 때, 그 계정에 예전에 붙었던 지갑에서 넣어도 받는다.
--  (지갑 연결이 첫 지갑인지 돌려주는 것은 반환 형식이 바뀌어 0025 에서 고쳤다.)
--  7. 0023 전에 건 매물 중 이제 팔 수 없는 신발(예전 신발 · 첫 신발 · 잠금 거리 전)은 내린다.
--     팔리지도 않으면서 같은 모델의 사기 주문을 매번 오류로 막고 있었다.
--  8. 내 거래 기록: 상대가 계정을 지우면 "판 것인가"가 비어(null) 앱이 기록을 못 읽었다.
-- ════════════════════════════════════════════════════════════════════

create or replace function public.attester_op_expire(p_op uuid, p_used_on_chain boolean, p_token numeric default null)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare v public.chain_ops;
begin
  perform economy.attester_guard();
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
    if v.user_id is not null then
      perform economy.ledger_apply(v.user_id, 'CHAIN_REFUND', v.amount, '꺼내기 만료 — 되돌림', 'refund:' || v.id);
    else
      -- 계정을 지웠다. 되돌려 줄 곳이 없으니 기록만 남긴다(체인에서는 나가지 않았다).
      perform public.admin_log('chain_expire_no_owner', v.id::text, jsonb_build_object('amount', v.amount));
    end if;
  else
    -- 신발은 앱으로 돌아온다. 보너스 뽑기로 만든 신발도 사라지지 않고 앱에 남는다.
    update public.market_sneakers
       set chain_state = 'APP', updated_at = now()
     where id = v.sneaker_id and chain_state = 'WITHDRAWING';
  end if;
  return 'EXPIRED';
end $$;

-- 체인에서 서버가 허락하지 않은 일이 일어났다 — 체인 작업을 멈추고 기록한다
create or replace function economy.chain_alarm(p_action text, p_tx text, p_data jsonb)
returns void language plpgsql security definer set search_path = public, economy as $$
begin
  update public.economy_settings set value = 'true'::jsonb, updated_at = now() where key = 'chain_paused';
  perform public.admin_log(p_action, p_tx, p_data);
end $$;
revoke all on function economy.chain_alarm(text, text, jsonb) from public;

/*
 * 확정된 체인 이벤트 하나. 어테스터가 확정 블록까지 기다린 뒤, 블록 번호 · 로그 번호
 * 순서대로 부른다(같은 신발의 꺼내기 이벤트가 넣기 이벤트보다 먼저 와야 한다).
 *
 *   SUP_CLAIMED        {"op": opRef, "runner": "0x…", "amount": "12.5"}
 *   SNEAKER_RELEASED   {"op": opRef, "tokenId": "123", "to": "0x…"}
 *   OP_CANCELLED       {"op": opRef}
 *   SUP_DEPOSITED      {"account": accountRef, "amount": "12.5"}
 *   SNEAKER_DEPOSITED  {"account": accountRef, "tokenId": "123", "from": "0x…"}
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
  v_shoe_token numeric;
begin
  perform economy.attester_guard();
  insert into public.chain_events (tx_hash, log_index, block_number, kind, data)
  values (v_tx, p_log, p_block, p_kind, p_data)
  on conflict do nothing;
  if not found then
    return 'DUPLICATE';
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
    -- 관리자(또는 지킴이)가 체인에서 작업 번호를 막았다. 그 번호로는 이제 풀 수 없으니
    -- 신발을 앱으로 돌려놓는다. 만료를 기다리지 않는다.
    v_op := economy.account_from_ref(p_data ->> 'op');
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

-- 정산 대조 — 넣기 합은 원장처럼 4자리 아래를 버린다(체인 금고 합보다 커 보이지 않게)
create or replace function public.attester_ledger_totals()
returns table (
  sup_withdrawn_confirmed numeric,
  sup_withdraw_pending numeric,
  sup_deposited numeric,
  sneakers_on_chain bigint,
  sneakers_pending bigint
)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select
    coalesce((select sum(amount) from public.chain_ops where kind = 'SUP_WITHDRAW' and status = 'CONFIRMED'), 0),
    coalesce((select sum(amount) from public.chain_ops
               where kind = 'SUP_WITHDRAW' and status in ('RESERVED', 'SIGNED', 'SUBMITTED')), 0),
    -- 원장이 아니라 체인 이벤트로 센다. 계정을 지우면 원장 줄은 사라진다.
    coalesce((select sum(trunc((data ->> 'amount')::numeric, 4)) from public.chain_events
               where kind = 'SUP_DEPOSITED'), 0),
    (select count(*) from public.market_sneakers where chain_state = 'ON_CHAIN'),
    (select count(*) from public.market_sneakers where chain_state in ('WITHDRAWING', 'DEPOSITING'));
end $$;

-- 7. 팔 수 없게 된 옛 매물 내리기 — 한 번 내리면 다시 걸리지 않으므로(guard_listing) 다시 돌려도 안전하다
with dead as (
  update public.market_listings l
     set status = 'CANCELLED', closed_at = now()
    from public.market_sneakers s
   where l.sneaker_id = s.id and l.status = 'OPEN'
     and (s.chain_state <> 'APP' or s.equipped or s.origin in ('STARTER', 'IMPORT', 'MINT') or s.km_run < s.lock_km)
  returning s.id
)
update public.market_sneakers set status = 'OWNED' where id in (select id from dead) and status = 'LISTED';

-- 8. 내가 사고판 기록 — 판 사람 칸이 비어도(계정 삭제) 판 것인지 거짓/참으로 답한다
create or replace function public.market_my_trades(p_limit int default 30)
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  level int,
  price numeric,
  fee numeric,
  sold boolean,
  traded_at timestamptz
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, level, price, fee,
         coalesce(seller_id = auth.uid(), false) as sold,
         traded_at
    from public.market_trades
   where seller_id = auth.uid() or buyer_id = auth.uid()
   order by traded_at desc
   limit greatest(1, least(coalesce(p_limit, 30), 100))
$$;

-- 9. (2차 점검) 같은 작업을 두 요청이 동시에 보내지 못하게 — 45초 동안 한 요청만 서명한다
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
  -- 방금 다른 요청이 서명해 보내는 중이다. 둘이 겹치면 가스비를 두 번 내고 하나는 체인이
  -- 거절한다. 보내기가 실패했으면 잠시 뒤 다시 보낼 수 있다.
  if v.status = 'SIGNED' and v.updated_at > now() - interval '45 seconds' then
    raise exception '이 작업을 보내는 중입니다. 잠시 뒤에 다시 해 주세요' using errcode = '55000';
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

-- 10. (2차 점검) 서버가 체인 작업을 멈춰 두었는가 — 어테스터가 매분 보고 컨트랙트도 멈춰 있게 한다
create or replace function public.attester_chain_paused()
returns boolean
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false);
end $$;
revoke all on function public.attester_chain_paused() from public, anon, authenticated;
grant execute on function public.attester_chain_paused() to stepup_attester, authenticated;

