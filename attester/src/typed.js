/**
 * 서명 재료 — 서버가 예약한 작업(attester_op_payload)을 컨트랙트가 받는 EIP-712 로 바꾼다.
 *
 * 외부 라이브러리를 쓰지 않는다. contracts/test/V2.test.js 가 이 파일을 그대로 불러
 * 서명하고, 실제 컨트랙트가 그 서명을 받는지 확인한다 — 여기와 컨트랙트가 한 글자라도
 * 어긋나면 그 검사가 깨진다.
 */

import { SHOE_MODELS } from './shoe-catalog.js'

// ── 신발 도감 — contracts/scripts/lib/catalog.js · 서버(0022)와 같은 표 ──
export const FACTIONS = ['FIRE', 'WATER', 'LIGHTNING', 'WIND']
export const RARITIES = ['COMMON', 'RARE', 'EPIC', 'LEGENDARY']
export const VARIANTS = [4, 4, 3, 2]

/**
 * 신발의 체인 모델 번호. 새 도감(0045, model_id 1101~)이면 그 번호 — 등급이 도감과 같아야 한다.
 * 아니면 예전 52종의 속성 × 100 + 등급 × 10 + 변형.
 */
export function sneakerModel(p) {
  if (p.model_id != null) {
    const model = Number(p.model_id)
    const shoe = SHOE_MODELS[model]
    if (!shoe) throw new Error(`알 수 없는 모델: ${p.model_id}`)
    if (shoe.rarity !== p.rarity) throw new Error(`모델 ${model} 의 등급이 다릅니다: ${p.rarity}`)
    return model
  }
  return modelId(p.faction, p.rarity, p.variant)
}

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

// ── v3 — 뽑은 신발을 금고로 발행 · 강화 · 수리를 금고의 신발에 반영 (contracts/StepUpSneakersV3.sol) ──
export const VAULT_MINT_TYPES = {
  VaultMint: [
    { name: 'opId', type: 'bytes32' },
    { name: 'account', type: 'bytes32' },
    { name: 'model', type: 'uint32' },
    { name: 'rarity', type: 'uint8' },
    { name: 'level', type: 'uint16' },
    { name: 'efficiencyBps', type: 'uint16' },
    { name: 'comfortBps', type: 'uint16' },
    { name: 'durability', type: 'uint16' },
    { name: 'genesisNo', type: 'uint32' },
    { name: 'deadline', type: 'uint64' },
  ],
}

export const STATS_SYNC_TYPES = {
  StatsSync: [
    { name: 'opId', type: 'bytes32' },
    { name: 'tokenId', type: 'uint256' },
    { name: 'level', type: 'uint16' },
    { name: 'durability', type: 'uint16' },
    { name: 'deadline', type: 'uint64' },
  ],
}

/** v3 토큰 번호는 여기서 시작한다 — v2 번호와 겹치지 않아 서버가 번호 하나로 신발을 찾는다 */
export const V3_FIRST_TOKEN_ID = 1000001n

/** 이 번호의 토큰이 v3 컨트랙트의 것인가 */
export const isV3Token = (tokenId) => tokenId != null && tokenId !== '' && BigInt(tokenId) >= V3_FIRST_TOKEN_ID

export function claimDomain(chainId, distributor) {
  return { name: 'StepUpRewards', version: '1', chainId: Number(chainId), verifyingContract: distributor }
}

/** 신발 컨트랙트의 서명 영역 — v2 는 '2', v3(금고 발행 · 스탯 갱신)는 '3' */
export function releaseDomain(chainId, sneakers, version = '2') {
  return { name: 'StepUpSneakers', version, chainId: Number(chainId), verifyingContract: sneakers }
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
    model: sneakerModel(p),
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

/** 뽑은 신발 → VaultMint. account 는 서버가 준 가명(bytes32) — 계정 번호가 체인에 나가지 않는다. */
export function vaultMintMessage(p) {
  if (p.kind !== 'VAULT_MINT') throw new Error(`금고 발행이 아닙니다: ${p.kind}`)
  if (!/^0x[0-9a-f]{64}$/i.test(String(p.account ?? '')) || /^0x0{64}$/i.test(p.account)) {
    throw new Error('받을 계정이 올바르지 않습니다')
  }
  return {
    opId: p.op_ref,
    account: p.account,
    model: sneakerModel(p),
    rarity: RARITIES.indexOf(p.rarity),
    level: Number(p.level),
    efficiencyBps: Number(p.efficiency_bps),
    comfortBps: Number(p.comfort_bps),
    durability: Math.round(Number(p.durability) * 100),
    genesisNo: p.genesis_no == null ? 0 : Number(p.genesis_no),
    deadline: BigInt(p.deadline_unix),
  }
}

/** 강화 · 수리 → StatsSync. 금고에 있는 v3 토큰만. */
export function statsSyncMessage(p) {
  if (p.kind !== 'STATS_SYNC') throw new Error(`스탯 갱신이 아닙니다: ${p.kind}`)
  if (!isV3Token(p.token_id)) throw new Error(`v3 토큰이 아닙니다: ${p.token_id}`)
  return {
    opId: p.op_ref,
    tokenId: BigInt(p.token_id),
    level: Number(p.level),
    durability: Math.round(Number(p.durability) * 100),
    deadline: BigInt(p.deadline_unix),
  }
}
