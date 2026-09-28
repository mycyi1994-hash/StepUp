package com.stepup.android.ui.screens.rewards

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MilitaryTech
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Token
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.SafeUrl
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.ChainNetwork
import com.stepup.android.domain.ChainRecord
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 체인 기록 — 내 정보 › 지갑 › 체인 기록.
 *
 * 서버가 GIWA Sepolia 에 올린(또는 올릴) 내 기록을 최근 것부터 보여 준다: 러닝 증명 · 코스 완주 · 배지,
 * 뽑은 신발 발행 · 강화/수리 반영. 가스비는 StepUp 이 내고, 사용자는 서명하지 않는다.
 * 확정 전에는 "대기 중" — 확정된 줄만 거래 번호가 있고, 누르면 익스플로러에서 그 거래를 연다.
 * 예시 기록을 만들지 않는다(서버가 준 것만).
 */
sealed interface ChainActivityLoad {
    data object Loading : ChainActivityLoad
    data object SignedOut : ChainActivityLoad
    data object Failed : ChainActivityLoad
    data class Ready(val records: List<ChainRecord>) : ChainActivityLoad
}

class ChainActivityViewModel : ViewModel() {
    private val _state = MutableStateFlow<ChainActivityLoad>(ChainActivityLoad.Loading)
    val state: StateFlow<ChainActivityLoad> = _state.asStateFlow()

    /** 신발 이름을 붙이려고 — 폰에 받아 둔 내 신발(서버 신발의 번호 그대로) */
    val sneakers: StateFlow<Map<Long, Sneaker>> = ServiceLocator.sneakerRepository.inventory
        .map { list -> list.associateBy { it.id } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun reload() {
        viewModelScope.launch {
            if (_state.value !is ChainActivityLoad.Ready) _state.value = ChainActivityLoad.Loading
            _state.value = try {
                if (!ServiceLocator.serverEconomyOn || !ServiceLocator.sessionHolder.isSignedIn()) {
                    ChainActivityLoad.SignedOut
                } else {
                    when (val r = ServiceLocator.economyApi.chainActivity()) {
                        is ServerResult.Ok -> ChainActivityLoad.Ready(r.value.map { it.toDomain() })
                        else -> (_state.value as? ChainActivityLoad.Ready) ?: ChainActivityLoad.Failed
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                (_state.value as? ChainActivityLoad.Ready) ?: ChainActivityLoad.Failed
            }
        }
    }

    companion object {
        val Factory = viewModelFactory { initializer { ChainActivityViewModel() } }
    }
}

@Composable
fun ChainActivityScreen(
    onBack: () -> Unit = {},
    viewModel: ChainActivityViewModel = viewModel(factory = ChainActivityViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val sneakers by viewModel.sneakers.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // 러닝 · 뽑기를 하고 돌아오면 바로 보이게 — 화면에 돌아올 때마다 다시 읽는다
    LifecycleResumeEffect(Unit) {
        viewModel.reload()
        onPauseOrDispose { }
    }
    ChainActivityContent(
        state = state,
        sneakers = sneakers,
        onBack = onBack,
        onReload = viewModel::reload,
        onOpenTx = { url ->
            if (SafeUrl.looksSafe(url)) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (_: ActivityNotFoundException) {
                }
            }
        },
    )
}

@Composable
fun ChainActivityContent(
    state: ChainActivityLoad,
    sneakers: Map<Long, Sneaker> = emptyMap(),
    onBack: () -> Unit = {},
    onReload: () -> Unit = {},
    onOpenTx: (String) -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    thisYear: Int = Year.now(zone).value,
    initialGuide: Boolean = false,
) {
    val p = settingsPalette()
    var guide by rememberSaveable { mutableStateOf(initialGuide) }
    Column(Modifier.fillMaxSize()) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.ca_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
            trailing = {
                IconButton(onClick = { guide = true }, modifier = Modifier.size(48.dp).testTag("ca-guide")) {
                    Icon(Icons.Outlined.Info, stringResource(R.string.ca_guide_cd), tint = p.text)
                }
            },
        )
        when (state) {
            ChainActivityLoad.Failed -> WalletState(
                icon = Icons.Filled.Refresh, title = stringResource(R.string.ca_failed_title), body = stringResource(R.string.ca_failed_body),
                action = stringResource(R.string.wl_reload), onAction = onReload,
                modifier = Modifier.weight(1f).padding(horizontal = StepUpDesign.Gutter).testTag("ca-failed"), primary = true,
            )
            ChainActivityLoad.SignedOut -> WalletState(
                icon = Icons.Outlined.Link, title = stringResource(R.string.ca_signed_out_title), body = stringResource(R.string.ca_signed_out_body),
                action = null, onAction = {},
                modifier = Modifier.weight(1f).padding(horizontal = StepUpDesign.Gutter).testTag("ca-signed-out"),
            )
            else -> LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().testTag("ca-list"),
                contentPadding = PaddingValues(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 4.dp, bottom = 28.dp),
            ) {
                item(key = "summary") { ChainSummary(state as? ChainActivityLoad.Ready) }
                when (state) {
                    is ChainActivityLoad.Ready -> if (state.records.isEmpty()) {
                        item(key = "empty") {
                            WalletState(
                                icon = Icons.Outlined.Link, title = stringResource(R.string.ca_empty_title),
                                body = stringResource(R.string.ca_empty_body), action = stringResource(R.string.ca_guide_open),
                                onAction = { guide = true }, modifier = Modifier.testTag("ca-empty"), compact = true,
                            )
                        }
                    } else {
                        items(state.records, key = { it.id }) { record ->
                            ChainRow(record, sneakers[record.sneakerId], zone, thisYear, onOpenTx)
                        }
                    }
                    else -> item(key = "loading") { ChainSkeleton() }
                }
            }
        }
    }
    if (guide) ChainGuideSheet(onClose = { guide = false })
}

