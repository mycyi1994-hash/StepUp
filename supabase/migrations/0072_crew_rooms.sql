-- ════════════════════════════════════════════════════════════════════
--  0072 — 크루 러닝 대기실: 이름 · 함께 달릴 코스 · 열린 대기실 목록 · 골라 들어가기 · 코스 바꾸기
--         (2026-10-24 크루 러닝 핸드오프 — 3번 듀얼 레일, 장면 01 ~ 09 · 22 · 25 · 59)
--
--  0071 의 경제 규칙(공용 체력 · 하루 한 번 · 2분 인원 · 레벨)은 그대로다. 이 판은 "방"만 넓힌다.
--
--  - 크루원 누구나 이름과 코스를 붙여 대기실을 연다(party_create). 크루장만이 아니다(R06). 방을 열거나 들어가는 것만으로
--    하루 한 번 참여는 쓰이지 않는다 — 참여는 출발이 서버에서 확정될 때(party_start) 그대로다.
--    단, 오늘 이미 크루 러닝을 한 사람은 새 방을 열지 않는다(준비할 수 없어 진행자가 출발시킬 수 없는 빈 방이 된다).
--  - 같은 크루의 열린 대기실을 고른다(party_rooms → party_join). 예전 party_open(크루의 열린 방 하나에 저절로)은
--    옛 앱 · 번개러닝을 위해 그대로 둔다.
--  - 방의 코스는 방에 사본으로 둔다(이름 · 거리 · 경로). 폰에만 있는 내 코스도 고를 수 있고, 원래 코스를 지우거나 바꿔도
--    방 · 기록의 코스는 그대로다. 진행자만 바꾼다(party_set_course). 바꿀 때마다 코스 판(course_rev)이 오른다.
--    D14(코스를 바꾸면 준비를 어떻게) — 진행자를 뺀 모두의 준비를 푼다: 새 코스를 보고 다시 준비하게(바뀐 코스로 모르고 출발하지 않게).
--  - 출발은 방의 코스 판을 맞대어 본다(party_start_rev) — 진행자가 본 코스와 방의 코스가 다르면 출발하지 않는다.
--  - 크루 방의 출발 시각은 6초 뒤(0071 은 4초) — 대기실은 2초마다 묻는다. 늦게 안 사람도 3 · 2 · 1 · 0 · StepUP! 을 처음부터 본다.
--    번개러닝은 예전 그대로 4초.
--  - 진행자를 넘길 때는 오늘 크루 러닝을 아직 하지 않은(준비할 수 있는) 사람이 먼저다 — 오늘 이미 한 사람은 방을 볼 수만 있어
--    그 사람이 진행자가 되면 방이 출발하지 못한다(party_leave · 다른 방으로 옮김 · 30초 넘게 안 보이는 진행자).
--  - 한 사람의 방 열기 · 들어가기는 하나씩(사람 자물쇠), 들어가기는 크루 자물쇠(party_open 과 같은 것)를 먼저 — 두 방에 남거나 20명을 넘지 않게.
--  - 출발 때 방 이름 · 코스를 참여 기록(crew_runs)에 함께 적는다 — 지난 크루 러닝 기록은 그때의 이름 · 코스로 보인다(장면 59).
-- ════════════════════════════════════════════════════════════════════

-- ── 방 · 참여 기록에 이름 · 코스 ─────────────────────────────────────

