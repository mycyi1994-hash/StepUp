-- 감사 C(2026-11-22): 러너 레벨(0121)을 켜면 Lv10 이상 러너에게 모든 강화 재료가 "아까운 재료 — 가장 높은 레벨 신발(TOP_LEVEL)"로 찍힌다.
-- economy.forge_xp_precious(0118) 는 "이 재료를 태운 뒤 하루 에너지"를 economy.forge_account_after → energy_max_for_level(가진 신발 최고 레벨)로 셈하고
-- (0119 부터 energy_per_level 0 → 늘 10) 그것을 economy.energy_max(0121 — 러너 레벨, 10.45 ~ 16.5)와 견준다. 10 < 16.5 → 모든 재료가 TOP_LEVEL.
-- 앱의 "남는 신발 넣기"(ShoeForge.kt bulkPick)는 아까운 재료를 고르지 않으므로 아무것도 넣지 않는다. 견적의 energy before/after 도 10 → 10.
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;
select t.mk(n) from generate_series(1, 2) n;
do $$
declare n int; i int; v_t bigint; v_m bigint[]; j jsonb; q jsonb; v_top int; v_all int; d int;
begin
  -- u1: 지난 러닝 20만 보(2주 전, 하루 4만 × 5일 — CLEAN) → 러너 Lv21 · 하루 에너지 16.5. u2: 새 계정(러너 Lv1 · 5.5)
  for d in 10 .. 14 loop
    insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verdict, reward_steps, backed_steps, gps_backed, rewarded_steps)
    values (t.u(1), now() - make_interval(days => d), now() - make_interval(days => d) + interval '4 hours', 14400, 40000, 'CLEAN', 40000, 40000, true, 0);
  end loop;
  foreach n in array array[1, 2] loop
    perform t.login(n);
    v_t := economy.draw_create(t.u(n), 'PAID_DRAW');
    update public.market_sneakers set rarity = 'RARE', level = 3, pts_eff = 0, pts_comfort = 0, pts_dur = 0 where id = v_t;
    v_m := '{}';
    for i in 1 .. 3 loop
      v_m := v_m || economy.draw_create(t.u(n), 'PAID_DRAW');
    end loop;
    update public.market_sneakers set rarity = 'COMMON', level = 1, pts_eff = 0, pts_comfort = 0, pts_dur = 0, dur_now = dur_max where id = any(v_m);
    j := public.forge_xp_materials(v_t);
    select count(*) filter (where x -> 'precious' ? 'TOP_LEVEL'), count(*) into v_top, v_all
      from jsonb_array_elements(j -> 'items') x where (x ->> 'id')::bigint = any(v_m);
    q := public.forge_xp_quote(v_t, v_m[1:1]);
    perform t.say(format('u%s 러너 Lv%s · 하루 에너지 %s', n, (select level from economy.runner_levels where user_id = t.u(n)), economy.energy_max(t.u(n))),
      format('일반 재료 %s켤레 가운데 TOP_LEVEL(가장 높은 레벨 신발 — 태우면 에너지가 준다)으로 찍힌 것 %s · 견적의 하루 에너지 %s → %s',
             v_all, v_top, q -> 'energy' ->> 'before', q -> 'energy' ->> 'after'));
    if n = 1 then
      perform t.chk('러너 Lv21: 일반 재료(레벨 1 · 대상은 레어 Lv3)는 가장 높은 레벨 신발이 아니다', v_top = 0,
                    format('%s / %s 켤레가 TOP_LEVEL — 앱의 "남는 신발 넣기"는 아까운 재료를 고르지 않아 아무것도 넣지 않는다', v_top, v_all));
      perform t.chk('견적의 하루 에너지는 지금 하루 에너지(러너 레벨)와 같아야', (q -> 'energy' ->> 'before')::numeric = economy.energy_max(t.u(n)),
                    format('견적 %s · 실제 %s', q -> 'energy' ->> 'before', economy.energy_max(t.u(n))));
    end if;
  end loop;
end $$;
