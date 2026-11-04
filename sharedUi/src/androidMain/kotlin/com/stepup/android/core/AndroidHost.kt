package com.stepup.android.core

import android.annotation.SuppressLint
import android.content.Context

/**
 * 공용 화면이 안드로이드 기기 일을 할 때 쓰는 앱 컨텍스트(2026-11-03 C8) — 앱이 켜질 때 한 번 [init] 한다(StepUpApp.onCreate).
 * 화면 안의 일(권한 창 · 사진 고르기)은 LocalContext 를 쓰고, 화면 밖의 일(설정 열기 · 공유 · 파일)만 이 컨텍스트를 쓴다.
 */
@SuppressLint("StaticFieldLeak")
object AndroidHost {
    lateinit var app: Context
        private set

    fun init(context: Context) {
        app = context.applicationContext
    }
}
