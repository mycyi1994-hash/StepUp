# iOS 앱 — Kotlin Multiplatform (2026-11-02 ~)

사용자 결정 "KMP로 가자"(2026-11-02). 안드로이드 앱의 규칙 · 화면을 iOS 와 함께 쓰고, 기기마다 다른 일만 따로 짠다.

**브랜치**(2026-11-02 사용자 "아이폰 어플용 브랜치를 따로 만들어서 분리"): iOS 작업은 `ios-app` 에 모은다 — 단계마다 작업 브랜치에서
`ios-app` 으로 PR 을 낸다. `main`(안드로이드 스토어 판)에는 안드로이드가 테스트 APK 로 확인된 뒤에만 합친다. `main` 이 바뀌면 `ios-app` 에 합쳐 둔다.
스토어에 낼 때 채우는 칸 · 사용자가 준비할 것은 [../APP-STORE.md](../APP-STORE.md).

## 구조

| 자리 | 무엇 | 대상 |
|---|---|---|
| `shared/` | 공용 규칙 — 순수 Kotlin(안드로이드 · iOS API 없음). 패키지는 예전 그대로 `com.stepup.android.domain` 이라 안드로이드 코드의 import 가 바뀌지 않는다 | jvm(안드로이드 앱이 쓴다) · iosArm64 · iosSimulatorArm64 |
| `sharedUi/` | iOS 앱 화면(Compose Multiplatform). Xcode 가 붙이는 틀 **StepUpKit**(정적, `:shared` 를 함께 내보낸다) | 지금은 iOS 만 — 안드로이드 화면을 옮길 때 `androidTarget()` 을 더한다 |
| `iosApp/` | Xcode 앱 — SwiftUI 입구(`StepUpApp.swift`)가 Kotlin 화면(`MainViewController`)을 띄운다. 프로젝트는 `project.yml`(XcodeGen)에서 만들고 저장소에 넣지 않는다 | iOS 16+, 아이폰만 |
| `app/` | 안드로이드 앱 — 그대로. `implementation(project(":shared"))` 로 공용 규칙을 받는다 | Android |

지금 공용으로 옮긴 것: **`domain/` 전부 · `domain/coaching/` 전부**(2026-11-02 1차 24개 → 2026-11-03 2차 나머지 — 크루 · 순위 · 기록 · 이야기 · 커뮤니티 · 뽑기 · 강화 · 코칭 …).
앱에 남은 규칙은 화면 글자 `formatKm`(기기 언어를 따르는 `%.2f`, `RunCourseFormat.kt`) 하나다.
- JVM 전용 호출은 같은 값을 내는 공용 함수로(`Platform.kt` — 안드로이드는 원래 JVM 함수를 그대로 부르고, iOS 는 같은 값을 내는 Kotlin 구현):
  `Math.toRadians/toDegrees` → 같은 상수, `Math.round` → `javaRound`, `Math.floorMod` · `String.hashCode` → `javaFloorMod` · `javaHash`,
  `System.currentTimeMillis()` → `epochMillis()`, 좌표 소수 둘째 자리 → `roundHalfUp2`(안드로이드 `BigDecimal`, iOS `NSDecimalNumber`),
  `String.format(Locale.ROOT, "%.Nf")` → `formatFixed`, `BigDecimal(v).setScale(n, HALF_UP)` → `exactHalfUp`, `"%,d"` → `formatGrouped`, `"%03d"` → `zeroPad`,
  `URLEncoder` → `urlEncodeUtf8`, 한중일 이름 글자(`Character.UnicodeScript`) → `isCjkNameLetter`. `shared/src/jvmTest/.../PlatformJvmTest.kt` 가 iOS 구현을 JVM 원본과 맞대어 본다.
- 날짜는 `java.time` 대신 **kotlinx-datetime 0.7**(`Dates.kt` — `KST` · `Long.localDate(zone)` · `todayIn` · `plusDays` · `isBefore` …, 예전 이름 그대로).
  시각은 `kotlin.time.Instant`. 안드로이드 화면이 아직 `DateTimeFormatter` 로 글자를 만드는 곳은 `core/DateCompat.kt`(`format` · `toJavaYearMonth` · `toEpochDay`)로 잇는다 — 화면을 옮기면(4단계) 사라진다.
