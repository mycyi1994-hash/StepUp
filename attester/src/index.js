import { clients } from './chain.js'
import { getUser, rpc, HttpError } from './supabase.js'
import { linkWallet, executeOp } from './handlers.js'
import { indexEvents, expireOps, reconcile, keepPaused } from './indexer.js'
import { sneakerMetadata } from './meta.js'

/**
 * StepUp 어테스터 v2 — 서버가 허락한 체인 작업만 서명하고, 가스비를 대신 내 보낸다.
 *
 * ## 이 워커가 하지 않는 것
 *
 * 금액과 신발 값을 정하지 않는다. 정본은 Supabase 다(0022~0026). 워커는 서버가
 * 예약한 작업(chain_ops)을 서명 재료로 받아 서명할 뿐이고, 요청 본문에서는 작업
 * 번호만 받는다. 예전 /claim (폰이 보낸 러닝 데이터로 서명)은 없앴다.
 *
 * ## 키 (전부 Secret — 저장소 · 로그 · 응답에 절대 나오지 않는다)
 *
 *   ATTESTER_PRIVATE_KEY   SUP 꺼내기 서명      RewardDistributor.attester
 *   SNEAKER_SIGNER_KEY     신발 발행 · 반환 서명 StepUpSneakers.signer
 *   RELAYER_PRIVATE_KEY    가스비를 내고 거래를 보낸다 (역할 없음, ETH 만 든다)
 *   GUARDIAN_PRIVATE_KEY   이상할 때 세 컨트랙트를 멈춘다 (재개는 못 한다)
 *   ATTESTER_DB_JWT        Supabase stepup_attester 역할 토큰 (attester_* 함수만)
 *
 * ## 사고가 나면
 *
 * 1분마다 체인과 서버 장부를 맞춰 보고, 서버가 모르는 지급이 보이면 서버와 세
 * 컨트랙트를 모두 멈춘다. 다시 켜는 것은 관리자 지갑과 관리자 계정만 할 수 있다.
 */

/**
 * 로그인한 사용자별 요청 수 제한 — IP 제한은 통신사 공용 IP 를 쓰는 사람들이 서로 막고, IP 를 바꾸면
 * 피해 간다. 작업 실행 · 지갑 연결은 로그인 확인 뒤 계정 번호로도 센다.
 */
async function limitUser(env, userId) {
  if (!env.RATE_LIMITER || !userId) return false
  const { success } = await env.RATE_LIMITER.limit({ key: `user:${userId}` })
  return !success
}

const deps = { getUser, rpc, clients, limitUser }

/** 컨트랙트 주소가 다 채워졌는가 — 배포 전에는 워커만 먼저 올려 주소(URL)를 정할 수 있다 */
const configured = (env) => Boolean(env.DISTRIBUTOR_ADDRESS && env.SNEAKERS_ADDRESS && env.VAULT_ADDRESS)

function corsHeaders(request, env) {
  const origin = request.headers.get('origin') ?? ''
  const allowed = String(env.ALLOWED_ORIGINS ?? '')
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  return {
    'access-control-allow-origin': allowed.includes(origin) ? origin : allowed[0] ?? 'null',
    'access-control-allow-headers': 'authorization, content-type',
    'access-control-allow-methods': 'POST, GET, OPTIONS',
    vary: 'origin',
  }
}

function json(request, env, body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'content-type': 'application/json; charset=utf-8', ...corsHeaders(request, env) },
  })
}

/** 사용자(또는 IP)별 요청 수 제한. wrangler.toml 의 RATE_LIMITER 가 있으면 쓴다. */
async function rateLimited(request, env) {
  if (!env.RATE_LIMITER) return false
  const key = request.headers.get('cf-connecting-ip') ?? 'unknown'
  const { success } = await env.RATE_LIMITER.limit({ key })
  return !success
}

