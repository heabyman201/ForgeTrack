plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    id("com.google.gms.google-services") version "4.4.3" apply false
    id("com.google.firebase.firebase-perf") version "2.0.1" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
}