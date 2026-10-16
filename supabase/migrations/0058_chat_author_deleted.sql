-- ════════════════════════════════════════════════════════════════════
--  0058 — 계정을 지운 사람의 크루 채팅은 남기고 "탈퇴한 사용자"로(2026-10-16 사용자 결정)
--
--  0048 은 계정을 지우면 그 사람의 메시지 · 공지를 함께 지웠다(게시판과 같다). 그런데 서버에서 사라진 메시지를
--  다른 크루원의 폰은 알 길이 없어(변경 번호가 오르지 않는다) 다시 받기 전까지 폰마다 대화가 달랐다
--  (점검 2026-10-15 — 사용자 결정 항목). 이제는 지우지 않고 쓴 사람만 비운다.
--
--    · 메시지 · 공지의 작성자 칸은 on delete set null — 계정 · 이름 · 프로필은 남지 않고 글 · 사진만 남는다.
--      알림 줄(SYSTEM)이 아닌데 작성자가 빈 줄이 "탈퇴한 사용자"의 것이다.
--    · 작성자가 비워지면 그 메시지와 그 메시지를 인용한 답장의 변경 번호(rev)를 올린다 — 다른 폰이 다음
--      따라오기에서 받아 이름을 바꾼다. 계정 삭제 함수(0021)가 아닌 길(대시보드에서 지우기)도 같다.
--    · 앱에 보내는 모양: author_id null · author_name '탈퇴한 사용자'(예전 앱도 그대로 보인다) · author_deleted true.
--      새 앱은 author_deleted 를 보고 폰의 언어로 쓴다. can_delete 는 null 이 아니라 false 로 보낸다 —
--      예전 앱은 null 을 읽지 못해 방 전체를 받지 못한다.
--  크루 게시판의 글 · 댓글, 내 읽은 위치 · 채팅 알림 설정은 전처럼 함께 지워진다(이번 결정은 채팅 메시지 · 공지만).
-- ════════════════════════════════════════════════════════════════════

-- 메시지 · 공지의 작성자: 지우지 않고 비운다. 다시 올려도(setup.sql) 이미 바뀌었으면 auth.users 를 잠그지 않는다
do $$
begin
  if not exists (select 1 from pg_constraint
                  where conrelid = 'public.crew_chat_messages'::regclass
                    and conname = 'crew_chat_messages_author_id_fkey' and confdeltype = 'n') then
    alter table public.crew_chat_messages drop constraint if exists crew_chat_messages_author_id_fkey;
    alter table public.crew_chat_messages add constraint crew_chat_messages_author_id_fkey
      foreign key (author_id) references auth.users (id) on delete set null;
  end if;
  -- 0048 의 모양 검사는 글 · 사진에 작성자를 요구했다 — 탈퇴한 사용자의 줄은 작성자가 비어도 된다
  if exists (select 1 from pg_constraint
              where conrelid = 'public.crew_chat_messages'::regclass and conname = 'crew_chat_messages_shape'
                and pg_get_constraintdef(oid) like '%author_id IS NOT NULL%') then
    alter table public.crew_chat_messages drop constraint crew_chat_messages_shape;
  end if;
  if not exists (select 1 from pg_constraint
                  where conrelid = 'public.crew_chat_messages'::regclass and conname = 'crew_chat_messages_shape') then
    alter table public.crew_chat_messages add constraint crew_chat_messages_shape check (
      (kind = 'SYSTEM' and author_id is null and event is not null)
      or (kind <> 'SYSTEM' and event is null));
  end if;

  if exists (select 1 from information_schema.columns
              where table_schema = 'public' and table_name = 'crew_chat_notices'
                and column_name = 'author_id' and is_nullable = 'NO') then
    alter table public.crew_chat_notices alter column author_id drop not null;
  end if;
  if not exists (select 1 from pg_constraint
                  where conrelid = 'public.crew_chat_notices'::regclass
                    and conname = 'crew_chat_notices_author_id_fkey' and confdeltype = 'n') then
    alter table public.crew_chat_notices drop constraint if exists crew_chat_notices_author_id_fkey;
    alter table public.crew_chat_notices add constraint crew_chat_notices_author_id_fkey
      foreign key (author_id) references auth.users (id) on delete set null;
  end if;
end $$;

comment on column public.crew_chat_messages.author_id is
  '보낸 사람. 알림 줄(SYSTEM)이 아닌데 비어 있으면 계정을 지운 사람(앱은 "탈퇴한 사용자")이다 (0058)';
comment on column public.crew_chat_notices.author_id is
  '쓴 크루장. 비어 있으면 계정을 지운 사람(앱은 "탈퇴한 사용자")이다 (0058)';

