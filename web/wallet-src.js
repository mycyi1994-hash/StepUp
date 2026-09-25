import {
  createPublicClient, createWalletClient, custom, defineChain, getAddress, http,
  parseAbi, parseAbiItem, stringToHex,
} from 'viem'
import { createEVMClient } from '@metamask/connect-evm'
import { accountRef, createApi, formatSup, jwtClaims, opStatusLabel, parseSup, tokenFromHash } from './wallet-core.js'

// STEPUP 지갑 페이지 — 지갑 연결 · 보너스 뽑기 · 꺼내기 · 넣기.
//
// 금액 · 신발 · 한도는 서버가 정하고, 서명과 가스비는 어테스터 워커가 맡는다.
// 이 페이지가 "완료"라고 말하는 것은 서버가 체인에서 확정을 본 뒤(CONFIRMED) 뿐이다.

const config = window.STEPUP_WALLET_CONFIG || {}
const chain = defineChain({
  id: Number(config.chainId || 91342),
  name: Number(config.chainId) === 9134 ? 'GIWA' : 'GIWA Sepolia',
  nativeCurrency: { name: 'Ether', symbol: 'ETH', decimals: 18 },
  rpcUrls: { default: { http: [config.rpcUrl || 'https://sepolia-rpc.giwa.io'] } },
  blockExplorers: { default: { name: 'GIWA Explorer', url: config.explorer || 'https://sepolia-explorer.giwa.io' } },
})
const configured = Boolean(config.supabaseUrl && config.supabaseKey && config.attesterUrl && config.sneakers && config.vault && config.sup)
const publicClient = createPublicClient({ chain, transport: http(chain.rpcUrls.default.http[0]) })

const supAbi = parseAbi([
  'function name() view returns (string)',
  'function nonces(address) view returns (uint256)',
  'function balanceOf(address) view returns (uint256)',
])
const vaultAbi = parseAbi([
  'function depositWithPermit(uint256 amount, bytes32 account, uint256 deadline, uint8 v, bytes32 r, bytes32 s)',
])
const sneakersAbi = parseAbi([
  'function ownerOf(uint256) view returns (address)',
  'function deposit(uint256 tokenId, bytes32 account)',
])
const RARITY_KO = { COMMON: '일반', RARE: '레어', EPIC: '희귀', LEGENDARY: '레전더리' }
const transferEvent = parseAbiItem('event Transfer(address indexed from, address indexed to, uint256 indexed tokenId)')

// ── 로그인 토큰 ──
const STORE = 'stepup.wallet.token'
let token = tokenFromHash(location.hash)
if (token) {
  try { sessionStorage.setItem(STORE, token) } catch {}
  history.replaceState(null, '', location.pathname + location.search) // 주소창 · 기록에서 지운다
} else {
  try { token = sessionStorage.getItem(STORE) } catch {}
}
function setToken(t) {
  token = t
  try { t ? sessionStorage.setItem(STORE, t) : sessionStorage.removeItem(STORE) } catch {}
}
const api = createApi(config, () => token)

// ── 화면 ──
const app = document.querySelector('#app')
const status = document.querySelector('#status')
function say(text) { status.textContent = text || '' }
function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag)
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'onclick') node.addEventListener('click', v)
    else if (v !== undefined && v !== null && v !== false) node.setAttribute(k, v === true ? '' : v)
  }
  for (const c of children.flat()) if (c !== null && c !== undefined && c !== false) node.append(c)
  return node
}
function card(title, ...children) { return el('section', { class: 'card' }, el('h2', {}, title), ...children) }
function explorerTx(hash) {
  return el('a', { href: `${chain.blockExplorers.default.url}/tx/${hash}`, target: '_blank', rel: 'noopener' }, '거래 보기')
}
async function guard(button, work) {
  const buttons = [...document.querySelectorAll('button')]
  buttons.forEach((b) => { b.disabled = true })
  try {
    await work()
  } catch (e) {
    say(e?.shortMessage || e?.message || '잠시 뒤에 다시 해 주세요')
  } finally {
    buttons.forEach((b) => { b.disabled = false })
  }
}

// ── 지갑 ──
let evmClientPromise
async function connectWallet() {
  let provider
  let accounts
  if (window.ethereum) {
    provider = window.ethereum
    accounts = await provider.request({ method: 'eth_requestAccounts' })
  } else {
    evmClientPromise ??= createEVMClient({
      dapp: { name: 'STEPUP', url: location.origin, iconUrl: new URL('assets/favicon.svg', location.href).href },
      api: { supportedNetworks: { [`0x${chain.id.toString(16)}`]: chain.rpcUrls.default.http[0] } },
    })
    const evm = await evmClientPromise
    ;({ accounts } = await evm.connect({ chainIds: [`0x${chain.id.toString(16)}`] }))
    provider = evm.getProvider()
  }
  await ensureChain(provider)
  const account = getAddress(accounts[0])
  return { provider, account, wallet: createWalletClient({ account, chain, transport: custom(provider) }) }
}

