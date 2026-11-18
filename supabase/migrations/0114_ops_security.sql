-- ════════════════════════════════════════════════════════════════════
--  0114 — 운영자 페이지 보안: 세션 · 키 기한 · 로그인 잠금 · 큰 일 다시 확인 · 하루 지급 한도 (2026-11-18)
--
--  사용자 "운영자 페이지 보안 더" — 방법은 맡겼다. 예전(0106)에는 키 하나를 모든 요청의 본문에 실어 보냈고,
--  그 키를 가진 사람은 누구에게나 아이템을 줄 수 있었다. 이제 서버가 다섯 겹을 지킨다:
--
--  · 세션: public.ops_login(키) 가 무작위 세션 번호를 돌려준다(서버에는 해시만). 8시간까지 · 30분 동안 쓰지 않으면 끝,
--    쓸 때마다 30분이 다시 시작된다. economy.ops_actor(번호) 는 이제 세션 번호만 받는다(키를 그대로 보내면 거절).
--    public.ops_logout(번호) 로 끝낸다. 키를 막거나 기한이 지나면 그 키의 세션도 바로 끝난다.
--  · 키 기한: economy.ops_keys.expires_at(기본 30일). economy.ops_key_new('이름', 날 수) — 예전 한 칸 호출은 30일.
--    이 마이그레이션 전에 만든 키는 처음 적용할 때 한 번 지금부터 30일(칸이 비지 않게 되어 다시 붙여넣어도 그대로).
--  · 로그인 잠금: 같은 곳(IP — 서버만 아는 소금을 친 해시, 원문은 남기지 않는다 · 0082 waitlist_client 와 같은 머리글)에서
--    10분에 5번 틀리면 그 곳을 15분, 모든 곳을 합쳐 10분에 30번 틀리면 모든 로그인을 15분 막는다. 막힌 동안의 시도는
--    키를 보지도 쌓지도 않는다. 큰 일의 다시 확인에서 틀린 키도 같은 실패로 센다(한 세션에서 세 번 틀리면 그 세션은 끝).
--  · 다시 확인: 전체 지급 · 200명 넘는 지급 · 3배 이상이거나 48시간 넘는 보상 이벤트 · 지급 되돌리기는 그 요청에
--    운영자 키를 한 번 더(p_confirm) — 이 세션을 연 키여야 한다. 요청 번호(같은 일은 한 번만)는 그대로다.
--  · 하루 지급 한도: 키마다 한국 날짜 하루 아이템 5,000개(받는 사람 수 × 개수)까지. 넘는 지급은 통째로 거절한다.
--  값은 economy_settings(ops_*) — 배포 없이 바꾼다(admin_economy_set). 모두 public.admin_audit 에 남는다(키 · 세션 값 없이).
--
--  거절은 예외가 아니라 {ok:false, code, message} 로 답한다 — 예외는 실패 기록까지 되돌려 잠금이 세지 못한다
--  (0082 대기 명단과 같은 까닭). 끝난 · 없는 세션만 예전처럼 예외(42501 → PostgREST 401)다.
--
--  가장 센 겹은 Cloudflare Access(사용자가 대시보드에서) — docs/items-2026-11-17.md "운영자 페이지 보안".
--
--  다시 붙여넣기: 0106 이 예전 칸 수의 함수를 다시 만들므로 여기서 매번 먼저 지우고(drop) 새 칸 수로 만든다.
--  ops_overview · ops_user_find 는 세션 시각을 고치므로 stable 을 뗐다(PostgREST 는 stable 함수를 읽기 전용으로 부른다).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('ops_session_hours',       '8'::jsonb),
  ('ops_session_idle_min',    '30'::jsonb),
  ('ops_login_window_min',    '10'::jsonb),
  ('ops_login_client_fails',  '5'::jsonb),
  ('ops_login_global_fails',  '30'::jsonb),
  ('ops_login_lock_min',      '15'::jsonb),
  ('ops_key_daily_units',     '5000'::jsonb),
  ('ops_confirm_recipients',  '200'::jsonb),
  ('ops_confirm_mult',        '3'::jsonb),
  ('ops_confirm_hours',       '48'::jsonb)
on conflict (key) do nothing;

-- ── 키 기한 ─────────────────────────────────────────────────────────
alter table economy.ops_keys add column if not exists expires_at timestamptz;
-- 이 마이그레이션 전에 만든 키 — 처음 적용할 때 한 번(아래에서 칸이 비지 않게 되므로 다시 붙여넣어도 바뀌지 않는다)
update economy.ops_keys set expires_at = now() + interval '30 days' where expires_at is null;
alter table economy.ops_keys alter column expires_at set default (now() + interval '30 days');
alter table economy.ops_keys alter column expires_at set not null;

comment on table economy.ops_keys is
  '운영자 페이지 키(해시만). economy.ops_key_new(이름, 날 수 — 기본 30) 로 만들고 revoked_at 으로 막는다. 기한은 expires_at(0106 · 0114).';

-- 같은 키 값의 해시 — 키 · 세션 번호 모두(앞뒤 공백은 뺀다. 키는 16진수라 원래 공백이 없다)
create or replace function economy.ops_hash(p_secret text) returns text
language sql immutable set search_path = '' as $$
  select encode(sha256(convert_to(btrim(coalesce(p_secret, '')), 'UTF8')), 'hex')
