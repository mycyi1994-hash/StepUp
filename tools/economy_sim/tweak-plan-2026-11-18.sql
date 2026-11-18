-- 2026-11-18 계획 1단계 묶음(docs/점검-2026-11-18-경제-흐름.md "해결 계획"):
--   적립 +60% · 수리비 −35% · 레벨마다 수리비 +2% · SUP 상한 × 1.6 · 에너지 최대치는 가진 신발 중 가장 높은 레벨
create or replace function economy.points_per_step() returns numeric language sql immutable as $$ select 0.016::numeric $$;
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level integer) returns numeric language sql immutable as $$
  select round(12.5 * 0.65 * power(4::numeric, coalesce(economy.rarity_ord(p_rarity), 0) / 5.0)
               / ((r.lo + r.hi) / 2.0) * (1 + 0.02 * p_level), 8)
    from economy.durability_range(p_rarity) r $$;
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
