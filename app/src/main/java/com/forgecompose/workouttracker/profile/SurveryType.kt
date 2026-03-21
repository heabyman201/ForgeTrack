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


import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@SuppressLint("StaticFieldLeak")
object SurveyTape {
    private val Context.surveyStore: DataStore<Preferences> by preferencesDataStore(name = "survey_tape_store")
    private lateinit var app: Context
    private val KEY_SURVEY_DATA = stringPreferencesKey("survey_tape_data")

    private var maxCharsTotal: Int = 16_000

    fun init(context: Context, maxCharsTotal: Int = 16_000) {
        this.app = context.applicationContext
        this.maxCharsTotal = maxCharsTotal.coerceAtLeast(1_000)
    }

    suspend fun logAnswer(
        questionId: String,
        questionText: String,
        answer: String,
        skipped: Boolean = false
    ) {
        ensureInit()
        val ts = nowStamp()

        // Format: [TIMESTAMP] Q: QuestionID | A: Answer (or SKIPPED)
        val entry = if (skipped) {
            "[$ts] ID: $questionId | SKIPPED\n"
        } else {
            "[$ts] ID: $questionId | A: $answer\n"
        }

        app.surveyStore.edit { prefs ->
            val current = prefs[KEY_SURVEY_DATA].orEmpty()
            val next = trimToLimit(current + entry, maxCharsTotal)
            prefs[KEY_SURVEY_DATA] = next
        }
    }

    suspend fun readTape(): String = tapeFlow().first()

    fun tapeFlow(): Flow<String> = app.surveyStore.data.map { it[KEY_SURVEY_DATA].orEmpty() }

    suspend fun clear() {
        ensureInit()
        app.surveyStore.edit { it[KEY_SURVEY_DATA] = "" }
    }

    private fun ensureInit() {
        check(::app.isInitialized) { "SurveyTape.init(context) must be called before use." }
    }

    private fun nowStamp(): String {
        val zdt = Instant.now().atZone(ZoneId.of("UTC"))
        return DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss'Z'", Locale.US).format(zdt)
    }

    private fun trimToLimit(s: String, limit: Int): String {
        if (s.length <= limit) return s
        val tail = s.takeLast(limit)
        val cut = tail.indexOf('\n')
        return if (cut == -1) tail else tail.substring(cut + 1)
    }
}