-- ════════════════════════════════════════════════════════════════════
--  0090 — 크루 러닝 보상 상한 · 예전 신발 올리기 · 에너지는 달린 신발로 · 대기실 25초 · 체인 꺼내기 · 가스비 (2026-11-04)
--
--  보안 점검(로컬 DB 에서 재현)에서 나온 경제 구멍을 막는다. 사용자 "심각한 버그네. 다 수정해놔".
--  정직한 사용자의 보상은 아래 적은 곳 말고는 그대로다.
--
--  1. 크루 러닝 보상(record_session, 0089 → 이 파일)
--     · 서버가 본 만큼만 — 크루 보상 걸음은 서버가 5초마다 알림(crew_run_beat)으로 센 달린 초(active_sec, 공용 체력 0 인 초 빼고)
--       × 4 + 60초 여유까지. 알림이 한 번도 없으면(서버가 센 초 0) 크루 보상은 없다(러닝은 적는다, 까닭 "크루 러닝 신호가 없어 …").
--       예전엔 알림을 보내지 않는 폰은 공용 체력도 쓰지 않고, 폰이 말한 10시간 · 경로 없는 러닝을 상한 없이 다 받았다(1,557 SUP).
--     · 서버가 본 마지막 소식 + 60초 뒤의 기록은 크루 보상 · 경험치에서 뺀다 — 끝난 까닭(내가 끝냄 · 마지막 한 사람 · 소식 끊김)과
--       상관없이, 끝냄 알림을 잃어 아직 끝나지 않았어도.
--     · 2분(crew_beat_timeout_sec) 넘게 소식이 없는 참여는 서버가 끝낸다(terminal_reason TIMEOUT, 끝난 때 = 마지막 소식 + 60초) —
--       공용 체력을 정산할 때마다(누군가의 알림 · 크루 홈 읽기 · 출발 · 올리기). 끊긴 사람 때문에 남은 사람을 끝내지는 않는다.
--     · 크루 러닝 보상도 개인과 같은 하루 상한(걸음 48,000 · 금액 600(가입 7일 안 절반) · 올린 날)에 함께 센다
--       (사용자 결정 "합쳐서 600", 설정 crew_counts_in_daily_cap — 끄면 0070 처럼 크루 러닝은 상한 밖).
--       경로 없는 러닝의 하루 상한(60)은 설정과 상관없이 크루 러닝에도 건다. 인원 보너스 규칙('ALL')은 그대로.
--     · 경로가 너무 크면(2MB · 5만 점, 설정) 받지 않고, 한 시간에 60번(설정) 넘게 올리면 429 로 잠시 뒤 다시 올리게 한다.
--  2. 예전 폰 신발(market_import — IMPORT) — 새로 올리는 신발은 일반 · 남은 내구도 0(기념품). 예전 신발의 최대 내구도는 늘 일반 범위
--     (등급을 지어내 피니시 내구도를 받던 길). 출발 신발(p_sneaker) · 크루 공용 체력 기여(economy.party_shoe)에 쓰지 않는다.
--     이미 올라온 예전 신발도 한 번 맞춘다(레드라인 · 피니시는 폰에 없던 등급이라 일반으로, 최대 내구도는 일반 범위로).
--  3. 개인 러닝의 에너지 최대치는 달린 신발(v_shoe — 효율 · 닳음과 같은 신발)의 레벨로(0089 는 올릴 때 신은 신발).
--  4. 대기실 출발 때 자리에 있다고 보는 시간 15 → 25초(설정 party_presence_sec).
--  5. 체인 꺼내기(sup_withdraw_request) · 상급 뽑기(premium_draw)는 서버 설정으로도 막는다(기본 꺼짐 — 스토어 판은 앱에서 숨길 뿐이었다).
--     꺼내기는 50 SUP 부터 · 하루 3번까지(설정).
--  6. 새 지갑 가스비(어테스터 drip)는 가입 3일 · 경로 있는 러닝 1번 이상 · 하루 전체 50번까지(attester_drip_claim).
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  -- 1. 크루 러닝
  ('crew_counts_in_daily_cap',   'true'::jsonb),
  ('crew_beat_timeout_sec',      '120'::jsonb),
  ('record_session_hourly_cap',  '60'::jsonb),
  ('track_max_bytes',            '2000000'::jsonb),
  ('track_max_points',           '50000'::jsonb),
  -- 4. 대기실
  ('party_presence_sec',         '25'::jsonb),
  -- 5. 체인 꺼내기 · 상급 뽑기
  ('chain_withdraw_enabled',     'false'::jsonb),
  ('premium_draw_enabled',       'false'::jsonb),
  ('withdraw_min_sup',           '50'::jsonb),
  ('withdraw_user_daily_count',  '3'::jsonb),
  -- 6. 새 지갑 가스비
  ('drip_min_account_days',      '3'::jsonb),
  ('drip_min_gps_runs',          '1'::jsonb),
  ('drip_global_daily',          '50'::jsonb)
on conflict (key) do nothing;

-- ── 1. 크루 러닝 — 도움 함수 ──────────────────────────────────────────

-- 서버가 본 달린 초(공용 체력 0 인 초 빼고)로 센 크루 보상 걸음 상한 — 1초에 4걸음(케이던스 상한 240/분) · 60초 여유
create or replace function economy.crew_observed_steps(p_active numeric, p_empty numeric) returns int
language sql immutable set search_path = '' as $$
  select floor((greatest(coalesce(p_active, 0) - coalesce(p_empty, 0), 0) + 60) * 4)::int
$$;
revoke all on function economy.crew_observed_steps(numeric, numeric) from public;

-- 소식이 이만큼(초) 없으면 서버가 끝낸다 — 앱은 5초마다 알리고 서버는 30초 안의 소식만 달린 것으로 센다. 35초보다 짧게 두지 못한다
create or replace function economy.crew_beat_timeout_sec() returns int
language sql stable security definer set search_path = public, economy as $$
  select greatest(coalesce(economy.setting_num('crew_beat_timeout_sec'), 120), 35)::int
$$;
revoke all on function economy.crew_beat_timeout_sec() from public;

