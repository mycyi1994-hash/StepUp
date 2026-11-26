package com.stepup.android.ui.screens.pup

import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.stepup.android.R
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors

/*
 * 스댕이 키우기 — v12 디자인 시제품(2026-11-26)의 화면만 옮긴 미리보기다. 사용자 "스댕이 키우기는 일단 그냥 두고 디자인만 대충 살려놔 · 기획 줄게".
 *
 * 규칙(밥 · 산책 · 쓰다듬기 · 도장 · 꾸미기 · 레벨)은 아직 없다 — 서버도 앱도 아무것도 세지 않는다. 그래서 숫자를 지어내지 않고
 * (시안의 "640 / 1,000" · "밥 1그릇" 같은 예시를 그대로 쓰지 않는다) 단계 이름과 모양만 보인다. 누르면 "곧 열려요".
 * 강아지를 누르면 쓰다듬는 움직임만 한 번 돈다(아무것도 남지 않는다).
 *
 * 그림: 방은 시제품 캡처에서 잘라 낸 `pup_room_<단계>`(어두운 테마는 `_d` — 앱 테마는 앱 설정이라 drawable-night 를 쓰지 않는다), 강아지는 `assets/pup/lv<단계>/` 의 json(Lottie, 1080 정사각,
 * 발 위치 540 · 900). 기획이 오면 이 파일을 다시 쓴다. 테스트 판(debug · qa)에서만 입구를 연다(`PupFeature.SHOWN`).
 */
object PupFeature {
    /** 입구(러닝 홈 한 줄 · 내 정보 줄)를 보이는가 — 기획 전에는 테스트 판에서만 */
    val SHOWN: Boolean get() = com.stepup.android.BuildConfig.TEST_TOOLS
}

/** 일곱 단계 — 시제품의 이름 · 방 · 바탕색 · 강아지 크기(방 그림 높이에 대한 비율) · 발 위치 */
private class PupStage(
    val level: Int,
    @StringRes val name: Int,
    @DrawableRes val room: Int,
    @DrawableRes val roomNight: Int,
    val light: Color,
    val night: Color,
    /** 방 그림 안의 발 위치(0 ~ 1) */
    val footX: Float,
    val footY: Float,
    /** 강아지 키 / 방 그림 높이 */
    val height: Float,
    /** 이 단계 Lottie 안에서 강아지 키(px, 1080 정사각 안) */
    val artHeight: Float,
)

// 시제품 data.js 의 PUP · LOTTIE 값 — 화면(1598px) 기준을 잘라 낸 방(150 ~ 615px)으로 옮겼다
private const val SCREEN_H = 1598f
private const val CROP_TOP = 150f
private const val CROP_H = 465f
private fun footY(screen: Float) = (screen * SCREEN_H - CROP_TOP) / CROP_H
private fun tall(screen: Float) = screen * SCREEN_H / CROP_H

private val Stages = listOf(
    PupStage(1, R.string.pup_stage_1, R.drawable.pup_room_1, R.drawable.pup_room_1_d, Color(0xFFE5E1EF), Color(0xFF211E33), .490f, footY(.3120f), tall(.0727f), 300f),
    PupStage(2, R.string.pup_stage_2, R.drawable.pup_room_2, R.drawable.pup_room_2_d, Color(0xFFF5E5E8), Color(0xFF2C1F29), .490f, footY(.3082f), tall(.1148f), 420f),
    PupStage(3, R.string.pup_stage_3, R.drawable.pup_room_3, R.drawable.pup_room_3_d, Color(0xFFE7EEDE), Color(0xFF1C2627), .490f, footY(.3131f), tall(.1131f), 470f),
    PupStage(4, R.string.pup_stage_4, R.drawable.pup_room_4, R.drawable.pup_room_4_d, Color(0xFFDBF0E9), Color(0xFF112929), .490f, footY(.3156f), tall(.1397f), 520f),
    PupStage(5, R.string.pup_stage_5, R.drawable.pup_room_5, R.drawable.pup_room_5_d, Color(0xFFF2E7D5), Color(0xFF272530), .504f, footY(.3462f), tall(.1746f), 560f),
    PupStage(6, R.string.pup_stage_6, R.drawable.pup_room_6, R.drawable.pup_room_6_d, Color(0xFFDCEAF5), Color(0xFF13253B), .501f, footY(.3436f), tall(.1589f), 600f),
    PupStage(7, R.string.pup_stage_7, R.drawable.pup_room_7, R.drawable.pup_room_7_d, Color(0xFFD5DCF6), Color(0xFF171D3F), .510f, footY(.3338f), tall(.1994f), 640f),
)

private const val LOTTIE_SIZE = 1080f
private const val LOTTIE_FOOT_X = 540f
private const val LOTTIE_FOOT_Y = 900f

