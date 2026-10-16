-- ════════════════════════════════════════════════════════════════════
--  0056 — 함께 달리기 · 크루 점검 수정 (2026-10-15 점검)
--
--  1) 함께 달리기 보너스 인원은 "실제로 곁에서 같이 뛴 사람"만 센다.
--     0032 는 출발 2분 뒤에도 위치를 보낸 사람, 또는 이 방으로 기록을 올린 사람을 셌다.
--     그래서 다른 계정 몇 개로 방을 채우고 열 걸음 뒤 마치거나(무효 · 짧은 기록도 셌다),
--     다른 도시에서 앱만 켜 둬도 +10%씩 붙었다. 서버는 앱이 보내는 인원(p_party_size)을 쓰지 않으므로
--     앱의 "진행자와 100m" 규칙은 화면 예상에만 걸렸다.
--     이제 달리는 동안의 위치 보고(party_ping)에서 30초 안에 위치를 보낸 다른 참가자가 200m 안에 있으면
--     두 사람을 "곁"으로 적고(party_near), 인원은 출발 2분 뒤에 곁이었던 사람들로 이어진 내 무리로 센다
--     (곁의 곁도 같은 무리 — 속도가 달라 흩어진 무리도 한 무리로 센다). 본인도 무리에 아무도 없으면 1명이다.
--     인원 상한(+50%) · 한 방에 한 번 · 하루 상한은 그대로다.
--     진행자가 방을 닫은(마친) 뒤에도 남은 사람의 앱은 자기 러닝이 끝날 때까지 위치를 보낸다 — 닫힌 방의 좌표는
--     본인 말고 아무에게도 보이지 않고(party_state 는 RUNNING 방만 보인다), 나갈 때(party_leave) 줄과 함께 지워진다.
--     그러지 않으면 진행자가 2분 전에 마친 방은 끝까지 같이 뛴 사람들도 인원이 1이 됐다.
--  2) 다른 사람 위치는 "위치를 보낸 시각"(pinged_at)으로 2분 안의 것만 보인다. 0037 은 방 상태를 물을 때마다
--     적히는 last_seen 으로 봐서, 일시정지하거나 GPS 를 잃은 사람의 옛 점이 계속 지금 위치처럼 보였다.
--     방이 끝나면 남은 좌표 · 거리를 지운다.
--  3) 진행자가 내보낸 사람은 그 방에 다시 들어오지 못한다(대기실 다시 시도로 다시 들어왔다).
--  4) 크루 순위에 적는 러닝(session_tag_crew)은 그 크루의 함께 달리기 방에서 달린 러닝만 — 크루원이면
--     혼자 달린 옛 러닝도 크루 기록에 얹을 수 있었다.
--  5) 크루 해산 · 가입 신청 결정은 크루를 잠근 뒤 크루장인지 본다(넘기는 중에 옛 크루장이 해산하지 못하게).
--  6) 크루 채팅 알림: 지우거나 가린 메시지는 보내다 실패해 다시 보낼 줄에서도 빠지고(지금 보내는 중인 줄은
--     그대로 보내는 쪽에 맡긴다), 합쳐진 알림 줄은 아직 보이는 앞 메시지로 바뀐다(같은 사람에게 기다리는 다른
--     줄이 있으면 지운다 — 같은 메시지 알림이 두 번 가지 않게). 크루에서 나간(내보내진) 사람에게 기다리던 알림은
--     보내지 않는다. 새로 만든 표(party_near · party_kicks)의 사람 칸은 계정을 지우면 함께 지워진다.
-- ════════════════════════════════════════════════════════════════════

-- ── 1) 곁에서 같이 뛴 짝 ─────────────────────────────────────────────

alter table public.party_members add column if not exists pinged_at timestamptz;

create table if not exists public.party_near (
  party_id bigint not null references public.parties on delete cascade,
  -- 계정을 지우면(0021 은 auth.users 에서 이어 지운다) 그 사람의 짝도 지운다
  user_a uuid not null references auth.users on delete cascade,
  user_b uuid not null references auth.users on delete cascade,
  first_at timestamptz not null default now(),
  last_at timestamptz not null default now(),
  primary key (party_id, user_a, user_b),
  constraint party_near_order check (user_a < user_b)
);
alter table public.party_near enable row level security;
revoke all on public.party_near from anon, authenticated;

comment on table public.party_near is
  '함께 달리기 중 서로 곁(200m)에 있던 두 사람. party_ping 이 적고 보너스 인원(economy.party_for)이 읽는다. 앱은 볼 수 없다.';

