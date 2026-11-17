-- ════════════════════════════════════════════════════════════════════
--  0106 — 아이템: 가방 · 운영자 지급 · 러닝 아이템 · 보상 이벤트 (2026-11-17)
--
--  사용자 "지급하고 싶을 때 쉽게 · 알람도 가게" · "운영자 페이지 만들자 — 24시간 보상 2배 같은 것도 설정" ·
--  "러닝 부스터 · 에너지 드링크 · 내구도 / 하루 할당량 초기화권 · 보상 2배권" · "출발 전에 고르기 · 같은 종류는 겹쳐서 못 쓴다".
--
--  · 아이템 종류(economy.item_kinds) — 값은 표에 있어 배포 없이 바꾼다(다시 배포해도 운영 중 바꾼 값을 덮지 않는다).
--      RUN_BOOSTER       러닝 부스터        이 러닝 적립 × 1.5         묶음 REWARD · 출발 전에 고른다
--      REWARD_DOUBLE     보상 2배권         이 러닝 적립 × 2           묶음 REWARD · 출발 전에 고른다
--      ENERGY_DRINK      에너지 드링크      이 러닝 에너지를 절반만    묶음 ENERGY · 출발 전에 고른다
--      DURABILITY_RESET  내구도 초기화권    신발 내구도를 가득         바로 쓴다
--      ENERGY_RESET      하루 할당량 초기화권  오늘 쓴 에너지를 되돌린다  바로 쓴다
--    하루 할당량 = 하루 에너지다(기본 10칸 = 6,000보 — 사람들이 실제로 닿는 하루 몫). 하루 금액 상한(daily_earn_cap) · 걸음 상한 ·
--    올린 날 상한은 부정 방지 선이라 그대로 둔다.
--  · 가방(public.user_items) — 사람 · 종류마다 개수. 앱은 자기 것만 읽고, 바꾸는 길은 아래 함수뿐이다.
--  · 바로 쓰는 아이템: item_use(종류, 요청 번호, 신발). 같은 요청 번호가 다시 오면 다시 쓰지 않는다.
--  · 러닝 아이템: run_items_use(러닝 시작 시각, 종류들) — 앱이 출발할 때 고른 것을 알린다(인터넷이 없었으면 올리기 일꾼이 러닝보다
--    먼저 보낸다 — 과속 신고와 같다). 그때 가방에서 빠지고, record_session 이 그 러닝(시작 시각)에 묶인 아이템을 읽는다.
--    이미 기록한 러닝에는 붙이지 못한다. 같은 묶음은 한 러닝에 하나. 무효 러닝은 효과가 없고 돌려주지 않는다.
--  · 적립 배수는 겹치지 않는다 — 24시간 부스터(XP_BOOSTER × 2) · 보상 이벤트 · 러닝 아이템 가운데 가장 큰 것 하나
--    (economy.run_reward_mult). 예전엔 XP_BOOSTER 만 봤다.
--  · 보상 이벤트(economy.reward_events) — 운영자가 정한 시간 동안 모든 사람의 러닝 적립 × N. 러닝이 시작한 시각으로 본다.
--  · 운영자(ops_*) — 운영자 페이지(/ops)가 키로 부른다(워킹데이 검토 키와 같은 방식 — 서버에는 해시만).
--      지급: 이메일 명단 · 전체 · 최근 7일 달린 사람 → 가방에 넣고 푸시(ITEM_GRANTED). 지급 묶음마다 기록 · 되돌리기.
--      보상 이벤트: 시작 · 취소, 시작 푸시(REWARD_EVENT — 알림 설정 "이벤트 소식"을 끈 사람 제외).
--    키 만들기 · 막기(SQL Editor):
--      select economy.ops_key_new('담당자 이름');   -- 한 번만 보이는 키
--      update economy.ops_keys set revoked_at = now() where label = '담당자 이름';
--  · record_session 은 0089 본문 그대로이고 세 곳만 고쳤다(적립 배수 · 에너지 · 끝에 아이템과 러닝을 잇는 줄).
-- ════════════════════════════════════════════════════════════════════

-- ── 아이템 종류 ─────────────────────────────────────────────────────
create table if not exists economy.item_kinds (
  kind text primary key check (kind ~ '^[A-Z][A-Z_]{2,30}$'),
  -- 같은 묶음끼리는 한 러닝에 하나만 쓴다
  grp text not null,
  -- RUN 출발 전에 골라 그 러닝에만 · NOW 바로 쓴다
  mode text not null check (mode in ('RUN', 'NOW')),
  -- 배수(REWARD) · 비율(ENERGY). 바로 쓰는 아이템은 비운다
  value numeric(6, 3),
  sort int not null default 0,
  enabled boolean not null default true
);
revoke all on economy.item_kinds from public, anon, authenticated;

comment on table economy.item_kinds is
  '아이템 종류와 값. 운영 중에 값을 바꿔도 다시 배포할 때 덮지 않는다(0106).';

insert into economy.item_kinds (kind, grp, mode, value, sort) values
  ('RUN_BOOSTER',      'REWARD',     'RUN', 1.5, 10),
  ('REWARD_DOUBLE',    'REWARD',     'RUN', 2,   20),
  ('ENERGY_DRINK',     'ENERGY',     'RUN', 0.5, 30),
  ('DURABILITY_RESET', 'DURABILITY', 'NOW', null, 40),
  ('ENERGY_RESET',     'DAILY',      'NOW', null, 50)
