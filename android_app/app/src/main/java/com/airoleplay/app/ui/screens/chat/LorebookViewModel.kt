package com.airoleplay.app.ui.screens.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LorebookViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val settingsDao: SettingsDao
) : ViewModel() {

    val characterId: Long = savedStateHandle.get<String>("characterId")?.toLongOrNull() ?: -1L

    val entries: StateFlow<List<LorebookEntryEntity>> = if (characterId != -1L) {
        settingsDao.getLorebookEntriesForCharacter(characterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun addLorebookEntry(entry: LorebookEntryEntity) {
        viewModelScope.launch {
            settingsDao.insertLorebookEntry(entry)
        }
    }

    fun updateLorebookEntry(entry: LorebookEntryEntity) {
        viewModelScope.launch {
            settingsDao.updateLorebookEntry(entry)
        }
    }

    fun deleteLorebookEntry(entry: LorebookEntryEntity) {
        viewModelScope.launch {
            settingsDao.deleteLorebookEntry(entry)
        }
    }
}
