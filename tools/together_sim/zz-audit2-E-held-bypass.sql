-- 감사 E: 묶인 체력(0122 #2)을 비켜 가기 — 탈주자 18명이 박동만 10분 보내고(달리지 않고) 끝내며 러닝을 올리지 않는다.
-- crew_pool_held(_except) 의 ① 은 active_sec < 600 만 묶고 ② 는 올린 러닝만 본다 → 10분 박동 뒤 올리지 않은 사람의 몫은 묶이지 않는다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n) from generate_series(11, 30) n;
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(13, 30) n);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; w11 public.walk_sessions; w12 public.walk_sessions; d economy.crew_day_pools; v_held numeric;
  arr int[] := (select array_agg(g) from generate_series(13, 30) g);
begin
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '오프라인', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.login(11); v := public.crew_run_beat(p, 'RUNNING');
  -- 18명이 10분 10초 박동만(25초마다 — 정산은 30초까지 한 번에 센다)
  for n in 1 .. 25 loop
    perform t.age(interval '25 seconds');
    foreach n in array arr loop perform t.login(n); v := public.crew_run_beat(p, 'RUNNING'); end loop;
  end loop;
  for n in 13 .. 30 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  raise notice '18명 끝냄 뒤: % · u13 %', t.pool(2), t.cr(13);
  perform t.age(interval '10800 seconds' - interval '625 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  raise notice '%', t.rec(11, v_start, 10800, 29700);
  raise notice '%', t.rec(12, v_start, 10800, 29700);
  select * into w11 from public.walk_sessions x where x.user_id = t.u(11) and x.party_id = p;
  select * into w12 from public.walk_sessions x where x.user_id = t.u(12) and x.party_id = p;
  select * into d from economy.crew_day_pools x where x.crew_id = t.c(2);
  v_held := economy.crew_pool_held(t.c(2), d.day, now());
  raise notice 'RESULT u11 % · u12 % SUP (0122 장면18(가) 434 · 0120 1,217) · 쓴 체력 % / % · 묶인 % · day_max %',
    w11.points_awarded, w12.points_awarded, round(d.consumed, 1), round(d.capacity, 1), round(v_held, 1), w11.day_max;
  raise notice '%', t.cr(11); raise notice '%', t.cr(12);
  delete from economy.reward_events;
end $$;
