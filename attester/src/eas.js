import { encodeAbiParameters, encodeFunctionData, encodePacked, keccak256, parseAbiParameters, zeroAddress, zeroHash } from 'viem'
import { EAS_ABI, SCHEMA_REGISTRY_ABI } from './chain.js'

/**
 * EAS 증명 — 서버가 확인한 러닝 · 코스 완주 · 배지를 체인에 남긴다(0044 chain_jobs).
 *
 * 올리는 값은 서버가 준 것뿐이다(attester_jobs_claim). 사람은 가명(bytes32)으로, 받는 사람(recipient)은
 * 연결한 지갑(없으면 0). 위치 · 경로 · 계정 번호는 올리지 않는다.
 *
 * 스키마는 한 번 등록하면 누구나 같은 번호(uid)로 찾는다 — 번호 = keccak256(스키마 문장, resolver, 취소 가능).
 * 증명은 취소 가능(revocable)으로 둔다 — 나중에 무효로 판정한 러닝은 거둘 수 있게.
 */

export const SCHEMAS = {
  RUN_PROOF: 'bytes32 runner,bytes32 run,uint32 day,uint32 distanceM,uint32 durationSec',
  COURSE_RUN: 'bytes32 runner,bytes32 course,bytes32 run,uint32 day,uint32 distanceM,uint32 durationSec',
  BADGE: 'bytes32 runner,string badge,uint32 value,uint32 day',
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

/** 서버 재료 → 스키마에 맞춘 증명 데이터 */
export function attestationData(kind, p) {
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
