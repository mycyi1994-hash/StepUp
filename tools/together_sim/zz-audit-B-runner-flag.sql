-- 감사 B: 하루 최대 SUP(0120)에 닿으면 그날 남은 러닝은 FLAGGED — 러너 레벨(0121)은 CLEAN 만 센다.
-- 정직한 20명 · 크루 Lv20 · 1시간(10,200보, 연결 끊김 — 0117 정산) 뒤 각자 혼자 20분(에너지 안, 3,300보) 두 번.
-- u11 = 가입 7일 안(하루 최대 절반), u12 = 오래된 계정. 같은 걸음인데 러너 걸음이 다르다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
-- 도우미의 경도 차이(49가지)는 20명이면 겹쳐 "이미 올린 경로"가 된다 — 이 장면에서만 넓힌다(1m 안쪽)
create or replace function t.track(p_start timestamptz, p_secs int, p_dps numeric default 0.000023706) returns text language sql as $$
  select string_agg(format('%s,%s,%s', (37.5 + i * p_dps)::numeric(12, 6), (127 + (abs(hashtext(coalesce(current_setting('request.jwt.claim.sub', true), ''))) % 997 + 1) * 0.00000001)::numeric(12, 8),
           (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
  from generate_series(0, p_secs) i $$;
select t.mk(n) from generate_series(11, 30) n;
update public.profiles set created_at = now() - interval '1 day' where id = t.u(11);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
do $$
declare p bigint; v_start timestamptz; n int; r text; s record; v_t0 timestamptz;
begin
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '정직 20명', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.age(interval '3700 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  for n in 11 .. 30 loop perform t.rec(n, v_start, 3700, 10200); end loop;
  -- 50분 뒤 혼자 20분씩 두 번
  perform t.age(interval '50 minutes');
  v_t0 := now() - interval '2900 seconds';
  foreach n in array array[11, 12] loop
    perform t.rec(n, v_t0, 1200, 3300);
    perform t.rec(n, v_t0 + interval '1300 seconds', 1200, 3300);
  end loop;
  foreach n in array array[11, 12] loop
    for s in select x.started_at, x.energy_pool, x.verdict, x.points_awarded, x.reward_steps, x.verdict_reason, x.day_max
               from public.walk_sessions x where x.user_id = t.u(n) order by x.started_at loop
      perform t.say(format('u%s %s', n, s.energy_pool), format('%s %s SUP · 보상 걸음 %s · 하루 최대 %s · [%s]', s.verdict, s.points_awarded, s.reward_steps, s.day_max, s.verdict_reason));
    end loop;
    perform t.say(format('u%s 러너', n), (select format('걸음 %s · Lv%s · 상자 %s · 오늘 받은 %s SUP', r2.steps, r2.level,
              (select count(*) from economy.box_awards b where b.user_id = t.u(n) and b.source like 'RUNNER%'),
              round(economy.day_earned(t.u(n), economy.game_day(v_start)), 2))
              from economy.runner_levels r2 where r2.user_id = t.u(n)));
  end loop;
  perform t.say('같은 한국 날짜?', (economy.game_day(v_start) = economy.game_day(now()))::text);
end $$;