on conflict (kind) do nothing;

-- ── 가방 ────────────────────────────────────────────────────────────
create table if not exists public.user_items (
  user_id uuid not null references auth.users on delete cascade,
  kind text not null references economy.item_kinds,
  qty int not null default 0 check (qty >= 0),
  updated_at timestamptz not null default now(),
  primary key (user_id, kind)
);

comment on table public.user_items is
  '가진 아이템. 앱은 자기 것만 읽는다 — 바꾸는 길은 item_use · run_items_use · 운영자 지급뿐이다(0106).';

alter table public.user_items enable row level security;
revoke all on public.user_items from anon, authenticated;
drop policy if exists user_items_select_own on public.user_items;
create policy user_items_select_own on public.user_items
  for select using ((select auth.uid()) = user_id);
grant select on public.user_items to authenticated;

-- ── 지급 기록 ───────────────────────────────────────────────────────
create table if not exists economy.item_batches (
  id bigint generated always as identity primary key,
  -- 같은 지급을 두 번 누르거나 응답을 잃고 다시 보내도 한 번만
  request_id uuid not null unique,
  kind text not null references economy.item_kinds,
  qty int not null check (qty between 1 and 99),
  reason text not null,
  -- 누구에게 — 'EMAILS' · 'ALL' · 'RUNNERS_7D'
  target text not null,
  actor text not null,
  push boolean not null default true,
  users int not null default 0,
  created_at timestamptz not null default now(),
  revoked_at timestamptz,
  revoked_by text
);
revoke all on economy.item_batches from public, anon, authenticated;

create table if not exists economy.item_grants (
  id bigint generated always as identity primary key,
  batch_id bigint not null references economy.item_batches on delete cascade,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null,
  qty int not null check (qty > 0),
  -- 되돌릴 때 실제로 거둔 개수(이미 쓴 것은 못 거둔다)
  revoked_qty int not null default 0,
  created_at timestamptz not null default now(),
  unique (batch_id, user_id)
);
create index if not exists item_grants_user on economy.item_grants (user_id, created_at desc);
revoke all on economy.item_grants from public, anon, authenticated;

comment on table economy.item_batches is '운영자 아이템 지급 한 번(누가 · 무엇을 · 몇 개 · 왜 · 누구에게). 되돌릴 때 이것부터 본다(0106).';
comment on table economy.item_grants is '지급 한 번에서 사람마다 받은 줄(0106).';

-- ── 쓴 기록 ─────────────────────────────────────────────────────────
create table if not exists economy.item_uses (
  id bigint generated always as identity primary key,
  user_id uuid not null references auth.users on delete cascade,
  kind text not null,
  used_at timestamptz not null default now(),
  -- 러닝 아이템이면 그 러닝의 시작 시각(앱이 올릴 때와 같은 값) — record_session 이 이것으로 찾는다
  run_started_at timestamptz,
  -- 기록된 러닝(record_session 이 적는다)
  session_id bigint,
  sneaker_id bigint
);
create unique index if not exists item_uses_run on economy.item_uses (user_id, run_started_at, kind)
  where run_started_at is not null;
create index if not exists item_uses_user on economy.item_uses (user_id, used_at desc);
revoke all on economy.item_uses from public, anon, authenticated;

comment on table economy.item_uses is '아이템을 쓴 기록. 러닝 아이템은 러닝 시작 시각에 묶인다(0106).';

-- 러닝에 적용된 적립 배수(부스터 · 이벤트 · 아이템 가운데 가장 큰 것) — 앱 결과 화면이 읽는다
alter table public.walk_sessions add column if not exists reward_mult numeric(6, 3) not null default 1;

-- ── 보상 이벤트 ─────────────────────────────────────────────────────
create table if not exists economy.reward_events (
  id bigint generated always as identity primary key,
  request_id uuid not null unique,
  title text not null,
  mult numeric(6, 3) not null check (mult > 1 and mult <= 5),
  starts_at timestamptz not null,
  ends_at timestamptz not null,
  actor text not null,
  created_at timestamptz not null default now(),
  cancelled_at timestamptz,
  cancelled_by text,
  check (ends_at > starts_at)
);
revoke all on economy.reward_events from public, anon, authenticated;

comment on table economy.reward_events is
  '보상 이벤트 — 이 시간에 시작한 러닝의 적립 × mult(모든 사람). 취소하면 그때까지만(0106).';

-- 그 시각에 켜져 있는 보상 이벤트(가장 큰 배수)
create or replace function economy.reward_event_at(p_at timestamptz)
returns economy.reward_events
language sql stable security definer set search_path = '' as $$
  select e.* from economy.reward_events e
   where e.starts_at <= p_at
     and p_at < least(e.ends_at, coalesce(e.cancelled_at, 'infinity'::timestamptz))
   order by e.mult desc, e.id desc
   limit 1
$$;
revoke all on function economy.reward_event_at(timestamptz) from public;

