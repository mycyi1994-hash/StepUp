-- ════════════════════════════════════════════════════════════════════
--  0047 — 크루 명함형(2026-09-28 확정 2번)
--
--  목록 카드와 상세가 보여 주는 것: 대표 이미지 · 크루장 · 레벨 · 인원/정원 · 한 줄 소개 ·
--  활동 지역 · 정기 모임 · 한 번에 달리는 거리 · 분위기 · 이번 주 목표.
--
--  이 파일이 정하는 것
--    · 크루에 붙는 값(크루장 한마디 · 대표 이미지 · 모임 · 정원 · 모집 중/쉬는 중 · 주간 목표 · 레벨 칸)
--    · 가입 신청서(crew_applications) — 고른 문구와 한마디를 남기고, 승인 · 미승인 · 취소를 기록한다.
--      예전 가입 신청(crew_join_requests, 대기만 있고 결과가 남지 않았다)은 여기로 옮기고 지운다.
--    · 크루장만 하는 일(수정 · 모집 멈춤/재개 · 목표 · 승인 · 내보내기 · 크루장 넘기기 · 해산)은 함수로만.
--
--  이 파일이 정하지 않는 것(패키지가 정책을 새로 확정하지 않았다)
--    · 레벨 계산식 · 승급 기준 · 등급 이름 · 보상. level 칸은 비어 있고 어느 함수도 채우지 않는다 → 앱은 "새 크루".
--    · 이름 중복 금지(기존에도 없다) · 한 사람이 가질 크루 수(기존 10개 상한 그대로) · 다중 가입(기존처럼 허용).
--    · 크루 거리: 기존 규칙 그대로 — 크루 러닝으로 적힌 러닝(walk_sessions.crew_id, 무효 제외, 0015).
--      이번 주는 한국 시간 월요일 0시부터. 목표 · 참여 인원은 지금 멤버의 크루 러닝만 센다.
--    · 알림: 새 푸시를 만들지 않는다. 가입 신청이 들어오면 크루장에게 가던 기존 푸시(0017)만 새 표로 옮긴다.
--
--  글자 수 상한은 표를 지키는 기술적 한도다(크루장 한마디 · 신청 한마디 300자, 사진 base64 200,000자).
-- ════════════════════════════════════════════════════════════════════

-- ────────────────────────────────────────────────────────────────────
--  크루에 붙는 값
-- ────────────────────────────────────────────────────────────────────

alter table public.crews
  add column if not exists leader_note text not null default '',
  add column if not exists image_bg smallint,
  add column if not exists image_ver int not null default 0,
  add column if not exists meet_days smallint not null default 0,
  add column if not exists meet_time smallint,
  add column if not exists run_distance text,
  add column if not exists moods text[] not null default '{}',
  add column if not exists capacity int,
  add column if not exists recruiting boolean not null default true,
  add column if not exists recruit_changed_at timestamptz,
  add column if not exists weekly_goal_km int,
  add column if not exists level int,
  add column if not exists client_key uuid;

-- 예전 크루는 만든 때를 모집 시작으로 본다(최근 모집순)
update public.crews set recruit_changed_at = created_at where recruit_changed_at is null;
alter table public.crews alter column recruit_changed_at set default now();
alter table public.crews alter column recruit_changed_at set not null;

do $$
declare r record;
begin
  for r in select * from (values
    ('crews_leader_note_len', 'check (length(leader_note) <= 300)'),
    ('crews_image_bg_range', 'check (image_bg is null or image_bg between 0 and 3)'),
    ('crews_meet_days_range', 'check (meet_days between 0 and 127)'),
    ('crews_meet_time_range', 'check (meet_time is null or meet_time between 0 and 1439)'),
    ('crews_meet_time_days', 'check (meet_time is null or meet_days > 0)'),
    ('crews_run_distance_check', 'check (run_distance is null or run_distance in (''D1_3'', ''D3_5'', ''D5P''))'),
    ('crews_moods_check', 'check (moods <@ array[''WALK_FIRST'', ''EASY'', ''RECORD'', ''BEGINNER'', ''EXPERIENCED'']::text[])'),
    ('crews_capacity_range', 'check (capacity is null or capacity between 1 and 9999)'),
    ('crews_goal_range', 'check (weekly_goal_km is null or weekly_goal_km between 1 and 99999)'),
    ('crews_level_range', 'check (level is null or level >= 1)')
  ) as t(name, def) loop
    if not exists (select 1 from pg_constraint where conname = r.name) then
      execute format('alter table public.crews add constraint %I %s', r.name, r.def);
    end if;
  end loop;
end $$;

-- 같은 만들기 요청이 두 번 와도(응답을 못 받고 다시 누름) 크루는 하나
create unique index if not exists crews_owner_client_key
  on public.crews (owner_id, client_key) where client_key is not null;

comment on column public.crews.leader_note is '크루장 한마디(선택). 상세 소개와 크루장 프로필에 보인다';
comment on column public.crews.image_bg is '이름이 들어간 기본 이미지의 바탕(0~3). 비어 있으면 앱이 크루 id 로 고른다';
comment on column public.crews.image_ver is '대표 사진을 바꾸거나 지울 때마다 1씩 오른다 — 앱이 내려받은 사진을 새로 받을지 가른다';
comment on column public.crews.meet_days is '정기 모임 요일. 월=1, 화=2, 수=4 … 일=64 를 더한 값. 0 이면 정해진 일정 없음';
comment on column public.crews.meet_time is '정기 모임 시작 시각(0시부터 분). 비어 있으면 시간 없음';
comment on column public.crews.run_distance is '한 번에 달리는 거리 — D1_3(1–3km) · D3_5(3–5km) · D5P(5km 이상)';
comment on column public.crews.moods is '분위기 — WALK_FIRST 걷기부터 · EASY 천천히 · RECORD 기록 도전 · BEGINNER 처음도 환영 · EXPERIENCED 경험자 모임';
comment on column public.crews.capacity is '모집 정원(크루장 포함). 비어 있으면 예전 크루 — 정원 없음';
comment on column public.crews.recruiting is '모집 중이면 true. 멈춰도 멤버 · 크루장은 그대로다';
comment on column public.crews.weekly_goal_km is '이번 주 함께 달릴 거리(km). 비어 있으면 목표 없음 — 달성률을 나누지 않는다';
comment on column public.crews.level is '크루 레벨. 계산식이 정해지지 않아 어느 함수도 채우지 않는다. 비어 있으면 "새 크루"';

