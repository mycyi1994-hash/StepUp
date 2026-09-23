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
 * 의상 목록.
 *
 * NFT 의상은 **아직 살 수 없다.** 의상을 사고파는 흐름(서버·결제·민팅)이
 * 없기 때문이다. 그래서 가격도 적지 않는다 — 팔지도 않는 물건에 값을
 * 매겨 두면 그 값이 사실처럼 읽힌다. 화면에는 "출시 예정"으로 나간다.
 */
object Outfits {
    val STARTER_HOODIE = Outfit(
        id = "starter_hoodie",
        top = 0xFF141A26, topShade = 0xFF0C1019, trim = 0xFF2F7BFF,
        shorts = 0xFF10151F, cap = 0xFF121826, capLogo = 0xFF3D8BFF,
        socks = 0xFFE8EEF8, longSleeve = true, nft = false, starter = true,
    )
    val STARTER_TEE = Outfit(
        id = "starter_tee",
        top = 0xFF1C2A4A, topShade = 0xFF121C33, trim = 0xFF5AA7FF,
        shorts = 0xFF10151F, cap = 0xFF1C2A4A, capLogo = 0xFFE8EEF8,
        socks = 0xFF1C2A4A, longSleeve = false, nft = false, starter = true,
    )
    val UP_HOODIE = Outfit(
        id = "up_hoodie",
        top = 0xFF0E1320, topShade = 0xFF070A12, trim = 0xFF22D3EE,
        shorts = 0xFF0E1320, cap = 0xFF0E1320, capLogo = 0xFF22D3EE,
        socks = 0xFF22D3EE, longSleeve = true, nft = true, starter = false,
    )
    val SPORTY_JACKET = Outfit(
        id = "sporty_jacket",
        top = 0xFFE9EEF6, topShade = 0xFFC3CCDA, trim = 0xFF1677FF,
        shorts = 0xFF1B2640, cap = 0xFFE9EEF6, capLogo = 0xFF1677FF,
        socks = 0xFFE9EEF6, longSleeve = true, nft = true, starter = false,
    )
    val NEON_TRACK = Outfit(
        id = "neon_track",
        top = 0xFF0B3A5C, topShade = 0xFF072740, trim = 0xFF5CF2FF,
        shorts = 0xFF0B3A5C, cap = 0xFF072740, capLogo = 0xFF5CF2FF,
        socks = 0xFF0B3A5C, longSleeve = false, nft = true, starter = false,
    )
    val STORM_SHELL = Outfit(
        id = "storm_shell",
        top = 0xFF3A4152, topShade = 0xFF262B38, trim = 0xFFFFC53D,
        shorts = 0xFF1C2029, cap = 0xFF262B38, capLogo = 0xFFFFC53D,
        socks = 0xFF1C2029, longSleeve = true, nft = true, starter = false,
    )

    val ALL = listOf(STARTER_HOODIE, STARTER_TEE, UP_HOODIE, SPORTY_JACKET, NEON_TRACK, STORM_SHELL)

    val DEFAULT = STARTER_HOODIE

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
