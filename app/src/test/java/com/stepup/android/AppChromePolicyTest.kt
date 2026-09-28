package com.stepup.android

import com.stepup.android.ui.AppChromePolicy
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import org.junit.Assert.*
import org.junit.Test

class AppChromePolicyTest {
    @Test fun `five tabs - running shoes draw community profile`() {
        // 신발 화면 확정안(2026-09-28) — 뽑기는 가운데 독립 탭. 길은 예전 하위 화면의 "mystery-box" 그대로
        assertEquals(listOf("home", "customize", "mystery-box", "community", "profile"), AppChromePolicy.tabs.map { it.route })
        assertEquals(Screen.Draw.route, Routes.MYSTERY_BOX)
    }

    @Test fun `draw is its own tab with the shared header and tabs`() {
        val draw = requireNotNull(AppChromePolicy.destination(Routes.MYSTERY_BOX))
        assertEquals(Screen.Draw, draw.parent)
        assertEquals(AppChromePolicy.Header.Main, draw.header)
        assertTrue(draw.showBottomBar)
        // 신발 탭 밑의 화면은 그대로 신발
        assertEquals(Screen.Customize, AppChromePolicy.destination(Routes.SNEAKER)?.parent)
    }

    @Test fun `the draw flow folds the shell only on the draw tab`() {
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
            Routes.marketModel("blue", "rare", 2, 42) to Routes.MARKET_MODEL,
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
        // 러닝 시작 메뉴 · 챌린지 · 지난 도전은 러닝 탭 안의 집중 화면(탭 줄 없음)
        for (route in listOf(Routes.RUN_MENU, Routes.RUN_GOALS, Routes.RUN_GOAL_HISTORY,
            Routes.RUN_DIET, Routes.RUN_DIET_PLAN, Routes.RUN_DIET_EDIT)) {
            assertEquals(route, AppChromePolicy.destination(route)?.route)
            assertEquals(Screen.Run, AppChromePolicy.destination(route)?.parent)
            assertFalse(AppChromePolicy.destination(route)!!.showBottomBar)
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
