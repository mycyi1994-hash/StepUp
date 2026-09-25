import { clients } from './chain.js'
import { getUser, rpc, HttpError } from './supabase.js'
import { linkWallet, executeOp } from './handlers.js'
import { indexEvents, expireOps, reconcile } from './indexer.js'

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

const deps = { getUser, rpc, clients }

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

export default {
  async fetch(request, env) {
    const url = new URL(request.url)
    if (request.method === 'OPTIONS') return new Response(null, { headers: corsHeaders(request, env) })

    try {
      if (url.pathname === '/health' && request.method === 'GET') {
        const c = clients(env)
        return json(request, env, {
          ok: true,
          service: 'stepup-attester',
          version: 2,
          chainId: c.chain.id,
          attester: c.attester.address,
          sneakerSigner: c.sneakerSigner.address,
          relayer: c.relayer.account.address,
          guardian: c.guardian?.account.address ?? null,
          contracts: c.addresses,
        })
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
    const run = async () => {
      const events = await indexEvents(env, deps)
      const expiry = await expireOps(env, deps)
      const books = await reconcile(env, deps)
      console.log(JSON.stringify({ events, expiry, books }))
    }
    ctx.waitUntil(run().catch((e) => console.error('scheduled error', e?.message)))
  },
}
