package com.stepup.android.ui.screens.walk

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.jetbrains.compose.resources.stringResource
import com.stepup.ui.res.*


/** 챌린지 화면 이름 — "10분 챌린지" */
@Composable
fun challengeTitle(goal: com.stepup.android.domain.RunGoal): String = stringResource(when (goal) {
    com.stepup.android.domain.RunGoal.TEN_MIN -> Res.string.run_challenge_title_10
    com.stepup.android.domain.RunGoal.ONE_KM -> Res.string.run_challenge_title_1k
    com.stepup.android.domain.RunGoal.THREE_KM -> Res.string.run_challenge_title_3k
})