-- 레벨 · 정원 같은 칸을 크루장이 표에 직접 고치지 못하게 한다. 고치는 일은 아래 함수로만.
revoke update on public.crews from anon, authenticated;

-- 가입도 함수로만(crew_join · crew_apply → 승인). 0035 가 남긴 직접 가입(자유 가입 크루)은 모집 쉼 · 정원을
-- 보지 않아, 정원 2명인 크루에 3명이 들어갔다. 앱은 표에 직접 쓰지 않는다(CrewApi 는 함수만 부른다).
revoke insert on public.crew_members from anon, authenticated;

-- ────────────────────────────────────────────────────────────────────
--  대표 사진 — 앱이 정사각형으로 자른 JPEG(base64). 크루마다 하나
-- ────────────────────────────────────────────────────────────────────

create table if not exists public.crew_images (
  crew_id uuid primary key references public.crews on delete cascade,
  data text not null check (length(data) between 16 and 200000),
  updated_at timestamptz not null default now()
);

comment on table public.crew_images is
  '크루 대표 사진(정사각형 JPEG · base64). 넣고 바꾸고 지우는 것은 crew_create_card · crew_update_profile 만 한다.';

alter table public.crew_images enable row level security;

drop policy if exists crew_images_select on public.crew_images;
create policy crew_images_select on public.crew_images for select
  using (not public.is_hidden('CREW', crew_id::text));

revoke insert, update, delete on public.crew_images from anon, authenticated;
grant select on public.crew_images to authenticated;

create or replace function public.crew_image_ok(p_data text)
returns boolean
language sql
immutable
as $$
  select p_data is not null
     and length(p_data) between 16 and 200000
     and (p_data like '/9j/%' or p_data like 'iVBOR%')
     and p_data ~ '^[A-Za-z0-9+/]+={0,2}$'
$$;

-- ────────────────────────────────────────────────────────────────────
--  가입 신청서
-- ────────────────────────────────────────────────────────────────────

create table if not exists public.crew_applications (
  id bigint generated always as identity primary key,
  crew_id uuid not null references public.crews on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  status text not null default 'PENDING'
    check (status in ('PENDING', 'APPROVED', 'DECLINED', 'CANCELED')),
  -- 고른 문구 — BEGINNER 러닝이 처음이에요 · AFTERWORK 퇴근 후 같이 뛰어요 · STEADY 꾸준히 달리고 싶어요 · EASY 천천히 뛰고 싶어요
  phrases text[] not null default '{}'
    check (phrases <@ array['BEGINNER', 'AFTERWORK', 'STEADY', 'EASY']::text[]),
  -- 크루장에게 한마디(선택). 문구와 따로 둔다 — 문구 버튼이 쓴 글을 덮지 않는다
  message text not null default '' check (length(message) <= 300),
  client_key uuid,
  created_at timestamptz not null default now(),
  decided_at timestamptz,
  -- 신청한 사람이 승인 · 미승인 결과를 본 때
  seen_at timestamptz
);

comment on table public.crew_applications is
  '크루 가입 신청서. 대기(PENDING)는 한 사람 · 한 크루에 하나. 넣고 바꾸는 것은 crew_apply · crew_application_* · crew_join/decide/leave 만 한다.';

create unique index if not exists crew_applications_pending
  on public.crew_applications (crew_id, user_id) where status = 'PENDING';
create unique index if not exists crew_applications_key
  on public.crew_applications (user_id, client_key) where client_key is not null;
create index if not exists crew_applications_crew
  on public.crew_applications (crew_id, status, created_at);
create index if not exists crew_applications_user
  on public.crew_applications (user_id, crew_id, id desc);

alter table public.crew_applications enable row level security;

-- 신청서는 신청한 사람과 그 크루의 지금 크루장만 본다
drop policy if exists crew_applications_select on public.crew_applications;
create policy crew_applications_select on public.crew_applications for select
  using (
    (select auth.uid()) = user_id
    or exists (
      select 1 from public.crews c
       where c.id = crew_id and c.owner_id = (select auth.uid())
    )
  );

revoke insert, update, delete on public.crew_applications from anon, authenticated;
grant select on public.crew_applications to authenticated;

-- 예전 가입 신청(대기)을 옮긴다. 이미 멤버가 된 사람 · 이미 옮긴 신청은 건너뛴다
do $$
begin
  if to_regclass('public.crew_join_requests') is not null then
    insert into public.crew_applications (crew_id, user_id, created_at)
    select r.crew_id, r.user_id, r.requested_at
      from public.crew_join_requests r
     where not exists (
             select 1 from public.crew_applications a
              where a.crew_id = r.crew_id and a.user_id = r.user_id and a.status = 'PENDING')
       and not exists (
             select 1 from public.crew_members m
              where m.crew_id = r.crew_id and m.user_id = r.user_id);
  end if;
end $$;

-- 가입 신청이 들어오면 크루장에게 — 0017 과 같은 푸시를 새 표에서
create or replace function public.push_on_crew_request()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner uuid;
  v_name text;
begin
  if new.status <> 'PENDING' then
    return new;
  end if;
  select c.owner_id, c.name into v_owner, v_name from public.crews c where c.id = new.crew_id;
  if v_owner is not null and v_owner <> new.user_id then
    perform public.push_enqueue(
      v_owner, 'CREW_REQUEST',
      jsonb_build_object('name', public.push_display_name(new.user_id), 'crew', coalesce(v_name, '')),
      'crew/' || new.crew_id::text);
  end if;
  return new;
end;
$$;

drop trigger if exists push_on_crew_request on public.crew_applications;
create trigger push_on_crew_request after insert on public.crew_applications
  for each row execute function public.push_on_crew_request();

-- ────────────────────────────────────────────────────────────────────
--  이번 주 — 한국 시간 월요일 0시부터
-- ────────────────────────────────────────────────────────────────────

create or replace function public.crew_week_start()
returns timestamptz
language sql
stable
as $$
  select date_trunc('week', now() at time zone 'Asia/Seoul') at time zone 'Asia/Seoul'
$$;

-- 이번 주 크루 러닝 — 지금 멤버가 이 크루로 달린 러닝(무효 제외). 러닝 표는 본인 것만 보이므로 definer 로 센다
create or replace function public.crew_week_stats(p_crew uuid)
returns table (km double precision, runners int)
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(sum(s.distance_meters), 0) / 1000.0, count(distinct s.user_id)::int
    from public.walk_sessions s
    join public.crew_members m on m.crew_id = s.crew_id and m.user_id = s.user_id
   where s.crew_id = p_crew
     and s.verdict <> 'VOID'
     and s.started_at >= public.crew_week_start()
