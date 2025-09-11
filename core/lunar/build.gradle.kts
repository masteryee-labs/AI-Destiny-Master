plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

// workaround: rotate alternate build directory to avoid Windows file locking
layout.buildDirectory.set(file("build5"))

android {
    namespace = "com.aidestinymaster.core.lunar"
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
    // Lunar calendar for BaZi (direct to avoid catalog resolution issues)
    api("cn.6tail:lunar:1.7.4")

    testImplementation("junit:junit:4.13.2")
}
