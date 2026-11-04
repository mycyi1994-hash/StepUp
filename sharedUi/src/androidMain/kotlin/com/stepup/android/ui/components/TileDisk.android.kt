package com.stepup.android.ui.components

import java.io.File

/**
 * 안드로이드 타일 디스크 캐시(예전 MapTiles 그대로 — cacheDir/tiles/<출처>/<줌>/<x>_<y>.png, 30MB 를 넘으면 오래된 것부터 지운다).
 */
class AndroidTileDisk(private val root: File) : TileDisk {
    private var writesSincePrune = 0

    override fun read(path: String): ByteArray? = File(root, path).takeIf { it.exists() }?.readBytes()

    override fun write(path: String, bytes: ByteArray) {
        val file = File(root, path)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
        prune()
    }

    @Synchronized
    private fun prune() {
        if (++writesSincePrune < PRUNE_EVERY) return
        writesSincePrune = 0
        val files = root.walkTopDown().filter { it.isFile }.toMutableList()
        var total = files.sumOf { it.length() }
        if (total <= DISK_BUDGET_BYTES) return
        files.sortBy { it.lastModified() }
        for (f in files) {
            if (total <= DISK_BUDGET_BYTES) break
            val size = f.length()
            if (f.delete()) total -= size
        }
    }

    private companion object {
        const val DISK_BUDGET_BYTES = 30L * 1024 * 1024
        const val PRUNE_EVERY = 40
    }
}
