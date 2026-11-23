-- 점검 2차 F(아이템 · 배수 · 에너지): 러닝 아이템을 두 번 · 다른 러닝에 · 겹쳐 쓰기, 바로 쓰는 아이템, 에너지 되돌리기와 러너 레벨.
-- run.sh 는 숫자로 시작하는 장면만 돈다 — 이 장면은 손으로: begin; … rollback; 으로 감싸 psql -f.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(1, 1500); select t.mk(2, 1500); select t.mk(3, 1500); select t.mk(4, 1500);
-- 0121 전 계정처럼(하루 에너지 10)
insert into economy.runner_levels (user_id, legacy_energy) select t.u(n), 10 from generate_series(1, 4) n
on conflict (user_id) do update set legacy_energy = 10;
do $$
declare v jsonb; t0 timestamptz := now() - interval '3 hours'; r text; e text;
begin
  perform economy.item_add(t.u(1), 'REWARD_DOUBLE', 2);
  perform economy.item_add(t.u(1), 'RUN_BOOSTER', 1);
  perform economy.item_add(t.u(1), 'ENERGY_DRINK', 1);
  perform economy.item_add(t.u(1), 'ENERGY_RESET', 2);
  perform t.login(1);
  -- (1) 같은 러닝에 두 번 → 그대로(빼지 않음)
  v := public.run_items_use(t0, array['REWARD_DOUBLE']);
  v := public.run_items_use(t0, array['REWARD_DOUBLE']);
  perform t.say('(1) 같은 러닝에 2배권 두 번 — 남은 2배권', (select qty from public.user_items where user_id = t.u(1) and kind = 'REWARD_DOUBLE'));
  -- (2) 같은 묶음 둘
  begin v := public.run_items_use(t0, array['RUN_BOOSTER']); perform t.say('(2) 같은 묶음 둘째', v);
  exception when others then perform t.say('(2) 같은 묶음 둘째 거절', sqlerrm); end;
  -- (3) 다른 묶음(에너지 드링크) + 2배권 한 러닝 — 허용(설계)
  v := public.run_items_use(t0, array['ENERGY_DRINK']);
  perform t.say('(3) 2배권 + 에너지 드링크', v);
  -- (4) 24시간 부스터 · 보상 이벤트 × 1.5 · 2배권 → 가장 큰 것 하나
  insert into public.boosts (user_id, kind, starts_at, ends_at, cost) values (t.u(1), 'XP_BOOSTER', t0 - interval '1 hour', t0 + interval '23 hours', 0);
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor) values (gen_random_uuid(), 'x1.5', 1.5, t0 - interval '1 hour', t0 + interval '1 hour', 'sim');
  perform t.say('(4) 부스터 2 · 이벤트 1.5 · 2배권 2 → 배수', economy.run_reward_mult(t.u(1), t0));
  r := t.rec(1, t0, 2160, 6000);
  perform t.say('(4) 러닝', r || ' mult=' || (select reward_mult from public.walk_sessions where user_id = t.u(1) and started_at = t0)
                || ' day_max=' || (select day_max from public.walk_sessions where user_id = t.u(1) and started_at = t0));
  -- (5) 기록한 러닝에 다시 → 거절
  begin v := public.run_items_use(t0, array['RUN_BOOSTER']); perform t.say('(5) 기록 뒤', v);
  exception when others then perform t.say('(5) 기록 뒤 거절', sqlerrm); end;
  -- (6) 에너지 — 드링크로 6,000보에 에너지 5 · 되돌리기 → 남은 = 최대
  perform t.say('(6) 에너지 최대 · 남은 · 쓴', format('%s · %s · %s', economy.energy_max(t.u(1)), economy.energy_left(t.u(1), economy.game_day(now())),
     (select used from public.energy_days where user_id = t.u(1) and day = economy.game_day(now()))));
  v := public.item_use('ENERGY_RESET', gen_random_uuid(), null);
  perform t.say('(6) 되돌린 뒤 남은', economy.energy_left(t.u(1), economy.game_day(now())));
  begin v := public.item_use('ENERGY_RESET', gen_random_uuid(), null); perform t.say('(6) 둘째 되돌리기', v);
  exception when others then perform t.say('(6) 둘째 되돌리기 거절', sqlerrm); end;
  -- (7) 이미 끝난(올리지 않은) 러닝에 나중에 2배권 — 결과를 보고 고를 수 있다
  -- 둘째 러닝(t0 + 1시간, 36분)은 이미 끝났지만 아직 안 올렸다 — 지금 2배권을 묶고 올린다
  v := public.run_items_use(t0 + interval '1 hour', array['REWARD_DOUBLE']);
  perform t.say('(7) 1시간 24분 전에 끝난 러닝에 지금 2배권', v);
  e := t.rec(1, t0 + interval '1 hour', 2160, 6000);
  perform t.say('(7) 그 러닝', e || ' mult=' || (select reward_mult from public.walk_sessions where user_id = t.u(1) and started_at = t0 + interval '1 hour'));
