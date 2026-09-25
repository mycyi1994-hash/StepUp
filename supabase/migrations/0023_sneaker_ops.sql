-- 신발 — 뽑기 · 강화 · 수리 · 착용을 서버가 한다.
--
-- 폰이 난수를 굴리면 원하는 결과가 나올 때까지 다시 굴릴 수 있다. 서버가 굴리고,
-- 결과를 미리 봉해 둔 씨앗(seed)에서 뽑아 나중에 누구든 확인할 수 있게 한다.
--
--   뽑기 공정성: 사람마다 비밀 씨앗이 있고, 그 해시(seed_hash)는 미리 공개된다.
--   n 번째 뽑기의 결과는 sha256(씨앗 || ':' || n) 에서 정해진다. 씨앗을 바꿀 때
--   옛 씨앗을 공개하므로, 공개된 해시와 맞는지·결과가 맞는지 누구나 다시 계산할
--   수 있다. 서버도 결과를 고를 수 없다 — 해시를 먼저 공개했기 때문이다.
--
--   다시 뽑기 막기: 값을 치르는 것과 결과를 정하는 것이 한 트랜잭션이다. 결과를
--   보고 값을 안 치르는 길이 없다.

-- ══════════════════════════════════════════════════════════════════
-- 씨앗
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.draw_seeds (
  user_id uuid primary key references auth.users on delete cascade,
  seed bytea not null,
  seed_hash text not null,
  -- 이 씨앗으로 다음에 뽑을 번호
  nonce bigint not null default 0,
  first_nonce bigint not null default 0,
  created_at timestamptz not null default now()
);

comment on table public.draw_seeds is
  '뽑기 씨앗. 씨앗 자체는 아무도 못 읽는다(앱 권한 없음). 해시만 draw_fairness 로 공개된다.';

alter table public.draw_seeds enable row level security;
revoke all on public.draw_seeds from anon, authenticated;

create table if not exists public.draw_seed_reveals (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  seed_hex text not null,
  seed_hash text not null,
  first_nonce bigint not null,
  last_nonce bigint not null,
  revealed_at timestamptz not null default now()
);

alter table public.draw_seed_reveals enable row level security;
revoke all on public.draw_seed_reveals from anon, authenticated;
drop policy if exists draw_seed_reveals_select_own on public.draw_seed_reveals;
create policy draw_seed_reveals_select_own on public.draw_seed_reveals
  for select using ((select auth.uid()) = user_id);
grant select on public.draw_seed_reveals to authenticated;

