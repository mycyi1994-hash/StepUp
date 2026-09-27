package com.stepup.android.data.repo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 사진 · 기본 이미지 저장의 지금 단계 */
sealed interface PhotoWork {
    data object Idle : PhotoWork
    data object Saving : PhotoWork
    /** 저장했다 — 화면이 받아 "이번에 바꾼 사진"으로 적고 [ProfileEditor.acknowledgePhoto] 한다 */
    data object Saved : PhotoWork
    /** 저장하지 못했다 — 기존 사진 · 번호 · 리비전은 그대로다 */
    data object Failed : PhotoWork
}

/** 닉네임 저장의 지금 단계 */
sealed interface NameWork {
    data object Idle : NameWork
    data class Saving(val draft: String) : NameWork
    data object Saved : NameWork
    /** 저장하지 못했다 — 누른 때의 초안을 들고 있다(화면을 나갔다 와도 되살린다) */
    data class Failed(val draft: String) : NameWork
}

/** 닉네임 정규화 — 저장과 같은 규칙(앞뒤 공백을 떼고 최대 길이로 자른다) */
fun normalizedNickname(text: String, max: Int): String = text.trim().take(max)

/** 초안이 저장된 이름과 달라 "저장"할 것이 있는가 — 저장 때와 같은 정규화로 비교한다 */
fun nicknameChanged(stored: String, draft: String, max: Int): Boolean =
    normalizedNickname(draft, max) != normalizedNickname(stored, max)

/**
 * 프로필 수정의 저장(프로필 수정 v1, 2026-09-28) — 기존 저장 시점 그대로: 사진 · 기본 이미지는 고른 즉시, 닉네임은 "저장"을 눌러야.
 *
 * 저장은 앱 범위([scope])에서 끝까지 한다 — 편집 화면을 나가도 반쯤 저장되지 않고, 다시 들어오면 지금 단계를 본다.
 * 사진 작업과 닉네임 작업은 따로 — 서로의 단계 · 실패를 덮지 않는다.
 */
class ProfileEditor(
    private val saveNickname: suspend (String) -> Unit,
    private val saveAvatarId: suspend (Int) -> Unit,
    private val savePhotoFile: suspend (open: () -> InputStream?) -> Boolean,
    private val scope: CoroutineScope,
) {
    private val _photo = MutableStateFlow<PhotoWork>(PhotoWork.Idle)
    val photo: StateFlow<PhotoWork> = _photo.asStateFlow()

    /** 기본 이미지 시트의 저장 — 실패는 시트 안에서 알린다 */
    private val _defaultImage = MutableStateFlow<PhotoWork>(PhotoWork.Idle)
    val defaultImage: StateFlow<PhotoWork> = _defaultImage.asStateFlow()

    private val _name = MutableStateFlow<NameWork>(NameWork.Idle)
    val name: StateFlow<NameWork> = _name.asStateFlow()

    /**
     * 편집 화면을 새로 열 때 — 앞선 방문이 남긴 결과(저장됨 · 사진 실패)는 비운다. 저장 중이면 그대로 보이고,
     * 닉네임 저장 실패면 그 초안을 돌려준다(되살려 다시 저장할 수 있게).
     */
    fun startSession(): String? {
        if (_photo.value !is PhotoWork.Saving) _photo.value = PhotoWork.Idle
        if (_defaultImage.value !is PhotoWork.Saving) _defaultImage.value = PhotoWork.Idle
        return when (val n = _name.value) {
            is NameWork.Failed -> n.draft
            NameWork.Saved -> null.also { _name.value = NameWork.Idle }
            else -> null
        }
    }

    private fun photoBusy() = _photo.value is PhotoWork.Saving || _defaultImage.value is PhotoWork.Saving

    /** 앨범에서 고른 사진 — 저장 중이면 다시 받지 않는다 */
    fun savePhoto(open: () -> InputStream?): Boolean {
        if (photoBusy() || _name.value is NameWork.Saving) return false
        _photo.value = PhotoWork.Saving
        scope.launch {
            _photo.value = try {
                if (savePhotoFile(open)) PhotoWork.Saved else PhotoWork.Failed
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PhotoWork.Failed
            }
        }
        return true
    }

    /** 기본 이미지 — 지금 쓰는 것을 다시 누르면 저장하지 않는다 */
    fun saveDefaultImage(id: Int, current: Int?): Boolean {
        if (id == current || photoBusy() || _name.value is NameWork.Saving) return false
        _defaultImage.value = PhotoWork.Saving
        scope.launch {
            _defaultImage.value = try {
                saveAvatarId(id)
                PhotoWork.Saved
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                PhotoWork.Failed
            }
        }
        return true
    }

    /** 닉네임 — 누른 때의 초안을 저장한다. 저장 중에는 다시 받지 않는다 */
    fun saveName(draft: String): Boolean {
        if (_name.value is NameWork.Saving || photoBusy()) return false
        _name.value = NameWork.Saving(draft)
        scope.launch {
            _name.value = try {
                saveNickname(draft)
                NameWork.Saved
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                NameWork.Failed(draft)
            }
        }
        return true
    }

    /** 화면이 저장 결과를 받았다 */
    fun acknowledgePhoto() {
        if (_photo.value is PhotoWork.Saved) _photo.value = PhotoWork.Idle
    }

    fun acknowledgeDefaultImage() {
        if (_defaultImage.value is PhotoWork.Saved || _defaultImage.value is PhotoWork.Failed) _defaultImage.value = PhotoWork.Idle
    }

    fun acknowledgeName() {
        if (_name.value is NameWork.Saved) _name.value = NameWork.Idle
    }

    /** 초안을 버리고 나갈 때 — 닉네임 실패 표시를 지운다(저장된 이름은 그대로) */
    fun discardName() {
        if (_name.value is NameWork.Failed) _name.value = NameWork.Idle
    }
}

