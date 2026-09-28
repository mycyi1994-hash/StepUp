import { test } from 'node:test'
import assert from 'node:assert/strict'
import { privateKeyToAccount, generatePrivateKey } from 'viem/accounts'
import { verifyTypedData } from 'viem'
import { linkWallet, executeOp } from '../src/handlers.js'
import { HttpError } from '../src/supabase.js'
import { toServerEvent, indexEvents, expireOps, pauseAll, keepPaused } from '../src/indexer.js'
import { NONCE_CLASH, ALREADY_KNOWN, nonceBackoffMs } from '../src/handlers.js'
import { walletLinkMessage, RELEASE_TYPES, releaseDomain } from '../src/typed.js'

const USER = { id: 'f1f1f1f1-f1f1-f1f1-f1f1-f1f1f1f1f1f1' }
const OP = '11111111-2222-3333-4444-555555555555'
const env = { DRIP_WEI: '0' }

function req(body, auth = 'Bearer t') {
  return new Request('https://attester.test/x', {
    method: 'POST',
    headers: { authorization: auth, 'content-type': 'application/json' },
    body: JSON.stringify(body),
  })
}

function fakeDeps({ payload, rpcCalls = [], submitted = [], firstWallet = null } = {}) {
  const signer = privateKeyToAccount(generatePrivateKey())
  const attester = privateKeyToAccount(generatePrivateKey())
  return {
    rpcCalls,
    submitted,
    signer,
    getUser: async (_env, request) => {
      if (!request.headers.get('authorization')) throw new HttpError(401, '로그인이 필요합니다')
      return USER
    },
    rpc: async (_env, fn, args) => {
      rpcCalls.push([fn, args])
      if (fn === 'attester_op_payload') return payload ? [payload] : []
      if (fn === 'attester_wallet_link') return firstWallet
      return null
    },
    clients: () => ({
      chain: { id: 91342 },
      addresses: { distributor: '0x' + '1'.repeat(40), sneakers: '0x' + '2'.repeat(40) },
      attester,
      sneakerSigner: signer,
      publicClient: {
        readContract: async () => 7n,
        simulateContract: async (x) => ({ request: x }),
        // 새 지갑은 0, 가스비를 대는 relayer 는 넉넉히
        getBalance: async ({ address }) => (address === '0x' + '3'.repeat(40) ? 10n ** 18n : 0n),
      },
      relayer: {
        account: { address: '0x' + '3'.repeat(40) },
        sendTransaction: async (x) => {
          submitted.push(x)
          return '0x' + 'cd'.repeat(32)
        },
        writeContract: async (x) => {
          submitted.push(x)
          return '0x' + 'ab'.repeat(32)
        },
      },
    }),
  }
}

test('지갑 연결: 그 계정의 문장에 한 서명만 받고 서버에 넘긴다', async () => {
  const wallet = privateKeyToAccount(generatePrivateKey())
  const nonce = 'a'.repeat(32)
  const signature = await wallet.signMessage({ message: walletLinkMessage(USER.id, nonce) })
  const deps = fakeDeps()
  const out = await linkWallet(req({ address: wallet.address, nonce, signature }), env, deps)
  assert.equal(out.ok, true)
  assert.deepEqual(deps.rpcCalls[0], [
    'attester_wallet_link',
    { p_user: USER.id, p_address: wallet.address, p_nonce: nonce },
  ])

  // 다른 계정 번호로 만든 문장의 서명은 받지 않는다
  const other = await wallet.signMessage({ message: walletLinkMessage('someone-else', nonce) })
  await assert.rejects(
    linkWallet(req({ address: wallet.address, nonce, signature: other }), env, fakeDeps()),
    (e) => e.status === 400,
  )
})

