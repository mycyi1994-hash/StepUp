package com.stepup.android.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.monotonicMillis
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.PI
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

/**
 * 실제 지도 타일(안드로이드 MapTiles 와 같은 출처 · 줌 · 상한 · 실패 규칙). 다른 점:
 * 받아 오기는 기기마다([fetchTileBytes] — iOS 는 Ktor/NSURLSession, 디스크 캐시는 NSURLCache 가 맡는다), 메모리 캐시는 장수로(64장).
 * 키(MapTiler)는 앱 설정 — iOS 는 Info.plist StepUpMapTilerKey. 비면 OpenStreetMap 공용 타일.
 */
object MapTiles {
    const val TILE_SIZE = 256
    const val MAX_TILES = 24
    const val DEFAULT_ZOOM = 17
    val FOLLOW_ZOOMS = 15..DEFAULT_ZOOM
    const val FOLLOW_MAGNIFY = 1.3f
    const val MIN_ZOOM = 3
    const val MAX_ZOOM = 18

    /** 앱이 넣는다(안드로이드 ServiceLocator · iOS IosApp) */
    var mapTilerKey: String = ""
    var userAgent: String = "StepUp (+https://stepupcrew.com)"

    /** 받은 타일을 남기는 곳(기기마다 — 안드로이드 cacheDir/tiles · iOS Caches/tiles). 없으면 메모리만 */
    var disk: TileDisk? = null

    private val source get() = if (mapTilerKey.isNotEmpty()) "maptiler" else "osm"

    val ATTRIBUTION: String
        get() = if (mapTilerKey.isNotEmpty()) "© MapTiler © OpenStreetMap contributors" else "© OpenStreetMap contributors"

    private val memory = LinkedHashMap<String, ImageBitmap>()
    private val failed = HashMap<String, Long>()
    private val lock = Mutex()
    private const val MEMORY_TILES = 64
    private const val FAILURE_TTL_MS = 15_000L
    private const val PERMANENT = Long.MAX_VALUE
    const val RETRY_MS = FAILURE_TTL_MS

    var reconnects by mutableIntStateOf(0)
        private set

    fun networkBack() {
        failed.entries.removeAll { it.value != PERMANENT }
        reconnects++
    }

    fun key(zoom: Int, x: Int, y: Int): String = "$source/$zoom/$x/$y"

    fun cached(zoom: Int, x: Int, y: Int): ImageBitmap? = memory[key(zoom, x, y)]

    fun retryable(zoom: Int, x: Int, y: Int): Boolean =
        memory[key(zoom, x, y)] == null && failed[key(zoom, x, y)] != PERMANENT

    /** 타일 한 장 — 메모리 → 네트워크. 실패하면 null(그 칸은 비우고 경로만 그린다) */
    suspend fun load(zoom: Int, x: Int, y: Int): ImageBitmap? {
        val k = key(zoom, x, y)
        lock.withLock {
            memory[k]?.let { return it }
            val failedAt = failed[k]
            if (failedAt != null) {
                if (failedAt == PERMANENT || monotonicMillis() - failedAt < FAILURE_TTL_MS) return null
                failed.remove(k)
            }
        }
        val path = "$source/$zoom/${x}_$y.png"
        // 디스크에 남은 타일 — 끊긴 동안에도 지나간 곳의 지도가 그려진다
        disk?.let { d ->
            val saved = runCatching { d.read(path) }.getOrNull()
            val image = saved?.let { runCatching { it.decodeToImageBitmap() }.getOrNull() }
            if (image != null) {
                lock.withLock {
                    memory[k] = image
                    while (memory.size > MEMORY_TILES) memory.remove(memory.keys.first())
                }
                return image
            }
        }
        val url = if (mapTilerKey.isNotEmpty()) "https://api.maptiler.com/maps/streets-v2/256/$zoom/$x/$y.png?key=$mapTilerKey"
        else "https://tile.openstreetmap.org/$zoom/$x/$y.png"
        val fetched = runCatching { fetchTileBytes(url, userAgent) }.getOrNull()
        val bytes = fetched?.bytes
        if (bytes == null) {
            lock.withLock { failed[k] = if (fetched?.permanent == true) PERMANENT else monotonicMillis() }
            return null
        }
        val image = runCatching { bytes.decodeToImageBitmap() }.getOrNull()
        if (image != null) runCatching { disk?.write(path, bytes) }
        lock.withLock {
            if (image == null) { failed[k] = PERMANENT; return null }
            memory[k] = image
            while (memory.size > MEMORY_TILES) memory.remove(memory.keys.first())
        }
        return image
    }

