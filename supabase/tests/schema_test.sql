-- 스키마가 약속한 것을 실제로 지키는지 확인한다.
--
-- 이 파일은 Supabase 에 올리는 것이 아니다. 로컬 Postgres 에서 돌린다:
--
--     supabase/tests/run.sh
--
-- 맞지 않는 것이 하나라도 있으면 그 자리에서 멈춘다. "아마 될 것이다"로
-- 넘어간 규칙은 실제로는 안 되는 경우가 많고, 돈이 걸린 표에서는 그 차이가
-- 나중에 원장으로 나타난다.

\set ON_ERROR_STOP on
set client_min_messages = notice;

create or replace function pg_temp.ok(cond boolean, label text) returns void
language plpgsql as $$
begin
  if cond then
    raise notice '  OK   %', label;
  else
    raise exception 'FAIL  %', label;
  end if;
end $$;

-- 로그인 흉내. auth.uid() 가 이 값을 읽는다.
-- 함수가 아니라 프로시저인 것은 call 이 결과 행을 찍지 않아서다 —
-- 검사 결과 사이에 빈 표가 끼면 읽기가 나빠진다.
create or replace procedure pg_temp.login(p_user uuid)
language plpgsql as $$
begin
  perform set_config('request.jwt.claim.sub', coalesce(p_user::text, ''), false);
  -- 사용자로 로그인하면 어테스터 역할 표시만 지운다 (2단계 인증 aal 은 그대로 둔다)
  perform set_config('request.jwt.claims',
    coalesce((nullif(current_setting('request.jwt.claims', true), '')::jsonb - 'role')::text, ''), false);
end $$;

-- 실패해야 하는 일. 성공해 버리면 그게 사고다.
create or replace procedure pg_temp.must_fail(p_sql text, label text)
language plpgsql as $$
begin
  begin
    execute p_sql;
  exception when others then
    raise notice '  OK   % (%)', label, replace(sqlerrm, E'\n', ' ');
    return;
  end;
  raise exception 'FAIL  % — 막혔어야 하는데 통과했다', label;
end $$;

-- 준비물 하나 꺼내기.
-- CALL 의 인자에는 서브쿼리를 넣을 수 없어서 함수로 감싼다.
create or replace function pg_temp.fx(p_key text) returns text
language plpgsql as $$
declare v_out text;
begin
  select v into v_out from fix where k = p_key;
  return v_out;
end $$;

-- 일정한 속도로 달린 경로를 만든다.
-- @param p_dps  1초에 움직이는 위도(도). 0.00003 이면 대략 시속 12km.
create or replace function pg_temp.track(p_start timestamptz, p_secs int, p_dps numeric)
returns text language sql as $$
  select string_agg(
    format('%s,%s,%s',
      (37.5 + i * p_dps)::numeric(12, 6),
      127.000000,
      (extract(epoch from p_start) * 1000)::bigint + i * 1000),
    ';' order by i)
  from generate_series(0, p_secs) i
$$;

create temp table fix (k text primary key, v text);
-- 앱 권한(authenticated)으로 바꿔 검사하는 동안에도 준비물은 읽어야 한다.
-- 앱 권한(authenticated)으로 바꿔 검사하는 동안에도 준비물은 읽어야 한다.
grant all on fix to authenticated;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 준비 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

insert into auth.users (id, email, raw_user_meta_data) values
  ('11111111-1111-1111-1111-111111111111', 'a@test', '{"full_name":"Ara Kim"}'),
  ('22222222-2222-2222-2222-222222222222', 'b@test', '{"full_name":"Bo Lee"}'),
  ('33333333-3333-3333-3333-333333333333', 'c@test', '{"full_name":"Cha Park"}');

do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.profiles) = 3,
    '가입하면 프로필이 자동으로 생긴다');
  perform pg_temp.ok(
    (select display_name from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') = 'Ara Kim',
    '구글이 준 이름이 프로필에 들어간다');
end $$;

-- 모든 세션 시각을 "어제 10시"에서 잰다.
--
-- now() 에 매달면 자정 무렵에 돌릴 때 세션이 이틀에 걸쳐, 하루 상한 검사가
-- 이유 없이 무너진다. 테스트가 시계에 따라 결과가 달라지면 그 테스트는
-- 못 믿는다.
-- 게임의 하루는 한국 시간이다(0022). 어제 한국 09시에서 시작하면 +8시간까지 같은 하루에 든다.
insert into fix (k, v) values
  ('base', (((now() at time zone 'Asia/Seoul')::date - 1)::timestamp at time zone 'Asia/Seoul'
            + interval '9 hours')::text);

insert into fix (k, v)
  select 'run_start', (pg_temp.fx('base')::timestamptz)::text;
insert into fix (k, v)
  select 'void_start', (pg_temp.fx('base')::timestamptz + interval '2 hours')::text;
insert into fix (k, v)
  select 'car_start', (pg_temp.fx('base')::timestamptz + interval '3 hours')::text;
insert into fix (k, v)
  select 'cap_start', (pg_temp.fx('base')::timestamptz + interval '4 hours')::text;
insert into fix (k, v)
  select 'last_start', (pg_temp.fx('base')::timestamptz + interval '6 hours')::text;

insert into fix (k, v)
  select 'track_ok', pg_temp.track(pg_temp.fx('run_start')::timestamptz, 600, 0.00003);

-- 새 경제 규칙(0022~0024)의 에너지·신규 계정 상한이 아래 옛 검사(걸음 상한 등)를
-- 가리지 않게 에너지를 넉넉히 주고 신규 계정 절반 상한을 끈다.
-- 새 규칙 자체는 뒤의 '서버 경제' 에서 따로 검사한다.
update public.economy_settings set value = '0' where key = 'new_account_days';
update public.economy_settings set value = '100000' where key in ('no_gps_daily_cap', 'upload_daily_cap');
insert into public.energy_days (user_id, day, bonus)
select u.id, d::date, 1000
  from auth.users u, generate_series(current_date - 8, current_date + 1, interval '1 day') d
on conflict do nothing;
insert into fix (k, v)
  select 'track_car', pg_temp.track(pg_temp.fx('car_start')::timestamptz, 600, 0.002);

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 경로에서 속도 읽기 ───────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

do $$
declare v_kmh double precision; v_glitch double precision;
begin
  select top_speed_kmh, glitch_ratio into v_kmh, v_glitch
    from economy.track_speed_stats(pg_temp.fx('track_ok'));
  perform pg_temp.ok(v_kmh between 11.5 and 12.5,
    format('시속 12km 로 만든 경로에서 %s km/h 를 읽는다', round(v_kmh::numeric, 2)));
  perform pg_temp.ok(v_glitch = 0, '정상 경로에는 튄 구간이 없다');

  select top_speed_kmh, glitch_ratio into v_kmh, v_glitch
    from economy.track_speed_stats(pg_temp.fx('track_car'));
  perform pg_temp.ok(v_glitch > 0.5, '차로 이동한 경로는 구간 대부분이 사람 속도를 넘는다');

  select top_speed_kmh into v_kmh from economy.track_speed_stats('');
  perform pg_temp.ok(v_kmh = 0, '경로가 없으면 속도는 0 이다');

  select top_speed_kmh into v_kmh from economy.track_speed_stats('깨진,값;37.5,127.0;,,');
  perform pg_temp.ok(v_kmh = 0, '깨진 경로에도 죽지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 적립 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare r record; v_start timestamptz := pg_temp.fx('run_start')::timestamptz;
begin
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.fx('track_ok'), 1200, 2, 'FIRE');

  -- 폰이 보낸 부스트(1200bps)와 파티 인원(2)은 믿지 않는다. 신은 신발도 파티
  -- 명단도 없으므로 2000보 × 0.01 = 20.
  perform pg_temp.ok(r.verdict = 'CLEAN', '정상 세션은 CLEAN');
  perform pg_temp.ok(r.points_awarded = 20,
    format('적립액을 서버가 계산한다 — 폰이 보낸 부스트·파티는 무시 (%s SUP)', r.points_awarded));
  perform pg_temp.ok(r.balance = 20, '잔고가 적립만큼 늘었다');

  perform pg_temp.ok(
    (select party_size from public.walk_sessions where id = r.session_id) = 1,
    '파티 인원은 서버 명단으로 센다 (명단이 없으면 1)');
  perform pg_temp.ok(
    (select top_speed_kmh from public.walk_sessions where id = r.session_id) between 11.5 and 12.5,
    '세션의 최고 속도는 경로에서 나온 값이다');
  perform pg_temp.ok(
    (select top_speed_kmh from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') between 11.5 and 12.5,
    '프로필의 최고 기록이 갱신된다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('run_start')::timestamptz;
begin
  -- 지하철에서 응답을 못 받고 앱이 다시 보낸 경우
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.fx('track_ok'), 1200, 2, 'FIRE');
  perform pg_temp.ok(r.points_awarded = 20, '같은 세션을 다시 보내도 결과가 같다');
  perform pg_temp.ok(
    (select coalesce(sum(amount), 0) from public.sup_ledger) = 20,
    '다시 보내도 원장에 두 번 쌓이지 않는다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('void_start')::timestamptz;
begin
  -- 1분에 500보 — 사람의 다리가 아니다
  select * into r from public.record_session(
    v_start, v_start + interval '120 seconds', 1000, 120, '', 0, 1, 'FIRE');
  perform pg_temp.ok(r.verdict = 'VOID', '케이던스가 사람 범위를 벗어나면 VOID');
  perform pg_temp.ok(r.points_awarded = 0, 'VOID 세션은 적립이 없다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('car_start')::timestamptz;
begin
  -- 차를 타고 이동한 경로. 케이던스는 정상 범위로 맞춰 둔다 —
  -- 속도 판정만으로 걸리는지 보려는 것이다.
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 1200, 601,
    pg_temp.fx('track_car'), 0, 1, 'WIND');
  perform pg_temp.ok(r.verdict = 'VOID', '차로 이동한 경로는 VOID');
  perform pg_temp.ok(r.points_awarded = 0, '타고 간 거리는 적립되지 않는다');
  perform pg_temp.ok(
    (select top_speed_kmh from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') between 11.5 and 12.5,
    'VOID 세션은 최고 속도 기록도 남기지 않는다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('cap_start')::timestamptz;
begin
  -- 하루 상한 48,000보. 이미 2,000보를 적립했으므로 46,000보만 인정되어야 한다.
  select * into r from public.record_session(
    v_start, v_start + interval '20000 seconds', 50000, 20000, '', 0, 1, '');
  perform pg_temp.ok(r.verdict = 'FLAGGED', '하루 상한을 넘으면 FLAGGED');
  perform pg_temp.ok(
    (select rewarded_steps from public.walk_sessions where id = r.session_id) = 46000,
    '상한까지만 인정된다 (46,000보)');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('last_start')::timestamptz;
begin
  select * into r from public.record_session(
    v_start, v_start + interval '1000 seconds', 3000, 1000, '', 0, 1, '');
  perform pg_temp.ok(
    (select rewarded_steps from public.walk_sessions where id = r.session_id) = 0,
    '상한을 채운 뒤에는 0보만 인정된다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 소비 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

do $$
declare v_left numeric;
begin
  select public.spend_sup('SPEND_BOOST', 10, '부스트') into v_left;
  perform pg_temp.ok(v_left = (20 + 460.00) - 10, format('차감 뒤 잔고 %s', v_left));
end $$;

call pg_temp.must_fail(
  $q$ select public.spend_sup('SPEND_MINT', 999999, '민팅') $q$,
  '잔고보다 많이 쓸 수 없다');
call pg_temp.must_fail(
  $q$ select public.spend_sup('EARN_WALK', 10, '적립인 척') $q$,
  '소비 함수로 적립할 수 없다');

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 원장·세션에 직접 쓰기 ────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.must_fail(
  $q$ insert into public.sup_ledger (user_id, kind, amount)
      values ('11111111-1111-1111-1111-111111111111', 'EARN_WALK', 100000) $q$,
  '원장에 직접 적립할 수 없다');
call pg_temp.must_fail(
  $q$ update public.sup_ledger set amount = 100000 $q$,
  '원장 금액을 고칠 수 없다');
call pg_temp.must_fail(
  $q$ delete from public.sup_ledger $q$,
  '원장을 지울 수 없다');
call pg_temp.must_fail(
  $q$ insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps)
      values ('11111111-1111-1111-1111-111111111111', now(), now(), 0, 99999) $q$,
  '세션을 직접 넣을 수 없다');

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 남의 것 ──────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
begin
  perform pg_temp.ok((select count(*) from public.sup_ledger) = 0, '남의 원장은 보이지 않는다');
  perform pg_temp.ok((select count(*) from public.walk_sessions) = 0, '남의 세션은 보이지 않는다');
  perform pg_temp.ok((select count(*) from public.profiles) = 3, '프로필은 서로 보인다');

  update public.profiles set display_name = '가로채기'
   where id = '11111111-1111-1111-1111-111111111111';
  perform pg_temp.ok(
    (select display_name from public.profiles
      where id = '11111111-1111-1111-1111-111111111111') = 'Ara Kim',
    '남의 프로필은 고쳐지지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 커뮤니티 ─────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare v_crew uuid;
begin
  -- 표에 직접 쓰기는 막혀 있다(0022). 앱과 같은 함수로 만든다.
  v_crew := public.crew_create('한강 러너스', 'HR', '매주 토요일', '서울 마포', 'OPEN');
  insert into fix (k, v) values ('crew', v_crew::text);

  perform pg_temp.ok(
    (select count(*) from public.crew_members
      where crew_id = v_crew and user_id = '11111111-1111-1111-1111-111111111111'
        and role = 'OWNER') = 1,
    '크루를 만들면 만든 사람이 바로 주인 멤버가 된다');
  perform pg_temp.ok(
    (select member_count from public.crew_feed where id = v_crew) = 1,
    '크루 목록에 인원이 세어진다');
end $$;

-- 정책은 예외를 던지지 않고 0행으로 막는다. 그래서 must_fail 이 아니라
-- 지운 뒤에 남아 있는지로 확인한다.
do $$
begin
  delete from public.crew_members
   where crew_id = pg_temp.fx('crew')::uuid
     and user_id = '11111111-1111-1111-1111-111111111111';
  perform pg_temp.ok(
    (select count(*) from public.crew_members
      where crew_id = pg_temp.fx('crew')::uuid) = 1,
    '주인은 크루에서 나갈 수 없다 (0행 삭제)');
end $$;

-- 크루 전용 글과 전체 글
do $$
declare v_post bigint; v_crew uuid := pg_temp.fx('crew')::uuid;
begin
  v_post := public.post_create('FREE', null, '오늘 날씨 좋네요', '한강 추천', null, null, null, null, null, null);
  insert into fix (k, v) values ('post_open', v_post::text);

  v_post := public.post_create('TIP', v_crew, '크루만 보는 글', '내일 6시', null, null, null, null, null, null);
  insert into fix (k, v) values ('post_crew', v_post::text);

  v_post := public.post_create('FLASH', null, '오늘 저녁 7시 번개', '', '여의도 한강공원', 5,
                               now() + interval '3 hours', 2, null, null);
  insert into fix (k, v) values ('post_flash', v_post::text);
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '전체 게시판 글은 누구나 본다');
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_crew')::bigint) = 0,
    '크루 글은 멤버가 아니면 안 보인다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.comments (post_id, parent_id, author_id, body)
             values (%s, null, '22222222-2222-2222-2222-222222222222', '안 보이는데 댓글') $q$,
         pg_temp.fx('post_crew')),
  '안 보이는 글에는 댓글을 못 단다');

call pg_temp.must_fail(
  format($q$ insert into public.posts (author_id, category, crew_id, title)
             values ('22222222-2222-2222-2222-222222222222', 'FREE', '%s', '남의 크루에 글쓰기') $q$,
         pg_temp.fx('crew')),
  '안 들어간 크루 게시판에는 글을 못 쓴다');

call pg_temp.must_fail(
  $q$ insert into public.posts (author_id, category, title)
      values ('11111111-1111-1111-1111-111111111111', 'FREE', '남의 이름으로') $q$,
  '남의 이름으로 글을 쓸 수 없다');

-- 가입하면 보인다
do $$
begin
  insert into public.crew_members (crew_id, user_id)
  values (pg_temp.fx('crew')::uuid, '22222222-2222-2222-2222-222222222222');
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_crew')::bigint) = 1,
    '크루에 들어가면 크루 글이 보인다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.crew_members (crew_id, user_id, role)
             values ('%s', '33333333-3333-3333-3333-333333333333', 'OWNER') $q$,
         pg_temp.fx('crew')),
  '남을 대신 가입시킬 수 없다');

-- 좋아요와 댓글
do $$
declare v_post bigint := pg_temp.fx('post_open')::bigint;
begin
  insert into public.post_likes (post_id, user_id)
  values (v_post, '22222222-2222-2222-2222-222222222222');
  insert into public.comments (post_id, author_id, body)
  values (v_post, '22222222-2222-2222-2222-222222222222', '저도 갈래요');

  perform pg_temp.ok(
    (select likes from public.post_feed where id = v_post) = 1, '좋아요가 세어진다');
  perform pg_temp.ok(
    (select liked from public.post_feed where id = v_post), '내가 누른 좋아요가 표시된다');
  perform pg_temp.ok(
    (select comment_count from public.post_feed where id = v_post) = 1, '댓글 수가 세어진다');
  perform pg_temp.ok(
    not (select mine from public.post_feed where id = v_post), '남의 글은 mine 이 아니다');
  perform pg_temp.ok(
    (select author from public.post_feed where id = v_post) = 'Ara Kim',
    '작성자 이름이 프로필에서 온다');
end $$;

-- 번개러닝 정원
do $$
declare v_post bigint := pg_temp.fx('post_flash')::bigint; v_n int;
begin
  -- 번개를 연 사람(1111)은 post_create 가 첫 참가자로 넣는다. 2222 가 둘째다.
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 2, '번개에 참가하면 인원이 센다');
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 2, '두 번 눌러도 한 명이다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.flash_participants (post_id, user_id)
             values (%s, '22222222-2222-2222-2222-222222222222') $q$,
         pg_temp.fx('post_flash')),
  '참가자 표에 직접 넣을 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
call pg_temp.must_fail(
  format($q$ select public.join_flash(%s) $q$, pg_temp.fx('post_flash')),
  '정원이 차면 더 들어갈 수 없다');

-- 차단
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '차단 전에는 글이 보인다');

  insert into public.user_blocks (blocker_id, blocked_id)
  values ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111111');

  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 0,
    '차단하면 그 사람 글이 내 화면에서 사라진다');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed
      where id = pg_temp.fx('post_open')::bigint) = 1,
    '남이 차단해도 내 화면은 그대로다');
  perform pg_temp.ok(
    (select count(*) from public.user_blocks) = 0, '남의 차단 목록은 보이지 않는다');
end $$;

-- 신고
do $$
begin
  insert into public.content_reports (reporter_id, target_type, target_id, reason, note)
  values ('22222222-2222-2222-2222-222222222222', 'POST',
          pg_temp.fx('post_open'), 'SPAM', '광고입니다');
  perform pg_temp.ok((select count(*) from public.content_reports) = 1, '신고를 넣을 수 있다');