async function metadataResponse(request, env, ctx, url, id) {
  const cache = globalThis.caches?.default
  // 쿼리(?x=…)를 바꿔 캐시를 피해 RPC 를 두드리지 못하게 경로만으로 캐시한다
  const key = new Request(url.origin + url.pathname, { method: 'GET' })
  if (cache) {
    const hit = await cache.match(key)
    if (hit) return hit
  }
  if (await rateLimited(request, env)) {
    return json(request, env, { ok: false, error: '요청이 너무 많습니다. 잠시 뒤에 다시 해 주세요' }, 429)
  }
  let body
  let status = 200
  let maxAge = 300
  try {
    body = await sneakerMetadata(env, deps, id)
  } catch (e) {
    if (!(e instanceof HttpError) || e.status >= 500) throw e
    body = { ok: false, error: e.message }
    status = e.status
    maxAge = 60
  }
  const res = new Response(JSON.stringify(body), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'cache-control': `public, max-age=${maxAge}`,
      'access-control-allow-origin': '*',
    },
  })
  if (cache) ctx.waitUntil(cache.put(key, res.clone()))
  return res
}

export default {
  async fetch(request, env, ctx) {
    const url = new URL(request.url)
    if (request.method === 'OPTIONS') return new Response(null, { headers: corsHeaders(request, env) })

    try {
      if (url.pathname === '/health' && request.method === 'GET') {
        const c = clients(env)
        return json(request, env, {
          ok: true,
          service: 'stepup-attester',
          version: 2,
          // 배포한 커밋 — 자동 배포가 새 코드가 떴는지 이것으로 확인한다
          commit: env.COMMIT_SHA || null,
          chainId: c.chain.id,
          attester: c.attester.address,
          sneakerSigner: c.sneakerSigner.address,
          relayer: c.relayer.account.address,
          guardian: c.guardian?.account.address ?? null,
          // 지킴이 키가 없으면 이상이 보여도 컨트랙트를 멈추지 못한다
          canPause: Boolean(c.guardian),
          contracts: c.addresses,
        })
      }

      // 신발 메타데이터 — 마켓 · 지갑이 읽는다. 누구나 부를 수 있으므로 워커 캐시를 먼저 보고,
      // 캐시에 없을 때만 요청 수 제한을 거쳐 체인을 읽는다. 없는 번호도 잠깐 캐시한다 —
      // 번호를 바꿔 가며 두드려 인덱서와 같이 쓰는 RPC 를 막지 못하게.
      const meta = url.pathname.match(/^\/v2\/meta\/([^/]+)$/)
      if (meta && request.method === 'GET') {
        return metadataResponse(request, env, ctx, url, meta[1])
      }

      if (request.method === 'POST' && !configured(env)) {
        return json(request, env, { ok: false, error: '아직 준비 중입니다' }, 503)
      }

      if (request.method === 'POST' && (await rateLimited(request, env))) {
        return json(request, env, { ok: false, error: '요청이 너무 많습니다. 잠시 뒤에 다시 해 주세요' }, 429)
      }

      if (url.pathname === '/v2/wallet/link' && request.method === 'POST') {
        return json(request, env, await linkWallet(request, env, deps))
      }

      const m = url.pathname.match(/^\/v2\/ops\/([^/]+)\/execute$/)
      if (m && request.method === 'POST') {
        return json(request, env, await executeOp(request, env, deps, m[1]))
      }

      return json(request, env, { ok: false, error: '없는 경로입니다' }, 404)
    } catch (e) {
      if (e instanceof HttpError) return json(request, env, { ok: false, error: e.message }, e.status)
      console.error('attester error', e?.message) // 키나 토큰은 찍지 않는다
      return json(request, env, { ok: false, error: '잠시 뒤에 다시 해 주세요' }, 500)
    }
  },

  async scheduled(_event, env, ctx) {
    // 컨트랙트 배포 전(주소가 비어 있을 때)에는 워커만 먼저 올려 둘 수 있게 아무것도 하지 않는다
    if (!configured(env)) {
      console.log('contracts not configured — skipping')
      return
    }
    // 세 일을 따로 돌린다 — 앞의 일이 실패해도 대조(키가 샜는지 보는 일)는 매번 한다
    const step = (name, fn) =>
      fn(env, deps).catch((e) => {
        console.error(`scheduled ${name} error`, e?.message)
        return { error: true }
      })
    // 안전장치(다시 멈추기 · 대조)를 먼저 — 인덱서 · 만료가 요청 수를 다 써 버려도 매번 돈다
    const run = async () => {
      const guard = await step('guard', keepPaused)
      const books = await step('reconcile', reconcile)
      const events = await step('events', indexEvents)
      const expiry = await step('expiry', expireOps)
      console.log(JSON.stringify({ events, expiry, books, guard }))
    }
    ctx.waitUntil(run())
  },
}
