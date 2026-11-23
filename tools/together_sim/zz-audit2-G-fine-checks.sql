-- 감사 2 G — 문제없어 보이는 것들을 숫자로: 다시 나누기의 비율 · 넘치는 경험치 · 레벨 상자 · 한 점씩 수리 · 무료 수리 동시 ·
-- 예전 확률 강화의 재료 막기(크루 러닝 중 신발).
select t.mk(1); select t.mk(2);
do $$
declare
  f bigint; c bigint; c2 bigint; s public.market_sneakers; s2 public.market_sneakers; v_val0 numeric; v_val1 numeric; q jsonb; v jsonb;
  m bigint; i int; bal0 numeric; bal1 numeric; full_cost numeric; one_cost numeric; n_box int;
begin
  perform t.login(1);
  insert into public.sup_ledger (user_id, kind, amount, description, occurred_at, created_at) values (t.u(1), 'EARN_EVENT', 50000, 'sim', now() - interval '20 days', now() - interval '20 days');
  update public.economy_settings set value = '0' where key = 'free_repair_hours';
  -- (1) 피니시 Lv30 · 304점 모두 내구도 · 거의 가득(−3) → 다시 나누기로 내구도 0 · 효율 304 — 남은 내구도의 SUP 값은 그대로인가
  f := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set rarity = 'FINISH', level = 30, efficiency_bps = economy.stat_efficiency('FINISH', 0.5), comfort_bps = economy.stat_comfort('FINISH', 0.5),
         dur_base = economy.stat_durability('FINISH', 0.5), pts_dur = 304, pts_respec = true where id = f;
  update public.market_sneakers set dur_now = dur_max - 3 where id = f;
  select * into s from public.market_sneakers where id = f;
  v_val0 := (s.dur_max - s.dur_now) * economy.repair_unit_of(s);
  perform public.sneaker_points_set(f, 304, 0, 0, gen_random_uuid());
  select * into s2 from public.market_sneakers where id = f;
  v_val1 := (s2.dur_max - s2.dur_now) * economy.repair_unit_of(s2);
  raise notice '%', format('(1) 다시 나누기: %s/%s → %s/%s · 모자란 몫의 수리비 %s → %s SUP · 다시 나누기 남음 %s',
    s.dur_now, s.dur_max, s2.dur_now, s2.dur_max, round(v_val0, 2), round(v_val1, 2), s2.pts_respec);
  begin
    perform public.sneaker_points_set(f, 0, 0, 304, gen_random_uuid());
    raise notice '(1) FAIL — 두 번째 줄이기가 됐다';
  exception when others then raise notice '(1) OK   두 번째 줄이기 막힘: %', sqlerrm; end;

  -- (2) 넘치는 경험치: 일반 Lv1 대상에 만렙 피니시(물려줌 ≈ 2천만) — 대상 만렙 20 · 넘친 몫은 버림 · 레벨 상자 5 · 10 · 15 · 20 한 번씩
  c := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set rarity = 'COMMON', level = 1, forge_xp = 0 where id = c;
  update public.market_sneakers set dur_now = dur_max where id = f;
  q := public.forge_xp_quote(c, array[f]);
  v := public.forge_xp_add(gen_random_uuid(), c, array[f], q ->> 'quote_version');
  select * into s from public.market_sneakers where id = c;
  select count(*) into n_box from economy.box_awards where user_id = t.u(1) and source like 'LEVEL:%';
  raise notice '%', format('(2) Lv%s xp=%s 넘침 %s · 레벨 상자 %s개(%s) · 다시 나누기 %s · 내구도 %s/%s',
    s.level, s.forge_xp, v ->> 'overflow', n_box, v -> 'level_boxes', s.pts_respec, s.dur_now, s.dur_max);
  begin
    q := public.forge_xp_quote(c, array[economy.draw_create(t.u(1), 'FREE_DRAW')]);
    raise notice '(2) 만렙 대상 견적: %', q;
  exception when others then raise notice '(2) %', sqlerrm; end;
  -- 둘째 신발이 Lv5 를 넘어도 상자는 다시 오지 않는다(계정마다 한 번)
  c2 := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set rarity = 'COMMON', level = 4 where id = c2;
  update public.market_sneakers set level = 5 where id = c2;
  select count(*) into i from economy.box_awards where user_id = t.u(1) and source like 'LEVEL:%';
  raise notice '%', format('(2) 둘째 신발 Lv5 — 레벨 상자 %s → %s', n_box, i);

  -- (3) 한 점씩 수리 1,000번 vs 한 번에 — 반올림으로 싸지지 않는가
  m := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set rarity = 'RARE', efficiency_bps = economy.stat_efficiency('RARE', 0.5), dur_base = 265 where id = m;
  update public.market_sneakers set dur_now = 0 where id = m;
  select * into s from public.market_sneakers where id = m;
  full_cost := round(s.dur_max * economy.repair_unit_of(s), 4);
  bal0 := economy.balance_of(t.u(1));
  for i in 1 .. s.dur_max loop perform public.sneaker_repair_v2(m, 1, gen_random_uuid(), null); end loop;
  bal1 := economy.balance_of(t.u(1));
  raise notice '%', format('(3) 한 점씩 %s번 %s SUP vs 한 번에 %s SUP (1점 %s)', s.dur_max, bal0 - bal1, full_cost, round(economy.repair_unit_of(s), 6));
end $$;

-- (4) 예전 앱의 확률 강화는 크루 러닝 중인 신발(crew_runs 스냅샷)을 재료로 받는다 — 새 강화는 IN_ROOM 으로 막는다
do $$
declare p bigint; x bigint; a bigint; b bigint; tgt bigint; q jsonb; v jsonb; qx jsonb;
begin
  perform t.crew(1, 1, array[2], 1);
  perform t.login(1);
  x := economy.draw_create(t.u(1), 'FREE_DRAW');
  a := economy.draw_create(t.u(1), 'FREE_DRAW'); b := economy.draw_create(t.u(1), 'FREE_DRAW'); tgt := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set rarity = 'COMMON', level = 1 where id in (x, a, b, tgt);
  p := public.party_create_v2(t.c(1), '방', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_pick_shoe(p, x);
  perform public.party_ready(p, true);
  perform t.login(2); perform public.party_join(p); perform public.party_ready(p, true);
  perform t.login(1); perform public.party_start_rev(p, 0);
  perform t.age(interval '10 seconds');
  qx := public.forge_xp_quote(tgt, array[x]);
  q := public.forge_quote(tgt, array[x, a, b]);
  raise notice '%', format('(4) 크루 스냅샷 신발 #%s(%s) — 새 강화 %s · 예전 강화 %s', x,
    (select sneaker_id from economy.crew_runs where user_id = t.u(1)), coalesce(qx ->> 'error', 'ok'), coalesce(q ->> 'error', 'rate ' || (q ->> 'rate_permille')));
end $$;
