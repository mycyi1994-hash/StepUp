// 지갑 페이지의 계산 · 서버 호출. 화면(wallet-src.js)과 나눠 두어 node --test 로 검사한다.
//
// 이 페이지는 아무것도 정하지 않는다. 금액 · 신발 · 한도는 전부 서버(Supabase)가 정하고,
// 서명과 가스비는 어테스터 워커가 맡는다. 페이지는 요청을 보내고 결과를 보여줄 뿐이다.

/** 계정 번호 → 체인의 bytes32 (서버 economy.account_ref · 워커 accountRef 와 같다) */
export function accountRef(userId) {
  const hex = String(userId).toLowerCase().replace(/-/g, '')
  if (!/^[0-9a-f]{32}$/.test(hex)) throw new Error('계정 번호가 올바르지 않습니다')
  return '0x' + hex.padStart(64, '0')
}

/** JWT 의 가운데를 읽는다. 검증은 서버가 한다 — 여기서는 화면에 무엇을 보일지만 정한다. */
export function jwtClaims(token) {
  try {
    const part = String(token).split('.')[1]
    const b64 = part.replace(/-/g, '+').replace(/_/g, '/').padEnd(Math.ceil(part.length / 4) * 4, '=')
    const text = typeof atob === 'function' ? atob(b64) : Buffer.from(b64, 'base64').toString('binary')
    return JSON.parse(decodeURIComponent(escape(text)))
  } catch {
    return null
  }
}

/**
 * 방금(maxAgeSec 안에) 2단계 인증을 했는가. 서버(economy.mfa_ok)는 15분까지 인정한다 — 페이지는
 * 그보다 조금 일찍 다시 묻는다. 로그인 자체가 aal2 로 남아 있어도 인증 시각이 오래됐으면 다시 묻는다.
 */
export function recentTotp(claims, nowSec = Date.now() / 1000, maxAgeSec = 600) {
  if (claims?.aal !== 'aal2' || !Array.isArray(claims.amr)) return false
  // 폰 · PC 시계가 빠르면 방금 한 인증도 오래된 것으로 보여 인증을 끝없이 다시 묻는다 — 토큰이 발급된
  // 시각(서버 시계)보다 늦게 보지 않는다
  if (Number.isFinite(Number(claims.iat))) nowSec = Math.min(nowSec, Number(claims.iat))
  return claims.amr.some((a) => a?.method === 'totp' && Number(a.timestamp) >= nowSec - maxAgeSec)
}

