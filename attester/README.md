# StepUp 어테스터 v2 (Cloudflare Worker)

서버(Supabase)가 **예약한** 체인 작업만 서명하고, 가스비를 대신 내 체인에 보낸다.
금액 · 신발 스탯 · 받는 지갑은 전부 서버가 정한다 — 요청 본문에서는 작업 번호만 받는다.
예전 `/claim`(폰이 보낸 러닝 데이터로 서명)과 `/draw/authorization`(v1 뽑기)은 없앴다.

## 하는 일

| 경로 | 무엇 |
|---|---|
| `POST /v2/wallet/link` | 지갑 서명(`wallet_link_challenge` 문장)을 확인하고 서버에 지갑을 붙인다. 새 지갑엔 테스트 가스 조금 |
| `POST /v2/ops/:id/execute` | 서버가 예약한 SUP 꺼내기 · 신발 꺼내기 · 보너스 발행을 서명 → 대납 제출. 응답은 `SUBMITTED` (완료 아님) |
| `GET /v2/meta/:tokenId` · `GET /v3/meta/:tokenId` | 신발 NFT 메타데이터 — 체인 스탯을 읽어 이름 · 그림 · 속성을 만든다 (`tokenURI` 가 가리키는 곳) |
| `GET /health` | 설정된 주소들 |
| 1분마다 | ① 확정 블록까지 이벤트를 읽어 서버에 반영(v3 포함) ② 유효 시간 + 안전 마진이 지난 작업을 체인 확인 뒤 되돌림 ③ 체인과 서버 장부 대조 — 서버가 모르는 지급 · 발행이 보이면 **서버와 컨트랙트를 모두 정지** |
| 2분마다(따로) | 체인 기록 보내기(`src/jobs.js`, 0044 `chain_jobs`) — 러닝 증명 · 코스 완주 · 배지(EAS), 뽑은 신발 금고 발행 · 강화 · 수리 반영(v3). v3 컨트랙트가 없으면 한 번 배포한다(`src/v3.js`) |

### 체인 기록 (2분마다)

서버가 줄 세운 일(`chain_jobs`)을 몇 개씩 가져가(`attester_jobs_claim`) 가스비를 대신 내 보낸다. 자세한 것은
[`docs/온체인-활동-1-2단계.md`](../docs/온체인-활동-1-2단계.md).

- **한 번만**: 서명한 거래(raw)와 번호(nonce)를 서버에 먼저 적고(`attester_jobs_signed`) 적힌 것만 보낸다.
  끊기면 같은 거래를 다시 보낼 뿐이다. 새로 서명하는 것은 그 번호가 다른 거래로 쓰였고 이 거래의 영수증이
  없음을 세 번 본 뒤다. v3 발행 · 갱신은 컨트랙트도 작업 번호로 한 번만 받는다.
- **보내기 전 가스 재기**: 체인이 받지 않을 일(바뀐 것 없는 갱신 · 금고에 없는 신발 · 취소된 번호)은 거두고,
  하루 상한 · 멈춤은 뒤로 미룬다. EAS 스키마가 없으면(`InvalidSchema`) 등록을 한 번 보낸다.
- **확정**: 확정(safe) 블록의 영수증만 확정으로 본다. 결과(EAS 증명 번호 · v3 토큰 번호)를 서버에 적는다.
  서버가 받지 않은 확정(토큰 번호 어긋남)은 멈춤 신호다.
- **몫**: 1분 작업과 따로 도는 실행이라 요청 수(무료 50) · CPU 를 나눠 쓰지 않는다. 한 번에 `CHAIN_JOBS_PER_RUN`
  (기본 3)건, 릴레이어 잔액이 `JOBS_MIN_RELAYER_WEI`(0.003 ETH) 아래면 보내지 않는다 — 꺼내기 가스비 몫.
- **RPC 수 제한**: 워커는 다른 워커들과 같은 주소로 나가 공개 RPC 의 수 제한을 함께 쓴다. 짝수 분에 1분 작업과
  겹치지 않게 2분 작업은 `JOBS_START_DELAY_SEC`(30초) 쉬었다 시작하고, RPC 가 "over rate limit" · 429 를 주면
  잠깐 쉬었다 두 번까지 다시 보낸다(`rpcFetch`).

