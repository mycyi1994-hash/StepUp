import { DISTRIBUTOR_ABI, SNEAKERS_ABI, VAULT_ABI } from './chain.js'
import { weiToSup } from './typed.js'

/**
 * 1분마다 도는 일 — 체인을 읽어 서버에 알리고, 오래된 작업을 되돌리고, 장부를 맞춰 본다.
 *
 * 1. 이벤트: 확정 블록(최신 - CONFIRMATIONS)까지만 읽는다. 뒤집힐 수 있는 블록의
 *    넣기를 반영하면 체인에는 없는 잔고가 앱에 생긴다. 컨트랙트마다 블록 · 로그 순서로
 *    서버에 넘기고, 모두 받아들여졌을 때만 커서를 옮긴다. 같은 이벤트를 두 번 넘겨도
 *    서버가 (거래, 로그 번호)로 한 번만 처리한다.
 * 2. 만료: 서명 유효 시간 + 안전 마진이 지난 작업. 체인에서 그 작업 번호가 안 쓰였을
 *    때만 되돌린다. 쓰였으면 이벤트를 놓친 것이므로 되돌리지 않고 다음 읽기에 맡긴다.
 * 3. 대조: 체인이 서버가 모르는 지급을 했거나, 서버가 체인보다 많이 넣어 줬으면
 *    서명 키가 샌 것으로 보고 모든 컨트랙트와 서버를 멈춘다.
 */

const MAX_RANGE = 2000n

/** 체인 로그 → 서버 이벤트. 모르는 로그는 null. */
export function toServerEvent(source, log) {
  const base = { p_tx: log.transactionHash, p_log: Number(log.logIndex), p_block: Number(log.blockNumber) }
  const a = log.args
  if (source === 'distributor' && log.eventName === 'Claimed') {
    return { ...base, p_kind: 'SUP_CLAIMED', p_data: { op: a.sessionHash } }
  }
  if (source === 'sneakers' && log.eventName === 'Released') {
    return { ...base, p_kind: 'SNEAKER_RELEASED', p_data: { op: a.opId, tokenId: a.tokenId.toString() } }
  }
  if (source === 'sneakers' && log.eventName === 'Deposited') {
    return {
      ...base,
      p_kind: 'SNEAKER_DEPOSITED',
      p_data: { account: a.account, tokenId: a.tokenId.toString(), from: a.from.toLowerCase() },
    }
  }
  if (source === 'vault' && log.eventName === 'Deposited') {
    return { ...base, p_kind: 'SUP_DEPOSITED', p_data: { account: a.account, amount: weiToSup(a.amount) } }
  }
  return null
}

const SOURCES = [
  ['distributor', DISTRIBUTOR_ABI],
  ['sneakers', SNEAKERS_ABI],
  ['vault', VAULT_ABI],
]

export async function indexEvents(env, deps) {
  const c = deps.clients(env)
  const latest = await c.publicClient.getBlockNumber()
  const safe = latest - BigInt(env.CONFIRMATIONS ?? '30')
  const report = {}

  for (const [source, abi] of SOURCES) {
    const address = c.addresses[source]
    if (!address) continue
    const cursor = await deps.rpc(env, 'attester_cursor_get', { p_name: source })
    const from = cursor == null ? BigInt(env.START_BLOCK ?? '0') : BigInt(cursor) + 1n
    if (from > safe) continue
    const to = safe < from + MAX_RANGE ? safe : from + MAX_RANGE

    const logs = await c.publicClient.getContractEvents({ address, abi, fromBlock: from, toBlock: to })
    logs.sort((x, y) =>
      x.blockNumber === y.blockNumber ? Number(x.logIndex) - Number(y.logIndex) : Number(x.blockNumber - y.blockNumber),
    )
    let handled = 0
    for (const log of logs) {
      const ev = toServerEvent(source, log)
      if (!ev) continue
      await deps.rpc(env, 'attester_chain_event', ev) // 실패하면 여기서 멈추고 커서를 옮기지 않는다
      handled += 1
    }
    await deps.rpc(env, 'attester_cursor_set', { p_name: source, p_block: Number(to) })
    report[source] = { from: Number(from), to: Number(to), events: handled }
  }
  return report
}

export async function expireOps(env, deps) {
  const c = deps.clients(env)
  const due = (await deps.rpc(env, 'attester_due_ops', {})) ?? []
  const out = { expired: 0, pendingOnChain: 0 }
  for (const op of due) {
    const used =
      op.kind === 'SUP_WITHDRAW'
        ? await c.publicClient.readContract({
            address: c.addresses.distributor,
            abi: DISTRIBUTOR_ABI,
            functionName: 'sessionClaimed',
            args: [op.op_ref],
          })
        : await c.publicClient.readContract({
            address: c.addresses.sneakers,
            abi: SNEAKERS_ABI,
            functionName: 'opUsed',
            args: [op.op_ref],
          })
    if (used) {
      out.pendingOnChain += 1 // 이벤트가 곧 들어온다. 되돌리지 않는다.
      continue
    }
    await deps.rpc(env, 'attester_op_expire', { p_op: op.op_id, p_used_on_chain: false })
    out.expired += 1
  }
  return out
}

/** 허용 오차 — 확정 전 구간의 금액 차이. 이보다 크게 어긋나면 멈춘다. */
export async function reconcile(env, deps) {
  const c = deps.clients(env)
  const rows = await deps.rpc(env, 'attester_ledger_totals', {})
  const t = Array.isArray(rows) ? rows[0] : rows
  const [paid, deposited] = await Promise.all([
    c.publicClient.readContract({ address: c.addresses.distributor, abi: DISTRIBUTOR_ABI, functionName: 'totalDistributed' }),
    c.addresses.vault
      ? c.publicClient.readContract({ address: c.addresses.vault, abi: VAULT_ABI, functionName: 'totalDeposited' })
      : 0n,
  ])
  const toWei = (v) => BigInt(Math.round(Number(v ?? 0) * 1e4)) * 10n ** 14n
  const allowedPaid = toWei(t.sup_withdrawn_confirmed) + toWei(t.sup_withdraw_pending)
  const problems = []
  // 체인이 서버가 아는 것(확정 + 대기)보다 더 줬다 → 서버가 모르는 서명이 쓰였다
  if (paid > allowedPaid) problems.push(`체인 지급 ${weiToSup(paid)} > 서버 장부 ${weiToSup(allowedPaid)}`)
  // 서버가 넣어 준 SUP 가 금고에 실제로 들어온 것보다 많다
  if (toWei(t.sup_deposited) > deposited) {
    problems.push(`서버 넣기 ${t.sup_deposited} > 금고 ${weiToSup(deposited)}`)
  }
  if (problems.length) await pauseAll(env, deps, problems.join(' · '))
  return { ok: problems.length === 0, problems }
}

/** 서버와 모든 컨트랙트를 멈춘다. 다시 켜는 것은 사람(관리자 지갑 · admin_economy_set)만. */
export async function pauseAll(env, deps, reason) {
  await deps.rpc(env, 'attester_pause', { p_reason: reason })
  const c = deps.clients(env)
  if (!c.guardian) return
  for (const [source, abi] of SOURCES) {
    const address = c.addresses[source]
    if (!address) continue
    const paused = await c.publicClient.readContract({ address, abi, functionName: 'paused' })
    if (!paused) await c.guardian.writeContract({ address, abi, functionName: 'pause' })
  }
}
