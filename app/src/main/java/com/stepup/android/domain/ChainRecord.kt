package com.stepup.android.domain

/**
 * 내 체인 기록 한 줄 — 서버 my_chain_activity(0044).
 *
 * 우리가 가스비를 내고 GIWA Sepolia 에 올리는 기록이다: 러닝 증명 · 코스 완주 · 배지(EAS 증명),
 * 뽑은 신발 발행 · 강화/수리 반영(v3 신발). 확정되기 전에는 "대기"이고 거래 번호가 없다 —
 * 서버가 확정 블록에서 확인하기 전에는 "기록됨"이라고 쓰지 않는다.
 */
data class ChainRecord(
    val id: Long,
    val kind: Kind,
    val status: Status,
    val distanceM: Int? = null,
    val durationSec: Int? = null,
    val badge: String? = null,
    val badgeValue: Int? = null,
    val sneakerId: Long? = null,
    val txHash: String? = null,
    val createdAt: Long,
    val confirmedAt: Long? = null,
) {
    enum class Kind { RUN_PROOF, COURSE_RUN, BADGE, VAULT_MINT, STATS_SYNC, UNKNOWN }

    enum class Status { PENDING, CONFIRMED, CANCELLED, FAILED }

    /** 익스플로러의 이 거래 — 확정된 것만 */
    val explorerUrl: String?
        get() = txHash?.takeIf { status == Status.CONFIRMED && TX.matches(it) }?.let { "${ChainNetwork.EXPLORER}/tx/$it" }

    companion object {
        private val TX = Regex("^0x[0-9a-fA-F]{64}$")

        fun kindOf(raw: String): Kind = Kind.entries.firstOrNull { it.name == raw } ?: Kind.UNKNOWN

        /** 서버는 확정 전(QUEUED · CLAIMED · SENT)을 PENDING 으로 준다. 모르는 값도 대기로 — 기록됐다고 보이지 않게 */
        fun statusOf(raw: String): Status = Status.entries.firstOrNull { it.name == raw } ?: Status.PENDING
    }
}

/** 체인 기록이 올라가는 곳 */
object ChainNetwork {
    const val NAME = "GIWA Sepolia"
    const val EXPLORER = "https://sepolia-explorer.giwa.io"
}
