-- 2026-11-18 능력치 설계안 2판(docs/shoe-stats-boxes-2026-11-18.md) — 설계 검증용(아직 서버에 없다).
--   사용자 "폭은 더 많이 흔들리게(최대 80% 차이) · 등급이 올라갈수록 팍팍 체감(레벨보단 등급) · 레벨업도 설계".
--   · 능력치는 지수로 본다: 효율 100 = 걸음당 기본 적립, 200 이면 2배. 같은 등급 안에서 100 → 180(가장 낮은 값의 1.8배).
--   · 등급 하나 = 효율 × 1.5 · 달리는 시간 × 1.35 · 수리비 × 1.5(버는 돈의 한결같은 몫).
--   · 레벨마다 포인트 2 + 5레벨마다 보너스 3, 포인트 1 = 고른 능력치 +1%(뽑은 값의) — Lv20 을 효율에 몰면 × 1.5 = 한 등급.
--   · 기본 걸음당 SUP 는 "가운데 일반 신발(효율 140) = 계획 1단계의 적립"이 되게 0.0118.
create or replace function economy.points_per_step() returns numeric language sql immutable as $$ select 0.0118::numeric $$;
-- 하루 상한은 등급을 따라가야 한다(설계안: 960 × 1.5^가진 신발 중 가장 높은 등급 — record_session 을 고쳐야 해서 여기서는
-- 상한을 풀어 두고 "에너지가 정하는 자연스러운 하루"를 본다. 설계안의 상한은 그 값보다 조금 위다)
update public.economy_settings set value = '100000'::jsonb where key = 'daily_earn_cap';
update public.economy_settings set value = '96'::jsonb where key = 'no_gps_daily_cap';
update public.economy_settings set value = '200000'::jsonb where key = 'upload_daily_cap';
create or replace function economy.energy_max(p_user uuid) returns numeric
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce((
    select max(e.level) from public.market_sneakers s,
           lateral economy.sneaker_effective(s.origin, s.rarity, s.level, s.efficiency_bps, s.comfort_bps, s.durability_pts) e
     where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'), 1))
$$;

alter table public.market_sneakers drop constraint market_sneakers_stats_check;
alter table public.market_sneakers add constraint market_sneakers_stats_check check (
  efficiency_bps >= 0 and efficiency_bps <= 200000 and comfort_bps >= 0 and comfort_bps <= 7000
  and durability_pts >= 0 and durability_pts <= 100 and km_run >= 0 and lock_km >= 0);

-- 효율 지수 100 ~ 180(일반), 등급마다 × 1.5 — 저장은 "적립 +bps"((지수 − 100) × 100)
create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 5000 when 'EPIC' then 12500
                       when 'LEGENDARY' then 23800 when 'REDLINE' then 40600 else 65900 end,
         case p_rarity when 'COMMON' then 8000 when 'RARE' then 17000 when 'EPIC' then 30500
                       when 'LEGENDARY' then 50800 when 'REDLINE' then 81100 else 126700 end $$;
-- 착화감 지수 100 ~ 180(일반), 등급마다 가운데 +10 — 저장은 "에너지 절약 bps"(1 − 100 / 지수)
create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 654 when 'EPIC' then 1228
                       when 'LEGENDARY' then 1736 when 'REDLINE' then 2248 else 2647 end,
         case p_rarity when 'COMMON' then 4444 when 'RARE' then 4819 when 'EPIC' then 5146
                       when 'LEGENDARY' then 5434 when 'REDLINE' then 5671 else 5902 end $$;
create or replace function economy.comfort_cap_bps() returns integer language sql immutable as $$ select 7000 $$;
-- 한 켤레로 약 22 ~ 40분(일반), 등급마다 × 1.35 (1분마다 5 그대로)
create or replace function economy.durability_range(p_rarity text, out lo int, out hi int) language sql immutable as $$
  select case p_rarity when 'COMMON' then 110 when 'RARE' then 150 when 'EPIC' then 200
                       when 'LEGENDARY' then 270 when 'REDLINE' then 360 else 480 end,
         case p_rarity when 'COMMON' then 200 when 'RARE' then 270 when 'EPIC' then 360
                       when 'LEGENDARY' then 485 when 'REDLINE' then 650 else 865 end $$;
-- 수리: 내구도 1 의 값 = 0.1625(계획 1단계의 일반) × 1.5^등급 × (1 + 2% × 레벨) — 가운데 신발로 걸을 때 버는 돈의 45%쯤
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level integer) returns numeric language sql immutable as $$
  select round(0.1625 * power(1.5::numeric, coalesce(economy.rarity_ord(p_rarity), 0)) * (1 + 0.02 * p_level), 8) $$;
-- 레벨마다 포인트 2, 5레벨마다 +3 (등급 배수 없음 — 포인트 1 이 "뽑은 값의 1%"라 등급이 높으면 저절로 크다)
create or replace function economy.points_at_level(p_level integer) returns integer language sql immutable as $$
  select case when p_level is null or p_level <= 1 then 0 else 2 + case when p_level % 5 = 0 then 3 else 0 end end $$;
create or replace function economy.points_rarity_mult(p_rarity text) returns integer language sql immutable as $$
  select case when economy.rarity_ord(p_rarity) is null then 0 else 1 end $$;
create or replace function economy.shoe_effective(s public.market_sneakers, out efficiency_bps integer, out comfort_bps integer,
    out level integer, out durability numeric, out points_total integer, out points_free integer)
returns record language sql immutable as $$
  select
    case when s.origin in ('IMPORT', 'MINT') then 200
         else (round((10000 + s.efficiency_bps) * (1 + s.pts_eff / 100.0)) - 10000)::int end,
    case when s.origin in ('IMPORT', 'MINT') then 0
         else least((10000 - round((10000 - s.comfort_bps) / (1 + s.pts_comfort / 100.0)))::int, economy.comfort_cap_bps()) end,
    case when s.origin in ('IMPORT', 'MINT') then 1 else s.level end,
    s.durability_pts,
    economy.points_total_of(s.origin, s.rarity, s.level),
    economy.points_total_of(s.origin, s.rarity, s.level) - s.pts_eff - s.pts_comfort - s.pts_dur
$$;
-- 첫 신발은 가운데 값(효율 140 · 착화감 140)
create or replace function pg_temp_starter_mid() returns trigger language plpgsql as $$
begin
  if new.origin = 'STARTER' then new.efficiency_bps := 4000; new.comfort_bps := 2857; end if;
  return new;
end $$;
create trigger sim_starter_mid before insert on public.market_sneakers for each row execute function pg_temp_starter_mid();
