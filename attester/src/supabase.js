/**
 * Supabase 와 말하는 두 가지 길.
 *
 *   getUser     사용자가 보낸 로그인 토큰이 진짜인지 Supabase 에 물어본다.
 *               워커가 토큰을 직접 풀지 않는다 — 서명 방식이 바뀌어도 그대로 맞다.
 *   rpc         attester_* 함수만 부른다. 이 워커의 DB 토큰(역할 토큰 또는 전용 계정)으로는
 *               표를 하나도 직접 읽지 못한다. service_role 을 쓰지 않는다.
 */

export class HttpError extends Error {
  constructor(status, message) {
    super(message)
    this.status = status
  }
}

export async function getUser(env, request, fetchImpl = fetch) {
  const auth = request.headers.get('authorization') ?? ''
  const token = auth.startsWith('Bearer ') ? auth.slice(7) : ''
  if (!token) throw new HttpError(401, '로그인이 필요합니다')
  const res = await fetchImpl(`${env.SUPABASE_URL}/auth/v1/user`, {
    headers: { apikey: env.SUPABASE_ANON_KEY, authorization: `Bearer ${token}` },
  })
  if (!res.ok) throw new HttpError(401, '로그인이 만료되었습니다')
  const user = await res.json()
  if (!user?.id) throw new HttpError(401, '로그인이 만료되었습니다')
  return user
}

/**
 * 어테스터의 DB 토큰.
 *   ATTESTER_DB_JWT 가 있으면 그것(stepup_attester 역할 토큰)을 쓴다.
 *   없으면 어테스터 전용 계정(ATTESTER_EMAIL / ATTESTER_PASSWORD)으로 로그인한다.
 *   서버는 economy_settings.attester_user_id 에 적힌 그 계정만 통과시킨다.
 * 로그인 토큰은 만료 1분 전까지 이 워커 인스턴스 안에서 다시 쓴다.
 */
let cached = { token: null, until: 0 }

export async function attesterToken(env, fetchImpl = fetch) {
  if (env.ATTESTER_DB_JWT) return env.ATTESTER_DB_JWT
  const now = Date.now()
  if (cached.token && cached.until > now) return cached.token
  const res = await fetchImpl(`${env.SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { apikey: env.SUPABASE_ANON_KEY, 'content-type': 'application/json' },
    body: JSON.stringify({ email: env.ATTESTER_EMAIL, password: env.ATTESTER_PASSWORD }),
  })
  if (!res.ok) throw new HttpError(502, '어테스터 계정으로 로그인하지 못했습니다')
  const body = await res.json()
  cached = { token: body.access_token, until: now + (Number(body.expires_in ?? 3600) - 60) * 1000 }
  return cached.token
}

/** attester_* 함수 호출. 실패하면 서버가 준 이유를 그대로 올린다. */
export async function rpc(env, fn, args, fetchImpl = fetch) {
  if (!fn.startsWith('attester_')) throw new Error(`어테스터 함수가 아닙니다: ${fn}`)
  const token = await attesterToken(env, fetchImpl)
  const res = await fetchImpl(`${env.SUPABASE_URL}/rest/v1/rpc/${fn}`, {
    method: 'POST',
    headers: {
      apikey: env.SUPABASE_ANON_KEY,
      authorization: `Bearer ${token}`,
      'content-type': 'application/json',
    },
    body: JSON.stringify(args ?? {}),
  })
  const text = await res.text()
  const body = text ? JSON.parse(text) : null
  if (!res.ok) {
    // 22023 · 23514 · 23505 · 55000 은 사용자의 요청이 규칙에 걸린 것 — 4xx 로 돌려준다
    const code = body?.code
    const status = code === '55000' ? 503 : ['22023', '23514', '23505', '42501'].includes(code) ? 409 : 502
    throw new HttpError(status, body?.message ?? `서버 오류 (${res.status})`)
  }
  return body
}
