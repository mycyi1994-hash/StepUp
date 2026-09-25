package com.stepup.android.ui.screens.community

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.UploadState
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.PartyState
import com.stepup.android.data.repo.StepRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class PartyLobbyViewModel(
    private val crewRepository: CrewRepository,
    stepRepository: StepRepository,
) : ViewModel() {

    val party: StateFlow<PartyState> = crewRepository.party

    /**
     * 끝난 파티런의 적립액 — 서버가 확인한 값만. 확인 전이면 null (결과 화면은 "서버 확인 중").
     * 서버는 파티 보너스를 출발할 때 적은 명단으로만 주고, 러닝을 무효 · 상한 처리할 수 있다.
     */
    val resultServerPoints: StateFlow<Double?> = combine(party, stepRepository.recentSessions(10)) { state, rows ->
        if (state.resultStartedAt == 0L) {
            null
        } else {
            rows.firstOrNull { it.startedAt == state.resultStartedAt }
                ?.takeIf { it.uploadState == UploadState.SIGNED.name }
                ?.claimAmount?.toDoubleOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun openLobby(crewId: String) = crewRepository.openLobby(crewId)

    /** 번개러닝 로비. 크루 로비와 같은 판을 쓴다. */
    fun openFlashLobby(postId: Long, title: String) =
        crewRepository.openFlashLobby(postId, title)

    fun retry() = crewRepository.retryLobby()

    fun setReady(ready: Boolean) = crewRepository.setMyReady(ready)

    fun leaveLobby() = crewRepository.leaveLobby()

    fun dismissResult() = crewRepository.dismissResult()

    // ── 파티장 권한 ──

    fun startParty() = crewRepository.startParty()

    fun kick(memberId: String) = crewRepository.kick(memberId)

    companion object {
        val Factory = viewModelFactory {
            initializer { PartyLobbyViewModel(ServiceLocator.crewRepository, ServiceLocator.stepRepository) }
        }
    }
}
