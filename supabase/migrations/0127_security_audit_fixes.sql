-- 0127 — 보안 점검 고침(서버 · DB) (2026-11-24, 사용자 "보안 관련해서는 빡세게 3번 이상 점검해서 다 수행")
--   1. leaderboard: 로그인한 사람만 · 100명까지(익명으로 모든 사용자 · 누적 SUP 를 한 번에 받던 것)
--   2. 크루 영역: 출발 · 도착 300m 칸은 칠하지 않는다 · 크루원 셋 이상인 크루만 보인다 · 이미 칠한 칸도 한 번 정리
--   3. 끝난 11.11 워킹데이의 받는 문(초대 링크 · 걸음 인증 · SNS 제출)을 닫는다 — 결과 · 검토는 그대로
--   4. 같은 곳 횟수(IP 해시)는 IPv6 를 /64 로 묶는다(주소를 바꿔 가며 횟수 제한을 피하던 것)
--   5. 새 계정 이름: 구글이 준 이름만 · 16자 · 제어 문자 없이(API 가입으로 긴 이름 · 운영자 흉내)
--   6. market_import: 예전 앱의 네 등급 · 레벨 20 까지만
--   7. 앱 역할의 TRUNCATE 권한을 거둔다(쓸 길은 없지만 정리)

-- ── 1 ──
create or replace function public.leaderboard(
  p_board text default 'TOP_SPEED',
  p_limit int default 20,
  p_period text default 'ALL'
)
returns table (
  rank int,
  user_id uuid,
  name text,
  monogram text,
  top_speed_kmh double precision,
  active_sec bigint,
  sup numeric,
  is_me boolean,
  total int
)
language sql
stable
security definer
set search_path = public
as $$
  with win as (
    select public.rank_period_start(p_period) as since
  ),
  board as (
    select upper(coalesce(nullif(trim(p_board), ''), 'TOP_SPEED')) as kind
  ),
  stats as (
    select
      p.id as user_id,
      p.display_name,
      -- 창 안의 세션 중 가장 빨랐던 구간. 전체기간일 때만 프로필에 적힌
      -- 역대 최고와 견준다 — 세션 표가 생기기 전에 세워진 기록도 있다.
      greatest(
        coalesce(a.top_speed_kmh, 0),
        case
          when (select since from win) = '-infinity'::timestamptz then p.top_speed_kmh
          else 0
        end
      )::double precision as top_speed_kmh,
      coalesce(a.active_sec, 0)::bigint as active_sec,
      coalesce(l.earned, 0)::numeric(20, 4) as sup
    from public.profiles p
    left join (
      select
        s.user_id,
        max(s.top_speed_kmh) as top_speed_kmh,
        -- 걸음 하나에 1초까지만 — 걸음 없이 며칠짜리 러닝을 올려 "가장 오래"를 차지하지 못하게
        sum(least(s.duration_sec, s.steps)) as active_sec
      from public.walk_sessions s
      -- 판정에서 떨어진 세션은 순위에 쓰지 않는다. 적립은 막아 놓고 순위는
      -- 올려 주면, 순위표는 막지 않은 쪽으로 뚫린다.
      where s.verdict <> 'VOID'
        and s.started_at >= (select since from win)
      group by s.user_id
    ) a on a.user_id = p.id
    left join (
      -- 누적 "적립"이다. 잔고가 아니다 — 쓴 사람이 순위에서 밀리면
      -- 상점은 아무도 안 쓰는 방이 된다.
      -- 번 것만 센다. 입찰을 걸었다 거두면 ESCROW_UNLOCK(+)이 적히고, 팔면
      -- TRADE_SELL(+)이 적힌다 — 이것까지 세면 입찰·취소를 되풀이해 공짜로 오른다.
      select user_id, sum(amount) filter (
               where amount > 0 and kind in ('EARN_WALK', 'EARN_PARTY', 'EARN_EVENT', 'BONUS_GOAL', 'EARN_COURSE')
             ) as earned
        from public.sup_ledger
       where occurred_at >= (select since from win)
       group by user_id
    ) l on l.user_id = p.id
  ),
  ranked as (
    select
      s.*,
      count(*) over ()::int as total,
      rank() over (
        order by
          case (select kind from board)
            when 'LONGEST_TIME' then s.active_sec::numeric
            when 'TOTAL_SUP' then s.sup
            else s.top_speed_kmh::numeric
          end desc,
          -- 같은 값이면 이름순. 순서가 매번 흔들리면 순위표를 믿지 않게 된다.
          s.display_name,
          s.user_id
      )::int as rnk
    from stats s
    -- 이 기간에 아무것도 안 한 사람은 순위에 넣지 않는다. 0 으로 채운 줄이
    -- 수백 개면 순위표가 아니라 가입자 명단이다.
    where s.active_sec > 0 or s.sup > 0 or s.top_speed_kmh > 0
  )
  select
    r.rnk,
    r.user_id,
    r.display_name,
    public.monogram_of(r.display_name),
    r.top_speed_kmh,
    r.active_sec,
    r.sup,
    r.user_id = auth.uid(),
    r.total
  from ranked r
  where r.rnk <= least(greatest(coalesce(p_limit, 20), 1), 100)  -- 0127: 100명까지
     or r.user_id = auth.uid()
  order by r.rnk
