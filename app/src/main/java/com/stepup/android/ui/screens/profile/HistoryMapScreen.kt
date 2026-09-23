package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.formatKm
import com.stepup.android.domain.toGeoPoints
import com.stepup.android.domain.trackDistanceKm
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.components.StepUpMap
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

/** 기록 지도에서 볼 기간 */
enum class HistoryPeriod { WEEK, MONTH, ALL }

/** 기록 지도에 그릴 것 — 달린 길들과 합계 */
data class HistoryMap(
    val routes: List<List<GeoPoint>> = emptyList(),
    val runs: Int = 0,
    val km: Double = 0.0,
)

/**
 * 기록 지도 — 이 폰에 남은 GPS 러닝을 한 지도에 겹친다.
 *
 * 선을 옅게 그려 겹칠수록 밝아지게 한다. 자주 달린 길이 저절로 히트맵처럼
 * 떠오른다. 경로는 폰에만 있고 이 화면은 서버에 묻지 않는다.
 */
class HistoryMapViewModel(dao: WalkSessionDao) : ViewModel() {

    val period = MutableStateFlow(HistoryPeriod.MONTH)

    val map: StateFlow<HistoryMap> = combine(dao.observeRecent(MAX_SESSIONS), period) { sessions, p ->
        val since = periodStart(p)
        val routes = sessions
            .filter { it.startedAt >= since && it.track.isNotBlank() }
            .map { RunTrack.decode(it.track).toGeoPoints() }
            .filter { it.size >= 2 }
        HistoryMap(
            // 그리기가 가볍도록 한 러닝당 최대 400점으로 솎는다
            routes = routes.map { r -> if (r.size <= 400) r else r.filterIndexed { i, _ -> i % (r.size / 400 + 1) == 0 } },
            runs = routes.size,
            km = routes.sumOf { it.trackDistanceKm() },
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryMap())

    private fun periodStart(p: HistoryPeriod): Long {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val start = when (p) {
            HistoryPeriod.WEEK -> today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            HistoryPeriod.MONTH -> today.withDayOfMonth(1)
            HistoryPeriod.ALL -> return 0L
        }
        return start.atStartOfDay(zone).toInstant().toEpochMilli()
    }

    companion object {
        private const val MAX_SESSIONS = 500

        val Factory = viewModelFactory {
            initializer { HistoryMapViewModel(ServiceLocator.database.walkSessionDao()) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HistoryMapScreen(
    onBack: () -> Unit = {},
    viewModel: HistoryMapViewModel = viewModel(factory = HistoryMapViewModel.Factory),
) {
    val period by viewModel.period.collectAsStateWithLifecycle()
    val map by viewModel.map.collectAsStateWithLifecycle()
    val focus = remember(map) { map.routes.flatten() }

    Column(Modifier.fillMaxSize()) {
        SecondaryHeader(
            title = stringResource(R.string.history_map_title),
            onBack = onBack, balance = null, onOpenWallet = null,
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PillChip(
                text = stringResource(R.string.history_period_week),
                selected = period == HistoryPeriod.WEEK,
                onClick = { viewModel.period.value = HistoryPeriod.WEEK },
            )
            PillChip(
                text = stringResource(R.string.history_period_month),
                selected = period == HistoryPeriod.MONTH,
                onClick = { viewModel.period.value = HistoryPeriod.MONTH },
            )
            PillChip(
                text = stringResource(R.string.history_period_all),
                selected = period == HistoryPeriod.ALL,
                onClick = { viewModel.period.value = HistoryPeriod.ALL },
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = StepUpDesign.Gutter)
                .clip(RoundedCornerShape(18.dp)),
        ) {
            StepUpMap(
                focus = focus,
                modifier = Modifier.fillMaxSize(),
                interactive = true,
            ) { plan ->
                for (route in map.routes) {
                    val screen = route.map { plan.toScreen(it) }
                    val path = Path().apply {
                        moveTo(screen[0].x, screen[0].y)
                        for (i in 1 until screen.size) lineTo(screen[i].x, screen[i].y)
                    }
                    // 옅은 선을 더해 가며 겹친다 — 많이 달린 길일수록 밝게 타오른다
                    drawPath(
                        path,
                        color = Volt.copy(alpha = 0.16f),
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                        blendMode = BlendMode.Plus,
                    )
                    drawPath(
                        path,
                        color = Volt.copy(alpha = 0.55f),
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }

        Box(Modifier.padding(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 12.dp, bottom = 18.dp)) {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 6.dp) {
                if (map.runs == 0) {
                    Text(
                        text = stringResource(R.string.history_map_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.history_map_stats, map.runs, formatKm(map.km)),
                        color = Snow,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = stringResource(R.string.history_map_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }
    }
}
