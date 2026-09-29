\set ON_ERROR_STOP on

-- 확인 링크가 만료된 미확인 신청을 매일 지운다.
delete from public.waitlist_entries
where status = 'pending' and confirmation_expires_at < now();

-- 하루 단위 예약 실행 지연을 고려해 출시 89일 뒤 지워 90일 이내를 지킨다.
delete from public.waitlist_entries
where nullif(:'launch_date', '')::date is not null
  and now() >= nullif(:'launch_date', '')::date + interval '89 days';
