package com.stepup.android.data.prefs

import platform.Foundation.NSFileManager

actual fun fileExists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)
