-- 감사 D: 러너 걸음(runner_steps_of, 0121)은 판정 CLEAN 만 센다. 그런데 record_session(0120)은 상한에 닿은 것만으로도 FLAGGED 를 준다
-- (하루 최대 SUP · 하루 금액 상한 · 올린 날 상한 · 하루 걸음 상한). 그래서 하루 최대에 닿은 러닝의 걸음은 러너 레벨에 들지 않고,
-- 같은 러닝이 daily_max_rule 'CAP' 이면 든다. 오늘의 보상(verified_steps_on)은 FLAGGED 도 센다 — 둘이 어긋난다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '0.3' where key = 'daily_max_hour_mult';
select t.mk(n) from generate_series(1, 2) n;
create or replace function t.rf_case(n int, p_rule text) returns text language plpgsql as $$
declare v_t timestamptz; r record;
begin
  update public.economy_settings set value = to_jsonb(p_rule) where key = 'daily_max_rule';
  perform t.login(n);
  v_t := greatest(economy.game_day_start(economy.game_day(now())), now() - interval '3 hours');
  select * into r from public.record_session(v_t, v_t + interval '3600 seconds', 6000, 3600, t.track(v_t, 3600, 0.000015), 0, 1, '', false);
  return format('rule=%s · %s %s SUP [%s] · reward_steps=%s · 러너 걸음=%s · 오늘 보상 걸음=%s',
    p_rule, r.verdict, r.points_awarded, r.verdict_reason,
    (select reward_steps from public.walk_sessions where id = r.session_id),
    (select steps from economy.runner_levels where user_id = t.u(n)),
    economy.verified_steps_on(t.u(n), economy.game_day(now())));
end $$;
select t.say('HOUR', t.rf_case(1, 'HOUR'));
select t.say('CAP ', t.rf_case(2, 'CAP'));
