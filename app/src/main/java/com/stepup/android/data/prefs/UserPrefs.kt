package com.stepup.android.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.stepup.android.domain.Faction
import com.stepup.android.domain.RewardEconomy
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

// 저장소 이름은 옛 이름 그대로 둔다 — 바꾸면 이미 설치된 기기의 설정과
// 로그인 세션이 사라진다. ServiceLocator 의 DB 파일명과 같은 이유다.
private val Context.dataStore by preferencesDataStore(name = "strideup_prefs")

// 로그인 세션만 따로 둔 파일. 백업에서 이 파일만 뺀다(res/xml/backup_rules ·
// data_extraction_rules) — 백업 파일을 손에 넣은 사람이 로그인까지 가져가지 못하게.
// 설정 파일(strideup_prefs)은 그대로 백업된다: 통째로 빼면 거래소 커서가 사라져
// 거래 줄이 다시 들어온다.
private val Context.authDataStore by preferencesDataStore(name = "stepup_auth")

data class ExperiencePreferences(
    val sounds: Boolean = true,
    val haptics: Boolean = true,
    val reducedMotion: Boolean = false,
    val ambience: Boolean = false,
)

/**
 * 사용자 설정과 가벼운 상태(DataStore Preferences).
 * 포인트 잔액은 Room의 rewards 원장 합계로 관리하고, 여기에는
 * 목표/스니커즈 레벨/에너지/스트릭/걸음 기준점만 저장한다.
 */
