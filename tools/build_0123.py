#!/usr/bin/env python3
"""서버 0123(2차 점검 — 꼼수 · 보안 고침)을 만든다. 고칠 함수는 마지막 정의를 복사해 고칠 곳만 바꾼다(0122 와 같은 방식).

    python3 tools/build_0123.py   # supabase/migrations/0123_cheat_security_fixes.sql 을 다시 쓴다

바꿀 곳은 정한 횟수만큼만 나와야 한다(아니면 멈춘다).
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
MIG = ROOT / "supabase" / "migrations"
OUT = MIG / "0123_cheat_security_fixes.sql"


def lines(name: str, start: int, end: int) -> str:
    text = (MIG / name).read_text(encoding="utf-8").split("\n")
    return "\n".join(text[start - 1:end]) + "\n"


def block(name: str, head: str, tail: str) -> str:
    """name 파일에서 head 로 시작하는 줄부터 tail 로 시작하는 첫 줄까지(포함) — 줄 번호가 바뀌어도 같은 함수를 집는다."""
    text = (MIG / name).read_text(encoding="utf-8").split("\n")
    starts = [i for i, l in enumerate(text) if l.startswith(head)]
    if len(starts) != 1:
        raise SystemExit(f"{name}: '{head}' 가 {len(starts)}번")
    i = starts[0]
    j = next(k for k in range(i, len(text)) if text[k].startswith(tail))
    return "\n".join(text[i:j + 1]) + "\n"


def patch(src: str, old: str, new: str, count: int = 1) -> str:
    n = src.count(old)
    if n != count:
        raise SystemExit(f"바꿀 곳이 {count}번이 아니라 {n}번: {old[:100]!r}")
    return src.replace(old, new)


# ── record_session (0122 가 마지막) ───────────────────────────────────────
rs = block("0122_economy_audit_fixes.sql", "create or replace function public.record_session(",
           "grant execute on function public.record_session(")
rs = patch(rs, "  v_reward_steps int := 0;\n",
           "  v_reward_steps int := 0;\n"
           "  -- 0123: 경로에서 잰 움직인 시간 · 이어진 과속 구간 · 경로 지문 · 태운 신발의 수리비\n"
           "  v_moving numeric := 0;\n  v_over_sec numeric := 0;\n  v_over_m numeric := 0;\n"
           "  v_sig double precision[];\n  v_dts int[];\n  v_lat0 double precision;\n  v_lng0 double precision;\n"
           "  v_wall_s int;\n  v_wear_cost numeric := 0;\n")
rs = patch(rs,
           "  v_gps_m := least(coalesce(v_gps_m, 0), greatest(v_elapsed, 1) * 7.0);\n",
           "  v_gps_m := least(coalesce(v_gps_m, 0), greatest(v_elapsed, 1) * 7.0);\n"
           "  -- 0123: 경로가 말해 주는 움직인 시간(닳음 · 크루 체력의 바닥) · 사람 속도를 넘는 구간 · 다시 올린 경로를 가릴 지문\n"
           "  v_wall_s := greatest(floor(extract(epoch from (p_ended_at - p_started_at)))::int, 0);\n"
           "  select m.moving_sec, m.over_sec, m.over_m into v_moving, v_over_sec, v_over_m from economy.track_motion(v_track) m;\n"
           "  select g.sig, g.dts, g.lat0, g.lng0 into v_sig, v_dts, v_lat0, v_lng0 from economy.track_sig(v_track) g;\n")
# 판정: 가입 전 러닝 · 서버가 보는 과속 · 거의 같은 경로
rs = patch(rs,
           "  elsif v_glitch > 0.5 then\n"
           "    v_verdict := 'VOID';\n"
           "    v_reason := '이동 속도가 사람 범위를 벗어납니다';\n",
           "  elsif v_glitch > 0.5 then\n"
           "    v_verdict := 'VOID';\n"
           "    v_reason := '이동 속도가 사람 범위를 벗어납니다';\n"
           "\n"
           "  -- 0123: 가입하기 전에 시작한 러닝 — 새 계정이 지난 이레를 지어내 첫날 러너 Lv30 · 상자 51개를 받았다\n"
           "  elsif coalesce((economy.setting('signup_run_guard') #>> '{}')::boolean, true)\n"
           "        and p_started_at < (select p.created_at from public.profiles p where p.id = v_user) - interval '10 minutes' then\n"
           "    v_verdict := 'VOID';\n"
           "    v_reason := '가입하기 전에 시작한 러닝입니다';\n"
           "\n"
           "  -- 0123: 과속 멈춤은 앱만 했다(앱이 신고하지 않으면 차 · 자전거로 달려도 CLEAN). 서버도 경로에서 본다 —\n"
           "  -- 시속 25km 를 넘는 10초 구간이 끊기지 않고 30초 · 150m 넘게 이어졌는데 앱의 신고(run_penalties)가 없다\n"
           "  elsif v_over_sec >= economy.fx('overspeed_min_sec', 30) and v_over_m >= economy.fx('overspeed_min_m', 150)\n"
           "        and not exists (select 1 from public.run_penalties x\n"
           "                         where x.user_id = v_user and x.run_started_at = p_started_at) then\n"
           "    v_verdict := 'VOID';\n"
           "    v_reason := '러닝 속도를 넘는 이동이 이어졌습니다';\n")
rs = patch(rs,
           "    v_verdict := 'VOID';\n"
           "    v_reason := '이미 올린 경로입니다';\n",
           "    v_verdict := 'VOID';\n"
           "    v_reason := '이미 올린 경로입니다';\n"
           "\n"
           "  -- 0123: 사본 경로 — 좌표를 1cm 옮기거나 시각을 통째로 민 사본이 md5 를 피했다(부계정 크루 · 같은 경로 되풀이).\n"
           "  -- 점 사이 시각 간격이 90% 넘게 같고 경로가 5m 안(economy.route_copy_of) — 나란히 달린 두 사람은 폰마다 간격이 달라 겹치지 않는다\n"
           "  elsif economy.route_copy_of(v_user, p_started_at, v_sig, v_dts, v_lat0, v_lng0, v_gps_m) then\n"
           "    v_verdict := 'VOID';\n"
           "    v_reason := '이미 올린 경로와 같습니다';\n")
# 태운 신발: 이 러닝을 시작한 뒤에 태웠을 때만
rs = patch(rs,
           "       and exists (select 1 from public.forge_requests r where r.user_id = v_user and x.id = any(r.material_ids));\n",
           "       and exists (select 1 from public.forge_requests r where r.user_id = v_user and x.id = any(r.material_ids)\n"
           "                      -- 0123: 이 러닝을 시작한 뒤에 태웠을 때만 — 예전엔 며칠 전에 태운 신발도 받아 그 신발로 영영 닳지 않고 달렸다\n"
           "                      and r.created_at >= p_started_at - interval '2 minutes');\n")
# 적립 배수: 이벤트 · 부스터는 그 시간 안에 달린 몫만큼 · 크루 러닝은 아이템 없음
rs = patch(rs,
           "  v_xp := economy.run_reward_mult(v_user, p_started_at);\n",
           "  -- 0123: 이벤트 · 24시간 부스터는 이 러닝 시간 가운데 그 창에 든 몫만큼(출발만 창 안에 두고 멈춰 뒀다 나중에 달리던 것) ·\n"
           "  -- 크루 러닝은 러닝 아이템을 쓰지 않는다(앱도 고르지 않는다 — 묶여 있었으면 아래에서 돌려준다)\n"
           "  v_xp := economy.run_reward_mult_span(v_user, p_started_at, p_ended_at, v_pool <> 'CREW');\n")
# 크루 체력: 경로가 말하는 움직인 시간을 바닥으로
rs = patch(rs,
           "      v_run_sec := v_elapsed;\n",
           "      v_run_sec := greatest(v_elapsed, least(coalesce(v_moving, 0)::int, v_wall_s));  -- 0123: 앱이 보낸 시간이 짧아도 경로로 움직인 시간만큼\n")
# 닳음: 경로가 말하는 움직인 시간을 바닥으로
rs = patch(rs,
           "  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(v_elapsed) * v_wear_luck * v_wear_mult\n",
           "  -- 0123: 닳는 시간은 앱이 보낸 시간과 경로가 말하는 움직인 시간 가운데 긴 쪽(달린 시간을 줄여 보내 덜 닳던 것)\n"
           "  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(greatest(v_elapsed, least(coalesce(v_moving, 0)::int, v_wall_s)))\n"
           "                                                              * v_wear_luck * v_wear_mult\n")
# 경로 지문을 기록에
rs = patch(rs,
           "    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit, together_paid\n  )",
           "    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit, together_paid,\n"
           "    route_sig, route_dts, route_lat0, route_lng0\n  )")
rs = patch(rs,
           "    case when v_pool = 'CREW' and not economy.together_on('CREW') then coalesce(v_head_sup, 0) else 0 end\n  )",
           "    case when v_pool = 'CREW' and not economy.together_on('CREW') then coalesce(v_head_sup, 0) else 0 end,\n"
           "    v_sig, v_dts, v_lat0, v_lng0\n  )")
# 태운 신발로 달린 몫의 수리비 · 크루 러닝의 아이템 돌려주기
rs = patch(rs,
           "  -- 이 러닝에 묶인 아이템을 러닝 기록에 잇고, 적용된 적립 배수를 적는다 (0106)\n",
           "  -- 0123: 출발 때 신은 신발을 올리기 전에 강화 재료로 태웠다 — 그 신발은 닳을 수 없으니, 이 러닝이 닳게 했을 몫의 수리비를\n"
           "  -- 이 러닝의 적립에서 뺀다(태울 때는 닳기 전 가득 값으로 경험치를 받았다 — 예전엔 닳음이 통째로 공짜였다)\n"
           "  if v_shoe.id is not null and v_shoe.status = 'BURNED' and v_verdict <> 'VOID' and v_wear > 0 and v_points > 0 then\n"
           "    v_wear_cost := least(round(v_wear * economy.repair_unit_of(v_shoe), 4), v_points);\n"
           "    if v_wear_cost >= 0.0001 then\n"
           "      perform economy.ledger_apply(v_user, 'SPEND_REPAIR', -v_wear_cost,\n"
           "                                   format('강화 재료로 태운 신발로 달린 몫의 수리비 (닳음 %s)', v_wear), 'burnwear:' || v_session_id);\n"
           "      v_reason := concat_ws(' · ', nullif(v_reason, ''), '강화 재료로 태운 신발로 달려 닳은 몫의 수리비를 뺐습니다');\n"
           "      update public.walk_sessions s set verdict_reason = nullif(v_reason, '') where s.id = v_session_id;\n"
           "    end if;\n"
           "  end if;\n"
           "  if v_pool = 'CREW' then\n"
           "    perform economy.items_refund(v_user, p_started_at);\n"
           "  end if;\n"
           "  -- 이 러닝에 묶인 아이템을 러닝 기록에 잇고, 적용된 적립 배수를 적는다 (0106)\n")
rs = patch(rs, "묶인 체력 · 8 : 2 무게 · 번개 한 번 · 미룬 정산 (0122)';",
           "묶인 체력 · 8 : 2 무게 · 번개 한 번 · 미룬 정산 (0122) · 가입 전 · 과속 · 거의 같은 경로 · 태운 신발 · 배수 시간 몫 (0123)';")

# ── 운영자 ──────────────────────────────────────────────────────────────
ops_fail = lines("0114_ops_security.sql", 198, 222)
ops_fail = patch(ops_fail,
                 "  if (select count(*) from economy.ops_login_fails f where f.at > now() - v_window)\n"
                 "       >= economy.ops_num('ops_login_global_fails', 30)\n"
                 "     and not exists (select 1 from economy.ops_login_locks l where l.scope = 'ALL' and l.until > now()) then\n"
                 "    insert into economy.ops_login_locks (scope, until) values ('ALL', v_until)\n"
                 "    on conflict (scope) do update set until = excluded.until;\n"
                 "    insert into public.admin_audit (actor, action, target, detail)\n"
                 "    values (null, 'ops_login_locked', 'all', jsonb_build_object('until', v_until, 'kind', p_kind));\n"
                 "  end if;\n",
                 "  -- 0123: 모든 곳(ALL) 잠금은 뺐다 — 누구나 틀린 키 30번으로 운영자 로그인 전체를 계속 막을 수 있었다(키는 244비트라 맞혀 볼 수 없다)\n")

ops_event = lines("0114_ops_security.sql", 677, 761)
ops_event = patch(ops_event,
                  "  if p_mult is null or p_mult <= 1 or p_mult > 5 then\n",
                  "  -- 0123: 저장하는 값(둘째 자리 반올림)으로 먼저 본다 — 2.995 는 3.00 으로 저장되는데 3 아래라 다시 확인 없이 지나갔다\n"
                  "  p_mult := round(p_mult, 2);\n"
                  "  if p_mult is null or p_mult <= 1 or p_mult > 5 then\n")
ops_event = patch(ops_event,
                  "  v_big := p_mult >= economy.ops_num('ops_confirm_mult', 3) or p_hours > economy.ops_num('ops_confirm_hours', 48);\n",
                  "  -- 0123: 줄줄이 이어 붙인 이벤트(이미 잡힌 앞으로의 시간까지 더해) · 하루 넘게 뒤에 시작하는 것도 키를 한 번 더\n"
                  "  v_big := p_mult >= economy.ops_num('ops_confirm_mult', 3) or p_hours > economy.ops_num('ops_confirm_hours', 48)\n"
                  "        or v_from > now() + interval '24 hours'\n"
                  "        or p_hours + coalesce((select sum(extract(epoch from (e.ends_at - greatest(e.starts_at, now()))) / 3600.0)\n"
                  "                                 from economy.reward_events e where e.cancelled_at is null and e.ends_at > now()), 0)\n"
                  "           > economy.ops_num('ops_confirm_hours', 48);\n")

# ── 타워 게임 ───────────────────────────────────────────────────────────
tower_join = lines("0115_waitlist_close.sql", 104, 123)
tower_join = patch(tower_join,
                   "  insert into economy.tower_players (entry_id, name) values (v_entry, v_name)\n"
                   "  on conflict (entry_id) do update set name = excluded.name, updated_at = now();\n",
                   "  -- 0123: 이름은 처음 참가한 곳(IP 해시)에서만 바꾼다 — 남의 이메일로 참가해 그 사람의 이름을 덮어쓰던 것\n"
                   "  insert into economy.tower_players (entry_id, name, client) values (v_entry, v_name, economy.waitlist_client())\n"
                   "  on conflict (entry_id) do update set name = excluded.name, updated_at = now(),\n"
                   "                                       client = coalesce(economy.tower_players.client, excluded.client)\n"
                   "   where economy.tower_players.client is null or economy.tower_players.client is not distinct from excluded.client;\n")

# ── 상자 현황 · 목표 ─────────────────────────────────────────────────────
box_status = block("0122_economy_audit_fixes.sql", "create or replace function public.box_status()", "end $$;")
box_status = patch(box_status,
                   "  select l.amount into v_paid from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day;\n",
                   "  select l.amount into v_paid from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day;\n"
                   "  -- 0123: 하루 최대에 막혀 보너스 없이 채운 날(goal_days.met_at, 0122)도 채운 날 — 0 으로 보낸다(앱이 \"확인 중\"에 머물지 않게)\n"
                   "  if v_paid is null and exists (select 1 from economy.goal_days g where g.user_id = v_user and g.day = v_day and g.met_at is not null) then\n"
                   "    v_paid := 0;\n"
                   "  end if;\n")
box_status = patch(box_status,
                   "        from (select distinct substr(l.ref, 6)::date as d from public.sup_ledger l\n"
                   "               where l.user_id = v_user and l.ref like 'goal:%') x) y\n",
                   "        from (select substr(l.ref, 6)::date as d from public.sup_ledger l\n"
                   "               where l.user_id = v_user and l.ref like 'goal:%'\n"
                   "              union\n"
                   "              select g.day from economy.goal_days g where g.user_id = v_user and g.met_at is not null) x) y\n")
box_status = patch(box_status,
                   "    'goal_bonus', round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2),\n",
                   "    -- 0123: 받을 수 있는 만큼만 보인다(하루 최대 SUP 에 남은 몫)\n"
                   "    'goal_bonus', least(round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2),\n"
                   "                        case when economy.daily_max_on() then greatest(round(economy.day_room(v_user, v_day, null), 2), 0) else 1000000 end),\n")

# ── 아이템 ──────────────────────────────────────────────────────────────
run_items = lines("0106_items.sql", 304, 369)
run_items = patch(run_items,
                  "  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));\n",
                  "  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));\n"
                  "  perform economy.items_refund_stale(v_user);  -- 0123: 기록되지 못한 러닝에 묶여 사라진 아이템을 돌려준다\n")
run_items = patch(run_items,
                  "    'mult', economy.run_reward_mult(v_user, p_run_started_at),\n",
                  "    'mult', economy.run_reward_mult(v_user, p_run_started_at),  -- 출발 때 값(끝나면 이벤트 시간 몫으로 다시 센다, 0123)\n")

# ── 강화 견적 서명 ──────────────────────────────────────────────────────
quote = lines("0118_forge_xp_daily.sql", 221, 303)
quote = patch(quote,
              "             economy.fx('repair_share', 0.45), economy.points_per_step());\n",
              "             economy.fx('repair_share', 0.45), economy.points_per_step(),\n"
              "             -- 0123: 통의 굽이 · 닳은 재료 값을 바꾸는 설정도(견적과 넣기 사이에 바뀌면 다시 묻게)\n"
              "             economy.fx('forge_xp_level_growth', 0), coalesce(economy.setting('forge_xp_curve')::text, ''),\n"
              "             economy.fx('dur_repair_exp', 1), economy.fx('eff_wear_exp', 1));\n")

# ── 체인 기록: 하루 걸음 목표는 서버 값으로 ──────────────────────────────
payload = lines("0078_chain_activity.sql", 571, 701)
payload = patch(payload,
                "    return v || jsonb_build_object('goalMet', r.steps >= r.goal,\n"
                "                                   '_audit', jsonb_build_object('steps', r.steps, 'goal', r.goal));\n",
                "    -- 0123: 목표 달성은 서버가 확인한 그날의 보상 걸음 · 그날의 목표로(앱이 보낸 걸음 · 목표는 지어낼 수 있다 — goal=1)\n"
                "    return v || jsonb_build_object(\n"
                "      'goalMet', economy.verified_steps_on(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int)\n"
                "                 >= economy.goal_of(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int),\n"
                "      '_audit', jsonb_build_object('steps', r.steps, 'goal', r.goal,\n"
                "                                   'verified', economy.verified_steps_on(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int)));\n")

HEAD = r"""-- 0123 2차 점검 고침 — 꼼수 · 보안 (2026-11-22) — 사용자 "다시 의심되는 부분 찔러보고 버그 한 번 더 고쳐놔" → "모든 꼼수 / 해킹 등 유지보수 관련해서 전부 할 수 있는 거 다".
-- 점검: docs/점검-2026-11-22-2차-꼼수-보안.md(여덟 갈래). tools/build_0123.py 가 만든다(고칠 함수는 마지막 정의를 복사).
-- record_session 은 0123 이 마지막 정의다(0122 본문 + 아래 1 ~ 7).
--   1. 태운 신발 — 이 러닝을 시작한 뒤에 태웠을 때만 그 신발로 셈하고, 닳을 몫의 수리비는 적립에서 뺀다(예전엔 며칠 전에 태운 신발로 영영 닳지 않았다)
--   2. 사본 경로(좌표를 조금 옮기거나 시각을 통째로 민 것 — 시각 간격 패턴이 같다) — 부계정 크루 · 같은 경로 되풀이(walk_sessions.route_sig · route_dts)
--   3. 가입하기 전에 시작한 러닝은 무효(새 계정이 지난 이레를 지어내 첫날 Lv30)
--   4. 과속 — 앱이 신고하지 않아도 경로에서 시속 25km 넘는 구간이 30초 · 150m 넘게 이어지면 무효
--   5. 닳음 · 크루 체력은 경로가 말하는 움직인 시간을 바닥으로(앱이 보낸 시간을 줄여 덜 닳던 것)
--   6. 이벤트 · 부스터 배수는 러닝 시간 가운데 그 창에 든 몫만큼(출발만 창 안 → 멈춰 뒀다 나중에 달리기)
--   7. 크루 러닝은 러닝 아이템을 쓰지 않는다 — 묶여 있었으면 돌려준다 · 기록되지 못한 러닝의 아이템도 여드레 뒤 돌려준다
--   8. 함께 달린 사람은 가입 24시간 뒤부터(together_min_account_hours — 그날 만든 부계정으로 인원 보너스)
--   9. 운영자: 모든 곳 잠금(아무나 30번 틀려 운영자를 막던 것) 없앰 · 보상 배수 반올림 우회 · 이어 붙인 이벤트는 키를 한 번 더
--  10. 권한: 프로필 직접 고치기(목표 · 이름 — 지난 날 목표를 바꿔 보너스) · 크루 · 글 · 코스 · 댓글 직접 지우기(달리는 크루 · 출발한 번개를 지움) ·
--      admin_economy_set(범위 없이 아무 설정 — 앱 · 운영자 페이지가 쓰지 않는다) 거둠 · 좋아요 · 번개 참석자는 보이는 글만
--  11. 이야기 지도는 앞뒤 300m 를 자른다(집 위치) · IP 해시에 비밀 소금 · 타워 이름은 처음 참가한 곳에서만 · 예전 레이스 닫음
--  12. 하루 걸음 체인 기록의 목표 달성은 서버 값 · 홈 목표 줄(하루 최대에 막힌 날) · 보호막은 그날 반 넘게 덮어야 · 강화 견적 서명 · 하루 최대의 남은 몫(× 1)

