package com.stepup.android.domain

/**
 * 러너 캐릭터 — 성별 · 의상 · 신발.
 *
 * ── 캐릭터는 NFT 가 아니다 ──
 *
 * 남녀 기본 캐릭터 둘은 누구나 처음부터 갖고 있고, 사고팔지 않는다. 얼굴과
 * 체형도 고정이다. 외형을 바꾸는 것은 **입히는 것**뿐이고, NFT 가 될 수
 * 있는 것은 그 입히는 것(의상·신발)이다. 그래서 이 파일에는 캐릭터의 가격이
 * 없다.
 *
 * ── 피부 ──
 *
 * 피부색은 성별과 무관하게 하나다([AvatarSkin]). 얼굴·귀·목·손·허벅지·
 * 무릎·종아리가 모두 이 한 값을 읽는다. 부위마다 색을 따로 두면 언젠가
 * 한 군데만 다른 색이 된다.
 */
enum class AvatarGender(val id: String) {
    MALE("M"),
    FEMALE("F");

    companion object {
        fun of(id: String?): AvatarGender = entries.firstOrNull { it.id == id } ?: MALE
    }
}

/**
 * 피부 — 짙은 블랙·네이비 한 벌. 남녀가 같다.
 *
 * 캐릭터 그림(res/drawable-nodpi/avatar_*.webp)이 이 색으로 그려져 있다.
 * 앱이 캐릭터를 직접 그리지는 않지만, 새 그림을 주문하거나 검수할 때의
 * 기준값으로 여기 남긴다.
 */
object AvatarSkin {
    /** 기본 피부 — 디자인 패키지의 #07111F */
    const val BASE: Long = 0xFF07111F

    /** 빛을 받는 쪽 — 같은 색상의 밝은 음영. 다른 피부색이 아니다. */
    const val LIGHT: Long = 0xFF14223A

    /** 가장자리 파란 조명 */
    const val RIM: Long = 0xFF3D8BFF

    /** 성별로 피부가 달라지지 않는다는 것을 코드로 못 박는다. */
    @Suppress("UNUSED_PARAMETER")
    fun of(gender: AvatarGender): Long = BASE
}

/**
 * 의상 한 벌 — 윗옷 · 반바지 · 모자 · 양말.
 *
 * 색은 ARGB Long 이다. 도메인이 Compose 에 기대지 않게 해 두면 단위
 * 테스트가 안드로이드 없이 돈다.
 */
data class Outfit(
    val id: String,
    /** 윗옷 */
    val top: Long,
    /** 윗옷 음영 */
    val topShade: Long,
    /** 줄무늬·지퍼·로고 */
    val trim: Long,
    val shorts: Long,
    val cap: Long,
    val capLogo: Long,
    val socks: Long,
    /** 긴소매면 팔뚝이 가려지고 손만 보인다 */
    val longSleeve: Boolean,
    /**
     * NFT 로 발행되는 의상인가.
     *
     * 기본 의상은 NFT 가 아니다 — 누구나 처음부터 입는다.
     */
    val nft: Boolean,
    /** 처음부터 갖고 있는가 */
    val starter: Boolean,
)

/**
 * 의상 목록 — 장비 카탈로그(design/equipment/equipment-catalog.json)와 같다.
 *
 *  * OUTFIT-BASE — 무료 기본 의상(남색 후드 · 반바지). 누구나 처음부터 입는다.
 *  * CLO-001 ~ CLO-005 — 새 의상. 상의 + 반바지가 한 벌이다(슬롯을 나누지 않는다).
 *
 * 새 의상은 **아직 살 수 없다.** 등급 · 가격 · 발행량 · 토큰 주소가 정해지지
 * 않았고 사고파는 흐름(서버 · 결제 · 민팅)도 없다. 그래서 값을 적지 않고 "출시
 * 예정"으로 두며, 보유하지 않았으니 미리보기만 된다(데모 모드에서는 체험 착용).
 *
 * 색은 상품 그림이 없을 때 쓰는 옷 아이콘([com.stepup.android.ui.components.GarmentArt])용이다.
 */
object Outfits {
    /** 기본 의상의 id. 예전부터 이 값으로 저장돼 있어 카탈로그의 "OUTFIT-BASE" 대신 그대로 쓴다. */
    const val BASE_ID = "starter_hoodie"

