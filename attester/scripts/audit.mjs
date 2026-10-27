#!/usr/bin/env node
/**
 * 체인 기록 하나를 대조한다(0078 — 나중에 필요할 때만).
 *
 *   node scripts/audit.mjs <EAS 증명 번호(uid)> [서버 data_hash]
 *
 * 1. 관리자가 서버에서 `select * from admin_chain_audit(<job id>);` 로 그 기록의 attestation(uid) · data_hash ·
 *    소금 · 스냅숏을 본다. hash_ok 가 true 면 서버의 스냅숏이 처음 봉인한 그대로다.
 * 2. 이 스크립트가 체인에서 그 증명을 읽어 칸을 풀고, 체인의 dataHash 가 서버 data_hash 와 같은지 본다.
 *    같으면 서버 기록(스냅숏)이 보낸 뒤로 바뀌지 않았다.
 *
 * 읽기만 한다(키 · 로그인 없이 공개 RPC). RPC_URL · EAS_ADDRESS 를 바꾸려면 환경 변수로.
 */
import { createPublicClient, decodeAbiParameters, http, parseAbi, parseAbiParameters } from 'viem'
import { SCHEMAS, schemaUid } from '../src/eas.js'

const RPC_URL = process.env.RPC_URL ?? 'https://sepolia-rpc.giwa.io'
const EAS = process.env.EAS_ADDRESS ?? '0x4200000000000000000000000000000000000021'
const ABI = parseAbi([
  'function getAttestation(bytes32 uid) view returns ((bytes32 uid, bytes32 schema, uint64 time, uint64 expirationTime, uint64 revocationTime, bytes32 refUID, address recipient, address attester, bool revocable, bytes data))',
])

const [uid, expected] = process.argv.slice(2)
if (!/^0x[0-9a-fA-F]{64}$/.test(uid ?? '')) {
  console.error('사용법: node scripts/audit.mjs <EAS 증명 번호 0x…64자리> [서버 data_hash]')
  process.exit(2)
}

const client = createPublicClient({ transport: http(RPC_URL) })
const a = await client.readContract({ address: EAS, abi: ABI, functionName: 'getAttestation', args: [uid] })
const kind = Object.keys(SCHEMAS).find((k) => schemaUid(SCHEMAS[k]).toLowerCase() === a.schema.toLowerCase())
if (!kind) {
  console.error('StepUp 스키마가 아닌 증명입니다:', a.schema)
  process.exit(1)
}
const names = SCHEMAS[kind].split(',').map((f) => f.trim().split(/\s+/)[1])
const values = decodeAbiParameters(parseAbiParameters(SCHEMAS[kind]), a.data)
const fields = Object.fromEntries(names.map((n, i) => [n, typeof values[i] === 'bigint' ? values[i].toString() : values[i]]))
console.log(JSON.stringify({ kind, time: new Date(Number(a.time) * 1000).toISOString(), revoked: a.revocationTime > 0n,
  attester: a.attester, recipient: a.recipient, fields }, null, 2))

if (expected) {
  const ok = String(fields.dataHash ?? '').toLowerCase() === expected.toLowerCase()
  console.log(ok ? '일치 — 서버 기록이 보낸 뒤로 바뀌지 않았습니다' : '불일치 — 서버 기록과 체인 기록이 다릅니다')
  process.exit(ok ? 0 : 1)
}
