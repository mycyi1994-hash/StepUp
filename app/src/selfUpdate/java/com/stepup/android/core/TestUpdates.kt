package com.stepup.android.core

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import com.stepup.android.BuildConfig
import com.stepup.android.data.remote.UrlConnectionPoster
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** test-apk 릴리스에 APK 와 함께 올라가는 빌드 정보(StepUp-test.json, CI 가 만든다) */
@Serializable
data class TestBuild(
    /** 이 빌드를 만든 커밋의 시각(유닉스 초) — 새 빌드인지 가르는 값 */
    val commitTime: Long = 0,
    val commit: String = "",
    val version: String = "",
    /**
     * 안드로이드 versionCode — 테스트 빌드는 앱 데이터베이스 판을 담는다(app/build.gradle.kts). 이 폰의 앱보다 낮으면
     * 안드로이드가 설치를 막으므로(데이터베이스가 더 새 작업 브랜치 빌드 위) 새 버전으로 알리지 않는다. 0 = 모름(예전 빌드 정보)
     */
    val versionCode: Int = 0,
)

/**
 * 테스트 APK 스스로 업데이트(2026-09-26 사용자 요청, 2026-10-18 "원격 업데이트 안 된다 — 근본적으로 해결",
 * 2026-10-23 "업데이트 떠도 '앱이 설치되지 않았습니다'").
 *
 * - 새 빌드를 받아 이 폰의 앱과 맞대어 본 뒤(패키지 · 판 · 서명) **안드로이드 설치 화면**을 연다 — "업데이트"를 한 번 누르면 깔린다.
 *   2026-10-18 판은 앱이 설치 세션(PackageInstaller)으로 직접 깔았다. 안드로이드 14 · 15 의 Play 스토어 기기에서 그러면
 *   Play 프로텍트가 "앱 검사 권장 · 설치 안 함" 창으로 가로막아 깔리지 않았다(에뮬레이터 검사 tools/self_update_e2e.sh).
 *   안드로이드 설치 화면으로 연 설치는 같은 기기에서 그대로 깔린다.
 * - 설치 화면에서 돌아왔는데 아직 옛 버전이면(취소 · 거절) 까닭과 함께 같은 창을 다시 띄운다 — 받아 둔 파일로 바로 다시 연다.
 * - "출처를 알 수 없는 앱 설치" 허용이 없으면 그 설정을 열고, 허용하고 돌아오면 이어서 연다.
 * - 앱을 켤 때 · 돌아올 때(5분에 한 번까지) · 설정에서 누를 때 묻고, 앱이 꺼져 있어도 일꾼([TestUpdateWorker])이 1시간마다 묻는다.
 *   일꾼은 새 빌드를 미리 받아 두고 "새 버전이 있어요 — 눌러서 업데이트" 알림을 남긴다(앱 밖에서는 설치 화면을 열 수 없다).
 * - 러닝 중에는 열지 않는다 — 깔면 앱이 다시 켜지며 러닝 서비스가 끊긴다.
 *
 * debug · qa 빌드(= test-apk 로 배포되는 파일)에서만 켜진다. 스토어 빌드에는 설치 권한도 받는 쪽도 없다(src/debug/AndroidManifest.xml).
 * 계측 테스트 중에는 끈다 — 에뮬레이터 화면 확인에 알림 창이 끼어들지 않게.
 */
object TestUpdates {
    private const val RELEASE = "https://github.com/mycyi1994-hash/StepUp/releases/download/test-apk"
    const val INFO_URL = "$RELEASE/StepUp-test.json"
    const val APK_URL = "$RELEASE/StepUp-test.apk"

    /** 앱으로 돌아올 때 다시 묻는 가장 짧은 간격 */
    const val RECHECK_MS = 5 * 60_000L

    private const val APK_MIME = "application/vnd.android.package-archive"

    /** 설치 화면을 연 뒤 이만큼 지나 앱으로 돌아왔는데 아직 옛 버전이면 깔리지 않은 것이다 */
    const val INSTALLER_SETTLE_MS = 1_000L

