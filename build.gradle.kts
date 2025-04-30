plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false

    id("com.autonomousapps.dependency-analysis") version "2.16.0"
}


buildscript {
    dependencies {
        classpath(libs.hilt.android.gradle.plugin)
    }
}