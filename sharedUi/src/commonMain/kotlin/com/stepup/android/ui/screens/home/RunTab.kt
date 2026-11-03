package com.stepup.android.ui.screens.home

import com.stepup.android.domain.plusDays
import com.stepup.android.domain.todayIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
import org.jetbrains.compose.resources.decodeToImageBitmap
import com.stepup.android.domain.CrewRoom
import com.stepup.android.domain.CrewRoomRules
import com.stepup.android.domain.RoomMode
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpSpinner
import com.stepup.android.ui.components.StepUpTabs
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.pressable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class GpsReadiness { READY, WAITING, NO_PERMISSION }

/** 사람 없는 배경 사진 15장(2026-10-26 전달본 02-backgrounds-15) — assets/run_photos */
object RunPhotos {
    val names = listOf(
        "01-blue-track", "02-midnight-stadium", "03-riverside-dawn", "04-bridge-path", "05-coastal-road",
        "06-forest-path", "07-city-night", "08-stadium-stairs", "09-running-tunnel", "10-rain-track",
        "11-lakeside-path", "12-mountain-ridge", "13-blue-court", "14-snow-promenade", "15-track-curve",
    )
    private val cache = LinkedHashMap<String, ImageBitmap>()

    /** files/run_photos/<이름>.webp — 안드로이드는 앱 assets 의 같은 사진. 세 장까지 기억한다 */
    @OptIn(org.jetbrains.compose.resources.ExperimentalResourceApi::class)
    suspend fun decode(name: String): ImageBitmap? {
        cache[name]?.let { return it }
        return runCatching {
            Res.readBytes("files/run_photos/$name.webp").decodeToImageBitmap()
        }.getOrNull()?.also {
            cache[name] = it
            while (cache.size > 3) cache.remove(cache.keys.first())
        }
    }
}

/** 큰 제목(시안 "러닝" · "크루 러닝") */
@Composable
fun RunTabTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text, style = StepUpUi.type.largeTitle.copy(fontSize = 30.sp, letterSpacing = (-0.03).em), color = StepUpUi.colors.label,
        modifier = modifier.semantics { heading() }.testTag("run-tab-title"),
    )
}

/** 오늘 걸음 — 왼쪽 "오늘 걸음" · 큰 숫자 · "/ 목표 걸음", 오른쪽 목표까지 고리(발자국). 누르면 오늘의 활동(E01) */
@Composable
fun StepsRing(fraction: Float, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Box(modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = 5.dp.toPx()
            val inset = w / 2
            val arc = Size(size.width - w, size.height - w)
            drawArc(c.separator, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(w))
            if (fraction > 0f) drawArc(c.primary, -90f, 360f * fraction.coerceIn(0f, 1f), false, Offset(inset, inset), arc,
                style = Stroke(w, cap = StrokeCap.Round))
        }
        Icon(
            painterResource(Res.drawable.ic_footprints_d26), contentDescription = null, tint = c.primary, modifier = Modifier.size(24.dp),
        )
    }
}

/** 배경 사진 카드(361 × 178) — 사람 없는 15장 중 하나, 왼쪽 아래 "나만의 속도로 / 오늘의 러닝을 시작해 볼까요?" */
@Composable
fun RunPhotoCard(photoIndex: Int, modifier: Modifier = Modifier) {
    val name = RunPhotos.names[photoIndex.mod(RunPhotos.names.size)]
    val bitmap by produceState<ImageBitmap?>(initialValue = null, name) { value = RunPhotos.decode(name) }
    val shape = RoundedCornerShape(24.dp)
    Box(modifier.fillMaxWidth().height(178.dp).clip(shape).background(Color(0xFF0B1A33)).testTag("home-spot")) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().testTag("run-photo-$name"))
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.horizontalGradient(0f to Color.Black.copy(alpha = .55f), .7f to Color.Black.copy(alpha = .05f), 1f to Color.Transparent),
            ),
        )
        Column(Modifier.align(Alignment.CenterStart).padding(start = 20.dp, end = 20.dp, top = 24.dp)) {
            Text(stringResource(Res.string.d26_photo_kicker), style = StepUpUi.type.caption.copy(fontSize = 13.sp), color = Color.White.copy(alpha = .9f))
            Text(
                stringResource(Res.string.d26_photo_title),
                style = StepUpUi.type.largeTitle.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.03).em), color = Color.White,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

/** GPS 준비 줄 — 초록 점 · "GPS 준비 완료"(미리 잡은 위치가 10초 안 · 50m 안). 권한이 없으면 그렇게 쓴다 */
@Composable
fun GpsLine(state: GpsReadiness, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Row(modifier.testTag("run-gps-${state.name.lowercase()}"), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (state == GpsReadiness.READY) Color(0xFF3CC08A) else c.tertiaryLabel))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(
                when (state) {
                    GpsReadiness.READY -> Res.string.d26_gps_ready
                    GpsReadiness.WAITING -> Res.string.d26_gps_waiting
                    GpsReadiness.NO_PERMISSION -> Res.string.d26_gps_permission
                },
            ),
            style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel,
        )
    }
}
