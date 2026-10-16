-- AI 코칭(0060)이 약속한 것을 실제로 지키는지 — 지시서 13절 "상태와 보안"을 앱 권한으로 두드린다.
--
--   supabase/tests/run.sh 가 schema_test.sql 다음에 돌린다(같은 데이터베이스, 새 세션).
--
-- 동의 · 소유권 · 같은 작업 다시 만들지 않기 · 한도 · 실패와 재시도 · 늦은 응답 · 동의 철회 · 삭제 · 출력 검사 · RLS ·
-- 계획 · 목표 적용 · 계정 삭제. 공급자는 모의(mock)만 쓴다 — 실제 AI 를 부르는 길은 이 검사에 없다.

\set ON_ERROR_STOP on
set client_min_messages = notice;

create or replace function pg_temp.ok(cond boolean, label text) returns void
language plpgsql as $$
begin
  if cond then raise notice '  OK   %', label; else raise exception 'FAIL  %', label; end if;
end $$;

create or replace procedure pg_temp.login(p_user uuid)
language plpgsql as $$
begin
  perform set_config('request.jwt.claim.sub', coalesce(p_user::text, ''), false);
  perform set_config('request.jwt.claims', '', false);
end $$;

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

create temp table fix (k text primary key, v text);
grant all on fix to authenticated, anon;
create or replace function pg_temp.fx(p_key text) returns text
language sql as $$ select v from fix where k = p_key $$;

\echo ''
\echo '── AI 코칭(0060) ──────────────────────────────────────────────'

-- 가 · 나 · 코칭 워커
insert into auth.users (id, email) values
  ('59000000-0000-0000-0000-00000000000a', 'coach-a@test'),
  ('59000000-0000-0000-0000-00000000000b', 'coach-b@test'),
  ('59000000-0000-0000-0000-0000000000ee', 'coach-worker@test')
on conflict (id) do nothing;

-- 가의 러닝 — 지난주(끝난 주): 월 3.24km 24:18(구간 432 · 446 · 472), 수 · 금(FLAGGED), 목 VOID. 그 전 주: 월 · 화(1km 미만) — 이어진 주 2.
-- 걸음 거리 = 걸음 × 0.762m 라 9km 를 딱 맞출 수 없다 — 11811 걸음(8999.98m), 4104초 → 456초/km
do $$
declare
  v_a uuid := '59000000-0000-0000-0000-00000000000a';
  v_b uuid := '59000000-0000-0000-0000-00000000000b';
  v_mon date := coaching.monday(coaching.today('Asia/Seoul')) - 7;
  v_id bigint;
  procedure_rows record;
begin
  insert into fix values ('week', v_mon::text), ('this_monday', (v_mon + 7)::text), ('next_monday', (v_mon + 14)::text);
  for procedure_rows in
    select * from (values
      ('r1', v_a, 0, 4252, 1458, 'CLEAN'),
      ('r2', v_a, 2, 2625, 960, 'CLEAN'),
      ('r3', v_a, 4, 4934, 1686, 'FLAGGED'),
      ('rvoid', v_a, 3, 3000, 1200, 'VOID'),
      ('rshort', v_a, -6, 1000, 400, 'CLEAN'),
      ('rold', v_a, -7, 3000, 1200, 'CLEAN'),
      ('b1', v_b, 0, 4000, 1500, 'CLEAN')) as t(k, who, day, steps, secs, verdict)
  loop
    insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict, track)
    values (procedure_rows.who,
            ((v_mon + procedure_rows.day)::timestamp + interval '7 hours') at time zone 'Asia/Seoul',
            ((v_mon + procedure_rows.day)::timestamp + interval '7 hours') at time zone 'Asia/Seoul' + make_interval(secs => procedure_rows.secs + 30),
            procedure_rows.secs, procedure_rows.steps, procedure_rows.steps * 0.762, procedure_rows.verdict,
            '37.123456,127.123456,1;37.124456,127.124456,2')
    returning id into v_id;
    insert into fix values (procedure_rows.k, v_id::text),
      (procedure_rows.k || '_at', (select started_at::text from public.walk_sessions where id = v_id));
  end loop;
end $$;

-- ── 권한 ────────────────────────────────────────────────────────────
do $$
begin
  perform pg_temp.ok(not has_function_privilege('anon', 'public.coaching_run(timestamptz, text, boolean)', 'execute'),
    '로그인하지 않으면 코칭을 부르지 못한다');
  perform pg_temp.ok(has_function_privilege('authenticated', 'public.coaching_run(timestamptz, text, boolean)', 'execute'),
    '로그인한 사람은 코칭을 부른다');
  perform pg_temp.ok(not has_function_privilege('authenticated', 'coaching.run_input(public.walk_sessions, int[], text)', 'execute')
                     and not has_function_privilege('authenticated', 'coaching.status_for(uuid, text, text, text, bigint, jsonb, boolean, text)', 'execute'),
    '안쪽 함수(입력 만들기 · 상태)는 앱이 바로 부르지 못한다');
  perform pg_temp.ok(not has_table_privilege('authenticated', 'public.coaching_jobs', 'select')
                     and not has_table_privilege('authenticated', 'public.coaching_costs', 'select'),
    '작업 큐 · 원가 장부는 앱 권한으로 못 본다');
  perform pg_temp.ok(not has_table_privilege('authenticated', 'public.coaching_results', 'insert')
                     and not has_table_privilege('authenticated', 'public.coaching_results', 'update'),
    '결과는 앱이 쓰거나 고치지 못한다');
