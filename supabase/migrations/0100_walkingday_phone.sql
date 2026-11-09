-- ════════════════════════════════════════════════════════════════════
--  0100 — 11.11 워킹데이 전화번호 (2026-11-09)
--
--  사용자 "탭 3개(걸음 인증 · SNS 게시물 · 친구 초대) 전부 전화번호 받아야 해 · supabase 에도 연동".
--  경품(빼빼로 기프티콘 · 배송)을 보낼 연락처. 이메일과 짝으로 economy.walkingday_contacts 에 쌓는다.
--  · 세 함수에 p_phone(+ 친구 초대는 p_consent)을 더했다. 휴대폰 번호(010 등)만, 저장은 010-1234-5678 모양.
--  · 성공한 제출 · 조회 때만 남긴다. 한 이메일에 번호가 여럿이면 모두 남긴다(덮어쓰지 않음 —
--    남의 이메일로 번호를 바꿔 경품을 가로채지 못하게. 여럿이면 검토 페이지가 최근 번호부터 다 보인다).
--  · 꼭 받기는 설정 walkingday_require_phone(기본 true). 끄면 번호 없이도 받는다(옛 화면 · 검사용).
--  · 검토 페이지: 인증 · SNS 카드와 응모권 명단 · SNS 명단(CSV 맨 끝 칸)에 전화번호.
--  · 끝나면 지운다(경품을 다 보낸 뒤, 발표 뒤 30일 안 — 동의 문구와 같게): truncate economy.walkingday_contacts;
--  · 함수 인자가 바뀌어 옛 모양은 지운다. setup.sql 은 배포마다 0090 · 0093 · 0094 · 0099 를 다시 올려 옛 모양을
--    다시 만들므로 여기서 매번 지운다(같은 이름이 둘이면 PostgREST 가 어느 것을 부를지 못 고른다).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values ('walkingday_require_phone', 'true'::jsonb)
on conflict (key) do nothing;

create table if not exists economy.walkingday_contacts (
  email text not null check (length(email) <= 254),
  phone text not null check (phone ~ '^01[016789]-[0-9]{3,4}-[0-9]{4}$'),
  source text not null check (source in ('steps', 'sns', 'invite')),
  first_at timestamptz not null default now(),
  last_at timestamptz not null default now(),
  primary key (email, phone)
);
create index if not exists walkingday_contacts_last on economy.walkingday_contacts (email, last_at desc);
alter table economy.walkingday_contacts enable row level security;
revoke all on economy.walkingday_contacts from public;

-- 휴대폰 번호 → '010-1234-5678'(틀리면 null). 띄어쓰기 · 하이픈 · 점 · +82 를 받는다
create or replace function economy.walkingday_phone(p text) returns text
language plpgsql immutable set search_path = ''
as $$
declare d text := regexp_replace(coalesce(p, ''), '[^0-9]', '', 'g');
begin
  if length(coalesce(p, '')) > 40 then return null; end if;
  if d ~ '^82' and length(d) between 11 and 12 then d := '0' || substr(d, 3); end if;
  if d ~ '^01[016789][0-9]{8}$' then return substr(d, 1, 3) || '-' || substr(d, 4, 4) || '-' || substr(d, 8, 4); end if;
  if d ~ '^01[16789][0-9]{7}$' then return substr(d, 1, 3) || '-' || substr(d, 4, 3) || '-' || substr(d, 7, 4); end if;
  return null;
end $$;
revoke all on function economy.walkingday_phone(text) from public;

create or replace function economy.walkingday_require_phone() returns boolean
language sql stable security definer set search_path = ''
as $$
  select coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'walkingday_require_phone'), true)
$$;
revoke all on function economy.walkingday_require_phone() from public;

-- 번호 확인: null = 통과(번호 있음 또는 꼭 받기 꺼짐), 'phone' = 거절
create or replace function economy.walkingday_phone_refusal(p_phone text) returns text
language sql stable security definer set search_path = ''
as $$
  select case
    when economy.walkingday_phone(p_phone) is not null then null
    when nullif(btrim(coalesce(p_phone, '')), '') is not null then 'phone'
    when economy.walkingday_require_phone() then 'phone'
  end
$$;
revoke all on function economy.walkingday_phone_refusal(text) from public;

create or replace function economy.walkingday_contact_save(p_email text, p_phone text, p_source text) returns void
language plpgsql security definer set search_path = ''
as $$
declare v_phone text := economy.walkingday_phone(p_phone);
begin
  if v_phone is null then return; end if;
  insert into economy.walkingday_contacts (email, phone, source, last_at) values (p_email, v_phone, p_source, clock_timestamp())
  on conflict (email, phone) do update set last_at = excluded.last_at, source = excluded.source;
