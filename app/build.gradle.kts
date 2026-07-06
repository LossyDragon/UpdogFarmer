import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
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

        versionCode = 100
        versionName = "3.0.0"

        val apiKey = providers.gradleProperty("steamApiKey").get()
        buildConfigField("String", "STEAM_API_KEY", apiKey)

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

    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3.adaptive:adaptive")

    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("me.zhanghai.compose.preference:preference:2.2.0")
    implementation("io.coil-kt.coil3:coil-compose:3.5.0")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.5.0")

    testImplementation(libs.junit)
}