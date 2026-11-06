-- ════════════════════════════════════════════════════════════════════
--  0090 — 11.11 워킹데이: 걸음 인증 제출 · 승인 · 공개 합계 · 응모권 (2026-11-05)
--
--  이벤트 페이지 stepupcrew.com/11events(web/11events). 사용자 결정 "아무 걸음 앱이든 인증만 하면 된다".
--  · 제출(누구나, 홈페이지): 사전 등록한 이메일 · 걸은 날(11/7 ~ 11/11) · 걸음 수 · 날짜가 보이는 캡처(작게 줄인 JPEG 글).
--    11/7 0시 ~ 11/12 23:59(한국 시간)만 받는다. 같은 날을 다시 내면 승인 전까지는 바꾼다. 같은 곳(IP 해시)에서 10분 10 · 하루 40.
--  · 승인(검토 페이지 web/11events/review.html): 운영자가 SQL 로 만든 검토 키로만. 키는 해시만 남는다.
--      select economy.walkingday_reviewer_new('이름');   -- 한 번만 보이는 키를 돌려준다
--  · 공개 합계 walkingday_total(): 승인된 걸음의 합 — 이벤트 페이지가 60초마다 읽는다(개인 값은 나가지 않는다).
--  · 응모권(검토 키로 보는 명단): 사전 등록 1 + 11,111걸음 승인 하루라도 3 + 11/11 당일 11,111걸음 승인 3
--    + 친구 초대(내 링크로 등록한 친구가 11,111걸음 승인을 받으면 친구 한 명마다 나도 2, 친구도 2).
--  · 캡처는 이벤트가 끝나면 지운다: select economy.walkingday_purge_images();  (발표 뒤 30일 안 — 동의 문구와 같게)
-- ════════════════════════════════════════════════════════════════════

-- 2026-11-07(KST) = epoch day 20764 · 11-11 = 20768
create table if not exists economy.walkingday_proofs (
  id bigint generated always as identity primary key,
  entry_id uuid not null references public.waitlist_entries(id) on delete cascade,
  epoch_day int not null check (epoch_day between 20764 and 20768),
  steps int not null check (steps between 1 and 100000),
  image text check (image is null or length(image) between 100 and 600000),
  status text not null default 'pending' check (status in ('pending', 'approved', 'rejected')),
  note text check (note is null or length(note) <= 200),
  client text,
  submitted_at timestamptz not null default now(),
  reviewed_at timestamptz,
  reviewed_by text,
  unique (entry_id, epoch_day)
);
create index if not exists walkingday_proofs_status on economy.walkingday_proofs (status, submitted_at);
alter table economy.walkingday_proofs enable row level security;
revoke all on economy.walkingday_proofs from public;

create table if not exists economy.walkingday_attempts (
  ip_hash text not null,
  at timestamptz not null default now()
);
create index if not exists walkingday_attempts_ip on economy.walkingday_attempts (ip_hash, at);
alter table economy.walkingday_attempts enable row level security;
revoke all on economy.walkingday_attempts from public;

create table if not exists economy.walkingday_reviewers (
  token_hash text primary key,
  label text not null,
  created_at timestamptz not null default now(),
  revoked_at timestamptz
);
alter table economy.walkingday_reviewers enable row level security;
revoke all on economy.walkingday_reviewers from public;

insert into public.economy_settings (key, value) values
  ('walkingday_open_from', '"2026-11-07T00:00:00+09:00"'::jsonb),
  ('walkingday_open_until', '"2026-11-13T00:00:00+09:00"'::jsonb),
  ('walkingday_goal_steps', '11111'::jsonb)
on conflict (key) do nothing;

-- 한국 날짜(epoch day)
create or replace function economy.walkingday_today() returns int
  language sql stable set search_path = '' as $$
  select ((now() at time zone 'Asia/Seoul')::date - date '1970-01-01')::int
$$;
revoke all on function economy.walkingday_today() from public;