-- 강한 난수 32바이트. gen_random_uuid 는 운영체제의 강한 난수로 만든다.
create or replace function economy.random_bytes32() returns bytea
  language sql volatile as $$
  select decode(replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', ''), 'hex')
$$;

create or replace function economy.seed_ensure(p_user uuid) returns void
language plpgsql security definer set search_path = public, economy as $$
declare v_seed bytea;
begin
  if exists (select 1 from public.draw_seeds where user_id = p_user) then
    return;
  end if;
  v_seed := economy.random_bytes32();
  insert into public.draw_seeds (user_id, seed, seed_hash)
  values (p_user, v_seed, encode(sha256(v_seed), 'hex'))
  on conflict (user_id) do nothing;
end $$;
revoke all on function economy.seed_ensure(uuid) from public;

-- 다음 뽑기의 32바이트. 번호를 하나 올린다. 부르는 쪽이 이 사람의 잠금을 잡고 있어야 한다.
create or replace function economy.draw_digest(p_user uuid, out o_nonce bigint, out o_digest bytea)
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.seed_ensure(p_user);
  update public.draw_seeds d
     set nonce = d.nonce + 1
   where d.user_id = p_user
  returning d.nonce - 1, sha256(d.seed || convert_to(':' || (d.nonce - 1)::text, 'UTF8'))
    into o_nonce, o_digest;
end $$;
revoke all on function economy.draw_digest(uuid) from public;

-- 바이트 몇 개를 음이 아닌 정수로
create or replace function economy.bytes_int(p_b bytea, p_off int, p_len int) returns bigint
  language plpgsql immutable as $$
declare v bigint := 0; i int;
begin
  for i in 0 .. p_len - 1 loop
    v := v * 256 + get_byte(p_b, p_off + i);
  end loop;
  return v;
end $$;

-- 씨앗 결과 → 등급. p_min 이 있으면 그 등급 이상만(예: Genesis 는 EPIC 이상).
-- 가중치 표 안에서 굴리므로 제한이 있어도 위 등급끼리의 비율(13:4)은 그대로다.
create or replace function economy.roll_rarity(p_roll bigint, p_min text default null) returns text
  language plpgsql immutable as $$
declare
  v_order text[] := array['COMMON', 'RARE', 'EPIC', 'LEGENDARY'];
  v_from int := coalesce(economy.rarity_ord(p_min), 0) + 1;
  v_total int := 0;
  v_pick int;
  i int;
begin
  for i in v_from .. 4 loop
    v_total := v_total + economy.rarity_weight(v_order[i]);
  end loop;
  v_pick := (p_roll % v_total)::int;
  for i in v_from .. 4 loop
    v_pick := v_pick - economy.rarity_weight(v_order[i]);
    if v_pick < 0 then
      return v_order[i];
    end if;
  end loop;
  return v_order[4];
end $$;

/*
 * 뽑은 결과로 신발을 만든다. 값은 부르는 쪽이 이미 치렀다.
 *
 *   FREE_DRAW   이 신발로 50km 를 달려야 꺼낼 수 있다(무료 농사 막기)
 *   PAID_DRAW   바로 꺼낼 수 있다
 *   BONUS_DRAW  체인에서 발행된다. 50km 전에는 전송 잠금
 *   STARTER     꺼낼 수 없다
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
    lock_km, withdrawable, draw_nonce, genesis_no
  ) values (
    p_user, v_faction, v_rarity, v_variant, 1, 1,
    -- 옛 comfort 칸(배율 1.00~1.40)은 옛 앱 화면용으로만 채운다.
    1 + v_cmf_lo / 10000.0, 100,
    p_origin,
    v_eff_lo + (economy.bytes_int(v_d, 8, 2) % (v_eff_hi - v_eff_lo + 1))::int,
    v_cmf_lo + (economy.bytes_int(v_d, 10, 2) % (v_cmf_hi - v_cmf_lo + 1))::int,
    100,
    v_lock, p_origin <> 'STARTER', v_nonce,
    case when p_genesis then nextval('public.genesis_seq')::int end
  ) returning id into v_id;

  return v_id;
end $$;
revoke all on function economy.draw_create(uuid, text, text, boolean) from public;

-- ══════════════════════════════════════════════════════════════════
-- 뽑기 권리 — 신규 무료 10회 · 지갑 보너스 10회(Genesis 1)
-- ══════════════════════════════════════════════════════════════════
create table if not exists public.draw_grants (
  user_id uuid not null references auth.users on delete cascade,
  kind text not null check (kind in ('FREE', 'BONUS')),
  granted int not null check (granted >= 0),
  used int not null default 0,
  genesis_granted int not null default 0,
  genesis_used int not null default 0,
  created_at timestamptz not null default now(),
  primary key (user_id, kind),
  check (used between 0 and granted),
  check (genesis_used between 0 and genesis_granted)
);

alter table public.draw_grants enable row level security;
revoke all on public.draw_grants from anon, authenticated;
drop policy if exists draw_grants_select_own on public.draw_grants;
create policy draw_grants_select_own on public.draw_grants
  for select using ((select auth.uid()) = user_id);
grant select on public.draw_grants to authenticated;

-- 첫 신발은 한 사람에 하나
create unique index if not exists market_sneakers_one_starter
  on public.market_sneakers (owner_id) where origin = 'STARTER';

/*
 * 앱을 켤 때 부른다. 여러 번 불러도 한 번만 일어난다.
 *   - 서버 신발이 없으면 첫 신발(바람 · 일반)을 주고 신긴다
 *   - 무료 뽑기 시작 시각 뒤에 가입했으면 무료 뽑기 10회
 */
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
      v_user, 'WIND', 'COMMON', 0, 1, 1, 1.01, 100,
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

revoke all on function public.economy_bootstrap() from public, anon;
grant execute on function public.economy_bootstrap() to authenticated;

-- 무료 뽑기 한 번. 남은 횟수가 없으면 23514.
create or replace function public.draw_free()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  update public.draw_grants set used = used + 1
   where user_id = v_user and kind = 'FREE' and used < granted;
  if not found then
    raise exception '무료 뽑기가 남아 있지 않습니다' using errcode = '23514';
  end if;

  return economy.draw_create(v_user, 'FREE_DRAW');
end $$;

-- SUP 로 뽑기. 치르는 것과 굴리는 것이 한 번에 일어난다.
create or replace function public.draw_paid()
returns bigint
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.ledger_apply(v_user, 'SPEND_DRAW', -economy.draw_cost(), '신발 뽑기');
  return economy.draw_create(v_user, 'PAID_DRAW');
end $$;

revoke all on function public.draw_free() from public, anon;
revoke all on function public.draw_paid() from public, anon;
grant execute on function public.draw_free() to authenticated;
grant execute on function public.draw_paid() to authenticated;

-- 공정성 — 지금 씨앗의 해시와 다음 번호
create or replace function public.draw_fairness()
returns table (seed_hash text, next_nonce bigint, first_nonce bigint)
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform economy.seed_ensure(v_user);
  return query select d.seed_hash, d.nonce, d.first_nonce from public.draw_seeds d where d.user_id = v_user;
end $$;

-- 씨앗을 바꾸고 옛 씨앗을 공개한다. 공개된 씨앗으로 지난 뽑기를 다시 계산해 볼 수 있다.
create or replace function public.draw_rotate_seed()
returns table (seed_hex text, seed_hash text, first_nonce bigint, last_nonce bigint)
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_old public.draw_seeds;
  v_new bytea := economy.random_bytes32();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.seed_ensure(v_user);

  select * into v_old from public.draw_seeds where user_id = v_user for update;
  insert into public.draw_seed_reveals (user_id, seed_hex, seed_hash, first_nonce, last_nonce)
  values (v_user, encode(v_old.seed, 'hex'), v_old.seed_hash, v_old.first_nonce, v_old.nonce - 1);

  -- 번호는 이어서 센다. 씨앗이 바뀌어도 번호가 겹치지 않아 신발의 draw_nonce 로 어느 씨앗인지 안다.
  update public.draw_seeds
     set seed = v_new, seed_hash = encode(sha256(v_new), 'hex'), first_nonce = nonce, created_at = now()
   where user_id = v_user;

  return query select encode(v_old.seed, 'hex'), v_old.seed_hash, v_old.first_nonce, v_old.nonce - 1;
end $$;

revoke all on function public.draw_fairness() from public, anon;
revoke all on function public.draw_rotate_seed() from public, anon;
grant execute on function public.draw_fairness() to authenticated;
grant execute on function public.draw_rotate_seed() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 강화 · 수리 · 착용
-- ══════════════════════════════════════════════════════════════════

-- 내 신발을 잠그고 가져온다. 앱에서 쓸 수 있는 상태가 아니면 거절.
create or replace function economy.my_app_sneaker(p_user uuid, p_id bigint)
returns public.market_sneakers
language plpgsql security definer set search_path = public as $$
declare v public.market_sneakers;
begin
  select * into v from public.market_sneakers where id = p_id for update;
  if not found or v.owner_id is distinct from p_user then
    raise exception '내 신발이 아닙니다' using errcode = '42501';
  end if;
  if v.chain_state <> 'APP' then
    raise exception '체인에 있는 신발입니다. 먼저 앱으로 넣어 주세요' using errcode = '22023';
  end if;
  if v.status <> 'OWNED' then
    raise exception '판매 중인 신발입니다' using errcode = '22023';
  end if;
  return v;
end $$;
revoke all on function economy.my_app_sneaker(uuid, bigint) from public;

create or replace function public.sneaker_upgrade(p_id bigint)
returns int
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  v := economy.my_app_sneaker(v_user, p_id);

  -- 폰에서 올린 신발은 레벨을 서버가 믿지 않으므로 서버에서 강화해도 효과가 없다.
  -- 효과 없는 곳에 SUP 를 받지 않는다.
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 강화할 수 없습니다' using errcode = '22023';
  end if;
  if v.level >= economy.max_level(v.rarity) then
    raise exception '최대 레벨입니다' using errcode = '23514';
  end if;

  perform economy.ledger_apply(v_user, 'SPEND_UPGRADE', -economy.upgrade_cost(v.rarity, v.level),
                               format('신발 강화 #%s Lv%s→%s', v.mint_number, v.level, v.level + 1));
  update public.market_sneakers set level = level + 1, updated_at = now() where id = p_id;
  return v.level + 1;
end $$;

-- 수리. p_points 를 비우면 가득 채운다. 치른 SUP 는 누구에게도 가지 않는다(소각).
create or replace function public.sneaker_repair(p_id bigint, p_points numeric default null)
returns numeric
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v public.market_sneakers;
  v_points numeric;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  v := economy.my_app_sneaker(v_user, p_id);

  v_points := least(coalesce(p_points, 100), 100 - v.durability_pts);
  if v_points is null or v_points <= 0 then
    raise exception '고칠 곳이 없습니다' using errcode = '23514';
  end if;
  v_points := round(v_points, 2);

  perform economy.ledger_apply(v_user, 'SPEND_REPAIR',
    -round(v_points * economy.repair_cost_per_point(v.rarity, v.level), 4),
    format('신발 수리 #%s +%s', v.mint_number, v_points));
  update public.market_sneakers
     set durability_pts = least(durability_pts + v_points, 100),
         durability = least(durability_pts + v_points, 100)::int,
         updated_at = now()
   where id = p_id;
  return least(v.durability_pts + v_points, 100);
end $$;

create or replace function public.sneaker_equip(p_id bigint)
returns void
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  perform economy.my_app_sneaker(v_user, p_id);
  update public.market_sneakers set equipped = false where owner_id = v_user and equipped and id <> p_id;
  update public.market_sneakers set equipped = true, updated_at = now() where id = p_id;
end $$;

revoke all on function public.sneaker_upgrade(bigint) from public, anon;
revoke all on function public.sneaker_repair(bigint, numeric) from public, anon;
revoke all on function public.sneaker_equip(bigint) from public, anon;
grant execute on function public.sneaker_upgrade(bigint) to authenticated;
grant execute on function public.sneaker_repair(bigint, numeric) to authenticated;
grant execute on function public.sneaker_equip(bigint) to authenticated;

-- 내 신발 — 화면에 필요한 실효 스탯까지
create or replace function public.my_sneakers()
returns table (
  id bigint, faction text, rarity text, variant int, level int, max_level int,
  efficiency_bps int, comfort_bps int, durability numeric,
  equipped boolean, origin text, chain_state text, status text,
  mint_number bigint, genesis_no int, token_id numeric,
  km_run numeric, lock_km numeric, can_withdraw boolean,
  upgrade_cost numeric, repair_cost_per_point numeric
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
         economy.repair_cost_per_point(s.rarity, s.level)
    from public.market_sneakers s,
         lateral economy.sneaker_effective(s.origin, s.rarity, s.level,
           s.efficiency_bps, s.comfort_bps, s.durability_pts) e
   where s.owner_id = v_user
   order by s.equipped desc, s.id;
end $$;

revoke all on function public.my_sneakers() from public, anon;
grant execute on function public.my_sneakers() to authenticated;

-- 첫 신발은 팔 수 없고, 무료로 받은 신발은 잠금 거리를 채우기 전에는 팔 수 없다.
-- 무료 신발을 팔 수 있으면 계정을 여러 개 만들어 무료 뽑기를 SUP 로 바꾼다.
create or replace function economy.guard_listing() returns trigger
language plpgsql security definer set search_path = public as $$
declare v public.market_sneakers;
begin
  select * into v from public.market_sneakers where id = new.sneaker_id;
  if v.chain_state <> 'APP' or v.equipped then
    raise exception '착용 중이거나 체인에 있는 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.origin = 'STARTER' then
    raise exception '첫 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.origin in ('IMPORT', 'MINT') then
    raise exception '예전 신발은 팔 수 없습니다' using errcode = '22023';
  end if;
  if v.km_run < v.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 팔 수 있습니다', round(v.lock_km - v.km_run, 1)
      using errcode = '22023';
  end if;
  return new;
end $$;

/*
 * 주인이 바뀌는 모든 길을 한곳에서 막는다.
 *
 * 매물 표 트리거(guard_listing)만으로는 부족하다. 즉시 판매(market_sell_now)와
 * 매물을 거는 순간 맞는 입찰이 있을 때의 체결은 매물 줄을 만들지 않고 바로
 * market_settle 로 주인을 바꾼다. 그래서 신발 표 자체에서 본다.
 *
 * 주인이 바뀌어도 되는 경우:
 *   - 앱 안에 있고(APP), 신고 있지 않고, 첫 신발 · 예전(폰) 신발이 아니고, 잠금 거리를 채운 신발
 *   - 체인에서 넣은 신발(ON_CHAIN → APP) — 어테스터의 넣기 처리만 이 길을 연다
 *   - 계정 삭제로 주인이 비워질 때
 */
create or replace function economy.guard_owner_change() returns trigger
language plpgsql security definer set search_path = public as $$
begin
  if new.owner_id is not distinct from old.owner_id then
    return new;
  end if;
  if new.owner_id is null then
    new.equipped := false;
    return new;
  end if;
  if old.chain_state = 'ON_CHAIN' and new.chain_state = 'APP'
     and current_setting('stepup.chain_deposit', true) = 'on' then
    new.equipped := false;
    return new;
  end if;
  -- 주인이 계정을 지워 비어 있는 신발은 아무에게도 넘어가지 않는다(체인에서 넣은 경우만 위에서 허락).
  if old.owner_id is null then
    raise exception '주인이 없는 신발입니다' using errcode = '42501';
  end if;

  if old.chain_state <> 'APP' or new.chain_state <> 'APP' then
    raise exception '체인에 있는 신발은 앱에서 넘길 수 없습니다' using errcode = '22023';
  end if;
  if old.equipped then
    raise exception '신고 있는 신발은 넘길 수 없습니다' using errcode = '22023';
  end if;
  if old.origin in ('STARTER', 'IMPORT', 'MINT') then
    raise exception '넘길 수 없는 신발입니다' using errcode = '22023';
  end if;
  if old.km_run < old.lock_km then
    raise exception '이 신발로 %km 를 더 달려야 넘길 수 있습니다', round(old.lock_km - old.km_run, 1)
      using errcode = '22023';
  end if;
  new.equipped := false;
  return new;
end $$;

drop trigger if exists market_sneakers_owner_guard on public.market_sneakers;
create trigger market_sneakers_owner_guard before update of owner_id on public.market_sneakers
  for each row execute function economy.guard_owner_change();
