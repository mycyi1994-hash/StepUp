-- 점검 2 (J): 과속(25km/h 30초)은 앱만 본다 — report_overspeed_stop 을 부르지 않으면 서버는 시속 60km 아래 구간을 모두 사람으로 친다.
-- 차(50km/h) · 자전거(24km/h)에 폰 흔들개(240보/분)를 달고 올린 러닝. 서버의 남은 검사: 60km/h 넘는 구간 50% · 걸음이 GPS 거리의 20% 아래.
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(51, 53) n;
select t.old(n) from generate_series(51, 53) n;
do $$
declare v_now timestamptz := date_trunc('minute', now()); v_start timestamptz := v_now - interval '3 hours'; w public.walk_sessions;
begin
  w := t.up(51, v_start, 1800, 7200, t.trku(51, v_start, 1800, 13.9));
  perform t.say('u51 차 50km/h 30분', t.line(w));
  w := t.up(52, v_start, 3600, 9900, t.trku(52, v_start, 3600, 6.67));
  perform t.say('u52 자전거 24km/h 60분', t.line(w));
  w := t.up(53, v_start, 3600, 9900, t.trku(53, v_start, 3600));
  perform t.say('u53 정직 9.5km/h 60분', t.line(w));
  perform t.say('profile', (select string_agg(format('u%s top=%s gps_km=%s', right(id::text, 2), round(top_speed_kmh::numeric, 1), round(gps_km::numeric, 1)), ' · ' order by id)
                            from public.profiles where id in (t.u(51), t.u(52), t.u(53))));
  perform t.say('run_penalties', (select count(*) from public.run_penalties where user_id in (t.u(51), t.u(52))));
end $$;
