package com.stepup.android.ui.screens.community.crew

import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.domain.CrewCapacityProblem
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDistance
import com.stepup.android.domain.CrewDraft
import com.stepup.android.domain.CrewDraftMode
import com.stepup.android.domain.CrewGoalChoice
import com.stepup.android.domain.CrewImageChoice
import com.stepup.android.domain.CrewMood
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRules
import com.stepup.android.domain.CrewSchedule
import com.stepup.android.ui.experience.feedbackClickable
import java.time.LocalTime
import kotlin.math.min

// ─────────────────────────────────────────────────────────────
// 25 모집할 크루 선택
// ─────────────────────────────────────────────────────────────

/**
 * 25 모집할 크루 선택 — 내가 운영하는 크루(누르면 모집 설정 43) · 새 크루 만들기(초안이 있으면 39) · 내 크루 관리(40).
 */
@Composable
fun CrewRecruitEntryScreen(
    onBack: () -> Unit,
    onRecruitSettings: (String) -> Unit,
    onManage: (String) -> Unit,
    onCreate: (resume: Boolean) -> Unit,
) {
    val ink = crewInk()
    val repo = ServiceLocator.crewCards
    val cards by repo.cards.collectAsStateWithLifecycle()
    val draft by remember(repo) { repo.draft(CrewDraft.KEY_CREATE) }.collectAsStateWithLifecycle(null)
    var sheet by rememberSaveable { mutableStateOf("") }
    val owned = cards.filter { it.owned }
    CrewPage(Modifier.testTag("crew-recruit-entry")) {
        CrewTopBar(stringResource(R.string.crew_entry_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.crew_entry_heading), color = ink.text, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.crew_entry_sub), color = ink.secondary, fontSize = 14.5.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(36.dp))
            owned.forEach { crew ->
                CrewOwnedRow(crew) { onRecruitSettings(crew.id) }
                Spacer(Modifier.height(16.dp))
            }
            Spacer(Modifier.height(36.dp))
            CrewButton(
                stringResource(R.string.crew_entry_new),
                { if (draft != null) sheet = SHEET_DRAFT_RESUME else onCreate(false) },
                Modifier.testTag("crew-entry-new"),
                CrewButtonKind.SECONDARY,
            )
            Spacer(Modifier.height(56.dp))
            owned.forEach { crew ->
                CrewRow(stringResource(R.string.crew_entry_manage), { onManage(crew.id) }, Modifier.testTag("crew-entry-manage-${crew.id}"), value = crew.name)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (sheet.isNotEmpty()) CrewStartSheets(draft, discard = sheet == SHEET_DRAFT_DISCARD, onStep = { sheet = it }, onCreate = onCreate)
}

/** 운영 중인 크루 한 줄 — 이미지 · 이름 · "24 / 30명 · 모집 중" */
@Composable
private fun CrewOwnedRow(crew: CrewCard, onClick: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ink.card).feedbackClickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp).testTag("crew-entry-crew-${crew.id}"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CrewImage(crew, 72.dp, 16.dp)
        Column(Modifier.weight(1f).padding(start = 18.dp)) {
            Text(crew.name, color = ink.text, fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            val members = if (crew.capacity != null) stringResource(R.string.crew_members_of, crew.memberCount, crew.capacity)
            else stringResource(R.string.crew_members_only, crew.memberCount)
            val status = stringResource(if (crew.recruiting) R.string.crew_recruit_open else R.string.crew_recruit_paused)
            Text("$members · $status", color = ink.info, fontSize = 14.sp, maxLines = 1)
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────────────────────────────────────
// 만들기(26 · 31 · 33 · 34) · 수정(41 · 42 · 43)
// ─────────────────────────────────────────────────────────────

/** 만들기 · 수정에서 나가는 곳 */
class CrewDraftActions(
    /** 초안을 정한 뒤 나간다(38 을 거쳤거나 바뀐 것이 없다) */
    val onBack: () -> Unit,
    /** 03 활동 지역 선택 — 고른 곳은 이 길의 [CrewDraftViewModel.PICK_REGION] 으로 돌아온다 */
    val onPickRegion: () -> Unit,
    /** 43 "직접 입력" — 운영 중인 크루의 목표는 주간 목표 수정(46)에서 바로 바꾼다 */
    val onGoalEdit: () -> Unit,
    /** 35 만들기 완료 */
    val onCreated: (String) -> Unit,
    /** 수정 저장 — 관리 홈(40)으로 */
    val onSaved: () -> Unit,
)

private enum class CrewDraftPage { IDENTITY, RUNNING, RECRUIT, PREVIEW }

private fun CrewDraft.page(): CrewDraftPage = when (mode) {
    CrewDraftMode.EDIT_PROFILE -> CrewDraftPage.IDENTITY
    CrewDraftMode.EDIT_RUNNING -> CrewDraftPage.RUNNING
    CrewDraftMode.EDIT_RECRUIT -> CrewDraftPage.RECRUIT
    CrewDraftMode.CREATE -> CrewDraftPage.entries[step.coerceIn(0, 3)]
}

/** 서버가 알려 준 칸 오류가 있는 만들기 단계 */
private fun CrewProblem.step(): Int? = when (this) {
    CrewProblem.NAME, CrewProblem.TAGLINE, CrewProblem.LEADER_NOTE, CrewProblem.IMAGE -> 0
    CrewProblem.AREA, CrewProblem.SCHEDULE -> 1
    CrewProblem.CAPACITY, CrewProblem.CAPACITY_BELOW_MEMBERS, CrewProblem.GOAL -> 2
    else -> null
}

private const val SHEET_IMAGE = "image"
private const val SHEET_SCHEDULE = "schedule"
private const val SHEET_GOAL = "goal"
private const val SHEET_LEAVE = "leave"

/**
 * 크루 만들기 1~3단계와 미리보기, 크루장의 소개 · 모임 정보 · 모집 설정 수정. 단계를 오가도 같은 초안을 고치고,
 * 저장이 실패하면 입력과 이미지가 그대로 남아 같은 저장(같은 요청 키)을 다시 한다(37). 칸 오류는 그 칸 아래에(36).
 */
@Composable
fun CrewDraftScreen(viewModel: CrewDraftViewModel, actions: CrewDraftActions) {
    val ink = crewInk()
    val context = LocalContext.current
    val words = rememberCrewWords()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val ready by viewModel.ready.collectAsStateWithLifecycle()
    val startFailed by viewModel.startFailed.collectAsStateWithLifecycle()
    val restored by viewModel.restored.collectAsStateWithLifecycle()
    val save by viewModel.save.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val card by viewModel.card.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf("") }
    var named by rememberSaveable { mutableStateOf(false) }
    val mode = viewModel.mode
    val page = draft.page()
    val problem = (save as? CrewSave.Failed)?.problem
    val scroll = rememberScrollState()

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // 고르지 않고 돌아오면 이미지는 그대로
        if (uri != null) viewModel.loadPhoto(context, uri)
    }
    val pickPhoto = {
        sheet = ""
        val staged = CrewPhotoPickerForTest.answer
        when {
            CrewPhotoPickerForTest.unavailable -> viewModel.photoUnavailable()
            staged != null -> viewModel.loadPhoto(context, staged)
            else -> runCatching { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                .onFailure { viewModel.photoUnavailable() }
        }
        Unit
    }

    LaunchedEffect(save) {
        when (val s = save) {
            is CrewSave.Created -> {
                viewModel.consumeSave()
                actions.onCreated(s.crewId)
            }
            CrewSave.Saved -> {
                viewModel.consumeSave()
                actions.onSaved()
            }
            is CrewSave.Failed -> if (mode == CrewDraftMode.CREATE) s.problem.step()?.let { if (it != draft.step) viewModel.step(it) }
            else -> Unit
        }
    }
    LaunchedEffect(page) { scroll.scrollTo(0) }

    val cropping = photo is CrewPhotoStep.Loading || photo is CrewPhotoStep.Crop || photo is CrewPhotoStep.Saving
    val requestBack: () -> Unit = {
        when {
            cropping -> viewModel.closePhoto()
            named -> named = false
            save is CrewSave.Saving -> Unit
            mode == CrewDraftMode.CREATE && draft.step > 0 -> viewModel.step(draft.step - 1)
            ready && viewModel.changed -> sheet = SHEET_LEAVE
            else -> actions.onBack()
        }
    }
    BackHandler(onBack = requestBack)

    if (cropping) {
        CrewCropPage(photo, onBack = viewModel::closePhoto, onUse = viewModel::applyCrop)
        return
    }
    if (named) {
        CrewNamedImagesPage(
            name = draft.name.ifBlank { card?.name.orEmpty() },
            initial = draftBg(draft, card),
            onBack = { named = false },
            onUse = { bg ->
                viewModel.replaceImage(CrewImageChoice.Named(bg))
                named = false
            },
        )
        return
    }

    val title = when (mode) {
        CrewDraftMode.CREATE -> stringResource(if (page == CrewDraftPage.PREVIEW) R.string.crew_preview_title else R.string.crew_create_title_new)
        CrewDraftMode.EDIT_PROFILE -> stringResource(R.string.crew_manage_profile)
        CrewDraftMode.EDIT_RUNNING -> stringResource(R.string.crew_manage_running)
        CrewDraftMode.EDIT_RECRUIT -> stringResource(R.string.crew_manage_recruit)
    }
    val label: String? = when {
        mode == CrewDraftMode.CREATE && page != CrewDraftPage.PREVIEW -> stringResource(
            R.string.crew_create_step,
            draft.step + 1,
            stringResource(
                when (page) {
                    CrewDraftPage.IDENTITY -> R.string.crew_create_step_identity
                    CrewDraftPage.RUNNING -> R.string.crew_create_step_running
                    else -> R.string.crew_create_step_recruit
                },
            ),
        )
        mode == CrewDraftMode.EDIT_RUNNING -> stringResource(R.string.crew_edit_running_label)
        mode == CrewDraftMode.EDIT_RECRUIT -> card?.name ?: draft.name
        else -> null
    }
    val members = card?.memberCount ?: 1
    val capacityProblem = CrewRules.capacityProblem(draft.capacity, if (mode == CrewDraftMode.CREATE) 1 else members)
    val pageOk = when (page) {
        CrewDraftPage.IDENTITY -> draft.nameOk
        CrewDraftPage.RUNNING -> true
        CrewDraftPage.RECRUIT -> capacityProblem == null
        CrewDraftPage.PREVIEW -> draft.nameOk && CrewRules.capacity(draft.capacity) != null
    }
    val saving = save is CrewSave.Saving
    val retry = problem != null && problem.step() == null
    val button = when {
        retry -> stringResource(R.string.crew_save_retry)
        mode == CrewDraftMode.CREATE -> when (page) {
            CrewDraftPage.PREVIEW -> stringResource(if (draft.recruiting) R.string.crew_create_start else R.string.crew_create_submit_paused)
            CrewDraftPage.RECRUIT -> stringResource(R.string.crew_create_preview)
            else -> stringResource(R.string.crew_next)
        }
        mode == CrewDraftMode.EDIT_PROFILE -> stringResource(R.string.crew_edit_profile_save)
        mode == CrewDraftMode.EDIT_RUNNING -> stringResource(R.string.crew_edit_running_save)
        else -> stringResource(R.string.crew_edit_recruit_save)
    }
    val allOk = draft.nameOk && (mode != CrewDraftMode.CREATE && mode != CrewDraftMode.EDIT_RECRUIT || capacityProblem == null)
    val enabled = ready && (if (retry) allOk else pageOk && (mode == CrewDraftMode.CREATE || viewModel.changed))
    val onPrimary: () -> Unit = {
        // 저장이 실패했으면 어느 단계에서든 같은 저장을 다시 한다(37)
        if (!retry && mode == CrewDraftMode.CREATE && page != CrewDraftPage.PREVIEW) viewModel.step(draft.step + 1) else viewModel.submit()
    }
    val caption = when {
        problem == null -> null
        problem.step() != null -> null
        problem == CrewProblem.NETWORK || problem == CrewProblem.OTHER -> stringResource(R.string.crew_save_failed)
        else -> crewProblemText(problem)
    }

    CrewPage(Modifier.imePadding().testTag("crew-draft-${mode.name.lowercase()}-${page.name.lowercase()}")) {
        CrewTopBar(title, requestBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(scroll).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(if (label != null) 18.dp else 8.dp))
            if (label != null) {
                Text(label, color = ink.info, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("crew-draft-label"))
                Spacer(Modifier.height(if (page == CrewDraftPage.IDENTITY) 26.dp else 30.dp))
            }
            if (restored && mode != CrewDraftMode.CREATE) {
                CrewRestoredLine(onRevert = viewModel::revert)
                Spacer(Modifier.height(18.dp))
            }
            if (!ready && startFailed) {
                CrewEmptyState(
                    icon = { Icon(Icons.Filled.Refresh, null, tint = ink.info, modifier = Modifier.size(44.dp)) },
                    title = stringResource(R.string.crew_detail_error_title),
                    body = stringResource(R.string.crew_list_error_body),
                    modifier = Modifier.padding(top = 60.dp).testTag("crew-draft-error"),
                ) { CrewButton(stringResource(R.string.crew_list_reload), viewModel::retryStart, Modifier.testTag("crew-draft-retry")) }
            } else if (!ready) {
                CrewSkeletonBox(Modifier.fillMaxWidth().height(180.dp), 18.dp)
            } else {
                when (page) {
                    CrewDraftPage.IDENTITY -> CrewIdentityForm(draft, card, problem, viewModel, onImage = { sheet = SHEET_IMAGE })
                    CrewDraftPage.RUNNING -> CrewRunningForm(draft, problem, words, viewModel, onArea = actions.onPickRegion, onSchedule = { sheet = SHEET_SCHEDULE })
                    CrewDraftPage.RECRUIT -> CrewRecruitForm(
                        draft = draft,
                        mode = mode,
                        members = members,
                        openJoin = mode != CrewDraftMode.CREATE && card?.openJoin == true,
                        capacityProblem = capacityProblem,
                        problem = problem,
                        viewModel = viewModel,
                        onCustomGoal = { if (mode == CrewDraftMode.CREATE) sheet = SHEET_GOAL else actions.onGoalEdit() },
                    )
                    CrewDraftPage.PREVIEW -> CrewPreview(draft, words)
                }
            }
            Spacer(Modifier.height(28.dp))
        }
        CrewBottomBar(caption) {
            CrewButton(button, onPrimary, Modifier.testTag("crew-draft-primary"), busy = saving, enabled = enabled)
        }
    }

    when (sheet) {
        SHEET_IMAGE -> CrewImageMethodSheet(
            hasPhoto = draft.image is CrewImageChoice.Photo || (draft.image == CrewImageChoice.Server && card?.hasImage == true),
            onPhoto = pickPhoto,
            onNamed = { sheet = ""; named = true },
            onRemove = {
                sheet = ""
                viewModel.replaceImage(CrewImageChoice.Named(draftBg(draft, card)))
            },
            onDismiss = { sheet = "" },
        )
        SHEET_SCHEDULE -> CrewScheduleSheet(
            initial = draft.schedule,
            words = words,
            onApply = { next ->
                sheet = ""
                viewModel.update { it.copy(schedule = next.normalized) }
            },
            onDismiss = { sheet = "" },
        )
        SHEET_GOAL -> CrewCustomGoalSheet(
            initial = draft.goal.km,
            onApply = { km ->
                sheet = ""
                viewModel.update { it.copy(goal = CrewGoalChoice.Km(km, custom = true)) }
            },
            onDismiss = { sheet = "" },
        )
        SHEET_LEAVE -> CrewConfirmSheet(
            title = stringResource(R.string.crew_leave_draft_title),
            body = stringResource(if (mode == CrewDraftMode.CREATE) R.string.crew_leave_draft_body else R.string.crew_leave_edit_body),
            confirm = stringResource(R.string.crew_leave_draft_keep),
            tag = "crew-leave-draft",
            onConfirm = { viewModel.keepDraft { sheet = ""; actions.onBack() } },
            onDismiss = { sheet = "" },
        )
    }
    when (photo) {
        CrewPhotoStep.Broken -> CrewPhotoProblemSheet(
            denied = false,
            onPrimary = { viewModel.closePhoto(); pickPhoto() },
            onNamed = { viewModel.closePhoto(); named = true },
            onDismiss = viewModel::closePhoto,
        )
        CrewPhotoStep.Denied -> CrewPhotoProblemSheet(
            denied = true,
            onPrimary = {
                viewModel.closePhoto()
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                }
            },
            onNamed = { viewModel.closePhoto(); named = true },
            onDismiss = viewModel::closePhoto,
        )
        else -> Unit
    }
}

