package com.stepup.android.ui.components

import androidx.compose.ui.text.style.LineBreak

/** 한글 낱말 단위 줄바꿈 — 안드로이드는 구(Phrase) 단위 · 균형, iOS 는 문단 기본값(낱말 경계에서 바꾼다) */
expect fun koreanLineBreak(): LineBreak

/** 갤러리에서 고른 아바타 그림(앱 저장 폴더의 [com.stepup.android.data.prefs.UserPrefs.AVATAR_FILE]) — 없거나 깨졌으면 null */
expect fun readCustomAvatar(): androidx.compose.ui.graphics.ImageBitmap?

/**
 * 시스템 뒤로 가기(안드로이드 BackHandler 자리) — 안드로이드는 뒤로 가기 버튼 · 몸짓을 받는다.
 * iOS 에는 시스템 뒤로 가기가 없어 받지 않는다(화면의 뒤로 · 닫기 버튼만).
 */
@androidx.compose.runtime.Composable
expect fun SystemBackHandler(enabled: Boolean = true, onBack: () -> Unit)

/**
 * 지금 내가 있는 자리 — 아직 모르면 null(안드로이드 CurrentLocation.kt 와 같은 약속). 마지막으로 알려진 위치를 먼저, 그다음 갱신을
 * 화면이 떠 있는 동안만 받는다. 권한이 없거나 위치가 꺼져 있으면 조용히 null — 여기서 권한을 조르지 않는다.
 */
@androidx.compose.runtime.Composable
expect fun rememberCurrentLocation(enabled: Boolean = true): com.stepup.android.domain.GeoPoint?

/** 창 그늘 없는 대화 창 — 화면이 직접 뒷면을 그린다(안드로이드는 창 그늘 0 · 시스템 창 겹치기, iOS 는 그늘 없음) */
expect fun clearDialogProperties(dismissOnClickOutside: Boolean = true): androidx.compose.ui.window.DialogProperties

/** 안드로이드 Bitmap.asImageBitmap() 자리 — 공용 저장소의 사진은 이미 화면 그림이다(옮긴 화면 코드가 그대로 부른다) */
fun androidx.compose.ui.graphics.ImageBitmap.asImageBitmap(): androidx.compose.ui.graphics.ImageBitmap = this

/**
 * Composable 밖에서 글자 하나(안드로이드 context.getString 자리) — 공유 글 · 알림처럼 누르는 순간 만드는 글자.
 * 글자 표는 앱이 이미 읽어 둔 것이라 기다림이 없다.
 */
fun stringNow(res: org.jetbrains.compose.resources.StringResource, vararg args: Any): String =
    kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getString(res, *args) }
