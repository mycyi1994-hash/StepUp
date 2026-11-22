-- 검사(0122 — 경제 점검 고침, 함께 뛰기 쪽): 묶어 둔 체력 · 돌려받기 · 크루 효율 8 : 2 의 무게 · 번개 한 번 · 규칙 뒤집기 ·
-- 하루 최대에 걸린 러닝의 러너 걸음 · 크루에서 하루 최대에 닿아도 목표 날. 줄마다 OK / FAIL — CI(test-sql.yml)가 FAIL 이 있으면 멈춘다.
-- 점검 docs/점검-2026-11-22-경제.md. 계정 · 경제 쪽(신발 등록 · 에너지 상한 · 가입 선물 …)은 서버 검사 "경제 점검 고침(0122)".
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;

-- (가) 20명 출발 · 18명 6초 만에 끝냄 · 크루 Lv20 · 보상 이벤트 × 2 — 남은 둘이 박동 없이(비행기 모드) 3시간.
-- 예전엔 올릴 때 묶어 둔 체력까지 써서 각자 1,217 SUP(박동을 보낸 같은 판 16 (가)는 433). 이제 묶인 몫은 쓰지 않는다
select t.mk(n) from generate_series(11, 30) n;
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(13, 30) n);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; w11 public.walk_sessions; w12 public.walk_sessions; d economy.crew_day_pools; v_held numeric;
begin
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '오프라인', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  for n in 13 .. 30 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.login(11); v := public.crew_run_beat(p, 'RUNNING');
  perform t.age(interval '10800 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  perform t.rec(11, v_start, 10800, 29700);
  perform t.rec(12, v_start, 10800, 29700);
  select * into w11 from public.walk_sessions x where x.user_id = t.u(11) and x.party_id = p;
  select * into w12 from public.walk_sessions x where x.user_id = t.u(12) and x.party_id = p;
  select * into d from economy.crew_day_pools x where x.crew_id = t.c(2);
  v_held := economy.crew_pool_held(t.c(2), d.day, now());
  perform t.chk('(가) 박동 없이 달린 둘은 묶어 둔 체력을 쓰지 못한다 — 박동을 보낸 판(433)과 비슷하게',
                w11.points_awarded < 500 and w12.points_awarded < 500 and d.consumed <= d.capacity - v_held + 0.01,
                format('u11 %s · u12 %s SUP (예전 1,168 · 1,217) · 쓴 체력 %s / %s · 묶인 %s', w11.points_awarded, w12.points_awarded,
                       round(d.consumed, 1), round(d.capacity, 1), round(v_held, 1)));
  delete from economy.reward_events;
end $$;

-- (나) 묶인 체력 비켜 가기(2차 점검 E1): 부계정 18명이 달리지 않고 박동만 10분 10초 보내고 끝내며 러닝을 올리지 않는다 — 남은 둘은 3시간 내내 박동.
-- 0122 첫 판은 "서버가 센 시간 10분 미만"만 묶어 둘이 각자 1,217 SUP. 이제 끝냈는데 함께 달린 사람으로 올리지 않은 사람의 몫은 묶는다
select t.mk(n) from generate_series(311, 330) n;
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(313, 330) n);
select t.crew(31, 311, (select array_agg(n) from generate_series(312, 330) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; i int; w1 public.walk_sessions; w2 public.walk_sessions; d economy.crew_day_pools;
  arr int[] := (select array_agg(g) from generate_series(313, 330) g);
begin
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(311);
  p := public.party_create_v2(t.c(31), '가짜 박동', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 312 .. 330 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(311); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  for i in 1 .. 25 loop
    perform t.age(interval '25 seconds');
    foreach n in array arr || array[311, 312] loop perform t.login(n); v := public.crew_run_beat(p, 'RUNNING'); end loop;
  end loop;
  for n in 313 .. 330 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  for i in 1 .. ((10800 - 625) / 25) loop
    perform t.age(interval '25 seconds');
    perform t.login(311); v := public.crew_run_beat(p, 'RUNNING');
    perform t.login(312); v := public.crew_run_beat(p, 'RUNNING');
  end loop;
  perform t.login(311); v := public.crew_run_beat(p, 'ENDED');
  perform t.login(312); v := public.crew_run_beat(p, 'ENDED');
  select started_at into v_start from economy.crew_runs where user_id = t.u(311);
  perform t.rec(311, v_start, 10800, 29700);
  perform t.rec(312, v_start, 10800, 29700);
  select * into w1 from public.walk_sessions x where x.user_id = t.u(311) and x.party_id = p;
  select * into w2 from public.walk_sessions x where x.user_id = t.u(312) and x.party_id = p;
  select * into d from economy.crew_day_pools x where x.crew_id = t.c(31);
  perform t.chk('(나) 10분 가짜 박동 뒤 끝낸 부계정의 몫은 묶인다 — 박동을 보낸 정직한 판(433)과 비슷하게',
                w1.points_awarded < 500 and w2.points_awarded < 500,
                format('u311 %s · u312 %s SUP (0122 첫 판 1,217) · 쓴 체력 %s / %s · 묶인 %s', w1.points_awarded, w2.points_awarded,
                       round(d.consumed, 1), round(d.capacity, 1), round(economy.crew_pool_held(t.c(31), d.day, now()), 1)));
  delete from economy.reward_events;
end $$;

-- (나2) 출발을 놓친 사람(박동 한 번도 없음)은 15분 뒤에도 돌려받아 같은 날 다른 방에서 달릴 수 있다 — 그 몫은 그동안 묶여 쓰이지 않았다
do $$
declare p bigint; p2 bigint; v jsonb; n int; ok boolean; cap0 numeric; used0 numeric;
begin
  perform t.mk(341); perform t.mk(342); perform t.mk(343, 1000, 4000); perform t.mk(344);
  perform t.crew(34, 341, array[342, 343, 344], 1);
  perform t.login(341);
  p := public.party_create_v2(t.c(34), '방 1', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 342 .. 343 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(341); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[341, 342], 900);
  select consumed into used0 from economy.crew_day_pools where crew_id = t.c(34);
  perform t.login(343); ok := public.crew_run_release(p);
  perform t.login(344);
  p2 := public.party_create_v2(t.c(34), '방 2', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p2, true);
  perform t.login(343); perform public.party_join(p2); perform public.party_ready(p2, true);
  perform t.login(344); perform public.party_start_rev(p2, 0);
  perform t.chk('(나2) 출발을 놓친 사람은 늦게도 돌려받고 다른 방에서 달린다 · 그동안 그 몫은 쓰이지 않았다',
                ok and exists (select 1 from economy.crew_runs c where c.user_id = t.u(343) and c.party_id = p2),
                format('돌려받기 %s · 방 2 출발 %s · 그동안 쓴 체력 %s (방 1 둘의 몫 300 안)', ok,
                       exists (select 1 from economy.crew_runs c where c.user_id = t.u(343) and c.party_id = p2), round(used0, 1)));
end $$;

-- (다) 크루 효율 8 : 2 의 무게 — 셋 가운데 둘은 약한 신발(1.4)로 60분, 셋째만 10분.
--   (B) 셋째가 피니시 신발: 예전엔 사람 수로 평균을 내 약한 둘이 각각 285 SUP(+58%). 이제 남의 무게는 내 무게까지 → 약 +13%
--   (C) 셋째가 보류 계정(지난 7일 무효 5번 — 보상 걸음 0): 함께 달린 사람이 아니다(평균 · 인원에 들지 않는다)
create or replace function t.run25(p_party bigint, p_users int[], p_secs int) returns void language plpgsql as $$
declare i int; m int; v jsonb;
begin
  for i in 1 .. p_secs / 25 loop
    perform t.age(interval '25 seconds');
    foreach m in array p_users loop perform t.login(m); v := public.crew_run_beat(p_party, 'RUNNING'); end loop;
  end loop;
end $$;
create or replace function t.blend(k int, a int, b int, s int, s_eff int, s_held boolean) returns jsonb language plpgsql as $$
declare p bigint; v jsonb; v_start timestamptz; n int; i int; w public.walk_sessions; c economy.crew_runs; out jsonb := '{}'::jsonb;
begin
  perform t.mk(a, 150, 4000); perform t.mk(b, 150, 4000); perform t.mk(s, 1000, s_eff);
  if s_held then
    perform t.login(s);
    for i in 1 .. 5 loop
      perform public.record_session(now() - make_interval(days => i), now() - make_interval(days => i) + interval '10 minutes', 1000, 600, '', 0, 1, '', true);
    end loop;
  end if;
  perform t.crew(k, a, array[b, s], 1);
  perform t.login(a);
  p := public.party_create_v2(t.c(k), 'blend', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  foreach n in array array[b, s] loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(a); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run25(p, array[a, b, s], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(a);
  perform t.login(s); v := public.crew_run_beat(p, 'ENDED');
  perform t.rec(s, v_start, 600, 1650);
  perform t.run25(p, array[a, b], 3000);
  foreach n in array array[a, b] loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  select started_at into v_start from economy.crew_runs where user_id = t.u(a);
  perform t.rec(a, v_start, 3600, 9900);
  perform t.rec(b, v_start, 3600, 9900);
  foreach n in array array[a, b, s] loop
    select * into w from public.walk_sessions x where x.user_id = t.u(n) and x.party_id = p;
    select * into c from economy.crew_runs x where x.user_id = t.u(n) and x.party_id = p;
    out := out || jsonb_build_object('u' || n, jsonb_build_object('sup', w.points_awarded, 'n', c.bonus_count,
                                     'mult', round(c.base_sup / nullif(w.together_unit, 0), 3)));
  end loop;
  return out;
end $$;
do $$
declare a jsonb; b jsonb; c jsonb;
begin
  a := t.blend(11, 101, 102, 103, 4000, false);
  b := t.blend(12, 111, 112, 113, 126688, false);
  c := t.blend(13, 121, 122, 123, 126688, true);
  perform t.chk('(다) 10분 달린 피니시 신발이 60분 달린 약한 신발을 사람 수만큼 끌어올리지 않는다 — 효율 1.4 → 1.7 아래',
                (b -> 'u111' ->> 'mult')::numeric < 1.7 and (b -> 'u111' ->> 'mult')::numeric > 1.4
                and (b -> 'u111' ->> 'sup')::numeric < (a -> 'u101' ->> 'sup')::numeric * 1.2,
                format('약한 신발 %s SUP(효율 %s · 예전 285 · 2.218) · 셋 다 약할 때 %s', b -> 'u111' ->> 'sup', b -> 'u111' ->> 'mult', a -> 'u101' ->> 'sup'));
  perform t.chk('(다) 보상 걸음 0(보류 계정)은 함께 달린 사람이 아니다 — 인원 · 평균에 들지 않는다',
                (c -> 'u121' ->> 'n')::int = 2 and (c -> 'u121' ->> 'mult')::numeric <= 1.4001,
                format('u121 %s명 · 효율 %s (예전 3명 · 2.218)', c -> 'u121' ->> 'n', c -> 'u121' ->> 'mult'));
end $$;

-- (라) 원격 번개: 같은 글로 방 둘 — 친구들보다 먼저 둘 다 올려도 번개 보너스는 글 하나에 한 번
select t.mk(1), t.mk(2), t.mk(3);
do $$
declare post bigint; pa bigint; pb bigint; va timestamptz; vb timestamptz; n_rows int; b_base numeric;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '번개', '', '', 5, now() + interval '30 minutes', 5, null, null, gen_random_uuid(), 'REMOTE');
  perform t.login(2); perform public.join_flash(post);
  perform t.login(3); perform public.join_flash(post);
  perform t.login(1); pa := public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(2); perform public.party_open(null, post); perform public.party_ready(pa, true);
  perform t.login(1); perform public.party_start(pa);
  perform t.age(interval '20 minutes 4 seconds');
  select starts_at into va from public.party_runs where party_id = pa and user_id = t.u(1);
  perform t.rec(1, va, 1190, 2400);
  perform t.login(1); perform public.party_finish(pa);
  perform t.login(1); pb := public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(3); perform public.party_open(null, post); perform public.party_ready(pb, true);
  perform t.login(1); perform public.party_start(pb);
  perform t.age(interval '20 minutes 4 seconds');
  select starts_at into vb from public.party_runs where party_id = pb and user_id = t.u(1);
  select starts_at into va from public.party_runs where party_id = pa and user_id = t.u(1);
  perform t.rec(1, vb, 1180, 2400);
  perform t.rec(2, va, 1190, 2400);
  perform t.rec(3, vb, 1180, 2400);
  select count(*) into n_rows from public.sup_ledger l where l.user_id = t.u(1) and l.ref like 'together:%';
  select x.together_base into b_base from public.walk_sessions x where x.user_id = t.u(1) and x.started_at = vb;
  perform t.chk('(라) 원격 번개 보너스는 글 하나에 한 번 — 둘째 방은 보너스 없는 러닝', n_rows = 1 and b_base is null,
                format('함께 달린 보너스 줄 %s · 둘째 방 바탕 %s (예전 줄 2)', n_rows, coalesce(b_base::text, '없음')));
end $$;

-- (마) 크루 보너스 규칙을 ALL(되돌리기)로 둔 동안 올린 러닝 — 다시 ACTIVE 로 켜도 인원 보너스를 한 번 더 주지 않는다
update public.economy_settings set value = '"ALL"' where key in ('crew_bonus_rule', 'party_bonus_rule');
select t.mk(n) from generate_series(51, 53) n;
select t.crew(5, 51, array[52, 53], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; c economy.crew_runs; w public.walk_sessions; v_extra numeric;
begin
  perform t.login(51);
  p := public.party_create_v2(t.c(5), '스위치', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 52 .. 53 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(51); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[51, 52, 53], 900);
  select started_at into v_start from economy.crew_runs where user_id = t.u(51);
  for n in 51 .. 53 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.rec(51, v_start, 900, 2475);
  update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
  perform t.rec(52, v_start, 900, 2475);
  perform t.rec(53, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(51);
  select * into w from public.walk_sessions where id = c.session_id;
  select coalesce(sum(l.amount), 0) into v_extra from public.sup_ledger l where l.user_id = t.u(51) and l.ref like 'together:%';
  perform t.chk('(마) ALL 동안 받은 인원 보너스는 준 것으로 적힌다 — ACTIVE 로 다시 켜도 더하지 않는다',
                v_extra = 0 and abs(w.together_paid - c.headcount_sup) < 0.0001 and c.headcount_sup > 0,
                format('인원 %s · 준 것으로 적힘 %s · 나중에 더한 것 %s (예전 4.09 를 한 번 더)', c.headcount_sup, w.together_paid, v_extra));
end $$;

-- (바) 하루 최대 SUP 에 닿아 FLAGGED 가 된 러닝도 러너 걸음에 든다 — 가입 7일 안(하루 최대 절반)과 오래된 계정이 같은 걸음이면 같은 러너 걸음.
-- 정직한 20명 · 크루 Lv20 · 1시간 뒤 각자 혼자 20분 두 번(예전엔 새 계정 10,200 · 오래된 계정 16,800)
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
create or replace function t.track(p_start timestamptz, p_secs int, p_dps numeric default 0.000023706) returns text language sql as $$
  select string_agg(format('%s,%s,%s', (37.5 + i * p_dps)::numeric(12, 6), (127 + (abs(hashtext(coalesce(current_setting('request.jwt.claim.sub', true), ''))) % 997 + 1) * 0.00000001)::numeric(12, 8),
           (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
  from generate_series(0, p_secs) i $$;
select t.mk(n) from generate_series(211, 230) n;
update public.profiles set created_at = now() - interval '3 days' where id = t.u(211);  -- 가입 7일 안(하루 최대 절반) · 함께 달린 사람은 24시간 뒤부터(0123)
select t.crew(21, 211, (select array_agg(n) from generate_series(212, 230) n), 20);
do $$
declare p bigint; v_start timestamptz; n int; v_t0 timestamptz; s1 bigint; s2 bigint; f int;
begin
  perform t.login(211);
  p := public.party_create_v2(t.c(21), '정직 20명', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 212 .. 230 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(211); perform public.party_start_rev(p, 0);
  perform t.age(interval '3706 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(211);
  for n in 211 .. 230 loop perform t.rec(n, v_start, 3700, 10200); end loop;
  perform t.age(interval '50 minutes');
  v_t0 := now() - interval '2900 seconds';
  foreach n in array array[211, 212] loop
    perform t.rec(n, v_t0, 1200, 3300);
    perform t.rec(n, v_t0 + interval '1300 seconds', 1200, 3300);
  end loop;
  select steps into s1 from economy.runner_levels where user_id = t.u(211);
  select steps into s2 from economy.runner_levels where user_id = t.u(212);
  select count(*) into f from public.walk_sessions x where x.user_id = t.u(211) and x.verdict = 'FLAGGED';
  perform t.chk('(바) 하루 최대에 걸린(FLAGGED) 러닝의 걸음도 러너 레벨에 든다', s1 = s2 and s1 = 16800,
                format('새 계정 %s · 오래된 계정 %s 걸음 · 새 계정 FLAGGED 러닝 %s (예전 10,200 · 16,800)', s1, s2, f));
end $$;

-- (사) 둘이 3시간 50분 크루 러닝(38,000보)으로 둘 다 하루 최대에 닿는다 — 지난 엿새 목표를 채운 둘.
-- 예전엔 먼저 올린 사람만 목표 줄 · 7일 상급 상자를 받고 나중 사람은 연속 0(인원 보너스가 먼저 남은 몫을 채웠다)
select t.mk(91, 3000); select t.mk(92, 3000);
select t.crew(9, 91, array[92], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; i int; v_day date; ok boolean := true; d text := '';
begin
  perform t.login(91);
  p := public.party_create_v2(t.c(9), '긴 러닝', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(92); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(91); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(91);
  v_day := economy.game_day(v_start);
  for n in 91 .. 92 loop
    for i in 1 .. 6 loop
      perform economy.ledger_apply(t.u(n), 'BONUS_GOAL', 15, '설정: 지난 목표', 'goal:' || (v_day - i));
    end loop;
  end loop;
  perform t.run(p, array[91, 92], 13800, 'RUNNING', 25);
  select started_at into v_start from economy.crew_runs where user_id = t.u(91);
  for n in 91 .. 92 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.rec(91, v_start, 13800, 38000);
  perform t.rec(92, v_start, 13800, 38000);
  for n in 91 .. 92 loop
    ok := ok and exists (select 1 from economy.goal_days g where g.user_id = t.u(n) and g.day = v_day and g.met_at is not null)
             and exists (select 1 from economy.box_awards a where a.user_id = t.u(n) and a.source like 'STREAK:%')
             and economy.goal_streak_before(t.u(n), v_day + 1, 60) >= 7
             and economy.day_earned(t.u(n), v_day) <= (select max(s.day_max) from public.walk_sessions s where s.user_id = t.u(n)) + 0.01;
    d := d || format('u%s 받은 %s / 최대 %s · 연속 %s · 7일 상자 %s ', n, round(economy.day_earned(t.u(n), v_day), 2),
                     (select max(s.day_max) from public.walk_sessions s where s.user_id = t.u(n)), economy.goal_streak_before(t.u(n), v_day + 1, 60),
                     exists (select 1 from economy.box_awards a where a.user_id = t.u(n) and a.source like 'STREAK:%'));
  end loop;
  perform t.chk('(사) 하루 최대에 닿아도 둘 다 목표를 채운 날 · 7일 연속 상급 상자 — 올린 차례와 상관없이', ok, d);
end $$;
