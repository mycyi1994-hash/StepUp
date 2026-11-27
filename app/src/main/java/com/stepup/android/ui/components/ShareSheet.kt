package com.stepup.android.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import com.stepup.android.R
import com.stepup.android.ui.experience.feedbackClickable
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 13차 공유(자랑하기) 시트 — 모든 공유가 같은 시트(S-403 ~ S-420): 러닝 완료 · 지난 러닝 · 순위 · 새 신발 · 10회 결과 · 크루 초대 · 번개러닝 · 초대 코드.
 *
 * 위에 보낼 카드 미리보기(9:16 — 보내는 그림 그대로), 카드가 여럿이면 고르기 칩, [link] 가 있으면 주소 + 「복사」, 아래 보낼 곳 한 줄.
 * 보낼 곳: 인스타그램 · 카카오톡 · X(폰에 깔린 앱만 — 아이콘은 그 앱이 가진 공식 아이콘을 폰에서 읽는다) · 이미지 저장 · 더보기(기기 공유 창).
 * 인스타 스토리 바로 넘기기 · 카카오 공유 카드는 앱 ID · 키가 있어야 해(13차 문서 "문제 · 제외") — 지금은 그 앱에 그림을 넘기고 앱이 스토리 · 대화를 고르게 한다.
 */
data class ShareCardOption(
    val key: String,
    val label: String,
    val render: suspend (Context) -> Bitmap?,
)

/** 보낼 앱 — 패키지 · 화면에 쓸 이름 */
private enum class ShareApp(val pkg: String, val label: Int, val tag: String) {
    INSTAGRAM("com.instagram.android", R.string.share_to_instagram, "share-to-instagram"),
    KAKAO("com.kakao.talk", R.string.share_to_kakao, "share-to-kakao"),
    X("com.twitter.android", R.string.share_to_x, "share-to-x"),
}

