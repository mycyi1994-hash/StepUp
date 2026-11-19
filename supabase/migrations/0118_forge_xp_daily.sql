-- 0118 — 강화 경험치 · 오늘의 보상(보상 걸음 · 그 날의 목표) (2026-11-19)
--
-- 사용자 "fable 5.2가 써둔 문제 수정 기획서를 전부 반영해서 진행해" — docs/forge-xp-daily-spec-2026-11-19.md 4장 ·
-- 앱과의 약속은 docs/forge-xp-daily-contract-2026-11-19.md.
--   강화: 확률 · 3켤레 대신 경험치(실패 없음 · 남는 점수는 다음 통으로 · 1 ~ 10켤레 · 대성공 10% × 2 · 강화한 재료는 50% 물려줌 ·
--         닳은 재료는 가득 수리비만큼 덜 · 등급 제한 없음). 예전 앱의 강화(forge_quote · forge_start)는 규칙 그대로, 재료만 내구도 가득.
--   걸음: 상자 · 목표 · 연속 · 스텝 서지는 "보상 걸음"(경로로 확인 · 신발 내구도가 남은 동안 · 크루는 공용 체력 몫) 하나로 센다.
--         바꾼 목표는 다음 날부터(오늘 보상 걸음이 0 이면 오늘부터) · 4,000 ~ 20,000 · 목표 보너스는 러닝을 기록할 때 서버가 바로.
--         같은 경로 검사를 모든 계정으로.
--
-- 함수 모양(인자 · 돌려주는 값)은 하나도 바꾸지 않았다 — 예전 스토어 판(1.17.1)이 부르는 그대로다.
-- record_session 은 0118 이 마지막 정의다 — 다시 고칠 때 0118 을 복사한다(0117 본문 + 보상 걸음 · 같은 경로 · 목표 정산).

-- ════════════════════════════════════════════════════════════════════
--  1. 설정 · 칸 · 표
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('forge_xp_step', '3.1'), ('forge_xp_need', '2.88'), ('forge_xp_spec', '[0.7, 1.3]'),
  ('forge_xp_crit_chance', '0.10'), ('forge_xp_crit_mult', '2'), ('forge_xp_inherit', '0.5'),
  ('forge_xp_sup_per_unit', '24'), ('forge_xp_max_materials', '10'),
  ('goal_min', '4000'), ('goal_max', '20000'), ('goal_default', '6000'),
  -- 되돌리기 스위치: 'OLD' 로 바꾸면 상자는 적립 걸음 · 목표는 경로 걸음으로(0117 까지의 셈). SQL 창에서만 바꾼다
  ('reward_steps_rule', '"REWARD"')
on conflict (key) do nothing;

-- 강화 경험치 통 — 그 레벨에서 다음 레벨까지 모은 점수(일반 1켤레값 = 1,000점)
alter table public.market_sneakers add column if not exists forge_xp int not null default 0;
alter table public.market_sneakers drop constraint if exists market_sneakers_forge_xp_check;
alter table public.market_sneakers add constraint market_sneakers_forge_xp_check check (forge_xp >= 0);
comment on column public.market_sneakers.forge_xp is '강화 경험치 통에 든 점수 — 일반 1켤레값 = 1,000점, 만렙이면 0 (0118)';
-- 0066 의 초기화 백업 표는 신발 표를 칸 차례 그대로 옮긴다(insert … select *) — 새 칸을 같은 자리에 더해 둔다
alter table if exists economy.backup_0066_market_sneakers add column if not exists forge_xp int;

-- 강화 기록 — 경험치 넣기(XP)도 같은 표에. 레벨이 안 올랐으면 ADDED
alter table public.forge_requests add column if not exists kind text not null default 'CHANCE';
alter table public.forge_requests add column if not exists xp_gain int;
alter table public.forge_requests add column if not exists xp_after int;
alter table public.forge_requests add column if not exists levels int;
alter table public.forge_requests add column if not exists detail jsonb;
alter table public.forge_requests drop constraint if exists forge_requests_status_check;
alter table public.forge_requests add constraint forge_requests_status_check
  check (status in ('SUCCESS', 'FAILED', 'VOID', 'ADDED'));
alter table public.forge_requests drop constraint if exists forge_requests_kind_check;
alter table public.forge_requests add constraint forge_requests_kind_check check (kind in ('CHANCE', 'XP'));

-- 보상 걸음 — 상자 · 목표 · 연속 · 스텝 서지가 세는 걸음 하나 (0118)
alter table public.walk_sessions add column if not exists reward_steps int;
comment on column public.walk_sessions.reward_steps is
  '보상 걸음 — 경로가 받쳐 주고 · 무효 · 보류가 아니고 · 신발 내구도가 남아 있는 동안의 확인된 걸음(크루 러닝은 공용 체력이 받쳐 준 몫) (0118)';
-- 다른 계정이 올린 같은 경로를 찾는다(같은 경로 검사를 모든 계정으로)
create index if not exists walk_sessions_track_md5 on public.walk_sessions (md5(track)) where track <> '';

-- 그 날의 목표 — 바꾼 목표는 다음 날부터(받기 직전에 목표를 낮추지 못하게)
create table if not exists economy.goal_days (
  user_id uuid not null references auth.users (id) on delete cascade,
  day date not null,
  goal int not null check (goal between 1000 and 100000),
  primary key (user_id, day)
);
alter table economy.goal_days enable row level security;
alter table public.profiles alter column daily_goal set default 6000;

-- 한 번만: 예전 앱의 강화는 내구도 가득인 재료만 · 지난 러닝의 보상 걸음 · 지난 8일의 목표 · 목표 4,000 부터
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'forge_xp_20261119') then
    update public.economy_settings set value = '100', updated_at = now() where key = 'forge_material_min_durability';
    update public.walk_sessions set reward_steps = backed_steps where reward_steps is null;
    insert into economy.goal_days (user_id, day, goal)
    select p.id, d::date, least(greatest(coalesce(p.daily_goal, 8000), 1000), 100000)
      from public.profiles p
      cross join generate_series(economy.game_day(now()) - 7, economy.game_day(now()), interval '1 day') d
    on conflict do nothing;
    update public.profiles set daily_goal = 4000 where daily_goal < 4000;
    insert into public.economy_settings (key, value) values ('forge_xp_20261119', to_jsonb(now()));
  end if;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  2. 강화 경험치 — 규칙
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.fx(p_key text, p_default numeric)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select coalesce(economy.setting_num(p_key), p_default)
$$;

-- 통의 크기 — 그 레벨에서 다음 레벨까지. 굽이는 예전 확률 방식의 평균 비용(3 ÷ 성공 확률)과 같다. 만렙이면 null
create or replace function economy.forge_need(p_rarity text, p_level int)
returns int language sql stable security definer set search_path = public, economy as $$
  select case when p_level is null or economy.rarity_ord(p_rarity) is null or p_level >= economy.max_level(p_rarity) then null
              else round(1000 * economy.fx('forge_xp_need', 2.88)
                         * power(economy.fx('forge_xp_step', 3.1), greatest(economy.rarity_ord(p_rarity) - 1, 0))
                         * 1.03 / (1.06 - 0.03 * greatest(p_level, 1)))::int end
$$;

-- 그 레벨에 닿기까지 든 통의 합(Lv1 = 0)
create or replace function economy.forge_cum_need(p_rarity text, p_level int)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(economy.forge_need(p_rarity, l)), 0)::bigint
    from generate_series(1, greatest(coalesce(p_level, 1), 1) - 1) l
$$;

-- 그 신발에 든 경험치 — 그 레벨까지 통의 합 + 지금 통(예전 확률 강화로 오른 레벨도 표로 센다)
create or replace function economy.forge_invested(s public.market_sneakers)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select economy.forge_cum_need(s.rarity, s.level) + coalesce(s.forge_xp, 0)
$$;