alter table public.parties add column if not exists name text;
alter table public.parties add column if not exists course_name text;
alter table public.parties add column if not exists course_km numeric(8, 3);
-- 앱의 RunCourse.encode 형식("위도,경도;위도,경도") — 시각이 없는 경로
alter table public.parties add column if not exists course_track text;
alter table public.parties add column if not exists course_rev int not null default 0;
-- 방 열기 요청 번호(앱이 만든 uuid) — 답을 잃고 같은 요청을 다시 보내도 방은 하나(같은 사람 · 같은 번호면 그 방을 돌려준다).
-- 연 사람(creator_id)으로 찾는다 — 진행자(host_id)는 넘겨받으면 바뀐다. 닫힌 방은 빼고 찾는다(닫힌 뒤 다시 보내면 새 방)
alter table public.parties add column if not exists create_key uuid;
alter table public.parties add column if not exists creator_id uuid references auth.users on delete set null;
create unique index if not exists parties_create_key on public.parties (creator_id, create_key)
  where create_key is not null and status <> 'FINISHED';

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'parties_name_len') then
    alter table public.parties add constraint parties_name_len
      check (name is null or char_length(name) between 1 and 30);
  end if;
  if not exists (select 1 from pg_constraint where conname = 'parties_course_sane') then
    alter table public.parties add constraint parties_course_sane check (
      (course_name is null or char_length(course_name) between 1 and 60)
      and (course_km is null or course_km between 0 and 100)
      and (course_track is null or char_length(course_track) <= 60000));
  end if;
end $$;

alter table economy.crew_runs add column if not exists party_name text;
alter table economy.crew_runs add column if not exists course_name text;
alter table economy.crew_runs add column if not exists course_km numeric(8, 3);
alter table economy.crew_runs add column if not exists course_rev int;

-- ── 도움 함수 ────────────────────────────────────────────────────────

-- 방 이름 — 앞뒤 공백을 빼고 1 ~ 30자. 아니면 오류
create or replace function economy.party_room_name(p_name text) returns text
language plpgsql immutable set search_path = '' as $$
declare
  v text := btrim(regexp_replace(coalesce(p_name, ''), '\s+', ' ', 'g'));
begin
  if char_length(v) not between 1 and 30 then
    raise exception 'room_name_invalid' using errcode = '22023', detail = '러닝 이름은 1~30자입니다';
  end if;
  return v;
end;
$$;
revoke all on function economy.party_room_name(text) from public;

-- 코스 — 이름 1 ~ 60자 · 거리 0 ~ 100km · 경로 "위도,경도;…" 점 2개 이상(6만 자까지) · 위도 ±90 · 경도 ±180 안. 아니면 오류
-- (방 사람 모두의 지도에 그려진다 — 지도에 없는 좌표는 받지 않는다)
create or replace function economy.party_course_check(p_name text, p_km numeric, p_track text) returns void
language plpgsql immutable set search_path = '' as $$
begin
  if char_length(btrim(coalesce(p_name, ''))) not between 1 and 60
     or p_km is null or p_km < 0 or p_km > 100
     or p_track is null or char_length(p_track) > 60000
     or p_track !~ '^-?[0-9]+(\.[0-9]+)?,-?[0-9]+(\.[0-9]+)?(;-?[0-9]+(\.[0-9]+)?,-?[0-9]+(\.[0-9]+)?)+$' then
    raise exception 'course_invalid' using errcode = '22023', detail = '코스 이름 · 거리 · 경로를 확인하세요';
  end if;
  -- 형식이 맞은 뒤에만 숫자로 읽는다(한 조건에 묶으면 읽는 차례가 정해져 있지 않다)
  if exists (
    select 1 from pg_catalog.regexp_split_to_table(p_track, ';') as pt(v)
     where pg_catalog.split_part(pt.v, ',', 1)::numeric not between -90 and 90
        or pg_catalog.split_part(pt.v, ',', 2)::numeric not between -180 and 180) then
    raise exception 'course_invalid' using errcode = '22023', detail = '코스 경로의 좌표가 지도 밖입니다';
  end if;
end;
$$;
revoke all on function economy.party_course_check(text, numeric, text) from public;

