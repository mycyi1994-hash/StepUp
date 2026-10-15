-- ════════════════════════════════════════════════════════════════════
--  0057 — 결제 요청 번호: 같은 요청을 다시 보내도 한 번만 치른다 (2026-10-15 점검)
--
--  부스터 구매(boost_buy) · 예전 강화(sneaker_upgrade) · 수리(sneaker_repair)는 요청 번호가 없어, 응답만 잃은
--  요청을 앱이(사람이) 다시 보내면 두 번 치렀다(에너지 셀 2칸 + 2칸, 지속형 부스터 24시간 더, 강화 한 단계 더).
--  이제 앱이 요청마다 번호(p_request)를 붙이고, 응답을 못 받은 같은 요청은 같은 번호로 다시 보낸다.
--  서버는 처음 처리한 결과를 적어 두고, 같은 번호가 다시 오면 다시 치르지 않고 그 결과를 돌려준다.
--  번호 없이 부르는 예전 앱은 예전과 같다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists economy.request_receipts (
  -- 계정을 지우면 그 사람의 영수증도 지운다(0021 은 auth.users 에서 이어 지운다)
  user_id uuid not null references auth.users on delete cascade,
  request_id uuid not null,
  kind text not null,
  result numeric,
  created_at timestamptz not null default now(),
  primary key (user_id, request_id)
);
revoke all on economy.request_receipts from public, anon, authenticated;

comment on table economy.request_receipts is
  '결제 요청 번호별 처음 결과. 같은 번호의 요청이 다시 오면 다시 치르지 않고 이 결과를 돌려준다(0057).';

-- 요청 번호가 붙은 인자 목록으로 바꾼다. 옛 목록을 남기면 번호 없이 부르는 호출이 어느 쪽인지 모호해진다.
drop function if exists public.boost_buy(text);
drop function if exists public.sneaker_upgrade(bigint);
drop function if exists public.sneaker_repair(bigint, numeric);

create or replace function public.boost_buy(p_kind text, p_request uuid default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_cost numeric := economy.boost_cost(p_kind);
  v_day date := economy.game_day(now());
  v_balance numeric;
  v_from timestamptz;
  v_prev numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_cost is null then
    raise exception '없는 부스터입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  -- 같은 요청을 다시 보냈다 — 다시 치르지 않고 처음 결과를 돌려준다
  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev;
    end if;
  end if;

  if p_kind = 'ENERGY_CELL' then
    -- 가득 찬데 사면 돈만 사라진다(결정표 X9). 2칸이 들어갈 자리가 있어야 판다.
    if economy.energy_left(v_user, v_day) + 2 > economy.energy_max(v_user) then
      raise exception '에너지가 이미 충분합니다' using errcode = '23514';
    end if;
  end if;

  v_balance := economy.ledger_apply(v_user, 'SPEND_BOOST', -v_cost, '부스터: ' || p_kind);

  if p_kind = 'ENERGY_CELL' then
    insert into public.energy_days (user_id, day, bonus) values (v_user, v_day, 2)
    on conflict (user_id, day) do update set bonus = public.energy_days.bonus + 2;
    insert into public.boosts (user_id, kind, cost) values (v_user, p_kind, v_cost);
  else
    -- 이미 켜져 있으면 끝나는 시각 뒤로 이어 붙인다. 겹쳐 사도 손해가 없게.
    select greatest(now(), coalesce(max(ends_at), now())) into v_from
      from public.boosts where user_id = v_user and kind = p_kind and ends_at > now();
    insert into public.boosts (user_id, kind, starts_at, ends_at, cost)
    values (v_user, p_kind, v_from, v_from + interval '24 hours', v_cost);
  end if;

  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'BOOST:' || p_kind, v_balance);
  end if;
  return v_balance;
end $$;

create or replace function public.sneaker_upgrade(p_id bigint, p_request uuid default null)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_prev numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev::int;
    end if;
  end if;

  v := economy.my_app_sneaker(v_user, p_id);

  -- 폰에서 올린 신발은 레벨을 서버가 믿지 않으므로 서버에서 강화해도 효과가 없다.
  -- 효과 없는 곳에 SUP 를 받지 않는다.
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 강화할 수 없습니다' using errcode = '22023';
  end if;
  if v.level >= economy.max_level(v.rarity) then
    raise exception '최대 레벨입니다' using errcode = '23514';
  end if;

  perform economy.ledger_apply(v_user, 'SPEND_UPGRADE', -economy.upgrade_cost(v.rarity, v.level),
                               format('신발 강화 #%s Lv%s→%s', v.mint_number, v.level, v.level + 1));
  update public.market_sneakers set level = level + 1, updated_at = now() where id = p_id;

  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'UPGRADE:' || p_id, v.level + 1);
  end if;
  return v.level + 1;
end $$;

-- 수리. p_points 를 비우면 가득 채운다. 치른 SUP 는 누구에게도 가지 않는다(소각).
create or replace function public.sneaker_repair(p_id bigint, p_points numeric default null, p_request uuid default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_points numeric;
  v_prev numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev;
    end if;
  end if;

  v := economy.my_app_sneaker(v_user, p_id);

  v_points := least(coalesce(p_points, 100), 100 - v.durability_pts);
  if v_points is null or v_points <= 0 then
    raise exception '고칠 곳이 없습니다' using errcode = '23514';
  end if;
  v_points := round(v_points, 2);

  perform economy.ledger_apply(v_user, 'SPEND_REPAIR',
    -round(v_points * economy.repair_cost_per_point(v.rarity, v.level), 4),
    format('신발 수리 #%s +%s', v.mint_number, v_points));
  update public.market_sneakers
     set durability_pts = least(durability_pts + v_points, 100),
         durability = least(durability_pts + v_points, 100)::int,
         updated_at = now()
   where id = p_id;

  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'REPAIR:' || p_id, least(v.durability_pts + v_points, 100));
  end if;
  return least(v.durability_pts + v_points, 100);
end $$;

revoke all on function public.boost_buy(text, uuid) from public, anon;
grant execute on function public.boost_buy(text, uuid) to authenticated;
revoke all on function public.sneaker_upgrade(bigint, uuid) from public, anon;
grant execute on function public.sneaker_upgrade(bigint, uuid) to authenticated;
revoke all on function public.sneaker_repair(bigint, numeric, uuid) from public, anon;
grant execute on function public.sneaker_repair(bigint, numeric, uuid) to authenticated;
