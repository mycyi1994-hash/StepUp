-- 감사 A — 하루 최대 SUP(0120) 밖으로 새는 길 · 하루 최대에 닿으면 목표 · 연속 · 연속 상자 · 러너 걸음을 잃는다
--
-- 사용자 1: 엿새 동안 목표(6,000)를 채우고, 오늘(일곱째 날) 한 번 길게 달려 하루 최대 SUP 에 닿는다.
--   그 위에 코스 완주 보상(course_run_submit) · 주간 도전(step_surge) · 나이트 러너(night_quest)를 받는다 — 셋 다 day_earned 밖.
-- 사용자 3(대조): 같은 엿새 + 오늘 보통 러닝 — 목표 보너스(7일 연속) · 연속 상급 상자를 받는다.
-- 하루 최대에 닿게 하려고 사용자 1 의 오늘 에너지를 늘려 둔다(에너지 초기화권 · 크루 러닝 자리 — 0120 이 막으려던 "크루로 길게"와 같은 자리).
select t.mk(1, 3000); select t.mk(2); select t.mk(3, 3000);

do $$
declare
  v_today date := economy.game_day(now());
  d int;
  r text;
  v_trk text;
  v_course text;
  v_A timestamptz := economy.game_day_start(economy.game_day(now())) + interval '7 hours';   -- 오늘 07:00 KST
  v_B timestamptz := economy.game_day_start(economy.game_day(now()) - 1) + interval '20 hours'; -- 어제 20:00 KST
  v_led numeric;
  v_ev numeric;
