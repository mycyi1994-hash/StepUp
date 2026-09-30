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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
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
import com.stepup.android.domain.Sneaker
import com.stepup.android.domain.tier
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GhostButton
import com.stepup.android.ui.components.SettingsGroupLabel
import com.stepup.android.ui.components.SettingsNote
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.ShoeGradeBadge
import com.stepup.android.ui.components.ShoeNameWithBadge
import com.stepup.android.ui.components.ShoeStage
import com.stepup.android.ui.components.GradeArtRatio
import com.stepup.android.ui.components.ShoeArtFull
import com.stepup.android.ui.components.ShoeArtLoad
import com.stepup.android.ui.components.SneakerGradeStage
import com.stepup.android.ui.components.rememberShoeArt
import com.stepup.android.ui.components.ShoeStageMessageAlignment
import com.stepup.android.ui.components.SneakerFrame
import com.stepup.android.ui.components.VoltButton
import com.stepup.android.ui.components.forgetShoeArt
import com.stepup.android.ui.components.label
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.components.variantLabel
import com.stepup.android.ui.experience.LocalMotion
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

/** 상세 위에 뜨는 시트 — 능력치(04) · 능력치 설명(05, 같은 시트 안) · 신발 정보(06) · 이 신발 관리(⋯) */
private enum class DetailSheet { None, Stats, Explain, Info, Manage }

/**
 * 신발 상세(보유 신발 상세 v1, 2026-09-28 전달본 — docs/redesign/shoe-detail-v1, 시안 02 ~ 17).
 *
 * 본문(2026-09-30 카툰 입체형 확정안 — docs/redesign/shoe-detail-cartoon-2026-09-30): 등급 무대 → 이름 · 등급 배지 · No. 번호 한 줄 →
 * 레벨 · 효율 · 착화감 · 내구도 네 칸(스틸 블루, 카툰 입체 막대) → "이 신발 신기" 하나. 설명 문구는 두지 않는다.
 * 능력치 자세히 · 신발 정보는 지우지 않고 ⋯(이 신발 관리) 안으로 옮겼다. 신고 있으면 버튼은
 * "지금 신고 있어요"(누를 수 없음) — 예전처럼 강화 버튼으로 바뀌지 않는다. 대상은 늘 소유 id 한 켤레다(같은 모델 묶음이 아니다).
 *
 * 착용은 기존 길 그대로([ItemsViewModel.equip] → 저장소 equipOnServer). 누르면 "신발 바꾸는 중…"(09)으로 막고, 화면이 보는
 * 목록에서 실제 착용이 바뀐 뒤에만 "○○을 신었어요"(10). 바뀌지 않으면 11 — 저장된 지금 착용을 보여 주고 다시 신기 · 닫기.
 * 조회 중(13) · 조회 실패(14) · 없는 신발(15) · 이미지만 실패(16) · 아직 착용 없음(17)을 가른다.
 *
 * 이전 상세의 강화 · 수리 · 판매는 지우지 않고 위쪽 ⋯(이 신발 관리)로 옮겼다 — 강화 확인 창 · 수리 · 거래소 판매 등록은 그대로다.
 */
