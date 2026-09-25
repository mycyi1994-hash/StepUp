# StepUp 다음 Codex 인수인계 지시서

## 최신 체크포인트 — 2026-09-25 캐릭터 없는 버전, 5단계

현재 사용자 결정은 캐릭터·의상·꾸미기 제거, 신발 수집/착용/마켓/뽑기 유지, 기록 중심 홈이다. 아래 9월 24일의 캐릭터 관련 순서는 역사적 기록이다.

4단계 검증 소스 `6e327f6`의 빌드와 Android 14·15 기기 검사/각 56장 캡처가 통과했다. 5단계에서 실제 Claude 읽기 전용 검수를 완료했고 판정은 **변경 요청**이다. 앱 소스는 4단계 이후 변경하지 않았다. 다음은 [5단계 결과와 후속 수정 목록](CHARACTER-FREE-STAGE5-2026-09-25-KO.md)의 필수 4묶음(하단 메뉴, 작은 화면 러닝, 프로필, 커뮤니티 안내) 수정이다. Claude 원문에 있는 잘못된 탭 수 계산과 과도한 글자 축소 제안은 그대로 구현하지 않는다.

기존 미추적 `design/redesign-2026-09/screens/`, 전체 화면 검수 문서/CSV 및 두 도구 파일은 계속 보존한다. main 병합이나 공개 APK 배포는 하지 않았다. 전체 화면 디자인 완료를 주장하지 않는다.

## 최신 사용자 지시 — 모듈형 디자인 기획 (2026-09-24)

사용자는 남은 50장 이상의 전체 화면과 버튼·배경 등의 독립 제작/재사용을 다시 강조했다. 다음 실행은 [디자인·모듈·자산 기획서](DESIGN-EXECUTION-PLAN-KO.md)의 **3A 공통 부품 보드/분해 조합도 → 3B 최소 합성 검증 → 3C 홈·시작** 순서다. [77개 화면·상태 적용표](DESIGN-SCREEN-BACKLOG-KO.md)는 제안 매핑이며 완료 보고가 아니다. 현재 버튼/UI와 배경은 분리돼 있지만 캐릭터 몸·의상·신발은 전신 이미지 한 장이다. 신규 레이어 합성이 이미 구현됐다고 말하지 않는다. 이번 개정은 문서 작업이며 앱 소스와 이미지 자산은 변경하지 않았다. 아래 이전 상태 기록은 역사적 맥락으로 읽는다.

작성: 2026-09-24. 인수인계 후 사용자가 남은 변경 전체의 커밋·푸시·배포를 요청했다. 이 문서를 포함한 작업을 브랜치에 보존하고, 빌드가 성공하면 별도 리디자인 테스트 APK로 배포한다. 이는 디자인 완성이나 main 병합을 의미하지 않는다. 새 기능 확장은 요청받지 않았다.

**아래 Git 미커밋 목록은 인수인계 작성 시점의 역사적 스냅샷이다. 후속 보존 커밋에 모두 포함한다. 현재 여부는 git status와 원격 HEAD로 확인하고, 배포 원본 SHA는 GitHub 리디자인 사전 릴리스 설명에서 확인한다.**

## 1. 가장 중요한 사용자 교정

사용자는 GASOK 출품용 StepUp의 **전체 이미지 디자인 변형, 높은 비주얼 완성도, 단순한 UI/UX, 톤앤매너와 디자인 일관성**을 가장 중요하게 생각한다.

이전 Codex는 전체 완성이라는 목표를 따라가다 저장 구조·보상·오류 처리·자동검사에 과도하게 집중했다. 사용자는 크레딧 부족을 언급했고, 디자인이 끝나지 않은 상태에서 내부 기능 작업을 오래 한 것을 명확히 지적했다. 이 우선순위 이탈을 반복하지 말 것.

