import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.kapt")
    id("com.google.devtools.ksp")
    id("androidx.baselineprofile") version "1.4.1" // match latest
    id ("org.jetbrains.kotlin.plugin.serialization") version "1.9.10"
    id("com.google.gms.google-services")
    id("com.google.firebase.firebase-perf")
    id("com.google.firebase.crashlytics")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use(::load)
    }
}

fun localProperty(name: String): String = localProperties.getProperty(name, "").trim()

fun String.asBuildConfigString(): String =
    "\"" + replace("\\", "\\\\").replace("\"", "\\\"") + "\""

android {
    namespace = "com.forgecompose.workouttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.forgecompose.workouttracker"
        minSdk = 31
        targetSdk = 36
        versionCode = 7
        versionName = "1.07"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GOOGLE_AI_STUDIO_API_KEY",
            localProperty("API_KEY").asBuildConfigString()
        )
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")

        }

    }

    buildTypes {
        release {
            isMinifyEnabled = true
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
        buildConfig = true
        compose = true
        mlModelBinding = true
    }
}

dependencies {

    // Compose BOM (Bill of Materials) to align all Compose versions
    implementation(platform(libs.androidx.compose.bom))

    // Jetpack Compose UI
    implementation(libs.ui)
    implementation(libs.androidx.compose.ui.ui.graphics2) // Required for asComposeRenderEffect()
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation(libs.generativeai)
    implementation(libs.androidx.datastore.core)
// build.gradle (module)
    implementation("androidx.security:security-crypto:1.1.0")
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.animation.core)
    implementation(libs.androidx.compose.ui.ui)
    implementation(libs.androidx.compose.ui.ui.graphics3)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.foundation)

    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.tensorflow:tensorflow-lite:2.17.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.5.0")
    implementation("org.tensorflow:tensorflow-lite-metadata:0.5.0")
    "baselineProfile"(project(":app:baselineprofile2"))
    implementation("com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15")
    implementation("com.google.mediapipe:tasks-genai:0.10.29") {
        exclude(group = "com.google.ai.edge.litert", module = "litert-api")
    }

    implementation("com.google.android.gms:play-services-tflite-java:16.4.0") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    implementation(platform("com.google.firebase:firebase-bom:33.7.0")) // pick latest
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-perf")

    // Firebase services
    implementation("com.google.firebase:firebase-crashlytics-ktx")

    // Material 3
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation(libs.material3)
    implementation("androidx.datastore:datastore-preferences:1.1.7")
    // Activity + Lifecycle for Compose
    implementation(libs.androidx.activity.compose)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // Navigation
    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("dev.chrisbanes.haze:haze-android:1.6.10")           // Android bits
    implementation("dev.chrisbanes.haze:haze-materials-android:1.6.10")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.tasks.genai)
    // Room
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.room.common.jvm)
    ksp(libs.xandroidx.room.compiler)

    implementation("androidx.health.connect:connect-client:1.1.0")
        implementation(libs.play.services.wearable.v1900)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation("com.google.android.gms:play-services-wearable:19.0.0")

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)


    testImplementation(libs.junit)
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(libs.aicore)
}
