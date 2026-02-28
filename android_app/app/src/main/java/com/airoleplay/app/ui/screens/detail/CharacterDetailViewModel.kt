package com.airoleplay.app.ui.screens.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CharacterDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val characterDao: CharacterDao,
    private val chatDao: ChatDao
) : ViewModel() {

    private val characterId: Long = savedStateHandle.get<String>("characterId")?.toLongOrNull() ?: -1

    private val _character = MutableStateFlow<CharacterEntity?>(null)
    val character = _character.asStateFlow()

    // Using a Flow to signal the UI when export is ready
    private val _exportUri = MutableStateFlow<android.net.Uri?>(null)
    val exportUri = _exportUri.asStateFlow()

    private val _pastSessions = MutableStateFlow<List<ChatSessionEntity>>(emptyList())
    val pastSessions = _pastSessions.asStateFlow()

    init {
        if (characterId != -1L) {
            viewModelScope.launch {
                characterDao.getCharacterById(characterId).collect {
                    _character.value = it
                }
            }
            viewModelScope.launch {
                chatDao.getSessionsForCharacter(characterId).collect {
                    _pastSessions.value = it
                }
            }
        }
    }

    fun startNewChatSession(title: String, onSessionCreated: (Long) -> Unit) {
        viewModelScope.launch {
            val session = ChatSessionEntity(
                characterId = characterId,
                title = title
            )
            val newSessionId = chatDao.insertSession(session)
            onSessionCreated(newSessionId)
        }
    }

    // Stub implementation to simulate export triggering - in real implementation would call CharacterExporter
    fun exportCharacter() {
        // Trigger export state, UI layer handles the Intent sharing
        // _exportUri.value = uriFromExporter
    }

    fun clearExportUri() {
        _exportUri.value = null
    }
}