-- 아무도 안 보는 방과 오래된 방을 닫는다(party_open 과 같은 기준). 닫는 방에 남은 위치 · 거리는 지운다
create or replace function economy.party_close_stale(p_crew uuid) returns void
language sql security definer set search_path = public, economy as $$
  with closed as (
    update public.parties p set status = 'FINISHED'
     where p.crew_id = p_crew
       and p.status <> 'FINISHED'
       and (p.created_at < now() - interval '6 hours'
            or not exists (
              select 1 from public.party_members m
               where m.party_id = p.id and m.last_seen > now() - interval '10 minutes'
            ))
    returning p.id)
  update public.party_members m set lat = null, lng = null, km = null
    from closed
   where m.party_id = closed.id
$$;
revoke all on function economy.party_close_stale(uuid) from public;

-- 다음 진행자 — 남은 사람 중 오늘 크루 러닝을 아직 하지 않은(준비할 수 있는) 사람이 먼저, 그다음 먼저 들어온 차례.
-- 오늘 이미 한 사람은 크루 방에서 준비할 수 없어(party_ready) 그 사람이 진행자가 되면 방이 출발하지 못한다.
-- p_seen 을 주면 그 안에 보인 사람만(30초 넘게 안 보이는 진행자 넘기기). 남은 사람이 없으면 null
create or replace function economy.party_next_host(p_party bigint, p_seen interval default null) returns uuid
language sql stable security definer set search_path = public, economy as $$
  select m.user_id
    from public.party_members m
    join public.parties p on p.id = m.party_id
   where m.party_id = p_party
     and (p_seen is null or m.last_seen > now() - p_seen)
   order by (p.crew_id is not null
             and exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = economy.game_day(now()))),
            m.joined_at, m.user_id
   limit 1
$$;
revoke all on function economy.party_next_host(bigint, interval) from public;

-- 출발 전(LOBBY) 다른 대기실에서 나온다 — 한 사람이 두 대기실에 남아 진행자로 굳지 않게. 나가는 규칙은 party_leave 와 같다
-- (진행자면 다음 사람에게 넘기고, 아무도 없으면 방을 닫는다). 그 방을 잠그고 다시 본다 — 그사이 출발했으면 그대로 두고
-- (출발 명단 · 참여 기록에 이미 들었다), 누가 막 들어왔으면 닫지 않고 넘긴다
create or replace function economy.party_leave_other_lobbies(p_user uuid, p_keep bigint) returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_id bigint;
  v_next uuid;
begin
  for v_id in
    select m.party_id from public.party_members m
     where m.user_id = p_user and m.party_id is distinct from p_keep
     order by m.party_id
  loop
    perform 1 from public.parties p where p.id = v_id and p.status = 'LOBBY' for update;
    if not found then
      continue;
    end if;
    delete from public.party_members where party_id = v_id and user_id = p_user;
    v_next := economy.party_next_host(v_id);
    if v_next is null then
      update public.parties set status = 'FINISHED' where id = v_id;
    else
      update public.parties set host_id = v_next where id = v_id and host_id = p_user;
    end if;
  end loop;
end;
$$;
revoke all on function economy.party_leave_other_lobbies(uuid, bigint) from public;

-- 방 한 줄(목록 · 상태 공통) — 이름 · 코스(경로 빼고) · 인원 · 준비 · 진행자. 경로는 party_course 로 따로 받는다(2초마다 묻는 상태가 무겁지 않게)
create or replace function economy.party_room_json(p_party bigint, p_user uuid) returns jsonb
language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object(
    'id', p.id,
    'name', p.name,
    'status', p.status,
    'host_id', p.host_id,
    'host_name', (select coalesce(pr.display_name, '러너') from public.profiles pr where pr.id = p.host_id),
    'created_at', p.created_at,
    'starts_at', p.starts_at,
    'members_count', (select count(*) from public.party_members m where m.party_id = p.id),
    'ready_count', (select count(*) from public.party_members m where m.party_id = p.id and m.ready),
    'capacity', economy.crew_room_max(),
    'course_name', p.course_name,
    'course_km', p.course_km,
    'course_rev', p.course_rev,
    'mine', exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = p_user),
    'kicked', exists (select 1 from public.party_kicks k where k.party_id = p.id and k.user_id = p_user))
    from public.parties p
   where p.id = p_party
