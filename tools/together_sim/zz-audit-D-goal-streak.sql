-- 감사 D: 하루 최대 SUP(daily_max_rule 'HOUR', 0120)에 닿은 날 목표를 채우면 goal_settle 이 0 을 돌려주고 원장 'goal:<날>' 을 적지 않는다.
-- 연속(goal_streak_before)은 그 원장 줄로 세므로 다음 날 연속이 1 로 끊기고, 7일 연속 상급 상자도 놓친다. 'CAP' 이면 끊기지 않는다.
-- (짧게 보이려고 daily_max_hour_mult 0.3 — 16-together-fix (마)와 같다. 기본값 4 에서는 긴 크루 러닝 · 가입 7일 안(절반)에서 닿는다)
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '[]'::jsonb where key = 'run_box_steps';
select t.mk(n) from generate_series(1, 2) n;
create or replace function t.goal_case(n int, p_rule text) returns text language plpgsql as $$
declare v_y timestamptz; v_t timestamptz; r record; y date := economy.game_day(now()) - 1; s1 numeric; s2 numeric;
begin
  update public.economy_settings set value = to_jsonb(p_rule) where key = 'daily_max_rule';
  update public.economy_settings set value = '0.3' where key = 'daily_max_hour_mult';
  perform t.login(n);
  -- 어제: 6,000보(목표 6,000) — 하루 최대에 걸린다
  v_y := economy.game_day_start(y) + interval '10 hours';
  select * into r from public.record_session(v_y, v_y + interval '3600 seconds', 6000, 3600, t.track(v_y, 3600, 0.000015), 0, 1, '', false);
  s1 := r.points_awarded;
  update public.economy_settings set value = '4' where key = 'daily_max_hour_mult';
  -- 오늘: 다시 6,000보
  v_t := greatest(economy.game_day_start(economy.game_day(now())), now() - interval '3 hours');
  select * into r from public.record_session(v_t, v_t + interval '3600 seconds', 6000, 3600, t.track(v_t, 3600, 0.000016), 0, 1, '', false);
  return format('rule=%s · 어제 보상 걸음 %s / 목표 %s · 어제 적립 %s · 어제 goal 원장 %s · 오늘 goal 보너스 %s (%s)',
    p_rule, economy.verified_steps_on(t.u(n), y), economy.goal_peek(t.u(n), y), s1,
    coalesce((select amount::text from public.sup_ledger where user_id = t.u(n) and ref = 'goal:' || y), '없음'),
    coalesce((select amount::text from public.sup_ledger where user_id = t.u(n) and ref = 'goal:' || economy.game_day(now())), '없음'),
    coalesce((select description from public.sup_ledger where user_id = t.u(n) and ref = 'goal:' || economy.game_day(now())), '-'));
end $$;
select t.say('HOUR', t.goal_case(1, 'HOUR'));
select t.say('CAP ', t.goal_case(2, 'CAP'));
