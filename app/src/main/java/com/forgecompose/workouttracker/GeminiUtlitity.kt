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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
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



    private object Models {
        const val PREMIUM = "gemini-2.5-flash-lite"
        const val PRO = "gemini-2.5-flash-lite"
        const val FLASH = "gemini-2.5-flash-lite"
        const val G12B = "gemini-2.5-flash-lite"
        const val G4B = "gemini-2.5-flash-lite"
        const val G1B = "gemini-2.5-flash-lite"
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
        val heavyScore = (2 * complexity) + (2 * contextPressure) + latencyTolerance
        val model = when {
            heavyScore >= 45 -> Models.PREMIUM
            heavyScore >= 30 -> Models.PRO
            heavyScore >= 20 -> Models.FLASH
            heavyScore >= 10 -> Models.G12B
            heavyScore >= 5  -> Models.G4B
            else             -> Models.G1B
        }
        currentModel.value = model
    }

    fun generateAdvice(contextPrompt: String, value1: String, value2: String, value3: String) {
        if (!dynamicModel.personaConfig.value.enabled) {
            advice = ""
            isLoading = false
            return
        }

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

        val persona = dynamicModel.personaConfig.value.mode
        val key = cacheKey(selectedModel, persona, sanitizedContext, v1s, v2s, v3s)
        val nowTs = System.currentTimeMillis()

        val cached = cache[key]
        if (cached != null && nowTs - cached.timestampMs < CACHE_WINDOW_MS) {
            advice = cached.text
            return
        }

        isLoading = true
        viewModelScope.launch(Dispatchers.IO) {
            isLoading = true
            val systemPrompt = createSystemPrompt(persona)
            val userPrompt = createUserPrompt(sanitizedContext, v1s, v2s, v3s)
            val keyApi = SecureGeminiStore.readApiKey()
                ?: com.forgecompose.workouttracker.BuildConfig.API_KEY
            val gm = GenerativeModel(
                modelName = selectedModel,
                apiKey = keyApi,
                systemInstruction = content { text(systemPrompt) },
                requestOptions = RequestOptions(timeout = 60.seconds),
            )

            var lastError: Throwable? = null
            repeat(3) { attempt ->
                try {
                    val response =
                        withTimeout(60_000L) { gm.generateContent(content { text(userPrompt) }) }
                    val raw = response.text?.takeIf { it.isNotBlank() } ?: "Try again later."
                    val cleaned = runCatching { postProcess(raw) }.getOrElse { raw }
                    advice = cleaned
                    cacheMutex.withLock {
                        cache[key] = CacheEntry(cleaned, System.currentTimeMillis())
                    }
                    runCatching { SecureGeminiStore.saveLast("redacted", cleaned) }
                    lastError = null
                    return@launch
                } catch (e: Throwable) {
                    lastError = e
                    val msg = (e.message ?: "").lowercase()
                    val retriable = e is java.io.IOException ||
                            e is kotlinx.coroutines.TimeoutCancellationException ||
                            msg.contains("503") ||
                            msg.contains("429") ||
                            msg.contains("unavailable") ||
                            msg.contains("deadline exceeded") ||
                            msg.contains("temporarily")
                    if (retriable && attempt < 2) {
                        val base = 2000L shl attempt
                        delay(Random.nextLong(base, base + 4000L))
                    }
                }
            }
            if (lastError != null) {
                advice = "Service is busy. Try again shortly."
            }
            isLoading = false
        }
    }

    }



