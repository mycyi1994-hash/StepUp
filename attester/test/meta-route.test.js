import { test } from 'node:test'
import assert from 'node:assert/strict'
import worker from '../src/index.js'

// 메타데이터는 누구나 부른다 — 캐시에 있으면 체인을 읽지 않고, 없으면 요청 수 제한부터 본다.
test('메타데이터: 캐시에 있으면 그대로, 없으면 요청 수 제한을 먼저 거친다', async () => {
  const stored = new Map()
  globalThis.caches = {
    default: {
      match: async (req) => stored.get(req.url),
      put: async (req, res) => stored.set(req.url, res),
    },
  }
  const waits = []
  const ctx = { waitUntil: (p) => waits.push(p) }
  const url = 'https://attester.test/v2/meta/5'

  stored.set(url, new Response('{"name":"cached"}', { status: 200 }))
  const hit = await worker.fetch(new Request(url), {}, ctx)
  assert.equal((await hit.json()).name, 'cached')

  stored.clear()
  const env = { RATE_LIMITER: { limit: async () => ({ success: false }) } }
  const limited = await worker.fetch(new Request(url), env, ctx)
  assert.equal(limited.status, 429)
  delete globalThis.caches
})

// 컨트랙트 배포 전에는 워커만 먼저 올려 둔다 — 요청은 받지 않고, 1분 작업도 하지 않는다.
test('컨트랙트 주소가 비어 있으면 POST 는 준비 중, 1분 작업은 건너뛴다', async () => {
  const env = { DISTRIBUTOR_ADDRESS: '', SNEAKERS_ADDRESS: '', VAULT_ADDRESS: '' }
  const res = await worker.fetch(
    new Request('https://attester.test/v2/wallet/link', { method: 'POST', body: '{}' }),
    env,
    { waitUntil() {} },
  )
  assert.equal(res.status, 503)
  const waits = []
  await worker.scheduled({}, env, { waitUntil: (p) => waits.push(p) })
  assert.equal(waits.length, 0)
})
