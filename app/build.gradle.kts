plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android") version "2.58"
//    id("com.google.dagger.hilt.android") version "2.48"
}


android {
    namespace = "com.local.offlinemediaplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.local.offlinemediaplayer"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Hilt
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
//    implementation("com.google.dagger:hilt-android:2.48")
//    kapt("com.google.dagger:hilt-compiler:2.48")

    implementation("com.google.dagger:hilt-android:2.58")
    kapt("com.google.dagger:hilt-compiler:2.58")

    // Media
    implementation("androidx.media:media:1.7.1")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    // Navigation for switching screens
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Media3 (The modern replacement for ExoPlayer/MediaPlayer)
    implementation("androidx.media3:media3-exoplayer:1.2.1")
    implementation("androidx.media3:media3-session:1.2.1")
    implementation("androidx.media3:media3-ui:1.2.1")

    // Coil (For loading album art and video thumbnails)
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Permissions handling in Compose
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")

    // Lifecycle integration
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
}