- **다음 구현의 중심은 사용자가 볼 수 있는 디자인 결과다.** 시안과 실제 화면을 나란히 비교하고, 실제 앱 캡처를 보여줘야 한다.
- 기능 코드를 계속 확장하거나 테스트 개수를 늘리는 것을 디자인 진척으로 보고하지 말 것.
- 모든 기능 완성이라는 장기 범위는 유지하되, 당장 디자인을 막는 중대한 오류 외에는 별도 목록에 남겨 디자인 마감을 밀어내지 말 것.
- 사용자가 크레딧을 걱정하고 있다. 이전 자동 Goal을 관성적으로 계속 수행하거나 이미지 생성을 시작하지 말 것. 현재 커밋·푸시·테스트 APK 배포 요청은 승인되어 있고, 그 이상의 작업 재개 범위는 새 사용자 지시를 확인한다.

## 2. 제품과 시각 방향

- 일반 사용자와 라이트 코인 사용자가 운동·캐릭터·꾸미기로 들어오는 앱. Web3 지식이나 지갑 설명이 첫 경험의 중심이 아니다.
- 단순함은 저품질/밋밋함이 아니다. 그림은 풍부하고 정교하게, 동시에 보이는 정보와 선택은 적게. 한 화면의 다음 행동 하나를 분명하게 한다.
- RUNO(남성), LUMI(포니테일 여성): 검은 얼굴, 푸른 발광 눈, UP 의상, 기존 형태·비율·재질 유지.
- 푸른 밤/노을 강변, 깊이 있는 배경과 캐릭터 중심 구성. 화면마다 조명·재질·캐릭터 비율이 다른 제품처럼 보이면 안 된다.
- 원본 STEPUP 워드마크를 재사용한다. 글자로 흉내 내거나 새 로고를 생성하지 않는다.
- 공통 헤더·잔액·하단바·버튼·뒤로 가기·글자 크기·여백을 한 구현과 상수로 고정한다. 화면별 복제/임의 덮어쓰기를 금지한다.
- 하단 탭 순서: 러닝 → 꾸미기 → 커뮤니티 → 내 정보. 선택 상태만 바뀐다. 숨김/상세 화면 예외는 중앙 라우트 정책이 소유한다.

## 3. 기준 시안과 자산 위치

사용자가 지정한 시안 폴더:
`C:/Users/gana0/OneDrive/문서/New project 3/output/stepup-concepts-v2`

확인된 파일:
1. `01-launch-loading.png` — 앱 실행 로딩
2. `02-logo-reveal.png` — 로고 플레이
3. `03-home.png` — 홈
4. `04-running.png` — 러닝 중
5. `05-community.png` — 커뮤니티
6. `06-wardrobe.png` — 꾸미기
7. `07-challenge.png` — 챌린지
8. `08-profile.png` — 내 정보

같은 폴더에 `index.html`, `prompts.md`가 있다. **사용자는 처음 두 시작 화면도 반드시 포함하라고 강조했다.** 번호를 임의로 다시 해석하지 않는다.

레포 참고 자료: `design/redesign-2026-09/concepts/`, `design/redesign-2026-09/assets/`, `design/characters/`, `design/equipment/`.
실제 앱 자산: `app/src/main/res/drawable-nodpi/`의 캐릭터·장비 이미지, `scene_riverside_night.webp`, `scene_riverside_sunset.webp`, `scene_wardrobe_terrace.webp`, `scene_terrace_stage.webp`, 원본 워드마크.

시안은 목표 이미지이고 실제 앱 캡처가 아니다. 시안 제작 완료를 앱 디자인 완료라고 보고하지 않는다. 배경/캐릭터만 이미지로 쓰고 버튼·문구·숫자·탭은 실제 UI로 구현한다.

## 4. 현재 Git 상태 — 작성 시 직접 확인

