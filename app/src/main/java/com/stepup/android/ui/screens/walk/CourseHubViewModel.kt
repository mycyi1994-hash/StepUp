package com.stepup.android.ui.screens.walk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.CourseRepository
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.RunCourse
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

    fun select(id: Long) {
        viewModelScope.launch {
            // 같은 코스를 다시 누르면 선택 해제
            if (selectedId.value == id) courseRepository.clearSelection()
            else courseRepository.select(id)
        }
    }

    fun toggleLike(id: Long) {
        viewModelScope.launch { courseRepository.toggleLike(id) }
    }

    fun setShared(id: Long, shared: Boolean) {
        viewModelScope.launch { courseRepository.setShared(id, shared) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { courseRepository.delete(id) }
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
            if (id != null) courseRepository.select(id)
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
