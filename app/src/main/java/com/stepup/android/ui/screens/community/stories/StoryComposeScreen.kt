package com.stepup.android.ui.screens.community.stories

import android.content.Context
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.domain.StoryComposeRules
import com.stepup.android.domain.StoryOrigin
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.StoryPlaceSource
import com.stepup.android.domain.StoryRecordCard
import com.stepup.android.domain.StoryRunRules
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.SignInAgainButton
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.theme.Alert
import com.stepup.android.ui.theme.Silver

/** 글쓰기 한 화면 안의 단계 — 쓰기 · 장소 선택 · 장소 확인. 단계를 오가도 본문 · 첨부가 그대로다 */
private enum class ComposeStep { WRITE, PICK, CONFIRM }

/**
 * 동네 이야기 쓰기 · 고치기 — 러닝 이야기(2026-09-28 쉬운 글쓰기 상황별 시안).
 *
 * 위부터 기록 칸(최근 3일 러닝 첨부 · 상황별 안내) · 장소(장소 줄과 코스 주변 · 내 주변 · 최근 장소 · 직접 검색) ·
 * 한 줄 이야기 버튼 · 본문 · 안내 한 줄 · 올리기. 첫 줄이 목록의 제목이 된다. 공개 장소가 있고 본문(또는 유효한
 * 러닝)이 있어야 올릴 수 있다. 내용이 바뀐 채 나가려 하면 임시저장 · 계속 쓰기 · 삭제하고 나가기를 묻는다.
 */
