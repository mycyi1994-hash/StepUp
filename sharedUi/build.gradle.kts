// iOS 앱 화면(Compose Multiplatform) — docs/ios/README.md. Xcode 앱(iosApp/)은 이 모듈이 만드는 StepUpKit 틀 하나만 붙인다.
// 지금은 iOS 대상만 둔다. 안드로이드 화면을 이리 옮길 때 androidTarget() 을 더한다(그 전까지 안드로이드 앱 화면은 :app 그대로).
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
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
        commonMain.dependencies {
            api(project(":shared"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
    }
}

compose.resources {
    packageOfResClass = "com.stepup.ui.res"
}
