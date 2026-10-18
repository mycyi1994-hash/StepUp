# WEB3 지갑 연결 — 지갑을 고르면 바로 (2026-10-18)

사용자: "그냥 누르면 바로 web3 지갑 연결 이런 거 뜨면 안 됨? 왜 이렇게까지 하는 건데" →
"WalletConnect 빼고 OKX WALLET · METAMASK · RABBY WALLET · GIWA WALLET(맨 위, 준비 중) 4개".

## 예전

앱 → 외부 브라우저로 웹 지갑 페이지(로그인 토큰을 `#` 에 실어) → 2단계 인증(인증 앱 QR 등록 · 6자리, 15분마다 다시) →
MetaMask 연결 · 네트워크 바꾸기 → 서명 → 연결.

## 지금

1. 앱의 "WEB3 지갑 연결"(뽑기 탭 상급 뽑기 · 내 정보 › 지갑 · 설정 › 연결된 계정) → 지갑 고르기 창(`WalletConnectSheet`).
   - **GIWA Wallet** — 맨 위, "준비 중"(누를 수 없다)
   - **MetaMask** — `https://link.metamask.io/dapp/{연결 페이지}` 로 MetaMask 앱 안 브라우저가 연다(MetaMask 공식 링크)
   - **OKX Wallet** — `okx://wallet/dapp/url?dappUrl={연결 페이지(인코딩)}`
   - **Rabby Wallet** — 앱 브라우저로 바로 여는 공개 링크가 없어 연결 페이지 주소를 **복사**하고 Rabby 앱을 연다(브라우저 주소창에 붙여 넣기)
   - 앱이 없으면 플레이 스토어로.
2. 앱은 고를 때 서버에서 **10분짜리 일회용 확인 번호**를 받는다(`wallet_link_challenge`). 연결 페이지는 `wallet.html?l=계정.확인번호` —
   지갑 앱에 **로그인 토큰을 넘기지 않는다**. 페이지는 읽자마자 주소창에서 지운다.
3. 지갑 앱 브라우저의 페이지에서 "지갑 연결하기" → 지갑이 문장에 서명(거래 아님, 수수료 없음, 네트워크 안 바꿈) →
   어테스터 `POST /v2/wallet/link-request` 가 "그 계정 + 그 번호" 문장의 서명인지 보고 서버 `attester_wallet_link` 를 부른다
   (번호가 그 계정에 10분 안에 준 것인지 · 한 번만).
4. 앱으로 돌아오면 서버에 연결 여부를 다시 묻고(`draw_status.wallet_linked`), 붙은 것을 본 뒤에만 "지갑을 연결했어요".

## 보안 — 바꾼 것 · 그대로인 것

- **처음 붙이는 지갑은 2단계 인증을 묻지 않는다**(서버 0067). 돈이 나가는 꺼내기는 그대로 2단계 인증 · 연결(바꾼) 뒤 72시간 대기 · 보안 알림.
- **이미 붙은 지갑을 다른 지갑으로 바꾸는 것**은 예전처럼 방금 한 2단계 인증이 있어야 한다 — 웹 지갑 페이지에서.
  고르기 창에서 시도하면 "지갑 페이지에서 2단계 인증" 안내.
- 지갑 1개 = 계정 1개, 첫 연결 보너스 상급 뽑기 10회(Genesis 1), 테스트넷 가스비 조금 — 그대로.
- 남는 위험: 연결 페이지 주소(계정 + 확인 번호)가 10분 안에 남의 손에 들어가면 그 사람 지갑을 내 계정에 붙일 수 있다.
  꺼내기는 2단계 인증 · 72시간 대기가 있고 연결되면 보안 알림이 간다. 주소는 지갑 앱에만 넘기고 앱 기록에 남기지 않는다.

## 확인 못 한 것

- 실제 기기에서 MetaMask · OKX · Rabby 앱으로 열어 보지 못했다. OKX 링크 형식은 공개 문서가 아니라 널리 쓰이는 형식이다.
  Rabby 안드로이드 패키지 이름(`com.debank.rabbymobile`)도 기기에서 확인이 필요하다.
- 어테스터(Cloudflare Worker)는 main 에 합쳐지면 배포된다(deploy-attester). 배포 전에는 새 연결 길이 404 다.

## 코드

- 서버 `supabase/migrations/0067_wallet_link_simple.sql`
- 어테스터 `attester/src/handlers.js` `linkWalletRequest` · `index.js` `/v2/wallet/link-request`
- 웹 `web/wallet-src.js` `renderLinkRequest` · `wallet-core.js` `linkRequestFromSearch` · `walletLinkMessage`
- 앱 `domain/WalletApps.kt` · `ui/screens/rewards/WalletConnectSheet.kt` · `EconomyApi.walletLinkChallenge` · 매니페스트 `<queries>`
- 검사 `schema_test.sql`(처음 연결 · 바꾸기 2단계 인증) · `attester/test/handlers.test.js` · `web/test/wallet-core.test.js` ·
  `WalletAppsTest` · `WalletDesignTest`(고르기 창 순서 · GIWA 준비 중)
