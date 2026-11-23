-- 감사 D: 예전 앱(v1.17.1)이 부르는 market_import(0007)로 IMPORT 신발의 레벨을 올리면 box_on_level(0116)이 상급 상자를 준다
-- (0116 의 한 번 채우기는 IMPORT · MINT 를 뺐지만 트리거는 빼지 않는다)
select t.mk(1);
do $$
declare v bigint; v2 bigint; g_before int; g_after int; s public.market_sneakers;
begin
  perform t.login(1);
  select coalesce(sum(granted), 0) into g_before from public.draw_grants where user_id = t.u(1) and kind = 'GIFT';
  v := public.market_import(9001, 'NONE', 'COMMON', 0, 1, 0, 0, 100);
  select * into s from public.market_sneakers where id = v;
  perform t.say('import 1', format('id=%s origin=%s rarity=%s level=%s dur=%s/%s', s.id, s.origin, s.rarity, s.level, s.dur_now, s.dur_max));
  -- 같은 폰 신발을 레벨 30 으로 다시 알린다(예전 앱이 강화한 폰 신발을 다시 올릴 때와 같은 부름)
  v2 := public.market_import(9001, 'NONE', 'COMMON', 0, 30, 0, 0, 100);
  select * into s from public.market_sneakers where id = v;
  select coalesce(sum(granted), 0) into g_after from public.draw_grants where user_id = t.u(1) and kind = 'GIFT';
  perform t.say('after re-import level 30', format('level=%s pts_respec=%s', s.level, s.pts_respec));
  perform t.say('GIFT(상급) boxes before → after', g_before || ' → ' || g_after);
  perform t.say('box_awards LEVEL:*', (select string_agg(source, ',' order by source) from economy.box_awards where user_id = t.u(1) and source like 'LEVEL:%'));
  -- 상급 상자를 실제로 쓸 수 있다
  v := public.premium_draw();
  perform t.say('premium_draw ok — draw id / GIFT used', v || ' / ' || (select used from public.draw_grants where user_id = t.u(1) and kind = 'GIFT'));
  -- 다른 폰 신발 29개를 더 올릴 수 있다(상한 30) — 상자는 계정마다 레벨마다 한 번이라 여기서는 더 안 늘어난다
end $$;
