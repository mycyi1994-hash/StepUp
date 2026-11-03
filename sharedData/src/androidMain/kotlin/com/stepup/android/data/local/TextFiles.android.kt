package com.stepup.android.data.local

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

actual object TextFiles {
    actual fun exists(path: String): Boolean = File(path).exists()

    actual fun read(path: String): String? = File(path).takeIf { it.exists() }?.let { runCatching { it.readText() }.getOrNull() }

    actual fun writeAtomic(path: String, text: String) {
        val target = File(path)
        target.parentFile?.mkdirs()
        val temp = File(target.parentFile, target.name + ".tmp")
        temp.writeText(text)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    actual fun delete(path: String): Boolean = File(path).delete()

    actual fun mkdirs(dir: String) {
        File(dir).mkdirs()
    }
}
