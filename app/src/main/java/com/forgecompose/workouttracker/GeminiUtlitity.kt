package com.forgecompose.workouttracker

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File

data class GeminiGeneratorState(
    val currentAdvice: String,
    val isLoading: Boolean,
    val isModelReady: Boolean,
    val generateBatch: (value1: String, value2: String, value3: String) -> Unit,
    val nextAdvice: () -> Unit
)

class GeminiUtilityViewModel(application: Application) : AndroidViewModel(application) {

    var currentAdvice by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set

    var hasAdvice by mutableStateOf(false)
        private set

    private val adviceList = mutableListOf<String>()
    private var currentIndex = 0
    private var rotationJob: kotlinx.coroutines.Job? = null
    private val _isModelReady = MutableStateFlow(false)
    val isModelReady = _isModelReady.asStateFlow()

    private var llmInference: LlmInference? = null
    private val inferenceMutex = Mutex()

    init {
        EdgeModelManager.init(application)

        viewModelScope.launch {
            EdgeModelManager.isModelReady.collect { ready ->
                _isModelReady.value = ready
            }
        }
    }

    private suspend fun loadModel() {
        if (llmInference != null) return

        inferenceMutex.withLock {
            val path = EdgeModelManager.getModelPath()
            if (path == null || !File(path).exists()) {
                currentAdvice = "Model not found."
                return@withLock
            }

            try {
                val options = LlmInference.LlmInferenceOptions.builder()
                    .setModelPath(path)
                    .setMaxTokens(1024)
                    .build()

                llmInference = LlmInference.createFromOptions(getApplication(), options)
                _isModelReady.value = true
            } catch (e: Exception) {
                e.printStackTrace()
                _isModelReady.value = false
                currentAdvice = "Load failed: ${e.localizedMessage}"
            }
        }
    }

    private suspend fun unloadModel() {
        inferenceMutex.withLock {
            llmInference = null
            System.gc()
        }
    }

    fun generateAdviceBatch(contextPrompt: String, v1: String, v2: String, v3: String) {
        rotationJob?.cancel() // Stop any existing loop
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            hasAdvice = false

            try {
                loadModel()
                val persona = dynamicModel.personaConfig.value.mode
                val prompt = buildBatchPrompt(persona, contextPrompt, v1, v2, v3)
                val result = llmInference?.generateResponse(prompt) ?: ""
                processBatchResult(result)

                // Start rotation automatically once we have results
                startAdviceRotation()
            } catch (e: Exception) {
                currentAdvice = "Error: ${e.localizedMessage}"
            } finally {
                unloadModel()
                isLoading = false
            }
        }
    }

    private fun startAdviceRotation() {
        rotationJob?.cancel()
        rotationJob = viewModelScope.launch {
            while (isActive && adviceList.isNotEmpty()) {
                currentAdvice = adviceList[currentIndex]
                delay(8000)
                currentIndex = (currentIndex + 1) % adviceList.size
            }
        }
    }

    fun getNextCachedAdvice() {
        if (adviceList.isNotEmpty()) {
            rotationJob?.cancel() // Reset timer if user clicks manually
            currentIndex = (currentIndex + 1) % adviceList.size
            currentAdvice = adviceList[currentIndex]
            startAdviceRotation()
        }
    }

    private fun processBatchResult(rawResult: String) {
        adviceList.clear()
        currentIndex = 0

        val lines = rawResult.split("\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { !it.startsWith("Here are") }
            .filter { !it.startsWith("Sure") }

        for (line in lines) {
            var cleanLine = line
                .replace(Regex("^\\d+\\.\\s*"), "")
                .replace(Regex("^-\\s*"), "")
                .replace(Regex("^\\*\\s*"), "")
                .trim()

            if (cleanLine.length > 5) {
                if (!cleanLine.endsWith(".") && !cleanLine.endsWith("!")) cleanLine += "."
                adviceList.add(cleanLine)
            }
        }

        if (adviceList.isEmpty()) {
            adviceList.add(postProcess(rawResult))
        }

        if (adviceList.isNotEmpty()) {
            currentAdvice = adviceList[0]
            hasAdvice = true
        }
    }

    override fun onCleared() {
        super.onCleared()
        llmInference = null
    }

    private fun buildBatchPrompt(persona: String, context: String, v1: String, v2: String, v3: String): String {
        val sysInstruction = createSystemPrompt(persona)

        return """
        <start_of_turn>user
        $sysInstruction
        
        Task: Generate a list of 7 distinct, short, punchy pieces of advice based on this context. 
        Format: Return only the list. One item per line. No numbering.
        
        Context: $context
        Data: $v1, $v2, $v3
        <end_of_turn>
        <start_of_turn>model
        """.trimIndent()
    }

    private fun createSystemPrompt(personaMode: String): String {
        return when (personaMode) {
            "surgeon_apex" -> "You are 'The Surgeon'. Cold, precise, analytical. Optimize for performance."
            "drill" -> "You are a Drill Sergeant. Loud, strict, demanding."
            "monk" -> "You are a Monk. Calm, focused, mindful."
            "nerd" -> "You are a Scientist. Use biomechanics."
            "hype" -> "You are a Hype Man. High energy."
            "minimal" -> "Be extremely concise."
            else -> "You are a fitness coach."
        } + " Limit each line to 15 words."
    }

    private fun postProcess(rawIn: String): String {
        return rawIn.replace(Regex("[*#]"), "").trim()
    }
}

@Composable
fun useGeminiAdviceGenerator(
    contextPrompt: String,
    viewModel: GeminiUtilityViewModel = viewModel()
): GeminiGeneratorState {
    val currentAdvice = viewModel.currentAdvice
    val isLoading = viewModel.isLoading
    val isReady by viewModel.isModelReady.collectAsState()
    val hasAdvice = viewModel.hasAdvice



    return GeminiGeneratorState(
        currentAdvice = currentAdvice,
        isLoading = isLoading,
        generateBatch = { v1, v2, v3 -> viewModel.generateAdviceBatch(contextPrompt, v1, v2, v3) },
        nextAdvice = { viewModel.getNextCachedAdvice() },
        isModelReady = isReady
    )
}

object dynamicModel {
    var currentModel = mutableStateOf("gemma-2b-it-gpu-int4")

    data class PersonaConfig(val mode: String, val enabled: Boolean)

    val personaConfig: MutableState<PersonaConfig> =
        mutableStateOf(PersonaConfig("coach", true))
}