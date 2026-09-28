-- ════════════════════════════════════════════════════════════════════
--  0046 — 러닝 이야기: 동네 이야기에 최근 러닝을 붙인다 (2026-09-28 쉬운 글쓰기 상황별 시안)
--
--  글쓰기 위 첨부 칸에 내 최근 러닝(코스 그림 또는 거리 · 시간)을 붙여 올린다. 규칙은 시안의 지시서 그대로:
--
--    - 붙일 수 있는 기록은 **내 것 · 끝난 것 · 무효(VOID)가 아닌 것**이고, 끝난 날(한국 날짜, economy.game_day)이
--      오늘 · 어제 · 2일 전 · 3일 전인 것이다. 9월 28일에는 9월 25일 기록까지, 9월 29일에는 같은 기록이 4일 전이
--      되어 새로 붙일 수 없다. 끝난 시각으로 판단한다 — 올린 시각 · 동기화 시각으로 기간을 늘리지 않는다.
--    - 3일 제한은 **새로 붙일 때**의 규칙이다. 이미 올라간 글의 첨부는 기간이 지나도 숨기거나 지우지 않고,
--      고칠 때도 같은 기록이면 그대로 둔다.
--    - 거리 · 시간은 서버 기록의 값을 옮겨 적는다(앱이 보낸 숫자를 믿지 않는다). 경로가 없는 러닝은 거리 · 시간만
--      붙고 코스 그림은 비어 있다 — 가짜 코스를 만들지 않는다. 코스 그림은 경로를 64점으로 줄인 것이다.
--    - 응답을 못 받아 "다시 올리기"를 눌러도 글이 두 편 생기지 않게 앱이 글쓰기마다 요청 키(client_key)를 보낸다.
--      같은 키로 다시 오면 앞서 만든 글 번호를 돌려준다.
--
--  기록 조회(story_runs)는 전체 완료 수 · 마지막 완료 시각과 첨부할 수 있는 기록을 함께 준다 — 앱이 "처음 뛰는
--  사람"과 "오래 쉰 사람"을 조회 결과로 가른다(조회에 실패하면 그 어느 쪽으로도 단정하지 않는다).
--  예전 앱은 계속 여섯 칸으로 story_create · story_update 를 부른다(새 칸은 기본값).
-- ════════════════════════════════════════════════════════════════════

alter table public.posts add column if not exists run_session bigint;
alter table public.posts add column if not exists run_started_at timestamptz;
alter table public.posts add column if not exists run_ended_at timestamptz;
alter table public.posts add column if not exists run_distance_m int;
alter table public.posts add column if not exists run_duration_s int;
alter table public.posts add column if not exists run_route text not null default '';
alter table public.posts add column if not exists client_key uuid;

alter table public.posts drop constraint if exists posts_run_session_fk;
alter table public.posts add constraint posts_run_session_fk
  foreign key (run_session) references public.walk_sessions (id) on delete set null;
alter table public.posts drop constraint if exists posts_run_route_len;
alter table public.posts add constraint posts_run_route_len check (length(run_route) <= 4000);

create unique index if not exists posts_author_client_key
  on public.posts (author_id, client_key) where client_key is not null;

comment on column public.posts.run_session is '글에 붙인 러닝(walk_sessions.id). 기록이 지워지면 비지만 옮겨 적은 값은 남는다';
comment on column public.posts.run_distance_m is '붙인 러닝의 거리(m) — 서버 기록에서 옮겨 적는다. 첨부가 없으면 null';
comment on column public.posts.run_duration_s is '붙인 러닝의 시간(초) — 서버 기록에서 옮겨 적는다';
comment on column public.posts.run_route is '붙인 러닝의 코스 그림 "위도,경도;…"(64점 이내). 경로 없는 러닝이면 빈 문자열';
comment on column public.posts.client_key is '앱이 글쓰기마다 만든 요청 키 — 같은 키로 다시 오면 새 글을 만들지 않는다';

-- 경로("위도,경도,시각;…") → 코스 그림("위도,경도;…"). 처음과 끝을 넣고 사이를 고르게 골라 64점 이내로.
-- 읽을 수 없는 조각은 건너뛴다. 쓸 만한 점이 둘 미만이면 빈 문자열 — 코스가 없는 기록이다.
create or replace function public.story_route(p_track text)
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
  ), total as (
    select count(*)::int as c from pts
  ), picks as (
    select distinct 1 + round(k::numeric * (total.c - 1) / (least(total.c, 64) - 1))::int as n
      from total, generate_series(0, least(total.c, 64) - 1) k
     where total.c >= 2
  )
  select coalesce(
           string_agg(format('%s,%s', round(pts.lat::numeric, 5), round(pts.lng::numeric, 5)), ';' order by pts.n),
           '')
    from pts join picks using (n)