$$;
revoke all on function economy.ops_hash(text) from public;

-- 운영 값(economy_settings) — 없으면 기본값
create or replace function economy.ops_num(p_key text, p_default numeric) returns numeric
language sql stable security definer set search_path = '' as $$
  select coalesce(economy.setting_num(p_key), p_default)
$$;
revoke all on function economy.ops_num(text, numeric) from public;

-- 키 만들기 — 날 수(1 ~ 365). 한 번만 보이는 키를 돌려준다
create or replace function economy.ops_key_new(p_label text, p_days int) returns text
language plpgsql security definer set search_path = '' as $$
declare v_token text := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
begin
  if p_days is null or p_days < 1 or p_days > 365 then
    raise exception '키 기한은 1 ~ 365일입니다' using errcode = '22023';
  end if;
  insert into economy.ops_keys (token_hash, label, expires_at)
  values (economy.ops_hash(v_token), coalesce(nullif(btrim(p_label), ''), 'ops'), now() + make_interval(days => p_days));
  return v_token;
end $$;
revoke all on function economy.ops_key_new(text, int) from public;

-- 예전 한 칸 호출(select economy.ops_key_new('이름')) — 30일. 두 칸 함수에 기본값을 두지 않는 것은 한 칸 호출이 둘 중 어느 것인지 모호해지지 않게
create or replace function economy.ops_key_new(p_label text) returns text
language sql security definer set search_path = '' as $$
  select economy.ops_key_new(p_label, 30)
$$;
revoke all on function economy.ops_key_new(text) from public;