-- 이 러닝의 적립 배수 — 겹치지 않고 가장 큰 것 하나: 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템(묶음 REWARD)
create or replace function economy.run_reward_mult(p_user uuid, p_started_at timestamptz)
returns numeric
language sql stable security definer set search_path = public, economy as $$
  select greatest(
    1,
    case when economy.boost_active(p_user, 'XP_BOOSTER', p_started_at) then 2 else 1 end,
    coalesce((economy.reward_event_at(p_started_at)).mult, 1),
    coalesce((select max(k.value) from economy.item_uses u join economy.item_kinds k on k.kind = u.kind
               where u.user_id = p_user and u.run_started_at = p_started_at and k.grp = 'REWARD'), 1)
  )::numeric
$$;
revoke all on function economy.run_reward_mult(uuid, timestamptz) from public;

-- 이 러닝의 에너지 비율 — 에너지 드링크를 골랐으면 그 비율만큼만 쓴다
create or replace function economy.run_energy_factor(p_user uuid, p_started_at timestamptz)
returns numeric
language sql stable security definer set search_path = public, economy as $$
  select coalesce((select min(greatest(least(k.value, 1), 0.1))
                     from economy.item_uses u join economy.item_kinds k on k.kind = u.kind
                    where u.user_id = p_user and u.run_started_at = p_started_at and k.grp = 'ENERGY'), 1)::numeric
$$;
revoke all on function economy.run_energy_factor(uuid, timestamptz) from public;

-- 가방에 넣는다(서버 안에서만)
create or replace function economy.item_add(p_user uuid, p_kind text, p_qty int) returns int
language sql security definer set search_path = public, economy as $$
  insert into public.user_items (user_id, kind, qty) values (p_user, p_kind, p_qty)
  on conflict (user_id, kind) do update set qty = public.user_items.qty + excluded.qty, updated_at = now()
  returning qty
$$;
revoke all on function economy.item_add(uuid, text, int) from public;

-- ── 앱: 내 가방 ─────────────────────────────────────────────────────
-- 종류마다 개수(0 개도 — 앱이 종류와 값을 여기서 받는다) · 지금 켜진 보상 이벤트 · 24시간 부스터가 끝나는 때
create or replace function public.items_me() returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_event economy.reward_events;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  v_event := economy.reward_event_at(now());
  return jsonb_build_object(
    'items', coalesce((
      select jsonb_agg(jsonb_build_object(
               'kind', k.kind, 'group', k.grp, 'mode', k.mode, 'value', k.value,
               'qty', coalesce((select i.qty from public.user_items i where i.user_id = v_user and i.kind = k.kind), 0))
             order by k.sort, k.kind)
        from economy.item_kinds k where k.enabled), '[]'::jsonb),
    'event', case when v_event.id is null then null else jsonb_build_object(
      'id', v_event.id, 'title', v_event.title, 'mult', v_event.mult,
      'starts_at', v_event.starts_at, 'ends_at', v_event.ends_at) end,
    'boost_until', (select max(b.ends_at) from public.boosts b
                     where b.user_id = v_user and b.kind = 'XP_BOOSTER' and b.ends_at > now())
  );
end $$;
revoke all on function public.items_me() from public, anon;
grant execute on function public.items_me() to authenticated;

-- ── 앱: 바로 쓰는 아이템 ────────────────────────────────────────────
-- 돌려주는 값: { kind, left(남은 개수) } — 같은 요청 번호가 다시 오면 다시 쓰지 않고 처음 결과
create or replace function public.item_use(p_kind text, p_request uuid, p_sneaker bigint default null)
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_k economy.item_kinds;
  v_prev numeric;
  v_left int;
  v_day date := economy.game_day(now());
  v_used numeric;
  v_bonus numeric;
  v public.market_sneakers;
  v_shoe_id bigint;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_request is null then
    raise exception '요청 번호가 필요합니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  select r.result into v_prev from economy.request_receipts r
   where r.user_id = v_user and r.request_id = p_request;
  if found then
    return jsonb_build_object('kind', p_kind, 'left', v_prev::int, 'replay', true);
  end if;

  select * into v_k from economy.item_kinds k where k.kind = p_kind and k.enabled;
  if v_k.kind is null then
    raise exception '없는 아이템입니다' using errcode = '22023';
  end if;
  if v_k.mode <> 'NOW' then
    raise exception '러닝을 시작할 때 고르는 아이템입니다' using errcode = '23514';
  end if;
  if coalesce((select i.qty from public.user_items i where i.user_id = v_user and i.kind = p_kind), 0) < 1 then
    raise exception '아이템이 없습니다' using errcode = '23514';
  end if;

  if p_kind = 'DURABILITY_RESET' then
    v_shoe_id := coalesce(p_sneaker, (select e.id from economy.equipped_sneaker(v_user) e));
    if v_shoe_id is null then
      raise exception '신은 신발이 없습니다' using errcode = '23514';
    end if;
    v := economy.my_app_sneaker(v_user, v_shoe_id);
    -- 가득 찬 신발에 쓰면 아이템만 사라진다
    if v.dur_now >= v.dur_max then
      raise exception '내구도가 이미 가득합니다' using errcode = '23514';
    end if;
    update public.market_sneakers set dur_now = dur_max, updated_at = now() where id = v.id;
  elsif p_kind = 'ENERGY_RESET' then
    select d.used, d.bonus into v_used, v_bonus from public.energy_days d where d.user_id = v_user and d.day = v_day;
    if coalesce(v_used, 0) <= coalesce(v_bonus, 0) then
      raise exception '오늘 쓴 에너지가 없습니다' using errcode = '23514';
    end if;
    -- 쓴 만큼을 되돌린다 — 남은 에너지 = 최대치(economy.energy_left: 최대 + bonus − used)
    update public.energy_days set bonus = used where user_id = v_user and day = v_day;
  else
    raise exception '쓸 수 없는 아이템입니다' using errcode = '22023';
  end if;

  update public.user_items set qty = qty - 1, updated_at = now()
   where user_id = v_user and kind = p_kind
  returning qty into v_left;
  insert into economy.item_uses (user_id, kind, sneaker_id) values (v_user, p_kind, v.id);
  insert into economy.request_receipts (user_id, request_id, kind, result)
  values (v_user, p_request, 'ITEM:' || p_kind, v_left);

  return jsonb_build_object('kind', p_kind, 'left', v_left, 'replay', false);