-- ════════════════════════════════════════════════════════════════════
--  0. 설정 · 칸
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('together_min_account_hours', '24'),  -- 함께 달린 사람: 가입하고 이 시간이 지난 계정만(0 = 끔)
  ('route_copy_max_m', '5'),             -- 거의 같은 경로: 시각 간격 패턴이 90% 넘게 같고 시간에 맞춰 고른 점들의 가운데 거리가 이 안이면 사본
  ('overspeed_min_sec', '30'),           -- 서버 과속: 시속 25km 넘는 구간이 끊기지 않고 이만큼(초)
  ('overspeed_min_m', '150'),            -- 서버 과속: 그 구간에서 움직인 거리(m)
  ('signup_run_guard', 'true')           -- 가입하기 전에 시작한 러닝은 무효(false = 끔 — 검사 · 예전 셈)
on conflict (key) do nothing;

alter table public.walk_sessions add column if not exists route_sig double precision[];
alter table public.walk_sessions add column if not exists route_dts int[];
alter table public.walk_sessions add column if not exists route_lat0 double precision;
alter table public.walk_sessions add column if not exists route_lng0 double precision;
comment on column public.walk_sessions.route_sig is '경로 지문 — 시간에 맞춰 고른 24점(위도 · 경도 번갈아). 거의 같은 경로를 다시 올렸는지 본다 (0123)';
comment on column public.walk_sessions.route_dts is '경로 점 사이 시각 간격(ms) 앞 240개 — 시각 패턴을 그대로 둔 사본을 가린다. 간격이 고르면(시험 경로 · 1초 고정) 비운다 (0123)';
create index if not exists walk_sessions_route_start on public.walk_sessions (route_lat0, route_lng0) where route_lat0 is not null;