    // ── 슬리피 맵 좌표 변환 ──────────────────────────────────────
    //
    // 웹 메르카토르. 경도는 선형, 위도는 로그 변환이라 극지방이 늘어난다.
    // 반환값은 "줌 z에서 세계 전체를 펼쳤을 때의 픽셀 좌표"다.

    fun worldX(lng: Double, zoom: Int): Double =
        (lng + 180.0) / 360.0 * worldSize(zoom)

    fun worldY(lat: Double, zoom: Int): Double {
        val s = sin(com.stepup.android.domain.toRadians(lat)).coerceIn(-0.9999, 0.9999)
        return (0.5 - ln((1 + s) / (1 - s)) / (4 * PI)) * worldSize(zoom)
    }

    /** [worldX] 의 반대 — 세계 픽셀 x → 경도 */
    fun lngOf(worldX: Double, zoom: Int): Double = worldX / worldSize(zoom) * 360.0 - 180.0

    /** [worldY] 의 반대 — 세계 픽셀 y → 위도 */
    fun latOf(worldY: Double, zoom: Int): Double {
        val n = PI - 2.0 * PI * worldY / worldSize(zoom)
        return com.stepup.android.domain.toDegrees(kotlin.math.atan(kotlin.math.sinh(n)))
    }

    fun worldSize(zoom: Int): Double = (1 shl zoom).toDouble() * TILE_SIZE

    /**
     * 경로가 화면에 다 들어오는 가장 가까운 줌.
     *
     * 높은 줌부터 내려오며 첫 번째로 들어맞는 값을 고른다. 여백을 12% 남겨
     * 경로가 가장자리에 딱 붙지 않게 한다.
     */
    fun fitZoom(points: List<GeoPoint>, widthPx: Int, heightPx: Int): Int {
        if (points.isEmpty() || widthPx <= 0 || heightPx <= 0) return DEFAULT_ZOOM
        if (points.size == 1) return DEFAULT_ZOOM

        val minLat = points.minOf { it.lat }
        val maxLat = points.maxOf { it.lat }
        val minLng = points.minOf { it.lng }
        val maxLng = points.maxOf { it.lng }

        val usableW = widthPx * 0.88
        val usableH = heightPx * 0.88

        for (zoom in MAX_ZOOM downTo MIN_ZOOM) {
            val spanX = worldX(maxLng, zoom) - worldX(minLng, zoom)
            // y는 위도가 커질수록 작아지므로 뒤집어 뺀다
            val spanY = worldY(minLat, zoom) - worldY(maxLat, zoom)
            if (spanX <= usableW && spanY <= usableH) {
                // 실제로 받는 타일은 경로 span이 아니라 **뷰포트**를 덮는다.
                // 예산을 span으로 재면 상한이 상한 노릇을 못 한다.
                val tiles = tileCount(widthPx.toDouble(), heightPx.toDouble())
                if (tiles <= MAX_TILES) return zoom
            }
        }
        return MIN_ZOOM
    }

    fun tileCount(spanX: Double, spanY: Double): Int {
        val cols = (spanX / TILE_SIZE).toInt() + 2
        val rows = (spanY / TILE_SIZE).toInt() + 2
        return max(1, cols * rows)
    }
}

/** 받아 온 타일 — [permanent] 면 404 · 403(다시 받아도 없다) */
class FetchedTile(val bytes: ByteArray?, val permanent: Boolean)

/** 타일 그림 바이트를 받는다(6초 제한, 앱을 알리는 User-Agent) — 기기마다 */
expect suspend fun fetchTileBytes(url: String, userAgent: String): FetchedTile


/** 타일 디스크 캐시 — [path] 는 "<출처>/<줌>/<x>_<y>.png" */
interface TileDisk {
    fun read(path: String): ByteArray?
    fun write(path: String, bytes: ByteArray)
}
