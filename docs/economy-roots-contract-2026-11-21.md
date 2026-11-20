# 경제의 뿌리 고치기 — 서버 ↔ 앱 약속 (서버 0119, 2026-11-21)

설계 · 까닭은 [economy-roots-2026-11-21.md](economy-roots-2026-11-21.md). 여기는 앱이 지켜야 할 서버 값 · 문구 · 예전 서버와의 어울림만 적는다.
함수 모양은 하나도 바뀌지 않았다. 새 함수는 `sneaker_points_preview` 하나, 새 값은 `shoe_rules_v4()` 의 `stats` 묶음과 `forge.curve` · `forge.level_growth`.

## 1. 규칙 묶음 — `shoe_rules_v4().stats`

```json
{"rule_version": "stats-2", "worn_rate": 0.30, "eff_wear_exp": 1, "eff_energy_exp": 1, "dur_repair_exp": 1,
 "wear_per_minute": 5, "energy_base": 10, "energy_per_level": 0, "steps_per_energy": 600, "respec_every": 5}
```

- 앱 값: `ShoeRules.stats`(`StatRule`). **묶음이 없으면 0119 전 서버 → `StatRule.LEGACY`**(닳은 뒤 적립 없음 · 효율이 수명 / 에너지를 안 건드림 · 레벨마다 에너지 +2 · 다시 나누기 없음).
  서버를 아직 못 읽었으면 `StatRule.FALLBACK`(0119 기본값 — `scripts/check-economy-twin.py` 가 서버와 맞대어 본다).
- 효율 포인트 배수 `u = 1 + point_rate_bps × pts_eff / 10000`(내구도 포인트 배수 `v` 도 같은 식).
  - 분당 닳음 = `wear_per_minute × u^eff_wear_exp` → **수명(분) = 내구도 ÷ (5 × u^eff_wear_exp)** — `StatRule.lifeSeconds / lifeMinutes / wearFor`.
  - 걸음당 에너지 = `(1 − 착화감) ÷ 600 × u^eff_energy_exp`(에너지 드링크는 그 위에 × 0.5).
  - 내구도 1점 수리비 = 예전 값 ÷ `u^eff_wear_exp` ÷ `v^dur_repair_exp` — **가득 수리비는 포인트를 어디에 넣든 같다**(뽑은 효율 · 뽑은 내구도가 정한다).
    신발마다 서버가 주는 1점 값(`my_sneakers` 의 수리 단가)이 정본이고, 앱의 셈은 `ShoeRules.repairUnit(효율, 등급, 효율 포인트, 내구도 포인트)`.
- 하루 에너지 최대치 = `energy_base + (가진 신발 중 최고 레벨 − 1) × energy_per_level` — 지금은 레벨과 상관없이 10. `energy_per_level` 이 0 이면
  "레벨이 오르면 에너지가 늘어요" 같은 글 · 숫자를 보이지 않는다. 최대치 · 남은 에너지의 정본은 서버(`my_economy`).

## 2. 러닝 정산 — `record_session`(모양 그대로)

- **가득 구간 / 닳은 구간**: 출발 때 남은 내구도로 달릴 수 있는 시간(내구도 × 12초 ÷ `u^eff_wear_exp`)까지의 걸음은 다 받고, 그 뒤의 걸음은 `worn_rate`(30%)만 받는다.
  내구도 0% 신발로 출발해도 된다(모든 걸음이 30%). 닳은 구간의 걸음도 에너지는 그대로 쓴다. `worn_rate` 는 0 ~ 0.45(그 위는 서버가 0.45 로 본다).
- **보상 걸음**(상자 · 목표 · 연속): 경로로 확인된 걸음 모두 — 신발이 닳았든 크루 체력이 비었든 센다. (`worn_rate` 가 0 이면 0118 처럼 내구도 몫만.)
- 닳은 구간이 있던 러닝의 `verdict_reason` 에 **`신발이 다 닳은 뒤의 걸음은 일부만 적립했습니다`** 가 붙는다(앱 `RunVerdictReason` 이 정확히 맞대어 본다).
  `worn_rate` 가 0 인 서버 · 0119 전 서버는 예전 문구(`내구도가 다 닳은 뒤의 걸음은 적립하지 않습니다` · `내구도가 다 닳아 적립이 없습니다`) 그대로 — 둘 다 알아야 한다.
- 닳는 양 = `5 × floor(달린 초 ÷ 60) × u^eff_wear_exp × 닳음 운`(크루 러닝은 0 그대로). 크루 러닝의 셈(공용 체력 · 보상)은 바뀌지 않았다.

## 3. 포인트 미리보기 — `sneaker_points_preview(p_id bigint) → jsonb`(로그인, 읽기만)

