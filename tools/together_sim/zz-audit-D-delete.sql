-- 감사 D: 계정 삭제 — 러너 레벨 · 상자 · 목표 날 · 크루 러닝 · 함께 달린 원장이 있는 사람을 지워도 막히지 않고 남는 줄이 없는가
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n) from generate_series(41, 43) n;
select t.crew(4, 41, array[42, 43], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; c economy.crew_runs; ok text;
begin
  perform t.login(41);
  p := public.party_create_v2(t.c(4), '삭제', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 42 .. 43 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(41); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[41, 42, 43], 900);
  select started_at into v_start from economy.crew_runs where user_id = t.u(41);
  for n in 41 .. 43 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  perform t.rec(41, v_start, 900, 2475);
  perform t.rec(42, v_start, 900, 2475);
  -- 러너 레벨 · 상자가 있게
  update economy.runner_levels set boxed_to = 1 where user_id = t.u(41);
  insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps)
  values (t.u(41), now() - interval '3 days', now() - interval '3 days' + interval '1 hour', 3600, 30000, 'CLEAN', 30000, 30000);
  perform t.say('before delete u41', format('runner=%s boxes=%s goal_days=%s crew_runs=%s ledger=%s',
    (select level from economy.runner_levels where user_id = t.u(41)),
    (select count(*) from economy.box_awards where user_id = t.u(41)),
    (select count(*) from economy.goal_days where user_id = t.u(41)),
    (select count(*) from economy.crew_runs where user_id = t.u(41)),
    (select count(*) from public.sup_ledger where user_id = t.u(41))));
  begin
    perform t.login(41); perform public.account_delete(); ok := 'ok';
  exception when others then ok := 'ERROR ' || sqlerrm;
  end;
  perform t.say('account_delete u41', ok);
  perform t.say('left for u41', format('runner=%s boxes=%s goal_days=%s crew_runs=%s ledger=%s sessions=%s grants=%s',
    (select count(*) from economy.runner_levels where user_id = t.u(41)),
    (select count(*) from economy.box_awards where user_id = t.u(41)),
    (select count(*) from economy.goal_days where user_id = t.u(41)),
    (select count(*) from economy.crew_runs where user_id = t.u(41)),
    (select count(*) from public.sup_ledger where user_id = t.u(41)),
    (select count(*) from public.walk_sessions where user_id = t.u(41)),
    (select count(*) from public.draw_grants where user_id = t.u(41))));
  -- 셋째가 지운 뒤에 올린다 — 정산이 막히지 않는가
  begin
    perform t.say('u43 upload after delete', t.rec(43, v_start, 900, 2475));
  exception when others then perform t.say('u43 upload after delete', 'ERROR ' || sqlerrm);
  end;
  select * into c from economy.crew_runs where user_id = t.u(42);
  perform t.say('u42 after', format('%s명 head=%s total=%s', c.bonus_count, c.headcount_sup, c.total_sup));
  -- 진행자를 넘겨받은 사람이 없는 방 · 크루장 넘김
  perform t.say('crew owner now', (select owner_id = t.u(42) from public.crews where id = t.c(4)));
end $$;
