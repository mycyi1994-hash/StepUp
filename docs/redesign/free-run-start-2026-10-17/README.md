# 자유러닝 시작 플로 — 신발 점검 → 3·2·1·0 → StepUP! → 지도 펼침 (2026-10-17 전달본)

사용자 지시: "이거 확인하고 진행시켜"(2026-10-16, `StepUp-free-run-Claude-handoff.zip`). 원본은 [source/](source/) —
[HANDOFF.txt](source/HANDOFF.txt) · [motion-spec.json](source/motion-spec.json) · [CLAUDE-PROMPT.txt](source/CLAUDE-PROMPT.txt) ·
[VALIDATION.txt](source/VALIDATION.txt) · 동작 참고 시안 [reference.html](source/reference.html) · 원본 [source/preview.html](source/source/preview.html).
`reference-fragment.html` 은 `source/build.mjs` 로 다시 만들 수 있어 넣지 않았다.

## 흐름

```
자유러닝 시작 → 신발 점검 → (출발 확인) → 권한 · 위치 기능 → 3 → 2 → 1 → 0 → StepUP! → 가운데에서 지도가 펼쳐짐 → 기존 자유 러닝 화면
```

| 상태 | 앱 | 파일 |
|---|---|---|
| IDLE | 기존 자유 러닝 화면(시작 전) | `FreeRunScreens.kt` `FreeRunLive(Idle)` |
| SHOE_CHECK | 가운데 창 "출발 전, 신발을 확인해요" | `FreeRunStart.kt` `FreeRunShoeCheck` · `FreeRunShoeCheckContent` |
| PREPARING | 점검창 안에서 권한 · 위치 기능을 확인. 모자라면 기존 안내(권한 카드 · E07)를 지난다 — 새 화면은 없다 | `WalkScreen.kt`(신발 점검의 출발) · `RunPermissionFlow.kt` `readRunPermissionState` |
| COUNTDOWN | 네이비 · 흰 숫자 · 도는 파란 원호. 숫자와 원호만 | `FreeRunStart.kt` `FreeRunStartStage` |
| LAUNCH | 숫자 · 원호가 빠지는 300ms 동안 StepUP! 이 함께 들어온다(같은 시작점) | 〃 |
| REVEAL | **러닝 기록 시작** → 실제로 켜진 것을 본 뒤 가운데에서 지도가 펼쳐진다 | `FreeRunStart.kt` `FreeRunStartSequence` |
| RUNNING | 기존 자유 러닝 화면(달리는 중) — 레이아웃 · 수치 · 지도는 그대로 | `FreeRunLive(Live)` |

시간표 · 곡선 · 레이어는 [motion-spec.json](source/motion-spec.json) 그대로다 — 수는 `FreeRunStartMotion.kt` 한 곳에 있고 `FreeRunStartMotionTest` 가 본다.

| ms | 일 |
|---|---|
| 0 · 1,000 · 2,000 · 3,000 | 3 · 2 · 1 · 0 (숫자마다 160ms 진입: 투명도 .6→1 · 아래 6→0 · .96→1) |
| 0 → 3,500 | 원호 0→1260도 한 번(1초에 한 바퀴, 숫자마다 다시 돌지 않는다) |
| 3,180 | 숫자 · 원호 퇴장(300ms) = StepUP! 진입(300ms, 72% 지점 오른쪽 3 · 1.025 배) — 같은 시작점 |
| 3,760 | 지도 펼침 · **러닝 기록 시작** |
| +0 · +35 · +70 | 선행 파랑 #5287FF(500ms) · 강조 파랑 #1450FF(540ms) · 러닝 화면(580ms), 곡선 (.77,0,.175,1), 반경 ceil(hypot(w/2, h/2)) + 12 |
| +120 → +420 | StepUP! 퇴장(투명 · 1.045 배) — 펼침이 로고를 덮는다 |
| 4,410 | 러닝 화면 전체 |

## 실제 데이터 연결

- **신발** — 신고 있는 신발 `WalkViewModel.equipped`(← `SneakerRepository.equipped`).
  - 그림: `ShoeArtThumbnail(sample = ShoeArtFull)`(그 모델의 그림, 보이는 부분만 맞춘다).
  - 등급: `Sneaker.tier` + `ShoeTier.label()`(6갈래).
  - 레벨: `Sneaker.level`.
  - 남은 내구도: `durabilityPercent()`(서버 `durability_pts`) — 러닝 · 홈의 작은 칸과 같은 "%".
  - 시안의 레어 · Lv.3 · 92% 는 넣지 않았다.
