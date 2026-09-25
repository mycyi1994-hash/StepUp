# StepUp 어테스터 v2 (Cloudflare Worker)

서버(Supabase)가 **예약한** 체인 작업만 서명하고, 가스비를 대신 내 체인에 보낸다.
금액 · 신발 스탯 · 받는 지갑은 전부 서버가 정한다 — 요청 본문에서는 작업 번호만 받는다.
예전 `/claim`(폰이 보낸 러닝 데이터로 서명)과 `/draw/authorization`(v1 뽑기)은 없앴다.

## 하는 일

| 경로 | 무엇 |
|---|---|
| `POST /v2/wallet/link` | 지갑 서명(`wallet_link_challenge` 문장)을 확인하고 서버에 지갑을 붙인다. 새 지갑엔 테스트 가스 조금 |
| `POST /v2/ops/:id/execute` | 서버가 예약한 SUP 꺼내기 · 신발 꺼내기 · 보너스 발행을 서명 → 대납 제출. 응답은 `SUBMITTED` (완료 아님) |
| `GET /v2/meta/:tokenId` | 신발 NFT 메타데이터 — 체인 스탯을 읽어 이름 · 그림 · 속성을 만든다 (`tokenURI` 가 가리키는 곳) |
| `GET /health` | 설정된 주소들 |
| 1분마다 | ① 확정 블록까지 이벤트를 읽어 서버에 반영 ② 유효 시간 + 안전 마진이 지난 작업을 체인 확인 뒤 되돌림 ③ 체인과 서버 장부 대조 — 서버가 모르는 지급이 보이면 **서버와 세 컨트랙트를 모두 정지** |

로그인은 사용자가 보낸 Supabase 토큰을 Supabase 에 물어 확인한다. 워커의 DB 권한은
`attester_*` 함수뿐이고(전용 계정 또는 `stepup_attester` 역할), service_role 은 쓰지 않는다.

## 키 (전부 Secret)

| 이름 | 역할 | 컨트랙트 |
|---|---|---|
| `ATTESTER_PRIVATE_KEY` | SUP 꺼내기 서명 | `RewardDistributor.attester` |
| `SNEAKER_SIGNER_KEY` | 신발 발행 · 반환 서명 | `StepUpSneakers.signer` |
| `RELAYER_PRIVATE_KEY` | 가스비 대납 (역할 없음, ETH 만) | — |
| `GUARDIAN_PRIVATE_KEY` | 긴급 정지 (재개 불가) | 세 컨트랙트의 `guardian` |
| `ATTESTER_EMAIL` · `ATTESTER_PASSWORD` | Supabase 어테스터 전용 계정 | `economy_settings.attester_user_id` |

키 만들기 — 주소만 화면에 나오고 개인키는 바로 Cloudflare 로 간다(윈도우 PowerShell 도 같다):

```bash
git pull                 # main 최신 — new-key 가 없다고 나오면 이게 빠진 것
npm install
npx wrangler login
npm run new-key -- ATTESTER_PRIVATE_KEY
npm run new-key -- SNEAKER_SIGNER_KEY
npm run new-key -- RELAYER_PRIVATE_KEY     # 이 주소에 테스트 ETH 를 넣는다
npm run new-key -- GUARDIAN_PRIVATE_KEY
```

어테스터 계정: Supabase 대시보드 → Authentication 에서 이 용도로만 쓸 사용자 하나를 만들고(긴 비밀번호),
그 id 를 `supabase/migrations/0027_attester_account.sql` 에 적는다(main 에 합치면 서버에 올라간다).
그 다음 `npx wrangler secret put ATTESTER_EMAIL` · `npx wrangler secret put ATTESTER_PASSWORD`.

## 배포

0. (처음 한 번) 컨트랙트보다 워커를 먼저 `npx wrangler deploy` — 주소(`https://stepup-attester.<계정>.workers.dev`)가
   정해져야 컨트랙트의 `SNEAKER_BASE_URI` 를 넣을 수 있다. 주소가 비어 있는 동안 워커는 요청을 받지 않고(503) 1분 작업도 쉰다
1. 컨트랙트 v2 배포 뒤 `wrangler.toml` 의 `DISTRIBUTOR_ADDRESS` · `SNEAKERS_ADDRESS` · `VAULT_ADDRESS` · `START_BLOCK` 을 채운다
2. 위 키 · 계정을 넣는다
3. `npx wrangler deploy`
4. `curl https://<워커 주소>/health` 로 주소가 컨트랙트 설정과 같은지 확인

### 고친 뒤 다시 배포

`attester/` 를 바꾼 PR 이 main 에 합쳐지면 `.github/workflows/deploy-attester.yml` 이 검사 후 배포하고
`/health` 로 확인한다. 저장소 Secrets 에 `CLOUDFLARE_API_TOKEN`(“Edit Cloudflare Workers” 템플릿)과
`CLOUDFLARE_ACCOUNT_ID` 가 있어야 한다 — 없으면 건너뛰므로 그때는 `cd attester` → `npm ci` →
`npx wrangler deploy` 를 직접 돌린다. (SQL 과 워커는 어느 쪽을 먼저 올려도 서로 깨지지 않게 만든다.)

## 검사

```bash
npm test                              # 서명 재료 · 요청 처리 · 이벤트 변환 (서버 SQL 과 대조 포함)
cd ../contracts && npx hardhat test   # 이 워커가 만든 서명을 실제 컨트랙트가 받는지
```
