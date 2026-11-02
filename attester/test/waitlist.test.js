import { test } from 'node:test'
import assert from 'node:assert/strict'
import { registerWaitlist, verifyTurnstile } from '../src/waitlist.js'
import { HttpError } from '../src/supabase.js'

function req(body, ip = '203.0.113.9') {
  return new Request('https://attester.test/v1/waitlist/register', {
    method: 'POST',
    headers: { 'content-type': 'application/json', 'cf-connecting-ip': ip },
    body: JSON.stringify(body),
  })
}

test('대기 명단: 비밀값이 없으면 받지 않는다(준비 중)', async () => {
  await assert.rejects(registerWaitlist(req({ email: 'a@gmail.com', token: 't' }), {}, {}), (e) => e instanceof HttpError && e.status === 503)
})

test('대기 명단: 사람 확인이 실패하면 서버를 부르지 않는다', async () => {
  const calls = []
  const out = await registerWaitlist(req({ email: 'a@gmail.com', consent: true, token: 'bad' }), { TURNSTILE_SECRET: 's' }, {
    verifyTurnstile: async () => false,
    rpc: async (...a) => calls.push(a),
  })
  assert.deepEqual(out, { ok: false, message: 'verification failed' })
  assert.equal(calls.length, 0)
})

test('대기 명단: 확인되면 어테스터 함수로 넣고 IP 를 넘긴다', async () => {
  const calls = []
  const out = await registerWaitlist(req({ email: 'a@gmail.com', consent: true, ref: 'AB2CDEFG', token: 'ok' }), { TURNSTILE_SECRET: 's' }, {
    verifyTurnstile: async (_env, token, ip) => token === 'ok' && ip === '203.0.113.9',
    rpc: async (_env, fn, args) => { calls.push([fn, args]); return { ok: true, receipt: 'r' } },
  })
  assert.deepEqual(out, { ok: true, receipt: 'r' })
  assert.equal(calls[0][0], 'attester_waitlist_register')
  assert.deepEqual(calls[0][1], { p_email: 'a@gmail.com', p_consent: true, p_trap: '', p_ref: 'AB2CDEFG', p_ip: '203.0.113.9' })
})

test('대기 명단: 토큰 · 이메일이 없으면 400', async () => {
  for (const body of [{ email: 'a@gmail.com' }, { token: 't' }, { email: 'a@gmail.com', token: 'x'.repeat(5000) }]) {
    await assert.rejects(registerWaitlist(req(body), { TURNSTILE_SECRET: 's' }, {}), (e) => e instanceof HttpError && e.status === 400)
  }
})

test('Turnstile: 성공이어도 다른 사이트의 토큰은 받지 않는다', async () => {
  const answer = (hostname, success = true) => async () => new Response(JSON.stringify({ success, hostname }))
  assert.equal(await verifyTurnstile({ TURNSTILE_SECRET: 's' }, 't', '1.2.3.4', answer('stepupcrew.com')), true)
  assert.equal(await verifyTurnstile({ TURNSTILE_SECRET: 's' }, 't', '1.2.3.4', answer('evil.com')), false)
  assert.equal(await verifyTurnstile({ TURNSTILE_SECRET: 's' }, 't', '1.2.3.4', answer('stepupcrew.com', false)), false)
})
