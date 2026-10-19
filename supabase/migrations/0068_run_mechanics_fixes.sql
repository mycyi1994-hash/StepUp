-- ════════════════════════════════════════════════════════════════════
--  0068 — 러닝 정산 점검 고치기: 거리와 상관없이 1분마다 5 · 상한에 걸린 러닝의 에너지 · 자정을 넘긴 목표 보너스 ·
--         경로 없는 초대 적립 · 코칭 거리 (2026-10-19 점검)
--
--  1. 신발은 거리 · 걸음과 상관없이 달린 시간(멈춘 시간을 뺀 값) 1분마다 5 닳는다(2026-10-19 사용자 결정 "걍 1분마다 5
--     닳는거로 바꿔 어떤식으로든간데"). 예전엔(0064 ~ 0066) 거리가 0 인 러닝은 닳지 않았다 — 켜 두고 움직이지 않아도 이제 닳는다.
--     무효 러닝은 예전처럼 닳지 않는다. 앱은 걸음이 0 이어도 시간이 있는 러닝을 올린다.
--  2. 하루 금액 · 경로 없는 러닝 · 올린 날 상한에 걸려 덜 받은 러닝도 에너지는 다 썼다(에너지를 상한보다 먼저 깎는다).
--     이제 받은 몫만큼만 쓴다 — 쓴 에너지 × 받은 SUP ÷ 상한 전 SUP. 다 닳은 신발(남은 내구도 0)은 0066 의 수명 자르기로
--     이미 적립 걸음이 0 이라 에너지를 쓰지 않는다.
--  3. 러닝은 시작한 날(한국 시각)의 목표에 드는데 목표 보너스(goal_claim)는 오늘만 봤다. 23:30 ~ 00:10 러닝이나 자정 뒤에
--     올린 러닝은 그날 보너스와 연속 일수를 잃었다. goal_claim_day(날)는 러닝을 올릴 수 있는 최근 7일 안의 날을 받는다 —
--     금액 · 규칙 · 꼬리표('goal:' || 날)가 goal_claim 과 같아 어느 길로도 두 번 받지 못한다. goal_claim() 은 예전 앱을 위해
--     남기고(오늘의 날로 부른다) 규칙은 goal_claim_day 한 곳에 둔다.
--  4. 초대 적립(두 사람 100 SUP 씩)이 경로 없는 러닝(폰이 센 걸음 1,000)에도 나갔다. 이제 경로가 받쳐 준 러닝(gps_backed)의
--     걸음(backed_steps)이 1,000 이상이어야 한다.
--  5. AI 코칭(0060)의 거리를 걸음 거리(걸음 × 0.762)에서 러닝의 인정 거리(walk_sessions.distance_meters — 경로가 100m 이상이면
--     경로 거리, 아니면 걸음 거리. record_session 이 정한다)로 바꾼다. 앱도 기록 거리 · 1km 랩을 같은 거리로 바꾼다.
--     폰이 잰 거리는 서버 거리와 조금 다를 수 있어 랩 수는 서버 거리의 정수 km 에서 ±1 까지 받는다.
--
--  그대로인 것: 판정(CLEAN · FLAGGED · VOID, 0065 과속 금지 · 과속 정지) · 적립 금액 · 상한 · 1분마다 5 · 무효 러닝은
--  닳지 않음 · 신발 수명 자르기(0066, 달린 시간과 견준다) · 수리 값 · 목표 보너스 금액(1,000보당 2.5 · 연속 하루 +10%,
--  7일까지) · 초대 적립액(0038) · 코칭의 판정 · 한도 · 출력 검사.
-- ════════════════════════════════════════════════════════════════════

-- ── 1 · 2 — 러닝 기록 ────────────────────────────────────────────────

-- 0066 그대로(0065 과속 · 0066 신발 수명) + 거리와 상관없이 달린 1분마다 5 · 상한에 걸린 러닝은 받은 만큼만 에너지
create or replace function public.record_session(
  p_started_at timestamptz,
  p_ended_at timestamptz,
  p_steps int,
  p_duration_sec int,
  p_track text default '',
  p_boost_bps int default 0,
  p_party_size int default 1,
  p_faction text default '',
  -- 안드로이드가 "모의 위치"로 표시한 좌표가 하나라도 있었나
  p_mock_location boolean default false
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric
)
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_user uuid := auth.uid();
  v_gday date;
  v_already_steps int;
  v_already_points numeric;
  v_verified int;
  v_rewardable int;
  v_points numeric(20, 4);
  v_cap numeric;
  v_verdict text := 'CLEAN';
  v_reason text := '';
  v_session_id bigint;
  v_kind text;
  v_elapsed int;
  v_top_speed double precision := 0;
  v_glitch double precision := 0;
  v_gps_m double precision := 0;
  v_points_n int := 0;
  v_first_at bigint;
  v_last_at bigint;
  v_step_m double precision;
  v_distance_m double precision;
  v_track text := coalesce(p_track, '');
  v_party int := 1;
  v_party_id bigint;
  v_gps_backed boolean;
  v_credit_m double precision := 0;
  v_cap_left numeric;
  v_shoe public.market_sneakers;
  v_eff int := 0;
  v_comfort int := 0;
  v_dur numeric := 100;
  v_energy_left numeric;
  v_energy_per_step numeric;
  v_energy_used numeric := 0;
  v_xp numeric := 1;
  v_recent_void int;
  -- 무효가 잦아 보류 중 — 목표 보너스 · 주간 도전 · 코스 보상에도 이 러닝을 세지 않는다
  v_held boolean := false;
  v_new_account boolean;
  -- 상한(하루 금액 · 경로 없는 러닝 · 올린 날)을 걸기 전 금액 — 상한에 걸려 덜 받은 만큼 에너지도 덜 쓴다 (0068)
  v_points_full numeric := 0;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  -- ── 형식 검사 ──
  if p_steps is null or p_steps < 0 then
    raise exception '걸음 수가 올바르지 않습니다' using errcode = '22023';
  end if;
  if p_ended_at < p_started_at then
    raise exception '종료 시각이 시작보다 빠릅니다' using errcode = '22023';
  end if;
  if p_ended_at > now() + interval '5 minutes' then
    raise exception '종료 시각이 미래입니다' using errcode = '22023';
  end if;
  if p_started_at < now() - interval '7 days' and not exists (
       select 1 from public.walk_sessions s
        where s.user_id = v_user and s.started_at = p_started_at) then
    raise exception '너무 오래된 러닝입니다' using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  -- 이미 올린 러닝의 재시도면 원래 결과를 그대로 돌려준다. 아래 계산을 다시 하면
  -- 에너지 · 내구도를 두 번 깎는다.
  select s.id, s.verdict, s.points_awarded into v_session_id, v_verdict, v_points
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
    return;
  end if;
  v_verdict := 'CLEAN';

  v_elapsed := least(
    greatest(coalesce(p_duration_sec, 0), 0),
    floor(extract(epoch from (p_ended_at - p_started_at)))::int
  );
  v_gday := economy.game_day(p_started_at);
  v_step_m := p_steps * 0.762;

  select t.top_speed_kmh, t.glitch_ratio into v_top_speed, v_glitch
    from economy.track_speed_stats(v_track) t;
  select t.gps_m, t.points, t.first_at, t.last_at
    into v_gps_m, v_points_n, v_first_at, v_last_at
    from economy.track_summary(v_track) t;
  -- 경로 거리는 러닝 시간 안에 달릴 수 있는 만큼만(시속 25km). 경로 시각은 앞뒤로 여유를
  -- 두므로, 아주 짧은 러닝 여러 개에 같은 몇 분짜리 경로를 붙여 거리를 여러 번 받지 못하게.
  v_gps_m := least(coalesce(v_gps_m, 0), greatest(v_elapsed, 1) * 7.0);

  -- ── 판정 ──
  if coalesce(p_mock_location, false) then
    v_verdict := 'VOID';
    v_reason := '가짜 위치가 감지되었습니다';

  -- 과속으로 멈춘 뒤 10분(러닝 금지 시간) 안에 시작한 러닝은 적립하지 않는다 (0065)
  elsif exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user
           and p_started_at >= x.stopped_at and p_started_at < x.banned_until) then
    v_verdict := 'VOID';
    v_reason := '과속으로 멈춘 뒤 러닝 금지 시간에 시작한 러닝입니다';

  -- 짧은 러닝도 실제 시간으로 잰다(예전엔 60초로 쳐서 1ms 러닝에 240걸음을 줬다)
  elsif p_steps::numeric * 60 / greatest(v_elapsed, 1) > 240 then
    v_verdict := 'VOID';
    v_reason := '케이던스가 사람 범위를 벗어납니다';

  elsif v_glitch > 0.5 then
    v_verdict := 'VOID';
    v_reason := '이동 속도가 사람 범위를 벗어납니다';

  elsif v_points_n >= 2 and (
        v_first_at < (extract(epoch from p_started_at) * 1000)::bigint - economy.track_time_slack_ms()
     or v_last_at > (extract(epoch from p_ended_at) * 1000)::bigint + economy.track_time_slack_ms()) then
    v_verdict := 'VOID';
    v_reason := '경로 시각이 러닝 시간과 맞지 않습니다';

  elsif v_track <> '' and exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.track <> ''
           and md5(s.track) = md5(v_track) and s.started_at <> p_started_at) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로입니다';

  -- 같은 사람의 러닝은 시간이 겹칠 수 없다. 겹치게 여러 개를 올리면 같은 시간을
  -- 여러 번 받는다.
  elsif exists (
        select 1 from public.walk_sessions s
         where s.user_id = v_user and s.verdict <> 'VOID'
           and s.started_at < p_ended_at and s.ended_at > p_started_at) then
    v_verdict := 'VOID';
    v_reason := '다른 러닝과 시간이 겹칩니다';

  elsif v_gps_m >= 1000 and v_step_m < v_gps_m * 0.2 then
    v_verdict := 'VOID';
    v_reason := '걸음 없이 이동한 거리입니다';
  end if;

  -- ── 검사를 통과한 걸음 ──
  if v_verdict = 'VOID' then
    v_verified := 0;
  else
    v_verified := p_steps;
    if v_gps_m >= economy.gps_check_min_m()
       and v_step_m > v_gps_m * economy.step_gps_max_ratio() then
      v_verified := floor(v_gps_m * economy.step_gps_max_ratio() / 0.762)::int;
      v_verdict := 'FLAGGED';
      v_reason := '걸음 수가 GPS 거리보다 많습니다';
    end if;
  end if;
  v_rewardable := v_verified;

  -- 과속이 30초 넘게 이어져 앱이 멈춘 러닝 — 적립 규칙은 그대로, 판정에 까닭을 남긴다 (0065)
  if v_verdict <> 'VOID' and exists (
        select 1 from public.run_penalties x
         where x.user_id = v_user and x.run_started_at = p_started_at) then
    v_verdict := 'FLAGGED';
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '과속이 이어져 러닝이 멈췄습니다');
  end if;

  -- ── 하루 걸음 상한 (한국 하루) ──
  if v_rewardable > 0 then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and economy.game_day(s.started_at) = v_gday;
    if v_rewardable > economy.max_daily_steps() - v_already_steps then
      v_rewardable := greatest(economy.max_daily_steps() - v_already_steps, 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 무효가 잦으면 보류 ──
  -- 적립할 걸음이 없는 러닝도 본다 — 걸음 0 인 경로 러닝이 보류 중에도 잠금 거리 · 꺼내기 조건 거리를 받았다
  if v_verdict <> 'VOID' then
    select count(*) into v_recent_void from public.walk_sessions s
     where s.user_id = v_user and s.verdict = 'VOID'
       and s.started_at > now() - interval '7 days';
    if v_recent_void >= 5 then
      v_rewardable := 0;
      v_held := true;
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '최근 무효 러닝이 많아 적립을 보류합니다');
    end if;
  end if;

  -- ── 신발 · 에너지 ──
  select * into v_shoe from economy.equipped_sneaker(v_user);
  if not found then
    -- 신은 신발이 없으면(꺼내기로 벗었다) 첫 신발을 신긴다. 신발 없이 달리면 내구도 100 · 닳지 않음으로
    -- 쳐서, 닳은 신발을 신고 수리비를 내는 것보다 나았다. 첫 신발을 아직 안 받은 계정(앱은 로그인하자마자
    -- 받는다)은 예전처럼 신발 없이 계산한다 — 앱이 받기 전에 러닝이 먼저 올라가도 적립을 잃지 않게.
    update public.market_sneakers set equipped = true, updated_at = now()
     where id = (select m.id from public.market_sneakers m
                  where m.owner_id = v_user and m.origin = 'STARTER'
                    and m.chain_state = 'APP' and m.status = 'OWNED'
                  limit 1)
       and not exists (select 1 from public.market_sneakers e where e.owner_id = v_user and e.equipped);
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  if v_shoe.id is not null then
    -- 효율 · 착화감은 레벨 1 값 + 나눈 포인트, 내구도는 남은 비율(%) — 이 러닝 전의 값으로 배율을 정한다 (0059)
    select e.efficiency_bps, e.comfort_bps, e.durability into v_eff, v_comfort, v_dur
      from economy.shoe_effective(v_shoe) e;
  end if;

  -- 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 (0066) — 남은 내구도로 달릴 수 있던 시간(1분마다 5)만큼만.
  -- 적립 배율은 남은 비율이 0 보다 크면 그대로라(economy.durability_factor) 거의 닳은 신발로 오래 달려도 다 받던 것을 막는다
  if v_rewardable > 0 and v_shoe.id is not null
     and v_shoe.dur_now * economy.durability_sec_per_point() < v_elapsed then
    v_rewardable := floor(v_rewardable * v_shoe.dur_now * economy.durability_sec_per_point()
                          / greatest(v_elapsed, 1)::numeric)::int;
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
  end if;

  if v_rewardable > 0 then
    v_energy_left := economy.energy_left(v_user, v_gday);
    v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
    if v_rewardable * v_energy_per_step > v_energy_left then
      v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
    end if;
    v_energy_used := round(v_rewardable * v_energy_per_step, 4);
  end if;

  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
  end if;
  if economy.boost_active(v_user, 'XP_BOOSTER', p_started_at) then
    v_xp := 2;
  end if;

  v_points := round(
    v_rewardable * economy.points_per_step()
      * (1 + v_eff / 10000.0)
      * economy.durability_factor(v_dur)
      * economy.party_multiplier(v_party)
      * v_xp,
    4
  );
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  if v_points > 0 then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap - v_already_points then
      v_points := greatest(round(v_cap - v_already_points, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 적립 금액 상한에 걸렸습니다');
    end if;
  end if;

  -- ── 걸음만 있는 러닝 · 한꺼번에 올린 러닝 ──
  --
  -- 경로가 없으면 걸음은 폰이 지어낼 수 있다. 그런 러닝은 하루 적은 몫만 준다.
  -- 7일치를 모아 한 번에 올리면 날마다 상한이 새로 열리므로, 올린 날 기준 상한도 둔다.
  v_gps_backed := v_verdict <> 'VOID' and v_gps_m >= economy.gps_check_min_m();
  if v_points > 0 and not v_gps_backed then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
  end if;

  -- 상한에 걸려 덜 받았으면 받은 몫만큼만 에너지를 쓴다 — 쓴 에너지 × 받은 SUP ÷ 상한 전 SUP (0068).
  -- 예전엔 하루 금액 상한에 걸려 0 SUP 를 받은 러닝도 에너지를 다 썼다. 적립 걸음(rewarded_steps)은 그대로 둔다
  if v_energy_used > 0 and v_points < v_points_full then
    v_energy_used := round(v_energy_used * v_points / v_points_full, 4);
  end if;

  v_distance_m := case
    -- 걸음이 있어야 100m 여유를 준다 — 걸음 0 인 경로만으로 거리를 받지 않게
    when v_gps_m >= 100 then least(v_gps_m, greatest(v_verified, 0) * 0.762 * 3
                                          + case when v_verified > 0 then 100 else 0 end)
    else v_step_m
  end;
  if v_verdict = 'VOID' then v_distance_m := 0; end if;

  -- 잠금 거리 · 꺼내기 조건에 쳐 주는 거리 — 경로로 잰 것만, 하루 상한 안에서
  if v_gps_backed and not v_held then
    select greatest(economy.setting_num('gps_km_daily_cap') * 1000 - coalesce(sum(s.gps_credit_m), 0), 0)
      into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and economy.game_day(s.started_at) = v_gday;
    v_credit_m := least(v_distance_m, v_cap_left);
  end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  if v_energy_used > 0 then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064). 달린 시간은 멈춘 시간을 뺀 값(v_elapsed).
  -- 거리 · 걸음과 상관없다 — 켜 두고 움직이지 않아도 닳는다(0068, 2026-10-19 사용자 결정. 예전엔 거리 0 이면 닳지 않았다).
  -- 무효 러닝은 닳지 않는다. 남은 비율(durability_pts)은 트리거가 다시 센다
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0),
           updated_at = now()
     where s.id = v_shoe.id;
  end if;

  if v_verdict <> 'VOID' then
    update public.profiles
       set lifetime_km = lifetime_km + (v_distance_m / 1000),
           gps_km = gps_km + (v_credit_m / 1000),
           top_speed_kmh = greatest(top_speed_kmh, v_top_speed)
     where id = v_user;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user);
