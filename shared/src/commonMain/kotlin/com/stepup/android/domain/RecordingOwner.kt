package com.stepup.android.domain

/** Never infer historical ownership from whoever happens to be signed in now. */
object RecordingOwner {
    const val LEGACY = "legacy"
    const val GUEST = "guest"
    fun account(userId: String): String = "account:$userId"
    fun userId(owner: String): String? = owner.takeIf { it.startsWith("account:") }
        ?.removePrefix("account:")?.takeIf { it.isNotBlank() }
}