@Composable
fun SneakerDetailScreen(
    sneakerId: Long,
    onBack: () -> Unit = {},
    /** 이 신발을 들고 NFT 마켓플레이스의 판매 등록으로 — 등록은 거기서 확정한다 */
    onSell: (faction: String, rarity: String, variant: Int, localId: Long) -> Unit =
        { _, _, _, _ -> },
    /** 14 · 15 "보유 신발로 돌아가기" — 앱 셸에서는 신발 탭의 최신 목록으로 */
    onOpenOwned: () -> Unit = onBack,
    viewModel: ItemsViewModel = viewModel(factory = ItemsViewModel.Factory),
) {
    val context = LocalContext.current
    val owned by viewModel.owned.collectAsStateWithLifecycle()
    val equipping by viewModel.equipping.collectAsStateWithLifecycle()
    val result by viewModel.equipResult.collectAsStateWithLifecycle()
    val balance by viewModel.balance.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val state = detailStateOf(owned, sneakerId)

    // Opening the confirmation never spends SUP; the confirmed repository write is atomic.
    var enhanceOpen by rememberSaveable(sneakerId) { mutableStateOf(false) }

    val msgNoBalance = stringResource(R.string.toast_no_balance)
    val msgMaxLevel = stringResource(R.string.toast_max_level)
    val msgUpgraded = stringResource(R.string.toast_upgraded)

    // 강화 · 수리의 결과(옮기기 전과 같은 짧은 알림). 착용 결과는 화면 안(10 · 11)에서 말한다
    LaunchedEffect(message) {
        val m = message ?: return@LaunchedEffect
        val text = when (m) {
            ItemsMessage.SaveFailed -> context.getString(R.string.feed_save_failed)
            ItemsMessage.NotEnoughBalance -> msgNoBalance
            ItemsMessage.MaxLevel -> msgMaxLevel
            is ItemsMessage.Upgraded -> msgUpgraded
            ItemsMessage.Repaired -> context.getString(R.string.toast_repaired)
            ItemsMessage.NothingToRepair -> context.getString(R.string.toast_nothing_to_repair)
            ItemsMessage.SignInRequired -> context.getString(R.string.toast_sign_in_required)
            ItemsMessage.Offline -> context.getString(R.string.toast_offline)
            ItemsMessage.UpgradeLegacy -> context.getString(R.string.sneaker_enhance_legacy)
            ItemsMessage.UpgradeListed -> context.getString(R.string.sneaker_enhance_listed)
            else -> null
        }
        if (text != null) Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
        viewModel.consumeMessage()
    }

    ShoeDetailContent(
        state = state,
        equipping = equipping,
        result = result,
        onBack = onBack,
        onOpenOwned = onOpenOwned,
        onReload = viewModel::reloadOwned,
        onWear = { viewModel.equip(sneakerId) },
        onResultShown = viewModel::consumeEquipResult,
        onEnhance = { enhanceOpen = true },
        onRepair = { viewModel.repair(sneakerId) },
        onSell = { shoe -> onSell(shoe.faction.id, shoe.rarity.id, shoe.variant, shoe.id) },
    )

    val sneaker = (state as? ShoeDetailState.Ready)?.shoe
    if (enhanceOpen && sneaker != null && sneaker.canUpgrade) {
        val cost = sneaker.upgradeCost
        val currentBalance = balance
        com.stepup.android.ui.components.DialogPanel(
            title = stringResource(R.string.sneaker_action_enhance),
            onDismiss = { enhanceOpen = false },
            actions = {
                VoltButton(
                    text = stringResource(R.string.sneaker_action_enhance),
                    enabled = currentBalance?.let { it >= cost } == true,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        enhanceOpen = false
                        viewModel.upgrade(sneaker.id)
                    },
                )
                GhostButton(
                    stringResource(R.string.common_cancel), { enhanceOpen = false }, Modifier.fillMaxWidth(),
                )
            },
        ) {
            SneakerFrame(sneaker = sneaker, modifier = Modifier.fillMaxWidth().height(144.dp))
            Text(stringResource(R.string.level_chip, sneaker.level) + " → " +
                stringResource(R.string.level_chip, sneaker.level + 1),
                style = MaterialTheme.typography.headlineSmall, color = Snow)
            Text(stringResource(R.string.items_upgrade_cost, "%,.0f".format(cost)),
                style = MaterialTheme.typography.bodyLarge, color = com.stepup.android.ui.theme.VoltText)
            if (currentBalance == null) {
                Text(stringResource(R.string.feed_loading), color = Silver)
            } else if (currentBalance < cost) {
                Text(msgNoBalance, color = Silver)
            }
        }
    }
}

