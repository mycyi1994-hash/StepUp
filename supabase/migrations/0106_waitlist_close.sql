-- ════════════════════════════════════════════════════════════════════
--  0106 — 사전 등록 마감 · 타워 게임은 누구나 (2026-11-18)
--
--  사용자 "홈페이지 사전예약 오늘 11시 59분에 닫아서 마감" → "게임은 계속으로".
--  1) 마감 시각 설정 waitlist_closes_at(문자열 timestamptz, 비우면 마감 없음). 지나면 등록은 {ok:false, message:'waitlist closed'}.
--     이미 있는 값은 덮지 않는다(배포마다 setup.sql 이 다시 올라간다). 바꾸기 · 다시 열기(SQL Editor):
--       select admin_economy_set('waitlist_closes_at', '"2026-11-30T23:59:00+09:00"');
--       select admin_economy_set('waitlist_closes_at', 'null');                        -- 다시 연다
--     홈페이지는 web/waitlist.js 의 WAITLIST_CLOSES 로 같은 시각에 "마감"을 보인다 — 바꾸면 둘 다.
--  2) 타워 게임: 사전 등록하지 않은 이메일도 참가(economy.tower_guests). 사전 등록한 이메일은 예전처럼 그 줄로 센다.
--     그래서 타워 표의 entry_id 는 사전 등록 줄 또는 게임 손님 줄 — 사전 등록 표에 대한 묶음(FK)을 푼다.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value)
values ('waitlist_closes_at', to_jsonb('2026-11-18T23:59:00+09:00'::text))
on conflict (key) do nothing;

create or replace function economy.waitlist_closed() returns boolean
language sql stable security definer set search_path = '' as $$
  select coalesce(now() >= (select (value #>> '{}')::timestamptz from public.economy_settings
                             where key = 'waitlist_closes_at' and jsonb_typeof(value) = 'string'), false)
$$;
revoke all on function economy.waitlist_closed() from public;

-- 등록 한 번 — 0083 그대로에 마감 검사만 앞에
create or replace function economy.waitlist_register_core(p_email text, p_consent boolean, p_trap text, p_ref text, p_client text)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_email text := lower(btrim(p_email));
  v_ref text := upper(btrim(coalesce(p_ref, '')));
  v_referrer uuid;
  v_receipt text;
  v_hash text;
  v_code text;
begin
  if economy.waitlist_closed() then
    return jsonb_build_object('ok', false, 'message', 'waitlist closed');
  end if;
  -- 같은 곳에서 너무 자주 — 잘못된 시도도 센다(예외 대신 답으로 끝내 기록이 남는다)
  if p_client is not null then
    delete from economy.waitlist_attempts where ip_hash = p_client and at < now() - interval '1 day';
    if (select count(*) from economy.waitlist_attempts
         where ip_hash = p_client and at > now() - interval '10 minutes')
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_10min'), 6)
       or (select count(*) from economy.waitlist_attempts where ip_hash = p_client)
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_day'), 30) then
      return jsonb_build_object('ok', false, 'message', 'too many waitlist attempts');
    end if;
    insert into economy.waitlist_attempts (ip_hash) values (p_client);
  end if;

  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true);
  end if;
  if p_consent is distinct from true or p_email is null or length(p_email) > 254
     or not economy.waitlist_email_ok(v_email) then
    return jsonb_build_object('ok', false, 'message', 'invalid waitlist registration');
  end if;
  -- 틀리거나 없는 추천 코드는 등록을 막지 않고 무시한다.
  if v_ref ~ '^[A-HJ-NP-Z2-9]{8}$' then
    select id into v_referrer from public.waitlist_entries
    where referral_code = v_ref and status = 'registered' and email <> v_email;
  end if;

  v_receipt := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
  v_hash := encode(sha256(convert_to(v_receipt, 'UTF8')), 'hex');
  -- 추천인은 처음 등록할 때만 남는다. 같은 이메일로 다시 등록해도 추천 코드와 추천인은 그대로다.
  insert into public.waitlist_entries (email, status, consented_at, receipt_hash, referred_by)
  values (v_email, 'registered', now(), v_hash, v_referrer)
  on conflict (email) do update
    set status = 'registered', consented_at = now(), receipt_hash = excluded.receipt_hash
  returning referral_code into v_code;

  return jsonb_build_object('ok', true, 'receipt', v_receipt, 'referral_code', v_code);
end;
$$;
revoke all on function economy.waitlist_register_core(text, boolean, text, text, text) from public;

-- ── 타워: 게임 손님 ──────────────────────────────────────────────
create table if not exists economy.tower_guests (
  id uuid primary key default gen_random_uuid(),
  email text not null unique check (email = lower(btrim(email)) and length(email) between 3 and 254),
  created_at timestamptz not null default now()
);
alter table economy.tower_guests enable row level security;
revoke all on economy.tower_guests from public;

alter table economy.tower_players drop constraint if exists tower_players_entry_id_fkey;
alter table economy.tower_runs drop constraint if exists tower_runs_entry_id_fkey;
alter table economy.tower_sns drop constraint if exists tower_sns_entry_id_fkey;

-- 사전 등록 줄이 먼저, 없으면 게임 손님 줄
create or replace function economy.tower_entry(p_email text) returns uuid
language sql stable security definer set search_path = '' as $$
  select coalesce(
    (select id from public.waitlist_entries where email = lower(btrim(coalesce(p_email, ''))) and status = 'registered'),
    (select id from economy.tower_guests where email = lower(btrim(coalesce(p_email, '')))))
$$;
revoke all on function economy.tower_entry(text) from public;

-- 참가 — 사전 등록하지 않은 이메일은 게임 손님으로 만든다(0104 의 not_registered 를 대신한다)
create or replace function public.tower_join(p_email text, p_name text, p_trap text default '')
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_name text := regexp_replace(btrim(coalesce(p_name, '')), '\s+', ' ', 'g');
  v_entry uuid;
begin
  if p_trap is distinct from '' then return jsonb_build_object('ok', false, 'code', 'email'); end if;
  if not economy.tower_throttle() then return jsonb_build_object('ok', false, 'code', 'busy'); end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then return jsonb_build_object('ok', false, 'code', 'email'); end if;
  if v_name !~ '^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$' then return jsonb_build_object('ok', false, 'code', 'name'); end if;
  v_entry := economy.tower_entry(v_email);
  if v_entry is null then
    insert into economy.tower_guests (email) values (v_email) on conflict (email) do nothing;
    v_entry := economy.tower_entry(v_email);
  end if;
  insert into economy.tower_players (entry_id, name) values (v_entry, v_name)
  on conflict (entry_id) do update set name = excluded.name, updated_at = now();
  return economy.tower_status(v_entry);
end $$;
revoke all on function public.tower_join(text, text, text) from public;
grant execute on function public.tower_join(text, text, text) to anon, authenticated;
