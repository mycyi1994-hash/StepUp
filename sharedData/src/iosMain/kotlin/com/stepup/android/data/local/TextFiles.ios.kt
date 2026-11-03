@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.stepup.android.data.local

import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.stringWithContentsOfFile
import platform.Foundation.writeToFile

actual object TextFiles {
    actual fun exists(path: String): Boolean = NSFileManager.defaultManager.fileExistsAtPath(path)

    actual fun read(path: String): String? =
        if (!exists(path)) null else NSString.stringWithContentsOfFile(path, encoding = NSUTF8StringEncoding, error = null)

    actual fun writeAtomic(path: String, text: String) {
        mkdirs(path.substringBeforeLast('/'))
        @Suppress("CAST_NEVER_SUCCEEDS")
        val ok = (text as NSString).writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        if (!ok) throw IllegalStateException("파일을 쓰지 못했습니다: $path")
    }

    actual fun delete(path: String): Boolean = NSFileManager.defaultManager.removeItemAtPath(path, error = null)

    actual fun mkdirs(dir: String) {
        NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    }
}
