-- 점검 2차 F(보상 이벤트 · 자정): 이벤트 배수 · 하루 에너지 · 하루 최대는 앱이 보낸 시작 시각(p_started_at)으로 정해진다.
-- 경로의 첫 점은 시작 시각보다 늦어도 된다(이른 쪽만 120초 여유로 본다) — 이벤트 끝 무렵에 "출발"하고 일시정지했다가 몇 시간 뒤 달려도 이벤트 배수.
-- 자정 전에 출발해 다음 날 달리면 어제 에너지 · 어제 하루 최대로 센다(오늘 에너지는 그대로 남는다).
-- run.sh 는 숫자로 시작하는 장면만 돈다 — 이 장면은 손으로: begin; … rollback; 으로 감싸 psql -f.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n, 1500) from generate_series(1, 4) n;
insert into economy.runner_levels (user_id, legacy_energy) select t.u(n), 10 from generate_series(1, 4) n
on conflict (user_id) do update set legacy_energy = 10;
-- 경로: 시작 p_start 부터 p_secs 초 — t.track 그대로(사람마다 경도가 조금씩 다르다)
create or replace function t.rec2(n int, p_claim timestamptz, p_track_start timestamptz, p_secs int, p_steps int)
returns text language plpgsql as $$
declare r record; s public.walk_sessions;
begin
  perform t.login(n);
  select * into r from public.record_session(p_claim, p_track_start + make_interval(secs => p_secs), p_steps, p_secs,
    t.track(p_track_start, p_secs), 0, 1, '', false, null);
  select * into s from public.walk_sessions where id = r.session_id;
  return format('u%s %s pts=%s mult=%s energy=%s day=%s day_max=%s reason=[%s]', n, r.verdict, r.points_awarded, s.reward_mult,
    s.energy_used, economy.game_day(s.started_at), s.day_max, r.verdict_reason);
end $$;
do $$
declare ev_from timestamptz := now() - interval '4 hours'; ev_to timestamptz := now() - interval '3 hours';
        midnight timestamptz := economy.game_day_start(economy.game_day(now())); r text;
begin
  -- 끝난 보상 이벤트 × 3 (4시간 전 ~ 3시간 전)
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), 'x3 한 시간', 3, ev_from, ev_to, 'sim');
  -- (가) 기준: 이벤트 밖에서 36분
  perform t.say('(가) u1 이벤트 밖 36분 6,000보', t.rec2(1, now() - interval '40 minutes', now() - interval '40 minutes', 2160, 6000));
  -- (나) 이벤트가 끝나기 1분 전 "출발" → 일시정지 → 2시간 20분 뒤 36분 달림(경로는 그때부터)
  perform t.say('(나) u2 이벤트 끝 1분 전 출발 · 2시간 20분 뒤 달림', t.rec2(2, ev_to - interval '1 minute', now() - interval '40 minutes', 2160, 6000));
  -- (다) 자정 — 어제 23:59 출발 · 오늘 낮(40분 전) 36분 달림 → 어제 에너지 · 어제 날. 그 뒤 오늘 에너지로 한 번 더
  perform t.say('(다) u3 어제 23:59 출발 · 오늘 달림', t.rec2(3, midnight - interval '1 minute', now() - interval '100 minutes', 2160, 6000));
  perform t.say('(다) u3 이어서 오늘 36분', t.rec2(3, now() - interval '40 minutes', now() - interval '40 minutes', 2160, 6000));
  perform t.say('(다) u3 오늘(달력) 받은 합 · 기준 u1', format('%s · %s',
     (select sum(points_awarded) from public.walk_sessions where user_id = t.u(3)), (select sum(points_awarded) from public.walk_sessions where user_id = t.u(1))));
  -- (라) 어제 23:00 ~ 24:00 이벤트 × 3 · 23:59 출발 · 오늘 달림 — 배수 · 어제 에너지 둘 다
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), 'x3 어제 밤', 3, midnight - interval '1 hour', midnight, 'sim');
  perform t.say('(라) u4 어제 밤 이벤트 끝 1분 전 출발 · 오늘 달림', t.rec2(4, midnight - interval '1 minute', now() - interval '100 minutes', 2160, 6000));
  perform t.say('(라) u4 이어서 오늘 36분', t.rec2(4, now() - interval '40 minutes', now() - interval '40 minutes', 2160, 6000));
  perform t.say('(라) u4 오늘(달력) 받은 합', (select sum(points_awarded) from public.walk_sessions where user_id = t.u(4)));
end $$;

-- (마) 겹친 이벤트(SQL 창 — 운영자 페이지는 겹침을 거절) → 가장 큰 배수 하나 · 취소된 이벤트는 취소 시각까지
do $$
declare a timestamptz := now() - interval '30 minutes';
begin
  delete from economy.reward_events;
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor) values
    (gen_random_uuid(), 'x2', 2, a - interval '1 hour', a + interval '1 hour', 'sim'),
    (gen_random_uuid(), 'x1.5', 1.5, a - interval '1 hour', a + interval '1 hour', 'sim');
  perform t.say('(마) 겹친 ×2 · ×1.5', (economy.reward_event_at(a)).mult);
  update economy.reward_events set cancelled_at = a - interval '1 minute' where title = 'x2';
  perform t.say('(마) ×2 를 그 전에 취소', (economy.reward_event_at(a)).mult);
end $$;

-- (바) 수정한 앱: 이틀 전에 끝난 이벤트 × 2 안을 시작 시각으로 보내고 오늘 달린 경로를 올린다 — 그 사이 러닝이 없으면 받아 준다(이틀 전 에너지 · 날)
-- (사) 운영자가 이벤트를 켠 지 5분 만에 끈다 — 그 5분 안에 "출발"하고 일시정지한 러닝은 끈 뒤 언제 달려도 × 5
do $$
declare ev2 timestamptz := now() - interval '2 days'; c economy.reward_events; st timestamptz;
begin
  delete from economy.reward_events;
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), 'x2 이틀 전', 2, ev2 - interval '1 hour', ev2, 'sim');
  perform t.mk(5, 1500); insert into economy.runner_levels (user_id, legacy_energy) values (t.u(5), 10) on conflict (user_id) do update set legacy_energy = 10;
  perform t.say('(바) u5 이틀 전 이벤트 안을 시작으로 · 오늘 경로', t.rec2(5, ev2 - interval '10 minutes', now() - interval '40 minutes', 2160, 6000));
  perform t.say('(바) u5 이어서 오늘 36분', t.rec2(5, now() - interval '40 minutes' + interval '2161 seconds', now() - interval '40 minutes' + interval '2161 seconds', 300, 800));
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), 'x5 실수', 5, now() - interval '3 hours', now() + interval '165 hours', 'sim') returning * into c;
  st := now() - interval '2 hours 57 minutes';
  update economy.reward_events set cancelled_at = now() - interval '2 hours 55 minutes' where id = c.id;
  perform t.mk(6, 1500); insert into economy.runner_levels (user_id, legacy_energy) values (t.u(6), 10) on conflict (user_id) do update set legacy_energy = 10;
  perform t.say('(사) u6 켠 지 3분에 출발 · 끈 뒤 2시간 15분 지나 달림', t.rec2(6, st, now() - interval '40 minutes', 2160, 6000));
end $$;
