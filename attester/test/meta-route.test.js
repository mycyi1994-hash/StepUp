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
