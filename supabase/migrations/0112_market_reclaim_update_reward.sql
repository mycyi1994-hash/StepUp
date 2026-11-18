-- ════════════════════════════════════════════════════════════════════
--  0112 — 거래소 매물 · 입찰 모두 거두기 · 앱 업데이트 보상(뽑기 5 + 상급 1) · 나중에 보낼 알림 (2026-11-18)
--
--  사용자 "모든 거래를 막고 거래 매물을 다 회수시키고" + "알림은 보내야 함, 근데 지금은 아님".
--  사용자 "앱업데이트 보상으로 신발 뽑기권 5개씩, 상급뽑기 1개씩 증정해 (18일 기준 이전에 실제로 코드로 등록한 사람만)" ·
--  "우리 새로운 버전을 만들고 있잖아. 만들어서 올려야 할 거 아니니"(스토어 판도 상급 뽑기 선물을 쓰게).
--
--  A. 거두기(한 번 — 표시 market_reclaim_20261118): 열린 매물은 모두 내리고 신발은 주인에게(OWNED), 열린 구매 입찰은 모두
--     거두고 묶인 SUP 를 돌려준다. 사람이 "거두기"를 누른 것과 똑같이 — market_cancel_listing · market_cancel_bid 의 몸통을
--     economy.market_cancel_listing_as · market_cancel_bid_as 로 옮기고 두 함수가 그것을 부른다(원장 줄 · 설명이 같다).
--     거래 멈춤(0108)은 그대로다. 누가 몇 개를 돌려받았는지는 economy.market_reclaim_20261118 에 남는다.
--  B. 업데이트 보상(한 번 — 표시 update_reward_20261118): 2026-11-18 00:00(한국 시각) 전에 접속 코드를 실제로 넣어 들어온 계정
--     (economy.access_grants.source = 'CODE' — 배포 때 있던 계정의 LEGACY 줄 · 운영자가 준 ADMIN · 공개 베타 WAITLIST 는 빼고,
--     막힌 권한 · 지운 계정도 뺀다)에 무료 뽑기(FREE) +5 · 상급 뽑기 선물(GIFT, 새 종류) +1. 받은 사람은
--     economy.update_reward_20261118 에 남는다.
--     상급 뽑기 선물(GIFT)은 지갑 없이 쓴다 — premium_draw 가 선물을 가장 먼저 쓰고, 없으면 예전처럼 지갑 혜택(BONUS → RUN,
--     지갑 · 체인 확인 그대로). 앱은 draw_status_v2 의 reward_left 로 그 수를 읽는다(draw_status 는 모양을 바꾸지 않는다).
--  C. 알림은 지금 보내지 않는다. 보낼 때(SQL Editor):
--       select admin_notice_send('MARKET_RECLAIMED');   -- 거래소에서 돌려받은 사람에게
--       select admin_notice_send('UPDATE_REWARD');      -- 업데이트 보상을 받은 사람에게(새 앱을 올린 뒤에)
--     한 사람에게 한 번만 간다(economy.notice_sends). 받을 폰이 없거나 푸시를 끈 사람은 적어 두지 않는다 — 다시 부르면
--     그 사이 폰을 등록한 사람에게만 간다. push-send 함수를 다시 올려야 새 종류 글이 나간다.
-- ════════════════════════════════════════════════════════════════════

-- ══════════════════════════════════════════════════════════════════
-- A. 거래소 거두기
-- ══════════════════════════════════════════════════════════════════

-- 매물 거두기 — 0007 market_cancel_listing 의 몸통 그대로, 누구의 일인지만 받는다. 거뒀으면 true(이미 끝난 매물은 false)
create or replace function economy.market_cancel_listing_as(p_user uuid, p_listing_id bigint)
returns boolean
language plpgsql security definer set search_path = public as $$
declare
  v_l record;
begin
  select * into v_l from public.market_listings where id = p_listing_id for update;
  if not found or v_l.seller_id is distinct from p_user then
    raise exception '내 매물이 아닙니다' using errcode = '42501';
  end if;
  if v_l.status <> 'OPEN' then
    return false;
  end if;

  update public.market_listings
     set status = 'CANCELLED', closed_at = now() where id = p_listing_id;
  update public.market_sneakers
     set status = 'OWNED' where id = v_l.sneaker_id;
  return true;
