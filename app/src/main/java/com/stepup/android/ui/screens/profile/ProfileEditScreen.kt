package com.stepup.android.ui.screens.profile

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
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
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.avatarEmoji
import com.stepup.android.ui.components.rememberCustomAvatar
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpColors
import com.stepup.android.ui.theme.StepUpDesign
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
 * 프로필 수정(2026-09-28 전달본, docs/redesign/profile-edit-v1) — 사진과 닉네임만. 하단 탭 없이 뒤로 가면 내 정보.
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
        Box(Modifier.fillMaxSize().testTag("pe-loading"))
        return
    }
    val field = rememberSaveable(saver = TextFieldState.Saver) { TextFieldState(viewModel.restoredDraft ?: storedName) }
    val focus = LocalFocusManager.current
    var sheet by rememberSaveable { mutableStateOf(EditSheet.None) }
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
            onPick = { id ->
                // 지금 쓰는 이미지를 다시 누르면 저장하지 않고 닫는다
                if (!viewModel.saveDefaultImage(id) && id == currentAvatar) sheet = EditSheet.None
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

@Composable
fun ProfileEditContent(
    ui: ProfileEditUi,
    field: TextFieldState,
    onBack: () -> Unit = {},
    onChangePhoto: () -> Unit = {},
    onRetryPhoto: () -> Unit = {},
    onSave: () -> Unit = {},
) {
    val p = settingsPalette()
    val max = UserPrefs.NICKNAME_MAX
    val text = field.text.toString()
    val photoSaving = ui.photo == PhotoWork.Saving
    val nameSaving = ui.name is NameWork.Saving
    val dirty = nicknameChanged(ui.stored, text, max)
    Column(Modifier.fillMaxSize().imePadding()) {
        SecondaryHeader(
            onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.pe_title),
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter),
        )
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = StepUpDesign.Gutter),
        ) {
            Spacer(Modifier.height(28.dp))
            EditAvatar(ui.avatarId, ui.customPhoto, photoSaving, Modifier.align(Alignment.CenterHorizontally))
            Box(
                Modifier.align(Alignment.CenterHorizontally).padding(top = 10.dp).heightIn(min = 48.dp)
                    .feedbackClickable(enabled = !photoSaving && !nameSaving, role = Role.Button, onClick = onChangePhoto)
                    .testTag("pe-change-photo"),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.clip(RoundedCornerShape(18.dp)).background(chipFace()).padding(horizontal = 22.dp, vertical = 8.dp)) {
                    Text(stringResource(if (photoSaving) R.string.pe_photo_saving_chip else R.string.pe_change_photo),
                        color = if (photoSaving) p.secondary else p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Text(
                stringResource(
                    when {
                        photoSaving -> R.string.pe_photo_saving
                        ui.photoChanged && ui.photo == PhotoWork.Idle -> R.string.pe_photo_saved
                        else -> R.string.pe_photo_note
                    },
                ),
                color = p.secondary, fontSize = 13.sp, textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 2.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }.testTag("pe-photo-note"),
            )
            Spacer(Modifier.height(30.dp))
            Text(stringResource(R.string.pe_nickname), color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(14.dp))
            NicknameField(field, enabled = !nameSaving, max = max)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.Top) {
                Text(
                    when {
                        text.isEmpty() -> stringResource(R.string.pe_nickname_empty)
                        text.length >= max -> stringResource(R.string.pe_nickname_limit, max)
                        else -> stringResource(R.string.pe_nickname_where)
                    },
                    color = p.secondary, fontSize = 13.sp, lineHeight = 1.45.em, modifier = Modifier.weight(1f).testTag("pe-hint"),
                )
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.pe_count, text.length, max),
                    color = if (text.length >= max) p.accent else p.secondary, fontSize = 13.sp, modifier = Modifier.testTag("pe-count"))
            }
            if (ui.photo == PhotoWork.Failed) {
                EditErrorCard(
                    stringResource(R.string.pe_photo_failed_title), stringResource(R.string.pe_photo_failed_body),
                    action = stringResource(R.string.pe_pick_again), onAction = onRetryPhoto, tag = "pe-photo-error",
                )
            }
            if (ui.name is NameWork.Failed) {
                EditErrorCard(stringResource(R.string.pe_name_failed_title), stringResource(R.string.pe_name_failed_body),
                    tag = "pe-name-error")
            }
            Spacer(Modifier.height(24.dp))
        }
        EditSaveButton(
            text = stringResource(
                when {
                    nameSaving -> R.string.pe_saving
                    ui.name is NameWork.Failed -> R.string.pe_save_again
                    else -> R.string.pe_save
                },
            ),
            enabled = dirty && !nameSaving && !photoSaving,
            onClick = onSave,
            modifier = Modifier.padding(horizontal = StepUpDesign.Gutter).padding(top = 8.dp, bottom = 16.dp).fillMaxWidth()
                .testTag("pe-save"),
        )
    }
}

