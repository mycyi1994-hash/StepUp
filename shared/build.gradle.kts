import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 안드로이드 · 아이폰이 함께 쓰는 코드(Kotlin Multiplatform) — docs/ios-plan-2026-11-04.md
//
// commonMain: 두 앱이 같은 한 벌로 쓰는 규칙(거리 · 과속 · 내구도 · 뽑기 · 코칭 …). 서버와 맞대어 본 규칙이라 둘로 나누지 않는다.
// androidMain: 아직 java.time 을 쓰는 규칙(옮기는 중) + 안드로이드 쪽 Platform 구현 — 예전 Java 함수를 그대로 불러 동작이 같다.
// iosMain: 아이폰 쪽 Platform 구현. 리눅스(CI)에서는 아이폰 컴파일을 건너뛴다 — Mac 에서만 돈다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        androidUnitTest.dependencies {
            implementation(libs.junit)
        }
    }
}

android {
    namespace = "com.stepup.shared"
    compileSdk = 36
    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