alter table economy.tower_players add column if not exists client text;

create table if not exists economy.app_secrets (
  name text primary key,
  value text not null,
  created_at timestamptz not null default now()
);
revoke all on economy.app_secrets from public, anon, authenticated;
insert into economy.app_secrets (name, value)
values ('client_salt', encode(sha256(convert_to(gen_random_uuid()::text || gen_random_uuid()::text || clock_timestamp()::text, 'UTF8')), 'hex'))
on conflict (name) do nothing;

-- ════════════════════════════════════════════════════════════════════
--  1. 경로에서 읽는 것 — 움직인 시간 · 이어진 과속 · 지문
-- ════════════════════════════════════════════════════════════════════
-- 10초 창(economy.speed_window_sec)마다: 1분 넘게 빈 곳은 끊는다(track_speed_stats 와 같은 규칙).
--   moving_sec = 초속 0.5m 넘게 움직인 창의 시간 합 · over_sec / over_m = 시속 25km 넘는 창이 끊기지 않고 이어진 가장 긴 구간
--   (60km/h 를 넘는 창은 GPS 튐으로 보고 구간을 끊지도 더하지도 않는다)
create or replace function economy.track_motion(p_track text)
returns table (moving_sec numeric, over_sec numeric, over_m numeric)
language plpgsql immutable set search_path = public, economy as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text; v_parts text[];
  v_lat double precision; v_lng double precision; v_at bigint;
  v_blat double precision; v_blng double precision; v_bat bigint;
  v_have boolean := false;
  v_dt double precision; v_m double precision; v_kmh double precision;
  v_move double precision := 0;
  v_run_s double precision := 0; v_run_m double precision := 0;
  v_best_s double precision := 0; v_best_m double precision := 0;
  v_limit double precision := economy.fx('overspeed_kmh', 25);
