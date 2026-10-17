# 러닝 중 오래 서 있음 경고 (2026-10-17)

사용자 결정: "너무 오래 멈췄을 때(30초 이상) 경고도 떠야" → 고른 뜻은 "러너가 30초 넘게 서 있음".

## 기획

| 항목 | 정한 것 |
|---|---|
| 언제 | 기록 중(시작함 · 일시정지 아님 · 저장 중 아님)인데 **30초 넘게 움직이지 않았을 때** |
| 움직임 | 걸음이 늘었거나, 경로 점이 새로 생겼을 때(8m 이상 이동). 시작 · 다시 시작한 때부터 센다 |
| 왜 둘 다 보나 | 러닝머신 · 실내는 위치가 그대로여도 걸음이 는다. 신호가 약하면 위치가 안 와도 걸음은 는다. 하나만 보면 달리는 사람에게 잘못 뜬다 |
| 자유 · 크루 러닝 | 지도 위 띠 "30초 넘게 멈춰 있어요" + 알약 "일시정지" |
| 챌린지 · 코스 · 다이어트 | 안내 띠 "30초 넘게 멈춰 있어요 — 쉬는 중이면 일시정지해 주세요. 그동안 시간은 계속 기록돼요." + "일시정지" |
| 소리 · 진동 | 뜰 때 한 번(앱이 화면에 있을 때, 앱의 소리 · 진동 설정을 따른다). 계속 서 있어도 다시 울리지 않는다 |
| 알림 줄 | "30초 넘게 멈춰 있어요 · 쉬는 중이면 일시정지해 주세요" — 화면이 꺼져 있어도 보인다. 움직이면 걸음 수로 돌아온다 |
| 사라질 때 | 다시 움직이면 바로(걸음 · 경로 점). 일시정지 · 마치기 · 저장 중에는 띠가 없다 |
| 띠 차례(자유 · 크루) | (과속 — [run-overspeed-stop](run-overspeed-stop-2026-10-17.md)) > 위치 꺼짐 > 측정 무효 > 속도 오류 > **멈춰 있음** > 신호 약함 > 대략적 위치 > 일시정지 |

하지 않는 것:

- 저절로 일시정지하지 않는다. 신호등 · 물 마시기처럼 짧게 서는 일이 많고, 기록을 사용자 몰래 바꾸지 않는다.
- 기록 · 거리 · 보상 · 부정 판정 규칙은 그대로다. 서버에 보내지 않고 저장본에 남기지 않는다(화면 표시용).

## 코드

- 규칙: [`domain/RunStill.kt`](../app/src/main/java/com/stepup/android/domain/RunStill.kt) — `STILL_MS = 30,000`, `isStill(recording, sinceMoveMs)`.
- 서비스: [`WalkSessionService`](../app/src/main/java/com/stepup/android/service/WalkSessionService.kt)
  - `lastMoveElapsed`: 걸음이 늘 때 · 경로 점이 생길 때 · 시작 · 다시 시작할 때 고친다.
  - 1초 타이머가 `WalkSessionState.still` 을 정한다. 바뀔 때 알림 글을 다시 쓴다.
  - 일시정지 · 저장 시작 때 `still = false`.
- 화면:
  - [`FreeRunScreens.kt`](../app/src/main/java/com/stepup/android/ui/screens/walk/FreeRunScreens.kt) `FreeBand.Still` — 띠 · 알약.
  - [`WalkScreen.kt`](../app/src/main/java/com/stepup/android/ui/screens/walk/WalkScreen.kt) — 알약 → 일시정지(자유 · 크루), 챌린지 · 코스 · 다이어트 안내 띠, 뜰 때 소리 · 진동 한 번.
- 글: `run_still_title` · `run_still_body` · `notification_run_still`(4개 언어).

## 검사

- `RunStillTest` — 30초 경계, 일시정지 · 저장 중에는 알리지 않음.
- `FreeRunUiTest` — 띠 차례(멈춰 있음이 신호 약함 · 대략 위치보다 앞, 위치 꺼짐 · 무효 · 속도 오류보다 뒤, 일시정지 중 없음).
- `RunLocationStateTest.standingStillIsWarned` — 띠 · 글 · 일시정지 알약, 캡처 `form-checks/run-still.png`.
- 실제 기기에서 신호등 대기 같은 상황은 아직 확인하지 못했다.
