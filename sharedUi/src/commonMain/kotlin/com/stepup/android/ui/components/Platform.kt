package com.stepup.android.ui.components

import androidx.compose.ui.text.style.LineBreak

/** 한글 낱말 단위 줄바꿈 — 안드로이드는 구(Phrase) 단위 · 균형, iOS 는 문단 기본값(낱말 경계에서 바꾼다) */
expect fun koreanLineBreak(): LineBreak

/** 갤러리에서 고른 아바타 그림(앱 저장 폴더의 [com.stepup.android.data.prefs.UserPrefs.AVATAR_FILE]) — 없거나 깨졌으면 null */
expect fun readCustomAvatar(): androidx.compose.ui.graphics.ImageBitmap?
