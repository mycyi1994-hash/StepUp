package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.ShareCardOption
import com.stepup.android.ui.components.ShareCards
import com.stepup.android.ui.components.StepUpShareSheet
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.sneakerArtRes
import com.stepup.android.ui.components.v12TierRim
import com.stepup.android.ui.screens.community.crew.withParticle
import com.stepup.android.ui.screens.items.formatShoeNumber
import com.stepup.android.ui.screens.items.shoeName
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date

/**
 * 러닝 공유(13차 S-403 ~ S-405 · S-416) — 공유 시트에 카드 셋: 지도(달린 길) · 기록(파란 면) · 신발(신은 신발).
 * 러닝 완료와 지난 러닝(기록 상세)이 같이 쓴다. 거리를 모르면(GPS 없음) "—", 경로가 없으면 지도 카드를 빼고, 신발이 없으면 신발 카드를 뺀다.
 * SUP 는 적지 않는다.
 */
@Composable
fun RunShareSheet(
    km: Double?,
    elapsedSec: Long,
    startedAt: Long,
    steps: Int,
    track: List<GeoPoint>,
    shoe: Sneaker?,
    onDismiss: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val distance = km?.let { "%.2f".format(java.util.Locale.ROOT, it) } ?: "—"
    val time = formatDuration(elapsedSec)
    val pace = freeAveragePaceSec(km, elapsedSec)?.let { formatPacePrime(it) + "/km" } ?: "—"
    val datePattern = stringResource(R.string.share_run_date_pattern)
    val date = remember(startedAt, datePattern, locale) {
        if (startedAt > 0) runCatching { SimpleDateFormat(datePattern, locale).format(Date(startedAt)) }.getOrDefault("") else ""
    }
    val part = stringResource(
        when (Calendar.getInstance().apply { timeInMillis = startedAt }.get(Calendar.HOUR_OF_DAY)) {
            in 5..10 -> R.string.share_daypart_morning
            in 11..16 -> R.string.share_daypart_day
            in 17..21 -> R.string.share_daypart_evening
            else -> R.string.share_daypart_night
        },
    )
    val title = stringResource(R.string.share_run_title, date, part)
    val text = stringResource(R.string.share_run_text, distance, time)
    val labelTime = stringResource(R.string.share_card_time)
    val labelPace = stringResource(R.string.run_label_pace)
    val labelSteps = stringResource(R.string.share_card_steps)
    val labelShoe = stringResource(R.string.share_card_shoe)
    val chipMap = stringResource(R.string.share_chip_map)
    val chipRecord = stringResource(R.string.share_chip_record)
    val chipShoe = stringResource(R.string.share_chip_shoe)
    val face = shoe?.let { shareFace(it) }
    val shoeLine = shoe?.let { stringResource(R.string.share_run_with_shoe, withParticle(it.shoeName(), "과", "와")) }
    val stepsText = "%,d".format(steps)
    val options = buildList {
        if (track.size >= 2) add(ShareCardOption("map", chipMap) { ctx -> ShareCards.runMap(ctx, track, distance, time, pace, date) })
        add(
            ShareCardOption("record", chipRecord) { ctx ->
                ShareCards.runRecord(
                    ctx, title, distance,
                    listOfNotNull(
                        ShareCards.Stat(labelTime, time), ShareCards.Stat(labelPace, pace),
                        ShareCards.Stat(labelSteps, stepsText), face?.let { ShareCards.Stat(labelShoe, it.name) },
                    ),
                )
            },
        )
        if (face != null && shoe != null && shoeLine != null) {
            add(
                ShareCardOption("shoe", chipShoe) { ctx ->
                    ShareCards.runShoe(ctx, face.copy(art = ShareCards.shoeArt(ctx, sneakerArtRes(shoe))), shoeLine, distance, time, pace)
                },
            )
        }
    }
    StepUpShareSheet(options = options, text = text, fileName = "StepUp-run-$startedAt", onDismiss = onDismiss)
}

/** 공유 카드 안 신발 얼굴(그림은 그릴 때 읽는다) */
@Composable
fun shareFace(shoe: Sneaker): ShareCards.ShoeFace = ShareCards.ShoeFace(
    art = null,
    tier = shoe.tier.label(),
    number = formatShoeNumber(shoe.mintNumber),
    name = shoe.shoeName(),
    rim = v12TierRim(shoe.tier).toArgb(),
)
