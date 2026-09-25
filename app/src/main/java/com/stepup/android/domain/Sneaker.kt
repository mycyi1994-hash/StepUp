package com.stepup.android.domain

import kotlin.random.Random

/**
 * StepUp 스니커즈 NFT — **속성(Faction) × 등급(Rarity) × 변형(Variant)** 체계.
 *
 * 속성 4개(불·물·번개·바람) × 등급별 변형 13종 = 총 52종.
 * 속성이 색과 이펙트를, 등급이 실루엣 정교함·오너먼트·부스트를 정한다.
 *
 * 적립 보너스는 켤레마다 도감 시트에 적혀 있다(SneakerDesigns). 가장 높은
 * 전설도 +2.55% 라 작게 잡혀 있다 — 수집의 재미는 성능 격차가 아니라 외형과
 * 도감 완성에서 나온다.
 */

// ─────────────────────────────────────────────────────────────
// 속성
// ─────────────────────────────────────────────────────────────

enum class Faction(
    val id: String,
    val displayName: String,
    /** 주 네온색 */
    val accent: Int,
    /** 밝은 하이라이트 */
    val accentSoft: Int,
    /** 음영 */
    val accentDeep: Int,
    /** 어퍼 기본색 */
    val upper: Int,
    /** 어퍼 음영 */
    val upperShade: Int,
    /** 밑창색 */
    val sole: Int,
) {
    FIRE(
        "FIRE", "Fire",
        accent = 0xFFFF2E2E.toInt(),
        accentSoft = 0xFFFF9166.toInt(),
        accentDeep = 0xFF9E1010.toInt(),
        upper = 0xFF17100F.toInt(),
        upperShade = 0xFF0D0808.toInt(),
        sole = 0xFF241416.toInt(),
    ),
    WATER(
        "WATER", "Water",
        accent = 0xFF2E9BFF.toInt(),
        accentSoft = 0xFFA6E4FF.toInt(),
        accentDeep = 0xFF0F4FA8.toInt(),
        upper = 0xFF0F151E.toInt(),
        upperShade = 0xFF080C12.toInt(),
        sole = 0xFF15202E.toInt(),
    ),
    LIGHTNING(
        "LIGHTNING", "Lightning",
        accent = 0xFFF5E800.toInt(),
        accentSoft = 0xFFFFFA9E.toInt(),
        accentDeep = 0xFFA89C00.toInt(),
        upper = 0xFF15140D.toInt(),
        upperShade = 0xFF0B0A06.toInt(),
        sole = 0xFF201E12.toInt(),
    ),
    WIND(
        "WIND", "Wind",
        accent = 0xFFA4F515.toInt(),
        accentSoft = 0xFFDCFF8F.toInt(),
        accentDeep = 0xFF5F9400.toInt(),
        upper = 0xFF10150C.toInt(),
        upperShade = 0xFF080B06.toInt(),
        sole = 0xFF182014.toInt(),
    );

    companion object {
        fun of(id: String): Faction = entries.firstOrNull { it.id == id } ?: FIRE
    }
}

// ─────────────────────────────────────────────────────────────
// 등급
// ─────────────────────────────────────────────────────────────

enum class Rarity(
    val id: String,
    /** 이 등급에 속한 변형 개수 */
    val variantCount: Int,
    /** 적립 부스트 (%) — 등급 한 단계당 +1% */
    val boostPercent: Double,
    val maxLevel: Int,
    /** 민팅 가중치 */
    val weight: Int,
) {
    COMMON("COMMON", variantCount = 4, boostPercent = 0.0, maxLevel = 10, weight = 55),
    RARE("RARE", variantCount = 4, boostPercent = 1.0, maxLevel = 15, weight = 28),
    EPIC("EPIC", variantCount = 3, boostPercent = 2.0, maxLevel = 20, weight = 13),
    LEGENDARY("LEGENDARY", variantCount = 2, boostPercent = 3.0, maxLevel = 30, weight = 4);

    companion object {
        fun of(id: String): Rarity = entries.firstOrNull { it.id == id } ?: COMMON

        /** 가중 추첨. luck이 높을수록 상위 등급 가중치가 커진다. */
        fun roll(random: Random, luck: Double = 0.0): Rarity {
            val weights = entries.map { r ->
                r to r.weight * (1.0 + luck * 0.15 * r.ordinal)
            }
            val total = weights.sumOf { it.second }
            var pick = random.nextDouble() * total
            for ((rarity, w) in weights) {
                pick -= w
                if (pick <= 0) return rarity
            }
            return COMMON
        }
    }
}

