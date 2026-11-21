-- 기준선(대격변 설계 — docs/stat-overhaul-2026-11-21.md): 지금 서버의 하루 셈(economy.points_day_net)을 등급 · 레벨 · 달리는 시간 · 포인트 배분마다 낸다.
-- 한 줄 = BASE|등급|레벨|분|효율|착화감|내구도 포인트|하루에 남는 SUP. tools/economy_sim/stats_today.py --verify 가 모형과 맞대어 본다.
do $$
declare
  s public.market_sneakers; g text; lv int; mi int; tot int; pe int; pc int; pd int; st int; r record; v_runs jsonb;
begin
  perform t.mk(1);
  select * into s from public.market_sneakers where owner_id = t.u(1) limit 1;
  foreach g in array array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'] loop
    s.rarity := g;
    s.efficiency_bps := economy.stat_efficiency(g, 0.5);
    s.comfort_bps := economy.stat_comfort(g, 0.5);
    s.dur_base := economy.stat_durability(g, 0.5);
    foreach lv in array array[1, 10, case g when 'COMMON' then 20 when 'RARE' then 20 when 'EPIC' then 26 when 'LEGENDARY' then 27 when 'REDLINE' then 28 else 30 end] loop
      tot := economy.points_total_of('FREE_DRAW', g, lv);
      st := greatest(tot / 4, 1);
      foreach mi in array array[20, 40, 60, 90] loop
        v_runs := jsonb_build_array(jsonb_build_object('d', 'x', 'o', 1, 'n', mi * 165, 't', mi * 60));
        for pe in 0 .. tot by st loop
          for pd in 0 .. tot - pe by st loop
            pc := tot - pe - pd;
            select * into r from economy.points_day_net(s, v_runs, 10, pe, pc, pd);
            raise notice 'BASE|%|%|%|%|%|%|%', g, lv, mi, pe, pc, pd, r.net;
          end loop;
        end loop;
      end loop;
    end loop;
  end loop;
end $$;
