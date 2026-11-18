-- ════════════════════════════════════════════════════════════════════
--  0116 — 경제 4판 (2026-11-18)
--
--  사용자 "전부 진행해" — docs/점검-2026-11-18-경제-흐름.md "해결 계획" · docs/shoe-stats-boxes-2026-11-18.md 3판 + 운:
--    · 능력치는 지수(100 = 기준, 2배면 2배). 같은 등급 안 100 → 180(폭 80%), 등급마다 효율 × 1.5 · 달리는 시간 × 1.35.
--      뽑기는 가운데가 자주(주사위 두 개). 착화감은 "에너지 절약 %"로 적는다(1 − 100 ÷ 지수).
--    · 레벨업 포인트는 등급마다: 레벨마다 2 · 3 · 4 · 5 · 6 · 8, 5레벨마다 보너스 3 · 5 · 6 · 8 · 9 · 12,
--      포인트 1 = 뽑은 값의 +1.0 · 1.1 · 1.25 · 1.4 · 1.6 · 1.8%. 5레벨마다 그 신발 내구도 가득 · 상급 상자(계정마다 그 레벨 처음).
--    · 걸음당 SUP 는 설정(0.0118 — 가운데 일반 신발 140 = 계획 1단계의 +60%). 수리비는 그 신발의 효율에 비례(걸어서 버는 돈의 45%).
--    · 운: 러닝 보상 × 0.7 ~ 1.3(평균 1), 신발이 닳는 양 × 0.667 ~ 1.444(평균 1 — 수리 부담이 번 돈의 30 ~ 65%).
--    · 이틀(48시간)에 한 번 한 켤레 무료 수리 · 부스터 판매 멈춤 · 에너지 최대치는 가진 신발 가운데 가장 높은 레벨 ·
--      하루 적립 상한 960(× 가진 신발 가운데 가장 센 효율 ÷ 140) · 경로 없는 러닝 96 · 올린 날 1,920(× 같은 배수).
--    · 상자: 일반 = 출석 1(하루 무료 3 → 1) + 러닝 2(오늘 적립 걸음 2,000 · 4,000) + SUP 40(하루 3),
--      상급 = 주간 25,000보 · 7일 연속 목표 · 신발 Lv5 · 10 · 15 · 20 … · SUP 600(주 2). 지갑 1km 상급은 멈춘다(주간으로 합침).
--    · 강화 재료는 내구도 50% 이상. SUP 로 레벨을 올리는 예전 길(sneaker_upgrade)은 닫는다(레벨 보상을 SUP 로 사지 못하게).
--    · 운영자: 바로 적용(내구도 가득 · 오늘 에너지 되돌리기) · 앱 판 보고 · 경제 현황 · 경제 설정.
--
--  예전 앱(스토어 1.17.1)도 부르는 함수는 이름 · 칸 · 돌려주는 모양을 그대로 둔다.
--  있는 신발은 한 번만 옮긴다(표시 economy_v4_migrated, 백업 economy.backup_0116_sneakers).
-- ════════════════════════════════════════════════════════════════════

-- ── 설정 ───────────────────────────────────────────────────────────
insert into public.economy_settings (key, value) values
  ('points_per_step', '0.0118'::jsonb),
  ('repair_share', '0.45'::jsonb),
  ('reward_luck', '[0.7, 1.0, 1.3]'::jsonb),
  ('wear_luck', '[0.667, 0.889, 1.444]'::jsonb),
  ('free_repair_hours', '48'::jsonb),
  ('boost_shop_enabled', 'false'::jsonb),
  ('run_box_steps', '[2000, 4000]'::jsonb),
  ('weekly_box_steps', '25000'::jsonb),
  ('streak_box_days', '7'::jsonb),
  ('box_price', '40'::jsonb),
  ('box_daily_buy', '3'::jsonb),
  ('premium_box_price', '600'::jsonb),
  ('premium_box_weekly_buy', '2'::jsonb),
  ('forge_material_min_durability', '50'::jsonb),
  ('app_current_version_code', '27'::jsonb),
  ('earn_cap_by_shoe', 'true'::jsonb)
on conflict (key) do nothing;

-- 예전 값을 바꾸는 것은 한 번만 — 다시 올려도(setup.sql) 운영자가 바꾼 값을 덮지 않는다
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'economy_v4_settings') then
    update public.economy_settings set value = '1'::jsonb, updated_at = now() where key = 'daily_free_draws';
    update public.economy_settings set value = '960'::jsonb, updated_at = now() where key = 'daily_earn_cap';
    update public.economy_settings set value = '96'::jsonb, updated_at = now() where key = 'no_gps_daily_cap';
    update public.economy_settings set value = '1920'::jsonb, updated_at = now() where key = 'upload_daily_cap';
    update public.economy_settings set value = '0'::jsonb, updated_at = now() where key = 'premium_run_daily_cap';
    insert into public.economy_settings (key, value) values ('economy_v4_settings', 'true'::jsonb);
  end if;
end $$;

-- ── 기본 값 ─────────────────────────────────────────────────────────

-- 걸음당 SUP — 효율 100(지수) 신발의 값. 설정에서 읽는다(배포 없이 바꾼다)
create or replace function economy.points_per_step() returns numeric
language sql stable security definer set search_path = public, economy as $$
  select coalesce(economy.setting_num('points_per_step'), 0.0118)
$$;

-- 삼각 분포 하나 — [가장 작게, 가장 자주, 가장 크게]. u 는 0 ~ 1
create or replace function economy.tri(p_lo numeric, p_mode numeric, p_hi numeric, p_u double precision) returns numeric
language sql immutable as $$
  select case
    when p_hi <= p_lo then p_lo
    when p_u < (p_mode - p_lo) / (p_hi - p_lo)
      then p_lo + sqrt(p_u * (p_hi - p_lo)::float8 * (p_mode - p_lo)::float8)::numeric
    else p_hi - sqrt((1 - p_u) * (p_hi - p_lo)::float8 * (p_hi - p_mode)::float8)::numeric
  end
$$;

-- 러닝마다 운 — 설정 [lo, mode, hi]. 없거나 lo = hi 면 그 값(시험에서 운을 끈다)
create or replace function economy.luck_draw(p_key text) returns numeric
language plpgsql volatile security definer set search_path = public, economy as $$
declare v jsonb := economy.setting(p_key);
begin
  if v is null or jsonb_typeof(v) <> 'array' or jsonb_array_length(v) <> 3 then
    return 1;
  end if;
  return round(economy.tri((v ->> 0)::numeric, (v ->> 1)::numeric, (v ->> 2)::numeric, random()), 4);
end $$;
revoke all on function economy.luck_draw(text) from public;

-- ── 능력치 범위 (지수 → 저장 값) ───────────────────────────────────
-- 효율: 저장 = 적립 +bps = (지수 − 100) × 100. 지수 바닥 100 × 1.5^등급, 꼭대기 = 바닥 × 1.8
create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int)
language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 5000 when 'EPIC' then 12500
                       when 'LEGENDARY' then 23750 when 'REDLINE' then 40625 else 65938 end,
         case p_rarity when 'COMMON' then 8000 when 'RARE' then 17000 when 'EPIC' then 30500
                       when 'LEGENDARY' then 50750 when 'REDLINE' then 81125 else 126688 end
$$;
-- 착화감: 지수 바닥 (140 + 10 × 등급) ÷ 1.4, 꼭대기 = 바닥 × 1.8. 저장 = 에너지 절약 bps
create or replace function economy.comfort_index_lo(p_rarity text) returns numeric
language sql immutable as $$
  select (140 + 10 * coalesce(economy.rarity_ord(p_rarity), 0)) / 1.4
$$;
create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int)
language sql immutable as $$
  select round(10000 * (1 - 100 / economy.comfort_index_lo(p_rarity)))::int,
         round(10000 * (1 - 100 / (economy.comfort_index_lo(p_rarity) * 1.8)))::int
$$;
-- 내구도(1 = 12초, 1분마다 5): 바닥 22분 × 1.35^등급, 꼭대기 = 바닥 × 1.8
create or replace function economy.durability_range(p_rarity text, out lo int, out hi int)
language sql immutable as $$
  select case p_rarity when 'COMMON' then 110 when 'RARE' then 150 when 'EPIC' then 200
                       when 'LEGENDARY' then 270 when 'REDLINE' then 365 else 495 end,
         case p_rarity when 'COMMON' then 200 when 'RARE' then 265 when 'EPIC' then 360
                       when 'LEGENDARY' then 485 when 'REDLINE' then 660 else 890 end
$$;
-- 범위 안의 자리 p(0 = 바닥, 1 = 꼭대기)에서 값
create or replace function economy.stat_efficiency(p_rarity text, p numeric) returns int
language sql immutable as $$
  select (r.lo + round((r.hi - r.lo) * least(greatest(p, 0), 1)))::int from economy.efficiency_range(p_rarity) r
$$;
create or replace function economy.stat_comfort(p_rarity text, p numeric) returns int
language sql immutable as $$
  select round(10000 * (1 - 100 / (economy.comfort_index_lo(p_rarity) * (1 + 0.8 * least(greatest(p, 0), 1)))))::int
$$;
create or replace function economy.stat_durability(p_rarity text, p numeric) returns int
language sql immutable as $$
  select (5 * round((r.lo + (r.hi - r.lo) * least(greatest(p, 0), 1)) / 5.0))::int from economy.durability_range(p_rarity) r
$$;
-- 트리거가 dur_base 가 빈 신발에 쓰는 값(고르게)
create or replace function economy.durability_roll(p_rarity text, p_roll bigint) returns int
language sql immutable as $$
  select economy.stat_durability(p_rarity, (abs(p_roll) % 65536) / 65535.0)
$$;

