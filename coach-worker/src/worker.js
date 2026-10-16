import { inputProblem } from './input-guard.js'
import { ProviderError, providerFor } from './providers.js'

/**
 * 한 바퀴 — 작업을 빌려 오고(coach_worker_claim), 하나씩 공급자에게 보내 결과를 돌려준다.
 *
 * 서버가 보내기 직전에 동의 · 예산을 다시 보고 작업을 빌려 준다. 결과의 형식 · id · 숫자 · 언어 · 금지 문구는 서버
 * (coach_worker_complete)가 검사해 통과한 것만 저장한다 — 검사 규칙의 정본은 서버 하나다. 여기서는 보내기 전에
 * 입력의 모양만 한 번 더 본다(input-guard).
 *
 * 실패의 갈래(지시서 7 · 8절):
 *   429 · 5xx · 시간 초과 · 끊긴 응답(truncated) · 깨진 JSON → 다시(같은 작업, 서버가 횟수를 센다)
 *   거절(refusal) · 승인되지 않은 공급자 · 입력 모양 이상 → 다시 하지 않는다
 * 네트워크가 끊긴 호출은 이미 과금됐을 수 있다 — 사용량을 알면 실패에도 함께 보낸다(원가 장부).
 *
 * @param {object} deps
 * @param {(fn: string, args: object) => Promise<any>} deps.rpc 서버 함수 부르기
 * @param {(name: string) => {generate: Function}} [deps.providers]
 * @param {number} [deps.limit] 한 바퀴에 빌릴 작업 수
 * @param {number} [deps.timeoutMs] 공급자 한 번의 시간 상한
 * @returns {Promise<Array<{job: number, outcome: string}>>}
 */
export async function runOnce({ rpc, providers = providerFor, limit = 5, timeoutMs = 20_000 }) {
  const jobs = (await rpc('coach_worker_claim', { p_limit: limit })) ?? []
  const outcomes = []
  for (const job of jobs) {
    const base = { p_job_id: job.job_id, p_attempt: job.attempt }
    const problem = inputProblem(job.input)
    if (problem) {
      const outcome = await rpc('coach_worker_fail', { ...base, p_error: `input_rejected: ${problem}`.slice(0, 80), p_retryable: false, p_usage: null })
      outcomes.push({ job: job.job_id, outcome })
      continue
    }
    let reply
    try {
      reply = await withTimeout(providers(job.provider).generate(job), timeoutMs)
      if (reply?.output === null || typeof reply?.output !== 'object') throw new ProviderError('bad_json', true, reply?.usage ?? null)
    } catch (error) {
      const failure = error instanceof ProviderError ? error : new ProviderError('provider_error', true)
      const outcome = await rpc('coach_worker_fail', { ...base, p_error: failure.code, p_retryable: failure.retryable, p_usage: failure.usage })
      outcomes.push({ job: job.job_id, outcome })
      continue
    }
    const outcome = await rpc('coach_worker_complete', { ...base, p_output: reply.output, p_usage: reply.usage ?? null })
    outcomes.push({ job: job.job_id, outcome })
  }
  return outcomes
}

/** 공급자 응답의 HTTP 상태를 실패 갈래로 — 실제 공급자를 붙일 때 쓴다 */
export function failureForStatus(status, usage = null) {
  if (status === 429) return new ProviderError('http_429', true, usage)
  if (status >= 500) return new ProviderError(`http_${status}`, true, usage)
  return new ProviderError(`http_${status}`, false, usage)
}

function withTimeout(promise, ms) {
  let timer
  return Promise.race([
    promise,
    new Promise((_, reject) => {
      timer = setTimeout(() => reject(new ProviderError('timeout', true)), ms)
    }),
  ]).finally(() => clearTimeout(timer))
}