-- ── 부른 곳(IP) — 서버만 아는 소금을 친 해시. 원문은 남기지 않는다 ──
create table if not exists economy.ops_secret (
  id boolean primary key default true check (id),
  salt text not null
);
revoke all on economy.ops_secret from public, anon, authenticated;
insert into economy.ops_secret (salt)
values (replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', ''))
on conflict (id) do nothing;

comment on table economy.ops_secret is '운영자 로그인 실패를 곳마다 셀 때 IP 에 치는 소금(서버만). 원문 IP 는 어디에도 남기지 않는다(0114).';

-- 머리글은 0082 economy.waitlist_client 와 같다(Cloudflare 가 넣는 cf-connecting-ip 먼저). 머리글이 없으면(SQL Editor · 검사) 'unknown'
create or replace function economy.ops_client() returns text
language plpgsql stable security definer set search_path = '' as $$
declare
  v_headers jsonb;
  v_ip text;
begin
  begin
    v_headers := nullif(current_setting('request.headers', true), '')::jsonb;
  exception when others then
    v_headers := null;
  end;
  v_ip := coalesce(nullif(btrim(v_headers ->> 'cf-connecting-ip'), ''),
                   nullif(btrim(split_part(v_headers ->> 'x-forwarded-for', ',', 1)), ''),
                   nullif(btrim(v_headers ->> 'x-real-ip'), ''));
  if v_ip is null then
    return 'unknown';
  end if;
  return encode(sha256(convert_to((select s.salt from economy.ops_secret s) || ':ops:' || v_ip, 'UTF8')), 'hex');
end $$;
revoke all on function economy.ops_client() from public;

-- ── 세션 ────────────────────────────────────────────────────────────
create table if not exists economy.ops_sessions (
  id bigint generated always as identity primary key,
  -- 세션 번호의 해시(번호는 로그인할 때 한 번 돌려주고 남기지 않는다)
  token_hash text not null unique,
  -- 이 세션을 연 키 — 키를 막거나 기한이 지나면 세션도 끝난다
  key_hash text not null references economy.ops_keys (token_hash) on delete cascade,
  label text not null,
  client text not null default 'unknown',
  created_at timestamptz not null default now(),
  expires_at timestamptz not null,
  last_used_at timestamptz not null default now(),
  ended_at timestamptz,
  -- 'logout' 로그아웃 · 'confirm' 다시 확인을 세 번 틀림
  ended_why text,
  -- 큰 일의 다시 확인에서 틀린 횟수(맞으면 0)
  confirm_fails int not null default 0
);
create index if not exists ops_sessions_key on economy.ops_sessions (key_hash);
revoke all on economy.ops_sessions from public, anon, authenticated;

comment on table economy.ops_sessions is
  '운영자 페이지 세션(번호의 해시만). 8시간 · 30분 쉬면 끝(ops_session_hours · ops_session_idle_min). 모두 끝내기: update economy.ops_sessions set ended_at = now() where ended_at is null (0114).';

-- ── 로그인 실패 · 잠금 ──────────────────────────────────────────────
create table if not exists economy.ops_login_fails (
  client text not null,
  at timestamptz not null default now(),
  -- LOGIN 로그인 · CONFIRM 큰 일의 다시 확인
  kind text not null default 'LOGIN' check (kind in ('LOGIN', 'CONFIRM'))
);
create index if not exists ops_login_fails_client on economy.ops_login_fails (client, at);
create index if not exists ops_login_fails_at on economy.ops_login_fails (at);
revoke all on economy.ops_login_fails from public, anon, authenticated;

create table if not exists economy.ops_login_locks (
  -- 곳(ops_client 해시) 또는 'ALL'(모든 곳)
  scope text primary key,
  until timestamptz not null
);
revoke all on economy.ops_login_locks from public, anon, authenticated;

comment on table economy.ops_login_fails is '운영자 로그인 · 다시 확인 실패(곳은 해시). 하루 지나면 지운다(0114).';
comment on table economy.ops_login_locks is
  '막힌 곳(해시)과 모든 곳(ALL) — 풀기: delete from economy.ops_login_locks (0114).';

-- ── 하루 지급 한도 ──────────────────────────────────────────────────
create table if not exists economy.ops_key_days (
  key_hash text not null references economy.ops_keys (token_hash) on delete cascade,
  -- 한국 날짜
  day date not null,
  -- 그날 이 키로 준 아이템(받는 사람 수 × 개수). 되돌려도 돌아오지 않는다
  units int not null default 0,
  primary key (key_hash, day)
);
revoke all on economy.ops_key_days from public, anon, authenticated;

comment on table economy.ops_key_days is '운영자 키마다 하루에 준 아이템 수 — 한도는 ops_key_daily_units(0114).';

-- 지금 막혀 있으면 풀리는 시각(p_global 이면 모든 곳 잠금도 본다)
create or replace function economy.ops_locked_until(p_client text, p_global boolean default true) returns timestamptz
language sql stable security definer set search_path = '' as $$
  select max(l.until) from economy.ops_login_locks l
   where l.until > now() and (l.scope = p_client or (p_global and l.scope = 'ALL'))
$$;
revoke all on function economy.ops_locked_until(text, boolean) from public;

create or replace function economy.ops_locked_reply(p_until timestamptz, p_code text default 'locked') returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object('ok', false, 'code', p_code, 'until', p_until,
    'message', format('운영자 키를 여러 번 틀려 잠시 막았어요. %s(한국 시간)부터 다시 할 수 있어요',
                      to_char(p_until at time zone 'Asia/Seoul', 'HH24:MI')))
$$;
revoke all on function economy.ops_locked_reply(timestamptz, text) from public;

-- 실패 하나를 적고, 곳 · 모든 곳의 기준을 넘으면 막는다. 지금 이 곳이 막혀 있으면 풀리는 시각
-- (부르는 쪽이 pg_advisory_xact_lock(hashtext('ops-login')) 으로 줄을 세운다 — 수가 정확하게)
create or replace function economy.ops_fail(p_client text, p_kind text) returns timestamptz
language plpgsql security definer set search_path = '' as $$
declare
  v_window interval := economy.ops_num('ops_login_window_min', 10)::float8 * interval '1 minute';
  v_until timestamptz := now() + economy.ops_num('ops_login_lock_min', 15)::float8 * interval '1 minute';
begin
  insert into economy.ops_login_fails (client, kind) values (p_client, p_kind);
  if (select count(*) from economy.ops_login_fails f where f.client = p_client and f.at > now() - v_window)
       >= economy.ops_num('ops_login_client_fails', 5)
     and economy.ops_locked_until(p_client, false) is null then
    insert into economy.ops_login_locks (scope, until) values (p_client, v_until)
    on conflict (scope) do update set until = excluded.until;
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_login_locked', 'client', jsonb_build_object('client', left(p_client, 12), 'until', v_until, 'kind', p_kind));
  end if;
  if (select count(*) from economy.ops_login_fails f where f.at > now() - v_window)
       >= economy.ops_num('ops_login_global_fails', 30)
     and not exists (select 1 from economy.ops_login_locks l where l.scope = 'ALL' and l.until > now()) then
    insert into economy.ops_login_locks (scope, until) values ('ALL', v_until)
    on conflict (scope) do update set until = excluded.until;
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_login_locked', 'all', jsonb_build_object('until', v_until, 'kind', p_kind));
  end if;
  return economy.ops_locked_until(p_client);
end $$;
revoke all on function economy.ops_fail(text, text) from public;

-- ── 로그인 · 로그아웃 ───────────────────────────────────────────────
-- 맞으면 { ok, session(이번 한 번만 보이는 번호), actor, expires_at, idle_min, key_expires_at }
-- 틀리면 { ok:false, code: 'wrong'(틀림 · 막힌 키) · 'expired'(기한 지남) · 'locked'(잠시 막힘), message, left? · until? }
create or replace function public.ops_login(p_key text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_client text := economy.ops_client();
  v_key economy.ops_keys;
  v_session economy.ops_sessions;
  v_reason text;
  v_until timestamptz;
  v_token text;
  v_left int;
  v_lock_min int := economy.ops_num('ops_login_lock_min', 15)::int;
begin
  -- 로그인끼리 줄을 선다 — 동시에 몰려도 실패 수가 정확하게
  perform pg_advisory_xact_lock(hashtext('ops-login'));
  delete from economy.ops_login_fails f where f.at < now() - interval '1 day';
  delete from economy.ops_login_locks l where l.until < now() - interval '1 day';
  delete from economy.ops_sessions s where coalesce(s.ended_at, s.expires_at) < now() - interval '30 days';

  -- 막힌 동안은 키를 보지도, 시도를 쌓지도 않는다
  v_until := economy.ops_locked_until(v_client);
  if v_until is not null then
    return economy.ops_locked_reply(v_until);
  end if;

  select * into v_key from economy.ops_keys k where k.token_hash = economy.ops_hash(p_key);
  if v_key.token_hash is null then
    v_reason := 'wrong';
  elsif v_key.revoked_at is not null then
    v_reason := 'revoked';
  elsif v_key.expires_at <= now() then
    v_reason := 'expired';
  end if;

  if v_reason is not null then
    v_until := economy.ops_fail(v_client, 'LOGIN');
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_login_fail', coalesce(v_key.label, ''),
            jsonb_build_object('reason', v_reason, 'client', left(v_client, 12)));
    if v_until is not null then
      return economy.ops_locked_reply(v_until);
    end if;
    if v_reason = 'expired' then
      return jsonb_build_object('ok', false, 'code', 'expired',
        'message', '기한이 지난 운영자 키예요. 새 키를 받아 주세요');
    end if;
    v_left := greatest(economy.ops_num('ops_login_client_fails', 5)::int
                - (select count(*) from economy.ops_login_fails f
                    where f.client = v_client
                      and f.at > now() - economy.ops_num('ops_login_window_min', 10)::float8 * interval '1 minute')::int, 0);
    return jsonb_build_object('ok', false, 'code', 'wrong', 'left', v_left,
      'message', format('운영자 키가 맞지 않거나 막힌 키예요. %s번 더 틀리면 %s분 동안 로그인을 막아요', v_left, v_lock_min));
  end if;

  v_token := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
  insert into economy.ops_sessions (token_hash, key_hash, label, client, expires_at)
  values (economy.ops_hash(v_token), v_key.token_hash, v_key.label, v_client,
          now() + economy.ops_num('ops_session_hours', 8)::float8 * interval '1 hour')
  returning * into v_session;
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_login', v_key.label,
          jsonb_build_object('by', v_key.label, 'session', v_session.id, 'client', left(v_client, 12),
                             'expires_at', v_session.expires_at));
  return jsonb_build_object('ok', true, 'session', v_token, 'actor', v_key.label,
    'expires_at', v_session.expires_at,
    'idle_min', economy.ops_num('ops_session_idle_min', 30),
    'key_expires_at', v_key.expires_at);
