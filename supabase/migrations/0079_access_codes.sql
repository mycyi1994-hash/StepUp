-- ════════════════════════════════════════════════════════════════════
--  0079 — 접속 코드: 비공개 베타(코드로만) · 공개 베타(사전 예약자 중 일부 + 코드) (2026-10-28)
--
--  사용자 결정: "closed beta 는 only access code 로만, open beta 는 사전예약한 사람들 중 일부 · 코드 생성 · 차단 · 사용 여부 다 체크 ·
--  코드로 들어온 사람이 하루에 몇 개씩 코드 발행".
--
--  문(access_mode — 운영 값, admin_access_mode_set 으로 바꾼다):
--    OPEN         누구나(지금 기본값 — 배포하자마자 막지 않는다)
--    CLOSED_BETA  접속 권한이 있는 계정만(코드를 넣었거나 · 운영자가 준 계정 · 배포 때 있던 계정)
--    OPEN_BETA    위 + 운영자가 고른 사전 예약 이메일로 구글 로그인한 계정(처음 상태를 물을 때 저절로 권한)
--
--  막는 곳은 서버다 — 앱 화면만 막으면 앱을 고쳐 지나갈 수 있다. PostgREST 의 요청 전 함수(pgrst.db_pre_request =
--  public.access_gate)가 로그인한 사람의 모든 API 요청을 보고, 권한이 없으면 403 access_required 로 막는다.
--  막지 않는 것: 로그인 안 한 요청(홈페이지 사전 등록 · 뉴스), 어테스터 · 운영자, 접속 코드 함수(access_*), 계정 삭제(account_delete).
--  요청 전 함수가 뜻밖의 오류를 내면 막지 않는다(앱 전체가 멈추지 않게).
--
--  코드:
--    · 운영자 코드(ADMIN) — 여러 명이 쓰는 공용(예: 100명) 또는 1인용, 만료일 · 메모. admin_access_codes_create
--    · 사용자 코드(USER) — 권한이 있는 사람(코드로 들어옴 · 운영자가 줌 · 배포 때 있던 계정)이 하루 N개(access_user_daily_codes),
--      한 번만 쓰이고 access_user_code_days 일 뒤 만료. access_code_issue
--    · 차단 — admin_access_code_revoke: 아직 안 쓴 자리는 막고, 원하면 그 코드로 들어온 사람의 권한과 그 사람이 만든 코드도 함께 막는다
--    · 확인 — admin_access_codes(목록 · 남은 자리 · 상태) · admin_access_code_uses(누가 언제 썼나) · admin_access_users(권한 명단)
--  코드는 대소문자 · 띄어쓰기 · '-' 를 가리지 않는다(저장은 대문자 8자, 보일 때 XXXX-XXXX). 헷갈리는 글자(0 O 1 I L)는 쓰지 않는다.
--  틀린 코드는 한 사람 1시간에 10번까지(맞혀 보기 막기).
--
--  운영자 함수는 앱의 운영자 계정(app_admins) 또는 Supabase SQL Editor(로그인 없이 postgres 로)에서 부른다.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('access_mode', '"OPEN"'::jsonb),
  ('access_user_daily_codes', '3'::jsonb),
  ('access_user_code_days', '14'::jsonb),
  ('access_issue_sources', '["CODE", "ADMIN", "LEGACY"]'::jsonb)
on conflict (key) do nothing;

-- ══════════════════════════════════════════════════════════════════
-- 표 — 앱은 직접 읽지도 쓰지도 못한다(아래 함수로만)
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.access_codes (
  code text primary key check (code ~ '^[A-Z0-9]{6,32}$'),
  kind text not null check (kind in ('ADMIN', 'USER')),
  created_by uuid references auth.users on delete set null,
  max_uses int not null default 1 check (max_uses between 1 and 100000),
  used_count int not null default 0 check (used_count >= 0),
  expires_at timestamptz,
  revoked_at timestamptz,
  revoked_reason text,
  note text check (note is null or length(note) <= 200),
  created_at timestamptz not null default now()
);
create index if not exists access_codes_creator on economy.access_codes (created_by, created_at desc);