test('작업 실행: 로그인 없이는 안 되고, 서버가 주지 않은 작업은 서명하지 않는다', async () => {
  await assert.rejects(executeOp(req({}, ''), env, fakeDeps(), OP), (e) => e.status === 401)
  await assert.rejects(executeOp(req({}), env, fakeDeps(), 'not-a-uuid'), (e) => e.status === 400)
  const deps = fakeDeps({ payload: null })
  await assert.rejects(executeOp(req({}), env, deps, OP), (e) => e.status === 409)
  assert.deepEqual(deps.rpcCalls[0], ['attester_op_payload', { p_op: OP, p_user: USER.id }])
  assert.equal(deps.submitted.length, 0)
})

test('작업 실행: 서버 값으로 신발 서명 → 대납 제출 → 제출 기록. 요청 본문 값은 쓰지 않는다', async () => {
  const payload = {
    kind: 'SNEAKER_WITHDRAW',
    op_ref: '0x' + '0'.repeat(32) + 'cd'.repeat(16),
    wallet: '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
    deadline_unix: 1790000000,
    token_id: '7',
    faction: 'FIRE',
    rarity: 'EPIC',
    variant: 1,
    level: 5,
    efficiency_bps: 800,
    comfort_bps: 700,
    durability: '90.5',
    genesis_no: null,
    transfer_locked: false,
  }
  const deps = fakeDeps({ payload })
  const out = await executeOp(req({ level: 30, wallet: '0xbad' }), env, deps, OP)
  assert.equal(out.status, 'SUBMITTED')
  const sent = deps.submitted[0]
  assert.equal(sent.functionName, 'release')
  const [message, signature] = sent.args
  assert.equal(message.level, 5)
  assert.equal(message.to, payload.wallet)
  assert.ok(
    await verifyTypedData({
      address: deps.signer.address,
      domain: releaseDomain(91342, '0x' + '2'.repeat(40)),
      types: RELEASE_TYPES,
      primaryType: 'Release',
      message,
      signature,
    }),
  )
  assert.deepEqual(deps.rpcCalls.at(-1), ['attester_op_submitted', { p_op: OP, p_tx: '0x' + 'ab'.repeat(32) }])
})

test('체인 이벤트 → 서버 이벤트', () => {
  const log = (eventName, args) => ({ eventName, args, transactionHash: '0xt', logIndex: 3, blockNumber: 10n })
  assert.deepEqual(toServerEvent('vault', log('Deposited', { account: '0xacc', from: '0xF', amount: 125n * 10n ** 17n })), {
    p_tx: '0xt',
    p_log: 3,
    p_block: 10,
    p_kind: 'SUP_DEPOSITED',
    p_data: { account: '0xacc', amount: '12.5' },
  })
  assert.equal(
    toServerEvent('sneakers', log('Deposited', { account: '0xacc', from: '0xABC', tokenId: 9n })).p_data.from,
    '0xabc',
  )
  assert.deepEqual(toServerEvent('sneakers', log('Released', { opId: '0xop', tokenId: 9n, to: '0xABC' })).p_data, {
    op: '0xop',
    tokenId: '9',
    to: '0xabc',
  })
  // 받는 지갑과 금액도 넘긴다 — 서버가 작업 기록과 맞는지 본다
  assert.deepEqual(
    toServerEvent('distributor', log('Claimed', { sessionHash: '0xop', runner: '0xABC', amount: 125n * 10n ** 17n })).p_data,
    { op: '0xop', runner: '0xabc', amount: '12.5' },
  )
  assert.deepEqual(toServerEvent('sneakers', log('OpCancelled', { opId: '0xop' })), {
    p_tx: '0xt',
    p_log: 3,
    p_block: 10,
    p_kind: 'OP_CANCELLED',
    p_data: { op: '0xop' },
  })
  assert.equal(toServerEvent('vault', log('Recycled', {})), null)
})

