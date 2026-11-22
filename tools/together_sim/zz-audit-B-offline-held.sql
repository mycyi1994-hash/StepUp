-- 감사 B: 묶어 둔 공용 체력(0120 R3)을 "소식 없이 달린 시간" 정산(0117)이 그대로 쓴다.
-- 16 (가)와 같은 판(20명 · 크루 Lv20 · 보상 이벤트 × 2 · 첫 신발) — 18명은 6초 만에 끝냄. 다른 점: 남은 둘은 출발 뒤 박동을 보내지 않는다(비행기 모드).
-- 16 (가)에서 둘은 박동을 보내 433 SUP 에서 막혔다. 여기서는 올릴 때 record_session 이 capacity − consumed 에서 가져간다(held 를 빼지 않는다).
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n) from generate_series(11, 30) n;
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(13, 30) n);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; r text; w public.walk_sessions; c economy.crew_runs; v_max numeric; pool0 jsonb;
begin
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '오프라인', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  for n in 13 .. 30 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.login(11); v := public.crew_run_beat(p, 'RUNNING');
  perform t.say('출발 6초 뒤 공용 체력(0120 화면 값)', format('total %s · held %s · remaining %s', v -> 'pool' ->> 'total', round((v -> 'pool' ->> 'held')::numeric, 1), round((v -> 'pool' ->> 'remaining')::numeric, 1)));
  -- 둘은 3시간 동안 박동 없이(오프라인) 달린다
  perform t.age(interval '10800 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  r := t.rec(11, v_start, 10800, 29700);
  perform t.say('u11 올림', r);
  r := t.rec(12, v_start, 10800, 29700);
  perform t.say('u12 올림', r);
  select * into c from economy.crew_runs where user_id = t.u(11);
  select * into w from public.walk_sessions where id = c.session_id;
  v_max := economy.daily_max_of(t.u(11), 4000, 2);
  perform t.say('u11 크루 러닝', format('%s SUP (rewarded %s / 29700 걸음 · 쓴 체력 %s · 서버가 센 초 %s · 인원 %s명) · 하루 최대 %s', w.points_awarded, w.rewarded_steps,
                round(c.consumed, 1), round(c.active_sec), c.bonus_count, v_max));
  perform t.say('공용 체력 끝', t.pool(2) || ' · held ' || round(economy.crew_pool_held(t.c(2), c.day, now()), 1));
  perform t.say('16 (가) 박동을 보낸 같은 판', '433.3 SUP (묶은 체력 5,769 은 쓰지 못함)'::text);
end $$;
-- (나) 같은 길 + 돌려받기: 부계정 넷은 박동을 한 번도 보내지 않는다(끝냄도 없이). 둘이 2시간 오프라인으로 달려 올린 뒤
-- 부계정이 crew_run_release 를 부른다 — 출발 전(카운트다운)인지 보지 않으므로 몇 시간 뒤에도 돌려받고, 같은 날 다른 방에 또 맡긴다
do $$
declare p bigint; p2 bigint; v jsonb; v_start timestamptz; n int; r text; ok boolean; cap0 numeric; cap1 numeric; cap2 numeric;
begin
  perform t.mk(41); perform t.mk(42); perform t.mk(47);
  for n in 43 .. 46 loop perform t.mk(n, 1000, 4000); end loop;
  perform t.crew(3, 41, array[42, 43, 44, 45, 46, 47], 1);
  perform t.login(41);
  p := public.party_create_v2(t.c(3), '방 1', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 42 .. 46 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(41); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.age(interval '7200 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(41);
  perform t.say('(나) u41 올림', t.rec(41, v_start, 7200, 19800));
  perform t.say('(나) u42 올림', t.rec(42, v_start, 7200, 19800));
  select capacity into cap0 from economy.crew_day_pools where crew_id = t.c(3);
  r := '';
  for n in 43 .. 46 loop perform t.login(n); ok := public.crew_run_release(p); r := r || 'u' || n || '=' || ok || ' '; end loop;
  select capacity into cap1 from economy.crew_day_pools where crew_id = t.c(3);
  perform t.say('(나) 2시간 뒤 돌려받기', r || format('· 공용 체력 %s → %s (쓴 것 %s)', cap0, cap1, (select round(consumed, 1) from economy.crew_day_pools where crew_id = t.c(3))));
  -- 같은 날 같은 크루의 새 방에 다시 맡긴다(u47 진행)
  perform t.login(47);
  p2 := public.party_create_v2(t.c(3), '방 2', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p2, true);
  for n in 43 .. 46 loop perform t.login(n); perform public.party_join(p2); perform public.party_ready(p2, true); end loop;
  perform t.login(47); perform public.party_start_rev(p2, 0);
  select capacity into cap2 from economy.crew_day_pools where crew_id = t.c(3);
  perform t.say('(나) 같은 날 두 번째 방에 다시 맡김', format('공용 체력 %s → %s · 오늘 크루 러닝 줄 u43 %s개 · 방 2 맡긴 것 %s', cap1, cap2,
                (select count(*) from economy.crew_runs c where c.user_id = t.u(43)),
                (select string_agg(round(c.contribution) || '', '+') from economy.crew_runs c where c.party_id = p2)));
end $$;