end;
$$;

comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발·에너지·파티·부스터·상한 모두 서버 값으로.';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean)
  to authenticated;

-- ── 3 — 목표 보너스: 러닝을 시작한 날로 ─────────────────────────────
/*
 * [p_day](한국 시각 게임의 하루)의 목표 보너스 — 0024 goal_claim 의 규칙 그대로, 오늘 대신 그날로.
 * 걸음은 그날 시작한 러닝의 서버가 확인한 걸음, 연속 일수는 그 전날부터 거꾸로 센다. 올릴 수 있는 기간(7일) 안의 날만.
 * 앱은 러닝이 올라가면 그 러닝들이 시작한 날마다(오래된 날부터) 부른다 — 자정을 넘긴 러닝도 그날 보너스를 받는다.
 */
create or replace function public.goal_claim_day(p_day date)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
  v_goal int;
  v_streak int := 1;
  v_day date;
  v_bonus numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  -- 러닝을 올릴 수 있는 기간(시작 7일 안)의 날만 — 미래 · 그보다 오래된 날은 받지 않는다
  if p_day is null or p_day > v_today or p_day < v_today - 7 then
    raise exception '목표 보너스는 최근 7일 안의 날만 받을 수 있습니다' using errcode = '22023';
  end if;

  select least(greatest(coalesce(p.daily_goal, 8000), 1000), 30000) into v_goal
    from public.profiles p where p.id = v_user;

  if economy.verified_steps_on(v_user, p_day) < v_goal then
    raise exception '아직 목표를 채우지 않았습니다' using errcode = '23514';
  end if;

  -- 그 전날부터 거꾸로 — 보너스를 받았거나 스트릭 보호가 덮은 날이 이어진 만큼
  v_day := p_day - 1;
  while v_streak < 8 loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day)
      or exists (select 1 from public.boosts b
                  where b.user_id = v_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(v_day + 1)
                    and b.ends_at > economy.game_day_start(v_day)));
    v_streak := v_streak + 1;
    v_day := v_day - 1;
  end loop;

  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  -- 같은 날 두 번 받으면 ref 가 겹쳐 23505 로 거절된다 — goal_claim()(오늘)과 같은 꼬리표라 어느 길로도 두 번 받지 못한다.
  perform economy.ledger_apply(v_user, 'BONUS_GOAL', v_bonus,
                               format('목표 달성 보너스 (%s일 연속)', v_streak), 'goal:' || p_day);
  -- 지난날의 보너스는 그날의 마지막 시각으로 적는다 — 러닝 적립이 러닝 끝 시각으로 적히는 것처럼. 자정 뒤에 받은
  -- 어제 보너스가 앱의 "오늘 받은 목표 보너스"로 보이지 않게
  if p_day < v_today then
    update public.sup_ledger l set occurred_at = economy.game_day_start(p_day + 1) - interval '1 second'
     where l.user_id = v_user and l.ref = 'goal:' || p_day;
  end if;
  return v_bonus;