- 레포: `C:/Users/gana0/StepUp`
- GitHub: `https://github.com/mycyi1994-hash/StepUp`
- 작업 브랜치: `codex/stepup-cohesive-redesign`
- 로컬 HEAD: **6497786** — 저장 진행/실패 화면과 재시도 처리.
- 원격 브랜치: **9cd67460bf29acfb5284cd5018cd75ebb773dfbb** — `git ls-remote`로 확인.
- **6497786은 아직 푸시되지 않았다.** 원격 추적 ref가 로컬에 없어 `git log origin/...`가 실패했다. remote가 없다고 오해하지 말고 `git ls-remote`로 확인한다.

이 문서 작성 전부터 존재한 미커밋 변경:

```
 M app/src/androidTest/java/com/stepup/android/DatabaseMigrationTest.kt
 M app/src/androidTest/java/com/stepup/android/PrivacyPermissionTest.kt
 M docs/redesign/PROGRESS.md
 M tools/run_screen_gallery.sh
?? app/src/androidTest/assets/schema-v13.sql
?? app/src/androidTest/java/com/stepup/android/RunSaveRecoveryTest.kt
```

내용: v13→14 데이터 보존 검사, 실제 서비스의 저장 실패→재시도 검사, 권한 검사를 실제 공통 화면 틀 안에서 실행하도록 변경. **아직 컴파일/실행하지 않은 변경이다.** 삭제하거나 완료된 작업으로 취급하지 않는다.

로컬에만 있는 커밋과 미커밋 파일이 있으므로 다른 컴퓨터에서 원격만 clone하면 전부 이어받을 수 없다. 이 작업 폴더를 그대로 사용하거나, 사용자가 다른 환경으로 옮기길 원할 때 변경 전체를 전달해야 한다. 이 인수인계 작성 중에는 다른 변경을 묶어 커밋/푸시하지 않았다.

## 5. 디자인 작업 현황 — 완성으로 과장하지 말 것

구현한 범위:
- 공통 화면 틀과 디자인 상수, 원본 로고 역할, 헤더/하단바와 중앙 표시 정책.
- 홈의 실제 착용 캐릭터와 시작 버튼, 러닝/일시정지/완료 화면.
- 꾸미기의 큰 캐릭터 미리보기와 의상/신발 선택, 커뮤니티, 프로필, 챌린지, 지갑 및 여러 설정 화면 수정.
- 첫 사용 안내 11단계 → 4단계(러닝·꾸미기·커뮤니티·내 정보).
- 글자 확대/키보드/하단 영역 문제 일부 수정. Android 15 필터 버튼 잘림은 새 캡처와 전체 버튼 경계 검사로 확인했다.

**아직 안 끝난 범위:**
- 모든 화면이 승인 시안 수준으로 완성된 것이 아니다. 전체 조명·캐릭터 비율·여백·타이포·버튼 표현 최종 비교가 필요하다.
- 프로필은 서 있는 캐릭터다. 기준 시안의 앉은 연출은 미구현.
- 일부 장비 이미지 해상도와 포즈별 일관성이 부족하다. 전체 착장 조합이 동일 품질인지 확인하지 않았다.
- 모든 상세/팝업/빈 상태/오류 상태/로그인 전후를 실제로 확인하지 않았다.
- 최신 버전의 모든 화면을 한 장씩 나열한 **최종 검수 갤러리**가 없다.
- 최종 제출용 APK + 같은 버전의 전체 캡처 + 완료 검증 보고서가 없다.

자동 목록: 33개 route, 34개 screen 함수, 19개 overlay. 이것은 완성된 화면 수가 아니다. `SCREEN-INVENTORY.md`/JSON은 생성되는 조사 목록이고, 모든 행의 완성을 증명하지 않는다. 수동 시각 검토는 `VISUAL-REVIEW.md`를 참고한다.

## 6. 다음 디자인 작업 순서

새 구현을 요청받으면:

1. 위 8개 원본 시안을 실제로 열고, 현재 앱 캡처와 비교한다. 이전 설명만 믿고 이미지를 안 보거나 새 방향을 제안하지 않는다.
2. 공통 로고·헤더·SUP 표시·하단바·버튼이 실제 화면에서 안정적인지 확인한다. 기존 공통 구현을 사용하고 새 화면마다 재작성하지 않는다.
3. **01 로딩 → 02 로고 플레이 → 03 홈**을 먼저 시안 수준으로 마감한다. 사용자가 볼 수 있는 실제 캡처를 제공한다.
4. 홈과 동일한 캐릭터/재질/광원 기준으로 러닝 → 완료 → 꾸미기를 마감한다.
5. 커뮤니티 → 챌린지 → 프로필을 마감하고, 나머지 상세·설정·팝업·상태에 같은 규칙을 적용한다.
6. 각 묶음마다 원본 시안과 실제 구현의 차이, 고친 부분, 남은 부분을 짧게 보여준다. 긴 내부 작업 보고로 대신하지 않는다.
7. 마지막에 실제 앱의 모든 화면/필요 상태를 한 장씩 나열해 검수하고, 동일 커밋의 APK를 제공한다.

우선순위: **사용자가 보는 디자인 결과 → 디자인을 막는 기능 오류 → 나머지 내부 안정화.** 테스트가 통과하기 쉬운 작은 변경을 반복하며 시각 완성을 미루지 않는다.

## 7. 관련 코드 위치

모두 `app/src/main/java/com/stepup/android/` 아래:
- `ui/theme/StepUpDesign.kt` — 공통 규격.
- `ui/StepUpRoot.kt` — 공통 shell, 헤더/하단바, 라우팅/표시 정책.
- `ui/components/RenewalParts.kt` 및 공통 컴포넌트들 — 버튼·캐릭터·화면 요소.
- `ui/screens/splash/SplashScreen.kt` — 시작 화면.
- `ui/screens/home/HomeScreen.kt`
- `ui/screens/walk/WalkScreen.kt` — 러닝 화면(폴더 이름은 run이 아니라 walk).
- `ui/screens/customize/CustomizeScreen.kt`
- `ui/screens/community/CommunityScreen.kt`
- `ui/screens/profile/ProfileScreen.kt`
- `ui/guide/GuideTour.kt` — 안내.

먼저 읽을 문서: `PRODUCT.md`, `DESIGN.md`, `AGENTS.md`, 본 문서. `PROGRESS.md`는 긴 변경 기록이므로 필요한 최신 항목만 확인한다. 기존 지침과 충돌하면 최신 사용자 우선순위를 따른다.

## 8. 내부 코드 작업 인계 — 디자인보다 앞세우지 말 것

- Room 현재 코드 버전 **14**. 기존 `strideup.db`, `strideup_prefs` 이름을 유지한다.
- Room 13: 새 러닝에 시작 계정 저장, 기존 기록은 legacy로 보존. 업로드는 실제 전송 토큰의 계정과 비교한다. **전체 계정 분리가 아니다.** 보상·장비·설정·총계 등은 여전히 공유 위험이 있다.
- Room 14: `run_settlements` 영수증으로 운동 기록·로컬 원장·알림을 한 transaction에 저장. 에너지 차감은 DataStore 영수증으로 재시도 시 중복 방지. 이것은 서버 SUP 확정 증명이 아니다.
- `RunCheckpointStore.kt`: 중간 기록의 원자적 파일 저장 계층. **서비스에 연결하지 않았으므로 앱 강제 종료 후 실제 복구는 아직 안 된다.**
- `6497786`: 저장 중/실패 상태, 재시도 버튼, 일단 저장을 시작한 러닝의 재개 금지. 코스/종족 후속 작업은 같은 프로세스 내 재시도 중복을 막는 수준이며 durable recovery는 아니다.
- 미전송 코스 기록을 20개로 잘라 버리던 제한 제거.
- 권한 화면은 실제 거절/대략적 위치/정확한 위치를 읽고 설정 복귀 시 갱신한다.

