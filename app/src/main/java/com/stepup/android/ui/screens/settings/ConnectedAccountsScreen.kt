package com.stepup.android.ui.screens.settings

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.stepup.android.R
import com.stepup.android.core.ServiceLocator
import com.stepup.android.data.remote.AccountDeletion
import com.stepup.android.data.remote.ServerResult
import com.stepup.android.ui.components.BlueGroup
import com.stepup.android.ui.components.BlueGroupLabel
import com.stepup.android.ui.components.BlueNavRow
import com.stepup.android.ui.components.BlueNote
import com.stepup.android.ui.components.BluePage
import com.stepup.android.ui.components.BlueSheet
import com.stepup.android.ui.components.BlueSheetDivider
import com.stepup.android.ui.components.BlueStatusRow
import com.stepup.android.ui.components.RunButton
import com.stepup.android.ui.components.RunButtonKind
import com.stepup.android.ui.components.runTextStyle
import com.stepup.android.ui.components.runTone
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** WEB3 지갑 줄 — 실제 연결 여부(서버 draw_status 의 wallet_linked)만 보인다 */
enum class WalletLink { Checking, Linked, NotLinked, Unknown, SignedOut }

/** 계정 삭제 — 확인 · 처리 중 · 지우지 않음(확실) · 결과 모름 */
enum class DeleteStep { Closed, Confirm, Deleting, Failed, Unknown }

/**
 * 연결된 계정(설정 v1 11~15 → 파란 톤 v4 SET11~15 · 31~34).
 *
 * 로그인은 이 휴대폰의 로그인 상태(sessionHolder)만, WEB3 지갑은 서버가 아는 실제 연결만 보인다 — 연결 전이면 "연결하기"로
 * 뽑기 화면과 같은 웹 지갑 페이지를 연다. Health Connect 는 아직 없어 "준비 중". 탭 한 번으로 연결된 것처럼 꾸미지 않는다.
 *
 * 계정 삭제는 기존 서버 동작(account_delete) 그대로다. 서버가 지웠다고 답한 뒤에만 이 휴대폰의 로그인과 사본을 지우고
 * 로그인 전 첫 화면으로 간다. 지우지 않은 것이 확실하면(보내지 못함 · 거절) 확인 창에 다시 시도할 수 있게 알리고,
 * 보냈는데 응답을 못 받았으면 결과를 단정하지 않는다 — 자동으로 다시 보내지 않고 문의 길을 준다.
 */
@Composable
fun ConnectedAccountsScreen(onBack: () -> Unit = {}, onConnectWallet: () -> Unit = {}) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var reads by remember { mutableIntStateOf(0) }
    var signedIn by remember { mutableStateOf<Boolean?>(null) }
    var wallet by remember { mutableStateOf(WalletLink.Checking) }
    LaunchedEffect(reads) {
        val now = ServiceLocator.sessionHolder.isSignedIn()
        signedIn = now
        wallet = when {
            !now -> WalletLink.SignedOut
            else -> when (val status = runCatching { ServiceLocator.economyApi.drawStatus() }.getOrNull()) {
                is ServerResult.Ok -> if (status.value.walletLinked) WalletLink.Linked else WalletLink.NotLinked
                // 읽지 못했다 — 연결 안 됨으로 단정하지 않는다
                else -> WalletLink.Unknown
            }
        }
    }
    var step by remember { mutableStateOf(DeleteStep.Closed) }
    var contactOpen by remember { mutableStateOf(false) }

    fun delete() {
        if (step == DeleteStep.Deleting) return
        // 인터넷이 없으면 보내지 않는다 — 처리되지 않은 것이 확실하다
        if (!hasNetwork(context)) {
            step = DeleteStep.Failed
            return
        }
        step = DeleteStep.Deleting
        scope.launch {
            val outcome = try {
                ServiceLocator.server.deleteAccount()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                AccountDeletion.Unknown
            }
            when (outcome) {
                AccountDeletion.Deleted -> {
                    // 로그인 표시를 지우는 순간 첫 화면이 로그인으로 바뀌며 이 화면이 닫히고, 이 코루틴도
                    // 함께 취소된다. 그 뒤에 지우면 앞 계정의 잔고 · 신발 · 키 · 몸무게가 남을 수 있다 —
                    // 폰에 남은 것을 취소되지 않게 먼저 다 지우고, 로그인 표시는 맨 마지막에 지운다.
                    withContext(NonCancellable) {
                        // 달리는 중이면 먼저 끝낸다 — 로그인 화면에는 러닝을 멈출 곳이 없다
                        if (com.stepup.android.service.WalkSessionService.state.value.isActive) {
                            com.stepup.android.service.WalkSessionService.stop(context)
                        }
                        // 서버 계정이 사라졌다
                        ServiceLocator.sessionHolder.signOut()
                        // 지운 계정의 잔고 · 신발 사본도 이 폰에서 지운다
                        ServiceLocator.economySync.clearLocal()
                        // 쓰다 둔 동네 이야기와 숨긴 글 목록도 지운다
                        ServiceLocator.userPrefs.clearStoryData()
                        // 쓰다 둔 크루 만들기 초안(사진 파일 포함) · 크루 채팅의 쓰다 만 공지도 지운다
                        ServiceLocator.userPrefs.clearCrewData()
                        runCatching { java.io.File(context.filesDir, "crew_drafts").deleteRecursively() }
                        // 이 폰에 들고 있던 크루 채팅 대화 · 받은 사진도 지운다
                        runCatching { ServiceLocator.crewChat.clearAll() }
                        // 첫 설정에서 적은 키 · 몸무게 · 목표도 이 폰에서 지운다(서버에는 애초에 없다)
                        ServiceLocator.userPrefs.setBodyProfile(com.stepup.android.domain.BodyProfile())
                        ServiceLocator.userPrefs.clearGoalAttempts()
                        ServiceLocator.userPrefs.setRunExperience(null)
                        // 이 폰의 로그인도 지운다 — 첫 화면(로그인)으로 돌아간다
                        ServiceLocator.userPrefs.setLoginMethod("")
                    }
                    step = DeleteStep.Closed
                }
                AccountDeletion.NotDeleted -> step = DeleteStep.Failed
                AccountDeletion.Unknown -> step = DeleteStep.Unknown
            }
        }
    }

    ConnectedAccountsContent(
        signedIn = signedIn, wallet = wallet, step = step, contactOpen = contactOpen,
        onBack = onBack, onConnectWallet = onConnectWallet,
        onAskDelete = { step = DeleteStep.Confirm }, onDelete = ::delete,
        onCloseDelete = {
            if (step != DeleteStep.Deleting) {
                val wasUnknown = step == DeleteStep.Unknown
                step = DeleteStep.Closed
                // 결과를 모른 채 닫으면 계정 상태를 다시 읽는다(다시 지우지는 않는다)
                if (wasUnknown) reads++
            }
        },
        onOpenContact = { contactOpen = true }, onCloseContact = { contactOpen = false },
    )
}