$$;

-- ────────────────────────────────────────────────────────────────────
--  앱이 읽는 크루 목록 — 예전 칸은 그대로, 새 칸은 뒤에
-- ────────────────────────────────────────────────────────────────────

drop view if exists public.crew_feed;
create view public.crew_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    c.name,
    c.monogram,
    c.tagline,
    c.area,
    c.lat,
    c.lng,
    c.join_policy,
    c.created_at,
    (select count(*) from public.crew_members m where m.crew_id = c.id) as member_count,
    array(
      select coalesce(pr.display_name, '러너')
        from public.crew_members m
        left join public.profiles pr on pr.id = m.user_id
       where m.crew_id = c.id
       order by (m.role = 'OWNER') desc, m.joined_at
       limit 8
    ) as roster,
    exists (
      select 1 from public.crew_members m
       where m.crew_id = c.id and m.user_id = auth.uid()
    ) as joined,
    exists (
      select 1 from public.crew_applications a
       where a.crew_id = c.id and a.user_id = auth.uid() and a.status = 'PENDING'
    ) as requested,
    case when c.owner_id = auth.uid() then
      (select count(*) from public.crew_applications a where a.crew_id = c.id and a.status = 'PENDING')
    else 0 end as pending_count,
    c.owner_id = auth.uid() as owned,
    -- 0047 크루 명함형
    coalesce(lp.display_name, '러너') as leader_name,
    c.leader_note,
    c.image_bg,
    c.image_ver,
    exists (select 1 from public.crew_images i where i.crew_id = c.id) as has_image,
    c.meet_days,
    c.meet_time,
    c.run_distance,
    c.moods,
    c.capacity,
    c.recruiting,
    c.recruit_changed_at,
    c.weekly_goal_km,
    c.level,
    coalesce(w.km, 0) as week_km,
    coalesce(w.runners, 0) as week_runners,
    ma.id as my_application_id,
    ma.status as my_application_status,
    (ma.seen_at is not null) as my_application_seen
  from public.crews c
  left join public.profiles lp on lp.id = c.owner_id
  left join lateral public.crew_week_stats(c.id) w on true
  left join lateral (
    select a.id, a.status, a.seen_at
      from public.crew_applications a
     where a.crew_id = c.id and a.user_id = auth.uid()
     order by a.id desc
     limit 1
  ) ma on true
  where not public.is_hidden('CREW', c.id::text);

comment on view public.crew_feed is
  '크루 목록 · 상세. 보는 사람 기준(joined · requested · owned · 내 최근 신청서)과 이번 주 크루 러닝을 한 줄에.';

grant select on public.crew_feed to authenticated;

-- 예전 표는 새 표로 옮겼다. 목록이 더 이상 가리키지 않으니 지운다(0017 의 트리거도 함께 사라진다)
drop table if exists public.crew_join_requests;

-- ────────────────────────────────────────────────────────────────────
--  입력 검사 — 실패하면 'invalid:<칸>' 으로 알린다(앱이 그 칸 아래에 보여 준다)
-- ────────────────────────────────────────────────────────────────────

create or replace function public.crew_check_profile(p_name text, p_tagline text, p_leader_note text, p_image_bg int)
returns void
language plpgsql
immutable
as $$
begin
  if length(btrim(coalesce(p_name, ''))) not between 1 and 40 then
    raise exception 'invalid:name' using errcode = '22023', detail = '크루 이름은 1~40자입니다';
  end if;
  if length(btrim(coalesce(p_tagline, ''))) > 120 then
    raise exception 'invalid:tagline' using errcode = '22023', detail = '한 줄 소개는 120자까지입니다';
  end if;
  if length(btrim(coalesce(p_leader_note, ''))) > 300 then
    raise exception 'invalid:leader_note' using errcode = '22023', detail = '크루장 한마디는 300자까지입니다';
  end if;
  if p_image_bg is not null and p_image_bg not between 0 and 3 then
    raise exception 'invalid:image_bg' using errcode = '22023';
  end if;
end;
$$;

create or replace function public.crew_check_running(
  p_area text, p_lat double precision, p_lng double precision,
  p_meet_days int, p_meet_time int, p_distance text, p_moods text[]
)
returns void
language plpgsql
immutable
as $$
begin
  if length(btrim(coalesce(p_area, ''))) > 60 then
    raise exception 'invalid:area' using errcode = '22023', detail = '활동 지역은 60자까지입니다';
  end if;
  if (p_lat is null) <> (p_lng is null)
     or (p_lat is not null and (p_lat not between -90 and 90 or p_lng not between -180 and 180)) then
    raise exception 'invalid:area' using errcode = '22023', detail = '활동 지역 좌표가 올바르지 않습니다';
  end if;
  if coalesce(p_meet_days, 0) not between 0 and 127
     or (p_meet_time is not null and (p_meet_time not between 0 and 1439 or coalesce(p_meet_days, 0) = 0)) then
    raise exception 'invalid:schedule' using errcode = '22023', detail = '정기 모임은 요일을 고른 뒤 시간을 정합니다';
  end if;
  if p_distance is not null and p_distance not in ('D1_3', 'D3_5', 'D5P') then
    raise exception 'invalid:distance' using errcode = '22023';
  end if;
  if not (coalesce(p_moods, '{}') <@ array['WALK_FIRST', 'EASY', 'RECORD', 'BEGINNER', 'EXPERIENCED']::text[]) then
    raise exception 'invalid:moods' using errcode = '22023';
  end if;
end;
$$;

-- 활동 지역 좌표는 동네 크기(소수 둘째 자리, 약 1km)로만 남긴다. 크루 목록은 로그인한 누구나 본다 —
-- "현재 위치"로 정한 크루의 좌표가 크루장이 서 있던 자리(대개 집)가 되지 않게. 앱도 동네 중심점을 보낸다.
-- 거리 범위(1 · 3 · 5km)와 가까운 순을 가르기에는 이 크기로 충분하다.
create or replace function public.crew_coarse(p double precision)
returns double precision
language sql
immutable
as $$ select round(p::numeric, 2)::double precision $$;

-- 예전에 들어간 좌표도 같은 크기로(다시 올려도 바뀌는 행이 없다)
update public.crews
   set lat = public.crew_coarse(lat), lng = public.crew_coarse(lng)
 where lat is distinct from public.crew_coarse(lat) or lng is distinct from public.crew_coarse(lng);

