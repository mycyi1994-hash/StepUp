-- 0050 크루 채팅 알림 정리(2026-09-29 검토 반영) — 지우거나 가린 메시지가 아직 안 나간 알림으로 나가지 않게
--
-- 크루 채팅 알림(0048 crew_chat_push)은 방마다 기다리는 알림 한 줄에 마지막 메시지의 글 · 사진 여부를 담는다.
-- 그 메시지가 보내지기 전에 지워지거나(작성자) 가려지면(크루장) 알림 줄에는 원래 글이 남아 있었다.
--   · 알림 줄에 어느 메시지인지(args.msg)를 함께 적는다.
--   · 메시지가 지워지거나 가려지는 순간, 아직 가져가지 않은(보내는 중이 아닌) 그 메시지의 알림 줄을 지운다.
--     같은 방의 더 새 메시지로 이미 바뀐 줄은 그대로 둔다(지운 글이 들어 있지 않다).

-- 새 메시지를 크루원에게 알린다 — 0048 과 같고 알림 줄에 메시지 id(msg)만 더했다
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
    'photo', case when p_msg.kind = 'IMAGE' then '1' else '0' end,
    'msg', p_msg.id::text);
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

-- 메시지가 지워지거나 가려졌다 — 아직 가져가지 않은 그 메시지의 알림 줄을 지운다
create or replace function public.crew_chat_push_drop()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  delete from public.push_outbox o
   where o.kind = 'CREW_CHAT' and o.link = 'crew-chat/' || new.crew_id
     and o.sent_at is null and o.claimed_at is null
     and o.args->>'msg' = new.id::text;
  return new;
end;
$$;

drop trigger if exists crew_chat_push_drop on public.crew_chat_messages;
create trigger crew_chat_push_drop
  after update of deleted_at, hidden_at on public.crew_chat_messages
  for each row
  when ((old.deleted_at is null and new.deleted_at is not null) or (old.hidden_at is null and new.hidden_at is not null))
  execute function public.crew_chat_push_drop();

revoke all on function public.crew_chat_push_drop() from public, anon, authenticated;
revoke execute on function public.crew_chat_push(public.crew_chat_messages) from public, anon, authenticated;
