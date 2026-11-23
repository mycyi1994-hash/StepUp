-- 감사 C(2026-11-22): 0121 뒤 하루 최대 SUP(0120 — 그 신발 1시간 × 4)는 어디서 걸리나 · 걸리면 러너 걸음은?
-- (표) 에너지로 받을 수 있는 걸음 ÷ 하루 최대의 걸음(39,600) — 러너 Lv1 · 예전 보장 10 · Lv21, 착화감 0 · 47% · 70%(상한), 효율 포인트 없음.
-- (가) 혼자: Lv21 · 착화감 70% 신발로 한 번 200분(33,000보). 보상 운 1.25(삼각 [0.7, 1, 1.3] 에서 하루 약 5.6%)면 하루 최대에 걸려 FLAGGED.
-- (나) 크루: Lv21 이 혼자 60분(CLEAN) 뒤 크루 Lv20 · 둘이 160분 — 크루 러닝이 하루 최대에 걸려 FLAGGED.
-- 둘 다 러너 걸음(runner_steps_of — CLEAN 만)에 한 걸음도 들지 않는다. 오늘의 보상 걸음(상자 · 목표)에는 든다.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;
-- 지난 러닝(2주 전, CLEAN) 20만 보 → 러너 Lv21
create or replace function t.lv21(n int) returns void language plpgsql as $$
declare d int;
begin
  for d in 10 .. 14 loop
    insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps, gps_backed, rewarded_steps)
    values (t.u(n), now() - make_interval(days => d) - make_interval(mins => n), now() - make_interval(days => d) - make_interval(mins => n) + interval '4 hours',
            14400, 40000, 'CLEAN', 40000, 40000, true, 0);
  end loop;
end $$;
select t.mk(1, 1500, 4000), t.mk(2, 1500, 4000), t.mk(3, 500, 4000), t.mk(4, 500, 4000);
select t.lv21(n) from generate_series(1, 4) n;
select t.crew(9, 3, array[4], 20);
do $$
declare v_l int; v_e numeric; c numeric; v_cap numeric := 4 * 9900;
begin
  foreach v_l in array array[1, 10, 21] loop
    v_e := case when v_l = 10 then 10 else economy.runner_energy_for(v_l) end;
    perform t.say(format('하루 에너지 %s(%s)', v_e, case v_l when 1 then '새 계정 Lv1' when 10 then '0121 전 계정 보장' else 'Lv21+' end),
      (select string_agg(format('착화감 %s%% → 걸음 %s = 하루 최대의 %s%%', x * 100, round(v_e * 600 / (1 - x)), round(100 * v_e * 600 / (1 - x) / v_cap)), ' · ')
         from unnest(array[0, 0.47, 0.7]) x));
  end loop;
end $$;
do $$
declare v_t timestamptz; r record; v_s0 bigint; v_s1 bigint; n int; v_luck numeric; w public.walk_sessions; v_day date;
begin
  -- (가) 혼자 — 착화감 70%(상한) 신발, 200분 · 33,000보(에너지 16.5 ÷ 0.3 × 600 = 33,000 걸음 전부 적립)
  foreach n in array array[1, 2] loop
    v_luck := case n when 1 then 1.25 else 1.0 end;
    update public.economy_settings set value = to_jsonb(array[v_luck, v_luck, v_luck]) where key = 'reward_luck';
    update public.market_sneakers set comfort_bps = 7000 where owner_id = t.u(n) and equipped;
    v_t := economy.game_day_start(economy.game_day(now()) - 1) + interval '8 hours';
    v_s0 := (select steps from economy.runner_levels where user_id = t.u(n));
    perform t.login(n);
    select * into r from public.record_session(v_t, v_t + interval '200 minutes', 33000, 12000, t.track(v_t, 12000), 0, 1, '', false, null);
    select * into w from public.walk_sessions where id = r.session_id;
    v_s1 := (select steps from economy.runner_levels where user_id = t.u(n));
    perform t.say(format('(가) u%s 보상 운 %s', n, v_luck), format('%s · %s SUP / 하루 최대 %s · [%s] · 보상 걸음 %s · 러너 걸음 +%s',
      r.verdict, r.points_awarded, w.day_max, r.verdict_reason, w.reward_steps, v_s1 - v_s0));
    if n = 1 then
      perform t.chk('(가) 하루 최대에 걸린 러닝도 경로로 확인된 걸음은 러너 레벨에 든다(에너지를 다 쓴 걸음처럼)', v_s1 - v_s0 >= 33000,
                    format('러너 걸음 +%s / 확인된 걸음 %s', v_s1 - v_s0, w.verified_steps));
    end if;
  end loop;
  update public.economy_settings set value = '[1,1,1]' where key = 'reward_luck';
end $$;
do $$
declare p bigint; v jsonb; v_start timestamptz; r text; v_s0 bigint; v_s1 bigint; w public.walk_sessions; c economy.crew_runs; v_t timestamptz;
begin
  -- (나) 크루 날 — u3 이 먼저 혼자 60분(9,900보 · 착화감 0) 뒤 크루 Lv20 방에서 u3 · u4 가 160분(26,400보)
  v_s0 := (select steps from economy.runner_levels where user_id = t.u(3));
  v_t := now() - interval '65 minutes';
  r := t.rec(3, v_t, 3600, 9900);
  perform t.say('(나) u3 혼자 60분', r);
  perform t.login(3);
  p := public.party_create_v2(t.c(9), '긴 크루', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(4); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(3); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[3, 4], 9600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(3);
  perform t.login(3); v := public.crew_run_beat(p, 'ENDED');
  perform t.login(4); v := public.crew_run_beat(p, 'ENDED');
  r := t.rec(4, v_start, 9600, 26400);
  r := t.rec(3, v_start, 9600, 26400);
  select * into c from economy.crew_runs where user_id = t.u(3);
  select * into w from public.walk_sessions where id = c.session_id;
  v_s1 := (select steps from economy.runner_levels where user_id = t.u(3));
  perform t.say('(나) u3 크루 160분', format('%s · %s SUP(기본 %s · 레벨 %s · 인원 %s) / 하루 최대 %s · 그날 받은 SUP %s · [%s] · 보상 걸음 %s',
    w.verdict, w.points_awarded, c.base_sup, c.level_sup, c.headcount_sup, w.day_max, economy.day_earned(t.u(3), economy.game_day(w.started_at)),
    w.verdict_reason, w.reward_steps));
  perform t.chk('(나) 크루 러닝의 확인된 걸음도 러너 레벨에 든다', v_s1 - v_s0 >= 9900 + 26400,
                format('그날 경로로 확인된 걸음 %s 가운데 러너 걸음 +%s · 오늘의 보상 걸음 %s', 9900 + 26400, v_s1 - v_s0,
                       economy.reward_steps_on(t.u(3), economy.game_day(w.started_at))));
end $$;
