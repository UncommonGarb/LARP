package com.airoleplay.app.ui.screens.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatMessageEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatHistoryItem(
    val session: ChatSessionEntity,
    val character: CharacterEntity,
    val lastMessage: ChatMessageEntity?
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatDao: ChatDao,
    private val characterDao: CharacterDao
) : ViewModel() {

    private val _chatHistory = MutableStateFlow<List<ChatHistoryItem>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryItem>> = _chatHistory

    init {
        viewModelScope.launch {
            chatDao.getAllSessions().collect { sessions ->
                val items = sessions.mapNotNull { session ->
                    val char = characterDao.getCharacterById(session.characterId).firstOrNull() ?: return@mapNotNull null
                    val msgs = chatDao.getMessagesForSession(session.id).firstOrNull()
                    val lastMsg = msgs?.lastOrNull()
                    ChatHistoryItem(session, char, lastMsg)
                }
                _chatHistory.value = items
            }
        }
    }

    fun deleteSession(session: ChatSessionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            chatDao.deleteSession(session)
        }
    }
}
