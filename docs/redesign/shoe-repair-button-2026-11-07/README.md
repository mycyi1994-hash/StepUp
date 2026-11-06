# 신발 상세 — 번호 옆 수리 버튼 (2026-11-07)

사용자가 두 배치 시안 중 **1안**을 골랐다 — 이름 · 등급 · `No. 번호` 줄에서 번호 바로 오른쪽에 붉은 `🔨 수리하기`, 머리 오른쪽 `⋯` 은 뺀다.
하단 강화 · 착용 사이에 수리를 넣는 2안은 하지 않는다.

| 파일 | 뜻 |
| --- | --- |
| [references/01-approved-number-right.png](references/01-approved-number-right.png) | 사용자가 고른 시안(그림 편집 — APK 캡처가 아니다). 자리 · 붉은색 · 망치 · ⋯ 없음만 따른다 |
| [references/02-before-native.png](references/02-before-native.png) | 바꾸기 전 실제 앱 화면(테스트 신발) |

## 바뀐 것

- 번호 옆 수리하기: `ShoeNameWithBadge(action = ShoeNameAction(...))`(`ui/components/ShoeGradeBadge.kt`) — 배지 · 번호와 한 덩어리로 이름 끝에 붙는 알약.
  붉은 채움 `#D92F45`(흰 글자 4.7:1) · 높이 28dp(글자가 커지면 함께) · 13sp SemiBold · 망치는 장식(읽기 도구는 "수리하기"만). 태그 `shoe-repair-open`.
  덩어리(배지 · 번호 · 알약)가 한 줄 폭에 다 들어가지 않으면(아주 큰 글씨 · 좁은 화면) 알약만 이름 바로 아래 줄로 — 숨기거나 화면 밖으로 밀지 않는다.
  `action` 을 넘기지 않는 다른 화면(내 신발 · 보관함 · 러닝 결과 등)은 그대로다.
- 누르면 기존 `onOpenRepair` → `ShoeCareStore.openRepair(sneakerId)`(견적 조회만) → 기존 `ShoeRepairSheet`. 수리 요청은 시트의 확인 버튼(`confirmRepair`)에서만.
  결과를 모르는 수리 · 강화(`ShoeCareStatus.busy`)면 흐리게 막히고, 위 "결과 확인" 안내(`shoe-repair-pending`)가 같은 시트를 연다.
- 머리 오른쪽 `⋯`(`shoe-more`)과 관리 시트(`ManageSheet` · `DetailSheet.Manage` · `CareMenuRow`)를 지웠다. 공용 머리가 오른쪽을 비워 제목은 가운데 그대로.
- 본문 신발 관리의 일반 수리 줄(`shoe-care-repair`)은 뺐다(같은 진입점을 되풀이하지 않는다). 스탯 포인트 · 판매하기 · 신발 기록 줄과 조건은 그대로.
- 쓰는 곳이 없어진 문구 `sdv_more` · `sneaker_about_body` · `care_durability_now`(4개 언어)를 지웠다.
- 하단 강화하기 · 이 신발 신기, 신발 그림 · 등급 프레임 · 네 능력치 줄 · 하단 탭은 그대로다.

## 시안과 다른 점

- 이름이 아주 긴 신발(이름 바꾸기 최대 20자)은 덩어리가 넓어진 만큼 두 줄 안에서 예전보다 한두 글자 일찍 "…"로 줄어든다(이름 두 줄 · 배지 · 번호는 늘 남는 기존 규칙 그대로).

## 확인

- 기기 검사: `ShoeCareDesignTest#repairButton`(기기 폭 · 360 · 390 · 430dp · 큰 글씨 1.3 · 2.0 · 긴 이름 · 밝은 테마, 누르면 시트만 열리고 확정 0번 · 막힌 상태),
  `ShoeCareDesignTest#careScenes` · `ShoeDetailDesignTest`(앱 셸 안에서 번호 옆 수리하기 → 실제 수리 시트 → 닫기, 내구도 · 착용 그대로)를 새 경로로 바꿨다.
- 짧은 캡처 묶음: Screen Gallery Capture 를 `scope = shoe-detail` 로 손으로 돌린다(1080×2400, API 34 · 35) — `screen-gallery/shoe-care-blue/` · `shoe-detail-v1/`.
