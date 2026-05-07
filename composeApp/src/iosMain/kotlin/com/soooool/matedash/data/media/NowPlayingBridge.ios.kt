package com.soooool.matedash.data.media

import platform.Foundation.NSUserDefaults

private const val SUITE_NAME = "group.com.soooool.matedash"
private const val KEY_TITLE = "now_playing_title"
private const val KEY_ARTIST = "now_playing_artist"
private const val KEY_IS_PLAYING = "now_playing_is_playing"

actual fun getNowPlaying(): NowPlayingInfo? {
    val defaults = NSUserDefaults(suiteName = SUITE_NAME) ?: return null
    val title = defaults.stringForKey(KEY_TITLE)?.takeIf { it.isNotBlank() } ?: return null
    val artist = defaults.stringForKey(KEY_ARTIST) ?: ""
    val isPlaying = defaults.boolForKey(KEY_IS_PLAYING)
    return NowPlayingInfo(title = title, artist = artist, isPlaying = isPlaying)
}
