// iOS 앱 화면(Compose Multiplatform) — docs/ios/README.md. Xcode 앱(iosApp/)은 이 모듈이 만드는 StepUpKit 틀 하나만 붙인다.
// 지금은 iOS 대상만 둔다. 안드로이드 화면을 이리 옮길 때 androidTarget() 을 더한다(그 전까지 안드로이드 앱 화면은 :app 그대로).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
    // 장소 목록(spots.json) 읽기
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "StepUpKit"
            isStatic = true
            // Swift 에서 공용 규칙(:shared)도 바로 부른다
            export(project(":shared"))
        }
    }

    sourceSets {
        iosMain.dependencies {
            // 지도 타일 받기(TileFetch.ios.kt)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.darwin)
        }
        commonMain.dependencies {
            api(project(":shared"))
            // 폰 DB(Room KMP) — iOS 는 buildIosDatabase()
            implementation(project(":sharedData"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            // 화면 상태(ViewModel) · 수명 주기 — 안드로이드와 같은 androidx.lifecycle(멀티플랫폼 판, CMP 가 끌어오는 판)
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
        }
    }
}

// 화면 글자(4개 언어) · 글꼴은 안드로이드 app/src/main/res 가 원본이다 — 빌드할 때 composeResources 로 만든다(저장소에 사본을 두지 않는다).
// 로고처럼 iOS 에만 있는 그림은 src/commonMain/composeResources 에 두고 함께 담는다. tools/sync_compose_resources.py
val stepupResources = layout.buildDirectory.dir("generated/stepupComposeResources")
val syncStepUpResources by tasks.registering(Exec::class) {
    val res = rootProject.layout.projectDirectory.dir("app/src/main/res")
    val static = layout.projectDirectory.dir("src/commonMain/composeResources")
    val script = rootProject.layout.projectDirectory.file("tools/sync_compose_resources.py")
    inputs.files(fileTree(res) { include("values*/**", "font/**", "drawable/**", "drawable-nodpi/**") })
    inputs.dir(static)
    inputs.dir(rootProject.layout.projectDirectory.dir("app/src/main/assets"))
    inputs.file(script)
    outputs.dir(stepupResources)
    commandLine("python3", script.asFile.path, res.asFile.path, stepupResources.get().asFile.path, static.asFile.path)
}

compose.resources {
    packageOfResClass = "com.stepup.ui.res"
    // 바깥에서 쓸 수 있게(안드로이드 앱이 같은 화면을 쓸 때)
    publicResClass = true
    customDirectory(sourceSetName = "commonMain", directoryProvider = syncStepUpResources.map { stepupResources.get() })
}