@Composable
fun PupScreen(onBack: () -> Unit) {
    var index by rememberSaveable { mutableIntStateOf(0) }
    val stage = Stages[index]
    val dark = StepUpColors.dark
    val page = if (dark) stage.night else stage.light
    val card = if (dark) Color.White.copy(alpha = .08f) else Color.White
    val c = StepUpUi.colors
    val context = LocalContext.current
    val soon = stringResource(R.string.pup_soon)
    val showSoon = { Toast.makeText(context, soon, Toast.LENGTH_SHORT).show() }

    Column(Modifier.fillMaxSize().background(page).testTag("pup-screen")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Box(Modifier.fillMaxWidth()) {
                PupRoom(stage)
                Row(
                    Modifier.statusBarsPadding().padding(horizontal = 8.dp).height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(22.dp)).feedbackClickable(onClick = onBack).testTag("pup-back"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.cd_back), tint = c.label)
                    }
                    Text(stringResource(R.string.pup_title), style = StepUpUi.type.navTitle.copy(fontSize = 20.sp), color = c.label)
                }
            }
            Column(Modifier.padding(horizontal = StepUpUi.Gutter), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PupCard(card) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Lv.${stage.level}", color = c.secondaryLabel, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(stage.name), color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f).testTag("pup-stage-name"))
                        Row(
                            Modifier.clip(RoundedCornerShape(10.dp)).feedbackClickable { index = (index + 1) % Stages.size }
                                .padding(horizontal = 4.dp, vertical = 6.dp).testTag("pup-next-stage"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(stringResource(R.string.pup_stages), color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.accentText, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    val next = Stages.getOrNull(index + 1)
                    Text(
                        if (next != null) stringResource(R.string.pup_next_stage, stringResource(next.name)) else stringResource(R.string.pup_last_stage),
                        color = c.label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(10.dp))
                    PupBar(0f)
                }
                PupCard(card) {
                    Row(Modifier.fillMaxWidth()) {
                        PupStat(R.string.pup_food, Modifier.weight(1.6f))
                        PupStat(R.string.pup_walk, Modifier.weight(1f))
                        PupStat(R.string.pup_pet, Modifier.weight(1f))
                    }
                }
                PupCard(card) {
                    Text(stringResource(R.string.pup_week_stamps), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(14.dp))
                    val days = stringResource(R.string.pup_week_days).split(",")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        days.forEach { day ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(36.dp)) {
                                Icon(Icons.Rounded.Pets, null, tint = c.separator, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.height(6.dp))
                                Text(day, color = c.secondaryLabel, fontSize = 13.sp)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(Modifier.fillMaxWidth().height(1.dp).background(c.separator))
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).feedbackClickable(onClick = showSoon).testTag("pup-deco"),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.pup_deco), color = c.label, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.pup_soon), color = c.secondaryLabel, fontSize = 13.sp)
                        }
                        (if (dark) listOf(R.drawable.pup_deco_1_d, R.drawable.pup_deco_2_d, R.drawable.pup_deco_3_d) else listOf(R.drawable.pup_deco_1, R.drawable.pup_deco_2, R.drawable.pup_deco_3)).forEach {
                            Spacer(Modifier.width(8.dp))
                            Image(painterResource(it), null, Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)).alpha(.85f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 8.dp, bottom = 12.dp).navigationBarsPadding()) {
            PrimaryButton(stringResource(R.string.pup_feed), showSoon, arrow = false, modifier = Modifier.fillMaxWidth().testTag("pup-feed"))
        }
    }
}

/** 방 그림 위에 강아지(Lottie) — 가만히 있을 때 숨쉬기, 누르면 쓰다듬기 한 번 */
@Composable
private fun PupRoom(stage: PupStage) {
    BoxWithConstraints(Modifier.fillMaxWidth().aspectRatio(720f / CROP_H)) {
        Image(painterResource(if (StepUpColors.dark) stage.roomNight else stage.room), null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
        var touches by remember(stage.level) { mutableIntStateOf(0) }
        val idle by rememberLottieComposition(LottieCompositionSpec.Asset("pup/lv${stage.level}/idle_loop.json"))
        val touch by rememberLottieComposition(LottieCompositionSpec.Asset("pup/lv${stage.level}/touch.json"))
        val touching = touches > 0
        val progress by animateLottieCompositionAsState(
            if (touching) touch else idle,
            iterations = if (touching) 1 else LottieConstants.IterateForever,
            restartOnPlay = true,
        )
        if (touching && progress >= 1f) touches = 0
        // 강아지 크기 — 방 높이 × 비율이 Lottie 안 강아지 키가 되도록 1080 정사각을 키운다
        val scale = maxHeight * stage.height / stage.artHeight
        val box = scale * LOTTIE_SIZE
        val left = maxWidth * stage.footX - scale * LOTTIE_FOOT_X
        val top = maxHeight * stage.footY - scale * LOTTIE_FOOT_Y
        LottieAnimation(
            composition = if (touching) touch else idle,
            progress = { progress },
            modifier = Modifier.offset(left, top).size(box)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { touches++ }
                .testTag("pup-dog"),
        )
    }
}

@Composable
private fun PupCard(color: Color, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(color).padding(20.dp)) { content() }
}

@Composable
private fun PupStat(@StringRes label: Int, modifier: Modifier) {
    val c = StepUpUi.colors
    Column(modifier.padding(end = 12.dp)) {
        Text(stringResource(label), color = c.secondaryLabel, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(6.dp))
        Text("—", color = c.label, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        PupBar(0f)
    }
}

@Composable
private fun PupBar(fraction: Float) {
    val c = StepUpUi.colors
    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.fill)) {
        if (fraction > 0f) Box(Modifier.fillMaxWidth(fraction).height(6.dp).clip(RoundedCornerShape(3.dp)).background(c.primary))
    }
}

/** 러닝 홈 · 내 정보의 한 줄 — "스댕이 키우기 · 곧 열려요 ›" */
@Composable
fun PupEntryRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = StepUpUi.colors
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).feedbackClickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 12.dp).testTag("pup-entry"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Pets, null, tint = c.label, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(stringResource(R.string.pup_title), color = c.label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.pup_soon), color = c.secondaryLabel, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.secondaryLabel, modifier = Modifier.size(20.dp))
    }
}
