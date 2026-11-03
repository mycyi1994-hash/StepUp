// Top-level build file where you can add configuration options common to all sub-projects/modules.

// 축소기 R8 을 AGP 8.10 에 딸린 판보다 새 판으로 — 그 판은 Kotlin 2.3 메타데이터를 읽지 못해
// 릴리스 · qa 빌드에서 "error occurred when parsing kotlin metadata" 경고를 수백 줄 냈다(2026-11-03).
// AGP 를 8.13 이상으로 올리면 이 줄을 지운다.
buildscript {
    repositories { google() }
    dependencies { classpath("com.android.tools:r8:9.0.55") }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
}
