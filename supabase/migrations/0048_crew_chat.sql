-- ════════════════════════════════════════════════════════════════════
--  0048 — 크루 채팅(2026-09-29 크루 채팅 패키지)
--
--  크루마다 크루원 전용 방 하나(방 id = 크루 id). 들어가는 사람은 지금 crew_members 에 있는 사람뿐이다 —
--  만든 사람은 처음부터, 신청자는 승인된 뒤. 방을 여는 · 초대하는 절차는 없다. 모든 읽기 · 쓰기는 아래
--  함수로만 하고, 함수마다 지금의 가입 · 크루장 여부를 다시 본다(화면에서 버튼을 숨기는 것만으로 막지 않는다).
--
--  이 파일이 정하는 것
--    · 메시지(글 · 사진 · 알림 줄) · 답장 · 내 메시지 삭제(시간 제한 없음) · 크루장 숨김(선택한 메시지만, 소속은 그대로)
--    · 공지(상단 고정은 하나 — 새로 고정하면 이전 고정은 풀린다) · 나만의 채팅 알림 · 읽은 위치 · 대화 검색 · 메시지 신고
--    · 앱이 따라오는 변경 번호(rev) — 방마다 한 줄씩 잠가 올리므로 커밋 순서대로 오른다(폴링이 건너뛰지 않는다)
--    · 같은 요청 키(client_id)로 다시 보내면 처음 저장한 메시지 하나
--
--  정하지 않은 것 — 기존 정책을 따른다(패키지가 새 수치 · 정책을 정하지 않았다)
--    · 가입 전 대화: 크루 게시판처럼 멤버가 되면 방의 대화를 본다. 다시 가입해도 같다.
--    · 보관 기간: 따로 지우지 않는다(게시판과 같다). 계정을 지우면 그 사람의 메시지 · 공지도 지워진다(게시판과 같다).
--    · 사진: 크루 대표 사진과 같은 기술 한도(JPEG/PNG base64 200,000자). 한 메시지에 한 장.
--    · 숨긴 메시지 되돌리기: 만들지 않는다. 내용은 서버에 남기고 아무에게도 보내지 않는다.
--    · 신고: content_reports 에 쌓고 사람이 본다. 채팅은 신고 수로 자동 숨기지 않는다.
--    · 입력 중 표시: 실시간 이벤트가 없어 만들지 않는다(앱은 방을 보는 동안 짧게 다시 읽는다).
--
--  잠금 순서: 채팅 표들은 crews 가 아니라 crew_chat_rooms 를 가리킨다. 메시지를 쓰는 쪽이 crews 에 잠금을
--  걸지 않게 하려는 것이다 — 크루장 일(crew_lock_owner · crew_leave)은 crews → 방 순서로 잠근다.
-- ════════════════════════════════════════════════════════════════════

-- ────────────────────────────────────────────────────────────────────
--  표
-- ────────────────────────────────────────────────────────────────────

-- 방 — 크루와 함께 생기고 함께 사라진다. last_rev 는 이 방의 마지막 변경 번호
create table if not exists public.crew_chat_rooms (
  crew_id uuid primary key references public.crews on delete cascade,
  last_rev bigint not null default 0
);

-- 크루가 생기면 방도 생긴다(예전 크루는 아래에서 한 번 채운다)
create or replace function public.crew_chat_room_for_crew()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  insert into public.crew_chat_rooms (crew_id) values (new.id) on conflict do nothing;
  return new;
end;
$$;

drop trigger if exists crew_chat_room_for_crew on public.crews;
create trigger crew_chat_room_for_crew
  after insert on public.crews
  for each row execute function public.crew_chat_room_for_crew();

insert into public.crew_chat_rooms (crew_id) select c.id from public.crews c on conflict do nothing;

create table if not exists public.crew_chat_messages (
  id bigint generated always as identity primary key,
  crew_id uuid not null references public.crew_chat_rooms on delete cascade,
  -- 방 안의 순서(보낸 때의 변경 번호). rev 는 마지막으로 바뀐 때(삭제 · 숨김 · 인용 원문 변경)
  seq bigint not null,
  rev bigint not null,
  author_id uuid references auth.users on delete cascade,
  kind text not null check (kind in ('TEXT', 'IMAGE', 'SYSTEM')),
  body text not null default '' check (length(body) <= 2000),
  reply_to bigint references public.crew_chat_messages on delete set null,
  -- 알림 줄: NOTICE_CREATED "○○ 님이 새 공지를 등록했어요." · MEMBER_LEFT "○○ 님이 크루에서 나갔어요."
  event text check (event in ('NOTICE_CREATED', 'MEMBER_LEFT')),
  event_name text not null default '',
  client_id uuid,
  deleted_at timestamptz,
  hidden_at timestamptz,
  hidden_by uuid references auth.users on delete set null,
  created_at timestamptz not null default now(),
  constraint crew_chat_messages_shape check (
    (kind = 'SYSTEM' and author_id is null and event is not null)
    or (kind <> 'SYSTEM' and author_id is not null and event is null)),
  constraint crew_chat_messages_order unique (crew_id, seq)
);

