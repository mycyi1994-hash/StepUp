-- ════════════════════════════════════════════════════════════════════
--  0107 — 거래소를 도감 모델 단위로 (2026-11-17)
--
--  테스터 "신발 상세에서 판매하기를 누르면 딴 신발로 연결된다"(타비 스플릿 토 조거 → 플레어 시프트).
--
--  거래소는 처음(0007)부터 신발을 "속성 × 등급 × 변형"으로 묶었다. 0066 뒤 신발은 속성이 없고(NONE),
--  레어부터는 도감 모델(model_id)을 따로 뽑는다 — 변형은 그림에 쓰이지 않는 남은 숫자다. 그래서
--  같은 "NONE · 레어 · 3" 줄에 서로 다른 모델이 섞이고, 앱은 그 줄에 예전 불 모델 이름(플레어 시프트)을
--  붙이거나(11-03 전) "레어 신발"이라고만 적었다(11-04). 어느 쪽이든 누른 신발이 아니다.
--
--  모델이 있는 신발은 모델 번호 하나로 묶는다:
--    · 체결 · 구매 입찰 표에 model_id 칸(체결은 신발에서 저절로 채운다).
--    · 모델에 거는 구매 입찰 market_bid_model — 그 모델 신발만 그 입찰에 팔린다.
--    · 새 앱이 읽는 자리는 _v2: market_quotes_v2(모델 하나가 한 줄) · market_asks_v2 · market_bid_book_v2 ·
--      market_my_bids_v2 · market_my_trades_v2 — 예전 것에 model_id 를 더한 모양.
--  예전 자리(market_quotes · market_asks · market_bid · …)는 모양을 바꾸지 않는다 — 예전 앱이 읽고,
--  setup.sql 을 다시 올릴 때 앞 파일이 예전 모양으로 다시 만들기 때문이다(모양을 바꾸면 두 번째 배포가 깨진다).
--  예전 앱이 거는 입찰(모델 없이 속성 × 등급 × 변형)은 예전 뜻 그대로 맞춘다(그 줄의 어느 모델이든).
-- ════════════════════════════════════════════════════════════════════

alter table public.market_bids add column if not exists model_id int references public.sneaker_models(id);
alter table public.market_trades add column if not exists model_id int;

comment on column public.market_bids.model_id is
  '이 모델 신발에만 건 입찰(0107). 비어 있으면 예전처럼 속성 × 등급 × 변형 줄의 어느 신발이든.';
comment on column public.market_trades.model_id is '체결된 신발의 도감 모델(0107). 신발이 지워져도 시세는 남는다.';

-- 지난 체결에도 모델을 적는다(아직 남아 있는 신발만 — 지워진 신발의 체결은 예전 줄로 남는다)
update public.market_trades t
   set model_id = s.model_id
  from public.market_sneakers s
 where s.id = t.sneaker_id and t.model_id is null and s.model_id is not null;

-- 체결은 market_settle 하나가 적는다 — 그 함수를 고치지 않고 적을 때 모델을 채운다
create or replace function economy.market_trade_model() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.model_id is null and new.sneaker_id is not null then
    select s.model_id into new.model_id from public.market_sneakers s where s.id = new.sneaker_id;
  end if;
  return new;
end $$;
revoke all on function economy.market_trade_model() from public;

drop trigger if exists market_trades_model on public.market_trades;
create trigger market_trades_model before insert on public.market_trades
  for each row execute function economy.market_trade_model();

-- 모델 번호는 가려 둘 정보가 아니다(0031 은 주인 · 뽑기 번호 · 토큰 번호만 가린다). 새 DB 에서도 열려 있게 따로 적는다
grant select (model_id) on public.market_sneakers to anon, authenticated;

-- ── 보는 자리(새 앱) — 예전 것에 model_id 를 더한 모양 ──
create or replace view public.market_asks_v2
with (security_invoker = true) as
  select l.id as listing_id,
         l.price,
         l.created_at,
         l.seller_id,
         s.id as sneaker_id,
         s.faction, s.rarity, s.variant,
         s.level, s.mint_number, s.luck, s.comfort, s.durability,
         s.model_id
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN';
comment on view public.market_asks_v2 is '열려 있는 매물(도감 모델 번호도, 0107).';
grant select on public.market_asks_v2 to anon, authenticated;

create or replace view public.market_bid_book_v2
with (security_invoker = true) as
  select id as bid_id, buyer_id, faction, rarity, variant, min_level, price, created_at, model_id
    from public.market_bids
   where status = 'OPEN';