- **권한** — `readRunPermissionState()` + `RunPermissionRules.next()`(기존 규칙 그대로: 걸음 · 정확한 위치 · 알림 차례).
- **위치 기능** — `locationServicesOn()`. 꺼져 있으면 기존 E07(위치 설정 열기).
- **러닝 시작** — `WalkSessionService.start(context)`. 시작 성공은 `WalkSessionService.state.isActive` 로 본다.
- **시작 실패** — `WalkSessionService.startRefusals`(새로 더한 신호), 서비스를 켜지 못한 예외, 5초 안에 켜지지 않음.
  - 끝나지 않은 러닝은 기존처럼 앱이 "이어 달리기 / 여기서 끝내기"를 묻는다(`recovery`).
  - 저장 공간을 못 쓰면 기존처럼 서비스가 알린다.
- **지도** — 펼침 아래는 기존 자유 러닝 화면의 지도(`freeLiveMap` → `LiveRouteMap`)다.
  - 3-2-1 동안 지금 자리를 미리 찾는다(`rememberCurrentLocation` — 구글 위치 서비스, 러닝 서비스가 이어 받는다).
  - 그 둘레 지도 타일을 지도 캐시에 받아 둔다(`prefetchRunMapTiles` → `MapTiles.load`).
  - 어느 것도 기다리지 않는다. 못 받은 칸은 지도가 늘 하던 대로 다시 받는다(그동안 의사 도로망).
- **로고** — 전달본의 흰색 워드마크 `res/drawable-nodpi/logo_wordmark_white.png`(900×156, 원본 그대로). 시안의 굵은 이탤릭 "!"는 2026-10-18 뺐다(사용자 "UP 옆 흰 일직선 줄 — 없애" — 로고보다 길어 세로 줄로 보였다).

## 세션 시작 이벤트의 정확한 위치

`app/src/main/java/com/stepup/android/ui/screens/walk/FreeRunStart.kt` `FreeRunStartSequence`:

- 시간표 `LaunchedEffect` 안의 `clock >= FreeRunStartMotion.REVEAL_AT`(3,760ms)에서 `onStartSession()` 을 한 번 부른다.
- 부르기 전에 `requestedAt` 을 적는다. 이 값은 회전 · 재생성에도 남는다(`rememberSaveable`). 그래서 다시 부르지 않는다.
- `WalkScreen.kt` 가 넘기는 `onStartSession`:
  - 이미 달리는 중이면 `true`.
  - 아니면 `WalkSessionService.start(context)`. 예외가 나면 `false` → 연출을 거둔다.
- 서비스도 이미 켜져 있거나 켜는 중이면 시작을 무시한다(기존).
- 펼침은 `WalkSessionService.state.isActive` 가 참이 된 프레임부터 시작한다.
- 카운트다운 · 로고 시간은 러닝 시간에 들어가지 않는다 — 러닝 서비스가 켜진 순간부터 잰다(기존).

## 정리 · 뒤로 가기 · 백그라운드

- 시퀀스의 주인은 하나다(`FreeRunStartSequence` 의 `LaunchedEffect` 하나). 시간은 화면 프레임 시계에서 프레임마다 더한다(한 프레임 최대 100ms).
- **앱이 화면에 없으면 시간이 흐르지 않는다.**
  - 돌아오면 그 자리부터 잇는다(기존 3-2-1 과 같은 규칙).
  - 백그라운드에서 러닝 서비스를 켜지 않는다(안드로이드 12+ 가 막는다).
- **기록 전 시스템 뒤로 가기** — 시작 취소. 아무것도 시작하지 않고, 이 러닝을 연 화면으로 돌아간다(기존 R01).
- **시작 요청 뒤** — 달리는 중으로 본다.
  - 켜지는 사이(수십 ms)의 뒤로 가기는 먹는다.
  - 켜진 뒤로는 기존 러닝 화면의 규칙(달리는 중이면 일시정지)이다.
- **이미 달리는 러닝으로 돌아오면**(되살린 러닝 · 알림 · 다른 화면에서 켠 러닝) 카운트다운을 다시 보이지 않는다.
- 연출 동안 아래 화면은 눌리지 않는다.
- 점검창 · 카운트다운 · 연출을 화면처럼 쌓지 않는다. 러닝 화면 한 곳의 겹으로 그린다.

## 모션 감소

시스템의 애니메이션 끔 또는 앱 설정 › 동작 줄이기.

- 숫자와 차례는 같다.
- 원호 회전 · 숫자 이동/확대 · 로고 이동/확대 · 원형 확장은 없다. 시안처럼 도는 원호도 그리지 않는다.
- 숫자 · 원호 → 로고는 같은 시작점에서 200ms 교차 페이드다.
- 지도도 200ms 교차 페이드로 드러난다.
- 기록 시작 시점(펼침 진입)은 같다.

