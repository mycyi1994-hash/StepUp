package com.stepup.android.data.repo

import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarLook
import com.stepup.android.domain.Outfit
import com.stepup.android.domain.Outfits
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/**
 * 러너 캐릭터가 지금 입고 있는 것.
 *
 * 홈 · 러닝 완료 · 꾸미기 · 내 정보가 모두 [look] 하나를 받아 그린다. 한
 * 화면에서 갈아입히면 다른 화면에도 그대로 보인다.
 *
 * ── 가진 의상 ──
 *
 * 지금 가질 수 있는 의상은 기본 의상 둘뿐이다. NFT 의상을 사는 흐름이
 * 아직 없기 때문이다. 그래서 [owned] 는 기본 의상만 돌려주고, 가지지 않은
 * 의상은 입힐 수 없다([equipOutfit] 이 거절한다).
 *
 * 데모 모드에서는 NFT 의상을 **체험으로** 입혀 볼 수 있다. 체험은 별도
 * 자리에 적히고([UserPrefs.demoOutfit]), 데모를 끄면 사라진다. 체험 중인
 * 것은 [AvatarLook.trial] 로 표시되어 화면이 "체험 중"이라고 적는다.
 */
class AvatarRepository(
    private val prefs: UserPrefs,
    sneakers: SneakerRepository,
) {
    val demoMode: Flow<Boolean> = prefs.demoMode

    val look: Flow<AvatarLook> = combine(
        prefs.avatarGender,
        prefs.avatarOutfit,
        prefs.demoMode,
        prefs.demoOutfit,
        sneakers.equipped,
    ) { gender, outfitId, demo, demoOutfitId, shoe ->
        // 저장된 값이라도 가진 옷이 아니면 입히지 않는다
        val worn = Outfits.of(outfitId).takeIf { isOwned(it) } ?: Outfits.DEFAULT
        val trial = if (demo) Outfits.PREVIEWABLE.firstOrNull { it.id == demoOutfitId } else null
        AvatarLook(
            gender = AvatarGender.of(gender),
            outfit = trial ?: worn,
            shoe = shoe,
            trial = trial != null && !isOwned(trial),
        )
    }

    /** 가진 의상 — 지금은 기본 의상뿐이다 */
    fun owned(): List<Outfit> = Outfits.ALL.filter { isOwned(it) }

    fun isOwned(outfit: Outfit): Boolean = outfit.starter

    suspend fun setGender(gender: AvatarGender) = prefs.setAvatarGender(gender.id)

    /**
     * 의상을 입힌다.
     *
     * @return 입혔으면 true. 가지지 않은 의상이고 데모도 꺼져 있으면 false.
     */
    suspend fun equipOutfit(outfit: Outfit): Boolean {
        if (isOwned(outfit)) {
            prefs.setAvatarOutfit(outfit.id)
            // 가진 옷을 입으면 체험은 끝난다
            prefs.setDemoOutfit("")
            return true
        }
        if (prefs.demoMode.first()) {
            prefs.setDemoOutfit(outfit.id)
            return true
        }
        return false
    }

    suspend fun setDemoMode(on: Boolean) = prefs.setDemoMode(on)
}
