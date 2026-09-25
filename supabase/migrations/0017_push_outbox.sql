-- 푸시 보낼 목록 — 무엇을 누구에게 보낼지는 여기(데이터베이스)가 정하고,
-- 실제로 보내는 일은 Edge Function(supabase/functions/push-send)이 한다.
--
-- 받는 사람을 고르는 규칙(자기 글에 자기 댓글은 알리지 않음, 알림 설정에서 끈
-- 종류는 빼기)을 SQL 로 두면 테스트로 지킬 수 있다. 보내는 쪽은 이 표를 비우기만
-- 한다 — 행이 생기면 Database Webhook 이 함수를 깨운다.

create table if not exists public.push_outbox (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  -- COMMENT · REPLY · CREW_REQUEST · PARTY_OPEN · CREW_FLASH
  kind text not null,
  -- 알림 글에 넣을 값(누가 · 어느 글 · 어느 크루). 글은 받는 사람의 언어로 함수가 짓는다.
  args jsonb not null default '{}'::jsonb,
  -- 알림을 눌렀을 때 열 앱 안 자리(예: crew/<id>). 앱의 InviteLinks 가 읽는다.
  link text not null default '',
  created_at timestamptz not null default now(),
  sent_at timestamptz,
  attempts int not null default 0,
  last_error text not null default ''
);

create index if not exists push_outbox_pending on public.push_outbox (created_at) where sent_at is null;

-- 보내는 쪽이 가져간 시각. 가져간 줄은 잠시 다른 호출에 다시 주지 않는다(아래 push_claim_batch).
alter table public.push_outbox add column if not exists claimed_at timestamptz;

comment on table public.push_outbox is
  '보낼 푸시. 트리거가 채우고 push-send 함수가 보낸 뒤 sent_at 을 적는다. 앱은 볼 수 없다.';

alter table public.push_outbox enable row level security;
revoke all on public.push_outbox from anon, authenticated;

