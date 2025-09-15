package com.forgecompose.workouttracker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class UserPreferencesManager(context: Context) {

    companion object {
        private const val LEGACY_PREFS = "user_profile_prefs"
        private const val SECURE_PREFS = "user_profile_prefs_secure"

        const val KEY_NAME = "user_name"
        const val KEY_AGE = "user_age"
        const val KEY_HEIGHT = "user_height"
        const val KEY_WEIGHT = "user_weight"
        const val KEY_EXPERIENCE = "user_experience"
        const val KEY_STYLE = "user_preferred_style"
        const val KEY_MUSCLES = "user_important_muscles"
    }

    private val legacyPrefs: SharedPreferences =
        context.getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            SECURE_PREFS,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    init {
        val secureEmpty = prefs.all.isEmpty()
        val legacyHasData = legacyPrefs.all.isNotEmpty()
        if (secureEmpty && legacyHasData) {
            val name = legacyPrefs.getString(KEY_NAME, null)
            val age = legacyPrefs.getString(KEY_AGE, null)
            val height = legacyPrefs.getString(KEY_HEIGHT, null)
            val weight = legacyPrefs.getString(KEY_WEIGHT, null)
            val experience = legacyPrefs.getString(KEY_EXPERIENCE, null)

            prefs.edit().apply {
                if (name != null) putString(KEY_NAME, name)
                if (age != null) putString(KEY_AGE, age)
                if (height != null) putString(KEY_HEIGHT, height)
                if (weight != null) putString(KEY_WEIGHT, weight)
                if (experience != null) putString(KEY_EXPERIENCE, experience)
                apply()
            }
            legacyPrefs.edit { clear() }
        }
    }

    fun saveUserData(
        name: String,
        age: String,
        height: String,
        weight: String,
        experience: String,
        preferredStyle: String,
        importantMuscles: List<String>
    ) {
        prefs.edit().apply {
            putString(KEY_NAME, name)
            putString(KEY_AGE, age)
            putString(KEY_HEIGHT, height)
            putString(KEY_WEIGHT, weight)
            putString(KEY_EXPERIENCE, experience)
            putString(KEY_STYLE, preferredStyle)
            putString(KEY_MUSCLES, importantMuscles.joinToString(","))
            apply()
        }
    }

    fun getName(): String = prefs.getString(KEY_NAME, "-") ?: "-"
    fun getAge(): String = prefs.getString(KEY_AGE, "-") ?: "-"
    fun getHeight(): String = prefs.getString(KEY_HEIGHT, "-") ?: "-"
    fun getWeight(): String = prefs.getString(KEY_WEIGHT, "-") ?: "-"
    fun getExperience(): String = prefs.getString(KEY_EXPERIENCE, "-") ?: "-"
    fun getPreferredStyle(): String = prefs.getString(KEY_STYLE, "Both") ?: "Both"
    fun getImportantMuscles(): List<String> =
        prefs.getString(KEY_MUSCLES, "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
}
