-- 0123 2차 점검 고침 — 꼼수 · 보안 (2026-11-22) — 사용자 "다시 의심되는 부분 찔러보고 버그 한 번 더 고쳐놔" → "모든 꼼수 / 해킹 등 유지보수 관련해서 전부 할 수 있는 거 다".
-- 점검: docs/점검-2026-11-22-2차-꼼수-보안.md(여덟 갈래). tools/build_0123.py 가 만든다(고칠 함수는 마지막 정의를 복사).
-- record_session 은 0123 이 마지막 정의다(0122 본문 + 아래 1 ~ 7).
--   1. 태운 신발 — 이 러닝을 시작한 뒤에 태웠을 때만 그 신발로 셈하고, 닳을 몫의 수리비는 적립에서 뺀다(예전엔 며칠 전에 태운 신발로 영영 닳지 않았다)
--   2. 사본 경로(좌표를 조금 옮기거나 시각을 통째로 민 것 — 시각 간격 패턴이 같다) — 부계정 크루 · 같은 경로 되풀이(walk_sessions.route_sig · route_dts)
--   3. 가입하기 전에 시작한 러닝은 무효(새 계정이 지난 이레를 지어내 첫날 Lv30)
--   4. 과속 — 앱이 신고하지 않아도 경로에서 시속 25km 넘는 구간이 30초 · 150m 넘게 이어지면 무효
--   5. 닳음 · 크루 체력은 경로가 말하는 움직인 시간을 바닥으로(앱이 보낸 시간을 줄여 덜 닳던 것)
--   6. 이벤트 · 부스터 배수는 러닝 시간 가운데 그 창에 든 몫만큼(출발만 창 안 → 멈춰 뒀다 나중에 달리기)
--   7. 크루 러닝은 러닝 아이템을 쓰지 않는다 — 묶여 있었으면 돌려준다 · 기록되지 못한 러닝의 아이템도 여드레 뒤 돌려준다
--   8. 함께 달린 사람은 가입 24시간 뒤부터(together_min_account_hours — 그날 만든 부계정으로 인원 보너스)
--   9. 운영자: 모든 곳 잠금(아무나 30번 틀려 운영자를 막던 것) 없앰 · 보상 배수 반올림 우회 · 이어 붙인 이벤트는 키를 한 번 더
--  10. 권한: 프로필 직접 고치기(목표 · 이름 — 지난 날 목표를 바꿔 보너스) · 크루 · 글 · 코스 · 댓글 직접 지우기(달리는 크루 · 출발한 번개를 지움) ·
--      admin_economy_set(범위 없이 아무 설정 — 앱 · 운영자 페이지가 쓰지 않는다) 거둠 · 좋아요 · 번개 참석자는 보이는 글만
--  11. 이야기 지도는 앞뒤 300m 를 자른다(집 위치) · IP 해시에 비밀 소금 · 타워 이름은 처음 참가한 곳에서만 · 예전 레이스 닫음
--  12. 하루 걸음 체인 기록의 목표 달성은 서버 값 · 홈 목표 줄(하루 최대에 막힌 날) · 보호막은 그날 반 넘게 덮어야 · 강화 견적 서명 · 하루 최대의 남은 몫(× 1)

-- ════════════════════════════════════════════════════════════════════
--  0. 설정 · 칸
-- ════════════════════════════════════════════════════════════════════
insert into public.economy_settings (key, value) values
  ('together_min_account_hours', '24'),  -- 함께 달린 사람: 가입하고 이 시간이 지난 계정만(0 = 끔)
  ('route_copy_max_m', '5'),             -- 거의 같은 경로: 시각 간격 패턴이 90% 넘게 같고 시간에 맞춰 고른 점들의 가운데 거리가 이 안이면 사본
  ('overspeed_min_sec', '30'),           -- 서버 과속: 시속 25km 넘는 구간이 끊기지 않고 이만큼(초)
  ('overspeed_min_m', '150'),            -- 서버 과속: 그 구간에서 움직인 거리(m)
  ('signup_run_guard', 'true')           -- 가입하기 전에 시작한 러닝은 무효(false = 끔 — 검사 · 예전 셈)
on conflict (key) do nothing;

alter table public.walk_sessions add column if not exists route_sig double precision[];
alter table public.walk_sessions add column if not exists route_dts int[];
alter table public.walk_sessions add column if not exists route_lat0 double precision;
alter table public.walk_sessions add column if not exists route_lng0 double precision;
comment on column public.walk_sessions.route_sig is '경로 지문 — 시간에 맞춰 고른 24점(위도 · 경도 번갈아). 거의 같은 경로를 다시 올렸는지 본다 (0123)';
comment on column public.walk_sessions.route_dts is '경로 점 사이 시각 간격(ms) 앞 240개 — 시각 패턴을 그대로 둔 사본을 가린다. 간격이 고르면(시험 경로 · 1초 고정) 비운다 (0123)';
create index if not exists walk_sessions_route_start on public.walk_sessions (route_lat0, route_lng0) where route_lat0 is not null;

alter table economy.tower_players add column if not exists client text;

create table if not exists economy.app_secrets (
  name text primary key,
  value text not null,
  created_at timestamptz not null default now()
);
revoke all on economy.app_secrets from public, anon, authenticated;
insert into economy.app_secrets (name, value)
values ('client_salt', encode(sha256(convert_to(gen_random_uuid()::text || gen_random_uuid()::text || clock_timestamp()::text, 'UTF8')), 'hex'))
on conflict (name) do nothing;

-- ════════════════════════════════════════════════════════════════════
--  1. 경로에서 읽는 것 — 움직인 시간 · 이어진 과속 · 지문
-- ════════════════════════════════════════════════════════════════════
-- 10초 창(economy.speed_window_sec)마다: 1분 넘게 빈 곳은 끊는다(track_speed_stats 와 같은 규칙).
--   moving_sec = 초속 0.5m 넘게 움직인 창의 시간 합 · over_sec / over_m = 시속 25km 넘는 창이 끊기지 않고 이어진 가장 긴 구간
--   (60km/h 를 넘는 창은 GPS 튐으로 보고 구간을 끊지도 더하지도 않는다)
create or replace function economy.track_motion(p_track text)
returns table (moving_sec numeric, over_sec numeric, over_m numeric)
language plpgsql immutable set search_path = public, economy as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text; v_parts text[];
  v_lat double precision; v_lng double precision; v_at bigint;
  v_blat double precision; v_blng double precision; v_bat bigint;
  v_have boolean := false;
  v_dt double precision; v_m double precision; v_kmh double precision;
  v_move double precision := 0;
  v_run_s double precision := 0; v_run_m double precision := 0;
  v_best_s double precision := 0; v_best_m double precision := 0;
  v_limit double precision := economy.fx('overspeed_kmh', 25);
begin
  if p_track is null or p_track = '' then
    return query select 0::numeric, 0::numeric, 0::numeric;
    return;
  end if;
  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';
    v_lat := v_parts[1]::double precision; v_lng := v_parts[2]::double precision; v_at := v_parts[3]::bigint;
    if not v_have then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at; v_have := true;
      continue;
    end if;
    v_dt := (v_at - v_bat) / 1000.0;
    if v_dt <= 0 or v_dt > economy.track_gap_sec() then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
      v_run_s := 0; v_run_m := 0;
      continue;
    end if;
    continue when v_dt < economy.speed_window_sec();
    v_m := economy.haversine_m(v_blat, v_blng, v_lat, v_lng);
    v_kmh := v_m / v_dt * 3.6;
    if v_kmh > economy.speed_glitch_kmh() then
      null;  -- 튐 — 이어진 구간을 끊지도 더하지도 않는다
    else
      if v_m / v_dt > 0.5 then v_move := v_move + v_dt; end if;
      if v_kmh > v_limit then
        v_run_s := v_run_s + v_dt; v_run_m := v_run_m + v_m;
        if v_run_s > v_best_s then v_best_s := v_run_s; v_best_m := v_run_m; end if;
      else
        v_run_s := 0; v_run_m := 0;
      end if;
    end if;
    v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
  end loop;
  return query select round(v_move::numeric, 1), round(v_best_s::numeric, 1), round(v_best_m::numeric, 1);
end $$;

-- 경로 지문 — 첫 점 · 끝 점 시각 사이를 23 칸으로 나눠 그 시각의 자리(앞뒤 점 사이를 시간으로 나눠)를 고른다(24점 · 위도 경도 번갈아 48개).
-- 시각 간격은 점이 20개 이상이고 간격이 고르지 않을 때만(1초 고정이면 서로 다른 폰도 같아진다). 경로를 한 번만 훑는다(1시간 경로 몇 ms)
create or replace function economy.track_sig(p_track text)
returns table (sig double precision[], dts int[], lat0 double precision, lng0 double precision)
language plpgsql immutable set search_path = public, economy as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text; v_parts text[];
  la double precision[] := '{}'; lo double precision[] := '{}'; ts bigint[] := '{}';
  n int; a0 bigint; a1 bigint; k int; i int := 1; t double precision; f double precision;
  v_sig double precision[] := '{}'; v_all int[] := '{}'; v_d int; v_distinct int;
begin
  if p_track is null or p_track = '' then
    return query select null::double precision[], null::int[], null::double precision, null::double precision;
    return;
  end if;
  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';
    la := la || v_parts[1]::double precision;
    lo := lo || v_parts[2]::double precision;
    ts := ts || v_parts[3]::bigint;
  end loop;
  n := coalesce(cardinality(ts), 0);
  if n = 0 then
    return query select null::double precision[], null::int[], null::double precision, null::double precision;
    return;
  end if;
  a0 := ts[1]; a1 := ts[n];
  if n >= 2 and a1 > a0 then
    for k in 0 .. 23 loop
      t := a0 + (a1 - a0) * k / 23.0;
      while i < n - 1 and ts[i + 1] < t loop i := i + 1; end loop;
      f := case when ts[i + 1] > ts[i] then least(greatest((t - ts[i]) / (ts[i + 1] - ts[i]), 0), 1) else 0 end;
      v_sig := v_sig || (la[i] + (la[i + 1] - la[i]) * f) || (lo[i] + (lo[i + 1] - lo[i]) * f);
    end loop;
  else
    v_sig := null;
  end if;
  for k in 1 .. n - 1 loop
    v_d := least(greatest(ts[k + 1] - ts[k], -2000000000), 2000000000)::int;
    v_all := v_all || v_d;
  end loop;
  select count(distinct x) into v_distinct from unnest(v_all) x;
  return query select v_sig,
                      case when coalesce(v_distinct, 0) > 5 and n - 1 >= 20 then v_all[1:240] end,
                      la[1], lo[1];
end $$;

-- 이 경로가 이미 올린 경로(다른 계정 · 내 다른 러닝)의 사본인가 — 첫 점이 약 50m 안 · 경로 거리 ±10% · 30일 안에서 견준다.
-- 사본 = 점 사이 시각 간격(ms)이 같은 자리에서 90% 넘게 같고, 시간에 맞춰 고른 24점의 가운데 거리가 route_copy_max_m(5m) 안.
-- 폰마다 시각 간격이 ms 로 달라 나란히 달린 두 사람도 이렇게 겹치지 않는다. 좌표만 조금 옮기거나 시각을 통째로 민 사본을 잡는다
-- (시각까지 새로 지어낸 사본은 기기 확인 없이는 가릴 수 없다 — 점검 문서)
create or replace function economy.route_copy_of(p_user uuid, p_started timestamptz, p_sig double precision[], p_dts int[],
    p_lat0 double precision, p_lng0 double precision, p_gps_m double precision)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select coalesce(p_lat0 is not null and coalesce(p_gps_m, 0) >= 500 and cardinality(p_sig) = 48 and cardinality(p_dts) >= 20 and exists (
    select 1 from public.walk_sessions s
     where s.route_lat0 between p_lat0 - 0.0005 and p_lat0 + 0.0005
       and s.route_lng0 between p_lng0 - 0.0007 and p_lng0 + 0.0007
       and s.created_at > now() - interval '30 days'
       and not (s.user_id = p_user and s.started_at = p_started)
       and coalesce(s.gps_distance_m, 0) between p_gps_m * 0.9 and p_gps_m * 1.1
       and cardinality(s.route_sig) = 48 and cardinality(s.route_dts) >= 20
       and (select count(*) filter (where s.route_dts[i] = p_dts[i])::numeric / count(*)
              from generate_series(1, least(cardinality(s.route_dts), cardinality(p_dts))) i) >= 0.9
       and (select percentile_cont(0.5) within group (order by economy.haversine_m(p_sig[2 * i - 1], p_sig[2 * i], s.route_sig[2 * i - 1], s.route_sig[2 * i]))
              from generate_series(1, 24) i) < economy.fx('route_copy_max_m', 5)), false)
$$;

-- ════════════════════════════════════════════════════════════════════
--  2. 적립 배수 — 이벤트 · 부스터는 그 창에 든 몫만큼 · 아이템
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.run_reward_mult_span(p_user uuid, p_started timestamptz, p_ended timestamptz, p_items boolean default true)
returns numeric language sql stable security definer set search_path = public, economy as $$
  with span as (select greatest(extract(epoch from (greatest(p_ended, p_started) - p_started)), 1)::numeric as sec)
  select greatest(
    1,
    -- 24시간 부스터(파는 곳은 닫혀 있다)
    coalesce((select max(1 + (2 - 1) * least(greatest(extract(epoch from (least(b.ends_at, p_ended) - greatest(b.starts_at, p_started)))::numeric, 0) / span.sec, 1))
                from public.boosts b, span
               where b.user_id = p_user and b.kind = 'XP_BOOSTER' and b.starts_at < p_ended and b.ends_at > p_started), 1),
    -- 보상 이벤트(취소하면 그때까지)
    coalesce((select max(1 + (e.mult - 1) * least(greatest(extract(epoch from (least(e.ends_at, coalesce(e.cancelled_at, 'infinity'::timestamptz), p_ended)
                                                                                - greatest(e.starts_at, p_started)))::numeric, 0) / span.sec, 1))
                from economy.reward_events e, span
               where e.starts_at < p_ended and least(e.ends_at, coalesce(e.cancelled_at, 'infinity'::timestamptz)) > p_started), 1),
    -- 이 러닝에 고른 아이템(크루 러닝은 없음)
    case when p_items then coalesce((select max(k.value) from economy.item_uses u join economy.item_kinds k on k.kind = u.kind
                                      where u.user_id = p_user and u.run_started_at = p_started and k.grp = 'REWARD'), 1) else 1 end
  )::numeric(6, 3)
$$;

-- 이 러닝에 묶인 아이템을 가방으로 돌려준다(크루 러닝 — 아이템을 쓰지 않는다)
create or replace function economy.items_refund(p_user uuid, p_run_started_at timestamptz)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_kind text; v_n int := 0;
begin
  for v_kind in delete from economy.item_uses u where u.user_id = p_user and u.run_started_at = p_run_started_at and u.session_id is null
                returning u.kind loop
    perform economy.item_add(p_user, v_kind, 1);
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;

-- 여드레가 지나도 기록되지 않은 러닝에 묶인 아이템 — record_session 이 거절해(이레 넘음 · 끝이 미래) 영영 쓰지 못하는 것을 돌려준다
create or replace function economy.items_refund_stale(p_user uuid)
returns int language plpgsql security definer set search_path = public, economy as $$
declare v_kind text; v_n int := 0;
begin
  for v_kind in delete from economy.item_uses u
                 where u.user_id = p_user and u.run_started_at is not null and u.session_id is null
                   and u.run_started_at < now() - interval '8 days'
                   and not exists (select 1 from public.walk_sessions s where s.user_id = p_user and s.started_at = u.run_started_at)
                returning u.kind loop
    perform economy.item_add(p_user, v_kind, 1);
    v_n := v_n + 1;
  end loop;
  return v_n;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  3. 함께 달린 사람 — 0122 + 가입 24시간 뒤부터(그날 만든 부계정으로 인원 보너스)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.together_qualifies(s public.walk_sessions)
returns boolean language sql stable security definer set search_path = public, economy as $$
  select s.verdict <> 'VOID' and s.gps_backed
     and coalesce(s.reward_steps, 1) > 0
     and coalesce(s.verified_steps, 0) >= economy.together_min_steps()
     and coalesce(s.duration_sec, 0) >= economy.together_min_sec()
     and coalesce(s.gps_credit_m, 0) >= coalesce(economy.together_goal_m(s.party_id, s.user_id), 0)
     and coalesce((select p.created_at from public.profiles p where p.id = s.user_id), s.started_at)
         <= s.started_at - make_interval(hours => greatest(economy.fx('together_min_account_hours', 24), 0)::int)
$$;

-- 하루 최대 SUP 의 남은 몫 — 0120 + 그날 가장 큰 하루 최대를 배수 없이(× 1) 본다. 1분짜리 2배 러닝 하나로 목표 · 코스 보너스의 남은 몫이
-- 두 배로 열리던 것. 정산(p_max 를 주는 곳)은 그 러닝의 하루 최대 그대로
create or replace function economy.day_room(p_user uuid, p_day date, p_max numeric)
returns numeric language sql stable security definer set search_path = public, economy as $$
  select coalesce(p_max,
                  (select max(s.day_max / greatest(coalesce(s.reward_mult, 1), 1)) from public.walk_sessions s
                    where s.user_id = p_user and economy.game_day(s.started_at) = p_day and s.day_max is not null),
                  economy.daily_max_of(p_user, (select (economy.shoe_effective(e)).efficiency_bps from economy.equipped_sneaker(p_user) e), 1))
         - economy.day_earned(p_user, p_day)
$$;

-- 지난날 목표를 채운 날 — 0122 + 연속 보호막은 그날을 반 넘게(12시간) 덮을 때만(24시간짜리 하나가 이틀로 세던 것)
create or replace function economy.goal_streak_before(p_user uuid, p_day date, p_max int default 60)
returns int language plpgsql stable security definer set search_path = public, economy as $$
declare v int := 0; d date := p_day - 1;
begin
  while v < p_max loop
    exit when not (
      exists (select 1 from public.sup_ledger l where l.user_id = p_user and l.ref = 'goal:' || d)
      or exists (select 1 from economy.goal_days g where g.user_id = p_user and g.day = d and g.met_at is not null)
      or exists (select 1 from public.boosts b
                  where b.user_id = p_user and b.kind = 'STREAK_SHIELD'
                    and b.starts_at < economy.game_day_start(d + 1) and b.ends_at > economy.game_day_start(d)
                    and least(b.ends_at, economy.game_day_start(d + 1)) - greatest(b.starts_at, economy.game_day_start(d)) >= interval '12 hours'));
    v := v + 1; d := d - 1;
  end loop;
  return v;
end $$;

-- ════════════════════════════════════════════════════════════════════
--  4. IP 해시에 비밀 소금(0083 은 고정 문구라 IPv4 전부를 몇 분이면 맞대어 볼 수 있었다)
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.waitlist_client_of(p_ip text) returns text
  language sql stable security definer set search_path = '' as $$
  select case when nullif(btrim(coalesce(p_ip, '')), '') is null then null
              else encode(sha256(convert_to(coalesce((select s.value from economy.app_secrets s where s.name = 'client_salt'), '')
                                            || ':stepup-waitlist:' || btrim(p_ip), 'UTF8')), 'hex') end
$$;
revoke all on function economy.waitlist_client_of(text) from public;

-- ════════════════════════════════════════════════════════════════════
--  5. 이야기 지도 — 크루 러닝 지도처럼 앞뒤 300m 를 자른다(집 · 회사 자리가 누구에게나 보였다)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.story_route(p_track text)
returns text language sql immutable set search_path = public as $$
  select public.crew_run_route(p_track)
$$;
revoke all on function public.story_route(text) from public, anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  6. 운영자
-- ════════════════════════════════════════════════════════════════════
create or replace function economy.ops_fail(p_client text, p_kind text) returns timestamptz
language plpgsql security definer set search_path = '' as $$
declare
  v_window interval := economy.ops_num('ops_login_window_min', 10)::float8 * interval '1 minute';
  v_until timestamptz := now() + economy.ops_num('ops_login_lock_min', 15)::float8 * interval '1 minute';