comment on view public.market_bid_book_v2 is '열려 있는 구매 입찰(모델에 건 입찰은 모델 번호도, 0107).';
grant select on public.market_bid_book_v2 to anon, authenticated;

-- 시세판 2판 — 한 줄 = 모델 하나. 모델이 있는 신발은 (NONE, 등급, 0, 모델)로, 없는 신발은 (속성, 등급, 변형, 빈칸)으로 묶는다
create or replace view public.market_quotes_v2
with (security_invoker = true) as
with shoes as (
  select case when s.model_id is null then s.faction else 'NONE' end as faction,
         s.rarity,
         case when s.model_id is null then s.variant else 0 end as variant,
         coalesce(s.model_id, 0) as model_id,
         s.id, s.status
    from public.market_sneakers s
),
supply as (
  select faction, rarity, variant, model_id,
         count(*)::int as supply,
         count(*) filter (where status = 'LISTED')::int as listed
    from shoes group by 1, 2, 3, 4
),
asks as (
  select k.faction, k.rarity, k.variant, k.model_id, min(l.price) as ask
    from public.market_listings l
    join shoes k on k.id = l.sneaker_id
   where l.status = 'OPEN' group by 1, 2, 3, 4
),
bids as (
  select case when model_id is null then faction else 'NONE' end as faction,
         rarity,
         case when model_id is null then variant else 0 end as variant,
         coalesce(model_id, 0) as model_id,
         max(price) as bid, count(*)::int as bid_count
    from public.market_bids where status = 'OPEN' group by 1, 2, 3, 4
),
trades as (
  select case when model_id is null then faction else 'NONE' end as faction,
         rarity,
         case when model_id is null then variant else 0 end as variant,
         coalesce(model_id, 0) as model_id,
         price, traded_at
    from public.market_trades
),
last_trade as (
  select distinct on (faction, rarity, variant, model_id)
         faction, rarity, variant, model_id, price as last_price, traded_at as last_traded_at
    from trades
   order by faction, rarity, variant, model_id, traded_at desc
),
vol as (
  select faction, rarity, variant, model_id,
         count(*)::int as trades_24h,
         coalesce(sum(price), 0)::numeric(20, 4) as volume_24h
    from trades
   where traded_at > now() - interval '24 hours'
   group by 1, 2, 3, 4
),
models as (
  select faction, rarity, variant, model_id from supply
  union
  select faction, rarity, variant, model_id from bids
)
select m.faction,
       m.rarity,
       m.variant,
       nullif(m.model_id, 0) as model_id,
       coalesce(s.supply, 0) as supply,
       coalesce(s.listed, 0) as listed,
       a.ask,
       b.bid,
       coalesce(b.bid_count, 0) as bid_count,
       t.last_price,
       t.last_traded_at,
       coalesce(v.trades_24h, 0) as trades_24h,
       coalesce(v.volume_24h, 0)::numeric(20, 4) as volume_24h
  from models m
  left join supply s using (faction, rarity, variant, model_id)
  left join asks a using (faction, rarity, variant, model_id)
  left join bids b using (faction, rarity, variant, model_id)
  left join last_trade t using (faction, rarity, variant, model_id)
  left join vol v using (faction, rarity, variant, model_id);

comment on view public.market_quotes_v2 is
  '모델별 시세 한 줄(0107) — 도감 모델이 있는 신발은 모델 하나가 한 줄(속성 NONE · 변형 0), 없으면 속성 × 등급 × 변형.';
grant select on public.market_quotes_v2 to anon, authenticated;

