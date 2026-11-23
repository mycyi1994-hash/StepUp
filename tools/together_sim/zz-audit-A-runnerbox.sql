-- 감사 A — 상자 수도꼭지(하루 최대 SUP 밖) · 러너 레벨 상자(0121) · 상자 기대값
--
-- (가) 오늘 만든 새 계정이 지난 7일 러닝(하루 48,000보)을 한 번에 올린다 — 러너 레벨 상자 · 러닝 상자 · 주간 · 연속 상자가
--      올린 날 하나에 몰린다(SUP 은 올린 날 상한 upload_daily_cap 이 막지만 상자에는 그런 상한이 없다).
-- (나) 상자 기대값 — 일반 · 상급 상자를 많이 열어 강화 경험치 값(forge_xp_value · 대성공 기대)으로 SUP 로 바꿔 본다(1,000 경험치 = 24 SUP).
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
select t.mk(61, 3000);
update public.profiles set created_at = now() - interval '1 hour' where id = t.u(61);

do $$
declare
  v_today date := economy.game_day(now());
  d int;
  v_sup numeric;
  v_free int; v_gift int;
begin
  for d in reverse 6 .. 0 loop
    perform t.say('upload day-' || d, t.rec(61, economy.game_day_start(v_today - d) + interval '2 hours', 17280, 48000));
    perform t.say('   runner level / energy_max now', (select r.level || ' / ' || economy.energy_max(t.u(61)) from economy.runner_levels r where r.user_id = t.u(61)));
  end loop;
  select coalesce(sum(l.amount), 0) into v_sup from public.sup_ledger l where l.user_id = t.u(61) and l.amount > 0;
  perform t.say('SUP credited in this one sitting', v_sup);
  perform t.say('box awards by source', (select string_agg(x.k || '=' || x.n, ' ' order by x.k) from (
      select regexp_replace(a.source, '[:0-9-]+$', '') || '/' || a.kind as k, count(*) as n from economy.box_awards a where a.user_id = t.u(61) group by 1) x));
  select coalesce(sum(g.granted - g.used) filter (where g.kind = 'FREE'), 0), coalesce(sum(g.granted - g.used) filter (where g.kind = 'GIFT'), 0)
    into v_free, v_gift from public.draw_grants g where g.user_id = t.u(61);
  perform t.say('draw_grants FREE / GIFT (+1 attendance FREE/day not counted)', v_free || ' / ' || v_gift);
  perform t.say('runner_me', (select jsonb_build_object('level', r.level, 'steps', r.steps, 'boxed_to', r.boxed_to) from economy.runner_levels r where r.user_id = t.u(61)));

  -- 상자 기대값(서버 값): 등급 가중치 × 3.1^등급 × 1,000 경험치 × (1 + 대성공 확률 × (배수 − 1)) × 24 SUP / 1,000
  perform t.say('EV FREE box (SUP-eq via forge XP)', (select round(sum(economy.draw_weight_v3('FREE', r) / 1000.0 * power(economy.fx('forge_xp_step', 3.1), economy.rarity_ord(r)))
        * (1 + economy.fx('forge_xp_crit_chance', 0.1) * (economy.fx('forge_xp_crit_mult', 2) - 1)) * economy.fx('forge_xp_sup_per_unit', 24), 2)
      from unnest(array['COMMON','RARE','EPIC','LEGENDARY','REDLINE','FINISH']) r));
  perform t.say('EV GIFT box (SUP-eq via forge XP)', (select round(sum(economy.draw_weight_v3('PREMIUM', r) / 1000.0 * power(economy.fx('forge_xp_step', 3.1), economy.rarity_ord(r)))
        * (1 + economy.fx('forge_xp_crit_chance', 0.1) * (economy.fx('forge_xp_crit_mult', 2) - 1)) * economy.fx('forge_xp_sup_per_unit', 24), 2)
      from unnest(array['COMMON','RARE','EPIC','LEGENDARY','REDLINE','FINISH']) r));
  perform t.say('box prices FREE / GIFT', economy.setting_num('box_price') || ' / ' || economy.setting_num('premium_box_price'));
  perform t.say('this sitting, SUP-eq at shop price', v_free * economy.setting_num('box_price') + v_gift * economy.setting_num('premium_box_price'));
end $$;

-- (나) 열어 본다 — 사용자 62 에게 일반 300 · 상급 120 을 주고 연다(경험치 값의 평균)
select t.mk(62);
insert into public.draw_grants (user_id, kind, granted) values (t.u(62), 'FREE', 300), (t.u(62), 'GIFT', 120)
on conflict (user_id, kind) do update set granted = excluded.granted;
do $$
declare i int; v bigint; v_ids_f bigint[] := '{}'; v_ids_g bigint[] := '{}';
begin
  perform t.login(62);
  for i in 1 .. 301 loop v := public.draw_free(); v_ids_f := v_ids_f || v; end loop;   -- 오늘 몫 1 + 받은 300
  for i in 1 .. 120 loop v := public.premium_draw(); v_ids_g := v_ids_g || v; end loop;
  perform t.say('FREE opened: rarity mix', (select string_agg(r || '=' || n, ' ' order by o) from (
      select s.rarity r, economy.rarity_ord(s.rarity) o, count(*) n from public.market_sneakers s where s.id = any(v_ids_f) group by 1, 2) x));
  perform t.say('FREE opened: mean SUP-eq (own XP × 1.1 crit EV × 24/1000)', (select round(avg((economy.forge_xp_value(s)).own) * 1.1 * 24 / 1000, 2)
      from public.market_sneakers s where s.id = any(v_ids_f)));
  perform t.say('GIFT opened: rarity mix', (select string_agg(r || '=' || n, ' ' order by o) from (
      select s.rarity r, economy.rarity_ord(s.rarity) o, count(*) n from public.market_sneakers s where s.id = any(v_ids_g) group by 1, 2) x));
  perform t.say('GIFT opened: mean SUP-eq', (select round(avg((economy.forge_xp_value(s)).own) * 1.1 * 24 / 1000, 2)
      from public.market_sneakers s where s.id = any(v_ids_g)));
  -- 산 상자를 팔 길이 있나(시세 · 체인) — 거래 멈춤 · 신발 체인 멈춤
  perform t.say('market_trading_enabled / shoe_chain_enabled', economy.setting('market_trading_enabled')::text || ' / ' || coalesce(economy.setting('shoe_chain_enabled')::text, 'null'));
end $$;