/** 맨 위 — 어디에 올라가는지 · 누가 가스비를 내는지 · 기록됨 · 대기 수(읽기 전에는 숫자를 보이지 않는다) */
@Composable
private fun ChainSummary(ready: ChainActivityLoad.Ready?) {
    val p = settingsPalette()
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(p.surface)
            .padding(horizontal = 20.dp, vertical = 18.dp).testTag("ca-summary"),
    ) {
        Text(stringResource(R.string.ca_summary_head, ChainNetwork.NAME), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.ca_summary_body), color = p.secondary, fontSize = 13.5.sp, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 6.dp))
        if (ready != null) {
            val confirmed = ready.records.count { it.status == ChainRecord.Status.CONFIRMED }
            val pending = ready.records.count { it.status == ChainRecord.Status.PENDING }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                SummaryCount(stringResource(R.string.ca_count_confirmed), confirmed, "ca-count-confirmed")
                SummaryCount(stringResource(R.string.ca_count_pending), pending, "ca-count-pending")
            }
        }
    }
}

@Composable
private fun SummaryCount(label: String, value: Int, tag: String) {
    val p = settingsPalette()
    Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, color = p.secondary, fontSize = 13.sp)
        Spacer(Modifier.width(8.dp))
        Text("$value", color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 기록 한 줄 — 무엇 · 언제(무엇의 값) · 상태. 확정된 줄만 눌러서 익스플로러로 */
@Composable
private fun ChainRow(record: ChainRecord, sneaker: Sneaker?, zone: ZoneId, thisYear: Int, onOpenTx: (String) -> Unit) {
    val p = settingsPalette()
    val title = chainTitle(record)
    val at = Instant.ofEpochMilli(record.confirmedAt ?: record.createdAt).atZone(zone)
    val date = rememberChainFormatter(if (at.year == thisYear) R.string.wl_date_pattern else R.string.wl_date_year_pattern).format(at)
    val detail = chainDetail(record, sneaker)
    val subtitle = listOfNotNull(detail, date).joinToString(" · ")
    val status = stringResource(
        when (record.status) {
            ChainRecord.Status.CONFIRMED -> R.string.ca_status_confirmed
            ChainRecord.Status.PENDING -> R.string.ca_status_pending
            ChainRecord.Status.CANCELLED -> R.string.ca_status_cancelled
            ChainRecord.Status.FAILED -> R.string.ca_status_failed
        },
    )
    val url = record.explorerUrl
    val spoken = stringResource(R.string.ca_row_cd, title, subtitle, status)
    Column(
        Modifier.fillMaxWidth()
            .then(if (url != null) Modifier.feedbackClickable(role = Role.Button) { onOpenTx(url) } else Modifier)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .testTag("ca-row-${record.id}"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 78.dp).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.surface), contentAlignment = Alignment.Center) {
                Icon(chainIcon(record.kind), contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em)
                Text(subtitle, color = p.secondary, fontSize = 12.5.sp, lineHeight = 1.35.em, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    status,
                    color = if (record.status == ChainRecord.Status.CONFIRMED) p.accent else p.secondary,
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.testTag("ca-status-${record.id}"),
                )
                if (url != null) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = p.secondary, modifier = Modifier.size(15.dp))
                }
            }
        }
        Box(Modifier.padding(start = 52.dp).fillMaxWidth().height(1.dp).background(p.divider))
    }
}

