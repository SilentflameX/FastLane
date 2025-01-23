import com.android.build.api.dsl.Packaging

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.multiplayertest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.multiplayertest"
        minSdk = 26
        targetSdk = 34
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

    packagingOptions {
        resources.excludes.add("META-INF/*")
    }
}

val ktor_version = "3.0.0"

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    //Ktor
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version") // JSON serialization with kotlinx
    implementation("io.ktor:ktor-serialization-kotlinx-protobuf:$ktor_version")//Content negotiation
    //(Client)
    implementation("io.ktor:ktor-client-core:$ktor_version")       // Core Ktor client
    implementation("io.ktor:ktor-client-cio:$ktor_version")        // CIO engine for HTTP requests
    implementation("io.ktor:ktor-client-logging:$ktor_version")    // Logging
    implementation("io.ktor:ktor-client-websockets:$ktor_version") // WebSocket support
    implementation("io.ktor:ktor-client-json:$ktor_version")       // JSON serialization
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    //(Server)
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
}