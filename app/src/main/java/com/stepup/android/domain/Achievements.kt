package com.stepup.android.domain

/**
 * 업적 100종 — 13개 카테고리 × 티어.
 *
 * 난이도는 "1년 꾸준히 쓰면 전부 달성"에 맞췄다.
 * (하루 1만 보 기준 연 365만 보, 주 4회 러닝 기준 연 200세션 등)
 * 티어가 오를수록 등급(브론즈→다이아)이 올라가고, 이름은
 * "카테고리 라벨 + 로마 숫자"로 조립해 5개 언어를 그대로 탄다.
 */

enum class AchievementGrade { BRONZE, SILVER, GOLD, PLATINUM, DIAMOND }

enum class AchievementCategory(
    val id: String,
    /** 티어 임계값 — 오름차순 */
    val tiers: List<Double>,
) {
    /** 누적 걸음 */
    STEPS("STEPS", listOf(1e3, 5e3, 2e4, 5e4, 1e5, 2.5e5, 5e5, 1e6, 2e6, 3.5e6)),

    /** 누적 거리 (km) */
    DISTANCE("DISTANCE", listOf(1.0, 5.0, 20.0, 50.0, 100.0, 250.0, 500.0, 1000.0, 1600.0, 2500.0)),

    /** 러닝 세션 수 */
    SESSIONS("SESSIONS", listOf(1.0, 3.0, 10.0, 25.0, 50.0, 100.0, 180.0, 250.0, 365.0, 500.0)),

    /** 연속 목표 달성일 */
    STREAK("STREAK", listOf(2.0, 3.0, 5.0, 7.0, 14.0, 21.0, 30.0, 60.0)),

    /** 누적 적립 SUP */
    SUP("SUP", listOf(50.0, 200.0, 500.0, 1e3, 2.5e3, 5e3, 1e4, 2e4, 3.5e4, 6e4)),

    /** 민팅 횟수 */
    MINT("MINT", listOf(1.0, 2.0, 3.0, 5.0, 8.0, 12.0, 20.0, 30.0)),

    /** 도감 슬롯 수집 (총 52) */
    COLLECTION("COLLECTION", listOf(2.0, 5.0, 9.0, 14.0, 21.0, 30.0, 40.0, 52.0)),

    /** 강화 횟수 */
    UPGRADE("UPGRADE", listOf(1.0, 3.0, 7.0, 15.0, 25.0, 40.0, 60.0, 90.0)),

    /** 파티런 완주 */
    PARTY("PARTY", listOf(1.0, 3.0, 7.0, 15.0, 30.0, 60.0, 100.0)),

    /** 이벤트 보상 수령 */
    EVENT("EVENT", listOf(1.0, 3.0, 6.0, 12.0, 24.0)),

    /** 크루 가입 */
    CREW("CREW", listOf(1.0, 2.0, 3.0, 4.0)),

    /** 커뮤니티 글 작성 */
    POST("POST", listOf(1.0, 3.0, 7.0, 15.0, 30.0, 60.0)),

    /** 최고 스니커즈 레벨 */
    LEVEL("LEVEL", listOf(2.0, 3.0, 5.0, 8.0, 12.0, 20.0)),
}

/** 업적 판정에 쓰는 실측치 묶음 */
data class AchievementMetrics(
    val steps: Long = 0,
    val km: Double = 0.0,
    val sessions: Int = 0,
    val streak: Int = 0,
    val supEarned: Double = 0.0,
    val mints: Int = 0,
    val collectionSlots: Int = 0,
    val upgrades: Int = 0,
    val partyRuns: Int = 0,
    val eventClaims: Int = 0,
    val crews: Int = 0,
    val posts: Int = 0,
    val maxSneakerLevel: Int = 0,
) {
    fun valueOf(category: AchievementCategory): Double = when (category) {
        AchievementCategory.STEPS -> steps.toDouble()
        AchievementCategory.DISTANCE -> km
        AchievementCategory.SESSIONS -> sessions.toDouble()
        AchievementCategory.STREAK -> streak.toDouble()
        AchievementCategory.SUP -> supEarned
        AchievementCategory.MINT -> mints.toDouble()
        AchievementCategory.COLLECTION -> collectionSlots.toDouble()
        AchievementCategory.UPGRADE -> upgrades.toDouble()
        AchievementCategory.PARTY -> partyRuns.toDouble()
        AchievementCategory.EVENT -> eventClaims.toDouble()
        AchievementCategory.CREW -> crews.toDouble()
        AchievementCategory.POST -> posts.toDouble()
        AchievementCategory.LEVEL -> maxSneakerLevel.toDouble()
    }
}

data class Achievement(
    val category: AchievementCategory,
    /** 카테고리 내 티어 (0부터) */
    val tier: Int,
    val threshold: Double,
    val grade: AchievementGrade,
    val unlocked: Boolean,
    /** 0..1 진행률 */
    val progress: Float,
    /** 현재 실측치 */
    val current: Double,
)

object AchievementBook {

    /** 전체 업적 수 = 100 */
    val TOTAL: Int = AchievementCategory.entries.sumOf { it.tiers.size }

