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



import android.app.Activity
import android.content.Intent
import android.os.Bundle

class NotificationDispatcherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ConnectedWorkout.restoreSnapshot(this)
        val hasSessionSnapshot = ConnectedWorkout.hasSessionSnapshot()
        val serviceRunning = WorkoutForegroundService.isRunning(this)

        // FIX: Check the new 'currentMode' state instead of the old 'isWorkoutActive' boolean.
        // We go to the workout screen if the session is either active or resting.
        val hasActiveWorkout = hasSessionSnapshot &&
                (ConnectedWorkout.currentMode.value != ConnectedWorkout.WorkoutMode.INACTIVE || serviceRunning)

        if (!hasSessionSnapshot && serviceRunning) {
            WorkoutForegroundService.stop(this)
        }

        val destinationIntent = if (hasActiveWorkout) {
            Intent(this, WorkoutActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        } else {
            // Otherwise, just open the main app screen
            Intent(this, MainActivity::class.java)
        }

        // Start the correct activity
        startActivity(destinationIntent)

        // Immediately finish this dispatcher so it doesn't appear in the back stack
        finish()
    }
}