test('지갑 연결: 가스비는 계정의 첫 지갑에만 보낸다', async () => {
  const dripEnv = { DRIP_WEI: '200000000000000' }
  const link = async (firstWallet) => {
    const wallet = privateKeyToAccount(generatePrivateKey())
    const nonce = 'b'.repeat(32)
    const signature = await wallet.signMessage({ message: walletLinkMessage(USER.id, nonce) })
    const deps = fakeDeps({ firstWallet })
    const out = await linkWallet(req({ address: wallet.address, nonce, signature }), dripEnv, deps)
    return { out, sent: deps.submitted.length }
  }
  const first = await link(true)
  assert.equal(first.sent, 1)
  assert.ok(first.out.drip)
  const again = await link(false)
  assert.equal(again.sent, 0)
  assert.equal(again.out.drip, null)
})

test('만료: 한 작업이 실패해도 뒤의 작업은 계속 되돌린다', async () => {
  const calls = []
  // 마진(30분)이 지난 서명 작업 — 체인 시각(safe 블록)도 기한을 넘었다
  const deadline = new Date(Date.now() - 3_600_000).toISOString()
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x1', sneakers: '0x2' },
      publicClient: {
        getBlock: async () => ({ number: 5n, timestamp: BigInt(Math.floor(Date.now() / 1000)) }),
        readContract: async () => false,
      },
    }),
    rpc: async (_env, fn, args) => {
      calls.push([fn, args])
      if (fn === 'attester_due_ops') {
        return [
          { op_id: 'bad', op_ref: '0x1', kind: 'SUP_WITHDRAW', status: 'SIGNED', deadline, early: false },
          { op_id: 'good', op_ref: '0x2', kind: 'SNEAKER_WITHDRAW', status: 'SUBMITTED', deadline, early: false },
        ]
      }
      if (fn === 'attester_op_expire' && args.p_op === 'bad') throw new Error('로그인이 필요합니다')
      return 'EXPIRED'
    },
  }
  const out = await expireOps({}, deps)
  assert.equal(out.expired, 1)
  assert.equal(out.failed, 1)
  assert.ok(calls.some(([fn, a]) => fn === 'attester_op_expire' && a.p_op === 'good'))
})

test('이벤트: 서버가 모르는 작업이면 컨트랙트를 멈춘다', async () => {
  const calls = []
  const paused = []
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x' + '1'.repeat(40) },
      publicClient: {
        getBlockNumber: async () => 1000n,
        getBlock: async ({ blockTag, blockNumber }) => ({ number: blockTag === 'safe' ? 970n : blockNumber }),
        waitForTransactionReceipt: async () => ({ status: 'success' }),
        getContractEvents: async () => [
          { eventName: 'Claimed', args: { sessionHash: '0xop', runner: '0xA', amount: 10n ** 18n }, transactionHash: '0xt', logIndex: 0, blockNumber: 900n },
        ],
        readContract: async () => false,
      },
      guardian: {
        writeContract: async (x) => {
          paused.push(x.address)
          return '0x' + 'ee'.repeat(32)
        },
      },
    }),
    rpc: async (_env, fn, args) => {
      calls.push([fn, args])
      if (fn === 'attester_cursor_get') return args.p_name.includes('@') ? null : 899
      if (fn === 'attester_chain_event') return 'UNKNOWN_OP'
      return null
    },
  }
  await indexEvents({}, deps)
  assert.ok(calls.some(([fn]) => fn === 'attester_pause'))
  assert.deepEqual(paused, ['0x' + '1'.repeat(40)])
  // 멈춘 뒤에도 커서는 옮긴다 — 같은 이벤트로 매분 다시 멈추지 않게
  assert.ok(calls.some(([fn]) => fn === 'attester_cursor_set'))
})

