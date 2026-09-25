package com.stepup.android.ui.screens.community

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.data.repo.CrewJoinPolicy
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.HexBadge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Snow

/**
 * 모임 만들기 — 당근 그룹처럼 이름·소개·활동 지역과 가입 방식만 받고 바로 개설한다.
 */
@Composable
fun CrewCreateScreen(
    onBack: () -> Unit = {},
    onCreated: (String) -> Unit = {},
    viewModel: CommunityViewModel = viewModel(factory = CommunityViewModel.Factory),
) {
    var name by rememberSaveable { mutableStateOf("") }
    var tagline by rememberSaveable { mutableStateOf("") }
    var area by rememberSaveable { mutableStateOf("") }
    var policy by rememberSaveable { mutableStateOf(CrewJoinPolicy.OPEN) }
    val creating by viewModel.creatingCrew.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf(false) }

    CrewNoticeToast(viewModel)

    val monogram = name.trim()
        .split(" ", "-", "_")
        .filter { it.isNotBlank() }
        .take(2)
        .map { it.first().uppercaseChar() }
        .joinToString("")
        .ifBlank { "ST" }

    com.stepup.android.ui.components.FormPage(
        title = stringResource(R.string.crew_create_title), onBack = onBack,
        actionLabel = stringResource(if (creating) R.string.feed_loading else R.string.crew_create_submit),
        actionEnabled = name.isNotBlank() && !creating, actionTag = "crew-create-submit",
        onAction = { viewModel.createCrew(name, tagline, area, policy, onCreated) },
    ) {
        item {
            if (!editing) {
                GlowCard(accent = true, contentPadding = PaddingValues(18.dp), spacing = 12.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(13.dp),
                    ) {
                        HexBadge(text = monogram, size = 64.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = name.ifBlank { stringResource(R.string.crew_create_preview_name) },
                                style = MaterialTheme.typography.titleLarge,
                                color = Snow,
                            )
                            Text(
                                text = tagline.ifBlank { stringResource(R.string.crew_create_preview_tag) },
                                fontSize = 14.sp,
                                color = Silver,
                            )
                        }
                    }
                }
            }
        }

        item {
            GlowCard(
                modifier = Modifier.onFocusChanged { editing = it.hasFocus },
                contentPadding = PaddingValues(16.dp), spacing = 14.dp,
            ) {
                LabeledField(
                    label = stringResource(R.string.crew_field_name),
                    value = name,
                    onValueChange = { name = it.take(28) },
                    placeholder = stringResource(R.string.crew_field_name_hint),
                )
                LabeledField(
                    label = stringResource(R.string.crew_field_tagline),
                    value = tagline,
                    onValueChange = { tagline = it.take(60) },
                    placeholder = stringResource(R.string.crew_field_tagline_hint),
                )
                LabeledField(
                    label = stringResource(R.string.crew_field_area),
                    value = area,
                    onValueChange = { area = it.take(28) },
                    placeholder = stringResource(R.string.crew_field_area_hint),
                )
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 12.dp) {
                // 나중에 크루 관리에서 바꿀 수 있다.
                CrewPolicyPicker(selected = policy, onSelect = { policy = it })
            }
        }
    }
}