begin
  insert into economy.ops_login_fails (client, kind) values (p_client, p_kind);
  if (select count(*) from economy.ops_login_fails f where f.client = p_client and f.at > now() - v_window)
       >= economy.ops_num('ops_login_client_fails', 5)
     and economy.ops_locked_until(p_client, false) is null then
    insert into economy.ops_login_locks (scope, until) values (p_client, v_until)
    on conflict (scope) do update set until = excluded.until;
    insert into public.admin_audit (actor, action, target, detail)
    values (null, 'ops_login_locked', 'client', jsonb_build_object('client', left(p_client, 12), 'until', v_until, 'kind', p_kind));
  end if;
  -- 0123: 모든 곳(ALL) 잠금은 뺐다 — 누구나 틀린 키 30번으로 운영자 로그인 전체를 계속 막을 수 있었다(키는 244비트라 맞혀 볼 수 없다)
  return economy.ops_locked_until(p_client);
end $$;

-- 보상 이벤트 — 0114 본문 + 저장 값으로 본다 · 이어 붙인 이벤트 · 하루 넘게 뒤에 시작하면 키를 한 번 더
create or replace function public.ops_event_start(
  p_token text,
  p_request uuid,
  p_title text,
  p_mult numeric,
  p_hours numeric,
  p_starts_at timestamptz default null,
  p_push boolean default true,
  p_confirm text default null
) returns jsonb
language plpgsql security definer set search_path = public, economy as $$
declare
  v_session economy.ops_sessions := economy.ops_session_of(p_token);
  v_actor text := v_session.label;
  v_title text := btrim(coalesce(p_title, ''));
  v_from timestamptz := coalesce(p_starts_at, now());
  v_to timestamptz;
  v_event economy.reward_events;
  v_user uuid;
  v_pushed int := 0;
  v_big boolean;
  v_refuse jsonb;
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
  -- 0123: 저장하는 값(둘째 자리 반올림)으로 먼저 본다 — 2.995 는 3.00 으로 저장되는데 3 아래라 다시 확인 없이 지나갔다
  p_mult := round(p_mult, 2);
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

  -- 다시 확인(0114) — 큰 배수 · 긴 기간
  -- 0123: 줄줄이 이어 붙인 이벤트(이미 잡힌 앞으로의 시간까지 더해) · 하루 넘게 뒤에 시작하는 것도 키를 한 번 더
  v_big := p_mult >= economy.ops_num('ops_confirm_mult', 3) or p_hours > economy.ops_num('ops_confirm_hours', 48)
        or v_from > now() + interval '24 hours'
        or p_hours + coalesce((select sum(extract(epoch from (e.ends_at - greatest(e.starts_at, now()))) / 3600.0)
                                 from economy.reward_events e where e.cancelled_at is null and e.ends_at > now()), 0)
           > economy.ops_num('ops_confirm_hours', 48);
  v_refuse := economy.ops_confirm(v_session, p_confirm,
    format('보상 ×%s · %s시간 이벤트', economy.ops_short(p_mult), economy.ops_short(p_hours)), v_big);
  if v_refuse is not null then
    return v_refuse;
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
                             'starts_at', v_from, 'ends_at', v_to,
                             'confirmed', nullif(btrim(coalesce(p_confirm, '')), '') is not null));

  return jsonb_build_object('ok', true, 'event', economy.reward_event_json(v_event), 'replay', false);
end $$;

-- ════════════════════════════════════════════════════════════════════
--  7. 타워 게임 — 이름은 처음 참가한 곳에서만(0115 본문)
-- ════════════════════════════════════════════════════════════════════
create or replace function public.tower_join(p_email text, p_name text, p_trap text default '')
returns jsonb language plpgsql security definer set search_path = '' as $$
declare
  v_email text := lower(btrim(coalesce(p_email, '')));
  v_name text := regexp_replace(btrim(coalesce(p_name, '')), '\s+', ' ', 'g');
  v_entry uuid;
begin
  if p_trap is distinct from '' then return jsonb_build_object('ok', false, 'code', 'email'); end if;
  if not economy.tower_throttle() then return jsonb_build_object('ok', false, 'code', 'busy'); end if;
  if length(v_email) > 254 or not economy.waitlist_email_ok(v_email) then return jsonb_build_object('ok', false, 'code', 'email'); end if;
  if v_name !~ '^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 _.\-]{2,12}$' then return jsonb_build_object('ok', false, 'code', 'name'); end if;
  v_entry := economy.tower_entry(v_email);
  if v_entry is null then
    insert into economy.tower_guests (email) values (v_email) on conflict (email) do nothing;
    v_entry := economy.tower_entry(v_email);
  end if;
  -- 0123: 이름은 처음 참가한 곳(IP 해시)에서만 바꾼다 — 남의 이메일로 참가해 그 사람의 이름을 덮어쓰던 것
  insert into economy.tower_players (entry_id, name, client) values (v_entry, v_name, economy.waitlist_client())
  on conflict (entry_id) do update set name = excluded.name, updated_at = now(),
                                       client = coalesce(economy.tower_players.client, excluded.client)
   where economy.tower_players.client is null or economy.tower_players.client is not distinct from excluded.client;
  return economy.tower_status(v_entry);
end $$;

-- ════════════════════════════════════════════════════════════════════
--  8. 상자 현황(0122 본문) — 하루 최대에 막혀 채운 날 · 목표 보너스는 받을 수 있는 만큼
-- ════════════════════════════════════════════════════════════════════
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
  -- 0123: 하루 최대에 막혀 보너스 없이 채운 날(goal_days.met_at, 0122)도 채운 날 — 0 으로 보낸다(앱이 "확인 중"에 머물지 않게)
  if v_paid is null and exists (select 1 from economy.goal_days g where g.user_id = v_user and g.day = v_day and g.met_at is not null) then
    v_paid := 0;
  end if;
  v_before := economy.goal_streak_before(v_user, v_day, 400);
  v_streak := v_before + 1;
  select coalesce(max(n), 0) into v_best from (
    select count(*) as n from (
      select d, d - (row_number() over (order by d))::int as grp
        from (select substr(l.ref, 6)::date as d from public.sup_ledger l
               where l.user_id = v_user and l.ref like 'goal:%'
              union
              select g.day from economy.goal_days g where g.user_id = v_user and g.met_at is not null) x) y
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
    -- 0123: 받을 수 있는 만큼만 보인다(하루 최대 SUP 에 남은 몫)
    'goal_bonus', least(round((v_goal / 1000.0) * 2.5 * (1 + 0.1 * least(v_streak - 1, 7)), 2),
                        case when economy.daily_max_on() then greatest(round(economy.day_room(v_user, v_day, null), 2), 0) else 1000000 end),
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
        select a.day, case when (a.source like 'RUN%' and a.source not like 'RUNNER%') then 'RUN_BOX' when a.source like 'WEEK:%' then 'WEEK_BOX'
                           when a.source like 'STREAK:%' then 'STREAK_BOX' else 'LEVEL_BOX' end as kind,
               null::numeric as amount, a.granted_at as at
          from economy.box_awards a
         where a.user_id = v_user and a.granted_at > now() - interval '3 days'
           and ((a.source like 'RUN%' and a.source not like 'RUNNER%') or a.source like 'WEEK:%' or a.source like 'STREAK:%' or a.source like 'LEVEL:%')
        union all
        select substr(l.ref, 6)::date, 'GOAL', l.amount, l.created_at
          from public.sup_ledger l
         where l.user_id = v_user and l.ref like 'goal:%' and l.created_at > now() - interval '3 days') r));
