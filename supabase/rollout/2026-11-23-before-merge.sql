-- 0118 ~ 0123 을 나눠 켜기 — 1단계: 합치기(배포) 전에 운영 DB SQL 창에서 한 번 (2026-11-23)
--
-- 0118 ~ 0123 은 새 설정을 "on conflict (key) do nothing" 으로 넣는다. 그래서 그 키를 예전 셈 값으로 먼저 넣어 두면
-- 배포해도 셈은 지금(0117)과 같고, 2 ~ 4단계에서 스위치를 하나씩 켠다 — docs/rollout-0118-0123-2026-11-23.md.
-- 언제나 켜지는 것(스위치 없음): 0123 꼼수 · 보안 고침, 0118 의 목표 4,000보 바닥(1,000보 목표로 상급 상자 받던 구멍).
-- 몇 번을 다시 돌려도 같다(이미 있는 키는 건드리지 않는다). 배포한 뒤에 돌리면 늦다 — 그때는 2-3-4 단계 문서의 "되돌리기"를 쓴다.

insert into public.economy_settings (key, value) values
  -- 0118 보상 걸음: 상자는 적립 걸음 · 목표는 경로 걸음(0117 까지의 셈)
  ('reward_steps_rule', '"OLD"'),
  -- 0119 경제의 뿌리 — 여섯 값을 예전으로 두면 0118 과 똑같이 셈한다(0119 머리말)
  ('worn_rate', '0'),
  ('eff_wear_exp', '0'),
  ('eff_energy_exp', '0'),
  ('dur_repair_exp', '0'),
  ('energy_per_level', '2'),
  ('forge_xp_curve', '"CLASSIC"'),
  -- 0120 함께 달리기 보너스 — 출발한 사람 모두(ALL) · 예전 하루 금액 상한(CAP) · 크루 효율 섞기 끔
  ('crew_bonus_rule', '"ALL"'),
  ('party_bonus_rule', '"ALL"'),
  ('daily_max_rule', '"CAP"'),
  ('crew_blend_share', '0'),
  -- 0120 의 한 번만 블록(ALL → ACTIVE)이 위 ALL 을 바꾸지 않게, 그 블록이 이미 돈 것으로 적어 둔다
  ('together_fix_20261121', to_jsonb(now())),
  -- 0121 러너 레벨 — 끔(하루 에너지는 예전처럼 가진 신발 가운데 가장 높은 레벨)
  ('runner_level_rule', '"OFF"')
on conflict (key) do nothing;

-- 확인 — 열세 줄이 위 값이면 된다
select key, value from public.economy_settings
 where key in ('reward_steps_rule', 'worn_rate', 'eff_wear_exp', 'eff_energy_exp', 'dur_repair_exp', 'energy_per_level',
               'forge_xp_curve', 'crew_bonus_rule', 'party_bonus_rule', 'daily_max_rule', 'crew_blend_share',
               'together_fix_20261121', 'runner_level_rule')
 order by key;
