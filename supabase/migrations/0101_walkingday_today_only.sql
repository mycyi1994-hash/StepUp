-- ════════════════════════════════════════════════════════════════════
--  0101 — 11.11 워킹데이 걸음 인증은 오늘 날짜만 (2026-11-09)
--
--  사용자 "저거 날짜 지난거 입력 못하게 해줘" — 지난 날짜로 새로 내거나 다시 내면 'day' 로 거절한다(한국 날짜 기준).
--  이미 들어온 지난 날짜 제출은 그대로 둔다.
--  끄기: update public.economy_settings set value = 'false'::jsonb where key = 'walkingday_today_only';
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values ('walkingday_today_only', 'true'::jsonb)
on conflict (key) do nothing;

create or replace function economy.walkingday_today_only() returns boolean
language sql stable security definer set search_path = ''
as $$
  select coalesce((select (value #>> '{}')::boolean from public.economy_settings where key = 'walkingday_today_only'), false)
$$;
revoke all on function economy.walkingday_today_only() from public;

-- 제출 함수: 0100 그대로 + 날짜 검사 한 줄
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
  if p_day is null or p_day not between 20764 and 20768 or p_day > economy.walkingday_today()
     or (p_day < economy.walkingday_today() and economy.walkingday_today_only()) then
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
