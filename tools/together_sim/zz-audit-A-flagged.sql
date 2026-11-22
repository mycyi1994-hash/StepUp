-- 감사 A — 러너 걸음(0121 runner_steps_of)은 판정 CLEAN 만 센다. FLAGGED 는 벌(과속)만이 아니라
-- 상한 · 다듬기에서도 붙는다: "걸음 수가 GPS 거리보다 많습니다"(확인된 걸음을 경로 거리로 줄였다) · "하루 최대 SUP 에 걸렸습니다" ·
-- "하루에 올릴 수 있는 적립 상한" · 한 러닝 48,000보 넘음. 그 러닝의 보상 걸음은 상자 · 목표(CLEAN + FLAGGED)에는 들고 러너 레벨에는 0.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
select t.mk(81, 3000);
do $$
declare
  v_start timestamptz := now() - interval '3 hours';
  r record;
  s public.walk_sessions;
begin
  perform t.login(81);
  -- 2시간 · 24,000보(분당 200), 경로는 약 4.8km(걸음 거리 18.3km 의 절반 아래 — GPS 가 모퉁이를 깎거나 끊긴 러닝) —
  -- 확인된 걸음은 경로 × 2 ÷ 0.762 로 줄인다(약 12,600보 — 러너 1레벨 넘는 양)
  select * into r from public.record_session(v_start, v_start + interval '7200 seconds', 24000, 7200,
                                             t.track(v_start, 7200, 0.000006), 0, 1, '', false);
  select * into s from public.walk_sessions where id = r.session_id;
  perform t.say('run', format('%s [%s] steps=%s verified=%s reward_steps=%s gps_m=%s', s.verdict, s.verdict_reason, s.steps, s.verified_steps,
                              s.reward_steps, round(s.gps_distance_m::numeric)));
  perform t.say('box/goal steps today (CLEAN+FLAGGED)', economy.box_steps(t.u(81), economy.game_day(now()), economy.game_day(now())));
  perform t.say('runner steps (CLEAN only)', economy.runner_steps_of(t.u(81)));
  perform t.say('runner level', (select r2.level from economy.runner_levels r2 where r2.user_id = t.u(81)));
  perform t.say('RUNNER boxes', (select count(*) from economy.box_awards a where a.user_id = t.u(81) and a.source like 'RUNNER%'));
  perform t.say('RUN boxes', (select count(*) from economy.box_awards a where a.user_id = t.u(81) and a.source like 'RUN2%' or a.user_id = t.u(81) and a.source like 'RUN4%'));
end $$;
