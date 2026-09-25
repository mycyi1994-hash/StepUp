# Phase 4 온체인 설계서 — 2026-09-25

> 목적: GASOK Phase 4 KPI(**트랜잭션 수 · TVL · 사용자 수**)를 실제 사용으로 만들기 위한 온체인 설계.
> 지금 StepUp 에서 가장 약한 부분이다. 이 문서는 **설계만** 한다. 코드는 결정 뒤(3단계)에 한다.
> 기준 코드: main 488ee5d. 체인 상태는 2026-09-25 GIWA Sepolia 에서 직접 조회함.

## 0. 지금 상태 — 한 줄 요약: **온체인 사용자는 0 이고, 지금 배포는 운영할 수 없다**

| 항목 | 상태 |
|---|---|
| 컨트랙트 | SUPToken · SneakerNFT · RewardDistributor · CourseRegistry 가 GIWA Sepolia(91342)에 배포됨(2026-07-31). MysteryDrawNFT 는 미배포 |
| 키 | **배포 키를 보관하지 않음**(`deployments/giwaSepolia.json` `_keysRetained: false`). 소유자·어테스터·롤러·기록자 전부 같은 주소 `0xe96a…6FF7` → `setAttester`·`pause`·`setBaseURI` 등 **어떤 관리도 불가능** |
| 보상 풀 | 50,000,000 SUP, 지급 0 (`totalDistributed = 0`) — 서명할 키가 없어 영원히 못 꺼냄 |
| 소스 검증 | CourseRegistry 만 검증됨. 나머지 3개 미검증 |
| 어테스터 | 배포 안 됨. `wrangler.toml` 이 키 잃은 배포를 가리킴 |
| 앱 | 지갑 없음. `AttesterClient` 호출부 없음. 지갑 화면은 "현재 버전에서 지원하지 않음" |
| 뽑기 웹(`web/draw`) | MetaMask 연결·뽑기 흐름 완성. 설정(`draw-config.js`)이 비어 잠김 |
| GIWA | 메인넷 **개발 중**, GIWA Wallet **개발 중**(공식 문서). 테스트넷 RPC `https://sepolia-rpc.giwa.io/`, 탐색기 `https://sepolia-explorer.giwa.io` |

기존 결정(LAUNCH-PLAN §0·§9.5): **v1 은 체인 없음**, 온체인은 v2 에서 "v1 지표를 본 뒤" 결정.
Phase 4 로 가려면 이 순서를 당겨야 한다 — 아래는 그 경우의 설계다.

## 1. 원칙

1. **금액의 정본은 서버.** 체인에 나가는 SUP 는 서버(`record_session`)가 판정한 러닝에서만 나온다. 폰이 보낸 숫자로
   서명하지 않는다(결정표 X1·X3).
2. **키는 대표님이 보관.** 개인키·시드는 저장소·앱·로그·CI 로그·채팅에 절대 넣지 않는다(CLAUDE.md). 저는 코드와
   절차만 만든다. 서명 키는 Cloudflare Secrets 에만(LAUNCH-PLAN D4).
3. **체인 확인 전에는 "청구 완료"라고 쓰지 않는다.** 서명 받음 ≠ 청구 완료. `Claimed` 이벤트를 본 뒤에만 완료.
4. **테스트넷 SUP 는 가치가 없다고 명시.** 앱 안 포인트를 토큰으로 1:1 바꿔 준다는 약속을 하지 않는다
   (남아 있는 미사용 문구 `wallet_withdraw_*` "1:1 그대로 넘어가요"는 쓰지 않는다).
5. **감사 전에는 실제 가치 없음**(LAUNCH-PLAN D8). 메인넷은 감사 뒤 새로 배포.

## 2. 키를 보관한 채 테스트넷에 다시 배포하는 절차

### 2-1. 키 역할 (지금은 전부 한 주소 → 나눈다)

