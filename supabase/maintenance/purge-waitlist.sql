\set ON_ERROR_STOP on

-- 운영 동작 확인에 사용하는 예약 도메인 테스트 기록을 지운다.
delete from public.waitlist_entries
where email like 'stepup-codex-smoke-%@example.invalid';

-- 하루 단위 예약 실행 지연을 고려해 출시 89일 뒤 지워 90일 이내를 지킨다.
delete from public.waitlist_entries
where nullif(:'launch_date', '')::date is not null
  and now() >= nullif(:'launch_date', '')::date + interval '89 days';