async function ensureChain(provider) {
  const hexId = `0x${chain.id.toString(16)}`
  if (Number(await provider.request({ method: 'eth_chainId' })) === chain.id) return
  try {
    await provider.request({ method: 'wallet_switchEthereumChain', params: [{ chainId: hexId }] })
  } catch (error) {
    if (error?.code !== 4902) throw error
    await provider.request({
      method: 'wallet_addEthereumChain',
      params: [{
        chainId: hexId, chainName: chain.name, nativeCurrency: chain.nativeCurrency,
        rpcUrls: chain.rpcUrls.default.http, blockExplorerUrls: [chain.blockExplorers.default.url],
      }],
    })
  }
}

// ── 단계 1: 로그인 ──
function renderLogin() {
  app.replaceChildren(card('로그인',
    el('p', {}, 'STEPUP 앱의 내 정보 › 지갑에서 열면 바로 로그인됩니다.'),
    el('button', { class: 'primary', onclick: () => { location.href = api.googleLoginUrl(location.origin + location.pathname) } }, '구글로 로그인'),
  ))
}

// ── 단계 2: 2단계 인증 ──
async function renderMfa() {
  const user = await api.user()
  const factor = (user.factors || []).find((f) => f.factor_type === 'totp' && f.status === 'verified')
  const code = el('input', { inputmode: 'numeric', autocomplete: 'one-time-code', maxlength: 6, placeholder: '6자리 번호' })
  let factorId = factor?.id
  const children = []
  if (!factor) {
    const enrolled = await api.enrollTotp()
    factorId = enrolled.id
    children.push(
      el('p', {}, '지갑을 연결하고 꺼내려면 2단계 인증이 필요합니다. Google Authenticator 같은 인증 앱으로 아래 QR 을 찍어 주세요.'),
      el('img', { class: 'qr', src: enrolled.totp.qr_code, alt: '2단계 인증 QR' }),
      el('p', { class: 'mono' }, `직접 입력: ${enrolled.totp.secret}`),
    )
  } else {
    children.push(el('p', {}, '인증 앱에 나온 6자리 번호를 넣어 주세요.'))
  }
  children.push(code, el('button', {
    class: 'primary',
    onclick: (e) => guard(e.target, async () => {
      const ch = await api.challenge(factorId)
      const session = await api.verify(factorId, ch.id, code.value.trim())
      setToken(session.access_token)
      say('2단계 인증을 마쳤습니다.')
      await main()
    }),
  }, '확인'))
  app.replaceChildren(card('2단계 인증', ...children))
}

// ── 작업 따라가기 — 체인에서 확정될 때까지 ──
async function runOp(opId, label) {
  say(`${label}: 서명하고 체인에 보내는 중…`)
  const sent = await api.attester(`/v2/ops/${opId}/execute`)
  say(`${label}: 체인에 보냈습니다. 확정을 기다리는 중…`)
  const deadline = Date.now() + 5 * 60 * 1000
  while (Date.now() < deadline) {
    await new Promise((r) => setTimeout(r, 4000))
    const [op] = await api.select(`chain_ops?id=eq.${opId}&select=status,tx_hash`)
    if (op?.status === 'CONFIRMED') {
      say(`${label}: 완료 (체인에서 확정)`)
      return main()
    }
    if (op?.status === 'EXPIRED') {
      say(`${label}: 체인에 올라가지 않아 되돌렸습니다.`)
      return main()
    }
  }
  say(`${label}: 아직 확정되지 않았습니다. 잠시 뒤 다시 열면 결과가 보입니다. (${sent.tx})`)
  return main()
}

