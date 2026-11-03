import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests

// 안드로이드 · iOS 가 함께 쓰는 순수 규칙(거리 · 페이스 · 경고 · 경제 …) — docs/ios/README.md.
// 안드로이드 앱은 jvm 판을 쓰고(:app 의 implementation(project(":shared"))), iOS 앱은 :sharedUi 의 StepUpKit 틀로 받는다.
// 여기에는 안드로이드 · iOS 어느 쪽 API 도 들이지 않는다 — 기기마다 다른 일은 앱 쪽에 둔다.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    // 뽑기 확률 표(DrawOdds.parse)가 서버 JSON 을 읽는다
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvm {
        // 안드로이드 앱과 같은 바이트코드 판(app/build.gradle.kts 의 jvmTarget = "17")
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    iosArm64()
    iosSimulatorArm64()

    // iOS 시뮬레이터 검사 기기 — Xcode 26 의 기본 시뮬레이터. 다른 기기는 -Pstepup.iosTestDevice="iPhone 17 Pro"(이름이나 UDID — CI 는 UDID)
    val testDevice = providers.gradleProperty("stepup.iosTestDevice").getOrElse("iPhone 17")
    targets.withType<KotlinNativeTargetWithSimulatorTests>().configureEach {
        testRuns.named("test") { deviceId = testDevice }
    }

    sourceSets {
        commonMain.dependencies {
            // 날짜 타입이 공용 규칙의 공개 API 에 나온다(앱이 받는다)
            api(libs.kotlinx.datetime)
            // 러닝 계획(RunPlan)의 상태 흐름
            api(libs.kotlinx.coroutines.core)
            // 서버 응답 칸(@Serializable) · JSON 이 서버 호출(data/remote)의 공개 API 에 나온다
            api(libs.kotlinx.serialization.core)
            api(libs.kotlinx.serialization.json)
        }
        iosMain.dependencies {
            // iOS 의 HTTP — 안드로이드(jvm)는 HttpURLConnection 그대로(UrlConnectionPoster)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
