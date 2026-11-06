-- ════════════════════════════════════════════════════════════════════
--  0092 — 11.11 워킹데이 5일 개근 1.5배 (2026-11-07)
--  개근 = 11/7 ~ 11/11 다섯 날(epoch day 20764 ~ 20768) 모두 11,111걸음 이상 승인된 사람. 개근이면 응모권 합계 × 1.5(소수점 올림).
--  approved_days 는 이제 "11,111걸음 이상 승인된 날 수"(개근 = 5). 반환 칸(이름 · 순서 · 타입)은 0090 그대로라 create or replace 로 덮는다.
--  나머지 규칙(등록 1 · 달성 하루라도 3 · 11/11 달성 3 · 초대 둘 다 2)은 그대로. SNS 응모권의 개근 곱은 추첨기가 따로 한다.
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.walkingday_tickets()
returns table (entry_id uuid, email text, registered_at timestamptz, approved_days int, best_steps int,
               goal_met boolean, goal_on_1111 boolean, invited_by text, friends int, tickets int)
language sql stable security definer set search_path = ''
as $$
  with q as (
    select p.entry_id,
           count(distinct p.epoch_day) filter (where p.status = 'approved' and p.steps >= economy.walkingday_goal()
                                                 and p.epoch_day between 20764 and 20768)::int as approved_days,
           max(p.steps) filter (where p.status = 'approved') as best_steps,
           bool_or(p.status = 'approved' and p.steps >= economy.walkingday_goal()) as goal_met,
           bool_or(p.status = 'approved' and p.steps >= economy.walkingday_goal() and p.epoch_day = 20768) as goal_on_1111
    from economy.walkingday_proofs p group by p.entry_id
  ), e as (
    select w.id, w.email, w.created_at, w.referred_by,
           coalesce(q.approved_days, 0) as approved_days, q.best_steps,
           coalesce(q.goal_met, false) as goal_met, coalesce(q.goal_on_1111, false) as goal_on_1111
    from public.waitlist_entries w left join q on q.entry_id = w.id
    where w.status = 'registered'
  ), f as (
    select e.referred_by as id, count(*)::int as friends from e where e.referred_by is not null and e.goal_met group by e.referred_by
  ), t as (
    select e.id, e.email, e.created_at, e.approved_days, e.best_steps, e.goal_met, e.goal_on_1111,
           r.email as invited_by, coalesce(f.friends, 0) as friends,
           (1 + case when e.goal_met then 3 else 0 end + case when e.goal_on_1111 then 3 else 0 end
              + 2 * coalesce(f.friends, 0) + case when e.goal_met and r.id is not null then 2 else 0 end)::int as base
    from e
    left join f on f.id = e.id
    left join public.waitlist_entries r on r.id = e.referred_by and r.status = 'registered'
  )
  select t.id, t.email, t.created_at, t.approved_days, t.best_steps, t.goal_met, t.goal_on_1111,
         t.invited_by, t.friends,
         case when t.approved_days >= 5 then ceil(t.base * 1.5)::int else t.base end
  from t
$$;
revoke all on function economy.walkingday_tickets() from public;
