import { encodeFunctionData, keccak256, parseEventLogs } from 'viem'
import { EAS_ABI, SNEAKERS_V3_ABI } from './chain.js'
import { EAS_KINDS, SCHEMAS, attestRequest, registerCall, schemaUid } from './eas.js'
import { ALREADY_KNOWN, revertName } from './handlers.js'
import { pauseAll } from './indexer.js'
import { STATS_SYNC_TYPES, VAULT_MINT_TYPES, releaseDomain, statsSyncMessage, vaultMintMessage } from './typed.js'
import { ensureV3 } from './v3.js'
import { ensureCatalog } from './catalog.js'

/**
 * 체인 기록 보내기(0044 chain_jobs) — 러닝 증명 · 코스 완주 · 배지(EAS), 금고 발행 · 스탯 갱신(v3).
 * 가스비는 모두 릴레이어가 낸다. 2분마다 도는 실행(wrangler.toml 의 두 번째 cron)에서 부른다 —
 * 1분 작업(대조 · 인덱서 · 만료)과 요청 수 · CPU 몫을 나눠 쓰지 않게.
 *
 * ## 한 번만
 *
 * 서명한 거래(raw)와 번호(nonce)를 서버에 먼저 적고(attester_jobs_signed), 적힌 것만 보낸다. 끊기면
 * 같은 거래를 다시 보낼 뿐 새로 서명하지 않는다. 새로 서명하는 것은 그 번호가 다른 거래로 쓰였고
 * 이 거래의 영수증이 없음을 세 번 본 뒤다(서버가 센다) — 그때는 이 거래가 영영 들어갈 수 없다.
 * v3 발행 · 갱신은 컨트랙트도 작업 번호로 한 번만 받는다.
 *
 * ## 한 번에 조금씩
 *
 * CHAIN_JOBS_PER_RUN(기본 3)건씩. 거래마다 가스를 미리 재 보고(estimateGas), 체인이 받지 않을 일은
 * 보내지 않는다 — 바뀐 것 없는 갱신 · 금고에 없는 신발은 거두고, 하루 상한 · 멈춤은 뒤로 미룬다.
 * 릴레이어 잔액이 JOBS_MIN_RELAYER_WEI 아래면 보내지 않는다 — 사용자의 꺼내기 가스비를 남겨 둔다.
 */

export const V3_KINDS = ['VAULT_MINT', 'STATS_SYNC']

/**
 * 두 번째 cron — 이 일만 한다. 1분 작업과 따로 돌려 요청 수(무료 50) · CPU 몫을 나눠 쓰지 않는다.
 * wrangler.toml 의 crons 와 같아야 한다(test/jobs.test.js 가 본다). index.js 는 이름 있는 값을 내보내지 않는다 —
 * 워커 모듈의 이름 있는 내보내기는 진입점으로 읽힌다.
 */
export const JOBS_CRON = '*/2 * * * *'

/** 보낸 뒤 이만큼 지나도 블록에 없으면 같은 거래를 다시 보낸다 */
export const REBROADCAST_AFTER_MS = 2 * 60 * 1000
const MAX_REBROADCAST = 3
/** 한 번에 확인하는 보낸 일 수 */
const CONFIRM_PER_RUN = 10

/** 체인이 받지 않을 일 — 다시 보내도 같다. 거둔다. */
const CANCEL = new Set(['NoChange', 'NotInVault', 'OpAlreadyUsed', 'ZeroAccount', 'IdentityChanged', 'LevelWentDown', 'GenesisTaken'])
/** 오늘은 더 못 보낸다 — 다음 날(UTC, 컨트랙트의 하루)에 */
const NEXT_DAY = new Set(['DailyMintCapReached', 'DailySyncCapReached'])
/** 도감 · 스탯 범위가 서버와 컨트랙트에서 어긋났다 — 사람이 볼 때까지 한 시간씩 */
const HOURLY = new Set(['UnknownModel', 'RarityMismatch', 'BadStats', 'GenesisOutOfRange'])

const secondsToNextUtcDay = (now = Date.now()) => Math.ceil((86_400_000 - (now % 86_400_000)) / 1000) + 60

