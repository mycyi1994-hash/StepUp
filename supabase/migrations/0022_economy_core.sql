-- 서버가 경제의 정본이 된다 (A안) — 잔고 · 신발 · 에너지 · 설정.
--
-- 지금까지 SUP 와 신발은 폰이 먼저 정했다. 폰 값을 체인으로 꺼낼 수 있게 되면
-- 폰을 고친 사람이 진짜 토큰과 NFT 를 만든다. 그래서 이 파일부터는 **체인으로
-- 나갈 수 있는 모든 가치를 서버가 정한다.** 폰은 요청만 보내고 화면을 그린다.
--
-- 이 파일은 더하기만 한다. 지금 깔려 있는 앱이 부르는 함수는 그대로 둔다 —
-- main 에 합치면 바로 배포되므로, 앱이 바뀌기 전에 옛 앱이 깨지면 안 된다.
--
--   economy_settings     운영 값(상한 · 정지 스위치). 다시 배포하지 않고 바꾼다
--   economy.ledger_apply 잔고를 바꾸는 유일한 길. 사용자별로 줄을 세운다
--   market_sneakers      서버 신발 표. 이름은 그대로 두고 스탯·체인 상태를 더한다
--   energy_days          하루 에너지. 폰이 아니라 서버가 센다
--   boosts               부스터(XP ×2 · 스트릭 보호 · 에너지 셀) 기록

-- ══════════════════════════════════════════════════════════════════
-- 게임의 하루 — 한국 시간
-- ══════════════════════════════════════════════════════════════════
--
-- 에너지 리필, 하루 상한, 목표 보너스가 모두 이 하루를 쓴다. 저장은 시각으로
-- 하고 날짜는 읽을 때 계산하므로, 예전 UTC 기준에서 바꿔도 상한이 두 번 열리지
-- 않는다(같은 시각이 두 하루에 동시에 들어가지 않는다).
create or replace function economy.game_day(p_at timestamptz) returns date
  language sql immutable as $$ select (p_at at time zone 'Asia/Seoul')::date $$;

create or replace function economy.game_day_start(p_day date) returns timestamptz
  language sql immutable as $$ select p_day::timestamp at time zone 'Asia/Seoul' $$;

-- ══════════════════════════════════════════════════════════════════
-- 운영 값
-- ══════════════════════════════════════════════════════════════════
--
-- 상한과 정지 스위치는 사고가 났을 때 **바로** 바꿔야 한다. 함수 상수로 두면
-- 바꾸려고 배포를 기다려야 한다. 그래서 표에 둔다. 앱 권한으로는 읽지도 쓰지도
-- 못한다 — 관리자 함수와 서버 함수만 본다.
create table if not exists public.economy_settings (
  key text primary key,
  value jsonb not null,
  updated_at timestamptz not null default now()
);

comment on table public.economy_settings is
  '운영 값(상한·정지 스위치·시작 시각). 앱 권한으로는 못 본다. admin_economy_set 으로 바꾼다.';

alter table public.economy_settings enable row level security;
revoke all on public.economy_settings from anon, authenticated;

-- 처음 설치할 때만 넣는다. 다시 붙여넣어도 운영 중에 바꾼 값을 덮지 않는다.
insert into public.economy_settings (key, value) values
  -- 이 시각 뒤에 가입한 사람만 무료 뽑기 10회를 받는다. 처음 배포한 순간으로 굳는다.
  ('free_draw_since',            to_jsonb(now())),
  -- 사람별 하루 적립 상한(SUP). 걸음 상한(48,000)과 별도로 금액에도 선을 긋는다.
  ('daily_earn_cap',             '600'::jsonb),
  -- 가입 며칠까지를 신규로 보고 적립 상한을 절반으로 할지
  ('new_account_days',           '7'::jsonb),
  -- 꺼내기(체인) 상한과 조건
  ('withdraw_user_daily_sup',    '1000'::jsonb),
  ('withdraw_global_daily_sup',  '50000'::jsonb),
  ('mint_global_daily',          '500'::jsonb),
  ('withdraw_min_account_days',  '7'::jsonb),
  ('withdraw_min_km',            '20'::jsonb),
  ('wallet_cooldown_hours',      '72'::jsonb),
  ('free_shoe_lock_km',          '50'::jsonb),
  -- 서명 유효 시간(초)과, 만료 뒤 되돌리기 전에 더 기다리는 시간(초)
  ('op_deadline_sec',            '600'::jsonb),
  ('op_expire_margin_sec',       '1800'::jsonb),
  -- 정지 스위치. true 면 꺼내기·넣기·보너스 발행을 모두 멈춘다.
  ('chain_paused',               'false'::jsonb)
