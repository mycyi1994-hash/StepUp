#!/usr/bin/env node
/**
 * 서버 키 하나를 새로 만들어 **바로** Cloudflare Secret 에 넣는다.
 * 화면에는 주소만 나온다. 개인키는 어디에도 찍히지 않고 파일로도 남지 않는다.
 *
 *   node scripts/new-key.mjs ATTESTER_PRIVATE_KEY
 *   node scripts/new-key.mjs SNEAKER_SIGNER_KEY
 *   node scripts/new-key.mjs RELAYER_PRIVATE_KEY
 *   node scripts/new-key.mjs GUARDIAN_PRIVATE_KEY
 *
 * 먼저 `npx wrangler login` 이 되어 있어야 한다. 나온 주소를 배포 .env 에 적는다.
 * 키를 잃어도 괜찮다 — 새로 만들고 관리자 지갑으로 컨트랙트의 주소만 바꾸면 된다.
 */
import { spawn } from 'node:child_process'
import { generatePrivateKey, privateKeyToAccount } from 'viem/accounts'

const NAMES = ['ATTESTER_PRIVATE_KEY', 'SNEAKER_SIGNER_KEY', 'RELAYER_PRIVATE_KEY', 'GUARDIAN_PRIVATE_KEY']
const name = process.argv[2]
if (!NAMES.includes(name)) {
  console.error(`이름을 하나 골라 주세요: ${NAMES.join(', ')}`)
  process.exit(1)
}

const key = generatePrivateKey()
const address = privateKeyToAccount(key).address

const child = spawn('npx', ['wrangler', 'secret', 'put', name], { stdio: ['pipe', 'inherit', 'inherit'] })
child.stdin.write(key)
child.stdin.end()
child.on('exit', (code) => {
  if (code !== 0) {
    console.error('\nCloudflare 에 넣지 못했습니다. 키는 버려졌습니다 — 다시 실행하세요.')
    process.exit(code ?? 1)
  }
  console.log(`\n${name} → Cloudflare 에 넣었습니다.`)
  console.log(`주소: ${address}`)
  if (name === 'RELAYER_PRIVATE_KEY') console.log('이 주소에 가스용 테스트 ETH 를 넣어 주세요 (https://faucet.giwa.io).')
})
