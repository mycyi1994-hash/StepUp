import { test } from 'node:test'
import assert from 'node:assert/strict'
import { privateKeyToAccount, generatePrivateKey } from 'viem/accounts'
import {
  concat,
  decodeAbiParameters,
  decodeFunctionData,
  encodeAbiParameters,
  encodeEventTopics,
  encodePacked,
  keccak256,
  parseAbiParameters,
  parseTransaction,
  verifyTypedData,
  zeroAddress,
} from 'viem'
import { EAS_ABI, SNEAKERS_V3_ABI } from '../src/chain.js'
import { SCHEMAS, attestRequest, attestationData, schemaUid } from '../src/eas.js'
import { JOBS_CRON, confirmJobs, sendJobs, releaseFor, runJobs } from '../src/jobs.js'
import { ensureV3, deployMarker } from '../src/v3.js'
import { V3_DEPLOYMENTS } from '../src/v3-deploy.js'
import { toServerEvent, sneakerSourceOf } from '../src/indexer.js'
import { executeOp } from '../src/handlers.js'
import { sneakerMetadata } from '../src/meta.js'
import { HttpError } from '../src/supabase.js'
import worker from '../src/index.js'
import {
  RELEASE_TYPES,
  STATS_SYNC_TYPES,
  VAULT_MINT_TYPES,
  releaseDomain,
  statsSyncMessage,
  vaultMintMessage,
} from '../src/typed.js'

const EAS = '0x4200000000000000000000000000000000000021'
const REGISTRY = '0x4200000000000000000000000000000000000020'
const V3 = V3_DEPLOYMENTS[91342].address
const RUNNER = '0x' + 'aa'.repeat(32)
const RUN = '0x' + 'bb'.repeat(32)
const WALLET = '0x' + '99'.repeat(20)

/** viem 이 컨트랙트 되돌림을 담아 주는 모양 — revertName 이 이름을 찾는다 */
function reverted(errorName) {
  const e = new Error(`reverted ${errorName}`)
  e.walk = (fn) => (fn({ data: { errorName } }) ? { data: { errorName } } : null)
  return e
}

function job(id, kind, payload) {
  return { job_id: id, kind, op_ref: '0x' + '0'.repeat(32) + String(id).padStart(32, '0'), payload }
}

const runPayload = { recipient: zeroAddress, runner: RUNNER, run: RUN, day: 20260927, distance_m: 5000, duration_sec: 1800 }
const badgePayload = { recipient: WALLET, runner: RUNNER, badge: 'DISTANCE_KM', value: 10, day: 20260927 }
const mintPayload = {
  account: RUNNER,
  faction: 'FIRE',
  rarity: 'EPIC',
  variant: 1,
  level: 2,
  efficiency_bps: 800,
  comfort_bps: 700,
  durability: '99.50',
  genesis_no: null,
  deadline_unix: 1790000000,
}

/** 가짜 체인 · 서버. events 에 일어난 순서를 적는다 */
function fakeWorld({ claim = [], accept = null, estimate = {}, balance = 10n ** 18n, pending = 7, signedFails = false } = {}) {
  const events = []
  const rpcCalls = []
  const relayer = privateKeyToAccount(generatePrivateKey())
  const signer = privateKeyToAccount(generatePrivateKey())
  const c = {
    chain: { id: 91342 },
    addresses: { eas: EAS, schemaRegistry: REGISTRY, sneakersV3: V3, sneakers: '0x' + '2'.repeat(40) },
    sneakerSigner: signer,
    relayer: {
      account: relayer,
      sendTransaction: async (x) => {
        events.push(['sendTransaction', x])
        return '0x' + 'dd'.repeat(32)
      },
    },
    publicClient: {
      getBalance: async () => balance,
      getTransactionCount: async ({ blockTag }) => (blockTag === 'pending' ? pending : pending),
      getBlock: async () => ({ baseFeePerGas: 300n, number: 1000n }),
      getBlockNumber: async () => 5000n,
      estimateMaxPriorityFeePerGas: async () => 1_000_000n,
      estimateContractGas: async (call) => {
        const jobId = call.functionName === 'attest' ? 'eas' : call.functionName
        const how = estimate[call.args?.[0]?.opId ?? ''] ?? estimate[jobId]
        if (how instanceof Error) throw how
        return 120_000n
      },
      sendRawTransaction: async ({ serializedTransaction }) => {
        events.push(['broadcast', serializedTransaction])
        return keccak256(serializedTransaction)
      },
    },
  }
  const deps = {
    clients: () => c,
    rpc: async (_env, fn, args) => {
      rpcCalls.push([fn, args])
      events.push(['rpc', fn])
      if (fn === 'attester_jobs_claim') return claim
      if (fn === 'attester_jobs_signed') {
        if (signedFails) throw new HttpError(502, 'down')
        return (accept ?? args.p_items.map((x) => x.id))
      }
      if (fn === 'attester_cursor_get') return null
      return null
    },
  }
  return { c, deps, events, rpcCalls, relayer, signer }
}

