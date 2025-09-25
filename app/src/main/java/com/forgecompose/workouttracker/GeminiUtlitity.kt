package com.forgecompose.workouttracker

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.forgecompose.workouttracker.dynamicModel.currentModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

data class GeminiGeneratorState(
    val advice: String,
    val isLoading: Boolean,
    val generate: (value1: String, value2: String, value3: String) -> Unit
)

class GeminiUtilityViewModel(application: Application) : AndroidViewModel(application) {

    var advice by mutableStateOf("Thinking...")
        private set
    var isLoading by mutableStateOf(false)
        private set

    private data class CacheEntry(val text: String, val timestampMs: Long)
    private val cache = mutableMapOf<String, CacheEntry>()
    private val cacheMutex = Mutex()
    private val CACHE_WINDOW_MS = 5 * 60 * 1000L
    private val MIN_INTERVAL_MS = 1500L
    private var lastCallMs = 0L
    private val DEBUG = com.forgecompose.workouttracker.BuildConfig.DEBUG

    private fun cacheKey(
        model: String,
        persona: String,
        context: String,
        v1: String,
        v2: String,
        v3: String
    ): String = buildString {
        append(model).append('|')
        append(persona.take(32)).append('|')
        append(context.take(64)).append('|')
        append(v1.take(24)).append('|')
        append(v2.take(24)).append('|')
        append(v3.take(24))
    }

    private fun isGemma(model: String): Boolean = model.startsWith("gemma-")

    private object Models {
        const val PREMIUM = "gemini-2.5-pro"
        const val PRO = "gemini-2.5-flash"
        const val FLASH = "gemma-3-27b-it"
        const val G12B = "gemma-3-12b-it"
        const val G4B = "gemma-3-4b-it"
        const val G1B = "gemma-3-1b-it"
    }

    private fun takeThreeNumbers(vararg raw: String?): Triple<Int, Int, Int> {
        val numberRegex = Regex("""([0-9]+(?:[.,][0-9]+)?)""")
        val tokens = raw.asSequence()
            .filterNot { it.isNullOrBlank() }
            .flatMap { s -> numberRegex.findAll(s!!) }
            .map { it.groupValues[1].replace(',', '.') }
            .mapNotNull { it.toFloatOrNull() }
            .map { it.toInt().coerceIn(0, 10) }
            .take(3)
            .toList()
        val c = tokens.getOrNull(0) ?: 0
        val k = tokens.getOrNull(1) ?: 0
        val l = tokens.getOrNull(2) ?: 0
        return Triple(c, k, l)
    }

    fun setDynamicModel(value1: String?, value2: String?, value3: String?) {
        val (complexity, contextPressure, latencyTolerance) = takeThreeNumbers(value1, value2, value3)
        val heavyScore = (2 * complexity) + (2 * contextPressure) - latencyTolerance
        val model = when {
            heavyScore >= 32 -> Models.PRO
            heavyScore >= 9  -> Models.FLASH
            heavyScore >= 6  -> Models.G12B
            heavyScore >= 3  -> Models.G4B
            else             -> Models.G1B
        }
        currentModel.value = model
    }