end $$;

-- 러닝 아이템(0106 본문) — 사라진 아이템 돌려주기
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
  perform economy.items_refund_stale(v_user);  -- 0123: 기록되지 못한 러닝에 묶여 사라진 아이템을 돌려준다

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
    'mult', economy.run_reward_mult(v_user, p_run_started_at),  -- 출발 때 값(끝나면 이벤트 시간 몫으로 다시 센다, 0123)
    'energy_factor', economy.run_energy_factor(v_user, p_run_started_at));
end $$;

-- 강화 견적(0118 본문) — 서명에 굽이 · 닳음 설정도
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
             economy.fx('repair_share', 0.45), economy.points_per_step(),
             -- 0123: 통의 굽이 · 닳은 재료 값을 바꾸는 설정도(견적과 넣기 사이에 바뀌면 다시 묻게)
             economy.fx('forge_xp_level_growth', 0), coalesce(economy.setting('forge_xp_curve')::text, ''),
             economy.fx('dur_repair_exp', 1), economy.fx('eff_wear_exp', 1));
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

-- 체인 기록 재료(0078 본문) — 하루 걸음의 목표 달성은 서버 값
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
    -- 0123: 목표 달성은 서버가 확인한 그날의 보상 걸음 · 그날의 목표로(앱이 보낸 걸음 · 목표는 지어낼 수 있다 — goal=1)
    return v || jsonb_build_object(
      'goalMet', economy.verified_steps_on(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int)
                 >= economy.goal_of(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int),
      '_audit', jsonb_build_object('steps', r.steps, 'goal', r.goal,
                                   'verified', economy.verified_steps_on(j.user_id, date '1970-01-01' + (a ->> 'epoch_day')::int)));
  elsif j.kind = 'CHECK_IN' then
    return v;
  end if;
  return null;
end $$;
revoke all on function economy.activity_payload(public.chain_jobs) from public;

