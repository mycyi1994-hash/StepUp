-- 감사 E(온라인 판): 남은 둘이 3시간 내내 박동을 보내도(0120 R3 · 장면 16 (가) 의 길) 같은 비켜 가기가 된다.
-- 감사 E: 묶인 체력(0122 #2)을 비켜 가기 — 탈주자 18명이 박동만 10분 보내고(달리지 않고) 끝내며 러닝을 올리지 않는다.
-- crew_pool_held(_except) 의 ① 은 active_sec < 600 만 묶고 ② 는 올린 러닝만 본다 → 10분 박동 뒤 올리지 않은 사람의 몫은 묶이지 않는다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n) from generate_series(511, 530) n;
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(513, 530) n);
select t.crew(52, 511, (select array_agg(n) from generate_series(512, 530) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; w11 public.walk_sessions; w12 public.walk_sessions; d economy.crew_day_pools; v_held numeric;
  arr int[] := (select array_agg(g) from generate_series(513, 530) g);
begin
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(511);
  p := public.party_create_v2(t.c(52), '오프라인', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 512 .. 530 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(511); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.login(511); v := public.crew_run_beat(p, 'RUNNING');
  -- 18명이 10분 10초 박동만(25초마다 — 정산은 30초까지 한 번에 센다)
  for n in 1 .. 25 loop
    perform t.age(interval '25 seconds');
    foreach n in array arr || array[511, 512] loop perform t.login(n); v := public.crew_run_beat(p, 'RUNNING'); end loop;
  end loop;
  for n in 513 .. 530 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  raise notice '18명 끝냄 뒤: % · u13 %', t.pool(52), t.cr(513);
  -- 남은 둘은 3시간 내내 박동(온라인)
  for n in 1 .. ((10800 - 625) / 25) loop
    perform t.age(interval '25 seconds');
    perform t.login(511); v := public.crew_run_beat(p, 'RUNNING');
    perform t.login(512); v := public.crew_run_beat(p, 'RUNNING');
  end loop;
  perform t.login(511); v := public.crew_run_beat(p, 'ENDED');
  perform t.login(512); v := public.crew_run_beat(p, 'ENDED');
  select started_at into v_start from economy.crew_runs where user_id = t.u(511);
  raise notice '%', t.rec(511, v_start, 10800, 29700);
  raise notice '%', t.rec(512, v_start, 10800, 29700);
  select * into w11 from public.walk_sessions x where x.user_id = t.u(511) and x.party_id = p;
  select * into w12 from public.walk_sessions x where x.user_id = t.u(512) and x.party_id = p;
  select * into d from economy.crew_day_pools x where x.crew_id = t.c(52);
  v_held := economy.crew_pool_held(t.c(52), d.day, now());
  raise notice 'RESULT u11 % · u12 % SUP (장면 16 (가) 온라인 0120 고침 뒤 433 · 고치기 전 2,639.7) · 쓴 체력 % / % · 묶인 % · day_max %',
    w11.points_awarded, w12.points_awarded, round(d.consumed, 1), round(d.capacity, 1), round(v_held, 1), w11.day_max;
  raise notice '%', t.cr(511); raise notice '%', t.cr(512);
  delete from economy.reward_events;
end $$;
