package com.stepup.android

import com.stepup.android.ui.AppChromePolicy
import com.stepup.android.ui.Routes
import com.stepup.android.ui.Screen
import org.junit.Assert.*
import org.junit.Test

class AppChromePolicyTest {
    @Test fun `tab order stays running wardrobe community profile`() {
        assertEquals(listOf("home", "customize", "community", "profile"), AppChromePolicy.tabs.map { it.route })
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
        for (route in listOf(Routes.RUN_MENU, Routes.RUN_GOALS, Routes.RUN_GOAL_HISTORY)) {
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
