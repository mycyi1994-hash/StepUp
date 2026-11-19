-- 장면 7: 경로 없는(걸음만) 크루 러닝 · 한 명이 바로 끝냄 · 신발이 사라짐
select t.mk(1), t.mk(2), t.mk(3), t.mk(4), t.mk(5), t.mk(6);
select t.crew(1, 1, array[2]), t.crew(2, 3, array[4]), t.crew(3, 5, array[6]);
do $$
declare p bigint; v jsonb; v_start timestamptz; v_shoe bigint;
begin
  -- (가) 박동은 정상, GPS 경로 없이 걸음만(분당 235) 30분
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '걸음만', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[1,2], 1800);
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.login(1); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(가) 1 경로 없이 30분 7,050보(크루)', t.rec(1, v_start, 1800, 7050, false));
  perform t.say('(가) 풀', t.pool(1));

  -- (나) 둘이 출발, 4 가 20초 만에 끝냄 → 3 은?
  perform t.login(3);
  p := public.party_create_v2(t.c(2), '바로 끝', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(4); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(3); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[3,4], 20);
  perform t.login(4); v := public.crew_run_beat(p, 'ENDED');
  perform t.login(3); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(나) 상대가 20초에 끝낸 뒤 3 의 박동 답', format('state=%s reason=%s earning=%s', v->>'state', v->>'terminal_reason', v->>'earning_state'));
  perform t.age(interval '30 minutes');
  select started_at into v_start from economy.crew_runs where user_id = t.u(3);
  perform t.say('(나) 3 이 모르고 30분 달려 올림(4,800보)', t.rec(3, v_start, 1820, 4850));
  perform t.say('(나) 3 신발(크루 러닝이라 안 닳음)', (select format('dur %s/%s', dur_now, dur_max) from public.market_sneakers where owner_id = t.u(3) and equipped));
  perform t.login(3);
  begin perform public.party_create_v2(t.c(2), '다시', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  exception when others then perform t.say('(나) 3 이 오늘 다시 크루 러닝을 하려면', sqlerrm); end;

  -- (다) 고른 신발이 달리는 중 사라짐(강화 재료로 태움 흉내)
  perform t.login(5);
  p := public.party_create_v2(t.c(3), '신발', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(6); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(5); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[5,6], 600);
  select started_at, sneaker_id into v_start, v_shoe from economy.crew_runs where user_id = t.u(5);
  update public.market_sneakers set status = 'BURNED', equipped = false where id = v_shoe;
  perform t.login(5); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(다) 고른 신발이 사라진 5 정산', t.rec(5, v_start, 600, 1600));
end $$;
