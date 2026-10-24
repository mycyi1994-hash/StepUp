# 크루 러닝 — 3번 듀얼 레일 (2026-10-24 핸드오프)

사용자: 핸드오프 ZIP(`StepUp-Claude-handoff-2026-10-24`)과 "이거 확인하고 진행해놔". 원문 지시 · 규칙 · 70장면 명세는 [handoff/](handoff/)
(01_CLAUDE_PROMPT · 02_RULES · 03_SCREEN_BEHAVIOR · 04_DESIGN_BACKGROUND · 05_BACKEND · 06_ANDROID · 07_ACCEPTANCE_TESTS · 08_OPEN_DECISIONS ·
screen-spec.json · rules.json · design-tokens.json · 시안 70장 묶음 [contact-sheet.webp](handoff/contact-sheet.webp)).

- **경제 규칙(공용 내구도 · 하루 한 번 · 2분 인원 · 레벨 · 보상)은 0071 이 이미 핸드오프 R01 ~ R13 과 같다** — 다시 만들지 않았다
  ([crew-run-shared-pool-2026-10-22](../../crew-run-shared-pool-2026-10-22.md)). 이번 판은 **방(대기실)** 과 **화면**이다.
- 서버 **0072**: 대기실 이름 · 함께 달릴 코스(방에 둔 사본 · 판) · 열린 대기실 목록 · 골라 들어가기 · 진행자 코스 바꾸기 · 판을 맞대고 출발.
- 앱: 크루 홈(01) → 열린 대기실(02) → 러닝 만들기(03 · 06, 코스 04 · 05 · 09) → **듀얼 레일 대기실(10 ~ 34, 사진 터널은 여기만)** →
  서버 출발 시각에 맞춘 3 · 2 · 1 · 0 · StepUP! · 가운데 펼침(35 · 36) → 기존 크루 러닝 화면 · 결과(41 ~ 60).

## 승인 디자인을 어떻게 지켰나