create unique index if not exists crew_chat_messages_client
  on public.crew_chat_messages (author_id, client_id) where client_id is not null;
create index if not exists crew_chat_messages_rev on public.crew_chat_messages (crew_id, rev);
create index if not exists crew_chat_messages_reply on public.crew_chat_messages (reply_to) where reply_to is not null;

comment on table public.crew_chat_messages is
  '크루 채팅 메시지. 앱은 crew_chat_* 함수로만 읽고 쓴다. 삭제는 내용을 지운 자리, 숨김은 내용을 남기되 보내지 않는다.';

-- 사진 — 메시지마다 한 장(JPEG/PNG base64). 삭제하면 함께 지운다
create table if not exists public.crew_chat_images (
  message_id bigint primary key references public.crew_chat_messages on delete cascade,
  data text not null check (length(data) between 16 and 200000)
);

-- 공지 — 크루장만 쓴다. 상단 고정은 방마다 하나
create table if not exists public.crew_chat_notices (
  id bigint generated always as identity primary key,
  crew_id uuid not null references public.crew_chat_rooms on delete cascade,
  author_id uuid not null references auth.users on delete cascade,
  title text not null check (length(title) between 1 and 100),
  body text not null default '' check (length(body) <= 2000),
  pinned boolean not null default false,
  client_key uuid,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index if not exists crew_chat_notices_pinned on public.crew_chat_notices (crew_id) where pinned;
create unique index if not exists crew_chat_notices_client on public.crew_chat_notices (crew_id, client_key) where client_key is not null;
create index if not exists crew_chat_notices_crew on public.crew_chat_notices (crew_id, created_at desc);

-- 읽은 위치 — 앱이 켜져 있고 그 대화가 실제로 보일 때만 앞으로 옮긴다
create table if not exists public.crew_chat_reads (
  crew_id uuid not null references public.crew_chat_rooms on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  last_read_seq bigint not null default 0,
  updated_at timestamptz not null default now(),
  primary key (crew_id, user_id)
);

-- 나만의 채팅 알림 — 끄면 나에게만 오지 않는다(다른 크루원은 그대로)
create table if not exists public.crew_chat_settings (
  crew_id uuid not null references public.crew_chat_rooms on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  notify boolean not null default true,
  primary key (crew_id, user_id)
);

alter table public.crew_chat_rooms enable row level security;
alter table public.crew_chat_messages enable row level security;
alter table public.crew_chat_images enable row level security;
alter table public.crew_chat_notices enable row level security;
alter table public.crew_chat_reads enable row level security;
alter table public.crew_chat_settings enable row level security;
revoke all on public.crew_chat_rooms, public.crew_chat_messages, public.crew_chat_images,
  public.crew_chat_notices, public.crew_chat_reads, public.crew_chat_settings from anon, authenticated;

-- 신고 대상에 채팅 메시지('CHAT')를 더한다
alter table public.content_reports drop constraint if exists content_reports_target_type_check;
alter table public.content_reports add constraint content_reports_target_type_check
  check (target_type in ('POST', 'COMMENT', 'CREW', 'COURSE', 'USER', 'CHAT'));

-- ────────────────────────────────────────────────────────────────────
--  안쪽 도움 함수
-- ────────────────────────────────────────────────────────────────────

-- 지금 이 크루의 멤버인가 — 아니면 chat_not_member(앱은 32 채팅 참여 종료). p_lock 이면 내 가입 행을 잠가,
-- 보내는 동안 내보내기 · 탈퇴가 끼어들지 않게 한다. 역할(OWNER · MEMBER)을 돌려준다.
create or replace function public.crew_chat_member(p_crew uuid, p_lock boolean default false)
returns text
language plpgsql
security definer
set search_path = public
as $$
declare
  v_role text;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_lock then
    select m.role into v_role from public.crew_members m
     where m.crew_id = p_crew and m.user_id = auth.uid() for share;
  else
    select m.role into v_role from public.crew_members m
     where m.crew_id = p_crew and m.user_id = auth.uid();
  end if;
  if v_role is null then
    raise exception 'chat_not_member' using errcode = '42501', detail = '이 채팅방에 참여할 수 없습니다';
  end if;
  return v_role;
end;
$$;

-- 이 방의 다음 변경 번호 — 방 줄을 잠그고 올린다(커밋할 때까지 다음 쓰기는 기다린다)
create or replace function public.crew_chat_next_rev(p_crew uuid)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_rev bigint;
begin
  update public.crew_chat_rooms set last_rev = last_rev + 1 where crew_id = p_crew returning last_rev into v_rev;
  if v_rev is null then
    insert into public.crew_chat_rooms (crew_id) values (p_crew) on conflict do nothing;
    update public.crew_chat_rooms set last_rev = last_rev + 1 where crew_id = p_crew returning last_rev into v_rev;
  end if;
  return v_rev;
end;
$$;

create or replace function public.crew_chat_state(p_msg public.crew_chat_messages)
returns text
language sql
immutable
as $$
  select case when p_msg.deleted_at is not null then 'DELETED' when p_msg.hidden_at is not null then 'HIDDEN' else 'VISIBLE' end
$$;

-- 앱에 보내는 메시지 모양. 삭제 · 숨김이면 내용 · 사진을 싣지 않는다(인용도 같다). 요청 키는 보낸 사람에게만.
-- 크루장 표시는 앱이 방 정보의 owner_id 로 정한다(크루장이 바뀌면 바로 따라가게).
create or replace function public.crew_chat_json(p_msg public.crew_chat_messages, p_me uuid)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'id', p_msg.id,
    'seq', p_msg.seq,
    'rev', p_msg.rev,
    'author_id', p_msg.author_id,
    'author_name', case when p_msg.author_id is not null
                        then coalesce((select pr.display_name from public.profiles pr where pr.id = p_msg.author_id), '러너') end,
    'kind', p_msg.kind,
    'state', public.crew_chat_state(p_msg),
    'body', case when p_msg.deleted_at is null and p_msg.hidden_at is null then p_msg.body end,
    'has_image', p_msg.kind = 'IMAGE' and p_msg.deleted_at is null and p_msg.hidden_at is null,
    'event', p_msg.event,
    'event_name', nullif(p_msg.event_name, ''),
    'reply', (
      select jsonb_build_object(
        'id', r.id,
        'seq', r.seq,
        'author_id', r.author_id,
        'author_name', coalesce(pr.display_name, '러너'),
        'kind', r.kind,
        'state', public.crew_chat_state(r),
        'body', case when r.deleted_at is null and r.hidden_at is null then left(r.body, 120) end)
        from public.crew_chat_messages r
        left join public.profiles pr on pr.id = r.author_id
       where r.id = p_msg.reply_to),
    'client_id', case when p_msg.author_id = p_me then p_msg.client_id end,
    'can_delete', p_msg.author_id = p_me and p_msg.deleted_at is null and p_msg.kind <> 'SYSTEM',
    'created_at', p_msg.created_at)
