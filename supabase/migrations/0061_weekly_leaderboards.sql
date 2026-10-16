-- 0061 주간 리더보드(2026-10-17 전달본 — 크루 순위 · 개인 러닝 순위) — 읽기만 한다
--
-- 앱의 리더보드 세 화면 중 둘이 읽는다. 적립 · 정산 · 신발 소모 · 기록은 바꾸지 않는다(새 표도 없다).
--
-- 지킨 기존 규칙
--   · 주(week) — 크루 홈 "이번 주 함께"와 같은 한국 시간 월요일 00:00 부터 7일(crew_week_start). 두 순위가 같은 주를 쓴다.
--     (예전 순위표 leaderboard 의 WEEK 는 '지금부터 7일 전' 이라 달력 주가 아니다 — 이 화면은 그것을 쓰지 않는다.)
--   · 거리 — 서버가 정한 러닝 거리(walk_sessions.distance_meters). 무효(VOID)는 빼고, 판정 대기 · 의심은 센다(leaderboard 와 같다).
--   · 같은 값 — 기존 순위표처럼 이름 · id 순으로 줄을 세운다(크루는 crew_leaderboard 처럼 id 순). 공동 순위는 없다.
--   · 크루 — 크루 전체 거리로 줄을 세운다(crew_leaderboard 와 같다 — 1인당 거리가 아니다). 값은 크루 홈 "이번 주 함께"와
--     같다: 지금 크루원이 그 크루로 달린 러닝만(crew_week_stats). 신고로 숨겨진 크루는 뺀다.
--   · 우리 크루 — 대표 크루(home_crew: 가장 먼저 가입한, 숨겨지지 않은 크루).
--   · 지난주 순위 — 같은 규칙으로 지난주를 다시 센다(따로 저장하지 않는다). 지난주에 순위가 없었으면 null(0 이 아니다).
--
-- 없는 것 — 주간 포인트(P) 순위. P 가 무엇인지(적립 SUP 인지 따로 쌓는 점수인지) 정해지지 않았다. 앱은 '준비 중'으로 보인다.

-- 한 주의 시작(한국 시간 월요일 00:00). p_offset 주만큼 옮긴다(-1 = 지난주)
create or replace function public.lb_week_start(p_offset int default 0)
returns timestamptz
language sql
stable
as $$
  select public.crew_week_start() + make_interval(days => 7 * coalesce(p_offset, 0))
$$;