end $$;
revoke all on function public.ops_login(text) from public;
grant execute on function public.ops_login(text) to anon, authenticated;

-- 로그아웃 — 몇 번을 불러도 { ok:true }
create or replace function public.ops_logout(p_token text) returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_id bigint;
  v_label text;
begin
  update economy.ops_sessions s set ended_at = now(), ended_why = 'logout'
   where s.token_hash = economy.ops_hash(p_token) and s.ended_at is null
  returning s.id, s.label into v_id, v_label;
  if v_id is not null then
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_logout', v_label, jsonb_build_object('by', v_label, 'session', v_id));
  end if;
  return jsonb_build_object('ok', true);
end $$;
revoke all on function public.ops_logout(text) from public;
grant execute on function public.ops_logout(text) to anon, authenticated;

-- 살아 있는 세션 — 쓸 때마다 30분이 다시 시작된다. 없거나 끝났으면 예외(42501 → PostgREST 401)
create or replace function economy.ops_session_of(p_token text) returns economy.ops_sessions
language plpgsql security definer set search_path = '' as $$
declare
  v_s economy.ops_sessions;
begin
  update economy.ops_sessions s set last_used_at = now()
    from economy.ops_keys k
   where s.token_hash = economy.ops_hash(p_token)
     and k.token_hash = s.key_hash
     and s.ended_at is null
     and s.expires_at > now()
     and s.last_used_at > now() - economy.ops_num('ops_session_idle_min', 30)::float8 * interval '1 minute'
     and k.revoked_at is null
     and k.expires_at > now()
  returning s.* into v_s;
  if v_s.id is null then
    raise exception 'ops session required' using errcode = '42501',
      hint = '로그인이 끝났습니다(30분 쉼 · 8시간 · 막힌 키 · 기한 지난 키) — 운영자 키로 다시 들어오세요';
  end if;
  return v_s;
end $$;
revoke all on function economy.ops_session_of(text) from public;

-- 세션이 맞으면 그 키의 이름, 아니면 예외 — 0106 과 같은 칸. 이제 키가 아니라 세션 번호를 받는다
create or replace function economy.ops_actor(p_token text) returns text
language plpgsql security definer set search_path = '' as $$
declare v_s economy.ops_sessions;
begin
  v_s := economy.ops_session_of(p_token);
  return v_s.label;
end $$;
revoke all on function economy.ops_actor(text) from public;

-- 큰 일의 다시 확인 — 됐으면 null, 아니면 돌려줄 답. p_need 가 아니어도 키를 넣었으면 맞는지 본다(틀린 키로 "됐다"가 나오지 않게)
create or replace function economy.ops_confirm(p_session economy.ops_sessions, p_confirm text, p_what text, p_need boolean)
returns jsonb
language plpgsql security definer set search_path = '' as $$
declare
  v_client text := economy.ops_client();
  v_until timestamptz;
  v_fails int;
  v_out boolean := false;
