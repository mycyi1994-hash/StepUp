import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { inputProblem } from '../src/input-guard.js'
import { ProviderError, mockProvider, providerFor } from '../src/providers.js'
import { failureForStatus, runOnce } from '../src/worker.js'

const SQL = fs.readFileSync(new URL('../../supabase/migrations/0060_coaching.sql', import.meta.url), 'utf8')

// 서버(0060 coaching.run_input)가 검수 숫자로 만드는 입력 — supabase/tests/coaching_test.sql 이 같은 값을 확인한다
const RUN_INPUT = {
  kind: 'run',
  locale: 'ko',
  schema_version: 'coach-out-v1',
  run: { distance_m: 3240, duration_sec: 1458, avg_pace_sec: 450, splits_sec: [432, 446, 472], partial: { meters: 240, seconds: 108 } },
  facts: [
    { id: 'run.avg_pace', values: { pace_sec: 450 } },
    { id: 'run.split_last_vs_first', values: { last_km: 3, delta_sec: 40 } },
  ],
  candidates: [
    { id: 'pace.start_easier', values: { pace_sec: 450 } },
    { id: 'pace.keep', values: { pace_sec: 450 } },
  ],
}
const WEEK_INPUT = {
  kind: 'weekly',
  locale: 'en',
  schema_version: 'coach-out-v1',
  week: { runs: 3, distance_m: 9000, duration_sec: 4104, avg_pace_sec: 456 },
  goal: { goal: 'steady', runs_per_week: 3, minutes: 25 },
  facts: [{ id: 'week.runs', values: { runs: 3, goal: 3 } }, { id: 'week.streak', values: { weeks: 4 } }],
  candidates: [{ id: 'plan.next_week', values: { runs: 3, minutes: 25 } }],
}
const REC_INPUT = {
  kind: 'recommendation',
  locale: 'ja',
  schema_version: 'coach-out-v1',
  week: { runs_so_far: 2 },
  goal: { goal: 'steady', runs_per_week: 3, minutes: 25 },
  facts: [{ id: 'week.runs', values: { runs: 2, goal: 3 } }],
  candidates: [{ id: 'template.easy_25', values: {} }, { id: 'template.walk_run_25', values: {} }],
}

/** 가짜 서버 — 빌려 줄 작업과 받은 부름을 적는다 */
function fakeServer(jobs) {
  const calls = []
  const rpc = async (fn, args) => {
    calls.push({ fn, args })
    if (fn === 'coach_worker_claim') return jobs
    if (fn === 'coach_worker_complete') return 'stored'
    if (fn === 'coach_worker_fail') return args.p_retryable ? 'requeued' : 'failed'
    throw new Error(fn)
  }
  return { rpc, calls }
}

const job = (input, provider = 'mock', id = 1) => ({ job_id: id, kind: input.kind, locale: input.locale, input, attempt: 1, provider })

test('입력 문 — 서버가 만드는 모양은 통과한다(기록 · 주간 · 추천)', () => {
  assert.equal(inputProblem(RUN_INPUT), null)
  assert.equal(inputProblem(WEEK_INPUT), null)
  assert.equal(inputProblem(REC_INPUT), null)
})

test('입력 문 — 좌표 · 경로 · 사용자 번호 · 날짜 · 시간대 · 자유 글이 섞이면 보내지 않는다', () => {
  assert.match(inputProblem({ ...RUN_INPUT, track: '37.1,127.1,1' }), /track/)
  assert.match(inputProblem({ ...RUN_INPUT, run: { ...RUN_INPUT.run, user_id: 'x' } }), /user_id/)
  assert.match(inputProblem({ ...WEEK_INPUT, week: { ...WEEK_INPUT.week, tz: 'Asia/Seoul' } }), /tz/)
  assert.match(inputProblem({ ...WEEK_INPUT, week: { ...WEEK_INPUT.week, start: '2026-10-05' } }), /start/)
  assert.match(inputProblem({ ...RUN_INPUT, facts: [{ id: 'run.avg_pace', values: { pace_sec: 'fast' } }] }), /정수/)
  assert.match(inputProblem({ ...RUN_INPUT, facts: [{ id: 'run.avg_pace', values: { lat: 37.12345 } }] }), /정수/)
  assert.match(inputProblem({ ...RUN_INPUT, facts: [{ id: 'run.avg_pace', note: '메모', values: {} }] }), /허용하지 않는 칸/)
  assert.match(inputProblem({ ...WEEK_INPUT, goal: { ...WEEK_INPUT.goal, goal: '마라톤 우승' } }), /목표/)
  assert.match(inputProblem({ ...RUN_INPUT, locale: 'fr' }), /언어/)
})

test('모의 공급자 — 구간 비교 근거와 첫 후보를 고르고, 숫자 없는 고정 문장을 쓴다', async () => {
  const { output, usage } = await mockProvider.generate(job(RUN_INPUT))
  assert.equal(output.observation_fact_id, 'run.split_last_vs_first')
  assert.equal(output.next_action_candidate_id, 'pace.start_easier')
  assert.equal(output.explanation, '후반으로 갈수록 페이스가 느려졌어요.')
  assert.equal(usage.provider, 'mock')
  const weekly = (await mockProvider.generate(job(WEEK_INPUT))).output
  assert.equal(weekly.observation_fact_id, 'week.streak')
  assert.equal(weekly.next_action_candidate_id, 'plan.next_week')
  const rec = (await mockProvider.generate(job(REC_INPUT))).output
  assert.equal(rec.next_action_candidate_id, 'template.easy_25')
  assert.equal(rec.action_reason, 'ウォーキングで体をほぐしてから始めましょう。')
})

