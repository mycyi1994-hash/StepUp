package com.stepup.android.core

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.stepup.android.BuildConfig
import com.stepup.android.data.remote.UrlConnectionPoster
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
 * 테스트 APK 새 버전 알림(2026-09-26 사용자 요청) — 스토어 밖에서는 몰래 설치할 수 없으므로,
 * 더 새 빌드가 있으면 알리고 받은 뒤 안드로이드 설치 화면을 연다. 사용자는 "설치"만 누른다.
 *
 * debug 빌드(= test-apk 로 배포되는 파일)에서만 켜진다. 스토어 빌드에는 설치 권한도 없다.
 * 계측 테스트 중에는 끈다 — 에뮬레이터 화면 확인에 알림 창이 끼어들지 않게.
 */
object TestUpdates {
    private const val RELEASE = "https://github.com/mycyi1994-hash/StepUp/releases/download/test-apk"
    const val INFO_URL = "$RELEASE/StepUp-test.json"
    const val APK_URL = "$RELEASE/StepUp-test.apk"

    /** 앱으로 돌아올 때 다시 묻는 가장 짧은 간격 */
    const val RECHECK_MS = 5 * 60_000L
    private const val APK_MIME = "application/vnd.android.package-archive"

    sealed interface State {
        data object Idle : State
        data object Checking : State
        data object UpToDate : State
        /** 설정에서 눌러 확인했는데 빌드 정보를 받지 못했다 */
        data object CheckFailed : State
        data class Available(val build: TestBuild) : State
        data class Downloading(val build: TestBuild, val progress: Float?) : State
        data class Failed(val build: TestBuild) : State
        /** "나중에" — 이번에 앱을 켜 있는 동안은 다시 묻지 않는다 */
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

    val enabled: Boolean by lazy {
        BuildConfig.SELF_UPDATE && BuildConfig.BUILD_COMMIT_TIME > 0 && !underInstrumentation()
    }

    /** 이 빌드 표시 — "1.17.0 · abc1234". 커밋을 모르면 버전만. */
    val buildLabel: String
        get() = BuildConfig.VERSION_NAME + BuildConfig.BUILD_COMMIT.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()

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

    /**
     * 지금 물어볼까. 받는 중 · 묻는 중 · 새 빌드를 알리는 중이면 아니다. 설정에서 누른 것([force])은 늘 묻는다.
     * 앱으로 돌아올 때(2026-10-18 사용자 "원격 업데이트 안 되는데" — 예전엔 앱을 켤 때 한 번만 물어, 켜 둔 앱은 새 빌드를 몰랐다)는
     * 마지막으로 물은 지 [RECHECK_MS] 가 지났을 때만.
     */
    fun shouldCheck(current: State, force: Boolean, sinceLastMs: Long): Boolean = when (current) {
        is State.Downloading, State.Checking, is State.Available, is State.Failed -> false
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
            val response = http.get(INFO_URL, mapOf("Cache-Control" to "no-cache"))
            val remote = if (response.status in 200..299) parse(response.body) else null
            val newer = remote != null &&
                isNewer(remote, BuildConfig.BUILD_COMMIT_TIME, BuildConfig.BUILD_COMMIT, BuildConfig.VERSION_CODE)
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

    fun dismiss() {
        val build = when (val s = _state.value) {
            is State.Available -> s.build
            is State.Failed -> s.build
            else -> return
        }
        _state.value = State.Dismissed(build)
    }

    /** [downloadAndInstall] 을 앱 수명의 작업으로 시작한다 */
    fun startDownload(context: Context) {
        val app = context.applicationContext
        scope.launch { downloadAndInstall(app) }
    }

    /** 새 APK 를 받아 확인하고 설치 화면을 연다. 실패하면 [State.Failed] — 다시 누를 수 있다. */
    suspend fun downloadAndInstall(context: Context) {
        val build = when (val s = _state.value) {
            is State.Available -> s.build
            is State.Failed -> s.build
            else -> return
        }
        _state.value = State.Downloading(build, null)
        val file = try {
            download(context) { progress -> _state.value = State.Downloading(build, progress) }
        } finally {
            if (_state.value is State.Downloading) _state.value = State.Failed(build)
        }
        if (file == null) {
            _state.value = State.Failed(build)
            return
        }
        val opened = runCatching {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.share", file)
            context.startActivity(
                Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, APK_MIME)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.isSuccess
        // 설치 화면이 열렸으면 창을 닫는다. 설치를 취소했으면 다음에 켤 때 다시 알린다.
        _state.value = if (opened) State.Dismissed(build) else State.Failed(build)
    }

    private suspend fun download(context: Context, onProgress: (Float?) -> Unit): File? = withContext(Dispatchers.IO) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val part = File(dir, "StepUp-test.apk.part")
        val apk = File(dir, "StepUp-test.apk")
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
        // 받은 파일이 이 앱의 APK 인지 확인한다 — 끊긴 파일 · 다른 파일로 설치 화면을 열지 않는다
        val valid = ok && runCatching {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageArchiveInfo(apk.path, 0)?.packageName == context.packageName
        }.getOrDefault(false)
        if (!valid) {
            part.delete()
            apk.delete()
        }
        if (valid) apk else null
    }

    /** 계측 테스트 러너가 같은 프로세스에 올라와 있으면 테스트 중이다 */
    private fun underInstrumentation(): Boolean =
        runCatching { Class.forName("androidx.test.platform.app.InstrumentationRegistry"); true }.getOrDefault(false)
}
