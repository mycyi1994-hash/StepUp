-- 장면 9: 박동 없이 · 경로 없이(걸음만) 6시간 크루 러닝 — 위 두 구멍이 겹칠 때
select t.mk(1), t.mk(2);
select t.crew(1, 1, array[2]);
do $$
declare p bigint; v_start timestamptz;
begin
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '6시간', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 hours 6 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.say('박동 · 경로 없이 6시간 80,000보(일반 신발)', t.rec(1, v_start, 21600, 80000, false));
  perform t.say('같은 걸음 혼자 러닝 하루 최대(상한 960 · 경로 없는 러닝 96)', 96);
end $$;
