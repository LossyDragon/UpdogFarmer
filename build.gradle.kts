// Top-level build file where you can add configuration options common to all sub-projects/modules.

// See https://github.com/autonomousapps/dependency-analysis-gradle-plugin/issues/1724
buildscript {
    dependencies {
        //noinspection UseTomlInstead
        classpath("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.0")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.dependency.analysis)
}
