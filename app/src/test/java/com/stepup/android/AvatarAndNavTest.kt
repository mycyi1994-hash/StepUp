package com.stepup.android

import com.stepup.android.domain.AvatarGender
import com.stepup.android.domain.AvatarSkin
import com.stepup.android.domain.Outfits
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import com.stepup.android.ui.parentTabOf
import com.stepup.android.ui.screens.feed.isDemo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 리뉴얼에서 깨지면 안 되는 약속들.
 *
 *   * 피부색은 성별과 무관하게 하나다.
 *   * 기본 의상은 NFT 가 아니고, NFT 의상은 처음부터 갖고 있지 않다.
 *   * 하위 화면은 자기 부모 탭에 불을 켠다.
 */
class AvatarAndNavTest {

    // ── 캐릭터 ──────────────────────────────────────────────────

    @Test
    fun `남녀 피부색이 같다`() {
        assertEquals(AvatarSkin.of(AvatarGender.MALE), AvatarSkin.of(AvatarGender.FEMALE))
        assertEquals(AvatarSkin.BASE, AvatarSkin.of(AvatarGender.MALE))
    }

    @Test
    fun `피부는 짙은 블랙·네이비다 — 살구색이 아니다`() {
        val base = AvatarSkin.BASE
        val r = (base shr 16) and 0xFF
        val g = (base shr 8) and 0xFF
        val b = base and 0xFF
        // 어둡고(각 채널 64 미만), 붉은 기보다 푸른 기가 많아야 한다
        assertTrue("너무 밝다: $r,$g,$b", r < 64 && g < 64 && b < 64)
        assertTrue("붉은 기가 더 많다: $r,$g,$b", b >= r)
    }

    @Test
    fun `기본 의상은 NFT 가 아니고 NFT 의상은 기본이 아니다`() {
        Outfits.ALL.forEach { o ->
            assertFalse("${o.id} 이 기본이면서 NFT 다", o.starter && o.nft)
        }
        assertTrue(Outfits.ALL.any { it.starter })
        assertTrue(Outfits.ALL.any { it.nft })
        assertTrue("기본 의상은 NFT 가 아니어야 한다", Outfits.DEFAULT.starter && !Outfits.DEFAULT.nft)
    }

    @Test
    fun `의상 id 는 겹치지 않고 모르는 id 는 기본으로 간다`() {
        assertEquals(Outfits.ALL.size, Outfits.ALL.map { it.id }.toSet().size)
        assertEquals(Outfits.DEFAULT, Outfits.of("없는-의상"))
        assertEquals(Outfits.DEFAULT, Outfits.of(null))
    }

    @Test
    fun `모르는 성별 값은 남자로 간다`() {
        assertEquals(AvatarGender.MALE, AvatarGender.of(""))
        assertEquals(AvatarGender.FEMALE, AvatarGender.of("F"))
    }

    // ── 탭 ──────────────────────────────────────────────────────

    @Test
    fun `소식과 챌린지와 러닝 화면은 러닝 탭 밑이다`() {
        assertEquals(Screen.Run, parentTabOf(Screen.Run.route))
        assertEquals(Screen.Run, parentTabOf(Routes.NEWS))
        assertEquals(Screen.Run, parentTabOf(Routes.EVENTS))
        assertEquals(Screen.Run, parentTabOf(Routes.RUN))
        assertEquals(Screen.Run, parentTabOf(Routes.RUN_ROUTE))
        // "runner-market" 도 "run" 으로 시작하지만 꾸미기 밑이다
        assertEquals(Screen.Customize, parentTabOf(Routes.RUNNER_MARKET))
    }

    @Test
    fun `러너 마켓과 신발 화면은 꾸미기 탭 밑이다`() {
        assertEquals(Screen.Customize, parentTabOf(Screen.Customize.route))
        assertEquals(Screen.Customize, parentTabOf(Routes.RUNNER_MARKET))
        assertEquals(Screen.Customize, parentTabOf(Routes.ITEMS))
        assertEquals(Screen.Customize, parentTabOf(Routes.SNEAKER))
        assertEquals(Screen.Customize, parentTabOf(Routes.SNEAKER_DEX))
        assertEquals(Screen.Customize, parentTabOf(Routes.MARKET_MODEL))
    }

    @Test
    fun `크루와 글은 커뮤니티 탭 밑이다`() {
        assertEquals(Screen.Community, parentTabOf(Routes.CREW_BOARD))
        assertEquals(Screen.Community, parentTabOf(Routes.POST_COMPOSE))
        assertEquals(Screen.Community, parentTabOf(Routes.RANKING))
    }

    @Test
    fun `지갑과 설정은 내 정보 탭 밑이다`() {
        assertEquals(Screen.Profile, parentTabOf(Routes.WALLET))
        assertEquals(Screen.Profile, parentTabOf(Routes.SETTINGS_THEME))
        assertEquals(Screen.Profile, parentTabOf(Routes.NOTIFICATIONS))
    }

    // ── 데모 ────────────────────────────────────────────────────

    @Test
    fun `데모 행은 id 로 가려진다`() {
        assertTrue(isDemo("demo-e1"))
        assertFalse(isDemo("7f3a"))
    }
}
