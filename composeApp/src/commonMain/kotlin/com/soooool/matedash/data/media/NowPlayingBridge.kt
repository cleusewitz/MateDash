package com.soooool.matedash.data.media

data class NowPlayingInfo(
    val title: String,
    val artist: String,
    val isPlaying: Boolean = false,
)

expect fun getNowPlaying(): NowPlayingInfo?
