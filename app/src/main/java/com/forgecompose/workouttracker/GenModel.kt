package com.forgecompose.workouttracker

import com.google.ai.client.generativeai.GenerativeModel

object GeminiProvider {
    val model by lazy {
        GenerativeModel(
            modelName = "gemini-1.5-flash", // swap to 1.5-pro if you want more reasoning
            apiKey = "AIzaSyCZmWP9bR6hb8T78JUqAqA6AX_4ozYWimE"
        )
    }
}