/** 체인이 거절한 이유 → 줄에 돌려주는 방법 */
export function releaseFor(jobId, name, e) {
  const error = String(name ?? e?.shortMessage ?? e?.message ?? 'estimate failed').slice(0, 300)
  if (name && CANCEL.has(name)) return { id: jobId, error, cancel: true }
  if (name && NEXT_DAY.has(name)) return { id: jobId, error, retry_sec: secondsToNextUtcDay() }
  if (name && HOURLY.has(name)) return { id: jobId, error, retry_sec: 3600 }
  if (name === 'InvalidSchema' || name === 'EnforcedPause') return { id: jobId, error, retry_sec: 120 }
  return { id: jobId, error }
}

/**
 * 일 하나 → 보낼 컨트랙트 호출 { address, abi, functionName, args }. ABI 를 함께 주어야 가스를 잴 때
 * 컨트랙트가 되돌린 이유를 이름으로 읽는다(NoChange · InvalidSchema …). v3 는 신발 서명 키로 서명한다.
 */
export async function buildCall(c, job) {
  const p = { ...job.payload, kind: job.kind, op_ref: job.op_ref }
  if (EAS_KINDS.includes(job.kind)) {
    return { address: c.addresses.eas, abi: EAS_ABI, functionName: 'attest', args: [attestRequest(job.kind, p)] }
  }
  const domain = releaseDomain(c.chain.id, c.addresses.sneakersV3, '3')
  if (job.kind === 'VAULT_MINT') {
    const message = vaultMintMessage(p)
    const signature = await c.sneakerSigner.signTypedData({ domain, types: VAULT_MINT_TYPES, primaryType: 'VaultMint', message })
    return { address: c.addresses.sneakersV3, abi: SNEAKERS_V3_ABI, functionName: 'vaultMint', args: [message, signature] }
  }
  if (job.kind === 'STATS_SYNC') {
    const message = statsSyncMessage(p)
    const signature = await c.sneakerSigner.signTypedData({ domain, types: STATS_SYNC_TYPES, primaryType: 'StatsSync', message })
    return { address: c.addresses.sneakersV3, abi: SNEAKERS_V3_ABI, functionName: 'syncStats', args: [message, signature] }
  }
  throw new Error(`모르는 일: ${job.kind}`)
}

/** 이번에 쓸 수수료 — 기본 수수료의 두 배 + 팁 */
async function feesOf(c) {
  const [block, priority] = await Promise.all([
    c.publicClient.getBlock({ blockTag: 'latest' }),
    c.publicClient.estimateMaxPriorityFeePerGas().catch(() => 1_000_000n),
  ])
  const base = BigInt(block?.baseFeePerGas ?? 0n)
  return { maxFeePerGas: base * 2n + priority, maxPriorityFeePerGas: priority }
}

/** 영수증에서 결과 — EAS 증명 번호 또는 v3 토큰 번호 */
export function resultOf(kind, receipt, addresses) {
  if (EAS_KINDS.includes(kind)) {
    const logs = parseEventLogs({
      abi: EAS_ABI,
      eventName: 'Attested',
      logs: receipt.logs.filter((l) => l.address.toLowerCase() === String(addresses.eas).toLowerCase()),
    })
    return logs[0]?.args?.uid ?? null
  }
  const logs = parseEventLogs({
    abi: SNEAKERS_V3_ABI,
    eventName: kind === 'VAULT_MINT' ? 'VaultMinted' : 'StatsSynced',
    logs: receipt.logs.filter((l) => l.address.toLowerCase() === String(addresses.sneakersV3).toLowerCase()),
  })
  return logs[0]?.args?.tokenId?.toString() ?? null
}

