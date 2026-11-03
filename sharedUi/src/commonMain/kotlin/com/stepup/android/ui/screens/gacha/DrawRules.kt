package com.stepup.android.ui.screens.gacha

import com.stepup.android.data.repo.EconomyOutcome
import com.stepup.android.domain.DrawKind
import com.stepup.android.domain.DrawStatus
import com.stepup.android.domain.Sneaker

/*
 * 신발 뽑기 — 규칙은 v2 전달본(2026-09-28, docs/redesign/shoe-draw-v2), 화면은 26장 디자인(docs/redesign/shoe-draw-v3). 화면이 서버 값에서 고르는 규칙.
 *
 * 모든 수 · 연결 상태는 서버(draw_status)가 준 것이다. 여기서는 그 값으로 "어느 장면인가"만 고른다 —
 * 폰이 기회를 주거나 줄이거나, 결과 신발을 정하지 않는다. 앱이 알 수 없는 상태(지갑에서 연결 중 · 연결 실패)는 만들지 않는다.
 */

/** 상급 탭의 모습 — 회원 상태별 행동(v2 전달서 "메인 두 칸") 중 서버 값으로 가를 수 있는 것 */
enum class PremiumMode {
    /** 01 — 지갑을 연결한 적이 없다(첫 연결 선물이 남아 있다). "지갑 연결하고 N회 받기" */
    Connect,

    /** 24 — 연결한 적은 있는데 지금은 연결돼 있지 않다. 받은 기회는 그대로, "지갑 다시 연결하기"(선물은 다시 주지 않는다) */
    Reconnect,

    /** 연결됨 · 체인 작업이 잠시 멈춤 — 상급을 뽑을 수 없다(서버 chain_paused) */
    Paused,

    /** 02 — 연결됨 · 기회 있음. "상급으로 1회 뽑기" */
    Ready,

    /** 15 — 연결됨 · 기회 없음. "러닝하고 기회 받기" */
    Empty,
}

/**
 * 서버 값으로 상급 칸을 고른다. `gift_on_link` 는 지갑을 연결한 적이 없을 때만 0 보다 크다(0042 draw_status —
 * wallet_history 가 있으면 0). 그래서 "연결 안 됨 + 선물 0"은 연결했다가 해제된 기존 회원이다.
 */
fun DrawStatus.premiumMode(): PremiumMode = when {
    !walletLinked && giftOnLink > 0 -> PremiumMode.Connect
    !walletLinked -> PremiumMode.Reconnect
    chainPaused -> PremiumMode.Paused
    premiumLeft > 0 -> PremiumMode.Ready
    else -> PremiumMode.Empty
}

/**
 * 결과를 아직 모르는 뽑기 요청 — 보내기 **전에** 적어 두고, 결과를 확인하면 지운다. 앱이 죽어도 남아 다시 열면 확인한다(26).
 *
 * @param newestBefore 보내기 직전 서버에 있던 내 신발 중 가장 큰 번호. 이보다 큰 번호의 그 종류 신발이 생겼으면 이 요청의 결과다.
 * @param leftBefore 보내기 직전 서버가 준 그 종류의 남은 수
 * @param shoeId 서버가 새 신발 번호를 돌려줬다(뽑기는 확정) — 목록만 아직 받지 못했다
 */
data class PendingDraw(
    val kind: DrawKind,
    val newestBefore: Long,
    val leftBefore: Int,
    val startedAt: Long,
    val account: String?,
    val shoeId: Long? = null,
)

/** 서버 신발의 출처(0023 draw_create) — 무료는 FREE_DRAW, 상급(예전 지갑 보너스 뽑기 포함)은 BONUS_DRAW */
fun DrawKind.origin(): String = if (this == DrawKind.FREE) "FREE_DRAW" else "BONUS_DRAW"

/** 결과를 모르는 요청을 확인한 결말 */
sealed interface DrawCheck {
    /** 뽑혔다 — 서버 목록에 새로 생긴 그 신발 */
    data class Found(val shoe: Sneaker) : DrawCheck

    /** 뽑기가 시작되지 않았다 — 새 신발이 없고 그 종류의 수도 줄지 않았다(기회를 쓰지 않았다, 19) */
    data object NotStarted : DrawCheck

