package com.airoleplay.app.ui.screens.create

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val characterDao: CharacterDao
) : ViewModel() {

    private val characterId: Long? = savedStateHandle.get<String>("characterId")?.toLongOrNull()

    private val _characterState = MutableStateFlow(CharacterEntity(
        name = "",
        description = "",
        personalitySummary = "",
        scenario = "",
        firstMessage = "",
        exampleDialogue = "",
        tags = ""
    ))
    val characterState = _characterState.asStateFlow()

    private val _avatarUri = MutableStateFlow<Uri?>(null)
    val avatarUri = _avatarUri.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()

    init {
        characterId?.let { id ->
            viewModelScope.launch {
                characterDao.getCharacterById(id).firstOrNull()?.let {
                    _characterState.value = it
                }
            }
        }
    }

    fun updateCharacter(update: (CharacterEntity) -> CharacterEntity) {
        _characterState.value = update(_characterState.value)
    }

    fun updateAvatarUri(uri: Uri?) {
        _avatarUri.value = uri
    }

    fun saveCharacter(onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                if (_characterState.value.id == 0L) {
                    characterDao.insertCharacter(_characterState.value)
                } else {
                    characterDao.updateCharacter(_characterState.value)
                }
                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
