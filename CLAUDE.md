# StepUp — 작업 메모

## 최신 시작점 — S2 리디자인 인계 (2026-09-26)

**먼저 [docs/redesign/s2/README.md](docs/redesign/s2/README.md)를 읽는다.** 사용자는 Figma S2를 최대한 따르되 확보한 이미지로 진행하도록 했다. 기획·원본 이미지·45개 프레임/34개 기존 경로 대응표·6개 비교 화면을 레포에 정리했다. S2 앱 구현은 아직 시작하지 않았다. 새 작업 범위는 현재 사용자 지시를 따른다.

이하 캐릭터/옷장 관련 설명은 이전 방향의 기록이다. 현행 캐릭터 없는 앱과 새 S2 방향을 우선한다. 비교 HTML은 최종 승인된 설계나 실제 APK 결과가 아니며, 서비스 정책·상품 매핑은 임의로 만들지 않는다. 이미지 내보내기 제한 때문에 사용자에게 동일한 추출을 다시 요구하지 않는다.

## 브랜드 · 디자인 자산 (먼저 볼 것)

- **로고 · 앱 아이콘 · 태그라인**: `design/brand/` — 어느 바탕에 어느 파일을 쓰는지는
  `design/brand/README.md`. 로고가 필요하면 이 파일을 그대로 쓰고, 새로 그리거나
  글꼴로 흉내 내지 않는다. 바탕색에 맞는 변형을 고른다(어두운 바탕 → on-navy,
  밝은 바탕 → on-white, 파란 면 → white-on-blue, 정사각형 → app-icon).
- **캐릭터 이름**: 남자 **RUNO(루노)**, 여자 **LUMI(루미)** — 가이드와 앱 그림 목록은
  `design/characters/README.md`.
- **화면 시안 · 캐릭터**: `design/blue-black-2026-09/` — 적용 현황과 추가로 필요한
  그림은 `적용-현황.md`. 캐릭터는 `res/drawable-nodpi/avatar_*.webp` 완성 그림을 쓰고
  코드로 다시 그리지 않는다. 성별·착장을 속여 보이지 않는다(`domain/AvatarArt.kt`).
- **RUNO · LUMI 장비(신발 52 · 의상 5)**: `design/equipment/`(RUNO) · `design/equipment/lumi/`(LUMI,
  모자는 의상을 따라감 — CAP_RULES.md) — 원본 시트 · 카탈로그 · 요청문,
  앱에 들어간 그림과 남은 그림은 `runtime-assets.md`. 그림을 더하면
  `python3 tools/gen_avatar_res.py` 로 `AvatarArtRes.kt` 를 다시 만든다.

## 지킬 것

- `strideup.db` · `strideup_prefs` 이름은 바꾸지 않는다(설치된 사용자 데이터가 사라진다).
- Supabase service_role / `sb_secret_*` 키, Google `GOCSPX-*` 비밀, 지갑 개인키 ·
  시드는 앱 코드 · 번들 · 로그 · 커밋에 넣지 않는다.
- 서버가 확인하기 전에는 "적립 완료"라고 쓰지 않는다. 예시 숫자로 실제 잔액을
  꾸미지 않는다. 데모는 데모 모드에서만, "데모" 표시와 함께.
- 사용자가 말한 범위 밖은 고치지 않는다. 디자인 작업에서는 기능이 바뀌지 않게 한다.
- 화면은 시안대로 버튼을 적게 — 한 화면에 큰 주 행동 하나. 덜 쓰는 기능은 지우지 말고
  내 정보 › 설정 같은 안쪽으로 옮긴다.

## 빌드 · 확인

로컬에 안드로이드 SDK 가 없다. 푸시하면 CI 가 돈다.

- Build APK → `test-apk` 사전 배포에 `StepUp-test.apk` — **main 에 들어간 것만** 올라간다.
  작업 브랜치의 APK 는 그 실행의 Actions 아티팩트에서 받는다. 사용자가 받을 앱은 main 에 합쳐야 바뀐다.
- Experience QA → 에뮬레이터 캡처를 `qa-captures` 사전 배포의 `qa-review.zip` 에.
- 로컬에서 되는 검사: `python3 scripts/check-strings.py`,
  `python3 tools/check_experience_assets.py`.
