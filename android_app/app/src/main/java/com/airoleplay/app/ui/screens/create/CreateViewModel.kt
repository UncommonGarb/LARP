package com.airoleplay.app.ui.screens.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import com.airoleplay.app.utils.FileUtils
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

    private val _hasChanges = MutableStateFlow(false)
    val hasChanges = _hasChanges.asStateFlow()

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
        _hasChanges.value = true
    }

    fun updateAvatarUri(uri: Uri?) {
        _avatarUri.value = uri
        _hasChanges.value = true
    }

    fun saveCharacter(context: Context, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                var finalCharacter = _characterState.value

                _avatarUri.value?.let { uri ->
                    FileUtils.saveImageToInternalStorage(context, uri)?.let { path ->
                        finalCharacter = finalCharacter.copy(avatarImagePath = path)
                    }
                }

                if (finalCharacter.id == 0L) {
                    characterDao.insertCharacter(finalCharacter)
                } else {
                    characterDao.updateCharacter(finalCharacter)
                }
                onSuccess()
            } finally {
                _isSaving.value = false
            }
        }
    }
}
