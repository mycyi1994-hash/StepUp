package com.stepup.android.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.NameWork
import com.stepup.android.data.repo.PhotoWork
import com.stepup.android.data.repo.ProfileEditor
import com.stepup.android.data.repo.nicknameChanged
import com.stepup.android.ui.components.AvatarEmojis
import com.stepup.android.ui.components.BluePageHeader
import com.stepup.android.ui.components.BlueSheet
import com.stepup.android.ui.components.BlueWideButton
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.RunSpinner
import com.stepup.android.ui.components.blueListColors
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 프로필 수정 — 저장된 이름 · 사진과 저장 단계. 초안(입력 중인 이름)은 화면의 입력 상태가 들고 있다 */
class ProfileEditViewModel(
    prefs: UserPrefs,
    private val editor: ProfileEditor,
    private val handle: SavedStateHandle,
) : ViewModel() {
    /** 앞선 방문에서 저장하지 못한 초안 — 있으면 입력칸에 되살린다 */
    val restoredDraft: String? = editor.startSession()

    /** 저장된 닉네임(null 은 읽는 중) */
    val stored: StateFlow<String?> = prefs.nickname.map<String, String?> { it }.catch { emit("") }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val avatarId: StateFlow<Int?> = prefs.avatarId.map<Int, Int?> { it }.catch { emit(0) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val avatarRev: StateFlow<Int> = prefs.avatarRev.catch { emit(0) }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    val photo: StateFlow<PhotoWork> = editor.photo
    val defaultImage: StateFlow<PhotoWork> = editor.defaultImage
    val name: StateFlow<NameWork> = editor.name

    /** 이번 방문에서 사진 · 기본 이미지를 실제로 바꿨는가 — 나가기 확인의 "이미 바꾼 사진은 유지돼요" */
    val photoChanged: StateFlow<Boolean> = handle.getStateFlow(PHOTO_CHANGED, false)

    init {
        viewModelScope.launch {
            editor.photo.collect { if (it == PhotoWork.Saved) { handle[PHOTO_CHANGED] = true; editor.acknowledgePhoto() } }
        }
    }

    fun savePhoto(uri: android.net.Uri) {
        val resolver = ServiceLocator.appContext.contentResolver
        editor.savePhoto { resolver.openInputStream(uri) }
    }

    fun saveDefaultImage(id: Int) = editor.saveDefaultImage(id, avatarId.value)
    fun acknowledgeDefaultImage() = editor.acknowledgeDefaultImage()

    /** 기본 이미지를 저장했다 — "이번에 바꾼 사진"으로 적고 결과를 받는다(시트는 화면이 닫는다) */
    fun defaultImageSaved() {
        if (editor.defaultImage.value == PhotoWork.Saved) handle[PHOTO_CHANGED] = true
        editor.acknowledgeDefaultImage()
    }
    fun saveName(draft: String) = editor.saveName(draft)
    fun acknowledgeName() = editor.acknowledgeName()
    fun discardName() = editor.discardName()

    companion object {
        private const val PHOTO_CHANGED = "photoChanged"
        val Factory = viewModelFactory {
            initializer { ProfileEditViewModel(ServiceLocator.userPrefs, ServiceLocator.profileEditor, createSavedStateHandle()) }
        }
    }
}

/** 편집 화면이 보일 것 */
data class ProfileEditUi(
    val stored: String,
    val avatarId: Int,
    val customPhoto: ImageBitmap? = null,
    val photo: PhotoWork = PhotoWork.Idle,
    val name: NameWork = NameWork.Idle,
    val photoChanged: Boolean = false,
)

private enum class EditSheet { None, Photo, Defaults, Leave }

/**
 * 프로필 수정(2026-09-28 전달본, 파란 톤 v4 PRO01~17 로 색 · 배치를 바꿈) — 사진과 닉네임만. 하단 탭 없이 뒤로 가면 내 정보.
 * 사진 · 기본 이미지는 고른 즉시 저장, 닉네임은 아래 "저장"으로(저장된 것과 다를 때만 누를 수 있다).
 * 저장하지 않은 닉네임이 있을 때만 나가기를 묻는다.
 */
@Composable
fun ProfileEditScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProfileEditViewModel = viewModel(factory = ProfileEditViewModel.Factory),
) {
    val stored by viewModel.stored.collectAsStateWithLifecycle()
    val avatarId by viewModel.avatarId.collectAsStateWithLifecycle()
    val rev by viewModel.avatarRev.collectAsStateWithLifecycle()
    val photo by viewModel.photo.collectAsStateWithLifecycle()
    val defaultImage by viewModel.defaultImage.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val photoChanged by viewModel.photoChanged.collectAsStateWithLifecycle()
    val storedName = stored
    val currentAvatar = avatarId
    if (storedName == null || currentAvatar == null) {
        // PRO13 — 저장된 이름 · 사진을 읽는 중. 임의 이름 · 사진 · 글자 수를 넣지 않는다
        ProfileEditLoading(onBack)
        return
    }
    val field = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(viewModel.restoredDraft ?: storedName) }
    val focus = LocalFocusManager.current
    var sheet by rememberSaveable { mutableStateOf(EditSheet.None) }
    // PRO14 — 저장 중인 기본 이미지(그 칸에만 도는 표시). 저장이 끝나면(성공 · 실패) 지운다
    var pendingDefault by remember { mutableStateOf<Int?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        // 고르지 않고 돌아오면 사진 · 이름 초안 모두 그대로
        if (uri != null) viewModel.savePhoto(uri)
    }
    val pickPhoto = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val nameSaving = name is NameWork.Saving
    val dirty = nicknameChanged(storedName, field.text.toString(), UserPrefs.NICKNAME_MAX)
    val askLeave = dirty && !nameSaving
    val requestBack = {
        if (askLeave) {
            focus.clearFocus()
            sheet = EditSheet.Leave
        } else {
            onBack()
        }
    }
    BackHandler(enabled = askLeave && sheet == EditSheet.None) { requestBack() }

    // 실제로 저장된 뒤에만 내 정보로(안내는 거기서 한 번)
    LaunchedEffect(name) {
        if (name == NameWork.Saved) {
            viewModel.acknowledgeName()
            onSaved()
        }
    }
    // 기본 이미지를 저장하면 시트를 닫고 편집 화면으로
    LaunchedEffect(defaultImage) {
        if (defaultImage != PhotoWork.Saving) pendingDefault = null
        if (defaultImage == PhotoWork.Saved) {
            viewModel.defaultImageSaved()
            if (sheet == EditSheet.Defaults) sheet = EditSheet.None
        }
    }

    ProfileEditContent(
        ui = ProfileEditUi(
            stored = storedName, avatarId = currentAvatar, customPhoto = rememberCustomAvatar(rev),
            photo = if (defaultImage == PhotoWork.Saving) PhotoWork.Saving else photo, name = name, photoChanged = photoChanged,
        ),
        field = field,
        onBack = requestBack,
        onChangePhoto = {
            focus.clearFocus()
            sheet = EditSheet.Photo
        },
        onRetryPhoto = pickPhoto,
        onSave = {
            focus.clearFocus()
            viewModel.saveName(field.text.toString())
        },
    )
    when (sheet) {
        EditSheet.Photo -> PhotoActionsSheet(
            onAlbum = {
                sheet = EditSheet.None
                pickPhoto()
            },
            onDefaults = { sheet = EditSheet.Defaults },
            onDismiss = { sheet = EditSheet.None },
        )
        EditSheet.Defaults -> DefaultImageSheet(
            current = currentAvatar,
            saving = defaultImage == PhotoWork.Saving,
            failed = defaultImage == PhotoWork.Failed,
            pending = pendingDefault,
            onPick = { id ->
                // 지금 쓰는 이미지를 다시 누르면 저장하지 않고 닫는다
                if (viewModel.saveDefaultImage(id)) pendingDefault = id
                else if (id == currentAvatar) sheet = EditSheet.None
            },
            onDismiss = {
                viewModel.acknowledgeDefaultImage()
                sheet = EditSheet.None
            },
        )
        EditSheet.Leave -> LeaveSheet(
            photoChanged = photoChanged,
            onKeep = { sheet = EditSheet.None },
            onDiscard = {
                sheet = EditSheet.None
                viewModel.discardName()
                onBack()
            },
        )
        EditSheet.None -> Unit
    }
}