test('정지: 한 컨트랙트가 실패해도 나머지는 멈추고, 문제가 난 컨트랙트부터 멈춘다', async () => {
  const order = []
  const addresses = { distributor: '0x' + '1'.repeat(40), sneakers: '0x' + '2'.repeat(40), vault: '0x' + '4'.repeat(40) }
  const deps = {
    clients: () => ({
      addresses,
      publicClient: {
        readContract: async () => false,
        waitForTransactionReceipt: async () => ({ status: 'success' }),
      },
      guardian: {
        writeContract: async (x) => {
          order.push(x.address)
          if (x.address === addresses.sneakers) throw new Error('insufficient funds')
          return '0x' + 'ee'.repeat(32)
        },
      },
    }),
    rpc: async (_env, fn) => {
      if (fn === 'attester_pause') throw new Error('supabase down')
      if (fn === 'attester_chain_paused') return true
      return null
    },
  }
  await assert.rejects(pauseAll({}, deps, '검사', 'vault'))
  // 서버 알리기가 실패해도 컨트랙트는 멈추려 했고, vault 가 먼저, sneakers 가 실패해도 distributor 까지
  assert.deepEqual(order, [addresses.vault, addresses.distributor, addresses.sneakers])
  // 서버가 멈춰 있으면 매분 다시 멈춘다
  order.length = 0
  const out = await keepPaused({}, deps)
  assert.equal(out.paused, true)
  assert.deepEqual(out.failed, ['sneakers'])
})

test('서버 호출: 로그인 토큰이 만료됐으면 한 번 새로 받고, 내부 오류 문구는 사용자에게 보이지 않는다', async () => {
  const { rpc } = await import('../src/supabase.js')
  const env = { SUPABASE_URL: 'https://s.test', SUPABASE_ANON_KEY: 'anon', ATTESTER_EMAIL: 'a@b', ATTESTER_PASSWORD: 'p' }
  let logins = 0
  let calls = 0
  const fetchImpl = async (url) => {
    if (url.includes('/auth/v1/token')) {
      logins += 1
      return new Response(JSON.stringify({ access_token: `t${logins}`, expires_in: 3600 }))
    }
    calls += 1
    if (calls === 1) return new Response('{"message":"JWT expired"}', { status: 401 })
    if (calls === 2) return new Response('"ok"')
    return new Response('{"code":"PGRST202","message":"Could not find the function public.attester_x"}', { status: 404 })
  }
  assert.equal(await rpc(env, 'attester_cursor_get', {}, fetchImpl), 'ok')
  assert.equal(logins, 2)
  await assert.rejects(rpc(env, 'attester_cursor_get', {}, fetchImpl), (e) => e.status === 502 && !e.message.includes('attester_x'))
  // 성공 응답인데 본문이 깨졌으면 성공으로 치지 않는다(인덱서가 이벤트를 건너뛰지 않게)
  const broken = async (url) =>
    url.includes('/auth/v1/token')
      ? new Response(JSON.stringify({ access_token: 't', expires_in: 3600 }))
      : new Response('<html>gateway</html>', { status: 200 })
  await assert.rejects(rpc(env, 'attester_chain_event', {}, broken), (e) => e.status === 502)
})

test('어테스터 로그인: 한꺼번에 불러도 한 번만, 실패하면 잠시 다시 하지 않는다', async () => {
  const { rpc, resetAttesterLogin } = await import('../src/supabase.js')
  resetAttesterLogin()
  const env = { SUPABASE_URL: 'https://s.test', SUPABASE_ANON_KEY: 'anon', ATTESTER_EMAIL: 'a@b', ATTESTER_PASSWORD: 'p' }
  let logins = 0
  const failing = async (url) => {
    if (url.includes('/auth/v1/token')) {
      logins += 1
      await new Promise((r) => setTimeout(r, 5))
      return new Response('{"code":429,"error_code":"over_request_rate_limit"}', { status: 429 })
    }
    return new Response('"ok"')
  }
  const results = await Promise.allSettled(
    ['attester_a', 'attester_b', 'attester_c', 'attester_d'].map((fn) => rpc(env, fn, {}, failing)),
  )
  assert.ok(results.every((r) => r.status === 'rejected' && r.reason.status === 502))
  assert.equal(logins, 1)
  // 실패 뒤에는 로그인 요청을 보내지 않고 바로 실패한다(제한을 계속 채우지 않게)
  await assert.rejects(rpc(env, 'attester_e', {}, failing), (e) => e.status === 502)
  assert.equal(logins, 1)
  // 로그인이 되면 여러 호출이 토큰 하나를 같이 쓴다
  resetAttesterLogin()
  const ok = async (url) => {
    if (url.includes('/auth/v1/token')) {
      logins += 1
      await new Promise((r) => setTimeout(r, 5))
      return new Response(JSON.stringify({ access_token: 't', expires_in: 3600 }))
    }
    return new Response('"ok"')
  }
  const values = await Promise.all(['attester_a', 'attester_b', 'attester_c'].map((fn) => rpc(env, fn, {}, ok)))
  assert.deepEqual(values, ['ok', 'ok', 'ok'])
  assert.equal(logins, 2)
  resetAttesterLogin()
})

