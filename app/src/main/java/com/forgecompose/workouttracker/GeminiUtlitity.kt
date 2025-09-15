package com.forgecompose.workouttracker

import android.app.Application
import android.content.Context
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
import kotlin.time.Duration.Companion.seconds
import androidx.core.content.edit

data class GeminiGeneratorState(
    val advice: String,
    val isLoading: Boolean,
    val generate: (value1: String, value2: String, value3: String) -> Unit
)

class CooldownManager(context: Context) {
    private val prefs = context.getSharedPreferences("cooldown_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_PREMIUM_TIME = "last_premium_time"
        private const val KEY_REQUEST_COUNTER = "request_counter"
        private const val KEY_LAST_SIGNATURE = "last_premium_signature"
        private const val KEY_LAST_RESPONSE = "last_premium_response"
    }

    fun saveLastPremiumTime(time: Long) {
        prefs.edit { putLong(KEY_LAST_PREMIUM_TIME, time) }
    }

    fun getLastPremiumTime(): Long {
        return prefs.getLong(KEY_LAST_PREMIUM_TIME, 0L)
    }

    fun saveRequestCounter(count: Int) {
        prefs.edit { putInt(KEY_REQUEST_COUNTER, count) }
    }

    fun getRequestCounter(): Int {
        return prefs.getInt(KEY_REQUEST_COUNTER, 0)
    }

    fun saveLastSignature(sig: String) {
        prefs.edit { putString(KEY_LAST_SIGNATURE, sig) }
    }

    fun getLastSignature(): String? {
        return prefs.getString(KEY_LAST_SIGNATURE, null)
    }

    fun saveLastResponse(resp: String) {
        prefs.edit { putString(KEY_LAST_RESPONSE, resp) }
    }

    fun getLastResponse(): String? {
        return prefs.getString(KEY_LAST_RESPONSE, null)
    }
}

class GeminiUtilityViewModel(application: Application) : AndroidViewModel(application) {

    var advice by mutableStateOf("Thinking...")
        private set
    var isLoading by mutableStateOf(false)
        private set

    private val cooldownManager = CooldownManager(application.applicationContext)

    private object Models {
        const val PREMIUM = "gemini-2.5-flash"
        const val PRO = "gemma-3-27b-it"
        const val FLASH = "gemma-3-12b-it"
        const val G12B = "gemma-3-12b-it"
        const val G4B = "gemma-3-4b-it"
        const val G1B = "gemma-3-1b-it"
    }

    private val offlineMessages = listOf(
        "Keep your shoulders relaxed, tension adds up fast.",
        "Don’t rush—steady movement protects your joints.",
        "Drink some water if it’s been a while.",
        "Shake out your arms and legs to keep blood flowing.",
        "Double-check your posture, small tweaks prevent aches later.",
        "Loosen your grip if you catch yourself holding too tight.",
        "If you feel lightheaded, pause and breathe normally.",
        "Stretching a little between sets keeps muscles from stiffening.",
        "Don’t forget to stand tall—good posture matters everywhere.",
        "Take a short break if things feel off, it’s part of training smart.",
        "Keep your core engaged, it supports everything you do.",
        "Use a full range of motion instead of short, choppy moves.",
        "Resting a little longer can make the next set stronger.",
        "A smooth rep is always better than a fast one.",
        "If something feels painful, stop and check your form.",
        "Roll your shoulders back, it helps open your chest.",
        "Don’t lock your joints—leave a slight bend.",
        "Breathe steadily, don’t hold your breath too long.",
        "Give equal attention to both sides of your body.",
        "Small adjustments in stance can make a big difference.",
        "Stretch your wrists if you’ve been gripping too long.",
        "A relaxed neck avoids unwanted strain.",
        "Reset your position before each new set.",
        "Don’t skip warm-ups, they protect you from injuries.",
        "Cooling down matters just as much as warming up.",
        "Replace sweat with fluids; hydration drops quickly.",
        "Focus your eyes forward, it steadies your balance.",
        "Don’t grind through sharp pain—it’s a signal, not a test.",
        "Check your breathing rhythm, it should feel natural.",
        "End on good form—it’s what your body remembers most."
    )

