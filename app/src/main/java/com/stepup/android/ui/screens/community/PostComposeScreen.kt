package com.stepup.android.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.PostCategory
import com.stepup.android.ui.components.BottomActionBar
import com.stepup.android.ui.components.ButtonState
import com.stepup.android.ui.components.FormField
import com.stepup.android.ui.components.GroupRow
import com.stepup.android.ui.components.GroupedList
import com.stepup.android.ui.components.NumberStepper
import com.stepup.android.ui.components.PrimaryButton
import com.stepup.android.ui.components.StepUpTabs
import com.stepup.android.ui.components.StepUpUi
import com.stepup.android.ui.components.rememberCurrentLocation
import java.util.Locale
import java.util.UUID

/**
 * 글쓰기 — UI 최종 통합안(2026-10-25) `compose` · `compose_flash`. 위 ← · "글쓰기" → 분류 세그먼트(번개러닝 · 자유 · 꿀팁) →
 * 칸 안에 이름이 있는 입력칸(제목 · 내용, 번개러닝은 만나는 곳) → 번개러닝이면 [−][값][+] 줄(달릴 거리 · 출발까지 · 정원) → 아래 올리기 하나.
 * 숫자는 조절 버튼으로만 바꿔 범위를 벗어나지 않는다(거리 0.5 ~ 50km · 출발 5분 ~ 24시간 · 정원 2 ~ 200명, 서버 규칙 그대로).
 */
