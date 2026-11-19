-- 강화 경험치(0118) 표 — tools/economy_sim/run.sh 가 부른다(ONLY=forge 면 이것만).
--   NEED|등급|레벨|통의 크기                   → forge_xp_model.py --check 가 모형과 맞대어 본다
--   VAL|등급|상태|제 값|물려줌|닳은 몫|스펙 q   → 같음(가득 · 절반 · 다 닳음 · Lv5)
--   REPAIR|등급|닳은 채 제 값|가득 제 값|수리비 SUP → check.py "수리하고 넣기 = 그냥 넣기"
\set ON_ERROR_STOP on
update public.economy_settings set value = '0'::jsonb where key = 'free_repair_hours';
insert into auth.users (id, email) values ('f0f0f0f0-0000-0000-0000-000000000118', 'forge@sim.test') on conflict do nothing;
select set_config('request.jwt.claim.sub', 'f0f0f0f0-0000-0000-0000-000000000118', false) \g /dev/null
select public.economy_bootstrap() \g /dev/null
select economy.ledger_apply('f0f0f0f0-0000-0000-0000-000000000118', 'EARN_WALK', 10000000, '강화 표', 'forge-sim') \g /dev/null

\pset format unaligned
\pset tuples_only on
select 'NEED|' || g || '|' || l || '|' || economy.forge_need(g, l)
  from unnest(array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH']) g, generate_series(1, 29) l
 where l < economy.max_level(g)
 order by array_position(array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'], g), l;

create temp table sim_shoes (g text, state text, id bigint);
do $$
declare g text; st text; v bigint;
begin
  foreach g in array array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'] loop
    foreach st in array array['FULL', 'HALF', 'ZERO', 'LV5'] loop
      v := economy.draw_create('f0f0f0f0-0000-0000-0000-000000000118', 'PAID_DRAW', g);
      update public.market_sneakers
         set rarity = g, model_id = (select min(m.id) from public.sneaker_models m where m.rarity = g),
             level = case when st = 'LV5' then 5 else 1 end, pts_eff = 0, pts_comfort = 0, pts_dur = 0, forge_xp = 0, equipped = false,
             efficiency_bps = economy.stat_efficiency(g, 0.5), comfort_bps = economy.stat_comfort(g, 0.5), dur_base = economy.stat_durability(g, 0.5)
       where id = v;
      update public.market_sneakers set dur_now = case st when 'HALF' then dur_max / 2 when 'ZERO' then 0 else dur_max end where id = v;
      insert into sim_shoes values (g, st, v);
    end loop;
  end loop;
end $$;
select 'VAL|' || x.g || '|' || x.state || '|' || v.own || '|' || v.inherit || '|' || v.repair_part || '|' || v.q
  from sim_shoes x join public.market_sneakers s on s.id = x.id cross join lateral economy.forge_xp_value(s) v
 order by array_position(array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'], x.g), x.state;

-- 닳은 재료를 실제로 수리해 본다(무료 수리 끔) — 수리비만큼 제 값이 오르는가
create temp table sim_repair (line text);
do $$
declare x record; s public.market_sneakers; v_worn int; v_full int; v_bal numeric;
begin
  for x in select * from sim_shoes where state in ('HALF', 'ZERO') order by id loop
    select * into s from public.market_sneakers where id = x.id;
    v_worn := (economy.forge_xp_value(s)).own;
    v_bal := economy.balance_of('f0f0f0f0-0000-0000-0000-000000000118');
    perform public.sneaker_repair(x.id);
    select * into s from public.market_sneakers where id = x.id;
    v_full := (economy.forge_xp_value(s)).own;
    insert into sim_repair values ('REPAIR|' || x.g || '|' || x.state || '|' || v_worn || '|' || v_full || '|'
                                   || round(v_bal - economy.balance_of('f0f0f0f0-0000-0000-0000-000000000118'), 6));
  end loop;
end $$;
select line from sim_repair;
\pset tuples_only off
\pset format aligned
