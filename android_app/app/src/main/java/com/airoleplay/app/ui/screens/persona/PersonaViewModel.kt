package com.airoleplay.app.ui.screens.persona

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonaViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    val personas: StateFlow<List<UserPersonaEntity>> = settingsDao.getAllPersonas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setActivePersona(personaId: Long) {
        viewModelScope.launch {
            settingsDao.deactivateAllPersonas()
            val all = personas.value
            all.find { it.id == personaId }?.let { personaToActivate ->
                settingsDao.updatePersona(personaToActivate.copy(isActive = true))
            }
        }
    }

    fun addPersona(name: String, description: String, avatarUri: Uri?) {
        viewModelScope.launch {
            // For now, save URI string. Production app would copy URI to internal storage as done in Character creation
            val newPersona = UserPersonaEntity(
                name = name,
                description = description,
                avatarImagePath = avatarUri?.toString(),
                isActive = personas.value.isEmpty() // If first, make active
            )
            settingsDao.insertPersona(newPersona)
        }
    }

    fun deletePersona(persona: UserPersonaEntity) {
        viewModelScope.launch {
            settingsDao.deletePersona(persona)
            // If deleted active, make another active
            if (persona.isActive) {
                val nextActive = personas.value.find { it.id != persona.id }
                if (nextActive != null) {
                    settingsDao.updatePersona(nextActive.copy(isActive = true))
                }
            }
        }
    }
}