@Composable
fun PostComposeScreen(
    crewId: String = "",
    crewName: String = "",
    onBack: () -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var category by rememberSaveable { mutableStateOf(PostCategory.FREE.id) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var place by rememberSaveable { mutableStateOf("") }
    var distance by rememberSaveable { mutableDoubleStateOf(1.0) }
    var startsIn by rememberSaveable { mutableIntStateOf(60) }
    var capacity by rememberSaveable { mutableIntStateOf(6) }
    // 글쓰기 화면마다 한 요청 키 — 응답을 못 받고 다시 올려도 서버(0062)가 같은 글 번호를 돌려준다(번개 모임이 둘이 되지 않게)
    var clientKey by rememberSaveable { mutableStateOf(UUID.randomUUID().toString()) }

    val selected = PostCategory.of(category)
    val flash = selected == PostCategory.FLASH
    // 번개러닝은 쓰는 자리가 모임 장소다. 읽는 사람의 폰이 이 좌표에서 거리를 잰다.
    val here = rememberCurrentLocation(enabled = flash)
    val posting by viewModel.posting.collectAsStateWithLifecycle()
    val canSubmit = !posting && title.isNotBlank() && (!flash || place.isNotBlank())
    val c = StepUpUi.colors

    // 올리지 못했으면(로그인·연결) 이유를 띄운다. 화면은 닫지 않아 쓴 글이 남는다.
    BoardNoticeToast(viewModel)

    Column(Modifier.fillMaxSize().imePadding().testTag("post-compose")) {
        Box(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 12.dp)) {
            com.stepup.android.ui.components.BackButton(onBack, modifier = Modifier.align(Alignment.CenterStart))
            Text(stringResource(R.string.post_write), style = StepUpUi.type.navTitle, color = c.label,
                modifier = Modifier.align(Alignment.Center).semantics { heading() })
        }
        val categories = PostCategory.entries
        StepUpTabs(
            categories.map { it.label() }, categories.indexOf(selected), { category = categories[it].id },
            Modifier.fillMaxWidth().padding(horizontal = StepUpUi.Gutter).padding(top = 6.dp),
            tags = categories.map { "post-cat-${it.id.lowercase(Locale.ROOT)}" },
        )
        Column(
            Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()).padding(top = 14.dp).testTag("form-content"),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (crewName.isNotBlank()) {
                Text(crewName, style = StepUpUi.type.rowSubtitle, color = c.secondaryLabel, modifier = Modifier.padding(horizontal = StepUpUi.TitleGutter))
            }
            FormField(
                stringResource(R.string.post_field_title), title, { title = it },
                Modifier.padding(horizontal = StepUpUi.Gutter), placeholder = stringResource(R.string.post_field_title_hint),
            )
            FormField(
                stringResource(R.string.post_field_body), body, { body = it },
                Modifier.padding(horizontal = StepUpUi.Gutter), placeholder = stringResource(R.string.post_field_body_hint),
                singleLine = flash, minLines = if (flash) 1 else 8, minHeight = if (flash) 56.dp else 240.dp,
            )
            if (flash) {
                FormField(
                    stringResource(R.string.post_field_place), place, { place = it },
                    Modifier.padding(horizontal = StepUpUi.Gutter), placeholder = stringResource(R.string.post_field_place_hint),
                )
                Spacer(Modifier.height(4.dp))
                GroupedList(
                    rows = listOf(
                        {
                            StepperRow(stringResource(R.string.post_field_distance_short), String.format(Locale.ROOT, "%.1f", distance), "km",
                                onMinus = { distance = (distance - 0.5).coerceAtLeast(0.5) }, onPlus = { distance = (distance + 0.5).coerceAtMost(50.0) },
                                minus = distance > 0.5, plus = distance < 50.0, tag = "post-distance")
                        },
                        {
                            StepperRow(stringResource(R.string.post_field_starts_short), startsIn.toString(), stringResource(R.string.post_unit_min),
                                onMinus = { startsIn = stepMinutes(startsIn, -1) }, onPlus = { startsIn = stepMinutes(startsIn, +1) },
                                minus = startsIn > 5, plus = startsIn < 24 * 60, tag = "post-starts")
                        },
                        {
                            StepperRow(stringResource(R.string.post_field_capacity), capacity.toString(), stringResource(R.string.post_unit_people),
                                onMinus = { capacity = (capacity - 1).coerceAtLeast(2) }, onPlus = { capacity = (capacity + 1).coerceAtMost(200) },
                                minus = capacity > 2, plus = capacity < 200, tag = "post-capacity")
                        },
                    ),
                )
                Text(
                    stringResource(if (here != null) R.string.post_flash_hint else R.string.post_flash_no_location),
                    style = StepUpUi.type.caption, color = c.secondaryLabel,
                    modifier = Modifier.padding(horizontal = StepUpUi.TitleGutter),
                )
            }
            Spacer(Modifier.height(12.dp))
        }
        BottomActionBar {
            PrimaryButton(
                stringResource(R.string.post_submit),
                onClick = {
                    viewModel.writePost(
                        category = selected,
                        title = title,
                        body = body,
                        crewId = crewId,
                        place = place,
                        distanceKm = if (flash) distance else 0.0,
                        meetInMinutes = if (flash) startsIn else 0,
                        capacity = if (flash) capacity else 0,
                        lat = here?.lat,
                        lng = here?.lng,
                        clientKey = clientKey,
                        onDone = {
                            // 올라갔다 — 이 화면에서 또 쓰면 새 글이다
                            clientKey = UUID.randomUUID().toString()
                            onBack()
                        },
                    )
                },
                state = when {
                    posting -> ButtonState.Loading
                    !canSubmit -> ButtonState.Disabled
                    else -> ButtonState.Idle
                },
                modifier = Modifier.weight(1f).testTag("post-submit"),
            )
        }
    }
}

/** 출발까지 — 1시간 안은 5분씩, 그 뒤는 30분씩(5분 ~ 24시간) */
internal fun stepMinutes(now: Int, dir: Int): Int {
    val step = if (now < 60 || (now == 60 && dir < 0)) 5 else 30
    return (now + dir * step).coerceIn(5, 24 * 60)
}

/** [−][값][+] 줄(시안 compose_flash) — 왼쪽 이름, 오른쪽 조절 칸 */
@Composable
private fun StepperRow(
    label: String,
    value: String,
    unit: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    minus: Boolean,
    plus: Boolean,
    tag: String,
) {
    GroupRow(minHeight = 68.dp) {
        Text(label, style = StepUpUi.type.rowTitle.copy(fontSize = 16.sp), color = StepUpUi.colors.label, modifier = Modifier.weight(1f))
        NumberStepper(
            value, unit, onMinus, onPlus, minusEnabled = minus, plusEnabled = plus,
            minusDescription = "$label ${stringResource(R.string.post_stepper_less)}",
            plusDescription = "$label ${stringResource(R.string.post_stepper_more)}",
            tag = tag,
        )
    }
}
