-- 감사 E: 정직한 둘이 모두 비행기 모드로 60분(박동 0) — 0122 의 crew_pool_held_except 는 먼저 올린 사람에게
-- 아직 올리지 않은(소식이 끊긴) 동료의 몫 전체를 묶는다. 동료가 올린 뒤에도 그 동료는 active_sec 0 · ENDED 라 ① 에 걸려 남은 몫이 영영 묶인다.
-- 같은 장면을 0120 처럼(held_except = 0) 한 번 더 돌려 견준다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
create or replace function t.pair(k int, a int, b int) returns text language plpgsql as $$
declare p bigint; v jsonb; v_start timestamptz; wa public.walk_sessions; wb public.walk_sessions; d economy.crew_day_pools;
begin
  perform t.mk(a, 150, 4000);   -- 30분짜리 신발
  perform t.mk(b, 600, 4000);   -- 120분짜리 신발
  perform t.crew(k, a, array[b], 1);
  perform t.login(a);
  p := public.party_create_v2(t.c(k), '오프라인 둘', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  perform t.login(b); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(a); perform public.party_start_rev(p, 0);
  perform t.age(interval '3606 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(a);
  perform t.rec(a, v_start, 3600, 9900);
  perform t.rec(b, v_start, 3600, 9900);
  select * into wa from public.walk_sessions x where x.user_id = t.u(a) and x.party_id = p;
  select * into wb from public.walk_sessions x where x.user_id = t.u(b) and x.party_id = p;
  select * into d from economy.crew_day_pools x where x.crew_id = t.c(k);
  return format('먼저 올린 u%s %s SUP (적립 걸음 %s) · 나중 u%s %s SUP (적립 걸음 %s) · 체력 %s / %s 씀 · 끝에 묶인 %s',
    a, wa.points_awarded, wa.rewarded_steps, b, wb.points_awarded, wb.rewarded_steps, round(d.consumed, 1), round(d.capacity, 1),
    round(economy.crew_pool_held(t.c(k), d.day, now()), 1));
end $$;
do $$ begin raise notice 'RESULT 0122: %', t.pair(31, 301, 302); end $$;
-- 0120 처럼: 올릴 때 묶인 몫을 빼지 않는다
create or replace function economy.crew_pool_held_except(p_crew uuid, p_day date, p_at timestamptz, p_user uuid)
returns numeric language sql stable as $$ select 0::numeric $$;
do $$ begin raise notice 'RESULT 0120식: %', t.pair(32, 311, 312); end $$;
