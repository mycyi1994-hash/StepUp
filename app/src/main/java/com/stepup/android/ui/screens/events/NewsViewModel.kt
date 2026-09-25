package com.stepup.android.ui.screens.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.RewardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * 뉴스 탭.
 *
 * 러닝 이벤트는 서버에서 받아 온다(하루 한 번 갱신). 특가 공지와 건강
 * 뉴스는 StepUp 이 쓴 글이라 앱 안에 그대로 있다.
 */
class NewsViewModel(rewardRepository: RewardRepository) : ViewModel() {

    val balance: StateFlow<Double?> = rewardRepository.balance
        .map<Double, Double?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * 러닝 이벤트와 러닝·건강 뉴스는 RunningFeedViewModel 이 맡는다.
     *
     * 여기는 머리글의 누적 리워드만 들고 있다. 옛 news_items 수집 경로는
     * 0009 의 news_articles 로 옮겨 갔고, 이 ViewModel 은 그 자리를 비웠다.
     */

    companion object {
        val Factory = viewModelFactory {
            initializer {
                NewsViewModel(ServiceLocator.rewardRepository)
            }
        }
    }
}
