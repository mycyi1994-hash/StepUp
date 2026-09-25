-- 코스별 기록 순위 — 코스를 따라 달린 기록을 서버가 확인하고 순위를 낸다.
--
-- 앱은 "이 러닝이 이 코스였다"고 알려 줄 뿐이다. 서버는 이미 받은 러닝 경로가
-- 정말 코스를 따라갔는지(코스 지점의 80% 를 50m 안으로 지났는지) 직접 보고,
-- 러닝 시간을 그 코스 기록으로 남긴다. 코스의 "달린 횟수"도 여기서 센다.
--
-- 코스는 번호가 아니라 **길**로 찾는다. 앱은 게시판 코스를 받아 폰에 따로
-- 저장하므로 서버 번호를 모른다. 길 문자열(md5)은 둘이 똑같다.

create table if not exists public.course_runs (
  id bigint generated always as identity primary key,
  course_id bigint not null references public.courses on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  session_id bigint not null references public.walk_sessions on delete cascade,
  duration_sec int not null check (duration_sec > 0),
  created_at timestamptz not null default now(),
  unique (course_id, session_id)
);

create index if not exists course_runs_board on public.course_runs (course_id, duration_sec);
create index if not exists courses_track_md5 on public.courses (md5(track));

comment on table public.course_runs is
  '코스를 따라 달린 기록. 서버가 러닝 경로와 코스를 맞춰 본 것만 들어온다.';

alter table public.course_runs enable row level security;
-- 직접 읽고 쓰지 않는다. 아래 함수로만.
revoke all on public.course_runs from anon, authenticated;

-- 코스 지점 중 몇 %를 러닝 경로가 p_meters 안으로 지나갔나.
-- 코스 점은 최대 80개로 솎아서 본다 — 긴 코스도 계산이 금방 끝나게.
create or replace function economy.course_coverage(
  p_course text,
  p_track text,
  p_meters double precision default 50
)
returns double precision
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_course double precision[][] := '{}';
  v_run_lat double precision[] := '{}';
  v_run_lng double precision[] := '{}';
  v_parts text[];
  v_chunk text;
  v_n int;
  v_step int;
  v_hit int := 0;
  v_seen int := 0;
  v_lat double precision;
  v_lng double precision;
  v_deg double precision := p_meters / 111000.0;
  i int;
  j int;