로그인은 사용자가 보낸 Supabase 토큰을 Supabase 에 물어 확인한다. 워커의 DB 권한은
`attester_*` 함수뿐이고(전용 계정 또는 `stepup_attester` 역할), service_role 은 쓰지 않는다.
워커 자신의 로그인(전용 계정)은 한 번에 하나만 하고, 실패하면 1분(수 제한이면 5분) 동안 다시 하지 않는다 —
Supabase 로그인 요청 수 제한(5분에 30번)을 실패한 재시도들이 계속 채우지 않게. 실패하면 로그에
`attester login failed <상태> <오류 코드>` 가 남는다(`invalid_credentials` 면 `ATTESTER_PASSWORD` 를 다시 넣는다).

실행 기록은 Actions → **Attester logs** → Run workflow 로 몇 분 받아 볼 수 있다(`.github/workflows/attester-logs.yml`).

## 키 (전부 Secret)

| 이름 | 역할 | 컨트랙트 |
|---|---|---|
| `ATTESTER_PRIVATE_KEY` | SUP 꺼내기 서명 | `RewardDistributor.attester` |
| `SNEAKER_SIGNER_KEY` | 신발 발행 · 반환 서명(v2 · v3), v3 금고 발행 · 스탯 갱신 서명 | `StepUpSneakers.signer` · `StepUpSneakersV3.signer` |
| `RELAYER_PRIVATE_KEY` | 가스비 대납 · EAS 증명을 보내는 주소(attester) · v3 배포 · v3 도감 추가(curator) | `StepUpSneakersV3.curator` |
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

배포는 늘 `npm run deploy` 로 한다 — 워커가 도는 계정(`gana003.workers.dev`)을 찾아 그 계정으로만 올린다
(`npx wrangler deploy` 를 그냥 쓰면 로그인한 다른 계정에 키 없는 복제 워커가 생길 수 있다).
토큰(`CLOUDFLARE_API_TOKEN`)이 없으면 `CLOUDFLARE_ACCOUNT_ID` 를 직접 준다.

0. (처음 한 번) 컨트랙트보다 워커를 먼저 `npm run deploy` — 주소(`https://stepup-attester.<계정>.workers.dev`)가
   정해져야 컨트랙트의 `SNEAKER_BASE_URI` 를 넣을 수 있다. 주소가 비어 있는 동안 워커는 요청을 받지 않고(503) 1분 작업도 쉰다
1. 컨트랙트 v2 배포 뒤 `wrangler.toml` 의 `DISTRIBUTOR_ADDRESS` · `SNEAKERS_ADDRESS` · `VAULT_ADDRESS` · `START_BLOCK` 을 채운다
2. 위 키 · 계정을 넣는다
3. `npm run deploy`
4. `curl https://<워커 주소>/health` 로 주소가 컨트랙트 설정과 같은지 확인

### 고친 뒤 다시 배포

`attester/` 를 바꾼 PR 이 main 에 합쳐지면 `.github/workflows/deploy-attester.yml` 이 검사 후 배포하고
`/health` 로 확인한다. 저장소 Secrets 에 `CLOUDFLARE_API_TOKEN`(“Edit Cloudflare Workers” 템플릿)이
있어야 한다 — 워커가 도는 계정에서 만든 토큰이어야 하고, 다른 계정 토큰이면 배포하지 않고 멈춘다. 없으면
건너뛰므로 그때는 `cd attester` → `npm ci` → `npm run deploy` 를 직접 돌린다. (SQL 과 워커는 어느 쪽을 먼저 올려도 서로 깨지지 않게 만든다.)

## 검사

```bash
npm test                              # 서명 재료 · 요청 처리 · 이벤트 변환 (서버 SQL 과 대조 포함)
cd ../contracts && npx hardhat test   # 이 워커가 만든 서명을 실제 컨트랙트가 받는지
```
