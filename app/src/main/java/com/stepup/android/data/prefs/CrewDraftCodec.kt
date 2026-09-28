package com.stepup.android.data.prefs

/**
 * 크루 만들기 · 수정 초안을 글자로 — 이 폰에 남길 때(UserPrefs)와 화면을 다시 그릴 때(SavedStateHandle)가 같은 모양을 쓴다.
 */
object CrewDraftCodec {
    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    fun encode(draft: com.stepup.android.domain.CrewDraft): String =
        json.encodeToString(CrewDraftJson.serializer(), CrewDraftJson.of(draft))

    fun decode(raw: String): com.stepup.android.domain.CrewDraft? =
        runCatching { json.decodeFromString(CrewDraftJson.serializer(), raw).toDomain() }.getOrNull()

    fun encodeArea(area: com.stepup.android.domain.CrewArea): String =
        json.encodeToString(CrewAreaJson.serializer(), CrewAreaJson.of(area))

    fun decodeArea(raw: String): com.stepup.android.domain.CrewArea? =
        runCatching { json.decodeFromString(CrewAreaJson.serializer(), raw).toDomain() }.getOrNull()
}

/** 크루 활동 지역 — 이름과 (있으면) 좌표 */
@kotlinx.serialization.Serializable
internal data class CrewAreaJson(val name: String, val address: String = "", val lat: Double? = null, val lng: Double? = null) {
    fun toDomain() = com.stepup.android.domain.CrewArea(name, address, lat, lng)

    companion object {
        fun of(area: com.stepup.android.domain.CrewArea) = CrewAreaJson(area.name, area.address, area.lat, area.lng)
    }
}

/**
 * 크루 만들기 · 수정 초안. 대표 이미지는 종류(SERVER · NAMED · PHOTO)와 바탕 · 파일 경로로 남긴다 — 사진은 앱 안
 * 파일(filesDir/crew_drafts)이라 초안과 함께 지운다.
 */
@kotlinx.serialization.Serializable
internal data class CrewDraftJson(
    val mode: String = "CREATE",
    val crewId: String = "",
    val name: String = "",
    val tagline: String = "",
    val leaderNote: String = "",
    val imageKind: String = "NAMED",
    val imageBg: Int = 0,
    val imagePath: String = "",
    val area: CrewAreaJson? = null,
    val days: Int = 0,
    val minutes: Int? = null,
    val distance: String? = null,
    val moods: List<String> = emptyList(),
    val capacity: String = "",
    val goalKm: Int? = null,
    val goalCustom: Boolean = false,
    val recruiting: Boolean = true,
    val step: Int = 0,
    val clientKey: String = "",
    val savedAt: Long = 0L,
) {
    fun toDomain() = com.stepup.android.domain.CrewDraft(
        mode = com.stepup.android.domain.CrewDraftMode.entries.firstOrNull { it.name == mode }
            ?: com.stepup.android.domain.CrewDraftMode.CREATE,
        crewId = crewId,
        name = name,
        tagline = tagline,
        leaderNote = leaderNote,
        image = when (imageKind) {
            "SERVER" -> com.stepup.android.domain.CrewImageChoice.Server
            "PHOTO" -> if (imagePath.isNotEmpty() && java.io.File(imagePath).exists()) {
                com.stepup.android.domain.CrewImageChoice.Photo(imagePath)
            } else {
                com.stepup.android.domain.CrewImageChoice.Named(imageBg)
            }
            else -> com.stepup.android.domain.CrewImageChoice.Named(imageBg)
        },
        area = area?.toDomain(),
        schedule = com.stepup.android.domain.CrewSchedule(days, minutes).normalized,
        distance = com.stepup.android.domain.CrewDistance.of(distance),
        moods = moods.mapNotNull(com.stepup.android.domain.CrewMood::of),
        capacity = capacity,
        goal = goalKm?.let { com.stepup.android.domain.CrewGoalChoice.Km(it, goalCustom) }
            ?: com.stepup.android.domain.CrewGoalChoice.None,
        recruiting = recruiting,
        step = step,
        clientKey = clientKey,
        savedAt = savedAt,
    )

    companion object {
        fun of(draft: com.stepup.android.domain.CrewDraft): CrewDraftJson {
            val image = draft.image
            return CrewDraftJson(
                mode = draft.mode.name,
                crewId = draft.crewId,
                name = draft.name,
                tagline = draft.tagline,
                leaderNote = draft.leaderNote,
                imageKind = when (image) {
                    com.stepup.android.domain.CrewImageChoice.Server -> "SERVER"
                    is com.stepup.android.domain.CrewImageChoice.Named -> "NAMED"
                    is com.stepup.android.domain.CrewImageChoice.Photo -> "PHOTO"
                },
                imageBg = (image as? com.stepup.android.domain.CrewImageChoice.Named)?.bg ?: 0,
                imagePath = (image as? com.stepup.android.domain.CrewImageChoice.Photo)?.path.orEmpty(),
                area = draft.area?.let(CrewAreaJson::of),
                days = draft.schedule.days,
                minutes = draft.schedule.minutes,
                distance = draft.distance?.name,
                moods = draft.moods.map { it.name },
                capacity = draft.capacity,
                goalKm = draft.goal.km,
                goalCustom = (draft.goal as? com.stepup.android.domain.CrewGoalChoice.Km)?.custom == true,
                recruiting = draft.recruiting,
                step = draft.step,
                clientKey = draft.clientKey,
                savedAt = draft.savedAt,
            )
        }
    }
}
