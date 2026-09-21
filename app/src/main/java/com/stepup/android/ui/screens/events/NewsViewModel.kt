package com.stepup.android.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.local.NewsItemEntity
import com.stepup.android.data.repo.NewsRepository
import com.stepup.android.data.repo.RewardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 뉴스 탭.
 *
 * 러닝 이벤트는 서버에서 받아 온다(하루 한 번 갱신). 특가 공지와 건강
 * 뉴스는 StepUp 이 쓴 글이라 앱 안에 그대로 있다.
 */
class NewsViewModel(
    rewardRepository: RewardRepository,
    private val news: NewsRepository,
) : ViewModel() {

    val balance: StateFlow<Double> = rewardRepository.balance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0.0)

    val runEvents: StateFlow<List<NewsItemEntity>> = news.observe(NewsRepository.RUN_EVENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val refreshing = MutableStateFlow(false)

    init {
        // 열 때 한 번 물어본다. 하루 안에 이미 받았으면 그냥 돌아온다.
        refresh(force = false)
    }

    fun refresh(force: Boolean = true) {
        viewModelScope.launch {
            refreshing.value = true
            news.refresh(force = force)
            refreshing.value = false
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NewsViewModel(ServiceLocator.rewardRepository, ServiceLocator.newsRepository)
            }
        }
    }
}
