import { test } from 'node:test'
import assert from 'node:assert/strict'
import { webcrypto } from 'node:crypto'
import { clientSecret, revokeApple, appleConfigured } from '../src/apple.js'
import { HttpError } from '../src/supabase.js'

const subtle = webcrypto.subtle

async function testKey() {
  const pair = await subtle.generateKey({ name: 'ECDSA', namedCurve: 'P-256' }, true, ['sign', 'verify'])
  const pkcs8 = Buffer.from(await subtle.exportKey('pkcs8', pair.privateKey)).toString('base64')
  return { pair, pem: `-----BEGIN PRIVATE KEY-----\n${pkcs8.match(/.{1,64}/g).join('\n')}\n-----END PRIVATE KEY-----` }
}

const env = (pem) => ({ APPLE_TEAM_ID: 'TEAM123456', APPLE_KEY_ID: 'KEY1234567', APPLE_PRIVATE_KEY: pem, APPLE_CLIENT_ID: 'com.stepupcrew.stepup' })

function req(body) {
  return new Request('https://attester.test/v1/apple/revoke', {
    method: 'POST', headers: { authorization: 'Bearer t', 'content-type': 'application/json' }, body: JSON.stringify(body),
  })
}

test('Apple 철회: 비밀값이 없으면 503 · 로그인 확인도 하지 않는다', async () => {
  assert.equal(appleConfigured({ APPLE_CLIENT_ID: 'x' }), false)
  let asked = false
  await assert.rejects(revokeApple(req({ code: 'c' }), { APPLE_CLIENT_ID: 'x' }, { getUser: async () => { asked = true } }),
    (e) => e instanceof HttpError && e.status === 503)
  assert.equal(asked, false)
})

test('Apple 철회: client_secret 은 ES256 JWT 이고 키로 검증된다', async () => {
  const { pair, pem } = await testKey()
  const jwt = await clientSecret(env(pem), 1_800_000_000, subtle)
  const [h, c, s] = jwt.split('.')
  const dec = (p) => JSON.parse(Buffer.from(p, 'base64url').toString())
  assert.deepEqual(dec(h), { alg: 'ES256', kid: 'KEY1234567' })
  assert.deepEqual(dec(c), { iss: 'TEAM123456', iat: 1_800_000_000, exp: 1_800_000_300, aud: 'https://appleid.apple.com', sub: 'com.stepupcrew.stepup' })
  const ok = await subtle.verify({ name: 'ECDSA', hash: 'SHA-256' }, pair.publicKey, Buffer.from(s, 'base64url'), new TextEncoder().encode(`${h}.${c}`))
  assert.equal(ok, true)
})

test('Apple 철회: code → refresh_token → 철회 순서 · 같은 client_id', async () => {
  const calls = []
  const fetchImpl = async (url, init) => {
    calls.push([url, Object.fromEntries(new URLSearchParams(init.body))])
    if (url.endsWith('/auth/token')) return new Response(JSON.stringify({ refresh_token: 'r1', access_token: 'a1' }))
    return new Response('', { status: 200 })
  }
  const out = await revokeApple(req({ code: 'code1' }), env('pem'), { fetch: fetchImpl, getUser: async () => ({ id: 'u' }), clientSecret: async () => 'S' })
  assert.deepEqual(out, { ok: true })
  assert.equal(calls[0][0], 'https://appleid.apple.com/auth/token')
  assert.deepEqual(calls[0][1], { client_id: 'com.stepupcrew.stepup', client_secret: 'S', code: 'code1', grant_type: 'authorization_code' })
  assert.equal(calls[1][0], 'https://appleid.apple.com/auth/revoke')
  assert.deepEqual(calls[1][1], { client_id: 'com.stepupcrew.stepup', client_secret: 'S', token: 'r1', token_type_hint: 'refresh_token' })
})

test('Apple 철회: Apple 이 code 를 거절하면 철회를 부르지 않고 ok:false', async () => {
  const urls = []
  const out = await revokeApple(req({ code: 'old' }), env('pem'), {
    fetch: async (url) => { urls.push(url); return new Response('{"error":"invalid_grant"}', { status: 400 }) },
    getUser: async () => ({ id: 'u' }), clientSecret: async () => 'S',
  })
  assert.deepEqual(out, { ok: false, error: 'apple_code_rejected' })
  assert.equal(urls.length, 1)
})

test('Apple 철회: code 가 없거나 너무 길면 400', async () => {
  for (const body of [{}, { code: '' }, { code: 'x'.repeat(3000) }, { code: 5 }]) {
    await assert.rejects(revokeApple(req(body), env('pem'), { getUser: async () => ({ id: 'u' }) }),
      (e) => e instanceof HttpError && e.status === 400)
  }
})
