package com.stepup.android.ui.screens.walk

import com.stepup.android.data.local.UploadState

/*
 * 크루 러닝 결과의 판단(2026-11-18 함께 뛰기 점검) — 화면 밖에서 검사한다(TogetherRunFixTest).
 */

/**
 * 크루 러닝 결과의 보상 줄 — 서버가 확인하기 전에는 금액을 적지 않는다.
 *
 * 다른 러닝은 확인 전에 이 폰이 셈한 값을 "예상 보상"으로 보이지만, 그 셈은 혼자 러닝 규칙(개인 에너지 · 신은 신발의 남은 내구도 ·
 * 번개 인원 배수)이다. 크루 러닝은 공용 체력 · 고른 신발 · 인원/레벨 보너스로 정산하고 공용 체력이 0 이던 구간은 빼므로,
 * 그 숫자가 서버가 줄 금액의 두 배이거나(체력이 중간에 바닥났을 때) 0 이었다(개인 에너지를 다 쓴 날). 확인 전에는 [pendingNote]
 * ("크루 보상은 정산이 끝나면 보여요") 한 줄만 두고, 확인된 뒤의 금액 · 무효 · 미지급은 그대로다.
 */
internal fun crewResultReward(reward: ResultReward, label: String, pendingNote: String): ResultReward =
    if (reward.settle == Settle.PENDING) {
        reward.copy(label = label, amount = "", notes = listOf(pendingNote) + reward.notes)
    } else {
        reward
    }

/**
 * 크루 러닝 한 줄(crew_run_result)을 읽을 때 — 서버가 이 러닝을 기록했으면. 적립이 0 이어도 읽는다: 예전엔 적립이 있을 때만 읽어,
 * 0 SUP 로 끝난 크루 러닝은 까닭(공용 체력을 다 씀 · 혼자 남아 끝남 · 크루 러닝으로 치지 않음)과 크루 레벨 줄이 끝내 보이지 않았다.
 */
internal fun crewOutcomeReady(uploadState: String?): Boolean = uploadState == UploadState.SIGNED.name

/** 크루 결과의 금액 줄(기본 · 인원 · 레벨 · 합)을 적을까 — 적립이 있을 때만. 0 이면 까닭 줄만 둔다 */
internal fun crewMoneyLines(total: Double?): Boolean = total != null && total > 0.0
