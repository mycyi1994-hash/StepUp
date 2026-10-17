-- ════════════════════════════════════════════════════════════════════
--  0062 — 다시 보내도 한 번만: 거래소 · 글 · 댓글 · 좋아요 (2026-10-17 인터넷 끊김 점검)
--
--  응답만 잃은 요청을 앱이(사람이) 다시 보내면 두 번 처리되거나 거꾸로 됐다.
--   - 거래소 구매 입찰(market_bid)은 같은 값을 두 번 묶거나 신발을 두 번 샀다. 즉시 구매 · 즉시 판매 · 내놓기는
--     두 번째가 "이미 끝난 매물" · "이미 판매 중"으로 거절돼, 실제로는 된 일을 실패로 보였다.
--   - 게시판 · 번개 글(post_create)과 댓글(comment_create)이 두 번 올라갔다(번개는 모임이 둘).
--   - 좋아요(post_toggle_like)는 다시 누르면 거꾸로 취소됐다.
--  이제 앱이 요청마다 번호를 붙인다 — 거래소는 p_request(0057 의 결제 영수증), 글 · 댓글은 p_client_key.
--  좋아요는 바라는 상태를 보낸다(post_set_like). 번호 없이 부르는 예전 앱은 예전과 같다.
-- ════════════════════════════════════════════════════════════════════

-- ── 거래소: 요청 번호별 처음 결과 ────────────────────────────────────

-- 결과를 영수증에 적고 그대로 돌려준다. 번호가 없으면(예전 앱) 적지 않는다.
create or replace function economy.market_receipt(p_user uuid, p_request uuid, p_kind text, p_result bigint)
returns bigint
language plpgsql security definer set search_path = public, economy as $$
begin
  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (p_user, p_request, p_kind, p_result);
  end if;
  return p_result;
end $$;
revoke all on function economy.market_receipt(uuid, uuid, text, bigint) from public, anon, authenticated;

-- 같은 번호가 다시 왔는가. 같은 번호의 요청은 차례로 들어오게 잠근다 — 둘이 동시에 와도 두 번째는 첫 영수증을 본다.
-- 번호마다 다른 잠금이라 다른 요청과 서로 기다리지 않는다.
create or replace function economy.market_replay(p_user uuid, p_request uuid, out found_receipt boolean, out result bigint)
language plpgsql security definer set search_path = public, economy as $$
begin
  found_receipt := false;
  result := null;
  if p_request is null then
    return;
  end if;
  perform pg_advisory_xact_lock(hashtext('receipt:' || p_user::text || ':' || p_request::text));
  select true, r.result::bigint into found_receipt, result
    from economy.request_receipts r
   where r.user_id = p_user and r.request_id = p_request;
  found_receipt := coalesce(found_receipt, false);
end $$;
revoke all on function economy.market_replay(uuid, uuid) from public, anon, authenticated;

-- 요청 번호가 붙은 인자 목록으로 바꾼다. 옛 목록을 남기면 번호 없이 부르는 호출이 어느 쪽인지 모호해진다.
drop function if exists public.market_list(bigint, numeric);
drop function if exists public.market_buy_now(bigint);
drop function if exists public.market_bid(text, text, int, int, numeric);
drop function if exists public.market_sell_now(bigint, bigint);

-- ── 판매 입찰 ─────────────────────────────────────────────────────
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
  -- 값은 먼저 걸어 둔 쪽(구매 입찰)의 값으로 정해진다.
  select * into v_bid from public.market_bids
   where status = 'OPEN'
     and faction = v_s.faction and rarity = v_s.rarity and variant = v_s.variant
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
  '내 스니커즈를 판다. 이미 그 값 이상을 부른 구매 입찰이 있으면 바로 체결된다. 같은 요청 번호는 한 번만(0062).';

-- ── 즉시 구매 ─────────────────────────────────────────────────────
create or replace function public.market_buy_now(p_listing_id bigint, p_request uuid default null)
returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_l record;
  v_balance numeric;
  v_prev record;
begin
  v_prev := economy.market_replay(v_user, p_request);
  if v_prev.found_receipt then
    return v_prev.result;
  end if;

  -- 잔고를 세고 쓰는 사이에 같은 사람의 다른 결제가 끼면 둘 다 "잔고 충분"을 보고
  -- 잔고가 음수가 된다. 이 사람의 원장 쓰기를 한 줄로 세운다(spend_sup 과 같은 잠금).
  -- 매물 잠금보다 먼저 잡아, 입찰과 이 잠금을 서로 반대 순서로 잡지 않게 한다.
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select * into v_l from public.market_listings where id = p_listing_id for update;
  if not found or v_l.status <> 'OPEN' then
    raise exception '이미 끝난 매물입니다' using errcode = '22023';
  end if;
  if v_l.seller_id = v_user then
    raise exception '내 매물은 살 수 없습니다' using errcode = '22023';
  end if;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;
  if v_balance < v_l.price then
    raise exception 'SUP 가 모자랍니다' using errcode = '22023';
  end if;

  update public.market_listings
     set status = 'SOLD', closed_at = now() where id = p_listing_id;

  return economy.market_receipt(v_user, p_request, 'MARKET_BUY', public.market_settle(
    v_l.sneaker_id, v_l.seller_id, v_user, v_l.price, 'BY_ASK', false
  ));
