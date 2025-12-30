package com.forgecompose.workouttracker

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.workouttracker.dynamicModel.currentModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import androidx.compose.runtime.collectAsState

data class GeminiGeneratorState(
    val advice: String,
    val isLoading: Boolean,
    val isModelReady: Boolean,
    val generate: (value1: String, value2: String, value3: String) -> Unit
)

class GeminiUtilityViewModel(application: Application) : AndroidViewModel(application) {

    var advice by mutableStateOf("Initializing local model...")
        private set
    var isLoading by mutableStateOf(false)
        private set

    // Track if the heavy .bin model is loaded into RAM
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady = _isModelReady.asStateFlow()

    private var llmInference: LlmInference? = null
    private val inferenceMutex = Mutex()

    // MODEL CONFIGURATION
    // You must push this file to: /data/data/com.forgecompose.workouttracker/files/model.bin
    // Or download it dynamically in the app.
    private val MODEL_PATH = File(application.filesDir, "model.bin").absolutePath

    init {
        initializeMediaPipe()
    }

    private fun initializeMediaPipe() {
        viewModelScope.launch(Dispatchers.IO) {
            val modelFile = File(MODEL_PATH)
            if (!modelFile.exists()) {
                advice = "Model file not found. Please download model.bin."
                return@launch
            }

            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(MODEL_PATH)
                    .setMaxTokens(512)


                    .build()

                llmInference = LlmInference.createFromOptions(getApplication(), options)
                _isModelReady.value = true
                advice = "Local AI Ready."
            } catch (e: Exception) {
                e.printStackTrace()
                advice = "Failed to load model: ${e.message}"
            }
        }
    }

    fun generateAdvice(contextPrompt: String, value1: String, value2: String, value3: String) {
        if (!dynamicModel.personaConfig.value.enabled) return

        // Safety check
        if (llmInference == null || !_isModelReady.value) {
            advice = "Model is not ready."
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            inferenceMutex.withLock {
                isLoading = true
                try {
                    val persona = dynamicModel.personaConfig.value.mode
                    val prompt = buildPrompt(persona, contextPrompt, value1, value2, value3)

                    // Generate response
                    val result = llmInference?.generateResponse(prompt) ?: "Error"

                    advice = cleanResponse(result)

                } catch (e: Exception) {
                    advice = "Inference error: ${e.localizedMessage}"
                } finally {
                    isLoading = false
                }
            }
        }
    }

    // Clean up memory when ViewModel is cleared
    override fun onCleared() {
        super.onCleared()
        // MediaPipe LlmInference doesn't strictly implement Closeable in all versions,
        // but it's good practice to null it out to release native handles if exposed.
        llmInference = null
    }

    private fun buildPrompt(persona: String, context: String, v1: String, v2: String, v3: String): String {
        // MediaPipe models (like Gemma) are sensitive to formatting.
        // We use a standard chat format often used by these models.
        val sysInstruction = createSystemPrompt(persona)

        return """
        <start_of_turn>user
        $sysInstruction
        
        Context: $context
        Data: $v1, $v2, $v3
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()
    }

    private fun cleanResponse(raw: String): String {
        // Remove common artifacts from local LLMs
        return raw.replace("<end_of_turn>", "")
            .replace("*", "")
            .trim()
            .take(160)
    }

    // Reuse your existing prompt logic
    private fun createSystemPrompt(personaMode: String): String {
        return "Act as a fitness coach. Persona: $personaMode. Keep it under 20 words. No questions."
    }
}
private fun postProcess(rawIn: String): String {
        var t = rawIn.trim()
        if (t.startsWith("```")) t = t.removePrefix("```").trim()
        if (t.endsWith("```")) t = t.removeSuffix("```").trim()
        val filtered = t.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it.matches(Regex("^input\\s*\\d*\\s*:", RegexOption.IGNORE_CASE)) }
            .filterNot { it.equals("output:", true) }
            .filterNot { it.startsWith("{") || it.startsWith("}") || it.startsWith("- ") || it.startsWith("* ") }
            .joinToString(" ")
            .replace("\\s+".toRegex(), " ")
            .trim()
        val unquoted = filtered.trim().trim('\"', '“', '”', '\'', '`')
        val sentenceRegex = Regex("""[^.!?]+[.!?]""")
        val sentences = sentenceRegex.findAll(unquoted).map { it.value.trim() }.toList()
        val candidate = when {
            sentences.isEmpty() -> unquoted
            sentences.size == 1 -> sentences[0]
            else -> (sentences[0] + " " + sentences[1]).trim()
        }
        fun wordCount(s: String) = s.split(Regex("\\s+")).filter { it.isNotBlank() }.size
        val ensured = if (wordCount(candidate) < 8 && wordCount(unquoted) >= 8) {
            unquoted.split(Regex("\\s+")).take(24).joinToString(" ").trim()
        } else candidate
        val clipped = ensured.take(160).trim()
        return if (clipped.isNotEmpty() && clipped.last() !in ".!?") "$clipped." else clipped
    }

@Composable
fun useGeminiAdviceGenerator(
    contextPrompt: String,
    viewModel: GeminiUtilityViewModel = viewModel()
): GeminiGeneratorState {
    val advice = viewModel.advice
    val isLoading = viewModel.isLoading
    return GeminiGeneratorState(
        advice = advice,
        isLoading = isLoading,
        generate = { v1, v2, v3 -> viewModel.generateAdvice(contextPrompt, v1, v2, v3) },
        isModelReady = viewModel.isModelReady.collectAsState().value
    )
}

object dynamicModel {
    var currentModel = mutableStateOf("gemma-3-4b-it")

    data class PersonaConfig(val mode: String, val enabled: Boolean)


    val personaConfig: MutableState<PersonaConfig> =
        mutableStateOf(PersonaConfig("coach", true))


}