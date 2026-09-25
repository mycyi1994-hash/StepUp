package com.stepup.android.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.stepup.android.R
import com.stepup.android.domain.PostCategory
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow

/**
 * 글쓰기.
 *
 * 번개러닝을 고르면 장소·거리·출발까지 남은 시간·정원 입력이 함께 열린다.
 * crewId가 비어 있으면 전체 게시판에, 값이 있으면 그 크루 게시판에 올라간다.
 */
@OptIn(ExperimentalLayoutApi::class)
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
    var distance by rememberSaveable { mutableStateOf("1.0") }
    var startsIn by rememberSaveable { mutableStateOf("60") }
    var capacity by rememberSaveable { mutableStateOf("6") }

    val selected = PostCategory.of(category)
    // 번개러닝은 쓰는 자리가 모임 장소다. 읽는 사람의 폰이 이 좌표에서 거리를 잰다.
    val here = rememberCurrentLocation(enabled = selected == PostCategory.FLASH)
    val posting by viewModel.posting.collectAsStateWithLifecycle()
    val parsedDistance = distance.trim().replace(',', '.').toDoubleOrNull()
    val parsedMinutes = startsIn.trim().toIntOrNull()
    val parsedCapacity = capacity.trim().toIntOrNull()
    val validNumbers = parsedDistance != null && parsedDistance.isFinite() && parsedDistance > 0 &&
        parsedMinutes != null && parsedMinutes > 0 && parsedCapacity != null && parsedCapacity in 2..200
    val canSubmit = !posting && title.isNotBlank() &&
        (selected != PostCategory.FLASH || (place.isNotBlank() && validNumbers))

    // 올리지 못했으면(로그인·연결) 이유를 띄운다. 화면은 닫지 않아 쓴 글이 남는다.
    BoardNoticeToast(viewModel)

    com.stepup.android.ui.components.FormPage(
        title = stringResource(R.string.post_write), onBack = onBack,
        actionLabel = stringResource(if (posting) R.string.feed_loading else R.string.post_submit),
        actionEnabled = canSubmit, actionTag = "post-submit",
        onAction = {
            viewModel.writePost(
                category = selected,
                title = title,
                body = body,
                crewId = crewId,
                place = place,
                distanceKm = if (selected == PostCategory.FLASH) requireNotNull(parsedDistance) else 0.0,
                meetInMinutes = if (selected == PostCategory.FLASH) requireNotNull(parsedMinutes) else 0,
                capacity = if (selected == PostCategory.FLASH) requireNotNull(parsedCapacity) else 0,
                lat = here?.lat,
                lng = here?.lng,
                onDone = onBack,
            )
        },
    ) {
        if (crewName.isNotBlank()) {
            item { Text(crewName, color = Silver, fontSize = 14.sp) }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.post_type), style = MaterialTheme.typography.titleSmall, color = Snow)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PostCategory.entries.forEach { entry ->
                        PillChip(
                            text = entry.label(),
                            selected = selected == entry,
                            onClick = { category = entry.id },
                        )
                    }
                }
            }
        }

        item {
            LabeledField(
                label = stringResource(R.string.post_field_title),
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(R.string.post_field_title_hint),
            )
        }
        item {
            LabeledField(
                label = stringResource(R.string.post_field_body),
                value = body,
                onValueChange = { body = it },
                placeholder = stringResource(R.string.post_field_body_hint),
                singleLine = false,
                minHeight = 144,
            )
        }

        if (selected == PostCategory.FLASH) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = stringResource(R.string.post_flash_details),
                        style = MaterialTheme.typography.titleSmall,
                        color = Snow,
                    )
                    LabeledField(
                        label = stringResource(R.string.post_field_place),
                        value = place,
                        onValueChange = { place = it },
                        placeholder = stringResource(R.string.post_field_place_hint),
                    )
                }
            }
            item {
                LabeledField(
                    label = stringResource(R.string.post_field_distance),
                    value = distance,
                    onValueChange = { distance = it },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    placeholder = "1.0",
                )
            }
            item {
                LabeledField(
                    label = stringResource(R.string.post_field_starts_in),
                    value = startsIn,
                    onValueChange = { startsIn = it },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    placeholder = "60",
                )
            }
            item {
                LabeledField(
                    label = stringResource(R.string.post_field_capacity),
                    value = capacity,
                    onValueChange = { capacity = it },
                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                    placeholder = "6",
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!validNumbers) {
                        Text(stringResource(R.string.post_invalid_numbers),
                            color = com.stepup.android.ui.theme.Alert,
                            style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        text = stringResource(
                            if (here != null) R.string.post_flash_hint else R.string.post_flash_no_location,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                    )
                }
            }
        }
    }
}
