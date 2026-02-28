package com.airoleplay.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    val isOnboarded: StateFlow<Boolean> = settingsDao.getAllConnections()
        .map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveInitialConnection(baseUrl: String, type: String) {
        viewModelScope.launch {
            settingsDao.deactivateAllConnections()
            val connection = BackendConnectionEntity(
                name = "My Initial Backend",
                type = type,
                baseUrl = baseUrl,
                isActive = true
            )
            settingsDao.insertConnection(connection)
        }
    }
}