$$;
revoke all on function economy.party_room_json(bigint, uuid) from public;

-- ── 목록 · 열기 · 들어가기 ───────────────────────────────────────────

-- 같은 크루의 열린 대기실(출발 전) — 내가 들어가 있는 방은 출발했어도 함께(먼저). 크루원만.
-- 각 방의 코스 경로(미리보기 지도)도 함께 준다 — 목록은 열 때 한 번 읽는다
create or replace function public.party_rooms(p_crew uuid)
returns jsonb
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
begin
  perform public.crew_home_member(p_crew);
  perform economy.party_close_stale(p_crew);
  return jsonb_build_object(
    'server_now', now(),
    'capacity', economy.crew_room_max(),
    'done_today', exists (select 1 from economy.crew_runs c where c.user_id = v_user and c.day = economy.game_day(now())),
    'rooms', coalesce((
      select jsonb_agg(economy.party_room_json(p.id, v_user) || jsonb_build_object('course_track', p.course_track)
                       order by exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = v_user) desc,
                                p.created_at desc)
        from public.parties p
       where p.crew_id = p_crew
         and (p.status = 'LOBBY'
              or (p.status <> 'FINISHED'
                  and exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = v_user)))
    ), '[]'::jsonb));
end;
$$;
revoke all on function public.party_rooms(uuid) from public, anon;
grant execute on function public.party_rooms(uuid) to authenticated;

-- 새 대기실 — 크루원 누구나. 이름과 코스를 붙인다. 연 사람이 진행자다. 다른 대기실(출발 전)에서는 나온다.
-- 오늘 이미 크루 러닝을 했으면 열지 않는다(crew_run_done_today). 방을 연 것만으로 참여는 쓰이지 않는다.
-- p_request — 같은 요청을 다시 보내면(답을 잃었다) 새 방을 만들지 않고 그때 연 방을 돌려준다
create or replace function public.party_create(p_crew uuid, p_name text, p_course_name text, p_course_km numeric, p_course_track text,
                                               p_request uuid default null)
returns bigint
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_name text;
  v_party bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_crew is null or not public.party_can_enter(p_crew, null) then
    raise exception '크루원만 대기실을 열 수 있습니다' using errcode = '42501';
  end if;
  -- 한 사람의 방 열기 · 들어가기는 하나씩(party_join 과 같은 자물쇠) — 동시에 눌러도 두 방에 남지 않게
  perform pg_advisory_xact_lock(hashtext('party_user:' || v_user::text));
  if p_request is not null then
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    if found then
      return v_party;
    end if;
  end if;
  v_name := economy.party_room_name(p_name);
  perform economy.party_course_check(p_course_name, p_course_km, p_course_track);
  if exists (select 1 from economy.crew_runs c where c.user_id = v_user and c.day = economy.game_day(now())) then
    raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
  end if;
  perform economy.party_close_stale(p_crew);

  insert into public.parties (crew_id, host_id, creator_id, name, course_name, course_km, course_track, course_rev, create_key)
  values (p_crew, v_user, v_user, v_name, btrim(p_course_name), round(p_course_km, 3), p_course_track, 1, p_request)
  on conflict (creator_id, create_key) where create_key is not null and status <> 'FINISHED' do nothing
  returning id into v_party;
  if v_party is null then
    -- 같은 요청이 동시에 두 번 왔다 — 먼저 연 방
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    return v_party;
  end if;
  perform economy.party_leave_other_lobbies(v_user, v_party);
  insert into public.party_members (party_id, user_id) values (v_party, v_user);
  return v_party;
end;
$$;
revoke all on function public.party_create(uuid, text, text, numeric, text, uuid) from public, anon;
grant execute on function public.party_create(uuid, text, text, numeric, text, uuid) to authenticated;