-- 공용 체력 정산 — 0071 그대로 + 소식이 끊긴 참여를 끝낸다(TIMEOUT).
-- 끝난 때는 마지막 소식 + 60초(그 뒤의 기록은 record_session 이 크루 보상에서 뺀다). 끊긴 사람 때문에 남은 사람을 끝내지 않는다 —
-- 마지막 한 사람 규칙(crew_run_close)은 누군가 스스로 끝내거나 올릴 때만 본다. 끝난 참여는 다시 살리지 않는다(crew_run_beat)
create or replace function economy.crew_pool_settle(p_crew uuid, p_day date, p_at timestamptz default now())
returns economy.crew_day_pools
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v economy.crew_day_pools;
  v_dt numeric;
  v_n int;
  v_rate numeric;
  v_need numeric;
  v_left numeric;
  v_each numeric;
  v_use numeric;
  v_from timestamptz;
  v_empty_dt numeric;
begin
  insert into economy.crew_day_pools (crew_id, day, settled_at) values (p_crew, p_day, p_at)
  on conflict (crew_id, day) do nothing;
  select * into v from economy.crew_day_pools d where d.crew_id = p_crew and d.day = p_day for update;

  -- 0090 — 2분 넘게 소식이 없는 참여는 끝낸다(풀 → 사람 차례로 잠근다)
  update economy.crew_runs r
     set state = 'ENDED', ended_at = least(r.last_beat_at + interval '60 seconds', p_at), terminal_reason = 'TIMEOUT'
   where r.crew_id = p_crew and r.day = p_day and r.state <> 'ENDED'
     and r.last_beat_at < p_at - make_interval(secs => economy.crew_beat_timeout_sec());

  v_dt := extract(epoch from (p_at - v.settled_at));
  if v_dt <= 0 then
    return v;
  end if;
  v_dt := least(v_dt, 30);
  v_from := p_at - make_interval(secs => v_dt);
  select count(*)::int into v_n from economy.crew_runs r
   where r.crew_id = p_crew and r.day = p_day and r.state = 'RUNNING'
     and r.last_beat_at >= p_at - interval '30 seconds';
  if v_n > 0 then
    -- 1분에 5 = 12초에 1 — 나눗셈 한 번으로(5/60 을 먼저 반올림하면 오차가 쌓인다)
    v_rate := v_n / economy.durability_sec_per_point()::numeric;
    v_need := v_n * v_dt / economy.durability_sec_per_point()::numeric;
    v_left := greatest(v.capacity - v.consumed, 0);
    -- 사람마다 똑같이 — 풀에서 쓴 양은 사람마다 쓴 양의 합과 같다(반올림하지 않는다)
    v_each := least(v_need, v_left) / v_n;
    v_use := v_each * v_n;
    -- 이 사이 중 0 이던 초 — 처음부터 0 이면 전부, 이 사이에 0 이 됐으면 그 뒤만
    v_empty_dt := case when v_left <= 0 then v_dt
                       when v_need >= v_left then v_dt - v_left * economy.durability_sec_per_point() / v_n
                       else 0 end;
    update economy.crew_runs r
       set consumed = r.consumed + v_each,
           active_sec = r.active_sec + v_dt,
           empty_sec = r.empty_sec + v_empty_dt
     where r.crew_id = p_crew and r.day = p_day and r.state = 'RUNNING'
       and r.last_beat_at >= p_at - interval '30 seconds';
    v.consumed := v.consumed + v_use;
    -- 이 사이에 0 이 됐다 — 정확한 때부터 적립을 멈춘다
    if v_left > 0 and v_need >= v_left then
      v.consumed := v.capacity;
      v.exhausted_at := v_from + make_interval(secs => v_left * economy.durability_sec_per_point() / v_n);
      insert into economy.crew_pool_empty (crew_id, day, from_at) values (p_crew, p_day, v.exhausted_at)
      on conflict do nothing;
    -- 처음부터 0 인데 달리는 사람이 있다(기여 0 인 출발 · 다 쓴 뒤 열린 구간이 없는 경우) — 0 인 구간을 연다.
    -- 새 하루 0/0 은 화면에서 "새 하루"로 따로 보이지만(총량 0), 적립은 다 쓴 것과 같이 멈춘다
    elsif v_left <= 0 and not exists (select 1 from economy.crew_pool_empty x
                                       where x.crew_id = p_crew and x.day = p_day and x.to_at is null) then
      v.exhausted_at := coalesce(v.exhausted_at, v_from);
      insert into economy.crew_pool_empty (crew_id, day, from_at) values (p_crew, p_day, v_from)
      on conflict do nothing;
    end if;
    if v.low_at is null and v.capacity > 0 and v.capacity - v.consumed <= v.capacity * 0.2 then
      v.low_at := p_at;
    end if;
  end if;
  update economy.crew_day_pools d
     set consumed = v.consumed, settled_at = p_at, version = d.version + 1,
         exhausted_at = v.exhausted_at, low_at = v.low_at
   where d.crew_id = p_crew and d.day = p_day
  returning * into v;
  return v;
end;
$$;
revoke all on function economy.crew_pool_settle(uuid, date, timestamptz) from public;

-- ── 3. 에너지 — 신발 레벨로 센 남은 에너지 ───────────────────────────
-- economy.energy_left(신은 신발) 와 같은 셈을 레벨을 받아서 — 러닝은 달린 신발의 레벨로 센다
create or replace function economy.energy_left_level(p_user uuid, p_day date, p_level int) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select greatest(economy.energy_max_for_level(coalesce(p_level, 1))
                  + coalesce((select bonus - used from public.energy_days
                               where user_id = p_user and day = p_day), 0), 0)
$$;
revoke all on function economy.energy_left_level(uuid, date, int) from public;