-- 같은 분위기를 두 번 골라도 한 번. 순서는 고른 차례를 지킨다
create or replace function public.crew_distinct(p_values text[])
returns text[]
language sql
immutable
as $$
  select coalesce(array_agg(v order by first_at), '{}')
    from (
      select v, min(o) as first_at
        from unnest(coalesce(p_values, '{}')) with ordinality as t(v, o)
       group by v
    ) d
$$;

create or replace function public.crew_monogram(p_name text)
returns text
language sql
immutable
as $$
  select upper(left(regexp_replace(btrim(coalesce(p_name, '')), '\s+', '', 'g'), 2))
$$;

-- ────────────────────────────────────────────────────────────────────
--  크루 만들기 — 3단계를 마친 초안을 한 번에. 가입은 크루장 승인(패키지 제안 · 기존 승인 구조)
-- ────────────────────────────────────────────────────────────────────

create or replace function public.crew_create_card(
  p_name text,
  p_tagline text default '',
  p_leader_note text default '',
  p_image_bg int default null,
  p_image text default null,
  p_area text default '',
  p_lat double precision default null,
  p_lng double precision default null,
  p_meet_days int default 0,
  p_meet_time int default null,
  p_distance text default null,
  p_moods text[] default '{}',
  p_capacity int default null,
  p_recruiting boolean default true,
  p_goal_km int default null,
  p_client_key uuid default null
)
returns uuid
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_id uuid;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_client_key is not null then
    select c.id into v_id from public.crews c where c.owner_id = v_user and c.client_key = p_client_key;
    if found then
      return v_id;
    end if;
  end if;

  perform public.crew_check_profile(p_name, p_tagline, p_leader_note, p_image_bg);
  perform public.crew_check_running(p_area, p_lat, p_lng, p_meet_days, p_meet_time, p_distance, p_moods);
  if p_capacity is null or p_capacity not between 1 and 9999 then
    raise exception 'invalid:capacity' using errcode = '22023', detail = '모집 정원은 1명 이상입니다';
  end if;
  if p_goal_km is not null and p_goal_km not between 1 and 99999 then
    raise exception 'invalid:goal' using errcode = '22023';
  end if;
  if p_image is not null and not public.crew_image_ok(p_image) then
    raise exception 'invalid:image' using errcode = '22023', detail = '대표 사진을 읽을 수 없습니다';
  end if;
  -- 기존 상한(0010) 그대로
  if (select count(*) from public.crews c where c.owner_id = v_user) >= 10 then
    raise exception 'crew_limit' using errcode = '23514', detail = '크루는 한 사람당 10개까지 만들 수 있습니다';
  end if;

  insert into public.crews (
    owner_id, name, monogram, tagline, area, lat, lng, join_policy,
    leader_note, image_bg, image_ver, meet_days, meet_time, run_distance, moods,
    capacity, recruiting, recruit_changed_at, weekly_goal_km, client_key
  )
  values (
    v_user, btrim(p_name), public.crew_monogram(p_name), btrim(coalesce(p_tagline, '')),
    btrim(coalesce(p_area, '')), public.crew_coarse(p_lat), public.crew_coarse(p_lng), 'APPROVAL',
    btrim(coalesce(p_leader_note, '')), p_image_bg, case when p_image is null then 0 else 1 end,
    coalesce(p_meet_days, 0), p_meet_time, p_distance, public.crew_distinct(p_moods),
    p_capacity, coalesce(p_recruiting, true), now(), p_goal_km, p_client_key
  )
  on conflict (owner_id, client_key) where client_key is not null do nothing
  returning id into v_id;

  if v_id is null then
    select c.id into v_id from public.crews c where c.owner_id = v_user and c.client_key = p_client_key;
    return v_id;
  end if;

  if p_image is not null then
    insert into public.crew_images (crew_id, data) values (v_id, p_image);
  end if;
  return v_id;
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  크루장 — 소개 · 모임 · 모집 · 목표 고치기
-- ────────────────────────────────────────────────────────────────────

