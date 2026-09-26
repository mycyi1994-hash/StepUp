package com.stepup.android.core

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.stepup.android.domain.GeoPoint

/**
 * 앱 밖으로 나가는 인텐트들.
 *
 * 지도는 구글 지도를 먼저 노리고, 없으면 다른 지도 앱, 그것도 없으면
 * 브라우저로 구글 지도 웹을 연다. 어느 단계에서 실패해도 앱이 죽지 않는다.
 */
object ExternalIntents {

    fun openAppSettings(context: Context): Boolean =
        start(context, Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}"))) ||
            start(context, Intent(android.provider.Settings.ACTION_SETTINGS))

    /** 휴대폰 위치 설정(위치 켜기) — 없으면 앱 설정으로 */
    fun openLocationSettings(context: Context): Boolean =
        start(context, Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)) ||
            openAppSettings(context)

    private const val GOOGLE_MAPS = "com.google.android.apps.maps"

    /** 장소 이름으로 구글 지도를 연다. 이름이 비어 있으면 아무것도 하지 않는다. */
    fun openPlaceInMaps(context: Context, place: String) {
        val query = place.trim()
        if (query.isEmpty()) return
        val encoded = Uri.encode(query)

        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$encoded"))
        // 구글 지도가 깔려 있으면 바로 그쪽으로
        if (start(context, Intent(geo).setPackage(GOOGLE_MAPS))) return
        // 아니면 지도 인텐트를 받을 수 있는 아무 앱
        if (start(context, geo)) return
        // 마지막 수단 — 브라우저
        start(
            context,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=$encoded"),
            ),
        )
    }

    /**
     * 달린 경로를 구글 지도에서 연다.
     *
     * 앱 안 지도는 OpenStreetMap 타일이라 "어느 길이었나"까지는 보여주지만,
     * 확대해서 뜯어보거나 길 안내를 받는 건 구글 지도가 낫다. 그래서 경로를
     * 통째로 넘긴다 — 출발지, 도착지, 그리고 사이의 경유지 몇 개.
     *
     * 구글 지도 URL은 경유지를 무한정 받지 않으므로 [MAX_WAYPOINTS]개만 고르게
     * 솎아낸다. 경로의 모양을 알아볼 수 있으면 충분하다.
     */
    fun openRouteInMaps(context: Context, points: List<GeoPoint>) {
        if (points.size < 2) {
            points.firstOrNull()?.let { openCoordinateInMaps(context, it) }
            return
        }
        val origin = points.first()
        val destination = points.last()
        val middle = points.drop(1).dropLast(1)
        val step = maxOf(1, middle.size / MAX_WAYPOINTS)
        val waypoints = middle.filterIndexed { index, _ -> index % step == 0 }
            .take(MAX_WAYPOINTS)
            .joinToString("|") { "${it.lat},${it.lng}" }

        val url = buildString {
            append("https://www.google.com/maps/dir/?api=1")
            append("&origin=").append(origin.lat).append(',').append(origin.lng)
            append("&destination=").append(destination.lat).append(',').append(destination.lng)
            if (waypoints.isNotEmpty()) append("&waypoints=").append(Uri.encode(waypoints))
            append("&travelmode=walking")
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        if (start(context, Intent(intent).setPackage(GOOGLE_MAPS))) return
        start(context, intent)
    }

    /** 좌표 한 점을 지도에서 연다 */
    fun openCoordinateInMaps(context: Context, point: GeoPoint) {
        val geo = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${point.lat},${point.lng}?z=17"))
        if (start(context, Intent(geo).setPackage(GOOGLE_MAPS))) return
        if (start(context, geo)) return
        start(
            context,
            Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://www.google.com/maps/search/?api=1&query=${point.lat},${point.lng}"),
            ),
        )
    }

    /** 구글 지도 URL이 안정적으로 받아주는 경유지 수 */
    private const val MAX_WAYPOINTS = 8

    /**
     * 웹 주소를 연다.
     *
     * 먼저 **커스텀 탭**을 쓴다. 앱 위에 브라우저가 얹히는 방식이라 닫으면
     * 보던 화면이 그대로 남는다 — 필터와 스크롤을 다시 맞출 필요가 없다.
     * 커스텀 탭을 지원하는 브라우저가 없으면 평범한 브라우저로 넘긴다.
     *
     * 기사 본문과 대회 안내는 원문에서 읽는다. 앱 안에 iframe 으로 통째로
     * 옮겨 담지 않는다 — 남의 화면을 우리 것처럼 두는 셈이고, 로그인과
     * 결제가 그 안에서 제대로 되지도 않는다.
     *
     * [SafeUrl] 을 지나지 못하는 주소는 조용히 무시한다. javascript: 하나가
     * 열리면 그 카드를 누른 사용자가 위험해진다.
     */
    fun openUrl(context: Context, url: String) {
        if (!SafeUrl.looksSafe(url)) return
        val uri = Uri.parse(SafeUrl.preferHttps(url))
        val tab = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()
        tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            tab.launchUrl(context, uri)
            return
        } catch (e: Exception) {
            // 커스텀 탭을 받아 줄 브라우저가 없는 기기가 있다.
        }
        start(context, Intent(Intent.ACTION_VIEW, uri))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
