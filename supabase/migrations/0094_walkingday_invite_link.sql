-- ════════════════════════════════════════════════════════════════════
--  0094 — 11.11 워킹데이 「내 초대 링크」 (2026-11-07)
--
--  초대 링크(?ref=코드)는 홈페이지 사전 등록 완료 화면에서만 보여서, 이미 등록한 사람은 자기 링크를 찾을 곳이 없었다.
--  이벤트 인증 페이지(/11events/proof#invite)에서 사전 등록한 이메일을 넣으면 내 코드와 초대 현황을 돌려준다.
--  · invited: 내 링크로 사전 등록한 친구 수 · counted: 그중 11,111걸음 이상 하루라도 승인된 친구 수(응모권 · 초대 순위에 들어가는 수)
--  · 같은 곳(IP 해시) 제한은 걸음 인증 · SNS 제출과 함께 센다(10분 10 · 하루 40).
-- ════════════════════════════════════════════════════════════════════

create or replace function public.walkingday_invite_link(p_email text, p_trap text default '')
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
  select w.id, w.referral_code into v_id, v_code from public.waitlist_entries w where w.email = v_email and w.status = 'registered';
  if v_id is null or v_code is null then
    return jsonb_build_object('ok', false, 'code', 'not_registered');
  end if;
  select count(*)::int,
         count(*) filter (where exists (select 1 from economy.walkingday_proofs p
                                         where p.entry_id = f.id and p.status = 'approved' and p.steps >= economy.walkingday_goal()))::int
    into v_invited, v_counted
    from public.waitlist_entries f where f.referred_by = v_id and f.status = 'registered';
  return jsonb_build_object('ok', true, 'code', v_code, 'invited', v_invited, 'counted', v_counted);
end;
$$;
revoke all on function public.walkingday_invite_link(text, text) from public;
grant execute on function public.walkingday_invite_link(text, text) to anon, authenticated;
