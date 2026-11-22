-- 점검 2 (J): 강화 재료로 태운 신발을 p_sneaker 로 알리면 그 신발의 "마지막 값"으로 셈하고 닳게 하지 않는다(0118 의 다른 폰 경우).
-- 러닝 시작이 태운 뒤여도 막지 않는다 → 태운 신발이 영원히 닳지 않는 신발이 된다(수리비 0 · 닳은 구간 없음). 재료 값(강화 경험치)도 이미 받았다.
-- 견줄 것: 같은 신발을 신고 날마다 31분 + 수리 + 20분(하루 에너지 10 = 8,415보)을 달리는 정직한 사람.
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(71, 72) n;
select t.old(n) from generate_series(71, 72) n;
do $$
declare v_now timestamptz := date_trunc('minute', now()); v_start timestamptz; r record; n int; d int; v_new bigint; v_starter bigint;
        v_q jsonb; v_res jsonb; v_pts numeric; v_rep numeric; v_unit numeric; v_dur0 int; s public.market_sneakers; v_sneaker bigint;
begin
  for n in 71 .. 72 loop
    perform t.login(n);
    perform public.draw_free();
    select id into v_new from public.market_sneakers where owner_id = t.u(n) and origin <> 'STARTER' order by id desc limit 1;
    -- 뽑은 신발을 첫 신발과 같은 값으로(견주기 쉽게)
    update public.market_sneakers set rarity = 'COMMON', level = 1, efficiency_bps = 4000, comfort_bps = 2857, pts_eff = 0, pts_comfort = 0, pts_dur = 0,
           dur_base = 155, dur_now = 155 where id = v_new;
  end loop;
  -- u71 정직: 뽑은 신발을 신고 달린다
  perform t.login(71);
  select id into v_new from public.market_sneakers where owner_id = t.u(71) and origin <> 'STARTER';
  update public.market_sneakers set equipped = (id = v_new) where owner_id = t.u(71);
  -- u72 부정: 뽑은 신발을 첫 신발의 강화 재료로 태운다
  perform t.login(72);
  select id into v_new from public.market_sneakers where owner_id = t.u(72) and origin <> 'STARTER';
  select id into v_starter from public.market_sneakers where owner_id = t.u(72) and origin = 'STARTER';
  v_q := public.forge_xp_quote(v_starter, array[v_new]);
  v_res := public.forge_xp_add(gen_random_uuid(), v_starter, array[v_new], v_q ->> 'quote_version');
  perform t.say('u72 강화', format('gain=%s · 재료 상태 %s', v_res ->> 'gain', (select status || ' owner=' || coalesce(owner_id::text, 'null') from public.market_sneakers where id = v_new)));
  select economy.repair_cost_per_unit('COMMON', 1) into v_unit;
  for n in 71 .. 72 loop
    v_pts := 0; v_rep := 0;
    v_sneaker := case when n = 72 then v_new else null end;
    for d in reverse 4 .. 0 loop
      perform t.login(n);
      v_start := v_now - make_interval(days => d) - interval '5 hours';
      select * into r from public.record_session(v_start, v_start + interval '1860 seconds', 5115, 1860, t.trku(n, v_start, 1860), 0, 1, '', false, v_sneaker);
      v_pts := v_pts + r.points_awarded;
      if n = 71 then
        select * into s from public.market_sneakers where owner_id = t.u(71) and equipped;
        v_rep := v_rep + (s.dur_max - s.dur_now) * v_unit;
        update public.market_sneakers set dur_now = dur_max where id = s.id;
      end if;
      v_start := v_start + interval '1 hour';
      select * into r from public.record_session(v_start, v_start + interval '1200 seconds', 3300, 1200, t.trku(n, v_start, 1200, 2.7), 0, 1, '', false, v_sneaker);
      v_pts := v_pts + r.points_awarded;
      if n = 71 then
        select * into s from public.market_sneakers where owner_id = t.u(71) and equipped;
        v_rep := v_rep + (s.dur_max - s.dur_now) * v_unit;
        update public.market_sneakers set dur_now = dur_max where id = s.id;
      end if;
    end loop;
    perform t.say(format('u%s 5일', n), format('SUP %s · 수리비 %s · 남는 몫 %s (하루 %s)', v_pts, round(v_rep, 2), round(v_pts - v_rep, 2), round((v_pts - v_rep) / 5, 2)));
  end loop;
  perform t.say('u72 러닝이 쓴 신발', (select string_agg(distinct s.sneaker_id::text || ' ' || m.status || ' dur=' || m.dur_now || '/' || m.dur_max, ', ')
                                       from public.walk_sessions s join public.market_sneakers m on m.id = s.sneaker_id where s.user_id = t.u(72)));
  perform t.say('u72 판정', (select string_agg(distinct verdict || ':' || verdict_reason, ' | ') from public.walk_sessions where user_id = t.u(72)));
  perform t.say('u72 첫 신발(대상)', (select format('Lv%s xp=%s dur=%s/%s', level, forge_xp, dur_now, dur_max) from public.market_sneakers where id = v_starter));
end $$;
