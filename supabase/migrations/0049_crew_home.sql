-- 0049 크루 홈(확정 4번, 2026-09-29) — 가입한 크루의 홈에서 여는 상세들이 읽을 것
--
-- 새 정책을 만들지 않고 있는 데이터를 잇는다.
--   · 다음 러닝 = 이 크루의 번개러닝 글(크루 게시판, posts.category = 'FLASH') 중 아직 시작하지 않은 가장 이른 것.
--     참석은 기존 참가(flash_participants, 정원 · 지난 모임 규칙 그대로), 불참만 새로 남긴다(flash_declines).
--     사람마다 응답 하나 — 참석하면 불참이 지워지고, 불참하면 참가가 지워진다. 같은 응답을 다시 보내도 그대로다.
--   · 주간 기록 = 기존 크루 거리 규칙(0015 · 0047) 그대로 — 지금 멤버가 이 크루로 적은 러닝(walk_sessions.crew_id,
--     무효 제외), 한국 시간 월요일부터. 요일별 · 사람별로 나눠 보인다. 아직 오지 않은 요일은 null(0km 와 다르다).
--   · 크루 러닝 기록 = 같은 러닝 한 건(거리 · 시간 · 코스). 코스는 처음과 끝 300m 를 떼고 64점까지만 —
--     크루원에게 보이는 코스로 집 · 회사 같은 출발 · 도착 지점이 드러나지 않게.
--   · 공지에 모임 잇기 = 크루장이 공지를 쓸 때 이 크루의 번개 하나를 고를 수 있다(없으면 모임 버튼을 숨긴다).
-- 레벨 승급 기준은 정해지지 않아 여기서도 만들지 않는다.

-- ────────────────────────────────────────────────────────────────────
--  불참 응답
-- ────────────────────────────────────────────────────────────────────

create table if not exists public.flash_declines (
  post_id bigint not null references public.posts on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  declined_at timestamptz not null default now(),
  primary key (post_id, user_id)
);

comment on table public.flash_declines is
  '번개러닝 "이번엔 어려워요" — 참가(flash_participants)와 함께 사람마다 응답 하나. crew_meeting_respond 로만 쓴다.';

alter table public.flash_declines enable row level security;
revoke all on public.flash_declines from anon, authenticated;

-- 크루 홈의 문 — 지금 이 크루의 멤버가 아니면 crew_not_member(42501)
create or replace function public.crew_home_member(p_crew uuid)
returns text
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_role text;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select m.role into v_role from public.crew_members m where m.crew_id = p_crew and m.user_id = auth.uid();
  if v_role is null then
    raise exception 'crew_not_member' using errcode = '42501', detail = '이 크루의 멤버만 볼 수 있습니다';
  end if;
  return v_role;
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  다음 러닝(모임)
-- ────────────────────────────────────────────────────────────────────

-- 모임 한 건 — 제목 · 시각 · 장소(좌표가 있으면) · 거리 · 메모 · 진행자 · 참석 인원 · 앞의 얼굴 셋 · 내 응답
create or replace function public.crew_meeting_json(p_post public.posts, p_me uuid)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'id', p_post.id,
    'crew_id', p_post.crew_id,
    'title', p_post.title,
    'body', p_post.body,
    'place', p_post.place,
    'lat', p_post.lat,
    'lng', p_post.lng,
    'distance_km', p_post.distance_km,
    'meet_at', p_post.meet_at,
    'capacity', p_post.capacity,
    'host_id', p_post.author_id,
    'host_name', coalesce((select pr.display_name from public.profiles pr where pr.id = p_post.author_id), '러너'),
    'host_owner', exists (select 1 from public.crews c where c.id = p_post.crew_id and c.owner_id = p_post.author_id),
    'attendees', (select count(*) from public.flash_participants f where f.post_id = p_post.id),
    'faces', coalesce((
      select jsonb_agg(jsonb_build_object('user_id', x.user_id, 'name', x.name) order by x.host desc, x.joined_at)
        from (
          select f.user_id, coalesce(pr.display_name, '러너') as name, f.joined_at, f.user_id = p_post.author_id as host
            from public.flash_participants f
            left join public.profiles pr on pr.id = f.user_id
           where f.post_id = p_post.id
           order by (f.user_id = p_post.author_id) desc, f.joined_at
           limit 3
        ) x), '[]'::jsonb),
    'my_response', case
      when exists (select 1 from public.flash_participants f where f.post_id = p_post.id and f.user_id = p_me) then 'YES'
      when exists (select 1 from public.flash_declines d where d.post_id = p_post.id and d.user_id = p_me) then 'NO'
    end,
    'open', p_post.meet_at is null or p_post.meet_at >= now())