test('RPC 수 제한 응답이면 잠깐 쉬었다 다시 보낸다 — 긴 응답 · 다른 오류는 그대로', async () => {
  const { rpcFetch } = await import('../src/chain.js')
  const waits = []
  const answers = [
    new Response('{"jsonrpc":"2.0","id":1,"error":{"code":-32016,"message":"over rate limit"}}'),
    new Response('Too Many Requests', { status: 429 }),
    new Response('{"jsonrpc":"2.0","id":1,"result":"0x1"}'),
  ]
  let sent = 0
  const f = rpcFetch(async () => answers[sent++], { sleep: async (ms) => waits.push(ms) })
  const res = await f('https://rpc.test', { method: 'POST', body: '{}' })
  assert.equal(await res.text(), '{"jsonrpc":"2.0","id":1,"result":"0x1"}')
  assert.equal(sent, 3)
  assert.equal(waits.length, 2)
  assert.ok(waits[1] > waits[0])
  // 두 번 다시 보내도 제한이면 그 응답을 넘긴다(viem 이 오류로 올린다)
  sent = 0
  const always = rpcFetch(async () => (sent++, new Response('over rate limit')), { sleep: async () => {} })
  assert.equal(await (await always('u', {})).text(), 'over rate limit')
  assert.equal(sent, 3)
  // 다른 오류 · 긴 정상 응답은 바로 넘긴다
  sent = 0
  const other = rpcFetch(async () => (sent++, new Response('{"error":{"code":-32000,"message":"execution reverted"}}')), { sleep: async () => {} })
  await other('u', {})
  assert.equal(sent, 1)
  sent = 0
  const long = rpcFetch(async () => (sent++, new Response('x'.repeat(5000) + 'rate limit')), { sleep: async () => {} })
  await long('u', {})
  assert.equal(sent, 1)
})

test('작업 실행: 로그인한 사용자별로도 요청 수를 센다', async () => {
  const deps = fakeDeps()
  deps.limitUser = async (_env, id) => id === USER.id
  await assert.rejects(executeOp(req({}), env, deps, OP), (e) => e.status === 429)
})

test('이벤트: 한 번에 넘기는 수를 넘으면 넘긴 이벤트 바로 뒤까지만 커서를 옮긴다', async () => {
  const events = Array.from({ length: 60 }, (_, i) => ({
    eventName: 'Claimed', args: { sessionHash: `0x${i}`, runner: '0xA', amount: 1n },
    transactionHash: `0xt${i}`, logIndex: 0, blockNumber: 101n + BigInt(i),
  }))
  const cursors = []
  let sent = 0
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x' + '1'.repeat(40) },
      publicClient: {
        getBlock: async ({ blockTag, blockNumber }) => ({ number: blockTag === 'safe' ? 500n : blockNumber }),
        getContractEvents: async () => events,
      },
    }),
    rpc: async (_env, fn, args) => {
      if (fn === 'attester_cursor_get') return args.p_name.includes('@') ? null : 100
      if (fn === 'attester_cursor_set') cursors.push([args.p_name, args.p_block])
      if (fn === 'attester_chain_event') { sent += 1; return 'OK' }
      return null
    },
  }
  await indexEvents({ INDEX_EVENTS_PER_RUN: '25' }, deps)
  assert.equal(sent, 25)
  // 26번째 이벤트(블록 126, 로그 0)부터 다음 실행에
  assert.deepEqual(cursors, [['distributor@:0x' + '1'.repeat(40), 126 * 100000]])
})

