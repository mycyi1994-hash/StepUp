# App Store 출시 준비

[PLAY-STORE.md](PLAY-STORE.md)의 iOS 판이다. App Store Connect 에 앱을 등록할 때 채우는 칸을 이 앱의 실제 동작에 맞춰 적어 둔다.
앱이 바뀌면(특히 모으는 정보가 바뀌면) 이 문서와 PLAY-STORE.md · `web/privacy.html` · `web/terms.html` 을 함께 고친다.

- 2026-11-02 사용자 결정: **iOS 앱은 Kotlin Multiplatform(KMP)으로 만든다** — 규칙 · 화면을 안드로이드와 함께 쓰고, 위치 · 걸음 ·
  백그라운드 러닝 · 잠금 화면 · 로그인 · 푸시만 iOS 로 따로 짠다. 만드는 순서 · 코드 구조는 [ios/README.md](ios/README.md).
- 지금 상태: 뼈대뿐이다(로고 화면 하나 · 공용 규칙 두 개 — `RunPace` · `RunStill`). **아직 제출할 앱이 없다.**

## 출시 전에 사용자가 정할 것 (코드로 못 정함)

| 할 일 | 왜 |
|---|---|
| **번들 ID 확정** | App Store Connect 에 처음 올린 뒤로는 바꿀 수 없다. 임시 값은 `com.stepupcrew.stepup`(`iosApp/Configuration/Config.xcconfig`) |
| ~~개인 · 조직 계정~~ **정함(2026-11-02): 조직(법인)** | 사용자 "우린 법인". Apple Developer Program 에 회사/기관으로 신청했다 — 애플이 법적 계약 서명 권한을 확인하면 메일로 남은 절차(회비 결제)가 온다. 스토어 판매자 이름은 D-U-N-S 에 오른 법인 이름 |
| **iOS 판에 지갑 주소 연결을 넣을지** | 안드로이드 스토어 판에는 있다(2026-10-29). 셀프 커스터디 지갑을 서명으로 확인만 해서 지갑 앱은 아니지만, 심사원이 3.1.5(암호화폐)로 물을 수 있다. 빼고 먼저 내는 쪽이 안전하다 |
| **블록체인 공개 기록을 iOS 사용자에게도 기본으로 할지** | 서버(0078)가 모든 사용자의 활동 기록(가명)을 공개 체인에 올린다. 애플 5.1.2 는 개인 데이터를 제3자와 나누기 전에 허락을 받으라고 한다 — 가입 때 고지 · 동의 한 줄을 넣거나, iOS 는 켠 사람만 올리는 쪽 중 하나 |
| 베타를 어디서 돌릴지 | 접속 코드로 막힌 앱(`access_mode` CLOSED · OPEN_BETA)은 "일부만 쓸 수 있는 앱은 TestFlight 로"라며 거절될 수 있다. 베타는 **TestFlight**(외부 테스터 1만 명, 공개 링크)로, 스토어 제출은 `OPEN` 때 하는 쪽을 권한다 |
| SUP · 거래소 법률 검토 · 위치기반서비스사업 신고 | 안드로이드와 같은 일이다(PLAY-STORE.md) — 애플 때문에 새로 생기지 않았다 |

## 0. 먼저 해 둘 것

| 할 일 | 어디서 | 비고 |
|---|---|---|
| Apple Developer Program | developer.apple.com/programs | 해마다 내는 회비(한국 129,000원 · US$99). 본인 확인에 하루 ~ 며칠 |
| Mac · 테스트용 아이폰 | — | iOS 빌드는 macOS 에서만 된다(CI 는 GitHub 의 macOS 러너 — `.github/workflows/ios.yml`). GPS · 화면 꺼진 러닝 · 잠금 화면은 실제 아이폰으로 밖에서 뛰어 봐야 한다 |
| Team ID 알려 주기 | Apple Developer › Membership details | 10자리. `Config.xcconfig` 의 `TEAM_ID` 와 초대 링크용 `web/.well-known/apple-app-site-association` 에 들어간다 |
| App ID 등록 | Certificates, Identifiers & Profiles › Identifiers | 위 번들 ID 로. 기능: **Sign in with Apple** · **Push Notifications** · **Associated Domains** |
| **Apple 로그인** 키 | Identifiers › Services ID 하나 + Keys › Sign in with Apple 키(.p8) | Supabase › Authentication › Providers › Apple 에 Services ID · Team ID · Key ID · .p8 를 넣는다. 같은 .p8 는 계정 삭제 때 Apple 토큰을 끊는 데도 쓴다(서버 비밀 — 앱에 넣지 않는다) |
| 푸시 키(APNs .p8) | Keys › Apple Push Notifications service | Firebase 콘솔 › 프로젝트 설정 › iOS 앱 추가(`GoogleService-Info.plist`) › Cloud Messaging 에 올린다. 서버의 `push-send`(FCM)는 그대로 쓴다 |
| 구글 로그인 iOS 클라이언트 | Google Cloud › API 및 서비스 › 사용자 인증 정보 › OAuth 클라이언트 ID(iOS) | 번들 ID 로 만든다. Supabase › Providers › Google › **Authorized Client IDs** 에 iOS 클라이언트 ID 를 더한다(안 하면 iOS 의 id_token 을 거절한다) |
| MapTiler 키 | MapTiler › Account › Keys | 키에 앱 제한을 걸어 두었으면 iOS 번들 ID 를 더한다 |
| App Store Connect 앱 만들기 | appstoreconnect.apple.com › 앱 › + | 이름 · 기본 언어(한국어) · 번들 ID · SKU(예: `stepup-ios`) |
| 연락 · 지원 주소 | — | `stepupofficial@stepupcrew.com` · https://stepupcrew.com (PLAY-STORE.md 와 같다) |

