package com.stepup.android.data.remote

import com.stepup.android.domain.Bag
import com.stepup.android.domain.BagItem
import com.stepup.android.domain.ItemKind
import com.stepup.android.domain.RewardEvent
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

/**
 * 아이템(서버 0106) — 가방 · 바로 쓰기 · 러닝에 묶기. 개수는 서버가 정본이고, 폰은 받은 값을 보일 뿐 스스로 더하거나 빼지 않는다.
 * 문서: docs/items-2026-11-17.md
 */
class ItemsApi(private val server: StepUpServer) {

    val isConfigured: Boolean get() = server.isConfigured

    /** 내 가방 · 지금 켜진 보상 이벤트 */
    suspend fun bag(): ServerResult<Bag> =
        server.authed { token -> server.http.post("${server.restUrl}/rpc/items_me", "{}", server.headers(token)) }
            .mapBody { parseBag(it) }

    /**
     * 바로 쓰는 아이템(내구도 초기화권 · 하루 할당량 초기화권). 남은 개수를 돌려준다.
     * [request] 가 같으면 서버가 다시 쓰지 않고 처음 결과를 돌려준다 — 응답을 잃고 다시 눌러도 하나만 빠진다.
     */
    suspend fun use(kind: ItemKind, request: String, sneakerServerId: Long?): ServerResult<Int> {
        val body = jsonBody {
            put("p_kind", kind.id)
            put("p_request", request)
            sneakerServerId?.takeIf { it > 0 }?.let { put("p_sneaker", it) }
        }
        return server.authed { token -> server.http.post("${server.restUrl}/rpc/item_use", body, server.headers(token)) }
            .mapBody { serverJson.decodeFromString<ItemUseRow>(it).left }
    }

    /**
     * 출발 때 고른 아이템을 그 러닝(시작 시각 — 러닝을 올릴 때와 같은 값)에 묶는다. 같은 러닝 · 같은 종류는 서버가 한 번만 뺀다.
     * [expectedUserId] 는 러닝을 기록한 계정 — 다른 계정으로 로그인해 있으면 보내지 않는다(그 계정의 가방에서 빠지지 않게).
     */
    suspend fun bindToRun(startedAtMillis: Long, kinds: List<String>, expectedUserId: String): ServerResult<Unit> {
        val body = "{\"p_run_started_at\":${startedAtMillis.toIsoInstant().asJsonString()}," +
            "\"p_kinds\":${kinds.joinToString(",", "[", "]") { it.asJsonString() }}}"
        return server.authed(expectedUserId) { token ->
            server.http.post("${server.restUrl}/rpc/run_items_use", body, server.headers(token))
        }.mapBody { }
    }

    companion object {
        internal fun parseBag(body: String): Bag? = runCatching {
            val row = serverJson.decodeFromString<BagRowDto>(body)
            Bag(
                items = row.items.mapNotNull { r -> ItemKind.of(r.kind)?.let { BagItem(it, r.value, r.qty) } },
                event = row.event?.let { e ->
                    val ends = runCatching { java.time.OffsetDateTime.parse(e.endsAt).toInstant().toEpochMilli() }.getOrNull()
                    ends?.let { RewardEvent(e.title, e.mult, it) }
                },
                boostUntilMillis = row.boostUntil?.let { runCatching { java.time.OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull() },
            )
        }.getOrNull()
    }
}

@Serializable
private data class BagRowDto(
    val items: List<BagItemRowDto> = emptyList(),
    val event: BagEventRow? = null,
    @SerialName("boost_until") val boostUntil: String? = null,
)

@Serializable
private data class BagItemRowDto(val kind: String, val value: Double? = null, val qty: Int = 0)

@Serializable
private data class BagEventRow(val title: String, val mult: Double, @SerialName("ends_at") val endsAt: String)

@Serializable
private data class ItemUseRow(val left: Int)