test('이벤트: 한 블록에 이벤트가 몰려 있어도 블록 안에서 이어 간다', async () => {
  const events = Array.from({ length: 50 }, (_, i) => ({
    eventName: 'Claimed', args: { sessionHash: `0x${i}`, runner: '0xA', amount: 1n },
    transactionHash: `0xt${i}`, logIndex: i, blockNumber: 300n,
  }))
  let pos = null
  const seen = []
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x' + '1'.repeat(40) },
      publicClient: {
        getBlock: async ({ blockTag, blockNumber }) => ({ number: blockTag === 'safe' ? 300n : blockNumber }),
        getContractEvents: async () => events,
      },
    }),
    rpc: async (_env, fn, args) => {
      if (fn === 'attester_cursor_get') return args.p_name.includes('@') ? pos : 299
      if (fn === 'attester_cursor_set') pos = args.p_block
      if (fn === 'attester_chain_event') { seen.push(args.p_tx); return 'OK' }
      return null
    },
  }
  for (let run = 0; run < 3; run++) await indexEvents({ INDEX_EVENTS_PER_RUN: '20' }, deps)
  // 세 번에 걸쳐 50개를 한 번씩만 넘기고, 커서는 다음 블록 처음으로
  assert.equal(seen.length, 50)
  assert.equal(new Set(seen).size, 50)
  assert.equal(pos, 301 * 100000)
})

test('이벤트: 중간에 실패하면 실패한 이벤트 앞까지는 커서를 옮긴다', async () => {
  const events = [0, 1, 2].map((i) => ({
    eventName: 'Claimed', args: { sessionHash: `0x${i}`, runner: '0xA', amount: 1n },
    transactionHash: `0xt${i}`, logIndex: 0, blockNumber: 201n + BigInt(i),
  }))
  const cursors = []
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x' + '1'.repeat(40) },
      publicClient: {
        getBlock: async ({ blockTag, blockNumber }) => ({ number: blockTag === 'safe' ? 500n : blockNumber }),
        getContractEvents: async () => events,
      },
    }),
    rpc: async (_env, fn, args) => {
      if (fn === 'attester_cursor_get') return args.p_name.includes('@') ? null : 200
      if (fn === 'attester_cursor_set') cursors.push(args.p_block)
      if (fn === 'attester_chain_event' && args.p_tx === '0xt2') throw new Error('too many subrequests')
      return 'OK'
    },
  }
  const out = await indexEvents({}, deps)
  assert.equal(out.distributor.error, true)
  // 실패한 이벤트(블록 203, 로그 0) 앞까지
  assert.deepEqual(cursors, [203 * 100000])
})

test('이벤트: safe · finalized 블록을 못 받으면 이번에는 읽지 않는다', async () => {
  const calls = []
  const deps = {
    clients: () => ({
      addresses: { distributor: '0x' + '1'.repeat(40) },
      publicClient: {
        getBlock: async () => { throw new Error('rpc down') },
        getBlockNumber: async () => 1000n,
      },
    }),
    rpc: async (_env, fn) => { calls.push(fn); return null },
  }
  const out = await indexEvents({}, deps)
  assert.ok(out.skipped)
  assert.deepEqual(calls, [])
})

test('체인 오류: 컨트랙트 오류를 이름으로 읽는다', async () => {
  const { decodeErrorResult } = await import('viem')
  const { DISTRIBUTOR_ABI, SNEAKERS_ABI } = await import('../src/chain.js')
  // SessionAlreadyClaimed(bytes32) 의 선택자 0x68825535
  const data = '0x68825535' + '00'.repeat(32)
  assert.equal(decodeErrorResult({ abi: DISTRIBUTOR_ABI, data }).errorName, 'SessionAlreadyClaimed')
  for (const name of ['OpAlreadyUsed', 'DailyMintCapReached', 'EnforcedPause']) {
    assert.ok(SNEAKERS_ABI.some((x) => x.type === 'error' && x.name === name), name)
  }
})

