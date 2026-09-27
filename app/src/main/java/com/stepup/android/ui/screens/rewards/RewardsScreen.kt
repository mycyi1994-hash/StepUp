package com.stepup.android.ui.screens.rewards

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
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.data.local.RewardEntity
import com.stepup.android.data.local.RewardTotals
import com.stepup.android.data.local.RewardType
import com.stepup.android.domain.DrawDistance
import com.stepup.android.domain.DrawStatus
import com.stepup.android.ui.components.RunningPathCard
import com.stepup.android.ui.components.RunningPathColors
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.Year
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private enum class WalletSheet { None, Info, Linked, Benefit }

/**
 * 지갑(지갑 v1, 2026-09-28 전달본, docs/redesign/wallet-v1) — 잔액 → WEB3 연결 줄 → 이용 내역.
 *
 * 잔액 · 누적 적립 · 누적 사용은 원장 전체의 기존 정의 그대로(필터를 바꿔도 같다). 내역은 전체 · 적립(양수) · 사용(음수)을
 * 원장 전체에서 거른 뒤 한 쪽씩 읽는다. 줄을 누르면 그 원장 줄의 금액 · 구분 · 일시 · 내용만(없는 해시 · 확정 상태를 만들지 않는다).
 * WEB3 줄은 이 빌드에 연결 기능이 없으면 "준비 중"(누르지 않음), 있으면 서버가 아는 실제 연결 — 연결 전이면 혜택 안내 →
 * 기존 웹 지갑 페이지, 연결됐으면 상태 안내 → 신발 뽑기(자동으로 뽑지 않는다) · 웹 지갑 페이지. SUP 와 뽑기 기회는 따로다.
 */
@Composable
fun WalletScreen(
    onBack: () -> Unit = {},
    onOpenDraw: () -> Unit = {},
    onOpenWalletPage: () -> Unit = {},
    viewModel: RewardsViewModel = viewModel(factory = RewardsViewModel.Factory),
) {
    val totals by viewModel.totals.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val web3 by viewModel.web3.collectAsStateWithLifecycle()
    val status by viewModel.drawStatus.collectAsStateWithLifecycle()
    // 웹 지갑 페이지에서 연결하고 돌아오면 바로 바뀌게 — 화면에 돌아올 때마다 연결 상태를 다시 읽는다
    LifecycleResumeEffect(Unit) {
        viewModel.refreshWeb3()
        onPauseOrDispose { }
    }
    WalletContent(
        totals = totals, history = history, filter = filter, web3 = web3, status = status,
        onBack = onBack, onFilter = viewModel::setFilter, onLoadMore = viewModel::loadMore,
        onReloadHistory = viewModel::reloadHistory, onReload = viewModel::reload, onRetryWeb3 = viewModel::refreshWeb3,
        onOpenDraw = onOpenDraw, onOpenWalletPage = onOpenWalletPage,
    )
}

