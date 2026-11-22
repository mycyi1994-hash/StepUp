-- 감사 2 G — 예전 앱(sneaker_repair)의 "방금 무료 수리를 썼어요" 멈춤이 한 번이 아니라 2분 내내.
-- repair_core 는 guarded = true 로 적은 뒤 같은 함수에서 raise 한다 — 요청 전체가 되돌아가 guarded 가 남지 않는다.
-- 앱의 요청 하나 = 트랜잭션 하나: 여기서는 begin … exception 블록(되돌림 지점)으로 흉내 낸다.
select t.mk(1);
do $$
declare a bigint; b bigint; v jsonb; n_fail int := 0; i int; g boolean; bal0 numeric; bal1 numeric;
begin
  perform t.login(1);
  perform economy.ledger_apply(t.u(1), 'EARN_EVENT', 500, 'sim');
  select id into a from public.market_sneakers where owner_id = t.u(1) and equipped;
  b := economy.draw_create(t.u(1), 'FREE_DRAW');
  update public.market_sneakers set dur_now = 0 where id in (a, b);
  -- 새 앱이 무료 수리를 쓴다(신발 a)
  v := public.sneaker_repair_v2(a, null, gen_random_uuid(), null);
  perform t.say('무료 수리(새 앱)', v ->> 'free');
  bal0 := economy.balance_of(t.u(1));
  -- 예전 앱이 다른 신발 b 를 고친다 — 다섯 번 다시 눌러 본다
  for i in 1 .. 5 loop
    begin
      perform public.sneaker_repair(b, null, gen_random_uuid());
      perform t.say('예전 앱 수리 ' || i, '고침');
    exception when others then
      n_fail := n_fail + 1;
      perform t.say('예전 앱 수리 ' || i, sqlerrm);
    end;
  end loop;
  select guarded into g from economy.repair_free_uses where user_id = t.u(1);
  bal1 := economy.balance_of(t.u(1));
  raise notice '%', format('%s — 멈춘 횟수 %s / 5 (의도: 1) · guarded 남음 = %s · 잔고 %s → %s',
    case when n_fail = 1 then 'OK  ' else 'FAIL' end, n_fail, g, bal0, bal1);
end $$;
