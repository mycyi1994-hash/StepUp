package com.stepup.android.ui.screens.community.stories

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.stepup.ui.res.*
import com.stepup.android.core.AppServices
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.formatStoryDistance
import com.stepup.android.domain.haversineMeters
import com.stepup.android.domain.GeoPoint
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.rememberCurrentLocation

/**
 * 상세 · 작성 · 지도 화면이 쓰는 기준점 — 목록과 같은 규칙(직접 고른 지역이 먼저, 없으면 내 위치).
 * 위치 권한이 없고 고른 지역도 없으면 null — 거리를 만들지 않는다.
 */
@Composable
fun rememberStoryOrigin(): StoryOrigin? {
    var allowed by remember { mutableStateOf(com.stepup.android.ui.StepPermissions.hasLocation()) }
    LifecycleResumeEffect(Unit) {
        allowed = com.stepup.android.ui.StepPermissions.hasLocation()
        onPauseOrDispose { }
    }
    val region by AppServices.userPrefs.storyRegion.collectAsState(initial = null)
    val here = rememberCurrentLocation(enabled = allowed && region == null)
    val chosen = region
    return when {
        chosen != null -> StoryOrigin(chosen.point, chosen.name, manual = true)
        here != null -> StoryOrigin(here, "", manual = false)
        else -> null
    }
}

/** "내 위치에서 300m" · "선택한 지역에서 300m". 기준점이 없으면 빈 문자열 */
@Composable
fun distanceFrom(origin: StoryOrigin?, point: GeoPoint): String {
    val from = origin ?: return ""
    val text = formatStoryDistance(haversineMeters(from.point, point))
    if (text.isEmpty()) return ""
    return stringResource(if (from.manual) Res.string.story_distance_from_region else Res.string.story_distance_from_me, text)
}