-- 고른 대기실에 들어간다 — 같은 크루 · 출발 전 · 20명 안 · 진행자가 내보내지 않은 방. 이미 들어가 있으면 그대로(여러 번 불러도 같다).
-- 다른 대기실(출발 전)에서는 나온다. 이미 출발한 방에는 늦게 끼지 않는다(중도 합류 없음 — D10)
create or replace function public.party_join(p_party bigint)
returns bigint
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_party public.parties;
  v_crew uuid;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 한 사람의 방 열기 · 들어가기는 하나씩(party_create 와 같은 자물쇠). 그다음 크루 자물쇠(party_open 과 같은 것)를 방 줄보다 먼저 —
  -- 옛 앱의 party_open 과 동시에 들어와도 20명을 넘지 않고, 차례가 같아 서로 기다리다 멈추지 않는다
  perform pg_advisory_xact_lock(hashtext('party_user:' || v_user::text));
  select p.crew_id into v_crew from public.parties p where p.id = p_party;
  if v_crew is not null then
    perform pg_advisory_xact_lock(hashtext(v_crew::text));
  end if;
  select * into v_party from public.parties where id = p_party for update;
  if not found or v_party.crew_id is null then
    raise exception 'room_closed' using errcode = '23514', detail = '대기실이 없습니다';
  end if;
  if not public.party_can_enter(v_party.crew_id, null) then
    raise exception '크루원만 들어갈 수 있습니다' using errcode = '42501';
  end if;
  if exists (select 1 from public.party_members m where m.party_id = p_party and m.user_id = v_user) then
    update public.party_members set last_seen = now() where party_id = p_party and user_id = v_user;
    return p_party;
  end if;
  if exists (select 1 from public.party_kicks k where k.party_id = p_party and k.user_id = v_user) then
    raise exception '진행자가 내보낸 방입니다' using errcode = '42501';
  end if;
  if v_party.status = 'FINISHED' then
    raise exception 'room_closed' using errcode = '23514', detail = '닫힌 대기실입니다';
  end if;
  if v_party.status <> 'LOBBY' then
    raise exception 'room_started' using errcode = '23514', detail = '이미 출발한 대기실입니다';
  end if;
  if (select count(*) from public.party_members m where m.party_id = p_party) >= economy.crew_room_max() then
    raise exception 'room_full' using errcode = '23514', detail = '대기실이 가득 찼습니다';
  end if;
  perform economy.party_leave_other_lobbies(v_user, p_party);
  insert into public.party_members (party_id, user_id) values (p_party, v_user)
  on conflict (party_id, user_id) do update set last_seen = now();
  return p_party;
end;
$$;
revoke all on function public.party_join(bigint) from public, anon;
grant execute on function public.party_join(bigint) to authenticated;

