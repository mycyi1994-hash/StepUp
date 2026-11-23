-- 확인: 새 사용자의 첫날 — 첫 신발(내구도 31분)로 기본 목표(6,000보)를 한 번에 채울 수 있는가
-- 2026-11-21(0118 까지): 달리기 37분 · 걷기 55분 모두 못 채웠다(신발이 먼저 닳아 그 뒤 걸음이 보상 걸음에서 빠졌다). docs/economy-roots-2026-11-21.md 뿌리 1.
-- 0119 부터: 닳은 뒤 걸음도 다 세고 적립은 30% — 둘 다 목표를 채운다(같은 장면이 tools/economy_sim/check.py 불변식 6 으로 CI 에 있다).
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
-- 새 사용자: 첫 신발 그대로
insert into auth.users (id, email) values (t.u(71), 'new71@test.invalid'), (t.u(72), 'new72@test.invalid'), (t.u(73), 'new73@test.invalid');
do $$
declare v_day date := economy.game_day(now()) - 1; v_d0 timestamptz := economy.game_day_start(economy.game_day(now()) - 1); n int; s public.market_sneakers; v_t timestamptz; r record; w public.walk_sessions; b jsonb; v_bal numeric; v_spm int; v_dps numeric; v_min int; v_label text;
begin
  foreach n in array array[71, 72, 73] loop
    perform t.login(n);
    perform public.economy_bootstrap();
    update public.profiles set created_at = now() - interval '1 day' where id = t.u(n);
    select * into s from public.market_sneakers where owner_id = t.u(n) and equipped;
    if n = 71 then
      perform t.say('첫 신발', format('%s Lv%s 내구도 %s/%s = %s분 · 효율 +%s%% · 하루 에너지 %s(= 적립 걸음 %s) · 기본 목표 %s · 시작 SUP %s',
        s.rarity, s.level, s.dur_now, s.dur_max, s.dur_max / 5, s.efficiency_bps / 100.0, economy.energy_max(t.u(n)), economy.energy_max(t.u(n)) * 600,
        (select daily_goal from public.profiles where id = t.u(n)), economy.balance_of(t.u(n))));
      perform t.say('가득 수리비', round(economy.repair_unit_of(s) * s.dur_max, 1));
    end if;
    -- 71: 걷기 55분(6,050보) 한 번에 · 72: 달리기 37분(6,105보) 한 번에 · 73: 걷기 30분 → 무료 수리 → 걷기 25분
    v_t := v_d0 + interval '9 hours';
    if n = 71 then v_spm := 110; v_dps := 0.000012477; v_min := 55; v_label := '걷기 55분 한 번에';
    elsif n = 72 then v_spm := 165; v_dps := 0.000023706; v_min := 37; v_label := '달리기 37분 한 번에';
    else v_spm := 110; v_dps := 0.000012477; v_min := 30; v_label := '걷기 30분'; end if;
    select * into r from public.record_session(v_t, v_t + make_interval(mins => v_min), v_min * v_spm, v_min * 60, t.track(v_t, v_min * 60, v_dps), 0, 1, '', false, s.id);
    select * into w from public.walk_sessions where id = r.session_id;
    perform t.say(v_label, format('걸음 %s → 적립 걸음 %s · 보상 걸음 %s · +%s SUP · [%s]', w.steps, w.rewarded_steps, w.reward_steps, r.points_awarded, r.verdict_reason));
    if n = 73 then
      v_bal := economy.balance_of(t.u(n));
      perform public.sneaker_repair(s.id);
      perform t.say('  수리(첫날 무료?)', format('%s SUP', v_bal - economy.balance_of(t.u(n))));
      v_t := v_d0 + interval '11 hours';
      select * into r from public.record_session(v_t, v_t + make_interval(mins => 25), 25 * v_spm, 25 * 60, t.track(v_t, 25 * 60, v_dps), 0, 1, '', false, s.id);
      select * into w from public.walk_sessions where id = r.session_id;
      perform t.say('  걷기 25분 더', format('걸음 %s → 적립 걸음 %s · 보상 걸음 %s · +%s SUP · [%s]', w.steps, w.rewarded_steps, w.reward_steps, r.points_awarded, r.verdict_reason));
    end if;
    select * into s from public.market_sneakers where id = s.id;
    perform t.say('  하루 끝', format('보상 걸음 %s / 목표 %s · 러닝 상자 %s개 · 목표 보너스 %s · 잔고 %s · 신발 내구도 %s/%s',
      economy.reward_steps_on(t.u(n), v_day), economy.goal_peek(t.u(n), v_day),
      (select count(*) from economy.box_awards a where a.user_id = t.u(n) and a.source like 'RUN%'),
      coalesce((select l.amount::text from public.sup_ledger l where l.user_id = t.u(n) and l.ref = 'goal:' || v_day), '없음'),
      economy.balance_of(t.u(n)), s.dur_now, s.dur_max));
  end loop;
end $$;
