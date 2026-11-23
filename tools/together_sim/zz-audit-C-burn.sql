-- 감사 C(2026-11-22): 0121 뒤 하루 발행 · 소각 — 에너지를 다 쓸 때까지 달리고(분당 165보) 닳으면 가득 수리(무료 수리 끔, 운 1).
-- 러너 Lv1(새 계정 5.5) · 0121 전 계정(보장 10) · 러너 Lv21(16.5)에서 같은 신발. 소각(수리 ÷ 적립)이 Lv1 신발 30% 에서
-- 만렙 · 내구도 포인트 신발은 5 ~ 12% 로 떨어지고(0119 dur_repair_exp), 0121 이 그 위의 하루 발행을 1.65 ~ 3 배로 올린다.
-- 상자 사기(일반 40 × 하루 3 + 상급 600 × 주 2 ≈ 하루 291 SUP)가 유일한 다른 소각이다.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '0' where key = 'free_repair_hours';
create table t.burn (who text, rarity text, lvl int, alloc text, energy numeric, minutes int, earned numeric, repair numeric);
create or replace function t.day(n int, p_rar text, p_lvl int, p_alloc text, p_ctx text) returns void language plpgsql as $$
declare v_shoe bigint; s public.market_sneakers; v_t timestamptz; v_day date; v_e numeric; v_need int; v_mins int; r record;
        v_earned numeric := 0; v_repair numeric := 0; v_b0 numeric; v_min_total int := 0; v_g int := 0; v_pts int; v_pe int; v_pc int; v_pd int; d int;
begin
  perform t.mk(n, 100, 0);
  if p_ctx = 'Lv21' then
    for d in 10 .. 14 loop
      insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps, gps_backed, rewarded_steps)
      values (t.u(n), now() - make_interval(days => d), now() - make_interval(days => d) + interval '4 hours', 14400, 40000, 'CLEAN', 40000, 40000, true, 0);
    end loop;
  elsif p_ctx = 'legacy' then
    insert into economy.runner_levels (user_id, legacy_energy) values (t.u(n), 10) on conflict (user_id) do update set legacy_energy = 10;
  end if;
  insert into public.sup_ledger (user_id, kind, amount, description) values (t.u(n), 'EARN_EVENT', 1000000, 'sim');
  v_pts := economy.points_total_of('FREE_DRAW', p_rar, p_lvl);
  v_pe := case p_alloc when 'EVEN' then v_pts / 3 else 0 end;
  v_pc := case p_alloc when 'EVEN' then v_pts / 3 else 0 end;
  v_pd := v_pts - v_pe - v_pc;
  update public.market_sneakers set equipped = false where owner_id = t.u(n);
  insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability, origin, efficiency_bps, comfort_bps, equipped, dur_base, pts_eff, pts_comfort, pts_dur)
  values (t.u(n), 'NONE', p_rar, 0, p_lvl, 100, 'FREE_DRAW', economy.stat_efficiency(p_rar, 0.5), economy.stat_comfort(p_rar, 0.5), true,
          economy.stat_durability(p_rar, 0.5), v_pe, v_pc, v_pd)
  returning id into v_shoe;
  update public.market_sneakers set dur_now = dur_max where id = v_shoe;
  perform t.login(n);
  v_t := economy.game_day_start(economy.game_day(now()) - 1) + interval '7 hours';
  v_day := economy.game_day(v_t);
  loop
    v_g := v_g + 1; exit when v_g > 200;
    v_e := economy.energy_left(t.u(n), v_day);
    exit when v_e < 0.05;
    select * into s from public.market_sneakers where id = v_shoe;
    if s.dur_now < 5 * economy.wear_mult(s) then
      v_b0 := economy.balance_of(t.u(n)); perform public.sneaker_repair(v_shoe); v_repair := v_repair + v_b0 - economy.balance_of(t.u(n));
      select * into s from public.market_sneakers where id = v_shoe;
    end if;
    v_need := ceil(v_e * 600 / (1 - (select comfort_bps from economy.shoe_effective(s)) / 10000.0) / economy.energy_mult(s) / 165)::int;
    v_mins := least(floor(s.dur_now / (5 * economy.wear_mult(s)))::int, v_need);
    exit when v_mins < 1;
    select * into r from public.record_session(v_t, v_t + make_interval(mins => v_mins), v_mins * 165, v_mins * 60, t.track(v_t, v_mins * 60), 0, 1, '', false, v_shoe);
    v_earned := v_earned + r.points_awarded; v_min_total := v_min_total + v_mins;
    v_t := v_t + make_interval(mins => v_mins + 2);
    exit when r.points_awarded = 0;
  end loop;
  select * into s from public.market_sneakers where id = v_shoe;
  if s.dur_now < s.dur_max then
    v_b0 := economy.balance_of(t.u(n)); perform public.sneaker_repair(v_shoe); v_repair := v_repair + v_b0 - economy.balance_of(t.u(n));
  end if;
  insert into t.burn values (p_ctx, p_rar, p_lvl, p_alloc, economy.energy_max(t.u(n)), v_min_total, v_earned, v_repair);
end $$;
do $$
declare n int := 100; c text; g record;
begin
  foreach c in array array['Lv1', 'legacy', 'Lv21'] loop
    for g in select * from (values ('RARE', 1, 'EVEN'), ('RARE', 20, 'EVEN'), ('EPIC', 26, 'EVEN'), ('FINISH', 1, 'EVEN'), ('FINISH', 30, 'EVEN'), ('FINISH', 30, 'DUR')) x(r, l, a) loop
      n := n + 1;
      perform t.day(n, g.r, g.l, g.a, c);
    end loop;
  end loop;
end $$;
do $$
declare b record;
begin
  for b in select * from t.burn order by rarity, lvl, alloc, array_position(array['Lv1', 'legacy', 'Lv21'], who) loop
    perform t.say(format('%s Lv%s %s · 러너 %s(에너지 %s, %s분)', b.rarity, b.lvl, b.alloc, b.who, b.energy, b.minutes),
      format('적립 %s · 수리 %s · 남음 %s · 소각 %s%%', round(b.earned, 1), round(b.repair, 1), round(b.earned - b.repair, 1), round(100 * b.repair / nullif(b.earned, 0), 1)));
  end loop;
  perform t.say('다른 소각(상자 사기)의 하루 최대', round(economy.setting_num('box_price') * economy.setting_num('box_daily_buy')
                + economy.setting_num('premium_box_price') * economy.setting_num('premium_box_weekly_buy') / 7.0, 1));
end $$;
