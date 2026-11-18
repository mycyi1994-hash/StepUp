-- ════════════════════════════════════════════════════════════════════
--  0113 — 베타 초대 코드: 하루 1개 · 7일에 3개 (2026-11-18, docs/access-codes-2026-10-28.md "2026-11-18")
--
--  사용자 결정: "3개씩 나오던 코드를 하루에 1개, 일주일에 3개까지만 나올 수 있게 해".
--  · 하루 — access_user_daily_codes(0079 기본 3 → 1). 하루는 지금처럼 한국 날짜(00시 KST)로 센다.
--  · 일주일 — 새 설정 access_user_weekly_codes = 3. 달력의 주가 아니라 **지금부터 거슬러 168시간(7 × 24시간)** 안에
--    만든 사용자 코드 수로 센다(굴러가는 7일 — 월요일에 몰아 만들고 일요일에 또 몰아 만드는 일이 없다).
--    가장 오래된 코드가 168시간을 넘기는 순간 한 자리가 다시 열린다.
--  · 이미 만든 코드는 그대로 쓸 수 있다(만료 · 차단 규칙도 그대로). 세는 것은 0079 처럼 만든 코드 모두(차단된 것도 — 만든 일은 만든 일).
--  · 0 이면 그 한도로 코드를 만들 수 없다(하루 0 과 같다 — can_issue = false).
--
--  앱 계약:
--    · access_status() — 예전 칸(daily_limit · issued_today) 그대로에 weekly_limit · issued_week · left_today · left_week ·
--      left · next_at(막혔을 때 다음 코드를 만들 수 있는 시각) · limited_by(DAY · WEEK)를 더했다. jsonb 라 모양은 그대로다.
--    · access_code_issue() — 예전 앱용. 거절은 지금처럼 답 안의 { error } (daily_limit 에 weekly_limit 이 더해졌다 —
--      예전 앱은 모르는 error 를 "지금은 초대 코드를 만들 수 없어요"로 보인다, 사실과 맞다).
--    · access_code_issue_v2() — 새 앱용. 한도 거절은 23514, 권한 없음은 42501 로 거절한다(한국어 문장 · hint 에 갈래 ·
--      detail 에 next_at). 이름에 숫자가 있어 문지기(access_gate)의 access_* 예외에 들지 않는다 — 문지기는 권한 없는
--      사람만 막고, 권한 없는 사람은 어차피 코드를 만들 수 없다(막히면 앱이 권한을 다시 묻는다).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('access_user_weekly_codes', '3'::jsonb)
on conflict (key) do nothing;

/*
 * 하루 개수 3 → 1. setup.sql 은 배포 때마다 다시 올라가므로 값만 보고 바꾸면, 운영자가 나중에 일부러 3 으로 돌린 값을
 * 다음 배포가 다시 1 로 덮는다. 그래서 두 가지를 함께 본다:
 *   · 한 번만 — 표시 access_daily_codes_0113 이 없을 때만(0079 의 access_v1_applied 와 같은 방식).
 *   · 0079 기본값(3) 그대로일 때만 — 운영자가 이미 다른 값(예: 5 · 0)으로 바꿨으면 그 결정을 따른다.
 * 바꿨으면 true. 검사가 같은 규칙을 다시 부를 수 있게 함수로 둔다.
 */
create or replace function economy.access_daily_codes_0113() returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare v_changed boolean := false;
begin
  if exists (select 1 from public.economy_settings where key = 'access_daily_codes_0113') then
    return false;
  end if;
  update public.economy_settings set value = '1'::jsonb, updated_at = now()
   where key = 'access_user_daily_codes' and economy.setting_num('access_user_daily_codes') = 3;
  v_changed := found;
  insert into public.economy_settings (key, value) values ('access_daily_codes_0113', 'true'::jsonb)
  on conflict (key) do nothing;
  return v_changed;
end $$;
revoke all on function economy.access_daily_codes_0113() from public;

do $$ begin perform economy.access_daily_codes_0113(); end $$;

