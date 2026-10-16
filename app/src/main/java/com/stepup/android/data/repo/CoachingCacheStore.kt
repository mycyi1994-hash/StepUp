package com.stepup.android.data.repo

import com.stepup.android.data.remote.parseResult
import com.stepup.android.domain.coaching.CachedCoach
import com.stepup.android.domain.coaching.CoachResult
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.addJsonObject

/**
 * AI 코칭 결과의 이 휴대폰 사본 — 오프라인(10)에서 "저장된 분석"을 생성 시각과 함께 보이려고 둔다. 정본은 서버다.
 *
 * 계정마다 따로 된 파일 하나다(이름은 계정 글자의 해시). 계정이 바뀌면 다른 파일을 읽으므로 이전 계정의 분석이 새 계정
 * 화면에 붙지 않는다. 열쇠는 대상(기록의 시작 시각 · 주 · 추천) · 언어 — 같은 계정 · 같은 대상 · 같은 언어일 때만 보인다.
 * 로컬 기록을 지우면 그 기록의 사본도 지운다([removeRun]). 계정마다 [MAX_ENTRIES] 개까지만 둔다(오래된 것부터 버린다).
 */
class CoachingCacheStore(private val dir: File) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun get(owner: String, key: String): CachedCoach? = withContext(Dispatchers.IO) {
        mutex.withLock { read(owner)[key] }
    }

    suspend fun put(owner: String, key: String, result: CoachResult, savedAt: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = read(owner).toMutableMap()
            all[key] = CachedCoach(result, savedAt)
            val kept = all.entries.sortedByDescending { it.value.savedAt }.take(MAX_ENTRIES).associate { it.key to it.value }
            write(owner, kept)
        }
    }

    /** 로컬 기록 하나를 지웠다 — 그 기록(시작 시각)의 모든 언어 사본을 지운다 */
    suspend fun removeRun(owner: String, startedAt: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val all = read(owner)
            val kept = all.filterKeys { !it.startsWith(runKeyPrefix(startedAt)) }
            if (kept.size != all.size) write(owner, kept)
        }
    }

    /** 이 계정의 사본을 모두 지운다(동의 해제 때 "저장된 AI 분석도 삭제"를 골랐을 때) */
    suspend fun clear(owner: String) = withContext(Dispatchers.IO) {
        mutex.withLock { fileOf(owner).delete() }
    }

    private fun fileOf(owner: String): File {
        val digest = MessageDigest.getInstance("SHA-256").digest(owner.toByteArray(Charsets.UTF_8))
        return File(dir, digest.take(16).joinToString("") { "%02x".format(it) } + ".json")
    }

    /** 읽을 수 없는 파일은 빈 사본으로 본다 — 사본은 다시 받을 수 있고, 깨진 사본으로 결과를 보이지 않는다 */
    private fun read(owner: String): Map<String, CachedCoach> {
        val file = fileOf(owner)
        if (!file.exists()) return emptyMap()
        val root = runCatching { json.parseToJsonElement(file.readText()) as? JsonObject }.getOrNull() ?: return emptyMap()
        return root.mapNotNull { (key, value) ->
            val entry = value as? JsonObject ?: return@mapNotNull null
            val savedAt = (entry["saved_at"] as? JsonPrimitive)?.longOrNull ?: return@mapNotNull null
            val result = (entry["result"] as? JsonObject)?.let(::parseResult) ?: return@mapNotNull null
            key to CachedCoach(result, savedAt)
        }.toMap()
    }

    private fun write(owner: String, entries: Map<String, CachedCoach>) {
        dir.mkdirs()
        val target = fileOf(owner)
        val body = buildJsonObject {
            entries.forEach { (key, cached) ->
                putJsonObject(key) {
                    put("saved_at", cached.savedAt)
                    put("result", resultJson(cached.result))
                }
            }
        }.toString()
        val temp = File(dir, target.name + ".tmp")
        temp.writeText(body)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    companion object {
        const val MAX_ENTRIES = 200

        fun runKeyPrefix(startedAt: Long) = "run:$startedAt:"
        fun runKey(startedAt: Long, locale: String) = "${runKeyPrefix(startedAt)}$locale"
        fun weekKey(monday: java.time.LocalDate, zone: String, locale: String) = "week:$monday:$zone:$locale"
        fun recKey(monday: java.time.LocalDate, locale: String) = "rec:$monday:$locale"
    }
}

/** 결과 → 서버가 내는 것과 같은 JSON 모양([parseResult] 가 다시 읽는다) */
internal fun resultJson(r: CoachResult): JsonObject = buildJsonObject {
    put("id", r.id)
    put("kind", r.kind.key)
    put("generated_at", java.time.Instant.ofEpochMilli(r.generatedAt).toString())
    put("locale", r.locale)
    putJsonArray("facts") { r.facts.forEach { f -> addJsonObject { put("id", f.id); putJsonObject("values") { f.values.forEach { (k, v) -> put(k, v) } } } } }
    put("fact_id", r.factId)
    put("explanation", r.explanation)
    putJsonArray("candidates") { r.candidates.forEach { c -> addJsonObject { put("id", c.id); putJsonObject("values") { c.values.forEach { (k, v) -> put(k, v) } } } } }
    put("candidate_id", r.candidateId)
    put("action_reason", r.actionReason)
    put("limitation", r.limitation)
    put("provider", r.provider)
}
