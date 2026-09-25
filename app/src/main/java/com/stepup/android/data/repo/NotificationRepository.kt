package com.stepup.android.data.repo

import com.stepup.android.data.local.NotificationDao
import com.stepup.android.data.local.NotificationEntity
import com.stepup.android.data.local.NotificationType
import kotlinx.coroutines.flow.Flow

/** 앱 내 알림함 — 읽음 처리 · 액션형(초대 수락, 보상 받기) 알림 처리 */
class NotificationRepository(
    private val dao: NotificationDao,
) {

    fun notifications(limit: Int = 100): Flow<List<NotificationEntity>> = dao.observeAll(limit)

    val unreadCount: Flow<Int> = dao.observeUnreadCount()

    suspend fun markAllRead() = dao.markAllRead()

    /**
     * "모두 읽음" — 알림함을 비운다.
     * 단, 아직 수령/응답하지 않은 액션형 알림(크루·파티 초대, 이벤트 보상)은 남긴다.
     */
    suspend fun clearAll() {
        dao.clearExceptPendingActions()
        dao.markAllRead()
    }

    /** 크루 초대 수락 — 크루 가입 후 알림을 처리 상태로 바꾼다 */
    suspend fun acceptCrewInvite(entity: NotificationEntity, crewRepository: CrewRepository): CrewActionResult =
        acceptCrewInvitation(dao, entity, crewRepository::join)

    /** 초대 거절 — 알림만 지운다 */
    suspend fun decline(entity: NotificationEntity) = dao.delete(entity.id)

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
