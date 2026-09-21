package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.AppTheme
import com.stepup.android.core.ServiceLocator
import com.stepup.android.ui.components.DarkIconButton
import com.stepup.android.ui.components.Eyebrow
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.IconSquare
import com.stepup.android.ui.components.quietClickable
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.OnVolt
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.ThemeMode
import com.stepup.android.ui.theme.Volt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private data class ThemeOption(
    val mode: ThemeMode,
    val icon: ImageVector,
    val labelRes: Int,
    val noteRes: Int,
)

private val OPTIONS = listOf(
    ThemeOption(ThemeMode.SYSTEM, Icons.Filled.PhoneAndroid, R.string.theme_system, R.string.theme_system_note),
    ThemeOption(ThemeMode.LIGHT, Icons.Filled.LightMode, R.string.theme_light, R.string.theme_light_note),
    ThemeOption(ThemeMode.DARK, Icons.Filled.DarkMode, R.string.theme_dark, R.string.theme_dark_note),
)

/**
 * 화면 테마 설정.
 *
 * 고르는 즉시 바뀐다. 액티비티를 다시 만들지 않으므로 보던 자리가 그대로
 * 있고 색만 갈린다 — 언어와 달리 리소스를 다시 읽을 일이 없기 때문이다.
 */
@Composable
fun ThemeScreen(onBack: () -> Unit = {}) {
    val selected = AppTheme.mode

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 10.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DarkIconButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back),
                    onClick = onBack,
                )
                Column {
                    Eyebrow(text = stringResource(R.string.profile_account))
                    Text(
                        text = stringResource(R.string.settings_theme),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        color = Snow,
                    )
                }
            }
        }

        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 11.dp) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    IconSquare(icon = Icons.Filled.DarkMode, size = 38.dp, tint = Volt)
                    Text(
                        text = stringResource(R.string.theme_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = Silver,
                        lineHeight = 18.sp,
                    )
                }
            }
        }

        items(OPTIONS.size) { index ->
            val option = OPTIONS[index]
            ThemeRow(
                option = option,
                checked = option.mode == selected,
                onClick = {
                    if (option.mode != selected) {
                        AppTheme.change(option.mode)
                        // 저장은 화면 수명과 무관한 스코프에서 — 고르자마자
                        // 뒤로 나가도 선택이 남아야 한다
                        CoroutineScope(Dispatchers.IO).launch {
                            ServiceLocator.userPrefs.setThemeMode(option.mode.name)
                        }
                    }
                },
            )
        }
    }
}

@Composable
private fun ThemeRow(option: ThemeOption, checked: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CarbonHigh)
            .border(
                width = 1.dp,
                color = if (checked) Volt.copy(alpha = 0.6f) else Edge,
                shape = RoundedCornerShape(18.dp),
            )
            .quietClickable(onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = option.icon,
            contentDescription = null,
            tint = if (checked) Volt else Slate,
            modifier = Modifier.size(20.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.titleSmall,
                color = if (checked) Volt else Snow,
            )
            Text(text = stringResource(option.noteRes), fontSize = 11.sp, color = Slate)
        }
        if (checked) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Volt),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = OnVolt,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}