end $$;
revoke all on function economy.market_cancel_listing_as(uuid, bigint) from public;

-- 구매 입찰 거두기 — 0007 market_cancel_bid 의 몸통 그대로. 돌려준 SUP(이미 끝난 입찰은 0)
create or replace function economy.market_cancel_bid_as(p_user uuid, p_bid_id bigint)
returns numeric
language plpgsql security definer set search_path = public as $$
declare
  v_b record;
begin
  select * into v_b from public.market_bids where id = p_bid_id for update;
  if not found or v_b.buyer_id is distinct from p_user then
    raise exception '내 입찰이 아닙니다' using errcode = '42501';
  end if;
  if v_b.status <> 'OPEN' then
    return 0;
  end if;

  update public.market_bids
     set status = 'CANCELLED', closed_at = now() where id = p_bid_id;

  insert into public.sup_ledger (user_id, kind, amount, description)
  values (p_user, 'ESCROW_UNLOCK', v_b.price, '구매 입찰 취소');
  return v_b.price;
end $$;
revoke all on function economy.market_cancel_bid_as(uuid, bigint) from public;

-- 앱이 부르는 두 함수 — 모양 · 뜻은 0007 그대로(같은 몸통을 부른다)
create or replace function public.market_cancel_listing(p_listing_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform economy.market_cancel_listing_as(public.market_me(), p_listing_id);
end $$;
comment on function public.market_cancel_listing(bigint) is '내 매물을 거둔다.';

create or replace function public.market_cancel_bid(p_bid_id bigint)
returns void
language plpgsql security definer set search_path = public as $$
begin
  perform economy.market_cancel_bid_as(public.market_me(), p_bid_id);
end $$;
comment on function public.market_cancel_bid(bigint) is '내 구매 입찰을 거두고 잠긴 SUP 를 푼다.';

-- 거두기로 돌려받은 사람 — 나중에 보낼 알림(MARKET_RECLAIMED)이 읽는다. 앱은 읽지 못한다
create table if not exists economy.market_reclaim_20261118 (
  user_id uuid primary key references auth.users on delete cascade,
  listings int not null default 0 check (listings >= 0),
  bids int not null default 0 check (bids >= 0),
  refunded numeric(20, 4) not null default 0 check (refunded >= 0),
  reclaimed_at timestamptz not null default now()
);
revoke all on economy.market_reclaim_20261118 from public, anon, authenticated;
comment on table economy.market_reclaim_20261118 is
  '2026-11-18 거래소 거두기(0112)로 돌려받은 사람 — 내린 매물 수 · 거둔 입찰 수 · 돌려받은 SUP.';

/*
 * 한 번만 — 열린 매물 · 열린 구매 입찰을 모두 거둔다. 이미 했으면(표시 market_reclaim_20261118) null.
 * 돌려주는 값: { listings, bids, sup, users }.
 */
create or replace function economy.market_reclaim_20261118()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  r record;
  v_amount numeric;
  v_listings int := 0;
  v_bids int := 0;
  v_sup numeric := 0;
  v_out jsonb;
begin
  if exists (select 1 from public.economy_settings where key = 'market_reclaim_20261118') then
    return null;
  end if;

  for r in select l.id, l.seller_id from public.market_listings l where l.status = 'OPEN' order by l.id loop
    if economy.market_cancel_listing_as(r.seller_id, r.id) then
      insert into economy.market_reclaim_20261118 as m (user_id, listings) values (r.seller_id, 1)
      on conflict (user_id) do update set listings = m.listings + 1;
      v_listings := v_listings + 1;
    end if;
  end loop;

  for r in select b.id, b.buyer_id from public.market_bids b where b.status = 'OPEN' order by b.id loop
    v_amount := economy.market_cancel_bid_as(r.buyer_id, r.id);
    if v_amount > 0 then
      insert into economy.market_reclaim_20261118 as m (user_id, bids, refunded) values (r.buyer_id, 1, v_amount)
      on conflict (user_id) do update set bids = m.bids + 1, refunded = m.refunded + excluded.refunded;
      v_bids := v_bids + 1;
      v_sup := v_sup + v_amount;
    end if;
  end loop;

  v_out := jsonb_build_object('listings', v_listings, 'bids', v_bids, 'sup', v_sup,
                              'users', (select count(*) from economy.market_reclaim_20261118));
  insert into public.admin_audit (actor, action, target, detail) values (null, 'market_reclaim', '20261118', v_out);
  insert into public.economy_settings (key, value) values ('market_reclaim_20261118', 'true'::jsonb)
  on conflict (key) do nothing;
  return v_out;
end $$;
revoke all on function economy.market_reclaim_20261118() from public;

-- ══════════════════════════════════════════════════════════════════
-- B. 앱 업데이트 보상 — 상급 뽑기 선물(GIFT)
-- ══════════════════════════════════════════════════════════════════

-- 운영자 · 업데이트 선물로 준 상급 뽑기('GIFT') — 지갑 없이 쓴다. 0042 의 목록 + GIFT(최종 목록이라 기존 줄까지 검사한다.
-- 0042 는 not valid 로 건다 — 다시 붙여넣을 때 GIFT 줄이 있어도 거기서 멈추지 않게)
alter table public.draw_grants drop constraint if exists draw_grants_kind_check;
alter table public.draw_grants add constraint draw_grants_kind_check check (kind in ('FREE', 'BONUS', 'RUN', 'GIFT'));

-- 이 시각 전에 접속 코드를 넣은 계정만(2026-11-18 00:00 한국 시각 — 사용자 "18일 기준 이전")
create or replace function economy.update_reward_cutoff() returns timestamptz
  language sql immutable as $$ select timestamptz '2026-11-18 00:00:00+09' $$;

-- 업데이트 보상을 받은 사람 — 나중에 보낼 알림(UPDATE_REWARD)이 읽는다. 앱은 읽지 못한다
create table if not exists economy.update_reward_20261118 (
  user_id uuid primary key references auth.users on delete cascade,
  free int not null check (free >= 0),
  premium int not null check (premium >= 0),
  granted_at timestamptz not null default now()
);
revoke all on economy.update_reward_20261118 from public, anon, authenticated;
comment on table economy.update_reward_20261118 is
  '2026-11-18 앱 업데이트 보상(0112)을 받은 사람 — 무료 뽑기 · 상급 뽑기 선물 수.';

/*
 * 한 번만 — 업데이트 보상을 준다. 이미 했으면(표시 update_reward_20261118) null, 했으면 받은 사람 수.
 *   대상: economy.access_grants 의 CODE 줄(코드를 실제로 넣은 계정 — LEGACY 줄을 코드로 바꾼 계정도 그때 CODE 가 된다)
 *         · 권한이 살아 있고(막힌 계정 빼기) · 그 시각이 update_reward_cutoff() 전. 지운 계정은 줄이 없다.
 *   무료 뽑기는 가입 선물과 같은 FREE 줄에 더한다. 아직 FREE 줄이 없는 계정(코드만 넣고 앱이 아직 첫 동기화를 못 함)은
 *   economy_bootstrap 이 줄 가입 선물(10회)을 먼저 같은 조건으로 넣는다 — 나중에 그 함수가 "이미 줄이 있다"며 건너뛰지 않게.
 */
create or replace function economy.update_reward_20261118()
returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  c_free constant int := 5;
  c_premium constant int := 1;
  v_since timestamptz := (economy.setting('free_draw_since') #>> '{}')::timestamptz;
  v_n int;
begin
  if exists (select 1 from public.economy_settings where key = 'update_reward_20261118') then
    return null;
  end if;

  insert into economy.update_reward_20261118 (user_id, free, premium)
  select g.user_id, c_free, c_premium
    from economy.access_grants g
   where g.source = 'CODE'
     and g.revoked_at is null
     and g.granted_at < economy.update_reward_cutoff()
     and exists (select 1 from auth.users u where u.id = g.user_id)
  on conflict (user_id) do nothing;
  get diagnostics v_n = row_count;

  -- 가입 선물(economy_bootstrap 0066 과 같은 조건) — FREE 줄이 아직 없는 계정만
  insert into public.draw_grants (user_id, kind, granted)
  select r.user_id, 'FREE', 10
    from economy.update_reward_20261118 r
    join public.profiles p on p.id = r.user_id
   where p.created_at >= v_since
     and not exists (select 1 from public.draw_grants d where d.user_id = r.user_id and d.kind = 'FREE')
  on conflict (user_id, kind) do nothing;

  insert into public.draw_grants as d (user_id, kind, granted)
  select r.user_id, 'FREE', r.free from economy.update_reward_20261118 r where r.free > 0
  on conflict (user_id, kind) do update set granted = d.granted + excluded.granted;

  insert into public.draw_grants as d (user_id, kind, granted)
  select r.user_id, 'GIFT', r.premium from economy.update_reward_20261118 r where r.premium > 0
  on conflict (user_id, kind) do update set granted = d.granted + excluded.granted;

  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'update_reward', '20261118',
          jsonb_build_object('users', v_n, 'free', c_free, 'premium', c_premium, 'cutoff', economy.update_reward_cutoff()));
  insert into public.economy_settings (key, value) values ('update_reward_20261118', 'true'::jsonb)
  on conflict (key) do nothing;
  return v_n;
end $$;
revoke all on function economy.update_reward_20261118() from public;

/*
 * 상급 뽑기 한 번 — 0044 + 선물(GIFT)부터(0112).
 *   1) 선물(GIFT) — 지갑 · 체인 상태와 상관없이. 스토어 판(지갑 없음)이 보이는 상급 뽑기는 이것뿐이고, 업데이트 알림도
 *      이것을 말한다 — 누른 사람이 보는 수가 줄어야 한다.
 *   2) 선물이 없으면 0044 그대로: 지갑을 연결한 사람만 · 체인이 멈춰 있으면 기다린다 · 첫 연결 선물(BONUS, 첫 번 Genesis)
 *      → 러닝으로 받은 기회(RUN). 지갑 혜택은 지갑을 끊어도 남아(다시 연결하면 쓴다) 선물을 먼저 써도 잃는 것이 없다.
 * 선물 뽑기도 같은 상급 표(레어 이상)에서 굴리고 출처는 BONUS_DRAW — 신발 체인 기록은 vault_mint_enqueue(shoe_chain_enabled)가 정한다.
 */
create or replace function public.premium_draw()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_genesis boolean := false;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  update public.draw_grants set used = used + 1
   where user_id = v_user and kind = 'GIFT' and used < granted;
  if not found then
    -- 지갑을 연결한 사람만(지갑 혜택), 체인이 멈춰 있으면 기다린다 — 예전과 같은 조건 · 같은 거절
    perform economy.withdraw_gate(v_user, false);

    select g.genesis_used < g.genesis_granted into v_genesis
      from public.draw_grants g
     where g.user_id = v_user and g.kind = 'BONUS' and g.used < g.granted;
    if found then
      update public.draw_grants
         set used = used + 1,
             genesis_used = genesis_used + case when v_genesis then 1 else 0 end
       where user_id = v_user and kind = 'BONUS';
    else
      v_genesis := false;
      update public.draw_grants set used = used + 1
       where user_id = v_user and kind = 'RUN' and used < granted;
      if not found then
        raise exception '상급 뽑기가 남아 있지 않습니다' using errcode = '23514';
      end if;
    end if;
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  perform economy.vault_mint_enqueue(v_id);
  return v_id;
end $$;
revoke all on function public.premium_draw() from public, anon;
grant execute on function public.premium_draw() to authenticated;

/*
 * 뽑기 화면 현황 2판 — draw_status(0042) 그대로 + reward_left(선물로 받은 상급 뽑기 중 남은 수, 지갑 없이 쓴다).
 * draw_status 는 예전 앱이 읽고 다시 붙여넣을 때 앞 파일이 예전 모양으로 만들므로 모양을 바꾸지 않는다.
 */
create or replace function public.draw_status_v2()
returns table (
  daily_left int,
  daily_total int,
  signup_left int,
  signup_granted int,
  wallet_linked boolean,
  gift_on_link int,
  gift_left int,
  run_left int,
  genesis_left int,
  run_progress_m double precision,
  run_step_m int,
  run_today int,
  run_daily_cap int,
  chain_paused boolean,
  reward_left int
)
language sql stable security definer set search_path = public, economy as $$
  select s.daily_left, s.daily_total, s.signup_left, s.signup_granted, s.wallet_linked, s.gift_on_link, s.gift_left,
         s.run_left, s.genesis_left, s.run_progress_m, s.run_step_m, s.run_today, s.run_daily_cap, s.chain_paused,
         coalesce((select g.granted - g.used from public.draw_grants g
                    where g.user_id = auth.uid() and g.kind = 'GIFT'), 0)::int
    from public.draw_status() s
$$;
revoke all on function public.draw_status_v2() from public, anon;
grant execute on function public.draw_status_v2() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- C. 나중에 보낼 알림 — 한 사람에게 한 번
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.notice_sends (
  notice text not null,
  user_id uuid not null references auth.users on delete cascade,
  sent_at timestamptz not null default now(),
  primary key (notice, user_id)
);
revoke all on economy.notice_sends from public, anon, authenticated;
comment on table economy.notice_sends is '운영 알림(economy.notice_send)을 푸시 목록에 넣은 사람 — 같은 알림은 한 사람에게 한 번(0112).';

/*
 * 알림 하나를 대상에게 푸시 목록(push_outbox)으로 넣는다. 넣은 사람 수를 돌려준다.
 *   MARKET_RECLAIMED  거래소 거두기로 돌려받은 사람(economy.market_reclaim_20261118) — 신발 탭(`shoes`)
 *   UPDATE_REWARD     업데이트 보상을 받은 사람(economy.update_reward_20261118) — 뽑기(`draw`)
 * 푸시를 받을 수 있는 사람(push_allowed · 등록된 폰)만 넣고 적어 둔다 — 못 받은 사람은 다음에 다시 부르면 그때 받는다.
 */
create or replace function economy.notice_send(p_notice text)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  r record;
  v_n int := 0;
begin
  if p_notice is null or p_notice not in ('MARKET_RECLAIMED', 'UPDATE_REWARD') then
    raise exception '모르는 알림입니다: %', coalesce(p_notice, '') using errcode = '22023';
  end if;
  -- 같은 알림을 두 곳에서 동시에 보내도 한 사람에게 한 번
  perform pg_advisory_xact_lock(hashtext('notice:' || p_notice));

  for r in
    select m.user_id,
           jsonb_build_object('listings', m.listings::text, 'bids', m.bids::text,
                              'sup', rtrim(to_char(m.refunded, 'FM999,999,999,999,990.9999'), '.')) as args,
           'shoes' as link
      from economy.market_reclaim_20261118 m
     where p_notice = 'MARKET_RECLAIMED'
    union all
    select u.user_id, jsonb_build_object('free', u.free::text, 'premium', u.premium::text), 'draw'
      from economy.update_reward_20261118 u
     where p_notice = 'UPDATE_REWARD'
  loop
    continue when exists (select 1 from economy.notice_sends s where s.notice = p_notice and s.user_id = r.user_id);
    continue when not public.push_allowed(r.user_id, p_notice)
               or not exists (select 1 from public.push_tokens t where t.user_id = r.user_id);
    perform public.push_enqueue(r.user_id, p_notice, r.args, r.link);
    insert into economy.notice_sends (notice, user_id) values (p_notice, r.user_id) on conflict do nothing;
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;
revoke all on function economy.notice_send(text) from public;

-- 운영자 — 앱의 운영자 계정 또는 Supabase SQL Editor(로그인 없이 postgres). 누가 언제 보냈는지 admin_audit 에 남는다
create or replace function public.admin_notice_send(p_notice text)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare v_n int;
begin
  perform economy.access_admin_guard();
  v_n := economy.notice_send(p_notice);
  perform public.admin_log('notice_send', p_notice, jsonb_build_object('enqueued', v_n));
  return v_n;
end $$;
revoke all on function public.admin_notice_send(text) from public, anon;
grant execute on function public.admin_notice_send(text) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 한 번만 — 거두기 · 업데이트 보상(각 함수가 표시를 보고 두 번 하지 않는다)
-- ══════════════════════════════════════════════════════════════════
do $$ begin
  perform economy.market_reclaim_20261118();
  perform economy.update_reward_20261118();
end $$;
