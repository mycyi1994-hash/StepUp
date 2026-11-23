-- 점검 2 (J): 막혀 있는 것들 — 경로 시각 · 겹침 · 같은 시작 다시 · 미래 · 8일 전 · 자정 넘김 · 박동만 · 경로 없음 · 순간이동 · 예전 9칸 호출
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(61, 69) n;
select t.old(n) from generate_series(61, 69) n;
select t.crew(61, 61, array[62, 63], 1);
do $$
declare v_now timestamptz := date_trunc('minute', now()); v_start timestamptz; w public.walk_sessions; r record; v jsonb; p bigint; n int;
        c economy.crew_runs; v_t text;
begin
  -- 경로 시각이 시작 3분 전부터
  v_start := v_now - interval '3 hours';
  w := t.up(61, v_start, 1800, 4950, t.trku(61, v_start - interval '3 minutes', 1980));
  perform t.say('경로가 시작 3분 전부터', t.line(w));
  -- 겹치는 둘째 러닝(1분 뒤 시작)
  w := t.up(62, v_start, 1800, 4950, t.trku(62, v_start, 1800));
  w := t.up(62, v_start + interval '1 minute', 1800, 4950, t.trku(62, v_start + interval '1 minute', 1800, 2.7));
  perform t.say('겹치는 둘째', t.line(w));
  -- 같은 시작 시각 · 다른 값으로 다시 → 처음 결과 그대로
  perform t.login(62);
  select * into r from public.record_session(v_start, v_start + interval '1800 seconds', 30000, 1800, t.trku(62, v_start, 1800, 4), 0, 1, '', false, null);
  perform t.say('같은 시작 · 걸음 30,000 으로 다시', format('%s %s', r.verdict, r.points_awarded));
  -- 미래 끝(6분 뒤) · 8일 전 시작
  begin
    perform t.login(63);
    perform public.record_session(v_now - interval '10 minutes', now() + interval '6 minutes', 100, 960, '', 0, 1, '', false, null);
    perform t.say('6분 뒤 끝', 'accepted');
  exception when others then perform t.say('6분 뒤 끝', sqlerrm); end;
  begin
    perform public.record_session(now() - interval '8 days', now() - interval '8 days' + interval '30 minutes', 4950, 1800, '', 0, 1, '', false, null);
    perform t.say('8일 전', 'accepted');
  exception when others then perform t.say('8일 전', sqlerrm); end;
  -- 자정 넘김: 어제 22:30 ~ 오늘 01:30(KST) 3시간 29,700보 · 오늘 01:40 30분
  v_start := (date_trunc('day', now() at time zone 'Asia/Seoul') - interval '90 minutes') at time zone 'Asia/Seoul';
  w := t.up(64, v_start, 10800, 29700, t.trku(64, v_start, 10800));
  perform t.say('자정 넘김(어제 시작)', t.line(w) || ' day=' || economy.game_day(w.started_at));
  w := t.up(64, v_start + interval '190 minutes', 1800, 4950, t.trku(64, v_start + interval '190 minutes', 1800, 2.7));
  perform t.say('오늘 01:40', t.line(w) || ' day=' || economy.game_day(w.started_at));
  perform t.say('에너지 날', (select string_agg(day || ':' || used, ' ') from public.energy_days where user_id = t.u(64)));
  perform t.say('러너 걸음 날', (select string_agg(d || ':' || s, ' ') from (select economy.game_day(started_at) d, sum(reward_steps) s
                                    from public.walk_sessions where user_id = t.u(64) group by 1) x));
  -- 박동만(올리지 않음) — u62 · u63 이 박동만 보내고 u61 만 올림
  perform t.login(61);
  p := public.party_create_v2(t.c(61), '박동만', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 62 .. 63 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(61); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[61, 62, 63], 1200);
  for n in 61 .. 63 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  select * into c from economy.crew_runs where user_id = t.u(61);
  w := t.up(61, c.started_at, 1200, 3300, t.trku(61, c.started_at, 1200, 2.65));
  select * into c from economy.crew_runs where user_id = t.u(61);
  perform t.say('박동만 보낸 둘', format('u61 %s · 함께 %s명 · u62 SUP %s · u63 SUP %s', t.line(w), c.bonus_count,
                 economy.balance_of(t.u(62)), economy.balance_of(t.u(63))));
  -- 경로 없는 러닝: 하루 96 · 보상 걸음 0
  w := t.up(65, v_now - interval '5 hours', 3600, 9900, '');
  perform t.say('경로 없음', t.line(w));
  perform t.say('경로 없음 계정', t.me(65));
  -- 순간이동: 10초마다 59km/h 로 튐(나머지 0) — 거리 상한 7m/s
  v_start := v_now - interval '3 hours';
  select string_agg(format('%s,%s,%s', (37.6 + (i / 10) * 163.9 / 111320.0)::numeric(12, 7), 127.066,
                           (extract(epoch from v_start) * 1000)::bigint + i * 1000), ';' order by i)
    into v_t from generate_series(0, 1800, 10) i;
  w := t.up(66, v_start, 1800, 7200, v_t);
  perform t.say('59km/h 순간이동', t.line(w));
  -- 예전 앱(9칸, 신발 없음)
  perform t.login(67);
  select * into r from public.record_session(v_now - interval '3 hours', v_now - interval '2 hours', 9900, 3600,
                                             t.trku(67, v_now - interval '3 hours', 3600), 0, 1, '', false);
  perform t.say('9칸 호출', format('%s %s', r.verdict, r.points_awarded));
  -- 모의 위치 false 로 보내면 그냥 통과(앱이 정하는 값)
  w := t.up(68, v_now - interval '3 hours', 3600, 9900, t.trku(68, v_now - interval '3 hours', 3600), null, true);
  perform t.say('모의 위치 true', t.line(w));
end $$;
