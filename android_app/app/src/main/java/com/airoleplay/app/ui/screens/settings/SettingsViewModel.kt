package com.airoleplay.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.data.local.entity.UserPersonaEntity
import com.airoleplay.app.di.NetworkModule
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDao: SettingsDao,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : ViewModel() {

    val activePersona: StateFlow<UserPersonaEntity?> = settingsDao.getActivePersona()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val backendConnections: StateFlow<List<BackendConnectionEntity>> = settingsDao.getAllConnections()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    fun testConnection(connection: BackendConnectionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            _testResult.value = "Testing..."
            try {
                val api = NetworkModule.createBackendAPI(connection.type, connection.baseUrl, okHttpClient, gson)
                val result = api.testConnection()

                if (result.isSuccessful) {
                    _testResult.value = "Success: \${result.message}"
                } else {
                    _testResult.value = "Failed: \${result.message}"
                }
            } catch (e: Exception) {
                _testResult.value = "Error: \${e.message}"
            }
        }
    }

    fun clearTestResult() {
        _testResult.value = null
    }

    fun setActiveConnection(connectionId: Long) {
        viewModelScope.launch {
            settingsDao.deactivateAllConnections()
            val all = backendConnections.value
            all.find { it.id == connectionId }?.let { conn ->
                settingsDao.updateConnection(conn.copy(isActive = true))
            }
        }
    }
}
