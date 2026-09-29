package com.stepup.android.data.repo

import com.stepup.android.data.remote.AnnouncementRow
import com.stepup.android.data.remote.AnnouncementSource
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.serverJson
import java.io.File
import java.io.IOException
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * 앱 공지(알림 › 공지, 알림·공지 v1 15~20 · 23).
 *
 * 외부 러닝 뉴스 · 대회(NewsScreen · RunningFeedRepository)와 다른 것이다 — 운영자가 서버 표에 직접 올리는
 * StepUp 안내만 보인다. 앱에 예시 공지를 심지 않는다: 서버가 빈 목록을 주면 "아직 공지가 없어요".
 *
 * 받은 목록은 폰에 사본으로 둔다. 새로 받기가 실패하면 사본을 계속 보이고 "새 공지를 확인하지 못했어요"를 알린다.
 * 사본도 없으면 "불러오지 못했어요". 서버가 목록에서 뺐거나 하나만 물었을 때 주지 않은 글은(내림 · 비공개)
 * 사본보다 서버 답이 우선이다 — 사본에서도 지운다. 닿지 못한 것은 내린 것으로 보지 않는다.
 */
class AnnouncementRepository(
    private val source: AnnouncementSource,
    private val cache: AnnouncementCache,
) {
    private val lock = Mutex()
    private var savedRead = false

    /** 사본과 같은 것 — 서버가 준 모양 그대로(언어별 글) */
    private var rows: List<AnnouncementRow>? = null

    private val _board = MutableStateFlow(NoticeBoard())
    val board: StateFlow<NoticeBoard> = _board.asStateFlow()

    /** 사본을 먼저 보이고 서버에서 새로 받는다 */
    suspend fun refresh() {
        lock.withLock {
            loadSaved()
            _board.value = _board.value.copy(loading = true)
        }
        val result = try {
            source.list()
        } catch (cancelled: CancellationException) {
            _board.value = _board.value.copy(loading = false)
            throw cancelled
        }
        lock.withLock {
            if (result is ServerResult.Ok) {
                rows = result.value
                save(result.value)
                _board.value = NoticeBoard(items = result.value.mapNotNull { it.toDomain() })
            } else {
                _board.value = _board.value.copy(loading = false, refreshFailed = true)
            }
        }
    }

    /** 목록에 있는 공지 — 상세를 서버에 확인하는 동안 먼저 보인다 */
    fun peek(id: Long): Announcement? = _board.value.items?.firstOrNull { it.id == id }

    /**
     * 공지 하나를 서버에 다시 묻는다. 서버가 주지 않으면 볼 수 없는 공지(19)로 알리고 목록 · 사본에서도 뺀다.
     * 닿지 못하면 받아 둔 내용을 보인다 — 받아 둔 것도 없으면 못 불러왔다고 알린다.
     */
    suspend fun detail(id: Long): NoticeDetail {
        lock.withLock { loadSaved() }
        val saved = peek(id)
        return when (val result = source.one(id)) {
            is ServerResult.Ok -> {
                val row = result.value
                val notice = row?.toDomain()
                lock.withLock { if (row == null) forget(id) else if (notice != null) keep(row) }
                if (notice == null) NoticeDetail.Unavailable else NoticeDetail.Ready(notice)
            }
            else -> if (saved != null) NoticeDetail.Ready(saved, saved = true) else NoticeDetail.Failed
        }
    }

    private suspend fun loadSaved() {
        if (savedRead) return
        savedRead = true
        val saved = try {
            cache.read()
        } catch (_: IOException) {
            null
        }
        if (saved != null && rows == null) {
            rows = saved
            _board.value = _board.value.copy(items = saved.mapNotNull { it.toDomain() })
        }
    }

    private suspend fun save(next: List<AnnouncementRow>) {
        try {
            cache.write(next)
        } catch (_: IOException) {
            // 사본을 못 남겨도 지금 받은 목록은 보인다 — 다음에 열 때 다시 받는다
        }
    }

    private suspend fun forget(id: Long) {
        val current = rows ?: return
        if (current.none { it.id == id }) return
        rows = current.filterNot { it.id == id }.also { save(it) }
        _board.value = _board.value.copy(items = _board.value.items?.filterNot { it.id == id })
    }

    private suspend fun keep(row: AnnouncementRow) {
        val current = rows ?: return
        if (current.none { it.id == row.id }) return
        rows = current.map { if (it.id == row.id) row else it }.also { save(it) }
        _board.value = _board.value.copy(items = rows?.mapNotNull { it.toDomain() })
    }
}

/** 공지 아래 버튼 하나가 갈 수 있는 앱 안 화면 — 정해 둔 것 말고는 버튼을 두지 않는다 */
enum class NoticeAction {
    /** 신발 뽑기(무료 · 상급) — 옮겨 갈 뿐 뽑기 기회를 쓰지 않는다 */
    DRAW,

    /** 내 러닝 기록 */
    RUN_HISTORY,

    /** 설정 › 알림 */
    NOTIFICATION_SETTINGS,