end $$;

-- ── 꺼져 있을 때(처음 설치 값) ───────────────────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb;
begin
  v := public.coaching_state('Asia/Seoul');
  perform pg_temp.ok((v ->> 'enabled')::boolean = false and v ->> 'provider' = 'none' and v -> 'consent' = 'null'::jsonb,
    '처음에는 꺼져 있고(공급자 none) 동의가 없다');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true) ->> 'state' = 'no_consent',
    '미동의면 작업을 만들지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('r1_at')::timestamptz, array[432, 878, 1350]) = 'no_consent',
    '미동의면 구간도 받지 않는다');
end $$;
call pg_temp.must_fail($q$ select public.coaching_consent_grant('old-version') $q$, '옛 판 동의 문구로는 동의할 수 없다');
call pg_temp.must_fail($q$ select public.coaching_prefs_save('steady', 3, 25, true, false, 0) $q$, '동의 없이 자동 분석을 켤 수 없다');
call pg_temp.must_fail($q$ select public.coaching_prefs_save('fast', 3, 25, false, false, 0) $q$, '목록 밖 목표는 저장하지 않는다');
call pg_temp.must_fail($q$ select public.coaching_week(date '2026-10-06', 'Asia/Seoul', 'ko') $q$, '주의 시작이 월요일이 아니면 받지 않는다');
call pg_temp.must_fail($q$ select public.coaching_state('Mars/Olympus') $q$, '모르는 시간대는 받지 않는다');
call pg_temp.must_fail($q$ select public.coaching_run(now(), 'fr') $q$, '지원하지 않는 언어는 받지 않는다');
do $$
declare v jsonb; v2 jsonb;
begin
  v := public.coaching_consent_grant('draft-2026-10-16');
  v2 := public.coaching_consent_grant('draft-2026-10-16');
  perform pg_temp.ok((v ->> 'ok')::boolean and (v2 ->> 'ok')::boolean
                     and (select count(*) from public.coaching_consents where withdrawn_at is null) = 1,
    '동의는 지금 판으로 한 번 — 다시 눌러도 한 줄');
  perform pg_temp.ok((public.coaching_state('Asia/Seoul') -> 'consent' ->> 'current')::boolean, '동의가 지금 판이다');
  -- 동의했어도 꺼져 있으면 만들지 않는다
  perform public.coaching_splits_put(pg_temp.fx('r1_at')::timestamptz, array[432, 878, 1350]);
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true) ->> 'state' = 'disabled',
    '운영자가 켜기 전에는 동의해도 작업을 만들지 않는다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok(not exists (select 1 from public.coaching_jobs), '꺼진 동안 작업이 하나도 생기지 않았다');
end $$;

-- 시험용으로 켠다 — 모의 공급자, 사용자 하루 4개, 워커 계정
update public.economy_settings set value = 'true'::jsonb where key = 'coaching_enabled';
update public.economy_settings set value = '"mock"'::jsonb where key = 'coaching_provider';
update public.economy_settings set value = '4'::jsonb where key = 'coaching_user_daily_limit';
update public.economy_settings set value = '"59000000-0000-0000-0000-0000000000ee"'::jsonb where key = 'coaching_worker_user_id';

-- ── 구간 받기 — 서버의 그 러닝과 맞대어 본다 ─────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v_at timestamptz := pg_temp.fx('r2_at')::timestamptz;
begin
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('r1_at')::timestamptz, array[432, 878, 1350]) = 'exists',
    '같은 구간을 다시 보내도 그대로(꺼져 있을 때 받은 것)');
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('r1_at')::timestamptz, array[432, 879, 1350]) = 'conflict',
    '먼저 받은 구간을 바꾸지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(v_at, array[400, 900, 960]) = 'mismatch', '랩 수가 서버 걸음 거리의 정수 km 와 다르면 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(v_at, array[100, 960]) = 'out_of_range', '사람이 낼 수 없는 구간은 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(v_at, array[480, 970]) = 'mismatch', '서버 활성 시간을 넘는 랩은 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(v_at, array[480, 470]) = 'mismatch', '거꾸로 가는 랩은 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(v_at, array[480, 960]) = 'stored', '맞는 구간은 받는다');
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('rvoid_at')::timestamptz, array[400, 800, 1200]) = 'not_a_run', 'VOID 러닝의 구간은 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('rshort_at')::timestamptz, array[400]) = 'too_short', '1km 미만 러닝의 구간은 받지 않는다');
  perform pg_temp.ok(public.coaching_splits_put(now() - interval '1 year', array[400]) = 'not_found', '없는 러닝은 찾지 못한다');
end $$;
-- 나는 가의 러닝을 시작 시각으로 가리켜도 찾지 못한다(같은 시각에 나도 달렸다 — 나의 러닝이 걸린다)
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
begin
  perform public.coaching_consent_grant('draft-2026-10-16');
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('r2_at')::timestamptz, array[480, 960]) = 'not_found',
    '남의 러닝 시작 시각으로는 찾지 못한다');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r2_at')::timestamptz, 'ko', true) ->> 'state' = 'not_found',
    '남의 러닝을 분석해 달라고 할 수 없다');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', false) ->> 'reason' = 'no_splits',
    '같은 시각의 내 러닝(구간 없음)만 보인다 — 가의 구간이 섞이지 않는다');
end $$;

-- ── 기록 하나의 코칭 ─────────────────────────────────────────────────
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb; v2 jsonb;
begin
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('rvoid_at')::timestamptz, 'ko', true) ->> 'reason' = 'not_a_run',
    'VOID 러닝은 건너뛴다(판정은 바꾸지 않는다)');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('rshort_at')::timestamptz, 'ko', true) ->> 'reason' = 'too_short',
    '1km 미만은 건너뛴다');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r3_at')::timestamptz, 'ko', true) ->> 'reason' = 'no_splits',
    '구간이 없는 러닝은 AI 를 부르지 않는다(09)');
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'none', '묻기만 하면 작업을 만들지 않는다(자동 분석 꺼짐)');
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true);
  v2 := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'queued' and v2 ->> 'job_id' = v ->> 'job_id',
    '요청하면 작업 하나 — 다시 눌러도 같은 작업');
  insert into fix values ('job1', v ->> 'job_id');
end $$;
reset role;
do $$
declare j public.coaching_jobs;
begin
  select * into j from public.coaching_jobs where id = pg_temp.fx('job1')::bigint;
  perform pg_temp.ok(j.input -> 'run' ->> 'distance_m' = '3240' and j.input -> 'run' ->> 'duration_sec' = '1458'
                     and j.input -> 'run' ->> 'avg_pace_sec' = '450' and j.input -> 'run' -> 'splits_sec' = '[432, 446, 472]'::jsonb
                     and j.input -> 'run' -> 'partial' = '{"meters": 240, "seconds": 108}'::jsonb,
    '검수 숫자 — 3.24km 24:18, 450초/km, 구간 432 · 446 · 472, 남은 0.24km 108초');
  perform pg_temp.ok(j.input -> 'facts' @> '[{"id": "run.split_last_vs_first", "values": {"last_km": 3, "delta_sec": 40}}]'::jsonb,
    '근거 — 마지막 1km 가 첫 1km 보다 40초 느렸다(부분 구간과 견주지 않는다)');
  perform pg_temp.ok(j.input -> 'candidates' @> '[{"id": "pace.start_easier", "values": {"pace_sec": 450}}]'::jsonb,
    '후보 — 첫 1km 를 조금 천천히, 7분30초/km(코드가 만든 값)');
  perform pg_temp.ok(position('37.12' in j.input::text) = 0 and position('track' in j.input::text) = 0
                     and position('59000000' in j.input::text) = 0 and position('steps' in j.input::text) = 0
                     and position('verdict' in j.input::text) = 0,
    '공급자 입력에 좌표 · 경로 · 사용자 번호 · 걸음 · 판정이 없다');
  perform pg_temp.ok(j.fingerprint = md5(j.input::text || '|') and j.policy_version = 'policy-v1' and j.prompt_version = 'prompt-v1',
    '작업 키의 지문 · 정책 · 프롬프트 판은 서버가 정한다');
end $$;

-- ── 워커 ────────────────────────────────────────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
call pg_temp.must_fail($q$ select * from public.coach_worker_claim(5) $q$, '사용자는 작업을 가져가지 못한다');
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare
  c record;
  v_out jsonb := '{"schema_version":"coach-out-v1","kind":"run","observation_fact_id":"run.split_last_vs_first",
                   "explanation":"후반에 페이스가 느려졌어요.","next_action_candidate_id":"pace.start_easier",
                   "action_reason":"처음을 조금 천천히 시작하면 끝까지 편해요.","limitation_code":null}';
  v text;
begin
  select * into c from public.coach_worker_claim(1);
  perform pg_temp.ok(c.job_id = pg_temp.fx('job1')::bigint and c.attempt = 1 and c.provider = 'mock',
    '워커는 오래된 작업부터 하나 가져간다(시도 1)');
  perform pg_temp.ok(not exists (select 1 from public.coach_worker_claim(5) x where x.job_id = c.job_id),
    '빌려 간 작업은 다른 워커가 다시 가져가지 못한다');
  v := public.coach_worker_complete(c.job_id, 1, jsonb_set(v_out, '{observation_fact_id}', '"run.made_up"'), '{"provider":"mock"}');
  perform pg_temp.ok(v = 'invalid:unknown_fact', '입력에 없던 근거 id 는 받지 않는다');
end $$;
reset role;
update public.coaching_jobs set next_at = now() where id = pg_temp.fx('job1')::bigint;
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare
  c record;
  v_out jsonb := '{"schema_version":"coach-out-v1","kind":"run","observation_fact_id":"run.split_last_vs_first",
                   "explanation":"후반에 페이스가 느려졌어요.","next_action_candidate_id":"pace.start_easier",
                   "action_reason":"처음을 조금 천천히 시작하면 끝까지 편해요.","limitation_code":null}';
begin
  select * into c from public.coach_worker_claim(1) x where x.job_id = pg_temp.fx('job1')::bigint;
  perform pg_temp.ok(c.attempt = 2, '실패한 작업은 같은 작업으로 다시(시도 2)');
  perform pg_temp.ok(public.coach_worker_complete(c.job_id, 2, jsonb_set(v_out, '{explanation}', '"40초 느려졌어요."'), null)
                     = 'invalid:number_in_text', 'AI 글에 숫자가 있으면 받지 않는다(숫자는 코드가 그린다)');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select status = 'QUEUED' and attempts = 2 and next_at > now() and error_code = 'invalid_output:number_in_text'
                        from public.coaching_jobs where id = pg_temp.fx('job1')::bigint),
    '잘못된 응답은 성공이 아니다 — 미뤄서 다시(까닭을 남긴다)');
end $$;
update public.coaching_jobs set next_at = now() where id = pg_temp.fx('job1')::bigint;
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare
  c record;
  v_out jsonb := '{"schema_version":"coach-out-v1","kind":"run","observation_fact_id":"run.split_last_vs_first",
                   "explanation":"후반에 페이스가 느려졌어요.","next_action_candidate_id":"pace.start_easier",
                   "action_reason":"처음을 조금 천천히 시작하면 끝까지 편해요.","limitation_code":null}';
begin
  select * into c from public.coach_worker_claim(1) x where x.job_id = pg_temp.fx('job1')::bigint;
  perform pg_temp.ok(public.coach_worker_complete(c.job_id, 2, v_out, null) = 'stale',
    '늦게 온 옛 시도의 답은 버린다');
  perform pg_temp.ok(public.coach_worker_complete(c.job_id, 3, v_out, '{"provider":"mock","input_tokens":0}') = 'stored',
    '검사를 통과한 답만 저장한다(시도 3)');
end $$;

-- 출력 검사 — 저장하지 않고 까닭만 본다
reset role;
do $$
declare
  v_in jsonb := (select input from public.coaching_jobs where id = pg_temp.fx('job1')::bigint);
  v_ok jsonb := '{"schema_version":"coach-out-v1","kind":"run","observation_fact_id":"run.avg_pace",
                  "explanation":"고르게 달렸어요.","next_action_candidate_id":null,"action_reason":null,"limitation_code":null}';
begin
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok) is null, '맞는 답은 통과한다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"next_action_candidate_id":"pace.free_form"}') = 'unknown_candidate',
    '허용 후보 밖의 행동은 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"explanation":"부상 위험이 있어요."}') = 'forbidden_term',
    '진단 · 부상 문구는 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"explanation":"SUP 보상을 더 받아요."}') = 'forbidden_term',
    '보상 문구는 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"explanation":"You slowed down."}') = 'wrong_language',
    '언어가 다르면 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"explanation":"<b>좋아요</b>"}') = 'markup_in_text',
    '글에 꺾쇠 · 링크를 넣지 못한다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"model":"x"}') = 'unknown_field', '모르는 칸은 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"schema_version":"coach-out-v2"}') = 'schema_version', '다른 형식 판은 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || '{"action_reason":"그냥 쉬어요."}') = 'reason_without_action',
    '행동 없이 이유만 있는 답은 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, v_ok || jsonb_build_object('explanation', repeat('가', 81))) = 'text_too_long',
    '긴 글은 받지 않는다');
  perform pg_temp.ok(coaching.output_problem(v_in, 'null'::jsonb) = 'not_object', '깨진 답(객체 아님)은 받지 않는다');
  perform pg_temp.ok(coaching.text_problem('Keep an easy start.', 'en') is null and coaching.text_problem('ゆっくり始めましょう。', 'ja') is null
                     and coaching.text_problem('慢慢开始吧。', 'zh') is null and coaching.text_problem('ゆっくり', 'zh') = 'wrong_language',
    '언어 검사 — 영어 · 일본어 · 중국어');
  perform pg_temp.ok((select count(*) from public.coaching_costs where job_id = pg_temp.fx('job1')::bigint) = 4,
    '원가는 시도마다 적는다(잘못된 응답 · 늦은 답 포함)');
end $$;

-- 결과 읽기 · 목표 페이스 적용(16)
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb; p jsonb; p2 jsonb;
begin
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'ready' and v -> 'result' ->> 'fact_id' = 'run.split_last_vs_first'
                     and v -> 'result' ->> 'candidate_id' = 'pace.start_easier' and v -> 'result' ->> 'provider' = 'mock'
                     and not (v ->> 'stale')::boolean,
    '다시 들어와도 저장된 결과를 읽는다(새로 만들지 않는다)');
  insert into fix values ('result1', v -> 'result' ->> 'id');
  p := public.coaching_apply_pace((v -> 'result' ->> 'id')::bigint, 0);
  perform pg_temp.ok((p ->> 'ok')::boolean and (p -> 'prefs' ->> 'target_pace_sec')::int = 450, '목표 페이스 적용 — 후보 값 그대로(7분30초)');
  p2 := public.coaching_apply_pace((v -> 'result' ->> 'id')::bigint, 0);
  perform pg_temp.ok((p2 ->> 'ok')::boolean and p2 -> 'prefs' ->> 'revision' = p -> 'prefs' ->> 'revision',
    '연타해도 한 번만 적용된다');
  perform pg_temp.ok(public.coaching_prefs_save('steady', 3, 25, false, false, 0) ->> 'conflict' = 'true',
    '다른 기기가 먼저 바꿨으면(판 다름) 저장하지 않는다');
  p := public.coaching_prefs_save('steady', 3, 25, true, false, (p -> 'prefs' ->> 'revision')::bigint);
  perform pg_temp.ok((p ->> 'ok')::boolean and (p -> 'prefs' ->> 'target_pace_sec')::int = 450
                     and (p -> 'prefs' ->> 'auto_analysis')::boolean,
    '선호 저장은 적용한 목표 페이스를 지우지 않는다');
  insert into fix values ('rev', p -> 'prefs' ->> 'revision');
end $$;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
call pg_temp.must_fail(format($q$ select public.coaching_apply_pace(%s, 0) $q$, pg_temp.fx('result1')),
  '남의 결과로 목표를 적용할 수 없다');
do $$ begin
  perform pg_temp.ok(not exists (select 1 from public.coaching_results), '남의 결과는 보이지 않는다(RLS)');
end $$;

-- ── 계획(13) ─────────────────────────────────────────────────────────
reset role;
do $$ begin
  perform pg_temp.ok(coaching.week_plan(date '2026-10-19', 3, 25) =
    '[{"date":"2026-10-19","template":"easy_25"},{"date":"2026-10-21","template":"walk_run_25"},{"date":"2026-10-23","template":"easy_25"}]'::jsonb,
    '주 3회 계획은 월 · 수 · 금, 편안한 러닝과 걷기와 달리기를 번갈아(앱 weekPlan 과 같다)');
  perform pg_temp.ok(jsonb_array_length(coaching.week_plan(date '2026-10-19', 2, 15)) = 2
                     and jsonb_array_length(coaching.week_plan(date '2026-10-19', 4, 40)) = 4,
    '주 2회 · 4회 계획');
end $$;
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
call pg_temp.must_fail(format($q$ select public.coaching_apply_plan('%s'::date, 'Asia/Seoul', %s) $q$, pg_temp.fx('week'), pg_temp.fx('rev')),
  '지난 주에는 계획을 적용하지 않는다');
do $$
declare v jsonb; v_next date := pg_temp.fx('next_monday')::date;
begin
  v := public.coaching_apply_plan(v_next, 'Asia/Seoul', pg_temp.fx('rev')::bigint);
  perform pg_temp.ok((v ->> 'ok')::boolean and jsonb_array_length(v -> 'days') = 3
                     and v -> 'days' -> 0 ->> 'template' = 'easy_25',
    '다음 주 계획 적용 — 서버가 지금 선호로 만든다');
  perform pg_temp.ok(jsonb_array_length(public.coaching_state('Asia/Seoul') -> 'plans') = 1, '적용한 계획이 상태에 보인다');
end $$;

-- ── 주간(04) · 추천(01) ──────────────────────────────────────────────
do $$
declare v jsonb; v_mon date := pg_temp.fx('week')::date;
begin
  perform pg_temp.ok(public.coaching_week(pg_temp.fx('this_monday')::date, 'Asia/Seoul', 'ko', true) ->> 'reason' = 'week_in_progress',
    '진행 중인 주는 분석하지 않는다');
  perform pg_temp.ok(public.coaching_week(pg_temp.fx('next_monday')::date, 'Asia/Seoul', 'ko', true) ->> 'reason' = 'future_week',
    '미래 주는 분석하지 않는다');
  perform pg_temp.ok(public.coaching_week(v_mon - 14, 'Asia/Seoul', 'ko', true) ->> 'reason' = 'no_runs',
    '러닝 없는 주는 AI 를 부르지 않는다');
  v := public.coaching_week(v_mon, 'Asia/Seoul', 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'queued', '끝난 주는 요청할 수 있다');
  insert into fix values ('week_job', v ->> 'job_id');
  v := public.coaching_recommend('Asia/Seoul', 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'queued', '선호가 있으면 다음 훈련 추천을 요청할 수 있다');
  insert into fix values ('rec_job', v ->> 'job_id');
  v := public.coaching_week(v_mon, 'UTC', 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'none', '시간대가 바뀌면 입력이 바뀌어 다른 작업이다');
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'en', true);
  perform pg_temp.ok(v ->> 'state' = 'queued' and v ->> 'job_id' <> pg_temp.fx('job1'), '언어가 다르면 다른 작업(언어별로 따로 둔다)');
  insert into fix values ('job_en', v ->> 'job_id');
