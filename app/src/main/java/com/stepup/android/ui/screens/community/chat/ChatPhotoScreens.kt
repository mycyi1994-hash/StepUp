package com.stepup.android.ui.screens.community.chat

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.filled.Check
import com.stepup.android.ui.components.RunSpinner
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.ui.screens.community.crew.rememberDraftPhoto

/**
 * 26 사진 전송 전 확인 — 고른 사진과 함께 보낼 말(선택). "사진 보내기"를 눌러야 보낸다. 실패하면 42(사진 · 설명 그대로,
 * 같은 요청 키로 다시). 뒤로 가면 고른 사진만 버리고 대화의 쓰던 글은 그대로다.
 */
@Composable
fun ChatPhotoScreen(viewModel: ChatPhotoViewModel, onBack: () -> Unit, onSent: () -> Unit, onEnded: () -> Unit) {
    val ink = blueInk()
    val caption by viewModel.caption.collectAsStateWithLifecycle()
    val sending by viewModel.sending.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    val sent by viewModel.sent.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    val photo = viewModel.photo
    LaunchedEffect(sent) { if (sent) onSent() }
    LaunchedEffect(ended) { if (ended) onEnded() }
    // 앱이 다시 시작돼 고른 사진이 없다 — 대화로 돌아간다
    LaunchedEffect(photo) { if (photo == null) onBack() }
    val cancel: () -> Unit = {
        if (!sending) {
            ChatPhotoDraft.clear(viewModel.crewId)
            onBack()
        }
    }
    BackHandler(onBack = cancel)
    val image = rememberDraftPhoto(photo?.file?.path)

    BluePage(Modifier.imePadding().testTag("chat-photo-preview")) {
        BlueTopBar(stringResource(R.string.chat_preview_bar), cancel)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter)) {
            Spacer(Modifier.height(20.dp))
            val ratio = if (photo != null && photo.height > 0) (photo.width.toFloat() / photo.height).coerceIn(0.75f, 1.6f) else 1f
            Box(
                Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(20.dp)).background(ink.card).testTag("chat-photo-image"),
                contentAlignment = Alignment.Center,
            ) {
                if (image != null) Image(image, stringResource(R.string.chat_photo), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else RunSpinner(Modifier.size(24.dp))
            }
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.chat_preview_count), style = blueText(15.sp, ink.secondary, FontWeight.Medium))
            Spacer(Modifier.height(26.dp))
            BlueFieldLabel(stringResource(R.string.chat_preview_caption))
            Spacer(Modifier.height(10.dp))
            BlueTextField(
                caption, viewModel::setCaption, Modifier.testTag("chat-photo-caption"),
                placeholder = stringResource(R.string.chat_preview_caption_hint), singleLine = false,
            )
            Spacer(Modifier.height(32.dp))
        }
        BlueBottomBar {
            BlueButton(stringResource(R.string.chat_preview_send), viewModel::send, Modifier.testTag("chat-photo-send"), busy = sending && !failed)
        }
    }
    if (failed) {
        BlueSheet(
            stringResource(R.string.chat_photo_error_title), viewModel::closeFailed, Modifier.testTag("chat-photo-failed"),
            dismissible = !sending, centered = true,
        ) {
            Spacer(Modifier.height(12.dp))
            BlueSheetBody(stringResource(R.string.chat_photo_error_body))
            Spacer(Modifier.height(26.dp))
            BlueButton(stringResource(R.string.chat_photo_retry), viewModel::send, Modifier.testTag("chat-photo-retry"), busy = sending)
            Spacer(Modifier.height(12.dp))
            BlueButton(
                stringResource(R.string.chat_photo_back), viewModel::closeFailed, Modifier.testTag("chat-photo-back"),
                BlueKind.SECONDARY, enabled = !sending,
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * 41 받은 사진 크게 보기 — 보내기 버튼이 없다(26 과 다르다). 방에 들어갈 수 있을 때만 서버가 사진을 준다. 지웠거나 숨긴
 * 사진이면 "지금은 볼 수 없는 사진이에요".
 */
@Composable
fun ChatViewerScreen(viewModel: ChatViewerViewModel, onBack: () -> Unit, onRoom: () -> Unit, onEnded: () -> Unit) {
    val ink = blueInk()
    val image by viewModel.image.collectAsStateWithLifecycle()
    val ended by viewModel.ended.collectAsStateWithLifecycle()
    LaunchedEffect(ended) { if (ended) onEnded() }
    val message = viewModel.message
    val gone = message != null && !message.visible
    BluePage(Modifier.testTag("chat-viewer")) {
        BlueTopBar(stringResource(R.string.chat_viewer_bar), onBack)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(20.dp))
            val bitmap = (image as? ChatLoad.Ready)?.value?.takeIf { !gone }
            val shown = remember(bitmap) { bitmap?.asImageBitmap() }
            val ratio = if (bitmap != null && bitmap.height > 0) (bitmap.width.toFloat() / bitmap.height).coerceIn(0.6f, 1.8f) else 1f
            Box(
                Modifier.fillMaxWidth().aspectRatio(ratio).clip(RoundedCornerShape(20.dp)).background(ink.card).testTag("chat-viewer-image"),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    shown != null -> Image(shown, stringResource(R.string.chat_photo), Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    gone || image is ChatLoad.Failed -> Text(
                        stringResource(R.string.chat_viewer_gone), style = blueText(16.sp, ink.secondary, FontWeight.Medium), textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp).testTag("chat-viewer-gone"),
                    )
                    else -> RunSpinner(Modifier.size(24.dp))
                }
            }
            if (message != null && !gone) {
                Spacer(Modifier.height(22.dp))
                Text(
                    stringResource(
                        R.string.chat_viewer_meta,
                        message.authorName?.takeIf { it.isNotBlank() } ?: stringResource(R.string.chat_unknown_name),
                        "${chatDate(message.createdAt)} ${chatTime(message.createdAt)}",
                    ),
                    style = blueText(15.sp, ink.secondary, FontWeight.Medium), textAlign = TextAlign.Center,
                )
                message.body?.takeIf { it.isNotBlank() }?.let { body ->
                    Spacer(Modifier.height(14.dp))
                    Text(body, style = blueText(20.sp, ink.text, FontWeight.Bold, 1.45f), textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        BlueBottomBar { BlueButton(stringResource(R.string.chat_back_to_room), onRoom, Modifier.testTag("chat-viewer-room")) }
    }
}

/** 29 신고 접수 — 서버가 받은 뒤에만 */
@Composable
fun ChatReportedScreen(onBack: () -> Unit, onRoom: () -> Unit) {
    BlueResultPage(
        title = stringResource(R.string.chat_report_sent_title),
        body = stringResource(R.string.chat_report_sent_body),
        button = stringResource(R.string.chat_back_to_room),
        onButton = onRoom,
        onBack = onBack,
        modifier = Modifier.testTag("chat-reported"),
        icon = Icons.Filled.Check,
    )
}

/**
 * 32 채팅 참여 종료 — 탈퇴 · 내보내기 · 해산으로 이 방에 참여할 수 없다. 이 폰에 남은 그 방의 대화 · 사진 · 입력 글은
 * 이미 지웠다. 내보낸 크루장이 보는 35 와 같은 화면이 아니다.
 */
@Composable
fun ChatEndedScreen(onList: () -> Unit) {
    val ink = blueInk()
    BackHandler(onBack = onList)
    BluePage(Modifier.testTag("chat-ended")) {
        BlueTopBar(stringResource(R.string.chat_ended_bar), onList)
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = BlueGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(96.dp))
            BlueStateIcon(Icons.Outlined.Info)
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.chat_ended_title), style = blueText(27.sp, ink.text, FontWeight.ExtraBold, 1.3f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.chat_ended_body), style = blueText(16.sp, ink.text.copy(alpha = 0.88f), FontWeight.Medium, 1.6f),
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(32.dp))
        }
        BlueBottomBar { BlueButton(stringResource(R.string.chat_ended_button), onList, Modifier.testTag("chat-ended-list")) }
    }
}
