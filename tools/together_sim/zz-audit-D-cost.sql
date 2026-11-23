-- 감사 D: 러너 트리거(0121)의 비용 — 줄마다 그 사람의 모든 러닝을 다시 더한다(runner_steps_of).
-- 러닝 3,000개(하루 3번 · 1,000일)인 사람: 올릴 때 한 번 · SQL 창에서 판정을 한꺼번에 바꿀 때(줄마다 다시 셈)
select t.mk(1);
do $$
declare t0 timestamptz; n int;
begin
  t0 := clock_timestamp();
  insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps)
  select t.u(1), now() - make_interval(hours => 8 * i + 1), now() - make_interval(hours => 8 * i + 1) + interval '30 minutes', 1800, 5000, 'CLEAN', 5000, 5000
    from generate_series(1, 3000) i;
  perform t.say('insert 3,000 rows one statement (trigger per row)', round(extract(epoch from clock_timestamp() - t0)::numeric, 2) || ' s');
  t0 := clock_timestamp();
  perform economy.runner_refresh(t.u(1));
  perform t.say('one runner_refresh with 3,000 sessions', round(extract(epoch from clock_timestamp() - t0)::numeric * 1000, 1) || ' ms');
  t0 := clock_timestamp();
  update public.walk_sessions set verdict = 'FLAGGED' where user_id = t.u(1) and started_at < now() - interval '500 days';
  get diagnostics n = row_count;
  perform t.say('bulk verdict update ' || n || ' rows', round(extract(epoch from clock_timestamp() - t0)::numeric, 2) || ' s');
  perform t.say('level / boxed_to', (select level || ' / ' || boxed_to from economy.runner_levels where user_id = t.u(1)));
  perform t.say('indexes', (select string_agg(indexdef, ' | ') from pg_indexes where tablename = 'walk_sessions' and indexdef like '%user_id%'));
end $$;