## 1. 앱 정보

- 이름(30자): **StepUp — 러닝 크루**
- 부제(30자): 예) "크루와 함께 달리는 러닝 기록"
- 기본 언어: 한국어 (영어 · 일본어 · 중국어 간체 추가 — 앱 언어 넷과 같다)
- 카테고리: **건강 및 피트니스** (보조: 소셜 네트워킹)
- 가격: 무료 · 판매 지역: 대한민국부터
  - EU 에 내려면 디지털 서비스법(DSA) 거래자 정보를 먼저 내야 한다 — 한국부터면 "EU 에 배포하지 않음"
- 개인정보처리방침: **https://stepupcrew.com/privacy** · 지원 URL: https://stepupcrew.com
- 콘텐츠 권리: "제3자 콘텐츠 있음, 권리 있음" — 지도(MapTiler · OpenStreetMap, 앱 지도 아래에 출처 표기) · 날씨(Open-Meteo)

## 2. 스토어 등록정보

- 설명 · 짧은 문구는 PLAY-STORE.md 2절을 그대로 옮긴다(블록체인 · 수익 · 투자 가치는 내세우지 않는다).
  - 프로모션 텍스트(170자, 심사 없이 바꿀 수 있다): PLAY-STORE.md 의 짧은 설명
  - 키워드(100자, 쉼표로): 러닝,달리기,크루,러닝크루,번개러닝,기록,GPS,마라톤,조깅,운동
- 스크린샷: **아이폰 6.9인치**(1320 × 2868 또는 1290 × 2796) 3 ~ 10장. 아이폰만 지원하므로(`TARGETED_DEVICE_FAMILY = 1`) 아이패드 스크린샷은 없다.
  실제 iOS 앱 화면이어야 한다 — 안드로이드 캡처 · 데모 화면을 쓰지 않는다.
- 앱 아이콘: 1024 × 1024, 투명 없음 — `iosApp/StepUp/Assets.xcassets/AppIcon.appiconset/AppIcon-1024.png`
  (`design/brand/stepup-app-icon.webp` 의 투명 모서리를 같은 네이비로 채운 것. 둥근 모서리는 iOS 가 깎는다)

## 3. 앱 개인정보 보호 (App Privacy — Play 데이터 보안에 해당)

PLAY-STORE.md 의 표를 애플 분류로 옮긴 것이다. 애플 표에는 "공유" 칸이 없다 — 앱 밖으로 보내는 것은 모두 "수집"이다.

- **추적(Tracking): 아니요** — 광고가 없고, 다른 회사 데이터와 엮지 않는다. 앱 추적 투명성(ATT) 창은 띄우지 않는다.
  Firebase Analytics 는 광고 ID 없이 넣는다(`FirebaseAnalyticsWithoutAdIdSupport` 또는 AdSupport 를 링크하지 않기).

| 애플 분류 | 항목 | 목적 | 사용자와 연결 |
|---|---|---|---|
| 위치 | 정확한 위치 | 앱 기능 | 예 |
| 연락처 정보 | 이름, 이메일 주소 | 앱 기능 | 예 |
| 건강 및 피트니스 | 피트니스(걸음 · 러닝) — 공개 체인 기록 포함 | 앱 기능 | 예 |
| 사용자 콘텐츠 | 사진(크루 대표 이미지 · 채팅 사진), 기타 사용자 콘텐츠(글 · 댓글 · 채팅 · 코스 · 장소 이야기) | 앱 기능 | 예 |
| 식별자 | 사용자 ID, 기기 ID(푸시 토큰 · Firebase 인스턴스 ID) | 앱 기능, 분석 | 예 |
| 사용 데이터 | 제품 상호작용(뽑기 · 강화 · 거래 · 출석 기록 포함) | 분석, 앱 기능 | 예 |
| 금융 정보 | 기타 금융 정보 — **지갑 주소 연결을 iOS 에 넣을 때만** | 앱 기능, 사기 방지 | 예 |

