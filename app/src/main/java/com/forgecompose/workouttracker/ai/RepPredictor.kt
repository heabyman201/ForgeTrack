package com.forgecompose.workouttracker.ai

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
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer
import java.nio.ByteOrder

class RepPredictor(context: Context) {
    private val appContext = context.applicationContext
    private var interpreter: Interpreter? = null


    private val means = floatArrayOf(1.0333f, 43.9857f, 2.3523f, 295847.61f)
    private val scales = floatArrayOf(1.2801f, 8.6465f, 1.0233f, 266574.62f)

    private fun ensureInterpreter(): Interpreter {
        val existing = interpreter
        if (existing != null) return existing

        val model = FileUtil.loadMappedFile(appContext, "rep_predictor_quant_fixed.tflite")
        val options = Interpreter.Options().apply {
            setNumThreads(4)
        }
        return Interpreter(model, options).also { interpreter = it }
    }

    fun predict(exerciseIndex: Int, weight: Float, sets: Int, durationSec: Int): Float {
        val localInterpreter = ensureInterpreter()
        val input = floatArrayOf(
            exerciseIndex.toFloat(),
            weight,
            sets.toFloat(),
            durationSec.toFloat()
        )


        val scaledInput = FloatArray(4)
        for (i in input.indices) {
            scaledInput[i] = (input[i] - means[i]) / scales[i]
        }

        val inputBuffer = ByteBuffer.allocateDirect(4 * 4).apply {
            order(ByteOrder.nativeOrder())
            for (value in scaledInput) {
                putFloat(value)
            }
        }

        // Output buffer for a single float prediction
        val outputBuffer = ByteBuffer.allocateDirect(4).apply {
            order(ByteOrder.nativeOrder())
        }

        localInterpreter.run(inputBuffer, outputBuffer)

        outputBuffer.rewind()
        return outputBuffer.float
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