test('만료: 기한이 지난 서명 작업은 safe 블록이 기한을 넘고 안 쓰였을 때만 바로 되돌린다', async () => {
  const deadline = new Date(Date.now() - 60_000).toISOString()
  const deadlineSec = BigInt(Math.ceil(Date.parse(deadline) / 1000))
  const run = async ({ safeTs, usedAtSafe, status = 'SIGNED' }) => {
    const calls = []
    const reads = []
    const deps = {
      clients: () => ({
        addresses: { distributor: '0x1', sneakers: '0x2' },
        publicClient: {
          getBlock: async () => ({ number: 77n, timestamp: safeTs }),
          readContract: async (a) => { reads.push(a); return usedAtSafe },
        },
      }),
      rpc: async (_env, fn, args) => {
        calls.push([fn, args])
        if (fn === 'attester_due_ops') return [{ op_id: 'op', op_ref: '0x9', kind: 'SUP_WITHDRAW', status, deadline, early: true }]
        return 'EXPIRED'
      },
    }
    const out = await expireOps({}, deps)
    return { out, calls, reads }
  }
  // safe 블록이 기한을 넘었고 그 블록에서 안 쓰였다 → 바로 되돌린다(그 블록 번호로 읽는다)
  let r = await run({ safeTs: deadlineSec + 5n, usedAtSafe: false })
  assert.equal(r.out.expired, 1)
  assert.equal(r.reads[0].blockNumber, 77n)
  assert.deepEqual(r.calls.find(([fn]) => fn === 'attester_op_expire')[1],
    { p_op: 'op', p_used_on_chain: false, p_safe_past_deadline: true })
  // safe 블록이 아직 기한 전(같은 초 포함)이면 기다린다
  r = await run({ safeTs: deadlineSec, usedAtSafe: false })
  assert.equal(r.out.expired, 0)
  assert.ok(!r.calls.some(([fn]) => fn === 'attester_op_expire'))
  // safe 블록에서 쓰였다 → 되돌리지 않는다(이벤트가 확정한다)
  r = await run({ safeTs: deadlineSec + 5n, usedAtSafe: true })
  assert.equal(r.out.pendingOnChain, 1)
  assert.ok(!r.calls.some(([fn]) => fn === 'attester_op_expire'))
  // 서명 전 작업은 체인에 있을 수 없다 — 예전 길로 되돌린다(확인 표시 없이)
  r = await run({ safeTs: 0n, usedAtSafe: false, status: 'RESERVED' })
  assert.equal(r.out.expired, 1)
  assert.deepEqual(r.calls.find(([fn]) => fn === 'attester_op_expire')[1], { p_op: 'op', p_used_on_chain: false })
})

test('보내기: 같은 번호(nonce) 충돌을 알아보고 흔들린 간격으로 다시 보낸다', () => {
  for (const msg of ['nonce too low', 'replacement transaction underpriced', 'Nonce has already been used']) {
    assert.ok(NONCE_CLASH.test(msg), msg)
  }
  assert.ok(!NONCE_CLASH.test('execution reverted: PayoutCapReached'))
  // 같은 거래를 이미 받아 둔 것 — 번호 충돌이 아니다
  assert.ok(!NONCE_CLASH.test('already known'))
  assert.ok(ALREADY_KNOWN.test('already known'))
  assert.equal(nonceBackoffMs(0, () => 0), 700)
  assert.equal(nonceBackoffMs(2, () => 0.999), 2100 + 599)
})

const SNEAKER_PAYLOAD = {
  kind: 'SNEAKER_WITHDRAW',
  op_ref: '0x' + '0'.repeat(32) + 'cd'.repeat(16),
  wallet: '0xaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa',
  deadline_unix: 1790000000,
  token_id: '7',
  faction: 'FIRE',
  rarity: 'EPIC',
  variant: 1,
  level: 5,
  efficiency_bps: 800,
  comfort_bps: 700,
  durability: '90.5',
  genesis_no: null,
  transfer_locked: false,
}

