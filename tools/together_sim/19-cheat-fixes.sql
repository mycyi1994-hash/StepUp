-- 검사(0123 — 2차 점검 꼼수 고침): 태운 신발 · 사본 경로 · 가입 전 러닝 · 서버 과속 · 움직인 시간 닳음 · 이벤트 시간 몫 · 크루 러닝 아이템 ·
-- 함께 달린 사람의 가입 시간. 줄마다 OK / FAIL — CI(test-sql.yml)가 FAIL 이 있으면 멈춘다. docs/점검-2026-11-22-2차-꼼수-보안.md
update public.economy_settings set value = '[1,1,1]' where key in ('reward_luck', 'wear_luck');
update public.economy_settings set value = '"ACTIVE"' where key in ('crew_bonus_rule', 'party_bonus_rule');
update public.economy_settings set value = '"HOUR"' where key = 'daily_max_rule';
update public.economy_settings set value = '"ON"' where key = 'runner_level_rule';
delete from economy.reward_events;
create or replace function t.chk(label text, ok boolean, detail text) returns void language plpgsql as $$
begin raise notice '% % — %', case when ok then 'OK  ' else 'FAIL' end, label, detail; end $$;
-- 점 사이 시각 간격이 고르지 않은 경로(실제 폰처럼) — p_seed 로 간격 패턴, p_dlat/p_dlng 로 자리를 옮긴다
create or replace function t.track_irregular(p_start timestamptz, p_secs int, p_seed int, p_dlat numeric default 0, p_dlng numeric default 0,
    p_dps numeric default 0.000023706) returns text language sql as $$
  with ts as (
    -- 매 초 ±20ms 흔들린 시각(폰마다 다른 패턴) — 간격이 고르지 않다
    select i, i * 1000 + case when i = 0 then 0 else ((i * i * 31 + i * 17 + p_seed * 101) % 41) - 20 end as ms
      from generate_series(0, p_secs - 1) i
  )
  select string_agg(format('%s,%s,%s', (37.5 + p_dlat + (ms / 1000.0) * p_dps)::numeric(14, 8), (127 + p_dlng)::numeric(14, 8),
                           (extract(epoch from p_start) * 1000)::bigint + ms), ';' order by i)
    from ts
$$;
create or replace function t.rec_track(n int, p_start timestamptz, p_secs int, p_steps int, p_track text, p_dur int default null, p_sneaker bigint default null)
returns record language plpgsql as $$
declare r record;
begin
  perform t.login(n);
  select * into r from public.record_session(p_start, p_start + make_interval(secs => p_secs), p_steps, coalesce(p_dur, p_secs),
    p_track, 0, 1, '', false, p_sneaker);
  return r;
end $$;
-- 두 번째 신발(일반) — 신지 않은 채
create or replace function t.shoe(n int, p_max int default 150) returns bigint language plpgsql as $$
declare v bigint;
begin
  insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability, origin, efficiency_bps, comfort_bps, equipped, dur_base)
  values (t.u(n), 'NONE', 'COMMON', 0, 1, 100, 'FREE_DRAW', 4000, 0, false, p_max) returning id into v;
  update public.market_sneakers set dur_now = dur_max where id = v;
  return v;
end $$;

-- (가) 태운 신발: 닷새 전에 강화 재료로 태운 신발을 p_sneaker 로 — 그 신발로 셈하지 않는다(신은 신발이 닳는다).
--      이 러닝을 시작한 뒤에 태웠으면 그 신발로 셈하되 닳을 몫의 수리비를 적립에서 뺀다(태울 때 가득 값으로 경험치를 받았다)
do $$
declare x bigint; y bigint; s0 int; s1 int; v_start timestamptz; r record; w public.walk_sessions; v_cost numeric;
begin
  perform t.mk(1);
  x := t.shoe(1); y := t.shoe(1);
  update public.market_sneakers set status = 'BURNED' where id in (x, y);
  insert into public.forge_requests (user_id, request_key, status, material_ids, created_at)
  values (t.u(1), gen_random_uuid(), 'SUCCESS', array[x], now() - interval '5 days');
  select dur_now into s0 from public.market_sneakers where owner_id = t.u(1) and equipped;
  v_start := now() - interval '3 hours';
  r := t.rec_track(1, v_start, 1200, 3000, t.track(v_start, 1200), null, x);
  select * into w from public.walk_sessions where id = r.session_id;
  select dur_now into s1 from public.market_sneakers where owner_id = t.u(1) and equipped;
  perform t.chk('(가) 닷새 전에 태운 신발로는 셈하지 않는다 — 신은 신발이 닳는다', w.sneaker_id <> x and s1 < s0,
                format('기록 신발 %s (태운 %s) · 신은 신발 %s → %s', w.sneaker_id, x, s0, s1));
  -- 러닝을 시작한 뒤에 태운 신발
  v_start := now() - interval '1 hour';
  insert into public.forge_requests (user_id, request_key, status, material_ids, created_at)
  values (t.u(1), gen_random_uuid(), 'SUCCESS', array[y], now() - interval '10 minutes');
  r := t.rec_track(1, v_start, 1200, 3000, t.track(v_start, 1200), null, y);
  select * into w from public.walk_sessions where id = r.session_id;
  select -sum(l.amount) into v_cost from public.sup_ledger l where l.user_id = t.u(1) and l.ref = 'burnwear:' || r.session_id;
  perform t.chk('(가) 러닝 뒤에 태운 신발 — 그 신발로 셈하고 닳을 몫의 수리비를 뺀다', w.sneaker_id = y and coalesce(v_cost, 0) > 0,
                format('기록 신발 %s · 적립 %s · 뺀 수리비 %s', w.sneaker_id, w.points_awarded, v_cost));
