package com.stepup.android.ui

/** The only source of tab ownership and shared-chrome visibility. */
object AppChromePolicy {
    enum class Header { Main, Detail, Focus, Form }

    data class Destination(val route: String, val parent: Screen, val header: Header) {
        val showBottomBar: Boolean get() = header == Header.Main || header == Header.Detail
    }

    val tabs: List<Screen> = listOf(Screen.Run, Screen.Customize, Screen.Community, Screen.Profile)

    val destinations: List<Destination> = listOf(
        Destination(Screen.Run.route, Screen.Run, Header.Main),
        Destination(Screen.Customize.route, Screen.Customize, Header.Main),
        Destination(Screen.Community.route, Screen.Community, Header.Main),
        Destination(Screen.Profile.route, Screen.Profile, Header.Main),
        Destination(Routes.MYSTERY_BOX, Screen.Customize, Header.Main),
        Destination(Routes.RUN_ROUTE, Screen.Run, Header.Focus),
        Destination(Routes.NEWS, Screen.Run, Header.Detail),
        Destination(Routes.EVENTS, Screen.Run, Header.Detail),
        Destination(Routes.COURSES, Screen.Run, Header.Detail),
        Destination(Routes.RUNNER_MARKET, Screen.Customize, Header.Detail),
        Destination(Routes.ITEMS, Screen.Customize, Header.Detail),
        Destination(Routes.SNEAKER_DEX, Screen.Customize, Header.Detail),
        Destination(Routes.SNEAKER, Screen.Customize, Header.Detail),
        Destination(Routes.MARKET_MODEL, Screen.Customize, Header.Detail),
        Destination(Routes.MAP, Screen.Community, Header.Detail),
        Destination(Routes.RANKING, Screen.Community, Header.Detail),
        Destination(Routes.CREW_BOARD, Screen.Community, Header.Detail),
        Destination(Routes.LOBBY, Screen.Community, Header.Detail),
        Destination(Routes.FLASH_DETAIL, Screen.Community, Header.Detail),
        Destination(Routes.FLASH_LOBBY, Screen.Community, Header.Detail),
        Destination(Routes.CREW_CREATE, Screen.Community, Header.Form),
        Destination(Routes.POST_COMPOSE, Screen.Community, Header.Form),
        Destination(Routes.WALLET, Screen.Profile, Header.Detail),
        Destination(Routes.NOTIFICATIONS, Screen.Profile, Header.Detail),
        Destination(Routes.ACHIEVEMENTS, Screen.Profile, Header.Detail),
        Destination(Routes.ANALYTICS, Screen.Profile, Header.Detail),
        Destination(Routes.HISTORY_MAP, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_NOTIFICATIONS, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_PRIVACY, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_SUPPORT, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_CONNECTED, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_LANGUAGE, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_EXPERIENCE, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_THEME, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_BODY, Screen.Profile, Header.Detail),
        Destination(Routes.SETTINGS_MODE, Screen.Profile, Header.Detail),
        Destination(Routes.INVITE, Screen.Profile, Header.Detail),
    )

    /** Accept both NavHost route templates and concrete/deep-link route values. */
    fun destination(route: String?): Destination? {
        if (route == null) return null
        val path = route.substringBefore('?')
        destinations.firstOrNull { it.route.substringBefore('?') == path }?.let { return it }
        val segments = path.split('/')
        return destinations.firstOrNull { entry ->
            val pattern = entry.route.substringBefore('?').split('/')
            pattern.size == segments.size && pattern.zip(segments).all { (part, value) ->
                part == value || (part.startsWith('{') && part.endsWith('}') && value.isNotBlank())
            }
        }
    }
}