- **듀얼 레일 틀 하나**(`ui/screens/walk/CrewDualRail.kt` `DualRailFrame`): 바깥 5dp(110° #96EAFF → #56CBFA 45% → #2E9FD5) · 모서리 20dp ·
  속 125° #113A58 → #09253C 82% · 9dp 안쪽 1.5dp 두 번째 선(#82D8F9 65%, 모서리 11dp) · 아래 5dp 음영(#052338) · 부드러운 그림자.
  크루원 카드와 내 신발 카드가 **같은 부품**이다. 테와 카드는 움직이지 않는다. 방 카드(02) · 코스 묶음(03) · 홈의 두 카드(01) · 신발 선택지(24, 고르지 않은 것은 옅은 테)도 같은 틀.
- 흰 주 버튼(`LobbyPrimaryButton` #F5F8FF / #071B3D · 모서리 14 · 60dp, 막히면 #24455D / #A2BDD0 + 실제 disabled), 준비 칩(#83E5FF / #073346 · 6dp),
  신발 변경(#95E6FF / #07384D · 8dp · 44dp, 누르는 곳 48dp), 대기실 바탕 #031427 · 본문 #F5F8FF · 보조 #B5CBE6 · 정보 #48D9FA.
- **사진 터널 · 움직이는 빛은 대기실에만**(`LobbyTunnelBackdrop`, 자산 `res/drawable-nodpi/crew_lobby_tunnel.webp` — 원본 PNG 2.4MB → 157KB):
  정지 사진 위 다가오는 두 겹(11초 · 위상 10% · 60% · 배율/투명도 0% 1/0 → 14% 1.03/1 → 70% 1.15/1 → 100% 1.23/0, 기준점 50%/43%),
  옆 빛 4.2초 왕복(왼쪽 x +60 → −30 · 회전 −20 → −30°, 오른쪽 거울, 세기 .3 → 1), 가운데 빛 .4 → 1, 글 쪽 덮개.
  - 흐르는 조건: 대기실이 보이고 · 앱이 앞(RESUMED) · 시트 없음 · 출발 전. 시트(대기실 시트 · 준비 전 확인 · 알리기)가 뜨면 멈추고 어둡게.
  - 모션 줄이기: 시계를 세운다(정지 사진 · 멈춘 빛, 이동 · 확대 · 회전 없음). 불러오는 중(68)은 정지 사진(LOBBY_STATIC).
  - 홈 · 목록 · 만들기 · 코스 · 결과 · 레벨 · 기록은 **앱의 기존 바탕**(`crewColors()` — 밝은/어두운 테마)이다. 지도는 실제 지도(밝은 거리 지도 — 크루 러닝 지도와 같다).
- 시트는 불투명한 대기실 면(`LobbySheetFrame` #08243A). 닫기 · 뒤로 · 바깥은 부모의 선택 · 역할을 바꾸지 않는다.
- 출발 연출은 자유 러닝의 승인된 무대(`FreeRunStartStage`)를 그대로 쓴다(COUNTDOWN_EXISTING). 터널을 다시 깔지 않는다.

## 시안과 다른 점(이유)

| 시안 | 앱 | 이유 |
|---|---|---|
| 다른 장면에도 터널 사진 | 대기실에만 | 04 · 00 최신 지시(배경 정책이 그림보다 우선) |
| 홈 · 목록 · 만들기의 흰 주 버튼 | 앱 기존 주 버튼(`FreeRunButton`) | APP_EXISTING — 밝은 테마 바탕에 흰 버튼은 보이지 않는다. 대기실 · 대기실 시트는 흰 버튼 |
| 대기실 코스 지도가 어둡다 | 밝은 거리 지도 | 크루 러닝 지도 결정(2026-10-17 "네이버지도처럼") · 지도는 실제 지도 |
| 진행자는 처음부터 준비된 모습 | 진행자도 "준비 완료"를 한 번 누른다 | 준비에는 걸음 · 정확한 위치 권한 · 위치 기능 확인이 붙는다(2026-10-16). 준비한 뒤 "N명과 출발" |
| "6명과 출발"(일부 준비 때도) | "준비한 N명과 출발" 수 | 실제 출발 인원으로 보인다(5명 출발에 6명처럼 보이지 않게) — 누르면 33 · 34 확인 |
| 출발 3.76초 전 바로 3 | 그 전에는 "출발 신호를 맞추는 중"(35) | 서버 출발 시각은 6초 뒤(0072) — 2초마다 묻는 폰도 3 부터 본다. 출발 시각 = 펼침 시작 |
| 내 아바타 글자 "나" | 내 이름 머리글자 + 이름 칸 "나" | 이름 칸은 시안대로 "나" |
| 37 출발 위치 500m 밖 시트 | 없음 | 서버가 출발 전 위치를 판정하지 않는다(0071 은 출발 무렵 · 2분 뒤 위치로 보너스만 센다). 결과는 42 ~ 44 로 알린다(D03) |

## 서버 0072 계약

| RPC | 누가 | 하는 일 | 오류(메시지) |
|---|---|---|---|
| `party_rooms(p_crew)` → `{server_now, capacity, done_today, rooms[]}` | 크루원 | 열린(LOBBY) 방 + 내 방. 방: id · name · status · host_id · host_name · created_at · members_count · ready_count · capacity · course_name · course_km · course_rev · course_track · mine · kicked. 보기만(들어가지 않는다) | 42501 크루원 아님 |
| `party_create(p_crew, p_name, p_course_name, p_course_km, p_course_track, p_request)` → 방 번호 | 크루원 누구나(R06) | 이름(공백 정리 · 1 ~ 30자) · 코스 사본 · 코스 판 1. 연 사람이 진행자, 다른 출발 전 방에서 나온다. **같은 요청 번호면 그때 연 방**(답을 잃고 다시 보내도 하나 — T03) | `room_name_invalid` · `course_invalid` · `crew_run_done_today` · 42501 |
| `party_join(p_party)` → 방 번호 | 크루원 | 이미 들어가 있으면 그대로. 다른 출발 전 방에서 나온다. 늦은 합류 없음(D10) | `room_full`(20명) · `room_started` · `room_closed` · 42501(내보내짐 · 크루원 아님) |
| `party_course(p_party)` → `{course_rev, course_name, course_km, course_track}` | 방 사람 | 경로는 판이 바뀔 때만 받는다(2초마다 묻는 `party_state` 에는 없다) | 42501 |
| `party_set_course(p_party, name, km, track)` → 새 판 | 진행자 · 출발 전 | 판 + 1, **진행자를 뺀 모두의 준비를 푼다(D14)** | 진행자 아님 · 출발함 · `course_invalid` |
| `party_start_rev(p_party, p_course_rev)` | 진행자 | 판이 다르면 `course_changed`, 같으면 `party_start`(0071 규칙 그대로) | `course_changed` + 0071 출발 오류 |
| `party_state` | 방 사람 | 0063 + name · created_at · course_name · course_km · course_rev | |
| `party_start` | 진행자 | 0071 그대로 + **출발 시각 6초 뒤**(0071 은 4초) + `crew_runs` 에 방 이름 · 코스 · 판 사본 | |
| `crew_run_result` | 본인 | 0071 + party_id · party_name · course_name · course_km · started_at · crew_level · shoe | |

- 표: `parties.name · course_name · course_km · course_track · course_rev · create_key`(+ `(host_id, create_key)` 유일 색인), `economy.crew_runs.party_name · course_name · course_km · course_rev`.
- 예전 `party_open`(크루의 열린 방 하나에 저절로)은 옛 앱 · 번개러닝용으로 그대로 둔다. 새 앱은 크루 방에 쓰지 않는다.
- 이미 적용된 마이그레이션(0001 ~ 0071)은 고치지 않았다. 개인 러닝 경제 경로(`record_session` 개인 · 600 SUP · 신발 내구도)는 그대로다.

## 앱 길

| 길 | 화면 | 들어오는 곳 |
|---|---|---|
| `Routes.RUN_CREW` | 크루 홈(01) — `CrewRunEntryContent`(Ready → `CrewHomeBody`) | 러닝 시작 메뉴의 크루 |
| `Routes.CREW_ROOMS` `crew-rooms/{crewId}` | 열린 대기실(02) — `CrewRoomsScreen` | 크루 홈 "크루 러닝 시작" · 커뮤니티 · 크루 상세 · 크루 홈(4번) · 알림 · 게시판(`Routes.crewRun`) |
| `Routes.CREW_ROOM_NEW` `crew-rooms/{crewId}/new` | 러닝 만들기(03 · 06) + 코스 고르기(04 · 09) + 코스 상세(05) | 목록 "새 대기실 만들기" |
| `Routes.LOBBY` `lobby/{crewId}?party={id}` | 대기실(10 ~ 34) — `PartyLobbyScreen` → `CrewLobbyContent` | 목록에서 고른 방 · 방금 연 방. 들어가 있던 방이 있으면 `Routes.crewRun` 이 그 방으로 |
| 앱 맨 위 겹 | 출발 연출(35 · 36) — `CrewLaunchOverlay` | 대기실이 서버 COUNTDOWN 을 받을 때. 러닝 화면으로 길이 바뀌어도 끊기지 않는다 |

- 저장소(`CrewRepository`): `openRoom(crewId, partyId)` — 고른 방에만 들어간다(`RoomTarget.Join`). 막히면 까닭(찼다 · 출발했다 · 닫혔다 · 내보내짐)을 남기고 멈춘다.
  `rooms` · `createRoom`(요청 번호) · `setRoomCourse` · `acknowledgeCourse`. `startParty` 는 코스가 있으면 판을 맞대고(`party_start_rev`).
  방 상태에 이름 · 연 시각 · 코스(판 · 이름 · 거리, 경로는 판이 바뀔 때 `party_course`). 참가자가 본 판보다 높으면 `courseChanged`(08).
  고른 방의 출발을 놓치면(2분 넘게 대기실 밖) 다른 방을 저절로 열지 않고 "출발을 놓쳤어요"(목록으로).

## 70장면 대응표

상태: **구현** = 이번에 만든 것 · **기존** = 10-22 · 10-16 크루 전달본으로 이미 있는 실제 화면(이번 규칙과 같다) · **부분** · **없음**.
캡처는 `RunJourneyDesignTest#crewRooms`(C..) · `#crew`(CR..) — Experience QA 아티팩트 `screen-gallery/run-journey/`.

| ID | 장면 | 배경 | 구현 | 상태 | 캡처 |
|---|---|---|---|---|---|
| 01 | 크루 홈 | 앱 | `CrewHomeBody` — 오늘 크루 내구도(남은/총량 · 오늘 N명 · 자정 초기화) · 다음 목표(Lv · 남은 km · 크루 레벨 보기) · 진행 중인 대기실 N개 · 크루 러닝 기록 · 크루 러닝 시작 | 구현 | C01-home |
| 02 | 열린 대기실 | 앱 | `CrewRoomsContent` — 방 카드(듀얼 레일 · 이름 · N/20 · 코스 · 실제 지도 · 진행자 · 준비 수), 빈 목록 · 실패 · 오늘 이미 함 | 구현 | C02-room-list(-empty · -failed) |
| 03 | 러닝 개설 | 앱 | `CrewRoomCreateContent` — 이름(1 ~ 30자) · 코스 선택하기 · 공개 범위 · 내 신발 기여 미리보기 | 구현 | C03-create |
| 04 | 코스 선택 | 앱 | `CrewCoursePickerContent` — 내 코스 · 공유 코스(게시판), 지금 코스 표시 | 구현 | C04-courses |
| 05 | 코스 상세 · 선택 | 시트 | `CrewCourseDetailSheet` — 전체 경로 · 거리 · 출발 점, "이 코스로 달리기"를 눌러야 고른다 | 구현 | C05-course-detail |
| 06 | 코스 선택 완료 | 앱 | 같은 화면(코스 있음) — 연타 막기 · 서버가 방 번호를 준 뒤에만 대기실로 · 실패면 입력 그대로 | 구현 | C06-create-selected · C06-create-failed |
| 07 | 진행자 코스 변경 | 시트 | `LobbyCourseSheet`(진행자) → 코스 목록 → 상세 → `party_set_course`(서버가 진행자 확인) | 구현 | C07-course-change |
| 08 | 참가자 코스 변경 알림 | 시트 | `CourseUpdatedSheet` — 판이 오르면 한 번, "코스 확인" → 읽기 전용 보기 | 구현 | C08-course-updated |
| 09 | 저장한 코스 없음 | 앱 | 빈 내 코스 · 공유 코스 실패/불러오는 중을 따로 | 구현 | C09-course-empty |
| 10 | 6명 준비 완료 | 터널 | `CrewLobbyContent` 진행자 — "6명과 출발" | 구현 | C10-host-lobby |
| 11 | 일부 준비 중 | 터널 | "5명과 출발" → 34 | 구현 | C11-host-waiting |
| 12 | 혼자 대기 | 터널 | 빈 자리 "함께 달려요" · 출발 막힘 · "크루원 부르기"(기존 크루 채팅 알리기) | 구현 | C12-solo · CR15 |
| 13 | 20명 | 터널 | 다섯 + "+15 전체 보기" | 구현 | C13-full |
| 14 | 참가자 관리 | 시트 | `RosterSheet`(진행자) — 20명 모두 · 내보내기 · 코스 바꾸기 · 부르기 | 구현 | C14-host-roster |
| 15 | 내보내기 확인 | 시트 | `KickSheet` — 실제 user_id, 대기실에서만(크루 탈퇴 아님) | 구현 | C15-kick |
| 16 | 내보내기 완료 | 터널 | 서버 명단 그대로(빠진 사람 · 준비 수 · 버튼 수) | 구현 | C16-removed-host |
| 17 ~ 20 | 모집 알림 · 보내는 중 · 보냄 · 실패 | 시트 | 기존 `CrewNotifySheet`(크루 채팅으로 실제로 보낸다 — SMS 없음) | 기존 | CR20 |
| 21 | 진행자 나가기 확인 | 시트 | `LeaveSheet(host)` — 승계(먼저 들어온 사람)/닫힘 안내 | 구현 | C21-host-leave |
| 22 | 참가자 준비 전 | 터널 | 출발 · 내보내기 · 코스 바꾸기 없음, ⓘ 규칙 | 구현 | C22-member-lobby |
| 23 | 준비 완료 · 진행자 대기 | 터널 | 꺼진 "진행자가 출발하면 함께 시작해요" + 준비 취소 | 구현 | C23-member-ready |
| 24 | 내 신발 선택 | 시트 | `ShoesSheet` — 실제 보유 신발(서버 · 앱 · 팔지 않음) · 최대 내구도 · 효율 · 착화감 · 기여 미리보기, 임시 선택 → "이 신발로 준비" | 구현 | C24-shoes |
| 25 | 참가자 코스 보기 | 시트 | `LobbyCourseSheet`(읽기 전용) — 준비 상태를 바꾸지 않는다 | 구현 | C25-course-view |
| 26 | 오늘 크루 내구도 | 시트 | `StaminaSheet` — 남은/총량 · 기여 인원 · 내가 더할 양 · 분당 5 · 자정 초기화, 0/0 · 못 읽음 구분 | 구현 | C26-stamina |
| 27 | 크루 목표 · 레벨 | 시트 | `GoalSheet` — Lv · 누적 · 다음 목표 · 효과(Lv20 최고) | 구현 | C27-goal |
| 28 | 레벨 1 ~ 20 | 시트 | `LevelsSheet` — 누적 거리 · 내구도 · 보상, 지금 레벨 강조 | 구현 | C28-levels |
| 29 | 인원 보너스 표 | 시트 | `BonusSheet` — 2 ~ 20명 모두, 조건 · 레벨 따로 | 구현 | C29-bonus |
| 30 | 크루 러닝 규칙 | 시트 | `RulesSheet` — 서버 규칙 값(0071)로 다섯 주제 | 구현 | C30-rules · CR04-rule |
| 31 | 함께 달릴 크루원 | 시트 | `RosterSheet`(읽기 전용) | 구현 | C31-member-roster |
| 32 | 참가자 나가기 확인 | 시트 | `LeaveSheet(member)` — 출발 전은 하루 한 번을 쓰지 않는다 | 구현 | C32-member-leave |
| 33 | 출발 전 마지막 확인 | 시트 | `StartSheet` — 내 신발 · 출발 인원 · 예상 합산 보너스(서버 미리보기) · 내가 더할 양 · 하루 한 번 | 구현 | C33-start-confirm |
| 34 | 준비된 인원만 | 시트 | `StartSheet`(일부) — 준비 중 인원은 보너스 · 기여에 없다 | 구현 | C34-partial-start |
| 35 | 출발 동기화 | 앱 | `CrewStartingStage` — 서버 출발 3.76초 전까지 | 구현 | C35-starting |
| 36 | 3 · 2 · 1 · 0 · StepUP! | 카운트다운 | `CrewLaunchOverlay` → `FreeRunStartStage`, 출발 시각 = 펼침, 러닝이 켜진 뒤 러닝 화면을 펼친다 · 뒤로 막음 · 모션 줄이기 | 구현 | C36-countdown · CR08 |
| 37 | 출발 위치 500m 밖 | 시트 | — | 없음(D03) | |
| 38 · 39 | GPS · 위치 권한 | 시트 | 기존 준비 전 확인(`CrewReadyCheckSheet`) · OS 권한 · 위치 기능 — 위치를 기다리는 화면은 두지 않는다(2026-10-16 결정) | 기존 | CR17 |
| 40 | 보너스 인원 제외 안내 | 시트 | `UnverifiedSheet` — 준비를 누를 때 앱을 켠 동안 한 번, 막지 않는다(번호 입력 · SMS 없음) | 구현 | C40-unverified |
| 41 ~ 44 | 2분 확인 · 확정 · 일부 제외 · 확인 불가 | 지도 | 기존 `CrewRunLive`(인원 확인 중 칩 · 확정/일부/없음 시트) | 기존 | CR09 ~ |
| 45 ~ 48 | 러닝 · 휴식 · 20% · 0 | 지도 | 기존 `CrewRunLive`(공용 체력 · 적립 멈춤 띠) | 기존 | CR09 ~ CR12 |
| 49 · 50 | 끝내기 · 마지막 자동 종료 | 시트 | 기존(`crew_finish_once` · `crew_alone_*`) | 기존 | |
| 51 | 잠금화면 | 시스템 | 기존 실시간 알림(`RunNotification` — 크루 체력 줄) | 기존 | |
| 52 | 함께 뛰는 크루원 | 시트 | 기존 함께 달리는 사람 시트 | 기존 | |
| 53 · 54 · 55 | 저장 중 · 결과 · 보상 상세 | 앱 | 기존 `CrewRunResult` · `CrewRewardSheet`(서버 확인 전 "예상 · 확인 중") + **대기실 이름 · 코스 줄**(이번, `crew_run_result`) | 기존+ | CR13 |
| 56 · 57 | 레벨 기여 거리 · 공용 사용량 | 시트 | 보상 시트 안 줄(내가 더한/쓴 체력 · 오늘 공용 · 개인 신발 0 · XP) — 따로 된 시트는 없다 | 부분 | CR13-reward-sheet |
| 58 | 크루 레벨 업 | 앱 | 결과 줄 + 크루 홈 레벨 화면 | 부분 | |
| 59 | 기록 상세 | 앱 | 내 러닝 기록의 지난 크루 러닝(결과와 같은 화면) + 그때의 대기실 이름 · 코스 | 기존+ | |
| 60 | 공유 | 앱 | 기존 `CrewShareScreen`(크루 채팅 · 실제로 보낸다) | 기존 | CR21 |
| 61 | 오늘 크루 러닝 완료 | 앱 | 크루 홈 "오늘 기록 보기" + 다음 참여 시각 · 목록 안내(서버가 막는다 — 다른 방 · 크루 · 재설치도) | 구현 | C61-used |
| 62 | 크루 이동 후 제한 | 앱 | 대기실 안내(`crew_lobby_done_elsewhere`) · 서버 user_id + 한국 날짜 | 기존 | |
| 63 · 64 | 연결 끊김 · 복구 | 지도 | 기존 CR18 띠 · 다시 연결 | 기존 | |
| 65 · 66 | 저장 실패 · 무효 | 앱 | 기존 저장 실패 · 무효(Settle.VOID) | 기존 | CR13-void |
| 67 | 자정 | 앱 | 크루 홈이 새 날짜 값(0/0 · 참여 가능)으로 보인다 — 따로 된 안내 화면은 없다 | 부분 | |
| 68 | 대기실 불러오는 중 | 정지 터널 | `LobbyLoading` — 출발 막음 | 구현 | C68-loading |
| 69 | 불러오기 실패 | 앱 | `CrewLobbyTerminal(FAILED)` — 같은 방 번호로 다시 | 구현 | C69-load-failed |
| 70 | 대기실에서 나옴 | 앱 | `CrewLobbyTerminal` — 나옴 · 닫힘 · 찼음 · 출발함 · 출발 놓침, "다른 대기실 보기" | 구현 | C70-removed · -started · -full |

## 규칙 · 결정

- R01 ~ R13: 0071 이 서버에서 정한다(앱은 서버 값만 보인다) — 검사는 `supabase/tests/schema_test.sql` 0069 ~ 0072 절(T01 · T02 · T03 · T05 · T06 · T08 ~ T11 · T13 ~ T18 · T20 · T21 · T23 ~ T25 · T27 · T32 · T34 · T37 · T41 · T42 · T46 · T68 · T71).
- 이번에 고른 것(바꿀 수 있다):
  - **D14** 코스를 바꾸면 진행자를 뺀 모두의 준비를 푼다(새 코스를 보고 다시 준비). 출발은 진행자가 본 판으로만(`course_changed`).
  - **D10** 늦은 합류 없음(출발한 방에 `room_started`). **D16** 진행자가 나가면 먼저 들어온 사람이 진행자, 아무도 없으면 닫힘(0012 그대로).
  - 오늘 크루 러닝을 한 사람은 새 방을 열지 않는다(`crew_run_done_today` — 준비할 수 없는 빈 방). 방에 들어가 보는 것은 된다.
  - 새 방을 열거나 들어가면 출발 전 다른 방에서 나온다(두 방에 남지 않게). 같은 요청 번호의 방 만들기는 방 하나.
  - 출발 시각은 6초 뒤(늦게 안 폰도 3 부터) · 코스는 방에 사본(폰에만 있는 내 코스도, 원래 코스를 지워도 방 · 기록은 그대로).
  - 인증 전 안내(40)는 준비를 누를 때 앱을 켠 동안 한 번.
- 사용자 결정이 필요한 것(손대지 않음): **D01 · D02 · D04 · D05 · D06 · D07 · D08 · D09**(0071 의 지금 동작이 있다 — 문서 crew-run-shared-pool), **D03 / 37**(500m 기준점 · 출발 전 위치 판정 — 지금은 2분 판정만),
  **D12**(휴대폰 인증 출처 — 지금은 인증한 사람이 없어 인원 보너스 0), D15(부분 출발은 0071 그대로 서버 명단), D17(모집 · 레벨 업 푸시 — 지금은 크루 채팅 글로 알린다).

## 확인

- 서버: `supabase/tests/run.sh` 전부 통과(0072 절 — 크루원 개설 · 이름 · 경로 검사 · 남의 크루 막기 · 목록 · 골라 들어가기(두 번) · 코스 바꾸기(진행자만 · D14 준비 풀림) · 옛 판 출발 막기 ·
  내보내기(다시 못 들어옴 · 크루는 그대로) · 6초 뒤 출발 · 참여 기록에 방 이름 · 코스 · 늦은 합류 막기 · 오늘 한 사람 개설 막기 · 다른 방에서 나오기 · 20명 · 같은 요청 번호), setup.sql 다시 붙이기.
- 앱: `CrewRoomsTest`(15 — 서버 답 · 보내는 몸통 · 이름 · 경로 규칙 · 막힘 까닭 · 코스 알림 · 한 화면 · 터널 시간표 · 모션 줄이기 · 출발 시계) +
  기존 단위 검사 · lint · `check-strings.py` · `check_design_contract.py`.
- 기기: `RunJourneyDesignTest#crewRooms`(C01 ~ C70 캡처 · 터널이 대기실에만 · 역할별 버튼 · 작은 화면 · 큰 글씨 · 밝은 테마에서 주 버튼이 화면 안)는 Experience QA(PR)에서 돈다.
  `#crewLobbyMotion`(T66 — 모션을 켜고 시계를 손으로 넘긴다): 대기실만 보이면 사진 · 빛이 흐르고(C10-motion-1 ~ 3), 시트가 뜨면 그 자리에 멈추고(C08-paused-1 · 2),
  모션 줄이기면 정지 사진(C10-reduced-1 · 2) — 화면 위쪽 띠의 픽셀을 맞대어 본다. 다른 캡처는 모션 줄이기로 찍는다(배경이 끝없이 돌아 기다림이 끝나지 않으므로).
  이 세션에는 에뮬레이터가 없어 직접 돌리지 않았다.
- **하지 않은 것**: 두 계정 이상으로 실제 서버에서 같은 방 출발 → 2분 → 휴식 → 끝내기(07 의 필수 실기기 시나리오), 운영 DB 적용(main 에 합치면 deploy-sql 이 붙인다),
  실제 GPS 기기. 웹 시안 QA 는 이 검사의 대신이 아니다.
