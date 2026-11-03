package com.stepup.android.core

import com.stepup.android.data.prefs.UserPrefs
import com.stepup.android.data.repo.BoostRepository
import com.stepup.android.data.repo.EconomySync
import com.stepup.android.data.repo.RewardRepository
import com.stepup.android.data.repo.SneakerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 공용 화면이 쓰는 저장소(안드로이드 ServiceLocator 의 화면 쪽 자리, 2026-11-03 iOS C3). 앱이 켜질 때 한 번 채운다 —
 * iOS 는 IosApp, 안드로이드가 같은 화면을 쓰게 되면 ServiceLocator 가 같은 저장소를 넣는다. 이름은 안드로이드 ServiceLocator 와 같다.
 */
object AppServices {
    lateinit var userPrefs: UserPrefs
    lateinit var rewardRepository: RewardRepository
    lateinit var sneakerRepository: SneakerRepository
    lateinit var boostRepository: BoostRepository
    lateinit var economySync: EconomySync

    lateinit var forgeRepository: com.stepup.android.data.repo.ForgeRepository

    /** 다시 로그인하러 갈 때 앱이 할 일(달리던 러닝 멈추기 · 로그인 화면으로) */
    var onReturnToSignIn: suspend () -> Unit = {}

    /** 서버 경제인가(서버 주소가 있는 빌드는 언제나) */
    var serverEconomyOn: Boolean = true

    private val economyScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 서버 경제를 뒤에서 한 번 맞춘다(안드로이드 ServiceLocator.refreshEconomyInBackground 와 같다) */
    fun refreshEconomyInBackground() {
        economyScope.launch { runCatching { economySync.refresh() } }
    }
}
