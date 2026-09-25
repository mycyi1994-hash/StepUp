-- NFT 마켓 — 러너끼리 스니커즈를 사고파는 자리.
--
-- ── 왜 서버에 두는가 ──
--
-- 거래는 두 사람 사이의 일이다. 내 폰 안에서만 벌어지는 거래는 거래가
-- 아니라 화면 연출이다. 매물도 입찰도 체결도 한 곳에 있어야 상대가 볼 수
-- 있고, 그 한 곳은 서버다.
--
-- 돈이 오가므로 앱을 믿을 수 없다. 앱에는 anon 키가 박혀 있고 누구나 꺼내
-- 볼 수 있다. 그래서 이 파일의 표는 **읽기만** 열려 있고, 모든 변화는 아래
-- 함수로만 들어온다. 원장(0003)과 같은 태도다.
--
-- ── 거래 방식 ──
--
-- 두 가지를 합쳤다.
--
--   * 모델 단위 시세 (KREAM 식)
--     - 같은 모델(속성×등급×변형)끼리 한 줄로 묶어 시세를 본다.
--     - 판매 입찰 = 내 신발 한 켤레에 값을 건다 → 가장 싼 값이 "즉시 구매가"
--     - 구매 입찰 = 모델에 값을 건다(켤레 지정 없음) → 가장 비싼 값이 "즉시 판매가"
--
--   * 아이템 단위 장부 (오픈시·블러 식)
--     - 매물마다 민팅 번호·레벨·스탯이 다르므로 줄마다 그것을 보여 준다.
--     - 체결 내역이 남아 시세 그래프가 된다.
--
-- 스니커즈는 레벨에 따라 값이 다르다. 같은 모델이라고 아무 켤레나 같은 값에
-- 팔리지는 않으므로, **구매 입찰에는 최소 레벨 조건**을 붙인다. 이것이
-- KREAM 의 "같은 모델은 같은 물건" 가정을 이 앱에 맞게 고친 부분이다.
--
-- ── 체결 규칙 ──
--
-- 먼저 걸린 쪽(메이커)의 값으로 체결한다. 거래소의 기본 규칙이고, 나중에
-- 들어온 쪽이 값을 밀어 올리거나 내리지 못하게 한다.
--
-- 자기 매물을 자기가 사는 것은 막는다. 혼자서 값을 지어내는 것(자전거래)을
-- 막기 위해서다. 그래서 **시험하려면 계정이 둘 필요하다**.

-- ══════════════════════════════════════════════════════════════════
-- 경제 규칙
-- ══════════════════════════════════════════════════════════════════

-- 거래 수수료 (bp, 10000 = 100%). 파는 쪽에서 뗀다.
-- 이 SUP 는 어디로도 가지 않고 사라진다 — 원장에서 빠지므로 총량이 준다.
-- 뛰어서 버는 쪽으로 무게를 두기 위한 것이다.
create or replace function economy.market_fee_bps() returns int
  language sql immutable as $$ select 250 $$;

-- 한 계정이 **기존에 갖고 있던** 스니커즈를 거래소에 올릴 수 있는 최대 수.
--
-- 지금까지 스니커즈는 폰 안에만 있었다. 그것을 거래소에 올리려면 서버가
-- 앱의 말을 믿는 수밖에 없다 — 앱이 "나 전설 신발 있어"라고 하면 확인할
-- 방법이 없다. 그래서 수를 묶는다.
--
-- 이 구멍은 **SUP 에 실제 가치가 붙기 전에 반드시 닫아야 한다.** 닫는 길은
-- 민팅을 서버에서 하는 것이다(서버가 뽑고, 서버가 적어 두면 믿을 근거가
-- 생긴다). 지금은 SUP 가 앱 안의 점수라 이 선에서 멈춘다.
create or replace function economy.market_import_cap() returns int
  language sql immutable as $$ select 30 $$;