@Composable
fun ConnectedAccountsContent(
    signedIn: Boolean?,
    wallet: WalletLink,
    step: DeleteStep = DeleteStep.Closed,
    contactOpen: Boolean = false,
    onBack: () -> Unit = {},
    onConnectWallet: () -> Unit = {},
    onAskDelete: () -> Unit = {},
    onDelete: () -> Unit = {},
    onCloseDelete: () -> Unit = {},
    onOpenContact: () -> Unit = {},
    onCloseContact: () -> Unit = {},
) {
    BluePage(title = stringResource(R.string.set_connected), onBack = onBack, subtitle = stringResource(R.string.set_connected_intro)) {
        item {
            BlueGroupLabel(stringResource(R.string.set_group_login))
            // 소셜 계정은 읽기 전용 — 이 휴대폰의 로그인 상태만
            BlueGroup {
                BlueStatusRow(
                    stringResource(R.string.set_social_account), null,
                    stringResource(
                        when (signedIn) {
                            null -> R.string.set_checking
                            true -> R.string.set_signed_in
                            false -> R.string.set_not_connected
                        },
                    ),
                    Modifier.testTag("connected-social"), strong = false,
                )
            }
        }
        item {
            BlueGroupLabel(stringResource(R.string.set_group_external))
            BlueGroup {
                when (wallet) {
                    // SET33 · SET34 — 연결 전이거나 서버에서 읽지 못했으면 웹 지갑 페이지에서 확인 · 연결한다(탭만으로 연결됨을 보이지 않는다)
                    WalletLink.NotLinked, WalletLink.Unknown -> BlueNavRow(
                        stringResource(R.string.set_web3_wallet), onClick = onConnectWallet,
                        value = stringResource(if (wallet == WalletLink.NotLinked) R.string.set_wallet_connect else R.string.set_wallet_unknown),
                        valueAccent = wallet == WalletLink.NotLinked,
                        modifier = Modifier.testTag("connected-wallet"),
                    )
                    else -> BlueStatusRow(
                        stringResource(R.string.set_web3_wallet), null,
                        stringResource(
                            when (wallet) {
                                WalletLink.Linked -> R.string.set_wallet_linked
                                WalletLink.Checking -> R.string.set_checking
                                else -> R.string.set_not_connected
                            },
                        ),
                        Modifier.testTag("connected-wallet"), strong = wallet == WalletLink.Linked,
                    )
                }
                // 준비 중 — 가짜 연결 기능을 만들지 않는다
                BlueStatusRow(
                    stringResource(R.string.set_health_connect), null, stringResource(R.string.set_coming_soon),
                    Modifier.testTag("connected-health"), strong = false,
                )
            }
        }
        item { BlueNote(stringResource(R.string.set_external_note), Modifier.padding(top = 2.dp)) }
        // 로그인이 확인된 사람에게만 — 확인 중이거나 로그인 전이면 숨긴다
        if (signedIn == true) {
            item {
                Column(Modifier.fillMaxWidth().padding(top = 18.dp)) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(runTone().divider))
                    BlueNavRow(
                        stringResource(R.string.set_delete_account), onClick = onAskDelete, danger = true, plain = true,
                        modifier = Modifier.padding(top = 4.dp).testTag("connected-delete"),
                    )
                }
            }
        }
    }

    when (step) {
        DeleteStep.Confirm, DeleteStep.Deleting, DeleteStep.Failed -> DeleteAccountSheet(step, onDelete, onCloseDelete)
        DeleteStep.Unknown -> DeleteUnknownSheet(onContact = onOpenContact, onClose = onCloseDelete)
        DeleteStep.Closed -> Unit
    }
    if (contactOpen) ContactSheet(onDismiss = onCloseContact)
}

