package com.airoleplay.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "backend_connections")
data class BackendConnectionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: String, // "OLLAMA" or "KOBOLDCPP"
    val baseUrl: String,
    val modelName: String? = null,
    val temperature: Float = 0.8f,
    val topP: Float = 0.9f,
    val topK: Int = 40,
    val repetitionPenalty: Float = 1.1f,
    val contextSizeLimit: Int = 4096,
    val maxNewTokens: Int = 400,
    val isActive: Boolean = false,
    val instructFormat: String = "ChatML",
    val useCustomSettings: Boolean = false // When false, inherits from GlobalSettingsEntity defaults
)
