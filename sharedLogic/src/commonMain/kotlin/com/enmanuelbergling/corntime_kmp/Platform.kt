package com.enmanuelbergling.corntime_kmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform