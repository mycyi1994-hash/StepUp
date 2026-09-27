package com.stepup.android.ui.screens.community.stories

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ExternalIntents
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.PlaceSearchResult
import com.stepup.android.ui.StepPermissions
import com.stepup.android.ui.experience.feedbackClickable
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.VoltText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * 내 주변 — 목록 위 동네 이름을 누르면 온다. "내 위치 사용하기"는 실제 권한 요청, "지역 직접 선택"은
 * 권한 없이 지역 검색. 권한을 허락받기 전의 안내를 허락받은 것과 섞지 않는다.
 */
@Composable
fun StoryLocationScreen(onBack: () -> Unit, onChooseRegion: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var denied by rememberSaveable { mutableStateOf(false) }
    val region by ServiceLocator.userPrefs.storyRegion.collectAsState(initial = null)
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (StepPermissions.hasLocation(context)) {
            // 내 위치로 본다 — 고른 지역을 푼다
            scope.launch { ServiceLocator.userPrefs.setStoryRegion(null); onBack() }
        } else {
            denied = true
        }
    }
    val useLocation = {
        if (StepPermissions.hasLocation(context)) {
            scope.launch { ServiceLocator.userPrefs.setStoryRegion(null); onBack() }
            Unit
        } else {
            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
    Column(Modifier.fillMaxSize().testTag("story-location")) {
        StoryHeader(stringResource(R.string.story_location_screen_title), onBack)
        StoryLocationPrompt(
            locating = false,
            denied = denied,
            onUseLocation = useLocation,
            onChooseRegion = onChooseRegion,
            onOpenSettings = { ExternalIntents.openAppSettings(context) },
            modifier = Modifier.weight(1f),
        )
        region?.let {
            Text(
                stringResource(R.string.story_location_current_region, it.name),
                color = Slate, fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 14.dp),
            )
        }
    }
}

/** 지역 직접 선택 — 동 · 구 이름으로 찾아 그 지역 한가운데를 기준점으로 쓴다 */
@Composable
fun StoryRegionScreen(onBack: () -> Unit, onChosen: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var query by rememberSaveable { mutableStateOf("") }
    var result by remember { mutableStateOf<PlaceSearchResult?>(null) }
    var searching by remember { mutableStateOf(false) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(query, attempt) {
        result = null
        if (query.isBlank()) return@LaunchedEffect
        searching = true
        delay(350)
        result = ServiceLocator.placeSearch.regions(query, null)
        searching = false
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (StepPermissions.hasLocation(context)) scope.launch { ServiceLocator.userPrefs.setStoryRegion(null); onChosen() }
    }
    Column(Modifier.fillMaxSize().imePadding().testTag("story-region")) {
        StoryHeader(stringResource(R.string.story_region_title), onBack)
        StorySearchField(
            value = query,
            onValueChange = { query = it },
            placeholder = stringResource(R.string.story_region_hint),
            modifier = Modifier.padding(horizontal = StoryFormGutter),
            tag = "story-region-search",
        )
        LazyColumn(
            Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(start = StoryFormGutter, end = StoryFormGutter, top = 8.dp, bottom = 24.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 52.dp).feedbackClickable(role = Role.Button, onClick = {
                        if (StepPermissions.hasLocation(context)) {
                            scope.launch { ServiceLocator.userPrefs.setStoryRegion(null); onChosen() }
                        } else {
                            permission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                    }).testTag("story-region-my-location"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, tint = Silver, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.story_region_my_location), color = Silver, fontSize = 14.sp)
                }
            }
            val found = (result as? PlaceSearchResult.Found)?.places?.distinctBy { it.key }
            when {
                query.isBlank() -> Unit
                searching || result == null -> item {
                    Text(stringResource(R.string.story_place_searching), color = Slate, fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 12.dp))
                }
                result is PlaceSearchResult.Offline -> item {
                    StoryStateBlock(Icons.Filled.Refresh, stringResource(R.string.story_place_offline_title),
                        stringResource(R.string.story_error_body)) {
                        StoryButton(stringResource(R.string.story_retry), { attempt++ })
                    }
                }
                found.isNullOrEmpty() -> item {
                    StoryStateBlock(Icons.Filled.Search, stringResource(R.string.story_region_empty_title),
                        stringResource(R.string.story_place_empty_body)) {
                        StoryTextButton(stringResource(R.string.story_place_clear), { query = "" }, color = VoltText)
                    }
                }
                else -> {
                    item {
                        Text(stringResource(R.string.story_region_results), color = Slate, fontSize = 12.sp,
                            modifier = Modifier.padding(top = 6.dp, bottom = 4.dp))
                    }
                    items(found, key = { it.key }) { place ->
                        StoryPlaceRow(place, "") {
                            scope.launch { ServiceLocator.userPrefs.setStoryRegion(place); onChosen() }
                        }
                        StoryDivider()
                    }
                }
            }
        }
    }
}