@Composable
fun StepUpShareSheet(
    options: List<ShareCardOption>,
    text: String,
    fileName: String,
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.share_sheet_title),
    link: String? = null,
) {
    if (options.isEmpty()) return
    val context = LocalContext.current
    var pick by rememberSaveable { mutableStateOf(options.first().key) }
    val option = options.firstOrNull { it.key == pick } ?: options.first()
    var card by remember { mutableStateOf<Bitmap?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(option.key) {
        card = null
        failed = false
        val shot = withContext(Dispatchers.Default) { runCatching { option.render(context) }.getOrNull() }
        card = shot
        failed = shot == null
    }
    val preview: ImageBitmap? = remember(card) { card?.asImageBitmap() }
    val scope = rememberCoroutineScope()
    val apps = remember { ShareApp.entries.mapNotNull { app -> appIcon(context, app.pkg)?.let { app to it } } }
    val save = rememberImageSave({ card }, fileName)
    val copiedText = stringResource(R.string.share_link_copied)
    val c = StepUpUi.colors
    RunSheet(onDismiss = onDismiss, modifier = Modifier.testTag("share-sheet")) {
        Text(title, color = c.label, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(16.dp))
        // 미리보기 — 보내는 그림 그대로(9:16)
        Box(
            Modifier.align(Alignment.CenterHorizontally).width(196.dp).aspectRatio(9f / 16f).clip(RoundedCornerShape(16.dp))
                .background(c.fill).testTag("share-preview"),
            contentAlignment = Alignment.Center,
        ) {
            when {
                preview != null -> Image(preview, contentDescription = option.label, contentScale = ContentScale.Fit)
                failed -> Text(stringResource(R.string.share_card_failed), color = c.secondaryLabel, fontSize = 13.sp)
                else -> CircularProgressIndicator(Modifier.size(28.dp), color = c.primary, strokeWidth = 3.dp)
            }
        }
        if (options.size > 1) {
            Spacer(Modifier.height(14.dp))
            Row(Modifier.align(Alignment.CenterHorizontally), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { o ->
                    val on = o.key == option.key
                    Text(
                        o.label, color = if (on) androidx.compose.ui.graphics.Color.White else c.secondaryLabel,
                        fontSize = 15.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                        modifier = Modifier.clip(CircleShape).background(if (on) c.primary else c.fill)
                            .feedbackClickable(role = Role.Tab) { pick = o.key }
                            .semantics { selected = on }
                            .padding(horizontal = 18.dp, vertical = 9.dp).testTag("share-card-${o.key}"),
                    )
                }
            }
        }
        if (link != null) {
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.fill).padding(start = 16.dp, end = 6.dp)
                    .heightIn(min = 48.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(link, color = c.secondaryLabel, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).testTag("share-link"))
                Text(
                    stringResource(R.string.share_link_copy), color = c.accentText, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).feedbackClickable(role = Role.Button) {
                        copyLink(context, link)
                        Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                    }.padding(horizontal = 12.dp, vertical = 10.dp).testTag("share-link-copy"),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        // 보낼 곳
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            apps.forEach { (app, icon) ->
                Target(stringResource(app.label), app.tag, enabled = card != null, icon = { AppIcon(icon) }) {
                    card?.let { shot -> scope.launch { send(context, shot, shareText(text, link), fileName, app.pkg) } }
                }
            }
            Target(stringResource(R.string.share_to_save), "share-to-save", enabled = card != null, icon = {
                Icon(Icons.Filled.Download, null, tint = c.label, modifier = Modifier.size(26.dp))
            }, onClick = save)
            Target(stringResource(R.string.share_to_more), "share-to-more", enabled = true, icon = {
                Icon(Icons.Filled.MoreHoriz, null, tint = c.label, modifier = Modifier.size(26.dp))
            }) {
                val shot = card
                if (shot == null) RunShareCard.shareText(context, shareText(text, link))
                else scope.launch { send(context, shot, shareText(text, link), fileName, null) }
            }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Target(label: String, tag: String, enabled: Boolean, icon: @Composable () -> Unit, onClick: () -> Unit) {
    val c = StepUpUi.colors
    Column(
        Modifier.width(68.dp).clip(RoundedCornerShape(12.dp))
            .feedbackClickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .padding(vertical = 6.dp).testTag(tag),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(c.fill), contentAlignment = Alignment.Center) { icon() }
        Spacer(Modifier.height(6.dp))
        Text(label, color = c.label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppIcon(icon: ImageBitmap) {
    Image(icon, contentDescription = null, modifier = Modifier.size(56.dp))
}

/** 폰에 깔린 앱의 아이콘 — 없으면 null(그 보낼 곳을 보이지 않는다) */
private fun appIcon(context: Context, pkg: String): ImageBitmap? = runCatching {
    val d: Drawable = context.packageManager.getApplicationIcon(pkg)
    d.toBitmap(144, 144).asImageBitmap()
}.getOrNull()

private fun shareText(text: String, link: String?): String = if (link.isNullOrBlank() || text.contains(link)) text else "$text\n$link"

private fun copyLink(context: Context, link: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("StepUp", link))
}

/** 그림을 캐시에 저장하고 [pkg] 앱(없으면 기기 공유 창)으로 보낸다 */
private suspend fun send(context: Context, bitmap: Bitmap, text: String, fileName: String, pkg: String?) {
    val file = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        File(dir, "$fileName.png").also { out -> out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) } }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, text)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    try {
        if (pkg != null) {
            context.startActivity(send.setPackage(pkg))
        } else {
            context.startActivity(Intent.createChooser(send, null))
        }
    } catch (_: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(send.setPackage(null), null))
    }
}

/**
 * 그림을 사진(Pictures/StepUp)에 저장한다 — 미리보기와 같은 그림. Android 9 이하는 저장 권한을 먼저 묻는다(10 부터는 권한 없이).
 * 저장했는지 짧게 알린다.
 */
@Composable
fun rememberImageSave(card: () -> Bitmap?, name: String): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedText = stringResource(R.string.result_saved)
    val failedText = stringResource(R.string.result_save_failed)
    val save: () -> Unit = {
        val bitmap = card()
        scope.launch {
            val saved = bitmap != null && withContext(Dispatchers.IO) {
                runCatching { com.stepup.android.ui.screens.walk.RunImageStore.save(context, bitmap, name) }.getOrDefault(false)
            }
            Toast.makeText(context, if (saved) savedText else failedText, Toast.LENGTH_SHORT).show()
        }
    }
    val permission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) save() else Toast.makeText(context, failedText, Toast.LENGTH_SHORT).show()
    }
    return {
        if (com.stepup.android.ui.screens.walk.RunImageStore.needsPermission(context)) {
            permission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            save()
        }
    }
}
