package com.driftdetector.app.core.ai

import android.content.Context
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Initialize RunAnywhere SDK for on-device AI
 */
object RunAnywhereInitializer {

    private var isInitialized = false

    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) {
            Timber.d("RunAnywhere SDK already initialized")
            return@withContext
        }

        try {
            Timber.d("Initializing RunAnywhere SDK...")

            // Step 1: Initialize SDK
            RunAnywhere.initialize(
                context = context,
                apiKey = "dev",  // Any string in DEVELOPMENT mode
                environment = SDKEnvironment.DEVELOPMENT
            )

            // Step 2: Register LLM Service Provider
            LlamaCppServiceProvider.register()

            // Step 3: Register lightweight models (optimized for drift analysis)
            registerModels()

            // Step 4: Scan for previously downloaded models
            RunAnywhere.scanForDownloadedModels()

            isInitialized = true
            Timber.i("✅ RunAnywhere SDK initialized successfully")

        } catch (e: Exception) {
            Timber.e(e, "❌ Failed to initialize RunAnywhere SDK")
            throw e
        }
    }

    private suspend fun registerModels() {
        try {
            // SmolLM2 360M - Fastest, smallest (119 MB) - Best for quick explanations
            addModelFromURL(
                url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
                name = "SmolLM2 360M Q8_0",
                type = "LLM"
            )
            Timber.d("Registered SmolLM2 360M model")

            // Qwen 2.5 0.5B - Better quality (374 MB) - For detailed analysis
            addModelFromURL(
                url = "https://huggingface.co/Triangle104/Qwen2.5-0.5B-Instruct-Q6_K-GGUF/resolve/main/qwen2.5-0.5b-instruct-q6_k.gguf",
                name = "Qwen 2.5 0.5B Instruct Q6_K",
                type = "LLM"
            )
            Timber.d("Registered Qwen 2.5 0.5B model")
        } catch (e: Exception) {
            Timber.e(e, "Error registering models")
            throw e
        }
    }

    fun isInitialized(): Boolean = isInitialized
}