- 다른 모듈의 `val` 은 스마트 캐스트가 안 된다 — 앱 화면에서 null 을 본 뒤 쓰는 곳은 `!!` 나 지역 값으로.
- 검사: 규칙 검사 22개는 `shared/src/commonTest`(kotlin.test — JVM · iOS 시뮬레이터 둘 다에서 돈다). 서버 응답 · 화면을 함께 보는 검사(`CrewHomeTest` · `StoryRunsTest` …)는 `app/src/test` 에 남았다.

## 데이터 (3단계)

- **서버 호출**(2026-11-03): `data/remote` 의 API 22개(`StepUpServer` · `SupabaseAuth` · `SessionHolder` · `CrewApi` · `EconomyApi` …)와 강화 서버 약속 `data/repo/ForgeServer` 를
  `shared/commonMain` 으로. 패키지는 그대로다. HTTP 는 `HttpPoster` 약속 하나로 — 기본값 `defaultHttpPoster()` 가 안드로이드는 예전 `UrlConnectionPoster`
  (`shared/jvmMain`, `HttpURLConnection` 그대로 — 동작 같음), iOS 는 `KtorPoster`(Ktor 3.6 Darwin = NSURLSession, `iosMain` 에만 — 안드로이드는 Ktor 를 받지 않는다).
  서버 시각 글자는 `Iso.kt`(`isoOffsetMillis` · `toIsoInstant`, `IsoJvmTest` 가 java.time 과 맞대어 본다).
  앱에 남은 것: 구글 로그인(`GoogleSignIn` — Credential Manager), 러닝 증명 올리기(`AttesterClient`), 장소 찾기(`PlaceSearchApi` — 앱 설정의 MapTiler 키).

- **로그인**(2026-11-03): 구글 · Apple 모두 Supabase id_token 로그인(`SupabaseAuth.signInWithGoogle` · `signInWithApple`), 일회용 값은 공용 `AuthNonce`
  (안드로이드 예전 방식 그대로 — 32바이트 16진수 · SHA-256). iOS 로그인 창만 Swift(`iosApp/StepUp/SignInUi.swift` — Apple AuthenticationServices,
  구글 GoogleSignIn SDK 8 · Swift 패키지)이고 나머지는 Kotlin `IosAuth`(세션은 키체인 `KeychainSessionStore` — 이 기기에만, 새로 깐 첫 실행에 지운다).
  Apple 계정을 지울 때는 Apple 토큰을 먼저 철회한다(워커 `/v1/apple/revoke` — `attester/src/apple.js`). 설정할 것은 [../APP-STORE.md](../APP-STORE.md) "0. 먼저 해 둘 것".

- **폰 DB**(2026-11-03): Room KMP 모듈 `:sharedData`(안드로이드 라이브러리 + iOS). 표 · DAO · `AppDatabase`(판 19)는 commonMain 하나.
  안드로이드는 예전과 똑같이 연다 — `Room.databaseBuilder(context, AppDatabase::class.java, "strideup.db")` · SupportSQLite · 옛 판 길 `AppMigrations`
  (androidMain, 예전 companion 의 MIGRATION_6_7 … 18_19 글자 그대로). 표 모양 기록은 `sharedData/schemas`(예전 `app/schemas`).
  iOS 는 `buildIosDatabase()` — 문서 폴더 strideup.db · 번들 SQLite · 판 19 에서 시작(옮길 옛 DB 가 없다). 판을 올리면 iOS 길도 연결(SQLiteConnection)로 더한다.
  iOS Room 의 KSP 는 Mac 에서만 돈다 — 검사는 CI 의 `:sharedData:iosSimulatorArm64Test`(IosDatabaseTest).

- **설정**(2026-11-03): `UserPrefs`(DataStore Preferences)와 `CrewDraftCodec` 을 `:sharedData` 로 — DataStore 두 개를 받는다.
  안드로이드는 `app/.../data/prefs/AndroidUserPrefs.kt` 의 `UserPrefs(context)` 가 예전 파일 그대로(`strideup_prefs` · `stepup_auth`, `preferencesDataStore` 대리자)를 넣는다 —
  부르는 곳 · 파일 · 백업 규칙이 그대로다. iOS 는 `createIosUserPrefs()`(문서 폴더의 같은 이름 파일, 로그인 세션은 키체인).
