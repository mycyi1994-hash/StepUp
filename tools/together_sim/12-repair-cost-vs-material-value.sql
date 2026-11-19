-- 확인: 등급마다 가득 수리비(가운데 신발) — 닳은 재료 규칙이 등급마다 맞는가
select t.mk(1);
do $$
declare r text; v_id bigint; s public.market_sneakers; v numeric;
begin
  foreach r in array array['COMMON','RARE','EPIC','LEGENDARY'] loop
    v_id := economy.draw_create(t.u(1), 'PAID_DRAW');
    update public.market_sneakers set rarity = r, level = 1, pts_eff = 0, pts_comfort = 0, pts_dur = 0,
           efficiency_bps = economy.stat_efficiency(r, 0.5), comfort_bps = economy.stat_comfort(r, 0.5),
           dur_base = economy.stat_durability(r, 0.5), dur_now = 0 where id = v_id;
    select * into s from public.market_sneakers where id = v_id;
    v := economy.repair_unit_of(s) * s.dur_max;
    perform t.say(r, format('가득 수리 %s SUP · 재료 값 %s SUP 어치(24 SUP × %s켤레값)', round(v, 1), round(24.1 * power(3.1, economy.rarity_ord(r)), 0), round(power(3.1, economy.rarity_ord(r))::numeric, 2)));
  end loop;
end $$;
