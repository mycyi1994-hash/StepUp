-- ════════════════════════════════════════════════════════════════════
--  0098 — 11.11 워킹데이 리더보드 걸음 순위에 제출분(대기)도 (2026-11-07)
--
--  사용자 "제출분도 넣어" — 승인 전이라 비어 보이던 걸음 순위를 공개 합계(0095)처럼 대기 + 승인으로 센다(거절은 빠진다).
--  친구 초대 순위는 경품이 걸려 있어 0096 그대로(11,111 → 0097 부터 1,111걸음 승인된 친구 수). 반환 모양은 0096 그대로.
-- ════════════════════════════════════════════════════════════════════
create or replace function public.walkingday_leaderboard() returns jsonb
language sql stable security definer set search_path = ''
as $$
  select jsonb_build_object(
    'invite', coalesce((select jsonb_agg(jsonb_build_object('rank', x.rk, 'who', economy.walkingday_mask_email(x.email), 'friends', x.friends) order by x.n)
      from (select t.email, t.friends,
                   rank() over (order by t.friends desc)::int as rk,
                   row_number() over (order by t.friends desc, t.tickets desc, t.registered_at) as n
            from economy.walkingday_tickets() t where t.friends > 0) x where x.n <= 11), '[]'::jsonb),
    'steps', coalesce((select jsonb_agg(jsonb_build_object('rank', y.rk, 'who', economy.walkingday_mask_email(y.email), 'steps', y.steps, 'days', y.days) order by y.n)
      from (select w.email, s.steps, s.days,
                   rank() over (order by s.steps desc)::int as rk,
                   row_number() over (order by s.steps desc, s.first_at) as n
            from (select p.entry_id, sum(p.steps)::bigint as steps, count(*)::int as days, min(p.submitted_at) as first_at
                    from economy.walkingday_proofs p where p.status in ('pending', 'approved') and p.epoch_day between 20764 and 20768
                   group by p.entry_id) s
            join public.waitlist_entries w on w.id = s.entry_id and w.status = 'registered') y where y.n <= 10), '[]'::jsonb),
    'updated_at', now())
$$;
revoke all on function public.walkingday_leaderboard() from public;
grant execute on function public.walkingday_leaderboard() to anon, authenticated;
