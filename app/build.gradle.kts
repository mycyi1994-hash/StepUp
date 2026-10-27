plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
    // app/google-services.json 을 읽어 Firebase(푸시·분석) 설정을 앱에 넣는다
    alias(libs.plugins.google.services)
}

// 릴리즈 서명 자격 — 환경변수(CI) 또는 ~/.gradle/gradle.properties(로컬)에서 읽는다.
// 저장소에는 키도 비밀번호도 커밋하지 않는다. 값이 없으면 release 빌드는
// 서명되지 않은 채로 만들어지고, 그 사실이 빌드 로그에 찍힌다.
// 빈 문자열은 "없음"으로 취급한다. CI가 시크릿 없이 env를 넘기면 getenv는
// null이 아니라 ""를 돌려주고, 그대로 file("")을 부르면 설정 단계에서 죽는다.
fun secret(env: String, prop: String): String? =
    (System.getenv(env) ?: project.findProperty(prop) as String?)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseStorePath = secret("RELEASE_KEYSTORE_PATH", "stepupReleaseKeystorePath")
val releaseStorePassword = secret("RELEASE_KEYSTORE_PASSWORD", "stepupReleaseKeystorePassword")
val releaseKeyAlias = secret("RELEASE_KEY_ALIAS", "stepupReleaseKeyAlias")
val releaseKeyPassword = secret("RELEASE_KEY_PASSWORD", "stepupReleaseKeyPassword")
val releaseSigningReady =
    releaseStorePath != null &&
        releaseStorePassword != null &&
        releaseKeyAlias != null &&
        releaseKeyPassword != null &&
        file(releaseStorePath).exists()

// 테스트 APK 새 버전 알림에 쓰는 빌드 표시 — 이 빌드가 만들어진 커밋과 그 커밋 시각(초).
// 앱은 test-apk 릴리스의 StepUp-test.json 과 커밋 시각을 비교해 더 새 빌드가 있으면 알린다.
// git 을 못 읽으면 0 — 그 빌드는 새 버전 알림을 끈다.
fun gitOutput(vararg args: String): String? = runCatching {
    providers.exec {
        commandLine("git", *args)
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() }
}.getOrNull()

// 웹 지갑 페이지 주소 — debug · qa(테스트 APK)만 쓴다. 스토어 판은 비운다(WEB3 = false)
val walletUrl = secret("WALLET_URL", "stepupWalletUrl") ?: "https://stepupcrew.com/wallet.html"

// 스스로 업데이트 에뮬레이터 검사(tools/self_update_e2e.sh)는 -PstepupCommitTime=1 로 "아주 옛 빌드"를 만들어 지금 test-apk 로 올라가 본다
val buildCommitTime = (findProperty("stepupCommitTime") as String?)?.toLongOrNull()
    ?: gitOutput("log", "-1", "--format=%ct")?.toLongOrNull() ?: 0L
val buildCommit = gitOutput("rev-parse", "--short=7", "HEAD") ?: ""

// 스토어 빌드(release)의 versionCode — 출시할 때 손으로 올린다
val releaseVersionCode = 25

// 앱 안 Room 데이터베이스의 판(AppDatabase 의 @Database(version = …)). 테스트 · 개발 빌드의 versionCode 에 담는다(아래 androidComponents)
val roomSchemaVersion: Int = Regex("""(?m)^\s*version\s*=\s*(\d+)\s*,?\s*$""")
    .find(file("src/main/java/com/stepup/android/data/local/AppDatabase.kt").readText())
    ?.groupValues?.get(1)?.toInt()
    ?: error("AppDatabase.kt 에서 데이터베이스 판(version = …)을 찾지 못했습니다")
check(releaseVersionCode in 1..999) { "versionCode 는 1~999 — 테스트 빌드는 데이터베이스 판 × 1000 을 더한다" }