/**
 * 프로필 수정 본문(파란 톤 v4 PRO01~17) — 뒤로 · 제목 → 원형 사진 → 사진 변경 → 즉시 저장 안내 → (사진 오류) →
 * 닉네임 입력 → 안내 · 글자 수 → (이름 오류) → 아래 저장. 공통 로고 · SUP · 하단 탭은 없다.
 * 사진 오류는 사진 자리 바로 아래에, 이름 오류는 닉네임 아래에 붙인다(둘 다면 둘 다, 본문은 넘어간다).
 */
@Composable
fun ProfileEditContent(
    ui: ProfileEditUi,
    field: TextFieldState,
    onBack: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
    onRetryPhoto: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val t = runTone()
    val max = UserPrefs.NICKNAME_MAX
    val text = field.text.toString()
    val photoSaving = ui.photo == PhotoWork.Saving
    val nameSaving = ui.name is NameWork.Saving
    val dirty = nicknameChanged(ui.stored, text, max)
    Column(Modifier.fillMaxSize().imePadding()) {
        BluePageHeader(stringResource(R.string.pe_title), onBack, Modifier.padding(horizontal = EditGutter))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = EditGutter),
        ) {
            Spacer(Modifier.height(24.dp))
            EditAvatar(ui.avatarId, ui.customPhoto, photoSaving, Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(16.dp))
            val pill = RoundedCornerShape(50)
            val photoEnabled = !photoSaving && !nameSaving
            Box(
                Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp).widthIn(min = 150.dp)
                    .clip(pill).background(t.secondaryFace, pill)
                    .border(1.5.dp, if (photoEnabled) t.secondaryEdge else t.divider, pill)
                    .feedbackClickable(enabled = photoEnabled, role = Role.Button, onClick = onChangePhoto)
                    .padding(horizontal = 26.dp)
                    .testTag("pe-change-photo"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(if (photoSaving) R.string.pe_photo_saving_chip else R.string.pe_change_photo),
                    style = runTextStyle(17.sp, if (photoEnabled) t.text else t.muted, FontWeight.Bold),
                )
            }
            Text(
                stringResource(
                    when {
                        photoSaving -> R.string.pe_photo_saving
                        ui.photoChanged && ui.photo == PhotoWork.Idle -> R.string.pe_photo_saved
                        else -> R.string.pe_photo_note
                    },
                ),
                style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 12.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }.testTag("pe-photo-note"),
            )
            // PRO08 — 사진 오류는 사진 자리 바로 아래
            if (ui.photo == PhotoWork.Failed) {
                EditErrorCard(
                    stringResource(R.string.pe_photo_failed_title), stringResource(R.string.pe_photo_failed_body),
                    action = stringResource(R.string.pe_pick_again), onAction = onRetryPhoto, tag = "pe-photo-error",
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(stringResource(R.string.pe_nickname), style = runTextStyle(17.sp, t.text, FontWeight.Bold))
            Spacer(Modifier.height(12.dp))
            NicknameField(field, enabled = !nameSaving, max = max)
            val atLimit = text.length >= max
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, start = 2.dp, end = 2.dp), verticalAlignment = Alignment.Top) {
                Text(
                    when {
                        text.isEmpty() -> stringResource(R.string.pe_nickname_empty)
                        atLimit -> stringResource(R.string.pe_nickname_limit, max)
                        else -> stringResource(R.string.pe_nickname_where)
                    },
                    style = runTextStyle(14.sp, if (atLimit) t.cyan else t.label, FontWeight.Medium, 1.45f),
                    modifier = Modifier.weight(1f).testTag("pe-hint"),
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    stringResource(R.string.pe_count, text.length, max),
                    style = runTextStyle(15.sp, if (atLimit) t.cyan else t.label, FontWeight.Bold),
                    modifier = Modifier.testTag("pe-count"),
                )
            }
            if (ui.name is NameWork.Failed) {
                EditErrorCard(stringResource(R.string.pe_name_failed_title), stringResource(R.string.pe_name_failed_body),
                    tag = "pe-name-error")
            }
            Spacer(Modifier.height(24.dp))
        }
        BlueWideButton(
            label = stringResource(
                when {
                    nameSaving -> R.string.pe_saving
                    ui.name is NameWork.Failed -> R.string.pe_save_again
                    else -> R.string.pe_save
                },
            ),
            onClick = onSave,
            enabled = dirty && !nameSaving && !photoSaving,
            busy = nameSaving,
            modifier = Modifier.padding(horizontal = EditGutter).padding(top = 8.dp, bottom = 16.dp).testTag("pe-save"),
        )
    }
}

