-- 감사 C(2026-11-22): 러너 레벨 상자(0121)가 강화 속도의 약속(레어 만렙 24일 · 에픽 119일 ±20%, pace.py --check — CI)을 조용히 깬다.
-- pace.py 의 하루 상자는 출석 1 + 러닝 2 · 주마다 상급 2 뿐이고 러너 상자(레벨마다 보통 1 · 5레벨마다 상급 1, Lv30 까지 29 + 6)를 모른다.
-- 러너 걸음은 에너지를 다 쓴 뒤의 걸음도 세고(그 걸음은 SUP 0 · 신발도 닳지 않는다) 하루 48,000 까지라, 새 계정이 6일 걸으면 Lv29 · 상자 33개.
-- 아래: 새 계정이 6일 동안 날마다 걷기 110분 × 4번(12,000보씩 = 하루 48,000) — 실제 record_session 으로.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;
insert into auth.users (id, email) values (t.u(81), 'new81@test.invalid');
do $$
declare d int; k int; v_t timestamptz; r record; s0 public.market_sneakers; s1 public.market_sneakers; v_sup numeric := 0; v_wear int := 0;
        v_day_sup numeric; v_day_wear int; v_free int; v_gift int; v_lv int; v_need bigint; v_xp numeric;
begin
  perform t.login(81);
  perform public.economy_bootstrap();
  update public.profiles set created_at = now() - interval '8 days' where id = t.u(81);
  for d in reverse 6 .. 1 loop
    v_day_sup := 0; v_day_wear := 0;
    for k in 0 .. 3 loop
      select * into s0 from public.market_sneakers where owner_id = t.u(81) and equipped;
      v_t := economy.game_day_start(economy.game_day(now()) - d) + make_interval(hours => 6 + k * 2);
      select * into r from public.record_session(v_t, v_t + interval '110 minutes', 12000, 6600,
                                                 t.track(v_t, 6600, 0.000012477), 0, 1, '', false, s0.id);
      select * into s1 from public.market_sneakers where id = s0.id;
      v_day_sup := v_day_sup + r.points_awarded; v_day_wear := v_day_wear + (s0.dur_now - s1.dur_now);
    end loop;
    v_sup := v_sup + v_day_sup; v_wear := v_wear + v_day_wear;
    select level into v_lv from economy.runner_levels where user_id = t.u(81);
    perform t.say(format('%s일째', 7 - d), format('걸음 48,000 · 이날 SUP %s · 신발이 닳은 양 %s · 러너 Lv%s · 하루 에너지 %s · 러너 상자 보통 %s · 상급 %s',
      round(v_day_sup, 1), v_day_wear, v_lv, economy.energy_max(t.u(81)),
      (select count(*) from economy.box_awards b where b.user_id = t.u(81) and b.source like 'RUNNER:%'),
      (select count(*) from economy.box_awards b where b.user_id = t.u(81) and b.source like 'RUNNER5:%')));
  end loop;
  v_free := (select count(*) from economy.box_awards b where b.user_id = t.u(81) and b.source like 'RUNNER:%');
  v_gift := (select count(*) from economy.box_awards b where b.user_id = t.u(81) and b.source like 'RUNNER5:%');
  -- 상자 한 개의 기대 경험치(제 값, 스펙 가운데) — 뽑기 확률(pace.py 와 같은 값: 보통 79 · 20 · 1%, 상급 60 · 28 · 9 · 2.5 · 0.5%)
  v_xp := v_free * (0.79 * 1000 + 0.20 * 3100 + 0.01 * 9610) + v_gift * (0.60 * 3100 + 0.28 * 9610 + 0.09 * 29791 + 0.025 * 92352 + 0.005 * 286292);
  select sum(economy.forge_need('RARE', l)) into v_need from generate_series(1, 19) l;
  perform t.say('6일 합', format('SUP %s · 신발이 닳은 양 %s · 러너 상자 %s + %s = 상점 값 %s SUP · 기대 경험치 %s = 레어 만렙 통 합(%s)의 %s%%',
    round(v_sup, 1), v_wear, v_free, v_gift, v_free * economy.setting_num('box_price') + v_gift * economy.setting_num('premium_box_price'),
    round(v_xp), v_need, round(100 * v_xp / v_need)));
  perform t.chk('러너 상자는 pace.py 의 하루 상자 수급(출석 1 + 러닝 2 · 주 상급 2) 밖이다 — 6일치 기본 수급 18 + 상급 0~2 와 견준다',
    v_free + v_gift <= 6, format('6일 만에 러너 상자 %s개(보통 %s · 상급 %s) — 기본 수급의 %s배', v_free + v_gift, v_free, v_gift, round((v_free + v_gift) / 18.0, 1)));
end $$;
