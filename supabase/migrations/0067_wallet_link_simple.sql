-- ════════════════════════════════════════════════════════════════════
--  0067 — 지갑 연결을 간단하게 (2026-10-18)
--
--  사용자: "그냥 누르면 바로 web3 지갑 연결(MetaMask · OKX · Rabby …) 이런 거 뜨면 안 됨? 왜 이렇게까지 하는 건데"
--  → 앱에서 지갑을 고르면 그 지갑 앱의 브라우저로 연결 페이지가 열리고, 서명 한 번이면 연결된다.
--
--  - 처음 붙이는 지갑은 2단계 인증을 묻지 않는다. 돈이 나가는 꺼내기는 그대로 2단계 인증 · 연결(바꾼) 뒤 72시간 대기 ·
--    보안 알림을 거친다(0025 · 0034 withdraw_gate). 2단계 인증 등록이 없는 계정은 로그인만 뺏겨도 등록부터 할 수 있어
--    연결 때 묻는 것이 실제로 막아 주는 것이 적었다.
--  - 이미 붙은 지갑을 **다른 지갑으로 바꾸는 것**은 예전처럼 방금 한 2단계 인증이 있어야 한다(웹 지갑 페이지).
--  - 확인 번호는 그대로 10분 · 한 번만 쓴다(attester_wallet_link). 지갑 브라우저로 넘기는 것은 로그인 토큰이 아니라
--    계정 번호 + 이 확인 번호뿐이다 — 어테스터는 서명이 그 문장의 것인지 보고, 서버는 번호가 그 계정에 막 준 것인지 본다.
-- ════════════════════════════════════════════════════════════════════

create or replace function public.wallet_link_challenge()
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_nonce text := replace(gen_random_uuid()::text, '-', '');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 붙어 있는 지갑을 바꾸는 것만 2단계 인증 — 꺼내는 곳을 몰래 바꾸지 못하게
  if exists (select 1 from public.wallet_links where user_id = v_user) and not economy.mfa_ok() then
    raise exception '연결된 지갑을 바꾸려면 2단계 인증이 필요합니다' using errcode = '42501';
  end if;
  insert into public.wallet_link_nonces (user_id, nonce) values (v_user, v_nonce)
  on conflict (user_id) do update set nonce = excluded.nonce, created_at = now();
  return economy.wallet_link_message(v_user, v_nonce);
end $$;

revoke all on function public.wallet_link_challenge() from public, anon;
grant execute on function public.wallet_link_challenge() to authenticated;
