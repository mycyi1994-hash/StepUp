package com.stepup.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.stepup.android.ui.Screen
import com.stepup.android.ui.StepUpTabBar
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.screens.customize.CustomizeScreen
import com.stepup.android.ui.screens.items.SneakerDetailScreen

/**
 * iOS 앱 안(안드로이드 StepUpRoot 의 탭 · 길 자리) — 하단 탭은 안드로이드와 같은 StepUpTabBar.
 * 옮긴 탭만 둔다(러닝 · 신발 · 내 정보). 함께 뛰기는 옮기는 대로 더한다(빈 탭 · "준비 중"을 두지 않는다).
 * 내 정보 안의 길은 안드로이드 Routes 이름 그대로 쌓는다(설정 › 테마 · 언어 …).
 */
@Composable
fun IosMain(onStartRun: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Screen.Run.route) }
    /** 신발 탭 안에서 연 신발 상세 */
    var sneakerId by rememberSaveable { mutableStateOf<Long?>(null) }
    /** 내 정보 탭 안에서 쌓인 화면(안드로이드 Routes 이름) — 비면 내 정보 첫 화면 */
    var profileStack by rememberSaveable { mutableStateOf(listOf<String>()) }
    val tabs = listOf(Screen.Run, Screen.Customize, Screen.Profile)
    val selected = tabs.firstOrNull { it.route == tab } ?: Screen.Run
    Column(Modifier.fillMaxSize().background(StepUpUi.colors.background)) {
        Box(Modifier.weight(1f)) {
            when (selected) {
                Screen.Customize -> {
                    val open = sneakerId
                    if (open != null) {
                        Box(Modifier.fillMaxSize().statusBarsPadding()) {
                            SneakerDetailScreen(sneakerId = open, onBack = { sneakerId = null })
                        }
                    } else {
                        Box(Modifier.fillMaxSize().statusBarsPadding()) {
                            CustomizeScreen(onOpenSneaker = { sneakerId = it })
                        }
                    }
                }
                Screen.Profile -> Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    IosProfileTab(profileStack, push = { profileStack = profileStack + it }, pop = { profileStack = profileStack.dropLast(1) })
                }
                else -> IosHome(onStartRun = onStartRun)
            }
        }
        val inner = (selected == Screen.Customize && sneakerId != null) || (selected == Screen.Profile && profileStack.isNotEmpty())
        if (!inner) {
            StepUpTabBar(selectedTab = selected, bottomTabs = tabs, onSelect = { screen ->
                // 같은 탭을 다시 누르면 그 탭의 첫 화면으로(안드로이드와 같다)
                if (screen == selected) { sneakerId = null; profileStack = emptyList() } else tab = screen.route
            })
        }
    }
}

/** 내 정보 탭 — 안드로이드 StepUpRoot 의 profile · settings 길과 같은 화면들. 옮기지 않은 길은 열지 않는다 */
@Composable
private fun IosProfileTab(stack: List<String>, push: (String) -> Unit, pop: () -> Unit) {
    when (stack.lastOrNull()) {
        null -> com.stepup.android.ui.screens.profile.ProfileScreen(
            onOpenSettings = { push(SETTINGS) },
            onOpenCustomize = {},
        )
        SETTINGS -> com.stepup.android.ui.screens.settings.SettingsHomeScreen(
            onBack = pop,
            onOpenTheme = { push(THEME) },
            onOpenLanguage = { push(LANGUAGE) },
        )
        THEME -> com.stepup.android.ui.screens.settings.ThemeScreen(onBack = pop)
        LANGUAGE -> com.stepup.android.ui.screens.settings.LanguageScreen(onBack = pop)
        else -> pop()
    }
}

private const val SETTINGS = "settings"
private const val THEME = "settings/theme"
private const val LANGUAGE = "settings/language"