-- p_image_action: KEEP(그대로) · SET(p_image 로 바꿈) · REMOVE(사진을 지우고 이름 이미지로)
create or replace function public.crew_update_profile(
  p_crew uuid,
  p_name text,
  p_tagline text,
  p_leader_note text,
  p_image_bg int default null,
  p_image_action text default 'KEEP',
  p_image text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_ver int;
begin
  perform public.crew_assert_owner(p_crew);
  perform public.crew_check_profile(p_name, p_tagline, p_leader_note, p_image_bg);
  if coalesce(p_image_action, 'KEEP') not in ('KEEP', 'SET', 'REMOVE') then
    raise exception 'invalid:image' using errcode = '22023';
  end if;
  if p_image_action = 'SET' and not public.crew_image_ok(p_image) then
    raise exception 'invalid:image' using errcode = '22023', detail = '대표 사진을 읽을 수 없습니다';
  end if;

  update public.crews
     set name = btrim(p_name),
         monogram = public.crew_monogram(p_name),
         tagline = btrim(coalesce(p_tagline, '')),
         leader_note = btrim(coalesce(p_leader_note, '')),
         image_bg = p_image_bg,
         image_ver = image_ver + case when coalesce(p_image_action, 'KEEP') = 'KEEP' then 0 else 1 end
   where id = p_crew
  returning image_ver into v_ver;

  if p_image_action = 'SET' then
    insert into public.crew_images (crew_id, data, updated_at) values (p_crew, p_image, now())
    on conflict (crew_id) do update set data = excluded.data, updated_at = excluded.updated_at;
  elsif p_image_action = 'REMOVE' then
    delete from public.crew_images where crew_id = p_crew;
  end if;

  return jsonb_build_object(
    'image_ver', v_ver,
    'has_image', exists (select 1 from public.crew_images i where i.crew_id = p_crew));
end;
$$;

create or replace function public.crew_update_running(
  p_crew uuid,
  p_area text,
  p_lat double precision,
  p_lng double precision,
  p_meet_days int,
  p_meet_time int,
  p_distance text,
  p_moods text[]
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);
  perform public.crew_check_running(p_area, p_lat, p_lng, p_meet_days, p_meet_time, p_distance, p_moods);
  update public.crews
     set area = btrim(coalesce(p_area, '')),
         lat = public.crew_coarse(p_lat),
         lng = public.crew_coarse(p_lng),
         meet_days = coalesce(p_meet_days, 0),
         meet_time = p_meet_time,
         run_distance = p_distance,
         moods = public.crew_distinct(p_moods)
   where id = p_crew;
end;
$$;

-- 정원 · 모집 상태 · (고르면) 목표. 정원은 지금 인원보다 작게 정할 수 없다 — 승인과 겹쳐도 최신 인원으로 잰다
create or replace function public.crew_update_recruit(
  p_crew uuid,
  p_capacity int,
  p_recruiting boolean,
  p_goal_change boolean default false,
  p_goal_km int default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew public.crews%rowtype;
  v_count int;
begin
  perform public.crew_assert_owner(p_crew);
  select * into v_crew from public.crews c where c.id = p_crew for update;
  select count(*) into v_count from public.crew_members m where m.crew_id = p_crew;
  if p_capacity is null or p_capacity not between 1 and 9999 then
    raise exception 'invalid:capacity' using errcode = '22023', detail = '모집 정원은 1명 이상입니다';
  end if;
  if p_capacity < v_count then
    raise exception 'capacity_below_members' using errcode = '22023',
      detail = format('지금 멤버 %s명보다 적게 정할 수 없습니다', v_count);
  end if;
  if coalesce(p_goal_change, false) and p_goal_km is not null and p_goal_km not between 1 and 99999 then
    raise exception 'invalid:goal' using errcode = '22023';
  end if;

  update public.crews
     set capacity = p_capacity,
         recruiting = coalesce(p_recruiting, recruiting),
         recruit_changed_at = case when coalesce(p_recruiting, recruiting) and not recruiting then now() else recruit_changed_at end,
         weekly_goal_km = case when coalesce(p_goal_change, false) then p_goal_km else weekly_goal_km end
   where id = p_crew
  returning * into v_crew;

  return jsonb_build_object(
    'capacity', v_crew.capacity, 'recruiting', v_crew.recruiting,
    'weekly_goal_km', v_crew.weekly_goal_km, 'member_count', v_count);
end;
$$;

-- 모집 멈추기 · 다시 시작. 멤버와 크루장은 그대로다
create or replace function public.crew_set_recruiting(p_crew uuid, p_open boolean)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew public.crews%rowtype;
begin
  perform public.crew_assert_owner(p_crew);
  update public.crews
     set recruiting = coalesce(p_open, false),
         recruit_changed_at = case when coalesce(p_open, false) and not recruiting then now() else recruit_changed_at end
   where id = p_crew
  returning * into v_crew;
  return jsonb_build_object(
    'recruiting', v_crew.recruiting,
    'capacity', v_crew.capacity,
    'member_count', (select count(*) from public.crew_members m where m.crew_id = p_crew));
end;
$$;

-- 주간 목표. 비우면 목표 없음. 이미 달린 거리는 러닝 기록에서 세므로 바뀌지 않는다
create or replace function public.crew_set_goal(p_crew uuid, p_goal_km int)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_km double precision;
  v_runners int;
begin
  perform public.crew_assert_owner(p_crew);
  if p_goal_km is not null and p_goal_km not between 1 and 99999 then
    raise exception 'invalid:goal' using errcode = '22023';
  end if;
  update public.crews set weekly_goal_km = p_goal_km where id = p_crew;
  select w.km, w.runners into v_km, v_runners from public.crew_week_stats(p_crew) w;
  return jsonb_build_object('weekly_goal_km', p_goal_km, 'week_km', coalesce(v_km, 0), 'week_runners', coalesce(v_runners, 0));
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  가입 신청
-- ────────────────────────────────────────────────────────────────────

-- 결과: {"result": "PENDING" | "MEMBER", "application_id": …}
--   · 모집을 쉬는 중이면 crew_closed, 정원이 찼으면 crew_full — 앱은 입력을 남긴 채 크루를 다시 읽는다
--   · 이미 기다리는 신청이 있으면 새로 만들지 않고 그 신청을 돌려준다(두 번 신청하지 않는다)
--   · 예전 "바로 가입" 크루(OPEN)는 신청서 없이 바로 멤버가 된다
create or replace function public.crew_apply(
  p_crew uuid,
  p_phrases text[] default '{}',
  p_message text default '',
  p_client_key uuid default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_crew public.crews%rowtype;
  v_app public.crew_applications%rowtype;
  v_count int;
  v_message text := rtrim(coalesce(p_message, ''), E' \t\r\n');
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_client_key is not null then
    select * into v_app from public.crew_applications a where a.user_id = v_user and a.client_key = p_client_key;
    if found then
      return jsonb_build_object('result', case when v_app.status = 'APPROVED' then 'MEMBER' else v_app.status end,
                                'application_id', v_app.id);
    end if;
  end if;

  select * into v_crew from public.crews c
   where c.id = p_crew and not public.is_hidden('CREW', c.id::text)
     for update;
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  if exists (select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = v_user) then
    return jsonb_build_object('result', 'MEMBER');
  end if;
  select * into v_app from public.crew_applications a
   where a.crew_id = p_crew and a.user_id = v_user and a.status = 'PENDING';
  if found then
    return jsonb_build_object('result', 'PENDING', 'application_id', v_app.id, 'duplicate', true);
  end if;
  if not v_crew.recruiting then
    raise exception 'crew_closed' using errcode = '22023', detail = '지금은 모집을 쉬고 있습니다';
  end if;
  select count(*) into v_count from public.crew_members m where m.crew_id = p_crew;
  if v_crew.capacity is not null and v_count >= v_crew.capacity then
    raise exception 'crew_full' using errcode = '22023', detail = '정원이 가득 찼습니다';
  end if;
  if not (coalesce(p_phrases, '{}') <@ array['BEGINNER', 'AFTERWORK', 'STEADY', 'EASY']::text[]) then
    raise exception 'invalid:phrases' using errcode = '22023';
  end if;
  if length(v_message) > 300 then
    raise exception 'invalid:message' using errcode = '22023', detail = '한마디는 300자까지입니다';
  end if;

  if v_crew.join_policy = 'OPEN' then
    insert into public.crew_members (crew_id, user_id, role) values (p_crew, v_user, 'MEMBER')
    on conflict do nothing;
    return jsonb_build_object('result', 'MEMBER');
  end if;

  insert into public.crew_applications (crew_id, user_id, phrases, message, client_key)
  values (p_crew, v_user, public.crew_distinct(p_phrases), v_message, p_client_key)
  returning * into v_app;
  return jsonb_build_object('result', 'PENDING', 'application_id', v_app.id);
end;
$$;

-- 신청서 한 장 — 신청한 사람(대기 · 결과 보기)이나 그 크루의 크루장(검토)만
create or replace function public.crew_application(p_application bigint)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_row record;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select a.*, coalesce(pr.display_name, '러너') as name, c.owner_id
    into v_row
    from public.crew_applications a
    join public.crews c on c.id = a.crew_id
    left join public.profiles pr on pr.id = a.user_id
   where a.id = p_application;
  if not found or (v_row.user_id <> v_user and v_row.owner_id <> v_user) then
    raise exception 'application_missing' using errcode = '22023', detail = '신청서를 찾을 수 없습니다';
  end if;
  return jsonb_build_object(
    'id', v_row.id, 'crew_id', v_row.crew_id, 'user_id', v_row.user_id, 'name', v_row.name,
    'status', v_row.status, 'phrases', to_jsonb(v_row.phrases), 'message', v_row.message,
    'created_at', v_row.created_at, 'decided_at', v_row.decided_at, 'seen', v_row.seen_at is not null);
end;
$$;

-- 신청 취소 — 아직 기다리는 신청만. 그 사이 승인 · 미승인됐으면 지금 상태를 돌려준다(앱이 화면을 다시 계산)
create or replace function public.crew_application_cancel(p_application bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_status text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select a.status into v_status from public.crew_applications a
   where a.id = p_application and a.user_id = v_user
     for update;
  if not found then
    raise exception 'application_missing' using errcode = '22023', detail = '신청서를 찾을 수 없습니다';
  end if;
  if v_status <> 'PENDING' then
    return jsonb_build_object('status', v_status);
  end if;
  update public.crew_applications set status = 'CANCELED', decided_at = now() where id = p_application;
  return jsonb_build_object('status', 'CANCELED');
end;
$$;

-- 승인 · 미승인 결과를 봤다 — 같은 결과 화면을 다시 띄우지 않는다
create or replace function public.crew_application_seen(p_application bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  update public.crew_applications
     set seen_at = coalesce(seen_at, now())
   where id = p_application and user_id = auth.uid() and status in ('APPROVED', 'DECLINED');
end;
$$;

-- 크루장이 보는 기다리는 신청 — 먼저 신청한 사람이 위
create or replace function public.crew_pending_applications(p_crew uuid)
returns table (
  id bigint,
  user_id uuid,
  name text,
  phrases text[],
  message text,
  created_at timestamptz
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);
  return query
    select a.id, a.user_id, coalesce(pr.display_name, '러너'), a.phrases, a.message, a.created_at
      from public.crew_applications a
      left join public.profiles pr on pr.id = a.user_id
     where a.crew_id = p_crew and a.status = 'PENDING'
     order by a.created_at, a.id;
end;
$$;

-- 승인 · 미승인. 크루를 잠그고 정원을 다시 잰다 — 승인 한 번에 인원은 한 번만 는다
-- 결과: {"status", "member_count", "capacity", "pending_count"}
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
  perform public.crew_assert_owner(v_crew_id);
  select * into v_crew from public.crews c where c.id = v_crew_id for update;
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

-- ────────────────────────────────────────────────────────────────────
--  예전 앱이 부르는 가입 함수 — 같은 이름 · 같은 인자로 새 신청서 표를 쓴다
-- ────────────────────────────────────────────────────────────────────

-- 가입. 자유 가입이면 바로(JOINED), 승인제면 신청만(REQUESTED). 모집을 쉬거나 정원이 차면 막는다
create or replace function public.crew_join(p_crew uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_crew public.crews%rowtype;
  v_count int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into v_crew from public.crews c
   where c.id = p_crew and not public.is_hidden('CREW', c.id::text)
     for update;
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  if exists (select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = v_user) then
    return 'JOINED';
  end if;
  if exists (select 1 from public.crew_applications a
              where a.crew_id = p_crew and a.user_id = v_user and a.status = 'PENDING') then
    return 'REQUESTED';
  end if;
  if not v_crew.recruiting then
    raise exception 'crew_closed' using errcode = '22023', detail = '지금은 모집을 쉬고 있습니다';
  end if;
  select count(*) into v_count from public.crew_members m where m.crew_id = p_crew;
  if v_crew.capacity is not null and v_count >= v_crew.capacity then
    raise exception 'crew_full' using errcode = '22023', detail = '정원이 가득 찼습니다';
  end if;

  if v_crew.join_policy = 'OPEN' then
    insert into public.crew_members (crew_id, user_id, role) values (p_crew, v_user, 'MEMBER')
    on conflict do nothing;
    return 'JOINED';
  end if;
  insert into public.crew_applications (crew_id, user_id) values (p_crew, v_user);
  return 'REQUESTED';
end;
$$;

-- 탈퇴(기다리던 신청도 거둔다). 크루장은 나갈 수 없다 — 먼저 크루장을 넘기거나 해산한다
create or replace function public.crew_leave(p_crew uuid)
returns void
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
  -- 크루장 넘기기와 겹치지 않게 크루를 먼저 잠근다 — 넘겨받는 사람이 그 사이 나가면 멤버가 아닌 크루장이 남는다
  perform 1 from public.crews c where c.id = p_crew for update;
  if exists (
    select 1 from public.crew_members m
     where m.crew_id = p_crew and m.user_id = v_user and m.role = 'OWNER'
  ) then
    raise exception 'owner_cannot_leave' using errcode = '42501', detail = '크루장은 크루를 나갈 수 없습니다';
  end if;
  update public.crew_applications set status = 'CANCELED', decided_at = now()
   where crew_id = p_crew and user_id = v_user and status = 'PENDING';
  delete from public.crew_members where crew_id = p_crew and user_id = v_user;
end;
$$;

-- 크루장이 보는 신청 목록(예전 모양)
create or replace function public.crew_requests(p_crew uuid)
returns table (user_id uuid, name text, requested_at timestamptz)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);
  return query
    select a.user_id, coalesce(pr.display_name, '러너'), a.created_at
      from public.crew_applications a
      left join public.profiles pr on pr.id = a.user_id
     where a.crew_id = p_crew and a.status = 'PENDING'
     order by a.created_at, a.id;
end;
$$;

-- 승인 · 거절(예전 모양) — 그 사람의 기다리는 신청서를 찾아 같은 규칙으로 처리한다
create or replace function public.crew_decide(p_crew uuid, p_user uuid, p_approve boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_id bigint;
begin
  perform public.crew_assert_owner(p_crew);
  select a.id into v_id from public.crew_applications a
   where a.crew_id = p_crew and a.user_id = p_user and a.status = 'PENDING';
  if not found then
    raise exception 'application_missing' using errcode = '22023', detail = '가입 신청을 찾을 수 없습니다';
  end if;
  perform public.crew_application_decide(v_id, p_approve);
end;
$$;

-- 가입 방식 바꾸기(예전 모양). 자유 가입으로 열면 기다리던 신청을 정원 안에서 먼저 신청한 순으로 받아 준다
create or replace function public.crew_set_join_policy(p_crew uuid, p_policy text)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew public.crews%rowtype;
  v_count int;
  v_admitted int := 0;
  r record;
begin
  perform public.crew_assert_owner(p_crew);
  if p_policy not in ('OPEN', 'APPROVAL') then
    raise exception '가입 방식이 올바르지 않습니다' using errcode = '22023';
  end if;
  select * into v_crew from public.crews c where c.id = p_crew for update;
  update public.crews set join_policy = p_policy where id = p_crew;

  if p_policy = 'OPEN' then
    select count(*) into v_count from public.crew_members m where m.crew_id = p_crew;
    for r in
      select a.id, a.user_id from public.crew_applications a
       where a.crew_id = p_crew and a.status = 'PENDING'
       order by a.created_at, a.id
    loop
      exit when v_crew.capacity is not null and v_count >= v_crew.capacity;
      insert into public.crew_members (crew_id, user_id, role) values (p_crew, r.user_id, 'MEMBER')
      on conflict do nothing;
      update public.crew_applications set status = 'APPROVED', decided_at = now() where id = r.id;
      v_count := v_count + 1;
      v_admitted := v_admitted + 1;
    end loop;
  end if;
  return v_admitted;
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  멤버 · 사람
-- ────────────────────────────────────────────────────────────────────

-- 크루 멤버 — 크루장 먼저, 그다음 먼저 들어온 순. 이번 주 크루 러닝 거리를 함께(목표 참여 멤버)
create or replace function public.crew_roster(p_crew uuid)
returns table (
  user_id uuid,
  name text,
  role text,
  joined_at timestamptz,
  week_km double precision,
  week_runs int
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not exists (select 1 from public.crews c where c.id = p_crew and not public.is_hidden('CREW', c.id::text)) then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  return query
    select m.user_id, coalesce(pr.display_name, '러너'), m.role, m.joined_at,
           coalesce(w.km, 0), coalesce(w.runs, 0)
      from public.crew_members m
      left join public.profiles pr on pr.id = m.user_id
      left join lateral (
        select sum(s.distance_meters) / 1000.0 as km, count(*)::int as runs
          from public.walk_sessions s
         where s.crew_id = p_crew and s.user_id = m.user_id
           and s.verdict <> 'VOID' and s.started_at >= public.crew_week_start()
      ) w on true
     where m.crew_id = p_crew
     order by (m.role = 'OWNER') desc, m.joined_at, m.user_id;
end;
$$;

-- 이 크루에서 본 한 사람 — 공개 정보만. 신청자라는 것은 크루장(과 본인)에게만 보인다
-- 결과: {"user_id", "name", "role": OWNER|MEMBER|APPLICANT|NONE, "joined_at", "week_km", "application"?}
create or replace function public.crew_person(p_crew uuid, p_user uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_owner uuid;
  v_name text;
  v_member public.crew_members%rowtype;
  v_app public.crew_applications%rowtype;
  v_km double precision;
begin
  if v_me is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select c.owner_id into v_owner from public.crews c where c.id = p_crew and not public.is_hidden('CREW', c.id::text);
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  select coalesce(pr.display_name, '러너') into v_name from public.profiles pr where pr.id = p_user;
  select * into v_member from public.crew_members m where m.crew_id = p_crew and m.user_id = p_user;
  if found then
    select coalesce(sum(s.distance_meters), 0) / 1000.0 into v_km
      from public.walk_sessions s
     where s.crew_id = p_crew and s.user_id = p_user
       and s.verdict <> 'VOID' and s.started_at >= public.crew_week_start();
    return jsonb_build_object('user_id', p_user, 'name', coalesce(v_name, '러너'), 'role', v_member.role,
                              'joined_at', v_member.joined_at, 'week_km', coalesce(v_km, 0));
  end if;
  if v_me = v_owner or v_me = p_user then
    select * into v_app from public.crew_applications a
     where a.crew_id = p_crew and a.user_id = p_user and a.status = 'PENDING';
    if found then
      return jsonb_build_object('user_id', p_user, 'name', coalesce(v_name, '러너'), 'role', 'APPLICANT',
        'application', jsonb_build_object('id', v_app.id, 'phrases', to_jsonb(v_app.phrases),
                                          'message', v_app.message, 'created_at', v_app.created_at));
    end if;
  end if;
  return jsonb_build_object('user_id', p_user, 'name', coalesce(v_name, '러너'), 'role', 'NONE');
end;
$$;

-- 레벨 안내 — 목록과 따로 읽는다(레벨만 못 읽어도 다른 정보는 보인다). 레벨은 저장된 값 그대로(없으면 null)
create or replace function public.crew_level(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_level int;
  v_km double precision;
  v_runners int;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select c.level into v_level from public.crews c where c.id = p_crew and not public.is_hidden('CREW', c.id::text);
  if not found then
    raise exception 'crew_missing' using errcode = '22023', detail = '크루를 찾을 수 없습니다';
  end if;
  select w.km, w.runners into v_km, v_runners from public.crew_week_stats(p_crew) w;
  return jsonb_build_object(
    'level', v_level,
    'week_km', coalesce(v_km, 0),
    'week_runners', coalesce(v_runners, 0),
    'member_count', (select count(*) from public.crew_members m where m.crew_id = p_crew));
end;
$$;

-- 크루를 잠근 뒤 크루장인지 본다(crew_assert_owner 는 잠그기 전에 본다). 같은 크루를 두 번 동시에 넘기면
-- 뒤에 온 쪽은 앞의 것이 끝날 때까지 기다렸다가, 이미 크루장이 아니어서 멈춘다 — 크루장이 둘 남지 않게.
create or replace function public.crew_lock_owner(p_crew uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_owner uuid;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select c.owner_id into v_owner from public.crews c where c.id = p_crew for update;
  if v_owner is distinct from auth.uid() then
    raise exception '크루장만 할 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 멤버 내보내기 — 크루장만, 크루장 자신은 대상이 아니다
create or replace function public.crew_member_remove(p_crew uuid, p_user uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_role text;
begin
  perform public.crew_lock_owner(p_crew);
  select m.role into v_role from public.crew_members m where m.crew_id = p_crew and m.user_id = p_user for update;
  if not found then
    raise exception 'target_not_member' using errcode = '22023', detail = '이 크루의 멤버가 아닙니다';
  end if;
  if v_role = 'OWNER' then
    raise exception 'target_is_owner' using errcode = '22023', detail = '크루장은 내보낼 수 없습니다';
  end if;
  delete from public.crew_members where crew_id = p_crew and user_id = p_user;
  return jsonb_build_object('member_count', (select count(*) from public.crew_members m where m.crew_id = p_crew));
end;
$$;

-- 크루장 넘기기 — 지금 멤버에게만. 넘긴 사람은 일반 멤버로 남는다(한 번에)
create or replace function public.crew_transfer_owner(p_crew uuid, p_user uuid)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  perform public.crew_lock_owner(p_crew);
  if p_user is null or p_user = v_me then
    raise exception 'target_not_member' using errcode = '22023', detail = '이 크루의 멤버에게만 넘길 수 있습니다';
  end if;
  -- 넘겨받을 사람의 가입 행도 잠근다 — 그 사이 나가거나(표에서 바로 지우기 포함) 내보내지지 않게
  perform 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = p_user for update;
  if not found then
    raise exception 'target_not_member' using errcode = '22023', detail = '이 크루의 멤버에게만 넘길 수 있습니다';
  end if;
  update public.crews set owner_id = p_user where id = p_crew;
  -- 크루장은 한 사람 — 넘겨받는 사람만 OWNER, 남아 있던 OWNER 행은 모두 MEMBER
  update public.crew_members
     set role = case when user_id = p_user then 'OWNER' else 'MEMBER' end
   where crew_id = p_crew and (user_id = p_user or role = 'OWNER');
  return jsonb_build_object(
    'owner_id', p_user,
    'leader_name', (select coalesce(pr.display_name, '러너') from public.profiles pr where pr.id = p_user));
end;
$$;

-- 해산 — 크루장이 고른 크루를 지운다(멤버 · 신청 · 크루 글이 함께 사라진다)
create or replace function public.crew_dissolve(p_crew uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);
  delete from public.crews where id = p_crew;
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  권한 — 앱(authenticated)은 함수로만
-- ────────────────────────────────────────────────────────────────────

revoke execute on function public.crew_image_ok(text) from public, anon;
revoke execute on function public.crew_week_start() from public, anon;
revoke execute on function public.crew_week_stats(uuid) from public, anon;
revoke execute on function public.crew_check_profile(text, text, text, int) from public, anon, authenticated;
revoke execute on function public.crew_check_running(text, double precision, double precision, int, int, text, text[])
  from public, anon, authenticated;
revoke execute on function public.crew_distinct(text[]) from public, anon, authenticated;
revoke execute on function public.crew_monogram(text) from public, anon, authenticated;
revoke execute on function public.crew_coarse(double precision) from public, anon, authenticated;
revoke execute on function public.crew_lock_owner(uuid) from public, anon, authenticated;
revoke execute on function public.push_on_crew_request() from public, anon, authenticated;

revoke execute on function public.crew_create_card(text, text, text, int, text, text, double precision, double precision,
  int, int, text, text[], int, boolean, int, uuid) from public, anon;
revoke execute on function public.crew_update_profile(uuid, text, text, text, int, text, text) from public, anon;
revoke execute on function public.crew_update_running(uuid, text, double precision, double precision, int, int, text, text[])
  from public, anon;
revoke execute on function public.crew_update_recruit(uuid, int, boolean, boolean, int) from public, anon;
revoke execute on function public.crew_set_recruiting(uuid, boolean) from public, anon;
revoke execute on function public.crew_set_goal(uuid, int) from public, anon;
revoke execute on function public.crew_apply(uuid, text[], text, uuid) from public, anon;
revoke execute on function public.crew_application(bigint) from public, anon;
revoke execute on function public.crew_application_cancel(bigint) from public, anon;
revoke execute on function public.crew_application_seen(bigint) from public, anon;
revoke execute on function public.crew_pending_applications(uuid) from public, anon;
revoke execute on function public.crew_application_decide(bigint, boolean) from public, anon;
revoke execute on function public.crew_join(uuid) from public, anon;
revoke execute on function public.crew_leave(uuid) from public, anon;
revoke execute on function public.crew_requests(uuid) from public, anon;
revoke execute on function public.crew_decide(uuid, uuid, boolean) from public, anon;
revoke execute on function public.crew_set_join_policy(uuid, text) from public, anon;
revoke execute on function public.crew_roster(uuid) from public, anon;
revoke execute on function public.crew_person(uuid, uuid) from public, anon;
revoke execute on function public.crew_level(uuid) from public, anon;
revoke execute on function public.crew_member_remove(uuid, uuid) from public, anon;
revoke execute on function public.crew_transfer_owner(uuid, uuid) from public, anon;
revoke execute on function public.crew_dissolve(uuid) from public, anon;

grant execute on function public.crew_week_start() to authenticated;
grant execute on function public.crew_week_stats(uuid) to authenticated;
grant execute on function public.crew_image_ok(text) to authenticated;
grant execute on function public.crew_create_card(text, text, text, int, text, text, double precision, double precision,
  int, int, text, text[], int, boolean, int, uuid) to authenticated;
grant execute on function public.crew_update_profile(uuid, text, text, text, int, text, text) to authenticated;
grant execute on function public.crew_update_running(uuid, text, double precision, double precision, int, int, text, text[])
  to authenticated;
grant execute on function public.crew_update_recruit(uuid, int, boolean, boolean, int) to authenticated;
grant execute on function public.crew_set_recruiting(uuid, boolean) to authenticated;
grant execute on function public.crew_set_goal(uuid, int) to authenticated;
grant execute on function public.crew_apply(uuid, text[], text, uuid) to authenticated;
grant execute on function public.crew_application(bigint) to authenticated;
grant execute on function public.crew_application_cancel(bigint) to authenticated;
grant execute on function public.crew_application_seen(bigint) to authenticated;
grant execute on function public.crew_pending_applications(uuid) to authenticated;
grant execute on function public.crew_application_decide(bigint, boolean) to authenticated;
grant execute on function public.crew_join(uuid) to authenticated;
grant execute on function public.crew_leave(uuid) to authenticated;
grant execute on function public.crew_requests(uuid) to authenticated;
grant execute on function public.crew_decide(uuid, uuid, boolean) to authenticated;
grant execute on function public.crew_set_join_policy(uuid, text) to authenticated;
grant execute on function public.crew_roster(uuid) to authenticated;
grant execute on function public.crew_person(uuid, uuid) to authenticated;
grant execute on function public.crew_level(uuid) to authenticated;
grant execute on function public.crew_member_remove(uuid, uuid) to authenticated;
grant execute on function public.crew_transfer_owner(uuid, uuid) to authenticated;
grant execute on function public.crew_dissolve(uuid) to authenticated;
