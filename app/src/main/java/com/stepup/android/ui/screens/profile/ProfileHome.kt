package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.ChamferShape
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunNumber
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.SneakerVisual
import com.stepup.android.ui.components.blueListColors
import com.stepup.android.ui.components.formatSupDown
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.guide.GuideTour
import com.stepup.android.ui.guide.guideTarget
import com.stepup.android.ui.theme.StepUpSans
import java.util.Locale

/**
 * 내 정보 첫 화면 — 파란 톤 v4 러닝 패스(PF01~04 · PRO12, docs/redesign/blue-v4-2026-10/01-packages/stepup-profile-blue-claude-v19).
 *
 * "내 정보" 제목 → 사진 · 이름 · 러너 레벨 · 프로필 수정 → 러닝 패스 카드(누적 거리 · 총 러닝) → 흰 "내 러닝 기록 보기" →
 * 챌린지 · 지갑 · 설정. 공통 머리(로고 · SUP)와 아래 다섯 탭은 앱 셸의 것을 쓴다.
 *
 * 러너 레벨은 걸음으로 쌓은 기존 `state.runner.level` 이다(신발 강화 레벨 · 러닝 횟수와 섞지 않는다). 패스의 신발은 지금 신은
 * 신발의 실제 그림이고, 신은 신발이 없거나 아직 읽지 못했으면 비워 둔다(다른 신발로 채우지 않는다). 패스의 도시 사진은 장식이며
 * 위치나 경로가 아니다. 거리 · 횟수는 저장된 러닝 세션의 합 — 읽는 중엔 "—", 못 읽으면 실패 안내와 다시 시도(0 으로 보이지 않는다).
 */
