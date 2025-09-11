// Use alternate build directory to avoid Windows file locks (fresh dir)
layout.buildDirectory.set(file("build4"))
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// (disabled) was build2

android {
    namespace = "com.aidestinymaster.features.bazi"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
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
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    api(project(":data"))
    api(project(":core:lunar"))
}
