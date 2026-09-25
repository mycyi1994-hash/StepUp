package com.stepup.android.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.CommunityRepository
import com.stepup.android.data.local.RewardDao
import com.stepup.android.data.local.RewardType
import com.stepup.android.data.local.WalkSessionDao
import com.stepup.android.data.repo.CrewRepository
import com.stepup.android.data.repo.SneakerRepository
import com.stepup.android.data.repo.StepRepository
import com.stepup.android.domain.Achievement
import com.stepup.android.domain.AchievementBook
import com.stepup.android.domain.AchievementCategory
import com.stepup.android.domain.AchievementGrade
import com.stepup.android.domain.AchievementMetrics
import com.stepup.android.domain.RewardEconomy
import com.stepup.android.ui.components.BarMeter
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.GlowCard
import com.stepup.android.ui.components.PillChip
import com.stepup.android.ui.theme.CarbonHigh
import com.stepup.android.ui.theme.Edge
import com.stepup.android.ui.theme.Silver
import com.stepup.android.ui.theme.Slate
import com.stepup.android.ui.theme.Snow
import com.stepup.android.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn

/**
 * 업적 100종 — 실데이터로 잠금 해제를 판정한다.
 * 걸음·거리·세션은 기록 테이블, 민팅·강화·파티·이벤트는 SUP 원장,
 * 도감·레벨은 스니커즈 인벤토리, 글 수는 커뮤니티에서 온다.
 */
class AchievementsViewModel(
    stepRepository: StepRepository,
    sneakerRepository: SneakerRepository,
    crewRepository: CrewRepository,
    walkSessionDao: WalkSessionDao,
    rewardDao: RewardDao,
    communityRepository: CommunityRepository,
) : ViewModel() {

    init {
        // 크루와 글은 서버에만 있다. 업적 화면만 먼저 열어도 세어지도록 받아 둔다.
        viewModelScope.launch {
            crewRepository.refresh()
            communityRepository.refresh()
        }
    }

    private val activity = combine(
        stepRepository.observeLifetimeSteps(),
        stepRepository.streak,
        walkSessionDao.observeSessionCount(),
        rewardDao.observeEarnedTotal(),
    ) { steps, streak, sessions, earned ->
        AchievementMetrics(
            steps = steps,
            km = steps * RewardEconomy.STRIDE_METERS / 1000.0,
            sessions = sessions,
            streak = streak,
            supEarned = earned,
        )
    }

    private val economy = combine(
        // 서버 경제의 유료 뽑기는 SPEND_DRAW 로 적힌다(예전 폰 경제는 SPEND_MINT)
        rewardDao.observeCountByTypes(listOf(RewardType.SPEND_MINT, "SPEND_DRAW")),
        rewardDao.observeCountByType(RewardType.SPEND_UPGRADE),
        rewardDao.observeCountByType(RewardType.EARN_PARTY),
        rewardDao.observeCountByType(RewardType.EARN_EVENT),
    ) { mints, upgrades, partyRuns, eventClaims ->
        listOf(mints, upgrades, partyRuns, eventClaims)
    }

    private val social = combine(
        sneakerRepository.inventory,
        crewRepository.joinedCrewIds,
        communityRepository.myPostCount,
    ) { inventory, crews, posts ->
        Triple(inventory, crews.size, posts)
    }

    val achievements: StateFlow<List<Achievement>> = combine(
        activity,
        economy,
        social,
    ) { act, eco, (inventory, crewCount, postCount) ->
        AchievementBook.build(
            act.copy(
                mints = eco[0],
                upgrades = eco[1],
                partyRuns = eco[2],
                eventClaims = eco[3],
                collectionSlots = inventory.map { it.slotKey }.toSet().size,
                crews = crewCount,
                posts = postCount,
                maxSneakerLevel = inventory.maxOfOrNull { it.level } ?: 0,
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer {
                AchievementsViewModel(
                    stepRepository = ServiceLocator.stepRepository,
                    sneakerRepository = ServiceLocator.sneakerRepository,
                    crewRepository = ServiceLocator.crewRepository,
                    walkSessionDao = ServiceLocator.database.walkSessionDao(),
                    rewardDao = ServiceLocator.database.rewardDao(),
                    communityRepository = ServiceLocator.communityRepository,
                )
            }
        }
    }
}

@Composable
fun AchievementsScreen(
    onBack: () -> Unit = {},
    viewModel: AchievementsViewModel = viewModel(factory = AchievementsViewModel.Factory),
) {
    val achievements by viewModel.achievements.collectAsStateWithLifecycle()
    val unlockedCount = achievements.count { it.unlocked }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }

    val visible = if (filter == null) {
        achievements
    } else {
        achievements.filter { it.grade.name == filter }
    }

    DetailPage(title = stringResource(R.string.ach_all_title), onBack = onBack) {
        // 전체 진행 바
        item {
            GlowCard(contentPadding = PaddingValues(16.dp), spacing = 9.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.ach_total_progress),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Silver,
                    )
                    Text(
                        text = "$unlockedCount / ${achievements.size}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Volt,
                    )
                }
                BarMeter(
                    fraction = if (achievements.isEmpty()) {
                        0f
                    } else {
                        unlockedCount.toFloat() / achievements.size
                    },
                    height = 8.dp,
                )
            }
        }

        // 등급 필터
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    PillChip(
                        text = stringResource(R.string.post_cat_all),
                        selected = filter == null,
                        onClick = { filter = null },
                    )
                }
                items(AchievementGrade.entries.size) { i ->
                    val grade = AchievementGrade.entries[i]
                    PillChip(
                        text = grade.label(),
                        selected = filter == grade.name,
                        onClick = { filter = if (filter == grade.name) null else grade.name },
                        badge = achievements.count { it.grade == grade && it.unlocked },
                    )
                }
            }
        }

        items(visible.size) { index ->
            AchievementTile(achievement = visible[index])
        }
    }
}

