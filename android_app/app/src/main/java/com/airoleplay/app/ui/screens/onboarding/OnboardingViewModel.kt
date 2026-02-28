package com.airoleplay.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsDao: SettingsDao
) : ViewModel() {

    fun saveInitialConnection(baseUrl: String, type: String) {
        viewModelScope.launch {
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
