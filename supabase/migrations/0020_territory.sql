-- 땅따먹기 — 달린 동네를 육각 칸으로 나눠 크루 색으로 칠한다.
--
-- 규칙
--   - 지도를 한 칸 약 200m 짜리 육각형으로 나눈다(웹 메르카토르 기준 반지름
--     150 — 앱의 Territory.kt 와 같은 식이다).
--   - 러닝이 서버에 올라오면 경로가 지난 칸마다 "표시" 하나를 남긴다. 한 사람이
--     한 칸에 하루 한 번만 — 같은 칸을 빙빙 돈다고 점수가 늘지 않는다.
--   - 표시는 그 사람의 크루 몫이다. 크루 러닝이면 그 크루, 아니면 가장 먼저
--     가입한 크루. 크루가 없으면 표시를 남기지 않는다.
--   - 칸의 주인은 최근 14일 동안 표시가 가장 많은 크루다. 달리지 않으면 땅을
--     잃는다 — 그래야 새 크루도 뺏을 수 있다.
--   - 판정에서 무효(VOID)가 된 러닝은 칸을 칠하지 못한다.
--
-- 경로는 서버가 이미 받은 것을 쓴다. 앱이 "이 칸을 지났다"고 말하는 것을 믿지
-- 않는다 — 그러면 누구나 서울 전체를 칠할 수 있다.

create or replace function economy.territory_hex_size() returns double precision
  language sql immutable as $$ select 150::double precision $$;

create or replace function economy.territory_window_days() returns int
  language sql immutable as $$ select 14 $$;

-- 좌표 → 칸 이름("q:r"). 뾰족한 쪽이 위인 육각형, 축 좌표계.
-- 앱의 Territory.cellOf 와 한 글자도 다르지 않게 유지한다.
create or replace function economy.hex_cell(p_lat double precision, p_lng double precision)
returns text
language plpgsql immutable as $$
declare
  v_r constant double precision := 6378137.0;
  v_s double precision := economy.territory_hex_size();
  v_lat double precision := greatest(-85, least(85, p_lat));
  v_x double precision;
  v_y double precision;
  v_q double precision;
  v_rr double precision;
  v_cx double precision;
  v_cz double precision;
  v_cy double precision;
  v_rx double precision;
  v_ry double precision;
  v_rz double precision;
begin
  v_x := v_r * radians(p_lng);
  v_y := v_r * ln(tan(pi() / 4 + radians(v_lat) / 2));
  v_q := (sqrt(3) / 3 * v_x - v_y / 3) / v_s;
  v_rr := (2.0 / 3 * v_y) / v_s;
  -- 큐브 좌표로 반올림
  v_cx := v_q; v_cz := v_rr; v_cy := -v_cx - v_cz;
  v_rx := round(v_cx); v_ry := round(v_cy); v_rz := round(v_cz);
  if abs(v_rx - v_cx) > abs(v_ry - v_cy) and abs(v_rx - v_cx) > abs(v_rz - v_cz) then
    v_rx := -v_ry - v_rz;
  elsif abs(v_ry - v_cy) > abs(v_rz - v_cz) then
    v_ry := -v_rx - v_rz;
  else
    v_rz := -v_rx - v_ry;
  end if;
  return v_rx::bigint || ':' || v_rz::bigint;
end;
$$;

-- 칸 이름 → 칸 가운데 좌표
create or replace function economy.hex_center(p_cell text)
returns table (lat double precision, lng double precision)
language plpgsql immutable as $$
declare
  v_r constant double precision := 6378137.0;
  v_s double precision := economy.territory_hex_size();
  v_q double precision := split_part(p_cell, ':', 1)::double precision;
  v_rr double precision := split_part(p_cell, ':', 2)::double precision;
  v_x double precision;
  v_y double precision;
