# 러너 레벨 — 서버 ↔ 앱 약속 (2026-11-21, 서버 0121)

설계는 [stat-overhaul-2026-11-21.md](stat-overhaul-2026-11-21.md) 14장. 사용자 "계정 레벨은 복잡하게 하지 마 · 걸음 수로 레벨업 · 레벨 오르면 좋은 점" → "줘 · 패치해놔"(지난 걸음으로 채운 레벨에도 상자를 준다).

## 규칙 (서버가 셈한다)

- 러너 걸음 = 판정이 CLEAN 인 러닝의 보상 걸음(`walk_sessions.reward_steps`, 없으면 `backed_steps`) — 한국 날짜마다 48,000(`economy.max_daily_steps()`)까지 더한 합.
  보류 · 무효 · FLAGGED(과속 등) · 경로 없는 러닝은 0. 에너지를 다 쓴 뒤 · 크루 러닝 걸음도 든다.
- 러너 레벨 = 1 + 러너 걸음 ÷ 10,000(내림), 최대 30.
- 하루 에너지(서버 `economy.energy_max`) = 5.5 + 0.55 × (Lv − 1), 최대 16.5 — 에너지 1 = 600보라 포인트 없이 달리기(분당 165보) 기준 Lv1 20분 · 레벨마다 2분 · Lv21 60분.
  0121 전에 있던 계정은 예전 하루 에너지(10, 약 36분)보다 줄지 않는다(`legacy_energy`). 레벨은 걸음대로 보인다.
- 레벨이 오르면: 보통 상자(`draw_grants` kind `FREE`) 1 · 5레벨마다 상급 상자(`GIFT`) 1 더 — 원장 `economy.box_awards` 의 `RUNNER:<Lv>` · `RUNNER5:<Lv>`(한 번만).
  0121 을 올릴 때 지난 걸음으로 채운 레벨도 같은 상자를 받는다.
- 설정 `runner_level_rule` = `'ON'`(기본) · `'OFF'` 면 예전 에너지(가진 신발 가운데 가장 높은 레벨)로 돌아가고 상자도 멈춘다.

## `public.runner_me()` → jsonb

```json
{
  "enabled": true,
  "level": 7,
  "max_level": 30,
  "steps": 64210,
  "level_steps": 10000,
  "level_from": 60000,
  "next_at": 70000,
  "to_next": 5790,
  "energy_max": 10.0,
  "energy_minutes": 36,
  "level_energy_minutes": 32,
  "next_energy_minutes": 34,
  "energy_cap_level": 21,
  "legacy_floor": true,
  "badge": 0,
  "boxes": { "free": 6, "gift": 1 },
  "rewards": { "energy_minutes_per_level": 2, "free_every_level": 1, "gift_every": 5, "badge_levels": [10, 20, 30] }
}
```

- `level_from` · `next_at` = 이 레벨 · 다음 레벨이 시작되는 누적 걸음. 만렙이면 `next_at` · `to_next` · `next_energy_minutes` 는 `null`.
- `energy_max` = 지금 하루 에너지(단위) — `my_economy().energy_max` 와 같은 값. `energy_minutes` 는 그 값의 분(포인트 없이 달리기 기준, 반올림).
- `level_energy_minutes` = 레벨만으로 정해지는 분. `legacy_floor` 가 true 면 예전 보장 때문에 `energy_minutes` 가 더 크다.
  `next_energy_minutes` 는 다음 레벨의 레벨 에너지 분이다(보장보다 작으면 앱은 "+2분"을 말하지 않는다).
- `badge` = 10 · 20 · 30 가운데 닿은 가장 큰 것(없으면 0).
- `boxes` = 러너 레벨로 지금까지 받은 상자 수(보통 · 상급). 앱은 처음 본 값과 견줘 "지난 러닝으로 Lv N · 상자 N개"를 한 번 알린다.
- `enabled` 가 false 면 앱은 러너 레벨 칸을 숨긴다. 함수가 없으면(PGRST202) 숨긴다.

## `public.runner_badges(p_users uuid[])` → setof (user_id uuid, level int, badge int)

- 크루 명단 · 대기실에서 이름 옆 배지를 그릴 때. 한 번에 200명까지, 배지(Lv10 이상)가 있는 사람만 돌려준다. 로그인한 사람이면 누구나 부른다(레벨은 공개 값).
