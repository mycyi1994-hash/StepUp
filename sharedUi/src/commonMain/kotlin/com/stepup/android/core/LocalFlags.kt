package com.stepup.android.core

/** 이 폰에만 남기는 작은 표시(안드로이드 SharedPreferences · iOS NSUserDefaults) — 다시 보지 않을 안내 같은 것 */
expect object LocalFlags {
    fun getInt(file: String, key: String, default: Int): Int
    fun putInt(file: String, key: String, value: Int)
}

/** android.text.format.DateFormat.getBestDateTimePattern 중 앱이 쓰는 뼈대(yMd · MMMd · MMMMdEEEE · Md)를 네 언어로 */
fun bestDatePattern(language: String, skeleton: String): String {
    val lang = language.lowercase().substringBefore('-').substringBefore('_')
    return when (skeleton) {
        "yMd" -> when (lang) { "ko" -> "y. M. d."; "ja", "zh" -> "y/M/d"; else -> "M/d/y" }
        "MMMd" -> when (lang) { "ko" -> "MMM d일"; "ja", "zh" -> "M月d日"; else -> "MMM d" }
        "MMMMdEEEE" -> when (lang) { "ko" -> "MMMM d일 EEEE"; "ja", "zh" -> "M月d日EEEE"; else -> "EEEE, MMMM d" }
        "Md" -> when (lang) { "ko" -> "M. d."; "ja", "zh" -> "M/d"; else -> "M/d" }
        else -> skeleton
    }
}