create or replace function economy.walkingday_goal() returns int
  language sql stable set search_path = '' as $$
  select coalesce((select (value #>> '{}')::int from public.economy_settings where key = 'walkingday_goal_steps'), 11111)
$$;
revoke all on function economy.walkingday_goal() from public;

-- ── 제출 ────────────────────────────────────────────────────────────
-- 답: {ok:true, day, status:'pending'} 또는 {ok:false, code} — code: closed · busy · consent · email · not_registered · day · steps · image · approved
create or replace function public.walkingday_proof_submit(p_email text, p_day int, p_steps int, p_image text,
                                                          p_consent boolean, p_trap text default '')
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
  if found and v_old.status = 'approved' then
    return jsonb_build_object('ok', false, 'code', 'approved');
  end if;
  insert into economy.walkingday_proofs (entry_id, epoch_day, steps, image, client)
  values (v_entry, p_day, p_steps, p_image, v_client)
  on conflict (entry_id, epoch_day) do update
    set steps = excluded.steps, image = excluded.image, client = excluded.client, status = 'pending',
        note = null, submitted_at = now(), reviewed_at = null, reviewed_by = null;
  return jsonb_build_object('ok', true, 'day', p_day, 'status', 'pending');
end;
$$;
revoke all on function public.walkingday_proof_submit(text, int, int, text, boolean, text) from public;
grant execute on function public.walkingday_proof_submit(text, int, int, text, boolean, text) to anon, authenticated;

-- ── 공개 합계 ───────────────────────────────────────────────────────
create or replace function public.walkingday_total()
returns table (total bigint, people int, updated_at timestamptz)
language sql stable security definer set search_path = ''
as $$
  select coalesce(sum(p.steps), 0)::bigint, count(distinct p.entry_id)::int, max(p.reviewed_at)
  from economy.walkingday_proofs p where p.status = 'approved'
$$;
revoke all on function public.walkingday_total() from public;
grant execute on function public.walkingday_total() to anon, authenticated;

-- ── 검토 키 ─────────────────────────────────────────────────────────
-- SQL Editor(운영자)에서만: 새 키를 한 번 보여 준다
create or replace function economy.walkingday_reviewer_new(p_label text) returns text
language plpgsql security definer set search_path = ''
as $$
declare v_token text := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
begin
  insert into economy.walkingday_reviewers (token_hash, label)
  values (encode(sha256(convert_to(v_token, 'UTF8')), 'hex'), coalesce(nullif(btrim(p_label), ''), 'reviewer'));
  return v_token;
end $$;
revoke all on function economy.walkingday_reviewer_new(text) from public;

-- 키가 맞으면 그 이름, 아니면 예외
create or replace function economy.walkingday_reviewer(p_token text) returns text
language plpgsql stable security definer set search_path = ''
as $$
declare v_label text;
begin
  select label into v_label from economy.walkingday_reviewers
   where token_hash = encode(sha256(convert_to(coalesce(p_token, ''), 'UTF8')), 'hex') and revoked_at is null;
  if v_label is null then
    raise exception 'review key required' using errcode = '42501';
  end if;
  return v_label;
end $$;
revoke all on function economy.walkingday_reviewer(text) from public;

-- 응모권 — 사전 등록한 사람마다
create or replace function economy.walkingday_tickets()
returns table (entry_id uuid, email text, registered_at timestamptz, approved_days int, best_steps int,
               goal_met boolean, goal_on_1111 boolean, invited_by text, friends int, tickets int)
language sql stable security definer set search_path = ''
as $$
  with q as (
    select p.entry_id,
           count(*) filter (where p.status = 'approved')::int as approved_days,
           max(p.steps) filter (where p.status = 'approved') as best_steps,
           bool_or(p.status = 'approved' and p.steps >= economy.walkingday_goal()) as goal_met,
           bool_or(p.status = 'approved' and p.steps >= economy.walkingday_goal() and p.epoch_day = 20768) as goal_on_1111
    from economy.walkingday_proofs p group by p.entry_id
  ), e as (
    select w.id, w.email, w.created_at, w.referred_by,
           coalesce(q.approved_days, 0) as approved_days, q.best_steps,
           coalesce(q.goal_met, false) as goal_met, coalesce(q.goal_on_1111, false) as goal_on_1111
    from public.waitlist_entries w left join q on q.entry_id = w.id
    where w.status = 'registered'
  ), f as (
    select e.referred_by as id, count(*)::int as friends from e where e.referred_by is not null and e.goal_met group by e.referred_by
  )
  select e.id, e.email, e.created_at, e.approved_days, e.best_steps, e.goal_met, e.goal_on_1111,
         r.email, coalesce(f.friends, 0),
         (1 + case when e.goal_met then 3 else 0 end + case when e.goal_on_1111 then 3 else 0 end
            + 2 * coalesce(f.friends, 0) + case when e.goal_met and r.id is not null then 2 else 0 end)::int
  from e
  left join f on f.id = e.id
  left join public.waitlist_entries r on r.id = e.referred_by and r.status = 'registered'
$$;
revoke all on function economy.walkingday_tickets() from public;

-- ── 검토 페이지 ─────────────────────────────────────────────────────
-- 요약: 상태별 수 · 합계 · 사람
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
    'registered', (select count(*) from public.waitlist_entries where status = 'registered'))
    from economy.walkingday_proofs);
end $$;
revoke all on function public.walkingday_review_summary(text) from public;
grant execute on function public.walkingday_review_summary(text) to anon, authenticated;

-- 목록(오래된 것부터, p_after 다음 번호부터 최대 10개). 같은 IP 에서 낸 다른 이메일 수(dup_client)를 함께 — 다중 계정 확인용
create or replace function public.walkingday_review_list(p_token text, p_status text default 'pending', p_after bigint default 0)
returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(x) order by x.id) from (
    select p.id, w.email, p.epoch_day as day, p.steps, p.image, p.status, p.note, p.submitted_at, p.reviewed_by,
           (select count(distinct p2.entry_id) from economy.walkingday_proofs p2
             where p.client is not null and p2.client = p.client and p2.entry_id <> p.entry_id)::int as dup_client
    from economy.walkingday_proofs p join public.waitlist_entries w on w.id = p.entry_id
    where p.status = coalesce(p_status, 'pending') and p.id > coalesce(p_after, 0)
    order by p.id limit 10) x), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_review_list(text, text, bigint) from public;
