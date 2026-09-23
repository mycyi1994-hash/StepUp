-- 크루 가입 방식, 가입 신청, 그리고 신고가 쌓인 글의 자동 숨김.
--
-- 지금까지 크루는 누구나 누르면 들어가는 방이었다. 동네 러닝 크루는
-- 그렇게 운영되지 않는 곳이 많다 — 모르는 사람이 새벽 6시 집합 장소에
-- 나타나는 것을 크루장이 먼저 알고 싶어 한다. 그래서 크루장이 방식을 고른다.
--
--   OPEN     누르면 바로 가입
--   APPROVAL 가입 신청 → 크루장이 승인하면 가입
--
-- 방식은 나중에 바꿀 수 있다. 승인제에서 자유 가입으로 바꾸면 기다리던
-- 신청은 그 자리에서 모두 받아 준다 — 문을 열었는데 줄 선 사람만 밖에
-- 세워 두는 것은 이상하다.

-- ════════════════════════════════════════════════════════════════════
--  가입 방식
-- ════════════════════════════════════════════════════════════════════

alter table public.crews
  add column if not exists join_policy text not null default 'OPEN';

do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'crews_join_policy_check'
  ) then
    alter table public.crews
      add constraint crews_join_policy_check check (join_policy in ('OPEN', 'APPROVAL'));
  end if;
end $$;

comment on column public.crews.join_policy is
  'OPEN = 누르면 바로 가입, APPROVAL = 크루장 승인 후 가입';

-- ════════════════════════════════════════════════════════════════════
--  가입 신청
-- ════════════════════════════════════════════════════════════════════

create table if not exists public.crew_join_requests (
  crew_id uuid not null references public.crews on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  requested_at timestamptz not null default now(),
  primary key (crew_id, user_id)
);

comment on table public.crew_join_requests is
  '승인제 크루의 가입 신청. 넣고 빼는 것은 crew_join / crew_decide / crew_leave 만 한다.';

alter table public.crew_join_requests enable row level security;

-- 신청은 신청한 사람과 그 크루의 크루장만 본다. 누가 어느 크루에
-- 들어가려다 기다리는지는 남이 알 일이 아니다.
drop policy if exists crew_join_requests_select on public.crew_join_requests;
create policy crew_join_requests_select on public.crew_join_requests for select
  using (
    (select auth.uid()) = user_id
    or exists (
      select 1 from public.crews c
       where c.id = crew_id and c.owner_id = (select auth.uid())
    )
  );

revoke insert, update, delete on public.crew_join_requests from anon, authenticated;

-- 직접 가입(INSERT)은 자유 가입 크루에만 열어 둔다. 승인제 크루에 표를
-- 직접 두드려 들어오면 승인이 아무 의미가 없다.
drop policy if exists crew_members_join_self on public.crew_members;
create policy crew_members_join_self on public.crew_members for insert
  with check (
    (select auth.uid()) = user_id
    and role = 'MEMBER'
    and exists (
      select 1 from public.crews c
       where c.id = crew_id and c.join_policy = 'OPEN'
    )
  );

-- ════════════════════════════════════════════════════════════════════
--  신고가 쌓이면 숨긴다
--
--  신고가 5건 모이면 그 글·댓글·크루·코스는 모두의 화면에서 사라진다.
--  운영자가 대시보드에서 신고를 기각(DISMISSED)하면 다시 보인다 — 여러
--  계정으로 멀쩡한 글을 내리는 일이 생겨도 되돌릴 길이 있어야 한다.
-- ════════════════════════════════════════════════════════════════════

create index if not exists content_reports_target
  on public.content_reports (target_type, target_id);