end $$;
revoke all on function public.item_use(text, uuid, bigint) from public, anon;
grant execute on function public.item_use(text, uuid, bigint) to authenticated;

-- ── 앱: 러닝 아이템 — 출발할 때 고른 것을 그 러닝에 묶는다 ──────────
-- 같은 러닝 · 같은 종류를 다시 보내면 그대로다(다시 빼지 않는다). 돌려주는 값: { kinds, mult, energy_factor }
create or replace function public.run_items_use(p_run_started_at timestamptz, p_kinds text[])
returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_user uuid := auth.uid();
  v_kinds text[];
  v_new text[];
  v_kind text;
  v_recorded boolean;
begin
  if v_user is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_run_started_at is null
     or p_run_started_at > now() + interval '5 minutes'
     or p_run_started_at < now() - interval '7 days' then
    raise exception '러닝 시작 시각이 올바르지 않습니다' using errcode = '22023';
  end if;
  select coalesce(array_agg(distinct k), '{}') into v_kinds from unnest(coalesce(p_kinds, '{}')) k where k is not null;
  if cardinality(v_kinds) > 5 then
    raise exception '아이템이 너무 많습니다' using errcode = '22023';
  end if;
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));

  if exists (select 1 from unnest(v_kinds) k
              where not exists (select 1 from economy.item_kinds c where c.kind = k and c.enabled and c.mode = 'RUN')) then
    raise exception '러닝에 쓸 수 없는 아이템입니다' using errcode = '22023';
  end if;

  -- 이 러닝에 이미 묶인 것은 빼고
  select coalesce(array_agg(k), '{}') into v_new from unnest(v_kinds) k
   where not exists (select 1 from economy.item_uses u
                      where u.user_id = v_user and u.run_started_at = p_run_started_at and u.kind = k);

  if cardinality(v_new) > 0 then
    select exists (select 1 from public.walk_sessions s where s.user_id = v_user and s.started_at = p_run_started_at)
      into v_recorded;
    if v_recorded then
      raise exception '이미 기록한 러닝입니다' using errcode = '23514';
    end if;
    -- 같은 묶음은 한 러닝에 하나 — 이미 묶인 것과 새로 고른 것을 함께 본다
    if exists (
      select 1 from (
        select c.grp from economy.item_uses u join economy.item_kinds c on c.kind = u.kind
         where u.user_id = v_user and u.run_started_at = p_run_started_at
        union all
        select c.grp from unnest(v_new) k join economy.item_kinds c on c.kind = k
      ) g group by g.grp having count(*) > 1) then
      raise exception '같은 종류의 아이템은 함께 쓸 수 없습니다' using errcode = '23514';
    end if;
    foreach v_kind in array v_new loop
      update public.user_items set qty = qty - 1, updated_at = now()
       where user_id = v_user and kind = v_kind and qty >= 1;
      if not found then
        raise exception '아이템이 없습니다: %', v_kind using errcode = '23514';
      end if;
      insert into economy.item_uses (user_id, kind, run_started_at) values (v_user, v_kind, p_run_started_at);
    end loop;
  end if;

  return jsonb_build_object(
    'kinds', coalesce((select jsonb_agg(u.kind order by u.kind) from economy.item_uses u
                        where u.user_id = v_user and u.run_started_at = p_run_started_at), '[]'::jsonb),
    'mult', economy.run_reward_mult(v_user, p_run_started_at),
    'energy_factor', economy.run_energy_factor(v_user, p_run_started_at));
end $$;
revoke all on function public.run_items_use(timestamptz, text[]) from public, anon;
grant execute on function public.run_items_use(timestamptz, text[]) to authenticated;

-- ── 푸시: 이벤트 소식을 끈 사람에게는 보상 이벤트를 알리지 않는다 ───
create or replace function public.push_allowed(p_user uuid, p_kind text)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce((
    select n.push and case p_kind
      when 'PARTY_OPEN' then n.party_invite
      when 'CREW_FLASH' then n.event_news
      when 'REWARD_EVENT' then n.event_news
      else true
    end
    from public.notify_prefs n where n.user_id = p_user
  ), true)
$$;

-- ════════════════════════════════════════════════════════════════════
--  운영자 — 운영자 페이지(/ops)가 키로 부른다. 서버에는 키의 해시만 있다
-- ════════════════════════════════════════════════════════════════════
create table if not exists economy.ops_keys (
  token_hash text primary key,
  label text not null,
  created_at timestamptz not null default now(),
  revoked_at timestamptz
);
revoke all on economy.ops_keys from public, anon, authenticated;