end $$;

call pg_temp.must_fail(
  $q$ update public.content_reports set status = 'DISMISSED' $q$,
  '신고 처리 상태를 앱이 바꿀 수 없다');

-- 코스 — 앱은 표에 직접 쓰지 못한다(course_share 로만). 검사용 줄은 관리자로 넣는다.
call pg_temp.login('11111111-1111-1111-1111-111111111111');
call pg_temp.must_fail(
  $q$ insert into public.courses (owner_id, name, distance_km, track, shared)
      values ('11111111-1111-1111-1111-111111111111', 'x', 'NaN', '1,1', true) $q$,
  '코스 표에 직접 넣을 수 없다 (거리 · 개수 검사를 건너뛰지 못하게)');
reset role;
insert into public.courses (owner_id, name, area, distance_km, track, shared)
values ('11111111-1111-1111-1111-111111111111', '한강 5km', '서울 마포', 5.0,
        '37.5,127.0;37.51,127.0', true);
insert into public.courses (owner_id, name, area, distance_km, shared)
values ('11111111-1111-1111-1111-111111111111', '혼자 보는 코스', '서울', 3.0, false);
set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform pg_temp.ok((select count(*) from public.course_feed) = 2, '내 코스는 다 보인다');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok((select count(*) from public.course_feed) = 1,
    '공유하지 않은 코스는 남에게 안 보인다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 랭킹 ─────────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.must_fail(
  $q$ select count(*) from public.runner_stats $q$,
  '러너 합계 뷰를 앱이 직접 읽을 수 없다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare r record;
begin
  select * into r from public.leaderboard('TOP_SPEED', 20) where is_me;
  perform pg_temp.ok(r.rank = 1, '뛴 사람이 나뿐이면 1등이다');
  perform pg_temp.ok(r.monogram = 'AK', '이름 약자가 나온다 (Ara Kim → AK)');
  perform pg_temp.ok(r.top_speed_kmh between 11.5 and 12.5, '순위표의 속도는 경로에서 나온 값이다');

  select * into r from public.leaderboard('TOTAL_SUP', 20) where is_me;
  -- 적립은 20 + 460.00, 쓴 10 은 순위에서 빼지 않는다
  perform pg_temp.ok(r.sup = 480,
    format('누적 적립으로 줄을 세운다 (쓴 돈은 빼지 않는다, %s)', r.sup));

  perform pg_temp.ok(
    (select count(*) from public.leaderboard('LONGEST_TIME', 20)) >= 1,
    '누적 시간 순위도 나온다');

  select * into r from public.leaderboard('TOP_SPEED', 20) where is_me;
  perform pg_temp.ok(r.total = 1, '전체 인원이 함께 온다 ("N명 중 몇 등"의 N)');
end $$;

-- ── 기간 ──
--
-- 준비물의 러닝은 어제 10시에 있었다. 주간·월간 창에는 확실히 들어가고,
-- 일간은 지금 몇 시냐에 따라 들쭉날쭉하므로 여기서 단정하지 않는다.
do $$
declare r record;
begin
  perform pg_temp.ok(
    public.rank_period_start('ALL') = '-infinity'::timestamptz,
    '전체기간의 시작은 -infinity 다 (어떤 시각과 비교해도 참)');
  perform pg_temp.ok(
    public.rank_period_start('WEEK') between now() - interval '7 days 1 minute'
                                        and now() - interval '6 days 23 hours',
    '주간은 최근 7일이다');
  perform pg_temp.ok(
    public.rank_period_start('없는기간') = '-infinity'::timestamptz,
    '모르는 기간은 전체기간으로 친다');

  select * into r from public.leaderboard('TOP_SPEED', 20, 'MONTH') where is_me;
  perform pg_temp.ok(r.rank = 1, '월간 순위에도 어제 뛴 기록이 들어간다');
  perform pg_temp.ok(r.top_speed_kmh between 11.5 and 12.5,
    '기간을 좁혀도 속도는 경로에서 나온 값이다');

  select * into r from public.leaderboard('TOTAL_SUP', 20, 'MONTH') where is_me;
  perform pg_temp.ok(r.sup > 0, '월간 적립 순위에도 내 줄이 있다');
end $$;

-- 300등도 자기 줄이 보여야 한다
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_start timestamptz := pg_temp.fx('base')::timestamptz + interval '8 hours';
begin
  perform public.record_session(v_start, v_start + interval '300 seconds', 300, 300, '', 0, 1, 'WATER');
  perform pg_temp.ok(
    (select count(*) from public.leaderboard('TOP_SPEED', 1) where is_me) = 1,
    '상위 1명만 받아도 내 줄은 함께 온다');
  perform pg_temp.ok(
    (select max(total) from public.leaderboard('TOP_SPEED', 1)) = 2,
    '한 줄만 받아도 전체 인원은 2명으로 나온다');
end $$;

-- 속성(종족)별 순위표는 없앴다 (0028)
do $$ begin
  perform pg_temp.ok(
    not exists (select 1 from pg_proc p join pg_namespace n on n.oid = p.pronamespace
                 where n.nspname = 'public' and p.proname = 'faction_leaderboard'),
    '속성별 순위 함수가 남아 있지 않다');
end $$;


-- ════════════════════════════════════════════════════════════════
--  NFT 마켓
-- ════════════════════════════════════════════════════════════════
--
-- 돈과 소유권이 함께 움직이는 곳이라 한 줄씩 두드려 본다. "아마 맞을
-- 것이다"로 넘어가면, 틀렸을 때 나타나는 곳은 남의 잔고다.

reset role;

-- 남의 잔고도 봐야 한다 (원장은 제 것만 보이게 막혀 있다).
-- 검사 안에서만 쓰는 뒷문이고, Supabase 에는 올라가지 않는다.
-- 신발의 주인 — 앱 권한으로는 주인 칸을 못 읽으므로(0031) 검사용으로 관리자 권한에서 읽는다
create or replace function pg_temp.owner_of(p_id bigint) returns uuid
language sql stable security definer as $$ select owner_id from public.market_sneakers where id = p_id $$;

create or replace function pg_temp.bal(p_user uuid) returns numeric
language sql security definer as $$
  select coalesce(sum(amount), 0)::numeric(20,4) from public.sup_ledger where user_id = p_user
$$;

-- economy 스키마는 앱 역할에 열려 있지 않다 (열 이유가 없다). 검사에서만 읽는다.
create or replace function pg_temp.cap() returns int
language sql security definer as $$ select economy.market_import_cap() $$;

-- 거래는 서버가 만든 신발만 된다(0023 — 폰이 정한 등급·레벨로 SUP 를 받아 가지 못하게).
-- 여기서는 폰에서 올린 신발을 서버 신발로 바꿔 거래 규칙을 검사한다.
create or replace function pg_temp.server_shoe(p_id bigint) returns bigint
language sql security definer as $$
  update public.market_sneakers set origin = 'PAID_DRAW' where id = p_id returning id
$$;

-- 살 돈을 쥐여 준다. 실제로는 뛰어야 생기지만 여기서는 원장에 바로 적는다.
insert into public.sup_ledger (user_id, kind, amount, description) values
  ('22222222-2222-2222-2222-222222222222', 'EARN_WALK', 10000, '검사용'),
  ('33333333-3333-3333-3333-333333333333', 'EARN_WALK', 10000, '검사용');

set role authenticated;

-- ── 등록 ──
call pg_temp.login(null);
call pg_temp.must_fail(
  $q$ select public.market_import(1, 'FIRE', 'COMMON', 0, 1, 1, 1, 100) $q$,
  '로그인 없이는 등록할 수 없다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_a bigint; v_b bigint;
begin
  v_a := public.market_import(1, 'FIRE', 'RARE', 0, 7, 1.2, 1.1, 100);
  perform pg_temp.ok(v_a is not null, '폰의 신발을 거래소에 올린다');

  v_b := public.market_import(1, 'FIRE', 'RARE', 0, 9, 1.2, 1.1, 90);
  perform pg_temp.ok(v_a = v_b, '같은 신발을 다시 올려도 늘어나지 않는다');
  perform pg_temp.ok(
    (select level from public.market_sneakers where id = v_a) = 9,
    '다시 올리면 레벨은 최신으로 갱신된다');
  perform pg_temp.ok(
    (select count(*) from public.market_sneakers) = 1,
    '장부에는 한 켤레만 있다');
  perform pg_temp.ok(
    (select mint_number from public.market_sneakers where id = v_a) > 0,
    '거래소 전체에서 유일한 민팅 번호가 붙는다');

  insert into fix values ('sn1', v_a::text);
end $$;

call pg_temp.must_fail(
  format($q$ select public.market_list(%s, 100) $q$, pg_temp.fx('sn1')),
  '폰에서 올린 예전 신발은 팔 수 없다 (등급·레벨을 폰이 정했다)');
do $$ begin perform pg_temp.server_shoe(pg_temp.fx('sn1')::bigint); end $$;

call pg_temp.must_fail(
  $q$ insert into public.market_sneakers (owner_id, faction, rarity, variant, level, durability)
      values ('11111111-1111-1111-1111-111111111111', 'FIRE', 'LEGENDARY', 0, 30, 100) $q$,
  '장부에 직접 신발을 적을 수는 없다');

call pg_temp.must_fail(
  $q$ insert into public.market_trades (faction, rarity, variant, level, price, kind)
      values ('FIRE', 'LEGENDARY', 0, 30, 999999, 'BY_ASK') $q$,
  '체결 내역을 직접 지어낼 수 없다');

-- ── 매물 ──
do $$
declare v_listing bigint; v_sn bigint := pg_temp.fx('sn1')::bigint;
begin
  call pg_temp.must_fail(
    format($q$ select public.market_list(%s, 0.5) $q$, v_sn),
    '너무 싼 값은 걸리지 않는다');

  v_listing := public.market_list(v_sn, 500);
  perform pg_temp.ok(v_listing is not null, '내 스니커즈를 판다고 내놓는다');
  perform pg_temp.ok(
    (select status from public.market_sneakers where id = v_sn) = 'LISTED',
    '내놓은 신발은 판매 중으로 바뀐다');
  perform pg_temp.ok(
    (select count(*) from public.market_asks where sneaker_id = v_sn) = 1,
    '매물 장부에 한 줄이 선다');
  perform pg_temp.ok(
    (select level from public.market_asks where sneaker_id = v_sn) = 9,
    '매물 줄에 레벨이 함께 온다 — 같은 모델도 켤레마다 값이 다르므로');
  perform pg_temp.ok(
    (select ask from public.market_quotes where rarity = 'RARE') = 500,
    '시세판의 즉시 구매가가 최저 매물 값이다');

  call pg_temp.must_fail(
    format($q$ select public.market_list(%s, 600) $q$, v_sn),
    '한 켤레를 두 곳에 걸 수는 없다');

  insert into fix values ('listing1', v_listing::text);
end $$;

call pg_temp.must_fail(
  $q$ select public.market_buy_now(pg_temp.fx('listing1')::bigint) $q$,
  '내 매물은 내가 살 수 없다 (자전거래)');

-- ── 즉시 구매 ──
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare
  v_seller uuid := '11111111-1111-1111-1111-111111111111';
  v_buyer  uuid := '22222222-2222-2222-2222-222222222222';
  v_sn bigint := pg_temp.fx('sn1')::bigint;
  v_before numeric := pg_temp.bal(v_buyer);
  v_before_seller numeric := pg_temp.bal(v_seller);
  v_trade bigint;
begin
  v_trade := public.market_buy_now(pg_temp.fx('listing1')::bigint);
  perform pg_temp.ok(v_trade is not null, '매물을 산다');
  perform pg_temp.ok(
    exists (select 1 from public.my_sneakers() where id = v_sn),
    '신발의 주인이 바뀐다 (산 사람의 내 신발에 들어온다)');
  perform pg_temp.ok(
    (select status from public.market_sneakers where id = v_sn) = 'OWNED',
    '팔린 신발은 판매 중에서 풀린다');
  perform pg_temp.ok(pg_temp.bal(v_buyer) = v_before - 500, '산 사람 잔고에서 값이 빠진다');
  -- 수수료 2.5% = 12.5
  -- 500 에서 수수료 2.5%(12.5)를 뗀 487.5
  perform pg_temp.ok(pg_temp.bal(v_seller) = v_before_seller + 487.5,
    '판 사람은 수수료를 뗀 만큼 받는다');
  perform pg_temp.ok(
    (select count(*) from public.market_asks where sneaker_id = v_sn) = 0,
    '팔린 매물은 장부에서 사라진다');
  perform pg_temp.ok(
    (select price from public.market_trades where id = v_trade) = 500,
    '체결 내역에 값이 남는다');
  perform pg_temp.ok(
    (select last_price from public.market_quotes where rarity = 'RARE') = 500,
    '시세판에 최근 체결가가 뜬다');
end $$;

call pg_temp.must_fail(
  $q$ select public.market_buy_now(pg_temp.fx('listing1')::bigint) $q$,
  '이미 팔린 매물은 두 번 팔리지 않는다');

-- ── 구매 입찰과 즉시 판매 ──
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare
  v_me uuid := '33333333-3333-3333-3333-333333333333';
  v_before numeric := pg_temp.bal(v_me);
  v_bid bigint;
begin
  call pg_temp.must_fail(
    $q$ select public.market_bid('FIRE', 'RARE', 0, 1, 999999) $q$,
    '잔고보다 큰 값은 걸 수 없다');

  v_bid := public.market_bid('FIRE', 'RARE', 0, 5, 300);
  perform pg_temp.ok(v_bid is not null, '모델에 구매 입찰을 건다');
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before - 300,
    '건 값은 잠긴다 — 같은 돈으로 여러 곳에 걸 수 없게');
  perform pg_temp.ok(
    (select bid from public.market_quotes where rarity = 'RARE') = 300,
    '시세판의 즉시 판매가가 최고 입찰가다');
  insert into fix values ('bid1', v_bid::text);
end $$;

-- 레벨이 모자란 신발은 그 입찰에 팔 수 없다
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_low bigint;
begin
  v_low := pg_temp.server_shoe(public.market_import(2, 'FIRE', 'RARE', 0, 3, 1, 1, 100));
  insert into fix values ('sn_low', v_low::text);
end $$;

call pg_temp.must_fail(
  $q$ select public.market_sell_now(pg_temp.fx('sn_low')::bigint, pg_temp.fx('bid1')::bigint) $q$,
  '최소 레벨에 못 미치면 그 입찰에 팔 수 없다');

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare
  v_seller uuid := '22222222-2222-2222-2222-222222222222';
  v_buyer  uuid := '33333333-3333-3333-3333-333333333333';
  v_sn bigint := pg_temp.fx('sn1')::bigint;
  v_before_seller numeric := pg_temp.bal(v_seller);
  v_before_buyer  numeric := pg_temp.bal(v_buyer);
begin
  perform public.market_sell_now(v_sn, pg_temp.fx('bid1')::bigint);
  perform pg_temp.ok(
    pg_temp.owner_of(v_sn) = v_buyer,
    '즉시 판매로 주인이 바뀐다');
  -- 300 - 2.5% = 292.5
  perform pg_temp.ok(pg_temp.bal(v_seller) = v_before_seller + 292.5,
    '판 사람은 입찰가에서 수수료를 뗀 만큼 받는다');
  perform pg_temp.ok(pg_temp.bal(v_buyer) = v_before_buyer,
    '산 사람 잔고는 그대로다 — 입찰할 때 이미 잠갔으므로 두 번 내지 않는다');
  perform pg_temp.ok(
    (select count(*) from public.market_bid_book) = 0,
    '체결된 입찰은 장부에서 사라진다');
end $$;

-- ── 입찰 취소 ──
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare
  v_me uuid := '33333333-3333-3333-3333-333333333333';
  v_before numeric := pg_temp.bal(v_me);
  v_bid bigint;
begin
  v_bid := public.market_bid('WATER', 'EPIC', 1, 1, 200);
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before - 200, '입찰하면 잠긴다');
  perform public.market_cancel_bid(v_bid);
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before, '입찰을 거두면 잠긴 SUP 가 풀린다');
  perform public.market_cancel_bid(v_bid);
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before, '두 번 거둬도 두 번 풀리지 않는다');
end $$;

-- ── 교차 체결 ──
--
-- 값은 먼저 걸려 있던 쪽(메이커)의 값으로 정해진다. 나중에 들어온 쪽이
-- 값을 밀어 올리거나 내리지 못하게 하는 거래소의 기본 규칙이다.
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_bid bigint;
begin
  v_bid := public.market_bid('FIRE', 'RARE', 0, 1, 400);
  insert into fix values ('bid_cross', v_bid::text);
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare
  v_me uuid := '11111111-1111-1111-1111-111111111111';
  v_before numeric := pg_temp.bal(v_me);
  v_sn bigint := pg_temp.fx('sn_low')::bigint;
  v_listing bigint;
begin
  -- 250 에 내놨는데 400 을 부른 사람이 이미 있다 → 400 에 팔린다
  v_listing := public.market_list(v_sn, 250);
  perform pg_temp.ok(v_listing is null, '값이 맞으면 매물로 서지 않고 바로 팔린다');
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before + 390,
    '체결가는 먼저 걸려 있던 입찰의 값이다 (400 - 수수료 10)');
  perform pg_temp.ok(
    pg_temp.owner_of(v_sn)
      = '33333333-3333-3333-3333-333333333333',
    '교차 체결로도 주인이 바뀐다');
  perform pg_temp.ok(
    (select status from public.market_bids where id = pg_temp.fx('bid_cross')::bigint) = 'FILLED',
    '체결된 입찰은 닫힌다');
end $$;

-- 반대 방향 — 매물이 먼저 있고 그보다 높은 값으로 입찰하면 매물 값에 산다
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_sn bigint; v_listing bigint;
begin
  v_sn := pg_temp.server_shoe(public.market_import(9, 'WIND', 'EPIC', 2, 12, 1, 1, 100));
  v_listing := public.market_list(v_sn, 700);
  perform pg_temp.ok(v_listing is not null, '매물이 먼저 선다');
  insert into fix values ('sn_wind', v_sn::text);
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare
  v_me uuid := '22222222-2222-2222-2222-222222222222';
  v_before numeric := pg_temp.bal(v_me);
  v_bid bigint;
begin
  v_bid := public.market_bid('WIND', 'EPIC', 2, 1, 900);
  perform pg_temp.ok(v_bid is null, '살 수 있는 매물이 있으면 기다리지 않는다');
  perform pg_temp.ok(pg_temp.bal(v_me) = v_before - 700,
    '부른 값(900)이 아니라 매물 값(700)을 낸다');
  perform pg_temp.ok(
    pg_temp.owner_of(pg_temp.fx('sn_wind')::bigint) = v_me,
    '교차 체결로 신발을 받는다');
end $$;

