-- ════════════════════════════════════════════════════════════════════
--  0108 — 거래소 거래 멈춤 (2026-11-17)
--
--  사용자 "일단 지금 판매하기 버튼도 비활성화 해놔야 함. 거래 하면 안 돼."
--
--  · 설정 market_trading_enabled(기본 false) — false 면 새 매물 · 새 구매 입찰 · 체결을 모두 받지 않는다.
--    서버가 막으므로 이미 깔린 예전 앱(스토어 판 26 등)에서도 거래가 일어나지 않는다.
--  · 막는 곳은 표 셋의 넣기(트리거) — market_listings(판매 등록) · market_bids(구매 입찰) · market_trades(체결:
--    즉시 구매 · 즉시 판매 · 걸려 있던 입찰에 바로 팔림이 모두 market_settle 의 이 한 줄을 지난다). 함수를 다시 쓰지 않는다.
--  · 이미 올린 매물 · 걸어 둔 입찰은 거둘 수 있다(market_cancel_listing · market_cancel_bid — 묶인 SUP 가 돌아온다).
--  · 보는 것(시세 · 매물 · 내 거래)은 그대로다.
--  · 다시 열려면: select admin_economy_set('market_trading_enabled', 'true');  (앱은 MarketFeature.TRADING 도 함께)
-- ════════════════════════════════════════════════════════════════════

-- 처음 설치할 때만 넣는다. 다시 붙여넣어도 운영 중에 바꾼 값을 덮지 않는다.
insert into public.economy_settings (key, value) values ('market_trading_enabled', 'false'::jsonb)
on conflict (key) do nothing;

create or replace function economy.market_trading_open() returns boolean
language sql stable security definer set search_path = public as $$
  select coalesce((select value = 'true'::jsonb from public.economy_settings where key = 'market_trading_enabled'), false)
$$;
revoke all on function economy.market_trading_open() from public;

create or replace function economy.market_trading_guard() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if not economy.market_trading_open() then
    raise exception '지금은 거래소를 쉬고 있습니다' using errcode = '55000';
  end if;
  return new;
end $$;
revoke all on function economy.market_trading_guard() from public;

drop trigger if exists market_listings_pause on public.market_listings;
create trigger market_listings_pause before insert on public.market_listings
  for each row execute function economy.market_trading_guard();

drop trigger if exists market_bids_pause on public.market_bids;
create trigger market_bids_pause before insert on public.market_bids
  for each row execute function economy.market_trading_guard();

drop trigger if exists market_trades_pause on public.market_trades;
create trigger market_trades_pause before insert on public.market_trades
  for each row execute function economy.market_trading_guard();