begin
  perform t.say('today', v_today);
  -- 엿새 전 ~ 이틀 전: 6,500보 · 40분(목표 6,000) — 사용자 1 · 3 모두
  for d in 2 .. 6 loop
    perform t.say('u1 day-' || d, t.rec(1, economy.game_day_start(v_today - d) + interval '8 hours', 2400, 6500));
    perform t.say('u3 day-' || d, t.rec(3, economy.game_day_start(v_today - d) + interval '8 hours', 2400, 6500));
  end loop;
  -- 어제 밤 20:00 — 사용자 1 은 4시간 40,000보(에너지는 10 — 6,000보만 적립, 걸음은 다 보상 걸음), 사용자 3 은 6,500보
  perform t.say('u1 B (yesterday 20:00, 4h 40k)', t.rec(1, v_B, 14400, 40000));
  perform t.say('u3 yesterday', t.rec(3, economy.game_day_start(v_today - 1) + interval '8 hours', 2400, 6500));
  perform t.say('u1 streak before today', economy.goal_streak_before(t.u(1), v_today, 60));
  perform t.say('u3 streak before today', economy.goal_streak_before(t.u(3), v_today, 60));

  -- 오늘: 사용자 1 은 에너지를 늘려(60) 4시간 40,000보 — 하루 최대 SUP 에 닿는다
  insert into public.energy_days (user_id, day, used, bonus) values (t.u(1), v_today, 0, 60)
  on conflict (user_id, day) do update set bonus = 60;
  r := t.rec(1, v_A, 14400, 40000);
  perform t.say('u1 A (today 07:00, 4h 40k)', r);
  perform t.say('u1 day_max', (select s.day_max from public.walk_sessions s where s.user_id = t.u(1) and s.started_at = v_A));
  perform t.say('u1 goal ledger today', coalesce((select l.amount::text from public.sup_ledger l where l.user_id = t.u(1) and l.ref = 'goal:' || v_today), 'NONE'));
  perform t.say('u1 STREAK box today', exists (select 1 from economy.box_awards a where a.user_id = t.u(1) and a.source like 'STREAK:%'));
  perform t.say('u1 streak tomorrow would count', economy.goal_streak_before(t.u(1), v_today + 1, 60));

  -- 대조: 사용자 3 은 오늘 6,500보
  perform t.say('u3 today', t.rec(3, v_A, 2400, 6500));
  perform t.say('u3 goal ledger today', coalesce((select l.amount::text from public.sup_ledger l where l.user_id = t.u(3) and l.ref = 'goal:' || v_today), 'NONE'));
  perform t.say('u3 STREAK box today', exists (select 1 from economy.box_awards a where a.user_id = t.u(3) and a.source like 'STREAK:%'));
  perform t.say('u3 streak tomorrow would count', economy.goal_streak_before(t.u(3), v_today + 1, 60));

  -- 하루 최대에 닿은 뒤: 코스 완주 보상 — 사용자 2 가 올린 코스(사용자 1 의 오늘 경로 그대로, 시각 없이)
  select s.track into v_trk from public.walk_sessions s where s.user_id = t.u(1) and s.started_at = v_A;
  select string_agg(split_part(p, ',', 1) || ',' || split_part(p, ',', 2), ';' order by o) into v_course
    from unnest(string_to_array(v_trk, ';')) with ordinality x(p, o) where o % 20 = 1;
  perform t.login(2);
  perform public.course_share('감사 코스', '', 38, 0, v_course);
  perform t.login(1);
  perform t.say('u1 course_run_submit', (select row_to_json(c)::text from public.course_run_submit(v_course, v_A) c));
  perform t.say('u1 course ledger', (select l.amount from public.sup_ledger l where l.user_id = t.u(1) and l.ref = 'course:' || v_today));

  -- 주간 도전 · 나이트 러너
  perform t.say('u1 step_surge progress', public.event_progress('step_surge'));
  begin v_ev := public.event_claim('step_surge'); exception when others then v_ev := -1; perform t.say('step_surge err', sqlerrm); end;
  perform t.say('u1 event_claim step_surge', v_ev);
  perform t.say('u1 night_quest progress km', public.event_progress('night_quest'));
  begin v_ev := public.event_claim('night_quest'); exception when others then v_ev := -1; perform t.say('night_quest err', sqlerrm); end;
  perform t.say('u1 event_claim night_quest', v_ev);

  -- 결과: 오늘 받은 SUP(원장) vs 하루 최대 · day_earned
  -- 오늘 날짜의 러닝 적립(day_earned 가 세는 것) + 오늘 받은 러닝 밖 적립(코스 · 도전)
  select economy.day_earned(t.u(1), v_today) + coalesce(sum(l.amount), 0) into v_led from public.sup_ledger l
   where l.user_id = t.u(1) and l.amount > 0 and l.session_id is null and l.ref is distinct from 'goal:' || v_today
     and economy.game_day(l.occurred_at) = v_today;
  perform t.say('u1 TODAY day_earned (what the cap sees)', economy.day_earned(t.u(1), v_today));
  perform t.say('u1 TODAY credited SUP (ledger)', v_led);
  perform t.say('u1 TODAY ledger by kind', (select string_agg(l.kind || ' ' || l.amount, ' | ' order by l.id) from public.sup_ledger l
     where l.user_id = t.u(1) and l.amount > 0 and economy.game_day(l.occurred_at) = v_today
       and (l.session_id is null or l.session_id in (select s.id from public.walk_sessions s where s.user_id = t.u(1) and economy.game_day(s.started_at) = v_today))));

  -- 러너 걸음(0121): 판정 CLEAN 만 — 하루 최대에 닿은 오늘 러닝(FLAGGED)의 40,000보는 빠진다. 상자 · 목표가 세는 보상 걸음에는 든다
  perform t.say('u1 runner_steps_of (level steps)', economy.runner_steps_of(t.u(1)));
  perform t.say('u1 box/goal reward steps all week', economy.box_steps(t.u(1), v_today - 6, v_today));
  perform t.say('u1 today run verdict', (select s.verdict || ' / ' || s.verdict_reason from public.walk_sessions s where s.user_id = t.u(1) and s.started_at = v_A));
end $$;
