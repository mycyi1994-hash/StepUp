package com.stepup.android.ui.screens.feed

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.stepup.android.R
import com.stepup.android.data.remote.EventRow
import com.stepup.android.data.remote.NewsRow
import com.stepup.android.data.repo.EventFilter
import com.stepup.android.data.repo.NewsFilter
import java.time.LocalDate
import java.time.OffsetDateTime

/**
 * 데모 모드의 소식 — 서버 없이 화면을 둘러보기 위한 예시.
 *
 * ── 지키는 것 ──
 *
 *   * 모두 "예시"라고 적힌다(카드에 표시가 붙는다). 진짜 대회로 읽히면 안 된다.
 *   * 바깥 링크가 없다. 없는 대회의 신청 페이지를 지어내지 않는다.
 *   * id 가 [DEMO_PREFIX] 로 시작한다. 관심 저장 같은 서버 요청은 이 id 를
 *     보고 보내지 않는다 — 예시가 운영 데이터에 섞이지 않게.
 *   * 날짜는 오늘을 기준으로 앞으로 잡는다. 지난 날짜면 "지난 대회"로 빠진다.
 */
const val DEMO_PREFIX = "demo-"

fun isDemo(id: String): Boolean = id.startsWith(DEMO_PREFIX)

@Composable
fun demoEvents(filter: EventFilter): List<EventRow> {
    val today = LocalDate.now()
    val verified = OffsetDateTime.now().toString()
    val all = listOf(
        EventRow(
            id = "${DEMO_PREFIX}e1",
            title = stringResource(R.string.demo_event_1),
            region = "서울",
            venue = stringResource(R.string.demo_event_1_venue),
            eventDate = today.plusDays(19).toString(),
            eventType = "ROAD",
            registrationStatus = "UPCOMING",
            lastVerifiedAt = verified,
            distances = listOf("LTE_5K"),
            disciplineNames = listOf("5km"),
        ),
        EventRow(
            id = "${DEMO_PREFIX}e2",
            title = stringResource(R.string.demo_event_2),
            region = "서울",
            venue = stringResource(R.string.demo_event_2_venue),
            eventDate = today.plusDays(40).toString(),
            eventType = "ROAD",
            registrationStatus = "OPEN",
            lastVerifiedAt = verified,
            distances = listOf("HALF"),
            disciplineNames = listOf("21.1km"),
        ),
        EventRow(
            id = "${DEMO_PREFIX}e3",
            title = stringResource(R.string.demo_event_3),
            region = "서울",
            venue = stringResource(R.string.demo_event_3_venue),
            eventDate = today.plusDays(61).toString(),
            eventType = "FUNRUN",
            registrationStatus = "UPCOMING",
            lastVerifiedAt = verified,
            distances = listOf("10K"),
            disciplineNames = listOf("10km"),
        ),
    )
    // 예시에서도 거르기가 동작해야 거르기 화면을 확인할 수 있다
    return all.filter { row ->
        (filter.eventType == "ALL" || row.eventType == filter.eventType) &&
            (filter.region == "ALL" || row.region == filter.region) &&
            (filter.distance == "ALL" || filter.distance in row.distances) &&
            (filter.status == "ALL" || row.registrationStatus == filter.status) &&
            (filter.query.isBlank() || row.title.contains(filter.query, ignoreCase = true))
    }.let { list -> if (filter.sort == "NEWEST") list.reversed() else list }
}

@Composable
fun demoNews(filter: NewsFilter): List<NewsRow> {
    val now = OffsetDateTime.now()
    val all = listOf(
        NewsRow(
            id = "${DEMO_PREFIX}n1",
            title = stringResource(R.string.demo_news_1),
            publisherName = stringResource(R.string.demo_publisher),
            originalUrl = "",
            publishedAt = now.minusHours(3).toString(),
            category = "TRAINING",
        ),
        NewsRow(
            id = "${DEMO_PREFIX}n2",
            title = stringResource(R.string.demo_news_2),
            publisherName = stringResource(R.string.demo_publisher),
            originalUrl = "",
            publishedAt = now.minusDays(1).toString(),
            category = "RUNNING",
        ),
    )
    return all.filter { row ->
        (filter.category == "ALL" || row.category == filter.category) &&
            (filter.query.isBlank() || row.title.contains(filter.query, ignoreCase = true))
    }
}
