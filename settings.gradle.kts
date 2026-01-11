enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Cubic-Music"

/* =========================
   Main App
   ========================= */
include(":composeApp")

/* =========================
   Extensions
   ========================= */
include(":oldtube")
project(":oldtube").projectDir = file("extensions/innertube")

include(":ktor-client-brotli")
project(":ktor-client-brotli").projectDir = file("extensions/ktor-client-brotli")

include(":kugou")
project(":kugou").projectDir = file("extensions/kugou")

include(":lrclib")
project(":lrclib").projectDir = file("extensions/lrclib")

include(":piped")
project(":piped").projectDir = file("extensions/piped")

include(":invidious")
project(":invidious").projectDir = file("extensions/invidious")

include(":genius")
project(":genius").projectDir = file("extensions/genius")

 //submodules
include(":innertube")
project(":innertube").projectDir = file("modules/innertube")