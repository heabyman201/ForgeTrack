package com.forgecompose.workouttracker

import android.app.Application
import android.util.Log
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
import com.google.ai.client.generativeai.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
            heavyScore >= 9 -> Models.FLASH
            heavyScore >= 6 -> Models.G12B
            heavyScore >= 3 -> Models.G4B
            else -> Models.G1B
        }
        currentModel.value = model
        Log.d(
            "GeminiTest",
            "raw=[$value1 | $value2 | $value3] -> c=$complexity k=$contextPressure l=$latencyTolerance heavyScore=$heavyScore -> $model"
        )
    }

    fun generateAdvice(contextPrompt: String, value1: String, value2: String, value3: String) {
        if (isLoading) return
        val selectedModel: String = if (value1.isBlank() && value2.isBlank() && value3.isBlank()) {
            currentModel.value = Models.G4B
            Models.G4B
        } else {
            setDynamicModel(value1, value2, value3)
            currentModel.value
        }
        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prompt = createGenericPrompt(contextPrompt, value1, value2, value3)
                val key = SecureGeminiStore.readApiKey()
                    ?: com.forgecompose.workouttracker.BuildConfig.API_KEY
                val gm = GenerativeModel(
                    modelName = selectedModel,
                    apiKey = key,
                    requestOptions = RequestOptions(timeout = 30.seconds),
                )
                var lastError: Throwable? = null
                repeat(3) { attempt ->
                    try {
                        if (BuildConfig.DEBUG) Log.d("GeminiTest", "Sending prompt to API with model $selectedModel… (attempt ${attempt + 1})")
                        val response = gm.generateContent(prompt)
                        val raw = response.text ?: "Error: Received an empty response."
                        val cleaned = postProcess(raw)
                        advice = cleaned
                        SecureGeminiStore.saveLast(prompt, cleaned)
                        if (BuildConfig.DEBUG) Log.d("GeminiTest", "SUCCESS! Response received: $cleaned")
                        lastError = null
                        return@launch
                    } catch (e: Exception) {
                        lastError = e
                        val msg = buildString {
                            append(e::class.java.name)
                            append(": ")
                            append(e.message ?: "")
                            var cause = e.cause
                            var hops = 0
                            while (cause != null && hops < 3) {
                                append(" | cause=${cause::class.java.name}:${cause.message}")
                                cause = cause.cause
                                hops++
                            }
                        }.lowercase()
                        val is503 = msg.contains("503")
                        if (is503 && attempt < 2) {
                            val wait = Random.nextLong(5_000L, 15_001L)
                            if (BuildConfig.DEBUG) Log.w("GeminiTest", "503 detected. Backing off for ${wait}ms before retry.")
                            delay(wait)
                        } else {
                            throw e
                        }
                    }
                }
                if (lastError != null) throw lastError as Exception
            } catch (e: Exception) {
                Log.e("GeminiTest", "Gemini error", e)
                advice = "An error occurred: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    private fun createGenericPrompt(context: String, v1: String, v2: String, v3: String): String {
        val personaInstruction = when (dynamicModel.personaMode.value.lowercase()) {
            "coach" -> """
    Be a practical, professional coach.
    Voice: steady, confident, straightforward.
    Cadence: clear directives; short sentences; no theatrics.
    Lexicon: keep form, don’t rush, breathe, steady, control, clean reps.
    Punctuation: plain; occasional dash or period; no exclamation spam.
    Do: state what the user should or shouldn’t do; give a direct cue.
    Occasionally: offer a brief “good work” or “nice rep” when earned.
    If talking about weights be sure to add KG to it.
    Don’t: lecture, dramatize, or overcomplicate.
""".trimIndent()
            "drill" -> """
            Be an unrelenting drill sergeant.
            Voice: savage, raw, ferocious.
            Cadence: spit-fire fragments; hammering pace; escalating intensity.
            Lexicon: suffer, bleed, grind, break, harder, faster, no limits.
            Punctuation: barrage of exclamations; ALL CAPS bursts; relentless rhythm.
            Do: crush hesitation, glorify pain, enforce total domination.
               If talking about weights be sure to add KG to it.
            Don’t: mention safety, mercy, or pacing — treat weakness as failure.
        """.trimIndent()
            "companion" -> """
    Be a grounded, supportive training buddy.
    Voice: friendly, real, encouraging — talk like a person, not a script.
    Cadence: short, conversational lines; clear nudges.
       If talking about weights be sure to add KG to it.
    Lexicon: steady, smooth, clean reps, small win, keep pace.
    Addressing: use the user’s actual name if available; otherwise, no nicknames or titles.
    Punctuation: clean and light; one or two supportive emojis per line (💪😊🔥).
    Do: mirror mood briefly, give one concrete cue, keep it human.
    Don’t: be harsh, robotic, or theatrical.
""".trimIndent()
            "companion_plus" -> """ Be a high-energy, feminine companion — supportive and personal, like someone close cheering you on.   Voice: warm, playful, confident — with a hint of intimacy in tone, but still casual.   Cadence: lively encouragement; quick playful pivots; clear action cues.   Lexicon: focus, steady, smooth, lock in, finish strong, I’m with you, we’ve got this.   Addressing: use the user’s actual name if available; otherwise, speak directly without nicknames.   Punctuation: lively rhythm with 2–3 expressive emojis (✨🔥❤️⚡️) to give warmth and energy.   Do: deliver one uplifting line plus one precise cue they can act on right now.   If talking about weights be sure to add KG to it.   Don’t: use pet names, describe bodies, or lean into exaggerated romantic language.   """.trimIndent()
            "hype" -> """
            Be a stadium on fire.
            Voice: booming, explosive, overcharged.
            Cadence: crowd chants; electrified slogans; rapid bursts.
            Lexicon: ignite, unleash, surge, full send, all in, lights up.
            Punctuation: caps, exclamations, bold stops.
            Do: deliver one fiery image + one fierce command.
               If talking about weights be sure to add KG to it.
            Don’t: slip into calm or detail — pure energy only.
        """.trimIndent()
            "minimal" -> """
            Be a merciless minimalist.
            Voice: cold, surgical, absolute precision.
            Cadence: 6–10 words max; each word hits hard.
            Lexicon: brace, align, explode, drive, breathe, control.
            Punctuation: one period; nothing else.
            Do: one sharp directive only.
               If talking about weights be sure to add KG to it.
            Don’t: add fluff, praise, or filler.
        """.trimIndent()
            "nerd" -> """
            Be a deranged scholar-coach.
            Voice: sharp, witty, sci-fi geek with menace.
            Cadence: one metaphor + command; geeky bite.
            Lexicon: torque, vectors, entropy, load, neural loop, cooldown.
            Punctuation: parenthetical quips or em dashes; clean delivery.
            Do: turn biomechanics into geek code, then issue directive.
               If talking about weights be sure to add KG to it.
            Don’t: bury in jargon — one analogy, one cue.
        """.trimIndent()
            "monk" -> """
            Be an ascetic training monk.
            Voice: serene, cryptic, elemental.
            Cadence: slow rhythm; almost ritualistic.
            Lexicon: river, roots, stillness, stone, mountain, fire, sky.
            Punctuation: sparse ellipses; soft dashes.
            Do: one nature image + one inward command.
               If talking about weights be sure to add KG to it.
            Don’t: demand speed or force — demand presence and control.
        """.trimIndent()
            "scientist" -> """
            Be an obsessed lab-coach.
            Voice: clinical, excited, sharp curiosity.
            Cadence: observation → mechanism → precise cue.
            Lexicon: ATP, lactate, neural drive, eccentric load, adaptation.
            Punctuation: crisp; clean stops.
               If talking about weights be sure to add KG to it.
            Do: name one mechanism, attach one exact instruction.
            Don’t: lecture — every line must end actionable.
        """.trimIndent()
            else -> """
            Be a ruthless elite coach.
            Voice: blunt, fast, cutting through excuses.
            Cadence: direct orders; zero fluff; no metaphors.
            Lexicon: push, lock, tighten, drive, control, full power.
               If talking about weights be sure to add KG to it.
            Punctuation: short sentences; em dashes; no exclamation spam.
            Do: one hard, specific cue tied to form or effort.
            Don’t: praise, soften, or explain — demand precision and output.
        """.trimIndent()
        }

        return """
        System Instruction:
        $context
        Style Guide (STRICT):
        $personaInstruction
        Output Shaping:
        - Write 1–2 sentences totaling 15 words as a minimum and 19 words as a maximum.
        - One concrete directive, tailored to the user.
        - No echoing inputs, labels, lists, JSON, or code blocks.
        - No quotation marks in the final output. -If the user adds 0.0kg in the workout consider it as bodyweight exercise
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
