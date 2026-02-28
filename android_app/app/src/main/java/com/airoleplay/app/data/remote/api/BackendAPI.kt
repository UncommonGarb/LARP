package com.airoleplay.app.data.remote.api

import com.airoleplay.app.data.remote.models.ConnectionTestResult
import com.airoleplay.app.data.remote.models.GenerationSettings
import com.airoleplay.app.data.remote.models.PromptMessage
import kotlinx.coroutines.flow.Flow

interface BackendAPI {
    val baseUrl: String

    suspend fun testConnection(): ConnectionTestResult
    suspend fun listModels(): List<String>

    // Some backends take a full string prompt (KoboldCPP), others take structured messages (Ollama)
    // The implementation will adapt based on what's provided.
    fun generateResponse(
        prompt: String?,
        messages: List<PromptMessage>?,
        settings: GenerationSettings
    ): Flow<String>

    suspend fun abortGeneration()
    suspend fun getContextSizeLimit(): Int
}