create table if not exists economy.access_grants (
  user_id uuid primary key references auth.users on delete cascade,
  source text not null check (source in ('CODE', 'WAITLIST', 'LEGACY', 'ADMIN')),
  code text references economy.access_codes on delete set null,
  granted_at timestamptz not null default now(),
  revoked_at timestamptz,
  revoked_reason text
);
create index if not exists access_grants_code on economy.access_grants (code);

-- 공개 베타로 고른 사전 예약 이메일
create table if not exists economy.access_open_beta (
  email text primary key check (email = lower(btrim(email))),
  invited_at timestamptz not null default now(),
  note text
);

-- 틀린 코드 시도(맞혀 보기 막기)
create table if not exists economy.access_attempts (
  user_id uuid not null references auth.users on delete cascade,
  at timestamptz not null default now()
);
create index if not exists access_attempts_user on economy.access_attempts (user_id, at desc);

do $$
declare t text;
begin
  foreach t in array array['economy.access_codes', 'economy.access_grants', 'economy.access_open_beta', 'economy.access_attempts'] loop
    execute format('revoke all on %s from public', t);
    begin
      execute format('revoke all on %s from anon, authenticated', t);
    exception when undefined_object then null;
    end;
  end loop;
end $$;

-- 배포 때 이미 있던 계정은 그대로 쓴다(한 번만 — 다시 붙여넣어도 그 뒤에 가입한 사람을 넣지 않는다)
do $$ begin
  if not exists (select 1 from public.economy_settings where key = 'access_v1_applied') then
    insert into economy.access_grants (user_id, source)
    select u.id, 'LEGACY' from auth.users u
    on conflict (user_id) do nothing;
    insert into public.economy_settings (key, value) values ('access_v1_applied', 'true'::jsonb)
    on conflict (key) do nothing;
  end if;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 도움 함수
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.access_mode() returns text
  language sql stable security definer set search_path = public, economy as $$
  select case when m in ('OPEN', 'CLOSED_BETA', 'OPEN_BETA') then m else 'OPEN' end
    from (select coalesce(economy.setting('access_mode') #>> '{}', 'OPEN') as m) x
$$;
revoke all on function economy.access_mode() from public;

-- 대소문자 · 띄어쓰기 · '-' 를 가리지 않는다
create or replace function economy.access_norm(p_code text) returns text
  language sql immutable as $$ select upper(regexp_replace(coalesce(p_code, ''), '[^A-Za-z0-9]', '', 'g')) $$;

create or replace function economy.access_pretty(p_code text) returns text
  language sql immutable as $$
  select case when length(p_code) = 8 then substr(p_code, 1, 4) || '-' || substr(p_code, 5, 4) else p_code end
$$;

-- 새 코드 8자 — 헷갈리는 글자(0 O 1 I L) 없이 31자에서. 겹치면 다시
create or replace function economy.access_new_code() returns text
language plpgsql volatile security definer set search_path = public, economy as $$
declare
  v_alphabet text := 'ABCDEFGHJKMNPQRSTUVWXYZ23456789';
  v_b bytea;
  v_code text;
  i int;
begin
  loop
    v_b := economy.random_bytes32();
    v_code := '';
    for i in 0 .. 7 loop
      v_code := v_code || substr(v_alphabet, (get_byte(v_b, i) % length(v_alphabet)) + 1, 1);
    end loop;
    exit when not exists (select 1 from economy.access_codes c where c.code = v_code);
  end loop;
  return v_code;
end $$;
revoke all on function economy.access_new_code() from public;

-- 이 계정이 쓸 수 있는가 — 문이 열렸거나, 권한이 살아 있거나, 운영자 · 어테스터
create or replace function economy.access_ok(p_user uuid) returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select economy.access_mode() = 'OPEN'
      or exists (select 1 from economy.access_grants g where g.user_id = p_user and g.revoked_at is null)
      or exists (select 1 from public.app_admins a where a.user_id = p_user)
      -- 어테스터 계정 칸이 비어 있으면(null) 비교가 null 이 되어 전체가 null 이 된다 — false 로
      or coalesce(p_user::text = (economy.setting('attester_user_id') #>> '{}'), false)
$$;
revoke all on function economy.access_ok(uuid) from public;

-- 공개 베타 — 고른 사전 예약 이메일로 로그인한 계정에 권한을 준다(차단된 권한은 되살리지 않는다)
create or replace function economy.access_waitlist_grant(p_user uuid) returns boolean
language plpgsql security definer set search_path = public, economy as $$
begin
  if economy.access_mode() <> 'OPEN_BETA' or p_user is null then
    return false;
  end if;
  if exists (select 1 from economy.access_grants g where g.user_id = p_user) then
    return false;
  end if;
  if not exists (select 1 from auth.users u join economy.access_open_beta o on o.email = lower(btrim(u.email))
                  where u.id = p_user) then
    return false;
  end if;
  insert into economy.access_grants (user_id, source) values (p_user, 'WAITLIST') on conflict (user_id) do nothing;
  return true;
end $$;
revoke all on function economy.access_waitlist_grant(uuid) from public;

-- 운영자 — 앱의 운영자 계정, 또는 SQL Editor(로그인 없이 postgres)
create or replace function economy.access_admin_guard() returns void
language plpgsql stable security definer set search_path = public, economy as $$
begin
  if public.is_admin() then
    return;
  end if;
  if auth.uid() is null and session_user in ('postgres', 'supabase_admin') then
    return;
  end if;
  raise exception '운영자만 할 수 있습니다' using errcode = '42501';
end $$;
revoke all on function economy.access_admin_guard() from public;

create or replace function economy.access_code_status(c economy.access_codes) returns text
  language sql stable as $$
  select case
    when c.revoked_at is not null then 'REVOKED'
    when c.used_count >= c.max_uses then 'USED'
    when c.expires_at is not null and c.expires_at <= now() then 'EXPIRED'
    else 'OPEN' end
$$;

-- ══════════════════════════════════════════════════════════════════
-- 요청 전 문지기 — PostgREST 가 로그인한 사람의 모든 요청 앞에서 부른다
-- ══════════════════════════════════════════════════════════════════
create or replace function public.access_gate() returns void
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_block boolean := false;
  v_path text;
  v_uid uuid;
begin
  begin
    if economy.access_mode() <> 'OPEN' then
      v_uid := auth.uid();
      v_path := coalesce(current_setting('request.path', true), '');
      if v_uid is not null
         and coalesce(auth.jwt() ->> 'role', '') = 'authenticated'
         and v_path !~ '/rpc/(access_[a-z_]+|account_delete|is_admin)$'
         and not coalesce(economy.access_ok(v_uid), true) then
        v_block := true;
      end if;
    end if;
  exception when others then
    -- 문지기가 고장 나도 앱 전체를 멈추지 않는다
    v_block := false;
  end;
  if v_block then
    raise exception 'access_required' using errcode = 'PT403',
      detail = '접속 코드가 필요합니다', hint = 'access_required';
  end if;
end $$;
revoke all on function public.access_gate() from public;
do $$
declare r text;
begin
  foreach r in array array['anon', 'authenticated', 'service_role', 'stepup_attester'] loop
    begin
      execute format('grant execute on function public.access_gate() to %I', r);
    exception when undefined_object then null;
    end;
  end loop;
end $$;

-- PostgREST 에 문지기를 건다(Supabase 의 authenticator 역할). 이 역할이 없는 곳(로컬 검사)에서는 건너뛴다
do $$ begin
  execute 'alter role authenticator set pgrst.db_pre_request to ''public.access_gate''';
  perform pg_notify('pgrst', 'reload config');
exception when others then
  raise notice 'access_gate 를 걸지 못했습니다(검사 환경이면 정상): %', sqlerrm;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 앱 — 문 상태 · 코드 넣기 · 내 코드 만들기 · 내가 만든 코드
-- ══════════════════════════════════════════════════════════════════
-- 로그인 전에도 — 지금 문이 어떤가(로그인 화면의 안내용)
create or replace function public.access_mode() returns text
  language sql stable security definer set search_path = public, economy as $$ select economy.access_mode() $$;
revoke all on function public.access_mode() from public;
grant execute on function public.access_mode() to anon, authenticated;

/*
 * 내 접속 상태. 공개 베타면 고른 사전 예약 이메일 계정에 여기서 권한을 준다.
 * { mode, granted, source, revoked, can_issue, daily_limit, issued_today }
 */
create or replace function public.access_status()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  g economy.access_grants;
  v_limit int := greatest(coalesce(economy.setting_num('access_user_daily_codes'), 0)::int, 0);
  v_today int;
  v_can boolean;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.access_waitlist_grant(v_user);
  select * into g from economy.access_grants x where x.user_id = v_user;
  select count(*)::int into v_today from economy.access_codes c
   where c.created_by = v_user and c.kind = 'USER' and c.created_at >= economy.today_start();
  v_can := economy.access_mode() <> 'OPEN' and v_limit > 0
           and g.user_id is not null and g.revoked_at is null
           and coalesce(economy.setting('access_issue_sources'), '[]'::jsonb) ? g.source;
  return jsonb_build_object(
    'mode', economy.access_mode(),
    'granted', economy.access_ok(v_user),
    'source', g.source,
    'revoked', g.revoked_at is not null,
    'can_issue', v_can,
    'daily_limit', v_limit,
    'issued_today', v_today);
end $$;
revoke all on function public.access_status() from public, anon;
grant execute on function public.access_status() to authenticated;

/*
 * 코드 넣기. { ok: true } 또는 { ok: false, error: not_found | revoked | expired | used_up | too_many | blocked }
 * 이미 권한이 있으면 코드를 쓰지 않고 ok. 차단된 계정은 코드로 되살리지 못한다(운영자만).
 */
create or replace function public.access_redeem(p_code text)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_code text := economy.access_norm(p_code);
  c economy.access_codes;
  g economy.access_grants;
  v_fails int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('access:' || v_user::text));
  select * into g from economy.access_grants x where x.user_id = v_user;
  if found and g.revoked_at is not null then
    return jsonb_build_object('ok', false, 'error', 'blocked');
  end if;
  if found then
    return jsonb_build_object('ok', true, 'already', true);
  end if;
  select count(*)::int into v_fails from economy.access_attempts a
   where a.user_id = v_user and a.at > now() - interval '1 hour';
  if v_fails >= 10 then
    return jsonb_build_object('ok', false, 'error', 'too_many');
  end if;

  select * into c from economy.access_codes x where x.code = v_code for update;
  if not found then
    insert into economy.access_attempts (user_id) values (v_user);
    return jsonb_build_object('ok', false, 'error', 'not_found');
  end if;
  if c.revoked_at is not null then
    return jsonb_build_object('ok', false, 'error', 'revoked');
  end if;
  if c.expires_at is not null and c.expires_at <= now() then
    return jsonb_build_object('ok', false, 'error', 'expired');
  end if;
  if c.used_count >= c.max_uses then
    return jsonb_build_object('ok', false, 'error', 'used_up');
  end if;
  if c.created_by = v_user then
    return jsonb_build_object('ok', false, 'error', 'not_found');
  end if;

  update economy.access_codes set used_count = used_count + 1 where code = c.code;
  insert into economy.access_grants (user_id, source, code) values (v_user, 'CODE', c.code);
  return jsonb_build_object('ok', true, 'already', false);
end $$;
revoke all on function public.access_redeem(text) from public, anon;
grant execute on function public.access_redeem(text) to authenticated;

/* 내 코드 하나 만들기 — 권한이 있는 사람만, 하루 N개. { code, expires_at } 또는 { error: not_allowed | daily_limit } */
create or replace function public.access_code_issue()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_status jsonb;
  v_code text;
  v_exp timestamptz := now() + make_interval(days => greatest(coalesce(economy.setting_num('access_user_code_days'), 14)::int, 1));
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('access:' || v_user::text));
  v_status := public.access_status();
  if not (v_status ->> 'can_issue')::boolean then
    return jsonb_build_object('error', 'not_allowed');
  end if;
  if (v_status ->> 'issued_today')::int >= (v_status ->> 'daily_limit')::int then
    return jsonb_build_object('error', 'daily_limit');
  end if;
  v_code := economy.access_new_code();
  insert into economy.access_codes (code, kind, created_by, max_uses, expires_at)
  values (v_code, 'USER', v_user, 1, v_exp);
  return jsonb_build_object('code', economy.access_pretty(v_code), 'expires_at', v_exp);
end $$;
revoke all on function public.access_code_issue() from public, anon;
grant execute on function public.access_code_issue() to authenticated;

/* 내가 만든 코드 — 쓰였는지만 보인다(누가 썼는지는 보이지 않는다) */
create or replace function public.my_access_codes(p_limit int default 50)
returns table (code text, status text, created_at timestamptz, expires_at timestamptz, used_at timestamptz)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select economy.access_pretty(c.code), economy.access_code_status(c), c.created_at, c.expires_at,
         (select min(g.granted_at) from economy.access_grants g where g.code = c.code)
    from economy.access_codes c
   where c.created_by = v_user and c.kind = 'USER'
   order by c.created_at desc
   limit greatest(1, least(coalesce(p_limit, 50), 200));
end $$;
revoke all on function public.my_access_codes(int) from public, anon;
grant execute on function public.my_access_codes(int) to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 운영자
-- ══════════════════════════════════════════════════════════════════
-- 문 바꾸기: OPEN · CLOSED_BETA · OPEN_BETA
create or replace function public.admin_access_mode_set(p_mode text)
returns text
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.access_admin_guard();
  if p_mode not in ('OPEN', 'CLOSED_BETA', 'OPEN_BETA') then
    raise exception 'OPEN · CLOSED_BETA · OPEN_BETA 중 하나' using errcode = '22023';
  end if;
  update public.economy_settings set value = to_jsonb(p_mode), updated_at = now() where key = 'access_mode';
  perform public.admin_log('access_mode', p_mode, '{}'::jsonb);
  return p_mode;
end $$;

-- 코드 만들기 — p_count 개, 각각 p_max_uses 명까지(공용이면 크게), p_expires_days 일 뒤 만료(없으면 계속)
create or replace function public.admin_access_codes_create(
  p_count int default 1, p_max_uses int default 1, p_expires_days int default null, p_note text default null)
returns table (code text, max_uses int, expires_at timestamptz)
language plpgsql security definer set search_path = public, economy as $$
declare
  v_code text;
  i int;
  v_exp timestamptz := case when p_expires_days is not null then now() + make_interval(days => p_expires_days) end;
begin
  perform economy.access_admin_guard();
  if coalesce(p_count, 0) not between 1 and 1000 or coalesce(p_max_uses, 0) not between 1 and 100000 then
    raise exception '개수는 1 ~ 1000, 인원은 1 ~ 100000' using errcode = '22023';
  end if;
  for i in 1 .. p_count loop
    v_code := economy.access_new_code();
    insert into economy.access_codes (code, kind, created_by, max_uses, expires_at, note)
    values (v_code, 'ADMIN', auth.uid(), p_max_uses, v_exp, p_note);
    code := economy.access_pretty(v_code);
    max_uses := p_max_uses;
    expires_at := v_exp;
    return next;
  end loop;
  perform public.admin_log('access_codes_create', p_count::text,
                           jsonb_build_object('max_uses', p_max_uses, 'expires_days', p_expires_days, 'note', p_note));
end $$;

-- 정한 글자로 만들기(예: STEPUPBETA) — 공용 행사 코드
create or replace function public.admin_access_code_named(
  p_code text, p_max_uses int default 100, p_expires_days int default null, p_note text default null)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare v_code text := economy.access_norm(p_code);
begin
  perform economy.access_admin_guard();
  if v_code !~ '^[A-Z0-9]{6,32}$' then
    raise exception '코드는 영문 · 숫자 6 ~ 32자' using errcode = '22023';
  end if;
  insert into economy.access_codes (code, kind, created_by, max_uses, expires_at, note)
  values (v_code, 'ADMIN', auth.uid(), p_max_uses,
          case when p_expires_days is not null then now() + make_interval(days => p_expires_days) end, p_note);
  perform public.admin_log('access_code_named', v_code, jsonb_build_object('max_uses', p_max_uses));
  return economy.access_pretty(v_code);
end $$;

-- 코드 차단 — 남은 자리를 막는다. p_revoke_users 면 그 코드로 들어온 사람의 권한과 그 사람들이 만든 안 쓴 코드도 막는다
create or replace function public.admin_access_code_revoke(p_code text, p_reason text default null, p_revoke_users boolean default false)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_code text := economy.access_norm(p_code);
  v_users int := 0;
  v_codes int := 0;
begin
  perform economy.access_admin_guard();
  update economy.access_codes set revoked_at = coalesce(revoked_at, now()), revoked_reason = coalesce(p_reason, revoked_reason)
   where code = v_code;
  if not found then
    raise exception '없는 코드입니다: %', p_code using errcode = '22023';
  end if;
  if coalesce(p_revoke_users, false) then
    with r as (
      update economy.access_grants g set revoked_at = now(), revoked_reason = coalesce(p_reason, 'code revoked')
       where g.code = v_code and g.revoked_at is null
      returning g.user_id)
    select count(*)::int into v_users from r;
    with r as (
      update economy.access_codes c set revoked_at = now(), revoked_reason = coalesce(p_reason, 'issuer revoked')
       where c.kind = 'USER' and c.revoked_at is null and c.used_count < c.max_uses
         and c.created_by in (select g.user_id from economy.access_grants g where g.code = v_code)
      returning c.code)
    select count(*)::int into v_codes from r;
  end if;
  perform public.admin_log('access_code_revoke', v_code,
                           jsonb_build_object('reason', p_reason, 'users', v_users, 'codes', v_codes));
  return jsonb_build_object('code', economy.access_pretty(v_code), 'users_revoked', v_users, 'codes_revoked', v_codes);
end $$;

-- 코드 목록 — p_kind(ADMIN · USER) · p_status(OPEN · USED · EXPIRED · REVOKED)로 거르기
create or replace function public.admin_access_codes(p_kind text default null, p_status text default null, p_limit int default 200)
returns table (code text, kind text, status text, used_count int, max_uses int, creator_email text,
               note text, created_at timestamptz, expires_at timestamptz, revoked_at timestamptz, revoked_reason text)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.access_admin_guard();
  return query
  select economy.access_pretty(c.code), c.kind, economy.access_code_status(c), c.used_count, c.max_uses,
         u.email::text, c.note, c.created_at, c.expires_at, c.revoked_at, c.revoked_reason
    from economy.access_codes c
    left join auth.users u on u.id = c.created_by
   where (p_kind is null or c.kind = p_kind)
     and (p_status is null or economy.access_code_status(c) = p_status)
   order by c.created_at desc
   limit greatest(1, least(coalesce(p_limit, 200), 5000));
end $$;

-- 코드 하나를 누가 언제 썼나
create or replace function public.admin_access_code_uses(p_code text)
returns table (email text, granted_at timestamptz, revoked_at timestamptz, codes_issued int)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.access_admin_guard();
  return query
  select u.email::text, g.granted_at, g.revoked_at,
         (select count(*)::int from economy.access_codes c where c.created_by = g.user_id and c.kind = 'USER')
    from economy.access_grants g
    join auth.users u on u.id = g.user_id
   where g.code = economy.access_norm(p_code)
   order by g.granted_at;
end $$;

-- 권한 명단 — 어떻게 들어왔나(코드 · 사전 예약 · 운영자 · 배포 때) · 누가 만든 코드였나 · 만든 코드 수
create or replace function public.admin_access_users(p_limit int default 500)
returns table (email text, source text, code text, code_creator text, granted_at timestamptz,
               revoked_at timestamptz, codes_issued int, codes_used int)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.access_admin_guard();
  return query
  select u.email::text, g.source, economy.access_pretty(g.code), cu.email::text, g.granted_at, g.revoked_at,
         (select count(*)::int from economy.access_codes c where c.created_by = g.user_id and c.kind = 'USER'),
         (select coalesce(sum(c.used_count), 0)::int from economy.access_codes c where c.created_by = g.user_id and c.kind = 'USER')
    from economy.access_grants g
    join auth.users u on u.id = g.user_id
    left join economy.access_codes c0 on c0.code = g.code
    left join auth.users cu on cu.id = c0.created_by
   order by g.granted_at desc
   limit greatest(1, least(coalesce(p_limit, 500), 10000));
end $$;

-- 계정에 직접 권한 주기 · 막기(이메일로)
create or replace function public.admin_access_grant(p_email text)
returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid;
begin
  perform economy.access_admin_guard();
  select u.id into v_user from auth.users u where lower(u.email) = lower(btrim(p_email));
  if v_user is null then
    raise exception '그 이메일로 가입한 계정이 없습니다(먼저 앱에서 로그인해야 합니다)' using errcode = '22023';
  end if;
  insert into economy.access_grants (user_id, source) values (v_user, 'ADMIN')
  on conflict (user_id) do update set revoked_at = null, revoked_reason = null;
  perform public.admin_log('access_grant', p_email, '{}'::jsonb);
  return true;
end $$;

create or replace function public.admin_access_revoke_user(p_email text, p_reason text default null)
returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid;
begin
  perform economy.access_admin_guard();
  select u.id into v_user from auth.users u where lower(u.email) = lower(btrim(p_email));
  if v_user is null then
    raise exception '그 이메일로 가입한 계정이 없습니다' using errcode = '22023';
  end if;
  insert into economy.access_grants (user_id, source, revoked_at, revoked_reason) values (v_user, 'ADMIN', now(), p_reason)
  on conflict (user_id) do update set revoked_at = now(), revoked_reason = p_reason;
  -- 그 사람이 만든 안 쓴 코드도 막는다
  update economy.access_codes c set revoked_at = now(), revoked_reason = coalesce(p_reason, 'issuer revoked')
   where c.created_by = v_user and c.kind = 'USER' and c.revoked_at is null and c.used_count < c.max_uses;
  perform public.admin_log('access_revoke_user', p_email, jsonb_build_object('reason', p_reason));
  return true;
end $$;

-- 공개 베타 — 사전 예약자 중 고르기. p_emails 를 주면 그 이메일만, 아니면 먼저 등록한 순서로 p_count 명
create or replace function public.admin_open_beta_invite(p_count int default null, p_emails text[] default null, p_note text default null)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare v_n int;
begin
  perform economy.access_admin_guard();
  if p_emails is not null then
    with ins as (
      insert into economy.access_open_beta (email, note)
      select lower(btrim(e)), p_note from unnest(p_emails) e
       where lower(btrim(e)) in (select w.email from public.waitlist_entries w where w.status = 'registered')
      on conflict (email) do nothing
      returning 1)
    select count(*)::int into v_n from ins;
  else
    if coalesce(p_count, 0) not between 1 and 100000 then
      raise exception '인원은 1 ~ 100000' using errcode = '22023';
    end if;
    with ins as (
      insert into economy.access_open_beta (email, note)
      select w.email, p_note from public.waitlist_entries w
       where w.status = 'registered' and not exists (select 1 from economy.access_open_beta o where o.email = w.email)
       order by w.created_at
       limit p_count
      on conflict (email) do nothing
      returning 1)
    select count(*)::int into v_n from ins;
  end if;
  perform public.admin_log('open_beta_invite', coalesce(p_count, cardinality(p_emails))::text, jsonb_build_object('added', v_n));
  return v_n;
end $$;

-- 공개 베타로 고른 사람 · 들어왔는가
create or replace function public.admin_open_beta_list(p_limit int default 500)
returns table (email text, invited_at timestamptz, joined boolean, note text)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.access_admin_guard();
  return query
  select o.email, o.invited_at,
         exists (select 1 from auth.users u join economy.access_grants g on g.user_id = u.id where lower(u.email) = o.email),
         o.note
    from economy.access_open_beta o
   order by o.invited_at desc
   limit greatest(1, least(coalesce(p_limit, 500), 100000));
end $$;

do $$
declare f text;
begin
  foreach f in array array[
    'public.admin_access_mode_set(text)', 'public.admin_access_codes_create(int, int, int, text)',
    'public.admin_access_code_named(text, int, int, text)', 'public.admin_access_code_revoke(text, text, boolean)',
    'public.admin_access_codes(text, text, int)', 'public.admin_access_code_uses(text)', 'public.admin_access_users(int)',
    'public.admin_access_grant(text)', 'public.admin_access_revoke_user(text, text)',
    'public.admin_open_beta_invite(int, text[], text)', 'public.admin_open_beta_list(int)'] loop
    execute format('revoke all on function %s from public, anon', f);
    execute format('grant execute on function %s to authenticated', f);
  end loop;
end $$;
