package com.soooool.matedash

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform