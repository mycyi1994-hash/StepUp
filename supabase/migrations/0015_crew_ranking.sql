-- 크루 순위 — 크루원 모두가 크루로 달린 거리를 서버에서 합친다.
--
-- 크루 순위는 이 폰에서 달린 크루 러닝만 셌다. 크루원이 열 명이어도 순위에는
-- 내 거리만 들어가서, 크루끼리 겨루는 표가 되지 못했다. 이제 러닝이 서버에
-- 올라갈 때 어느 크루로 달렸는지를 적고(session_tag_crew), 순위는 서버가 모든
-- 크루원의 기록으로 센다(crew_leaderboard).

alter table public.walk_sessions
  add column if not exists crew_id uuid references public.crews on delete set null;

create index if not exists walk_sessions_crew
  on public.walk_sessions (crew_id, started_at) where crew_id is not null;

comment on column public.walk_sessions.crew_id is
  '크루 러닝이었다면 그 크루. 러닝을 올린 뒤 session_tag_crew 로 적는다. 크루원일 때만 적힌다.';

-- 방금 올린 러닝에 크루를 적는다. 그 크루원이어야 하고, 한 번 적힌 크루는 바꾸지
-- 않는다 — 크루를 옮겨 다니며 같은 거리를 여러 크루에 얹지 못하게.
create or replace function public.session_tag_crew(p_started_at timestamptz, p_crew uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_crew is null or not public.is_crew_member(p_crew) then
    raise exception '이 크루의 멤버가 아닙니다' using errcode = '42501';
  end if;
  update public.walk_sessions
     set crew_id = p_crew
   where user_id = auth.uid() and started_at = p_started_at and crew_id is null;
  return found;
end;
$$;

-- 크루 순위. 모든 크루가 나온다 — 아직 안 달린 크루는 0 km 로.
-- 판정에서 무효(VOID)가 된 러닝은 빼고, 숨겨진 크루(신고 5건)는 뺀다.
create or replace function public.crew_leaderboard(p_period text default 'ALL')
returns table (
  crew_id uuid,
  km double precision,
  runs int,
  runners int
)
language sql
stable
security definer
set search_path = public
as $$
  select
    c.id,
    coalesce(sum(s.distance_meters), 0) / 1000.0,
    count(s.id)::int,
    count(distinct s.user_id)::int
  from public.crews c
  left join public.walk_sessions s
    on s.crew_id = c.id
   and s.verdict <> 'VOID'
   and s.started_at >= public.rank_period_start(p_period)
  where not public.is_hidden('CREW', c.id::text)
  group by c.id
  order by 2 desc, c.id
$$;

comment on function public.crew_leaderboard(text) is
  '크루별로 크루원 모두가 크루로 달린 거리를, p_period(DAY·WEEK·MONTH·ALL) 기간으로.';

revoke execute on function public.session_tag_crew(timestamptz, uuid) from public, anon;
revoke execute on function public.crew_leaderboard(text) from public, anon;
grant execute on function public.session_tag_crew(timestamptz, uuid) to authenticated;
grant execute on function public.crew_leaderboard(text) to authenticated;
