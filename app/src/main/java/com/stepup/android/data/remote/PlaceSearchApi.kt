package com.stepup.android.data.remote

import com.stepup.android.BuildConfig
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.haversineMeters
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 장소 이름 검색 · 좌표로 이름 찾기 — MapTiler Geocoding.
 *
 * 지도 타일([com.stepup.android.ui.components.MapTiles])과 같은 MapTiler 계정 · 키를 쓴다(새 외부
 * 서비스가 아니다). 데이터는 OpenStreetMap 이라 한국의 작은 가게 · 일부 공원은 없을 수 있다 —
 * 그래서 화면은 "지도에서 장소 보기"로 직접 고르는 길을 늘 함께 둔다.
 */
class PlaceSearchApi(
    private val key: String = BuildConfig.MAPTILER_KEY.trim(),
    private val fetch: suspend (String) -> String? = ::download,
) {
    val isConfigured: Boolean get() = key.isNotEmpty()

    /** 장소 이름으로 찾는다. [near] 가 있으면 그 가까이를 앞에 둔다. 연결이 안 되면 null */
    suspend fun search(query: String, near: GeoPoint?, language: String, limit: Int = 8): List<StoryPlace>? {
        if (!isConfigured || query.isBlank()) return emptyList()
        val url = buildString {
            append("https://api.maptiler.com/geocoding/")
            append(encode(query.trim()))
            append(".json?key=").append(key)
            append("&language=").append(language)
            append("&limit=").append(limit)
            // 가게 · 역 · 공원(poi)은 types 에 적어야 나온다 — 빼면 "여의나루역" 같은 이름이 안 잡힌다
            append("&types=").append(PLACE_TYPES)
            if (near != null) append("&proximity=").append(coordinate(near.lng)).append(',').append(coordinate(near.lat))
        }
        val body = fetch(url) ?: return null
        return parse(body)?.mapNotNull { it.toPlace() }
    }

    /** 지역(동 · 구) 이름으로 찾는다 — 위치 없이 "선택한 지역에서" 볼 때 */
    suspend fun regions(query: String, near: GeoPoint?, language: String): List<StoryPlace>? {
        if (!isConfigured || query.isBlank()) return emptyList()
        val url = buildString {
            append("https://api.maptiler.com/geocoding/")
            append(encode(query.trim()))
            append(".json?key=").append(key)
            append("&language=").append(language)
            append("&limit=8&types=").append(REGION_TYPES)
            if (near != null) append("&proximity=").append(coordinate(near.lng)).append(',').append(coordinate(near.lat))
        }
        val body = fetch(url) ?: return null
        return parse(body)?.mapNotNull { it.toPlace() }
    }

    /**
     * 좌표에 이름을 붙인다 — 지도에서 직접 고른 자리. 바로 곁(120m 안)에 가게 · 시설(POI)이 있으면 그 이름,
     * 없으면 길 · 동네 이름. 이름을 못 찾으면 null(좌표를 이름처럼 적지 않는다).
     */
    suspend fun nameAt(point: GeoPoint, language: String): StoryPlace? {
        if (!isConfigured) return null
        val base = "https://api.maptiler.com/geocoding/${coordinate(point.lng)},${coordinate(point.lat)}.json?key=$key&language=$language"
        // 먼 시설 이름을 붙이면 다른 곳으로 보인다 — 누른 자리 곁의 것만
        val poi = fetch("$base&types=poi")?.let(::parse)
            ?.firstOrNull { feature -> feature.centerPoint()?.let { haversineMeters(it, point) <= POI_REACH_METERS } == true }
        val any = poi ?: fetch(base)?.let(::parse)?.firstOrNull { it.kind in NAMEABLE_TYPES }
        val feature = any ?: return null
        val name = feature.text.trim().takeIf { it.isNotEmpty() } ?: return null
        // 지도에서 누른 자리 그대로 — 찾은 시설의 한가운데로 옮기지 않는다
        return StoryPlace(name.take(80), feature.address().take(120), point.lat, point.lng)
    }

    /** 이 좌표의 동네 이름 — 목록 위 "여의도동 주변". 못 찾으면 null */
    suspend fun areaName(point: GeoPoint, language: String): String? {
        if (!isConfigured) return null
        val url = "https://api.maptiler.com/geocoding/${coordinate(point.lng)},${coordinate(point.lat)}.json?key=$key&language=$language"
        val features = fetch(url)?.let(::parse) ?: return null
        return AREA_TYPES.firstNotNullOfOrNull { type -> features.firstOrNull { it.kind == type }?.text?.trim() }
            ?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        /** 글에 붙일 장소로 찾는 것 — 시설 · 길 이름 · 동네. 나라 · 시도 · 우편번호는 너무 넓다 */
        private const val PLACE_TYPES = "poi,address,place,neighbourhood,locality,municipality,major_landform"

        private const val REGION_TYPES = "place,municipality,municipal_district,locality,neighbourhood,county"

        /**
         * 좌표에 붙일 이름으로 쓸 수 있는 것 — 나라 · 시도처럼 넓은 이름은 뺀다. 도로(road)는
         * "국도 제46호선" 같은 이름이라 뺀다.
         */
        private val NAMEABLE_TYPES = setOf("poi", "address", "place", "neighbourhood", "locality", "municipality", "major_landform")

        /** 지도에서 누른 자리에 시설 이름을 붙일 수 있는 거리 */
        private const val POI_REACH_METERS = 120.0

        /** 목록 위에 보일 동네 이름 — 좁은 것부터 */
        private val AREA_TYPES = listOf("place", "neighbourhood", "municipality", "locality", "municipal_district", "county")

        fun parse(body: String): List<GeoFeature>? =
            runCatching { json.decodeFromString(GeoResponse.serializer(), body).features }.getOrNull()

        private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")

        /** 로케일에 따라 소수점이 쉼표가 되면 주소가 깨진다 */
        private fun coordinate(value: Double): String = String.format(Locale.ROOT, "%.6f", value)

        private val userAgent = "StepUp/${BuildConfig.VERSION_NAME} (Android; +https://stepupcrew.com)"

        private suspend fun download(url: String): String? = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.setRequestProperty("User-Agent", userAgent)
                    connection.connectTimeout = 6_000
                    connection.readTimeout = 6_000
                    if (connection.responseCode != HttpURLConnection.HTTP_OK) null
                    else connection.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
                } finally {
                    connection.disconnect()
                }
            }.getOrNull()
        }
    }
}