    /** 깔지 못한 까닭 — 화면에 그대로 다른 글로 보인다 */
    enum class Failure {
        /** 받다가 끊겼다 · 받은 파일이 깨졌다 */
        DOWNLOAD,
        /** 이 폰의 앱과 서명이 다르다 — 지우고 새로 깔아야 한다 */
        SIGNATURE,
        /** 이 폰의 앱이 더 새 데이터베이스 판이다 */
        DOWNGRADE,
        /** 저장 공간이 모자라다 */
        STORAGE,
        /** 기기 정책 · 보안 설정이 막았다 */
        BLOCKED,
        /** 안드로이드 설치 화면에서 돌아왔는데 깔리지 않았다(취소 · Play 프로텍트 · 그 밖의 거절) */
        UNFINISHED,
        /** 그 밖 */
        OTHER,
    }

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        /** 설정에서 눌러 확인했는데 빌드 정보를 받지 못했다 */
        data object CheckFailed : State
        data class Available(val build: TestBuild) : State
        data class Downloading(val build: TestBuild, val progress: Float?) : State
        /** 안드로이드 설치 화면을 열었다 — 거기서 "업데이트"를 누르면 앱이 닫히고 새 버전이 깔린다 */
        data class Installing(val build: TestBuild) : State
        /** "출처를 알 수 없는 앱 설치"를 허용해야 깔 수 있다 */
        data class NeedsPermission(val build: TestBuild) : State
        data class Failed(val build: TestBuild, val reason: Failure = Failure.OTHER, val detail: String = "") : State
        /** "나중에" — 이 빌드는 앱을 켜 있는 동안 다시 묻지 않는다(설정에서 누르면 다시) */
        data class Dismissed(val build: TestBuild) : State
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * 확인 · 받기는 화면이 아니라 앱 수명에 묶는다 — 받는 중에 화면이 돌거나 다시 그려지면 화면의 작업은
     * 취소되고, 그러면 "받는 중"에 멈춘 창을 닫을 수 없었다.
     */
    // 처음 쓸 때 만든다 — 단위 테스트(JVM)에는 Main 디스패처가 없다
    private val scope by lazy { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.Main.immediate) }

    private val json = Json { ignoreUnknownKeys = true }
    private val http = UrlConnectionPoster(timeoutMillis = 10_000)

    /** 받기 · 깔기는 한 번에 하나 — 화면의 버튼과 일꾼이 겹치지 않게 */
    private val installLock = Mutex()

    val enabled: Boolean by lazy {
        BuildConfig.SELF_UPDATE && BuildConfig.BUILD_COMMIT_TIME > 0 && !underInstrumentation()
    }

    /** 이 빌드 표시 — "1.17.0 · abc1234". 커밋을 모르면 버전만. */
    val buildLabel: String
        get() = BuildConfig.VERSION_NAME + BuildConfig.BUILD_COMMIT.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()

    // ---- 앱이 화면에 있는가 ----

    @Volatile private var startedActivities = 0

    /** 앱 화면이 하나라도 보이는가 — 일꾼은 이때 깔지 않는다(쓰는 중에 앱이 닫히지 않게) */
    val inForeground: Boolean get() = startedActivities > 0

    /** 앱을 켤 때 한 번 — 화면이 보이는지 세고, 1시간마다 묻는 일꾼을 건다 */
    fun attach(app: Application) {
        if (!enabled) return
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) { startedActivities++ }
            override fun onActivityStopped(activity: Activity) { startedActivities = (startedActivities - 1).coerceAtLeast(0) }
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
            override fun onActivityResumed(activity: Activity) = Unit
            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
            override fun onActivityDestroyed(activity: Activity) = Unit
        })
        TestUpdateWorker.createChannel(app)
        runCatching { TestUpdateWorker.schedule(app) }
    }

    // ---- 묻기 ----

    fun parse(body: String): TestBuild? = runCatching {
        json.decodeFromString(TestBuild.serializer(), body).takeIf { it.commitTime > 0 }
    }.getOrNull()

    /**
     * 올라가 있는 빌드가 지금 빌드보다 나중 커밋인가. 같은 커밋이거나 지금 빌드를 모르면 아니다.
     * versionCode 가 이 폰의 것보다 낮으면(데이터베이스가 더 옛 빌드) 깔 수 없으니 알리지 않는다.
     */
    fun isNewer(remote: TestBuild, localCommitTime: Long, localCommit: String, localVersionCode: Int = 0): Boolean =
        localCommitTime > 0 && remote.commitTime > localCommitTime &&
            (localCommit.isBlank() || !remote.commit.startsWith(localCommit)) &&
            (remote.versionCode == 0 || remote.versionCode >= localVersionCode)

    /** 마지막으로 물어본 때(SystemClock.elapsedRealtime) — 앱으로 돌아올 때마다 묻되 너무 자주는 묻지 않는다 */
    @Volatile private var lastCheckAt = 0L

    /** 창이 떠 있는 상태 — 받는 중 · 까는 중 · 허용 기다림 · 실패도 창이다 */
    private fun State.busy(): Boolean =
        this is State.Downloading || this == State.Checking || this is State.Available || this is State.Failed ||
            this is State.Installing || this is State.NeedsPermission

    /**
     * 지금 물어볼까. 받는 중 · 묻는 중 · 새 빌드를 알리는 중이면 아니다. 설정에서 누른 것([force])은 늘 묻는다.
     * 앱으로 돌아올 때(2026-10-18 사용자 "원격 업데이트 안 되는데" — 예전엔 앱을 켤 때 한 번만 물어, 켜 둔 앱은 새 빌드를 몰랐다)는
     * 마지막으로 물은 지 [RECHECK_MS] 가 지났을 때만.
     */
    fun shouldCheck(current: State, force: Boolean, sinceLastMs: Long): Boolean = when {
        current.busy() -> false
        else -> force || current == State.Idle && sinceLastMs < 0 || sinceLastMs >= RECHECK_MS
    }

    /**
     * 물어본 뒤의 상태. "나중에"를 누른 빌드와 같은 빌드면 다시 묻지 않고, 그 뒤에 또 새 빌드가 올라왔으면 다시 알린다.
     */
    fun afterCheck(current: State, remote: TestBuild?, newer: Boolean, force: Boolean): State = when {
        remote == null -> if (force) State.CheckFailed else (current.takeIf { it is State.Dismissed } ?: State.Idle)
        !newer -> State.UpToDate
        !force && current is State.Dismissed && current.build.commit == remote.commit -> current
        else -> State.Available(remote)
    }

    /** 올라가 있는 빌드 정보. 못 받으면 null */
    private suspend fun fetchInfo(): TestBuild? {
        val response = http.get(INFO_URL, mapOf("Cache-Control" to "no-cache"))
        return if (response.status in 200..299) parse(response.body) else null
    }

    private fun newerThanThisApp(remote: TestBuild): Boolean =
        isNewer(remote, BuildConfig.BUILD_COMMIT_TIME, BuildConfig.BUILD_COMMIT, BuildConfig.VERSION_CODE)

    /**
     * 새 빌드가 있는지 묻는다. 앱을 켤 때 · 앱으로 돌아올 때(5분에 한 번까지), 설정에서 누르면 [force] 로 바로.
     */
    suspend fun check(force: Boolean = false) {
        if (!enabled) return
        val current = _state.value
        val now = android.os.SystemClock.elapsedRealtime()
        val since = if (lastCheckAt == 0L) -1L else now - lastCheckAt
        if (!shouldCheck(current, force, since)) return
        lastCheckAt = now
        _state.value = State.Checking
        try {
            val remote = fetchInfo()
            val newer = remote != null && newerThanThisApp(remote)
            _state.value = afterCheck(current, remote, newer, force)
        } finally {
            // 중간에 끊겼으면 "확인 중"에 멈추지 않게 되돌린다
            if (_state.value == State.Checking) _state.value = if (current is State.Dismissed) current else State.Idle
        }
    }

    /** [check] 를 앱 수명의 작업으로 — 화면이 바뀌어도 끝까지 */
    fun startCheck(force: Boolean = false) {
        scope.launch { check(force) }
    }

    /**
     * 앱으로 돌아올 때 — "출처를 알 수 없는 앱" 설정에서 허용하고 돌아왔으면 이어서 깔고, 아니면 새 빌드를 묻는다.
     */
    fun onResume(context: Context) {
        if (!enabled) return
        val s = _state.value
        if (s is State.Installing) {
            val since = android.os.SystemClock.elapsedRealtime() - installerOpenedAt
            val next = afterInstallerReturn(s, since)
            if (next != s) clearInstalling(context)
            _state.value = next
            return
        }
        if (s is State.NeedsPermission && canInstall(context)) {
            _state.value = State.Available(s.build)
            startDownload(context)
            return
        }
        startCheck()
    }

    /**
     * 안드로이드 설치 화면에서 앱으로 돌아왔다 — 깔렸으면 이 앱은 이미 닫혀 새 버전으로 바뀌었을 것이다. 아직 옛 버전으로
     * 돌아왔으면(취소 · 거절) 다시 누를 수 있는 창으로. 설치 화면이 막 뜨는 사이의 돌아옴은 세지 않는다.
     */
    fun afterInstallerReturn(current: State, sinceOpenedMs: Long): State =
        if (current is State.Installing && sinceOpenedMs >= INSTALLER_SETTLE_MS) State.Failed(current.build, Failure.UNFINISHED) else current

    /** 안드로이드 설치 화면을 연 때(SystemClock.elapsedRealtime) */
    @Volatile private var installerOpenedAt = 0L

    fun dismiss() {
        val build = when (val s = _state.value) {
            is State.Available -> s.build
            is State.Failed -> s.build
            is State.NeedsPermission -> s.build
            else -> return
        }
        _state.value = State.Dismissed(build)
    }

    // ---- 받고 깔기 ----

    /** 이 앱이 패키지를 깔 수 있나 — "출처를 알 수 없는 앱 설치" 허용 */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /** "출처를 알 수 없는 앱 설치"의 이 앱 칸을 연다 */
    fun openInstallPermission(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
    }

    /** [downloadAndInstall] 을 앱 수명의 작업으로 시작한다 */
    fun startDownload(context: Context) {
        val app = context.applicationContext
        scope.launch { downloadAndInstall(app) }
    }

    /** 화면의 "업데이트" — 받아서 맞대어 보고 안드로이드 설치 화면을 연다. 실패하면 [State.Failed] 에 까닭과 함께 — 다시 누를 수 있다. */
    suspend fun downloadAndInstall(context: Context) {
        val build = when (val s = _state.value) {
            is State.Available -> s.build
            is State.Failed -> s.build
            is State.NeedsPermission -> s.build
            else -> return
        }
        if (runBusy()) return // 화면이 러닝 중에는 창을 숨긴다 — 혹시 눌렸어도 깔지 않는다
        if (!canInstall(context)) {
            _state.value = State.NeedsPermission(build)
            openInstallPermission(context)
            return
        }
        installLock.withLock {
            _state.value = State.Downloading(build, null)
            val result = try {
                prepare(context, build) { progress -> _state.value = State.Downloading(build, progress) }
            } finally {
                if (_state.value is State.Downloading) _state.value = State.Failed(build, Failure.DOWNLOAD)
            }
            when (result) {
                is Prepared.Ready -> {
                    installerOpenedAt = android.os.SystemClock.elapsedRealtime()
                    _state.value = State.Installing(build)
                    if (!openInstaller(context, result.file, build)) _state.value = State.Failed(build, Failure.OTHER)
                }
                is Prepared.Refused -> _state.value = State.Failed(build, result.reason)
            }
        }
    }

    /**
     * 일꾼(앱이 화면에 없을 때) — 새 빌드가 있으면 미리 받아 맞대어 두고 "새 버전이 있어요 — 눌러서 업데이트" 알림을 남긴다(빌드마다 한 번).
     * 알림을 눌러 앱을 열면 창의 "업데이트"가 받아 둔 파일로 바로 설치 화면을 연다. 앱 밖에서 설치 세션으로 깔면
     * Play 프로텍트가 가로막아(2026-10-23) 앱 밖에서는 깔지 않는다.
     */
    suspend fun backgroundUpdate(context: Context) {
        if (!enabled || inForeground || runBusy()) return
        val remote = fetchInfo() ?: return
        if (!newerThanThisApp(remote)) return
        if (installLock.tryLock()) {
            try {
                prepare(context, remote) {}
            } finally {
                installLock.unlock()
            }
        }
        TestUpdateWorker.notifyAvailable(context, remote)
    }

    /** 러닝 중이거나 막 끝나 저장하는 중 — 이때 깔면 앱이 다시 켜지며 러닝이 끊긴다 */
    private fun runBusy(): Boolean {
        val run = com.stepup.android.service.WalkSessionService.state.value
        return run.isActive || run.saveStatus == com.stepup.android.service.RunSaveStatus.SAVING
    }

    private sealed interface Prepared {
        data class Ready(val file: File) : Prepared
        data class Refused(val reason: Failure) : Prepared
    }

    /** 받은(또는 이미 받아 둔) 이 빌드의 APK 를 이 폰의 앱과 맞대어 본다 */
    private suspend fun prepare(context: Context, build: TestBuild, onProgress: (Float?) -> Unit): Prepared {
        val file = cached(context, build) ?: download(context, build, onProgress) ?: return Prepared.Refused(Failure.DOWNLOAD)
        val reason = withContext(Dispatchers.IO) { verify(context, file) }
        if (reason != null) {
            file.delete()
            return Prepared.Refused(reason)
        }
        return Prepared.Ready(file)
    }

    private fun updatesDir(context: Context) = File(context.cacheDir, "updates").apply { mkdirs() }
    private fun apkFile(context: Context, build: TestBuild) =
        File(updatesDir(context), "StepUp-test-${build.commit.ifBlank { build.commitTime.toString() }}.apk")

    /** 같은 빌드를 이미 다 받아 두었으면 다시 받지 않는다(취소 뒤 다시 누를 때 · 일꾼이 받아 둔 것) */
    private fun cached(context: Context, build: TestBuild): File? =
        apkFile(context, build).takeIf { it.isFile && it.length() > 0 }

    private suspend fun download(context: Context, build: TestBuild, onProgress: (Float?) -> Unit): File? = withContext(Dispatchers.IO) {
        val dir = updatesDir(context)
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "download.part")
        val apk = apkFile(context, build)
        val ok = runCatching {
            // github.com → 파일 저장소(https)로 넘겨 준다. HttpURLConnection 은 같은 https 끼리 따라간다.
            val connection = URL(APK_URL).openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = 15_000
                connection.readTimeout = 30_000
                connection.setRequestProperty("Cache-Control", "no-cache")
                if (connection.responseCode !in 200..299) return@runCatching false
                val total = connection.contentLengthLong
                var read = 0L
                var lastShown = -1
                connection.inputStream.use { input ->
                    part.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buffer)
                            if (n < 0) break
                            output.write(buffer, 0, n)
                            read += n
                            if (total > 0) {
                                val percent = (read * 100 / total).toInt()
                                if (percent != lastShown) {
                                    lastShown = percent
                                    onProgress(percent / 100f)
                                }
                            }
                        }
                    }
                }
                (total <= 0 || read == total) && part.renameTo(apk)
            } finally {
                connection.disconnect()
            }
        }.getOrDefault(false)
        if (!ok) {
            part.delete()
            apk.delete()
        }
        if (ok) apk else null
    }

    /**
     * 받은 APK 가 이 폰의 앱 위에 깔릴 수 있나 — 안드로이드에 넘기기 전에 이유를 알 수 있는 것은 여기서 가른다.
     * 모르는 값(안드로이드가 서명을 읽어 주지 않는 판)은 막지 않는다 — 그때는 안드로이드의 답을 따른다.
     */
    private fun verify(context: Context, apk: File): Failure? {
        val pm = context.packageManager
        val archive = runCatching { archiveInfo(pm, apk.path) }.getOrNull() ?: return Failure.DOWNLOAD
        if (archive.packageName != context.packageName) return Failure.DOWNLOAD
        val installed = runCatching { installedInfo(pm, context.packageName) }.getOrNull() ?: return null
        val failure = compatibility(
            archiveVersion = archive.longVersionCodeCompat(),
            installedVersion = installed.longVersionCodeCompat(),
            archiveSigners = signerHistory(archive),
            installedSigner = currentSigners(installed),
        )
        return failure
    }

    /**
     * 덮어 깔 수 있나 — 판이 낮으면 [Failure.DOWNGRADE], 이 폰의 앱을 서명한 키가 새 APK 의 서명(이력 포함)에 없으면 [Failure.SIGNATURE].
     * 서명 값을 모르면(빈 목록) 서명은 보지 않는다.
     */
    fun compatibility(
        archiveVersion: Long,
        installedVersion: Long,
        archiveSigners: List<String>,
        installedSigner: Set<String>,
    ): Failure? = when {
        archiveVersion < installedVersion -> Failure.DOWNGRADE
        archiveSigners.isNotEmpty() && installedSigner.isNotEmpty() && archiveSigners.none { it in installedSigner } -> Failure.SIGNATURE
        else -> null
    }

    @Suppress("DEPRECATION")
    private fun archiveInfo(pm: PackageManager, path: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return pm.getPackageArchiveInfo(path, flags)
    }

    @Suppress("DEPRECATION")
    private fun installedInfo(pm: PackageManager, packageName: String): PackageInfo? {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        return pm.getPackageInfo(packageName, flags)
    }

    @Suppress("DEPRECATION")
    private fun PackageInfo.longVersionCodeCompat(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) longVersionCode else versionCode.toLong()

    /** 새 APK 가 쓸 수 있다고 증명한 모든 키(서명 이력 포함) */
    @Suppress("DEPRECATION")
    private fun signerHistory(info: PackageInfo): List<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptyList()
            val certs = if (signing.hasMultipleSigners()) signing.apkContentsSigners else signing.signingCertificateHistory
            return certs.orEmpty().map { sha256(it.toByteArray()) }
        }
        return info.signatures.orEmpty().map { sha256(it.toByteArray()) }
    }

    /** 이 폰의 앱을 지금 서명한 키 — 서명 이력의 맨 끝(지금 키) */
    @Suppress("DEPRECATION")
    private fun currentSigners(info: PackageInfo): Set<String> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val signing = info.signingInfo ?: return emptySet()
            if (signing.hasMultipleSigners()) return signing.apkContentsSigners.orEmpty().map { sha256(it.toByteArray()) }.toSet()
            return signing.signingCertificateHistory.orEmpty().lastOrNull()?.let { setOf(sha256(it.toByteArray())) }.orEmpty()
        }
        return info.signatures.orEmpty().map { sha256(it.toByteArray()) }.toSet()
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    /**
     * 안드로이드 설치 화면을 연다(받은 파일은 이 앱의 FileProvider 로 읽게 한다). 새 작업으로 열어 예전 설치 화면이
     * 남아 있어도 이번 파일로 새로 뜬다. 깔리면 이 앱은 닫히고 새 버전이 켜질 때 [onReplaced] 가 알린다.
     */
    private fun openInstaller(context: Context, apk: File, build: TestBuild): Boolean = runCatching {
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.share", apk)
        markInstalling(context, build)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, APK_MIME)
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK),
        )
        true
    }.getOrElse {
        clearInstalling(context)
        false
    }

    // ---- 다 깔린 뒤 ----

    private const val PREFS = "test_updates"
    private const val KEY_INSTALLING = "installing_commit"

    private fun markInstalling(context: Context, build: TestBuild) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_INSTALLING, build.commit).apply()
    }

    private fun clearInstalling(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_INSTALLING).apply()
    }

    /**
     * 새 버전이 깔린 직후(MY_PACKAGE_REPLACED, 새 버전의 코드로 돈다) — 이 앱이 깐 업데이트면 "업데이트했어요 · 눌러서 열기" 알림.
     * 브라우저로 손수 깐 것에는 알리지 않는다.
     */
    internal fun onReplaced(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_INSTALLING, null) ?: return
        prefs.edit().remove(KEY_INSTALLING).apply()
        updatesDir(context).listFiles()?.forEach { it.delete() }
        TestUpdateWorker.notifyUpdated(context, buildLabel)
    }

    /** 계측 테스트 러너가 같은 프로세스에 올라와 있으면 테스트 중이다 */
    private fun underInstrumentation(): Boolean =
        runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry"); true }.getOrDefault(false)
}
