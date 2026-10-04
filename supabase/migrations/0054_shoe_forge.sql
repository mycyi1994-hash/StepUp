-- ════════════════════════════════════════════════════════════════════
--  0054 — 신발 강화(재료 신발 3개 · 확률) — 2026-10-01 사용자 지시서 v6, 재료 규칙은 2026-10-04 사용자 결정(forge-v2)
--
--  예전 강화(0023 sneaker_upgrade)는 SUP 를 받고 레벨을 하나 올렸다. 새 강화는 SUP 를 받지 않고,
--  내 신발 3개를 태워 확률로 레벨을 하나 올린다. 실패해도 재료 3개는 탄다.
--  예전 함수는 지우지 않는다(이미 깔린 앱이 부른다). 새 앱은 이 파일의 함수만 부른다.
--
--   재료           대상보다 한 등급 아래 · 같은 등급 · 더 높은 등급(두 등급 이상 아래는 받지 않는다). 일반 신발도 강화할 수 있다
--   최대 레벨      20(금고 v3 신발은 컨트랙트 상한과 20 중 작은 값 — StepUpSneakersV3.maxLevel)
--   기본 성공률    max(0, 1000 − 30 × 현재 레벨) (천분율, 80.4% = 804)
--   재료 한 개     step × k / 2 — step = 20 + 4 × (min(재료 레벨, 20) − 1), 재료 레벨은 1 이상(20 을 넘으면 20 으로 센다)
--                  k = 등급 차이(재료 − 대상) −1 → 2 · 0 → 3 · +1 → 4 · +2 → 5 · +3 이상 → 6 (step 이 늘 짝수라 정수로 나뉜다)
--   최종           min(1000, 기본 + 재료 3개의 합)
--   성공           레벨 +1. 효율 · 착화감은 레벨에서 계산되므로(economy.sneaker_effective) 원시 값은 그대로 둔다
--   실패           대상은 그대로
--
--  판정 · 소각 · 레벨 · 기록은 한 트랜잭션이다. 같은 요청 키(폰이 보내기 전에 저장)로 다시 부르면 같은
--  결과를 돌려주고 다시 태우지 않는다. 결과 조회(forge_result)가 아직 없는 키를 보면 그 키를 막아 둔다 —
--  늦게 도착한 요청이 "접수 안 됨"이라고 알려 준 뒤에 재료를 태우는 일이 없게.
--
--  소각: 주인을 비우고(계정 삭제와 같은 길 — economy.guard_owner_change) 상태를 BURNED 로 둔다. 체인에 기록된
--  신발(토큰 번호가 있거나 금고 발행 · 꺼내기가 진행 중)은 컨트랙트에 소각 기능이 없어 재료로 받지 않는다 —
--  DB 에서 숨긴 것을 NFT 소각이라고 할 수 없다.
-- ════════════════════════════════════════════════════════════════════

alter table public.market_sneakers drop constraint if exists market_sneakers_status_check;
alter table public.market_sneakers add constraint market_sneakers_status_check
  check (status in ('OWNED', 'LISTED', 'BURNED'));

create table if not exists public.forge_requests (
  user_id uuid not null references auth.users on delete cascade,
  request_key uuid not null,
  -- VOID: 결과 조회가 먼저 와서 막아 둔 키(접수되지 않았다)
  status text not null check (status in ('SUCCESS', 'FAILED', 'VOID')),
  target_id bigint references public.market_sneakers on delete set null,
  material_ids bigint[],
  rate_permille int check (rate_permille between 0 and 1000),
  roll int check (roll between 0 and 999),
  level_before int,
  level_after int,
  rule_version text,
  quote_version text,
  created_at timestamptz not null default now(),
  primary key (user_id, request_key)
);

comment on table public.forge_requests is
  '신발 강화 한 번 — 요청 키마다 한 줄. 판정 · 재료 소각 · 레벨과 같은 트랜잭션에 적힌다. 앱은 forge_result 로만 읽는다.';

alter table public.forge_requests enable row level security;
revoke all on public.forge_requests from anon, authenticated;

-- ── 규칙 ───────────────────────────────────────────────────────────

create or replace function economy.forge_rule_version() returns text
  language sql immutable as $$ select 'forge-v2'::text $$;

-- 강화 상한. 금고(v3)에 있는 신발은 컨트랙트가 등급별 상한(10 · 15 · 20 · 30)을 넘는 레벨 기록을 거절하므로 더 낮을 수 있다
create or replace function economy.forge_max_level(p_rarity text, p_token numeric) returns int
  language sql immutable as $$
  select case when p_token is not null and p_token >= economy.v3_first_token()
              then least(20, economy.max_level(p_rarity)) else 20 end