@Composable
fun WalletContent(
    totals: TotalsLoad,
    history: HistoryLoad,
    filter: LedgerFilter,
    web3: Web3State,
    status: DrawStatus? = null,
    onBack: () -> Unit = {},
    onFilter: (LedgerFilter) -> Unit = {},
    onLoadMore: () -> Unit = {},
    onReloadHistory: () -> Unit = {},
    onReload: () -> Unit = {},
    onRetryWeb3: () -> Unit = {},
    onOpenDraw: () -> Unit = {},
    onOpenWalletPage: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    zone: ZoneId = ZoneId.systemDefault(),
    thisYear: Int = Year.now(zone).value,
    initialSheet: String? = null,
    initialEntry: RewardEntity? = null,
) {
    val p = settingsPalette()
    var sheet by rememberSaveable { mutableStateOf(initialSheet?.let { WalletSheet.valueOf(it) } ?: WalletSheet.None) }
    var opened by remember { mutableStateOf(initialEntry) }
    Column(Modifier.fillMaxSize()) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.wl_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
            trailing = {
                IconButton(onClick = { sheet = WalletSheet.Info }, modifier = Modifier.size(48.dp).testTag("wl-info")) {
                    Icon(Icons.Outlined.Info, stringResource(R.string.wl_info_cd), tint = p.text)
                }
            },
        )
        if (totals == TotalsLoad.Failed) {
            // 10 — 확인한 잔액이 없다. 0 으로 보이지 않고 다시 불러오게
            WalletState(
                icon = Icons.Filled.Refresh, title = stringResource(R.string.wl_failed_title), body = stringResource(R.string.wl_failed_body),
                action = stringResource(R.string.wl_reload), onAction = onReload,
                modifier = Modifier.weight(1f).padding(horizontal = StepUpDesign.Gutter).testTag("wl-failed"), primary = true,
            )
            return@Column
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().testTag("wl-list"),
            contentPadding = PaddingValues(start = StepUpDesign.Gutter, end = StepUpDesign.Gutter, top = 4.dp, bottom = 28.dp),
        ) {
            item(key = "balance") { BalanceCard(totals) }
            item(key = "web3") {
                Web3Row(
                    web3,
                    onClick = {
                        when (web3) {
                            Web3State.NotLinked -> sheet = WalletSheet.Benefit
                            Web3State.Linked -> sheet = WalletSheet.Linked
                            else -> Unit
                        }
                    },
                    onRetry = onRetryWeb3,
                )
            }
            item(key = "history-head") { HistoryHead(filter, onFilter) }
            when (history) {
                HistoryLoad.Loading -> item(key = "history-loading") { HistorySkeleton() }
                HistoryLoad.Failed -> item(key = "history-failed") {
                    // 11 — 잔액은 확인했다. 내역만 다시
                    WalletState(
                        icon = Icons.Filled.Refresh, title = stringResource(R.string.wl_history_failed_title),
                        body = stringResource(R.string.wl_history_failed_body), action = stringResource(R.string.wl_history_reload),
                        onAction = onReloadHistory, modifier = Modifier.testTag("wl-history-failed"), primary = true, compact = true,
                    )
                }
                is HistoryLoad.Ready -> {
                    if (history.stale) item(key = "stale") { StaleNote() }
                    if (history.rows.isEmpty()) {
                        item(key = "empty-${history.filter}") { EmptyHistory(history.filter, onShowAll = { onFilter(LedgerFilter.ALL) }) }
                    } else {
                        items(history.rows, key = { it.id }) { entry -> LedgerRow(entry, zone, thisYear) { opened = entry } }
                        if (history.pageFailed) {
                            item(key = "page-failed") {
                                // 16 — 읽은 줄 · 자리는 그대로, 다음 쪽만 다시
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 26.dp).testTag("wl-page-failed"),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(stringResource(R.string.wl_page_failed), color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
                                    SettingsSecondaryButton(stringResource(R.string.wl_reload), onReloadHistory,
                                        Modifier.padding(top = 14.dp).widthIn(min = 200.dp).testTag("wl-page-retry"))
                                }
                            }
                        } else if (history.more) {
                            item(key = "more-${history.rows.size}") {
                                LaunchedEffect(history.rows.size) { onLoadMore() }
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    opened?.let { entry -> EntrySheet(entry, zone, onClose = { opened = null }) }
    when (sheet) {
        WalletSheet.Info -> InfoSheet(onClose = { sheet = WalletSheet.None })
        WalletSheet.Linked -> LinkedSheet(
            onOpenDraw = {
                sheet = WalletSheet.None
                onOpenDraw()
            },
            onOpenWalletPage = {
                sheet = WalletSheet.None
                onOpenWalletPage()
            },
            onClose = { sheet = WalletSheet.None },
        )
        WalletSheet.Benefit -> BenefitSheet(
            status = status,
            onConnect = {
                sheet = WalletSheet.None
                onOpenWalletPage()
            },
            onClose = { sheet = WalletSheet.None },
        )
        WalletSheet.None -> Unit
    }
}

/** 잔액 카드 — 러닝 패스 카드의 면과 장식. 읽기 전에는 자리만(0 으로 보이지 않는다) */
@Composable
private fun BalanceCard(totals: TotalsLoad) {
    val ready = (totals as? TotalsLoad.Ready)?.totals
    val balance = ready?.let { amountText(it.balance, signed = false) }
    val size: TextUnit = when {
        balance == null -> 55.sp
        balance.length > 10 -> 38.sp
        balance.length > 7 -> 46.sp
        else -> 55.sp
    }
    RunningPathCard(Modifier.padding(top = 8.dp).testTag("wl-balance-card"), textEnd = 230.dp, minHeight = 176.dp) {
        Column(Modifier.padding(start = 22.dp, end = 18.dp)) {
            Text(stringResource(R.string.wl_balance), color = RunningPathColors.title, fontSize = 15.sp,
                modifier = Modifier.paddingFromBaseline(top = 36.dp))
            if (balance != null) {
                Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("wl-balance")) {
                    Text(balance, color = RunningPathColors.number, fontSize = size, fontWeight = FontWeight.SemiBold, maxLines = 1,
                        modifier = Modifier.alignByBaseline().paddingFromBaseline(top = 62.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.wl_unit), color = RunningPathColors.unit, fontSize = 19.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.alignByBaseline())
                }
            } else {
                Box(Modifier.padding(top = 18.dp).size(width = 170.dp, height = 44.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33A6BCDD)).testTag("wl-balance-loading"))
            }
            Box(Modifier.padding(top = 16.dp).fillMaxWidth().height(1.dp).background(Color(0x33A6BCDD)))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 16.dp)) {
                TotalColumn(stringResource(R.string.wl_earned), ready?.let { amountText(it.earned, signed = false) }, "wl-earned", Modifier.weight(1f))
                TotalColumn(stringResource(R.string.wl_spent), ready?.let { amountText(it.spent, signed = false) }, "wl-spent", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TotalColumn(label: String, value: String?, tag: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}.testTag(tag)) {
        Text(label, color = RunningPathColors.title, fontSize = 13.sp)
        if (value != null) {
            Text(value, color = RunningPathColors.number, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
        } else {
            Box(Modifier.padding(top = 8.dp).size(width = 72.dp, height = 12.dp).clip(RoundedCornerShape(6.dp)).background(Color(0x33A6BCDD)))
        }
    }
}

/** WEB3 지갑 줄 — 실제로 되는 것만 누를 수 있다 */
@Composable
private fun Web3Row(web3: Web3State, onClick: () -> Unit, onRetry: () -> Unit) {
    val p = settingsPalette()
    val clickable = web3 == Web3State.NotLinked || web3 == Web3State.Linked
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 76.dp)
                .then(if (clickable) Modifier.feedbackClickable(role = Role.Button, onClick = onClick) else Modifier)
                .semantics(mergeDescendants = true) {}
                .testTag("wl-web3"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AccountBalanceWallet, contentDescription = null, tint = p.accent, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(18.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.wl_web3), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    if (web3 == Web3State.Linked) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(
                        stringResource(
                            when (web3) {
                                Web3State.Checking -> R.string.wl_web3_checking
                                Web3State.Unavailable -> R.string.wl_web3_unavailable
                                Web3State.SignedOut -> R.string.wl_web3_signed_out
                                Web3State.NotLinked -> R.string.wl_web3_unlinked
                                Web3State.Linked -> R.string.wl_web3_linked
                                Web3State.Unknown -> R.string.wl_web3_unknown
                            },
                        ),
                        color = if (web3 == Web3State.Linked) p.accent else p.secondary, fontSize = 14.sp,
                        modifier = Modifier.testTag("wl-web3-state"),
                    )
                }
            }
            when {
                web3 == Web3State.Unavailable -> Text(stringResource(R.string.wl_web3_soon), color = p.secondary, fontSize = 13.sp)
                clickable -> Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary)
            }
        }
        if (web3 == Web3State.Unknown) {
            Box(
                Modifier.padding(start = 44.dp).heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onRetry)
                    .testTag("wl-web3-retry"),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(stringResource(R.string.wl_web3_retry), color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Box(Modifier.padding(top = 4.dp).fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** 이용 내역 제목 · 최근순 · 거르개(전체 · 적립 · 사용) — 고른 것만 밝은 면. 누르는 곳은 48dp */
@Composable
private fun HistoryHead(filter: LedgerFilter, onFilter: (LedgerFilter) -> Unit) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.wl_history), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.wl_recent_first), color = p.secondary, fontSize = 13.sp)
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                LedgerFilter.ALL to R.string.wl_filter_all,
                LedgerFilter.EARNED to R.string.wl_filter_earned,
                LedgerFilter.SPENT to R.string.wl_filter_spent,
            ).forEach { (value, label) ->
                val on = value == filter
                Box(
                    Modifier.heightIn(min = 48.dp).feedbackClickable(role = null) { onFilter(value) }
                        .semantics {
                            role = Role.Tab
                            selected = on
                        }
                        .testTag("wl-filter-${value.name.lowercase()}"),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.widthIn(min = 76.dp).clip(RoundedCornerShape(18.dp))
                            .background(if (on) chipOn() else chipOff()).padding(horizontal = 20.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(label), color = if (on) chipOnText() else p.text.copy(alpha = 0.85f), fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** 원장 한 줄 — 부호 표시 · 종류 · 일시(올해가 아니면 연도까지) · 금액(적립은 파랑, 사용은 밝은 중립). 줄 전체가 누르는 곳 */
@Composable
private fun LedgerRow(entry: RewardEntity, zone: ZoneId, thisYear: Int, onClick: () -> Unit) {
    val p = settingsPalette()
    val title = stringResource(ledgerLabel(entry.type))
    val at = Instant.ofEpochMilli(entry.timestamp).atZone(zone)
    val date = rememberWalletFormatter(if (at.year == thisYear) R.string.wl_date_pattern else R.string.wl_date_year_pattern).format(at)
    val amount = amountText(entry.amount, signed = true)
    val kind = stringResource(kindLabel(entry.amount))
    val spoken = stringResource(R.string.wl_row_cd, title, date, "$kind ${amountText(kotlin.math.abs(entry.amount), signed = false)}")
    Column(
        Modifier.fillMaxWidth().feedbackClickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = spoken }
            .testTag("wl-row-${entry.id}"),
    ) {
        Row(Modifier.fillMaxWidth().heightIn(min = 80.dp).padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(signFace()), contentAlignment = Alignment.Center) {
                Text(if (entry.amount > 0) "+" else if (entry.amount < 0) "−" else "·", color = if (entry.amount > 0) p.accent else p.text,
                    fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em)
                Text(date, color = p.secondary, fontSize = 12.5.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(amount, color = if (entry.amount > 0) p.accent else p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.wl_unit), color = p.secondary, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp))
            }
        }
        Box(Modifier.padding(start = 50.dp).fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/** 빈 내역 — 첫 적립 전(07) · 거른 내역만 없음(08, 전체 내역 보기) */
@Composable
private fun EmptyHistory(filter: LedgerFilter, onShowAll: () -> Unit) {
    val (title, body) = when (filter) {
        LedgerFilter.ALL -> R.string.wl_empty_title to R.string.wl_empty_body
        LedgerFilter.EARNED -> R.string.wl_no_earned_title to R.string.wl_no_earned_body
        LedgerFilter.SPENT -> R.string.wl_no_spent_title to R.string.wl_no_spent_body
    }
    WalletState(
        icon = Icons.Outlined.ReceiptLong, title = stringResource(title), body = stringResource(body),
        action = if (filter == LedgerFilter.ALL) null else stringResource(R.string.wl_show_all), onAction = onShowAll,
        modifier = Modifier.testTag(if (filter == LedgerFilter.ALL) "wl-empty" else "wl-empty-filtered"), compact = true,
    )
}

/** 안내 한 덩어리 — 그림 · 제목 · 한 줄 · (있으면) 버튼 하나 */
@Composable
private fun WalletState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String?,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    compact: Boolean = false,
) {
    val p = settingsPalette()
    Column(
        modifier.fillMaxWidth().padding(top = if (compact) 34.dp else 140.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (compact) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(32.dp))
        } else {
            Box(Modifier.size(70.dp).clip(RoundedCornerShape(20.dp)).background(p.surface), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(30.dp))
            }
        }
        Text(title, color = p.text, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 18.dp))
        Text(body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 8.dp))
        if (action != null) {
            val buttonModifier = Modifier.padding(top = 22.dp).then(if (compact) Modifier.widthIn(min = 220.dp) else Modifier.fillMaxWidth())
                .testTag("wl-state-action")
            if (primary) SettingsPrimaryButton(action, onAction, buttonModifier) else SettingsSecondaryButton(action, onAction, buttonModifier)
        }
    }
}

