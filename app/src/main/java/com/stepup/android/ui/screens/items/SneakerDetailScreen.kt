package com.stepup.android.ui.screens.items

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunNotice
import com.stepup.android.ui.components.RunSheet
import com.stepup.android.ui.components.RunSheetText
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.ShoeArtFull
import com.stepup.android.ui.components.ShoeArtLoad
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.ShoeStage
import com.stepup.android.ui.components.ShoeStageMessageAlignment
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.forgetShoeArt
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.rememberShoeArt
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** 상세 위에 뜨는 시트 — 신발 관리(SD13 · SD14) · 신발 기록(SD15 ~ SD17). 수리 시트는 따로(한 번에 하나) */
private enum class DetailSheet { None, Manage, Record }

/** 상세가 보이는 수리 · 강화의 결과 미확정(SD18 · SD19) — 이때는 착용 · 강화 · 수리 · 판매를 막는다 */
data class ShoeCareStatus(val upgradePending: Boolean = false, val repairPending: Boolean = false) {
    val busy: Boolean get() = upgradePending || repairPending
}

/** 휴대폰에 쓸 수 있는 연결이 있는가 — 없으면 강화 요청을 보내지 않는다(UP14). 있다고 서버에 닿는다는 보장은 아니다 */
private fun hasNetwork(context: android.content.Context): Boolean {
    val manager = context.getSystemService(android.net.ConnectivityManager::class.java) ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

/**
 * 신발 상세(파란 톤 v4 — docs/redesign/blue-v4-2026-10, 상세 · 수리 30 · 강화 17).
 *
 * 본문: 등급 무대 → 이름 · 등급 배지 · No. 번호 → 레벨 · 효율 · 착화감 · 내구도 네 독립 줄 → 아래 "강화하기"(파란 면) + "이 신발 신기"(흰 면).
 * 강화하기는 같은 경로 안의 독립 강화 화면([ShoeUpgradeContent])을, ⋯ 관리는 수리하기 · 판매하기 · 신발 기록을 연다.
 * 수리는 관리에서 여는 하단 시트([ShoeRepairSheet])다. 착용은 기존 길 그대로([ItemsViewModel.equip]).
 * 수리 · 강화는 지금 서버 계약 그대로 [ShoeCareStore] 가 보낸다 — 결과를 모르면 결과 확인은 읽기만 한다.
 */
@Composable
fun SneakerDetailScreen(
    sneakerId: Long,
    onBack: () -> Unit = {},
    /** 이 신발을 들고 NFT 마켓플레이스의 판매 등록으로 — 등록은 거기서 확정한다 */
    onSell: (faction: String, rarity: String, variant: Int, localId: Long) -> Unit =
        { _, _, _, _ -> },
    /** SD09 · SD10 "보관함으로" — 앱 셸에서는 신발 탭의 최신 목록으로 */
    onOpenOwned: () -> Unit = onBack,
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val context = LocalContext.current
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    val equipping by viewModel.equipping.collectAsStateWithLifecycle()
    val result by viewModel.equipResult.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val store = ShoeCareStore.shared
    val repairs by store.repair.collectAsStateWithLifecycle()
    val upgrades by store.upgrade.collectAsStateWithLifecycle()
    val state = detailStateOf(owned, sneakerId)
    val scope = rememberCoroutineScope()

    var upgradeOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }
    var repairOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }

    // 로그인 화면으로 — 돌아온 뒤 자동으로 착용 · 수리 · 강화하지 않는다. 서버 설정이 없는 빌드는 로그인할 수 없다(버튼 없음)
    val signIn: (() -> Unit)? = if (!ServiceLocator.serverEconomyOn) null else ({
        scope.launch {
            try {
                com.stepup.android.ui.components.returnToSignIn(context)
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Toast.makeText(context, R.string.feed_save_failed, Toast.LENGTH_SHORT).show()
            }
        }
    })

    // 다른 길(보관함 등)의 짧은 알림 — 착용 · 수리 · 강화의 결말은 화면 안에서 말한다
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.SaveFailed -> context.getString(R.string.feed_save_failed)
            ItemsMessage.SignInRequired -> context.getString(R.string.toast_sign_in_required)
            ItemsMessage.Offline -> context.getString(R.string.toast_offline)
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    val upgradePhase = upgrades[sneakerId]
    val repairPhase = repairs[sneakerId]
    // 화면이 다시 만들어졌는데(프로세스 재시작) 열려 있던 수리 시트의 상태가 없으면 — 조회부터 다시(읽기만)
    LaunchedEffect(repairOpen) {
        if (repairOpen && store.repair.value[sneakerId] == null) store.openRepair(sneakerId)
    }

    if (upgradeOpen) {
        val leave = {
            upgradeOpen = false
            store.clearUpgrade(sneakerId)
        }
        BackHandler { leave() }
        ShoeUpgradeContent(
            state = state,
            balance = balance,
            phase = upgradePhase,
            onBack = leave,
            onOpenOwned = {
                leave()
                onOpenOwned()
            },
            onStart = store::startUpgrade,
            onRecheck = { store.recheckUpgrade(sneakerId) },
            onReset = { store.clearUpgrade(sneakerId) },
            onSignIn = signIn,
            isOnline = { !ServiceLocator.serverEconomyOn || hasNetwork(context) },
        )
        return
    }

    ShoeDetailContent(
        state = state,
        equipping = equipping,
        result = result,
        onBack = onBack,
        onOpenOwned = onOpenOwned,
        onReload = viewModel::reloadOwned,
        onWear = { if (!store.busy(sneakerId)) viewModel.equip(sneakerId) },
        onResultShown = viewModel::consumeEquipResult,
        onEnhance = { upgradeOpen = true },
        onSell = { shoe -> onSell(shoe.faction.id, shoe.rarity.id, shoe.variant, shoe.id) },
        care = ShoeCareStatus(upgradePending = upgradePhase?.pending == true, repairPending = repairPhase?.pending == true),
        repair = if (repairOpen) repairPhase ?: RepairPhase.Loading else null,
        onOpenRepair = {
            repairOpen = true
            store.openRepair(sneakerId)
        },
        onRepairConfirm = { store.confirmRepair(sneakerId) },
        onRepairRecheck = { store.recheckRepair(sneakerId) },
        onRepairReload = { store.reloadRepair(sneakerId) },
        onRepairClose = {
            repairOpen = false
            store.closeRepair(sneakerId)
        },
        onSignIn = signIn,
        onCheckWear = viewModel::reloadOwned,
    )
}

