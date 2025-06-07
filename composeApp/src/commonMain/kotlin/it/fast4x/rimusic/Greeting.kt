package it.fast4x.rimusic

class Greeting {
    private val platform = it.fast4x.rimusic.getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}