test('모의 문장은 모두 서버의 글 검사(0060)를 통과한다 — 숫자 · 길이 · 금지 문구 · 언어', async () => {
  const terms = SQL.match(/function coaching\.forbidden_terms\(\)[\s\S]*?array\[([\s\S]*?)\]\s*\$\$/)[1]
    .match(/'([^']+)'/g).map((t) => t.slice(1, -1))
  assert.ok(terms.length > 40)
  const script = {
    ko: (s) => /[가-힣]/.test(s),
    ja: (s) => /[ぁ-んァ-ン]/.test(s) && !/[가-힣]/.test(s),
    zh: (s) => /[一-鿿]/.test(s) && !/[ぁ-んァ-ン가-힣]/.test(s),
    en: (s) => /^[ -~]+$/.test(s) && /[A-Za-z]/.test(s),
  }
  for (const locale of ['ko', 'en', 'ja', 'zh']) {
    for (const input of [RUN_INPUT, WEEK_INPUT, REC_INPUT]) {
      const { output } = await mockProvider.generate(job({ ...input, locale }))
      for (const text of [output.explanation, output.action_reason]) {
        assert.ok(text && text.length <= 80, `${locale} 길이: ${text}`)
        assert.ok(!/[0-9０-９]/.test(text), `${locale} 숫자: ${text}`)
        assert.ok(!/[<>{}[\]\\`]|https?:/.test(text), `${locale} 꺾쇠: ${text}`)
        assert.ok(!terms.some((t) => text.toLowerCase().includes(t)), `${locale} 금지 문구: ${text}`)
        assert.ok(script[locale](text), `${locale} 언어: ${text}`)
      }
    }
  }
})

test('한 바퀴 — 빌린 작업을 보내고 결과를 돌려준다', async () => {
  const { rpc, calls } = fakeServer([job(RUN_INPUT)])
  const outcomes = await runOnce({ rpc })
  assert.deepEqual(outcomes, [{ job: 1, outcome: 'stored' }])
  const done = calls.find((c) => c.fn === 'coach_worker_complete')
  assert.equal(done.args.p_job_id, 1)
  assert.equal(done.args.p_attempt, 1)
  assert.equal(done.args.p_output.observation_fact_id, 'run.split_last_vs_first')
})

test('입력 모양이 다르면 공급자를 부르지 않고 실패로 끝낸다(다시 하지 않는다)', async () => {
  let called = false
  const { rpc, calls } = fakeServer([job({ ...RUN_INPUT, track: '37.1,127.1,1' })])
  const outcomes = await runOnce({ rpc, providers: () => ({ generate: async () => { called = true } }) })
  assert.equal(called, false)
  assert.deepEqual(outcomes, [{ job: 1, outcome: 'failed' }])
  const fail = calls.find((c) => c.fn === 'coach_worker_fail')
  assert.equal(fail.args.p_retryable, false)
  assert.match(fail.args.p_error, /^input_rejected/)
})

test('429 · 5xx · 시간 초과 · 깨진 JSON 은 다시, 거절은 다시 하지 않는다 — 과금됐을 수 있는 사용량도 보낸다', async () => {
  const cases = [
    [failureForStatus(429, { provider: 'x', input_tokens: 10 }), 'http_429', true],
    [failureForStatus(503), 'http_503', true],
    [failureForStatus(400), 'http_400', false],
    [new ProviderError('refusal', false, { provider: 'x', output_tokens: 3 }), 'refusal', false],
    [new ProviderError('truncated', true), 'truncated', true],
  ]
  for (const [error, code, retryable] of cases) {
    const { rpc, calls } = fakeServer([job(RUN_INPUT)])
    await runOnce({ rpc, providers: () => ({ generate: async () => { throw error } }) })
    const fail = calls.find((c) => c.fn === 'coach_worker_fail')
    assert.equal(fail.args.p_error, code)
    assert.equal(fail.args.p_retryable, retryable)
    assert.deepEqual(fail.args.p_usage, error.usage)
  }
  const { rpc, calls } = fakeServer([job(RUN_INPUT)])
  await runOnce({ rpc, providers: () => ({ generate: async () => ({ output: 'not json', usage: null }) }) })
  assert.equal(calls.find((c) => c.fn === 'coach_worker_fail').args.p_error, 'bad_json')
  const slow = fakeServer([job(RUN_INPUT)])
  await runOnce({ rpc: slow.rpc, timeoutMs: 20, providers: () => ({ generate: () => new Promise(() => {}) }) })
  const timeout = slow.calls.find((c) => c.fn === 'coach_worker_fail')
  assert.equal(timeout.args.p_error, 'timeout')
  assert.equal(timeout.args.p_retryable, true)
})

test('승인되지 않은 실제 공급자는 보내지 않는다 — 네트워크를 쓰지 않고 실패로 끝낸다', async () => {
  const realFetch = globalThis.fetch
  globalThis.fetch = () => { throw new Error('network used') }
  try {
    const { rpc, calls } = fakeServer([job(RUN_INPUT, 'anthropic')])
    const outcomes = await runOnce({ rpc })
    assert.deepEqual(outcomes, [{ job: 1, outcome: 'failed' }])
    assert.equal(calls.find((c) => c.fn === 'coach_worker_fail').args.p_error, 'provider_not_approved')
    assert.equal(providerFor('mock').name, 'mock')
  } finally {
    globalThis.fetch = realFetch
  }
})
