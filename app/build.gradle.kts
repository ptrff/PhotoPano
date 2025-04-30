plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    kotlin("kapt")
    id("com.autonomousapps.dependency-analysis")
}

apply {
    plugin("com.android.application")
    plugin("com.google.dagger.hilt.android")
}

android {
    namespace = "ru.ptrff.photopano"
    compileSdk = 35

    defaultConfig {
        applicationId = "ru.ptrff.photopano"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "AWS_KEY_ID",
            "\"${project.properties["AWS_KEY_ID"]}\""
        )
        buildConfigField(
            "String",
            "AWS_SECRET_KEY",
            "\"${project.properties["AWS_SECRET_KEY"]}\""
        )
        buildConfigField(
            "String",
            "BUCKET_NAME",
            "\"${project.properties["BUCKET_NAME"]}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // lottie animations
    implementation(libs.lottie)

    // Glide
    implementation(libs.glide)

    // Flexbox layout
    implementation(libs.flexbox)

    // AWS S3
    implementation(libs.aws.android.sdk.s3)
    implementation(libs.aws.android.sdk.mobile.client)

    // QR code generator
    implementation(libs.custom.qr.generator)

    // ffmpegkit
    implementation(libs.ffmpeg.kit.full)

    // android gif drawable
    implementation(libs.android.gif.drawable)

    // hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)

    // rxjava
    implementation(libs.rxandroid)
    implementation(libs.rxjava)

    // rxkotlin
    implementation(libs.rxkotlin)
}

kapt {
    correctErrorTypes = true
}
