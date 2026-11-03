-- ════════════════════════════════════════════════════════════════════
--  0085 — 친구 초대 코드(0036) 잠시 멈춤 (2026-11-03)
--
--  사용자 결정 "지금은 개인 초대 코드는 비활성화". 베타 동안 입장은 베타 초대 코드(0079 접속 코드)뿐인데
--  설정 › 친구 초대의 코드(STEP-XXXXXX, 추천 100 SUP)와 헷갈렸다(베타 테스터 피드백).
--  · 설정 invite_enabled(기본 false) — false 면 invite_redeem 이 새 입력을 받지 않는다. 앱은 설정의 친구 초대 줄을 숨긴다.
--  · 이미 입력해 둔 사람(invite_redemptions)의 첫 러닝 적립(economy.invite_reward_after_session)은 그대로다.
--  · 다시 켜려면: select admin_economy_set('invite_enabled', 'true');  (앱은 InviteFeature.ENABLED 도 함께)
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values ('invite_enabled', 'false'::jsonb)
on conflict (key) do nothing;

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
    raise exception '초대 코드를 찾을 수 없어요' using errcode = 'P0002';
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