-- 방의 코스 경로 — 방 사람만. 코스 판이 바뀌었을 때만 앱이 다시 받는다
create or replace function public.party_course(p_party bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
begin
  perform public.party_assert_member(p_party);
  return (select jsonb_build_object('course_rev', p.course_rev, 'course_name', p.course_name,
                                    'course_km', p.course_km, 'course_track', p.course_track)
            from public.parties p where p.id = p_party);
end;
$$;
revoke all on function public.party_course(bigint) from public, anon;
grant execute on function public.party_course(bigint) to authenticated;

-- 진행자가 코스를 바꾼다(출발 전만). 코스 판이 오르고, 진행자를 뺀 모두의 준비가 풀린다(D14). 새 판을 돌려준다
create or replace function public.party_set_course(p_party bigint, p_course_name text, p_course_km numeric, p_course_track text)
returns int
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_rev int;
begin
  perform economy.party_course_check(p_course_name, p_course_km, p_course_track);
  update public.parties p
     set course_name = btrim(p_course_name), course_km = round(p_course_km, 3), course_track = p_course_track,
         course_rev = p.course_rev + 1
   where p.id = p_party and p.host_id = auth.uid() and p.status = 'LOBBY' and p.crew_id is not null
  returning course_rev into v_rev;
  if v_rev is null then
    raise exception '대기실에서 진행자만 코스를 바꿀 수 있습니다' using errcode = '42501';
  end if;
  update public.party_members m set ready = false
   where m.party_id = p_party and m.user_id <> auth.uid() and m.ready;
  return v_rev;
end;
$$;
revoke all on function public.party_set_course(bigint, text, numeric, text) from public, anon;
grant execute on function public.party_set_course(bigint, text, numeric, text) to authenticated;

-- ── 나가기 — 0012 그대로 + 방을 잠그고 · 준비할 수 있는 사람에게 먼저 넘긴다 ──────────

-- 방 줄을 잠근다 — 막 들어온 사람이 있는 방을 닫지 않게(party_join 도 같은 줄을 잠근다)
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
  perform 1 from public.parties where id = p_party for update;
  delete from public.party_members where party_id = p_party and user_id = auth.uid();

  v_next := economy.party_next_host(p_party);

  if v_next is null then
    update public.parties set status = 'FINISHED' where id = p_party;
  else
    update public.parties set host_id = v_next
     where id = p_party and host_id = auth.uid();
  end if;
end;
$$;

-- ── 방 상태 — 0063 그대로 + 이름 · 코스 판 ───────────────────────────

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

  -- 대기실의 진행자가 30초 넘게 보이지 않는다 — 지금 보이는 사람 중 먼저 들어온 사람에게 넘긴다(0063).
  -- 오늘 크루 러닝을 아직 하지 않은(준비할 수 있는) 사람이 먼저(0072 — economy.party_next_host). 부른 사람이 방금 보였으니 비지 않는다
  update public.parties p
     set host_id = economy.party_next_host(p.id, interval '30 seconds')
   where p.id = p_party
     and p.status = 'LOBBY'
     and not exists (
       select 1 from public.party_members h
        where h.party_id = p.id and h.user_id = p.host_id and h.last_seen > now() - interval '30 seconds');

  select json_build_object(
    'id', p.id,
    'status', p.status,
    'host_id', p.host_id,
    'crew_id', p.crew_id,
    'flash_post_id', p.flash_post_id,
    'starts_at', p.starts_at,
    'server_now', now(),
    -- 0072 — 방 이름 · 코스(경로는 party_course) · 연 시각
    'name', p.name,
    'created_at', p.created_at,
    'course_name', p.course_name,
    'course_km', p.course_km,
    'course_rev', p.course_rev,
    'members', coalesce((
      select json_agg(json_build_object(
        'user_id', m.user_id,
        'name', coalesce(pr.display_name, '러너'),
        'ready', m.ready,
        'is_me', m.user_id = auth.uid(),
        'is_host', m.user_id = p.host_id,
        'share', m.share_location,
        'lat', case when v.visible then m.lat end,
        'lng', case when v.visible then m.lng end,
        'km', case when m.user_id = auth.uid() or (m.share_location and p.status = 'RUNNING') then m.km end,
        'seen_sec', extract(epoch from (now() - m.last_seen))::int,
        'ping_sec', case when v.visible and m.pinged_at is not null
                         then extract(epoch from (now() - m.pinged_at))::int end
      ) order by (m.user_id = p.host_id) desc, m.joined_at)
        from public.party_members m
        left join public.profiles pr on pr.id = m.user_id
        cross join lateral (
          select m.user_id = auth.uid()
              or (m.share_location and p.status = 'RUNNING' and m.pinged_at > now() - interval '2 minutes')
              as visible
        ) v
       where m.party_id = p.id
    ), '[]'::json)
  ) into v_state
  from public.parties p
  where p.id = p_party;

  return v_state;
end;
$$;

-- ── 출발 — 0071 그대로 + 6초 뒤 · 방 이름 · 코스를 참여 기록에 ───────────

create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_starts timestamptz;
  v_party public.parties;
  v_day date;
  v_level int;
  v_n int;
  v_added numeric;
  v_pool economy.crew_day_pools;
begin
  select * into v_party from public.parties where id = p_party and host_id = auth.uid() and status = 'LOBBY' for update;
  if not found then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  -- 크루 방은 6초 뒤 — 대기실은 2초마다 묻는다. 늦게 안 사람도 3 · 2 · 1 · 0 · StepUP! 을 처음부터 본다(0072). 번개러닝은 예전 그대로 4초
  v_starts := now() + case when v_party.crew_id is not null then interval '6 seconds' else interval '4 seconds' end;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  delete from public.party_members where party_id = p_party and not ready;

  if v_party.crew_id is not null then
    v_day := economy.game_day(v_starts);
    v_level := greatest(coalesce((select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
    -- 공용 풀을 먼저 잠근다(모든 크루 러닝 함수가 같은 차례) — 같은 크루의 다른 방이 동시에 출발해도 기여가 섞이지 않게
    perform economy.crew_pool_settle(v_party.crew_id, v_day, now());
    delete from public.party_members m
     where m.party_id = p_party
       and (exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day)
            or not exists (select 1 from public.crew_members cm where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id));
    select count(*)::int into v_n from public.party_members m where m.party_id = p_party;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    if not exists (select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()) then
      raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
    end if;
  end if;

  update public.parties p
     set status = 'COUNTDOWN', starts_at = v_starts,
         crew_level = case when p.crew_id is not null then v_level end
   where p.id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at, sneaker_id, shoe_dur_max)
  select p_party, m.user_id, v_starts, s.id, s.dur_max
    from public.party_members m
    left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
   where m.party_id = p_party
  on conflict do nothing;

  if v_party.crew_id is not null then
    -- 참여 · 신발 스냅샷 · 기여 = 최대 내구도 × (1 + 레벨 체력 보너스). 같은 날 다른 방에서 먼저 출발했으면(동시 출발) 이 방에서 뺀다
    with ins as (
      insert into economy.crew_runs (user_id, day, crew_id, party_id, started_at, state, last_beat_at,
                                     sneaker_id, shoe_dur_max, shoe_eff_bps, crew_level, contribution, level_pct,
                                     party_name, course_name, course_km, course_rev)
      select m.user_id, v_day, v_party.crew_id, p_party, v_starts, 'RUNNING', v_starts,
             s.id, coalesce(s.dur_max, 0), coalesce(e.efficiency_bps, 0), v_level,
             round(coalesce(s.dur_max, 0) * (1 + economy.crew_level_stamina(v_level)), 4), economy.crew_level_reward(v_level),
             v_party.name, v_party.course_name, v_party.course_km, v_party.course_rev
        from public.party_members m
        left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
        left join lateral economy.shoe_effective(s) e on s.id is not null
       where m.party_id = p_party
      on conflict (user_id, day) do nothing
      returning user_id, contribution)
    select count(*)::int, coalesce(sum(ins.contribution), 0) into v_n, v_added from ins;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    delete from public.party_runs r
     where r.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = r.user_id and c.party_id = p_party);
    delete from public.party_members m
     where m.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.party_id = p_party);
    update economy.crew_day_pools d
       set capacity = d.capacity + v_added, contributors = d.contributors + v_n, version = d.version + 1,
           exhausted_at = case when d.capacity + v_added > d.consumed then null else d.exhausted_at end,
           low_at = case when d.capacity + v_added - d.consumed > (d.capacity + v_added) * 0.2 then null else d.low_at end
     where d.crew_id = v_party.crew_id and d.day = v_day
    returning * into v_pool;
    -- 새 기여로 다시 생겼으면 0 이던 구간을 닫는다(그 뒤로는 다시 적립 — D06)
    if v_pool.capacity > v_pool.consumed then
      update economy.crew_pool_empty x set to_at = now()
       where x.crew_id = v_party.crew_id and x.day = v_day and x.to_at is null;
    end if;
  end if;