/**
 * 상세 화면 — 상태를 받아 그리기만 한다(테스트가 시안 장면을 바로 넣어 찍는다). [initialSheet] 는 None · Stats · Explain · Info · Manage.
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
    onRepair: () -> Unit = {},
    onSell: (Sneaker) -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
    initialSheet: String? = null,
) {
    var sheet by rememberSaveable { mutableStateOf(initialSheet?.let(DetailSheet::valueOf) ?: DetailSheet.None) }
    var artAttempt by rememberSaveable { mutableIntStateOf(0) }
    val ready = state as? ShoeDetailState.Ready
    val shoe = ready?.shoe
    val wearing = ready?.wearing
    val korean = LocalConfiguration.current.locales[0].language == "ko"

    // 10 — 저장된 착용이 이 켤레로 바뀐 뒤 한 번만. 결말은 곧바로 지워 다시 들어와도 되풀이하지 않는다
    var toast by remember { mutableStateOf<String?>(null) }
    val wornMessage = shoe?.let { stringResource(R.string.sdv_worn_toast, it.shoeName().withParticle(korean)) }
    LaunchedEffect(result, shoe?.id, wearing?.id) {
        val current = result ?: return@LaunchedEffect
        if (shoe == null || current.targetId != shoe.id) return@LaunchedEffect
        // 11 을 띄운 뒤 늦게라도 저장된 착용이 이 켤레가 되면(연결이 돌아와 서버 값을 받음) 완료로 바꾼다
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

    // 목록 아래 고정 줄 — 조회 중이면 "확인 중…", 신발이 있으면 이 신발 신기 · 지금 신고 있어요 · 신발 바꾸는 중…
    val checkingBar: @Composable ColumnScope.() -> Unit = {
        WearButton(stringResource(R.string.sdv_checking), enabled = false, onClick = {})
    }
    val wearBar: @Composable ColumnScope.() -> Unit = {
        val worn = shoe?.equipped == true
        val label = when {
            equipping -> stringResource(R.string.sdv_wear_busy)
            worn -> stringResource(R.string.sdv_status_wearing)
            else -> stringResource(R.string.sdv_wear)
        }
        WearButton(label, enabled = !equipping && !worn, onClick = onWear)
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
                ShoeStatePanel(
                    icon = Icons.Filled.Refresh,
                    title = stringResource(R.string.sdv_load_failed_title),
                    body = stringResource(R.string.sdv_load_failed_body),
                    primary = stringResource(R.string.sdv_reload) to onReload,
                    secondary = stringResource(R.string.sdv_back_to_owned) to onOpenOwned,
                    modifier = Modifier.testTag("shoe-detail-failed"),
                )
            }
            ShoeDetailState.NotFound -> item(key = "missing") {
                ShoeStatePanel(
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
                    toast = toast,
                    artAttempt = artAttempt,
                    onRetryArt = {
                        forgetShoeArt(state.shoe)
                        artAttempt += 1
                    },
                )
            }
        }
    }

    if (shoe != null) {
        when (sheet) {
            DetailSheet.Stats, DetailSheet.Explain -> StatsSheet(
                shoe = shoe, explain = sheet == DetailSheet.Explain,
                onPage = { sheet = if (it) DetailSheet.Explain else DetailSheet.Stats },
                onClose = { sheet = DetailSheet.None },
            )
            DetailSheet.Info -> InfoSheet(shoe, zone, onClose = { sheet = DetailSheet.None })
            DetailSheet.Manage -> ManageSheet(
                shoe = shoe,
                onClose = { sheet = DetailSheet.None },
                onEnhance = { sheet = DetailSheet.None; onEnhance() },
                onRepair = { sheet = DetailSheet.None; onRepair() },
                onSell = { sheet = DetailSheet.None; onSell(shoe) },
                onOpenStats = { sheet = DetailSheet.Stats },
                onOpenInfo = { sheet = DetailSheet.Info },
            )
            DetailSheet.None -> Unit
        }
        // 11 — 바뀌지 않았다. 저장된 지금 착용(목록이 보는 값)을 말하고 다시 신기 · 닫기
        val failure = result as? EquipResult.NotWorn
        if (failure != null && failure.targetId == shoe.id && wearing?.id != shoe.id) {
            EquipErrorSheet(
                failure = failure,
                wearing = wearing,
                korean = korean,
                onClose = onResultShown,
                onRetry = {
                    onResultShown()
                    onWear()
                },
            )
        }
    }
}

// ── 본문 ──────────────────────────────────────────────────────

@Composable
private fun DetailBody(
    state: ShoeDetailState.Ready,
    toast: String?,
    artAttempt: Int,
    onRetryArt: () -> Unit,
) {
    val p = settingsPalette()
    val shoe = state.shoe
    // 어두운 테마는 확정안의 글자색, 밝은 테마는 설정 v1 의 글자색(흰 글자가 밝은 바닥에 묻히지 않게)
    val nameColor = if (StepUpColors.dark) CartoonColors.Text else p.text
    val numberColor = if (StepUpColors.dark) CartoonColors.Number else p.secondary
    val art by rememberShoeArt(shoe, ShoeArtFull, artAttempt)
    val failed = art == ShoeArtLoad.Failed
    // 작은 화면은 무대부터 줄인다 — 이름 · 네 칸 · 버튼이 한 화면에 들도록. 그래도 모자라면 목록이 넘어간다
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val stageMaxHeight = (screenHeight - 470.dp).coerceAtLeast(190.dp)
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        // 무대 — 기존 등급 무대(무대 면 → 뒤 효과 → 신발 → 앞 효과 → 프레임). 완료 알림(10)은 무대 위쪽에
        Box(
            Modifier.fillMaxWidth().widthIn(max = stageMaxHeight * GradeArtRatio).aspectRatio(GradeArtRatio)
                .then(if (art is ShoeArtLoad.Ready) Modifier.testTag("shoe-art") else Modifier),
        ) {
            SneakerGradeStage(shoe, Modifier.fillMaxSize(), showShoe = !failed)
            if (failed) {
                Box(Modifier.fillMaxSize().testTag("shoe-art-failed")) {
                    ArtFailed(onRetryArt, Modifier.align(Alignment.Center))
                }
            }
            WornToast(toast, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
        }
        Spacer(Modifier.height(16.dp))
        // 이름 → 등급 배지 → No. 번호 한 줄. 이름이 길면 두 줄까지, 배지와 번호는 함께 남는다
        ShoeNameWithBadge(
            name = shoe.shoeName(), tier = shoe.tier,
            style = androidx.compose.ui.text.TextStyle(
                color = nameColor, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 1.24.em, letterSpacing = (-0.02).em,
            ),
            maxLines = 2,
            suffix = formatShoeNumber(shoe.mintNumber),
            suffixStyle = androidx.compose.ui.text.TextStyle(
                color = numberColor, fontSize = 16.sp, fontWeight = FontWeight.Medium, fontFeatureSettings = "tnum",
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp).semantics { heading() }.testTag("shoe-name"),
        )
        Spacer(Modifier.height(12.dp))
        ShoeStatCells(shoe, Modifier.testTag("shoe-stat-cells"))
    }
}

/**
 * 화면 폭 주 버튼 — 이 신발 신기(흰 면), 지금 신고 있어요 · 신발 바꾸는 중… · 확인 중…(누를 수 없는 어두운 면).
 * 밝은 테마는 설정 v1 의 주 버튼(남색 면) · 옅은 면.
 */