-- ────────────────────────────────────────────────────────────────────
--  개인 러닝 순위 — 이번 주 누적 거리
-- ────────────────────────────────────────────────────────────────────
--
-- 한 번에 화면 하나 분량을 같은 시각(as_of)에 센다 — 상위 줄 · 내 줄 · 바로 앞 · 내 앞뒤 둘 · 이번 주 내 러닝.
-- 바로 앞과의 차이는 앱이 이 답 안의 두 값으로 뺀다(같은 집계).
create or replace function public.weekly_runner_board(p_top int default 20)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  with bounds as (
    select public.lb_week_start(0) as w0, public.lb_week_start(-1) as wp, public.lb_week_start(1) as w1
  ),
  runs as (
    select s.user_id, s.started_at, s.distance_meters
      from public.walk_sessions s, bounds b
     where s.verdict <> 'VOID'
       and s.started_at >= b.wp
       and s.started_at < b.w1
  ),
  this_week as (
    select r.user_id, sum(r.distance_meters)::double precision as meters, count(*)::int as runs
      from runs r, bounds b
     where r.started_at >= b.w0
     group by r.user_id
    -- 0 m 는 순위에 넣지 않는다 — 0 으로 채운 줄이면 순위표가 아니라 가입자 명단이다
    having sum(r.distance_meters) > 0
  ),
  last_week as (
    select r.user_id, sum(r.distance_meters)::double precision as meters
      from runs r, bounds b
     where r.started_at < b.w0
     group by r.user_id
    having sum(r.distance_meters) > 0
  ),
  ranked as (
    select t.user_id, p.display_name, t.meters, t.runs,
           rank() over (order by t.meters desc, p.display_name, t.user_id)::int as rnk,
           count(*) over ()::int as total
      from this_week t
      join public.profiles p on p.id = t.user_id
  ),
  last_ranked as (
    select l.user_id, rank() over (order by l.meters desc, p.display_name, l.user_id)::int as rnk
      from last_week l
      join public.profiles p on p.id = l.user_id
  ),
  brd as (
    select r.rnk, r.total, r.user_id = auth.uid() as is_me,
           jsonb_build_object(
             'user_id', r.user_id,
             'name', coalesce(nullif(trim(r.display_name), ''), '러너'),
             'monogram', public.monogram_of(coalesce(nullif(trim(r.display_name), ''), '러너')),
             'rank', r.rnk,
             'previous_rank', l.rnk,
             'meters', r.meters,
             'runs', r.runs,
             'is_me', r.user_id = auth.uid()
           ) as j
      from ranked r
      left join last_ranked l on l.user_id = r.user_id
  ),
  me as (
    select rnk from brd where is_me
  ),
  mine as (
    select coalesce(sum(r.distance_meters), 0)::double precision as meters, count(*)::int as runs,
           coalesce(jsonb_agg(jsonb_build_object('started_at', r.started_at, 'meters', r.distance_meters)
                              order by r.started_at desc), '[]'::jsonb) as list
      from runs r, bounds b
     where r.user_id = auth.uid()
       and r.started_at >= b.w0
  )
  select jsonb_build_object(
    'week_start', ((select w0 from bounds) at time zone 'Asia/Seoul')::date,
    'week_end', ((select w1 from bounds) at time zone 'Asia/Seoul')::date,
    'timezone', 'Asia/Seoul',
    'as_of', now(),
    'total', coalesce((select max(total) from brd), 0),
    'me', (select j from brd where is_me),
    'ahead', (select j from brd where rnk = (select rnk from me) - 1),
    'nearby', coalesce((select jsonb_agg(j order by rnk) from brd
                         where rnk between (select rnk from me) - 2 and (select rnk from me) + 2), '[]'::jsonb),
    'top', coalesce((select jsonb_agg(j order by rnk) from brd
                      where rnk <= least(greatest(coalesce(p_top, 20), 1), 100)), '[]'::jsonb),
    'my_name', (select coalesce(nullif(trim(p.display_name), ''), '러너') from public.profiles p where p.id = auth.uid()),
    'my_meters', (select meters from mine),
    'my_runs', (select runs from mine),
    'my_run_list', (select list from mine)
  )
$$;

comment on function public.weekly_runner_board(int) is
  '이번 주(한국 시간 월요일부터) 누적 거리 순위 — 상위 p_top(최대 100) · 내 줄 · 바로 앞 · 내 앞뒤 둘 · 이번 주 내 러닝. 무효 제외, 지난주 순위는 같은 규칙으로 다시 센다.';

