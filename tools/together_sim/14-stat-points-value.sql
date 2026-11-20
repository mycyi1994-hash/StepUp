-- 확인: 능력치 포인트를 한 곳에 다 넣으면 하루에 남는 SUP 가 얼마나 다른가(30분 · 60분 한 번 달리기)
-- 2026-11-21: 효율이 늘 가장 많이 남고, 착화감은 포인트를 안 쓴 것과 같다(에너지가 남아돈다). docs/economy-roots-2026-11-21.md 뿌리 3.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '0' where key = 'free_repair_hours';
create temp table out_pts (rar text, lvl int, alloc text, mins int, dur_max int, eff int, comfort int, energy_max numeric, energy_used numeric, rewarded int, steps int, earned numeric, repair numeric, net numeric, reason text);
do $$
declare v_rar text; v_lvl int; v_alloc text; v_min int; n int := 300; v_id bigint; s public.market_sneakers; v_pts int; v_t timestamptz; r record; w public.walk_sessions;
  v_d0 timestamptz := economy.game_day_start(economy.game_day(now()) - 1); e record; v_rep numeric;
begin
  foreach v_rar in array array['COMMON', 'RARE', 'EPIC'] loop
  foreach v_lvl in array array[5, 10, 20] loop
  foreach v_alloc in array array['EFF', 'COMFORT', 'DUR'] loop
  foreach v_min in array array[30, 60] loop
    n := n + 1;
    insert into auth.users (id, email) values (t.u(n), 'pts' || n || '@test.invalid');
    perform t.login(n);
    perform public.economy_bootstrap();
    update public.profiles set created_at = now() - interval '40 days' where id = t.u(n);
    update public.market_sneakers set equipped = false where owner_id = t.u(n);
    v_pts := economy.points_total_of('FREE_DRAW', v_rar, v_lvl);
    v_id := economy.draw_create(t.u(n), 'PAID_DRAW');
    update public.market_sneakers
       set rarity = v_rar, level = v_lvl, equipped = true,
           efficiency_bps = economy.stat_efficiency(v_rar, 0.5), comfort_bps = economy.stat_comfort(v_rar, 0.5), dur_base = economy.stat_durability(v_rar, 0.5),
           pts_eff = case when v_alloc = 'EFF' then v_pts else 0 end, pts_comfort = case when v_alloc = 'COMFORT' then v_pts else 0 end,
           pts_dur = case when v_alloc = 'DUR' then v_pts else 0 end
     where id = v_id;
    update public.market_sneakers set dur_now = dur_max where id = v_id;
    select * into s from public.market_sneakers where id = v_id;
    select * into e from economy.shoe_effective(s);
    v_t := v_d0 + interval '9 hours' + make_interval(secs => n * 7);
    select * into r from public.record_session(v_t, v_t + make_interval(mins => v_min), v_min * 165, v_min * 60, t.track(v_t, v_min * 60), 0, 1, '', false, v_id);
    select * into w from public.walk_sessions where id = r.session_id;
    v_rep := (s.dur_max - (select dur_now from public.market_sneakers where id = v_id)) * economy.repair_unit_of(s);
    insert into out_pts values (v_rar, v_lvl, v_alloc, v_min, s.dur_max, e.efficiency_bps, e.comfort_bps, economy.energy_max(t.u(n)), w.energy_used, w.rewarded_steps, w.steps,
      r.points_awarded, v_rep, r.points_awarded - v_rep, r.verdict_reason);
  end loop; end loop; end loop; end loop;
end $$;
do $$
declare r record;
begin
  for r in select * from out_pts order by array_position(array['COMMON','RARE','EPIC'], rar), lvl, mins, array_position(array['EFF','COMFORT','DUR'], alloc) loop
    perform t.say(format('%s Lv%s · %s분 · %s', r.rar, r.lvl, r.mins, case r.alloc when 'EFF' then '효율' when 'COMFORT' then '착화감' else '내구도' end),
      format('신발 수명 %s분 · 에너지 %s/%s · 적립 걸음 %s/%s · 적립 %s − 수리 %s = 남음 %s%s', r.dur_max / 5, round(r.energy_used, 1), r.energy_max,
             r.rewarded, r.steps, round(r.earned, 1), round(r.repair, 1), round(r.net, 1),
             case when r.reason like '%내구도%' then ' · 신발이 먼저 닳음' when r.reason like '%에너지%' then ' · 에너지 끝' else '' end));
  end loop;
end $$;
