-- 확인: 목표를 그날 걸은 만큼으로 바꿔 가며 받기 · 가장 높은 레벨 신발을 태우면 에너지 · 상한이 줄어드는가
-- 0117 까지: 낮춘 목표로 받았다. 0118 뒤: 목표는 20,000 까지 · 걸은 뒤 바꾼 목표는 다음 날부터(오늘은 그대로라 못 받는다) ·
-- 에너지 · 상한은 계정 전체 그대로(강화 견적이 태우기 전 앞뒤 값을 보인다)
select t.mk(1), t.mk(2);
do $$
declare v_start timestamptz := now() - interval '3 hours'; v numeric; a bigint; b bigint;
begin
  -- 1: 아침에 목표 30,000 → 12,400보만 달림 → 밤에 목표를 12,000 으로 낮추고 받기
  perform t.login(1); perform public.profile_set_daily_goal(30000);
  perform t.rec(1, v_start, 6000, 12400, true);
  begin v := public.goal_claim_day(economy.game_day(v_start)); perform t.say('목표 30,000 그대로 받기', v);
  exception when others then perform t.say('목표 30,000 그대로 받기', sqlerrm); end;
  perform public.profile_set_daily_goal(12000);
  begin v := public.goal_claim_day(economy.game_day(v_start)); perform t.say('목표를 12,000 으로 낮춘 뒤 받은 보너스(SUP)', v);
  exception when others then perform t.say('목표를 12,000 으로 낮춘 뒤 받기', sqlerrm); end;
  perform t.say('  오늘 목표 · 내일부터', format('%s · %s', public.box_status() ->> 'goal', public.box_status() ->> 'goal_next'));
  -- 2: Lv10 신발(가장 높은 레벨)을 재료처럼 태우면
  perform t.login(2);
  a := (select id from public.market_sneakers where owner_id = t.u(2) and equipped);
  b := economy.draw_create(t.u(2), 'PAID_DRAW');
  update public.market_sneakers set level = 10, equipped = false where id = b;
  perform t.say('Lv10 신발이 있을 때 하루 에너지 · 상한 배수', format('%s · %s', economy.energy_max(t.u(2)), round(economy.earn_cap_factor(t.u(2)), 2)));
  update public.market_sneakers set owner_id = null, status = 'BURNED' where id = b;
  perform t.say('그 신발을 태운 뒤', format('%s · %s', economy.energy_max(t.u(2)), round(economy.earn_cap_factor(t.u(2)), 2)));
end $$;
