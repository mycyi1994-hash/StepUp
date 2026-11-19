-- 확인: 경로 없이 걸음만(폰 흔들기)으로 러닝 상자를 받는가 · 목표 1,000보로 7일 연속 상급 상자를 받는가
select t.mk(1), t.mk(2);
do $$
declare v_start timestamptz := now() - interval '3 hours'; d int;
begin
  perform t.say('경로 없이 4,200보 30분', t.rec(1, v_start, 1800, 4200, false));
  perform t.say('  받은 상자', (select string_agg(source, ', ') from economy.box_awards where user_id = t.u(1)));
  -- 2: 목표를 1,000보로 낮추고 7일 동안 날마다 1,100보(경로 있음)만
  perform t.login(2); perform public.profile_set_daily_goal(1000);
  for d in reverse 6..0 loop
    v_start := now() - make_interval(days => d) - interval '2 hours';
    perform t.rec(2, v_start, 600, 1100, true);
    perform public.goal_claim_day(economy.game_day(v_start));
  end loop;
  perform t.say('목표 1,000보 · 7일 — 받은 상자', (select string_agg(source, ', ') from economy.box_awards where user_id = t.u(2)));
  perform t.say('  목표 보너스 합', (select sum(amount) from public.sup_ledger where user_id = t.u(2) and kind = 'BONUS_GOAL'));
end $$;
