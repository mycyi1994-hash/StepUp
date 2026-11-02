pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "StepUp"
include(":app")
// 안드로이드 · iOS 가 함께 쓰는 규칙(순수 Kotlin)과 iOS 앱 화면(Compose Multiplatform) — docs/ios/README.md
include(":shared")
include(":sharedUi")
