package com.stepup.android.ui.screens.community.crew

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.stepup.android.R
import com.stepup.android.domain.CrewArea
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.components.rememberCurrentLocation
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.StepUpSans
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

/** 지역 검색을 연 곳 — 목록(02)은 "현재 위치"를 내 위치 기준으로 두고, 만들기 · 수정은 동네 이름이 있는 곳을 받는다 */
enum class CrewRegionTarget { LIST, DRAFT }

/**
 * 03 활동 지역 선택 · 71 위치 사용 선택 · 72 검색 결과 없음. 결과를 누르면 연 화면으로 돌아간다([onPicked]).
 * 목록에서 "현재 위치"를 고르면 null(내 위치 기준), 만들기 · 수정에서는 지금 위치의 동네 이름을 찾아 넘긴다.
 * 동네 이름을 찾지 못하면 좌표를 이름처럼 적지 않고 직접 검색하게 한다.
 */
@Composable
fun CrewRegionScreen(viewModel: CrewRegionViewModel, target: CrewRegionTarget, onBack: () -> Unit, onPicked: (CrewArea?) -> Unit) {
    val ink = crewInk()
    val context = LocalContext.current
    val query by viewModel.query.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sheet by rememberSaveable { mutableStateOf(false) }
    var locating by rememberSaveable { mutableStateOf(false) }
    var hereFailed by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val here = rememberCurrentLocation(enabled = locating)
    val latestHere by rememberUpdatedState(here)

    fun useHere() {
        hereFailed = false
        if (target == CrewRegionTarget.LIST) onPicked(null) else locating = true
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (StepPermissions.hasLocation(context)) useHere()
    }
    LaunchedEffect(locating) {
        if (!locating) return@LaunchedEffect
        val point = withTimeoutOrNull(12_000) { snapshotFlow { latestHere }.filterNotNull().first() }
        val area = point?.let { viewModel.areaAt(it) }
        locating = false
        if (area != null) onPicked(area) else hereFailed = true
    }

    CrewPage(Modifier.imePadding().testTag("crew-region-search")) {
        CrewTopBar(stringResource(R.string.crew_region_title), onBack)
        Column(Modifier.weight(1f).fillMaxWidth().padding(horizontal = CrewGutter)) {
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(RoundedCornerShape(18.dp)).background(ink.card).padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = ink.secondary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it.replace('\n', ' ').take(40)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    textStyle = TextStyle(fontFamily = StepUpSans, color = ink.text, fontSize = 16.sp),
                    cursorBrush = SolidColor(ink.info),
                    modifier = Modifier.weight(1f).focusRequester(focus).testTag("crew-region-input"),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text(stringResource(R.string.crew_region_hint), color = ink.secondary, fontSize = 16.sp)
                            inner()
                        }
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth().heightIn(min = 52.dp).clip(RoundedCornerShape(12.dp))
                    .feedbackClickable(enabled = !locating, role = Role.Button) {
                        if (StepPermissions.hasLocation(context)) useHere() else sheet = true
                    }
                    .testTag("crew-region-here"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = null, tint = ink.info, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(16.dp))
                Text(stringResource(R.string.crew_region_here_find), color = ink.info, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (locating) CircularProgressIndicator(Modifier.size(18.dp), color = ink.info, strokeWidth = 2.dp)
            }
            if (hereFailed) {
                CrewHelp(stringResource(R.string.crew_region_here_failed), error = true, modifier = Modifier.padding(bottom = 6.dp).testTag("crew-region-here-failed"))
            }
            Spacer(Modifier.height(16.dp))
            when (val current = state) {
                CrewRegionState.Idle -> Unit
                CrewRegionState.Searching -> Text(stringResource(R.string.crew_region_searching), color = ink.secondary, fontSize = 13.sp)
                is CrewRegionState.Found -> {
                    Text(stringResource(R.string.crew_region_results), color = ink.secondary, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("crew-region-results")) {
                        items(current.areas, key = { "${it.name}|${it.address}|${it.lat}|${it.lng}" }) { area ->
                            CrewRow(
                                area.name, { onPicked(area) }, Modifier.testTag("crew-region-result"),
                                value = area.address.takeIf { it.isNotBlank() }, titleSize = 18.sp,
                            )
                        }
                    }
                }
                CrewRegionState.Empty -> CrewEmptyState(
                    icon = { Icon(Icons.Filled.Search, null, tint = ink.info, modifier = Modifier.size(40.dp)) },
                    title = stringResource(R.string.crew_region_empty_title),
                    body = stringResource(R.string.crew_region_empty_body),
                    modifier = Modifier.padding(top = 56.dp).testTag("crew-region-empty"),
                ) {
                    CrewButton(stringResource(R.string.crew_region_retry), {
                        viewModel.setQuery("")
                        runCatching { focus.requestFocus() }
                    }, Modifier.testTag("crew-region-retry"))
                }
                CrewRegionState.Offline -> CrewEmptyState(
                    icon = { Icon(Icons.Filled.Refresh, null, tint = ink.info, modifier = Modifier.size(40.dp)) },
                    title = stringResource(R.string.crew_region_offline_title),
                    body = stringResource(R.string.crew_region_offline_body),
                    modifier = Modifier.padding(top = 56.dp).testTag("crew-region-offline"),
                ) {
                    CrewButton(stringResource(R.string.crew_region_retry), { viewModel.search() }, Modifier.testTag("crew-region-retry"))
                }
            }
        }
    }

    if (sheet) {
        // 71 위치를 쓰기 전에 묻는다 — 직접 검색으로도 고를 수 있다
        CrewSheet(stringResource(R.string.crew_location_title), { sheet = false }, Modifier.testTag("crew-location-sheet")) {
            Spacer(Modifier.height(18.dp))
            Text(stringResource(R.string.crew_location_body), color = ink.secondary, fontSize = 15.sp, lineHeight = 25.sp)
            Spacer(Modifier.height(64.dp))
            CrewButton(stringResource(R.string.crew_location_use), {
                sheet = false
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.testTag("crew-location-use"))
            Spacer(Modifier.height(16.dp))
            CrewButton(stringResource(R.string.crew_location_search), {
                sheet = false
                runCatching { focus.requestFocus() }
            }, Modifier.testTag("crew-location-search"), CrewButtonKind.SECONDARY)
        }
    }
}