$$;

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
    'updated_at', p_notice.updated_at)
$$;

-- 방 정보 — 제목 · 대표 이미지 · 인원(지금 크루원 수, 접속자 수 아님) · 내 역할 · 고정 공지 · 공지 수 · 내 알림 ·
-- 크루원의 읽은 위치(내 메시지 옆 미확인 수를 앱이 센다 — 크루가 너무 크면 싣지 않고 앱도 숫자를 보이지 않는다)
create or replace function public.crew_chat_meta(p_crew uuid, p_me uuid)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select jsonb_build_object(
    'crew_id', c.id,
    'name', c.name,
    'image_bg', c.image_bg,
    'image_ver', c.image_ver,
    'has_image', exists (select 1 from public.crew_images i where i.crew_id = c.id),
    'owner_id', c.owner_id,
    'role', (select m.role from public.crew_members m where m.crew_id = c.id and m.user_id = p_me),
    'member_count', n.members,
    'last_rev', coalesce((select r.last_rev from public.crew_chat_rooms r where r.crew_id = c.id), 0),
    'notify', coalesce((select s.notify from public.crew_chat_settings s where s.crew_id = c.id and s.user_id = p_me), true),
    'notice_count', (select count(*) from public.crew_chat_notices x where x.crew_id = c.id),
    'pinned', (select public.crew_chat_notice_json(x) from public.crew_chat_notices x where x.crew_id = c.id and x.pinned),
    'my_read_seq', coalesce((select rd.last_read_seq from public.crew_chat_reads rd where rd.crew_id = c.id and rd.user_id = p_me), 0),
    'reads', case when n.members <= 300 then (
      select coalesce(jsonb_agg(jsonb_build_object(
               'user_id', m.user_id, 'seq', coalesce(rd.last_read_seq, 0), 'joined_at', m.joined_at)), '[]'::jsonb)
        from public.crew_members m
        left join public.crew_chat_reads rd on rd.crew_id = m.crew_id and rd.user_id = m.user_id
       where m.crew_id = c.id) end)
    from public.crews c
    cross join lateral (select count(*)::int as members from public.crew_members m where m.crew_id = c.id) n
   where c.id = p_crew
