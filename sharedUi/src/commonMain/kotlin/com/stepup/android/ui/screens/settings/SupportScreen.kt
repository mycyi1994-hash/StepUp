package com.stepup.android.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.ui.res.*
import androidx.compose.ui.text.style.TextAlign
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueGroupLabel
import com.stepup.android.ui.components.BlueNavRow
import com.stepup.android.ui.components.BluePage
import com.stepup.android.ui.components.BlueSheet
import com.stepup.android.ui.components.BlueToast
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.blueListColors
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.delay

/**
 * 자주 묻는 질문 — 질문 · 답변 문자열 쌍(docs/redesign/settings-v1 의 FAQ 답변안). 적립률 · 출금 조건 · 확률을 새로
 * 약속하지 않는다. 뽑기는 무료 정책(0042) 그대로다.
 */
private val faqEntries = listOf(
    Res.string.set_faq_q_earn to Res.string.set_faq_a_earn,
    Res.string.set_faq_q_stopped to Res.string.set_faq_a_stopped,
    Res.string.set_faq_q_grade to Res.string.set_faq_a_grade,
    Res.string.set_faq_q_withdraw to Res.string.set_faq_a_withdraw,
    Res.string.set_faq_q_draw to Res.string.set_faq_a_draw,
)

/**
 * 도움말 · 문의(설정 v1 21~23 · 28 → 파란 톤 v4 SET21~23 · 28 · 40). 질문은 하나씩 펼친다. 앱 사용 안내는 한 장짜리 다시 보기(시작·로그인·첫 사용 v1 시안 20)를 연다.
 * 문의는 메일 앱 열기와 주소 복사뿐이다 — 자동으로 보내지 않고, 위치 · 계정 정보 · 기록을 붙이지 않는다.
 */
@Composable
fun SupportScreen(onBack: () -> Unit = {}, onOpenGuide: () -> Unit = {}) {
    var expanded by rememberSaveable { mutableIntStateOf(-1) }
    var contactOpen by rememberSaveable { mutableStateOf(false) }
    SupportContent(
        expanded = expanded, contactOpen = contactOpen, onBack = onBack, onOpenGuide = onOpenGuide,
        onToggle = { index -> expanded = if (expanded == index) -1 else index },
        onOpenContact = { contactOpen = true }, onCloseContact = { contactOpen = false },
    )
}

@Composable
fun SupportContent(
    expanded: Int = -1,
    contactOpen: Boolean = false,
    contactFailed: Boolean = false,
    onBack: () -> Unit = {},
    onOpenGuide: () -> Unit = {},
    onToggle: (Int) -> Unit = {},
    onOpenContact: () -> Unit = {},
    onCloseContact: () -> Unit = {},
) {
    BluePage(title = stringResource(Res.string.set_support), onBack = onBack) {
        item {
            BlueGroupLabel(stringResource(Res.string.set_faq))
            // 질문은 하나만 펼친다
            BlueGroup {
                faqEntries.forEachIndexed { index, (question, answer) ->
                    FaqRow(stringResource(question), stringResource(answer), expanded == index, { onToggle(index) },
                        Modifier.testTag("faq-$index"))
                }
            }
        }
        item {
            BlueGroupLabel(stringResource(Res.string.set_guide), Modifier.padding(top = 6.dp))
            BlueGroup {
                BlueNavRow(stringResource(Res.string.onb_guide_row_desc), onClick = onOpenGuide, modifier = Modifier.testTag("support-guide"))
            }
        }
        item {
            RunButton(
                stringResource(Res.string.set_contact_mail), onOpenContact,
                Modifier.padding(top = 14.dp).testTag("support-contact"),
            )
        }
    }
    if (contactOpen) ContactSheet(onDismiss = onCloseContact, startFailed = contactFailed)
}

/** 질문 한 줄 — 누르면 그 자리에서 답을 펼친다(+ / −). 줄 전체가 하나의 누름 대상이다 */
@Composable
private fun FaqRow(question: String, answer: String, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val t = runTone()
    val c = blueListColors()
    Column(modifier.fillMaxWidth().background(c.face)) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 62.dp)
                .feedbackClickable(role = Role.Button, onClick = onToggle)
                // 읽기 도구가 펼침 · 접힘을 알린다
                .semantics { if (open) collapse { onToggle(); true } else expand { onToggle(); true } }
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(question, style = runTextStyle(18.sp, t.text, FontWeight.SemiBold, 1.35f), modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Filled.Remove else Icons.Filled.Add, contentDescription = null, tint = t.label,
                modifier = Modifier.size(28.dp))
        }
        if (open) {
            Text(answer, style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.6f),
                modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 18.dp))
        }
    }
}

/**
 * 메일로 문의(SET23) · 메일 앱을 열지 못함(SET28) · 주소 복사 완료(SET40). 메일 앱의 작성 창만 연다 — 여는 것은 문의 접수가 아니다.
 * 메일 앱이 없으면 주소 복사로 잇는다. 복사는 클립보드에 실제로 들어간 뒤에만 2.5초 알린다(시트와 버튼은 그대로).
 */
@Composable
fun ContactSheet(onDismiss: () -> Unit, startFailed: Boolean = false) {
    val t = runTone()
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val address = stringResource(Res.string.support_email)
    val subject = stringResource(Res.string.set_mail_subject)
    val copiedText = stringResource(Res.string.set_address_copied)
    var failed by remember { mutableStateOf(startFailed) }
    var copied by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copied) {
        if (copied != null) {
            delay(2_500)
            copied = null
        }
    }
    fun copy() {
        // 클립보드에 넣은 뒤에만 알린다(안드로이드와 같다)
        if (runCatching { clipboard.setText(androidx.compose.ui.text.AnnotatedString(address)) }.isSuccess) copied = copiedText
    }
    BlueSheet(
        title = stringResource(if (failed) Res.string.set_mail_open_failed else Res.string.set_contact_mail),
        onDismiss = onDismiss, modifier = Modifier.testTag(if (failed) "contact-failed-sheet" else "contact-sheet"),
        centered = true,
        actions = {
            if (failed) {
                RunButton(stringResource(Res.string.set_copy_mail_address), ::copy, Modifier.testTag("contact-copy"))
                RunButton(stringResource(Res.string.set_close), onDismiss, kind = RunButtonKind.Secondary)
            } else {
                RunButton(
                    stringResource(Res.string.set_open_mail_app),
                    onClick = { if (!openMailApp(address, subject)) failed = true },
                    modifier = Modifier.testTag("contact-open-mail"),
                )
                RunButton(stringResource(Res.string.set_copy_address), ::copy, Modifier.testTag("contact-copy"), kind = RunButtonKind.Secondary)
            }
            BlueToast(copied, Modifier.padding(top = 2.dp))
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Email, contentDescription = null, tint = t.cyan, modifier = Modifier.size(34.dp))
            Text(address, style = runTextStyle(20.sp, t.text, FontWeight.SemiBold), modifier = Modifier.testTag("contact-address"))
        }
        Text(stringResource(if (failed) Res.string.set_mail_open_failed_body else Res.string.set_contact_body),
            style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.5f), textAlign = TextAlign.Center)
    }
}

/** 메일 앱의 작성 창 — 받는 사람과 제목만 채운다. 열 곳이 없으면 false(공용 ExternalIntents.composeEmail) */
private fun openMailApp(address: String, subject: String): Boolean =
    com.stepup.android.core.ExternalIntents.composeEmail(address, subject)