$$;

-- 새로 붙일 수 있는 러닝인가 — 내 것 · 무효 아님 · 거리와 시간이 있음 · 끝난 날이 오늘~3일 전(한국 날짜).
-- 이유를 앱이 가를 수 있게 메시지를 고정 낱말로 둔다(run_invalid · run_expired).
create or replace function public.story_attachable_run(p_run bigint, p_user uuid)
returns public.walk_sessions
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_run public.walk_sessions;
begin
  select * into v_run from public.walk_sessions s where s.id = p_run and s.user_id = p_user;
  if not found or v_run.verdict = 'VOID' or v_run.distance_meters <= 0 or v_run.duration_sec <= 0
     or v_run.ended_at > now() then
    raise exception 'run_invalid' using errcode = '22023', detail = '이 기록은 붙일 수 없습니다';
  end if;
  if economy.game_day(v_run.ended_at) < economy.game_day(now()) - 3 then
    raise exception 'run_expired' using errcode = '22023', detail = '3일이 지난 기록은 새로 붙일 수 없습니다';
  end if;
  return v_run;
end;
$$;

-- 글쓰기의 기록 칸 — 오늘(한국 날짜) · 전체 완료 수 · 마지막 완료 시각 · 첨부할 수 있는 기록(최근 것부터).
create or replace function public.story_runs(p_limit int default 20)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return jsonb_build_object(
    'today', v_today,
    'total', (select count(*) from public.walk_sessions s
               where s.user_id = v_user and s.verdict <> 'VOID' and s.ended_at <= now()),
    'voided', (select count(*) from public.walk_sessions s where s.user_id = v_user and s.verdict = 'VOID'),
    'last_ended_at', (select max(s.ended_at) from public.walk_sessions s
                       where s.user_id = v_user and s.verdict <> 'VOID' and s.ended_at <= now()),
    'runs', coalesce((
      select jsonb_agg(jsonb_build_object(
               'id', r.id,
               'started_at', r.started_at,
               'ended_at', r.ended_at,
               'distance_m', round(r.distance_meters)::int,
               'duration_s', r.duration_sec,
               'day', economy.game_day(r.ended_at),
               'route', public.story_route(r.track)
             ) order by r.ended_at desc, r.id desc)
        from (
          select s.* from public.walk_sessions s
           where s.user_id = v_user and s.verdict <> 'VOID'
             and s.ended_at <= now() and s.distance_meters > 0 and s.duration_sec > 0
             and economy.game_day(s.ended_at) between v_today - 3 and v_today
           order by s.ended_at desc, s.id desc
           limit greatest(least(coalesce(p_limit, 20), 50), 1)
        ) r
    ), '[]'::jsonb)
  );
end;
$$;

-- 목록 뷰에 붙인 러닝을 더한다. 열은 맨 뒤에 더한다(권한 · 순서 그대로). 러닝 번호는 내보내지 않는다.
create or replace view public.post_feed
with (security_invoker = true) as
  select
    p.id,
    p.category,
    p.crew_id,
    p.author_id,
    coalesce(pr.display_name, '러너') as author,
    p.title,
    p.body,
    p.place,
    p.lat,
    p.lng,
    p.distance_km,
    p.meet_at,
    p.capacity,
    p.created_at,
    (select count(*) from public.post_likes l where l.post_id = p.id) as likes,
    (select count(*) from public.comments c where c.post_id = p.id) as comment_count,
    (select count(*) from public.flash_participants f where f.post_id = p.id) as joined_count,
    exists (
      select 1 from public.post_likes l
       where l.post_id = p.id and l.user_id = auth.uid()
    ) as liked,
    exists (
      select 1 from public.flash_participants f
       where f.post_id = p.id and f.user_id = auth.uid()
    ) as joined,
    p.author_id = auth.uid() as mine,
    p.place_address,
    p.run_started_at,
    p.run_ended_at,
    p.run_distance_m,
    p.run_duration_s,
    p.run_route
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  where not public.is_blocked(p.author_id)
    and not public.is_hidden('POST', p.id::text);

grant select on public.post_feed to authenticated;

