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
import androidx.compose.material.icons.outlined.ErrorOutline
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
import androidx.compose.ui.text.TextStyle
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
import com.stepup.android.ui.components.RunBackdrop
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.screens.onboarding.BluePlainButton
import com.stepup.android.ui.screens.onboarding.BlueRule
import com.stepup.android.ui.screens.onboarding.BlueTitleBar
import com.stepup.android.ui.theme.StepUpDesign
import com.stepup.android.ui.theme.StepUpSans
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.CancellationException

/**
 * 공지 상세(파란 톤 v4 NOT16 · NOT19) — 경로 `notice/{id}`, 부모는 내 정보(하단 5탭 유지).
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
    val t = runTone()
    Box(Modifier.fillMaxSize()) {
        RunBackdrop(Modifier.fillMaxSize())
        Column(Modifier.fillMaxSize().padding(horizontal = StepUpDesign.Gutter)) {
            BlueTitleBar(stringResource(R.string.inbox_tab_notices), onBack = onBack, backTag = "notice-back")
            when (detail) {
                // 아직 받는 중 — 제목 · 날짜 · 본문 자리(NOT36 계열). 예시 글을 미리 보이지 않는다
                NoticeDetail.Loading -> DetailSkeleton()
                // 받아 둔 사본도 없이 받지 못했다(NOT37 계열) — 내린 공지로 보지 않는다
                NoticeDetail.Failed -> PaneState(
                    icon = Icons.Outlined.ErrorOutline, title = stringResource(R.string.notice_load_failed),
                    body = stringResource(R.string.inbox_load_failed_body), actionLabel = stringResource(R.string.set_reload),
                    onAction = onRetry, modifier = Modifier.testTag("notice-detail-failed"),
                )
                // 서버가 내렸거나 비공개라고 확인했다(NOT19)
                NoticeDetail.Unavailable -> PaneState(
                    icon = Icons.AutoMirrored.Outlined.Article, title = stringResource(R.string.notice_unavailable_title),
                    body = stringResource(R.string.notice_unavailable_body), actionLabel = stringResource(R.string.notice_back_to_list),
                    onAction = onBack, modifier = Modifier.testTag("notice-unavailable"),
                )
                is NoticeDetail.Ready -> {
                    val notice = detail.notice
                    val language = LocalConfiguration.current.locales[0].language
                    val date = rememberPattern(R.string.date_notice)
                    Column(
                        Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                            .padding(top = 18.dp, bottom = 24.dp).testTag("notice-detail"),
                    ) {
                        Text(stringResource(R.string.notice_kicker), style = runTextStyle(17.sp, t.label, FontWeight.SemiBold))
                        Text(
                            notice.title(language),
                            style = TextStyle(
                                fontFamily = StepUpSans, fontWeight = FontWeight.ExtraBold, fontSize = 31.sp, lineHeight = 1.3.em,
                                letterSpacing = (-0.02).em, color = t.text,
                            ),
                            modifier = Modifier.padding(top = 8.dp).semantics { heading() },
                        )
                        Text(date.format(Instant.ofEpochMilli(notice.publishedAt).atZone(zone)),
                            style = runTextStyle(17.sp, t.label, FontWeight.Medium), modifier = Modifier.padding(top = 12.dp))
                        if (detail.saved) {
                            // 통신 실패일 뿐 — 받아 둔 사본을 그대로 보인다(NOT38 계열)
                            Text(stringResource(R.string.notice_saved_note), style = runTextStyle(14.sp, t.label, FontWeight.Medium),
                                modifier = Modifier.padding(top = 8.dp).testTag("notice-saved"))
                        }
                        BlueRule(Modifier.padding(top = 22.dp, bottom = 4.dp), strong = true)
                        NoticeBody(notice.body(language))
                    }
                    // 정해 둔 앱 안 화면으로 옮겨 갈 뿐 — 뽑기 · 지갑 연결 · 보상을 실행하지 않는다. 없거나 모르는 action 이면 버튼도 없다
                    notice.action?.let { action ->
                        BluePlainButton(
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
}

/** 본문 — 서비스가 준 글 그대로. "## " 줄은 소제목, "---" 는 구분선, 그 아래 문단은 옅은 파랑 안내 글자 */
@Composable
private fun NoticeBody(body: String) {
    val t = runTone()
    var afterRule = false
    noticeBlocks(body).forEach { block ->
        when (block) {
            is NoticeBlock.Heading -> Text(block.text, style = runTextStyle(21.sp, t.text, FontWeight.ExtraBold, 1.35f),
                modifier = Modifier.padding(top = 26.dp).semantics { heading() })
            is NoticeBlock.Paragraph -> Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                block.lines.forEach { line ->
                    Text(line, style = runTextStyle(if (afterRule) 17.sp else 18.sp, if (afterRule) t.label else t.text,
                        FontWeight.Medium, 1.55f))
                }
            }
            NoticeBlock.Rule -> {
                afterRule = true
                BlueRule(Modifier.padding(top = 26.dp), strong = true)
            }
        }
    }
}

/** 받는 동안 — 제목 · 날짜 · 본문 자리 */
@Composable
private fun DetailSkeleton() {
    Column(Modifier.fillMaxWidth().padding(top = 22.dp).testTag("notice-detail-loading"),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        listOf(0.3f to 12.dp, 0.8f to 24.dp, 0.25f to 10.dp, 0f to 1.dp, 0.9f to 12.dp, 0.7f to 12.dp, 0.85f to 12.dp)
            .forEach { (fraction, height) ->
                if (fraction == 0f) {
                    BlueRule(Modifier.padding(vertical = 8.dp), strong = true)
                } else {
                    SkeletonBar(fraction, height)
                }
            }
    }
}