comment on table economy.ops_keys is '운영자 페이지 키(해시만). economy.ops_key_new 로 만들고 revoked_at 으로 막는다(0106).';

create or replace function economy.ops_key_new(p_label text) returns text
language plpgsql security definer set search_path = '' as $$
declare v_token text := replace(gen_random_uuid()::text, '-', '') || replace(gen_random_uuid()::text, '-', '');
begin
  insert into economy.ops_keys (token_hash, label)
  values (encode(sha256(convert_to(v_token, 'UTF8')), 'hex'), coalesce(nullif(btrim(p_label), ''), 'ops'));
  return v_token;
end $$;
revoke all on function economy.ops_key_new(text) from public;

-- 키가 맞으면 그 이름, 아니면 예외
create or replace function economy.ops_actor(p_token text) returns text
language plpgsql stable security definer set search_path = '' as $$
declare v_label text;
begin
  select label into v_label from economy.ops_keys
   where token_hash = encode(sha256(convert_to(coalesce(p_token, ''), 'UTF8')), 'hex') and revoked_at is null;
  if v_label is null then
    raise exception 'ops key required' using errcode = '42501';
  end if;
  return v_label;
end $$;
revoke all on function economy.ops_actor(text) from public;

create or replace function economy.reward_event_json(e economy.reward_events) returns jsonb
language sql stable set search_path = '' as $$
  select jsonb_build_object(
    'id', e.id, 'title', e.title, 'mult', e.mult, 'starts_at', e.starts_at, 'ends_at', e.ends_at,
    'actor', e.actor, 'created_at', e.created_at, 'cancelled_at', e.cancelled_at,
    'state', case when e.cancelled_at is not null then 'CANCELLED'
                  when now() < e.starts_at then 'SCHEDULED'
                  when now() < e.ends_at then 'LIVE' else 'ENDED' end)
$$;
revoke all on function economy.reward_event_json(economy.reward_events) from public;

-- 한눈에: 아이템 종류(가진 개수 합 · 가진 사람 수) · 지금 이벤트 · 최근 이벤트 · 최근 지급
create or replace function public.ops_overview(p_token text) returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_event economy.reward_events;
begin
  v_event := economy.reward_event_at(now());
  return jsonb_build_object(
    'actor', v_actor,
    'now', now(),
    'users', (select count(*) from public.profiles),
    'runners_7d', (select count(distinct s.user_id) from public.walk_sessions s
                    where s.started_at > now() - interval '7 days' and s.verdict <> 'VOID'),
    'kinds', coalesce((
      select jsonb_agg(jsonb_build_object(
               'kind', k.kind, 'group', k.grp, 'mode', k.mode, 'value', k.value, 'enabled', k.enabled,
               'held', coalesce((select sum(i.qty) from public.user_items i where i.kind = k.kind), 0),
               'holders', (select count(*) from public.user_items i where i.kind = k.kind and i.qty > 0),
               'used', (select count(*) from economy.item_uses u where u.kind = k.kind))
             order by k.sort, k.kind)
        from economy.item_kinds k), '[]'::jsonb),
    'event', case when v_event.id is null then null else economy.reward_event_json(v_event) end,
    'events', coalesce((
      select jsonb_agg(economy.reward_event_json(e) order by e.id desc)
        from (select * from economy.reward_events order by id desc limit 12) e), '[]'::jsonb),
    'batches', coalesce((
      select jsonb_agg(jsonb_build_object(
               'id', b.id, 'kind', b.kind, 'qty', b.qty, 'reason', b.reason, 'target', b.target,
               'actor', b.actor, 'users', b.users, 'push', b.push, 'created_at', b.created_at,
               'revoked_at', b.revoked_at) order by b.id desc)
        from (select * from economy.item_batches order by id desc limit 30) b), '[]'::jsonb)
  );
end $$;
revoke all on function public.ops_overview(text) from public;
grant execute on function public.ops_overview(text) to anon, authenticated;

-- 사람 찾기 — 이메일 · 이름 일부(2자 이상), 20명까지. 가진 아이템도 함께
create or replace function public.ops_user_find(p_token text, p_query text) returns jsonb
language plpgsql stable security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_q text := btrim(coalesce(p_query, ''));
  v_like text;