end $$;
reset role;
do $$
declare w jsonb := (select input from public.coaching_jobs where id = pg_temp.fx('week_job')::bigint);
        r jsonb := (select input from public.coaching_jobs where id = pg_temp.fx('rec_job')::bigint);
begin
  perform pg_temp.ok(w -> 'week' ->> 'runs' = '3' and w -> 'week' ->> 'duration_sec' = '4104' and w -> 'week' ->> 'avg_pace_sec' = '456',
    '검수 숫자 — 9km 1:08:24 는 456초/km(가중 평균, VOID 는 빼고 3회)');
  perform pg_temp.ok(w -> 'facts' @> '[{"id": "week.streak", "values": {"weeks": 2}}]'::jsonb
                     and w -> 'candidates' @> '[{"id": "plan.next_week", "values": {"runs": 3, "minutes": 25}}]'::jsonb,
    '주간 근거 — 이어진 주 2, 다음 주 계획 후보는 고른 횟수 · 시간');
  perform pg_temp.ok(r -> 'candidates' = '[{"id": "template.easy_25", "values": {}}, {"id": "template.walk_run_25", "values": {}}]'::jsonb,
    '추천 후보는 고른 시간의 검토된 템플릿만');
  perform pg_temp.ok(position('59000000' in w::text) = 0 and position('steady' in r::text) > 0
                     and position('Asia/Seoul' in w::text) = 0 and position('Asia/Seoul' in r::text) = 0
                     and position(pg_temp.fx('week') in w::text) = 0,
    '주간 · 추천 입력에 사용자 번호 · 시간대 · 날짜가 없고, 목표는 정해진 값(enum)만');
  perform pg_temp.ok((select fingerprint from public.coaching_jobs where id = pg_temp.fx('week_job')::bigint) <> md5(w::text),
    '시간대는 보내지 않지만 지문에는 든다(시간대가 바뀌면 다른 결과)');
end $$;
do $$ begin
  perform pg_temp.ok((select runs from coaching.week_totals('59000000-0000-0000-0000-00000000000a', pg_temp.fx('week')::date - 7, 'Asia/Seoul')) = 2,
    '주간 횟수는 VOID 만 뺀다(1km 미만 러닝 · FLAGGED 도 러닝이다)');
end $$;

-- ── 한도(12) ────────────────────────────────────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb;
begin
  -- 오늘 만든 작업: job1 · job_en · week · rec = 4 → 다음 새 작업은 한도
  perform public.coaching_splits_put(pg_temp.fx('r2_at')::timestamptz, array[480, 960]);
  v := public.coaching_run(pg_temp.fx('r2_at')::timestamptz, 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'limit' and v ->> 'reason' = 'user_daily' and v ->> 'reset_at' is not null,
    '사용자 하루 한도에 닿으면 새로 만들지 않는다(다시 열리는 시각을 알려 준다)');
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'ready', '한도여도 저장된 분석은 읽는다');
end $$;

-- ── 실패와 재시도(11) ───────────────────────────────────────────────
reset role;
update public.coaching_jobs set next_at = now() + interval '1 hour'
 where id in (pg_temp.fx('week_job')::bigint, pg_temp.fx('rec_job')::bigint);
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare c record;
begin
  select * into c from public.coach_worker_claim(1);
  perform pg_temp.ok(c.job_id = pg_temp.fx('job_en')::bigint, '워커가 en 작업을 가져갔다');
  perform pg_temp.ok(public.coach_worker_fail(c.job_id, c.attempt, 'http_429', true, null) = 'requeued', '429 는 미뤄서 다시');
end $$;
reset role;
update public.coaching_jobs set next_at = now() where id = pg_temp.fx('job_en')::bigint;
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare c record;
begin
  select * into c from public.coach_worker_claim(1);
  perform pg_temp.ok(c.job_id = pg_temp.fx('job_en')::bigint and c.attempt = 2, '미룬 작업을 같은 작업으로 다시 가져갔다');
  perform pg_temp.ok(public.coach_worker_fail(c.job_id, c.attempt, 'refusal', false, null) = 'failed', '거절 응답은 다시 하지 않는다');
end $$;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb;
begin
  v := public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'en', false);
  perform pg_temp.ok(v ->> 'state' = 'failed' and (v ->> 'retryable')::boolean, '실패 — 시도가 남았으면 다시 할 수 있다고 알린다');
  v := public.coaching_job_retry(pg_temp.fx('job_en')::bigint);
  perform pg_temp.ok(v ->> 'state' = 'queued' and v ->> 'job_id' = pg_temp.fx('job_en'), '다시 시도는 같은 작업으로');
end $$;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$ begin
  perform pg_temp.ok(public.coaching_job_retry(pg_temp.fx('job_en')::bigint) ->> 'state' = 'not_found', '남의 작업은 다시 시도하지 못한다');
end $$;