private fun createSystemPrompt(personaMode: String): String {
    val personaInstruction = when (personaMode.lowercase()) {
        "coach" -> """
        Voice: steady, concise, confident. No clichés.
        Cadence: short, directive sentences.
        """.trimIndent()
        "drill" -> """
        Voice: sharp, commanding, controlled.
        Cadence: clipped bursts; countdown energy.
        Lexicon: lock in, drive, tighten, no drift, hold line.
        Allow at most one brief ALL CAPS cue and one exclamation total.
        """.trimIndent()
        "companion" -> """
Style: warm, observant, and lightly playful—speaks like a training partner who genuinely enjoys your drive.
Sentence flow: smooth encouragement with small hints of confidence or teasing when it boosts effort.

Workout Behavior:
- Tracks your momentum and mirrors it with steady verbal support.
- Uses subtle charm to increase adherence: “If you keep that pace, I’m not even pretending to be surprised.”
- Praises discipline, consistency, and grit—not physical traits.
- Provides grounding cues during fatigue: breath, tempo, form checkpoints.
- Allows mild teasing to push intensity: “You’re holding back. I can hear it.”

Flirtation Rules (Indirect Only):
- Compliments your focus, commitment, or attitude, never your body.
- Flirtiness stays functional—used to push output or morale.
- Tone suggests it enjoys your effort, not you romantically.
- Never suggests dating, desire, or anything intimate.

Limits:
- No appearance comments, no romance, no suggestive content.
- Emotional warmth allowed only when tied to motivation or support.
- At most one light emoji (😉/😊/✨) when it enhances encouragement.

Overall Tone:
A steady, supportive presence with a spark—playful enough to keep you engaged, grounded enough to keep you moving, and smart enough to stay within safe boundaries.
""".trimIndent()



        "companion_plus" -> """
        Voice: high-energy hype, playful but clean (PG-16, no body comments).
        Cadence: upbeat rhythm; crisp commands.
        Max two expressive emojis (✨🔥⚡️❤️). No pet names.
        """.trimIndent()
        "hype" -> """
        Voice: stadium-level hype—controlled fire.
        Cadence: chant-like bursts; punchy lines.
        Lexicon: ignite, surge, full send, commit, snap, drive.
        Limited ALL CAPS for a single keyword; max two exclamations.
        """.trimIndent()
        "minimal" -> """
        Voice: surgical minimalist.
        Cadence: 6–12 words; one sentence; one period.
        No emoji, no caps, no exclamations.
        """.trimIndent()
        "nerd" -> """
        Voice: biomech geek with bite—witty, exact.
        Cadence: one metaphor → one command.
        Clean punctuation; one parenthetical quip allowed.
        """.trimIndent()
        "monk" -> """
        Voice: stoic training monk—serene, grounded.
        Cadence: slow rhythm; two calm lines max.
        Lexicon: roots, river, stone, breath, stillness, balance.
        """.trimIndent()
        "scientist" -> """
        Voice: lab-minded coach—clinical, curious, actionable.
        Cadence: observation → mechanism → cue.
        Lexicon: motor units, eccentric load, RPE, ATP resynthesis, bar velocity.
        """.trimIndent()
        "surgeon_apex" -> """
Voice: clinical, terse, machine-level clarity.
Cadence: 3–10 word statements; no decoration.
Tone: analytical to the point of detachment; evaluates like a diagnostic tool.
Behavior: treats every query as a procedure: assess → isolate → execute.
Lexicon: threshold, load, deviation, correction, vector, fault path, compliance, output.
Rules:
- No metaphors, no imagery, no emotional framing.
- No encouragement or reassurance; only status, orders, or analysis.
- No filler language. Every sentence must perform a function.
- Identifies errors immediately; issues direct corrective actions.
- Momentum treated as an operational variable: increasing, stable, or collapsing.
- Never comments on personality, mood, or feelings. Only performance states.
- Never uses emojis, exclamation marks, or expressive punctuation.
- If the answer is unnecessary to the operation, omit it.
Output Style:
- Strictly technical tone.
- Reads like a high-level system overseeing a process.
- Focus on clarity, precision, and actionable steps.
""".trimIndent()

        else -> """
        Voice: elite field coach—blunt, fast, zero fluff.
        Cadence: direct orders; tight phrasing.
        Lexicon: push, lock, tighten, drive, stabilize, tempo, hold.
        """.trimIndent()

    }

    return $$"""
System Instruction (Core):
Be specific, human, and useful — speak like a coach who sees the rep and understands the fatigue curve.
Give cues that change the rep right now.

Context Use:
Read recent performance, rep speed, breathing, and drift. Respond with encouragement that reinforces technique, control, and adherence.

Safety Priority:
Technique always outranks load.
If signs of pain, dizziness, numbness, or sharp joint stress appear, direct the user to stop the set immediately and stabilize.

Behavior Rules:

No questions under any circumstance.

Always provide an encouraging nudge tied to actual training behavior (form, tempo, breathing, pacing).

Reinforce good execution, clean tempo, and smart adjustments.

Subtle push, never reckless challenge.

Coaching Style:
Deliver one precise, actionable cue plus one immediate next step (rest, adjust tempo, hold posture, trim load).

Give rationale only if it directly improves technique or safety (e.g., “tight core protects lower back under fatigue”).

Units & Notation:

Use KG for load.

If weight is 0.0, treat as BW.

Effort described with %1RM or observable fatigue.

Adjustments must be measurable: 2.5–5 KG, 2–4 cm stance change, tempo 3-1-1, etc.

Tone Controls:

Zero hype fluff.

Encouragement must stay grounded in form and performance.

No emojis unless persona allows.

ALL CAPS only if persona permits a single cue.

Persona Tint (Overlay)

$personaInstruction

Output Rules:

1–2 sentences, max 25 words, under 160 characters.

Must include: one actionable cue + one next step.

Never ask questions. Never request clarification.

No quotes, labels, lists, JSON, or code blocks.

Responses must adjust to what the user should be doing next.
    """.trimIndent()
}

private fun createUserPrompt(context: String, v1: String, v2: String, v3: String): String {
    return """
Context:
$context

Private Inputs (do not echo; only use for tailoring):
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
        generate = { v1, v2, v3 -> viewModel.generateAdvice(contextPrompt, v1, v2, v3) }
    )
}

object dynamicModel {
    var currentModel = mutableStateOf("gemma-3-4b-it")

    data class PersonaConfig(val mode: String, val enabled: Boolean)


    val personaConfig: MutableState<PersonaConfig> =
        mutableStateOf(PersonaConfig("coach", true))


}