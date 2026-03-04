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

    private val _selectedCategory = MutableStateFlow("GENERAL")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val entries: StateFlow<List<LorebookEntryEntity>> = if (characterId != -1L) {
        combine(
            settingsDao.getLorebookEntriesForCharacter(characterId),
            _selectedCategory
        ) { entries, category ->
            entries.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    } else {
        MutableStateFlow(emptyList())
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
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