## 시안과 다른 점 · 고른 것

- **숫자 서체** — 시안의 Bahnschrift 는 앱에 없다.
  - 앱 숫자 서체(`StepUpNumbers` — Pretendard ExtraBold)를 이탤릭(합성)으로 썼다.
  - 크기 154/142, 자간 −6, 광학 중심 보정(왼쪽 8 · 아래 3.5)은 시안 그대로다.
- **"!"** — Arial Black 대신 Pretendard ExtraBold 이탤릭 + 시안의 skewX(−5°). 워드마크는 그림 그대로다(글꼴로 다시 쓰지 않았다).
- **크기**
  - 원 지름은 폭 380dp 미만이면 224, 아니면 244다. 화면이 좁거나 낮으면 더 줄인다(폭 − 48 · 높이의 50%). 숫자도 같은 비율로 줄인다.
  - 숫자는 글자 크기 설정을 따르지 않는다(dp) — 큰 글씨에서 원 밖으로 넘치지 않게.
- **신발 점검**
  - 이름 줄은 시안 글("장착 중인 러닝화")에 실제 등급 · 레벨을 붙였다.
  - 신발을 신지 않았으면 기존 문구("신고 있는 신발이 없어요 / 신발 탭에서…")를 보이고 "러닝 시작"으로 잇는다. 지금처럼 신발 없이도 달릴 수 있다.
  - 뒷면을 눌러도 닫히지 않는다(시안에 없음). 닫기는 X · 돌아가기 · 뒤로다.
- **신발 사용 가능 여부** — 앱에 막는 규칙이 없어 새로 만들지 않았다. 내구도가 0 이어도 지금처럼 달릴 수 있고, 점검창은 실제 % 를 보인다.
- **이전 자유 러닝 3-2-1(2026-10-14 v2)을 대신한다.**
  - 대신한 것: 지도 · 고리 숫자 · 기록 판 · 바로 시작 | 취소(`FreeRunCountdownStage` · `run_free_go_now`).
  - 화면을 눌러 건너뛰기도 없다(시퀀스가 정해진 차례로 간다).
  - 챌린지 · 코스 · 다이어트 러닝의 3-2-1(R01)은 그대로다.

## 확인

- 로컬:
  - `./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug`
  - `scripts/check-strings.py` · `tools/check_experience_assets.py`
- 단위 테스트 `FreeRunStartMotionTest`(1ms 간격으로 훑는다):
  - 3·2·1·0 시각.
  - 원호가 숫자마다 끊기지 않는다.
  - 0→로고 300ms 동안 둘 다 안 보이는 프레임이 없다(투명도 합 ≥ 0.99, 모션 감소는 정확히 1).
  - 로고 120ms 유지 뒤 퇴장.
  - 펼침 층의 순서(선행 ≥ 강조 ≥ 화면).
  - 마스크가 네 모서리를 덮는다.
- 기기 검사 `RunStartFlowTest`(CI 에뮬레이터):
  - 시작은 펼침 진입에서 한 번이다(연타 · 다시 그리기에도).
  - 기록 전 뒤로 가기는 시작하지 않는다.
  - 시작이 거절되면 펼치지 않는다.
  - 달리는 러닝으로 돌아오면 카운트다운이 없다.
  - 신발 점검: 실제 등급 · 레벨 · 내구도, 출발 연타는 한 번, 돌아가기 · 닫기는 시작하지 않음, 신발 없음.
- 시안 캡처 `RunJourneyDesignTest#states` F01~F07(Experience QA 의 `qa-review.zip`):
  - 신발 점검.
  - 3 · 0.
  - 0→로고 교차 중간(120ms).
  - StepUP!.
  - 펼침 중간(300ms).
  - 모션 감소 교차.

## 하지 못한 검증 · 남은 제한

- **실제 기기에서 첫 실행 · 반복 실행, 느린 재생 점검은 하지 않았다** — 이 작업 환경에는 기기가 없다.
  - 0→StepUP! 구간은 단위 테스트(프레임 단위 수치)와 에뮬레이터 고정 시각 캡처로만 보았다.
  - 전환 영상은 없다.
- 60/120Hz 프레임 시간은 재지 않았다.
- 펼침 직후 러닝 서비스의 첫 위치가 오기 전 몇십 ms 는 지도 대신 "내 위치를 찾는 중" 자리일 수 있다.
  - 3-2-1 동안 구글 위치 서비스를 켜 두어 보통은 서비스가 바로 마지막 위치를 받는다.
- 지도 타일 미리 받기는 네트워크 상태에 달렸다. 못 받으면 기존처럼 의사 도로망 위에 타일이 차례로 깔린다.