-- ── 동의 철회(15) ───────────────────────────────────────────────────
reset role;
update public.coaching_jobs set next_at = now() + interval '1 hour' where id = pg_temp.fx('job_en')::bigint;
update public.coaching_jobs set next_at = now() where id = pg_temp.fx('week_job')::bigint;
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare c record;
begin
  select * into c from public.coach_worker_claim(1);
  perform pg_temp.ok(c.job_id = pg_temp.fx('week_job')::bigint, '워커가 주간 작업을 가져갔다(해제 직전)');
  insert into fix values ('week_attempt', c.attempt::text);
end $$;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb;
begin
  v := public.coaching_consent_withdraw(false);
  perform pg_temp.ok((v ->> 'cancelled')::int = 3 and (v ->> 'deleted_results')::int = 0,
    '해제하면 대기 · 진행 작업을 취소한다(분석 삭제는 고르지 않음)');
  perform pg_temp.ok(public.coaching_run(pg_temp.fx('r1_at')::timestamptz, 'ko', true) ->> 'state' = 'no_consent',
    '해제 뒤에는 새 분석을 만들지 않는다');
  perform pg_temp.ok(not (public.coaching_state('Asia/Seoul') -> 'prefs' ->> 'auto_analysis')::boolean,
    '해제하면 자동 분석도 꺼진다');
end $$;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$ begin
  perform pg_temp.ok(public.coach_worker_complete(pg_temp.fx('week_job')::bigint, pg_temp.fx('week_attempt')::int,
    '{"schema_version":"coach-out-v1","kind":"weekly","observation_fact_id":"week.streak","explanation":"꾸준히 이어가고 있어요.",
      "next_action_candidate_id":null,"action_reason":null,"limitation_code":null}', null) = 'cancelled',
    '해제 직전에 가져간 작업의 답도 저장하지 않는다');
  perform pg_temp.ok(not exists (select 1 from public.coach_worker_claim(5)), '해제한 사람의 작업은 가져가지 않는다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok((select count(*) from public.coaching_results where user_id = '59000000-0000-0000-0000-00000000000a') = 1
                     and exists (select 1 from public.coaching_run_splits where session_id = pg_temp.fx('r1')::bigint)
                     and exists (select 1 from public.walk_sessions where id = pg_temp.fx('r1')::bigint),
    '삭제를 고르지 않으면 저장된 분석은 남고, 러닝 · 구간은 어느 쪽이든 그대로');
end $$;
-- 다시 동의하면 취소된 같은 작업을 처음부터
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000a');
do $$
declare v jsonb;
begin
  perform public.coaching_consent_grant('draft-2026-10-16');
  v := public.coaching_week(pg_temp.fx('week')::date, 'Asia/Seoul', 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'none', '취소된 작업은 다시 요청하기 전에는 대기열에 없다');
  v := public.coaching_consent_withdraw(true);
  perform pg_temp.ok((v ->> 'deleted_results')::int = 1, '해제하며 삭제를 고르면 저장된 분석을 지운다');
end $$;
reset role;
do $$ begin
  perform pg_temp.ok(not exists (select 1 from public.coaching_results where user_id = '59000000-0000-0000-0000-00000000000a')
                     and not exists (select 1 from public.coaching_jobs where user_id = '59000000-0000-0000-0000-00000000000a')
                     and exists (select 1 from public.coaching_costs where job_id is null),
    '분석 · 입력은 지우고, 원가 장부는 사람 정보 없이 남는다');
  perform pg_temp.ok((select count(*) from public.coaching_consents where user_id = '59000000-0000-0000-0000-00000000000a') = 2,
    '동의 · 해제 이력은 남는다');
end $$;

-- ── 오래된 결과 · 꺼짐 ──────────────────────────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
declare v jsonb; c record;
begin
  perform public.coaching_splits_put(pg_temp.fx('b1_at')::timestamptz, array[440, 880, 1320, 1440]);
  perform pg_temp.ok(public.coaching_splits_put(pg_temp.fx('b1_at')::timestamptz, array[440, 880, 1320]) = 'stored',
    '나의 3km 러닝 구간');
  v := public.coaching_run(pg_temp.fx('b1_at')::timestamptz, 'ko', true);
  insert into fix values ('b_job', v ->> 'job_id');
end $$;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare c record;
begin
  select * into c from public.coach_worker_claim(5) x where x.job_id = pg_temp.fx('b_job')::bigint;
  perform pg_temp.ok(public.coach_worker_complete(c.job_id, c.attempt,
    '{"schema_version":"coach-out-v1","kind":"run","observation_fact_id":"run.avg_pace","explanation":"고르게 달렸어요.",
      "next_action_candidate_id":"pace.keep","action_reason":"지금처럼 편하게 이어가요.","limitation_code":null}', null) = 'stored',
    '나의 결과 저장');
end $$;
reset role;
update public.economy_settings set value = '"prompt-v2"'::jsonb where key = 'coaching_prompt_version';
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
declare v jsonb;
begin
  v := public.coaching_run(pg_temp.fx('b1_at')::timestamptz, 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'none' and not (v ->> 'stale')::boolean and v -> 'result' ->> 'fact_id' = 'run.avg_pace',
    '프롬프트 판이 바뀌면 새로 요청할 수 있고, 입력이 같은 옛 결과는 그대로 읽는다');
