package com.soooool.matedash.data.crypto

import java.security.MessageDigest

actual fun sha256(input: ByteArray): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(input)
}
