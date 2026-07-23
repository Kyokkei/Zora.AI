plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.yozora.aichat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yozora.aichat"
        minSdk = 26
        targetSdk = 35
        versionCode = 244
        versionName = "2.2.8"

        val googleWebClientId = providers.gradleProperty("ZORA_GOOGLE_WEB_CLIENT_ID")
            .orElse(System.getenv("ZORA_GOOGLE_WEB_CLIENT_ID") ?: "CONFIGURE_ME")
            .get()
        resValue("string", "google_web_client_id", googleWebClientId)

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    flavorDimensions += "brand"
    productFlavors {
        create("zora") {
            dimension = "brand"
            resValue("string", "app_name", "Zora.AI")
            resValue("string", "app_version_panel", "Zora.AI v2.2.8")
        }
        create("slv") {
            dimension = "brand"
            applicationIdSuffix = ".slv"
            resValue("string", "app_name", "SanLoVerse (SLV)")
            resValue("string", "app_version_panel", "SanLoVerse (SLV) v2.2.8")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.credentials:credentials:1.5.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-test-manifest")
    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