end $$;

comment on function public.market_buy_now(bigint, uuid) is
  '매물을 그 값에 산다. 값은 매물이 정한 값이다. 같은 요청 번호는 한 번만(0062).';

-- ── 구매 입찰 ─────────────────────────────────────────────────────
create or replace function public.market_bid(
  p_faction text,
  p_rarity text,
  p_variant int,
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

  -- 잔고 확인과 묶기 사이에 같은 사람의 다른 결제가 끼지 못하게(spend_sup 과 같은 잠금)
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = v_user;
  if v_balance < p_price then
    raise exception 'SUP 가 모자랍니다' using errcode = '22023';
  end if;

  -- 그 값 이하의 매물이 이미 있으면 굳이 기다릴 이유가 없다.
  -- 값은 먼저 걸려 있던 매물의 값으로 — 부른 값보다 싸게 산다.
  select l.id as listing_id, l.price, l.seller_id, s.id as sneaker_id
    into v_ask
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN'
     and s.faction = p_faction and s.rarity = p_rarity and s.variant = p_variant
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
  insert into public.market_bids (buyer_id, faction, rarity, variant, min_level, price)
  values (v_user, p_faction, p_rarity, p_variant, p_min_level, p_price)
  returning id into v_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_LOCK', -p_price, '구매 입찰');

  return economy.market_receipt(v_user, p_request, 'MARKET_BID', v_id);
end $$;

comment on function public.market_bid(text, text, int, int, numeric, uuid) is
  '모델에 값을 건다. 조건에 맞는 매물이 이미 있으면 그 값에 바로 산다. 같은 요청 번호는 한 번만(0062).';

-- ── 즉시 판매 ─────────────────────────────────────────────────────
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
  if v_b.faction <> v_s.faction or v_b.rarity <> v_s.rarity or v_b.variant <> v_s.variant then
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
  '걸려 있는 구매 입찰에 내 스니커즈를 판다. 값은 그 입찰의 값이다. 같은 요청 번호는 한 번만(0062).';

-- 예전처럼 로그인한 사람(market_me 가 막는다)이 부른다
revoke all on function public.market_list(bigint, numeric, uuid) from public, anon;
grant execute on function public.market_list(bigint, numeric, uuid) to authenticated;
revoke all on function public.market_buy_now(bigint, uuid) from public, anon;
grant execute on function public.market_buy_now(bigint, uuid) to authenticated;
revoke all on function public.market_bid(text, text, int, int, numeric, uuid) from public, anon;
grant execute on function public.market_bid(text, text, int, int, numeric, uuid) to authenticated;
revoke all on function public.market_sell_now(bigint, bigint, uuid) from public, anon;
grant execute on function public.market_sell_now(bigint, bigint, uuid) to authenticated;

-- ── 게시판 · 번개 글: 요청 키 ───────────────────────────────────────
-- posts.client_key 와 (작성자, 키) 유일 색인은 0046 에 있다(동네 이야기 story_create 와 같은 규칙).
drop function if exists public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision
);

