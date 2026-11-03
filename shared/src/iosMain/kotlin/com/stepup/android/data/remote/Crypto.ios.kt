@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.stepup.android.data.remote

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.Security.SecRandomCopyBytes
import platform.Security.errSecSuccess
import platform.Security.kSecRandomDefault

actual fun secureRandomBytes(size: Int): ByteArray {
    val out = ByteArray(size)
    if (size == 0) return out
    val status = out.usePinned { SecRandomCopyBytes(kSecRandomDefault, size.convert(), it.addressOf(0)) }
    check(status == errSecSuccess) { "SecRandomCopyBytes $status" }
    return out
}

@OptIn(ExperimentalUnsignedTypes::class)
actual fun sha256(data: ByteArray): ByteArray {
    val out = UByteArray(CC_SHA256_DIGEST_LENGTH)
    out.usePinned { o ->
        if (data.isEmpty()) CC_SHA256(null, 0u, o.addressOf(0))
        else data.usePinned { d -> CC_SHA256(d.addressOf(0), data.size.convert(), o.addressOf(0)) }
    }
    return out.toByteArray()
}
