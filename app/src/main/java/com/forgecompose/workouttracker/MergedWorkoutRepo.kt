package com.forgecompose.workouttracker

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart

class MergedWorkoutRepository(
    private val localRepo: WorkoutRepo
) : WorkoutRepo by localRepo {

    private val db = FirebaseFirestore.getInstance()

    // 1. Robust Firestore Flow
    private fun getFirestoreWorkoutsFlow(): Flow<List<Workout>> = callbackFlow {
        Log.d("MergedRepo", "Starting Firestore listener...")

        val listener = db.collection("workouts")

            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("MergedRepo", "Firestore Error: ${error.message}")
                    close(error) // Close this channel on critical error
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    Log.d("MergedRepo", "Firestore received ${snapshot.size()} documents")
                    val workouts = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toWorkout()
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
            // Merge and sort
            (local + remote).sortedByDescending { it.date }
        }
    }

    // ... keep the getLatestWorkout override from before ...
    override fun getLatestWorkout(): Flow<Workout?> {
        val localFlow = localRepo.getLatestWorkout()
        val remoteFlow = getFirestoreWorkoutsFlow()

        return combine(localFlow, remoteFlow) { localLatest, remoteList ->
            val remoteLatest = remoteList.firstOrNull()
            when {
                localLatest == null -> remoteLatest
                remoteLatest == null -> localLatest
                localLatest.date > remoteLatest.date -> localLatest
                else -> remoteLatest
            }
        }
    }
}