begin
  if p_track is null or p_track = '' then
    return query select 0::numeric, 0::numeric, 0::numeric;
    return;
  end if;
  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';
    v_lat := v_parts[1]::double precision; v_lng := v_parts[2]::double precision; v_at := v_parts[3]::bigint;
    if not v_have then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at; v_have := true;
      continue;
    end if;
    v_dt := (v_at - v_bat) / 1000.0;
    if v_dt <= 0 or v_dt > economy.track_gap_sec() then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
      v_run_s := 0; v_run_m := 0;
      continue;
    end if;
    continue when v_dt < economy.speed_window_sec();
    v_m := economy.haversine_m(v_blat, v_blng, v_lat, v_lng);
    v_kmh := v_m / v_dt * 3.6;
    if v_kmh > economy.speed_glitch_kmh() then
      null;  -- 튐 — 이어진 구간을 끊지도 더하지도 않는다
    else
      if v_m / v_dt > 0.5 then v_move := v_move + v_dt; end if;
      if v_kmh > v_limit then
        v_run_s := v_run_s + v_dt; v_run_m := v_run_m + v_m;
        if v_run_s > v_best_s then v_best_s := v_run_s; v_best_m := v_run_m; end if;
      else
        v_run_s := 0; v_run_m := 0;
      end if;
    end if;
    v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
  end loop;
  return query select round(v_move::numeric, 1), round(v_best_s::numeric, 1), round(v_best_m::numeric, 1);
