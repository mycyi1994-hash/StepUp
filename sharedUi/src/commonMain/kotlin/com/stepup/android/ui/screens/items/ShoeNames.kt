package com.stepup.android.ui.screens.items

import androidx.compose.runtime.Composable
import com.stepup.android.domain.Sneaker
import com.stepup.android.ui.components.shoeModelNameRes
import com.stepup.android.ui.components.variantLabel
import com.stepup.ui.res.Res
import com.stepup.ui.res.sdv_unknown_model
import org.jetbrains.compose.resources.stringResource

/*
 * 신발 이름(안드로이드 SneakerDetailScreen.kt 의 shoeName · modelShoeName 과 같다) — 신발 상세를 옮길 때 그 파일의 것을 이것으로 대신한다.
 */

/** 화면에 쓰는 신발 이름 — 내가 붙인 이름(이 폰에만)이 있으면 그것, 아니면 모델 이름 */
@Composable
fun Sneaker.shoeName(): String =
    com.stepup.android.ui.components.LocalShoeNicknames.current[id] ?: modelShoeName()

/** 내가 붙인 이름을 빼고 읽은 원래 이름. 이 앱이 모르는 새 도감 번호면 다른 신발 이름 대신 "신발 모델 1401" */
@Composable
fun Sneaker.modelShoeName(): String {
    val model = modelId
    return if (model != null && shoeModelNameRes(model) == null) stringResource(Res.string.sdv_unknown_model, model) else variantLabel()
}