/** 보낸 일 — 확정 · 되돌림 · 죽은 거래 · 다시 보내기 */
export async function confirmJobs(env, deps, c) {
  const rows = (await deps.rpc(env, 'attester_jobs_open', { p_limit: CONFIRM_PER_RUN })) ?? []
  if (!rows.length) return { open: 0 }
  const latest = BigInt(
    await c.publicClient.getTransactionCount({ address: c.relayer.account.address, blockTag: 'latest' }),
  )
  const out = { open: rows.length, confirmed: 0, retry: 0, dead: 0, waiting: 0, rebroadcast: 0 }
  const results = []
  let safe // 확정으로 볼 블록 — 들어간 거래가 있을 때만 한 번 읽는다(못 읽으면 null)
  for (const r of rows) {
    const nonce = BigInt(r.nonce)
    if (nonce >= latest) {
      // 아직 블록에 없다. 보낸 지 오래됐으면(노드가 잃었거나 앞 번호가 비었다) 같은 거래를 다시 보낸다
      if (Date.now() - Date.parse(r.sent_at) > REBROADCAST_AFTER_MS && out.rebroadcast < MAX_REBROADCAST) {
        out.rebroadcast += 1
        await c.publicClient.sendRawTransaction({ serializedTransaction: r.raw_tx }).catch((e) => {
          const msg = String(e?.details ?? e?.shortMessage ?? e?.message ?? '')
          if (!ALREADY_KNOWN.test(msg) && !/nonce too low/i.test(msg)) console.error('rebroadcast failed', r.job_id, msg)
        })
      }
      out.waiting += 1
      continue
    }
    // 이 번호의 거래는 들어갔다 — 이 거래인가
    let receipt = null
    try {
      receipt = await c.publicClient.getTransactionReceipt({ hash: r.tx_hash })
    } catch (e) {
      if (e?.name !== 'TransactionReceiptNotFoundError') {
        console.error('receipt failed', r.job_id, e?.message)
        continue // 못 읽었다 — 다음에
      }
    }
    if (!receipt) {
      // 다른 거래가 이 번호를 썼다. 서버가 세 번 본 뒤에 다시 서명하게 한다
      results.push({ id: r.job_id, status: 'DEAD' })
      out.dead += 1
      continue
    }
    if (safe === undefined) safe = await safeBlockNumber(c)
    if (safe == null || receipt.blockNumber > safe) {
      out.waiting += 1
      continue
    }
    if (receipt.status !== 'success') {
      results.push({ id: r.job_id, status: 'RETRY', error: 'reverted' })
      out.retry += 1
      continue
    }
    results.push({
      id: r.job_id,
      status: 'CONFIRMED',
      block: Number(receipt.blockNumber),
      result: resultOf(r.kind, receipt, c.addresses),
    })
    out.confirmed += 1
  }
  if (results.length) {
    const verdict = await deps.rpc(env, 'attester_jobs_result', { p_items: results })
    // 서버가 받아들이지 못한 확정(토큰 번호가 어긋났다) — 서버는 이미 멈췄다. 컨트랙트도 멈춘다
    if (verdict === 'MISMATCH') await pauseAll(env, deps, 'chain job result mismatch', 'sneakersV3')
  }
  return out
}

async function safeBlockNumber(c) {
  for (const blockTag of ['safe', 'finalized']) {
    try {
      const b = await c.publicClient.getBlock({ blockTag })
      if (b?.number != null) return b.number
    } catch (e) {
      console.error(`${blockTag} block unavailable`, e?.message)
    }
  }
  return null
}

/** 스키마가 없어 EAS 가 받지 않았다 — 등록을 한 번 보낸다(보낸 뒤 10분은 다시 보내지 않는다) */
async function registerSchemas(env, deps, c, kinds) {
  const sent = []
  for (const kind of kinds) {
    const uid = schemaUid(SCHEMAS[kind])
    const marker = `eas-schema@${c.chain.id}:${uid}`
    const [at, head] = await Promise.all([
      deps.rpc(env, 'attester_cursor_get', { p_name: marker }),
      c.publicClient.getBlockNumber(),
    ])
    if (at != null && BigInt(head) - BigInt(at) < 600n) continue
    await deps.rpc(env, 'attester_cursor_set', { p_name: marker, p_block: Number(head) })
    try {
      const hash = await c.relayer.sendTransaction({ to: c.addresses.schemaRegistry, data: registerCall(kind) })
      sent.push({ kind, uid, tx: hash })
    } catch (e) {
      // 이미 있다(AlreadyExists) — 다른 워커가 먼저 올렸다
      if (revertName(e) !== 'AlreadyExists') console.error('schema register failed', kind, e?.shortMessage ?? e?.message)
    }
  }
  return sent
}