@Composable
private fun StaleNote() {
    val p = settingsPalette()
    Text(stringResource(R.string.wl_stale), color = p.secondary, fontSize = 13.sp,
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clip(RoundedCornerShape(14.dp)).background(p.surface)
            .padding(14.dp).testTag("wl-stale"))
}

/** 읽는 중 — 줄 자리만 */
@Composable
private fun HistorySkeleton() {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 8.dp).testTag("wl-history-loading")) {
        repeat(3) {
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(11.dp)).background(p.skeleton))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(Modifier.width(140.dp).height(12.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
                    Box(Modifier.width(90.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(p.skeleton.copy(alpha = 0.7f)))
                }
                Box(Modifier.width(80.dp).height(16.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
            }
        }
    }
}

/** 내역 상세(05 · 06) — 그 원장 줄에 있는 것만 */
@Composable
private fun EntrySheet(entry: RewardEntity, zone: ZoneId, onClose: () -> Unit) {
    val p = settingsPalette()
    val full = rememberWalletFormatter(R.string.wl_date_year_pattern).format(Instant.ofEpochMilli(entry.timestamp).atZone(zone))
    SettingsSheet(
        title = stringResource(R.string.wl_detail_title), onDismiss = onClose, modifier = Modifier.testTag("wl-entry-sheet"),
        actions = { SettingsPrimaryButton(stringResource(R.string.wl_ok), onClose, Modifier.fillMaxWidth().testTag("wl-entry-ok")) },
    ) {
        Text(stringResource(ledgerLabel(entry.type)), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.semantics(mergeDescendants = true) {}.testTag("wl-entry-amount")) {
            Text(amountText(entry.amount, signed = true), color = if (entry.amount > 0) p.accent else p.text, fontSize = 52.sp,
                fontWeight = FontWeight.SemiBold, maxLines = 1, modifier = Modifier.alignByBaseline())
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.wl_unit), color = p.secondary, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.alignByBaseline())
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        DetailLine(stringResource(R.string.wl_detail_kind), stringResource(kindLabel(entry.amount)), bold = true)
        DetailLine(stringResource(R.string.wl_detail_time), full)
        if (entry.description.isNotBlank()) {
            Text(stringResource(R.string.wl_detail_note), color = p.secondary, fontSize = 15.sp)
            Text(entry.description, color = p.text, fontSize = 16.sp, lineHeight = 1.45.em, modifier = Modifier.testTag("wl-entry-note"))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String, bold: Boolean = false) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().heightIn(min = 36.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.secondary, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(value, color = p.text, fontSize = 16.sp, fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal)
    }
}

