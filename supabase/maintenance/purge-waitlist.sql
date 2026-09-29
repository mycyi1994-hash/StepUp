\set ON_ERROR_STOP on

-- 하루 단위 예약 실행 지연을 고려해 출시 89일 뒤 지워 90일 이내를 지킨다.
delete from public.waitlist_entries
where nullif(:'launch_date', '')::date is not null
  and now() >= nullif(:'launch_date', '')::date + interval '89 days';
