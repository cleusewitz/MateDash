package com.soooool.matedash.data.share

actual fun readSharedText(): String? = null

actual fun clearSharedText() {}

actual fun writeTestSharedText(text: String) {}

actual fun readShareExtensionLog(): List<String> = emptyList()