@Composable
internal fun ProfileHome(
    state: ProfileViewModel.UiState,
    records: RunRecordState,
    onRetryRecords: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenRecords: () -> Unit,
    onOpenChallenges: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val t = runTone()
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.tab_me),
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                fontSize = 36.sp, lineHeight = 1.15.em, letterSpacing = (-0.03).em, color = t.text,
            ),
            modifier = Modifier.padding(start = 2.dp).semantics { heading() },
        )
        Spacer(Modifier.height(14.dp))
        ProfileRow(state, onEditProfile)
        Spacer(Modifier.height(18.dp))
        PassCard(records, state.equipped.takeIf { state.loaded }, onRetryRecords)
        Spacer(Modifier.height(18.dp))
        val recordsLabel = stringResource(R.string.me_path_records)
        RunButton(
            recordsLabel, onOpenRecords, Modifier.testTag("profile-records"),
            icon = Icons.Filled.Description, chevron = true, italic = recordsLabel.length <= 14,
        )
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MenuCard(
                icon = Icons.AutoMirrored.Filled.DirectionsRun, label = stringResource(R.string.challenge_title),
                onClick = onOpenChallenges, tag = "profile-challenges",
            )
            MenuCard(
                icon = Icons.Outlined.AccountBalanceWallet, label = stringResource(R.string.me_path_wallet),
                // 읽기 전에 "0 SUP"을 보이면 잔액이 사라진 것처럼 읽힌다
                value = if (state.loaded) stringResource(R.string.me_path_balance, formatSupDown(state.balance)) else "—",
                onClick = onOpenWallet, tag = "profile-wallet",
            )
            MenuCard(
                icon = Icons.Filled.Settings, label = stringResource(R.string.profile_tab_settings),
                onClick = onOpenSettings, tag = "profile-settings",
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

/** 누적 러닝 — 불러오는 중 · 값 · 실패. 실패를 0km 로, 불러오는 중을 0회로 보이지 않는다 */
sealed interface RunRecordState {
    data object Loading : RunRecordState
    data class Ready(val meters: Double, val runs: Int) : RunRecordState
    data object Failed : RunRecordState
}

/** 사진 · 이름 · 러너 레벨 · 프로필 수정 — 사진을 눌러도 같은 편집 화면 */
@Composable
private fun ProfileRow(state: ProfileViewModel.UiState, onEditProfile: () -> Unit) {
    val t = runTone()
    val edit = stringResource(R.string.me_path_edit)
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp), verticalAlignment = Alignment.CenterVertically) {
        BlueAvatar(
            avatarId = state.avatarId, photo = rememberCustomAvatar(state.avatarRev), size = 68.dp,
            contentDescription = edit,
            modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape)
                .feedbackClickable(onClick = onEditProfile).guideTarget(GuideTour.Targets.PROFILE_AVATAR),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                state.nickname.ifBlank { stringResource(R.string.me_default_name) },
                style = runTextStyle(23.sp, t.text, FontWeight.Bold, 1.2f),
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("profile-name"),
            )
            // 러너 레벨 — 읽기 전엔 비운다(1 로 보이지 않게)
            if (state.loaded) {
                val chip = RoundedCornerShape(50)
                Text(
                    stringResource(R.string.level_chip, state.runner.level),
                    style = runTextStyle(14.sp, t.cyan, FontWeight.Bold, 1.1f), maxLines = 1,
                    modifier = Modifier.clip(chip).background(t.inset, chip).border(1.dp, t.cyan.copy(alpha = 0.55f), chip)
                        .padding(horizontal = 12.dp, vertical = 4.dp).testTag("profile-level"),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        val pill = RoundedCornerShape(50)
        Row(
            Modifier.heightIn(min = 48.dp).clip(pill).background(t.secondaryFace, pill).border(1.5.dp, t.secondaryEdge, pill)
                .feedbackClickable(role = Role.Button, onClick = onEditProfile)
                .padding(horizontal = 16.dp).testTag("profile-edit"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = t.label, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(edit, style = runTextStyle(15.sp, t.text, FontWeight.SemiBold), maxLines = 1)
        }
    }
}

/** 패스 카드의 색 — 밝은 테마에서도 밤 사진 위의 남색 표(글자는 흰색) */
private val PassTop = Color(0xFF0B2E62)
private val PassBottom = Color(0xFF051733)
private val PassEdge = Color(0xFF48D9FA)
private val PassText = Color(0xFFF5F8FF)
private val PassLabel = Color(0xFFAAC3EA)

/**
 * 러닝 패스 — 왼쪽 위 큰 이름, 오른쪽 위 밤 도시(장식) 와 지금 신은 신발, 아래 누적 거리 · 총 러닝.
 * 모서리를 깎은 남색 표에 시안 빛 테두리.
 */
@Composable
private fun PassCard(records: RunRecordState, shoe: Sneaker?, onRetry: () -> Unit) {
    val shape = ChamferShape(18.dp)
    Column(
        Modifier.fillMaxWidth()
            .drawBehind {
                val outline = shape.createOutline(size, layoutDirection, this)
                if (outline is Outline.Generic) drawPath(outline.path, PassEdge.copy(alpha = 0.16f), style = Stroke(9.dp.toPx()))
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(PassTop, PassBottom)))
            .drawWithContent {
                drawContent()
                val outline = shape.createOutline(size, layoutDirection, this)
                if (outline is Outline.Generic) drawPath(outline.path, PassEdge.copy(alpha = 0.9f), style = Stroke(1.8.dp.toPx()))
            }
            .testTag("profile-record-card"),
    ) {
        PassScene(shoe)
        Column(Modifier.fillMaxWidth().padding(start = 22.dp, end = 22.dp, bottom = 20.dp)) {
            if (records is RunRecordState.Failed) {
                Column(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        stringResource(R.string.me_path_failed), style = runTextStyle(20.sp, PassText, FontWeight.Bold),
                        textAlign = TextAlign.Center, modifier = Modifier.testTag("profile-record-failed"),
                    )
                    Spacer(Modifier.height(10.dp))
                    val chip = RoundedCornerShape(50)
                    Box(
                        Modifier.heightIn(min = 48.dp).clip(chip).border(1.5.dp, PassEdge.copy(alpha = 0.8f), chip)
                            .feedbackClickable(role = Role.Button, onClick = onRetry)
                            .padding(horizontal = 30.dp).testTag("profile-record-retry"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.me_path_retry), style = runTextStyle(16.sp, PassEdge, FontWeight.Bold))
                    }
                }
            } else {
                val ready = records as? RunRecordState.Ready
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                    PassStat(
                        stringResource(R.string.pf_total_distance),
                        // 천 단위 쉼표(랭킹과 같은 모양) — 1,284.5
                        ready?.let { String.format(Locale.ROOT, "%,.1f", it.meters / 1000) } ?: "—",
                        if (ready != null) "km" else null, "profile-distance", Modifier.weight(1f),
                    )
                    Box(Modifier.padding(vertical = 6.dp).width(1.dp).fillMaxHeight().background(PassLabel.copy(alpha = 0.35f)))
                    PassStat(
                        stringResource(R.string.pf_total_runs),
                        ready?.let { String.format(Locale.ROOT, "%,d", it.runs) } ?: "—",
                        if (ready != null) stringResource(R.string.pf_runs_unit) else null, "profile-runs",
                        Modifier.weight(1f).padding(start = 22.dp),
                    )
                }
                when {
                    records is RunRecordState.Loading -> Row(
                        Modifier.fillMaxWidth().padding(top = 14.dp).testTag("profile-record-loading"),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                    ) {
                        RunSpinner(Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.me_path_loading), style = runTextStyle(15.sp, PassEdge, FontWeight.Medium))
                    }
                    ready != null && ready.runs == 0 -> Text(
                        stringResource(R.string.pf_first_run), style = runTextStyle(15.sp, PassLabel, FontWeight.Medium),
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).testTag("profile-first-run"),
                    )
                }
            }
        }
    }
}