/** 원형 사진 100dp — 갤러리 사진 · 기본 이미지(기존 이모지) · 사진 파일이 없을 때의 사람 모양. 저장 중이면 안에 진행 표시만 */
@Composable
private fun EditAvatar(avatarId: Int, photo: ImageBitmap?, saving: Boolean, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    val names = stringArrayResource(R.array.pe_avatar_names)
    val label = stringResource(R.string.pe_photo_cd)
    val savingLabel = stringResource(R.string.pe_photo_saving_chip)
    Box(
        modifier.size(100.dp).clip(CircleShape)
            .background(Brush.linearGradient(listOf(Color(0xFF223D63), Color(0xFF101D31))))
            .semantics {
                contentDescription = if (avatarId in AvatarEmojis.indices) "$label, ${names[avatarId]}" else label
                if (saving) stateDescription = savingLabel
            }
            .testTag("pe-avatar"),
        contentAlignment = Alignment.Center,
    ) {
        when {
            avatarId == UserPrefs.AVATAR_CUSTOM && photo != null ->
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            avatarId == UserPrefs.AVATAR_CUSTOM -> Icon(Icons.Outlined.Person, null, tint = p.accent, modifier = Modifier.size(58.dp))
            else -> Text(avatarEmoji(avatarId), fontSize = 46.sp)
        }
        if (saving) {
            Box(Modifier.matchParentSize().background(Color(0x66050912)))
            CircularProgressIndicator(Modifier.size(38.dp), color = p.accent, strokeWidth = 2.5.dp)
        }
    }
}

/** 한 줄 입력 — 최대 길이는 저장과 같은 기준, 길면 가로로 흐른다(글씨를 줄이거나 자르지 않는다). 완료 키는 글자판만 닫는다 */
@Composable
private fun NicknameField(field: TextFieldState, enabled: Boolean, max: Int) {
    val p = settingsPalette()
    val focus = LocalFocusManager.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(15.dp)
    BasicTextField(
        state = field,
        enabled = enabled,
        inputTransformation = InputTransformation.maxLength(max),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = TextStyle(color = if (enabled) p.text else p.secondary, fontSize = 18.sp),
        cursorBrush = SolidColor(p.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        onKeyboardAction = { focus.clearFocus() },
        interactionSource = interaction,
        modifier = Modifier.fillMaxWidth().testTag("pe-nickname"),
        decorator = { inner ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(fieldFace())
                    .border(1.dp, if (focused) p.accent else Color.Transparent, shape)
                    .padding(start = 20.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 16.dp)) {
                    if (field.text.isEmpty()) {
                        Text(stringResource(R.string.pe_nickname_placeholder), color = p.secondary.copy(alpha = 0.8f), fontSize = 18.sp,
                            maxLines = 1)
                    }
                    inner()
                }
                if (field.text.isNotEmpty()) {
                    IconButton(onClick = { field.clearText() }, enabled = enabled, modifier = Modifier.size(48.dp).testTag("pe-clear")) {
                        Icon(Icons.Filled.Close, stringResource(R.string.pe_nickname_clear), tint = p.secondary)
                    }
                } else {
                    Spacer(Modifier.size(width = 16.dp, height = 48.dp))
                }
            }
        },
    )
}