-- ── 1 · 2 · 3. 러닝 기록 — 0089 그대로 + 위 고침 ───────────────────────
-- 돌려주는 칸 · 인자는 0089 와 같아 create or replace 로 바꾼다(권한도 그대로)
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
  p_mock_location boolean default false,
  -- 출발 때 신은 신발(market_sneakers.id, 0089) — 앱이 사실로만 알린다. 내 신발이 아니거나 신을 수 없는 신발이면 예전처럼 지금 신은 신발
  p_sneaker bigint default null
)
returns table (
  session_id bigint,
  verdict text,
  points_awarded numeric,
  balance numeric,
  -- 판정 까닭(0089) — 적립이 0 이거나 줄어든 까닭. 예전 앱은 모르는 칸이라 읽지 않는다
  verdict_reason text
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
  -- 크루 러닝(그 크루의 방 · 함께 달린 2명 이상 · 크루원)이면 그 크루 — 크루 체력 · 크루 레벨 거리 (0069)
  v_crew uuid;
  -- 이 러닝의 적립을 받쳐 준 체력 — PERSONAL 개인 에너지 · CREW 크루 체력(그날 나눠 받는 크루원만) (0069)
  v_pool text := 'PERSONAL';
  -- 크루 러닝 3판 (0070)
  v_party_starts timestamptz;
  v_crew_room boolean := false;
  v_crew_level int := 1;
  v_runners int := 0;
  v_pool_pts numeric := 0;
  v_pool_max numeric := 0;
  v_share numeric := 0;
  v_limit_sec int := 0;
  v_wall numeric;
  v_together_sec int := 0;
  v_crew_sec numeric := 0;
  v_crew_used numeric := 0;
  v_headcount numeric := 0;
  v_level_bonus numeric := 0;
  -- 크루 러닝 — 공용 체력 (0071)
  v_cr economy.crew_runs;
  v_is_cr boolean := false;
  v_base numeric := 0;
  v_head_sup numeric := 0;
  v_lvl_sup numeric := 0;
  v_tm double precision;
  v_em double precision;
  v_lm double precision;
  v_frac numeric := 1;
  -- 서버가 이미 끝낸 참여(마지막 한 사람 자동 종료 · 끝냄)면 그때 + 60초(폰 시계 차이) 뒤의 기록은 크루 보상 · 경험치에서 뺀다
  v_cut timestamptz;
  v_xp_frac numeric := 1;
  v_level_now int;
  v_level_was int;
  v_starters int := 1;
  v_xp_m numeric := 0;
  -- 0090 — 크루 러닝도 개인과 같은 하루 상한(걸음 · 금액 · 올린 날)에 함께 세는가(설정, 기본 예)
  v_crew_in_caps boolean := coalesce((economy.setting('crew_counts_in_daily_cap') #>> '{}')::boolean, true);
  -- 0090 — 서버가 본 달린 초로 센 크루 러닝 걸음 상한
  v_obs_steps int;
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
  select s.id, s.verdict, s.points_awarded, coalesce(s.verdict_reason, '') into v_session_id, v_verdict, v_points, v_reason
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
    return;
  end if;
  v_reason := '';
  v_verdict := 'CLEAN';

  -- ── 경로 크기 · 올리기 횟수 (0090) ──
  -- 경로는 아래에서 몇 번 처음부터 끝까지 읽는다. 아주 큰 경로 · 짧은 사이 많은 올리기로 서버를 붙잡지 못하게 읽기 전에 막는다.
  -- 앱은 2.5초마다 점 하나(약 35바이트) — 10시간 러닝도 1.3만 점 · 0.5MB 안팎이라 정직한 러닝은 닿지 않는다
  if octet_length(v_track) > coalesce(economy.setting_num('track_max_bytes'), 2000000)
     or length(v_track) - length(replace(v_track, ';', '')) + 1 > coalesce(economy.setting_num('track_max_points'), 50000) then
    raise exception '경로가 너무 깁니다' using errcode = '22023';
  end if;
  -- 한 시간에 올린 러닝 수(무효 포함) — 넘으면 429(앱은 잠시 뒤 다시 올린다, 러닝은 폰에 남는다).
  -- 새로 올리는 러닝은 시작이 7일 안이라 시작 시각 색인으로 좁힌 뒤 센다
  if (select count(*) from public.walk_sessions s
       where s.user_id = v_user and s.started_at > now() - interval '8 days'
         and s.created_at > now() - interval '1 hour')
     >= coalesce(economy.setting_num('record_session_hourly_cap'), 60) then
    raise exception '러닝을 너무 자주 올렸습니다. 잠시 뒤 다시 올립니다' using errcode = 'PT429', hint = 'rate_limited';
  end if;

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

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
    v_party_id := null;
    v_party := 1;
  end if;

  -- 크루 러닝(0071) — 이 러닝의 방에서 서버가 출발을 확정한 참여(economy.crew_runs, party_start)가 있어야 한다.
  -- 앱이 크루 러닝이라고 말해서가 아니다. 참여가 이미 정산됐으면(같은 방 · 같은 날 두 번째 러닝) 보통 러닝이다
  if v_party_id is not null then
    select p.crew_id into v_crew from public.parties p where p.id = v_party_id;
    v_crew_room := v_crew is not null;
    v_crew := null;
    if v_crew_room then
      select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.party_id = v_party_id;
      if found and v_cr.session_id is null and v_cr.crew_id is not null then
        v_is_cr := true;
      elsif found then
        v_party_id := null;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '오늘 크루 러닝을 이미 했습니다');
      end if;
      -- 크루 방의 보너스는 크루 러닝 보너스 하나다 — 크루 러닝이 아니면 예전 함께 달리기 보너스도 없다(겹치지 않게)
      if not v_is_cr then
        v_party := 1;
      end if;
    end if;
  end if;
  if v_is_cr then
    v_pool := 'CREW';
    v_crew := v_cr.crew_id;
    v_crew_level := v_cr.crew_level;
    v_party_starts := v_cr.started_at;
    select count(*)::int into v_starters from economy.crew_runs c where c.party_id = v_cr.party_id;
    v_party := v_starters;
    -- 0090 — 서버가 본 마지막 소식(crew_run_beat — 달리는 중 · 쉼) + 60초 뒤의 기록은 크루 보상 · 경험치에서 뺀다.
    -- 끝난 까닭(내가 끝냄 · 마지막 한 사람 · 소식 끊김 TIMEOUT)과 상관없이, 아직 끝나지 않았어도(끝냄 알림을 잃었다) 같다.
    -- 소식을 한 번도 보내지 않았으면 출발 + 60초 — 예전엔 끝난 까닭이 둘일 때만 잘라, 소식 없는 폰이 10시간 러닝을 다 받았다
    v_cut := least(coalesce(v_cr.ended_at, 'infinity'::timestamptz), v_cr.last_beat_at) + interval '60 seconds';
    -- 공용 체력을 지금까지 정산하고(풀 → 사람 차례로 잠근다) 이 러닝을 끝낸다 — 다른 사람이 모두 끝났으면 마지막 사람도
    perform economy.crew_pool_settle(v_cr.crew_id, v_cr.day, now());
    perform economy.crew_run_close(v_user, v_cr.day, 'RECORDED');
    -- 인원 보너스 — 2분이 지났으면 지금 정하고, 아니면 확인할 수 없다(D01)
    perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
    select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
    v_headcount := v_cr.bonus_pct;
    v_level_bonus := v_cr.level_pct;
  end if;

  -- ── 하루 걸음 상한 (한국 하루) — 개인 러닝. 크루 러닝 걸음도 함께 센다(0090, crew_counts_in_daily_cap — 끄면 0070 처럼 뺀다).
  -- 크루 러닝 자신은 공용 체력 · 신호로 걸음을 줄인 뒤 아래 크루 갈래에서 같은 상한을 본다
  if v_rewardable > 0 and v_pool = 'PERSONAL' then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and (v_crew_in_caps or s.energy_pool <> 'CREW')
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

  -- ── 신발 ──
  -- 크루 러닝은 출발 때 고른 신발(아직 내 것이면) — 효율 · 착화감은 그 신발 것 (0070)
  if v_pool = 'CREW' then
    -- 출발 때 고른 신발 — 기록에 적는 번호만. 효율은 출발 때 스냅샷(v_cr.shoe_eff_bps)으로 센다
    select x.* into v_shoe from public.market_sneakers x where x.id = v_cr.sneaker_id and x.owner_id = v_user;
  end if;
  -- 개인 러닝은 출발 때 신은 신발(앱이 알린 p_sneaker, 0089) — 올릴 때 신은 신발이 아니라 달린 신발로 닳고 효율을 센다.
  -- 내 것이고 신을 수 있는 신발(앱 안 · 갖고 있음 — 판매 중 · 체인 · 태운 신발은 아니다)일 때만. 아니면 예전처럼 지금 신은 신발
  if v_shoe.id is null and p_sneaker is not null then
    select x.* into v_shoe from public.market_sneakers x
     where x.id = p_sneaker and x.owner_id = v_user and x.chain_state = 'APP' and x.status = 'OWNED'
       -- 0090 — 폰이 정한 값으로 올라온 예전 신발(IMPORT · MINT)은 출발 신발로 받지 않는다(등급을 지어내 내구도를 부풀리던 길)
       and x.origin not in ('IMPORT', 'MINT');
  end if;
  if v_shoe.id is null then
    select * into v_shoe from economy.equipped_sneaker(v_user);
  end if;
  if v_shoe.id is null then
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

  if v_pool = 'CREW' then
    -- 크루 러닝은 개인 신발 내구도 · 개인 에너지를 쓰지 않는다(R02 · R11). 효율은 출발 때 고른 신발의 스냅샷(R04).
    -- 공용 체력이 0 이던 구간에 달린 걸음은 적립하지 않는다(R03) — 경로를 시간으로 갈라 그 구간의 거리만큼, 경로가 없으면 시간만큼
    v_dur := 100;
    v_eff := v_cr.shoe_eff_bps;
    v_comfort := 0;
    select t.total_m, t.empty_m, t.late_m into v_tm, v_em, v_lm
      from economy.track_meters_empty(v_track, v_cr.crew_id, v_cr.day, v_cut) t;
    if v_tm > 0 then
      v_frac := greatest(1 - v_em / v_tm, 0)::numeric;
      v_xp_frac := greatest(1 - v_lm / v_tm, 0)::numeric;
    else
      -- 경로가 없으면 시간으로. 서버가 끝낸 뒤의 시간은 빼고, 0 이던 몫은 서버가 센 달린 초 중 0 이던 초로(일시정지와 상관없다)
      v_xp_frac := case when v_cut is null or v_cut >= p_ended_at then 1
                        else least(greatest(extract(epoch from (v_cut - p_started_at)), 0)
                                   / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 1)::numeric end;
      if v_cr.active_sec > 0 then
        v_frac := greatest(1 - v_cr.empty_sec / v_cr.active_sec, 0) * v_xp_frac;
      else
        v_frac := greatest(v_xp_frac - economy.crew_empty_sec(v_cr.crew_id, v_cr.day, p_started_at, least(p_ended_at, coalesce(v_cut, p_ended_at)))
                                       / greatest(extract(epoch from (p_ended_at - p_started_at)), 1), 0);
      end if;
    end if;
    if v_rewardable > 0 and v_frac < 1 then
      v_rewardable := floor(v_rewardable * v_frac)::int;
      if v_frac < v_xp_frac then
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 체력을 다 썼습니다');
      end if;
    end if;
    if v_xp_frac < 1 then
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 러닝이 끝난 뒤의 기록은 크루 보상에서 뺐습니다');
    end if;
    -- 0090 — 서버가 본 만큼만: 5초마다 알림(crew_run_beat)으로 서버가 센 달린 초(active_sec, 공용 체력이 0 이던 초 빼고)
    -- × 4걸음(케이던스 상한 240/분) + 60초 여유. 폰이 말한 시작 · 끝 · 걸음만으로는 크루 보상을 받지 못한다.
    -- 소식이 한 번도 없었으면(서버가 센 초 0) 크루 보상은 없다 — 러닝은 그대로 적는다
    if v_cr.active_sec <= 0 then
      if v_rewardable > 0 then
        v_rewardable := 0;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 러닝 신호가 없어 적립하지 않았습니다');
      end if;
    else
      v_obs_steps := economy.crew_observed_steps(v_cr.active_sec, v_cr.empty_sec);
      if v_rewardable > v_obs_steps then
        v_rewardable := v_obs_steps;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '크루 러닝 신호가 끊긴 동안의 걸음은 적립하지 않았습니다');
      end if;
    end if;
    -- 하루 걸음 상한 — 개인 러닝과 함께(0090, crew_counts_in_daily_cap)
    if v_rewardable > 0 and v_crew_in_caps then
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
  else
    -- 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 (0066) — 남은 내구도로 달릴 수 있던 시간(1분마다 5)만큼만.
    -- 적립 배율은 남은 비율이 0 보다 크면 그대로라(economy.durability_factor) 거의 닳은 신발로 오래 달려도 다 받던 것을 막는다
    if v_rewardable > 0 and v_shoe.id is not null
       and v_shoe.dur_now * economy.durability_sec_per_point() < v_elapsed then
      v_rewardable := floor(v_rewardable * v_shoe.dur_now * economy.durability_sec_per_point()
                            / greatest(v_elapsed, 1)::numeric)::int;
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
    end if;

    -- 개인 에너지 — 적립 걸음 600 에 1, 착화감만큼 덜
    if v_rewardable > 0 then
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy();
      -- 0090 — 에너지 최대치는 달린 신발(v_shoe, 효율 · 닳음과 같은 신발)의 레벨로. 예전(0089)엔 올릴 때 신은 신발이라
      -- 레벨 높은 신발을 신고 올리면 다른 신발로 달린 러닝의 에너지가 늘었다. 신발 번호가 없으면(예전 앱) v_shoe 가 신은 신발 — 예전과 같다
      v_energy_left := case when v_shoe.id is null then economy.energy_left(v_user, v_gday)
                            else economy.energy_left_level(v_user, v_gday,
                                                           (select e.level from economy.shoe_effective(v_shoe) e)) end;
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
  end if;

  if economy.boost_active(v_user, 'XP_BOOSTER', p_started_at) then
    v_xp := 2;
  end if;

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    v_points := round(
      v_rewardable * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * economy.durability_factor(v_dur)
        * economy.party_multiplier(v_party)
        * v_xp,
      4
    );
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 0090 — 크루 러닝 보상도 개인과 같은 하루 금액 · 올린 날 상한에 함께 센다(사용자 결정 "합쳐서 600",
  -- 설정 crew_counts_in_daily_cap — 끄면 0070 처럼 크루 러닝은 상한 밖 · 개인 합에서도 뺀다).
  -- 경로 없는 러닝의 상한은 설정과 상관없이 크루 러닝에도 건다(걸음만으로는 폰이 지어낼 수 있다)
  if v_points > 0 and (v_pool = 'PERSONAL' or v_crew_in_caps) then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and (v_crew_in_caps or s.energy_pool <> 'CREW') and economy.game_day(s.started_at) = v_gday;
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
     where s.user_id = v_user and not s.gps_backed
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and (v_pool = 'PERSONAL' or v_crew_in_caps) then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and (v_crew_in_caps or s.energy_pool <> 'CREW')
       and economy.game_day(s.created_at) = economy.game_day(now());
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
  -- 0090 — 크루 보상이 상한에 걸렸으면 기본 · 인원 · 레벨 몫을 같은 비율로 줄인다(결과 화면의 세 몫의 합 = 원장)
  if v_pool = 'CREW' and v_points < v_points_full and v_points_full > 0 then
    v_head_sup := round(v_head_sup * v_points / v_points_full, 4);
    v_lvl_sup := round(v_lvl_sup * v_points / v_points_full, 4);
    v_base := greatest(v_points - v_head_sup - v_lvl_sup, 0);
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
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool
  )
  returning id into v_session_id;

  if v_points > 0 then
    v_kind := case when v_party > 1 then 'EARN_PARTY' else 'EARN_WALK' end;
    insert into public.sup_ledger (user_id, kind, amount, description, session_id, occurred_at)
    values (v_user, v_kind, v_points, format('러닝 세션 적립 (%s보)', v_rewardable),
            v_session_id, p_ended_at);
  end if;

  -- 개인 에너지는 개인 러닝만 쓴다 — 크루 러닝은 공용 체력(crew_runs · crew_day_pools)
  if v_energy_used > 0 and v_pool = 'PERSONAL' then
    insert into public.energy_days (user_id, day, used) values (v_user, v_gday, v_energy_used)
    on conflict (user_id, day) do update set used = public.energy_days.used + excluded.used;
  end if;

  -- 크루 레벨 — 경험치는 유효 크루 러닝의 사람마다 자기 거리(R09). 보류 · 무효 · 크루를 나간 뒤는 쌓지 않는다. 레벨은 내려가지 않고,
  -- 넘은 레벨마다 레벨 업을 한 번 적는다. 정산은 참여 한 줄에 한 번(같은 러닝을 다시 올리면 위에서 원래 결과를 돌려준다)
  if v_pool = 'CREW' then
    select coalesce(c.level, 1) into v_level_was from public.crews c where c.id = v_crew for update;
    v_level_now := v_level_was;
    -- 서버가 끝낸 뒤의 거리는 빼고(v_xp_frac)
    if v_verdict <> 'VOID' and not v_held and v_distance_m * v_xp_frac > 0
       and exists (select 1 from public.crew_members m where m.crew_id = v_crew and m.user_id = v_user) then
      update public.crews c
         set run_km = c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3),
             level = greatest(coalesce(c.level, 1),
                              economy.crew_level_for_km(c.run_km + round((v_distance_m * v_xp_frac / 1000)::numeric, 3)))
       where c.id = v_crew
      returning c.level into v_level_now;
      v_xp_m := round((v_distance_m * v_xp_frac)::numeric, 1);
      insert into economy.crew_level_events (crew_id, level, session_id, run_km)
      select v_crew, l, v_session_id, (select run_km from public.crews where id = v_crew)
        from generate_series(v_level_was + 1, v_level_now) l
      on conflict (crew_id, level) do nothing;
    end if;
    update economy.crew_runs c
       set session_id = v_session_id, base_sup = case when v_points > 0 then v_base else 0 end,
           headcount_sup = case when v_points > 0 then v_head_sup else 0 end,
           level_sup = case when v_points > 0 then v_lvl_sup else 0 end, total_sup = v_points,
           xp_m = v_xp_m,
           level_before = v_level_was, level_after = v_level_now, settled_at = now()
     where c.user_id = v_user and c.day = v_cr.day;
  end if;

  -- 신발이 닳는다. 예전 신발(IMPORT·MINT)도 똑같이 닳는다 — 안 닳으면 수리비를 안 내는 신발이 된다.
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064). 달린 시간은 멈춘 시간을 뺀 값(v_elapsed).
  -- 거리 · 걸음과 상관없다 — 켜 두고 움직이지 않아도 닳는다(0068, 2026-10-19 사용자 결정. 예전엔 거리 0 이면 닳지 않았다).
  -- 무효 러닝은 닳지 않는다. 남은 비율(durability_pts)은 트리거가 다시 센다.
  -- 크루 러닝은 크루 체력을 쓰고 신발은 닳지 않는다 — 거리만 쌓는다 (0070, 사용자 결정 "하루에 내 자유러닝 + 크루러닝")
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0) end,
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

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
end;
$$;

comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것 · 예전 폰 신발 아님)·에너지(달린 신발 레벨)·크루 러닝(서버가 본 달린 초 · 하루 상한 함께)·파티·부스터·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0090)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;

-- ── 4. 출발 — 0087 그대로 + 자리에 있다고 보는 시간은 설정(기본 25초) ──────────────
-- 대기실은 2초마다 묻지만, 화면 전환 · 잠깐 끊긴 연결로 15초를 넘겨 준비한 사람이 빠지던 것(사용자 결정 25초)
create or replace function public.party_start(p_party bigint)
returns void
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_starts timestamptz;
  v_party public.parties;
  v_day date;
  v_level int;
  v_n int;
  v_added numeric;
  v_pool economy.crew_day_pools;
begin
  select * into v_party from public.parties where id = p_party and host_id = auth.uid() and status = 'LOBBY' for update;
  if not found then
    raise exception '로비에서 방장만 출발할 수 있습니다' using errcode = '42501';
  end if;
  -- 크루 방은 6초 뒤 — 대기실은 2초마다 묻는다. 늦게 안 사람도 3 · 2 · 1 · 0 · StepUP! 을 처음부터 본다(0072). 번개러닝은 예전 그대로 4초
  v_starts := now() + case when v_party.crew_id is not null then interval '6 seconds' else interval '4 seconds' end;
  if not exists (
    select 1 from public.party_members
     where party_id = p_party and user_id = auth.uid() and ready
  ) then
    raise exception '방장이 먼저 준비해야 합니다' using errcode = '23514';
  end if;

  -- 진행자는 지금 누르고 있다. 다른 사람은 party_presence_sec(기본 25초, 0090) 안에 대기실에 물어 온 사람만 함께 출발한다(0087)
  update public.party_members set last_seen = now() where party_id = p_party and user_id = auth.uid();
  update public.party_members set ready = false
   where party_id = p_party and ready
     and last_seen < now() - make_interval(secs => greatest(coalesce(economy.setting_num('party_presence_sec'), 25), 5)::int);

  delete from public.party_members where party_id = p_party and not ready;

  if v_party.crew_id is not null then
    v_day := economy.game_day(v_starts);
    v_level := greatest(coalesce((select c.level from public.crews c where c.id = v_party.crew_id), 1), 1);
    -- 공용 풀을 먼저 잠근다(모든 크루 러닝 함수가 같은 차례) — 같은 크루의 다른 방이 동시에 출발해도 기여가 섞이지 않게
    perform economy.crew_pool_settle(v_party.crew_id, v_day, now());
    delete from public.party_members m
     where m.party_id = p_party
       and (exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.day = v_day)
            or not exists (select 1 from public.crew_members cm where cm.crew_id = v_party.crew_id and cm.user_id = m.user_id));
    select count(*)::int into v_n from public.party_members m where m.party_id = p_party;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    if not exists (select 1 from public.party_members m where m.party_id = p_party and m.user_id = auth.uid()) then
      raise exception 'crew_run_done_today' using errcode = '23514', detail = '오늘 크루 러닝을 이미 했습니다';
    end if;
  end if;

  update public.parties p
     set status = 'COUNTDOWN', starts_at = v_starts,
         crew_level = case when p.crew_id is not null then v_level end
   where p.id = p_party;

  insert into public.party_runs (party_id, user_id, starts_at, sneaker_id, shoe_dur_max)
  select p_party, m.user_id, v_starts, s.id, s.dur_max
    from public.party_members m
    left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
   where m.party_id = p_party
  on conflict do nothing;

  if v_party.crew_id is not null then
    -- 참여 · 신발 스냅샷 · 기여 = 최대 내구도 × (1 + 레벨 체력 보너스). 같은 날 다른 방에서 먼저 출발했으면(동시 출발) 이 방에서 뺀다
    with ins as (
      insert into economy.crew_runs (user_id, day, crew_id, party_id, started_at, state, last_beat_at,
                                     sneaker_id, shoe_dur_max, shoe_eff_bps, crew_level, contribution, level_pct,
                                     party_name, course_name, course_km, course_rev)
      select m.user_id, v_day, v_party.crew_id, p_party, v_starts, 'RUNNING', v_starts,
             s.id, coalesce(s.dur_max, 0), coalesce(e.efficiency_bps, 0), v_level,
             round(coalesce(s.dur_max, 0) * (1 + economy.crew_level_stamina(v_level)), 4), economy.crew_level_reward(v_level),
             v_party.name, v_party.course_name, v_party.course_km, v_party.course_rev
        from public.party_members m
        left join lateral economy.party_shoe(m.user_id, m.sneaker_id) s on true
        left join lateral economy.shoe_effective(s) e on s.id is not null
       where m.party_id = p_party
      on conflict (user_id, day) do nothing
      returning user_id, contribution)
    select count(*)::int, coalesce(sum(ins.contribution), 0) into v_n, v_added from ins;
    if v_n < 2 then
      raise exception 'crew_run_need_two' using errcode = '23514', detail = '크루 러닝은 준비한 크루원 2명 이상이 함께 출발합니다';
    end if;
    delete from public.party_runs r
     where r.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = r.user_id and c.party_id = p_party);
    delete from public.party_members m
     where m.party_id = p_party
       and not exists (select 1 from economy.crew_runs c where c.user_id = m.user_id and c.party_id = p_party);
    update economy.crew_day_pools d
       set capacity = d.capacity + v_added, contributors = d.contributors + v_n, version = d.version + 1,
           exhausted_at = case when d.capacity + v_added > d.consumed then null else d.exhausted_at end,
           low_at = case when d.capacity + v_added - d.consumed > (d.capacity + v_added) * 0.2 then null else d.low_at end
     where d.crew_id = v_party.crew_id and d.day = v_day
    returning * into v_pool;
    -- 새 기여로 다시 생겼으면 0 이던 구간을 닫는다(그 뒤로는 다시 적립 — D06)
    if v_pool.capacity > v_pool.consumed then
      update economy.crew_pool_empty x set to_at = now()
       where x.crew_id = v_party.crew_id and x.day = v_day and x.to_at is null;
    end if;
  end if;
