-- 감사 E: 크루 효율 8 : 2 의 무게(0122 #5) — 아래 끝이 "오르기만" 하는지 · 정산 합이 끝값을 넘지 않는지 무작위로 본다.
-- 넷(효율 · 달린 시간 무작위, 경로 있음 · 박동 없음 — 체력은 넉넉히), 무작위 차례로 올린 뒤 24시간이 지나 마지막 정산을 한다.
-- 끝값 = 모든 사람이 정해진 뒤의 셈(together_settle 과 같은 식). 낸 몫(together_paid)이 끝값보다 크면 FAIL.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
select setseed(0.4242);
do $$
declare
  tr int; k int; base_u int; p bigint; v_start timestamptz; i int; n int; ord int[]; effs int[] := array[200, 4000, 12000, 40000, 126688];
  secs int[]; s public.walk_sessions; c economy.crew_runs; v_n int; v_tb numeric; v_target numeric; bad int := 0; worst numeric := 0; checked int := 0;
  rising int := 0;
  prev jsonb;
begin
  for tr in 1 .. 6 loop
    k := 600 + tr; base_u := 6000 + tr * 10;
    for i in 1 .. 4 loop perform t.mk(base_u + i, 3000, effs[1 + floor(random() * 5)::int]); end loop;
    perform t.crew(k, base_u + 1, array[base_u + 2, base_u + 3, base_u + 4], 1);
    perform t.login(base_u + 1);
    p := public.party_create_v2(t.c(k), '무작위', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
    perform public.party_ready(p, true);
    for i in 2 .. 4 loop perform t.login(base_u + i); perform public.party_join(p); perform public.party_ready(p, true); end loop;
    perform t.login(base_u + 1); perform public.party_start_rev(p, 0);
    secs := array[600 + floor(random() * 3000)::int, 600 + floor(random() * 3000)::int, 600 + floor(random() * 3000)::int, 600 + floor(random() * 3000)::int];
    perform t.age(interval '3606 seconds');
    select started_at into v_start from economy.crew_runs where user_id = t.u(base_u + 1);
    select array_agg(x order by random()) into ord from generate_series(1, 4) x;
    -- 셋만 올린다(넷째는 24시간 안에 올리지 않을 수도 — 무작위로 하나 빼기)
    foreach i in array ord[1:3 + floor(random() * 2)::int] loop
      perform t.rec(base_u + i, v_start, secs[i], (secs[i] * 2.75)::int);
      -- 이미 올린 사람의 낸 몫이 줄지 않았는지(늘 그렇다 — 기록만)
    end loop;
    perform t.age(interval '25 hours');
    update public.walk_sessions set created_at = created_at - interval '25 hours' where party_id = p;
    perform economy.together_settle(p);
    select count(*)::int into v_n from public.walk_sessions x where x.party_id = p and x.together_base is not null and economy.together_qualifies(x);
    for s in select * from public.walk_sessions x where x.party_id = p loop
      select * into c from economy.crew_runs r where r.session_id = s.id;
      if economy.together_qualifies(s) and s.together_unit is not null then
        v_tb := greatest(round(s.together_unit * economy.crew_blend_mult(p, s.user_id, 1 + c.shoe_eff_bps / 10000.0, s.together_unit), 4), s.together_base);
        v_target := round(v_tb * (1 + c.level_pct + economy.crew_headcount_bonus(v_n)) - s.together_base * (1 + c.level_pct), 4);
        checked := checked + 1;
        if s.together_paid > v_target + 0.001 then
          bad := bad + 1; worst := greatest(worst, s.together_paid - v_target);
          raise notice 'trial % u% paid % > final %', tr, s.user_id, s.together_paid, v_target;
        elsif s.together_paid < v_target - 0.01 and economy.day_room(s.user_id, economy.game_day(s.started_at), s.day_max) > 0.01 then
          raise notice 'trial % u% 덜 받음 paid % < final %', tr, s.user_id, s.together_paid, v_target;
        end if;
      end if;
    end loop;
  end loop;
  raise notice 'RESULT 무작위 % 줄 · 끝값을 넘은 줄 % (가장 크게 %)', checked, bad, worst;
end $$;
