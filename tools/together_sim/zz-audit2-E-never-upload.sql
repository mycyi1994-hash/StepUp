-- 감사 E: 출발만 하고 올리지 않는 사람(N)은 24시간 동안 크루 평균(8 : 2)의 아래 끝을 끌어내린다(약한 신발이면).
-- 24시간이 지나면 셈에서 빠지지만, 그 뒤 정산을 부르는 것은 같은 방의 누군가가 러닝을 올릴 때뿐이다(together_settle 은 record_session 에서만) —
-- 아무도 더 올리지 않으면 A 의 몫은 영영 아래 끝에 머문다. 24시간 뒤 정산을 손으로 한 번 불러 그 차이를 본다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select t.mk(401, 1000, 4000), t.mk(402, 1000, 126688), t.mk(403, 1000, 200);
select t.crew(40, 401, array[402, 403], 1);
do $$
declare p bigint; v_start timestamptz; a0 numeric; a1 numeric; n int; b0 numeric;
begin
  perform t.login(401);
  p := public.party_create_v2(t.c(40), '셋', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  foreach n in array array[402, 403] loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(401); perform public.party_start_rev(p, 0);
  perform t.age(interval '3606 seconds');
  select started_at into v_start from economy.crew_runs where user_id = t.u(401);
  raise notice '%', t.rec(401, v_start, 3600, 9900);
  raise notice '%', t.rec(402, v_start, 3600, 9900);
  select points_awarded into a0 from public.walk_sessions where user_id = t.u(401) and party_id = p;
  select points_awarded into b0 from public.walk_sessions where user_id = t.u(402) and party_id = p;
  perform t.age(interval '25 hours');
  -- 아무도 더 올리지 않는다. 25시간 뒤 누가 정산을 한 번 불렀다면:
  perform economy.together_settle(p);
  select points_awarded into a1 from public.walk_sessions where user_id = t.u(401) and party_id = p;
  raise notice 'RESULT 약한 신발 A: 올린 뒤 % SUP · 24시간 뒤 정산을 불렀다면 % SUP (차이 % — 정산을 부를 길이 없어 받지 못한다) · 피니시 B %',
    a0, a1, round(a1 - a0, 2), b0;
end $$;