-- 달리는 동안의 위치 보고. 곁에 있는 사람을 적는다 — 보너스 인원은 이것으로 센다.
create or replace function public.party_ping(p_party bigint, p_lat double precision, p_lng double precision)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  perform public.party_assert_member(p_party);
  if p_lat is not null and (p_lat not between -90 and 90 or p_lng is null or p_lng not between -180 and 180) then
    raise exception '위치가 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 위치 없이 오면(GPS 를 잃었다) 남은 옛 점을 지운다 — 다른 사람에게 지금 위치처럼 보이지 않게
  update public.party_members
     set lat = p_lat, lng = case when p_lat is null then null else p_lng end, last_seen = now(),
         pinged_at = case when p_lat is not null then now() else pinged_at end
   where party_id = p_party and user_id = v_me;
  if p_lat is null then
    return;
  end if;
  -- 위치가 있는 보고만 "뛰는 중"으로 친다(출발한 사람만)
  update public.party_runs
     set last_ping = now()
   where party_id = p_party and user_id = v_me and starts_at <= now();
  if not found then
    return;
  end if;
  -- 30초 안에 위치를 보낸, 같이 출발한 다른 사람이 200m 안에 있으면 곁으로 적는다
  insert into public.party_near (party_id, user_a, user_b)
  select p_party, least(v_me, o.user_id), greatest(v_me, o.user_id)
    from public.party_members o
    join public.party_runs r
      on r.party_id = o.party_id and r.user_id = o.user_id and r.starts_at <= now()
   where o.party_id = p_party and o.user_id <> v_me
     and o.pinged_at > now() - interval '30 seconds'
     and o.lat is not null and o.lng is not null
     and economy.haversine_m(p_lat, p_lng, o.lat, o.lng) <= 200
  on conflict (party_id, user_a, user_b) do update set last_at = now();
end;
$$;

-- 보너스 인원 — 이 러닝의 방에서, 출발 2분 뒤에 곁이었던 사람들로 이어진 내 무리(나 포함)
create or replace function economy.party_for(p_user uuid, p_started_at timestamptz,
                                             out party_id bigint, out size int)
language sql stable security definer set search_path = public as $$
  with recursive p as (
    select r.party_id, r.starts_at from public.party_runs r
     where r.user_id = p_user
       and r.starts_at between p_started_at - interval '10 minutes'
                           and p_started_at + interval '10 minutes'
     order by abs(extract(epoch from (r.starts_at - p_started_at)))
     limit 1),
  e as (
    select n.user_a, n.user_b
      from public.party_near n join p on n.party_id = p.party_id
     where n.last_at >= p.starts_at + interval '2 minutes'),
  g(u) as (
    select p_user
    union
    select case when e.user_a = g.u then e.user_b else e.user_a end
      from e join g on g.u = e.user_a or g.u = e.user_b)
  select p.party_id, (select count(*)::int from g) from p
$$;
revoke all on function economy.party_for(uuid, timestamptz) from public;

-- ── 2) 위치는 보낸 시각으로 · 끝난 방의 좌표는 지운다 ───────────────

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

-- 진행자가 마치면 방을 닫고, 남은 좌표 · 거리를 지운다
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
  if found then
    update public.party_members set lat = null, lng = null, km = null
     where party_id = p_party;
  end if;
end;
$$;

-- ── 3) 내보낸 사람은 그 방에 다시 들어오지 못한다 ───────────────────

create table if not exists public.party_kicks (
  party_id bigint not null references public.parties on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  kicked_at timestamptz not null default now(),
  primary key (party_id, user_id)
);
alter table public.party_kicks enable row level security;
revoke all on public.party_kicks from anon, authenticated;

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
  if found then
    insert into public.party_kicks (party_id, user_id) values (p_party, p_user)
    on conflict do nothing;
  end if;
end;
$$;

-- 로비 열기 — 0012 와 같고, 내보내진 방에는 다시 들어가지 않는다 · 닫은 방의 좌표를 지운다
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
  -- 오늘 들어가게 된다. 닫는 방에 남은 위치 · 거리는 지운다.
  with closed as (
    update public.parties p set status = 'FINISHED'
     where p.status <> 'FINISHED'
       and (p.created_at < now() - interval '6 hours'
            or not exists (
              select 1 from public.party_members m
               where m.party_id = p.id and m.last_seen > now() - interval '10 minutes'
            ))
    returning p.id)
  update public.party_members m set lat = null, lng = null, km = null
    from closed
   where m.party_id = closed.id;

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

  -- 진행자가 내보낸 방에는 다시 들어가지 않는다
  if v_party is not null and exists (
    select 1 from public.party_kicks k where k.party_id = v_party and k.user_id = v_user
  ) then
    raise exception '진행자가 내보낸 방입니다' using errcode = '42501';
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

-- ── 4) 크루 순위에는 그 크루의 함께 달리기 방에서 달린 러닝만 ───────

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
  -- 정산이 이 러닝을 그 크루의 방(party_id)에 묶었을 때만 적는다
  update public.walk_sessions s
     set crew_id = p_crew
   where s.user_id = auth.uid() and s.started_at = p_started_at and s.crew_id is null
     and exists (select 1 from public.parties p where p.id = s.party_id and p.crew_id = p_crew);
  return found;
end;
$$;

-- ── 5) 크루를 잠근 뒤 크루장인지 본다 ───────────────────────────────