-- ── 레벨업 포인트 — 등급마다 ─────────────────────────────────────────
create or replace function economy.points_per_level_of(p_rarity text) returns int
language sql immutable as $$
  select case p_rarity when 'COMMON' then 2 when 'RARE' then 3 when 'EPIC' then 4
                       when 'LEGENDARY' then 5 when 'REDLINE' then 6 when 'FINISH' then 8 else 0 end
$$;
create or replace function economy.points_milestone_of(p_rarity text) returns int
language sql immutable as $$
  select case p_rarity when 'COMMON' then 3 when 'RARE' then 5 when 'EPIC' then 6
                       when 'LEGENDARY' then 8 when 'REDLINE' then 9 when 'FINISH' then 12 else 0 end
$$;
-- 포인트 1 이 올리는 폭 — 만분율(100 = 뽑은 값의 1%)
create or replace function economy.point_rate_of(p_rarity text) returns int
language sql immutable as $$
  select case p_rarity when 'COMMON' then 100 when 'RARE' then 110 when 'EPIC' then 125
                       when 'LEGENDARY' then 140 when 'REDLINE' then 160 when 'FINISH' then 180 else 0 end
$$;
create or replace function economy.points_total_of(p_origin text, p_rarity text, p_level integer) returns integer
language sql immutable as $$
  select case when p_origin in ('IMPORT', 'MINT') then 0
              else economy.points_per_level_of(p_rarity) * (greatest(coalesce(p_level, 1), 1) - 1)
                   + economy.points_milestone_of(p_rarity) * (greatest(coalesce(p_level, 1), 1) / 5) end
$$;
create or replace function economy.dur_max_of(p_base int, p_rarity text, p_pts int) returns int
language sql immutable as $$
  select (p_base + round(p_base * economy.point_rate_of(p_rarity) * coalesce(p_pts, 0) / 10000.0))::int
$$;
-- 착화감 포인트를 더한 절약 bps(상한 전)
create or replace function economy.comfort_with_points(p_comfort_bps int, p_rarity text, p_pts int) returns int
language sql immutable as $$
  select (10000 - round((10000 - p_comfort_bps) / (1 + economy.point_rate_of(p_rarity) * coalesce(p_pts, 0) / 10000.0)))::int
$$;
create or replace function economy.comfort_cap_bps() returns integer
language sql immutable as $$ select 7000 $$;

create or replace function economy.shoe_effective(s public.market_sneakers, out efficiency_bps integer, out comfort_bps integer,
    out level integer, out durability numeric, out points_total integer, out points_free integer)
returns record language sql immutable as $$
  select
    case when s.origin in ('IMPORT', 'MINT') then 0
         else (round((10000 + s.efficiency_bps) * (1 + economy.point_rate_of(s.rarity) * s.pts_eff / 10000.0)) - 10000)::int end,
    case when s.origin in ('IMPORT', 'MINT') then 0
         else least(economy.comfort_with_points(s.comfort_bps, s.rarity, s.pts_comfort), economy.comfort_cap_bps()) end,
    case when s.origin in ('IMPORT', 'MINT') then 1 else s.level end,
    s.durability_pts,
    economy.points_total_of(s.origin, s.rarity, s.level),
    economy.points_total_of(s.origin, s.rarity, s.level) - s.pts_eff - s.pts_comfort - s.pts_dur
$$;

-- 남은 내구도 ↔ 남은 비율 — 0066 그대로 + 최대 내구도는 등급의 포인트 폭으로 + 5레벨마다 가득
create or replace function economy.sneaker_durability_sync() returns trigger
language plpgsql as $$
begin
  if new.dur_base is null then
    new.dur_base := economy.durability_roll(new.rarity, hashtext('dur:' || new.id::text)::bigint);
  end if;
  new.dur_max := economy.dur_max_of(new.dur_base, new.rarity, new.pts_dur);
  if tg_op = 'UPDATE' and old.dur_now is not null
     and new.dur_now is not distinct from old.dur_now
     and new.dur_max is not distinct from old.dur_max
     and new.durability_pts is distinct from old.durability_pts then
    new.dur_now := round(new.dur_max * new.durability_pts / 100);
  elsif new.dur_now is null then
    new.dur_now := round(new.dur_max * coalesce(new.durability_pts, 100) / 100);
  end if;
  -- 5 · 10 · 15 …레벨에 닿으면 그 신발 내구도 가득 (0116)
  if tg_op = 'UPDATE' and new.level / 5 > old.level / 5 then
    new.dur_now := new.dur_max;
  end if;
  new.dur_now := least(greatest(new.dur_now, 0), new.dur_max);
  new.durability_pts := round(100.0 * new.dur_now / new.dur_max, 2);
  if tg_op = 'UPDATE' and new.durability_pts is distinct from old.durability_pts then
    new.durability := floor(new.durability_pts)::int;
  end if;
  return new;
end $$;

-- ── 있는 신발 옮기기 (한 번만) ───────────────────────────────────────
-- 예전(0066) 범위에서의 자리(0 = 바닥, 1 = 꼭대기) — 있는 신발을 옮길 때. 첫 신발은 가운데
create or replace function economy.v4_position(p_rarity text, p_origin text, p_eff int, p_comfort int, p_dur_base int,
    out pe numeric, out pc numeric, out pd numeric)
language sql immutable as $$
  select
    case when p_origin = 'STARTER' then 0.5 else
      least(greatest((p_eff - (case p_rarity when 'COMMON' then 200 when 'RARE' then 400 when 'EPIC' then 800
                                               when 'LEGENDARY' then 1600 when 'REDLINE' then 2800 else 4500 end))::numeric
        / (case p_rarity when 'COMMON' then 200 when 'RARE' then 200 when 'EPIC' then 300
                         when 'LEGENDARY' then 400 when 'REDLINE' then 700 else 1000 end), 0), 1) end,
    case when p_origin = 'STARTER' then 0.5 else
      least(greatest((p_comfort - 300 * coalesce(economy.rarity_ord(p_rarity), 0))::numeric / 300, 0), 1) end,
    case when p_origin = 'STARTER' then 0.5 else
      least(greatest((p_dur_base - (case p_rarity when 'COMMON' then 45 when 'RARE' then 70 when 'EPIC' then 115
                                                  when 'LEGENDARY' then 180 when 'REDLINE' then 270 else 405 end))::numeric
        / (case p_rarity when 'COMMON' then 10 when 'RARE' then 10 when 'EPIC' then 20
                         when 'LEGENDARY' then 40 when 'REDLINE' then 60 else 90 end), 0), 1) end
$$;

alter table public.market_sneakers drop constraint if exists market_sneakers_stats_check;
alter table public.market_sneakers drop constraint if exists market_sneakers_points_check;

do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'economy_v4_migrated') then
    create table if not exists economy.backup_0116_sneakers as
      select id, owner_id, rarity, level, origin, efficiency_bps, comfort_bps, durability_pts,
             dur_base, dur_max, dur_now, pts_eff, pts_comfort, pts_dur, pts_respec, now() as backed_up_at
        from public.market_sneakers;

    -- 예전 범위(0066)에서의 자리 그대로 새 범위로(economy.v4_position). 첫 신발은 가운데. 나눈 포인트는 모두 돌려준다(포인트 값이 바뀌었다)
    update public.market_sneakers m
       set efficiency_bps = economy.stat_efficiency(m.rarity, p.pe),
           comfort_bps = economy.stat_comfort(m.rarity, p.pc),
           dur_base = economy.stat_durability(m.rarity, p.pd),
           dur_now = round(economy.stat_durability(m.rarity, p.pd) * m.durability_pts / 100),
           pts_eff = 0, pts_comfort = 0, pts_dur = 0, pts_respec = false,
           updated_at = now()
      from public.market_sneakers o, lateral economy.v4_position(o.rarity, o.origin, o.efficiency_bps, o.comfort_bps, o.dur_base) p
     where m.id = o.id and o.origin not in ('IMPORT', 'MINT');

    insert into public.economy_settings (key, value) values ('economy_v4_migrated', to_jsonb(now()));
  end if;
end $$;

alter table public.market_sneakers add constraint market_sneakers_stats_check check (
  efficiency_bps >= 0 and efficiency_bps <= 130000 and comfort_bps >= 0 and comfort_bps <= 7000
  and durability_pts >= 0 and durability_pts <= 100 and km_run >= 0 and lock_km >= 0) not valid;
alter table public.market_sneakers validate constraint market_sneakers_stats_check;
alter table public.market_sneakers add constraint market_sneakers_points_check check (
  dur_base > 0 and dur_max = economy.dur_max_of(dur_base, rarity, pts_dur) and dur_now >= 0 and dur_now <= dur_max
  and pts_eff >= 0 and pts_comfort >= 0 and pts_dur >= 0
  and pts_eff + pts_comfort + pts_dur <= economy.points_total_of(origin, rarity, level)) not valid;
alter table public.market_sneakers validate constraint market_sneakers_points_check;

-- ── 뽑기 — 같은 씨앗 · 같은 등급 · 같은 모델(draw_verify 그대로), 능력치는 가운데가 자주 ────────
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
  v_variant int;
  v_id bigint;
  v_lock numeric := 0;
  v_pe numeric;
  v_pc numeric;
  v_pd numeric;
