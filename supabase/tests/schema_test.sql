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
insert into fix (k, v) values
  ('base', (date_trunc('day', now() - interval '1 day') + interval '10 hours')::text);

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

  -- 2000보 × 0.01 × 파티2(1.1) × 부스트1200bps(1.12) = 24.64
  perform pg_temp.ok(r.verdict = 'CLEAN', '정상 세션은 CLEAN');
  perform pg_temp.ok(r.points_awarded = 24.64,
    format('적립액을 서버가 계산한다 (%s SUP)', r.points_awarded));
  perform pg_temp.ok(r.balance = 24.64, '잔고가 적립만큼 늘었다');

  perform pg_temp.ok(
    (select faction from public.walk_sessions where id = r.session_id) = 'FIRE',
    '정산 시점 종족이 함께 남는다');
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
  perform pg_temp.ok(r.points_awarded = 24.64, '같은 세션을 다시 보내도 결과가 같다');
  perform pg_temp.ok(
    (select coalesce(sum(amount), 0) from public.sup_ledger) = 24.64,
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
  perform pg_temp.ok(v_left = (24.64 + 460.00) - 10, format('차감 뒤 잔고 %s', v_left));
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
  insert into public.crews (owner_id, name, monogram, tagline, area)
  values ('11111111-1111-1111-1111-111111111111', '한강 러너스', 'HR', '매주 토요일', '서울 마포')
  returning id into v_crew;
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
  insert into public.posts (author_id, category, title, body)
  values ('11111111-1111-1111-1111-111111111111', 'FREE', '오늘 날씨 좋네요', '한강 추천')
  returning id into v_post;
  insert into fix (k, v) values ('post_open', v_post::text);

  insert into public.posts (author_id, category, crew_id, title, body)
  values ('11111111-1111-1111-1111-111111111111', 'TIP', v_crew, '크루만 보는 글', '내일 6시')
  returning id into v_post;
  insert into fix (k, v) values ('post_crew', v_post::text);

  insert into public.posts (author_id, category, title, place, distance_km, meet_at, capacity)
  values ('11111111-1111-1111-1111-111111111111', 'FLASH', '오늘 저녁 7시 번개',
          '여의도 한강공원', 5, now() + interval '3 hours', 2)
  returning id into v_post;
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
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 1, '번개에 참가하면 인원이 센다');
  select public.join_flash(v_post) into v_n;
  perform pg_temp.ok(v_n = 1, '두 번 눌러도 한 명이다');
end $$;

call pg_temp.must_fail(
  format($q$ insert into public.flash_participants (post_id, user_id)
             values (%s, '22222222-2222-2222-2222-222222222222') $q$,
         pg_temp.fx('post_flash')),
  '참가자 표에 직접 넣을 수 없다');

call pg_temp.login('33333333-3333-3333-3333-333333333333');
do $$
declare v_n int;
begin
  select public.join_flash(pg_temp.fx('post_flash')::bigint) into v_n;
  perform pg_temp.ok(v_n = 2, '정원 2명 중 둘째가 들어간다');
end $$;

call pg_temp.login('11111111-1111-1111-1111-111111111111');
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

-- 코스
call pg_temp.login('11111111-1111-1111-1111-111111111111');
do $$
begin
  insert into public.courses (owner_id, name, area, distance_km, track, shared)
  values ('11111111-1111-1111-1111-111111111111', '한강 5km', '서울 마포', 5.0,
          '37.5,127.0;37.51,127.0', true);
  insert into public.courses (owner_id, name, area, distance_km, shared)
  values ('11111111-1111-1111-1111-111111111111', '혼자 보는 코스', '서울', 3.0, false);
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
  -- 적립은 24.64 + 460.00, 쓴 10 은 순위에서 빼지 않는다
  perform pg_temp.ok(r.sup = 484.64,
    format('누적 적립으로 줄을 세운다 (쓴 돈은 빼지 않는다, %s)', r.sup));

  perform pg_temp.ok(
    (select count(*) from public.leaderboard('LONGEST_TIME', 20)) >= 1,
    '누적 시간 순위도 나온다');

  select * into r from public.leaderboard('TOP_SPEED', 20) where is_me;
  perform pg_temp.ok(r.total = 1, '전체 인원이 함께 온다 ("N명 중 몇 등"의 N)');
end $$;

