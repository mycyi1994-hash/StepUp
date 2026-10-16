# 테스트 APK 서명 키 바꾸기 (2026-10-16)

폰에 까는 테스트 APK(`test-apk` 릴리스의 `StepUp-test.apk`)는 지금까지 저장소에 들어 있는 **공개 키**
(`app/debug.keystore`)로 서명했다. 저장소가 공개라 누구나 같은 키로 "StepUp 업데이트"를 만들어
폰의 테스트 앱 위에 덮어 깔 수 있었다 — 앱 데이터와 로그인이 그대로 그 앱으로 넘어간다.

이제 테스트 APK 는 **저장소 밖에만 있는 새 키**로 서명한다. 옛 키에서 새 키로 넘어간다는 증명(서명 이력,
APK Signature Scheme v3 키 회전)을 함께 붙이므로 **폰에 깔린 테스트 앱 위에 그대로 덮어 설치된다**(지우지 않아도 된다).
한 번 새 키로 올라간 폰에는 옛 공개 키로 만든 APK 가 더는 덮어 깔리지 않는다.

| | 옛 키(공개) | 새 키(저장소 밖) |
|---|---|---|
| 어디에 | `app/debug.keystore` | GitHub 환경 `test-apk` 의 비밀 `TEST_APK_SIGNING_KEY` (인증서만 `app/test-apk-signing.x509.pem`) |
| SHA-1 | `34:ED:A6:73:DF:C6:27:FD:AF:D1:06:C0:A0:05:91:1C:AF:59:8A:EC` | `91:4B:36:0F:54:DD:FA:87:7E:AD:2B:8A:61:A4:03:FF:EC:16:23:16` |
| SHA-256 | `90:82:87:38:7B:4F:62:7C:5B:42:05:61:70:C3:B0:15:BF:BC:38:90:1D:EE:20:95:7F:38:B6:B1:3E:EB:34:D4` | `B4:4C:E6:A4:0C:EB:55:0E:3F:50:13:67:2C:74:DD:26:AC:BE:FA:D3:36:5A:11:51:50:25:2B:BA:29:5C:17:A4` |
| 쓰는 곳 | 개발 · 에뮬레이터 빌드(debug), 작업 브랜치의 qa 아티팩트 | main 에서 올리는 테스트 APK(`test-apk` · `apk-dist`) |

새 키를 넣기 전까지는 **아무것도 바뀌지 않는다** — CI 는 전처럼 옛 키 서명 그대로 올리고 경고만 남긴다.

## 사용자가 할 일 (두 가지, 이 순서로)

### 1. 구글 로그인에 새 지문 등록

새 키로 서명한 앱도 구글 로그인이 되려면 구글에 지문을 하나 더 등록해야 한다. 옛 등록(`StepUp Android`)은 **지우지 않는다**.

1. [Google Cloud Console](https://console.cloud.google.com/apis/credentials) → 웹 클라이언트(`StepUp Web`)가 있는 프로젝트
2. **+ 사용자 인증 정보 만들기** → **OAuth 클라이언트 ID** → 애플리케이션 유형 **Android**
3. 이름 `StepUp Android (test key)` · 패키지 이름 `com.stepup.android` · SHA-1 `91:4B:36:0F:54:DD:FA:87:7E:AD:2B:8A:61:A4:03:FF:EC:16:23:16`
4. **만들기**

### 2. GitHub 에 새 키 넣기

키 값은 Claude 가 이 세션에서 파일(`TEST_APK_SIGNING_KEY.txt`, 184자 한 줄)로 전달했다. 저장소 · 채팅에는 남기지 않았다.

1. 저장소 **Settings** → **Environments** → `test-apk` (없으면 **New environment** 로 같은 이름을 만든다)
2. **Deployment branches and tags** → **Selected branches and tags** → **Add deployment branch or tag rule** → `main`
   — 작업 브랜치가 워크플로를 고쳐도 키에 닿지 못하게 main 에서만 쓰게 묶는다.
3. **Environment secrets** → **Add environment secret** → 이름 `TEST_APK_SIGNING_KEY`, 값은 파일 내용 그대로 → **Add secret**
4. 키 파일은 비밀번호 관리자처럼 안전한 곳에 한 부 보관하고, 내려받은 사본은 지운다.
   **이 키를 잃으면** 새 키로 올라간 폰은 다음 키로 넘어갈 수 없어 앱을 지우고 다시 깔아야 한다(폰에만 있던 기록이 사라진다).

넣은 뒤 main 에 다음 빌드가 올라가면 릴리스 설명의 **서명** 칸이 "테스트 전용 키"로 바뀐다. 폰의 앱은 평소처럼
내 정보 › 설정 › 테스트 빌드 업데이트로 받으면 된다.

## 바뀌는 것 · 주의

- **안드로이드 9 이상**: 새 키로 넘어간다. **안드로이드 8**: 서명 이력을 몰라 옛 키(v2 서명)로 확인한다 — 덮어 설치는 되지만 보호는 옛 그대로다.
- 새 키로 올라간 폰에는 **작업 브랜치의 qa 아티팩트(옛 키)가 덮어 깔리지 않는다**("앱이 설치되지 않음"). 미리 보려면 main 의 테스트 APK 를 쓴다.
  작업 브랜치 빌드에는 키를 주지 않는다 — 검토 전 코드가 키를 쓰지 못하게.
- 옛 키에는 "앱 데이터 이어받기"와 "서명 권한" 신뢰만 남겼다. 되돌리기(옛 키로 다시 업데이트) · 공유 UID · 계정 인증 신뢰는 뺐다.
  서명 권한을 빼면 안드로이드가 업데이트를 막는다(앱이 스스로 정한 권한을 다시 선언하지 못해 `INSTALL_FAILED_DUPLICATE_PERMISSION` —
  Experience QA 에뮬레이터에서 확인). 남겨 둔 대가는 옛 키로 서명한 다른 앱이 그 권한(androidx 의 `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`)을
  받을 수 있다는 것 — 안드로이드 12 이하에서 라이브러리가 등록한 내보내지 않는 수신기에 신호를 보낼 수 있는 정도다.
- 스토어 빌드(release, Play 업로드 키)와는 관계없다.

## 어떻게 되어 있나

- `tools/sign_test_apk.sh` — Gradle 이 옛 키로 만든 qa APK 를 받아 서명 이력(옛 키 → 새 키)을 만들고 다시 서명한 뒤,
  v3 서명자가 새 키인지 · 서명 이력이 옛 키 → 새 키인지 확인한다. 키와 인증서(`app/test-apk-signing.x509.pem`)가 짝이 아니면 멈춘다.
- `.github/workflows/build-apk.yml` — 빌드 작업은 읽기 권한만 있고 키를 받지 않는다. main 에서만 도는 `publish` 작업(환경 `test-apk`)이
  키로 다시 서명해 `test-apk` 릴리스와 `apk-dist` 브랜치에 올린다.
- `tools/check_test_apk_rotation.sh` — Experience QA 에뮬레이터에서 일회용 키로 같은 절차를 밟아, 옛 키 앱 위에 회전 서명 APK 가
  업데이트로 깔리고 그 뒤 옛 키 APK 는 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 로 막히는지 확인한다.
