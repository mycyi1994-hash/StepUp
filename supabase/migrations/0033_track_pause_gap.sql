-- ════════════════════════════════════════════════════════════════════
--  0033 — 일시정지로 생긴 경로의 빈 구간은 재지 않는다 (2026-09-25 점검)
--
--  러닝을 멈춘 동안 앱은 경로에 점을 넣지 않는다. 다시 시작하면 멈추기 전 마지막 점과
--  다시 시작한 뒤 첫 점이 바로 이어져, 서버는 그 사이를 한 구간으로 쟀다.
--    · 멈추고 버스를 타면(시속 60km 아래) 그 거리가 경로 거리에 들어가, "걸음 없이
--      이동한 거리"로 정직한 러닝이 VOID 되고 최고 속도도 버스 속도로 남았다.
--    · 멈추고 걸어간 거리도 경로로 잰 거리(잠금 · 꺼내기 조건)에 들어갔다.
--
--  앱은 8m 이상 움직일 때마다 점을 넣으므로, 달리는 중에는 점 사이가 몇 초다. 1분 넘게
--  빈 구간은 멈췄거나 GPS 가 끊긴 것이라 거리 · 속도 어느 쪽에도 넣지 않고 새로 잰다.
--  거리를 빼기만 하므로 적립이 늘어날 수는 없다.
-- ════════════════════════════════════════════════════════════════════

create or replace function economy.track_gap_sec() returns int
  language sql immutable as $$ select 60 $$;

create or replace function economy.track_summary(p_track text)
returns table (
  gps_m double precision,
  points int,
  first_at bigint,
  last_at bigint
)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  v_plat double precision;
  v_plng double precision;
  v_pat bigint;
  v_have boolean := false;
  v_d double precision;
  v_dt double precision;
  v_total double precision := 0;
  v_n int := 0;
  v_first bigint;
  v_last bigint;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0, null::bigint, null::bigint;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;
    continue when v_lat not between -90 and 90 or v_lng not between -180 and 180;

    v_n := v_n + 1;
    v_first := least(coalesce(v_first, v_at), v_at);
    v_last := greatest(coalesce(v_last, v_at), v_at);

    if v_have then
      v_d := economy.haversine_m(v_plat, v_plng, v_lat, v_lng);
      v_dt := (v_at - v_pat) / 1000.0;
      -- 튄 구간(시속 60km 초과), 시각이 같거나 거꾸로 간 점, 1분 넘게 빈 구간(일시정지 ·
      -- GPS 끊김)은 거리에 넣지 않는다.
      if v_dt > 0 and v_dt <= economy.track_gap_sec()
         and v_d / v_dt * 3.6 <= economy.speed_glitch_kmh() then
        v_total := v_total + v_d;
      end if;
    end if;
    v_plat := v_lat; v_plng := v_lng; v_pat := v_at; v_have := true;
  end loop;

  return query select v_total, v_n, v_first, v_last;
end;
$$;

create or replace function economy.track_speed_stats(p_track text)
returns table (top_speed_kmh double precision, glitch_ratio double precision)
language plpgsql immutable as $$
declare
  v_num constant text := '^-?[0-9]+(\.[0-9]+)?$';
  v_chunk text;
  v_parts text[];
  v_lat double precision;
  v_lng double precision;
  v_at bigint;
  v_blat double precision;
  v_blng double precision;
  v_bat bigint;
  v_have_base boolean := false;
  v_dt double precision;
  v_kmh double precision;
  v_best double precision := 0;
  v_windows int := 0;
  v_glitches int := 0;
begin
  if p_track is null or p_track = '' then
    return query select 0::double precision, 0::double precision;
    return;
  end if;

  foreach v_chunk in array string_to_array(p_track, ';') loop
    v_parts := string_to_array(v_chunk, ',');
    continue when v_parts is null or array_length(v_parts, 1) <> 3;
    continue when v_parts[1] !~ v_num or v_parts[2] !~ v_num or v_parts[3] !~ '^[0-9]+$';

    v_lat := v_parts[1]::double precision;
    v_lng := v_parts[2]::double precision;
    v_at := v_parts[3]::bigint;

    if not v_have_base then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at; v_have_base := true;
      continue;
    end if;

    v_dt := (v_at - v_bat) / 1000.0;
    -- 시계가 거꾸로 갔거나 1분 넘게 빈 구간(일시정지 · GPS 끊김)이면 기준을 다시 잡는다
    if v_dt <= 0 or v_dt > economy.track_gap_sec() then
      v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
      continue;
    end if;
    continue when v_dt < economy.speed_window_sec();

    v_kmh := economy.haversine_m(v_blat, v_blng, v_lat, v_lng) / v_dt * 3.6;
    v_windows := v_windows + 1;
    if v_kmh > economy.speed_glitch_kmh() then
      v_glitches := v_glitches + 1;
    elsif v_kmh > v_best then
      v_best := v_kmh;
    end if;

    v_blat := v_lat; v_blng := v_lng; v_bat := v_at;
  end loop;

  return query select
    least(v_best, economy.speed_record_cap_kmh()),
    case when v_windows = 0 then 0::double precision
         else v_glitches::double precision / v_windows end;
end;
$$;
