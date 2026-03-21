package com.forgecompose.workouttracker

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

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