end $$;

-- 경로 지문 — 첫 점 · 끝 점 시각 사이를 23 칸으로 나눠 그 시각의 자리(앞뒤 점 사이를 시간으로 나눠)를 고른다(24점 · 위도 경도 번갈아 48개).
-- 시각 간격은 점이 20개 이상이고 간격이 고르지 않을 때만(1초 고정이면 서로 다른 폰도 같아진다). 경로를 한 번만 훑는다(1시간 경로 몇 ms)
create or replace function economy.track_sig(p_track text)
returns table (sig double precision[], dts int[], lat0 double precision, lng0 double precision)
language plpgsql immutable set search_path = public, economy as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text; v_parts text[];
  la double precision[] := '{}'; lo double precision[] := '{}'; ts bigint[] := '{}';
  n int; a0 bigint; a1 bigint; k int; i int := 1; t double precision; f double precision;
  v_sig double precision[] := '{}'; v_all int[] := '{}'; v_d int; v_distinct int;
begin
  if p_track is null or p_track = '' then
    return query select null::double precision[], null::int[], null::double precision, null::double precision;
    return;
  end if;
  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';
    la := la || v_parts[1]::double precision;
    lo := lo || v_parts[2]::double precision;
    ts := ts || v_parts[3]::bigint;
  end loop;
  n := coalesce(cardinality(ts), 0);
  if n = 0 then
    return query select null::double precision[], null::int[], null::double precision, null::double precision;
    return;
  end if;
  a0 := ts[1]; a1 := ts[n];
  if n >= 2 and a1 > a0 then
    for k in 0 .. 23 loop
      t := a0 + (a1 - a0) * k / 23.0;
      while i < n - 1 and ts[i + 1] < t loop i := i + 1; end loop;
      f := case when ts[i + 1] > ts[i] then least(greatest((t - ts[i]) / (ts[i + 1] - ts[i]), 0), 1) else 0 end;
      v_sig := v_sig || (la[i] + (la[i + 1] - la[i]) * f) || (lo[i] + (lo[i + 1] - lo[i]) * f);
    end loop;
  else
    v_sig := null;
  end if;
  for k in 1 .. n - 1 loop
    v_d := least(greatest(ts[k + 1] - ts[k], -2000000000), 2000000000)::int;
    v_all := v_all || v_d;
  end loop;
  select count(distinct x) into v_distinct from unnest(v_all) x;
  return query select v_sig,
                      case when coalesce(v_distinct, 0) > 5 and n - 1 >= 20 then v_all[1:240] end,
                      la[1], lo[1];
