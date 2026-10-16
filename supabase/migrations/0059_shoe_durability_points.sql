-- ════════════════════════════════════════════════════════════════════
--  0059 — 신발 내구도는 거리로 · 레벨업은 스탯 포인트로 (2026-10-16 사용자 결정)
--
--  내구도
--    · 1 = 10m. 모든 신발이 같은 빠르기로 닳는다 — 달린 100m 마다 10(100m 가 안 되는 나머지는 닳지 않는다).
--      예전에는 1km 마다 등급별로 0.7 ~ 1.0 칸(최대 100)이 닳았다.
--    · 최대 내구도는 신발마다 다르다 — 만들 때 등급 범위 안에서 10 단위(100m)로 고른다
--      (뽑기는 뽑기 씨앗의 14 ~ 15번째 바이트, 그 밖의 길은 신발 번호로).
--        일반 1,000 ~ 1,500(10 ~ 15km) · 레어 1,500 ~ 2,000 · 에픽 2,000 ~ 2,500 · 레전더리 2,500 ~ 3,000
--      최대 내구도(dur_max) = 고른 값(dur_base) + 내구도 포인트 × 100. 남은 내구도는 dur_now.
--    · durability_pts 는 이제 남은 비율(%, dur_now ÷ dur_max)이다. 적립 배율(50% 이상 그대로 · 20 ~ 50% 는 30% 덜 ·
--      20% 아래는 없음) · 체인(비율 × 100) · 예전 앱이 이 값을 그대로 읽는다. 트리거가 dur_now 에서 다시 센다.
--    · 수리 1점(10m) 값 = 예전 1km 수리 값(1km 에 닳던 칸 × 칸 값) ÷ 100 — 1km 달린 만큼 고치는 값은 그대로다
--      (일반 Lv1 1km = 1.05 SUP).
--
--  스탯 포인트
--    · 레벨이 오르면 효율 +0.5%p · 착화감 +0.2%p 가 저절로 오르던 것을 없애고, 한 레벨에 포인트 4 를 준다.
--      포인트 1 = 효율 +0.25%p · 착화감 +0.1%p(착화감은 20% 까지) · 최대 내구도 +100(1km — 남은 내구도도 같이 +100) 중 하나.
--      효율 2 · 착화감 2 가 예전에 저절로 오르던 값과 같다. 에너지 최대치(레벨마다 +2칸)는 그대로 레벨을 따른다.
--    · 나눈 포인트는 되돌리지 않는다 — sneaker_points_set 은 늘리기만 받는다.
--    · 이미 레벨을 올린 신발은 (레벨 − 1) × 4 포인트를 받는다. 옮기는 날 적립 · 체인 표시가 줄지 않게 효율 2 · 착화감 2 씩
--      나눠 둔 채로 받고, 한 번은 처음부터 다시 나눌 수 있다(pts_respec — 내구도로 옮기기 등).
--    · 폰에서 올린 예전 신발(IMPORT · MINT)은 지금처럼 일반 1레벨로 치므로 포인트가 없다.
--
--  체인: 컨트랙트(v3)의 효율 · 착화감은 레벨 1 값 그대로고(바뀌지 않는다) 내구도는 지금처럼 남은 비율 × 100 이다.
--  포인트는 체인 밖의 값이라 메타데이터(워커 meta.js)가 attester_sneaker_points 로 받아 더한다.
--  예전 앱도 그대로 돈다 — my_sneakers 의 효율 · 착화감(포인트까지 더한 값) · 내구도(%) · 수리 1점 값(1% 당)은 예전 뜻 그대로고
--  새 칸만 뒤에 더했다.
-- ════════════════════════════════════════════════════════════════════

-- ── 칸 ──────────────────────────────────────────────────────────────
alter table public.market_sneakers
  add column if not exists dur_base int,
  add column if not exists dur_max int,
  add column if not exists dur_now int,
  add column if not exists pts_eff int not null default 0,
  add column if not exists pts_comfort int not null default 0,
  add column if not exists pts_dur int not null default 0,
  add column if not exists pts_respec boolean not null default false;

