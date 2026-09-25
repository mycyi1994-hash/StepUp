import { DISTRIBUTOR_ABI, SNEAKERS_ABI, VAULT_ABI } from './chain.js'
import { parseEventLogs } from 'viem'
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
    return {
      ...base,
      p_kind: 'SUP_CLAIMED',
      p_data: { op: a.sessionHash, runner: a.runner?.toLowerCase(), amount: a.amount == null ? undefined : weiToSup(a.amount) },
    }
  }
  if (source === 'sneakers' && log.eventName === 'Released') {
    return {
      ...base,
      p_kind: 'SNEAKER_RELEASED',
      p_data: { op: a.opId, tokenId: a.tokenId.toString(), to: a.to?.toLowerCase() },
    }
  }
  if (source === 'sneakers' && log.eventName === 'OpCancelled') {
    return { ...base, p_kind: 'OP_CANCELLED', p_data: { op: a.opId } }
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

/**
 * 확정으로 볼 블록. 이 체인(OP 스택)은 1초마다 블록이 나와 "최신 - 30" 은 30초뿐이다 —
 * 뒤집힐 수 있다. 체인이 알려 주는 safe 블록까지만 읽는다(못 받으면 옛 방식으로).
 */
async function safeBlock(env, c) {
  try {
    const b = await c.publicClient.getBlock({ blockTag: 'safe' })
    if (b?.number != null) return b.number
  } catch (e) {
    console.error('safe block unavailable', e?.message)
  }
  return (await c.publicClient.getBlockNumber()) - BigInt(env.CONFIRMATIONS ?? '30')
}

/** 서버가 허락하지 않은 지급 · 발행을 봤을 때 — 서버와 컨트랙트를 멈춘다(그 컨트랙트 먼저) */
async function alarm(env, deps, source, ev, result) {
  await pauseAll(env, deps, `${source} ${ev.p_kind} ${result} ${ev.p_tx}`, source)
}

export async function indexEvents(env, deps) {
  const c = deps.clients(env)
  const safe = await safeBlock(env, c)
  const report = {}

  for (const [source, abi] of SOURCES) {
    const address = c.addresses[source]
    if (!address) continue
    // 한 컨트랙트를 못 읽어도 다른 컨트랙트는 읽는다
    try {
      const cursor = await deps.rpc(env, 'attester_cursor_get', { p_name: source })
      const from = cursor == null ? BigInt(env.START_BLOCK ?? '0') : BigInt(cursor) + 1n
      if (from > safe) continue
      const to = safe < from + MAX_RANGE ? safe : from + MAX_RANGE

      const logs = await c.publicClient.getContractEvents({ address, abi, fromBlock: from, toBlock: to })
      // RPC 뒤의 노드가 아직 그 블록을 모르면 오류 없이 빈 목록을 준다. 끝 블록이 실제로
      // 있는지 확인한 뒤에만 커서를 옮긴다 — 안 그러면 못 본 이벤트를 영영 건너뛴다.
      const edge = await c.publicClient.getBlock({ blockNumber: to }).catch(() => null)
      if (!edge) {
        report[source] = { from: Number(from), to: Number(to), skipped: 'block not available yet' }
        continue
      }
      logs.sort((x, y) =>
        x.blockNumber === y.blockNumber ? Number(x.logIndex) - Number(y.logIndex) : Number(x.blockNumber - y.blockNumber),
      )
      let handled = 0
      for (const log of logs) {
        const ev = toServerEvent(source, log)
        if (!ev) continue
        // 실패하면 여기서 멈추고 커서를 옮기지 않는다
        const result = await deps.rpc(env, 'attester_chain_event', ev)
        handled += 1
        // 서버가 허락하지 않은 지급 · 발행이다(서명 키가 샜다). 서버는 이미 멈췄고, 컨트랙트도 멈춘다.
        // 컨트랙트 정지가 실패해도 다음 실행의 keepPaused 가 다시 멈춘다.
        if (result === 'UNKNOWN_OP' || result === 'MISMATCH') await alarm(env, deps, source, ev, result)
      }
      await deps.rpc(env, 'attester_cursor_set', { p_name: source, p_block: Number(to) })
      report[source] = { from: Number(from), to: Number(to), events: handled }
    } catch (e) {
      console.error(`index ${source} failed`, e?.message)
      report[source] = { error: true }
    }
  }
  return report
}

/** 체인에서 쓰였는데 이벤트를 못 받은 채 이만큼 지났으면 거래 영수증에서 이벤트를 찾아 넘긴다 */
const RECOVER_AFTER_MS = 60 * 60 * 1000

export async function expireOps(env, deps) {
  const c = deps.clients(env)
  const due = (await deps.rpc(env, 'attester_due_ops', {})) ?? []
  const out = { expired: 0, pendingOnChain: 0 }
  let safe = null
  for (const op of due) {
    // 한 작업이 막혀도 뒤의 작업은 계속 한다 — 앞에서 매번 같은 오류로 멈추면 그 뒤의
    // SUP · 신발이 영영 묶인다.
    try {
      const sup = op.kind === 'SUP_WITHDRAW'
      const used = await c.publicClient.readContract({
        address: sup ? c.addresses.distributor : c.addresses.sneakers,
        abi: sup ? DISTRIBUTOR_ABI : SNEAKERS_ABI,
        functionName: sup ? 'sessionClaimed' : 'opUsed',
        args: [op.op_ref],
      })
      if (used) {
        out.pendingOnChain += 1 // 이벤트가 곧 들어온다(취소도 이벤트로 들어온다). 되돌리지 않는다.
        if (op.tx_hash && Date.parse(op.deadline) + RECOVER_AFTER_MS < Date.now()) {
          safe ??= await safeBlock(env, c)
          if (await recoverFromReceipt(env, deps, c, op, sup ? 'distributor' : 'sneakers', safe)) out.recovered = (out.recovered ?? 0) + 1
        }
        continue
      }
      await deps.rpc(env, 'attester_op_expire', { p_op: op.op_id, p_used_on_chain: false })
      out.expired += 1
    } catch (e) {
      out.failed = (out.failed ?? 0) + 1
      console.error('expire failed', op.op_id, e?.message)
    }
  }
  return out
}

/**
 * 인덱서가 놓친 이벤트를 그 작업의 거래 영수증에서 다시 찾는다. 서버는 (거래, 로그 번호)로
 * 한 번만 처리하므로 이미 받은 이벤트면 DUPLICATE 로 끝난다.
 */
async function recoverFromReceipt(env, deps, c, op, source, safe) {
  const receipt = await c.publicClient.getTransactionReceipt({ hash: op.tx_hash }).catch(() => null)
  if (!receipt || receipt.status !== 'success' || receipt.blockNumber > safe) return false
  const abi = source === 'distributor' ? DISTRIBUTOR_ABI : SNEAKERS_ABI
  const address = c.addresses[source].toLowerCase()
  const logs = parseEventLogs({ abi, logs: receipt.logs.filter((l) => l.address.toLowerCase() === address) })
  let sent = false
  for (const log of logs) {
    const ev = toServerEvent(source, log)
    if (!ev) continue
    const result = await deps.rpc(env, 'attester_chain_event', ev)
    if (result === 'UNKNOWN_OP' || result === 'MISMATCH') await alarm(env, deps, source, ev, result)
    sent = true
  }
  return sent
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

/**
 * 서버와 모든 컨트랙트를 멈춘다. 다시 켜는 것은 사람(관리자 지갑 · admin_economy_set)만.
 * 문제가 난 컨트랙트([first])부터, 하나가 실패해도 나머지를 멈춘다. 서버에 알리기가 실패해도
 * 컨트랙트는 멈춘다. 실패한 것은 다음 실행의 [keepPaused] 가 다시 한다.
 */
export async function pauseAll(env, deps, reason, first) {
  console.error('PAUSE', reason)
  let serverError = null
  try {
    await deps.rpc(env, 'attester_pause', { p_reason: reason })
  } catch (e) {
    serverError = e
    console.error('server pause failed', e?.message)
  }
  const failed = await pauseContracts(env, deps, first)
  if (serverError) throw serverError
  if (failed.length) throw new Error(`컨트랙트 정지 실패: ${failed.join(', ')}`)
}

/** 아직 안 멈춘 컨트랙트를 멈춘다. 실패한 컨트랙트 이름을 돌려준다. */
export async function pauseContracts(env, deps, first) {
  const c = deps.clients(env)
  if (!c.guardian) {
    console.error('GUARDIAN_PRIVATE_KEY 가 없어 컨트랙트를 멈출 수 없습니다')
    return SOURCES.map(([s]) => s)
  }
  const order = [...SOURCES].sort(([a], [b]) => (a === first ? -1 : b === first ? 1 : 0))
  const failed = []
  for (const [source, abi] of order) {
    const address = c.addresses[source]
    if (!address) continue
    try {
      const paused = await c.publicClient.readContract({ address, abi, functionName: 'paused' })
      if (paused) continue
      const hash = await c.guardian.writeContract({ address, abi, functionName: 'pause' })
      const receipt = await c.publicClient.waitForTransactionReceipt({ hash, timeout: 20_000 })
      if (receipt.status !== 'success') throw new Error(`pause reverted ${hash}`)
    } catch (e) {
      console.error(`pause ${source} failed`, e?.message)
      failed.push(source)
    }
  }
  return failed
}

/**
 * 서버가 멈춰 있으면 컨트랙트도 멈춰 있게 한다. 정지 신호를 준 이벤트는 한 번만 처리되므로
 * (다시 오면 DUPLICATE) 그때 컨트랙트 정지가 실패했으면 여기서 매분 다시 한다.
 */
export async function keepPaused(env, deps) {
  const paused = await deps.rpc(env, 'attester_chain_paused', {})
  if (paused !== true) return { paused: false }
  const failed = await pauseContracts(env, deps)
  return { paused: true, failed }
}