@Serializable
data class GeoResponse(val features: List<GeoFeature> = emptyList())

@Serializable
data class GeoFeature(
    val id: String = "",
    val text: String = "",
    @SerialName("place_name") val placeName: String = "",
    @SerialName("place_type") val placeType: List<String> = emptyList(),
    /** [경도, 위도] */
    val center: List<Double> = emptyList(),
    val context: List<GeoContext> = emptyList(),
) {
    val kind: String get() = placeType.firstOrNull() ?: id.substringBefore('.')

    /**
     * "서울특별시 영등포구 여의공원로" — 시도 · 구 · 길 이름. 길 이름을 모르면 동 이름을 쓴다.
     * 이 장소의 이름과 같은 것은 뺀다.
     */
    fun address(): String {
        fun level(name: String) = context.firstOrNull { it.id.substringBefore('.') == name }?.text?.trim().orEmpty()
        val street = level("address").ifEmpty { level("place") }.ifEmpty { level("municipality") }
        return listOf(level("region"), level("county"), level("municipal_district"), street)
            .filter { it.isNotEmpty() && it != text.trim() }
            .distinct()
            .joinToString(" ")
    }

    /** 이 장소의 좌표. 없거나 범위를 벗어나면 null */
    fun centerPoint(): GeoPoint? {
        val lng = center.getOrNull(0) ?: return null
        val lat = center.getOrNull(1) ?: return null
        if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return null
        return GeoPoint(lat, lng)
    }

    fun toPlace(): StoryPlace? {
        val point = centerPoint() ?: return null
        val name = text.trim().takeIf { it.isNotEmpty() } ?: return null
        return StoryPlace(name.take(80), address().take(120), point.lat, point.lng)
    }
}

@Serializable
data class GeoContext(val id: String = "", val text: String = "")
