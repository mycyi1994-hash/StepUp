-- 감사 E: 목표 날(0122 #4)의 met_at 이 나중에 생긴 하루 최대 여유를 막는다.
-- 첫 러닝이 하루 최대에 닿아 목표 보너스 0 → met_at 만 남김. 같은 날 보상 이벤트 × 2(또는 더 센 신발)로 하루 최대가 커져 여유가 생겨도
-- goal_settle 은 met_at 을 보고 그대로 끝난다 — 0120 은 다음 러닝 때 목표 보너스를 줬다(줄이 없었으므로).
-- 하루 최대에 닿는 길은 장면 18 (사)의 긴 크루 러닝이 현실이다 — 여기서는 빨리 보려고 daily_max_hour_mult 를 0.5 로 낮춘다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
insert into public.economy_settings (key, value) values ('daily_max_hour_mult', '0.5')
on conflict (key) do update set value = excluded.value;
select t.mk(61, 3000);
do $$
declare v_day date := economy.game_day(now()); r text; v_room numeric; v_goal numeric;
begin
  perform t.login(61); perform public.profile_set_daily_goal(4000);
  r := t.rec(61, now() - interval '3 hours', 1800, 4950);
  raise notice '1) %', r;
  raise notice '   goal_days met_at=% · goal 줄=% · 여유=%',
    (select g.met_at is not null from economy.goal_days g where g.user_id = t.u(61) and g.day = v_day),
    (select l.amount from public.sup_ledger l where l.user_id = t.u(61) and l.ref = 'goal:' || v_day),
    round(economy.day_room(t.u(61), v_day, null), 2);
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '1 hour', now() + interval '6 hours', 'sim');
  r := t.rec(61, now() - interval '50 minutes', 600, 1650);
  raise notice '2) %', r;
  v_room := economy.day_room(t.u(61), v_day, null);
  select l.amount into v_goal from public.sup_ledger l where l.user_id = t.u(61) and l.ref = 'goal:' || v_day;
  raise notice 'RESULT 둘째 러닝 뒤 하루 여유 % SUP 가 남았는데 목표 보너스 줄=% (0120: 10 SUP 를 이때 줬다)', round(v_room, 2), v_goal;
  begin
    raise notice '   예전 앱 goal_claim_day => %', public.goal_claim_day(v_day);
  exception when others then
    raise notice '   예전 앱 goal_claim_day 거절: % (%)', sqlerrm, sqlstate;
  end;
  delete from economy.reward_events;
exception when others then
  raise notice 'RESULT(오류) %', sqlerrm;
end $$;
