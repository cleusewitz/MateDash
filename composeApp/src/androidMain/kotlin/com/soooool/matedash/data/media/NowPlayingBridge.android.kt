package com.soooool.matedash.data.media

actual fun getNowPlaying(): NowPlayingInfo? {
    // Android에서는 NotificationListenerService 권한이 필요하므로 추후 구현
    return null
}