test('EAS 스키마 번호는 등록소와 같은 식(keccak256(스키마, resolver, 취소 가능))', () => {
  for (const schema of Object.values(SCHEMAS)) {
    assert.equal(schemaUid(schema), keccak256(encodePacked(['string', 'address', 'bool'], [schema, zeroAddress, true])))
  }
  assert.notEqual(schemaUid(SCHEMAS.RUN_PROOF), schemaUid(SCHEMAS.BADGE))
})

test('EAS 증명 데이터 — 서버 값 그대로, 위치 없음. 틀린 값은 만들지 않는다', () => {
  const [runner, run, day, distance, duration] = decodeAbiParameters(
    parseAbiParameters(SCHEMAS.RUN_PROOF),
    attestationData('RUN_PROOF', runPayload),
  )
  assert.deepEqual([runner, run, day, distance, duration], [RUNNER, RUN, 20260927, 5000, 1800])
  const [, badge, value] = decodeAbiParameters(parseAbiParameters(SCHEMAS.BADGE), attestationData('BADGE', badgePayload))
  assert.equal(badge, 'DISTANCE_KM')
  assert.equal(value, 10)
  assert.throws(() => attestationData('RUN_PROOF', { ...runPayload, runner: '0x12' }))
  assert.throws(() => attestationData('RUN_PROOF', { ...runPayload, distance_m: -1 }))
  assert.throws(() => attestationData('BADGE', { ...badgePayload, badge: 'drop table' }))
  // 받는 사람: 지갑이 있으면 그 지갑, 없으면 0
  assert.equal(attestRequest('BADGE', badgePayload).data.recipient, WALLET)
  assert.equal(attestRequest('RUN_PROOF', { ...runPayload, recipient: null }).data.recipient, zeroAddress)
  assert.equal(attestRequest('RUN_PROOF', runPayload).data.revocable, true)
})

