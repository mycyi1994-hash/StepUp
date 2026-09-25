import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { accountRef, jwtClaims, tokenFromHash, parseSup, opStatusLabel, createApi, formatSup, sameWallet, blockRanges } from '../wallet-core.js'

const jwt = (claims) => `h.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.s`

test('계정 번호 → bytes32 가 서버 · 워커와 같다', () => {
  const uid = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1'
  assert.equal(accountRef(uid), '0x' + '0'.repeat(32) + 'f1'.repeat(16))
  // 워커(attester/src/typed.js)와 같은 규칙인지 파일에서 확인
  assert.match(readFileSync(new URL('../../attester/src/typed.js', import.meta.url), 'utf8'), /padStart\(64, '0'\)/)
})

test('주소 # 의 토큰만 받는다 (앱: #t= · 구글 로그인: #access_token=)', () => {
  const t = jwt({ sub: 'u', aal: 'aal1', exp: 9999999999 })
  assert.equal(tokenFromHash(`#t=${t}`), t)
  assert.equal(tokenFromHash(`#access_token=${t}&refresh_token=x&type=bearer`), t)
  assert.equal(tokenFromHash('#t=not-a-jwt'), null)
  assert.equal(jwtClaims(t).aal, 'aal1')
})

test('SUP 금액: 소수 4자리까지, 0 · 음수 · 글자는 거절', () => {
  assert.deepEqual(parseSup('12.5'), { text: '12.5', wei: 125n * 10n ** 17n })
  assert.equal(parseSup('1,000').text, '1000')
  for (const bad of ['0', '-1', 'abc', '1.00001', '']) assert.equal(parseSup(bad), null, bad)
})

test('확정 전에는 "완료"라고 하지 않는다', () => {
  for (const s of ['RESERVED', 'SIGNED', 'SUBMITTED', 'EXPIRED']) assert.doesNotMatch(opStatusLabel(s), /완료/)
  assert.match(opStatusLabel('CONFIRMED'), /완료/)
})

test('서버 요청: 공개 키 + 로그인 토큰, 오류는 서버 문장 그대로', async () => {
  const calls = []
  const fake = async (url, init) => {
    calls.push([url, init])
    return new Response(JSON.stringify({ code: '23514', message: '오늘 꺼낼 수 있는 양을 넘었습니다' }), { status: 400 })
  }
  const api = createApi({ supabaseUrl: 'https://s.co/', supabaseKey: 'pk', attesterUrl: 'https://w' }, () => 'T', fake)
  await assert.rejects(api.rpc('sup_withdraw_request', { p_amount: '5' }), /오늘 꺼낼 수 있는 양/)
  assert.equal(calls[0][0], 'https://s.co/rest/v1/rpc/sup_withdraw_request')
  assert.equal(calls[0][1].headers.apikey, 'pk')
  assert.equal(calls[0][1].headers.authorization, 'Bearer T')
})

test('잔고 표시는 버림 — 보이는 만큼은 꺼낼 수 있다', () => {
  assert.equal(formatSup('12.3456'), '12.3456')
  assert.equal(formatSup('12.34569'), '12.3456')
  assert.equal(formatSup(0.9999), '0.9999')
  assert.equal(formatSup(1000), '1,000')
})

test('넣기는 계정에 연결된 지갑에서만', () => {
  assert.equal(sameWallet('0xAbC0000000000000000000000000000000000001', '0xabc0000000000000000000000000000000000001'), true)
  assert.equal(sameWallet('0xabc0000000000000000000000000000000000002', '0xabc0000000000000000000000000000000000001'), false)
  assert.equal(sameWallet('0xabc', null), false)
})

test('블록 구간은 RPC 한도 안으로 나눈다', () => {
  assert.deepEqual(blockRanges(100n, 100n), [[100n, 100n]])
  const r = blockRanges(36979816n, 37000000n)
  assert.equal(r[0][0], 36979816n)
  assert.equal(r.at(-1)[1], 37000000n)
  for (const [a, b] of r) assert.ok(b - a < 10000n)
  for (let i = 1; i < r.length; i++) assert.equal(r[i][0], r[i - 1][1] + 1n)
})