/**
 * 기기 검사 — OS 사진 선택기(다른 앱 화면이라 검사가 누를 수 없다) 대신 고른 사진([answer])이나 사진 접근 불가([unavailable]).
 * 앱에서는 늘 비어 있다.
 */
object CrewPhotoPickerForTest {
    @Volatile var answer: Uri? = null
    @Volatile var unavailable: Boolean = false
}

/** 이름 이미지의 바탕 — 고른 것, 없으면 크루의 것, 그것도 없으면 요청 키로 */
private fun draftBg(draft: CrewDraft, card: CrewCard?): Int =
    (draft.image as? CrewImageChoice.Named)?.bg ?: card?.bg ?: CrewRules.bgFor(draft.clientKey)

/** 이 폰에 남겨 둔 수정 초안을 불러왔다 — 처음 값으로 되돌릴 수 있다 */
@Composable
private fun CrewRestoredLine(onRevert: () -> Unit) {
    val ink = crewInk()
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(ink.card).padding(start = 16.dp, end = 6.dp).heightIn(min = 48.dp)
            .testTag("crew-draft-restored"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.crew_draft_restored), color = ink.secondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Box(
            Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp)).feedbackClickable(role = Role.Button, onClick = onRevert)
                .padding(horizontal = 10.dp).testTag("crew-draft-revert"),
            contentAlignment = Alignment.Center,
        ) {
            Text(stringResource(R.string.crew_draft_revert), color = ink.info, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** 초안의 대표 이미지 — 고른 사진, 이름 이미지, 또는 크루의 지금 이미지(이름은 고치는 중인 것으로) */
@Composable
internal fun CrewDraftImage(
    draft: CrewDraft,
    size: Dp,
    radius: Dp,
    modifier: Modifier = Modifier,
    card: CrewCard? = null,
    textSize: TextUnit? = null,
) {
    val name = draft.name.ifBlank { card?.name.orEmpty() }
    when (val image = draft.image) {
        is CrewImageChoice.Photo -> CrewImageFace(name, draftBg(draft, card), rememberDraftPhoto(image.path), size, radius, modifier.testTag("crew-image"), textSize)
        is CrewImageChoice.Named -> CrewImageFace(name, image.bg, null, size, radius, modifier.testTag("crew-image"), textSize)
        CrewImageChoice.Server ->
            if (card != null) CrewImage(card.copy(name = name), size, radius, modifier, textSize)
            else CrewImageFace(name, draftBg(draft, null), null, size, radius, modifier.testTag("crew-image"), textSize)
    }
}

// ── 26 · 41 크루 소개 ─────────────────────────────────────────

@Composable
private fun CrewIdentityForm(draft: CrewDraft, card: CrewCard?, problem: CrewProblem?, viewModel: CrewDraftViewModel, onImage: () -> Unit) {
    val ink = crewInk()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(20.dp)).feedbackClickable(role = Role.Button, onClick = onImage).testTag("crew-draft-image")) {
            CrewDraftImage(draft, 88.dp, 20.dp, card = card)
        }
        Row(
            Modifier.padding(start = 12.dp).heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                .feedbackClickable(role = Role.Button, onClick = onImage).padding(horizontal = 12.dp).testTag("crew-draft-image-choose"),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.PhotoCamera, contentDescription = null, tint = ink.info, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(stringResource(R.string.crew_image_choose), color = ink.info, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    if (problem == CrewProblem.IMAGE) {
        Spacer(Modifier.height(10.dp))
        CrewHelp(stringResource(R.string.crew_image_upload_failed), error = true, modifier = Modifier.testTag("crew-draft-image-error"))
    }
    Spacer(Modifier.height(36.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_name))
    Spacer(Modifier.height(12.dp))
    CrewTextField(
        draft.name, { next -> viewModel.update { it.copy(name = next.replace('\n', ' ')) } }, Modifier.testTag("crew-draft-name"),
        placeholder = stringResource(R.string.crew_field_name_hint), maxChars = CrewRules.NAME_MAX,
    )
    Spacer(Modifier.height(10.dp))
    if (problem == CrewProblem.NAME) CrewHelp(stringResource(R.string.crew_field_name_error), error = true, modifier = Modifier.testTag("crew-draft-name-error"))
    else CrewHelp(stringResource(R.string.crew_field_name_help))
    Spacer(Modifier.height(24.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_tagline))
    Spacer(Modifier.height(12.dp))
    CrewTextField(
        draft.tagline, { next -> viewModel.update { it.copy(tagline = next.replace('\n', ' ')) } }, Modifier.testTag("crew-draft-tagline"),
        placeholder = stringResource(R.string.crew_field_tagline_hint), maxChars = CrewRules.TAGLINE_MAX,
    )
    Spacer(Modifier.height(10.dp))
    if (problem == CrewProblem.TAGLINE) CrewHelp(stringResource(R.string.crew_field_tagline_error), error = true)
    else CrewHelp(stringResource(R.string.crew_field_tagline_help))
    Spacer(Modifier.height(24.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_note))
    Spacer(Modifier.height(12.dp))
    CrewTextField(
        draft.leaderNote, { next -> viewModel.update { it.copy(leaderNote = next) } }, Modifier.testTag("crew-draft-note"),
        placeholder = stringResource(R.string.crew_field_note_hint), singleLine = false, minHeight = 104.dp, maxChars = CrewRules.NOTE_MAX,
    )
    if (problem == CrewProblem.LEADER_NOTE) {
        Spacer(Modifier.height(10.dp))
        CrewHelp(stringResource(R.string.crew_field_note_error), error = true)
    }
}

// ── 31 · 42 함께 달리는 방식 ──────────────────────────────────

@Composable
private fun CrewRunningForm(
    draft: CrewDraft,
    problem: CrewProblem?,
    words: CrewWords,
    viewModel: CrewDraftViewModel,
    onArea: () -> Unit,
    onSchedule: () -> Unit,
) {
    CrewFieldLabel(stringResource(R.string.crew_field_area))
    Spacer(Modifier.height(12.dp))
    CrewPickerField(draft.area?.name.orEmpty(), stringResource(R.string.crew_field_area_hint), onArea, Modifier.testTag("crew-draft-area"))
    if (problem == CrewProblem.AREA) {
        Spacer(Modifier.height(10.dp))
        CrewHelp(stringResource(R.string.crew_field_area_error), error = true)
    }
    Spacer(Modifier.height(30.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_schedule))
    Spacer(Modifier.height(12.dp))
    CrewPickerField(
        if (draft.schedule.none) "" else words.scheduleLong(draft.schedule), stringResource(R.string.crew_schedule_none), onSchedule,
        Modifier.testTag("crew-draft-schedule"),
    )
    if (problem == CrewProblem.SCHEDULE) {
        Spacer(Modifier.height(10.dp))
        CrewHelp(stringResource(R.string.crew_field_schedule_error), error = true)
    }
    Spacer(Modifier.height(36.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_distance))
    Spacer(Modifier.height(10.dp))
    CrewChoiceRow {
        CrewDistance.entries.forEach { value ->
            CrewChoice(
                words.distance(value), draft.distance == value,
                { viewModel.update { it.copy(distance = if (it.distance == value) null else value) } },
                Modifier.weight(1f).testTag("crew-draft-distance-${value.name}"),
            )
        }
    }
    Spacer(Modifier.height(30.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_mood))
    Spacer(Modifier.height(10.dp))
    CrewMood.entries.chunked(3).forEach { row ->
        CrewChoiceRow {
            row.forEach { mood ->
                CrewChoice(
                    words.mood(mood), mood in draft.moods, { viewModel.update { it.toggleMood(mood) } },
                    Modifier.weight(1f).testTag("crew-draft-mood-${mood.name}"),
                )
            }
        }
    }
}

// ── 33 · 43 모집과 목표 ───────────────────────────────────────

@Composable
private fun CrewRecruitForm(
    draft: CrewDraft,
    mode: CrewDraftMode,
    members: Int,
    openJoin: Boolean,
    capacityProblem: CrewCapacityProblem?,
    problem: CrewProblem?,
    viewModel: CrewDraftViewModel,
    onCustomGoal: () -> Unit,
) {
    val ink = crewInk()
    CrewFieldLabel(stringResource(R.string.crew_field_capacity))
    Spacer(Modifier.height(12.dp))
    CrewTextField(
        draft.capacity, { next -> viewModel.update { it.copy(capacity = next.filter(Char::isDigit).take(4)) } }, Modifier.testTag("crew-draft-capacity"),
        placeholder = stringResource(R.string.crew_field_capacity_hint),
        keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
        suffix = stringResource(R.string.crew_people_suffix),
    )
    Spacer(Modifier.height(10.dp))
    val below = capacityProblem == CrewCapacityProblem.BELOW_MEMBERS || problem == CrewProblem.CAPACITY_BELOW_MEMBERS
    val invalid = draft.capacity.isNotEmpty() && capacityProblem == CrewCapacityProblem.INVALID || problem == CrewProblem.CAPACITY
    when {
        mode != CrewDraftMode.CREATE -> CrewHelp(
            stringResource(R.string.crew_field_capacity_members, members), error = below || invalid,
            modifier = Modifier.testTag("crew-draft-capacity-help"),
        )
        invalid -> CrewHelp(stringResource(R.string.crew_field_capacity_error), error = true, modifier = Modifier.testTag("crew-draft-capacity-help"))
        else -> CrewHelp(stringResource(R.string.crew_field_capacity_help), modifier = Modifier.testTag("crew-draft-capacity-help"))
    }
    Spacer(Modifier.height(40.dp))
    Text(
        stringResource(if (openJoin) R.string.crew_recruit_open_join_title else R.string.crew_recruit_approval_title),
        color = ink.text, fontSize = 20.sp, lineHeight = 27.sp, fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(10.dp))
    Text(
        stringResource(if (openJoin) R.string.crew_recruit_open_join_body else R.string.crew_recruit_approval_body),
        color = ink.secondary, fontSize = 14.sp, lineHeight = 22.sp,
    )
    Spacer(Modifier.height(40.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_goal))
    Spacer(Modifier.height(10.dp))
    val goalKm = draft.goal.km
    val custom = goalKm != null && goalKm !in CrewRules.CREATE_GOALS
    CrewChoiceRow {
        CrewRules.CREATE_GOALS.forEach { km ->
            CrewChoice(
                stringResource(R.string.crew_km_value, km.toString()), goalKm == km,
                { viewModel.update { it.copy(goal = CrewGoalChoice.Km(km)) } },
                Modifier.weight(1f).testTag("crew-draft-goal-$km"),
            )
        }
        CrewChoice(
            if (custom) stringResource(R.string.crew_km_value, goalKm.toString()) else stringResource(R.string.crew_goal_custom),
            custom, onCustomGoal, Modifier.weight(1f).testTag("crew-draft-goal-custom"),
        )
    }
    Box(
        Modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(10.dp))
            .feedbackClickable(role = Role.RadioButton) { viewModel.update { it.copy(goal = CrewGoalChoice.None) } }
            .semantics { selected = goalKm == null }
            .testTag("crew-draft-goal-none"),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            stringResource(R.string.crew_goal_later), color = ink.info, fontSize = 14.sp,
            fontWeight = if (goalKm == null) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
    if (problem == CrewProblem.GOAL) CrewHelp(stringResource(R.string.crew_goal_invalid), error = true)
    Spacer(Modifier.height(14.dp))
    CrewFieldLabel(stringResource(R.string.crew_field_recruiting))
    Spacer(Modifier.height(10.dp))
    CrewChoiceRow {
        CrewChoice(
            stringResource(R.string.crew_recruit_open), draft.recruiting, { viewModel.update { it.copy(recruiting = true) } },
            Modifier.weight(1f).testTag("crew-draft-recruit-open"),
        )
        CrewChoice(
            stringResource(R.string.crew_recruit_pause_choice), !draft.recruiting, { viewModel.update { it.copy(recruiting = false) } },
            Modifier.weight(1f).testTag("crew-draft-recruit-paused"),
        )
    }
}

// ── 34 모집 미리보기 ─────────────────────────────────────────

/** 초안으로 만든 명함 — 미리보기에서만 쓴다(서버에 없는 값을 꾸미지 않는다: 새 크루 · 1명 · 크루장 나) */
private fun CrewDraft.previewCard(me: String): CrewCard = CrewCard(
    id = clientKey.ifEmpty { "draft" },
    name = name.trim(),
    tagline = tagline.trim(),
    leaderNote = leaderNote.trim(),
    leaderName = me,
    imageBg = (image as? CrewImageChoice.Named)?.bg ?: CrewRules.bgFor(clientKey),
    area = area?.name.orEmpty(),
    lat = area?.lat,
    lng = area?.lng,
    schedule = schedule.normalized,
    distance = distance,
    moods = moods,
    memberCount = 1,
    capacity = CrewRules.capacity(capacity),
    recruiting = recruiting,
    goalKm = goal.km,
    owned = true,
)

@Composable
private fun CrewPreview(draft: CrewDraft, words: CrewWords) {
    val ink = crewInk()
    val me = stringResource(R.string.crew_leader_me)
    val card = draft.previewCard(me)
    Text(stringResource(R.string.crew_preview_heading), color = ink.text, fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(14.dp))
    Text(stringResource(R.string.crew_preview_sub), color = ink.secondary, fontSize = 14.sp, lineHeight = 22.sp)
    Spacer(Modifier.height(36.dp))
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(ink.card).padding(horizontal = 18.dp, vertical = 18.dp)
            .testTag("crew-preview-card"),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrewDraftImage(draft, 68.dp, 14.dp)
            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                Text(card.name, color = ink.text, fontSize = 23.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                Text(crewLevelMembers(card), color = ink.info, fontSize = 13.5.sp, maxLines = 1)
            }
        }
        if (card.tagline.isNotBlank()) {
            Spacer(Modifier.height(18.dp))
            Text(card.tagline, color = ink.text, fontSize = 19.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        val info = words.infoLine(card)
        if (info.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Text(info, color = ink.secondary, fontSize = 13.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            CrewAvatar(me, 32.dp, leaderFace(card), ink.text)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.crew_leader_named, me), color = ink.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        card.goalKm?.let { km ->
            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.crew_preview_goal, km), color = ink.info, fontSize = 13.5.sp)
        }
    }
    Spacer(Modifier.height(40.dp))
    val lines = listOfNotNull(
        stringResource(R.string.crew_preview_leader, me),
        card.moods.joinToString(" · ") { words.moodLong(it) }.ifEmpty { null },
        stringResource(if (card.recruiting) R.string.crew_preview_approval else R.string.crew_preview_paused),
    )
    lines.forEach { line ->
        Text(line, color = ink.secondary, fontSize = 14.5.sp, lineHeight = 25.sp)
    }
}

// ── 27 대표 이미지 바꾸기 · 29 이미지 처리 실패 · 69 사진 접근 불가 ─────────

@Composable
private fun CrewImageMethodSheet(hasPhoto: Boolean, onPhoto: () -> Unit, onNamed: () -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(stringResource(R.string.crew_image_sheet_title), onDismiss, Modifier.testTag("crew-image-sheet")) {
        Spacer(Modifier.height(14.dp))
        CrewRow(stringResource(R.string.crew_image_from_photos), onPhoto, Modifier.testTag("crew-image-photos"))
        CrewRow(stringResource(R.string.crew_image_named_start), onNamed, Modifier.testTag("crew-image-named"), divider = hasPhoto)
        if (hasPhoto) CrewRow(stringResource(R.string.crew_image_remove), onRemove, Modifier.testTag("crew-image-remove"), titleColor = ink.warn, divider = false)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CrewPhotoProblemSheet(denied: Boolean, onPrimary: () -> Unit, onNamed: () -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    CrewSheet(
        stringResource(if (denied) R.string.crew_photo_denied_title else R.string.crew_photo_broken_title), onDismiss,
        Modifier.testTag(if (denied) "crew-photo-denied" else "crew-photo-broken"),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(
            stringResource(if (denied) R.string.crew_photo_denied_body else R.string.crew_photo_broken_body),
            color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp,
        )
        Spacer(Modifier.height(64.dp))
        CrewButton(
            stringResource(if (denied) R.string.crew_photo_settings else R.string.crew_photo_other),
            onPrimary, Modifier.testTag("crew-photo-primary"),
        )
        Spacer(Modifier.height(16.dp))
        CrewButton(stringResource(R.string.crew_photo_named), onNamed, Modifier.testTag("crew-photo-named"), CrewButtonKind.SECONDARY)
    }
}

// ── 28 이미지 맞추기 ─────────────────────────────────────────

/**
 * 28 정사각형 자르기 — 틀을 채우는 크기에서 시작해 손가락으로 옮기고 4배까지 키운다. 틀 밖으로 빈칸이 생기지 않게 막는다.
 * 자르는 좌표는 원본 사진 기준으로 넘긴다(보이는 틀 그대로 잘린다).
 */
@Composable
private fun CrewCropPage(step: CrewPhotoStep, onBack: () -> Unit, onUse: (left: Float, top: Float, side: Float) -> Unit) {
    val ink = crewInk()
    val source = (step as? CrewPhotoStep.Crop)?.source
    val image = remember(source) { source?.asImageBitmap() }
    var frame by remember { mutableFloatStateOf(0f) }
    var zoom by remember(source) { mutableFloatStateOf(1f) }
    var offset by remember(source) { mutableStateOf<Offset?>(null) }

    fun base(): Float = if (image == null || frame <= 0f) 1f else frame / min(image.width, image.height).toFloat()
    fun centered(z: Float): Offset {
        val img = image ?: return Offset.Zero
        val scale = base() * z
        return Offset((frame - img.width * scale) / 2f, (frame - img.height * scale) / 2f)
    }
    fun clamp(o: Offset, z: Float): Offset {
        val img = image ?: return o
        val scale = base() * z
        return Offset(o.x.coerceIn(frame - img.width * scale, 0f), o.y.coerceIn(frame - img.height * scale, 0f))
    }

    CrewPage(Modifier.testTag("crew-crop")) {
        CrewTopBar(stringResource(R.string.crew_crop_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.crew_crop_heading), color = ink.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(56.dp))
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 10.dp).aspectRatio(1f).clip(RoundedCornerShape(42.dp)).background(ink.card)
                    .onSizeChanged { frame = it.width.toFloat() }
                    .pointerInput(image, frame) {
                        if (image == null) return@pointerInput
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            val before = zoom
                            val next = (before * gestureZoom).coerceIn(1f, 4f)
                            val current = offset ?: centered(before)
                            val ratio = next / before
                            val moved = Offset(
                                centroid.x - (centroid.x - current.x) * ratio + pan.x,
                                centroid.y - (centroid.y - current.y) * ratio + pan.y,
                            )
                            zoom = next
                            offset = clamp(moved, next)
                        }
                    }
                    .testTag("crew-crop-frame"),
                contentAlignment = Alignment.Center,
            ) {
                if (image == null) {
                    CircularProgressIndicator(Modifier.size(28.dp), color = ink.info, strokeWidth = 2.5.dp)
                } else {
                    Canvas(Modifier.fillMaxSize()) {
                        val scale = base() * zoom
                        val at = offset ?: centered(zoom)
                        drawImage(
                            image,
                            srcOffset = IntOffset.Zero,
                            srcSize = IntSize(image.width, image.height),
                            dstOffset = IntOffset(at.x.toInt(), at.y.toInt()),
                            dstSize = IntSize((image.width * scale).toInt(), (image.height * scale).toInt()),
                            filterQuality = FilterQuality.High,
                        )
                    }
                }
            }
            Spacer(Modifier.height(28.dp))
            Text(
                stringResource(R.string.crew_crop_help), color = ink.secondary, fontSize = 13.5.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CrewBottomBar {
            CrewButton(
                stringResource(R.string.crew_image_use),
                {
                    val img = image
                    if (img != null && frame > 0f) {
                        val scale = base() * zoom
                        val at = offset ?: centered(zoom)
                        onUse(-at.x / scale, -at.y / scale, frame / scale)
                    }
                },
                Modifier.testTag("crew-crop-use"),
                busy = step is CrewPhotoStep.Saving,
                enabled = image != null && frame > 0f,
            )
        }
    }
}

// ── 30 기본 이미지 ───────────────────────────────────────────

/** 30 이름이 들어간 기본 이미지 — 실제 크루 이름과 바탕 네 가지 중 하나 */
@Composable
private fun CrewNamedImagesPage(name: String, initial: Int, onBack: () -> Unit, onUse: (Int) -> Unit) {
    val ink = crewInk()
    var picked by rememberSaveable { mutableIntStateOf(initial) }
    CrewPage(Modifier.testTag("crew-named-images")) {
        CrewTopBar(stringResource(R.string.crew_named_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(24.dp))
            Text(stringResource(R.string.crew_named_heading), color = ink.text, fontSize = 26.sp, lineHeight = 33.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Text(stringResource(R.string.crew_named_sub), color = ink.secondary, fontSize = 14.sp, lineHeight = 22.sp)
            Spacer(Modifier.height(36.dp))
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val cell = (maxWidth - 12.dp) / 2
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    (0 until CrewRules.BG_COUNT).chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { bg ->
                                val shape = RoundedCornerShape(18.dp)
                                Box(
                                    Modifier.size(cell).clip(shape)
                                        .feedbackClickable(role = Role.RadioButton) { picked = bg }
                                        .semantics { selected = picked == bg }
                                        .then(if (picked == bg) Modifier.border(3.dp, ink.info, shape) else Modifier)
                                        .testTag("crew-named-$bg"),
                                ) {
                                    CrewImageFace(name, bg, null, cell, 18.dp)
                                    if (picked == bg) {
                                        Box(
                                            Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp).clip(CircleShape).background(ink.choiceOn),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = ink.choiceOnText, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
        CrewBottomBar { CrewButton(stringResource(R.string.crew_image_use), { onUse(picked) }, Modifier.testTag("crew-named-use")) }
    }
}

// ── 32 정기 일정 ─────────────────────────────────────────────

/** 32 요일 버튼 · 시작 시간(OS 시간 선택) · 정해진 시간 없어요. 고른 것은 "이 일정으로 선택"을 눌러야 초안에 들어간다 */
@Composable
private fun CrewScheduleSheet(initial: CrewSchedule, words: CrewWords, onApply: (CrewSchedule) -> Unit, onDismiss: () -> Unit) {
    val ink = crewInk()
    val context = LocalContext.current
    var days by rememberSaveable { mutableIntStateOf(initial.days) }
    var minutes by rememberSaveable { mutableStateOf(initial.minutes) }
    val schedule = CrewSchedule(days, minutes)
    CrewSheet(stringResource(R.string.crew_schedule_title), onDismiss, Modifier.testTag("crew-schedule-sheet")) {
        Spacer(Modifier.height(20.dp))
        CrewFieldLabel(stringResource(R.string.crew_schedule_days))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            (0..6).forEach { day ->
                CrewDayChip(words.day(day), schedule.has(day), { days = schedule.toggle(day).days }, Modifier.weight(1f).testTag("crew-schedule-day-$day"))
            }
        }
        Spacer(Modifier.height(30.dp))
        CrewFieldLabel(stringResource(R.string.crew_schedule_time))
        Spacer(Modifier.height(12.dp))
        CrewPickerField(
            if (days != 0) minutes?.let(words::timeLong).orEmpty() else "",
            stringResource(if (days != 0) R.string.crew_schedule_time_hint else R.string.crew_schedule_time_days_first),
            {
                val start = minutes ?: LocalTime.now().hour * 60
                TimePickerDialog(
                    context,
                    { _, hour, minute -> minutes = hour * 60 + minute },
                    start / 60, start % 60, DateFormat.is24HourFormat(context),
                ).show()
            },
            Modifier.testTag("crew-schedule-time"),
            enabled = days != 0,
        )
        Spacer(Modifier.height(28.dp))
        CrewChoice(
            stringResource(R.string.crew_schedule_no_time), days == 0,
            { days = 0; minutes = null },
            Modifier.fillMaxWidth().testTag("crew-schedule-none"),
        )
        Spacer(Modifier.height(36.dp))
        CrewButton(
            stringResource(R.string.crew_schedule_apply),
            { onApply(if (days == 0) CrewSchedule.NONE else CrewSchedule(days, minutes)) },
            Modifier.testTag("crew-schedule-apply"),
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** 요일 버튼 — 일곱 개가 한 줄에 들어가게 안쪽 여백을 줄였다 */
@Composable
private fun CrewDayChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ink = crewInk()
    Box(
        modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Checkbox, onClick = onClick).semantics { this.selected = selected }
            .padding(vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.fillMaxWidth().heightIn(min = 44.dp).clip(RoundedCornerShape(14.dp))
                .background(if (selected) ink.choiceOn else ink.choice).padding(horizontal = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = if (selected) ink.choiceOnText else ink.choiceText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

// ── 88 만드는 크루의 목표 ─────────────────────────────────────

/** 88 목표 직접 입력 — 초안의 목표만 바꾼다(운영 중인 크루는 바꾸지 않는다) */
@Composable
private fun CrewCustomGoalSheet(initial: Int?, onApply: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial?.toString().orEmpty()) }
    val value = CrewRules.goal(text)
    CrewSheet(stringResource(R.string.crew_custom_goal_title), onDismiss, Modifier.testTag("crew-custom-goal")) {
        Spacer(Modifier.height(22.dp))
        CrewFieldLabel(stringResource(R.string.crew_custom_goal_field))
        Spacer(Modifier.height(12.dp))
        CrewTextField(
            text, { next -> text = next.filter(Char::isDigit).take(5) }, Modifier.testTag("crew-custom-goal-input"),
            keyboard = KeyboardOptions(keyboardType = KeyboardType.Number),
            suffix = stringResource(R.string.crew_km_unit),
        )
        Spacer(Modifier.height(10.dp))
        val invalid = text.isNotEmpty() && value == null
        CrewHelp(stringResource(if (invalid) R.string.crew_goal_invalid else R.string.crew_custom_goal_help), error = invalid)
        Spacer(Modifier.height(56.dp))
        CrewButton(stringResource(R.string.crew_custom_goal_apply), { value?.let(onApply) }, Modifier.testTag("crew-custom-goal-apply"), enabled = value != null)
        Spacer(Modifier.height(8.dp))
    }
}

// ── 35 만들기 완료 ───────────────────────────────────────────

/** 35 크루를 만들었다 — 크루장으로 관리 화면(77)으로 이어진다 */
@Composable
fun CrewCreatedScreen(viewModel: CrewScreenViewModel, onBack: () -> Unit, onManage: () -> Unit) {
    val card by viewModel.card.collectAsStateWithLifecycle()
    val name = card?.name.orEmpty()
    CrewResultPage(
        title = stringResource(R.string.crew_created_title, withParticle(name, "을", "를")),
        body = stringResource(R.string.crew_created_body),
        button = stringResource(R.string.crew_manage_mine),
        onButton = onManage,
        onBack = onBack,
        modifier = Modifier.testTag("crew-created"),
    )
}