grant execute on function public.walkingday_review_list(text, text, bigint) to anon, authenticated;

-- 승인 · 거절. 승인 때 걸음 수를 캡처에 맞게 고칠 수 있다
create or replace function public.walkingday_review_set(p_token text, p_id bigint, p_status text, p_steps int default null, p_note text default null)
returns jsonb
language plpgsql security definer set search_path = ''
as $$
declare v_label text := economy.walkingday_reviewer(p_token);
begin
  if p_status not in ('approved', 'rejected', 'pending') then
    return jsonb_build_object('ok', false, 'code', 'status');
  end if;
  if p_steps is not null and p_steps not between 1 and 100000 then
    return jsonb_build_object('ok', false, 'code', 'steps');
  end if;
  update economy.walkingday_proofs
     set status = p_status, steps = coalesce(p_steps, steps), note = left(nullif(btrim(coalesce(p_note, '')), ''), 200),
         reviewed_at = case when p_status = 'pending' then null else now() end,
         reviewed_by = case when p_status = 'pending' then null else v_label end
   where id = p_id;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'missing');
  end if;
  return jsonb_build_object('ok', true);
end $$;
revoke all on function public.walkingday_review_set(text, bigint, text, int, text) from public;
grant execute on function public.walkingday_review_set(text, bigint, text, int, text) to anon, authenticated;

-- 응모권 명단(초대 많은 순 → 응모권 많은 순). 사전 등록만 한 사람(1장)도 들어간다
create or replace function public.walkingday_review_entries(p_token text) returns jsonb
language plpgsql stable security definer set search_path = ''
as $$
begin
  perform economy.walkingday_reviewer(p_token);
  return coalesce((select jsonb_agg(row_to_json(t) order by t.friends desc, t.tickets desc, t.registered_at)
                   from economy.walkingday_tickets() t), '[]'::jsonb);
end $$;
revoke all on function public.walkingday_review_entries(text) from public;
grant execute on function public.walkingday_review_entries(text) to anon, authenticated;

-- 이벤트가 끝난 뒤 캡처만 지운다(걸음 수 · 상태는 남는다)
create or replace function economy.walkingday_purge_images() returns int
language plpgsql security definer set search_path = ''
as $$
declare n int;
begin
  update economy.walkingday_proofs set image = null where image is not null;
  get diagnostics n = row_count;
  return n;
end $$;
revoke all on function economy.walkingday_purge_images() from public;

-- 첫 검토 키(대표) — 원문은 저장소에 두지 않고 사용자에게 따로 전했다. 막으려면:
--   update economy.walkingday_reviewers set revoked_at = now() where label = '대표';
insert into economy.walkingday_reviewers (token_hash, label)
values ('c6dfc6970499c02ac23580a579d80006d107da55fc6ddfe206068cdff80f5064', '대표')
on conflict (token_hash) do nothing;
