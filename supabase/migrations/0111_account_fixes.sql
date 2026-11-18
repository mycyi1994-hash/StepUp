-- ════════════════════════════════════════════════════════════════════
--  0111 — 계정 · 커뮤니티 점검 수정 (2026-11-17, docs/점검-2026-11-17-버그.md #23)
--
--  친구 초대 코드 "찾을 수 없어요"가 errcode P0002 였다 — PostgREST 는 P0002 를 HTTP 500 으로 보내
--  앱이 "연결 끊김 · 다시 시도"로 읽는다(초대는 지금 멈춰 있어 안 보이지만 다시 켤 때). 앱에 까닭을 보일
--  거절은 22023 으로. 0085 의 마지막 정의를 그대로 옮기고 그 errcode 하나만 바꿨다.
-- ════════════════════════════════════════════════════════════════════

create or replace function public.invite_redeem(p_code text)
returns text
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_code text := upper(regexp_replace(coalesce(p_code, ''), '\s', '', 'g'));
  v_inviter uuid;
  v_cfg economy.invite_config;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 0085 — 베타 동안 친구 초대 코드는 받지 않는다(입장은 베타 초대 코드)
  if not coalesce((select value = 'true'::jsonb from public.economy_settings where key = 'invite_enabled'), false) then
    raise exception '지금은 친구 초대 코드를 받지 않아요' using errcode = '22023';
  end if;
  if v_code !~ '^STEP-' then
    v_code := 'STEP-' || v_code;
  end if;
  select c.user_id into v_inviter from public.invite_codes c where c.code = v_code;
  if v_inviter is null then
    raise exception '초대 코드를 찾을 수 없어요' using errcode = '22023';  -- 0111: P0002 는 HTTP 500
  end if;
  if v_inviter = v_user then
    raise exception '내 초대 코드는 입력할 수 없어요' using errcode = '22023';
  end if;
  if exists (select 1 from public.invite_redemptions r where r.invitee = v_user) then
    raise exception '이미 초대 코드를 입력했어요' using errcode = '23505';
  end if;
  -- 서로 맞초대로 두 번 받지 못하게
  if exists (select 1 from public.invite_redemptions r where r.invitee = v_inviter and r.inviter = v_user) then
    raise exception '나를 초대한 사람의 코드는 입력할 수 없어요' using errcode = '22023';
  end if;
  select * into v_cfg from economy.invite_config where id;
  if not exists (select 1 from auth.users u where u.id = v_user
                  and u.created_at > now() - coalesce(v_cfg.redeem_window, interval '7 days')) then
    raise exception '초대 코드는 가입하고 7일 안에만 입력할 수 있어요' using errcode = '22023';
  end if;
  insert into public.invite_redemptions (invitee, inviter) values (v_user, v_inviter);
  return (select coalesce(p.display_name, '러너') from public.profiles p where p.id = v_inviter);
end $$;
revoke all on function public.invite_redeem(text) from public, anon;
grant execute on function public.invite_redeem(text) to authenticated;