create or replace function public.post_create(
  p_category text,
  p_crew uuid,
  p_title text,
  p_body text,
  p_place text,
  p_distance_km double precision,
  p_meet_at timestamptz,
  p_capacity int,
  p_lat double precision,
  p_lng double precision,
  p_client_key uuid default null
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
  v_flash boolean := p_category = 'FLASH';
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 같은 요청이 다시 왔다(응답을 받지 못하고 다시 올리기) — 앞서 만든 글 번호를 돌려준다(번개 모임이 둘이 되지 않게)
  if p_client_key is not null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
    if v_id is not null then
      return v_id;
    end if;
  end if;
  if p_category not in ('FLASH', 'FREE', 'TIP') then
    raise exception '게시판 종류가 올바르지 않습니다' using errcode = '22023';
  end if;
  if not public.is_crew_member(p_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  if (select count(*) from public.posts p
       where p.author_id = v_user and p.created_at > now() - interval '1 hour') >= 20 then
    raise exception '글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;
  if v_flash and (p_meet_at is null or p_meet_at < now()) then
    raise exception '번개러닝은 앞으로의 모임 시각이 있어야 합니다' using errcode = '23514';
  end if;

  if p_lat is not null and (p_lat not between -90 and 90 or p_lng is null
                            or p_lng not between -180 and 180) then
    raise exception '모임 장소 좌표가 올바르지 않습니다' using errcode = '22023';
  end if;

  insert into public.posts (
    author_id, category, crew_id, title, body,
    place, distance_km, meet_at, capacity, lat, lng, client_key
  )
  values (
    v_user,
    p_category,
    p_crew,
    btrim(p_title),
    btrim(coalesce(p_body, '')),
    case when v_flash then btrim(coalesce(p_place, '')) else '' end,
    case when v_flash then greatest(coalesce(p_distance_km, 0), 0) else 0 end,
    case when v_flash then p_meet_at else null end,
    -- 번개는 둘 이상이 모여야 번개다. 정원 1은 혼자 뛰는 것과 같다.
    case when v_flash then least(greatest(coalesce(p_capacity, 2), 2), 200) else 0 end,
    case when v_flash then p_lat else null end,
    case when v_flash then p_lng else null end,
    p_client_key
  )
  on conflict (author_id, client_key) where client_key is not null do nothing
  returning id into v_id;

  -- 같은 키의 요청이 동시에 들어와 다른 쪽이 먼저 만들었다 — 그 글 번호를 돌려준다
  if v_id is null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
    return v_id;
  end if;

  -- 번개를 연 사람은 참가자 첫 줄이다.
  if v_flash then
    insert into public.flash_participants (post_id, user_id) values (v_id, v_user);
  end if;

  return v_id;
end;
$$;

revoke all on function public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision, uuid
) from public, anon;
grant execute on function public.post_create(
  text, uuid, text, text, text, double precision, timestamptz, int, double precision, double precision, uuid
) to authenticated;

-- ── 댓글: 요청 키 ─────────────────────────────────────────────────
alter table public.comments add column if not exists client_key uuid;
create unique index if not exists comments_author_client_key
  on public.comments (author_id, client_key) where client_key is not null;
comment on column public.comments.client_key is '앱이 댓글 쓰기마다 만든 요청 키 — 같은 키로 다시 오면 새 댓글을 만들지 않는다(0062)';

drop function if exists public.comment_create(bigint, bigint, text);

-- @param p_parent 0 또는 null 이면 새 댓글, 그 외에는 그 댓글에 대한 답글
create or replace function public.comment_create(p_post bigint, p_parent bigint, p_body text, p_client_key uuid default null)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_parent bigint := nullif(p_parent, 0);
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 같은 요청이 다시 왔다(응답을 받지 못하고 다시 보내기) — 앞서 단 댓글 번호를 돌려준다
  if p_client_key is not null then
    select c.id into v_id from public.comments c where c.author_id = v_user and c.client_key = p_client_key;
    if v_id is not null then
      return v_id;
    end if;
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;
  -- 다른 글의 댓글에 답글을 달면 그 답글은 어느 글에도 보이지 않는다.
  if v_parent is not null and not exists (
    select 1 from public.comments c where c.id = v_parent and c.post_id = p_post
  ) then
    raise exception '답글을 달 댓글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;
  if (select count(*) from public.comments c
       where c.author_id = v_user and c.created_at > now() - interval '1 hour') >= 60 then
    raise exception '댓글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;

  insert into public.comments (post_id, parent_id, author_id, body, client_key)
  values (p_post, v_parent, v_user, btrim(p_body), p_client_key)
  on conflict (author_id, client_key) where client_key is not null do nothing
  returning id into v_id;

  if v_id is null then
    select c.id into v_id from public.comments c where c.author_id = v_user and c.client_key = p_client_key;
  end if;
  return v_id;
end;
$$;

revoke all on function public.comment_create(bigint, bigint, text, uuid) from public, anon;
grant execute on function public.comment_create(bigint, bigint, text, uuid) to authenticated;

-- ── 좋아요: 바라는 상태로 ──────────────────────────────────────────
-- 누르기 · 거두기를 번갈아 하는 post_toggle_like 는 응답을 잃고 다시 누르면 거꾸로 된다. 바라는 상태를 보내면
-- 몇 번을 다시 보내도 같다. 돌려주는 값은 그 뒤의 상태(눌렸으면 true). 예전 앱을 위해 toggle 은 그대로 둔다.
create or replace function public.post_set_like(p_post bigint, p_liked boolean)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_liked is null then
    raise exception '좋아요 상태가 없습니다' using errcode = '22023';
  end if;
  if not public.can_see_post(p_post) then
    raise exception '글을 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;

  if p_liked then
    insert into public.post_likes (post_id, user_id) values (p_post, v_user) on conflict do nothing;
  else
    delete from public.post_likes where post_id = p_post and user_id = v_user;
  end if;
  return p_liked;
end;
$$;

revoke all on function public.post_set_like(bigint, boolean) from public, anon;
grant execute on function public.post_set_like(bigint, boolean) to authenticated;

-- ── 지우기: 이미 없으면 된 것으로 ──────────────────────────────────
-- 지운 응답을 잃고 "다시 삭제"를 누르면 줄이 이미 없어 "내가 쓴 글만 지울 수 있습니다"가 났고, 몇 번을 눌러도 같았다.
-- 그 번호의 줄이 아예 없으면 이미 지워진 것이다 — 조용히 끝낸다. 남의 글 · 댓글은 예전처럼 거절한다.
create or replace function public.post_delete(p_post bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.posts where id = p_post and author_id = auth.uid();
  if not found and exists (select 1 from public.posts where id = p_post) then
    raise exception '내가 쓴 글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

create or replace function public.comment_delete(p_comment bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.comments where id = p_comment and author_id = auth.uid();
  if not found and exists (select 1 from public.comments where id = p_comment) then
    raise exception '내가 쓴 댓글만 지울 수 있습니다' using errcode = '42501';
  end if;
end;
$$;
