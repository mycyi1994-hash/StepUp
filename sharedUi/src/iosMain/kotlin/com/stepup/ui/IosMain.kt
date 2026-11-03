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
 * 옮긴 탭만 둔다(러닝 · 신발). 함께 뛰기 · 내 정보는 옮기는 대로 더한다(빈 탭 · "준비 중"을 두지 않는다).
 */
@Composable
fun IosMain(onStartRun: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(Screen.Run.route) }
    /** 신발 탭 안에서 연 신발 상세 */
    var sneakerId by rememberSaveable { mutableStateOf<Long?>(null) }
    val tabs = listOf(Screen.Run, Screen.Customize)
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
                else -> IosHome(onStartRun = onStartRun)
            }
        }
        if (sneakerId == null || selected != Screen.Customize) {
            StepUpTabBar(selectedTab = selected, bottomTabs = tabs, onSelect = { screen ->
                if (screen == selected) sneakerId = null else tab = screen.route
            })
        }
    }
}