begin
  if nullif(btrim(coalesce(p_confirm, '')), '') is null then
    if p_need then
      return jsonb_build_object('ok', false, 'code', 'confirm_required',
        'message', p_what || ' — 운영자 키를 한 번 더 넣어 주세요');
    end if;
    return null;
  end if;
  -- 이 곳이 로그인 실패로 막혀 있으면 다시 확인도 받지 않는다(세션은 이미 있으므로 모든 곳 잠금은 보지 않는다)
  v_until := economy.ops_locked_until(v_client, false);
  if v_until is not null then
    return economy.ops_locked_reply(v_until, 'confirm_locked');
  end if;
  if economy.ops_hash(p_confirm) = p_session.key_hash then
    if p_session.confirm_fails > 0 then
      update economy.ops_sessions s set confirm_fails = 0 where s.id = p_session.id;
    end if;
    return null;
  end if;

  perform pg_advisory_xact_lock(hashtext('ops-login'));
  v_until := economy.ops_fail(v_client, 'CONFIRM');
  update economy.ops_sessions s set confirm_fails = s.confirm_fails + 1
   where s.id = p_session.id returning s.confirm_fails into v_fails;
  if v_fails >= 3 then
    update economy.ops_sessions s set ended_at = now(), ended_why = 'confirm' where s.id = p_session.id;
    v_out := true;
  end if;
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_confirm_fail', p_session.label,
          jsonb_build_object('by', p_session.label, 'what', p_what, 'fails', v_fails, 'logged_out', v_out,
                             'client', left(v_client, 12)));
  return jsonb_build_object('ok', false, 'code', 'confirm_wrong', 'logged_out', v_out,
    'message', case
      when v_out then '운영자 키를 세 번 틀려 로그아웃했어요. 키로 다시 들어와 주세요'
      when v_until is not null then format('운영자 키를 여러 번 틀려 잠시 막았어요. %s(한국 시간)부터 다시 할 수 있어요',
                                            to_char(v_until at time zone 'Asia/Seoul', 'HH24:MI'))
      else format('운영자 키가 맞지 않아요(이 세션을 연 키여야 해요). %s번 더 틀리면 로그아웃해요', 3 - v_fails)
    end);
end $$;
revoke all on function economy.ops_confirm(economy.ops_sessions, text, text, boolean) from public;

-- 배수 · 시간을 사람이 읽는 글자로(2.00 → 2)
create or replace function economy.ops_short(p numeric) returns text
language sql immutable set search_path = '' as $$
  select trim(trailing '.' from trim(trailing '0' from round(p, 2)::text))
$$;
revoke all on function economy.ops_short(numeric) from public;

-- ════════════════════════════════════════════════════════════════════
--  운영자 함수 — 0106 본문 그대로에 세션 · 한도 · 다시 확인만 더했다
-- ════════════════════════════════════════════════════════════════════

-- 한눈에 — 0106 에 세션 · 오늘 한도 · 다시 확인 기준을 더했다(세션 시각을 고치므로 stable 아님)
create or replace function public.ops_overview(p_token text) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_event economy.reward_events;
  v_limit int := economy.ops_num('ops_key_daily_units', 5000)::int;
  v_used int;
begin
  select coalesce(sum(d.units), 0) into v_used from economy.ops_key_days d
   where d.key_hash = v_session.key_hash and d.day = economy.game_day(now());
  v_event := economy.reward_event_at(now());
  return jsonb_build_object(
    'actor', v_actor,
    'now', now(),
    'users', (select count(*) from public.profiles),
    'runners_7d', (select count(distinct s.user_id) from public.walk_sessions s
                    where s.started_at > now() - interval '7 days' and s.verdict <> 'VOID'),
    'kinds', coalesce((
      select jsonb_agg(jsonb_build_object(
               'kind', k.kind, 'group', k.grp, 'mode', k.mode, 'value', k.value, 'enabled', k.enabled,
               'held', coalesce((select sum(i.qty) from public.user_items i where i.kind = k.kind), 0),
               'holders', (select count(*) from public.user_items i where i.kind = k.kind and i.qty > 0),
               'used', (select count(*) from economy.item_uses u where u.kind = k.kind))
             order by k.sort, k.kind)
        from economy.item_kinds k), '[]'::jsonb),
    'event', case when v_event.id is null then null else economy.reward_event_json(v_event) end,
    'events', coalesce((
      select jsonb_agg(economy.reward_event_json(e) order by e.id desc)
        from (select * from economy.reward_events order by id desc limit 12) e), '[]'::jsonb),
    'batches', coalesce((
      select jsonb_agg(jsonb_build_object(
               'id', b.id, 'kind', b.kind, 'qty', b.qty, 'reason', b.reason, 'target', b.target,
               'actor', b.actor, 'users', b.users, 'push', b.push, 'created_at', b.created_at,
               'revoked_at', b.revoked_at) order by b.id desc)
        from (select * from economy.item_batches order by id desc limit 30) b), '[]'::jsonb),
    'session', jsonb_build_object(
      'expires_at', v_session.expires_at,
      'idle_min', economy.ops_num('ops_session_idle_min', 30),
      'key_expires_at', (select k.expires_at from economy.ops_keys k where k.token_hash = v_session.key_hash)),
    'cap', jsonb_build_object('limit', v_limit, 'used', v_used, 'left', greatest(v_limit - v_used, 0)),
    'confirm', jsonb_build_object(
      'recipients', economy.ops_num('ops_confirm_recipients', 200),
      'mult', economy.ops_num('ops_confirm_mult', 3),
      'hours', economy.ops_num('ops_confirm_hours', 48))
  );
