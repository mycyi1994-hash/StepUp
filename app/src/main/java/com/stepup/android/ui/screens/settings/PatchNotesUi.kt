package com.stepup.android.ui.screens.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.stepup.android.BuildConfig
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.PatchArt
import com.stepup.android.domain.PatchNote
import com.stepup.android.domain.PatchNotes
import com.stepup.android.domain.PatchNotesRules
import com.stepup.android.domain.PatchRelease
import com.stepup.android.ui.components.LockCover
import com.stepup.android.ui.components.NavHeader
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpIcons
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.experience.feedbackClickable

/*
 * 업데이트 소식 창(S-337) · 업데이트 내역(S-339) — v12 디자인 전달 2026-11-26 「07_추가_업데이트소식창」.
 * 문구 · 판은 앱 안 목록([PatchNotes]). 소식 창은 업데이트한 뒤 처음 러닝 홈에서 한 번(판마다 한 번), 새로 설치한 사람에게는 띄우지 않는다.
 */

/** 테스트 판(debug · qa) — 테스트 전용 줄(스댕이 미리보기)을 함께 보인다 */
private val testBuild: Boolean get() = BuildConfig.TEST_TOOLS

/** 처음 설치한 뒤 한 번도 업데이트하지 않았다 */
private fun freshInstall(context: android.content.Context): Boolean = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    info.firstInstallTime == info.lastUpdateTime
}.getOrDefault(false)

/**
 * 소식 창을 띄울지 정하고 띄운다 — [allowed](러닝 홈 · 첫 안내 · 멈춘 러닝 · 가야 할 화면이 없을 때)일 때만.
 * 띄운 순간 그 판을 본 것으로 적는다(같은 판에서 다시 뜨지 않게). 새로 설치한 사람은 보이지 않고 판만 적는다.
 */
@Composable
fun UpdateNewsHost(allowed: Boolean, onOpenHistory: () -> Unit) {
    val context = LocalContext.current
    val current = BuildConfig.VERSION_NAME
    val release = remember { PatchNotes.of(current) }
    var decided by remember { mutableStateOf(false) }
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = ServiceLocator.userPrefs
        val seen = runCatching { prefs.patchNotesSeen() }.getOrNull()
        val fresh = freshInstall(context)
        val hasNotes = release != null && PatchNotes.headline(release, testBuild).isNotEmpty()
        if (PatchNotesRules.shouldShow(seen, current, fresh, hasNotes)) {
            show = true
        } else if (seen != current) {
            runCatching { prefs.setPatchNotesSeen(current) }
        }
        decided = true
    }
    if (!decided || !show || !allowed || release == null) return
    LaunchedEffect(release.version) { runCatching { ServiceLocator.userPrefs.setPatchNotesSeen(current) } }
    UpdateNewsDialog(
        release, testBuild,
        onClose = { show = false },
        onOpenHistory = {
            show = false
            onOpenHistory()
        },
    )
}

/** 소식 창 — 홈 위 어두운 막 · 가운데 창 · 줄 셋까지 · 「그 밖에」 · 확인 · 업데이트 내역 전체 보기 */
@Composable
fun UpdateNewsDialog(release: PatchRelease, testBuild: Boolean, onClose: () -> Unit, onOpenHistory: () -> Unit) {
    if (LockCover.hidesWindows()) return
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        UpdateNewsContent(release, testBuild, onClose, onOpenHistory)
    }
}

