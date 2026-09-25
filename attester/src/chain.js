import { createPublicClient, createWalletClient, defineChain, http, parseAbi } from 'viem'
import { privateKeyToAccount } from 'viem/accounts'

/**
 * 체인 연결. 체인 번호 · RPC · 컨트랙트 주소는 wrangler.toml 의 vars, 키는 Secret 에서 온다.
 * 테스트넷(91342)과 메인넷(9134)은 설정만 다르다.
 */

export const DISTRIBUTOR_ABI = parseAbi([
  'function currentDay() view returns (uint64)',
  'function sessionClaimed(bytes32) view returns (bool)',
  'function totalDistributed() view returns (uint256)',
  'function paused() view returns (bool)',
  'function pause()',
  'function claim((address runner, bytes32 sessionHash, uint256 amount, uint64 day, uint256 deadline) c, bytes signature)',
  'event Claimed(address indexed runner, bytes32 indexed sessionHash, uint256 amount, uint64 indexed day, uint256 dayRemaining)',
  // 되돌아간 이유를 이름으로 읽으려면 오류도 적어 둬야 한다(없으면 0x6882… 같은 번호만 나온다)
  'error ClaimExpired(uint256 deadline)',
  'error SessionAlreadyClaimed(bytes32 sessionHash)',
  'error BadSignature()',
  'error ZeroAmount()',
  'error DayInFuture(uint64 day, uint64 currentDay)',
  'error DayTooOld(uint64 day, uint64 currentDay)',
  'error DailyBudgetExceeded(uint64 day, uint256 requested, uint256 remaining)',
  'error PoolExhausted(uint256 requested, uint256 balance)',
  'error ClaimTooLarge(uint256 amount, uint256 maxClaim)',
  'error PayoutCapReached(uint64 payoutDay, uint256 requested, uint256 remaining)',
  'error EnforcedPause()',
])

export const SNEAKERS_ABI = parseAbi([
  'function opUsed(bytes32) view returns (bool)',
  'function statsOf(uint256) view returns ((uint32 model, uint8 rarity, uint16 level, uint16 efficiencyBps, uint16 comfortBps, uint16 durability, uint32 genesisNo))',
  'function transferLocked(uint256) view returns (bool)',
  'function paused() view returns (bool)',
  'function pause()',
  'function release((bytes32 opId, address to, uint256 tokenId, uint32 model, uint8 rarity, uint16 level, uint16 efficiencyBps, uint16 comfortBps, uint16 durability, uint32 genesisNo, bool locked, uint64 deadline) r, bytes signature)',
  'event Released(bytes32 indexed opId, uint256 indexed tokenId, address indexed to, bool minted)',
  'event OpCancelled(bytes32 indexed opId)',
  'event Deposited(uint256 indexed tokenId, address indexed from, bytes32 indexed account)',
  'error ReleaseExpired(uint64 deadline)',
  'error OpAlreadyUsed(bytes32 opId)',
  'error BadSignature()',
  'error UnknownModel(uint32 model)',
  'error RarityMismatch(uint32 model, uint8 rarity)',
  'error BadStats()',
  'error IdentityChanged(uint256 tokenId)',
  'error LevelWentDown(uint256 tokenId, uint16 was, uint16 now_)',
  'error NotInVault(uint256 tokenId)',
  'error GenesisTaken(uint32 genesisNo)',
  'error DailyMintCapReached(uint64 day)',
  'error DailyReleaseCapReached(uint64 day)',
  'error GenesisOutOfRange(uint32 genesisNo)',
  'error EnforcedPause()',
])

export const VAULT_ABI = parseAbi([
  'function totalDeposited() view returns (uint256)',
  'function paused() view returns (bool)',
  'function pause()',
  'event Deposited(address indexed from, bytes32 indexed account, uint256 amount)',
])

export function chainOf(env) {
  const id = Number(env.CHAIN_ID)
  return defineChain({
    id,
    name: id === 9134 ? 'GIWA' : 'GIWA Sepolia',
    nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
    rpcUrls: { default: { http: [env.RPC_URL] } },
  })
}

export function clients(env) {
  const chain = chainOf(env)
  const transport = http(env.RPC_URL)
  const publicClient = createPublicClient({ chain, transport })
  const wallet = (key) => createWalletClient({ chain, transport, account: privateKeyToAccount(key) })
  return {
    chain,
    publicClient,
    /** SUP 꺼내기 서명 */
    attester: privateKeyToAccount(env.ATTESTER_PRIVATE_KEY),
    /** 신발 발행 · 반환 서명 */
    sneakerSigner: privateKeyToAccount(env.SNEAKER_SIGNER_KEY),
    /** 가스비를 내고 거래를 보낸다 */
    relayer: wallet(env.RELAYER_PRIVATE_KEY),
    /** 긴급 정지만 할 수 있는 키 */
    guardian: env.GUARDIAN_PRIVATE_KEY ? wallet(env.GUARDIAN_PRIVATE_KEY) : null,
    addresses: {
      distributor: env.DISTRIBUTOR_ADDRESS,
      sneakers: env.SNEAKERS_ADDRESS,
      vault: env.VAULT_ADDRESS,
    },
  }
}