test('보내기: 서명한 거래를 서버에 먼저 적고, 적힌 것만 차례 번호로 보낸다', async () => {
  const w = fakeWorld({
    claim: [job(1, 'RUN_PROOF', runPayload), job(2, 'BADGE', badgePayload), job(3, 'VAULT_MINT', mintPayload)],
  })
  const out = await sendJobs({}, w.deps, w.c, ['RUN_PROOF', 'BADGE', 'VAULT_MINT'])
  assert.equal(out.sent, 3)

  const signedAt = w.events.findIndex((e) => e[0] === 'rpc' && e[1] === 'attester_jobs_signed')
  const firstBroadcast = w.events.findIndex((e) => e[0] === 'broadcast')
  assert.ok(signedAt >= 0 && firstBroadcast > signedAt, '적은 뒤에 보낸다')

  const items = w.rpcCalls.find(([fn]) => fn === 'attester_jobs_signed')[1].p_items
  assert.deepEqual(items.map((x) => x.nonce), [7, 8, 9])
  for (const it of items) {
    assert.equal(it.tx, keccak256(it.raw), '거래 번호 = 서명한 거래의 해시')
    const tx = parseTransaction(it.raw)
    assert.equal(tx.nonce, it.nonce)
    assert.equal(tx.chainId, 91342)
    assert.equal(tx.maxFeePerGas, 300n * 2n + 1_000_000n)
  }
  const easTx = parseTransaction(items[0].raw)
  assert.equal(easTx.to.toLowerCase(), EAS.toLowerCase())
  const { args } = decodeFunctionData({ abi: EAS_ABI, data: easTx.data })
  assert.equal(args[0].schema, schemaUid(SCHEMAS.RUN_PROOF))

  // 금고 발행 — 신발 서명 키가 v3 영역에 서명한 서버 값 그대로
  const mintTx = parseTransaction(items[2].raw)
  assert.equal(mintTx.to.toLowerCase(), V3.toLowerCase())
  const mint = decodeFunctionData({ abi: SNEAKERS_V3_ABI, data: mintTx.data })
  assert.equal(mint.functionName, 'vaultMint')
  const [message, signature] = mint.args
  assert.equal(message.level, 2)
  assert.equal(message.durability, 9950)
  assert.equal(message.account, RUNNER)
  assert.ok(
    await verifyTypedData({
      address: w.signer.address,
      domain: releaseDomain(91342, V3, '3'),
      types: VAULT_MINT_TYPES,
      primaryType: 'VaultMint',
      message,
      signature,
    }),
  )
})

test('보내기: 체인이 받지 않을 일은 거두고, 번호를 비우지 않는다', async () => {
  const noChange = job(2, 'STATS_SYNC', { token_id: '1000001', level: 3, durability: '100.00', deadline_unix: 1790000000 })
  const w = fakeWorld({
    claim: [job(1, 'RUN_PROOF', runPayload), noChange, job(3, 'BADGE', badgePayload)],
    estimate: { syncStats: reverted('NoChange') },
  })
  const out = await sendJobs({}, w.deps, w.c, ['RUN_PROOF', 'BADGE', 'STATS_SYNC'])
  assert.equal(out.sent, 2)
  const released = w.rpcCalls.find(([fn]) => fn === 'attester_jobs_release')[1].p_items
  assert.deepEqual(released, [{ id: 2, error: 'NoChange', cancel: true }])
  const items = w.rpcCalls.find(([fn]) => fn === 'attester_jobs_signed')[1].p_items
  assert.deepEqual(items.map((x) => [x.id, x.nonce]), [[1, 7], [3, 8]])
})

test('보내기: 거둘 이유 · 미룰 이유', () => {
  assert.equal(releaseFor(1, 'NotInVault').cancel, true)
  assert.equal(releaseFor(1, 'OpAlreadyUsed').cancel, true)
  const tomorrow = releaseFor(1, 'DailyMintCapReached')
  assert.ok(!tomorrow.cancel && tomorrow.retry_sec > 60 && tomorrow.retry_sec <= 86_400 + 60)
  assert.equal(releaseFor(1, 'UnknownModel').retry_sec, 3600)
  assert.equal(releaseFor(1, 'InvalidSchema').retry_sec, 120)
  assert.equal(releaseFor(1, null, new Error('timeout')).cancel, undefined)
})

test('보내기: 스키마가 없으면 등록을 한 번 보내고 일은 조금 뒤로', async () => {
  const w = fakeWorld({ claim: [job(1, 'RUN_PROOF', runPayload)], estimate: { eas: reverted('InvalidSchema') } })
  const out = await sendJobs({}, w.deps, w.c, ['RUN_PROOF'])
  assert.equal(out.sent, 0)
  assert.equal(out.schemas.length, 1)
  const reg = w.events.find((e) => e[0] === 'sendTransaction')[1]
  assert.equal(reg.to, REGISTRY)
  assert.deepEqual(w.rpcCalls.find(([fn]) => fn === 'attester_jobs_release')[1].p_items, [
    { id: 1, error: 'InvalidSchema', retry_sec: 120 },
  ])
  // 표시를 먼저 남긴 뒤에 보낸다
  const marker = w.rpcCalls.findIndex(([fn]) => fn === 'attester_cursor_set')
  assert.ok(marker >= 0)
})

