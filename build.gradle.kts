plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.example.mangabuilder"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.mangabuilder"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
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
    implementation("androidx.core:core-ktx:1.11.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.9.0")
    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")
    implementation("io.coil-kt:coil:2.4.0")
    implementation("androidx.activity:activity-ktx:1.8.0")
    implementation("androidx.fragment:fragment-ktx:1.6.1")
    implementation("androidx.core:core-ktx:1.11.0")
}
