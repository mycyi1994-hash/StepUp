-- 감사 B: 크루 효율 8 : 2 의 평균은 사람 수로만 센다(걸음 · 받은 SUP 와 상관없다).
-- 셋(약한 신발 둘 1.4 + 셋째) — 셋째는 10분(1,650보)만 달리고 끝낸다, 약한 둘은 60분(9,900보). 셋째만 바꿔 본다:
--   (A) 셋째도 약한 신발 1.4 — 견줄 값
--   (B) 셋째가 피니시 꼭대기 신발(효율 +126,688bps = × 13.67)
--   (C) (B) 와 같은데 셋째가 "보류" 계정(지난 7일 무효 5번 — 자기 적립 0) — 잃는 것이 없다
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
create or replace function t.run25(p_party bigint, p_users int[], p_secs int) returns void language plpgsql as $$
declare i int; m int; v jsonb;
begin
  for i in 1 .. p_secs / 25 loop
    perform t.age(interval '25 seconds');
    foreach m in array p_users loop perform t.login(m); v := public.crew_run_beat(p_party, 'RUNNING'); end loop;
  end loop;
end $$;
create or replace function t.scene(k int, a int, b int, s int, s_eff int, s_held boolean) returns text language plpgsql as $$
declare p bigint; v jsonb; v_start timestamptz; n int; i int; w public.walk_sessions; c economy.crew_runs; d text := ''; tot numeric := 0;
begin
  perform t.mk(a, 150, 4000); perform t.mk(b, 150, 4000); perform t.mk(s, 1000, s_eff);
  if s_held then
    -- 지난 7일 무효 5번(가짜 위치) — 이 계정의 러닝은 보류(적립 0)
    perform t.login(s);
    for i in 1 .. 5 loop
      perform public.record_session(now() - make_interval(days => i), now() - make_interval(days => i) + interval '10 minutes', 1000, 600, '', 0, 1, '', true);
    end loop;
  end if;
  perform t.crew(k, a, array[b, s], 1);
  perform t.login(a);
  p := public.party_create_v2(t.c(k), 'blend', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  foreach n in array array[b, s] loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(a); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run25(p, array[a, b, s], 600);
  select started_at into v_start from economy.crew_runs where user_id = t.u(a);
  perform t.login(s); v := public.crew_run_beat(p, 'ENDED');
  perform t.rec(s, v_start, 600, 1650);
  perform t.run25(p, array[a, b], 3000);
  foreach n in array array[a, b] loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  select started_at into v_start from economy.crew_runs where user_id = t.u(a);
  perform t.rec(a, v_start, 3600, 9900);
  perform t.rec(b, v_start, 3600, 9900);
  foreach n in array array[a, b, s] loop
    select * into w from public.walk_sessions x where x.user_id = t.u(n) and x.party_id = p;
    select * into c from economy.crew_runs x where x.user_id = t.u(n) and x.party_id = p;
    d := d || format('u%s %s SUP(효율 %s · %s명 · 판정 %s) ', n, w.points_awarded, round(w.together_base / nullif(w.together_unit, 0), 3) || '→' ||
                     round((c.base_sup) / nullif(w.together_unit, 0), 3), c.bonus_count, w.verdict);
    tot := tot + w.points_awarded;
  end loop;
  return d || format('· 셋 합 %s', tot);
end $$;
do $$
begin
  perform t.say('(A) 셋째 약한 신발', t.scene(1, 101, 102, 103, 4000, false));
  perform t.say('(B) 셋째 피니시 신발', t.scene(2, 111, 112, 113, 126688, false));
  perform t.say('(C) 셋째 피니시 · 보류 계정', t.scene(3, 121, 122, 123, 126688, true));
end $$;
