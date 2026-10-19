package com.stepup.android.data.repo

import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.data.remote.SessionRecorded
import kotlinx.coroutines.flow.Flow

/**
 * 한 번 돌린 업로드의 결말.
 *
 * 일꾼(Worker)이 "다시 깨워야 하는가"를 판단하는 근거다.
 */
data class UploadRun(
    val signed: Int = 0,
    val rejected: Int = 0,
    val failed: Int = 0,
    /** 다시 시도할 가치가 있는 세션이 남았는가 */
    val shouldRetry: Boolean = false,
    /** 아예 시도하지 못한 이유 (지갑 없음, 서버 주소 없음 등) */
    val blockedBy: String? = null,
)

/**
 * 세션 하나를 서버에 기록하는 쪽.
 *
 * 인터페이스로 둔 것은 업로드 규칙(무엇을 언제 다시 시도할지)을 실제 네트워크
 * 없이 검증하기 위해서다. 그 규칙이 틀리면 사용자가 정당하게 뛴 기록을 잃거나
 * 영원히 거절당할 요청으로 배터리를 태운다.
 */
interface SessionRecorder {
    val isConfigured: Boolean

    suspend fun record(session: WalkSessionEntity): ServerResult<SessionRecorded>

    /**
     * 서버가 기록한 러닝의 뒤 일(크루 표시 · 코스 기록). 같은 러닝으로 다시 보내도 한 번만 된다. 러닝 확인과 따로 한다 —
     * 이것이 실패해도 서버는 이미 기록 · 지급했다. Ok 면 더 할 일이 없다(서버가 거절한 것도 다시 보내지 않는다).
     * Retry · SignInRequired 면 다음 업로드 차례에 다시 낸다.
     */
    suspend fun followUp(session: WalkSessionEntity): ServerResult<Unit> = ServerResult.Ok(Unit)
}

/**
 * 쌓인 러닝 세션을 서버로 올린다.
 *
 * 러닝이 끝나는 곳은 지하철이거나 산이다. 그 자리에서 바로 보내는 것을
 * 전제로 만들면 기록이 사라진다. 그래서 세션은 일단 기기에 쌓이고, 이
 * 저장소가 연결이 될 때 대신 밀어 올린다.
 *
 * 적립액은 서버가 정한다. 앱이 계산한 금액은 보내지도 않는다 —
 * `supabase/migrations/0003_ledger.sql` 의 `record_session()` 이 걸음 수를
 * 받아 직접 계산하고, 그 값을 돌려준다.
 */
class ClaimRepository(
    private val sessionDao: WalkSessionDao,
    private val recorder: SessionRecorder,
    private val now: () -> Long = System::currentTimeMillis,
    private val uploadOwner: suspend () -> String = { "legacy" },
    /**
     * 서버가 러닝을 하나라도 확인했으면 한 번 부른다 — 잔고 · 에너지를 다시 받아 온다. 이번 차례에 확인받은 러닝들의
     * 시작 시각(epoch ms)을 넘긴다 — 목표 보너스를 러닝이 시작한 날(한국 시각)마다 받게(0068)
     */
    private val onSigned: suspend (signedStartedAt: List<Long>) -> Unit = {},
    /**
     * 다시 로그인해야 올릴 수 있다 — 화면(러닝 결과의 "다시 로그인")이 알게 알린다. 예전엔 일꾼이 조용히 물러나
     * 결과가 "정산 대기"에 머물렀고, 서버가 7일 지난 러닝을 받지 않아 적립을 잃을 수 있었다(2026-10-17 점검).
     */
    private val onSignInRequired: suspend () -> Unit = {},
) {

    /** 올릴 것이 몇 개 남았는지 — 화면에 보여주기 위한 값 */
    fun observePendingCount(): Flow<Int> = sessionDao.observePendingUploadCount()

    /**
     * 대기열을 한 번 훑는다.
     *
     * 한 번에 [limit] 개까지만 처리한다. 오래 돌수록 안드로이드가 일꾼을
     * 중간에 끊을 확률이 커지고, 끊기면 어디까지 했는지가 애매해진다.
     * 남은 것은 다음 차례에 이어서 한다.
     */
    suspend fun uploadPending(limit: Int = 10): UploadRun {
        if (!recorder.isConfigured) {
            return UploadRun(blockedBy = "서버 주소가 아직 설정되지 않았습니다")
        }

        val owner = uploadOwner()
        val pending = sessionDao.pendingUploads(limit, owner)
        // 올릴 러닝이 없어도 끝내지 못한 뒤 일은 다시 낸다(확인받은 러닝 — 일꾼을 다시 깨우지도, 확인을 다시 알리지도 않는다)
        if (pending.isEmpty()) {
            retryFollowUps(owner, skip = emptySet())
            return UploadRun()
        }

        var signed = 0
        var rejected = 0
        var failed = 0
        var retriesInRow = 0
        // 이번 차례에 확인받아 뒤 일을 이미 낸 러닝 — 아래 다시 내기에서 건너뛴다
        val followedNow = HashSet<Long>()
        // 이번 차례에 확인받은 러닝의 시작 시각 — onSigned 로 넘긴다
        val signedStarts = ArrayList<Long>()

        for (session in pending) {
            when (val result = recorder.record(session)) {
                is ServerResult.Ok -> {
                    // 서버가 기록 · 지급을 마쳤다 — 바로 확인(SIGNED)으로 적는다. 뒤 일(크루 표시 · 코스 기록)은 그다음에 따로
                    // 낸다. 예전엔 뒤 일이 끊기면 러닝이 FAILED 로 남아 결과가 "정산 대기"에 머물고 일꾼의 기다림이 몇 시간까지
                    // 늘어 뒤의 러닝까지 늦게 올라갔다(2026-10-17 점검). 뒤 일을 마치기 전에 앱이 꺼져도 다시 내도록 표시를 먼저 둔다
                    val confirmed = session.copy(
                        uploadState = UploadState.SIGNED.name,
                        uploadAttemptedAt = now(),
                        uploadAttempts = session.uploadAttempts + 1,
                        uploadError = FOLLOW_UP_PENDING,
                        verdict = result.value.verdict,
                        // 서버가 계산한 금액으로 덮는다. 앱이 화면에 보여준
                        // 값과 다를 수 있고, 다르면 서버 쪽이 맞다.
                        claimAmount = result.value.pointsAwarded.toString(),
                        claimDay = result.value.sessionId,
                        // 기록에 보이는 적립액도 서버가 정한 값이다
                        pointsEarned = result.value.pointsAwarded,
                    )
                    sessionDao.update(confirmed)
                    signed++
                    followedNow += session.id
                    signedStarts += session.startedAt
                    followUp(confirmed)
                }

                // 폰 시계가 앞서 있어 "종료 시각이 미래" 로 거절됐으면 시간이 지나면 받아진다 — 다시 시도할 목록에 둔다
                is ServerResult.Rejected -> if ("종료 시각이 미래" in result.reason) {
                    sessionDao.update(
                        session.copy(
                            uploadState = UploadState.FAILED.name,
                            uploadAttemptedAt = now(),
                            uploadAttempts = session.uploadAttempts + 1,
                            uploadError = result.reason,
                        ),
                    )
                    failed++
                } else {
                    sessionDao.update(session.rejected(result.reason))
                    rejected++
                }

                is ServerResult.Retry -> {
                    sessionDao.update(
                        session.copy(
                            uploadState = UploadState.FAILED.name,
                            uploadAttemptedAt = now(),
                            uploadAttempts = session.uploadAttempts + 1,
                            uploadError = result.reason,
                        ),
                    )
                    failed++
                    // 연달아 두 번 끊기면 서버나 연결이 멈춘 것이다 — 남은 세션까지 줄줄이 실패시켜 시도 횟수만 올릴
                    // 이유가 없다. 한 번이면 그 러닝만의 문제일 수 있다 — 가장 오래된 러닝 하나가 계속 실패하면 뒤의
                    // 러닝이 모두 기다리다 서버의 7일 기한을 넘겼다. 다음 것을 계속 올린다.
                    if (++retriesInRow >= 2) break
                    continue
                }

                is ServerResult.SignInRequired -> {
                    // 다시 로그인해야 한다. 여기서 세션을 실패로 찍어 봐야
                    // 시도 횟수만 오른다 — 대기열을 그대로 두고 물러난다.
                    // 그 전에 확인받은 러닝이 있으면 잔고는 다시 받아 둔다.
                    if (signed > 0) runCatching { onSigned(signedStarts) }
                    runCatching { onSignInRequired() }
                    return UploadRun(
                        signed = signed,
                        rejected = rejected,
                        failed = failed,
                        blockedBy = result.reason,
                    )
                }
            }
            retriesInRow = 0
        }
        // 앞서 끝내지 못한 뒤 일 — 연결이 끊겨 멈춘 차례면 다음으로 미룬다
        if (retriesInRow < 2) retryFollowUps(owner, skip = followedNow)

        if (signed > 0) runCatching { onSigned(signedStarts) }
        return UploadRun(
            signed = signed,
            rejected = rejected,
            failed = failed,
            shouldRetry = failed > 0,
        )
    }

    /** 뒤 일이 남은 러닝이 없다고 본 계정 — 다시 훑지 않는다. 앱을 켠 뒤 처음 한 번은 훑고, 뒤 일이 실패하면 그 계정을 다시 훑는다 */
    private val followUpsClear: MutableSet<String> = java.util.concurrent.ConcurrentHashMap.newKeySet()

    /** 뒤 일을 한 번 낸다. 마쳤으면 표시를 지운다 — 실패해도 그 러닝의 확인(SIGNED)은 그대로다 */
    private suspend fun followUp(row: WalkSessionEntity): ServerResult<Unit> {
        val result = try {
            recorder.followUp(row)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            ServerResult.Retry(error.message.orEmpty())
        }
        if (result is ServerResult.Ok) sessionDao.update(row.copy(uploadError = "")) else followUpsClear.remove(row.recordingOwner)
        return result
    }

    /**
     * 확인은 받았지만 뒤 일을 끝내지 못한 이 계정의 최근 러닝을 다시 낸다([skip] 은 이번 차례에 이미 낸 러닝).
     * 일꾼을 다시 깨우지 않는다 — 뒤 일 때문에 기다림이 길어져 새 러닝이 늦게 올라가지 않게. 다음 업로드 차례(앱을 켤 때 ·
     * 인터넷이 돌아올 때 · 러닝을 끝낼 때)에 다시 본다. 확인은 이미 알렸으므로 [onSigned] 를 다시 부르지 않는다.
     */
    private suspend fun retryFollowUps(owner: String, skip: Set<Long>) {
        if (!followUpsClear.add(owner)) return
        val marked = sessionDao.signedWithFollowUp(owner, FOLLOW_UP_PENDING, FOLLOW_UP_SCAN)
        // 한 번에 다 보지 못했다 — 다음 차례에 이어 본다
        if (marked.size >= FOLLOW_UP_SCAN) followUpsClear.remove(owner)
        var retriesInRow = 0
        for (row in marked) {
            if (row.id in skip) {
                // 이번 차례에 막 실패한 러닝 — 다음 차례에 다시 낸다
                followUpsClear.remove(owner)
                continue
            }
            when (followUp(row)) {
                is ServerResult.Ok -> retriesInRow = 0
                is ServerResult.SignInRequired -> break
                else -> if (++retriesInRow >= 2) break
            }
        }
    }

    private fun WalkSessionEntity.rejected(reason: String) = copy(
        uploadState = UploadState.REJECTED.name,
        uploadAttemptedAt = now(),
        uploadAttempts = uploadAttempts + 1,
        uploadError = reason,
    )

    companion object {
        /** 확인받은 러닝(SIGNED)의 뒤 일이 남았다는 표시 — 그 줄의 uploadError 에 둔다(확인된 줄의 이 칸은 화면이 읽지 않는다) */
        const val FOLLOW_UP_PENDING = "follow-up"

        /** 한 차례에 뒤 일을 다시 낼 러닝 수(오래된 것부터) */
        private const val FOLLOW_UP_SCAN = 30
    }
}