end $$;

-- 이 경로가 이미 올린 경로(다른 계정 · 내 다른 러닝)의 사본인가 — 첫 점이 약 50m 안 · 경로 거리 ±10% · 30일 안에서 견준다.
-- 사본 = 점 사이 시각 간격(ms)이 같은 자리에서 90% 넘게 같고, 시간에 맞춰 고른 24점의 가운데 거리가 route_copy_max_m(5m) 안.
-- 폰마다 시각 간격이 ms 로 달라 나란히 달린 두 사람도 이렇게 겹치지 않는다. 좌표만 조금 옮기거나 시각을 통째로 민 사본을 잡는다
-- (시각까지 새로 지어낸 사본은 기기 확인 없이는 가릴 수 없다 — 점검 문서)
create or replace function economy.route_copy_of(p_user uuid, p_started timestamptz, p_sig double precision[], p_dts int[],
    p_lat0 double precision, p_lng0 double precision, p_gps_m double precision)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select coalesce(p_lat0 is not null and coalesce(p_gps_m, 0) >= 500 and cardinality(p_sig) = 48 and cardinality(p_dts) >= 20 and exists (
    select 1 from public.walk_sessions s
     where s.route_lat0 between p_lat0 - 0.0005 and p_lat0 + 0.0005
       and s.route_lng0 between p_lng0 - 0.0007 and p_lng0 + 0.0007
       and s.created_at > now() - interval '30 days'
       and not (s.user_id = p_user and s.started_at = p_started)
       and coalesce(s.gps_distance_m, 0) between p_gps_m * 0.9 and p_gps_m * 1.1
       and cardinality(s.route_sig) = 48 and cardinality(s.route_dts) >= 20
       and (select count(*) filter (where s.route_dts[i] = p_dts[i])::numeric / count(*)
              from generate_series(1, least(cardinality(s.route_dts), cardinality(p_dts))) i) >= 0.9
       and (select percentile_cont(0.5) within group (order by economy.haversine_m(p_sig[2 * i - 1], p_sig[2 * i], s.route_sig[2 * i - 1], s.route_sig[2 * i]))
              from generate_series(1, 24) i) < economy.fx('route_copy_max_m', 5)), false)
$$;

-- ════════════════════════════════════════════════════════════════════
--  2. 적립 배수 — 이벤트 · 부스터는 그 창에 든 몫만큼 · 아이템
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.run_reward_mult_span(p_user uuid, p_started timestamptz, p_ended timestamptz, p_items boolean default true)
returns numeric language sql stable security definer set search_path = public, economy as $$
  with span as (select greatest(extract(epoch from (greatest(p_ended, p_started) - p_started)), 1)::numeric as sec)
  select greatest(
    1,
    -- 24시간 부스터(파는 곳은 닫혀 있다)
    coalesce((select max(1 + (2 - 1) * least(greatest(extract(epoch from (least(b.ends_at, p_ended) - greatest(b.starts_at, p_started)))::numeric, 0) / span.sec, 1))
                from public.boosts b, span
               where b.user_id = p_user and b.kind = 'XP_BOOSTER' and b.starts_at < p_ended and b.ends_at > p_started), 1),
    -- 보상 이벤트(취소하면 그때까지)
    coalesce((select max(1 + (e.mult - 1) * least(greatest(extract(epoch from (least(e.ends_at, coalesce(e.cancelled_at, 'infinity'::timestamptz), p_ended)
                                                                                - greatest(e.starts_at, p_started)))::numeric, 0) / span.sec, 1))
                from economy.reward_events e, span
               where e.starts_at < p_ended and least(e.ends_at, coalesce(e.cancelled_at, 'infinity'::timestamptz)) > p_started), 1),
    -- 이 러닝에 고른 아이템(크루 러닝은 없음)
    case when p_items then coalesce((select max(k.value) from economy.item_uses u join economy.item_kinds k on k.kind = u.kind
                                      where u.user_id = p_user and u.run_started_at = p_started and k.grp = 'REWARD'), 1) else 1 end
  )::numeric(6, 3)
