// 워커 한 바퀴를 실제 서버 함수(로컬 Postgres)에 붙여 본다 — supabase/tests/run.sh 가 coaching_test.sql 다음에 부른다.
//
//   node coach-worker/test/e2e-psql.mjs <psql 소켓 폴더> <포트> <데이터베이스>
//
// 모의 공급자의 고정 문장이 서버 검사(coach_worker_complete → coaching.output_problem)를 네 언어 · 세 종류 모두 통과해
// 저장되는지, 승인되지 않은 공급자(anthropic)는 보내지 않고 실패로 끝나는지 본다. 사람 · 러닝은 이 스크립트가 만들고 지운다.
import { execFileSync } from 'node:child_process'
import assert from 'node:assert/strict'
import { runOnce } from '../src/worker.js'

const [host, port, db] = process.argv.slice(2)
const USER = '59200000-0000-0000-0000-00000000000e'
const WORKER = '59200000-0000-0000-0000-0000000000ef'

function psql(sql) {
  return execFileSync('psql', ['-h', host, '-p', port, '-U', 'postgres', '-d', db, '-v', 'ON_ERROR_STOP=1', '-qAt', '-c', sql],
    { encoding: 'utf8' }).trim()
}

// 로그인한 사람으로 한 문장 — 결과는 JSON 한 줄
function as(user, sql) {
  const out = execFileSync('psql', ['-h', host, '-p', port, '-U', 'postgres', '-d', db, '-v', 'ON_ERROR_STOP=1', '-qAt',
    '-c', 'set role authenticated',
    '-c', `select set_config('request.jwt.claim.sub', '${user}', false)`,
    '-c', sql], { encoding: 'utf8' }).trim().split('\n')
  return out[out.length - 1]
}

const rpc = async (fn, args) => {
  const json = JSON.stringify(args).replaceAll("'", "''")
  if (fn === 'coach_worker_claim') {
    const rows = as(WORKER, `select coalesce(jsonb_agg(to_jsonb(c)), '[]'::jsonb) from public.coach_worker_claim((('${json}')::jsonb ->> 'p_limit')::int) c`)
    return JSON.parse(rows)
  }
  if (fn === 'coach_worker_complete') {
    return as(WORKER, `select public.coach_worker_complete((a->>'p_job_id')::bigint, (a->>'p_attempt')::int, a->'p_output', a->'p_usage') from (select '${json}'::jsonb a) x`)
  }
  if (fn === 'coach_worker_fail') {
    return as(WORKER, `select public.coach_worker_fail((a->>'p_job_id')::bigint, (a->>'p_attempt')::int, a->>'p_error', (a->>'p_retryable')::boolean, a->'p_usage') from (select '${json}'::jsonb a) x`)
  }
  throw new Error(fn)
}

psql(`
insert into auth.users (id, email) values ('${USER}', 'e2e-user@test'), ('${WORKER}', 'e2e-worker@test') on conflict (id) do nothing;
-- 코칭 거리는 러닝의 인정 거리(distance_meters, 0068) — 걸음 거리(걸음 × 0.762m)로 넣는다
insert into public.walk_sessions (user_id, started_at, ended_at, duration_sec, steps, distance_meters, verdict) values
  ('${USER}', timestamptz '2026-02-02 07:00+09', timestamptz '2026-02-02 07:30+09', 1458, 4252, 3240.024, 'CLEAN'),
  ('${USER}', timestamptz '2026-02-04 07:00+09', timestamptz '2026-02-04 07:30+09', 1290, 3937, 2999.994, 'CLEAN');
insert into public.coaching_consents (user_id, version) values ('${USER}', 'draft-2026-10-16');
insert into public.coaching_preferences (user_id, goal, runs_per_week, minutes) values ('${USER}', 'steady', 3, 25);
insert into public.coaching_run_splits (session_id, user_id, laps)
  select id, user_id, case when steps = 4252 then array[432, 878, 1350] else array[430, 860, 1290] end
    from public.walk_sessions where user_id = '${USER}';
update public.economy_settings set value = 'true'::jsonb where key = 'coaching_enabled';
update public.economy_settings set value = '"mock"'::jsonb where key = 'coaching_provider';
update public.economy_settings set value = '20'::jsonb where key = 'coaching_user_daily_limit';
update public.economy_settings set value = '"${WORKER}"'::jsonb where key = 'coaching_worker_user_id';
`)