| 역할 | 하는 일 | 보관 | 비고 |
|---|---|---|---|
| 배포자(deployer) | 배포만. 끝나면 소유권을 넘기고 안 씀 | 대표님 PC, 배포 때만 | 가스용 테스트 ETH 필요 |
| 소유자(owner) | `setAttester`·`pause`·`setRoller`·`setBaseURI` | 대표님 하드웨어 지갑 또는 별도 지갑 | 메인넷은 멀티시그(D8) |
| 트레저리 | SUP 10억 개 수령, 보상 풀 충전 | 대표님 별도 지갑 | 메인넷은 멀티시그 |
| 어테스터(attester) | 러닝 보상 서명 | **Cloudflare Secret** 만 | 사람이 들고 다니지 않음 |
| 롤러(roller) | 신발 민트·뽑기 허가 서명 | Cloudflare Secret | 어테스터와 분리 |
| 기록자(recorder) | 코스 완주 기록 | Cloudflare Secret 또는 서버 크론 | 지금은 어테스터 주소를 같이 씀 → 분리 |

모든 키는 **복구 문구를 종이·오프라인에 백업**. 잃으면 지금처럼 배포가 영구히 굳는다.

### 2-2. 먼저 고칠 코드 (제가 할 일, 3단계)

- `deploy.js`
  - 결과 파일이 기존 `giwaSepolia.json` 을 덮지 않게 `giwaSepolia-v2.json` 으로.
  - MysteryDrawNFT 배포 추가(가격은 결정 항목).
  - 기록자 주소 분리(`RECORDER_ADDRESS`).
  - 끝에 `transferOwnership(OWNER_ADDRESS)` 추가 — 배포자에 소유권이 남지 않게.
  - 배포 전 요약 출력 + 확인 단계(드라이런) 추가.
- 신발 카탈로그 통일
  - SneakerNFT 는 44종(3/3/3/2), MysteryDrawNFT 는 52종(4/4/3/2), 앱·디자인은 52종(`design/equipment`).
  - 배포 전에 하나로 맞춰야 한다(결정 항목).
- NFT 메타데이터: 그림 52 + 의상 5 를 IPFS 에 올리고 CID 를 `SNEAKER_BASE_URI` 로.
  MysteryDrawNFT 는 `setBaseURI` 가 없어 **배포 때 한 번에 맞아야 함** → 함수 추가를 권장.
- 어테스터 체인 ID 하드코딩(91342) → 환경 변수로(메인넷 대비).
- 새 주소 반영: `attester/wrangler.toml`, `web/draw-config.js`, 앱 빌드 값(`ATTESTER_URL`·`DRAW_DAPP_URL`·
  `DRAW_CONTRACT_ADDRESS`)을 GitHub Secrets 에서 넣게 CI 연결, 문서.
- 문서 어긋남 정리: ARCHITECTURE §12·어테스터 README 의 "배포 키로 `setAttester`"(불가능),
  TOKENOMICS 의 `rate(d)`(코드엔 없음, 선착순).

### 2-3. 배포 당일 절차 (대표님이 직접, 제가 체크리스트 제공)

1. 대표님 PC 에서 키 6개 생성(MetaMask 계정 여러 개 또는 `cast wallet new`). **주소만** 저에게 알려 주심.
2. 배포자·소유자 주소에 GIWA Sepolia 테스트 ETH(파우싯).
3. 저장소 `contracts/` 에서 `.env` 는 **대표님 PC 에만**:
   `DEPLOYER_PRIVATE_KEY`, `OWNER_ADDRESS`, `TREASURY_ADDRESS`, `ATTESTER_ADDRESS`, `ROLLER_ADDRESS`,
   `RECORDER_ADDRESS`, `SNEAKER_BASE_URI`. (`.env` 는 `.gitignore` 에 있음 — 커밋 전 `git status` 로 확인.)
