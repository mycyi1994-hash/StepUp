-- 코스 공유와 파티런 로비.
--
-- 코스 게시판은 폰 안의 코스 중 "공유" 표시가 켜진 것이었다. 남이 올린 코스는
-- 볼 수 없었다. 이제 공유한 코스는 서버에 올라가고, 게시판은 서버에서 읽는다.
--
-- 파티런은 로비에 들어온 "크루원"이 폰이 지어낸 사람이었다. 이제 로비는
-- 서버에 있고, 같은 크루의 로비를 연 사람들이 같은 방에 모인다. 앱은 몇 초마다
-- 방의 상태를 물어 준비·출발·거리를 맞춘다.

-- ════════════════════════════════════════════════════════════════════
--  코스 공유
-- ════════════════════════════════════════════════════════════════════

-- 같은 사람이 같은 길을 두 번 올리면 게시판에 같은 코스가 둘 뜬다.
-- 길은 길어서 그대로 색인하지 않고 지문(md5)으로 잡는다.
create unique index if not exists courses_owner_track
  on public.courses (owner_id, md5(track));

-- 코스 올리기. 이미 올린 길이면 이름만 고쳐 다시 공개한다. 서버 번호를 돌려준다.
create or replace function public.course_share(
  p_name text,
  p_area text,
  p_distance_km double precision,
  p_elevation_m int,
  p_track text
)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(length(p_track), 0) < 7 or length(p_track) > 200000 then
    raise exception '코스 경로가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_distance_km is null or p_distance_km < 0.2 or p_distance_km > 200 then
    raise exception '코스 거리가 올바르지 않습니다' using errcode = '22023';
  end if;
  if (select count(*) from public.courses c
       where c.owner_id = v_user and c.created_at > now() - interval '1 day') >= 30 then
    raise exception '코스를 너무 많이 올렸습니다. 내일 다시 올려 주세요' using errcode = '23514';
  end if;

  insert into public.courses (owner_id, name, area, distance_km, elevation_m, track, shared)
  values (
    v_user,
    left(coalesce(nullif(btrim(p_name), ''), 'Course'), 60),
    left(btrim(coalesce(p_area, '')), 60),
    p_distance_km,
    coalesce(p_elevation_m, 0),
    p_track,
    true
  )
  on conflict (owner_id, md5(track)) do update
    set name = excluded.name,
        area = excluded.area,
        shared = true
  returning id into v_id;

  return v_id;
end;
$$;

