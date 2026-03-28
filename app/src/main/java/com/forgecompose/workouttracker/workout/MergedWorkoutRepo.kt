package com.forgecompose.workouttracker.workout

import com.forgecompose.workouttracker.*
import com.forgecompose.workouttracker.ai.*
import com.forgecompose.workouttracker.analytics.*
import com.forgecompose.workouttracker.badges.*
import com.forgecompose.workouttracker.health.*
import com.forgecompose.workouttracker.muscle.*
import com.forgecompose.workouttracker.profile.*
import com.forgecompose.workouttracker.ui.components.*
import com.forgecompose.workouttracker.workout.*

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

class MergedWorkoutRepository(
    private val localRepo: WorkoutRepo
) : WorkoutRepo by localRepo {

    private val db = FirebaseFirestore.getInstance()
    private val remoteDocIdsByWorkoutId = ConcurrentHashMap<Int, String>()
    private val healthConnectTagRegex = Regex("""\[HC_ID:[^\]]+]""")

    private fun syntheticIdForDocId(docId: String): Int {
        val positiveHash = (docId.hashCode().toLong() and 0x7FFF_FFFFL).toInt()
        return if (positiveHash == Int.MAX_VALUE) Int.MIN_VALUE else -(positiveHash + 1)
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun extractExternalSyncTag(notes: String?): String? =
        notes?.let { healthConnectTagRegex.find(it)?.value }

    private fun remoteKeyForWorkout(workout: Workout): String {
        val externalTag = extractExternalSyncTag(workout.notes)
        if (!externalTag.isNullOrBlank()) {
            return "hc_${sha256(externalTag)}"
        }

        val base = listOf(
            workout.name.trim(),
            workout.date,
            workout.startTime,
            workout.endTime ?: -1L,
            workout.durationMillis ?: -1L,
            workout.status.name,
            workout.weight ?: -1.0,
            workout.sets ?: -1,
            workout.reps ?: -1,
            workout.distance ?: -1.0
        ).joinToString("|")
        return "wk_${sha256(base)}"
    }

    private fun mergeKeyForWorkout(workout: Workout): String =
        extractExternalSyncTag(workout.notes)?.let { "hc:$it" } ?: remoteKeyForWorkout(workout)

    private fun workoutToRemoteMap(workout: Workout): Map<String, Any?> = mapOf(
        "syncKey" to remoteKeyForWorkout(workout),
        "name" to workout.name,
        "date" to Timestamp(Date(workout.date)),
        "startTime" to workout.startTime,
        "endTime" to workout.endTime,
        "durationMillis" to workout.durationMillis,
        "status" to workout.status.name,
        "weight" to workout.weight,
        "sets" to workout.sets,
        "reps" to workout.reps,
        "distance" to workout.distance,
        "notes" to workout.notes,
        "rpe" to workout.rpe,
        "tempo" to workout.tempo,
        "restPeriodSeconds" to workout.restPeriodSeconds,
        "heartRateAvg" to workout.heartRateAvg,
        "heartRateMax" to workout.heartRateMax,
        "heartRateTimeline" to workout.heartRateTimeline,
        "caloriesBurned" to workout.caloriesBurned,
        "equipmentUsed" to workout.equipmentUsed,
        "weatherConditions" to workout.weatherConditions,
        "fatigueLevel" to workout.fatigueLevel,
        "sleepQualityScore" to workout.sleepQualityScore,
        "trainingEnvironment" to workout.trainingEnvironment,
        "sessionRpe" to workout.sessionRpe,
        "systemicDrainScore" to workout.systemicDrainScore,
        "intensityScore" to workout.intensityScore,
        "timingFatigueScore" to workout.timingFatigueScore,
        "updatedAt" to System.currentTimeMillis()
    )

    private fun workoutsCollection() =
        Firebase.auth.currentUser?.uid?.takeIf { it.isNotBlank() }?.let { uid ->
            db.collection("users")
                .document(uid)
                .collection("workouts")
        }

    private suspend fun syncWorkoutToRemote(workout: Workout) {
        val workoutsRef = workoutsCollection() ?: return
        val remoteKey = remoteKeyForWorkout(workout)
        runCatching {
            workoutsRef.document(remoteKey).set(workoutToRemoteMap(workout)).await()
        }.onFailure {
            Log.w("MergedRepo", "Failed to sync workout to Firestore", it)
        }
    }

    private suspend fun deleteWorkoutFromRemote(workout: Workout) {
        val workoutsRef = workoutsCollection() ?: return
        val remoteKey = remoteDocIdsByWorkoutId[workout.id] ?: remoteKeyForWorkout(workout)
        runCatching {
            workoutsRef.document(remoteKey).delete().await()
        }.onFailure {
            Log.w("MergedRepo", "Failed to delete workout from Firestore", it)
        }
        remoteDocIdsByWorkoutId.remove(workout.id)
    }

    // 1. Robust Firestore Flow
    private fun getFirestoreWorkoutsFlow(): Flow<List<Workout>> = callbackFlow {
        Log.d("MergedRepo", "Starting Firestore listener...")
        val workoutsRef = workoutsCollection()
        if (workoutsRef == null) {
            remoteDocIdsByWorkoutId.clear()
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener = workoutsRef
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MergedRepo", "Firestore Error: ${error.message}")
                    close(error) // Close this channel on critical error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d("MergedRepo", "Firestore received ${snapshot.size()} documents")
                    remoteDocIdsByWorkoutId.clear()
                    val workouts = snapshot.documents.mapNotNull { doc ->
                        try {
                            val syntheticId = syntheticIdForDocId(doc.id)
                            remoteDocIdsByWorkoutId[syntheticId] = doc.id
                            doc.toWorkout().copy(id = syntheticId)
                        } catch (e: Exception) {
                            Log.e("MergedRepo", "Error parsing doc ${doc.id}: ${e.message}")
                            null
                        }
                    }
                    trySend(workouts)
                } else {
                    Log.d("MergedRepo", "Firestore snapshot is empty")
                    trySend(emptyList())
                }
            }
        awaitClose {
            Log.d("MergedRepo", "Stopping Firestore listener")
            listener.remove()
        }
    }
        // Apply safety operators to the flow
        .onStart { emit(emptyList()) } // Don't block! Show local data immediately.
        .catch { e ->
            Log.e("MergedRepo", "Flow exception: ${e.message}")
            emit(emptyList()) // If it crashes, just show nothing from cloud, don't crash app
        }

    // 2. Combine Local + Remote
    override fun getAllWorkouts(): Flow<List<Workout>> {
        val localFlow = localRepo.getAllWorkouts()
        val remoteFlow = getFirestoreWorkoutsFlow()

        return combine(localFlow, remoteFlow) { local, remote ->
            Log.d("MergedRepo", "Merging: Local=${local.size}, Remote=${remote.size}")
            val merged = LinkedHashMap<String, Workout>()

            local.forEach { merged[mergeKeyForWorkout(it)] = it }
            remote.forEach { workout ->
                val key = mergeKeyForWorkout(workout)
                val existing = merged[key]
                merged[key] = if (existing != null && existing.id >= 0) {
                    workout.copy(id = existing.id)
                } else {
                    workout
                }
            }

            merged.values.sortedByDescending { it.date }
        }
    }

    override suspend fun insertWorkout(workout: Workout): Long {
        val rowId = localRepo.insertWorkout(workout)
        syncWorkoutToRemote(workout.copy(id = rowId.toInt()))
        return rowId
    }

    override suspend fun updateWorkout(workout: Workout) {
        localRepo.updateWorkout(workout)
        syncWorkoutToRemote(workout)
    }

    override suspend fun updateWorkoutNotes(workoutId: Long, notes: String?) {
        val normalizedNotes = notes?.trim()?.ifBlank { null }
        if (workoutId >= 0) {
            localRepo.updateWorkoutNotes(workoutId, normalizedNotes)
        }

        val localMatch = if (workoutId >= 0) {
            localRepo.getWorkoutById(workoutId).first()
        } else {
            val remoteWorkout = getAllWorkouts().first().firstOrNull { it.id.toLong() == workoutId }
            val matchingLocal = remoteWorkout?.let { remote ->
                localRepo.getAllWorkouts().first().firstOrNull { mergeKeyForWorkout(it) == mergeKeyForWorkout(remote) }
            }
            if (matchingLocal != null) {
                localRepo.updateWorkoutNotes(matchingLocal.id.toLong(), normalizedNotes)
            }
            matchingLocal
        }

        val workoutToSync = localMatch
            ?: getAllWorkouts().first().firstOrNull { it.id.toLong() == workoutId }?.copy(notes = normalizedNotes)
            ?: return

        syncWorkoutToRemote(workoutToSync.copy(notes = normalizedNotes))
    }

    override fun getLatestWorkout(): Flow<Workout?> {
        val localFlow = localRepo.getLatestWorkout()
        val remoteFlow = getFirestoreWorkoutsFlow()

        return combine(localFlow, remoteFlow) { localLatest, remoteList ->
            val remoteLatest = remoteList.maxByOrNull { it.date }
            when {
                localLatest == null -> remoteLatest
                remoteLatest == null -> localLatest
                localLatest.date > remoteLatest.date -> localLatest
                else -> remoteLatest
            }
        }
    }

    override suspend fun deleteWorkout(workout: Workout) {
        if (workout.id >= 0) {
            localRepo.deleteWorkout(workout)
            deleteWorkoutFromRemote(workout)
            return
        }

        val workoutsRef = workoutsCollection() ?: run {
            Log.w("MergedRepo", "No authenticated user; skipping remote workout delete")
            return
        }

        val docId = remoteDocIdsByWorkoutId[workout.id] ?: run {
            val snapshot = workoutsRef.get().await()
            snapshot.documents.firstOrNull { doc -> syntheticIdForDocId(doc.id) == workout.id }?.id
        }

        if (docId != null) {
            workoutsRef.document(docId).delete().await()
            remoteDocIdsByWorkoutId.remove(workout.id)
        } else {
            Log.w("MergedRepo", "Remote workout not found for id=${workout.id}; nothing deleted")
        }
    }

    override suspend fun deleteAllWorkouts() {
        localRepo.deleteAllWorkouts()
        val workoutsRef = workoutsCollection() ?: run {
            remoteDocIdsByWorkoutId.clear()
            Log.w("MergedRepo", "No authenticated user; skipping remote workout purge")
            return
        }

        val snapshot = workoutsRef.get().await()
        snapshot.documents.forEach { document ->
            document.reference.delete().await()
        }

        remoteDocIdsByWorkoutId.clear()
    }
}
