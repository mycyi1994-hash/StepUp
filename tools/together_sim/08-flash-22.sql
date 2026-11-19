-- 장면 8: 번개러닝 정원 30, 22명이 함께 출발
select count(t.mk(i)) from generate_series(1, 22) i;
do $$
declare post bigint; p bigint; p2 bigint; i int; v_start timestamptz; j json;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '큰 번개', '', '여의도', 5, now() + interval '10 minutes', 30, 37.52, 126.93, gen_random_uuid(), 'TOGETHER');
  for i in 2..22 loop perform t.login(i); perform public.join_flash(post); end loop;
  perform t.login(1); p := public.party_open(null, post); perform public.party_ready(p, true);
  for i in 2..22 loop
    perform t.login(i); p2 := public.party_open(null, post);
    if p2 <> p then perform t.say('정원이 찬 뒤 들어온 사람 u' || i || ' → 새 대기실', p2); end if;
    perform public.party_ready(p2, true);
  end loop;
  perform t.login(1); j := public.party_state(p);
  perform t.say('번개 대기실', format('capacity=%s members=%s', j->>'capacity', json_array_length(j->'members')));
  perform public.party_start(p);
  perform t.age(interval '20 minutes 4 seconds');
  select starts_at into v_start from public.party_runs where party_id = p and user_id = t.u(1);
  perform t.say('22명 번개 — 1 정산', t.rec(1, v_start, 1200, 3200));
end $$;
