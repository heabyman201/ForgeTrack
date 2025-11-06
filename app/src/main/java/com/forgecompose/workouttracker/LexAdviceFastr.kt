package com.forgecompose.workouttracker



import kotlin.math.abs
import kotlin.random.Random

data class AdviceRequest(val context: String, val metrics: Map<String, Any?> = emptyMap(), val seed: Long = 0L)
data class AdviceOutput(val text: String, val tags: Set<String>, val score: Float)

data class Rule(val id: String, val includeIds: IntArray, val excludeIds: IntArray, val weight: Float, val tags: Set<String>, val templateIds: IntArray)
data class Template(val id: String, val text: String)

class LexAdviceFast(
    private val vocab: Vocab,
    private val rules: Array<Rule>,
    private val templates: Array<Template>,
    private val templateIndex: Map<String, Int>
) {
    fun advise(req: AdviceRequest): AdviceOutput {
        val ctx = req.context.lowercase()
        val bagIds = vocab.tokenizeToIds(ctx)
        val matched = ArrayList<Pair<Int, Float>>(8)
        for (i in rules.indices) {
            val r = rules[i]
            if (!containsAll(bagIds, r.includeIds)) continue
            if (containsAny(bagIds, r.excludeIds)) continue
            matched.add(i to r.weight)
        }
        if (matched.isEmpty()) {
            val base = templates[templateIndex.getValue("fallback_generic")]
            val filled = fill(base.text, req.metrics, bagIds)
            return AdviceOutput(filled, emptySet(), 0f)
        }
        matched.sortByDescending { it.second }
        var total = 0f
        for (p in matched) total += p.second
        val rng = Random(req.seed xor ctx.hashCode().toLong())
        val pick = weightedPick(matched, total, rng)
        val r = rules[pick]
        val tid = if (r.templateIds.size == 1) r.templateIds[0] else r.templateIds[abs(rng.nextInt()) % r.templateIds.size]
        val base = templates[tid]
        val filled = fill(base.text, req.metrics, bagIds)
        val score = (r.weight / total).coerceIn(0f, 1f)
        return AdviceOutput(filled, r.tags, score)
    }

    private fun containsAll(bag: IntArray, targets: IntArray): Boolean {
        for (t in targets) if (t !in bag) return false
        return true
    }

    private fun containsAny(bag: IntArray, targets: IntArray): Boolean {
        for (t in targets) if (t in bag) return true
        return false
    }

    private fun weightedPick(items: List<Pair<Int, Float>>, total: Float, rng: Random): Int {
        var r = rng.nextFloat() * total
        for (p in items) {
            r -= p.second
            if (r <= 0f) return p.first
        }
        return items.last().first
    }

    private fun num(key: String, m: Map<String, Any?>, d: Int): Int {
        val v = m[key] ?: return d
        return when (v) { is Int -> v; is Long -> v.toInt(); is Float -> v.toInt(); is Double -> v.toInt(); is String -> v.toIntOrNull() ?: d; else -> d }
    }

    private fun fnum(key: String, m: Map<String, Any?>, d: Float): Float {
        val v = m[key] ?: return d
        return when (v) { is Int -> v.toFloat(); is Long -> v.toFloat(); is Float -> v; is Double -> v.toFloat(); is String -> v.toFloatOrNull() ?: d; else -> d }
    }

    private fun str(key: String, m: Map<String, Any?>, d: String): String {
        val v = m[key] ?: return d
        return v.toString()
    }

    private fun fill(t: String, m: Map<String, Any?>, bagIds: IntArray): String {
        val sets = num("sets", m, 3)
        val reps = num("reps", m, 8)
        val weight = fnum("weight", m, 0f)
        val rpe = fnum("rpe", m, 7f)
        val hr = fnum("hr", m, 0f)
        val hrv = fnum("hrv", m, 0f)
        val sleep = fnum("sleep_h", m, 7.5f)
        val muscle = str("muscle", m, guessMuscle(bagIds))
        val load = if (weight > 0f) "${"%.0f".format(weight)}kg" else "${reps}reps"
        val rest = num("rest_s", m, 120)
        val delta = decideDelta(bagIds, rpe, hr, hrv, sleep)
        val dir = when {
            delta > 0.05f -> "+${"%.0f".format(delta * 100)}%"
            delta < -0.05f -> "${"%.0f".format(delta * 100)}%"
            else -> "keep"
        }
        val pace = when {
            vocab.has(bagIds, "tempo") || vocab.has(bagIds, "slow") -> "slow controlled"
            vocab.has(bagIds, "speed") || vocab.has(bagIds, "fast") -> "dynamic but clean"
            else -> "steady"
        }
        return t.replace("{muscle}", muscle)
            .replace("{sets}", sets.toString())
            .replace("{reps}", reps.toString())
            .replace("{load}", load)
            .replace("{rest_s}", rest.toString())
            .replace("{delta}", dir)
            .replace("{pace}", pace)
    }

    private fun guessMuscle(ids: IntArray): String {
        val m = arrayOf("chest","back","legs","quads","hamstrings","glutes","biceps","triceps","shoulders","delts","abs","calves")
        for (w in m) if (vocab.has(ids, w)) return w
        return "target"
    }

    private fun decideDelta(ids: IntArray, rpe: Float, hr: Float, hrv: Float, sleep: Float): Float {
        var s = 0f
        if (vocab.has(ids, "easy") || rpe <= 6.5f) s += 0.12f
        if (vocab.has(ids, "hard") || rpe >= 8.5f) s -= 0.12f
        if (hrv >= 70f) s += 0.06f
        if (sleep >= 8f) s += 0.04f
        if (hr >= 150f) s -= 0.06f
        if (vocab.has(ids, "pain") || vocab.has(ids, "sore")) s -= 0.15f
        if (vocab.has(ids, "plateau") || vocab.has(ids, "stalled")) s += 0.08f
        return s.coerceIn(-0.25f, 0.25f)
    }
}

