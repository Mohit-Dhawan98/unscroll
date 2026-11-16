plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.maverick.unscroll"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.maverick.unscroll"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // Single source of truth for the quote library lives in ../shared/quotes.json
    sourceSets["main"].assets.srcDir("../../shared")

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    // Headless Homebrew JDK has no AWT, which lint needs; skip lint on release builds.
    lint { checkReleaseBuilds = false; abortOnError = false }
}

// Deliberately no dependencies: plain android.* widgets only, keeps the APK tiny.