class UserPrefs(
    private val context: Context,
    private val store: androidx.datastore.core.DataStore<Preferences> = context.dataStore,
    private val authStore: androidx.datastore.core.DataStore<Preferences> = context.authDataStore,
    /**
     * 예전 설정 파일의 세션을 이어받아도 되는가. 이 폰에서 **업데이트된** 설치일 때만 그렇다.
     * 새 폰에 백업을 복원하면 옛 설정 파일(세션 포함)이 같이 오는데, 그때는 새 설치라
     * 처음 설치 시각과 마지막 업데이트 시각이 같다 — 그 세션은 버리고 다시 로그인하게 한다.
     */
    private val legacySessionAdoptable: () -> Boolean = { updatedInPlace(context) },
) {

    private object Keys {
        val SOUNDS = booleanPreferencesKey("experience_sounds")
        val AMBIENCE = booleanPreferencesKey("experience_ambience")
        val HAPTICS = booleanPreferencesKey("experience_haptics")
        val REDUCED_MOTION = booleanPreferencesKey("experience_reduced_motion")
        val DAILY_GOAL = intPreferencesKey("daily_goal")
        val SNEAKER_LEVEL = intPreferencesKey("sneaker_level")
        val ENERGY = doublePreferencesKey("energy_remaining")
        val ENERGY_DAY = longPreferencesKey("energy_day")
        val ENERGY_RECEIPTS = androidx.datastore.preferences.core.stringSetPreferencesKey("energy_purchase_receipts")
        val RUN_ENERGY_RECEIPTS = androidx.datastore.preferences.core.stringSetPreferencesKey("run_energy_receipts")
        val STREAK = intPreferencesKey("streak")
        val LAST_GOAL_MET_DAY = longPreferencesKey("last_goal_met_day")
        val BASELINE_DAY = longPreferencesKey("baseline_day")
        val BASELINE_STEPS = longPreferencesKey("baseline_steps")
        /** 기준점을 잡을 때의 기기 부팅 횟수. 바뀌었으면 재부팅이다(-1 = 모름). */
        val BASELINE_BOOT = intPreferencesKey("baseline_boot")
        /** 에너지 상한을 정하는 레벨 — 지금 신은 신발의 레벨. 없으면 옛 SNEAKER_LEVEL */
        val ENERGY_CAP_LEVEL = intPreferencesKey("energy_cap_level")
        val RUNNER_UID = stringPreferencesKey("runner_uid")
        val NICKNAME = stringPreferencesKey("nickname")

        // ── 핫글 ──
        /** 이번 주 핫글 목록 (순서 있는 글 id) */
        val HOT_POST_IDS = stringPreferencesKey("hot_post_ids")
        /** 지금까지 한 번이라도 핫글에 오른 글 id — 다시 오르지 않게 기억한다 */
        val HOT_FEATURED_IDS = stringPreferencesKey("hot_featured_ids")
        /** 이 목록이 어느 갱신 시각의 것인지 (epoch millis) */
        val HOT_ROTATED_AT = longPreferencesKey("hot_rotated_at")
        val AVATAR_ID = intPreferencesKey("avatar_id")
        val AVATAR_REV = intPreferencesKey("avatar_rev")
        val LOGIN_METHOD = stringPreferencesKey("login_method")
        val GUIDE_SEEN = intPreferencesKey("guide_seen")
        val RUN_PERMISSION_PRIMER_SEEN = booleanPreferencesKey("run_permission_primer_seen")
        val S2_SETUP_SEEN = booleanPreferencesKey("s2_setup_seen")
        val WEATHER_BACKGROUND = booleanPreferencesKey("weather_background")
        val RUN_MODE = stringPreferencesKey("run_mode")
        val BODY_HEIGHT_CM = intPreferencesKey("body_height_cm")
        val BODY_WEIGHT_KG = doublePreferencesKey("body_weight_kg")
        val BODY_GOAL_WEIGHT_KG = doublePreferencesKey("body_goal_weight_kg")
        val BODY_GOAL_WEEKS = intPreferencesKey("body_goal_weeks")
        val LANGUAGE = stringPreferencesKey("language")
        /** 화면 테마 — ThemeMode 의 이름 문자열 */
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val SELECTED_COURSE = longPreferencesKey("selected_course")
        /** 서버에 코스 기록으로 낼 러닝 — "시작시각\t코스 길" 줄들 */
        val PENDING_COURSE_RUNS = stringPreferencesKey("pending_course_runs")
        /** "지금부터 뛰는 길을 코스로 저장한다"를 켜 둔 상태 */
        val COURSE_RECORDING = booleanPreferencesKey("course_recording")
        /** 지금 심어져 있는 데모 코스가 몇 번째 판인지 */
        val COURSE_SEED_VERSION = intPreferencesKey("course_seed_version")
        val TOP_SPEED = doublePreferencesKey("top_speed_kmh")
        val ACCOUNTED_STEPS = longPreferencesKey("accounted_steps")
        val ACCOUNTED_DAY = longPreferencesKey("accounted_day")
        val RUNNER_ADDRESS = stringPreferencesKey("runner_address")
        val AUTH_SESSION = stringPreferencesKey("auth_session")
        val MARKET_LEDGER_CURSOR = longPreferencesKey("market_ledger_cursor")
        val SERVER_ENERGY_MAX = doublePreferencesKey("server_energy_max")
        val FREE_DRAWS_LEFT = intPreferencesKey("free_draws_left")
        val BONUS_DRAWS_LEFT = intPreferencesKey("bonus_draws_left")
        val LEGACY_ECONOMY_IMPORTED = stringPreferencesKey("legacy_economy_imported")
        /** 폰의 서버 경제 사본(잔고 · 신발 · 부스터 · 받은 도전)이 어느 계정 것인가 */
        val ECONOMY_OWNER = stringPreferencesKey("economy_owner")
        val NEWS_FETCHED_AT = longPreferencesKey("news_fetched_at")

        // ── 러너 캐릭터 ──
        /** "M" / "F" — 기본 캐릭터 둘 중 무엇을 쓰는가 */
        val AVATAR_GENDER = stringPreferencesKey("avatar_gender")
        /** 입고 있는 의상 id. 실제로 가진 것만 들어간다. */
        val AVATAR_OUTFIT = stringPreferencesKey("avatar_outfit")

        // ── 데모 모드 ──
        //
        // 운영 데이터와 **다른 열쇠**를 쓴다. 데모에서 입혀 본 의상이
        // AVATAR_OUTFIT 에 섞이면, 데모를 끈 뒤에도 갖지 않은 옷을 입고 있게 된다.
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val NOTIFY_PUSH = booleanPreferencesKey("notify_push")
        val NOTIFY_GOAL = booleanPreferencesKey("notify_goal_reminder")
        val NOTIFY_PARTY = booleanPreferencesKey("notify_party_invite")
        val NOTIFY_EVENT = booleanPreferencesKey("notify_event_news")
        val DEMO_OUTFIT = stringPreferencesKey("demo_outfit")
    }

    val dailyGoal: Flow<Int> = store.data.map { it[Keys.DAILY_GOAL] ?: DEFAULT_GOAL }

    // ── 러너 캐릭터 ──────────────────────────────────────────

    val avatarGender: Flow<String> = store.data.map { it[Keys.AVATAR_GENDER] ?: "" }

    suspend fun setAvatarGender(id: String) {
        store.edit { it[Keys.AVATAR_GENDER] = id }
    }

    val avatarOutfit: Flow<String> = store.data.map { it[Keys.AVATAR_OUTFIT] ?: "" }

    suspend fun setAvatarOutfit(id: String) {
        store.edit { it[Keys.AVATAR_OUTFIT] = id }
    }

    // ── 데모 모드 ────────────────────────────────────────────

    /**
     * 서버 없이 화면을 둘러보는 모드.
     *
     * 켜져 있으면 소식 · 러너 마켓이 **예시 데이터**를 보여 주고, 화면마다
     * "데모"라고 적힌다. 원장(SUP)과 서버에는 아무것도 쓰지 않는다.
     */
    val demoMode: Flow<Boolean> = store.data.map { it[Keys.DEMO_MODE] ?: false }

    suspend fun setDemoMode(on: Boolean) {
        store.edit {
            it[Keys.DEMO_MODE] = on
            // 끄면 체험으로 입힌 옷도 벗긴다. 남겨 두면 다음에 켤 때 갑자기
            // 입고 나타난다 — 그 사이에 무엇을 했는지 사용자는 기억하지 못한다.
            if (!on) it.remove(Keys.DEMO_OUTFIT)
        }
    }

    /** 데모에서 체험으로 입혀 본 의상. 데모가 꺼지면 비어 있다. */
    val demoOutfit: Flow<String> = store.data.map { it[Keys.DEMO_OUTFIT] ?: "" }

    suspend fun setDemoOutfit(id: String) {
        store.edit {
            // 데모가 꺼져 있으면 체험 착용을 받지 않는다
            if (it[Keys.DEMO_MODE] == true) it[Keys.DEMO_OUTFIT] = id
        }
    }

    val experience: Flow<ExperiencePreferences> = store.data.map {
        ExperiencePreferences(
            sounds = it[Keys.SOUNDS] ?: true,
            haptics = it[Keys.HAPTICS] ?: true,
            reducedMotion = it[Keys.REDUCED_MOTION] ?: false,
            ambience = it[Keys.AMBIENCE] ?: false,
        )
    }

    suspend fun setSounds(enabled: Boolean) { store.edit { it[Keys.SOUNDS] = enabled } }
    suspend fun setAmbience(enabled: Boolean) { store.edit { it[Keys.AMBIENCE] = enabled } }
    suspend fun setHaptics(enabled: Boolean) { store.edit { it[Keys.HAPTICS] = enabled } }
    suspend fun setReducedMotion(enabled: Boolean) { store.edit { it[Keys.REDUCED_MOTION] = enabled } }

    // ── 알림 설정 ─────────────────────────────────────────────
    //
    // 알림은 서버가 보내므로 서버에도 같은 값을 올린다(notify_prefs). 여기는 화면이
    // 바로 읽는 사본이다.

    val notifyPrefs: Flow<NotifyPrefs> = store.data.map {
        NotifyPrefs(
            push = it[Keys.NOTIFY_PUSH] ?: true,
            goalReminder = it[Keys.NOTIFY_GOAL] ?: true,
            partyInvite = it[Keys.NOTIFY_PARTY] ?: true,
            eventNews = it[Keys.NOTIFY_EVENT] ?: true,
        )
    }

    suspend fun setNotifyPrefs(prefs: NotifyPrefs) {
        store.edit {
            it[Keys.NOTIFY_PUSH] = prefs.push
            it[Keys.NOTIFY_GOAL] = prefs.goalReminder
            it[Keys.NOTIFY_PARTY] = prefs.partyInvite
            it[Keys.NOTIFY_EVENT] = prefs.eventNews
        }
    }

    // ── 러너 식별 · 프로필 ───────────────────────────────────

    /** 러너 고유 ID — "SU-XXXXXX". 발급 전이면 빈 문자열. */
    val runnerUid: Flow<String> = store.data.map { it[Keys.RUNNER_UID] ?: "" }

    /**
     * 사용자가 정한 닉네임. 정하지 않았으면 빈 문자열이고, 화면은 기본 호칭을 쓴다.
     *
     * 빈 문자열을 "러너"로 채워 두지 않는 이유는, 그러면 "아직 안 정했다"와
     * "러너라고 정했다"를 구별할 수 없기 때문이다.
     */
    val nickname: Flow<String> = store.data.map { it[Keys.NICKNAME] ?: "" }

    // ── 핫글 ─────────────────────────────────────────────────

    /** 이번 주 핫글 — 뽑힌 순서 그대로 */
    val hotPostIds: Flow<List<Long>> =
        store.data.map { it[Keys.HOT_POST_IDS].toIdList() }

    /** 지금까지 핫글에 올랐던 글 전부 */
    val hotFeaturedIds: Flow<Set<Long>> =
        store.data.map { it[Keys.HOT_FEATURED_IDS].toIdList().toSet() }

    suspend fun hotRotatedAt(): Long =
        store.data.first()[Keys.HOT_ROTATED_AT] ?: 0L

    /**
     * 이번 주 핫글을 확정한다.
     *
     * 뽑힌 글은 "이미 올랐다" 목록에도 더해진다. 같은 글이 다음 주에 또 오르면
     * 새 글이 올라올 자리가 없어지고, 핫글은 붙박이 명예의 전당이 된다.
     */
    suspend fun setHotPosts(ids: List<Long>, rotatedAt: Long) {
        store.edit { prefs ->
            val featured = prefs[Keys.HOT_FEATURED_IDS].toIdList().toMutableSet()
            featured += ids
            prefs[Keys.HOT_POST_IDS] = ids.joinToString(",")
            prefs[Keys.HOT_FEATURED_IDS] = featured.joinToString(",")
            prefs[Keys.HOT_ROTATED_AT] = rotatedAt
        }
    }

    /**
     * 핫글 기록을 비운다. 게시판이 서버로 옮겨 가 글 번호가 새로 매겨졌을 때 쓴다 —
     * 옛 번호가 남아 있으면 같은 번호의 새 글이 "이미 올랐던 글"로 막힌다.
     */
    suspend fun clearHotPosts() {
        store.edit { prefs ->
            prefs.remove(Keys.HOT_POST_IDS)
            prefs.remove(Keys.HOT_FEATURED_IDS)
            prefs.remove(Keys.HOT_ROTATED_AT)
        }
    }

    /** 선택한 아바타 인덱스 (기본 0, [AVATAR_CUSTOM]이면 갤러리 사진) */
    val avatarId: Flow<Int> = store.data.map { it[Keys.AVATAR_ID] ?: 0 }

    /** 갤러리 사진이 바뀔 때마다 올라가는 리비전 — UI가 파일을 다시 읽는 신호 */
    val avatarRev: Flow<Int> = store.data.map { it[Keys.AVATAR_REV] ?: 0 }

    /** 앞뒤 공백을 떼고 [NICKNAME_MAX] 자로 자른다. 빈 값이면 기본 호칭으로 돌아간다. */
    suspend fun setNickname(name: String) {
        val cleaned = name.trim().take(NICKNAME_MAX)
        store.edit { it[Keys.NICKNAME] = cleaned }
    }

    suspend fun setAvatarId(id: Int) {
        store.edit { it[Keys.AVATAR_ID] = id }
    }

    suspend fun bumpAvatarRev() {
        store.edit { it[Keys.AVATAR_REV] = (it[Keys.AVATAR_REV] ?: 0) + 1 }
    }

    /** 로그인 방식 — "google" / "guest" / ""(미선택) */
    val loginMethod: Flow<String> = store.data.map { it[Keys.LOGIN_METHOD] ?: "" }

    suspend fun setLoginMethod(method: String) {
        store.edit { it[Keys.LOGIN_METHOD] = method }
    }

    /**
     * 보상을 받을 지갑 주소. 지갑 기능이 붙기 전에는 비어 있다.
     *
     * 비어 있으면 세션은 올라가지 않고 기기에 쌓인다. 나중에 지갑을 만들었을
     * 때 그동안 뛴 기록이 살아 있어야 하기 때문이다.
     */
    val runnerAddress: Flow<String> = store.data.map { it[Keys.RUNNER_ADDRESS] ?: "" }

    suspend fun setRunnerAddress(address: String) {
        store.edit { it[Keys.RUNNER_ADDRESS] = address.trim() }
    }

    // ── 로그인 세션 ──────────────────────────────────────────
    //
    // 서버가 준 출입증을 그대로 담아 둔다. 앱을 껐다 켜도 로그인이 유지되어야
    // 하고, 유지되지 않으면 그 사람의 서버 기록에 다시 닿지 못한다.

    //
    // 세션은 백업에서 빠지는 별도 파일(stepup_auth)에 둔다. 예전 버전은 설정 파일에
    // 두었으므로, 처음 읽을 때 옮기고 설정 파일에서는 지운다.

    suspend fun authSessionJson(): String {
        val current = authStore.data.map { it[Keys.AUTH_SESSION] ?: "" }.first()
        if (current.isNotBlank()) return current
        val legacy = store.data.map { it[Keys.AUTH_SESSION] ?: "" }.first()
        if (legacy.isBlank()) return ""
        val adopt = legacySessionAdoptable()
        if (adopt) authStore.edit { it[Keys.AUTH_SESSION] = legacy }
        store.edit { it.remove(Keys.AUTH_SESSION) }
        return if (adopt) legacy else ""
    }

    suspend fun setAuthSessionJson(json: String) {
        authStore.edit { it[Keys.AUTH_SESSION] = json }
        store.edit { it.remove(Keys.AUTH_SESSION) }
    }

    suspend fun clearAuthSession() {
        authStore.edit { it.remove(Keys.AUTH_SESSION) }
        store.edit { it.remove(Keys.AUTH_SESSION) }
    }

    /** 앱 언어 태그. 빈 문자열이면 기기 설정을 따른다 */
    val language: Flow<String> = store.data.map { it[Keys.LANGUAGE] ?: "" }

    suspend fun setLanguage(tag: String) {
        store.edit { it[Keys.LANGUAGE] = tag }
    }

    suspend fun languageNow(): String = store.data.first()[Keys.LANGUAGE] ?: ""

    // ── 화면 테마 ────────────────────────────────────────────

    /** 고른 화면 테마의 이름. 빈 문자열이면 아직 안 골랐다(기기 설정을 따른다). */
    val themeMode: Flow<String> = store.data.map { it[Keys.THEME_MODE] ?: "" }

    suspend fun setThemeMode(name: String) {
        store.edit { it[Keys.THEME_MODE] = name }
    }

    suspend fun themeModeNow(): String = store.data.first()[Keys.THEME_MODE] ?: ""

    /**
     * 거래 원장을 어디까지 폰에 옮겨 적었는지.
     *
     * 거래는 서버 원장에서 일어나는데 화면이 보여 주는 잔고는 폰의 원장이다.
     * 같은 거래를 두 번 적으면 잔고가 늘어나므로, 옮긴 줄의 번호를 기억해
     * 그다음부터만 가져온다.
     */
    val marketLedgerCursor: Flow<Long> =
        store.data.map { it[Keys.MARKET_LEDGER_CURSOR] ?: 0L }

    suspend fun setMarketLedgerCursor(id: Long) {
        store.edit { it[Keys.MARKET_LEDGER_CURSOR] = id }
    }

    /** 소식을 마지막으로 받아 온 시각. 하루에 한 번만 받으려고 쓴다. */
    suspend fun newsFetchedAtNow(): Long =
        store.data.first()[Keys.NEWS_FETCHED_AT] ?: 0L

    suspend fun setNewsFetchedAt(millis: Long) {
        store.edit { it[Keys.NEWS_FETCHED_AT] = millis }
    }

    /** 선택한 러닝 코스 id. -1이면 선택 없음 */
    val selectedCourseId: Flow<Long> = store.data.map { it[Keys.SELECTED_COURSE] ?: -1L }

    suspend fun setSelectedCourse(id: Long) {
        store.edit { it[Keys.SELECTED_COURSE] = id }
    }

    suspend fun selectedCourseNow(): Long = store.data.first()[Keys.SELECTED_COURSE] ?: -1L

    // ── 코스 기록으로 낼 러닝 ─────────────────────────────────
    //
    // 코스를 완주한 러닝은 서버에 올라간 **뒤에** 코스 기록으로 낸다(서버가 올라온
    // 경로로 코스를 따라갔는지 본다). 그 사이 앱이 꺼져도 잊지 않게 여기 적어 둔다.

    /** 이 러닝이 끝나면 [track] 코스의 기록으로 낸다 */
    suspend fun addPendingCourseRun(startedAt: Long, track: String) {
        if (track.isBlank()) return
        store.edit {
            val rows = decodePendingRuns(it[Keys.PENDING_COURSE_RUNS]).toMutableMap()
            rows[startedAt] = track
            // This is an outbox, not recent-history UI. Never discard an unacknowledged run.
            it[Keys.PENDING_COURSE_RUNS] = rows.entries.sortedBy { e -> e.key }
                .joinToString("\n") { e -> "${e.key}\t${e.value}" }
        }
    }

    /** Read without consuming: a failed or account-mismatched request must retain it. */
    suspend fun pendingCourseRun(startedAt: Long): String? =
        decodePendingRuns(store.data.first()[Keys.PENDING_COURSE_RUNS])[startedAt]

    /** Remove only after the server acknowledges this run. */
    suspend fun takePendingCourseRun(startedAt: Long): String? {
        var found: String? = null
        store.edit {
            val rows = decodePendingRuns(it[Keys.PENDING_COURSE_RUNS]).toMutableMap()
            found = rows.remove(startedAt)
            if (found != null) {
                it[Keys.PENDING_COURSE_RUNS] = rows.entries.joinToString("\n") { e -> "${e.key}\t${e.value}" }
            }
        }
        return found
    }

    private fun decodePendingRuns(raw: String?): Map<Long, String> =
        raw.orEmpty().lineSequence().mapNotNull { line ->
            val tab = line.indexOf('\t')
            if (tab <= 0) return@mapNotNull null
            val at = line.substring(0, tab).toLongOrNull() ?: return@mapNotNull null
            at to line.substring(tab + 1)
        }.toMap()

    /**
     * 코스 녹화 중인가 — "코스 만들기"를 누르고 아직 저장하지 않은 상태.
     *
     * 화면이 아니라 여기에 두는 이유는 러닝이 전경 서비스에서 돌기 때문이다.
     * 러닝 중에 앱이 메모리에서 내려가도, 돌아왔을 때 "아, 이건 코스 녹화였지"를
     * 기억하고 있어야 끝나고 저장 창을 띄울 수 있다.
     */
    val courseRecording: Flow<Boolean> =
        store.data.map { it[Keys.COURSE_RECORDING] ?: false }

    suspend fun setCourseRecording(on: Boolean) {
        store.edit { it[Keys.COURSE_RECORDING] = on }
    }

    suspend fun courseSeedVersion(): Int =
        store.data.first()[Keys.COURSE_SEED_VERSION] ?: 0

    suspend fun setCourseSeedVersion(version: Int) {
        store.edit { it[Keys.COURSE_SEED_VERSION] = version }
    }

    // ── 적립 정산 기준점 ─────────────────────────────────────────

    /**
     * 오늘 이미 정산에 반영한 걸음 수.
     *
     * 백그라운드 적립과 러닝 세션 정산이 같은 걸음을 두 번 세지 않게 하는
     * 기준점이다. 세션이 끝나면 그 세션의 걸음이 여기 더해지고, 백그라운드
     * 스윕은 "오늘 걸음 − 이 값"만 정산한다. 날짜가 바뀌면 0에서 시작한다.
     */
    suspend fun accountedStepsToday(today: Long): Int {
        val prefs = store.data.first()
        val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
        if (day != today) return 0
        return (prefs[Keys.ACCOUNTED_STEPS] ?: 0L).toInt()
    }

    /** 정산한 걸음을 기준점에 더한다. 날짜가 바뀌었으면 오늘치부터 다시 센다. */
    suspend fun addAccountedSteps(today: Long, steps: Int) {
        if (steps <= 0) return
        store.edit { prefs ->
            val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
            val base = if (day == today) prefs[Keys.ACCOUNTED_STEPS] ?: 0L else 0L
            prefs[Keys.ACCOUNTED_DAY] = today
            prefs[Keys.ACCOUNTED_STEPS] = base + steps
        }
    }

    /**
     * 기준점을 [floorValue] 이상으로 끌어올린다. 이미 그보다 크면 두고, 절대 내리지 않는다.
     *
     * 세션 정산이 쓰는 경로다. 세션 걸음을 그냥 더하면 두 가지가 깨진다.
     *  - **자정을 넘긴 세션**: 어제 걸은 몫까지 오늘 기준점에 얹혀, 오늘 처음 걷는
     *    그만큼이 통째로 사라진다.
     *  - **일시정지 중 걸음**: 세션 걸음에 안 잡히므로 더하기만으로는 기준점이
     *    모자라고, 그 몫을 백그라운드가 다시 지급한다.
     *
     * 두 경우 모두 "지금 이 순간의 오늘 걸음 수"를 기준점으로 못 박으면 해결된다.
     */
    suspend fun raiseAccountedTo(today: Long, floorValue: Int) {
        if (floorValue <= 0) return
        store.edit { prefs ->
            val day = prefs[Keys.ACCOUNTED_DAY] ?: -1L
            val base = if (day == today) prefs[Keys.ACCOUNTED_STEPS] ?: 0L else 0L
            prefs[Keys.ACCOUNTED_DAY] = today
            prefs[Keys.ACCOUNTED_STEPS] = maxOf(base, floorValue.toLong())
        }
    }

    // ── 랭킹 재료 ────────────────────────────────────────────────

    /** 역대 최고 속도(km/h). 러닝 판정을 통과한 구간에서만 갱신된다. */
    val topSpeedKmh: Flow<Double> = store.data.map { it[Keys.TOP_SPEED] ?: 0.0 }

    suspend fun recordTopSpeed(kmh: Double) {
        if (kmh <= 0.0) return
        store.edit { prefs ->
            val best = prefs[Keys.TOP_SPEED] ?: 0.0
            if (kmh > best) prefs[Keys.TOP_SPEED] = kmh
        }
    }

    /** 온보딩 가이드를 끝까지 봤는지 */
    val guideSeen: Flow<Boolean> = store.data.map { (it[Keys.GUIDE_SEEN] ?: 0) == 1 }

    suspend fun setGuideSeen() {
        store.edit { it[Keys.GUIDE_SEEN] = 1 }
    }

    /** 러닝 권한 안내(S2)를 한 번 봤는지 — 걸음 권한이 없을 때는 이 값과 상관없이 다시 보인다 */
    val runPermissionPrimerSeen: Flow<Boolean> = store.data.map { it[Keys.RUN_PERMISSION_PRIMER_SEEN] ?: false }

    suspend fun setRunPermissionPrimerSeen() {
        store.edit { it[Keys.RUN_PERMISSION_PRIMER_SEEN] = true }
    }

    /** S2 첫 설정(신체 정보 · 목표 · 모드)을 끝냈거나 건너뛰었는지 — 새로 가입한 사람에게만 한 번 보인다 */
    val s2SetupSeen: Flow<Boolean> = store.data.map { it[Keys.S2_SETUP_SEEN] ?: false }

    suspend fun setS2SetupSeen() {
        store.edit { it[Keys.S2_SETUP_SEEN] = true }
    }

    /** 날씨에 맞춰 홈 풍경 바꾸기 — 대략적인 위치를 날씨 서비스에 보내므로 사용자가 켜야 동작한다 */
    val weatherBackground: Flow<Boolean> = store.data.map { it[Keys.WEATHER_BACKGROUND] ?: false }

    suspend fun setWeatherBackground(on: Boolean) {
        store.edit { it[Keys.WEATHER_BACKGROUND] = on }
    }

    /** 홈 구성 모드. 적립 규칙과는 상관없다. */
    val runMode: Flow<com.stepup.android.domain.RunMode> = store.data.map { prefs ->
        prefs[Keys.RUN_MODE]?.let { raw ->
            com.stepup.android.domain.RunMode.entries.firstOrNull { it.name == raw }
        } ?: com.stepup.android.domain.RunMode.LITE
    }

    suspend fun setRunMode(mode: com.stepup.android.domain.RunMode) {
        store.edit { it[Keys.RUN_MODE] = mode.name }
    }

    /** 신체 정보와 목표 — 이 기기에만 둔다 */
    val bodyProfile: Flow<com.stepup.android.domain.BodyProfile> = store.data.map {
        com.stepup.android.domain.BodyProfile(
            heightCm = it[Keys.BODY_HEIGHT_CM],
            weightKg = it[Keys.BODY_WEIGHT_KG],
            goalWeightKg = it[Keys.BODY_GOAL_WEIGHT_KG],
            goalWeeks = it[Keys.BODY_GOAL_WEEKS],
        )
    }

    suspend fun setBodyProfile(profile: com.stepup.android.domain.BodyProfile) {
        store.edit { prefs ->
            fun <T> put(key: Preferences.Key<T>, value: T?) { if (value == null) prefs.remove(key) else prefs[key] = value }
            put(Keys.BODY_HEIGHT_CM, profile.heightCm)
            put(Keys.BODY_WEIGHT_KG, profile.weightKg)
            put(Keys.BODY_GOAL_WEIGHT_KG, profile.goalWeightKg)
            put(Keys.BODY_GOAL_WEEKS, profile.goalWeeks)
        }
    }

    /**
     * 첫 실행 시 러너 UID를 발급한다. 이미 있으면 그 값을 반환한다.
     * 헷갈리는 문자(0/O, 1/I)를 뺀 32문자 알파벳을 쓴다.
     */
    suspend fun ensureRunnerUid(): String {
        val existing = store.data.first()[Keys.RUNNER_UID]
        if (!existing.isNullOrBlank()) return existing
        val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = java.security.SecureRandom()
        val body = buildString {
            repeat(6) { append(alphabet[random.nextInt(alphabet.length)]) }
        }
        val uid = "SU-$body"
        store.edit { it[Keys.RUNNER_UID] = uid }
        return uid
    }

    val sneakerLevel: Flow<Int> = store.data.map { it[Keys.SNEAKER_LEVEL] ?: 1 }

    val streak: Flow<Int> = store.data.map { it[Keys.STREAK] ?: 0 }

    /**
     * 표시용 에너지. 저장된 날짜가 오늘이 아니면 아직 소모가 없는 것이므로
     * 최대치(= 자정 리필 후 값)로 보여준다. 실제 저장값 갱신은 [currentEnergy]가 담당.
     */
    val energy: Flow<Double> = store.data.map { prefs ->
        val max = energyMax(prefs)
        val day = prefs[Keys.ENERGY_DAY] ?: -1L
        if (day != energyToday(prefs)) max else (prefs[Keys.ENERGY] ?: max).coerceIn(0.0, max)
    }

    /**
     * 에너지가 다시 차는 "오늘". 서버 경제에서는 서버의 하루(한국 시간 자정)를 따른다 —
     * 폰의 자정으로 채우면 해외에서는 서버에 없는 에너지가 가득 찬 것처럼 보인다.
     */
    private fun energyToday(prefs: Preferences): Long =
        if (prefs[Keys.SERVER_ENERGY_MAX] != null) LocalDate.now(SERVER_ZONE).toEpochDay()
        else LocalDate.now().toEpochDay()

    suspend fun setDailyGoal(goal: Int) {
        store.edit { it[Keys.DAILY_GOAL] = goal }
    }

    suspend fun setSneakerLevel(level: Int) {
        store.edit { it[Keys.SNEAKER_LEVEL] = level }
    }

    /**
     * 오늘 남은 에너지를 반환한다. 날짜가 바뀌었으면 최대치로 리필해 저장한다.
     *
     * 리필도 edit 안에서 읽고 쓴다. 바깥에서 읽고 안에서 쓰면 자정 직후
     * 동시에 들어온 소모가 리필에 덮여 사라진다.
     */
    suspend fun currentEnergy(today: Long): Double {
        var remaining = 0.0
        store.edit { prefs ->
            remaining = energyIn(prefs, today)
            if ((prefs[Keys.ENERGY_DAY] ?: -1L) != today) {
                prefs[Keys.ENERGY] = remaining
                prefs[Keys.ENERGY_DAY] = today
            }
        }
        return remaining
    }

    /**
     * 에너지 상한을 정하는 레벨을 적는다 — 착용 신발이 바뀌거나 강화될 때.
     *
     * 화면(홈·러닝)은 착용 신발 레벨로 상한을 보여 주는데, 소모·리필이 옛 레벨 값(기본 1)을
     * 쓰면 강화해도 실제로 벌 수 있는 걸음이 늘지 않는다. 신발이 없으면 null.
     */
    suspend fun setEnergyCapLevel(level: Int?) {
        store.edit { if (level == null) it.remove(Keys.ENERGY_CAP_LEVEL) else it[Keys.ENERGY_CAP_LEVEL] = level }
    }

    private fun energyMax(prefs: Preferences): Double =
        prefs[Keys.SERVER_ENERGY_MAX]
            ?: RewardEconomy.maxEnergy(prefs[Keys.ENERGY_CAP_LEVEL] ?: prefs[Keys.SNEAKER_LEVEL] ?: 1)

    // ── 서버 경제의 사본 (EconomySync 가 적는다) ─────────────────────
    //
    // 에너지 · 뽑기 횟수는 서버가 정한다. 폰은 마지막으로 받아 온 값을 보여 줄 뿐이다.

    /** @param today 서버의 오늘(my_economy.game_day, 한국 시간) */
    suspend fun setServerEconomy(today: Long, energyLeft: Double, energyMax: Double, freeDraws: Int, bonusDraws: Int) {
        store.edit {
            it[Keys.ENERGY] = energyLeft.coerceAtLeast(0.0)
            it[Keys.ENERGY_DAY] = today
            it[Keys.SERVER_ENERGY_MAX] = energyMax
            it[Keys.FREE_DRAWS_LEFT] = freeDraws.coerceAtLeast(0)
            it[Keys.BONUS_DRAWS_LEFT] = bonusDraws.coerceAtLeast(0)
        }
    }

    /** 서버 경제의 사본을 지운다(계정 삭제) — 에너지는 폰 기본값으로 돌아간다 */
    suspend fun clearServerEconomy() {
        store.edit {
            it.remove(Keys.SERVER_ENERGY_MAX)
            it.remove(Keys.FREE_DRAWS_LEFT)
            it.remove(Keys.BONUS_DRAWS_LEFT)
            it.remove(Keys.ENERGY)
            it.remove(Keys.ENERGY_DAY)
        }
    }

    /** 남은 무료 뽑기 (서버 draw_grants FREE) */
    val freeDrawsLeft: Flow<Int> = store.data.map { it[Keys.FREE_DRAWS_LEFT] ?: 0 }

    /** 남은 보너스 뽑기 — 지갑 연결로 받은 것. 지갑 페이지에서 뽑는다 */
    val bonusDrawsLeft: Flow<Int> = store.data.map { it[Keys.BONUS_DRAWS_LEFT] ?: 0 }

    /** 폰의 서버 경제 사본이 어느 계정 것인가 — 다른 계정으로 로그인하면 사본을 먼저 지운다 */
    suspend fun economyOwner(): String? = store.data.map { it[Keys.ECONOMY_OWNER] }.first()

    suspend fun setEconomyOwner(userId: String?) {
        store.edit { if (userId == null) it.remove(Keys.ECONOMY_OWNER) else it[Keys.ECONOMY_OWNER] = userId }
    }

    /** 폰에만 있던 옛 신발을 이 계정으로 한 번 올렸는가 */
    suspend fun legacyEconomyImported(userId: String): Boolean =
        store.data.map { userId in (it[Keys.LEGACY_ECONOMY_IMPORTED] ?: "").split(',') }.first()

    suspend fun setLegacyEconomyImported(userId: String) {
        store.edit {
            val done = (it[Keys.LEGACY_ECONOMY_IMPORTED] ?: "").split(',').filter(String::isNotBlank).toSet()
            it[Keys.LEGACY_ECONOMY_IMPORTED] = (done + userId).joinToString(",")
        }
    }

    /**
     * 에너지를 소모한다.
     *
     * 읽기와 쓰기를 한 [edit] 블록 안에서 끝낸다. DataStore는 edit를 직렬화하므로
     * 이렇게 해야 원자적이다. 백그라운드 적립이 생기면서 세션 정산과 동시에
     * 에너지를 건드리는 일이 일상이 됐고, 바깥에서 읽고 안에서 쓰면 갱신이
     * 유실되어 상한이 새거나 구매한 에너지 셀이 사라진다.
     */
    suspend fun consumeEnergy(today: Long, amount: Double) {
        store.edit { prefs ->
            val remaining = energyIn(prefs, today)
            prefs[Keys.ENERGY] = (remaining - amount).coerceAtLeast(0.0)
            prefs[Keys.ENERGY_DAY] = today
        }
    }

    /** Receipt and debit are one durable preference edit, safe to replay after a Room-ack failure. */
    suspend fun consumeRunEnergy(receiptId: String, energyDay: Long, amount: Double, today: Long = LocalDate.now().toEpochDay()) {
        require(receiptId.isNotBlank() && amount.isFinite() && amount >= 0)
        // 영수증 날짜가 오늘보다 뒤(폰 날짜를 되돌림 · 서쪽으로 이동)면 오늘 에너지에서 빼지 않고 받은 것으로만
        // 적는다 — 거절하면 이 영수증이 남아 그 뒤 모든 러닝의 저장이 날짜가 따라잡을 때까지 막힌다
        store.edit { prefs ->
            val receipts = prefs[Keys.RUN_ENERGY_RECEIPTS].orEmpty()
            if (receiptId !in receipts) {
                // A later day's refill supersedes the old day's consumption. Do not debit it twice.
                if (energyDay == today) {
                    prefs[Keys.ENERGY] = (energyIn(prefs, today) - amount).coerceAtLeast(0.0)
                    prefs[Keys.ENERGY_DAY] = today
                }
                prefs[Keys.RUN_ENERGY_RECEIPTS] = receipts + receiptId
            }
        }
    }

    /** 에너지 셀 등으로 에너지를 회복한다. 최대치를 넘지 않는다. */
    suspend fun restoreEnergy(today: Long, amount: Double) {
        store.edit { prefs ->
            val remaining = energyIn(prefs, today)
            val max = energyMax(prefs)
            prefs[Keys.ENERGY] = (remaining + amount).coerceIn(0.0, max)
            prefs[Keys.ENERGY_DAY] = today
        }
    }

    /** Applying a receipt and remembering it are one durable edit, including on replay. */
    suspend fun hasEnergyCapacity(today: Long, amount: Double): Boolean {
        require(amount.isFinite() && amount > 0)
        val prefs = store.data.first()
        return energyMax(prefs) - energyIn(prefs, today) >= amount
    }

    suspend fun restorePurchasedEnergy(receiptId: String, today: Long, amount: Double): Boolean {
        require(receiptId.isNotBlank() && amount.isFinite() && amount > 0)
        var applied = false
        store.edit { prefs ->
            val receipts = prefs[Keys.ENERGY_RECEIPTS].orEmpty()
            if (receiptId in receipts) {
                applied = true
            } else {
                val max = energyMax(prefs)
                val remaining = energyIn(prefs, today)
                if (max - remaining >= amount) {
                    prefs[Keys.ENERGY] = remaining + amount
                    prefs[Keys.ENERGY_DAY] = today
                    prefs[Keys.ENERGY_RECEIPTS] = receipts + receiptId
                    applied = true
                }
            }
        }
        return applied
    }

    /** 자정 리필을 반영한 현재 에너지. [consumeEnergy]/[restoreEnergy]가 edit 안에서 쓴다. */
    private fun energyIn(prefs: Preferences, today: Long): Double {
        val day = prefs[Keys.ENERGY_DAY] ?: -1L
        val max = energyMax(prefs)
        return if (day != today) max else (prefs[Keys.ENERGY] ?: max).coerceIn(0.0, max)
    }

    suspend fun streakValue(): Int = store.data.first()[Keys.STREAK] ?: 0

    suspend fun lastGoalMetDay(): Long = store.data.first()[Keys.LAST_GOAL_MET_DAY] ?: -1L

    suspend fun setGoalMet(day: Long, newStreak: Int) {
        store.edit {
            it[Keys.LAST_GOAL_MET_DAY] = day
            it[Keys.STREAK] = newStreak
        }
    }

    /** 걸음 센서 기준점 (기준 날짜 epochDay, 그 시점의 센서 누적값) */
    suspend fun baseline(): Pair<Long, Long> {
        val prefs = store.data.first()
        return (prefs[Keys.BASELINE_DAY] ?: -1L) to (prefs[Keys.BASELINE_STEPS] ?: -1L)
    }

    /** 기준점을 잡을 때의 부팅 횟수. 모르면 -1 */
    suspend fun baselineBoot(): Int = store.data.first()[Keys.BASELINE_BOOT] ?: -1

    suspend fun setBaseline(day: Long, steps: Long, boot: Int) {
        store.edit {
            it[Keys.BASELINE_DAY] = day
            it[Keys.BASELINE_STEPS] = steps
            it[Keys.BASELINE_BOOT] = boot
        }
    }

    companion object {
        const val DEFAULT_GOAL = 8000
        const val MIN_GOAL = 3000
        const val MAX_GOAL = 20000

        /** 쉼표로 이어 붙인 id 문자열을 목록으로. 비었거나 깨진 값은 건너뛴다. */
        private fun String?.toIdList(): List<Long> =
            this?.split(",")?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()

        /** 닉네임 최대 길이. 순위표 한 줄에 들어가야 해서 짧게 잡는다. */
        const val NICKNAME_MAX = 16

        /** avatarId가 이 값이면 갤러리에서 고른 사진을 쓴다 */
        const val AVATAR_CUSTOM = -2

        /** 갤러리 아바타 저장 파일명 (filesDir) */
        const val AVATAR_FILE = "avatar_custom.jpg"
    }
}

/** 받을 푸시 종류 — 알림 설정 화면의 네 가지 */
data class NotifyPrefs(
    val push: Boolean = true,
    val goalReminder: Boolean = true,
    val partyInvite: Boolean = true,
    val eventNews: Boolean = true,
)

/** 이 앱이 이 폰에서 업데이트된 설치인가 (백업 복원으로 막 설치된 것이 아닌가) */
private fun updatedInPlace(context: Context): Boolean = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    info.lastUpdateTime > info.firstInstallTime
}.getOrDefault(false)

/** 서버의 하루 기준 (economy.game_day) */
private val SERVER_ZONE: java.time.ZoneId = java.time.ZoneId.of("Asia/Seoul")
