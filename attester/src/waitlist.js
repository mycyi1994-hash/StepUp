import { HttpError } from './supabase.js'

/**
 * 홈페이지 대기 명단 등록 — 사람 확인(Cloudflare Turnstile)을 거친 것만 서버에 넣는다(2026-11-02, 서버 0083).
 *
 *   POST /v1/waitlist/register  { email, consent, trap, ref, token }
 *
 * 토큰은 Cloudflare 에 물어 확인한다(비밀 TURNSTILE_SECRET — 워커 비밀값, 저장소에 넣지 않는다).
 * 확인되면 attester_waitlist_register 를 부르고, 서버의 답({ok, receipt, referral_code} 또는 {ok:false, message})을 그대로 돌려준다.
 * 같은 곳 횟수는 서버가 부른 사람의 IP(cf-connecting-ip)로 센다.
 */

const SITEVERIFY = 'https://challenges.cloudflare.com/turnstile/v0/siteverify'

/** Cloudflare 에 토큰을 묻는다. 다른 사이트에서 받은 토큰은 받지 않는다(hostname) */
export async function verifyTurnstile(env, token, ip, fetchImpl = fetch) {
  const form = new FormData()
  form.append('secret', env.TURNSTILE_SECRET)
  form.append('response', token)
  if (ip) form.append('remoteip', ip)
  const res = await fetchImpl(SITEVERIFY, { method: 'POST', body: form })
  const out = await res.json().catch(() => null)
  if (!out?.success) return false
  const hosts = String(env.TURNSTILE_HOSTNAMES ?? 'stepupcrew.com,www.stepupcrew.com')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return hosts.includes(out.hostname)
}

export async function registerWaitlist(request, env, deps) {
  if (!env.TURNSTILE_SECRET) throw new HttpError(503, '아직 준비 중입니다')
  const body = await request.json().catch(() => null)
  const token = String(body?.token ?? '')
  const email = String(body?.email ?? '')
  if (!token || token.length > 4096 || !email || email.length > 254) {
    throw new HttpError(400, '요청이 올바르지 않습니다')
  }
  const ip = request.headers.get('cf-connecting-ip') ?? ''
  const verify = deps.verifyTurnstile ?? verifyTurnstile
  if (!(await verify(env, token, ip))) {
    return { ok: false, message: 'verification failed' }
  }
  return deps.rpc(env, 'attester_waitlist_register', {
    p_email: email,
    p_consent: body?.consent === true,
    p_trap: String(body?.trap ?? ''),
    p_ref: String(body?.ref ?? ''),
    p_ip: ip,
  })
}
