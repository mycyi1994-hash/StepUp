package com.stepup.android

import com.stepup.android.ui.AppChromePolicy
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import org.junit.Assert.*
import org.junit.Test

class AppChromePolicyTest {
    @Test fun `five tabs - running together shoes items profile`() {
        // 2026-10-26 시안 — 뽑기는 신발 탭 안의 글자 탭. 길은 "mystery-box" 그대로.
        // 2026-11-17 아이템 탭(가방, 서버 0106)을 신발과 내 정보 사이에 — 길은 "bag"("items" 는 예전 보관함 · 거래소 길)
        assertEquals(listOf("home", "community", "customize", "bag", "profile"), AppChromePolicy.tabs.map { it.route })
        val bag = requireNotNull(AppChromePolicy.destination(Routes.BAG))
        assertEquals(Screen.Bag, bag.parent)
        assertEquals(AppChromePolicy.Header.Main, bag.header)
        assertEquals(Screen.Customize, AppChromePolicy.destination(Routes.ITEMS)?.parent)
        assertEquals(Screen.Draw.route, Routes.MYSTERY_BOX)
    }

    @Test fun `draw lives under the shoe tab with the shared header and tabs`() {
        val draw = requireNotNull(AppChromePolicy.destination(Routes.MYSTERY_BOX))
        assertEquals(Screen.Customize, draw.parent)
        assertEquals(AppChromePolicy.Header.Main, draw.header)
        assertTrue(draw.showBottomBar)
        // 신발 탭 밑의 화면은 그대로 신발
        assertEquals(Screen.Customize, AppChromePolicy.destination(Routes.SNEAKER)?.parent)
    }

    @Test fun `the draw flow hides the tabs only on the draw tab`() {
        try {
            AppChromePolicy.immersive = true
            assertTrue(AppChromePolicy.immersiveAt(Routes.MYSTERY_BOX))
            assertFalse(AppChromePolicy.immersiveAt(Screen.Customize.route))
            assertFalse(AppChromePolicy.immersiveAt(Screen.Run.route))
            AppChromePolicy.immersive = false
            assertFalse(AppChromePolicy.immersiveAt(Routes.MYSTERY_BOX))
        } finally {
            AppChromePolicy.immersive = false
        }
    }

    @Test fun `concrete routes and restored templates have the same chrome`() {
        listOf(
            Routes.RUN_NOW to Routes.RUN_ROUTE,
            Routes.RUN to Routes.RUN_ROUTE,
            Routes.sneaker(42) to Routes.SNEAKER,
            Routes.marketModel(com.stepup.android.data.remote.ModelKey("blue", "rare", 2), 42) to Routes.MARKET_MODEL,
            Routes.marketModel(com.stepup.android.data.remote.ModelKey.ofModel("RARE", 1119), 42) to Routes.MARKET_MODEL,
            Routes.crewBoard("my-crew") to Routes.CREW_BOARD,
            Routes.postCompose("") to Routes.POST_COMPOSE,
            Routes.flashLobby(9) to Routes.FLASH_LOBBY,
        ).forEach { (concrete, template) ->
            assertEquals(concrete, AppChromePolicy.destination(template), AppChromePolicy.destination(concrete))
        }
    }

    @Test fun `focus and forms hide tabs but keep their parent`() {
        assertEquals(Screen.Run, AppChromePolicy.destination(Routes.RUN_NOW)?.parent)
        assertFalse(AppChromePolicy.destination(Routes.RUN_NOW)!!.showBottomBar)
        // 러닝 시작 메뉴는 13차(S-361)부터 아래 탭 줄이 보인다 — 러닝 탭 안의 화면
        assertEquals(Screen.Run, AppChromePolicy.destination(Routes.RUN_MENU)?.parent)
        assertTrue(AppChromePolicy.destination(Routes.RUN_MENU)!!.showBottomBar)
        // 챌린지 · 지난 도전은 러닝 탭 안의 집중 화면(탭 줄 없음)
        for (route in listOf(Routes.RUN_GOALS, Routes.RUN_GOAL_HISTORY,
            Routes.RUN_DIET, Routes.RUN_DIET_PLAN, Routes.RUN_DIET_EDIT)) {
            assertEquals(route, AppChromePolicy.destination(route)?.route)
            assertEquals(Screen.Run, AppChromePolicy.destination(route)?.parent)
            assertFalse(AppChromePolicy.destination(route)!!.showBottomBar)
        }
        // 크루 러닝 대기실 목록 · 만들기는 함께 뛰기 탭 밑(2026-10-26 — 크루 러닝 · 번개러닝 한 탭)
        for (route in listOf(Routes.crewRooms("crew"), Routes.crewRoomNew("crew"), Routes.room("crew", 3), Routes.postCompose("", flash = true))) {
            assertEquals(route, Screen.Community, AppChromePolicy.destination(route)?.parent)
            assertFalse(route, AppChromePolicy.destination(route)!!.showBottomBar)
        }
        assertFalse(AppChromePolicy.destination(Routes.CREW_CREATE)!!.showBottomBar)
        assertFalse(AppChromePolicy.destination(Routes.postCompose("crew"))!!.showBottomBar)
        assertTrue(AppChromePolicy.destination(Routes.crewBoard("crew"))!!.showBottomBar)
    }

    @Test fun `unknown routes cannot acquire an unrelated parent through a prefix`() {
        listOf(null, "runaway", "runner-market-typo", "settings/bogus", "crew/board/", "sneaker/1/extra")
            .forEach { assertNull(it, AppChromePolicy.destination(it)) }
    }
}