begin
  if char_length(v_q) < 2 then
    return '[]'::jsonb;
  end if;
  v_like := '%' || replace(replace(replace(v_q, '\', '\\'), '%', '\%'), '_', '\_') || '%';
  return coalesce((
    select jsonb_agg(jsonb_build_object(
             'email', x.email, 'name', x.name, 'joined_at', x.created_at,
             'items', coalesce((select jsonb_object_agg(i.kind, i.qty) from public.user_items i
                                 where i.user_id = x.id and i.qty > 0), '{}'::jsonb))
           order by x.created_at desc)
      from (
        select u.id, u.email, coalesce(p.display_name, '') as name, u.created_at
          from auth.users u join public.profiles p on p.id = u.id
         where u.email ilike v_like or p.display_name ilike v_like
         order by u.created_at desc
         limit 20
      ) x), '[]'::jsonb);
end $$;
revoke all on function public.ops_user_find(text, text) from public;
grant execute on function public.ops_user_find(text, text) to anon, authenticated;

-- 지급 — 이메일 명단(p_emails) 또는 대상(p_audience: 'ALL' 전체 · 'RUNNERS_7D' 최근 7일 달린 사람).
-- 같은 요청 번호가 다시 오면 다시 주지 않고 처음 결과. 못 찾은 이메일은 missing 으로 돌려준다(나머지는 지급한다).
create or replace function public.ops_item_grant(
  p_token text,
  p_request uuid,
  p_kind text,
  p_qty int,
  p_reason text,
  p_emails text[] default null,
  p_audience text default null,
  p_push boolean default true
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_reason text := btrim(coalesce(p_reason, ''));
  v_emails text[];
  v_users uuid[];
  v_missing text[] := '{}';
  v_target text;
  v_batch economy.item_batches;
  v_user uuid;
begin
  if p_request is null then
    return jsonb_build_object('ok', false, 'code', 'request', 'message', '요청 번호가 필요합니다');
  end if;
  -- 같은 지급을 두 번 누르지 못하게 — 요청 번호로 줄을 선다
  perform pg_advisory_xact_lock(hashtext('ops-grant:' || p_request::text));
  select * into v_batch from economy.item_batches b where b.request_id = p_request;
  if found then
    return jsonb_build_object('ok', true, 'batch', v_batch.id, 'users', v_batch.users, 'missing', '[]'::jsonb, 'replay', true);
  end if;

  if not exists (select 1 from economy.item_kinds k where k.kind = p_kind) then
    return jsonb_build_object('ok', false, 'code', 'kind', 'message', '없는 아이템입니다');
  end if;
  if p_qty is null or p_qty < 1 or p_qty > 99 then
    return jsonb_build_object('ok', false, 'code', 'qty', 'message', '개수는 1 ~ 99');
  end if;
  if char_length(v_reason) < 2 or char_length(v_reason) > 80 then
    return jsonb_build_object('ok', false, 'code', 'reason', 'message', '사유를 2 ~ 80자로 적어 주세요');
  end if;

  if p_audience = 'ALL' then
    v_target := 'ALL';
    select coalesce(array_agg(p.id), '{}') into v_users from public.profiles p;
  elsif p_audience = 'RUNNERS_7D' then
    v_target := 'RUNNERS_7D';
    select coalesce(array_agg(distinct s.user_id), '{}') into v_users from public.walk_sessions s
     where s.started_at > now() - interval '7 days' and s.verdict <> 'VOID';
  elsif p_audience is null or p_audience = 'EMAILS' then
    v_target := 'EMAILS';
    select coalesce(array_agg(distinct lower(btrim(e))), '{}') into v_emails
      from unnest(coalesce(p_emails, '{}')) e where btrim(coalesce(e, '')) <> '';
    if cardinality(v_emails) > 2000 then
      return jsonb_build_object('ok', false, 'code', 'too_many', 'message', '한 번에 2,000명까지');
    end if;
    select coalesce(array_agg(u.id), '{}') into v_users
      from auth.users u join public.profiles p on p.id = u.id
     where lower(u.email) = any (v_emails);
    select coalesce(array_agg(e order by e), '{}') into v_missing from unnest(v_emails) e
     where not exists (select 1 from auth.users u join public.profiles p on p.id = u.id where lower(u.email) = e);
  else
    return jsonb_build_object('ok', false, 'code', 'audience', 'message', '대상이 올바르지 않습니다');
  end if;

  if cardinality(v_users) = 0 then
    return jsonb_build_object('ok', false, 'code', 'no_target', 'message', '받을 사람이 없습니다', 'missing', to_jsonb(v_missing));
  end if;

  insert into economy.item_batches (request_id, kind, qty, reason, target, actor, push, users)
  values (p_request, p_kind, p_qty, v_reason, v_target, v_actor, coalesce(p_push, true), cardinality(v_users))
  returning * into v_batch;

  foreach v_user in array v_users loop
    perform economy.item_add(v_user, p_kind, p_qty);
    insert into economy.item_grants (batch_id, user_id, kind, qty) values (v_batch.id, v_user, p_kind, p_qty);
    if coalesce(p_push, true) then
      perform public.push_enqueue(v_user, 'ITEM_GRANTED',
        jsonb_build_object('item', p_kind, 'qty', p_qty::text, 'reason', v_reason), 'items');
    end if;
  end loop;

  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_item_grant', p_kind,
          jsonb_build_object('by', v_actor, 'batch', v_batch.id, 'qty', p_qty, 'users', cardinality(v_users),
                             'target', v_target, 'reason', v_reason));

  return jsonb_build_object('ok', true, 'batch', v_batch.id, 'users', cardinality(v_users),
                            'missing', to_jsonb(v_missing), 'replay', false);
end $$;
revoke all on function public.ops_item_grant(text, uuid, text, int, text, text[], text, boolean) from public;
grant execute on function public.ops_item_grant(text, uuid, text, int, text, text[], text, boolean) to anon, authenticated;

-- 지급 되돌리기 — 아직 갖고 있는 만큼만 거둔다(이미 쓴 것은 못 거둔다). taken 거둔 개수 · short 덜 거둔 사람 수
create or replace function public.ops_grant_revoke(p_token text, p_batch bigint) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_batch economy.item_batches;
  g record;
  v_take int;
  v_taken int := 0;
  v_short int := 0;
begin
  select * into v_batch from economy.item_batches b where b.id = p_batch for update;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'not_found', 'message', '없는 지급입니다');
  end if;
  if v_batch.revoked_at is not null then
    return jsonb_build_object('ok', false, 'code', 'revoked', 'message', '이미 되돌린 지급입니다');
  end if;
  for g in select * from economy.item_grants x where x.batch_id = p_batch loop
    perform pg_advisory_xact_lock(hashtext('ledger:' || g.user_id::text));
    select least(g.qty - g.revoked_qty, coalesce(i.qty, 0)) into v_take
      from (select 1) one left join public.user_items i on i.user_id = g.user_id and i.kind = g.kind;
    v_take := greatest(coalesce(v_take, 0), 0);
    if v_take > 0 then
      update public.user_items set qty = qty - v_take, updated_at = now()
       where user_id = g.user_id and kind = g.kind;
      update economy.item_grants set revoked_qty = revoked_qty + v_take where id = g.id;
    end if;
    v_taken := v_taken + v_take;
    if v_take < g.qty - g.revoked_qty then
      v_short := v_short + 1;
    end if;
  end loop;
  update economy.item_batches set revoked_at = now(), revoked_by = v_actor where id = p_batch;
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_grant_revoke', v_batch.kind,
          jsonb_build_object('by', v_actor, 'batch', p_batch, 'taken', v_taken, 'short', v_short));
  return jsonb_build_object('ok', true, 'taken', v_taken, 'short', v_short);
end $$;
revoke all on function public.ops_grant_revoke(text, bigint) from public;
grant execute on function public.ops_grant_revoke(text, bigint) to anon, authenticated;

-- 보상 이벤트 시작 — 지금부터(또는 p_starts_at 부터) p_hours 시간 동안 러닝 적립 × p_mult.
-- 켜져 있거나 예약된 이벤트와 겹치면 거절한다(배수는 겹치지 않는다). 지금 시작하는 이벤트만 푸시를 보낸다.
create or replace function public.ops_event_start(
  p_token text,
  p_request uuid,
  p_title text,
  p_mult numeric,
  p_hours numeric,
  p_starts_at timestamptz default null,
  p_push boolean default true
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_title text := btrim(coalesce(p_title, ''));
  v_from timestamptz := coalesce(p_starts_at, now());
  v_to timestamptz;
  v_event economy.reward_events;
  v_user uuid;
  v_pushed int := 0;
begin
  if p_request is null then
    return jsonb_build_object('ok', false, 'code', 'request', 'message', '요청 번호가 필요합니다');
  end if;
  perform pg_advisory_xact_lock(hashtext('ops-event'));
  select * into v_event from economy.reward_events e where e.request_id = p_request;
  if found then
    return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event), 'replay', true);
  end if;

  if char_length(v_title) < 2 or char_length(v_title) > 40 then
    return jsonb_build_object('ok', false, 'code', 'title', 'message', '이름을 2 ~ 40자로 적어 주세요');
  end if;
  if p_mult is null or p_mult <= 1 or p_mult > 5 then
    return jsonb_build_object('ok', false, 'code', 'mult', 'message', '배수는 1 보다 크고 5 이하');
  end if;
  if p_hours is null or p_hours < 1 or p_hours > 168 then
    return jsonb_build_object('ok', false, 'code', 'hours', 'message', '기간은 1 ~ 168시간');
  end if;
  if v_from < now() - interval '1 minute' then
    return jsonb_build_object('ok', false, 'code', 'past', 'message', '지난 시각에는 시작할 수 없습니다');
  end if;
  if v_from > now() + interval '30 days' then
    return jsonb_build_object('ok', false, 'code', 'far', 'message', '30일 안으로 정해 주세요');
  end if;
  v_to := v_from + make_interval(secs => (p_hours * 3600)::double precision);

  if exists (select 1 from economy.reward_events e
              where e.cancelled_at is null and e.ends_at > now()
                and e.starts_at < v_to and e.ends_at > v_from) then
    return jsonb_build_object('ok', false, 'code', 'overlap', 'message', '그 시간에 이미 보상 이벤트가 있습니다');
  end if;

  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (p_request, v_title, round(p_mult, 2), v_from, v_to, v_actor)
  returning * into v_event;

  if coalesce(p_push, true) and v_from <= now() + interval '1 minute' then
    for v_user in select p.id from public.profiles p loop
      perform public.push_enqueue(v_user, 'REWARD_EVENT',
        jsonb_build_object('mult', trim(trailing '.' from trim(trailing '0' from round(p_mult, 2)::text)),
                           'hours', trim(trailing '.' from trim(trailing '0' from round(p_hours, 1)::text)),
                           'title', v_title), 'run');
      v_pushed := v_pushed + 1;
    end loop;
  end if;

  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_event_start', v_event.id::text,
          jsonb_build_object('by', v_actor, 'title', v_title, 'mult', v_event.mult,
                             'starts_at', v_from, 'ends_at', v_to));

  return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event), 'replay', false);
