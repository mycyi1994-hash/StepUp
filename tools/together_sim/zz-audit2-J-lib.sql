-- 점검 2 (J, 2026-11-22) 장면 도우미 — 부정 행위(루팅 폰 · API 를 직접 부르는 스크립트) 갈래. CI 밖(run.sh 는 숫자로 시작하는 장면만).
-- 장면에서 `\ir zz-audit2-J-lib.sql` 로 부른다. 장면은 begin; … rollback; 안에서 돈다.
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
-- 운영과 같게: 러너 레벨 켬 · 함께 달린 사람만 · 하루 최대 SUP
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';

-- 0121 전부터 있던 계정처럼(하루 에너지 10 보장)
create or replace function t.old(n int) returns void language sql as $$
  insert into economy.runner_levels (user_id, legacy_energy) values (t.u(n), 10)
  on conflict (user_id) do update set legacy_energy = 10 $$;
-- 가입 선물 첫 신발 그대로(일반 · 효율 4000 · 착화감 2857 · 내구도 155) — t.mk 는 착화감을 0 으로 바꾼다
create or replace function t.mk0(n int, p_created timestamptz default now() - interval '40 days') returns uuid language plpgsql as $$
declare v uuid := t.u(n);
begin
  insert into auth.users (id, email) values (v, 'j' || n || '@test.invalid') on conflict do nothing;
  perform t.login(n);
  perform public.economy_bootstrap();
  update public.profiles set display_name = '러너' || n, created_at = p_created where id = v;
  return v;
end $$;
-- 경로: 북쪽으로 p_mps(m/s) 곧게 · p_every 초마다 한 점 · 시작점 · 시각 밀기(ms)
create or replace function t.trk(p_start timestamptz, p_secs int, p_mps numeric default 2.636, p_every int default 5,
                                 p_lat0 numeric default 37.5, p_lng0 numeric default 127.0) returns text language sql as $$
  select string_agg(format('%s,%s,%s', (p_lat0 + i * p_mps / 111320.0)::numeric(12, 7), p_lng0::numeric(12, 7),
                           (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
    from generate_series(0, p_secs, p_every) i $$;
-- 경로 옮기기 — 모든 점의 위도 + p_dlat(도), 시각 + p_dms(ms)
create or replace function t.shift(p_track text, p_dlat numeric, p_dms bigint) returns text language sql as $$
  select string_agg(format('%s,%s,%s', (split_part(c, ',', 1)::numeric + p_dlat)::numeric(12, 7), split_part(c, ',', 2),
                           split_part(c, ',', 3)::bigint + p_dms), ';' order by o)
    from unnest(string_to_array(p_track, ';')) with ordinality u(c, o) $$;
-- 점마다 ±p_m 미터 무작위 흔들기(같은 씨앗 → 같은 결과)
create or replace function t.jit(p_track text, p_m numeric, p_seed int) returns text language sql as $$
  select string_agg(format('%s,%s,%s',
           (split_part(c, ',', 1)::numeric + (((abs(hashtext(p_seed || ':a:' || o)) % 2001) - 1000) / 1000.0) * p_m / 111320.0)::numeric(12, 7),
           (split_part(c, ',', 2)::numeric + (((abs(hashtext(p_seed || ':b:' || o)) % 2001) - 1000) / 1000.0) * p_m / 88000.0)::numeric(12, 7),
           split_part(c, ',', 3)), ';' order by o)
    from unnest(string_to_array(p_track, ';')) with ordinality u(c, o) $$;
-- 러닝을 올린다 — 경로 · 앱이 보낸 달린 초(null 이면 벽시계)를 직접
create or replace function t.up(n int, p_start timestamptz, p_secs int, p_steps int, p_track text, p_dur int default null,
                                p_mock boolean default false)
returns public.walk_sessions language plpgsql as $$
declare r record; s public.walk_sessions;
begin
  perform t.login(n);
  select * into r from public.record_session(p_start, p_start + make_interval(secs => p_secs), p_steps, coalesce(p_dur, p_secs),
    p_track, 0, 1, '', p_mock, null);
  select * into s from public.walk_sessions where id = r.session_id;
  return s;
end $$;
create or replace function t.line(s public.walk_sessions) returns text language sql as $$
  select format('%s pts=%s rewarded=%s reward_steps=%s verified=%s energy=%s dur=%s gps=%sm dist=%sm top=%s reason=[%s]',
                s.verdict, s.points_awarded, s.rewarded_steps, s.reward_steps, s.verified_steps, s.energy_used, s.duration_sec,
                round(s.gps_distance_m::numeric), round(s.distance_meters::numeric), round(s.top_speed_kmh::numeric, 1), s.verdict_reason) $$;
-- 계정 한 줄: 잔고 · 상자(일반 / 상급) · 러너 레벨 · 목표 보너스 합
create or replace function t.me(n int) returns text language sql as $$
  select format('u%s bal=%s free_boxes=%s gift_boxes=%s runner_lv=%s runner_steps=%s goal_sup=%s goal_days=%s energy_max=%s',
                n, round(economy.balance_of(t.u(n)), 2),
                (select count(*) from economy.box_awards a where a.user_id = t.u(n) and a.kind = 'FREE' and a.source <> 'SIGNUP'),
                (select count(*) from economy.box_awards a where a.user_id = t.u(n) and a.kind = 'GIFT'),
                (select r.level from economy.runner_levels r where r.user_id = t.u(n)),
                (select r.steps from economy.runner_levels r where r.user_id = t.u(n)),
                coalesce((select sum(l.amount) from public.sup_ledger l where l.user_id = t.u(n) and l.ref like 'goal:%'), 0),
                (select count(*) from economy.goal_days g where g.user_id = t.u(n) and g.met_at is not null),
                economy.energy_max(t.u(n))) $$;
-- 신발 한 줄
create or replace function t.shoe(n int) returns text language sql as $$
  select format('shoe dur=%s/%s repair_unit=%s', s.dur_now, s.dur_max, round(economy.repair_cost_per_unit(s.rarity, s.level), 4))
    from public.market_sneakers s where s.owner_id = t.u(n) and s.equipped $$;
grant execute on all functions in schema t to public;
-- 사람마다 다른 자리의 경로(경도 n × 0.001)
create or replace function t.trku(n int, p_start timestamptz, p_secs int, p_mps numeric default 2.636) returns text language sql as $$
  select t.trk(p_start, p_secs, p_mps, 5, 37.5, 127 + n * 0.001) $$;
grant execute on all functions in schema t to public;
