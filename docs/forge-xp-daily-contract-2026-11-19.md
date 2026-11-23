# 서버 0118 ↔ 앱 30 약속 (2026-11-19)

기획서 [forge-xp-daily-spec-2026-11-19.md](forge-xp-daily-spec-2026-11-19.md) 를 만들 때 서버와 앱이 맞추는 함수 · 칸. 서버에 0118 이 없으면(함수 없음) 앱은 예전 길로 돈다.
점수: 일반 신발 1켤레값 = 1,000점. %는 `floor(xp × 1000 ÷ need) ÷ 10`(소수 한 자리, 버림). 만렙이면 `need` 는 null.

## 강화

### `forge_xp_materials(p_target bigint) → jsonb`

```json
{ "ok": true, "rule_version": "forge-xp-1",
  "target": {"id": 1, "rarity": "RARE", "level": 3, "max_level": 20, "xp": 1200, "need": 3020, "pct": 39.7},
  "items": [ {"id": 9, "rarity": "COMMON", "level": 1, "model_id": null, "variant": 2,
              "q": 0.512, "stars": 2, "durability_pct": 100.0,
              "own": 1007, "inherit": 0, "repair_part": 0, "value": 1007, "pct": 33.3,
              "block": null, "precious": []} ],
  "crit": {"chance": 0.10, "mult": 2}, "max_materials": 10 }
```

- `items`: 대상 말고 내 신발 모두. 막히지 않은 것(값 낮은 것부터) → 막힌 것.
- `block`: `EQUIPPED` · `LOCKED`(첫 신발 · 옛 신발) · `LISTED` · `ON_CHAIN` · `LEVEL` · `IN_ROOM`(대기실에서 고름 · 끝나지 않은 크루 러닝의 신발) · `NO_VALUE`(닳아서 0) · null.
- `precious`: `HIGHER_GRADE` · `LEVELED`(Lv2 이상) · `STARS3` · `LAST_MODEL`(도감에 하나뿐) · `SELL_UNLOCKED`(잠금 거리를 채움) · `TOP_LEVEL`(이 신발을 빼면 하루 에너지가 준다) · `TOP_EFFICIENCY`(빼면 하루 상한이 준다). 이름 붙인 신발은 앱이 더한다(폰에만 있다).
- 거절: `{"error": "TARGET_GONE" | "TARGET_UNAVAILABLE" | "LEGACY" | "MAX_LEVEL" | "RUNNING"}` — `RUNNING` = 내가 크루 러닝 중.

### `forge_xp_quote(p_target bigint, p_materials bigint[]) → jsonb`

```json
{ "ok": true, "rule_version": "forge-xp-1", "quote_version": "md5…",
  "before": {"level": 3, "xp": 1200, "need": 3020, "pct": 39.7},
  "after":  {"level": 4, "xp": 320, "need": 3140, "pct": 10.1, "levels": 1, "overflow": 0},
  "gain": 2140,
  "materials": [{"id": 9, "own": 1007, "inherit": 0, "value": 1007, "pct": 33.3}],
  "crit": {"chance": 0.10, "mult": 2},
  "energy": {"before": 28, "after": 28}, "cap": {"before": 1.36, "after": 1.36},
  "max_level": 20, "overflow_possible": false }
```

- `after` 는 대성공 없이 받는 **최소**. 결과는 그 이상.
- 재료 1 ~ `max_materials` 켤레, 서로 달라야 한다.
- 거절: `MATERIALS_INVALID` · `MATERIAL_UNAVAILABLE`(+ `"unavailable": [{"id", "block"}]`) · `NO_VALUE`(+ `"ids"`) · 대상 거절(위와 같다).

### `forge_xp_add(p_key uuid, p_target bigint, p_materials bigint[], p_quote text) → jsonb`

```json
{ "ok": true, "kind": "XP", "request_key": "…", "target_id": 1,
  "level_before": 3, "level_after": 4, "levels": 1,
  "gain": 2247, "xp_after": 427, "need_after": 3140, "pct_after": 13.5, "overflow": 0,
  "materials": [{"id": 9, "own": 1007, "inherit": 0, "crit": true, "value": 2014}],
  "crits": 1, "level_boxes": [5] }
```

