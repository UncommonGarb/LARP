package com.airoleplay.app.ui.screens.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.dao.ChatDao
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.data.local.entity.ChatSessionEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiscoverViewModel @Inject constructor(
    private val characterDao: CharacterDao,
    private val chatDao: ChatDao,
    private val settingsDao: SettingsDao
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<String?>("All")
    val selectedTag = _selectedTag.asStateFlow()

    val activePersona: StateFlow<UserPersonaEntity?> = settingsDao.getActivePersona()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // A map of CharacterId to its latest ChatSession to find "Recents"
    private val recentSessions = chatDao.getAllSessions().map { sessions ->
        sessions.distinctBy { it.characterId }.take(5) // Get latest distinct characters
    }

    val recentCharacters: StateFlow<List<CharacterEntity>> = combine(
        characterDao.getAllCharacters(),
        recentSessions
    ) { allChars, recents ->
        val recentIds = recents.map { it.characterId }
        // Order by the recent sessions list
        recentIds.mapNotNull { id -> allChars.find { it.id == id } }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val characters: StateFlow<List<CharacterEntity>> = combine(
        characterDao.getAllCharacters(),
        _searchQuery,
        _selectedTag
    ) { allChars, query, tag ->
        var filtered = allChars
        if (query.isNotBlank()) {
            filtered = filtered.filter { it.name.contains(query, ignoreCase = true) || it.tags.contains(query, ignoreCase = true) }
        }
        if (tag != null && tag != "All") {
            filtered = filtered.filter { it.tags.split(",").map { t -> t.trim().lowercase() }.contains(tag.lowercase()) }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAvailableTags: StateFlow<List<String>> = characterDao.getAllCharacters().map { chars ->
        val tags = mutableSetOf("All")
        chars.forEach { char ->
            char.tags.split(",").map { it.trim() }.filter { it.isNotBlank() }.forEach { tags.add(it) }
        }
        tags.toList().sorted()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("All"))

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectTag(tag: String) {
        _selectedTag.value = tag
    }
}