미완성 세부 사항은 `ACCOUNT-DATA-AUDIT.md`, `RUN-RECOVERY.md`에 있다. GIWA 실제 연동, 실 로그인/서버 보상, GPS/백그라운드/실기기 검증도 완료되지 않았다. GASOK 기준은 `GASOK-EVIDENCE.md`를 참고하되 실제 확인한 사실과 해석을 구분한다. 사용자에게 GIWA 비공개 문서/연동 안내가 있는지 물었으나 답변은 받지 못했다.

## 9. 검증 증거와 제한

최신 원격 커밋 9cd6746:
- Build APK **35934709879: 성공**(작성 시 다시 확인). 6497786이나 미커밋 변경의 성공을 의미하지 않는다.
- Gallery **35934709852: 전체 실패**.
- Android 14 interaction: **29개 중 28개 통과**. 중간 기록 저장 3개, 원자적 정산 2개, 기존 migration 4개 포함. Chrome 테스트는 챌린지 진입 시 `No compose hierarchies found`로 실패. 에뮬레이터가 사라진 실행으로 단정하지 말 것. 다른 28개 검사는 끝까지 실행됐다.
- Android 14·15 permissions: 둘 다 성공. 실제 Settings 열기/Back, 거절→대략적→정확한 위치 갱신 포함. 다만 촬영 당시에는 공통 shell 밖의 테스트 화면이어서 흰 배경/헤더 대비가 잘못 보였다. 미커밋 변경에서 MainScaffold로 교체했으며 새 캡처는 아직 없다.
- Android 14·15 large-font: 둘 다 성공.
- 그 외 최신 실패는 아직 각각 원인을 확인하지 않았다. 녹색 검사 하나로 전체 시각 완료라고 판단하지 않는다.

실제 증거 폴더:
- `C:/Users/gana0/StepUp-captures/redesign-9cd6746/api-34-interaction`
- `C:/Users/gana0/StepUp-captures/redesign-9cd6746/api-35-permissions`
- `C:/Users/gana0/StepUp-captures/redesign-e537c44/api-34-gallery`
- `C:/Users/gana0/StepUp-captures/redesign-e537c44/api-35-large-font`
- 다른 이전 revision 폴더도 `C:/Users/gana0/StepUp-captures/` 아래에 있다. 항상 파일의 revision을 구분한다.

일부 갤러리는 큰 기기 화면 안에 고정 dp viewport를 넣어 주변 흰 영역이 보인다. 최종 사용자용 전체 화면 캡처로 대신하지 않는다. 예시 데이터와 실제 계정 데이터를 혼동하지 않는다.

## 10. 재개 시 필요한 최소 실행 지식

로컬 Android SDK/JDK/adb가 준비되어 있지 않았으므로 빌드는 GitHub Actions를 사용했다. PowerShell 환경, Python은 `py`, GitHub CLI 인증은 사용 가능했다. 다시 확인할 것.

로컬 기본 검사:
```
py tools/ui_inventory.py
py tools/check_design_contract.py
py scripts/check-strings.py
py tools/check_experience_assets.py
git diff --check
```

CI: `.github/workflows/build-apk.yml`, `.github/workflows/screen-gallery.yml`, 실행 스크립트 `tools/run_screen_gallery.sh`.
새 push가 진행 중 APK 빌드를 취소할 수 있으므로 기존 build가 끝났는지 확인하고 묶어서 올린다. 현재 확인한 35934709879는 이미 종료됐다. 인수인계만 요청받은 상황에서는 이 이유로 새 push/CI를 만들지 않는다.

핵심: **완성은 입증되지 않았다. 다음 Codex는 내부 코드 확장을 관성적으로 계속하지 말고, 최신 사용자 요청에 따라 시각적 디자인 마감에 집중한다.**
