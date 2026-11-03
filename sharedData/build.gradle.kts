import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// 폰 DB(Room KMP, 2026-11-03 — docs/ios/README.md "데이터"). 표 · DAO · AppDatabase 는 commonMain 하나를 안드로이드 · iOS 가 같이 쓴다.
// 안드로이드는 예전과 똑같이 연다(SupportSQLite · 파일 strideup.db · 판 19 · 옛 판에서 올리는 길 MIGRATIONS — androidMain, 글자 그대로 옮겼다).
// iOS 는 새로 깐 앱이라 판 19 에서 시작한다(옮길 옛 DB 가 없다) — 번들 SQLite 드라이버.
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room)
    // 설정 저장소(UserPrefs)의 이야기 · 크루 초안 JSON
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    // iOS 시뮬레이터 검사 기기 — :shared 와 같게(CI 는 UDID)
    val testDevice = providers.gradleProperty("stepup.iosTestDevice").getOrElse("iPhone 17")
    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests>().configureEach {
        testRuns.named("test") { deviceId = testDevice }
    }
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
            api(libs.androidx.room.runtime)
            // 설정(UserPrefs) — 안드로이드는 예전 파일 strideup_prefs · stepup_auth 그대로(app 의 AndroidUserPrefs.kt)
            api(libs.androidx.datastore.preferences.core)
        }
        iosMain.dependencies {
            implementation(libs.androidx.sqlite.bundled)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.stepup.android.data"
    compileSdk = 36
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// 표 모양 기록(판마다 JSON) — 옛 판에서 올리는 검사(DatabaseMigrationTest)가 읽는다. 예전 app/schemas 를 옮겼다
room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}