test('보내기: 서버가 받지 않은 일은 보내지 않고, 적기가 실패하면 아무것도 보내지 않는다', async () => {
  const partial = fakeWorld({ claim: [job(1, 'RUN_PROOF', runPayload), job(2, 'BADGE', badgePayload)], accept: [2] })
  const out = await sendJobs({}, partial.deps, partial.c, ['RUN_PROOF', 'BADGE'])
  assert.equal(out.sent, 1)
  assert.equal(partial.events.filter((e) => e[0] === 'broadcast').length, 1)

  const down = fakeWorld({ claim: [job(1, 'RUN_PROOF', runPayload)], signedFails: true })
  await assert.rejects(sendJobs({}, down.deps, down.c, ['RUN_PROOF']))
  assert.equal(down.events.filter((e) => e[0] === 'broadcast').length, 0)
})

test('보내기: 릴레이어 잔액이 모자라면 가져가지도 않는다(꺼내기 가스비를 남긴다)', async () => {
  const w = fakeWorld({ claim: [job(1, 'RUN_PROOF', runPayload)], balance: 10n ** 15n })
  const out = await sendJobs({}, w.deps, w.c, ['RUN_PROOF'])
  assert.equal(out.skipped, 'relayer balance low')
  assert.equal(w.rpcCalls.filter(([fn]) => fn === 'attester_jobs_claim').length, 0)
})

function receiptWithAttested(uid, block) {
  const topics = encodeEventTopics({
    abi: EAS_ABI,
    eventName: 'Attested',
    args: { recipient: zeroAddress, attester: '0x' + '33'.repeat(20), schemaUID: schemaUid(SCHEMAS.RUN_PROOF) },
  })
  return {
    status: 'success',
    blockNumber: block,
    logs: [
      {
        address: EAS,
        topics,
        data: encodeAbiParameters([{ type: 'bytes32' }], [uid]),
        blockNumber: block,
        logIndex: 0,
        transactionHash: '0x' + '01'.repeat(32),
        transactionIndex: 0,
        blockHash: '0x' + '02'.repeat(32),
        removed: false,
      },
    ],
  }
}

test('확인: 확정 · 되돌림 · 죽은 거래 · 다시 보내기 · 확정 블록 기다리기', async () => {
  const uid = '0x' + 'ee'.repeat(32)
  const old = new Date(Date.now() - 5 * 60 * 1000).toISOString()
  const rows = [
    { job_id: 1, kind: 'RUN_PROOF', nonce: 3, tx_hash: '0x' + 'a1'.repeat(32), raw_tx: '0xr1', sent_at: old, dead_checks: 0 },
    { job_id: 2, kind: 'BADGE', nonce: 4, tx_hash: '0x' + 'a2'.repeat(32), raw_tx: '0xr2', sent_at: old, dead_checks: 0 },
    { job_id: 3, kind: 'BADGE', nonce: 5, tx_hash: '0x' + 'a3'.repeat(32), raw_tx: '0xr3', sent_at: old, dead_checks: 0 },
    { job_id: 4, kind: 'RUN_PROOF', nonce: 6, tx_hash: '0x' + 'a4'.repeat(32), raw_tx: '0xr4', sent_at: old, dead_checks: 0 },
    { job_id: 5, kind: 'RUN_PROOF', nonce: 9, tx_hash: '0x' + 'a5'.repeat(32), raw_tx: '0xr5', sent_at: old, dead_checks: 0 },
    { job_id: 6, kind: 'RUN_PROOF', nonce: 10, tx_hash: '0x' + 'a6'.repeat(32), raw_tx: '0xr6', sent_at: new Date().toISOString(), dead_checks: 0 },
  ]
  const receipts = {
    ['0x' + 'a1'.repeat(32)]: receiptWithAttested(uid, 90n),
    ['0x' + 'a2'.repeat(32)]: { status: 'reverted', blockNumber: 91n, logs: [] },
    // a3 — 없음(다른 거래가 번호를 썼다)
    ['0x' + 'a4'.repeat(32)]: receiptWithAttested(uid, 150n), // 아직 확정 블록 뒤
  }
  const calls = []
  const rebroadcast = []
  const c = {
    chain: { id: 91342 },
    addresses: { eas: EAS, sneakersV3: V3 },
    relayer: { account: { address: '0x' + '44'.repeat(20) } },
    publicClient: {
      getTransactionCount: async ({ blockTag }) => (blockTag === 'latest' ? 9 : 9),
      getBlock: async ({ blockTag }) => (blockTag === 'safe' ? { number: 100n } : null),
      getTransactionReceipt: async ({ hash }) => {
        if (receipts[hash]) return receipts[hash]
        const e = new Error('not found')
        e.name = 'TransactionReceiptNotFoundError'
        throw e
      },
      sendRawTransaction: async ({ serializedTransaction }) => rebroadcast.push(serializedTransaction),
    },
  }
  const deps = {
    rpc: async (_env, fn, args) => {
      calls.push([fn, args])
      if (fn === 'attester_jobs_open') return rows
      if (fn === 'attester_jobs_result') return 'OK'
      return null
    },
  }
  const out = await confirmJobs({}, deps, c)
  const results = calls.find(([fn]) => fn === 'attester_jobs_result')[1].p_items
  assert.deepEqual(results, [
    { id: 1, status: 'CONFIRMED', block: 90, result: uid },
    { id: 2, status: 'RETRY', error: 'reverted' },
    { id: 3, status: 'DEAD' },
  ])
  // 4 는 확정 블록 뒤라 기다리고, 5 는 아직 블록에 없고 오래됐다 → 같은 거래를 다시 보낸다. 6 은 방금 보냈다
  assert.deepEqual(rebroadcast, ['0xr5'])
  assert.equal(out.confirmed, 1)
  assert.equal(out.dead, 1)
})

