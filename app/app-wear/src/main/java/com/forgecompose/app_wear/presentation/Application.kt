package com.forgecompose.app_wear.presentation



import android.app.Application

class WearApplication : Application() {
    // Lazy initialization of the database
    val database by lazy { AppDatabase.getDatabase(this) }

    // Lazy initialization of the repository
    val repository by lazy { WorkoutRepositoryImpl(database.workoutDao()) }

}