@Composable
private fun WearButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val p = settingsPalette()
    val shape = RoundedCornerShape(17.dp)
    val face = if (enabled) p.primaryFace else SolidColor(if (StepUpColors.dark) Color(0xFF152137) else p.surface)
    Box(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(face, shape)
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp)
            .testTag("detail-primary-action"),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) p.primaryText else p.secondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center)
    }
}

/** 10 — "✓ 새벽 강변을 신었어요." 무대 위쪽 빈 곳에 잠깐. 한 번만 읽힌다 */
@Composable
private fun WornToast(message: String?, modifier: Modifier) {
    val p = settingsPalette()
    val motion = LocalMotion.current
    AnimatedVisibility(
        visible = message != null, modifier = modifier,
        enter = fadeIn(tween(motion.duration(160))),
        exit = fadeOut(tween(motion.duration(160))),
    ) {
        Row(
            Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(13.dp)).background(p.toast)
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag("shoe-worn-toast"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFFC7DCFF), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(message.orEmpty(), color = p.toastText, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 16 — 그림만 읽지 못했다. 이름 · 능력치 · 신기는 그대로, 그림만 다시 */
@Composable
private fun ArtFailed(onRetry: () -> Unit, modifier: Modifier) {
    val p = settingsPalette()
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Outlined.Image, contentDescription = null, tint = p.secondary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.sdv_art_failed), color = p.secondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Box(
            Modifier.heightIn(min = StepUpDesign.TouchTarget).feedbackClickable(role = Role.Button, onClick = onRetry)
                .padding(horizontal = 12.dp).testTag("shoe-art-retry"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.sdv_art_retry), color = p.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 13 — 임시 이름 · 능력치 · 착용 상태를 보이지 않고 자리만(무대 · 이름 줄 · 네 칸의 자리) */
@Composable
private fun DetailLoading() {
    val p = settingsPalette()
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp
    val stageMaxHeight = (screenHeight - 470.dp).coerceAtLeast(190.dp)
    Column(Modifier.fillMaxWidth().testTag("shoe-detail-loading"), horizontalAlignment = Alignment.CenterHorizontally) {
        ShoeStage(
            sneaker = null,
            modifier = Modifier.fillMaxWidth().widthIn(max = stageMaxHeight * GradeArtRatio).zIndex(-1f),
            overlay = {
                Text(stringResource(R.string.sdv_loading), color = p.secondary, fontSize = 15.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.align(ShoeStageMessageAlignment).semantics { liveRegion = LiveRegionMode.Polite })
            },
        )
        Spacer(Modifier.height(16.dp))
        Box(Modifier.size(width = 196.dp, height = 26.dp).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(4) {
                Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(14.dp)).background(p.skeleton))
            }
        }
    }
}

// ── 시트 ──────────────────────────────────────────────────────

/**
 * 04 능력치 자세히 · 05 능력치 설명 — 한 시트 안에서 내용만 바꾼다(겹쳐 쌓지 않는다).
 * 05 의 "능력치로 돌아가기"와 시스템 뒤로 가기는 04 로, X · 바깥 누르기는 시트 전체를 닫는다.
 */
@Composable
private fun StatsSheet(shoe: Sneaker, explain: Boolean, onPage: (explain: Boolean) -> Unit, onClose: () -> Unit) {
    val p = settingsPalette()
    val stats = shoeStats(shoe)
    SettingsSheet(
        title = stringResource(if (explain) R.string.sdv_explain_title else R.string.sdv_row_stats),
        onDismiss = onClose,
        modifier = Modifier.testTag(if (explain) "shoe-explain-sheet" else "shoe-stats-sheet"),
        actions = {
            SettingsPrimaryButton(
                stringResource(if (explain) R.string.sdv_explain_back else R.string.sdv_ok),
                onClick = { if (explain) onPage(false) else onClose() },
                modifier = Modifier.fillMaxWidth().testTag(if (explain) "shoe-explain-back" else "shoe-stats-ok"),
            )
        },
    ) {
        BackHandler(enabled = explain) { onPage(false) }
        if (!explain) {
            Text(stringResource(R.string.sdv_stats_subtitle, shoe.shoeName(), shoe.level), color = p.secondary, fontSize = 14.sp)
            Column {
                stats.forEach { StatRow(statLabel(it.stat), it.text, "shoe-stat-${it.stat.name.lowercase()}") }
            }
            Row(
                Modifier.fillMaxWidth().heightIn(min = StepUpDesign.TouchTarget)
                    .feedbackClickable(role = Role.Button) { onPage(true) }.testTag("shoe-stats-help"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.sdv_stats_help), color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f))
                Icon(Icons.Outlined.Info, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
            }
        } else {
            stats.forEach { stat ->
                Column(Modifier.fillMaxWidth().padding(top = 6.dp).semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(statLabel(stat.stat), color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() })
                    Text(statExplanation(stat.stat), color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String, tag: String) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}.testTag(tag)) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = p.secondary, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Text(value, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, softWrap = false)
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

@Composable
private fun statLabel(stat: ShoeStat): String = stringResource(
    when (stat) {
        ShoeStat.BONUS -> R.string.sdv_stat_bonus
        ShoeStat.ENERGY -> R.string.sdv_stat_energy
        ShoeStat.LUCK -> R.string.sdv_stat_luck
        ShoeStat.COMFORT -> R.string.sdv_stat_comfort
        ShoeStat.DURABILITY -> R.string.sdv_stat_durability
    },
)

@Composable
private fun statExplanation(stat: ShoeStat): String = stringResource(
    when (stat) {
        ShoeStat.BONUS -> R.string.sdv_explain_bonus
        ShoeStat.ENERGY -> R.string.sdv_explain_energy
        ShoeStat.LUCK -> R.string.sdv_explain_luck
        ShoeStat.COMFORT -> R.string.sdv_explain_comfort
        ShoeStat.DURABILITY -> R.string.sdv_explain_durability
    },
)

/**
 * 06 신발 정보 — 이름 · 신발 번호(민팅 번호, DB id · 토큰 번호가 아니다) · 등급 · 속성(예전 52종만) · 레벨 · 받은 날짜 · 체인.
 * 체인 줄은 시안에 없는 줄이다 — 서버가 준 토큰 번호만 보인다(없으면 "아직 체인에 없음").
 */
@Composable
private fun InfoSheet(shoe: Sneaker, zone: ZoneId, onClose: () -> Unit) {
    val p = settingsPalette()
    val pattern = stringResource(R.string.sdv_date_pattern)
    val formatter = remember(pattern) { DateTimeFormatter.ofPattern(pattern) }
    val received = formatter.format(Instant.ofEpochMilli(shoe.acquiredAt).atZone(zone))
    SettingsSheet(
        title = stringResource(R.string.sdv_row_info),
        onDismiss = onClose,
        modifier = Modifier.testTag("shoe-info-sheet"),
        actions = {
            SettingsPrimaryButton(stringResource(R.string.sdv_ok), onClose, Modifier.fillMaxWidth().testTag("shoe-info-ok"))
        },
    ) {
        Column {
            InfoRow(stringResource(R.string.sdv_info_name), shoe.shoeName(), "shoe-info-name")
            InfoRow(stringResource(R.string.sdv_info_number), "#%04d".format(shoe.mintNumber), "shoe-info-number")
            // 등급은 글 대신 공통 배지 — 신발 정보가 있는 곳마다 같은 모양
            InfoRow(stringResource(R.string.sdv_info_rarity), shoe.tier.label(), "shoe-info-rarity") {
                ShoeGradeBadge(shoe.tier, decorative = true)
            }
            // 속성은 예전 52종에만 있다 — 새 도감 신발에 자리 값을 보이지 않는다
            if (shoe.modelId == null) InfoRow(stringResource(R.string.sdv_info_faction), shoe.faction.label(), "shoe-info-faction")
            InfoRow(stringResource(R.string.sdv_info_level), stringResource(R.string.level_chip, shoe.level), "shoe-info-level")
            InfoRow(stringResource(R.string.sdv_info_received), received, "shoe-info-received")
            val chain = chainMarkOf(shoe.tokenId)
            InfoRow(
                stringResource(R.string.sdv_info_chain),
                when (chain) {
                    ChainMark.None -> stringResource(R.string.sdv_chain_none)
                    is ChainMark.Token -> stringResource(R.string.sdv_chain_token, chain.tokenId)
                    is ChainMark.Vault -> stringResource(R.string.sdv_chain_vault, chain.tokenId)
                },
                "shoe-info-chain",
                note = if (chain == ChainMark.None) stringResource(R.string.sdv_chain_none_note) else null,
            )
        }
        Text(stringResource(R.string.sdv_info_note), color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em,
            modifier = Modifier.padding(top = 6.dp))
    }
}

/** 정보 한 줄 — 이름 왼쪽, 값 오른쪽. 값 · 날짜는 필요한 만큼 줄을 바꾼다 */
@Composable
private fun InfoRow(
    label: String,
    value: String,
    tag: String,
    note: String? = null,
    /** 값 자리에 글 대신 둘 것(등급 배지) — [value] 는 읽기 도구가 읽는다 */
    valueContent: (@Composable () -> Unit)? = null,
) {
    val p = settingsPalette()
    Column(
        Modifier.fillMaxWidth().padding(vertical = 10.dp)
            .semantics(mergeDescendants = true) { if (valueContent != null) contentDescription = "$label $value" }
            .testTag(tag),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = if (valueContent != null) Alignment.CenterVertically else Alignment.Top) {
            Text(label, color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em, modifier = Modifier.padding(end = 16.dp))
            if (valueContent != null) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) { valueContent() }
            } else {
                Text(value, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em,
                    textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }
        }
        if (note != null) {
            Text(note, color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em, textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp))
        }
    }
}

