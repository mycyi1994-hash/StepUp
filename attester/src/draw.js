import { createPublicClient, defineChain, getAddress, isAddress, http, keccak256, toBytes } from 'viem'
import { privateKeyToAccount } from 'viem/accounts'

const giwaSepolia = defineChain({
  id: 91342,
  name: 'GIWA Sepolia',
  nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
  rpcUrls: { default: { http: ['https://sepolia-rpc.giwa.io'] } },
})

const drawAbi = [
  { type: 'function', name: 'nextNonce', stateMutability: 'view', inputs: [{ type: 'address' }], outputs: [{ type: 'uint256' }] },
  { type: 'function', name: 'roller', stateMutability: 'view', inputs: [], outputs: [{ type: 'address' }] },
  { type: 'function', name: 'shoeCost', stateMutability: 'view', inputs: [], outputs: [{ type: 'uint256' }] },
  { type: 'function', name: 'tracksuitCost', stateMutability: 'view', inputs: [], outputs: [{ type: 'uint256' }] },
]

const types = {
  DrawAuth: [
    { name: 'to', type: 'address' },
    { name: 'category', type: 'uint8' },
    { name: 'faction', type: 'uint8' },
    { name: 'rarity', type: 'uint8' },
    { name: 'variant', type: 'uint8' },
    { name: 'outfitId', type: 'uint8' },
    { name: 'nonce', type: 'uint256' },
    { name: 'deadline', type: 'uint256' },
  ],
}

function response(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      'content-type': 'application/json; charset=utf-8',
      'access-control-allow-origin': '*',
      'access-control-allow-headers': 'content-type',
      'access-control-allow-methods': 'POST, GET, OPTIONS',
    },
  })
}

/**
 * The outcome is fixed for address/category/on-chain nonce. Asking again before
 * paying cannot change it; a new nonce only exists after a confirmed draw.
 * DRAW_SEED is a private high-entropy Worker secret, never a client parameter.
 */
export function drawPrize(seed, address, category, nonce) {
  if (typeof seed !== 'string' || seed.length < 32) throw new Error('DRAW_SEED is missing')
  if (!isAddress(address)) throw new Error('Invalid address')
  if (category !== 0 && category !== 1) throw new Error('Invalid category')
  const key = `${seed}|91342|${address.toLowerCase()}|${category}|${nonce.toString()}`
  const draw = (label, ceiling) => Number(BigInt(keccak256(toBytes(`${key}|${label}`))) % BigInt(ceiling))
  if (category === 1) {
    return { category, faction: 0, rarity: 0, variant: 0, outfitId: draw('outfit', 5) + 1 }
  }
  const roll = draw('rarity', 100)
  const rarity = roll < 55 ? 0 : roll < 83 ? 1 : roll < 96 ? 2 : 3
  const variants = [4, 4, 3, 2]
  return {
    category,
    faction: draw('faction', 4),
    rarity,
    variant: draw('variant', variants[rarity]),
    outfitId: 0,
  }
}

export async function handleDrawAuth(request, env) {
  if (!env.DRAW_CONTRACT_ADDRESS || !env.DRAW_ROLLER_PRIVATE_KEY || !env.DRAW_SEED) {
    return response({ ok: false, error: 'Draw contract/roller is not configured' }, 503)
  }
  if (!isAddress(env.DRAW_CONTRACT_ADDRESS)) {
    return response({ ok: false, error: 'Invalid draw contract address' }, 503)
  }
  let body
  try { body = await request.json() } catch { return response({ ok: false, error: 'JSON body required' }, 400) }
  const { address, category } = body ?? {}
  if (!isAddress(address ?? '') || !Number.isInteger(category) || (category !== 0 && category !== 1)) {
    return response({ ok: false, error: 'Valid address and category (0 or 1) required' }, 400)
  }

  const client = createPublicClient({
    chain: giwaSepolia,
    transport: http(env.GIWA_SEPOLIA_RPC || 'https://sepolia-rpc.giwa.io'),
  })
  const wallet = getAddress(address)
  const contract = getAddress(env.DRAW_CONTRACT_ADDRESS)
  const signer = privateKeyToAccount(env.DRAW_ROLLER_PRIVATE_KEY)
  let nonce, roller, cost
  try {
    ;[nonce, roller, cost] = await Promise.all([
      client.readContract({ address: contract, abi: drawAbi, functionName: 'nextNonce', args: [wallet] }),
      client.readContract({ address: contract, abi: drawAbi, functionName: 'roller' }),
      client.readContract({ address: contract, abi: drawAbi, functionName: category === 0 ? 'shoeCost' : 'tracksuitCost' }),
    ])
  } catch {
    return response({ ok: false, error: 'GIWA draw contract is unavailable' }, 502)
  }
  if (getAddress(roller) !== signer.address) {
    return response({ ok: false, error: 'Draw roller does not match deployed contract' }, 503)
  }

  let prize
  try { prize = drawPrize(env.DRAW_SEED, wallet, category, nonce) }
  catch { return response({ ok: false, error: 'Draw seed is not configured' }, 503) }
  const deadline = BigInt(Math.floor(Date.now() / 1000) + 600)
  const auth = { to: wallet, ...prize, nonce, deadline }
  const signature = await signer.signTypedData({
    domain: { name: 'StepUpMysteryDraw', version: '1', chainId: 91342, verifyingContract: contract },
    types,
    primaryType: 'DrawAuth',
    message: auth,
  })
  return response({
    ok: true,
    chainId: 91342,
    contract,
    cost: cost.toString(),
    auth: { ...auth, nonce: nonce.toString(), deadline: deadline.toString() },
    signature,
  })
}
