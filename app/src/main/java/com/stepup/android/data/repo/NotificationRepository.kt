package com.stepup.android.data.repo

import com.stepup.android.data.local.NotificationDao
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 앱 내 알림함 — 읽음 처리 · 액션형(초대 수락, 보상 받기) 알림 처리 */
class NotificationRepository(
    private val dao: NotificationDao,
) {

    fun notifications(limit: Int = 100): Flow<List<NotificationEntity>> = dao.observeAll(limit)

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    /** 가장 최근 알림 번호 — "모두 읽음"이 어디까지 읽음으로 바꿀지 정한다 */
    val newestId: Flow<Long?> = dao.observeNewestId()

    suspend fun byId(id: Long): NotificationEntity? = dao.byId(id)

    /** 기록 지우기(clearAll) 뒤의 정리 — 알림함의 "모두 읽음"은 [markReadUpTo] 를 쓴다 */
    suspend fun markAllRead() = dao.markAllRead()

    /** 알림 하나를 열어 봤다. 읽음만 바꾼다 — 삭제 · 초대 응답 · 보상 지급과 별개다 */
    suspend fun markRead(id: Long) {
        dao.markRead(id)
    }

    /**
     * 알림함의 "모두 읽음" — 누른 때 목록에 있던 알림까지만. 요청 중에 새로 들어온 알림은 새 알림으로 남는다.
     * 행을 지우거나 답하지 않은 초대를 처리하지 않는다(그건 [clearAll] 과 초대 응답의 일이다).
     */
    suspend fun markReadUpTo(upToId: Long) {
        dao.markReadUpTo(upToId)
    }

    private val _inviteAfterSignIn = MutableStateFlow<SignInReturn?>(null)

    /**
     * 로그인이 필요해 멈춘 초대 — 로그인하고 돌아오면 알림함이 그 초대를 다시 연다.
     * 다시 보여 줄 뿐 자동으로 수락하지 않는다. 앱을 완전히 닫으면 잊는다(초대 알림은 그대로 남는다).
     */
    val inviteAfterSignIn: StateFlow<SignInReturn?> = _inviteAfterSignIn.asStateFlow()

    fun reopenAfterSignIn(request: SignInReturn?) {
        _inviteAfterSignIn.value = request
    }

    /**
     * "알림 기록 지우기"(설정 › 개인정보 · 앱 권한) — 일반 알림을 지운다.
     * 단, 아직 수령/응답하지 않은 액션형 알림(크루·파티 초대, 이벤트 보상)은 남기고 읽음으로 바꾼다.
     * 알림함의 "모두 읽음"과 다르다 — 그쪽은 행을 지우지 않는다([markReadUpTo]).
     */
    suspend fun clearAll() {
        dao.clearExceptPendingActions()
        dao.markAllRead()
    }

    /** 크루 초대 수락 — 크루 가입 후 알림을 처리 상태로 바꾼다 */
    suspend fun acceptCrewInvite(entity: NotificationEntity, crewRepository: CrewRepository): CrewActionResult =
        acceptCrewInvitation(dao, entity, crewRepository::join)

    /**
     * 수락 응답을 못 받았는데, 다시 받은 서버 크루 목록에 가입 · 신청이 있다 — 초대는 응답한 것으로 둔다.
     * 서버가 확인한 가입 상태로만 부른다(수락을 다시 보내지 않는다).
     */
    suspend fun markInviteAnswered(entity: NotificationEntity) {
        if (entity.type == NotificationType.CREW_INVITE) dao.markActioned(entity.id)
    }

    /** 초대 거절 — 그 초대 알림만 지운다. 가입한 크루 · 대기실은 그대로 */
    suspend fun decline(entity: NotificationEntity) {
        if (entity.type == NotificationType.CREW_INVITE || entity.type == NotificationType.PARTY_INVITE) dao.delete(entity.id)
    }

    /** 파티런 초대 수락 표시 (로비 이동은 화면에서) */
    suspend fun acceptPartyInvite(entity: NotificationEntity) {
        if (entity.type == NotificationType.PARTY_INVITE) dao.markActioned(entity.id)
    }

    /**
     * 예전 첫 실행 알림에 들어 있던 크루·파티런 초대를 지운다.
     *
     * 그 초대는 폰 안에 심어 둔 가짜 크루로 가는 것이었다. 크루가 서버로
     * 옮겨 가면서 그 크루들은 없어졌고, 수락해도 갈 곳이 없다.
     */
    suspend fun purgeLegacyInvites() = dao.deleteInvitesTo(LEGACY_CREW_IDS)

    private companion object {
        val LEGACY_CREW_IDS = listOf("trailblazer", "night_runners", "summit", "new_striders")
    }
}

/** Do not consume an invitation until the server accepts joining or requesting membership. */
internal suspend fun acceptCrewInvitation(
    dao: NotificationDao,
    entity: NotificationEntity,
    join: suspend (String) -> CrewActionResult,
): CrewActionResult {
    if (entity.actioned) return CrewActionResult.Done
    if (entity.type != NotificationType.CREW_INVITE || entity.argExtra.isBlank()) {
        return CrewActionResult.Failed("Invalid invitation")
    }
    val result = join(entity.argExtra)
    if (result == CrewActionResult.Joined || result == CrewActionResult.Requested) {
        dao.markActioned(entity.id)
    }
    return result
}

/**
 * 로그인하고 돌아오면 다시 열 초대.
 * @param requestedAt 로그인으로 보낸 때(SystemClock.elapsedRealtime) — 그 뒤에 새로 열린 화면만 초대를 다시 연다
 */
data class SignInReturn(val notificationId: Long, val requestedAt: Long)
