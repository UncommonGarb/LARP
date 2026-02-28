package com.airoleplay.app.data.remote.models

data class GenerationSettings(
    val model: String? = null,
    val temperature: Float = 0.8f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repetitionPenalty: Float = 1.1f,
    val maxNewTokens: Int = 400,
    val stopSequences: List<String> = emptyList()
)

data class PromptMessage(
    val role: String,
    val content: String,
    val images: List<String>? = null // Base64 encoded images for Multimodal models
)

data class ModelInfo(
    val name: String,
    val isMultimodal: Boolean = false
)

data class ConnectionTestResult(
    val isSuccessful: Boolean,
    val message: String,
    val models: List<ModelInfo> = emptyList(), // For Ollama
    val currentModel: String? = null // For KoboldCPP
)