end;
$$;

-- ── 2. 크루 러닝 신발 — 0070 그대로 + 예전 폰 신발(IMPORT · MINT)은 쓰지 않는다 ────────────
-- 고른 신발이 예전 신발이면 신은 신발로, 신은 신발도 예전 신발이면 신발 없이(기여 0 · 기본 효율) — 공용 체력을 지어낸 내구도로 채우지 못하게
create or replace function economy.party_shoe(p_user uuid, p_pick bigint)
returns setof public.market_sneakers
language sql stable security definer set search_path = public as $$
  select x.* from public.market_sneakers x
   where x.owner_id = p_user and x.chain_state = 'APP' and x.status = 'OWNED'
     and x.origin not in ('IMPORT', 'MINT')
     and (x.id = p_pick or x.equipped)
   order by (x.id is not distinct from p_pick) desc, x.id
   limit 1
$$;
revoke all on function economy.party_shoe(uuid, bigint) from public;

-- ── 2. 예전 폰 신발의 최대 내구도는 늘 일반 범위 — 0066 그대로 + 등급 대신 일반으로 굴린다 ──────────────
create or replace function economy.sneaker_durability_sync() returns trigger
language plpgsql as $$
begin
  if new.dur_base is null then
    -- 예전 폰 신발(IMPORT · MINT)의 등급은 폰이 정한 값이라 믿지 않는다 — 최대 내구도는 일반 범위로(0090)
    new.dur_base := economy.durability_roll(case when new.origin in ('IMPORT', 'MINT') then 'COMMON' else new.rarity end,
                                            hashtext('dur:' || new.id::text)::bigint);
  end if;
  new.dur_max := new.dur_base + economy.point_durability_of(new.dur_base) * new.pts_dur;
  if tg_op = 'UPDATE' and old.dur_now is not null
     and new.dur_now is not distinct from old.dur_now
     and new.dur_max is not distinct from old.dur_max
     and new.durability_pts is distinct from old.durability_pts then
    new.dur_now := round(new.dur_max * new.durability_pts / 100);
  elsif new.dur_now is null then
    -- 새 신발 · 아직 옮기지 않은 신발 — 적힌 비율대로 채운다
    new.dur_now := round(new.dur_max * coalesce(new.durability_pts, 100) / 100);
  end if;
  new.dur_now := least(greatest(new.dur_now, 0), new.dur_max);
  new.durability_pts := round(100.0 * new.dur_now / new.dur_max, 2);
  -- 옛 정수 칸(0007 — 거래소 화면)도 비율을 따른다
  if tg_op = 'UPDATE' and new.durability_pts is distinct from old.durability_pts then
    new.durability := floor(new.durability_pts)::int;
  end if;
  return new;