$$;

-- 이 러닝에 묶인 아이템을 가방으로 돌려준다(크루 러닝 — 아이템을 쓰지 않는다)
create or replace function economy.items_refund(p_user uuid, p_run_started_at timestamptz)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_kind text; v_n int := 0;
begin
  for v_kind in delete from economy.item_uses u where u.user_id = p_user and u.run_started_at = p_run_started_at and u.session_id is null
                returning u.kind loop
    perform economy.item_add(p_user, v_kind, 1);
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;

-- 여드레가 지나도 기록되지 않은 러닝에 묶인 아이템 — record_session 이 거절해(이레 넘음 · 끝이 미래) 영영 쓰지 못하는 것을 돌려준다
create or replace function economy.items_refund_stale(p_user uuid)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_kind text; v_n int := 0;
begin
  for v_kind in delete from economy.item_uses u
                 where u.user_id = p_user and u.run_started_at is not null and u.session_id is null
                   and u.run_started_at < now() - interval '8 days'
                   and not exists (select 1 from public.walk_sessions s where s.user_id = p_user and s.started_at = u.run_started_at)
                returning u.kind loop
    perform economy.item_add(p_user, v_kind, 1);
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  3. 함께 달린 사람 — 0122 + 가입 24시간 뒤부터(그날 만든 부계정으로 인원 보너스)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.together_qualifies(s public.walk_sessions)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select s.verdict <> 'VOID' and s.gps_backed
     and coalesce(s.reward_steps, 1) > 0
     and coalesce(s.verified_steps, 0) >= economy.together_min_steps()
     and coalesce(s.duration_sec, 0) >= economy.together_min_sec()
     and coalesce(s.gps_credit_m, 0) >= coalesce(economy.together_goal_m(s.party_id, s.user_id), 0)
     and coalesce((select p.created_at from public.profiles p where p.id = s.user_id), s.started_at)
         <= s.started_at - make_interval(hours => greatest(economy.fx('together_min_account_hours', 24), 0)::int)
$$;

-- 하루 최대 SUP 의 남은 몫 — 0120 + 그날 가장 큰 하루 최대를 배수 없이(× 1) 본다. 1분짜리 2배 러닝 하나로 목표 · 코스 보너스의 남은 몫이
-- 두 배로 열리던 것. 정산(p_max 를 주는 곳)은 그 러닝의 하루 최대 그대로
create or replace function economy.day_room(p_user uuid, p_day date, p_max numeric)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select coalesce(p_max,
                  (select max(s.day_max / greatest(coalesce(s.reward_mult, 1), 1)) from public.walk_sessions s
                    where s.user_id = p_user and economy.game_day(s.started_at) = p_day and s.day_max is not null),
                  economy.daily_max_of(p_user, (select (economy.shoe_effective(e)).efficiency_bps from economy.equipped_sneaker(p_user) e), 1))
         - economy.day_earned(p_user, p_day)
$$;

-- 지난날 목표를 채운 날 — 0122 + 연속 보호막은 그날을 반 넘게(12시간) 덮을 때만(24시간짜리 하나가 이틀로 세던 것)
create or replace function economy.goal_streak_before(p_user uuid, p_day date, p_max int default 60)
returns int language plpgsql stable security definer set search_path = public, economy as $$
declare v int := 0; d date := p_day - 1;
begin
  while v < p_max loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || d)
      or exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = d and g.met_at is not null)
      or exists (select 1 from public.boosts b
                  where b.user_id = p_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(d + 1) and b.ends_at > economy.game_day_start(d)
                    and least(b.ends_at, economy.game_day_start(d + 1)) - greatest(b.starts_at, economy.game_day_start(d)) >= interval '12 hours'));
    v := v + 1; d := d - 1;
  end loop;
  return v;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  4. IP 해시에 비밀 소금(0083 은 고정 문구라 IPv4 전부를 몇 분이면 맞대어 볼 수 있었다)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.waitlist_client_of(p_ip text) returns text
  language sql stable security definer set search_path = '' as $$
  select case when nullif(btrim(coalesce(p_ip, '')), '') is null then null
              else encode(sha256(convert_to(coalesce((select s.value from economy.app_secrets s where s.name = 'client_salt'), '')
                                            || ':stepup-waitlist:' || btrim(p_ip), 'UTF8')), 'hex') end
$$;
revoke all on function economy.waitlist_client_of(text) from public;

-- ════════════════════════════════════════════════════════════════════
--  5. 이야기 지도 — 크루 러닝 지도처럼 앞뒤 300m 를 자른다(집 · 회사 자리가 누구에게나 보였다)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.story_route(p_track text)
returns text language sql immutable set search_path = public as $$
  select public.crew_run_route(p_track)