/**
 * 상세 화면 — 상태를 받아 그리기만 한다(테스트가 시안 장면을 바로 넣어 찍는다). [initialSheet] 는 None · Manage · Record.
 * [repair] 가 있으면 수리 시트를 그 장면으로 띄운다.
 */
@Composable
fun ShoeDetailContent(
    state: ShoeDetailState,
    equipping: Boolean = false,
    result: EquipResult? = null,
    onBack: () -> Unit = {},
    onOpenOwned: () -> Unit = {},
    onReload: () -> Unit = {},
    onWear: () -> Unit = {},
    onResultShown: () -> Unit = {},
    onEnhance: () -> Unit = {},
    onSell: (Sneaker) -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    initialSheet: String? = null,
    care: ShoeCareStatus = ShoeCareStatus(),
    repair: RepairPhase? = null,
    onOpenRepair: () -> Unit = {},
    onRepairConfirm: () -> Unit = {},
    onRepairRecheck: () -> Unit = {},
    onRepairReload: () -> Unit = {},
    onRepairClose: () -> Unit = {},
    onSignIn: (() -> Unit)? = null,
    onCheckWear: () -> Unit = {},
) {
    var sheet by rememberSaveable { mutableStateOf(initialSheet?.let(DetailSheet::valueOf) ?: DetailSheet.None) }
    var artAttempt by rememberSaveable { mutableIntStateOf(0) }
    val ready = state as? ShoeDetailState.Ready
    val shoe = ready?.shoe
    val wearing = ready?.wearing
    val korean = LocalConfiguration.current.locales[0].language == "ko"

    // SD04 — 저장된 착용이 이 켤레로 바뀐 뒤 한 번만(약 2초). 결말은 곧바로 지워 다시 들어와도 되풀이하지 않는다
    var toast by remember { mutableStateOf<String?>(null) }
    val wornMessage = shoe?.let { stringResource(R.string.sdv_worn_toast, it.shoeName().withParticle(korean)) }
    LaunchedEffect(result, shoe?.id, wearing?.id) {
        val current = result ?: return@LaunchedEffect
        if (shoe == null || current.targetId != shoe.id) return@LaunchedEffect
        // 미확인 시트를 띄운 뒤 늦게라도 저장된 착용이 이 켤레가 되면(착용 상태 확인 · 연결 복구) 완료로 바꾼다
        if (current is EquipResult.Worn || wearing?.id == shoe.id) {
            toast = wornMessage
            onResultShown()
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(2_000)
            toast = null
        }
    }

    val upgradeLabel = stringResource(if (care.upgradePending) R.string.care_upgrade_checking else R.string.care_upgrade_open)
    val checkingBar: @Composable ColumnScope.() -> Unit = {
        DetailButtons(upgradeLabel, upgradeEnabled = false, onUpgrade = {},
            wearLabel = stringResource(R.string.sdv_checking), wearEnabled = false, wearBusy = true, onWear = {})
    }
    val wearBar: @Composable ColumnScope.() -> Unit = {
        val worn = shoe?.equipped == true
        WornToast(toast, Modifier.fillMaxWidth().padding(bottom = 10.dp))
        if (toast == null && !worn && wearing == null && !equipping) {
            Text(stringResource(R.string.care_first_hint), style = runTextStyle(14.sp, runTone().label, androidx.compose.ui.text.font.FontWeight.Medium),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).testTag("shoe-first-hint"))
        }
        DetailButtons(
            upgradeLabel = upgradeLabel,
            upgradeEnabled = !equipping && !care.busy,
            onUpgrade = onEnhance,
            wearLabel = when {
                equipping -> stringResource(R.string.sdv_wear_busy)
                worn -> stringResource(R.string.sdv_status_wearing)
                else -> stringResource(R.string.sdv_wear)
            },
            wearEnabled = !equipping && !worn && !care.busy,
            wearBusy = equipping,
            onWear = onWear,
        )
    }

    DetailPage(
        title = stringResource(R.string.sdv_title),
        onBack = onBack,
        trailing = {
            if (ready != null) {
                DarkIconButton(Icons.Filled.MoreHoriz, stringResource(R.string.sdv_more), onClick = { sheet = DetailSheet.Manage },
                    modifier = Modifier.testTag("shoe-more"))
            } else {
                Spacer(Modifier.size(StepUpDesign.TouchTarget))
            }
        },
        bottomBar = when {
            state == ShoeDetailState.Loading -> checkingBar
            shoe != null -> wearBar
            else -> null
        },
    ) {
        when (state) {
            ShoeDetailState.Loading -> item(key = "loading") { DetailLoading() }
            ShoeDetailState.Failed -> item(key = "failed") {
                CareStatePanel(
                    icon = Icons.Filled.Refresh,
                    title = stringResource(R.string.sdv_load_failed_title),
                    body = stringResource(R.string.sdv_load_failed_body),
                    primary = stringResource(R.string.sdv_reload) to onReload,
                    secondary = stringResource(R.string.sdv_back_to_owned) to onOpenOwned,
                    modifier = Modifier.testTag("shoe-detail-failed"),
                )
            }
            ShoeDetailState.NotFound -> item(key = "missing") {
                CareStatePanel(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.sdv_missing_title),
                    body = stringResource(R.string.sdv_missing_body),
                    primary = stringResource(R.string.sdv_back_to_owned) to onOpenOwned,
                    modifier = Modifier.testTag("shoe-detail-missing"),
                )
            }
            is ShoeDetailState.Ready -> item(key = "shoe-${state.shoe.id}") {
                DetailBody(
                    state = state,
                    care = care,
                    artAttempt = artAttempt,
                    onRetryArt = {
                        forgetShoeArt(state.shoe)
                        artAttempt += 1
                    },
                    onShowUpgrade = onEnhance,
                    onShowRepair = onOpenRepair,
                )
            }
        }
    }

    if (shoe != null) {
        when (sheet) {
            DetailSheet.Record -> RecordSheet(shoe, zone, onClose = { sheet = DetailSheet.None })
            DetailSheet.Manage -> ManageSheet(
                shoe = shoe,
                busy = care.busy,
                onClose = { sheet = DetailSheet.None },
                onRepair = {
                    // 한 번에 시트는 하나 — 관리를 닫고 수리 시트를 연다
                    sheet = DetailSheet.None
                    onOpenRepair()
                },
                onSell = {
                    sheet = DetailSheet.None
                    onSell(shoe)
                },
                onOpenRecord = { sheet = DetailSheet.Record },
            )
            DetailSheet.None -> Unit
        }
        if (repair != null && sheet == DetailSheet.None) {
            ShoeRepairSheet(
                shoe = shoe, phase = repair,
                onConfirm = onRepairConfirm, onRecheck = onRepairRecheck, onReload = onRepairReload,
                onSignIn = onSignIn, onClose = onRepairClose,
            )
        }
        // SD05 · SD06 · SD07 — 바뀌지 않았거나 확인하지 못했다
        val failure = result as? EquipResult.NotWorn
        if (failure != null && failure.targetId == shoe.id && wearing?.id != shoe.id && repair == null) {
            EquipErrorSheet(
                failure = failure,
                wearing = wearing,
                korean = korean,
                onClose = onResultShown,
                onRetry = {
                    onResultShown()
                    onWear()
                },
                onSignIn = onSignIn,
                onCheck = onCheckWear,
                onOpenOwned = {
                    onResultShown()
                    onOpenOwned()
                },
            )
        }
    }
}

