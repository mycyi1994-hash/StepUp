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

/**
 * 원뿔(스윕) 그라디언트를 [degrees] 만큼 돌린 붓 — 안드로이드 SweepGradient + setLocalMatrix(setRotate) 자리.
 * 돌리는 대신 색 자리를 같은 만큼 민다(한 바퀴를 넘는 자리는 앞으로 돌리고, 넘는 곳의 색은 사이 값으로 이어 붙인다).
 */
fun rotatedSweepBrush(
    center: androidx.compose.ui.geometry.Offset,
    colors: List<androidx.compose.ui.graphics.Color>,
    stops: List<Float>,
    degrees: Float,
): androidx.compose.ui.graphics.Brush {
    val shift = (((degrees / 360f) % 1f) + 1f) % 1f
    if (shift == 0f) return androidx.compose.ui.graphics.Brush.sweepGradient(*stops.zip(colors).toTypedArray(), center = center)
    // 원래 자리 1 - shift 에서 한 바퀴를 넘는다 — 그 자리의 색
    val cut = 1f - shift
    var i = stops.indexOfFirst { it >= cut }.coerceAtLeast(1)
    val t = if (stops[i] == stops[i - 1]) 0f else (cut - stops[i - 1]) / (stops[i] - stops[i - 1])
    val atCut = androidx.compose.ui.graphics.lerp(colors[i - 1], colors[i], t.coerceIn(0f, 1f))
    val moved = mutableListOf<Pair<Float, androidx.compose.ui.graphics.Color>>()
    moved += 0f to atCut
    stops.forEachIndexed { k, p -> if (p >= cut) moved += (p - cut) to colors[k] }
    stops.forEachIndexed { k, p -> if (p < cut) moved += (p + shift) to colors[k] }
    moved += 1f to atCut
    return androidx.compose.ui.graphics.Brush.sweepGradient(*moved.sortedBy { it.first }.toTypedArray(), center = center)
}

/** 화면이 늘 어두워서 상태바 · 내비바 아이콘을 밝게 — 떠나면 되돌린다(iOS 는 상태바 글자색을 앱이 정해 두어 할 일 없음) */
@androidx.compose.runtime.Composable
expect fun LightSystemBarIcons()

/** 화면 밖에서 수에 맞는 글자(안드로이드 resources.getQuantityString 자리) */
fun pluralNow(res: org.jetbrains.compose.resources.PluralStringResource, quantity: Int, vararg args: Any): String =
    kotlinx.coroutines.runBlocking { org.jetbrains.compose.resources.getPluralString(res, quantity, *args) }

/**
 * 큰 그림(webp · png)을 한 번 풀어 앱 전체에서 다시 쓰는 곳 — 안드로이드는 화면 스레드에서 원본 크기로 푸는 것을 미리 뒤에서 해 둔다
 * (2026-10-04 "렉 걸린다"). iOS 는 Compose 리소스가 처음 그릴 때 읽어 할 일이 없다.
 */
expect object RasterCache {
    /** 이미 풀어 두었는가 — 모르면 true(기다리지 않는다) */
    fun isReady(id: org.jetbrains.compose.resources.DrawableResource): Boolean
    fun prewarm(vararg ids: org.jetbrains.compose.resources.DrawableResource)
}

/**
 * [enabled] 인 동안, 앱이 화면에 있을 때만 위치를 미리 잡아 둔다(러닝 탭 · 시작 흐름 · 크루 대기실) — 러닝을 누를 때 위치가 이미 잡혀 있게.
 * 앱이 뒤로 가면 끈다.
 */
@androidx.compose.runtime.Composable
expect fun RunLocationWarmup(enabled: Boolean)
