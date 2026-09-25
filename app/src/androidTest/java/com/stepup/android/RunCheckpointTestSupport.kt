package com.stepup.android

import com.stepup.android.core.ServiceLocator
import kotlinx.coroutines.runBlocking

/**
 * 러닝 저장본(체크포인트)을 비운다. 저장본은 앱 데이터에 남아 같은 에뮬레이터의 다음 테스트로 넘어가고,
 * 다른 러닝의 저장본이 남아 있으면 저장소가 새 러닝 저장을 거절한다("unresolved run cannot be
 * replaced"). 러닝을 저장하는 테스트는 시작할 때 이것을 불러 앞 테스트와 섞이지 않게 한다.
 */
internal fun clearAnyRunCheckpointForTest() = runBlocking {
    val store = ServiceLocator.runCheckpoints
    val existing = runCatching { store.read() }.getOrElse { store.setAsideUnreadable(); null }
    if (existing != null) store.clear(existing.state.startedAt, existing.state.recordingOwner)
}
