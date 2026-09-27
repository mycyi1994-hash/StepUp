import { concat, getContractAddress } from 'viem'
import { V3_DEPLOYMENTS } from './v3-deploy.js'

/**
 * v3 신발 컨트랙트 — CREATE2 배포기로 한 번 올린다.
 *
 * 배포 재료(src/v3-deploy.js)는 contracts/scripts/v3-initcode.js 가 설정 파일과 컴파일 결과로 만든다.
 * 주소는 배포기 · salt · initcode 로만 정해지므로 wrangler.toml 의 SNEAKERS_V3_ADDRESS 와 같아야 하고,
 * 여기서도 다시 계산해 본다 — 다르면 보내지 않는다(엉뚱한 주소에 두 번째 컨트랙트가 생긴다).
 * 관리자(owner)는 생성자에 들어 있어, 배포를 보낸 릴레이어는 아무 권한도 갖지 않는다.
 *
 * 보낸 뒤 확정될 때까지는 다시 보내지 않는다(DEPLOY_WAIT_BLOCKS). 표시는 chain_cursors 에 남긴다.
 */

/** 보낸 배포가 확정되기를 기다리는 블록 수 — 이 체인은 1초에 한 블록(10분) */
export const DEPLOY_WAIT_BLOCKS = 600n

export const deployMarker = (chainId, address) => `v3-deploy@${chainId}:${String(address).toLowerCase()}`

/** v3 가 쓸 수 있는가. 없으면 한 번 배포를 보낸다. { ready, reason?, deploying? } */
export async function ensureV3(env, deps, c) {
  const address = c.addresses.sneakersV3
  if (!address) return { ready: false, reason: 'not configured' }
  const d = V3_DEPLOYMENTS[c.chain.id]
  if (!d || d.address.toLowerCase() !== address.toLowerCase()) return { ready: false, reason: 'address mismatch' }

  const code = await c.publicClient.getCode({ address })
  if (code && code !== '0x') return { ready: true }

  const computed = getContractAddress({ opcode: 'CREATE2', from: d.proxy, salt: d.salt, bytecode: d.initcode })
  if (computed.toLowerCase() !== address.toLowerCase()) return { ready: false, reason: 'initcode mismatch' }

  const marker = deployMarker(c.chain.id, address)
  const [sentAt, head] = await Promise.all([
    deps.rpc(env, 'attester_cursor_get', { p_name: marker }),
    c.publicClient.getBlockNumber(),
  ])
  if (sentAt != null && BigInt(head) - BigInt(sentAt) < DEPLOY_WAIT_BLOCKS) return { ready: false, reason: 'deploying' }

  // 표시를 먼저 남긴다 — 보내기가 실패하면 기다렸다가 다시 보낸다(두 번 보내는 쪽보다 낫다)
  await deps.rpc(env, 'attester_cursor_set', { p_name: marker, p_block: Number(head) })
  const hash = await c.relayer.sendTransaction({ to: d.proxy, data: concat([d.salt, d.initcode]) })
  console.log(JSON.stringify({ v3: 'deploying', address, tx: hash }))
  return { ready: false, deploying: hash }
}