$$;

revoke all on function public.leaderboard(text, int, text) from public, anon;
grant execute on function public.leaderboard(text, int, text) to authenticated;

-- ── 2 ──
insert into public.economy_settings (key, value) values ('territory_min_members', '3') on conflict (key) do nothing;
create or replace function economy.near_m(a_lat double precision, a_lng double precision, b_lat double precision, b_lng double precision)
returns double precision language sql immutable set search_path = '' as $$
  select sqrt(power((a_lat - b_lat) * 111320.0, 2) + power((a_lng - b_lng) * 111320.0 * cos(radians((a_lat + b_lat) / 2)), 2))
$$;
revoke all on function economy.near_m(double precision, double precision, double precision, double precision) from public, anon, authenticated;

-- 경로의 처음 · 마지막 점("위도,경도,…;…") — 읽을 수 없으면 null
create or replace function economy.track_ends(p_track text, out lat1 double precision, out lng1 double precision,
                                              out lat2 double precision, out lng2 double precision)
language plpgsql immutable set search_path = '' as $$
declare
  v text[] := string_to_array(coalesce(p_track, ''), ';');
  p text[];
  i int;
  n int := coalesce(array_length(v, 1), 0);
  num constant text := '^-?[0-9]+(\.[0-9]+)?$';
begin
  for i in 1 .. n loop
    p := string_to_array(v[i], ',');
    if array_length(p, 1) >= 2 and p[1] ~ num and p[2] ~ num then lat1 := p[1]::double precision; lng1 := p[2]::double precision; exit; end if;
  end loop;
  for i in reverse n .. 1 loop
    p := string_to_array(v[i], ',');
    if array_length(p, 1) >= 2 and p[1] ~ num and p[2] ~ num then lat2 := p[1]::double precision; lng2 := p[2]::double precision; exit; end if;
  end loop;
end $$;
revoke all on function economy.track_ends(text) from public, anon, authenticated;