end $$;

revoke all on function public.goal_claim_day(date) from public, anon;
grant execute on function public.goal_claim_day(date) to authenticated;

-- 예전 앱 — 오늘(한국 시각)의 목표 보너스. 규칙은 goal_claim_day 한 곳에 둔다
create or replace function public.goal_claim()
returns numeric
language plpgsql security definer set search_path = public, economy as $$
begin
  return public.goal_claim_day(economy.game_day(now()));
end $$;

revoke all on function public.goal_claim() from public, anon;
grant execute on function public.goal_claim() to authenticated;

-- ── 4 — 초대 적립: 경로가 받쳐 준 러닝만 ────────────────────────────
-- 0039 그대로(적립이 실패해도 러닝은 저장 · 이미 적힌 쪽은 건너뜀) + 경로가 받쳐 준 걸음 1,000 이상
create or replace function economy.invite_reward_after_session()
returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_r public.invite_redemptions;
  v_cfg economy.invite_config;
  v_amount numeric(20, 4);
begin
  if new.verdict = 'VOID' then
    return null;
  end if;
  select * into v_r from public.invite_redemptions r
   where r.invitee = new.user_id and r.rewarded_at is null
   for update;
  if not found then
    return null;
  end if;
  select * into v_cfg from economy.invite_config where id;
  v_amount := coalesce(v_cfg.reward_sup, 0);
  if v_amount <= 0 or coalesce(new.steps, 0) < coalesce(v_cfg.min_steps, 1000) then
    return null;
  end if;
  -- 경로가 받쳐 준 러닝(gps_backed — 경로 300m 이상)의 걸음(backed_steps)이 1,000 이상이어야 한다 (0068).
  -- 걸음은 폰이 지어낼 수 있어, 경로 없는 걸음만으로 두 사람이 100 SUP 씩 받았다. 확정을 적지 않으므로
  -- 경로가 있는 다음 러닝에서 받는다
  if not coalesce(new.gps_backed, false) or coalesce(new.backed_steps, 0) < coalesce(v_cfg.min_steps, 1000) then
    return null;
  end if;
  -- 적립이 어떤 까닭으로든 실패해도 러닝 저장은 막지 않는다. 확정을 적지 않으므로 다음 러닝에서 다시 시도한다.
  begin
    -- 이미 적힌 쪽은 건너뛴다(같은 ref 로 다시 적으면 영원히 실패한다) — 빠진 쪽만 적고 확정한다
    if not exists (select 1 from public.sup_ledger l
                    where l.user_id = new.user_id and l.ref = 'invite:joined') then
      perform economy.ledger_apply(new.user_id, 'EARN_INVITE', v_amount, '친구 초대 — 첫 러닝', 'invite:joined');
    end if;
    if (select count(*) from public.invite_redemptions r
         where r.inviter = v_r.inviter and r.rewarded_at is not null) < v_cfg.max_rewards_per_inviter
       and not exists (select 1 from public.sup_ledger l
                        where l.user_id = v_r.inviter and l.ref = 'invite:' || new.user_id::text) then
      perform economy.ledger_apply(v_r.inviter, 'EARN_INVITE', v_amount,
        '친구 초대 — 초대한 친구의 첫 러닝', 'invite:' || new.user_id::text);
    end if;
    update public.invite_redemptions set rewarded_at = now(), reward_sup = v_amount
     where invitee = new.user_id;
  exception when others then
    raise warning '초대 적립 실패(러닝은 저장됨): % %', sqlstate, sqlerrm;
  end;
  return null;
