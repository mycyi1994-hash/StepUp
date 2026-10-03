package com.stepup.android.ui.screens.community.crew

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.stepup.android.ui.components.RunSpinner
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
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
            Spacer(Modifier.height(10.dp))
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(ink.field, shape)
                    .border(1.5.dp, Color(0xFF2E6BE6), shape).padding(start = 18.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = ink.text, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(14.dp))
                BasicTextField(
                    value = query,
                    onValueChange = { viewModel.setQuery(it.replace('\n', ' ').take(40)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { viewModel.search() }),
                    textStyle = TextStyle(fontFamily = StepUpSans, color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                    cursorBrush = SolidColor(ink.info),
                    modifier = Modifier.weight(1f).focusRequester(focus).testTag("crew-region-input"),
                    decorationBox = { inner ->
                        Box {
                            if (query.isEmpty()) Text(stringResource(R.string.crew_region_hint), color = ink.secondary, fontSize = 18.sp)
                            inner()
                        }
                    },
                )
                if (query.isNotEmpty()) {
                    val clear = stringResource(R.string.crew_blue_clear)
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).feedbackClickable(role = Role.Button) {
                            viewModel.setQuery("")
                            runCatching { focus.requestFocus() }
                        }.semantics { contentDescription = clear }.testTag("crew-region-clear"),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(22.dp).clip(CircleShape).background(ink.secondary), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = ink.field, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            // 현재 위치 — 권한이 있으면 바로, 없으면 앱 설명(71) 뒤 OS 권한 창
            Row(
                Modifier.fillMaxWidth().heightIn(min = 60.dp).clip(shape).background(ink.secondaryButton, shape)
                    .border(1.5.dp, Color(0xFF2E6BE6), shape)
                    .feedbackClickable(enabled = !locating, role = Role.Button) {
                        if (StepPermissions.hasLocation(context)) useHere() else sheet = true
                    }
                    .padding(horizontal = 16.dp)
                    .testTag("crew-region-here"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                if (locating) RunSpinner(Modifier.size(22.dp)) else Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ink.info, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.crew_region_here_find), color = ink.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            if (hereFailed) {
                Spacer(Modifier.height(8.dp))
                CrewErrorLine(stringResource(R.string.crew_region_here_failed), Modifier.padding(bottom = 6.dp).testTag("crew-region-here-failed"))
            }
            Spacer(Modifier.height(22.dp))
            when (val current = state) {
                CrewRegionState.Idle -> Unit
                CrewRegionState.Searching -> Row(verticalAlignment = Alignment.CenterVertically) {
                    RunSpinner(Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.crew_region_searching), color = ink.secondary, fontSize = 16.sp)
                }
                is CrewRegionState.Found -> {
                    Text(stringResource(R.string.crew_region_results), style = crewTitleStyle(ink.text, 26.sp))
                    Spacer(Modifier.height(6.dp))
                    LazyColumn(Modifier.fillMaxWidth().weight(1f).testTag("crew-region-results")) {
                        items(current.areas, key = { "${it.name}|${it.address}|${it.lat}|${it.lng}" }) { area ->
                            CrewRegionRow(area) { onPicked(area) }
                        }
                    }
                }
                CrewRegionState.Empty -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    CrewEmptyState(
                        icon = { CrewStateIcon(Icons.Filled.Search, circled = true) },
                        title = stringResource(R.string.crew_region_empty_title),
                        body = stringResource(R.string.crew_region_empty_body),
                        modifier = Modifier.padding(top = 40.dp).testTag("crew-region-empty"),
                    ) {
                        CrewButton(stringResource(R.string.crew_region_retry), {
                            viewModel.setQuery("")
                            runCatching { focus.requestFocus() }
                        }, Modifier.testTag("crew-region-retry"))
                    }
                }
                CrewRegionState.Offline -> Column(Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState())) {
                    CrewEmptyState(
                        icon = { CrewStateIcon(Icons.Filled.Refresh) },
                        title = stringResource(R.string.crew_region_offline_title),
                        body = stringResource(R.string.crew_region_offline_body),
                        modifier = Modifier.padding(top = 40.dp).testTag("crew-region-offline"),
                    ) {
                        CrewButton(stringResource(R.string.crew_region_retry), { viewModel.search() }, Modifier.testTag("crew-region-retry"))
                    }
                }
            }
        }
    }

    if (sheet) {
        // 71 위치를 쓰기 전에 묻는 앱 설명(OS 권한 창과 별개) — 직접 검색으로도 고를 수 있다
        CrewSheet(null, { sheet = false }, Modifier.testTag("crew-location-sheet")) {
            Box(Modifier.fillMaxWidth()) {
                CrewSheetClose({ sheet = false }, Modifier.align(Alignment.TopEnd))
                Column(Modifier.fillMaxWidth().padding(top = 44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.crew_location_title), style = crewTitleStyle(ink.text, 25.sp), textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.crew_location_body), color = ink.text, fontSize = 16.sp, lineHeight = 25.sp, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(26.dp))
            CrewButton(stringResource(R.string.crew_location_use), {
                sheet = false
                permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.testTag("crew-location-use"))
            Spacer(Modifier.height(8.dp))
            CrewButton(stringResource(R.string.crew_location_search), {
                sheet = false
                runCatching { focus.requestFocus() }
            }, Modifier.testTag("crew-location-search"), CrewButtonKind.SECONDARY)
        }
    }
}

/** 검색 결과 한 줄 — 위치 그림 · 동네 이름 · 주소 · 파란 꺾쇠 */
@Composable
private fun CrewRegionRow(area: CrewArea, onClick: () -> Unit) {
    val ink = crewInk()
    Column(Modifier.fillMaxWidth().testTag("crew-region-result")) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 72.dp).feedbackClickable(role = Role.Button, onClick = onClick).padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, tint = ink.text, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f).padding(start = 16.dp)) {
                Text(area.name, color = ink.text, fontSize = 19.sp, fontWeight = FontWeight.Bold, maxLines = 2)
                if (area.address.isNotBlank()) {
                    Text(area.address, color = ink.text.copy(alpha = 0.86f), fontSize = 15.sp, maxLines = 2)
                }
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = ink.link, modifier = Modifier.size(26.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(ink.divider))
    }
}
