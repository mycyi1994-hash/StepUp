-- ════════════════════════════════════════════════════════════════════
--  0040 — 체인이 거절한 작업을 40분 붙잡지 않는다 (2026-09-27 서버 · 지갑 점검)
--
--  서명한 작업(SIGNED · SUBMITTED)은 기한(10분) + 마진(30분)이 지나야 되돌렸다. 노드가 늦어
--  방금 쓰인 작업을 못 보고 환불하면 두 번 나가기 때문이다. 그래서 시뮬레이션에서 거절된 작업도
--  40분 동안 잔고 · 하루 한도를 잡고 있었다.
--
--  컨트랙트는 블록 시각이 기한을 넘으면 그 작업을 받지 않는다(ClaimExpired · ReleaseExpired).
--  그러니 **safe 블록의 시각이 기한을 넘었고 그 블록에서 쓰이지 않았으면** 앞으로도 쓰일 수 없다.
--  어테스터가 그것을 확인했을 때(p_safe_past_deadline)는 마진을 기다리지 않고 되돌린다.
--  확인하지 못하면 예전처럼 마진이 지난 뒤에 되돌린다.
-- ════════════════════════════════════════════════════════════════════

-- 기한이 지난 작업 전부 — early 는 아직 마진 안(어테스터가 safe 블록으로 확인해야 되돌릴 수 있다)
drop function if exists public.attester_due_ops();
create function public.attester_due_ops()
returns table (op_id uuid, op_ref text, status text, kind text, deadline timestamptz, tx_hash text, early boolean)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select o.id, economy.op_ref(o.id), o.status, o.kind, o.deadline, o.tx_hash,
         o.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now()
    from public.chain_ops o
   where o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')
     and o.deadline < now()
   order by o.deadline
   limit 200;
end $$;

-- 3개 인자(0025 · 0029)와 이 파일의 4개 인자 모두 지운다 — setup.sql 을 다시 붙여도 겹치지 않게
drop function if exists public.attester_op_expire(uuid, boolean, numeric);
drop function if exists public.attester_op_expire(uuid, boolean, numeric, boolean);
create function public.attester_op_expire(
  p_op uuid,
  p_used_on_chain boolean,
  p_token numeric default null,
  -- 어테스터가 safe 블록 시각이 기한을 넘었고 그 블록에서 쓰이지 않았음을 확인했다
  p_safe_past_deadline boolean default false
)
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
  if v.deadline >= now() then
    raise exception '아직 유효한 작업입니다' using errcode = '55000';
  end if;
  -- 서명한 작업은 체인에 있을 수 있다 — 마진이 지났거나, 체인에서 기한이 지난 것을 확인했을 때만
  if v.status <> 'RESERVED'
     and not (coalesce(p_safe_past_deadline, false) and not coalesce(p_used_on_chain, false))
     and v.deadline + make_interval(secs => economy.setting_num('op_expire_margin_sec')::int) >= now() then
    raise exception '아직 만료 마진이 지나지 않았습니다' using errcode = '55000';
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

-- 어테스터 함수 권한 — 0026 과 같은 규칙(첫 줄의 attester_guard 가 막는다)
do $$
declare r record;
begin
  for r in
    select p.oid::regprocedure as sig from pg_proc p
     where p.pronamespace = 'public'::regnamespace
       and p.proname in ('attester_due_ops', 'attester_op_expire')
  loop
    execute format('revoke all on function %s from public, anon, authenticated', r.sig);
    execute format('grant execute on function %s to stepup_attester, authenticated', r.sig);
  end loop;
end $$;
