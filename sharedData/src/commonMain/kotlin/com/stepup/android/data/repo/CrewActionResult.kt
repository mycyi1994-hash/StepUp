package com.stepup.android.data.repo

/** 크루에 무언가를 했을 때의 결말 */
sealed interface CrewActionResult {
    data object Joined : CrewActionResult
    data object Requested : CrewActionResult
    data class Created(val crewId: String) : CrewActionResult
    data object Done : CrewActionResult

    /**
     * @param signIn 로그인해야 할 수 있는 일이었다
     * @param retryable 연결·서버 문제라 나중에 다시 하면 될 수 있다
     */
    data class Failed(
        val reason: String,
        val signIn: Boolean = false,
        val retryable: Boolean = false,
    ) : CrewActionResult
}