/** SUP 안내(13) — SUP 와 뽑기 기회를 가른다 */
@Composable
private fun InfoSheet(onClose: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.wl_info_title), onDismiss = onClose, modifier = Modifier.testTag("wl-info-sheet"),
        actions = { SettingsPrimaryButton(stringResource(R.string.wl_ok), onClose, Modifier.fillMaxWidth()) },
    ) {
        Text(stringResource(R.string.wl_info_head), color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.wl_info_body), color = p.secondary, fontSize = 15.sp)
        Box(Modifier.fillMaxWidth().padding(vertical = 6.dp).height(1.dp).background(p.divider))
        Text(stringResource(R.string.wl_info_draw_head), color = p.text, fontSize = 16.sp)
        Text(stringResource(R.string.wl_info_draw_body), color = p.secondary, fontSize = 15.sp)
    }
}

/**
 * 연결 상태(14) — 뽑기 기회는 신발에서. 자동으로 뽑지 않는다.
 * 기존 웹 지갑 페이지(SUP · 신발 옮기기, 2단계 인증)는 연결한 뒤에도 쓰므로 안쪽 글자 버튼으로 남긴다.
 */
@Composable
private fun LinkedSheet(onOpenDraw: () -> Unit, onOpenWalletPage: () -> Unit, onClose: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.wl_linked_title), onDismiss = onClose, modifier = Modifier.testTag("wl-linked-sheet"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.wl_open_draw), onOpenDraw, Modifier.fillMaxWidth().testTag("wl-open-draw"))
            SettingsSecondaryButton(stringResource(R.string.common_close), onClose, Modifier.fillMaxWidth())
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(42.dp).clip(CircleShape).background(p.surface), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = p.accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(14.dp))
            Text(stringResource(R.string.wl_linked_head), color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(stringResource(R.string.wl_linked_body), color = p.secondary, fontSize = 15.sp)
        Text(stringResource(R.string.wl_linked_gift), color = p.secondary, fontSize = 15.sp)
        Box(
            Modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onOpenWalletPage).testTag("wl-open-wallet-page"),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(stringResource(R.string.wallet_web_open), color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 연결 혜택(뽑기 전달본 06 재사용) — 숫자는 서버가 준 것만. 못 읽었으면 그 줄을 빼고 보인다 */
@Composable
private fun BenefitSheet(status: DrawStatus?, onConnect: () -> Unit, onClose: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.wl_benefit_title), onDismiss = onClose, modifier = Modifier.testTag("wl-benefit-sheet"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.wl_connect), onConnect, Modifier.fillMaxWidth().testTag("wl-connect"))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onClose),
                contentAlignment = Alignment.Center,
            ) {
                Text(stringResource(R.string.wl_later), color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        },
    ) {
        Text(stringResource(R.string.wl_benefit_sub), color = p.secondary, fontSize = 15.sp)
        if (status != null && status.giftOnLink > 0) {
            BenefitLine(stringResource(R.string.wl_benefit_first), stringResource(R.string.wl_benefit_first_value, status.giftOnLink))
        }
        if (status != null && status.runStepMeters > 0) {
            BenefitLine(stringResource(R.string.wl_benefit_run, DrawDistance.stepKm(status.runStepMeters)), stringResource(R.string.wl_benefit_run_value))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
        Text(stringResource(R.string.wl_benefit_note), color = p.secondary, fontSize = 15.sp, lineHeight = 1.5.em)
    }
}

