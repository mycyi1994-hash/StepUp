-- ════════════════════════════════════════════════════════════════════
--  0083 — 대기 명단: 사람 확인(Cloudflare Turnstile)을 거친 등록만 받을 수 있게 (2026-11-02)
--
--  사용자 결정 "보안 단단하게 — 전부 진행". 0082 의 이메일 규칙 · 같은 곳 횟수에 더해, 등록을
--  어테스터 워커(/v1/waitlist/register)가 Turnstile 토큰을 확인한 뒤에만 넣게 할 수 있다.
--  · 워커는 attester_waitlist_register 를 부른다(어테스터만) — 부른 사람의 IP 는 워커가 넘긴다(여기서 해시).
--  · 설정 waitlist_require_turnstile 을 true 로 바꾸면(키를 넣고 홈페이지가 워커로 보낸 뒤) 공개 waitlist_register 는
--    {ok:false, message:'verification required'} 로 답한다. 기본은 false — 키가 없는 동안 지금처럼 받는다.
--  · 규칙 · 횟수 · 저장은 한 함수(economy.waitlist_register_core)가 한다.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values ('waitlist_require_turnstile', 'false'::jsonb)
on conflict (key) do nothing;

-- IP 원문 → 소금 친 해시(0082 economy.waitlist_client 와 같은 값)
create or replace function economy.waitlist_client_of(p_ip text) returns text
  language sql immutable set search_path = '' as $$
  select case when nullif(btrim(coalesce(p_ip, '')), '') is null then null
              else encode(sha256(convert_to('stepup-waitlist:' || btrim(p_ip), 'UTF8')), 'hex') end
$$;
revoke all on function economy.waitlist_client_of(text) from public;

create or replace function economy.waitlist_client() returns text
  language plpgsql stable security definer set search_path = '' as $$
declare
  v_headers jsonb := nullif(current_setting('request.headers', true), '')::jsonb;
begin
  if v_headers is null then
    return null;
  end if;
  return economy.waitlist_client_of(coalesce(nullif(btrim(v_headers ->> 'cf-connecting-ip'), ''),
                                             nullif(btrim(split_part(v_headers ->> 'x-forwarded-for', ',', 1)), ''),
                                             nullif(btrim(v_headers ->> 'x-real-ip'), '')));
end $$;
revoke all on function economy.waitlist_client() from public;

-- 등록 한 번 — 같은 곳 횟수 · 이메일 규칙 · 저장(0082 waitlist_register 의 몸통)
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

-- 공개 등록(홈페이지가 바로 부른다) — 사람 확인을 켜면 받지 않는다
create or replace function public.waitlist_register(p_email text, p_consent boolean, p_trap text default '', p_ref text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
begin
  if coalesce((select value = 'true'::jsonb from public.economy_settings where key = 'waitlist_require_turnstile'), false) then
    return jsonb_build_object('ok', false, 'message', 'verification required');
  end if;
  return economy.waitlist_register_core(p_email, p_consent, p_trap, p_ref, economy.waitlist_client());
end;
$$;
revoke execute on function public.waitlist_register(text, boolean, text, text) from public, authenticated;
grant execute on function public.waitlist_register(text, boolean, text, text) to anon;

-- 워커가 Turnstile 을 확인한 뒤 — 어테스터만. 부른 사람의 IP 는 워커가 넘긴다(여기서 해시해 센다)
create or replace function public.attester_waitlist_register(p_email text, p_consent boolean, p_trap text, p_ref text, p_ip text)
returns jsonb
language plpgsql security definer set search_path = public, economy
as $$
begin
  perform economy.attester_guard();
  return economy.waitlist_register_core(p_email, p_consent, coalesce(p_trap, ''), coalesce(p_ref, ''), economy.waitlist_client_of(p_ip));
end;
$$;
revoke all on function public.attester_waitlist_register(text, boolean, text, text, text) from public, anon;
grant execute on function public.attester_waitlist_register(text, boolean, text, text, text) to authenticated;

