package com.stepup.android.ui.screens.items

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.domain.Sneaker
import kotlinx.coroutines.launch

/**
 * 신발 이름 바꾸기(2026-10-17) — 이 폰에서만 바뀐다(설정 저장소, domain/ShoeNickname). 비우고 저장하거나 "원래 이름으로"를
 * 누르면 모델 이름으로 돌아간다. 거래소 · 다른 사람에게는 늘 원래 이름이다.
 */
@Composable
internal fun ShoeRenameSheet(shoe: Sneaker, onDone: () -> Unit) {
    val t = com.stepup.android.ui.components.runTone()
    val nicknames = com.stepup.android.ui.components.LocalShoeNicknames.current
    val current = nicknames[shoe.id]
    // 원래 이름 — 내가 붙인 이름을 빼고 읽는다
    val original = shoe.modelShoeName()
    var draft by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf(current ?: original) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    fun save(name: String?) {
        scope.launch {
            com.stepup.android.core.ServiceLocator.userPrefs.setShoeNickname(shoe.id, name?.takeIf { it.trim() != original })
            onDone()
        }
    }
    com.stepup.android.ui.components.RunSheet(onDismiss = onDone, modifier = Modifier.testTag("shoe-rename-sheet")) {
        com.stepup.android.ui.components.RunSheetText(
            stringResource(R.string.shoe_rename_title),
            body = stringResource(R.string.shoe_rename_body, original),
        )
        Spacer(Modifier.height(16.dp))
        com.stepup.android.ui.components.RunTextField(
            value = draft,
            onValueChange = { next ->
                // 글자 수 상한까지만 받는다(이모지 등은 한 글자로 센다)
                draft = if (next.codePointCount(0, next.length) <= com.stepup.android.domain.ShoeNickname.MAX_LENGTH) next
                    else String(next.codePoints().limit(com.stepup.android.domain.ShoeNickname.MAX_LENGTH.toLong()).toArray(), 0,
                        com.stepup.android.domain.ShoeNickname.MAX_LENGTH)
            },
            placeholder = original,
            clearLabel = stringResource(R.string.run_course_clear_text),
            onImeAction = { save(draft) },
            fieldTag = "shoe-rename-field",
        )
        Text(
            stringResource(R.string.shoe_rename_count, draft.codePointCount(0, draft.length), com.stepup.android.domain.ShoeNickname.MAX_LENGTH),
            color = t.label, fontSize = 13.sp, textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        Spacer(Modifier.height(16.dp))
        com.stepup.android.ui.components.RunButton(
            stringResource(R.string.shoe_rename_save), { save(draft) },
            modifier = Modifier.fillMaxWidth().testTag("shoe-rename-save"),
        )
        if (current != null) {
            Spacer(Modifier.height(10.dp))
            com.stepup.android.ui.components.RunButton(
                stringResource(R.string.shoe_rename_reset), { save(null) },
                kind = com.stepup.android.ui.components.RunButtonKind.Secondary,
                modifier = Modifier.fillMaxWidth().testTag("shoe-rename-reset"),
            )
        }
    }
}
