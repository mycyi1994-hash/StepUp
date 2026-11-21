-- 확인(2026-11-21 보너스 전수 점검): 크루 러닝의 인원 보너스 · 공용 체력 · 상한이 겹치면 얼마나 되나 — 실제 서버 규칙으로.
--   (가) 둘이 1시간(크루 Lv1) — 기준
--   (나) 20명이 출발하고 18명은 바로 끝냄(새 계정) · 둘이 3시간(크루 Lv20 · 보상 이벤트 × 2) — 인원 · 체력 · 배수가 다 겹친 경우
--   (다) 같은 날 (나)의 1 이 혼자 러닝 — 크루 보상이 하루 상한에 드는가
--   (라) 인원 보너스 표 · 레벨 보너스
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
select t.mk(n) from generate_series(1, 2) n;
select t.mk(n) from generate_series(11, 30) n;
-- 11 ~ 30 중 13 ~ 30 은 오늘 만든 계정(첫 신발 그대로)
update public.profiles set created_at = now() where id in (select t.u(n) from generate_series(13, 30) n);
select t.crew(1, 1, array[2], 1);
select t.crew(2, 11, (select array_agg(n) from generate_series(12, 30) n), 20);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; r text;
begin
  -- (가) 둘이 1시간 · 크루 Lv1
  perform t.login(1);
  p := public.party_create_v2(t.c(1), '기준', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[1, 2], 3600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(1);
  perform t.login(1); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(가) 둘 · Lv1 · 1시간 9,900보', t.rec(1, v_start, 3600, 9900));
  perform t.say('(가) 1', t.cr(1));
  perform t.say('(가) 혼자 1시간이면(에너지 10 · 하루 상한 전)', round(least(9900, 10 * 600) * 0.0118 * 1.4, 2));
end $$;
do $$
declare p bigint; v jsonb; v_start timestamptz; n int;
begin
  -- (나) 20명 · 크루 Lv20 · 보상 이벤트 × 2 · 18명은 출발 직후 끝냄 · 11 · 12 가 3시간
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 2배', 2, now() - interval '6 hours', now() + interval '6 hours', 'sim');
  perform t.login(11);
  p := public.party_create_v2(t.c(2), '겹침', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 30 loop
    perform t.login(n);
    begin perform public.party_join(p); perform public.party_ready(p, true);
    exception when others then perform t.say('(나) ' || n || ' 들어가기 거절', sqlerrm); end;
  end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.say('(나) 출발한 사람', (select count(*) from economy.crew_runs where party_id = p));
  perform t.age(interval '6 seconds');
  for n in 13 .. 30 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.say('(나) 18명이 바로 끝낸 뒤 풀', t.pool(2));
  perform t.run(p, array[11, 12], 10800);
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  perform t.login(11); v := public.crew_run_beat(p, 'ENDED');
  perform t.say('(나) 3시간 뒤 풀', t.pool(2));
  perform t.say('(나) 11 · 20명 · Lv20 · × 2 · 3시간 29,700보', t.rec(11, v_start, 10800, 29700));
  perform t.say('(나) 11', t.cr(11));
  perform t.say('(나) 바로 끝낸 13(새 계정)', t.cr(13));
  perform t.say('(나) 11 의 혼자 하루 상한', round(economy.setting_num('daily_earn_cap') * economy.earn_cap_factor(t.u(11)), 2));
  -- (다) 같은 날 혼자 1시간
  perform t.say('(다) 같은 날 11 혼자 1시간(크루 러닝 전에 달린 것)', t.rec(11, v_start - interval '90 minutes', 3600, 9900));
  perform t.say('(다) 11 그날 받은 합', (select round(sum(points_awarded), 2) from public.walk_sessions where user_id = t.u(11)));
end $$;
do $$
declare n int; s text := '';
begin
  for n in 1 .. 21 loop s := s || n || '명 +' || round(100 * economy.crew_headcount_bonus(n)) || '% · '; end loop;
  perform t.say('(라) 인원 보너스', s);
  perform t.say('(라) 크루 레벨 보상 · 체력', format('Lv1 +%s%% / +%s%% · Lv10 +%s%% / +%s%% · Lv20 +%s%% / +%s%%',
    100 * economy.crew_level_reward(1), 100 * economy.crew_level_stamina(1), 100 * economy.crew_level_reward(10), 100 * economy.crew_level_stamina(10),
    100 * economy.crew_level_reward(20), 100 * economy.crew_level_stamina(20)));
  perform t.say('(라) 번개 인원 배수', format('2명 ×%s · 6명 ×%s · 10명 ×%s', economy.party_multiplier(2), economy.party_multiplier(6), economy.party_multiplier(10)));
end $$;