end $$;
revoke all on function public.ops_overview(text) from public;
grant execute on function public.ops_overview(text) to anon, authenticated;

-- 사람 찾기 — 0106 그대로(세션 시각을 고치므로 stable 아님)
create or replace function public.ops_user_find(p_token text, p_query text) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_q text := btrim(coalesce(p_query, ''));
  v_like text;
begin
  if char_length(v_q) < 2 then
    return '[]'::jsonb;
  end if;
  v_like := '%' || replace(replace(replace(v_q, '\', '\\'), '%', '\%'), '_', '\_') || '%';
  return coalesce((
    select jsonb_agg(jsonb_build_object(
             'email', x.email, 'name', x.name, 'joined_at', x.created_at,
             'items', coalesce((select jsonb_object_agg(i.kind, i.qty) from public.user_items i
                                 where i.user_id = x.id and i.qty > 0), '{}'::jsonb))
           order by x.created_at desc)
      from (
        select u.id, u.email, coalesce(p.display_name, '') as name, u.created_at
          from auth.users u join public.profiles p on p.id = u.id
         where u.email ilike v_like or p.display_name ilike v_like
         order by u.created_at desc
         limit 20
      ) x), '[]'::jsonb);
end $$;
revoke all on function public.ops_user_find(text, text) from public;
grant execute on function public.ops_user_find(text, text) to anon, authenticated;

-- 지급 — 0106 에 칸 하나(p_confirm)를 더했다. 같은 요청 번호면 키 없이도 처음 결과(두 번 주지 않는다).
-- 차례: 검사(종류 · 개수 · 사유 · 대상) → 하루 한도 → 다시 확인(전체 · 200명 넘게) → 지급
drop function if exists public.ops_item_grant(text, uuid, text, int, text, text[], text, boolean);
create or replace function public.ops_item_grant(
  p_token text,
  p_request uuid,
  p_kind text,
  p_qty int,
  p_reason text,
  p_emails text[] default null,
  p_audience text default null,
  p_push boolean default true,
  p_confirm text default null
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_reason text := btrim(coalesce(p_reason, ''));
  v_emails text[];
  v_users uuid[];
  v_missing text[] := '{}';
  v_target text;
  v_batch economy.item_batches;
  v_user uuid;
  v_day date := economy.game_day(now());
  v_units int;
  v_limit int;
  v_used int;
  v_big boolean;
  v_refuse jsonb;
begin
  if p_request is null then
    return jsonb_build_object('ok', false, 'code', 'request', 'message', '요청 번호가 필요합니다');
  end if;
  -- 같은 지급을 두 번 누르지 못하게 — 요청 번호로 줄을 선다
  perform pg_advisory_xact_lock(hashtext('ops-grant:' || p_request::text));
  select * into v_batch from economy.item_batches b where b.request_id = p_request;
  if found then
    return jsonb_build_object('ok', true, 'batch', v_batch.id, 'users', v_batch.users, 'missing', '[]'::jsonb, 'replay', true);
  end if;

  if not exists (select 1 from economy.item_kinds k where k.kind = p_kind) then
    return jsonb_build_object('ok', false, 'code', 'kind', 'message', '없는 아이템입니다');
  end if;
  if p_qty is null or p_qty < 1 or p_qty > 99 then
    return jsonb_build_object('ok', false, 'code', 'qty', 'message', '개수는 1 ~ 99');
  end if;
  if char_length(v_reason) < 2 or char_length(v_reason) > 80 then
    return jsonb_build_object('ok', false, 'code', 'reason', 'message', '사유를 2 ~ 80자로 적어 주세요');
  end if;

  if p_audience = 'ALL' then
    v_target := 'ALL';
    select coalesce(array_agg(p.id), '{}') into v_users from public.profiles p;
  elsif p_audience = 'RUNNERS_7D' then
    v_target := 'RUNNERS_7D';
    select coalesce(array_agg(distinct s.user_id), '{}') into v_users from public.walk_sessions s
     where s.started_at > now() - interval '7 days' and s.verdict <> 'VOID';
  elsif p_audience is null or p_audience = 'EMAILS' then
    v_target := 'EMAILS';
    select coalesce(array_agg(distinct lower(btrim(e))), '{}') into v_emails
      from unnest(coalesce(p_emails, '{}')) e where btrim(coalesce(e, '')) <> '';
    if cardinality(v_emails) > 2000 then
      return jsonb_build_object('ok', false, 'code', 'too_many', 'message', '한 번에 2,000명까지');
    end if;
    select coalesce(array_agg(u.id), '{}') into v_users
      from auth.users u join public.profiles p on p.id = u.id
     where lower(u.email) = any (v_emails);
    select coalesce(array_agg(e order by e), '{}') into v_missing from unnest(v_emails) e
     where not exists (select 1 from auth.users u join public.profiles p on p.id = u.id where lower(u.email) = e);
  else
    return jsonb_build_object('ok', false, 'code', 'audience', 'message', '대상이 올바르지 않습니다');
  end if;

  if cardinality(v_users) = 0 then
    return jsonb_build_object('ok', false, 'code', 'no_target', 'message', '받을 사람이 없습니다', 'missing', to_jsonb(v_missing));
  end if;

  -- 하루 한도(0114) — 키마다 한국 날짜 하루. 같은 키의 지급끼리 줄을 세워 함께 넘지 않게
  v_units := cardinality(v_users) * p_qty;
  perform pg_advisory_xact_lock(hashtext('ops-cap:' || v_session.key_hash));
  v_limit := economy.ops_num('ops_key_daily_units', 5000)::int;
  select coalesce(sum(d.units), 0) into v_used from economy.ops_key_days d
   where d.key_hash = v_session.key_hash and d.day = v_day;
  if v_used + v_units > v_limit then
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_grant_cap', p_kind,
            jsonb_build_object('by', v_actor, 'units', v_units, 'used', v_used, 'limit', v_limit, 'target', v_target));
    return jsonb_build_object('ok', false, 'code', 'cap', 'units', v_units, 'used', v_used, 'limit', v_limit,
      'left', greatest(v_limit - v_used, 0),
      'message', format('이 키로 오늘 더 줄 수 있는 아이템은 %s개예요(하루 %s개 — 받는 사람 수 × 개수). 이번 지급은 %s개예요',
                        to_char(greatest(v_limit - v_used, 0), 'FM999,999,990'), to_char(v_limit, 'FM999,999,990'),
                        to_char(v_units, 'FM999,999,990')));
  end if;

  -- 다시 확인(0114) — 전체 · 기준보다 많은 사람
  v_big := v_target = 'ALL' or cardinality(v_users) > economy.ops_num('ops_confirm_recipients', 200);
  v_refuse := economy.ops_confirm(v_session, p_confirm,
    case when v_target = 'ALL' then '전체 지급'
         else format('%s명에게 지급', to_char(cardinality(v_users), 'FM999,999,990')) end, v_big);
  if v_refuse is not null then
    return v_refuse;
  end if;

  insert into economy.ops_key_days (key_hash, day, units) values (v_session.key_hash, v_day, v_units)
  on conflict (key_hash, day) do update set units = economy.ops_key_days.units + excluded.units;

  insert into economy.item_batches (request_id, kind, qty, reason, target, actor, push, users)
  values (p_request, p_kind, p_qty, v_reason, v_target, v_actor, coalesce(p_push, true), cardinality(v_users))
  returning * into v_batch;

  foreach v_user in array v_users loop
    perform economy.item_add(v_user, p_kind, p_qty);
    insert into economy.item_grants (batch_id, user_id, kind, qty) values (v_batch.id, v_user, p_kind, p_qty);
    if coalesce(p_push, true) then
      perform public.push_enqueue(v_user, 'ITEM_GRANTED',
        jsonb_build_object('item', p_kind, 'qty', p_qty::text, 'reason', v_reason), 'bag');
    end if;
  end loop;

  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_item_grant', p_kind,
          jsonb_build_object('by', v_actor, 'batch', v_batch.id, 'qty', p_qty, 'users', cardinality(v_users),
                             'target', v_target, 'reason', v_reason, 'units', v_units,
                             'confirmed', nullif(btrim(coalesce(p_confirm, '')), '') is not null));

  return jsonb_build_object('ok', true, 'batch', v_batch.id, 'users', cardinality(v_users),
                            'missing', to_jsonb(v_missing), 'replay', false);
end $$;
revoke all on function public.ops_item_grant(text, uuid, text, int, text, text[], text, boolean, text) from public;
grant execute on function public.ops_item_grant(text, uuid, text, int, text, text[], text, boolean, text) to anon, authenticated;

-- 지급 되돌리기 — 0106 에 칸 하나(p_confirm, 늘 필요)를 더했다
drop function if exists public.ops_grant_revoke(text, bigint);
create or replace function public.ops_grant_revoke(p_token text, p_batch bigint, p_confirm text default null) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_batch economy.item_batches;
  v_refuse jsonb;
  g record;
  v_take int;
  v_taken int := 0;
  v_short int := 0;
begin
  select * into v_batch from economy.item_batches b where b.id = p_batch for update;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'not_found', 'message', '없는 지급입니다');
  end if;
  if v_batch.revoked_at is not null then
    return jsonb_build_object('ok', false, 'code', 'revoked', 'message', '이미 되돌린 지급입니다');
  end if;
  v_refuse := economy.ops_confirm(v_session, p_confirm, '지급 되돌리기', true);
  if v_refuse is not null then
    return v_refuse;
  end if;
  for g in select * from economy.item_grants x where x.batch_id = p_batch loop
    perform pg_advisory_xact_lock(hashtext('ledger:' || g.user_id::text));
    select least(g.qty - g.revoked_qty, coalesce(i.qty, 0)) into v_take
      from (select 1) one left join public.user_items i on i.user_id = g.user_id and i.kind = g.kind;
    v_take := greatest(coalesce(v_take, 0), 0);
    if v_take > 0 then
      update public.user_items set qty = qty - v_take, updated_at = now()
       where user_id = g.user_id and kind = g.kind;
      update economy.item_grants set revoked_qty = revoked_qty + v_take where id = g.id;
    end if;
    v_taken := v_taken + v_take;
    if v_take < g.qty - g.revoked_qty then
      v_short := v_short + 1;
    end if;
  end loop;
  update economy.item_batches set revoked_at = now(), revoked_by = v_actor where id = p_batch;
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_grant_revoke', v_batch.kind,
          jsonb_build_object('by', v_actor, 'batch', p_batch, 'taken', v_taken, 'short', v_short, 'confirmed', true));
  return jsonb_build_object('ok', true, 'taken', v_taken, 'short', v_short);