class Vocab(
    words: Array<String>,
    private val synonyms: Map<String, Array<String>> = emptyMap()
) {
    private val index: Map<String, Int> = buildMap(words.size + synonyms.size * 2) {
        var i = 0
        for (w in words) put(w, i++)
    }
    fun idOf(w: String): Int = index[w] ?: -1
    fun has(ids: IntArray, word: String): Boolean {
        val id = idOf(word)
        if (id >= 0 && id in ids) return true
        val syn = synonyms[word] ?: return false
        for (s in syn) {
            val sid = idOf(s)
            if (sid >= 0 && sid in ids) return true
        }
        return false
    }
    fun tokenizeToIds(s: String): IntArray {
        val out = IntArray(s.length)
        var n = 0
        var i = 0
        val L = s.length
        while (i < L) {
            while (i < L && !isWordChar(s[i])) i++
            val start = i
            while (i < L && isWordChar(s[i])) i++
            if (start < i) {
                val w = s.substring(start, i)
                val id = idOf(w)
                if (id >= 0 && id !in out.copyOf(n)) {
                    out[n++] = id
                } else {
                    val k = synonyms.keys.firstOrNull { it == w }
                    if (k != null) {
                        val kid = idOf(k)
                        if (kid >= 0 && kid !in out.copyOf(n)) out[n++] = kid
                    }
                }
            }
        }
        return out.copyOf(n)
    }
    private fun isWordChar(c: Char): Boolean {
        val x = c.code
        return (x in 97..122) || (x in 48..57) || x == 95
    }
}

object LexAdviceFactory {
    fun build(): LexAdviceFast {
        val vocabWords = arrayOf(
            "easy","hard","pain","sore","plateau","stalled","form","technique","tempo",
            "slow","fast","speed","slept","good","sleep","tired","fatigued","exhausted",
            "hr_high","pulse_fast","hrv_low","low_hrv","chest","back","legs","quads","hamstrings","glutes",
            "biceps","triceps","shoulders","delts","abs","calves"
        )
        val synonyms = mapOf(
            "tired" to arrayOf("fatigued","exhausted","drained"),
            "pain" to arrayOf("ache","hurt"),
            "sore" to arrayOf("doms"),
            "easy" to arrayOf("light","comfortable"),
            "plateau" to arrayOf("stalled","stuck"),
            "form" to arrayOf("technique"),
            "tempo" to arrayOf("controlled","eccentric","concentric"),
            "hr_high" to arrayOf("tachy","pulse_fast"),
            "hrv_low" to arrayOf("low_hrv")
        )
        val vocab = Vocab(vocabWords, synonyms)
        val templates = arrayOf(
            Template("fallback_generic","Keep {pace} work on {muscle}. {delta}. Run {sets}x{reps} at {load}, rest {rest_s}s."),
            Template("volume_push","Increase volume on {muscle}. {delta}. Do {sets}x{reps}, maintain {pace}, rest {rest_s}s."),
            Template("intensity_push","Push load on {muscle}. {delta}. Hold {sets} sets of {reps}, rest {rest_s}s, keep technique {pace}."),
            Template("deload","Back off today. Reduce load on {muscle}. {delta}. Keep {sets}x{reps}, extend rest to {rest_s}s, stay {pace}."),
            Template("skill_focus","Form focus for {muscle}. {delta}. Use {sets}x{reps}, {pace} tempo, rest {rest_s}s.")
        )
        val tIndex = templates.indices.associateBy { templates[it].id }
        fun ids(vararg words: String) = words.map { vocab.idOf(it) }.filter { it >= 0 }.toIntArray()
        val rules = arrayOf(
            Rule("r_easy_progress", ids("easy"), ids("pain","sore"), 1.0f, setOf("progress"), intArrayOf(tIndex.getValue("volume_push"), tIndex.getValue("intensity_push"))),
            Rule("r_plateau", ids("plateau","stalled"), intArrayOf(), 1.2f, setOf("progress"), intArrayOf(tIndex.getValue("intensity_push"), tIndex.getValue("skill_focus"))),
            Rule("r_sleep_good", ids("slept","good","sleep"), ids("pain"), 0.8f, setOf("readiness"), intArrayOf(tIndex.getValue("volume_push"))),
            Rule("r_fatigued", ids("tired","fatigued","exhausted"), intArrayOf(), 1.3f, setOf("recovery"), intArrayOf(tIndex.getValue("deload"))),
            Rule("r_pain", ids("pain","sore"), intArrayOf(), 1.5f, setOf("safety"), intArrayOf(tIndex.getValue("deload"))),
            Rule("r_hr_high", ids("hr_high","pulse_fast"), intArrayOf(), 1.1f, setOf("recovery"), intArrayOf(tIndex.getValue("deload"))),
            Rule("r_hrv_low", ids("hrv_low"), intArrayOf(), 1.1f, setOf("recovery"), intArrayOf(tIndex.getValue("deload"))),
            Rule("r_form", ids("form","technique","tempo"), intArrayOf(), 0.9f, setOf("skill"), intArrayOf(tIndex.getValue("skill_focus")))
        )
        return LexAdviceFast(vocab, rules, templates, tIndex)
    }
}

object LexAdviceFacadeFast {
    private val engine by lazy { LexAdviceFactory.build() }
    fun advise(context: String, metrics: Map<String, Any?> = emptyMap(), seed: Long = 0L): AdviceOutput {
        return engine.advise(AdviceRequest(context, metrics, seed))
    }
}
