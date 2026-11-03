package com.stepup.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.SolidColor

/**
 * StepUp 팔레트 — 밝은 바탕과 어두운 바탕 두 벌.
 *
 * 두 벌 모두 강조색은 블루 하나다. 바뀌는 것은 바닥과 글자이고, "무엇이
 * 중요한가"를 말하는 색은 테마와 무관하게 같아야 한다.
 *
 * ── 이름을 안 바꾼 이유 ──
 *
 * 화면 코드에는 Snow·Volt·Carbon 같은 이름이 1,200군데 넘게 박혀 있다. 이름을
 * 바꾸거나 `테마.색.snow` 꼴로 옮기면 화면 마흔 개를 동시에 고쳐야 하고,
 * 그러면 색을 바꾸려다 기능을 건드리게 된다. 이름은 **자리**를 가리키게 두고,
 * 그 이름이 지금 팔레트를 읽어 오게 했다 — 아래 게터들이 그 일을 한다.
 *
 * 이름들은 예전 딥 블랙·네온 라임 시절에 붙었다. 지금 각 이름이 무슨 자리인지는
 * [StepUpPalette] 의 필드 주석에 적어 둔다.
 */
@Immutable
class StepUpPalette(
    /** 앱 바닥 */
    val night: Color,
    /** 카드 표면 */
    val carbon: Color,
    /** 카드 안의 한 단계 위 — 칩·입력칸·보조 블록 */
    val carbonHigh: Color,
    /** 헤어라인 */
    val edge: Color,
    /** 주 강조 */
    val volt: Color,
    /** 강조 음영 — 그라데이션의 어두운 끝 */
    val voltDeep: Color,
    /** 강조 하이라이트 — 그라데이션의 밝은 끝 */
    val voltSoft: Color,
    /**
     * 강조색 **위에** 얹는 글자·아이콘.
     *
     * 두 테마 모두 흰색이다. 파란 버튼 위의 글자는 바닥이 희든 검든 흰색이라야
     * 읽힌다 — 이 자리를 [night] 와 같이 쓰면 다크에서 파란 버튼에 검은 글자가
     * 찍힌다.
     */
    val onVolt: Color,
    /** 종료 · 차감 · 경고 */
    val alert: Color,
    /** 제목과 본문 */
    val snow: Color,
    /** 보조 텍스트 */
    val silver: Color,
    /** 힌트 · 비활성 */
    val slate: Color,
    /**
     * 화면 전체를 덮어 눌러 주는 막.
     *
     * 두 테마 모두 **어둡다.** 막의 일은 뒤를 가리는 것이고, 그 위에는 흰
     * 글자와 밝은 패널이 뜬다. 이 자리에 [snow] 같은 테마 반전 색을 쓰면
     * 다크 테마에서 화면이 흰 막으로 덮이고, 그 위의 밝은 글자가 통째로
     * 사라진다.
     */
    val scrim: Color,
    /** 그 막을 얼마나 진하게 칠하는가 */
    val scrimAlpha: Float,
    /**
     * 막 **위에** 뜨는 패널의 바닥.
     *
     * 카드([carbon])와 다른 자리다. 카드는 앱 바닥 위에 뜨고, 이것은 어두운
     * 막 위에 뜬다 — 밝은 테마에서는 흰 패널, 어두운 테마에서는 막보다 한
     * 단계 들린 남색이라야 둘레가 읽힌다.
     */
    val overlay: Color,
    /**
     * 표면 **위에 글자로 얹는** 강조색.
     *
     * [volt] 는 버튼의 **바닥**이라 흰 글자가 얹힐 만큼 진해야 하고, 이것은
     * 반대로 어두운 표면 위에서 읽힐 만큼 밝아야 한다. 한 색이 둘을 겸하면
     * 어두운 테마에서 파란 글자가 카드에 묻는다.
     */
    val voltText: Color,
    /**
     * 시안 — 보조 강조. 발광하는 눈, 진행 막대의 밝은 끝, 선택된 탭의 빛.
     *
     * 블루가 "누르는 것"이라면 시안은 "빛나는 것"이다. 버튼 바닥에는 쓰지
     * 않는다 — 흰 글자가 얹히지 않는다.
     */
    val cyan: Color,
    /** 카드 표면 그라데이션의 위·아래 */
    val cardTop: Color,
    val cardBottom: Color,
    /** 앱 바닥 그라데이션의 위·아래 */
    val backdropTop: Color,
    val backdropBottom: Color,
    /** OSM 타일을 이 테마의 톤으로 옮기는 4x5 색 행렬 */
    mapMatrix: FloatArray,
    /** 다크 테마인가 — 상태바 아이콘과 워드마크를 고르는 데 쓴다 */
    val dark: Boolean,
    /** 타일 위 막(MapTone.shade)의 짙기 비율 — 어두운 테마는 막을 얇게 둔다 */
    val mapShadeScale: Float = 1f,
) {
    /** 진행 링 — 한 바퀴 돌며 강조색의 밝기가 굽이친다 */
    val voltSweep: Brush = Brush.sweepGradient(
        0.00f to voltDeep,
        0.30f to volt,
        0.62f to voltSoft,
        1.00f to voltDeep,
    )

    /** 버튼 면 — 통합안은 평평한 블루 한 색이다(번지는 빛 · 그라데이션 없음) */
    val voltPlate: Brush = SolidColor(volt)

    /** 강조 텍스트 채움 */
    val voltInk: Brush = Brush.verticalGradient(listOf(voltSoft, volt))

    /** 세로 강조 — 차트 바 */
    val voltVertical: Brush = Brush.verticalGradient(listOf(voltSoft, volt))

    /** 카드 표면 — 위가 미세하게 밝다 */
    val cardFill: Brush = Brush.verticalGradient(listOf(cardTop, cardBottom))

    /** 앱 배경 */
    val backdrop: Brush = Brush.verticalGradient(listOf(backdropTop, backdropBottom))

    /** 양끝이 사라지는 헤어라인 */
    val hairlineFade: Brush = Brush.horizontalGradient(
        listOf(Color.Transparent, snow.copy(alpha = if (dark) 0.14f else 0.08f), Color.Transparent),
    )

    val mapFilter: ColorFilter = ColorFilter.colorMatrix(ColorMatrix(mapMatrix))
}