```json
{"ok": true,
 "basis": {"source": "MINE", "days": 6, "runs": 7, "avg_min": 42},
 "total": 77, "free": 12, "respec": true,
 "now": {"efficiency": 40, "comfort": 5, "durability": 20, "day_net": 151.2, "life_min": 38.5, "worn_runs": 3, "energy_out_days": 0},
 "per_point": {"efficiency": 0.42, "comfort": 0.0, "durability": 1.31},
 "life_min_per_point": {"efficiency": -0.3, "durability": 0.46},
 "comfort_capped": false,
 "recommend": {"efficiency": 2, "comfort": 0, "durability": 10, "day_net": 166.0, "life_min": 43.1},
 "recommend_all": {"efficiency": 22, "comfort": 6, "durability": 49, "day_net": 171.3, "life_min": 54.0}}
```

- `basis.source`: `MINE` = 내 지난 14일 혼자 러닝(무효 · 크루 빼고, 가장 최근 60번까지) · `DEFAULT` = 기록이 없어 "30분 달리기 하루 한 번"으로 봄.
- `day_net` = 달린 날 하루에 남는 SUP(적립 − 수리비, 운 · 배수 없이 평균). 러닝마다 신발은 가득에서 출발한다고 본다. **어림값이다 — "약"을 붙여 보인다.**
- `per_point` = 그 능력치에 1포인트를 더 넣으면 하루에 약 +N SUP(0 이면 지금 내 기록으로는 늘지 않는다). `comfort_capped` 면 착화감은 상한(에너지 70% 절약).
- `life_min` = 가득에서 달릴 수 있는 분(지금 배분) · `life_min_per_point` = 1포인트에 늘거나 주는 분(효율은 음수).
- `now.worn_runs` = 지난 러닝 중 신발 수명보다 길었던 횟수 · `now.energy_out_days` = 에너지가 먼저 끝난 날 수 — 안내 글에 쓴다.
- `recommend` = **남은 포인트**(`free`)를 나누는 추천(더할 양). `recommend_all` = 다시 나눌 수 있을 때(`respec`)만 — 모든 포인트를 처음부터 나눈 값(절대값), 아니면 null.
- 내 신발이 아니거나 예전 신발(IMPORT · MINT)이면 `{"ok": false, "code": "NO_POINTS"}`. 0119 전 서버에는 함수가 없다(PGRST202 / 404) — 미리보기 칸을 숨긴다.

## 4. 다시 나누기

- 5 · 10 · 15 … 레벨에 닿을 때마다 그 신발에 다시 나누기 한 번(`pts_respec = true` — `my_sneakers` 가 이미 준다). 낮추는 순간 쓴다(`sneaker_points_set` 그대로).
- 0119 를 올릴 때 한 번, 포인트를 나눈 모든 신발에 다시 나누기 한 번을 줬다(능력치 규칙이 바뀌어서).
- **내구도 포인트를 바꾸면 남은 내구도는 같은 비율로 바뀐다**(`sneaker_points_set` — 가득이면 가득 · 절반이면 절반 · 0 이면 0). 0119 전에는 늘어난 만큼 더하고 줄어든 만큼 뺐다
  (다 닳은 신발의 포인트를 뺐다 넣으면 공짜로 찼다). 앱의 미리보기 `PointsBasis.durabilityNow(draft)` 는 서버가 `stats` 묶음을 준 뒤에만 비율로 셈한다.

## 5. 강화 통

- `forge.curve` = `LINEAR`: 통 = Lv1 통 × (1 + `level_growth`(0.30) × (레벨 − 1)). `CLASSIC` 이면 0118 식. 통 · % 의 정본은 서버(`my_shoe_xp` · `forge_xp_quote`) — 앱이 통을 스스로 셈하는 곳이 있으면 이 굽이를 따른다.
- 예전 앱의 확률 강화 성공률도 같은 굽이로 내려갔다(서버 `forge_base_permille`) — 앱이 성공률을 스스로 셈하지 않는다(서버 견적).

## 6. 예전 서버 · 예전 앱

- 새 앱 + 0119 전 서버: `stats` 묶음 없음 → LEGACY 로 보이고, 미리보기 함수가 없으면 그 칸을 숨긴다. 닳은 신발 문구 · 수명은 예전 그대로.
- 예전 앱 + 0119 서버: 정산은 서버가 하므로 그대로 된다. 예전 앱은 0% 신발의 출발을 막거나 "적립 없음"이라고 말할 수 있다(화면 문구일 뿐 — 서버는 30% 적립).
  새 까닭 문구는 예전 앱이 모르는 문구다(그 줄을 보이지 않는다).
