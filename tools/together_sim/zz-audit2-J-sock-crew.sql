-- 점검 2 (J): 한 사람 + 부계정 다섯이 원격 크루 방 — 한 폰(스크립트)이 여섯 계정으로 박동을 보내고,
-- 부계정은 진짜 러닝 경로를 점마다 ±1.5m 흔들어 올린다. 모두 "함께 달린 사람"이 되어 인원 보너스 6명 +50%.
-- 견줄 것: 같은 사람이 혼자(개인 러닝만). 크루 러닝은 개인 에너지와 따로라 하루에 둘 다 받는다.
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(21, 26) n;
select t.old(n) from generate_series(21, 26) n;
select t.crew(21, 21, array[22, 23, 24, 25, 26], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; v_t text; w public.walk_sessions; c economy.crew_runs; v_tot numeric := 0;
begin
  perform t.login(21);
  p := public.party_create_v2(t.c(21), '부계정', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 22 .. 26 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(21); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[21, 22, 23, 24, 25, 26], 1800);
  for n in 21 .. 26 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  select started_at into v_start from economy.crew_runs where user_id = t.u(21);
  v_t := t.trk(v_start, 1800);
  w := t.up(21, v_start, 1800, 4950, v_t);
  perform t.say('u21 진짜(크루)', t.line(w));
  for n in 22 .. 26 loop
    w := t.up(n, v_start, 1800, 4950, t.jit(v_t, 1.5, n));
    perform t.say('u' || n || ' 부계정(흔든 사본)', t.line(w));
  end loop;
  for n in 21 .. 26 loop
    select * into c from economy.crew_runs where user_id = t.u(n);
    perform t.say('crew', format('u%s 함께 %s명 +%s · 기본 %s · 인원 %s · 합 %s', n, c.bonus_count, c.bonus_pct, c.base_sup, c.headcount_sup,
                  (select points_awarded from public.walk_sessions where id = c.session_id)));
    v_tot := v_tot + (select points_awarded from public.walk_sessions where id = c.session_id);
  end loop;
  perform t.say('여섯 계정 크루 SUP 합', v_tot);
  -- 같은 날 개인 러닝도(크루 체력과 따로) — 진짜 사람 하나의 60분 경로를 여섯이
  v_t := t.trk(v_start - interval '2 hours', 3600);
  for n in 21 .. 26 loop
    w := t.up(n, v_start - interval '2 hours', 3600, 9900, case when n = 21 then v_t else t.jit(v_t, 1.5, 100 + n) end);
    if n in (21, 22) then perform t.say('u' || n || ' 개인', t.line(w)); end if;
  end loop;
  for n in 21 .. 26 loop perform t.say('계정', t.me(n)); end loop;
  perform t.say('여섯 계정 잔고 합', (select round(sum(economy.balance_of(t.u(g.k))), 2) from generate_series(21, 26) g(k)));
end $$;
