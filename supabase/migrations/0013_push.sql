-- 푸시 알림 — 폰마다 받는 주소(FCM 토큰)를 적어 둔다.
--
-- 토큰 하나는 폰 하나(앱 설치 하나)다. 한 사람이 폰을 여러 대 쓰면 줄이
-- 여럿이고, 한 폰에서 다른 계정으로 로그인하면 그 토큰은 새 계정으로 옮겨
-- 간다 — 앞 사람의 알림이 뒤 사람 폰에 뜨면 안 되기 때문이다.
--
-- 보내는 일은 서버(Edge Function)가 한다. 앱은 토큰을 적기만 하고, 남의
-- 토큰은 읽을 수 없다.

create table if not exists public.push_tokens (
  token text primary key check (length(token) between 20 and 4096),
  user_id uuid not null references auth.users on delete cascade,
  platform text not null default 'android' check (platform in ('android', 'ios')),
  -- 알림 글을 어느 언어로 쓸지. 앱 언어(ko · en · ja · zh)를 그대로 받는다.
  locale text not null default 'ko' check (length(locale) <= 10),
  updated_at timestamptz not null default now()
);

create index if not exists push_tokens_user on public.push_tokens (user_id);

comment on table public.push_tokens is
  '푸시 알림을 받을 폰(FCM 토큰). 앱은 push_register 로 적기만 하고 읽지 못한다.';

alter table public.push_tokens enable row level security;
revoke all on public.push_tokens from anon, authenticated;

-- 이 폰의 토큰을 지금 로그인한 사람에게 붙인다. 앱을 켤 때마다, 토큰이
-- 바뀔 때마다 부른다.
create or replace function public.push_register(p_token text, p_locale text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(length(p_token), 0) not between 20 and 4096 then
    raise exception '알림 토큰이 올바르지 않습니다' using errcode = '22023';
  end if;
  -- 한 사람이 폰을 수십 대 쓰지는 않는다. 오래된 것부터 정리한다.
  delete from public.push_tokens
   where user_id = auth.uid()
     and token <> p_token
     and token not in (
       select t.token from public.push_tokens t
        where t.user_id = auth.uid()
        order by t.updated_at desc
        limit 9
     );

  insert into public.push_tokens (token, user_id, locale, updated_at)
  values (p_token, auth.uid(), left(coalesce(nullif(p_locale, ''), 'ko'), 10), now())
  on conflict (token) do update
    set user_id = excluded.user_id,
        locale = excluded.locale,
        updated_at = now();
end;
$$;

-- 이 폰에서 알림을 끈다(로그아웃·알림 끄기).
create or replace function public.push_unregister(p_token text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  delete from public.push_tokens where token = p_token and user_id = auth.uid();
end;
$$;

grant execute on function public.push_register(text, text) to authenticated;
grant execute on function public.push_unregister(text) to authenticated;
