import {
  createPublicClient, createWalletClient, custom, decodeEventLog, defineChain,
  formatUnits, getAddress, http, isAddress, parseAbi, parseAbiItem,
} from 'viem'
import { createEVMClient } from '@metamask/connect-evm'

const chain = defineChain({
  id: 91342,
  name: 'GIWA Sepolia',
  nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
  rpcUrls: { default: { http: ['https://sepolia-rpc.giwa.io'] } },
  blockExplorers: { default: { name: 'GIWA Explorer', url: 'https://sepolia-explorer.giwa.io' } },
})
const supAbi = parseAbi([
  'function balanceOf(address) view returns (uint256)',
  'function allowance(address,address) view returns (uint256)',
  'function approve(address,uint256) returns (bool)',
])
const drawAbi = parseAbi([
  'function drawWithAuth((address to,uint8 category,uint8 faction,uint8 rarity,uint8 variant,uint8 outfitId,uint256 nonce,uint256 deadline),bytes) returns (uint256)',
])
const drawnEvent = parseAbiItem('event Drawn(uint256 indexed tokenId,address indexed owner,uint8 indexed category,uint8 faction,uint8 rarity,uint8 variant,uint8 outfitId,uint256 costBurned)')

const config = window.STEPUP_DRAW_CONFIG || {}
const status = document.querySelector('#status')
const result = document.querySelector('#result')
const shoeButton = document.querySelector('#shoe')
const outfitButton = document.querySelector('#outfit')
const buttons = [shoeButton, outfitButton]
const soundEnabled = new URLSearchParams(location.search).get('sound') === 'on'
const drawSounds = new Set([
  'draw_charge', 'draw_box_open', 'draw_reveal_common', 'draw_reveal_rare',
  'draw_reveal_epic', 'draw_reveal_legendary', 'draw_cancel', 'draw_fail',
])
const configured = isAddress(config.contract || '') && isAddress(config.sup || '') && /^https:\/\//.test(config.attesterUrl || '')
const rpcUrl = config.rpcUrl || chain.rpcUrls.default.http[0]
const publicClient = createPublicClient({ chain, transport: http(rpcUrl) })
let evmClientPromise

function message(value) { status.textContent = value }
function busy(value) { buttons.forEach(button => { button.disabled = value || !configured }) }
function playDrawSound(name) {
  if (!soundEnabled || !drawSounds.has(name)) return false
  const player = new Audio(new URL(`assets/sounds/${name}.wav`, location.href))
  player.volume = 0.45
  player.play().catch(() => {})
  return true
}
const wait = ms => new Promise(resolve => setTimeout(resolve, ms))

if (!configured) {
  busy(false)
  message('GIWA 뽑기 계약이 아직 연결되지 않았습니다.')
} else {
  message('뽑기를 누르면 지갑 연결이 시작됩니다.')
}

async function connectWallet() {
  if (window.ethereum) {
    const accounts = await window.ethereum.request({ method: 'eth_requestAccounts' })
    return { provider: window.ethereum, account: getAddress(accounts[0]) }
  }
  evmClientPromise ??= createEVMClient({
    dapp: {
      name: 'STEPUP Mystery Box',
      url: location.origin,
      iconUrl: new URL('assets/favicon.svg', location.href).href,
    },
    api: { supportedNetworks: { [`0x${chain.id.toString(16)}`]: rpcUrl } },
  })
  const evm = await evmClientPromise
  const { accounts } = await evm.connect({ chainIds: [`0x${chain.id.toString(16)}`] })
  return { provider: evm.getProvider(), account: getAddress(accounts[0]) }
}

async function ensureChain(provider) {
  const hexId = `0x${chain.id.toString(16)}`
  const current = await provider.request({ method: 'eth_chainId' })
  if (Number(current) === chain.id) return
  try {
    await provider.request({ method: 'wallet_switchEthereumChain', params: [{ chainId: hexId }] })
  } catch (error) {
    if (error?.code !== 4902) throw error
    await provider.request({ method: 'wallet_addEthereumChain', params: [{
      chainId: hexId,
      chainName: chain.name,
      nativeCurrency: chain.nativeCurrency,
      rpcUrls: [rpcUrl],
      blockExplorerUrls: [chain.blockExplorers.default.url],
    }] })
  }
}

function prizeLabel(args) {
  if (Number(args.category) === 1) {
    return ['코어 집', '엠버 셸', '타이드 아노락', '볼트 저지', '에어로 윈드브레이커'][Number(args.outfitId) - 1] || '트레이닝복'
  }
  const faction = ['불', '물', '번개', '바람'][Number(args.faction)] || '신발'
  const rarity = ['커먼', '레어', '에픽', '레전더리'][Number(args.rarity)] || ''
  return `${faction} · ${rarity} · ${Number(args.variant) + 1}번 디자인`
}

