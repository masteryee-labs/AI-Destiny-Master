plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

tasks.register<JavaExec>("exportSolarTerms") {
    group = "verification"
    description = "Export solar terms 2020-2030 JSON into src/test/resources"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.aidestinymaster.core.lunar.SolarTermExporter")
}

// Allow passing -PgenerateSolarTerms=true to propagate into tests as system property "generate.solar.terms"
tasks.withType<Test>().configureEach {
    val prop = project.findProperty("generateSolarTerms")?.toString()
        ?: System.getProperty("generate.solar.terms")
        ?: System.getenv("GENERATE_SOLAR_TERMS")
        ?: "false"
    systemProperty("generate.solar.terms", prop)
    // pass shard export parameters if provided
    listOf("export.from", "export.to", "export.out", "export.all").forEach { key ->
        val v = project.findProperty(key)?.toString()
            ?: System.getProperty(key)
            ?: System.getenv(key.replace('.', '_').uppercase())
        if (v != null) systemProperty(key, v)
    }
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
