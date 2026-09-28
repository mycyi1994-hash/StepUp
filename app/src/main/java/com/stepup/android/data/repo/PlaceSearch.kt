package com.stepup.android.data.repo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.stepup.android.core.AppLocale
import com.stepup.android.data.remote.PlaceSearchApi
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryPlace
import com.stepup.android.domain.haversineMeters
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 장소 검색의 결말 */
sealed interface PlaceSearchResult {
    data class Found(val places: List<StoryPlace>) : PlaceSearchResult

    /** 검색하는 곳에 닿지 못했다 — "찾는 장소가 없어요"와 섞지 않는다 */
    data object Offline : PlaceSearchResult
}

/** 기기에 들어 있는 지오코더(구글 플레이 서비스가 있는 폰). 없으면 쓰지 않는다 */
interface PlatformGeocoder {
    suspend fun search(query: String, near: GeoPoint?, limit: Int): List<StoryPlace>?
}

/**
 * 글에 붙일 장소 찾기.
 *
 * 먼저 지도 타일과 같은 MapTiler 로 찾고, 결과가 적으면 기기의 지오코더로 보탠다. 한국 공원 ·
 * 시설 이름은 MapTiler(OpenStreetMap)에 빠진 것이 있어서다. 둘 다 새 계약이 필요 없다.
 * 어느 쪽도 못 찾으면 지어내지 않고 빈 결과를 돌려준다 — 화면이 "지도에서 장소 보기"를 권한다.
 */
class PlaceSearch(
    private val api: PlaceSearchApi,
    private val platform: PlatformGeocoder?,
    private val language: () -> String = ::appLanguage,
) {
    suspend fun search(query: String, near: GeoPoint?): PlaceSearchResult {
        val text = query.trim()
        if (text.isEmpty()) return PlaceSearchResult.Found(emptyList())
        val primary = api.search(text, near, language())
        val fromDevice = if ((primary?.size ?: 0) < ENOUGH) platform?.search(text, near, ENOUGH) else emptyList()
        if (primary == null && fromDevice == null) return PlaceSearchResult.Offline
        // MapTiler 는 이미 [near] 가까이를 앞에 둔 관련도 순이다 — 그 순서를 지키고 기기 결과를 뒤에 붙인다
        return PlaceSearchResult.Found((primary.orEmpty() + fromDevice.orEmpty()).distinctPlaces())
    }

    /** 지역(동 · 구) 찾기 — 위치 권한 없이 "선택한 지역에서" 볼 때 */
    suspend fun regions(query: String, near: GeoPoint?): PlaceSearchResult {
        val text = query.trim()
        if (text.isEmpty()) return PlaceSearchResult.Found(emptyList())
        val found = api.regions(text, near, language()) ?: return PlaceSearchResult.Offline
        return PlaceSearchResult.Found(found.distinctPlaces())
    }

    /** 지도에서 누른 자리의 이름 */
    suspend fun nameAt(point: GeoPoint): StoryPlace? = api.nameAt(point, language())

    /** 목록 위의 동네 이름 */
    suspend fun areaName(point: GeoPoint): String? = api.areaName(point, language())

    /** 이 좌표가 속한 동네의 이름과 중심점 — 크루 활동 지역처럼 남에게 보일 자리 */
    suspend fun area(point: GeoPoint): StoryPlace? = api.area(point, language())

    private fun List<StoryPlace>.distinctPlaces(): List<StoryPlace> {
        val kept = mutableListOf<StoryPlace>()
        for (place in this) {
            // 이름이 같고 100m 안이면 같은 곳 — 두 검색원이 같은 공원을 조금 다른 좌표로 준다
            if (kept.none { it.name == place.name && haversineMeters(it.point, place.point) < 100 }) kept += place
        }
        return kept
    }

    companion object {
        private const val ENOUGH = 3

        fun appLanguage(): String =
            AppLocale.tag.ifEmpty { Locale.getDefault().language }.takeIf { it in AppLocale.SUPPORTED } ?: "ko"
    }
}

/** 안드로이드 지오코더 — 구글 플레이 서비스가 있는 폰에서만 동작한다 */
class AndroidGeocoder(private val context: Context) : PlatformGeocoder {
    override suspend fun search(query: String, near: GeoPoint?, limit: Int): List<StoryPlace>? {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(context, Locale.forLanguageTag(PlaceSearch.appLanguage()))
        val addresses = withTimeoutOrNull(6_000) { lookup(geocoder, query, near, limit) } ?: return null
        return addresses.mapNotNull { it.toPlace() }
    }

    private suspend fun lookup(geocoder: Geocoder, query: String, near: GeoPoint?, limit: Int): List<Address>? {
        // 가까이를 먼저 — 대략 ±0.5도(수십 km) 안에서 찾는다. 위치를 모르면 전국에서
        val box = near?.let { doubleArrayOf(it.lat - 0.5, it.lng - 0.5, it.lat + 0.5, it.lng + 0.5) }
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            suspendCancellableCoroutine { continuation ->
                val listener = object : Geocoder.GeocodeListener {
                    override fun onGeocode(addresses: MutableList<Address>) {
                        if (continuation.isActive) continuation.resume(addresses.toList())
                    }

                    override fun onError(errorMessage: String?) {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }
                runCatching {
                    if (box != null) geocoder.getFromLocationName(query, limit, box[0], box[1], box[2], box[3], listener)
                    else geocoder.getFromLocationName(query, limit, listener)
                }.onFailure { if (continuation.isActive) continuation.resume(null) }
            }
        } else {
            withContext(Dispatchers.IO) {
                @Suppress("DEPRECATION")
                runCatching {
                    if (box != null) geocoder.getFromLocationName(query, limit, box[0], box[1], box[2], box[3])
                    else geocoder.getFromLocationName(query, limit)
                }.getOrNull()
            }
        }
    }

    private fun Address.toPlace(): StoryPlace? {
        if (!hasLatitude() || !hasLongitude()) return null
        // 번지(숫자)만 있는 이름은 쓰지 않는다. 시설 이름이 없으면 길 · 동네 이름 — 검색한 말을 이름처럼 붙이지 않는다
        val name = listOfNotNull(featureName, premises, thoroughfare, subLocality, locality)
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() && it.any(Char::isLetter) }
            ?: return null
        val line = getAddressLine(0).orEmpty().trim()
        val address = countryName?.let { line.removePrefix(it).trim() } ?: line
        return StoryPlace(name.take(80), address.take(120), latitude, longitude)
    }
}