4. `npx hardhat run scripts/deploy.js --network hardhat` 로 드라이런 → 요약 확인.
5. `npx hardhat run scripts/deploy.js --network giwaSepolia` → `deployments/giwaSepolia-v2.json` 생성.
6. 소스 검증: `scripts/verify-blockscout.js` (4–5개 전부).
7. 트레저리 지갑에서 보상 풀 충전(`RewardDistributor.fund`) — 금액은 결정 항목.
8. Cloudflare: `wrangler secret put ATTESTER_PRIVATE_KEY` / `DRAW_ROLLER_PRIVATE_KEY` / `DRAW_SEED` → `wrangler deploy`.
9. 확인: 탐색기에서 `owner()`·`attester()`·`roller()` 가 계획한 주소인지.
10. 배포자 키는 PC 에서 지우고 오프라인 백업만 남김. `.env` 삭제.
11. 저에게 `giwaSepolia-v2.json` 커밋 요청 → 앱·웹 설정 연결.

**키 교체 절차(런북)**: 어테스터 키 유출 시 → 소유자 지갑으로 `pause()` → 새 키 `wrangler secret put` →
`setAttester(새 주소)` → `unpause()`. 이 절차를 배포 직후 한 번 연습한다.

## 3. 러닝 보상 청구 흐름

```
[폰] 러닝 끝 ─ 업로드 ─▶ [Supabase] record_session  → 판정 CLEAN, 포인트 확정 (지금 있음)
                                         │
[폰] "체인으로 받기" ─ 로그인 토큰 + 세션 id ─▶ [어테스터]
                                         │  ① 토큰으로 사용자 확인
                                         │  ② Supabase claim_reserve(세션 id) 호출(사용자 토큰으로)
                                         │     → 서버가 금액·지갑·하루 상한 계산, 세션을 RESERVED 로 잠금
                                         │  ③ EIP-712 서명(runner=연결 지갑, sessionHash=세션 id 기반)
                                         ▼
[지갑] RewardDistributor.claim(서명) ─▶ [GIWA] Claimed 이벤트
                                         │
[어테스터 크론/인덱서] Claimed 확인 ─▶ Supabase 세션 CONFIRMED + 앱 잔고에서 차감 줄
                                         ▼
[폰] "청구 완료" (여기서 처음)
```

### 3-1. 서버에 새로 필요한 것

- `wallet_links(user_id, address, linked_at)` — 지갑 연결. 지갑 서명(메시지: "StepUp 계정 <id> 연결, nonce")을
  **어테스터가 검증**해 기록. 한 지갑 = 한 계정(여러 계정이 한 지갑으로 몰아주기 방지).
- `chain_claims(session_id PK, user_id, wallet, amount, day, session_hash, deadline, status, tx_hash)` —
  status: RESERVED → SIGNED → CONFIRMED / EXPIRED.
- `claim_reserve(session_id)` RPC — 본인·CLEAN·7일 이내·아직 청구 안 함·지갑 연결됨·**러너별 하루 상한** 확인 후
  금액을 돌려주고 잠금. 금액 = min(그 세션 서버 포인트, 앱 안 남은 잔고).
- `sup_ledger` 에 `WITHDRAW_CHAIN` 종류 — CONFIRMED 때 음수 줄. 앱 안 잔고와 체인 이중 사용 방지.
  (LAUNCH-PLAN D2 의 "출금 가능 = min(앱 잔고, 청구 안 한 세션)" 을 그대로 구현.)
- 어테스터가 Supabase 에 쓸 때(지갑 연결·CONFIRMED)는 **그 일만 할 수 있는 전용 최소권한 키**. service_role 은 쓰지 않는다.

### 3-2. 서명 내용

- `runner` = 연결된 지갑, `amount` = 서버 계산, `day` = **러닝한 날**(지금은 서명한 날이라 7일 창이 무의미),
  `sessionHash = keccak(chainId, distributor, 세션 uuid)` — 같은 러닝은 해시가 하나 → 재서명해도 체인에서 한 번만 성공.
- `deadline` 은 짧게(10분). 만료되면 같은 해시로 다시 서명 가능(체인이 중복을 막음).

### 3-3. 가스

- 1차: **사용자가 직접 제출**(테스트넷 가스는 파우싯으로 무료). 사용자 트랜잭션 수로 잡혀 KPI 에도 유리.
- 2차: 릴레이어가 대신 제출(LAUNCH-PLAN D3) + 새 지갑에 소액 가스 지급. 메인넷 전에 결정.

## 4. 어테스터 강화 (결정표 X3 — 체인을 켜기 전 필수)

