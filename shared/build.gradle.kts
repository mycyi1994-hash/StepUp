import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

// 안드로이드 · iOS 가 함께 쓰는 순수 규칙(거리 · 페이스 · 경고 · 경제 …) — docs/ios/README.md.
// 안드로이드 앱은 jvm 판을 쓰고(:app 의 implementation(project(":shared"))), iOS 앱은 :sharedUi 의 StepUpKit 틀로 받는다.
// 여기에는 안드로이드 · iOS 어느 쪽 API 도 들이지 않는다 — 기기마다 다른 일은 앱 쪽에 둔다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm {
        // 안드로이드 앱과 같은 바이트코드 판(app/build.gradle.kts 의 jvmTarget = "17")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    iosArm64()
    iosSimulatorArm64()

    // iOS 시뮬레이터 검사 기기 — Xcode 16 의 기본 시뮬레이터. 다른 기기는 -Pstepup.iosTestDevice="iPhone 16 Pro"
    val testDevice = providers.gradleProperty("stepup.iosTestDevice").getOrElse("iPhone 16")
    targets.withType<KotlinNativeTargetWithSimulatorTests>().configureEach {
        testRuns.named("test") { deviceId = testDevice }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }
}