end $$;
revoke all on function economy.invite_reward_after_session() from public;

-- ── 5 — AI 코칭 거리: 러닝의 인정 거리로 ────────────────────────────
-- 한 주의 횟수 · 거리 · 시간 — 거리는 러닝의 인정 거리(distance_meters)
create or replace function coaching.week_totals(p_user uuid, p_monday date, p_tz text,
  out runs int, out meters numeric, out seconds bigint)
  language sql stable security definer set search_path = public, coaching as $$
  select count(*)::int, coalesce(sum(s.distance_meters), 0)::numeric, coalesce(sum(s.duration_sec), 0)::bigint
    from public.walk_sessions s, coaching.week_bounds(p_monday, p_tz) b
   where s.user_id = p_user and s.started_at >= b.v_from and s.started_at < b.v_until and coaching.counts(s)
$$;
revoke all on function coaching.week_totals(uuid, date, text) from public;

-- 기록 하나의 입력 — 인정 거리 · 활성 시간 · 1km 구간(초)과 근거 · 후보. 좌표 · 날짜 · 사람 정보 없음
create or replace function coaching.run_input(s public.walk_sessions, p_laps int[], p_locale text) returns jsonb
  language plpgsql immutable set search_path = public, coaching as $$
declare
  v_m numeric := s.distance_meters::numeric;
  v_sec int := s.duration_sec;
  v_pace int := round(s.duration_sec / (v_m / 1000))::int;
  v_n int := coalesce(cardinality(p_laps), 0);
  v_splits int[] := '{}';
  v_prev int := 0;
  v_facts jsonb := '[]'::jsonb;
  v_cands jsonb := '[]'::jsonb;
  v_delta int;
  v_spread int;
  v_target int := (round(round(s.duration_sec / (v_m / 1000)) / 5.0) * 5)::int;
  v_rest_m numeric;
  v_rest_s int;
