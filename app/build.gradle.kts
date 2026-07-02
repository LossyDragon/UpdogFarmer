import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
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
        targetSdk = 36 // Android 162

        versionCode = 100
        versionName = "2.0.50"

        val apiKey = providers.gradleProperty("steamApiKey").get()
        buildConfigField("String", "STEAM_API_KEY", apiKey)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
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
        isCoreLibraryDesugaringEnabled = true
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.bundles.javasteam) {
        isChanging = version?.contains("SNAPSHOT") ?: false
    }
    implementation(libs.bundles.androidx.ui)
    implementation(libs.bundles.legacy)
    implementation(libs.bundles.network)
    implementation(libs.glide)
    implementation(libs.jsoup)
    implementation(libs.lifecycle.extensions)
    implementation(libs.zxing.core)

    testImplementation(libs.junit)
}