/** 편집 화면 좌우 여백(지시서 24dp) */
private val EditGutter = 24.dp

/** PRO13 — 저장된 이름 · 사진을 읽는 중. 자리만(이름 · 숫자를 꾸미지 않는다) · 도는 표시 하나 · 뒤로는 그대로 */
@Composable
internal fun ProfileEditLoading(onBack: () -> Unit) {
    val t = runTone()
    val c = blueListColors()
    Column(Modifier.fillMaxSize().testTag("pe-loading")) {
        BluePageHeader(stringResource(R.string.pe_title), onBack, Modifier.padding(horizontal = EditGutter))
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = EditGutter),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(Modifier.size(120.dp).clip(CircleShape).background(c.skeleton.copy(alpha = 0.55f)).border(1.5.dp, c.edge, CircleShape))
            Spacer(Modifier.height(18.dp))
            Box(Modifier.size(width = 150.dp, height = 40.dp).clip(RoundedCornerShape(50)).background(c.skeleton.copy(alpha = 0.55f)))
            Spacer(Modifier.height(30.dp))
            Box(Modifier.fillMaxWidth().height(60.dp).clip(RoundedCornerShape(16.dp)).background(c.skeleton.copy(alpha = 0.55f)))
            Spacer(Modifier.height(60.dp))
            RunSpinner(Modifier.size(52.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.pe_blue_loading), style = runTextStyle(17.sp, t.text, FontWeight.SemiBold),
                textAlign = TextAlign.Center, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}

/** 원형 사진 120dp — 갤러리 사진 · 기본 이미지 · 파일이 없을 때의 사람 윤곽. 저장 중이면 안에 도는 표시 하나(가짜 백분율 없음) */
@Composable
private fun EditAvatar(avatarId: Int, photo: ImageBitmap?, saving: Boolean, modifier: Modifier = Modifier) {
    val t = runTone()
    val names = stringArrayResource(R.array.pe_avatar_names)
    val label = stringResource(R.string.pe_photo_cd)
    val savingLabel = stringResource(R.string.pe_photo_saving_chip)
    Box(
        modifier.size(120.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = if (avatarId in AvatarEmojis.indices) "$label, ${names[avatarId]}" else label
                if (saving) stateDescription = savingLabel
            }
            .testTag("pe-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        BlueAvatar(avatarId, photo, 120.dp, ring = t.cobaltText)
        if (saving) {
            Box(Modifier.matchParentSize().clip(CircleShape).background(Color(0x66031427)))
            RunSpinner(Modifier.size(44.dp))
        }
    }
}

/** 한 줄 입력 — 최대 길이는 저장과 같은 기준, 길면 가로로 흐른다(글씨를 줄이거나 자르지 않는다). 완료 키는 글자판만 닫는다 */
@Composable
private fun NicknameField(field: TextFieldState, enabled: Boolean, max: Int) {
    val t = runTone()
    val c = blueListColors()
    val focus = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(16.dp)
    BasicTextField(
        state = field,
        enabled = enabled,
        inputTransformation = InputTransformation.maxLength(max),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = runTextStyle(19.sp, if (enabled) t.text else t.muted, FontWeight.Medium),
        cursorBrush = SolidColor(t.cyan),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { focus.clearFocus() },
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().testTag("pe-nickname"),
        decorator = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 62.dp).clip(shape).background(c.face, shape)
                    .border(if (focused) 2.dp else 1.5.dp, if (focused) t.cyan else if (enabled) c.edge else t.divider, shape)
                    .padding(start = 20.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 16.dp)) {
                    if (field.text.isEmpty()) {
                        Text(stringResource(R.string.pe_nickname_placeholder), style = runTextStyle(19.sp, t.muted, FontWeight.Medium), maxLines = 1)
                    }
                    inner()
                }
                if (field.text.isNotEmpty()) {
                    IconButton(onClick = { field.clearText() }, enabled = enabled, modifier = Modifier.size(48.dp).testTag("pe-clear")) {
                        Icon(Icons.Filled.Close, stringResource(R.string.pe_nickname_clear), tint = if (enabled) t.label else t.muted)
                    }
                } else {
                    Spacer(Modifier.size(width = 16.dp, height = 48.dp))
                }
            }
        },
    )
}