@Composable
private fun BenefitLine(label: String, value: String) {
    val p = settingsPalette()
    Row(Modifier.fillMaxWidth().heightIn(min = 40.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = p.secondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Text(value, color = p.accent, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * 금액 글자 — 기존 정밀도(원장 소수 4자리 → 두 자리, 0 쪽으로). [signed] 면 +/−, 아니면 음수에만 −(잔액이 음수여도 숨기지 않는다).
 * 0 은 부호 없이("-0.00" 없음).
 */
internal fun amountText(value: Double, signed: Boolean): String {
    val magnitude = formatSupDown(kotlin.math.abs(value), 2)
    return when {
        magnitude == formatSupDown(0.0, 2) -> magnitude
        value < 0 -> "−$magnitude"
        signed -> "+$magnitude"
        else -> magnitude
    }
}

private fun kindLabel(amount: Double): Int = when {
    amount > 0 -> R.string.wl_kind_earned
    amount < 0 -> R.string.wl_kind_spent
    else -> R.string.wl_kind_other
}

/** 원장 종류 이름 — 기존 종류 그대로, 모르는 종류는 "기타 내역"(내용은 상세에서) */
private fun ledgerLabel(type: String): Int = when (type) {
    RewardType.EARN_WALK -> R.string.ledger_earn_walk
    RewardType.BONUS_GOAL -> R.string.ledger_bonus_goal
    RewardType.SPEND_UPGRADE -> R.string.ledger_spend_upgrade
    RewardType.SPEND_MINT -> R.string.ledger_spend_mint
    RewardType.SPEND_BOOST -> R.string.ledger_spend_boost
    RewardType.EARN_EVENT -> R.string.ledger_earn_event
    RewardType.EARN_PARTY -> R.string.ledger_earn_party
    RewardType.TRADE_BUY -> R.string.ledger_trade_buy
    RewardType.TRADE_SELL -> R.string.ledger_trade_sell
    RewardType.TRADE_FEE -> R.string.ledger_trade_fee
    RewardType.ESCROW_LOCK -> R.string.ledger_escrow_lock
    RewardType.ESCROW_UNLOCK -> R.string.ledger_escrow_unlock
    // 서버 경제(0022 · 0025)의 줄
    "SPEND_DRAW" -> R.string.ledger_spend_draw
    "SPEND_REPAIR" -> R.string.ledger_spend_repair
    "EARN_COURSE" -> R.string.ledger_earn_course
    "EARN_INVITE" -> R.string.ledger_earn_invite
    "CHAIN_WITHDRAW", "CHAIN_REFUND", "CHAIN_DEPOSIT" -> R.string.ledger_chain
    com.stepup.android.data.repo.EconomySync.CARRIED_OVER -> R.string.ledger_carried_over
    else -> R.string.ledger_other
}

@Composable
private fun rememberWalletFormatter(pattern: Int): DateTimeFormatter {
    val text = stringResource(pattern)
    val locale = LocalConfiguration.current.locales[0]
    return remember(text, locale) { DateTimeFormatter.ofPattern(text, locale) }
}

@Composable
private fun chipOn(): Color = if (StepUpColors.dark) Color(0xFFF2F4FC) else settingsPalette().accent

@Composable
private fun chipOnText(): Color = if (StepUpColors.dark) Color(0xFF081223) else Color.White

@Composable
private fun chipOff(): Color = if (StepUpColors.dark) Color(0xFF152235) else settingsPalette().surface

@Composable
private fun signFace(): Color = if (StepUpColors.dark) Color(0xFF13223A) else settingsPalette().surface