on conflict (key) do nothing;

create or replace function economy.setting(p_key text) returns jsonb
  language sql stable security definer set search_path = public as $$
  select value from public.economy_settings where key = p_key
$$;

create or replace function economy.setting_num(p_key text) returns numeric
  language sql stable security definer set search_path = public as $$
  select (value #>> '{}')::numeric from public.economy_settings where key = p_key
$$;

revoke all on function economy.setting(text) from public;
revoke all on function economy.setting_num(text) from public;

-- 관리자만 바꾼다. 누가 언제 무엇을 바꿨는지 admin_audit 에 남는다.
create or replace function public.admin_economy_set(p_key text, p_value jsonb)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform public.admin_guard();
  if not exists (select 1 from public.economy_settings where key = p_key) then
    raise exception '없는 설정입니다: %', p_key using errcode = '22023';
  end if;
  update public.economy_settings set value = p_value, updated_at = now() where key = p_key;
  perform public.admin_log('economy_set', p_key, jsonb_build_object('value', p_value));
end $$;

revoke all on function public.admin_economy_set(text, jsonb) from public, anon;
grant execute on function public.admin_economy_set(text, jsonb) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 원장 — 종류를 더하고, 잔고를 바꾸는 길을 하나로 모은다
-- ══════════════════════════════════════════════════════════════════
--
--   EARN_COURSE     코스 완주 보상
--   SPEND_DRAW      뽑기
--   SPEND_REPAIR    신발 수리 (태운다 — 누구에게도 가지 않는다)
--   CHAIN_WITHDRAW  체인으로 꺼냄 (음수)
--   CHAIN_REFUND    꺼내기가 체인에 안 올라가 만료됨 → 되돌림 (양수)
--   CHAIN_DEPOSIT   체인에서 넣음 (양수)
alter table public.sup_ledger drop constraint if exists sup_ledger_kind_check;
alter table public.sup_ledger add constraint sup_ledger_kind_check check (kind in (
  'EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL', 'EARN_COURSE',
  'SPEND_MINT', 'SPEND_UPGRADE', 'SPEND_BOOST', 'SPEND_DRAW', 'SPEND_REPAIR',
  'ESCROW_LOCK', 'ESCROW_UNLOCK', 'TRADE_BUY', 'TRADE_SELL', 'TRADE_FEE',
  'CHAIN_WITHDRAW', 'CHAIN_REFUND', 'CHAIN_DEPOSIT'
));

-- 같은 요청이 두 번 들어와도 한 번만 적히게 하는 꼬리표.
-- 체인 작업 번호 · 목표 보너스의 날짜 같은 것이 들어간다.
alter table public.sup_ledger add column if not exists ref text;
create unique index if not exists sup_ledger_ref_once
  on public.sup_ledger (user_id, ref) where ref is not null;

/*
 * 잔고를 바꾸는 유일한 길.
 *
 * 새로 만드는 함수는 모두 이것을 부른다. 잔고 검사와 기록 사이에 다른 요청이
 * 끼어들지 못하게 이 사람의 원장 잠금을 먼저 잡는다 — record_session ·
 * spend_sup · 거래소가 잡는 것과 같은 잠금이라 모두 한 줄로 선다.
 *
 * 앱 권한으로는 부를 수 없다. economy 스키마는 앱에 열려 있지 않고, 함수 권한도 거둔다.
 */
create or replace function economy.ledger_apply(
  p_user uuid,
  p_kind text,
  p_amount numeric,
  p_description text,
  p_ref text default null
) returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_balance numeric(20, 4);
begin
  if p_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_amount is null or p_amount = 0 then
    raise exception '금액이 올바르지 않습니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));

  if p_ref is not null and exists (
       select 1 from public.sup_ledger where user_id = p_user and ref = p_ref) then
    raise exception '이미 처리된 요청입니다' using errcode = '23505';
  end if;

  select coalesce(sum(amount), 0) into v_balance
    from public.sup_ledger where user_id = p_user;

  if p_amount < 0 and v_balance + p_amount < 0 then
    raise exception 'SUP가 부족합니다 (보유 %, 필요 %)', v_balance, -p_amount
      using errcode = '23514';
  end if;

  insert into public.sup_ledger (user_id, kind, amount, description, ref)
  values (p_user, p_kind, round(p_amount, 4), coalesce(p_description, ''), p_ref);

  return v_balance + round(p_amount, 4);
end $$;

revoke all on function economy.ledger_apply(uuid, text, numeric, text, text) from public;

create or replace function economy.balance_of(p_user uuid) returns numeric
  language sql stable security definer set search_path = public as $$
  select coalesce(sum(amount), 0)::numeric(20, 4) from public.sup_ledger where user_id = p_user
$$;
revoke all on function economy.balance_of(uuid) from public;

-- ══════════════════════════════════════════════════════════════════
-- 신발 규칙 — 등급 · 레벨 · 효율성 · 착화감 · 내구도
-- ══════════════════════════════════════════════════════════════════
--
-- 행운과 속성은 스탯에서 뺐다. 속성(불·물·번개·바람)은 모델 그림의 주제로만 남는다.
--
--            확률   최대Lv  효율성(Lv1)    착화감(Lv1)   내구도 -/km  수리계수
--   COMMON    55%     10    +2 ~  4%       0 ~  3%        1.0        1.00
--   RARE      28%     15    +4 ~  7%       3 ~  6%        0.9        1.25
--   EPIC      13%     20    +7 ~ 10%       6 ~  9%        0.8        1.50
--   LEGENDARY  4%     30    +10 ~ 13%      9 ~ 12%        0.7        1.75
--
-- 레벨 한 칸: 효율성 +0.5%p, 착화감 +0.2%p(최대 20%), 에너지 최대 +2칸.
-- 내구도 50 이상 정상 · 20~49 적립 -30% · 20 미만 적립 없음.
-- 값은 bps(1% = 100)로 적는다. 소수로 적으면 더할 때 오차가 쌓인다.

create or replace function economy.rarity_ord(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 1
                       when 'EPIC' then 2 when 'LEGENDARY' then 3 end
$$;

create or replace function economy.rarity_weight(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 55 when 'RARE' then 28
                       when 'EPIC' then 13 when 'LEGENDARY' then 4 end
$$;

create or replace function economy.max_level(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 10 when 'RARE' then 15
                       when 'EPIC' then 20 when 'LEGENDARY' then 30 end
$$;

-- 등급 안 모델 수. 앱 도감(domain/Sneaker.kt)과 같다 — 속성마다 13종.
create or replace function economy.variant_count(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 4 when 'RARE' then 4
                       when 'EPIC' then 3 when 'LEGENDARY' then 2 end
$$;

create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 200 when 'RARE' then 400
                       when 'EPIC' then 700 else 1000 end,
         case p_rarity when 'COMMON' then 400 when 'RARE' then 700
                       when 'EPIC' then 1000 else 1300 end
$$;

create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 300
                       when 'EPIC' then 600 else 900 end,
         case p_rarity when 'COMMON' then 300 when 'RARE' then 600
                       when 'EPIC' then 900 else 1200 end
$$;

create or replace function economy.efficiency_per_level_bps() returns int
  language sql immutable as $$ select 50 $$;
create or replace function economy.comfort_per_level_bps() returns int
  language sql immutable as $$ select 20 $$;
create or replace function economy.comfort_cap_bps() returns int
  language sql immutable as $$ select 2000 $$;

create or replace function economy.durability_loss_per_km(p_rarity text) returns numeric
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 1.0 when 'RARE' then 0.9
                       when 'EPIC' then 0.8 else 0.7 end::numeric
$$;

-- 내구도에 따른 적립 배율
create or replace function economy.durability_factor(p_durability numeric) returns numeric
  language sql immutable as $$
  select case when p_durability >= 50 then 1.0
              when p_durability >= 20 then 0.7
              else 0 end::numeric
$$;

-- 수리 1칸 값. 좋은 신발일수록 더 벌고 더 든다.
create or replace function economy.repair_cost_per_point(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round((1 + 0.25 * economy.rarity_ord(p_rarity)) * (1 + 0.05 * p_level), 4)
$$;

-- 강화 값 — 앱의 RewardEconomy.sneakerUpgradeCost 와 같다
create or replace function economy.upgrade_cost(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round(p_level * 100.0 * (1 + 0.25 * economy.rarity_ord(p_rarity)))
$$;

create or replace function economy.draw_cost() returns numeric
  language sql immutable as $$ select 500::numeric $$;

create or replace function economy.energy_max_for_level(p_level int) returns numeric
  language sql immutable as $$
  select economy.base_max_energy() + greatest(coalesce(p_level, 1) - 1, 0) * 2
$$;

-- ══════════════════════════════════════════════════════════════════
-- 서버 신발 표
-- ══════════════════════════════════════════════════════════════════
--
-- 거래소 장부(market_sneakers)를 그대로 신발의 정본으로 쓴다. 이미 소유자·
-- 등급·레벨을 들고 있고 거래소 함수가 이 표를 본다. 이름을 바꾸면 옛 앱이 부르는
-- 함수가 깨지므로 이름은 두고 칸을 더한다.
--
-- origin
--   IMPORT      폰에 있던 신발을 올린 것. 폰이 정한 값이라 **믿지 않는다** —
--               적립 계산에서는 일반 1레벨로 치고, 체인으로 꺼낼 수 없다.
--   STARTER     가입 때 주는 첫 신발. 꺼낼 수 없다.
--   FREE_DRAW   신규 무료 뽑기. 이 신발로 50km 를 달려야 꺼낼 수 있다.
--   PAID_DRAW   SUP 로 뽑은 신발
--   BONUS_DRAW  지갑 연결 보너스(체인에서 발행). 50km 전에는 전송이 잠긴다.
--   DEPOSIT     체인에서 처음 넣은 신발(남에게 산 NFT 등)
--   MINT        예전 표기. 쓰지 않는다.
alter table public.market_sneakers
  add column if not exists efficiency_bps int not null default 200,
  add column if not exists comfort_bps int not null default 0,
  add column if not exists durability_pts numeric(6, 2) not null default 100,
  add column if not exists equipped boolean not null default false,
  add column if not exists chain_state text not null default 'APP',
  add column if not exists token_id numeric(78, 0),
  add column if not exists genesis_no int,
  add column if not exists km_run numeric(10, 3) not null default 0,
  add column if not exists lock_km numeric(10, 3) not null default 0,
  add column if not exists withdrawable boolean not null default false,
  add column if not exists draw_nonce bigint,
  add column if not exists updated_at timestamptz not null default now();

alter table public.market_sneakers drop constraint if exists market_sneakers_origin_check;
alter table public.market_sneakers add constraint market_sneakers_origin_check check (origin in (
  'IMPORT', 'MINT', 'STARTER', 'FREE_DRAW', 'PAID_DRAW', 'BONUS_DRAW', 'DEPOSIT'
));

alter table public.market_sneakers drop constraint if exists market_sneakers_chain_state_check;
alter table public.market_sneakers add constraint market_sneakers_chain_state_check check (
  chain_state in ('APP', 'WITHDRAWING', 'ON_CHAIN', 'DEPOSITING')
);

alter table public.market_sneakers drop constraint if exists market_sneakers_stats_check;
alter table public.market_sneakers add constraint market_sneakers_stats_check check (
  efficiency_bps between 0 and 5000
  and comfort_bps between 0 and 2000
  and durability_pts between 0 and 100
  and km_run >= 0 and lock_km >= 0
);

create unique index if not exists market_sneakers_token_once
  on public.market_sneakers (token_id) where token_id is not null;
create unique index if not exists market_sneakers_genesis_once
  on public.market_sneakers (genesis_no) where genesis_no is not null;
-- 한 사람이 동시에 신는 신발은 하나
create unique index if not exists market_sneakers_one_equipped
  on public.market_sneakers (owner_id) where equipped;

create sequence if not exists public.genesis_seq start 1;
revoke all on sequence public.genesis_seq from anon, authenticated;

comment on table public.market_sneakers is
  '서버 신발 표 — 신발의 정본. 폰 목록은 사본이다. IMPORT 는 폰이 정한 값이라 적립·꺼내기에 쓰지 않는다.';

-- 적립 계산에 쓰는 실효 스탯. 폰이 정한 신발(IMPORT·MINT)은 일반 1레벨로 친다.
create or replace function economy.sneaker_effective(
  p_origin text, p_rarity text, p_level int,
  p_efficiency_bps int, p_comfort_bps int, p_durability numeric,
  out efficiency_bps int, out comfort_bps int, out level int, out durability numeric
) language sql immutable as $$
  select
    case when p_origin in ('IMPORT', 'MINT') then 200
         else p_efficiency_bps + economy.efficiency_per_level_bps() * greatest(p_level - 1, 0) end,
    case when p_origin in ('IMPORT', 'MINT') then 0
         else least(p_comfort_bps + economy.comfort_per_level_bps() * greatest(p_level - 1, 0),
                    economy.comfort_cap_bps()) end,
    case when p_origin in ('IMPORT', 'MINT') then 1 else p_level end,
    case when p_origin in ('IMPORT', 'MINT') then 100 else p_durability end
$$;

-- 지금 신고 있는 신발(서버 기준). 없거나 체인에 나가 있으면 null.
create or replace function economy.equipped_sneaker(p_user uuid)
returns setof public.market_sneakers
language sql stable security definer set search_path = public as $$
  select * from public.market_sneakers
   where owner_id = p_user and equipped and chain_state = 'APP' and status = 'OWNED'
   limit 1
$$;
revoke all on function economy.equipped_sneaker(uuid) from public;

-- 판매 중이거나 체인에 나가 있는 신발은 신을 수도, 강화할 수도, 다시 팔 수도
-- 없다(결정표 X7). 거래소 함수를 고치지 않고 표에서 막는다 — 새 매물을 거는
-- 순간 신발 상태를 본다.
create or replace function economy.guard_listing() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if exists (select 1 from public.market_sneakers
              where id = new.sneaker_id and (chain_state <> 'APP' or equipped)) then
    raise exception '착용 중이거나 체인에 있는 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  return new;
end $$;

drop trigger if exists market_listings_guard on public.market_listings;
create trigger market_listings_guard before insert on public.market_listings
  for each row execute function economy.guard_listing();

-- ══════════════════════════════════════════════════════════════════
-- 에너지 — 하루(한국 시간)마다 최대치로 찬다
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.energy_days (
  user_id uuid not null references auth.users on delete cascade,
  day date not null,
  -- 쓴 에너지(칸)
  used numeric(10, 4) not null default 0 check (used >= 0),
  -- 에너지 셀로 더 받은 칸
  bonus numeric(10, 4) not null default 0 check (bonus >= 0),
  primary key (user_id, day)
);

comment on table public.energy_days is
  '하루 에너지 사용량. 최대치는 신은 신발 레벨로 정해지고 한국 자정에 다시 찬다.';

alter table public.energy_days enable row level security;
revoke all on public.energy_days from anon, authenticated;
drop policy if exists energy_days_select_own on public.energy_days;
create policy energy_days_select_own on public.energy_days
  for select using ((select auth.uid()) = user_id);
grant select on public.energy_days to authenticated;

create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce((
    select e.level from public.market_sneakers s,
           lateral economy.sneaker_effective(s.origin, s.rarity, s.level,
             s.efficiency_bps, s.comfort_bps, s.durability_pts) e
     where s.owner_id = p_user and s.equipped and s.chain_state = 'APP' and s.status = 'OWNED'
     limit 1), 1))
$$;
revoke all on function economy.energy_max(uuid) from public;

-- 남은 에너지(칸)
create or replace function economy.energy_left(p_user uuid, p_day date) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select greatest(economy.energy_max(p_user)
                  + coalesce((select bonus - used from public.energy_days
                               where user_id = p_user and day = p_day), 0), 0)
$$;
revoke all on function economy.energy_left(uuid, date) from public;

-- ══════════════════════════════════════════════════════════════════
-- 부스터 — 앱의 BoostType 과 같은 값
-- ══════════════════════════════════════════════════════════════════
--   ENERGY_CELL    50 SUP   즉시 에너지 2칸
--   STREAK_SHIELD 120 SUP   24시간 동안 목표를 못 채워도 연속 기록 유지
--   XP_BOOSTER    200 SUP   24시간 동안 러닝 적립 ×2
create table if not exists public.boosts (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('ENERGY_CELL', 'STREAK_SHIELD', 'XP_BOOSTER')),
  starts_at timestamptz not null default now(),
  ends_at timestamptz not null default now(),
  cost numeric(20, 4) not null check (cost >= 0),
  created_at timestamptz not null default now()
);

create index if not exists boosts_user_active on public.boosts (user_id, ends_at desc);

alter table public.boosts enable row level security;
revoke all on public.boosts from anon, authenticated;
drop policy if exists boosts_select_own on public.boosts;
create policy boosts_select_own on public.boosts
  for select using ((select auth.uid()) = user_id);
grant select on public.boosts to authenticated;

create or replace function economy.boost_cost(p_kind text) returns numeric
  language sql immutable as $$
  select case p_kind when 'ENERGY_CELL' then 50 when 'STREAK_SHIELD' then 120
                     when 'XP_BOOSTER' then 200 end::numeric
$$;

create or replace function economy.boost_active(p_user uuid, p_kind text, p_at timestamptz)
returns boolean language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.boosts
                  where user_id = p_user and kind = p_kind
                    and starts_at <= p_at and ends_at > p_at)
$$;
revoke all on function economy.boost_active(uuid, text, timestamptz) from public;

create or replace function public.boost_buy(p_kind text)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_cost numeric := economy.boost_cost(p_kind);
  v_day date := economy.game_day(now());
  v_balance numeric;
  v_from timestamptz;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_cost is null then
    raise exception '없는 부스터입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

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

  return v_balance;
end $$;

revoke all on function public.boost_buy(text) from public, anon;
grant execute on function public.boost_buy(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 직접 쓰기 막기 (결정표 X4)
-- ══════════════════════════════════════════════════════════════════
--
-- 게시글·크루는 post_create · crew_create 가 도배 제한과 크루 10개 상한을
-- 검사한다. 표에 직접 INSERT 할 수 있으면 그 검사를 건너뛴다. 앱은 이미 두
-- 함수만 부르므로 직접 쓰기를 닫아도 바뀌는 것이 없다.
drop policy if exists posts_insert_own on public.posts;
drop policy if exists crews_insert_own on public.crews;
revoke insert on public.posts from anon, authenticated;
revoke insert on public.crews from anon, authenticated;

-- 거래소 표도 함수로만 바뀐다. 지금은 RLS 정책이 없어 막혀 있지만, 누가 정책을
-- 하나 잘못 붙이는 순간 열린다. 잔고와 소유권이 걸린 표라 권한부터 거둔다.
revoke insert, update, delete on public.market_sneakers, public.market_imports,
  public.market_listings, public.market_bids, public.market_trades from anon, authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 내 경제 한눈에 — 앱이 화면을 그릴 때 읽는다
-- ══════════════════════════════════════════════════════════════════
create or replace function public.my_economy()
returns table (
  balance numeric,
  energy_max numeric,
  energy_left numeric,
  xp_booster_until timestamptz,
  streak_shield_until timestamptz,
  equipped_id bigint,
  game_day date
)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query select
    economy.balance_of(v_user),
    economy.energy_max(v_user),
    economy.energy_left(v_user, v_day),
    (select max(b.ends_at) from public.boosts b
      where b.user_id = v_user and b.kind = 'XP_BOOSTER' and b.ends_at > now()),
    (select max(b.ends_at) from public.boosts b
      where b.user_id = v_user and b.kind = 'STREAK_SHIELD' and b.ends_at > now()),
    (select s.id from economy.equipped_sneaker(v_user) s),
    v_day;
end $$;

revoke all on function public.my_economy() from public, anon;
grant execute on function public.my_economy() to authenticated;
