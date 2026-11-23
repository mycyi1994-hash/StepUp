-- 2026-11-22 보안 점검(I): 앱 함수를 거치지 않는 표 바로 쓰기(PostgREST PATCH · DELETE)가 함수의 막음을 비켜 가는지
-- (가) profiles.daily_goal 을 바로 바꿔 지난 날 목표를 낮춘다 — profile_set_daily_goal 의 "지난 7일 굳히기"를 비켜 간다
-- (나) 크루장이 crews 를 바로 지운다 — crew_dissolve 의 crew_running 거절(0117)을 비켜 간다
-- (다) 번개 글쓴이가 posts 를 바로 지운다 — post_delete 의 flash_started 거절(0109)을 비켜 간다
select t.mk(1), t.mk(2), t.mk(3), t.mk(4), t.mk(5);
update public.profiles set daily_goal = 20000 where id in (t.u(1), t.u(2));
select t.crew(1, 3, array[4, 5]);
do $$
declare v_yst timestamptz := economy.game_day_start(economy.game_day(now()) - 1) + interval '12 hours';
  p bigint; post bigint; n int;
begin
  -- (가) 1 은 표를 바로 고치고, 2 는 앱 함수로 — 둘 다 어제 연결 없이 8,000보
  perform t.login(1);
  perform set_config('role', 'authenticated', true);
  update public.profiles set daily_goal = 4000 where id = t.u(1);
  perform set_config('role', 'postgres', true);
  perform t.rec(1, v_yst, 3000, 8000);
  perform t.login(2); perform public.profile_set_daily_goal(4000);
  perform t.rec(2, v_yst, 3000, 8000);
  perform t.say('(가) 어제 목표 보너스 — 표를 바로 고친 1', (select coalesce(sum(amount), 0) from public.sup_ledger where user_id = t.u(1) and kind = 'BONUS_GOAL'));
  perform t.say('(가) 어제 목표 보너스 — 앱 함수로 바꾼 2', (select coalesce(sum(amount), 0) from public.sup_ledger where user_id = t.u(2) and kind = 'BONUS_GOAL'));

  -- (나) 크루 러닝이 달리는 중 크루장 3 이 crews 를 바로 지운다
  perform t.login(3);
  p := public.party_create_v2(t.c(1), '해산', null, null, null, gen_random_uuid(), 'TOGETHER', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(4); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(5); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(3); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[3, 4, 5], 300);
  perform t.login(3);
  begin perform public.crew_dissolve(t.c(1)); perform t.say('(나) crew_dissolve', '됨'::text);
  exception when others then perform t.say('(나) crew_dissolve 거절', sqlerrm); end;
  perform set_config('role', 'authenticated', true);
  delete from public.crews where id = t.c(1);
  get diagnostics n = row_count;
  perform set_config('role', 'postgres', true);
  perform t.say('(나) DELETE /crews 로 지운 줄', n);
  perform t.say('(나) 남은 방 · 크루 러닝(달리는 중, 크루 없음)', (select format('parties=%s crew_runs RUNNING without crew=%s', (select count(*) from public.parties where id = p),
     count(*)) from economy.crew_runs where party_id = p and state = 'RUNNING' and crew_id is null));

  -- (다) 출발한 번개러닝 글을 글쓴이 1 이 바로 지운다
  perform t.login(1);
  post := public.post_create('FLASH', null, '번개', '', '', 5, now() + interval '30 minutes', 4, null, null, gen_random_uuid(), 'REMOTE');
  perform t.login(2); perform public.join_flash(post);
  perform t.login(1); p := public.party_open(null, post); perform public.party_ready(p, true);
  perform t.login(2); perform public.party_open(null, post); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start(p);
  perform t.age(interval '4 seconds');
  begin perform public.post_delete(post); perform t.say('(다) post_delete', '됨'::text);
  exception when others then perform t.say('(다) post_delete 거절', sqlerrm); end;
  perform set_config('role', 'authenticated', true);
  delete from public.posts where id = post;
  get diagnostics n = row_count;
  perform set_config('role', 'postgres', true);
  perform t.say('(다) DELETE /posts 로 지운 줄 · 남은 방', format('%s · %s', n, (select count(*) from public.parties where id = p)));
end $$;