/** 보내기가 차례로 [answers] 를 겪는 작업 실행 — { reject } 는 노드의 거절, 문자열은 거래 번호 */
function sendingDeps(answers) {
  const deps = fakeDeps({ payload: SNEAKER_PAYLOAD })
  const sends = []
  const clients = deps.clients()
  clients.relayer.writeContract = async (x) => {
    sends.push(x)
    const next = answers[sends.length - 1]
    if (next?.reject) throw Object.assign(new Error('send failed'), { details: next.reject })
    return next
  }
  deps.clients = () => clients
  return { deps, sends }
}

test('보내기: 번호 충돌은 새 번호로 다시 보내고, 성공한 거래를 제출로 적는다', async () => {
  const { deps, sends } = sendingDeps([{ reject: 'nonce too low' }, '0x' + 'ef'.repeat(32)])
  const out = await executeOp(req({}), env, deps, OP)
  assert.equal(sends.length, 2)
  assert.equal(out.tx, '0x' + 'ef'.repeat(32))
  assert.deepEqual(deps.rpcCalls.at(-1), ['attester_op_submitted', { p_op: OP, p_tx: '0x' + 'ef'.repeat(32) }])
})

test('보내기: 노드가 이미 받아 둔 거래면 다시 보내지 않는다(되돌아갈 두 번째 거래를 만들지 않게)', async () => {
  const { deps, sends } = sendingDeps([{ reject: 'already known' }, '0x' + 'ef'.repeat(32)])
  await assert.rejects(executeOp(req({}), env, deps, OP), (e) => e.status === 409 && /이미 체인에 보낸/.test(e.message))
  assert.equal(sends.length, 1)
  assert.ok(!deps.rpcCalls.some(([fn]) => fn === 'attester_op_submitted'))
})

test('만료: 마진이 지나도 체인 시각이 기한 전이면(시퀀서가 멈춤) 서명한 작업을 되돌리지 않는다', async () => {
  const deadline = new Date(Date.now() - 3_600_000).toISOString() // 마진(30분)보다 오래 지났다
  const deadlineSec = BigInt(Math.ceil(Date.parse(deadline) / 1000))
  const run = async ({ header, status = 'SIGNED' }) => {
    const calls = []
    const deps = {
      clients: () => ({
        addresses: { distributor: '0x1', sneakers: '0x2' },
        publicClient: {
          getBlock: async () => {
            if (!header) throw new Error('safe block unavailable')
            return header
          },
          readContract: async () => false, // 아직 체인에서 안 쓰였다
        },
      }),
      rpc: async (_env, fn, args) => {
        calls.push([fn, args])
        if (fn === 'attester_due_ops') return [{ op_id: 'op', op_ref: '0x9', kind: 'SUP_WITHDRAW', status, deadline, early: false }]
        return 'EXPIRED'
      },
    }
    const out = await expireOps({}, deps)
    return { out, expired: calls.filter(([fn]) => fn === 'attester_op_expire') }
  }
  // 체인이 기한 전 시각에 멈춰 있다 — 풀의 거래가 따라잡는 블록에 들어갈 수 있으니 기다린다
  let r = await run({ header: { number: 9n, timestamp: deadlineSec - 600n } })
  assert.equal(r.out.expired, 0)
  assert.equal(r.out.waiting, 1)
  assert.equal(r.expired.length, 0)
  // safe 블록을 못 읽으면 되돌리지 않는다
  r = await run({ header: null })
  assert.equal(r.expired.length, 0)
  // 체인 시각이 기한을 넘었다 — 되돌린다
  r = await run({ header: { number: 9n, timestamp: deadlineSec + 1n } })
  assert.equal(r.out.expired, 1)
  assert.deepEqual(r.expired[0][1], { p_op: 'op', p_used_on_chain: false })
  // 서명 전 작업은 체인에 있을 수 없다 — 체인 시각과 상관없이 되돌린다
  r = await run({ header: null, status: 'RESERVED' })
  assert.equal(r.out.expired, 1)
})
