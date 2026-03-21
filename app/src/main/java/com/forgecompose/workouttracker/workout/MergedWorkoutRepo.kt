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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap

class MergedWorkoutRepository(
    private val localRepo: WorkoutRepo
) : WorkoutRepo by localRepo {

    private val db = FirebaseFirestore.getInstance()
    private val remoteDocIdsByWorkoutId = ConcurrentHashMap<Int, String>()

    private fun syntheticIdForDocId(docId: String): Int {
        val positiveHash = (docId.hashCode().toLong() and 0x7FFF_FFFFL).toInt()
        return if (positiveHash == Int.MAX_VALUE) Int.MIN_VALUE else -(positiveHash + 1)
    }

    private fun workoutSignature(workout: Workout): String {
        return listOf(
            workout.name,
            workout.date,
            workout.durationMillis,
            workout.status.name,
            workout.weight,
            workout.sets,
            workout.reps,
            workout.distance,
            workout.startTime,
            workout.endTime
        ).joinToString("|")
    }

    // 1. Robust Firestore Flow
    private fun getFirestoreWorkoutsFlow(): Flow<List<Workout>> = callbackFlow {
        Log.d("MergedRepo", "Starting Firestore listener...")

        val listener = db.collection("workouts")
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

            // Prefer local when records look the same.
            local.forEach { merged[workoutSignature(it)] = it }
            remote.forEach { workout -> merged.putIfAbsent(workoutSignature(workout), workout) }

            merged.values.sortedByDescending { it.date }
        }
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
            return
        }

        val docId = remoteDocIdsByWorkoutId[workout.id] ?: run {
            val snapshot = db.collection("workouts").get().await()
            snapshot.documents.firstOrNull { doc -> syntheticIdForDocId(doc.id) == workout.id }?.id
        }

        if (docId != null) {
            db.collection("workouts").document(docId).delete().await()
            remoteDocIdsByWorkoutId.remove(workout.id)
        } else {
            Log.w("MergedRepo", "Remote workout not found for id=${workout.id}; nothing deleted")
        }
    }

    override suspend fun deleteAllWorkouts() {
        localRepo.deleteAllWorkouts()

        val snapshot = db.collection("workouts").get().await()
        snapshot.documents.forEach { document ->
            document.reference.delete().await()
        }

        remoteDocIdsByWorkoutId.clear()
    }
}