end $$;

-- (8) 크루 러닝에도 2배권이 실린다(앱은 크루 · 번개 러닝에 아이템을 싣지 않는다 — fromCheck=false). 2배권 하나로 크루 기본 · 인원 · 레벨 · 하루 최대가 모두 × 2
select t.crew(1, 2, array[3], 10);
do $$
declare p bigint; v jsonb; v_start timestamptz; a public.walk_sessions; b public.walk_sessions;
begin
  perform economy.item_add(t.u(2), 'REWARD_DOUBLE', 1);
  perform t.login(2);
  p := public.party_create_v2(t.c(1), '2배권', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(3); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(2); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[2, 3], 3600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(2);
  -- 시계를 옮기는 장면이라 묶기는 달린 뒤(옮긴 시작 시각으로) — 앱(수정한 앱)이 출발 때 묶은 것과 같다
  perform t.login(2); v := public.run_items_use(v_start, array['REWARD_DOUBLE']);
  perform t.say('(8) 크루 러닝 시작 시각에 2배권', v);
  perform t.login(2); v := public.crew_run_beat(p, 'ENDED');
  perform t.login(3); v := public.crew_run_beat(p, 'ENDED');
  perform t.rec(2, v_start, 3600, 9900);
  perform t.rec(3, v_start, 3600, 9900);
  select * into a from public.walk_sessions where user_id = t.u(2) and party_id = p;
  select * into b from public.walk_sessions where user_id = t.u(3) and party_id = p;
  perform t.say('(8) 크루 · 2배권 u2 / 없는 u3', format('u2 %s SUP (mult %s · day_max %s · unit %s) · u3 %s SUP (mult %s · unit %s)',
     a.points_awarded, a.reward_mult, a.day_max, a.together_unit, b.points_awarded, b.reward_mult, b.together_unit));
  perform t.say('(8) u2 크루 몫', t.cr(2));
end $$;

-- (9) 기록이 실패한(예외) 러닝의 아이템 — 돌려받지 못한다
do $$
declare v jsonb; t1 timestamptz := now() - interval '1 minute';
begin
  perform economy.item_add(t.u(4), 'REWARD_DOUBLE', 1);
  perform t.login(4);
  v := public.run_items_use(t1, array['REWARD_DOUBLE']);
  begin
    perform public.record_session(t1, now() + interval '10 minutes', 1000, 600, '', 0, 1, '', false, null);
  exception when others then perform t.say('(9) 기록 거절', sqlerrm); end;
  perform t.say('(9) 남은 2배권 · 묶인 줄', format('%s · %s', (select qty from public.user_items where user_id = t.u(4) and kind = 'REWARD_DOUBLE'),
     (select count(*) from economy.item_uses where user_id = t.u(4) and session_id is null)));
end $$;

-- (10) 연속 방패(STREAK_SHIELD — 판매는 0116 부터 멈춤): 24시간 하나가 이틀(달력)에 걸치면 이틀을 연속으로 센다
do $$
begin
  perform t.mk(9, 1500);
  insert into public.boosts (user_id, kind, starts_at, ends_at, cost)
  values (t.u(9), 'STREAK_SHIELD', economy.game_day_start(economy.game_day(now()) - 2) + interval '12 hours',
          economy.game_day_start(economy.game_day(now()) - 1) + interval '12 hours', 120);
  perform t.say('(10) 방패 하나 · 러닝 없음 → 오늘 앞 연속', economy.goal_streak_before(t.u(9), economy.game_day(now()), 8));
  perform t.say('(10) 부스터 판매', economy.setting('boost_shop_enabled'));
end $$;