- **러닝 올리기**(2026-11-03): 규칙(과속 신고 먼저 · 10개씩 이어서 · 막혔으면 끝 · 실패면 다시)은 공용 `UploadPass`(`:sharedData`, `UploadPassTest`) —
  안드로이드 `SessionUploadWorker` 가 그대로 쓰고, iOS 는 `BGProcessingTask`(`iosApp/StepUp/UploadTask.swift`, 네트워크 필요) · 앱이 앞에 올 때
  `IosUploads.run` 이 같은 것을 돈다. 대기열 처리 `ClaimRepository` 도 `:sharedData` 로 옮겼다. iOS 의 올릴 곳은 화면 · 저장소를 붙일 때 `IosUploads.connect`.

## 화면 (4단계)

- **바탕(C0, 2026-11-03)**: 화면 글자(영어 기본 · 한국어 · 일본어 · 중국어)와 글꼴의 원본은 안드로이드 `app/src/main/res` 하나다 —
  `:sharedUi` 를 빌드할 때 `tools/sync_compose_resources.py` 가 composeResources 를 만든다(저장소에 사본 없음, `Res.string.*` · `Res.font.*`).
  Compose 리소스가 모르는 `\@` · `\?` 는 풀고 자리 번호 없는 `%s` · `%d` 는 `%1$s` … 로. Build APK 가 이 변환을 매번 돌려 본다(글자를 고쳐 iOS 가 깨지지 않게).
  테마(`ui/theme` — 팔레트 · 모양 · 글꼴 · 글자 크기)와 부품 `StepUpUi`(`ui/components`), 누름 · 모션(`ui/experience`)을 `:sharedUi` 에 같은 패키지 · 같은 이름으로 —
  다른 점: 글꼴은 테마가 읽어 `StepUpFonts` 에 넣는다(CMP 글꼴은 화면 안에서 읽는다), 한글 줄바꿈은 기기마다(`koreanLineBreak`), 소리 · 진동은 `ExperiencePlayer`.
  안드로이드 화면 코드를 옮길 때는 `tools/port_ui_to_cmp.py`(R → Res · 리소스 함수 · 글꼴 · 시계)로 기계 변환하고 남는 것만 손으로.
  안드로이드 앱이 이 화면 코드를 쓰게 되는 것(`androidTarget`)은 화면 묶음을 옮기며 차례로 — 그때 app 쪽 같은 파일을 지운다.
- **로그인 · 접속 코드 · 첫 안내(C1, 2026-11-03)**: `LoginContent`(+ iOS 에만 Apple 버튼) · `AccessCodeScreen` · 첫 안내(`OnboardingScreens`)를 공용으로.
  iOS 차례는 `IosRoot`(sharedUi/iosMain): 키체인 세션 → 문 확인(안드로이드 AccessGate 규칙) → 앱 안. 함께 옮긴 부품: `RunStyle` · `Components` · `UtilityParts` · `Motion` · `StepUpIcons` · `RunBackButton` · `Screen`.
- **홈 · 혼자 러닝 · 지도(C2, 2026-11-03)**: `HomeContent` · 자유 러닝 화면(`FreeRunLive` · `FreeRunResult`) · 지도(`LiveRouteMap` · `MapTiles` — 타일 받기만 기기별, iOS 는 Ktor) ·
  신발 점검 · 3·2·1 · StepUP!(`FreeRunStart`). 러닝 셈은 공용 `RunEngine`(`shared`, 위치 거르기 · 거리 · 시간 · 지금 자리 — 안드로이드 러닝 서비스와 같은 규칙, 검사).
  iOS 기기 코드: `IosRunSession`(Core Location · Core Motion → RunEngine), `IosSteps`(오늘 걸음), 기록은 같은 폰 DB · 같은 올리기.
