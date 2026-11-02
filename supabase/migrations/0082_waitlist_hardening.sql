-- ════════════════════════════════════════════════════════════════════
--  0082 — 대기 명단 등록 단단하게 (2026-11-02)
--
--  2026-11-02 13:05 ~ 13:20(한국 시간) 누군가 공개 등록 API 에 자동 점검을 돌렸다 —
--  example.com 시험 주소 · XSS 흉내(<script>…@evil.com 등) · 운영자 주소(대신 등록).
--  화면에 이메일을 그리는 곳이 없어 실행되지는 않았지만, 느슨한 검사("공백 없고 @ 와 점")가 통과시켰다.
--  사용자 결정: 검사를 강화하고, 그때 들어온 가짜 줄과 운영자 주소 줄을 지운다.
--
--  · 이메일: 영문 소문자 · 숫자 · . _ % + - 만(로컬 64자), 도메인은 글자 · 숫자 · - 마디 + 2 ~ 24자 끝.
--    '..' · 점으로 시작 · @ 앞 점은 안 된다. 시험용 도메인(example.* · .test · .invalid · .localhost · .example)은 안 된다.
--  · 같은 곳(IP)에서 10분 6번 · 하루 30번까지(잘못된 시도도 센다). IP 는 소금 친 해시로만 남기고 하루 지나면 지운다.
--    값은 economy_settings 의 waitlist_ip_per_10min · waitlist_ip_per_day 로 바꾼다.
--  · 틀린 등록 · 너무 잦은 등록은 예외 대신 {ok:false, message} 로 답한다 — 예외는 시도 기록까지 되돌린다.
--    홈페이지는 message 를 그대로 본다('invalid waitlist registration' → 이메일 확인 안내).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('waitlist_ip_per_10min', '6'::jsonb),
  ('waitlist_ip_per_day', '30'::jsonb)
on conflict (key) do nothing;

-- 등록 시도(같은 곳에서 너무 자주) — IP 원문은 남기지 않는다
create table if not exists economy.waitlist_attempts (
  ip_hash text not null,
  at timestamptz not null default now()
);
create index if not exists waitlist_attempts_ip_at on economy.waitlist_attempts (ip_hash, at);
revoke all on economy.waitlist_attempts from public, anon, authenticated;

-- 이 이메일을 받는가 — 서버 규칙 한 곳(홈페이지 web/waitlist-core.mjs validEmail 이 같은 규칙)
create or replace function economy.waitlist_email_ok(p_email text) returns boolean
  language sql immutable set search_path = '' as $$
  select p_email is not null
     and length(p_email) between 6 and 254
     and p_email ~ '^[a-z0-9._%+-]{1,64}@([a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?\.)+[a-z]{2,24}$'
     and p_email !~ '\.\.'
     and p_email !~ '^\.'
     and p_email !~ '\.@'
     and split_part(p_email, '@', 2) !~ '(^|\.)(example\.(com|net|org)|example|test|invalid|localhost)$'
$$;
revoke all on function economy.waitlist_email_ok(text) from public;

-- 부른 곳의 IP(소금 친 해시). Supabase 앞단이 넣어 주는 머리글에서 — 없으면(SQL Editor · 검사) null
create or replace function economy.waitlist_client() returns text
  language plpgsql stable security definer set search_path = '' as $$
declare
  v_headers jsonb := nullif(current_setting('request.headers', true), '')::jsonb;
  v_ip text;
begin
  if v_headers is null then
    return null;
  end if;
  v_ip := coalesce(nullif(btrim(v_headers ->> 'cf-connecting-ip'), ''),
                   nullif(btrim(split_part(v_headers ->> 'x-forwarded-for', ',', 1)), ''),
                   nullif(btrim(v_headers ->> 'x-real-ip'), ''));
  if v_ip is null then
    return null;
  end if;
  return encode(sha256(convert_to('stepup-waitlist:' || v_ip, 'UTF8')), 'hex');
end $$;
revoke all on function economy.waitlist_client() from public;

create or replace function public.waitlist_register(p_email text, p_consent boolean, p_trap text default '', p_ref text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_email text := lower(btrim(p_email));
  v_ref text := upper(btrim(coalesce(p_ref, '')));
  v_client text := economy.waitlist_client();
  v_referrer uuid;
  v_receipt text;
  v_hash text;
  v_code text;
begin
  -- 같은 곳에서 너무 자주 — 잘못된 시도도 센다(아래에서 예외 대신 답으로 끝내 기록이 남는다)
  if v_client is not null then
    delete from economy.waitlist_attempts where ip_hash = v_client and at < now() - interval '1 day';
    if (select count(*) from economy.waitlist_attempts
         where ip_hash = v_client and at > now() - interval '10 minutes')
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_10min'), 6)
       or (select count(*) from economy.waitlist_attempts where ip_hash = v_client)
         >= coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'waitlist_ip_per_day'), 30) then
      return jsonb_build_object('ok', false, 'message', 'too many waitlist attempts');
    end if;
    insert into economy.waitlist_attempts (ip_hash) values (v_client);
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
revoke execute on function public.waitlist_register(text, boolean, text, text) from public, authenticated;
grant execute on function public.waitlist_register(text, boolean, text, text) to anon;

-- 2026-11-02 자동 점검으로 들어온 줄 · 운영자 주소 줄을 한 번만 지운다(사용자 결정).
-- 다시 붙여넣어도 그 뒤에 운영자가 직접 등록한 줄은 지우지 않는다(표시 waitlist_cleanup_20261102).
do $$ begin
  if not exists (select 1 from public.economy_settings where key = 'waitlist_cleanup_20261102') then
    update public.waitlist_entries set referred_by = null
     where referred_by in (select w.id from public.waitlist_entries w
                            where w.email ~ '@evil\.com$' or w.email ~ '@example\.(com|net|org)$'
                               or w.email in ('mycyi1994@gmail.com', 'support@stepupcrew.com', 'stepupofficial@stepupcrew.com'));
    delete from public.waitlist_entries
     where email ~ '@evil\.com$' or email ~ '@example\.(com|net|org)$'
        or email in ('mycyi1994@gmail.com', 'support@stepupcrew.com', 'stepupofficial@stepupcrew.com');
    insert into public.economy_settings (key, value) values ('waitlist_cleanup_20261102', 'true'::jsonb)
    on conflict (key) do nothing;
  end if;
end $$;