/** 속성 하나가 가진 변형 개수 (2+3+4+4 = 13) */
val VARIANTS_PER_FACTION: Int = Rarity.entries.sumOf { it.variantCount }

/** 전체 도감 크기 = 속성 4 × 변형 13 = 52 */
val TOTAL_COLLECTION: Int = Faction.entries.size * VARIANTS_PER_FACTION

// ─────────────────────────────────────────────────────────────
// 도감 — 속성마다 13종, 모두 52종
// ─────────────────────────────────────────────────────────────

/**
 * 도감 한 칸.
 *
 * 값을 규칙으로 계산하지 않고 **한 켤레씩 적어 둔다.** 도감 시트에 찍힌
 * 숫자가 곧 이 표이고, 규칙으로 만들면 시트와 앱이 조금씩 어긋난다.
 *
 * @property code 도감에 적힌 번호 — "FIR-001"
 * @property boostPercent 레벨 1에서의 적립 보너스(%). 레벨이 오르면 더해진다.
 * @property luck 행운. 강화로 조금씩 오르므로 이 값은 **기준값**이다.
 * @property englishName 로그·알림에 쓰는 이름. 화면은 현지화 문자열을 쓴다.
 */
data class SneakerDesign(
    val code: String,
    val faction: Faction,
    val rarity: Rarity,
    val variant: Int,
    val index: Int,
    val boostPercent: Double,
    val luck: Double,
    val englishName: String,
)

/**
 * 52종 전체.
 *
 * 속성 안에서의 번호(1..13)는 등급 순이다 — 레전더리 2, 희귀 3, 레어 4,
 * 일반 4. 이 차례가 도감 시트의 차례이자 그림 파일 이름의 차례다.
 */
object SneakerDesigns {

    /** 등급·변형 → 속성 안에서의 번호(1부터) */
    fun indexOf(rarity: Rarity, variant: Int): Int = when (rarity) {
        Rarity.LEGENDARY -> 1 + variant
        Rarity.EPIC -> 3 + variant
        Rarity.RARE -> 6 + variant
        Rarity.COMMON -> 10 + variant
    }

    /** 번호(1..13) → 등급·변형. [indexOf] 의 반대. */
    fun slotOf(index: Int): Pair<Rarity, Int> = when {
        index <= 2 -> Rarity.LEGENDARY to index - 1
        index <= 5 -> Rarity.EPIC to index - 3
        index <= 9 -> Rarity.RARE to index - 6
        else -> Rarity.COMMON to index - 10
    }

    /** 도감 번호의 앞자리 — 시트에 적힌 그대로 */
    private val prefix = mapOf(
        Faction.FIRE to "FIR",
        Faction.WATER to "WAT",
        Faction.LIGHTNING to "LIT",
        Faction.WIND to "WND",
    )

    private fun row(index: Int, name: String, boost: Double, luck: Double) =
        Triple(index, name, boost to luck)