    val STARTER_HOODIE = Outfit(
        id = BASE_ID,
        top = 0xFF141A26, topShade = 0xFF0C1019, trim = 0xFF2F7BFF,
        shorts = 0xFF10151F, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFFE8EEF8, longSleeve = true, nft = false, starter = true,
    )
    /** 코어 집 — 남색 집업 후드 · 반바지, 코발트 대각선 패널과 청록 파이핑 */
    val CORE_ZIP = Outfit(
        id = "CLO-001",
        top = 0xFF141C33, topShade = 0xFF0B1122, trim = 0xFF1E5BFF,
        shorts = 0xFF111830, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFF1E5BFF, longSleeve = true, nft = true, starter = false,
    )
    /** 엠버 셸 — 검정 기술형 재킷 · 반바지, 주황 불꽃형 패널 */
    val EMBER_SHELL = Outfit(
        id = "CLO-002",
        top = 0xFF18181C, topShade = 0xFF0C0C10, trim = 0xFFFF6A1A,
        shorts = 0xFF141418, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFFFF6A1A, longSleeve = true, nft = true, starter = false,
    )
    /** 타이드 아노락 — 흰색 · 로열블루 반집업 아노락, 남색 반바지 */
    val TIDE_ANORAK = Outfit(
        id = "CLO-003",
        top = 0xFFEEF3FA, topShade = 0xFFC7D3E6, trim = 0xFF1F4FE0,
        shorts = 0xFF141C33, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFF1F4FE0, longSleeve = true, nft = true, starter = false,
    )
    /** 볼트 저지 — 차콜 반팔 저지 · 반바지, 노랑 번개와 연보라 파이핑 */
    val VOLT_JERSEY = Outfit(
        id = "CLO-004",
        top = 0xFF2E3038, topShade = 0xFF1C1E24, trim = 0xFFFFD12A,
        shorts = 0xFF26282E, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFFFFD12A, longSleeve = false, nft = true, starter = false,
    )
    /** 에어로 윈드브레이커 — 흰색 바람막이 · 연회색 반바지, 민트 곡선 패널 */
    val AERO_WINDBREAKER = Outfit(
        id = "CLO-005",
        top = 0xFFF4F6F8, topShade = 0xFFD2D9E0, trim = 0xFF3FE0C0,
        shorts = 0xFFC9CED6, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFF3FE0C0, longSleeve = true, nft = true, starter = false,
    )

    val ALL = listOf(STARTER_HOODIE, CORE_ZIP, EMBER_SHELL, TIDE_ANORAK, VOLT_JERSEY, AERO_WINDBREAKER)

    // Design samples belong to the existing trial mode, not the owned/NFT catalog.
    val SOFT_PINK = STARTER_HOODIE.copy(
        id = "STUDIO-PINK", top = 0xFFE7ADBD, topShade = 0xFFBA7E93,
        trim = 0xFFE7ADBD, shorts = 0xFFE7ADBD, cap = 0xFFE7ADBD, starter = false,
    )
    val SOFT_LAVENDER = STARTER_HOODIE.copy(
        id = "STUDIO-LAVENDER", top = 0xFFB5A0D9, topShade = 0xFF8270AE,
        trim = 0xFFB5A0D9, shorts = 0xFFB5A0D9, cap = 0xFFB5A0D9, starter = false,
    )
    val SOFT_OLIVE = STARTER_HOODIE.copy(
        id = "STUDIO-OLIVE", top = 0xFF8D9471, topShade = 0xFF626B4F,
        trim = 0xFF8D9471, shorts = 0xFF8D9471, cap = 0xFF8D9471, starter = false,
    )
    val STUDIO = listOf(SOFT_PINK, SOFT_LAVENDER, SOFT_OLIVE)
    val PREVIEWABLE = listOf(STARTER_HOODIE, TIDE_ANORAK, SOFT_PINK, CORE_ZIP,
        SOFT_LAVENDER, SOFT_OLIVE, EMBER_SHELL, VOLT_JERSEY, AERO_WINDBREAKER)

    val DEFAULT = STARTER_HOODIE

    /** 모르는 id(예전에 있던 기본 티셔츠 등)는 기본 의상으로 */
    fun of(id: String?): Outfit = ALL.firstOrNull { it.id == id } ?: DEFAULT
}

/**
 * 캐릭터가 지금 입고 있는 것 전부.
 *
 * 홈 · 러닝 완료 · 꾸미기 · 내 정보 · 커뮤니티가 **모두 이 한 값**을 받아
 * 그린다. 화면마다 따로 모으면 한 화면에서 갈아입힌 것이 다른 화면에
 * 안 보인다.
 *
 * @param shoe 신고 있는 NFT 신발. 없으면 기본 운동화를 그린다.
 * @param trial 데모 모드에서 **체험으로** 입힌 의상인가. 실제로 가진 것이
 *   아니므로 화면이 그 사실을 표시해야 한다.
 */
data class AvatarLook(
    val gender: AvatarGender = AvatarGender.MALE,
    val outfit: Outfit = Outfits.DEFAULT,
    val shoe: Sneaker? = null,
    val trial: Boolean = false,
)