$$;

-- 새 메시지를 크루원에게 알린다 — 알림을 켠 사람만(전체 알림 설정 push_allowed 도 본다), 보낸 사람 · 보낸 사람을
-- 차단한 사람은 빼고. 아직 안 나간 같은 방 알림이 있으면 새로 쌓지 않고 내용만 바꾼다(한 방에 한 줄).
create or replace function public.crew_chat_push(p_msg public.crew_chat_messages)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  r record;
  v_link text := 'crew-chat/' || p_msg.crew_id;
  v_args jsonb;
begin
  if p_msg.kind = 'SYSTEM' then
    return;
  end if;
  v_args := jsonb_build_object(
    'crew', (select c.name from public.crews c where c.id = p_msg.crew_id),
    'name', public.push_display_name(p_msg.author_id),
    'text', left(regexp_replace(p_msg.body, '\s+', ' ', 'g'), 80),
    'photo', case when p_msg.kind = 'IMAGE' then '1' else '0' end);
  for r in
    select m.user_id
      from public.crew_members m
      left join public.crew_chat_settings s on s.crew_id = m.crew_id and s.user_id = m.user_id
     where m.crew_id = p_msg.crew_id
       and m.user_id <> p_msg.author_id
       and coalesce(s.notify, true)
       and not exists (select 1 from public.user_blocks b where b.blocker_id = m.user_id and b.blocked_id = p_msg.author_id)
  loop
    update public.push_outbox o set args = v_args
     where o.user_id = r.user_id and o.kind = 'CREW_CHAT' and o.link = v_link
       and o.sent_at is null and o.claimed_at is null;
    if not found then
      perform public.push_enqueue(r.user_id, 'CREW_CHAT', v_args, v_link);
    end if;
  end loop;
end;
$$;

-- 크루를 떠난 사람(탈퇴 · 내보내기 · 계정 삭제) — 대화에 한 줄 남긴다. 크루가 해산되는 중이면 남기지 않는다.
create or replace function public.crew_chat_on_member_left()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_rev bigint;
begin
  if not exists (select 1 from public.crews c where c.id = old.crew_id)
     or not exists (select 1 from public.crew_chat_rooms r where r.crew_id = old.crew_id) then
    return old;
  end if;
  v_rev := public.crew_chat_next_rev(old.crew_id);
  insert into public.crew_chat_messages (crew_id, seq, rev, kind, event, event_name)
  values (old.crew_id, v_rev, v_rev, 'SYSTEM', 'MEMBER_LEFT', public.push_display_name(old.user_id));
  return old;
end;
$$;

drop trigger if exists crew_chat_on_member_left on public.crew_members;
create trigger crew_chat_on_member_left
  after delete on public.crew_members
  for each row execute function public.crew_chat_on_member_left();

-- ────────────────────────────────────────────────────────────────────
--  목록 · 방 · 대화
-- ────────────────────────────────────────────────────────────────────

-- 01 내 크루 대화 — 내가 가입한 크루만. 마지막 메시지 · 내가 읽지 않은 수(내 메시지 · 알림 줄 · 가입 전 메시지는 세지 않는다)
create or replace function public.crew_chat_rooms()
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  if v_me is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return (
    select coalesce(jsonb_agg(x.room order by x.sort_at desc, x.crew_id), '[]'::jsonb)
      from (
        select c.id as crew_id,
               greatest(c.created_at, coalesce(l.created_at, c.created_at)) as sort_at,
               jsonb_build_object(
                 'crew_id', c.id,
                 'name', c.name,
                 'image_bg', c.image_bg,
                 'image_ver', c.image_ver,
                 'has_image', exists (select 1 from public.crew_images i where i.crew_id = c.id),
                 'owner_id', c.owner_id,
                 'role', m.role,
                 'member_count', (select count(*) from public.crew_members x where x.crew_id = c.id),
                 'last', (select public.crew_chat_json(lm, v_me) from public.crew_chat_messages lm where lm.id = l.id),
                 'unread', (
                   select count(*) from public.crew_chat_messages u
                    where u.crew_id = c.id and u.kind <> 'SYSTEM' and u.author_id is distinct from v_me
                      and u.seq > coalesce(rd.last_read_seq, 0) and u.created_at > m.joined_at
                      and u.deleted_at is null and u.hidden_at is null),
                 'created_at', c.created_at) as room
          from public.crew_members m
          join public.crews c on c.id = m.crew_id
          left join public.crew_chat_reads rd on rd.crew_id = c.id and rd.user_id = v_me
          left join lateral (
            select lm.id, lm.created_at from public.crew_chat_messages lm where lm.crew_id = c.id order by lm.seq desc limit 1
          ) l on true
         where m.user_id = v_me
      ) x
  );