// ── 라벨 · 색 · 아이콘 ───────────────────────────────────────

@Composable
fun AchievementGrade.label(): String = stringResource(
    when (this) {
        AchievementGrade.BRONZE -> R.string.ach_grade_bronze
        AchievementGrade.SILVER -> R.string.ach_grade_silver
        AchievementGrade.GOLD -> R.string.ach_grade_gold
        AchievementGrade.PLATINUM -> R.string.ach_grade_platinum
        AchievementGrade.DIAMOND -> R.string.ach_grade_diamond
    }
)

fun AchievementGrade.tint(): Color = when (this) {
    AchievementGrade.BRONZE -> Color(0xFFB4703C)
    AchievementGrade.SILVER -> Color(0xFF8C9BAD)
    AchievementGrade.GOLD -> Color(0xFFD9A400)
    AchievementGrade.PLATINUM -> Color(0xFF2FA898)
    AchievementGrade.DIAMOND -> Color(0xFF8B4DE8)
}

@Composable
private fun AchievementCategory.label(): String = stringResource(
    when (this) {
        AchievementCategory.STEPS -> R.string.ach_cat_steps
        AchievementCategory.DISTANCE -> R.string.ach_cat_distance
        AchievementCategory.SESSIONS -> R.string.ach_cat_sessions
        AchievementCategory.STREAK -> R.string.ach_cat_streak
        AchievementCategory.SUP -> R.string.ach_cat_sup
        AchievementCategory.MINT -> R.string.ach_cat_mint
        AchievementCategory.COLLECTION -> R.string.ach_cat_collection
        AchievementCategory.UPGRADE -> R.string.ach_cat_upgrade
        AchievementCategory.PARTY -> R.string.ach_cat_party
        AchievementCategory.EVENT -> R.string.ach_cat_event
        AchievementCategory.CREW -> R.string.ach_cat_crew
        AchievementCategory.POST -> R.string.ach_cat_post
        AchievementCategory.LEVEL -> R.string.ach_cat_level
    }
)

/** 임계값을 넣은 목표 설명 */
@Composable
private fun Achievement.goalText(): String {
    val n = "%,d".format(threshold.toLong())
    return when (category) {
        AchievementCategory.STEPS -> stringResource(R.string.ach_goal_steps, n)
        AchievementCategory.DISTANCE -> stringResource(R.string.ach_goal_distance, n)
        AchievementCategory.SESSIONS -> stringResource(R.string.ach_goal_sessions, n)
        AchievementCategory.STREAK -> stringResource(R.string.ach_goal_streak, n)
        AchievementCategory.SUP -> stringResource(R.string.ach_goal_sup, n)
        AchievementCategory.MINT -> stringResource(R.string.ach_goal_mint, n)
        AchievementCategory.COLLECTION -> stringResource(R.string.ach_goal_collection, n)
        AchievementCategory.UPGRADE -> stringResource(R.string.ach_goal_upgrade, n)
        AchievementCategory.PARTY -> stringResource(R.string.ach_goal_party, n)
        AchievementCategory.EVENT -> stringResource(R.string.ach_goal_event, n)
        AchievementCategory.CREW -> stringResource(R.string.ach_goal_crew, n)
        AchievementCategory.POST -> stringResource(R.string.ach_goal_post, n)
        AchievementCategory.LEVEL -> stringResource(R.string.ach_goal_level, n)
    }
}

private fun categoryIcon(category: AchievementCategory): ImageVector = when (category) {
    AchievementCategory.STEPS -> Icons.Filled.BarChart
    AchievementCategory.DISTANCE -> Icons.AutoMirrored.Filled.DirectionsWalk
    AchievementCategory.SESSIONS -> Icons.Filled.EmojiEvents
    AchievementCategory.STREAK -> Icons.Filled.Whatshot
    AchievementCategory.SUP -> Icons.Filled.AccountBalanceWallet
    AchievementCategory.MINT -> Icons.Filled.AutoAwesome
    AchievementCategory.COLLECTION -> Icons.Filled.WorkspacePremium
    AchievementCategory.UPGRADE -> Icons.Filled.TrendingUp
    AchievementCategory.PARTY -> Icons.Filled.Bolt
    AchievementCategory.EVENT -> Icons.Filled.Redeem
    AchievementCategory.CREW -> Icons.Filled.Groups
    AchievementCategory.POST -> Icons.Filled.Forum
    AchievementCategory.LEVEL -> Icons.Filled.Shield
}

/** 업적 카드 — 등급색 헥사곤 배지 + "카테고리 로마숫자" + 목표 */
@Composable
private fun AchievementTile(
    achievement: Achievement,
    modifier: Modifier = Modifier,
) {
    val gradeColor = achievement.grade.tint()
    GlowCard(modifier = modifier, contentPadding = PaddingValues(18.dp), spacing = 12.dp, accent = achievement.unlocked) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            HexAchievementBadge(icon = categoryIcon(achievement.category), unlocked = achievement.unlocked,
                color = gradeColor, badgeSize = 60.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("${achievement.category.label()} ${AchievementBook.roman(achievement.tier)}",
                    style = MaterialTheme.typography.titleMedium, color = Snow)
                Text(achievement.goalText(), style = MaterialTheme.typography.bodyMedium, color = Silver)
                Text(achievement.grade.label(), style = MaterialTheme.typography.labelLarge, color = Silver)
            }
        }
        BarMeter(fraction = achievement.progress, modifier = Modifier.fillMaxWidth(), height = 7.dp,
            color = if (achievement.unlocked) Volt else gradeColor)
    }
}

/** 6각형 배지 — 등급색 스트로크, 잠금 시 Lock */
@Composable
private fun HexAchievementBadge(
    icon: ImageVector,
    unlocked: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    badgeSize: Dp = 48.dp,
) {
    Box(modifier = modifier.size(badgeSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = min(size.width, size.height) / 2f * 0.94f
            val hex = Path().apply {
                for (i in 0 until 6) {
                    val angle = (-90f + i * 60f) * (PI / 180.0)
                    val x = cx + r * cos(angle).toFloat()
                    val y = cy + r * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(hex, color = if (unlocked) color.copy(alpha = 0.12f) else CarbonHigh)
            drawPath(
                path = hex,
                color = if (unlocked) color else Edge,
                style = Stroke(width = 1.6.dp.toPx()),
            )
        }
        Icon(
            imageVector = if (unlocked) icon else Icons.Filled.Lock,
            contentDescription = null,
            tint = if (unlocked) color else Slate,
            modifier = Modifier.size(badgeSize * 0.38f),
        )
    }
}