create or replace function public.territory_credit(p_session bigint)
returns int
language plpgsql
security definer
set search_path = public, economy
as $$
declare
  v_s public.walk_sessions%rowtype;
  v_crew uuid;
  v_day bigint;
  v_chunk text;
  v_parts text[];
  v_cell text;
  v_cells text[] := '{}';
  v_cap int;
  v_n int := 0;
  v_lat double precision;
  v_lng double precision;
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_ends record;
begin
  select * into v_s from public.walk_sessions where id = p_session;
  if not found or v_s.verdict = 'VOID' or v_s.track = '' then
    return 0;
  end if;

  v_crew := coalesce(v_s.crew_id, public.home_crew(v_s.user_id));
  if v_crew is null then
    return 0;
  end if;

  -- 이미 칠한 러닝이다(크루를 나중에 적은 경우) — 크루만 바꾼다.
  if exists (select 1 from public.territory_marks t where t.session_id = p_session) then
    update public.territory_marks set crew_id = v_crew where session_id = p_session;
    return 0;
  end if;

  v_day := floor(extract(epoch from v_s.started_at) / 86400)::bigint;
  -- 칸 수 상한 — 경로 길이로 지날 수 있는 만큼만. 150m 칸이면 100m 마다 한 칸이 넉넉하다.
  v_cap := greatest(1, (greatest(v_s.gps_distance_m, 0) / 100)::int + 3);

  -- 0127: 출발 · 도착 300m 안의 칸은 칠하지 않는다(이야기 · 크루 러닝 지도와 같은 까닭 — 집 · 회사 자리)
  select * into v_ends from economy.track_ends(v_s.track);
  foreach v_chunk in array string_to_array(v_s.track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    continue when v_lat not between -85 and 85 or v_lng not between -180 and 180;
    continue when v_ends.lat1 is not null and (economy.near_m(v_lat, v_lng, v_ends.lat1, v_ends.lng1) < 300
                                               or economy.near_m(v_lat, v_lng, v_ends.lat2, v_ends.lng2) < 300);
    v_cell := economy.hex_cell(v_lat, v_lng);
    continue when v_cell = any(v_cells);
    exit when coalesce(array_length(v_cells, 1), 0) >= v_cap;
    v_cells := v_cells || v_cell;
  end loop;

  foreach v_cell in array v_cells loop
    insert into public.territory_marks (cell, user_id, day, crew_id, session_id, lat, lng)
    select v_cell, v_s.user_id, v_day, v_crew, p_session, c.lat, c.lng
      from economy.hex_center(v_cell) c
    on conflict (cell, user_id, day) do nothing;
    if found then
      v_n := v_n + 1;
    end if;
  end loop;
  return v_n;
end;
$$;

revoke execute on function public.territory_credit(bigint) from public, anon, authenticated;

create or replace function public.territory_view(
  p_min_lat double precision,
  p_min_lng double precision,
  p_max_lat double precision,
  p_max_lng double precision
)
returns table (
  cell text,
  lat double precision,
  lng double precision,
  crew_id uuid,
  crew_name text,
  score int,
  mine boolean
)
language plpgsql
stable
security definer
set search_path = public, economy
as $$
declare
  v_since bigint := floor(extract(epoch from now()) / 86400)::bigint - economy.territory_window_days();
begin
  if auth.uid() is null then
    raise exception '로그인이 필요합니다' using errcode = '28000';
  end if;
  if p_max_lat - p_min_lat > 0.3 or p_max_lng - p_min_lng > 0.3
     or p_max_lat < p_min_lat or p_max_lng < p_min_lng then
    raise exception '지도를 더 확대해 주세요' using errcode = '22023';
  end if;

  return query
    with counts as (
      select t.cell, t.crew_id, count(*)::int as n, max(t.day) as last_day,
             min(t.lat) as lat, min(t.lng) as lng
        from public.territory_marks t
       where t.day >= v_since
         and t.lat between p_min_lat - 0.002 and p_max_lat + 0.002
         and t.lng between p_min_lng - 0.002 and p_max_lng + 0.002
         and not public.is_hidden('CREW', t.crew_id::text)
         -- 0127: 남의 크루는 크루원이 셋 이상일 때만 — 혼자 · 둘인 크루는 칸이 곧 그 사람의 길(집)이다
         and ((select count(*) from public.crew_members m where m.crew_id = t.crew_id) >= coalesce(economy.setting_num('territory_min_members'), 3)
              or exists (select 1 from public.crew_members m where m.crew_id = t.crew_id and m.user_id = auth.uid()))  -- 내 크루는 작아도 보인다
       group by t.cell, t.crew_id
    ),
    owner as (
      select distinct on (k.cell) k.*
        from counts k
       order by k.cell, k.n desc, k.last_day desc, k.crew_id
    )
    select o.cell, o.lat, o.lng, o.crew_id, c.name, o.n,
           exists (select 1 from public.crew_members m
                    where m.crew_id = o.crew_id and m.user_id = auth.uid())
      from owner o
      join public.crews c on c.id = o.crew_id
     limit 3000;
end;
$$;


-- 한 번만: 이미 칠한 칸 가운데 그 러닝의 출발 · 도착 300m(칸 반지름만큼 넉넉히 375m) 안의 칸을 지운다
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'territory_trim_20261124') then
    delete from public.territory_marks t
     using public.walk_sessions s, lateral economy.track_ends(s.track) e
     where s.id = t.session_id and s.id in (select distinct m.session_id from public.territory_marks m) and e.lat1 is not null
       and (economy.near_m(t.lat, t.lng, e.lat1, e.lng1) < 375 or economy.near_m(t.lat, t.lng, e.lat2, e.lng2) < 375);
    insert into public.economy_settings (key, value) values ('territory_trim_20261124', to_jsonb(now()));
  end if;
end $$;

-- ── 3 ── (이벤트 끝 — 결과 · 당첨 · 검토 함수는 그대로)
revoke all on function public.walkingday_invite_link(text, text, text, boolean) from public, anon, authenticated;
revoke all on function public.walkingday_proof_submit(text, int, int, text, boolean, text, text) from public, anon, authenticated;
revoke all on function public.walkingday_sns_submit(text, text, text, boolean, text, text) from public, anon, authenticated;

