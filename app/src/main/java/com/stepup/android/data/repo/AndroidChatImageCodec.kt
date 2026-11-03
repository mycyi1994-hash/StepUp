package com.stepup.android.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 안드로이드 채팅 사진 — 예전 CrewChatRepository 의 Bitmap 코드 그대로(공용 저장소가 ChatImageCodec 으로 부른다).
 * 공용 화면이 그리는 모양(ImageBitmap)으로 주고받는다 — 속은 안드로이드 Bitmap 이다.
 */
object AndroidChatImageCodec : ChatImageCodec<ImageBitmap> {
    override fun byteCount(img: ImageBitmap): Int = img.asAndroidBitmap().byteCount

    override fun decodeFile(path: String): ImageBitmap? = runCatching { BitmapFactory.decodeFile(path)?.asImageBitmap() }.getOrNull()

    override fun decodeBase64(data: String): ImageBitmap? = runCatching {
        val bytes = Base64.decode(data, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    }.getOrNull()

    override fun encode(image: ImageBitmap, dir: String, maxSide: Int, maxBase64: Int): ChatPhoto? {
        val source = image.asAndroidBitmap()
        var side = maxSide
        while (side >= 480) {
            val scale = side.toFloat() / maxOf(source.width, source.height)
            val bitmap = if (scale >= 1f) source else Bitmap.createScaledBitmap(
                source, (source.width * scale).toInt().coerceAtLeast(1), (source.height * scale).toInt().coerceAtLeast(1), true,
            )
            var quality = 84
            while (quality >= 46) {
                val bytes = ByteArrayOutputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                    out.toByteArray()
                }
                val text = Base64.encodeToString(bytes, Base64.NO_WRAP)
                if (text.length <= maxBase64) {
                    File(dir).mkdirs()
                    val file = File(dir, "photo-${UUID.randomUUID()}.jpg")
                    file.writeBytes(bytes)
                    return ChatPhoto(text, file.path, bitmap.width, bitmap.height)
                }
                quality -= 12
            }
            side -= 240
        }
        return null
    }

    override fun clearDir(dir: String) {
        File(dir).listFiles()?.forEach { it.delete() }
    }
}

/** 보낼 사진의 앱 안 파일(예전 ChatPhoto.file) */
val ChatPhoto.file: File get() = File(path)

/** 예전 모양 그대로 만든다(파일로) */
fun ChatPhoto(data: String, file: File, width: Int, height: Int): ChatPhoto = ChatPhoto(data, file.path, width, height)

/** 안드로이드 채팅 저장소 */
typealias AndroidCrewChat = CrewChatRepository<ImageBitmap>

/**
 * 고른 사진을 보낼 모양으로 — 긴 변 PHOTO_SIDE 까지 줄이고 크루 대표 사진과 같은 기술 한도(base64 200,000자 —
 * 넉넉히 180,000자) 안의 JPEG 로. 읽을 수 없으면 null, 접근이 막혔으면 [CrewPhotos.Loaded.Denied].
 */
suspend fun CrewChatRepository<ImageBitmap>.preparePhoto(context: Context, uri: Uri): Pair<ChatPhoto?, CrewPhotos.Loaded> {
    val loaded = CrewPhotos.load(context, uri)
    val source = (loaded as? CrewPhotos.Loaded.Ok)?.bitmap ?: return null to loaded
    val photo = withContext(Dispatchers.Default) { photoOf(source.asImageBitmap()) }
    return photo to loaded
}

/** 예전 decodeFile(File) */
fun CrewChatRepository<ImageBitmap>.decodeFile(file: File): ImageBitmap? = decodeFile(file.path)
