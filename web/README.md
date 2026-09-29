# web — stepupcrew.com

Cloudflare Pages 가 이 폴더를 그대로 올린다(빌드 없음). main 에 합치면 사이트가 바뀐다.

| 경로 | 내용 |
|---|---|
| `/` | 소개 페이지(`index.html`, `styles.css`, `site.js`) — 홍보영상 `#film`을 먼저 보여주고 러닝 `#run` · 크루 `#crew` · 보상 `#reward` · 코스 `#course` · 시작 `#start` 순서로 이어진다. 휠·키보드·스와이프로 넘긴다. 영상은 `assets/video/`의 1080p/720p 파일을 화면 폭에 맞게 선택하며 무음 자동 재생·네이티브 컨트롤·화면 이탈 시 일시정지를 지원한다. 그림은 `assets/img/`, 공유 미리보기는 `assets/og-image.jpg`. 예시 데이터에는 화면 예시 배지를 둔다. 앱 다운로드는 출시 준비 중으로 표시한다. 머리 줄 오른쪽에는 공식 X(`x.com/GiwaStepUp`) · 텔레그램(`t.me/StepUpOfficialTG`) 로고 링크(새 창)가 소리 · 출시 준비 중 왼쪽에 선다 — 폰(≤480px)에서는 머리 줄을 줄여 한 줄로, 340px 이하에서는 누를 수 없는 "출시 준비 중"만 뺀다. |
| `/c/<크루 id>` | 크루 초대 링크. 앱이 있으면 앱이 바로 열리고, 없으면 `invite.html`에서 앱 열기와 출시 준비 상태를 안내한다(`_redirects`). 다운로드 버튼은 비활성화되어 있다. |
| `/.well-known/assetlinks.json` | 안드로이드 App Links 확인 파일. 앱 서명의 SHA-256 이 들어 있다 |
| `/privacy.html`, `/terms.html` | 개인정보처리방침 · 이용약관 |
| `/wallet.html` | 지갑 — 2단계 인증 · 지갑 연결 · 보너스 뽑기 · SUP/신발 꺼내기(가스비 대납) · 앱으로 넣기. 앱에서 여는 길(`…/wallet.html#t=<로그인 토큰>` — # 뒤는 서버로 가지 않고, 페이지가 읽자마자 주소창에서 지운다)은 앱의 내 정보 › 지갑 카드의 "지갑 페이지 열기" 버튼이다. 앱 없이 열면 구글 로그인으로 들어온다. 설정은 `wallet-config.js`, 빌드는 `npm run build:wallet`. |
| `/draw.html` | (v1, 쓰지 않음) 옛 미스터리 박스 거래 화면. 뽑기는 앱 안(서버)과 지갑 페이지의 보너스 뽑기로 옮겼다. 설정이 비어 잠긴 상태다. |

## 홈페이지 화면과 출시 준비 상태

- 영상 위 가로 배너에서 이메일 대기 명단을 신청할 수 있다. 확인 메일 없이 Supabase의 공개 RPC가 바로 저장한다. 등록 후 X·인스타그램·스레드 게시물 주소를 플랫폼별로 남길 수 있다. 보너스 지급은 아직 연결하지 않았다.

- 화면 폭이 900px보다 크면 오른쪽 장면 탐색 영역을 카드 밖에 따로 확보한다. 막대 오른쪽에 현재 장면·호버·키보드 포커스의 이름을 표시하고, 각 버튼의 클릭 영역은 최소 44px 높이다. 900px 이하에서는 이 영역을 숨겨 콘텐츠 폭을 유지하며 휠·키보드·스와이프 이동을 계속 지원한다. 380px 이하에서는 헤더 간격과 크기를 줄인다.
- 홈페이지 5개와 초대 페이지 1개의 기존 다운로드 링크는 비활성화된 출시 준비 버튼이다. Android와 iPhone을 모두 준비 중으로 안내한다. 공개 웹 HTML에는 APK 다운로드 링크가 없다. 기존 테스트 링크를 위해 `downloads/StepUp-MVP.apk`의 `_redirects` 연결은 유지하며, GitHub의 최신 `test-apk`로 보낸다.
- 변경 내용과 브라우저 검증 범위는 [홈페이지 보고서](../docs/web/HOMEPAGE-VIDEO-2026-09-28.md)에 기록한다.

뽑기 웹 화면은 `npm ci` 후 `npm run build:draw`로 `assets/draw.js`를 만든다. 새 계약 배포 후 `draw-config.js`에 공개 계약 주소, SUP 주소, 추첨 서명 Worker의 HTTPS 주소를 넣고, Android 빌드에 `DRAW_DAPP_URL`과 `DRAW_CONTRACT_ADDRESS`를 설정한다. 이 세 설정과 실제 체인 거래 검증 전에는 앱 버튼을 활성화하지 않는다. 연결은 MetaMask Connect가 모바일 지갑까지 처리하며, 결과는 서명 응답이 아닌 GIWA 거래 영수증의 `Drawn` 이벤트를 읽어 표시한다.

## 서명이 바뀌면

`assetlinks.json` 의 `sha256_cert_fingerprints` 는 지금 테스트 APK 의 서명(`app/debug.keystore`)이다.
Play 스토어에 올리면 **Play 앱 서명 키**의 SHA-256 을 더해야 한다
(Play Console › 앱 무결성 › 앱 서명 키 인증서). 빼면 스토어 앱에서는 링크가 브라우저로 열린다.
