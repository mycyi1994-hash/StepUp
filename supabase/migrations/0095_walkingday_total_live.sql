-- ════════════════════════════════════════════════════════════════════
--  0095 — 11.11 워킹데이 공개 합계를 제출 즉시 (2026-11-07)
--
--  사용자 "실시간 걸음수 반영 바로바로". 0090 의 walkingday_total() 은 승인된 걸음만 더해서, 제출해도 담당자가 승인하기 전까지
--  첫 화면 숫자가 오르지 않았다. 이제 제출된 걸음(대기 + 승인)을 바로 더하고, 거절되면 빠진다.
--  응모권(walkingday_tickets)은 그대로 승인 기준이다. 반환 칸(total · people · updated_at)은 0090 그대로.
-- ════════════════════════════════════════════════════════════════════
create or replace function public.walkingday_total()
returns table (total bigint, people int, updated_at timestamptz)
language sql stable security definer set search_path = ''
as $$
  select coalesce(sum(p.steps), 0)::bigint, count(distinct p.entry_id)::int, max(coalesce(p.reviewed_at, p.submitted_at))
  from economy.walkingday_proofs p where p.status in ('pending', 'approved')
$$;
revoke all on function public.walkingday_total() from public;
grant execute on function public.walkingday_total() to anon, authenticated;