/** 수치 한 칸 — 작은 이름 · 큰 수 · 단위(폭이 모자라면 같이 줄인다) */
@Composable
private fun PassStat(label: String, value: String, unit: String?, tag: String, modifier: Modifier) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = runTextStyle(16.sp, PassLabel, FontWeight.Medium), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(2.dp))
        RunNumber(
            value, unit = unit, size = 46.sp, unitSize = 22.sp, color = PassText, unitColor = PassText,
            valueTag = tag, italicUnit = false, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** 패스 위 장면 — 밤 도시 사진(장식)에 파란 빛을 얹고, 비스듬한 파란 띠 · 바닥 빛 위에 지금 신은 신발 */
@Composable
private fun PassScene(shoe: Sneaker?) {
    Box(Modifier.fillMaxWidth().height(236.dp)) {
        // 도시 사진 — 오른쪽 위 3분의 2 · 위 64%
        Box(Modifier.align(Alignment.TopEnd).fillMaxWidth(0.68f).fillMaxHeight(0.66f)) {
            Image(
                com.stepup.android.ui.components.cachedPainterResource(R.drawable.home_banner_blue_night), contentDescription = null,
                contentScale = ContentScale.Crop, alignment = BiasAlignment(0.75f, 0.1f),
                modifier = Modifier.fillMaxSize(),
            )
            Box(Modifier.fillMaxSize().background(Color(0xFF0A3CA8).copy(alpha = 0.38f)))
            // 왼쪽 · 아래로 남색에 녹아든다
            Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(0f to PassTop, 0.35f to PassTop.copy(alpha = 0f))))
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(0.7f to Color.Transparent, 1f to Color(0xFF0A2856))))
        }
        // 비스듬한 파란 띠와 바닥 빛
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val floor = h * 0.66f
            drawPath(
                Path().apply {
                    moveTo(w * 0.58f, 0f); lineTo(w * 0.70f, 0f); lineTo(w * 0.44f, floor); lineTo(w * 0.32f, floor); close()
                },
                Color(0xFF1C5BFF).copy(alpha = 0.55f),
            )
            drawLine(PassEdge.copy(alpha = 0.18f), Offset(0f, floor), Offset(w, floor), strokeWidth = 1.dp.toPx())
            drawOval(
                Brush.radialGradient(
                    listOf(Color(0xFF3D8BFF).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.70f, h * 0.90f), radius = w * 0.32f,
                ),
                topLeft = Offset(w * 0.38f, h * 0.80f), size = androidx.compose.ui.geometry.Size(w * 0.64f, h * 0.2f),
            )
        }
        Text(
            stringResource(R.string.pf_pass_title),
            style = TextStyle(
                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontStyle = FontStyle.Italic,
                fontSize = 32.sp, lineHeight = 1.15.em, letterSpacing = (-0.03).em, color = PassText,
            ),
            modifier = Modifier.align(Alignment.TopStart).padding(start = 22.dp, top = 22.dp).fillMaxWidth(0.55f),
        )
        // 지금 신은 신발 — 없으면 비워 둔다
        if (shoe != null) {
            SneakerVisual(
                shoe,
                Modifier.align(Alignment.BottomEnd).padding(end = 14.dp, bottom = 4.dp).fillMaxWidth(0.62f).height(150.dp)
                    .testTag("profile-pass-shoe"),
            )
        }
    }
}

/** 메뉴 칸 — 줄 전체를 누른다. 아이콘 · 이름 · (값 알약) · 화살표 */
@Composable
private fun MenuCard(icon: ImageVector, label: String, onClick: () -> Unit, tag: String, value: String? = null) {
    val t = runTone()
    val c = blueListColors()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp).clip(shape).background(c.face, shape)
            .border(1.dp, c.edge.copy(alpha = 0.6f), shape)
            .feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(start = 18.dp, end = 12.dp, top = 8.dp, bottom = 8.dp).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = t.cyan, modifier = Modifier.size(28.dp))
        Spacer(Modifier.width(18.dp))
        Text(
            label, style = runTextStyle(18.sp, t.text, FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (value != null) {
            val pill = RoundedCornerShape(50)
            Text(
                value, style = runTextStyle(15.sp, t.cyan, FontWeight.Bold), maxLines = 1, softWrap = false,
                modifier = Modifier.clip(pill).background(t.inset, pill).border(1.dp, t.cyan.copy(alpha = 0.3f), pill)
                    .padding(horizontal = 14.dp, vertical = 5.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.label, modifier = Modifier.size(26.dp))
    }
}
