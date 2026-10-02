# 러닝 전체 리메이크 · 신발 색감 (2026-10-02 전달본)

사용자가 준 패키지 「StepUp 러닝 전체 리메이크와 신발 색감 적용 지시서」(러닝 109장 + 신발 색감 참고 1장)를 앱에 적용한 기록이다.
지시서 · 화면별 동작표 · 검수 목록 원문은 [source/](source/)에 그대로 두었다. 원본 PNG 109장(약 148MB)은 용량 때문에 레포에 넣지 않았다
— 원문 링크(`../01-screens/*.png`)는 전달 패키지 안에서만 열린다.

- 새 화면 · 라우트를 109개 만들지 않았다. 각 장면은 기존 화면 · 부품 · 시트 · 상태 분기 중 하나다(아래 대응표).
- 글자 · 버튼 · 숫자 · 상태 표시줄은 모두 Compose. 지도는 실제 타일 · 실제 좌표 · 실제 경로만 그리고, 자리를 모르면 지도를 꾸미지 않는다.
- 없는 서버 API 를 흉내 내지 않았다 — [남은 연동](#남은-연동)에 적었다.
- 머리글 · 하단 탭 다섯(러닝 · 신발 · 뽑기 · 커뮤니티 · 내 정보) · 라우팅 정책은 그대로 쓴다. SUP 알약은 HOME · U01 에만.
- 신발 탭은 구조(내 신발 · 신발 보관함 · 능력치 · 정렬 · 필터 · 고른 것과 신은 것의 구분)를 그대로 두고 색감과 조명만 바꿨다.

## 단계와 커밋

| 단계 | 내용 | 커밋 |
|---|---|---|
| 1 | 공통 남색 스타일(`ui/components/RunStyle.kt`) · HOME · U01 · 자유 러닝(R01~R07 · E01~E11 · S01) | `38b42c9` |
| 2 | 챌린지 · 추천 코스 · 코스 허브 · 다이어트(U02~U06 · C01~C05 · K01~K18 · D01~D13 · S02) | `60551d7` |
| 3 | 내 러닝 기록 · 통계 · 상세 · 경로(H01~H16) · 권한 안내(P01~P05 · L01~L03) · 휴대폰 위치 꺼짐(E07) | `572276e` |
| 4 | 크루 달리기(CR03~CR21) | `b718954` |
| 5 | 신발 탭 색감 · 조명 | `09d98bb` |
| 마무리 | 기기 검사 실패 고침(메인 스레드 이동 · GPS 대략 위치 문구 · 검사 갱신) · L03 · 캡처 보강 · 이 문서 · Kotlin 컴파일 데몬 힙 5GB | `d55914b` · `90cbfe5` · `eaf9f52` |

## 지킨 동작

| 규칙 | 어디서 | 검사 |
|---|---|---|
| 러닝 중 시스템 뒤로 가기 = 일시정지(이미 멈춰 있으면 마칠지 묻기) | `WalkScreen.kt` BackHandler | `ChromeNavigationTest`(멈춘 러닝 → R04). 달리는 중 → 일시정지는 서비스가 필요해 기기 검사 없음 |
| 3-2-1 취소 · 뒤로 가기는 기록을 만들지 않음 | `RunStartOverlays.kt` RunCountdown | `RunStartFlowTest` |
| 위치 없이 기록(R02_TIME)은 거리 · 페이스를 '—'(재지 않음) | `WalkScreen.kt` timeOnly | `RunJourneyDesignTest#screens` |
| GPS 가 끊긴 구간을 직선 거리로 더하지 않음(앱) | `WalkSessionService` — 끊긴 뒤 첫 점은 새 구간 · 지도는 `domain/RunTrack.kt` segmentBreaks | `RunGeometryTest`(지도 구간) · L04 캡처. 서비스 거리 계산은 실외 GPS 로 확인하지 않음 |
| 보상 상태: 저장 → 확인 중(예상) → 서버 확인 금액 / 제외 / 미지급 — 서버 확인 전 "적립 완료" 없음 | `WalkScreen.kt` finishReward · `RunLive.kt` Settle | E10 · E11 · E08 · E09 |
| 공유: 경로 포함은 처음에 꺼짐 — 끄면 그림에 지도 · 길이 없음 | `RunLive.kt` RunSharePreviewContent · `CrewRunScreens.kt` | E05 · CR21 |
| 삭제는 누를 때만 · 실패하면 기록이 남음 | `WalkScreen.kt` · `RunRecordScreen.kt` | R06 · H14 · H16 |
| 기록 합계는 고른 기간이 아니라 전체 기간에서 | `RecordsViewModels.kt` | `RecordsDesignTest` |
| 통계는 사용자 시간대 기준 | `domain/RunRecords.kt` (ZoneId) | `RunRecordsTest` |
| 크루: 미리보기에 `PartyApi.open` 을 부르지 않음 · 위치 공유는 처음에 꺼짐 · 출발은 서버 시각과 명단 · 늦은 합류 없음 · 알리기 · 공유는 미리보기 뒤 직접 보낼 때만 | `CrewRunScreens.kt` · `PartyLobbyScreen.kt` | `RunJourneyDesignTest#crew` |
| 다이어트: 휴대폰 숫자 자판 · 루틴 15분 | `DietScreens.kt` · `domain/DietPlan` | `RunJourneyDesignTest#diet` |

## 시안과 다른 점

- **COURSES · LOBBY · FLASH_LOBBY · RUN_CREW** 는 러닝 화면처럼 하단 탭 없는 Focus 머리글이다(시안에 탭이 없다).
- **내 러닝 기록 · 통계(H01~H10 · H15)** 는 기존 머리글 · 하단 탭 정책(Detail — 하단 탭 보임)을 그대로 쓴다. 시안에는 하단 탭이 없다.
  지난 러닝 상세 · 경로(H11~H14 · H16)는 기존대로 Form(하단 탭 없음).
- **다이어트 경험 고르기**: 시안처럼 세 줄이되 줄마다 짧은 설명을 남겼다(루틴을 고르는 기준이라 지우지 않음).
- **K15 달린 코스 저장**: "저장 안 함"을 남겼다(저장을 강요하지 않는다).
- **위치 없이 기록한 결과**: 거리 칸에 걸음으로 셈한 값을 그 근거와 함께 적는다(러닝 중에는 '—').
- **기간 고르기(H03)**: 첫 기록이 있는 달부터만 보인다(빈 달을 꾸미지 않는다).
- **통계(H04~H06)**: 시안의 거리 · 횟수에 더해 시간 · 페이스와 걸음 통계로 가는 줄을 남겼다(기존 기능).
- **상세(H11)**: 러닝 종류(챌린지 · 크루) 부제는 앱이 그 종류를 알 때만 쓴다(세션에 종류가 저장되지 않는다).
- **L02**: 주 버튼이 "설정에서 위치 켜기", "시간만 기록"이 보조다(시안과 강조가 반대 — 위치를 켜는 것이 기본 행동).
- **P01**: 가운데 창에 닫기(X)가 없다 — 뒤로 가기로 닫는다.
- **신발 탭 바탕**: 예전 무대 바탕 대신 러닝과 같은 남색 RunBackdrop.
- **L03 지도**: 시안은 '지도 예시'를 깔지만 자리를 모를 때 지도를 꾸미지 않는다는 규칙에 따라 빈 틀만 둔다.

## 남은 연동

| 무엇 | 왜 필요한가 | 지금 앱 |
|---|---|---|
| 크루 보기 전용 방 조회 API | CR02(준비 중인 방 미리보기) · CR16(이미 출발한 방) | 만들지 않음 — `party_open` 은 방을 만들고 들어가며, 늦게 오면 새 방을 연다 |
| party_state 의 일정 · 목표 거리 · 모임 장소 · 코스 | CR03~CR06 의 모임 정보 줄 | 서버 값이 없어 그 줄을 그리지 않음 |
| 사람마다 일시정지 상태 | CR10 의 '쉬는 중' | 표시하지 않음 |
| 크루 채팅 바로가기(딥링크) | CR20 · CR21 보낸 뒤 채팅으로 | 보낸 결과만 알림 |
| 코스 추천 서버 API | U04 · K01 · K02 | 기기 코스 + 공유 게시판 코스를 거리순으로(`CourseRecommendations.near`) |
| 세션의 러닝 종류(코스 · 다이어트 · 챌린지) 저장 | H11 부제 · K06 · D13 | 챌린지는 폰의 도전 기록, 크루는 방 기록으로만 안다 |
| 서버 · attester 의 GPS 끊긴 구간 처리 | 끊긴 구간을 직선 거리로 세지 않는지 | 앱은 끊긴 구간을 더하지 않는다. 서버 쪽은 이 작업에서 확인하지 않음 |
| 다이어트 추천 서버 | D04 · D05 | 이 폰에 저장하는 진행 · 실패만 |

## 검사 · 캡처

- 로컬: `./gradlew :app:compileDebugKotlin :app:testDebugUnitTest :app:lintDebug`, `python3 scripts/check-strings.py`,
  `python3 tools/check_experience_assets.py`, `python3 tools/check_design_contract.py`.
- 기기(CI Experience QA, 에뮬레이터 pixel_2 · API 35): 아티팩트 `StepUp-experience-qa` 의 `screen-gallery/` 아래
  `run-journey/`(RunJourneyDesignTest) · `running-records/`(RecordsDesignTest) · `onboarding-v1/`(OnboardingDesignTest) ·
  `forms/`(RunStartFlowTest · RunLocationStateTest) · `my-shoes/`(MyShoesDesignTest).
- R01 · E07 · L03 · K01 · K02 · K17 · K18 · U04 · D04 · D05 는 앱에서 잠깐만 지나가거나 서버 없이는 만들 수 없는 상태라, 같은 화면 부품을
  그 상태로 그려 찍었다(`RunJourneyDesignTest#states`). R01 은 앱 안 캡처(`forms/run-countdown.png`)도 있다. K17 · K18 캡처에는 허브의 글자 탭 · 검색 줄이
  없다 — 그 둘레는 앱 안 K11 캡처에서 본다.
- CR02 · CR16 은 보기 전용 방 조회 API 가 없어 만들지 않았다(캡처 없음).
- 화면별 구현 · 기기 검증 상태는 [implementation-checklist.csv](implementation-checklist.csv). 기기 검증 칸은 같은 코드의 CI 캡처를 보고 확인한 줄만 채웠다.
- **기기 확인(2026-10-02)**: Experience QA run `37068564191`(커밋 `eaf9f52`, 에뮬레이터 pixel_2 · API 35 · ko-KR) 통과. 캡처 108장(CR02 · CR16 제외)을
  시안과 나란히 놓고 보았다. CI 기기는 로그인 전이라 K11 · K12 · K16 은 로그인 필요 · 올릴 내 코스 없음 상태로 찍힌다(그 상태 그대로가 맞다).
  R01 · CR08 상태 캡처에는 지도 타일이 아직 그려지지 않아 바탕만 보인다.

## 화면 대응표

| ID | 코드 | route | 상태 | 기기 캡처 | 남은 연동 |
|---|---|---|---|---|---|
| HOME | `ui/screens/home/HomeScreen.kt` | `home (Screen.Run)` | 러닝 탭 첫 화면 · 신고 있는 신발 · SUP 알약 | `run-journey/HOME.png` |  |
| U01 | `ui/screens/walk/RunFlowScreens.kt (RunStartMenuContent)` | `run-start` | HOME 러닝 시작 · 다섯 버튼 · SUP 알약 | `run-journey/U01.png` |  |
| U02 | `ui/screens/walk/RunFlowScreens.kt (RunGoalsScreen)` | `run-goals` | U01 챌린지 | `run-journey/U02.png` |  |
| U03 | `ui/screens/walk/RunLive.kt (RunLiveContent · LiveHero.Clock)` | `run` | RunPlan.Goal(10분) 진행 중 | `run-journey/U03.png` |  |
| U04 | `ui/screens/walk/RunCourseScreens.kt (CourseRecUi.Found)` | `run-course` | 가까운 코스를 찾음(1/2) | `run-journey/U04.png` | 코스 추천 서버 API 없음 — 기기 코스 + 공유 게시판 코스 중 거리순(CourseRecommendations.near) |
| U05 | `ui/screens/walk/DietScreens.kt (DietInputContent)` | `run-diet` | 몸 정보 · 경험 첫 입력 | `run-journey/U05.png` |  |
| U06 | `ui/screens/walk/DietScreens.kt (DietPlanScreen)` | `run-diet/plan` | 저장된 경험으로 15분 루틴 | `run-journey/U06.png` |  |
| R01 | `ui/screens/walk/RunStartOverlays.kt (RunCountdown · RunCountdownStage)` | `run?start=true` | 권한 준비 후 3-2-1 · 취소/뒤로는 기록 없음 | `run-journey/R01.png` · `forms/run-countdown.png` |  |
| R02 | `ui/screens/walk/RunLive.kt (RunLiveContent)` | `run` | 자유 러닝 중 · 실제 지도 · GPS 연결됨 | `run-journey/R02.png` |  |
| R02_TIME | `ui/screens/walk/RunLive.kt (NoLocationMap) · WalkScreen.kt(timeOnly)` | `run` | 위치 없이 기록 — 거리 · 페이스 '—' | `run-journey/R02_TIME.png` |  |
| R03 | `ui/screens/walk/RunLive.kt (paused)` | `run` | 일시정지 · 뒤로 가기도 일시정지 | `run-journey/R03.png` |  |
| R04 | `ui/screens/walk/WalkScreen.kt (run-end-dialog)` | `run` | 마칠까요? | `run-journey/R04.png` |  |
| R05 | `ui/screens/walk/RunLive.kt (RunResultContent)` | `run` | 저장한 결과 · 보상 확인 중 | `run-journey/R05.png` |  |
| R06 | `ui/screens/walk/WalkScreen.kt (run-delete-sheet)` | `run` | 결과의 삭제 — 누르기 전에는 지우지 않음 | `run-journey/R06.png` |  |
| R07 | `ui/screens/walk/WalkScreen.kt (run-discard-dialog)` | `run` | 기록 없이 끝낼까요? | `run-journey/R07.png` |  |
| C01 | `ui/screens/walk/RunLive.kt (run-goal-reached)` | `run` | 챌린지 목표 도달 · 더 달리기/마치기 | `run-journey/C01.png` |  |
| C02 | `ui/screens/walk/RunLive.kt (RunResultContent · run-result-note)` | `run` | 목표 전 종료 후 저장 | `run-journey/C02.png` |  |
| C03 | `ui/screens/walk/RunFlowScreens.kt (RunGoalHistoryScreen)` | `run-goals/history` | 지난 도전(폰에 남긴 도전 기록) | `run-journey/C03.png` |  |
| C04 | `ui/screens/walk/RunLive.kt (LiveHero.Distance)` | `run` | RunPlan.Goal(1km) 진행 중 | `run-journey/C04.png` |  |
| C05 | `ui/screens/walk/RunLive.kt (LiveHero.Distance)` | `run` | RunPlan.Goal(3km) 진행 중 | `run-journey/C05.png` |  |
| L01 | `ui/screens/walk/RunPermissionFlow.kt (LocationRationale · Sheet)` | `run-start 위` | 위치 쓰기 전 안내 | `onboarding-v1/s16-location-rationale.png` |  |
| L02 | `ui/screens/walk/RunPermissionFlow.kt (WithoutLocation · Page)` | `run-start 위` | 앱 위치 권한 없음 — 설정 열기 · 시간만 기록 | `onboarding-v1/s18-without-location.png` |  |
| L03 | `ui/screens/walk/RunCourseScreens.kt (CourseRecUi.Locating)` | `run-course` | 추천 코스 전 지금 자리 확인 중 — 자리를 모르면 지도에 자리 없음 | `run-journey/L03.png` |  |
| L04 | `ui/screens/walk/WalkScreen.kt (run-gps-lost)` | `run` | 러닝 중 위치 신호 끊김 — 시간은 계속 | `run-journey/L04.png` | 서버 · attester 가 GPS 끊긴 구간을 직선 거리로 세지 않는지 확인 필요 |
| K01 | `ui/screens/walk/RunCourseScreens.kt (CourseRecUi.Finding)` | `run-course` | 자리를 알고 코스를 찾는 중 | `run-journey/K01.png` | 코스 추천 서버 API 없음 |
| K02 | `ui/screens/walk/RunCourseScreens.kt (CourseRecUi.Found · 다음 코스)` | `run-course` | 다른 코스(2/2) | `run-journey/K02.png` | 코스 추천 서버 API 없음 |
| K03 | `ui/screens/walk/RunCourseScreens.kt (CourseRecUi.None)` | `run-course` | 가까운 코스 없음 — 다시 찾기 · 자유 러닝 | `run-journey/K03.png` |  |
| K04 | `ui/screens/walk/RunLive.kt (코스 선 · run-goal-bar)` | `run` | 고른 코스로 달리는 중 | `run-journey/K04.png` |  |
| K05 | `ui/screens/walk/RunLive.kt (run-course-off-map)` | `run` | 코스 선에서 60m · 15초 넘게 벗어남 | `run-journey/K05.png` |  |
| K06 | `ui/screens/walk/RunLive.kt (RunResultContent · 코스)` | `run` | 코스 러닝 결과 | `run-journey/K06.png` | 세션에 러닝 종류(코스 · 다이어트)가 저장되지 않음 |
| K07 | `ui/screens/walk/RunLive.kt (run-full-map)` | `run` | 러닝 중 전체 지도 | `run-journey/K07.png` |  |
| K08 | `ui/screens/walk/RunLive.kt (run-course-free-ok)` | `run` | 코스 안내 끄고 자유 러닝으로 | `run-journey/K08.png` |  |
| D01 | `ui/screens/walk/DietScreens.kt (BodyField · 휴대폰 숫자 자판)` | `run-diet` | 입력칸에 적는 중 — 완료 | `run-journey/D01.png` |  |
| D02 | `ui/screens/walk/DietScreens.kt (diet-input-error)` | `run-diet` | 비었거나 범위 밖 | `run-journey/D02.png` |  |
| D03 | `ui/screens/walk/DietScreens.kt (diet-leave-dialog)` | `run-diet` | 적던 중 뒤로 | `run-journey/D03.png` |  |
| D04 | `ui/screens/walk/DietScreens.kt (DietPreparingContent)` | `run-diet` | 몸 정보 · 경험 저장 중 | `run-journey/D04.png` | 추천 서버 없음 — 이 폰 저장만(D04 · D05 는 로컬 저장의 진행 · 실패) |
| D05 | `ui/screens/walk/DietScreens.kt (DietFailedContent)` | `run-diet` | 저장 실패 — 입력은 그대로 | `run-journey/D05.png` | 추천 서버 없음 — 이 폰 저장만 |
| D06 | `ui/screens/walk/DietScreens.kt (editing = true)` | `run-diet/edit` | U06 의 몸 정보 수정 | `run-journey/D06.png` |  |
| D07 | `ui/screens/walk/RunLive.kt (diet · 준비 걷기)` | `run` | 루틴 0~3분 | `run-journey/D07.png` |  |
| D08 | `ui/screens/walk/RunLive.kt (diet · 러닝 구간)` | `run` | 1분 러닝 | `run-journey/D08.png` |  |
| D09 | `ui/screens/walk/RunLive.kt (diet · 걷기 구간)` | `run` | 2분 걷기 | `run-journey/D09.png` |  |
| D11 | `ui/screens/walk/RunLive.kt (diet · 마무리 걷기)` | `run` | 마지막 3분 | `run-journey/D11.png` |  |
| D10 | `ui/screens/walk/RunLive.kt (run-diet-done)` | `run` | 15분 끝 · 저장 전 | `run-journey/D10.png` |  |
| D12 | `ui/screens/walk/RunLive.kt (diet · paused)` | `run` | 다이어트 루틴 일시정지 | `run-journey/D12.png` |  |
| D13 | `ui/screens/walk/RunLive.kt (RunResultContent · 다이어트 중간 종료)` | `run` | 루틴 중간 종료 후 저장 | `run-journey/D13.png` | 세션에 러닝 종류가 저장되지 않음 |
| S01 | `ui/screens/walk/WalkScreen.kt (run-save-failed-dialog)` | `run` | 저장 실패 — 기록은 화면에 남고 같은 러닝으로 다시 저장 | `run-journey/S01.png` |  |
| S02 | `ui/screens/walk/RunLive.kt (RunResultContent · 다이어트 완료)` | `run` | 15분 루틴 저장 | `run-journey/S02.png` |  |
| H01 | `ui/screens/records/RecordsScreen.kt` | `records` | 이번 달 목록 | `running-records/01-record-list.png` |  |
| H02 | `ui/screens/records/RecordsScreen.kt (전체 기간)` | `records` | 전체 기간 — 합계도 전체 기간 | `running-records/02-all-records.png` |  |
| H03 | `ui/screens/records/RecordsScreen.kt (PeriodSheet)` | `records` | 기간 고르기 | `running-records/03-period-sheet.png` |  |
| H04 | `ui/screens/records/RecordStatsScreen.kt (주간)` | `records/stats` | 주간 통계(사용자 시간대) | `running-records/04-week-statistics.png` |  |
| H05 | `ui/screens/records/RecordStatsScreen.kt (날짜 선택)` | `records/stats` | 주간 · 날짜 고름 | `running-records/05-week-selected-day.png` |  |
| H06 | `ui/screens/records/RecordStatsScreen.kt (월간)` | `records/stats` | 월간 통계 | `running-records/06-month-statistics.png` |  |
| H07 | `ui/screens/records/RecordsScreen.kt (records-empty)` | `records` | 첫 기록 없음 | `running-records/s07-first-empty.png` |  |
| H08 | `ui/screens/records/RecordsScreen.kt (records-period-empty)` | `records` | 고른 기간에 기록 없음 | `running-records/s08-period-empty.png` |  |
| H09 | `ui/screens/records/RecordsScreen.kt (records-loading)` | `records` | 불러오는 중 | `running-records/s09-loading.png` |  |
| H10 | `ui/screens/records/RecordsScreen.kt (records-failed)` | `records` | 불러오기 실패 | `running-records/s10-load-error.png` |  |
| H11 | `ui/screens/records/RunRecordScreen.kt` | `records/run/{id}` | GPS 있는 지난 기록 | `running-records/11-run-detail.png` |  |
| H12 | `ui/screens/records/RunRouteMapScreen.kt` | `records/run/{id}/map` | 저장 경로 확대(실제 타일) | `running-records/12-route-expanded.png` |  |
| H13 | `ui/screens/records/RunRecordScreen.kt (경로 없음)` | `records/run/{id}` | 위치 없이 저장한 기록 | `running-records/13-no-gps-detail.png` |  |
| H14 | `ui/screens/records/RunRecordScreen.kt (run-delete-sheet)` | `records/run/{id}` | 지난 기록 삭제 확인 | `running-records/14-delete-confirm.png` |  |
| H15 | `ui/screens/records/RecordStatsScreen.kt (stats-empty)` | `records/stats` | 아직 통계 없음 | `running-records/s15-statistics-empty.png` |  |
| H16 | `ui/screens/records/RunRecordScreen.kt (삭제 실패)` | `records/run/{id}` | 삭제 실패 | `running-records/16-delete-error.png` |  |
| CR02 | — | `run-crew` | 소속 크루에 준비 중인 방(보기 전용) | — | 보기 전용 방 조회 API 없음 — PartyApi.open 은 방을 만들고 들어가므로 미리보기에 쓰지 않는다 |
| CR03 | `ui/screens/walk/CrewRunScreens.kt (CrewRunEntryContent · Ready)` | `run-crew` | 고른 크루 · 대기실 없음 — 대기실 열기 | `run-journey/CR03.png` | 일정 · 목표 거리 · 모임 장소 · 코스가 party_state 에 없음 |
| CR04 | `ui/screens/community/PartyLobbyScreen.kt (PartyLobbyContent)` | `lobby/{crewId}` | 크루원 준비 전 | `run-journey/CR04.png` |  |
| CR05 | `ui/screens/community/PartyLobbyScreen.kt` | `lobby/{crewId}` | 크루원 준비 완료 | `run-journey/CR05.png` |  |
| CR06 | `ui/screens/community/PartyLobbyScreen.kt` | `lobby/{crewId}` | 진행자 | `run-journey/CR06.png` |  |
| CR07 | `ui/screens/walk/CrewRunScreens.kt (CrewStartConfirmSheet)` | `lobby/{crewId}` | 준비 안 된 사람을 두고 출발 | `run-journey/CR07.png` |  |
| CR08 | `ui/screens/walk/RunStartOverlays.kt (RunCountdownStage) · PartyLobbyScreen.kt` | `lobby/{crewId}` | 서버 출발 시각 · 명단으로 3-2-1 | `run-journey/CR08.png` |  |
| CR09 | `ui/screens/walk/RunLive.kt (TogetherRow)` | `run` | 함께 달리는 중 | `run-journey/CR09.png` |  |
| CR10 | `ui/screens/walk/WalkScreen.kt (TogetherSheetContent)` | `run` | 참가자 목록 | `run-journey/CR10.png` | 사람마다 일시정지 상태가 서버에 없음 |
| CR11 | `ui/screens/walk/RunLive.kt (paused · crew)` | `run` | 내 크루 러닝 일시정지 | `run-journey/CR11.png` |  |
| CR12 | `ui/screens/walk/WalkScreen.kt (run-end-dialog · crew)` | `run` | 내 크루 러닝 마치기 확인 | `run-journey/CR12.png` |  |
| CR13 | `ui/screens/walk/RunLive.kt (RunResultContent · crew)` | `run` | 크루 러닝 결과 | `run-journey/CR13.png` |  |
| CR14 | `ui/screens/walk/CrewRunScreens.kt (CrewEntryUi.NoCrew)` | `run-crew` | 가입한 크루 없음 | `run-journey/CR14.png` |  |
| CR15 | `ui/screens/community/PartyLobbyScreen.kt (party-alone)` | `lobby/{crewId}` | 대기실에 나만 | `run-journey/CR15.png` |  |
| CR16 | — | `run-crew` | 이미 출발한 방에 새로 들어옴 | — | 보기 전용 방 조회 API 없음 — 늦게 들어오면 party_open 이 새 방을 만든다(늦은 합류 없음) |
| CR17 | `ui/screens/walk/CrewRunScreens.kt (CrewReadyCheckSheet)` | `lobby/{crewId}` | 준비 전 위치 · 공유 안내(공유는 처음에 꺼짐) | `run-journey/CR17.png` |  |
| CR18 | `ui/screens/walk/WalkScreen.kt (party.networkProblem)` | `run` | 크루 실시간 연결 끊김 — 내 기록은 계속 | `run-journey/CR18.png` |  |
| CR19 | `ui/screens/walk/CrewRunScreens.kt (CrewPickSheet)` | `run-crew` | 소속 크루 바꾸기 | `run-journey/CR19.png` |  |
| CR20 | `ui/screens/walk/CrewRunScreens.kt (CrewNotifySheet)` | `lobby/{crewId}` | 크루에게 알리기 — 미리보기 후 보내기 | `run-journey/CR20.png` | 크루 채팅 바로가기(딥링크) 없음 |
| CR21 | `ui/screens/walk/CrewRunScreens.kt (CrewShareScreen)` | `run` | 크루 기록 공유 — 미리보기 후 보내기 · 경로 끄면 지도 없음 | `run-journey/CR21.png` · `run-journey/CR21-app.png` |  |
| P01 | `ui/screens/walk/RunPermissionFlow.kt (ActivityRationale · Card)` | `run-start 위` | 신체 활동 권한 안내 | `onboarding-v1/s13-activity-rationale.png` |  |
| P02 | `ui/screens/walk/RunPermissionFlow.kt (ActivityDenied · Page)` | `run-start 위` | 다시 물을 수 있는 거절 | `onboarding-v1/s14-activity-denied.png` |  |
| P03 | `ui/screens/walk/RunPermissionFlow.kt (ActivitySettings · Page)` | `run-start 위` | 설정에서 켜야 하는 거절 | `onboarding-v1/s15-activity-settings.png` |  |
| P04 | `ui/screens/walk/RunPermissionFlow.kt (ApproximateLocation · Page)` | `run-start 위` | 대략적인 위치만 | `onboarding-v1/s17-approximate-location.png` |  |
| P05 | `ui/screens/walk/RunPermissionFlow.kt (NotificationRationale · Page)` | `run-start 위` | 러닝 알림 안내 | `onboarding-v1/s19-notification-rationale.png` |  |
| E01 | `ui/screens/home/HomeScreen.kt (home-details)` | `home` | 오늘 걸음 상세 시트 | `run-journey/E01.png` |  |
| E02 | `ui/screens/walk/RunLive.kt (run-goal-value)` | `run` | 멈춘 채 목표 수정 | `run-journey/E02.png` |  |
| E03 | `ui/screens/walk/RunLive.kt (run-details)` | `run` | 러닝 상세 정보 시트 | `run-journey/E03.png` |  |
| E04 | `ui/screens/walk/RunLive.kt (run-saving)` | `run` | 저장 중 | `run-journey/E04.png` |  |
| E05 | `ui/screens/walk/RunLive.kt (RunSharePreviewContent)` | `run` | 공유 미리보기 — 경로 포함은 처음에 꺼짐(지도 없음) | `run-journey/E05.png` · `run-journey/E05b-route.png` |  |
| E06 | `ui/screens/walk/RunJourney.kt/WalkScreen.kt (run-speed-banner · run-void-banner)` | `run` | 측정 확인 필요 구간 · 가짜 위치 | `run-journey/E06.png` · `run-journey/E06b-void.png` |  |
| E07 | `ui/screens/walk/RunStartOverlays.kt (RunLocationOffContent)` | `run?start=true` | 앱 권한은 있는데 휴대폰 위치 기능 꺼짐 — 지도 없음 | `run-journey/E07.png` |  |
| E08 | `ui/screens/walk/RunLive.kt (Settle.VOID)` | `run` | 보상 제외 · 까닭 | `run-journey/E08.png` · `run-journey/E08b-reason.png` |  |
| E09 | `ui/screens/walk/RunLive.kt (Settle.NOT_PAID)` | `run` | 미지급(걸음 0 · 서버 거절) | `run-journey/E09.png` |  |
| K09 | `ui/screens/walk/CourseHubScreen.kt (코스 선택)` | `courses` | 저장 코스 목록 | `run-journey/K09.png` |  |
| K10 | `ui/screens/walk/CourseHubScreen.kt (코스 만들기)` | `courses` | 코스 만들기(마지막 GPS 트랙) | `run-journey/K10.png` |  |
| K11 | `ui/screens/walk/CourseHubScreen.kt (코스 게시판)` | `courses` | 공유 코스 게시판 | `run-journey/K11.png` |  |
| K12 | `ui/screens/walk/CourseHubScreen.kt (course-ranking)` | `courses` | 코스 순위 | `run-journey/K12.png` |  |
| K13 | `ui/screens/walk/CourseHubScreen.kt (course-apply-sheet)` | `courses` | 코스 고르기 확인 | `run-journey/K13.png` |  |
| K14 | `ui/screens/walk/CourseHubScreen.kt (course-clear-sheet)` | `courses` | 고른 코스 풀기 확인 | `run-journey/K14.png` |  |
| K15 | `ui/screens/walk/WalkScreen.kt (course-save-sheet)` | `run` | 달린 코스 저장 | `run-journey/K15.png` |  |
| K16 | `ui/screens/walk/CourseHubScreen.kt (course-upload-sheet)` | `courses` | 공유할 내 코스 고르기 | `run-journey/K16.png` |  |
| K17 | `ui/screens/walk/CourseHubScreen.kt (BoardEmpty)` | `courses` | 공유 코스 없음 | `run-journey/K17.png` |  |
| K18 | `ui/screens/walk/CourseHubScreen.kt (BoardFailed)` | `courses` | 공유 코스 불러오기 실패 | `run-journey/K18.png` |  |
| E10 | `ui/screens/walk/RunLive.kt (Settle.PENDING)` | `run` | 저장됨 · 보상 확인 중(예상 금액) | `run-journey/E10.png` |  |
| E11 | `ui/screens/walk/RunLive.kt (Settle.DONE)` | `run` | 서버가 확인한 금액 | `run-journey/E11.png` |  |
| SHOES | `ui/screens/customize/CustomizeScreen.kt · ui/components/SneakerImage.kt` | `customize (Screen.Customize)` | 구조 그대로 · 색감과 조명만 | `my-shoes/01-my-shoes-390x844.png` · `my-shoes/02-vault-390x844.png` |  |
