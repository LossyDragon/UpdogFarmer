@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.dependency.analysis)
}

dependencyAnalysis {
    issues {
        onUnusedDependencies {
        }
    }
}

// Used to not cache JavaSteam snapshots when developing
configurations.configureEach {
    resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
    resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexplicit-backing-fields")
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.steevsapps.idledaddy"
    compileSdk {
        version = release(37) // Android 17
    }

    defaultConfig {
        applicationId = "com.steevsapps.idledaddy"
        minSdk = 26 // Android 8
        targetSdk = 36 // Android 16

        versionCode = 111
        versionName = "3.0.2"

        // val apiKey = providers.gradleProperty("steamApiKey").get()
        // buildConfigField("String", "STEAM_API_KEY", apiKey)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += setOf(
                "**/*.proto",
                "org/bouncycastle/x509/CertPathReviewerMessages*.properties",
            )
        }
    }

    androidResources {
        localeFilters += setOf(
            "en", "ar", "bg", "bs", "cs", "de", "es-rES", "fa", "fr", "he", "iw", "in", "id",
            "pl", "pt-rBR", "pt-rPT", "ro", "ru", "sl", "sr", "th", "tr", "uk", "vi", "zh",
            "zh-rCN",
        )
    }
}

dependencies {
    /** Java Steam **/
    implementation(libs.bundles.javasteam) {
        isChanging = version?.contains("SNAPSHOT") ?: false
    }
    runtimeOnly(libs.bouncycastle)

    /** AndroidX views (service notifications, preferences) **/
    implementation(libs.bundles.androidx.ui)

    /** Compose **/
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)

    /** Lifecycle & Navigation **/
    implementation(libs.bundles.lifecycle)
    implementation(libs.bundles.navigation)

    /** Images **/
    implementation(libs.bundles.coil)

    /** Network & serialization **/
    implementation(libs.bundles.network)

    /** Misc **/
    implementation(libs.jsoup)
    implementation(libs.zxing.core)
    implementation(libs.compose.preference)

    /** Dependency injection **/
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.androidx.compose)

    /** Testing **/
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}