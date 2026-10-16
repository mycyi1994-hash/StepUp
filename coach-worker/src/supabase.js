/**
 * 서버 함수 부르기 — coach_worker_* 만. 이 워커의 DB 토큰으로는 표를 하나도 직접 읽지 못한다(service_role 을 쓰지 않는다).
 *
 *   COACH_DB_JWT 가 있으면 그것(stepup_coach 역할 토큰)을 쓰고, 없으면 코칭 워커 전용 계정(COACH_EMAIL / COACH_PASSWORD)으로
 *   로그인한다. 서버는 economy_settings.coaching_worker_user_id 에 적힌 계정만 통과시킨다. 어테스터 계정과 따로다 —
 *   서명 키와 AI 키를 한 상자에 두지 않는다.
 */

let cached = { token: null, until: 0 }

export function resetWorkerLogin() {
  cached = { token: null, until: 0 }
}

async function workerToken(env, fetchImpl) {
  if (env.COACH_DB_JWT) return env.COACH_DB_JWT
  if (cached.token && cached.until > Date.now()) return cached.token
  const res = await fetchImpl(`${env.SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { apikey: env.SUPABASE_ANON_KEY, 'content-type': 'application/json' },
    body: JSON.stringify({ email: String(env.COACH_EMAIL ?? '').trim(), password: String(env.COACH_PASSWORD ?? '').trim() }),
  })
  if (!res.ok) throw new Error(`coach worker login failed: ${res.status}`)
  const body = await res.json()
  cached = { token: body.access_token, until: Date.now() + Math.max(0, (body.expires_in ?? 0) - 60) * 1000 }
  return cached.token
}

export function makeRpc(env, fetchImpl = fetch) {
  return async function rpc(fn, args) {
    if (!/^coach_worker_[a-z_]+$/.test(fn)) throw new Error(`not a coach worker function: ${fn}`)
    const token = await workerToken(env, fetchImpl)
    const res = await fetchImpl(`${env.SUPABASE_URL}/rest/v1/rpc/${fn}`, {
      method: 'POST',
      headers: { apikey: env.SUPABASE_ANON_KEY, authorization: `Bearer ${token}`, 'content-type': 'application/json' },
      body: JSON.stringify(args),
    })
    // 서버의 오류 본문은 로그에만, 사용자 정보 없이(상태와 함수 이름)
    if (!res.ok) throw new Error(`${fn} failed: ${res.status}`)
    return res.json()
  }
}
