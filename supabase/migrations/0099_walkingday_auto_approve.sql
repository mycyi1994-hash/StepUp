-- ════════════════════════════════════════════════════════════════════
--  0099 — 11.11 워킹데이 제출 자동 승인 (2026-11-07)
--
--  사용자 "검수페이지에 자동으로 승인되게해줘" — 걸음 인증 · SNS 게시물을 내는 즉시 '승인'으로 둔다(확인자 '자동').
--  그래서 응모권 · 친구 초대 순위(0096, 승인된 친구 수)가 담당자 확인을 기다리지 않고 바로 반영된다.
--  · 담당자는 검수 페이지 '승인' 탭에서 이상한 제출을 거절할 수 있다(거절은 응모권 · 순위에서 빠진다).
--  · 담당자가 거절한 날을 다시 내면 자동 승인하지 않고 '대기'로 둔다 — 담당자가 다시 본다.
--  · 담당자가 '대기로 되돌리기' 한 줄도 그대로 대기(제출 시각이 바뀔 때만 자동 승인).
--  · 자동 승인된 날은 다시 내서 고칠 수 있다(담당자가 직접 승인한 날만 'approved' 로 막는다).
--  · 끄기: update public.economy_settings set value = 'false'::jsonb where key = 'walkingday_auto_approve';
--  · 이미 들어와 '대기'인 제출도 이 마이그레이션에서 한 번 승인한다.
--  · 제출 함수 반환에 실제 상태(status)를 담는다(SNS 는 새로 추가) — 인증 페이지가 '바로 반영' / '확인 뒤 반영' 을 고른다.
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values ('walkingday_auto_approve', 'true'::jsonb)
on conflict (key) do nothing;

create or replace function economy.walkingday_auto_approve() returns boolean
language sql stable security definer set search_path = ''
as $$
  select coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'walkingday_auto_approve'), false)
$$;
revoke all on function economy.walkingday_auto_approve() from public;

-- 새 제출(INSERT) · 다시 낸 제출(제출 시각이 바뀐 UPDATE, 앞 상태가 거절이 아님)만 '대기' → '승인'
create or replace function economy.walkingday_auto_approve_row() returns trigger
language plpgsql security definer set search_path = ''
as $$
begin
  if new.status = 'pending' and economy.walkingday_auto_approve()
     and (tg_op = 'INSERT' or (new.submitted_at is distinct from old.submitted_at and old.status <> 'rejected')) then
    new.status := 'approved';
    new.reviewed_at := now();
    new.reviewed_by := '자동';
  end if;
  return new;
end;
$$;
revoke all on function economy.walkingday_auto_approve_row() from public;

drop trigger if exists walkingday_proofs_auto_approve on economy.walkingday_proofs;
create trigger walkingday_proofs_auto_approve before insert or update on economy.walkingday_proofs
  for each row execute function economy.walkingday_auto_approve_row();
drop trigger if exists walkingday_sns_auto_approve on economy.walkingday_sns;
create trigger walkingday_sns_auto_approve before insert or update on economy.walkingday_sns
  for each row execute function economy.walkingday_auto_approve_row();

-- 이미 들어와 기다리던 제출도 승인(설정이 켜져 있을 때만)
update economy.walkingday_proofs set status = 'approved', reviewed_at = now(), reviewed_by = '자동'
 where status = 'pending' and economy.walkingday_auto_approve();
update economy.walkingday_sns set status = 'approved', reviewed_at = now(), reviewed_by = '자동'
 where status = 'pending' and economy.walkingday_auto_approve();

-- 제출 함수: 0090 · 0093 그대로 + 위 주석의 바뀐 점
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
  -- 자동 승인(0099)된 날은 다시 내서 고칠 수 있다. 담당자가 직접 승인한 날만 막는다
  if found and v_old.status = 'approved' and v_old.reviewed_by is distinct from '자동' then
    return jsonb_build_object('ok', false, 'code', 'approved');
  end if;
  insert into economy.walkingday_proofs (entry_id, epoch_day, steps, image, client)
  values (v_entry, p_day, p_steps, p_image, v_client)
  on conflict (entry_id, epoch_day) do update
    set steps = excluded.steps, image = excluded.image, client = excluded.client, status = 'pending',
        note = null, submitted_at = now(), reviewed_at = null, reviewed_by = null
  returning status into v_status;
  return jsonb_build_object('ok', true, 'day', p_day, 'status', v_status);
end;
$$;
revoke all on function public.walkingday_proof_submit(text, int, int, text, boolean, text) from public;
grant execute on function public.walkingday_proof_submit(text, int, int, text, boolean, text) to anon, authenticated;

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
  on conflict (channel, post_id) do nothing
  returning status into v_status;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'duplicate');
  end if;
  return jsonb_build_object('ok', true, 'channel', v_ch, 'account', v_account, 'today', v_day + 1, 'total', v_all + 1, 'status', v_status);
end;
$$;
revoke all on function public.walkingday_sns_submit(text, text, text, boolean, text) from public;
grant execute on function public.walkingday_sns_submit(text, text, text, boolean, text) to anon, authenticated;