/**
 * 화이트블루 — UI 최종 통합안(2026-10-25 개발 전달본 tokens.json colors.whiteBlue).
 *
 * 바닥은 G(나이키 × 애플)의 회색(#F2F2F7), 카드 · 묶음 목록은 흰색, 강조는 블루 하나(#1F5EFF). 제목은 검정.
 * 반투명 역할색(보조 글자 · 구분선)은 흰 카드 위에서 시안과 같은 값이 나오게 불투명으로 옮겼다(구분선만 반투명).
 */
val LightPalette = StepUpPalette(
    night = Color(0xFFF2F2F7),
    carbon = Color(0xFFFFFFFF),
    // 카드 안 한 단계 위 — 보조 버튼 · 입력칸 · 세그먼트 트랙(fill rgba(120,120,128,.14) 를 흰 바탕에 얹은 값)
    carbonHigh = Color(0xFFE9E9EE),
    // separator rgba(60,60,67,.16)
    edge = Color(0x293C3C43),
    volt = Color(0xFF1F5EFF),
    voltDeep = Color(0xFF1A4FE0),
    voltSoft = Color(0xFF5B8CFF),
    onVolt = Color(0xFFFFFFFF),
    alert = Color(0xFFFF3B30),
    snow = Color(0xFF000000),
    // secondaryLabel 자리 — 시안 값(#848489)은 흰 카드에서 3.7:1 이라 작은 글자 기준(4.5:1, PaletteContrastTest)에 못 미친다.
    // 같은 회색을 가장 옅은 면(#E9E9EE)에서도 4.5:1 이 되게 누른 값
    silver = Color(0xFF68686D),
    // tertiaryLabel 자리 — 힌트 · 안 고른 탭 이름. 가장 옅은 면에서도 3:1(면 기준), 보조 글자보다는 옅게
    slate = Color(0xFF838389),
    // 막 — 가이드 투어처럼 막 위에 흰 글자가 뜨는 자리. 시트 막은 StepUpUi.sheetScrim(더 옅다)
    scrim = Color(0xFF000000),
    scrimAlpha = 0.72f,
    overlay = Color(0xFFFFFFFF),
    voltText = Color(0xFF1F5EFF),
    // 흰 바탕에서는 밝은 시안이 날아가므로 한 단계 깊게
    cyan = Color(0xFF0891B2),
    // 통합안은 카드 · 바닥에 그라데이션을 두지 않는다(번지는 빛 금지)
    cardTop = Color(0xFFFFFFFF),
    cardBottom = Color(0xFFFFFFFF),
    backdropTop = Color(0xFFF2F2F7),
    backdropBottom = Color(0xFFF2F2F7),
    // 타일은 원래 밝은 지도다. 채도만 40%로 눌러 초록 공원과 노란 도로가
    // 경로의 파랑과 다투지 않게 하고, 전체를 조금 띄워 카드 바닥과 잇는다.
    mapMatrix = floatArrayOf(
        0.5276f, 0.4291f, 0.0433f, 0f, 18f,
        0.1276f, 0.8291f, 0.0433f, 0f, 18f,
        0.1276f, 0.4291f, 0.4433f, 0f, 18f,
        0f, 0f, 0f, 1f, 0f,
    ),
    dark = false,
)

/**
 * 다크블루 — UI 최종 통합안(tokens.json colors.darkBlue). 바닥은 A(나이키 런클럽)의 남색 검정 #070B14.
 *
 * 순검정이 아니라 푸른 기가 도는 검정이다. 카드 · 묶음 목록은 #0E1424, 강조는 블루 #2F6BFF, 표면 위 파란 글자는 #6E9BFF.
 */
