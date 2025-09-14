plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.github.triplet.play") version "3.10.1"
}

// Work around Windows file lock on build/intermediates by using a fresh alternate build directory
layout.buildDirectory.set(file("build3"))

play {
    // CI 會將 service-account.json 放在 :app 目錄
    serviceAccountCredentials.set(file("service-account.json"))
    track.set("internal")
    defaultToAppBundles.set(true)
}

android {
    namespace = "com.aidestinymaster.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aidestinymaster"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // AdMob App ID placeholder for Manifest meta-data
        // Allow override via -PAD_APP_ID or gradle.properties (AD_APP_ID), fallback to Google test App ID for debug/dev
        val adAppId = (project.findProperty("AD_APP_ID") as String?) ?: "ca-app-pub-3940256099942544~3347511713"
        manifestPlaceholders["AD_APP_ID"] = adAppId

        // Legal pages (can be overridden from gradle.properties)
        val privacyUrl = (project.findProperty("PRIVACY_URL") as String?)
            ?: "https://aidestinymaster.com/privacy"
        val termsUrl = (project.findProperty("TERMS_URL") as String?)
            ?: "https://aidestinymaster.com/terms"
        buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
        buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Signing configuration loaded from ~/.gradle/gradle.properties
    signingConfigs {
        create("release") {
            val storePath = project.findProperty("RELEASE_STORE_FILE") as String?
            val storePassword = project.findProperty("RELEASE_STORE_PASSWORD") as String?
            val keyAlias = project.findProperty("RELEASE_KEY_ALIAS") as String?
            val keyPassword = project.findProperty("RELEASE_KEY_PASSWORD") as String?
            if (storePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
                storeFile = rootProject.file(storePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }


    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Use release signing config if credentials are provided
            signingConfig = signingConfigs.getByName("release")

            // AdMob ad unit IDs from gradle.properties (optional); empty if not provided
            val bannerId = (project.findProperty("AD_BANNER_ID") as String?) ?: ""
            val interId = (project.findProperty("AD_INTERSTITIAL_ID") as String?) ?: ""
            val rewardedId = (project.findProperty("AD_REWARDED_ID") as String?) ?: ""
            val rewardedInterId = (project.findProperty("AD_REWARDED_INTERSTITIAL_ID") as String?) ?: ""
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$bannerId\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$interId\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"$rewardedId\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"$rewardedInterId\"")
            val webClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: ""
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
            // Hide debug UI; release should not show
            buildConfigField("boolean", "SHOW_DEBUG_UI", "false")
        }
        getByName("debug") {
            // Keep debug readable; optional minify can be off
            isMinifyEnabled = false

            // Use Google official test ad unit IDs in Debug
            buildConfigField("String", "ADMOB_BANNER_ID", "\"ca-app-pub-3940256099942544/6300978111\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/1033173712\"")
            buildConfigField("String", "ADMOB_REWARDED_ID", "\"ca-app-pub-3940256099942544/5224354917\"")
            buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL_ID", "\"ca-app-pub-3940256099942544/5354046379\"")
            val webClientId = (project.findProperty("GOOGLE_WEB_CLIENT_ID") as String?) ?: ""
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$webClientId\"")
            // Debug mirrors Release UX; fully hide debug-only controls
            buildConfigField("boolean", "SHOW_DEBUG_UI", "false")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Compose Material Icons (use BOM-managed version)
    implementation("androidx.compose.material:material-icons-extended")
    // LiveData interop for compose observeAsState
    implementation("androidx.compose.runtime:runtime-livedata")

    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)
    implementation(libs.onnxruntime.android)
    implementation(libs.google.play.services.auth)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    // Credential Manager (Sign in with Google)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.0")
    // Google Drive REST API（可於 :sync 使用；app 僅做端到端驗證時用）
    implementation(libs.google.api.client.android)
    implementation(libs.google.http.client.gson)
    implementation(libs.google.api.services.drive.v3)

    // Module deps
    implementation(project(":data"))
    implementation(project(":sync"))
    implementation(project(":billing"))
    implementation(project(":core:ai"))
    implementation(project(":features:bazi"))
    implementation(project(":features:design"))
    implementation(project(":features:mix-ai"))
    implementation(project(":features:astrochart"))
    implementation(project(":features:ziwei"))
    implementation(project(":features:almanac"))
    implementation(libs.google.billing.ktx)
    implementation(libs.google.play.services.ads)
    implementation(libs.kotlinx.serialization.json)
    // In-App Review
    implementation("com.google.android.play:review:2.0.1")
    // AppCompat for per-app locales
    implementation("androidx.appcompat:appcompat:1.7.0")

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation("androidx.test:core-ktx:1.5.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.1.5")
    androidTestImplementation("junit:junit:4.13.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation(libs.junit)
}
