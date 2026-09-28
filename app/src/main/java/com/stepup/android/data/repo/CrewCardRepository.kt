package com.stepup.android.data.repo

import android.graphics.Bitmap
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.remote.CrewApi
import com.stepup.android.data.remote.CrewApplicationRow
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.domain.CrewApplication
import com.stepup.android.domain.CrewApplicationStatus
import com.stepup.android.domain.CrewArea
import com.stepup.android.domain.CrewCard
import com.stepup.android.domain.CrewDecision
import com.stepup.android.domain.CrewDraft
import com.stepup.android.domain.CrewDraftMode
import com.stepup.android.domain.CrewImageChoice
import com.stepup.android.domain.CrewLevelInfo
import com.stepup.android.domain.CrewMember
import com.stepup.android.domain.CrewPerson
import com.stepup.android.domain.CrewPersonRole
import com.stepup.android.domain.CrewPhrase
import com.stepup.android.domain.CrewProblem
import com.stepup.android.domain.CrewRules
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

/** 크루 명함형 한 동작의 결말 — 실패는 화면이 고를 다음 화면(칸 오류 · 정원 · 재시도)을 담는다 */
sealed interface CrewOutcome<out T> {
    data class Ok<T>(val value: T) : CrewOutcome<T>
    data class Failed(val problem: CrewProblem) : CrewOutcome<Nothing>
}

/** 가입 신청의 결말 */
sealed interface CrewApplied {
    /** 크루장의 확인을 기다린다 */
    data class Pending(val applicationId: Long) : CrewApplied

    /** 바로 멤버가 됐다(예전 바로 가입 크루) · 이미 멤버였다 */
    data object Member : CrewApplied
}

/**
 * 크루 명함형 — 목록 · 상세 · 가입 신청 · 크루장 관리 · 만들기/수정 초안.
 *
 * 인원 · 정원 · 가입 상태 · 관리 권한은 서버가 기준이다. 누른 순간 화면을 바꾸지 않고, 서버가 받아 준 뒤 그 크루를
 * 다시 읽어 목록 · 상세 · 관리 화면이 같은 값을 보게 한다. 크루 목록은 [CrewRepository] 가 한 번 받아 예전 화면
 * (크루 게시판 · 파티런 · 순위)과 나눠 쓴다.
 */
