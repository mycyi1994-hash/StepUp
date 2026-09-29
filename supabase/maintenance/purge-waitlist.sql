\set ON_ERROR_STOP on

-- 확인 링크가 만료된 미확인 신청을 매일 지운다.
delete from public.waitlist_entries
where status = 'pending' and confirmation_expires_at < now();

-- 출시일을 설정하면 출시 90일 후 모든 대기 명단 기록을 지운다.
delete from public.waitlist_entries
where nullif(:'launch_date', '')::date is not null
  and now() >= nullif(:'launch_date', '')::date + interval '90 days';
