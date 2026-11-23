-- 감사 A — 크루 러닝: 둘째로 올린 사람은 together_settle(인원 보너스)이 먼저 하루 최대를 채워 목표 보너스 줄이 안 생긴다.
-- record_session(0120)은 together_settle → goal_settle 차례. 첫째는 올릴 때 목표 보너스를 받고 나중 더하기가 그만큼 줄어든다 —
-- SUP 은 같아도 둘째만 그날 목표 줄('goal:<날>')이 없어 연속이 끊기고 7일 연속 상급 상자를 잃는다.
-- 두 사람 모두 지난 엿새 목표를 채워 두었다(줄만 — 설정 편의). 크루 2명 · 3시간 50분 · 38,000보(분당 165).
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
delete from economy.reward_events;
select t.mk(91, 3000); select t.mk(92, 3000);
select t.crew(9, 91, array[92], 1);
do $$
declare
  p bigint; v jsonb; v_start timestamptz; n int; d int;
  v_today date := economy.game_day(now());
begin
  for n in 91 .. 92 loop
    for d in 1 .. 6 loop
      perform economy.ledger_apply(t.u(n), 'BONUS_GOAL', 15, '설정: 지난 목표', 'goal:' || (v_today - d));
    end loop;
  end loop;
  perform t.login(91);
  p := public.party_create_v2(t.c(9), '긴 러닝', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(92); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(91); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[91, 92], 13800);
  select started_at into v_start from economy.crew_runs where user_id = t.u(91);
  for n in 91 .. 92 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.say('u91 (first upload)', t.rec(91, v_start, 13800, 38000));
  perform t.say('u92 (second upload)', t.rec(92, v_start, 13800, 38000));
  for n in 91 .. 92 loop
    perform t.say(format('u%s', n), format('points=%s day_max=%s day_earned=%s goal_row=%s STREAK_box=%s streak_for_tomorrow=%s',
      (select s.points_awarded from public.walk_sessions s where s.user_id = t.u(n) and s.party_id = p),
      (select s.day_max from public.walk_sessions s where s.user_id = t.u(n) and s.party_id = p),
      economy.day_earned(t.u(n), v_today),
      coalesce((select l.amount::text from public.sup_ledger l where l.user_id = t.u(n) and l.ref = 'goal:' || v_today), 'NONE'),
      exists (select 1 from economy.box_awards a where a.user_id = t.u(n) and a.source like 'STREAK:%'),
      economy.goal_streak_before(t.u(n), v_today + 1, 60)));
  end loop;
end $$;
