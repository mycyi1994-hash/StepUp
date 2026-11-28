-- 0128 — 하루 걸음 상자 (2026-11-26)
--
-- 테스터 "토스 만보기처럼 앱을 안 켜도 백그라운드 걸음으로 약간의 보상 · 매일 들어오게". 사용자 "묻지 말고 전부".
-- SUP 를 바로 주지 않는다(하루 에너지 상한 · 인플레이션 — 0122 A). 대신 하루에 한 번, 그날(한국 날짜) 폰이 올린 하루 걸음
-- (public.daily_steps — steps_sync 가 받은 값, 하루 상한 · 줄지 않음)이 step_box_steps 를 넘으면 일반 상자 하나(box_grant 'STEPS:<날>' FREE).
--
--   - 하루 걸음은 폰이 센 값이라 서버가 확인하지 못한다 — 그래서 상자 하나만, 하루 한 번(출석 상자와 같은 무게).
--     러닝 상자 · 목표 · 연속은 그대로 보상 걸음(경로로 확인된 걸음, 0118)으로만 센다 — 이 상자는 그 셈에 들지 않는다.
--   - 가입하고 step_box_min_account_hours(24) 뒤부터(그날 만든 부계정으로 상자를 받지 않게 — 0123 함께 달린 사람과 같은 바닥).
--   - 끄기: step_box_rule = '"OFF"'. 걸음 기준은 step_box_steps(기본 6,000 — 새 계정의 하루 목표).
--   - 앱은 하루 걸음을 올린 뒤(DailyStepsUploader) 부른다 — 앱을 켜지 않아도 걸음 일꾼(DailyStepsWorker)이 한 시간마다 올리고 부른다.

insert into public.economy_settings (key, value) values
  ('step_box_rule', '"ON"'),
  ('step_box_steps', '6000'),
  ('step_box_min_account_hours', '24')
on conflict (key) do nothing;

create or replace function public.step_box_claim()
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_epoch bigint := economy.game_day(now()) - date '1970-01-01';
  v_need int := greatest(coalesce(economy.setting_num('step_box_steps'), 6000), 1000)::int;
  v_hours int := greatest(coalesce(economy.setting_num('step_box_min_account_hours'), 24), 0)::int;
  v_steps int;
  v_created timestamptz;
  v_new boolean := false;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(economy.setting('step_box_rule') #>> '{}', 'ON') <> 'ON' then
    return jsonb_build_object('on', false);
  end if;
  select d.steps into v_steps from public.daily_steps d where d.user_id = v_user and d.epoch_day = v_epoch;
  v_steps := coalesce(v_steps, 0);
  select p.created_at into v_created from public.profiles p where p.id = v_user;
  if v_steps >= v_need and coalesce(v_created, now()) <= now() - make_interval(hours => v_hours) then
    v_new := economy.box_grant(v_user, 'STEPS:' || v_day, 'FREE', v_day);
  end if;
  return jsonb_build_object(
    'on', true,
    'day', v_day,
    'need', v_need,
    'steps', v_steps,
    'got', exists (select 1 from economy.box_awards a where a.user_id = v_user and a.source = 'STEPS:' || v_day),
    'new', v_new,
    'account_new', coalesce(v_created, now()) > now() - make_interval(hours => v_hours));
end $$;

revoke all on function public.step_box_claim() from public, anon;
grant execute on function public.step_box_claim() to authenticated;
