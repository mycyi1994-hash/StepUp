-- 감사 2 G — 달린 신발을 올리기 전에 강화 재료로 태우면 그 러닝은 닳지 않고, 재료 값도 닳기 전 값으로 들어간다.
-- record_session(0120)은 태운 신발이면 "그 신발의 마지막 값으로 셈 · 닳음을 남기지 않음"(0118) — 강화 쪽(forge_xp_value)은
-- 닳은 몫(repair_part)을 빼는데, 아직 올라가지 않은 러닝의 닳음은 서버가 모른다. 두 사람이 같은 신발로 같은 러닝:
--   u1 정직: 올린 뒤 태움 · u2 꼼수: 다른 신발을 신고(다른 폰 · 앱 수정) 태운 뒤 올림.
-- 견줄 것: 적립 SUP(같아야), 받은 경험치(꼼수가 더), 그 차이 = 이 러닝의 닳음 × 수리 1점 값(무료로 고친 것과 같다).
select t.mk(1); select t.mk(2);
do $$
declare
  n int; r text; x bigint; tgt bigint; v jsonb; q jsonb; s public.market_sneakers; v_start timestamptz := now() - interval '3 hours';
  res text; xp_got int; pts numeric; d_before int; d_after int; v_unit numeric;
  out_pts numeric[] := '{}'; out_xp int[] := '{}';
begin
  foreach r in array array['RARE', 'EPIC'] loop
    out_pts := '{}'; out_xp := '{}';
    for n in 1 .. 2 loop
      perform t.login(n);
      delete from public.walk_sessions where user_id = t.u(n);
      x := economy.draw_create(t.u(n), 'FREE_DRAW');
      tgt := economy.draw_create(t.u(n), 'FREE_DRAW');
      update public.market_sneakers set rarity = r, efficiency_bps = economy.stat_efficiency(r, 0.5), comfort_bps = economy.stat_comfort(r, 0.5),
             dur_base = economy.stat_durability(r, 0.5), dur_now = economy.stat_durability(r, 0.5) where id = x;
      update public.market_sneakers set rarity = r, level = 1, forge_xp = 0 where id = tgt;
      -- 출발 신발 = x (앱이 p_sneaker 로 알린다) · 신은 것도 x
      perform public.sneaker_equip(x);
      select * into s from public.market_sneakers where id = x;
      v_unit := economy.repair_unit_of(s);
      d_before := s.dur_now;
      if n = 1 then
        res := t.rec(n, v_start, 1200, 3300, true, x);          -- 20분(Lv1 에너지) 러닝을 먼저 올린다
        perform public.sneaker_equip((select id from public.market_sneakers where owner_id = t.u(n) and origin = 'STARTER'));
        q := public.forge_xp_quote(tgt, array[x]);
        v := public.forge_xp_add(gen_random_uuid(), tgt, array[x], q ->> 'quote_version');
      else
        perform public.sneaker_equip((select id from public.market_sneakers where owner_id = t.u(n) and origin = 'STARTER'));
        q := public.forge_xp_quote(tgt, array[x]);
        v := public.forge_xp_add(gen_random_uuid(), tgt, array[x], q ->> 'quote_version');
        res := t.rec(n, v_start, 1200, 3300, true, x);          -- 태운 뒤에 같은 러닝을 올린다
      end if;
      select points_awarded into pts from public.walk_sessions where user_id = t.u(n) and started_at = v_start;
      xp_got := (v ->> 'xp_gain')::int;
      if xp_got is null then xp_got := (q ->> 'gain')::int; end if;
      raise notice '%', format('%s u%s %s · SUP %s · 재료 own(닳은 몫 뺀 값) %s · 견적 gain %s · 결과 %s', r, n, case when n = 1 then '정직' else '꼼수' end,
        pts, q -> 'materials' -> 0 ->> 'own', q ->> 'gain', left(v::text, 160));
      raise notice '%', format('   러닝 결과: %s', res);
      out_pts := out_pts || pts; out_xp := out_xp || (q ->> 'gain')::int;
    end loop;
    raise notice '%', format('%s %s — SUP 같음 %s = %s · 경험치 꼼수 %s vs 정직 %s (+%s = %s SUP어치, 수리 1점 %s SUP)',
      r, case when out_xp[2] > out_xp[1] and out_pts[1] = out_pts[2] then 'FAIL' else 'OK  ' end,
      out_pts[1], out_pts[2], out_xp[2], out_xp[1], out_xp[2] - out_xp[1],
      round((out_xp[2] - out_xp[1]) * economy.fx('forge_xp_sup_per_unit', 24) / 1000.0, 1), round(v_unit, 4));
  end loop;
end $$;
