import { encodeAbiParameters, encodeFunctionData, encodePacked, keccak256, parseAbiParameters, zeroAddress, zeroHash } from 'viem'
import { EAS_ABI, SCHEMA_REGISTRY_ABI } from './chain.js'

/**
 * EAS 증명 — 서버가 확인한 러닝 · 코스 완주 · 배지(0044 chain_jobs)와 게임 · 크루 · 매일 활동(0078)을 체인에 남긴다.
 *
 * 올리는 값은 서버가 준 것뿐이다(attester_jobs_claim). 사람은 가명(bytes32)으로, 받는 사람(recipient)은
 * 연결한 지갑(없으면 0). 위치 · 경로 · 계정 번호는 올리지 않는다.
 *
 * 스키마는 한 번 등록하면 누구나 같은 번호(uid)로 찾는다 — 번호 = keccak256(스키마 문장, resolver, 취소 가능).
 * 증명은 취소 가능(revocable)으로 둔다 — 나중에 무효로 판정한 러닝은 거둘 수 있게.
 */

/**
 * 0078 새 기록 — 게임 · 러닝 · 크루 · 매일 활동. 서버 재료(economy.activity_seal)의 키가 칸 이름 그대로다.
 * 사람 · 신발 · 크루 · 방은 가명(bytes32), 글자 칸은 대문자 이름(FREE · RARE …)뿐이다.
 * dataHash = sha256(기록마다 다른 소금 ‖ 원본 스냅숏) — 나중에 서버의 스냅숏과 대조한다(scripts/audit.mjs). 소금이 있어 추측해 맞출 수 없다.
 */
export const ACTIVITY_SCHEMAS = {
  SHOE_DRAW: 'bytes32 runner,bytes32 shoe,string drawKind,string rarity,uint32 modelId,uint64 drawNonce,bytes32 seedHash,bytes32 dataHash,uint32 day',
  SHOE_FORGE: 'bytes32 runner,bytes32 shoe,bool success,uint32 levelBefore,uint32 levelAfter,uint32 ratePermille,uint32 roll,uint32 materials,bytes32 dataHash,uint32 day',
  SHOE_REPAIR: 'bytes32 runner,bytes32 shoe,uint32 points,bytes32 dataHash,uint32 day',
  SHOE_POINTS: 'bytes32 runner,bytes32 shoe,uint32 efficiency,uint32 comfort,uint32 durability,bytes32 dataHash,uint32 day',
  SHOE_TRADE: 'bytes32 seller,bytes32 buyer,bytes32 shoe,string rarity,uint32 level,uint64 priceCenti,bytes32 dataHash,uint32 day',
  DRAW_SEED: 'bytes32 runner,bytes32 seedHash,uint64 firstNonce,bytes32 dataHash,uint32 day',
  DRAW_REVEAL: 'bytes32 runner,bytes32 seed,bytes32 seedHash,uint64 firstNonce,uint64 lastNonce,bytes32 dataHash,uint32 day',
  RUN_SETTLE: 'bytes32 runner,bytes32 run,uint32 distanceM,uint32 durationSec,uint64 pointsMilli,string verdict,bytes32 dataHash,uint32 day',
  SHOE_WEAR: 'bytes32 runner,bytes32 shoe,bytes32 run,uint32 runM,uint64 totalM,uint32 durability,bytes32 dataHash,uint32 day',
  CREW_RUN: 'bytes32 runner,bytes32 crew,bytes32 room,bytes32 run,uint32 distanceM,uint32 activeSec,uint32 together,bytes32 dataHash,uint32 day',
  FLASH_RUN: 'bytes32 runner,bytes32 flash,uint32 together,bytes32 dataHash,uint32 day',
  CREW_JOIN: 'bytes32 runner,bytes32 crew,bytes32 dataHash,uint32 day',
  PARTY_START: 'bytes32 runner,bytes32 room,bytes32 crew,uint32 starters,bytes32 dataHash,uint32 day',
  DAILY_STEPS: 'bytes32 runner,bool goalMet,bytes32 dataHash,uint32 day',
  CHECK_IN: 'bytes32 runner,bytes32 dataHash,uint32 day',
}

export const SCHEMAS = {
  RUN_PROOF: 'bytes32 runner,bytes32 run,uint32 day,uint32 distanceM,uint32 durationSec',
  COURSE_RUN: 'bytes32 runner,bytes32 course,bytes32 run,uint32 day,uint32 distanceM,uint32 durationSec',
  BADGE: 'bytes32 runner,string badge,uint32 value,uint32 day',
  ...ACTIVITY_SCHEMAS,
}

export const EAS_KINDS = Object.keys(SCHEMAS)

