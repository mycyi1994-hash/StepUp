-- ════════════════════════════════════════════════════════════════════
--  0031 — 신발 표에서 주인 칸을 가린다 (2026-09-25 점검)
--
--  market_sneakers 는 거래소 호가 · 매물 뷰(market_quotes · market_asks, 읽는 사람 권한)가 읽으므로
--  모두에게 열려 있다. 그런데 owner_id 와 token_id 가 같이 보여, 체인에서 그 신발을 가진 지갑을 찾으면
--  "이 계정의 지갑"이 드러났다(wallet_links 는 비공개인데도). 앱 · 웹은 이 표를 직접 읽지 않고
--  my_sneakers() · 뷰로만 읽는다. 주인 칸 · 뽑기 번호 · 체인 토큰 번호를 빼고 연다.
--  (호가 · 거래 기록은 판 사람 계정과 신발 번호를 보여 주므로, 토큰 번호가 열려 있으면
--   그 둘을 이어 체인의 지갑을 찾을 수 있다. 토큰 번호는 my_sneakers() 로 주인만 본다.)
-- ════════════════════════════════════════════════════════════════════

revoke select on public.market_sneakers from anon, authenticated;

do $$
declare cols text;
begin
  select string_agg(quote_ident(column_name), ', ' order by ordinal_position) into cols
    from information_schema.columns
   where table_schema = 'public' and table_name = 'market_sneakers'
     and column_name not in ('owner_id', 'draw_nonce', 'token_id');
  execute format('grant select (%s) on public.market_sneakers to anon, authenticated', cols);
end $$;