/** 실패 안내 — i 아이콘 · 제목 · 한 줄 · (있으면 오른쪽) 다시 하기. 화면 낭독이 바로 읽는다 */
@Composable
private fun EditErrorCard(title: String, body: String, action: String? = null, onAction: () -> Unit = {}, tag: String) {
    val t = runTone()
    val c = blueListColors()
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().padding(top = 20.dp).clip(shape).background(c.face, shape).border(1.dp, c.edge, shape)
            .heightIn(min = 72.dp).padding(start = 18.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }.testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = t.cyan, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = runTextStyle(16.sp, t.text, FontWeight.Bold))
            Text(body, style = runTextStyle(14.sp, t.label, FontWeight.Medium))
        }
        if (action != null) {
            Box(Modifier.padding(horizontal = 6.dp).width(1.dp).height(40.dp).background(c.divider))
            Box(
                Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(10.dp))
                    .feedbackClickable(role = Role.Button, onClick = onAction).padding(horizontal = 10.dp).testTag("$tag-action"),
                contentAlignment = Alignment.Center,
            ) {
                Text(action, style = runTextStyle(16.sp, t.cyan, FontWeight.Bold), maxLines = 1)
            }
        }
    }
}

/** 사진 변경 메뉴(PRO03) — 앨범 · 기본 이미지 · 닫기만. 고르면 바로 저장된다는 것을 먼저 알린다 */
@Composable
internal fun PhotoActionsSheet(onAlbum: () -> Unit, onDefaults: () -> Unit, onDismiss: () -> Unit) {
    val t = runTone()
    BlueSheet(
        title = stringResource(R.string.pe_photo_sheet_title), onDismiss = onDismiss, modifier = Modifier.testTag("pe-photo-sheet"),
        showClose = false, centered = true,
        actions = {
            RunButton(stringResource(R.string.common_close), onDismiss, Modifier.testTag("pe-photo-close"), kind = RunButtonKind.Secondary)
        },
    ) {
        Text(stringResource(R.string.pe_photo_sheet_body), style = runTextStyle(15.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        Spacer(Modifier.height(6.dp))
        Column(Modifier.fillMaxWidth()) {
            SheetRow(Icons.Outlined.Image, stringResource(R.string.pe_from_album), onAlbum, "pe-from-album")
            Box(Modifier.fillMaxWidth().height(1.dp).background(t.secondaryEdge.copy(alpha = 0.35f)))
            SheetRow(Icons.Outlined.Person, stringResource(R.string.pe_default_images), onDefaults, "pe-default-images")
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val t = runTone()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 68.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = t.text, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(20.dp))
        Text(label, style = runTextStyle(18.sp, t.text, FontWeight.SemiBold), modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = t.text, modifier = Modifier.size(28.dp))
    }
}

/**
 * 기본 이미지(PRO04 · 14 · 15) — 기존 16개(순서 · 번호 그대로). 체크는 저장된 것에만(갤러리 사진을 쓰는 중이면 없음).
 * 누르면 바로 저장되고 저장되면 시트를 닫는다. 저장 중에는 고른 칸에만 도는 표시를 두고 모든 칸 · 닫기 · 바깥 · 뒤로를 막는다.
 * 못 하면 시트 안에서 알린다(선택 표시는 저장된 것 그대로).
 */
@Composable
internal fun DefaultImageSheet(
    current: Int,
    saving: Boolean,
    failed: Boolean,
    onPick: (Int) -> Unit,
    onDismiss: () -> Unit,
    pending: Int? = null,
) {
    val t = runTone()
    val c = blueListColors()
    val names = stringArrayResource(R.array.pe_avatar_names)
    val selectedLabel = stringResource(R.string.pe_selected)
    val savingLabel = stringResource(R.string.pe_photo_saving_chip)
    BlueSheet(
        title = stringResource(R.string.pe_default_images), onDismiss = onDismiss, modifier = Modifier.testTag("pe-default-sheet"),
        showClose = false, dismissible = !saving,
        actions = {
            RunButton(
                stringResource(R.string.common_close), onDismiss, Modifier.testTag("pe-default-close"),
                kind = RunButtonKind.Secondary, enabled = !saving,
            )
        },
    ) {
        Text(stringResource(R.string.pe_default_sheet_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
            AvatarEmojis.chunked(4).forEachIndexed { line, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEachIndexed { column, emoji ->
                        val id = line * 4 + column
                        val on = id == current
                        val busyHere = saving && pending == id
                        val shape = RoundedCornerShape(16.dp)
                        Box(
                            Modifier.weight(1f).heightIn(min = 68.dp).clip(shape)
                                .background(if (on) c.selectedFace else c.face, shape)
                                .border(if (on) 2.dp else 1.dp, if (on) t.cyan else c.edge.copy(alpha = 0.7f), shape)
                                .feedbackClickable(enabled = !saving, role = null) { onPick(id) }
                                .semantics {
                                    role = Role.RadioButton
                                    selected = on
                                    contentDescription = names[id]
                                    if (on) stateDescription = selectedLabel
                                    if (busyHere) stateDescription = savingLabel
                                }
                                .testTag("pe-avatar-$id"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 28.sp, modifier = Modifier.alpha(if (saving && !busyHere) 0.45f else 1f))
                            if (on) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(t.cyan),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF051A36), modifier = Modifier.size(13.dp))
                                }
                            }
                            if (busyHere) {
                                RunSpinner(Modifier.align(Alignment.BottomEnd).padding(8.dp).size(20.dp))
                            }
                        }
                    }
                }
            }
        }
        if (saving) {
            Text(
                stringResource(R.string.pe_blue_default_saving), style = runTextStyle(15.sp, t.text, FontWeight.Medium),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp).semantics { liveRegion = LiveRegionMode.Polite }
                    .testTag("pe-default-saving"),
            )
        }
        if (failed) {
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp).clip(shape).background(c.face, shape).border(1.dp, c.edge, shape)
                    .padding(horizontal = 18.dp, vertical = 14.dp)
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }.testTag("pe-default-error"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.ErrorOutline, contentDescription = null, tint = t.cyan, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.pe_blue_default_failed_title), style = runTextStyle(16.sp, t.text, FontWeight.Bold))
                    Text(stringResource(R.string.pe_blue_default_failed_body), style = runTextStyle(14.sp, t.label, FontWeight.Medium))
                }
            }
        }
    }
}

/** 수정 중 나가기(PRO09) — 저장하지 않은 닉네임만 버린다. 이번에 사진을 실제로 바꿨을 때만 "이미 바꾼 사진은 유지돼요" */
@Composable
internal fun LeaveSheet(photoChanged: Boolean, onKeep: () -> Unit, onDiscard: () -> Unit) {
    val t = runTone()
    BlueSheet(
        title = stringResource(R.string.pe_leave_title), onDismiss = onKeep, modifier = Modifier.testTag("pe-leave-sheet"),
        showClose = false, centered = true,
        actions = {
            RunButton(stringResource(R.string.pe_keep_editing), onKeep, Modifier.testTag("pe-keep-editing"))
            RunButton(stringResource(R.string.pe_discard_leave), onDiscard, Modifier.testTag("pe-discard"), kind = RunButtonKind.Secondary)
        },
    ) {
        Text(stringResource(R.string.pe_leave_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        if (photoChanged) {
            Text(stringResource(R.string.pe_leave_photo_kept), style = runTextStyle(16.sp, t.label, FontWeight.Medium), textAlign = TextAlign.Center)
        }
    }
}