-- ════════════════════════════════════════════════════════════════════
--  9. record_session — 0122 본문 + 태운 신발 · 거의 같은 경로 · 가입 전 · 과속 · 움직인 시간 · 배수 시간 몫 · 크루 아이템
--     다시 고칠 때는 이것(0123)을 복사한다
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
  -- 크루 효율 8 : 2 (0120) — 효율 뺀 바탕 · 이번에 쓴 효율 배수
  v_unit numeric;
  v_cmult numeric;
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
  -- 0123: 경로에서 잰 움직인 시간 · 이어진 과속 구간 · 경로 지문 · 태운 신발의 수리비
  v_moving numeric := 0;
  v_over_sec numeric := 0;
  v_over_m numeric := 0;
  v_sig double precision[];
  v_dts int[];
  v_lat0 double precision;
  v_lng0 double precision;
  v_wall_s int;
  v_wear_cost numeric := 0;
  -- 0119 — 닳은 뒤 적립 비율 · 효율 포인트가 닳음 / 에너지를 올리는 배수 · 가득 구간 / 닳은 구간 걸음
  v_worn_rate numeric := 0;
  v_wear_mult numeric := 1;
  v_enr_mult numeric := 1;
  v_life_steps int := 0;
  v_full_steps int := 0;
  v_worn_steps int := 0;
  -- 0120 — 함께 달린 보너스의 바탕(크루 B · 번개는 인원 배수 전 적립) · 이 러닝의 하루 최대
  v_tb numeric;
  v_day_max numeric;
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
  -- 0123: 경로가 말해 주는 움직인 시간(닳음 · 크루 체력의 바닥) · 사람 속도를 넘는 구간 · 다시 올린 경로를 가릴 지문
  v_wall_s := greatest(floor(extract(epoch from (p_ended_at - p_started_at)))::int, 0);
  select m.moving_sec, m.over_sec, m.over_m into v_moving, v_over_sec, v_over_m from economy.track_motion(v_track) m;
  select g.sig, g.dts, g.lat0, g.lng0 into v_sig, v_dts, v_lat0, v_lng0 from economy.track_sig(v_track) g;

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

  -- 0123: 가입하기 전에 시작한 러닝 — 새 계정이 지난 이레를 지어내 첫날 러너 Lv30 · 상자 51개를 받았다
  elsif coalesce((economy.setting('signup_run_guard') #>> '{}')::boolean, true)
        and p_started_at < (select p.created_at from public.profiles p where p.id = v_user) - interval '10 minutes' then
    v_verdict := 'VOID';
    v_reason := '가입하기 전에 시작한 러닝입니다';

  -- 0123: 과속 멈춤은 앱만 했다(앱이 신고하지 않으면 차 · 자전거로 달려도 CLEAN). 서버도 경로에서 본다 —
  -- 시속 25km 를 넘는 10초 구간이 끊기지 않고 30초 · 150m 넘게 이어졌는데 앱의 신고(run_penalties)가 없다
  elsif v_over_sec >= economy.fx('overspeed_min_sec', 30) and v_over_m >= economy.fx('overspeed_min_m', 150)
        and not exists (select 1 from public.run_penalties x
                         where x.user_id = v_user and x.run_started_at = p_started_at) then
    v_verdict := 'VOID';
    v_reason := '러닝 속도를 넘는 이동이 이어졌습니다';

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

  -- 0123: 사본 경로 — 좌표를 1cm 옮기거나 시각을 통째로 민 사본이 md5 를 피했다(부계정 크루 · 같은 경로 되풀이).
  -- 점 사이 시각 간격이 90% 넘게 같고 경로가 5m 안(economy.route_copy_of) — 나란히 달린 두 사람은 폰마다 간격이 달라 겹치지 않는다
  elsif economy.route_copy_of(v_user, p_started_at, v_sig, v_dts, v_lat0, v_lng0, v_gps_m) then
    v_verdict := 'VOID';
    v_reason := '이미 올린 경로와 같습니다';

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
            where s.user_id = v_user and x.flash_post_id = v_flash
              and (s.party_size > 1 or (s.together_base is not null and economy.together_qualifies(s)))
              and s.verdict <> 'VOID')) then  -- 0122: 원격 번개는 인원이 나중에 정해져 처음엔 1명으로 적힌다 — 함께 달린 사람으로 올린 러닝이면 받은 것
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
    -- 0120: 함께 달린 사람만 센다(ACTIVE) — 인원 보너스는 기록한 뒤 together_settle 이 함께 달린 사람이 올릴 때마다 더한다
    if economy.together_on('CREW') then
      v_headcount := 0;
    else
      perform economy.crew_run_bonus_eval(v_user, v_cr.day, true);
      select * into v_cr from economy.crew_runs c where c.user_id = v_user and c.day = v_cr.day;
      v_headcount := v_cr.bonus_pct;
    end if;
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
  -- 출발 때 신은 신발(크루 러닝은 고른 신발)을 이 계정이 올리기 전에 강화 재료로 태웠다(다른 폰에서 — 0118) — 그 신발의 마지막 값으로 셈한다.
  -- 예전엔 지금 신은 다른 신발로 정산해, 닳은 신발로 달린 러닝이 내구도 몫만큼 깎이지 않았다. 태운 신발은 닳음 · 거리를 남기지 않는다(아래)
  if v_shoe.id is null and coalesce(case when v_pool = 'CREW' then v_cr.sneaker_id end, p_sneaker) is not null then
    select x.* into v_shoe from public.market_sneakers x
     where x.id = coalesce(case when v_pool = 'CREW' then v_cr.sneaker_id end, p_sneaker) and x.status = 'BURNED'
       and exists (select 1 from public.forge_requests r where r.user_id = v_user and x.id = any(r.material_ids)
                      -- 0123: 이 러닝을 시작한 뒤에 태웠을 때만 — 예전엔 며칠 전에 태운 신발도 받아 그 신발로 영영 닳지 않고 달렸다
                      and r.created_at >= p_started_at - interval '2 minutes');
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
      v_run_sec := greatest(v_elapsed, least(coalesce(v_moving, 0)::int, v_wall_s));  -- 0123: 앱이 보낸 시간이 짧아도 경로로 움직인 시간만큼
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
        -- 0122: 10분 전에 끝낸 · 소식이 끊긴 사람이 맡긴 몫(묶어 둔 체력, 0120 R3)은 쓰지 않는다 — 내 몫은 쓴다
        v_take := least(v_need, greatest(coalesce(v_pool_row.capacity, 0) - coalesce(v_pool_row.consumed, 0)
                                         - economy.crew_pool_held_except(v_cr.crew_id, v_cr.day, now(), v_user), 0));
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
    -- 내구도는 문이 아니라 요율이다 (0119): 남은 내구도로 달린 걸음(가득 구간)은 다 받고, 다 닳은 뒤의 걸음(닳은 구간)은
    -- worn_rate(30%)만 받는다 — 0% 신발로도 달리고, 걸음은 다 센다. worn_rate 가 0 이면 예전처럼 닳은 뒤 걸음은 적립이 없다(0066).
    -- 효율 포인트는 같은 시간에 더 벌게 하고 그만큼 신발과 에너지를 빨리 쓴다(wear_mult · energy_mult) —
    -- 한 켤레로 버는 양은 내구도, 하루에 버는 양은 착화감이 늘린다. 남은 내구도로 달릴 수 있는 시간 = 내구도 × 12초 ÷ wear_mult
    v_worn_rate := economy.worn_rate();
    if v_shoe.id is not null then
      v_wear_mult := economy.wear_mult(v_shoe);
      v_enr_mult := economy.energy_mult(v_shoe);
    end if;
    v_life_steps := v_rewardable;
    if v_rewardable > 0 and v_shoe.id is not null
       and v_shoe.dur_now * economy.durability_sec_per_point() / v_wear_mult < v_elapsed then
      -- 큰 수(걸음 × 내구도 × 12)가 int 를 넘지 않게 numeric 으로 셈한다
      v_life_steps := floor(v_rewardable::numeric * v_shoe.dur_now * economy.durability_sec_per_point() / v_wear_mult
                            / greatest(v_elapsed, 1))::int;
      if v_worn_rate <= 0 then
        v_rewardable := v_life_steps;
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다');
      end if;
    end if;

    -- 개인 에너지 — 적립 걸음 600 에 1, 착화감만큼 덜. 닳은 구간의 걸음도 에너지는 그대로 쓴다 —
    -- 덜 쓰게 하면 수리하지 않고 오래 걷는 쪽이 하루에 같은 돈을 수리비 없이 번다
    if v_rewardable > 0 then
      -- 에너지 드링크를 고른 러닝은 그 비율만큼만 쓴다 (0106)
      v_energy_per_step := (1 - v_comfort / 10000.0) / economy.steps_per_energy()
                           * economy.run_energy_factor(v_user, p_started_at) * v_enr_mult;
      v_energy_left := economy.energy_left(v_user, v_gday);
      if v_rewardable * v_energy_per_step > v_energy_left then
        v_lost_cap := v_lost_cap + v_rewardable - greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_rewardable := greatest(floor(v_energy_left / v_energy_per_step)::int, 0);
        v_reason := concat_ws(' · ', nullif(v_reason, ''), '에너지를 다 썼습니다');
      end if;
      v_energy_used := round(v_rewardable * v_energy_per_step, 4);
    end if;
    v_full_steps := least(v_rewardable, v_life_steps);
    v_worn_steps := v_rewardable - v_full_steps;
    if v_worn_steps > 0 then
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다');
    end if;
  end if;

  -- 적립 배수 — 24시간 부스터 · 보상 이벤트 · 이 러닝에 고른 아이템 가운데 가장 큰 것 하나(겹치지 않는다, 0106)
  -- 0123: 이벤트 · 24시간 부스터는 이 러닝 시간 가운데 그 창에 든 몫만큼(출발만 창 안에 두고 멈춰 뒀다 나중에 달리던 것) ·
  -- 크루 러닝은 러닝 아이템을 쓰지 않는다(앱도 고르지 않는다 — 묶여 있었으면 아래에서 돌려준다)
  v_xp := economy.run_reward_mult_span(v_user, p_started_at, p_ended_at, v_pool <> 'CREW');
  v_luck := economy.luck_draw('reward_luck');

  if v_pool = 'CREW' then
    -- 기본 B(고른 신발) + B × 인원 보너스(구간 하나) + B × 레벨 보너스 — 더하기, 각 몫을 4자리로 정해 합이 원장과 같게 (R08 · R10)
    -- 0120 크루 효율 8 : 2 — 효율은 내 신발 80% + 함께 달린 크루원 평균 20%. 올릴 때는 둘 가운데 낮은 값(아래 끝)으로 주고,
    -- 함께 달린 사람이 정해질 때마다 together_settle 이 차이만 더한다(함께 달리지 못했으면 내 신발 값으로). 경로 없는 러닝은 내 신발 그대로
    v_unit := v_rewardable * economy.points_per_step() * v_xp * v_luck;
    v_cmult := 1 + v_eff / 10000.0;
    if economy.crew_blend_share() > 0 and v_verdict <> 'VOID' and v_gps_m >= economy.gps_check_min_m() then
      v_cmult := least(v_cmult, economy.crew_blend_mult(v_cr.party_id, v_user, v_cmult, v_unit));  -- 0122: 평균에 달린 만큼 무게
    else
      v_unit := null;
    end if;
    v_base := round(v_rewardable * economy.points_per_step() * v_cmult * v_xp * v_luck, 4);
    v_head_sup := round(v_base * v_headcount, 4);
    v_lvl_sup := round(v_base * v_level_bonus, 4);
    v_points := v_base + v_head_sup + v_lvl_sup;
    v_tb := v_base;
  else
    -- 가득 구간은 다, 닳은 구간은 worn_rate 만큼(0119). worn_rate 가 0 이면 닳은 구간이 없고 예전 배율(내구도 0 이면 0)을 쓴다
    v_points := round(
      (v_full_steps + v_worn_steps * v_worn_rate) * economy.points_per_step()
        * (1 + v_eff / 10000.0)
        * case when v_worn_rate > 0 then 1 else economy.durability_factor(v_dur) end
        * economy.party_multiplier(case when economy.together_on('FLASH') and v_party_id is not null then 1 else v_party end)
        * v_xp * v_luck,
      4
    );
  end if;
  -- 번개 · 옛 방(0120 ACTIVE): 인원 배수는 기록한 뒤 together_settle 이 함께 달린 사람이 올릴 때마다 더한다 — 바탕은 배수 전 적립
  if v_pool = 'PERSONAL' and v_party_id is not null and economy.together_on('FLASH') then
    v_tb := v_points;
  end if;
  v_points_full := v_points;
  if v_rewardable > 0 and v_worn_rate <= 0 and economy.durability_factor(v_dur) < 1 then
    v_reason := concat_ws(' · ', nullif(v_reason, ''), '내구도가 다 닳아 적립이 없습니다');
  end if;

  -- ── 하루 금액 상한 — 가입 7일 안은 절반 ──
  -- 아래 상한(하루 금액 · 경로 없는 러닝 · 올린 날)은 개인 러닝만 — 크루 러닝 보상은 따로이고 아직 상한이 없다
  -- (2026-10-21 사용자 결정 "일단 무제한"). 개인 상한의 합에서도 크루 러닝은 뺀다 (0070)
  -- 0120 — 하루 최대 SUP(사용자 결정): 혼자 · 번개 · 크루 · 보너스 · 목표까지 그날 받는 SUP 의 합 ≤ 이 러닝 신발로 1시간 달린 값 × 4
  -- (보상 이벤트 · 2배권이면 그 배수만큼 · 가입 7일 안은 절반). 크루 러닝도 든다 — 크루 러닝에 상한이 없던 것(점검 P1)
  v_day_max := economy.daily_max_of(v_user, v_eff, v_xp);
  if v_points > 0 and economy.daily_max_on() then
    v_already_points := economy.day_earned(v_user, v_gday);
    if v_points > v_day_max - v_already_points then
      v_points := greatest(round(v_day_max - v_already_points, 4), 0);
      v_verdict := 'FLAGGED';
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '하루 최대 SUP 에 걸렸습니다');
    end if;
  elsif v_points > 0 and v_pool = 'PERSONAL' then
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
  -- 0119: 신발이 닳았든 크루 체력이 비었든 경로로 확인된 걸음은 다 센다(내구도는 적립 비율만 정한다).
  -- worn_rate 가 0 이면 0118 처럼 — 혼자 러닝은 남은 내구도로 달릴 수 있던 시간만큼, 크루 러닝은 공용 체력이 받쳐 준 몫만큼.
  -- 에너지 · 하루 상한과는 상관없다(에너지를 다 쓴 뒤의 걸음도 센다). 하루 48,000 은 합할 때(economy.reward_steps_on)
  v_reward_steps := case
    when v_verdict = 'VOID' or v_held or not v_gps_backed or coalesce(v_verified, 0) <= 0 then 0
    when economy.worn_rate() > 0 then v_verified
    when v_pool = 'CREW' then floor(v_verified * least(greatest(v_frac, 0), 1))::int
    when v_shoe.id is null then v_verified
    when coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() / v_wear_mult < v_elapsed
      then floor(v_verified::numeric * coalesce(v_shoe.dur_now, 0) * economy.durability_sec_per_point() / v_wear_mult
                 / greatest(v_elapsed, 1))::int
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
    crew_id, energy_pool, reward_steps, together_base, day_max, together_unit, together_paid,
    route_sig, route_dts, route_lat0, route_lng0
  )
  values (
    v_user, p_started_at, p_ended_at, v_elapsed, p_steps,
    v_distance_m, p_steps * 0.04, v_track, least(v_eff, 5000), least(greatest(v_party, 1), 20),
    '', case when v_verdict = 'VOID' then 0 else v_top_speed end, v_gps_m,
    v_verdict, v_reason, v_points, v_rewardable,
    coalesce(p_mock_location, false), v_verified, v_energy_used, v_shoe.id,
    v_gps_backed, case when v_gps_backed and not v_held then v_verified else 0 end, v_credit_m, v_party_id,
    v_crew, v_pool, v_reward_steps, v_tb, v_day_max, case when v_pool = 'CREW' then round(v_unit, 8) end,
    -- 0122: 예전 규칙(ALL)이면 인원 보너스를 적립에 이미 넣었다 — 규칙을 다시 켜도 정산이 한 번 더 주지 않게 준 것으로 적는다
    case when v_pool = 'CREW' and not economy.together_on('CREW') then coalesce(v_head_sup, 0) else 0 end,
    v_sig, v_dts, v_lat0, v_lng0
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
  -- 효율 포인트를 넣은 신발은 그 배수만큼 빨리 닳는다(0119 — v_wear_mult, 크루 러닝 · 신발 없음은 1)
  -- 0123: 닳는 시간은 앱이 보낸 시간과 경로가 말하는 움직인 시간 가운데 긴 쪽(달린 시간을 줄여 보내 덜 닳던 것)
  v_wear := case when v_pool = 'CREW' then 0 else round(economy.durability_wear_time(greatest(v_elapsed, least(coalesce(v_moving, 0)::int, v_wall_s)))
                                                              * v_wear_luck * v_wear_mult
              * least(1, greatest(0, (1 - v_lost_cap::numeric / greatest(v_verified, 1))
                                     * case when v_points_full > 0 then v_points / v_points_full else 1 end)))::int end;
  if v_shoe.id is not null and v_verdict <> 'VOID' and v_shoe.status <> 'BURNED' then
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

  -- 0123: 출발 때 신은 신발을 올리기 전에 강화 재료로 태웠다 — 그 신발은 닳을 수 없으니, 이 러닝이 닳게 했을 몫의 수리비를
  -- 이 러닝의 적립에서 뺀다(태울 때는 닳기 전 가득 값으로 경험치를 받았다 — 예전엔 닳음이 통째로 공짜였다)
  if v_shoe.id is not null and v_shoe.status = 'BURNED' and v_verdict <> 'VOID' and v_wear > 0 and v_points > 0 then
    v_wear_cost := least(round(v_wear * economy.repair_unit_of(v_shoe), 4), v_points);
    if v_wear_cost >= 0.0001 then
      perform economy.ledger_apply(v_user, 'SPEND_REPAIR', -v_wear_cost,
                                   format('강화 재료로 태운 신발로 달린 몫의 수리비 (닳음 %s)', v_wear), 'burnwear:' || v_session_id);
      v_reason := concat_ws(' · ', nullif(v_reason, ''), '강화 재료로 태운 신발로 달려 닳은 몫의 수리비를 뺐습니다');
      update public.walk_sessions s set verdict_reason = nullif(v_reason, '') where s.id = v_session_id;
    end if;
  end if;
  if v_pool = 'CREW' then
    perform economy.items_refund(v_user, p_started_at);
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

  -- 함께 달린 보너스(0120) — 이 방에서 함께 달린 사람(10분 · 경로로 확인된 1,500보)을 세어 이미 올린 모두에게 차이만큼 더한다(나도).
  -- 그 뒤 내 러닝의 적립을 다시 읽어 돌려준다(이미 다른 사람이 올렸으면 내 몫의 인원 보너스가 바로 든다)
  if v_tb is not null and v_party_id is not null and v_verdict <> 'VOID' then
    perform economy.together_settle(v_party_id);
    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;
  end if;
  -- 0122: 남이 올릴 때 내가 마침 다른 것을 기록하던 중이라 미뤄 둔 정산 — 내 원장 자물쇠를 잡은 지금 한다
  if exists (select 1 from economy.together_pending q where q.user_id = v_user) then
    perform economy.together_pending_run(v_user);
    select s.points_awarded into v_points from public.walk_sessions s where s.id = v_session_id;
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
  '러닝 세션을 기록하고 적립액을 서버가 계산한다. 신발(출발 때 신은 신발 — 내 것일 때만)·에너지·크루 러닝·파티·부스터·보상 이벤트·러닝 아이템·상한 모두 서버 값으로. 판정 까닭도 돌려준다 (0089) · 운 · 상한 배수 (0116) · 보상 걸음 · 목표 보너스 (0118) · 닳은 뒤 적립 비율 · 효율 포인트의 닳음 / 에너지 (0119) · 함께 달린 사람만 · 하루 최대 SUP (0120) · 묶인 체력 · 8 : 2 무게 · 번개 한 번 · 미룬 정산 (0122) · 가입 전 · 과속 · 거의 같은 경로 · 태운 신발 · 배수 시간 몫 (0123)';
revoke all on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) from public, anon;
grant execute on function public.record_session(timestamptz, timestamptz, int, int, text, int, int, text, boolean, bigint) to authenticated;

