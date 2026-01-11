import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val APP_NAME = "Cubic-Music"

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)

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

/* ---------------------------------------------------
   KOTLIN MULTIPLATFORM
--------------------------------------------------- */

kotlin {
<<<<<<< HEAD
    jvmToolchain(21)
=======
    jvmToolchain(17)
>>>>>>> 74dcd1a2ab76c96a52052fc60b634905596a5138

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xcontext-receivers")
        }
    }

    // Desktop JVM (allowed to use Java 21)
    jvm("desktop") {
        compilations.all {
            compilerOptions.configure {
                jvmTarget.set(JvmTarget.JVM_21)
            }
        }
    }

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(projects.innertube)
                implementation(projects.piped)
                implementation(projects.invidious)

                implementation(libs.room)
                implementation(libs.room.runtime)
                implementation(libs.room.sqlite.bundled)

                implementation(libs.mediaplayer.kmp)
                implementation(libs.navigation.kmp)

                implementation(libs.coil.mp)
                implementation(libs.coil.network.okhttp)
<<<<<<< HEAD
                implementation("io.coil-kt:coil-compose:2.4.0") // latest stable Coil for Compose

=======
>>>>>>> 74dcd1a2ab76c96a52052fc60b634905596a5138

                implementation(libs.translator)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.compose.ui)
                implementation(libs.compose.foundation)
                implementation(libs.compose.runtime.livedata)
                implementation(libs.media3.exoplayer)
                implementation(libs.media3.session)
                implementation(libs.media3.datasource.okhttp)
                implementation(libs.media3.ui)
                implementation(libs.kotlinx.coroutines.guava)
                implementation(libs.newpipe.extractor)
                implementation(libs.nanojson)
                implementation(libs.androidx.webkit)
                implementation(libs.coil.compose)
                implementation(libs.coil.compose.core)
                implementation(libs.coil.network.okhttp)

                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("org.jsoup:jsoup:1.17.2")
            }
        }

        val desktopMain by getting {
            dependencies {
                implementation(compose.components.resources)
                implementation(compose.desktop.currentOs)

                implementation(libs.material.icon.desktop)
                implementation(libs.vlcj)

                implementation(libs.coil.network.okhttp)
                runtimeOnly(libs.kotlinx.coroutines.swing)

                implementation("com.squareup.okhttp3:okhttp:4.12.0")
                implementation("com.google.code.gson:gson:2.10.1")
                implementation("org.jsoup:jsoup:1.17.2")
            }
        }
    }
}

/* ---------------------------------------------------
   ANDROID CONFIG
--------------------------------------------------- */

android {

    namespace = "app.kreate.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cubic.music"
        minSdk = 23
        targetSdk = 36
        versionCode = 108
        versionName = "1.7.6"

        buildConfigField("Boolean", "IS_AUTOUPDATE", "true")
        buildConfigField("String", "APP_NAME", "\"$APP_NAME\"")

        // Read GENIUS_API_KEY from .env file
        val envFile = rootProject.file(".env")
        val geniusApiKey = if (envFile.exists()) {
            envFile.readLines()
                .firstOrNull { it.startsWith("GENIUS_API_KEY=") }
                ?.substringAfter("GENIUS_API_KEY=")
                ?.trim()
                ?: ""
        } else {
            ""
        }

        buildConfigField("String", "GENIUS_API_KEY", "\"$geniusApiKey\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
<<<<<<< HEAD
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
=======
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
>>>>>>> 74dcd1a2ab76c96a52052fc60b634905596a5138
        isCoreLibraryDesugaringEnabled = true
    }

    androidResources {
        generateLocaleConfig = true
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("Boolean", "IS_AUTOUPDATE", "false")
        }

        create("full") {
            versionNameSuffix = "-f"
        }

        create("minified") {
            versionNameSuffix = "-m"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        create("beta") {
            initWith(getByName("full"))
            versionNameSuffix = "-b"
        }

        forEach {
            it.manifestPlaceholders.putIfAbsent("appName", APP_NAME)
        }
    }

    applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }
            .forEach { output ->
                output.outputFileName = "$APP_NAME-${buildType.name}.apk"
            }
    }
}

/* ---------------------------------------------------
   COMPOSE DESKTOP
--------------------------------------------------- */

compose.desktop {
    application {
        mainClass = "MainKt"

        group = "rimusic"
        version = "0.0.1"

        nativeDistributions {
            vendor = "RiMusic.DesktopApp"
            description = "RiMusic Desktop Music Player"

            targetFormats(
                TargetFormat.Msi,
                TargetFormat.Deb,
                TargetFormat.Rpm
            )

            packageName = "RiMusic.DesktopApp"
            packageVersion = "0.0.1"
        }
    }
}

/* ---------------------------------------------------
   ROOM
--------------------------------------------------- */

room {
    schemaDirectory("$projectDir/schemas")
}

/* ---------------------------------------------------
   DEPENDENCIES (ANDROID-SPECIFIC)
--------------------------------------------------- */

dependencies {

    implementation(libs.compose.activity)
    implementation(libs.compose.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.shimmer)

    implementation(libs.androidx.palette)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.crypto)
    implementation(libs.androidx.glance.widgets)

    implementation(libs.material3)
    implementation(libs.androidmaterial)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.androidyoutubeplayer)

    implementation(libs.kotlin.csv)
    implementation(libs.monetcompat)
    implementation(libs.timber)
    implementation(libs.math3)
    implementation(libs.toasty)
    implementation(libs.gson)

    implementation(libs.kizzy.rpc)
    implementation(libs.hypnoticcanvas)
    implementation(libs.hypnoticcanvas.shaders)
    implementation(libs.github.jeziellago.compose.markdown)

    implementation(projects.genius)
    implementation(projects.innertube)
    implementation(projects.oldtube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.piped)

    ksp(libs.room.compiler)

    coreLibraryDesugaring(libs.desugaring.nio)

    debugImplementation(libs.ui.tooling.preview.android)
}