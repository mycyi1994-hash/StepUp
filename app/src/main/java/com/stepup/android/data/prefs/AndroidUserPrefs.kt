package com.stepup.android.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

// 저장소 이름은 옛 이름 그대로 둔다 — 바꾸면 이미 설치된 기기의 설정과
// 로그인 세션이 사라진다. ServiceLocator 의 DB 파일명과 같은 이유다.
private val Context.dataStore by preferencesDataStore(name = "strideup_prefs")

// 로그인 세션만 따로 둔 파일. 백업에서 이 파일만 뺀다(res/xml/backup_rules ·
// data_extraction_rules) — 백업 파일을 손에 넣은 사람이 로그인까지 가져가지 못하게.
// 설정 파일(strideup_prefs)은 그대로 백업된다: 통째로 빼면 거래소 커서가 사라져
// 거래 줄이 다시 들어온다.
private val Context.authDataStore by preferencesDataStore(name = "stepup_auth")

/**
 * 안드로이드의 설정 저장소 — 공용 UserPrefs(:sharedData, 2026-11-03 KMP 3단계)에 예전과 같은 파일 둘을 넣는다.
 * 예전 생성자 UserPrefs(context, store, authStore, legacySessionAdoptable) 와 같은 모양이라 부르는 곳은 그대로다.
 */
fun UserPrefs(
    context: Context,
    store: DataStore<Preferences> = context.dataStore,
    authStore: DataStore<Preferences> = context.authDataStore,
    legacySessionAdoptable: () -> Boolean = { updatedInPlace(context) },
): UserPrefs = UserPrefs(store = store, authStore = authStore, legacySessionAdoptable = legacySessionAdoptable)

/** 이 앱이 이 폰에서 업데이트된 설치인가 (백업 복원으로 막 설치된 것이 아닌가) */
private fun updatedInPlace(context: Context): Boolean = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    info.lastUpdateTime > info.firstInstallTime
}.getOrDefault(false)
