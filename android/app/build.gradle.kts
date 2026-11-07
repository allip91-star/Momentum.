plugins {
    id("com.android.application")
}

android {
    namespace = "com.momentum.habits"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.momentum.habits"
        minSdk = 26
        targetSdk = 34
        versionCode = 8
        versionName = "8.0-compact-today"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
