package com.stepup.android.ui.components

import androidx.compose.ui.text.style.LineBreak

/** 한글 낱말 단위 줄바꿈 — 안드로이드는 구(Phrase) 단위 · 균형, iOS 는 문단 기본값(낱말 경계에서 바꾼다) */
expect fun koreanLineBreak(): LineBreak
