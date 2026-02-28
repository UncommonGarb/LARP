package com.airoleplay.app.ui.screens.create

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.CharacterDao
import com.airoleplay.app.data.local.entity.CharacterEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateViewModel @Inject constructor(
    private val characterDao: CharacterDao
) : ViewModel() {

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

    fun updateField(
        name: String = _characterState.value.name,
        description: String = _characterState.value.description,
        personalitySummary: String = _characterState.value.personalitySummary,
        scenario: String = _characterState.value.scenario,
        firstMessage: String = _characterState.value.firstMessage,
        exampleDialogue: String = _characterState.value.exampleDialogue,
        tags: String = _characterState.value.tags,
        systemPromptOverride: String? = _characterState.value.systemPromptOverride,
        postHistoryInstructions: String? = _characterState.value.postHistoryInstructions
    ) {
        _characterState.value = _characterState.value.copy(
            name = name,
            description = description,
            personalitySummary = personalitySummary,
            scenario = scenario,
            firstMessage = firstMessage,
            exampleDialogue = exampleDialogue,
            tags = tags,
            systemPromptOverride = systemPromptOverride,
            postHistoryInstructions = postHistoryInstructions
        )
    }

    fun updateAvatarUri(uri: Uri?) {
        _avatarUri.value = uri
        // We'll process and save the image path during the final save step
    }

    fun saveCharacter(onSuccess: (Long) -> Unit) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                // In a real scenario, you'd copy the content from `_avatarUri` to internal storage here
                val savedId = characterDao.insertCharacter(_characterState.value)
                onSuccess(savedId)
            } finally {
                _isSaving.value = false
            }
        }
    }
}
