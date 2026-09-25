import { SNEAKERS_ABI } from './chain.js'
import { FACTIONS, RARITIES } from './typed.js'
import { HttpError } from './supabase.js'

/**
 * 신발 NFT 메타데이터 — tokenURI(= baseURI + 번호) 가 가리키는 곳.
 *
 * 신발마다 레벨 · 스탯이 달라지므로 IPFS 에 미리 올려 둘 수 없다. 대신 체인의
 * statsOf 를 그 자리에서 읽어 만든다. 그림은 웹사이트의 앱 그림 그대로
 * (web/assets/sneakers — res/drawable-nodpi/sneaker_*.webp 와 같은 파일).
 *
 * 컨트랙트 배포 때 SNEAKER_BASE_URI 를 `https://<워커 주소>/v2/meta/` 로 준다.
 * 나중에 주소가 바뀌면 관리자 지갑으로 setBaseURI 한 번이면 된다.
 */

/** 모델 이름 — 앱 도감(domain/Sneaker.kt)의 순서 그대로. 속성마다 13종, 번호는 그림 파일 번호. */
export const MODEL_NAMES = {
  FIRE: ["INFERNO CROWN", "PHOENIX SURGE", "MAGMA PULSE", "EMBER NOVA", "CINDER VORTEX", "HEAT RUNNER", "TORCH SPRINT", "ASH BLAZE", "FLARE SHIFT", "SPARK RUNNER", "CORE RUNNER", "REDLINE", "WARM UP"],
  WATER: ["ABYSS TIDE", "LEVIATHAN FLOW", "TSUNAMI ARC", "AZURE CURRENT", "DEEPWAVE PRIME", "WATER RUNNER", "RIPPLE PACE", "REEF GLIDE", "OCEAN SPRINT", "BLUE RUNNER", "BROOK RUNNER", "MIST RUNNER", "TIDE LITE"],
  LIGHTNING: ["THUNDER ZENITH", "VOLT PHANTOM", "STORM CIRCUIT", "VOLT APEX", "PLASMA RUNNER", "THUNDER RUNNER", "VOLT DASH", "FLASH PACE", "SPARK BLADE", "YELLOW RUNNER", "STATIC RUNNER", "PULSE RUNNER", "CHARGE LITE"],
  WIND: ["ZEPHYR CROWN", "TEMPEST WING", "AERO PHANTOM", "GALE ORBIT", "CYCLONE STEP", "WIND RUNNER", "BREEZE GLIDE", "AIR DASH", "SKY PACE", "CLOUD RUNNER", "FEATHER RUNNER", "DRIFT RUNNER", "LIFT LITE"],
}

const RARITY_LABEL = ['Common', 'Rare', 'Epic', 'Legendary']
const THEME_LABEL = { FIRE: 'Fire', WATER: 'Water', LIGHTNING: 'Lightning', WIND: 'Wind' }

/** 모델 번호(속성 × 100 + 등급 × 10 + 변형) → 도감 칸. 앱 SneakerDesigns.indexOf 와 같다. */
export function modelInfo(model) {
  const faction = FACTIONS[Math.floor(model / 100)]
  const rarityIndex = Math.floor(model / 10) % 10
  const variant = model % 10
  const no = [10, 6, 3, 1][rarityIndex] + variant // COMMON 10+v · RARE 6+v · EPIC 3+v · LEGENDARY 1+v
  if (!faction || rarityIndex > 3 || no < 1 || no > 13) throw new Error(`알 수 없는 모델 ${model}`)
  return {
    faction,
    rarity: RARITIES[rarityIndex],
    rarityIndex,
    no,
    name: MODEL_NAMES[faction][no - 1],
    file: `sneaker_${faction.toLowerCase()}_${String(no).padStart(2, '0')}.webp`,
  }
}

/** 체인 스탯 → OpenSea 형식 메타데이터 */
export function metadataOf(tokenId, stats, locked, imageBase) {
  const m = modelInfo(Number(stats.model))
  const level = Number(stats.level)
  // 화면과 같은 실효 값 — 레벨 한 칸에 효율성 +0.5%p, 착화감 +0.2%p (최대 20%)
  const efficiency = (Number(stats.efficiencyBps) + 50 * (level - 1)) / 100
  const comfort = Math.min(Number(stats.comfortBps) + 20 * (level - 1), 2000) / 100
  const genesis = Number(stats.genesisNo)
  const attributes = [
    { trait_type: 'Rarity', value: RARITY_LABEL[m.rarityIndex] },
    { trait_type: 'Theme', value: THEME_LABEL[m.faction] },
    { trait_type: 'Model', value: m.name },
    { display_type: 'number', trait_type: 'Level', value: level },
    { trait_type: 'Efficiency', value: `+${efficiency}%` },
    { trait_type: 'Comfort', value: `${comfort}%` },
    { display_type: 'number', trait_type: 'Durability', value: Number(stats.durability) / 100 },
    { trait_type: 'Transfer', value: locked ? 'Locked (run 50 km in the app)' : 'Unlocked' },
  ]
  if (genesis) attributes.push({ display_type: 'number', trait_type: 'Genesis', value: genesis })
  return {
    name: `${m.name}${genesis ? ` · Genesis #${genesis}` : ''} #${tokenId}`,
    description:
      'StepUp 러닝 신발. 앱으로 넣으면 신고 달릴 수 있고, 레벨 · 내구도는 앱에서 달라진다. ' +
      'A StepUp running sneaker — deposit it into the app to run with it.',
    image: `${imageBase}${m.file}`,
    external_url: 'https://stepupcrew.com',
    attributes,
  }
}

export async function sneakerMetadata(env, deps, id) {
  if (!/^\d{1,20}$/.test(id)) throw new HttpError(400, '신발 번호가 올바르지 않습니다')
  const c = deps.clients(env)
  const tokenId = BigInt(id)
  let stats, locked
  try {
    ;[stats, locked] = await Promise.all([
      c.publicClient.readContract({ address: c.addresses.sneakers, abi: SNEAKERS_ABI, functionName: 'statsOf', args: [tokenId] }),
      c.publicClient.readContract({ address: c.addresses.sneakers, abi: SNEAKERS_ABI, functionName: 'transferLocked', args: [tokenId] }),
    ])
  } catch {
    throw new HttpError(404, '없는 신발입니다')
  }
  return metadataOf(id, stats, locked, env.IMAGE_BASE ?? 'https://stepupcrew.com/assets/sneakers/')
}