-- 신고함은 "내가 넣은 것"만 보이게 막혀 있다. 그 정책 아래서 세면 모두가
-- 자기 신고 1건만 보게 되므로, 세는 일만 이 함수가 대신 한다.
create or replace function public.is_hidden(p_type text, p_id text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select (
    select count(*) from public.content_reports r
     where r.target_type = p_type
       and r.target_id = p_id
       and r.status <> 'DISMISSED'
  ) >= 5
$$;

comment on function public.is_hidden is
  '신고가 5건 이상 쌓였는지. 기각된 신고는 세지 않는다.';

-- ════════════════════════════════════════════════════════════════════
--  앱이 읽는 모양 — 숨김을 반영해 다시 만든다
-- ════════════════════════════════════════════════════════════════════

drop view if exists public.post_feed;
create view public.post_feed
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
    p.author_id = auth.uid() as mine
  from public.posts p
  left join public.profiles pr on pr.id = p.author_id
  where not public.is_blocked(p.author_id)
    and not public.is_hidden('POST', p.id::text);

comment on view public.post_feed is
  '게시글 목록. 좋아요·댓글·참가 수와 "내가 눌렀는지"까지 한 줄에 담는다.';

drop view if exists public.comment_feed;
create view public.comment_feed
with (security_invoker = true) as
  select
    c.id,
    c.post_id,
    coalesce(c.parent_id, 0) as parent_id,
    c.author_id,
    coalesce(pr.display_name, '러너') as author,
    c.body,
    c.created_at,
    c.author_id = auth.uid() as mine
  from public.comments c
  left join public.profiles pr on pr.id = c.author_id
  where not public.is_blocked(c.author_id)
    and not public.is_hidden('COMMENT', c.id::text);

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
    -- 카드에 얼굴 몇 개와 파티런 초대 후보를 그리는 데 쓴다. 크루장이 먼저,
    -- 그다음은 먼저 들어온 순.
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
      select 1 from public.crew_join_requests r
       where r.crew_id = c.id and r.user_id = auth.uid()
    ) as requested,
    -- 기다리는 신청 수는 크루장에게만 의미가 있다. 다른 사람에게는 0이다.
    case when c.owner_id = auth.uid() then
      (select count(*) from public.crew_join_requests r where r.crew_id = c.id)
    else 0 end as pending_count,
    c.owner_id = auth.uid() as owned
  from public.crews c
  where not public.is_hidden('CREW', c.id::text);

drop view if exists public.course_feed;
create view public.course_feed
with (security_invoker = true) as
  select
    c.id,
    c.owner_id,
    coalesce(pr.display_name, '러너') as author,
    c.name,
    c.area,
    c.distance_km,
    c.elevation_m,
    c.track,
    c.shared,
    c.run_count,
    c.created_at,
    (select count(*) from public.course_likes l where l.course_id = c.id) as likes,
    exists (
      select 1 from public.course_likes l
       where l.course_id = c.id and l.user_id = auth.uid()
    ) as liked,
    c.owner_id = auth.uid() as mine
  from public.courses c
  left join public.profiles pr on pr.id = c.owner_id
  where not public.is_blocked(c.owner_id)
    and not public.is_hidden('COURSE', c.id::text);

grant select on public.post_feed, public.comment_feed, public.crew_feed, public.course_feed
  to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  크루 함수 — 앱은 이것만 부른다
--
--  표를 직접 두드리게 두면 "승인제인지 확인하고 → 신청을 넣는다"가 두 번의
--  요청이 되고, 그 사이에 크루장이 방식을 바꾸면 어긋난다. 한 번에 한다.
-- ════════════════════════════════════════════════════════════════════

-- 크루장 확인. 여러 함수가 같은 말을 하므로 한곳에 둔다.
create or replace function public.crew_assert_owner(p_crew uuid)
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
    select 1 from public.crews c where c.id = p_crew and c.owner_id = auth.uid()
  ) then
    raise exception '크루장만 할 수 있습니다' using errcode = '42501';
  end if;
end;
$$;

