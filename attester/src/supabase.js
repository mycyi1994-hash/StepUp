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
  // 로그인 서버가 잠깐 바쁘거나(429 · 5xx) 끊긴 것을 "로그인 만료"로 보내면 페이지가 토큰을 버리고
  // 다시 로그인 · 2단계 인증을 시킨다 — 토큰을 거절한 경우(401 · 403)만 만료다
  if (res.status === 401 || res.status === 403) throw new HttpError(401, '로그인이 만료되었습니다')
  if (!res.ok) throw new HttpError(503, '잠시 뒤에 다시 해 주세요')
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
 *
 * 로그인은 한 번에 하나만 한다 — 인덱서처럼 여러 일을 한꺼번에 시작하면 저마다 로그인해 Supabase 의
 * 로그인 요청 수 제한(5분에 30번)에 걸린다. 실패하면 잠시(LOGIN_RETRY_MS, 수 제한이면 5배) 다시 로그인하지
 * 않는다 — 실패할 때마다 모든 일이 다시 로그인하면 그 요청들이 제한을 계속 채워 영영 풀리지 않는다.
 */
export const LOGIN_RETRY_MS = 60_000

let cached = { token: null, until: 0 }
let pending = null
let failedUntil = 0

/** 검사용 — 인스턴스에 남은 로그인 상태를 비운다 */
export function resetAttesterLogin() {
  cached = { token: null, until: 0 }
  pending = null
  failedUntil = 0
}

export async function attesterToken(env, fetchImpl = fetch) {
  if (env.ATTESTER_DB_JWT) return env.ATTESTER_DB_JWT
  const now = Date.now()
  if (cached.token && cached.until > now) return cached.token
  if (failedUntil > now) throw new HttpError(502, '어테스터 계정으로 로그인하지 못했습니다')
  pending ??= login(env, fetchImpl).finally(() => {
    pending = null
  })
  return pending
}

async function login(env, fetchImpl) {
  const res = await fetchImpl(`${env.SUPABASE_URL}/auth/v1/token?grant_type=password`, {
    method: 'POST',
    headers: { apikey: env.SUPABASE_ANON_KEY, 'content-type': 'application/json' },
    // 비밀을 넣을 때 끝에 줄바꿈 · 공백이 섞이면(echo … | wrangler secret put) 로그인이 invalid_credentials 로 실패한다
    body: JSON.stringify({ email: String(env.ATTESTER_EMAIL ?? '').trim(), password: String(env.ATTESTER_PASSWORD ?? '').trim() }),
  })
  if (!res.ok) {
    // 비밀이 아닌 것만 남긴다 — 상태와 오류 코드(invalid_credentials · email_not_confirmed · over_request_rate_limit …)
    const body = await res.json().catch(() => null)
    console.error('attester login failed', res.status, body?.error_code ?? body?.error ?? '')
    failedUntil = Date.now() + (res.status === 429 ? 5 : 1) * LOGIN_RETRY_MS
    throw new HttpError(502, '어테스터 계정으로 로그인하지 못했습니다')
  }
  const body = await res.json()
  cached = { token: body.access_token, until: Date.now() + (Number(body.expires_in ?? 3600) - 60) * 1000 }
  failedUntil = 0
  return cached.token
}

/** 서버 함수가 규칙으로 거절한 코드 — 이유 문구는 우리가 쓴 한국어라 사용자에게 그대로 보여 줘도 된다 */
const RULE_CODES = ['22023', '23514', '23505', '42501']

/**
 * attester_* 함수 호출. 규칙에 걸린 거절은 서버가 적은 이유를, 그 밖의 오류는 일반 문구를 올린다
 * (PostgREST 내부 문구 · 함수 이름 같은 것을 사용자에게 흘리지 않게). 로그인 토큰이 만료 · 취소됐으면
 * 한 번 새로 받아 다시 부른다 — 예전에는 한 시간 동안 이 인스턴스의 모든 호출이 실패했다.
 */
export async function rpc(env, fn, args, fetchImpl = fetch) {
  if (!fn.startsWith('attester_')) throw new Error(`어테스터 함수가 아닙니다: ${fn}`)
  for (let attempt = 0; ; attempt++) {
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
    let body = null
    let parsed = true
    try {
      body = text ? JSON.parse(text) : null
    } catch {
      parsed = false
    }
    if (res.ok) {
      // 성공인데 본문이 깨졌으면 성공으로 치지 않는다 — 인덱서가 이벤트를 처리한 것으로 보고
      // 커서를 옮기면 그 입금 · 지급을 영영 건너뛴다
      if (!parsed) {
        console.error('rpc malformed success body', fn, res.status)
        throw new HttpError(502, '서버가 응답하지 않습니다. 잠시 뒤에 다시 해 주세요')
      }
      return body
    }
    if (res.status === 401 && attempt === 0 && !env.ATTESTER_DB_JWT) {
      cached = { token: null, until: 0 }
      continue
    }
    const code = body?.code
    if (code === '55000') throw new HttpError(503, body?.message ?? '지금은 잠시 멈췄습니다')
    if (RULE_CODES.includes(code)) throw new HttpError(409, body?.message ?? '처리할 수 없는 요청입니다')
    console.error('rpc failed', fn, res.status, code ?? '')
    throw new HttpError(502, '서버가 응답하지 않습니다. 잠시 뒤에 다시 해 주세요')
  }
}