    private val ROMAN = listOf("I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X")

    fun roman(tier: Int): String = ROMAN.getOrElse(tier) { "${tier + 1}" }

    /** 티어 위치(비율)로 등급을 정한다 */
    fun gradeOf(tier: Int, tierCount: Int): AchievementGrade {
        val f = (tier + 1).toDouble() / tierCount
        return when {
            f <= 0.25 -> AchievementGrade.BRONZE
            f <= 0.50 -> AchievementGrade.SILVER
            f <= 0.75 -> AchievementGrade.GOLD
            f <= 0.90 -> AchievementGrade.PLATINUM
            else -> AchievementGrade.DIAMOND
        }
    }

    /**
     * 실측치로 100종 전체를 판정한다. [best] 는 카테고리마다 지금까지 닿은 가장 큰 값(AchievementCategory.id → 값, 계정마다
     * UserPrefs 에) — 지금 값이 줄어도(스트릭이 끊김 · 크루를 나감 · 강화 재료로 신발을 태움 · 연결 없이 0 을 읽음) 한 번 연
     * 티어는 다시 잠기지 않고 진행 막대도 뒤로 가지 않는다(2026-10-17 점검).
     */
    fun build(metrics: AchievementMetrics, best: Map<String, Double> = emptyMap()): List<Achievement> =
        AchievementCategory.entries.flatMap { category ->
            val value = maxOf(metrics.valueOf(category), best[category.id] ?: 0.0)
            category.tiers.mapIndexed { tier, threshold ->
                Achievement(
                    category = category,
                    tier = tier,
                    threshold = threshold,
                    grade = gradeOf(tier, category.tiers.size),
                    unlocked = value >= threshold,
                    progress = (value / threshold).toFloat().coerceIn(0f, 1f),
                    current = value,
                )
            }
        }

    /**
     * 이 계정의 폰 기록으로 세는 카테고리 — 러닝 수는 계정마다 센다. 걸음 · 거리(하루 걸음 표) · 연속 달성(설정)은 이 폰의 값이라
     * 계정마다 적지 않는다 — 한 폰을 두 계정이 쓰면 앞 계정의 값이 뒤 계정의 최고값으로 굳었다(Codex 리뷰). 그 값들은 줄지 않으므로
     * 적지 않아도 다시 잠기지 않는다.
     */
    private val LOCAL = setOf(AchievementCategory.SESSIONS)

    /** 서버 경제의 사본(원장 · 신발)으로 세는 카테고리 — 사본이 지금 계정 것일 때만 믿는다(계정을 바꾼 직후엔 앞 계정 것이다) */
    private val ECONOMY = setOf(
        AchievementCategory.SUP, AchievementCategory.MINT, AchievementCategory.UPGRADE, AchievementCategory.PARTY,
        AchievementCategory.EVENT, AchievementCategory.COLLECTION, AchievementCategory.LEVEL,
    )

    /**
     * 최고값으로 적어도 되는 카테고리. 받는 중 · 못 받은 목록(크루 · 글은 서버에만 있다)과 다른 계정의 경제 사본은 적지 않는다 —
     * 0 이거나 남의 값일 수 있다.
     */
    fun trusted(economyMine: Boolean, crewsLoaded: Boolean, postsLoaded: Boolean): Set<AchievementCategory> = buildSet {
        addAll(LOCAL)
        if (economyMine) addAll(ECONOMY)
        if (crewsLoaded) add(AchievementCategory.CREW)
        if (postsLoaded) add(AchievementCategory.POST)
    }

    /** 새로 적을 최고값 — [trusted] 카테고리에서 지금 값이 [best] 보다 클 때만. 없으면 빈 맵 */
    fun raised(metrics: AchievementMetrics, best: Map<String, Double>, trusted: Set<AchievementCategory>): Map<String, Double> =
        trusted.mapNotNull { category ->
            val value = metrics.valueOf(category)
            if (value.isFinite() && value > (best[category.id] ?: 0.0)) category.id to value else null
        }.toMap()

    /** 두 최고값을 합친다 — 카테고리마다 큰 쪽 */
    fun mergeBest(now: Map<String, Double>, values: Map<String, Double>): Map<String, Double> {
        val merged = now.toMutableMap()
        values.forEach { (id, value) ->
            if (value.isFinite() && value > (merged[id] ?: 0.0)) merged[id] = value
        }
        return merged
    }

    /** 저장 글자 — "STEPS=12000.0;STREAK=7.0" */
    fun encodeBest(best: Map<String, Double>): String =
        best.entries.filter { it.value.isFinite() && it.value > 0.0 }.sortedBy { it.key }
            .joinToString(";") { "${it.key}=${it.value}" }

    /** [encodeBest] 를 되읽는다. 깨진 칸은 건너뛴다 */
    fun decodeBest(raw: String?): Map<String, Double> =
        raw.orEmpty().split(';').mapNotNull { part ->
            val id = part.substringBefore('=', "").trim()
            val value = part.substringAfter('=', "").trim().toDoubleOrNull()
            if (id.isEmpty() || value == null || !value.isFinite() || value <= 0.0) null else id to value
        }.toMap()
}
