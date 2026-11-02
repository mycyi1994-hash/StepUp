-- 보안 감시(2026-11-02) — .github/workflows/security-watch.yml 이 한 시간마다 읽기만 한다.
-- 줄마다 "종류|내용". 아무 줄도 없으면 조용하다. 바꾸지 않는다(select 만).
-- 기준값은 psql 변수(-v)로 받는다: waitlist_hour · signup_hour · access_fail_hour
\set ON_ERROR_STOP on
\pset format unaligned
\pset tuples_only on
\pset fieldsep '|'

-- 1. 대기 명단 등록이 한 시간에 너무 많다(봇 · 자동 점검)
select 'waitlist_burst', format('최근 1시간 대기 명단 등록 %s건(기준 %s)', count(*), :'waitlist_hour')
  from public.waitlist_entries where created_at > now() - interval '1 hour'
having count(*) >= (:'waitlist_hour')::int;

-- 2. 같은 곳에서 횟수 제한에 걸렸다(0082 — 10분 6번 넘게 시도한 IP 해시)
select 'waitlist_blocked', format('최근 1시간 등록 제한에 걸린 곳 %s군데(가장 많은 곳 %s번 시도)', count(*), max(n))
  from (select ip_hash, count(*) n from economy.waitlist_attempts
         where at > now() - interval '1 hour' group by ip_hash
        having count(*) >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_10min'), 6)) t
having count(*) > 0;

-- 3. 접속 코드를 계속 틀리는 계정(무작위 대입)
select 'access_code_guessing', format('최근 1시간 접속 코드를 %s번 넘게 틀린 계정 %s개', :'access_fail_hour', count(*))
  from (select user_id from economy.access_attempts where at > now() - interval '1 hour'
         group by user_id having count(*) >= (:'access_fail_hour')::int) t
having count(*) > 0;

-- 4. 새 가입이 한 시간에 너무 많다
select 'signup_burst', format('최근 1시간 새 계정 %s개(기준 %s)', count(*), :'signup_hour')
  from auth.users where created_at > now() - interval '1 hour'
having count(*) >= (:'signup_hour')::int;

-- 5. 운영자 함수 사용(문 바꾸기 · 코드 만들기 · 차단 · 경제 설정) — 내가 한 것이 맞는지 본다
select 'admin_action', format('%s %s %s', to_char(created_at at time zone 'Asia/Seoul', 'MM-DD HH24:MI'), action, left(target, 60))
  from public.admin_audit where created_at > now() - interval '1 hour'
 order by created_at;
