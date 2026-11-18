-- 장면 4: 달리는 중 크루 해산 / 크루 나가기 / 내보내기 / 진행자 계정 삭제
select t.mk(1), t.mk(2), t.mk(3), t.mk(4), t.mk(5), t.mk(6), t.mk(7), t.mk(8);
select t.crew(1, 1, array[2,3]), t.crew(2, 4, array[5,6]), t.crew(3, 7, array[8]);
do $$
declare p bigint; v jsonb; v_start timestamptz;
begin
  -- (가) 달리는 중 크루장이 크루를 해산
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '해산', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[1,2], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(2);
  perform t.login(1);
  begin perform public.crew_dissolve(t.c(1)); perform t.say('(가) 달리는 중 해산', '됨'::text);
  exception when others then perform t.say('(가) 해산 거절', sqlerrm); end;
  perform t.login(2); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(가) 해산 뒤 2 박동 답', v);
  perform t.say('(가) 2 정산(10분 1,600보)', t.rec(2, v_start, 600, 1600));
  perform t.say('(가) 2 신발', (select format('dur %s/%s', dur_now, dur_max) from public.market_sneakers where owner_id = t.u(2) and equipped));
  perform t.say('(가) 2 오늘 크루 러닝 자리', t.cr(2));

  -- (나) 달리는 중 5 가 크루를 나감 · 6 은 크루장이 내보냄
  perform t.login(4);
  p := public.party_create_v2(t.c(2), '이탈', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(5); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(6); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(4); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[4,5,6], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(4);
  perform t.login(5); perform public.crew_leave(t.c(2));
  perform t.login(4); perform public.crew_member_remove(t.c(2), t.u(6));
  perform t.login(5); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(나) 나간 5 정산', t.rec(5, v_start, 600, 1600));
  perform t.say('(나) 5', t.cr(5));
  perform t.say('(나) 내보내진 6 정산', t.rec(6, v_start, 600, 1600));
  perform t.say('(나) 6', t.cr(6));
  perform t.say('(나) 남은 4 정산', t.rec(4, v_start, 600, 1600));
  perform t.say('(나) 4', t.cr(4));

  -- (다) 진행자 7 이 러닝 뒤 계정을 지움 — 8 은 아직 안 올림
  perform t.login(7);
  p := public.party_create_v2(t.c(3), '삭제', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(8); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(7); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[7,8], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(8);
  delete from auth.users where id = t.u(7);
  perform t.say('(다) 진행자 계정 삭제 뒤 방', (select count(*) from public.parties where id = p));
  perform t.say('(다) 8 정산(10분)', t.rec(8, v_start, 600, 1600));
  perform t.say('(다) 8 신발', (select format('dur %s/%s', dur_now, dur_max) from public.market_sneakers where owner_id = t.u(8) and equipped));
  perform t.say('(다) 8', t.cr(8));
end $$;