comment on column public.market_sneakers.dur_base is
  '만들 때 고른 최대 내구도(1 = 10m, 등급 범위 안). 최대 내구도 dur_max = dur_base + pts_dur × 100';
comment on column public.market_sneakers.dur_now is
  '남은 내구도(1 = 10m). durability_pts 는 이 값의 비율(%)이다 — 트리거가 센다';
comment on column public.market_sneakers.pts_respec is
  '포인트를 한 번 처음부터 다시 나눌 수 있다 — 0059 전에 레벨을 올린 신발(효율 2 · 착화감 2 씩 나눠 둔 채로 받았다)';

-- ── 규칙 ────────────────────────────────────────────────────────────
create or replace function economy.durability_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 1000 when 'RARE' then 1500
                       when 'EPIC' then 2000 else 2500 end,
         case p_rarity when 'COMMON' then 1500 when 'RARE' then 2000
                       when 'EPIC' then 2500 else 3000 end
$$;

-- 등급 범위 안에서 10 단위로 고른다. p_roll 은 0 이상의 아무 정수(씨앗 바이트 · 해시)
create or replace function economy.durability_roll(p_rarity text, p_roll bigint) returns int
  language sql immutable as $$
  select r.lo + 10 * (abs(p_roll) % ((r.hi - r.lo) / 10 + 1))::int from economy.durability_range(p_rarity) r
$$;

create or replace function economy.points_per_level() returns int
  language sql immutable as $$ select 4 $$;
create or replace function economy.point_efficiency_bps() returns int
  language sql immutable as $$ select 25 $$;
create or replace function economy.point_comfort_bps() returns int
  language sql immutable as $$ select 10 $$;
create or replace function economy.point_durability() returns int
  language sql immutable as $$ select 100 $$;

-- 이 레벨까지 받은 포인트. 예전 신발(IMPORT · MINT)은 1레벨로 친다
create or replace function economy.points_total(p_origin text, p_level int) returns int
  language sql immutable as $$
  select case when p_origin in ('IMPORT', 'MINT') then 0
              else economy.points_per_level() * greatest(coalesce(p_level, 1) - 1, 0) end
$$;

-- 한 번 달린 거리만큼 닳는 양 — 100m 마다 10
create or replace function economy.durability_wear(p_distance_m numeric) returns int
  language sql immutable as $$
  select (10 * floor(greatest(coalesce(p_distance_m, 0), 0) / 100))::int
$$;

-- 수리 1점(10m) 값. 예전 1km 에 닳던 칸(durability_loss_per_km) × 칸 값(repair_cost_per_point) 이 1km 수리 값이었다 —
-- 그 값을 100 점에 나눈다. durability_loss_per_km 은 이제 이 계산에만 쓴다(닳는 양은 durability_wear)
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round(economy.durability_loss_per_km(p_rarity) * economy.repair_cost_per_point(p_rarity, p_level) / 100, 8)
$$;

-- 적립 · 화면에 쓰는 실효 스탯 — 레벨 1 값 + 나눈 포인트. 예전 신발(IMPORT · MINT)은 일반 1레벨로 친다.
-- durability 는 남은 비율(%) — 적립 배율(economy.durability_factor)이 읽는다
create or replace function economy.shoe_effective(
  s public.market_sneakers,
  out efficiency_bps int, out comfort_bps int, out level int, out durability numeric,
  out points_total int, out points_free int
) language sql immutable as $$
  select
    case when s.origin in ('IMPORT', 'MINT') then 200
         else s.efficiency_bps + economy.point_efficiency_bps() * s.pts_eff end,
    case when s.origin in ('IMPORT', 'MINT') then 0
         else least(s.comfort_bps + economy.point_comfort_bps() * s.pts_comfort, economy.comfort_cap_bps()) end,
    case when s.origin in ('IMPORT', 'MINT') then 1 else s.level end,
    s.durability_pts,
    economy.points_total(s.origin, s.level),
    economy.points_total(s.origin, s.level) - s.pts_eff - s.pts_comfort - s.pts_dur
$$;

