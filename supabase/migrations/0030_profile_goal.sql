-- ════════════════════════════════════════════════════════════════════
--  0030 — 앱의 하루 목표를 서버에
--
--  goal_claim(0024)은 profiles.daily_goal 로 판정하는데, 앱은 목표를 폰에만 두고 서버에
--  보내지 않았다. 그래서 목표를 3000 으로 둔 사람은 "+7.5 SUP" 를 보고도 8000 걸음 전에는
--  받지 못했고, 20000 인 사람은 +50 을 보고 20 을 받았다. 앱이 목표를 바꿀 때 이 함수로 보낸다.
--  범위는 goal_claim 이 쓰는 범위(1000~30000)로 맞춘다.
-- ════════════════════════════════════════════════════════════════════

create or replace function public.profile_set_daily_goal(p_goal int)
returns int
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := auth.uid();
  v_goal int := least(greatest(coalesce(p_goal, 8000), 1000), 30000);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  insert into public.profiles (id, daily_goal) values (v_user, v_goal)
  on conflict (id) do update set daily_goal = excluded.daily_goal;
  return v_goal;
end $$;

revoke all on function public.profile_set_daily_goal(int) from public, anon;
grant execute on function public.profile_set_daily_goal(int) to authenticated;
