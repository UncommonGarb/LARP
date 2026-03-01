package com.airoleplay.app.data.remote.api

import com.airoleplay.app.data.remote.models.ConnectionTestResult
import com.airoleplay.app.data.remote.models.GenerationSettings
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
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.IOException

class KoboldCPPClient(
    override val baseUrl: String,
    private val client: OkHttpClient,
    private val gson: Gson
) : BackendAPI {

    private var activeEventSource: EventSource? = null

    override suspend fun testConnection(): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/v1/model"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val root = gson.fromJson(responseBody, Map::class.java)
                val modelName = root["result"] as? String

                if (modelName.isNullOrBlank() || modelName == "none") {
                    ConnectionTestResult(false, "KoboldCPP connected, but NO MODEL is loaded.")
                } else {
                    ConnectionTestResult(true, "Connected! Model: $modelName", currentModel = modelName)
                }
            } else {
                ConnectionTestResult(false, "Failed to connect: ${response.code} ${response.message}")
            }
        } catch (e: Exception) {
            ConnectionTestResult(false, "Connection error: ${e.message}")
        }
    }

    override suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        val result = testConnection()
        if (result.isSuccessful && result.currentModel != null) {
            listOf(result.currentModel)
        } else {
            emptyList()
        }
    }

    override fun generateResponse(
        prompt: String?,
        messages: List<PromptMessage>?,
        settings: GenerationSettings
    ): Flow<String> = callbackFlow {
        if (prompt.isNullOrBlank()) {
            close(Exception("KoboldCPP requires a flat prompt string to generate."))
            return@callbackFlow
        }

        val url = "$baseUrl/api/extra/generate/stream"

        // Build JSON body
        val jsonMap = mutableMapOf<String, Any>(
            "prompt" to prompt,
            "max_length" to settings.maxNewTokens,
            "temperature" to settings.temperature,
            "top_p" to settings.topP,
            "top_k" to settings.topK,
            "rep_pen" to settings.repetitionPenalty,
            "stop_sequence" to settings.stopSequences
        )

        val jsonBody = gson.toJson(jsonMap)
        val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .header("Accept", "text/event-stream")
            .build()

        val eventSourceFactory = EventSources.createFactory(client)

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                // Connection opened
            }

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                try {
                    val root = gson.fromJson(data, Map::class.java)
                    val token = root["token"] as? String
                    if (token != null) {
                        trySend(token)
                    }
                } catch (e: Exception) {
                    // JSON Parse error for this event
                }
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                if (t != null) {
                    close(t)
                } else {
                    close(Exception("SSE Stream Failed: ${response?.code}"))
                }
            }
        }

        activeEventSource = eventSourceFactory.newEventSource(request, listener)

        awaitClose {
            activeEventSource?.cancel()
            activeEventSource = null
        }
    }

    override suspend fun abortGeneration() {
        withContext(Dispatchers.IO) {
            try {
                // First cancel the active connection/stream
                activeEventSource?.cancel()
                activeEventSource = null

                // Then send the abort signal to the backend
                val url = "$baseUrl/api/extra/abort"
                val request = Request.Builder()
                    .url(url)
                    .post("{}".toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull()))
                    .build()
                client.newCall(request).execute()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override suspend fun getContextSizeLimit(): Int = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/v1/config/max_context_length"
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val root = gson.fromJson(responseBody, Map::class.java)
                val limit = (root["value"] as? Double)?.toInt() ?: 4096
                limit
            } else {
                4096 // Fallback
            }
        } catch (e: Exception) {
            e.printStackTrace()
            4096 // Fallback
        }
    }

    suspend fun countTokensExact(prompt: String): Int = withContext(Dispatchers.IO) {
        try {
            val url = "$baseUrl/api/extra/tokencount"
            val jsonMap = mapOf("prompt" to prompt)
            val jsonBody = gson.toJson(jsonMap)
            val requestBody = jsonBody.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

            val request = Request.Builder().url(url).post(requestBody).build()
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string() ?: ""
                val root = gson.fromJson(responseBody, Map::class.java)
                val count = (root["value"] as? Double)?.toInt() ?: (prompt.length / 4)
                count
            } else {
                (prompt.length / 4) // Fallback heuristic
            }
        } catch (e: Exception) {
            e.printStackTrace()
            (prompt.length / 4) // Fallback heuristic
        }
    }
}
