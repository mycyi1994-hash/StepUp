import { test } from 'node:test'
import assert from 'node:assert/strict'
import fs from 'node:fs'
import { privateKeyToAccount, generatePrivateKey } from 'viem/accounts'
import { verifyTypedData } from 'viem'
import { SHOE_MODELS } from '../src/shoe-catalog.js'
import { CATALOG_WAIT_BLOCKS, MULTICALL3, catalogMarker, ensureCatalog, isCatalogModel, releasesOnV3 } from '../src/catalog.js'
import { RELEASE_TYPES, VAULT_MINT_TYPES, releaseDomain, sneakerModel, vaultMintMessage } from '../src/typed.js'
import { V3_DEPLOYMENTS } from '../src/v3-deploy.js'
import { sneakerSourceOf } from '../src/indexer.js'
import { executeOp } from '../src/handlers.js'
import { metadataOf } from '../src/meta.js'
import { runJobs } from '../src/jobs.js'

const V3 = V3_DEPLOYMENTS[91342].address
const WALLET = '0x' + '99'.repeat(20)
const ROOT = new URL('../../', import.meta.url)
const read = (path) => fs.readFileSync(new URL(path, ROOT), 'utf8')

test('새 도감: catalog.json · 서버 0045 · 워커 표가 같다(번호 · 등급), 그림 파일이 웹에 있다', () => {
  const catalog = JSON.parse(read('design/shoes-2026-09/catalog.json')).models
  assert.equal(catalog.length, 70)
  assert.deepEqual(
    Object.entries(SHOE_MODELS).map(([id, m]) => [Number(id), m.rarity, m.en]),
    catalog.map((m) => [m.id, m.rarity, m.en]),
  )
  const sql = read('supabase/migrations/0045_shoe_catalog.sql')
  for (const m of catalog) {
    assert.ok(sql.includes(`(${m.id}, '${m.rarity}', '${m.series}', '${m.file}'`), `0045 에 ${m.id}`)
    assert.ok(fs.existsSync(new URL(`web/assets/sneakers/${SHOE_MODELS[m.id].file}`, ROOT)), `웹 그림 ${m.id}`)
    assert.ok(fs.existsSync(new URL(`app/src/main/res/drawable-nodpi/shoe_${m.id}.webp`, ROOT)), `앱 그림 ${m.id}`)
  }
  // 예전 52종 번호(속성 × 100 + 등급 × 10 + 변형 = 0~333)와 겹치지 않는다
  assert.ok(Object.keys(SHOE_MODELS).every((id) => Number(id) >= 1000))
})

test('서명 재료: 새 도감 모델이면 그 번호, 등급이 다르면 만들지 않는다, 없으면 예전 번호', () => {
  const base = { faction: 'WIND', rarity: 'LEGENDARY', variant: 1 }
  assert.equal(sneakerModel({ ...base, model_id: 1317 }), 1317)
  assert.throws(() => sneakerModel({ ...base, rarity: 'EPIC', model_id: 1317 }), /등급/)
  assert.throws(() => sneakerModel({ ...base, model_id: 999 }), /알 수 없는 모델/)
  assert.equal(sneakerModel({ ...base, model_id: null }), 331)
  const m = vaultMintMessage({
    kind: 'VAULT_MINT', op_ref: '0x' + '0'.repeat(32) + 'ab'.repeat(16), account: '0x' + 'aa'.repeat(32),
    model_id: 1101, faction: 'FIRE', rarity: 'RARE', variant: 2, level: 1, efficiency_bps: 500, comfort_bps: 400,
    durability: '100', genesis_no: null, deadline_unix: 1790000000,
  })
  assert.equal(m.model, 1101)
  assert.equal(m.rarity, 1)
  assert.equal(VAULT_MINT_TYPES.VaultMint.find((f) => f.name === 'model').type, 'uint32')
})

test('v3 로 보내는 신발 작업: 금고의 v3 토큰 · 새 모델의 첫 발행', () => {
  assert.equal(releasesOnV3({ kind: 'SNEAKER_WITHDRAW', token_id: '1000003', model_id: null }), true)
  assert.equal(releasesOnV3({ kind: 'SNEAKER_WITHDRAW', token_id: null, model_id: 1205 }), true)
  assert.equal(releasesOnV3({ kind: 'BONUS_MINT', token_id: null, model_id: 1205 }), true)
  assert.equal(releasesOnV3({ kind: 'BONUS_MINT', token_id: null, model_id: null }), false)
  assert.equal(releasesOnV3({ kind: 'SNEAKER_WITHDRAW', token_id: '12', model_id: null }), false)
  assert.equal(sneakerSourceOf({ kind: 'BONUS_MINT', token_id: null, model_id: 1330 }), 'sneakersV3')
  assert.equal(sneakerSourceOf({ kind: 'BONUS_MINT', token_id: null, model_id: null }), 'sneakers')
  assert.equal(isCatalogModel(1101), true)
  assert.equal(isCatalogModel(21), false)
  assert.equal(isCatalogModel(null), false)
})

