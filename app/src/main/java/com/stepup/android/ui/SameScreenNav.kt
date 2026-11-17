package com.stepup.android.ui

import androidx.navigation.NavController

/**
 * 같은 화면(길 모양)이 맨 위에 있을 때 다른 방 · 다른 글을 여는 법(점검 2026-11-17 #6).
 *
 * `launchSingleTop` 만 쓰면 길 모양이 같을 때 맨 위 화면을 그대로 두고 값만 바꾼다 — 뷰모델은 처음 받은 id 를
 * 계속 써서 크루 A 채팅을 보다가 크루 B 채팅 푸시를 눌러도 A 가 남았다. 같은 id 면 그대로 두고(겹쳐 쌓지 않는다),
 * 다른 id 면 맨 위를 걷고 새로 연다.
 */
object SameScreenNav {
    enum class Step {
        /** 이미 그 방 · 그 글이 맨 위 — 아무것도 하지 않는다 */
        STAY,

        /** 같은 화면에 다른 방 · 다른 글 — 맨 위를 걷고 연다 */
        REPLACE,

        /** 맨 위가 다른 화면 — 그냥 연다 */
        OPEN,
    }

    fun step(
        topPattern: String?,
        topArgs: Map<String, String?>,
        pattern: String,
        args: Map<String, String>,
    ): Step = when {
        topPattern != pattern -> Step.OPEN
        args.all { (k, v) -> topArgs[k] == v } -> Step.STAY
        else -> Step.REPLACE
    }
}

/** [route] 를 연다 — [pattern] 은 그 길의 모양, [args] 는 그 길에 든 값(맨 위 화면의 값과 맞대어 본다). */
fun NavController.openReplacingSame(route: String, pattern: String, args: Map<String, Any>) {
    val top = currentBackStackEntry
    val wanted = args.mapValues { it.value.toString() }
    @Suppress("DEPRECATION")
    val topArgs = wanted.keys.associateWith { key -> top?.arguments?.get(key)?.toString() }
    when (SameScreenNav.step(top?.destination?.route, topArgs, pattern, wanted)) {
        SameScreenNav.Step.STAY -> Unit
        SameScreenNav.Step.REPLACE -> {
            popBackStack()
            navigate(route) { launchSingleTop = true }
        }
        SameScreenNav.Step.OPEN -> navigate(route) { launchSingleTop = true }
    }
}
