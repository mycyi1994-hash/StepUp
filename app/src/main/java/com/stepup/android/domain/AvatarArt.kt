package com.stepup.android.domain

/**
 * 캐릭터 그림 — 앱에 실제로 들어 있는 완성 그림과, 그 그림이 보여 줄 수
 * 있는 착장.
 *
 * ── 그림은 두 장뿐이다 ──
 *
 * 디자인 패키지(design/blue-black-2026-09)에서 받은 캐릭터는 **남자 달리기**와
 * **여자 서 있기** 두 장이다. 둘 다 기본 후드(검정 바탕에 파란 줄)와 기본
 * 운동화를 입고 있다. 다른 자세·다른 옷·다른 신발을 입은 그림은 없다.
 *
 * 그래서 화면이 그림을 고를 때 지키는 것은 둘이다.
 *
 *  1. **성별은 속이지 않는다.** 여자를 고른 사람에게 남자 그림을 보여 주지
 *     않는다. 원하는 자세의 그림이 없으면 같은 성별의 다른 자세를 쓴다
 *     ([AvatarRender.exactPose] 가 false).
 *  2. **착장은 속이지 않는다.** 그림 속 옷·신발이 실제로 입은 것과 다르면
 *     [AvatarRender.outfitShown] / [AvatarRender.shoeShown] 이 false 가 되고,
 *     화면은 "그림은 기본 착장"이라고 적는다. 아이템 카드만 바뀌고 캐릭터는
 *     다른 신발을 신은 채로 "장착 완료"라고 하지 않는다.
 *
 * 그림이 늘면 [AvatarArt] 에 한 줄을 더하고, 그 그림이 입은 것을
 * [AvatarArt.outfitId] · [AvatarArt.wearsBaseShoes] 에 적는다.
 */
enum class AvatarPose { IDLE, RUN, CHEER }

enum class AvatarArt(
    val gender: AvatarGender,
    val pose: AvatarPose,
    /** 그림 속 의상 */
    val outfitId: String,
    /** 그림 속 신발이 기본 운동화인가 (NFT 신발이 아니다) */
    val wearsBaseShoes: Boolean,
) {
    MALE_RUN(AvatarGender.MALE, AvatarPose.RUN, Outfits.STARTER_HOODIE.id, wearsBaseShoes = true),
    FEMALE_IDLE(AvatarGender.FEMALE, AvatarPose.IDLE, Outfits.STARTER_HOODIE.id, wearsBaseShoes = true),
}

/**
 * 한 화면이 실제로 그릴 그림과, 그 그림이 사실과 어디까지 맞는지.
 */
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

object AvatarArtCatalog {

    fun resolve(look: AvatarLook, pose: AvatarPose): AvatarRender {
        val sameGender = AvatarArt.entries.filter { it.gender == look.gender }
        // 성별마다 그림이 적어도 한 장 있다 — AvatarArtTest 가 지킨다
        val art = sameGender.firstOrNull { it.pose == pose } ?: sameGender.first()
        return AvatarRender(
            art = art,
            exactPose = art.pose == pose,
            outfitShown = look.outfit.id == art.outfitId,
            shoeShown = look.shoe == null && art.wearsBaseShoes,
        )
    }

    /** 아직 없는 성별·자세 조합 — 추가로 그려야 할 그림 */
    fun missingPoses(): List<Pair<AvatarGender, AvatarPose>> =
        AvatarGender.entries.flatMap { g -> AvatarPose.entries.map { g to it } }
            .filter { (g, p) -> AvatarArt.entries.none { it.gender == g && it.pose == p } }
}