- 같은 `p_key` 로 다시 부르면 같은 결과. 다른 대상 · 재료면 `KEY_REUSED`. 견적이 바뀌었으면 `{"error": "QUOTE_CHANGED", "quote": {…}}`.
- `forge_xp_result(p_key uuid)` — 그 요청의 결과(없으면 null). 응답을 잃었을 때.
- `level_boxes`: 이 강화로 받은 레벨 상자(5 · 10 …).

### `my_shoe_xp() → jsonb`

`[{"id": 1, "xp": 1200, "need": 3020, "pct": 39.7}]` — 내 신발마다(만렙이면 `need` null · `pct` null).

### `shoe_rules_v4()` 에 더하는 칸

```json
"forge": {"rule_version": "forge-xp-1", "step": 3.1, "need": 2.88, "spec": [0.7, 1.3],
          "crit_chance": 0.10, "crit_mult": 2, "inherit": 0.5, "sup_per_unit": 24, "max_materials": 10},
"goal":  {"min": 4000, "max": 20000, "default": 6000, "step": 500}
```

### 예전 길(예전 앱)

`forge_quote` · `forge_start` · `forge_materials` · `forge_result` 모양 그대로. 재료는 내구도 가득만(`WORN`). 만렙에 닿으면 통을 비운다.
`my_forge_successes()` 는 오른 레벨 수의 합(모양 그대로).

## 오늘의 보상

### 보상 걸음

러닝 하나의 `walk_sessions.reward_steps` = 경로가 받쳐 주고 · 무효 · 보류가 아니고 · 신발 내구도가 남아 있는 동안의 확인된 걸음(크루 러닝은 공용 체력이 받쳐 준 몫). 하루 48,000 까지.
상자 · 주간 · 목표 · 연속 · 스텝 서지 · 이벤트의 "오늘 걸음 목표"가 모두 이 숫자. 경로가 받쳐 주지 않아 0 이면 판정 까닭에 **"경로가 확인되지 않아 보상 걸음에 들지 않았습니다"**(이 글자 그대로).

### `box_status()` 에 더하는 칸

예전 칸은 그대로(`steps_today` · `steps_week` 의 값만 보상 걸음으로 바뀐다).

```json
"rule": "REWARD_STEPS",
"reward_steps_today": 3240,
"goal": 8000, "goal_next": null,
"goal_paid": null, "goal_bonus": 24.0,
"streak": 3, "streak_best": 9, "streak_box_in": 4,
"ticks": [{"steps": 2000, "kind": "BOX", "got": true}, {"steps": 4000, "kind": "BOX", "got": false},
          {"steps": 8000, "kind": "GOAL", "got": false}],
"surge": {"steps": 21000, "target": 80000, "reward": 250, "claimed": false},
"recent": [{"day": "2026-11-19", "kind": "RUN_BOX", "amount": null, "at": "2026-11-19T09:12:00+00"}]
```

- `goal` 오늘의 목표 · `goal_next` 내일부터 바뀔 목표(바꾼 게 없으면 null).
- `goal_paid` 오늘 받은 SUP(안 받았으면 null) · `goal_bonus` 오늘 채우면 받을 SUP(연속 포함).
- `streak` 오늘 채우면 며칠째 · `streak_best` 가장 긴 연속 · `streak_box_in` 매일 채우면 상급 상자까지 남은 날(0 = 오늘 채우면).
- `recent` 최근 3일에 받은 것: `RUN_BOX` · `WEEK_BOX` · `STREAK_BOX` · `LEVEL_BOX` · `GOAL`(`amount` SUP). `at` 으로 앱이 "새로 받은 것"을 가린다.

### 목표

- `profile_set_daily_goal(p_goal) → int` — 4,000 ~ 20,000 · 500 단위로 맞춰 적고 그 값을 돌려준다. **오늘 보상 걸음이 0 이 아니면 오늘 목표는 그대로**(`goal_next` 에 보인다).
- 목표 보너스는 러닝을 기록할 때 서버가 바로 준다. `goal_claim` · `goal_claim_day` 는 모양 · 거절 그대로(이미 받았으면 23505).
