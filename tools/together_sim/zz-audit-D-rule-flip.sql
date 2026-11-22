-- 감사 D: 되돌리기 스위치 crew_bonus_rule 'ALL' 로 둔 동안 올린 크루 러닝은 인원 보너스를 올릴 때 받고(crew_run_bonus_eval) together_base 도 적힌다.
-- 스위치를 다시 'ACTIVE' 로 돌리면 24시간 안에 다음 사람이 올릴 때 together_settle 이 같은 러닝에 인원 보너스를 한 번 더 준다(together_paid 가 0 이라서)
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
update public.economy_settings set value = '"ALL"' where key in ('crew_bonus_rule', 'party_bonus_rule');
select t.mk(n) from generate_series(41, 43) n;
select t.crew(4, 41, array[42, 43], 1);
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; c economy.crew_runs; w public.walk_sessions; head1 numeric; r text;
begin
  perform t.login(41);
  p := public.party_create_v2(t.c(4), '스위치', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 42 .. 43 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(41); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[41, 42, 43], 900);
  select started_at into v_start from economy.crew_runs where user_id = t.u(41);
  for n in 41 .. 43 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  -- 'ALL' 동안 첫째가 올린다 — 인원 보너스(출발한 셋)를 올릴 때 받는다
  r := t.rec(41, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(41);
  select * into w from public.walk_sessions where id = c.session_id;
  head1 := c.headcount_sup;
  perform t.say('ALL: u41 upload', format('base=%s head=%s(%s명 +%s) total=%s · together_base=%s paid=%s', c.base_sup, c.headcount_sup, c.bonus_count, c.bonus_pct, c.total_sup, w.together_base, w.together_paid));
  -- 운영자가 되돌리기를 풀어 'ACTIVE' 로(같은 날)
  update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
  perform t.rec(42, v_start, 900, 2475);
  perform t.rec(43, v_start, 900, 2475);
  select * into c from economy.crew_runs where user_id = t.u(41);
  select * into w from public.walk_sessions where id = c.session_id;
  perform t.say('ACTIVE: u41 after u42/u43 upload', format('base=%s head=%s(%s명 +%s) total=%s · together_paid=%s', c.base_sup, c.headcount_sup, c.bonus_count, c.bonus_pct, c.total_sup, w.together_paid));
  perform t.say('u41 headcount paid (ALL at upload + ACTIVE top-ups)', format('%s + %s = %s  (one bonus would be ≈ %s)', head1, w.together_paid, head1 + w.together_paid, round(c.base_sup * c.bonus_pct, 4)));
  perform t.say('u41 ledger', (select string_agg(format('%s %s %s', kind, amount, coalesce(ref, 'session')), ' | ' order by id) from public.sup_ledger where user_id = t.u(41)));
  select * into c from economy.crew_runs where user_id = t.u(42);
  perform t.say('u42 (uploaded under ACTIVE)', format('base=%s head=%s total=%s', c.base_sup, c.headcount_sup, c.total_sup));
end $$;