// 블랙(2026-10-26 디자인 자료 — 사용자 결정 "시안처럼 검게"): 바닥 #000 · 면 #101216 · 선 #292E37 · 글자 #F6F7FA · 보조 #A7AFBC · 블루 #477DFF(버튼 바닥은 흰 글자 4:1 이 되게 #3D74FA)
val DarkPalette = StepUpPalette(
    night = Color(0xFF000000),
    carbon = Color(0xFF101216),
    // fill rgba(130,150,190,.20) 을 카드 위에 얹은 값 — 보조 버튼 · 입력칸 · 세그먼트 트랙
    carbonHigh = Color(0xFF1E2128),
    // separator rgba(255,255,255,.09)
    edge = Color(0xFF292E37),
    volt = Color(0xFF3D74FA),
    voltDeep = Color(0xFF3A6AE6),
    // 막 위 패널 테두리 · 스포트라이트 — 시안 #6E9BFF 를 55% 로 얹으면 막과 2.9:1 이라 한 단계 밝게(3:1, PaletteContrastTest)
    voltSoft = Color(0xFF7DA6FF),
    onVolt = Color(0xFFFFFFFF),
    alert = Color(0xFFFF453A),
    snow = Color(0xFFFFFFFF),
    // secondaryLabel rgba(226,232,245,.64)
    silver = Color(0xFFA7AFBC),
    // tertiaryLabel 자리(.38 보다 한 단계 밝게 — 카드 위 힌트가 읽히게)
    slate = Color(0xFF79808C),
    scrim = Color(0xFF000000),
    scrimAlpha = 0.78f,
    // 시트 · 막 위 패널 — groupedBackground
    overlay = Color(0xFF101216),
    voltText = Color(0xFF7DA2FF),
    cyan = Color(0xFF24D8FF),
    cardTop = Color(0xFF101216),
    cardBottom = Color(0xFF101216),
    backdropTop = Color(0xFF000000),
    backdropBottom = Color(0xFF000000),
    // 밝은 타일을 어두운 지도로 옮긴다(2026-10-14 "지도가 너무 어둡다") — 땅 #22324A 쯤 · 물 파랑(예전에는 뒤집혀 갈색이었다) ·
    // 길은 땅보다 어두운 선 · 지명은 밝은 글자. 실제 MapTiler 타일 색(땅 #F5F0E4 · 물 #85CAF9 · 길 흰색 · 공원 #D7E9C8 · 글자 회색)에 맞춘 값이다.
    mapMatrix = floatArrayOf(
        0.7577f, -1.861f, 0.0683f, 0f, 281.8f,
        0.1532f, -1.0204f, -0.1254f, 0f, 287.8f,
        0.0708f, -1.3206f, 0.3286f, 0f, 297.5f,
        0f, 0f, 0f, 1f, 0f,
    ),
    // 예전에는 타일 위에 바닥색 막을 30% 덮어 더 어두웠다 — 10% 남짓으로
    mapShadeScale = 0.35f,
    dark = true,
)

// ─────────────────────────────────────────────────────────────
// 지금 쓰는 팔레트
// ─────────────────────────────────────────────────────────────
//
// 스냅샷 상태라 값이 바뀌면 이 색을 읽은 컴포지션과 그리기가 저절로 다시
// 돈다. 화면마다 테마를 따로 쓰는 일은 없으므로 프로세스에 하나만 둔다.

private val active = mutableStateOf(LightPalette)

/** 지금 팔레트. 테마 판단(다크인가)이 필요할 때 읽는다. */
val StepUpColors: StepUpPalette get() = active.value

/** 테마가 정해지면 꽂는다. 같은 팔레트면 아무 일도 하지 않는다. */
fun applyPalette(palette: StepUpPalette) {
    if (active.value !== palette) active.value = palette
}

val Night: Color get() = active.value.night
val Carbon: Color get() = active.value.carbon
val CarbonHigh: Color get() = active.value.carbonHigh
val Edge: Color get() = active.value.edge
val Volt: Color get() = active.value.volt
val VoltDeep: Color get() = active.value.voltDeep
val VoltSoft: Color get() = active.value.voltSoft

/** 강조색 위에 얹는 글자·아이콘. 두 테마 모두 흰색이다. */
val OnVolt: Color get() = active.value.onVolt

/** 화면을 덮는 막과 그 위에 뜨는 패널 — 두 테마 모두 막은 어둡다 */
val Scrim: Color get() = active.value.scrim
val ScrimAlpha: Float get() = active.value.scrimAlpha
val Overlay: Color get() = active.value.overlay

/** 표면 위에 글자로 얹는 강조색. 버튼 바닥에는 [Volt] 를 쓴다. */
val VoltText: Color get() = active.value.voltText

/** 보조 강조 — 빛나는 것. 버튼 바닥에는 쓰지 않는다. */
val Cyan: Color get() = active.value.cyan

val Alert: Color get() = active.value.alert
val Snow: Color get() = active.value.snow
val Silver: Color get() = active.value.silver
val Slate: Color get() = active.value.slate
