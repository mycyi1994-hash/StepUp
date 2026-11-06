package com.stepup.android.domain

/**
 * 동네 이야기(목록형 커뮤니티, 2026-09-27 사용자 결정) — 위에 지도, 아래에 가까운 곳의 글.
 *
 * 글마다 쓴 사람이 고른 **공개 장소**가 붙는다. 거리는 읽는 사람의 위치(또는 직접 고른 지역)에서
 * 그 장소까지 잰다. 쓴 사람의 실시간 위치도, 번개의 "함께 달릴 거리"(Post.distanceKm)도 아니다.
 */

/** 글에 붙인 공개 장소 */
data class StoryPlace(
    val name: String,
    /** 지도에서 장소를 확인할 때 보이는 주소. 모르면 빈 문자열 */
    val address: String,
    val lat: Double,
    val lng: Double,
) {
    val point: GeoPoint get() = GeoPoint(lat, lng)

    /** 같은 장소로 묶는 열쇠 — 이름이 같고 좌표가 약 10m 안이면 한 곳이다 */
    val key: String get() = "$name@${"%.4f".format(java.util.Locale.ROOT, lat)},${"%.4f".format(java.util.Locale.ROOT, lng)}"
}

/** 이 글에 붙은 공개 장소. 이름이나 좌표가 없으면(예전 글 · 좌표 없이 쓴 번개) null */
val Post.storyPlace: StoryPlace?
    get() {
        val lat = lat ?: return null
        val lng = lng ?: return null
        if (place.isBlank()) return null
        return StoryPlace(place, placeAddress, lat, lng)
    }

/**
 * 쓰다 만 글 — 본문 · 장소 · 고치던 글 번호(새 글이면 0)를 함께 둔다. 계정마다 따로, 폰에만 남는다.
 * 붙인 러닝도 함께 둔다 — 다시 열었을 때 기간이 지났으면 본문 · 장소는 그대로 두고 코스만 정리하게 한다.
 */
data class StoryDraft(
    val text: String,
    val place: StoryPlace?,
    val editingId: Long,
    val savedAt: Long,
    val run: StoryAttachment? = null,
    /** 고치는 글에서 러닝을 바꾸거나 뺐다 — 아니면 서버의 첨부를 그대로 둔다 */
    val runChanged: Boolean = false,
    /** 글쓰기 요청 키 — 결과를 모른 채 저장했다가 다시 올려도 글이 두 편 생기지 않게 */
    val clientKey: String = "",
) {
    val isEdit: Boolean get() = editingId > 0

    /** "작성하던 글이 있어요" 아래 한 줄 */
    val preview: String get() = text.trim().lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trim()
}

/** 주변 범위 — 기본 1km */
enum class StoryRange(val meters: Int) {
    M500(500),
    KM1(1_000),
    KM3(3_000),
    ;

    companion object {
        val DEFAULT = KM1
        fun of(name: String?): StoryRange = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}

/**
 * 글을 보는 기준점 — 내 위치 또는 직접 고른 지역.
 * 위치를 모르면 기준점이 없다(거리를 지어내지 않는다).
 */
data class StoryOrigin(
    val point: GeoPoint,
    /** 목록 위에 보일 이름 — "여의도" 처럼. 모르면 빈 문자열 */
    val label: String,
    /** 직접 고른 지역이면 true — 문구가 "내 위치에서" 대신 "선택한 지역에서" 가 된다 */
    val manual: Boolean,
)

/**
 * 신고 사유 — 시안의 네 가지. 서버(content_reports.reason)의 값과 **명시적으로** 짝짓는다.
 * 서버 목록에 없는 "개인정보 노출"은 OTHER 로 보내고, 원래 사유를 note 에 적어 검토하는 사람이 본다.
 */
enum class StoryReportReason(val serverReason: String, val note: String) {
    SPAM("SPAM", ""),
    ABUSE("ABUSE", ""),
    PRIVACY("OTHER", "PRIVACY"),
    OTHER("OTHER", ""),
}

/**
 * 본문 한 칸을 서버의 제목 · 본문으로 나누고 다시 잇는다.
 *
 * 글쓰기에는 제목 칸이 없다. 첫 줄이 제목, 나머지가 본문이다. 서버 제목은 120자까지라, 첫 줄이
 * 더 길면 120자에서 잘라 나머지를 본문 앞에 둔다. 본문은 제목 **바로 뒤의 글자부터** 담는다 —
 * 줄바꿈으로 끝난 첫 줄이면 본문이 줄바꿈으로 시작한다. 그래서 [join] 이 원문을 그대로 되살린다.
 */
object StoryText {
    /** 글자 수 상한(시안 1,000자) — 코드포인트로 센다(이모지 하나가 한 글자) */
    const val MAX_CHARS = 1_000

    /** 서버 제목 상한(posts.title) */
    const val TITLE_MAX_CHARS = 120

    /** 댓글 상한(시안 200자, 서버는 1,000자) */
    const val COMMENT_MAX_CHARS = 200

    fun length(text: String): Int = text.codePointCount(0, text.length)

    /** 앞뒤 공백을 걷은 글. 공백뿐이면 빈 문자열 — 올릴 수 없다 */
    fun normalize(text: String): String = text.trim()

    fun canPost(text: String): Boolean {
        val clean = normalize(text)
        return clean.isNotEmpty() && length(clean) <= MAX_CHARS
    }

    /** 글 → (제목, 본문). [text] 는 [normalize] 한 것이어야 한다 */
    fun split(text: String): Pair<String, String> {
        val newline = text.indexOf('\n')
        val firstLineEnd = if (newline < 0) text.length else newline
        val firstLine = text.substring(0, firstLineEnd)
        val cut = if (length(firstLine) <= TITLE_MAX_CHARS) {
            firstLineEnd
        } else {
            // 코드포인트 경계에서 자른다 — 이모지를 반으로 가르지 않는다
            text.offsetByCodePoints(0, TITLE_MAX_CHARS)
        }
        return text.substring(0, cut) to text.substring(cut)
    }

    /**
     * (제목, 본문) → 글. 이 방식으로 쓴 글은 본문이 비었거나 줄바꿈으로 시작하거나(첫 줄이 제목에
     * 다 들어간 경우) 제목이 120자를 꽉 채웠다(잘린 경우). 그 밖은 예전 앱이 제목 · 본문을 따로 받은
     * 글이라 한 줄을 띄워 잇는다.
     */
    fun join(title: String, body: String): String = when {
        body.isEmpty() -> title
        body.startsWith('\n') || length(title) >= TITLE_MAX_CHARS -> title + body
        else -> title + "\n" + body
    }

    /** 목록 · 상세에 보일 본문 — 제목 뒤 첫 줄바꿈을 걷는다 */
    fun bodyForDisplay(body: String): String = body.removePrefix("\n").trimEnd()
}

/** 거리 표시 — "300m", "1.8km". 1km 안은 10m 단위로 반올림한다 */
fun formatStoryDistance(meters: Double): String {
    if (!meters.isFinite() || meters < 0) return ""
    val rounded = (Math.round(meters / 10.0) * 10).coerceAtLeast(10)
    return if (rounded < 1_000) "${rounded}m" else "%.1fkm".format(java.util.Locale.ROOT, meters / 1_000)
}
