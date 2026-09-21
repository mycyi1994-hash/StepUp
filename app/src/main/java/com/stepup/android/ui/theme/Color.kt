package com.stepup.android.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * StepUp "Soft Balance" 팔레트.
 *
 * 화이트 캔버스 + 연한 블루 카드 + 로열 블루 단일 강조. 제목은 짙은 네이비로
 * 눌러 준다. 경계선은 거의 안 보일 만큼 옅고, 깊이는 색이 아니라 큰 곡률과
 * 여백에서 나온다.
 *
 * ── 이름을 안 바꾼 이유 ──
 *
 * 예전 팔레트는 딥 블랙 + 네온 라임이었고, 이름도 거기서 왔다(Night·Carbon·
 * Volt·Snow). 이름을 전부 바꾸면 화면 스무 개를 동시에 고쳐야 하고, 그러면
 * 색을 바꾸려다 기능을 건드리게 된다. 이름은 **자리**를 가리키게 두고 값만
 * 갈아 끼운다 — 각 이름이 지금 무슨 자리인지는 아래에 적어 둔다.
 */

// ── Canvas ───────────────────────────────────────────────────
/**
 * 가장 밝은 바닥 — 앱 배경, 그리고 **강조색 위에 얹는 글자색**.
 *
 * 예전에는 딥 블랙이었다. 두 자리 모두에서 뜻이 "강조색과 가장 대비되는 색"
 * 이었기 때문에, 라이트 테마에서는 둘 다 화이트가 맞다.
 */
val Night = Color(0xFFFFFFFF)

/** 카드 표면 — 배경보다 살짝 푸른 기가 도는 흰색 */
val Carbon = Color(0xFFF4F8FF)

/** 카드 안의 한 단계 위 — 칩·입력칸·보조 블록 */
val CarbonHigh = Color(0xFFE9F0FF)

/** 헤어라인 — 있는 듯 없는 듯한 경계 */
val Edge = Color(0xFFDCE6F7)

// ── Accent: 로열 블루 ─────────────────────────────────────────
/** 주 강조 */
val Volt = Color(0xFF145BFF)

/** 강조 음영 — 그라데이션의 어두운 끝 */
val VoltDeep = Color(0xFF0B3FCC)

/** 강조 하이라이트 — 그라데이션의 밝은 끝 */
val VoltSoft = Color(0xFF5B8CFF)

// ── Support ──────────────────────────────────────────────────
/** 종료 · 차감 · 경고 */
val Alert = Color(0xFFE5484D)

// ── Text ─────────────────────────────────────────────────────
/** 제목과 본문 — 짙은 네이비. 순검정은 흰 바닥에서 너무 딱딱하다. */
val Snow = Color(0xFF101D40)

/** 보조 텍스트 */
val Silver = Color(0xFF5A6A8C)

/** 힌트 · 비활성 */
val Slate = Color(0xFF93A1BE)
