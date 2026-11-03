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
| 3. 데이터 | 서버 호출(`data/remote` — 지금 `HttpURLConnection`)을 Ktor 로, 폰 DB 를 Room KMP 로, 설정을 DataStore KMP 로. 안드로이드의 `strideup.db` · `strideup_prefs` 이름은 그대로 둔다 | 예 |
| 4. 화면 | `ui/` 를 `sharedUi/` 로 화면 묶음씩(테마 · 부품 → 로그인 · 접속 코드 → 홈 · 러닝 → 함께 뛰기 → 신발 → 내 정보). 글자(`res/values*` 4개 언어) · 글꼴 · 그림은 `composeResources/` 로 — `check-strings.py` · `check-copy.py` 도 새 자리를 본다 | 예 — 안드로이드 앱이 `sharedUi` 의 화면을 쓴다 |
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
- App Store 제출은 Xcode 26 · iOS 26 SDK 가 필요하다(2026-04 부터). Kotlin/Native 는 2.2.20 부터 Xcode 26 을 안다 — CI 는 Xcode 26 중 가장 새 판을 고른다.
