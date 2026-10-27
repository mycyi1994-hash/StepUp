-- ════════════════════════════════════════════════════════════════════
--  0078 — 체인 기록 넓히기: 게임 · 러닝 · 크루 · 매일 활동 + 나중에 대조 + 개인정보 최소 (2026-10-27, GIWA 테스트넷)
--
--  사용자 결정 "거래수가중요해" → "검증은 실시간이 아니라 나중에 필요하면 대조할 수 있을 정도" → "해시에 개인정보 유출 없게".
--  원칙은 0044 그대로: 사용자가 실제로 한 일 하나 = 체인 기록 하나, 서버에 적힌 것만, 위치 · 이름 · 계정 번호는 올리지 않는다.
--  체인 기록이 실패해도 그 일(뽑기 · 강화 · 러닝 …)은 그대로 된다. 체인 기록을 통과해야 되는 일은 없다(관문이 아니다).
--
--  새 기록(모두 EAS 증명 — 어테스터 워커 attester/src/eas.js ACTIVITY_SCHEMAS 의 같은 이름 스키마):
--    게임    SHOE_DRAW(뽑기) · SHOE_FORGE(강화) · SHOE_REPAIR(수리) · SHOE_POINTS(포인트 나누기) · SHOE_TRADE(거래소 체결)
--            DRAW_SEED(뽑기 씨앗 약속 — 씨앗 해시를 바로 올린다) · DRAW_REVEAL(씨앗 공개 — 누구나 draw_verify 로 다시 굴려 본다)
--    러닝    RUN_SETTLE(러닝 정산 — 거리 · 시간 · 판정 · 받은 포인트) · SHOE_WEAR(신발 주행 기록 — 그 러닝 거리 · 누적 거리 · 남은 내구도)
--    크루    CREW_RUN(크루 러닝 완주) · FLASH_RUN(번개 출석, 둘 이상) · CREW_JOIN(크루 가입) · PARTY_START(함께 출발 명단, 방마다)
--    매일    DAILY_STEPS(목표 달성 여부만 — 걸음 수는 올리지 않는다) · CHECK_IN(앱 출석)
--  그리고 러닝 증명(RUN_PROOF)의 기준을 1km → 200m, 한 사람 하루 상한을 10 → 30 으로(한 번만 — 관리자가 바꾼 값은 두고).
--
--  나중에 대조 — 새 기록마다 dataHash = sha256(기록마다 다른 무작위 소금 ‖ 원본 스냅숏)을 함께 올린다.
--    스냅숏 · 소금은 서버(economy.chain_audit)에만 둔다. 의심이 생기면 admin_chain_audit 로 꺼내 체인 값과 맞대어 본다
--    (attester/scripts/audit.mjs). 소금이 있어 걸음 수 · 경로 같은 원본을 추측해 맞춰 볼 수 없다.
--  개인정보 최소:
--    · 사람 가명은 사람마다 따로 둔 비밀값으로 만든다(economy.chain_person_keys). 계정을 지우면 그 값도 지워져
--      우리조차 그 사람의 체인 기록을 다시 그 사람과 잇지 못한다(사실상 지움).
--    · 받는 사람(recipient)은 기본 비움(0). 원하는 사람만 "내 지갑으로 받기"를 켠다(chain_recipient_set) — 지갑 하나로 활동이 묶이지 않게.
--    · 보내는 시각을 무작위로 10 ~ 60분 늦춘다(chain_jitter_*) — 거래 시각으로 생활 패턴(몇 시에 달리는지)이 드러나지 않게.
--      걸음 · 출석은 하루가 끝난 뒤, 씨앗 약속 · 공개는 바로(뽑기 기록보다 먼저 올라가야 약속이 된다).
--
--  막는 것: 종류마다 한 사람 하루 상한(chain_activity_daily), 전체 하루 상한(chain_jobs_global_daily)은 그대로.
--  사용자 요청(로그인한 사람의 RPC) 안에서 생긴 것만 — 배포 · 관리자 작업이 표를 고쳐도 기록을 만들지 않는다(씨앗 · 러닝 표는 사용자만 쓴다).
--
--  운영 값(economy_settings):
--    chain_activity_enabled  false 면 새 기록을 줄 세우지 않는다(이미 선 것은 나간다)
--    chain_activity_daily    {"SHOE_DRAW": 50, …} 종류마다 한 사람 하루 상한(0 이면 그 종류는 끈다)
--    chain_jitter_min_min · chain_jitter_max_min  보내는 시각을 늦추는 범위(분)
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('chain_activity_enabled', 'true'::jsonb),
  ('chain_activity_daily', '{"SHOE_DRAW": 50, "SHOE_FORGE": 30, "SHOE_REPAIR": 20, "SHOE_POINTS": 20, "SHOE_TRADE": 30,
                             "DRAW_SEED": 5, "DRAW_REVEAL": 5, "RUN_SETTLE": 30, "SHOE_WEAR": 30,
                             "CREW_RUN": 3, "FLASH_RUN": 5, "CREW_JOIN": 3, "PARTY_START": 10,
                             "DAILY_STEPS": 1, "CHECK_IN": 1}'::jsonb),
  ('chain_jitter_min_min', '10'::jsonb),
  ('chain_jitter_max_min', '60'::jsonb)
on conflict (key) do nothing;

-- 러닝 증명 200m · 하루 30 — 한 번만(setup.sql 을 다시 붙여넣어도 관리자가 바꾼 값을 되돌리지 않게)
do $$ begin
  if not exists (select 1 from public.economy_settings where key = 'chain_activity_v1_applied') then
    update public.economy_settings set value = '200'::jsonb, updated_at = now()
     where key = 'run_proof_min_m' and value = '1000'::jsonb;
    update public.economy_settings set value = '30'::jsonb, updated_at = now()
     where key = 'run_proof_user_daily' and value = '10'::jsonb;
    insert into public.economy_settings (key, value) values ('chain_activity_v1_applied', 'true'::jsonb)
    on conflict (key) do nothing;
  end if;
end $$;

