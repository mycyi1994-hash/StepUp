@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.data.prefs

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS 의 설정 저장소 — 앱 문서 폴더에 안드로이드와 같은 이름의 파일 둘(strideup_prefs · stepup_auth).
 * 한 앱에서 한 번만 만든다(DataStore 는 파일 하나에 하나만). 로그인 세션은 키체인(KeychainSessionStore)에 두므로
 * 옛 설정 파일의 세션을 이어받을 일이 없다(legacySessionAdoptable = false).
 */
fun createIosUserPrefs(): UserPrefs {
    val dir = NSFileManager.defaultManager.URLForDirectory(NSDocumentDirectory, NSUserDomainMask, null, true, null)?.path
        ?: error("문서 폴더를 찾지 못했습니다")
    return UserPrefs(
        store = PreferenceDataStoreFactory.createWithPath(produceFile = { "$dir/strideup_prefs.preferences_pb".toPath() }),
        authStore = PreferenceDataStoreFactory.createWithPath(produceFile = { "$dir/stepup_auth.preferences_pb".toPath() }),
        legacySessionAdoptable = { false },
    )
}