end $$;

-- (나) 사본 경로: u2 가 올린 경로를 u3 이 좌표를 1cm 옮기고 시각을 통째로 3초 밀어 올린다 → 무효.
--      u4 는 같은 길을 다른 폰(다른 시각 간격 · 4m 옆)으로 → 정상
do $$
declare v_start timestamptz := now() - interval '2 hours'; r2 record; r3 record; r4 record; tr text;
begin
  perform t.mk(2); perform t.mk(3); perform t.mk(4);
  tr := t.track_irregular(v_start, 1200, 7);
  r2 := t.rec_track(2, v_start, 1200, 3000, tr);
  r3 := t.rec_track(3, v_start + interval '3 seconds', 1200, 3000, t.track_irregular(v_start + interval '3 seconds', 1200, 7, 0.0000001, 0.0000001));
  r4 := t.rec_track(4, v_start, 1200, 3000, t.track_irregular(v_start, 1200, 19, 0.00004, 0.00004));
  perform t.chk('(나) 좌표만 조금 옮기고 시각을 민 사본은 무효 · 같은 길을 다른 폰으로 달린 사람은 정상',
                r2.verdict <> 'VOID' and r3.verdict = 'VOID' and r3.verdict_reason like '%이미 올린 경로와 같습니다%' and r4.verdict <> 'VOID',
                format('원본 %s · 사본 %s [%s] · 다른 폰 %s [%s]', r2.verdict, r3.verdict, r3.verdict_reason, r4.verdict, r4.verdict_reason));
end $$;

-- (다) 가입하기 전에 시작한 러닝 — 무효
do $$
declare v_start timestamptz := now() - interval '2 days'; r record;
begin
  perform t.mk(5);
  update public.profiles set created_at = now() - interval '1 hour' where id = t.u(5);
  r := t.rec_track(5, v_start, 1200, 3000, t.track(v_start, 1200));
  perform t.chk('(다) 가입하기 전에 시작한 러닝은 무효', r.verdict = 'VOID' and r.verdict_reason like '%가입하기 전%', format('%s [%s]', r.verdict, r.verdict_reason));
end $$;

-- (라) 서버 과속: 40km/h 로 90초 이어진 구간이 있는 러닝 — 앱의 신고가 없으면 무효 · 앱이 신고했으면(멈춤 · 패널티) 그 규칙대로
create or replace function t.track_fast(p_start timestamptz, p_secs int, p_fast_from int, p_fast_secs int) returns text language sql as $$
  with s as (
    select i, sum(case when i between p_fast_from and p_fast_from + p_fast_secs then 0.0000998 else 0.000023706 end) over (order by i) as lat
      from generate_series(0, p_secs) i
  )
  select string_agg(format('%s,%s,%s', (37.5 + lat)::numeric(14, 8), 127.0, (extract(epoch from p_start) * 1000)::bigint + i * 1000), ';' order by i) from s
$$;
do $$
declare v_start timestamptz := now() - interval '2 hours'; r6 record; r7 record;
begin
  perform t.mk(6); perform t.mk(7);
  r6 := t.rec_track(6, v_start, 1200, 3600, t.track_fast(v_start, 1200, 300, 90));
  insert into public.run_penalties (user_id, run_started_at, stopped_at, banned_until)
  values (t.u(7), v_start, v_start + interval '20 minutes', v_start + interval '30 minutes');
  r7 := t.rec_track(7, v_start, 1200, 3600, replace(t.track_fast(v_start, 1200, 300, 90), ',127.0,', ',127.0002,'));
  perform t.chk('(라) 시속 40km 90초 — 앱 신고 없으면 무효 · 신고한 러닝은 과속 멈춤 규칙(무효 아님)',
                r6.verdict = 'VOID' and r6.verdict_reason like '%러닝 속도를 넘는%' and r7.verdict <> 'VOID',
                format('신고 없음 %s [%s] · 신고함 %s [%s]', r6.verdict, r6.verdict_reason, r7.verdict, r7.verdict_reason));