| 약점(지금) | 고침 |
|---|---|
| 누가 불렀는지 확인 안 함, CORS `*` | Supabase 로그인 토큰 검증(JWKS), CORS 는 `stepupcrew.com` 만 |
| 폰이 보낸 러닝 데이터로 서명 | **세션 id 만 받음**. 데이터·금액은 서버에서 |
| 시작 시각만 바꾸면 다른 해시 → 같은 트랙 무한 재서명 | 해시를 서버 세션 id 로 → 러닝당 하나 |
| 러너별 하루 상한 없음(가짜 요청 ~295번이면 하루 풀 25만 소진) | 서버 `claim_reserve` 에서 계정·지갑별 하루 상한 |
| 러닝 날짜 제한 없음 | 서버가 7일 이내만 허용 + `day` 를 러닝한 날로 |
| 부스트·파티 인원을 폰 값 그대로 | 서버 판정 금액만 사용(파티는 결정표 X2 로 서버 검증) |
| 요청 횟수 제한 없음 | Cloudflare Rate Limiting(사용자당 분당 N회) |
| 체인 91342 고정 | 환경 변수 |

선택: Play Integrity 검사(LAUNCH-PLAN D6) — 메인넷 전.

## 5. 지갑 연결

GIWA Wallet 은 아직 나오지 않았다. GASOK 선정 기준에 "지갑 내장"이 있으므로 **나오면 바로 갈아 끼울 수 있게** 설계한다.

| 방식 | 내용 | 공수 | 장단점 |
|---|---|---|---|
| **A. 웹 페이지 + MetaMask (임시, 권장)** | 앱이 `stepupcrew.com/claim?session=…` 을 브라우저로 염. 이미 있는 뽑기 웹(`web/draw`, viem + MetaMask Connect)과 같은 방식 | 소 (3–4일) | 앱에 지갑 코드 0 줄. 키 책임 없음. 앱→브라우저 이동이 불편 |
| B. 앱 안 WalletConnect | 앱에서 MetaMask·다른 지갑 앱을 바로 부름 | 중 (1–2주) | 경험 좋음. 라이브러리·세션 관리 필요 |
| C. 앱 내장 지갑(D1) | 키를 Android Keystore 에, 백업은 Drive | 대 (3주+) | 가장 매끄러움. **키 분실·유출 책임이 우리에게** — 감사 필요 |
| D. GIWA Wallet | 나오면 A/B 의 지갑 자리에 끼움 | 미정 | 심사 가점. 출시 시점 불명 |

권장: **A 로 시작 → GIWA Wallet 나오면 D**. 지갑 자리를 한 군데(웹의 연결 모듈)로 모아 두면 교체가 쉽다.
C 는 GIWA Wallet 이 Phase 4 안에 안 나올 때만.

## 6. 트랜잭션 양 · TVL 기능 후보 (우선순위)

| 순위 | 기능 | 만드는 것 | 컨트랙트 | 공수 | 비고 |
|---|---|---|---|---|---|
| 1 | **러닝 보상 청구** | 사용자 tx (활성 러너 × 러닝 수) · 청구 지갑 수 | 있음(RewardDistributor) | 중 | §3·§4. 모든 것의 바탕 — 체인 SUP 가 있어야 2–4 가능 |
| 2 | **미스터리 뽑기** | 사용자 tx 2개(approve + draw) | 있음(MysteryDrawNFT, 미배포) | 소 | 웹 완성. 배포 + 가격 결정 + 앱 버튼 연결만 |
| 3 | **신발 민트·강화** | 사용자 tx · SUP 소각 | 있음(SneakerNFT) | 중 | 카탈로그 통일, `/mint-auth` 필요. 앱 신발과 체인 신발 관계 결정 |
| 4 | **코스 등록·완주 기록** | 코스 등록(사용자 tx) · 완주 기록(서버 tx) | 있음(CourseRegistry) | 소–중 | 완주 기록은 서버 tx 라 "사용자 수"엔 안 잡힘. 보상 지급 경로 없음 |
| 5 | **SUP 스테이킹 → 부스트** | **TVL** · 예치/인출 tx | 새로 | 중 (+감사) | 러닝 앱에 자연스러움. 잠근 동안 보상 부스트 |
| 6 | **주간 챌린지 예치** | TVL · tx | 새로 | 중 (+법률 검토) | "목표 달성하면 돌려받음". 사행성으로 보일 수 있음 |
| 7 | **크루 금고** | TVL · tx | 새로 | 중 | 크루가 SUP 모아 이벤트 보상 |
| 8 | **신발 거래소 온체인화** | tx · 에스크로 TVL | 새로 | 대 (+감사) | 서버 거래소(0007) 설계를 옮김. 로열티 5% 는 이미 있음 |

