package com.forgecompose.workouttracker.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GeminiUtlitityTest {

    @Test
    fun `cleanRuleList trims collapses whitespace removes duplicates and limits size`() {
        val result = invokePrivate<List<String>>(
            "cleanRuleList",
            listOf(
                "  Use slow eccentrics  ",
                "Use   slow   eccentrics",
                "",
                "  Focus on full range   of motion ",
                "Leave one rep in reserve",
                "Add a pause at the bottom"
            ),
            3
        )

        assertEquals(
            listOf(
                "Use slow eccentrics",
                "Focus on full range of motion",
                "Leave one rep in reserve"
            ),
            result
        )
    }

    @Test
    fun `inferWorkoutName extracts workout from context and strips trailing goal details`() {
        val result = invokePrivate<String?>(
            "inferWorkoutName",
            "Track progress for Barbell Row on upper day Goal: hypertrophy",
            listOf("irrelevant")
        )

        assertEquals("Barbell Row", result)
    }

    @Test
    fun `inferWorkoutName returns null when no workout markers exist`() {
        val result = invokePrivate<String?>(
            "inferWorkoutName",
            "General readiness check for today's session",
            listOf("sleep ok", "motivation high")
        )

        assertNull(result)
    }

    @Test
    fun `loadProgressionInstruction returns bodyweight guidance for bodyweight exercise`() {
        val result = invokePrivate<String>(
            "loadProgressionInstruction",
            "Push Up"
        )

        assertTrue(result.contains("do not suggest increasing or decreasing weight"))
        assertTrue(result.contains("progress with reps, sets, tempo"))
    }

    @Test
    fun `loadProgressionInstruction returns blank for weighted exercise`() {
        val result = invokePrivate<String>(
            "loadProgressionInstruction",
            "Barbell Bench Press"
        )

        assertTrue(result.isBlank())
    }

    @Test
    fun `GeminiSignalSnapshot round trips through json`() {
        val snapshot = GeminiSignalSnapshot(
            capturedAtEpochMs = 123456789L,
            recoveryEfficacy = 0.85f,
            avgInjuryRisk = 0.12f,
            avgLoadScore = 0.73f,
            highReadinessCount = 4,
            cautionCount = 1
        )

        val restored = GeminiSignalSnapshot.fromJson(snapshot.toJson().toString())

        assertNotNull(restored)
        assertEquals(snapshot.capturedAtEpochMs, restored?.capturedAtEpochMs)
        assertEquals(snapshot.recoveryEfficacy, restored?.recoveryEfficacy)
        assertEquals(snapshot.avgInjuryRisk, restored?.avgInjuryRisk)
        assertEquals(snapshot.avgLoadScore, restored?.avgLoadScore)
        assertEquals(snapshot.highReadinessCount, restored?.highReadinessCount)
        assertEquals(snapshot.cautionCount, restored?.cautionCount)
    }

    @Test
    fun `WorkoutTrendSnapshot fromJson keeps optional fields nullable when absent`() {
        val restored = WorkoutTrendSnapshot.fromJson(
            """
            {
              "workoutName":"Pull Up",
              "source":"history",
              "capturedAtEpochMs":42,
              "phase":"deload"
            }
            """.trimIndent()
        )

        assertNotNull(restored)
        assertEquals("Pull Up", restored?.workoutName)
        assertEquals("history", restored?.source)
        assertEquals(42L, restored?.capturedAtEpochMs)
        assertEquals("deload", restored?.phase)
        assertEquals(null, restored?.strengthChangePct)
        assertEquals(null, restored?.volumeChangePct)
        assertEquals(null, restored?.avgGapDays)
        assertEquals(null, restored?.rpe)
        assertEquals(null, restored?.fatigue)
        assertEquals(null, restored?.weight)
        assertEquals(null, restored?.sets)
        assertEquals(null, restored?.reps)
    }

    @Test
    fun `WorkoutTrendSnapshot round trips populated optional fields`() {
        val snapshot = WorkoutTrendSnapshot(
            workoutName = "Barbell Row",
            source = "trend_engine",
            capturedAtEpochMs = 99L,
            phase = "progress",
            strengthChangePct = 5.2,
            volumeChangePct = 8.5,
            avgGapDays = 2.3,
            rpe = 8,
            fatigue = 3,
            weight = 85.0,
            sets = 4,
            reps = 10
        )

        val restored = WorkoutTrendSnapshot.fromJson(snapshot.toJson().toString())

        assertNotNull(restored)
        assertEquals(snapshot, restored)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> invokePrivate(name: String, vararg args: Any?): T {
        val fileClass = Class.forName("com.forgecompose.workouttracker.ai.GeminiUtlitityKt")
        val method = fileClass.declaredMethods.firstOrNull { candidate ->
            candidate.name == name && candidate.parameterTypes.size == args.size
        } ?: error("Method $name with ${args.size} params not found")

        method.isAccessible = true
        return method.invoke(null, *args) as T
    }
}
