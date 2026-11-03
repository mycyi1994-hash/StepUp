package com.stepup.android.data.repo

/**
 * 거래소가 거절한 까닭 — 화면이 번역된 문구를 고르는 데 쓴다.
 *
 * 서버 함수(0007 · 0022 · 0023 · 0062 의 market_* · 거래 막이)는 한국어 문장으로 거절한다. 그대로 띄우면 영어 · 일본어 ·
 * 중국어 화면에 한국어가 뜨므로 아는 문장은 여기서 갈래로 옮기고, 모르는 문장은 [OTHER](일반 안내)로 둔다.
 * 문장이 바뀌면 [OTHER] 로 떨어질 뿐 틀린 말을 하지는 않는다.
 */
enum class MarketRefusal {
    PRICE_INVALID,
    PRICE_TOO_LOW,
    NOT_MINE,
    NO_SNEAKER,
    ALREADY_LISTED,
    LISTING_CLOSED,
    OWN_LISTING,
    NOT_MY_LISTING,
    NOT_ENOUGH_SUP,
    BID_CLOSED,
    OWN_BID,
    NOT_MY_BID,
    MODEL_MISMATCH,
    LEVEL_TOO_LOW,
    IMPORT_LIMIT,
    /** 신고 있거나 체인에 나가 있는 신발 */
    LOCKED_SHOE,
    /** 처음 받은 신발 등 넘길 수 없는 신발 */
    NOT_TRANSFERABLE,
    /** 이 신발로 더 달려야 넘길 수 있다 — 남은 km 는 [marketRefusalKm] */
    RUN_MORE,
    SIGN_IN,
    OTHER;

    companion object {
        // 서버 문장의 고정된 앞부분 → 갈래. 앞의 것부터 본다
        private val PHRASES = listOf(
            "값이 올바르지 않" to PRICE_INVALID,
            "값이 너무 낮" to PRICE_TOO_LOW,
            "내 스니커즈가 아닙" to NOT_MINE,
            "없는 스니커즈" to NO_SNEAKER,
            "이미 판매 중" to ALREADY_LISTED,
            "판매 중인 신발" to ALREADY_LISTED,
            "이미 끝난 매물" to LISTING_CLOSED,
            "내 매물은 살 수 없" to OWN_LISTING,
            "내 매물이 아닙" to NOT_MY_LISTING,
            "SUP 가 모자" to NOT_ENOUGH_SUP,
            "SUP가 부족" to NOT_ENOUGH_SUP,
            "이미 끝난 입찰" to BID_CLOSED,
            "내 입찰에는 팔 수 없" to OWN_BID,
            "내 입찰이 아닙" to NOT_MY_BID,
            "모델이 다릅" to MODEL_MISMATCH,
            "레벨이 모자" to LEVEL_TOO_LOW,
            "올릴 수 있는 수를 넘었" to IMPORT_LIMIT,
            "착용 중이거나 체인에 있는 신발" to LOCKED_SHOE,
            "넘길 수 없는 신발" to NOT_TRANSFERABLE,
            "더 달려야 넘길 수 있" to RUN_MORE,
            "로그인이 필요" to SIGN_IN,
        )

        /** 서버(또는 앱)가 적어 보낸 거절 문장을 갈래로 */
        fun of(reason: String): MarketRefusal =
            PHRASES.firstOrNull { (phrase, _) -> phrase in reason }?.second ?: OTHER
    }
}

/** "이 신발로 3.5km 를 더 달려야 넘길 수 있습니다" 의 남은 km("3.5"). 없으면 null */
fun marketRefusalKm(reason: String): String? =
    Regex("""(\d+(?:\.\d+)?)\s*km""").find(reason)?.groupValues?.get(1)
