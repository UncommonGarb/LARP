package com.airoleplay.app.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.data.local.entity.MemoryEntity
import com.airoleplay.app.data.local.entity.GlobalSettingsEntity
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import com.airoleplay.app.data.remote.models.GenerationSettings
import com.airoleplay.app.di.NetworkModule
import com.airoleplay.app.domain.prompt.InstructFormat
import com.airoleplay.app.domain.prompt.PromptBuilder
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import javax.inject.Inject

data class ChatUiState(
    val character: CharacterEntity? = null,
    val session: ChatSessionEntity? = null,
    val messages: List<ChatMessageEntity> = emptyList(),
    val isGenerating: Boolean = false,
    val partialGeneration: String = "",
    val activeConnection: BackendConnectionEntity? = null,
    val activePersona: UserPersonaEntity? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN,
    val currentContextTokens: Int = 0,
    val maxContextTokens: Int = 4096,
    val memories: List<MemoryEntity> = emptyList()
)

enum class ConnectionStatus {
    UNKNOWN, CONNECTED, DISCONNECTED, CONNECTING
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val chatDao: ChatDao,
    private val characterDao: CharacterDao,
    private val settingsDao: SettingsDao,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
    private val promptBuilder: PromptBuilder
) : ViewModel() {

    private val sessionId: Long = savedStateHandle.get<String>("sessionId")?.toLongOrNull() ?: -1

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()

    private var generationJob: Job? = null

    // Default Generation Settings (will be overridable per session later)
    var currentGenSettings = GenerationSettings()

    // Cached global settings for fallback
    private var cachedGlobalSettings: GlobalSettingsEntity? = null

    init {
        if (sessionId != -1L) {
            loadChatData()
        }
        observeActiveConnection()
        observeActivePersona()
        observeGlobalSettings()
        startConnectionPolling()
    }

    private fun startConnectionPolling() {
        viewModelScope.launch {
            while (true) {
                checkConnectionStatus()
                kotlinx.coroutines.delay(30_000) // Poll every 30 seconds
            }
        }
    }

    private fun loadChatData() {
        viewModelScope.launch {
            chatDao.getMessagesForSession(sessionId).collect { msgs ->
                _uiState.update { it.copy(messages = msgs) }
            }
        }

        // Load Session and Character info initially (non-flow to avoid loop issues, or flow if needed)
        // For simplicity, fetching once here
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.getAllSessions().firstOrNull()?.find { it.id == sessionId }?.let { session ->
                _uiState.update { it.copy(session = session) }
                characterDao.getCharacterById(session.characterId).firstOrNull()?.let { char ->
                    _uiState.update { it.copy(character = char) }
                }
                // Load memories for this session
                observeMemories(session.characterId)
            }
        }
    }

    private fun observeMemories(characterId: Long) {
        viewModelScope.launch {
            chatDao.getMemoriesForSession(sessionId).collect { memories ->
                _uiState.update { it.copy(memories = memories) }
            }
        }
    }

    private fun observeGlobalSettings() {
        viewModelScope.launch {
            settingsDao.getGlobalSettings().collect { settings ->
                cachedGlobalSettings = settings
                // Re-apply generation settings if the active connection doesn't use custom
                val conn = _uiState.value.activeConnection
                if (conn != null && !conn.useCustomSettings && settings != null) {
                    applyGlobalSettings(settings, conn)
                }
            }
        }
    }

    private fun observeActiveConnection() {
        viewModelScope.launch {
            settingsDao.getActiveConnection().collect { conn ->
                _uiState.update { it.copy(activeConnection = conn) }
                // Set generation settings based on connection or global defaults
                conn?.let {
                    if (it.useCustomSettings) {
                        // Use per-connection gen params
                        currentGenSettings = currentGenSettings.copy(
                            model = it.modelName,
                            temperature = it.temperature,
                            topP = it.topP,
                            topK = it.topK,
                            repetitionPenalty = it.repetitionPenalty,
                            maxNewTokens = it.maxNewTokens
                        )
                    } else {
                        // Use global defaults, falling back to connection values only for model name
                        val global = cachedGlobalSettings
                        if (global != null) {
                            applyGlobalSettings(global, it)
                        } else {
                            // Fallback: use connection values until globals load
                            currentGenSettings = currentGenSettings.copy(
                                model = it.modelName,
                                temperature = it.temperature,
                                topP = it.topP,
                                topK = it.topK,
                                repetitionPenalty = it.repetitionPenalty,
                                maxNewTokens = it.maxNewTokens
                            )
                        }
                    }
                }
                checkConnectionStatus()
            }
        }
    }

    private fun applyGlobalSettings(global: GlobalSettingsEntity, conn: BackendConnectionEntity) {
        currentGenSettings = currentGenSettings.copy(
            model = conn.modelName,
            temperature = global.defaultTemperature,
            topP = global.defaultTopP,
            topK = global.defaultTopK,
            repetitionPenalty = global.defaultRepetitionPenalty,
            maxNewTokens = global.defaultMaxNewTokens
        )
    }

    private fun observeActivePersona() {
        viewModelScope.launch {
            settingsDao.getActivePersona().collect { persona ->
                _uiState.update { it.copy(activePersona = persona) }
            }
        }
    }

    fun checkConnectionStatus() {
        val conn = _uiState.value.activeConnection ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTING) }
            try {
                val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)
                val result = api.testConnection()
                if (result.isSuccessful) {
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.CONNECTED) }
                    // Fetch and apply context size from backend
                    val backendContextSize = api.getContextSizeLimit()
                    if (backendContextSize > 0) {
                        _uiState.update { it.copy(maxContextTokens = backendContextSize) }
                    }
                } else {
                    _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(connectionStatus = ConnectionStatus.DISCONNECTED) }
            }
        }
    }

    fun sendMessage(content: String, attachedImagePath: String? = null) {
        if (content.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Save User Message
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = content,
                attachedImagePath = attachedImagePath
            )
            chatDao.insertMessage(userMsg)

            // 2. Update Session lastActive
            _uiState.value.session?.let {
                chatDao.updateSession(it.copy(lastActiveAt = System.currentTimeMillis()))
            }

            // 3. Trigger Generation
            // Fetch explicit latest list to avoid race conditions with state updates
            val latestMessages = chatDao.getMessagesForSession(sessionId).first()
            generateAIResponse(latestMessages = latestMessages)
        }
    }

    fun regenerateLastMessage() {
        if (_uiState.value.isGenerating) return

        viewModelScope.launch(Dispatchers.IO) {
            val msgs = chatDao.getMessagesForSession(sessionId).first()
            if (msgs.isEmpty()) return@launch

            val lastMsg = msgs.last()
            if (lastMsg.role == "assistant") {
                // Deactivate current last message
                chatDao.deactivateMessage(lastMsg.id)
                // Fetch again to get the clean list without the deactivated message
                val cleanMessages = chatDao.getMessagesForSession(sessionId).first()
                generateAIResponse(swipeGroupId = lastMsg.swipeGroupId, latestMessages = cleanMessages)
            } else {
                generateAIResponse(latestMessages = msgs)
            }
        }
    }

    fun switchSwipeAlternative(groupId: String, messageId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deactivateAllInSwipeGroup(groupId)
            chatDao.activateMessage(messageId)
        }
    }

    private val alternativesCache = mutableMapOf<String, List<ChatMessageEntity>>()

    suspend fun getSwipeAlternatives(groupId: String): List<ChatMessageEntity> {
        if (!alternativesCache.containsKey(groupId)) {
            val msgs = chatDao.getMessagesBySwipeGroupId(groupId)
            alternativesCache[groupId] = msgs
        }
        return alternativesCache[groupId] ?: emptyList()
    }

    fun clearAlternativesCache(groupId: String) {
        alternativesCache.remove(groupId)
    }

    private suspend fun generateAIResponse(
        swipeGroupId: String? = null,
        latestMessages: List<ChatMessageEntity>? = null
    ) {
        val state = _uiState.value
        val char = state.character ?: return
        val conn = state.activeConnection

        if (conn == null) {
            // Handle no connection case (e.g. show error message in chat)
             val errorMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = "Error: No active backend connection configured. Please set one up in Settings."
            )
            chatDao.insertMessage(errorMsg)
            return
        }

        val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)

        _uiState.update { it.copy(isGenerating = true, partialGeneration = "") }

        try {
            // Build Prompt
            val globalSettings = settingsDao.getGlobalSettings().firstOrNull()
            val globalPrompt = globalSettings?.globalSystemPrompt ?: "You are {{char}}."
            val lorebooks = settingsDao.getLorebookEntriesForCharacter(char.id).firstOrNull() ?: emptyList()

            // Add required Stop Sequences
            val personaName = state.activePersona?.name ?: "User"
            val stopSeqs = mutableListOf(
                "$personaName:", "\n$personaName", "User:", "\nUser",
                "${char.name}:", "\n${char.name}",
                "<$personaName>", "</$personaName>",
                "**$personaName:**", "\n**$personaName**"
            )

            // Determine context size based on connection custom vs. global vs. backend
            // Prefer backend-sourced value if we just connected, otherwise follow settings
            var contextSize = if (conn.useCustomSettings) {
                conn.contextSizeLimit
            } else {
                globalSettings?.defaultContextSizeLimit ?: conn.contextSizeLimit
            }

            // Sync with UI state which might have been updated from backend
            if (_uiState.value.maxContextTokens != 4096 && _uiState.value.maxContextTokens != contextSize) {
                contextSize = _uiState.value.maxContextTokens
            }
            
            _uiState.update { it.copy(maxContextTokens = contextSize) }

            // Add format specific stop sequences
            if (conn.type.uppercase() == "KOBOLDCPP") {
                when (conn.instructFormat.uppercase().replace(" ", "")) {
                    "CHATML" -> stopSeqs.addAll(listOf("<|im_end|>", "<|im_start|>", "<|im_start|>assistant", "<|im_start|>user", "<|im_start|>system"))
                    "LLAMA3" -> stopSeqs.addAll(listOf("<|eot_id|>", "<|start_header_id|>", "<|begin_of_text|>"))
                    "ALPACA" -> stopSeqs.addAll(listOf("### Instruction:", "### Response:", "### Input:"))
                    "MISTRAL" -> stopSeqs.addAll(listOf("[INST]", "[/INST]"))
                }
            } else if (conn.type.uppercase() == "OLLAMA") {
                 // Ollama handles tokens internally but extra stop sequences can't hurt
                 stopSeqs.addAll(listOf("<|im_end|>", "<|im_start|>", "<|eot_id|>", "<|start_header_id|>", "### Instruction:", "### Response:", "[INST]"))
            }

            currentGenSettings = currentGenSettings.copy(stopSequences = stopSeqs)

            val promptMessages = promptBuilder.buildPromptMessages(
                character = char,
                persona = state.activePersona,
                globalSystemPrompt = globalPrompt,
                messages = latestMessages ?: state.messages,
                lorebookEntries = lorebooks,
                authorsNote = char.authorsNote,
                authorsNoteDepth = char.authorsNoteDepth,
                contextSizeLimit = contextSize
            )

            // If Kobold, we need flat string
            var promptString: String? = null
            var requestMessages: List<com.airoleplay.app.data.remote.models.PromptMessage>? = null

            if (conn.type.uppercase() == "KOBOLDCPP") {
                val format = InstructFormat.valueOf(conn.instructFormat.uppercase().replace(" ", ""))
                promptString = com.airoleplay.app.domain.prompt.InstructFormatter.format(promptMessages, format)
            } else {
                requestMessages = promptMessages
            }

            val estimatedTokens = if (promptString != null) {
                promptString.length / 4
            } else {
                requestMessages?.sumOf { it.content.length / 4 } ?: 0
            }
            _uiState.update { it.copy(currentContextTokens = estimatedTokens) }

            // Start Flow — STREAMING FIX: use StringBuilder and flowOn(Default) for responsive UI updates
            generationJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val flow = api.generateResponse(promptString, requestMessages, currentGenSettings)

                    val responseBuilder = StringBuilder()
                    flow
                        .flowOn(Dispatchers.IO) // Ensure collection happens on IO
                        .collect { chunk ->
                            responseBuilder.append(chunk)
                            val currentText = responseBuilder.toString()
                            // Update on Main dispatcher for immediate UI recomposition
                            withContext(Dispatchers.Main) {
                                _uiState.update { it.copy(partialGeneration = currentText) }
                            }
                        }

                    // Done streaming, save to DB
                    finalizeGeneration(responseBuilder.toString(), swipeGroupId)

                } catch (e: Exception) {
                    // Save whatever we got so far, plus error if needed
                    e.printStackTrace()
                    finalizeGeneration(_uiState.value.partialGeneration + "\n\n[Connection Interrupted]", swipeGroupId)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
             _uiState.update { it.copy(isGenerating = false, partialGeneration = "") }
             val errorMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = "Connection Error: ${e.message}\nPlease check your backend settings and try again.",
                isError = true
            )
            chatDao.insertMessage(errorMsg)
        }
    }

    private suspend fun finalizeGeneration(finalText: String, swipeGroupId: String? = null) {
        var processedText = finalText.trim()
        
        // Optional: Remove unfinished sentence
        if (cachedGlobalSettings?.trimIncompleteSentences == true && processedText.isNotEmpty()) {
            val lastPunctuation = processedText.lastIndexOfAny(charArrayOf('.', '!', '?', '"', '*', ')'))
            if (lastPunctuation != -1 && lastPunctuation < processedText.length - 1) {
                // Check if the remaining text is alphanumeric (i.e., a cut-off word/sentence)
                val remaining = processedText.substring(lastPunctuation + 1)
                if (remaining.any { it.isLetterOrDigit() }) {
                    processedText = processedText.substring(0, lastPunctuation + 1)
                }
            }
        }

        if (processedText.isNotBlank()) {
            // Check if we are regenerating, if so, we reuse the group ID, otherwise we create a new one
            val groupToUse = swipeGroupId ?: java.util.UUID.randomUUID().toString()

            val aiMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = processedText,
                swipeGroupId = groupToUse
            )
            chatDao.insertMessage(aiMsg)

            clearAlternativesCache(groupToUse)

            // Wait for DB to update and Flow to emit before clearing partial
            // This prevents the visual "pop"
            chatDao.getMessagesForSession(sessionId).first { msgs ->
                msgs.any { it.swipeGroupId == groupToUse && it.content.trim() == processedText.trim() }
            }
        }
        _uiState.update { it.copy(isGenerating = false, partialGeneration = "") }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        viewModelScope.launch(Dispatchers.IO) {
            val conn = _uiState.value.activeConnection
            if (conn != null) {
                val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)
                api.abortGeneration()
            }
            finalizeGeneration(_uiState.value.partialGeneration)
        }
    }

    fun deleteMessage(message: ChatMessageEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMessage(message)
        }
    }

    fun editMessage(message: ChatMessageEntity, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val updatedMessage = message.copy(content = newContent)
            chatDao.updateMessage(updatedMessage)

            // Clear cache if editing an AI message that's part of a swipe group
            message.swipeGroupId?.let { groupId ->
                clearAlternativesCache(groupId)
            }
        }
    }

    // --- Memory Bank ---

    fun toggleMemory(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMemory(memory.copy(isActive = !memory.isActive))
        }
    }

    fun pinMemory(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMemory(memory.copy(isPinned = !memory.isPinned))
        }
    }

    fun deleteMemory(memory: MemoryEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteMemory(memory)
        }
    }

    fun editMemory(memory: MemoryEntity, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.updateMemory(memory.copy(content = newContent))
        }
    }

    fun extractMemory() {
        if (_uiState.value.isGenerating) return

        val state = _uiState.value
        val char = state.character ?: return
        val conn = state.activeConnection ?: return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isGenerating = true, partialGeneration = "[Extracting Memory...]") }

            try {
                val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)

                // Get the last 20 messages for context
                val recentMessages = chatDao.getMessagesForSession(sessionId).firstOrNull()?.takeLast(20) ?: emptyList()
                if (recentMessages.isEmpty()) {
                    _uiState.update { it.copy(isGenerating = false, partialGeneration = "") }
                    return@launch
                }

                val personaName = state.activePersona?.name ?: "User"
                val conversationText = recentMessages.joinToString("\n") { "${if (it.role == "user") personaName else char.name}: ${it.content}" }

                val promptStr = "Analyze the following conversation and extract ONLY the new, important factual information about the characters (like relationships, birthdays, locations, specific past events mentioned). Format each fact as a bullet point starting with '- '. Do not include opinions or summaries, only facts.\n\nConversation:\n$conversationText\n\nFacts:"

                val promptMessage = com.airoleplay.app.data.remote.models.PromptMessage(role = "user", content = promptStr)

                var requestMessages: List<com.airoleplay.app.data.remote.models.PromptMessage>? = listOf(promptMessage)
                var rawPromptStr: String? = null

                if (conn.type.uppercase() == "KOBOLDCPP") {
                    val format = InstructFormat.valueOf(conn.instructFormat.uppercase().replace(" ", ""))
                    rawPromptStr = com.airoleplay.app.domain.prompt.InstructFormatter.format(requestMessages!!, format)
                }

                val settings = currentGenSettings.copy(maxNewTokens = 300, temperature = 0.3f, stopSequences = listOf("<|im_end|>", "<|eot_id|>", "Conversation:"))
                val flow = api.generateResponse(rawPromptStr, requestMessages, settings)

                val memoryResult = StringBuilder()
                flow.collect { chunk ->
                    memoryResult.append(chunk)
                }

                val resultText = memoryResult.toString().trim()
                if (resultText.isNotBlank()) {
                    // Parse bullet points into individual memories
                    val bullets = resultText.lines()
                        .map { it.trim() }
                        .filter { it.startsWith("- ") || it.startsWith("• ") || it.startsWith("* ") }
                        .map { it.removePrefix("- ").removePrefix("• ").removePrefix("* ").trim() }
                        .filter { it.isNotBlank() }

                    if (bullets.isNotEmpty()) {
                        bullets.forEach { fact ->
                            chatDao.insertMemory(
                                MemoryEntity(
                                    sessionId = sessionId,
                                    characterId = char.id,
                                    content = fact
                                )
                            )
                        }

                        // Add a system message to chat to notify the user
                        val systemMsg = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = "[Memory Extracted — ${bullets.size} facts saved]\n${bullets.joinToString("\n") { "• $it" }}"
                        )
                        chatDao.insertMessage(systemMsg)
                    } else {
                        // Fallback: save the whole thing as one memory if no bullet format detected
                        chatDao.insertMemory(
                            MemoryEntity(
                                sessionId = sessionId,
                                characterId = char.id,
                                content = resultText
                            )
                        )
                        val systemMsg = ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = "[Memory Extracted & Saved]\n$resultText"
                        )
                        chatDao.insertMessage(systemMsg)
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _uiState.update { it.copy(isGenerating = false, partialGeneration = "") }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Stop generation if we exit screen
        if (_uiState.value.isGenerating) {
            stopGeneration()
        }
    }
}
