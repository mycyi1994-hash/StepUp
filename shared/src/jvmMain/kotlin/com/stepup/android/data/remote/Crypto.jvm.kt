package com.stepup.android.data.remote

actual fun secureRandomBytes(size: Int): ByteArray = ByteArray(size).also { java.security.SecureRandom().nextBytes(it) }

actual fun sha256(data: ByteArray): ByteArray = java.security.MessageDigest.getInstance("SHA-256").digest(data)