test('확인: 영수증을 못 읽은 것(RPC 오류)은 죽은 거래로 치지 않는다', async () => {
  const rows = [{ job_id: 1, kind: 'RUN_PROOF', nonce: 3, tx_hash: '0x' + 'a1'.repeat(32), raw_tx: '0x01', sent_at: new Date().toISOString() }]
  const calls = []
  const c = {
    chain: { id: 91342 },
    addresses: { eas: EAS },
    relayer: { account: { address: '0x' + '44'.repeat(20) } },
    publicClient: {
      getTransactionCount: async () => 9,
      getTransactionReceipt: async () => {
        throw new Error('rate limited')
      },
    },
  }
  const deps = { rpc: async (_env, fn, args) => (calls.push([fn, args]), fn === 'attester_jobs_open' ? rows : null) }
  await confirmJobs({}, deps, c)
  assert.equal(calls.filter(([fn]) => fn === 'attester_jobs_result').length, 0)
})

test('확인: 서버가 확정을 받지 않으면(토큰 어긋남) 멈춤을 건다', async () => {
  const rows = [{ job_id: 1, kind: 'VAULT_MINT', nonce: 3, tx_hash: '0x' + 'a1'.repeat(32), raw_tx: '0x01', sent_at: new Date().toISOString() }]
  const calls = []
  const c = {
    chain: { id: 91342 },
    addresses: { eas: EAS, sneakersV3: V3 },
    relayer: { account: { address: '0x' + '44'.repeat(20) } },
    guardian: null,
    publicClient: {
      getTransactionCount: async () => 9,
      getBlock: async () => ({ number: 100n }),
      getTransactionReceipt: async () => ({ status: 'success', blockNumber: 50n, logs: [] }),
    },
  }
  const deps = {
    clients: () => c,
    rpc: async (_env, fn, args) => {
      calls.push([fn, args])
      if (fn === 'attester_jobs_open') return rows
      if (fn === 'attester_jobs_result') return 'MISMATCH'
      return null
    },
  }
  await assert.rejects(confirmJobs({}, deps, c)) // 지킴이 키가 없어 컨트랙트는 못 멈췄다고 알린다
  assert.ok(calls.some(([fn]) => fn === 'attester_pause'))
})

