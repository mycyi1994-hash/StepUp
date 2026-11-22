-- 점검 2 (J): 신발은 앱이 보낸 달린 초로만 닳는다(1분 5). 바닥은 케이던스 240 뿐 — 그래서
--   (가) 진짜로 51분 달리고 달린 초만 35분으로 보내면 닳음 31% 덜(경로 시각은 51분인데 서버는 맞대지 않는다)
--   (나) 걸음을 240/분으로 부풀려 35분 — 하루 에너지(8,400보)는 같고 닳음 · 수리비는 31% 덜, 보상 걸음은 1.45배
--   (다) 걸음을 GPS 비율보다 많이 보내도 FLAGGED 로 잘라 줄 뿐 무효 횟수(보류)에 들지 않는다 — 늘 최대로 보내면 된다
-- 하루 에너지 10 · 첫 신발(일반 · 효율 4000 · 착화감 2857 · 내구도 155 = 31분). 수리는 러닝 사이에 가득(수리비 = 닳은 점수 × 1점 값).
\ir zz-audit2-J-lib.sql
select t.mk0(n) from generate_series(41, 44) n;
select t.old(n) from generate_series(41, 44) n;
do $$
declare v_now timestamptz := date_trunc('minute', now()); v_start timestamptz; w public.walk_sessions; v_unit numeric;
        v_pts numeric; v_wear int; v_rs int; n int; v_rep numeric;
begin
  select economy.repair_cost_per_unit(s.rarity, s.level) into v_unit from public.market_sneakers s where s.owner_id = t.u(41) and s.equipped;
  -- 정직: 51분 · 165/분(8,415보) · 9.5km/h — 신발이 31분에 닳으니 31분 + 수리 + 20분 두 번
  v_pts := 0; v_wear := 0; v_rs := 0;
  v_start := v_now - interval '5 hours';
  w := t.up(41, v_start, 1860, 5115, t.trku(41, v_start, 1860));
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u41 정직 1', t.line(w));
  update public.market_sneakers set dur_now = dur_max where id = w.sneaker_id;
  v_start := v_now - interval '4 hours';
  w := t.up(41, v_start, 1200, 3300, t.trku(41, v_start, 1200));
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u41 정직 2', t.line(w));
  v_rep := round(v_wear * v_unit, 2);
  perform t.say('u41 정직 하루', format('SUP %s · 닳음 %s점 · 수리비 %s · 남는 몫 %s · 보상 걸음 %s', v_pts, v_wear, v_rep, v_pts - v_rep, v_rs));

  -- (가) 같은 51분 · 같은 걸음 · 같은 경로를 달리고 달린 초만 낮춰 보낸다(31분 → 1,280초 · 20분 → 825초 — 케이던스 240 아래)
  v_pts := 0; v_wear := 0; v_rs := 0;
  v_start := v_now - interval '5 hours';
  w := t.up(42, v_start, 1860, 5115, t.trku(42, v_start, 1860), 1280);
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u42 (가) 1', t.line(w));
  update public.market_sneakers set dur_now = dur_max where id = w.sneaker_id;
  v_start := v_now - interval '4 hours';
  w := t.up(42, v_start, 1200, 3300, t.trku(42, v_start, 1200), 825);
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u42 (가) 2', t.line(w));
  v_rep := round(v_wear * v_unit, 2);
  perform t.say('u42 (가) 하루', format('SUP %s · 닳음 %s점 · 수리비 %s · 남는 몫 %s · 보상 걸음 %s', v_pts, v_wear, v_rep, v_pts - v_rep, v_rs));

  -- (나) 같은 빠르기로 35분만 달리고 걸음은 240/분: 31분 7,440보 + 수리 + 4분 960보
  v_pts := 0; v_wear := 0; v_rs := 0;
  v_start := v_now - interval '5 hours';
  w := t.up(43, v_start, 1860, 7440, t.trku(43, v_start, 1860));
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u43 (나) 1', t.line(w));
  update public.market_sneakers set dur_now = dur_max where id = w.sneaker_id;
  v_start := v_now - interval '4 hours';
  w := t.up(43, v_start, 240, 960, t.trku(43, v_start, 240));
  v_pts := v_pts + w.points_awarded; v_rs := v_rs + w.reward_steps; v_wear := v_wear + 155 - (select dur_now from public.market_sneakers where id = w.sneaker_id);
  perform t.say('u43 (나) 2', t.line(w));
  v_rep := round(v_wear * v_unit, 2);
  perform t.say('u43 (나) 하루', format('SUP %s · 닳음 %s점 · 수리비 %s · 남는 몫 %s · 보상 걸음 %s', v_pts, v_wear, v_rep, v_pts - v_rep, v_rs));

  -- (다) 걸음을 GPS 로 받쳐 주는 양의 두 배(케이던스 240 안)로 여섯 번 — 모두 FLAGGED · 잘림, 보류 없음
  for n in 1 .. 6 loop
    v_start := v_now - make_interval(days => n) - interval '3 hours';
    -- 30분 · 1.2 m/s(2,160m → 받쳐 주는 걸음 5,669) · 보낸 걸음 7,200
    w := t.up(44, v_start, 1800, 7200, t.trku(44, v_start, 1800, 1.2));
    if n in (1, 6) then perform t.say('u44 (다) ' || n, t.line(w)); end if;
  end loop;
  perform t.say('u44 (다) 무효 수 · 보류된 러닝', (select format('VOID %s · 보류 %s', count(*) filter (where verdict = 'VOID'),
                 count(*) filter (where verdict_reason like '%보류%')) from public.walk_sessions where user_id = t.u(44)));
end $$;
