-- 감사 A — 48시간 무료 수리(0116 repair_core): 값은 한 켤레 "가득"이라 위가 없고 등급 효율만큼 커진다. 하루 최대 SUP 밖.
-- 등급마다 가운데 신발(효율 · 착화감 · 내구도 0.5, 포인트 없음)을 다 닳게 두고 무료 수리를 쓴다 — 그 값 · 유료였으면 낼 값 ·
-- 에너지로 하루에 닳는 양(Lv1 20분 · Lv21 60분, 착화감만큼 더 오래)과 견주어 "이틀 닳은 몫 가운데 무료가 메우는 몫"을 본다.
do $$
declare
  r text;
  i int := 0;
  v_id bigint;
  s public.market_sneakers;
  v jsonb;
  v_unit numeric;
  v_comfort int;
  v_eff int;
  e numeric;
  v_min numeric;
  v_wear48 numeric;
  v_gross numeric;
  v_paid numeric;
  v_max numeric;
begin
  foreach r in array array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'] loop
    i := i + 1;
    perform t.mk(40 + i);
    update public.market_sneakers x
       set rarity = r, efficiency_bps = economy.stat_efficiency(r, 0.5), comfort_bps = economy.stat_comfort(r, 0.5),
           dur_base = economy.stat_durability(r, 0.5), dur_now = 0, origin = 'FREE_DRAW'
     where x.owner_id = t.u(40 + i) and x.equipped
    returning x.id into v_id;
    select * into s from public.market_sneakers where id = v_id;
    v_unit := economy.repair_unit_of(s);
    v_eff := (economy.shoe_effective(s)).efficiency_bps;
    v_comfort := (economy.shoe_effective(s)).comfort_bps;
    perform t.login(40 + i);
    v := public.sneaker_repair_v2(v_id, null, gen_random_uuid(), null);
    v_max := economy.daily_max_of(t.u(40 + i), v_eff, 1);
    raise notice '%', format('%s dur_max=%s unit=%s free=%s cost=%s free_value=%s daily_max=%s', r, s.dur_max, round(v_unit, 4),
                         v ->> 'free', v ->> 'cost', (select u.value from economy.repair_free_uses u where u.user_id = t.u(40 + i)),
                         round(v_max, 1));
    -- 하루 에너지 e(Lv1 5.5 · Lv21 16.5) → 달리는 분(분당 165보 · 착화감만큼 덜 씀) → 닳음(1분 5) · 버는 돈(걸음 × 걸음당 × 효율)
    foreach e in array array[5.5, 16.5] loop
      v_min := e * 600 / (1 - v_comfort / 10000.0) / 165;
      v_wear48 := 2 * 5 * v_min;
      v_gross := e * 600 / (1 - v_comfort / 10000.0) * economy.points_per_step() * (1 + v_eff / 10000.0);
      v_paid := greatest(v_wear48 - s.dur_max, 0) / 2 * v_unit;   -- 하루에 실제로 내는 수리비(무료가 이틀에 한 번 가득)
      raise notice '%', format('   energy %s: %s min/day · gross %s SUP/day · repair owed %s SUP/day (%s%%) · paid after free %s SUP/day (%s%%) · free worth %s SUP/day',
                           e, round(v_min), round(v_gross, 1), round(v_wear48 / 2 * v_unit, 1), round(100 * v_wear48 / 2 * v_unit / v_gross),
                           round(v_paid, 1), round(100 * v_paid / v_gross), round(least(v_wear48, s.dur_max) / 2 * v_unit, 1));
    end loop;
  end loop;
end $$;