    private val raw: Map<Faction, List<Triple<Int, String, Pair<Double, Double>>>> = mapOf(
        Faction.FIRE to listOf(
            row(1, "INFERNO CROWN", 2.55, 1.36),
            row(2, "PHOENIX SURGE", 2.35, 1.33),
            row(3, "MAGMA PULSE", 1.70, 1.24),
            row(4, "EMBER NOVA", 1.60, 1.22),
            row(5, "CINDER VORTEX", 1.50, 1.20),
            row(6, "HEAT RUNNER", 1.05, 1.15),
            row(7, "TORCH SPRINT", 0.95, 1.14),
            row(8, "ASH BLAZE", 0.85, 1.12),
            row(9, "FLARE SHIFT", 0.75, 1.10),
            row(10, "SPARK RUNNER", 0.55, 1.08),
            row(11, "CORE RUNNER", 0.50, 1.07),
            row(12, "REDLINE", 0.45, 1.06),
            row(13, "WARM UP", 0.40, 1.05),
        ),
        Faction.WATER to listOf(
            row(1, "ABYSS TIDE", 2.35, 1.34),
            row(2, "LEVIATHAN FLOW", 2.15, 1.31),
            row(3, "TSUNAMI ARC", 1.50, 1.22),
            row(4, "AZURE CURRENT", 1.40, 1.20),
            row(5, "DEEPWAVE PRIME", 1.30, 1.18),
            row(6, "WATER RUNNER", 0.85, 1.13),
            row(7, "RIPPLE PACE", 0.75, 1.11),
            row(8, "REEF GLIDE", 0.65, 1.10),
            row(9, "OCEAN SPRINT", 0.55, 1.08),
            row(10, "BLUE RUNNER", 0.35, 1.06),
            row(11, "BROOK RUNNER", 0.30, 1.05),
            row(12, "MIST RUNNER", 0.25, 1.04),
            row(13, "TIDE LITE", 0.20, 1.03),
        ),
        Faction.LIGHTNING to listOf(
            row(1, "THUNDER ZENITH", 2.50, 1.37),
            row(2, "VOLT PHANTOM", 2.30, 1.34),
            row(3, "STORM CIRCUIT", 1.65, 1.25),
            row(4, "VOLT APEX", 1.55, 1.23),
            row(5, "PLASMA RUNNER", 1.45, 1.21),
            row(6, "THUNDER RUNNER", 1.00, 1.16),
            row(7, "VOLT DASH", 0.90, 1.15),
            row(8, "FLASH PACE", 0.80, 1.13),
            row(9, "SPARK BLADE", 0.70, 1.11),
            row(10, "YELLOW RUNNER", 0.50, 1.09),
            row(11, "STATIC RUNNER", 0.45, 1.08),
            row(12, "PULSE RUNNER", 0.40, 1.07),
            row(13, "CHARGE LITE", 0.35, 1.06),
        ),
        Faction.WIND to listOf(
            row(1, "ZEPHYR CROWN", 2.40, 1.35),
            row(2, "TEMPEST WING", 2.20, 1.32),
            row(3, "AERO PHANTOM", 1.55, 1.23),
            row(4, "GALE ORBIT", 1.45, 1.21),
            row(5, "CYCLONE STEP", 1.35, 1.19),
            row(6, "WIND RUNNER", 0.90, 1.14),
            row(7, "BREEZE GLIDE", 0.80, 1.13),
            row(8, "AIR DASH", 0.70, 1.11),
            row(9, "SKY PACE", 0.60, 1.09),
            row(10, "CLOUD RUNNER", 0.40, 1.07),
            row(11, "FEATHER RUNNER", 0.35, 1.06),
            row(12, "DRIFT RUNNER", 0.30, 1.05),
            row(13, "LIFT LITE", 0.25, 1.04),
        ),
    )

    val all: List<SneakerDesign> = raw.flatMap { (faction, rows) ->
        rows.map { (index, name, stats) ->
            val (rarity, variant) = slotOf(index)
            SneakerDesign(
                code = "${prefix[faction]}-%03d".format(index),
                faction = faction,
                rarity = rarity,
                variant = variant,
                index = index,
                boostPercent = stats.first,
                luck = stats.second,
                englishName = name,
            )
        }
    }

    private val bySlot: Map<String, SneakerDesign> =
        all.associateBy { "${it.faction.id}:${it.rarity.id}:${it.variant}" }

    /**
     * 한 칸을 찾는다.
     *
     * 없는 조합이 들어오면 그 등급의 첫 칸으로 떨어뜨린다 — 옛 저장값이
     * 범위를 벗어나더라도 화면이 비지 않게 하려는 것이다.
     */
    fun of(faction: Faction, rarity: Rarity, variant: Int): SneakerDesign {
        val v = variant.coerceIn(0, rarity.variantCount - 1)
        return bySlot["${faction.id}:${rarity.id}:$v"]
            ?: bySlot["${faction.id}:${rarity.id}:0"]
            ?: all.first()
    }
}

// ─────────────────────────────────────────────────────────────
// 실루엣
// ─────────────────────────────────────────────────────────────

enum class StripeStyle { SWOOSH, BLADE, CHEVRON, DUAL, WAVE, SPLIT }

/**
 * 등급·변형마다 다른 신발 형태.
 * 상위 등급일수록 밑창이 두껍고 발광 포드가 늘어나며 하이탑이 섞인다.
 */
data class Silhouette(
    val soleThickness: Float,
    val toeRise: Float,
    val collarTop: Float,
    val stripe: StripeStyle,
    /** 미드솔 발광 포드 개수 */
    val pods: Int,
    val highTop: Boolean,
)

/**
 * 등급·변형별 모델 키. 화면에서는 이 키에 대응하는 현지화 문자열을 쓰고,
 * 여기 영문 값은 로그·저장용 식별자로만 남긴다.
 */
object VariantNames {
    /**
     * 이름은 속성마다 다르다 — 불의 전설은 "인페르노 크라운", 물의 전설은
     * "어비스 타이드"다. 등급만으로는 정할 수 없으므로 속성을 함께 받는다.
     */
    fun of(faction: Faction, rarity: Rarity, variant: Int): String =
        SneakerDesigns.of(faction, rarity, variant).englishName
}

