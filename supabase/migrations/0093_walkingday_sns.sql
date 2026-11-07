-- ════════════════════════════════════════════════════════════════════
--  0093 — 11.11 워킹데이 SNS 게시물 링크 제출 (2026-11-07)
--
--  해시태그 검색만으로는 인스타 게시물을 놓치고(인기 게시물만 보인다) SNS 계정과 이메일을 이을 수 없었다.
--  이제 참가자가 인증 페이지(/11events/proof#sns)에서 이메일 + 게시물 링크(X · 인스타 · 스레드)를 낸다.
--  · 제출(누구나): 이메일(사전 등록 안 해도 됨 — SNS만 참여해도 당첨 대상) · 게시물 링크 · 인스타는 계정 아이디.
--    기간은 걸음 인증과 같다(walkingday_open_from ~ walkingday_open_until). 같은 곳(IP 해시) 제한도 걸음 인증과 함께 센다.
--    한 이메일 하루 3개 · 전체 15개(거절된 것은 세지 않는다). 같은 게시물은 한 번만.
--  · 승인(검토 페이지): 담당자가 링크를 열어 해시태그 4개 · 전체 공개를 확인하고 승인 · 거절.
--  · 명단(walkingday_sns_roster): 승인된 게시물을 이메일 · 채널 · 계정마다 묶어 추첨기 SNS 명단 양식으로 내려받는다.
-- ════════════════════════════════════════════════════════════════════

create table if not exists economy.walkingday_sns (
  id bigint generated always as identity primary key,
  email text not null check (length(email) <= 254),
  channel text not null check (channel in ('x', 'instagram', 'threads')),
  account text not null check (account ~ '^[a-z0-9._]{1,30}$'),
  post_id text not null check (length(post_id) between 1 and 64),
  url text not null check (length(url) <= 300),
  epoch_day int not null,
  status text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
  note text check (note is null or length(note) <= 200),
  client text,
  submitted_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by text,
  unique (channel, post_id)
);
create index if not exists walkingday_sns_status on economy.walkingday_sns (status, id);
create index if not exists walkingday_sns_email on economy.walkingday_sns (email, epoch_day);
alter table economy.walkingday_sns enable row level security;
revoke all on economy.walkingday_sns from public;

-- 링크 → 채널 · 계정 · 게시물 번호 · 정리한 주소. 모르는 주소면 null
create or replace function economy.walkingday_sns_parse(p_url text)
returns table (channel text, account text, post_id text, url text)
language plpgsql immutable set search_path = ''
as $$
declare
  v text := btrim(coalesce(p_url, ''));
  m text[];
begin
  v := regexp_replace(v, '[?#].*$', '');
  v := regexp_replace(v, '/+$', '');
  if v !~* '^https?://' then v := 'https://' || v; end if;
  m := regexp_match(v, '^https?://(?:www\.|mobile\.|m\.)?(?:x|twitter)\.com/([A-Za-z0-9_]{1,15})/status/([0-9]{5,25})(?:/(?:photo|video)/[0-9]+)?$', 'i');
  if m is not null and lower(m[1]) <> 'i' then
    return query select 'x'::text, lower(m[1]), m[2], 'https://x.com/' || m[1] || '/status/' || m[2];
    return;
  end if;
  m := regexp_match(v, '^https?://(?:www\.|mobile\.|m\.)?(?:x|twitter)\.com/i/(?:web/)?status/([0-9]{5,25})(?:/(?:photo|video)/[0-9]+)?$', 'i');
  if m is not null then
    return query select 'x'::text, null::text, m[1], 'https://x.com/i/status/' || m[1];
    return;
  end if;
  m := regexp_match(v, '^https?://(?:www\.|m\.)?instagram\.com/(?:[A-Za-z0-9._]{1,30}/)?(?:p|reel|reels|tv)/([A-Za-z0-9_-]{5,64})$', 'i');
  if m is not null then
    return query select 'instagram'::text, null::text, m[1], 'https://www.instagram.com/p/' || m[1] || '/';
    return;
  end if;
  m := regexp_match(v, '^https?://(?:www\.)?threads\.(?:net|com)/@([A-Za-z0-9._]{1,30})/post/([A-Za-z0-9_-]{5,64})(?:/media)?$', 'i');
  if m is not null then
    return query select 'threads'::text, lower(m[1]), m[2], 'https://www.threads.com/@' || lower(m[1]) || '/post/' || m[2];
    return;
  end if;
end $$;
revoke all on function economy.walkingday_sns_parse(text) from public;

-- 계정 아이디 정리: @ · 주소 앞부분을 떼고 소문자. 형식이 틀리면 null
create or replace function economy.walkingday_sns_account(p text) returns text
language sql immutable set search_path = '' as $$
  select case when x ~ '^[a-z0-9._]{1,30}$' then x end
  from (select lower(regexp_replace(regexp_replace(btrim(coalesce(p, '')), '^(https?://)?(www\.)?(instagram\.com|threads\.(net|com)|x\.com)/', '', 'i'), '^@|/+$', '', 'g')) as x) s
$$;
revoke all on function economy.walkingday_sns_account(text) from public;

-- ── 제출 ────────────────────────────────────────────────────────────
-- 답: {ok:true, channel, account, today, total} 또는 {ok:false, code}
--   code: closed · busy · consent · email · url · account · duplicate · day_limit · limit
create or replace function public.walkingday_sns_submit(p_email text, p_url text, p_account text, p_consent boolean, p_trap text default '')
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
  select x.channel, x.account, x.post_id, x.url into v_ch, v_acct0, v_pid, v_url from economy.walkingday_sns_parse(p_url) x;
  if v_ch is null then
    return jsonb_build_object('ok', false, 'code', 'url');
  end if;
  v_account := coalesce(v_acct0, economy.walkingday_sns_account(p_account));
  if v_account is null then
    return jsonb_build_object('ok', false, 'code', 'account');
  end if;

  -- 같은 이메일끼리 한 줄씩(하루 · 전체 한도를 동시에 넘지 않게)
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
  on conflict (channel, post_id) do nothing;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'duplicate');
  end if;
  return jsonb_build_object('ok', true, 'channel', v_ch, 'account', v_account, 'today', v_day + 1, 'total', v_all + 1);
