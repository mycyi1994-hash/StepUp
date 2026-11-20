-- 지난 14일 실제 사용 — 읽기만 한다(Supabase SQL 창에 그대로 붙여 넣는다).
-- 에너지 · 신발 수명 · 능력치 포인트가 실제로 얼마나 닿는지 본다(docs/economy-roots-2026-11-21.md "실제 숫자로 정할 것").
with runs as (
  select s.user_id, s.duration_sec / 60.0 as min, coalesce(s.verdict_reason, '') as why,
         (s.started_at at time zone 'Asia/Seoul')::date as day
    from public.walk_sessions s
   where s.started_at > now() - interval '14 days' and s.verdict <> 'VOID' and s.energy_pool <> 'CREW'
), days as (
  select user_id, day, sum(min) as min, count(*) as n from runs group by 1, 2
), shoes as (
  select * from public.market_sneakers where owner_id is not null and status <> 'BURNED'
), top as (
  select owner_id, max(level) as lv from shoes group by 1
), q as (
  select (select round(percentile_cont(p) within group (order by min)::numeric) from runs) as run_min,
         (select round(percentile_cont(p) within group (order by min)::numeric) from days) as day_min, p
    from unnest(array[0.25, 0.5, 0.75, 0.9]) p
)
select * from (values
  (1, '러닝 수 · 사람 수', (select count(*) || ' · ' || count(distinct user_id) from runs)),
  (2, '한 번에 달린 분 (25 · 50 · 75 · 90%)', (select string_agg(coalesce(run_min::text, '-'), ' · ' order by p) from q)),
  (3, '하루에 달린 분 (25 · 50 · 75 · 90%)', (select string_agg(coalesce(day_min::text, '-'), ' · ' order by p) from q)),
  (4, '하루에 두 번 넘게 달린 날 %', (select round(100.0 * count(*) filter (where n >= 2) / greatest(count(*), 1))::text from days)),
  (5, '신발이 먼저 닳은 러닝 %', (select round(100.0 * count(*) filter (where why like '%내구도가 다 닳%' or why like '%신발이 다 닳은 뒤%') / greatest(count(*), 1))::text from runs)),
  (6, '에너지가 끝난 러닝 %', (select round(100.0 * count(*) filter (where why like '%에너지를 다 썼습니다%') / greatest(count(*), 1))::text from runs)),
  (7, '가장 높은 신발 레벨별 사람 수 (1 · 2~4 · 5~9 · 10~19 · 20 넘게)', (select concat_ws(' · ', count(*) filter (where lv = 1), count(*) filter (where lv between 2 and 4),
        count(*) filter (where lv between 5 and 9), count(*) filter (where lv between 10 and 19), count(*) filter (where lv >= 20)) from top)),
  (8, '나눈 포인트 합 (효율 · 착화감 · 내구도)', (select concat_ws(' · ', coalesce(sum(pts_eff), 0), coalesce(sum(pts_comfort), 0), coalesce(sum(pts_dur), 0)) from shoes)),
  (9, '착화감에 포인트를 넣은 신발 수 · 사람 수', (select count(*) || ' · ' || count(distinct owner_id) from shoes where pts_comfort > 0))
) t(no, 항목, 값) order by no;