-- 예전 계산(0022)은 레벨에서 효율 · 착화감을 올렸다. 이제 레벨은 스탯을 올리지 않는다 — 포인트까지 더한 값은
-- economy.shoe_effective 로. 이 함수는 에너지 최대치(economy.energy_max)가 실효 레벨을 읽는 데만 남는다
create or replace function economy.sneaker_effective(
  p_origin text, p_rarity text, p_level int,
  p_efficiency_bps int, p_comfort_bps int, p_durability numeric,
  out efficiency_bps int, out comfort_bps int, out level int, out durability numeric
) language sql immutable as $$
  select
    case when p_origin in ('IMPORT', 'MINT') then 200 else p_efficiency_bps end,
    case when p_origin in ('IMPORT', 'MINT') then 0 else least(p_comfort_bps, economy.comfort_cap_bps()) end,
    case when p_origin in ('IMPORT', 'MINT') then 1 else p_level end,
    p_durability
$$;

-- ── 남은 내구도 ↔ 남은 비율 ─────────────────────────────────────────
-- 모든 쓰기에서 최대 내구도를 다시 세고, 남은 비율(durability_pts)을 남은 내구도에서 다시 센다.
-- 남은 비율만 바로 쓰는 예전 길이 남아 있어도(관리자 손질 등) 그 비율을 남은 내구도로 옮긴다
create or replace function economy.sneaker_durability_sync() returns trigger
language plpgsql as $$
begin
  if new.dur_base is null then
    new.dur_base := economy.durability_roll(new.rarity, hashtext('dur:' || new.id::text)::bigint);
  end if;
  new.dur_max := new.dur_base + economy.point_durability() * new.pts_dur;
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

drop trigger if exists market_sneakers_durability on public.market_sneakers;
create trigger market_sneakers_durability
  before insert or update on public.market_sneakers
  for each row execute function economy.sneaker_durability_sync();

-- 이미 있는 신발 옮기기 — 최대 내구도를 고르고(남은 비율은 그대로 — 트리거가 남은 내구도를 채운다), 올린 레벨만큼 받은
-- 포인트를 효율 · 착화감에 2 씩 나눠 둔다(옮기는 날 스탯이 그대로). 한 번 다시 나눌 수 있게 표시한다.
-- 다시 붙여넣어도(setup.sql) 이미 옮긴 신발은 건너뛴다
create or replace function economy.sneaker_points_backfill() returns int
language plpgsql security definer set search_path = public, economy as $$
declare v_n int;
begin
  update public.market_sneakers s
     set dur_base = economy.durability_roll(s.rarity, hashtext('dur:' || s.id::text)::bigint),
         pts_eff = economy.points_total(s.origin, s.level) / 2,
         pts_comfort = economy.points_total(s.origin, s.level) / 2,
         pts_dur = 0,
         pts_respec = economy.points_total(s.origin, s.level) > 0
   where s.dur_base is null;
  get diagnostics v_n = row_count;
  return v_n;
end $$;
revoke all on function economy.sneaker_points_backfill() from public;

select economy.sneaker_points_backfill();

alter table public.market_sneakers
  alter column dur_base set not null,
  alter column dur_max set not null,
  alter column dur_now set not null;

alter table public.market_sneakers drop constraint if exists market_sneakers_points_check;
alter table public.market_sneakers add constraint market_sneakers_points_check check (
  dur_base > 0
  and dur_max = dur_base + 100 * pts_dur
  and dur_now between 0 and dur_max
  and pts_eff >= 0 and pts_comfort >= 0 and pts_dur >= 0
  -- 한 레벨에 4 (economy.points_per_level)
  and pts_eff + pts_comfort + pts_dur <= 4 * (level - 1)
);

-- 체인 스탯 갱신(0044) — 레벨이 오르거나 남은 비율이 오르면(수리 · 내구도 포인트). 이제 남은 비율은 트리거가 세므로
-- 남은 내구도 · 최대 내구도를 쓰는 갱신에서도 본다. 달려서 닳는 것은 여전히 올리지 않는다
drop trigger if exists market_sneakers_chain_jobs on public.market_sneakers;
create trigger market_sneakers_chain_jobs
  after update of level, durability_pts, dur_now, dur_base, pts_dur on public.market_sneakers
  for each row execute function economy.chain_jobs_on_sneaker();

