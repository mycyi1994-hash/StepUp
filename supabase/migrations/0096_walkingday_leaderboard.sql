-- ════════════════════════════════════════════════════════════════════
--  0096 — 11.11 워킹데이 공개 리더보드 (2026-11-07)
--
--  이벤트 페이지 경품 칸 아래 「지금 순위」. 이메일은 가려서만 내보낸다(앞 두 글자 + ** + 끝 한 글자 @ 흔한 메일 도메인만, 나머지 도메인은 *** — 예: ab**d@gmail.com).
--  · invite: 친구 초대 순위 — walkingday_tickets() 와 같은 규칙(11,111걸음 승인된 친구 수 → 응모권 → 먼저 등록), 1명 이상만, 위 11명
--  · steps : 걸음 순위 — 승인된 걸음의 합(11/7 ~ 11/11), 위 10명. 대기 중인 걸음은 넣지 않는다(가짜 캡처가 공개 순위에 먼저 오르지 않게)
--  같은 값이면 같은 순위(rank). 개인 이메일 원문 · 응모권 수는 나가지 않는다.
-- ════════════════════════════════════════════════════════════════════

create or replace function economy.walkingday_mask_email(p text) returns text
language sql immutable set search_path = '' as $$
  select case when position('@' in coalesce(p, '')) = 0 then '***'
    else case when length(split_part(p, '@', 1)) <= 3 then left(split_part(p, '@', 1), 1) || '**'
              else left(split_part(p, '@', 1), 2) || '**' || right(split_part(p, '@', 1), 1) end || '@' ||
         case when split_part(p, '@', 2) in ('gmail.com', 'naver.com', 'daum.net', 'hanmail.net', 'kakao.com', 'nate.com',
                                             'icloud.com', 'me.com', 'outlook.com', 'hotmail.com', 'yahoo.com', 'proton.me', 'protonmail.com')
              then split_part(p, '@', 2) else '***' end
  end
$$;
revoke all on function economy.walkingday_mask_email(text) from public;

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
                    from economy.walkingday_proofs p where p.status = 'approved' and p.epoch_day between 20764 and 20768
                   group by p.entry_id) s
            join public.waitlist_entries w on w.id = s.entry_id and w.status = 'registered') y where y.n <= 10), '[]'::jsonb),
    'updated_at', now())
$$;
revoke all on function public.walkingday_leaderboard() from public;
grant execute on function public.walkingday_leaderboard() to anon, authenticated;