-- ══════════════════════════════════════════════════════════════════
-- 종류
-- ══════════════════════════════════════════════════════════════════
-- not valid: 새 줄만 검사한다. 뒤 파일이 종류를 더 늘리면, setup.sql 을 다시 붙여넣을 때 여기서 옛 목록으로
-- 기존 줄을 검사해 배포가 멈춘다(0036 의 원장 종류와 같은 까닭).
alter table public.chain_jobs drop constraint if exists chain_jobs_kind_check;
alter table public.chain_jobs add constraint chain_jobs_kind_check check (kind in (
  'RUN_PROOF', 'COURSE_RUN', 'BADGE', 'VAULT_MINT', 'STATS_SYNC',
  'SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE', 'DRAW_SEED', 'DRAW_REVEAL',
  'RUN_SETTLE', 'SHOE_WEAR', 'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'PARTY_START', 'DAILY_STEPS', 'CHECK_IN'
)) not valid;

create or replace function economy.activity_kinds() returns text[]
  language sql immutable as $$
  select array['SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE', 'DRAW_SEED', 'DRAW_REVEAL',
               'RUN_SETTLE', 'SHOE_WEAR', 'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'PARTY_START', 'DAILY_STEPS', 'CHECK_IN']
$$;

-- ══════════════════════════════════════════════════════════════════
-- 사람 가명 — 사람마다 비밀값. 계정을 지우면 함께 지워진다(그 뒤로는 아무도 가명과 사람을 잇지 못한다)
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.chain_person_keys (
  user_id uuid primary key references auth.users on delete cascade,
  k bytea not null default economy.random_bytes32(),
  created_at timestamptz not null default now()
);
revoke all on economy.chain_person_keys from public;
do $$ begin
  execute 'revoke all on economy.chain_person_keys from anon, authenticated';
exception when undefined_object then null; end $$;

create or replace function economy.person_pseudonym(p_user uuid) returns text
language plpgsql security definer set search_path = public, economy as $$
declare v bytea;
begin
  if p_user is null then
    return null;
  end if;
  insert into economy.chain_person_keys (user_id) values (p_user) on conflict (user_id) do nothing;
  select k.k into v from economy.chain_person_keys k where k.user_id = p_user;
  return '0x' || encode(sha256(v || convert_to('stepup:person', 'UTF8')), 'hex');
end $$;
revoke all on function economy.person_pseudonym(uuid) from public;

-- ══════════════════════════════════════════════════════════════════
-- 받는 사람 — 기본 비움. 켠 사람만 연결한 지갑
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.chain_recipient_optin (
  user_id uuid primary key references auth.users on delete cascade,
  on_at timestamptz not null default now()
);
revoke all on economy.chain_recipient_optin from public;
do $$ begin
  execute 'revoke all on economy.chain_recipient_optin from anon, authenticated';
exception when undefined_object then null; end $$;

create or replace function economy.chain_recipient(p_user uuid) returns text
  language sql stable security definer set search_path = public, economy as $$
  select coalesce((select l.address from public.wallet_links l
                    where l.user_id = p_user
                      and exists (select 1 from economy.chain_recipient_optin o where o.user_id = p_user)),
                  '0x0000000000000000000000000000000000000000')
$$;
revoke all on function economy.chain_recipient(uuid) from public;

create or replace function public.chain_recipient_set(p_on boolean)
returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if coalesce(p_on, false) then
    insert into economy.chain_recipient_optin (user_id) values (v_user) on conflict (user_id) do nothing;
  else
    delete from economy.chain_recipient_optin where user_id = v_user;
  end if;
  return coalesce(p_on, false);
end $$;
revoke all on function public.chain_recipient_set(boolean) from public, anon;
grant execute on function public.chain_recipient_set(boolean) to authenticated;

-- 내 체인 기록 설정 — 지갑으로 받기를 켰는가 · 지갑이 연결돼 있는가
create or replace function public.my_chain_settings()
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare v_user uuid := auth.uid();
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return jsonb_build_object(
    'wallet_recipient', exists (select 1 from economy.chain_recipient_optin o where o.user_id = v_user),
    'wallet_linked', exists (select 1 from public.wallet_links l where l.user_id = v_user));
end $$;
revoke all on function public.my_chain_settings() from public, anon;
grant execute on function public.my_chain_settings() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 보내는 시각 늦추기 — 거래 시각으로 생활 패턴이 드러나지 않게. 씨앗 약속 · 공개 · 신발 체인 일은 바로
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_jobs_jitter() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_min numeric := greatest(coalesce(economy.setting_num('chain_jitter_min_min'), 0), 0);
  v_max numeric := greatest(coalesce(economy.setting_num('chain_jitter_max_min'), 0), 0);
begin
  if new.kind not in ('VAULT_MINT', 'STATS_SYNC', 'DRAW_SEED', 'DRAW_REVEAL') and new.next_at <= now() + interval '1 second' then
    new.next_at := now() + make_interval(secs => ((v_min + random() * greatest(v_max - v_min, 0)) * 60)::double precision);
  end if;
  return new;
end $$;
revoke all on function economy.chain_jobs_jitter() from public;

drop trigger if exists chain_jobs_jitter on public.chain_jobs;
create trigger chain_jobs_jitter
  before insert on public.chain_jobs
  for each row execute function economy.chain_jobs_jitter();

-- ══════════════════════════════════════════════════════════════════
-- 대조 재료 — 기록마다 소금 · 원본 스냅숏 · 보낸 재료. 서버만 읽는다(관리자는 admin_chain_audit)
-- ══════════════════════════════════════════════════════════════════
create table if not exists economy.chain_audit (
  job_id bigint primary key references public.chain_jobs on delete cascade,
  -- 계정을 지우면 대조용 원본도 함께 지운다(체인의 기록을 다시 그 사람과 잇지 못하게)
  user_id uuid references auth.users on delete cascade,
  recipe text not null,
  salt bytea not null,
  snapshot text not null,
  data_hash text not null,
  payload jsonb not null,
  created_at timestamptz not null default now()
);
revoke all on economy.chain_audit from public;
do $$ begin
  execute 'revoke all on economy.chain_audit from anon, authenticated';
exception when undefined_object then null; end $$;

-- ══════════════════════════════════════════════════════════════════
-- 줄 세우기 — 모든 새 기록이 여기로. 실패해도 경고만(부른 쪽의 일은 그대로 된다)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.activity_enqueue(
  p_kind text,
  p_ref text,
  p_user uuid,
  p_day date,
  p_args jsonb,
  p_sneaker bigint default null,
  p_session bigint default null,
  p_next_at timestamptz default null
) returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare
  v_day int;
  v_cap int;
  v_n int;
  v_id bigint;
begin
  if p_user is null or p_day is null or not economy.chain_jobs_on()
     or not coalesce((economy.setting('chain_activity_enabled') #>> '{}')::boolean, false) then
    return false;
  end if;
  v_day := economy.day_number(p_day);
  v_cap := coalesce((economy.setting('chain_activity_daily') ->> p_kind)::int, 0);
  if v_cap <= 0 then
    return false;
  end if;
  select count(*)::int into v_n from public.chain_jobs j
   where j.user_id = p_user and j.kind = p_kind and (j.args ->> 'day')::int = v_day;
  if v_n >= v_cap then
    return false;
  end if;
  insert into public.chain_jobs (kind, ref, user_id, sneaker_id, session_id, args, next_at)
  values (p_kind, p_ref, p_user, p_sneaker, p_session,
          coalesce(p_args, '{}'::jsonb) || jsonb_build_object('day', v_day), coalesce(p_next_at, now()))
  on conflict (kind, ref) do nothing
  returning id into v_id;
  return v_id is not null;
exception when others then
  raise warning 'activity_enqueue %: %', p_kind, sqlerrm;
  return false;
end $$;
revoke all on function economy.activity_enqueue(text, text, uuid, date, jsonb, bigint, bigint, timestamptz) from public;

-- ══════════════════════════════════════════════════════════════════
-- 신발 — 뽑기 · 수리 · 포인트 (market_sneakers)
-- ══════════════════════════════════════════════════════════════════
-- 뽑기 — 뽑은 순간의 씨앗 해시(약속)를 함께 적는다. 씨앗을 공개하면(draw_seed_reveal) 이 번호로 다시 굴려 볼 수 있다
create or replace function economy.chain_activity_on_draw() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if auth.uid() is null or new.draw_nonce is null or new.origin not in ('FREE_DRAW', 'BONUS_DRAW', 'PAID_DRAW') then
    return null;
  end if;
  perform economy.activity_enqueue('SHOE_DRAW', 'draw:' || new.id, new.owner_id, economy.game_day(now()),
    jsonb_build_object('shoe_id', new.id, 'draw_kind', economy.draw_kind(new.origin), 'rarity', new.rarity,
                       'model_id', new.model_id, 'draw_nonce', new.draw_nonce,
                       'seed_hash', (select d.seed_hash from public.draw_seeds d where d.user_id = new.owner_id)),
    new.id);
  return null;
end $$;
revoke all on function economy.chain_activity_on_draw() from public;

drop trigger if exists market_sneakers_chain_activity_draw on public.market_sneakers;
create trigger market_sneakers_chain_activity_draw
  after insert on public.market_sneakers
  for each row execute function economy.chain_activity_on_draw();

-- 수리(남은 내구도만 오름) · 포인트 나누기(포인트가 바뀜) — 주인이 부른 요청 안에서만. 달려서 닳는 것은 기록하지 않는다
create or replace function economy.chain_activity_on_sneaker() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if auth.uid() is null or auth.uid() is distinct from new.owner_id then
    return null;
  end if;
  if (new.pts_eff, new.pts_comfort, new.pts_dur) is distinct from (old.pts_eff, old.pts_comfort, old.pts_dur) then
    perform economy.activity_enqueue('SHOE_POINTS', 'points:' || new.id || ':' || gen_random_uuid(), new.owner_id,
      economy.game_day(now()),
      jsonb_build_object('shoe_id', new.id, 'efficiency', new.pts_eff, 'comfort', new.pts_comfort, 'durability', new.pts_dur),
      new.id);
  elsif new.dur_now > old.dur_now then
    perform economy.activity_enqueue('SHOE_REPAIR', 'repair:' || new.id || ':' || gen_random_uuid(), new.owner_id,
      economy.game_day(now()),
      jsonb_build_object('shoe_id', new.id, 'points', round(new.dur_now - old.dur_now)::int),
      new.id);
  end if;
  return null;
end $$;
revoke all on function economy.chain_activity_on_sneaker() from public;

drop trigger if exists market_sneakers_chain_activity on public.market_sneakers;
create trigger market_sneakers_chain_activity
  after update of dur_now, pts_eff, pts_comfort, pts_dur on public.market_sneakers
  for each row execute function economy.chain_activity_on_sneaker();

-- ══════════════════════════════════════════════════════════════════
-- 뽑기 씨앗 — 새 씨앗의 해시를 바로 약속하고(뽑기 기록은 10분 넘게 늦게 나가니 약속이 먼저 올라간다), 공개하면 씨앗을 올린다
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_seed() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if tg_op = 'UPDATE' and new.seed_hash is not distinct from old.seed_hash then
    return null;
  end if;
  perform economy.activity_enqueue('DRAW_SEED', 'seed:' || new.user_id || ':' || new.seed_hash, new.user_id,
    economy.game_day(now()),
    jsonb_build_object('seed_hash', new.seed_hash, 'first_nonce', new.first_nonce));
  return null;
end $$;
revoke all on function economy.chain_activity_on_seed() from public;

drop trigger if exists draw_seeds_chain_activity on public.draw_seeds;
create trigger draw_seeds_chain_activity
  after insert or update of seed_hash on public.draw_seeds
  for each row execute function economy.chain_activity_on_seed();

create or replace function economy.chain_activity_on_reveal() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.activity_enqueue('DRAW_REVEAL', 'reveal:' || new.id, new.user_id, economy.game_day(new.revealed_at),
    jsonb_build_object('reveal_id', new.id, 'seed_hash', new.seed_hash, 'first_nonce', new.first_nonce,
                       'last_nonce', new.last_nonce));
  return null;
end $$;
revoke all on function economy.chain_activity_on_reveal() from public;

drop trigger if exists draw_seed_reveals_chain_activity on public.draw_seed_reveals;
create trigger draw_seed_reveals_chain_activity
  after insert on public.draw_seed_reveals
  for each row execute function economy.chain_activity_on_reveal();

-- 공개된 씨앗으로 다시 굴려 본다 — 누구나(로그인 없이도). 뽑기가 쓰는 함수 그대로(economy.draw_create, 0066)
create or replace function public.draw_verify(p_seed_hex text, p_nonce bigint, p_pool text default 'FREE')
returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_seed bytea;
  v_d bytea;
  v_rarity text;
begin
  if coalesce(p_seed_hex, '') !~ '^(0x)?[0-9a-fA-F]{64}$' or p_nonce is null or p_nonce < 0
     or coalesce(p_pool, '') not in ('FREE', 'PREMIUM') then
    raise exception '씨앗(64자리 16진수) · 번호 · 뽑기 종류(FREE · PREMIUM)를 확인해 주세요' using errcode = '22023';
  end if;
  v_seed := decode(regexp_replace(p_seed_hex, '^0x', ''), 'hex');
  v_d := sha256(v_seed || convert_to(':' || p_nonce::text, 'UTF8'));
  v_rarity := economy.roll_rarity_v3(economy.bytes_int(v_d, 0, 4), p_pool, null);
  return jsonb_build_object(
    'seed_hash', encode(sha256(v_seed), 'hex'),
    'nonce', p_nonce, 'pool', p_pool, 'rarity', v_rarity,
    'model_id', economy.draw_model(v_rarity, economy.bytes_int(v_d, 12, 2)));
end $$;
revoke all on function public.draw_verify(text, bigint, text) from public;
grant execute on function public.draw_verify(text, bigint, text) to anon, authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 강화 (forge_requests — 판정과 같은 트랜잭션에 한 줄)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_forge() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if new.status not in ('SUCCESS', 'FAILED') or new.target_id is null then
    return null;
  end if;
  perform economy.activity_enqueue('SHOE_FORGE', 'forge:' || new.user_id || ':' || new.request_key, new.user_id,
    economy.game_day(new.created_at),
    jsonb_build_object('shoe_id', new.target_id, 'success', new.status = 'SUCCESS',
                       'level_before', coalesce(new.level_before, 0), 'level_after', coalesce(new.level_after, 0),
                       'rate_permille', coalesce(new.rate_permille, 0), 'roll', coalesce(new.roll, 0),
                       'materials', coalesce(cardinality(new.material_ids), 0), 'rule_version', new.rule_version),
    new.target_id);
  return null;
end $$;
revoke all on function economy.chain_activity_on_forge() from public;

drop trigger if exists forge_requests_chain_activity on public.forge_requests;
create trigger forge_requests_chain_activity
  after insert on public.forge_requests
  for each row execute function economy.chain_activity_on_forge();

-- ══════════════════════════════════════════════════════════════════
-- 거래소 체결 (market_trades) — 산 사람의 기록으로(판 사람도 가명으로 함께 올라간다)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_trade() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if new.buyer_id is null or new.seller_id is null or new.sneaker_id is null then
    return null;
  end if;
  perform economy.activity_enqueue('SHOE_TRADE', 'trade:' || new.id, new.buyer_id, economy.game_day(new.traded_at),
    jsonb_build_object('shoe_id', new.sneaker_id, 'seller_id', new.seller_id, 'rarity', new.rarity, 'level', new.level,
                       'price_centi', round(new.price * 100)::bigint, 'trade_id', new.id),
    new.sneaker_id);
  return null;
end $$;
revoke all on function economy.chain_activity_on_trade() from public;

drop trigger if exists market_trades_chain_activity on public.market_trades;
create trigger market_trades_chain_activity
  after insert on public.market_trades
  for each row execute function economy.chain_activity_on_trade();

-- ══════════════════════════════════════════════════════════════════
-- 러닝 정산 · 신발 주행 기록 — 올린 러닝마다(무효 제외). 값은 보낼 때 서버에 적힌 것(정산 · 신발 갱신이 끝난 뒤)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_session() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare v_day date := economy.game_day(new.started_at);
begin
  if new.verdict = 'VOID' or new.duration_sec <= 0 then
    return null;
  end if;
  perform economy.activity_enqueue('RUN_SETTLE', 'settle:' || new.id, new.user_id, v_day,
    jsonb_build_object('session_id', new.id, 'distance_m', round(greatest(new.distance_meters, 0))::int,
                       'duration_sec', new.duration_sec),
    null, new.id);
  if new.sneaker_id is not null then
    perform economy.activity_enqueue('SHOE_WEAR', 'wear:' || new.id, new.user_id, v_day,
      jsonb_build_object('session_id', new.id, 'shoe_id', new.sneaker_id, 'distance_m', round(greatest(new.gps_credit_m, 0))::int),
      new.sneaker_id, new.id);
  end if;
  return null;
end $$;
revoke all on function economy.chain_activity_on_session() from public;

drop trigger if exists walk_sessions_chain_activity on public.walk_sessions;
create trigger walk_sessions_chain_activity
  after insert on public.walk_sessions
  for each row execute function economy.chain_activity_on_session();

-- ══════════════════════════════════════════════════════════════════
-- 크루 러닝 완주 — 올린 러닝과 짝지어질 때(record_session 이 crew_runs.session_id 를 채운다)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_crew_run() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  w public.walk_sessions;
begin
  if old.session_id is not null or new.session_id is null or new.crew_id is null then
    return null;
  end if;
  select * into w from public.walk_sessions s where s.id = new.session_id;
  if not found or w.verdict = 'VOID' or w.mock_location then
    return null;
  end if;
  perform economy.activity_enqueue('CREW_RUN', 'crew_run:' || new.user_id || ':' || new.day, new.user_id, new.day,
    jsonb_build_object('crew_id', new.crew_id, 'party_id', new.party_id, 'session_id', new.session_id,
                       'distance_m', round(greatest(coalesce(w.gps_distance_m, 0), 0))::int,
                       'duration_sec', round(greatest(coalesce(new.active_sec, 0), 0))::int,
                       'together', (select count(*)::int from economy.crew_runs c where c.party_id = new.party_id)),
    null, new.session_id);
  return null;
end $$;
revoke all on function economy.chain_activity_on_crew_run() from public;

drop trigger if exists crew_runs_chain_activity on economy.crew_runs;
create trigger crew_runs_chain_activity
  after update of session_id on economy.crew_runs
  for each row execute function economy.chain_activity_on_crew_run();

-- ══════════════════════════════════════════════════════════════════
-- 함께 출발 — 방이 출발할 때 진행자의 기록 하나(명단은 보낼 때 출발 명단 party_runs 로 센다 — 둘 이상만)
-- 번개러닝 출석 — 함께 출발한 명단에 둘 이상일 때 사람마다. 행 트리거는 문장이 끝난 뒤에 돌아 명단이 다 보인다
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_party_start() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  if new.status <> 'COUNTDOWN' or old.status = 'COUNTDOWN' then
    return null;
  end if;
  perform economy.activity_enqueue('PARTY_START', 'party_start:' || new.id, new.host_id,
    economy.game_day(coalesce(new.starts_at, now())),
    jsonb_build_object('party_id', new.id, 'crew_id', new.crew_id));
  return null;
end $$;
revoke all on function economy.chain_activity_on_party_start() from public;

drop trigger if exists parties_chain_activity on public.parties;
create trigger parties_chain_activity
  after update of status on public.parties
  for each row execute function economy.chain_activity_on_party_start();

create or replace function economy.chain_activity_on_party_run() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_flash bigint;
  v_n int;
begin
  select p.flash_post_id into v_flash from public.parties p where p.id = new.party_id;
  if v_flash is null then
    return null;
  end if;
  select count(*)::int into v_n from public.party_runs r where r.party_id = new.party_id;
  if v_n < 2 then
    return null;
  end if;
  perform economy.activity_enqueue('FLASH_RUN', 'flash:' || new.party_id || ':' || new.user_id, new.user_id,
    economy.game_day(new.starts_at),
    jsonb_build_object('party_id', new.party_id, 'together', v_n));
  return null;
end $$;
revoke all on function economy.chain_activity_on_party_run() from public;

drop trigger if exists party_runs_chain_activity on public.party_runs;
create trigger party_runs_chain_activity
  after insert on public.party_runs
  for each row execute function economy.chain_activity_on_party_run();

-- ══════════════════════════════════════════════════════════════════
-- 크루 가입 — 같은 크루는 한 번(나갔다 들어와도)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_crew_join() returns trigger
language plpgsql security definer set search_path = public, economy as $$
begin
  perform economy.activity_enqueue('CREW_JOIN', 'crew_join:' || new.crew_id || ':' || new.user_id, new.user_id,
    economy.game_day(new.joined_at),
    jsonb_build_object('crew_id', new.crew_id, 'role', new.role));
  return null;
end $$;
revoke all on function economy.chain_activity_on_crew_join() from public;

drop trigger if exists crew_members_chain_activity on public.crew_members;
create trigger crew_members_chain_activity
  after insert on public.crew_members
  for each row execute function economy.chain_activity_on_crew_join();

-- ══════════════════════════════════════════════════════════════════
-- 하루 걸음 — 그날 처음 적힐 때 줄 세우고, 그날이 끝난 뒤(한국 자정 + 10분)에 목표 달성 여부만 보낸다.
-- 걸음 수는 체인에 올리지 않는다(대조용 스냅숏에만). 어제 · 오늘 것만(지난 날을 한꺼번에 올려 기록을 불리지 못하게)
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.chain_activity_on_steps() returns trigger
language plpgsql security definer set search_path = public, economy as $$
declare
  v_day date := date '1970-01-01' + new.epoch_day::int;
  v_today date := economy.game_day(now());
begin
  if new.steps <= 0 or v_day < v_today - 1 or v_day > v_today then
    return null;
  end if;
  perform economy.activity_enqueue('DAILY_STEPS', 'steps:' || new.user_id || ':' || new.epoch_day, new.user_id, v_day,
    jsonb_build_object('epoch_day', new.epoch_day), null, null,
    economy.game_day_start(v_day + 1) + interval '10 minutes');
  return null;
end $$;
revoke all on function economy.chain_activity_on_steps() from public;

drop trigger if exists daily_steps_chain_activity on public.daily_steps;
create trigger daily_steps_chain_activity
  after insert or update of steps on public.daily_steps
  for each row execute function economy.chain_activity_on_steps();

-- ══════════════════════════════════════════════════════════════════
-- 앱 출석 — 앱이 하루 한 번 부른다. 돌려주는 값: 오늘 줄을 새로 세웠는가(이미 했으면 false)
-- ══════════════════════════════════════════════════════════════════
create or replace function public.chain_check_in()
returns boolean
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_day date := economy.game_day(now());
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  return economy.activity_enqueue('CHECK_IN', 'checkin:' || v_user || ':' || v_day, v_user, v_day, '{}'::jsonb);
end $$;
revoke all on function public.chain_check_in() from public, anon;
grant execute on function public.chain_check_in() to authenticated;

-- ══════════════════════════════════════════════════════════════════
-- 보낼 재료 — 키는 워커 스키마의 칸 이름 그대로(attester/src/eas.js). '_audit' 는 체인에 올리지 않고 스냅숏에만 넣는다.
-- null 이면 거둔다
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.activity_payload(j public.chain_jobs) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  a jsonb := j.args;
  v jsonb;
  w public.walk_sessions;
  s public.market_sneakers;
  r record;
  v_roster text[];
begin
  v := jsonb_build_object(
    'recipient', economy.chain_recipient(j.user_id),
    'runner', economy.person_pseudonym(j.user_id),
    'day', (a ->> 'day')::int);
  if j.kind = 'SHOE_DRAW' then
    if coalesce(a ->> 'seed_hash', '') !~ '^[0-9a-f]{64}$' then
      return null;
    end if;
    return v || jsonb_build_object(
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'drawKind', a ->> 'draw_kind', 'rarity', a ->> 'rarity',
      'modelId', coalesce((a ->> 'model_id')::int, 0),
      'drawNonce', (a ->> 'draw_nonce')::bigint,
      'seedHash', '0x' || (a ->> 'seed_hash'));
  elsif j.kind = 'SHOE_FORGE' then
    return v || jsonb_build_object(
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'success', (a ->> 'success')::boolean,
      'levelBefore', (a ->> 'level_before')::int, 'levelAfter', (a ->> 'level_after')::int,
      'ratePermille', (a ->> 'rate_permille')::int, 'roll', (a ->> 'roll')::int,
      'materials', (a ->> 'materials')::int,
      '_audit', jsonb_build_object('rule_version', a ->> 'rule_version'));
  elsif j.kind = 'SHOE_REPAIR' then
    return v || jsonb_build_object('shoe', economy.pseudonym('shoe', a ->> 'shoe_id'), 'points', (a ->> 'points')::int);
  elsif j.kind = 'SHOE_POINTS' then
    return v || jsonb_build_object(
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'efficiency', (a ->> 'efficiency')::int, 'comfort', (a ->> 'comfort')::int, 'durability', (a ->> 'durability')::int);
  elsif j.kind = 'SHOE_TRADE' then
    return v || jsonb_build_object(
      'seller', economy.person_pseudonym((a ->> 'seller_id')::uuid),
      'buyer', v ->> 'runner',
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'rarity', a ->> 'rarity', 'level', (a ->> 'level')::int,
      'priceCenti', (a ->> 'price_centi')::text,
      '_audit', jsonb_build_object('trade_id', a ->> 'trade_id'));
  elsif j.kind = 'DRAW_SEED' then
    if coalesce(a ->> 'seed_hash', '') !~ '^[0-9a-f]{64}$' then
      return null;
    end if;
    return v || jsonb_build_object('seedHash', '0x' || (a ->> 'seed_hash'), 'firstNonce', (a ->> 'first_nonce')::bigint);
  elsif j.kind = 'DRAW_REVEAL' then
    select x.seed_hex, x.seed_hash, x.first_nonce, x.last_nonce into r
      from public.draw_seed_reveals x where x.id = (a ->> 'reveal_id')::bigint;
    if not found or coalesce(r.seed_hex, '') !~ '^[0-9a-f]{64}$' then
      return null;
    end if;
    return v || jsonb_build_object('seed', '0x' || r.seed_hex, 'seedHash', '0x' || r.seed_hash,
                                   'firstNonce', r.first_nonce, 'lastNonce', greatest(r.last_nonce, 0));
  elsif j.kind = 'RUN_SETTLE' then
    select * into w from public.walk_sessions x where x.id = (a ->> 'session_id')::bigint;
    if not found then
      return null;
    end if;
    return v || jsonb_build_object(
      'run', economy.pseudonym('run', w.id::text),
      'distanceM', round(greatest(w.distance_meters, 0))::int, 'durationSec', greatest(w.duration_sec, 0),
      'pointsMilli', round(greatest(coalesce(w.points_awarded, 0), 0) * 1000)::bigint::text,
      'verdict', w.verdict,
      '_audit', jsonb_build_object(
        'started_at', w.started_at, 'ended_at', w.ended_at, 'steps', w.steps,
        'gps_distance_m', w.gps_distance_m, 'gps_credit_m', w.gps_credit_m, 'backed_steps', w.backed_steps,
        'points', w.points_awarded, 'verdict_reason', w.verdict_reason, 'party_size', w.party_size,
        'boost_bps', w.boost_bps, 'energy_used', w.energy_used,
        'shoe', case when w.sneaker_id is not null then economy.pseudonym('shoe', w.sneaker_id::text) end,
        'track_sha256', encode(sha256(convert_to(coalesce(w.track, ''), 'UTF8')), 'hex')));
  elsif j.kind = 'SHOE_WEAR' then
    select * into s from public.market_sneakers x where x.id = (a ->> 'shoe_id')::bigint;
    if not found then
      return null;
    end if;
    return v || jsonb_build_object(
      'shoe', economy.pseudonym('shoe', s.id::text),
      'run', economy.pseudonym('run', a ->> 'session_id'),
      'runM', (a ->> 'distance_m')::int,
      'totalM', round(greatest(s.km_run, 0) * 1000)::bigint::text,
      'durability', round(greatest(s.dur_now, 0))::int,
      '_audit', jsonb_build_object('dur_max', s.dur_max, 'level', s.level, 'rarity', s.rarity));
  elsif j.kind = 'CREW_RUN' then
    select * into w from public.walk_sessions x where x.id = (a ->> 'session_id')::bigint;
    if not found or w.verdict = 'VOID' then
      return null;
    end if;
    return v || jsonb_build_object(
      'crew', economy.pseudonym('crew', a ->> 'crew_id'),
      'room', economy.pseudonym('party', a ->> 'party_id'),
      'run', economy.pseudonym('run', a ->> 'session_id'),
      'distanceM', (a ->> 'distance_m')::int, 'activeSec', (a ->> 'duration_sec')::int,
      'together', (a ->> 'together')::int);
  elsif j.kind = 'FLASH_RUN' then
    return v || jsonb_build_object(
      'flash', economy.pseudonym('party', a ->> 'party_id'), 'together', (a ->> 'together')::int);
  elsif j.kind = 'CREW_JOIN' then
    return v || jsonb_build_object('crew', economy.pseudonym('crew', a ->> 'crew_id'),
                                   '_audit', jsonb_build_object('role', a ->> 'role'));
  elsif j.kind = 'PARTY_START' then
    select array_agg(economy.person_pseudonym(x.user_id) order by economy.person_pseudonym(x.user_id)) into v_roster
      from public.party_runs x where x.party_id = (a ->> 'party_id')::bigint;
    if coalesce(cardinality(v_roster), 0) < 2 then
      return null;
    end if;
    return v || jsonb_build_object(
      'room', economy.pseudonym('party', a ->> 'party_id'),
      'crew', case when a ->> 'crew_id' is not null then economy.pseudonym('crew', a ->> 'crew_id')
                   else '0x' || repeat('0', 64) end,
      'starters', cardinality(v_roster),
      '_audit', jsonb_build_object('roster', to_jsonb(v_roster)));
  elsif j.kind = 'DAILY_STEPS' then
    select d.steps, d.goal into r from public.daily_steps d
     where d.user_id = j.user_id and d.epoch_day = (a ->> 'epoch_day')::bigint;
    if not found or coalesce(r.steps, 0) <= 0 then
      return null;
    end if;
    return v || jsonb_build_object('goalMet', r.steps >= r.goal,
                                   '_audit', jsonb_build_object('steps', r.steps, 'goal', r.goal));
  elsif j.kind = 'CHECK_IN' then
    return v;
  end if;
  return null;
end $$;
revoke all on function economy.activity_payload(public.chain_jobs) from public;

-- 재료 봉인 — 처음 가져갈 때 소금 · 스냅숏 · dataHash 를 적고, 다시 가져가면(2분 임대가 끝남) 그때 재료를 그대로 쓴다.
-- 스냅숏 = 'stepup-audit-v1|종류|ref|' ‖ 받는 사람을 뺀 재료(jsonb 글자 — 키 순서가 늘 같다)
create or replace function economy.activity_seal(j public.chain_jobs) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  au economy.chain_audit;
  p jsonb;
  v_salt bytea;
  v_snap text;
  v_hash text;
begin
  select * into au from economy.chain_audit x where x.job_id = j.id;
  if found then
    return au.payload;
  end if;
  p := economy.activity_payload(j);
  if p is null then
    return null;
  end if;
  v_salt := economy.random_bytes32();
  v_snap := 'stepup-audit-v1|' || j.kind || '|' || j.ref || '|' || (p - 'recipient')::text;
  v_hash := '0x' || encode(sha256(v_salt || convert_to(v_snap, 'UTF8')), 'hex');
  p := (p - '_audit') || jsonb_build_object('dataHash', v_hash);
  insert into economy.chain_audit (job_id, user_id, recipe, salt, snapshot, data_hash, payload)
  values (j.id, j.user_id, 'stepup-audit-v1', v_salt, v_snap, v_hash, p);
  return p;
end $$;
revoke all on function economy.activity_seal(public.chain_jobs) from public;

-- 관리자 — 기록 하나를 대조한다: 서버에 남은 소금 · 스냅숏으로 dataHash 를 다시 만들어 적어 둔 값과 같은가.
-- 체인 쪽 값은 attester/scripts/audit.mjs <EAS 증명 번호> 로 읽어 이 data_hash 와 맞대어 본다.
create or replace function public.admin_chain_audit(p_job bigint)
returns table (job_id bigint, kind text, ref text, status text, tx_hash text, attestation text, recipe text,
               salt_hex text, snapshot text, data_hash text, hash_ok boolean)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform public.admin_guard();
  return query
  select c.id, c.kind, c.ref, c.status, c.tx_hash, c.result, au.recipe, encode(au.salt, 'hex'), au.snapshot, au.data_hash,
         au.data_hash = '0x' || encode(sha256(au.salt || convert_to(au.snapshot, 'UTF8')), 'hex')
    from public.chain_jobs c
    join economy.chain_audit au on au.job_id = c.id
   where c.id = p_job;
end $$;
revoke all on function public.admin_chain_audit(bigint) from public, anon;
grant execute on function public.admin_chain_audit(bigint) to authenticated;

/*
 * 0045 그대로 + 새 기록(economy.activity_kinds — 재료는 activity_seal: 처음 가져갈 때 봉인, 다시 가져가면 같은 재료) +
 * 러닝 증명 · 코스 · 배지도 받는 사람은 켠 사람만, 사람 가명은 사람마다 비밀값(0078).
 *
 * 돌려주는 payload (종류마다):
 *   RUN_PROOF   recipient · runner · run · day · distance_m · duration_sec
 *   COURSE_RUN  recipient · runner · course · run · day · distance_m · duration_sec
 *   BADGE       recipient · runner · badge · value · day
 *   VAULT_MINT  account · model_id(새 도감, 없으면 null) · faction · rarity · variant · level · efficiency_bps · comfort_bps ·
 *               durability · genesis_no · deadline_unix
 *   STATS_SYNC  token_id · level · durability · deadline_unix
 *   그 밖(0078) recipient · runner · day · dataHash + 스키마 칸(eas.js ACTIVITY_SCHEMAS)
 */
create or replace function public.attester_jobs_claim(p_kinds text[], p_limit int)
returns table (job_id bigint, op_ref text, kind text, payload jsonb)
language plpgsql security definer set search_path = public, economy as $$
declare
  j public.chain_jobs;
  v_left int;
  v_taken int := 0;
  v_want int := greatest(0, least(coalesce(p_limit, 0), 50));
  v_payload jsonb;
  v_owner uuid;
  s public.market_sneakers;
  w public.walk_sessions;
  v_deadline bigint := extract(epoch from now())::bigint
                       + coalesce(economy.setting_num('chain_job_deadline_sec'), 3600)::bigint;
begin
  perform economy.attester_guard();
  if coalesce((economy.setting('chain_paused') #>> '{}')::boolean, false) or not economy.chain_jobs_on() then
    return;
  end if;
  -- 오늘 보낸 수 + 지금 보내는 중인 수가 하루 상한 안에서만
  v_left := coalesce(economy.setting_num('chain_jobs_global_daily'), 0)::int
            - (select count(*) from public.chain_jobs c
                where (c.sent_at >= economy.today_start()) or c.status = 'CLAIMED')::int;
  v_want := least(v_want, greatest(v_left, 0));

  for j in
    select * from public.chain_jobs c
     where c.kind = any (p_kinds)
       and (c.status = 'QUEUED' or (c.status = 'CLAIMED' and c.lease_until < now()))
       and c.next_at <= now()
     order by c.next_at, c.id
     limit 200
     for update skip locked
  loop
    exit when v_taken >= v_want;
    v_payload := null;

    if j.user_id is null then
      perform economy.job_cancel(j.id, 'account deleted');
      continue;
    end if;

    if j.kind = any (economy.activity_kinds()) then
      v_payload := economy.activity_seal(j);
      if v_payload is null then
        perform economy.job_cancel(j.id, 'nothing to record');
        continue;
      end if;

    elsif j.kind in ('RUN_PROOF', 'COURSE_RUN', 'BADGE') then
      if j.kind <> 'BADGE' then
        select * into w from public.walk_sessions where id = j.session_id;
        if not found or w.verdict = 'VOID' then
          perform economy.job_cancel(j.id, 'run gone');
          continue;
        end if;
      end if;
      -- 0078: 받는 사람은 켠 사람만 지갑, 사람 가명은 사람마다 비밀값(계정을 지우면 다시 잇지 못한다)
      v_payload := jsonb_build_object(
        'recipient', economy.chain_recipient(j.user_id),
        'runner', economy.person_pseudonym(j.user_id),
        'day', (j.args ->> 'day')::int);
      if j.kind = 'RUN_PROOF' then
        v_payload := v_payload || jsonb_build_object(
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      elsif j.kind = 'COURSE_RUN' then
        v_payload := v_payload || jsonb_build_object(
          'course', economy.pseudonym('course', j.args ->> 'course_id'),
          'run', economy.pseudonym('run', j.session_id::text),
          'distance_m', (j.args ->> 'distance_m')::int,
          'duration_sec', (j.args ->> 'duration_sec')::int);
      else
        v_payload := v_payload || jsonb_build_object(
          'badge', j.args ->> 'badge',
          'value', (j.args ->> 'value')::int);
      end if;

    else
      -- 신발 — 지금 주인의 꺼내기와 엇갈리지 않게 같은 잠금을 잡는다(앱에서 팔렸으면 새 주인).
      -- 못 잡으면(꺼내는 중) 다음에 — 기다리지 않아 서로 잠금을 기다리며 멈추는 일이 없다
      select owner_id into v_owner from public.market_sneakers where id = j.sneaker_id;
      if v_owner is null then
        perform economy.job_cancel(j.id, 'sneaker gone');
        continue;
      end if;
      if not pg_try_advisory_xact_lock(hashtext('ledger:' || v_owner::text)) then
        continue;
      end if;
      select * into s from public.market_sneakers where id = j.sneaker_id;
      if not found or s.owner_id is distinct from v_owner then
        continue;
      end if;
      if exists (select 1 from public.chain_ops o
                  where o.sneaker_id = s.id and o.status in ('RESERVED', 'SIGNED', 'SUBMITTED')) then
        -- 꺼내는 중 — 끝나거나 만료된 뒤에 다시 본다
        update public.chain_jobs set next_at = now() + interval '10 minutes', updated_at = now() where id = j.id;
        continue;
      end if;
      if j.kind = 'VAULT_MINT' then
        if s.token_id is not null then
          perform economy.job_cancel(j.id, 'already on chain');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'account', economy.pseudonym('runner', s.owner_id::text),
          'model_id', s.model_id,
          'faction', s.faction, 'rarity', s.rarity, 'variant', s.variant, 'level', s.level,
          'efficiency_bps', s.efficiency_bps, 'comfort_bps', s.comfort_bps,
          'durability', s.durability_pts, 'genesis_no', s.genesis_no,
          'deadline_unix', v_deadline);
      else
        if s.token_id is null or s.token_id < economy.v3_first_token() or s.chain_state <> 'APP' then
          perform economy.job_cancel(j.id, 'not in the v3 vault');
          continue;
        end if;
        v_payload := jsonb_build_object(
          'token_id', s.token_id::text, 'level', s.level, 'durability', s.durability_pts,
          'deadline_unix', v_deadline);
      end if;
    end if;

    update public.chain_jobs c
       set status = 'CLAIMED', lease_until = now() + interval '2 minutes', payload = v_payload, updated_at = now()
     where c.id = j.id;
    v_taken := v_taken + 1;
    job_id := j.id;
    op_ref := economy.op_ref(j.op_id);
    kind := j.kind;
    payload := v_payload;
    return next;
  end loop;
end $$;

-- 관리자 — 종류마다 몇 건이 기다리고 · 나가고 · 확정됐는지(0044 + 새 기록)
create or replace function public.admin_chain_jobs_stats()
returns table (kind text, queued bigint, in_flight bigint, confirmed_today bigint, confirmed_total bigint,
               failed bigint, cancelled bigint)
language plpgsql stable security definer set search_path = public, economy as $$
begin
  perform public.admin_guard();
  return query
  select k.kind,
         count(*) filter (where c.status = 'QUEUED'),
         count(*) filter (where c.status in ('CLAIMED', 'SENT')),
         count(*) filter (where c.status = 'CONFIRMED' and c.updated_at >= economy.today_start()),
         count(*) filter (where c.status = 'CONFIRMED'),
         count(*) filter (where c.status = 'FAILED'),
         count(*) filter (where c.status = 'CANCELLED')
    from unnest(array['RUN_PROOF', 'COURSE_RUN', 'BADGE', 'VAULT_MINT', 'STATS_SYNC'] || economy.activity_kinds()) k(kind)
    left join public.chain_jobs c on c.kind = k.kind
   group by k.kind
   order by k.kind;
end $$;
revoke all on function public.admin_chain_jobs_stats() from public, anon;
grant execute on function public.admin_chain_jobs_stats() to authenticated;
