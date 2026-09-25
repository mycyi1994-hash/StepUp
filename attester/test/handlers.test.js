import { test } from 'node:test'
import assert from 'node:assert/strict'
import { privateKeyToAccount, generatePrivateKey } from 'viem/accounts'
import { verifyTypedData } from 'viem'
import { linkWallet, executeOp } from '../src/handlers.js'
import { HttpError } from '../src/supabase.js'
import { toServerEvent } from '../src/indexer.js'
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

function fakeDeps({ payload, rpcCalls = [], submitted = [] } = {}) {
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
        getBalance: async () => 0n,
      },
      relayer: {
        account: { address: '0x' + '3'.repeat(40) },
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
  assert.equal(toServerEvent('sneakers', log('Released', { opId: '0xop', tokenId: 9n })).p_kind, 'SNEAKER_RELEASED')
  assert.equal(toServerEvent('distributor', log('Claimed', { sessionHash: '0xop' })).p_data.op, '0xop')
  assert.equal(toServerEvent('vault', log('Recycled', {})), null)
})
