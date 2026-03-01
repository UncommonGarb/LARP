package com.airoleplay.app.ui.screens.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.LorebookEntryEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LorebookViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    fun getLorebookEntries(characterId: Long): StateFlow<List<LorebookEntryEntity>> {
        return settingsDao.getLorebookEntriesForCharacter(characterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
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