-- ════════════════════════════════════════════════════════════════════
--  10. 권한 — 앱(지금 판 · 예전 스토어 판 1.17.1)은 이 표들을 읽기만 하고 RPC 로만 바꾼다
-- ════════════════════════════════════════════════════════════════════
-- 프로필을 직접 고치면 목표(profile_set_daily_goal 의 지난 이레 고정) · 이름(profile_name_set 의 16자 · 제어 문자)을 비켜 갔다
revoke update on public.profiles from anon, authenticated;
-- 직접 지우면 crew_dissolve(달리는 크루) · post_delete(출발한 번개) · course_unshare(남의 기록) · crew_leave 를 비켜 갔다
revoke delete on public.crews, public.posts, public.courses, public.crew_members, public.comments from anon, authenticated;
-- 범위 없이 아무 설정(attester_user_id 까지) — 앱 · 운영자 페이지 · 예전 앱 모두 쓰지 않는다(운영자는 ops_economy_set)
revoke execute on function public.admin_economy_set(text, jsonb) from authenticated, anon, public;
-- 좋아요 · 번개 참석자는 내가 볼 수 있는 글의 것만(크루 글의 참석자를 크루 밖 · 로그인하지 않은 사람이 읽었다)
drop policy if exists post_likes_select_all on public.post_likes;
drop policy if exists post_likes_select_visible on public.post_likes;
create policy post_likes_select_visible on public.post_likes for select
  using (exists (select 1 from public.posts p where p.id = post_likes.post_id));