end $$;
revoke all on function public.ops_grant_revoke(text, bigint, text) from public;
grant execute on function public.ops_grant_revoke(text, bigint, text) to anon, authenticated;

-- 보상 이벤트 시작 — 0106 에 칸 하나(p_confirm)를 더했다. 3배 이상 · 48시간 넘게는 다시 확인
drop function if exists public.ops_event_start(text, uuid, text, numeric, numeric, timestamptz, boolean);
create or replace function public.ops_event_start(
  p_token text,
  p_request uuid,
  p_title text,
  p_mult numeric,
  p_hours numeric,
  p_starts_at timestamptz default null,
  p_push boolean default true,
  p_confirm text default null
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_title text := btrim(coalesce(p_title, ''));
  v_from timestamptz := coalesce(p_starts_at, now());
  v_to timestamptz;
  v_event economy.reward_events;
  v_user uuid;
  v_pushed int := 0;
  v_big boolean;
  v_refuse jsonb;
begin
  if p_request is null then
    return jsonb_build_object('ok', false, 'code', 'request', 'message', '요청 번호가 필요합니다');
  end if;
  perform pg_advisory_xact_lock(hashtext('ops-event'));
  select * into v_event from economy.reward_events e where e.request_id = p_request;
  if found then
    return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event), 'replay', true);
  end if;

  if char_length(v_title) < 2 or char_length(v_title) > 40 then
    return jsonb_build_object('ok', false, 'code', 'title', 'message', '이름을 2 ~ 40자로 적어 주세요');
  end if;
  if p_mult is null or p_mult <= 1 or p_mult > 5 then
    return jsonb_build_object('ok', false, 'code', 'mult', 'message', '배수는 1 보다 크고 5 이하');
  end if;
  if p_hours is null or p_hours < 1 or p_hours > 168 then
    return jsonb_build_object('ok', false, 'code', 'hours', 'message', '기간은 1 ~ 168시간');
  end if;
  if v_from < now() - interval '1 minute' then
    return jsonb_build_object('ok', false, 'code', 'past', 'message', '지난 시각에는 시작할 수 없습니다');
  end if;
  if v_from > now() + interval '30 days' then
    return jsonb_build_object('ok', false, 'code', 'far', 'message', '30일 안으로 정해 주세요');
  end if;
  v_to := v_from + make_interval(secs => (p_hours * 3600)::double precision);

  if exists (select 1 from economy.reward_events e
              where e.cancelled_at is null and e.ends_at > now()
                and e.starts_at < v_to and e.ends_at > v_from) then
    return jsonb_build_object('ok', false, 'code', 'overlap', 'message', '그 시간에 이미 보상 이벤트가 있습니다');
  end if;

  -- 다시 확인(0114) — 큰 배수 · 긴 기간
  v_big := p_mult >= economy.ops_num('ops_confirm_mult', 3) or p_hours > economy.ops_num('ops_confirm_hours', 48);
  v_refuse := economy.ops_confirm(v_session, p_confirm,
    format('보상 ×%s · %s시간 이벤트', economy.ops_short(p_mult), economy.ops_short(p_hours)), v_big);
  if v_refuse is not null then
    return v_refuse;
  end if;

  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (p_request, v_title, round(p_mult, 2), v_from, v_to, v_actor)
  returning * into v_event;

  if coalesce(p_push, true) and v_from <= now() + interval '1 minute' then
    for v_user in select p.id from public.profiles p loop
      perform public.push_enqueue(v_user, 'REWARD_EVENT',
        jsonb_build_object('mult', trim(trailing '.' from trim(trailing '0' from round(p_mult, 2)::text)),
                           'hours', trim(trailing '.' from trim(trailing '0' from round(p_hours, 1)::text)),
                           'title', v_title), 'run');
      v_pushed := v_pushed + 1;
    end loop;
  end if;

  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_event_start', v_event.id::text,
          jsonb_build_object('by', v_actor, 'title', v_title, 'mult', v_event.mult,
                             'starts_at', v_from, 'ends_at', v_to,
                             'confirmed', nullif(btrim(coalesce(p_confirm, '')), '') is not null));

  return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event), 'replay', false);
end $$;
revoke all on function public.ops_event_start(text, uuid, text, numeric, numeric, timestamptz, boolean, text) from public;
grant execute on function public.ops_event_start(text, uuid, text, numeric, numeric, timestamptz, boolean, text) to anon, authenticated;

-- ops_event_cancel 은 0106 그대로다(economy.ops_actor 가 세션을 본다 · 끄기는 다시 확인 없음).
