-- 장면 1: 둘이 크루 러닝 — 정상 · 한 사람은 박동 없이(오프라인) 2시간
select t.mk(1), t.mk(2), t.mk(3), t.mk(4);
select t.crew(1, 1, array[2,3,4]);
do $$
declare p bigint; v jsonb; v_start timestamptz;
begin
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '아침 러닝', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.say('출발 뒤 풀', t.pool(1));
  perform t.age(interval '6 seconds');
  -- 1 은 20분 박동하며 달린다. 2 는 박동이 없다(오프라인)
  perform t.run(p, array[1], 1200);
  perform t.say('20분 뒤 풀', t.pool(1));
  perform t.say('1', t.cr(1));
  perform t.say('2', t.cr(2));
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.login(1); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('1 끝낸 뒤 2', t.cr(2));
  perform t.say('1 정산', t.rec(1, v_start, 1200, 3200));
  -- 2 는 2시간 달린 것을 나중에 올린다
  perform t.age(interval '100 minutes');
  select started_at into v_start from economy.crew_runs where user_id = t.u(2);
  perform t.say('2 정산(박동 없이 2시간 · 19,000보)', t.rec(2, v_start, 7200, 19000));
  perform t.say('2', t.cr(2));
  perform t.say('풀', t.pool(1));
end $$;
