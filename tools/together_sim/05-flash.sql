-- 장면 5: 번개러닝 — 원격 · 보너스 인원 · 같은 글로 여러 번 · 지난 글 · 글 삭제 · 정원
select t.mk(1), t.mk(2), t.mk(3), t.mk(4), t.mk(5), t.mk(6), t.mk(7), t.mk(8);
do $$
declare post bigint; p bigint; p2 bigint; v json; v_start timestamptz; i int;
begin
  perform t.login(1);
  post := public.post_create('FLASH', null, '한강 번개', '', '', 5, now() + interval '30 minutes', 4, null, null, gen_random_uuid(), 'REMOTE');
  for i in 2..4 loop perform t.login(i); perform public.join_flash(post); end loop;
  perform t.login(5);
  begin perform public.join_flash(post); perform t.say('(가) 정원 4 에 5번째', '들어감'::text);
  exception when others then perform t.say('(가) 정원 4 에 5번째 거절', sqlerrm); end;
  -- 모임 30분 전인데 대기실을 열고 바로 출발
  perform t.login(1); p := public.party_open(null, post); perform public.party_ready(p, true);
  for i in 2..4 loop perform t.login(i); perform public.party_open(null, post); perform public.party_ready(p, true); end loop;
  perform t.login(1); v := public.party_state(p);
  perform t.say('(가) 번개 대기실', format('mode=%s capacity=%s plan=%s members=%s', v->>'run_mode', v->>'capacity', v->>'plan_at', json_array_length(v->'members')));
  perform public.party_start(p);
  perform t.say('(가) 모임 30분 전에 출발', (select status from public.parties where id = p));
  perform t.age(interval '4 seconds');
  perform t.age(interval '20 minutes');
  select starts_at into v_start from public.party_runs where party_id = p and user_id = t.u(1);
  perform t.say('(가) 1 정산(넷이 출발 · 혼자만 달림)', t.rec(1, v_start, 1200, 3200));
  perform t.say('(가) 비교: 혼자 러닝이면', round(3200 * 0.0118 * 1.4, 4));
  perform t.login(1); perform public.party_finish(p);
  -- (나) 같은 글로 바로 또 방을 연다
  perform t.login(1); p2 := public.party_open(null, post); perform public.party_ready(p2, true);
  for i in 2..4 loop perform t.login(i); perform public.party_open(null, post); perform public.party_ready(p2, true); end loop;
  perform t.login(1); perform public.party_start(p2);
  perform t.age(interval '4 seconds');
  perform t.age(interval '23 minutes');
  select starts_at into v_start from public.party_runs where party_id = p2 and user_id = t.u(1);
  perform t.say('(나) 같은 글 두 번째 방 ' || p2 || ' (첫 방 ' || p || ')', t.rec(1, v_start, 1200, 3200));
  -- (다) 모임 시각이 이틀 지난 글로 방 열기
  update public.posts set meet_at = now() - interval '2 days' where id = post;
  perform t.login(1); perform public.party_finish(p2);
  begin p2 := public.party_open(null, post); perform t.say('(다) 이틀 지난 번개 글로 대기실', '열림 ' || p2);
  exception when others then perform t.say('(다) 거절', sqlerrm); end;
  -- (라) 달리는 중 글 삭제 — 모이는 시각을 다시 앞으로
  update public.posts set meet_at = now() + interval '10 minutes' where id = post;
  perform t.login(1); p2 := public.party_open(null, post);
  perform public.party_ready(p2, true);
  perform t.login(2); perform public.party_open(null, post); perform public.party_ready(p2, true);
  perform t.login(1); perform public.party_start(p2);
  perform t.age(interval '4 seconds');
  perform t.login(1);
  begin perform public.post_delete(post); perform t.say('(라) 달리는 중 글 삭제', '됨'::text);
  exception when others then perform t.say('(라) 달리는 중 글 삭제 거절', sqlerrm); end;
  perform t.age(interval '10 minutes');
  select starts_at into v_start from public.party_runs where party_id = p2 and user_id = t.u(2);
  if v_start is null then v_start := now() - interval '10 minutes'; end if;
  perform t.say('(라) 글 삭제 뒤 2 정산', t.rec(2, v_start, 600, 1600));
  perform t.login(2);
  begin perform public.party_state(p2); exception when others then perform t.say('(라) 글 삭제 뒤 대기실 상태', sqlerrm); end;
end $$;
