package com.stepup.ui

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Swift 의 `MainViewControllerKt.MainViewController()` — iosApp/StepUp/StepUpApp.swift 가 이 화면을 앱 전체에 띄운다 */
fun MainViewController(): UIViewController = ComposeUIViewController { StepUpRoot() }