/** 새 일 — 가져가기 → 서명 → 적기 → 보내기 */
export async function sendJobs(env, deps, c, kinds) {
  const limit = Math.max(0, Math.min(Number(env.CHAIN_JOBS_PER_RUN ?? 3), 20))
  if (!kinds.length || limit === 0) return { skipped: 'nothing to send' }
  const relayer = c.relayer.account.address
  const floor = BigInt(env.JOBS_MIN_RELAYER_WEI ?? '3000000000000000')
  const balance = await c.publicClient.getBalance({ address: relayer })
  if (balance < floor) return { skipped: 'relayer balance low', balance: balance.toString() }

  const jobs = (await deps.rpc(env, 'attester_jobs_claim', { p_kinds: kinds, p_limit: limit })) ?? []
  if (!jobs.length) return { claimed: 0 }

  const [pending, fees] = await Promise.all([
    c.publicClient.getTransactionCount({ address: relayer, blockTag: 'pending' }),
    feesOf(c),
  ])
  let nonce = Number(pending)
  const signed = []
  const released = []
  const needSchema = new Set()
  let v3Paused = false
  for (const job of jobs) {
    if (v3Paused && V3_KINDS.includes(job.kind)) {
      released.push({ id: job.job_id, error: 'EnforcedPause', retry_sec: 120 })
      continue
    }
    let call
    let gas
    try {
      call = await buildCall(c, job)
      gas = await c.publicClient.estimateContractGas({ ...call, account: relayer })
    } catch (e) {
      const name = revertName(e)
      released.push(releaseFor(job.job_id, name, e))
      if (name === 'InvalidSchema') needSchema.add(job.kind)
      if (name === 'EnforcedPause' && V3_KINDS.includes(job.kind)) v3Paused = true
      continue
    }
    const raw = await c.relayer.account.signTransaction({
      chainId: c.chain.id,
      type: 'eip1559',
      nonce,
      to: call.address,
      data: encodeFunctionData(call),
      value: 0n,
      gas: (BigInt(gas) * 13n) / 10n + 10_000n,
      maxFeePerGas: fees.maxFeePerGas,
      maxPriorityFeePerGas: fees.maxPriorityFeePerGas,
    })
    signed.push({ id: job.job_id, nonce, tx: keccak256(raw), raw })
    nonce += 1
  }

  if (released.length) await deps.rpc(env, 'attester_jobs_release', { p_items: released })
  const out = { claimed: jobs.length, signed: signed.length, released: released.length, sent: 0 }
  if (signed.length) {
    // 적힌 일만 보낸다 — 적기에 실패하면 아무것도 보내지 않는다(2분 뒤 다시 가져간다)
    const accepted = new Set(((await deps.rpc(env, 'attester_jobs_signed', { p_items: signed })) ?? []).map(Number))
    for (const s of signed) {
      if (!accepted.has(Number(s.id))) continue
      try {
        await c.publicClient.sendRawTransaction({ serializedTransaction: s.raw })
        out.sent += 1
      } catch (e) {
        const msg = String(e?.details ?? e?.shortMessage ?? e?.message ?? '')
        if (ALREADY_KNOWN.test(msg)) {
          out.sent += 1
          continue
        }
        // 나머지는 적혀 있으니 확인 단계가 다시 보낸다
        console.error('send failed', s.id, msg)
        out.failed = (out.failed ?? 0) + 1
        break
      }
    }
  }
  if (needSchema.size && c.addresses.schemaRegistry) out.schemas = await registerSchemas(env, deps, c, [...needSchema])
  return out
}

/** 2분 작업 — 확인 → v3 준비 → 보내기. 한 단계가 실패해도 다음 단계는 한다. */
export async function runJobs(env, deps) {
  const c = deps.clients(env)
  const step = (name, fn) =>
    fn().catch((e) => {
      console.error(`jobs ${name} error`, e?.message)
      return { error: true }
    })
  const confirm = await step('confirm', () => confirmJobs(env, deps, c))
  const v3 = await step('v3', () => ensureV3(env, deps, c))
  // 새 도감 모델이 v3 도감에 다 들어간 뒤에 v3 일(금고 발행 · 갱신)을 보낸다 — 없는 모델은 컨트랙트가 받지 않는다
  const catalog = v3?.ready ? await step('catalog', () => ensureCatalog(env, deps, c)) : null
  const kinds = [...(c.addresses.eas ? EAS_KINDS : []), ...(v3?.ready && catalog?.ready ? V3_KINDS : [])]
  const send = await step('send', () => sendJobs(env, deps, c, kinds))
  return { confirm, v3, catalog, send }
}

