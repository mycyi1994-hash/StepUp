-- 감사 2 G — 태운 신발이 "유령 신발"로 남는다: record_session(0120)의 태운 신발 갈래(0118)는 그 신발이 이 러닝 전에 태워졌는지 보지 않는다.
-- 앱(또는 직접 부른 RPC)이 p_sneaker 에 지난달에 태운 신발 번호를 넣으면, 그 뒤 모든 러닝이 그 신발의 마지막 효율 · 내구도(가득)로
-- 셈되고 아무 신발도 닳지 않는다 — 수리비가 영영 0. 태운 신발의 경험치는 이미 다른 신발에 들어갔다.
--   u1 정직: 쓰던 에픽(가득)으로 사흘 달리고 수리 · u2 꼼수: 그 에픽을 첫날 강화 재료로 태우고 사흘 모두 p_sneaker = 태운 에픽.
select t.mk(1); select t.mk(2);
do $$
declare
  n int; d int; ep bigint; tgt bigint; st bigint; q jsonb; v jsonb; s public.market_sneakers; res text;
  v_start timestamptz; sup numeric[] := array[0, 0]::numeric[]; repair numeric[] := array[0, 0]::numeric[]; bal numeric; wear int;
begin
  for n in 1 .. 2 loop
    perform t.login(n);
    insert into public.sup_ledger (user_id, kind, amount, description, occurred_at, created_at) values (t.u(n), 'EARN_EVENT', 2000, 'sim', now() - interval '20 days', now() - interval '20 days');
    st := (select id from public.market_sneakers where owner_id = t.u(n) and origin = 'STARTER');
    ep := economy.draw_create(t.u(n), 'FREE_DRAW', 'RARE');
    tgt := economy.draw_create(t.u(n), 'FREE_DRAW', 'RARE');
    update public.market_sneakers set rarity = 'EPIC', efficiency_bps = economy.stat_efficiency('EPIC', 0.5), comfort_bps = economy.stat_comfort('EPIC', 0.5),
           dur_base = economy.stat_durability('EPIC', 0.5), dur_now = economy.stat_durability('EPIC', 0.5) where id = ep;
    update public.market_sneakers set rarity = 'EPIC', level = 1, forge_xp = 0 where id = tgt;
    update public.economy_settings set value = '0' where key = 'free_repair_hours';
    if n = 2 then
      -- 첫날 아침: 에픽을 벗고(첫 신발을 신는다) 강화 재료로 태운다
      perform public.sneaker_equip(st);
      q := public.forge_xp_quote(tgt, array[ep]);
      v := public.forge_xp_add(gen_random_uuid(), tgt, array[ep], q ->> 'quote_version');
      -- 태운 것은 닷새 전 — 아래 러닝은 모두 태운 뒤에 시작한다(서버는 때를 보지 않는다)
      update public.forge_requests set created_at = now() - interval '5 days' where user_id = t.u(2);
      update public.market_sneakers set updated_at = now() - interval '5 days' where id = ep;
      perform t.say('u2 닷새 전 에픽을 태움', format('gain %s · 대상 Lv%s', v ->> 'gain', v ->> 'level_after'));
    else
      perform public.sneaker_equip(ep);
    end if;
    for d in 1 .. 3 loop
      -- 사흘: 날마다 하루 에너지(새 계정 20분)만큼 — 한국 시각 낮(시계는 장면 밖에서 낮으로 맞춤), 날마다 하루씩 앞
      v_start := date_trunc('day', now() at time zone 'Asia/Seoul') at time zone 'Asia/Seoul' + interval '9 hours' - make_interval(days => 3 - d);
      res := t.rec(n, v_start, 1200, 3300, true, ep);
      sup[n] := sup[n] + (select points_awarded from public.walk_sessions where user_id = t.u(n) and started_at = v_start);
      if n = 1 then
        select * into s from public.market_sneakers where id = ep;
        wear := s.dur_max - s.dur_now;
        bal := economy.balance_of(t.u(1));
        if wear > 0 then perform public.sneaker_repair_v2(ep, null, gen_random_uuid(), null); end if;
        repair[1] := repair[1] + bal - economy.balance_of(t.u(1));
      end if;
      raise notice '%', format('  u%s 날 %s: %s', n, d, res);
    end loop;
    raise notice '%', format('u%s %s — 사흘 적립 %s SUP · 수리비 %s SUP · 남는 것 %s · 닳은 신발 %s', n, case when n = 1 then '정직' else '유령 신발' end,
      sup[n], round(repair[n], 2), round(sup[n] - repair[n], 2),
      (select string_agg(format('#%s %s/%s %s', x.id, x.dur_now, x.dur_max, x.status), ', ') from public.market_sneakers x
        where x.id in (ep, st, tgt)));
  end loop;
  raise notice '%', format('%s — 유령 신발은 수리 없이 같은 적립(%s vs %s SUP) · 수리비 0 vs %s',
    case when sup[2] >= sup[1] - 0.01 and repair[2] = 0 then 'FAIL' else 'OK  ' end, sup[2], sup[1], round(repair[1], 2));
end $$;