**TVL 주의**: 지금 TVL 이 될 수 있는 건 우리 토큰 SUP 뿐이다. 유동성(거래 가능한 시장)이 없으면 SUP 의 가격이
정해지지 않아 **TVL 을 금액으로 말할 근거가 없다**(TOKENOMICS §9 유동성 계획 미정). 트레저리가 채운 보상 풀은
사용자 예치가 아니라 TVL 로 내세우면 안 된다. 심사에서 TVL 을 보여 주려면 (a) SUP 유동성 계획 또는
(b) 스테이블코인 등 외부 자산 예치 기능이 필요 — 결정 항목.

## 7. 순서와 공수 (3단계, 결정 뒤)

| 단계 | 내용 | 공수 | 누가 |
|---|---|---|---|
| 7-1 | 코드: deploy.js·카탈로그·체인 설정·어테스터 강화·서버 표/RPC + 검사 | 1–1.5주 | 저 |
| 7-2 | 테스트넷 재배포(§2-3) | 반나절 | 대표님(키) + 저(체크리스트) |
| 7-3 | 청구 웹 페이지(A) + 앱 "체인으로 받기" 버튼 | 3–4일 | 저 (앱 부분은 Codex 작업 뒤) |
| 7-4 | 뽑기 연결(배포 + 앱 빌드 값) | 1일 | 저 |
| 7-5 | 인덱서(Claimed·Drawn 등 이벤트 → 지표) | 1–2일 | 저 — `docs/초기-사용자-지표-설계.md` §3-5 |
| 7-6 | 신발 민트, 스테이킹 등 §6 3–8 | 기능별 | 결정에 따라 |

## 8. 대표님이 정해야 할 것

1. **온체인을 v2 로 미루던 결정(LAUNCH-PLAN §0)을 당겨 Phase 4 용으로 진행**할까요?
2. **키 보관**: 소유자·트레저리를 무엇으로 둘까요? (하드웨어 지갑 / MetaMask 별도 계정 / 멀티시그 — GIWA 에서
   Safe 사용 가능 여부 확인 필요). 배포 날짜.
3. **신발 카탈로그**: 체인 신발을 앱과 같은 **52종**으로 맞출까요? 아니면 SneakerNFT 를 빼고 MysteryDrawNFT 하나만 쓸까요?
4. **앱 신발 ↔ 체인 신발 관계**: 앱 신발을 체인으로 "꺼내는" 방식인지, 체인 신발은 뽑기로만 얻는 별도 수집품인지.
5. **보상 풀 충전량**(테스트넷). 기존 설계는 5,000만 SUP.
6. **뽑기 가격**: 신발 / 의상 (테스트 값 500 / 300 SUP).
7. **지갑**: 임시로 웹 + MetaMask(A) 로 시작해도 될까요?
8. **가스**: 처음엔 사용자가 직접 내기(테스트넷 무료) → 메인넷 전에 릴레이어. 이대로 될까요?
9. **TVL**: SUP 스테이킹(5)으로 갈지, 외부 자산 예치까지 고려할지, 유동성 계획을 세울지.
10. **청구 금액 기준**을 서버 원장으로(결정표 X1) — 앱 안 부스트·에너지 상한을 서버에도 넣을지.
11. **법률**: 토큰 청구·예치 기능을 한국 사용자에게 여는 것에 대한 검토 여부(LAUNCH-PLAN §8 미정).