$$;
revoke all on function public.story_route(text) from public, anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  6. 운영자
-- ════════════════════════════════════════════════════════════════════
"""

GRANTS = r"""
-- ════════════════════════════════════════════════════════════════════
--  10. 권한 — 앱(지금 판 · 예전 스토어 판 1.17.1)은 이 표들을 읽기만 하고 RPC 로만 바꾼다
-- ════════════════════════════════════════════════════════════════════
-- 프로필을 직접 고치면 목표(profile_set_daily_goal 의 지난 이레 고정) · 이름(profile_name_set 의 16자 · 제어 문자)을 비켜 갔다
revoke update on public.profiles from anon, authenticated;
-- 직접 지우면 crew_dissolve(달리는 크루) · post_delete(출발한 번개) · course_unshare(남의 기록) · crew_leave 를 비켜 갔다
revoke delete on public.crews, public.posts, public.courses, public.crew_members, public.comments from anon, authenticated;
-- 범위 없이 아무 설정(attester_user_id 까지) — 앱 · 운영자 페이지 · 예전 앱 모두 쓰지 않는다(운영자는 ops_economy_set)
revoke execute on function public.admin_economy_set(text, jsonb) from authenticated, anon, public;
-- 좋아요 · 번개 참석자는 내가 볼 수 있는 글의 것만(크루 글의 참석자를 크루 밖 · 로그인하지 않은 사람이 읽었다)
drop policy if exists post_likes_select_all on public.post_likes;
drop policy if exists post_likes_select_visible on public.post_likes;
create policy post_likes_select_visible on public.post_likes for select
  using (exists (select 1 from public.posts p where p.id = post_likes.post_id));
drop policy if exists flash_participants_select_all on public.flash_participants;
drop policy if exists flash_participants_select_visible on public.flash_participants;
create policy flash_participants_select_visible on public.flash_participants for select
  using (exists (select 1 from public.posts p where p.id = flash_participants.post_id));

revoke all on function economy.track_motion(text) from public, anon, authenticated;
revoke all on function economy.track_sig(text) from public, anon, authenticated;
revoke all on function economy.route_copy_of(uuid, timestamptz, double precision[], int[], double precision, double precision, double precision) from public, anon, authenticated;
revoke all on function economy.run_reward_mult_span(uuid, timestamptz, timestamptz, boolean) from public, anon, authenticated;
revoke all on function economy.items_refund(uuid, timestamptz) from public, anon, authenticated;
revoke all on function economy.items_refund_stale(uuid) from public, anon, authenticated;
revoke all on function economy.together_qualifies(public.walk_sessions) from public, anon, authenticated;
revoke all on function economy.day_room(uuid, date, numeric) from public, anon, authenticated;
revoke all on function economy.goal_streak_before(uuid, date, int) from public, anon, authenticated;
revoke all on function economy.ops_fail(text, text) from public;
revoke all on function economy.forge_xp_quote_for(uuid, bigint, bigint[]) from public, anon, authenticated;
revoke all on function economy.activity_payload(public.chain_jobs) from public, anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  11. 한 번만: 모든 곳 잠금 풀기 · 예전 레이스 닫기 · 이야기 지도 다시 · 지난 여드레 러닝의 경로 지문
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'cheat_fix_20261122') then
    delete from economy.ops_login_locks where scope = 'ALL';
    update public.economy_settings set value = 'false'::jsonb, updated_at = now() where key = 'race_open';
    update public.posts p set run_route = coalesce(public.story_route(s.track), '')
      from public.walk_sessions s where s.id = p.run_session and p.run_route <> '';
    update public.walk_sessions s set route_sig = g.sig, route_dts = g.dts, route_lat0 = g.lat0, route_lng0 = g.lng0
      from (select x.id, (economy.track_sig(x.track)).* from public.walk_sessions x
             where x.created_at > now() - interval '8 days' and x.track <> '' and x.route_sig is null) g
     where s.id = g.id;
    insert into public.economy_settings (key, value) values ('cheat_fix_20261122', to_jsonb(now()));
  end if;
end $$;
"""


def main() -> None:
    parts = [
        HEAD,
        ops_fail,
        "\n-- 보상 이벤트 — 0114 본문 + 저장 값으로 본다 · 이어 붙인 이벤트 · 하루 넘게 뒤에 시작하면 키를 한 번 더\n",
        ops_event,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  7. 타워 게임 — 이름은 처음 참가한 곳에서만(0115 본문)\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        tower_join,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  8. 상자 현황(0122 본문) — 하루 최대에 막혀 채운 날 · 목표 보너스는 받을 수 있는 만큼\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        box_status,
        "\n-- 러닝 아이템(0106 본문) — 사라진 아이템 돌려주기\n",
        run_items,
        "\n-- 강화 견적(0118 본문) — 서명에 굽이 · 닳음 설정도\n",
        quote,
        "\n-- 체인 기록 재료(0078 본문) — 하루 걸음의 목표 달성은 서버 값\n",
        payload,
        "\n-- ════════════════════════════════════════════════════════════════════\n"
        "--  9. record_session — 0122 본문 + 태운 신발 · 거의 같은 경로 · 가입 전 · 과속 · 움직인 시간 · 배수 시간 몫 · 크루 아이템\n"
        "--     다시 고칠 때는 이것(0123)을 복사한다\n"
        "-- ════════════════════════════════════════════════════════════════════\n",
        rs,
        GRANTS,
    ]
    OUT.write_text("".join(parts), encoding="utf-8")
    print(f"썼다: {OUT.relative_to(ROOT)} ({sum(p.count(chr(10)) for p in parts)}줄)")


if __name__ == "__main__":
    main()