// ── 본문 ──────────────────────────────────────────────────────

@Composable
private fun DetailBody(
    state: ShoeDetailState.Ready,
    care: ShoeCareStatus,
    artAttempt: Int,
    onRetryArt: () -> Unit,
    onShowUpgrade: () -> Unit,
    onShowRepair: () -> Unit,
) {
    val t = runTone()
    val shoe = state.shoe
    val art by rememberShoeArt(shoe, ShoeArtFull, artAttempt)
    val failed = art == ShoeArtLoad.Failed
    // 작은 화면은 무대부터 줄인다 — 이름 · 네 줄 · 버튼이 한 화면에 들도록. 그래도 모자라면 목록이 넘어간다
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val stageMaxHeight = (screenHeight - 500.dp).coerceAtLeast(190.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 무대 — 기존 등급 무대(무대 면 → 뒤 효과 → 신발 → 앞 효과 → 프레임)
        Box(
            Modifier.widthIn(max = stageMaxHeight * GradeArtRatio).fillMaxWidth().aspectRatio(GradeArtRatio)
                .then(if (art is ShoeArtLoad.Ready) Modifier.testTag("shoe-art") else Modifier),
        ) {
            SneakerGradeStage(shoe, Modifier.fillMaxSize(), showShoe = !failed)
            if (failed) {
                Box(Modifier.fillMaxSize().testTag("shoe-art-failed")) {
                    ArtFailed(onRetryArt, Modifier.align(Alignment.Center))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // 이름 → 등급 배지 → No. 번호 한 줄. 이름이 길면 두 줄까지, 배지와 번호는 함께 남는다. 착용 여부로 배지를 바꾸지 않는다
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = androidx.compose.ui.text.TextStyle(
                color = t.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 1.24.em, letterSpacing = (-0.02).em,
            ),
            maxLines = 2,
            suffix = formatShoeNumber(shoe.mintNumber),
            suffixStyle = androidx.compose.ui.text.TextStyle(
                color = t.label, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum",
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics { heading() }.testTag("shoe-name"),
        )
        Spacer(Modifier.height(12.dp))
        // SD18 · SD19 — 결과가 안 정해진 강화 · 수리. 아래 값은 마지막으로 확인한 값이다
        if (care.upgradePending) {
            RunNotice(
                stringResource(R.string.care_pending_upgrade_title), body = stringResource(R.string.care_pending_upgrade_body),
                action = stringResource(R.string.care_check_result), onAction = onShowUpgrade, tag = "shoe-upgrade-pending",
            )
            Spacer(Modifier.height(8.dp))
        }
        if (care.repairPending) {
            RunNotice(
                stringResource(R.string.care_pending_repair_title), body = stringResource(R.string.care_pending_repair_body),
                action = stringResource(R.string.care_check_result), onAction = onShowRepair, tag = "shoe-repair-pending",
            )
            Spacer(Modifier.height(8.dp))
        }
        ShoeStatCells(shoe, Modifier.testTag("shoe-stat-cells"))
    }
}

/**
 * 아래 두 버튼 — 작은 파란 "강화하기"와 넓은 흰 "이 신발 신기"(지금 신고 있어요 · 신발 바꾸는 중… · 확인 중…).
 * 큰 글씨 · 좁은 화면에서는 위아래로 놓는다(글자를 줄이지 않는다).
 */
@Composable
private fun DetailButtons(
    upgradeLabel: String,
    upgradeEnabled: Boolean,
    onUpgrade: () -> Unit,
    wearLabel: String,
    wearEnabled: Boolean,
    wearBusy: Boolean,
    onWear: () -> Unit,
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val stacked = maxWidth < 340.dp || fontScale > 1.15f
        val upgrade = @Composable { m: Modifier ->
            CareBlueButton(upgradeLabel, onUpgrade, m.testTag("shoe-upgrade-open"), enabled = upgradeEnabled)
        }
        val wear = @Composable { m: Modifier ->
            RunButton(wearLabel, onWear, m.testTag("detail-primary-action"), enabled = wearEnabled, busy = wearBusy)
        }
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                wear(Modifier.fillMaxWidth())
                upgrade(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                upgrade(Modifier.weight(0.36f))
                wear(Modifier.weight(0.64f))
            }
        }
    }
}

/** SD04 — "✓ 이 신발을 신었어요." 버튼 위에 잠깐. 한 번만 읽힌다 */
@Composable
private fun WornToast(message: String?, modifier: Modifier) {
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = message != null, modifier = modifier,
        enter = fadeIn(tween(motion.duration(160))),
        exit = fadeOut(tween(motion.duration(160))),
    ) {
        Box(Modifier.semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag("shoe-worn-toast")) {
            CareStrip(message.orEmpty(), icon = Icons.Filled.Check)
        }
    }
}

