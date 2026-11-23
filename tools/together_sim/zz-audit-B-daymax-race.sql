-- 감사 B: together_settle 은 다른 사람의 원장 자물쇠를 잡지 않는다(0120 — "서로 기다리며 멈추지 않게").
-- 그래서 X 가 혼자 러닝을 올리는 동안(X 의 원장 자물쇠 · 아직 커밋 전) 같은 크루 방의 Y 가 올리면,
-- 둘 다 X 의 "오늘 받은 SUP"를 X 의 새 러닝 없이 읽고 각자 남은 몫까지 준다 → 하루 최대를 넘는다.
-- 두 연결이 필요해 dblink 로 돌린다(연결마다 커밋된다 — 감사용 DB 에만 남고, 끝에 지운다). 설정은 기본값(ACTIVE · HOUR) 그대로.
--   X(201) 첫 신발 1.4 · Y(202) 피니시 꼭대기 13.67 — 크루 Lv1 둘, 박동 없이 X 3시간 · Y 10분. X 는 먼저 올렸다(하루 최대 654.2 중 505.6).
--   그 뒤 X 가 혼자 36분(에너지 안)을 올리는 바로 그때 Y 가 크루 러닝을 올린다(X 에게 크루 평균 8 : 2 · 인원 몫이 더해진다)
create extension if not exists dblink;
do $$
declare
  v_conn text := 'dbname=' || current_database() || ' port=' || current_setting('port') || ' user=postgres host=' || split_part(current_setting('unix_socket_directories'), ',', 1);
  v_start timestamptz; v_ps timestamptz; r text; i int; v_busy boolean; v_max numeric; v_earned numeric;
begin
  perform dblink_connect('s', v_conn);
  perform dblink_exec('s', $x$
    do $y$
    declare p bigint; v_start timestamptz;
    begin
      perform t.mk(201, 150, 4000); perform t.mk(202, 1000, 126688);
      perform t.crew(20, 201, array[202], 1);
      perform t.login(201);
      p := public.party_create_v2(t.c(20), 'race', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
      perform public.party_ready(p, true);
      perform t.login(202); perform public.party_join(p); perform public.party_ready(p, true);
      perform t.login(201); perform public.party_start_rev(p, 0);
      perform t.age(interval '6 seconds');
      perform t.age(interval '10800 seconds');
      select started_at into v_start from economy.crew_runs where user_id = t.u(201);
      perform t.rec(201, v_start, 10800, 29700);
      perform t.age(interval '40 minutes');
      select started_at into v_start from economy.crew_runs where user_id = t.u(201);
      insert into t.fix values ('race_start', v_start::text), ('race_ps', (now() - interval '38 minutes')::text)
      on conflict (k) do update set v = excluded.v;
    end $y$;
  $x$);
  select v::timestamptz into v_start from t.fix where k = 'race_start';
  select v::timestamptz into v_ps from t.fix where k = 'race_ps';
  v_max := economy.daily_max_of(t.u(201), 4000, 1);
  perform t.say('준비: X 크루 러닝 뒤 오늘 받은 SUP', round(economy.day_earned(t.u(201), economy.game_day(v_start)), 4)::text || ' / 하루 최대 ' || v_max);
  -- A: X 가 혼자 러닝을 올린다 — 커밋하지 않고 둔다
  perform dblink_connect('a', v_conn);
  perform dblink_exec('a', 'begin');
  select x into r from dblink('a', format('select t.rec(201, %L::timestamptz, 2160, 6000)', v_ps)) as q(x text);
  perform t.say('A (X 혼자, 커밋 전)', r);
  -- B: 그동안 Y 가 크루 러닝을 올린다(자동 커밋)
  perform dblink_connect('b', v_conn);
  perform dblink_send_query('b', format('select t.rec(202, %L::timestamptz, 600, 1650)', v_start));
  for i in 1 .. 50 loop
    v_busy := dblink_is_busy('b') = 1;
    exit when not v_busy;
    perform pg_sleep(0.1);
  end loop;
  perform t.say('B 가 A 를 기다렸나', v_busy::text);
  perform dblink_exec('a', 'commit');
  select x into r from dblink_get_result('b') as q(x text);
  perform t.say('B (Y 크루)', r);
  select v into r from dblink('s', format('select round(economy.day_earned(%L::uuid, economy.game_day(%L::timestamptz)), 4)::text', t.u(201), v_start)) as q(v text);
  perform t.say('X 오늘 받은 SUP(러닝 + 더한 몫 + 목표)', r || ' / 하루 최대 ' || v_max);
  select v into r from dblink('s', format($q$select string_agg(format('%%s %%s SUP 더한 몫 %%s', energy_pool, points_awarded, together_paid), ' · ' order by started_at)
                                             from public.walk_sessions where user_id = %L::uuid$q$, t.u(201))) as q(v text);
  perform t.say('X 러닝', r);
  -- 치우기(감사용 DB)
  perform dblink_exec('s', format('delete from public.crews where id = %L::uuid; delete from auth.users where id in (%L::uuid, %L::uuid)', t.c(20), t.u(201), t.u(202)));
  perform dblink_disconnect('a'); perform dblink_disconnect('b'); perform dblink_disconnect('s');
end $$;
