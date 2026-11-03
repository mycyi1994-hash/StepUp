package com.stepup.android.data.local

/**
 * 앱 파일 읽고 쓰기(java.io.File 자리, 2026-11-03 iOS C) — 저장소 사본(공지 · 코칭)이 쓴다. 경로는 앱 저장 폴더 안 절대 경로.
 * 쓰기는 다 쓴 뒤 바꿔 끼운다(반쯤 쓴 사본이 남지 않게). 못 쓰면 예외.
 */
expect object TextFiles {
    fun exists(path: String): Boolean
    /** 없거나 읽지 못하면 null */
    fun read(path: String): String?
    fun writeAtomic(path: String, text: String)
    fun delete(path: String): Boolean
    fun mkdirs(dir: String)
}

/** 경로 잇기 */
fun joinPath(dir: String, name: String): String = if (dir.endsWith("/")) dir + name else "$dir/$name"