private fun chainIcon(kind: ChainRecord.Kind): ImageVector = when (kind) {
    ChainRecord.Kind.RUN_PROOF -> Icons.AutoMirrored.Filled.DirectionsRun
    ChainRecord.Kind.COURSE_RUN -> Icons.Outlined.Flag
    ChainRecord.Kind.BADGE -> Icons.Outlined.MilitaryTech
    ChainRecord.Kind.VAULT_MINT -> Icons.Outlined.Token
    ChainRecord.Kind.STATS_SYNC -> Icons.Outlined.Sync
    ChainRecord.Kind.UNKNOWN -> Icons.Outlined.Link
}

@Composable
private fun chainTitle(record: ChainRecord): String = when (record.kind) {
    ChainRecord.Kind.RUN_PROOF -> stringResource(R.string.ca_kind_run)
    ChainRecord.Kind.COURSE_RUN -> stringResource(R.string.ca_kind_course)
    ChainRecord.Kind.BADGE -> stringResource(R.string.ca_kind_badge, badgeLabel(record.badge, record.badgeValue))
    ChainRecord.Kind.VAULT_MINT -> stringResource(R.string.ca_kind_mint)
    ChainRecord.Kind.STATS_SYNC -> stringResource(R.string.ca_kind_sync)
    ChainRecord.Kind.UNKNOWN -> stringResource(R.string.ca_kind_other)
}

@Composable
private fun badgeLabel(badge: String?, value: Int?): String = when (badge) {
    "FIRST_RUN" -> stringResource(R.string.ca_badge_first_run)
    "DISTANCE_KM" -> stringResource(R.string.ca_badge_distance, value ?: 0)
    "STREAK_DAYS" -> stringResource(R.string.ca_badge_streak, value ?: 0)
    else -> badge.orEmpty()
}

/** 무엇의 값 — 러닝은 거리 · 시간, 신발은 이름(폰에 없으면 "신발") */
@Composable
private fun chainDetail(record: ChainRecord, sneaker: Sneaker?): String? = when (record.kind) {
    ChainRecord.Kind.RUN_PROOF, ChainRecord.Kind.COURSE_RUN -> record.distanceM?.let { m ->
        val km = String.format(Locale.US, "%.1f", m / 1000.0)
        val minutes = record.durationSec?.let { (it + 30) / 60 }
        if (minutes != null && minutes > 0) stringResource(R.string.ca_run_detail, km, minutes) else stringResource(R.string.ca_run_km, km)
    }
    ChainRecord.Kind.VAULT_MINT, ChainRecord.Kind.STATS_SYNC -> sneaker?.variantLabel() ?: stringResource(R.string.ca_sneaker)
    else -> null
}

@Composable
private fun rememberChainFormatter(pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    return remember(text) { DateTimeFormatter.ofPattern(text) }
}

/** 읽는 중 — 줄 자리만 */
@Composable
private fun ChainSkeleton() {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("ca-loading")) {
        repeat(4) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(p.skeleton))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
                    Box(Modifier.width(96.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton.copy(alpha = 0.7f)))
                }
                Box(Modifier.width(52.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
            }
        }
    }
}

/**
 * 어디서 무엇을 하면 기록되나 — docs/온체인-거래-발생-지도.md 의 앱 부분.
 * 한 일 하나 = 기록 최대 하나. 기록 수를 늘리려고 쪼개지 않는다.
 */
@Composable
fun ChainGuideSheet(onClose: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.ca_guide_title), onDismiss = onClose, modifier = Modifier.testTag("ca-guide-sheet"),
        actions = { SettingsPrimaryButton(stringResource(R.string.wl_ok), onClose, Modifier.fillMaxWidth()) },
    ) {
        Text(stringResource(R.string.ca_guide_intro), color = p.secondary, fontSize = 14.5.sp, lineHeight = 1.45.em)
        listOf(
            Triple(Icons.AutoMirrored.Filled.DirectionsRun, R.string.ca_guide_run_head, R.string.ca_guide_run_body),
            Triple(Icons.Outlined.MilitaryTech, R.string.ca_guide_badge_head, R.string.ca_guide_badge_body),
            Triple(Icons.Outlined.Flag, R.string.ca_guide_course_head, R.string.ca_guide_course_body),
            Triple(Icons.Outlined.Token, R.string.ca_guide_mint_head, R.string.ca_guide_mint_body),
            Triple(Icons.Outlined.Sync, R.string.ca_guide_sync_head, R.string.ca_guide_sync_body),
            Triple(Icons.Outlined.Link, R.string.ca_guide_wallet_head, R.string.ca_guide_wallet_body),
        ).forEach { (icon, head, body) ->
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.Top) {
                Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(head), color = p.text, fontSize = 15.5.sp, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(body), color = p.secondary, fontSize = 13.5.sp, lineHeight = 1.4.em, modifier = Modifier.padding(top = 3.dp))
                }
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 8.dp).height(1.dp).background(p.divider))
        Text(stringResource(R.string.ca_guide_foot), color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em)
    }
}