-- 크루 만들기. 만든 사람은 트리거로 주인 멤버가 된다.
create or replace function public.crew_create(
  p_name text,
  p_monogram text,
  p_tagline text,
  p_area text,
  p_join_policy text
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
  if p_join_policy not in ('OPEN', 'APPROVAL') then
    raise exception '가입 방식이 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 한 사람이 크루를 무한히 찍어 내면 목록이 빈 방으로 덮인다.
  if (select count(*) from public.crews c where c.owner_id = v_user) >= 10 then
    raise exception '크루는 한 사람당 10개까지 만들 수 있습니다' using errcode = '23514';
  end if;

  insert into public.crews (owner_id, name, monogram, tagline, area, join_policy)
  values (
    v_user,
    btrim(p_name),
    left(coalesce(p_monogram, ''), 4),
    btrim(coalesce(p_tagline, '')),
    btrim(coalesce(p_area, '')),
    p_join_policy
  )
  returning id into v_id;

  return v_id;
end;
$$;

-- 가입. 자유 가입이면 바로 들어가고(JOINED), 승인제면 신청만 남긴다(REQUESTED).
create or replace function public.crew_join(p_crew uuid)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_user uuid := auth.uid();
  v_policy text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- 크루를 잠근다. 크루장이 방식을 바꾸는 순간과 겹치면, 잠그지 않는 한
  -- 자유 가입으로 본 사람이 승인제 크루에 그냥 들어간다.
  select c.join_policy into v_policy
    from public.crews c
   where c.id = p_crew and not public.is_hidden('CREW', c.id::text)
     for share;

  if not found then
    raise exception '크루를 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  if exists (
    select 1 from public.crew_members m where m.crew_id = p_crew and m.user_id = v_user
  ) then
    return 'JOINED';
  end if;

  if v_policy = 'OPEN' then
    insert into public.crew_members (crew_id, user_id, role)
    values (p_crew, v_user, 'MEMBER')
    on conflict do nothing;
    delete from public.crew_join_requests where crew_id = p_crew and user_id = v_user;
    return 'JOINED';
  end if;

  insert into public.crew_join_requests (crew_id, user_id)
  values (p_crew, v_user)
  on conflict do nothing;
  return 'REQUESTED';
end;
$$;

-- 탈퇴. 기다리던 신청이 있으면 그것도 거둔다. 크루장은 나갈 수 없다 —
-- 나가면 주인 없는 크루가 남는다.
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
  if exists (
    select 1 from public.crew_members m
     where m.crew_id = p_crew and m.user_id = v_user and m.role = 'OWNER'
  ) then
    raise exception '크루장은 크루를 나갈 수 없습니다' using errcode = '42501';
  end if;

  delete from public.crew_join_requests where crew_id = p_crew and user_id = v_user;
  delete from public.crew_members where crew_id = p_crew and user_id = v_user;
end;
$$;

-- 가입 방식 바꾸기. 자유 가입으로 열면 기다리던 신청을 모두 받아 준다.
-- 받아 준 인원을 돌려준다.
create or replace function public.crew_set_join_policy(p_crew uuid, p_policy text)
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  v_admitted int := 0;
begin
  perform public.crew_assert_owner(p_crew);
  if p_policy not in ('OPEN', 'APPROVAL') then
    raise exception '가입 방식이 올바르지 않습니다' using errcode = '22023';
  end if;

  update public.crews set join_policy = p_policy where id = p_crew;

  if p_policy = 'OPEN' then
    with admitted as (
      delete from public.crew_join_requests r where r.crew_id = p_crew
      returning r.user_id
    )
    insert into public.crew_members (crew_id, user_id, role)
    select p_crew, a.user_id, 'MEMBER' from admitted a
    on conflict do nothing;
    get diagnostics v_admitted = row_count;
  end if;

  return v_admitted;
end;
$$;

-- 크루장이 보는 신청 목록. 먼저 신청한 사람이 위.
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
    select r.user_id, coalesce(pr.display_name, '러너'), r.requested_at
      from public.crew_join_requests r
      left join public.profiles pr on pr.id = r.user_id
     where r.crew_id = p_crew
     order by r.requested_at;
end;
$$;

-- 승인 또는 거절. 어느 쪽이든 신청은 사라진다.
create or replace function public.crew_decide(p_crew uuid, p_user uuid, p_approve boolean)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_assert_owner(p_crew);

  delete from public.crew_join_requests where crew_id = p_crew and user_id = p_user;
  if not found then
    raise exception '가입 신청을 찾을 수 없습니다' using errcode = 'P0002';
  end if;

  if p_approve then
    insert into public.crew_members (crew_id, user_id, role)
    values (p_crew, p_user, 'MEMBER')
    on conflict do nothing;
  end if;
end;
$$;

revoke execute on function public.crew_assert_owner(uuid) from public, anon;
grant execute on function public.crew_create(text, text, text, text, text) to authenticated;
grant execute on function public.crew_join(uuid) to authenticated;
grant execute on function public.crew_leave(uuid) to authenticated;
grant execute on function public.crew_set_join_policy(uuid, text) to authenticated;
grant execute on function public.crew_requests(uuid) to authenticated;
grant execute on function public.crew_decide(uuid, uuid, boolean) to authenticated;