-- ══════════════════════════════════════════════════════════════════
-- 한도 — 오늘(한국 날짜) · 지난 168시간
-- ══════════════════════════════════════════════════════════════════
/*
 * { daily_limit, issued_today, weekly_limit, issued_week, left_today, left_week, left, next_at, limited_by }
 * next_at — left 가 0 일 때 다음 코드를 만들 수 있는 가장 이른 시각(없으면 null):
 *   하루가 막혔으면 내일 00시(KST), 7일이 막혔으면 창 안 코드 중 (issued_week − weekly_limit + 1)번째로 오래된 것 + 168시간.
 *   둘 다 막혔으면 늦은 쪽(앞의 시각에는 다른 쪽이 아직 막는다). 앞으로 만들 코드는 없으니(막혀 있다) 이 시각이 맞다.
 * limited_by — 그 늦은 쪽(DAY · WEEK). 한도가 0 이면(꺼 둠) 다시 열리지 않으니 next_at · limited_by 는 null.
 */
create or replace function economy.access_issue_quota(p_user uuid) returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_day_limit int := greatest(coalesce(economy.setting_num('access_user_daily_codes'), 0)::int, 0);
  -- 설정 줄이 지워졌으면 사용자가 정한 3
  v_week_limit int := greatest(coalesce(economy.setting_num('access_user_weekly_codes'), 3)::int, 0);
  v_window timestamptz := now() - interval '168 hours';
  v_today int;
  v_week int;
  v_left_today int;
  v_left_week int;
  v_day_next timestamptz;
  v_week_next timestamptz;
  v_next timestamptz;
begin
  select count(*) filter (where c.created_at >= economy.today_start())::int, count(*)::int
    into v_today, v_week
    from economy.access_codes c
   where c.created_by = p_user and c.kind = 'USER' and c.created_at > v_window;
  v_left_today := greatest(v_day_limit - v_today, 0);
  v_left_week := greatest(v_week_limit - v_week, 0);
  if v_left_today = 0 and v_day_limit > 0 then
    v_day_next := economy.game_day_start(economy.game_day(now()) + 1);
  end if;
  if v_left_week = 0 and v_week_limit > 0 then
    select c.created_at + interval '168 hours' into v_week_next
      from economy.access_codes c
     where c.created_by = p_user and c.kind = 'USER' and c.created_at > v_window
     order by c.created_at
     offset greatest(v_week - v_week_limit, 0) limit 1;
  end if;
  if v_day_limit > 0 and v_week_limit > 0 then
    v_next := greatest(v_day_next, v_week_next);  -- greatest 는 null 을 건너뛴다
  end if;
  return jsonb_build_object(
    'daily_limit', v_day_limit,
    'issued_today', v_today,
    'weekly_limit', v_week_limit,
    'issued_week', v_week,
    'left_today', v_left_today,
    'left_week', v_left_week,
    'left', least(v_left_today, v_left_week),
    'next_at', v_next,
    'limited_by', case
      when least(v_left_today, v_left_week) > 0 or v_day_limit = 0 or v_week_limit = 0 then null
      when v_left_week = 0 and (v_left_today > 0 or v_week_next >= v_day_next or v_day_next is null) then 'WEEK'
      else 'DAY' end);
end $$;
revoke all on function economy.access_issue_quota(uuid) from public;

-- 코드를 만들 수 있는 사람인가 — 0079 access_status 의 can_issue 그대로 + 7일 한도가 0 이 아닌가
create or replace function economy.access_can_issue(p_user uuid) returns boolean
language sql stable security definer set search_path = public, economy as $$
  select economy.access_mode() <> 'OPEN'
     and greatest(coalesce(economy.setting_num('access_user_daily_codes'), 0), 0) > 0
     and greatest(coalesce(economy.setting_num('access_user_weekly_codes'), 3), 0) > 0
     and exists (select 1 from economy.access_grants g
                  where g.user_id = p_user and g.revoked_at is null
                    and coalesce(economy.setting('access_issue_sources'), '[]'::jsonb) ? g.source)
$$;
revoke all on function economy.access_can_issue(uuid) from public;

/*
 * 코드 하나 만들기(잠금은 부르는 쪽이 잡는다). 된 답 { code, expires_at } + 만든 뒤의 한도,
 * 거절 { error: not_allowed | daily_limit | weekly_limit } + 지금 한도(next_at · limited_by).
 */
