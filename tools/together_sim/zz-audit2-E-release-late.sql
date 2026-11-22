-- 감사 E: 돌려받기 10분(0122 #2) — 출발을 놓친 정직한 사람(박동 0 · 러닝 없음)이 앱을 15분 뒤에 열면 돌려받기 false →
-- 그날 크루 러닝을 다시 못 한다. 앱은 놓친 출발을 몇 시간 뒤에도 돌려받도록 만들어져 있다(CrewRepository.recoverUnrunToday · rooms).
-- 0122 의 묶인 체력(crew_pool_held_except)이 이미 박동 0 인 사람의 몫을 남이 쓰지 못하게 하므로, 늦은 돌려받기는 아무도 쓰지 않은 몫만 돌려준다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(n) from generate_series(41, 45) n;
select t.crew(4, 41, array[42, 43, 44, 45], 1);
do $$
declare p bigint; p2 bigint; v jsonb; n int; ok boolean; cap0 numeric; cap1 numeric; c43 text; err text := '';
begin
  perform t.login(41);
  p := public.party_create_v2(t.c(4), '방 1', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  foreach n in array array[42, 43] loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(41); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  -- 41 · 42 는 15분 박동, 43 은 출발을 놓쳤다(폰이 뒤에 있었다)
  perform t.run(p, array[41, 42], 900);
  select capacity into cap0 from economy.crew_day_pools where crew_id = t.c(4);
  perform t.login(43); ok := public.crew_run_release(p);
  select capacity into cap1 from economy.crew_day_pools where crew_id = t.c(4);
  c43 := t.cr(43);
  -- 같은 날 다른 방: 44 진행 · 45 · 43
  perform t.login(44);
  p2 := public.party_create_v2(t.c(4), '방 2', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p2, true);
  foreach n in array array[45, 43] loop perform t.login(n); perform public.party_join(p2); perform public.party_ready(p2, true); end loop;
  perform t.login(44);
  begin perform public.party_start_rev(p2, 0); exception when others then err := sqlerrm; end;
  raise notice 'RESULT 15분 뒤 돌려받기 % · 공용 체력 % → % · 43 의 오늘 크루 러닝 [%] · 방 2 출발 명단에 43 %  %',
    ok, cap0, cap1, c43,
    exists (select 1 from economy.crew_runs c where c.party_id = p2 and c.user_id = t.u(43)), err;
  raise notice '   43 의 몫은 묶여 있다(아무도 못 씀): held %', round(economy.crew_pool_held(t.c(4), economy.game_day(now()), now()), 2);
end $$;
