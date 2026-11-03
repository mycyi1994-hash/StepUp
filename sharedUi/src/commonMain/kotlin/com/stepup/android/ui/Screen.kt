package com.stepup.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.ui.res.Res
import com.stepup.ui.res.tab_community
import com.stepup.ui.res.tab_customize
import com.stepup.ui.res.tab_draw
import com.stepup.ui.res.tab_me
import com.stepup.ui.res.tab_run
import org.jetbrains.compose.resources.StringResource

/** 하단 탭 — 안드로이드 StepUpRoot.kt 의 Screen 과 같은 길 이름 · 글자 · 아이콘 */
sealed class Screen(val route: String, val labelRes: StringResource, val icon: ImageVector) {
    data object Run : Screen("home", Res.string.tab_run, Icons.AutoMirrored.Filled.DirectionsRun)
    data object Customize : Screen("customize", Res.string.tab_customize, StepUpIcons.Shoe)
    data object Draw : Screen("mystery-box", Res.string.tab_draw, StepUpIcons.DrawBox)
    data object Community : Screen("community", Res.string.tab_community, Icons.Filled.Groups)
    data object Profile : Screen("profile", Res.string.tab_me, Icons.Filled.Person)
}
