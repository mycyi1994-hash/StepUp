-- 2026-11-18 능력치 설계안(docs/shoe-stats-boxes-2026-11-18.md) — 계획 1단계 묶음 위에 능력치를 얹는다. 설계 검증용(아직 서버에 없다).
--   효율 · 착화감 · 내구도 범위를 넓히고(일반도 뽑는 값이 보이게), 레벨마다 포인트를 고르게(등급마다 4 · 5 · 6 · 7 · 8 · 10),
--   포인트 1 = 효율 +0.5%p · 착화감 +0.8%p(상한 40%) · 최대 내구도 + 기본의 1%, 분당 수리비는 계획 1단계와 같게.
create or replace function economy.points_per_step() returns numeric language sql immutable as $$ select 0.016::numeric $$;
update public.economy_settings set value = '960'::jsonb where key = 'daily_earn_cap';
update public.economy_settings set value = '96'::jsonb where key = 'no_gps_daily_cap';
update public.economy_settings set value = '1920'::jsonb where key = 'upload_daily_cap';
create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce((
    select max(e.level) from public.market_sneakers s,
           lateral economy.sneaker_effective(s.origin, s.rarity, s.level, s.efficiency_bps, s.comfort_bps, s.durability_pts) e
     where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'), 1))
$$;

create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 500 when 'RARE' then 1200 when 'EPIC' then 2200
                       when 'LEGENDARY' then 3400 when 'REDLINE' then 4800 else 6400 end,
         case p_rarity when 'COMMON' then 1500 when 'RARE' then 2400 when 'EPIC' then 3600
                       when 'LEGENDARY' then 5000 when 'REDLINE' then 6600 else 8400 end $$;
create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 300 when 'EPIC' then 600
                       when 'LEGENDARY' then 900 when 'REDLINE' then 1200 else 1400 end,
         case p_rarity when 'COMMON' then 600 when 'RARE' then 900 when 'EPIC' then 1200
                       when 'LEGENDARY' then 1500 when 'REDLINE' then 1800 else 2000 end $$;
-- 새 신발 한 켤레로 약 30 · 40 · 50 · 70 · 90 · 120분(1분마다 5 그대로), 뽑는 값은 ±20%
create or replace function economy.durability_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 120 when 'RARE' then 160 when 'EPIC' then 200
                       when 'LEGENDARY' then 280 when 'REDLINE' then 360 else 480 end,
         case p_rarity when 'COMMON' then 180 when 'RARE' then 240 when 'EPIC' then 300
                       when 'LEGENDARY' then 420 when 'REDLINE' then 540 else 720 end $$;
-- 분당 수리비는 계획 1단계 그대로(내구도 1 의 값 = 예전 범위 가운데로 센 값 × 0.65, 레벨마다 +2%)
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level integer) returns numeric language sql immutable as $$
  select round(12.5 * 0.65 * power(4::numeric, coalesce(economy.rarity_ord(p_rarity), 0) / 5.0)
               / (case p_rarity when 'COMMON' then 50 when 'RARE' then 75 when 'EPIC' then 125
                                when 'LEGENDARY' then 200 when 'REDLINE' then 300 else 450 end)
               * (1 + 0.02 * p_level), 8) $$;
create or replace function economy.points_at_level(p_level integer) returns integer language sql immutable as $$
  select case when p_level is null or p_level <= 1 then 0 else 1 end $$;
create or replace function economy.points_rarity_mult(p_rarity text) returns integer language sql immutable as $$
  select case p_rarity when 'COMMON' then 4 when 'RARE' then 5 when 'EPIC' then 6
                       when 'LEGENDARY' then 7 when 'REDLINE' then 8 when 'FINISH' then 10 else 0 end $$;
create or replace function economy.point_efficiency_bps() returns integer language sql immutable as $$ select 50 $$;
create or replace function economy.point_comfort_bps() returns integer language sql immutable as $$ select 80 $$;
create or replace function economy.comfort_cap_bps() returns integer language sql immutable as $$ select 4000 $$;
