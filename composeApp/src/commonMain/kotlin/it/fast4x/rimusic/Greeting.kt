package app.it.fast4x.rimusic

class Greeting {
    private val platform = app.it.fast4x.rimusic.getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}