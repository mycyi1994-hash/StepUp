-- 감사 D: 러너 레벨(0121)이 켜지면 하루 에너지는 신발 레벨과 상관없다. 그런데 강화 화면의 값(forge_account_after, 0118)은
-- 여전히 "가진 신발 가운데 최고 레벨"로 에너지를 센다 — 견적의 하루 에너지 앞뒤가 틀리고, 러너 에너지가 10 을 넘는 사람(Lv11 ~)에게는
-- 모든 재료가 'TOP_LEVEL'(태우면 하루 에너지가 준다)로 보인다. energy_per_level 2(0119 되돌리기)도 러너 규칙이 켜져 있으면 아무 일도 않는다.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
select t.mk(1);
do $$
declare a bigint; b bigint; tgt bigint; j jsonb; q jsonb;
begin
  perform t.login(1);
  a := (select economy.draw_create(t.u(1), 'PAID_DRAW', 'COMMON'));
  b := (select economy.draw_create(t.u(1), 'PAID_DRAW', 'COMMON'));
  select id into tgt from public.market_sneakers where owner_id = t.u(1) and equipped;
  update public.market_sneakers set rarity = 'COMMON', equipped = false, level = 1 where owner_id = t.u(1) and id <> tgt;
  -- 지난 사흘 러닝 102,000 걸음(하루 34,000) — 러너 Lv11
  insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps)
  select t.u(1), now() - make_interval(days => d), now() - make_interval(days => d) + interval '3 hours', 10800, 34000, 'CLEAN', 34000, 34000
    from generate_series(1, 3) d;
  perform t.say('runner level / economy.energy_max', (select level from economy.runner_levels where user_id = t.u(1)) || ' / ' || economy.energy_max(t.u(1)));
  j := public.forge_xp_materials(tgt);
  perform t.say('forge target energy shown', j -> 'target' -> 'energy');
  perform t.say('materials precious', (select string_agg((x ->> 'id') || ':' || (x -> 'precious')::text, ' ') from jsonb_array_elements(j -> 'items') x));
  update public.economy_settings set value = '2' where key = 'energy_per_level';
  q := public.forge_xp_quote(tgt, array[a]);
  perform t.say('energy_per_level=2 → quote energy before/after', coalesce(q -> 'energy', q));
  perform t.say('energy_per_level=2 → real energy_max', economy.energy_max(t.u(1)));
end $$;