-- ── 매물 거두기 ──
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare v_sn bigint := pg_temp.fx('sn_wind')::bigint; v_listing bigint;
begin
  v_listing := public.market_list(v_sn, 1200);
  perform public.market_cancel_listing(v_listing);
  perform pg_temp.ok(
    (select status from public.market_sneakers where id = v_sn) = 'OWNED',
    '매물을 거두면 신발이 판매 중에서 풀린다');
  perform pg_temp.ok(
    (select count(*) from public.market_asks where sneaker_id = v_sn) = 0,
    '거둔 매물은 장부에서 사라진다');
  insert into fix values ('listing_gone', v_listing::text);
end $$;

call pg_temp.login('33333333-3333-3333-3333-333333333333');
call pg_temp.must_fail(
  $q$ select public.market_cancel_listing(pg_temp.fx('listing_gone')::bigint) $q$,
  '남의 매물은 거둘 수 없다');

-- ── 내 것 보기 ──
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.market_my_sneakers()) = 1,
    '거래소가 아는 내 신발만 온다');
  perform pg_temp.ok(
    (select count(*) from public.market_my_trades(30)) >= 2,
    '내가 사고판 기록이 온다');
  perform pg_temp.ok(
    (select count(*) from public.market_my_trades(30) where not sold) >= 1,
    '산 것과 판 것이 구분된다');
end $$;

-- ── 올릴 수 있는 수 ──
--
-- 폰의 신발은 서버가 확인할 길이 없어 앱의 말을 믿는 수밖에 없다.
-- 그 믿음의 크기를 이 선으로 묶는다.
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare i int;
begin
  -- 이미 둘을 올렸다. 상한까지 채운다.
  for i in 3..pg_temp.cap() loop
    perform public.market_import(i, 'WATER', 'COMMON', 0, 1, 1, 1, 100);
  end loop;
  perform pg_temp.ok(
    (select count(*) from public.market_imports
      where user_id = '11111111-1111-1111-1111-111111111111') = pg_temp.cap(),
    '상한까지는 올라간다');
end $$;

call pg_temp.must_fail(
  $q$ select public.market_import(9999, 'WATER', 'COMMON', 0, 1, 1, 1, 100) $q$,
  '상한을 넘으면 더 올릴 수 없다');

call pg_temp.must_fail(
  $q$ select public.market_settle(1, '11111111-1111-1111-1111-111111111111',
        '22222222-2222-2222-2222-222222222222', 1, 'BY_ASK', true) $q$,
  '체결 함수는 바깥에서 부를 수 없다');


-- ════════════════════════════════════════════════════════════════
--  러닝 이벤트 · 러닝 건강 뉴스
-- ════════════════════════════════════════════════════════════════
--
-- 바깥 정보를 다루는 자리라, 틀렸을 때 나타나는 곳이 "헛걸음한 사용자"다.
-- 접수 상태와 날짜, 그리고 어떤 링크가 열리는지를 특히 두드려 본다.

reset role;

insert into public.app_admins (user_id, note)
values ('11111111-1111-1111-1111-111111111111', '검사용 운영자');

set role authenticated;

-- ── 주소 검사 ──
do $$
begin
  perform pg_temp.ok(public.is_web_url('https://seoul-marathon.com/main'), 'https 주소는 통과한다');
  perform pg_temp.ok(public.is_web_url(null), '주소가 없는 것은 막지 않는다 (모를 수 있다)');
  perform pg_temp.ok(not public.is_web_url('javascript:alert(1)'), 'javascript 스킴은 막힌다');
  perform pg_temp.ok(not public.is_web_url('data:text/html,hi'), 'data 스킴은 막힌다');
  perform pg_temp.ok(not public.is_web_url('file:///etc/passwd'), 'file 스킴은 막힌다');
  perform pg_temp.ok(not public.is_web_url('intent://x#Intent;end'), '앱 실행 스킴은 막힌다');
end $$;

-- ── 운영자만 고칠 수 있다 ──
call pg_temp.login('22222222-2222-2222-2222-222222222222');
call pg_temp.must_fail(
  $q$ select public.admin_upsert_event(null, '남이 만든 대회') $q$,
  '운영자가 아니면 대회를 등록할 수 없다');
call pg_temp.must_fail(
  $q$ insert into public.running_events (title) values ('직접 넣은 대회') $q$,
  '표에 직접 대회를 적을 수는 없다');
call pg_temp.must_fail(
  $q$ insert into public.news_articles (title, original_url)
      values ('직접 넣은 기사', 'https://example.com/x') $q$,
  '표에 직접 기사를 적을 수는 없다');
call pg_temp.must_fail(
  $q$ insert into public.app_admins (user_id) values (auth.uid()) $q$,
  '스스로를 운영자로 올릴 수 없다');

-- ── 대회 등록 ──
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_id uuid; v_past uuid; v_unknown uuid;
begin
  call pg_temp.must_fail(
    $q$ select public.admin_upsert_event(null, '나쁜 링크 대회',
        p_official_url => 'javascript:alert(1)') $q$,
    '이상한 주소가 든 대회는 등록되지 않는다');

  v_id := public.admin_upsert_event(
    null, '서울하프마라톤',
    p_organizer => '서울시', p_region => '서울', p_venue => '여의도',
    p_event_date => (current_date + 40),
    p_event_type => 'ROAD',
    p_registration_status => 'OPEN',
    p_registration_close_at => now() + interval '20 days',
    p_official_url => 'https://seoul-marathon.com/main',
    p_registration_url => 'https://seoul-marathon.com/apply',
    p_source_id => 'manual', p_edition_year => 2026,
    p_visibility => 'PUBLIC');
  perform pg_temp.ok(v_id is not null, '운영자는 대회를 등록한다');
  insert into fix values ('ev1', v_id::text);

  -- 한 대회에 여러 종목
  perform public.admin_add_discipline(v_id, '10K', '10K', 10000,
    now() + interval '20 days', 'OPEN', 30000);
  perform public.admin_add_discipline(v_id, '하프', 'HALF', 21097,
    now() + interval '10 days', 'OPEN', 50000);
  perform public.admin_add_discipline(v_id, '5km 건강달리기', 'LTE_5K', 5000,
    null, 'UNKNOWN', null);

  -- 지난 대회
  v_past := public.admin_upsert_event(
    null, '작년 가을 마라톤', p_region => '부산',
    p_event_date => (current_date - 30), p_visibility => 'PUBLIC',
    p_source_url => 'https://example.org/past');
  insert into fix values ('ev_past', v_past::text);

  -- 접수 정보를 모르는 대회
  v_unknown := public.admin_upsert_event(
    null, '접수 미상 대회', p_region => '경기',
    p_event_date => (current_date + 60), p_visibility => 'PUBLIC',
    p_source_url => 'https://example.org/unknown');
  insert into fix values ('ev_unknown', v_unknown::text);
end $$;

-- ── 목록 ──
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare r record; n int;
begin
  select count(*) into n from public.list_running_events();
  perform pg_temp.ok(n = 2, '지난 대회는 기본 목록에 없다 (' || n || '건)');

  perform pg_temp.ok(
    (select count(*) from public.list_running_events(p_include_past => true)) = 3,
    '지난 대회는 따로 물으면 나온다');

  -- 복수 종목 대회가 거리마다 나온다
  for r in select unnest(array['10K', 'HALF', 'LTE_5K']) as d loop
    perform pg_temp.ok(
      (select count(*) from public.list_running_events(p_distance => r.d)) = 1,
      r.d || ' 필터에 그 대회가 나온다');
  end loop;
  perform pg_temp.ok(
    (select count(*) from public.list_running_events(p_distance => 'FULL')) = 0,
    '없는 거리로 거르면 나오지 않는다');

  select * into r from public.list_running_events(p_query => '하프마라톤');
  perform pg_temp.ok(r.title = '서울하프마라톤', '이름으로 찾는다');
  perform pg_temp.ok(r.event_date = current_date + 40, '개최일이 그대로다');
  perform pg_temp.ok(r.registration_close_at::date = (now() + interval '20 days')::date,
    '접수 마감일이 개최일과 섞이지 않는다');
  perform pg_temp.ok(not r.has_start_time,
    '출발 시각을 모르면 시각이 없다고 알려 준다 (00:00 으로 지어내지 않는다)');
  perform pg_temp.ok(r.destination_type = 'REGISTRATION' and r.target_url like '%/apply',
    '접수 링크가 있으면 접수 사이트로 보낸다');
  perform pg_temp.ok(r.total_count = 1,
    '전체 건수는 거른 뒤의 수다 — 쪽나눔이 이 수를 보고 다음 쪽을 부른다');
  perform pg_temp.ok(
    (select max(total_count) from public.list_running_events()) = 2,
    '거르지 않으면 전체 건수가 다 센다');

  select * into r from public.list_running_events(p_region => '경기');
  perform pg_temp.ok(r.registration_status = 'UNKNOWN', '접수 정보 미상은 미상으로 남는다');
  perform pg_temp.ok(r.destination_type = 'SOURCE_ONLY',
    '공식 링크를 모르면 출처에서 확인으로 보낸다');

  perform pg_temp.ok(
    (select count(*) from public.list_running_events(p_status => 'OPEN')) = 1,
    '접수 중으로 거르면 미상인 대회는 끼지 않는다');

  perform pg_temp.ok(
    (select count(*) from public.event_disciplines_of(pg_temp.fx('ev1')::uuid)) = 3,
    '종목 세 개가 함께 온다');
end $$;

-- ── 취소·연기 ──
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform public.admin_set_event_state(pg_temp.fx('ev1')::uuid, p_cancelled => 'POSTPONED');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare first_row record;
begin
  select * into first_row from public.list_running_events() limit 1;
  perform pg_temp.ok(first_row.cancelled_or_postponed = 'NONE',
    '연기된 대회는 뒤로 밀린다 — 달릴 수 있는 대회가 먼저다');
  perform pg_temp.ok(
    (select cancelled_or_postponed from public.list_running_events()
      where id = pg_temp.fx('ev1')::uuid) = 'POSTPONED',
    '연기 표시는 목록에 그대로 실린다');
end $$;

-- ── 숨김 ──
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform public.admin_set_event_state(pg_temp.fx('ev_unknown')::uuid, p_visibility => 'HIDDEN');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.list_running_events()) = 1,
    '숨긴 대회는 목록에서 빠진다');
  perform pg_temp.ok(
    (select count(*) from public.get_running_event(pg_temp.fx('ev_unknown')::uuid)) = 0,
    '숨긴 대회는 상세로도 볼 수 없다');
end $$;

-- ── 관심 저장 ──
do $$
declare v_me uuid := '22222222-2222-2222-2222-222222222222';
begin
  perform public.save_event(pg_temp.fx('ev1')::uuid, true);
  perform public.save_event(pg_temp.fx('ev1')::uuid, true);
  perform pg_temp.ok(
    (select count(*) from public.saved_events where user_id = v_me) = 1,
    '두 번 저장해도 한 줄이다');
  perform pg_temp.ok(
    (select saved from public.list_running_events() where id = pg_temp.fx('ev1')::uuid),
    '목록에 저장 여부가 함께 온다');
  perform public.save_event(pg_temp.fx('ev1')::uuid, false);
  perform public.save_event(pg_temp.fx('ev1')::uuid, false);
  perform pg_temp.ok(
    (select count(*) from public.saved_events where user_id = v_me) = 0,
    '두 번 해제해도 탈이 없다');
end $$;

-- ── 뉴스 ──
reset role;

-- 수집기(service_role)가 하는 일을 흉내 낸다. 앱 역할로는 못 넣는다.
insert into public.news_articles
  (title, publisher_name, publisher_domain, original_url, published_at, fetched_at,
   description, category, source_id, rights_status, relevance_score)
values
  ('러닝 초보를 위한 무릎 부상 예방법', 'SBS', 'news.sbs.co.kr',
   'https://news.sbs.co.kr/news/endPage.do?news_id=1', now() - interval '2 days', now(),
   '전문가들은 준비운동을 강조했다', 'INJURY', 'sbs', 'DESCRIPTION_OK', 8.5),
  ('걷기만 해도 혈압이 내려간다', 'KBS', 'news.kbs.co.kr',
   'https://news.kbs.co.kr/news/view.do?ncd=2', now() - interval '1 day', now(),
   '하루 30분 걷기의 효과', 'WALK_JOG', 'kbs', 'LINK_ONLY', 6.0);

-- 이용 범위가 확인되지 않은 기사에 요약과 이미지를 붙여 둔다.
-- 표에 값이 있어도 내보내면 안 된다는 것을 확인하기 위해서다.
update public.news_articles
   set summary = '이 요약은 나가면 안 된다', summary_type = 'AI_SUMMARY',
       thumbnail_url = 'https://news.kbs.co.kr/img/2.jpg', image_usage_status = 'UNKNOWN'
 where publisher_domain = 'news.kbs.co.kr';

set role authenticated;
call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
declare r record;
begin
  perform pg_temp.ok((select count(*) from public.list_running_news()) = 2, '기사 두 건이 나온다');

  select * into r from public.list_running_news() where publisher_domain = 'news.kbs.co.kr';
  perform pg_temp.ok(r.summary is null,
    '본문 이용이 확인되지 않은 출처의 요약은 내보내지 않는다');
  perform pg_temp.ok(r.summary_type = 'NONE',
    '설명 이용도 확인되지 않았으면 표시 방식이 없음이 된다');
  perform pg_temp.ok(r.thumbnail_url is null,
    '이미지 사용 권한이 없으면 사진을 내보내지 않는다');
  perform pg_temp.ok(r.original_url like 'https://news.kbs.co.kr/%',
    '원문 주소와 출처명이 같은 곳을 가리킨다');

  select * into r from public.list_running_news() where publisher_domain = 'news.sbs.co.kr';
  perform pg_temp.ok(r.summary_type = 'SEARCH_DESCRIPTION',
    '검색 설명은 설명으로 표시된다 — AI 요약이라고 하지 않는다');
  perform pg_temp.ok(r.published_at < r.fetched_at,
    '기사 발행일과 우리가 주워 온 날이 구분된다');

  perform pg_temp.ok(
    (select count(*) from public.list_running_news(p_category => 'INJURY')) = 1,
    '주제로 거른다');
  perform pg_temp.ok(
    (select count(*) from public.list_running_news(p_publisher => 'news.sbs.co.kr')) = 1,
    '언론사로 거른다');
  perform pg_temp.ok(
    (select count(*) from public.list_running_news(p_query => '무릎')) = 1,
    '제목으로 찾는다');
end $$;

-- 중복은 수집기가 부딪히는 것이라 수집기 권한으로 확인한다.
-- 앱 권한으로 하면 RLS 에 먼저 막혀서 정작 유일 색인은 확인되지 않는다.
reset role;
call pg_temp.must_fail(
  $q$ insert into public.news_articles (title, original_url)
      values ('같은 주소 기사', 'https://news.sbs.co.kr/news/endPage.do?news_id=1') $q$,
  '같은 원문 주소는 두 번 들어가지 않는다');
set role authenticated;
call pg_temp.login('22222222-2222-2222-2222-222222222222');

-- ── 쪽나눔 한도 ──
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.list_running_news(p_limit => 100000)) <= 50,
    '앱이 큰 수를 보내도 서버가 잘라 준다');
  perform pg_temp.ok(
    (select count(*) from public.list_running_events(p_limit => -5)) >= 1,
    '앱이 음수를 보내도 빈 목록이 되지 않는다');
end $$;

-- ── 출처 ──
do $$
declare r record;
begin
  perform pg_temp.ok((select count(*) from public.running_sources_public) >= 8,
    '초기 출처 목록이 들어 있다');
  select * into r from public.running_sources_public where id = 'naver-news';
  perform pg_temp.ok(not r.enabled,
    '키가 없는 어댑터는 꺼진 채로 들어간다 — 켜져 있으면 연동이 끝난 것처럼 읽힌다');
  perform pg_temp.ok(not r.can_summarize, '요약 권한은 기본이 꺼짐이다');
  select * into r from public.running_sources_public where id = 'manual';
  perform pg_temp.ok(r.enabled, '운영자 등록 경로는 처음부터 열려 있다');
end $$;

call pg_temp.must_fail(
  $q$ select public.admin_set_source('naver-news', p_enabled => true) $q$,
  '운영자가 아니면 출처를 켤 수 없다');

reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 크루 가입 방식 · 가입 신청 ───────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare v_crew uuid;
begin
  v_crew := public.crew_create('새벽 6시 크루', 'D6', '출근 전 5km', '서울 성수', 'APPROVAL');
  insert into fix (k, v) values ('crew_appr', v_crew::text);

  perform pg_temp.ok(
    (select owned and joined and join_policy = 'APPROVAL' from public.crew_feed where id = v_crew),
    '승인제 크루를 만들면 만든 사람이 주인으로 들어가 있다');
  perform pg_temp.ok(
    (select roster[1] from public.crew_feed where id = v_crew) = 'Ara Kim',
    '크루 명단은 크루장이 맨 앞이다');
end $$;

call pg_temp.must_fail(
  $q$ select public.crew_create('이상한 크루', 'X', '', '', 'SOMETIMES') $q$,
  '없는 가입 방식으로는 크루를 못 만든다');

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
declare v_crew uuid := pg_temp.fx('crew_appr')::uuid;
begin
  perform pg_temp.ok(public.crew_join(v_crew) = 'REQUESTED', '승인제 크루에 가입하면 신청이 된다');
  perform pg_temp.ok(
    (select requested and not joined and pending_count = 0 from public.crew_feed where id = v_crew),
    '신청한 사람에게는 "신청함"으로 보이고, 기다리는 수는 보이지 않는다');
  perform pg_temp.ok(public.crew_join(v_crew) = 'REQUESTED', '신청을 두 번 눌러도 한 건이다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.crew_members (crew_id, user_id)
             values ('%s', '22222222-2222-2222-2222-222222222222') $q$,
         pg_temp.fx('crew_appr')),
  '승인제 크루에는 표를 직접 두드려 들어갈 수 없다');

call pg_temp.must_fail(
  format($q$ select * from public.crew_requests('%s') $q$, pg_temp.fx('crew_appr')),
  '크루장이 아니면 신청 목록을 못 본다');

call pg_temp.must_fail(
  format($q$ select public.crew_decide('%s', '22222222-2222-2222-2222-222222222222', true) $q$,
         pg_temp.fx('crew_appr')),
  '크루장이 아니면 스스로를 승인할 수 없다');