end;
$$;
revoke all on function public.walkingday_sns_submit(text, text, text, boolean, text) from public;
grant execute on function public.walkingday_sns_submit(text, text, text, boolean, text) to anon, authenticated;

-- ── 검토 ────────────────────────────────────────────────────────────
-- 목록(오래된 것부터, p_after 다음 번호부터 최대 20개). registered: 사전 등록한 이메일인지. dup_client: 같은 곳(IP)에서 낸 다른 이메일 수
create or replace function public.walkingday_sns_list(p_token text, p_status text default 'pending', p_after bigint default 0)
returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.id) from (
    select s.id, s.email, s.channel, s.account, s.url, s.epoch_day as day, s.status, s.note, s.submitted_at, s.reviewed_by,
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

-- 승인 · 거절 · 대기로. 계정 아이디를 고칠 수 있다(인스타는 링크에 계정이 없다)
create or replace function public.walkingday_sns_set(p_token text, p_id bigint, p_status text, p_note text default null, p_account text default null)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare
  v_label text := economy.walkingday_reviewer(p_token);
  v_account text := case when nullif(btrim(coalesce(p_account, '')), '') is null then null else economy.walkingday_sns_account(p_account) end;
begin
  if p_status not in ('approved', 'rejected', 'pending') then
    return jsonb_build_object('ok', false, 'code', 'status');
  end if;
  if nullif(btrim(coalesce(p_account, '')), '') is not null and v_account is null then
    return jsonb_build_object('ok', false, 'code', 'account');
  end if;
  update economy.walkingday_sns
     set status = p_status, account = coalesce(v_account, account),
         note = left(nullif(btrim(coalesce(p_note, '')), ''), 200),
         reviewed_at = case when p_status = 'pending' then null else now() end,
         reviewed_by = case when p_status = 'pending' then null else v_label end
   where id = p_id;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'missing');
  end if;
  return jsonb_build_object('ok', true);
end $$;
revoke all on function public.walkingday_sns_set(text, bigint, text, text, text) from public;
grant execute on function public.walkingday_sns_set(text, bigint, text, text, text) to anon, authenticated;

-- 추첨기 SNS 명단: 승인된 게시물을 이메일 · 채널 · 계정마다 묶는다(인정 게시물 수 1~15)
create or replace function public.walkingday_sns_roster(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.email, x.channel, x.account) from (
    select s.email, s.channel, s.account, min(s.url) as url, least(count(*), 15)::int as posts,
           (array_agg(s.reviewed_by order by s.reviewed_at desc))[1] as reviewed_by, max(s.reviewed_at) as reviewed_at
    from economy.walkingday_sns s where s.status = 'approved'
    group by s.email, s.channel, s.account) x), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_sns_roster(text) from public;
grant execute on function public.walkingday_sns_roster(text) to anon, authenticated;

-- 검토 요약에 SNS 수를 더한다(반환 jsonb 그대로)
create or replace function public.walkingday_review_summary(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return (select jsonb_build_object(
    'pending', count(*) filter (where status = 'pending'),
    'approved', count(*) filter (where status = 'approved'),
    'rejected', count(*) filter (where status = 'rejected'),
    'total', coalesce(sum(steps) filter (where status = 'approved'), 0),
    'people', count(distinct entry_id) filter (where status = 'approved'),
    'registered', (select count(*) from public.waitlist_entries where status = 'registered'),
    'sns_pending', (select count(*) from economy.walkingday_sns where status = 'pending'),
    'sns_approved', (select count(*) from economy.walkingday_sns where status = 'approved'))
    from economy.walkingday_proofs);
end $$;
revoke all on function public.walkingday_review_summary(text) from public;
grant execute on function public.walkingday_review_summary(text) to anon, authenticated;