/** SD11 — 그림만 읽지 못했다. 이름 · 능력치 · 신기는 그대로, 그림만 다시 */
@Composable
private fun ArtFailed(onRetry: () -> Unit, modifier: Modifier) {
    val t = runTone()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = t.label, modifier = Modifier.size(44.dp))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sdv_art_failed), style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.heightIn(min = StepUpDesign.TouchTarget).clip(RoundedCornerShape(10.dp)).background(t.cobalt)
                .feedbackClickable(onClick = onRetry)
                .padding(horizontal = 16.dp).testTag("shoe-art-retry"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.sdv_art_retry), style = runTextStyle(14.sp, androidx.compose.ui.graphics.Color.White, FontWeight.Bold))
        }
    }
}

/** SD08 — 임시 이름 · 능력치 · 착용 상태를 보이지 않고 자리만(무대 · 이름 줄 · 네 줄의 이름과 "—") */
@Composable
private fun DetailLoading() {
    val t = runTone()
    val p = settingsPalette()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val stageMaxHeight = (screenHeight - 500.dp).coerceAtLeast(190.dp)
    Column(Modifier.fillMaxWidth().testTag("shoe-detail-loading"), horizontalAlignment = Alignment.CenterHorizontally) {
        ShoeStage(
            sneaker = null,
            modifier = Modifier.widthIn(max = stageMaxHeight * GradeArtRatio).fillMaxWidth().zIndex(-1f),
            overlay = {
                Text(stringResource(R.string.sdv_loading), style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                    modifier = Modifier.align(ShoeStageMessageAlignment).semantics { liveRegion = LiveRegionMode.Polite })
            },
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.align(Alignment.Start).size(width = 196.dp, height = 26.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
        Spacer(Modifier.height(16.dp))
        ShoeStatCellsPlaceholder(Modifier.testTag("shoe-stat-cells-loading"))
    }
}

// ── 시트 ──────────────────────────────────────────────────────

/**
 * SD15 ~ SD17 신발 기록 — 읽기 전용. 신발 번호(민팅 번호) · 등급 · 레벨 · 획득일 · 체인 등록(서버가 준 토큰 번호만 —
 * 등록 없음 · Token · Vault). 민팅 번호와 체인 번호를 합치지 않는다. 예전 52종이면 속성 줄을 더 둔다.
 */
@Composable
private fun RecordSheet(shoe: Sneaker, zone: ZoneId, onClose: () -> Unit) {
    val t = runTone()
    val pattern = stringResource(R.string.sdv_date_pattern)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    val received = formatter.format(Instant.ofEpochMilli(shoe.acquiredAt).atZone(zone))
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("shoe-info-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CareSheetTitle(stringResource(R.string.care_record), null)
            Spacer(Modifier.height(2.dp))
            CareShoeHeader(shoe, stageWidth = 96.dp, showLevel = false)
            Spacer(Modifier.height(4.dp))
            CareInfoRow(stringResource(R.string.sdv_info_number), formatShoeNumber(shoe.mintNumber), "shoe-info-number")
            CareInfoRow(stringResource(R.string.sdv_info_rarity), shoe.tier.label(), "shoe-info-rarity") {
                ShoeGradeBadge(shoe.tier, decorative = true)
            }
            CareInfoRow(stringResource(R.string.sdv_info_level), "${shoe.level} / ${shoe.maxLevel}", "shoe-info-level")
            // 속성은 예전 52종에만 있다 — 새 도감 신발에 자리 값을 보이지 않는다
            if (shoe.modelId == null) CareInfoRow(stringResource(R.string.sdv_info_faction), shoe.faction.label(), "shoe-info-faction")
            CareInfoRow(stringResource(R.string.care_record_received), received, "shoe-info-received")
            val chain = chainMarkOf(shoe.tokenId)
            CareInfoRow(
                stringResource(R.string.care_record_chain),
                when (chain) {
                    ChainMark.None -> stringResource(R.string.care_chain_none)
                    is ChainMark.Token -> stringResource(R.string.care_chain_token, chain.tokenId)
                    is ChainMark.Vault -> stringResource(R.string.care_chain_vault, chain.tokenId)
                },
                "shoe-info-chain",
            )
            if (chain == ChainMark.None) {
                Text(stringResource(R.string.sdv_chain_none_note), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.45f))
            }
            Spacer(Modifier.height(6.dp))
            RunButton(stringResource(R.string.common_close), onClose, Modifier.testTag("shoe-info-ok"))
        }
    }
}