-- ── 뽑기 — 0045 그대로 + 최대 내구도(씨앗 14 ~ 15번째 바이트) ─────────────
/*
 * 뽑은 결과로 신발을 만든다(0023 그대로 + 새 도감 모델). 값은 부르는 쪽이 이미 치렀다.
 * 씨앗 결과 바이트: 0~3 등급 · 4~5 속성 · 6~7 변형 · 8~9 효율 · 10~11 착화감 · 12~13 모델 · 14~15 최대 내구도
 */
create or replace function economy.draw_create(
  p_user uuid,
  p_origin text,
  p_min_rarity text default null,
  p_genesis boolean default false
) returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_nonce bigint;
  v_d bytea;
  v_rarity text;
  v_faction text;
  v_variant int;
  v_eff_lo int; v_eff_hi int;
  v_cmf_lo int; v_cmf_hi int;
  v_id bigint;
  v_lock numeric := 0;
begin
  select n.o_nonce, n.o_digest into v_nonce, v_d from economy.draw_digest(p_user) n;

  v_rarity := economy.roll_rarity(economy.bytes_int(v_d, 0, 4), p_min_rarity);
  v_faction := (array['FIRE', 'WATER', 'LIGHTNING', 'WIND'])[economy.bytes_int(v_d, 4, 2) % 4 + 1];
  v_variant := (economy.bytes_int(v_d, 6, 2) % economy.variant_count(v_rarity))::int;
  select lo, hi into v_eff_lo, v_eff_hi from economy.efficiency_range(v_rarity);
  select lo, hi into v_cmf_lo, v_cmf_hi from economy.comfort_range(v_rarity);

  if p_origin in ('FREE_DRAW', 'BONUS_DRAW') then
    v_lock := economy.setting_num('free_shoe_lock_km');
  end if;

  insert into public.market_sneakers (
    owner_id, faction, rarity, variant, level, luck, comfort, durability,
    origin, efficiency_bps, comfort_bps, durability_pts,
    lock_km, withdrawable, draw_nonce, genesis_no, model_id, dur_base
  ) values (
    p_user, v_faction, v_rarity, v_variant, 1, 1,
    -- 옛 comfort 칸(배율 1.00~1.40)은 옛 앱 화면용으로만 채운다.
    1 + v_cmf_lo / 10000.0, 100,
    p_origin,
    v_eff_lo + (economy.bytes_int(v_d, 8, 2) % (v_eff_hi - v_eff_lo + 1))::int,
    v_cmf_lo + (economy.bytes_int(v_d, 10, 2) % (v_cmf_hi - v_cmf_lo + 1))::int,
    100,
    v_lock, p_origin <> 'STARTER', v_nonce,
    case when p_genesis then nextval('public.genesis_seq')::int end,
    economy.draw_model(v_rarity, economy.bytes_int(v_d, 12, 2)),
    economy.durability_roll(v_rarity, economy.bytes_int(v_d, 14, 2))
  ) returning id into v_id;

  return v_id;
end $$;
revoke all on function economy.draw_create(uuid, text, text, boolean) from public;

-- ── 러닝 기록 — 0024 그대로 + 실효 스탯(포인트) · 닳는 양(100m 마다 10) ──────────
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
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 낮아 적립이 줄었습니다');
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
  -- 모든 신발이 같은 빠르기로 — 100m 마다 남은 내구도 10 (0059). 남은 비율(durability_pts)은 트리거가 다시 센다
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_distance_m > 0 then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = greatest(s.dur_now - economy.durability_wear(v_distance_m::numeric), 0),
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

