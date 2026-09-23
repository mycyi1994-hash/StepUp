package com.stepup.android.ui.screens.walk

import com.stepup.android.ui.experience.ExperienceEvents
import com.stepup.android.ui.experience.FeedbackCue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.BoardResult
import com.stepup.android.data.repo.BoardSyncState
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CourseHubViewModel(
    private val courseRepository: CourseRepository,
    prefs: UserPrefs,
) : ViewModel() {

    val courses: StateFlow<List<RunCourse>> = courseRepository.courses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedId: StateFlow<Long> = prefs.selectedCourseId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), -1L)

    /** 코스 게시판 — 서버의 공유 코스와 폰의 기본 코스 */
    val board: StateFlow<List<RunCourse>> = courseRepository.board
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val boardSync: StateFlow<BoardSyncState> = courseRepository.boardSync

    /**
     * 고른 코스의 길. 게시판의 서버 코스는 번호가 달라서, 이미 받아 둔 코스인지는
     * 길로 알아본다.
     */
    val selectedTrack: StateFlow<String?> = courseRepository.selectedCourse
        .map { it?.encode() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _problem = MutableStateFlow<BoardResult.Failed?>(null)

    /** 방금 한 일이 서버에서 막혔다. 화면이 한 번 띄우고 비운다. */
    val problem: StateFlow<BoardResult.Failed?> = _problem

    fun consumeProblem() {
        _problem.value = null
    }

    fun refreshBoard() {
        viewModelScope.launch { courseRepository.refreshBoard() }
    }

    fun select(id: Long) {
        viewModelScope.launch {
            val local = courseRepository.localIdFor(id)
            if (local == null) {
                ExperienceEvents.emit(FeedbackCue.Error)
                return@launch
            }
            // 같은 코스를 다시 누르면 선택 해제
            if (selectedId.value == local) courseRepository.clearSelection()
            else courseRepository.select(local)
            ExperienceEvents.emit(FeedbackCue.Select)
        }
    }

    fun toggleLike(id: Long) {
        viewModelScope.launch { report(courseRepository.toggleLike(id)) }
    }

    fun setShared(id: Long, shared: Boolean) {
        viewModelScope.launch { report(courseRepository.setShared(id, shared)) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { report(courseRepository.delete(id)) }
    }

    private fun report(result: BoardResult) {
        if (result is BoardResult.Failed) {
            _problem.value = result
            ExperienceEvents.emit(FeedbackCue.Error)
        }
    }

    /** 코스 녹화 중인가 — 러닝 화면이 저장 창을 띄울 상태 */
    val recording: StateFlow<Boolean> = courseRepository.recording
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * 녹화 시작 — 다음 러닝이 코스가 된다.
     *
     * @param onReady 깃발을 세운 뒤에 부른다. 화면은 이때 러닝 화면으로 간다.
     *   먼저 옮겨 가면 러닝 화면이 깃발을 아직 못 본 상태로 그려진다.
     */
    fun startRecording(onReady: () -> Unit) {
        viewModelScope.launch {
            courseRepository.beginRecording()
            onReady()
        }
    }

    fun cancelRecording() {
        viewModelScope.launch { courseRepository.cancelRecording() }
    }

    fun create(name: String, area: String, track: List<GeoPoint>, shared: Boolean) {
        viewModelScope.launch {
            val id = courseRepository.create(name, area, track, shared)
            if (id != null) {
                courseRepository.select(id)
                ExperienceEvents.emit(FeedbackCue.Success)
            } else ExperienceEvents.emit(FeedbackCue.Error)
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                CourseHubViewModel(
                    ServiceLocator.courseRepository,
                    ServiceLocator.userPrefs,
                )
            }
        }
    }
}