-- ── 판매 등록 — 걸려 있는 입찰이 그 신발에 맞는지에 모델을 더한다 ──
create or replace function public.market_list(
  p_sneaker_id bigint,
  p_price numeric,
  p_request uuid default null
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_bid record;
  v_id bigint;
  v_prev record;
begin
  -- 같은 요청이 다시 왔다(응답을 잃고 다시 보냄) — 다시 하지 않고 처음 결과를 돌려준다
  v_prev := economy.market_replay(v_user, p_request);
  if v_prev.found_receipt then
    return v_prev.result;
  end if;

  -- NaN 은 Postgres 에서 어떤 수보다 커서 아래 비교를 통과한다 — 먼저 막는다
  if p_price is null or p_price = 'NaN'::numeric then
    raise exception '값이 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_price < economy.market_min_price() then
    raise exception '값이 너무 낮습니다' using errcode = '22023';
  end if;

  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found or v_s.owner_id is distinct from v_user then
    raise exception '내 스니커즈가 아닙니다' using errcode = '42501';
  end if;
  if v_s.status = 'LISTED' then
    raise exception '이미 판매 중입니다' using errcode = '22023';
  end if;

  -- 이미 이 값 이상을 부른 사람이 있으면 바로 팔린다.
  -- 값은 먼저 걸어 둔 쪽(구매 입찰)의 값으로 정해진다. 모델에 건 입찰은 그 모델 신발에만 맞는다(0107).
  select * into v_bid from public.market_bids
   where status = 'OPEN'
     and ((model_id is null and faction = v_s.faction and rarity = v_s.rarity and variant = v_s.variant)
          or (model_id is not null and model_id = v_s.model_id))
     and min_level <= v_s.level
     and buyer_id <> v_user
     and price >= p_price
   order by price desc, created_at asc
   limit 1
   for update;

  if found then
    update public.market_bids
       set status = 'FILLED', closed_at = now()
     where id = v_bid.id;
    perform public.market_settle(
      p_sneaker_id, v_user, v_bid.buyer_id, v_bid.price, 'BY_BID', true
    );
    return economy.market_receipt(v_user, p_request, 'MARKET_LIST', null);
  end if;

  insert into public.market_listings (sneaker_id, seller_id, price)
  values (p_sneaker_id, v_user, p_price)
  returning id into v_id;

  update public.market_sneakers set status = 'LISTED' where id = p_sneaker_id;
  return economy.market_receipt(v_user, p_request, 'MARKET_LIST', v_id);
end $$;

comment on function public.market_list(bigint, numeric, uuid) is
  '내 스니커즈를 판다. 이미 그 값 이상을 부른 구매 입찰이 있으면 바로 체결된다(모델에 건 입찰은 그 모델만, 0107). 같은 요청 번호는 한 번만(0062).';

-- ── 모델에 거는 구매 입찰 — 그 도감 모델 신발만 이 입찰에 팔린다 ──
-- 예전 market_bid(속성 × 등급 × 변형)는 그대로 둔다(예전 앱). 규칙(값 · 잔고 · 바로 사기 · 잠그기)은 같다.
create or replace function public.market_bid_model(
  p_model int,
  p_min_level int,
  p_price numeric,
  p_request uuid default null
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_ask record;
  v_balance numeric;
  v_id bigint;
  v_prev record;
  v_rarity text;
begin
  v_prev := economy.market_replay(v_user, p_request);
  if v_prev.found_receipt then
    return v_prev.result;
  end if;

  if p_price is null or p_price = 'NaN'::numeric then
    raise exception '값이 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_price < economy.market_min_price() then
    raise exception '값이 너무 낮습니다' using errcode = '22023';
  end if;
  -- 등급은 모델이 정한다. 속성 · 변형은 그 모델 신발에 뜻이 없어 NONE · 0 으로 적는다
  select m.rarity into v_rarity from public.sneaker_models m where m.id = p_model;
  if not found then
    raise exception '없는 모델입니다' using errcode = '22023';
  end if;
  if p_min_level is null or p_min_level < 1 or p_min_level > economy.max_level(v_rarity) then
    raise exception '레벨 조건이 올바르지 않습니다' using errcode = '22023';
  end if;

  -- 잔고 확인과 묶기 사이에 같은 사람의 다른 결제가 끼지 못하게(spend_sup 과 같은 잠금)
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;
  if v_balance < p_price then
    raise exception 'SUP 가 모자랍니다' using errcode = '22023';
  end if;

  -- 그 값 이하의 매물이 이미 있으면 그 값에 바로 산다
  select l.id as listing_id, l.price, l.seller_id, s.id as sneaker_id
    into v_ask
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN'
     and s.model_id = p_model
     and s.level >= p_min_level
     and l.seller_id <> v_user
     and l.price <= p_price
   order by l.price asc, l.created_at asc
   limit 1
   for update of l;

  if found then
    update public.market_listings
       set status = 'SOLD', closed_at = now() where id = v_ask.listing_id;
    perform public.market_settle(
      v_ask.sneaker_id, v_ask.seller_id, v_user, v_ask.price, 'BY_ASK', false
    );
    return economy.market_receipt(v_user, p_request, 'MARKET_BID', null);
  end if;

  -- 기다린다. 건 값은 잠근다 — 잠그지 않으면 같은 돈으로 열 곳에 입찰할 수 있다.
  insert into public.market_bids (buyer_id, faction, rarity, variant, min_level, price, model_id)
  values (v_user, 'NONE', v_rarity, 0, p_min_level, p_price, p_model)
  returning id into v_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_LOCK', -p_price, '구매 입찰');

  return economy.market_receipt(v_user, p_request, 'MARKET_BID', v_id);
end $$;

comment on function public.market_bid_model(int, int, numeric, uuid) is
  '도감 모델에 값을 건다(0107) — 그 모델 신발만 이 입찰에 팔린다. 조건에 맞는 매물이 이미 있으면 그 값에 바로 산다. 같은 요청 번호는 한 번만.';

-- ── 즉시 판매 — 모델에 건 입찰은 그 모델 신발만 받는다 ──
create or replace function public.market_sell_now(
  p_sneaker_id bigint,
  p_bid_id bigint,
  p_request uuid default null
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_b record;
  v_trade bigint;
  v_prev record;
begin
  v_prev := economy.market_replay(v_user, p_request);
  if v_prev.found_receipt then
    return v_prev.result;
  end if;

  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found or v_s.owner_id is distinct from v_user then
    raise exception '내 스니커즈가 아닙니다' using errcode = '42501';
  end if;

  select * into v_b from public.market_bids where id = p_bid_id for update;
  if not found or v_b.status <> 'OPEN' then
    raise exception '이미 끝난 입찰입니다' using errcode = '22023';
  end if;
  if v_b.buyer_id = v_user then
    raise exception '내 입찰에는 팔 수 없습니다' using errcode = '22023';
  end if;
  if (v_b.model_id is not null and v_s.model_id is distinct from v_b.model_id)
     or (v_b.model_id is null
         and (v_b.faction <> v_s.faction or v_b.rarity <> v_s.rarity or v_b.variant <> v_s.variant)) then
    raise exception '모델이 다릅니다' using errcode = '22023';
  end if;
  if v_s.level < v_b.min_level then
    raise exception '레벨이 모자랍니다' using errcode = '22023';
  end if;

  -- 팔려고 내놓은 상태였다면 그 매물은 거둔다
  if v_s.status = 'LISTED' then
    update public.market_listings
       set status = 'CANCELLED', closed_at = now()
     where sneaker_id = p_sneaker_id and status = 'OPEN';
  end if;

  update public.market_bids
     set status = 'FILLED', closed_at = now() where id = p_bid_id;

  v_trade := public.market_settle(
    p_sneaker_id, v_user, v_b.buyer_id, v_b.price, 'BY_BID', true
  );
  return economy.market_receipt(v_user, p_request, 'MARKET_SELL', v_trade);
end $$;

comment on function public.market_sell_now(bigint, bigint, uuid) is
  '걸려 있는 구매 입찰에 내 스니커즈를 판다. 값은 그 입찰의 값이다. 모델에 건 입찰은 그 모델만(0107). 같은 요청 번호는 한 번만(0062).';

-- ── 내 입찰 · 내 거래(새 앱) — 예전 것에 모델 번호를 더한 모양 ──
create or replace function public.market_my_bids_v2()
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  min_level int,
  price numeric,
  created_at timestamptz,
  model_id int
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, min_level, price, created_at, model_id
    from public.market_bids
   where buyer_id = auth.uid() and status = 'OPEN'
   order by created_at desc
$$;
comment on function public.market_my_bids_v2() is '내가 걸어 둔 구매 입찰(모델에 건 입찰은 모델 번호도, 0107).';

create or replace function public.market_my_trades_v2(p_limit int default 30)
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  level int,
  price numeric,
  fee numeric,
  sold boolean,
  traded_at timestamptz,
  model_id int
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, level, price, fee,
         coalesce(seller_id = auth.uid(), false) as sold,
         traded_at, model_id
    from public.market_trades
   where seller_id = auth.uid() or buyer_id = auth.uid()
   order by traded_at desc
   limit greatest(1, least(coalesce(p_limit, 30), 100))
$$;
comment on function public.market_my_trades_v2(int) is '내가 사고판 기록(모델 번호도, 0107).';

revoke all on function public.market_list(bigint, numeric, uuid) from public, anon;
grant execute on function public.market_list(bigint, numeric, uuid) to authenticated;
revoke all on function public.market_bid_model(int, int, numeric, uuid) from public, anon;
grant execute on function public.market_bid_model(int, int, numeric, uuid) to authenticated;
revoke all on function public.market_sell_now(bigint, bigint, uuid) from public, anon;
grant execute on function public.market_sell_now(bigint, bigint, uuid) to authenticated;
revoke all on function public.market_my_bids_v2() from public, anon;
grant execute on function public.market_my_bids_v2() to authenticated;
revoke all on function public.market_my_trades_v2(int) from public, anon;
grant execute on function public.market_my_trades_v2(int) to authenticated;