end;
$$;

-- 방을 따라온다 — p_since_rev 뒤에 바뀐 메시지(새 메시지 · 삭제 · 숨김)와 지금의 방 정보.
-- 처음 열 때(p_since_rev 가 null)나 너무 많이 밀렸으면 최근 50개를 다시 준다(reset).
create or replace function public.crew_chat_sync(p_crew uuid, p_since_rev bigint default null)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_last bigint;
  v_reset boolean;
  v_messages jsonb;
begin
  perform public.crew_chat_member(p_crew);
  v_last := coalesce((select r.last_rev from public.crew_chat_rooms r where r.crew_id = p_crew), 0);
  v_reset := p_since_rev is null or p_since_rev > v_last or v_last - p_since_rev > 300;
  if v_reset then
    select coalesce(jsonb_agg(public.crew_chat_json(m, v_me) order by m.seq), '[]'::jsonb) into v_messages
      from public.crew_chat_messages m
     where m.id in (select x.id from public.crew_chat_messages x
                     where x.crew_id = p_crew and x.rev <= v_last
                     order by x.seq desc limit 50);
  else
    select coalesce(jsonb_agg(public.crew_chat_json(m, v_me) order by m.seq), '[]'::jsonb) into v_messages
      from public.crew_chat_messages m
     where m.crew_id = p_crew and m.rev > p_since_rev and m.rev <= v_last;
  end if;
  return jsonb_build_object(
    'room', public.crew_chat_meta(p_crew, v_me),
    'messages', v_messages,
    'reset', v_reset,
    'last_rev', v_last);
end;
$$;

-- 이전 대화 — p_before_seq 앞의 메시지를 오래된 순으로(한 번에 최대 100개)
create or replace function public.crew_chat_history(p_crew uuid, p_before_seq bigint, p_limit int default 50)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  perform public.crew_chat_member(p_crew);
  return (
    select coalesce(jsonb_agg(public.crew_chat_json(m, v_me) order by m.seq), '[]'::jsonb)
      from public.crew_chat_messages m
     where m.id in (select x.id from public.crew_chat_messages x
                     where x.crew_id = p_crew and x.seq < coalesce(p_before_seq, 9223372036854775807)
                     order by x.seq desc limit least(greatest(coalesce(p_limit, 50), 1), 100))
  );
end;
$$;