    /** 설정 › 개인정보 · 앱 권한 */
    PRIVACY_SETTINGS;

    companion object {
        fun of(raw: String?): NoticeAction? = entries.firstOrNull { it.name == raw }
    }
}

/** 공지 한 편. 글은 언어 코드별로 받아 두고 보일 때 고른다(언어를 바꿔도 다시 받지 않는다) */
data class Announcement(
    val id: Long,
    val titles: Map<String, String>,
    val bodies: Map<String, String>,
    /** 게시 시각(epoch ms) — 서버의 published_at */
    val publishedAt: Long,
    val action: NoticeAction?,
) {
    fun title(language: String): String = pickText(titles, language)
    fun body(language: String): String = pickText(bodies, language)
}

/** 공지 목록 — [items] 가 null 이면 아직 한 번도 받지 못했다(사본도 없다) */
data class NoticeBoard(
    val items: List<Announcement>? = null,
    val loading: Boolean = false,
    /** 마지막 새로 받기가 실패했다 — 보이는 목록은 사본이다 */
    val refreshFailed: Boolean = false,
)

/** 공지 상세를 서버에 물은 결과 */
sealed interface NoticeDetail {
    data object Loading : NoticeDetail

    /** @param saved 서버에 확인하지 못해 받아 둔 내용을 보인다 */
    data class Ready(val notice: Announcement, val saved: Boolean = false) : NoticeDetail

    /** 서버가 이 공지를 주지 않았다 — 내렸거나 비공개로 바뀌었다(19) */
    data object Unavailable : NoticeDetail

    /** 서버에 닿지 못했고 받아 둔 내용도 없다 */
    data object Failed : NoticeDetail
}

/** 지금 언어 → 한국어 → 영어 → 남은 아무 글 */
internal fun pickText(texts: Map<String, String>, language: String): String {
    val wanted = language.lowercase()
    return texts[wanted]
        ?: texts.entries.firstOrNull { it.key.lowercase().substringBefore('-') == wanted }?.value
        ?: texts["ko"]
        ?: texts["en"]
        ?: texts.values.firstOrNull().orEmpty()
}

internal fun AnnouncementRow.toDomain(): Announcement? {
    val published = try {
        OffsetDateTime.parse(publishedAt).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        return null
    }
    val titles = title.texts()
    if (titles.isEmpty()) return null
    return Announcement(id, titles, body.texts(), published, NoticeAction.of(action))
}

private fun JsonObject.texts(): Map<String, String> = entries.mapNotNull { (language, value) ->
    val text = (value as? JsonPrimitive)?.takeIf { it.isString }?.content?.trim()
    if (text.isNullOrEmpty()) null else language to text
}.toMap()

/** 공지 본문의 한 덩어리 — 빈 줄로 문단을 나누고, "## " 줄은 소제목, "---" 한 줄은 구분선 */
sealed interface NoticeBlock {
    data class Heading(val text: String) : NoticeBlock
    data class Paragraph(val lines: List<String>) : NoticeBlock
    data object Rule : NoticeBlock
}

fun noticeBlocks(body: String): List<NoticeBlock> {
    val blocks = mutableListOf<NoticeBlock>()
    val lines = mutableListOf<String>()
    fun flush() {
        if (lines.isNotEmpty()) {
            blocks += NoticeBlock.Paragraph(lines.toList())
            lines.clear()
        }
    }
    body.replace("\r\n", "\n").lineSequence().map { it.trim() }.forEach { line ->
        when {
            line.isEmpty() -> flush()
            line == "---" -> {
                flush()
                blocks += NoticeBlock.Rule
            }
            line.startsWith("## ") -> {
                flush()
                line.removePrefix("## ").trim().takeIf { it.isNotEmpty() }?.let { blocks += NoticeBlock.Heading(it) }
            }
            else -> lines += line
        }
    }
    flush()
    return blocks
}

/** 받은 공지 목록의 사본 */
interface AnnouncementCache {
    /** 없거나 읽지 못하면 null */
    suspend fun read(): List<AnnouncementRow>?

    @Throws(IOException::class)
    suspend fun write(rows: List<AnnouncementRow>)
}

/** 앱 파일 하나에 둔다 — 다 쓴 다음에 바꿔 끼워 반쯤 쓴 사본이 남지 않게 한다 */
class FileAnnouncementCache(private val file: File) : AnnouncementCache {
    private val serializer = ListSerializer(AnnouncementRow.serializer())

    override suspend fun read(): List<AnnouncementRow>? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null
        try {
            serverJson.decodeFromString(serializer, file.readText())
        } catch (_: IllegalArgumentException) {
            null
        } catch (_: IOException) {
            null
        }
    }

    override suspend fun write(rows: List<AnnouncementRow>) = withContext(Dispatchers.IO) {
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(serverJson.encodeToString(serializer, rows))
        if (!temp.renameTo(file)) {
            file.delete()
            if (!temp.renameTo(file)) throw IOException("공지 사본을 바꿔 끼우지 못했습니다")
        }
    }
}