object Silhouettes {
    private val common = listOf(
        Silhouette(0.098f, 0.026f, 0.150f, StripeStyle.SWOOSH, 2, false),
        Silhouette(0.108f, 0.023f, 0.145f, StripeStyle.DUAL, 2, false),
        Silhouette(0.116f, 0.020f, 0.140f, StripeStyle.BLADE, 2, false),
        Silhouette(0.132f, 0.034f, 0.132f, StripeStyle.CHEVRON, 3, false),
    )
    private val rare = listOf(
        Silhouette(0.126f, 0.030f, 0.136f, StripeStyle.DUAL, 3, false),
        Silhouette(0.134f, 0.027f, 0.131f, StripeStyle.SWOOSH, 3, false),
        Silhouette(0.142f, 0.024f, 0.126f, StripeStyle.WAVE, 3, false),
        Silhouette(0.150f, 0.040f, 0.118f, StripeStyle.SPLIT, 4, false),
    )
    private val epic = listOf(
        Silhouette(0.138f, 0.032f, 0.128f, StripeStyle.SWOOSH, 4, false),
        Silhouette(0.156f, 0.026f, 0.108f, StripeStyle.SPLIT, 4, true),
        Silhouette(0.168f, 0.044f, 0.100f, StripeStyle.CHEVRON, 5, true),
    )
    private val legendary = listOf(
        Silhouette(0.160f, 0.038f, 0.112f, StripeStyle.WAVE, 5, false),
        Silhouette(0.178f, 0.048f, 0.096f, StripeStyle.SPLIT, 6, true),
    )

    fun of(rarity: Rarity, variant: Int): Silhouette {
        val list = when (rarity) {
            Rarity.COMMON -> common
            Rarity.RARE -> rare
            Rarity.EPIC -> epic
            Rarity.LEGENDARY -> legendary
        }
        return list[variant.coerceIn(0, list.size - 1)]
    }
}

// ─────────────────────────────────────────────────────────────
// 스니커즈
// ─────────────────────────────────────────────────────────────

data class Sneaker(
    val id: Long,
    val faction: Faction,
    val rarity: Rarity,
    /** 등급 내 변형 인덱스 (0부터) */
    val variant: Int,
    val level: Int,
    val mintNumber: Int,
    /** 행운 — 민팅 시 상위 등급 확률 보정 (1.00 ~ 1.60) */
    val luck: Double,
    /** 착화감 — 에너지 소모 절감 (1.00 ~ 1.40) */
    val comfort: Double,
    val durability: Int,
    val equipped: Boolean,
    val acquiredAt: Long,
    /** 서버가 준 스탯. 비어 있으면(origin == "") 폰에만 있던 옛 신발이다. */
    val server: ServerStats? = null,
) {
    val silhouette: Silhouette get() = Silhouettes.of(rarity, variant)

    /** 이 신발이 도감의 어느 칸인지 */
    val design: SneakerDesign get() = SneakerDesigns.of(faction, rarity, variant)

    /** 도감 번호 — "FIR-001" */
    val code: String get() = design.code

    /** 모델명 — "Inferno Crown" */
    val variantName: String get() = design.englishName

    /** 전체 이름 — "Fire Apex". 알림·토스트처럼 Composable 밖에서 쓴다. */
    val displayName: String get() = "${faction.displayName} $variantName"

    /**
     * 적립 보너스(%).
     *
     * 기본값은 도감에 적힌 값 그대로이고, 레벨 한 칸에 +0.5%가 더해진다.
     * 규칙으로 다시 계산하지 않는 이유는, 그러면 도감 시트에 찍힌 숫자와
     * 앱이 보여 주는 숫자가 어긋나기 때문이다.
     */
    val boostPercent: Double
        get() = design.boostPercent + (level - 1).coerceAtLeast(0) * 0.5

    /**
     * 적립 배수. 서버 신발이면 서버 계산(0024 record_session)과 같다 —
     * (1 + 효율) × 내구도 계수. 예상치일 뿐이고 실제 적립은 서버가 정한다.
     */
    val earningMultiplier: Double
        get() = server?.let { (1.0 + it.efficiencyBps / 10_000.0) * it.durabilityFactor }
            ?: (1.0 + boostPercent / 100.0)

    /** 에너지를 덜 쓰는 비율. 서버 신발은 착화감(bps)만큼, 옛 신발은 최대 15% */
    val energyEfficiency: Double
        get() = server?.let { 1.0 - it.comfortBps / 10_000.0 }
            ?: (1.0 - ((comfort - 1.0) * 0.375).coerceIn(0.0, 0.15))

    val upgradeCost: Double
        get() = server?.upgradeCost?.takeIf { it > 0 } ?: RewardEconomy.sneakerUpgradeCost(level, rarity)

    val maxLevel: Int get() = server?.maxLevel?.takeIf { it > 0 } ?: rarity.maxLevel

    /** 서버가 강화를 받아 주는가 — 폰에서 올린 옛 신발(IMPORT · MINT)과 판매 중인 신발은 아니다 */
    val canUpgrade: Boolean
        get() = level < maxLevel && (server == null || (server.upgradable && server.status == "OWNED"))

    /** 가득 채우는 수리 비용. 수리할 곳이 없으면 0 */
    val repairCost: Double
        get() = server?.let { ((100.0 - it.durabilityPts).coerceAtLeast(0.0) * it.repairCostPerPoint) } ?: 0.0

    /** 도감 슬롯 식별자 */
    val slotKey: String get() = "${faction.id}:${rarity.id}:$variant"
}