create or replace function economy.access_code_issue_core(p_user uuid) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  q jsonb;
  v_code text;
  v_exp timestamptz := now() + make_interval(days => greatest(coalesce(economy.setting_num('access_user_code_days'), 14)::int, 1));
begin
  if not economy.access_can_issue(p_user) then
    return jsonb_build_object('error', 'not_allowed');
  end if;
  q := economy.access_issue_quota(p_user);
  if (q ->> 'left')::int <= 0 then
    return q || jsonb_build_object('error', case when q ->> 'limited_by' = 'WEEK' then 'weekly_limit' else 'daily_limit' end);
  end if;
  v_code := economy.access_new_code();
  insert into economy.access_codes (code, kind, created_by, max_uses, expires_at)
  values (v_code, 'USER', p_user, 1, v_exp);
  return economy.access_issue_quota(p_user) || jsonb_build_object('code', economy.access_pretty(v_code), 'expires_at', v_exp);
end $$;
revoke all on function economy.access_code_issue_core(uuid) from public;

-- ══════════════════════════════════════════════════════════════════
-- 앱
-- ══════════════════════════════════════════════════════════════════
/*
 * 내 접속 상태(0079 그대로 + 한도 칸). 공개 베타면 고른 사전 예약 이메일 계정에 여기서 권한을 준다.
 * { mode, granted, source, revoked, can_issue, daily_limit, issued_today,
 *   weekly_limit, issued_week, left_today, left_week, left, next_at, limited_by }
 */
create or replace function public.access_status()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  g economy.access_grants;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.access_waitlist_grant(v_user);
  select * into g from economy.access_grants x where x.user_id = v_user;
  return jsonb_build_object(
    'mode', economy.access_mode(),
    'granted', economy.access_ok(v_user),
    'source', g.source,
    'revoked', g.revoked_at is not null,
    'can_issue', economy.access_can_issue(v_user)) || economy.access_issue_quota(v_user);
end $$;
revoke all on function public.access_status() from public, anon;
grant execute on function public.access_status() to authenticated;

/* 예전 앱 — 내 코드 하나 만들기. { code, expires_at, … } 또는 { error: not_allowed | daily_limit | weekly_limit, … } */
create or replace function public.access_code_issue()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('access:' || v_user::text));
  perform economy.access_waitlist_grant(v_user);
  return economy.access_code_issue_core(v_user);
end $$;
revoke all on function public.access_code_issue() from public, anon;
grant execute on function public.access_code_issue() to authenticated;

/*
 * 새 앱 — 내 코드 하나 만들기. 된 답은 access_code_issue 와 같다({ code, expires_at } + 만든 뒤 한도).
 * 거절은 앱이 까닭을 읽는 코드로: 한도 23514(hint daily_limit · weekly_limit, detail 다음 시각), 권한 없음 42501(hint not_allowed).
 */
create or replace function public.access_code_issue_v2()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v jsonb;
  v_next timestamptz;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('access:' || v_user::text));
  perform economy.access_waitlist_grant(v_user);
  v := economy.access_code_issue_core(v_user);
  v_next := (v ->> 'next_at')::timestamptz;
  if v ->> 'error' = 'not_allowed' then
    raise exception '지금은 초대 코드를 만들 수 없어요' using errcode = '42501', hint = 'not_allowed';
  elsif v ->> 'error' = 'weekly_limit' then
    raise exception '초대 코드는 7일 동안 %개까지 만들 수 있어요.%', v ->> 'weekly_limit',
      coalesce(' ' || to_char(v_next at time zone 'Asia/Seoul', 'FMMM"월" FMDD"일" HH24:MI') || '부터 다시 만들 수 있어요', '')
      using errcode = '23514', hint = 'weekly_limit', detail = coalesce(v ->> 'next_at', '');
  elsif v ->> 'error' = 'daily_limit' then
    raise exception '초대 코드는 하루 %개까지 만들 수 있어요. 내일 다시 만들 수 있어요', v ->> 'daily_limit'
      using errcode = '23514', hint = 'daily_limit', detail = coalesce(v ->> 'next_at', '');
  end if;
  return v;
end $$;
revoke all on function public.access_code_issue_v2() from public, anon;
grant execute on function public.access_code_issue_v2() to authenticated;
