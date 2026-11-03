package com.stepup.android.ui.background

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import com.stepup.ui.res.Res
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/*
 * 장소 · 시간대 배경 사진(안드로이드 ui/background/RunBackground.kt 와 같은 고르는 규칙 · 같은 사진).
 * 다른 점은 읽는 길뿐 — 앱 assets 대신 composeResources files/backgrounds(빌드 때 안드로이드 assets 에서 복사), 장소 목록 · 사진은 화면이 처음 쓸 때 읽어 기억한다.
 */

enum class TimeOfDay { DAWN, DAY, DUSK, NIGHT } // 05–08 · 08–17 · 17–20 · 20–05

enum class SpotKind { HANGANG, RIVER, STREAM, LAKE, PARK, MOUNTAIN, TRAIL, TRACK, CITY, BEACH }

@Serializable
data class Spot(
    val id: String,
    val name: String,
    val kind: SpotKind,
    val lat: Double,
    val lng: Double,
    val radiusM: Int = 800,
    val region: String = "",
    val district: String = "",
    val credit: String = "",
)

@Serializable
private data class SpotFile(val spots: List<Spot> = emptyList())

/** 장소 목록 — files/backgrounds/spots.json 을 한 번 읽어 둔다 */
object Spots {
    private var loaded: List<Spot>? = null
    private val lock = Mutex()
    private val json = Json { ignoreUnknownKeys = true }

    @OptIn(ExperimentalResourceApi::class)
    suspend fun all(): List<Spot> = loaded ?: lock.withLock {
        loaded ?: runCatching {
            json.decodeFromString(SpotFile.serializer(), Res.readBytes("files/backgrounds/spots.json").decodeToString()).spots
        }.getOrDefault(emptyList()).also { loaded = it }
    }

    suspend fun match(text: String?): Spot? = matchIn(all(), text)

    /** 장소 하나(안드로이드 Spots.byId 와 같다) */
    suspend fun byId(id: String): Spot? = all().firstOrNull { it.id == id }

    /** [match] 의 판정만 — 장소 목록을 받아서(검사가 먹여 본다) */
    fun matchIn(spots: List<Spot>, text: String?): Spot? {
        if (text.isNullOrBlank()) return null
        val t = text.replace(" ", "")
        return spots.map { it to keywords(it).filter { k -> k.length >= 2 && t.contains(k) }.maxOfOrNull { k -> k.length } }
            .filter { it.second != null }.maxByOrNull { it.second!! }?.first
    }

    /** 이름에서 알아볼 낱말 — 붙은 말(한강공원 · 공원 · 해변 …)을 떼어 본다 */
    private fun keywords(spot: Spot): List<String> {
        val name = spot.name.replace(" ", "")
        val stripped = GENERIC.fold(name) { acc, suffix -> acc.removeSuffix(suffix) }
        // 낱말마다(망원 · 한강공원) — 어느 장소에나 붙는 말(한강공원 · 공원 …)은 뺀다. "여의도 한강공원"이 망원으로 잡히지 않게
        val parts = spot.name.split(' ', '·').map { it.trim() }.filter { it.isNotEmpty() && it !in GENERIC }
        return (listOf(name, stripped) + parts).distinct()
    }

    private val GENERIC = listOf(
        "한강공원", "국가정원", "호수공원", "공원", "해변", "해안산책로", "산책로", "수변길", "성곽길", "숲길", "호수", "습지",
        "해안", "구름산책로", "서우봉", "둘레길",
    )

    suspend fun nearest(lat: Double, lng: Double): Spot? = RunBackground.nearest(all(), lat, lng)
}

/** 에셋 이름 = bg_<spotId>_<time> · bg_kind_<kind>_<time> */
object RunBackground {
    const val DEFAULT = "bg_mangwon_dusk"

    fun timeOf(hour: Int) = when (hour) {
        in 5..7 -> TimeOfDay.DAWN
        in 8..16 -> TimeOfDay.DAY
        in 17..19 -> TimeOfDay.DUSK
        else -> TimeOfDay.NIGHT
    }

