# web — stepupcrew.com

Cloudflare Pages 가 이 폴더를 그대로 올린다(빌드 없음). main 에 합치면 사이트가 바뀐다.

| 경로 | 내용 |
|---|---|
| `/` | 소개 페이지(`index.html`, `styles.css`, `site.js`) — 오프라인 MVP 체험판 안내와 APK(`downloads/StepUp-MVP.apk`) |
| `/c/<크루 id>` | 크루 초대 링크. 앱이 있으면 앱이 바로 열리고, 없으면 `invite.html` 이 설치를 안내한다(`_redirects`) |
| `/.well-known/assetlinks.json` | 안드로이드 App Links 확인 파일. 앱 서명의 SHA-256 이 들어 있다 |
| `/privacy.html`, `/terms.html` | 개인정보처리방침 · 이용약관 |
| `/wallet.html` | 지갑 — 2단계 인증 · 지갑 연결 · 보너스 뽑기 · SUP/신발 꺼내기(가스비 대납) · 앱으로 넣기. 앱은 `…/wallet.html#t=<로그인 토큰>` 으로 연다(# 뒤는 서버로 가지 않고, 페이지가 읽자마자 주소창에서 지운다). 설정은 `wallet-config.js`, 빌드는 `npm run build:wallet`. |
| `/draw.html` | (v1, 쓰지 않음) 옛 미스터리 박스 거래 화면. 뽑기는 앱 안(서버)과 지갑 페이지의 보너스 뽑기로 옮겼다. 설정이 비어 잠긴 상태다. |

뽑기 웹 화면은 `npm ci` 후 `npm run build:draw`로 `assets/draw.js`를 만든다. 새 계약 배포 후 `draw-config.js`에 공개 계약 주소, SUP 주소, 추첨 서명 Worker의 HTTPS 주소를 넣고, Android 빌드에 `DRAW_DAPP_URL`과 `DRAW_CONTRACT_ADDRESS`를 설정한다. 이 세 설정과 실제 체인 거래 검증 전에는 앱 버튼을 활성화하지 않는다. 연결은 MetaMask Connect가 모바일 지갑까지 처리하며, 결과는 서명 응답이 아닌 GIWA 거래 영수증의 `Drawn` 이벤트를 읽어 표시한다.

## 서명이 바뀌면

`assetlinks.json` 의 `sha256_cert_fingerprints` 는 지금 테스트 APK 의 서명(`app/debug.keystore`)이다.
Play 스토어에 올리면 **Play 앱 서명 키**의 SHA-256 을 더해야 한다
(Play Console › 앱 무결성 › 앱 서명 키 인증서). 빼면 스토어 앱에서는 링크가 브라우저로 열린다.
