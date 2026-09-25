-- ════════════════════════════════════════════════════════════════════
--  0027 — 어테스터 전용 로그인 계정
--
--  워커(attester)가 ATTESTER_EMAIL / ATTESTER_PASSWORD 로 로그인하는 계정이다.
--  이 계정만 attester_* 함수를 부를 수 있다(0025 economy.attester_guard).
--  사람이 앱에서 쓰는 계정을 여기에 두면 안 된다 — 대시보드 Authentication 에서
--  이 용도로만 만든 계정이다.
--
--  id 는 비밀이 아니다(비밀번호는 Cloudflare Secret 에만). setup.sql 을 다시 올릴
--  때마다 이 값으로 맞춰지므로, 계정을 바꿀 때는 이 파일을 고친다.
-- ════════════════════════════════════════════════════════════════════

update public.economy_settings
   set value = to_jsonb('389719d5-735c-4242-8e86-e26a2373d1d8'::text)
 where key = 'attester_user_id';
