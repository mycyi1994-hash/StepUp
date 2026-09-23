package com.stepup.android.domain

/**
 * 캐릭터 그림 — 앱에 실제로 들어 있는 완성 그림과, 그 그림이 보여 줄 수
 * 있는 착장.
 *
 * 남자 캐릭터는 **RUNO(루노)**, 여자 캐릭터는 **LUMI(루미)** 다
 * (design/characters/ 의 캐릭터 가이드).
 *
 * ── 어떤 그림이 있나 ──
 *
 *  * RUNO 달리기 · RUNO 서 있기 · LUMI 서 있기 — 기본 의상(OUTFIT-BASE) + 기본 신발(SHOES-BASE)
 *  * RUNO 서 있기 × 신발 52종 — 기본 의상에 그 신발을 신은 모습 (design/equipment 신발 시트)
 *  * RUNO 서 있기 × 의상 5종(CLO-001~005) — 그 의상에 기본 신발을 신은 모습 (의상 시트)
 *  * LUMI 서 있기 × 신발 52종 — 속성마다 정해진 추천 의상에 그 신발을 신은 모습
 *    (불=엠버 셸, 물=타이드 아노락, 번개=볼트 저지, 바람=에어로 윈드브레이커 · design/equipment/lumi)
 *  * LUMI 서 있기 × 의상 5종(LUM-CLO-001~005) — 그 의상 · 같은 색 모자에 기본 신발
 *
 * 시트는 한 칸에 한 조합만 그려져 있다. 그 밖의 조합(RUNO 새 의상 + NFT 신발,
 * LUMI 기본 의상 + NFT 신발 등)은 그림이 없다 — 그러려면 같은 포즈 · 같은 캔버스에
 * 맞춘 몸 · 의상(+모자) · 신발 투명 레이어가 따로 있어야 한다.
 *
 * 의상은 한 디자인이 캐릭터마다 다른 디자인 번호를 가진다(RUNO CLO-001, LUMI
 * LUM-CLO-001 — [designIdFor]). 앱 안의 착장 값은 캐릭터와 상관없이 [Outfit.id] 하나다.
 * 루미의 모자 색은 의상을 따라가며 따로 저장하지 않는다 — 그림에 함께 그려져 있다.
 *
 * ── 그림을 고를 때 지키는 것 ──
 *
 *  1. **성별은 속이지 않는다.** 여자를 고른 사람에게 남자 그림을 보여 주지 않는다.
 *  2. **입은 것이 보이는 그림이 먼저다.** 자세보다 착장이 우선이다 — 홈은 달리는
 *     자세를 원하지만, 클라우드 러너를 신었으면 클라우드 러너를 신은 서 있는
 *     그림을 쓴다(시트의 HOME = PROFILE).
 *  3. **착장은 속이지 않는다.** 그림 속 옷·신발이 실제로 입은 것과 다르면
 *     [AvatarRender.outfitShown] / [AvatarRender.shoeShown] 이 false 가 되고,
 *     화면은 그 사실을 적는다. 다른 아이템의 그림을 보여 주며 "장착 완료"라고
 *     하지 않는다.
 */
enum class AvatarPose { IDLE, RUN, CHEER }

/**
 * 그림 한 장.
 *
 * @param key 리소스를 찾는 열쇠 — 리소스 이름은 `avatar_` + key 다
 * @param outfitId 그림 속 의상 — [Outfits] 의 id
 * @param shoeCode 그림 속 신발의 도감 번호("WND-010"). null 이면 기본 운동화(SHOES-BASE).
 */
data class AvatarArt(
    val key: String,
    val gender: AvatarGender,
    val pose: AvatarPose,
    val outfitId: String,
    val shoeCode: String?,
) {
    companion object {
        val MALE_RUN = AvatarArt("male_running", AvatarGender.MALE, AvatarPose.RUN, Outfits.BASE_ID, null)
        val MALE_IDLE = AvatarArt("male_idle", AvatarGender.MALE, AvatarPose.IDLE, Outfits.BASE_ID, null)
        val FEMALE_IDLE = AvatarArt("female_idle", AvatarGender.FEMALE, AvatarPose.IDLE, Outfits.BASE_ID, null)
    }
}

/** 한 화면이 실제로 그릴 그림과, 그 그림이 사실과 어디까지 맞는지. */
data class AvatarRender(
    val art: AvatarArt,
    /** 원한 자세 그대로인가. false 면 같은 성별의 다른 자세 그림이다. */
    val exactPose: Boolean,
    /** 그림 속 옷이 입은 옷과 같은가 */
    val outfitShown: Boolean,
    /** 그림 속 신발이 신은 신발과 같은가 */
    val shoeShown: Boolean,
) {
    /** 입은 것이 그림에 그대로 보이는가 */
    val lookShown: Boolean get() = outfitShown && shoeShown
}

