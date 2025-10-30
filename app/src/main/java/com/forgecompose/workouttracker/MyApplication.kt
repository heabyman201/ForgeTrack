package com.forgecompose.workouttracker

import android.app.Application
import androidx.compose.runtime.mutableStateOf
import androidx.appcompat.app.AppCompatDelegate

class MyApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val workoutRepository: WorkoutRepo by lazy {
        WorkoutRepositoryImpl(
            database.workoutDao(),
            database.workoutExerciseDao(),
            database.exerciseSetDao(),

        )
    }
    override fun onCreate() {
        super.onCreate()
        PersonaPrefs.init(this)
        PersonaPrefs.bootstrapInto(dynamicModel.personaConfig)
    SecureGeminiStore.init(this)
        PDE.init(applicationContext)
        blurAnim.init(applicationContext)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        PerformanceOptionsManager.initialize(this)

    }
}