/**
 * 갤러리 사진 파일 — 고른 사진을 긴 변 512px 이하 JPEG(품질 90)로 줄여 앱 저장소에 둔다(기존 정책 그대로), 사진의 방향은 바로 세운다.
 *
 * 새 사진은 임시 파일에 먼저 만들고 다시 읽어 볼 수 있을 때만 기존 파일과 바꾼다. 설정(아바타 번호 · 리비전, [commit])을
 * 적지 못하면 기존 파일을 되돌린다 — 실패하면 기존 사진 · 번호 · 리비전이 그대로다. 파일을 바꾸고 설정을 적는 동안은
 * 취소되지 않는다(반쯤 바뀐 채로 남지 않게).
 */
class AvatarPhotoStore(
    private val dir: File,
    private val fileName: String,
    private val commit: suspend () -> Unit,
    private val maxSide: Int = 512,
) {
    suspend fun save(open: () -> InputStream?): Boolean = withContext(Dispatchers.IO) {
        val target = File(dir, fileName)
        val temp = File(dir, "$fileName.new")
        val backup = File(dir, "$fileName.old")
        try {
            val bitmap = decode(open) ?: return@withContext false
            val written = temp.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            if (!written || !readable(temp)) return@withContext false
            withContext(NonCancellable) { swap(target, temp, backup) }
        } finally {
            temp.delete()
        }
    }

    private suspend fun swap(target: File, temp: File, backup: File): Boolean {
        backup.delete()
        val hadOld = target.exists()
        if (hadOld && !target.renameTo(backup)) return false
        if (!temp.renameTo(target)) {
            if (hadOld) backup.renameTo(target)
            return false
        }
        return try {
            commit()
            backup.delete()
            true
        } catch (_: Exception) {
            target.delete()
            if (hadOld) backup.renameTo(target)
            false
        }
    }

    /** 크기만 먼저 읽어 줄여 읽고(큰 사진도 메모리를 넘기지 않게), 방향을 세우고, 긴 변을 [maxSide] 이하로 */
    private fun decode(open: () -> InputStream?): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) } ?: return null
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val raw = open()?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: return null
        val rotation = runCatching {
            open()?.use {
                when (ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        val scale = minOf(maxSide.toFloat() / raw.width, maxSide.toFloat() / raw.height, 1f)
        if (scale >= 1f && rotation == 0f) return raw
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postRotate(rotation)
        }
        return Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
    }

    private fun readable(file: File): Boolean {
        val check = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, check)
        return check.outWidth > 0 && check.outHeight > 0
    }
}