end $$;
revoke all on function economy.walkingday_contact_save(text, text, text) from public;

-- 이메일의 번호들(최근 것부터, 쉼표로)
create or replace function economy.walkingday_phones(p_email text) returns text
language sql stable security definer set search_path = ''
as $$
  select string_agg(c.phone, ', ' order by c.last_at desc) from economy.walkingday_contacts c where c.email = p_email
$$;
revoke all on function economy.walkingday_phones(text) from public;

-- ── 제출 함수: 0099 · 0094 그대로 + 전화번호 ─────────────────────────
drop function if exists public.walkingday_proof_submit(text, int, int, text, boolean, text);
drop function if exists public.walkingday_sns_submit(text, text, text, boolean, text);
drop function if exists public.walkingday_invite_link(text, text);

create or replace function public.walkingday_proof_submit(p_email text, p_day int, p_steps int, p_image text,
                                                          p_consent boolean, p_trap text default '', p_phone text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_entry uuid;
  v_from timestamptz := coalesce((select (value #>> '{}')::timestamptz from public.economy_settings where key = 'walkingday_open_from'),
                                 '2026-11-07T00:00:00+09:00');
  v_until timestamptz := coalesce((select (value #>> '{}')::timestamptz from public.economy_settings where key = 'walkingday_open_until'),
                                  '2026-11-13T00:00:00+09:00');
  v_old economy.walkingday_proofs;
  v_status text;
begin
  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true, 'day', p_day, 'status', 'pending');
  end if;
  if v_client is not null then
    delete from economy.walkingday_attempts where ip_hash = v_client and at < now() - interval '1 day';
    if (select count(*) from economy.walkingday_attempts where ip_hash = v_client and at > now() - interval '10 minutes') >= 10
       or (select count(*) from economy.walkingday_attempts where ip_hash = v_client) >= 40 then
      return jsonb_build_object('ok', false, 'code', 'busy');
    end if;
    insert into economy.walkingday_attempts (ip_hash) values (v_client);
  end if;
  if now() < v_from or now() >= v_until then
    return jsonb_build_object('ok', false, 'code', 'closed');
  end if;
  if p_consent is distinct from true then
    return jsonb_build_object('ok', false, 'code', 'consent');
  end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then
    return jsonb_build_object('ok', false, 'code', 'email');
  end if;
  if economy.walkingday_phone_refusal(p_phone) is not null then
    return jsonb_build_object('ok', false, 'code', 'phone');
  end if;
  select id into v_entry from public.waitlist_entries where email = v_email and status = 'registered';
  if v_entry is null then
    return jsonb_build_object('ok', false, 'code', 'not_registered');
  end if;
  if p_steps is null or p_steps not between 1 and 100000 then
    return jsonb_build_object('ok', false, 'code', 'steps');
  end if;
  if p_image is null or length(p_image) not between 100 and 600000
     or p_image !~ '^data:image/(jpeg|png|webp);base64,[A-Za-z0-9+/]+=*$' then
    return jsonb_build_object('ok', false, 'code', 'image');
  end if;
  if p_day is null or p_day not between 20764 and 20768 or p_day > economy.walkingday_today() then
    return jsonb_build_object('ok', false, 'code', 'day');
  end if;

  select * into v_old from economy.walkingday_proofs where entry_id = v_entry and epoch_day = p_day for update;
  if found and v_old.status = 'approved' and v_old.reviewed_by is distinct from '자동' then
    return jsonb_build_object('ok', false, 'code', 'approved');
  end if;
  insert into economy.walkingday_proofs (entry_id, epoch_day, steps, image, client)
  values (v_entry, p_day, p_steps, p_image, v_client)
  on conflict (entry_id, epoch_day) do update
    set steps = excluded.steps, image = excluded.image, client = excluded.client, status = 'pending',
        note = null, submitted_at = clock_timestamp(), reviewed_at = null, reviewed_by = null
  returning status into v_status;
  perform economy.walkingday_contact_save(v_email, p_phone, 'steps');
  return jsonb_build_object('ok', true, 'day', p_day, 'status', v_status);
end;
$$;
revoke all on function public.walkingday_proof_submit(text, int, int, text, boolean, text, text) from public;
grant execute on function public.walkingday_proof_submit(text, int, int, text, boolean, text, text) to anon, authenticated;

create or replace function public.walkingday_sns_submit(p_email text, p_url text, p_account text, p_consent boolean,
                                                        p_trap text default '', p_phone text default '')
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_from timestamptz := coalesce((select (value #>> '{}')::timestamptz from public.economy_settings where key = 'walkingday_open_from'),
                                 '2026-11-07T00:00:00+09:00');
  v_until timestamptz := coalesce((select (value #>> '{}')::timestamptz from public.economy_settings where key = 'walkingday_open_until'),
                                  '2026-11-11T15:00:00+09:00');
  v_today int := economy.walkingday_today();
  v_ch text; v_acct0 text; v_pid text; v_url text;
  v_account text;
  v_day int;
  v_all int;
  v_status text;
begin
  if p_trap is distinct from '' then
    return jsonb_build_object('ok', true);
  end if;
  if v_client is not null then
    delete from economy.walkingday_attempts where ip_hash = v_client and at < now() - interval '1 day';
    if (select count(*) from economy.walkingday_attempts where ip_hash = v_client and at > now() - interval '10 minutes') >= 10
       or (select count(*) from economy.walkingday_attempts where ip_hash = v_client) >= 40 then
      return jsonb_build_object('ok', false, 'code', 'busy');
    end if;
    insert into economy.walkingday_attempts (ip_hash) values (v_client);
  end if;
  if now() < v_from or now() >= v_until then
    return jsonb_build_object('ok', false, 'code', 'closed');
  end if;
  if p_consent is distinct from true then
    return jsonb_build_object('ok', false, 'code', 'consent');
  end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then
    return jsonb_build_object('ok', false, 'code', 'email');
  end if;
  if economy.walkingday_phone_refusal(p_phone) is not null then
    return jsonb_build_object('ok', false, 'code', 'phone');
  end if;
  select x.channel, x.account, x.post_id, x.url into v_ch, v_acct0, v_pid, v_url from economy.walkingday_sns_parse(p_url) x;
  if v_ch is null then
    return jsonb_build_object('ok', false, 'code', 'url');
  end if;
  v_account := coalesce(v_acct0, economy.walkingday_sns_account(p_account));
  if v_account is null then
    return jsonb_build_object('ok', false, 'code', 'account');
  end if;

  perform pg_advisory_xact_lock(hashtext('walkingday_sns:' || v_email));
  if exists (select 1 from economy.walkingday_sns s where s.channel = v_ch and s.post_id = v_pid) then
    return jsonb_build_object('ok', false, 'code', 'duplicate');
  end if;
  select count(*) filter (where s.epoch_day = v_today), count(*) into v_day, v_all
    from economy.walkingday_sns s where s.email = v_email and s.status <> 'rejected';
  if v_all >= 15 then
    return jsonb_build_object('ok', false, 'code', 'limit');
  end if;
  if v_day >= 3 then
    return jsonb_build_object('ok', false, 'code', 'day_limit');
  end if;
  insert into economy.walkingday_sns (email, channel, account, post_id, url, epoch_day, client)
  values (v_email, v_ch, v_account, v_pid, v_url, v_today, v_client)
  on conflict (channel, post_id) do nothing
  returning status into v_status;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'duplicate');
  end if;
  perform economy.walkingday_contact_save(v_email, p_phone, 'sns');
  return jsonb_build_object('ok', true, 'channel', v_ch, 'account', v_account, 'today', v_day + 1, 'total', v_all + 1, 'status', v_status);
end;
$$;
revoke all on function public.walkingday_sns_submit(text, text, text, boolean, text, text) from public;
grant execute on function public.walkingday_sns_submit(text, text, text, boolean, text, text) to anon, authenticated;

-- 친구 초대: 번호를 받으니 동의도 받는다(꼭 받기가 꺼져 있고 번호가 없으면 예전처럼 동의 없이)
create or replace function public.walkingday_invite_link(p_email text, p_trap text default '', p_phone text default '',
                                                         p_consent boolean default false)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_client text := economy.waitlist_client();
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_id uuid;
  v_code text;
  v_invited int;
  v_counted int;
begin
  if p_trap is distinct from '' then
    return jsonb_build_object('ok', false, 'code', 'not_registered');
  end if;
  if v_client is not null then
    delete from economy.walkingday_attempts where ip_hash = v_client and at < now() - interval '1 day';
    if (select count(*) from economy.walkingday_attempts where ip_hash = v_client and at > now() - interval '10 minutes') >= 10
       or (select count(*) from economy.walkingday_attempts where ip_hash = v_client) >= 40 then
      return jsonb_build_object('ok', false, 'code', 'busy');
    end if;
    insert into economy.walkingday_attempts (ip_hash) values (v_client);
  end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then
    return jsonb_build_object('ok', false, 'code', 'email');
  end if;
  if economy.walkingday_phone_refusal(p_phone) is not null then
    return jsonb_build_object('ok', false, 'code', 'phone');
  end if;
  if economy.walkingday_phone(p_phone) is not null and p_consent is distinct from true then
    return jsonb_build_object('ok', false, 'code', 'consent');
  end if;
  select w.id, w.referral_code into v_id, v_code from public.waitlist_entries w where w.email = v_email and w.status = 'registered';
  if v_id is null or v_code is null then
    return jsonb_build_object('ok', false, 'code', 'not_registered');
  end if;
  select count(*)::int,
         count(*) filter (where exists (select 1 from economy.walkingday_proofs p
                                         where p.entry_id = f.id and p.status = 'approved' and p.steps >= economy.walkingday_goal()))::int
    into v_invited, v_counted
    from public.waitlist_entries f where f.referred_by = v_id and f.status = 'registered';
  perform economy.walkingday_contact_save(v_email, p_phone, 'invite');
  return jsonb_build_object('ok', true, 'code', v_code, 'invited', v_invited, 'counted', v_counted);
end;
$$;
revoke all on function public.walkingday_invite_link(text, text, text, boolean) from public;
grant execute on function public.walkingday_invite_link(text, text, text, boolean) to anon, authenticated;

-- ── 검토: 0090 · 0093 그대로 + phone ─────────────────────────────────
create or replace function public.walkingday_review_list(p_token text, p_status text default 'pending', p_after bigint default 0)
returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.id) from (
    select p.id, w.email, economy.walkingday_phones(w.email) as phone, p.epoch_day as day, p.steps, p.image, p.status, p.note,
           p.submitted_at, p.reviewed_by,
           (select count(distinct p2.entry_id) from economy.walkingday_proofs p2
             where p.client is not null and p2.client = p.client and p2.entry_id <> p.entry_id)::int as dup_client
    from economy.walkingday_proofs p join public.waitlist_entries w on w.id = p.entry_id
    where p.status = coalesce(p_status, 'pending') and p.id > coalesce(p_after, 0)
    order by p.id limit 10) x), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_review_list(text, text, bigint) from public;
grant execute on function public.walkingday_review_list(text, text, bigint) to anon, authenticated;

create or replace function public.walkingday_review_entries(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(to_jsonb(t) || jsonb_build_object('phone', economy.walkingday_phones(t.email))
                                    order by t.friends desc, t.tickets desc, t.registered_at)
                   from economy.walkingday_tickets() t), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_review_entries(text) from public;
grant execute on function public.walkingday_review_entries(text) to anon, authenticated;

create or replace function public.walkingday_sns_list(p_token text, p_status text default 'pending', p_after bigint default 0)
returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.id) from (
    select s.id, s.email, economy.walkingday_phones(s.email) as phone, s.channel, s.account, s.url, s.epoch_day as day, s.status, s.note,
           s.submitted_at, s.reviewed_by,
           exists (select 1 from public.waitlist_entries w where w.email = s.email and w.status = 'registered') as registered,
           (select count(distinct s2.email) from economy.walkingday_sns s2
             where s.client is not null and s2.client = s.client and s2.email <> s.email)::int as dup_client,
           (select count(*) from economy.walkingday_sns s3 where s3.email = s.email and s3.status = 'approved')::int as approved_posts
    from economy.walkingday_sns s
    where s.status = coalesce(p_status, 'pending') and s.id > coalesce(p_after, 0)
    order by s.id limit 20) x), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_sns_list(text, text, bigint) from public;
grant execute on function public.walkingday_sns_list(text, text, bigint) to anon, authenticated;

create or replace function public.walkingday_sns_roster(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.email, x.channel, x.account) from (
    select s.email, economy.walkingday_phones(s.email) as phone, s.channel, s.account, min(s.url) as url, least(count(*), 15)::int as posts,
           (array_agg(s.reviewed_by order by s.reviewed_at desc))[1] as reviewed_by, max(s.reviewed_at) as reviewed_at
    from economy.walkingday_sns s where s.status = 'approved'
    group by s.email, s.channel, s.account) x), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_sns_roster(text) from public;
grant execute on function public.walkingday_sns_roster(text) to anon, authenticated;

-- 함수 모양이 바뀌었으니 PostgREST 가 바로 새로 읽게
notify pgrst, 'reload schema';