end $$;
reset role;
update public.economy_settings set value = '"prompt-v1"'::jsonb where key = 'coaching_prompt_version';
-- 주간 결과 뒤에 그 주의 러닝이 늦게 올라오면(입력이 바뀌면) 옛 결과는 "오래된 결과"다
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
declare v jsonb;
begin
  v := public.coaching_week(pg_temp.fx('week')::date, 'Asia/Seoul', 'ko', true);
  insert into fix values ('b_week_job', v ->> 'job_id');
end $$;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$
declare c record;
begin
  select * into c from public.coach_worker_claim(1);
  perform pg_temp.ok(c.job_id = pg_temp.fx('b_week_job')::bigint and public.coach_worker_complete(c.job_id, c.attempt,
    '{"schema_version":"coach-out-v1","kind":"weekly","observation_fact_id":"week.runs","explanation":"한 주를 잘 마쳤어요.",
      "next_action_candidate_id":null,"action_reason":null,"limitation_code":null}', null) = 'stored',
    '나의 주간 결과 저장');
end $$;
reset role;
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict)
values ('59000000-0000-0000-0000-00000000000b',
        ((pg_temp.fx('week')::date + 2)::timestamp + interval '18 hours') at time zone 'Asia/Seoul',
        ((pg_temp.fx('week')::date + 2)::timestamp + interval '18 hours') at time zone 'Asia/Seoul' + interval '20 minutes',
        1100, 2000, 1524, 'CLEAN');
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
declare v jsonb;
begin
  v := public.coaching_week(pg_temp.fx('week')::date, 'Asia/Seoul', 'ko', false);
  perform pg_temp.ok(v ->> 'state' = 'none' and (v ->> 'stale')::boolean and v -> 'result' ->> 'fact_id' = 'week.runs',
    '입력이 바뀌면 옛 결과는 "오래된 결과"로 표시한다(새 기록을 분석한 결과인 척하지 않는다)');
end $$;
reset role;
update public.economy_settings set value = 'false'::jsonb where key = 'coaching_enabled';
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$
declare v jsonb;
begin
  v := public.coaching_run(pg_temp.fx('b1_at')::timestamptz, 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'ready' and v -> 'result' ->> 'fact_id' = 'run.avg_pace',
    '긴급 중지에도 이미 만든 결과는 읽는다');
  v := public.coaching_week(pg_temp.fx('week')::date, 'Asia/Seoul', 'ko', true);
  perform pg_temp.ok(v ->> 'state' = 'disabled' and v -> 'result' ->> 'fact_id' = 'week.runs' and (v ->> 'stale')::boolean,
    '긴급 중지 — 새로 만들지 않고, 저장된(오래된) 결과는 오래된 결과로 보인다');
end $$;
call pg_temp.login('59000000-0000-0000-0000-0000000000ee');
do $$ begin
  perform pg_temp.ok(not exists (select 1 from public.coach_worker_claim(5)), '꺼져 있으면 워커도 가져가지 않는다');
end $$;
reset role;
update public.economy_settings set value = 'true'::jsonb where key = 'coaching_enabled';
update public.economy_settings set value = '"anthropic"'::jsonb where key = 'coaching_provider';
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$ begin
  perform pg_temp.ok(public.coaching_week(pg_temp.fx('week')::date, 'Asia/Seoul', 'ko', true) ->> 'reason' = 'service_budget',
    '실제 공급자는 월 예산 · 예약 값이 정해지기 전에는 만들지 않는다');
end $$;
reset role;
update public.economy_settings set value = '"mock"'::jsonb where key = 'coaching_provider';
update public.economy_settings set value = '"prompt-v1"'::jsonb where key = 'coaching_prompt_version';

-- ── 계정 삭제 ───────────────────────────────────────────────────────
set role authenticated;
call pg_temp.login('59000000-0000-0000-0000-00000000000b');
do $$ begin perform public.account_delete(); end $$;
reset role;
do $$ begin
  perform pg_temp.ok(not exists (select 1 from public.coaching_consents where user_id = '59000000-0000-0000-0000-00000000000b')
                     and not exists (select 1 from public.coaching_results where user_id = '59000000-0000-0000-0000-00000000000b')
                     and not exists (select 1 from public.coaching_run_splits where user_id = '59000000-0000-0000-0000-00000000000b')
                     and not exists (select 1 from public.coaching_preferences where user_id = '59000000-0000-0000-0000-00000000000b'),
    '계정을 지우면 코칭 동의 · 결과 · 구간 · 선호가 함께 사라진다');
end $$;

-- 처음 설치 값으로 되돌린다(다음 setup.sql 재적용 검사가 켜진 채로 돌지 않게)
update public.economy_settings set value = 'false'::jsonb where key = 'coaching_enabled';
update public.economy_settings set value = '"none"'::jsonb where key = 'coaching_provider';
update public.economy_settings set value = 'null'::jsonb where key in ('coaching_user_daily_limit', 'coaching_worker_user_id');

\echo ' AI 코칭 검사 통과'
