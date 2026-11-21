-- 검사(0120 — 함께 달리기 보너스 고치기): 함께 달린 사람만 · 올릴 때마다 더하기 · 탈주한 사람의 체력 · 하루 최대 SUP.
-- 줄마다 OK / FAIL — CI(test-sql.yml)가 FAIL 이 있으면 멈춘다. docs/together-bonus-fix-spec-2026-11-21.md
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;
select t.mk(n) from generate_series(1, 2) n;
select t.mk(n) from generate_series(11, 30) n;
select t.mk(n) from generate_series(41, 46) n;
select t.mk(n) from generate_series(51, 55) n;
select t.mk(n) from generate_series(71, 73) n;
select t.crew(7, 71, array[72, 73], 1);
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(13, 30) n);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
select t.crew(4, 41, array[42, 43], 1);
select t.crew(5, 44, array[45, 46], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; r text; w public.walk_sessions; c economy.crew_runs; v_max numeric;
begin
  -- (가) 20명 출발 · 18명은 6초 만에 끝냄 · 둘이 3시간 · 크루 Lv20 · 보상 이벤트 × 2 — 점검 P1 · P2 · P3 의 재현
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '탈주', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  for n in 13 .. 30 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.login(11); v := public.crew_run_beat(p, 'RUNNING');
  perform t.chk('(가) 바로 끝낸 18명의 체력은 묶인다', (v -> 'pool' ->> 'held')::numeric > 5000 and (v -> 'pool' ->> 'total')::numeric < 700,
                v -> 'pool' ->> 'total' || ' / 묶음 ' || (v -> 'pool' ->> 'held'));
  perform t.run(p, array[11, 12], 10800);
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  perform t.login(11); v := public.crew_run_beat(p, 'ENDED');
  r := t.rec(11, v_start, 10800, 29700);
  perform t.login(12); v := public.crew_run_beat(p, 'ENDED');
  perform t.rec(12, v_start, 10800, 29700);
  select * into c from economy.crew_runs where user_id = t.u(11);
  select * into w from public.walk_sessions where id = c.session_id;
  v_max := economy.daily_max_of(t.u(11), 4000, 2);
  perform t.chk('(가) 인원은 함께 달린 둘만', c.bonus_count = 2 and c.bonus_pct = 0.05, format('%s명 +%s', c.bonus_count, c.bonus_pct));
  perform t.chk('(가) 한 번에 받은 SUP — 예전 2,639.7', w.points_awarded between 300 and 600 and w.points_awarded <= v_max,
                format('%s SUP (하루 최대 %s)', w.points_awarded, v_max));

  -- (나) 셋이 15분씩 — 한 사람씩 올릴 때마다 먼저 올린 사람에게 더해진다
  perform t.login(41);
  p := public.party_create_v2(t.c(4), '더하기', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 42 .. 43 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(41); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[41, 42, 43], 900);
  select started_at into v_start from economy.crew_runs where user_id = t.u(41);
  for n in 41 .. 43 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.rec(41, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(41);
  perform t.chk('(나) 혼자 먼저 올리면 인원 1', c.bonus_count = 1 and coalesce(c.headcount_sup, 0) = 0, format('%s명 · %s', c.bonus_count, c.headcount_sup));
  perform t.rec(42, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(41);
  perform t.chk('(나) 둘째가 올리면 첫째에게 +5% 가 더해진다', c.bonus_count = 2 and abs(c.headcount_sup - round(c.base_sup * 0.05, 4)) < 0.001,
                format('%s명 · 기본 %s · 인원 %s', c.bonus_count, c.base_sup, c.headcount_sup));
  perform t.rec(43, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(41);
  perform t.chk('(나) 셋째가 올리면 모두 +10%', c.bonus_count = 3 and abs(c.headcount_sup - round(c.base_sup * 0.10, 4)) < 0.001
                and (select count(*) from public.sup_ledger l where l.user_id = t.u(41) and l.ref like 'together:%') = 2,
                format('%s명 · 인원 %s · 원장 %s줄', c.bonus_count, c.headcount_sup, (select count(*) from public.sup_ledger l where l.user_id = t.u(41) and l.ref like 'together:%')));
  perform t.chk('(나) 원장 = 러닝 적립', (select sum(amount) from public.sup_ledger l where l.user_id = t.u(41) and (l.session_id = c.session_id or l.ref like 'together:%'))
                = (select points_awarded from public.walk_sessions where id = c.session_id), 'ok');

  -- (다) 셋이 출발 · 한 사람은 5분 만에 끝냄 — 둘만 센다
  perform t.login(44);
  p := public.party_create_v2(t.c(5), '5분', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 45 .. 46 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(44); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[44, 45, 46], 300);
  select started_at into v_start from economy.crew_runs where user_id = t.u(44);
  perform t.login(46); v := public.crew_run_beat(p, 'ENDED');
  perform t.rec(46, v_start, 300, 825);
  perform t.run(p, array[44, 45], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(44);
  for n in 44 .. 45 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); perform t.rec(n, v_start, 900, 2475); end loop;
  select * into c from economy.crew_runs where user_id = t.u(44);
  perform t.chk('(다) 5분 만에 끝낸 사람은 인원에 들지 않는다', c.bonus_count = 2, format('%s명', c.bonus_count));
  select * into c from economy.crew_runs where user_id = t.u(46);
  perform t.chk('(다) 5분 만에 끝낸 사람 자신은 인원 보너스 없음', c.bonus_reason = 'NOT_TOGETHER' and coalesce(c.headcount_sup, 0) = 0, coalesce(c.bonus_reason, '-'));
end $$;
do $$
declare post bigint; p bigint; v_start timestamptz; i int; w public.walk_sessions; v_max numeric; r record; v_first numeric;
begin
  -- (라) 번개: 넷이 출발 · 셋은 달리지 않음 → 배수 없음. 한 사람이 15분 달려 올리면 먼저 올린 사람에게 × 1.1 이 더해진다
  perform t.login(51);
  post := public.post_create('FLASH', null, '번개', '', '', 5, now() + interval '30 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  for i in 52..54 loop perform t.login(i); perform public.join_flash(post); end loop;
  perform t.login(51); p := public.party_open(null, post); perform public.party_ready(p, true);
  for i in 52..54 loop perform t.login(i); perform public.party_open(null, post); perform public.party_ready(p, true); end loop;
  perform t.login(51); perform public.party_start(p);
  perform t.age(interval '4 seconds');
  perform t.age(interval '20 minutes');
  select starts_at into v_start from public.party_runs where party_id = p and user_id = t.u(51);
  perform t.rec(51, v_start, 1190, 3200);
  select * into w from public.walk_sessions where user_id = t.u(51) and party_id = p;
  v_first := w.points_awarded;
  perform t.chk('(라) 넷이 출발했어도 혼자 달렸으면 배수 없음(보상 이벤트 × 2 만)', abs(w.points_awarded - round(3200 * 0.0118 * 1.4 * 2, 4)) < 0.01
                and w.together_n = 1 and w.together_paid = 0, format('%s SUP · %s명', w.points_awarded, w.together_n));
  -- 번개 글의 거리 5km — 절반(2.5km)을 넘게 달려야 함께 달린 사람이다(20분 · 약 3.1km)
  perform t.rec(52, v_start, 1190, 3200);
  select * into w from public.walk_sessions where user_id = t.u(51) and party_id = p;
  perform t.chk('(라) 함께 달린 둘째가 올리면 첫째에게 × 1.1', abs(w.points_awarded - v_first * 1.1) < 0.02 and w.together_n = 2,
                format('%s SUP · %s명', w.points_awarded, w.together_n));

  -- (마) 하루 최대 — 1시간 값 × 0.3 으로 낮추면 혼자 1시간이 걸리고 목표 보너스도 남은 만큼만(보상 이벤트는 끈다)
  delete from economy.reward_events;
  update public.economy_settings set value = '0.3' where key = 'daily_max_hour_mult';
  update public.economy_settings set value = '[]'::jsonb where key = 'run_box_steps';
  v_max := economy.daily_max_of(t.u(55), 4000, 1);
  v_start := economy.game_day_start(economy.game_day(now())) + interval '30 minutes';
  if v_start + interval '1 hour' > now() then v_start := now() - interval '70 minutes'; end if;
  perform t.login(55);
  select * into r from public.record_session(v_start, v_start + interval '3600 seconds', 6000, 3600, t.track(v_start, 3600, 0.000015), 0, 1, '', false);
  perform t.chk('(마) 하루 최대에 걸린다', r.points_awarded = v_max and r.verdict_reason like '%하루 최대 SUP 에 걸렸습니다%',
                format('%s SUP (최대 %s) · %s', r.points_awarded, v_max, r.verdict_reason));
  perform t.chk('(마) 목표 보너스도 하루 최대 안에서 — 남은 것이 없으면 0',
                not exists (select 1 from public.sup_ledger l where l.user_id = t.u(55) and l.ref like 'goal:%'), 'ok');
  update public.economy_settings set value = '4' where key = 'daily_max_hour_mult';
end $$;
do $$
declare p bigint; v jsonb; cap0 numeric; cap1 numeric;
begin
  -- (바) 출발한 뒤 계정을 지운다 — 그 사람의 안 쓴 체력을 뺀다
  perform t.mk(61); perform t.mk(62); perform t.mk(63);
  perform t.crew(6, 61, array[62, 63], 1);
  perform t.login(61);
  p := public.party_create_v2(t.c(6), '삭제', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(62); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(63); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(61); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  select capacity into cap0 from economy.crew_day_pools where crew_id = t.c(6);
  delete from auth.users where id = t.u(63);
  select capacity into cap1 from economy.crew_day_pools where crew_id = t.c(6);
  perform t.chk('(바) 계정을 지우면 맡긴 체력이 빠진다', cap1 <= cap0 - 149, format('%s → %s', cap0, cap1));
  -- (사) 출발한 뒤 박동이 없는 사람(앱 강제 종료) — 30초 뒤 묶인다
  perform t.run(p, array[61], 60);
  perform t.login(61); v := public.crew_run_beat(p, 'RUNNING');
  perform t.chk('(사) 소식이 끊긴 사람의 체력은 묶인다', (v -> 'pool' ->> 'held')::numeric >= 145, 'held ' || (v -> 'pool' ->> 'held'));
  perform t.run(p, array[61, 62], 60);
  perform t.login(61); v := public.crew_run_beat(p, 'RUNNING');
  perform t.chk('(사) 다시 달리면 풀린다', (v -> 'pool' ->> 'held')::numeric < 1, 'held ' || (v -> 'pool' ->> 'held'));
end $$;
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; c economy.crew_runs;
begin
  -- (아) 5km 코스 — 목표의 절반(2.5km) 이상 함께 달린 사람만 센다. 셋째는 15분(약 2.4km)에 그만 — 10분 · 1,500보는 넘지만 절반은 못 채웠다
  perform t.login(71);
  p := public.party_create_v2(t.c(7), '5km', '한강 5km', 5, '37.5,127;37.545,127', gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 72 .. 73 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(71); perform public.party_start_rev(p, (select course_rev from public.parties where id = p));
  perform t.age(interval '6 seconds');
  perform t.run(p, array[71, 72, 73], 900);
  perform t.login(73); v := public.crew_run_beat(p, 'ENDED');
  select started_at into v_start from economy.crew_runs where user_id = t.u(71);
  perform t.rec(73, v_start, 900, 2475);
  perform t.run(p, array[71, 72], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(71);
  for n in 71 .. 72 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); perform t.rec(n, v_start, 1500, 4125); end loop;
  select * into c from economy.crew_runs where user_id = t.u(71);
  perform t.chk('(아) 5km 코스에서 2.5km 넘게 달린 둘만 센다', c.bonus_count = 2,
                format('%s명 · 셋째 거리 %s m', c.bonus_count, (select round(gps_credit_m) from public.walk_sessions s where s.user_id = t.u(73) and s.party_id = p)));
  select * into c from economy.crew_runs where user_id = t.u(73);
  perform t.chk('(아) 절반을 못 채운 사람은 인원 보너스 없음 · 체력은 쓴 만큼만', c.bonus_reason = 'NOT_TOGETHER'
                and (select (v2 ->> 'held')::numeric from (select economy.crew_pool_json(t.c(7), c.day) v2) q) >= 0,
                coalesce(c.bonus_reason, '-') || ' · 묶음 ' || (select economy.crew_pool_json(t.c(7), c.day) ->> 'held'));
end $$;