// ── 단계 3~: 지갑 · 꺼내기 · 넣기 ──
async function renderWallet(userId) {
  const [[econ], [wallet], shoes, ops] = await Promise.all([
    api.rpc('my_economy'),
    api.rpc('my_wallet'),
    api.rpc('my_sneakers'),
    api.select('chain_ops?select=id,kind,status,amount,tx_hash,deadline,created_at&order=created_at.desc&limit=8'),
  ])
  const sections = []

  if (wallet?.paused) {
    sections.push(card('잠시 멈춤', el('p', {}, '안전 점검으로 체인 작업을 잠시 멈췄습니다. 잔고와 신발은 그대로 안전합니다.')))
  }

  if (!wallet?.address) {
    sections.push(card('지갑 연결',
      el('p', {}, '지갑을 연결하면 보너스 뽑기 10회(첫 번째는 Genesis 확정)를 드립니다. 한 지갑은 한 계정에만 연결됩니다.'),
      el('button', {
        class: 'primary',
        onclick: (e) => guard(e.target, async () => {
          const { provider, account } = await connectWallet()
          const message = await api.rpc('wallet_link_challenge')
          const nonce = String(message).match(/확인 번호: ([0-9a-f]+)/)?.[1]
          say('지갑 앱에서 서명해 주세요. 거래가 아니며 수수료가 들지 않습니다.')
          const signature = await provider.request({ method: 'personal_sign', params: [stringToHex(message), account] })
          await api.attester('/v2/wallet/link', { address: account, nonce, signature })
          say('지갑을 연결했습니다.')
          await main()
        }),
      }, '지갑 연결하기'),
    ))
    app.replaceChildren(...sections)
    return
  }

  const openAt = wallet.withdraw_open_at ? new Date(wallet.withdraw_open_at) : null
  sections.push(card('내 지갑',
    el('p', { class: 'mono' }, wallet.address),
    el('p', {}, `앱 잔고 ${formatSup(econ?.balance)} SUP · 오늘 꺼낸 양 ${formatSup(wallet.withdrawn_today)} / ${formatSup(wallet.withdraw_daily_limit)} SUP`),
    openAt && openAt > new Date() ? el('p', { class: 'note' }, `보안을 위해 ${openAt.toLocaleString('ko-KR')} 부터 꺼낼 수 있습니다.`) : null,
  ))

  if (wallet.bonus_left > 0) {
    sections.push(card('보너스 뽑기',
      el('p', {}, `${wallet.bonus_left}회 남았습니다.${wallet.genesis_left > 0 ? ' 이번 뽑기는 Genesis(희귀 이상) 확정입니다.' : ''} 뽑은 신발은 지갑으로 발행되고, 앱에서 50km 를 달리기 전까지는 옮길 수 없습니다.`),
      el('button', {
        class: 'primary',
        onclick: (e) => guard(e.target, async () => runOp(await api.rpc('bonus_draw_request'), '보너스 뽑기')),
      }, '보너스 뽑기'),
    ))
  }

  const amount = el('input', { inputmode: 'decimal', placeholder: '꺼낼 SUP' })
  const withdrawable = (shoes || []).filter((s) => s.can_withdraw)
  sections.push(card('지갑으로 꺼내기',
    el('p', {}, '가스비는 STEPUP 이 냅니다.'),
    el('div', { class: 'row' }, amount, el('button', {
      class: 'secondary',
      onclick: (e) => guard(e.target, async () => {
        const v = parseSup(amount.value)
        if (!v) throw new Error('금액을 확인해 주세요')
        await runOp(await api.rpc('sup_withdraw_request', { p_amount: v.text }), 'SUP 꺼내기')
      }),
    }, 'SUP 꺼내기')),
    withdrawable.length
      ? el('ul', { class: 'list' }, withdrawable.map((s) => el('li', {},
          el('span', {}, `${RARITY_KO[s.rarity] ?? s.rarity} Lv${s.level}${Number.isFinite(s.efficiency_bps) ? ` · 효율성 +${(s.efficiency_bps / 100).toFixed(1)}%` : ''} · 내구도 ${Math.round(s.durability)}${s.genesis_no ? ` · Genesis #${s.genesis_no}` : ''}`),
          el('button', {
            class: 'secondary small',
            onclick: (e) => guard(e.target, async () => runOp(await api.rpc('sneaker_withdraw_request', { p_sneaker_id: s.id }), '신발 꺼내기')),
          }, '꺼내기'))))
      : el('p', { class: 'note' }, '꺼낼 수 있는 신발이 없습니다. 무료로 받은 신발은 50km 를 달린 뒤 꺼낼 수 있습니다.'),
  ))

  const depositAmount = el('input', { inputmode: 'decimal', placeholder: '넣을 SUP' })
  const walletShoes = el('ul', { class: 'list' })
  sections.push(card('앱으로 넣기',
    el('p', {}, '넣기는 지갑에서 직접 보내는 거래입니다. 체인에서 확정된 뒤 앱에 반영됩니다.'),
    el('div', { class: 'row' }, depositAmount, el('button', {
      class: 'secondary',
      onclick: (e) => guard(e.target, async () => {
        const v = parseSup(depositAmount.value)
        if (!v) throw new Error('금액을 확인해 주세요')
        const { account, wallet: w } = await connectWallet()
        const deadline = BigInt(Math.floor(Date.now() / 1000) + 1800)
        const [name, nonce] = await Promise.all([
          publicClient.readContract({ address: config.sup, abi: supAbi, functionName: 'name' }),
          publicClient.readContract({ address: config.sup, abi: supAbi, functionName: 'nonces', args: [account] }),
        ])
        say('지갑 앱에서 SUP 사용을 허락해 주세요.')
        const sig = await w.signTypedData({
          domain: { name, version: '1', chainId: chain.id, verifyingContract: config.sup },
          types: { Permit: [
            { name: 'owner', type: 'address' }, { name: 'spender', type: 'address' }, { name: 'value', type: 'uint256' },
            { name: 'nonce', type: 'uint256' }, { name: 'deadline', type: 'uint256' },
          ] },
          primaryType: 'Permit',
          message: { owner: account, spender: config.vault, value: v.wei, nonce, deadline },
        })
        const r = `0x${sig.slice(2, 66)}`
        const s = `0x${sig.slice(66, 130)}`
        const vv = parseInt(sig.slice(130, 132), 16)
        say('지갑 앱에서 넣기 거래를 보내 주세요.')
        const hash = await w.writeContract({
          address: config.vault, abi: vaultAbi, functionName: 'depositWithPermit',
          args: [v.wei, accountRef(userId), deadline, vv, r, s],
        })
        say('보냈습니다. 체인에서 확정되면 앱 잔고에 들어갑니다.')
        status.append(' ', explorerTx(hash))
      }),
    }, 'SUP 넣기')),
    el('button', {
      class: 'secondary',
      onclick: (e) => guard(e.target, async () => {
        const { account, wallet: w } = await connectWallet()
        say('지갑의 신발을 찾는 중…')
        const logs = await publicClient.getLogs({
          address: config.sneakers, event: transferEvent, args: { to: account },
          fromBlock: BigInt(config.startBlock || 0), toBlock: 'latest',
        })
        const ids = [...new Set(logs.map((l) => l.args.tokenId))]
        const owned = []
        for (const id of ids) {
          const owner = await publicClient.readContract({ address: config.sneakers, abi: sneakersAbi, functionName: 'ownerOf', args: [id] })
          if (getAddress(owner) === account) owned.push(id)
        }
        walletShoes.replaceChildren(...(owned.length ? owned.map((id) => el('li', {},
          el('span', {}, `신발 #${id}`),
          el('button', {
            class: 'secondary small',
            onclick: (ev) => guard(ev.target, async () => {
              say('지갑 앱에서 넣기 거래를 보내 주세요.')
              const hash = await w.writeContract({
                address: config.sneakers, abi: sneakersAbi, functionName: 'deposit', args: [id, accountRef(userId)],
              })
              say('보냈습니다. 체인에서 확정되면 앱 신발장에 들어갑니다.')
              status.append(' ', explorerTx(hash))
            }),
          }, '앱으로 넣기'))) : [el('li', {}, '지갑에 STEPUP 신발이 없습니다.')]))
        say('')
      }),
    }, '지갑의 신발 불러오기'),
    walletShoes,
  ))

  if (ops?.length) {
    const KIND = { SUP_WITHDRAW: 'SUP 꺼내기', SNEAKER_WITHDRAW: '신발 꺼내기', BONUS_MINT: '보너스 뽑기' }
    sections.push(card('최근 작업', el('ul', { class: 'list' }, ops.map((o) => {
      const label = KIND[o.kind] ?? o.kind
      // 예약만 되고 체인에 못 보낸 작업(연결이 끊겼을 때)은 유효 시간 안이면 다시 보낼 수 있다.
      // 같은 작업 번호라 체인은 한 번만 받는다.
      const resumable = (o.status === 'RESERVED' || o.status === 'SIGNED') && !o.tx_hash && new Date(o.deadline) > new Date()
      return el('li', {},
        el('span', {}, `${label}${o.amount ? ` ${formatSup(o.amount)} SUP` : ''} — ${opStatusLabel(o.status)}`),
        o.tx_hash ? explorerTx(o.tx_hash) : null,
        resumable
          ? el('button', { class: 'secondary small', onclick: (e) => guard(e.target, () => runOp(o.id, label)) }, '다시 보내기')
          : null)
    }))))
  }

  app.replaceChildren(...sections)
}

async function main() {
  if (!configured) {
    app.replaceChildren(card('준비 중', el('p', {}, 'GIWA 연결을 준비하고 있습니다.')))
    return
  }
  const claims = token ? jwtClaims(token) : null
  if (!claims || claims.exp * 1000 < Date.now() + 30_000) {
    setToken(null)
    renderLogin()
    return
  }
  try {
    if (claims.aal !== 'aal2') {
      await renderMfa()
      return
    }
    await renderWallet(claims.sub)
  } catch (e) {
    if (e?.status === 401) {
      setToken(null)
      renderLogin()
      say('로그인이 만료되었습니다. 다시 로그인해 주세요.')
      return
    }
    say(e?.message || '잠시 뒤에 다시 해 주세요')
  }
}

main()
