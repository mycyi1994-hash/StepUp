package com.stepup.android.data.repo

import com.stepup.android.data.remote.ServerResult

/** 서버의 강화 함수(0054, 규칙 forge-v2) — EconomyApi 가 구현한다. 검사에서는 흉내 낸 서버를 넣는다 */
interface ForgeServer {
    suspend fun forgeMaterials(target: Long): ServerResult<String>
    suspend fun forgeQuote(target: Long, materials: List<Long>): ServerResult<String>
    suspend fun forgeStart(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String>
    suspend fun forgeResult(key: String): ServerResult<String>
}