-- ── 수리 — 0057 그대로(요청 번호) + 남은 내구도로 ───────────────────────────
-- p_points 는 고칠 내구도(1 = 10m), 비우면 가득. 앱(예전 앱도)은 늘 비워 보낸다.
-- 돌려주는 값 · 영수증은 예전처럼 고친 뒤의 남은 비율(%) — 예전 앱이 그 값을 내구도로 읽는다.
-- 치른 SUP 는 누구에게도 가지 않는다(소각).
create or replace function public.sneaker_repair(p_id bigint, p_points numeric default null, p_request uuid default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_units int;
  v_after numeric;
  v_prev numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev;
    end if;
  end if;

  v := economy.my_app_sneaker(v_user, p_id);

  v_units := least(coalesce(ceil(p_points)::int, v.dur_max), v.dur_max - v.dur_now);
  if v_units is null or v_units <= 0 then
    raise exception '고칠 곳이 없습니다' using errcode = '23514';
  end if;

  perform economy.ledger_apply(v_user, 'SPEND_REPAIR',
    -round(v_units * economy.repair_cost_per_unit(v.rarity, v.level), 4),
    format('신발 수리 #%s +%s', v.mint_number, v_units));
  update public.market_sneakers
     set dur_now = dur_now + v_units, updated_at = now()
   where id = p_id
  returning durability_pts into v_after;

  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'REPAIR:' || p_id, v_after);
  end if;
  return v_after;
end $$;
revoke all on function public.sneaker_repair(bigint, numeric, uuid) from public, anon;
grant execute on function public.sneaker_repair(bigint, numeric, uuid) to authenticated;

-- ── 포인트 나누기 ───────────────────────────────────────────────────
/*
 * 나눈 뒤의 값(효율 · 착화감 · 내구도 포인트 합계)을 보낸다 — 더하기가 아니라 결과라서 같은 요청이 두 번 와도 두 번 더하지 않는다.
 * 줄이는 것은 받지 않는다. 다시 나누기가 남은 신발(pts_respec)만 한 번 줄일 수 있고, 줄이면 다시 나누기를 쓴 것이다.
 * 내구도 포인트 1 은 최대 · 남은 내구도를 함께 100 올린다(줄이면 함께 내린다).
 * 돌려주는 값: 남은 포인트. 같은 요청 번호로 다시 오면 처음 결과를 돌려준다.
 */
create or replace function public.sneaker_points_set(
  p_id bigint,
  p_efficiency int,
  p_comfort int,
  p_durability int,
  p_request uuid default null
) returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_prev numeric;
  v_total int;
  v_lowered boolean;
  v_free int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev::int;
    end if;
  end if;

  v := economy.my_app_sneaker(v_user, p_id);
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 포인트가 없습니다' using errcode = '22023';
  end if;
  if p_efficiency is null or p_comfort is null or p_durability is null
     or p_efficiency < 0 or p_comfort < 0 or p_durability < 0 then
    raise exception '포인트가 올바르지 않습니다' using errcode = '22023';
  end if;

  v_total := economy.points_total(v.origin, v.level);
  if p_efficiency + p_comfort + p_durability > v_total then
    raise exception '포인트가 모자랍니다' using errcode = '23514';
  end if;
  v_lowered := p_efficiency < v.pts_eff or p_comfort < v.pts_comfort or p_durability < v.pts_dur;
  if v_lowered and not v.pts_respec then
    raise exception '나눈 포인트는 되돌릴 수 없습니다' using errcode = '23514';
  end if;
  if p_comfort > v.pts_comfort
     and v.comfort_bps + economy.point_comfort_bps() * p_comfort > economy.comfort_cap_bps() then
    raise exception '착화감은 20%%까지 올릴 수 있습니다' using errcode = '23514';
  end if;

  update public.market_sneakers
     set pts_eff = p_efficiency,
         pts_comfort = p_comfort,
         pts_dur = p_durability,
         dur_now = greatest(dur_now + economy.point_durability() * (p_durability - v.pts_dur), 0),
         pts_respec = pts_respec and not v_lowered,
         updated_at = now()
   where id = p_id;

  v_free := v_total - p_efficiency - p_comfort - p_durability;
  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'POINTS:' || p_id, v_free);
  end if;
  return v_free;
end $$;
revoke all on function public.sneaker_points_set(bigint, int, int, int, uuid) from public, anon;
grant execute on function public.sneaker_points_set(bigint, int, int, int, uuid) to authenticated;