begin
  for i in 1 .. v_n loop
    v_splits := v_splits || (p_laps[i] - v_prev);
    v_prev := p_laps[i];
  end loop;
  v_facts := v_facts || jsonb_build_object('id', 'run.avg_pace', 'values', jsonb_build_object('pace_sec', v_pace));
  if v_n >= 2 then
    v_delta := v_splits[v_n] - v_splits[1];
    select max(x) - min(x) into v_spread from unnest(v_splits) as x;
    if abs(v_delta) > coaching.even_split_sec() then
      v_facts := v_facts || jsonb_build_object('id', 'run.split_last_vs_first',
        'values', jsonb_build_object('last_km', v_n, 'delta_sec', v_delta));
    end if;
    if v_spread <= coaching.even_split_sec() then
      v_facts := v_facts || jsonb_build_object('id', 'run.split_even', 'values', jsonb_build_object('spread_sec', v_spread));
    end if;
    if v_delta > coaching.even_split_sec() then
      v_cands := v_cands || jsonb_build_object('id', 'pace.start_easier', 'values', jsonb_build_object('pace_sec', v_target));
    end if;
  end if;
  v_cands := v_cands || jsonb_build_object('id', 'pace.keep', 'values', jsonb_build_object('pace_sec', v_target));
  -- 폰의 랩이 서버 거리의 정수 km 보다 하나 많을 수 있다(±1 까지 받는다) — 남은 거리는 0 아래로 내려가지 않는다
  v_rest_m := greatest(v_m - v_n * 1000, 0);
  v_rest_s := v_sec - v_prev;
  return jsonb_build_object(
    'kind', 'run', 'locale', p_locale, 'schema_version', coaching.schema_version(),
    'run', jsonb_build_object(
      'distance_m', round(v_m)::int, 'duration_sec', v_sec, 'avg_pace_sec', v_pace,
      'splits_sec', to_jsonb(v_splits),
      'partial', case when v_rest_m >= coaching.min_partial_m() and v_rest_s > 0
                      then jsonb_build_object('meters', round(v_rest_m)::int, 'seconds', v_rest_s) end),
    'facts', v_facts, 'candidates', v_cands);
