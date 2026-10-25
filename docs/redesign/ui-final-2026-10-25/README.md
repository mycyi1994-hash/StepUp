# UI 최종 통합안 (2026-10-25 전달본)

원본: `StepUp_UI_최종_개발전달_2026-10-25.zip`(사용자 공유 Drive) — `04_개발_넘김/HANDOFF.md` 순서(글꼴 → 테마 → 부품 → 화면 → 움직임 → 장소 배경 · 날씨 → 확인)대로 옮겼다.
사용자 지시: "이거 확인하고 적용해". 전달본의 약속은 **UI 레이어만** 바꾸는 것이다(데이터 · 서버 호출 · 내비게이션 구조 · 문구는 그대로).
아래 "시안과 다른 점"은 그 약속과 앱의 실제 데이터 · 이전 사용자 결정 때문에 시안대로 하지 않은 곳이다.

## 단계(커밋)

| 단계 | 내용 |
|---|---|
| 1 | 글꼴 Paperlogy(한글) · Saira Condensed SemiBold(숫자, 유료 Agency FB 대신 — HANDOFF 1-1) · 다크블루 / 화이트블루 팔레트(`tokens.json`) · 창 바닥색으로 시작 흰 번쩍임 없앰 · 공통 부품(`ui/components/StepUpUi.kt`) |
| 2 | 예전 버튼 계열(Run · FreeRun · Crew · Blue · Story · Volt · Ghost …)을 공통 주 · 보조 · 위험 버튼으로 · 러닝 화면 키트(`RunStyle`) 색 · 하단 탭바(반투명 · 가는 선 아이콘) |
| 3 | 시작 화면 · 함께 뛰기(번개러닝 · 크루 · 이야기 세그먼트) · 장소별 배경 사진(`assets/backgrounds`, `spots.json`, `ui/background/RunBackground.kt`) · 날씨 효과(`WeatherEffect.kt`) · 탭 머리 |
| 4 | 러닝 홈(오늘 걸음 · 자주 달리는 곳 사진 카드 · 둥근 시작) · 신발 카드(`ShoeCard.kt` — 기울기 · 유리 빛) · 신발 탭 |
| 5 | 내 정보(착용 신발 사진 · 지금까지 달린 거리 · 숫자 줄 · 묶음 목록) |
| 6 | 자유 러닝 중 · 일시정지 · 길게 눌러 종료 · 러닝 완료 |
| 7 | 번개러닝 상세 · 참여 완료 |
| 8 | 내 크루 · 승인제 가입 신청 시트 · 크루 상세 · 크루 만들기 입력칸 |
| 9 | 러닝 코스(선택 · 만들기 · 게시판) · 코스 기록 순위 시트 |
| 10 | 글쓰기(분류 세그먼트 · [−][값][+] · 올리기 하나) |
| 11 | 지도(내 주변 · 땅따먹기) |
| 12 | 기기 테스트 · 색 대비 · 문서 |

## 화면 대응표(시안 29장)

