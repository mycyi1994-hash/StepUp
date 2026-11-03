package com.stepup.android.data.prefs

actual fun fileExists(path: String): Boolean = java.io.File(path).exists()
