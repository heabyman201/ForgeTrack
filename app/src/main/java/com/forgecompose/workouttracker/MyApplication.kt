package com.forgecompose.workouttracker

import android.app.Application
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
    SecureGeminiStore.init(this)
        PersonaPrefs.init(this)
        WellnessAI.initialize(this)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        PersonaPrefs.bootstrapInto(dynamicModel.personaConfig)
    }
}