test('v3 배포: 설정 주소 · 재료가 맞을 때만, 코드가 없을 때 한 번', async () => {
  const d = V3_DEPLOYMENTS[91342]
  const make = ({ code = '0x', marker = null, address = d.address } = {}) => {
    const sent = []
    const calls = []
    const c = {
      chain: { id: 91342 },
      addresses: { sneakersV3: address },
      relayer: { sendTransaction: async (x) => (sent.push(x), '0x' + 'de'.repeat(32)) },
      publicClient: { getCode: async () => code, getBlockNumber: async () => 10_000n },
    }
    const deps = {
      rpc: async (_env, fn, args) => {
        calls.push([fn, args])
        return fn === 'attester_cursor_get' ? marker : null
      },
    }
    return { c, deps, sent, calls }
  }
  assert.deepEqual(await ensureV3({}, make({ address: null }).deps, make({ address: null }).c), { ready: false, reason: 'not configured' })
  const wrong = make({ address: '0x' + '12'.repeat(20) })
  assert.equal((await ensureV3({}, wrong.deps, wrong.c)).reason, 'address mismatch')
  assert.equal(wrong.sent.length, 0)

  const live = make({ code: '0x6080' })
  assert.deepEqual(await ensureV3({}, live.deps, live.c), { ready: true })

  const fresh = make()
  const out = await ensureV3({}, fresh.deps, fresh.c)
  assert.ok(out.deploying)
  assert.equal(fresh.sent.length, 1)
  assert.equal(fresh.sent[0].to, d.proxy)
  assert.equal(fresh.sent[0].data, concat([d.salt, d.initcode]))
  const setAt = fresh.calls.findIndex(([fn]) => fn === 'attester_cursor_set')
  assert.deepEqual(fresh.calls[setAt][1], { p_name: deployMarker(91342, d.address), p_block: 10000 })

  const waiting = make({ marker: 9_900 })
  assert.equal((await ensureV3({}, waiting.deps, waiting.c)).reason, 'deploying')
  assert.equal(waiting.sent.length, 0)
})

test('v3 이벤트 → 서버 이벤트, 신발 작업은 토큰 번호로 컨트랙트를 고른다', () => {
  const log = (eventName, args) => ({ eventName, args, transactionHash: '0xt', logIndex: 2, blockNumber: 11n })
  assert.deepEqual(toServerEvent('sneakersV3', log('VaultMinted', { opId: '0xop', tokenId: 1000001n, account: RUNNER })).p_data, {
    op: '0xop',
    tokenId: '1000001',
    account: RUNNER,
  })
  assert.deepEqual(
    toServerEvent('sneakersV3', log('StatsSynced', { opId: '0xop', tokenId: 1000001n, level: 3, durability: 9500 })),
    { p_tx: '0xt', p_log: 2, p_block: 11, p_kind: 'STATS_SYNCED', p_data: { op: '0xop', tokenId: '1000001', level: 3, durability: 9500 } },
  )
  assert.equal(toServerEvent('sneakersV3', log('Released', { opId: '0xop', tokenId: 1000001n, to: WALLET })).p_kind, 'SNEAKER_RELEASED')
  assert.equal(toServerEvent('sneakersV3', log('Deposited', { tokenId: 1000001n, from: WALLET, account: RUNNER })).p_kind, 'SNEAKER_DEPOSITED')
  assert.equal(toServerEvent('sneakers', log('VaultMinted', { opId: '0xop', tokenId: 1n, account: RUNNER })), null)

  assert.equal(sneakerSourceOf({ kind: 'SNEAKER_WITHDRAW', token_id: '1000001' }), 'sneakersV3')
  assert.equal(sneakerSourceOf({ kind: 'SNEAKER_WITHDRAW', token_id: '12' }), 'sneakers')
  assert.equal(sneakerSourceOf({ kind: 'SNEAKER_WITHDRAW', token_id: null }), 'sneakers')
  assert.equal(sneakerSourceOf({ kind: 'BONUS_MINT', token_id: null }), 'sneakers')
})

