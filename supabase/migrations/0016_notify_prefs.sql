-- 알림 설정 — 어떤 푸시를 받을지. 폰의 설정 화면과 같은 네 가지.
--
-- 알림은 서버가 보낸다. 그래서 "파티 초대는 받지 않음" 같은 선택도 서버가
-- 알아야 지켜진다. 폰에만 두면 끈 알림이 계속 온다.

create table if not exists public.notify_prefs (
  user_id uuid primary key references auth.users on delete cascade,
  -- 끄면 어떤 푸시도 보내지 않는다
  push boolean not null default true,
  goal_reminder boolean not null default true,
  party_invite boolean not null default true,
  event_news boolean not null default true,
  updated_at timestamptz not null default now()
);

comment on table public.notify_prefs is
  '받을 푸시 종류. 줄이 없으면 전부 받음. 보내는 쪽(Edge Function)이 보내기 전에 본다.';

alter table public.notify_prefs enable row level security;
revoke insert, update, delete on public.notify_prefs from anon, authenticated;
drop policy if exists notify_prefs_select_own on public.notify_prefs;
create policy notify_prefs_select_own on public.notify_prefs
  for select using ((select auth.uid()) = user_id);
grant select on public.notify_prefs to authenticated;

create or replace function public.notify_prefs_set(
  p_push boolean,
  p_goal_reminder boolean,
  p_party_invite boolean,
  p_event_news boolean
)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  insert into public.notify_prefs (user_id, push, goal_reminder, party_invite, event_news, updated_at)
  values (auth.uid(), coalesce(p_push, true), coalesce(p_goal_reminder, true),
          coalesce(p_party_invite, true), coalesce(p_event_news, true), now())
  on conflict (user_id) do update
    set push = excluded.push,
        goal_reminder = excluded.goal_reminder,
        party_invite = excluded.party_invite,
        event_news = excluded.event_news,
        updated_at = now();
end;
$$;

revoke execute on function public.notify_prefs_set(boolean, boolean, boolean, boolean) from public, anon;
grant execute on function public.notify_prefs_set(boolean, boolean, boolean, boolean) to authenticated;
