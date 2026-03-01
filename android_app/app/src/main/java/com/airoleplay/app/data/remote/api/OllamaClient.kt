package com.airoleplay.app.data.remote.api

import com.airoleplay.app.data.remote.models.ConnectionTestResult
import com.airoleplay.app.data.remote.models.GenerationSettings
import com.airoleplay.app.data.remote.models.ModelInfo
import com.airoleplay.app.data.remote.models.PromptMessage
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class OllamaClient(
    override val baseUrl: String,
    private val client: OkHttpClient,
    private val gson: Gson
) : BackendAPI {

    private var activeCall: Call? = null

    override suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/tags"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val models = parseModelsList(responseBody)
                ConnectionTestResult(
                    isSuccessful = true,
                    message = "Connected to Ollama. Models retrieved.",
                    models = models
                )
            } else {
                ConnectionTestResult(false, "Failed to connect: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            ConnectionTestResult(false, "Connection error: ${e.message}")
        }
    }

    override suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val result = testConnection()
        if (result.isSuccessful) {
            result.models.map { it.name }
        } else {
            emptyList()
        }
    }

    override fun generateResponse(
        prompt: String?,
        messages: List<PromptMessage>?,
        settings: GenerationSettings
    ): Flow<String> = callbackFlow {
        if (settings.model == null) {
            close(Exception("Ollama requires a model name to generate."))
            return@callbackFlow
        }

        val url = "$baseUrl/api/chat"

        // Build JSON body
        val jsonMap = mutableMapOf<String, Any>(
            "model" to settings.model,
            "stream" to true
        )

        if (messages != null) {
             val mappedMessages = messages.map { msg ->
                 val msgMap = mutableMapOf<String, Any>(
                     "role" to msg.role,
                     "content" to msg.content
                 )
                 if (msg.images != null && msg.images.isNotEmpty()) {
                     msgMap["images"] = msg.images
                 }
                 msgMap
             }
             jsonMap["messages"] = mappedMessages
        } else if (prompt != null) {
            // Fallback for prompt if messages not provided, though Ollama prefers messages array
             jsonMap["messages"] = listOf(
                 mapOf("role" to "user", "content" to prompt)
             )
        }

        val optionsMap = mapOf(
            "temperature" to settings.temperature,
            "top_p" to settings.topP,
            "top_k" to settings.topK,
            "repeat_penalty" to settings.repetitionPenalty,
            "num_predict" to settings.maxNewTokens, // max new tokens
            "stop" to settings.stopSequences
        )

        jsonMap["options"] = optionsMap

        val jsonBody = gson.toJson(jsonMap)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        activeCall = client.newCall(request)

        activeCall?.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!call.isCanceled()) {
                    close(e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                try {
                    if (!response.isSuccessful) {
                        close(Exception("Server returned: ${response.code} ${response.message}"))
                        return
                    }

                    val source = response.body?.source()
                    if (source == null) {
                        close(Exception("Empty response body"))
                        return
                    }

                    // Read newline-delimited JSON
                    while (!source.exhausted() && !call.isCanceled()) {
                        val line = source.readUtf8Line()
                        if (line != null && line.isNotBlank()) {
                            try {
                                val chunk = gson.fromJson(line, Map::class.java)
                                val messageBlock = chunk["message"] as? Map<*, *>
                                val content = messageBlock?.get("content") as? String
                                val done = chunk["done"] as? Boolean ?: false

                                if (content != null) {
                                    trySend(content)
                                }

                                if (done) {
                                    close()
                                    return
                                }
                            } catch (e: Exception) {
                                // JSON parse error on a chunk, ignore or close
                            }
                        }
                    }
                    close()
                } catch (e: Exception) {
                    if (!call.isCanceled()) {
                        close(e)
                    }
                }
            }
        })

        awaitClose {
            activeCall?.cancel()
            activeCall = null
        }
    }

    override suspend fun abortGeneration() {
        activeCall?.cancel()
        activeCall = null
    }

    override suspend fun getContextSizeLimit(): Int {
        // Ollama usually defaults to 2048, and configurable via `num_ctx`.
        // Returning a large default, since we let the user define the limit in settings anyway.
        return 4096
    }

    private fun parseModelsList(json: String): List<ModelInfo> {
        val models = mutableListOf<ModelInfo>()
        try {
            val root = gson.fromJson(json, Map::class.java)
            val modelsArray = root["models"] as? List<Map<String, Any>>
            modelsArray?.forEach { modelMap ->
                val name = modelMap["name"] as? String
                val details = modelMap["details"] as? Map<String, Any>
                val isMultimodal = details?.get("family") == "clip" || details?.get("families")?.toString()?.contains("clip") == true // basic multimodal check
                if (name != null) {
                    models.add(ModelInfo(name, isMultimodal))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return models
    }
}