try {
  // 네 언어 × (기록 둘 · 주간 · 추천) — 모두 요청해 둔다
  for (const locale of ['ko', 'en', 'ja', 'zh']) {
    for (const at of ['2026-02-02 07:00+09', '2026-02-04 07:00+09']) {
      assert.equal(JSON.parse(as(USER, `select public.coaching_run(timestamptz '${at}', '${locale}', true)`)).state, 'queued')
    }
    assert.equal(JSON.parse(as(USER, `select public.coaching_week(date '2026-02-02', 'Asia/Seoul', '${locale}', true)`)).state, 'queued')
    assert.equal(JSON.parse(as(USER, `select public.coaching_recommend('Asia/Seoul', '${locale}', true)`)).state, 'queued')
  }
  const outcomes = [...await runOnce({ rpc, limit: 20 })]
  assert.equal(outcomes.length, 16)
  assert.ok(outcomes.every((o) => o.outcome === 'stored'), JSON.stringify(outcomes))
  const run = JSON.parse(as(USER, `select public.coaching_run(timestamptz '2026-02-02 07:00+09', 'ja', false)`))
  assert.equal(run.state, 'ready')
  assert.equal(run.result.fact_id, 'run.split_last_vs_first')
  assert.equal(run.result.explanation, '後半にかけてペースが落ちました。')
  assert.equal(run.result.provider, 'mock')
  const even = JSON.parse(as(USER, `select public.coaching_run(timestamptz '2026-02-04 07:00+09', 'en', false)`))
  assert.equal(even.result.fact_id, 'run.split_even')

  // 승인되지 않은 공급자 — 작업을 만들 수 없어야 한다(예산 미정). 설정을 억지로 열어도 워커는 보내지 않는다
  psql(`update public.economy_settings set value = '"anthropic"'::jsonb where key = 'coaching_provider';
        update public.economy_settings set value = '1000000'::jsonb where key in ('coaching_monthly_budget_micro', 'coaching_reserve_micro');
        update public.economy_settings set value = '2000000'::jsonb where key = 'coaching_monthly_budget_micro';`)
  assert.equal(JSON.parse(as(USER, `select public.coaching_run(timestamptz '2026-02-02 07:00+09', 'ko', true)`)).state, 'ready')
  psql(`update public.economy_settings set value = '"prompt-e2e"'::jsonb where key = 'coaching_prompt_version'`)
  assert.equal(JSON.parse(as(USER, `select public.coaching_run(timestamptz '2026-02-02 07:00+09', 'ko', true)`)).state, 'queued')
  const refused = await runOnce({ rpc, limit: 5 })
  assert.deepEqual(refused.map((o) => o.outcome), ['failed'])
  assert.equal(psql(`select error_code from public.coaching_jobs where user_id = '${USER}' and prompt_version = 'prompt-e2e'`), 'provider_not_approved')
  console.log('  OK   워커 한 바퀴 — 모의 문장 16개가 서버 검사를 통과해 저장되고, 승인되지 않은 공급자는 보내지 않는다')
} finally {
  psql(`delete from auth.users where id in ('${USER}', '${WORKER}');
        update public.economy_settings set value = 'false'::jsonb where key = 'coaching_enabled';
        update public.economy_settings set value = '"none"'::jsonb where key = 'coaching_provider';
        update public.economy_settings set value = '"prompt-v1"'::jsonb where key = 'coaching_prompt_version';
        update public.economy_settings set value = 'null'::jsonb
         where key in ('coaching_user_daily_limit', 'coaching_worker_user_id', 'coaching_monthly_budget_micro', 'coaching_reserve_micro');`)
}
