-- ════════════════════════════════════════════════════════════════════
--  0066 — 신발 경제 3판: 6등급 · 속성 없음 · 새 뽑기 · 레벨 · 포인트 · 내구도 = 시간 · 초기화 (2026-10-17 사용자 결정)
--
--  사용자 결정(이 대화):
--    · 등급 6개 — 일반 · 레어 · 에픽 · 레전더리 · 레드라인 · 피니시(예전에는 레드라인 · 피니시가 레전더리 안의 시리즈였다).
--    · 속성(불 · 물 · 번개 · 바람)은 없앤다 — 새 신발은 'NONE'.
--    · 뽑기 — 무료: 일반 79 · 레어 20 · 에픽 1%. 상급: 레어 60 · 에픽 28 · 레전더리 9 · 레드라인 2.5 · 피니시 0.5%(일반 없음).
--      지갑 첫 선물의 첫 번(Genesis)은 상급 표의 에픽 이상.
--    · 새 신발로 달릴 수 있는 시간(1분 = 내구도 5, 0064) — 10 · 15 · 25 · 40 · 60 · 90분, 신발마다 ±10%.
--    · 효율(적립 보너스) — 에픽부터 급수적으로: 2~4 · 4~6 · 8~11 · 16~20 · 28~35 · 45~55%. 착화감 0~3 … 15~18%(3%p 씩).
--    · 최대 레벨 — 일반 · 레어 20, 에픽 26 · 레전더리 27 · 레드라인 28 · 피니시 30.
--    · 레벨업 포인트 — 오른 레벨마다 2~10: 1 · 11~15: 2 · 16~20: 3 · 21~25: 5 · 26: 15 · 27: 30 · 28: 60 · 29: 100 · 30: 150,
--      등급 배수 ×1 · ×2 · ×3 · ×4 · ×6 · ×8. 포인트 1 = 효율 +0.05%p · 착화감 +0.02%p(20% 까지) · 내구도 기본의 +1%.
--    · 수리 — 새 신발(레벨 1)을 가득 고치는 값이 일반 : 피니시 = 1 : 4(일반 13.1 · 피니시 52.5 SUP), 레벨마다 +5%.
--    · 적립은 남은 내구도가 0 일 때만 없다. 한 러닝 안에서 다 닳으면 그 뒤 걸음은 적립하지 않는다.
--    · 모든 신발 · SUP 를 한 번 비우고 새로 시작한다(지금 쓰는 사람은 직원뿐) — 첫 신발 하나 + 무료 뽑기 10회.
--    · 잠재능력(21 레벨 위의 해방)은 보류.
--
--  체인: v3 신발 컨트랙트는 4등급과 예전 스탯 범위만 안다(새 신발은 발행이 거절된다). 새 컨트랙트 전까지 신발의 체인 기록
--  (금고 발행 · 꺼내기 · 지갑 보너스 발행)을 멈춘다(economy_settings.shoe_chain_enabled = false). 러닝 증명 · 배지는 그대로.
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('shoe_chain_enabled', 'false'::jsonb)
on conflict (key) do nothing;