begin
  foreach v_chunk in array string_to_array(coalesce(p_track, ''), ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_run_lat := v_run_lat || v_parts[1]::double precision;
    v_run_lng := v_run_lng || v_parts[2]::double precision;
  end loop;
  if coalesce(array_length(v_run_lat, 1), 0) = 0 then
    return 0;
  end if;

  v_parts := string_to_array(coalesce(p_course, ''), ';');
  v_n := coalesce(array_length(v_parts, 1), 0);
  if v_n = 0 then
    return 0;
  end if;
  v_step := greatest(1, ceil(v_n / 80.0)::int);

  i := 1;
  while i <= v_n loop
    v_chunk := v_parts[i];
    i := i + v_step;
    continue when split_part(v_chunk, ',', 1) !~ v_num or split_part(v_chunk, ',', 2) !~ v_num;
    v_lat := split_part(v_chunk, ',', 1)::double precision;
    v_lng := split_part(v_chunk, ',', 2)::double precision;
    v_seen := v_seen + 1;
    for j in 1 .. array_length(v_run_lat, 1) loop
      -- 위도 차이로 먼저 거른다. 하버사인은 비싸다.
      continue when abs(v_run_lat[j] - v_lat) > v_deg;
      if economy.haversine_m(v_lat, v_lng, v_run_lat[j], v_run_lng[j]) <= p_meters then
        v_hit := v_hit + 1;
        exit;
      end if;
    end loop;
  end loop;

  if v_seen = 0 then
    return 0;
  end if;
  return v_hit::double precision / v_seen;
end;
$$;

-- 이 사용자가 볼 수 있는 코스를 길로 찾는다. 공유된 코스가 먼저, 오래된 것이 먼저.
create or replace function public.course_by_track(p_track text)
returns bigint
language sql
stable
security definer
set search_path = public
as $$
  select c.id
    from public.courses c
   where md5(c.track) = md5(coalesce(p_track, ''))
     and (c.shared or c.owner_id = auth.uid())
   order by c.shared desc, c.id
   limit 1
$$;

-- 방금 올린 러닝을 코스 기록으로 낸다.
-- 서버에 없는 코스(내 폰에만 있는 코스)면 아무 줄도 돌려주지 않는다.
create or replace function public.course_run_submit(p_course_track text, p_started_at timestamptz)
returns table (
  course_id bigint,
  duration_sec int,
  rank int,
  runners int
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_course bigint;
  v_course_track text;
  v_session public.walk_sessions%rowtype;
  v_inserted bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  v_course := public.course_by_track(p_course_track);
  if v_course is null then
    return;
  end if;
  select c.track into v_course_track from public.courses c where c.id = v_course;

  select * into v_session
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if not found then
    raise exception '러닝 기록이 서버에 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;
  if v_session.verdict = 'VOID' or v_session.duration_sec <= 0 then
    raise exception '인정되지 않은 러닝입니다' using errcode = '23514';
  end if;
  if economy.course_coverage(v_course_track, v_session.track) < 0.8 then
    raise exception '코스를 따라 달리지 않았습니다' using errcode = '23514';
  end if;

  insert into public.course_runs (course_id, user_id, session_id, duration_sec)
  values (v_course, v_user, v_session.id, v_session.duration_sec)
  on conflict do nothing
  returning id into v_inserted;

  if v_inserted is not null then
    update public.courses set run_count = run_count + 1 where id = v_course;
  end if;

  return query
    with best as (
      select r.user_id, min(r.duration_sec) as t
        from public.course_runs r
       where r.course_id = v_course
       group by r.user_id
    )
    select v_course,
           (select b.t from best b where b.user_id = v_user),
           (select count(*)::int + 1 from best b
             where b.t < (select b2.t from best b2 where b2.user_id = v_user)),
           (select count(*)::int from best);
end;
$$;

comment on function public.course_run_submit is
  '러닝이 코스를 따라갔는지 서버가 보고 코스 기록으로 남긴다. 코스의 달린 횟수도 올린다.';

-- 코스 순위 — 사람마다 가장 빠른 기록 하나. 내가 막은 사람은 빼고, 나는 늘 넣는다.
create or replace function public.course_leaderboard(p_course_track text, p_limit int default 20)
returns table (
  rank int,
  display_name text,
  duration_sec int,
  runs int,
  is_me boolean
)
language sql
stable
security definer
set search_path = public
as $$
  with c as (
    select public.course_by_track(p_course_track) as id
  ),
  best as (
    select r.user_id, min(r.duration_sec) as t, count(*)::int as n
      from public.course_runs r, c
     where r.course_id = c.id
       and not exists (
         select 1 from public.user_blocks b
          where b.blocker_id = auth.uid() and b.blocked_id = r.user_id)
     group by r.user_id
  ),
  ranked as (
    select (rank() over (order by b.t))::int as rk, b.*
      from best b
  )
  select k.rk, coalesce(p.display_name, '러너'), k.t, k.n, k.user_id = auth.uid()
    from ranked k
    left join public.profiles p on p.id = k.user_id
   where k.rk <= greatest(1, least(coalesce(p_limit, 20), 100)) or k.user_id = auth.uid()
   order by k.rk, k.user_id
$$;

comment on function public.course_leaderboard is
  '코스별 기록 순위. 사람마다 가장 빠른 기록 하나, 상위 p_limit 명과 나.';

revoke execute on function public.course_by_track(text) from public, anon;
revoke execute on function public.course_run_submit(text, timestamptz) from public, anon;
revoke execute on function public.course_leaderboard(text, int) from public, anon;
grant execute on function public.course_by_track(text) to authenticated;
grant execute on function public.course_run_submit(text, timestamptz) to authenticated;
grant execute on function public.course_leaderboard(text, int) to authenticated;
