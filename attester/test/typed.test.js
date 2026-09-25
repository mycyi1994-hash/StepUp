import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import {
  modelId,
  FACTIONS,
  RARITIES,
  VARIANTS,
  accountRef,
  accountFromRef,
  walletLinkMessage,
  supToWei,
  weiToSup,
  claimMessage,
  releaseMessage,
} from '../src/typed.js'

// 서버 · 컨트랙트와 어긋나면 안 되는 것들을 파일에서 직접 읽어 맞춰 본다.
const sql = (name) => readFileSync(new URL(`../../supabase/migrations/${name}`, import.meta.url), 'utf8')

test('지갑 연결 문장이 서버(economy.wallet_link_message)와 한 글자도 다르지 않다', () => {
  const m = sql('0025_wallet_bridge.sql').match(/format\(E'(StepUp 지갑 연결[^']*)', p_user, p_nonce\)/)
  assert.ok(m, '서버 문장을 찾지 못했다')
  let i = 0
  const fromSql = m[1].replace(/\\n/g, '\n').replace(/%s/g, () => ['U', 'N'][i++])
  assert.equal(walletLinkMessage('U', 'N'), fromSql)
})

test('계정 번호 ↔ bytes32 가 서버(economy.account_ref)와 같다', () => {
  const uid = 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1'
  assert.equal(accountRef(uid), '0x' + '0'.repeat(32) + 'f1'.repeat(16))
  assert.equal(accountFromRef(accountRef(uid)), uid)
  assert.equal(accountFromRef('0xdead'), null)
})

test('도감 52종, 번호가 겹치지 않고 컨트랙트 배포 목록과 같다', async () => {
  const ids = []
  FACTIONS.forEach((f) => RARITIES.forEach((r, ri) => {
    for (let v = 0; v < VARIANTS[ri]; v++) ids.push(modelId(f, r, v))
  }))
  assert.equal(ids.length, 52)
  assert.equal(new Set(ids).size, 52)
  const { createRequire } = await import('node:module')
  const catalog = createRequire(import.meta.url)('../../contracts/scripts/lib/catalog.js')
  assert.deepEqual(catalog.allModels().ids, ids)
  assert.throws(() => modelId('FIRE', 'LEGENDARY', 2)) // 레전더리는 2종
})

test('금액: 서버 소수 4자리 ↔ 체인 18자리', () => {
  assert.equal(supToWei('100'), 100n * 10n ** 18n)
  assert.equal(supToWei('12.5'), 125n * 10n ** 17n)
  assert.equal(supToWei('0.0001'), 10n ** 14n)
  assert.equal(weiToSup(125n * 10n ** 17n), '12.5')
  assert.throws(() => supToWei('-1'))
  assert.throws(() => supToWei('1e18'))
})

const base = {
  op_ref: '0x' + '0'.repeat(32) + 'ab'.repeat(16),
  wallet: '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  deadline_unix: 1790000000,
}

test('SUP 꺼내기 → Claim: 금액·받는 지갑·작업 번호는 서버 값 그대로, 날짜는 컨트랙트의 날', () => {
  const m = claimMessage({ ...base, kind: 'SUP_WITHDRAW', amount: '100.0000' }, 55n)
  assert.deepEqual(m, {
    runner: base.wallet,
    sessionHash: base.op_ref,
    amount: 100n * 10n ** 18n,
    day: 55n,
    deadline: 1790000000n,
  })
  assert.throws(() => claimMessage({ ...base, kind: 'BONUS_MINT' }, 1n))
})

test('신발 → Release: 서버 스탯, 내구도는 100분의 1 단위, 처음이면 tokenId 0', () => {
  const m = releaseMessage({
    ...base,
    kind: 'BONUS_MINT',
    token_id: null,
    faction: 'WIND',
    rarity: 'EPIC',
    variant: 2,
    level: 1,
    efficiency_bps: 812,
    comfort_bps: 640,
    durability: '81.25',
    genesis_no: 3,
    transfer_locked: true,
  })
  assert.equal(m.tokenId, 0n)
  assert.equal(m.model, 322)
  assert.equal(m.rarity, 2)
  assert.equal(m.durability, 8125)
  assert.equal(m.genesisNo, 3)
  assert.equal(m.locked, true)
  assert.throws(() => releaseMessage({ ...base, kind: 'SUP_WITHDRAW' }))
})
