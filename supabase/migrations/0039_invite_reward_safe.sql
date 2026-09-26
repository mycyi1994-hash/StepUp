-- ════════════════════════════════════════════════════════════════════
--  0039 — 초대 적립이 실패해도 러닝 저장은 막지 않는다 (2026-09-26 전수 점검)
--
--  0036 의 적립은 walk_sessions 에 러닝을 넣는 트리거 안에서 돈다. 적립(ledger_apply)이
--  예외를 내면(예: 같은 ref 가 이미 있다) 러닝 저장까지 함께 실패했고, 앱이 다시 올려도
--  같은 이유로 계속 실패한다. 이제 적립 부분만 따로 되돌리고 러닝은 저장한다. 확정 시각을
--  적지 않으므로 다음 유효 러닝에서 다시 시도한다. 이미 적힌 적립(같은 ref)은 건너뛰고 빠진 쪽만 적어
--  확정한다 — 같은 ref 로 다시 적으려 하면 매번 실패해 영영 확정되지 않는다. 금액 · 조건은 0036 그대로다.
-- ════════════════════════════════════════════════════════════════════

create or replace function economy.invite_reward_after_session()
returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_r public.invite_redemptions;
  v_cfg economy.invite_config;
  v_amount numeric(20, 4);
begin
  if new.verdict = 'VOID' then
    return null;
  end if;
  select * into v_r from public.invite_redemptions r
   where r.invitee = new.user_id and r.rewarded_at is null
   for update;
  if not found then
    return null;
  end if;
  select * into v_cfg from economy.invite_config where id;
  v_amount := coalesce(v_cfg.reward_sup, 0);
  if v_amount <= 0 or coalesce(new.steps, 0) < coalesce(v_cfg.min_steps, 1000) then
    return null;
  end if;
  -- 적립이 어떤 까닭으로든 실패해도 러닝 저장은 막지 않는다. 확정을 적지 않으므로 다음 러닝에서 다시 시도한다.
  begin
    -- 이미 적힌 쪽은 건너뛴다(같은 ref 로 다시 적으면 영원히 실패한다) — 빠진 쪽만 적고 확정한다
    if not exists (select 1 from public.sup_ledger l
                    where l.user_id = new.user_id and l.ref = 'invite:joined') then
      perform economy.ledger_apply(new.user_id, 'EARN_INVITE', v_amount, '친구 초대 — 첫 러닝', 'invite:joined');
    end if;
    if (select count(*) from public.invite_redemptions r
         where r.inviter = v_r.inviter and r.rewarded_at is not null) < v_cfg.max_rewards_per_inviter
       and not exists (select 1 from public.sup_ledger l
                        where l.user_id = v_r.inviter and l.ref = 'invite:' || new.user_id::text) then
      perform economy.ledger_apply(v_r.inviter, 'EARN_INVITE', v_amount,
        '친구 초대 — 초대한 친구의 첫 러닝', 'invite:' || new.user_id::text);
    end if;
    update public.invite_redemptions set rewarded_at = now(), reward_sup = v_amount
     where invitee = new.user_id;
  exception when others then
    raise warning '초대 적립 실패(러닝은 저장됨): % %', sqlstate, sqlerrm;
  end;
  return null;
end $$;
revoke all on function economy.invite_reward_after_session() from public;
