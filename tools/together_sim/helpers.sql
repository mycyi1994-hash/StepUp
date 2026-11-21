-- 함께 뛰기 장면 검사 도우미 — tools/together_sim/run.sh 가 먼저 올린다. 가짜 사용자 · 크루 · 시계 옮기기 · 박동 · 러닝 올리기.
drop schema if exists t cascade;
create schema t;
create table t.fix (k text primary key, v text);
create or replace function t.u(n int) returns uuid language sql immutable as $$ select ('7e570000-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid $$;
create or replace function t.c(n int) returns uuid language sql immutable as $$ select ('7e57cccc-0000-0000-0000-' || lpad(n::text, 12, '0'))::uuid $$;
create or replace function t.login(n int) returns void language plpgsql as $$
begin perform set_config('request.jwt.claim.sub', t.u(n)::text, false); end $$;
create or replace function t.say(label text, val anyelement) returns void language plpgsql as $$
begin raise notice '% => %', label, val; end $$;
-- 사용자 n 을 만든다(첫 신발 포함). 신발: 최대 내구도 · 효율
create or replace function t.mk(n int, p_max int default 150, p_eff int default 4000) returns uuid language plpgsql as $$
declare v uuid := t.u(n); v_id bigint;
begin
  insert into auth.users (id, email) values (v, 'tg' || n || '@test.invalid') on conflict do nothing;
  perform t.login(n);
  perform public.economy_bootstrap();
  update public.profiles set display_name = '러너' || n, created_at = now() - interval '40 days' where id = v;
  update public.market_sneakers
     set level = 1, efficiency_bps = p_eff, comfort_bps = 0, pts_eff = 0, pts_comfort = 0, pts_dur = 0, dur_base = p_max, dur_now = p_max
   where owner_id = v and equipped;
  return v;
end $$;
-- 크루 k: 크루장 owner + 크루원들
create or replace function t.crew(k int, owner int, members int[], p_level int default 1) returns uuid language plpgsql as $$
declare v uuid := t.c(k); m int;
begin
  insert into public.crews (id, owner_id, name) values (v, t.u(owner), '검사 크루 ' || k);
  insert into public.crew_members (crew_id, user_id, role) values (v, t.u(owner), 'OWNER') on conflict do nothing;
  foreach m in array members loop
    insert into public.crew_members (crew_id, user_id, role) values (v, t.u(m), 'MEMBER') on conflict do nothing;
  end loop;
  update public.crews set level = p_level where id = v;
  return v;
end $$;
-- 시간을 흘려 보낸다 — 기록을 뒤로 옮긴다
create or replace function t.age(p interval) returns void language plpgsql as $$
begin
  update public.parties set starts_at = starts_at - p, created_at = created_at - p, plan_at = plan_at - p;
  update public.party_members set joined_at = joined_at - p, last_seen = last_seen - p, pinged_at = pinged_at - p;
  update public.party_runs set starts_at = starts_at - p, last_ping = last_ping - p;
  update public.party_near set first_at = first_at - p, last_at = last_at - p;
  update economy.crew_runs set started_at = started_at - p, last_beat_at = last_beat_at - p, ended_at = ended_at - p, created_at = created_at - p;
  update economy.crew_day_pools set settled_at = settled_at - p, exhausted_at = exhausted_at - p, low_at = low_at - p, created_at = created_at - p;
  update economy.crew_pool_empty set from_at = from_at - p, to_at = to_at - p;
  update public.walk_sessions set started_at = started_at - p, ended_at = ended_at - p;
end $$;
-- 달리는 동안 p_secs 초를 5초마다 박동하며 보낸다(달리는 사람 목록)
create or replace function t.run(p_party bigint, p_users int[], p_secs int, p_state text default 'RUNNING') returns void language plpgsql as $$
declare i int; m int; v jsonb;
begin
  for i in 1 .. p_secs / 5 loop
    perform t.age(interval '5 seconds');
    foreach m in array p_users loop
      perform t.login(m);
      v := public.crew_run_beat(p_party, p_state);
    end loop;
  end loop;
end $$;
-- 경도는 사람마다 0.5m 안쪽으로 다르게(동쪽으로만) — 같은 경로 검사가 모든 계정끼리 맞대어 보므로(0118) 함께 출발한 둘째가 무효가 되지 않게
create or replace function t.track(p_start timestamptz, p_secs int, p_dps numeric default 0.000023706) returns text language sql as $$
  select string_agg(format('%s,%s,%s', (37.5 + i * p_dps)::numeric(12, 6), (127 + (abs(hashtext(coalesce(current_setting('request.jwt.claim.sub', true), ''))) % 49 + 1) * 0.0000001)::numeric(12, 7),
           (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i)
  from generate_series(0, p_secs) i $$;
-- 러닝을 올린다(사용자 n, 시작, 초, 걸음) — 결과 한 줄
create or replace function t.rec(n int, p_start timestamptz, p_secs int, p_steps int, p_track boolean default true, p_sneaker bigint default null)
returns text language plpgsql as $$
declare r record; s public.walk_sessions;
begin
  perform t.login(n);
  select * into r from public.record_session(p_start, p_start + make_interval(secs => p_secs), p_steps, p_secs,
    case when p_track then t.track(p_start, p_secs) else '' end, 0, 1, '', false, p_sneaker);
  select * into s from public.walk_sessions where id = r.session_id;
  return format('u%s %s pts=%s pool=%s party=%s size=%s rewarded=%s energy=%s reason=[%s] luck=%s', n, r.verdict, r.points_awarded, s.energy_pool, s.party_id, s.party_size,
    s.rewarded_steps, s.energy_used, r.verdict_reason, r.reward_luck);
end $$;
create or replace function t.pool(k int) returns text language sql as $$
  select format('cap=%s used=%s left=%s contributors=%s exhausted=%s', round(d.capacity,2), round(d.consumed,2), round(d.capacity-d.consumed,2), d.contributors, d.exhausted_at is not null)
    from economy.crew_day_pools d where d.crew_id = t.c(k) order by d.day desc limit 1 $$;
create or replace function t.cr(n int) returns text language sql as $$
  select format('u%s state=%s reason=%s contrib=%s used=%s active=%s empty=%s bonus=%s/%s/%s eff=%s session=%s total=%s', n, c.state, c.terminal_reason, c.contribution, round(c.consumed,2), c.active_sec, c.empty_sec,
     c.bonus_status, c.bonus_count, c.bonus_pct, c.shoe_eff_bps, c.session_id, c.total_sup)
    from economy.crew_runs c where c.user_id = t.u(n) order by c.day desc limit 1 $$;
grant usage on schema t to public; grant all on all tables in schema t to public; grant execute on all functions in schema t to public;
-- 운을 끈다(값을 맞대어 보려고)
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');

-- 러너 레벨(0121)은 계정 에너지를 바꾼다(새 계정 Lv1 = 20분). 이 장면들은 그 전 규칙(하루 에너지 10)으로 맞춰 쓴 것이라 끄고 돌린다
update public.economy_settings set value = '"OFF"' where key = 'runner_level_rule';
