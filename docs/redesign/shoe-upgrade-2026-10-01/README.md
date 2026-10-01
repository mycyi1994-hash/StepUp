# 신발 강화 — 하위 등급 신발 3개 (2026-10-01 지시서 v6)

사용자가 보낸 지시서 v6(17장, 16번 성공률 설명 화면 없음)를 실제 앱 · 서버에 적용했다.
원문은 [CLAUDE-HANDOFF.md](CLAUDE-HANDOFF.md) · [balance-rules.json](balance-rules.json) · [screen-index.json](screen-index.json),
시안은 [screens/](screens/) (원본 PNG 를 540px WebP 로 줄여 보관 — screen-index.json 의 `.png` 경로는 같은 이름의 `.webp`).
시안의 이름 · 번호 · 수치는 예시이고, 앱은 서버가 준 값을 쓴다.

## 흐름

신발 상세 → 강화하기(⋯ 이 신발 관리의 강화도 같은 화면) → 01 → 빈 슬롯 + → 02 재료 선택(하단 시트) → 선택한 n개 넣기 → 03 →
강화하기 → 04 최종 확인(하단 시트) → 강화 시작 → 05 → 06 성공 / 07 실패. 뒤로는 상세로.

| 시안 | 앱 상태 (`ShoeUpgradeState`) |
|---|---|
| 01 · 03 · 08 | `UpgradePhase.Editing` — 적용한 재료 0 · 3 · 2개 |
| 02 · 09 · 10 · 11 | `UpgradeSheet.Picker` + `MaterialsLoad.Ready(목록)` · `Ready(빈 목록)` · `Loading` · `Failed` |
| 04 | `UpgradeSheet.Confirm(서버 견적)` |
| 05 | `UpgradePhase.Running` |
| 06 · 07 | `UpgradePhase.Succeeded` · `Failed` — 서버가 확정한 결과만 |
| 12 | `UpgradeSheet.Changed` — 실행 전 확인에서 쓸 수 없게 된 재료(이미 선택에서 뺌) |
| 13 · 17 · 18 | `UpgradePhase.Blocked(MAX_LEVEL · LOWEST_GRADE · TARGET_UNAVAILABLE/TARGET_GONE/LEGACY)` |
| 14 | `UpgradeSheet.Offline` — 요청을 보내지 않았다고 확실할 때 |
| 15 | `UpgradePhase.Unknown(저장한 요청)` — 같은 요청 키의 결과만 다시 묻는다 |

## 규칙 (서버 `supabase/migrations/0054_shoe_forge.sql`, 앱 `domain/ShoeForge.kt`)

- 상한 Lv 20. 기본 성공률 100 − 3L %, 재료 한 개 보정 (2 + 0.4(재료 Lv − 1)) × 등급 차 계수(1 · ½ · ¼) %p, 최종 min(100%, 합).
  확률은 천분율 정수로 계산 · 저장한다(80.4% = 804).
- 재료는 대상보다 낮은 실제 등급 3개. 소유 id 로 고른다(모델 번호 · 목록 순번이 아님). 성공 · 실패 모두 재료 3개 소각.
- 성공 시 레벨 +1. 효율 +0.5%p · 착화감 +0.2%p(20% 상한)는 서버 실효 계산(`economy.sneaker_effective`)이 레벨에서 만든다 — 따로 더하지 않는다.
- SUP 요금 없음. 예전 SUP 강화(`sneaker_upgrade`)는 쓰지 않는다(설치된 예전 앱을 위해 서버 함수는 남겨 둠).
- 판정 · 소각 · 대상 갱신 · 기록은 `forge_start` 한 트랜잭션. 요청 키(uuid)는 앱이 보내기 전에 저장하고(`forge_pending`),
  같은 키는 다시 보내도 같은 결과를 돌려준다. 결과를 모르면 `forge_result` 로 같은 키만 묻는다 — 서버에 기록이 없으면
  그 키를 무효로 남겨 "받지 않음"을 확정한다(늦게 온 같은 키 요청도 실행되지 않는다).

### 재료가 될 수 없는 신발

착용 중 · 판매 중 · 시작 신발(STARTER) · 가져온 신발(IMPORT · MINT) · 체인에 기록된 신발(토큰 있음 · 금고 발행 대기 · 체인 작업 중).
체인 신발은 계약에 소각 기능이 없어서 DB 에서 숨기는 것만으로 소각했다고 할 수 없기 때문이다(지시서 188행).

### 상한

모든 등급 20. 단 v3 금고 토큰 신발은 계약 상한(레어 15)과 20 중 작은 값 — 체인 값과 어긋나지 않게.

## 확인

- 서버: `supabase/tests/run.sh` — 80.4% 예시 · 보정 · 막힘 사유 · 견적 바뀜 · 성공/실패 소각 · 같은 키 재시도 · 키 재사용 거부 · 무효화.
- 앱 단위: `ShoeForgeTest` — 식 · 선택(넷째 무시 · 임시 선택 · 취소) · 요청 키(보내기 전 저장 · 미확인 · 받지 않음 · 견적 바뀜).
- 기기: `ShoeUpgradeDesignTest` — 17장 상태 + 360dp · 큰 글씨, `screen-gallery/shoe-upgrade/`.
