/**
 * Apple 로그인 토큰 철회(2026-11-03, iOS 앱 — App Store 심사 5.1.1(v)).
 *
 * Apple 로 로그인한 사람이 앱에서 계정을 지우면 Apple 에 그 앱의 토큰을 철회해야 한다.
 * 철회에는 Apple 이 준 개인 키로 만든 client_secret 이 필요해 앱이 아니라 서버(이 워커)가 한다.
 *
 *   앱: 계정 삭제 직전에 Apple 로그인을 한 번 더 받아 authorizationCode 를 얻어 보낸다(5분 · 한 번).
 *   워커: 로그인 토큰을 Supabase 에 확인 → code 를 Apple 에서 refresh_token 으로 바꾼다 → 그 토큰을 철회한다.
 *
 * 비밀값(Secret) 셋이 있어야 켜진다 — 없으면 503 apple_not_configured(앱은 계정 삭제를 막지 않는다).
 *   APPLE_TEAM_ID       Apple Developer 팀 ID(10자)
 *   APPLE_KEY_ID        "Sign in with Apple" 키의 ID(10자)
 *   APPLE_PRIVATE_KEY   그 키의 .p8 내용(-----BEGIN PRIVATE KEY----- …)
 * 보통 설정(vars): APPLE_CLIENT_ID = iOS 앱 번들 ID(앱이 받은 code 의 대상).
 * code · 토큰 · 키는 기록에 남기지 않는다.
 */
import { getUser, HttpError } from './supabase.js'

const APPLE = 'https://appleid.apple.com'

export function appleConfigured(env) {
  return Boolean(env.APPLE_TEAM_ID && env.APPLE_KEY_ID && env.APPLE_PRIVATE_KEY && env.APPLE_CLIENT_ID)
}

function b64url(bytes) {
  let s = ''
  for (const b of bytes) s += String.fromCharCode(b)
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function pemBytes(pem) {
  const body = String(pem).replace(/-----[^-]+-----/g, '').replace(/\s+/g, '')
  return Uint8Array.from(atob(body), (c) => c.charCodeAt(0))
}

/** Apple client_secret — ES256 JWT(iss 팀 · sub 앱 · aud appleid · 5분). WebCrypto 의 ECDSA 서명은 JWS 가 바라는 r||s 그대로다 */
export async function clientSecret(env, nowSeconds = Math.floor(Date.now() / 1000), subtle = crypto.subtle) {
  const header = { alg: 'ES256', kid: env.APPLE_KEY_ID }
  const claims = { iss: env.APPLE_TEAM_ID, iat: nowSeconds, exp: nowSeconds + 300, aud: APPLE, sub: env.APPLE_CLIENT_ID }
  const enc = new TextEncoder()
  const input = `${b64url(enc.encode(JSON.stringify(header)))}.${b64url(enc.encode(JSON.stringify(claims)))}`
  const key = await subtle.importKey('pkcs8', pemBytes(env.APPLE_PRIVATE_KEY), { name: 'ECDSA', namedCurve: 'P-256' }, false, ['sign'])
  const sig = new Uint8Array(await subtle.sign({ name: 'ECDSA', hash: 'SHA-256' }, key, enc.encode(input)))
  return `${input}.${b64url(sig)}`
}

function form(fields) {
  return new URLSearchParams(fields).toString()
}

/**
 * POST /v1/apple/revoke  { code }  (Authorization: Bearer 로그인 토큰)
 * → { ok: true } · { ok: false, error } — 철회하지 못해도 앱은 계정 삭제를 이어 간다.
 */
export async function revokeApple(request, env, deps = {}) {
  const fetchImpl = deps.fetch ?? fetch
  if (!appleConfigured(env)) throw new HttpError(503, 'apple_not_configured')
  await (deps.getUser ?? getUser)(env, request, fetchImpl)
  const body = await request.json().catch(() => null)
  const code = typeof body?.code === 'string' ? body.code : ''
  if (!code || code.length > 2048) throw new HttpError(400, '잘못된 요청입니다')

  const secret = await (deps.clientSecret ?? clientSecret)(env)
  const headers = { 'content-type': 'application/x-www-form-urlencoded' }
  const tokenRes = await fetchImpl(`${APPLE}/auth/token`, {
    method: 'POST',
    headers,
    body: form({ client_id: env.APPLE_CLIENT_ID, client_secret: secret, code, grant_type: 'authorization_code' }),
  })
  if (!tokenRes.ok) return { ok: false, error: 'apple_code_rejected' }
  const tokens = await tokenRes.json().catch(() => null)
  const [token, hint] = tokens?.refresh_token
    ? [tokens.refresh_token, 'refresh_token']
    : [tokens?.access_token, 'access_token']
  if (!token) return { ok: false, error: 'apple_no_token' }

  const revokeRes = await fetchImpl(`${APPLE}/auth/revoke`, {
    method: 'POST',
    headers,
    body: form({ client_id: env.APPLE_CLIENT_ID, client_secret: secret, token, token_type_hint: hint }),
  })
  return revokeRes.ok ? { ok: true } : { ok: false, error: 'apple_revoke_failed' }
}
