# 전체 디자인 자료 적용 (2026-10-26 전달본)

사용자: "이거 적용해줘"(구글 드라이브 `StepUp-all-design-assets.zip`). 시안 9장 × 블랙 · 화이트, 사람 없는 배경 15장, 미리보기 HTML.
원본은 이 폴더 — `01-screens/{black,light}` · `03-overviews` · `interactive-preview.html` · `editable-latest.html` · `backgrounds-prompts.json`.
앱 화면을 로컬에서 그려 본 것은 `app-render/`(Robolectric, 예시 값 — 실제 앱은 기기 기록 · 서버 값).

## 사용자 결정(AskUserQuestion, 모두 추천안)

| 질문 | 고른 것 |
| --- | --- |
| 하단 탭 넷(뽑기 탭 없음) | **뽑기는 신발 탭 안 글자 탭**(내 신발 · 신발 보관함 · 뽑기). 기능은 그대로 |
| 원격 뛰기 · 만나는 곳 · 출발 시각 · 정원 | **서버까지** — 0074 |
| 블랙 테마 | **시안처럼 검게** — 어두운 테마가 #000 바탕 · #101216 카드. 기본 테마는 그대로(시스템 따름) |
| 러닝 탭 신발 카드 | **없음** — 신발은 러닝 시작의 신발 점검 · 신발 탭에서 |

## 화면 대응

| 시안 | 앱 | 비고 |
| --- | --- | --- |
| run | 러닝 탭 자유 러닝 칸(`HomeContent` · `RunTab.kt`) | 세그먼트 · 내 소속 크루 · 오늘 걸음 고리 · 배경 사진(15장 중 무작위, `assets/run_photos`) · GPS 줄 · 오늘 거리 / 적립 / 이번 주 · 러닝 시작(→ 시작 메뉴 U01 그대로) |
| lobby | 러닝 탭 크루 러닝 칸 | 내 소속 크루 · 진행 중인 대기실(`party_rooms`, 10초마다) · 새 대기실 만들기. 크루가 없으면 "크루 찾기"(커뮤니티) |
| create-* 4장 | 새 대기실 만들기(`CrewRoomCreateScreen`) | 코스 직접 설정 / 자유 러닝 · 같이 / 원격 · 만나는 곳 / 출발 위치 · 출발 시각 · 정원(2 ~ 20) · 내 소속 크루 → `party_create_v2` |
| active | 자유 러닝 중 · 일시정지(`FreeRunMapLive`) | 지도가 위, 2 × 2 숫자 · 알약 둘 |
| compose | 글쓰기 번개러닝(`PostComposeScreen`) | 만나는 곳 · 출발 시각(시각 칸) · 거리 · 정원 · 함께 뛰는 분께(선택 — 본문) |
| profile | 내 정보(`ProfileHome`) | 사진 틀 거리 카드 · 다음 기록 막대 · 최근 러닝 줄 |

## 서버 0074 (`supabase/migrations/0074_crew_room_modes.sql`)

- `parties` 에 `run_mode`(TOGETHER · REMOTE) · `meet_place` · `meet_lat/lng` · `plan_at` · `capacity`.
- `party_create_v2` — 0073 `party_create` 와 같고 위 값을 받는다. 원격 방은 만나는 곳을 받지 않는다. 출발 시각은 지금 − 10분 ~ 14일, 정원 2 ~ 20.
  틀리면 `room_options_invalid`. 옛 `party_create` 는 그대로(같이 뛰기 · 정원 20).
- `party_join` 은 방의 정원으로 막는다. `party_rooms` · `party_state` 가 새 값과 이름 앞 넷(`member_names`)을 준다.
- 원격 방은 함께 달리기 인원 보너스가 없다(`crew_run_bonus_eval` → UNAVAILABLE · REMOTE). 공용 체력 · 하루 한 번 · 레벨 보상은 그대로.
- 출발 시각은 안내일 뿐이다 — 출발은 진행자가 누른다(판 맞대기 그대로).

## 시안과 다른 점

- 숫자 · 이름 · 크루는 모두 실제 값이다(시안의 "망원 러닝클럽 · 3,420 · 128.4km"는 예시).
- 러닝 시작 버튼은 시작 메뉴(챌린지 · 코스 · 다이어트 · 순위 · 기록)로 간다 — 메뉴를 지우지 않는다.
- 달리는 중 오른쪽 알약은 "화면 잠금"이다. "길게 눌러 종료"는 예전처럼 일시정지 뒤에만(달리는 중 종료를 새로 만들지 않는다).
  러닝 정보(⋯) · 크게 보기는 지도 위 작은 원 버튼.
- 번개러닝의 목표 페이스 · 출발 날짜 · 임시저장은 없다 — 서버에 페이스 칸이 없고 출발은 24시간 안(서버 규칙)이다. 버튼은 바로 올리기.
- 대기실 만나는 곳은 글자만 받는다(지도 핀 좌표 칸은 서버에 있으나 앱은 아직 보내지 않는다).
- 블랙 시안의 버튼 파랑 #477DFF 는 흰 글자 대비가 3.7:1 이라 버튼 바닥은 #3D74FA(4.1:1, `PaletteContrastTest`).
- 화이트 시안의 세부 색(#F4F5FA 등)은 `WhiteBlueUi` 에, 블랙은 `DarkBlueUi` · `DarkPalette` 에 넣었다. 서체는 앱 서체 그대로(10-25 의 Paperlogy · Saira Condensed).

## 남은 결정

- 대기실 만나는 곳을 지도에서 고를지(좌표 → 거리 표시).
- 번개러닝 목표 페이스를 서버에 둘지.
