-- ════════════════════════════════════════════════════════════════════
--  0074 — 크루 러닝 대기실: 달리는 방식(같이 뛰기 · 원격 뛰기) · 만나는 곳 · 출발 시각 · 정원
--         (2026-10-26 디자인 자료 "새 대기실 만들기" — 사용자 "이거 적용해줘", 결정 "서버까지 다")
--
--  - 같이 뛰기(TOGETHER): 같은 장소에 모여 함께 출발한다. 만나는 곳(이름 · 좌표)을 붙일 수 있다.
--  - 원격 뛰기(REMOTE): 각자의 위치에서 같은 시간에 출발한다. 만나는 곳은 없다.
--    서로 떨어져 있으니 2분 500m 인원 보너스는 세지 않는다(bonus_reason 'REMOTE'). 공용 체력 · 하루 한 번 · 레벨 보상은 그대로다.
--  - 출발 시각은 안내 · 목록 표시용이다 — 출발은 지금처럼 진행자가 누른다(서버가 저절로 출발시키지 않는다).
--  - 정원은 2 ~ 20명(크루 방 최대 20 그대로). 정원이 차면 party_join 이 room_full.
--  - 옛 앱은 party_create(0072 · 0073)를 그대로 쓴다 — 같이 뛰기 · 만나는 곳 없음 · 출발 시각 없음 · 정원 20.
--    새 앱은 party_create_v2 를 쓴다(이름이 다른 함수 — 같은 이름에 인자만 늘리면 PostgREST 가 둘 중 고르지 못한다).
-- ════════════════════════════════════════════════════════════════════

alter table public.parties add column if not exists run_mode text not null default 'TOGETHER';
alter table public.parties add column if not exists meet_place text;
alter table public.parties add column if not exists meet_lat double precision;
alter table public.parties add column if not exists meet_lng double precision;
alter table public.parties add column if not exists plan_at timestamptz;
alter table public.parties add column if not exists capacity int;

do $$
begin
  if not exists (select 1 from pg_constraint where conname = 'parties_room_options_sane') then
    alter table public.parties add constraint parties_room_options_sane check (
      run_mode in ('TOGETHER', 'REMOTE')
      and (meet_place is null or char_length(meet_place) between 1 and 60)
      and ((meet_lat is null) = (meet_lng is null))
      and (meet_lat is null or (meet_lat between -90 and 90 and meet_lng between -180 and 180))
      and (run_mode = 'TOGETHER' or (meet_place is null and meet_lat is null))
      and (capacity is null or capacity between 2 and 20));
  end if;
end $$;

-- 방의 정원 — 정하지 않았으면 크루 방 최대(20)
create or replace function economy.party_capacity(p_party bigint) returns int
language sql stable security definer set search_path = public, economy as $$
  select least(coalesce(p.capacity, economy.crew_room_max()), economy.crew_room_max()) from public.parties p where p.id = p_party
$$;
revoke all on function economy.party_capacity(bigint) from public;

-- 새 대기실(0074) — 0073 의 party_create 와 같고 달리는 방식 · 만나는 곳 · 출발 시각 · 정원을 더한다
create or replace function public.party_create_v2(
  p_crew uuid, p_name text, p_course_name text, p_course_km numeric, p_course_track text, p_request uuid,
  p_mode text, p_meet_place text, p_meet_lat double precision, p_meet_lng double precision, p_plan_at timestamptz, p_capacity int)
returns bigint
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_name text;
  v_course boolean;
  v_mode text := upper(btrim(coalesce(p_mode, 'TOGETHER')));
  v_place text := nullif(btrim(regexp_replace(coalesce(p_meet_place, ''), '\s+', ' ', 'g')), '');
  v_party bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_crew is null or not public.party_can_enter(p_crew, null) then
    raise exception '크루원만 대기실을 열 수 있습니다' using errcode = '42501';
  end if;
  perform pg_advisory_xact_lock(hashtext('party_user:' || v_user::text));
  if p_request is not null then
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    if found then
      return v_party;
    end if;
  end if;
  v_name := economy.party_room_name(p_name);
  v_course := economy.party_course_given(p_course_name, p_course_km, p_course_track);
  if v_mode not in ('TOGETHER', 'REMOTE')
     or (v_mode = 'REMOTE' and (v_place is not null or p_meet_lat is not null))
     or (v_place is not null and char_length(v_place) > 60)
     or ((p_meet_lat is null) <> (p_meet_lng is null))
     or (p_meet_lat is not null and (p_meet_lat not between -90 and 90 or p_meet_lng not between -180 and 180))
     or (p_capacity is not null and p_capacity not between 2 and 20)
     or (p_plan_at is not null and (p_plan_at < now() - interval '10 minutes' or p_plan_at > now() + interval '14 days')) then
    raise exception 'room_options_invalid' using errcode = '22023', detail = '달리는 방식 · 만나는 곳 · 출발 시각 · 정원을 확인하세요';
  end if;
  if exists (select 1 from economy.crew_runs c where c.user_id = v_user and c.day = economy.game_day(now())) then
    raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
  end if;
  perform economy.party_close_stale(p_crew);

  insert into public.parties (crew_id, host_id, creator_id, name, course_name, course_km, course_track, course_rev, create_key,
                              run_mode, meet_place, meet_lat, meet_lng, plan_at, capacity)
  values (p_crew, v_user, v_user, v_name,
          case when v_course then btrim(p_course_name) end,
          case when v_course then round(p_course_km, 3) end,
          case when v_course then p_course_track end,
          case when v_course then 1 else 0 end,
          p_request,
          v_mode, v_place, p_meet_lat, p_meet_lng, p_plan_at, p_capacity)
  on conflict (creator_id, create_key) where create_key is not null and status <> 'FINISHED' do nothing
  returning id into v_party;
  if v_party is null then
    select id into v_party from public.parties
     where creator_id = v_user and create_key = p_request and status <> 'FINISHED';
    return v_party;
  end if;
  perform economy.party_leave_other_lobbies(v_user, v_party);
  insert into public.party_members (party_id, user_id) values (v_party, v_user);
  return v_party;
