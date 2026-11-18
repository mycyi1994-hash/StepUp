-- 2026-11-18 사용자 안(적립 +60% · 수리비 −35%) + 레벨마다 수리비 +5% → +2% (강화할수록 남는 돈이 줄지 않게)
create or replace function economy.points_per_step() returns numeric language sql immutable as $$ select 0.016::numeric $$;
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level integer) returns numeric language sql immutable as $$
  select round(12.5 * 0.65 * power(4::numeric, coalesce(economy.rarity_ord(p_rarity), 0) / 5.0)
               / ((r.lo + r.hi) / 2.0) * (1 + 0.02 * p_level), 8)
    from economy.durability_range(p_rarity) r $$;