- **신발(C3)**: 신발 탭(`CustomizeScreen`) · 신발 상세 · 수리 · 스탯 포인트 · 이름 바꾸기 · 그림(`ShoeDisplay` — 공용 디코드 · 바이트 예산 캐시).
  저장소(`SneakerRepository` · `RewardRepository` · `EconomySync` · `BoostRepository` · `ForgeRepository` · `RequestKeys`)는 `:sharedData` — 안드로이드도 같은 코드.
- **내 정보 · 설정(C4)**: 내 정보 · 분석 · 프로필 수정(사진 — iOS PHPicker) · 설정 목록 · 테마 · 언어(iOS 는 다음 실행부터) · 알림 · 개인정보 · 도움말 · 연결 계정(로그아웃 · 계정 삭제 — Apple 토큰 철회).
- **함께 뛰기 · 크루 · 채팅 · 커뮤니티 · 순위 · 기록 · 알림(C5 · C6)**: 화면 모두 `:sharedUi`. 저장소(크루 · 커뮤니티 · 크루 홈 · 명함 · 채팅 · 크루 러닝 · 순위 · 리더보드 ·
  기록 · 코칭 · 공지 · 장소 찾기 · 코스 · 이벤트 · 마켓 · 알림)는 `:sharedData`. 사진은 기기마다 — 안드로이드 Bitmap(`AndroidChatImageCodec` · `CrewImageStore`),
  iOS Skia(`IosImages` — 같은 크기 · 화질 단계). 공유 시트 · 캘린더 · 지도 앱 · 메일은 `ExternalIntents`(iOS UIActivityViewController · EventKit · Apple 지도).
- **공용 화면이 쓰는 이음새**(sharedUi `core/`): `AppServices`(저장소 · 러닝 `RunSessionControl` · 기기 일), `AppBuild`(빌드 값 — iOS 는 스토어 판처럼 WEB3 · 지갑 연결 끔),
  `AppTheme` · `AppLocale` · `AccessGate` · `InviteLinks` · `LocalFlags` · `Permissions`. 날짜는 `shared` 의 `DatePattern` · `PatternFormatter` · `DateCompat`
  (DateTimeFormatter · java.time 셈 자리 — JDK 와 맞대어 검사), 돈은 `Decimal`(BigDecimal 자리), 동시성은 `Concurrent.kt`.
- **앱 본체 · 남은 화면 전부(C7, 2026-11-03)**: 앱 본체 `MainScaffold`(하단 탭 넷 · 모든 길 · 머리 · 지갑 시트 · 러닝 되살리기)가 `:sharedUi` 에 있다 —
  iOS 는 로그인 · 접속 코드 단계(`IosRoot`)를 지나면 이것을 그대로 쓴다(예전 손으로 만든 탭 `IosMain` 은 지웠다). 로그인 단계는 기기마다 둔다(안드로이드 구글 · iOS 애플).
  뽑기 · 지갑 연결 · 체인 기록 · 코칭 · 소식 · 이벤트 · 초대 · 베타 코드 · 신발 보관함 · 마켓 · 지도 · 첫 설정 · 러닝 화면 전체(`WalkScreen` · 권한 안내 · 코스)도 공용.
  공유 그림(`RunShareCard`)은 공용 Compose 그리기로 다시 썼다(같은 자리 · 크기 · 색). 러닝은 `AppServices.run`(iOS `IosRunSession` — 1초 시계 · 위치 · 걸음).
  기기 이음새: 걸음 · 알림 권한 창(`rememberActivityPermissionLauncher` …), 그림 공유 · 사진첩(`shareImage` · `rememberRunImageSaver` — iOS 사진 보관함은 더하기만),
  지갑 앱 열기(`openWallet` — iOS 는 유니버설 링크만), 클립보드(`ExternalIntents.copyText`), 마지막 위치(`lastKnownLocation` — 날씨 풍경), `RasterCache` · `RunLocationWarmup`.
  잠금 화면 러닝 현황(`LockRunPanel`)은 안드로이드 전용이라 app 에 남는다(iOS 는 라이브 액티비티 — 따로 정할 일).
- **옮기는 도구**: `tools/port_ui_to_cmp.py`(R → Res · java.time · String.format · Toast · Intent 일부 · Context 빼기 …), `tools/prune_imports.py`,
  `tools/extract_kotlin_functions.py` · `tools/drop_kotlin_functions.py`.