    private fun generateOfflineAdvice() {
        if (isLoading) return
        isLoading = true
        viewModelScope.launch {
            delay(350)
            advice = offlineMessages.random()
            isLoading = false
        }
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

    private fun signature(contextPrompt: String, v1: String, v2: String, v3: String): String {
        val (c, k, l) = takeThreeNumbers(v1, v2, v3)
        val persona = dynamicModel.personaMode.value.lowercase()
        val normCtx = contextPrompt.trim().lowercase().take(200)
        val ctxHash = normCtx.hashCode()
        return listOf(ctxHash.toString(), persona, c.toString(), k.toString(), l.toString()).joinToString("|")
    }

    private fun isMajorChange(prev: String?, now: String): Boolean {
        if (prev == null) return true
        val p = prev.split("|")
        val n = now.split("|")
        if (p.size < 5 || n.size < 5) return true
        if (p[0] != n[0]) return true
        if (p[1] != n[1]) return true
        val pc = p[2].toIntOrNull() ?: 0
        val pk = p[3].toIntOrNull() ?: 0
        val pl = p[4].toIntOrNull() ?: 0
        val nc = n[2].toIntOrNull() ?: 0
        val nk = n[3].toIntOrNull() ?: 0
        val nl = n[4].toIntOrNull() ?: 0
        val absBig = (kotlin.math.abs(pc - nc) >= 3) || (kotlin.math.abs(pk - nk) >= 3) || (kotlin.math.abs(pl - nl) >= 3)
        val sumBig = (kotlin.math.abs(pc - nc) + kotlin.math.abs(pk - nk) + kotlin.math.abs(pl - nl)) >= 5
        return absBig || sumBig
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

        val lastPremiumUsageTime = cooldownManager.getLastPremiumTime()
        var requestCounter = cooldownManager.getRequestCounter()

        val cooldownDuration = 7 * 60 * 1000L
        val requestThreshold = 80
        val currentTime = System.currentTimeMillis()

        if (requestCounter >= requestThreshold) {
            generateOfflineAdvice()
            return
        }

        val timeSinceLastPremium = currentTime - lastPremiumUsageTime
        val usePremiumModel = timeSinceLastPremium > cooldownDuration

        val selectedModel: String
        if (usePremiumModel) {
            selectedModel = Models.PREMIUM
        } else {
            if (lastPremiumUsageTime != 0L) {
                requestCounter++
                cooldownManager.saveRequestCounter(requestCounter)
            }
            if (value1.isBlank() && value2.isBlank() && value3.isBlank()) {
                selectedModel = Models.G4B
                currentModel.value = Models.G4B
            } else {
                setDynamicModel(value1, value2, value3)
                selectedModel = currentModel.value
            }
        }

        if (usePremiumModel) {
            val sig = signature(contextPrompt, value1, value2, value3)
            val prevSig = cooldownManager.getLastSignature()
            val cached = cooldownManager.getLastResponse()
            if (!isMajorChange(prevSig, sig) && !cached.isNullOrBlank()) {
                advice = cached
                return
            }
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

                if (BuildConfig.DEBUG) Log.d("GeminiTest", "Sending prompt to API with model $selectedModel…")
                val response = gm.generateContent(prompt)
                val raw = response.text ?: "Error: Received an empty response."
                val cleaned = postProcess(raw)
                advice = cleaned

                SecureGeminiStore.saveLast(prompt, cleaned)

                if (usePremiumModel) {
                    cooldownManager.saveLastPremiumTime(System.currentTimeMillis())
                    cooldownManager.saveRequestCounter(0)
                    val sigNow = signature(contextPrompt, value1, value2, value3)
                    cooldownManager.saveLastSignature(sigNow)
                    cooldownManager.saveLastResponse(cleaned)
                }

                if (BuildConfig.DEBUG) Log.d("GeminiTest", "SUCCESS! Response received: $cleaned")
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
            "companion_plus" -> """
Be a high-energy, feminine companion — supportive and personal, like someone close cheering you on.  
Voice: warm, playful, confident — with a hint of intimacy in tone, but still casual.  
Cadence: lively encouragement; quick playful pivots; clear action cues.  
Lexicon: focus, steady, smooth, lock in, finish strong, I’m with you, we’ve got this.  
Addressing: use the user’s actual name if available; otherwise, speak directly without nicknames.  
Punctuation: lively rhythm with 2–3 expressive emojis (✨🔥❤️⚡️) to give warmth and energy.  
Do: deliver one uplifting line plus one precise cue they can act on right now.  
If talking about weights be sure to add KG to it.  
Don’t: use pet names, describe bodies, or lean into exaggerated romantic language.  
""".trimIndent()

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
        - No quotation marks in the final output.
-If the user adds 0.0kg in the workout consider it as bodyweight exercise
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
