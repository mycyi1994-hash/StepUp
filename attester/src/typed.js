/**
 * 서명 재료 — 서버가 예약한 작업(attester_op_payload)을 컨트랙트가 받는 EIP-712 로 바꾼다.
 *
 * 외부 라이브러리를 쓰지 않는다. contracts/test/V2.test.js 가 이 파일을 그대로 불러
 * 서명하고, 실제 컨트랙트가 그 서명을 받는지 확인한다 — 여기와 컨트랙트가 한 글자라도
 * 어긋나면 그 검사가 깨진다.
 */

// ── 신발 도감 — contracts/scripts/lib/catalog.js · 서버(0022)와 같은 표 ──
export const FACTIONS = ['FIRE', 'WATER', 'LIGHTNING', 'WIND']
export const RARITIES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY']
export const VARIANTS = [4, 4, 3, 2]

export function modelId(faction, rarity, variant) {
  const f = FACTIONS.indexOf(faction)
  const r = RARITIES.indexOf(rarity)
  if (f < 0 || r < 0 || !Number.isInteger(variant) || variant < 0 || variant >= VARIANTS[r]) {
    throw new Error(`알 수 없는 모델: ${faction} ${rarity} ${variant}`)
  }
  return f * 100 + r * 10 + variant
}

// ── 계정 · 작업 번호 ↔ bytes32 — 서버의 economy.account_ref / op_ref 와 같다 ──
export function accountRef(userId) {
  const hex = String(userId).toLowerCase().replace(/-/g, '')
  if (!/^[0-9a-f]{32}$/.test(hex)) throw new Error('사용자 번호가 올바르지 않습니다')
  return '0x' + hex.padStart(64, '0')
}

export function accountFromRef(ref) {
  const v = String(ref).toLowerCase().replace(/^0x/, '')
  if (!/^0{32}[0-9a-f]{32}$/.test(v)) return null
  const h = v.slice(32)
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`
}

// ── 지갑 연결 문장 — 서버의 economy.wallet_link_message 와 한 글자도 다르면 안 된다 ──
export function walletLinkMessage(userId, nonce) {
  return `StepUp 지갑 연결\n계정: ${userId}\n확인 번호: ${nonce}\n\n이 서명은 거래가 아니며 수수료가 들지 않습니다.`
}

// ── 금액 — 서버는 소수 4자리 SUP, 체인은 18자리 ──
export function supToWei(amount) {
  const s = String(amount)
  if (!/^\d+(\.\d{1,18})?$/.test(s)) throw new Error(`금액이 올바르지 않습니다: ${s}`)
  const [whole, frac = ''] = s.split('.')
  return BigInt(whole) * 10n ** 18n + BigInt((frac + '0'.repeat(18)).slice(0, 18))
}

export function weiToSup(wei) {
  const v = BigInt(wei)
  const whole = v / 10n ** 18n
  const frac = (v % 10n ** 18n).toString().padStart(18, '0').replace(/0+$/, '')
  return frac ? `${whole}.${frac}` : `${whole}`
}

// ── EIP-712 ──
export const CLAIM_TYPES = {
  Claim: [
    { name: 'runner', type: 'address' },
    { name: 'sessionHash', type: 'bytes32' },
    { name: 'amount', type: 'uint256' },
    { name: 'day', type: 'uint64' },
    { name: 'deadline', type: 'uint256' },
  ],
}

export const RELEASE_TYPES = {
  Release: [
    { name: 'opId', type: 'bytes32' },
    { name: 'to', type: 'address' },
    { name: 'tokenId', type: 'uint256' },
    { name: 'model', type: 'uint32' },
    { name: 'rarity', type: 'uint8' },
    { name: 'level', type: 'uint16' },
    { name: 'efficiencyBps', type: 'uint16' },
    { name: 'comfortBps', type: 'uint16' },
    { name: 'durability', type: 'uint16' },
    { name: 'genesisNo', type: 'uint32' },
    { name: 'locked', type: 'bool' },
    { name: 'deadline', type: 'uint64' },
  ],
}

export function claimDomain(chainId, distributor) {
  return { name: 'StepUpRewards', version: '1', chainId: Number(chainId), verifyingContract: distributor }
}

export function releaseDomain(chainId, sneakers) {
  return { name: 'StepUpSneakers', version: '2', chainId: Number(chainId), verifyingContract: sneakers }
}

/**
 * SUP 꺼내기 → Claim. `day` 는 컨트랙트의 currentDay() — 서버 날짜가 아니다
 * (컨트랙트의 하루는 배포 시각부터 센다).
 */
export function claimMessage(p, contractDay) {
  if (p.kind !== 'SUP_WITHDRAW') throw new Error(`SUP 꺼내기가 아닙니다: ${p.kind}`)
  return {
    runner: p.wallet,
    sessionHash: p.op_ref,
    amount: supToWei(p.amount),
    day: BigInt(contractDay),
    deadline: BigInt(p.deadline_unix),
  }
}

/** 신발 꺼내기 · 보너스 발행 → Release. 서버의 신발 값 그대로. */
export function releaseMessage(p) {
  if (p.kind !== 'SNEAKER_WITHDRAW' && p.kind !== 'BONUS_MINT') {
    throw new Error(`신발 작업이 아닙니다: ${p.kind}`)
  }
  return {
    opId: p.op_ref,
    to: p.wallet,
    tokenId: p.token_id == null ? 0n : BigInt(p.token_id),
    model: modelId(p.faction, p.rarity, p.variant),
    rarity: RARITIES.indexOf(p.rarity),
    level: Number(p.level),
    efficiencyBps: Number(p.efficiency_bps),
    comfortBps: Number(p.comfort_bps),
    durability: Math.round(Number(p.durability) * 100),
    genesisNo: p.genesis_no == null ? 0 : Number(p.genesis_no),
    locked: Boolean(p.transfer_locked),
    deadline: BigInt(p.deadline_unix),
  }
}