| 시안 | 앱 | 상태 |
|---|---|---|
| 01 시작화면 | `splash/SplashScreen.kt` | 적용 — 망원 저녁 사진 · 왼쪽에서 들어오는 워드마크 · 아래 2dp 로딩 선 |
| 02 함께뛰기 · 03 번개러닝 목록 | `community/CommunityScreen.kt` · `TogetherHome.kt` | 적용 — 세그먼트 셋, 가장 가까운 번개 사진 포스터(LIVE 한 줄 · 큰 시각 · 같이 달리기 + 공유), 이번 주 가로 카드 |
| 04 번개러닝 상세 · 05 참여 완료 | `community/FlashRunDetailScreen.kt` | 적용 — 참여가 서버에서 확인된 뒤에만 완료 화면이 아래에서 올라온다 |
| 06 이야기 | `community/stories/StoriesTab.kt` | 적용 — 범위는 글자 탭(알약 칩 줄 없음) |
| 07 지도 내주변 | `map/MapScreen.kt` | 적용 — 알약 핀 · 내 자리 점 · 아래 근처 번개 카드 |
| 08 땅따먹기 칸 · 10 칸 선택 | `map/MapScreen.kt` `TerritoryCard` | 적용 — 고른 칸의 크루 · 표시 수, 없으면 최근 14일 우리 크루 땅 · 순위 |
| 09 땅따먹기 동네 | — | **없음** — 서버가 동네 묶음을 주지 않는다 |
| 11 내크루 · 27 가입신청 시트 | `CommunityScreen.kt` `CrewTab` · `TogetherHome.kt` `CrewApplySheet` | 적용 |
| 12 크루상세 | `community/crew/CrewDetailScreen.kt` | 적용 |
| 13 모임만들기 | `community/crew/CrewCreateScreens.kt` · `CrewUi.kt` | 적용 — 가입 방식 세그먼트(체크 없음) · 대표 사진 줄 |
| 14 ~ 16 러닝코스 · 17 코스기록순위 시트 | `walk/CourseHubScreen.kt` | 적용 — 미리보기는 실제 경로 |
| 18 · 19 글쓰기 | `community/PostComposeScreen.kt` · `UtilityParts.kt` `FormField` | 적용 |
| 20 홈 | `home/HomeScreen.kt` | 적용 — 아치 · 신발 전시장 없음, 시작 버튼 빛번짐 없음 |
| 21 · 28 일시정지 · 22 길게 눌러 종료 · 26 달리는 중 | `walk/FreeRunScreens.kt` `FreeRunLive` | 적용 — 자유 러닝 |
| 23 · 29 러닝 완료 | `walk/FreeRunScreens.kt` `FreeRunResult` | 적용 — 자유 러닝 |
| 24 신발 | `customize/CustomizeScreen.kt` `MyShoesReady` | 적용 — 카드에 있는 등급 · 번호 · 레벨은 아래에서 다시 쓰지 않는다 |
| 25 내정보 | `profile/ProfileHome.kt` | 적용 — 아바타 = 올린 사진 또는 착용 신발(사람 아이콘 · 이니셜 없음) |

화면 밖 공통: 주 버튼 끝 원 화살표(다음으로 넘어가는 주 버튼만) · 버튼 상태 5개 · 작은 버튼 · 세그먼트 · 글자 탭 · 사진 카드 · LIVE 한 줄 · 묶음 목록 · 숫자 줄 ·
숫자 조절 · 시트 틀 · 누름(0.97배 + 어둡게 10% + 스프링, 물결 없음). 크루 달리기 · 챌린지 · 코스 · 다이어트 · 설정 · 채팅 같은 시안 밖 화면은 부품과 색만 따라간다.

## 시안과 다른 점

- **테마 기본값 = 기기 설정 따르기**(전달본 "시스템 다크 모드를 따라 자동으로 바뀝니다"). 2026-10-21 사용자 결정 "밝은 테마를 기본으로"를 대신한다 —
  테마를 고른 적 없는 사람만 바뀌고, 고른 사람은 그대로다. 되돌리려면 `ThemeMode.of` 기본값 · `AppTheme` 첫 값 한 줄씩이다.
- **러닝 지도(다크블루)** — 러닝 화면 키트(`runTone`)를 쓰는 지도(자유 러닝 · 코스 · 이야기 등)는 다크 테마에서 시안처럼 어둡게 눌렀다(밝은 타일 + 30% 막). 화이트블루는 밝은 거리 지도 그대로,
  크루 달리기 지도는 두 테마 모두 밝은 지도 그대로다(2026-10-17 "네이버지도처럼"). 다크에서도 밝게 두려면 `NavyRunTone.mapFilter = BrightMapFilter` · `mapShade = 0f`.
- **길게 눌러 종료는 자유 러닝만**(시안 범위). "러닝을 마칠까요?" 확인창은 자유 러닝에서 뺐고, 저장하지 않고 끝내기는 멈춘 뒤 러닝 정보(⋯) 안으로 옮겼다.
  챌린지 · 목표 · 코스 · 다이어트 · 크루 러닝은 예전 확인창 그대로다. 달리는 중 화면 잠금(1초 길게 풀기)은 시안의 잠금 버튼 자리다.
