import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val APP_NAME = "Cubic-Music"

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
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

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
            implementation(libs.media3.session)
            implementation(libs.kotlinx.coroutines.guava)
            implementation(libs.newpipe.extractor)
            implementation(libs.nanojson)
            implementation(libs.androidx.webkit)

            // Related to built-in game, maybe removed in future?
            implementation(libs.compose.runtime.livedata)
        }
        commonMain.dependencies {
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
            implementation("androidx.compose.material:material-icons-extended:1.6.0")
            //coil3 mp
            implementation(libs.coil.compose.core)
            implementation(libs.coil.compose)
            implementation(libs.coil.mp)
            implementation(libs.coil.network.okhttp)

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
        versionCode = 108
        versionName = "1.7.8"

        /*
                UNIVERSAL VARIABLES
         */
        buildConfigField( "Boolean", "IS_AUTOUPDATE", "true" )
        buildConfigField( "String", "APP_NAME", "\"$APP_NAME\"" )
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
        }

        create( "minified" ) {
            // App's properties
            versionNameSuffix = "-m"

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
            vendor = "RiMusic.DesktopApp"
            description = "RiMusic Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiMusic.DesktopApp"
            packageVersion = "0.0.1"
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.compose.activity)
    implementation(libs.compose.foundation)
    implementation(libs.compose.ui)
    implementation(libs.compose.shimmer)
    implementation(libs.androidx.palette)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.datasource.okhttp)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.material3)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.compose.animation)
    implementation(libs.kotlin.csv)
    implementation(libs.monetcompat)
    implementation(libs.androidmaterial)
    implementation(libs.timber)
    implementation(libs.androidx.crypto)
    implementation(libs.math3)
    implementation(libs.toasty)
    implementation(libs.androidyoutubeplayer)
    implementation(libs.androidx.glance.widgets)
    implementation(libs.kizzy.rpc)
    implementation(libs.gson)
    implementation (libs.hypnoticcanvas)
    implementation (libs.hypnoticcanvas.shaders)
    implementation(libs.github.jeziellago.compose.markdown)
// In your build.gradle.kts or build.gradle
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("androidx.media3:media3-exoplayer:1.2.0")
    implementation("androidx.media3:media3-ui:1.2.0")
    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(projects.innertube)
    implementation(projects.oldtube)
    implementation(projects.kugou)
    implementation(projects.lrclib)
    implementation(projects.piped)

    coreLibraryDesugaring(libs.desugaring.nio)

    // Debug only
    debugImplementation(libs.ui.tooling.preview.android)
}