수집하지 않음: 연락처, 브라우징 · 검색 기록, 오디오, 진단(충돌 기록 — Crashlytics 를 넣지 않았다), 심박 등 건강 측정값, 광고 데이터.
AI 코칭(`coaching_enabled=false`)을 켜기 전에 이 표를 먼저 고친다(PLAY-STORE.md 와 같다).

## 4. 연령 등급

새 연령 등급 설문(2025 개편 — 4+ · 9+ · 13+ · 16+ · 18+)에 답한다. 답은 PLAY-STORE.md 의 콘텐츠 등급과 같게:

- 사용자 생성 콘텐츠 · 메시지: **예**(글 · 댓글 · 크루 채팅 — 신고 · 차단 · 신고 5건이면 숨김)
- 위치 공유: **예**(번개 모임 장소, 함께 뛰기 방 안)
- 무작위 아이템(뽑기): **예** — 모두 무료, 확률은 앱 안(뽑기 › 기회 받는 방법 시트)과 이용약관 3-2
- 도박 · 현금 상품: 아니요(SUP 는 현금으로 바꿀 수 없는 앱 포인트)
- 광고: 아니요 · 의료 정보: 아니요

## 5. 심사 지침 대조 — 애플이 구글보다 까다로운 곳

| 지침 | 우리 앱 | 할 일 |
|---|---|---|
| **4.8 로그인 서비스** | 구글 로그인만 있다 | **Apple 로그인**을 같은 자리에 넣는다(이름 · 이메일만, 이메일 숨기기 가능). 안드로이드에도 넣을지는 따로 정한다 |
| **5.1.1(v) 계정 삭제** | 앱 안 + 웹에 있다 | Apple 로그인 계정은 지울 때 Apple 토큰도 끊는다(Sign in with Apple REST API `revoke`) — 서버 일 |
| 3.1.1 앱 내 구입 · 뽑기 | 유료 결제 · 유료 뽑기 없음, 확률 표 앱 안에 있음 | 유료로 파는 것이 생기면 애플 인앱 결제만 쓴다 |
| 3.1.5 암호화폐 | iOS 는 스토어 판처럼 체인 기능을 뺀다(`WEB3 = false` 와 같게) | 지갑 주소 연결은 위 결정 |
| 5.1.2 데이터 사용 · 공유 | 활동 기록(가명)을 공개 체인에 올린다 | 위 결정 — 고지 · 동의 |
| 1.2 사용자 생성 콘텐츠 | 신고 · 차단 · 숨김 · 연락처 있음 | 그대로 |
| 2.1 완성도 · 심사용 접근 | 접속 코드 문 | 심사 메모에 심사용 코드(아래 7절). 베타는 TestFlight |
| 2.5.4 백그라운드 · 5.1.1 권한 | 러닝 중에만 위치를 쓴다 | "사용하는 동안" 권한 + 백그라운드 위치 모드(러닝 중 파란 표시). "항상 허용"은 묻지 않는다. 권한 문구는 쓰임을 구체적으로(`Info.plist`) |
| 2.3 메타데이터 | — | 스크린샷 · 설명이 실제 iOS 앱과 같아야 한다 |

## 6. 수출 규정 (암호화)

HTTPS 만 쓴다 → `ITSAppUsesNonExemptEncryption = NO`(`iosApp/StepUp/Info.plist` 에 넣어 두었다 — 올릴 때마다 묻지 않는다).

## 7. 심사 정보 (App Review)

- 연락처: 이름 · 전화 · `stepupofficial@stepupcrew.com`
- 로그인: 심사원은 Apple 로그인으로 직접 들어올 수 있다. 접속 코드 문이 열려 있으면(`access_mode` 가 OPEN 이 아님) 심사용 코드를 적는다:
  `admin_access_code_named('APPREVIEW2026', 20, 60, 'App Store 심사')`([access-codes-2026-10-28.md](access-codes-2026-10-28.md))
- 메모에 적을 것: 러닝은 밖에서 걸어야 거리가 쌓인다 · 뽑기는 모두 무료이고 확률은 어디에 · SUP 는 현금 가치 없는 포인트 ·
  위치는 러닝 중에만(백그라운드 위치 모드의 까닭)
- 심사원이 영상으로 보여 달라고 하면: 러닝 시작 → 화면 잠금 → 잠금 화면 현황(Live Activity) → 종료

## 8. 올리기

1. 버전 · 빌드 번호(`Config.xcconfig` 의 `MARKETING_VERSION` · `CURRENT_PROJECT_VERSION`)를 올린다
2. **Xcode 26 · iOS 26 SDK 로 빌드한다** — 2026-04 부터 App Store Connect 가 요구한다(ios/README.md "Kotlin 올리기")
3. Archive → App Store Connect 에 올리기(나중에 CI 로 — 서명 인증서 · 프로비저닝을 GitHub Secrets 에)
4. **TestFlight** 내부 테스터 → 외부 테스터(첫 외부 빌드는 짧은 베타 심사)
5. 앱 심사 제출 → 국가: 대한민국부터

Play 처럼 "테스터 12명 · 14일" 요건은 없다.
