package com.soooool.matedash.data.media

import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPMediaItemPropertyArtist

actual fun getNowPlaying(): NowPlayingInfo? {
    val center = MPNowPlayingInfoCenter.defaultCenter()
    val info = center.nowPlayingInfo ?: return null
    val title = info[MPMediaItemPropertyTitle] as? String ?: return null
    if (title.isBlank()) return null
    val artist = info[MPMediaItemPropertyArtist] as? String ?: ""
    return NowPlayingInfo(
        title = title,
        artist = artist,
        isPlaying = true,
    )
}
