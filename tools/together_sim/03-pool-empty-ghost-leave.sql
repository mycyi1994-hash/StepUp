-- 장면 3: 풀이 중간에 바닥 / 출발 못 한 사람(유령) 뒤 혼자 러닝 / 나가기
select t.mk(1, 110, 0), t.mk(2, 110, 0), t.mk(3), t.mk(4), t.mk(5), t.mk(6);
select t.crew(1, 1, array[2]), t.crew(2, 3, array[4,5,6]);
do $$
declare p bigint; v jsonb; v_start timestamptz; r record;
begin
  -- (가) 풀 220 = 둘이 22분. 30분 달린다
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '바닥', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[1,2], 1800);
  perform t.say('(가) 30분 뒤 풀', t.pool(1));
  perform t.say('(가) 1', t.cr(1));
  perform t.login(1); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(가) 박동 답', v - 'pool' - 'bonus' - 'level' || jsonb_build_object('pool', v->'pool'));
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.login(1); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(가) 1 정산 30분 4,800보(효율 0)', t.rec(1, v_start, 1800, 4800));
  perform t.say('(가) 기대: 22분/30분 × 4800 × 0.0118 × 1.05 =', round(4800 * 22/30.0 * 0.0118 * 1.05, 2));
  perform t.say('(가) 2 정산', t.rec(2, v_start, 1800, 4800));

  -- (나) 셋이 출발, 6 은 폰이 출발하지 못함(박동 없음 · 자리도 안 돌려줌). 4분 뒤 혼자 러닝을 시작해 20분
  perform t.login(3);
  p := public.party_create_v2(t.c(2), '유령', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(4); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(5); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(6); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(3); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  -- 5 는 출발 직후 대기실을 나간다(party_leave) — 러닝은 계속
  perform t.login(5); perform public.party_leave(p);
  perform t.say('(나) 5 가 나간 뒤 방', (select format('status=%s members=%s runs=%s crew_runs=%s', status,
     (select count(*) from public.party_members m where m.party_id = p), (select count(*) from public.party_runs m where m.party_id = p),
     (select count(*) from economy.crew_runs m where m.party_id = p)) from public.parties where id = p));
  perform t.run(p, array[3,4,5], 1440);
  select started_at into v_start from economy.crew_runs where user_id = t.u(3);
  perform t.say('(나) 6(유령)', t.cr(6));
  perform t.say('(나) 6 혼자 러닝(크루 출발 4분 뒤 시작 · 20분)', t.rec(6, v_start + interval '4 minutes', 1200, 3200));
  perform t.say('(나) 6', t.cr(6));
  perform t.say('(나) 6 신발', (select format('dur %s/%s', dur_now, dur_max) from public.market_sneakers where owner_id = t.u(6) and equipped));
  perform t.login(5); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(나) 나간 5 의 박동 답', v->>'state');
  begin perform public.party_state(p); perform t.say('(나) 5 party_state', 'ok'::text);
  exception when others then perform t.say('(나) 5 party_state 거절', sqlerrm); end;
  perform t.say('(나) 풀', t.pool(2));
end $$;