end $$;
revoke all on function coaching.run_input(public.walk_sessions, int[], text) from public;

/*
 * 앱이 잰 1km 랩(누적 활성 초)을 서버의 그 러닝과 맞대어 보고 받는다 — 랩 수 = 서버 인정 거리의 정수 km ± 1(하나 이상),
 * 늘어나기만, 서버 활성 시간 안, 사람 범위. 먼저 받은 것을 바꾸지 않는다. 코칭에만 쓰는 값이라 동의가 있을 때만 받는다.
 * 'stored' · 'exists' · 'no_consent' · 'not_found' · 'not_a_run' · 'too_short' · 'mismatch' · 'out_of_range' · 'conflict'
 */
create or replace function public.coaching_splits_put(p_started_at timestamptz, p_laps int[])
returns text
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  s public.walk_sessions;
  v_whole int;
  v_n int;
  v_prev int := 0;
  v_old int[];
begin
  if not coaching.consented(v_user) then return 'no_consent'; end if;
  select * into s from public.walk_sessions w where w.user_id = v_user and w.started_at = p_started_at;
  if not found then return 'not_found'; end if;
  if not coaching.counts(s) then return 'not_a_run'; end if;
  v_whole := floor(s.distance_meters / 1000)::int;
  if v_whole < 1 then return 'too_short'; end if;
  -- 폰이 잰 거리는 서버 거리와 조금 다를 수 있다 — 랩 수는 서버 거리의 정수 km 에서 ±1 까지(하나 이상)
  v_n := coalesce(cardinality(p_laps), 0);
  if p_laps is null or v_n < 1 or abs(v_n - v_whole) > 1 or array_lower(p_laps, 1) <> 1 then return 'mismatch'; end if;
  for i in 1 .. v_n loop
    if p_laps[i] is null or p_laps[i] <= v_prev or p_laps[i] > s.duration_sec then return 'mismatch'; end if;
    if p_laps[i] - v_prev not between coaching.min_split_sec() and coaching.max_split_sec() then return 'out_of_range'; end if;
    v_prev := p_laps[i];
  end loop;
  insert into public.coaching_run_splits (session_id, user_id, laps) values (s.id, v_user, p_laps)
  on conflict (session_id) do nothing;
  if found then return 'stored'; end if;
  select laps into v_old from public.coaching_run_splits where session_id = s.id;
  return case when v_old = p_laps then 'exists' else 'conflict' end;
