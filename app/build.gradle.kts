import java.util.Properties

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val apiKey: String =
    (localProps.getProperty("API_KEY")
        ?: System.getenv("API_KEY")
        ?: "")
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

android {
    namespace = "com.forgecompose.workouttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.forgecompose.workouttracker"
        buildFeatures { buildConfig = true }
        minSdk = 31
        targetSdk = 36
        versionCode = 5
        versionName = "1.04"
        buildConfigField("String", "API_KEY", "\"$apiKey\"")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        compose = true
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

    "baselineProfile"(project(":app:baselineprofile2"))
    implementation("com.github.Kyant0:AndroidLiquidGlass:1.0.0-alpha15")
    implementation("com.google.mediapipe:tasks-genai:0.10.29")
    implementation("com.google.android.gms:play-services-tflite-java:16.4.0")

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
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)


    implementation(libs.aicore)
}
