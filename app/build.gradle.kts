plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.android.kotlin)
    alias(libs.plugins.gradle.ktLint)
    id("kotlin-parcelize")
}

android {
    namespace = "com.steevsapps.idledaddy"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.steevsapps.idledaddy"
        minSdk = 23
        targetSdk = 29

        multiDexEnabled = true

        versionCode = 87
        versionName = "2.0.47"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            val apiKey = project.property("IdleDaddy_SteamApiKey") as String
            buildConfigField("String", "SteamApiKey", apiKey)
            // buildConfigField "String", "AdmobAppId", IdleDaddy_AdmobAppId
            // resValue "string", "admob_app_id", IdleDaddy_AdmobAppId
            // resValue "string", "admob_ad_unit_Id", IdleDaddy_AdmobAdUnitId
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
            val apiKey = project.property("IdleDaddy_SteamApiKey") as String
            buildConfigField("String", "SteamApiKey", apiKey)
            // buildConfigField "String", "AdmobAppId", IdleDaddy_AdmobAppId
            // resValue "string", "admob_app_id", IdleDaddy_AdmobAppId
            // resValue "string", "admob_ad_unit_Id", IdleDaddy_AdmobAdUnitId
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.javasteam) {
        isChanging = version?.contains("SNAPSHOT") ?: false
    }

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.bundles.compose.utils)

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.legacy.preference.v14)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.legacy.support.v4)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.media)
    implementation(libs.androidx.multidex)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.converter.gson)
    implementation(libs.glide)
    implementation(libs.gson)
    implementation(libs.jsoup)
    implementation(libs.libsuperuser)
    implementation(libs.material)
    implementation(libs.protobuf.java)
    implementation(libs.prov)
    implementation(libs.retrofit)

    /* Billing and Ads */
    // implementation "com.android.billingclient:billing:6.0.1"
    // implementation "com.google.android.ads.consent:consent-library:1.0.8"
    // implementation "com.google.android.gms:play-services-ads:22.5.0"

    testImplementation("androidx.test.espresso:espresso-core:3.1.0") {
        exclude("com.android.support", "support-annotations")
    }

    testImplementation(libs.junit)
}