/**
 * 서버 신발의 스탯 (my_sneakers). 등급 · 레벨 · 효율 · 착화감 · 내구도.
 */
data class ServerStats(
    val origin: String,
    val efficiencyBps: Int,
    val comfortBps: Int,
    val durabilityPts: Double,
    val maxLevel: Int,
    val status: String,
    val chainState: String,
    val canWithdraw: Boolean,
    val upgradeCost: Double,
    val repairCostPerPoint: Double,
    val genesisNo: Int,
) {
    /** 내구도 계수 — 50 이상 1, 20 이상 0.7, 그 아래 0 (서버 economy.durability_factor) */
    val durabilityFactor: Double
        get() = when {
            durabilityPts >= 50 -> 1.0
            durabilityPts >= 20 -> 0.7
            else -> 0.0
        }

    /** 폰에서 올린 옛 신발(IMPORT · MINT)은 서버가 강화 · 수리를 받지 않는다 */
    val upgradable: Boolean get() = origin !in setOf("IMPORT", "MINT")
}

/** 민팅기 */
object SneakerMint {

    private fun rollStat(random: Random, rarity: Rarity, span: Double): Double {
        // 상위 등급일수록 좋은 롤이 나오되, 차이는 완만하게
        val base = random.nextDouble() * span
        val bonus = rarity.ordinal * span * 0.12
        return 1.0 + base + bonus
    }

    fun mint(
        random: Random,
        mintNumber: Int,
        luck: Double = 0.0,
        forcedRarity: Rarity? = null,
        forcedFaction: Faction? = null,
    ): Sneaker {
        val rarity = forcedRarity ?: Rarity.roll(random, luck)
        val faction = forcedFaction ?: Faction.entries[random.nextInt(Faction.entries.size)]
        val variant = random.nextInt(rarity.variantCount)
        return Sneaker(
            id = 0,
            faction = faction,
            rarity = rarity,
            variant = variant,
            level = 1,
            mintNumber = mintNumber,
            // 행운은 굴리지 않는다. 도감에 켤레마다 값이 적혀 있으므로
            // 같은 모델인데 사람마다 다른 값이 나오면 그 표가 거짓이 된다.
            luck = SneakerDesigns.of(faction, rarity, variant).luck,
            comfort = rollStat(random, rarity, 0.30),
            durability = 100,
            equipped = false,
            acquiredAt = System.currentTimeMillis(),
        )
    }

    /** 첫 실행 시 지급하는 스타터 — Wind 초급 */
    fun starter(): Sneaker = Sneaker(
        id = 0,
        faction = Faction.WIND,
        rarity = Rarity.COMMON,
        variant = 0,
        level = 1,
        mintNumber = 1,
        // WND-010 클라우드 러너
        luck = SneakerDesigns.of(Faction.WIND, Rarity.COMMON, 0).luck,
        comfort = 1.12,
        durability = 100,
        equipped = true,
        acquiredAt = System.currentTimeMillis(),
    )
}

/** "fire:epic:1" 같은 슬롯 키를 되돌린다. 형식이 어긋나면 null */
fun parseSlotKey(key: String): Triple<Faction, Rarity, Int>? {
    val parts = key.split(':')
    if (parts.size != 3) return null
    val faction = Faction.entries.firstOrNull { it.id == parts[0] } ?: return null
    val rarity = Rarity.entries.firstOrNull { it.id == parts[1] } ?: return null
    val variant = parts[2].toIntOrNull() ?: return null
    return Triple(faction, rarity, variant)
}
