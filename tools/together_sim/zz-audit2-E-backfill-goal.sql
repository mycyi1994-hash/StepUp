-- 감사 E: 0122 맨 끝 한 번만 블록의 "하루 최대에 막혔던 목표 날" 채우기 — goal_days 줄이 아직 없는 날이면
-- 같은 INSERT 안에서 goal_of() 가 그 줄을 먼저 넣고, 바깥 INSERT ... ON CONFLICT DO UPDATE 가 그 줄을 다시 건드려
-- "ON CONFLICT DO UPDATE command cannot affect row a second time" 로 이주 전체가 멈추는지.
-- 0118 전(goal_days 가 생기기 전)에 달린 날 · goal_settle 을 거치지 않은 날(예: 보상 걸음 0 · 예전 앱 기록)이 그런 날이다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(71, 3000);
do $$
declare v_day date := economy.game_day(now()) - 3; r text;
begin
  r := t.rec(71, economy.game_day_start(v_day) + interval '10 hours', 3600, 9900);
  raise notice 'run %', r;
  -- 그날 하루 최대를 다 받은 것으로(장면을 짧게) · 그날 goal_days 줄이 없던 것으로(0118 전 러닝)
  update public.walk_sessions set day_max = 1 where user_id = t.u(71);
  delete from public.sup_ledger where user_id = t.u(71) and ref = 'goal:' || v_day;
  delete from economy.goal_days where user_id = t.u(71);
  raise notice 'verified %, goal_peek %, room %', economy.verified_steps_on(t.u(71), v_day), economy.goal_peek(t.u(71), v_day),
    economy.day_room(t.u(71), v_day, null);
end $$;
-- 0122 의 한 번만 블록 그대로(사용자 71 만으로 좁힘)
do $$
begin
    insert into economy.goal_days (user_id, day, goal, met_at)
    select u.id, d.day::date, economy.goal_of(u.id, d.day::date), now()
      from auth.users u
     cross join generate_series(economy.game_day(now()) - 7, economy.game_day(now()) - 1, interval '1 day') d(day)
     where u.id = t.u(71) and economy.daily_max_on()
       and not exists (select 1 from public.sup_ledger l where l.user_id = u.id and l.ref = 'goal:' || d.day::date)
       and exists (select 1 from public.walk_sessions s where s.user_id = u.id and economy.game_day(s.started_at) = d.day::date)
       and economy.verified_steps_on(u.id, d.day::date) >= economy.goal_of(u.id, d.day::date)
       and economy.day_room(u.id, d.day::date, null) <= 0
    on conflict (user_id, day) do update set met_at = coalesce(economy.goal_days.met_at, excluded.met_at);
    raise notice 'RESULT 채우기 성공 — met_at %', (select g.met_at is not null from economy.goal_days g where g.user_id = t.u(71) and g.day = economy.game_day(now()) - 3);
exception when others then
    raise notice 'RESULT 채우기 실패(이주가 멈춘다): % (%)', sqlerrm, sqlstate;
end $$;
