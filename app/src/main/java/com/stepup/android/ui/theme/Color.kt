package com.stepup.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix

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
) {
    /** 진행 링 — 한 바퀴 돌며 강조색의 밝기가 굽이친다 */
    val voltSweep: Brush = Brush.sweepGradient(
        0.00f to voltDeep,
        0.30f to volt,
        0.62f to voltSoft,
        1.00f to voltDeep,
    )

    /** 큰 CTA 면 — START RUN 등 */
    val voltPlate: Brush = Brush.linearGradient(listOf(voltDeep, volt, voltSoft))

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
 * Soft Balance — 화이트 캔버스 + 연한 블루 카드 + 로열 블루 강조.
 * 제목은 짙은 네이비로 눌러 준다.
 */
val LightPalette = StepUpPalette(
    night = Color(0xFFFFFFFF),
    carbon = Color(0xFFF4F8FF),
    carbonHigh = Color(0xFFE9F0FF),
    edge = Color(0xFFDCE6F7),
    volt = Color(0xFF145BFF),
    voltDeep = Color(0xFF0B3FCC),
    voltSoft = Color(0xFF5B8CFF),
    onVolt = Color(0xFFFFFFFF),
    alert = Color(0xFFE5484D),
    snow = Color(0xFF101D40),
    silver = Color(0xFF5A6A8C),
    // 힌트·비활성 자리. 처음 #93A1BE 는 흰 바탕에서 2.6:1 이라 안 읽혔다.
    // 더 누르면 보조 텍스트(silver)보다 진해져 위계가 뒤집히므로, 흰 바탕
    // 4.5:1 에서 멈춘다 — 하단 탭의 안 고른 이름도 이 색이다.
    slate = Color(0xFF687796),
    scrim = Color(0xFF0B1326),
    scrimAlpha = 0.80f,
    overlay = Color(0xFFFFFFFF),
    voltText = Color(0xFF145BFF),
    cardTop = Color(0xFFFAFCFF),
    cardBottom = Color(0xFFF1F6FF),
    backdropTop = Color(0xFFFBFCFF),
    backdropBottom = Color(0xFFFFFFFF),
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
 * 딥 네이비 — 검은 바탕에 같은 블루.
 *
 * 순검정이 아니라 푸른 기가 도는 검정이다. 순검정 위의 파랑은 떠 보이고,
 * 카드와 바닥을 밝기만으로 나누면 층이 잘 안 읽힌다.
 */
val DarkPalette = StepUpPalette(
    night = Color(0xFF060A12),
    carbon = Color(0xFF101827),
    carbonHigh = Color(0xFF132038),
    edge = Color(0xFF203557),
    volt = Color(0xFF1677FF),
    voltDeep = Color(0xFF0B4FC4),
    voltSoft = Color(0xFF5AA7FF),
    onVolt = Color(0xFFFFFFFF),
    alert = Color(0xFFFF6B5E),
    snow = Color(0xFFF7FAFF),
    silver = Color(0xFFD7E2F2),
    slate = Color(0xFFA7B5CA),
    scrim = Color(0xFF01030A),
    scrimAlpha = 0.84f,
    // 막이 거의 검정이라 패널은 카드보다 한 단계 더 들어 올린다.
    overlay = Color(0xFF1D2C4C),
    // 어두운 표면 위의 파란 글자. volt 그대로면 3.5:1 이라 묻힌다.
    voltText = Color(0xFF5AA7FF),
    cardTop = Color(0xFF131C2E),
    cardBottom = Color(0xFF0D1422),
    backdropTop = Color(0xFF080D18),
    backdropBottom = Color(0xFF060A12),
    // 밝은 타일을 뒤집어 어두운 지도로 만든다. 채도를 낮춰 도로가 회색 계열로만
    // 남고, 그 위의 파란 경로가 화면에서 유일한 색이 된다.
    mapMatrix = floatArrayOf(
        -0.62f, -0.20f, -0.10f, 0f, 232f,
        -0.20f, -0.62f, -0.10f, 0f, 232f,
        -0.16f, -0.20f, -0.56f, 0f, 236f,
        0f, 0f, 0f, 1f, 0f,
    ),
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

val Alert: Color get() = active.value.alert
val Snow: Color get() = active.value.snow
val Silver: Color get() = active.value.silver
val Slate: Color get() = active.value.slate
