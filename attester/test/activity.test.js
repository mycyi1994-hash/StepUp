import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { decodeAbiParameters, parseAbiParameters, zeroAddress } from 'viem'
import { ACTIVITY_SCHEMAS, EAS_KINDS, SCHEMAS, attestRequest, attestationData, schemaUid } from '../src/eas.js'

// 0078 새 기록 — 서버 재료(economy.activity_payload)의 키가 스키마 칸 이름 그대로다
const H = (b) => '0x' + b.repeat(32)
const base = { recipient: zeroAddress, runner: H('aa'), day: 20261027 }
const D = { dataHash: H('99') }
const PAYLOADS = {
  SHOE_DRAW: { ...base, ...D, shoe: H('bb'), drawKind: 'FREE', rarity: 'RARE', modelId: 12, drawNonce: 7, seedHash: H('cc') },
  SHOE_FORGE: { ...base, ...D, shoe: H('bb'), success: true, levelBefore: 3, levelAfter: 4, ratePermille: 600, roll: 123, materials: 3 },
  SHOE_REPAIR: { ...base, ...D, shoe: H('bb'), points: 25 },
  SHOE_POINTS: { ...base, ...D, shoe: H('bb'), efficiency: 2, comfort: 1, durability: 0 },
  SHOE_TRADE: { ...base, ...D, seller: H('dd'), buyer: H('aa'), shoe: H('bb'), rarity: 'EPIC', level: 5, priceCenti: '123456' },
  DRAW_SEED: { ...base, ...D, seedHash: H('cc'), firstNonce: 0 },
  DRAW_REVEAL: { ...base, ...D, seed: H('12'), seedHash: H('cc'), firstNonce: 0, lastNonce: 41 },
  RUN_SETTLE: { ...base, ...D, run: H('11'), distanceM: 5200, durationSec: 1800, pointsMilli: '12500', verdict: 'CLEAN' },
  SHOE_WEAR: { ...base, ...D, shoe: H('bb'), run: H('11'), runM: 5000, totalM: '42000', durability: 820 },
  CREW_RUN: { ...base, ...D, crew: H('ee'), room: H('ff'), run: H('11'), distanceM: 4200, activeSec: 1500, together: 3 },
  FLASH_RUN: { ...base, ...D, flash: H('ff'), together: 2 },
  CREW_JOIN: { ...base, ...D, crew: H('ee') },
  PARTY_START: { ...base, ...D, room: H('ff'), crew: H('00'), starters: 4 },
  DAILY_STEPS: { ...base, ...D, goalMet: true },
  CHECK_IN: { ...base, ...D },
}


test('새 기록이 모두 EAS 일이다(스키마가 다 다르다)', () => {
  assert.deepEqual(Object.keys(ACTIVITY_SCHEMAS).sort(), Object.keys(PAYLOADS).sort())
  for (const kind of Object.keys(ACTIVITY_SCHEMAS)) assert.ok(EAS_KINDS.includes(kind), kind)
  const uids = new Set(Object.values(SCHEMAS).map((s) => schemaUid(s)))
  assert.equal(uids.size, Object.keys(SCHEMAS).length)
})

test('새 기록 — 서버 재료 그대로 담고, 다시 풀면 같은 값', () => {
  for (const [kind, p] of Object.entries(PAYLOADS)) {
    const schema = ACTIVITY_SCHEMAS[kind]
    const decoded = decodeAbiParameters(parseAbiParameters(schema), attestationData(kind, p))
    schema.split(',').forEach((field, i) => {
      const name = field.trim().split(/\s+/)[1]
      const want = p[name]
      const got = decoded[i]
      assert.equal(typeof got === 'bigint' ? got.toString() : got, typeof want === 'number' && typeof got === 'bigint' ? String(want) : want,
        `${kind}.${name}`)
    })
    assert.equal(attestRequest(kind, p).schema, schemaUid(schema))
  }
})

test('새 기록 — 틀린 재료는 만들지 않는다', () => {
  assert.throws(() => attestationData('SHOE_DRAW', { ...PAYLOADS.SHOE_DRAW, seedHash: 'abc' }))
  assert.throws(() => attestationData('SHOE_DRAW', { ...PAYLOADS.SHOE_DRAW, rarity: 'rare; drop' }))
  assert.throws(() => attestationData('SHOE_DRAW', { ...PAYLOADS.SHOE_DRAW, drawNonce: -1 }))
  assert.throws(() => attestationData('SHOE_TRADE', { ...PAYLOADS.SHOE_TRADE, priceCenti: '12.5' }))
  assert.throws(() => attestationData('SHOE_TRADE', { ...PAYLOADS.SHOE_TRADE, priceCenti: (2n ** 64n).toString() }))
  assert.throws(() => attestationData('SHOE_FORGE', { ...PAYLOADS.SHOE_FORGE, success: 'yes' }))
  assert.throws(() => attestationData('DAILY_STEPS', { ...PAYLOADS.DAILY_STEPS, goalMet: 1 }))
  assert.throws(() => attestationData('RUN_SETTLE', { ...PAYLOADS.RUN_SETTLE, dataHash: undefined }))
  assert.throws(() => attestationData('CHECK_IN', { ...PAYLOADS.CHECK_IN, runner: '0x12' }))
})

test('서버 재료의 키 = 워커 스키마 칸 이름(0078 activity_payload 와 맞대어 본다)', () => {
  const sql = readFileSync(new URL('../../supabase/migrations/0078_chain_activity.sql', import.meta.url), 'utf8')
  const body = sql.slice(sql.indexOf('create or replace function economy.activity_payload'))
  for (const [kind, schema] of Object.entries(ACTIVITY_SCHEMAS)) {
    const start = body.indexOf(`'${kind}'`)
    assert.ok(start > 0, `${kind} 가 activity_payload 에 없다`)
    const next = body.slice(start + 1).search(/\n  (elsif j\.kind|end if;)/)
    const branch = body.slice(start, start + 1 + next)
    for (const field of schema.split(',')) {
      const name = field.trim().split(/\s+/)[1]
      if (['runner', 'day', 'dataHash'].includes(name)) continue // 모든 기록에 공통으로 넣는다(dataHash 는 봉인할 때)
      assert.ok(branch.includes(`'${name}'`), `${kind}.${name} 를 서버가 주지 않는다`)
    }
  }
})
