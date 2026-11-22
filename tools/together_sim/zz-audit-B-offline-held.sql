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