$$;

create or replace function economy.forge_base_permille(p_level int) returns int
  language sql immutable as $$ select greatest(0, 1000 - 30 * p_level) $$;

-- 재료 한 개의 보정(천분율) = step × k / 2. 두 등급 이상 아래 · 레벨 1 미만 · 모르는 등급은 0 — 그런 재료는 애초에 받지 않는다
create or replace function economy.forge_bonus_permille(p_target_rarity text, p_rarity text, p_level int) returns int
  language sql immutable as $$
  select case
    when p_level is null or p_level < 1 then 0
    when economy.rarity_ord(p_rarity) is null or economy.rarity_ord(p_target_rarity) is null then 0
    when economy.rarity_ord(p_rarity) - economy.rarity_ord(p_target_rarity) < -1 then 0
    else (20 + 4 * (least(p_level, 20) - 1))
         * case economy.rarity_ord(p_rarity) - economy.rarity_ord(p_target_rarity)
             when -1 then 2 when 0 then 3 when 1 then 4 when 2 then 5 else 6 end
         / 2
    end
$$;

-- 체인 쪽 일이 걸려 있는가 — 금고 발행 · 스탯 기록 대기, 꺼내기 진행 중
create or replace function economy.forge_chain_busy(p_id bigint) returns boolean
  language sql stable security definer set search_path = public as $$
  select exists (select 1 from public.chain_jobs j
                  where j.sneaker_id = p_id and j.kind = 'VAULT_MINT' and j.status in ('QUEUED', 'CLAIMED', 'SENT'))
      or exists (select 1 from public.chain_ops o
                  where o.sneaker_id = p_id and o.status in ('RESERVED', 'SIGNED', 'SUBMITTED'))
$$;
revoke all on function economy.forge_chain_busy(bigint) from public;

-- 강화 대상이 될 수 없는 까닭. null 이면 된다
create or replace function economy.forge_target_block(p_user uuid, t public.market_sneakers) returns text
  language plpgsql stable security definer set search_path = public, economy as $$
begin
  if t.id is null or t.owner_id is distinct from p_user then return 'TARGET_GONE'; end if;
  if t.status <> 'OWNED' or t.chain_state <> 'APP' then return 'TARGET_UNAVAILABLE'; end if;
  if t.origin in ('IMPORT', 'MINT') then return 'LEGACY'; end if;
  if t.level >= economy.forge_max_level(t.rarity, t.token_id) then return 'MAX_LEVEL'; end if;
  return null;
end $$;
revoke all on function economy.forge_target_block(uuid, public.market_sneakers) from public;

-- 재료가 될 수 없는 까닭. null 이면 된다
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
  -- 레벨 20 을 넘은 예전 신발은 막지 않는다(보정은 20 으로 센다)
  if m.level is null or m.level < 1 then return 'LEVEL'; end if;
  return null;
end $$;
revoke all on function economy.forge_material_block(uuid, public.market_sneakers, public.market_sneakers) from public;

-- 대상 한 켤레의 지금 값(실효 효율 · 착화감 포함)
create or replace function economy.forge_target_json(t public.market_sneakers, p_level int) returns jsonb
  language sql stable security definer set search_path = public, economy as $$
  select jsonb_build_object(
    'id', t.id, 'rarity', t.rarity, 'level', p_level,
    'max_level', economy.forge_max_level(t.rarity, t.token_id),
    'efficiency_bps', e.efficiency_bps, 'comfort_bps', e.comfort_bps)
    from economy.sneaker_effective(t.origin, t.rarity, p_level, t.efficiency_bps, t.comfort_bps, t.durability_pts) e
$$;
revoke all on function economy.forge_target_json(public.market_sneakers, int) from public;

-- ── 앱이 부르는 것 ──────────────────────────────────────────────────

/*
 * 강화 화면을 열 때 — 대상의 상태와 재료 후보(대상보다 한 등급 아래 이상인 내 신발 전부, 쓸 수 없는 것은 까닭과 함께).
 * 쓸 수 있는 것이 먼저, 그 안에서는 보정이 큰 것부터.
 *   { target: {id, rarity, level, max_level, efficiency_bps, comfort_bps, base_permille, block},
 *     materials: [{id, rarity, level, model_id, faction, variant, mint_number, bonus_permille, block}] }
 */
