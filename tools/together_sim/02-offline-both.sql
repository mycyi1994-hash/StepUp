-- 장면 2: 둘 다 출발 뒤 인터넷을 끄고 3시간 달린 뒤 올린다 / 비교: 혼자 러닝 3시간
select t.mk(1), t.mk(2), t.mk(3, 150, 4000), t.mk(5, 600, 60000), t.mk(6, 600, 60000);
select t.crew(1, 1, array[2,3]), t.crew(2, 5, array[6]);
do $$
declare p bigint; v jsonb; v_start timestamptz;
begin
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '오프라인', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '3 hours 6 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.say('1 정산(일반 신발 · 박동 없이 3시간 · 28,000보)', t.rec(1, v_start, 10800, 28000));
  perform t.say('2 정산(같음)', t.rec(2, v_start, 10800, 28000));
  perform t.say('1', t.cr(1)); perform t.say('2', t.cr(2));
  perform t.say('풀', t.pool(1));
  -- 비교: 같은 신발로 혼자 3시간
  perform t.say('3 혼자 3시간 28,000보', t.rec(3, v_start, 10800, 28000));
  perform t.say('3 신발', (select format('dur %s/%s', dur_now, dur_max) from public.market_sneakers where owner_id = t.u(3) and equipped));
  -- 레전더리급 신발(효율 +600% · 내구도 600) 둘
  perform t.login(5);
  p := public.party_create_v2(t.c(2), '오프라인2', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(6); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(5); perform public.party_start_rev(p, 0);
  perform t.age(interval '3 hours 6 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(5);
  perform t.say('5 정산(효율 +600% 신발 · 박동 없이 3시간)', t.rec(5, v_start, 10800, 28000));
  perform t.say('풀2', t.pool(2));
end $$;