end;
$$;
revoke all on function public.party_create_v2(uuid, text, text, numeric, text, uuid, text, text, double precision, double precision, timestamptz, int)
  from public, anon;
grant execute on function public.party_create_v2(uuid, text, text, numeric, text, uuid, text, text, double precision, double precision, timestamptz, int)
  to authenticated;

-- 방 한 줄(목록 · 상태 공통) — 0072 그대로 + 달리는 방식 · 만나는 곳 · 출발 시각 · 방의 정원
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
    'capacity', economy.party_capacity(p.id),
    'course_name', p.course_name,
    'course_km', p.course_km,
    'course_rev', p.course_rev,
    'run_mode', p.run_mode,
    'meet_place', p.meet_place,
    'meet_lat', p.meet_lat,
    'meet_lng', p.meet_lng,
    'plan_at', p.plan_at,
    'member_names', (select coalesce(jsonb_agg(coalesce(pr.display_name, '러너') order by m.joined_at), '[]'::jsonb)
                       from (select * from public.party_members mm where mm.party_id = p.id order by mm.joined_at limit 4) m
                       left join public.profiles pr on pr.id = m.user_id),
    'mine', exists (select 1 from public.party_members m where m.party_id = p.id and m.user_id = p_user),
    'kicked', exists (select 1 from public.party_kicks k where k.party_id = p.id and k.user_id = p_user))
    from public.parties p
   where p.id = p_party
$$;
revoke all on function economy.party_room_json(bigint, uuid) from public;

-- 들어가기 — 0072 그대로, 정원은 방의 정원으로
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
  if (select count(*) from public.party_members m where m.party_id = p_party) >= economy.party_capacity(p_party) then
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

-- 방 상태 — 0072 그대로 + 달리는 방식 · 만나는 곳 · 출발 시각 · 정원
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
    'name', p.name,
    'created_at', p.created_at,
    'course_name', p.course_name,
    'course_km', p.course_km,
    'course_rev', p.course_rev,
    -- 0074
    'run_mode', p.run_mode,
    'meet_place', p.meet_place,
    'meet_lat', p.meet_lat,
    'meet_lng', p.meet_lng,
    'plan_at', p.plan_at,
    'capacity', economy.party_capacity(p.id),
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

-- 2분 인원 보너스 — 0071 그대로 + 원격 뛰기 방은 세지 않는다(서로 떨어져 달린다)
create or replace function economy.crew_run_bonus_eval(p_user uuid, p_day date, p_final boolean default false)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  r economy.crew_runs;
  v_check timestamptz;
  v_n int;
  v_located boolean;
begin
  select * into r from economy.crew_runs c where c.user_id = p_user and c.day = p_day for update;
  if not found or r.bonus_status <> 'PENDING' then
    return;
  end if;
  if exists (select 1 from public.parties p where p.id = r.party_id and p.run_mode = 'REMOTE') then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'REMOTE', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  v_check := r.started_at + interval '2 minutes';
  if now() < v_check + interval '15 seconds' and not p_final then
    return;
  end if;
  if (r.ended_at is not null and r.ended_at < v_check) or now() < v_check then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'ENDED_BEFORE_CHECK', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  v_located := exists (select 1 from public.party_members m
                        where m.party_id = r.party_id and m.user_id = p_user and m.pinged_at is not null)
            or exists (select 1 from public.party_near n
                        where n.party_id = r.party_id and (n.user_a = p_user or n.user_b = p_user));
  if not v_located then
    update economy.crew_runs c set bonus_status = 'UNAVAILABLE', bonus_reason = 'NO_LOCATION', bonus_count = 0, bonus_pct = 0
     where c.user_id = p_user and c.day = p_day;
    return;
  end if;
  with recursive e as (
    select n.user_a, n.user_b from public.party_near n
     where n.party_id = r.party_id
       and n.first_at <= r.started_at + interval '90 seconds'
       and n.last_at >= v_check),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select count(*)::int into v_n
    from g
    join economy.crew_runs c on c.user_id = g.u and c.party_id = r.party_id
    join public.crew_members m on m.crew_id = r.crew_id and m.user_id = g.u
   where economy.crew_bonus_person(g.u);
  update economy.crew_runs c
     set bonus_status = 'CONFIRMED', bonus_count = v_n, bonus_pct = economy.crew_headcount_bonus(v_n),
         bonus_reason = case when v_n < 2 then 'TOO_FEW' end
   where c.user_id = p_user and c.day = p_day;
end;
$$;
revoke all on function economy.crew_run_bonus_eval(uuid, date, boolean) from public;
