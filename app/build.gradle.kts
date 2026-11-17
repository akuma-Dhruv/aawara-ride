plugins {
    id("com.android.application")
}

android {
    namespace = "com.aawararide"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aawararide"
        minSdk = 29
        targetSdk = 35
        versionCode = 6
        versionName = "0.1.m3b"
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
}

base {
    archivesName.set("AawaraRide-${android.defaultConfig.versionName}")
}

dependencies {
    implementation("androidx.core:core:1.13.1")
    testImplementation("junit:junit:4.13.2")
}
