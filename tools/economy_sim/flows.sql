-- 흐름 사고실험 — tools/economy_sim/run.sh 가 부른다. 실제 서버 함수로 "막히는 곳"을 재현해 한 줄씩 적는다.
--   1) 새 사용자 첫날: 첫 신발로 걷기 10분 → 수리할 돈이 되는가 · 0% 신발로 걸으면 · 새 신발로 30분을 한 번에 걸으면
--   2) 레벨 높은 신발로 달린 뒤 레벨 낮은 새 신발로 갈아 신으면(에너지 최대치는 신은 신발 레벨을 따른다) · 에너지 셀 값
\set ON_ERROR_STOP on
set client_min_messages = notice;
create or replace function pg_temp.track(p_start timestamptz, p_secs int, p_dps numeric)
returns text language sql as $$
  select string_agg(format('%s,%s,%s', (37.5 + i * p_dps)::numeric(12, 6), 127.000000,
      (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
  from generate_series(0, p_secs) i $$;
do $$
declare v_user uuid := gen_random_uuid(); v_t timestamptz; r record; s public.market_sneakers; v_id bigint; i int; v_new bigint;
begin
  insert into auth.users (id, email) values (v_user, 'new@sim.test');
  perform set_config('request.jwt.claim.sub', v_user::text, false);
  perform public.economy_bootstrap();
  select * into s from public.market_sneakers where owner_id = v_user;
  raise notice '[가입] 첫 신발 % Lv% 내구도 %/% 효율 %bps, 잔고 %, 에너지 %', s.rarity, s.level, s.dur_now, s.dur_max, s.efficiency_bps, economy.balance_of(v_user), economy.energy_max(v_user);
  v_t := now() - interval '6 hours';
  -- 1) 첫 러닝: 걷기 10분
  select * into r from public.record_session(v_t, v_t + interval '600 seconds', 1100, 600, pg_temp.track(v_t, 600, 0.000012477), 0, 1, '', false, s.id);
  select * into s from public.market_sneakers where id = s.id;
  raise notice '[걷기 10분] +% SUP (%) → 잔고 %, 내구도 %/%', r.points_awarded, r.verdict, r.balance, s.dur_now, s.dur_max;
  begin
    perform public.sneaker_repair(s.id);
    raise notice '[수리] 됨 — % SUP', r.balance - economy.balance_of(v_user);
  exception when others then
    raise notice '[수리 시도] 거절 — % (%)', sqlerrm, sqlstate;
  end;
  -- 2) 같은 신발로 또 걷기 10분(수리가 거절됐으면 0% 인 채로)
  v_t := v_t + interval '15 minutes';
  select * into r from public.record_session(v_t, v_t + interval '600 seconds', 1100, 600, pg_temp.track(v_t, 600, 0.000012477), 0, 1, '', false, s.id);
  select * into s from public.market_sneakers where id = s.id;
  raise notice '[같은 신발로 걷기 10분 더] +% SUP (% · %) 내구도 %/%', r.points_awarded, r.verdict, r.verdict_reason, s.dur_now, s.dur_max;
  -- 3) 30분 한 번에(새 신발로) — 무료 뽑기
  v_new := public.draw_free();
  select * into s from public.market_sneakers where id = v_new;
  raise notice '[무료 뽑기] % 내구도 %/%', s.rarity, s.dur_now, s.dur_max;
  v_t := v_t + interval '15 minutes';
  select * into r from public.record_session(v_t, v_t + interval '1800 seconds', 3300, 1800, pg_temp.track(v_t, 1800, 0.000012477), 0, 1, '', false, v_new);
  select * into s from public.market_sneakers where id = v_new;
  raise notice '[새 신발로 걷기 30분 한 번에] +% SUP (% · %) 내구도 %/%, 잔고 %, 남은 에너지 %', r.points_awarded, r.verdict, r.verdict_reason, s.dur_now, s.dur_max, r.balance,
    economy.energy_left(v_user, economy.game_day(v_t));
end $$;

do $$
declare v_user uuid := gen_random_uuid(); v_t timestamptz; r record; s public.market_sneakers; v_hi bigint; v_lo bigint; v_day date; v_b numeric;
begin
  insert into auth.users (id, email) values (v_user, 'lv10@sim.test');
  update public.profiles set created_at = now() - interval '30 days' where id = v_user;
  perform set_config('request.jwt.claim.sub', v_user::text, false);
  insert into public.sup_ledger (user_id, kind, amount, description) values (v_user, 'EARN_EVENT', 1000, 'sim');
  insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability, origin, efficiency_bps, comfort_bps, equipped, dur_base, pts_eff)
  values (v_user, 'NONE', 'RARE', 0, 10, 100, 'FREE_DRAW', 500, 450, true, 75, 18) returning id into v_hi;
  insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability, origin, efficiency_bps, comfort_bps, equipped, dur_base)
  values (v_user, 'NONE', 'COMMON', 0, 1, 100, 'FREE_DRAW', 300, 150, false, 50) returning id into v_lo;
  v_t := now() - interval '8 hours'; v_day := economy.game_day(v_t);
  raise notice '[Lv10 신발 신음] 에너지 최대 % · 남음 %', economy.energy_max(v_user), economy.energy_left(v_user, v_day);
  -- Lv10 신발로 15분 달리기 ×4 (수리하며) = 에너지 16 쯤 사용
  for i in 1..4 loop
    select * into r from public.record_session(v_t, v_t + interval '900 seconds', 2475, 900, pg_temp.track(v_t, 900, 0.000023706), 0, 1, '', false, v_hi);
    perform public.sneaker_repair(v_hi);
    v_t := v_t + interval '20 minutes';
  end loop;
  raise notice '[Lv10 신발로 60분 달림] 남은 에너지 % / %', economy.energy_left(v_user, v_day), economy.energy_max(v_user);
  perform public.sneaker_equip(v_lo);
  raise notice '[Lv1 새 신발로 갈아 신음] 남은 에너지 % / %', economy.energy_left(v_user, v_day), economy.energy_max(v_user);
  select * into r from public.record_session(v_t, v_t + interval '600 seconds', 1650, 600, pg_temp.track(v_t, 600, 0.000023706), 0, 1, '', false, v_lo);
  select * into s from public.market_sneakers where id = v_lo;
  raise notice '[Lv1 신발로 달리기 10분] +% SUP (% · %) — 신발 내구도 %/%', r.points_awarded, r.verdict, r.verdict_reason, s.dur_now, s.dur_max;
  perform public.sneaker_equip(v_hi);
  raise notice '[다시 Lv10 신발] 남은 에너지 % / %', economy.energy_left(v_user, v_day), economy.energy_max(v_user);
  -- 부스터: 에너지 셀
  v_b := economy.balance_of(v_user);
  begin
    perform public.boost_buy('ENERGY_CELL');
    raise notice '[에너지 셀] % SUP 내고 에너지 +2(적립 걸음 1,200보) — 남은 에너지 %', v_b - economy.balance_of(v_user), economy.energy_left(v_user, economy.game_day(now()));
  exception when others then raise notice '[에너지 셀] 거절 — % (%)', sqlerrm, sqlstate; end;
end $$;