create or replace function public.forge_materials(p_target bigint)
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  t public.market_sneakers;
  v_block text;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  select * into t from public.market_sneakers where id = p_target;
  v_block := economy.forge_target_block(v_user, t);
  if v_block = 'TARGET_GONE' then
    return jsonb_build_object('target', jsonb_build_object('id', p_target, 'block', v_block), 'materials', '[]'::jsonb);
  end if;
  return jsonb_build_object(
    'target', economy.forge_target_json(t, t.level)
              || jsonb_build_object('base_permille', economy.forge_base_permille(t.level), 'block', v_block),
    'materials', coalesce((
      select jsonb_agg(jsonb_build_object(
               'id', c.id, 'rarity', c.rarity, 'level', c.level, 'model_id', c.model_id,
               'faction', c.faction, 'variant', c.variant, 'mint_number', c.mint_number,
               'bonus_permille', c.bonus, 'block', c.block)
             order by (c.block is null) desc, c.bonus desc, economy.rarity_ord(c.rarity), c.level desc, c.mint_number, c.id)
        from (select m.*, economy.forge_bonus_permille(t.rarity, m.rarity, m.level) as bonus,
                     economy.forge_material_block(v_user, m, t) as block
                from public.market_sneakers m
               where m.owner_id = v_user and m.id <> t.id
                 and economy.rarity_ord(m.rarity) >= economy.rarity_ord(t.rarity) - 1) c
    ), '[]'::jsonb));
end $$;

-- 견적 계산(잠그지 않는다 — 부르는 쪽이 필요하면 잠근다)
create or replace function economy.forge_quote_for(p_user uuid, p_target bigint, p_materials bigint[])
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  t public.market_sneakers;
  m public.market_sneakers;
  v_block text;
  v_bad bigint[] := '{}';
  v_rate int;
  v_bonus int := 0;
  v_items jsonb := '[]'::jsonb;
  v_sig text;
  v_id bigint;
begin
  if p_materials is null or cardinality(p_materials) <> 3 or array_position(p_materials, null) is not null
     or (select count(distinct x) from unnest(p_materials) x) <> 3 then
    return jsonb_build_object('error', 'MATERIALS_INVALID');
  end if;
  select * into t from public.market_sneakers where id = p_target;
  v_block := economy.forge_target_block(p_user, t);
  if v_block is not null then
    return jsonb_build_object('error', v_block);
  end if;
  v_sig := t.id || ':' || t.level || ':' || t.rarity;
  foreach v_id in array (select array_agg(x order by x) from unnest(p_materials) x) loop
    select * into m from public.market_sneakers where id = v_id;
    if not found then
      m := null;
    end if;
    if economy.forge_material_block(p_user, m, t) is not null then
      v_bad := v_bad || v_id;
    else
      v_bonus := v_bonus + economy.forge_bonus_permille(t.rarity, m.rarity, m.level);
      v_items := v_items || jsonb_build_object('id', m.id,
        'bonus_permille', economy.forge_bonus_permille(t.rarity, m.rarity, m.level));
      v_sig := v_sig || '|' || m.id || ':' || m.level || ':' || m.rarity;
    end if;
  end loop;
  if cardinality(v_bad) > 0 then
    return jsonb_build_object('error', 'MATERIAL_UNAVAILABLE', 'unavailable', to_jsonb(v_bad));
  end if;
  v_rate := least(1000, economy.forge_base_permille(t.level) + v_bonus);
  return jsonb_build_object(
    'rate_permille', v_rate,
    'base_permille', economy.forge_base_permille(t.level),
    'materials', v_items,
    'before', economy.forge_target_json(t, t.level),
    'after', economy.forge_target_json(t, t.level + 1),
    'rule_version', economy.forge_rule_version(),
    'quote_version', md5(economy.forge_rule_version() || '|' || v_sig || '|' || v_rate));
end $$;
revoke all on function economy.forge_quote_for(uuid, bigint, bigint[]) from public;

/*
 * 실행 전 견적 — 서버가 실제로 판정할 확률과 성공 시 값. 앱은 이 확률을 그대로 보이고 quote_version 을 실행에 실어 보낸다.
 * 거절이면 { error: TARGET_GONE · TARGET_UNAVAILABLE · LEGACY · MAX_LEVEL · MATERIALS_INVALID ·
 * MATERIAL_UNAVAILABLE(unavailable: [id]) }.
 */
create or replace function public.forge_quote(p_target bigint, p_materials bigint[])
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return economy.forge_quote_for(v_user, p_target, p_materials);
end $$;

-- 기록된 한 번의 결과
create or replace function economy.forge_result_json(r public.forge_requests) returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare t public.market_sneakers;
begin
  if r.status = 'VOID' then
    return jsonb_build_object('status', 'NOT_ACCEPTED', 'request_key', r.request_key);
  end if;
  select * into t from public.market_sneakers where id = r.target_id;
  return jsonb_build_object(
    'status', r.status, 'request_key', r.request_key, 'target_id', r.target_id,
    'material_ids', to_jsonb(r.material_ids), 'rate_permille', r.rate_permille,
    'level_before', r.level_before, 'level_after', r.level_after,
    'target', case when t.id is null then null else economy.forge_target_json(t, r.level_after) end);