-- ── 4 ──
create or replace function economy.waitlist_client_of(p_ip text) returns text
  language plpgsql stable security definer set search_path = '' as $$
declare
  v_ip text := nullif(btrim(coalesce(p_ip, '')), '');
begin
  if v_ip is null then
    return null;
  end if;
  -- 0127: IPv6 는 한 집 · 한 기기가 /64 를 통째로 받는다 — 그 안에서 주소를 바꿔 횟수 제한을 피하지 못하게 /64 로 묶는다
  begin
    if position(':' in v_ip) > 0 then
      if lower(host(v_ip::inet)) like '::ffff:%.%' then
        v_ip := substr(host(v_ip::inet), 8);   -- IPv4 를 IPv6 꼴로 적은 주소(::ffff:1.2.3.4)는 그 IPv4 로
      else
        v_ip := host(network(set_masklen(v_ip::inet, 64)));
      end if;
    end if;
  exception when others then
    null;   -- 읽을 수 없는 값이면 글자 그대로
  end;
  return encode(sha256(convert_to(coalesce((select s.value from economy.app_secrets s where s.name = 'client_salt'), '')
                                  || ':stepup-waitlist:' || v_ip, 'UTF8')), 'hex');
end $$;
revoke all on function economy.waitlist_client_of(text) from public;

-- ── 5 ──
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name text;
begin
  -- 구글 로그인이면 구글이 준 이름(16자 · 제어 문자 없이). 그 밖(이메일 가입 등)은 사용자가 마음대로 적어 보낸 값이라 쓰지 않는다(0127)
  if coalesce(new.raw_app_meta_data ->> 'provider', '') = 'google' then
    v_name := left(btrim(regexp_replace(coalesce(new.raw_user_meta_data ->> 'full_name', ''), '[[:cntrl:]]', '', 'g')), 16);
  end if;
  insert into public.profiles (id, display_name)
  values (new.id, coalesce(nullif(v_name, ''), '러너'))
  on conflict (id) do nothing;
  return new;
end;
$$;

-- ── 6 ──
create or replace function public.market_import(
  p_local_id bigint,
  p_faction text,
  p_rarity text,
  p_variant int,
  p_level int,
  p_luck numeric,
  p_comfort numeric,
  p_durability int
) returns bigint
language plpgsql security definer set search_path = public as $$
declare
  v_user uuid := public.market_me();
  v_id bigint;
  v_count int;
begin
  perform pg_advisory_xact_lock(hashtext('ledger:' || v_user::text));
  -- 0127: 예전 앱이 올리던 신발만 — 네 등급 · 레벨 20 까지(폰이 적은 값을 믿지 않는다)
  if p_rarity is null or upper(p_rarity) not in ('COMMON', 'RARE', 'EPIC', 'LEGENDARY') then
    raise exception '올릴 수 없는 신발입니다' using errcode = '22023';
  end if;
  p_level := least(greatest(coalesce(p_level, 1), 1), 20);
  select sneaker_id into v_id from public.market_imports
   where user_id = v_user and local_id = p_local_id;
  if v_id is not null then
    -- 0122: 다시 올려도 레벨을 따라가지 않는다 — 폰이 적은 레벨로 서버 신발의 레벨이 오르면 레벨 상자(0116)가 나갔다(아무 계정이나 상급 6개)
    return v_id;
  end if;

  select count(*) into v_count from public.market_imports where user_id = v_user;
  if v_count >= economy.market_import_cap() then
    raise exception '올릴 수 있는 수를 넘었습니다 (최대 %)', economy.market_import_cap()
      using errcode = '22023';
  end if;

  insert into public.market_sneakers
    (owner_id, faction, rarity, variant, level, luck, comfort, durability)
  values
    (v_user, p_faction, p_rarity, p_variant, p_level, p_luck, p_comfort, p_durability)
  returning id into v_id;

  insert into public.market_imports (user_id, local_id, sneaker_id)
  values (v_user, p_local_id, v_id);

  return v_id;
end $$;

-- ── 7 ──
do $$
declare r record;
begin
  for r in select c.relname from pg_class c join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public' and c.relkind in ('r', 'p', 'v', 'm') loop
    execute format('revoke truncate on public.%I from anon, authenticated', r.relname);
  end loop;
end $$;
-- 앞으로 만드는 표도(이 배포를 올리는 역할의 기본 권한)
alter default privileges in schema public revoke truncate on tables from anon, authenticated;