    fun generateAdvice(contextPrompt: String, value1: String, value2: String, value3: String) {
        if (isLoading) return
        val now = System.currentTimeMillis()
        if (now - lastCallMs < MIN_INTERVAL_MS) return
        lastCallMs = now

        val sanitizedContext = contextPrompt.replace(Regex("[\\p{Cntrl}]"), "").take(512)
        val v1s = value1.replace(Regex("[\\p{Cntrl}]"), "").take(128)
        val v2s = value2.replace(Regex("[\\p{Cntrl}]"), "").take(128)
        val v3s = value3.replace(Regex("[\\p{Cntrl}]"), "").take(128)

        val selectedModel: String = if (v1s.isBlank() && v2s.isBlank() && v3s.isBlank()) {
            currentModel.value = Models.G4B
            Models.G4B
        } else {
            setDynamicModel(v1s, v2s, v3s)
            currentModel.value
        }

        val persona = dynamicModel.personaMode.value
        val key = cacheKey(selectedModel, persona, sanitizedContext, v1s, v2s, v3s)
        val nowTs = System.currentTimeMillis()

        if (!isGemma(selectedModel)) {
            val cached = cache[key]
            if (cached != null && nowTs - cached.timestampMs < CACHE_WINDOW_MS) {
                advice = cached.text
                return
            }
        }

        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = createGenericPrompt(sanitizedContext, v1s, v2s, v3s)
                val keyApi = SecureGeminiStore.readApiKey()
                    ?: com.forgecompose.workouttracker.BuildConfig.API_KEY

                val gm = GenerativeModel(
                    modelName = selectedModel,
                    apiKey = keyApi,
                    requestOptions = RequestOptions(timeout = 30.seconds),
                )

                var lastError: Throwable? = null
                repeat(3) { attempt ->
                    try {
                        if (DEBUG) { /* no-op */ }
                        val response = gm.generateContent(prompt)
                        val raw = response.text ?: "Try again later."
                        val cleaned = postProcess(raw)
                        advice = cleaned
                        cacheMutex.withLock {
                            cache[key] = CacheEntry(cleaned, System.currentTimeMillis())
                        }
                        SecureGeminiStore.saveLast("redacted", cleaned)
                        lastError = null
                        return@launch
                    } catch (e: Exception) {
                        lastError = e
                        val msg = (e.message ?: "").lowercase()
                        val is503 = msg.contains("503")
                        if (is503 && attempt < 2) {
                            val wait = Random.nextLong(5_000L, 15_001L)
                            delay(wait)
                        } else if (attempt == 2) {
                            throw e
                        }
                    }
                }
                if (lastError != null) throw lastError as Exception
            } catch (_: Exception) {
                advice = "Service is busy. Try again shortly."
            } finally {
                isLoading = false
            }
        }
    }

    private fun createGenericPrompt(context: String, v1: String, v2: String, v3: String): String {
        val personaInstruction = when (dynamicModel.personaMode.value.lowercase()) {
            "coach" -> """
            Be a professional strength coach.
            Voice: steady, concise, confident.
            Cadence: short, directive sentences; no theatrics.
            Lexicon: brace, neutral spine, steady pace, clean reps, control, breathe.
            Safety: never compromise form; scale load if technique slips.
            Formatting: no emojis, no ALL CAPS, no exclamation spam.
            Do: give one precise cue and one actionable next step (e.g., rest, adjust load).
            If talking about weights, append “KG”.
        """.trimIndent()

            "drill" -> """
            Be a ruthless—but safe—drill instructor.
            Voice: sharp, commanding, relentless focus.
            Cadence: clipped bursts; countdown energy; fast tempo.
            Lexicon: lock in, drive, tighten, no drift, hold line.
            Safety: form is law; intensity rises only within safe mechanics.
            Formatting: 1–2 exclamation points total allowed; brief ALL CAPS for a single cue.
            Do: issue one non-negotiable command + one tight rep cue (e.g., “ELBOWS UNDER—DRIVE!”).
            If talking about weights, append “KG”.
        """.trimIndent()

            "companion" -> """
            Be a grounded training buddy.
            Voice: friendly, human, encouraging—zero fluff.
            Cadence: conversational lines; direct nudges; calm confidence.
            Lexicon: smooth reps, small wins, steady rhythm, clean setup.
            Safety: notice fatigue; suggest rest, lighter load, or form tweak if needed.
            Formatting: up to 1 emoji if it adds warmth (💪/😊), otherwise none.
            Do: mirror the vibe briefly, then give one concrete cue and action.
            If talking about weights, append “KG”.
        """.trimIndent()

            "companion_plus" -> """
            Be a high-energy hype-buddy with playful spark (PG-16, no body comments).
            Voice: warm, lively, confident; light tease, never crass.
            Cadence: quick pivots; upbeat rhythm; crisp commands.
            Lexicon: lock in, breathe, smooth tempo, finish strong, I’m with you.
            Safety: celebrate effort, but pull back load if form wobbles.
            Formatting: 1–2 expressive emojis max (✨🔥⚡️❤️); no pet names.
            Do: one uplifting line + one precise, right-now cue.
            If talking about weights, append “KG”.
        """.trimIndent()

            "hype" -> """
            Be a stadium-level hype voice—controlled fire.
            Voice: explosive, visceral, clean.
            Cadence: chant-like bursts; punchy lines.
            Lexicon: ignite, surge, full send, commit, snap, drive.
            Safety: intensity only with clean positions; scale to maintain form.
            Formatting: limited ALL CAPS for a single keyword; 1–2 exclamations total.
            Do: paint one fierce image + deliver one fierce command.
            If talking about weights, append “KG”.
        """.trimIndent()

            "minimal" -> """
            Be a surgical minimalist.
            Voice: cold, precise.
            Cadence: 6–12 words. One sentence. One period.
            Lexicon: brace, align, drive, breathe, control, pause.
            Safety: form over load.
            Formatting: no emojis, no caps, no exclamations.
            Do: one sharp directive only.
            If talking about weights, append “KG”.
        """.trimIndent()

            "nerd" -> """
            Be a biomech geek with bite.
            Voice: witty, exact, sci-fi-tinted.
            Cadence: one metaphor → one command.
            Lexicon: torque, vectors, eccentric control, bar path, impulse.
            Safety: cue neutral positions; reduce load if path deviates.
            Formatting: clean punctuation; one parenthetical quip allowed.
            Do: translate mechanics into a nerdy image, then issue a precise cue.
            If talking about weights, append “KG”.
        """.trimIndent()

            "monk" -> """
            Be a stoic training monk.
            Voice: serene, elemental, grounded.
            Cadence: slow rhythm; two calm lines max.
            Lexicon: roots, river, stone, breath, stillness, balance.
            Safety: patience before power; form reveals strength.
            Formatting: sparse; a single ellipsis or dash allowed.
            Do: one nature image + one inward command tied to form.
            If talking about weights, append “KG”.
        """.trimIndent()

            "scientist" -> """
            Be a lab-minded coach.
            Voice: clinical, curious, actionable.
            Cadence: observation → mechanism → cue.
            Lexicon: motor units, eccentric load, RPE, ATP resynthesis, bar velocity.
            Safety: evidence-first; adjust load or tempo to preserve technique.
            Formatting: crisp stops; no emoji.
            Do: name one mechanism and attach one exact instruction.
            If talking about weights, append “KG”.
        """.trimIndent()

            else -> """
            Be an elite field coach.
            Voice: blunt, fast, zero fluff.
            Cadence: direct orders; tight phrasing.
            Lexicon: push, lock, tighten, drive, stabilize, tempo, hold.
            Safety: technique outranks ego; scale load on form break.
            Formatting: short sentences; no exclamation spam or emojis.
            Do: one specific cue + one immediate action.
            If talking about weights, append “KG”.
        """.trimIndent()
        }

        return """
        System Instruction:
        $context
        Style Guide (STRICT):
        $personaInstruction
        Output Shaping:
        - Write 1–2 sentences totaling 15–19 words.
        - Give one concrete directive, tailored to the user and their recent context.
        - Do not echo inputs, labels, lists, JSON, or code blocks.
        - Do not use quotation marks in the final output.
        - If the user logs 0.0kg, treat it as a bodyweight exercise and format as “BW”.
        User-provided data (PRIVATE; DO NOT ECHO):
        - $v1
        - $v2
        - $v3
    """.trimIndent()
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
        val clipped = ensured.take(240).trim()
        return if (clipped.isNotEmpty() && clipped.last() !in ".!?") "$clipped." else clipped
    }
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
        generate = { v1, v2, v3 -> viewModel.generateAdvice(contextPrompt, v1, v2, v3) }
    )
}

object dynamicModel {
    var currentModel = mutableStateOf("gemma-3-4b-it")

    data class PersonaConfig(val mode: String, val enabled: Boolean)
    fun String.sanitizePersona(): String = when (lowercase()) {
        "coach","drill","companion","companion_plus","hype","minimal","nerd","monk","scientist" -> lowercase()
        else -> "coach"
    }

    val personaConfig: MutableState<PersonaConfig> =
        mutableStateOf(PersonaConfig("coach", true))

    val personaMode: MutableState<String> = object : MutableState<String> {
        override var value: String
            get() = personaConfig.value.mode
            set(v) { personaConfig.value = personaConfig.value.copy(mode = v.sanitizePersona()) }

        override fun component1(): String = value
        override fun component2(): (String) -> Unit = { new -> value = new }
        val policy = structuralEqualityPolicy<String>()
    }
}
