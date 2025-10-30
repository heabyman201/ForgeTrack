package com.forgecompose.workouttracker

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

private val Context.presetDataStore by preferencesDataStore("preset_store")
private val CUSTOM_PRESETS = stringPreferencesKey("custom_presets_json")
private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

object CustomPresetStore {
    fun flow(context: Context): Flow<List<WorkoutPreset>> = context.presetDataStore.data.map {
        val raw = it[CUSTOM_PRESETS] ?: "[]"
        runCatching { json.decodeFromString(ListSerializer(WorkoutPreset.serializer()), raw) }.getOrElse { emptyList() }
    }

    suspend fun add(context: Context, preset: WorkoutPreset) {
        val current = flow(context).first()
        val updated = current.filterNot { it.name.equals(preset.name, true) } + preset.copy(category = preset.category.ifBlank { "Custom" })
        context.presetDataStore.edit { prefs ->
            prefs[CUSTOM_PRESETS] = json.encodeToString(ListSerializer(WorkoutPreset.serializer()), updated)
        }
    }

    suspend fun remove(context: Context, name: String) {
        val current = flow(context).first()
        val updated = current.filterNot { it.name.equals(name, true) }
        context.presetDataStore.edit { prefs ->
            prefs[CUSTOM_PRESETS] = json.encodeToString(ListSerializer(WorkoutPreset.serializer()), updated)
        }
    }
}