-- 스펙 — 세 능력치(뽑은 값)가 그 등급 범위의 어디쯤인가, 0 ~ 1. 앱 ShoeQuality.of 와 같은 셈 · 같은 반올림(★ 0.5 아래 · ★★ 0.65 아래 · ★★★)
create or replace function economy.shoe_spec_q(s public.market_sneakers)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select round(((pe + pc + pd) / 3)::numeric, 3) from (
    select least(greatest((s.efficiency_bps - er.lo)::numeric / greatest(er.hi - er.lo, 1), 0), 1) as pe,
           least(greatest(((10000.0 - cr.lo) / greatest(10000.0 - least(coalesce(s.comfort_bps, 0), 9999), 1) - 1) / 0.8, 0), 1) as pc,
           least(greatest((coalesce(s.dur_base, dr.lo) - dr.lo)::numeric / greatest(dr.hi - dr.lo, 1), 0), 1) as pd
      from economy.efficiency_range(s.rarity) er, economy.comfort_range(s.rarity) cr, economy.durability_range(s.rarity) dr) x
$$;

create or replace function economy.shoe_stars(p_q numeric)
returns int language sql immutable as $$
  select case when p_q is null then null when p_q < 0.5 then 1 when p_q < 0.65 then 2 else 3 end
$$;

-- 재료 한 켤레의 값 — 제 값 = 등급 값 × 스펙 − 닳은 몫(가득 수리비 ÷ 켤레값의 SUP), 물려줌 = 그 신발에 든 경험치(표로 셈) × 50%.
-- 스펙 · 닳음 · 대성공은 제 값에만(물려주는 몫은 건드리지 않는다)
create or replace function economy.forge_xp_value(m public.market_sneakers,
    out own int, out inherit int, out repair_part int, out q numeric)
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_spec jsonb := coalesce(economy.setting('forge_xp_spec'), '[0.7, 1.3]'::jsonb);
  v_lo numeric := coalesce((v_spec ->> 0)::numeric, 0.7);
  v_hi numeric := coalesce((v_spec ->> 1)::numeric, 1.3);
begin
  q := economy.shoe_spec_q(m);
  repair_part := round(greatest(coalesce(m.dur_max, 0) - coalesce(m.dur_now, 0), 0) * economy.repair_unit_of(m)
                       / greatest(economy.fx('forge_xp_sup_per_unit', 24), 0.0001) * 1000)::int;
  own := greatest(round(1000 * power(economy.fx('forge_xp_step', 3.1), coalesce(economy.rarity_ord(m.rarity), 0))
                        * (v_lo + (v_hi - v_lo) * coalesce(q, 0.5)))::int - repair_part, 0);
  inherit := round(economy.fx('forge_xp_inherit', 0.5) * economy.forge_invested(m))::int;
end $$;

-- 재료가 될 수 없는 까닭(대상과 상관없이 같은 값 — 등급 제한은 없다)
create or replace function economy.forge_xp_block(p_user uuid, m public.market_sneakers, t public.market_sneakers)
returns text language plpgsql stable security definer set search_path = public, economy as $$
begin
  if m.id is null or m.owner_id is distinct from p_user then return 'GONE'; end if;
  if m.id = t.id then return 'TARGET'; end if;
  if m.status <> 'OWNED' then return 'LISTED'; end if;
  if m.equipped then return 'EQUIPPED'; end if;
  if m.origin in ('STARTER', 'IMPORT', 'MINT') then return 'LOCKED'; end if;
  if m.chain_state <> 'APP' or m.token_id is not null or economy.forge_chain_busy(m.id) then return 'ON_CHAIN'; end if;
  if m.level is null or m.level < 1 then return 'LEVEL'; end if;
  if exists (select 1 from public.party_members pm join public.parties p on p.id = pm.party_id
              where pm.user_id = p_user and pm.sneaker_id = m.id and p.status in ('LOBBY', 'COUNTDOWN', 'RUNNING'))
     or exists (select 1 from economy.crew_runs c
                 where c.user_id = p_user and c.sneaker_id = m.id and c.state <> 'ENDED' and c.started_at > now() - interval '6 hours') then
    return 'IN_ROOM';
  end if;
  return null;
end $$;

-- 대상이 될 수 없는 까닭 — 예전 강화와 같고, 크루 러닝 중이면 막는다(레벨 · 에너지가 그 러닝에 섞이지 않게)
create or replace function economy.forge_xp_target_block(p_user uuid, t public.market_sneakers)
returns text language plpgsql stable security definer set search_path = public, economy as $$
declare v text := economy.forge_target_block(p_user, t);
begin
  if v is not null then return v; end if;
  if exists (select 1 from economy.crew_runs c
              where c.user_id = p_user and c.state <> 'ENDED' and c.started_at > now() - interval '6 hours') then
    return 'RUNNING';
  end if;
  return null;
end $$;

create or replace function economy.forge_pct(p_xp bigint, p_need int)
returns numeric language sql immutable as $$
  select case when p_need is null or p_need <= 0 then null else round(floor(p_xp * 1000.0 / p_need) / 10, 1) end
$$;

-- 통에 점수를 더하면 어디까지 오르나 — 레벨 · 남은 점수 · 오른 레벨 수 · 만렙에서 넘친 점수
create or replace function economy.forge_xp_apply(p_rarity text, p_level int, p_xp bigint, p_gain bigint,
    out level int, out xp bigint, out levels int, out overflow bigint)
language plpgsql stable security definer set search_path = public, economy as $$
declare v_need int; v_max int := economy.max_level(p_rarity);
begin
  level := p_level; xp := coalesce(p_xp, 0) + greatest(coalesce(p_gain, 0), 0); levels := 0; overflow := 0;
  loop
    v_need := economy.forge_need(p_rarity, level);
    exit when v_need is null or xp < v_need;
    xp := xp - v_need; level := level + 1; levels := levels + 1;
  end loop;
  if level >= v_max then
    overflow := xp; xp := 0;
  end if;
end $$;

-- 레벨 하나의 값어치는 계정 전체다 — 이 재료들을 태우고 대상이 [p_level] 이 되면 하루 에너지 · 하루 상한이 얼마인가
create or replace function economy.forge_account_after(p_user uuid, p_target bigint, p_level int, p_burn bigint[],
    out energy numeric, out cap numeric)
language sql stable security definer set search_path = public, economy as $$
  select economy.energy_max_for_level(coalesce(max(case when s.id = p_target then p_level
                                                         when s.origin in ('IMPORT', 'MINT') then 1 else s.level end), 1)),
         case when not coalesce((economy.setting('earn_cap_by_shoe') #>> '{}')::boolean, true) then 1
              else greatest(1, coalesce(max(10000 + e.efficiency_bps) / 14000.0, 1)) end
    from public.market_sneakers s
    left join lateral economy.shoe_effective(s) e on true
   where s.owner_id = p_user and s.chain_state = 'APP' and s.status = 'OWNED'
     and not (s.id = any(coalesce(p_burn, '{}'::bigint[])))
$$;

create or replace function economy.forge_xp_target_json(t public.market_sneakers)
returns jsonb language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object('id', t.id, 'rarity', t.rarity, 'level', t.level, 'max_level', economy.max_level(t.rarity),
                            'xp', t.forge_xp, 'need', economy.forge_need(t.rarity, t.level),
                            'pct', economy.forge_pct(t.forge_xp, economy.forge_need(t.rarity, t.level)))
$$;

-- 견적 — 대성공 없이 받는 최소. 서명에는 대상 · 재료(레벨 · 통 · 내구도 · 스펙) · 손잡이 값이 든다
create or replace function economy.forge_xp_quote_for(p_user uuid, p_target bigint, p_materials bigint[])
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  t public.market_sneakers;
  m public.market_sneakers;
  v_block text;
  v_bad jsonb := '[]'::jsonb;
  v_zero bigint[] := '{}';
  v_items jsonb := '[]'::jsonb;
  v_sig text;
  v_id bigint;
  v_val record;
  v_gain bigint := 0;
  v_gain_max bigint := 0;
  v_need int;
  v_max int;
  a record;
  amax record;
  v_before record;
  v_after record;
  v_ids bigint[];
begin
  if p_materials is null or cardinality(p_materials) < 1
     or cardinality(p_materials) > economy.fx('forge_xp_max_materials', 10)
     or array_position(p_materials, null) is not null
     or (select count(distinct x) from unnest(p_materials) x) <> cardinality(p_materials) then
    return jsonb_build_object('error', 'MATERIALS_INVALID');
  end if;
  select * into t from public.market_sneakers where id = p_target;
  v_block := economy.forge_xp_target_block(p_user, t);
  if v_block is not null then
    return jsonb_build_object('error', v_block);
  end if;
  v_need := economy.forge_need(t.rarity, t.level);
  v_max := economy.max_level(t.rarity);
  v_ids := (select array_agg(x order by x) from unnest(p_materials) x);
  v_sig := 'forge-xp-1|' || t.id || ':' || t.level || ':' || t.forge_xp;
  foreach v_id in array v_ids loop
    select * into m from public.market_sneakers where id = v_id;
    if not found then m := null; end if;
    v_block := economy.forge_xp_block(p_user, m, t);
    if v_block is not null then
      v_bad := v_bad || jsonb_build_object('id', v_id, 'block', v_block);
      continue;
    end if;
    select * into v_val from economy.forge_xp_value(m);
    if v_val.own + v_val.inherit <= 0 then
      v_zero := v_zero || v_id;
      continue;
    end if;
    v_gain := v_gain + v_val.own + v_val.inherit;
    v_gain_max := v_gain_max + round(v_val.own * economy.fx('forge_xp_crit_mult', 2)) + v_val.inherit;
    v_items := v_items || jsonb_build_object('id', m.id, 'own', v_val.own, 'inherit', v_val.inherit,
      'value', v_val.own + v_val.inherit, 'pct', economy.forge_pct(v_val.own + v_val.inherit, v_need));
    v_sig := v_sig || '|' || m.id || ':' || m.level || ':' || m.forge_xp || ':' || m.dur_now || ':' || m.dur_max || ':'
             || m.efficiency_bps || ':' || m.comfort_bps || ':' || coalesce(m.dur_base, 0) || ':' || m.pts_eff;
  end loop;
  if jsonb_array_length(v_bad) > 0 then
    return jsonb_build_object('error', 'MATERIAL_UNAVAILABLE', 'unavailable', v_bad);
  end if;
  if cardinality(v_zero) > 0 then
    return jsonb_build_object('error', 'NO_VALUE', 'ids', to_jsonb(v_zero));
  end if;
  v_sig := v_sig || '|' || concat_ws(':', economy.fx('forge_xp_step', 3.1), economy.fx('forge_xp_need', 2.88),
             economy.setting('forge_xp_spec')::text, economy.fx('forge_xp_crit_chance', 0.1), economy.fx('forge_xp_crit_mult', 2),
             economy.fx('forge_xp_inherit', 0.5), economy.fx('forge_xp_sup_per_unit', 24),
             economy.fx('repair_share', 0.45), economy.points_per_step());
  select * into a from economy.forge_xp_apply(t.rarity, t.level, t.forge_xp, v_gain);
  select * into amax from economy.forge_xp_apply(t.rarity, t.level, t.forge_xp, v_gain_max);
  select * into v_before from economy.forge_account_after(p_user, t.id, t.level, '{}');
  select * into v_after from economy.forge_account_after(p_user, t.id, a.level, v_ids);
  return jsonb_build_object(
    'ok', true, 'rule_version', 'forge-xp-1', 'quote_version', md5(v_sig),
    'before', jsonb_build_object('level', t.level, 'xp', t.forge_xp, 'need', v_need, 'pct', economy.forge_pct(t.forge_xp, v_need)),
    'after', jsonb_build_object('level', a.level, 'xp', a.xp, 'need', economy.forge_need(t.rarity, a.level),
                                'pct', economy.forge_pct(a.xp, economy.forge_need(t.rarity, a.level)),
                                'levels', a.levels, 'overflow', a.overflow),
    'gain', v_gain, 'materials', v_items,
    'crit', jsonb_build_object('chance', economy.fx('forge_xp_crit_chance', 0.1), 'mult', economy.fx('forge_xp_crit_mult', 2)),
    'energy', jsonb_build_object('before', v_before.energy, 'after', v_after.energy),
    'cap', jsonb_build_object('before', round(v_before.cap, 2), 'after', round(v_after.cap, 2)),
    'max_level', v_max, 'overflow_possible', amax.overflow > 0);
end $$;

-- 이 재료를 빼면 아까운 까닭 — 서버가 아는 것만(이름 붙인 신발은 앱이 더한다)
create or replace function economy.forge_xp_precious(p_user uuid, m public.market_sneakers, t public.market_sneakers, p_q numeric)
returns jsonb language sql stable security definer set search_path = public, economy as $$
  select coalesce(jsonb_agg(r) filter (where r is not null), '[]'::jsonb) from (
    select case when economy.rarity_ord(m.rarity) > economy.rarity_ord(t.rarity) then 'HIGHER_GRADE' end as r
    union all select case when m.level >= 2 then 'LEVELED' end
    union all select case when p_q >= 0.65 then 'STARS3' end
    union all select case when m.model_id is not null and not exists (
        select 1 from public.market_sneakers o where o.owner_id = p_user and o.id <> m.id and o.status <> 'BURNED'
           and o.model_id = m.model_id) then 'LAST_MODEL' end
    union all select case when coalesce(m.lock_km, 0) > 0 and coalesce(m.km_run, 0) >= m.lock_km then 'SELL_UNLOCKED' end
    union all select case when (economy.forge_account_after(p_user, t.id, t.level, array[m.id])).energy
                               < economy.energy_max(p_user) then 'TOP_LEVEL' end
    union all select case when (economy.forge_account_after(p_user, t.id, t.level, array[m.id])).cap
                               < economy.earn_cap_factor(p_user) then 'TOP_EFFICIENCY' end) x
$$;

-- ════════════════════════════════════════════════════════════════════
--  3. 강화 경험치 — 앱이 부르는 함수
-- ════════════════════════════════════════════════════════════════════
create or replace function public.forge_xp_materials(p_target bigint)
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  t public.market_sneakers;
  v_block text;
  v_need int;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into t from public.market_sneakers where id = p_target;
  v_block := economy.forge_xp_target_block(v_user, t);
  if v_block is not null then
    return jsonb_build_object('error', v_block);
  end if;
  v_need := economy.forge_need(t.rarity, t.level);
  return jsonb_build_object(
    'ok', true, 'rule_version', 'forge-xp-1',
    'target', economy.forge_xp_target_json(t),
    'items', coalesce((
      select jsonb_agg(jsonb_build_object(
          'id', x.id, 'rarity', x.rarity, 'level', x.level, 'model_id', x.model_id, 'variant', x.variant,
          'q', x.q, 'stars', economy.shoe_stars(x.q), 'durability_pct', x.durability_pts,
          'own', x.own, 'inherit', x.inherit, 'repair_part', x.repair_part, 'value', x.own + x.inherit,
          'pct', economy.forge_pct(x.own + x.inherit, v_need),
          'block', coalesce(x.block, case when x.own + x.inherit <= 0 then 'NO_VALUE' end),
          'precious', x.precious)
        order by (coalesce(x.block, case when x.own + x.inherit <= 0 then 'NO_VALUE' end) is not null), x.own + x.inherit, x.id)
        from (select m.id, m.rarity, m.level, m.model_id, m.variant, m.durability_pts, v.own, v.inherit, v.repair_part, v.q,
                     economy.forge_xp_block(v_user, m, t) as block,
                     economy.forge_xp_precious(v_user, m, t, v.q) as precious
                from public.market_sneakers m
                cross join lateral economy.forge_xp_value(m) v
               where m.owner_id = v_user and m.id <> t.id and m.status <> 'BURNED') x), '[]'::jsonb),
    'crit', jsonb_build_object('chance', economy.fx('forge_xp_crit_chance', 0.1), 'mult', economy.fx('forge_xp_crit_mult', 2)),
    'max_materials', economy.fx('forge_xp_max_materials', 10)::int);
end $$;

create or replace function public.forge_xp_quote(p_target bigint, p_materials bigint[])
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return economy.forge_xp_quote_for(auth.uid(), p_target, p_materials);
end $$;

create or replace function economy.forge_xp_result_json(r public.forge_requests)
returns jsonb language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object(
    'ok', true, 'kind', 'XP', 'request_key', r.request_key, 'target_id', r.target_id,
    'level_before', r.level_before, 'level_after', r.level_after, 'levels', coalesce(r.levels, 0),
    'gain', r.xp_gain, 'xp_after', r.xp_after,
    'need_after', economy.forge_need(s.rarity, r.level_after),
    'pct_after', economy.forge_pct(r.xp_after, economy.forge_need(s.rarity, r.level_after)),
    'overflow', coalesce((r.detail ->> 'overflow')::bigint, 0),
    'materials', coalesce(r.detail -> 'materials', '[]'::jsonb),
    'crits', coalesce(r.roll, 0),
    'level_boxes', coalesce((select jsonb_agg(substr(a.source, 7)::int order by substr(a.source, 7)::int)
                               from economy.box_awards a
                              where a.user_id = r.user_id and a.source like 'LEVEL:%' and a.granted_at = r.created_at
                                and substr(a.source, 7)::int > r.level_before and substr(a.source, 7)::int <= r.level_after), '[]'::jsonb))
    from (select 1) one
    left join public.market_sneakers s on s.id = r.target_id
$$;

create or replace function public.forge_xp_result(p_key uuid)
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare r public.forge_requests;
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into r from public.forge_requests where user_id = auth.uid() and request_key = p_key and kind = 'XP';
  if not found then
    return null;
  end if;
  return economy.forge_xp_result_json(r);
end $$;

-- 경험치 넣기 — 실패가 없다. 재료마다 대성공을 굴리고, 통이 차면 레벨이 오른다(포인트 · 5레벨 내구도 가득 · 레벨 상자는 트리거가 그대로)
create or replace function public.forge_xp_add(p_key uuid, p_target bigint, p_materials bigint[], p_quote text)
returns jsonb language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  r public.forge_requests;
  t public.market_sneakers;
  m public.market_sneakers;
  v_quote jsonb;
  v_ids bigint[];
  v_bytes bytea := economy.random_bytes32();
  v_chance int := round(economy.fx('forge_xp_crit_chance', 0.1) * 1000)::int;
  v_mult numeric := economy.fx('forge_xp_crit_mult', 2);
  v_val record;
  v_crit boolean;
  v_crits int := 0;
  v_gain bigint := 0;
  v_items jsonb := '[]'::jsonb;
  v_i int := 0;
  v_id bigint;
  a record;
  v_part bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_key is null then
    return jsonb_build_object('error', 'KEY_REQUIRED');
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  v_ids := (select array_agg(x order by x) from unnest(p_materials) x);
  select * into r from public.forge_requests where user_id = v_user and request_key = p_key;
  if found then
    if r.kind <> 'XP' or r.target_id is distinct from p_target or r.material_ids is distinct from v_ids then
      return jsonb_build_object('error', 'KEY_REUSED');
    end if;
    return economy.forge_xp_result_json(r);
  end if;

  perform 1 from public.market_sneakers where id = p_target or id = any(p_materials) for update;
  v_quote := economy.forge_xp_quote_for(v_user, p_target, p_materials);
  if v_quote ? 'error' then
    return v_quote;
  end if;
  if p_quote is distinct from v_quote ->> 'quote_version' then
    return jsonb_build_object('error', 'QUOTE_CHANGED', 'quote', v_quote);
  end if;

  select * into t from public.market_sneakers where id = p_target;
  foreach v_id in array v_ids loop
    select * into m from public.market_sneakers where id = v_id;
    select * into v_val from economy.forge_xp_value(m);
    v_crit := (economy.bytes_int(v_bytes, 2 * v_i, 2) % 1000) < v_chance;
    v_part := case when v_crit then round(v_val.own * v_mult) else v_val.own end + v_val.inherit;
    v_gain := v_gain + v_part;
    v_crits := v_crits + case when v_crit then 1 else 0 end;
    v_items := v_items || jsonb_build_object('id', m.id, 'own', v_val.own, 'inherit', v_val.inherit, 'crit', v_crit, 'value', v_part);
    v_i := v_i + 1;
  end loop;

  -- 재료를 태운다
  update public.market_sneakers
     set owner_id = null, status = 'BURNED', equipped = false, updated_at = now()
   where id = any(v_ids);
  select * into a from economy.forge_xp_apply(t.rarity, t.level, t.forge_xp, v_gain);
  update public.market_sneakers set level = a.level, forge_xp = a.xp, updated_at = now() where id = t.id;

  insert into public.forge_requests (user_id, request_key, status, target_id, material_ids, rate_permille, roll,
                                     level_before, level_after, rule_version, quote_version,
                                     kind, xp_gain, xp_after, levels, detail)
  values (v_user, p_key, case when a.levels > 0 then 'SUCCESS' else 'ADDED' end, t.id, v_ids, 1000, least(v_crits, 999),
          t.level, a.level, 'forge-xp-1', v_quote ->> 'quote_version',
          'XP', v_gain, a.xp, a.levels, jsonb_build_object('materials', v_items, 'overflow', a.overflow))
  returning * into r;
  return economy.forge_xp_result_json(r);
end $$;

create or replace function public.my_shoe_xp()
returns jsonb language sql stable security definer set search_path = public, economy as $$
  select coalesce(jsonb_agg(jsonb_build_object('id', s.id, 'xp', s.forge_xp, 'need', economy.forge_need(s.rarity, s.level),
                                               'pct', economy.forge_pct(s.forge_xp, economy.forge_need(s.rarity, s.level)))
                            order by s.id), '[]'::jsonb)
    from public.market_sneakers s
   where s.owner_id = auth.uid() and s.status <> 'BURNED'
$$;

-- 만렙에 닿으면 통을 비운다(예전 강화로 올라도)
create or replace function economy.forge_xp_at_max()
returns trigger language plpgsql as $$
begin
  if new.level is not null and new.rarity is not null and new.level >= economy.max_level(new.rarity) then
    new.forge_xp := 0;
  end if;
  return new;
end $$;
drop trigger if exists market_sneakers_forge_xp_max on public.market_sneakers;
create trigger market_sneakers_forge_xp_max before update of level on public.market_sneakers
  for each row execute function economy.forge_xp_at_max();

-- ════════════════════════════════════════════════════════════════════
--  4. 오늘의 보상 — 보상 걸음 · 그 날의 목표 · 받기
-- ════════════════════════════════════════════════════════════════════
-- 그 날의 보상 걸음(하루 48,000 까지). 예전 이름 그대로 — 목표 보너스 · 이벤트(오늘의 목표 · 스텝 서지)가 이 숫자를 센다
create or replace function economy.reward_rule_old()
returns boolean language sql stable security definer set search_path = public, economy as $$
  select coalesce(economy.setting('reward_steps_rule') #>> '{}', 'REWARD') = 'OLD'
$$;

create or replace function economy.verified_steps_on(p_user uuid, p_day date)
returns integer language sql stable security definer set search_path = public, economy as $$
  select least(coalesce(sum(case when economy.reward_rule_old() then s.backed_steps
                                 else coalesce(s.reward_steps, s.backed_steps) end), 0), economy.max_daily_steps())::int
    from public.walk_sessions s
   where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED')
     and economy.game_day(s.started_at) = p_day
$$;

create or replace function economy.reward_steps_on(p_user uuid, p_day date)
returns integer language sql stable security definer set search_path = public, economy as $$
  select economy.verified_steps_on(p_user, p_day)
$$;

create or replace function economy.reward_steps_week(p_user uuid, p_day date)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select coalesce(sum(economy.verified_steps_on(p_user, d::date)), 0)::bigint
    from generate_series(economy.game_week(p_day), p_day, interval '1 day') d
$$;

-- 상자가 세는 걸음 — 보상 걸음(되돌리기 스위치가 'OLD' 면 예전처럼 적립 걸음, 하루 상한 없이)
create or replace function economy.box_steps(p_user uuid, p_from date, p_to date)
returns bigint language sql stable security definer set search_path = public, economy as $$
  select case when economy.reward_rule_old() then
           (select coalesce(sum(s.rewarded_steps), 0) from public.walk_sessions s
             where s.user_id = p_user and s.verdict in ('CLEAN', 'FLAGGED') and economy.game_day(s.started_at) between p_from and p_to)
         else (select coalesce(sum(economy.verified_steps_on(p_user, d::date)), 0)
                 from generate_series(p_from, p_to, interval '1 day') d) end::bigint
$$;

create or replace function economy.goal_clamp(p_goal int)
returns int language sql stable security definer set search_path = public, economy as $$
  select least(greatest((round(coalesce(p_goal, economy.fx('goal_default', 6000)::int) / 500.0) * 500)::int,
                        economy.fx('goal_min', 4000)::int), economy.fx('goal_max', 20000)::int)
$$;

-- 그 날의 목표(읽기만) — 적어 둔 값, 없으면 지금 목표
create or replace function economy.goal_peek(p_user uuid, p_day date)
returns int language sql stable security definer set search_path = public, economy as $$
  select coalesce((select g.goal from economy.goal_days g where g.user_id = p_user and g.day = p_day),
                  (select economy.goal_clamp(p.daily_goal) from public.profiles p where p.id = p_user),
                  economy.fx('goal_default', 6000)::int)
$$;

-- 그 날의 목표 — 없으면 지금 목표로 적어 두고 그 값을 쓴다(그 뒤로 바꾼 목표는 다음 날부터)
create or replace function economy.goal_of(p_user uuid, p_day date)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v int;
begin
  select g.goal into v from economy.goal_days g where g.user_id = p_user and g.day = p_day;
  if found then return v; end if;
  insert into economy.goal_days (user_id, day, goal) values (p_user, p_day, economy.goal_peek(p_user, p_day))
  on conflict do nothing;
  select g.goal into v from economy.goal_days g where g.user_id = p_user and g.day = p_day;
  return v;
end $$;

-- [p_day] 앞으로 이어진 목표 날 수(보너스를 받은 날 · 연속 지킴이 있던 날)
create or replace function economy.goal_streak_before(p_user uuid, p_day date, p_max int default 60)
returns int language plpgsql stable security definer set search_path = public, economy as $$
declare v int := 0; d date := p_day - 1;
begin
  while v < p_max loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || d)
      or exists (select 1 from public.boosts b
                  where b.user_id = p_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(d + 1) and b.ends_at > economy.game_day_start(d)));
    v := v + 1; d := d - 1;
  end loop;
  return v;
end $$;

-- 목표 보너스 정산 — 그 날의 보상 걸음이 그 날의 목표를 채웠고 아직 안 받았으면 준다(러닝을 기록할 때 서버가 바로).
-- 금액 · 연속 · 7일 연속 상급 상자는 예전 goal_claim_day 그대로. 받지 않았으면 null
create or replace function economy.goal_settle(p_user uuid, p_day date)
returns numeric language plpgsql security definer set search_path = public, economy as $$
declare
  v_goal int;
  v_streak int;
  v_bonus numeric;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
begin
  if p_user is null or p_day is null or p_day > economy.game_day(now()) or p_day < economy.game_day(now()) - 7 then
    return null;
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || p_user::text));
  if exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || p_day) then
    return null;
  end if;
  v_goal := economy.goal_of(p_user, p_day);
  if economy.verified_steps_on(p_user, p_day) < v_goal then
    return null;
  end if;
  v_streak := 1 + economy.goal_streak_before(p_user, p_day, greatest(8, v_need));
  v_bonus := round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2);
  perform economy.ledger_apply(p_user, 'BONUS_GOAL', v_bonus,
                               format('목표 달성 보너스 (%s일 연속)', least(v_streak, 8)), 'goal:' || p_day);
  if p_day < economy.game_day(now()) then
    update public.sup_ledger l set occurred_at = economy.game_day_start(p_day + 1) - interval '1 second'
     where l.user_id = p_user and l.ref = 'goal:' || p_day;
  end if;
  if v_need > 0 and v_streak >= v_need and not exists (
       select 1 from economy.box_awards a
        where a.user_id = p_user and a.source like 'STREAK:%' and a.day > p_day - v_need and a.day <= p_day) then
    perform economy.box_grant(p_user, 'STREAK:' || p_day, 'GIFT', p_day);
  end if;
  return v_bonus;