end $$;

/*
 * 기록 하나의 코칭 — 0060 그대로, 1km 미만은 인정 거리로 가린다.
 * 미동의 · 서버에 없는 러닝 · 무효 · 1km 미만 · 구간 없음은 작업을 만들지 않는다(공급자를 부르지 않는다).
 */
create or replace function public.coaching_run(p_started_at timestamptz, p_locale text, p_request boolean default false)
returns jsonb
language plpgsql security definer set search_path = public, coaching as $$
declare
  v_user uuid := coaching.user_required();
  v_locale text := coaching.locale_ok(p_locale);
  s public.walk_sessions;
  v_laps int[];
  latest public.coaching_results;
begin
  if not coaching.consented(v_user) then return coaching.status('no_consent'); end if;
  select * into s from public.walk_sessions w where w.user_id = v_user and w.started_at = p_started_at;
  if not found then return coaching.status('not_found'); end if;
  if not coaching.counts(s) then return coaching.status('skipped', p_reason => 'not_a_run'); end if;
  if s.distance_meters < 1000 then return coaching.status('skipped', p_reason => 'too_short'); end if;
  select laps into v_laps from public.coaching_run_splits where session_id = s.id;
  if v_laps is null then
    select * into latest from public.coaching_results x
     where x.user_id = v_user and x.kind = 'run' and x.source_key = 'session:' || s.id and x.locale = v_locale
     order by x.created_at desc limit 1;
    return coaching.status('skipped', p_reason => 'no_splits', p_result => coaching.result_json(latest));
  end if;
  return coaching.status_for(v_user, 'run', 'session:' || s.id, v_locale, s.id,
    coaching.run_input(s, v_laps, v_locale), p_request);
end $$;

revoke all on function public.coaching_splits_put(timestamptz, int[]) from public, anon;
revoke all on function public.coaching_run(timestamptz, text, boolean) from public, anon;
grant execute on function public.coaching_splits_put(timestamptz, int[]) to authenticated;
grant execute on function public.coaching_run(timestamptz, text, boolean) to authenticated;