drop policy if exists flash_participants_select_all on public.flash_participants;
drop policy if exists flash_participants_select_visible on public.flash_participants;
create policy flash_participants_select_visible on public.flash_participants for select
  using (exists (select 1 from public.posts p where p.id = flash_participants.post_id));

revoke all on function economy.track_motion(text) from public, anon, authenticated;
revoke all on function economy.track_sig(text) from public, anon, authenticated;
revoke all on function economy.route_copy_of(uuid, timestamptz, double precision[], int[], double precision, double precision, double precision) from public, anon, authenticated;
revoke all on function economy.run_reward_mult_span(uuid, timestamptz, timestamptz, boolean) from public, anon, authenticated;
revoke all on function economy.items_refund(uuid, timestamptz) from public, anon, authenticated;
revoke all on function economy.items_refund_stale(uuid) from public, anon, authenticated;
revoke all on function economy.together_qualifies(public.walk_sessions) from public, anon, authenticated;
revoke all on function economy.day_room(uuid, date, numeric) from public, anon, authenticated;
revoke all on function economy.goal_streak_before(uuid, date, int) from public, anon, authenticated;
revoke all on function economy.ops_fail(text, text) from public;
revoke all on function economy.forge_xp_quote_for(uuid, bigint, bigint[]) from public, anon, authenticated;
revoke all on function economy.activity_payload(public.chain_jobs) from public, anon, authenticated;

-- ════════════════════════════════════════════════════════════════════
--  11. 한 번만: 모든 곳 잠금 풀기 · 예전 레이스 닫기 · 이야기 지도 다시 · 지난 여드레 러닝의 경로 지문
-- ════════════════════════════════════════════════════════════════════
do $$
begin
  if not exists (select 1 from public.economy_settings where key = 'cheat_fix_20261122') then
    delete from economy.ops_login_locks where scope = 'ALL';
    update public.economy_settings set value = 'false'::jsonb, updated_at = now() where key = 'race_open';
    update public.posts p set run_route = coalesce(public.story_route(s.track), '')
      from public.walk_sessions s where s.id = p.run_session and p.run_route <> '';
    update public.walk_sessions s set route_sig = g.sig, route_dts = g.dts, route_lat0 = g.lat0, route_lng0 = g.lng0
      from (select x.id, (economy.track_sig(x.track)).* from public.walk_sessions x
             where x.created_at > now() - interval '8 days' and x.track <> '' and x.route_sig is null) g
     where s.id = g.id;
    insert into public.economy_settings (key, value) values ('cheat_fix_20261122', to_jsonb(now()));
  end if;
end $$;