-- 신발을 체인에 기록하는가(금고 발행 · 스탯 갱신 · 꺼내기 · 지갑 보너스 발행)
create or replace function economy.shoe_chain_on() returns boolean
  language sql stable security definer set search_path = public, economy as $$
  select coalesce((economy.setting('shoe_chain_enabled') #>> '{}')::boolean, false)
$$;
revoke all on function economy.shoe_chain_on() from public;

-- ── 등급 · 속성 칸 ──────────────────────────────────────────────────
-- 등급 6개(레드라인 · 피니시를 더한다), 속성은 'NONE'(새 신발). 이름이 정해지지 않은 예전 검사도 찾아서 바꾼다
do $$
declare r record;
begin
  for r in
    select c.conrelid::regclass as tbl, c.conname
      from pg_constraint c
     where c.contype = 'c'
       and c.conrelid in ('public.market_sneakers'::regclass, 'public.market_bids'::regclass)
       and (pg_get_constraintdef(c.oid) ilike '%faction%' or pg_get_constraintdef(c.oid) ilike '%LEGENDARY%')
  loop
    execute format('alter table %s drop constraint %I', r.tbl, r.conname);
  end loop;
end $$;
alter table public.market_sneakers add constraint market_sneakers_rarity_check
  check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'));
alter table public.market_sneakers add constraint market_sneakers_faction_check
  check (faction in ('NONE', 'FIRE', 'WATER', 'LIGHTNING', 'WIND'));
alter table public.market_sneakers alter column faction set default 'NONE';
alter table public.market_bids add constraint market_bids_rarity_check
  check (rarity in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'));
alter table public.market_bids add constraint market_bids_faction_check
  check (faction in ('NONE', 'FIRE', 'WATER', 'LIGHTNING', 'WIND'));

-- ── 등급 규칙 ────────────────────────────────────────────────────────
create or replace function economy.rarity_ord(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 0 when 'RARE' then 1 when 'EPIC' then 2
                       when 'LEGENDARY' then 3 when 'REDLINE' then 4 when 'FINISH' then 5 end
$$;

create or replace function economy.max_level(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 20 when 'RARE' then 20 when 'EPIC' then 26
                       when 'LEGENDARY' then 27 when 'REDLINE' then 28 when 'FINISH' then 30 end
$$;

-- 예전 그림(속성 × 변형)을 고를 때만 쓴다 — 새 신발은 도감 모델(draw_model)로 그린다
create or replace function economy.variant_count(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 4 when 'RARE' then 4 when 'EPIC' then 3 else 2 end
$$;

create or replace function economy.efficiency_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 200 when 'RARE' then 400 when 'EPIC' then 800
                       when 'LEGENDARY' then 1600 when 'REDLINE' then 2800 else 4500 end,
         case p_rarity when 'COMMON' then 400 when 'RARE' then 600 when 'EPIC' then 1100
                       when 'LEGENDARY' then 2000 when 'REDLINE' then 3500 else 5500 end
$$;

create or replace function economy.comfort_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select 300 * coalesce(economy.rarity_ord(p_rarity), 0), 300 * coalesce(economy.rarity_ord(p_rarity), 0) + 300
$$;

-- 새 신발 최대 내구도(달린 1분 = 5) — 10 · 15 · 25 · 40 · 60 · 90분, ±10%
create or replace function economy.durability_range(p_rarity text, out lo int, out hi int)
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 45 when 'RARE' then 70 when 'EPIC' then 115
                       when 'LEGENDARY' then 180 when 'REDLINE' then 270 else 405 end,
         case p_rarity when 'COMMON' then 55 when 'RARE' then 80 when 'EPIC' then 135
                       when 'LEGENDARY' then 220 when 'REDLINE' then 330 else 495 end
$$;

-- 등급 범위 안에서 5 단위(1분)로 고른다. p_roll 은 0 이상의 아무 정수(씨앗 바이트 · 해시)
create or replace function economy.durability_roll(p_rarity text, p_roll bigint) returns int
  language sql immutable as $$
  select r.lo + 5 * (abs(p_roll) % ((r.hi - r.lo) / 5 + 1))::int from economy.durability_range(p_rarity) r
$$;

-- 내구도 1 로 달릴 수 있는 시간(초) — 1분마다 5 닳는다(0064 economy.durability_wear_time)
create or replace function economy.durability_sec_per_point() returns int
  language sql immutable as $$ select 12 $$;

-- 수리 1점 값 — 새 신발(레벨 1)을 가득 고치는 값이 일반의 4^(등급/5) 배(피니시 = 4 배)가 되게 등급 범위 가운데로 나눈다.
-- 레벨마다 +5%
create or replace function economy.repair_cost_per_unit(p_rarity text, p_level int) returns numeric
  language sql immutable as $$
  select round(12.5 * power(4::numeric, coalesce(economy.rarity_ord(p_rarity), 0) / 5.0)
               / ((r.lo + r.hi) / 2.0) * (1 + 0.05 * p_level), 8)
    from economy.durability_range(p_rarity) r
$$;

-- 적립 배율 — 남은 비율이 0 보다 크면 그대로, 다 닳았으면 없다
create or replace function economy.durability_factor(p_durability numeric) returns numeric
  language sql immutable as $$
  select case when coalesce(p_durability, 0) > 0 then 1.0 else 0 end::numeric
$$;

-- ── 레벨업 포인트 ────────────────────────────────────────────────────
-- 레벨 p_level 에 오를 때 받는 기본 포인트(등급 배수 전)
create or replace function economy.points_at_level(p_level int) returns int
  language sql immutable as $$
  select case
    when p_level is null or p_level <= 1 then 0
    when p_level <= 10 then 1
    when p_level <= 15 then 2
    when p_level <= 20 then 3
    when p_level <= 25 then 5
    when p_level = 26 then 15
    when p_level = 27 then 30
    when p_level = 28 then 60
    when p_level = 29 then 100
    else 150 end
$$;

-- 등급 배수 — 같은 레벨업이라도 등급마다 크게 다르다
create or replace function economy.points_rarity_mult(p_rarity text) returns int
  language sql immutable as $$
  select case p_rarity when 'COMMON' then 1 when 'RARE' then 2 when 'EPIC' then 3
                       when 'LEGENDARY' then 4 when 'REDLINE' then 6 when 'FINISH' then 8 else 0 end
$$;

-- 이 레벨까지 받은 포인트. 예전 신발(IMPORT · MINT)은 없다
create or replace function economy.points_total_of(p_origin text, p_rarity text, p_level int) returns int
  language sql immutable as $$
  select case when p_origin in ('IMPORT', 'MINT') then 0
              else economy.points_rarity_mult(p_rarity)
                   * coalesce((select sum(economy.points_at_level(l))::int
                                 from generate_series(2, greatest(coalesce(p_level, 1), 1)) l), 0) end
$$;

-- 포인트 1 의 값 — 효율 +0.05%p · 착화감 +0.02%p
create or replace function economy.point_efficiency_bps() returns int
  language sql immutable as $$ select 5 $$;
create or replace function economy.point_comfort_bps() returns int
  language sql immutable as $$ select 2 $$;

-- 내구도 포인트 1 이 올리는 최대 내구도 — 그 신발 기본 최대 내구도의 1%(반올림, 적어도 1). 앱 StatPoints.durabilityPerPoint 와 같다
create or replace function economy.point_durability_of(p_base int) returns int
  language sql immutable as $$ select greatest((coalesce(p_base, 0) + 50) / 100, 1) $$;

-- 강화 상한 — 등급의 최대 레벨(신발 체인 기록이 멈춰 있어 금고 토큰의 상한은 보지 않는다)
create or replace function economy.forge_max_level(p_rarity text, p_token numeric) returns int
  language sql immutable as $$ select economy.max_level(p_rarity) $$;

-- ── 뽑기 ─────────────────────────────────────────────────────────────
-- 어떤 표로 굴리는가 — 무료 뽑기 · 첫 신발은 무료, 그 밖(상급 · 예전 유료)은 상급
create or replace function economy.draw_kind(p_origin text) returns text
  language sql immutable as $$
  select case when p_origin in ('FREE_DRAW', 'STARTER') then 'FREE' else 'PREMIUM' end
$$;

-- 등급 가중치(천분율)
create or replace function economy.draw_weight_v3(p_kind text, p_rarity text) returns int
  language sql immutable as $$
  select case p_kind
    when 'FREE' then case p_rarity when 'COMMON' then 790 when 'RARE' then 200 when 'EPIC' then 10 else 0 end
    else case p_rarity when 'RARE' then 600 when 'EPIC' then 280 when 'LEGENDARY' then 90
                       when 'REDLINE' then 25 when 'FINISH' then 5 else 0 end
  end
$$;

-- 씨앗 결과 → 등급. p_min 이 있으면 그 등급 이상만(Genesis 는 에픽 이상) — 표 안의 위 등급끼리 비율은 그대로
create or replace function economy.roll_rarity_v3(p_roll bigint, p_kind text, p_min text default null) returns text
  language plpgsql immutable as $$
declare
  v_order text[] := array['COMMON', 'RARE', 'EPIC', 'LEGENDARY', 'REDLINE', 'FINISH'];
  v_from int := coalesce(economy.rarity_ord(p_min), 0) + 1;
  v_total int := 0;
  v_pick int;
  i int;
begin
  for i in v_from .. 6 loop
    v_total := v_total + economy.draw_weight_v3(p_kind, v_order[i]);
  end loop;
  v_pick := (abs(p_roll) % v_total)::int;
  for i in v_from .. 6 loop
    v_pick := v_pick - economy.draw_weight_v3(p_kind, v_order[i]);
    if v_pick < 0 then
      return v_order[i];
    end if;
  end loop;
  return v_order[6];
end $$;

-- ── 내 신발 · 효율 · 착화감 · 포인트 · 체인 · 뽑기 ─────────────────────────

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
    economy.points_total_of(s.origin, s.rarity, s.level),
    economy.points_total_of(s.origin, s.rarity, s.level) - s.pts_eff - s.pts_comfort - s.pts_dur
$$;

create or replace function public.economy_bootstrap()
returns void
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
      origin, efficiency_bps, comfort_bps, durability_pts, withdrawable
    ) values (
      v_user, 'NONE', 'COMMON', 0, 1, 1, 1.01, 100,
      'STARTER', 300, 100, 100, false
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

create or replace function economy.vault_mint_enqueue(p_sneaker bigint) returns void
language plpgsql security definer set search_path = public, economy as $$
begin
  -- 체인 기록이 실패해도 뽑기는 그대로 된다 — 경고만 남긴다
  begin
    -- 신발 체인 기록은 멈춰 있다(0066 — v3 컨트랙트는 4등급 · 예전 스탯 범위만 안다)
    if not economy.chain_jobs_on() or not economy.shoe_chain_on() then
      return;
    end if;
    insert into public.chain_jobs (kind, ref, user_id, sneaker_id)
    select 'VAULT_MINT', 'mint:' || s.id, s.owner_id, s.id
      from public.market_sneakers s
     where s.id = p_sneaker and s.token_id is null
    on conflict (kind, ref) do nothing;
  exception when others then
    raise warning 'vault_mint_enqueue: %', sqlerrm;
  end;
  return;
end $$;

create or replace function public.bonus_draw_request()
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v_genesis boolean;
  v_min text := nullif(economy.setting('premium_min_rarity') #>> '{}', '');
  v_id bigint;
  v_op uuid := gen_random_uuid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  if not economy.shoe_chain_on() then
    raise exception '신발을 지갑으로 발행하는 기능은 준비 중입니다' using errcode = '55000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, false);

  if economy.mints_today('BONUS_MINT') >= economy.setting_num('bonus_mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  select g.genesis_used < g.genesis_granted into v_genesis
    from public.draw_grants g where g.user_id = v_user and g.kind = 'BONUS';

  update public.draw_grants
     set used = used + 1,
         genesis_used = genesis_used + case when v_genesis then 1 else 0 end
   where user_id = v_user and kind = 'BONUS' and used < granted;
  if not found then
    raise exception '보너스 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  v_id := economy.draw_create(v_user, 'BONUS_DRAW',
                              case when v_genesis then 'EPIC' else v_min end, coalesce(v_genesis, false));
  update public.market_sneakers set chain_state = 'WITHDRAWING' where id = v_id;

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'BONUS_MINT', v_wallet, v_id, economy.op_deadline());
  return v_op;
end $$;

create or replace function public.sneaker_withdraw_request(p_sneaker_id bigint)
returns uuid
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_wallet text;
  v public.market_sneakers;
  v_op uuid := gen_random_uuid();
begin
  if not economy.shoe_chain_on() then
    raise exception '신발 꺼내기는 준비 중입니다' using errcode = '55000';
  end if;
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;

  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform pg_advisory_xact_lock(hashtext('chain:withdraw'));
  v_wallet := economy.withdraw_gate(v_user, true);

  v := economy.my_app_sneaker(v_user, p_sneaker_id);
  if not v.withdrawable or v.origin in ('IMPORT', 'MINT', 'STARTER') then
    raise exception '꺼낼 수 없는 신발입니다' using errcode = '22023';
  end if;
  if v.km_run < v.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 꺼낼 수 있습니다', round(v.lock_km - v.km_run, 1)
      using errcode = '23514';
  end if;
  if exists (select 1 from public.chain_jobs j
              where j.sneaker_id = p_sneaker_id and j.kind in ('VAULT_MINT', 'STATS_SYNC')
                and j.status in ('CLAIMED', 'SENT')) then
    raise exception '이 신발을 체인에 기록하는 중입니다. 잠시 뒤에 다시 해 주세요' using errcode = '55000';
  end if;
  if v.token_id is null and economy.mints_today('SNEAKER_WITHDRAW') >= economy.setting_num('mint_global_daily') then
    raise exception '오늘 발행 한도가 찼습니다. 내일 다시 해 주세요' using errcode = '23514';
  end if;

  update public.market_sneakers
     set chain_state = 'WITHDRAWING', equipped = false, updated_at = now()
   where id = p_sneaker_id;
  -- 신고 있던 신발을 꺼내면 첫 신발로 갈아 신긴다(신발 없이 달리는 틈이 없게)
  update public.market_sneakers set equipped = true, updated_at = now()
   where id = (select m.id from public.market_sneakers m
                where m.owner_id = v_user and m.origin = 'STARTER'
                  and m.chain_state = 'APP' and m.status = 'OWNED' limit 1)
     and not exists (select 1 from public.market_sneakers e where e.owner_id = v_user and e.equipped);

  insert into public.chain_ops (id, user_id, kind, wallet, sneaker_id, deadline)
  values (v_op, v_user, 'SNEAKER_WITHDRAW', v_wallet, p_sneaker_id, economy.op_deadline());
  return v_op;
end $$;

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
  v_eff_lo int; v_eff_hi int;
  v_cmf_lo int; v_cmf_hi int;
  v_id bigint;
  v_lock numeric := 0;
begin
  select n.o_nonce, n.o_digest into v_nonce, v_d from economy.draw_digest(p_user) n;

  -- 무료 뽑기 · 첫 신발은 무료 표, 상급(보너스 · 예전 유료)은 상급 표(0066). 속성은 없다(씨앗 4 ~ 5번째 바이트는 쓰지 않는다)
  v_rarity := economy.roll_rarity_v3(economy.bytes_int(v_d, 0, 4), economy.draw_kind(p_origin), p_min_rarity);
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
    p_user, 'NONE', v_rarity, v_variant, 1, 1,
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


-- ── 남은 내구도 ↔ 남은 비율 — 0059 그대로 + 포인트 1 = 기본의 1% ──────────

create or replace function economy.sneaker_durability_sync() returns trigger
language plpgsql as $$
begin
  if new.dur_base is null then
    new.dur_base := economy.durability_roll(new.rarity, hashtext('dur:' || new.id::text)::bigint);
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


-- ── 한 번만: 모든 신발 · SUP 를 비우고 새로 시작한다 (2026-10-17 사용자 결정) ──────────────
-- "지금 앱 설치하고 한 사람 우리 직원들밖에 없음. 그냥 싹 다 초기화해도 됨. SUP 도 초기화해."
--  · 지우기 전에 economy.backup_0066_* 에 옮겨 둔다(앱 · 워커는 읽지 못한다).
--  · 체인에 나가 있는(지갑 · 꺼내는 중) 신발이 있으면 멈춘다 — 그 신발은 서버에서 지워도 체인에 남는다.
--  · 러닝 기록 · 프로필 · 지갑 연결 · 크루 · 글은 그대로다. 러닝 기록의 신발 칸만 빈다.
--  · 새로 시작: 모든 계정에 첫 신발(일반 · 신은 채로) + 무료 뽑기 10회, 지갑을 연결한 계정은 상급 10회(첫 번 Genesis).
--  · economy_settings.shoe_economy_v3_reset 에 한 번 했다고 적는다 — setup.sql 을 다시 붙여넣어도 다시 비우지 않는다.
create or replace function economy.shoe_economy_v3_reset() returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  v_shoes int;
begin
  if exists (select 1 from public.market_sneakers where chain_state <> 'APP') then
    raise exception '체인에 나가 있는 신발이 있어 초기화하지 않습니다 — 먼저 확인하세요' using errcode = '55000';
  end if;

  create table if not exists economy.backup_0066_market_sneakers as table public.market_sneakers with no data;
  create table if not exists economy.backup_0066_sup_ledger as table public.sup_ledger with no data;
  create table if not exists economy.backup_0066_draw_grants as table public.draw_grants with no data;
  insert into economy.backup_0066_market_sneakers select * from public.market_sneakers;
  insert into economy.backup_0066_sup_ledger select * from public.sup_ledger;
  insert into economy.backup_0066_draw_grants select * from public.draw_grants;
  revoke all on economy.backup_0066_market_sneakers, economy.backup_0066_sup_ledger, economy.backup_0066_draw_grants
    from public, anon, authenticated;

  -- 신발과 함께 사라지는 것 — 거래소(매물 · 입찰 · 체결), 강화 기록, 신발 체인 일, 꺼내기 · 발행 예약
  delete from public.market_bids;
  delete from public.market_listings;
  delete from public.market_trades;
  delete from public.forge_requests;
  delete from public.chain_jobs where kind in ('VAULT_MINT', 'STATS_SYNC');
  delete from public.chain_ops where kind in ('SNEAKER_WITHDRAW', 'BONUS_MINT');
  delete from public.market_sneakers;
  get diagnostics v_shoes = row_count;

  -- SUP — 원장을 비우면 잔고가 0 이다. 같은 요청을 다시 보냈을 때 돌려주던 영수증도 비운다
  delete from public.sup_ledger;
  delete from economy.request_receipts;

  -- 뽑기 — 받은 수 · 쓴 수 · 오늘 쓴 수 · 러닝으로 모은 거리
  delete from public.draw_grants;
  delete from public.draw_daily;
  delete from public.premium_run_progress;

  -- 상급 뽑기는 상급 표 전체(레어부터)로 — 예전 운영 값(에픽 이상, 0045)을 비운다. 0045 는 "RARE" 만 "EPIC" 으로 바꾸므로 빈 값은 그대로 남는다
  update public.economy_settings set value = '""'::jsonb, updated_at = now() where key = 'premium_min_rarity';

  -- 새로 시작 — 첫 신발(신은 채로) · 무료 뽑기 10회 · 지갑을 연결한 계정은 상급 10회(첫 번 Genesis)
  insert into public.market_sneakers (
    owner_id, faction, rarity, variant, level, luck, comfort, durability,
    origin, efficiency_bps, comfort_bps, durability_pts, withdrawable, equipped
  )
  select p.id, 'NONE', 'COMMON', 0, 1, 1, 1.01, 100, 'STARTER', 300, 100, 100, false, true
    from public.profiles p
    join auth.users u on u.id = p.id;
  insert into public.draw_grants (user_id, kind, granted)
  select p.id, 'FREE', 10 from public.profiles p join auth.users u on u.id = p.id;
  insert into public.draw_grants (user_id, kind, granted, genesis_granted)
  select w.user_id, 'BONUS', 10, 1 from public.wallet_links w join auth.users u on u.id = w.user_id;

  return v_shoes;
end $$;
revoke all on function economy.shoe_economy_v3_reset() from public;

-- 표 검사는 비운 뒤에 다시 건다(예전 기준의 신발은 새 식에 맞지 않는다)
alter table public.market_sneakers drop constraint if exists market_sneakers_points_check;
do $$
begin
  insert into public.economy_settings (key, value) values ('shoe_economy_v3_reset', to_jsonb(now()::text))
  on conflict (key) do nothing;
  if found then
    perform economy.shoe_economy_v3_reset();
  end if;
end $$;


-- ── 표 검사 — 포인트는 등급 · 레벨 표, 내구도 포인트 1 = 기본의 1% (비운 뒤에 모두 검사한다) ──────────────
alter table public.market_sneakers drop constraint if exists market_sneakers_points_check;
alter table public.market_sneakers add constraint market_sneakers_points_check check (
  dur_base > 0
  and dur_max = dur_base + greatest((dur_base + 50) / 100, 1) * pts_dur
  and dur_now between 0 and dur_max
  and pts_eff >= 0 and pts_comfort >= 0 and pts_dur >= 0
  and pts_eff + pts_comfort + pts_dur <= economy.points_total_of(origin, rarity, level)
);


-- ── 포인트 나누기 — 0059 그대로 + 등급 · 레벨마다 다른 포인트 · 내구도 포인트 1 = 기본의 1% ───────────────────────────────────────────────────
/*
 * 나눈 뒤의 값(효율 · 착화감 · 내구도 포인트 합계)을 보낸다 — 더하기가 아니라 결과라서 같은 요청이 두 번 와도 두 번 더하지 않는다.
 * 줄이는 것은 받지 않는다. 다시 나누기가 남은 신발(pts_respec)만 한 번 줄일 수 있고, 줄이면 다시 나누기를 쓴 것이다.
 * 내구도 포인트 1 은 최대 · 남은 내구도를 함께 그 신발 기본 최대 내구도의 1% 올린다(줄이면 함께 내린다, 0066).
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

  v_total := economy.points_total_of(v.origin, v.rarity, v.level);
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
         dur_now = greatest(dur_now + economy.point_durability_of(v.dur_base) * (p_durability - v.pts_dur), 0),
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


-- ── 러닝 기록 — 0065 그대로 + 내구도가 다 닳은 뒤의 걸음은 적립하지 않는다 ──────────
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
  -- 모든 신발이 같은 빠르기로 — 달린 1분마다 남은 내구도 5 (0064, 0059 의 100m 마다 10 을 대신한다).
  -- 달린 시간은 멈춘 시간을 뺀 값(v_elapsed). 남은 비율(durability_pts)은 트리거가 다시 센다
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_distance_m > 0 then
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

