package com.stepup.android.ui.screens.settings

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.ui.components.DetailPage
import com.stepup.android.ui.components.SettingsGroupLabel
import com.stepup.android.ui.components.SettingsNavRow
import com.stepup.android.ui.components.SettingsPrimaryButton
import com.stepup.android.ui.components.SettingsSecondaryButton
import com.stepup.android.ui.components.SettingsSheet
import com.stepup.android.ui.components.SettingsToast
import com.stepup.android.ui.components.settingsPalette
import com.stepup.android.ui.experience.feedbackClickable
import kotlinx.coroutines.delay

/**
 * 자주 묻는 질문 — 질문 · 답변 문자열 쌍(docs/redesign/settings-v1 의 FAQ 답변안). 적립률 · 출금 조건 · 확률을 새로
 * 약속하지 않는다. 뽑기는 무료 정책(0042) 그대로다.
 */
private val faqEntries = listOf(
    R.string.set_faq_q_earn to R.string.set_faq_a_earn,
    R.string.set_faq_q_stopped to R.string.set_faq_a_stopped,
    R.string.set_faq_q_grade to R.string.set_faq_a_grade,
    R.string.set_faq_q_withdraw to R.string.set_faq_a_withdraw,
    R.string.set_faq_q_draw to R.string.set_faq_a_draw,
)

/**
 * 도움말 · 문의(설정 v1 21~23 · 28). 질문은 하나씩 펼친다. 앱 사용 안내는 한 장짜리 다시 보기(시작·로그인·첫 사용 v1 시안 20)를 연다.
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
    DetailPage(title = stringResource(R.string.set_support), onBack = onBack) {
        item {
            Column(Modifier.fillMaxWidth()) {
                SettingsGroupLabel(stringResource(R.string.set_faq))
                faqEntries.forEachIndexed { index, (question, answer) ->
                    FaqRow(stringResource(question), stringResource(answer), expanded == index, { onToggle(index) },
                        Modifier.testTag("faq-$index"))
                }
            }
        }
        item {
            SettingsNavRow(
                stringResource(R.string.set_guide), onClick = onOpenGuide, description = stringResource(R.string.onb_guide_row_desc),
                modifier = Modifier.padding(top = 8.dp).testTag("support-guide"),
            )
        }
        item {
            SettingsPrimaryButton(
                stringResource(R.string.set_contact_mail), onOpenContact,
                Modifier.fillMaxWidth().padding(top = 18.dp).testTag("support-contact"),
            )
        }
    }
    if (contactOpen) ContactSheet(onDismiss = onCloseContact, startFailed = contactFailed)
}

/** 질문 한 줄 — 누르면 그 자리에서 답을 펼친다(+ / −). 줄 전체가 하나의 누름 대상이다 */
@Composable
private fun FaqRow(question: String, answer: String, open: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val p = settingsPalette()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 58.dp)
                .feedbackClickable(role = Role.Button, onClick = onToggle)
                // 읽기 도구가 펼침 · 접힘을 알린다
                .semantics { if (open) collapse { onToggle(); true } else expand { onToggle(); true } }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(question, color = p.text, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 1.35.em,
                modifier = Modifier.weight(1f))
            Icon(if (open) Icons.Filled.Remove else Icons.Filled.Add, contentDescription = null, tint = p.secondary,
                modifier = Modifier.size(22.dp))
        }
        if (open) {
            Text(answer, color = p.secondary, fontSize = 14.sp, lineHeight = 1.6.em,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(p.divider))
    }
}

/**
 * 메일로 문의(23) · 메일 앱을 열지 못함(28). 메일 앱의 작성 창만 연다 — 여는 것은 문의 접수가 아니다.
 * 메일 앱이 없으면 주소 복사로 잇는다. 복사는 실제로 된 뒤에만 알린다.
 */
@Composable
internal fun ContactSheet(onDismiss: () -> Unit, startFailed: Boolean = false) {
    val p = settingsPalette()
    val context = LocalContext.current
    val address = stringResource(R.string.support_email)
    val subject = stringResource(R.string.set_mail_subject)
    val copiedText = stringResource(R.string.set_address_copied)
    var failed by remember { mutableStateOf(startFailed) }
    var copied by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(copied) {
        if (copied != null) {
            delay(2_500)
            copied = null
        }
    }
    fun copy() {
        if (copyToClipboard(context, address)) copied = copiedText
    }
    SettingsSheet(
        title = stringResource(if (failed) R.string.set_mail_open_failed else R.string.set_contact_mail),
        onDismiss = onDismiss, modifier = Modifier.testTag(if (failed) "contact-failed-sheet" else "contact-sheet"),
        actions = {
            if (failed) {
                SettingsPrimaryButton(stringResource(R.string.set_copy_mail_address), ::copy, Modifier.fillMaxWidth().testTag("contact-copy"))
                SettingsSecondaryButton(stringResource(R.string.set_close), onDismiss, Modifier.fillMaxWidth())
            } else {
                SettingsPrimaryButton(
                    stringResource(R.string.set_open_mail_app),
                    onClick = { if (!openMailApp(context, address, subject)) failed = true },
                    modifier = Modifier.fillMaxWidth().testTag("contact-open-mail"),
                )
                SettingsSecondaryButton(stringResource(R.string.set_copy_address), ::copy, Modifier.fillMaxWidth().testTag("contact-copy"))
            }
            SettingsToast(copied, Modifier.padding(top = 4.dp))
        },
    ) {
        Icon(Icons.Outlined.Email, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
        Text(address, color = p.text, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("contact-address"))
        Text(stringResource(if (failed) R.string.set_mail_open_failed_body else R.string.set_contact_body),
            color = p.secondary, fontSize = 14.sp, lineHeight = 1.5.em)
    }
}

/** 메일 앱의 작성 창 — 받는 사람과 제목만 채운다. 열 곳이 없으면 false */
private fun openMailApp(context: Context, address: String, subject: String): Boolean {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        putExtra(Intent.EXTRA_SUBJECT, subject)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    return try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}

private fun copyToClipboard(context: Context, text: String): Boolean = try {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    if (clipboard == null) false else {
        clipboard.setPrimaryClip(ClipData.newPlainText("StepUp", text))
        true
    }
} catch (_: Exception) {
    false
}