@Composable
fun StoryComposeScreen(
    onBack: () -> Unit,
    onDone: () -> Unit,
    /** 첫 러닝 시작 · 러닝 시작 · 러닝 화면으로 — 쓰던 글을 저장한 뒤 부른다 */
    onStartRun: () -> Unit = {},
    /** 지난 기록 보기 — 쓰던 글을 저장한 뒤 부른다 */
    onOpenRecords: () -> Unit = {},
    viewModel: StoryComposeViewModel = viewModel(factory = StoryComposeViewModel.Factory),
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val place by viewModel.place.collectAsStateWithLifecycle()
    val placeSource by viewModel.placeSource.collectAsStateWithLifecycle()
    val editingId by viewModel.editingId.collectAsStateWithLifecycle()
    val submitting by viewModel.submitting.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val card by viewModel.card.collectAsStateWithLifecycle()
    val records by viewModel.records.collectAsStateWithLifecycle()
    val phrase by viewModel.phrase.collectAsStateWithLifecycle()
    val autoFill by viewModel.autoFill.collectAsStateWithLifecycle()
    val recent by viewModel.recentPlaces.collectAsStateWithLifecycle()
    val origin = rememberStoryOrigin()
    val words = rememberStoryWords()
    val context = LocalContext.current

    var step by rememberSaveable { mutableStateOf(ComposeStep.WRITE) }
    var pickMode by rememberSaveable { mutableStateOf(StoryPickMode.NEARBY) }
    var candidate by rememberSaveable(stateSaver = StoryPlaceSaver) { mutableStateOf<StoryPlace?>(null) }
    var pickOnMap by rememberSaveable { mutableStateOf(false) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    var showRuns by rememberSaveable { mutableStateOf(false) }
    var showNoLocation by rememberSaveable { mutableStateOf(false) }

    // 내 주변 — 위치 권한과 위치 기능을 확인한다. 쓸 수 없으면 검색 · 지도로 이어 가게 한다
    var locationAllowed by remember { mutableStateOf(StepPermissions.hasLocation(context)) }
    LifecycleResumeEffect(Unit) {
        locationAllowed = StepPermissions.hasLocation(context)
        viewModel.onReturn()
        onPauseOrDispose { }
    }
    // 내 위치는 "내 주변"으로 장소를 고를 때만 묻는다(글쓰기를 열었다고 위치를 쓰지 않는다)
    val here = rememberCurrentLocation(enabled = locationAllowed && step == ComposeStep.PICK && pickMode == StoryPickMode.NEARBY)

    val attached = StoryComposeRules.postableRun(card)
    val validRun = attached?.run
    // 기록 · 장소 · 고른 버튼으로 만든 문장 — 사용자가 아직 고치지 않은 앱의 문장만 이것으로 바꾼다
    val chosenPhrase = phrase
    val candidateText = when {
        chosenPhrase == null -> words.starter(null, validRun, place)
        chosenPhrase.usesRun -> words.starter(chosenPhrase, validRun, place)
        else -> ""
    }
    LaunchedEffect(candidateText, autoFill) { viewModel.syncAuto(candidateText) }
    val fallback = if (validRun != null) words.starter(null, validRun, place) else ""

    val leave = {
        when {
            submitting -> Unit
            viewModel.changed() -> leaving = true
            else -> onBack()
        }
    }
    val openPicker = { mode: StoryPickMode ->
        pickMode = mode
        step = ComposeStep.PICK
    }
    // 러닝 · 지난 기록 화면에 다녀와도 쓰던 글이 남게 먼저 저장한다(목록에 "임시저장했어요"는 띄우지 않는다)
    val leaveFor = { go: () -> Unit -> viewModel.saveDraft(notify = false) { go() } }

    BackHandler(enabled = step == ComposeStep.WRITE && !leaving) { leave() }
    BackHandler(enabled = step == ComposeStep.PICK) { step = ComposeStep.WRITE }
    BackHandler(enabled = step == ComposeStep.CONFIRM) { step = ComposeStep.PICK }

    when (step) {
        ComposeStep.PICK -> StoryPlacePicker(
            origin = if (pickMode == StoryPickMode.NEARBY && here != null) StoryOrigin(here, "", manual = false) else origin,
            onBack = { step = ComposeStep.WRITE },
            onPick = { candidate = it; pickOnMap = false; step = ComposeStep.CONFIRM },
            onPickOnMap = { candidate = null; pickOnMap = true; step = ComposeStep.CONFIRM },
            mode = pickMode,
            course = validRun?.route.orEmpty(),
            recent = recent,
        )
        ComposeStep.CONFIRM -> StoryPlaceConfirm(
            initial = candidate,
            pickOnMap = pickOnMap,
            origin = origin,
            onBack = { step = ComposeStep.PICK },
            onChoose = { chosen ->
                viewModel.setPlace(chosen, pickMode.source.takeUnless { pickOnMap })
                step = ComposeStep.WRITE
            },
            hint = validRun?.route.orEmpty() + recent.map { it.point },
        )
        ComposeStep.WRITE -> Column(Modifier.fillMaxSize().imePadding().testTag("story-compose")) {
            val edit = editingId > 0
            StoryHeader(stringResource(if (edit) R.string.story_compose_edit_title else R.string.story_compose_title), onBack = { leave() })
            val large = LocalDensity.current.fontScale > 1.3f
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(horizontal = ComposeGutter),
            ) {
                Spacer(Modifier.height(12.dp))
                StoryRecordCardView(
                    card = card,
                    words = words,
                    actions = StoryRecordActions(
                        onPick = { showRuns = true; viewModel.onReturn() },
                        onRemove = viewModel::removeRun,
                        onRetry = viewModel::loadRecords,
                        onStartRun = { leaveFor(onStartRun) },
                        onHistory = { leaveFor(onOpenRecords) },
                    ),
                    modifier = Modifier.testTag("story-record-card"),
                )
                Spacer(Modifier.height(15.dp))
                StoryPlaceBlock(
                    place = place,
                    sources = StoryComposeRules.placeSources(card, hasRecent = recent.isNotEmpty()),
                    selected = placeSource.takeIf { place != null },
                    onOpen = { openPicker(StoryPickMode.DEFAULT) },
                    onSource = { source ->
                        when (source) {
                            StoryPlaceSource.COURSE -> openPicker(StoryPickMode.COURSE)
                            StoryPlaceSource.RECENT -> openPicker(StoryPickMode.RECENT)
                            StoryPlaceSource.SEARCH -> openPicker(StoryPickMode.SEARCH)
                            StoryPlaceSource.NEARBY ->
                                if (locationUsable(context, locationAllowed)) openPicker(StoryPickMode.NEARBY) else showNoLocation = true
                        }
                    },
                )
                Spacer(Modifier.height(6.dp))
                StoryPhraseBlock(
                    set = StoryComposeRules.phraseSet(card),
                    selected = phrase.takeIf { text.isNotBlank() },
                    words = words,
                    onPhrase = { chosen -> viewModel.applyStarter(chosen, words.starter(chosen, validRun, place)) },
                )
                Spacer(Modifier.height(8.dp))
                StoryBodyBox(text, viewModel::setText, enabled = ready && !submitting, large = large)
                Spacer(Modifier.height(14.dp))
            }
            Column(Modifier.padding(start = ComposeGutter, end = ComposeGutter, bottom = 12.dp)) {
                val (note, warn) = composeNote(card, place != null, text, fallback, error)
                Text(
                    note, color = if (warn) composeInk().warn else composeInk().secondary, fontSize = 12.sp, lineHeight = 17.sp,
                    modifier = Modifier.fillMaxWidth().testTag(if (error != null) "story-publish-error" else "story-compose-note"),
                )
                if (error == StoryPublishError.SIGN_IN) {
                    Spacer(Modifier.height(8.dp))
                    SignInAgainButton()
                }
                Spacer(Modifier.height(12.dp))
                ComposePrimaryButton(
                    text = stringResource(
                        when {
                            submitting -> R.string.story_publishing
                            error != null -> R.string.story_publish_retry
                            edit -> R.string.story_edit_done
                            else -> R.string.story_publish
                        },
                    ),
                    onClick = { viewModel.submit(fallback, onDone) },
                    enabled = ready && StoryComposeRules.canPost(card, place != null, text, fallback),
                    busy = submitting,
                    modifier = Modifier.testTag("story-compose-submit"),
                )
            }
        }
    }

    if (showRuns) {
        StoryRunsSheet(
            records = records,
            attachedId = attached?.run?.id ?: (card as? StoryRecordCard.Expired)?.run?.id ?: (card as? StoryRecordCard.Invalid)?.run?.id,
            words = words,
            onPick = { run -> viewModel.attach(run); showRuns = false },
            onRetry = viewModel::loadRecords,
            onHistory = { showRuns = false; leaveFor(onOpenRecords) },
            onDismiss = { showRuns = false },
        )
    }

    if (showNoLocation) {
        StoryNoLocationSheet(
            onSearch = { showNoLocation = false; openPicker(StoryPickMode.SEARCH) },
            onMap = {
                showNoLocation = false
                pickMode = StoryPickMode.SEARCH
                candidate = null
                pickOnMap = true
                step = ComposeStep.CONFIRM
            },
            onSettings = {
                showNoLocation = false
                if (!StepPermissions.hasLocation(context)) ExternalIntents.openAppSettings(context)
                else ExternalIntents.openLocationSettings(context)
            },
            onDismiss = { showNoLocation = false },
        )
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

/** 지금 내 위치를 쓸 수 있는가 — 권한과 위치 기능이 모두 켜져 있어야 한다 */
private fun locationUsable(context: Context, allowed: Boolean): Boolean {
    if (!allowed) return false
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
    return LocationManagerCompat.isLocationEnabled(manager)
}

/**
 * 올리기 위 안내 한 줄 — 올리지 못했으면 그 사실, 코스를 정리해야 하면 그 사실, 빠진 것(장소 · 이야기)이 있으면
 * 그것을 먼저 말하고, 아니면 지금 상황의 안내. 두 번째 값이 true 면 주의 색.
 */
@Composable
private fun composeNote(
    card: StoryRecordCard,
    hasPlace: Boolean,
    text: String,
    fallback: String,
    error: StoryPublishError?,
): Pair<String, Boolean> {
    val words = rememberStoryWords()
    return when {
        error == StoryPublishError.SIGN_IN -> stringResource(R.string.story_note_sign_in) to true
        error == StoryPublishError.FAILED -> stringResource(R.string.story_note_failed) to true
        card is StoryRecordCard.Expired -> stringResource(R.string.story_note_expired) to false
        card is StoryRecordCard.Invalid -> stringResource(R.string.story_note_invalid) to false
        !hasPlace -> stringResource(
            if (card is StoryRecordCard.Attached) R.string.story_note_need_place_run else R.string.story_note_need_place,
        ) to false
        !StoryComposeRules.canPost(card, true, text, fallback) -> stringResource(R.string.story_note_need_text) to false
        else -> when (card) {
            is StoryRecordCard.Attached -> {
                val run = card.attachment.run
                when {
                    card.attachment.kept -> stringResource(R.string.story_note_kept)
                    !run.hasRoute -> stringResource(R.string.story_note_summary)
                    card.daysAgo >= StoryRunRules.WINDOW_DAYS -> stringResource(R.string.story_note_deadline_today, card.daysAgo.toInt())
                    else -> stringResource(R.string.story_note_deadline, words.date(StoryRunRules.lastAttachDay(run.day)))
                }
            }
            StoryRecordCard.Never, is StoryRecordCard.Available -> stringResource(R.string.story_note_never)
            is StoryRecordCard.Old, StoryRecordCard.Neutral -> stringResource(R.string.story_note_old)
            StoryRecordCard.FetchError -> stringResource(R.string.story_note_error)
            StoryRecordCard.Loading -> stringResource(R.string.story_card_loading_body)
            StoryRecordCard.ActiveRun, StoryRecordCard.Pending -> stringResource(R.string.story_note_waiting)
            is StoryRecordCard.Expired, is StoryRecordCard.Invalid -> ""
        } to false
    }
}