test('지갑 선물: 새 모델 신발은 v3 컨트랙트 · v3 서명 영역으로 바로 발행(토큰 번호 0)', async () => {
  const signer = privateKeyToAccount(generatePrivateKey())
  const submitted = []
  const payload = {
    kind: 'BONUS_MINT',
    op_ref: '0x' + '0'.repeat(32) + 'ef'.repeat(16),
    wallet: WALLET,
    deadline_unix: 1790000000,
    token_id: null,
    model_id: 1210,
    faction: 'WATER',
    rarity: 'EPIC',
    variant: 2,
    level: 1,
    efficiency_bps: 800,
    comfort_bps: 700,
    durability: '100',
    genesis_no: 7,
    transfer_locked: true,
  }
  const deps = {
    getUser: async () => ({ id: 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' }),
    rpc: async (_env, fn) => (fn === 'attester_op_payload' ? [payload] : null),
    clients: () => ({
      chain: { id: 91342 },
      addresses: { sneakers: '0x' + '2'.repeat(40), sneakersV3: V3 },
      sneakerSigner: signer,
      publicClient: { simulateContract: async (x) => ({ request: x }) },
      relayer: { writeContract: async (x) => (submitted.push(x), '0x' + 'ab'.repeat(32)) },
    }),
  }
  const request = new Request('https://attester.test/x', { method: 'POST', headers: { authorization: 'Bearer t' } })
  await executeOp(request, {}, deps, '11111111-2222-3333-4444-555555555555')
  assert.equal(submitted[0].address, V3)
  const [message, signature] = submitted[0].args
  assert.equal(message.tokenId, 0n)
  assert.equal(message.model, 1210)
  assert.equal(message.locked, true)
  assert.ok(
    await verifyTypedData({
      address: signer.address,
      domain: releaseDomain(91342, V3, '3'),
      types: RELEASE_TYPES,
      primaryType: 'Release',
      message,
      signature,
    }),
  )
})

test('v3 도감 추가: 없는 모델만 한 거래로, 표시를 먼저 남기고, 확정을 기다리는 동안 다시 보내지 않는다', async () => {
  const ids = Object.keys(SHOE_MODELS).map(Number)
  const make = ({ present = [], marker = null } = {}) => {
    const sent = []
    const calls = []
    const reads = []
    const c = {
      chain: { id: 91342 },
      addresses: { sneakersV3: V3 },
      relayer: { writeContract: async (x) => (calls.push(['send']), sent.push(x), '0x' + 'cd'.repeat(32)) },
      publicClient: {
        multicall: async (x) => (reads.push(x), x.contracts.map((k) => present.includes(Number(k.args[0])))),
        getBlockNumber: async () => 20_000n,
      },
    }
    const deps = {
      rpc: async (_env, fn, args) => {
        calls.push([fn, args])
        return fn === 'attester_cursor_get' ? marker : null
      },
    }
    return { c, deps, sent, calls, reads }
  }
  const all = make({ present: ids })
  assert.deepEqual(await ensureCatalog({}, all.deps, all.c), { ready: true })
  assert.equal(all.reads.length, 1, '70개를 요청 한 번으로')
  assert.equal(all.reads[0].multicallAddress, MULTICALL3)
  assert.equal(all.sent.length, 0)

  const none = make({ present: [1101] })
  const out = await ensureCatalog({}, none.deps, none.c)
  assert.equal(out.ready, false)
  assert.equal(out.missing, 69)
  assert.equal(none.sent.length, 1)
  const [models, rarities] = none.sent[0].args
  assert.equal(none.sent[0].functionName, 'addModels')
  assert.equal(none.sent[0].address, V3)
  assert.ok(!models.includes(1101) && models.length === 69)
  assert.deepEqual(rarities, models.map((id) => ['COMMON', 'RARE', 'EPIC', 'LEGENDARY'].indexOf(SHOE_MODELS[id].rarity)))
  const setAt = none.calls.findIndex(([fn]) => fn === 'attester_cursor_set')
  assert.ok(setAt >= 0 && setAt < none.calls.findIndex(([fn]) => fn === 'send'), '표시를 먼저')
  assert.deepEqual(none.calls[setAt][1], { p_name: catalogMarker(91342, V3), p_block: 20000 })

  const waiting = make({ marker: 20_000 - Number(CATALOG_WAIT_BLOCKS) + 1 })
  assert.equal((await ensureCatalog({}, waiting.deps, waiting.c)).reason, 'adding models')
  assert.equal(waiting.sent.length, 0)
  const retry = make({ marker: 20_000 - Number(CATALOG_WAIT_BLOCKS) })
  await ensureCatalog({}, retry.deps, retry.c)
  assert.equal(retry.sent.length, 1, '기다린 뒤에도 없으면 다시')
})

test('2분 작업: v3 도감이 다 들어가기 전에는 v3 일(금고 발행 · 갱신)을 보내지 않는다', async () => {
  const c = {
    chain: { id: 91342 },
    addresses: { eas: null, sneakersV3: V3 },
    relayer: { account: { address: '0x' + '44'.repeat(20) }, writeContract: async () => '0x' + 'cd'.repeat(32) },
    publicClient: {
      getCode: async () => '0x6080',
      multicall: async (x) => x.contracts.map(() => false),
      getBlockNumber: async () => 30_000n,
    },
  }
  const deps = {
    clients: () => c,
    rpc: async (_env, fn) => {
      if (fn === 'attester_jobs_open') return []
      return null
    },
  }
  const out = await runJobs({}, deps)
  assert.deepEqual(out.v3, { ready: true })
  assert.ok(out.catalog.sent)
  assert.deepEqual(out.send, { skipped: 'nothing to send' })
})

test('메타데이터: 새 모델은 도감 이름 · 시리즈 · 그림(shoe_<번호>.webp)', () => {
  const meta = metadataOf('1000009', { model: 1321, rarity: 3, level: 1, efficiencyBps: 1100, comfortBps: 1000, durability: 10000, genesisNo: 0 }, true, 'https://img.test/')
  assert.equal(meta.name, `${SHOE_MODELS[1321].en} #1000009`)
  assert.equal(meta.image, 'https://img.test/shoe_1321.webp')
  assert.equal(meta.attributes.find((a) => a.trait_type === 'Series').value, 'Finish')
  assert.equal(meta.attributes.find((a) => a.trait_type === 'Rarity').value, 'Legendary')
  assert.equal(meta.attributes.find((a) => a.trait_type === 'Theme'), undefined)
})