test('꺼내기: 금고로 발행된 v3 신발은 v3 컨트랙트 · v3 서명 영역으로 보낸다', async () => {
  const signer = privateKeyToAccount(generatePrivateKey())
  const submitted = []
  const payload = {
    kind: 'SNEAKER_WITHDRAW',
    op_ref: '0x' + '0'.repeat(32) + 'cd'.repeat(16),
    wallet: WALLET,
    deadline_unix: 1790000000,
    token_id: '1000005',
    faction: 'FIRE',
    rarity: 'EPIC',
    variant: 1,
    level: 4,
    efficiency_bps: 800,
    comfort_bps: 700,
    durability: '90',
    genesis_no: null,
    transfer_locked: false,
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
  const sent = submitted[0]
  assert.equal(sent.address, V3)
  const [message, signature] = sent.args
  assert.equal(message.tokenId, 1000005n)
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

test('메타데이터: /v3/meta 는 v3 컨트랙트를 읽는다', async () => {
  const read = []
  const deps = {
    clients: () => ({
      addresses: { sneakers: '0x' + '2'.repeat(40), sneakersV3: V3 },
      publicClient: {
        readContract: async (x) => {
          read.push(x.address)
          return x.functionName === 'statsOf'
            ? { model: 21, rarity: 2, level: 3, efficiencyBps: 800, comfortBps: 700, durability: 9500, genesisNo: 0 }
            : false
        },
      },
    }),
  }
  const meta = await sneakerMetadata({}, deps, '1000001', '3')
  assert.ok(read.every((a) => a === V3))
  assert.ok(meta.name.endsWith('#1000001'))
  assert.equal(meta.attributes.find((a) => a.trait_type === 'Level').value, 3)
})

test('v3 서명 재료 — 계정 가명 · v3 토큰만', () => {
  assert.throws(() => vaultMintMessage({ ...mintPayload, kind: 'VAULT_MINT', op_ref: RUN, account: '0x' + '0'.repeat(64) }))
  assert.throws(() => statsSyncMessage({ kind: 'STATS_SYNC', op_ref: RUN, token_id: '12', level: 2, durability: 90, deadline_unix: 1 }))
  const s = statsSyncMessage({ kind: 'STATS_SYNC', op_ref: RUN, token_id: '1000001', level: 2, durability: '87.25', deadline_unix: 5 })
  assert.deepEqual(s, { opId: RUN, tokenId: 1000001n, level: 2, durability: 8725, deadline: 5n })
  assert.ok(STATS_SYNC_TYPES.StatsSync.length === 5)
})

test('두 번째 cron 은 체인 기록 보내기만 한다 — wrangler.toml 과 같은 식', async () => {
  const fs = await import('node:fs')
  const toml = fs.readFileSync(new URL('../wrangler.toml', import.meta.url), 'utf8')
  assert.ok(toml.includes(`"${JOBS_CRON}"`))
  const env = { DISTRIBUTOR_ADDRESS: '0x1', SNEAKERS_ADDRESS: '0x2', VAULT_ADDRESS: '0x3', JOBS_START_DELAY_SEC: '0' }
  const waits = []
  await worker.scheduled({ cron: JOBS_CRON }, env, { waitUntil: (p) => waits.push(p) })
  assert.equal(waits.length, 1)
  await waits[0] // 키가 없어 실패해도 잡아서 기록만 한다
})

test('2분 작업: 한 단계가 실패해도 다음 단계는 한다', async () => {
  const calls = []
  const c = {
    chain: { id: 91342 },
    addresses: { eas: null, sneakersV3: null },
    relayer: { account: { address: '0x' + '44'.repeat(20) } },
    publicClient: {},
  }
  const deps = {
    clients: () => c,
    rpc: async (_env, fn) => {
      calls.push(fn)
      if (fn === 'attester_jobs_open') throw new Error('down')
      return null
    },
  }
  const out = await runJobs({}, deps)
  assert.deepEqual(out.confirm, { error: true })
  assert.equal(out.v3.reason, 'not configured')
  assert.deepEqual(out.send, { skipped: 'nothing to send' })
})
