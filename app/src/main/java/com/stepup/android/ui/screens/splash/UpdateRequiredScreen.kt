package com.stepup.android.ui.screens.splash

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.BuildConfig
import com.stepup.android.R
import com.stepup.android.core.TestUpdates
import com.stepup.android.ui.background.Spots
import com.stepup.android.ui.background.TimeOfDay
import com.stepup.android.ui.background.rememberSpotPhoto
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.Wordmark
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import com.stepup.android.ui.theme.BrandLogoRole
import com.stepup.android.ui.theme.StepUpColors

/**
 * 업데이트가 필요해요(서버 0120 강제 업데이트) — 운영자가 정한 판보다 이 앱이 옛 판이라 서버가 요청을 받지 않는다.
 * 시작 화면과 같은 바탕(사진 · 워드마크) 위에 까닭 한 줄과 주 버튼 하나 "업데이트하기"만 둔다(다른 버튼 없음).
 * 스토어 판은 Google Play 의 이 앱 쪽으로, 테스트 APK(debug · qa)는 앱 안 새 빌드 받기로 간다([openUpdate]).
 * 이 화면이 떠 있는 동안 서버에는 몇 분에 한 번만 다시 묻는다(StepUpServer).
 */
@Composable
internal fun UpdateRequiredScreen() {
    val dark = StepUpColors.dark
    val ui = StepUpUi.colors
    val context = LocalContext.current
    val photo = rememberSpotPhoto(
        remember { Spots.byId(context, "mangwon") },
        time = if (dark) TimeOfDay.DUSK else TimeOfDay.DAY,
    )
    val ground = if (dark) Color(0xFF070B14) else Color.White
    Box(Modifier.fillMaxSize().background(ground).testTag("update-required")) {
        if (photo != null) {
            Image(
                photo.painter, null, contentScale = ContentScale.Crop, colorFilter = photo.colorFilter,
                modifier = if (dark) Modifier.fillMaxSize() else Modifier.fillMaxWidth().fillMaxHeight(0.5f),
            )
        }
        Box(
            Modifier.fillMaxSize().background(
                if (dark) Brush.verticalGradient(0f to Color.Black.copy(alpha = .08f), .55f to Color.Black.copy(alpha = .18f), 1f to ground.copy(alpha = .78f))
                else Brush.verticalGradient(0f to Color.White.copy(alpha = .08f), .24f to Color.White.copy(alpha = .22f), .40f to Color.White.copy(alpha = .9f), .46f to Color.White, 1f to Color.White),
            ),
        )
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val room = maxHeight
                Column(
                    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).heightIn(min = room),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(room * 0.44f))
                    Wordmark(role = BrandLogoRole.Launch)
                    Spacer(Modifier.height(40.dp))
                    Text(
                        stringResource(R.string.tb_update_title),
                        style = StepUpUi.type.section.copy(fontSize = 24.sp), color = if (dark) Color.White else ui.label,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.testTag("update-required-title").semantics { heading(); liveRegion = LiveRegionMode.Polite },
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        stringResource(R.string.tb_update_body),
                        style = StepUpUi.type.body, color = if (dark) Color.White.copy(alpha = .8f) else ui.secondaryLabel,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.weight(1f))
                }
            }
            BluePlainButton(
                stringResource(R.string.tb_update_action), { openUpdate(context) },
                Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(bottom = 16.dp).testTag("update-required-go"),
            )
        }
    }
}

/** 업데이트하러 — 테스트 APK 는 앱 안 새 빌드 받기, 아니면 Google Play 의 이 앱(Play 앱이 없으면 웹) */
internal fun openUpdate(context: Context) {
    if (TestUpdates.requestUpdate(context)) return
    openStoreListing(context)
}

/** Google Play 의 이 앱 쪽 — Play 앱이 있으면 그 앱으로, 없으면 브라우저로 */
internal fun openStoreListing(context: Context) {
    val id = BuildConfig.APPLICATION_ID
    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(market)
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(storeWebUrl(id))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    } catch (_: SecurityException) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(storeWebUrl(id))).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }
}

/** Play 앱이 없을 때 여는 웹 주소 */
internal fun storeWebUrl(applicationId: String): String = "https://play.google.com/store/apps/details?id=$applicationId"