android {
    namespace = "com.stepup.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.stepup.android"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode
        versionName = "1.17.0"

        // 러닝 증명을 받을 서버 주소. 배포 전에는 비어 있고, 비어 있으면 앱은
        // 업로드를 시도하지 않고 세션을 대기열에 쌓아 둔다.
        // 로컬에서 바꾸려면 ~/.gradle/gradle.properties 에 stepupAttesterUrl 를 둔다.
        buildConfigField(
            "String",
            "ATTESTER_URL",
            "\"${secret("ATTESTER_URL", "stepupAttesterUrl") ?: ""}\"",
        )
        // Public web entry and on-chain address; both stay blank until the new
        // draw contract, roller and web config are deployed together.
        buildConfigField("String", "DRAW_DAPP_URL", "\"${secret("DRAW_DAPP_URL", "stepupDrawDappUrl") ?: ""}\"")
        buildConfigField("String", "DRAW_CONTRACT_ADDRESS", "\"${secret("DRAW_CONTRACT_ADDRESS", "stepupDrawContractAddress") ?: ""}\"")
        // 웹 지갑 페이지 — 지갑 연결 · 보너스 뽑기 · SUP · 신발 꺼내기/넣기 (web/wallet.html)
        buildConfigField("String", "WALLET_URL", "\"$walletUrl\"")
        // 블록체인 기능(지갑 연결 · 출금 · 상급 뽑기 · 체인 안내). 스토어 판(release)은 끈다 — 아래 release 블록.
        buildConfigField("boolean", "WEB3", "true")

        // ── 서버·로그인 설정 ────────────────────────────────────────
        //
        // 아래 세 값은 **공개되어도 되는 값**이다. APK 를 뜯으면 어차피 나오고,
        // 애초에 앱에 넣으라고 발급되는 값이다. 실제 보호는 서버의 행 단위
        // 보안 규칙(supabase/migrations/)이 한다.
        //
        // 절대 여기 넣으면 안 되는 것: sb_secret_ 로 시작하는 Supabase 키와
        // GOCSPX- 로 시작하는 구글 보안 비밀. 둘 다 모든 검사를 건너뛴다.
        //
        // 값을 먼저 계산해 두는 이유는 buildConfigField 인자 안에서 계산하면
        // 문자열 템플릿이 길어져 읽기도 어렵고 깨지기도 쉽기 때문이다.
        val supabaseUrl = secret("SUPABASE_URL", "stepupSupabaseUrl")
            ?: "https://pupjzcmybuoyhzfwrsdf.supabase.co"
        val supabaseKey = secret("SUPABASE_KEY", "stepupSupabaseKey")
            ?: "sb_publishable_jt74AKM32zdqnJlsFHEo2g_MNHa-WRO"

        // 구글 로그인에는 **웹** 클라이언트 ID 를 쓴다. 안드로이드 클라이언트
        // ID 가 아니다 — 그쪽은 "이 앱이 진짜 맞다"를 구글이 확인하는 용도로
        // 등록만 해 두고 코드에는 넣지 않는다. 앱이 받는 ID 토큰의 수신자(aud)가
        // 웹 클라이언트 ID 이고, 서버는 그 값으로 토큰을 검증한다.
        val googleWebClientId = secret("GOOGLE_WEB_CLIENT_ID", "stepupGoogleWebClientId")
            ?: "201996080239-8hrgea5hfe58ank2f6rbbqnkk5ek2mf3.apps.googleusercontent.com"

        // 지도 타일(MapTiler). 앱에 넣으라고 발급되는 공개 키다 — MapTiler 대시보드에서
        // 이 앱에서만 쓰이도록 제한해 둔다. 비워 두면 OpenStreetMap 공용 타일로 돌아간다.
        val mapTilerKey = secret("MAPTILER_KEY", "stepupMapTilerKey") ?: "Tsagjrb4rNikrZug4jBG"
        buildConfigField("String", "MAPTILER_KEY", "\"$mapTilerKey\"")

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_KEY", "\"$supabaseKey\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
        buildConfigField("long", "BUILD_COMMIT_TIME", "${buildCommitTime}L")
        buildConfigField("String", "BUILD_COMMIT", "\"$buildCommit\"")
        // 테스트 APK(debug) 에서만 새 버전을 스스로 받아 설치 화면을 연다. 스토어 빌드에는 없다.
        buildConfigField("boolean", "SELF_UPDATE", "false")
        // 러닝 화면의 "걸음 흉내" 같은 시험용 버튼 — debug · qa 에서만 켠다.
        buildConfigField("boolean", "TEST_TOOLS", "false")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // CI 러너는 매번 새로 생성되므로 AGP가 자동 생성하는 debug 키스토어는
        // 빌드마다 서명이 달라진다. 그러면 이전에 설치한 앱 위에 덮어쓸 때
        // INSTALL_FAILED_UPDATE_INCOMPATIBLE로 설치가 거부된다.
        // 저장소에 고정 키스토어를 두어 모든 빌드가 같은 서명을 갖게 한다.
        // (표준 Android debug 키와 동일한 성격의 공개 키 — 배포용 서명이 아니다.)
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
            // 사이드로딩 호환성을 위해 v1(JAR) 서명도 함께 넣는다.
            enableV1Signing = true
            enableV2Signing = true
        }

        // Play Console 업로드용. 자격이 없으면 아예 만들지 않는다 — 빈 값으로
        // 만들어 두면 AGP가 서명 단계에서 알기 어려운 오류를 낸다.
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(releaseStorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("boolean", "TEST_TOOLS", "true")
        }
        release {
            // R8을 지금부터 켜 둔다. 출시 직전에 켜면 그때 처음 보는 난독화
            // 문제를 출시 압박 속에서 고치게 된다.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
            // 스토어 판에는 블록체인 기능을 넣지 않는다(2026-10-20). Google Play 는 2026-01-28 부터 한국에서 암호화폐 거래소 ·
            // 지갑 앱에 금융정보분석원 가상자산사업자 신고 수리를 요구한다 — 지갑 연결 · 출금 · 상급 뽑기(지갑 연결 혜택)를 숨기고
            // 웹 지갑 페이지 주소도 비운다. 테스트 APK(qa) · debug 는 그대로 켠다(docs/PLAY-STORE.md)
            buildConfigField("boolean", "WEB3", "false")
            buildConfigField("String", "WALLET_URL", "\"\"")
        }
        // 폰에 받는 테스트 APK(test-apk 의 StepUp-test.apk). 예전에는 debug 를 그대로 올렸는데, 디버그 가능 앱은
        // ART 가 최적화를 끄고 R8 도 돌지 않아 Compose 화면 전체가 눈에 띄게 끊겼다(2026-10-04 "렉 걸린다").
        // 빌드 이름은 qa(test 로 시작하는 이름은 AGP 가 막는다). release 처럼 R8 로 줄이고 디버그 불가로 만들되, 서명은 debug 키 그대로라 이미 깔린 테스트 앱 위에 덮어 설치된다
        // (applicationId · 서명 · strideup.db 그대로). 자가 업데이트 · 테스트 도구(걸음 흉내)는 debug 처럼 켠다.
        create("qa") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            // 리소스 줄이기는 크기만 줄이고 속도와 상관없다 — 이름으로 찾는 리소스가 빠지는 위험만 있어 끈다.
            isShrinkResources = false
            proguardFiles("proguard-qa.pro")
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "SELF_UPDATE", "true")
            buildConfigField("boolean", "TEST_TOOLS", "true")
            // release 를 이어받으므로 블록체인 기능은 다시 켠다 — 테스트 APK 는 지갑 · 출금을 그대로 시험한다
            buildConfigField("boolean", "WEB3", "true")
            buildConfigField("String", "WALLET_URL", "\"$walletUrl\"")
        }
        // 스토어 판 미리 보기(2026-10-27 사용자 "스토어판을 실제로 보고 싶어") — release 와 같은 코드 · 설정
        // (블록체인 기능 없음 · 자가 업데이트 없음 · 시험 도구 없음 · R8 · 리소스 줄이기)에 서명 · versionCode 만 테스트 APK 와 같다.
        // 그래서 깔린 테스트 앱 위에 그대로 덮어 깔리고(이 폰의 기록 · 로그인 그대로), 테스트 APK 를 다시 깔면 돌아간다.
        // 구글 제출용은 이것이 아니라 Release AAB 다(docs/PLAY-STORE.md).
        create("storePreview") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
    }
    sourceSets {
        // 새 버전을 받아 설치 화면을 여는 권한(REQUEST_INSTALL_PACKAGES)은 debug 와 같은 파일을 쓴다.
        getByName("qa") { manifest.srcFile("src/debug/AndroidManifest.xml") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    // 앱이 지원하는 네 언어만 담는다 — 라이브러리(머티리얼 · 플레이 서비스 등)의 다른 수십 개 언어 글자는 쓰지 않아 빼서
    // 스토어 판 · 테스트 APK 크기를 줄인다(2026-10-26 플레이 출시 최적화). 언어를 더하면 여기와 xml/locales_config 를 함께 고친다
    androidResources {
        localeFilters += listOf("ko", "en", "ja", "zh")
    }

    // AAB에는 언어 리소스를 전부 담되, 기기가 받을 때만 골라 받게 한다.
    // (4개 언어를 지원하므로 language 스플릿을 끄면 APK가 불필요하게 커진다.)
    bundle {
        language { enableSplit = false } // 앱 내 언어 설정이 있으므로 분리하면 안 된다
        density { enableSplit = true }
        abi { enableSplit = true }
    }
}

// 테스트 · 개발 빌드(debug · qa)의 versionCode = 데이터베이스 판 × 1000 + 스토어 versionCode (2026-10-16).
// 모두 25 였을 때는 데이터베이스가 더 새 빌드(작업 브랜치 APK) 위에 더 옛 빌드(main 의 테스트 APK)를 덮어 깔 수 있었고,
// Room 은 판을 내리는 길을 몰라 앱이 켜지자마자 죽었다(지우고 다시 깔면 이 폰의 기록이 사라진다).
// 이제 안드로이드가 설치 단계에서 "더 낮은 버전"으로 막는다. 판이 같으면 versionCode 도 같아 서로 덮어 깔 수 있다.
// 스토어 빌드는 그대로 둔다 — 스토어는 내려가는 설치를 스스로 막고, 올린 번호는 되돌릴 수 없다.
val testVersionCode = roomSchemaVersion * 1000 + releaseVersionCode
androidComponents {
    onVariants { variant ->
        // storePreview 도 — 깔린 테스트 앱 위에 덮어 깔고 다시 되돌릴 수 있게 같은 번호를 쓴다
        if (variant.buildType == "debug" || variant.buildType == "qa" || variant.buildType == "storePreview") {
            variant.outputs.forEach { it.versionCode.set(testVersionCode) }
        }
    }
}

// 릴리즈 서명 자격이 없는 채로 release 빌드가 돌면 로그에 분명히 남긴다.
tasks.matching {
    it.name.endsWith("Release") && (it.name.startsWith("assemble") || it.name.startsWith("bundle"))
}
    .configureEach {
        doFirst {
            if (!releaseSigningReady) {
                logger.lifecycle(
                    "⚠️  릴리즈 서명 자격이 없어 서명하지 않고 빌드합니다. " +
                        "Play Console 업로드에는 쓸 수 없습니다. " +
                        "설정 방법: docs/RELEASE-SIGNING.md",
                )
            }
        }
    }

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.google.id)
    implementation(libs.play.services.location)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.messaging)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