end;
$$;

-- 출발 — 진행자가 본 코스 판과 방의 코스 판이 같을 때만(장면 33 · T68). 다르면 course_changed — 앱이 새 코스를 보이고 다시 묻는다
create or replace function public.party_start_rev(p_party bigint, p_course_rev int)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
begin
  -- 진행자면 방 줄을 먼저 잠그고 판을 본다 — 판을 본 뒤 출발하기 전에 코스가 바뀌지 않게(party_start 도 같은 줄을 잠근다)
  perform 1 from public.parties p where p.id = p_party and p.host_id = auth.uid() for update;
  if exists (select 1 from public.parties p where p.id = p_party and p.course_rev is distinct from p_course_rev) then
    raise exception 'course_changed' using errcode = '23514', detail = '코스가 바뀌었습니다. 새 코스를 확인하고 다시 출발하세요';
  end if;
  perform public.party_start(p_party);
end;
$$;
revoke all on function public.party_start_rev(bigint, int) from public, anon;
grant execute on function public.party_start_rev(bigint, int) to authenticated;

-- ── 결과 — 0071 그대로 + 그때의 방 이름 · 코스 · 신발 · 레벨(지난 기록 상세, 장면 59) ──────────

create or replace function public.crew_run_result(p_started_at timestamptz)
returns jsonb
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_s public.walk_sessions;
  c economy.crew_runs;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into v_s from public.walk_sessions s where s.user_id = v_user and s.started_at = p_started_at;
  if not found or v_s.energy_pool <> 'CREW' then
    return null;
  end if;
  select * into c from economy.crew_runs r where r.user_id = v_user and r.session_id = v_s.id;
  if not found then
    return null;
  end if;
  return jsonb_build_object(
    'settled', c.settled_at is not null,
    'day', c.day,
    'crew_id', c.crew_id,
    'crew_name', (select cr.name from public.crews cr where cr.id = c.crew_id),
    'session_id', c.session_id,
    -- 0072 — 그때의 방 이름 · 코스 · 신발 · 레벨(지금 크루를 옮겼거나 레벨이 올라도 그대로)
    'party_id', c.party_id,
    'party_name', c.party_name,
    'course_name', c.course_name,
    'course_km', c.course_km,
    'started_at', c.started_at,
    'crew_level', c.crew_level,
    'shoe', jsonb_build_object('sneaker_id', c.sneaker_id, 'dur_max', c.shoe_dur_max, 'eff_bps', c.shoe_eff_bps),
    'verdict', v_s.verdict,
    'reason', v_s.verdict_reason,
    'terminal_reason', c.terminal_reason,
    'participants', (select count(*) from economy.crew_runs r where r.party_id = c.party_id),
    'reward', jsonb_build_object('base', c.base_sup, 'headcount_pct', c.bonus_pct, 'headcount', c.headcount_sup,
                                 'level_pct', c.level_pct, 'level', c.level_sup, 'total', c.total_sup),
    'bonus', jsonb_build_object('status', c.bonus_status, 'count', c.bonus_count, 'pct', c.bonus_pct, 'reason', c.bonus_reason),
    'pool_usage', jsonb_build_object('contribution', c.contribution, 'consumed', c.consumed, 'active_sec', c.active_sec,
                                     'pool', economy.crew_pool_json(c.crew_id, c.day), 'personal_durability_delta', 0),
    'xp', jsonb_build_object('distance_m', c.xp_m, 'applied', coalesce(c.xp_m, 0) > 0,
                             'level_before', c.level_before, 'level_after', c.level_after,
                             'run_km', (select cr.run_km from public.crews cr where cr.id = c.crew_id),
                             'next_level_km', (select economy.crew_level_km(coalesce(cr.level, 1) + 1)
                                                 from public.crews cr where cr.id = c.crew_id)),
    'next_eligible_at', economy.game_day_start(c.day + 1));
end;
$$;
revoke all on function public.crew_run_result(timestamptz) from public, anon;
grant execute on function public.crew_run_result(timestamptz) to authenticated;
