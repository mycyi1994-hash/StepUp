package com.stepup.android.data.repo

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import com.stepup.android.core.AppLocale
import com.stepup.android.domain.GeoPoint
import com.stepup.android.domain.StoryPlace
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** 장소 찾기 언어 — 앱이 고른 언어, 없으면 기기 언어(지원 넷 밖이면 한국어). 예전 PlaceSearch.appLanguage */
fun placeSearchLanguage(): String =
    AppLocale.tag.ifEmpty { Locale.getDefault().language }.takeIf { it in AppLocale.SUPPORTED } ?: "ko"

class AndroidGeocoder(private val context: Context) : PlatformGeocoder {
    override suspend fun search(query: String, near: GeoPoint?, limit: Int): List<StoryPlace>? {
        if (!Geocoder.isPresent()) return emptyList()
        val geocoder = Geocoder(context, Locale.forLanguageTag(placeSearchLanguage()))
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