- **남은 일**: `:sharedUi` 에 `androidTarget()` 을 더해 안드로이드가 같은 화면(`MainScaffold`)을 쓰고 app 의 같은 파일을 지운다 —
  안드로이드 쪽 이음새(actual: 권한 창 · 그림 공유 · 사진첩 · 지갑 앱 · RasterCache · 위치 미리 잡기 · 러닝 서비스 → `RunSessionControl`)를 채우고 테스트 APK 로 확인.

## Mac 에서 돌리기

```bash
brew install xcodegen
```

```bash
cd iosApp && xcodegen generate && open StepUp.xcodeproj
```

Xcode 에서 아이폰 시뮬레이터를 고르고 ▶. 빌드 첫 단계가 `./gradlew :sharedUi:embedAndSignAppleFrameworkForXcode` 로 Kotlin 틀을 만든다
(JDK 17 이 필요하다 — Xcode 는 셸 설정을 읽지 않으니 안 되면 `/usr/libexec/java_home -v 17` 이 나오는지 본다). 실기기는 `Config.xcconfig` 의 `TEAM_ID` 를 채운 뒤.

공용 규칙 검사는 리눅스에서도 된다: `./gradlew :shared:jvmTest`(안드로이드 빌드 Build APK 의 Unit tests 단계에서도 돈다).
iOS 시뮬레이터 검사는 Mac 에서 `./gradlew :shared:iosSimulatorArm64Test`.

## CI

`.github/workflows/ios.yml`(macOS 러너) — `shared/` · `sharedUi/` · `iosApp/` · Gradle 설정이 바뀔 때만 돈다(macOS 러너는 비공개 저장소에서 분이 10배로 깎인다).
공용 규칙 검사(JVM · iOS 시뮬레이터) → XcodeGen → 서명 없는 시뮬레이터 빌드 → 시뮬레이터에 띄워 화면 찍기(충돌 기록이 남으면 실패) →
`ios-launch-screenshot` · `StepUp-ios-simulator` 아티팩트(7일).
리눅스(안드로이드 CI)에서는 iOS 대상이 꺼진다(`kotlin.native.ignoreDisabledTargets=true`) — Kotlin/Native 도구를 받지 않아 안드로이드 빌드가 느려지지 않는다.

## 만드는 순서

