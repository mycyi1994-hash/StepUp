package com.stepup.android.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.repo.AnnouncementRepository
import com.stepup.android.data.repo.NoticeAction
import com.stepup.android.data.repo.NoticeBlock
import com.stepup.android.data.repo.NoticeDetail
import com.stepup.android.data.repo.noticeBlocks
import com.stepup.android.ui.components.SecondaryHeader
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.theme.StepUpDesign
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

/**
 * 공지 상세(알림·공지 v1 16 · 19) — 경로 `notice/{id}`, 부모는 내 정보.
 *
 * 목록에서 받은 내용을 먼저 보이고 서버에 다시 묻는다. 서버가 주지 않으면(내림 · 비공개) "지금은 볼 수 없는 공지"로,
 * 닿지 못하면 받아 둔 내용을 그대로 보인다(일시적 통신 실패를 내린 공지로 보지 않는다). 글 아래 버튼은 정해 둔
 * 앱 안 화면으로 옮겨 갈 뿐이다 — 신발 뽑기로 가도 뽑기 기회를 쓰지 않는다.
 */
@Composable
fun NoticeDetailScreen(
    id: Long,
    onBack: () -> Unit = {},
    onAction: (NoticeAction) -> Unit = {},
    repository: AnnouncementRepository = ServiceLocator.announcementRepository,
) {
    var attempt by remember { mutableIntStateOf(0) }
    var detail by remember(id) {
        mutableStateOf<NoticeDetail>(repository.peek(id)?.let { NoticeDetail.Ready(it) } ?: NoticeDetail.Loading)
    }
    LaunchedEffect(id, attempt) {
        if (detail !is NoticeDetail.Ready) detail = NoticeDetail.Loading
        detail = try {
            repository.detail(id)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            repository.peek(id)?.let { NoticeDetail.Ready(it, saved = true) } ?: NoticeDetail.Failed
        }
    }
    NoticeDetailContent(detail, onBack, onAction, onRetry = { attempt++ })
}

@Composable
fun NoticeDetailContent(
    detail: NoticeDetail,
    onBack: () -> Unit = {},
    onAction: (NoticeAction) -> Unit = {},
    onRetry: () -> Unit = {},
    zone: ZoneId = ZoneId.systemDefault(),
) {
    val p = settingsPalette()
    Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
        SecondaryHeader(onBack = onBack, balance = null, onOpenWallet = null, title = stringResource(R.string.inbox_tab_notices))
        when (detail) {
            NoticeDetail.Loading -> DetailSkeleton()
            NoticeDetail.Failed -> PaneState(
                icon = Icons.Filled.Refresh, title = stringResource(R.string.notice_load_failed),
                body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
                onAction = onRetry, modifier = Modifier.testTag("notice-detail-failed"),
            )
            NoticeDetail.Unavailable -> PaneState(
                icon = Icons.AutoMirrored.Outlined.Article, title = stringResource(R.string.notice_unavailable_title),
                body = stringResource(R.string.notice_unavailable_body), actionLabel = stringResource(R.string.notice_back_to_list),
                onAction = onBack, boxed = false, modifier = Modifier.testTag("notice-unavailable"),
            )
            is NoticeDetail.Ready -> {
                val notice = detail.notice
                val language = LocalConfiguration.current.locales[0].language
                val date = rememberPattern(R.string.date_notice)
                Column(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                        .padding(top = 18.dp, bottom = 24.dp).testTag("notice-detail"),
                ) {
                    Text(stringResource(R.string.notice_kicker), color = p.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(notice.title(language), color = p.text, fontSize = 26.sp, fontWeight = FontWeight.SemiBold,
                        lineHeight = 1.3.em, modifier = Modifier.padding(top = 10.dp).semantics { heading() })
                    Text(date.format(Instant.ofEpochMilli(notice.publishedAt).atZone(zone)), color = p.secondary,
                        fontSize = 15.sp, modifier = Modifier.padding(top = 14.dp))
                    if (detail.saved) {
                        Text(stringResource(R.string.notice_saved_note), color = p.secondary, fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp).testTag("notice-saved"))
                    }
                    Rule(Modifier.padding(top = 22.dp, bottom = 4.dp))
                    NoticeBody(notice.body(language))
                }
                notice.action?.let { action ->
                    SettingsPrimaryButton(
                        stringResource(
                            when (action) {
                                NoticeAction.DRAW -> R.string.notice_action_draw
                                NoticeAction.RUN_HISTORY -> R.string.notice_action_history
                                NoticeAction.NOTIFICATION_SETTINGS -> R.string.inbox_open_settings
                                NoticeAction.PRIVACY_SETTINGS -> R.string.notice_action_privacy
                            },
                        ),
                        { onAction(action) },
                        Modifier.fillMaxWidth().padding(vertical = 12.dp).testTag("notice-action"),
                    )
                }
            }
        }
    }
}

/** 본문 — 서비스가 준 글 그대로. "## " 줄은 소제목, "---" 는 구분선, 그 아래 문단은 작은 안내 글자 */
@Composable
private fun NoticeBody(body: String) {
    val p = settingsPalette()
    var afterRule = false
    noticeBlocks(body).forEach { block ->
        when (block) {
            is NoticeBlock.Heading -> Text(block.text, color = p.text, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                lineHeight = 1.35.em, modifier = Modifier.padding(top = 26.dp).semantics { heading() })
            is NoticeBlock.Paragraph -> Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.lines.forEach { line ->
                    Text(line, color = if (afterRule) p.secondary else p.text, fontSize = if (afterRule) 15.sp else 16.sp,
                        lineHeight = 1.55.em)
                }
            }
            NoticeBlock.Rule -> {
                afterRule = true
                Rule(Modifier.padding(top = 26.dp))
            }
        }
    }
}

@Composable
private fun Rule(modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Box(modifier.fillMaxWidth().height(1.dp).background(p.divider))
}

/** 받는 동안 — 제목 · 날짜 · 본문 자리 */
@Composable
private fun DetailSkeleton() {
    val p = settingsPalette()
    Column(Modifier.fillMaxWidth().padding(top = 22.dp).testTag("notice-detail-loading"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(0.3f to 12.dp, 0.8f to 24.dp, 0.25f to 10.dp, 0f to 1.dp, 0.9f to 12.dp, 0.7f to 12.dp, 0.85f to 12.dp)
            .forEach { (fraction, height) ->
                if (fraction == 0f) {
                    Box(Modifier.padding(vertical = 8.dp).fillMaxWidth().height(height).background(p.divider))
                } else {
                    Box(Modifier.fillMaxWidth(fraction).height(height).clip(RoundedCornerShape(6.dp)).background(p.skeleton))
                }
            }
    }
}