begin
  v_x := v_s * (sqrt(3) * v_q + sqrt(3) / 2 * v_rr);
  v_y := v_s * (1.5 * v_rr);
  return query select
    degrees(2 * atan(exp(v_y / v_r)) - pi() / 2),
    degrees(v_x / v_r);
end;
$$;

create table if not exists public.territory_marks (
  cell text not null,
  user_id uuid not null references auth.users on delete cascade,
  day bigint not null,
  crew_id uuid not null references public.crews on delete cascade,
  session_id bigint not null references public.walk_sessions on delete cascade,
  lat double precision not null,
  lng double precision not null,
  created_at timestamptz not null default now(),
  primary key (cell, user_id, day)
);

create index if not exists territory_marks_area on public.territory_marks (lat, lng, day);
create index if not exists territory_marks_session on public.territory_marks (session_id);
create index if not exists territory_marks_crew on public.territory_marks (crew_id, day);

comment on table public.territory_marks is
  '땅따먹기 표시. 한 사람이 한 칸에 하루 한 번. 러닝 경로에서 서버가 만든다.';

alter table public.territory_marks enable row level security;
revoke all on public.territory_marks from anon, authenticated;

-- 사람의 대표 크루 — 가장 먼저 가입한 크루. 숨겨진 크루는 빼고.
create or replace function public.home_crew(p_user uuid)
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select m.crew_id
    from public.crew_members m
   where m.user_id = p_user
     and not public.is_hidden('CREW', m.crew_id::text)
   order by m.joined_at, m.crew_id
   limit 1
$$;

-- 러닝 하나로 칸을 칠한다. 이미 칠한 러닝이면 크루만 맞춘다.
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

  foreach v_chunk in array string_to_array(v_s.track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) < 2;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num;
    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    continue when v_lat not between -85 and 85 or v_lng not between -180 and 180;
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

create or replace function public.territory_on_session()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.territory_credit(new.id);
  return new;
end;
$$;

drop trigger if exists territory_after_insert on public.walk_sessions;
create trigger territory_after_insert
  after insert on public.walk_sessions
  for each row execute function public.territory_on_session();

drop trigger if exists territory_after_crew on public.walk_sessions;
create trigger territory_after_crew
  after update of crew_id on public.walk_sessions
  for each row when (new.crew_id is distinct from old.crew_id)
  execute function public.territory_on_session();

-- 지도에 보이는 칸과 주인. 한 번에 너무 넓은 곳은 받지 않는다(약 30km).
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

-- 크루별 차지한 칸 수. 최근 14일 기준.
create or replace function public.territory_board(p_limit int default 20)
returns table (
  crew_id uuid,
  crew_name text,
  cells int,
  mine boolean
)
language sql
stable
security definer
set search_path = public, economy
as $$
  with counts as (
    select t.cell, t.crew_id, count(*) as n, max(t.day) as last_day
      from public.territory_marks t
     where t.day >= floor(extract(epoch from now()) / 86400)::bigint - economy.territory_window_days()
       and not public.is_hidden('CREW', t.crew_id::text)
     group by t.cell, t.crew_id
  ),
  owner as (
    select distinct on (k.cell) k.cell, k.crew_id
      from counts k
     order by k.cell, k.n desc, k.last_day desc, k.crew_id
  )
  select o.crew_id, c.name, count(*)::int,
         exists (select 1 from public.crew_members m
                  where m.crew_id = o.crew_id and m.user_id = auth.uid())
    from owner o
    join public.crews c on c.id = o.crew_id
   group by o.crew_id, c.name
   order by 3 desc, o.crew_id
   limit greatest(1, least(coalesce(p_limit, 20), 100))
$$;

revoke execute on function public.home_crew(uuid) from public, anon, authenticated;
revoke execute on function public.territory_view(double precision, double precision, double precision, double precision) from public, anon;
revoke execute on function public.territory_board(int) from public, anon;
grant execute on function public.territory_view(double precision, double precision, double precision, double precision) to authenticated;
grant execute on function public.territory_board(int) to authenticated;