-- 값을 붙일 수 있는 최소 단위. 0.0001 SUP 짜리 매물로 장부를 채우는 것을 막는다.
create or replace function economy.market_min_price() returns numeric
  language sql immutable as $$ select 1::numeric $$;

-- ══════════════════════════════════════════════════════════════════
-- 원장에 거래 항목을 더한다
-- ══════════════════════════════════════════════════════════════════
--
-- 0003 은 `create table if not exists` 라 이미 만들어진 표의 제약은 바뀌지
-- 않는다. 항목을 늘리려면 제약을 다시 걸어야 한다.
--
--   ESCROW_LOCK    구매 입찰을 걸 때 잠근다 (음수)
--   ESCROW_UNLOCK  입찰을 거둘 때 푼다 (양수)
--   TRADE_BUY      살 때 낸 값 (음수)
--   TRADE_SELL     팔아서 받은 값 (양수)
--   TRADE_FEE      수수료 (음수)
--
-- 잠금을 따로 두지 않고 원장의 음수로 적는 이유는, 잔고가 곧 원장의 합이기
-- 때문이다. 잠근 만큼 잔고가 줄어 있으므로 잠긴 돈을 두 번 쓸 수 없다.
alter table public.sup_ledger drop constraint if exists sup_ledger_kind_check;
-- not valid: 이 파일은 배포 때마다 다시 돈다. 뒤 파일(0022)이 종류를 더 늘린 뒤에
-- 여기서 옛 목록으로 기존 줄을 검사하면 배포가 통째로 멈춘다. 새 줄만 검사하고,
-- 전체 검사는 최종 목록을 거는 0022 가 한다.
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE'
)) not valid;

-- ══════════════════════════════════════════════════════════════════
-- 등록부 — 거래소가 아는 스니커즈
-- ══════════════════════════════════════════════════════════════════

-- 민팅 번호는 거래소 전체에서 하나씩 늘어난다. 폰 안의 번호는 그 폰에서만
-- 1, 2, 3 이라 다른 사람 것과 겹친다. 장부에 올리는 순간 전체에서 유일한
-- 번호가 필요하다.
create sequence if not exists public.market_mint_seq start 1;