begin
  select n.o_nonce, n.o_digest into v_nonce, v_d from economy.draw_digest(p_user) n;

  v_rarity := economy.roll_rarity_v3(economy.bytes_int(v_d, 0, 4), economy.draw_kind(p_origin), p_min_rarity);
  v_variant := (economy.bytes_int(v_d, 6, 2) % economy.variant_count(v_rarity))::int;
  -- 주사위 두 개의 평균 — 가운데가 자주, 양 끝은 드물게 (0116)
  v_pe := (economy.bytes_int(v_d, 8, 2) + economy.bytes_int(v_d, 16, 2)) / 131070.0;
  v_pc := (economy.bytes_int(v_d, 10, 2) + economy.bytes_int(v_d, 18, 2)) / 131070.0;
  v_pd := (economy.bytes_int(v_d, 14, 2) + economy.bytes_int(v_d, 20, 2)) / 131070.0;

  if p_origin in ('FREE_DRAW', 'BONUS_DRAW') then
    v_lock := economy.setting_num('free_shoe_lock_km');
  end if;

  insert into public.market_sneakers (
    owner_id, faction, rarity, variant, level, luck, comfort, durability,
    origin, efficiency_bps, comfort_bps, durability_pts,
    lock_km, withdrawable, draw_nonce, genesis_no, model_id, dur_base
  ) values (
    p_user, 'NONE', v_rarity, v_variant, 1, 1,
    1 + economy.stat_comfort(v_rarity, v_pc) / 10000.0, 100,
    p_origin,
    economy.stat_efficiency(v_rarity, v_pe),
    economy.stat_comfort(v_rarity, v_pc),
    100,
    v_lock, p_origin <> 'STARTER', v_nonce,
    case when p_genesis then nextval('public.genesis_seq')::int end,
    economy.draw_model(v_rarity, economy.bytes_int(v_d, 12, 2)),
    economy.stat_durability(v_rarity, v_pd)
  ) returning id into v_id;

  return v_id;
end $$;

