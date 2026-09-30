# 신발 상세 — 카툰 입체형 능력치 네 칸 (2026-09-30 확정)

사용자가 확정한 1번 카툰 입체형([approved.png](approved.png))을 실제 신발 상세(`SneakerDetailScreen`)에 적용했다.
지시서 원문은 [CLAUDE-HANDOFF.md](CLAUDE-HANDOFF.md). 시안의 이름 · 번호 · 수치는 예시이고, 앱은 고른 보유 켤레의 값을 쓴다.

## 화면

등급 무대(`SneakerGradeStage`, 440:418) → 이름 · 등급 배지 · `No. 0007` 한 줄 → 레벨 · 효율 · 착화감 · 내구도 네 칸 → 이 신발 신기.

- 네 칸은 스틸 블루(#244768) 독립 칸, 칸 사이는 8dp. 칸마다 `항목명 · 카툰 입체 막대 · 값` 한 줄.
- 이름 열 · 값 열 폭은 네 줄 중 가장 긴 글에 맞춰 같게 — 네 막대의 시작 · 끝이 같다(큰 글씨 · 320dp 포함).
- 막대(`CartoonStatBar`, `ui/screens/items/ShoeDetailBars.kt`)는 경로로 그린다: 받침 → 잉크 외곽선 → 프레임(밝은 윗면 · 어두운 아랫면) →
  남색 트랙 → 채움(윗면 · 정면 · 아랫면) → 하이라이트 한 줄. 진행률은 안쪽 트랙 폭에 적용, 0 이면 채움 없음, 사선 깊이는 채움 길이의 절반 이하.
- 설명 문구(SUP 적립 보너스 · 에너지 절감 두 칸), 본문의 "능력치 자세히" · "신발 정보" 줄, 보유 중 · 착용 상태 글은 본문에서 뺐다.
  능력치 자세히 · 신발 정보는 지우지 않고 ⋯(이 신발 관리) 안으로 옮겼다. 강화 · 수리 · 판매 · 착용 흐름은 그대로.

## 값

| 줄 | 값 | 막대 |
|---|---|---|
| 레벨 | `level / maxLevel` (서버 양수 상한, 없으면 등급 기본 10 · 15 · 20 · 30) | level ÷ maxLevel |
| 효율 | `formatBonus(bonusPercent)` | ÷ 27.5% |
| 착화감 | `formatPercent(energySavingPercent)` | ÷ 20% |
| 내구도 | `formatDurability` (서버 값은 내림) | ÷ 100 |

계산은 `detailStatRows`(ShoeDetailModel.kt) — 레벨 한 줄 + 내 신발의 `statBars` 세 줄. 번호는 `formatShoeNumber(mintNumber)`.

## 확인

- 단위: `ShoeDetailModelTest`(네 줄 순서 · 값 · 길이, 레벨 상한, 끝값, 번호 형식).
- 기기: `ShoeDetailDesignTest` — `s00-cartoon-approved`(확정 시안 예시 값), `s00b-cartoon-edges`(0% · 작은 양수 · 100% · 30/30 · 다섯 자리 번호),
  큰 글씨(`s31d`) · 320dp(`s32d`)에서 막대 시작 · 끝 정렬 검사, 착용 흐름(⋯ 안의 능력치 · 정보 포함).