@Composable
fun UpdateNewsContent(release: PatchRelease, testBuild: Boolean, onClose: () -> Unit, onOpenHistory: () -> Unit) {
    val dark = StepUpUi.colors.dark
    val face = if (dark) Color(0xFF10172A) else Color.White
    val blue = if (dark) Color(0xFF86A9FF) else Color(0xFF2458E9)
    val sub = if (dark) Color(0xFFA3A9B6) else Color(0xFF6B7684)
    val line = if (dark) Color(0x1FA0B9FF) else Color(0x1F3C3C43)
    val shape = RoundedCornerShape(24.dp)
    Box(
        Modifier.fillMaxSize().background(if (dark) Color(0xB8000000) else Color(0x94080C16))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(horizontal = 24.dp).widthIn(max = 420.dp).fillMaxWidth().clip(shape).background(face)
                .then(if (dark) Modifier.border(1.dp, line, shape) else Modifier)
                // 창 안을 눌러도 닫히지 않게
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 18.dp)
                .testTag("update-news"),
        ) {
            Text(stringResource(R.string.pn_update_label, release.version), color = blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.pn_title), color = StepUpUi.colors.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.03).em, modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                PatchNotes.headline(release, testBuild).forEach { note -> NewsRow(note, sub, blue) }
            }
            release.more?.let {
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(1.dp).background(line))
                Spacer(Modifier.height(14.dp))
                Text(stringResource(it), color = sub, fontSize = 14.sp)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton(stringResource(R.string.pn_ok), onClose, arrow = false, modifier = Modifier.fillMaxWidth().testTag("update-news-ok"))
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.align(Alignment.CenterHorizontally).heightIn(min = 40.dp).clip(RoundedCornerShape(10.dp))
                    .feedbackClickable(role = Role.Button, onClick = onOpenHistory).padding(horizontal = 8.dp)
                    .testTag("update-news-history"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.pn_history_all), color = blue, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = blue, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun NewsRow(note: PatchNote, sub: Color, blue: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ArtTile(note.art)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(note.title), color = StepUpUi.colors.label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (note.isNew) {
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.pn_new), color = blue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            note.body?.let {
                Spacer(Modifier.height(2.dp))
                Text(stringResource(it), color = sub, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** 그림 칸 56 — 러닝(파랑) · 신발(작은 남색 카드) · 수리(주황 렌치 — 내구도 경고 색) · 스댕이(방 바탕) */
@Composable
private fun ArtTile(art: PatchArt?) {
    val dark = StepUpUi.colors.dark
    val (bg, ink) = when (art) {
        PatchArt.REPAIR -> (if (dark) Color(0x29FF8A3D) else Color(0xFFFFEEE2)) to (if (dark) Color(0xFFFF9A57) else Color(0xFFC24E06))
        PatchArt.PUP -> (if (dark) Color(0xFF12292A) else Color(0xFFDCEFEA)) to (if (dark) Color(0xFFE8F4F0) else Color(0xFF1B2433))
        PatchArt.SHOE -> (if (dark) Color(0xFF18213A) else Color(0xFFEEF2F7)) to Color.White
        else -> StepUpUi.colors.primary.copy(alpha = if (dark) .22f else .12f) to StepUpUi.colors.primary
    }
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(bg), contentAlignment = Alignment.Center) {
        when (art) {
            PatchArt.SHOE -> Box(
                Modifier.size(width = 30.dp, height = 40.dp).clip(RoundedCornerShape(6.dp))
                    .background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF13285A), Color(0xFF0A1633))))
                    .border(1.5.dp, Color(0xFFD3DBE8), RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center,
            ) { Icon(StepUpIcons.Shoe, null, tint = ink, modifier = Modifier.size(20.dp)) }
            PatchArt.REPAIR -> Icon(Icons.Outlined.Build, null, tint = ink, modifier = Modifier.size(26.dp))
            // 스댕이 — 미리보기와 같은 Lottie 의 한 장면(움직이지 않는다)
            PatchArt.PUP -> {
                val pup by com.airbnb.lottie.compose.rememberLottieComposition(com.airbnb.lottie.compose.LottieCompositionSpec.Asset("pup/lv4/idle_loop.json"))
                if (pup != null) {
                    com.airbnb.lottie.compose.LottieAnimation(pup, progress = { 0.2f }, modifier = Modifier.size(78.dp).padding(top = 10.dp))
                } else {
                    Icon(Icons.Rounded.Pets, null, tint = ink, modifier = Modifier.size(28.dp))
                }
            }
            else -> Icon(Icons.AutoMirrored.Filled.DirectionsRun, null, tint = ink, modifier = Modifier.size(28.dp))
        }
    }
}

/** 업데이트 내역(S-339) — 판마다 머리 줄(판 · 지금 버전 · 날짜) 아래 바뀐 것 목록. 최신이 맨 위 */
@Composable
fun UpdateHistoryScreen(onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val c = StepUpUi.colors
    val current = BuildConfig.VERSION_NAME
    val face = if (c.dark) Color(0xFF10172A) else Color.White
    val line = if (c.dark) Color(0x1FA0B9FF) else Color(0x1F3C3C43)
    Column(Modifier.fillMaxSize().testTag("update-history")) {
        NavHeader(stringResource(R.string.pn_history_title), onBack = onBack, backTag = "update-history-back")
        LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 28.dp)) {
            items(PatchNotes.releases, key = { it.version }) { release ->
                val notes = PatchNotes.visible(release, testBuild)
                Column(Modifier.padding(bottom = 28.dp).testTag("update-history-${release.version}")) {
                    Row(Modifier.fillMaxWidth().height(24.dp).padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(release.version, color = c.label, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold,
                            style = androidx.compose.ui.text.TextStyle(fontFeatureSettings = "tnum"))
                        if (release.version == current) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.pn_current), color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.weight(1f))
                        Text(release.date, color = c.secondaryLabel, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(face)) {
                        notes.forEachIndexed { i, note ->
                            if (i > 0) Box(Modifier.padding(horizontal = 18.dp).fillMaxWidth().height(1.dp).background(line))
                            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(note.title), color = c.label, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f, fill = false))
                                if (note.isNew) {
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.pn_new), color = c.accentText, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
