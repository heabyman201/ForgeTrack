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


}

android {
    namespace = "com.forgecompose.workouttracker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.forgecompose.workouttracker"
        buildFeatures { buildConfig = true }
        minSdk = 31
        targetSdk = 36
        versionCode = 2
        versionName = "1.0"
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
    "baselineProfile"(project(":app:baselineprofile2"))



    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

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


        implementation(libs.play.services.wearable.v1900)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")
    // Core Android libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material) // Google Material Components

    // Compose UI testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // JUnit for unit tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Optional: Your AI Core or other libraries
    implementation(libs.aicore)
}
