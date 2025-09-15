package com.forgecompose.workouttracker



import android.app.Activity
import android.content.Intent
import android.os.Bundle

class NotificationDispatcherActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // FIX: Check the new 'currentMode' state instead of the old 'isWorkoutActive' boolean.
        // We go to the workout screen if the session is either active or resting.
        val destinationIntent = if (ConnectedWorkout.currentMode.value != ConnectedWorkout.WorkoutMode.INACTIVE) {
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