/**
 * 계정 삭제 확인(SET13) · 처리 중(SET14) · 확정 실패(SET31 r2). 서버에서 지워지는 것과 이 휴대폰에 남는 것을 나눠 알린다.
 * 실패해도 삭제 범위 · 크루장 이전 · 되돌릴 수 없음 · 휴대폰 기록 안내를 그대로 두고 아래에 실패 안내를 더한다.
 */
@Composable
private fun DeleteAccountSheet(step: DeleteStep, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val t = runTone()
    val deleting = step == DeleteStep.Deleting
    BlueSheet(
        title = stringResource(R.string.set_delete_title), onDismiss = onDismiss,
        dismissible = !deleting, showClose = !deleting, modifier = Modifier.testTag("delete-sheet"),
        actions = {
            if (deleting) {
                // 타이머로 끝내지 않는다 — 서버의 확정 답(지움 · 지우지 않음 · 모름)을 기다린다
                RunButton(stringResource(R.string.set_deleting), {}, Modifier.testTag("delete-busy"), busy = true)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RunButton(stringResource(R.string.set_cancel), onDismiss, Modifier.weight(1f), kind = RunButtonKind.Secondary)
                    RunButton(stringResource(R.string.set_delete_action), onDelete, Modifier.weight(1f).testTag("delete-confirm"),
                        kind = RunButtonKind.Danger)
                }
            }
        },
    ) {
        Text(stringResource(R.string.set_delete_heading), style = runTextStyle(18.sp, t.label, FontWeight.Bold))
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            listOf(R.string.set_delete_item_runs, R.string.set_delete_item_sup, R.string.set_delete_item_places).forEach {
                Text(stringResource(it), style = runTextStyle(17.sp, t.text, FontWeight.Medium))
            }
        }
        Text(stringResource(R.string.set_delete_crew_leader), style = runTextStyle(15.sp, t.label, FontWeight.Medium, 1.45f))
        BlueSheetDivider()
        Text(stringResource(R.string.set_delete_cannot_undo), style = runTextStyle(17.sp, t.dangerText, FontWeight.Bold))
        Text(stringResource(R.string.set_delete_phone_note), style = runTextStyle(15.sp, t.text.copy(alpha = 0.9f), FontWeight.Medium, 1.45f))
        if (step == DeleteStep.Failed) {
            Text(
                stringResource(R.string.set_delete_failed), style = runTextStyle(16.sp, t.dangerText, FontWeight.Bold, 1.45f),
                modifier = Modifier.padding(top = 6.dp).semantics { liveRegion = LiveRegionMode.Polite }.testTag("delete-failed"),
            )
        }
    }
}

/** 삭제 결과 미확인(SET15) — 완료나 실패로 단정하지 않고, 같은 요청을 자동으로 다시 보내지 않는다 */
@Composable
private fun DeleteUnknownSheet(onContact: () -> Unit, onClose: () -> Unit) {
    val t = runTone()
    BlueSheet(
        title = stringResource(R.string.set_delete_unknown_title), onDismiss = onClose, modifier = Modifier.testTag("delete-unknown"),
        centered = true,
        actions = {
            RunButton(stringResource(R.string.set_contact_options), onContact, Modifier.testTag("delete-unknown-contact"))
            RunButton(stringResource(R.string.set_close), onClose, kind = RunButtonKind.Secondary)
        },
    ) {
        Text(stringResource(R.string.set_delete_unknown_heading), style = runTextStyle(16.sp, t.label, FontWeight.SemiBold),
            textAlign = TextAlign.Center)
        Text(stringResource(R.string.set_delete_unknown_body), style = runTextStyle(16.sp, t.label, FontWeight.Medium, 1.55f),
            textAlign = TextAlign.Center)
    }
}

private fun hasNetwork(context: Context): Boolean {
    val manager = context.getSystemService(ConnectivityManager::class.java) ?: return true
    val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
