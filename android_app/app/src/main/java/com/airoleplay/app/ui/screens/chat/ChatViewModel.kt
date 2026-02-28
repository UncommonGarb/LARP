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
    val connectionStatus: ConnectionStatus = ConnectionStatus.UNKNOWN
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

    init {
        if (sessionId != -1L) {
            loadChatData()
        }
        observeActiveConnection()
        observeActivePersona()
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
            }
        }
    }

    private fun observeActiveConnection() {
        viewModelScope.launch {
            settingsDao.getActiveConnection().collect { conn ->
                _uiState.update { it.copy(activeConnection = conn) }
                // Set default generation settings based on connection
                conn?.let {
                    currentGenSettings = currentGenSettings.copy(
                        model = it.modelName,
                        temperature = it.temperature,
                        topP = it.topP,
                        topK = it.topK,
                        repetitionPenalty = it.repetitionPenalty,
                        maxNewTokens = it.maxNewTokens
                    )
                }
                checkConnectionStatus()
            }
        }
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
            val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)
            val result = api.testConnection()
            _uiState.update {
                it.copy(connectionStatus = if (result.isSuccessful) ConnectionStatus.CONNECTED else ConnectionStatus.DISCONNECTED)
            }
        }
    }

    fun sendMessage(content: String, base64Images: List<String> = emptyList()) {
        if (content.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Save User Message
            val userMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "user",
                content = content
                // Note: Image persistence in DB would need an update to ChatMessageEntity to store the URI or Base64 string, omitted here for scope constraints
            )
            chatDao.insertMessage(userMsg)

            // 2. Update Session lastActive
            _uiState.value.session?.let {
                chatDao.updateSession(it.copy(lastActiveAt = System.currentTimeMillis()))
            }

            // 3. Trigger Generation
            // Ensure we fetch the most up-to-date messages including the one we just inserted before building prompt
            val latestMessages = chatDao.getMessagesForSession(sessionId).firstOrNull() ?: emptyList()
            _uiState.update { it.copy(messages = latestMessages) }

            generateAIResponse()
        }
    }

    fun regenerateLastMessage() {
        if (_uiState.value.isGenerating) return

        viewModelScope.launch(Dispatchers.IO) {
            val msgs = _uiState.value.messages
            if (msgs.isEmpty()) return@launch

            val lastMsg = msgs.last()
            if (lastMsg.role == "assistant") {
                // Deactivate current last message
                chatDao.deactivateMessage(lastMsg.id)
                generateAIResponse(lastMsg.swipeGroupId)
            } else {
                generateAIResponse()
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

    private suspend fun generateAIResponse(swipeGroupId: String? = null) {
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
            val lorebooks = settingsDao.getLorebookEntriesForCharacter(char.id).firstOrNull() ?: emptyList()

            // Add required Stop Sequences
            val personaName = state.activePersona?.name ?: "User"
            val stopSeqs = listOf(
                "$personaName:", "\n$personaName", "User:", "\nUser",
                "${char.name}:", "\n${char.name}"
            )
            currentGenSettings = currentGenSettings.copy(stopSequences = stopSeqs)

            val promptMessages = promptBuilder.buildPromptMessages(
                character = char,
                persona = state.activePersona,
                globalSystemPrompt = "You are {{char}}.", // Would normally come from settings
                messages = state.messages,
                lorebookEntries = lorebooks,
                authorsNote = char.authorsNote,
                authorsNoteDepth = char.authorsNoteDepth,
                contextSizeLimit = conn.contextSizeLimit
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

            // Start Flow
            generationJob = viewModelScope.launch(Dispatchers.IO) {
                try {
                    val flow = api.generateResponse(promptString, requestMessages, currentGenSettings)

                    var fullResponse = ""
                    flow.collect { chunk ->
                        fullResponse += chunk
                        _uiState.update { it.copy(partialGeneration = fullResponse) }
                    }

                    // Done streaming, save to DB
                    finalizeGeneration(fullResponse, swipeGroupId)

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
                content = "Error generating response: ${e.message}"
            )
            chatDao.insertMessage(errorMsg)
        }
    }

    private suspend fun finalizeGeneration(finalText: String, swipeGroupId: String? = null) {
        if (finalText.isNotBlank()) {
            // Check if we are regenerating, if so, we reuse the group ID, otherwise we create a new one
            val groupToUse = swipeGroupId ?: java.util.UUID.randomUUID().toString()

            val aiMsg = ChatMessageEntity(
                sessionId = sessionId,
                role = "assistant",
                content = finalText.trim(),
                swipeGroupId = groupToUse
            )
            chatDao.insertMessage(aiMsg)
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

    override fun onCleared() {
        super.onCleared()
        // Stop generation if we exit screen
        if (_uiState.value.isGenerating) {
            stopGeneration()
        }
    }
}
