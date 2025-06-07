package it.fast4x.rimusic

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform