-- ════════════════════════════════════════════════════════════════════
--  0078 — 체인 기록 넓히기: 게임 · 러닝 · 크루 · 매일 활동 (2026-10-27, GIWA 테스트넷 — 가스비는 우리가 낸다)
--
--  사용자 결정 "거래수가중요해" — 게임 활동 · 러닝 · 크루 활동 · 매일 기록을 고름(1시간 묶음 앵커는 고르지 않음).
--  원칙은 0044 그대로: 사용자가 실제로 한 일 하나 = 체인 기록 하나, 서버에 적힌 것만, 사람은 가명(economy.pseudonym),
--  위치 · 이름 · 계정 번호는 올리지 않는다. 체인 기록이 실패해도 그 일(뽑기 · 강화 · 러닝 …)은 그대로 된다.
--
--  새 기록(모두 EAS 증명 — 어테스터 워커 attester/src/eas.js 의 같은 이름 스키마):
--    SHOE_DRAW    뽑기 한 번(무료 · 상급 · 보너스) — 등급 · 모델 · 뽑기 번호 · 씨앗 해시(공정성: 씨앗을 공개하면 누구나 다시 굴려 본다)
--    SHOE_FORGE   강화 한 번 — 성공/실패 · 레벨 전후 · 확률 · 굴린 값 · 태운 재료 수
--    SHOE_REPAIR  수리 한 번 — 채운 내구도
--    SHOE_POINTS  스탯 포인트 나누기 한 번 — 나눈 뒤 효율 · 착화감 · 내구도 포인트
--    SHOE_TRADE   거래소 체결 한 번 — 판 사람 · 산 사람 가명, 등급 · 레벨 · 값
--    CREW_RUN     크루 러닝 완주(올린 러닝과 짝지어진 것) — 크루 · 방 · 거리 · 달린 초 · 함께 출발한 수
--    FLASH_RUN    번개러닝 출석 — 둘 이상 함께 출발한 번개 방
--    CREW_JOIN    크루 가입(만든 사람 포함) — 같은 크루는 한 번
--    DAILY_STEPS  하루 걸음(기기가 보고한 값) — 하루 한 번, 그날이 끝난 뒤(한국 자정 + 10분) 마지막 값
--    CHECK_IN     앱 출석 — 하루 한 번(앱이 chain_check_in 을 부른다)
--  그리고 러닝 증명(RUN_PROOF)의 기준을 1km → 200m, 한 사람 하루 상한을 10 → 30 으로(한 번만 — 관리자가 바꾼 값은 두고).
--
--  막는 것: 종류마다 한 사람 하루 상한(chain_activity_daily), 전체 하루 상한(chain_jobs_global_daily)은 그대로.
--  사용자 요청(로그인한 사람의 RPC) 안에서 생긴 것만 — 배포 · 관리자 작업이 표를 고쳐도 기록을 만들지 않는다.
--
--  운영 값(economy_settings):
--    chain_activity_enabled  false 면 새 기록을 줄 세우지 않는다(이미 선 것은 나간다)
--    chain_activity_daily    {"SHOE_DRAW": 50, …} 종류마다 한 사람 하루 상한(0 이면 그 종류는 끈다)
-- ════════════════════════════════════════════════════════════════════

insert into public.economy_settings (key, value) values
  ('chain_activity_enabled', 'true'::jsonb),
  ('chain_activity_daily', '{"SHOE_DRAW": 50, "SHOE_FORGE": 30, "SHOE_REPAIR": 20, "SHOE_POINTS": 20, "SHOE_TRADE": 30,
                             "CREW_RUN": 3, "FLASH_RUN": 5, "CREW_JOIN": 3, "DAILY_STEPS": 1, "CHECK_IN": 1}'::jsonb)
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
  'SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE',
  'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'DAILY_STEPS', 'CHECK_IN'
)) not valid;

create or replace function economy.activity_kinds() returns text[]
  language sql immutable as $$
  select array['SHOE_DRAW', 'SHOE_FORGE', 'SHOE_REPAIR', 'SHOE_POINTS', 'SHOE_TRADE',
               'CREW_RUN', 'FLASH_RUN', 'CREW_JOIN', 'DAILY_STEPS', 'CHECK_IN']
$$;

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
                       'materials', coalesce(cardinality(new.material_ids), 0)),
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
                       'price_centi', round(new.price * 100)::bigint),
    new.sneaker_id);
  return null;
end $$;
revoke all on function economy.chain_activity_on_trade() from public;

drop trigger if exists market_trades_chain_activity on public.market_trades;
create trigger market_trades_chain_activity
  after insert on public.market_trades
  for each row execute function economy.chain_activity_on_trade();

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
-- 번개러닝 출석 — 함께 출발한 명단(party_runs)에 둘 이상일 때. 행 트리거는 문장이 끝난 뒤에 돌아 명단이 다 보인다
-- ══════════════════════════════════════════════════════════════════
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
-- 하루 걸음 — 그날 처음 적힐 때 줄 세우고, 그날이 끝난 뒤(한국 자정 + 10분)에 마지막 값을 보낸다.
-- 기기가 보고한 값이다. 어제 · 오늘 것만(지난 날을 한꺼번에 올려 기록을 불리지 못하게)
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
-- 보낼 재료 — 키는 워커 스키마의 칸 이름 그대로(attester/src/eas.js). null 이면 거둔다
-- ══════════════════════════════════════════════════════════════════
create or replace function economy.activity_payload(j public.chain_jobs) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  a jsonb := j.args;
  v jsonb;
  w public.walk_sessions;
  v_steps int;
begin
  v := jsonb_build_object(
    'recipient', coalesce((select l.address from public.wallet_links l where l.user_id = j.user_id),
                          '0x0000000000000000000000000000000000000000'),
    'runner', economy.pseudonym('runner', j.user_id::text),
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
      'materials', (a ->> 'materials')::int);
  elsif j.kind = 'SHOE_REPAIR' then
    return v || jsonb_build_object('shoe', economy.pseudonym('shoe', a ->> 'shoe_id'), 'points', (a ->> 'points')::int);
  elsif j.kind = 'SHOE_POINTS' then
    return v || jsonb_build_object(
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'efficiency', (a ->> 'efficiency')::int, 'comfort', (a ->> 'comfort')::int, 'durability', (a ->> 'durability')::int);
  elsif j.kind = 'SHOE_TRADE' then
    return v || jsonb_build_object(
      'seller', economy.pseudonym('runner', a ->> 'seller_id'),
      'buyer', v ->> 'runner',
      'shoe', economy.pseudonym('shoe', a ->> 'shoe_id'),
      'rarity', a ->> 'rarity', 'level', (a ->> 'level')::int,
      'priceCenti', (a ->> 'price_centi')::text);
  elsif j.kind = 'CREW_RUN' then
    select * into w from public.walk_sessions s where s.id = (a ->> 'session_id')::bigint;
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
    return v || jsonb_build_object('crew', economy.pseudonym('crew', a ->> 'crew_id'));
  elsif j.kind = 'DAILY_STEPS' then
    select d.steps into v_steps from public.daily_steps d
     where d.user_id = j.user_id and d.epoch_day = (a ->> 'epoch_day')::bigint;
    if coalesce(v_steps, 0) <= 0 then
      return null;
    end if;
    return v || jsonb_build_object('steps', least(v_steps, 1000000));
  elsif j.kind = 'CHECK_IN' then
    return v;
  end if;
  return null;
end $$;
revoke all on function economy.activity_payload(public.chain_jobs) from public;

/*
 * 0045 그대로 + 새 기록(economy.activity_kinds) — 재료는 activity_payload.
 *
 * 돌려주는 payload (종류마다):
 *   RUN_PROOF   recipient · runner · run · day · distance_m · duration_sec
 *   COURSE_RUN  recipient · runner · course · run · day · distance_m · duration_sec
 *   BADGE       recipient · runner · badge · value · day
 *   VAULT_MINT  account · model_id(새 도감, 없으면 null) · faction · rarity · variant · level · efficiency_bps · comfort_bps ·
 *               durability · genesis_no · deadline_unix
 *   STATS_SYNC  token_id · level · durability · deadline_unix
 *   그 밖(0078) recipient · runner · day + 스키마 칸(eas.js SCHEMAS) — activity_payload
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
  v_wallet text;
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
      v_payload := economy.activity_payload(j);
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
      select address into v_wallet from public.wallet_links where user_id = j.user_id;
      v_payload := jsonb_build_object(
        'recipient', coalesce(v_wallet, '0x0000000000000000000000000000000000000000'),
        'runner', economy.pseudonym('runner', j.user_id::text),
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