end $$;
revoke all on function economy.forge_result_json(public.forge_requests) from public;

/*
 * 강화 한 번. [p_key] 는 폰이 보내기 전에 저장한 요청 키 — 같은 키로 다시 부르면 기록된 결과를 그대로 돌려준다.
 * 견적이 확인한 것(p_quote)과 달라졌으면 { error: QUOTE_CHANGED, quote } — 아무것도 태우지 않는다.
 * 결과: { status: SUCCESS | FAILED, ... } · 막아 둔 키면 { status: NOT_ACCEPTED } · 거절이면 { error }.
 */
create or replace function public.forge_start(p_key uuid, p_target bigint, p_materials bigint[], p_quote text)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  r public.forge_requests;
  v_quote jsonb;
  v_rate int;
  v_roll int;
  v_success boolean;
  v_level int;
  v_sorted bigint[];
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_key is null then
    return jsonb_build_object('error', 'KEY_REQUIRED');
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select * into r from public.forge_requests where user_id = v_user and request_key = p_key;
  if found then
    if r.status <> 'VOID' and (r.target_id is distinct from p_target
        or r.material_ids is distinct from (select array_agg(x order by x) from unnest(p_materials) x)) then
      return jsonb_build_object('error', 'KEY_REUSED');
    end if;
    return economy.forge_result_json(r);
  end if;

  -- 대상 · 재료를 잠그고 다시 본다(같은 사람의 다른 요청은 위 잠금에서 줄을 선다)
  perform 1 from public.market_sneakers where id = p_target or id = any(p_materials) for update;
  v_quote := economy.forge_quote_for(v_user, p_target, p_materials);
  if v_quote ? 'error' then
    return v_quote;
  end if;
  if p_quote is distinct from v_quote ->> 'quote_version' then
    return jsonb_build_object('error', 'QUOTE_CHANGED', 'quote', v_quote);
  end if;

  v_rate := (v_quote ->> 'rate_permille')::int;
  v_roll := (economy.bytes_int(economy.random_bytes32(), 0, 6) % 1000)::int;
  v_success := v_roll < v_rate;
  v_level := (v_quote -> 'before' ->> 'level')::int;
  v_sorted := (select array_agg(x order by x) from unnest(p_materials) x);

  -- 재료 소각 — 성공 · 실패 모두
  update public.market_sneakers
     set owner_id = null, status = 'BURNED', equipped = false, updated_at = now()
   where id = any(v_sorted);
  if v_success then
    update public.market_sneakers set level = level + 1, updated_at = now() where id = p_target;
  end if;

  insert into public.forge_requests (user_id, request_key, status, target_id, material_ids, rate_permille, roll,
                                     level_before, level_after, rule_version, quote_version)
  values (v_user, p_key, case when v_success then 'SUCCESS' else 'FAILED' end, p_target, v_sorted, v_rate, v_roll,
          v_level, v_level + case when v_success then 1 else 0 end,
          economy.forge_rule_version(), v_quote ->> 'quote_version')
  returning * into r;
  return economy.forge_result_json(r);
end $$;

/*
 * 같은 요청 키의 결과. 아직 기록이 없으면 그 키를 막아 두고 { status: NOT_ACCEPTED } — 그 뒤에 도착한 같은 키의
 * 요청은 아무것도 하지 않는다. 그래서 NOT_ACCEPTED 는 "재료가 타지 않았다"가 확정된 말이다.
 */
create or replace function public.forge_result(p_key uuid)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  r public.forge_requests;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_key is null then
    return jsonb_build_object('error', 'KEY_REQUIRED');
  end if;
  -- 실행 중인 같은 사람의 강화가 끝날 때까지 기다린다
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  select * into r from public.forge_requests where user_id = v_user and request_key = p_key;
  if not found then
    insert into public.forge_requests (user_id, request_key, status) values (v_user, p_key, 'VOID')
    returning * into r;
  end if;
  return economy.forge_result_json(r);
end $$;

revoke all on function public.forge_materials(bigint) from public, anon;
revoke all on function public.forge_quote(bigint, bigint[]) from public, anon;
revoke all on function public.forge_start(uuid, bigint, bigint[], text) from public, anon;
revoke all on function public.forge_result(uuid) from public, anon;
grant execute on function public.forge_materials(bigint) to authenticated;
grant execute on function public.forge_quote(bigint, bigint[]) to authenticated;
grant execute on function public.forge_start(uuid, bigint, bigint[], text) to authenticated;
grant execute on function public.forge_result(uuid) to authenticated;
