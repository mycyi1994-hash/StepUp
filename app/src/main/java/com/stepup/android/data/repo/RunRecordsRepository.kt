package com.stepup.android.data.repo

import com.stepup.android.data.local.RecordTotals
import com.stepup.android.data.local.RunRecordRow
import com.stepup.android.data.local.UploadState
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.local.WalkSessionEntity
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunMark
import com.stepup.android.domain.RunTrack
import com.stepup.android.domain.TimeRange
import com.stepup.android.domain.toGeoPoints
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withContext

/** 기록 하나를 지운 결과 */
enum class RecordDeletion {
    Deleted,

    /** 서버에 올려 보상을 확인받는 중이라 지우지 않았다 — 확인이 끝나면 지울 수 있다 */
    Uploading,

    /** 이미 없다(다른 곳에서 지웠다) */
    Missing,
}

/**
 * 내 러닝 기록(2026-09-28 전달본) — 이 휴대폰에 저장된 지금 계정의 러닝(+ 계정을 나누기 전의 옛 기록).
 *
 * 내 정보의 누적 거리 · 횟수와 같은 자료(walk_sessions)와 같은 기준으로 센다: 저장된 러닝의 거리(distanceMeters) ·
 * 운동 시간(durationSec) · 횟수. 목록은 한 쪽씩 늘려 가며 읽고, 합계와 막대는 기간 전체를 센다.
 * 경로 글자는 목록에서 빼고, 줄마다 썸네일이 필요할 때 따로 읽는다.
 *
 * 지우기는 이 휴대폰의 기록에서만 — 서버에 올린 러닝 · 이미 받은 SUP · 순위 기록은 바꾸지 않는다(삭제 계약이 서버에 없다).
 * 서버 확인을 기다리는 러닝은 지우지 않는다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RunRecordsRepository(
    private val dao: WalkSessionDao,
    private val owner: Flow<String>,
) {
    fun rows(range: TimeRange, limit: Int): Flow<List<RunRecordRow>> =
        owner.flatMapLatest { dao.observeRecordRows(it, range.from, range.until, limit) }

    fun totals(range: TimeRange): Flow<RecordTotals> =
        owner.flatMapLatest { dao.observeRecordTotals(it, range.from, range.until) }

    fun marks(range: TimeRange): Flow<List<RunMark>> =
        owner.flatMapLatest { dao.observeRunMarks(it, range.from, range.until) }

    fun record(id: Long): Flow<WalkSessionEntity?> = owner.flatMapLatest { dao.observeRecord(it, id) }

    fun firstRecordAt(): Flow<Long?> = owner.flatMapLatest { dao.observeFirstRecordAt(it) }

    /** 러닝 하나의 저장된 경로. 없거나 깨졌으면 빈 목록 — 긴 러닝은 점이 수천 개라 글자 풀기는 화면 스레드 밖에서 */
    suspend fun route(id: Long): List<GeoPoint> {
        val raw = dao.trackOf(owner.first(), id).orEmpty()
        return withContext(Dispatchers.Default) { RunTrack.decode(raw).toGeoPoints() }
    }

    /** 그 시각에 시작한 러닝의 기록 번호 — 없으면 null */
    suspend fun idForStart(startedAt: Long): Long? = dao.idForStart(owner.first(), startedAt)

    suspend fun delete(id: Long): RecordDeletion {
        val who = owner.first()
        if (dao.deleteRecord(who, id) > 0) return RecordDeletion.Deleted
        val left = dao.observeRecord(who, id).first() ?: return RecordDeletion.Missing
        return if (left.isUploading()) RecordDeletion.Uploading else RecordDeletion.Missing
    }
}

/** 서버에 올려 보상을 확인받는 중 — 지우면 확인이 끝나지 않는다 */
fun WalkSessionEntity.isUploading(): Boolean =
    steps > 0 && (uploadState == UploadState.PENDING.name || uploadState == UploadState.FAILED.name)

fun RunRecordRow.isUploading(): Boolean =
    steps > 0 && (uploadState == UploadState.PENDING.name || uploadState == UploadState.FAILED.name)
