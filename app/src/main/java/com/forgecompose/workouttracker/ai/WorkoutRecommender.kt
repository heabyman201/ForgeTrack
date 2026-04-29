package com.forgecompose.workouttracker.ai

import android.content.Context
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

data class WorkoutRecommendation(
    val intensityTier: String,
    val intensityConfidence: Float,
    val exerciseCategory: String,
    val categoryConfidence: Float,
)

class WorkoutEngine(context: Context) {

    private val interpreter: Interpreter
    private val scalerMean: FloatArray
    private val scalerScale: FloatArray
    private val intensityTiers: List<String>
    private val exerciseCategories: List<String>

    init {
        // Load TFLite model
        val modelBuffer = loadModelFile(context, "forgetrack_workout.tflite")
        interpreter = Interpreter(modelBuffer)

        // Load label maps + scaler params
        val metaJson = context.assets.open("forgetrack_label_maps.json")
            .bufferedReader().readText()
        val meta = JSONObject(metaJson)

        scalerMean = meta.getJSONArray("scaler_mean").let { arr ->
            FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
        }
        scalerScale = meta.getJSONArray("scaler_scale").let { arr ->
            FloatArray(arr.length()) { arr.getDouble(it).toFloat() }
        }
        intensityTiers = meta.getJSONArray("intensity_tiers").let { arr ->
            List(arr.length()) { arr.getString(it) }
        }
        exerciseCategories = meta.getJSONArray("exercise_categories").let { arr ->
            List(arr.length()) { arr.getString(it) }
        }
    }

    fun recommend(
        sleepHours: Float,
        daysSinceLast: Float,
        weeklyVolume: FloatArray,    // 8 muscle groups
        trainingWeek: Float,
        avgSleep7d: Float,
    ): WorkoutRecommendation {
        // Build raw feature vector
        val raw = floatArrayOf(
            sleepHours, daysSinceLast,
            *weeklyVolume,
            trainingWeek, avgSleep7d,
        )

        // StandardScaler transform: (x - mean) / scale
        val scaled = FloatArray(raw.size) { i ->
            (raw[i] - scalerMean[i]) / scalerScale[i]
        }

        // Prepare input buffer
        val inputBuffer = ByteBuffer.allocateDirect(4 * scaled.size)
            .order(ByteOrder.nativeOrder())
        scaled.forEach { inputBuffer.putFloat(it) }

        // Prepare output buffers (two heads)
        val intensityOutput = Array(1) { FloatArray(5) }
        val categoryOutput = Array(1) { FloatArray(5) }

        val outputs = mapOf(
            0 to intensityOutput,
            1 to categoryOutput,
        )

        // Run inference
        interpreter.runForMultipleInputsOutputs(
            arrayOf(inputBuffer),
            outputs,
        )

        // Decode predictions
        val intProbs = intensityOutput[0]
        val catProbs = categoryOutput[0]

        val intIdx = intProbs.indices.maxByOrNull { intProbs[it] } ?: 0
        val catIdx = catProbs.indices.maxByOrNull { catProbs[it] } ?: 0

        return WorkoutRecommendation(
            intensityTier = intensityTiers[intIdx],
            intensityConfidence = intProbs[intIdx],
            exerciseCategory = exerciseCategories[catIdx],
            categoryConfidence = catProbs[catIdx],
        )
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fd = context.assets.openFd(filename)
        val input = FileInputStream(fd.fileDescriptor)
        val channel = input.channel
        return channel.map(
            FileChannel.MapMode.READ_ONLY,
            fd.startOffset,
            fd.declaredLength,
        )
    }

    fun close() {
        interpreter.close()
    }
}