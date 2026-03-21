package com.forgecompose.workouttracker.profile

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.content.Context
import android.content.SharedPreferences

class OnboardingManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"
    }

    fun isFirstTimeLaunch(): Boolean {
        // Defaults to true if the key doesn't exist
        return prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)
    }

    fun setFirstTimeLaunch(isFirstTime: Boolean) {
        prefs.edit().putBoolean(IS_FIRST_TIME_LAUNCH, isFirstTime).apply()
    }
}