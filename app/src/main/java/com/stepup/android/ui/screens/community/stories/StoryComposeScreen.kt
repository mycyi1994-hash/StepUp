package com.stepup.android.ui.screens.community.stories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryText
import com.stepup.android.ui.components.SignInAgainButton
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Carbon
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.VoltText

/** 글쓰기 한 화면 안의 단계 — 쓰기 · 장소 선택 · 장소 확인. 단계를 오가도 본문이 그대로다 */
private enum class ComposeStep { WRITE, PICK, CONFIRM }

/**
 * 동네 이야기 쓰기 · 고치기.
 *
 * 본문 한 칸(1,000자)과 장소. 첫 줄이 목록의 제목이 된다. 장소가 없거나 본문이 공백뿐이면 올릴 수 없다.
 * 내용이 바뀐 채 나가려 하면 임시저장 · 계속 쓰기 · 삭제하고 나가기를 묻는다.
 */
@Composable
fun StoryComposeScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    viewModel: StoryComposeViewModel = viewModel(factory = StoryComposeViewModel.Factory),
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val place by viewModel.place.collectAsStateWithLifecycle()
    val editingId by viewModel.editingId.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val origin = rememberStoryOrigin()

    var step by rememberSaveable { mutableStateOf(ComposeStep.WRITE) }
    var candidate by rememberSaveable(stateSaver = StoryPlaceSaver) { mutableStateOf<StoryPlace?>(null) }
    var pickOnMap by rememberSaveable { mutableStateOf(false) }
    var leaving by rememberSaveable { mutableStateOf(false) }

    val leave = {
        when {
            submitting -> Unit
            viewModel.changed() -> leaving = true
            else -> onBack()
        }
    }
    BackHandler(enabled = step == ComposeStep.WRITE && !leaving) { leave() }
    BackHandler(enabled = step == ComposeStep.PICK) { step = ComposeStep.WRITE }
    BackHandler(enabled = step == ComposeStep.CONFIRM) { step = ComposeStep.PICK }

    when (step) {
        ComposeStep.PICK -> StoryPlacePicker(
            origin = origin,
            onBack = { step = ComposeStep.WRITE },
            onPick = { candidate = it; pickOnMap = false; step = ComposeStep.CONFIRM },
            onPickOnMap = { candidate = null; pickOnMap = true; step = ComposeStep.CONFIRM },
        )
        ComposeStep.CONFIRM -> StoryPlaceConfirm(
            initial = candidate,
            pickOnMap = pickOnMap,
            origin = origin,
            onBack = { step = ComposeStep.PICK },
            onChoose = { chosen -> viewModel.setPlace(chosen); step = ComposeStep.WRITE },
        )
        ComposeStep.WRITE -> Column(Modifier.fillMaxSize().imePadding().testTag("story-compose")) {
            val edit = editingId > 0
            StoryHeader(stringResource(if (edit) R.string.story_edit_title else R.string.story_write_title), onBack = { leave() })
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = StoryFormGutter),
            ) {
                Spacer(Modifier.height(12.dp))
                Text(stringResource(if (edit) R.string.story_edit_headline else R.string.story_write_headline),
                    color = Snow, fontSize = 25.sp, fontWeight = FontWeight.SemiBold, lineHeight = 32.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.story_write_sub), color = Silver, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                // 장소 한 줄 — 모르면 지어내지 않고 "장소를 골라 주세요"
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (place != null) VoltText else Slate,
                        modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        place?.name ?: stringResource(R.string.story_place_needed),
                        color = if (place != null) VoltText else Slate, fontSize = 13.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("story-compose-place"),
                    )
                    StoryLink(
                        stringResource(if (place != null) R.string.story_place_change else R.string.story_place_pick),
                        { step = ComposeStep.PICK },
                        Modifier.testTag("story-compose-pick"),
                    )
                }
                Spacer(Modifier.height(6.dp))
                val shape = RoundedCornerShape(14.dp)
                val large = LocalDensity.current.fontScale > 1.3f
                Box(
                    Modifier.fillMaxWidth().heightIn(min = if (large) 180.dp else 240.dp).clip(shape).background(Carbon)
                        .border(1.dp, if (text.isNotEmpty()) VoltText.copy(alpha = 0.45f) else Edge, shape)
                        .padding(16.dp),
                ) {
                    if (text.isEmpty()) Text(stringResource(R.string.story_write_hint), color = Slate, fontSize = 17.sp)
                    BasicTextField(
                        value = text,
                        onValueChange = viewModel::setText,
                        enabled = ready && !submitting,
                        textStyle = TextStyle(color = Snow, fontSize = 17.sp, lineHeight = 26.sp),
                        cursorBrush = SolidColor(VoltText),
                        modifier = Modifier.fillMaxWidth().testTag("story-compose-text"),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.story_write_place_note), color = Slate, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(
                        "%,d / %,d".format(StoryText.length(text), StoryText.MAX_CHARS),
                        color = Slate, fontSize = 11.sp, modifier = Modifier.testTag("story-compose-count"),
                    )
                }
                error?.let { kind ->
                    Spacer(Modifier.height(14.dp))
                    StoryPublishErrorBox(kind)
                }
                Spacer(Modifier.height(18.dp))
            }
            StoryButton(
                text = stringResource(
                    when {
                        submitting -> R.string.story_publishing
                        error != null -> R.string.story_publish_retry
                        edit -> R.string.story_edit_done
                        else -> R.string.story_publish
                    },
                ),
                onClick = { viewModel.submit(onDone) },
                enabled = ready && StoryText.canPost(text) && place != null,
                busy = submitting,
                modifier = Modifier.padding(start = StoryFormGutter, end = StoryFormGutter, bottom = 12.dp)
                    .testTag("story-compose-submit"),
            )
        }
    }

    if (leaving) {
        StorySheet(stringResource(R.string.story_leave_title), onDismiss = { leaving = false }) {
            Text(stringResource(R.string.story_leave_body), color = Silver, fontSize = 13.sp, modifier = Modifier.padding(bottom = 16.dp))
            StoryButton(stringResource(R.string.story_leave_save), { viewModel.saveDraft { leaving = false; onBack() } },
                Modifier.testTag("story-leave-save"))
            Spacer(Modifier.height(10.dp))
            StoryButton(stringResource(R.string.story_leave_keep), { leaving = false }, style = StoryButtonStyle.SECONDARY)
            Spacer(Modifier.height(4.dp))
            StoryTextButton(stringResource(R.string.story_leave_discard), { viewModel.discard { leaving = false; onBack() } },
                Modifier.align(Alignment.CenterHorizontally).testTag("story-leave-discard"), color = Alert)
        }
    }
}

/** 올리지 못했을 때 — 쓴 내용은 그대로 있다고 말한다 */
@Composable
private fun StoryPublishErrorBox(kind: StoryPublishError) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(Alert.copy(alpha = 0.10f)).border(1.dp, Alert.copy(alpha = 0.35f), shape)
            .padding(14.dp).testTag("story-publish-error"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = Alert, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(if (kind == StoryPublishError.SIGN_IN) R.string.story_publish_sign_in else R.string.story_publish_failed),
                color = Snow, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.story_publish_kept), color = Silver, fontSize = 12.sp, modifier = Modifier.padding(start = 24.dp))
        if (kind == StoryPublishError.SIGN_IN) {
            Spacer(Modifier.height(8.dp))
            SignInAgainButton()
        }
    }
}