end $$;
revoke all on function public.ops_event_start(text, uuid, text, numeric, numeric, timestamptz, boolean) from public;
grant execute on function public.ops_event_start(text, uuid, text, numeric, numeric, timestamptz, boolean) to anon, authenticated;

-- 보상 이벤트 취소 — 지금부터 꺼진다(이미 시작한 러닝은 시작 시각으로 보므로 그대로 받는다)
create or replace function public.ops_event_cancel(p_token text, p_id bigint) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_actor text := economy.ops_actor(p_token);
  v_event economy.reward_events;
begin
  update economy.reward_events e set cancelled_at = now(), cancelled_by = v_actor
   where e.id = p_id and e.cancelled_at is null and e.ends_at > now()
  returning * into v_event;
  if not found then
    return jsonb_build_object('ok', false, 'code', 'not_found', 'message', '끌 이벤트가 없습니다');
  end if;
  insert into public.admin_audit (actor, action, target, detail)
  values (null, 'ops_event_cancel', p_id::text, jsonb_build_object('by', v_actor));
  return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event));
end $$;
revoke all on function public.ops_event_cancel(text, bigint) from public;
grant execute on function public.ops_event_cancel(text, bigint) to anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  record_session — 0089 본문 그대로, 세 곳만:
--    ① 적립 배수 = economy.run_reward_mult(24시간 부스터 · 보상 이벤트 · 러닝 아이템 가운데 가장 큰 것)
--    ② 에너지 = 걸음마다 쓰는 양 × economy.run_energy_factor(에너지 드링크)
--    ③ 끝에 이 러닝에 묶인 아이템과 적용된 배수를 적는다
--  칸 · 돌려주는 값이 같아 create or replace 로 바꾼다(0089 가 먼저 지우고 만든 그 함수).
-- ════════════════════════════════════════════════════════════════════

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
  verdict_reason text
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
  -- 서버가 이미 끝낸 참여(마지막 한 사람 자동 종료 · 끝냄)면 그때 + 60초(폰 시계 차이) 뒤의 기록은 크루 보상 · 경험치에서 뺀다
  v_cut timestamptz;
  v_xp_frac numeric := 1;
  v_level_now int;
  v_level_was int;
  v_starters int := 1;
  v_xp_m numeric := 0;
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
  select s.id, s.verdict, s.points_awarded, coalesce(s.verdict_reason, '') into v_session_id, v_verdict, v_points, v_reason
    from public.walk_sessions s
   where s.user_id = v_user and s.started_at = p_started_at;
  if found then
    return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
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

  -- ── 함께 달리기 · 크루 러닝 (0070 — 걸음 상한 · 신발보다 먼저 정한다) ──
  -- 파티 보너스는 한 파티에서 한 러닝만
  select pf.party_id, pf.size into v_party_id, v_party from economy.party_for(v_user, p_started_at) pf;
  if v_party_id is null or exists (select 1 from public.walk_sessions s
                                    where s.user_id = v_user and s.party_id = v_party_id) then
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
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
  end if;

  -- 적립 배수 — 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템 가운데 가장 큰 것 하나(겹치지 않는다, 0106)
  v_xp := economy.run_reward_mult(v_user, p_started_at);

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    v_base := round(v_rewardable * economy.points_per_step() * (1 + v_eff / 10000.0) * v_xp, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
  else
    v_points := round(
      v_rewardable * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * economy.durability_factor(v_dur)
        * economy.party_multiplier(v_party)
        * v_xp,
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
    v_cap := economy.setting_num('daily_earn_cap') * case when v_new_account then 0.5 else 1 end;
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
  if v_points > 0 and not v_gps_backed and v_pool = 'PERSONAL' then
    select economy.setting_num('no_gps_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and not s.gps_backed and s.energy_pool <> 'CREW'
       and economy.game_day(s.started_at) = v_gday;
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '경로 없는 러닝의 하루 상한에 걸렸습니다');
    end if;
  end if;
  if v_points > 0 and v_pool = 'PERSONAL' then
    select economy.setting_num('upload_daily_cap') - coalesce(sum(s.points_awarded), 0) into v_cap_left
      from public.walk_sessions s
     where s.user_id = v_user and s.energy_pool <> 'CREW'
       and economy.game_day(s.created_at) = economy.game_day(now());
    if v_points > v_cap_left then
      v_points := greatest(round(v_cap_left, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루에 올릴 수 있는 적립 상한에 걸렸습니다');
    end if;
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

  -- ── 기록 ──
  insert into public.walk_sessions (
    user_id, started_at, ended_at, duration_sec, steps,
    distance_meters, calories, track, boost_bps, party_size,
    faction, top_speed_kmh, gps_distance_m,
    verdict, verdict_reason, points_awarded, rewarded_steps,
    mock_location, verified_steps, energy_used, sneaker_id,
    gps_backed, backed_steps, gps_credit_m, party_id,
    crew_id, energy_pool
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool
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
  if v_shoe.id is not null and v_verdict <> 'VOID' then
    update public.market_sneakers s
       set km_run = s.km_run + round((v_credit_m / 1000)::numeric, 3),
           dur_now = case when v_pool = 'CREW' then s.dur_now
                          else greatest(s.dur_now - economy.durability_wear_time(v_elapsed), 0) end,
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

  return query select v_session_id, v_verdict, v_points, economy.balance_of(v_user), v_reason;
end;
$$;


comment on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) is
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089)';

revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint)
  to authenticated;