-- 첫 신발 — 가운데 값(효율 140 · 착화감 140 · 31분). 나머지는 0066 그대로
create or replace function public.economy_bootstrap() returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_since timestamptz := (economy.setting('free_draw_since') #>> '{}')::timestamptz;
  v_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.seed_ensure(v_user);

  if not exists (select 1 from public.market_sneakers where owner_id = v_user and origin = 'STARTER') then
    insert into public.market_sneakers (
      owner_id, faction, rarity, variant, level, luck, comfort, durability,
      origin, efficiency_bps, comfort_bps, durability_pts, withdrawable, dur_base
    ) values (
      v_user, 'NONE', 'COMMON', 0, 1, 1, 1 + economy.stat_comfort('COMMON', 0.5) / 10000.0, 100,
      'STARTER', economy.stat_efficiency('COMMON', 0.5), economy.stat_comfort('COMMON', 0.5), 100, false,
      economy.stat_durability('COMMON', 0.5)
    ) returning id into v_id;
    if not exists (select 1 from public.market_sneakers where owner_id = v_user and equipped) then
      update public.market_sneakers set equipped = true where id = v_id;
    end if;
  end if;

  if (select p.created_at from public.profiles p where p.id = v_user) >= v_since then
    insert into public.draw_grants (user_id, kind, granted)
    values (v_user, 'FREE', 10)
    on conflict (user_id, kind) do nothing;
  end if;
end $$;

-- ── 포인트 나누기 — 착화감 상한 70% · 내구도 포인트는 등급의 폭 ─────────────
create or replace function public.sneaker_points_set(p_id bigint, p_efficiency integer, p_comfort integer, p_durability integer,
    p_request uuid default null)
returns integer
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

  v_total := economy.points_total_of(v.origin, v.rarity, v.level);
  if p_efficiency + p_comfort + p_durability > v_total then
    raise exception '포인트가 모자랍니다' using errcode = '23514';
  end if;
  v_lowered := p_efficiency < v.pts_eff or p_comfort < v.pts_comfort or p_durability < v.pts_dur;
  if v_lowered and not v.pts_respec then
    raise exception '나눈 포인트는 되돌릴 수 없습니다' using errcode = '23514';
  end if;
  if p_comfort > v.pts_comfort
     and economy.comfort_with_points(v.comfort_bps, v.rarity, p_comfort) > economy.comfort_cap_bps() then
    raise exception '착화감은 에너지 70%%를 아끼는 데까지 올릴 수 있습니다' using errcode = '23514';
  end if;

  update public.market_sneakers
     set pts_eff = p_efficiency,
         pts_comfort = p_comfort,
         pts_dur = p_durability,
         dur_now = greatest(dur_now + economy.dur_max_of(v.dur_base, v.rarity, p_durability)
                                    - economy.dur_max_of(v.dur_base, v.rarity, v.pts_dur), 0),
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

-- SUP 로 레벨을 올리는 예전 길은 닫는다 — 앱은 재료 강화(forge)만 쓴다. 레벨 보상(5레벨 상급 상자)을 SUP 로 사지 못하게
create or replace function public.sneaker_upgrade(p_id bigint, p_request uuid default null) returns integer
language plpgsql security definer set search_path = public, economy as $$
begin
  raise exception '강화는 재료 신발로만 할 수 있습니다' using errcode = '22023';
end $$;

-- ── 에너지 최대치 — 가진 신발 가운데 가장 높은 레벨 ──────────────────────
create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce((
    select max(case when s.origin in ('IMPORT', 'MINT') then 1 else s.level end)
      from public.market_sneakers s
     where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'), 1))
$$;

-- 하루 상한의 배수 — 가진 신발 가운데 가장 센 효율 ÷ 지수 140(가운데 일반), 1 아래로는 가지 않는다
create or replace function economy.earn_cap_factor(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select case when not coalesce((economy.setting('earn_cap_by_shoe') #>> '{}')::boolean, true) then 1 else greatest(1, coalesce((
    select max(10000 + e.efficiency_bps) / 14000.0
      from public.market_sneakers s, lateral economy.shoe_effective(s) e
     where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'), 1)) end
$$;

-- ── 수리 — 그 신발의 효율에 비례 · 이틀에 한 번 무료 ───────────────────────
-- 내구도 1(12초)의 값 = 걸어서(분당 110걸음) 그 신발로 버는 돈 × repair_share ÷ 5
create or replace function economy.repair_unit_of(s public.market_sneakers) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select round(coalesce(economy.setting_num('repair_share'), 0.45) * 110 * economy.points_per_step() / 5
               * (10000 + (economy.shoe_effective(s)).efficiency_bps) / 10000.0, 8)
$$;
-- 화면용 · 예전 표시 자리 — 가운데로 뽑힌 신발(포인트 없음)의 값
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level integer) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select round(coalesce(economy.setting_num('repair_share'), 0.45) * 110 * economy.points_per_step() / 5
               * (10000 + economy.stat_efficiency(p_rarity, 0.5)) / 10000.0, 8)
$$;

create table if not exists economy.repair_free_uses (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  sneaker_id bigint references public.market_sneakers (id) on delete set null,
  units int not null,
  value numeric(20, 4) not null,
  used_at timestamptz not null default now(),
  -- 예전 앱이 무료 값(0)을 본 채로 바로 다른 신발을 고치면 한 번 거절했는가
  guarded boolean not null default false
);
create index if not exists repair_free_uses_user on economy.repair_free_uses (user_id, used_at desc);

-- 다음 무료 수리 시각(지금 쓸 수 있으면 null)
create or replace function economy.free_repair_next(p_user uuid) returns timestamptz
language sql stable security definer set search_path = public, economy as $$
  select case when coalesce(economy.setting_num('free_repair_hours'), 0) <= 0 then 'infinity'::timestamptz
              else (select u.used_at + economy.setting_num('free_repair_hours') * interval '1 hour'
                      from economy.repair_free_uses u where u.user_id = p_user
                     order by u.used_at desc limit 1) end
$$;
create or replace function economy.free_repair_ready(p_user uuid) returns boolean
language sql stable security definer set search_path = public, economy as $$
  select coalesce(economy.free_repair_next(p_user) <= now(), true)
$$;

-- 수리 본체 — 예전 길(sneaker_repair)과 새 길(sneaker_repair_v2)이 같이 쓴다
create or replace function economy.repair_core(p_user uuid, p_id bigint, p_points numeric, p_request uuid,
    p_expected numeric, p_legacy boolean) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v public.market_sneakers;
  v_units int;
  v_after numeric;
  v_prev numeric;
  v_free boolean;
  v_cost numeric;
  v_unit numeric;
begin
  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));

  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = p_user and r.request_id = p_request;
    if found then
      return jsonb_build_object('ok', true, 'durability', v_prev, 'replay', true, 'balance', economy.balance_of(p_user));
    end if;
  end if;

  v := economy.my_app_sneaker(p_user, p_id);
  v_free := economy.free_repair_ready(p_user);
  if v_free then
    v_units := v.dur_max - v.dur_now;   -- 무료는 한 켤레 가득
  else
    v_units := least(coalesce(ceil(p_points)::int, v.dur_max), v.dur_max - v.dur_now);
  end if;
  if v_units is null or v_units <= 0 then
    raise exception '고칠 곳이 없습니다' using errcode = '23514';
  end if;
  v_unit := economy.repair_unit_of(v);
  v_cost := case when v_free then 0 else round(v_units * v_unit, 4) end;

  if not v_free and p_legacy then
    -- 예전 앱은 무료가 남은 동안 값을 0 으로 본다 — 무료를 막 쓰고 목록을 다시 읽기 전에 다른 신발을 고치면 한 번 멈춘다
    update economy.repair_free_uses u set guarded = true
     where u.id = (select x.id from economy.repair_free_uses x
                    where x.user_id = p_user and x.used_at > now() - interval '2 minutes' and not x.guarded
                    order by x.used_at desc limit 1);
    if found then
      raise exception '방금 무료 수리를 썼어요. 수리비가 바뀌었으니 다시 확인해 주세요' using errcode = '23514';
    end if;
  end if;
  if p_expected is not null and abs(p_expected - v_cost) > 0.0001 then
    return jsonb_build_object('ok', false, 'code', 'QUOTE_CHANGED', 'cost', v_cost, 'free', v_free, 'units', v_units);
  end if;

  if v_free then
    insert into economy.repair_free_uses (user_id, sneaker_id, units, value)
    values (p_user, p_id, v_units, round(v_units * v_unit, 4));
  else
    perform economy.ledger_apply(p_user, 'SPEND_REPAIR', -v_cost, format('신발 수리 #%s +%s', v.mint_number, v_units));
  end if;
  update public.market_sneakers
     set dur_now = dur_now + v_units, updated_at = now()
   where id = p_id
  returning durability_pts into v_after;

  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (p_user, p_request, 'REPAIR:' || p_id, v_after);
  end if;
  return jsonb_build_object('ok', true, 'durability', v_after, 'units', v_units, 'cost', v_cost, 'free', v_free,
                            'balance', economy.balance_of(p_user), 'free_next', economy.free_repair_next(p_user));
end $$;
revoke all on function economy.repair_core(uuid, bigint, numeric, uuid, numeric, boolean) from public;

-- 예전 앱 — 이름 · 칸 · 돌려주는 값(고친 뒤 남은 비율) 그대로
create or replace function public.sneaker_repair(p_id bigint, p_points numeric default null, p_request uuid default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return (economy.repair_core(v_user, p_id, p_points, p_request, null, true) ->> 'durability')::numeric;
end $$;
revoke all on function public.sneaker_repair(bigint, numeric, uuid) from public, anon;
grant execute on function public.sneaker_repair(bigint, numeric, uuid) to authenticated;

-- 새 앱 — 확인한 값(p_expected)이 달라졌으면 고치지 않고 새 값을 돌려준다. 가진 만큼 수리는 p_points
create or replace function public.sneaker_repair_v2(p_id bigint, p_points numeric default null, p_request uuid default null,
    p_expected numeric default null)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return economy.repair_core(v_user, p_id, p_points, p_request, p_expected, false);
end $$;
revoke all on function public.sneaker_repair_v2(bigint, numeric, uuid, numeric) from public, anon;
grant execute on function public.sneaker_repair_v2(bigint, numeric, uuid, numeric) to authenticated;

-- 신발 목록 — 칸 그대로. 수리 단가는 그 신발의 값, 무료 수리가 남아 있으면 0(예전 앱도 "0 SUP"로 보이고 잔고 없이 된다)
create or replace function public.my_sneakers()
returns table (id bigint, faction text, rarity text, variant integer, level integer, max_level integer,
  efficiency_bps integer, comfort_bps integer, durability numeric, equipped boolean, origin text, chain_state text,
  status text, mint_number bigint, genesis_no integer, token_id numeric, km_run numeric, lock_km numeric,
  can_withdraw boolean, upgrade_cost numeric, repair_cost_per_point numeric, model_id integer,
  durability_now integer, durability_max integer, durability_base integer, efficiency_base_bps integer,
  comfort_base_bps integer, points_total integer, points_free integer, points_efficiency integer,
  points_comfort integer, points_durability integer, points_respec boolean, repair_cost_per_unit numeric)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_free boolean;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  v_free := economy.free_repair_ready(v_user);
  return query
  select s.id, s.faction, s.rarity, s.variant, s.level, economy.max_level(s.rarity),
         e.efficiency_bps, e.comfort_bps, s.durability_pts,
         s.equipped, s.origin, s.chain_state, s.status,
         s.mint_number, s.genesis_no, s.token_id,
         s.km_run, s.lock_km,
         (s.withdrawable and s.km_run >= s.lock_km and s.chain_state = 'APP' and s.status = 'OWNED'),
         economy.upgrade_cost(s.rarity, s.level),
         case when v_free then 0::numeric else round(economy.repair_unit_of(s) * s.dur_max / 100, 4) end,
         s.model_id,
         s.dur_now, s.dur_max, s.dur_base,
         case when s.origin in ('IMPORT', 'MINT') then 0 else s.efficiency_bps end,
         case when s.origin in ('IMPORT', 'MINT') then 0 else s.comfort_bps end,
         e.points_total, e.points_free,
         s.pts_eff, s.pts_comfort, s.pts_dur, s.pts_respec,
         case when v_free then 0::numeric else economy.repair_unit_of(s) end
    from public.market_sneakers s,
         lateral economy.shoe_effective(s) e
   where s.owner_id = v_user
   order by s.equipped desc, s.id;
end $$;

-- 새 앱이 읽는 신발 규칙 — 범위 · 포인트 · 수리 · 무료 수리 · 운. 앱은 숫자를 지어내지 않는다
create or replace function public.shoe_rules_v4() returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  return jsonb_build_object(
    'points_per_step', economy.points_per_step(),
    'repair_share', coalesce(economy.setting_num('repair_share'), 0.45),
    'reward_luck', economy.setting('reward_luck'),
    'wear_luck', economy.setting('wear_luck'),
    'comfort_cap_bps', economy.comfort_cap_bps(),
    'free_repair_hours', economy.setting_num('free_repair_hours'),
    'free_repair_ready', case when v_user is null then null else economy.free_repair_ready(v_user) end,
    'free_repair_next', case when v_user is null then null else economy.free_repair_next(v_user) end,
    'forge_material_min_durability', economy.setting_num('forge_material_min_durability'),
    'boost_shop_enabled', coalesce((economy.setting('boost_shop_enabled') #>> '{}')::boolean, false),
    'grades', (select jsonb_agg(jsonb_build_object(
        'rarity', g.r,
        'efficiency', jsonb_build_array((economy.efficiency_range(g.r)).lo, (economy.efficiency_range(g.r)).hi),
        'comfort', jsonb_build_array((economy.comfort_range(g.r)).lo, (economy.comfort_range(g.r)).hi),
        'durability', jsonb_build_array((economy.durability_range(g.r)).lo, (economy.durability_range(g.r)).hi),
        'points_per_level', economy.points_per_level_of(g.r),
        'points_milestone', economy.points_milestone_of(g.r),
        'point_rate_bps', economy.point_rate_of(g.r),
        'max_level', economy.max_level(g.r)) order by g.o)
      from (values ('COMMON', 0), ('RARE', 1), ('EPIC', 2), ('LEGENDARY', 3), ('REDLINE', 4), ('FINISH', 5)) g(r, o)));
end $$;
revoke all on function public.shoe_rules_v4() from public;
grant execute on function public.shoe_rules_v4() to anon, authenticated;

-- ── 부스터 판매 멈춤 — 0057 본문 그대로, 맨 앞에 설정 확인 ─────────────────
create or replace function public.boost_buy(p_kind text, p_request uuid default null) returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_cost numeric := economy.boost_cost(p_kind);
  v_day date := economy.game_day(now());
  v_balance numeric;
  v_from timestamptz;
  v_prev numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if v_cost is null then
    raise exception '없는 부스터입니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r
     where r.user_id = v_user and r.request_id = p_request;
    if found then
      return v_prev;
    end if;
  end if;
  if not coalesce((economy.setting('boost_shop_enabled') #>> '{}')::boolean, false) then
    raise exception '지금은 부스터를 팔지 않아요. 아이템은 이벤트 · 보상으로 받을 수 있어요' using errcode = '23514';
  end if;
  if p_kind = 'ENERGY_CELL' then
    if economy.energy_left(v_user, v_day) + 2 > economy.energy_max(v_user) then
      raise exception '에너지가 이미 충분합니다' using errcode = '23514';
    end if;
  end if;
  v_balance := economy.ledger_apply(v_user, 'SPEND_BOOST', -v_cost, '부스터: ' || p_kind);
  if p_kind = 'ENERGY_CELL' then
    insert into public.energy_days (user_id, day, bonus) values (v_user, v_day, 2)
    on conflict (user_id, day) do update set bonus = public.energy_days.bonus + 2;
    insert into public.boosts (user_id, kind, cost) values (v_user, p_kind, v_cost);
  else
    select greatest(now(), coalesce(max(ends_at), now())) into v_from
      from public.boosts where user_id = v_user and kind = p_kind and ends_at > now();
    insert into public.boosts (user_id, kind, starts_at, ends_at, cost)
    values (v_user, p_kind, v_from, v_from + interval '24 hours', v_cost);
  end if;
  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result)
    values (v_user, p_request, 'BOOST:' || p_kind, v_balance);
  end if;
  return v_balance;
end $$;

-- ── 강화 재료 — 내구도 50% 이상 (0054 그대로 + WORN) ─────────────────────
create or replace function economy.forge_material_block(p_user uuid, m public.market_sneakers, t public.market_sneakers)
returns text
language plpgsql stable security definer set search_path = public, economy as $$
begin
  if m.id is null or m.owner_id is distinct from p_user then return 'GONE'; end if;
  if m.id = t.id then return 'TARGET'; end if;
  if economy.rarity_ord(m.rarity) is null or economy.rarity_ord(m.rarity) < economy.rarity_ord(t.rarity) - 1 then
    return 'GRADE';
  end if;
  if m.status <> 'OWNED' then return 'LISTED'; end if;
  if m.equipped then return 'EQUIPPED'; end if;
  if m.origin in ('STARTER', 'IMPORT', 'MINT') then return 'LOCKED'; end if;
  if m.chain_state <> 'APP' or m.token_id is not null or economy.forge_chain_busy(m.id) then return 'ON_CHAIN'; end if;
  if m.durability_pts < coalesce(economy.setting_num('forge_material_min_durability'), 0) then return 'WORN'; end if;
  if m.level is null or m.level < 1 then return 'LEVEL'; end if;
  return null;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  상자 — 출석 · 러닝 · 주간 · 연속 · 레벨 · 구매
-- ════════════════════════════════════════════════════════════════════
create table if not exists economy.box_awards (
  user_id uuid not null references auth.users (id) on delete cascade,
  source text not null,               -- 'RUN2000:2026-11-18' · 'WEEK:2026-11-16' · 'STREAK:2026-11-18' · 'LEVEL:5'
  kind text not null check (kind in ('FREE', 'GIFT')),
  day date,
  granted_at timestamptz not null default now(),
  primary key (user_id, source)
);

-- 한 번만 주기 — 처음이면 상자 하나(FREE = 일반, GIFT = 상급)를 더하고 true
create or replace function economy.box_grant(p_user uuid, p_source text, p_kind text, p_day date default null) returns boolean
language plpgsql security definer set search_path = public, economy as $$
begin
  insert into economy.box_awards (user_id, source, kind, day) values (p_user, p_source, p_kind, p_day)
  on conflict (user_id, source) do nothing;
  if not found then
    return false;
  end if;
  insert into public.draw_grants as d (user_id, kind, granted) values (p_user, p_kind, 1)
  on conflict (user_id, kind) do update set granted = d.granted + 1;
  return true;
end $$;
revoke all on function economy.box_grant(uuid, text, text, date) from public;

-- 한국 날짜의 주(월요일 시작)
create or replace function economy.game_week(p_day date) returns date
language sql immutable as $$ select p_day - (extract(isodow from p_day)::int - 1) $$;

-- 러닝을 올리면 — 그날 적립 걸음 2,000 · 4,000 이면 일반 상자, 그 주 25,000 이면 상급 상자
create or replace function economy.box_on_session() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_day date;
  v_week date;
  v_steps bigint;
  v_t jsonb;
begin
  if new.verdict = 'VOID' or coalesce(new.rewarded_steps, 0) <= 0 then
    return null;
  end if;
  v_day := economy.game_day(new.started_at);
  select coalesce(sum(s.rewarded_steps), 0) into v_steps from public.walk_sessions s
   where s.user_id = new.user_id and s.verdict in ('CLEAN', 'FLAGGED') and economy.game_day(s.started_at) = v_day;
  for v_t in select jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) loop
    if v_steps >= (v_t #>> '{}')::numeric then
      perform economy.box_grant(new.user_id, 'RUN' || (v_t #>> '{}') || ':' || v_day, 'FREE', v_day);
    end if;
  end loop;
  if coalesce(economy.setting_num('weekly_box_steps'), 0) > 0 then
    v_week := economy.game_week(v_day);
    select coalesce(sum(s.rewarded_steps), 0) into v_steps from public.walk_sessions s
     where s.user_id = new.user_id and s.verdict in ('CLEAN', 'FLAGGED')
       and economy.game_day(s.started_at) between v_week and v_week + 6;
    if v_steps >= economy.setting_num('weekly_box_steps') then
      perform economy.box_grant(new.user_id, 'WEEK:' || v_week, 'GIFT', v_week);
    end if;
  end if;
  return null;
end $$;
revoke all on function economy.box_on_session() from public;
drop trigger if exists walk_sessions_boxes on public.walk_sessions;
create trigger walk_sessions_boxes after insert on public.walk_sessions
  for each row execute function economy.box_on_session();

-- 신발이 5 · 10 · 15 …레벨에 닿으면 상급 상자 — 계정마다 그 레벨 처음 한 번
create or replace function economy.box_on_level() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare v_l int;
begin
  if new.owner_id is null or new.level / 5 <= old.level / 5 then
    return null;
  end if;
  for v_l in select generate_series((old.level / 5 + 1) * 5, (new.level / 5) * 5, 5) loop
    perform economy.box_grant(new.owner_id, 'LEVEL:' || v_l, 'GIFT', economy.game_day(now()));
  end loop;
  return null;
end $$;
revoke all on function economy.box_on_level() from public;
drop trigger if exists market_sneakers_level_boxes on public.market_sneakers;
create trigger market_sneakers_level_boxes after update of level on public.market_sneakers
  for each row execute function economy.box_on_level();

-- 이미 그 레벨에 닿아 있던 사람 — 한 번만 같은 상자를 준다(포인트를 돌려준 것과 함께)
do $$
declare r record; v_l int;
begin
  if not exists (select 1 from public.economy_settings where key = 'economy_v4_level_boxes') then
    for r in select s.owner_id, max(s.level) as lv from public.market_sneakers s
              where s.owner_id is not null and s.origin not in ('IMPORT', 'MINT') group by s.owner_id having max(s.level) >= 5 loop
      for v_l in select generate_series(5, (r.lv / 5) * 5, 5) loop
        perform economy.box_grant(r.owner_id, 'LEVEL:' || v_l, 'GIFT', economy.game_day(now()));
      end loop;
    end loop;
    insert into public.economy_settings (key, value) values ('economy_v4_level_boxes', to_jsonb(now()));
  end if;
end $$;

-- 7일 연속 목표 — goal_claim_day(0068) 그대로 + 이어진 날이 7 이 되면 상급 상자(이레에 한 번)
create or replace function public.goal_claim_day(p_day date) returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
  v_goal int;
  v_streak int := 1;
  v_day date;
  v_bonus numeric;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_day is null or p_day > v_today or p_day < v_today - 7 then
    raise exception '목표 보너스는 최근 7일 안의 날만 받을 수 있습니다' using errcode = '22023';
  end if;

  select least(greatest(coalesce(p.daily_goal, 8000), 1000), 30000) into v_goal
    from public.profiles p where p.id = v_user;

  if economy.verified_steps_on(v_user, p_day) < v_goal then
    raise exception '아직 목표를 채우지 않았습니다' using errcode = '23514';
  end if;

  v_day := p_day - 1;
  while v_streak < greatest(8, v_need) loop
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
  perform economy.ledger_apply(v_user, 'BONUS_GOAL', v_bonus,
                               format('목표 달성 보너스 (%s일 연속)', least(v_streak, 8)), 'goal:' || p_day);
  if p_day < v_today then
    update public.sup_ledger l set occurred_at = economy.game_day_start(p_day + 1) - interval '1 second'
     where l.user_id = v_user and l.ref = 'goal:' || p_day;
  end if;

  if v_need > 0 and v_streak >= v_need and not exists (
       select 1 from economy.box_awards a
        where a.user_id = v_user and a.source like 'STREAK:%' and a.day > p_day - v_need and a.day <= p_day) then
    perform economy.box_grant(v_user, 'STREAK:' || p_day, 'GIFT', p_day);
  end if;
  return v_bonus;
end $$;

-- 상자 사기 — 일반 40 SUP(하루 3) · 상급 600 SUP(한 주 2). 산 상자는 무료 · 상급 뽑기 수로 쌓인다
create table if not exists economy.box_buys (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users (id) on delete cascade,
  kind text not null check (kind in ('NORMAL', 'PREMIUM')),
  day date not null,
  price numeric(20, 4) not null,
  bought_at timestamptz not null default now()
);
create index if not exists box_buys_user_day on economy.box_buys (user_id, day);

create or replace function public.box_buy(p_kind text, p_request uuid default null) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_price numeric;
  v_limit int;
  v_used int;
  v_prev numeric;
  v_balance numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_kind not in ('NORMAL', 'PREMIUM') then
    raise exception '없는 상자입니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  if p_request is not null then
    select r.result into v_prev from economy.request_receipts r where r.user_id = v_user and r.request_id = p_request;
    if found then
      return jsonb_build_object('ok', true, 'kind', p_kind, 'balance', v_prev, 'replay', true);
    end if;
  end if;
  if p_kind = 'NORMAL' then
    v_price := economy.setting_num('box_price');
    v_limit := coalesce(economy.setting_num('box_daily_buy'), 0)::int;
    select count(*) into v_used from economy.box_buys b where b.user_id = v_user and b.kind = 'NORMAL' and b.day = v_day;
  else
    v_price := economy.setting_num('premium_box_price');
    v_limit := coalesce(economy.setting_num('premium_box_weekly_buy'), 0)::int;
    select count(*) into v_used from economy.box_buys b
     where b.user_id = v_user and b.kind = 'PREMIUM' and b.day between economy.game_week(v_day) and economy.game_week(v_day) + 6;
  end if;
  if v_price is null or v_price <= 0 or v_limit <= 0 then
    raise exception '지금은 상자를 팔지 않아요' using errcode = '23514';
  end if;
  if v_used >= v_limit then
    raise exception '%', case when p_kind = 'NORMAL' then format('오늘은 상자를 %s개까지 살 수 있어요', v_limit)
                              else format('상급 상자는 한 주에 %s개까지 살 수 있어요', v_limit) end
      using errcode = '23514';
  end if;
  v_balance := economy.ledger_apply(v_user, 'SPEND_DRAW', -v_price,
                                    case when p_kind = 'NORMAL' then '상자 사기' else '상급 상자 사기' end);
  insert into economy.box_buys (user_id, kind, day, price) values (v_user, p_kind, v_day, v_price);
  insert into public.draw_grants as d (user_id, kind, granted)
  values (v_user, case when p_kind = 'NORMAL' then 'FREE' else 'GIFT' end, 1)
  on conflict (user_id, kind) do update set granted = d.granted + 1;
  if p_request is not null then
    insert into economy.request_receipts (user_id, request_id, kind, result) values (v_user, p_request, 'BOX:' || p_kind, v_balance);
  end if;
  return jsonb_build_object('ok', true, 'kind', p_kind, 'price', v_price, 'balance', v_balance, 'replay', false);
end $$;
revoke all on function public.box_buy(text, uuid) from public, anon;
grant execute on function public.box_buy(text, uuid) to authenticated;

-- 상자 현황 — 새 앱의 "상자 받는 곳" 화면
create or replace function public.box_status() returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_week date := economy.game_week(economy.game_day(now()));
  v_steps_day bigint;
  v_steps_week bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select coalesce(sum(s.rewarded_steps), 0) into v_steps_day from public.walk_sessions s
   where s.user_id = v_user and s.verdict in ('CLEAN', 'FLAGGED') and economy.game_day(s.started_at) = v_day;
  select coalesce(sum(s.rewarded_steps), 0) into v_steps_week from public.walk_sessions s
   where s.user_id = v_user and s.verdict in ('CLEAN', 'FLAGGED') and economy.game_day(s.started_at) between v_week and v_week + 6;
  return jsonb_build_object(
    'day', v_day,
    'daily_free', coalesce(economy.setting_num('daily_free_draws'), 0),
    'steps_today', v_steps_day,
    'run_boxes', (select coalesce(jsonb_agg(jsonb_build_object('steps', (t #>> '{}')::int,
                     'got', exists (select 1 from economy.box_awards a where a.user_id = v_user
                                     and a.source = 'RUN' || (t #>> '{}') || ':' || v_day))), '[]'::jsonb)
                    from jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) t),
    'week_start', v_week,
    'steps_week', v_steps_week,
    'week_target', economy.setting_num('weekly_box_steps'),
    'week_got', exists (select 1 from economy.box_awards a where a.user_id = v_user and a.source = 'WEEK:' || v_week),
    'streak_days', economy.setting_num('streak_box_days'),
    'levels_got', (select coalesce(jsonb_agg(substr(a.source, 7)::int order by substr(a.source, 7)::int), '[]'::jsonb)
                     from economy.box_awards a where a.user_id = v_user and a.source like 'LEVEL:%'),
    'normal_price', economy.setting_num('box_price'),
    'normal_limit', economy.setting_num('box_daily_buy'),
    'normal_bought', (select count(*) from economy.box_buys b where b.user_id = v_user and b.kind = 'NORMAL' and b.day = v_day),
    'premium_price', economy.setting_num('premium_box_price'),
    'premium_limit', economy.setting_num('premium_box_weekly_buy'),
    'premium_bought', (select count(*) from economy.box_buys b where b.user_id = v_user and b.kind = 'PREMIUM'
                         and b.day between v_week and v_week + 6),
    'free_left', coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'FREE'), 0),
    'gift_left', coalesce((select g.granted - g.used from public.draw_grants g where g.user_id = v_user and g.kind = 'GIFT'), 0),
    'balance', economy.balance_of(v_user));
end $$;
revoke all on function public.box_status() from public, anon;
grant execute on function public.box_status() to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  러닝 — 운(보상 · 닳음) · 하루 상한은 가진 신발의 효율을 따라
-- ════════════════════════════════════════════════════════════════════
alter table public.walk_sessions add column if not exists reward_luck numeric(6, 3);
alter table public.walk_sessions add column if not exists wear_luck numeric(6, 3);
comment on column public.walk_sessions.reward_luck is '이 러닝 보상의 운(0.7 ~ 1.3, 평균 1) — 상한 전 적립에 곱했다 (0116)';
comment on column public.walk_sessions.wear_luck is '이 러닝에 신발이 닳은 양의 운(0.667 ~ 1.444, 평균 1) (0116)';

-- record_session — 0110 본문 그대로, 다섯 곳만 (0116):
--   ① 보상 운(v_luck) — 개인 · 크루 적립 모두 상한 전에 곱한다  ② 하루 금액 상한 · 올린 날 상한 × economy.earn_cap_factor
--   ③ 닳는 양 운(v_wear_luck) — v_wear 에 곱하고, 신발은 v_wear 만큼 닳는다  ④ 두 운을 러닝 기록에 적는다
--   ⑤ 돌려주는 줄에 reward_luck 을 더한다(예전 앱은 모르는 칸을 읽지 않는다 — serverJson ignoreUnknownKeys) — 돌려주는 모양이 바뀌어 지우고 다시 만든다
--   다시 고칠 때는 이 본문(0116)을 복사한다.
drop function if exists public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint);
create function public.record_session(
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
  verdict_reason text,
  -- 이 러닝 보상의 운(0116) — 무효면 null
  reward_luck numeric
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
  -- 이 러닝 · 이 신발의 과속 패널티 — 신고가 먼저 와 신발을 이미 절반으로 깎았다 (0110)
  v_pen public.run_penalties;
  -- 이 러닝이 신발을 닳게 하는 양(크루 러닝은 0, 0110)
  v_wear int := 0;
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
  -- 운 (0116) — 보상 × 0.7 ~ 1.3 · 닳는 양 × 0.667 ~ 1.444(평균 1)
  v_luck numeric := 1;
  v_wear_luck numeric := 1;
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
  select s.id, s.verdict, s.points_awarded, coalesce(s.verdict_reason, ''), s.reward_luck
    into v_session_id, v_verdict, v_points, v_reason, v_luck
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason, v_luck;
    return;
  end if;
  v_reason := '';
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

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at, p_ended_at) pf;  -- 0109 #13: 출발 전에 끝난 러닝은 그 방 러닝이 아니다
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
    if v_cr.state = 'ENDED' and v_cr.terminal_reason in ('USER_END', 'LAST_PARTICIPANT_END') and v_cr.ended_at is not null then
      v_cut := v_cr.ended_at + interval '60 seconds';
    end if;
    -- 공용 체력을 지금까지 정산하고(풀 → 사람 차례로 잠근다) 이 러닝을 끝낸다 — 다른 사람이 모두 끝났으면 마지막 사람도
    perform economy.crew_pool_settle(v_cr.crew_id, v_cr.day, now());
    perform economy.crew_run_close(v_user, v_cr.day, 'RECORDED');
    -- 인원 보너스 — 2분이 지났으면 지금 정하고, 아니면 확인할 수 없다(D01)
    perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
    select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
    v_headcount := v_cr.bonus_pct;
    v_level_bonus := v_cr.level_pct;
  end if;

  -- ── 하루 걸음 상한 (한국 하루) — 개인 러닝만, 크루 러닝 걸음은 세지 않는다 (0070) ──
  if v_rewardable > 0 and v_pool = 'PERSONAL' then
    select coalesce(sum(s.rewarded_steps), 0) into v_already_steps
      from public.walk_sessions s
     where s.user_id = v_user
       and s.verdict in ('CLEAN', 'FLAGGED')
       and s.energy_pool <> 'CREW'
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
     where x.id = p_sneaker and x.owner_id = v_user and x.chain_state = 'APP' and x.status = 'OWNED';
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
  -- 과속으로 멈춘 러닝 — 신고(report_overspeed_stop)가 먼저 와 이 신발을 이미 절반으로 깎았다 (0110).
  -- 멈춘 러닝 자체는 평소처럼 정산한다(0065) — 수명 자르기 · 적립 배율은 신고 전 내구도로 센다. 여기서는 v_shoe(이 함수의 사본)만
  -- 되돌린다. 표의 신발은 아래 "신발이 닳는다"에서 (신고 전 − 이 러닝의 닳음)의 절반으로 맞춘다.
  -- 패널티가 다른 신발에 갔으면(예전 앱 · 출발 뒤 신발을 바꿈) 이 신발은 그대로 정산하고 그 신발의 패널티도 그대로다
  if v_shoe.id is not null then
    select x.* into v_pen from public.run_penalties x
     where x.user_id = v_user and x.run_started_at = p_started_at and x.sneaker_id = v_shoe.id
       and x.dur_before is not null and x.dur_after is not null;
    if v_pen.user_id is not null then
      v_shoe.dur_now := least(greatest(v_pen.dur_before, 0), v_shoe.dur_max);
      v_shoe.durability_pts := round(100.0 * v_shoe.dur_now / greatest(v_shoe.dur_max, 1), 2);
    end if;
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
      -- 에너지 드링크를 고른 러닝은 그 비율만큼만 쓴다 (0106)
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy()
                           * economy.run_energy_factor(v_user, p_started_at);
      v_energy_left := economy.energy_left(v_user, v_gday);
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
  end if;

  -- 적립 배수 — 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템 가운데 가장 큰 것 하나(겹치지 않는다, 0106)
  v_xp := economy.run_reward_mult(v_user, p_started_at);
  v_luck := economy.luck_draw('reward_luck');

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp * v_luck, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    v_points := round(
      v_rewardable * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * economy.durability_factor(v_dur)
        * economy.party_multiplier(v_party)
        * v_xp * v_luck,
      4
    );
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  if v_points > 0 and v_pool = 'PERSONAL' then
    v_new_account := (select p.created_at from public.profiles p where p.id = v_user)
                     > now() - make_interval(days => economy.setting_num('new_account_days')::int);
    -- 상한은 가진 신발 가운데 가장 센 효율을 따라간다 (0116)
    v_cap := economy.setting_num('daily_earn_cap') * economy.earn_cap_factor(v_user) * case when v_new_account then 0.5 else 1 end;
    select coalesce(sum(s.points_awarded), 0) into v_already_points
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW' and economy.game_day(s.started_at) = v_gday;
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
  if v_points > 0 and not v_gps_backed and v_pool = 'PERSONAL' then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and s.energy_pool <> 'CREW'
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and v_pool = 'PERSONAL' then
    select economy.setting_num('upload_daily_cap') * economy.earn_cap_factor(v_user) - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW'
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
  --
  -- 과속 패널티가 먼저 깎은 러닝(v_pen, 0110): B = 신고 전 내구도(dur_before), A = 신고가 남긴 값(dur_after = B / 2),
  -- w = 이 러닝의 닳음(크루 러닝은 0). 맞는 끝값은 (B − w) / 2(버림, 0 아래로 가지 않음) — 평소처럼 닳은 뒤 남은 것의 절반.
  -- 표에는 이미 A 가 있으니 그 차이((B − w) / 2 − A, 0 또는 음수)만큼 더한다 — 신고와 정산 사이에 바뀐 값(수리 등)은 그대로 둔다.
  -- 무효 러닝은 닳지 않아(w = 0) 신고가 남긴 A 그대로다
  v_wear_luck := economy.luck_draw('wear_luck');
  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(v_elapsed) * v_wear_luck)::int end;
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pen.user_id is not null
                            then greatest(s.dur_now + (greatest(v_pen.dur_before - v_wear, 0) / 2 - v_pen.dur_after), 0)
                          when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - v_wear, 0) end,
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

  -- 이 러닝에 묶인 아이템을 러닝 기록에 잇고, 적용된 적립 배수를 적는다 (0106)
  update economy.item_uses u set session_id = v_session_id
   where u.user_id = v_user and u.run_started_at = p_started_at;
  if v_xp <> 1 then
    update public.walk_sessions s set reward_mult = v_xp where s.id = v_session_id;
  end if;
  if v_verdict <> 'VOID' then
    update public.walk_sessions s
       set reward_luck = round(v_luck, 3),
           wear_luck = case when v_pool = 'CREW' then null else round(v_wear_luck, 3) end
     where s.id = v_session_id;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason,
    case when v_verdict = 'VOID' then null else round(v_luck, 3) end;
end;
$$;


comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089) · 운 · 상한 배수 (0116)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  앱 판 보고 · 운영자 — 바로 적용 · 받는 사람의 앱 판 · 경제 현황 · 경제 설정
-- ════════════════════════════════════════════════════════════════════
create table if not exists public.app_clients (
  user_id uuid primary key references auth.users (id) on delete cascade,
  version_code int not null,
  version_name text not null default '',
  seen_at timestamptz not null default now()
);
alter table public.app_clients enable row level security;

-- 새 앱(1.19.0 부터)이 동기화 때 자기 판을 알린다. 알리지 않은 사람은 예전 판으로 본다
create or replace function public.app_client_report(p_version_code int, p_version_name text default '') returns void
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null or p_version_code is null or p_version_code < 1 then
    return;
  end if;
  insert into public.app_clients as c (user_id, version_code, version_name) values (v_user, p_version_code, left(coalesce(p_version_name, ''), 40))
  on conflict (user_id) do update set version_code = excluded.version_code, version_name = excluded.version_name, seen_at = now();
end $$;
revoke all on function public.app_client_report(int, text) from public, anon;
grant execute on function public.app_client_report(int, text) to authenticated;

-- 운영자 대상 고르기 — ops_item_grant 와 같은 규칙(EMAILS · RUNNERS_7D · ALL)
create or replace function economy.ops_targets(p_audience text, p_emails text[], out users uuid[], out missing text[], out target text)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_emails text[];
begin
  missing := '{}';
  if p_audience = 'ALL' then
    target := 'ALL';
    select coalesce(array_agg(p.id), '{}') into users from public.profiles p;
  elsif p_audience = 'RUNNERS_7D' then
    target := 'RUNNERS_7D';
    select coalesce(array_agg(distinct s.user_id), '{}') into users from public.walk_sessions s
     where s.started_at > now() - interval '7 days' and s.verdict <> 'VOID';
  elsif p_audience is null or p_audience = 'EMAILS' then
    target := 'EMAILS';
    select coalesce(array_agg(distinct lower(btrim(e))), '{}') into v_emails
      from unnest(coalesce(p_emails, '{}')) e where btrim(coalesce(e, '')) <> '';
    select coalesce(array_agg(u.id), '{}') into users
      from auth.users u join public.profiles p on p.id = u.id where lower(u.email) = any (v_emails);
    select coalesce(array_agg(e order by e), '{}') into missing from unnest(v_emails) e
     where not exists (select 1 from auth.users u join public.profiles p on p.id = u.id where lower(u.email) = e);
  else
    target := null;
    users := '{}';
  end if;
end $$;
revoke all on function economy.ops_targets(text, text[]) from public;

-- 받는 사람 가운데 아이템 가방이 있는 앱(새 판)을 쓰는 사람 수 — 지급 전에 운영자 페이지가 묻는다
create or replace function public.ops_audience_preview(p_token text, p_audience text, p_emails text[] default null) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  t record;
  v_current int := coalesce(economy.setting_num('app_current_version_code'), 27)::int;
  v_new int;
begin
  select * into t from economy.ops_targets(p_audience, p_emails);
  if t.target is null then
    return jsonb_build_object('ok', false, 'code', 'audience', 'message', '대상이 올바르지 않습니다');
  end if;
  select count(*) into v_new from public.app_clients c where c.user_id = any (t.users) and c.version_code >= v_current;
  return jsonb_build_object('ok', true, 'users', cardinality(t.users), 'new_app', v_new,
                            'old_app', cardinality(t.users) - v_new, 'missing', to_jsonb(t.missing),
                            'current_version_code', v_current);
end $$;
revoke all on function public.ops_audience_preview(text, text, text[]) from public;
grant execute on function public.ops_audience_preview(text, text, text[]) to anon, authenticated;

-- 바로 적용 — 아이템을 거치지 않고 서버가 직접(예전 판에도 다음 동기화부터 보인다)
--   DURABILITY_FULL: 그 사람의 앱 안 신발 모두 내구도 가득 · ENERGY_RESET: 오늘 쓴 에너지 되돌리기
create table if not exists economy.ops_applies (
  id bigint generated always as identity primary key,
  request_id uuid not null unique,
  action text not null,
  reason text not null,
  target text not null,
  actor text not null,
  users int not null,
  shoes int not null default 0,
  created_at timestamptz not null default now()
);

create or replace function public.ops_apply_now(
  p_token text, p_request uuid, p_action text, p_reason text,
  p_emails text[] default null, p_audience text default null, p_confirm text default null
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_reason text := btrim(coalesce(p_reason, ''));
  t record;
  v_prev economy.ops_applies;
  v_day date := economy.game_day(now());
  v_units int;
  v_limit int;
  v_used int;
  v_refuse jsonb;
  v_shoes int := 0;
begin
  if p_request is null then
    return jsonb_build_object('ok', false, 'code', 'request', 'message', '요청 번호가 필요합니다');
  end if;
  perform pg_advisory_xact_lock(hashtext('ops-apply:' || p_request::text));
  select * into v_prev from economy.ops_applies a where a.request_id = p_request;
  if found then
    return jsonb_build_object('ok', true, 'users', v_prev.users, 'shoes', v_prev.shoes, 'replay', true);
  end if;
  if p_action not in ('DURABILITY_FULL', 'ENERGY_RESET') then
    return jsonb_build_object('ok', false, 'code', 'action', 'message', '없는 동작입니다');
  end if;
  if char_length(v_reason) < 2 or char_length(v_reason) > 80 then
    return jsonb_build_object('ok', false, 'code', 'reason', 'message', '사유를 2 ~ 80자로 적어 주세요');
  end if;
  select * into t from economy.ops_targets(p_audience, p_emails);
  if t.target is null then
    return jsonb_build_object('ok', false, 'code', 'audience', 'message', '대상이 올바르지 않습니다');
  end if;
  if cardinality(t.users) = 0 then
    return jsonb_build_object('ok', false, 'code', 'no_target', 'message', '받을 사람이 없습니다', 'missing', to_jsonb(t.missing));
  end if;

  v_units := cardinality(t.users);
  perform pg_advisory_xact_lock(hashtext('ops-cap:' || v_session.key_hash));
  v_limit := economy.ops_num('ops_key_daily_units', 5000)::int;
  select coalesce(sum(d.units), 0) into v_used from economy.ops_key_days d where d.key_hash = v_session.key_hash and d.day = v_day;
  if v_used + v_units > v_limit then
    return jsonb_build_object('ok', false, 'code', 'cap', 'units', v_units, 'used', v_used, 'limit', v_limit,
      'message', format('이 키로 오늘 더 할 수 있는 양은 %s이에요(하루 %s — 사람 수)', greatest(v_limit - v_used, 0), v_limit));
  end if;
  v_refuse := economy.ops_confirm(v_session, p_confirm,
    case when t.target = 'ALL' then '전체에 바로 적용' else format('%s명에게 바로 적용', cardinality(t.users)) end,
    t.target = 'ALL' or cardinality(t.users) > economy.ops_num('ops_confirm_recipients', 200));
  if v_refuse is not null then
    return v_refuse;
  end if;

  insert into economy.ops_key_days (key_hash, day, units) values (v_session.key_hash, v_day, v_units)
  on conflict (key_hash, day) do update set units = economy.ops_key_days.units + excluded.units;

  if p_action = 'DURABILITY_FULL' then
    update public.market_sneakers s set dur_now = s.dur_max, updated_at = now()
     where s.owner_id = any (t.users) and s.status = 'OWNED' and s.chain_state = 'APP' and s.dur_now < s.dur_max;
    get diagnostics v_shoes = row_count;
  else
    update public.energy_days d set bonus = d.used
     where d.user_id = any (t.users) and d.day = v_day and d.used > d.bonus;
    get diagnostics v_shoes = row_count;
  end if;

  insert into economy.ops_applies (request_id, action, reason, target, actor, users, shoes)
  values (p_request, p_action, v_reason, t.target, v_actor, cardinality(t.users), v_shoes);
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_apply_now', p_action,
          jsonb_build_object('by', v_actor, 'users', cardinality(t.users), 'changed', v_shoes, 'target', t.target,
                             'reason', v_reason));
  return jsonb_build_object('ok', true, 'users', cardinality(t.users), 'shoes', v_shoes,
                            'missing', to_jsonb(t.missing), 'replay', false);
end $$;
revoke all on function public.ops_apply_now(text, uuid, text, text, text[], text, text) from public;
grant execute on function public.ops_apply_now(text, uuid, text, text, text[], text, text) to anon, authenticated;

-- 경제 현황 — 지난 7일(개인 러닝) · 어제 · 막힌 사람
create or replace function public.ops_economy_status(p_token text) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_runs jsonb;
  v_money jsonb;
  v_yday jsonb;
  v_stuck int;
  v_versions jsonb;
  v_current int := coalesce(economy.setting_num('app_current_version_code'), 27)::int;
begin
  select jsonb_build_object(
           'runs', count(*), 'people', count(distinct user_id),
           'avg_min', round(avg(duration_sec) / 60.0, 1),
           'over_10min_pct', round(100.0 * count(*) filter (where duration_sec > 600) / greatest(count(*), 1)),
           'durability_cut_pct', round(100.0 * count(*) filter (where verdict_reason like '%내구도가 다 닳은 뒤%') / greatest(count(*), 1)),
           'energy_cut_pct', round(100.0 * count(*) filter (where verdict_reason like '%에너지를 다 썼습니다%') / greatest(count(*), 1)),
           'zero_pct', round(100.0 * count(*) filter (where points_awarded = 0) / greatest(count(*), 1)),
           'avg_luck', round(avg(reward_luck), 3))
    into v_runs
    from public.walk_sessions
   where started_at > now() - interval '7 days' and verdict <> 'VOID' and energy_pool <> 'CREW';

  with per as (
    select user_id,
           coalesce(sum(amount) filter (where kind in ('EARN_WALK', 'EARN_PARTY')), 0) as earned,
           coalesce(-sum(amount) filter (where kind = 'SPEND_REPAIR'), 0) as repair,
           coalesce(-sum(amount) filter (where kind in ('SPEND_DRAW')), 0) as boxes
      from public.sup_ledger where occurred_at > now() - interval '7 days' group by user_id)
  select jsonb_build_object('earners', count(*) filter (where earned > 0), 'earned', round(sum(earned)),
                            'repair', round(sum(repair)), 'boxes', round(sum(boxes)),
                            'repair_over_earn', count(*) filter (where repair > earned))
    into v_money from per;

  select jsonb_build_object(
           'earned', round(coalesce(sum(amount) filter (where kind in ('EARN_WALK', 'EARN_PARTY')), 0)),
           'repair', round(coalesce(-sum(amount) filter (where kind = 'SPEND_REPAIR'), 0)),
           'boxes', round(coalesce(-sum(amount) filter (where kind = 'SPEND_DRAW'), 0)))
    into v_yday from public.sup_ledger
   where economy.game_day(occurred_at) = economy.game_day(now()) - 1;

  -- 막힌 사람: 앱 안 신발이 모두 0% 이고, 잔고가 가장 싼 가득 수리비보다 적고, 무료 수리도 아직
  select count(*) into v_stuck from public.profiles p
   where exists (select 1 from public.market_sneakers s where s.owner_id = p.id and s.status = 'OWNED' and s.chain_state = 'APP')
     and not exists (select 1 from public.market_sneakers s where s.owner_id = p.id and s.status = 'OWNED'
                       and s.chain_state = 'APP' and s.dur_now > 0)
     and not economy.free_repair_ready(p.id)
     and economy.balance_of(p.id) < (select min(economy.repair_unit_of(s) * s.dur_max) from public.market_sneakers s
                                      where s.owner_id = p.id and s.status = 'OWNED' and s.chain_state = 'APP');

  select jsonb_build_object('new_app', count(*) filter (where c.version_code >= v_current),
                            'reported', count(*), 'users', (select count(*) from public.profiles))
    into v_versions from public.app_clients c;

  return jsonb_build_object('ok', true, 'actor', v_actor, 'runs_7d', v_runs, 'money_7d', v_money,
                            'yesterday', v_yday, 'stuck', v_stuck, 'versions', v_versions);
end $$;
revoke all on function public.ops_economy_status(text) from public;
grant execute on function public.ops_economy_status(text) to anon, authenticated;

-- 경제 설정 — 운영자 페이지에서 바꿀 수 있는 값만(종류 · 범위를 본다), 언제나 키를 한 번 더
create or replace function economy.ops_setting_rule(p_key text, out kind text, out lo numeric, out hi numeric, out label text)
language sql immutable as $$
  select r.kind, r.lo, r.hi, r.label from (values
    ('points_per_step', 'num', 0.001, 0.05, '걸음당 SUP(효율 100 기준)'),
    ('repair_share', 'num', 0.1, 0.9, '수리비 = 걸어서 버는 돈의 몫'),
    ('reward_luck', 'luck', 0.3, 2.0, '보상 운 [가장 작게, 가장 자주, 가장 크게]'),
    ('wear_luck', 'luck', 0.3, 2.5, '닳는 양 운 [가장 작게, 가장 자주, 가장 크게]'),
    ('free_repair_hours', 'num', 0, 720, '무료 수리 간격(시간, 0 = 끔)'),
    ('boost_shop_enabled', 'bool', null, null, '부스터 판매'),
    ('daily_earn_cap', 'num', 100, 100000, '하루 적립 상한(가운데 일반 신발 기준)'),
    ('no_gps_daily_cap', 'num', 0, 10000, '경로 없는 러닝 하루 상한'),
    ('upload_daily_cap', 'num', 100, 200000, '하루에 올릴 수 있는 적립 상한(가운데 일반 기준)'),
    ('daily_free_draws', 'num', 0, 10, '하루 출석 상자'),
    ('run_box_steps', 'steps', 100, 100000, '러닝 상자 걸음(오늘 적립 걸음)'),
    ('weekly_box_steps', 'num', 0, 1000000, '주간 상급 상자 걸음(0 = 끔)'),
    ('streak_box_days', 'num', 0, 60, '연속 목표 상급 상자 날 수(0 = 끔)'),
    ('box_price', 'num', 0, 100000, '상자 값(SUP, 0 = 안 팜)'),
    ('box_daily_buy', 'num', 0, 50, '상자 하루 살 수 있는 수'),
    ('premium_box_price', 'num', 0, 1000000, '상급 상자 값(SUP, 0 = 안 팜)'),
    ('premium_box_weekly_buy', 'num', 0, 50, '상급 상자 한 주 살 수 있는 수'),
    ('forge_material_min_durability', 'num', 0, 100, '강화 재료 최소 내구도(%)'),
    ('app_current_version_code', 'num', 1, 999, '아이템 가방이 있는 앱 판(이 번호부터)')
  ) r(key, kind, lo, hi, label) where r.key = p_key
$$;

create or replace function public.ops_economy_settings(p_token text) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare v_actor text := economy.ops_actor(p_token);
begin
  return jsonb_build_object('ok', true, 'settings', (
    select jsonb_agg(jsonb_build_object('key', k.key, 'value', s.value, 'label', (economy.ops_setting_rule(k.key)).label,
                                        'kind', (economy.ops_setting_rule(k.key)).kind, 'updated_at', s.updated_at) order by k.o)
      from (values ('points_per_step', 1), ('repair_share', 2), ('reward_luck', 3), ('wear_luck', 4), ('free_repair_hours', 5),
                   ('boost_shop_enabled', 6), ('daily_earn_cap', 7), ('no_gps_daily_cap', 8), ('upload_daily_cap', 9),
                   ('daily_free_draws', 10), ('run_box_steps', 11), ('weekly_box_steps', 12), ('streak_box_days', 13),
                   ('box_price', 14), ('box_daily_buy', 15), ('premium_box_price', 16), ('premium_box_weekly_buy', 17),
                   ('forge_material_min_durability', 18), ('app_current_version_code', 19)) k(key, o)
      left join public.economy_settings s on s.key = k.key));
end $$;
revoke all on function public.ops_economy_settings(text) from public;
grant execute on function public.ops_economy_settings(text) to anon, authenticated;

create or replace function public.ops_economy_set(p_token text, p_key text, p_value jsonb, p_confirm text default null) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  r record;
  v_refuse jsonb;
  v_old jsonb;
  v_ok boolean := true;
  v_n numeric;
  e jsonb;
begin
  select * into r from economy.ops_setting_rule(p_key);
  if r.kind is null then
    return jsonb_build_object('ok', false, 'code', 'key', 'message', '여기서 바꿀 수 없는 설정입니다');
  end if;
  if p_value is null then
    v_ok := false;
  elsif r.kind = 'bool' then
    v_ok := jsonb_typeof(p_value) = 'boolean';
  elsif r.kind = 'num' then
    v_ok := jsonb_typeof(p_value) = 'number' and (p_value #>> '{}')::numeric between r.lo and r.hi;
  elsif r.kind = 'luck' then
    v_ok := jsonb_typeof(p_value) = 'array' and jsonb_array_length(p_value) = 3
            and (select bool_and(jsonb_typeof(x) = 'number' and (x #>> '{}')::numeric between r.lo and r.hi)
                   from jsonb_array_elements(p_value) x)
            and (p_value ->> 0)::numeric <= (p_value ->> 1)::numeric and (p_value ->> 1)::numeric <= (p_value ->> 2)::numeric;
  elsif r.kind = 'steps' then
    v_ok := jsonb_typeof(p_value) = 'array' and jsonb_array_length(p_value) between 0 and 5
            and coalesce((select bool_and(jsonb_typeof(x) = 'number' and (x #>> '{}')::numeric between r.lo and r.hi)
                            from jsonb_array_elements(p_value) x), true);
  end if;
  if not v_ok then
    return jsonb_build_object('ok', false, 'code', 'value',
      'message', case r.kind when 'bool' then '켬 · 끔(true · false)으로 넣어 주세요'
                             when 'luck' then format('[작게, 자주, 크게] 세 숫자(%s ~ %s, 작은 것부터)', r.lo, r.hi)
                             when 'steps' then format('걸음 수 목록(예: [2000, 4000], %s ~ %s)', r.lo, r.hi)
                             else format('%s ~ %s 사이 숫자', r.lo, r.hi) end);
  end if;
  v_refuse := economy.ops_confirm(v_session, p_confirm, format('경제 설정 바꾸기(%s)', r.label), true);
  if v_refuse is not null then
    return v_refuse;
  end if;
  select s.value into v_old from public.economy_settings s where s.key = p_key;
  insert into public.economy_settings (key, value) values (p_key, p_value)
  on conflict (key) do update set value = excluded.value, updated_at = now();
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_economy_set', p_key, jsonb_build_object('by', v_session.label, 'old', v_old, 'new', p_value));
  return jsonb_build_object('ok', true, 'key', p_key, 'old', v_old, 'value', p_value);
end $$;
revoke all on function public.ops_economy_set(text, text, jsonb, text) from public;
grant execute on function public.ops_economy_set(text, text, jsonb, text) to anon, authenticated;
