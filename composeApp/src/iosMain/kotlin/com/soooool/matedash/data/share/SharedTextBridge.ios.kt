package com.soooool.matedash.data.share

import platform.Foundation.NSUserDefaults

private const val SUITE_NAME = "group.com.soooool.matedash"
private const val KEY_SHARE_RAW = "share_text_raw"
private const val KEY_SHARE_UPDATED = "share_text_updated"

actual fun readSharedText(): String? {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return null
    val raw = defaults.stringForKey(KEY_SHARE_RAW)
    return if (raw.isNullOrBlank()) null else raw
}

actual fun clearSharedText() {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return
    defaults.removeObjectForKey(KEY_SHARE_RAW)
    defaults.removeObjectForKey(KEY_SHARE_UPDATED)
    defaults.synchronize()
}

actual fun writeTestSharedText(text: String) {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return
    defaults.setObject(text, forKey = KEY_SHARE_RAW)
    val now = platform.Foundation.NSDate()
    defaults.setDouble(now.timeIntervalSinceReferenceDate, forKey = KEY_SHARE_UPDATED)
    defaults.synchronize()
}
