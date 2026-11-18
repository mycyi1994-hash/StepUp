-- 장면 6: 대기실 — 카운트다운 중 나가기 · 유령 자리 · 방이 저절로 닫힘 · 진행자 넘어감 · 자정
select t.mk(1), t.mk(2), t.mk(3), t.mk(4), t.mk(5), t.mk(6);
select t.crew(1, 1, array[2,3,4,5,6]);
do $$
declare p bigint; p2 bigint; v jsonb; j json; v_start timestamptz; ok boolean;
begin
  -- (가) 출발 누른 뒤 6초 사이에 2 가 나가기를 누른다
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '카운트다운', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(3); perform public.party_join(p);   -- 3 은 준비 안 함
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.login(3);
  begin j := public.party_state(p); exception when others then perform t.say('(가) 준비 안 한 3 이 출발 뒤 대기실을 물으면', sqlerrm); end;
  perform t.age(interval '2 seconds');
  perform t.login(2); perform public.party_leave(p);
  perform t.say('(가) 카운트다운 중 나간 2 의 오늘 자리', t.cr(2));
  begin p2 := public.party_create_v2(t.c(1), '다시', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
    perform t.say('(가) 2 새 방', '열림'::text);
  exception when others then perform t.say('(가) 2 가 새 방을 열려 하면', sqlerrm); end;
  ok := public.crew_run_release(p);
  perform t.say('(가) 2 자리 돌려받기(crew_run_release)', ok);
  perform t.say('(가) 풀', t.pool(1));
  perform t.say('(가) 1(혼자 남음)', t.cr(1));
  -- 1 은 혼자 15분 달린다
  perform t.age(interval '4 seconds');
  perform t.run(p, array[1], 900);
  perform t.login(1); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(가) 1 박동 답', format('state=%s earning=%s participants=%s bonus=%s', v->>'state', v->>'earning_state', v->>'participants', v->'bonus'));
  -- (나) 달리는 15분 동안 방(parties) 상태
  perform t.say('(나) 15분 달린 뒤 방 상태', (select format('status=%s members=%s last_seen 나이=%s', status,
     (select count(*) from public.party_members m where m.party_id = p),
     (select max(now() - last_seen) from public.party_members m where m.party_id = p)) from public.parties where id = p));
  perform t.login(4); v := public.party_rooms(t.c(1));
  perform t.say('(나) 다른 크루원이 방 목록을 본 뒤 방 상태', (select status from public.parties where id = p));
  perform t.login(1);
  begin perform public.party_live(p, 2.5); perform public.party_ping(p, 37.5, 127.0);
    perform t.say('(나) 닫힌 뒤 1 의 거리 · 위치', (select format('km=%s lat=%s', km, lat) from public.party_members where party_id = p and user_id = t.u(1)));
  exception when others then perform t.say('(나) 닫힌 뒤 거리 보내기', sqlerrm); end;
  v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('(나) 닫힌 뒤 박동', v->>'earning_state');
end $$;