-- 올린 코스 내리기. 폰에는 내 코스로 그대로 남는다.
create or replace function public.course_unshare(p_track text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 지우지 않고 내리기만 한다. 지우면 다른 러너의 코스 기록·좋아요가 함께 사라지고,
  -- 다시 올리면 새 코스가 되어 이미 받아 간 사람들의 기록이 이어지지 않는다.
  -- (다시 올리면 course_share 의 on conflict 가 shared 를 되돌린다.)
  update public.courses set shared = false
   where owner_id = auth.uid() and md5(track) = md5(coalesce(p_track, ''));
end;
$$;

-- 코스 좋아요를 누르거나 거둔다. 누른 뒤의 상태를 돌려준다.
create or replace function public.course_toggle_like(p_course bigint)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (
    select 1 from public.courses c
     where c.id = p_course and (c.shared or c.owner_id = v_user)
  ) then
    raise exception '코스를 찾을 수 없습니다' using errcode = '22023';  -- 4xx 로 가야 앱이 이유를 보여 준다(P0002 는 500)
  end if;

  delete from public.course_likes where course_id = p_course and user_id = v_user;
  if found then
    return false;
  end if;
  insert into public.course_likes (course_id, user_id) values (p_course, v_user);
  return true;
end;
$$;

grant execute on function public.course_share(text, text, double precision, int, text) to authenticated;
grant execute on function public.course_unshare(text) to authenticated;
grant execute on function public.course_toggle_like(bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  파티런 로비
--
--  방 하나는 크루 하나 또는 번개 글 하나에 붙는다. 같은 크루의 로비를 연
--  사람들은 열려 있는 같은 방에 들어간다. 방장은 처음 연 사람이고, 방장이
--  나가면 그다음 먼저 들어온 사람이 이어받는다.
--
--  표에는 앱이 직접 닿지 못한다. 전부 아래 함수로만 한다 — 준비·출발·강퇴는
--  누가 할 수 있는지가 정해져 있고, 표를 열어 두면 그 규칙이 없어진다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.parties (
  id bigint generated always as identity primary key,
  crew_id uuid references public.crews on delete cascade,
  flash_post_id bigint references public.posts on delete cascade,
  host_id uuid not null references auth.users on delete cascade,
  status text not null default 'LOBBY'
    check (status in ('LOBBY', 'COUNTDOWN', 'RUNNING', 'FINISHED')),
  -- 카운트다운이 끝나는 시각. 모두가 이 시각에 같이 출발한다.
  starts_at timestamptz,
  created_at timestamptz not null default now(),
  constraint parties_one_target check ((crew_id is null) <> (flash_post_id is null))
);

create index if not exists parties_open_crew on public.parties (crew_id)
  where status <> 'FINISHED';
create index if not exists parties_open_flash on public.parties (flash_post_id)
  where status <> 'FINISHED';

create table if not exists public.party_members (
  party_id bigint not null references public.parties on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  ready boolean not null default false,
  joined_at timestamptz not null default now(),
  -- 마지막으로 방 상태를 물은 시각. 앱을 닫고 사라진 사람을 가려낸다.
  last_seen timestamptz not null default now(),
  -- 달리는 동안의 위치. 방장에게서 너무 멀어진 사람을 가려낸다.
  lat double precision,
  lng double precision,
  primary key (party_id, user_id)
);

alter table public.parties enable row level security;
alter table public.party_members enable row level security;
revoke all on public.parties, public.party_members from anon, authenticated;

-- 이 크루(또는 번개)의 방에 들어갈 수 있는지
create or replace function public.party_can_enter(p_crew uuid, p_post bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select case
    when p_crew is not null then exists (
      select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = auth.uid()
    )
    when p_post is not null then exists (
      select 1 from public.flash_participants f where f.post_id = p_post and f.user_id = auth.uid()
    ) or exists (
      select 1 from public.posts p where p.id = p_post and p.author_id = auth.uid()
    )
    else false
  end
$$;

-- 방의 멤버인지
create or replace function public.party_assert_member(p_party bigint)
returns void
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (
    select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()
  ) then
    raise exception '이 파티에 들어와 있지 않습니다' using errcode = '42501';
  end if;
end;
$$;

-- 로비 열기. 열려 있는 방이 있으면 거기에 들어가고, 없으면 새로 연다.
-- @return 방 번호
create or replace function public.party_open(p_crew uuid, p_post bigint)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_party bigint;
  v_status text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if (p_crew is null) = (p_post is null) then
    raise exception '크루 또는 번개 하나를 골라야 합니다' using errcode = '22023';
  end if;
  if not public.party_can_enter(p_crew, p_post) then
    raise exception '크루원이나 번개 참가자만 들어갈 수 있습니다' using errcode = '42501';
  end if;

  -- 아무도 안 보는 방과 오래된 방은 닫는다. 안 닫으면 어제 열고 버린 방에
  -- 오늘 들어가게 된다.
  update public.parties p set status = 'FINISHED'
   where p.status <> 'FINISHED'
     and (p.created_at < now() - interval '6 hours'
          or not exists (
            select 1 from public.party_members m
             where m.party_id = p.id and m.last_seen > now() - interval '10 minutes'
          ));

  -- 이 크루(번개)의 열린 방. 두 사람이 동시에 열어도 방이 둘 생기지 않게 잠근다.
  perform pg_advisory_xact_lock(hashtext(coalesce(p_crew::text, 'post:' || p_post)));

  select p.id, p.status into v_party, v_status
    from public.parties p
   where p.status <> 'FINISHED'
     and ((p_crew is not null and p.crew_id = p_crew)
          or (p_post is not null and p.flash_post_id = p_post))
   order by p.created_at desc
   limit 1;

  -- 이미 출발한 방에 늦게 온 사람은 끼워 넣지 않는다. 같이 출발하지 않았으면
  -- 같이 뛴 것이 아니다. 대신 새 방을 연다.
  if v_party is not null and v_status <> 'LOBBY' and not exists (
    select 1 from public.party_members m where m.party_id = v_party and m.user_id = v_user
  ) then
    v_party := null;
  end if;

  if v_party is null then
    insert into public.parties (crew_id, flash_post_id, host_id)
    values (p_crew, p_post, v_user)
    returning id into v_party;
  end if;

  insert into public.party_members (party_id, user_id)
  values (v_party, v_user)
  on conflict (party_id, user_id) do update set last_seen = now();

  return v_party;
end;
$$;

-- 카운트다운이 끝났으면 달리는 중으로 넘긴다. 따로 시계를 돌리지 않고, 누군가
-- 방 상태를 물을 때 넘긴다.
create or replace function public.party_tick(p_party bigint)
returns void
language sql
security definer
set search_path = public
as $$
  update public.parties set status = 'RUNNING'
   where id = p_party and status = 'COUNTDOWN' and starts_at <= now()
$$;

-- 방 상태. 앱이 몇 초마다 부른다. 부를 때마다 "나 아직 여기 있다"가 기록된다.
create or replace function public.party_state(p_party bigint)
returns json
language plpgsql
security definer
set search_path = public
as $$
declare
  v_state json;
begin
  perform public.party_assert_member(p_party);
  perform public.party_tick(p_party);

  update public.party_members set last_seen = now()
   where party_id = p_party and user_id = auth.uid();

  select json_build_object(
    'id', p.id,
    'status', p.status,
    'host_id', p.host_id,
    'crew_id', p.crew_id,
    'flash_post_id', p.flash_post_id,
    'starts_at', p.starts_at,
    'server_now', now(),
    'members', coalesce((
      select json_agg(json_build_object(
        'user_id', m.user_id,
        'name', coalesce(pr.display_name, '러너'),
        'ready', m.ready,
        'is_me', m.user_id = auth.uid(),
        'is_host', m.user_id = p.host_id,
        'lat', m.lat,
        'lng', m.lng,
        'seen_sec', extract(epoch from (now() - m.last_seen))::int
      ) order by (m.user_id = p.host_id) desc, m.joined_at)
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
       where m.party_id = p.id
    ), '[]'::json)
  ) into v_state
  from public.parties p
  where p.id = p_party;

  return v_state;
end;
$$;

create or replace function public.party_ready(p_party bigint, p_ready boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  if not exists (select 1 from public.parties where id = p_party and status = 'LOBBY') then
    raise exception '이미 출발한 파티입니다' using errcode = '23514';
  end if;
  update public.party_members set ready = p_ready, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 나가기. 방장이 나가면 다음으로 먼저 들어온 사람이 방장이 된다. 아무도 안
-- 남으면 방을 닫는다.
create or replace function public.party_leave(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_next uuid;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.party_members where party_id = p_party and user_id = auth.uid();

  select m.user_id into v_next
    from public.party_members m
   where m.party_id = p_party
   order by m.joined_at
   limit 1;

  if v_next is null then
    update public.parties set status = 'FINISHED' where id = p_party;
  else
    update public.parties set host_id = v_next
     where id = p_party and host_id = auth.uid();
  end if;
end;
$$;

-- 방장 — 로비에서 한 사람을 내보낸다.
create or replace function public.party_kick(p_party bigint, p_user uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 할 수 있습니다' using errcode = '42501';
  end if;
  if p_user = auth.uid() then
    raise exception '방장은 자신을 내보낼 수 없습니다' using errcode = '22023';
  end if;
  delete from public.party_members where party_id = p_party and user_id = p_user;
end;
$$;

-- 방장 — 출발. 준비한 사람들끼리 뛴다. 준비 안 한 사람은 방에서 빠진다 —
-- 데리고 가면 적립 보너스(인원수)에는 들어가면서 실제로는 안 뛰는 사람이 생긴다.
-- 4초 뒤에 모두 같이 출발한다.
create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if not exists (
    select 1 from public.parties
     where id = p_party and host_id = auth.uid() and status = 'LOBBY'
  ) then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;
  update public.parties
     set status = 'COUNTDOWN', starts_at = now() + interval '4 seconds'
   where id = p_party;
end;
$$;

-- 달리는 동안의 위치 보고. 멀어진 사람을 가려내는 데만 쓴다.
create or replace function public.party_ping(p_party bigint, p_lat double precision, p_lng double precision)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  if p_lat is not null and (p_lat not between -90 and 90 or p_lng not between -180 and 180) then
    raise exception '위치가 올바르지 않습니다' using errcode = '22023';
  end if;
  update public.party_members
     set lat = p_lat, lng = p_lng, last_seen = now()
   where party_id = p_party and user_id = auth.uid();
end;
$$;

-- 방장이 러닝을 마치면 방을 닫는다. 다음 파티런은 새 방에서 한다.
create or replace function public.party_finish(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.party_assert_member(p_party);
  update public.parties set status = 'FINISHED'
   where id = p_party and host_id = auth.uid();
end;
$$;

revoke execute on function public.party_assert_member(bigint) from public, anon;
revoke execute on function public.party_tick(bigint) from public, anon;
grant execute on function public.party_open(uuid, bigint) to authenticated;
grant execute on function public.party_state(bigint) to authenticated;
grant execute on function public.party_ready(bigint, boolean) to authenticated;
grant execute on function public.party_leave(bigint) to authenticated;
grant execute on function public.party_kick(bigint, uuid) to authenticated;
grant execute on function public.party_start(bigint) to authenticated;
grant execute on function public.party_ping(bigint, double precision, double precision) to authenticated;
grant execute on function public.party_finish(bigint) to authenticated;