create or replace function public.crew_dissolve(p_crew uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_lock_owner(p_crew);
  delete from public.crews where id = p_crew;
end;
$$;

create or replace function public.crew_application_decide(p_application bigint, p_approve boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew_id uuid;
  v_crew public.crews%rowtype;
  v_app public.crew_applications%rowtype;
  v_count int;
begin
  select a.crew_id into v_crew_id from public.crew_applications a where a.id = p_application;
  if not found then
    raise exception 'application_missing' using errcode = '22023', detail = '신청서를 찾을 수 없습니다';
  end if;
  perform public.crew_lock_owner(v_crew_id);
  select * into v_crew from public.crews c where c.id = v_crew_id;
  select * into v_app from public.crew_applications a where a.id = p_application for update;
  if v_app.status <> 'PENDING' then
    raise exception 'application_decided' using errcode = '22023', detail = v_app.status;
  end if;

  select count(*) into v_count from public.crew_members m where m.crew_id = v_crew_id;
  if coalesce(p_approve, false) then
    if v_crew.capacity is not null and v_count >= v_crew.capacity then
      raise exception 'crew_full' using errcode = '22023', detail = '정원이 가득 찼습니다';
    end if;
    insert into public.crew_members (crew_id, user_id, role) values (v_crew_id, v_app.user_id, 'MEMBER')
    on conflict do nothing;
    update public.crew_applications set status = 'APPROVED', decided_at = now() where id = p_application;
  else
    update public.crew_applications set status = 'DECLINED', decided_at = now() where id = p_application;
  end if;

  return jsonb_build_object(
    'status', case when coalesce(p_approve, false) then 'APPROVED' else 'DECLINED' end,
    'member_count', (select count(*) from public.crew_members m where m.crew_id = v_crew_id),
    'capacity', v_crew.capacity,
    'pending_count', (select count(*) from public.crew_applications a where a.crew_id = v_crew_id and a.status = 'PENDING'));
end;
$$;

-- ── 6) 크루 채팅 알림 정리 ───────────────────────────────────────────

-- 메시지가 지워지거나 가려졌다 — 아직 보내지 못한 그 메시지의 알림 줄(보내다 실패해 다시 보낼 줄 포함)을
-- 그 줄이 기다리기 시작한 뒤 온, 아직 보이는 가장 새 메시지로 바꾼다. 그런 메시지가 없으면 지운다.
create or replace function public.crew_chat_push_drop()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  o record;
  v_next public.crew_chat_messages;
begin
  for o in
    select q.id, q.user_id, q.created_at from public.push_outbox q
     where q.kind = 'CREW_CHAT' and q.link = 'crew-chat/' || new.crew_id
       and q.sent_at is null
       -- 지금 보내는 중인 줄(가져간 지 2분 안)은 보내는 쪽에 맡긴다. 보내다 실패해 다시 가져갈 줄은 고친다
       and (q.claimed_at is null or q.claimed_at < now() - interval '2 minutes')
       and q.args->>'msg' = new.id::text
  loop
    -- 같은 사람에게 이 채팅의 다른 알림 줄이 아직 남아 있으면(더 새 메시지로 기다리는 줄) 이 줄은 지운다 —
    -- 가장 새 메시지로 고쳐 두면 그 줄과 같은 메시지 알림이 두 번 갔다
    if exists (select 1 from public.push_outbox q2
                where q2.id <> o.id and q2.user_id = o.user_id and q2.kind = 'CREW_CHAT'
                  and q2.link = 'crew-chat/' || new.crew_id and q2.sent_at is null) then
      delete from public.push_outbox where id = o.id;
      continue;
    end if;
    select m.* into v_next from public.crew_chat_messages m
     where m.crew_id = new.crew_id and m.id <> new.id
       and m.kind <> 'SYSTEM' and m.deleted_at is null and m.hidden_at is null
       and m.author_id <> o.user_id
       and m.created_at >= o.created_at
       and not exists (select 1 from public.user_blocks b
                        where b.blocker_id = o.user_id and b.blocked_id = m.author_id)
     order by m.seq desc
     limit 1;
    if found then
      update public.push_outbox
         set args = jsonb_build_object(
               'crew', (select c.name from public.crews c where c.id = v_next.crew_id),
               'name', public.push_display_name(v_next.author_id),
               'text', left(regexp_replace(v_next.body, '\s+', ' ', 'g'), 80),
               'photo', case when v_next.kind = 'IMAGE' then '1' else '0' end,
               'msg', v_next.id::text)
       where id = o.id;
    else
      delete from public.push_outbox where id = o.id;
    end if;
  end loop;
  return new;
end;
$$;

-- 크루에서 나갔거나 내보내졌다 — 그 크루 채팅의 기다리던 알림은 보내지 않는다
create or replace function public.crew_chat_push_member_gone()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.push_outbox o
   where o.user_id = old.user_id and o.kind = 'CREW_CHAT'
     and o.link = 'crew-chat/' || old.crew_id and o.sent_at is null;
  return old;
end;
$$;

drop trigger if exists crew_chat_push_member_gone on public.crew_members;
create trigger crew_chat_push_member_gone
  after delete on public.crew_members
  for each row
  execute function public.crew_chat_push_member_gone();

revoke all on function public.crew_chat_push_drop() from public, anon, authenticated;
revoke all on function public.crew_chat_push_member_gone() from public, anon, authenticated;
