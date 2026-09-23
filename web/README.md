# web — stepupcrew.com

Cloudflare Pages 가 이 폴더를 그대로 올린다(빌드 없음). main 에 합치면 사이트가 바뀐다.

| 경로 | 내용 |
|---|---|
| `/` | 소개 페이지(`index.html`, `styles.css`, `site.js`) — 오프라인 MVP 체험판 안내와 APK(`downloads/StepUp-MVP.apk`) |
| `/c/<크루 id>` | 크루 초대 링크. 앱이 있으면 앱이 바로 열리고, 없으면 `invite.html` 이 설치를 안내한다(`_redirects`) |
| `/.well-known/assetlinks.json` | 안드로이드 App Links 확인 파일. 앱 서명의 SHA-256 이 들어 있다 |
| `/privacy.html`, `/terms.html` | 개인정보처리방침 · 이용약관 |

## 서명이 바뀌면

`assetlinks.json` 의 `sha256_cert_fingerprints` 는 지금 테스트 APK 의 서명(`app/debug.keystore`)이다.
Play 스토어에 올리면 **Play 앱 서명 키**의 SHA-256 을 더해야 한다
(Play Console › 앱 무결성 › 앱 서명 키 인증서). 빼면 스토어 앱에서는 링크가 브라우저로 열린다.
