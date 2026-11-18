-- 하루 플레이 시뮬레이션 — tools/economy_sim/run.sh 가 부른다.
--
-- 등급 · 레벨마다 새 계정을 하나 만들고, 그 신발 하나로 어제(한국 날짜) 하루를 산다:
--   에너지가 남아 있는 동안 [남은 내구도만큼 달리기(record_session) → 다 닳으면 가득 수리(sneaker_repair)]를 되풀이하고,
--   끝에 한 번 더 가득 수리해 그날 닳은 값을 모두 치른다.
-- 걷기 = 분당 110걸음 · 시속 5km, 달리기 = 분당 165걸음 · 시속 9.5km. 효율 · 착화감 · 내구도는 등급 범위의 가운데,
-- 레벨업 포인트는 모두 효율에. 계정은 가입 30일(새 계정 절반 상한 밖), 잔고는 넉넉히 준다(수리가 막히지 않게).
-- earned = 그날 적립, repair = 그날 수리비, net = 남는 돈. reasons = 서버가 적은 까닭(상한에 걸렸는지 본다).
\set ON_ERROR_STOP on
set client_min_messages = warning;
create or replace function pg_temp.track(p_start timestamptz, p_secs int, p_dps numeric)
returns text language sql as $$
  select string_agg(format('%s,%s,%s', (37.5 + i * p_dps)::numeric(12, 6), 127.000000,
      (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
  from generate_series(0, p_secs) i
$$;
create temp table sim_out (rarity text, lvl int, mode text, energy numeric, eff_bps int, steps int, minutes int,
  earned numeric, repair numeric, sessions int, repairs int, reasons text);
do $$
declare
  v_rar text; v_lvl int; v_mode text; v_user uuid; v_shoe bigint; v_t timestamptz; v_day date;
  v_spm int; v_dps numeric; v_e numeric; v_mins int; v_need int; r record; s public.market_sneakers;
  v_earned numeric; v_repair numeric; v_b0 numeric; v_b1 numeric; v_sess int; v_reps int; v_steps int; v_min_total int;
  v_lo int; v_hi int; v_elo int; v_ehi int; v_clo int; v_chi int; v_reasons text; v_guard int;
begin
  foreach v_rar in array array['COMMON','RARE','EPIC','LEGENDARY','REDLINE','FINISH'] loop
    foreach v_lvl in array array[1,5,10,15,20,26,30] loop
      continue when v_lvl > economy.max_level(v_rar);
      foreach v_mode in array array['WALK','RUN'] loop
        v_user := gen_random_uuid();
        insert into auth.users (id, email) values (v_user, v_user::text || '@sim.test');
        update public.profiles set created_at = now() - interval '30 days' where id = v_user;
        insert into public.sup_ledger (user_id, kind, amount, description) values (v_user, 'EARN_EVENT', 1000000, 'sim');
        select lo, hi into v_lo, v_hi from economy.durability_range(v_rar);
        select lo, hi into v_elo, v_ehi from economy.efficiency_range(v_rar);
        select lo, hi into v_clo, v_chi from economy.comfort_range(v_rar);
        update public.market_sneakers set equipped = false where owner_id = v_user;
        insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability, origin,
            efficiency_bps, comfort_bps, equipped, dur_base, pts_eff)
        values (v_user, 'NONE', v_rar, 0, v_lvl, 100, 'FREE_DRAW', (v_elo+v_ehi)/2, least((v_clo+v_chi)/2, economy.comfort_cap_bps()), true,
            (v_lo+v_hi)/2, economy.points_total_of('FREE_DRAW', v_rar, v_lvl))
        returning id into v_shoe;
        perform set_config('request.jwt.claim.sub', v_user::text, false);
        v_t := ((date_trunc('day', now() at time zone 'Asia/Seoul') - interval '1 day') at time zone 'Asia/Seoul') + interval '10 minutes';
        v_day := economy.game_day(v_t);
        if v_mode = 'WALK' then v_spm := 110; v_dps := 0.000012477; else v_spm := 165; v_dps := 0.000023706; end if;
        v_earned := 0; v_repair := 0; v_sess := 0; v_reps := 0; v_steps := 0; v_min_total := 0; v_reasons := ''; v_guard := 0;
        loop
          v_guard := v_guard + 1; exit when v_guard > 400;
          v_e := economy.energy_left(v_user, v_day);
          exit when v_e < 0.05;
          select * into s from public.market_sneakers where id = v_shoe;
          if s.dur_now < 5 then
            v_b0 := economy.balance_of(v_user);
            perform public.sneaker_repair(v_shoe);
            v_repair := v_repair + v_b0 - economy.balance_of(v_user); v_reps := v_reps + 1;
            select * into s from public.market_sneakers where id = v_shoe;
          end if;
          v_need := ceil(v_e * 600 / (1 - (select comfort_bps from economy.shoe_effective(s)) / 10000.0) / v_spm)::int;
          v_mins := least(s.dur_now / 5, v_need);
          exit when v_mins < 1;
          select * into r from public.record_session(v_t, v_t + make_interval(secs => v_mins * 60), v_mins * v_spm, v_mins * 60,
              pg_temp.track(v_t, v_mins * 60, v_dps), 0, 1, '', false, v_shoe);
          v_earned := v_earned + r.points_awarded; v_sess := v_sess + 1; v_steps := v_steps + v_mins * v_spm; v_min_total := v_min_total + v_mins;
          if r.verdict <> 'CLEAN' or coalesce(r.verdict_reason, '') <> '' then
            if position(coalesce(r.verdict_reason, r.verdict) in v_reasons) = 0 then
              v_reasons := v_reasons || '[' || r.verdict || ':' || coalesce(r.verdict_reason, '') || ']';
            end if;
          end if;
          v_t := v_t + make_interval(secs => v_mins * 60 + 90);
          exit when r.points_awarded = 0;
        end loop;
        select * into s from public.market_sneakers where id = v_shoe;
        if s.dur_now < s.dur_max then
          v_b0 := economy.balance_of(v_user);
          perform public.sneaker_repair(v_shoe);
          v_repair := v_repair + v_b0 - economy.balance_of(v_user); v_reps := v_reps + 1;
        end if;
        insert into sim_out values (v_rar, v_lvl, v_mode, economy.energy_max(v_user),
          (select efficiency_bps from economy.shoe_effective(s)), v_steps, v_min_total, v_earned, v_repair, v_sess, v_reps, v_reasons);
      end loop;
    end loop;
  end loop;
end $$;
select rarity, lvl, mode, energy, eff_bps, steps, minutes as min, round(earned,1) as earned, round(repair,1) as repair,
       round(earned-repair,1) as net, sessions as runs, repairs as reps, reasons
  from sim_out order by economy.rarity_ord(rarity), lvl, mode desc;
