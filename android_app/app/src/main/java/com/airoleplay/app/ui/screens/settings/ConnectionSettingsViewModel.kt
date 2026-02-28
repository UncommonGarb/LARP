package com.airoleplay.app.ui.screens.settings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airoleplay.app.data.local.dao.SettingsDao
import com.airoleplay.app.data.local.entity.BackendConnectionEntity
import com.airoleplay.app.di.NetworkModule
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class ConnectionSettingsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsDao: SettingsDao,
    private val okHttpClient: OkHttpClient,
    private val gson: Gson
) : ViewModel() {

    private val connectionId = savedStateHandle.get<String>("connectionId")?.toLongOrNull() ?: -1L

    private val _connectionState = MutableStateFlow(
        BackendConnectionEntity(
            name = "",
            type = "OLLAMA",
            baseUrl = "http://192.168.1.100:11434",
            isActive = false
        )
    )
    val connectionState = _connectionState.asStateFlow()

    private val _testResult = MutableStateFlow<String?>(null)
    val testResult = _testResult.asStateFlow()

    private val _availableModels = MutableStateFlow<List<String>>(emptyList())
    val availableModels = _availableModels.asStateFlow()

    init {
        if (connectionId != -1L) {
            viewModelScope.launch {
                settingsDao.getAllConnections().firstOrNull()?.find { it.id == connectionId }?.let { conn ->
                    _connectionState.value = conn
                    if (conn.type == "OLLAMA") {
                        // Prefetch models if existing ollama connection
                        testConnection()
                    }
                }
            }
        }
    }

    fun updateField(
        name: String = _connectionState.value.name,
        type: String = _connectionState.value.type,
        baseUrl: String = _connectionState.value.baseUrl,
        modelName: String? = _connectionState.value.modelName,
        instructFormat: String = _connectionState.value.instructFormat,
        isActive: Boolean = _connectionState.value.isActive
    ) {
        _connectionState.value = _connectionState.value.copy(
            name = name,
            type = type,
            baseUrl = baseUrl,
            modelName = modelName,
            instructFormat = instructFormat,
            isActive = isActive
        )
    }

    fun testConnection() {
        val conn = _connectionState.value
        if (conn.baseUrl.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _testResult.value = "Testing..."
            try {
                val api = NetworkModule.createBackendAPI(conn.type, conn.baseUrl, okHttpClient, gson)
                val result = api.testConnection()

                if (result.isSuccessful) {
                    _testResult.value = "Success: \${result.message}"
                    if (conn.type == "OLLAMA") {
                        _availableModels.value = result.models.map { it.name }
                    } else if (conn.type == "KOBOLDCPP" && result.currentModel != null) {
                        _availableModels.value = listOf(result.currentModel)
                        updateField(modelName = result.currentModel)
                    }
                } else {
                    _testResult.value = "Failed: \${result.message}"
                }
            } catch (e: Exception) {
                _testResult.value = "Error: \${e.message}"
            }
        }
    }

    fun saveConnection(onComplete: () -> Unit) {
        viewModelScope.launch {
            if (_connectionState.value.isActive) {
                settingsDao.deactivateAllConnections()
            }
            if (connectionId == -1L) {
                settingsDao.insertConnection(_connectionState.value)
            } else {
                settingsDao.updateConnection(_connectionState.value)
            }
            onComplete()
        }
    }

    fun deleteConnection(onComplete: () -> Unit) {
        if (connectionId != -1L) {
            viewModelScope.launch {
                settingsDao.deleteConnection(_connectionState.value)
                onComplete()
            }
        }
    }
}