end $$;

-- (마) 닳음은 경로가 말하는 움직인 시간으로 — 20분 달리고 달린 시간을 5분으로 보내도 20분만큼 닳는다
do $$
declare v_start timestamptz := now() - interval '2 hours'; r record; d0 int; d1 int;
begin
  perform t.mk(8, 1000);
  select dur_now into d0 from public.market_sneakers where owner_id = t.u(8) and equipped;
  r := t.rec_track(8, v_start, 1200, 1150, t.track(v_start, 1200), 300);
  select dur_now into d1 from public.market_sneakers where owner_id = t.u(8) and equipped;
  perform t.chk('(마) 달린 시간을 줄여 보내도 경로로 움직인 시간만큼 닳는다(20분 = 약 100)', d0 - d1 >= 90,
                format('%s → %s (닳음 %s · 보낸 시간 300초 · 경로 1200초) [%s]', d0, d1, d0 - d1, r.verdict_reason));
end $$;

-- (바) 보상 이벤트 × 3 이 출발 1분 뒤 끝났다 — 30분 러닝은 그 시간 몫만큼(약 × 1.07, 예전엔 × 3)
do $$
declare v_start timestamptz := now() - interval '2 hours'; r record; w public.walk_sessions;
begin
  perform t.mk(9);
  insert into economy.reward_events (request_id, title, mult, starts_at, ends_at, actor)
  values (gen_random_uuid(), '검사 3배', 3, v_start - interval '3 hours', v_start + interval '1 minute', 'sim');
  r := t.rec_track(9, v_start, 1800, 3000, t.track(v_start, 1800));
  select * into w from public.walk_sessions where id = r.session_id;
  perform t.chk('(바) 이벤트 배수는 러닝 시간 가운데 이벤트에 든 몫만큼', w.reward_mult between 1.05 and 1.09,
                format('적립 배수 %s (출발 뒤 1분만 이벤트 · 30분 러닝)', w.reward_mult));
  delete from economy.reward_events;
end $$;

-- (사) 크루 러닝에 묶은 보상 2배권 — 쓰지 않고 돌려준다 · (아) 방금 가입한 크루원은 함께 달린 사람이 아니다
do $$
declare p bigint; v jsonb; v_start timestamptz; n int; w public.walk_sessions; c economy.crew_runs; q int;
begin
  perform t.mk(11); perform t.mk(12); perform t.mk(13);
  update public.profiles set created_at = now() - interval '2 hours' where id = t.u(13);
  perform t.crew(11, 11, array[12, 13], 1);
  perform economy.item_add(t.u(11), 'REWARD_DOUBLE', 1);
  perform t.login(11);
  p := public.party_create_v2(t.c(11), '아이템', null, null, null, gen_random_uuid(), 'REMOTE', null, null, null, null, null);
  perform public.party_ready(p, true);
  for n in 12 .. 13 loop perform t.login(n); perform public.party_join(p); perform public.party_ready(p, true); end loop;
  perform t.login(11); perform public.party_start_rev(p, 0);
  perform t.age(interval '6 seconds');
  perform t.run(p, array[11, 12, 13], 1200);
  for n in 11 .. 13 loop perform t.login(n); v := public.crew_run_beat(p, 'ENDED'); end loop;
  select started_at into v_start from economy.crew_runs where user_id = t.u(11);
  -- 수정한 앱이 크루 러닝 시작 시각에 보상 2배권을 묶는다(앱은 크루 러닝에 아이템을 고르지 않는다)
  perform t.login(11); v := public.run_items_use(v_start, array['REWARD_DOUBLE']);
  for n in 11 .. 13 loop perform t.rec(n, v_start, 1200, 3300); end loop;
  select * into w from public.walk_sessions x where x.user_id = t.u(11) and x.party_id = p;
  select qty into q from public.user_items where user_id = t.u(11) and kind = 'REWARD_DOUBLE';
  perform t.chk('(사) 크루 러닝은 보상 2배권을 쓰지 않고 가방으로 돌려준다', w.reward_mult = 1 and q = 1,
                format('적립 배수 %s · 가방 %s개', w.reward_mult, q));
  select * into c from economy.crew_runs x where x.user_id = t.u(11) and x.party_id = p;
  perform t.chk('(아) 방금(2시간 전) 가입한 크루원은 함께 달린 사람이 아니다 — 인원 2명', c.bonus_count = 2,
                format('u11 인원 %s명 (셋이 달림 · 하나는 가입 2시간)', c.bonus_count));
end $$;
