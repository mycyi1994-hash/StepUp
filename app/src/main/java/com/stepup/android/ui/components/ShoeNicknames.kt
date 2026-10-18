package com.stepup.android.ui.components

import androidx.compose.runtime.compositionLocalOf

/** 내가 붙인 신발 이름(서버 신발 번호 → 이름, 2026-10-17). 앱 뿌리(StepUpRoot)가 설정 저장소에서 읽어 내려 준다 */
val LocalShoeNicknames = compositionLocalOf<Map<Long, String>> { emptyMap() }