-- 보내기 — 글 또는 사진(+ 설명). 같은 요청 키로 다시 보내면 처음 저장한 메시지를 돌려준다(두 개가 되지 않는다).
create or replace function public.crew_chat_send(
  p_crew uuid,
  p_client_id uuid,
  p_body text default '',
  p_reply_to bigint default null,
  p_image text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_msg public.crew_chat_messages%rowtype;
  v_body text := btrim(coalesce(p_body, ''), E' \t\r\n');
  v_rev bigint;
begin
  perform public.crew_chat_member(p_crew, true);
  if p_client_id is null then
    raise exception 'invalid:client' using errcode = '22023';
  end if;
  select * into v_msg from public.crew_chat_messages m where m.author_id = v_me and m.client_id = p_client_id;
  if found then
    if v_msg.crew_id <> p_crew then
      raise exception 'invalid:client' using errcode = '22023';
    end if;
    return public.crew_chat_json(v_msg, v_me);
  end if;
  if p_image is null and v_body = '' then
    raise exception 'invalid:empty' using errcode = '22023', detail = '보낼 내용이 없습니다';
  end if;
  if length(v_body) > 2000 then
    raise exception 'invalid:body' using errcode = '22023', detail = '메시지는 2000자까지입니다';
  end if;
  if p_image is not null and not public.crew_image_ok(p_image) then
    raise exception 'invalid:image' using errcode = '22023', detail = '사진을 읽을 수 없습니다';
  end if;
  if p_reply_to is not null and not exists (
    select 1 from public.crew_chat_messages r where r.id = p_reply_to and r.crew_id = p_crew and r.kind <> 'SYSTEM'
  ) then
    raise exception 'invalid:reply' using errcode = '22023';
  end if;

  v_rev := public.crew_chat_next_rev(p_crew);
  insert into public.crew_chat_messages (crew_id, seq, rev, author_id, kind, body, reply_to, client_id)
  values (p_crew, v_rev, v_rev, v_me, case when p_image is null then 'TEXT' else 'IMAGE' end, v_body, p_reply_to, p_client_id)
  on conflict (author_id, client_id) where client_id is not null do nothing
  returning * into v_msg;
  if v_msg.id is null then
    -- 같은 요청이 동시에 두 번 왔다 — 먼저 끝난 것을 돌려준다
    select * into v_msg from public.crew_chat_messages m where m.author_id = v_me and m.client_id = p_client_id;
    return public.crew_chat_json(v_msg, v_me);
  end if;
  if p_image is not null then
    insert into public.crew_chat_images (message_id, data) values (v_msg.id, p_image);
  end if;
  -- 내가 보낸 것까지는 읽은 것
  insert into public.crew_chat_reads (crew_id, user_id, last_read_seq) values (p_crew, v_me, v_msg.seq)
  on conflict (crew_id, user_id) do update
    set last_read_seq = greatest(public.crew_chat_reads.last_read_seq, excluded.last_read_seq), updated_at = now();
  perform public.crew_chat_push(v_msg);
  return public.crew_chat_json(v_msg, v_me);
end;
$$;

-- 연결이 끊겼을 때 보내던 메시지 — 서버가 받았는지 요청 키로 확인한다(받은 것만 돌려준다. 다시 보내지는 않는다)
create or replace function public.crew_chat_confirm(p_crew uuid, p_client_ids uuid[])
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
begin
  perform public.crew_chat_member(p_crew);
  return (
    select coalesce(jsonb_agg(public.crew_chat_json(m, v_me) order by m.seq), '[]'::jsonb)
      from public.crew_chat_messages m
     where m.crew_id = p_crew and m.author_id = v_me and m.client_id = any(coalesce(p_client_ids, '{}'))
  );
end;
$$;

-- 읽은 위치 — 앞으로만 옮긴다(방의 마지막 순서를 넘지 않게)
create or replace function public.crew_chat_read(p_crew uuid, p_seq bigint)
returns bigint
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_seq bigint;
begin
  perform public.crew_chat_member(p_crew);
  v_seq := least(greatest(coalesce(p_seq, 0), 0),
                 coalesce((select r.last_rev from public.crew_chat_rooms r where r.crew_id = p_crew), 0));
  insert into public.crew_chat_reads (crew_id, user_id, last_read_seq) values (p_crew, v_me, v_seq)
  on conflict (crew_id, user_id) do update
    set last_read_seq = greatest(public.crew_chat_reads.last_read_seq, excluded.last_read_seq), updated_at = now()
  returning last_read_seq into v_seq;
  return v_seq;
end;
$$;

-- 내 메시지 삭제 — 모든 크루원에게 "삭제된 메시지예요."(자리 · 시간은 그대로). 시간 제한은 두지 않는다.
create or replace function public.crew_chat_delete(p_message bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_msg public.crew_chat_messages%rowtype;
  v_rev bigint;
begin
  select * into v_msg from public.crew_chat_messages m where m.id = p_message;
  if not found or v_msg.kind = 'SYSTEM' then
    raise exception 'message_missing' using errcode = '22023';
  end if;
  perform public.crew_chat_member(v_msg.crew_id);
  if v_msg.author_id is distinct from v_me then
    raise exception 'not_author' using errcode = '42501', detail = '내가 보낸 메시지만 지울 수 있습니다';
  end if;
  if v_msg.deleted_at is not null then
    return public.crew_chat_json(v_msg, v_me);
  end if;
  v_rev := public.crew_chat_next_rev(v_msg.crew_id);
  update public.crew_chat_messages set deleted_at = now(), body = '', rev = v_rev where id = p_message
  returning * into v_msg;
  delete from public.crew_chat_images where message_id = p_message;
  -- 이 메시지를 인용한 답장도 새로 읽게(인용 부분이 삭제 상태로 바뀐다)
  update public.crew_chat_messages set rev = v_rev where reply_to = p_message;
  return public.crew_chat_json(v_msg, v_me);
end;
$$;

-- 크루장 · 메시지 숨기기 — 선택한 메시지만. 작성자의 크루 소속은 그대로다(내보내기와 다르다)
create or replace function public.crew_chat_hide(p_message bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_msg public.crew_chat_messages%rowtype;
  v_rev bigint;
begin
  select * into v_msg from public.crew_chat_messages m where m.id = p_message;
  if not found or v_msg.kind = 'SYSTEM' then
    raise exception 'message_missing' using errcode = '22023';
  end if;
  perform public.crew_chat_member(v_msg.crew_id);
  perform public.crew_lock_owner(v_msg.crew_id);
  if v_msg.author_id = v_me then
    raise exception 'own_message' using errcode = '22023', detail = '내 메시지는 삭제로 지웁니다';
  end if;
  select * into v_msg from public.crew_chat_messages m where m.id = p_message;
  if v_msg.hidden_at is not null or v_msg.deleted_at is not null then
    return public.crew_chat_json(v_msg, v_me);
  end if;
  v_rev := public.crew_chat_next_rev(v_msg.crew_id);
  update public.crew_chat_messages set hidden_at = now(), hidden_by = v_me, rev = v_rev where id = p_message
  returning * into v_msg;
  update public.crew_chat_messages set rev = v_rev where reply_to = p_message;
  return public.crew_chat_json(v_msg, v_me);
end;
$$;

-- 사진 크게 보기 — 방에 들어갈 수 있고 메시지가 삭제 · 숨김되지 않았을 때만
create or replace function public.crew_chat_image(p_message bigint)
returns text
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_msg public.crew_chat_messages%rowtype;
begin
  select * into v_msg from public.crew_chat_messages m where m.id = p_message;
  if not found then
    raise exception 'message_missing' using errcode = '22023';
  end if;
  perform public.crew_chat_member(v_msg.crew_id);
  if v_msg.kind <> 'IMAGE' or v_msg.deleted_at is not null or v_msg.hidden_at is not null then
    raise exception 'message_gone' using errcode = '22023';
  end if;
  return (select i.data from public.crew_chat_images i where i.message_id = p_message);
end;
$$;

-- 대화 검색 — 이 방의 보이는 메시지(글 · 사진 설명)만, 최근 것부터 50개
create or replace function public.crew_chat_search(p_crew uuid, p_query text)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_me uuid := auth.uid();
  v_query text := btrim(coalesce(p_query, ''));
begin
  perform public.crew_chat_member(p_crew);
  if v_query = '' then
    return '[]'::jsonb;
  end if;
  v_query := '%' || replace(replace(replace(left(v_query, 60), '\', '\\'), '%', '\%'), '_', '\_') || '%';
  return (
    select coalesce(jsonb_agg(public.crew_chat_json(m, v_me) order by m.seq desc), '[]'::jsonb)
      from public.crew_chat_messages m
     where m.id in (select x.id from public.crew_chat_messages x
                     where x.crew_id = p_crew and x.kind <> 'SYSTEM'
                       and x.deleted_at is null and x.hidden_at is null
                       and x.body ilike v_query
                     order by x.seq desc limit 50)
  );
end;
$$;

-- 메시지 신고 — 욕설 또는 괴롭힘(ABUSE) · 광고 또는 도배(SPAM) · 부적절한 내용(OTHER). 같은 메시지는 한 번만 쌓인다
create or replace function public.crew_chat_report(p_message bigint, p_reason text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_msg public.crew_chat_messages%rowtype;
begin
  select * into v_msg from public.crew_chat_messages m where m.id = p_message;
  if not found or v_msg.kind = 'SYSTEM' then
    raise exception 'message_missing' using errcode = '22023';
  end if;
  perform public.crew_chat_member(v_msg.crew_id);
  if v_msg.author_id = auth.uid() then
    raise exception 'own_message' using errcode = '22023';
  end if;
  if v_msg.deleted_at is not null or v_msg.hidden_at is not null then
    raise exception 'message_gone' using errcode = '22023';
  end if;
  if p_reason is null or p_reason not in ('ABUSE', 'SPAM', 'OTHER') then
    raise exception 'invalid:reason' using errcode = '22023';
  end if;
  insert into public.content_reports (reporter_id, target_type, target_id, reason, note)
  values (auth.uid(), 'CHAT', p_message::text, p_reason, '')
  on conflict (reporter_id, target_type, target_id) do nothing;
end;
$$;

-- 내 채팅 알림 — 나에게만 적용된다
create or replace function public.crew_chat_notify_set(p_crew uuid, p_on boolean)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.crew_chat_member(p_crew);
  insert into public.crew_chat_settings (crew_id, user_id, notify) values (p_crew, auth.uid(), coalesce(p_on, true))
  on conflict (crew_id, user_id) do update set notify = excluded.notify;
  return coalesce(p_on, true);
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  공지
-- ────────────────────────────────────────────────────────────────────

-- 09 공지 모아보기 — 고정 공지가 먼저, 그다음 최근 공지
create or replace function public.crew_chat_notices(p_crew uuid)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.crew_chat_member(p_crew);
  return (
    select coalesce(jsonb_agg(public.crew_chat_notice_json(n) order by n.pinned desc, n.created_at desc, n.id desc), '[]'::jsonb)
      from public.crew_chat_notices n
     where n.crew_id = p_crew
  );
end;
$$;

-- 10 등록 · 11 수정 — 크루장만. 새로 고정하면 이전 고정은 풀린다(공지는 목록에 남는다). 새 공지는 대화에 한 줄
-- ("○○ 님이 새 공지를 등록했어요.")을 한 번만 남긴다 — 같은 요청 키로 다시 보내도 두 번 남지 않는다.
create or replace function public.crew_chat_notice_save(
  p_crew uuid,
  p_notice bigint,
  p_title text,
  p_body text,
  p_pinned boolean,
  p_client_key uuid default null
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
    insert into public.crew_chat_notices (crew_id, author_id, title, body, pinned, client_key)
    values (p_crew, v_me, v_title, v_body, coalesce(p_pinned, false), p_client_key)
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
       set title = v_title, body = v_body, pinned = coalesce(p_pinned, false), updated_at = now()
     where id = p_notice
    returning * into v_row;
  end if;
  return public.crew_chat_notice_json(v_row);
end;
$$;

-- 12 공지 삭제 — 공지와 고정만 지운다. 대화 메시지는 그대로다
create or replace function public.crew_chat_notice_delete(p_notice bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_crew uuid;
begin
  select n.crew_id into v_crew from public.crew_chat_notices n where n.id = p_notice;
  if v_crew is null then
    raise exception 'notice_missing' using errcode = '22023', detail = '공지를 찾을 수 없습니다';
  end if;
  perform public.crew_chat_member(v_crew);
  perform public.crew_lock_owner(v_crew);
  delete from public.crew_chat_notices where id = p_notice;
end;
$$;

-- ────────────────────────────────────────────────────────────────────
--  권한 — 앱(authenticated)은 함수로만
-- ────────────────────────────────────────────────────────────────────

revoke execute on function public.crew_chat_room_for_crew() from public, anon, authenticated;
revoke execute on function public.crew_chat_member(uuid, boolean) from public, anon, authenticated;
revoke execute on function public.crew_chat_next_rev(uuid) from public, anon, authenticated;
revoke execute on function public.crew_chat_state(public.crew_chat_messages) from public, anon, authenticated;
revoke execute on function public.crew_chat_json(public.crew_chat_messages, uuid) from public, anon, authenticated;
revoke execute on function public.crew_chat_notice_json(public.crew_chat_notices) from public, anon, authenticated;
revoke execute on function public.crew_chat_meta(uuid, uuid) from public, anon, authenticated;
revoke execute on function public.crew_chat_push(public.crew_chat_messages) from public, anon, authenticated;
revoke execute on function public.crew_chat_on_member_left() from public, anon, authenticated;

revoke execute on function public.crew_chat_rooms() from public, anon;
revoke execute on function public.crew_chat_sync(uuid, bigint) from public, anon;
revoke execute on function public.crew_chat_history(uuid, bigint, int) from public, anon;
revoke execute on function public.crew_chat_send(uuid, uuid, text, bigint, text) from public, anon;
revoke execute on function public.crew_chat_confirm(uuid, uuid[]) from public, anon;
revoke execute on function public.crew_chat_read(uuid, bigint) from public, anon;
revoke execute on function public.crew_chat_delete(bigint) from public, anon;
revoke execute on function public.crew_chat_hide(bigint) from public, anon;
revoke execute on function public.crew_chat_image(bigint) from public, anon;
revoke execute on function public.crew_chat_search(uuid, text) from public, anon;
revoke execute on function public.crew_chat_report(bigint, text) from public, anon;
revoke execute on function public.crew_chat_notify_set(uuid, boolean) from public, anon;
revoke execute on function public.crew_chat_notices(uuid) from public, anon;
revoke execute on function public.crew_chat_notice_save(uuid, bigint, text, text, boolean, uuid) from public, anon;
revoke execute on function public.crew_chat_notice_delete(bigint) from public, anon;

grant execute on function public.crew_chat_rooms() to authenticated;
grant execute on function public.crew_chat_sync(uuid, bigint) to authenticated;
grant execute on function public.crew_chat_history(uuid, bigint, int) to authenticated;
grant execute on function public.crew_chat_send(uuid, uuid, text, bigint, text) to authenticated;
grant execute on function public.crew_chat_confirm(uuid, uuid[]) to authenticated;
grant execute on function public.crew_chat_read(uuid, bigint) to authenticated;
grant execute on function public.crew_chat_delete(bigint) to authenticated;
grant execute on function public.crew_chat_hide(bigint) to authenticated;
grant execute on function public.crew_chat_image(bigint) to authenticated;
grant execute on function public.crew_chat_search(uuid, text) to authenticated;
grant execute on function public.crew_chat_report(bigint, text) to authenticated;
grant execute on function public.crew_chat_notify_set(uuid, boolean) to authenticated;
grant execute on function public.crew_chat_notices(uuid) to authenticated;
grant execute on function public.crew_chat_notice_save(uuid, bigint, text, text, boolean, uuid) to authenticated;
grant execute on function public.crew_chat_notice_delete(bigint) to authenticated;
