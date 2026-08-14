import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val APP_NAME = "Cubic-Music"
val DESKTOP_APP_NAME = "Cubic Music"

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun localProperty(name: String, fallback: String = ""): String =
    (localProperties.getProperty(name)
        ?: providers.environmentVariable(name.uppercase()).orNull
        ?: fallback)
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")

plugins {
    // Multiplatform
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)

    // Android
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)


    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-receivers")
            freeCompilerArgs.add("-Xexpect-actual-classes")
        }
    }

    jvm("desktop")



    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val desktopMain by getting
        desktopMain.kotlin.exclude("it/fast4x/rimusic/ui/ThreeColumnsApp.kt")
        desktopMain.kotlin.exclude("it/fast4x/rimusic/ui/DesktopApp.kt")
        desktopMain.kotlin.exclude("it/fast4x/rimusic/player/vlcj/VlcjFrameController.kt")
        desktopMain.kotlin.exclude("it/fast4x/rimusic/ui/screens/ArtistScreen.kt")
        desktopMain.kotlin.exclude("it/fast4x/rimusic/ui/components/LayoutWithAdaptiveThumbnail.kt")
        desktopMain.kotlin.exclude("it/fast4x/rimusic/ui/components/PlayerEssential.kt")
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)
            implementation(projects.oldtube)
            implementation(projects.lrclib)
            implementation(libs.newpipe.extractor)
            implementation(libs.nanojson)
            implementation("org.openjfx:javafx-base:19:win")
            implementation("org.openjfx:javafx-graphics:19:win")
            implementation("org.openjfx:javafx-controls:19:win")
            implementation("org.openjfx:javafx-media:19:win")
            implementation("org.openjfx:javafx-web:19:win")

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)
            implementation("ws.schild:jave-core:3.5.0")
            runtimeOnly("ws.schild:jave-nativebin-win64:3.5.0")
            implementation(libs.hypnoticcanvas)
            implementation(libs.hypnoticcanvas.shaders)

            implementation(libs.coil.network.okhttp)
            runtimeOnly(libs.kotlinx.coroutines.swing)

            /*
            // Uncomment only for build jvm desktop version
            // Comment before build android version
            configurations.commonMainApi {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
            */
        }

      androidMain.dependencies {
    // OkHttp dependencies
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    
    // Ktor OkHttp engine (THIS WAS MISSING)
    implementation(libs.ktor.client.okhttp)
    
    // Your existing dependencies
    implementation(libs.media3.session)
    implementation(libs.kotlinx.coroutines.guava)
    implementation("com.google.guava:guava:31.1-android")
    implementation(libs.newpipe.extractor)
    implementation(libs.nanojson)
    implementation(libs.androidx.webkit)
    implementation(libs.compose.runtime.livedata)
    implementation(libs.compose.activity)
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation(projects.betterlyrics)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.monetcompat)
    implementation(libs.androidmaterial)
    implementation(libs.androidx.crypto)
    implementation(libs.toasty)
    implementation(libs.androidyoutubeplayer)
    implementation(libs.androidx.glance.widgets)
    implementation(libs.kizzy.rpc)
}
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(projects.oldtube)
            implementation(projects.piped)
            implementation(projects.invidious)

            implementation(libs.room)
            implementation(libs.room.runtime)
            implementation(libs.room.sqlite.bundled)

            implementation(libs.mediaplayer.kmp)

            implementation(libs.navigation.kmp)
            //coil3 mp
            implementation(libs.coil.compose.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.mp)
            implementation(libs.coil.network.okhttp)
            // Direct OkHttp dependency
            implementation("com.squareup.okhttp3:okhttp:5.1.0")
            implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
            implementation(libs.translator)

        }
    }
}

android {
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    androidComponents {
        beforeVariants(selector().withBuildType("release")) {
            it.enable = false
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileSdk = 35

    defaultConfig {
        applicationId = "com.Cubic.music"
        minSdk = 23
        targetSdk = 36
        versionCode = 110
        versionName = "1.9.7"

        /*
                UNIVERSAL VARIABLES
         */
        buildConfigField( "Boolean", "IS_AUTOUPDATE", "true" )
        buildConfigField( "String", "APP_NAME", "\"$APP_NAME\"" )
        buildConfigField( "String", "WEATHER_API_KEY", "\"${localProperty("weather_api_key")}\"" )
        buildConfigField( "String", "OMADA_API", "\"${localProperty("omada_api", "https://yt.omada.cafe/api/v1/search")}\"" )
        buildConfigField( "String", "SUPPORT_API_ENDPOINT", "\"${localProperty("support_api_endpoint")}\"" )
        buildConfigField( "String", "SPOTIFY_MATCH_API_KEY", "\"${localProperty("spotify_match_api_key")}\"" )
        buildConfigField( "String", "SHAZAM_PROXY_API_KEY", "\"${localProperty("shazam_proxy_api_key")}\"" )
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    namespace = "app.kreate.android"

    buildTypes {
        debug {
            manifestPlaceholders += mapOf()
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "$APP_NAME-debug"

            buildConfigField( "Boolean", "IS_AUTOUPDATE", "false" )
            signingConfig = signingConfigs.getByName("debug")
        }

        create( "full" ) {
            // App's properties
            versionNameSuffix = "-f"
            matchingFallbacks += listOf("release", "debug")
        }

        create( "minified" ) {
            // App's properties
            versionNameSuffix = "-m"
            matchingFallbacks += listOf("release", "debug")

            // Package optimization
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create( "beta" ) {
            initWith( maybeCreate("full") )
            versionNameSuffix = "-b"
            matchingFallbacks += listOf("release", "debug")
        }

        /**
         * For convenience only.
         * "Forkers" want to change app name across builds
         * just need to change this variable
         */
        forEach {
            it.manifestPlaceholders.putIfAbsent( "appName", APP_NAME )
        }
    }

    applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }
               .forEach { output ->
                   val typeName = buildType.name
                   output.outputFileName = "$APP_NAME-$typeName.apk"
               }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    sourceSets.getByName("main").jniLibs.srcDir("src/androidMain/jniLibs")

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    androidResources {
        generateLocaleConfig = true
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"

        //conveyor
        version = "0.0.1"
        group = "rimusic"

        //jpackage
        nativeDistributions {
            //conveyor
            vendor = DESKTOP_APP_NAME
            description = "$DESKTOP_APP_NAME Desktop Music Player"

            targetFormats(TargetFormat.Exe, TargetFormat.Msi)
            packageName = "CubicMusic"
            packageVersion = "0.0.1"
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

tasks.matching { it.name == "proguardReleaseJars" }.configureEach {
    enabled = false
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.shimmer)
    implementation(libs.androidx.palette)
    implementation(libs.material3)
    implementation(libs.compose.animation)
    implementation(libs.kotlin.csv)
    implementation(libs.timber)
    implementation(libs.math3)
    implementation(libs.gson)
    implementation (libs.hypnoticcanvas)
    implementation (libs.hypnoticcanvas.shaders)
    implementation(libs.github.jeziellago.compose.markdown)
    implementation(libs.room)
    add("kspAndroid", libs.room.compiler)
    add("kspDesktop", libs.room.compiler)

    implementation(projects.oldtube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.piped)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

    // Debug only
    debugImplementation(libs.ui.tooling.preview.android)
}
