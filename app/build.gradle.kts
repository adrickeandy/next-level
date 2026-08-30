plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.benign.notes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.benign.notes"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "2.0"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    // Modern Background Work
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    // Accurate Location Services
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Kotlin Coroutines for async network calls
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
