-- ════════════════════════════════════════════════════════════════════
--  0034 — 꺼내기 · 지갑 연결의 2단계 인증은 "방금" 한 것만 인정한다 (2026-09-25 점검)
--
--  지갑 페이지는 앱의 로그인 토큰으로 2단계 인증(TOTP)을 한다. Supabase 는 인증을 마친 로그인
--  자체를 aal2 로 올려, 그 뒤 앱이 새로 받는 토큰도 모두 aal2 다. 그래서 한 번 6자리를 넣은 뒤로는
--  앱 로그인(몇 달 가는 refresh 토큰)만 있으면 6자리 없이 꺼낼 수 있었다.
--  토큰의 amr 에 적힌 TOTP 시각이 15분 안일 때만 인정한다.
-- ════════════════════════════════════════════════════════════════════

create or replace function economy.mfa_ok() returns boolean
  language sql stable as $$
  select coalesce(auth.jwt() ->> 'aal', 'aal1') = 'aal2'
     and exists (
       select 1
         from jsonb_array_elements(
                case when jsonb_typeof(auth.jwt() -> 'amr') = 'array' then auth.jwt() -> 'amr' else '[]'::jsonb end) a
        where a ->> 'method' = 'totp'
          and (a ->> 'timestamp') ~ '^[0-9]+$'
          and (a ->> 'timestamp')::bigint >= extract(epoch from now())::bigint - 900)
$$;
