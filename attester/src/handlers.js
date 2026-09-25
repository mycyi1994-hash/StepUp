import { verifyMessage, isAddress } from 'viem'
import { DISTRIBUTOR_ABI, SNEAKERS_ABI } from './chain.js'
import { HttpError } from './supabase.js'
import {
  CLAIM_TYPES,
  RELEASE_TYPES,
  claimDomain,
  releaseDomain,
  claimMessage,
  releaseMessage,
  walletLinkMessage,
} from './typed.js'

/**
 * 사용자 요청 두 가지. 둘 다 로그인한 사람만, 둘 다 서버가 먼저 허락한 것만 한다.
 *
 *   POST /v2/wallet/link          지갑 서명을 확인하고 서버에 지갑을 붙인다
 *   POST /v2/ops/:id/execute      서버가 예약한 꺼내기 · 보너스 발행을 서명하고,
 *                                 가스비를 대신 내서 체인에 보낸다
 *
 * 금액 · 신발 스탯 · 받는 지갑은 전부 서버(attester_op_payload)에서 온다. 요청 본문에서
 * 받는 것은 작업 번호뿐이다.
 *
 * deps 로 Supabase · 체인을 받는다 — 테스트에서 가짜를 넣는다.
 */

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i

export async function linkWallet(request, env, deps) {
  const user = await deps.getUser(env, request)
  const body = await request.json().catch(() => null)
  const address = String(body?.address ?? '')
  const nonce = String(body?.nonce ?? '')
  const signature = String(body?.signature ?? '')
  if (!isAddress(address) || !/^[0-9a-f]{32}$/.test(nonce) || !/^0x[0-9a-fA-F]+$/.test(signature)) {
    throw new HttpError(400, '요청이 올바르지 않습니다')
  }

  const ok = await verifyMessage({ address, message: walletLinkMessage(user.id, nonce), signature })
  if (!ok) throw new HttpError(400, '지갑 서명이 맞지 않습니다')

  await deps.rpc(env, 'attester_wallet_link', { p_user: user.id, p_address: address, p_nonce: nonce })

  // 새 지갑에 가스 조금 — 앱으로 넣기(deposit)는 사용자가 직접 보내는 거래라서.
  // 이미 가스가 있는 지갑에는 보내지 않는다. 테스트넷에서만 켠다(DRIP_WEI).
  let drip = null
  const dripWei = BigInt(env.DRIP_WEI ?? '0')
  if (dripWei > 0n) {
    const c = deps.clients(env)
    const balance = await c.publicClient.getBalance({ address })
    if (balance < dripWei) {
      drip = await c.relayer.sendTransaction({ to: address, value: dripWei })
    }
  }
  return { ok: true, address: address.toLowerCase(), drip }
}

export async function executeOp(request, env, deps, opId) {
  if (!UUID.test(opId)) throw new HttpError(400, '작업 번호가 올바르지 않습니다')
  const user = await deps.getUser(env, request)

  // 서버가 주인 확인 · 상태 · 유효 시간 · 정지 스위치를 모두 본다
  const rows = await deps.rpc(env, 'attester_op_payload', { p_op: opId, p_user: user.id })
  const p = Array.isArray(rows) ? rows[0] : rows
  if (!p) throw new HttpError(409, '서명할 수 없는 작업입니다')

  const c = deps.clients(env)
  let tx
  if (p.kind === 'SUP_WITHDRAW') {
    const day = await c.publicClient.readContract({
      address: c.addresses.distributor,
      abi: DISTRIBUTOR_ABI,
      functionName: 'currentDay',
    })
    const message = claimMessage(p, day)
    const signature = await c.attester.signTypedData({
      domain: claimDomain(c.chain.id, c.addresses.distributor),
      types: CLAIM_TYPES,
      primaryType: 'Claim',
      message,
    })
    tx = await submit(c, c.addresses.distributor, DISTRIBUTOR_ABI, 'claim', [message, signature])
  } else {
    const message = releaseMessage(p)
    const signature = await c.sneakerSigner.signTypedData({
      domain: releaseDomain(c.chain.id, c.addresses.sneakers),
      types: RELEASE_TYPES,
      primaryType: 'Release',
      message,
    })
    tx = await submit(c, c.addresses.sneakers, SNEAKERS_ABI, 'release', [message, signature])
  }

  await deps.rpc(env, 'attester_op_submitted', { p_op: opId, p_tx: tx })
  // 아직 "완료"가 아니다. 확정 블록이 지나고 이벤트를 서버가 받아야 완료다.
  return { ok: true, status: 'SUBMITTED', tx }
}

/** 먼저 시뮬레이션해서 되돌아갈 거래는 보내지 않는다(가스 낭비 · 이유를 사용자에게). */
async function submit(c, address, abi, functionName, args) {
  try {
    const { request } = await c.publicClient.simulateContract({
      account: c.relayer.account,
      address,
      abi,
      functionName,
      args,
    })
    return await c.relayer.writeContract(request)
  } catch (e) {
    const reason = e?.cause?.data?.errorName ?? e?.shortMessage ?? '체인이 거절했습니다'
    throw new HttpError(409, `체인이 거절했습니다: ${reason}`)
  }
}
