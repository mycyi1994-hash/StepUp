package com.stepup.android.data.remote

/**
 * 경험치 강화(서버 0118, 규칙 forge-xp-1) — 재료 1 ~ 10켤레를 태워 통을 채운다. 응답은 JSON 한 덩어리(ForgeRepository 가 읽는다).
 * 서버에 0118 이 없으면 PostgREST 가 함수를 못 찾는다(PGRST202) — [isMissingForgeXp] 이면 ForgeRepository 가 예전 강화(forge_*)로 돈다.
 * 문서: docs/forge-xp-daily-contract-2026-11-19.md "강화"
 */
class ForgeXpApi(private val server: StepUpServer) : com.stepup.android.data.repo.ForgeXpServer {

    val isConfigured: Boolean get() = server.isConfigured

    /** 강화 화면을 열 때 — 대상의 통과 내 신발마다 값 · % · 막힌 까닭 · 아까운 까닭, 대성공 확률 · 한 번에 넣는 상한 */
    override suspend fun forgeXpMaterials(target: Long): ServerResult<String> =
        rpc("forge_xp_materials", "{\"p_target\":$target}")

    /** 넣기 전 견적 — 대성공 없이 받는 최소값과 견적 서명 */
    override suspend fun forgeXpQuote(target: Long, materials: List<Long>): ServerResult<String> =
        rpc("forge_xp_quote", "{\"p_target\":$target,\"p_materials\":${materials.joinToString(",", "[", "]")}}")

    /** 넣기 한 번. 같은 [key] 로 다시 보내면 서버가 처음 결과를 그대로 돌려준다(두 번 태우지 않는다) */
    override suspend fun forgeXpAdd(key: String, target: Long, materials: List<Long>, quoteVersion: String): ServerResult<String> =
        rpc(
            "forge_xp_add",
            "{\"p_key\":${key.asJsonString()},\"p_target\":$target," +
                "\"p_materials\":${materials.joinToString(",", "[", "]")},\"p_quote\":${quoteVersion.asJsonString()}}",
        )

    /** 같은 요청 번호의 결과 — 서버에 기록이 없으면 본문이 `null` */
    override suspend fun forgeXpResult(key: String): ServerResult<String> =
        rpc("forge_xp_result", "{\"p_key\":${key.asJsonString()}}")

    private suspend fun rpc(name: String, body: String): ServerResult<String> =
        server.authed { token -> server.http.post("${server.restUrl}/rpc/$name", body, server.headers(token)) }
}

/** PostgREST 가 이름 · 인자에 맞는 서버 함수를 못 찾았다(PGRST202 — 서버에 0118 이 아직 없다) */
internal fun ServerResult<*>.isMissingForgeXp(): Boolean =
    this is ServerResult.Rejected && ("Could not find the function" in reason || "PGRST202" in reason)
