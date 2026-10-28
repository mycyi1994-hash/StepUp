-- ════════════════════════════════════════════════════════════════════
--  0080 — 접속 코드: 기존 계정도 코드를 넣어야 들어온다 (2026-10-28)
--
--  사용자 결정: "기존 직원도 무조건 다시 입력해야 하는 버전".
--  0079 는 배포 때 있던 계정에 LEGACY 권한을 주어 그대로 통과시켰다 — 이제 LEGACY 권한은 문을 열지 않는다.
--  · 기록은 지우지 않는다(LEGACY 줄은 남는다). 그 계정이 코드를 넣으면 같은 줄이 CODE 권한으로 바뀐다.
--  · 운영자 계정(app_admins)도 앱에서는 코드를 넣어야 한다. SQL Editor 의 운영자 함수는 그대로 된다(로그인 없는 postgres).
--  · 어테스터 계정은 그대로 통과(체인 기록 워커).
--  · 사용자 코드 만들기는 코드 · 운영자 · 공개 베타가 아니라 코드(CODE) · 운영자가 준(ADMIN) 권한만 —
--    access_issue_sources 가 0079 기본값이면 LEGACY 를 뺀다(관리자가 바꾼 값은 그대로).
-- ════════════════════════════════════════════════════════════════════

update public.economy_settings set value = '["CODE", "ADMIN"]'::jsonb, updated_at = now()
 where key = 'access_issue_sources' and value = '["CODE", "ADMIN", "LEGACY"]'::jsonb;

-- 이 계정이 쓸 수 있는가 — 문이 열렸거나, LEGACY 가 아닌 권한이 살아 있거나, 어테스터
create or replace function economy.access_ok(p_user uuid) returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select economy.access_mode() = 'OPEN'
      or exists (select 1 from economy.access_grants g
                  where g.user_id = p_user and g.revoked_at is null and g.source <> 'LEGACY')
      -- 어테스터 계정 칸이 비어 있으면(null) 비교가 null 이 되어 전체가 null 이 된다 — false 로
      or coalesce(p_user::text = (economy.setting('attester_user_id') #>> '{}'), false)
$$;
revoke all on function economy.access_ok(uuid) from public;

-- 공개 베타 — 고른 사전 예약 이메일로 로그인한 계정에 권한(LEGACY 줄은 WAITLIST 로 바꾼다, 차단된 권한은 되살리지 않는다)
create or replace function economy.access_waitlist_grant(p_user uuid) returns boolean
language plpgsql security definer set search_path = public, economy as $$
begin
  if economy.access_mode() <> 'OPEN_BETA' or p_user is null then
    return false;
  end if;
  if exists (select 1 from economy.access_grants g where g.user_id = p_user and (g.source <> 'LEGACY' or g.revoked_at is not null)) then
    return false;
  end if;
  if not exists (select 1 from auth.users u join economy.access_open_beta o on o.email = lower(btrim(u.email))
                  where u.id = p_user) then
    return false;
  end if;
  insert into economy.access_grants (user_id, source) values (p_user, 'WAITLIST')
  on conflict (user_id) do update set source = 'WAITLIST', granted_at = now();
  return true;
end $$;
revoke all on function economy.access_waitlist_grant(uuid) from public;

/* 0079 그대로 + LEGACY 권한만 있는 계정도 코드를 넣는다(같은 줄을 CODE 로 바꾼다) */
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
  if found and g.source <> 'LEGACY' then
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
  insert into economy.access_grants (user_id, source, code) values (v_user, 'CODE', c.code)
  on conflict (user_id) do update set source = 'CODE', code = excluded.code, granted_at = now();
  return jsonb_build_object('ok', true, 'already', false);
end $$;
revoke all on function public.access_redeem(text) from public, anon;
grant execute on function public.access_redeem(text) to authenticated;