end $$;

-- 이미 올라온 예전 신발도 맞춘다 — 늘 같은 값으로 바꾸므로 다시 붙여넣어도(setup.sql) 바뀌는 줄이 없다.
-- 레드라인 · 피니시는 폰 앱에 없던 등급(폰은 일반 ~ 레전더리만 알았다)이라 지어낸 것 — 일반으로(수리 값도 일반).
-- 최대 내구도는 일반 범위로, 남은 내구도는 새 최대를 넘지 않게(트리거)
update public.market_sneakers s
   set rarity = 'COMMON', updated_at = now()
 where s.origin in ('IMPORT', 'MINT') and s.rarity in ('REDLINE', 'FINISH');
update public.market_sneakers s
   set dur_base = economy.durability_roll('COMMON', hashtext('dur:' || s.id::text)::bigint), updated_at = now()
 where s.origin in ('IMPORT', 'MINT')
   and s.dur_base > (select r.hi from economy.durability_range('COMMON') r);

-- ── 2. 예전 폰 신발 올리기 — 0007 그대로 + 새로 올리는 신발은 일반 · 남은 내구도 0 ──────────────
-- 폰이 보낸 등급 · 내구도를 믿지 않는다(앱은 적립 효과 없는 기념품으로 올린다 — EconomySync.importLegacy). 돌려주는 값(서버 신발 번호)은 그대로.
-- 다시 올리면 예전처럼 레벨만 기념으로 따라간다. 계정당 상한(economy.market_import_cap, 30)은 그대로
create or replace function public.market_import(
  p_local_id bigint,
  p_faction text,
  p_rarity text,
  p_variant int,
  p_level int,
  p_luck numeric,
  p_comfort numeric,
  p_durability int
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_id bigint;
  v_owner uuid;
  v_count int;
begin
  -- 두 요청이 동시에 세고 넣으면 상한을 넘는다 — 같은 사람의 요청은 한 줄로
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;

  if v_id is not null then
    select owner_id into v_owner from public.market_sneakers where id = v_id;
    -- 이미 판 신발이면 손대지 않는다. 지금 주인의 것이다. 레벨만 기념으로 따라간다 — 적립에는 1레벨로
    -- 친다(0022). 내구도는 서버가 정한다(폰 값으로 새것처럼 되돌리지 않는다).
    if v_owner = v_user then
      update public.market_sneakers
         set level = greatest(level, p_level)
       where id = v_id;
    end if;
    return v_id;
  end if;

  select count(*) into v_count from public.market_imports where user_id = v_user;
  if v_count >= economy.market_import_cap() then
    raise exception '올릴 수 있는 수를 넘었습니다 (최대 %)', economy.market_import_cap()
      using errcode = '22023';
  end if;

  -- 0090 — 등급은 일반, 남은 내구도는 0(최대 내구도는 일반 범위 — 트리거). 폰이 말한 등급 · 내구도는 버린다
  insert into public.market_sneakers
    (owner_id, faction, rarity, variant, level, luck, comfort, durability, durability_pts)
  values
    (v_user, p_faction, 'COMMON', p_variant, p_level, p_luck, p_comfort, 0, 0)
  returning id into v_id;

  insert into public.market_imports (user_id, local_id, sneaker_id)
  values (v_user, p_local_id, v_id);

  return v_id;
end $$;

comment on function public.market_import(bigint, text, text, int, int, numeric, numeric, int) is
  '폰의 신발을 거래소 장부에 올린다. 같은 신발은 한 번만, 계정당 상한이 있다. 등급 · 내구도는 폰 값을 버리고 일반 · 0 (0090).';

-- ── 5. SUP 체인 꺼내기 — 0025 그대로 + 서버 설정으로 열고 닫기 · 최소 금액 · 하루 횟수 ──────────────
-- 스토어 판은 앱에서 숨길 뿐이었다(가상자산사업자 신고 전) — 서버에서도 막는다. 기본 꺼짐(chain_withdraw_enabled)
create or replace function public.sup_withdraw_request(p_amount numeric)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_amount numeric := round(p_amount, 4);
  v_op uuid := gen_random_uuid();
  v_min numeric := greatest(coalesce(economy.setting_num('withdraw_min_sup'), 50), 1);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not coalesce((economy.setting('chain_withdraw_enabled') #>> '{}')::boolean, false) then
    raise exception '지금은 SUP 꺼내기를 열지 않았습니다' using errcode = '23514';
  end if;
  if v_amount is null or v_amount < 1 then
    raise exception '% SUP 이상부터 꺼낼 수 있습니다', v_min using errcode = '22023';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 전체 상한은 모두가 함께 쓰므로 전체 잠금 아래에서 센다
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  -- 0090 — 최소 금액 · 하루 횟수(만료된 것은 빼고)
  if v_amount < v_min then
    raise exception '% SUP 이상부터 꺼낼 수 있습니다', v_min using errcode = '22023';
  end if;
  if (select count(*) from public.chain_ops o
       where o.user_id = v_user and o.kind = 'SUP_WITHDRAW' and o.status <> 'EXPIRED'
         and o.created_at >= economy.today_start())
     >= coalesce(economy.setting_num('withdraw_user_daily_count'), 3) then
    raise exception '오늘은 더 꺼낼 수 없습니다 (하루 %번)', coalesce(economy.setting_num('withdraw_user_daily_count'), 3)
      using errcode = '23514';
  end if;

  if v_amount > economy.setting_num('withdraw_user_daily_sup') - economy.withdrawn_today(v_user) then
    raise exception '오늘 꺼낼 수 있는 양을 넘었습니다' using errcode = '23514';
  end if;
  if v_amount > economy.setting_num('withdraw_global_daily_sup') - economy.withdrawn_today(null) then
    raise exception '오늘 전체 꺼내기 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  -- 예약하는 순간 잔고에서 뺀다. 서명을 기다리는 동안 같은 돈을 앱에서 쓰지 못하게.
  perform economy.ledger_apply(v_user, 'CHAIN_WITHDRAW', -v_amount, '체인으로 꺼내기', 'op:' || v_op);

  insert into public.chain_ops (id, user_id, kind, wallet, amount, deadline)
  values (v_op, v_user, 'SUP_WITHDRAW', v_wallet, v_amount, economy.op_deadline());
  return v_op;
end $$;
revoke all on function public.sup_withdraw_request(numeric) from public, anon;
grant execute on function public.sup_withdraw_request(numeric) to authenticated;

-- ── 5. 상급 뽑기 — 0044 그대로 + 서버 설정으로 열고 닫기(기본 꺼짐) ──────────────
-- 거절 글에 "체인 작업을 잠시 멈췄" 가 들어 있어 앱(테스트 판)은 "지금은 상급 뽑기를 잠시 멈췄어요"로 보이고 기회는 쓰지 않는다
create or replace function public.premium_draw()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_genesis boolean := false;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if not coalesce((economy.setting('premium_draw_enabled') #>> '{}')::boolean, false) then
    raise exception '지금은 상급 뽑기 · 체인 작업을 잠시 멈췄습니다' using errcode = '23514';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 지갑을 연결한 사람만(지갑 혜택), 체인이 멈춰 있으면 기다린다 — 예전과 같은 조건
  perform economy.withdraw_gate(v_user, false);

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g
   where g.user_id = v_user and g.kind = 'BONUS' and g.used < g.granted;
  if found then
    update public.draw_grants
       set used = used + 1,
           genesis_used = genesis_used + case when v_genesis then 1 else 0 end
     where user_id = v_user and kind = 'BONUS';
  else
    v_genesis := false;
    update public.draw_grants set used = used + 1
     where user_id = v_user and kind = 'RUN' and used < granted;
    if not found then
      raise exception '상급 뽑기가 남아 있지 않습니다' using errcode = '23514';
    end if;
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  perform economy.vault_mint_enqueue(v_id);
  return v_id;
end $$;
revoke all on function public.premium_draw() from public, anon;
grant execute on function public.premium_draw() to authenticated;

-- ── 6. 새 지갑 가스비(어테스터 drip) — 서버가 허락한 때만 ──────────────
-- 예전엔 계정의 첫 지갑이면 누구나 0.0002 ETH 를 받아, 계정을 많이 만들어 릴레이어 가스비를 비울 수 있었다.
-- 가입 drip_min_account_days(3)일 · 경로 있는(무효 아닌) 러닝 drip_min_gps_runs(1)번 이상 · 사람마다 한 번 · 하루 전체 drip_global_daily(50)번.
-- 지갑 연결 자체는 그대로다(attester_wallet_link) — 가스비만 이 함수가 허락해야 보낸다
create table if not exists economy.wallet_drips (
  user_id uuid primary key references auth.users on delete cascade,
  address text not null,
  created_at timestamptz not null default now()
);
create index if not exists wallet_drips_recent on economy.wallet_drips (created_at);
alter table economy.wallet_drips enable row level security;
revoke all on economy.wallet_drips from public, anon, authenticated;
comment on table economy.wallet_drips is '새 지갑에 보낸(보내려 한) 테스트 가스비 — 사람마다 한 번, 하루 전체 상한 (0090)';

create or replace function public.attester_drip_claim(p_user uuid, p_address text)
returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare
  v_addr text := lower(coalesce(p_address, ''));
begin
  perform economy.attester_guard();
  -- 하루 전체 상한은 모두가 함께 쓰므로 전체 잠금 아래에서 센다
  perform pg_advisory_xact_lock(hashtext('chain:drip'));
  if exists (select 1 from economy.wallet_drips d where d.user_id = p_user) then
    return false;
  end if;
  -- 그 계정에 지금 붙은 지갑으로만
  if not exists (select 1 from public.wallet_links w where w.user_id = p_user and w.address = v_addr) then
    return false;
  end if;
  if coalesce((select p.created_at from public.profiles p where p.id = p_user), now())
       > now() - make_interval(days => coalesce(economy.setting_num('drip_min_account_days'), 3)::int) then
    return false;
  end if;
  if (select count(*) from (select 1 from public.walk_sessions s
                             where s.user_id = p_user and s.gps_backed and s.verdict <> 'VOID'
                             limit greatest(coalesce(economy.setting_num('drip_min_gps_runs'), 1), 0)::int) x)
     < coalesce(economy.setting_num('drip_min_gps_runs'), 1) then
    return false;
  end if;
  if (select count(*) from economy.wallet_drips d where d.created_at >= economy.today_start())
     >= coalesce(economy.setting_num('drip_global_daily'), 50) then
    return false;
  end if;
  insert into economy.wallet_drips (user_id, address) values (p_user, v_addr);
  return true;
end $$;
revoke all on function public.attester_drip_claim(uuid, text) from public, anon, authenticated;
grant execute on function public.attester_drip_claim(uuid, text) to stepup_attester, authenticated;

-- 허락받고 보내지 못했으면(체인 오류) 되돌린다 — 하루 상한을 헛되이 쓰지 않게. 10분 안의 허락만
create or replace function public.attester_drip_release(p_user uuid)
returns void
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  delete from economy.wallet_drips d where d.user_id = p_user and d.created_at > now() - interval '10 minutes';
end $$;
revoke all on function public.attester_drip_release(uuid) from public, anon, authenticated;
grant execute on function public.attester_drip_release(uuid) to stepup_attester, authenticated;