-- 작성자가 비워지면 다른 폰이 다시 받게 — 방의 변경 번호를 올려 이 메시지에 준다. 보낸 사람의 요청 키는 더 쓸 데가 없다.
-- 크루가 함께 지워지는 중이면(크루원 없던 크루장의 계정 삭제) 그대로 둔다 — 지우는 일은 차례로 이어져서 크루가 먼저
-- 사라지고 방 · 메시지는 뒤에 지워진다. 그 사이에 방을 고치면 사라진 크루를 가리킨다고 막힌다(0048 나간 사람 줄과 같다).
create or replace function public.crew_chat_on_author_gone()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_rev bigint;
begin
  if not exists (select 1 from public.crews c where c.id = new.crew_id) then
    return new;
  end if;
  update public.crew_chat_rooms set last_rev = last_rev + 1 where crew_id = new.crew_id returning last_rev into v_rev;
  if v_rev is not null then
    new.rev := v_rev;
  end if;
  new.client_id := null;
  return new;
end;
$$;

-- 이 메시지를 인용한 답장도 새로 읽게(인용의 이름이 바뀐다) — 삭제 · 숨김(0048)과 같다. 더 늦게 바뀐 답장은 그대로
create or replace function public.crew_chat_on_author_gone_replies()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if exists (select 1 from public.crews c where c.id = new.crew_id) then
    update public.crew_chat_messages set rev = new.rev where reply_to = new.id and rev < new.rev;
  end if;
  return null;
end;
$$;

drop trigger if exists crew_chat_on_author_gone on public.crew_chat_messages;
create trigger crew_chat_on_author_gone
  before update of author_id on public.crew_chat_messages
  for each row when (old.author_id is not null and new.author_id is null)
  execute function public.crew_chat_on_author_gone();

drop trigger if exists crew_chat_on_author_gone_replies on public.crew_chat_messages;
create trigger crew_chat_on_author_gone_replies
  after update of author_id on public.crew_chat_messages
  for each row when (old.author_id is not null and new.author_id is null)
  execute function public.crew_chat_on_author_gone_replies();

revoke all on function public.crew_chat_on_author_gone() from public, anon, authenticated;
revoke all on function public.crew_chat_on_author_gone_replies() from public, anon, authenticated;

-- 앱에 보내는 메시지 모양 — 0048 과 같고 탈퇴한 사용자(author_deleted)만 더했다
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
                        then coalesce((select pr.display_name from public.profiles pr where pr.id = p_msg.author_id), '러너')
                        when p_msg.kind <> 'SYSTEM' then '탈퇴한 사용자' end,
    'author_deleted', p_msg.author_id is null and p_msg.kind <> 'SYSTEM',
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
        'author_name', case when r.author_id is null and r.kind <> 'SYSTEM' then '탈퇴한 사용자'
                            else coalesce(pr.display_name, '러너') end,
        'author_deleted', r.author_id is null and r.kind <> 'SYSTEM',
        'kind', r.kind,
        'state', public.crew_chat_state(r),
        'body', case when r.deleted_at is null and r.hidden_at is null then left(r.body, 120) end)
        from public.crew_chat_messages r
        left join public.profiles pr on pr.id = r.author_id
       where r.id = p_msg.reply_to),
    'client_id', case when p_msg.author_id = p_me then p_msg.client_id end,
    'can_delete', coalesce(p_msg.author_id = p_me, false) and p_msg.deleted_at is null and p_msg.kind <> 'SYSTEM',
    'created_at', p_msg.created_at)
$$;

-- 공지 모양 — 0049 와 같고 탈퇴한 사용자(author_deleted)만 더했다
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
    'author_name', case when p_notice.author_id is null then '탈퇴한 사용자'
                        else coalesce((select pr.display_name from public.profiles pr where pr.id = p_notice.author_id), '러너') end,
    'author_deleted', p_notice.author_id is null,
    'created_at', p_notice.created_at,
    'updated_at', p_notice.updated_at,
    'meeting', (
      select jsonb_build_object('id', p.id, 'title', p.title, 'place', p.place, 'meet_at', p.meet_at)
        from public.posts p
       where p.id = p_notice.meeting_post and p.crew_id = p_notice.crew_id and p.category = 'FLASH'
         and not public.is_hidden('POST', p.id::text) and not public.is_blocked(p.author_id)))
$$;

revoke execute on function public.crew_chat_json(public.crew_chat_messages, uuid) from public, anon, authenticated;
revoke execute on function public.crew_chat_notice_json(public.crew_chat_notices) from public, anon, authenticated;