    /** 아직 모른다 — 다시 읽지 못했거나, 수는 줄었는데 신발이 보이지 않는다(20 에 머문다, 새 뽑기는 막는다) */
    data object Unknown : DrawCheck
}

/**
 * 결과를 모르는 요청을 서버 값으로 가른다. 새 신발이 보이면 그것이 결과다. 서버가 번호를 이미 돌려줬는데 목록에 없으면
 * 뽑기는 된 것이라 "시작 안 됨"이라 하지 않는다. 수를 다시 읽지 못했거나 수만 줄었으면 단정하지 않는다.
 */
fun judgePending(pending: PendingDraw, found: Sneaker?, status: DrawStatus?): DrawCheck = when {
    found != null -> DrawCheck.Found(found)
    pending.shoeId != null -> DrawCheck.Unknown
    status == null -> DrawCheck.Unknown
    status.left(pending.kind) < pending.leftBefore -> DrawCheck.Unknown
    else -> DrawCheck.NotStarted
}

/** 뽑기를 시작하지 못한 까닭(19) — 서버가 거절했거나(기회를 쓰지 않았다) 보내지 못했다 */
enum class DrawStop { Network, NoFree, NoPremium, Wallet, MintLimit, ChainPaused, SignIn, Other }

/** 서버가 거절한 결말 → 19 의 까닭 */
fun EconomyOutcome.toDrawStop(): DrawStop = when (this) {
    EconomyOutcome.NoFreeDraws -> DrawStop.NoFree
    EconomyOutcome.NoPremiumDraws -> DrawStop.NoPremium
    EconomyOutcome.WalletRequired -> DrawStop.Wallet
    EconomyOutcome.MintLimitReached -> DrawStop.MintLimit
    EconomyOutcome.ChainPaused -> DrawStop.ChainPaused
    EconomyOutcome.SignInRequired -> DrawStop.SignIn
    EconomyOutcome.Offline -> DrawStop.Network
    else -> DrawStop.Other
}

/** 다시 눌러 볼 만한가 — 남은 기회 없음 · 지갑 필요 · 발행 한도 · 로그인은 다시 눌러도 같다 */
val DrawStop.retryable: Boolean
    get() = this == DrawStop.Network || this == DrawStop.Other || this == DrawStop.ChainPaused

/**
 * 받은 신발 한 켤레 — 결과 화면(12 · 13 · 14)에 보인다.
 * @param left 뽑은 **뒤** 서버가 준 그 종류의 남은 수. 아직 다시 읽지 못했으면 null(0 으로 보이지 않는다)
 */
data class DrawnShoe(val kind: DrawKind, val shoe: Sneaker, val left: Int? = null) {
    /** 14 — 이 종류의 마지막 기회였다 */
    val lastChance: Boolean get() = left == 0
}

/** 러닝으로 늘어난 상급 기회(18) — 같은 계정에서 마지막으로 본 수보다 늘었을 때만. 처음 보는 수는 알리지 않는다 */
fun runChancesAdded(seen: Int?, now: DrawStatus): Int =
    if (seen == null || !now.walletLinked) 0 else (now.runLeft - seen).coerceAtLeast(0)

/** 지갑 페이지를 열기 직전의 서버 값 — 연결됐는가, 첫 연결 선물이 몇 회 남아 있었나 */
data class LinkWatch(val linked: Boolean, val giftLeft: Int)

/**
 * 지갑 연결을 이 화면에서 확인했는가(08) — 연결하러 가기 전 값과 돌아와 읽은 값이 모두 서버 값일 때만.
 * 돌려주는 수는 새로 받은 첫 연결 선물(0 이면 다시 연결 — 선물은 다시 주지 않는다). 연결이 확인되지 않았으면 null.
 */
fun linkedGift(before: LinkWatch?, now: DrawStatus): Int? =
    if (before == null || before.linked || !now.walletLinked) null
    else (now.giftLeft - before.giftLeft).coerceAtLeast(0)