| 단계 | 할 일 | 안드로이드에 닿나 |
|---|---|---|
| 0. 뼈대 (끝남, 2026-11-02) | 모듈 셋 · Xcode 앱 · 로고 화면 · 아이콘 · CI · 공용 규칙 둘 | 규칙 둘이 `shared/` 로 이사(동작 같음) |
| 1. Kotlin 올리기 (끝남, #152) | 2.0.21 → **2.3.21** · KSP **2.3.6**(KSP2) · Room **2.8.5** · Compose Multiplatform **1.12.2**. `android { kotlinOptions }` → `kotlin { compilerOptions }`. CI 의 Xcode 를 26 으로 | 예 — 테스트 APK 로 한 바퀴 확인 |
| 2. 규칙 옮기기 (끝남, 2026-11-03) | `domain/` 44개 · `domain/coaching/` 을 `shared/` 로. `java.time` · `java.util` 은 kotlinx-datetime 등으로. `check-coaching-twin.py` 경로를 함께 고친다 | 예(동작 같음) |
| 3. 데이터 (끝남, 2026-11-03) | 서버 호출 · 로그인 · 폰 DB · 설정 · 올리기 — 아래 "데이터 (3단계)". 안드로이드의 `strideup.db` · `strideup_prefs` 이름은 그대로 둔다 | 예 |
| **4. 화면** (진행 중 — 위 "화면") | `ui/` 를 `sharedUi/` 로 화면 묶음씩(테마 · 부품 → 로그인 · 접속 코드 → 홈 · 러닝 → 함께 뛰기 → 신발 → 내 정보). 글자(`res/values*` 4개 언어) · 글꼴 · 그림은 `composeResources/` 로 — `check-strings.py` · `check-copy.py` 도 새 자리를 본다 | 예 — 안드로이드 앱이 `sharedUi` 의 화면을 쓴다 |
| 5. iOS 전용 | 아래 표 | 아니요 |
| 6. TestFlight | 서명 · 올리기 CI, 내부 → 외부 테스터 | 아니요 |
| 7. 심사 | APP-STORE.md | 아니요 |

1 ~ 4 는 안드로이드 앱을 건드리므로 한 단계씩 테스트 APK 로 확인하고 넘어간다. 디자인 · 동작은 바꾸지 않는다(CLAUDE.md "지킬 것").

## 안드로이드 기능 ↔ iOS

| 안드로이드 | iOS |
|---|---|
| 러닝 포그라운드 서비스(`WalkSessionService`) | 백그라운드 위치 모드(`UIBackgroundModes: location`) + `CLLocationManager.allowsBackgroundLocationUpdates` — "사용하는 동안" 권한으로 러닝 중 파란 표시. "항상"은 묻지 않는다 |
| 구글 위치 서비스(`LocationEngine`) | Core Location(GPS · 와이파이 섞임이 기본). 걸러 내는 규칙(`TrackFixRule` 등)은 공용 |
| 걸음 센서 | Core Motion `CMPedometer` — 앱이 뒤에 있을 때는 돌아와서 그 사이 걸음을 한 번에 받는다(`queryPedometerData`). 권한 문구 `NSMotionUsageDescription` |
| 잠금 화면 현황(`LockRunHost` · `LockRunActivity`) · 실시간 알림 | **Live Activity**(잠금 화면 · 다이내믹 아일랜드, 위젯 확장 대상을 `project.yml` 에 더한다). 일시정지 버튼은 iOS 17+(App Intents). 앱을 잠금 위에 띄우는 방식은 iOS 에 없다 |
| 구글 로그인(Credential Manager → `id_token`) | Google Sign-In iOS SDK → 같은 `grant_type=id_token`. **Apple 로그인**을 더한다(심사 4.8) |
| FCM 푸시 | Firebase Messaging iOS + APNs 키. 서버 `push-send` 그대로 |
| 앱 링크(`assetlinks.json`) | 유니버설 링크 — `web/.well-known/apple-app-site-association`(Team ID 필요) + Associated Domains `applinks:stepupcrew.com` |
| 올리기 일꾼(WorkManager) | 앱이 앞에 올 때 · 네트워크가 되면 다시 보내기 + `BGTaskScheduler` |
| 테스트 APK 스스로 업데이트 | 없다 — **TestFlight** 가 대신한다 |
| 지갑 앱 찾기(`<queries>`) | `LSApplicationQueriesSchemes` — iOS 에 지갑 주소 연결을 넣을 때만(APP-STORE.md 결정) |
| `BuildConfig.WEB3` · `WALLET_LINK` | iOS 는 스토어 판과 같게 시작한다(WEB3 꺼짐) |
| Experience QA(에뮬레이터 캡처) | 나중에 iOS 시뮬레이터 캡처(`xcrun simctl io booted screenshot`) |

## 판 맞춤

- Kotlin 2.3.21 · KSP 2.3.6 · Room 2.8.5 · Compose Multiplatform 1.12.2 · AGP 8.10.1 그대로(`gradle/libs.versions.toml`, 2026-11-02 1단계).
  안드로이드 앱과 한 빌드라 Kotlin 판이 하나다.
  - 더 새 KSP(2.3.13 에서 확인)는 AGP 8.12 이상을 요구한다 — AGP 를 올릴 때 함께 올린다.
  - Compose Multiplatform 1.12 는 Kotlin 2.2.20 으로 만들어졌다 — Kotlin 은 그보다 같거나 새 판이어야 한다.
  - 축소기 R8 은 **9.0.55** 로 고정(`build.gradle.kts` 의 `buildscript`) — AGP 8.10 에 딸린 R8 은 Kotlin 2.3 메타데이터를 못 읽어 릴리스 · qa 빌드가 경고를 수백 줄 냈다. AGP 를 올리면 지운다.
- App Store 제출은 Xcode 26 · iOS 26 SDK 가 필요하다(2026-04 부터). Kotlin/Native 는 2.2.20 부터 Xcode 26 을 안다 — CI 는 Xcode 26 중 가장 새 판을 고른다.