/** 로그인 토큰 — 주소 뒤 # 에서 받아(서버로 가지 않는다) 바로 주소창에서 지운다. */
export function tokenFromHash(hash) {
  const params = new URLSearchParams(String(hash || '').replace(/^#/, ''))
  const token = params.get('access_token') || params.get('t')
  if (!token || !jwtClaims(token)?.sub) return null
  return token
}

/** 사람이 적은 SUP 금액 → 서버 값(소수 4자리) · 체인 값(18자리) */
export function parseSup(text) {
  const s = String(text ?? '').trim().replace(/,/g, '')
  if (!/^\d+(\.\d{1,4})?$/.test(s)) return null
  const [whole, frac = ''] = s.split('.')
  const wei = BigInt(whole) * 10n ** 18n + BigInt((frac + '0'.repeat(18)).slice(0, 18))
  if (wei === 0n) return null
  return { text: s, wei }
}

/** 잔고 표시 — 4자리까지, 남는 자리는 버린다(보이는 만큼은 꼭 꺼낼 수 있게) */
export function formatSup(value) {
  const n = Math.floor(Number(value ?? 0) * 1e4 + 1e-6) / 1e4
  return n.toLocaleString('ko-KR', { maximumFractionDigits: 4 })
}

/** 넣기는 지금 연결된 계정의 지갑에서만 — 남의 로그인 링크로 열린 페이지에서 내 SUP 를 남에게 넣지 않게 */
export function sameWallet(account, linked) {
  return Boolean(account && linked) && String(account).toLowerCase() === String(linked).toLowerCase()
}

/** [from, to] 를 RPC 한 번에 읽을 수 있는 크기로 나눈다 (GIWA RPC 는 10,000 블록까지) */
export function blockRanges(from, to, size = 9000n) {
  const out = []
  for (let start = BigInt(from); start <= BigInt(to); start += size) {
    const end = start + size - 1n
    out.push([start, end < BigInt(to) ? end : BigInt(to)])
  }
  return out
}

/** 작업 상태를 사람이 읽는 말로. CONFIRMED 전에는 "완료"라고 하지 않는다. */
export function opStatusLabel(status) {
  return {
    RESERVED: '예약됨',
    SIGNED: '서명됨 — 체인에 보내는 중',
    SUBMITTED: '체인에 보냄 — 확정을 기다리는 중',
    CONFIRMED: '완료 (체인에서 확정)',
    EXPIRED: '만료 — 되돌렸습니다',
  }[status] ?? status
}

/** Supabase 와 어테스터에게 말하는 작은 창구 */
export function createApi(config, getToken, fetchImpl = (...a) => fetch(...a)) {
  const base = config.supabaseUrl.replace(/\/$/, '')
  const headers = (extra = {}) => ({
    apikey: config.supabaseKey,
    authorization: `Bearer ${getToken()}`,
    'content-type': 'application/json',
    ...extra,
  })
  async function read(res) {
    const text = await res.text()
    let body = null
    try { body = text ? JSON.parse(text) : null } catch { body = { message: text } }
    if (!res.ok) {
      const err = new Error(body?.message || body?.msg || body?.error_description || body?.error || `요청 실패 (${res.status})`)
      err.status = res.status
      err.code = body?.code
      throw err
    }
    return body
  }
  return {
    rpc: (name, args = {}) =>
      fetchImpl(`${base}/rest/v1/rpc/${name}`, { method: 'POST', headers: headers(), body: JSON.stringify(args) }).then(read),
    select: (path) => fetchImpl(`${base}/rest/v1/${path}`, { headers: headers() }).then(read),
    user: () => fetchImpl(`${base}/auth/v1/user`, { headers: headers() }).then(read),
    // 2단계 인증 (인증 앱 6자리) — Supabase Auth MFA
    enrollTotp: () =>
      fetchImpl(`${base}/auth/v1/factors`, {
        method: 'POST', headers: headers(), body: JSON.stringify({ factor_type: 'totp', friendly_name: `StepUp ${Date.now()}` }),
      }).then(read),
    // 끝내지 못한 등록을 치운다 — 열 때마다 새 QR 을 만들면 앞서 찍은 인증 앱의 번호가 맞지 않고,
    // 등록 수 한도(10)에 걸린다
    unenroll: (factorId) =>
      fetchImpl(`${base}/auth/v1/factors/${factorId}`, { method: 'DELETE', headers: headers() }).then(read),
    challenge: (factorId) =>
      fetchImpl(`${base}/auth/v1/factors/${factorId}/challenge`, { method: 'POST', headers: headers(), body: '{}' }).then(read),
    verify: (factorId, challengeId, code) =>
      fetchImpl(`${base}/auth/v1/factors/${factorId}/verify`, {
        method: 'POST', headers: headers(), body: JSON.stringify({ challenge_id: challengeId, code }),
      }).then(read),
    attester: (path, body) =>
      fetchImpl(`${config.attesterUrl.replace(/\/$/, '')}${path}`, {
        method: 'POST', headers: { authorization: `Bearer ${getToken()}`, 'content-type': 'application/json' },
        body: JSON.stringify(body ?? {}),
      }).then(read),
    googleLoginUrl: (redirectTo) =>
      `${base}/auth/v1/authorize?provider=google&redirect_to=${encodeURIComponent(redirectTo)}`,
  }
}