/** 실패 안내 — 아이콘 · 제목 · 한 줄 · (있으면) 다시 하기. 화면 낭독이 바로 읽는다 */
@Composable
private fun EditErrorCard(title: String, body: String, action: String? = null, onAction: () -> Unit = {}, tag: String) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().padding(top = 22.dp).clip(RoundedCornerShape(15.dp))
            .background(if (StepUpColors.dark) Color(0xFF182033) else p.surface)
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = if (action == null) 16.dp else 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }.testTag(tag),
    ) {
        Icon(Icons.Outlined.Info, contentDescription = null, tint = p.text, modifier = Modifier.padding(top = 1.dp).size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = p.text, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(body, color = p.secondary, fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
            if (action != null) {
                Box(
                    Modifier.heightIn(min = 48.dp).feedbackClickable(role = Role.Button, onClick = onAction).testTag("$tag-action"),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(action, color = p.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** 아래 주 버튼 — 저장할 것이 없거나 저장 중이면 누를 수 없고 면이 가라앉는다 */
@Composable
private fun EditSaveButton(text: String, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    if (enabled) {
        SettingsPrimaryButton(text, onClick, modifier)
        return
    }
    val shape = RoundedCornerShape(17.dp)
    Box(
        modifier.heightIn(min = 56.dp).clip(shape)
            .background(if (StepUpColors.dark) Color(0xFF182438) else p.surface, shape)
            .feedbackClickable(enabled = false, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (StepUpColors.dark) Color(0xFF8293AD) else p.secondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** 사진 변경 메뉴(03) — 앨범 · 기본 이미지. 고르면 바로 저장된다는 것을 먼저 알린다 */
@Composable
internal fun PhotoActionsSheet(onAlbum: () -> Unit, onDefaults: () -> Unit, onDismiss: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.pe_photo_sheet_title), onDismiss = onDismiss, modifier = Modifier.testTag("pe-photo-sheet"),
        showClose = false,
        actions = { SettingsSecondaryButton(stringResource(R.string.common_close), onDismiss, Modifier.fillMaxWidth().testTag("pe-photo-close")) },
    ) {
        Text(stringResource(R.string.pe_photo_sheet_body), color = p.secondary, fontSize = 14.sp)
        Column {
            SheetRow(Icons.Outlined.Image, stringResource(R.string.pe_from_album), onAlbum, "pe-from-album")
            Box(Modifier.padding(start = 40.dp).fillMaxWidth().height(1.dp).background(p.divider))
            SheetRow(Icons.Outlined.AccountCircle, stringResource(R.string.pe_default_images), onDefaults, "pe-default-images")
        }
    }
}

@Composable
private fun SheetRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, tag: String) {
    val p = settingsPalette()
    Row(
        Modifier.fillMaxWidth().heightIn(min = 60.dp).feedbackClickable(role = Role.Button, onClick = onClick).testTag(tag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.secondary)
    }
}

/**
 * 기본 이미지(04) — 기존 16개(순서 · 번호 그대로). 체크는 저장된 것에만(갤러리 사진을 쓰는 중이면 없음).
 * 누르면 바로 저장, 저장되면 시트를 닫는다. 못 하면 시트를 두고 알린다(선택 표시는 저장된 것 그대로).
 */
@Composable
internal fun DefaultImageSheet(current: Int, saving: Boolean, failed: Boolean, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val p = settingsPalette()
    val names = stringArrayResource(R.array.pe_avatar_names)
    val selectedLabel = stringResource(R.string.pe_selected)
    SettingsSheet(
        title = stringResource(R.string.pe_default_images), onDismiss = onDismiss, modifier = Modifier.testTag("pe-default-sheet"),
        showClose = false, dismissible = !saving,
        actions = {
            SettingsSecondaryButton(stringResource(R.string.common_close), onDismiss, Modifier.fillMaxWidth().testTag("pe-default-close"),
                enabled = !saving)
        },
    ) {
        Text(stringResource(R.string.pe_default_sheet_body), color = p.secondary, fontSize = 14.sp)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
            AvatarEmojis.chunked(4).forEachIndexed { line, row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEachIndexed { column, emoji ->
                        val id = line * 4 + column
                        val on = id == current
                        Box(
                            Modifier.weight(1f).heightIn(min = 64.dp).clip(RoundedCornerShape(16.dp))
                                .background(if (on) selectedCell() else cellFace())
                                .feedbackClickable(enabled = !saving, role = null) { onPick(id) }
                                .semantics {
                                    role = Role.RadioButton
                                    selected = on
                                    contentDescription = names[id]
                                    if (on) stateDescription = selectedLabel
                                }
                                .testTag("pe-avatar-$id"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emoji, fontSize = 28.sp)
                            if (on) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(20.dp).clip(CircleShape).background(p.accent),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Filled.Check, contentDescription = null, tint = Color(0xFF081223), modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
        if (failed) {
            Text(stringResource(R.string.pe_default_failed), color = p.text, fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                    .background(if (StepUpColors.dark) Color(0xFF182033) else p.surface).padding(14.dp)
                    .semantics { liveRegion = LiveRegionMode.Polite }.testTag("pe-default-error"))
        }
        if (saving) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(22.dp).testTag("pe-default-saving"), color = p.accent, strokeWidth = 2.dp)
            }
        }
    }
}

/** 수정 중 나가기(09) — 저장하지 않은 닉네임만 버린다. 이미 바꾼 사진은 그대로라는 것도 알린다 */
@Composable
internal fun LeaveSheet(photoChanged: Boolean, onKeep: () -> Unit, onDiscard: () -> Unit) {
    val p = settingsPalette()
    SettingsSheet(
        title = stringResource(R.string.pe_leave_title), onDismiss = onKeep, modifier = Modifier.testTag("pe-leave-sheet"),
        showClose = false,
        actions = {
            SettingsPrimaryButton(stringResource(R.string.pe_keep_editing), onKeep, Modifier.fillMaxWidth().testTag("pe-keep-editing"))
            SettingsSecondaryButton(stringResource(R.string.pe_discard_leave), onDiscard, Modifier.fillMaxWidth().testTag("pe-discard"))
        },
    ) {
        Text(stringResource(R.string.pe_leave_body), color = p.secondary, fontSize = 15.sp)
        if (photoChanged) Text(stringResource(R.string.pe_leave_photo_kept), color = p.secondary, fontSize = 15.sp)
    }
}

@Composable
private fun chipFace(): Color = if (StepUpColors.dark) Color(0xFF1A2B45) else settingsPalette().surface

@Composable
private fun fieldFace(): Color = if (StepUpColors.dark) Color(0xFF0E192A) else settingsPalette().surface

@Composable
private fun cellFace(): Color = if (StepUpColors.dark) Color(0xFF172538) else settingsPalette().surface

@Composable
private fun selectedCell(): Color = if (StepUpColors.dark) Color(0xFF2A4166) else settingsPalette().accent.copy(alpha = 0.25f)