- **숫자 대신 쓴 값** — 시안의 값 중 앱에 없는 것은 있는 값으로 바꿨다(예시 숫자로 채우지 않는다):
  신발 탭 발행 수 · 누적 거리 → 내구도 · 효율 · 착화감, 크루 상세 모임 수 → 이번 주 km, 내 정보 최장 거리는 폰 기록에서 센다(무효 제외).
- **땅따먹기 동네(09) · 칸 안 거리 막대 없음** — 서버가 주지 않는 값이다.
- **크루 가입 신청 한 줄 소개 칸 없음** — 가입 API 가 소개 글을 받지 않는다.
- **날씨 효과는 비 · 눈만** — 앱이 이미 받는 Open-Meteo 날씨 코드로 고른다(새 요청 없음). 미세먼지 효과는 대기질 값을 받지 않아 쓰지 않는다.
- **자주 달리는 곳** = 최근 30번 러닝의 출발 자리에서 가장 가까운 장소를 센 것(`RunBackground.frequent`). 기록이 없으면 "달리기 좋은 곳" 목록이다.
- **신발 이름 바꾸기**는 홈에서 신발 탭 연필로 옮겼다(홈에 신발 줄이 없어졌다 — `ShoeRenameSheet`).
- **움직임 일부만** — 누름 · 숫자 올라가기 · 바뀔 때 튀기(홈 걸음) · 참여 완료 올라오기 · 신발 카드 기울기 · 길게 눌러 종료(1초, 짧게 누르면 안내)는 넣었다.
  번개 카드 → 상세의 공유 요소 전환(`SharedTransitionLayout` 550ms) · 알림 내려오기 · 토스트 위치는 아직이다(남은 일).
- **색 대비** — 옛 팔레트의 라이트 보조 글자 · 힌트(`silver` #68686D · `slate` #838389)와 다크 `voltSoft`(#7DA6FF)는 시안 값보다 한 단계 진하게 / 밝게 뒀다.
  시안 값(#848489 등)은 흰 카드에서 3.7:1 이라 작은 글자 기준 4.5:1(`PaletteContrastTest`)에 못 미친다. 새 부품(`StepUpUi.colors`)은 시안 값 그대로다.
- **데이터 읽기 둘을 더했다**(쓰기 · 서버 호출 없음): 최장 거리(`observeLongestMetersFor`) · 최근 러닝 출발 자리(`HomeViewModel.runStarts`). 날씨 응답에서 WMO 코드를 함께 읽는다.

## 확인

- 로컬: `:app:compileDebugKotlin` · `:app:compileDebugAndroidTestKotlin` · `:app:testDebugUnitTest` · `:app:lintDebug` · `:app:assembleQa` · `:app:assembleRelease` ·
  `scripts/check-strings.py` · `scripts/check-coaching-twin.py` · `tools/check_design_contract.py` · `tools/check_experience_assets.py`.
- `tools/check_design_contract.py` 는 새 공용 머리(`NavHeader` · `BackButton`)와 예전 버튼의 공용 주 · 보조 버튼 위임을 인정하게 고쳤다.
  화면 파일이 뒤로 화살표(`ArrowBack`)를 직접 그리거나 예전 버튼이 크기를 따로 정하면 그대로 막는다(일부러 넣어 보아 걸리는 것을 확인).
- 화면은 Robolectric 그림으로 시안과 나란히 봤다(홈 · 신발 · 내 정보 · 달리는 중 · 일시정지 · 완료 · 번개 상세 · 참여 완료 · 내 크루). Robolectric 은 글자 줄 높이를 무시해
  숫자 상자가 실제보다 높게 그려진다 — 실제 기기 모습은 PR 의 Experience QA 캡처로 본다.
- 기기 테스트는 바뀐 태그 · 흐름에 맞췄다(홈 `home-*` · 신발 카드 `shoe-card-*` · 길게 눌러 종료 `run-finish` 길게 누름 · 내 정보 순서 · 글쓰기 숫자 조절 등).
  이 환경에는 에뮬레이터가 없어 기기 테스트는 CI 에서만 돈다.
- HANDOFF 1-7 의 "에뮬레이터 1080×2340 캡처와 4dp 이내" 맞대기는 CI 캡처가 나온 뒤에 한다.
