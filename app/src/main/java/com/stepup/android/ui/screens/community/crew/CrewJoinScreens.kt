package com.stepup.android.ui.screens.community.crew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.CrewApplied
import com.stepup.android.domain.CrewApplication
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewPhrase
import com.stepup.android.domain.CrewProblem

/**
 * 14 가입 신청 작성 · 17 전송 실패 · 24 신청 중 모집 상태 변경.
 * 문구 버튼은 고르고 풀 수 있고, 직접 쓴 한마디를 덮지 않는다(둘 다 따로 보낸다). 한마디가 비어도 보낼 수 있다.
 * 보내는 동안 버튼은 도는 표시와 함께 다시 누를 수 없고, 실패하면 같은 입력 · 같은 요청 키로 다시 보낸다.
 */
@Composable
fun CrewJoinScreen(
    viewModel: CrewScreenViewModel,
    onBack: () -> Unit,
    onPending: (Long) -> Unit,
    onJoinedNow: () -> Unit,
    onDecided: (Long) -> Unit,
    onShowCurrent: () -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val phrases by viewModel.phrases.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var unavailable by rememberSaveable { mutableStateOf(false) }
    val sending = op.op == CrewOp.APPLY && op.running
    val failed = op.op == CrewOp.APPLY && op.problem != null && !unavailable

    LaunchedEffect(op) {
        if (op.op != CrewOp.APPLY) return@LaunchedEffect
        when {
            op.done -> {
                viewModel.consumeOp()
                when (val applied = op.applied()) {
                    is CrewApplied.Pending -> onPending(applied.applicationId)
                    CrewApplied.Member -> onJoinedNow()
                    is CrewApplied.Declined -> onDecided(applied.applicationId)
                    CrewApplied.Canceled, null -> Unit
                }
            }
            op.problem == CrewProblem.CREW_CLOSED || op.problem == CrewProblem.CREW_FULL || op.problem == CrewProblem.CREW_MISSING ->
                unavailable = true
        }
    }

    CrewPage(Modifier.imePadding().testTag("crew-join")) {
        CrewTopBar(stringResource(R.string.crew_join_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            card?.let { CrewIdentityStrip(it) }
            Spacer(Modifier.height(34.dp))
            Text(stringResource(R.string.crew_join_question), color = ink.text, fontSize = 24.sp, lineHeight = 31.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.crew_join_question_sub), color = ink.secondary, fontSize = 13.5.sp)
            Spacer(Modifier.height(20.dp))
            CrewPhrase.entries.chunked(2).forEach { row ->
                CrewChoiceRow {
                    row.forEach { phrase ->
                        CrewChoice(
                            words.phrase(phrase), phrase.name in phrases, { viewModel.togglePhrase(phrase) },
                            Modifier.weight(1f).testTag("crew-join-phrase-${phrase.name}"), enabled = !sending,
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            CrewFieldLabel(stringResource(R.string.crew_join_message_label))
            Spacer(Modifier.height(12.dp))
            CrewTextField(
                message, viewModel::setMessage, Modifier.testTag("crew-join-message"),
                placeholder = stringResource(R.string.crew_join_message_hint), singleLine = false, minHeight = 137.dp,
            )
            card?.let { crew ->
                val where = listOfNotNull(crew.area.takeIf { it.isNotBlank() }, crew.schedule.takeIf { !it.none }?.let(words::scheduleLong))
                    .joinToString(" · ")
                val once = words.onceLine(crew)
                if (where.isNotEmpty() || once != null) {
                    Spacer(Modifier.height(34.dp))
                    if (where.isNotEmpty()) Text(where, color = ink.secondary, fontSize = 13.5.sp, lineHeight = 25.sp)
                    if (once != null) Text(once, color = ink.secondary, fontSize = 13.5.sp, lineHeight = 25.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar {
            // 17 보내지 못함 — 작은 화면에서도 보이게 본문 끝이 아니라 버튼 바로 위에 둔다
            if (failed) {
                Text(
                    stringResource(if (op.problem == CrewProblem.SIGN_IN) R.string.crew_problem_signin else R.string.crew_join_failed),
                    color = ink.warn, fontSize = 13.5.sp, lineHeight = 21.sp,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).testTag("crew-join-error"),
                )
            }
            CrewButton(
                stringResource(if (failed) R.string.crew_join_retry else R.string.crew_join_send),
                { viewModel.apply() },
                Modifier.testTag("crew-join-send"),
                busy = sending,
            )
        }
    }

    if (unavailable) {
        CrewUnavailableSheet(
            onShow = {
                unavailable = false
                viewModel.consumeOp()
                viewModel.reload()
                onShowCurrent()
            },
            onDismiss = { unavailable = false; viewModel.consumeOp() },
        )
    }
}

/**
 * 15 신청 완료 · 확인 대기(보낸 내용과 취소) · 16 신청 취소 확인. 열 때마다 신청서를 다시 읽는다 — 그 사이 승인 ·
 * 미승인됐으면 결과(18 · 19)로 바꿔 보인다(별도 푸시 없이 앱 안에서).
 */
@Composable
fun CrewPendingScreen(
    viewModel: CrewScreenViewModel,
    applicationId: Long,
    onBack: () -> Unit,
    onBackToCrew: () -> Unit,
    onResult: (Long) -> Unit,
) {
    val ink = crewInk()
    val words = rememberCrewWords()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val application by viewModel.application.collectAsStateWithLifecycle()
    val op by viewModel.op.collectAsStateWithLifecycle()
    var cancelSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(applicationId) { viewModel.loadApplication(applicationId) }
    val loaded = (application as? CrewLoad.Ready)?.value
    LaunchedEffect(loaded?.status) {
        when (loaded?.status) {
            CrewApplicationStatus.APPROVED, CrewApplicationStatus.DECLINED -> onResult(applicationId)
            CrewApplicationStatus.CANCELED -> onBackToCrew()
            else -> Unit
        }
    }
    LaunchedEffect(op) {
        if (op.op == CrewOp.CANCEL && op.done) {
            cancelSheet = false
            viewModel.consumeOp()
            when (op.cancelStatus()) {
                CrewApplicationStatus.APPROVED, CrewApplicationStatus.DECLINED -> onResult(applicationId)
                else -> onBackToCrew()
            }
        }
    }

    CrewPage(Modifier.testTag("crew-pending")) {
        CrewTopBar(stringResource(R.string.crew_pending_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(18.dp))
            card?.let { CrewIdentityStrip(it) }
            Spacer(Modifier.height(50.dp))
            Text(stringResource(R.string.crew_pending_heading), color = ink.text, fontSize = 26.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.crew_pending_body, card?.name.orEmpty()), color = ink.secondary, fontSize = 14.sp, lineHeight = 25.sp,
            )
            Spacer(Modifier.height(64.dp))
            when (val state = application) {
                is CrewLoad.Ready -> CrewSentMessage(state.value, words)
                is CrewLoad.Failed -> Text(stringResource(R.string.crew_pending_load_failed), color = ink.warn, fontSize = 13.sp)
                CrewLoad.Loading -> CrewSkeletonBox(Modifier.fillMaxWidth().height(110.dp), 16.dp)
            }
            loaded?.let {
                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.crew_pending_date, words.date(it.createdAt)), color = ink.secondary, fontSize = 12.5.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar {
            CrewButton(
                stringResource(R.string.crew_pending_cancel), { cancelSheet = true }, Modifier.testTag("crew-pending-cancel"),
                CrewButtonKind.SECONDARY, enabled = loaded?.status == CrewApplicationStatus.PENDING,
            )
            Spacer(Modifier.height(18.dp))
            CrewButton(stringResource(R.string.crew_pending_back), onBackToCrew, Modifier.testTag("crew-pending-back"))
        }
    }

    if (cancelSheet) {
        CrewConfirmSheet(
            title = stringResource(R.string.crew_cancel_title),
            body = stringResource(R.string.crew_cancel_body),
            confirm = stringResource(R.string.crew_pending_cancel),
            busy = op.op == CrewOp.CANCEL && op.running,
            error = if (op.op == CrewOp.CANCEL && op.problem != null) stringResource(R.string.crew_cancel_failed) else null,
            tag = "crew-cancel",
            onConfirm = { viewModel.cancel(applicationId) },
            onDismiss = { if (!op.running) { cancelSheet = false; if (op.op == CrewOp.CANCEL) viewModel.consumeOp() } },
        )
    }
}

/** 보낸 한마디 — 고른 문구(푸른 글자) · 직접 쓴 글(첫 줄 굵게) */
@Composable
internal fun CrewSentMessage(application: CrewApplication, words: CrewWords, label: String? = null) {
    val ink = crewInk()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(ink.card).padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("crew-sent-message"),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label ?: stringResource(R.string.crew_pending_sent_label), color = ink.secondary, fontSize = 12.5.sp)
        if (application.phrases.isNotEmpty()) {
            Text(application.phrases.joinToString(" · ") { words.phrase(it) }, color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        val lines = application.message.lines()
        if (application.message.isBlank()) {
            if (application.phrases.isEmpty()) Text(stringResource(R.string.crew_pending_no_message), color = ink.secondary, fontSize = 14.sp)
        } else {
            Text(lines.first(), color = ink.text, fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold)
            if (lines.size > 1) Text(lines.drop(1).joinToString("\n"), color = ink.text, fontSize = 15.sp, lineHeight = 24.sp)
        }
    }
}

/** 18 가입 승인 결과 · 19 미승인 결과 — 볼 때 "봤음"을 남긴다 */
@Composable
fun CrewResultScreen(
    viewModel: CrewScreenViewModel,
    applicationId: Long,
    onBack: () -> Unit,
    onOpenCrew: () -> Unit,
    onOtherCrews: () -> Unit,
    /** 승인된 뒤에만 — 그 크루의 채팅방(신청 대기자는 들어가지 않는다) */
    onOpenChat: () -> Unit = {},
) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val application by viewModel.application.collectAsStateWithLifecycle()
    LaunchedEffect(applicationId) { viewModel.loadApplication(applicationId) }
    val status = (application as? CrewLoad.Ready)?.value?.status
    LaunchedEffect(status) {
        if (status == CrewApplicationStatus.APPROVED || status == CrewApplicationStatus.DECLINED) viewModel.markSeen(applicationId)
    }
    val name = card?.name.orEmpty()
    when (status) {
        CrewApplicationStatus.DECLINED -> CrewResultPage(
            title = stringResource(R.string.crew_declined_title),
            body = stringResource(R.string.crew_declined_body),
            button = stringResource(R.string.crew_other_crews),
            onButton = onOtherCrews,
            onBack = onBack,
            info = true,
            modifier = Modifier.testTag("crew-result-declined"),
        )
        CrewApplicationStatus.APPROVED -> CrewResultPage(
            title = stringResource(R.string.crew_approved_title, withParticle(name, "과", "와")),
            body = stringResource(R.string.crew_approved_body),
            button = stringResource(R.string.crew_view),
            onButton = onOpenCrew,
            onBack = onBack,
            modifier = Modifier.testTag("crew-result-approved"),
            secondary = stringResource(R.string.chat_open),
            onSecondary = onOpenChat,
        )
        else -> CrewPage(Modifier.testTag("crew-result-loading")) {
            CrewTopBar(stringResource(R.string.crew_notice_title), onBack)
            if (application is CrewLoad.Failed) {
                Spacer(Modifier.height(140.dp))
                CrewEmptyState(icon = {}, title = stringResource(R.string.crew_pending_load_failed), body = "") {
                    CrewButton(stringResource(R.string.crew_list_reload), { viewModel.loadApplication(applicationId) })
                }
            }
        }
    }
}

/** 바로 가입 크루에 들어갔다 — 18 과 같은 모양 */
@Composable
fun CrewJoinedNowScreen(card: CrewCard?, onBack: () -> Unit, onOpenCrew: () -> Unit, onOpenChat: () -> Unit = {}) {
    val name = card?.name.orEmpty()
    CrewResultPage(
        title = stringResource(R.string.crew_approved_title, withParticle(name, "과", "와")),
        body = stringResource(R.string.crew_joined_now_body),
        button = stringResource(R.string.crew_view),
        onButton = onOpenCrew,
        onBack = onBack,
        modifier = Modifier.testTag("crew-result-joined"),
        secondary = stringResource(R.string.chat_open),
        onSecondary = onOpenChat,
    )
}

/** 65 신고 접수 완료 — 서버가 받은 뒤에만 */
@Composable
fun CrewReportedScreen(onBack: () -> Unit, onDone: () -> Unit) {
    CrewResultPage(
        title = stringResource(R.string.crew_reported_title),
        body = stringResource(R.string.crew_reported_body),
        button = stringResource(R.string.crew_ok),
        onButton = onDone,
        onBack = onBack,
        modifier = Modifier.testTag("crew-reported"),
    )
}

/**
 * 이름에 한국어 조사를 붙인다("퇴근런과") — 앱이 한국어일 때만. 다른 언어는 이름 그대로(문자열 쪽이 문장을 짠다).
 */
@Composable
internal fun withParticle(word: String, withFinal: String, without: String): String {
    val korean = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]?.language == "ko"
    return if (korean) word + koreanParticle(word, withFinal, without) else word
}

/** 한국어 조사 — 받침이 있으면 [withFinal], 없으면 [without] */
internal fun koreanParticle(word: String, withFinal: String, without: String): String {
    val last = word.trim().lastOrNull() ?: return without
    if (last in '가'..'힣') return if ((last - '가') % 28 != 0) withFinal else without
    // 숫자 · 영문 끝 — 읽는 소리로 대강(0 1 3 6 7 8 · l m n r 은 받침)
    return if (last.lowercaseChar() in "013678lmnr") withFinal else without
}