-- ────────────────────────────────────────────────────────────────────
--  크루 순위 — 이번 주 크루 전체 거리
-- ────────────────────────────────────────────────────────────────────
create or replace function public.weekly_crew_board(p_top int default 30)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  with bounds as (
    select public.lb_week_start(0) as w0, public.lb_week_start(-1) as wp, public.lb_week_start(1) as w1
  ),
  tagged as (
    -- 지금 크루원이 그 크루로 달린 러닝(크루 홈 "이번 주 함께"와 같은 규칙)
    select s.crew_id, s.user_id, s.started_at, s.distance_meters
      from public.walk_sessions s
      join public.crew_members m on m.crew_id = s.crew_id and m.user_id = s.user_id,
           bounds b
     where s.crew_id is not null
       and s.verdict <> 'VOID'
       and s.started_at >= b.wp
       and s.started_at < b.w1
  ),
  this_week as (
    select t.crew_id, sum(t.distance_meters)::double precision as meters, count(distinct t.user_id)::int as runners
      from tagged t, bounds b
     where t.started_at >= b.w0
     group by t.crew_id
    having sum(t.distance_meters) > 0
  ),
  last_week as (
    select t.crew_id, sum(t.distance_meters)::double precision as meters
      from tagged t, bounds b
     where t.started_at < b.w0
     group by t.crew_id
    having sum(t.distance_meters) > 0
  ),
  shown as (
    select w.crew_id, w.meters, w.runners
      from this_week w
     where not public.is_hidden('CREW', w.crew_id::text)
  ),
  ranked as (
    select c.id, c.name, c.monogram, c.image_bg, c.image_ver, w.meters, w.runners,
           rank() over (order by w.meters desc, c.id)::int as rnk,
           count(*) over ()::int as total
      from shown w
      join public.crews c on c.id = w.crew_id
  ),
  last_ranked as (
    select l.crew_id, rank() over (order by l.meters desc, l.crew_id)::int as rnk
      from last_week l
     where not public.is_hidden('CREW', l.crew_id::text)
  ),
  home as (
    select public.home_crew(auth.uid()) as crew_id
  ),
  brd as (
    select r.rnk, r.total, r.id = (select crew_id from home) as is_mine,
           jsonb_build_object(
             'crew_id', r.id,
             'name', r.name,
             'monogram', r.monogram,
             'image_bg', r.image_bg,
             'image_ver', r.image_ver,
             'has_image', exists (select 1 from public.crew_images i where i.crew_id = r.id),
             'rank', r.rnk,
             'previous_rank', l.rnk,
             'meters', r.meters,
             'runners', r.runners,
             'is_mine', r.id = (select crew_id from home)
           ) as j
      from ranked r
      left join last_ranked l on l.crew_id = r.id
  ),
  mine as (
    select rnk from brd where is_mine
  )
  select jsonb_build_object(
    'week_start', ((select w0 from bounds) at time zone 'Asia/Seoul')::date,
    'week_end', ((select w1 from bounds) at time zone 'Asia/Seoul')::date,
    'timezone', 'Asia/Seoul',
    'as_of', now(),
    'total', coalesce((select max(total) from brd), 0),
    'top', coalesce((select jsonb_agg(j order by rnk) from brd
                      where rnk <= least(greatest(coalesce(p_top, 30), 1), 100)), '[]'::jsonb),
    -- 우리 크루 — 이번 주 기록이 없으면 순위 없이(rank null · 0 m) 이름만. 크루가 없으면 null
    'my_crew', coalesce(
      (select j from brd where is_mine),
      (select jsonb_build_object(
                'crew_id', c.id, 'name', c.name, 'monogram', c.monogram, 'image_bg', c.image_bg, 'image_ver', c.image_ver,
                'has_image', exists (select 1 from public.crew_images i where i.crew_id = c.id),
                'rank', null, 'previous_rank', (select l.rnk from last_ranked l where l.crew_id = c.id),
                'meters', 0, 'runners', 0, 'is_mine', true)
         from public.crews c
        where c.id = (select crew_id from home))
    ),
    'nearby', coalesce((select jsonb_agg(j order by rnk) from brd
                         where rnk between (select rnk from mine) - 2 and (select rnk from mine) + 2), '[]'::jsonb)
  )
$$;

comment on function public.weekly_crew_board(int) is
  '이번 주(한국 시간 월요일부터) 크루 전체 거리 순위 — 상위 p_top(최대 100) · 우리 크루(대표 크루) · 그 앞뒤 둘. 지금 크루원이 그 크루로 달린 러닝만, 무효 · 숨겨진 크루 제외.';

revoke execute on function public.lb_week_start(int) from public, anon, authenticated;
revoke execute on function public.weekly_runner_board(int) from public, anon;
revoke execute on function public.weekly_crew_board(int) from public, anon;
grant execute on function public.weekly_runner_board(int) to authenticated;
grant execute on function public.weekly_crew_board(int) to authenticated;