-- ── 내 신발 — 0045 그대로 + 내구도 · 포인트 칸 ──────────────────────────
-- 효율 · 착화감은 포인트까지 더한 값, durability 는 남은 비율(%), repair_cost_per_point 는 1% 고치는 값 — 예전 앱이 읽는 뜻 그대로.
-- 새 앱은 durability_now / durability_max(1 = 10m) 와 repair_cost_per_unit(10m 고치는 값)을 쓴다.
drop function if exists public.my_sneakers();
create function public.my_sneakers()
returns table (
  id bigint, faction text, rarity text, variant int, level int, max_level int,
  efficiency_bps int, comfort_bps int, durability numeric,
  equipped boolean, origin text, chain_state text, status text,
  mint_number bigint, genesis_no int, token_id numeric,
  km_run numeric, lock_km numeric, can_withdraw boolean,
  upgrade_cost numeric, repair_cost_per_point numeric,
  model_id int,
  durability_now int, durability_max int, durability_base int,
  efficiency_base_bps int, comfort_base_bps int,
  points_total int, points_free int,
  points_efficiency int, points_comfort int, points_durability int, points_respec boolean,
  repair_cost_per_unit numeric
)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return query
  select s.id, s.faction, s.rarity, s.variant, s.level, economy.max_level(s.rarity),
         e.efficiency_bps, e.comfort_bps, s.durability_pts,
         s.equipped, s.origin, s.chain_state, s.status,
         s.mint_number, s.genesis_no, s.token_id,
         s.km_run, s.lock_km,
         (s.withdrawable and s.km_run >= s.lock_km and s.chain_state = 'APP' and s.status = 'OWNED'),
         economy.upgrade_cost(s.rarity, s.level),
         round(economy.repair_cost_per_unit(s.rarity, s.level) * s.dur_max / 100, 4),
         s.model_id,
         s.dur_now, s.dur_max, s.dur_base,
         case when s.origin in ('IMPORT', 'MINT') then 200 else s.efficiency_bps end,
         case when s.origin in ('IMPORT', 'MINT') then 0 else s.comfort_bps end,
         e.points_total, e.points_free,
         s.pts_eff, s.pts_comfort, s.pts_dur, s.pts_respec,
         economy.repair_cost_per_unit(s.rarity, s.level)
    from public.market_sneakers s,
         lateral economy.shoe_effective(s) e
   where s.owner_id = v_user
   order by s.equipped desc, s.id;
end $$;
revoke all on function public.my_sneakers() from public, anon;
grant execute on function public.my_sneakers() to authenticated;

-- ── 강화 화면의 대상 값 — 0054 와 같은 모양 + 포인트 ───────────────────────
-- 레벨이 올라도 효율 · 착화감은 그대로고 포인트가 4 늘어난다(견적의 before · after 가 이 차이를 보인다)
create or replace function economy.forge_target_json(t public.market_sneakers, p_level int) returns jsonb
  language plpgsql stable security definer set search_path = public, economy as $$
declare
  e record;
begin
  t.level := p_level;
  select * into e from economy.shoe_effective(t);
  return jsonb_build_object(
    'id', t.id, 'rarity', t.rarity, 'level', p_level,
    'max_level', economy.forge_max_level(t.rarity, t.token_id),
    'efficiency_bps', e.efficiency_bps, 'comfort_bps', e.comfort_bps,
    'points_total', e.points_total, 'points_free', e.points_free);
end $$;
revoke all on function economy.forge_target_json(public.market_sneakers, int) from public;

-- ── 체인 메타데이터(워커 meta.js) — 토큰의 포인트 · 최대 내구도 ─────────────────
-- 컨트랙트에는 레벨 1 의 효율 · 착화감과 남은 비율만 있다. 나눈 포인트는 서버에만 있어 메타데이터가 이것으로 더한다.
-- 서버가 모르는 토큰이면 줄이 없다
create or replace function public.attester_sneaker_points(p_token numeric)
returns table (efficiency_points int, comfort_points int, durability_points int, durability_max int)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform economy.attester_guard();
  return query
  select s.pts_eff, s.pts_comfort, s.pts_dur, s.dur_max
    from public.market_sneakers s
   where s.token_id = p_token and s.origin not in ('IMPORT', 'MINT');
end $$;
revoke all on function public.attester_sneaker_points(numeric) from public, anon, authenticated;
grant execute on function public.attester_sneaker_points(numeric) to stepup_attester, authenticated;