-- 이 사람이 이 종류의 푸시를 받는가. 설정 줄이 없으면 받는다.
create or replace function public.push_allowed(p_user uuid, p_kind text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((
    select n.push and case p_kind
      when 'PARTY_OPEN' then n.party_invite
      when 'CREW_FLASH' then n.event_news
      else true
    end
    from public.notify_prefs n where n.user_id = p_user
  ), true)
$$;

create or replace function public.push_enqueue(p_user uuid, p_kind text, p_args jsonb, p_link text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_user is null or not public.push_allowed(p_user, p_kind) then
    return;
  end if;
  -- 받을 폰이 없으면 적어 둘 까닭이 없다
  if not exists (select 1 from public.push_tokens t where t.user_id = p_user) then
    return;
  end if;
  insert into public.push_outbox (user_id, kind, args, link)
  values (p_user, p_kind, coalesce(p_args, '{}'::jsonb), coalesce(p_link, ''));
end;
$$;

create or replace function public.push_display_name(p_user uuid)
returns text
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((select display_name from public.profiles where id = p_user), '러너')
$$;

-- 댓글 → 글쓴이에게, 답글 → 댓글 단 사람에게. 자기 자신에게는 보내지 않는다.
create or replace function public.push_on_comment()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_to uuid;
  v_kind text;
  v_title text;
  v_crew uuid;
begin
  select p.title, p.crew_id into v_title, v_crew from public.posts p where p.id = new.post_id;
  if new.parent_id is not null then
    select c.author_id into v_to from public.comments c where c.id = new.parent_id;
    v_kind := 'REPLY';
  else
    select p.author_id into v_to from public.posts p where p.id = new.post_id;
    v_kind := 'COMMENT';
  end if;
  if v_to is not null and v_to <> new.author_id then
    perform public.push_enqueue(
      v_to, v_kind,
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', coalesce(v_title, '')),
      case when v_crew is not null then 'crew/' || v_crew::text else '' end);
  end if;
  return new;
end;
$$;

-- 가입 신청 → 크루장에게
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

-- 크루 파티런 로비가 열렸다 → 그 크루원들에게(연 사람 빼고)
create or replace function public.push_on_party()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
  v_member uuid;
begin
  if new.crew_id is null then
    return new;
  end if;
  select c.name into v_name from public.crews c where c.id = new.crew_id;
  for v_member in
    select m.user_id from public.crew_members m
     where m.crew_id = new.crew_id and m.user_id <> new.host_id
  loop
    perform public.push_enqueue(
      v_member, 'PARTY_OPEN',
      jsonb_build_object('name', public.push_display_name(new.host_id), 'crew', coalesce(v_name, '')),
      'crew/' || new.crew_id::text);
  end loop;
  return new;
end;
$$;

-- 크루 게시판에 번개가 올라왔다 → 그 크루원들에게(쓴 사람 빼고)
create or replace function public.push_on_flash()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_member uuid;
begin
  if new.category <> 'FLASH' or new.crew_id is null then
    return new;
  end if;
  for v_member in
    select m.user_id from public.crew_members m
     where m.crew_id = new.crew_id and m.user_id <> new.author_id
  loop
    perform public.push_enqueue(
      v_member, 'CREW_FLASH',
      jsonb_build_object('name', public.push_display_name(new.author_id), 'title', new.title),
      'crew/' || new.crew_id::text);
  end loop;
  return new;
end;
$$;

drop trigger if exists push_on_comment on public.comments;
create trigger push_on_comment after insert on public.comments
  for each row execute function public.push_on_comment();

drop trigger if exists push_on_crew_request on public.crew_join_requests;
create trigger push_on_crew_request after insert on public.crew_join_requests
  for each row execute function public.push_on_crew_request();

drop trigger if exists push_on_party on public.parties;
create trigger push_on_party after insert on public.parties
  for each row execute function public.push_on_party();

drop trigger if exists push_on_flash on public.posts;
create trigger push_on_flash after insert on public.posts
  for each row execute function public.push_on_flash();

revoke execute on function public.push_allowed(uuid, text) from public, anon, authenticated;
revoke execute on function public.push_enqueue(uuid, text, jsonb, text) from public, anon, authenticated;
revoke execute on function public.push_display_name(uuid) from public, anon, authenticated;

-- ── 보내는 쪽(push-send, service_role)만 쓰는 함수 ────────────────

-- 보낼 것을 한 묶음 가져간다. 함수가 동시에 두 번 깨어나도 같은 줄을 두 번
-- 보내지 않게 잠근 줄은 건너뛴다. 다섯 번 실패한 줄은 더 시도하지 않는다.
-- 행 잠금은 이 호출이 끝나면 풀린다. 보내고 push_mark 로 적기 전까지 다음 호출이
-- 같은 줄을 또 가져가 두 번 울리지 않게, 가져간 시각을 적고 2분은 건너뛴다.
-- 실패한 줄도 2분 뒤에 다시 가져간다 — 곧바로 다시 가져가면 FCM 이 잠깐 아플 때
-- 다섯 번을 몇 초 만에 다 써 버리고 영영 못 보낸다.
create or replace function public.push_claim_batch(p_limit int default 100)
returns table (
  id bigint,
  user_id uuid,
  kind text,
  args jsonb,
  link text,
  tokens jsonb
)
language sql
security definer
set search_path = public
as $$
  with picked as (
    select o.id from public.push_outbox o
     where o.sent_at is null and o.attempts < 5
       and (o.claimed_at is null or o.claimed_at < now() - interval '2 minutes')
     order by o.id
     limit least(greatest(coalesce(p_limit, 100), 1), 500)
     for update skip locked
  ), bumped as (
    update public.push_outbox o
       set attempts = o.attempts + 1,
           claimed_at = now()
      from picked
     where o.id = picked.id
    returning o.id, o.user_id, o.kind, o.args, o.link
  )
  select b.id, b.user_id, b.kind, b.args, b.link,
         coalesce((select jsonb_agg(jsonb_build_object('token', t.token, 'locale', t.locale))
                     from public.push_tokens t where t.user_id = b.user_id), '[]'::jsonb)
    from bumped b
$$;

-- 보냈다고 적는다(실패면 이유를 남긴다)
create or replace function public.push_mark(p_id bigint, p_sent boolean, p_error text default '')
returns void
language sql
security definer
set search_path = public
as $$
  update public.push_outbox
     set sent_at = case when p_sent then now() else sent_at end,
         last_error = left(coalesce(p_error, ''), 500)
   where id = p_id
$$;

-- 더는 받지 않는 폰(앱을 지웠거나 토큰이 바뀜)을 지운다
create or replace function public.push_forget_token(p_token text)
returns void
language sql
security definer
set search_path = public
as $$
  delete from public.push_tokens where token = p_token
$$;

revoke execute on function public.push_claim_batch(int) from public, anon, authenticated;
revoke execute on function public.push_mark(bigint, boolean, text) from public, anon, authenticated;
revoke execute on function public.push_forget_token(text) from public, anon, authenticated;
do $$
begin
  -- 로컬 검사용 Postgres 에는 service_role 이 없을 수 있다
  if exists (select 1 from pg_roles where rolname = 'service_role') then
    grant execute on function public.push_claim_batch(int) to service_role;
    grant execute on function public.push_mark(bigint, boolean, text) to service_role;
    grant execute on function public.push_forget_token(text) to service_role;
    grant select, update on public.push_outbox to service_role;
  end if;
end $$;