$$;

-- 홈의 다음 러닝 — 아직 시작하지 않은 가장 이른 크루 번개. 없으면 null(빈 상태)
create or replace function public.crew_meeting_next(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_post public.posts%rowtype;
begin
  perform public.crew_home_member(p_crew);
  select * into v_post
    from public.posts p
   where p.crew_id = p_crew and p.category = 'FLASH' and p.meet_at is not null and p.meet_at >= now()
   order by p.meet_at, p.id
   limit 1;
  if not found then
    return null;
  end if;
  return public.crew_meeting_json(v_post, auth.uid());
end;
$$;

-- 모임 상세 — 그 모임이 있는 크루의 멤버만. 지워진 모임은 meeting_missing
create or replace function public.crew_meeting(p_post bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_post public.posts%rowtype;
begin
  select * into v_post from public.posts p where p.id = p_post and p.category = 'FLASH' and p.crew_id is not null;
  if not found then
    raise exception 'meeting_missing' using errcode = '22023', detail = '모임을 찾을 수 없습니다';
  end if;
  perform public.crew_home_member(v_post.crew_id);
  return public.crew_meeting_json(v_post, auth.uid());
end;
$$;

-- 참석 · 불참 — 사람마다 응답 하나로 바꿔 적는다. 정원 · 지난 모임은 기존 참가 규칙(join_flash) 그대로.
-- 서버가 받은 뒤의 모임(인원 · 내 응답)을 돌려준다.
create or replace function public.crew_meeting_respond(p_post bigint, p_attend boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_post public.posts%rowtype;
begin
  if v_me is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_attend is null then
    raise exception 'invalid:response' using errcode = '22023';
  end if;
  -- 글을 잠근다 — 참가 정원 계산(join_flash)과 같은 순서로
  select * into v_post from public.posts p where p.id = p_post and p.category = 'FLASH' and p.crew_id is not null for update;
  if not found then
    raise exception 'meeting_missing' using errcode = '22023', detail = '모임을 찾을 수 없습니다';
  end if;
  perform public.crew_home_member(v_post.crew_id);
  if v_post.meet_at is not null and v_post.meet_at < now() then
    raise exception 'meeting_closed' using errcode = '23514', detail = '이미 시작한 모임입니다';
  end if;
  if p_attend then
    perform public.join_flash(p_post);
    delete from public.flash_declines where post_id = p_post and user_id = v_me;
  else
    delete from public.flash_participants where post_id = p_post and user_id = v_me;
    insert into public.flash_declines (post_id, user_id) values (p_post, v_me)
    on conflict (post_id, user_id) do nothing;
  end if;
  return public.crew_meeting_json(v_post, v_me);
end;
$$;

-- 13 참석자 — 진행자 먼저, 참석한 순서대로. 크루장인지도 함께
create or replace function public.crew_meeting_attendees(p_post bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_post public.posts%rowtype;
  v_owner uuid;
begin
  select * into v_post from public.posts p where p.id = p_post and p.category = 'FLASH' and p.crew_id is not null;
  if not found then
    raise exception 'meeting_missing' using errcode = '22023', detail = '모임을 찾을 수 없습니다';
  end if;
  perform public.crew_home_member(v_post.crew_id);
  select c.owner_id into v_owner from public.crews c where c.id = v_post.crew_id;
  return coalesce((
    select jsonb_agg(jsonb_build_object(
             'user_id', f.user_id,
             'name', coalesce(pr.display_name, '러너'),
             'host', f.user_id = v_post.author_id,
             'owner', f.user_id = v_owner)
           order by (f.user_id = v_post.author_id) desc, f.joined_at)
      from public.flash_participants f
      left join public.profiles pr on pr.id = f.user_id
     where f.post_id = p_post), '[]'::jsonb);
end;
$$;

-- 공지에 이을 수 있는 모임 — 이 크루의 아직 시작하지 않은 번개(가까운 순, 20개)
create or replace function public.crew_meetings_upcoming(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_home_member(p_crew);
  return coalesce((
    select jsonb_agg(public.crew_meeting_json(p, auth.uid()) order by p.meet_at, p.id)
      from public.posts p
     where p.id in (
       select x.id from public.posts x
        where x.crew_id = p_crew and x.category = 'FLASH' and x.meet_at is not null and x.meet_at >= now()
        order by x.meet_at, x.id
        limit 20)), '[]'::jsonb);
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  주간 기록 · 크루 러닝 기록
-- ────────────────────────────────────────────────────────────────────

-- 고를 수 있는 주 — 이번 주부터 거슬러 12주(한국 시간 월요일). 그 밖이면 invalid:week
create or replace function public.crew_week_bounds(p_week date)
returns table (week_start timestamptz, week_end timestamptz, first_day date)
language plpgsql
stable
set search_path = public
as $$
declare
  v_this date := (public.crew_week_start() at time zone 'Asia/Seoul')::date;
  v_day date := coalesce(p_week, v_this);
begin
  if extract(isodow from v_day) <> 1 or v_day > v_this or v_day < v_this - 7 * 11 then
    raise exception 'invalid:week' using errcode = '22023', detail = '고를 수 없는 주입니다';
  end if;
  week_start := v_day::timestamp at time zone 'Asia/Seoul';
  week_end := (v_day + 7)::timestamp at time zone 'Asia/Seoul';
  first_day := v_day;
  return next;
end;
$$;

-- 한 주의 크루 기록 — 총 거리 · 참여한 크루원 · 요일별 거리(오지 않은 요일은 null) · 사람별 거리(많은 순) · 목표(지금 값).
-- 목표가 없으면 goal_km 는 null(앱이 나누지 않는다). 고를 수 있는 주 목록도 함께
create or replace function public.crew_week(p_crew uuid, p_week date default null)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  b record;
  v_today date := (now() at time zone 'Asia/Seoul')::date;
  v_this date := (public.crew_week_start() at time zone 'Asia/Seoul')::date;
begin
  perform public.crew_home_member(p_crew);
  select * into b from public.crew_week_bounds(p_week);
  return (
    with runs as (
      select s.id, s.user_id, s.distance_meters, s.ended_at,
             ((s.started_at at time zone 'Asia/Seoul')::date) as day
        from public.walk_sessions s
        join public.crew_members m on m.crew_id = s.crew_id and m.user_id = s.user_id
       where s.crew_id = p_crew and s.verdict <> 'VOID'
         and s.started_at >= b.week_start and s.started_at < b.week_end
    ), people as (
      select r.user_id, sum(r.distance_meters) / 1000.0 as km, count(*)::int as runs,
             (array_agg(r.distance_meters order by r.ended_at desc))[1] / 1000.0 as last_km,
             max(r.ended_at) as last_at
        from runs r group by r.user_id
    )
    select jsonb_build_object(
      'week_start', b.first_day,
      'this_week', b.first_day = v_this,
      'goal_km', (select c.weekly_goal_km from public.crews c where c.id = p_crew),
      'km', coalesce((select sum(r.distance_meters) from runs r), 0) / 1000.0,
      'runners', (select count(distinct r.user_id) from runs r),
      'days', (
        select jsonb_agg(
                 case when b.first_day + k > v_today then null
                      else to_jsonb(coalesce((select sum(r.distance_meters) from runs r where r.day = b.first_day + k), 0) / 1000.0) end
                 order by k)
          from generate_series(0, 6) k),
      'members', coalesce((
        select jsonb_agg(jsonb_build_object(
                 'user_id', p.user_id, 'name', coalesce(pr.display_name, '러너'), 'km', p.km, 'runs', p.runs,
                 'last_km', p.last_km, 'last_at', p.last_at)
               order by p.km desc, p.last_at desc)
          from people p left join public.profiles pr on pr.id = p.user_id), '[]'::jsonb),
      'weeks', (
        select jsonb_agg(v_this - 7 * k order by k)
          from generate_series(0, 2) k))
  );
end;
$$;

-- 참여 기록 — 이번 주 전체 · 한 날 · 한 사람. 늦게 끝난 순, 한 번에 30개(마지막 줄의 끝난 시각 · id 로 이어 읽기)
create or replace function public.crew_runs(
  p_crew uuid,
  p_week date default null,
  p_day date default null,
  p_user uuid default null,
  p_before_at timestamptz default null,
  p_before_id bigint default null,
  p_limit int default 30
)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  b record;
begin
  perform public.crew_home_member(p_crew);
  select * into b from public.crew_week_bounds(p_week);
  if p_day is not null and (p_day < b.first_day or p_day > b.first_day + 6) then
    raise exception 'invalid:day' using errcode = '22023';
  end if;
  return coalesce((
    select jsonb_agg(x.j order by x.ended_at desc, x.id desc)
      from (
        select s.id, s.ended_at, jsonb_build_object(
                 'id', s.id, 'user_id', s.user_id, 'name', coalesce(pr.display_name, '러너'),
                 'distance_m', s.distance_meters, 'duration_s', s.duration_sec,
                 'started_at', s.started_at, 'ended_at', s.ended_at) as j
          from public.walk_sessions s
          join public.crew_members m on m.crew_id = s.crew_id and m.user_id = s.user_id
          left join public.profiles pr on pr.id = s.user_id
         where s.crew_id = p_crew and s.verdict <> 'VOID'
           and s.started_at >= b.week_start and s.started_at < b.week_end
           and (p_day is null or (s.started_at at time zone 'Asia/Seoul')::date = p_day)
           and (p_user is null or s.user_id = p_user)
           and (p_before_at is null or (s.ended_at, s.id) < (p_before_at, coalesce(p_before_id, 9223372036854775807)))
         order by s.ended_at desc, s.id desc
         limit least(greatest(coalesce(p_limit, 30), 1), 100)) x), '[]'::jsonb);
end;
$$;

-- 크루원이 가장 최근에 크루로 적은 러닝 하나(05 · 06 "공개한 러닝 기록") — 없으면 null
create or replace function public.crew_member_last_run(p_crew uuid, p_user uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_home_member(p_crew);
  return (
    select jsonb_build_object(
             'id', s.id, 'user_id', s.user_id, 'name', coalesce(pr.display_name, '러너'),
             'distance_m', s.distance_meters, 'duration_s', s.duration_sec,
             'started_at', s.started_at, 'ended_at', s.ended_at)
      from public.walk_sessions s
      join public.crew_members m on m.crew_id = s.crew_id and m.user_id = s.user_id
      left join public.profiles pr on pr.id = s.user_id
     where s.crew_id = p_crew and s.user_id = p_user and s.verdict <> 'VOID'
     order by s.ended_at desc, s.id desc
     limit 1);
end;
$$;

-- 크루원에게 보일 코스 — 처음과 끝 300m 를 떼고(출발 · 도착 지점을 숨긴다) 64점 이내로. 남는 점이 둘 미만이면 빈 문자열
create or replace function public.crew_run_route(p_track text)
returns text
language sql
immutable
set search_path = public
as $$
  with parsed as (
    select u.ord,
           case when s.p[1] ~ '^-?[0-9]+(\.[0-9]+)?$' then s.p[1]::double precision end as lat,
           case when s.p[2] ~ '^-?[0-9]+(\.[0-9]+)?$' then s.p[2]::double precision end as lng
      from unnest(string_to_array(coalesce(p_track, ''), ';')) with ordinality as u(chunk, ord)
      cross join lateral (select string_to_array(u.chunk, ',') as p) s
  ), pts as (
    select row_number() over (order by ord) as n, lat, lng
      from parsed
     where lat between -90 and 90 and lng between -180 and 180
  ), steps as (
    select n, lat, lng,
           coalesce(6371000 * 2 * asin(sqrt(
             power(sin(radians(lat - lag(lat) over w) / 2), 2) +
             cos(radians(lag(lat) over w)) * cos(radians(lat)) * power(sin(radians(lng - lag(lng) over w) / 2), 2))), 0) as step
      from pts
    window w as (order by n)
  ), walked as (
    select n, lat, lng, sum(step) over (order by n) as done from steps
  ), kept as (
    select row_number() over (order by n) as k, lat, lng
      from walked
     where done >= 300 and done <= (select max(done) from walked) - 300
  ), total as (
    select count(*)::int as c from kept
  ), picks as (
    select distinct 1 + round(i::numeric * (total.c - 1) / (least(total.c, 64) - 1))::int as k
      from total, generate_series(0, least(total.c, 64) - 1) i
     where total.c >= 2
  )
  select coalesce(
           string_agg(format('%s,%s', round(kept.lat::numeric, 5), round(kept.lng::numeric, 5)), ';' order by kept.k),
           '')
    from kept join picks using (k)
$$;

-- 19 크루 러닝 한 건 — 그 크루의 지금 멤버만, 달린 사람도 지금 멤버일 때만
create or replace function public.crew_run(p_run bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_run public.walk_sessions%rowtype;
begin
  select * into v_run from public.walk_sessions s where s.id = p_run and s.crew_id is not null and s.verdict <> 'VOID';
  if not found or not exists (select 1 from public.crew_members m where m.crew_id = v_run.crew_id and m.user_id = v_run.user_id) then
    raise exception 'run_missing' using errcode = '22023', detail = '볼 수 없는 기록입니다';
  end if;
  perform public.crew_home_member(v_run.crew_id);
  return jsonb_build_object(
    'id', v_run.id, 'crew_id', v_run.crew_id, 'user_id', v_run.user_id,
    'name', coalesce((select pr.display_name from public.profiles pr where pr.id = v_run.user_id), '러너'),
    'distance_m', v_run.distance_meters, 'duration_s', v_run.duration_sec,
    'started_at', v_run.started_at, 'ended_at', v_run.ended_at,
    'route', public.crew_run_route(v_run.track));
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  00 홈 — 크루 명함(crew_feed)과 함께 읽는 홈의 나머지
-- ────────────────────────────────────────────────────────────────────

-- 내 역할 · 채팅 미확인 수(대화 목록과 같은 셈) · 다음 러닝 · 이번 주 함께(목표는 지금 값) · 대표 공지(고정, 없으면 최근).
-- 모임이 없으면 meeting 은 null, 공지가 없으면 notice 는 null — 둘은 서로와 주간 기록에 영향을 주지 않는다
create or replace function public.crew_home(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_role text;
  v_joined timestamptz;
  v_week record;
begin
  v_role := public.crew_home_member(p_crew);
  select m.joined_at into v_joined from public.crew_members m where m.crew_id = p_crew and m.user_id = v_me;
  select * into v_week from public.crew_week_stats(p_crew);
  return jsonb_build_object(
    'crew_id', p_crew,
    'role', v_role,
    'owner_id', (select c.owner_id from public.crews c where c.id = p_crew),
    'member_count', (select count(*) from public.crew_members x where x.crew_id = p_crew),
    'unread', (
      select count(*) from public.crew_chat_messages u
       where u.crew_id = p_crew and u.kind <> 'SYSTEM' and u.author_id is distinct from v_me
         and u.seq > coalesce((select rd.last_read_seq from public.crew_chat_reads rd
                                where rd.crew_id = p_crew and rd.user_id = v_me), 0)
         and u.created_at > v_joined
         and u.deleted_at is null and u.hidden_at is null),
    'meeting', public.crew_meeting_next(p_crew),
    'week', jsonb_build_object(
      'week_start', (public.crew_week_start() at time zone 'Asia/Seoul')::date,
      'km', v_week.km,
      'runners', v_week.runners,
      'goal_km', (select c.weekly_goal_km from public.crews c where c.id = p_crew)),
    'notice', (
      select public.crew_chat_notice_json(n)
        from public.crew_chat_notices n
       where n.crew_id = p_crew
       order by n.pinned desc, n.created_at desc, n.id desc
       limit 1));
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  공지에 모임 잇기
-- ────────────────────────────────────────────────────────────────────

alter table public.crew_chat_notices add column if not exists meeting_post bigint;
alter table public.crew_chat_notices drop constraint if exists crew_chat_notices_meeting_fk;
alter table public.crew_chat_notices add constraint crew_chat_notices_meeting_fk
  foreign key (meeting_post) references public.posts (id) on delete set null;

comment on column public.crew_chat_notices.meeting_post is
  '공지에 이은 모임(이 크루의 번개러닝 글). 모임이 지워지면 비고, 비어 있으면 앱이 모임 버튼을 숨긴다';

create or replace function public.crew_chat_notice_json(p_notice public.crew_chat_notices)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'id', p_notice.id,
    'crew_id', p_notice.crew_id,
    'title', p_notice.title,
    'body', p_notice.body,
    'pinned', p_notice.pinned,
    'author_id', p_notice.author_id,
    'author_name', coalesce((select pr.display_name from public.profiles pr where pr.id = p_notice.author_id), '러너'),
    'created_at', p_notice.created_at,
    'updated_at', p_notice.updated_at,
    'meeting', (
      select jsonb_build_object('id', p.id, 'title', p.title, 'place', p.place, 'meet_at', p.meet_at)
        from public.posts p
       where p.id = p_notice.meeting_post and p.crew_id = p_notice.crew_id and p.category = 'FLASH'))
$$;

-- 공지 저장 — 0048 과 같고 이을 모임(p_meeting)만 더했다. 이 크루의 번개가 아니면 invalid:meeting.
-- 고칠 때는 p_meeting_change 가 참일 때만 이은 모임을 바꾼다(목표 거리 p_goal_change 와 같은 방식) —
-- 모임을 모르는 예전 앱이 공지를 고쳐도 이어 둔 모임이 풀리지 않는다
drop function if exists public.crew_chat_notice_save(uuid, bigint, text, text, boolean, uuid);
create or replace function public.crew_chat_notice_save(
  p_crew uuid,
  p_notice bigint,
  p_title text,
  p_body text,
  p_pinned boolean,
  p_client_key uuid default null,
  p_meeting bigint default null,
  p_meeting_change boolean default false
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_row public.crew_chat_notices%rowtype;
  v_title text := btrim(coalesce(p_title, ''), E' \t\r\n');
  v_body text := btrim(coalesce(p_body, ''), E' \t\r\n');
  v_rev bigint;
begin
  perform public.crew_chat_member(p_crew);
  perform public.crew_lock_owner(p_crew);
  if length(v_title) not between 1 and 100 then
    raise exception 'invalid:title' using errcode = '22023', detail = '공지 제목은 1~100자입니다';
  end if;
  if length(v_body) > 2000 then
    raise exception 'invalid:body' using errcode = '22023', detail = '공지 내용은 2000자까지입니다';
  end if;
  if p_meeting is not null and not exists (
    select 1 from public.posts p where p.id = p_meeting and p.crew_id = p_crew and p.category = 'FLASH'
  ) then
    raise exception 'invalid:meeting' using errcode = '22023', detail = '이 크루의 모임만 이을 수 있습니다';
  end if;

  if p_notice is null then
    if p_client_key is not null then
      select * into v_row from public.crew_chat_notices n where n.crew_id = p_crew and n.client_key = p_client_key;
      if found then
        return public.crew_chat_notice_json(v_row);
      end if;
    end if;
    if coalesce(p_pinned, false) then
      update public.crew_chat_notices set pinned = false where crew_id = p_crew and pinned;
    end if;
    insert into public.crew_chat_notices (crew_id, author_id, title, body, pinned, client_key, meeting_post)
    values (p_crew, v_me, v_title, v_body, coalesce(p_pinned, false), p_client_key, p_meeting)
    returning * into v_row;
    v_rev := public.crew_chat_next_rev(p_crew);
    insert into public.crew_chat_messages (crew_id, seq, rev, kind, event, event_name)
    values (p_crew, v_rev, v_rev, 'SYSTEM', 'NOTICE_CREATED', public.push_display_name(v_me));
  else
    select * into v_row from public.crew_chat_notices n where n.id = p_notice and n.crew_id = p_crew for update;
    if not found then
      raise exception 'notice_missing' using errcode = '22023', detail = '공지를 찾을 수 없습니다';
    end if;
    if coalesce(p_pinned, false) then
      update public.crew_chat_notices set pinned = false where crew_id = p_crew and pinned and id <> p_notice;
    end if;
    update public.crew_chat_notices
       set title = v_title, body = v_body, pinned = coalesce(p_pinned, false),
           meeting_post = case when coalesce(p_meeting_change, false) then p_meeting else meeting_post end,
           updated_at = now()
     where id = p_notice
    returning * into v_row;
  end if;
  return public.crew_chat_notice_json(v_row);
end;
$$;

-- 한 공지(20) — 크루 홈 · 공지 목록에서 id 로 연다
create or replace function public.crew_chat_notice(p_notice bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_row public.crew_chat_notices%rowtype;
begin
  select * into v_row from public.crew_chat_notices n where n.id = p_notice;
  if not found then
    raise exception 'notice_missing' using errcode = '22023', detail = '공지를 찾을 수 없습니다';
  end if;
  perform public.crew_chat_member(v_row.crew_id);
  return public.crew_chat_notice_json(v_row);
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  권한
-- ────────────────────────────────────────────────────────────────────

revoke all on function public.crew_home_member(uuid) from public, anon, authenticated;
revoke all on function public.crew_meeting_json(public.posts, uuid) from public, anon, authenticated;
revoke all on function public.crew_week_bounds(date) from public, anon, authenticated;
revoke all on function public.crew_run_route(text) from public, anon, authenticated;

revoke all on function public.crew_home(uuid) from public, anon;
revoke all on function public.crew_meeting_next(uuid) from public, anon;
revoke all on function public.crew_meeting(bigint) from public, anon;
revoke all on function public.crew_meeting_respond(bigint, boolean) from public, anon;
revoke all on function public.crew_meeting_attendees(bigint) from public, anon;
revoke all on function public.crew_meetings_upcoming(uuid) from public, anon;
revoke all on function public.crew_week(uuid, date) from public, anon;
revoke all on function public.crew_runs(uuid, date, date, uuid, timestamptz, bigint, int) from public, anon;
revoke all on function public.crew_member_last_run(uuid, uuid) from public, anon;
revoke all on function public.crew_run(bigint) from public, anon;
revoke all on function public.crew_chat_notice_save(uuid, bigint, text, text, boolean, uuid, bigint, boolean) from public, anon;
revoke all on function public.crew_chat_notice(bigint) from public, anon;

grant execute on function public.crew_home(uuid) to authenticated;
grant execute on function public.crew_meeting_next(uuid) to authenticated;
grant execute on function public.crew_meeting(bigint) to authenticated;
grant execute on function public.crew_meeting_respond(bigint, boolean) to authenticated;
grant execute on function public.crew_meeting_attendees(bigint) to authenticated;
grant execute on function public.crew_meetings_upcoming(uuid) to authenticated;
grant execute on function public.crew_week(uuid, date) to authenticated;
grant execute on function public.crew_runs(uuid, date, date, uuid, timestamptz, bigint, int) to authenticated;
grant execute on function public.crew_member_last_run(uuid, uuid) to authenticated;
grant execute on function public.crew_run(bigint) to authenticated;
grant execute on function public.crew_chat_notice_save(uuid, bigint, text, text, boolean, uuid, bigint, boolean) to authenticated;
grant execute on function public.crew_chat_notice(bigint) to authenticated;
