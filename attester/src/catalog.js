import { RARITIES, isV3Token } from './typed.js'
import { SNEAKERS_V3_ABI } from './chain.js'
import { SHOE_MODELS } from './shoe-catalog.js'

/**
 * 새 신발 도감(0045 · src/shoe-catalog.js)을 v3 컨트랙트 도감에 더한다 — 릴레이어가 curator 라 할 수 있는 일은
 * 도감 추가뿐이다(있는 모델은 바꾸지도 지우지도 못한다). 없는 모델만 한 거래로 더한다.
 *
 * 새 모델 신발의 금고 발행 · 꺼내기(v3 release)는 컨트랙트 도감에 그 모델이 있어야 받는다 — 그래서 2분 작업은
 * 도감이 다 들어간 뒤에 v3 일을 보낸다. 보낸 뒤 확정될 때까지는 다시 보내지 않는다(CATALOG_WAIT_BLOCKS).
 */

/** OP 스택 기본 탑재 Multicall3 — 70개 모델을 요청 한 번으로 확인한다(무료 워커는 실행마다 요청 50개) */
export const MULTICALL3 = '0xcA11bde05977b3631167028862bE2a173976CA11'

/** 보낸 도감 추가가 확정되기를 기다리는 블록 수(1초에 한 블록 — 10분) */
export const CATALOG_WAIT_BLOCKS = 600n

export const catalogMarker = (chainId, address) => `v3-models@${chainId}:${String(address).toLowerCase()}`

/** 새 도감 모델 번호 — 이 번호의 신발은 v3 로만 체인에 오른다(v2 도감에 없다) */
export function isCatalogModel(model) {
  return model != null && Object.hasOwn(SHOE_MODELS, Number(model))
}

/**
 * 이 신발 작업(꺼내기 · 지갑 선물 발행)을 v3 컨트랙트로 보내는가 — 금고로 발행된 v3 토큰(1000001~)을 꺼낼 때,
 * 또는 v2 도감에 없는 새 도감 모델을 처음 발행할 때. 나머지(예전 52종의 첫 발행 · v2 토큰)는 v2.
 */
export function releasesOnV3(p) {
  const tokenless = p.token_id == null || p.token_id === ''
  return (p.kind === 'SNEAKER_WITHDRAW' && isV3Token(p.token_id)) || (tokenless && isCatalogModel(p.model_id))
}

/** 컨트랙트 등급 번호(0 COMMON · 1 RARE · 2 EPIC · 3 LEGENDARY) */
export function catalogRarity(model) {
  const r = RARITIES.indexOf(SHOE_MODELS[model]?.rarity)
  if (r < 0) throw new Error(`알 수 없는 모델 ${model}`)
  return r
}

/** v3 도감에 새 모델이 다 있는가. 없으면 한 번 더한다. { ready, missing?, sent? } */
export async function ensureCatalog(env, deps, c) {
  const address = c.addresses.sneakersV3
  if (!address) return { ready: false, reason: 'not configured' }
  const ids = Object.keys(SHOE_MODELS).map(Number)
  const exists = await c.publicClient.multicall({
    multicallAddress: MULTICALL3,
    allowFailure: false,
    contracts: ids.map((id) => ({ address, abi: SNEAKERS_V3_ABI, functionName: 'modelExists', args: [id] })),
  })
  const missing = ids.filter((_, i) => !exists[i])
  if (missing.length === 0) return { ready: true }

  const marker = catalogMarker(c.chain.id, address)
  const [sentAt, head] = await Promise.all([
    deps.rpc(env, 'attester_cursor_get', { p_name: marker }),
    c.publicClient.getBlockNumber(),
  ])
  if (sentAt != null && BigInt(head) - BigInt(sentAt) < CATALOG_WAIT_BLOCKS) {
    return { ready: false, reason: 'adding models', missing: missing.length }
  }
  // 표시를 먼저 남긴다 — 보내기가 실패하면 기다렸다가 다시 보낸다(두 번 보내면 두 번째는 "model exists" 로 되돌아간다)
  await deps.rpc(env, 'attester_cursor_set', { p_name: marker, p_block: Number(head) })
  const hash = await c.relayer.writeContract({
    address,
    abi: SNEAKERS_V3_ABI,
    functionName: 'addModels',
    args: [missing, missing.map(catalogRarity)],
  })
  console.log(JSON.stringify({ v3: 'adding models', count: missing.length, tx: hash }))
  return { ready: false, sent: hash, missing: missing.length }
}