/** 신발 도감 번호 — "FIR-001" … "WND-013". 장비 카탈로그(design/equipment)의 id 와 같다. */
fun shoeCode(faction: Faction, rarity: Rarity, variant: Int): String =
    SneakerDesigns.of(faction, rarity, variant).code

/**
 * 이 의상의 캐릭터별 디자인 번호 — 장비 카탈로그의 id.
 * RUNO 는 "CLO-001", LUMI 는 "LUM-CLO-001", 기본 의상은 둘 다 "OUTFIT-BASE".
 */
fun Outfit.designIdFor(gender: AvatarGender): String = when {
    !nft -> "OUTFIT-BASE"
    gender == AvatarGender.FEMALE -> "LUM-$id"
    else -> id
}

/** 이 신발 한 켤레(소유 인스턴스)의 디자인 번호. 인스턴스 id 와는 다르다. */
fun Sneaker.designCode(): String = shoeCode(faction, rarity, variant)

object AvatarArtCatalog {

    /** 신발 52종 도감 번호 — 속성마다 13 */
    val SHOE_CODES: List<String> = SneakerDesigns.all.map { it.code }

    private fun keyOf(id: String) = "runo_idle_" + id.lowercase().replace('-', '_')
    private fun lumiKeyOf(id: String) = "lumi_idle_" + id.lowercase().replace('-', '_')

    /** LUMI 신발 시트의 추천 의상 — 속성마다 하나. 그림 속 루미가 입은 옷이다(강제 조합은 아니다). */
    val LUMI_SHEET_OUTFIT: Map<String, Outfit> = mapOf(
        "FIR" to Outfits.EMBER_SHELL,
        "WAT" to Outfits.TIDE_ANORAK,
        "LIT" to Outfits.VOLT_JERSEY,
        "WND" to Outfits.AERO_WINDBREAKER,
    )

    /** 앱에 들어 있는 그림 전부 */
    val ALL: List<AvatarArt> = buildList {
        add(AvatarArt.MALE_RUN)
        add(AvatarArt.MALE_IDLE)
        add(AvatarArt.FEMALE_IDLE)
        // RUNO — 기본 의상 + 신발 52종
        SHOE_CODES.forEach { code ->
            add(AvatarArt(keyOf(code), AvatarGender.MALE, AvatarPose.IDLE, Outfits.BASE_ID, code))
        }
        // RUNO — 새 의상 5종 + 기본 신발
        Outfits.ALL.filter { it.nft }.forEach { o ->
            add(AvatarArt(keyOf(o.id), AvatarGender.MALE, AvatarPose.IDLE, o.id, null))
        }
        // LUMI — 속성별 추천 의상 + 신발 52종
        SHOE_CODES.forEach { code ->
            add(AvatarArt(lumiKeyOf(code), AvatarGender.FEMALE, AvatarPose.IDLE, LUMI_SHEET_OUTFIT.getValue(code.take(3)).id, code))
        }
        // LUMI — 새 의상 5종(모자 포함) + 기본 신발
        Outfits.ALL.filter { it.nft }.forEach { o ->
            add(AvatarArt(lumiKeyOf(o.designIdFor(AvatarGender.FEMALE)), AvatarGender.FEMALE, AvatarPose.IDLE, o.id, null))
        }
    }

    fun resolve(look: AvatarLook, pose: AvatarPose): AvatarRender {
        val shoe = look.shoe?.designCode()
        // 성별마다 그림이 적어도 한 장 있다 — AvatarArtTest 가 지킨다
        val art = ALL.filter { it.gender == look.gender }.maxBy { a ->
            var score = 0
            if (a.outfitId == look.outfit.id) score += 5 // 의상이 화면을 더 많이 차지한다
            if (a.shoeCode == shoe) score += 4
            if (a.pose == pose) score += 1
            score
        }
        return AvatarRender(
            art = art,
            exactPose = art.pose == pose,
            outfitShown = art.outfitId == look.outfit.id,
            shoeShown = art.shoeCode == shoe,
        )
    }

    /** 아직 없는 성별·자세 조합 — 추가로 그려야 할 그림 */
    fun missingPoses(): List<Pair<AvatarGender, AvatarPose>> =
        AvatarGender.entries.flatMap { g -> AvatarPose.entries.map { g to it } }
            .filter { (g, p) -> ALL.none { it.gender == g && it.pose == p } }
}