do $$
declare r record;
begin
  select * into r from public.faction_leaderboard() where faction = 'FIRE';
  perform pg_temp.ok(r.km > 0, '종족에 거리가 쌓인다');
  perform pg_temp.ok(r.my_km = r.km, '내 몫이 따로 나온다');

  select * into r from public.faction_leaderboard() where faction = 'WIND';
  perform pg_temp.ok(r.km = 0, 'VOID 세션은 종족에도 쌓이지 않는다');

  perform pg_temp.ok((select count(*) from public.faction_leaderboard()) = 4,
    '아무도 안 뛴 종족도 줄은 나온다');
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

  select * into r from public.faction_leaderboard('MONTH') where faction = 'FIRE';
  perform pg_temp.ok(r.km > 0, '종족 순위도 기간을 받는다');
  perform pg_temp.ok((select count(*) from public.faction_leaderboard('MONTH')) = 4,
    '기간을 좁혀도 종족 네 줄은 그대로 나온다');
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


-- ════════════════════════════════════════════════════════════════
--  NFT 마켓
-- ════════════════════════════════════════════════════════════════
--
-- 돈과 소유권이 함께 움직이는 곳이라 한 줄씩 두드려 본다. "아마 맞을
-- 것이다"로 넘어가면, 틀렸을 때 나타나는 곳은 남의 잔고다.

reset role;

-- 남의 잔고도 봐야 한다 (원장은 제 것만 보이게 막혀 있다).
-- 검사 안에서만 쓰는 뒷문이고, Supabase 에는 올라가지 않는다.
create or replace function pg_temp.bal(p_user uuid) returns numeric
language sql security definer as $$
  select coalesce(sum(amount), 0)::numeric(20,4) from public.sup_ledger where user_id = p_user
$$;

-- economy 스키마는 앱 역할에 열려 있지 않다 (열 이유가 없다). 검사에서만 읽는다.
create or replace function pg_temp.cap() returns int
language sql security definer as $$ select economy.market_import_cap() $$;

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
    (select owner_id from public.market_sneakers where id = v_sn) = v_buyer,
    '신발의 주인이 바뀐다');
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
  v_low := public.market_import(2, 'FIRE', 'RARE', 0, 3, 1, 1, 100);
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
    (select owner_id from public.market_sneakers where id = v_sn) = v_buyer,
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
    (select owner_id from public.market_sneakers where id = v_sn)
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
  v_sn := public.market_import(9, 'WIND', 'EPIC', 2, 12, 1, 1, 100);
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
    (select owner_id from public.market_sneakers where id = pg_temp.fx('sn_wind')::bigint) = v_me,
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
  insert into public.posts (author_id, category, title, body)
  values ('11111111-1111-1111-1111-111111111111', 'FREE', '신고받을 글', '...')
  returning id into v_post;
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
  perform pg_temp.ok(
    (select count(*) from public.course_feed where id = pg_temp.fx('course')::bigint) = 0,
    '내 코스를 내리면 게시판에서 사라진다');
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
  -- 사흘치 걸음. 하루 상한(48,000)을 넘긴 날은 상한까지만 들어간다.
  perform public.steps_sync(json_build_array(
    json_build_object('epoch_day', v_today, 'steps', 30000, 'goal', 8000),
    json_build_object('epoch_day', v_today - 1, 'steps', 99999, 'goal', 8000),
    json_build_object('epoch_day', v_today - 40, 'steps', 40000, 'goal', 8000)
  ));
  perform pg_temp.ok(
    public.event_progress('step_surge', 'Asia/Seoul') = 78000,
    '올린 걸음은 하루 상한까지, 30일보다 오래된 날은 버린다');

  -- 걸음은 줄지 않는다
  perform public.steps_sync(json_build_array(
    json_build_object('epoch_day', v_today, 'steps', 100, 'goal', 8000)));
  perform pg_temp.ok(
    public.event_progress('step_surge', 'Asia/Seoul') = 78000,
    '같은 날 더 적은 걸음을 올려도 줄지 않는다');

  perform public.steps_sync(json_build_array(
    json_build_object('epoch_day', v_today, 'steps', 32000, 'goal', 8000)));
  perform pg_temp.ok(
    public.event_claim('step_surge', 'Asia/Seoul') = 250,
    '목표를 채우면 주간 도전 보상이 나온다');
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
  perform pg_temp.ok(
    (select count(*) from public.event_claims) = 2,
    '받은 기록은 본인이 읽을 수 있다');
end $$;
reset role;

do $$
begin
  perform pg_temp.ok(
    (select sum(amount) from public.sup_ledger
      where user_id = '44444444-4444-4444-4444-444444444444' and kind = 'EARN_EVENT') = 550,
    '두 보상이 서버 원장에 EARN_EVENT 로 적힌다');
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

\echo ''
\echo '════════════════════════════════════════════════════════════════'
\echo ' 전부 통과했습니다.'
\echo '════════════════════════════════════════════════════════════════'
