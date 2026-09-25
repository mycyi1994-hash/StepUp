// 워커 배포 — 워커가 도는 Cloudflare 계정(workers.dev 이름 WORKER_SUBDOMAIN, 기본 gana003)으로만 올린다.
//
// wrangler.toml 에 계정을 적지 않으므로 `npx wrangler deploy` 를 그냥 돌리면 지금 로그인한(또는 토큰의)
// 계정으로 올라간다. 다른 계정이면 키가 없는 복제 워커가 생기고 진짜 워커는 옛 버전으로 남는다.
// 그래서 배포는 이 스크립트(`npm run deploy`)로만 한다. 자동 배포(deploy-attester.yml)도 이것을 쓴다.
//
//   CLOUDFLARE_API_TOKEN 이 있으면: 토큰이 닿는 계정 중 workers.dev 이름이 맞는 계정을 찾아 그 계정으로 올린다.
//   없으면(wrangler login): CLOUDFLARE_ACCOUNT_ID 를 직접 줘야 한다(대시보드 주소의 계정 ID).
// 뒤에 붙인 인자는 wrangler deploy 로 넘긴다(예: -- --var COMMIT_SHA:abc).
import { spawnSync } from 'node:child_process'

const want = process.env.WORKER_SUBDOMAIN || 'gana003'
const token = process.env.CLOUDFLARE_API_TOKEN
const api = async (path) => {
  const res = await fetch(`https://api.cloudflare.com/client/v4${path}`, { headers: { authorization: `Bearer ${token}` } })
  const body = await res.json().catch(() => ({}))
  if (!res.ok || body.success === false) throw new Error(`${path}: ${res.status}`)
  return body.result
}

let account = process.env.CLOUDFLARE_ACCOUNT_ID || ''
if (token) {
  let found = ''
  for (const a of (await api('/accounts')) ?? []) {
    const sub = await api(`/accounts/${a.id}/workers/subdomain`).then((r) => r?.subdomain ?? '').catch(() => '')
    console.log(`계정 ${a.id} → workers.dev: ${sub || '(없음)'}`)
    if (sub === want) found = a.id
  }
  if (!found) {
    console.error(`이 토큰으로는 워커가 도는 계정(${want}.workers.dev)에 올릴 수 없습니다. 그 계정에 로그인해 토큰을 다시 만들어 주세요.`)
    process.exit(1)
  }
  if (account && account !== found) {
    console.error(`CLOUDFLARE_ACCOUNT_ID(${account})가 ${want}.workers.dev 의 계정(${found})과 다릅니다.`)
    process.exit(1)
  }
  account = found
} else if (!account) {
  console.error(`CLOUDFLARE_API_TOKEN 이 없으면 CLOUDFLARE_ACCOUNT_ID(${want}.workers.dev 가 있는 계정)를 직접 주세요.`)
  process.exit(1)
}

const r = spawnSync('npx', ['wrangler', 'deploy', ...process.argv.slice(2)], {
  stdio: 'inherit',
  env: { ...process.env, CLOUDFLARE_ACCOUNT_ID: account },
})
process.exit(r.status ?? 1)