call pg_temp.must_fail(
  format($q$ select public.crew_set_join_policy('%s', 'OPEN') $q$, pg_temp.fx('crew_appr')),
  '크루장이 아니면 가입 방식을 못 바꾼다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare v_crew uuid := pg_temp.fx('crew_appr')::uuid;
begin
  perform pg_temp.ok(
    (select pending_count from public.crew_feed where id = v_crew) = 1,
    '크루장에게는 기다리는 신청 수가 보인다');
  perform pg_temp.ok(
    (select name from public.crew_requests(v_crew)) = 'Bo Lee',
    '크루장은 누가 신청했는지 본다');
  perform public.crew_decide(v_crew, '22222222-2222-2222-2222-222222222222', true);
  perform pg_temp.ok(
    (select member_count from public.crew_feed where id = v_crew) = 2
      and (select pending_count from public.crew_feed where id = v_crew) = 0,
    '승인하면 멤버가 되고 신청은 사라진다');
end $$;

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  perform pg_temp.ok(
    public.crew_join(pg_temp.fx('crew_appr')::uuid) = 'REQUESTED', '다른 사람도 신청한다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_crew uuid := pg_temp.fx('crew_appr')::uuid;
begin
  perform public.crew_decide(v_crew, '33333333-3333-3333-3333-333333333333', false);
  perform pg_temp.ok(
    (select member_count from public.crew_feed where id = v_crew) = 2
      and (select pending_count from public.crew_feed where id = v_crew) = 0,
    '거절하면 멤버가 되지 않고 신청만 사라진다');
end $$;

call pg_temp.must_fail(
  format($q$ select public.crew_decide('%s', '33333333-3333-3333-3333-333333333333', true) $q$,
         pg_temp.fx('crew_appr')),
  '없는 신청은 승인할 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  perform public.crew_join(pg_temp.fx('crew_appr')::uuid);
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_crew uuid := pg_temp.fx('crew_appr')::uuid;
begin
  perform pg_temp.ok(
    public.crew_set_join_policy(v_crew, 'OPEN') = 1,
    '자유 가입으로 열면 기다리던 신청을 모두 받아 준다');
  perform pg_temp.ok(
    (select member_count from public.crew_feed where id = v_crew) = 3
      and (select join_policy from public.crew_feed where id = v_crew) = 'OPEN',
    '받아 준 사람이 멤버로 세어진다');
end $$;

call pg_temp.must_fail(
  format($q$ select public.crew_leave('%s') $q$, pg_temp.fx('crew_appr')),
  '크루장은 크루를 나갈 수 없다');

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare v_crew uuid := pg_temp.fx('crew_appr')::uuid;
begin
  perform public.crew_leave(v_crew);
  perform pg_temp.ok(
    not (select joined from public.crew_feed where id = v_crew),
    '멤버는 크루를 나갈 수 있다');
  perform pg_temp.ok(public.crew_join(v_crew) = 'JOINED', '자유 가입 크루는 누르면 바로 들어간다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 신고 5건이면 숨김 ────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

reset role;
insert into auth.users (id, email) values
  ('44444444-4444-4444-4444-444444444444', 'd@test'),
  ('55555555-5555-5555-5555-555555555555', 'e@test');
set role authenticated;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_post bigint;
begin
  v_post := public.post_create('FREE', null, '신고받을 글', '...', null, null, null, null, null, null);
  insert into fix (k, v) values ('post_reported', v_post::text);
end $$;

-- 네 사람이 신고한다. 아직 보인다.
do $$
declare
  v_post text := pg_temp.fx('post_reported');
  v_user text;
begin
  foreach v_user in array array[
    '22222222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    '44444444-4444-4444-4444-444444444444',
    '55555555-5555-5555-5555-555555555555']
  loop
    perform set_config('request.jwt.claim.sub', v_user, false);
    insert into public.content_reports (reporter_id, target_type, target_id, reason)
    values (v_user::uuid, 'POST', v_post, 'SPAM');
  end loop;
  perform pg_temp.ok(
    (select count(*) from public.post_feed where id = v_post::bigint) = 1,
    '신고 4건까지는 그대로 보인다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
call pg_temp.must_fail(
  format($q$ insert into public.content_reports (reporter_id, target_type, target_id, reason)
             values ('22222222-2222-2222-2222-222222222222', 'POST', '%s', 'SPAM') $q$,
         pg_temp.fx('post_reported')),
  '남의 이름으로 신고할 수 없다');

do $$
begin
  insert into public.content_reports (reporter_id, target_type, target_id, reason)
  values ('11111111-1111-1111-1111-111111111111', 'POST', pg_temp.fx('post_reported'), 'OTHER');
  perform pg_temp.ok(
    (select count(*) from public.post_feed where id = pg_temp.fx('post_reported')::bigint) = 0,
    '신고가 5건이 되면 글이 사라진다');
end $$;

reset role;
update public.content_reports set status = 'DISMISSED'
 where target_type = 'POST' and target_id = pg_temp.fx('post_reported')
   and reporter_id = '55555555-5555-5555-5555-555555555555';
set role authenticated;
call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed where id = pg_temp.fx('post_reported')::bigint) = 1,
    '운영자가 신고를 기각해 5건 아래로 내려가면 다시 보인다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 게시판 함수 ──────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('33333333-3333-3333-3333-333333333333');

do $$
declare v_post bigint;
begin
  v_post := public.post_create('FLASH', null, '내일 아침 번개', '석촌호수 한 바퀴',
                               '석촌호수 동문', 5, now() + interval '1 day', 1, 37.5096, 127.1057);
  insert into fix (k, v) values ('fn_flash', v_post::text);
  perform pg_temp.ok(
    (select joined and joined_count = 1 and capacity = 2 from public.post_feed where id = v_post),
    '번개를 쓰면 쓴 사람이 첫 참가자이고, 정원은 최소 2명이다');
  perform pg_temp.ok(
    (select lat = 37.5096 and lng = 127.1057 from public.post_feed where id = v_post),
    '번개 모임 장소 좌표가 목록에 실린다');
  perform pg_temp.ok(
    (select count(*) = 1 and bool_and(is_host and is_me) from public.flash_roster where post_id = v_post),
    '참가자 명단에 주최자가 첫 줄로 있다');

  v_post := public.post_create('FREE', null, '러닝화 추천', '발볼 넓은 분들', '무시', 99, now(), 99, 37.5, 127.0);
  insert into fix (k, v) values ('fn_free', v_post::text);
  perform pg_temp.ok(
    (select place = '' and capacity = 0 and meet_at is null and lat is null from public.post_feed where id = v_post),
    '번개가 아닌 글에는 장소·정원·모임 시각이 붙지 않는다');
end $$;

call pg_temp.must_fail(
  $q$ select public.post_create('FLASH', null, '지난 번개', '', '', 0, now() - interval '1 hour', 4, null, null) $q$,
  '지난 시각으로는 번개를 열 수 없다');

call pg_temp.must_fail(
  format($q$ select public.post_create('FREE', '%s', '남의 크루', '', '', 0, null, 0, null, null) $q$,
         pg_temp.fx('crew')),
  '안 들어간 크루 게시판에는 함수로도 글을 못 쓴다');

call pg_temp.must_fail(
  $q$ select public.post_create('FLASH', null, '바다 위 번개', '', '', 5, now() + interval '1 day', 4, 123.0, 127.0) $q$,
  '있을 수 없는 좌표로는 번개를 열 수 없다');

call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
declare v_post bigint := pg_temp.fx('fn_free')::bigint; v_c bigint; v_r bigint;
begin
  perform pg_temp.ok(public.post_toggle_like(v_post), '좋아요를 누르면 눌린 상태가 돌아온다');
  perform pg_temp.ok((select likes from public.post_feed where id = v_post) = 1, '좋아요가 세어진다');
  perform pg_temp.ok(not public.post_toggle_like(v_post), '한 번 더 누르면 거둔다');
  perform pg_temp.ok((select likes from public.post_feed where id = v_post) = 0, '거두면 빠진다');

  v_c := public.comment_create(v_post, 0, '  저는 뉴발 추천요  ');
  v_r := public.comment_create(v_post, v_c, '저도요');
  insert into fix (k, v) values ('fn_comment', v_c::text);
  perform pg_temp.ok(
    (select body from public.comment_feed where id = v_c) = '저는 뉴발 추천요',
    '댓글 앞뒤 공백은 지운다');
  perform pg_temp.ok(
    (select parent_id from public.comment_feed where id = v_r) = v_c,
    '답글은 부모 댓글에 붙는다');
  perform pg_temp.ok(
    (select comment_count from public.post_feed where id = v_post) = 2,
    '댓글 수가 세어진다');
end $$;

call pg_temp.must_fail(
  format($q$ select public.comment_create(%s, %s, '엉뚱한 글의 답글') $q$,
         pg_temp.fx('fn_flash'), pg_temp.fx('fn_comment')),
  '다른 글의 댓글에는 답글을 달 수 없다');

call pg_temp.must_fail(
  format($q$ select public.post_delete(%s) $q$, pg_temp.fx('fn_free')),
  '남의 글은 지울 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
call pg_temp.must_fail(
  format($q$ select public.comment_delete(%s) $q$, pg_temp.fx('fn_comment')),
  '남의 댓글은 지울 수 없다');

-- 신고와 차단
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform public.content_report('POST', pg_temp.fx('fn_flash'), 'SPAM', '');
  perform public.content_report('POST', pg_temp.fx('fn_flash'), 'ABUSE', '');
  perform pg_temp.ok(
    (select count(*) from public.content_reports
      where target_type = 'POST' and target_id = pg_temp.fx('fn_flash')) = 1,
    '같은 글을 두 번 신고해도 한 건이다');

  perform public.user_block('33333333-3333-3333-3333-333333333333');
  perform pg_temp.ok(
    (select count(*) from public.post_feed where author_id = '33333333-3333-3333-3333-333333333333') = 0,
    '차단한 사람의 글은 내 화면에서 사라진다');
end $$;

call pg_temp.must_fail(
  $q$ select public.user_block('22222222-2222-2222-2222-222222222222') $q$,
  '나 자신은 차단할 수 없다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.post_feed where author_id = '33333333-3333-3333-3333-333333333333') = 2,
    '차단은 차단한 사람에게만 적용된다');
end $$;

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  perform public.post_delete(pg_temp.fx('fn_free')::bigint);
  perform pg_temp.ok(
    (select count(*) from public.comments where post_id = pg_temp.fx('fn_free')::bigint) = 0,
    '글을 지우면 달린 댓글도 함께 지워진다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 코스 공유 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_id bigint; v_again bigint;
begin
  v_id := public.course_share('망원 한강 루프', '서울 마포', 3.2, 20, '37.55,126.89;37.56,126.90');
  v_again := public.course_share('망원 한 바퀴', '서울 마포', 3.2, 20, '37.55,126.89;37.56,126.90');
  insert into fix (k, v) values ('course', v_id::text);
  perform pg_temp.ok(v_id = v_again, '같은 길을 다시 올리면 새 코스가 아니라 이름만 바뀐다');
  perform pg_temp.ok(
    (select name from public.course_feed where id = v_id) = '망원 한 바퀴',
    '올린 코스가 게시판에 보인다');
end $$;

call pg_temp.must_fail(
  $q$ select public.course_share('너무 짧은 코스', '', 0.05, 0, '37.55,126.89;37.55,126.89') $q$,
  '200m 도 안 되는 코스는 올릴 수 없다');

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
begin
  perform pg_temp.ok(public.course_toggle_like(pg_temp.fx('course')::bigint), '남의 코스에 좋아요를 누른다');
  perform pg_temp.ok(
    (select likes from public.course_feed where id = pg_temp.fx('course')::bigint) = 1,
    '코스 좋아요가 세어진다');
  perform public.course_unshare('37.55,126.89;37.56,126.90');
  perform pg_temp.ok(
    (select count(*) from public.course_feed where id = pg_temp.fx('course')::bigint) = 1,
    '남의 코스는 내릴 수 없다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform public.course_unshare('37.55,126.89;37.56,126.90');
  -- 게시판(앱은 shared=eq.true 로 읽는다)에서 사라진다
  perform pg_temp.ok(
    (select count(*) from public.course_feed
      where id = pg_temp.fx('course')::bigint and shared) = 0,
    '내 코스를 내리면 게시판에서 사라진다');
  -- 지우지 않고 내리기만 한다 — 다른 러너의 좋아요·기록이 남는다
  perform pg_temp.ok(
    (select likes from public.course_feed where id = pg_temp.fx('course')::bigint) = 1,
    '코스를 내려도 받은 좋아요는 남는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 파티런 로비 ──────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_party bigint;
begin
  v_party := public.party_open(pg_temp.fx('crew')::uuid, null);
  insert into fix (k, v) values ('party', v_party::text);
  perform pg_temp.ok(
    (public.party_state(v_party)->>'status') = 'LOBBY',
    '크루원이 로비를 열면 방이 생긴다');
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare v_party bigint; v_state json;
begin
  v_party := public.party_open(pg_temp.fx('crew')::uuid, null);
  perform pg_temp.ok(v_party = pg_temp.fx('party')::bigint, '같은 크루의 로비를 열면 같은 방에 들어간다');
  v_state := public.party_state(v_party);
  perform pg_temp.ok(json_array_length(v_state->'members') = 2, '방에 두 사람이 보인다');
  perform pg_temp.ok(
    (v_state->'members'->0->>'is_host')::boolean and (v_state->'members'->0->>'name') = 'Ara Kim',
    '방장이 명단 맨 앞이다');
  perform public.party_ready(v_party, true);
end $$;

call pg_temp.must_fail(
  format($q$ select public.party_start(%s) $q$, pg_temp.fx('party')),
  '방장이 아니면 출발시킬 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
call pg_temp.must_fail(
  format($q$ select public.party_open('%s', null) $q$, pg_temp.fx('crew')),
  '크루원이 아니면 로비에 들어갈 수 없다');
call pg_temp.must_fail(
  format($q$ select public.party_state(%s) $q$, pg_temp.fx('party')),
  '방에 없는 사람은 방 상태를 볼 수 없다');

call pg_temp.login('11111111-1111-1111-1111-111111111111');
call pg_temp.must_fail(
  format($q$ select public.party_start(%s) $q$, pg_temp.fx('party')),
  '방장이 준비하지 않으면 출발할 수 없다');

do $$
declare v_party bigint := pg_temp.fx('party')::bigint;
begin
  perform public.party_ready(v_party, true);
  perform public.party_start(v_party);
  perform pg_temp.ok(
    (public.party_state(v_party)->>'status') = 'COUNTDOWN',
    '출발하면 카운트다운에 들어간다');
end $$;

-- 카운트다운이 지난 것으로 만든다.
reset role;
update public.parties set starts_at = now() - interval '1 second' where id = pg_temp.fx('party')::bigint;
set role authenticated;
call pg_temp.login('22222222-2222-2222-2222-222222222222');

do $$
declare v_party bigint := pg_temp.fx('party')::bigint;
begin
  perform pg_temp.ok(
    (public.party_state(v_party)->>'status') = 'RUNNING',
    '카운트다운이 끝나면 다 같이 달리는 중이 된다');
  perform public.party_ping(v_party, 37.5301, 126.9340);
  perform pg_temp.ok(
    (public.party_state(v_party)->'members'->1->>'lat')::double precision = 37.5301,
    '달리는 동안의 위치가 방에 실린다');
  perform pg_temp.ok(
    public.party_open(pg_temp.fx('crew')::uuid, null) = v_party,
    '달리는 중인 방의 멤버는 로비를 다시 열어도 같은 방이다');
end $$;

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
begin
  -- 3번은 크루원이 아니지만 번개 참가자다(앞의 게시판 검사에서 연 번개)
  perform pg_temp.ok(
    public.party_open(null, pg_temp.fx('fn_flash')::bigint) > 0,
    '번개를 연 사람은 그 번개의 로비를 연다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
declare v_party bigint := pg_temp.fx('party')::bigint;
begin
  perform public.party_leave(v_party);
end $$;

call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$
declare v_party bigint := pg_temp.fx('party')::bigint;
begin
  perform pg_temp.ok(
    (public.party_state(v_party)->>'host_id') = '22222222-2222-2222-2222-222222222222',
    '방장이 나가면 남은 사람이 방장이 된다');
  perform public.party_leave(v_party);
end $$;

reset role;
do $$
begin
  perform pg_temp.ok(
    (select status from public.parties where id = pg_temp.fx('party')::bigint) = 'FINISHED',
    '아무도 안 남으면 방이 닫힌다');
end $$;
set role authenticated;

-- 로비에서 내보내기
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  insert into fix (k, v) values ('party2', public.party_open(pg_temp.fx('crew')::uuid, null)::text);
  perform pg_temp.ok(pg_temp.fx('party2')::bigint <> pg_temp.fx('party')::bigint, '닫힌 방 대신 새 방이 열린다');
end $$;
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$ begin perform public.party_open(pg_temp.fx('crew')::uuid, null); end $$;
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform public.party_kick(pg_temp.fx('party2')::bigint, '22222222-2222-2222-2222-222222222222');
  perform pg_temp.ok(
    json_array_length(public.party_state(pg_temp.fx('party2')::bigint)->'members') = 1,
    '방장은 로비에서 한 사람을 내보낼 수 있다');
end $$;

call pg_temp.must_fail(
  $q$ select * from public.parties $q$,
  '방 표는 앱이 직접 읽을 수 없다');

reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 푸시 토큰 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$ begin perform public.push_register('fcm-token-aaaaaaaaaaaaaaaaaaaa', 'ko'); end $$;
-- 같은 폰에서 다른 계정으로 로그인하면 토큰이 새 계정으로 옮겨 간다
call pg_temp.login('22222222-2222-2222-2222-222222222222');
do $$ begin perform public.push_register('fcm-token-aaaaaaaaaaaaaaaaaaaa', 'en'); end $$;

call pg_temp.must_fail(
  $q$ select * from public.push_tokens $q$,
  '앱은 토큰 표를 읽을 수 없다');
call pg_temp.must_fail(
  $q$ select public.push_register('short', 'ko') $q$,
  '짧은 토큰은 받지 않는다');
reset role;

do $$
begin
  perform pg_temp.ok(
    (select user_id = '22222222-2222-2222-2222-222222222222' and locale = 'en'
       from public.push_tokens where token = 'fcm-token-aaaaaaaaaaaaaaaaaaaa'),
    '한 폰의 토큰은 마지막으로 로그인한 사람의 것이다');
  perform pg_temp.ok(
    (select count(*) from public.push_tokens where token = 'fcm-token-aaaaaaaaaaaaaaaaaaaa') = 1,
    '토큰은 한 줄뿐이다');
end $$;

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$ begin perform public.push_unregister('fcm-token-aaaaaaaaaaaaaaaaaaaa'); end $$;
reset role;
do $$
begin
  perform pg_temp.ok(
    exists (select 1 from public.push_tokens where token = 'fcm-token-aaaaaaaaaaaaaaaaaaaa'),
    '남의 폰 토큰은 지울 수 없다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 도전 보상 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

-- 도전을 받을 사람: 4444 — 앞선 검사의 기록과 섞이지 않게 새 사람으로 한다.
insert into auth.users (id) values ('44444444-4444-4444-4444-444444444444') on conflict do nothing;

set role authenticated;
call pg_temp.login('44444444-4444-4444-4444-444444444444');

call pg_temp.must_fail(
  $q$ select public.event_claim('step_surge', 'Asia/Seoul') $q$,
  '걸음이 모자라면 주간 도전을 받을 수 없다');

do $$
declare v_today bigint := (now() at time zone 'Asia/Seoul')::date - date '1970-01-01';
begin
  -- 폰이 올린 하루 걸음은 누구나 적을 수 있다. 주간 도전에 세지 않는다.
  perform public.steps_sync(json_build_array(
    json_build_object('epoch_day', v_today, 'steps', 48000, 'goal', 8000),
    json_build_object('epoch_day', v_today - 1, 'steps', 48000, 'goal', 8000)
  ));
  perform pg_temp.ok(
    public.event_progress('step_surge', 'Asia/Seoul') = 0,
    '폰이 올린 하루 걸음은 주간 도전에 세지 않는다');
end $$;

reset role;
-- 경로가 받쳐 준 러닝 걸음 80,000보 — 하루 상한(48,000) 안으로 이틀에 나눠 (한국 시간 오늘 · 어제)
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verified_steps,
                                  backed_steps, gps_backed, rewarded_steps, verdict)
select '44444444-4444-4444-4444-444444444444',
       economy.game_day_start(economy.game_day(now()) - d) + interval '1 minute',
       economy.game_day_start(economy.game_day(now()) - d) + interval '2 minutes', 60, 40000, 40000,
       40000, true, 0, 'CLEAN'
  from generate_series(0, 1) d;
set role authenticated;

do $$
begin
  -- 주간 도전은 이번 ISO 주(한국 시각)만 센다. 한국 시각 월요일이면 어제(일요일)는 지난주다.
  if extract(isodow from (now() at time zone 'Asia/Seoul')::date) = 1 then
    perform pg_temp.ok(
      public.event_progress('step_surge', 'Asia/Seoul') = 40000,
      '주간 도전은 이번 주 걸음만 센다 (월요일 — 어제는 지난주)');
  else
    perform pg_temp.ok(
      public.event_progress('step_surge', 'Asia/Seoul') = 80000,
      '주간 도전은 서버가 확인한 러닝 걸음으로 센다');
    perform pg_temp.ok(
      public.event_claim('step_surge', 'Asia/Seoul') = 250,
      '목표를 채우면 주간 도전 보상이 나온다');
  end if;
end $$;

call pg_temp.must_fail(
  $q$ select public.event_claim('step_surge', 'Asia/Seoul') $q$,
  '같은 주에 두 번 받을 수 없다');

call pg_temp.must_fail(
  $q$ select public.event_claim('night_quest', 'Asia/Seoul') $q$,
  '밤에 뛴 거리가 모자라면 나이트 러너를 받을 수 없다');

call pg_temp.must_fail(
  $q$ insert into public.event_claims (user_id, event_id, period, amount)
      values ('44444444-4444-4444-4444-444444444444', 'night_quest', 'once', 300) $q$,
  '앱은 받은 기록을 직접 적을 수 없다');

call pg_temp.must_fail(
  $q$ select public.event_claim('free_money', 'Asia/Seoul') $q$,
  '없는 도전은 받을 수 없다');

reset role;

-- 밤 9시(서울)에 21km 뛴 기록 하나와, 판정에서 걸린 30km 기록 하나
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict)
values
  ('44444444-4444-4444-4444-444444444444',
   (date '2026-09-01' + time '21:00') at time zone 'Asia/Seoul',
   (date '2026-09-01' + time '23:00') at time zone 'Asia/Seoul', 7200, 25000, 21000, 'CLEAN'),
  ('44444444-4444-4444-4444-444444444444',
   (date '2026-09-02' + time '21:00') at time zone 'Asia/Seoul',
   (date '2026-09-02' + time '23:00') at time zone 'Asia/Seoul', 7200, 30000, 30000, 'FLAGGED');

set role authenticated;
call pg_temp.login('44444444-4444-4444-4444-444444444444');
do $$
begin
  perform pg_temp.ok(
    round(public.event_progress('night_quest', 'Asia/Seoul')::numeric, 1) = 21.0,
    '나이트 러너는 판정에서 걸린 세션을 빼고 센다');
  perform pg_temp.ok(
    public.event_claim('night_quest', 'Asia/Seoul') = 300,
    '밤 20km 를 채우면 나이트 러너 보상이 나온다');
  -- 한국 시각 월요일에는 주간 도전을 받지 못했다(위) — 나이트 러너 하나만 있다
  perform pg_temp.ok(
    (select count(*) from public.event_claims)
      = case when extract(isodow from (now() at time zone 'Asia/Seoul')::date) = 1 then 1 else 2 end,
    '받은 기록은 본인이 읽을 수 있다');
end $$;
reset role;

do $$
begin
  perform pg_temp.ok(
    (select sum(amount) from public.sup_ledger
      where user_id = '44444444-4444-4444-4444-444444444444' and kind = 'EARN_EVENT')
      = case when extract(isodow from (now() at time zone 'Asia/Seoul')::date) = 1 then 300 else 550 end,
    '받은 보상이 서버 원장에 EARN_EVENT 로 적힌다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 크루 순위 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict)
values
  ('11111111-1111-1111-1111-111111111111', timestamptz '2026-09-20 07:00+09', timestamptz '2026-09-20 07:40+09', 2400, 6000, 5000, 'CLEAN'),
  ('11111111-1111-1111-1111-111111111111', timestamptz '2026-09-21 07:00+09', timestamptz '2026-09-21 07:40+09', 2400, 6000, 9000, 'VOID');

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  perform pg_temp.ok(
    public.session_tag_crew(timestamptz '2026-09-20 07:00+09', pg_temp.fx('crew')::uuid),
    '크루원은 자기 러닝에 크루를 적을 수 있다');
  perform public.session_tag_crew(timestamptz '2026-09-21 07:00+09', pg_temp.fx('crew')::uuid);
  perform pg_temp.ok(
    (select km = 5 and runs = 1 and runners = 1 from public.crew_leaderboard('ALL')
      where crew_id = pg_temp.fx('crew')::uuid),
    '크루 순위는 무효 판정 러닝을 빼고 센다');
  perform pg_temp.ok(
    (select count(*) from public.crew_leaderboard('ALL')) >= 1,
    '아직 안 달린 크루도 순위표에 나온다');
end $$;

call pg_temp.login('44444444-4444-4444-4444-444444444444');
call pg_temp.must_fail(
  format($q$ select public.session_tag_crew(timestamptz '2026-09-01 21:00+09', '%s') $q$, pg_temp.fx('crew')),
  '크루원이 아니면 러닝에 그 크루를 적을 수 없다');
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 알림 설정 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('44444444-4444-4444-4444-444444444444');
do $$
begin
  perform public.notify_prefs_set(true, false, false, true);
  perform public.notify_prefs_set(true, true, false, true);
  perform pg_temp.ok(
    (select not party_invite and goal_reminder from public.notify_prefs),
    '알림 설정은 한 사람에 한 줄로, 마지막 값이 남는다');
end $$;
call pg_temp.must_fail(
  $q$ update public.notify_prefs set push = false $q$,
  '앱은 알림 설정 표를 직접 고칠 수 없다');
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 푸시 보낼 목록 ───────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

set role authenticated;
call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$ begin perform public.push_register('fcm-token-3333333333333333333333', 'ko'); end $$;
reset role;

do $$
declare v_post bigint; v_c bigint; v_before int;
begin
  delete from public.push_outbox;
  insert into public.posts (author_id, category, title, body)
  values ('33333333-3333-3333-3333-333333333333', 'FREE', '푸시 검사 글', '')
  returning id into v_post;

  -- 2222 가 3333 의 글에 댓글 → 3333 에게
  insert into public.comments (post_id, author_id, body)
  values (v_post, '22222222-2222-2222-2222-222222222222', '좋은 글이에요') returning id into v_c;
  perform pg_temp.ok(
    (select count(*) = 1 and bool_and(kind = 'COMMENT' and args->>'title' <> '')
       from public.push_outbox where user_id = '33333333-3333-3333-3333-333333333333'),
    '댓글이 달리면 글쓴이에게 보낼 푸시가 생긴다');

  -- 3333 이 자기 글에 댓글 → 아무에게도 안 감. 2222 댓글에 답글 → 2222 에게
  insert into public.comments (post_id, author_id, body)
  values (v_post, '33333333-3333-3333-3333-333333333333', '감사합니다');
  insert into public.comments (post_id, parent_id, author_id, body)
  values (v_post, v_c, '33333333-3333-3333-3333-333333333333', '저도요');
  perform pg_temp.ok(
    (select count(*) from public.push_outbox where user_id = '33333333-3333-3333-3333-333333333333') = 1,
    '자기 글에 단 댓글은 자기에게 알리지 않는다');
  perform pg_temp.ok(
    (select count(*) from public.push_outbox
      where user_id = '22222222-2222-2222-2222-222222222222' and kind = 'REPLY') = 1,
    '답글이 달리면 댓글 단 사람에게 보낼 푸시가 생긴다');

  -- 알림을 끈 사람에게는 쌓지 않는다
  insert into public.notify_prefs (user_id, push) values ('33333333-3333-3333-3333-333333333333', false)
    on conflict (user_id) do update set push = false;
  select count(*) into v_before from public.push_outbox;
  insert into public.comments (post_id, author_id, body)
  values (v_post, '22222222-2222-2222-2222-222222222222', '한 번 더');
  perform pg_temp.ok(
    (select count(*) from public.push_outbox) = v_before,
    '푸시를 끈 사람에게는 보낼 푸시가 쌓이지 않는다');

  -- 폰이 없는 사람(4444 는 토큰이 없다)에게도 쌓지 않는다
  perform pg_temp.ok(
    not exists (select 1 from public.push_outbox where user_id = '44444444-4444-4444-4444-444444444444'),
    '받을 폰이 없는 사람에게는 쌓지 않는다');
end $$;

do $$
declare r record; v_n int := 0;
begin
  for r in select * from public.push_claim_batch(10) loop
    v_n := v_n + 1;
    perform pg_temp.ok(jsonb_array_length(r.tokens) >= 1, '가져간 줄에는 받을 폰이 함께 온다');
    perform public.push_mark(r.id, true, '');
  end loop;
  perform pg_temp.ok(v_n = 2, '쌓인 푸시를 한 번에 가져간다');
  perform pg_temp.ok(
    (select count(*) from public.push_claim_batch(10)) = 0,
    '보냈다고 적은 줄은 다시 가져가지 않는다');
end $$;

set role authenticated;
call pg_temp.login('22222222-2222-2222-2222-222222222222');
call pg_temp.must_fail($q$ select * from public.push_outbox $q$, '앱은 보낼 푸시 목록을 볼 수 없다');
call pg_temp.must_fail($q$ select * from public.push_claim_batch(10) $q$, '앱은 보낼 푸시를 가져갈 수 없다');
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 서버 GPS 검증 ────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

-- 원하는 경도에서 시작하는 경로. 위의 pg_temp.track 은 늘 127.0 이다.
create or replace function pg_temp.track_at(p_start timestamptz, p_secs int, p_dps numeric, p_lng numeric)
returns text language sql as $$
  select string_agg(
    format('%s,%s,%s',
      (37.5 + i * p_dps)::numeric(12, 6),
      p_lng::numeric(12, 6),
      (extract(epoch from p_start) * 1000)::bigint + i * 1000),
    ';' order by i)
  from generate_series(0, p_secs) i
$$;

insert into auth.users (id, email, raw_user_meta_data) values
  ('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', 'g@test', '{"full_name":"Eun Seo"}'),
  ('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', 'f@test', '{"full_name":"Fin Yoo"}');

do $$
declare v record;
begin
  select * into v from economy.track_summary(pg_temp.fx('track_ok'));
  perform pg_temp.ok(v.gps_m between 1950 and 2050,
    format('경로에서 거리를 잰다 (%s m)', round(v.gps_m::numeric)));
  perform pg_temp.ok(v.points = 601, '경로의 점을 센다');
  select * into v from economy.track_summary('');
  perform pg_temp.ok(v.gps_m = 0 and v.points = 0, '경로가 없으면 거리도 0');
  select * into v from economy.track_summary(pg_temp.fx('track_car'));
  perform pg_temp.ok(v.gps_m < 1000, '튄 구간(시속 60km 초과)은 거리에 넣지 않는다');
end $$;

-- 일시정지 (0033) — 5분 달리고, 멈춘 채 버스로 약 3km 가서, 다시 5분 달렸다
do $$
declare v record; s record; v_t0 timestamptz := now() - interval '3 hours'; v_track text;
begin
  v_track := pg_temp.track(v_t0, 300, 0.00003) || ';'
          || pg_temp.track_at(v_t0 + interval '900 seconds', 300, 0.00003, 127.03);
  select * into v from economy.track_summary(v_track);
  perform pg_temp.ok(v.gps_m between 1900 and 2100,
    format('멈춘 동안 이동한 거리는 경로 거리에 넣지 않는다 (%s m)', round(v.gps_m::numeric)));
  select * into s from economy.track_speed_stats(v_track);
  perform pg_temp.ok(s.top_speed_kmh < 13,
    format('멈춘 동안의 속도는 최고 속도가 되지 않는다 (%s km/h)', round(s.top_speed_kmh::numeric, 1)));
end $$;

set role authenticated;
call pg_temp.login('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');

do $$
declare r record; v_start timestamptz := pg_temp.fx('run_start')::timestamptz;
begin
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601, pg_temp.fx('track_ok'), 0, 1, '');
  perform pg_temp.ok(r.verdict = 'CLEAN', '걸음과 경로가 맞는 러닝은 CLEAN');
  perform pg_temp.ok(
    (select distance_meters from public.walk_sessions where id = r.session_id) between 1950 and 2050,
    '거리는 걸음이 아니라 경로로 잰다');
  perform pg_temp.ok(
    (select gps_distance_m from public.walk_sessions where id = r.session_id) between 1950 and 2050,
    '경로 거리가 따로 남는다');

  -- 같은 경로를 1분 뒤 시작한 다른 세션으로 다시 올린다
  select * into r from public.record_session(
    v_start + interval '60 seconds', v_start + interval '600 seconds', 1800, 540,
    pg_temp.fx('track_ok'), 0, 1, '');
  perform pg_temp.ok(r.verdict = 'VOID', '이미 올린 경로를 다시 쓰면 VOID');
  perform pg_temp.ok(
    (select verdict_reason from public.walk_sessions where id = r.session_id) = '이미 올린 경로입니다',
    '이유가 남는다 — 이미 올린 경로');

  -- 어제 경로를 오늘 저녁 세션에 붙인다
  select * into r from public.record_session(
    v_start + interval '8 hours', v_start + interval '8 hours 601 seconds', 2000, 601,
    pg_temp.fx('track_ok') || ';37.6,127.0,1', 0, 1, '');
  perform pg_temp.ok(r.verdict = 'VOID', '경로 시각이 러닝 시간 밖이면 VOID');
  perform pg_temp.ok(r.points_awarded = 0, '시각이 안 맞는 경로는 적립이 없다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('base')::timestamptz + interval '9 hours';
begin
  -- 시속 24km 로 10분, 걸음은 100보 — 자전거
  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 100, 601,
    pg_temp.track_at(v_start, 600, 0.00006, 127.1), 0, 1, '');
  perform pg_temp.ok(r.verdict = 'VOID', '걸음 없이 1km 넘게 움직이면 VOID');
  perform pg_temp.ok(
    (select verdict_reason from public.walk_sessions where id = r.session_id) = '걸음 없이 이동한 거리입니다',
    '이유가 남는다 — 바퀴');

  -- 30분에 2km 걸으면서 6,000보 — 폰을 흔들었다
  select * into r from public.record_session(
    v_start + interval '1 hour', v_start + interval '1 hour 1801 seconds', 6000, 1801,
    pg_temp.track_at(v_start + interval '1 hour', 1800, 0.00001, 127.2), 0, 1, '');
  perform pg_temp.ok(r.verdict = 'FLAGGED', '걸음이 경로의 2배를 넘으면 FLAGGED');
  perform pg_temp.ok(
    (select verified_steps from public.walk_sessions where id = r.session_id) between 5100 and 5400,
    format('경로가 받쳐 주는 만큼만 인정된다 (%s보)',
      (select verified_steps from public.walk_sessions where id = r.session_id)));
  perform pg_temp.ok(
    (select distance_meters from public.walk_sessions where id = r.session_id) between 1950 and 2050,
    '거리도 경로만큼이다');
end $$;

do $$
declare r record; v_start timestamptz := pg_temp.fx('base')::timestamptz + interval '12 hours';
begin
  -- 러닝머신 — 경로가 없다. 케이던스만 본다.
  select * into r from public.record_session(
    v_start, v_start + interval '1200 seconds', 3000, 1200, '', 0, 1, '');
  perform pg_temp.ok(r.verdict = 'CLEAN', '경로가 없는 실내 러닝은 걸음으로 받는다');
  perform pg_temp.ok(
    (select distance_meters from public.walk_sessions where id = r.session_id) = 3000 * 0.762,
    '실내 러닝의 거리는 걸음으로 잰다');
end $$;
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 코스 기록 순위 ───────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

-- 코스는 track_ok 와 같은 길(시각 없이, 10점마다 하나)
insert into fix (k, v)
  select 'course_ok', string_agg(
    format('%s,%s', (37.5 + i * 0.00003)::numeric(12, 6), 127.000000), ';' order by i)
  from generate_series(0, 600, 10) i;

set role authenticated;
call pg_temp.login('11111111-1111-1111-1111-111111111111');

do $$
declare v_id bigint; r record;
begin
  v_id := public.course_share('성수 한 바퀴', '서울 성수', 2.0, 0, pg_temp.fx('course_ok'));
  perform pg_temp.ok(v_id is not null, '코스를 공유한다');

  select * into r from public.course_run_submit(pg_temp.fx('course_ok'), pg_temp.fx('run_start')::timestamptz);
  perform pg_temp.ok(r.course_id = v_id, '길로 코스를 찾아 기록을 낸다');
  perform pg_temp.ok(r.duration_sec = 601, '기록은 서버에 남은 러닝 시간이다');
  perform pg_temp.ok(r.rank = 1 and r.runners = 1, '첫 기록은 1위');

  select * into r from public.course_run_submit(pg_temp.fx('course_ok'), pg_temp.fx('run_start')::timestamptz);
  perform pg_temp.ok(
    (select run_count from public.courses where id = v_id) = 1,
    '같은 러닝을 다시 내도 달린 횟수는 한 번');

  perform pg_temp.ok(
    (select count(*) from public.course_run_submit('37.0,126.0;37.1,126.1', pg_temp.fx('run_start')::timestamptz)) = 0,
    '서버에 없는 코스는 조용히 넘어간다');
end $$;

call pg_temp.must_fail(
  $q$ select * from public.course_run_submit(pg_temp.fx('course_ok'), pg_temp.fx('car_start')::timestamptz) $q$,
  '무효 판정 러닝은 코스 기록이 될 수 없다');
call pg_temp.must_fail(
  $q$ select * from public.course_run_submit(pg_temp.fx('course_ok'), now() - interval '3 days') $q$,
  '서버에 없는 러닝은 코스 기록이 될 수 없다');
call pg_temp.must_fail($q$ select * from public.course_runs $q$, '코스 기록 표를 직접 읽을 수 없다');
call pg_temp.must_fail(
  $q$ insert into public.course_runs (course_id, user_id, session_id, duration_sec) values (1, auth.uid(), 1, 1) $q$,
  '코스 기록을 직접 쓸 수 없다');

call pg_temp.login('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
do $$
declare r record;
begin
  select * into r from public.course_run_submit(pg_temp.fx('course_ok'), pg_temp.fx('run_start')::timestamptz);
  perform pg_temp.ok(r.runners = 2, '다른 사람의 기록이 더해진다');
end $$;

-- 다른 동네를 달린 러닝은 이 코스 기록이 아니다
call pg_temp.must_fail(
  $q$ select * from public.course_run_submit(pg_temp.fx('course_ok'),
        pg_temp.fx('base')::timestamptz + interval '10 hours') $q$,
  '코스를 따라 달리지 않은 러닝은 받지 않는다');

do $$
declare n int; me record;
begin
  select count(*) into n from public.course_leaderboard(pg_temp.fx('course_ok'), 20);
  perform pg_temp.ok(n = 2, '코스 순위에 두 사람이 나온다');
  select * into me from public.course_leaderboard(pg_temp.fx('course_ok'), 20) where is_me;
  perform pg_temp.ok(me.display_name = 'Eun Seo' and me.duration_sec = 601, '내 기록이 표시된다');
  perform pg_temp.ok(
    (select run_count from public.courses where md5(track) = md5(pg_temp.fx('course_ok'))) = 2,
    '코스의 달린 횟수는 서버가 센다');
end $$;
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 땅따먹기 ─────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

do $$
declare c text; v record;
begin
  c := economy.hex_cell(37.5443, 127.0557);
  select * into v from economy.hex_center(c);
  perform pg_temp.ok(economy.hex_cell(v.lat, v.lng) = c, format('칸 가운데는 그 칸 안이다 (%s)', c));
  perform pg_temp.ok(economy.haversine_m(37.5443, 127.0557, v.lat, v.lng) < 200,
    '칸 가운데는 좌표에서 200m 안이다');
  perform pg_temp.ok(economy.hex_cell(37.5443, 127.0557) <> economy.hex_cell(37.5543, 127.0557),
    '1km 떨어지면 다른 칸이다');
  -- 앱(domain/Territory.kt, TerritoryTest)과 같은 값이어야 한다. 어긋나면 한 칸씩 밀려 보인다.
  perform pg_temp.ok(
    economy.hex_cell(37.5443, 127.0557) = '44405:20068'
    and economy.hex_cell(37.440309273233716, 127.13897349477489) = '44474:20003'
    and economy.hex_cell(37.629132385692984, 126.90202761029576) = '44313:20121'
    and economy.hex_cell(37.428157876032266, 126.8113389906088) = '44337:19995',
    '칸 이름이 앱과 같은 식으로 나온다');
end $$;

insert into fix (k, v)
  select 'terr_start', (pg_temp.fx('base')::timestamptz + interval '1 hour')::text;

set role authenticated;
call pg_temp.login('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');

do $$
declare v_a uuid; v_b uuid; r record; v_start timestamptz := pg_temp.fx('terr_start')::timestamptz;
  v_marks int;
begin
  v_a := public.crew_create('뚝섬 크루', 'TS', '', '서울 성수', 'OPEN');
  insert into fix (k, v) values ('terr_crew_a', v_a::text);

  select * into r from public.record_session(
    v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.track_at(v_start, 600, 0.00003, 129.0), 0, 1, '');
  insert into fix (k, v) values ('terr_session', r.session_id::text);

  select count(*) into v_marks from public.territory_view(37.49, 128.99, 37.53, 129.01);
  perform pg_temp.ok(v_marks between 5 and 20, format('달린 길의 칸이 칠해진다 (%s칸)', v_marks));
  perform pg_temp.ok(
    (select bool_and(mine and crew_name = '뚝섬 크루') from public.territory_view(37.49, 128.99, 37.53, 129.01)),
    '칸은 내 크루 색이다');
  perform pg_temp.ok(
    (select cells from public.territory_board(20) where crew_id = v_a) = v_marks,
    '크루 순위에 차지한 칸 수가 나온다');

  -- 크루 러닝이었다고 나중에 적으면 칸이 그 크루로 옮겨 간다
  v_b := public.crew_create('새벽 크루', 'SB', '', '서울 성수', 'OPEN');
  perform public.session_tag_crew(v_start, v_b);
  perform pg_temp.ok(
    (select bool_and(crew_id = v_b) from public.territory_view(37.49, 128.99, 37.53, 129.01)),
    '크루 러닝으로 적으면 칸이 그 크루 몫이 된다');

  -- 같은 날 같은 길을 또 달려도 칸 점수는 그대로다
  select * into r from public.record_session(
    v_start + interval '2 hours', v_start + interval '2 hours 601 seconds', 2000, 601,
    pg_temp.track_at(v_start + interval '2 hours', 600, 0.00003, 129.0), 0, 1, '');
  perform pg_temp.ok(
    (select max(score) from public.territory_view(37.49, 128.99, 37.53, 129.01)) = 1,
    '한 사람이 한 칸에 하루 한 번만 칠한다');
end $$;

call pg_temp.must_fail(
  $q$ select * from public.territory_view(37.0, 126.0, 38.0, 128.0) $q$,
  '너무 넓은 지역은 한 번에 받지 않는다');
call pg_temp.must_fail($q$ select * from public.territory_marks $q$, '땅 표시를 직접 읽을 수 없다');
call pg_temp.must_fail(
  $q$ select public.territory_credit(pg_temp.fx('terr_session')::bigint) $q$,
  '앱이 칸 칠하기를 직접 부를 수 없다');

-- 크루가 없는 사람은 칠하지 않는다(aaaa 는 크루가 없다)
call pg_temp.login('aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa');
do $$
begin
  perform pg_temp.ok(
    (select count(*) from public.territory_view(37.49, 126.99, 37.53, 127.01)
      where crew_name is null) = 0,
    '크루가 없는 러닝은 칸을 칠하지 않는다');
end $$;
reset role;

do $$
begin
  perform pg_temp.ok(
    not exists (select 1 from public.territory_marks
                 where user_id = 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa'),
    '크루가 없는 사람의 러닝은 표시를 남기지 않는다');
  perform pg_temp.ok(
    not exists (select 1 from public.territory_marks t
                  join public.walk_sessions s on s.id = t.session_id
                 where s.verdict = 'VOID'),
    '무효 판정 러닝은 칸을 칠하지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 계정 삭제 ────────────────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════

insert into auth.users (id, email, raw_user_meta_data) values
  ('dddddddd-dddd-dddd-dddd-dddddddddddd', 'h@test', '{"full_name":"Del Me"}');

set role authenticated;
call pg_temp.login('dddddddd-dddd-dddd-dddd-dddddddddddd');
do $$
declare v_crew uuid; v_start timestamptz := pg_temp.fx('base')::timestamptz + interval '13 hours';
begin
  v_crew := public.crew_create('떠나는 크루', 'DM', '', '서울', 'OPEN');
  insert into fix (k, v) values ('del_crew', v_crew::text);
  perform public.record_session(v_start, v_start + interval '601 seconds', 2000, 601,
    pg_temp.track_at(v_start, 600, 0.00003, 130.0), 0, 1, '');
end $$;

call pg_temp.login('bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb');
do $$ begin perform public.crew_join(pg_temp.fx('del_crew')::uuid); end $$;

call pg_temp.login('dddddddd-dddd-dddd-dddd-dddddddddddd');
do $$ begin perform public.account_delete(); end $$;
reset role;

do $$
begin
  perform pg_temp.ok(
    not exists (select 1 from auth.users where id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    '계정이 지워진다');
  perform pg_temp.ok(
    not exists (select 1 from public.profiles where id = 'dddddddd-dddd-dddd-dddd-dddddddddddd')
    and not exists (select 1 from public.walk_sessions where user_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd')
    and not exists (select 1 from public.sup_ledger where user_id = 'dddddddd-dddd-dddd-dddd-dddddddddddd'),
    '프로필·러닝·원장이 함께 지워진다');
  perform pg_temp.ok(
    (select owner_id from public.crews where id = pg_temp.fx('del_crew')::uuid)
      = 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    '크루장이 떠나면 가장 오래된 크루원이 크루장이 된다');
end $$;

set role authenticated;
call pg_temp.login(null);
call pg_temp.must_fail($q$ select public.account_delete() $q$, '로그인하지 않으면 지울 수 없다');
reset role;

-- ════════════════════════════════════════════════════════════════════
\echo '── 보수 점검 회귀 검사 ──────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════
-- 한 번 뚫렸던 자리들. 다시 열리면 여기서 걸린다.

insert into auth.users (id, email, raw_user_meta_data) values
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'r@test', '{"full_name":"Regress"}');

set role authenticated;
call pg_temp.login('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee');

-- 내부 함수는 로그인한 사람도 부를 수 없다(Supabase 기본 권한까지 거둔다)
call pg_temp.must_fail(
  $q$ select public.market_settle(1, 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee',
        'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 1000000, 'BY_ASK', true) $q$,
  '로그인한 사람도 체결 함수로 SUP 를 만들 수 없다');
call pg_temp.must_fail(
  $q$ select public.admin_log('X', 'Y', '{}'::jsonb) $q$,
  '감사 기록을 바깥에서 적을 수 없다');

-- 순위 재료는 표에 바로 쓰지 못한다
call pg_temp.must_fail(
  $q$ update public.profiles set top_speed_kmh = 999
       where id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' $q$,
  '최고 속도를 직접 고칠 수 없다');
call pg_temp.must_fail(
  $q$ update public.profiles set lifetime_km = 99999
       where id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' $q$,
  '누적 거리를 직접 고칠 수 없다');
do $$ begin
  update public.profiles set display_name = '고친 이름'
   where id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
  perform pg_temp.ok(
    (select display_name from public.profiles where id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee') = '고친 이름',
    '이름은 직접 고칠 수 있다');
end $$;
call pg_temp.must_fail(
  $q$ insert into public.daily_steps (user_id, epoch_day, steps, goal)
      values ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 20000, 9999999, 8000) $q$,
  '일별 걸음을 표에 직접 적을 수 없다(steps_sync 만)');
call pg_temp.must_fail(
  $q$ update public.courses set run_count = 1000000
       where owner_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' $q$,
  '코스 달린 횟수를 직접 고칠 수 없다');

call pg_temp.must_fail(
  $q$ insert into public.courses (owner_id, name, track, shared, run_count)
      values ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'x', '1,2;3,4', true, 999999) $q$,
  '코스를 새로 넣으며 달린 횟수를 적을 수 없다');

-- 세션 시각·시간은 앱이 적은 대로 믿지 않는다
call pg_temp.must_fail(
  $q$ select public.record_session(now() - interval '30 days', now() - interval '30 days' + interval '10 minutes',
        40000, 0, '', 2000, 6, '') $q$,
  '7일보다 오래된 러닝은 받지 않는다');
do $$
declare r record; v_start timestamptz := now() - interval '2 hours';
begin
  select * into r from public.record_session(v_start, v_start + interval '10 minutes',
    40000, 0, '', 0, 1, '');
  perform pg_temp.ok(r.verdict = 'VOID' and r.points_awarded = 0,
    '운동 시간을 0으로 적어도 케이던스 검사를 피하지 못한다');

  v_start := now() - interval '3 hours';
  perform public.record_session(v_start, v_start + interval '10 minutes',
    0, 2147483647, '', 0, 1, '');
  perform pg_temp.ok(
    (select duration_sec from public.walk_sessions
      where user_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee' and started_at = v_start) <= 600,
    '운동 시간은 시작~종료보다 길게 적히지 않는다');

  v_start := now() - interval '4 hours';
  select * into r from public.record_session(v_start, v_start + interval '10 minutes',
    1200, 600, '', 3000, 1, '');
  perform pg_temp.ok(r.session_id is not null
    and (select boost_bps from public.walk_sessions where id = r.session_id) = 0,
    '폰이 보낸 부스트는 무시하고 서버 신발 값(신발 없음 = 0)을 적는다');
end $$;
reset role;

-- 입찰을 걸었다 거둔 돈(ESCROW_UNLOCK)은 적립 순위에 들어가지 않는다
insert into public.sup_ledger (user_id, kind, amount, description) values
  ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'ESCROW_UNLOCK', 50000, '회귀 검사');
set role authenticated;
call pg_temp.login('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee');
do $$
declare r record;
begin
  select * into r from public.leaderboard('TOTAL_SUP', 20) where is_me;
  perform pg_temp.ok(r.sup is null or r.sup < 50000, '적립 순위는 번 것만 센다');
end $$;
reset role;

-- 출처 원본 표(endpoint · 오류 원문)는 앱이 읽지 못하고, 공개 뷰는 읽힌다
set role anon;
call pg_temp.login(null);
call pg_temp.must_fail($q$ select endpoint from public.content_sources $q$,
  '출처 원본 표를 바로 읽을 수 없다');
do $$ begin
  perform pg_temp.ok((select count(*) from public.running_sources_public) >= 1,
    '출처 공개 뷰는 그대로 읽힌다');
end $$;
call pg_temp.must_fail($q$ update public.running_sources_public set enabled = true where id = 'manual' $q$,
  '출처 공개 뷰로 출처를 고칠 수 없다');
call pg_temp.must_fail($q$ delete from public.running_sources_public where id = 'naver-news' $q$,
  '출처 공개 뷰로 출처를 지울 수 없다');
reset role;

-- 푸시: 가져간 줄은 표시하기 전에 다른 호출이 다시 가져가지 않는다
insert into public.push_outbox (user_id, kind, args)
  values ('eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee', 'COMMENT', '{}'::jsonb);
do $$
declare v_first int; v_again int;
begin
  select count(*) into v_first from public.push_claim_batch(500)
   where user_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
  select count(*) into v_again from public.push_claim_batch(500)
   where user_id = 'eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee';
  perform pg_temp.ok(v_first = 1 and v_again = 0, '같은 푸시를 두 번 가져가지 않는다');
end $$;

-- ════════════════════════════════════════════════════════════════════
\echo ''
\echo '── 서버 경제 (0022~0025) ────────────────────────────────────────'
-- ════════════════════════════════════════════════════════════════════
--
-- 체인으로 나갈 수 있는 가치는 모두 서버가 정한다. 폰이 정하던 것이 하나라도
-- 남아 있으면 그 틈으로 진짜 토큰이 나간다. 한 줄씩 두드린다.

reset role;
-- 어테스터 역할로 바꿔 검사하는 동안에도 준비물은 읽어야 한다(그 밖의 표는 못 읽는다).
grant all on fix to stepup_attester;
insert into auth.users (id, email, raw_user_meta_data) values
  ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 'fa@test', '{"full_name":"Fa One"}'),
  ('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', 'fb@test', '{"full_name":"Fb Two"}');
update public.economy_settings set value = '7' where key = 'new_account_days';

set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');

do $$
begin
  perform public.economy_bootstrap();
  perform public.economy_bootstrap();
  perform pg_temp.ok((select count(*) from public.my_sneakers() where origin = 'STARTER') = 1,
    '첫 신발은 여러 번 불러도 하나다');
  perform pg_temp.ok((select equipped from public.my_sneakers() where origin = 'STARTER'),
    '첫 신발을 바로 신긴다');
  perform pg_temp.ok((select granted from public.draw_grants where kind = 'FREE') = 10,
    '신규 가입자는 무료 뽑기 10회');
  perform pg_temp.ok((select energy_max from public.my_economy()) = 10, '에너지 최대는 신발 1레벨 = 10칸');
end $$;

-- 무료 뽑기 10회, 11번째는 거절
do $$
declare i int; v_id bigint;
begin
  for i in 1..10 loop
    v_id := public.draw_free();
  end loop;
  perform pg_temp.ok((select count(*) from public.my_sneakers() where origin = 'FREE_DRAW') = 10,
    '무료 뽑기 10켤레');
  perform pg_temp.ok((select bool_and(lock_km = 50 and not can_withdraw)
                        from public.my_sneakers() where origin = 'FREE_DRAW'),
    '무료 신발은 50km 전에는 꺼낼 수 없다');
  insert into fix (k, v) select 'free_shoe', min(id)::text from public.my_sneakers() where origin = 'FREE_DRAW';
  insert into fix (k, v) select 'starter_shoe', min(id)::text from public.my_sneakers() where origin = 'STARTER';
end $$;
call pg_temp.must_fail($q$ select public.draw_free() $q$, '무료 뽑기는 10회까지');
reset role;
do $$ begin
  perform pg_temp.ok(not exists (
      select 1 from public.market_sneakers s,
             lateral economy.efficiency_range(s.rarity) er,
             lateral economy.comfort_range(s.rarity) cr
       where s.owner_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and s.origin = 'FREE_DRAW'
         and (s.efficiency_bps not between er.lo and er.hi or s.comfort_bps not between cr.lo and cr.hi
              or s.level <> 1 or s.durability_pts <> 100)),
    '뽑은 스탯은 모두 등급 범위 안이다');
end $$;
set role authenticated;
call pg_temp.must_fail($q$ select public.draw_paid() $q$, 'SUP 가 없으면 유료 뽑기를 못 한다');

-- 공정성: 씨앗을 공개하면 지난 뽑기를 누구나 다시 계산할 수 있다
do $$
declare v_hash text; r record;
begin
  select seed_hash into v_hash from public.draw_fairness();
  select * into r from public.draw_rotate_seed();
  perform pg_temp.ok(r.seed_hash = v_hash and encode(sha256(decode(r.seed_hex, 'hex')), 'hex') = v_hash,
    '공개된 씨앗의 해시가 미리 알려 준 해시와 같다');
  perform pg_temp.ok((select seed_hash from public.draw_fairness()) <> v_hash, '새 씨앗으로 바뀐다');
  insert into fix (k, v) values ('seed_hex', r.seed_hex);
end $$;
reset role;
do $$
declare v_bad int;
begin
  select count(*) into v_bad from public.market_sneakers s
   where s.owner_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and s.origin = 'FREE_DRAW'
     and s.rarity <> economy.roll_rarity(economy.bytes_int(
           sha256(decode(pg_temp.fx('seed_hex'), 'hex') || convert_to(':' || s.draw_nonce::text, 'UTF8')), 0, 4));
  perform pg_temp.ok(v_bad = 0, '공개된 씨앗으로 다시 계산한 등급이 모두 맞는다');
end $$;
set role authenticated;

-- 직접 고치기는 막혀 있다
call pg_temp.must_fail(
  $q$ update public.market_sneakers set level = 30 where owner_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' $q$,
  '신발 레벨을 직접 고칠 수 없다');
call pg_temp.must_fail($q$ select * from public.economy_settings $q$, '운영 값은 앱이 읽을 수 없다');
call pg_temp.must_fail($q$ select * from public.chain_events $q$, '체인 이벤트 표는 앱이 읽을 수 없다');
call pg_temp.must_fail($q$ select public.attester_pause('앱이 멈추기') $q$, '앱은 어테스터 함수를 부를 수 없다');
call pg_temp.must_fail(
  $q$ select public.attester_wallet_link('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', '0x1111111111111111111111111111111111111111', 'x') $q$,
  '앱은 지갑을 서명 검증 없이 붙일 수 없다');
call pg_temp.must_fail(
  $q$ select economy.ledger_apply('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 'EARN_WALK', 1000, 'x') $q$,
  '앱은 잔고 함수를 직접 부를 수 없다');

-- 에너지 셀: 가득이면 팔지 않는다
reset role;
insert into public.sup_ledger (user_id, kind, amount, description)
values ('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 'EARN_EVENT', 5000, '검사용 잔고');
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
call pg_temp.must_fail($q$ select public.boost_buy('ENERGY_CELL') $q$, '에너지가 가득이면 에너지 셀을 팔지 않는다 (X9)');

-- 유료 뽑기 · 강화 · 착용
do $$
declare v_id bigint; v_before numeric; v_lv int;
begin
  v_before := (select balance from public.my_economy());
  v_id := public.draw_paid();
  perform pg_temp.ok((select balance from public.my_economy()) = v_before - 500, '유료 뽑기는 500 SUP');
  perform pg_temp.ok((select origin from public.my_sneakers() where id = v_id) = 'PAID_DRAW', '유료 뽑기 신발');
  insert into fix (k, v) values ('paid_shoe', v_id::text);

  v_before := (select balance - (select upgrade_cost from public.my_sneakers() where id = v_id)
                 from public.my_economy());
  v_lv := public.sneaker_upgrade(v_id);
  perform pg_temp.ok(v_lv = 2, '강화하면 레벨이 오른다');
  perform pg_temp.ok((select balance from public.my_economy()) = v_before, '강화 값만큼 잔고가 준다');

  perform public.sneaker_equip(v_id);
  perform pg_temp.ok((select count(*) from public.my_sneakers() where equipped) = 1, '신는 신발은 하나');
  perform pg_temp.ok((select energy_max from public.my_economy()) = 12, '2레벨 신발을 신으면 에너지 12칸');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.market_listings (sneaker_id, seller_id, price)
      values (%s, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', 100) $q$, pg_temp.fx('starter_shoe')),
  '매물 표에 직접 쓸 수 없다 (권한 없음)');
call pg_temp.must_fail(
  format($q$ select public.market_list(%s, 100) $q$, pg_temp.fx('free_shoe')),
  '무료 신발은 50km 전에는 팔 수 없다');
call pg_temp.must_fail(
  format($q$ select public.market_list(%s, 100) $q$, pg_temp.fx('paid_shoe')),
  '신고 있는 신발은 팔 수 없다');

-- 러닝 보상 — 신발 효율성 · 에너지 · 내구도
do $$
declare
  r record;
  v_start timestamptz := now() - interval '3 hours';
  v_eff int := (select efficiency_bps from public.my_sneakers() where id = pg_temp.fx('paid_shoe')::bigint);
  v_rarity text := (select rarity from public.my_sneakers() where id = pg_temp.fx('paid_shoe')::bigint);
  v_comfort int := (select comfort_bps from public.my_sneakers() where id = pg_temp.fx('paid_shoe')::bigint);
begin
  select * into r from public.record_session(v_start, v_start + interval '1201 seconds', 2000, 1201,
    pg_temp.track(v_start, 1200, 0.00001), 0, 5, '', false);
  perform pg_temp.ok(r.verdict = 'CLEAN', '정상 러닝');
  perform pg_temp.ok(r.points_awarded = round(2000 * 0.01 * (1 + v_eff / 10000.0), 4),
    format('적립 = 걸음 × 0.01 × (1 + 신발 효율성 %s bps) = %s', v_eff, r.points_awarded));
  perform pg_temp.ok(
    (select energy_used from public.walk_sessions where id = r.session_id)
      = round(2000 * (1 - v_comfort / 10000.0) / 600, 4),
    '착화감만큼 에너지를 덜 쓴다');
  perform pg_temp.ok(
    (select durability from public.my_sneakers() where id = pg_temp.fx('paid_shoe')::bigint)
      < 100,
    '달린 만큼 내구도가 준다');
  perform pg_temp.ok(
    (select km_run from public.my_sneakers() where id = pg_temp.fx('paid_shoe')::bigint) > 1,
    '신발에 달린 거리가 쌓인다');
end $$;

do $$
declare r record; v_start timestamptz := now() - interval '5 hours';
begin
  select * into r from public.record_session(v_start, v_start + interval '600 seconds', 1000, 600,
    '', 0, 1, '', true);
  perform pg_temp.ok(r.verdict = 'VOID' and r.points_awarded = 0, '가짜 위치가 감지되면 보상이 없다');
end $$;

-- 수리: 태운 SUP 만큼 잔고가 줄고 내구도가 100 으로
do $$
declare v_before numeric; v_dur numeric;
begin
  v_before := (select balance from public.my_economy());
  v_dur := public.sneaker_repair(pg_temp.fx('paid_shoe')::bigint);
  perform pg_temp.ok(v_dur = 100, '수리하면 내구도가 가득 찬다');
  perform pg_temp.ok((select balance from public.my_economy()) < v_before, '수리 값이 빠진다');
end $$;
call pg_temp.must_fail(format($q$ select public.sneaker_repair(%s) $q$, pg_temp.fx('paid_shoe')),
  '고칠 곳이 없으면 값을 받지 않는다');

-- 목표 보너스는 서버가 확인한 걸음으로, 하루 한 번
update public.profiles set daily_goal = 1000 where id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1';
-- 위 러닝(3시간 전)은 한국 시각 00~03시에 돌면 어제로 잡힌다. 그때만 오늘 러닝 하나를 더 둔다 —
-- 검사가 도는 시각에 따라 결과가 달라지지 않게 (다른 시각에는 아무것도 바꾸지 않는다).
reset role;
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, verified_steps,
                                  backed_steps, gps_backed, rewarded_steps, verdict)
select 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
       economy.game_day_start(economy.game_day(now())) + interval '3 seconds',
       economy.game_day_start(economy.game_day(now())) + interval '4 seconds', 1, 1000, 1000, 1000, true, 0, 'CLEAN'
 where economy.verified_steps_on('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', economy.game_day(now())) < 1000;
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$
begin
  perform pg_temp.ok(public.goal_claim() = 2.5, '목표 1,000보 달성 보너스 2.5 SUP');
end $$;
call pg_temp.must_fail($q$ select public.goal_claim() $q$, '목표 보너스는 하루 한 번');

-- 파티 인원은 출발 명단으로 센다
reset role;
do $$
declare v_party bigint; v_at timestamptz := now() - interval '7 hours';
begin
  insert into public.parties (flash_post_id, host_id, status, starts_at)
  values (pg_temp.fx('post_flash')::bigint, 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', 'FINISHED', v_at)
  returning id into v_party;
  -- f2 는 달리는 동안 위치를 보냈고, 3333 은 출발 직후 한 번 보내고 사라졌다
  insert into public.party_runs (party_id, user_id, starts_at, last_ping) values
    (v_party, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', v_at, null),
    (v_party, 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2', v_at, v_at + interval '5 minutes'),
    (v_party, '33333333-3333-3333-3333-333333333333', v_at, v_at + interval '10 seconds');
  insert into fix (k, v) values ('party_at', v_at::text);
end $$;
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$
declare r record; v_at timestamptz := pg_temp.fx('party_at')::timestamptz;
begin
  select * into r from public.record_session(v_at + interval '5 seconds', v_at + interval '605 seconds', 600, 600,
    '', 0, 9, '', false);
  perform pg_temp.ok((select party_size from public.walk_sessions where id = r.session_id) = 2,
    '폰이 9명이라고 해도 출발 명단에서 실제로 뛴 2명으로 센다 (X2)');
end $$;
-- 출발 명단에 있어도 뛰지 않은 사람은 세지 않는다 (0032)
reset role;
do $$
declare v_at timestamptz := pg_temp.fx('party_at')::timestamptz; v_size int;
begin
  select size into v_size from economy.party_for('33333333-3333-3333-3333-333333333333', v_at);
  perform pg_temp.ok(v_size = 3, '본인 · 위치를 계속 보낸 사람 · 기록을 올린 사람을 센다');
  update public.party_runs set last_ping = null
   where user_id = 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2' and starts_at = v_at;
  select size into v_size from economy.party_for('33333333-3333-3333-3333-333333333333', v_at);
  perform pg_temp.ok(v_size = 2, '위치를 안 보내고 기록도 없는 사람은 빠진다');
end $$;

-- 하루 금액 상한 — 가입 7일 안은 절반
reset role;
update public.economy_settings set value = '40' where key = 'daily_earn_cap';
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$
declare r record; v_start timestamptz := now() - interval '9 hours';
begin
  select * into r from public.record_session(v_start, v_start + interval '1800 seconds', 3000, 1800,
    '', 0, 1, '', false);
  perform pg_temp.ok(r.verdict = 'FLAGGED', '하루 금액 상한에 걸리면 FLAGGED');
  insert into fix (k, v) values ('cap_start', v_start::text) on conflict (k) do update set v = excluded.v;
end $$;
reset role;
do $$
declare v_before numeric; v_this numeric; v_at timestamptz := pg_temp.fx('cap_start')::timestamptz;
begin
  select coalesce(sum(points_awarded), 0) into v_before from public.walk_sessions
   where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and started_at <> v_at
     and economy.game_day(started_at) = economy.game_day(v_at);
  select points_awarded into v_this from public.walk_sessions
   where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and started_at = v_at;
  perform pg_temp.ok(v_this = greatest(20 - v_before, 0),
    format('신규 계정은 하루 적립 상한이 절반(20)이다 (앞서 %s, 이번 %s)', v_before, v_this));
end $$;
update public.economy_settings set value = '600' where key = 'daily_earn_cap';

-- ── 지갑 · 꺼내기 ──
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
call pg_temp.must_fail($q$ select public.sup_withdraw_request(10) $q$, '지갑이 없으면 꺼낼 수 없다');
call pg_temp.must_fail($q$ select public.wallet_link_challenge() $q$, '2단계 인증 없이는 지갑을 붙일 수 없다');
-- 0034 — 예전에 한 2단계 인증(로그인이 aal2 로 남아 있을 뿐)은 인정하지 않는다
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now() - interval '1 hour')::bigint)))::text, false);
call pg_temp.must_fail($q$ select public.wallet_link_challenge() $q$, '1시간 전에 한 2단계 인증으로는 지갑을 붙일 수 없다');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
do $$
declare v_msg text;
begin
  v_msg := public.wallet_link_challenge();
  insert into fix (k, v) values ('nonce_a', substring(v_msg from '확인 번호: ([0-9a-f]+)'));
end $$;
reset role;

set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
call pg_temp.must_fail(
  $q$ select public.attester_wallet_link('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1', '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 'wrong') $q$,
  '확인 번호가 틀리면 지갑을 붙이지 않는다');
do $$ begin
  perform pg_temp.ok(public.attester_wallet_link('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1',
    '0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA', pg_temp.fx('nonce_a')),
    '계정의 첫 지갑이면 true (어테스터가 이때만 가스비를 보낸다)');
end $$;
call pg_temp.must_fail($q$ select * from public.wallet_links $q$, '어테스터도 표를 직접 읽지 못한다');
reset role;

do $$ begin
  perform pg_temp.ok((select address from public.wallet_links
                       where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1') = '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    '지갑이 소문자로 붙는다');
  perform pg_temp.ok((select granted = 10 and genesis_granted = 1 from public.draw_grants
                       where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and kind = 'BONUS'),
    '처음 붙인 지갑이면 보너스 뽑기 10회(Genesis 1)');
end $$;

-- 같은 지갑을 다른 계정에 붙일 수 없다
set role authenticated;
call pg_temp.login('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
do $$
declare v_msg text;
begin
  perform public.economy_bootstrap();
  v_msg := public.wallet_link_challenge();
  insert into fix (k, v) values ('nonce_b', substring(v_msg from '확인 번호: ([0-9a-f]+)'));
end $$;
reset role;
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
call pg_temp.must_fail(
  format($q$ select public.attester_wallet_link('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2',
            '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '%s') $q$, pg_temp.fx('nonce_b')),
  '다른 계정에 붙은 지갑은 붙일 수 없다');
reset role;

set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
select set_config('request.jwt.claims', '', false);
call pg_temp.must_fail($q$ select public.sup_withdraw_request(10) $q$, '2단계 인증 없이는 꺼낼 수 없다');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
call pg_temp.must_fail($q$ select public.sup_withdraw_request(10) $q$, '지갑을 붙이고 72시간은 꺼낼 수 없다');
reset role;
update public.wallet_links set changed_at = now() - interval '73 hours'
 where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1';
set role authenticated;
call pg_temp.must_fail($q$ select public.sup_withdraw_request(10) $q$, '가입 7일 전에는 꺼낼 수 없다');
reset role;
update public.profiles set created_at = now() - interval '8 days', gps_km = 25
 where id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1';
set role authenticated;

do $$
declare v_before numeric; v_op uuid;
begin
  v_before := (select balance from public.my_economy());
  v_op := public.sup_withdraw_request(100);
  perform pg_temp.ok((select balance from public.my_economy()) = v_before - 100,
    '예약하는 순간 잔고에서 빠진다');
  perform pg_temp.ok((select status from public.chain_ops where id = v_op) = 'RESERVED', '작업이 예약된다');
  insert into fix (k, v) values ('op_sup', v_op::text);
end $$;
call pg_temp.must_fail($q$ select public.sup_withdraw_request(950) $q$, '사람별 하루 꺼내기 상한(1,000)');
reset role;

set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$
declare r record; v_op uuid := pg_temp.fx('op_sup')::uuid;
begin
  begin
    perform public.attester_op_payload(v_op, 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
    raise exception 'FAIL  남의 작업 번호로 서명 재료를 받았다';
  exception when sqlstate '22023' then
    raise notice '  OK   남의 작업 번호로는 서명 재료를 받지 못한다';
  end;
  select * into r from public.attester_op_payload(v_op, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(r.amount = 100 and r.wallet = '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'
                     and r.op_ref = '0x' || lpad(replace(v_op::text, '-', ''), 64, '0'), '서명 재료가 예약 그대로다');
  perform public.attester_op_submitted(v_op, '0x' || repeat('ab', 32));
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('ab', 32), 0, 100, 'SUP_CLAIMED',
                       jsonb_build_object('op', r.op_ref)) = 'CONFIRMED', '체인 이벤트로 확정된다');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('ab', 32), 0, 100, 'SUP_CLAIMED',
                       jsonb_build_object('op', r.op_ref)) = 'DUPLICATE', '같은 이벤트는 한 번만 처리한다');
  perform pg_temp.ok(public.attester_op_expire(v_op, false) = 'CONFIRMED', '확정된 작업은 되돌리지 않는다');
end $$;
reset role;

-- 만료: 마진 전에는 못 되돌리고, 지나면 환불
set role authenticated;
do $$ declare v_op uuid; begin
  v_op := public.sup_withdraw_request(50);
  insert into fix (k, v) values ('op_exp', v_op::text);
end $$;
reset role;
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ declare r record; begin
  select * into r from public.attester_op_payload(pg_temp.fx('op_exp')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
end $$;
call pg_temp.must_fail(format($q$ select public.attester_op_expire('%s', false) $q$, pg_temp.fx('op_exp')),
  '서명한 작업은 만료 마진이 지나기 전에 되돌리지 않는다');
reset role;
update public.chain_ops set deadline = now() - interval '2 hours' where id = pg_temp.fx('op_exp')::uuid;
insert into fix (k, v) values ('bal_before_exp', economy.balance_of('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1')::text);
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$
begin
  perform pg_temp.ok((select count(*) from public.attester_due_ops() where op_id = pg_temp.fx('op_exp')::uuid) = 1,
    '만료 대상 목록에 나온다');
  perform pg_temp.ok(public.attester_op_expire(pg_temp.fx('op_exp')::uuid, false) = 'EXPIRED', '체인에 없으면 만료');
  perform pg_temp.ok(public.attester_op_expire(pg_temp.fx('op_exp')::uuid, false) = 'EXPIRED', '두 번 불러도 결과가 같다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok(economy.balance_of('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1') = pg_temp.fx('bal_before_exp')::numeric + 50,
    '환불은 한 번만 된다');
end $$;

-- 신발 꺼내기 · 넣기
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
call pg_temp.must_fail(
  format($q$ select public.sneaker_withdraw_request(%s) $q$, pg_temp.fx('starter_shoe')),
  '첫 신발은 꺼낼 수 없다');
call pg_temp.must_fail(
  format($q$ select public.sneaker_withdraw_request(%s) $q$, pg_temp.fx('free_shoe')),
  '무료 신발은 50km 전에는 꺼낼 수 없다');
do $$
declare v_op uuid; v_shoe bigint := pg_temp.fx('paid_shoe')::bigint;
begin
  v_op := public.sneaker_withdraw_request(v_shoe);
  perform pg_temp.ok((select chain_state from public.my_sneakers() where id = v_shoe) = 'WITHDRAWING',
    '꺼내는 중 상태가 된다');
  insert into fix (k, v) values ('op_shoe', v_op::text);
end $$;
call pg_temp.must_fail(format($q$ select public.sneaker_equip(%s) $q$, pg_temp.fx('paid_shoe')),
  '꺼내는 중인 신발은 신을 수 없다');
call pg_temp.must_fail(format($q$ select public.sneaker_upgrade(%s) $q$, pg_temp.fx('paid_shoe')),
  '꺼내는 중인 신발은 강화할 수 없다');
reset role;

set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$
declare r record; v_op uuid := pg_temp.fx('op_shoe')::uuid;
begin
  select * into r from public.attester_op_payload(v_op, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(r.level = 2 and not r.transfer_locked, '서명 재료에 서버 스탯이 담긴다');
  perform public.attester_chain_event('0x' || repeat('cd', 32), 1, 101, 'SNEAKER_RELEASED',
    jsonb_build_object('op', r.op_ref, 'tokenId', '7'));
  -- 체인에서 산 사람(f2)이 넣는다
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('ce', 32), 0, 102, 'SNEAKER_DEPOSITED',
    jsonb_build_object('account', '0x' || lpad('f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2', 64, '0'), 'tokenId', '7'))
    = 'CREDITED', '넣은 신발이 넣은 사람 것이 된다');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('cf', 32), 0, 103, 'SUP_DEPOSITED',
    jsonb_build_object('account', '0x' || lpad('f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2f2', 64, '0'), 'amount', '12.5'))
    = 'CREDITED', 'SUP 넣기가 잔고에 들어간다');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('d0', 32), 0, 104, 'SUP_DEPOSITED',
    jsonb_build_object('account', '0xdead', 'amount', '5')) = 'ORPHAN', '받을 계정이 없는 입금은 사람에게 넘긴다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select owner_id = 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2' and chain_state = 'APP' and token_id = 7
                        from public.market_sneakers where id = pg_temp.fx('paid_shoe')::bigint),
    '넣은 신발은 앱으로 돌아와 새 주인 것이 된다');
  perform pg_temp.ok(economy.balance_of('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2') = 12.5, '넣은 SUP 가 잔고다');
end $$;

-- 지갑 보너스 — 첫 번째가 Genesis(희귀 이상)
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$
declare v_op uuid; i int; v_first bigint;
begin
  v_op := public.bonus_draw_request();
  v_first := (select sneaker_id from public.chain_ops where id = v_op);
  perform pg_temp.ok((select genesis_no is not null and rarity in ('EPIC', 'LEGENDARY')
                        from public.my_sneakers() where id = v_first),
    'Genesis 는 희귀 이상 확정이고 번호가 붙는다');
  for i in 2..10 loop
    v_op := public.bonus_draw_request();
  end loop;
  perform pg_temp.ok((select count(*) from public.my_sneakers() where origin = 'BONUS_DRAW' and genesis_no is not null) = 1,
    'Genesis 는 한 켤레뿐');
end $$;
call pg_temp.must_fail($q$ select public.bonus_draw_request() $q$, '보너스 뽑기는 10회까지');
reset role;

-- 어테스터 전용 로그인 계정 — 정해 둔 계정만 어테스터 함수를 부른다
reset role;
select set_config('request.jwt.claims', '', false);
update public.economy_settings set value = to_jsonb('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2'::text)
 where key = 'attester_user_id';
set role authenticated;
call pg_temp.login('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
do $$ begin
  perform pg_temp.ok(public.attester_cursor_get('nothing') is null, '어테스터로 정한 계정은 어테스터 함수를 부른다');
end $$;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
call pg_temp.must_fail($q$ select public.attester_cursor_get('nothing') $q$,
  '다른 사용자는 어테스터 함수를 부를 수 없다');
reset role;
update public.economy_settings set value = 'null'::jsonb where key = 'attester_user_id';

-- 정지 스위치
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ begin perform public.attester_pause('검사'); end $$;
reset role;
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
call pg_temp.must_fail($q$ select public.sup_withdraw_request(10) $q$, '정지 스위치가 켜지면 꺼내기를 멈춘다');
select set_config('request.jwt.claims', '', false);
reset role;
update public.economy_settings set value = 'false' where key = 'chain_paused';

-- ── 보안 검토에서 찾은 구멍 — 막혔는지 ──
\echo ''
\echo '── 서버 경제 보안 회귀 ──────────────────────────────────────────'

-- (1) 즉시 판매(market_sell_now)로 체인에 나간 신발 · 첫 신발을 팔 수 없다
reset role;
update public.market_sneakers set chain_state = 'ON_CHAIN' where id = pg_temp.fx('paid_shoe')::bigint;
insert into fix (k, v)
  select 'f2_starter', id::text from public.market_sneakers
   where owner_id = 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2' and origin = 'STARTER';
set role authenticated;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$
declare v_s record;
begin
  select faction, rarity, variant into v_s from public.market_sneakers where id = pg_temp.fx('paid_shoe')::bigint;
  insert into fix (k, v) values ('bid_chain', public.market_bid(v_s.faction, v_s.rarity, v_s.variant, 1, 300)::text);
  insert into fix (k, v) values ('bid_starter', public.market_bid('WIND', 'COMMON', 0, 1, 50)::text);
end $$;
call pg_temp.login('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
call pg_temp.must_fail(
  format($q$ select public.market_sell_now(%s, %s) $q$, pg_temp.fx('paid_shoe'), pg_temp.fx('bid_chain')),
  '체인에 나간 신발은 즉시 판매로도 팔 수 없다');
call pg_temp.must_fail(
  format($q$ select public.market_sell_now(%s, %s) $q$, pg_temp.fx('f2_starter'), pg_temp.fx('bid_starter')),
  '첫 신발은 즉시 판매로도 팔 수 없다');
reset role;
update public.market_sneakers set chain_state = 'APP' where id = pg_temp.fx('paid_shoe')::bigint;

-- (3) 경로 없는 러닝: 적립은 하루 조금만, 신발 잠금 거리 · 꺼내기 거리는 쌓이지 않는다.
--     시간이 겹치는 러닝은 무효.
update public.economy_settings set value = '60' where key = 'no_gps_daily_cap';
set role authenticated;
call pg_temp.login('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
do $$
declare
  r record;
  v_start timestamptz := now() - interval '6 days';
  v_km numeric := (select km_run from public.my_sneakers() where origin = 'STARTER');
begin
  select * into r from public.record_session(v_start, v_start + interval '17000 seconds', 60000, 17000,
    '', 0, 1, '', false);
  perform pg_temp.ok(r.points_awarded <= 60, format('경로 없는 러닝은 하루 60 SUP 까지 (%s)', r.points_awarded));
  perform pg_temp.ok((select km_run from public.my_sneakers() where origin = 'STARTER') = v_km,
    '경로 없는 러닝은 신발 잠금 거리에 쌓이지 않는다');
  perform pg_temp.ok((select gps_km from public.profiles where id = 'f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2') = 0,
    '경로 없는 러닝은 꺼내기 조건 거리에 쌓이지 않는다');

  select * into r from public.record_session(v_start + interval '10 minutes', v_start + interval '20 minutes',
    1000, 600, '', 0, 1, '', false);
  perform pg_temp.ok(r.verdict = 'VOID', '시간이 겹치는 러닝은 무효');
end $$;

-- (2) 자기가 만든 코스로는 코스 보상을 받지 못한다
do $$
declare v_start timestamptz := now() - interval '5 days'; v_track text; r record; v_before numeric;
begin
  v_track := pg_temp.track(v_start, 1200, 0.00002);
  perform public.course_share('내 코스', '서울', 42, 0, v_track);
  perform public.record_session(v_start, v_start + interval '1201 seconds', 2600, 1201, v_track, 0, 1, '', false);
  v_before := (select balance from public.my_economy());
  perform public.course_run_submit(v_track, v_start);
  perform pg_temp.ok((select balance from public.my_economy()) = v_before, '내가 만든 코스는 보상이 없다');
end $$;
reset role;

-- (8) 잠금 거리를 못 채운 무료 신발은 그 계정에 붙은 지갑이 넣을 때만 받는다
reset role;
update public.economy_settings set value = 'false' where key = 'chain_paused';
insert into fix (k, v)
  select 'op_bonus', id::text from public.chain_ops
   where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and kind = 'BONUS_MINT' order by created_at limit 1;
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$
declare r record;
begin
  select * into r from public.attester_op_payload(pg_temp.fx('op_bonus')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(r.transfer_locked, '보너스 신발은 잠긴 채로 발행된다');
  perform public.attester_chain_event('0x' || repeat('e1', 32), 0, 200, 'SNEAKER_RELEASED',
    jsonb_build_object('op', r.op_ref, 'tokenId', '900'));
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('e2', 32), 0, 201, 'SNEAKER_DEPOSITED',
    jsonb_build_object('account', '0x' || lpad('f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1', 64, '0'), 'tokenId', '900',
                       'from', '0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb')) = 'ORPHAN',
    '잠긴 신발을 남의 지갑에서 넣으면 받지 않는다');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('e3', 32), 0, 202, 'SNEAKER_DEPOSITED',
    jsonb_build_object('account', '0x' || lpad('f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1', 64, '0'), 'tokenId', '900',
                       'from', '0xAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA')) = 'CREDITED',
    '잠긴 신발은 그 계정의 지갑에서 넣으면 받는다');
end $$;
reset role;

-- 0029 점검 반영 — 모르는 작업 · 어긋난 작업은 멈추고, 취소는 신발을 돌려놓고, 넣기는 버림
insert into fix (k, v)
  select 'op_bonus' || row_number() over (order by created_at), id::text from public.chain_ops
   where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' and kind = 'BONUS_MINT' and status <> 'CONFIRMED';
insert into fix (k, v) values ('bal_0029', economy.balance_of('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1')::text);
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$
declare r record; r3 record;
begin
  -- 서명 재료는 멈추기 전에 받아 둔다(멈추면 서명 재료를 주지 않는다)
  select * into r from public.attester_op_payload(pg_temp.fx('op_bonus2')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  select * into r3 from public.attester_op_payload(pg_temp.fx('op_bonus3')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('f1', 32), 0, 300, 'SUP_CLAIMED',
      jsonb_build_object('op', '0x' || lpad('12345678123456781234567812345678', 64, '0'),
                         'runner', '0xcccccccccccccccccccccccccccccccccccccccc', 'amount', '1000'))
    = 'UNKNOWN_OP', '서버가 모르는 작업 번호로 나간 SUP 는 멈춤 신호');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('f2', 32), 0, 301, 'SNEAKER_RELEASED',
      jsonb_build_object('op', r.op_ref, 'tokenId', '901', 'to', '0xbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb'))
    = 'MISMATCH', '작업 번호가 맞아도 받는 지갑이 다르면 멈춤 신호');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('f3', 32), 0, 302, 'SNEAKER_RELEASED',
      jsonb_build_object('op', r.op_ref, 'tokenId', '900', 'to', '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa'))
    = 'MISMATCH', '다른 신발의 토큰 번호로 풀리면 멈춤 신호');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('f4', 32), 0, 303, 'OP_CANCELLED',
      jsonb_build_object('op', r3.op_ref)) = 'CANCELLED', '체인에서 취소한 작업');
  perform pg_temp.ok(public.attester_chain_event('0x' || repeat('f5', 32), 0, 304, 'SUP_DEPOSITED',
      jsonb_build_object('account', '0x' || lpad('f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1f1', 64, '0'), 'amount', '1.00009'))
    = 'CREDITED', '잘게 넣은 SUP');
  perform pg_temp.ok((select sup_deposited = 18.5 from public.attester_ledger_totals()),
    '대조용 넣기 합도 버림 (12.5 + 5 + 1.00009 → 18.5)');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select value = 'true'::jsonb from public.economy_settings where key = 'chain_paused'),
    '모르는 작업 · 어긋난 작업이 보이면 서버가 체인 작업을 멈춘다');
  perform pg_temp.ok((select status <> 'CONFIRMED' from public.chain_ops where id = pg_temp.fx('op_bonus2')::uuid),
    '어긋난 이벤트로는 작업을 확정하지 않는다');
  perform pg_temp.ok((select o.status = 'EXPIRED' and s.chain_state = 'APP'
                        from public.chain_ops o join public.market_sneakers s on s.id = o.sneaker_id
                       where o.id = pg_temp.fx('op_bonus3')::uuid),
    '취소한 작업의 신발은 앱으로 돌아온다');
  perform pg_temp.ok(economy.balance_of('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1') = pg_temp.fx('bal_0029')::numeric + 1,
    '4자리 아래는 버린다 — 넣은 것보다 더 주지 않는다');
end $$;
update public.economy_settings set value = 'false' where key = 'chain_paused';

-- 2차 점검 — 같은 작업을 곧바로 다시 서명하지 않는다 · 멈춤 상태를 어테스터가 읽는다
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ begin
  perform public.attester_op_payload(pg_temp.fx('op_bonus4')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(public.attester_chain_paused() = false, '멈추지 않았을 때 false');
end $$;
call pg_temp.must_fail(
  format($q$ select * from public.attester_op_payload('%s', 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1') $q$, pg_temp.fx('op_bonus4')),
  '방금 서명한 작업은 다른 요청이 곧바로 다시 서명하지 못한다');
reset role;
update public.chain_ops set updated_at = now() - interval '1 minute' where id = pg_temp.fx('op_bonus4')::uuid;
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ begin
  perform public.attester_op_payload(pg_temp.fx('op_bonus4')::uuid, 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
  perform pg_temp.ok(true, '잠시 뒤에는 다시 서명할 수 있다(보내기가 실패했을 때)');
  perform public.attester_pause('검사');
  perform pg_temp.ok(public.attester_chain_paused(), '멈추면 true');
end $$;
reset role;
update public.economy_settings set value = 'false' where key = 'chain_paused';

-- 계정을 지운 사람의 꺼내기가 만료돼도 오류 없이 만료된다(어테스터 만료가 멈추지 않게)
insert into public.chain_ops (id, user_id, kind, status, wallet, amount, deadline)
values ('0d0d0d0d-0d0d-0d0d-0d0d-0d0d0d0d0d0d', null, 'SUP_WITHDRAW', 'SIGNED',
        '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 5, now() - interval '2 hours');
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ begin
  perform pg_temp.ok(public.attester_op_expire('0d0d0d0d-0d0d-0d0d-0d0d-0d0d0d0d0d0d', false) = 'EXPIRED',
    '주인이 없는 꺼내기도 만료된다');
end $$;
reset role;

-- 체인 커서는 앞으로만 간다
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
do $$ begin
  perform public.attester_cursor_set('sneakers', 100);
  perform public.attester_cursor_set('sneakers', 50);
  perform pg_temp.ok(public.attester_cursor_get('sneakers') = 100, '체인 커서는 뒤로 가지 않는다');
end $$;
reset role;

-- (5)(6) 계정을 지워도 지갑은 다시 못 쓰고, 체인 작업 기록은 남는다
do $$ begin
  perform pg_temp.ok((select count(*) from public.chain_ops
                       where user_id = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1') > 0, '지우기 전 작업 기록');
end $$;
set role authenticated;
call pg_temp.login('f2f2f2f2-f2f2-f2f2-f2f2-f2f2f2f2f2f2');
do $$ begin perform public.account_delete(); end $$;
call pg_temp.login('f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1');
do $$ begin perform public.account_delete(); end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select count(*) from public.chain_ops where user_id is null) > 0,
    '계정을 지워도 체인 작업 기록은 남는다 (신발을 다른 사람이 가져간 경우 포함)');
  perform pg_temp.ok(exists (select 1 from public.market_sneakers where token_id = 7),
    '체인에 나간 적 있는 신발 기록은 남는다');
  perform pg_temp.ok(exists (select 1 from public.wallet_history
                              where address = '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa' and user_id is null),
    '지운 계정의 지갑 기록이 남는다');
end $$;
insert into auth.users (id, email) values ('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3', 'fc@test');
set role authenticated;
call pg_temp.login('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
do $$
declare v_msg text;
begin
  v_msg := public.wallet_link_challenge();
  insert into fix (k, v) values ('nonce_c', substring(v_msg from '확인 번호: ([0-9a-f]+)'));
end $$;
select set_config('request.jwt.claims', '', false);
reset role;
set role stepup_attester;
select set_config('request.jwt.claims',
  (coalesce(nullif(current_setting('request.jwt.claims', true), ''), '{}')::jsonb || '{"role":"stepup_attester"}')::text, false);
call pg_temp.must_fail(
  format($q$ select public.attester_wallet_link('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3',
            '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', '%s') $q$, pg_temp.fx('nonce_c')),
  '지운 계정이 쓰던 지갑을 새 계정에 붙여 보너스를 다시 받을 수 없다');
reset role;

-- 2차 점검 — 주인이 계정을 지운 신발은 아무도 가져가지 못한다
insert into fix (k, v)
  select 'orphan_shoe', id::text from public.market_sneakers
   where owner_id is null and chain_state = 'APP' order by id limit 1;
do $$ begin
  perform pg_temp.ok(pg_temp.fx('orphan_shoe') is not null, '(준비) 주인 없는 앱 신발이 있다');
end $$;
set role authenticated;
call pg_temp.login('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3');
do $$ begin perform public.economy_bootstrap(); end $$;
call pg_temp.must_fail(format($q$ select public.market_list(%s, 900) $q$, pg_temp.fx('orphan_shoe')),
  '주인 없는 신발을 매물로 걸 수 없다');
call pg_temp.must_fail(format($q$ select public.sneaker_equip(%s) $q$, pg_temp.fx('orphan_shoe')),
  '주인 없는 신발을 신을 수 없다');
call pg_temp.must_fail(format($q$ select public.sneaker_upgrade(%s) $q$, pg_temp.fx('orphan_shoe')),
  '주인 없는 신발을 강화할 수 없다');
select set_config('request.jwt.claims', json_build_object('aal', 'aal2', 'amr', json_build_array(
  json_build_object('method', 'totp', 'timestamp', extract(epoch from now())::bigint)))::text, false);
call pg_temp.must_fail(format($q$ select public.sneaker_withdraw_request(%s) $q$, pg_temp.fx('orphan_shoe')),
  '주인 없는 신발을 지갑으로 꺼낼 수 없다');
select set_config('request.jwt.claims', '', false);
call pg_temp.must_fail($q$ select public.market_list(
    (select id from public.my_sneakers() limit 1), 'NaN') $q$,
  'NaN 값으로 매물을 걸 수 없다');
reset role;
call pg_temp.must_fail(
  format($q$ update public.market_sneakers set owner_id = 'f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3' where id = %s $q$,
         pg_temp.fx('orphan_shoe')),
  '주인 없는 신발의 주인을 바꿀 수 없다(체인에서 넣은 경우 빼고)');

-- 0029 — 상대가 계정을 지운 거래도 "판 것인가"가 거짓/참으로 나온다
insert into public.market_trades (faction, rarity, variant, level, seller_id, buyer_id, price, fee, kind)
values ('WIND', 'COMMON', 0, 1, null, 'f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3', 10, 0, 'BY_ASK');
set role authenticated;
call pg_temp.login('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3');
do $$ begin
  perform pg_temp.ok((select bool_and(sold is not null) and bool_and(not sold) from public.market_my_trades()),
    '판 사람이 지워진 거래도 sold 가 비지 않는다');
end $$;
reset role;

-- 0030 — 앱의 하루 목표가 서버 판정에 쓰인다
set role authenticated;
call pg_temp.login('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3');
do $$ begin
  perform pg_temp.ok(public.profile_set_daily_goal(3000) = 3000, '목표를 서버에 적는다');
  perform pg_temp.ok(public.profile_set_daily_goal(999999) = 30000, '범위 밖 목표는 goal_claim 범위로 맞춘다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select daily_goal from public.profiles where id = 'f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3') = 30000,
    '프로필에 남는다');
end $$;
set role anon;
call pg_temp.must_fail($q$ select public.profile_set_daily_goal(5000) $q$, '로그인 없이는 못 바꾼다');
reset role;

-- 0031 — 신발 표의 주인 칸은 아무도 직접 읽지 못한다 (계정 ↔ 지갑 연결이 드러나지 않게)
set role authenticated;
call pg_temp.login('f3f3f3f3-f3f3-f3f3-f3f3-f3f3f3f3f3f3');
call pg_temp.must_fail($q$ select owner_id from public.market_sneakers limit 1 $q$, '신발 표의 주인 칸은 읽을 수 없다');
call pg_temp.must_fail($q$ select token_id from public.market_sneakers limit 1 $q$, '체인 토큰 번호도 읽을 수 없다 (거래 기록과 이으면 지갑이 드러난다)');
do $$ begin
  perform pg_temp.ok((select count(*) from public.market_quotes) >= 0, '호가 뷰는 그대로 읽힌다');
  perform pg_temp.ok((select count(*) from public.market_asks) >= 0, '매물 뷰는 그대로 읽힌다');
  perform pg_temp.ok((select count(*) from public.my_sneakers()) >= 0, '내 신발은 그대로 읽힌다');
end $$;
reset role;

\echo ''
\echo '════════════════════════════════════════════════════════════════'
\echo ' 전부 통과했습니다.'
\echo '════════════════════════════════════════════════════════════════'
