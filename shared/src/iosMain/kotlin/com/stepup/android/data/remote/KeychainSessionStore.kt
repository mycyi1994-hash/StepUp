@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package com.stepup.android.data.remote

import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryAddValue
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFMutableDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFTypeDictionaryKeyCallBacks
import platform.CoreFoundation.kCFTypeDictionaryValueCallBacks
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

/**
 * iOS 의 로그인 세션 보관 — 키체인(이 기기에만, 첫 잠금 해제 뒤 — 러닝 중 백그라운드에서도 토큰을 갱신할 수 있게).
 * 안드로이드는 앱 설정 저장소(UserPrefs)에 둔다. 앱을 지워도 키체인은 남는다 — 새로 깐 첫 실행에 [clear] 하는 것은 앱(iosApp)이 한다.
 */
class KeychainSessionStore(
    private val service: String = "com.stepupcrew.stepup.auth",
    private val account: String = "session",
) : AuthSessionStore {

    override suspend fun load(): AuthSession? {
        val text = read() ?: return null
        return runCatching { serverJson.decodeFromString(AuthSession.serializer(), text) }.getOrNull()
    }

    override suspend fun save(session: AuthSession) {
        write(serverJson.encodeToString(AuthSession.serializer(), session))
    }

    override suspend fun clear() {
        delete()
    }

    /** [clear] 와 같다 — 앱 시작처럼 기다릴 수 없는 곳에서 */
    fun wipe() = delete()

    /** 키체인 물음 하나 — 서비스 · 계정 + [extra]. CF 값은 쓰고 나서 놓는다 */
    private inline fun <T> withQuery(extra: List<Pair<CFTypeRef?, CFTypeRef?>>, block: (CFMutableDictionaryRef?) -> T): T {
        val serviceRef = CFBridgingRetain(service as NSString)
        val accountRef = CFBridgingRetain(account as NSString)
        val pairs = listOf<Pair<CFTypeRef?, CFTypeRef?>>(
            kSecClass to kSecClassGenericPassword,
            kSecAttrService to serviceRef,
            kSecAttrAccount to accountRef,
        ) + extra
        val dict = CFDictionaryCreateMutable(null, pairs.size.convert(), kCFTypeDictionaryKeyCallBacks.ptr, kCFTypeDictionaryValueCallBacks.ptr)
        pairs.forEach { (k, v) -> CFDictionaryAddValue(dict, k, v) }
        try {
            return block(dict)
        } finally {
            CFRelease(dict)
            CFRelease(serviceRef)
            CFRelease(accountRef)
        }
    }

    private fun read(): String? = memScoped {
        val result = alloc<CFTypeRefVar>()
        val status = withQuery(listOf(kSecReturnData to kCFBooleanTrue, kSecMatchLimit to kSecMatchLimitOne)) {
            SecItemCopyMatching(it, result.ptr)
        }
        if (status != errSecSuccess) return null
        val data = CFBridgingRelease(result.value) as? NSData ?: return null
        NSString.create(data, NSUTF8StringEncoding)?.toString()
    }

    private fun write(text: String) {
        delete()
        val data = (text as NSString).dataUsingEncoding(NSUTF8StringEncoding) ?: return
        val dataRef = CFBridgingRetain(data)
        try {
            withQuery(listOf(kSecValueData to dataRef, kSecAttrAccessible to kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)) {
                SecItemAdd(it, null)
            }
        } finally {
            CFRelease(dataRef)
        }
    }

    private fun delete() {
        withQuery(emptyList()) { SecItemDelete(it) }
    }
}