end $$;

-- 예전 앱의 받기 — 모양 · 거절 그대로(이미 받았으면 23505 · 못 채웠으면 23514)
create or replace function public.goal_claim_day(p_day date)
returns numeric language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_today date := economy.game_day(now());
  v numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_day is null or p_day > v_today or p_day < v_today - 7 then
    raise exception '목표 보너스는 최근 7일 안의 날만 받을 수 있습니다' using errcode = '22023';
  end if;
  if exists (select 1 from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || p_day) then
    raise exception '이미 처리된 요청입니다' using errcode = '23505';
  end if;
  v := economy.goal_settle(v_user, p_day);
  if v is null then
    raise exception '아직 목표를 채우지 않았습니다' using errcode = '23514';
  end if;
  return v;
end $$;

-- 목표 바꾸기 — 4,000 ~ 20,000 · 500 단위. 오늘 보상 걸음이 있으면 오늘 목표는 그대로(바꾼 목표는 다음 날부터)
create or replace function public.profile_set_daily_goal(p_goal integer)
returns integer language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_goal int := economy.goal_clamp(p_goal);
  v_today date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if economy.verified_steps_on(v_user, v_today) > 0
     or exists (select 1 from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_today) then
    perform economy.goal_of(v_user, v_today);
  else
    insert into economy.goal_days (user_id, day, goal) values (v_user, v_today, v_goal)
    on conflict (user_id, day) do update set goal = excluded.goal;
  end if;
  insert into public.profiles (id, daily_goal) values (v_user, v_goal)
  on conflict (id) do update set daily_goal = excluded.daily_goal;
  return v_goal;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  5. 상자 — 보상 걸음으로 센다
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.box_on_session()
returns trigger language plpgsql security definer set search_path = public, economy as $$
declare
  v_day date;
  v_week date;
  v_steps bigint;
  v_t jsonb;
begin
  if new.verdict = 'VOID'
     or coalesce(case when economy.reward_rule_old() then new.rewarded_steps else new.reward_steps end, 0) <= 0 then
    return null;
  end if;
  v_day := economy.game_day(new.started_at);
  v_steps := economy.box_steps(new.user_id, v_day, v_day);
  for v_t in select jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) loop
    if v_steps >= (v_t #>> '{}')::numeric then
      perform economy.box_grant(new.user_id, 'RUN' || (v_t #>> '{}') || ':' || v_day, 'FREE', v_day);
    end if;
  end loop;
  if coalesce(economy.setting_num('weekly_box_steps'), 0) > 0 then
    v_week := economy.game_week(v_day);
    v_steps := economy.box_steps(new.user_id, v_week, v_week + 6);
    if v_steps >= economy.setting_num('weekly_box_steps') then
      perform economy.box_grant(new.user_id, 'WEEK:' || v_week, 'GIFT', v_week);
    end if;
  end if;
  return null;
end $$;

-- 예전 칸은 그대로(steps_today · steps_week 의 값만 보상 걸음으로). 홈의 보상 막대가 읽는 칸을 더한다
create or replace function public.box_status()
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
  v_week date := economy.game_week(economy.game_day(now()));
  v_steps_day bigint;
  v_steps_week bigint;
  v_goal int;
  v_goal_now int;
  v_paid numeric;
  v_before int;
  v_streak int;
  v_best int;
  v_need int := greatest(coalesce(economy.setting_num('streak_box_days'), 0)::int, 0);
  v_last date;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  v_steps_day := economy.box_steps(v_user, v_day, v_day);
  v_steps_week := economy.box_steps(v_user, v_week, v_day);
  v_goal := economy.goal_peek(v_user, v_day);
  select economy.goal_clamp(p.daily_goal) into v_goal_now from public.profiles p where p.id = v_user;
  select l.amount into v_paid from public.sup_ledger l where l.user_id = v_user and l.ref = 'goal:' || v_day;
  v_before := economy.goal_streak_before(v_user, v_day, 400);
  v_streak := v_before + 1;
  select coalesce(max(n), 0) into v_best from (
    select count(*) as n from (
      select d, d - (row_number() over (order by d))::int as grp
        from (select distinct substr(l.ref, 6)::date as d from public.sup_ledger l
               where l.user_id = v_user and l.ref like 'goal:%') x) y
     group by grp) z;
  v_best := greatest(v_best, case when v_paid is not null then v_streak else v_before end);
  select max(a.day) into v_last from economy.box_awards a where a.user_id = v_user and a.source like 'STREAK:%';
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
    'balance', economy.balance_of(v_user),
    -- 0118 — 오늘의 보상(홈 막대)
    'rule', case when economy.reward_rule_old() then 'OLD' else 'REWARD_STEPS' end,
    'reward_steps_today', economy.reward_steps_on(v_user, v_day),
    'goal', v_goal,
    'goal_next', case when v_goal_now is distinct from v_goal then v_goal_now end,
    'goal_paid', v_paid,
    'goal_bonus', round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2),
    'streak', v_streak,
    'streak_best', v_best,
    'streak_box_in', case when v_need <= 0 then null
                          else greatest(v_need - v_streak, coalesce(v_last + v_need - v_day, 0),
                                        case when v_paid is not null then 1 else 0 end) end,
    'ticks', (select coalesce(jsonb_agg(x.j order by x.s, x.o), '[]'::jsonb) from (
        select (t #>> '{}')::int as s, 0 as o,
               jsonb_build_object('steps', (t #>> '{}')::int, 'kind', 'BOX',
                 'got', exists (select 1 from economy.box_awards a where a.user_id = v_user
                                 and a.source = 'RUN' || (t #>> '{}') || ':' || v_day)) as j
          from jsonb_array_elements(coalesce(economy.setting('run_box_steps'), '[]'::jsonb)) t
        union all
        select v_goal, 1, jsonb_build_object('steps', v_goal, 'kind', 'GOAL', 'got', v_paid is not null)) x),
    'surge', jsonb_build_object(
        'steps', economy.reward_steps_week(v_user, v_day), 'target', economy.event_target('step_surge'), 'reward', economy.event_reward('step_surge'),
        'claimed', exists (select 1 from public.event_claims c where c.user_id = v_user and c.event_id = 'step_surge'
                             and c.period = economy.event_period('step_surge', 'Asia/Seoul'))),
    'recent', (select coalesce(jsonb_agg(jsonb_build_object('day', r.day, 'kind', r.kind, 'amount', r.amount, 'at', r.at)
                                         order by r.at desc), '[]'::jsonb) from (
        select a.day, case when a.source like 'RUN%' then 'RUN_BOX' when a.source like 'WEEK:%' then 'WEEK_BOX'
                           when a.source like 'STREAK:%' then 'STREAK_BOX' else 'LEVEL_BOX' end as kind,
               null::numeric as amount, a.granted_at as at
          from economy.box_awards a
         where a.user_id = v_user and a.granted_at > now() - interval '3 days'
           and (a.source like 'RUN%' or a.source like 'WEEK:%' or a.source like 'STREAK:%' or a.source like 'LEVEL:%')
        union all
        select substr(l.ref, 6)::date, 'GOAL', l.amount, l.created_at
          from public.sup_ledger l
         where l.user_id = v_user and l.ref like 'goal:%' and l.created_at > now() - interval '3 days') r));
end $$;

-- ════════════════════════════════════════════════════════════════════
--  6. 규칙 묶음 · 강화 성공 수 · 운영자 손잡이
-- ════════════════════════════════════════════════════════════════════
create or replace function public.shoe_rules_v4()
returns jsonb language plpgsql stable security definer set search_path = public, economy as $$
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
      from (values ('COMMON', 0), ('RARE', 1), ('EPIC', 2), ('LEGENDARY', 3), ('REDLINE', 4), ('FINISH', 5)) g(r, o)),
    -- 0118 — 강화 경험치 · 목표(앱이 미리보기 · 안내 글에 쓴다 — 값은 늘 서버 견적이 정한다)
    'forge', jsonb_build_object(
        'rule_version', 'forge-xp-1',
        'step', economy.fx('forge_xp_step', 3.1),
        'need', economy.fx('forge_xp_need', 2.88),
        'spec', coalesce(economy.setting('forge_xp_spec'), '[0.7, 1.3]'::jsonb),
        'crit_chance', economy.fx('forge_xp_crit_chance', 0.1),
        'crit_mult', economy.fx('forge_xp_crit_mult', 2),
        'inherit', economy.fx('forge_xp_inherit', 0.5),
        'sup_per_unit', economy.fx('forge_xp_sup_per_unit', 24),
        'max_materials', economy.fx('forge_xp_max_materials', 10)::int),
    'goal', jsonb_build_object(
        'min', economy.fx('goal_min', 4000)::int, 'max', economy.fx('goal_max', 20000)::int,
        'default', economy.fx('goal_default', 6000)::int, 'step', 500));
end $$;

-- 오른 레벨 수의 합(예전 성공은 1씩, 경험치 넣기는 오른 레벨 수) — 모양 그대로
create or replace function public.my_forge_successes()
returns integer language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return (select coalesce(sum(case when r.kind = 'XP' then coalesce(r.levels, 0) else 1 end), 0)::int
            from public.forge_requests r where r.user_id = v_user and r.status = 'SUCCESS');
end $$;

create or replace function economy.ops_setting_rule(p_key text, out kind text, out lo numeric, out hi numeric, out label text)
returns record language sql immutable as $$
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
    ('run_box_steps', 'steps', 100, 100000, '러닝 상자 걸음(오늘 보상 걸음)'),
    ('weekly_box_steps', 'num', 0, 1000000, '주간 상급 상자 걸음(0 = 끔)'),
    ('streak_box_days', 'num', 0, 60, '연속 목표 상급 상자 날 수(0 = 끔)'),
    ('box_price', 'num', 0, 100000, '상자 값(SUP, 0 = 안 팜)'),
    ('box_daily_buy', 'num', 0, 50, '상자 하루 살 수 있는 수'),
    ('premium_box_price', 'num', 0, 1000000, '상급 상자 값(SUP, 0 = 안 팜)'),
    ('premium_box_weekly_buy', 'num', 0, 50, '상급 상자 한 주 살 수 있는 수'),
    ('forge_material_min_durability', 'num', 0, 100, '예전 강화(예전 앱) 재료 최소 내구도(%)'),
    ('app_current_version_code', 'num', 1, 999, '아이템 가방이 있는 앱 판(이 번호부터)'),
    ('forge_xp_step', 'num', 1.5, 6, '강화 경험치: 등급 한 칸의 값(배)'),
    ('forge_xp_need', 'num', 1, 10, '강화 경험치: Lv1 통의 크기(일반 켤레)'),
    ('forge_xp_spec', 'pair', 0.3, 2, '강화 경험치: 재료 스펙 [가장 낮게, 가장 높게]'),
    ('forge_xp_crit_chance', 'num', 0, 0.5, '강화 경험치: 대성공 확률'),
    ('forge_xp_crit_mult', 'num', 1, 5, '강화 경험치: 대성공 배수'),
    ('forge_xp_inherit', 'num', 0, 1, '강화 경험치: 강화한 재료가 물려주는 몫'),
    ('forge_xp_sup_per_unit', 'num', 1, 1000, '강화 경험치: 일반 1켤레값(SUP — 닳은 재료의 수리비를 이것으로 나눠 뺀다)'),
    ('forge_xp_max_materials', 'num', 1, 20, '강화 경험치: 한 번에 넣는 최대 켤레'),
    ('goal_min', 'num', 1000, 20000, '하루 목표 가장 낮게(걸음)'),
    ('goal_max', 'num', 4000, 100000, '하루 목표 가장 높게(걸음)'),
    ('goal_default', 'num', 1000, 100000, '새 사용자 하루 목표(걸음)')
  ) r(key, kind, lo, hi, label) where r.key = p_key
$$;

create or replace function public.ops_economy_settings(p_token text)
returns jsonb language plpgsql security definer set search_path = public, economy as $$
declare v_actor text := economy.ops_actor(p_token);
begin
  return jsonb_build_object('ok', true, 'settings', (
    select jsonb_agg(jsonb_build_object('key', k.key, 'value', s.value, 'label', (economy.ops_setting_rule(k.key)).label,
                                        'kind', (economy.ops_setting_rule(k.key)).kind, 'updated_at', s.updated_at) order by k.o)
      from (values ('points_per_step', 1), ('repair_share', 2), ('reward_luck', 3), ('wear_luck', 4), ('free_repair_hours', 5),
                   ('boost_shop_enabled', 6), ('daily_earn_cap', 7), ('no_gps_daily_cap', 8), ('upload_daily_cap', 9),
                   ('daily_free_draws', 10), ('run_box_steps', 11), ('weekly_box_steps', 12), ('streak_box_days', 13),
                   ('box_price', 14), ('box_daily_buy', 15), ('premium_box_price', 16), ('premium_box_weekly_buy', 17),
                   ('forge_material_min_durability', 18), ('app_current_version_code', 19),
                   ('forge_xp_step', 20), ('forge_xp_need', 21), ('forge_xp_spec', 22), ('forge_xp_crit_chance', 23),
                   ('forge_xp_crit_mult', 24), ('forge_xp_inherit', 25), ('forge_xp_sup_per_unit', 26),
                   ('forge_xp_max_materials', 27), ('goal_min', 28), ('goal_max', 29), ('goal_default', 30)) k(key, o)
      left join public.economy_settings s on s.key = k.key));
end $$;

create or replace function public.ops_economy_set(p_token text, p_setting text, p_value jsonb, p_confirm text DEFAULT NULL::text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'economy'
AS $function$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  r record;
  v_refuse jsonb;
  v_old jsonb;
  v_ok boolean := true;
  v_n numeric;
  e jsonb;
begin
  select * into r from economy.ops_setting_rule(p_setting);
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
  elsif r.kind = 'pair' then
    v_ok := jsonb_typeof(p_value) = 'array' and jsonb_array_length(p_value) = 2
            and (select bool_and(jsonb_typeof(x) = 'number' and (x #>> '{}')::numeric between r.lo and r.hi)
                   from jsonb_array_elements(p_value) x)
            and (p_value ->> 0)::numeric <= (p_value ->> 1)::numeric;
  elsif r.kind = 'steps' then
    v_ok := jsonb_typeof(p_value) = 'array' and jsonb_array_length(p_value) between 0 and 5
            and coalesce((select bool_and(jsonb_typeof(x) = 'number' and (x #>> '{}')::numeric between r.lo and r.hi)
                            from jsonb_array_elements(p_value) x), true);
  end if;
  if not v_ok then
    return jsonb_build_object('ok', false, 'code', 'value',
      'message', case r.kind when 'bool' then '켬 · 끔(true · false)으로 넣어 주세요'
                             when 'luck' then format('[작게, 자주, 크게] 세 숫자(%s ~ %s, 작은 것부터)', r.lo, r.hi)
                             when 'pair' then format('[가장 낮게, 가장 높게] 두 숫자(%s ~ %s, 작은 것부터)', r.lo, r.hi)
                             when 'steps' then format('걸음 수 목록(예: [2000, 4000], %s ~ %s)', r.lo, r.hi)
                             else format('%s ~ %s 사이 숫자', r.lo, r.hi) end);
  end if;
  v_refuse := economy.ops_confirm(v_session, p_confirm, format('경제 설정 바꾸기(%s)', r.label), true);
  if v_refuse is not null then
    return v_refuse;
  end if;
  select s.value into v_old from public.economy_settings s where s.key = p_setting;
  insert into public.economy_settings (key, value) values (p_setting, p_value)
  on conflict (key) do update set value = excluded.value, updated_at = now();
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_economy_set', p_setting, jsonb_build_object('by', v_session.label, 'old', v_old, 'new', p_value));
  return jsonb_build_object('ok', true, 'key', p_setting, 'old', v_old, 'value', p_value);
end $function$;

create or replace function public.ops_economy_status(p_token text)
 RETURNS jsonb
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO 'public', 'economy'
AS $function$
declare
  v_actor text := economy.ops_actor(p_token);
  v_runs jsonb;
  v_money jsonb;
  v_yday jsonb;
  v_stuck int;
  v_versions jsonb;
  v_forge jsonb;
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

  -- 지난 7일 강화 경험치(0118): 넣은 횟수 · 넣은 켤레 · 오른 레벨 · 대성공 · 넣은 사람
  select jsonb_build_object('adds', count(*), 'pairs', coalesce(sum(cardinality(r.material_ids)), 0),
                            'levels', coalesce(sum(r.levels), 0), 'crits', coalesce(sum(r.roll), 0),
                            'people', count(distinct r.user_id))
    into v_forge from public.forge_requests r
   where r.kind = 'XP' and r.created_at > now() - interval '7 days';

  return jsonb_build_object('ok', true, 'actor', v_actor, 'runs_7d', v_runs, 'money_7d', v_money,
                            'yesterday', v_yday, 'stuck', v_stuck, 'versions', v_versions, 'forge_7d', v_forge);
end $function$;

-- ════════════════════════════════════════════════════════════════════
--  7. 러닝 기록 — record_session(0117 본문 복사 + 세 곳)
-- ════════════════════════════════════════════════════════════════════
-- 0117 본문 그대로, 세 곳만 (0118): ① 보상 걸음(reward_steps)을 적는다 ② 같은 경로 검사를 모든 계정으로 ③ 기록한 뒤 목표 보너스 정산.
-- 경로가 받쳐 주지 않은 러닝은 까닭에 '경로가 확인되지 않아 보상 걸음에 들지 않았습니다'를 더한다. 다시 고칠 때는 이 본문(0118)을 복사한다.
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
  v_reward_steps int := 0;
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
  -- 하루 걸음 상한 · 에너지로 적립에서 빠진 걸음 — 그 몫만큼은 신발이 닳지 않는다 (0116)
  v_lost_cap int := 0;
  -- 0117 — 서버가 세지 못한 크루 러닝 시간을 정산 때 공용 체력에서
  v_run_sec numeric := 0;
  v_unseen numeric := 0;
  v_need numeric := 0;
  v_take numeric := 0;
  v_uncovered numeric := 0;
  v_pool_row economy.crew_day_pools;
  v_flash bigint;
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
        -- 모든 계정의 러닝과 맞대어 본다(0118) — 다른 계정의 경로를 그대로 올려 상자 · 목표를 받지 못하게
        select 1 from public.walk_sessions s
         where s.track <> '' and md5(s.track) = md5(v_track)
           and not (s.user_id = v_user and s.started_at = p_started_at)) then
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
  -- 번개러닝 보너스는 번개 글 하나에 한 번(0117) — 방을 닫고 같은 글로 다시 열어 보너스를 되풀이하지 않게
  select x.flash_post_id into v_flash from public.parties x where x.id = v_party_id;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id)
     or (v_flash is not null and exists (
           select 1 from public.walk_sessions s join public.parties x on x.id = s.party_id
            where s.user_id = v_user and x.flash_post_id = v_flash and s.party_size > 1 and s.verdict <> 'VOID')) then
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
      v_lost_cap := v_lost_cap + v_rewardable - greatest(economy.max_daily_steps() - v_already_steps, 0);
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
    -- 서버가 세지 못한 달린 시간(상태 알림이 오지 않은 동안 — 연결이 끊겼다 · 폰이 출발하지 않았다) 만큼 공용 체력을 지금 쓴다 (0117).
    -- 예전엔 알림이 안 오면 체력이 줄지 않아 러닝 전체가 적립됐다(둘이 3시간 오프라인 485 SUP · 걸음만 6시간 1,388 SUP).
    -- 체력이 모자라면 그 시간만큼 적립에서 뺀다. 0 이던 구간(이미 뺐다)과 서버가 끝낸 뒤(v_cut)는 다시 세지 않는다
    if v_rewardable > 0 then
      v_run_sec := v_elapsed;
      if v_cut is not null then
        v_run_sec := least(v_run_sec, greatest(extract(epoch from (v_cut - p_started_at)), 0));
      end if;
      v_unseen := greatest(v_run_sec - coalesce(v_cr.active_sec, 0), 0);
      v_unseen := greatest(v_unseen - greatest(economy.crew_empty_sec(v_cr.crew_id, v_cr.day, p_started_at,
                                least(p_ended_at, coalesce(v_cut, p_ended_at))) - coalesce(v_cr.empty_sec, 0), 0), 0);
      if v_unseen > 0 then
        select * into v_pool_row from economy.crew_day_pools d
         where d.crew_id = v_cr.crew_id and d.day = v_cr.day for update;
        v_need := v_unseen / economy.durability_sec_per_point()::numeric;
        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0), 0));
        if v_take > 0 then
          update economy.crew_day_pools d
             set consumed = least(d.consumed + v_take, d.capacity), version = d.version + 1,
                 low_at = case when d.low_at is null and d.capacity > 0
                                    and d.capacity - d.consumed - v_take <= d.capacity * 0.2 then now() else d.low_at end,
                 exhausted_at = case when d.capacity > 0 and d.capacity - d.consumed - v_take <= 0.0001
                                     then coalesce(d.exhausted_at, now()) else d.exhausted_at end
           where d.crew_id = v_cr.crew_id and d.day = v_cr.day
          returning * into v_pool_row;
          update economy.crew_runs c set consumed = c.consumed + v_take
           where c.user_id = v_user and c.day = v_cr.day;
          -- 이 정산으로 0 이 됐다 — 지금부터 0 인 구간을 연다(달리는 다른 사람의 그 뒤 걸음은 적립하지 않는다)
          if v_pool_row.capacity > 0 and v_pool_row.capacity - v_pool_row.consumed <= 0.0001
             and not exists (select 1 from economy.crew_pool_empty x
                              where x.crew_id = v_cr.crew_id and x.day = v_cr.day and x.to_at is null) then
            insert into economy.crew_pool_empty (crew_id, day, from_at) values (v_cr.crew_id, v_cr.day, now())
            on conflict do nothing;
          end if;
        end if;
        v_uncovered := (v_need - v_take) * economy.durability_sec_per_point();
        if v_uncovered > 0 and v_run_sec > 0 then
          v_frac := greatest(v_frac - v_uncovered / v_run_sec, 0);
        end if;
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
        v_lost_cap := v_lost_cap + v_rewardable - greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
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
  -- 크루 러닝도 같은 상한을 함께 쓴다(0117) — 크루 러닝은 다른 상한이 없어 걸음만으로 끝없이 받았다
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

  -- 크루 러닝이 상한에 걸렸으면 세 몫(기본 · 인원 · 레벨)을 같은 비율로 줄여 합이 원장과 같게 (0117)
  if v_pool = 'CREW' and v_points_full > 0 and v_points < v_points_full then
    v_base := round(v_base * v_points / v_points_full, 4);
    v_head_sup := round(v_head_sup * v_points / v_points_full, 4);
    v_lvl_sup := v_points - v_base - v_head_sup;
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

  -- ── 보상 걸음 (0118) ──
  -- 상자 · 목표 보너스 · 연속 · 스텝 서지가 세는 걸음 하나. 무효 · 보류 · 경로가 받쳐 주지 않은 러닝은 0.
  -- 혼자 러닝은 출발 때 남은 내구도로 달릴 수 있던 시간만큼(내구도 0 이면 0), 크루 러닝은 공용 체력이 받쳐 준 몫만큼.
  -- 에너지 · 하루 상한과는 상관없다(에너지를 다 쓴 뒤의 걸음도 센다). 하루 48,000 은 합할 때(economy.reward_steps_on)
  v_reward_steps := case
    when v_verdict = 'VOID' or v_held or not v_gps_backed or coalesce(v_verified, 0) <= 0 then 0
    when v_pool = 'CREW' then floor(v_verified * least(greatest(v_frac, 0), 1))::int
    when v_shoe.id is null then v_verified
    when coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() < v_elapsed
      then floor(v_verified * coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() / greatest(v_elapsed, 1)::numeric)::int
    else v_verified end;
  if v_verdict <> 'VOID' and not v_held and not v_gps_backed and coalesce(v_verified, 0) > 0 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로가 확인되지 않아 보상 걸음에 들지 않았습니다');
  end if;

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool, reward_steps
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool, v_reward_steps
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
  -- 하루 걸음 상한 · 에너지 · 하루 금액 상한으로 적립에서 빠진 몫만큼은 닳지 않는다 — 적립이 없는 구간에 수리비만 늘지 않게 (0116).
  -- 내구도가 다 닳아 빠진 몫은 그대로 닳는다(신발이 다 닳았다). 걸음 0 인 러닝은 예전처럼 켜 둔 시간만큼(0068)
  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(v_elapsed) * v_wear_luck
              * least(1, greatest(0, (1 - v_lost_cap::numeric / greatest(v_verified, 1))
                                     * case when v_points_full > 0 then v_points / v_points_full else 1 end)))::int end;
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

  -- 목표 보너스 — 그 날의 보상 걸음이 그 날의 목표를 채웠고 아직 안 받았으면 서버가 바로 준다(예전 앱에도, 0118).
  -- 보너스에 문제가 생겨도 러닝 기록은 남긴다
  if v_reward_steps > 0 then
    begin
      perform economy.goal_settle(v_user, v_gday);
    exception when others then
      raise warning 'goal_settle(%, %) 실패: %', v_user, v_gday, sqlerrm;
    end;
  end if;

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason,
    case when v_verdict = 'VOID' then null else round(v_luck, 3) end;
end;
$$;

comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089) · 운 · 상한 배수 (0116) · 보상 걸음 · 목표 보너스 (0118)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  8. 권한 — 서버 안의 셈은 앱이 부르지 못한다
-- ════════════════════════════════════════════════════════════════════
revoke all on function economy.fx(text, numeric) from public, anon, authenticated;
revoke all on function economy.forge_need(text, int) from public, anon, authenticated;
revoke all on function economy.forge_cum_need(text, int) from public, anon, authenticated;
revoke all on function economy.forge_invested(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.shoe_spec_q(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.shoe_stars(numeric) from public, anon, authenticated;
revoke all on function economy.forge_xp_value(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.forge_xp_block(uuid, public.market_sneakers, public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.forge_xp_target_block(uuid, public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.forge_pct(bigint, int) from public, anon, authenticated;
revoke all on function economy.forge_xp_apply(text, int, bigint, bigint) from public, anon, authenticated;
revoke all on function economy.forge_account_after(uuid, bigint, int, bigint[]) from public, anon, authenticated;
revoke all on function economy.forge_xp_target_json(public.market_sneakers) from public, anon, authenticated;
revoke all on function economy.forge_xp_quote_for(uuid, bigint, bigint[]) from public, anon, authenticated;
revoke all on function economy.forge_xp_precious(uuid, public.market_sneakers, public.market_sneakers, numeric) from public, anon, authenticated;
revoke all on function economy.forge_xp_result_json(public.forge_requests) from public, anon, authenticated;
revoke all on function economy.forge_xp_at_max() from public, anon, authenticated;
revoke all on function economy.reward_steps_on(uuid, date) from public, anon, authenticated;
revoke all on function economy.reward_steps_week(uuid, date) from public, anon, authenticated;
revoke all on function economy.goal_clamp(int) from public, anon, authenticated;
revoke all on function economy.goal_peek(uuid, date) from public, anon, authenticated;
revoke all on function economy.goal_of(uuid, date) from public, anon, authenticated;
revoke all on function economy.goal_streak_before(uuid, date, int) from public, anon, authenticated;
revoke all on function economy.goal_settle(uuid, date) from public, anon, authenticated;
revoke all on function economy.reward_rule_old() from public, anon, authenticated;
revoke all on function economy.box_steps(uuid, date, date) from public, anon, authenticated;
revoke all on function public.forge_xp_materials(bigint) from public, anon;
grant execute on function public.forge_xp_materials(bigint) to authenticated;
revoke all on function public.forge_xp_quote(bigint, bigint[]) from public, anon;
grant execute on function public.forge_xp_quote(bigint, bigint[]) to authenticated;
revoke all on function public.forge_xp_add(uuid, bigint, bigint[], text) from public, anon;
grant execute on function public.forge_xp_add(uuid, bigint, bigint[], text) to authenticated;
revoke all on function public.forge_xp_result(uuid) from public, anon;
grant execute on function public.forge_xp_result(uuid) to authenticated;
revoke all on function public.my_shoe_xp() from public, anon;
grant execute on function public.my_shoe_xp() to authenticated;
revoke all on table economy.goal_days from public, anon, authenticated;

comment on function public.forge_xp_add(uuid, bigint, bigint[], text) is
  '강화 경험치 넣기 — 재료 1 ~ 10켤레를 태워 대상 신발의 통에 점수를 더한다. 실패 없음 · 대성공은 재료마다 · 통이 차면 레벨 + 1 (0118)';
comment on function public.forge_xp_quote(bigint, bigint[]) is
  '강화 경험치 견적 — 대성공 없이 받는 최소 · 통 앞뒤 · 하루 에너지 · 상한 앞뒤 · 견적 서명 (0118)';
comment on table economy.goal_days is '그 날의 목표 — 바꾼 목표는 다음 날부터 (0118)';