async function draw(category) {
  if (!configured) return
  busy(true)
  result.hidden = true
  let transactionConfirmed = false
  try {
    message('지갑을 연결해 주세요.')
    const { provider, account } = await connectWallet()
    await ensureChain(provider)
    const wallet = createWalletClient({ account, chain, transport: custom(provider) })
    message('GIWA에서 뽑기 조건을 확인하고 있어요…')
    const response = await fetch(`${config.attesterUrl.replace(/\/$/, '')}/draw/authorization`, {
      method: 'POST',
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({ address: account, category }),
    })
    const data = await response.json()
    if (!response.ok || !data.ok) throw new Error(data.error || '뽑기 인증을 받을 수 없습니다.')
    if (data.chainId !== chain.id || getAddress(data.contract) !== getAddress(config.contract) || getAddress(data.auth.to) !== account || data.auth.category !== category) {
      throw new Error('뽑기 계약 정보가 서로 맞지 않습니다.')
    }
    const cost = BigInt(data.cost)
    const sup = getAddress(config.sup)
    const contract = getAddress(config.contract)
    const [balance, allowance] = await Promise.all([
      publicClient.readContract({ address: sup, abi: supAbi, functionName: 'balanceOf', args: [account] }),
      publicClient.readContract({ address: sup, abi: supAbi, functionName: 'allowance', args: [account, contract] }),
    ])
    if (balance < cost) throw new Error(`SUP 잔액이 부족합니다. 필요: ${formatUnits(cost, 18)} SUP`)
    if (allowance < cost) {
      message(`지갑에서 ${formatUnits(cost, 18)} SUP 사용 승인을 확인해 주세요.`)
      const approval = await wallet.writeContract({ address: sup, abi: supAbi, functionName: 'approve', args: [contract, cost] })
      const approved = await publicClient.waitForTransactionReceipt({ hash: approval })
      if (approved.status !== 'success') throw new Error('SUP 사용 승인이 완료되지 않았습니다.')
    }
    message('지갑에서 뽑기 거래를 확인해 주세요.')
    const auth = data.auth
    const request = {
      to: account,
      category: auth.category,
      faction: auth.faction,
      rarity: auth.rarity,
      variant: auth.variant,
      outfitId: auth.outfitId,
      nonce: BigInt(auth.nonce),
      deadline: BigInt(auth.deadline),
    }
    const hash = await wallet.writeContract({ address: contract, abi: drawAbi, functionName: 'drawWithAuth', args: [request, data.signature] })
    message('GIWA 거래 확인을 기다리고 있어요…')
    const receipt = await publicClient.waitForTransactionReceipt({ hash })
    if (receipt.status !== 'success') throw new Error('뽑기 거래가 완료되지 않았습니다.')
    transactionConfirmed = true
    let prize
    for (const log of receipt.logs) {
      if (getAddress(log.address) !== contract) continue
      try {
        const event = decodeEventLog({ abi: [drawnEvent], data: log.data, topics: log.topics })
        if (event.eventName === 'Drawn' && getAddress(event.args.owner) === account) { prize = event.args; break }
      } catch { /* Another event from the same transaction. */ }
    }
    if (!prize) throw new Error('거래는 확인됐지만 뽑기 결과를 읽지 못했습니다. 탐색기에서 확인해 주세요.')
    message('GIWA 거래가 확인됐습니다. 상자를 여는 중이에요…')
    if (playDrawSound('draw_charge')) await wait(1100)
    if (playDrawSound('draw_box_open')) await wait(750)
    const rarity = Math.max(0, Math.min(3, Number(prize.rarity)))
    playDrawSound(['draw_reveal_common', 'draw_reveal_rare', 'draw_reveal_epic', 'draw_reveal_legendary'][rarity])
    result.replaceChildren()
    const title = document.createElement('strong')
    title.textContent = `${prizeLabel(prize)} 획득 · #${prize.tokenId}`
    const link = document.createElement('a')
    link.href = `${chain.blockExplorers.default.url}/tx/${hash}`
    link.rel = 'noopener noreferrer'
    link.target = '_blank'
    link.textContent = 'GIWA 거래 보기'
    result.append(title, document.createElement('br'), link)
    result.hidden = false
    message('GIWA에서 뽑기가 완료됐습니다.')
  } catch (error) {
    const rejected = error?.code === 4001 || error?.cause?.code === 4001 ||
      /reject|denied|cancel|거절/i.test(error?.shortMessage || error?.message || '')
    if (!transactionConfirmed) playDrawSound(rejected ? 'draw_cancel' : 'draw_fail')
    message(error?.shortMessage || error?.message || '뽑기를 완료하지 못했습니다.')
  } finally {
    busy(false)
  }
}

shoeButton.addEventListener('click', () => draw(0))
outfitButton.addEventListener('click', () => draw(1))
if (new URLSearchParams(location.search).get('category') === 'outfit') outfitButton.focus()
else if (new URLSearchParams(location.search).get('category') === 'shoe') shoeButton.focus()