-- 쓰기 — 0041 에 러닝 첨부와 요청 키를 더했다. 새 칸이 비면 예전과 같다.
drop function if exists public.story_create(text, text, text, text, double precision, double precision);
create or replace function public.story_create(
  p_title text,
  p_body text,
  p_place text,
  p_place_address text,
  p_lat double precision,
  p_lng double precision,
  p_run bigint default null,
  p_client_key uuid default null
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
  v_run public.walk_sessions;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 같은 요청이 다시 왔다(응답을 받지 못하고 "다시 올리기") — 앞서 만든 글 번호를 돌려준다
  if p_client_key is not null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
    if v_id is not null then
      return v_id;
    end if;
  end if;
  perform public.story_check(p_title, p_body, p_place, p_place_address, p_lat, p_lng);
  if (select count(*) from public.posts p
       where p.author_id = v_user and p.created_at > now() - interval '1 hour') >= 20 then
    raise exception '글을 너무 자주 쓰고 있습니다. 잠시 뒤에 다시 써 주세요' using errcode = '23514';
  end if;
  if p_run is not null then
    v_run := public.story_attachable_run(p_run, v_user);
  end if;

  insert into public.posts (
    author_id, category, crew_id, title, body, place, place_address, lat, lng,
    run_session, run_started_at, run_ended_at, run_distance_m, run_duration_s, run_route, client_key
  )
  values (
    v_user, 'FREE', null, btrim(p_title), coalesce(p_body, ''),
    btrim(p_place), btrim(coalesce(p_place_address, '')), p_lat, p_lng,
    v_run.id, v_run.started_at, v_run.ended_at, round(v_run.distance_meters)::int, v_run.duration_sec,
    coalesce(public.story_route(v_run.track), ''), p_client_key
  )
  on conflict (author_id, client_key) where client_key is not null do nothing
  returning id into v_id;
  -- 같은 키의 요청 둘이 동시에 왔다 — 먼저 들어간 글 번호
  if v_id is null then
    select p.id into v_id from public.posts p where p.author_id = v_user and p.client_key = p_client_key;
  end if;
  return v_id;
end;
$$;

-- 고치기 — p_run_change 가 false 면 붙어 있던 러닝을 그대로 둔다(기간이 지났어도). true 면 p_run 으로 바꾸거나
-- (새 기록은 3일 규칙을 따른다, 같은 기록이면 그대로) null 이면 뺀다.
drop function if exists public.story_update(bigint, text, text, text, text, double precision, double precision);
create or replace function public.story_update(
  p_post bigint,
  p_title text,
  p_body text,
  p_place text,
  p_place_address text,
  p_lat double precision,
  p_lng double precision,
  p_run bigint default null,
  p_run_change boolean default false
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_current bigint;
  v_change boolean := coalesce(p_run_change, false);
  v_run public.walk_sessions;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform public.story_check(p_title, p_body, p_place, p_place_address, p_lat, p_lng);
  select p.run_session into v_current
    from public.posts p
   where p.id = p_post
     and p.author_id = v_user
     and p.category <> 'FLASH'
     and p.crew_id is null
   for update;
  if not found then
    raise exception '내가 쓴 글만 고칠 수 있습니다' using errcode = '42501';
  end if;
  if v_change and p_run is not null and p_run = v_current then
    v_change := false;
  end if;
  if v_change and p_run is not null then
    v_run := public.story_attachable_run(p_run, v_user);
  end if;
  update public.posts
     set title = btrim(p_title),
         body = coalesce(p_body, ''),
         place = btrim(p_place),
         place_address = btrim(coalesce(p_place_address, '')),
         lat = p_lat,
         lng = p_lng,
         run_session = case when v_change then v_run.id else run_session end,
         run_started_at = case when v_change then v_run.started_at else run_started_at end,
         run_ended_at = case when v_change then v_run.ended_at else run_ended_at end,
         run_distance_m = case when v_change then round(v_run.distance_meters)::int else run_distance_m end,
         run_duration_s = case when v_change then v_run.duration_sec else run_duration_s end,
         run_route = case when v_change then coalesce(public.story_route(v_run.track), '') else run_route end
   where id = p_post;
end;
$$;

revoke all on function public.story_route(text) from public, anon, authenticated;
revoke all on function public.story_attachable_run(bigint, uuid) from public, anon, authenticated;
revoke all on function public.story_runs(int) from public, anon;
grant execute on function public.story_runs(int) to authenticated;
revoke all on function public.story_create(text, text, text, text, double precision, double precision, bigint, uuid) from public, anon;
grant execute on function public.story_create(text, text, text, text, double precision, double precision, bigint, uuid) to authenticated;
revoke all on function public.story_update(bigint, text, text, text, text, double precision, double precision, bigint, boolean) from public, anon;
grant execute on function public.story_update(bigint, text, text, text, text, double precision, double precision, bigint, boolean) to authenticated;
