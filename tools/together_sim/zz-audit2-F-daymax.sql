-- 점검 2차 F(하루 최대 × 배수): 하루 최대 SUP 는 러닝마다 그 러닝의 배수로 센다(daily_max_of(사람, 효율, 배수)) —
-- 같은 러닝 · 같은 아이템이라도 2배권 러닝을 마지막에 올리면 이미 닿은 하루 최대 위로 더 받는다(먼저 올리면 못 받는다).
-- 목표 보너스의 남은 몫(day_room(…, null))은 그날 러닝의 day_max 가운데 가장 큰 값 — 1분짜리 2배권 러닝 하나가 그날 목표 몫을 두 배로 연다.
-- run.sh 는 숫자로 시작하는 장면만 돈다 — 이 장면은 손으로: begin; … rollback; 으로 감싸 psql -f.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n, 5000) from generate_series(1, 3) n;
insert into economy.runner_levels (user_id, legacy_energy) select t.u(n), 10 from generate_series(1, 3) n
on conflict (user_id) do update set legacy_energy = 10;
-- 36분 · 6,000보(에너지 10) 러닝을 k 번 — 사이마다 에너지 되돌리기권
create or replace function t.day(n int, p_k int, p_double_at int) returns text language plpgsql as $$
declare i int; t0 timestamptz := now() - interval '9 hours'; v jsonb; st timestamptz;
begin
  perform economy.item_add(t.u(n), 'ENERGY_RESET', p_k);
  perform economy.item_add(t.u(n), 'REWARD_DOUBLE', 1);
  for i in 1 .. p_k loop
    st := t0 + make_interval(mins => 40 * i);
    perform t.login(n);
    if i = p_double_at then v := public.run_items_use(st, array['REWARD_DOUBLE']); end if;
    perform t.rec(n, st, 2160, 6000);
    perform t.login(n);
    begin v := public.item_use('ENERGY_RESET', gen_random_uuid(), null); exception when others then null; end;
  end loop;
  return format('u%s 그날 합 %s SUP · 하루 최대(×1) %s · 2배권 러닝 %s SUP', n,
    round(economy.day_earned(t.u(n), economy.game_day(now())), 2),
    economy.daily_max_of(t.u(n), (select (economy.shoe_effective(e)).efficiency_bps from economy.equipped_sneaker(t.u(n)) e), 1),
    (select points_awarded from public.walk_sessions s where s.user_id = t.u(n) and s.reward_mult = 2));
end $$;
do $$
begin
  -- 같은 8번 · 2배권 하나 — 첫 러닝에 / 마지막 러닝에
  perform t.say('(가) 2배권을 첫 러닝에', t.day(1, 8, 1));
  perform t.say('(나) 2배권을 마지막 러닝에', t.day(2, 8, 8));
end $$;
do $$
declare st timestamptz := now() - interval '30 minutes'; v jsonb;
begin
  -- (다) 목표 보너스 몫: 1분 2배권 러닝 하나 뒤 day_room
  perform economy.item_add(t.u(3), 'REWARD_DOUBLE', 1);
  perform t.login(3);
  perform t.say('(다) 러닝 전 day_room', round(economy.day_room(t.u(3), economy.game_day(now()), null), 2));
  v := public.run_items_use(st, array['REWARD_DOUBLE']);
  perform t.rec(3, st, 60, 150);
  perform t.say('(다) 1분 2배권 러닝 뒤 day_room', round(economy.day_room(t.u(3), economy.game_day(now()), null), 2));
end $$;