    fun now(): TimeOfDay = timeOf(kotlinx.datetime.Instant.fromEpochMilliseconds(com.stepup.android.domain.epochMillis()).toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).hour)

    /** 지금 위치에서 가장 가까운 장소(반경 안). 번개 = 모이는 곳, 크루 = 활동 지역, 러닝 중 = GPS, 완료 = 가장 오래 달린 곳, 홈 = 자주 달리는 곳 */
    fun nearest(spots: List<Spot>, lat: Double, lng: Double): Spot? =
        spots.map { it to distM(lat, lng, it.lat, it.lng) }.filter { (s, d) -> d <= s.radiusM }.minByOrNull { it.second }?.first

    /**
     * 홈의 "자주 달리는 곳" — 러닝을 시작한 자리마다 반경 안 가장 가까운 장소를 세어 가장 많이 달린 곳. 같으면 최근 것.
     * [starts] 는 최근 러닝부터(위도, 경도). 어느 장소 반경에도 들지 않은 러닝은 세지 않는다 — 그런 러닝뿐이면 null.
     */
    fun frequent(spots: List<Spot>, starts: List<Pair<Double, Double>>): Spot? {
        val counts = LinkedHashMap<Spot, Int>()
        starts.forEach { (lat, lng) -> nearest(spots, lat, lng)?.let { counts[it] = (counts[it] ?: 0) + 1 } }
        // 처음 나온 순서(= 최근 순서)로 들어 있어 maxBy 가 같은 수에서 최근 장소를 고른다
        return counts.maxByOrNull { it.value }?.key
    }

    /** 있는 에셋 이름 목록에서 가장 잘 맞는 것 + 색보정이 필요한지. 날씨는 [WeatherLayer] 로 따로 얹는다 */
    fun resolve(have: Set<String>, spot: Spot?, time: TimeOfDay, kind: SpotKind? = spot?.kind): Pick {
        val t = time.name.lowercase()
        val candidates = buildList {
            if (spot != null) {
                add("bg_${spot.id}_$t" to false)
                add("bg_${spot.id}" to true) // 장소 기본 1장 → 시간대는 색보정
            }
            if (kind != null) {
                val k = kind.name.lowercase()
                add("bg_kind_${k}_$t" to false)
                add("bg_kind_$k" to true)
            }
            add(DEFAULT to false)
        }
        val (key, grade) = candidates.firstOrNull { it.first in have } ?: (DEFAULT to false)
        return Pick(key, if (grade) gradeFor(time) else null)
    }

    data class Pick(val assetKey: String, val grade: ColorMatrix?)

    /** 장소를 모를 때 쓰는 종류 — 같은 글(크루 · 모임)은 늘 같은 종류(사진이 넘길 때마다 바뀌지 않게). 특정 장소처럼 보이지 않는 대표 사진만 */
    fun kindFor(seed: String): SpotKind {
        val kinds = listOf(SpotKind.HANGANG, SpotKind.CITY, SpotKind.RIVER, SpotKind.PARK, SpotKind.LAKE, SpotKind.STREAM)
        return kinds[abs(com.stepup.android.domain.javaHash(seed) % kinds.size)]
    }

    /** 시간대 색보정(사진 1장으로 새벽 · 낮 · 저녁 · 밤을 만든다) — Image(colorFilter = ColorFilter.colorMatrix(grade)) */
    fun gradeFor(time: TimeOfDay): ColorMatrix {
        // [밝기, 채도, 파랑 쪽 치우침] — 시네마 청회색 톤(cineub) 기준
        val (b, s, blue) = when (time) {
            TimeOfDay.DAWN -> Triple(.92f, .85f, .06f)
            TimeOfDay.DAY -> Triple(1.05f, .95f, .02f)
            TimeOfDay.DUSK -> Triple(.85f, .9f, .08f)
            TimeOfDay.NIGHT -> Triple(.55f, .75f, .14f)
        }
        val m = ColorMatrix().apply { setToSaturation(s) }
        val scale = ColorMatrix(floatArrayOf(b, 0f, 0f, 0f, 0f, 0f, b, 0f, 0f, 0f, 0f, 0f, b + blue, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        m.timesAssign(scale)
        return m
    }

    /** 에셋 이름 → 앱 안 경로 */
    fun assetPath(key: String): String {
        val name = key.removePrefix("bg_")
        return if (name.startsWith("kind_")) "backgrounds/_kind/$key.webp"
        else "backgrounds/${spotIdOf(name)}/$key.webp"
    }

    private fun spotIdOf(name: String): String {
        val times = listOf("_dawn", "_day", "_dusk", "_night")
        return times.fold(name) { acc, t -> acc.removeSuffix(t) }
    }

    fun distM(a: Double, b: Double, c: Double, d: Double): Double {
        val r = 6371000.0
        val dLat = com.stepup.android.domain.toRadians(c - a)
        val dLng = com.stepup.android.domain.toRadians(d - b)
        val h = sin(dLat / 2).pow(2) + cos(com.stepup.android.domain.toRadians(a)) * cos(com.stepup.android.domain.toRadians(c)) * sin(dLng / 2).pow(2)
        return 2 * r * asin(sqrt(h))
    }
}

/** 앱 안 배경 사진 — 푼 그림을 몇 장 기억한다(같은 사진을 화면마다 다시 풀지 않게) */
object BackgroundAssets {
    private val cache = LinkedHashMap<String, ImageBitmap>()
    private val lock = Mutex()

    /** 앱에 넣은 사진 이름 — spots.json 의 장소 × 시간대 넉 장 + 종류 대표 × 넉 장(안드로이드 bundled 와 같다) */
    fun bundled(spots: List<Spot>): Set<String> {
        val times = TimeOfDay.entries.map { it.name.lowercase() }
        return (spots.flatMap { s -> times.map { "bg_${s.id}_$it" } } +
            SpotKind.entries.flatMap { k -> times.map { "bg_kind_${k.name.lowercase()}_$it" } }).toSet()
    }

    suspend fun have(): Set<String> = bundled(Spots.all())

    @OptIn(ExperimentalResourceApi::class)
    suspend fun decode(key: String): ImageBitmap? = lock.withLock {
        cache[key]?.let { return@withLock it }
        runCatching { Res.readBytes("files/" + RunBackground.assetPath(key)).decodeToImageBitmap() }.getOrNull()?.also {
            cache[key] = it
            while (cache.size > 6) cache.remove(cache.keys.first())
        }
    }
}

/** 고른 장소 사진 + (필요하면) 시간대 색보정 */
data class SpotPhoto(val painter: Painter, val colorFilter: ColorFilter?)

/** 장소 · 시간대에 맞는 배경 사진 — 안드로이드와 같은 차례(장소 → 종류 대표 → 기본 망원 저녁). 다 읽기 전에는 null */
@Composable
fun rememberSpotPhoto(spot: Spot?, kind: SpotKind? = null, time: TimeOfDay = RunBackground.now(), widthDp: Int = 393): SpotPhoto? {
    val photo by produceState<SpotPhoto?>(initialValue = null, spot?.id, kind, time) {
        val pick = RunBackground.resolve(BackgroundAssets.have(), spot, time, kind ?: spot?.kind)
        value = BackgroundAssets.decode(pick.assetKey)?.let { SpotPhoto(BitmapPainter(it), pick.grade?.let { g -> ColorFilter.colorMatrix(g) }) }
    }
    return photo
}

/** 글(크루 활동 지역 · 모이는 곳) · 좌표 · 고정 씨앗으로 장소 사진을 고른다 — 이름 → 좌표 반경 → 씨앗의 종류 대표 */
@Composable
fun rememberPlacePhoto(
    place: String?,
    lat: Double? = null,
    lng: Double? = null,
    seed: String = place.orEmpty(),
    widthDp: Int = 393,
    time: TimeOfDay = RunBackground.now(),
): SpotPhoto? {
    val spot by produceState<Spot?>(initialValue = null, place, lat, lng) {
        value = Spots.match(place) ?: if (lat != null && lng != null) Spots.nearest(lat, lng) else null
    }
    return rememberSpotPhoto(spot, kind = if (spot == null) RunBackground.kindFor(seed) else null, time = time, widthDp = widthDp)
}