/**
 * ⋯ 이 신발 관리 — 이전 상세의 강화 · 수리 · 판매(지우지 않고 옮김). 누를 수 없는 줄은 까닭을 함께 쓴다.
 * 강화는 기존 확인 창을 연다(SUP 는 확인한 뒤에만), 수리는 기존처럼 비용을 적은 줄을 누르면 바로, 판매는 거래소 등록 화면으로.
 */
@Composable
private fun ManageSheet(
    shoe: Sneaker,
    onClose: () -> Unit,
    onEnhance: () -> Unit,
    onRepair: () -> Unit,
    onSell: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    SettingsSheet(
        title = stringResource(R.string.sdv_manage_title),
        onDismiss = onClose,
        modifier = Modifier.testTag("shoe-manage-sheet"),
    ) {
        val block = shoe.upgradeBlock
        ManageRow(
            title = stringResource(R.string.sneaker_action_enhance),
            description = if (block == null) {
                stringResource(R.string.sdv_enhance_desc, shoe.level, shoe.level + 1, "%,.0f".format(shoe.upgradeCost))
            } else {
                stringResource(
                    when (block) {
                        com.stepup.android.domain.UpgradeBlock.MAX_LEVEL -> R.string.sneaker_enhance_max
                        com.stepup.android.domain.UpgradeBlock.LEGACY -> R.string.sneaker_enhance_legacy
                        com.stepup.android.domain.UpgradeBlock.LISTED -> R.string.sneaker_enhance_listed
                    },
                )
            },
            enabled = block == null,
            onClick = onEnhance,
            tag = "shoe-manage-enhance",
        )
        // 내구도가 줄었으면 수리 — 치른 SUP 는 소각된다(기존 조건 그대로)
        val repairCost = shoe.repairCost
        if (shoe.server?.upgradable == true && repairCost > 0.0) {
            ManageRow(
                title = stringResource(R.string.sneaker_action_repair, "%,.0f".format(kotlin.math.ceil(repairCost))),
                description = null,
                enabled = true,
                onClick = onRepair,
                tag = "shoe-manage-repair",
            )
        }
        ManageRow(
            title = stringResource(R.string.sneaker_action_sell),
            description = stringResource(if (shoe.equipped) R.string.sneaker_sell_equipped else R.string.sdv_sell_desc),
            enabled = !shoe.equipped,
            onClick = onSell,
            tag = "shoe-manage-sell",
        )
        // 본문에서 옮긴 능력치 자세히 · 신발 정보(2026-09-30 — 본문은 네 칸만)
        ManageRow(stringResource(R.string.sdv_row_stats), null, enabled = true, onClick = onOpenStats, tag = "shoe-row-stats")
        ManageRow(stringResource(R.string.sdv_row_info), null, enabled = true, onClick = onOpenInfo, tag = "shoe-row-info")
        // 이 NFT 정보(이전 상세의 설명 칸) — 네 속성 이야기라 예전 52종에만
        if (shoe.modelId == null) {
            SettingsGroupLabel(stringResource(R.string.sneaker_about))
            SettingsNote(stringResource(R.string.sneaker_about_body), top = true)
        }
    }
}

