package com.forgecompose.workouttracker

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object SecureGeminiStore {
    private const val FILE = "gemini_secure_store"
    private const val K_LAST_PROMPT = "last_prompt"
    private const val K_LAST_ADVICE = "last_advice"
    private const val K_API_KEY = "gemini_api_key"

    @Volatile private var ready = false
    private lateinit var prefs: EncryptedSharedPreferences

    fun init(context: Context) {
        if (ready) return
        synchronized(this) {
            if (ready) return
            val app = context.applicationContext
            val masterKey = MasterKey.Builder(app)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            @Suppress("UNCHECKED_CAST")
            prefs = EncryptedSharedPreferences.create(
                app,
                FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ) as EncryptedSharedPreferences
            val stored = prefs.getString(K_API_KEY, null)
            if (stored.isNullOrEmpty() && BuildConfig.API_KEY.isNotEmpty()) {
                prefs.edit().putString(K_API_KEY, BuildConfig.API_KEY).apply()
            }
            ready = true
        }
    }

    fun saveLast(prompt: String, advice: String) {
        if (!ready) return
        prefs.edit()
            .putString(K_LAST_PROMPT, prompt)
            .putString(K_LAST_ADVICE, advice)
            .apply()
    }

    fun readLastPrompt(): String? = if (ready) prefs.getString(K_LAST_PROMPT, null) else null
    fun readLastAdvice(): String? = if (ready) prefs.getString(K_LAST_ADVICE, null) else null

    fun saveApiKey(key: String) { if (ready) prefs.edit().putString(K_API_KEY, key).apply() }
    fun readApiKey(): String? =
        if (ready) prefs.getString(K_API_KEY, null) ?: BuildConfig.API_KEY.takeIf { it.isNotEmpty() } else null
}