// ── 메인 한 패널의 모습(파란 톤 v4 DRAW01 · 02 · 15 · 22 · 24 · 26 · 28 · 29 · 32) ──────────────────────
//
// 2026-10-03 파란 톤 통합 전달본 v4 — 무료 · 상급 두 패널을 한 화면에 함께 보인다(예전 v3 의 글자 탭 대신). 각 패널은 이 규칙을
// 종류마다 한 번씩 불러 상자 · 남은 수 · 한 줄 · 주 버튼 하나를 고른다(아래 [drawHomeCard] 의 tab = 그 패널의 종류).
// 모르는 수(불러오는 중 · 실패 · 로그인 전)는 "—"로 두고 0 으로 보이지 않는다. 결과를 모르는 요청이 있으면 두 패널 모두
// 새 뽑기 대신 "결과 확인"이다.

/** 무대 제목 */
enum class DrawCardTitle { DailyFree, Free, Premium, Pending, SignedOut }

/** 무대 제목 아래 한 줄 — 상급 탭의 지갑 · 서비스 상태 */
enum class DrawCardSub { Linked, WalletNeeded, Paused }

/** 무대 오른쪽 위 */
sealed interface DrawChip {
    data class Count(val value: Int) : DrawChip

    /** 20 — 결과 확인 중 */
    data object Checking : DrawChip

    /** 09 — 지갑을 연결한 적이 없다(수 대신 지갑 그림) */
    data object Wallet : DrawChip

    /** 모르는 수 — "—" */
    data object Unknown : DrawChip

    data object None : DrawChip
}

/** 남은 횟수 줄의 이름 */
enum class DrawRowLabel { FreeLeft, PremiumLeft, PremiumKept, Result }

/** 무대 아래 한 줄 — 남은 횟수 · 모르는 수 · 결과 확인 중 · 첫 연결 선물 */
sealed interface DrawRow {
    data class Count(val label: DrawRowLabel, val value: Int) : DrawRow
    data class Unknown(val label: DrawRowLabel) : DrawRow
    data object Pending : DrawRow

    /** 09 — 받을 수 있는 첫 연결 선물(이미 가진 횟수가 아니다) */
    data class Gift(val value: Int) : DrawRow
}

/** 실행 버튼 하나 */
enum class DrawCardAction {
    /** 무료로 1회 뽑기 · 상급으로 1회 뽑기 */
    Draw,

    /** 13 — 무료 기회를 모두 썼다(버튼은 안내를 연다) */
    FreeInfo,

    /** 14 — 연결됨 · 상급 기회 없음(러닝하고 기회 받기) */
    RunInfo,

    /** 09 → 10 — 지갑 연결하고 N회 받기 */
    Connect,

    /** 12 — 지갑 다시 연결하기(선물은 다시 주지 않는다) */
    Reconnect,

    /** 23 — 상급 뽑기를 잠시 멈췄다(누를 수 없다) */
    Paused,

    /** 20 — 결과 확인(새 뽑기는 막는다) */
    Check,

    /** 03 — 불러오는 중(누를 수 없다) */
    Loading,

    /** 21 — 다시 불러오기 */
    Reload,

    /** 22 — 로그인 화면으로 */
    SignIn,
}

data class DrawHomeCard(
    val kind: DrawKind,
    val title: DrawCardTitle,
    val sub: DrawCardSub?,
    val chip: DrawChip,
    val row: DrawRow,
    val action: DrawCardAction,
    /** 불러오는 중 · 실패 — 상자를 흐리게 */
    val dim: Boolean = false,
    /** 불러오는 중 — 상자 위에 돌아가는 표시 */
    val loading: Boolean = false,
)

private fun DrawKind.leftLabel(): DrawRowLabel = if (this == DrawKind.FREE) DrawRowLabel.FreeLeft else DrawRowLabel.PremiumLeft

private fun DrawKind.plainTitle(): DrawCardTitle = if (this == DrawKind.FREE) DrawCardTitle.Free else DrawCardTitle.Premium

/**
 * 고른 탭 [tab] 의 모습. [status] 가 없으면 [loading] · [signedOut] 으로 가른다(둘 다 아니면 불러오기 실패).
 * 결과를 모르는 요청 [pending] 이 그 종류면 "뽑은 신발 · 확인 중", 다른 종류여도 버튼은 "결과 확인"이다.
 */