@Composable
private fun ManageRow(title: String, description: String?, enabled: Boolean, onClick: () -> Unit, tag: String) {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick).testTag(tag)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 12.dp).alpha(if (enabled) 1f else 0.55f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.3.em)
                if (description != null) Text(description, color = p.secondary, fontSize = 13.sp, lineHeight = 1.4.em)
            }
            if (enabled) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary,
                    modifier = Modifier.size(22.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/**
 * 11 — 착용을 바꾸지 못했다. 저장된 지금 착용을 말한다: 신고 있던 켤레 그대로 · 아직 없음. 결과를 모르면(연결 끊김 ·
 * 확인 안 됨) 단정하지 않는다. 다시 신기는 최신 목록으로 소유를 다시 확인한 뒤 보낸다.
 */
@Composable
private fun EquipErrorSheet(failure: EquipResult.NotWorn, wearing: Sneaker?, korean: Boolean, onClose: () -> Unit, onRetry: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(if (failure.confirmed) R.string.sdv_error_title else R.string.sdv_error_unknown_title),
        onDismiss = onClose,
        modifier = Modifier.testTag("shoe-equip-error"),
        actions = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                SettingsSecondaryButton(stringResource(R.string.common_close), onClose,
                    Modifier.weight(1f).heightIn(min = 54.dp).testTag("shoe-error-close"))
                SettingsPrimaryButton(stringResource(R.string.sdv_retry_wear), onRetry,
                    Modifier.weight(1f).heightIn(min = 54.dp).testTag("shoe-error-retry"))
            }
        },
    ) {
        val kept = when {
            !failure.confirmed -> stringResource(R.string.sdv_error_unknown_body)
            wearing != null -> stringResource(R.string.sdv_error_kept, wearing.shoeName().withParticle(korean))
            else -> stringResource(R.string.sdv_error_none)
        }
        Text(kept, color = p.text, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.4.em,
            modifier = Modifier.testTag("shoe-error-kept"))
        Text(
            stringResource(
                when (failure.reason) {
                    EquipFailure.SIGN_IN -> R.string.toast_sign_in_required
                    EquipFailure.OFFLINE -> R.string.toast_offline
                    EquipFailure.MISSING -> R.string.sdv_missing_body
                    EquipFailure.REJECTED, EquipFailure.UNCONFIRMED -> R.string.sdv_error_hint
                },
            ),
            color = p.secondary, fontSize = 14.sp, lineHeight = 1.45.em,
        )
    }
}

// ── 공용 ──────────────────────────────────────────────────────

/**
 * 조회 실패(14) · 없는 신발(15) · 빈 목록(18) — 아이콘 칸 · 제목 · 한 줄 · 화면 폭 버튼(주 · 보조).
 * 지갑 v1 의 상태 칸과 같은 모양이다.
 */
@Composable
internal fun ShoeStatePanel(
    icon: ImageVector,
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