/** 스키마 등록소가 매기는 번호 */
export function schemaUid(schema, resolver = zeroAddress, revocable = true) {
  return keccak256(encodePacked(['string', 'address', 'bool'], [schema, resolver, revocable]))
}

const bytes32 = (v, name) => {
  if (!/^0x[0-9a-f]{64}$/i.test(String(v ?? ''))) throw new Error(`${name} 가 bytes32 가 아닙니다`)
  return v
}
const uint32 = (v, name) => {
  const n = Number(v)
  if (!Number.isInteger(n) || n < 0 || n > 0xffffffff) throw new Error(`${name} 가 uint32 가 아닙니다: ${v}`)
  return n
}

const uint64 = (v, name) => {
  let n
  try {
    n = BigInt(typeof v === 'number' && Number.isInteger(v) ? v : String(v ?? ''))
  } catch {
    throw new Error(`${name} 가 uint64 가 아닙니다: ${v}`)
  }
  if (typeof v !== 'number' && !/^\d+$/.test(String(v))) throw new Error(`${name} 가 uint64 가 아닙니다: ${v}`)
  if (n < 0n || n > 0xffffffffffffffffn) throw new Error(`${name} 가 uint64 가 아닙니다: ${v}`)
  return n
}
const bool = (v, name) => {
  if (typeof v !== 'boolean') throw new Error(`${name} 가 참/거짓이 아닙니다: ${v}`)
  return v
}
const word = (v, name) => {
  const s = String(v ?? '')
  if (!/^[A-Z0-9_]{1,32}$/.test(s)) throw new Error(`${name} 가 올바르지 않습니다: ${s}`)
  return s
}
const FIELD = { bytes32, uint32, uint64, bool, string: word }

/** 칸 이름 그대로인 재료 → 증명 데이터(새 기록). 스키마에 없는 키는 쓰지 않는다 */
function activityData(kind, p) {
  const schema = ACTIVITY_SCHEMAS[kind]
  const values = schema.split(',').map((field) => {
    const [type, name] = field.trim().split(/\s+/)
    const check = FIELD[type]
    if (!check) throw new Error(`모르는 칸 종류: ${type}`)
    return check(p[name], name)
  })
  return encodeAbiParameters(parseAbiParameters(schema), values)
}

/** 서버 재료 → 스키마에 맞춘 증명 데이터 */
export function attestationData(kind, p) {
  if (Object.hasOwn(ACTIVITY_SCHEMAS, kind)) return activityData(kind, p)
  const types = parseAbiParameters(SCHEMAS[kind] ?? '')
  if (kind === 'RUN_PROOF') {
    return encodeAbiParameters(types, [
      bytes32(p.runner, 'runner'),
      bytes32(p.run, 'run'),
      uint32(p.day, 'day'),
      uint32(p.distance_m, 'distance_m'),
      uint32(p.duration_sec, 'duration_sec'),
    ])
  }
  if (kind === 'COURSE_RUN') {
    return encodeAbiParameters(types, [
      bytes32(p.runner, 'runner'),
      bytes32(p.course, 'course'),
      bytes32(p.run, 'run'),
      uint32(p.day, 'day'),
      uint32(p.distance_m, 'distance_m'),
      uint32(p.duration_sec, 'duration_sec'),
    ])
  }
  if (kind === 'BADGE') {
    const badge = String(p.badge ?? '')
    if (!/^[A-Z_]{1,32}$/.test(badge)) throw new Error(`배지 이름이 올바르지 않습니다: ${badge}`)
    return encodeAbiParameters(types, [bytes32(p.runner, 'runner'), badge, uint32(p.value, 'value'), uint32(p.day, 'day')])
  }
  throw new Error(`EAS 일이 아닙니다: ${kind}`)
}

/** EAS.attest 의 인자(AttestationRequest) */
export function attestRequest(kind, p) {
  const recipient = /^0x[0-9a-f]{40}$/i.test(String(p.recipient ?? '')) ? p.recipient : zeroAddress
  return {
    schema: schemaUid(SCHEMAS[kind]),
    data: {
      recipient,
      expirationTime: 0n,
      revocable: true,
      refUID: zeroHash,
      data: attestationData(kind, p),
      value: 0n,
    },
  }
}

/** EAS.attest 호출 데이터 */
export function attestCall(kind, p) {
  return encodeFunctionData({ abi: EAS_ABI, functionName: 'attest', args: [attestRequest(kind, p)] })
}

/** 스키마 등록 호출 데이터 */
export function registerCall(kind) {
  return encodeFunctionData({
    abi: SCHEMA_REGISTRY_ABI,
    functionName: 'register',
    args: [SCHEMAS[kind], zeroAddress, true],
  })
}