fun drawHomeCard(status: DrawStatus?, tab: DrawKind, pendingKind: DrawKind?, loading: Boolean, signedOut: Boolean): DrawHomeCard {
    if (status == null) {
        return when {
            signedOut -> DrawHomeCard(tab, DrawCardTitle.SignedOut, null, DrawChip.None, DrawRow.Unknown(tab.leftLabel()), DrawCardAction.SignIn)
            loading -> DrawHomeCard(
                tab, tab.plainTitle(), null, DrawChip.Unknown, DrawRow.Unknown(tab.leftLabel()), DrawCardAction.Loading,
                dim = true, loading = true,
            )
            else -> DrawHomeCard(tab, tab.plainTitle(), null, DrawChip.Unknown, DrawRow.Unknown(tab.leftLabel()), DrawCardAction.Reload, dim = true)
        }
    }
    if (pendingKind == tab) {
        return DrawHomeCard(tab, DrawCardTitle.Pending, null, DrawChip.Checking, DrawRow.Pending, DrawCardAction.Check)
    }
    val card = if (tab == DrawKind.FREE) {
        DrawHomeCard(
            tab, if (status.dailyTotal > 0) DrawCardTitle.DailyFree else DrawCardTitle.Free, null,
            DrawChip.Count(status.freeLeft), DrawRow.Count(DrawRowLabel.FreeLeft, status.freeLeft),
            if (status.freeLeft > 0) DrawCardAction.Draw else DrawCardAction.FreeInfo,
        )
    } else {
        when (status.premiumMode()) {
            PremiumMode.Connect -> DrawHomeCard(
                tab, DrawCardTitle.Premium, DrawCardSub.WalletNeeded, DrawChip.Wallet, DrawRow.Gift(status.giftOnLink), DrawCardAction.Connect,
            )
            PremiumMode.Reconnect -> DrawHomeCard(
                tab, DrawCardTitle.Premium, DrawCardSub.WalletNeeded, DrawChip.Count(status.premiumLeft),
                DrawRow.Count(DrawRowLabel.PremiumKept, status.premiumLeft), DrawCardAction.Reconnect,
            )
            PremiumMode.Paused -> DrawHomeCard(
                tab, DrawCardTitle.Premium, DrawCardSub.Paused, DrawChip.Count(status.premiumLeft),
                DrawRow.Count(DrawRowLabel.PremiumLeft, status.premiumLeft), DrawCardAction.Paused,
            )
            PremiumMode.Ready -> DrawHomeCard(
                tab, DrawCardTitle.Premium, DrawCardSub.Linked, DrawChip.Count(status.premiumLeft),
                DrawRow.Count(DrawRowLabel.PremiumLeft, status.premiumLeft), DrawCardAction.Draw,
            )
            PremiumMode.Empty -> DrawHomeCard(
                tab, DrawCardTitle.Premium, DrawCardSub.Linked, DrawChip.Count(status.premiumLeft),
                DrawRow.Count(DrawRowLabel.PremiumLeft, status.premiumLeft), DrawCardAction.RunInfo,
            )
        }
    }
    // 다른 종류의 결과를 아직 모른다 — 이 탭의 수는 보이되 버튼은 결과 확인(새 뽑기는 막는다)
    return if (pendingKind != null) card.copy(action = DrawCardAction.Check) else card
}

/**
 * 한 종류의 안내 — 무료는 무료 기회(DRAW03), 상급은 상급 기회 · 러닝(DRAW04 · 30), 지갑을 연결한 적이 없으면 받는 방법(DRAW05).
 * v4 메인은 패널마다 "기회 내역 보기"(연결 전 상급 패널에는 없음)와 아래 "기회 받는 방법"을 따로 두어 이 고르기를 그대로 따른다.
 */
fun drawInfoSheet(status: DrawStatus, tab: DrawKind): DrawSheet = when {
    tab == DrawKind.FREE -> DrawSheet.FreeChances
    status.premiumMode() == PremiumMode.Connect -> DrawSheet.Rules
    else -> DrawSheet.PremiumChances
}