/**
 * SD13 · SD14 신발 관리 — 수리하기 · 판매하기 · 신발 기록. 착용 중이면 판매만 까닭과 함께 막는다(자동으로 벗기지 않는다).
 * 강화는 본문 아래 직접 버튼이 있어 여기 두지 않는다. 결과가 안 정해진 수리 · 강화가 있으면 수리 · 판매를 막는다.
 */
@Composable
private fun ManageSheet(
    shoe: Sneaker,
    busy: Boolean,
    onClose: () -> Unit,
    onRepair: () -> Unit,
    onSell: () -> Unit,
    onOpenRecord: () -> Unit,
) {
    val t = runTone()
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("shoe-manage-sheet")) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CareSheetTitle(stringResource(R.string.care_manage_title), shoe.nameAndNumber())
            Spacer(Modifier.height(2.dp))
            CareMenuRow(
                title = stringResource(R.string.care_repair),
                description = if (busy) stringResource(R.string.care_busy_note)
                else stringResource(R.string.care_durability_now, formatDurability(shoe)),
                enabled = !busy,
                onClick = onRepair,
                tag = "shoe-manage-repair",
            )
            CareMenuRow(
                title = stringResource(R.string.care_sell),
                description = when {
                    shoe.equipped -> stringResource(R.string.care_sell_equipped)
                    busy -> stringResource(R.string.care_busy_note)
                    else -> null
                },
                enabled = !shoe.equipped && !busy,
                onClick = onSell,
                tag = "shoe-manage-sell",
            )
            CareMenuRow(stringResource(R.string.care_record), null, enabled = true, onClick = onOpenRecord, tag = "shoe-row-info")
            // 이 NFT 정보(이전 상세의 설명) — 네 속성 이야기라 예전 52종에만
            if (shoe.modelId == null) {
                Text(stringResource(R.string.sneaker_about_body), style = runTextStyle(13.sp, t.label, FontWeight.Medium, 1.45f),
                    modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * SD05 거절(기존 착용을 말한다) · SD06 로그인 필요 · SD07 결과 미확인(착용 상태 확인은 읽기만) · 없는 신발.
 * 결과를 모르면 "다시 신기"를 보내지 않고 먼저 실제 착용을 다시 읽는다.
 */
@Composable
private fun EquipErrorSheet(
    failure: EquipResult.NotWorn,
    wearing: Sneaker?,
    korean: Boolean,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSignIn: (() -> Unit)?,
    onCheck: () -> Unit,
    onOpenOwned: () -> Unit,
) {
    val title: String
    val body: String
    val note: String?
    val leftLabel: String
    val right: Pair<String, () -> Unit>?
    when {
        failure.reason == EquipFailure.SIGN_IN -> {
            title = stringResource(R.string.care_sign_in_title)
            body = stringResource(R.string.care_sign_in_wear)
            note = null
            leftLabel = stringResource(R.string.common_close)
            right = onSignIn?.let { stringResource(R.string.care_sign_in) to it }
        }
        failure.reason == EquipFailure.MISSING -> {
            title = stringResource(R.string.sdv_error_title)
            body = stringResource(R.string.sdv_missing_body)
            note = null
            leftLabel = stringResource(R.string.common_close)
            right = stringResource(R.string.sdv_back_to_owned) to onOpenOwned
        }
        !failure.confirmed -> {
            title = stringResource(R.string.care_wear_check_title)
            body = stringResource(R.string.care_wear_check_body)
            note = stringResource(R.string.care_wear_check_note)
            leftLabel = stringResource(R.string.care_later)
            right = stringResource(R.string.care_wear_check) to onCheck
        }
        else -> {
            title = stringResource(R.string.sdv_error_title)
            body = if (wearing != null) stringResource(R.string.sdv_error_kept, wearing.shoeName().withParticle(korean))
            else stringResource(R.string.sdv_error_none)
            note = stringResource(R.string.care_wear_failed_hint)
            leftLabel = stringResource(R.string.common_close)
            right = stringResource(R.string.sdv_retry_wear) to onRetry
        }
    }
    RunSheet(onDismiss = onClose, modifier = Modifier.testTag("shoe-equip-error")) {
        RunSheetText(title)
        Spacer(Modifier.height(10.dp))
        Text(body, style = runTextStyle(16.sp, runTone().text, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("shoe-error-kept"))
        if (note != null) {
            Spacer(Modifier.height(4.dp))
            Text(note, style = runTextStyle(14.sp, runTone().label, FontWeight.Medium, 1.45f), textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CareBlueButton(leftLabel, onClose, Modifier.weight(1f).testTag("shoe-error-close"))
            if (right != null) {
                RunButton(right.first, right.second, Modifier.weight(1f).testTag("shoe-error-retry"))
            }
        }
    }
}

// ── 공용 ──────────────────────────────────────────────────────

/**
 * 신발 탭의 조회 실패 · 빈 목록(신발 탭 전달본) — 아이콘 칸 · 제목 · 한 줄 · 화면 폭 버튼(주 · 보조).
 * 상세는 [CareStatePanel] 을 쓴다. 이것은 신발 탭(CustomizeScreen)이 그대로 쓴다.
 */
@Composable
internal fun ShoeStatePanel(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>?,
    modifier: Modifier = Modifier,
    secondary: Pair<String, () -> Unit>? = null,
    top: androidx.compose.ui.unit.Dp = 96.dp,
) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth().padding(top = top, bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(70.dp).clip(RoundedCornerShape(20.dp)).background(p.surface), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(30.dp))
        }
        Text(title, color = p.text, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            lineHeight = 1.3.em, modifier = Modifier.padding(top = 20.dp).semantics { heading() })
        Text(body, color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 10.dp))
        if (primary != null) {
            SettingsPrimaryButton(primary.first, primary.second,
                Modifier.padding(top = 28.dp).fillMaxWidth().heightIn(min = 54.dp).testTag("shoe-state-primary"))
        }
        if (secondary != null) {
            SettingsSecondaryButton(secondary.first, secondary.second,
                Modifier.padding(top = 12.dp).fillMaxWidth().heightIn(min = 54.dp).testTag("shoe-state-secondary"))
        }
    }
}

/**
 * 화면에 쓰는 신발 이름 — 기존 현지화 이름([variantLabel]). 이 앱이 모르는 새 도감 번호면 다른 신발 이름 대신 "신발 모델 1401".
 */
@Composable
internal fun Sneaker.shoeName(): String {
    val model = modelId
    return if (model != null && shoeModelNameRes(model) == null) stringResource(R.string.sdv_unknown_model, model) else variantLabel()
}

/** 한국어 화면이면 목적격 조사를 붙인다("새벽 강변을") — 다른 언어 문장은 조사가 없다 */
internal fun String.withParticle(korean: Boolean): String = if (korean) withObjectParticle(this) else this