create table if not exists public.market_sneakers (
  id bigint generated always as identity primary key,

  owner_id uuid not null references auth.users on delete cascade,

  faction text not null check (faction in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')),
  rarity text not null check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
  variant int not null check (variant between 0 and 10),

  level int not null check (level between 1 and 30),
  luck numeric(6, 3) not null default 1,
  comfort numeric(6, 3) not null default 1,
  durability int not null check (durability between 0 and 100),

  mint_number bigint not null default nextval('public.market_mint_seq'),

  -- IMPORT = 폰에 있던 것을 올렸다, MINT = 서버에서 뽑았다(아직 없음)
  origin text not null default 'IMPORT' check (origin in ('IMPORT', 'MINT')),

  -- OWNED = 그냥 갖고 있다, LISTED = 팔려고 내놨다
  status text not null default 'OWNED' check (status in ('OWNED', 'LISTED')),

  created_at timestamptz not null default now()
);

comment on table public.market_sneakers is
  '거래소에 올라온 스니커즈. 소유자는 이 표가 정한다 — 폰 안의 목록은 이것의 사본이다.';

create index if not exists market_sneakers_owner on public.market_sneakers (owner_id);
create index if not exists market_sneakers_model on public.market_sneakers (faction, rarity, variant);

-- 올린 기록. 소유자가 바뀌어도 "누가 몇 개를 올렸는지"는 여기 남는다.
--
-- 표를 따로 두는 이유: 팔고 나면 market_sneakers 의 owner_id 가 바뀌므로
-- 거기서는 셀 수 없다. 팔았다고 같은 신발을 다시 올릴 수 있으면 상한이
-- 아무 뜻도 없어진다.
create table if not exists public.market_imports (
  user_id uuid not null references auth.users on delete cascade,
  -- 폰 안 sneakers 표의 id
  local_id bigint not null,
  sneaker_id bigint not null references public.market_sneakers on delete cascade,
  created_at timestamptz not null default now(),
  primary key (user_id, local_id)
);

comment on table public.market_imports is
  '폰의 신발을 거래소에 올린 기록. 같은 신발을 두 번 올리는 것과 상한 우회를 막는다.';

-- ══════════════════════════════════════════════════════════════════
-- 판매 입찰 (매물)
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_listings (
  id bigint generated always as identity primary key,
  sneaker_id bigint not null references public.market_sneakers on delete cascade,
  seller_id uuid not null references auth.users on delete cascade,
  price numeric(20, 4) not null check (price > 0),
  status text not null default 'OPEN' check (status in ('OPEN', 'SOLD', 'CANCELLED')),
  created_at timestamptz not null default now(),
  closed_at timestamptz
);

comment on table public.market_listings is '판매 입찰. 한 켤레에 값을 건다.';

-- 한 켤레는 한 번에 한 곳에만 걸린다. 두 곳에 걸면 둘 다 팔려 한 켤레가
-- 두 사람에게 간다.
create unique index if not exists market_listing_one_open
  on public.market_listings (sneaker_id) where status = 'OPEN';

create index if not exists market_listings_seller on public.market_listings (seller_id, status);

-- ══════════════════════════════════════════════════════════════════
-- 구매 입찰
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_bids (
  id bigint generated always as identity primary key,
  buyer_id uuid not null references auth.users on delete cascade,

  -- 켤레가 아니라 모델에 건다
  faction text not null check (faction in ('FIRE', 'WATER', 'LIGHTNING', 'WIND')),
  rarity text not null check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY')),
  variant int not null check (variant between 0 and 10),

  -- 레벨이 값을 가르므로 조건을 붙일 수 있게 한다
  min_level int not null default 1 check (min_level between 1 and 30),

  price numeric(20, 4) not null check (price > 0),
  status text not null default 'OPEN' check (status in ('OPEN', 'FILLED', 'CANCELLED')),
  created_at timestamptz not null default now(),
  closed_at timestamptz
);

comment on table public.market_bids is
  '구매 입찰. 켤레를 고르지 않고 모델과 최소 레벨로 건다. 건 값은 원장에서 잠긴다.';

create index if not exists market_bids_model
  on public.market_bids (faction, rarity, variant, price desc) where status = 'OPEN';
create index if not exists market_bids_buyer on public.market_bids (buyer_id, status);

-- ══════════════════════════════════════════════════════════════════
-- 체결 내역
-- ══════════════════════════════════════════════════════════════════

create table if not exists public.market_trades (
  id bigint generated always as identity primary key,
  sneaker_id bigint references public.market_sneakers on delete set null,

  -- 모델과 레벨을 여기에도 적어 둔다. 신발이 지워져도 시세는 남아야 한다.
  faction text not null,
  rarity text not null,
  variant int not null,
  level int not null,

  seller_id uuid references auth.users on delete set null,
  buyer_id uuid references auth.users on delete set null,

  price numeric(20, 4) not null,
  fee numeric(20, 4) not null default 0,

  -- 누가 먼저 걸려 있었는지. BY_ASK = 매물이 메이커, BY_BID = 구매 입찰이 메이커
  kind text not null check (kind in ('BY_ASK', 'BY_BID')),

  traded_at timestamptz not null default now()
);

comment on table public.market_trades is '체결 내역. 시세 그래프의 원천이다.';

create index if not exists market_trades_model
  on public.market_trades (faction, rarity, variant, traded_at desc);
create index if not exists market_trades_user
  on public.market_trades (traded_at desc);

-- ══════════════════════════════════════════════════════════════════
-- 권한 — 읽기는 모두, 쓰기는 함수만
-- ══════════════════════════════════════════════════════════════════

alter table public.market_sneakers enable row level security;
alter table public.market_imports  enable row level security;
alter table public.market_listings enable row level security;
alter table public.market_bids     enable row level security;
alter table public.market_trades   enable row level security;

-- 장부는 모두에게 보여야 시세가 된다. 남의 신발이라도 무엇이 얼마에 걸려
-- 있는지는 봐야 살지 말지 정한다.
do $$ begin
  create policy market_sneakers_read on public.market_sneakers for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_listings_read on public.market_listings for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_bids_read on public.market_bids for select using (true);
exception when duplicate_object then null; end $$;

do $$ begin
  create policy market_trades_read on public.market_trades for select using (true);
exception when duplicate_object then null; end $$;

-- 올린 기록은 내 것만 본다. 남이 몇 개를 올렸는지는 시세와 상관없다.
do $$ begin
  create policy market_imports_read on public.market_imports
    for select using (user_id = auth.uid());
exception when duplicate_object then null; end $$;

-- INSERT/UPDATE/DELETE 정책은 하나도 만들지 않는다. RLS 가 켜진 표에
-- 정책이 없으면 그 동작은 아무에게도 허용되지 않는다 — 그것이 목적이다.

-- ══════════════════════════════════════════════════════════════════
-- 보는 자리 — 화면이 그대로 받아 쓰는 모양
-- ══════════════════════════════════════════════════════════════════

-- 시세판 한 줄 = 모델 하나.
--
-- 아무 일도 없는 모델은 빼둔다. 44줄을 전부 띄우면 41줄이 빈칸이라
-- 무엇이 살아 있는 장인지 알 수 없다.
create or replace view public.market_quotes
with (security_invoker = true) as
with supply as (
  select faction, rarity, variant,
         count(*)::int as supply,
         count(*) filter (where status = 'LISTED')::int as listed
    from public.market_sneakers group by 1, 2, 3
),
asks as (
  select s.faction, s.rarity, s.variant, min(l.price) as ask
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN' group by 1, 2, 3
),
bids as (
  select faction, rarity, variant, max(price) as bid, count(*)::int as bid_count
    from public.market_bids where status = 'OPEN' group by 1, 2, 3
),
last_trade as (
  select distinct on (faction, rarity, variant)
         faction, rarity, variant, price as last_price, traded_at as last_traded_at
    from public.market_trades
   order by faction, rarity, variant, traded_at desc
),
vol as (
  select faction, rarity, variant,
         count(*)::int as trades_24h,
         coalesce(sum(price), 0)::numeric(20, 4) as volume_24h
    from public.market_trades
   where traded_at > now() - interval '24 hours'
   group by 1, 2, 3
),
models as (
  select faction, rarity, variant from supply
  union
  select faction, rarity, variant from bids
)
select m.faction,
       m.rarity,
       m.variant,
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
  left join supply s using (faction, rarity, variant)
  left join asks a using (faction, rarity, variant)
  left join bids b using (faction, rarity, variant)
  left join last_trade t using (faction, rarity, variant)
  left join vol v using (faction, rarity, variant);

comment on view public.market_quotes is
  '모델별 시세 한 줄 — 즉시 구매가(ask) · 즉시 판매가(bid) · 최근 체결가.';

-- 매물 장부. 켤레마다 다른 값(레벨·민팅번호)을 함께 실어 보낸다.
create or replace view public.market_asks
with (security_invoker = true) as
  select l.id as listing_id,
         l.price,
         l.created_at,
         l.seller_id,
         s.id as sneaker_id,
         s.faction, s.rarity, s.variant,
         s.level, s.mint_number, s.luck, s.comfort, s.durability
    from public.market_listings l
    join public.market_sneakers s on s.id = l.sneaker_id
   where l.status = 'OPEN';

comment on view public.market_asks is '열려 있는 매물. 화면의 "판매 입찰" 목록.';

create or replace view public.market_bid_book
with (security_invoker = true) as
  select id as bid_id, buyer_id, faction, rarity, variant, min_level, price, created_at
    from public.market_bids
   where status = 'OPEN';

comment on view public.market_bid_book is '열려 있는 구매 입찰. 화면의 "구매 입찰" 목록.';

-- ══════════════════════════════════════════════════════════════════
-- 함수
-- ══════════════════════════════════════════════════════════════════

-- 로그인 확인 — 함수마다 같은 문장을 쓰지 않으려고 뺐다.
create or replace function public.market_me() returns uuid
language plpgsql stable security definer set search_path = public as $$
declare v uuid := auth.uid();
begin
  if v is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return v;
end $$;

-- 체결 한 번. 이 함수만이 소유자를 바꾸고 원장을 쓴다.
--
-- @param p_escrowed 산 쪽이 이미 입찰로 돈을 잠가 두었는가.
--   잠가 둔 돈은 이미 잔고에서 빠져 있으므로 여기서 또 빼면 두 번 낸다.
create or replace function public.market_settle(
  p_sneaker_id bigint,
  p_seller uuid,
  p_buyer uuid,
  p_price numeric,
  p_kind text,
  p_escrowed boolean
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_fee numeric(20, 4) := round(p_price * economy.market_fee_bps() / 10000.0, 4);
  v_trade bigint;
  v_s record;
begin
  select * into v_s from public.market_sneakers where id = p_sneaker_id for update;
  if not found then
    raise exception '없는 스니커즈입니다' using errcode = '22023';
  end if;

  update public.market_sneakers
     set owner_id = p_buyer, status = 'OWNED'
   where id = p_sneaker_id;

  -- 산 쪽: 잠가 둔 돈이 아니면 여기서 낸다
  if not p_escrowed then
    insert into public.sup_ledger (user_id, kind, amount, description)
    values (p_buyer, 'TRADE_BUY', -p_price, '스니커즈 구매 #' || v_s.mint_number);
  end if;

  -- 판 쪽: 값을 받고 수수료를 뗀다. 두 줄로 적어야 무엇을 얼마나 뗐는지 보인다.
  insert into public.sup_ledger (user_id, kind, amount, description)
  values (p_seller, 'TRADE_SELL', p_price, '스니커즈 판매 #' || v_s.mint_number);

  if v_fee > 0 then
    insert into public.sup_ledger (user_id, kind, amount, description)
    values (p_seller, 'TRADE_FEE', -v_fee, '거래 수수료');
  end if;

  insert into public.market_trades
    (sneaker_id, faction, rarity, variant, level, seller_id, buyer_id, price, fee, kind)
  values
    (p_sneaker_id, v_s.faction, v_s.rarity, v_s.variant, v_s.level,
     p_seller, p_buyer, p_price, v_fee, p_kind)
  returning id into v_trade;

  return v_trade;
end $$;

-- 함수만 부를 수 있어야 한다. 아무나 체결을 지어내면 안 된다.
-- Supabase 는 public 스키마의 새 함수에 anon · authenticated 실행 권한을 따로 붙인다.
-- public 에서만 거두면 그 권한이 남아, 누구나 이 함수로 SUP 를 만들고 남의 신발을 가져간다.
revoke all on function public.market_settle(bigint, uuid, uuid, numeric, text, boolean) from public, anon, authenticated;

comment on function public.market_settle(bigint, uuid, uuid, numeric, text, boolean) is
  '체결 한 번 — 소유자 이전, 원장 기록, 체결 내역. 다른 함수 안에서만 불린다.';

-- ── 등록 ──────────────────────────────────────────────────────────
--
-- 폰에 있던 신발을 거래소가 알게 한다. 이미 올린 것이면 레벨만 갱신한다
-- (강화하면 값이 오르므로 장부의 레벨도 따라가야 한다).
create or replace function public.market_import(
  p_local_id bigint,
  p_faction text,
  p_rarity text,
  p_variant int,
  p_level int,
  p_luck numeric,
  p_comfort numeric,
  p_durability int
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_id bigint;
  v_owner uuid;
  v_count int;
begin
  -- 두 요청이 동시에 세고 넣으면 상한을 넘는다 — 같은 사람의 요청은 한 줄로
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;

  if v_id is not null then
    select owner_id into v_owner from public.market_sneakers where id = v_id;
    -- 이미 판 신발이면 손대지 않는다. 지금 주인의 것이다. 레벨만 기념으로 따라간다 — 적립에는 1레벨로
    -- 친다(0022). 내구도는 서버가 정한다(폰 값으로 새것처럼 되돌리지 않는다).
    if v_owner = v_user then
      update public.market_sneakers
         set level = greatest(level, p_level)
       where id = v_id;
    end if;
    return v_id;
  end if;

  select count(*) into v_count from public.market_imports where user_id = v_user;
  if v_count >= economy.market_import_cap() then
    raise exception '올릴 수 있는 수를 넘었습니다 (최대 %)', economy.market_import_cap()
      using errcode = '22023';
  end if;

  insert into public.market_sneakers
    (owner_id, faction, rarity, variant, level, luck, comfort, durability)
  values
    (v_user, p_faction, p_rarity, p_variant, p_level, p_luck, p_comfort, p_durability)
  returning id into v_id;

  insert into public.market_imports (user_id, local_id, sneaker_id)
  values (v_user, p_local_id, v_id);

  return v_id;
end $$;

comment on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) is
  '폰의 신발을 거래소 장부에 올린다. 같은 신발은 한 번만, 계정당 상한이 있다.';

-- ── 판매 입찰 ─────────────────────────────────────────────────────
create or replace function public.market_list(
  p_sneaker_id bigint,
  p_price numeric
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_bid record;
  v_id bigint;
begin
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
    return null;
  end if;

  insert into public.market_listings (sneaker_id, seller_id, price)
  values (p_sneaker_id, v_user, p_price)
  returning id into v_id;

  update public.market_sneakers set status = 'LISTED' where id = p_sneaker_id;
  return v_id;
end $$;

comment on function public.market_list(bigint, numeric) is
  '내 스니커즈를 판다. 이미 그 값 이상을 부른 구매 입찰이 있으면 바로 체결된다.';

create or replace function public.market_cancel_listing(p_listing_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_l record;
begin
  select * into v_l from public.market_listings where id = p_listing_id for update;
  if not found or v_l.seller_id is distinct from v_user then
    raise exception '내 매물이 아닙니다' using errcode = '42501';
  end if;
  if v_l.status <> 'OPEN' then
    return;
  end if;

  update public.market_listings
     set status = 'CANCELLED', closed_at = now() where id = p_listing_id;
  update public.market_sneakers
     set status = 'OWNED' where id = v_l.sneaker_id;
end $$;

comment on function public.market_cancel_listing(bigint) is '내 매물을 거둔다.';

-- ── 즉시 구매 ─────────────────────────────────────────────────────
create or replace function public.market_buy_now(p_listing_id bigint)
returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_l record;
  v_balance numeric;
begin
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

  return public.market_settle(
    v_l.sneaker_id, v_l.seller_id, v_user, v_l.price, 'BY_ASK', false
  );
end $$;

comment on function public.market_buy_now(bigint) is
  '매물을 그 값에 산다. 값은 매물이 정한 값이다.';

-- ── 구매 입찰 ─────────────────────────────────────────────────────
create or replace function public.market_bid(
  p_faction text,
  p_rarity text,
  p_variant int,
  p_min_level int,
  p_price numeric
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_ask record;
  v_balance numeric;
  v_id bigint;
begin
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
    return null;
  end if;

  -- 기다린다. 건 값은 잠근다 — 잠그지 않으면 같은 돈으로 열 곳에 입찰할 수 있다.
  insert into public.market_bids (buyer_id, faction, rarity, variant, min_level, price)
  values (v_user, p_faction, p_rarity, p_variant, p_min_level, p_price)
  returning id into v_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_LOCK', -p_price, '구매 입찰');

  return v_id;
end $$;

comment on function public.market_bid(text, text, int, int, numeric) is
  '모델에 값을 건다. 조건에 맞는 매물이 이미 있으면 그 값에 바로 산다.';

create or replace function public.market_cancel_bid(p_bid_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_b record;
begin
  select * into v_b from public.market_bids where id = p_bid_id for update;
  if not found or v_b.buyer_id is distinct from v_user then
    raise exception '내 입찰이 아닙니다' using errcode = '42501';
  end if;
  if v_b.status <> 'OPEN' then
    return;
  end if;

  update public.market_bids
     set status = 'CANCELLED', closed_at = now() where id = p_bid_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (v_user, 'ESCROW_UNLOCK', v_b.price, '구매 입찰 취소');
end $$;

comment on function public.market_cancel_bid(bigint) is '내 구매 입찰을 거두고 잠긴 SUP 를 푼다.';

-- ── 즉시 판매 ─────────────────────────────────────────────────────
create or replace function public.market_sell_now(
  p_sneaker_id bigint,
  p_bid_id bigint
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_s record;
  v_b record;
  v_trade bigint;
begin
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
  return v_trade;
end $$;

comment on function public.market_sell_now(bigint, bigint) is
  '걸려 있는 구매 입찰에 내 스니커즈를 판다. 값은 그 입찰의 값이다.';

-- ── 내 것 보기 ────────────────────────────────────────────────────
--
-- 뷰가 아니라 함수인 이유: 앱이 한 번에 받아야 하는 것이 "내 신발 + 내 매물
-- + 내 입찰"인데, 세 번 물어보면 그 사이에 팔려서 앞뒤가 안 맞는 화면이 된다.
create or replace function public.market_my_sneakers()
returns table (
  id bigint,
  local_id bigint,
  faction text,
  rarity text,
  variant int,
  level int,
  mint_number bigint,
  luck numeric,
  comfort numeric,
  durability int,
  status text,
  listing_id bigint,
  listing_price numeric
)
language sql stable security definer set search_path = public as $$
  select s.id,
         i.local_id,
         s.faction, s.rarity, s.variant, s.level, s.mint_number,
         s.luck, s.comfort, s.durability,
         s.status,
         l.id,
         l.price
    from public.market_sneakers s
    left join public.market_imports i on i.sneaker_id = s.id and i.user_id = s.owner_id
    left join public.market_listings l on l.sneaker_id = s.id and l.status = 'OPEN'
   where s.owner_id = auth.uid()
   order by s.created_at desc
$$;

comment on function public.market_my_sneakers() is
  '거래소가 아는 내 스니커즈. 판매 중이면 그 매물도 함께 온다.';

create or replace function public.market_my_bids()
returns table (
  id bigint,
  faction text,
  rarity text,
  variant int,
  min_level int,
  price numeric,
  created_at timestamptz
)
language sql stable security definer set search_path = public as $$
  select id, faction, rarity, variant, min_level, price, created_at
    from public.market_bids
   where buyer_id = auth.uid() and status = 'OPEN'
   order by created_at desc
$$;

comment on function public.market_my_bids() is '내가 걸어 둔 구매 입찰.';

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
         seller_id = auth.uid() as sold,
         traded_at
    from public.market_trades
   where seller_id = auth.uid() or buyer_id = auth.uid()
   order by traded_at desc
   limit greatest(1, least(coalesce(p_limit, 30), 100))
$$;

comment on function public.market_my_trades(int) is '내가 사고판 기록.';