class CrewCardRepository(
    private val api: CrewApi,
    private val crews: CrewRepository,
    val images: CrewImageStore,
    private val prefs: UserPrefs,
    private val owner: suspend () -> String,
    private val ownerFlow: Flow<String>,
    /** 초안 사진을 두는 곳(filesDir/crew_drafts) */
    private val draftDir: File,
) {
    val cards: StateFlow<List<CrewCard>> = crews.cards
    val sync: StateFlow<CrewSyncState> = crews.sync

    /** 목록(200곳)에 없는 크루를 상세로 읽었을 때 */
    private val extra = MutableStateFlow<Map<String, CrewCard>>(emptyMap())

    private val _missing = MutableStateFlow<Set<String>>(emptySet())

    /** 해산 · 숨김 · 권한 변경으로 볼 수 없게 된 크루 */
    val missing: StateFlow<Set<String>> = _missing

    fun card(crewId: String): Flow<CrewCard?> =
        combine(cards, extra) { list, more -> list.firstOrNull { it.id == crewId } ?: more[crewId] }

    fun cardNow(crewId: String): CrewCard? = cards.value.firstOrNull { it.id == crewId } ?: extra.value[crewId]

    suspend fun refresh(): CrewSyncState = crews.refresh()

    /** 한 크루를 서버에서 다시 읽는다. 볼 수 없게 됐으면 Ok(null) */
    suspend fun load(crewId: String): CrewOutcome<CrewCard?> = when (val result = api.crew(crewId)) {
        is ServerResult.Ok -> {
            val row = result.value
            if (row == null) {
                _missing.value = _missing.value + crewId
                crews.dropCrew(crewId)
                extra.value = extra.value - crewId
                CrewOutcome.Ok(null)
            } else {
                val card = row.toCard()
                _missing.value = _missing.value - crewId
                crews.patchCard(card)
                if (cards.value.none { it.id == crewId }) extra.value = extra.value + (crewId to card)
                CrewOutcome.Ok(card)
            }
        }
        else -> CrewOutcome.Failed(result.problem())
    }

    // ── 가입 신청 ──────────────────────────────────────────────────

    suspend fun apply(crewId: String, phrases: List<CrewPhrase>, message: String, clientKey: String): CrewOutcome<CrewApplied> {
        val outcome = when (val result = api.apply(crewId, phrases.map { it.name }, CrewRules.message(message), clientKey)) {
            is ServerResult.Ok -> {
                val id = result.value.applicationId
                if (result.value.result == "PENDING" && id != null) CrewOutcome.Ok(CrewApplied.Pending(id))
                else CrewOutcome.Ok(CrewApplied.Member)
            }
            else -> CrewOutcome.Failed(result.problem())
        }
        // 성공이든 모집 상태가 바뀌었든 — 지금 상태를 다시 읽는다
        load(crewId)
        crews.refresh()
        return outcome
    }

    suspend fun application(applicationId: Long): CrewOutcome<CrewApplication> =
        when (val result = api.application(applicationId)) {
            is ServerResult.Ok -> CrewOutcome.Ok(result.value.toDomain())
            else -> CrewOutcome.Failed(result.problem())
        }

    /** 신청 취소 — 지금 상태를 돌려준다(그 사이 승인됐으면 APPROVED — 멤버 상세로) */
    suspend fun cancel(crewId: String, applicationId: Long): CrewOutcome<CrewApplicationStatus> {
        val outcome = when (val result = api.cancelApplication(applicationId)) {
            is ServerResult.Ok -> CrewOutcome.Ok(CrewApplicationStatus.of(result.value) ?: CrewApplicationStatus.CANCELED)
            else -> CrewOutcome.Failed(result.problem())
        }
        load(crewId)
        crews.refresh()
        return outcome
    }

    /** 승인 · 미승인 결과를 봤다 — 같은 결과 화면을 다시 띄우지 않는다 */
    suspend fun markSeen(crewId: String, applicationId: Long) {
        cardNow(crewId)?.takeIf { it.myApplicationId == applicationId }?.let { card ->
            val seen = card.copy(myApplicationSeen = true)
            crews.patchCard(seen)
            if (extra.value.containsKey(crewId)) extra.value = extra.value + (crewId to seen)
        }
        api.seenApplication(applicationId)
    }

    // ── 크루장 ────────────────────────────────────────────────────

    suspend fun pending(crewId: String): CrewOutcome<List<CrewApplication>> =
        when (val result = api.pendingApplications(crewId)) {
            is ServerResult.Ok -> CrewOutcome.Ok(result.value.map { it.toDomain(crewId) })
            else -> CrewOutcome.Failed(result.problem())
        }

    suspend fun decide(crewId: String, applicationId: Long, approve: Boolean): CrewOutcome<CrewDecision> {
        val outcome = when (val result = api.decideApplication(applicationId, approve)) {
            is ServerResult.Ok -> CrewOutcome.Ok(
                CrewDecision(result.value.status == "APPROVED", result.value.memberCount, result.value.capacity, result.value.pendingCount),
            )
            else -> CrewOutcome.Failed(result.problem())
        }
        load(crewId)
        return outcome
    }

    suspend fun roster(crewId: String): CrewOutcome<List<CrewMember>> = when (val result = api.roster(crewId)) {
        is ServerResult.Ok -> CrewOutcome.Ok(
            result.value.map { CrewMember(it.userId, it.name, it.role == "OWNER", it.joinedAt.isoToMillis(), it.weekKm, it.weekRuns) },
        )
        else -> CrewOutcome.Failed(result.problem())
    }

    suspend fun person(crewId: String, userId: String): CrewOutcome<CrewPerson> = when (val result = api.person(crewId, userId)) {
        is ServerResult.Ok -> {
            val row = result.value
            CrewOutcome.Ok(
                CrewPerson(
                    userId = row.userId,
                    name = row.name,
                    role = CrewPersonRole.entries.firstOrNull { it.name == row.role } ?: CrewPersonRole.NONE,
                    joinedAt = row.joinedAt?.isoToMillis(),
                    weekKm = row.weekKm,
                    application = row.application?.let { app ->
                        CrewApplication(
                            id = app.id, crewId = crewId, userId = row.userId, name = row.name,
                            status = CrewApplicationStatus.PENDING, phrases = app.phrases.mapNotNull(CrewPhrase::of),
                            message = app.message, createdAt = app.createdAt.isoToMillis(),
                        )
                    },
                ),
            )
        }
        else -> CrewOutcome.Failed(result.problem())
    }

    suspend fun level(crewId: String): CrewOutcome<CrewLevelInfo> = when (val result = api.level(crewId)) {
        is ServerResult.Ok -> CrewOutcome.Ok(
            CrewLevelInfo(result.value.level, result.value.weekKm, result.value.weekRunners, result.value.memberCount),
        )
        else -> CrewOutcome.Failed(result.problem())
    }

    suspend fun setRecruiting(crewId: String, open: Boolean): CrewOutcome<Unit> = act(crewId) { api.setRecruiting(crewId, open) }

    suspend fun setGoal(crewId: String, goalKm: Int?): CrewOutcome<Unit> = act(crewId) { api.setGoal(crewId, goalKm) }

    suspend fun removeMember(crewId: String, userId: String): CrewOutcome<Unit> = act(crewId) { api.removeMember(crewId, userId) }

    suspend fun transfer(crewId: String, userId: String): CrewOutcome<Unit> = act(crewId, list = true) { api.transferOwner(crewId, userId) }

    /** 멤버 탈퇴 — 크루장은 먼저 넘기거나 해산한다(서버가 막는다) */
    suspend fun leave(crewId: String): CrewOutcome<Unit> = act(crewId, list = true) {
        when (val result = crews.leave(crewId)) {
            is CrewActionResult.Failed -> when {
                result.signIn -> ServerResult.SignInRequired(result.reason)
                result.retryable -> ServerResult.Retry(result.reason)
                else -> ServerResult.Rejected(result.reason)
            }
            else -> ServerResult.Ok(Unit)
        }
    }

    /** 가입 방식(예전 기능 — 운영 설정 안쪽): true 면 바로 가입, false 면 크루장 확인 */
    suspend fun setOpenJoin(crewId: String, open: Boolean): CrewOutcome<Unit> = act(crewId, list = true) {
        when (val result = crews.setJoinPolicy(crewId, if (open) CrewJoinPolicy.OPEN else CrewJoinPolicy.APPROVAL)) {
            is CrewActionResult.Failed -> when {
                result.signIn -> ServerResult.SignInRequired(result.reason)
                result.retryable -> ServerResult.Retry(result.reason)
                else -> ServerResult.Rejected(result.reason)
            }
            else -> ServerResult.Ok(Unit)
        }
    }

    /** 해산 — 성공하면 목록에서 뺀다. 실패하면 그대로(크루 · 권한 유지) */
    suspend fun dissolve(crewId: String): CrewOutcome<Unit> = when (val result = api.dissolve(crewId)) {
        is ServerResult.Ok -> {
            crews.dropCrew(crewId)
            _missing.value = _missing.value + crewId
            crews.refresh()
            CrewOutcome.Ok(Unit)
        }
        else -> CrewOutcome.Failed(result.problem())
    }

    /** 신고 — 서버가 받은 뒤에만 완료 */
    suspend fun report(crewId: String, reason: String, note: String): CrewOutcome<Unit> =
        when (val result = api.report(crewId, reason, note)) {
            is ServerResult.Ok -> CrewOutcome.Ok(Unit)
            else -> CrewOutcome.Failed(result.problem())
        }

    // ── 만들기 · 수정 ──────────────────────────────────────────────

    /** 만들기 — 같은 초안(clientKey)은 서버가 한 크루로 받는다. 성공하면 초안과 사진 파일을 지운다 */
    suspend fun create(draft: CrewDraft): CrewOutcome<String> {
        val capacity = CrewRules.capacity(draft.capacity) ?: return CrewOutcome.Failed(CrewProblem.CAPACITY)
        val photo = (draft.image as? CrewImageChoice.Photo)?.let { encodePhoto(it.path) ?: return CrewOutcome.Failed(CrewProblem.IMAGE) }
        val bg = (draft.image as? CrewImageChoice.Named)?.bg ?: CrewRules.bgFor(draft.clientKey)
        val result = api.createCard(
            name = draft.name.trim(),
            tagline = draft.tagline.trim(),
            leaderNote = draft.leaderNote.trim(),
            imageBg = bg,
            image = photo,
            area = draft.area?.name.orEmpty(),
            lat = draft.area?.lat,
            lng = draft.area?.lng,
            meetDays = draft.schedule.normalized.days,
            meetTime = draft.schedule.normalized.minutes,
            distance = draft.distance?.name,
            moods = draft.moods.map { it.name },
            capacity = capacity,
            recruiting = draft.recruiting,
            goalKm = draft.goal.km,
            clientKey = draft.clientKey,
        )
        return when (result) {
            is ServerResult.Ok -> {
                val crewId = result.value
                (draft.image as? CrewImageChoice.Photo)?.let { rememberPhoto(crewId, 1, it.path) }
                deleteDraft(draft)
                crews.refresh()
                load(crewId)
                CrewOutcome.Ok(crewId)
            }
            else -> CrewOutcome.Failed(result.problem())
        }
    }

    /**
     * 수정 저장 — 서버 값은 성공한 뒤에만 바뀐다. 실패하면 초안이 그대로 남아 같은 저장을 다시 한다.
     * 모집 설정은 목표를 이 화면에서 바꿨을 때만 목표도 보낸다([goalChanged]) — 주간 목표 화면에서 바꾼 값을 덮지 않게.
     */
    suspend fun saveEdit(draft: CrewDraft, goalChanged: Boolean = true): CrewOutcome<Unit> {
        val crewId = draft.crewId
        val card = cardNow(crewId)
        val result: ServerResult<Unit> = when (draft.mode) {
            CrewDraftMode.EDIT_PROFILE -> {
                val image = draft.image
                val photo = (image as? CrewImageChoice.Photo)?.let { encodePhoto(it.path) ?: return CrewOutcome.Failed(CrewProblem.IMAGE) }
                api.updateProfile(
                    crewId = crewId,
                    name = draft.name.trim(),
                    tagline = draft.tagline.trim(),
                    leaderNote = draft.leaderNote.trim(),
                    imageBg = (image as? CrewImageChoice.Named)?.bg ?: card?.imageBg,
                    imageAction = when {
                        image is CrewImageChoice.Photo -> "SET"
                        image is CrewImageChoice.Named && card?.hasImage == true -> "REMOVE"
                        else -> "KEEP"
                    },
                    image = photo,
                )
            }
            CrewDraftMode.EDIT_RUNNING -> api.updateRunning(
                crewId = crewId,
                area = draft.area?.name.orEmpty(),
                lat = draft.area?.lat,
                lng = draft.area?.lng,
                meetDays = draft.schedule.normalized.days,
                meetTime = draft.schedule.normalized.minutes,
                distance = draft.distance?.name,
                moods = draft.moods.map { it.name },
            )
            CrewDraftMode.EDIT_RECRUIT -> {
                val capacity = CrewRules.capacity(draft.capacity) ?: return CrewOutcome.Failed(CrewProblem.CAPACITY)
                api.updateRecruit(crewId, capacity, draft.recruiting, goalChange = goalChanged, goalKm = draft.goal.km)
            }
            CrewDraftMode.CREATE -> return CrewOutcome.Failed(CrewProblem.OTHER)
        }
        return when (result) {
            is ServerResult.Ok -> {
                val fresh = (load(crewId) as? CrewOutcome.Ok)?.value
                (draft.image as? CrewImageChoice.Photo)?.let { photo -> fresh?.let { rememberPhoto(crewId, it.imageVer, photo.path) } }
                deleteDraft(draft)
                crews.refresh()
                CrewOutcome.Ok(Unit)
            }
            else -> CrewOutcome.Failed(result.problem())
        }
    }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun draft(key: String): Flow<CrewDraft?> = ownerFlow.flatMapLatest { prefs.crewDraft(it, key) }

    suspend fun storedDraft(key: String): CrewDraft? = runCatching { prefs.crewDraft(owner(), key).first() }.getOrNull()

    suspend fun saveDraft(draft: CrewDraft) {
        runCatching { prefs.setCrewDraft(owner(), draft.key, draft.copy(savedAt = System.currentTimeMillis())) }
    }

    /** 초안을 지운다 — 초안이 쓰던 사진 파일도 함께 */
    suspend fun deleteDraft(draft: CrewDraft) {
        runCatching { prefs.setCrewDraft(owner(), draft.key, null) }
        (draft.image as? CrewImageChoice.Photo)?.let { runCatching { File(it.path).delete() } }
    }

    /** 초안 사진 파일 — 자를 때마다 새 이름(되돌리기 · 두 초안이 같은 파일을 쓰지 않게) */
    fun newDraftPhotoFile(): File = File(draftDir, "photo-${System.currentTimeMillis()}.jpg")

    /** 목록 지역 · 범위 · 정렬 */
    val region: Flow<CrewArea?> = prefs.crewRegion
    val radius: Flow<Int> = prefs.crewRadius
    val sort: Flow<String> = prefs.crewSort

    suspend fun setRegion(area: CrewArea?) = prefs.setCrewRegion(area)
    suspend fun setRadius(km: Int) = prefs.setCrewRadius(km)
    suspend fun setSort(sort: String) = prefs.setCrewSort(sort)

    // ──────────────────────────────────────────────────────────────

    private suspend fun act(crewId: String, list: Boolean = false, call: suspend () -> ServerResult<Unit>): CrewOutcome<Unit> =
        when (val result = call()) {
            is ServerResult.Ok -> {
                load(crewId)
                if (list) crews.refresh()
                CrewOutcome.Ok(Unit)
            }
            else -> CrewOutcome.Failed(result.problem())
        }

    private fun encodePhoto(path: String): String? = CrewPhotos.decodeFile(File(path))?.let(CrewPhotos::encode)

    private fun rememberPhoto(crewId: String, version: Int, path: String) {
        val bitmap: Bitmap = CrewPhotos.decodeFile(File(path)) ?: return
        images.remember(crewId, version, bitmap)
    }

    /** 화면 검사용 — 서버 없이 명함을 채운다 */
    @androidx.annotation.VisibleForTesting
    fun showForTest(card: CrewCard) {
        extra.value = extra.value + (card.id to card)
    }
}

private fun CrewApplicationRow.toDomain(fallbackCrew: String = ""): CrewApplication = CrewApplication(
    id = id,
    crewId = crewId.ifBlank { fallbackCrew },
    userId = userId,
    name = name,
    status = CrewApplicationStatus.of(status) ?: CrewApplicationStatus.PENDING,
    phrases = phrases.mapNotNull(CrewPhrase::of),
    message = message,
    createdAt = createdAt.isoToMillis(),
    decidedAt = decidedAt?.isoToMillis(),
    seen = seen,
)

/** 서버가 받아 주지 않은 까닭 → 화면이 고를 다음 */
internal fun ServerResult<*>.problem(): CrewProblem = when (this) {
    is ServerResult.SignInRequired -> CrewProblem.SIGN_IN
    is ServerResult.Retry -> CrewProblem.NETWORK
    is ServerResult.Rejected -> CrewProblem.of(reason)
    is ServerResult.Ok -> CrewProblem.OTHER
}
