package com.littleetx.sustechnav

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform