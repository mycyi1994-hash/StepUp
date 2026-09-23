package com.stepup.android.ui.theme

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fixed shared chrome. Screens select content/state, never their own geometry. */
object StepUpDesign {
    val Gutter = 20.dp
    val HeaderHeight = 64.dp
    val TouchTarget = 48.dp
    val ControlIcon = 24.dp
    val ControlRadius = 24.dp
    val HeaderLogoHeight = 26.dp
    val LaunchLogoHeight = 52.dp
    const val LogoAspectRatio = 5.76f
    val BalanceHeight = 48.dp
    val BalanceAmount = 16.sp
    val BalanceUnit = 12.sp
    val PrimaryHeight = 60.dp
    val PrimaryLabel = 18.sp
    val SecondaryLabel = 14.sp
    val SecondaryHorizontalPadding = 20.dp
    val SecondaryVerticalPadding = 12.dp
    val NavigationHeight = 76.dp
    val NavigationItemHeight = 68.dp
    val NavigationLabel = 13.sp
    val NavigationIcon = 24.dp
    val NavigationIndicator = 4.dp
}

/** Only sanctioned logo roles. No per-screen numeric size API. */
enum class BrandLogoRole { Header, Launch }
